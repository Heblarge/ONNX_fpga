package LayerNormalization

import spinal.core._
import spinal.lib._
import FloatingPoint._

case class LayerNormOpts(
                          c: FpxxConfig,
                          dim: Int,
                          epsilon: Double = 1e-5
                        )

class LayerNorm(opts: LayerNormOpts) extends Component {
  val io = new Bundle {
    val input  = slave  Flow (Vec(Fpxx(opts.c), opts.dim))
    val gamma  = in     Vec(Fpxx(opts.c), opts.dim)
    val beta   = in     Vec(Fpxx(opts.c), opts.dim)
    val output = master Flow (Vec(Fpxx(opts.c), opts.dim))
  }

  // --- 1. 计算均值 (Mean) ---
  val sumChain = new FpxxAddChain(io.input.payload.map(x => {
    val f = Flow(Fpxx(opts.c))
    f.valid := io.input.valid
    f.payload := x
    f
  }).toSeq, opts.c) // 显式转为 Seq

  val meanMul = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
  meanMul.io.input.valid := sumChain.result.valid
  meanMul.io.input.payload.a := sumChain.result.payload
  meanMul.io.input.payload.b := AttentionOps.fpxxConst(1.0 / opts.dim, opts.c)

  val mean = meanMul.io.result // Flow[Fpxx]

  // --- 2. 计算 (X - Mean) 并暂存分支 ---
  val xMinusMeanFlows = Array.tabulate(opts.dim) { i =>
    val lat = LatencyAnalysis(io.input.valid, mean.valid)
    val xDelayed = AttentionOps.delayWhenValid(io.input.payload(i), lat, io.input.valid)

    val sub = new FpxxSubCompatible(FpxxAdd.Options(opts.c, pipeStages = 2))
    sub.io.op.valid := mean.valid
    sub.io.op.a := xDelayed
    sub.io.op.b := mean.payload
    sub.io.result // Flow[Fpxx]
  }

  // --- 3. 计算方差 (Variance) ---
  val varMuls = Array.tabulate(opts.dim) { i =>
    val mul = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
    mul.io.input.valid := xMinusMeanFlows(i).valid
    mul.io.input.payload.a := xMinusMeanFlows(i).payload
    mul.io.input.payload.b := xMinusMeanFlows(i).payload
    mul.io.result
  }

  val varSumChain = new FpxxAddChain(varMuls, opts.c)

  val varMeanMul = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
  varMeanMul.io.input.valid := varSumChain.result.valid
  varMeanMul.io.input.payload.a := varSumChain.result.payload
  varMeanMul.io.input.payload.b := AttentionOps.fpxxConst(1.0 / opts.dim, opts.c)

  val varWithEps = new FpxxAddCompatible(FpxxAdd.Options(opts.c, pipeStages = 1))
  varWithEps.io.op.valid := varMeanMul.io.result.valid
  varWithEps.io.op.a := varMeanMul.io.result.payload
  varWithEps.io.op.b := AttentionOps.fpxxConst(opts.epsilon, opts.c)

  // --- 4. 计算标准差及其倒数 (Sqrt & Div) ---
  // 使用你提供的 FpxxSqrt
  val sqrtUnit = new FpxxSqrt(opts.c, FpxxSqrtConfig(pipeStages = 1))
  sqrtUnit.io.op_vld := varWithEps.io.result.valid
  sqrtUnit.io.op     := varWithEps.io.result.payload

  // 使用封装好的 FpxxDivCompatible
  val divUnit = new FpxxDivCompatible(opts.c, pipeStages = 2)
  divUnit.io.input.valid := sqrtUnit.io.result_vld
  divUnit.io.input.payload.num := AttentionOps.fpxxConst(1.0, opts.c)
  divUnit.io.input.payload.den := sqrtUnit.io.result

  val invStdDev = divUnit.io.result // Flow[Fpxx]

  // --- 5. 最终对齐与输出 (Align X-Mean to InvStdDev) ---
  val finalSyncLat = LatencyAnalysis(xMinusMeanFlows(0).valid, invStdDev.valid)

  val finalResults = Array.tabulate(opts.dim) { i =>
    // 对齐 (X - Mean)
    val xmmAligned = AttentionOps.delayWhenValid(xMinusMeanFlows(i).payload, finalSyncLat, xMinusMeanFlows(i).valid)

    // (X-Mean) * (1/StdDev)
    val mul1 = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
    mul1.io.input.valid := invStdDev.valid
    mul1.io.input.payload.a := xmmAligned
    mul1.io.input.payload.b := invStdDev.payload

    // * Gamma
    val mul2 = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
    mul2.io.input.valid := mul1.io.result.valid
    mul2.io.input.payload.a := mul1.io.result.payload
    mul2.io.input.payload.b := io.gamma(i)

    // + Beta
    val adder = new FpxxAddCompatible(FpxxAdd.Options(opts.c, pipeStages = 1))
    adder.io.op.valid := mul2.io.result.valid
    adder.io.op.a := mul2.io.result.payload
    adder.io.op.b := io.beta(i)
    adder.io.result
  }

  io.output.valid := finalResults.head.valid
  // 使用 zip 是一种更健壮的硬件赋值方式
  (io.output.payload, finalResults).zipped.foreach(_ := _.payload)
}
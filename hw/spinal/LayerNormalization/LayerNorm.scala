package LayerNormalization

import spinal.core._
import spinal.lib._
import FloatingPoint._
import FloatingPoint.AttentionOps._


case class LayerNormOpts(
                          c: FpxxConfig,
                          dim: Int,
                          epsilon: Double = 1e-5
                        )


class LayerNorm(opts: LayerNormOpts) extends Component {
  val io = new Bundle {
    // 输入：一组浮点数
    val input  = slave Flow(Vec(Fpxx(opts.c), opts.dim))
    val gamma  = in Vec(Fpxx(opts.c), opts.dim)
    val beta   = in Vec(Fpxx(opts.c), opts.dim)
    val output = master Flow(Fpxx(opts.c))
  }

  println(s">>> 正在构建 LayerNorm 硬件模块 <<<")
  println(s">>> 配置位宽: ${opts.c.full_size} bits (Exp:${opts.c.exp_size}, Mant:${opts.c.mant_size})")
  // --- 1. 计算均值 (ReduceMean) ---
  val inputFlows = io.input.payload.map(item => {
    val f = Flow(Fpxx(opts.c))
    f.valid := io.input.valid
    f.payload := item
    f
  }).toSeq
  val sumChain = new FpxxAddChain(inputFlows, opts.c)
  val sum = sumChain.result

  // 计算 Mean = Sum / dim
  val invDim = fpxxConst(1.0 / opts.dim, opts.c)
  val meanUnit = new FpxxMulCompatible(FpxxMul.Options(opts.c, pipeStages = 2))
  meanUnit.io.input.valid := sum.valid
  meanUnit.io.input.payload.a := sum.payload
  meanUnit.io.input.payload.b := invDim

  io.output <<  meanUnit.io.result // 最终的均值 E[x]

  // --- 2. 这里的 mean 比输入数据晚了很多个时钟周期 (由于加法树的流水线延迟) ---
  // 我们需要把原始输入延迟对齐，才能做后面的 (X - Mean)
  //val addedLatency = LatencyAnalysis(io.input.valid, mean.valid)
  //val delayedInput = delayWhenValid(io.input.payload, addedLatency, io.input.valid)


  //when(mean.valid) {8
    //val meanVal = mean.payload
    // 这里打印出十六进制，方便对比
    // printf(s"Time: $${simTime()}, Mean Value: %x\n", meanVal.asBits)
  //}
  // 占位逻辑：先观察 mean 是否计算正确
  //io.output.valid := mean.valid
  //io.output.payload := delayedInput // 暂时先输出延迟对齐后的输入


}
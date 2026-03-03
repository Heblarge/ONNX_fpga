package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * SystolicArray 模块
 * size: 方阵的大小 (N x N)
 * fpxxCfg: 输入浮点数的配置 (如 Fp8 或 Fp16)
 * accIntBits/accFracBits: 定点累加器的整数和小数位宽
 */
case class SystolicArray(
    size: Int,
    fpxxCfg: FpxxConfig,
    accIntBits: BitCount,
    accFracBits: BitCount,
    mulStages: Int = 1,
    f2iStages: Int = 1
) extends Component {

  val io = new Bundle {
    // 矩阵 A 从左侧输入 (size 路)
    val dinA = Vec(slave Stream(Fpxx(fpxxCfg)), size)
    // 矩阵 B 从上方输入 (size 路)
    val dinB = Vec(slave Stream(Fpxx(fpxxCfg)), size)

    // 控制信号：清空所有 PE 内部的累加器
    val clear = in Bool()

    // 输出：所有 PE 的当前累加值 (定点数矩阵)
    val results = out Vec(Vec(AFix.SQ(accIntBits, accFracBits), size), size)
  }

  // 1. 实例化 PE 矩阵
  // 注意：FpxxPE 内部的 mulStages 使用了 StageMask，这里简单转为常数处理
  val peMatrix = Array.tabulate(size, size) { (r, c) =>
    val pe = new FpxxPE(
      fpxxCfg     = fpxxCfg,
      accIntBits  = accIntBits,
      accFracBits = accFracBits,
      mulStages   = mulStages,
      f2iStages   = f2iStages
    )
    pe.setName(s"PE_${r}_${c}")
    pe
  }

  // 2. 建立 PE 之间的互联逻辑
  for (r <- 0 until size) {
    for (c <- 0 until size) {
      val pe = peMatrix(r)(c)
      pe.io.clear := io.clear

      // --- 处理 A 数据流 (水平方向) ---
      if (c == 0) {
        // 第一列：连接外部输入 io.dinA
        pe.io.inSig.valid := io.dinA(r).valid
        pe.io.inSig.a     := io.dinA(r).payload
        io.dinA(r).ready  := True // 这里假设阵列总是 ready，可根据流控需求修改
      } else {
        // 后续列：连接左侧 PE 传递过来的 A 数据，延迟一个周期
        val leftPeA = peMatrix(r)(c - 1).io.inSig.a
        val leftValid = peMatrix(r)(c - 1).io.inSig.valid
        
        pe.io.inSig.a     := RegNext(leftPeA)
        pe.io.inSig.valid := RegNext(leftValid) init(False)
      }

      // --- 处理 B 数据流 (垂直方向) ---
      if (r == 0) {
        // 第一行：连接外部输入 io.dinB
        pe.io.inSig.b     := io.dinB(c).payload
        // valid 信号已在处理 A 时由 dinA 提供或逻辑关联
        // 在标准脉动阵列中，A 和 B 共享 valid 或各自独立，这里为了严谨，我们对 B 也做同步
        io.dinB(c).ready  := True
      } else {
        // 后续行：连接上方 PE 传递过来的 B 数据，延迟一个周期
        val topPeB = peMatrix(r - 1)(c).io.inSig.b
        pe.io.inSig.b     := RegNext(topPeB)
      }

      // --- 结果读出 ---
      // 将 PE 内部的 acc 结果映射到顶层输出
      io.results(r)(c) := pe.io.out.payload
    }
  }
}

/**
 * 伴生对象用于简单测试生成
 */
object SystolicArrayApp extends App {
  SpinalVerilog(new SystolicArray(
    size        = 4,
    fpxxCfg     = FpxxConfig.float16(),
    accIntBits  = 16 bits,
    accFracBits = 16 bits
  ))
}
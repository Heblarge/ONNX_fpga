package FloatingPoint
import spinal.core._
import spinal.core.sim._
import scala.util.Random
import FloatingPoint._

object SystolicArraySim extends App {
  // --- 1. 参数配置 ---
  val N = 8                // 阵列规模 (可改为 16, 32 等)
  val fpCfg = FpxxConfig.float16()
  val accInt = 16 bits
  val accFrac = 16 bits
  
  // 这里的延迟需与 FpxxPE 实例化时保持一致
  val mulStages = 1
  val f2iStages = 1

  SimConfig.withWave.compile(new SystolicArray(N, fpCfg, accInt, accFrac, mulStages, f2iStages)).doSim { dut =>
    // --- 2. 环境初始化 ---
    dut.clockDomain.forkStimulus(10)
    dut.io.clear #= false
    dut.io.dinA.foreach(_.valid #= false)
    dut.io.dinB.foreach(_.valid #= false)
    dut.clockDomain.waitSampling()

    // 清空累加器
    dut.io.clear #= true
    dut.clockDomain.waitSampling()
    dut.io.clear #= false

    // --- 3. 随机数据生成与金本位计算 ---
    val matA = Array.fill(N, N)(Random.nextFloat() * 10.0f - 5.0f) // 范围 -5 到 5
    val matB = Array.fill(N, N)(Random.nextFloat() * 10.0f - 5.0f)
    val golden = Array.fill(N, N)(0.0)

    // CPU 计算参考结果
    for (i <- 0 until N; j <- 0 until N; k <- 0 until N) {
      golden(i)(j) += (matA(i)(k).toDouble * matB(k)(j).toDouble)
    }

    // --- 4. 自动化斜角驱动逻辑 ---
    // 总输入跨度是 2N-1 个周期
    // 整个仿真需运行足够长以覆盖：输入斜角 + 阵列穿透延迟 + PE内部延迟
    val inputDuration = N + N - 1
    val totalSimCycles = inputDuration + N + mulStages + f2iStages + 5

    println(s"Starting $N x $N Matrix Multiplication...")

    for (cycle <- 0 until totalSimCycles) {
      for (i <- 0 until N) {
        // 驱动 Matrix A (左侧第 i 行)
        val aCol = cycle - i
        if (aCol >= 0 && aCol < N) {
          val (s, e, m) = floatToFpComponents(matA(i)(aCol), fpCfg)
          dut.io.dinA(i).valid #= true
          dut.io.dinA(i).payload.sign #= (s != 0)
          dut.io.dinA(i).payload.exp  #= e
          dut.io.dinA(i).payload.mant #= m
        } else {
          dut.io.dinA(i).valid #= false
        }

        // 驱动 Matrix B (上方第 i 列)
        val bRow = cycle - i
        if (bRow >= 0 && bRow < N) {
          val (s, e, m) = floatToFpComponents(matB(bRow)(i), fpCfg)
          dut.io.dinB(i).valid #= true
          dut.io.dinB(i).payload.sign #= (s != 0)
          dut.io.dinB(i).payload.exp  #= e
          dut.io.dinB(i).payload.mant #= m
        } else {
          dut.io.dinB(i).valid #= false
        }
      }
      dut.clockDomain.waitSampling()
    }

    // --- 5. 自动化校验 ---
    var pass = true
    val tolerance = 0.5 // 允许的误差（取决于定点数精度和浮点数动态范围）
    
    println("\n--- Comparing Hardware results with Golden Model ---")
    for (r <- 0 until N) {
      for (c <- 0 until N) {
        val hwVal = dut.io.results(r)(c).toDouble
        val swVal = golden(r)(c)
        val diff = (hwVal - swVal).abs
        
        if (diff > tolerance) {
          println(f"Error at [$r%d,$c%d]: HW=$hwVal%.4f, SW=$swVal%.4f (Diff=$diff%.4f)")
          pass = false
        }
      }
    }

    if (pass) println("\n[TEST PASSED] All results are within tolerance!")
    else println("\n[TEST FAILED] Significant deviations detected.")
  }

  // --- 辅助转换函数 ---
  def floatToFpComponents(v: Float, cfg: FpxxConfig): (BigInt, BigInt, BigInt) = {
    import java.lang.Float._
    val i = floatToRawIntBits(v)
    val s = (i >> 31) & 0x1
    val e = (i >> 23) & 0xFF
    val m = i & 0x7FFFFF
    val newExp = if (v == 0) 0 else (e - 127 + cfg.bias).max(0).min((1 << cfg.exp_size) - 1)
    val newMant = m >> (23 - cfg.mant_size)
    (BigInt(s), BigInt(newExp), BigInt(newMant))
  }
}


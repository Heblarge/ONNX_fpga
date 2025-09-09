package Accelerator

import Util._
import Interface._

import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.StreamDriver
import spinal.sim.VCSFlags
import scala.jdk.CollectionConverters._
import scala.math._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import java.io.FileWriter
import scala.sys.process._
import java.lang.ProcessBuilder
import scala.util.control.NonFatal

// ========================= 子程序：单次性能测试 =========================
object AcceleratorPerfOnce extends App {
  // 统一模式：总是接收21个参数
  require(args.length == 20, s"AcceleratorPerfOnce: expect 20 args, got ${args.length}")
  
  // 解析参数
  val testID                      = args(0).toInt
  val runID                       = args(1).toInt
  val systolicArraySideNum        = args(2).toInt
  val systolicArrayInFifoDepth    = args(3).toInt
  val systolicArrayOutFifoDepth   = args(4).toInt
  val systolicArrayInstFifoDepth  = args(5).toInt
  val activationOutFifoDepth      = args(6).toInt
  val slicedInstFifoDepth         = args(7).toInt
  val numCores                    = args(8).toInt
  val testNum                     = args(9).toInt
  val period                      = args(10).toInt
  val instDriveSpeed              = args(11).toFloat
  val errRateLimit                = args(12).toDouble
  val zeroLimit                   = args(13).toInt
  val baseSeed                    = args(14).toInt
  val UIDWidth                    = args(15).toInt
  val AddressWidth                = args(16).toInt
  val ShapeWidth                  = args(17).toInt
  val elementWidth                = args(18).toInt
  val intWidth                    = args(19).toInt

  // 构建配置
  val currentCfg = AcceleratorCfg(
    UIDWidth = UIDWidth,
    AddressWidth = AddressWidth,
    ShapeWidth = ShapeWidth,
    systolicArraySideNum = systolicArraySideNum,
    elementWidth = elementWidth,
    intWidth = intWidth,
    systolicArrayInFifoDepth = systolicArrayInFifoDepth,
    systolicArrayOutFifoDepth = systolicArrayOutFifoDepth,
    systolicArrayInstFifoDepth = systolicArrayInstFifoDepth,
    activationOutFifoDepth = activationOutFifoDepth,
    slicedInstFifoDepth = slicedInstFifoDepth,
    numCores = numCores
  )

  // ---- 打开CSV（追加写）----
  val resultsFile = new FileWriter("performance_results.csv", true)

  // ---- 单轮仿真逻辑（与你原代码一致）----
  try {
    val runSeed = baseSeed + runID * 1000
    val random  = new Random(runSeed)

    println(s"[Once] Testing configuration (Test ID: $testID), Run $runID, seed=$runSeed")
    println(s"[Once] Configuration: $currentCfg")
    
    val path = s"simWorkspace/Accelerator_PerformanceTest_${testID}_Run${runID}"
    import java.io.File
    new File(path).mkdirs()
    val compiled = SimConfig.workspacePath(path)//.withVcdWave
      .withConfig(
        SpinalConfig(
          bitVectorWidthMax = 100000
        )
      )
      .withVCS(
        VCSFlags(
          compileFlags   = List("-kdb", "-lca", "+notimingchecks", "-reportstats"),
          elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks", "-reportstats"),
          runFlags       = List("-l ./run.log")
        )
      )
      .compile {
        val dut = Accelerator(currentCfg)
        dut.sdpramA.mem.simPublic()
        dut.sdpramB.mem.simPublic()
        dut.sdpramZ.mem.simPublic()
        dut.collector.instFinish.simPublic()
        dut
      }

    def genMat(instSim: InstSim) = {
      if (instSim.activationFunction == Activation_TypeDef.Log) {
        val one = pow(2, currentCfg.fracWidth).toInt
        val matA =
          if (random.nextBoolean) random.nextMat(instSim.input0Shape0, instSim.input0Shape1, one * 2, one * 3)
          else random.nextMat(instSim.input0Shape0, instSim.input0Shape1, one / 3, one / 2)
        val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1, 0, 1)
        (matA, matB)
      } else {
        val matA = random.nextMat(instSim.input0Shape0, instSim.input0Shape1, -9, 10)
        val matB = random.nextMat(instSim.input1Shape0, instSim.input1Shape1, -9, 10)
        (matA, matB)
      }
    }

    val instSims = ArrayBuffer[InstSim]()
    val matAs    = ArrayBuffer[Array[Array[Int]]]()
    val matBs    = ArrayBuffer[Array[Array[Int]]]()
    val matZs    = ArrayBuffer[Array[Array[Int]]]()
    for (i <- 0 until testNum) {
      val instSim = InstSim(random, currentCfg.systolicArraySideNum)
      instSims += instSim
      val (matA, matB) = genMat(instSim)
      matAs += matA
      matBs += matB
      matZs += instSim.acceleratorSim(matA, matB, currentCfg)
    }

    var totalCycles: Long   = -1
    var cyclesPerTest: Double = -1
    var totalOps: Long = -1 // 总浮点运算次数
    var flopsPerCycle: Double = -1 // 每周期浮点运算次数 (FLOPS/cycle)

    compiled.doSimUntilVoid { dut =>
      SimTimeout(1000000 * 4 * period)
      dut.clockDomain.forkStimulus(period)
      dut.clkCore.forkStimulus(4*period)

      var startTime: Long = -1
      var endTime: Long   = -1
      var cycleCount: Long = 0

      // 手动计数
      fork {
        while (true) {
          dut.clockDomain.waitSampling()
          cycleCount += 1
        }
      }

      // 预装载
      InstSim.memSetInstSims(
        dut.sdpramA.mem,
        instSims,
        true,
        matAs,
        currentCfg.systolicArraySideNum,
        currentCfg.elementWidth
      )
      InstSim.memSetInstSims(
        dut.sdpramB.mem,
        instSims,
        false,
        matBs,
        currentCfg.systolicArraySideNum,
        currentCfg.elementWidth
      )

      var m = 0
      StreamDriver(dut.io.inst, dut.clockDomain) { payload =>
        if (m < testNum) {
          if (m == 0) startTime = cycleCount
          instSims(m).driveSim(payload)
          m += 1
          true
        } else false
      }.setFactor(instDriveSpeed)

      var n = 0
      fork {
        while (true) {
          dut.clockDomain.waitSamplingWhere(dut.collector.instFinish.toBoolean == true)
          dut.clockDomain.waitSampling()
          dut.clockDomain.waitSampling()
          val instSim   = instSims(n)
          val matA      = matAs(n)
          val matB      = matBs(n)
          val matZRef   = matZs(n)
          val matZResult = memGetMat(
            dut.sdpramZ.mem,
            instSim.outputAddress,
            currentCfg.systolicArraySideNum,
            currentCfg.elementWidth,
            instSim.outputShape0,
            instSim.outputShape1
          )
          matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
            val err     = abs(zRef - zResult)
            val errRate = abs((zRef - zResult).toDouble / zRef.toDouble)
            assert(
              if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
              s"matZRef compute error, test $n, position ($i, $j), zRef: $zRef(${zRef.toHexString}), " +
              s"zResult: $zResult(${zResult.toHexString}), err:$err, errRate: $errRate\n" +
              s"$instSim\nmatA:\n${matToStringWithHex(matA)}\nmatB:\n${matToStringWithHex(matB)}\n" +
              s"matZRef:\n${matToStringWithHex(matZRef)}\nmatZResult:\n${matToStringWithHex(matZResult)}"
            )
          }
          println(s"[Once] test $n pass")
          n += 1
          if (n == testNum) {
            endTime = cycleCount
            totalCycles   = endTime - startTime
            cyclesPerTest = totalCycles.toDouble / testNum
            
            // 计算总浮点运算次数和 FLOPS/cycle
            totalOps = instSims.map { inst =>
              // M*K*N*2，其中 K=inst.input0Shape1，也是 inst.input1Shape0
              2L * inst.input0Shape0 * inst.input0Shape1 * inst.input1Shape1
            }.sum
            flopsPerCycle = totalOps.toDouble / totalCycles

            println(s"[Once] TEST PASS for configuration, Run $runID (Test ID: $testID)")
            println(s"[Once] Total cycles: $totalCycles, Cycles/test: $cyclesPerTest")
            println(s"[Once] Total operations: $totalOps, FLOPS/cycle: $flopsPerCycle")
            simSuccess()
          }
        }
      }
    }

    // 成功写CSV
    resultsFile.write(s"$testID,$runID," +
      s"${currentCfg.systolicArraySideNum}," +
      s"${currentCfg.systolicArrayInFifoDepth}," +
      s"${currentCfg.systolicArrayOutFifoDepth}," +
      s"${currentCfg.systolicArrayInstFifoDepth}," +
      s"${currentCfg.activationOutFifoDepth}," +
      s"${currentCfg.slicedInstFifoDepth}," +
      s"${currentCfg.numCores}," +
      s"$totalCycles,$cyclesPerTest,$flopsPerCycle,Success,\n")
    resultsFile.flush()
    resultsFile.close()
    sys.exit(0)

  } catch {
    case e: Exception =>
      val rawMessage = Option(e.getMessage).getOrElse("Unknown error").replace(",", ";")
      // 失败写CSV
      resultsFile.write(s"$testID,$runID," +
        s"${currentCfg.systolicArraySideNum}," +
        s"${currentCfg.systolicArrayInFifoDepth}," +
        s"${currentCfg.systolicArrayOutFifoDepth}," +
        s"${currentCfg.systolicArrayInstFifoDepth}," +
        s"${currentCfg.activationOutFifoDepth}," +
        s"${currentCfg.slicedInstFifoDepth}," +
        s"${currentCfg.numCores}," +
        s"-1,-1,-1,Failed,$rawMessage\n")
      resultsFile.flush()
      resultsFile.close()
      System.err.println(s"[Once][FAIL] $rawMessage")
      sys.exit(1)
  }
}

// ========================= 父程序：参数扫描/逐轮拉起子进程 =========================
object AcceleratorTb_PerformanceTest extends App {
  val period          = 10
  val instDriveSpeed  = 1.0f
  val errRateLimit    = 0.01
  val zeroLimit       = 10
  val baseSeed        = 114514
  var testNum         = 50

  val runsPerConfig   = 1

  val paramVariations = Map(
    "systolicArraySideNum"        -> List(8, 32),
    "systolicArrayInFifoDepth"    -> List(8, 16, 32),
    "systolicArrayOutFifoDepth"   -> List(8, 16, 32),
    "systolicArrayInstFifoDepth"  -> List(16, 64),
    "activationOutFifoDepth"      -> List(32, 64),
    "slicedInstFifoDepth"         -> List(16, 64),
    "numCores"                    -> List(1, 2, 4)
  )

  val defaultCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 20,
    ShapeWidth = 16,
    systolicArraySideNum = 4,
    elementWidth = 24,
    intWidth = 12,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 32,
    slicedInstFifoDepth = 16,
    numCores = 1
  )

  // 生成所有可能的参数组合（笛卡尔积）
  val paramNames = paramVariations.keys.toList
  val paramValues = paramNames.map(paramVariations)
  val allCombinations = cartesianProduct(paramValues).map(values => paramNames.zip(values).toMap)

  // 辅助函数：计算笛卡尔积
  def cartesianProduct[T](lists: List[List[T]]): List[List[T]] = {
    lists match {
      case Nil => List(Nil)
      case head :: tail => for {
        h <- head
        t <- cartesianProduct(tail)
      } yield h :: t
    }
  }

  // 若CSV不存在或为空，需要写表头
  val csvFile = new java.io.File("performance_results.csv")
  val needHeader = !csvFile.exists() || csvFile.length() == 0
  val resultsFile = new FileWriter(csvFile, true)
  if (needHeader) {
    resultsFile.write("TestID,RunID,SA_SideNum,SA_InFifoDepth,SA_OutFifoDepth," +
      "SA_InstFifoDepth,activationOutFifoDepth,slicedInstFifoDepth,numCores," +
      "TotalCycles,CyclesPerTest,FLOPSPerCycle,Status,ErrorMessage\n")
    resultsFile.flush()
  }
  resultsFile.close()

  var testID = 0
  val classpath = System.getProperty("java.class.path")
  require(classpath != null && classpath.nonEmpty, "java.class.path is empty; cannot fork child JVM")

  // 遍历所有参数组合
  for (combination <- allCombinations) {
    testID += 1
    println(s"[Parent] Testing combination (Test ID: $testID): ${combination.mkString(", ")}")

    for (runID <- 1 to runsPerConfig) {
      // 使用当前参数组合创建配置
      val currentCfg = defaultCfg.copy(
        systolicArraySideNum = combination.getOrElse("systolicArraySideNum", defaultCfg.systolicArraySideNum).asInstanceOf[Int],
        systolicArrayInFifoDepth = combination.getOrElse("systolicArrayInFifoDepth", defaultCfg.systolicArrayInFifoDepth).asInstanceOf[Int],
        systolicArrayOutFifoDepth = combination.getOrElse("systolicArrayOutFifoDepth", defaultCfg.systolicArrayOutFifoDepth).asInstanceOf[Int],
        systolicArrayInstFifoDepth = combination.getOrElse("systolicArrayInstFifoDepth", defaultCfg.systolicArrayInstFifoDepth).asInstanceOf[Int],
        activationOutFifoDepth = combination.getOrElse("activationOutFifoDepth", defaultCfg.activationOutFifoDepth).asInstanceOf[Int],
        slicedInstFifoDepth = combination.getOrElse("slicedInstFifoDepth", defaultCfg.slicedInstFifoDepth).asInstanceOf[Int],
        numCores = combination.getOrElse("numCores", defaultCfg.numCores).asInstanceOf[Int]
      )

      val args = Array(
        testID.toString,           // 0
        runID.toString,            // 1
        currentCfg.systolicArraySideNum.toString, // 3
        currentCfg.systolicArrayInFifoDepth.toString, // 4
        currentCfg.systolicArrayOutFifoDepth.toString, // 5
        currentCfg.systolicArrayInstFifoDepth.toString, // 6
        currentCfg.activationOutFifoDepth.toString, // 7
        currentCfg.slicedInstFifoDepth.toString, // 8
        currentCfg.numCores.toString, // 9
        testNum.toString,          // 10
        period.toString,           // 11
        instDriveSpeed.toString,   // 12
        errRateLimit.toString,     // 13
        zeroLimit.toString,        // 14
        baseSeed.toString,         // 15
        currentCfg.UIDWidth.toString,      // 16
        currentCfg.AddressWidth.toString,  // 17
        currentCfg.ShapeWidth.toString,    // 18
        currentCfg.elementWidth.toString,  // 19
        currentCfg.intWidth.toString       // 20
      )

      val cmd = Seq(
        "java",
        "-cp", classpath,
        "Accelerator.AcceleratorPerfOnce"
      ) ++ args

      val pb = new ProcessBuilder(cmd.asJava)
      pb.inheritIO()
      val proc = pb.start()
      val exit = proc.waitFor()
      if (exit == 0) {
        println(s"[Parent] Run $runID/$runsPerConfig (Test ID: $testID) done.")
      } else {
        println(s"[Parent][WARN] Run $runID failed (exit=$exit). See above logs.")
      }
    }
  }
  println("All parameter combinations tested successfully!")
}
package Accelerator

import Util._
import Interface._

import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.StreamDriver
import spinal.sim.VCSFlags

import scala.math._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import java.io.FileWriter

object AcceleratorTb_PerformanceTest extends App {
  val period = 10
  val instDriveSpeed = 1.0f
  val errRateLimit = 0.01
  val zeroLimit = 10
  val baseSeed = 114514
  var testNum = 100
  
  // 定义每个配置运行的次数
  val runsPerConfig = 3

  // 定义要测试的参数范围
  val paramVariations = Map(
    "systolicArraySideNum" -> List(4, 8, 16,32),
    "systolicArrayInFifoDepth" -> List(8,16,32),
    "systolicArrayOutFifoDepth" -> List(8,16,32),
    "systolicArrayInstFifoDepth" -> List(16,32, 64),
    "activationOutFifoDepth" -> List(16, 32, 64),
    "slicedInstFifoDepth" -> List(16, 32, 64),
    "numCores" -> List(1, 2, 4, 8, 16)
  )

  // 默认配置
  val defaultCfg = AcceleratorCfg(
    UIDWidth = 19,
    AddressWidth = 17,
    ShapeWidth = 15,
    systolicArraySideNum = 4,
    elementWidth = 25,
    intWidth = 13,
    systolicArrayInFifoDepth = 2,
    systolicArrayOutFifoDepth = 2,
    systolicArrayInstFifoDepth = 16,
    activationOutFifoDepth = 16,
    slicedInstFifoDepth = 16,
    numCores = 1
  )

  // 结果文件 - 记录所有参数
  val resultsFile = new FileWriter("performance_results.csv", true)
  resultsFile.write("TestID,RunID,SA_SideNum,SA_InFifoDepth,SA_OutFifoDepth," +
    "SA_InstFifoDepth,activationOutFifoDepth,slicedInstFifoDepth,numCores," +
    "TotalCycles,CyclesPerTest,Throughput,Status,ErrorMessage\n")

  var testID = 0

  // 参数扫描
  for ((paramName, values) <- paramVariations) {
    for (value <- values) {
      testID += 1
      
      // 创建当前测试的配置
      val currentCfg = paramName match {
        case "systolicArraySideNum" => defaultCfg.copy(systolicArraySideNum = value)
        case "systolicArrayInFifoDepth" => defaultCfg.copy(systolicArrayInFifoDepth = value)
        case "systolicArrayOutFifoDepth" => defaultCfg.copy(systolicArrayOutFifoDepth = value)
        case "systolicArrayInstFifoDepth" => defaultCfg.copy(systolicArrayInstFifoDepth = value)
        case "activationOutFifoDepth" => defaultCfg.copy(activationOutFifoDepth = value)
        case "slicedInstFifoDepth" => defaultCfg.copy(slicedInstFifoDepth = value)
        case "numCores" => defaultCfg.copy(numCores = value)
      }

      println(s"Testing $paramName = $value (Test ID: $testID)")

      // 对每个配置运行多次
      for (runID <- 1 to runsPerConfig) {
        try {
          val runSeed = baseSeed + runID * 1000
          val random = new Random(runSeed)
          
          println(s"  Run $runID/$runsPerConfig with seed $runSeed")

          val compiled = SimConfig.withVcdWave
            .withConfig(
              SpinalConfig(
                bitVectorWidthMax = 100000
              )
            )
            .withVCS(
              VCSFlags(
                compileFlags = List("-kdb", "-lca", "+notimingchecks","-reportstats"),
                elaborateFlags = List("-fgp", "-kdb", "-lca", "+rad", "+notimingchecks","-reportstats"),
                runFlags = List("-l ./run.log")
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
          val matAs = ArrayBuffer[Array[Array[Int]]]()
          val matBs = ArrayBuffer[Array[Array[Int]]]()
          val matZs = ArrayBuffer[Array[Array[Int]]]()
          for (i <- 0 until testNum) {
            val instSim = InstSim(random, currentCfg.systolicArraySideNum)
            instSims += instSim
            val (matA, matB) = genMat(instSim)
            matAs += matA
            matBs += matB
            matZs += instSim.acceleratorSim(matA, matB, currentCfg.elementWidth, currentCfg.fracWidth)
          }

          var totalCycles: Long = -1
          var cyclesPerTest: Double = -1
          var throughput: Double = -1
          
          compiled.doSimUntilVoid { dut =>
            SimTimeout(1000000 * period)
            dut.clockDomain.forkStimulusRandomClk(random, period)
            dut.clkCore.forkStimulusRandomClk(random, period)
            
            // 性能测量变量
            var startTime: Long = -1
            var endTime: Long = -1
            var cycleCount: Long = 0
            
            // 手动计数时钟周期
            fork {
              while (true) {
                dut.clockDomain.waitSampling()
                cycleCount += 1
              }
            }
            
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
                if (m == 0) {
                  startTime = cycleCount // 记录第一个指令开始的时间
                }
                instSims(m).driveSim(payload)
                m += 1
                true
              } else {
                false
              }
            }.setFactor(instDriveSpeed)

            var n = 0
            fork {
              while (true) {
                dut.clockDomain.waitSamplingWhere(dut.collector.instFinish.toBoolean == true)
                dut.clockDomain.waitSampling()
                dut.clockDomain.waitSampling()
                val instSim = instSims(n)
                val matA = matAs(n)
                val matB = matBs(n)
                val matZRef = matZs(n)
                val matZResult = memGetMat(
                  dut.sdpramZ.mem,
                  instSim.outputAddress,
                  currentCfg.systolicArraySideNum,
                  currentCfg.elementWidth,
                  instSim.outputShape0,
                  instSim.outputShape1
                )
                matZipForeach(matZRef, matZResult) { (zRef, zResult, i, j) =>
                  val err = abs(zRef - zResult)
                  val errRate = abs((zRef - zResult).toDouble / zRef.toDouble)
                  assert(
                    if (errRate < errRateLimit) true else abs(zRef) < zeroLimit && abs(zResult) < zeroLimit,
                    s"matZRef compute error, test $n, position ($i, $j), zRef: $zRef(${zRef.toHexString}), ".red +
                      s"zResult: $zResult(${zResult.toHexString}), err:$err, errRate: $errRate\n".red +
                      s"$instSim\nmatA:\n${matToStringWithHex(matA)}\nmatB:\n${matToStringWithHex(matB)}\n" +
                      s"matZRef:\n${matToStringWithHex(matZRef)}\nmatZResult:\n${matToStringWithHex(matZResult)}"
                  )
                }
                println(s"test $n pass")
                n += 1
                if (n == testNum) {
                  endTime = cycleCount // 记录最后一个指令完成的时间
                  
                  // 计算性能指标
                  totalCycles = endTime - startTime
                  cyclesPerTest = totalCycles.toDouble / testNum
                  throughput = testNum.toDouble / totalCycles
                  
                  println(s"TEST PASS for $paramName = $value, Run $runID/$runsPerConfig (Test ID: $testID)".green)
                  println(s"Total cycles: $totalCycles, Cycles per test: $cyclesPerTest, Throughput: $throughput tests/cycle")
                  simSuccess()
                }
              }
            }
          }
          
          // 写入成功结果
          resultsFile.write(s"$testID,$runID," +
            s"${currentCfg.systolicArraySideNum}," +
            s"${currentCfg.systolicArrayInFifoDepth}," +
            s"${currentCfg.systolicArrayOutFifoDepth}," +
            s"${currentCfg.systolicArrayInstFifoDepth}," +
            s"${currentCfg.activationOutFifoDepth}," +
            s"${currentCfg.slicedInstFifoDepth}," +
            s"${currentCfg.numCores}," +
            s"$totalCycles,$cyclesPerTest,$throughput,Success,\n")
          resultsFile.flush()
          
        } catch {
            case e: Exception =>
              // 记录错误信息，处理可能为null的getMessage
              val rawMessage = Option(e.getMessage).getOrElse("Unknown error")
              val errorMsg = rawMessage.replace(",", ";") // 替换逗号以避免CSV格式问题
              println(s"TEST FAILED for $paramName = $value, Run $runID/$runsPerConfig (Test ID: $testID): $errorMsg".red)
              
              // 写入失败结果
              resultsFile.write(s"$testID,$runID," +
                s"${currentCfg.systolicArraySideNum}," +
                s"${currentCfg.systolicArrayInFifoDepth}," +
                s"${currentCfg.systolicArrayOutFifoDepth}," +
                s"${currentCfg.systolicArrayInstFifoDepth}," +
                s"${currentCfg.activationOutFifoDepth}," +
                s"${currentCfg.slicedInstFifoDepth}," +
                s"${currentCfg.numCores}," +
                s"-1,-1,-1,Failed,$errorMsg\n")
              resultsFile.flush()
        }
        import scala.concurrent.duration._
        import java.util.concurrent.TimeUnit
        //delay 10 seconds, so that vcs release the memory and can run again with a new config
        Thread.sleep(10 * 1000)

      }
    }
  }
  
  resultsFile.close()
  println("All parameter variations tested successfully!")
}
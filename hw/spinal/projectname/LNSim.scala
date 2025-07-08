package projectname

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.sim.{FlowDriver, FlowMonitor, ScoreboardInOrder}
import scala.collection.mutable


import scala.util.Random

object LNSim extends App {


  // 新增：兼容Java调用的重载方法
  def runSim(inputFloats: Array[Float]): Array[Float] = {
    val flags = VCSFlags(
      compileFlags = List("-kdb", "-lca"),
      elaborateFlags = List("-kdb", "-lca")
    )
    val cfg = LN_function_cfg(
      bit_int = 8,
      bit_frac = 12
    )

    val fixedPointInputs = inputFloats.map { x =>
      (x * Math.pow(2, cfg.bit_frac)).toLong
    }
    val inputQueue  = mutable.Queue(fixedPointInputs: _*)
    val outputQueue = mutable.Queue[Long]()

    SimConfig
      .withVCS(flags)
      .withFSDBWave.compile(LN_function(cfg))
      .doSim{dut =>
        dut.clockDomain.forkStimulus(period = 10)
        // 输入
        FlowDriver(dut.io.x, dut.clockDomain) { payload =>
          if (inputQueue.nonEmpty) {
            payload #= inputQueue.dequeue()
            true
          } else {
            false
          }
        }

        // 输出
        FlowMonitor(dut.io.lnx, dut.clockDomain) { payload =>
          outputQueue.enqueue(payload.toLong)
        }

        dut.clockDomain.waitActiveEdgeWhere(outputQueue.size == inputFloats.length)
        dut.clockDomain.waitRisingEdge(10)
      }
      val fixedPointOutputs = outputQueue.map(x => x.toDouble / Math.pow(2, cfg.bit_frac)).toArray

    fixedPointOutputs.map(_.toFloat)
  }
}



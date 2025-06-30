package projectname

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

object StreamDemoTopSim {
  def runSim(
              inputs: java.util.List[java.util.List[java.lang.Short]]
            ): scala.collection.Seq[
    (scala.collection.Seq[scala.collection.Seq[BigInt]],
      scala.collection.Seq[scala.collection.Seq[BigInt]])
  ] = {

    val groupedInputs = inputs.asScala.grouped(4).toList.map { mat =>
      mat.map(_.asScala.map(v => BigInt(v.shortValue())))
    }

    val results = ListBuffer.empty[
      (scala.collection.Seq[scala.collection.Seq[BigInt]],
        scala.collection.Seq[scala.collection.Seq[BigInt]])
    ]

    val compiled = SimConfig
      .withVCS()
      .withFSDBWave
      .withConfig(SpinalConfig(defaultClockDomainFrequency = FixedFrequency(100 MHz)))
      .compile(new StreamDemoTop())

    compiled.doSim { dut =>
      dut.clockDomain.forkStimulus(10)
      SimTimeout(10000)

      val inputVecs = groupedInputs.iterator

      StreamDriver(dut.io.inStream, dut.clockDomain) { payload =>
        if (!inputVecs.hasNext) false
        else {
          val mat = inputVecs.next()
          for (i <- 0 until 4; j <- 0 until 4) {
            payload(i)(j) #= mat(i)(j)
          }
          true
        }
      }

      StreamReadyRandomizer(dut.io.outStream, dut.clockDomain)

      StreamMonitor(dut.io.outStream, dut.clockDomain) { payload =>
        val outMatrix = (0 until 4).map { i =>
          (0 until 4).map { j => payload(i)(j).toBigInt }
        }
        val inputIdx = results.length
        val inMatrix = if (inputIdx < groupedInputs.length) groupedInputs(inputIdx) else Seq.fill(4)(Seq.fill(4)(BigInt(0)))
        results.append((inMatrix, outMatrix))
      }

      while (results.length < groupedInputs.length) {
        dut.clockDomain.waitSampling()
      }
    }

    results.toSeq
  }
}

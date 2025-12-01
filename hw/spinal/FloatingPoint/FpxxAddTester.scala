package FloatingPoint

import spinal.core._
import spinal.core.formal._
import spinal.core.sim._
import spinal.lib._
import FpxxTesterSupport._
import spinal.sim.VCSFlags
import spinal.lib.sim.ScoreboardInOrder
import java.io.File


// object FpxxAddTester {

//     case class FpxxAddDut(config: FpxxConfig) extends Component {
//         val op     = slave Flow (Vec(Fpxx(config), 2))
//         val result = master Flow (Fpxx(config))

//         val inner = new FpxxAdd(FpxxAdd.Options(config))
//         inner.io.op.valid := op.valid
//         inner.io.op.a     := op.payload(0)
//         inner.io.op.b     := op.payload(1)

//         val addDelay = LatencyAnalysis(inner.io.op.valid, inner.io.result.valid)

//         val equiv = new Area {
//             val toFix = List.tabulate(2) { i =>
//                 val conv = new Fpxx2AFix(
//                   (result.payload.exp.maxValue - config.bias + 3) bits,
//                   config.bias + config.mant_size bits,
//                   config,
//                   if (addDelay > 2) 1 else 0,
//                   true
//                 )
//                 conv.io.op << op.map(_(i))
//                 conv.io.result
//             }

//             val sum = toFix.reduce { (a, b) =>
//                 val next = cloneOf(a)
//                 next.valid      := a.valid && b.valid
//                 next.number     := (a.number + b.number).truncated
//                 next.flags.inf  := a.flags.inf || b.flags.inf
//                 next.flags.nan  := a.flags.nan || b.flags.nan
//                 next.flags.sign := a.flags.sign || b.flags.sign
//                 next
//             }

//             val toFpxx = new AFix2Fpxx(
//               result.payload.exp.maxValue - config.bias + 3 bits,
//               config.bias + config.mant_size bits,
//               config,
//               scala.math.min(addDelay, 2),
//               true
//             )

//             toFpxx.io.op.assignAllByName(sum)

//             val fixedDelay = LatencyAnalysis(op.valid, toFpxx.io.result.valid)

//             val fixRes = Delay(toFpxx.io.result, addDelay - fixedDelay)

//             when(pastValidAfterReset()) {
//                 assert(fixRes.valid === inner.io.result.valid, "Valid should be equal")
//                 when(fixRes.valid) {
//                     assert(
//                       fixRes.payload === inner.io.result.payload ||
//                           (fixRes.is_nan() || fixRes.is_infinite()) && (inner.io.result.is_nan() || inner.io.result
//                               .is_infinite()) || fixRes.is_zero() && inner.io.result.is_zero(),
//                       "Fixed and adder outputs should match"
//                     )
//                 }
//             }
//         }

//         result << inner.io.result
//     }
// }

object FpxxAdd_Gen {
  def main(args: Array[String]): Unit = {

    val config = FpxxConfig.float8_e4m3fnuz()

    SpinalConfig(
      targetDirectory = "rtl/FpxxAdd",
      oneFilePerComponent = true,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW)
    ).generateVerilog(new FpxxAdd(FpxxAdd.Options(config)))
  }
}

case class FpxxAddDut(config: FpxxConfig) extends Component {
    val op     = slave Flow (Vec(Fpxx(config), 2))
    val result = master Flow (Fpxx(config))

    val inner = new FpxxAdd(FpxxAdd.Options(config))
    inner.io.op.valid := op.valid
    inner.io.op.a     := op.payload(0)
    inner.io.op.b     := op.payload(1)

    val addDelay = LatencyAnalysis(inner.io.op.valid, inner.io.result.valid)

    val equiv = new Area {
        val toFix = List.tabulate(2) { i =>
            val conv = new Fpxx2AFix(
                (result.payload.exp.maxValue - config.bias + 3) bits,
                config.bias + config.mant_size bits,
                config,
                if (addDelay > 2) 1 else 0,
                true
            )
            conv.io.op << op.map(_(i))
            conv.io.result
        }

        val sum = toFix.reduce { (a, b) =>
            val next = cloneOf(a)
            next.valid      := a.valid && b.valid
            next.number     := (a.number + b.number).truncated
            next.flags.inf  := a.flags.inf || b.flags.inf
            next.flags.nan  := a.flags.nan || b.flags.nan
            next.flags.sign := a.flags.sign || b.flags.sign
            next
        }

        val toFpxx = new AFix2Fpxx(
            result.payload.exp.maxValue - config.bias + 3 bits,
            config.bias + config.mant_size bits,
            config,
            scala.math.min(addDelay, 2),
            true
        )

        toFpxx.io.op.assignAllByName(sum)

        val fixedDelay = LatencyAnalysis(op.valid, toFpxx.io.result.valid)

        val fixRes = Delay(toFpxx.io.result, addDelay - fixedDelay)

        when(pastValidAfterReset()) {
            assert(fixRes.valid === inner.io.result.valid, "Valid should be equal")
            when(fixRes.valid) {
                assert(
                    fixRes.payload === inner.io.result.payload ||
                        (fixRes.is_nan() || fixRes.is_infinite()) && (inner.io.result.is_nan() || inner.io.result
                            .is_infinite()) || fixRes.is_zero() && inner.io.result.is_zero(),
                    "Fixed and adder outputs should match"
                )
            }
        }
    }

    result << inner.io.result
}


object FpxxAddTester extends App {

    val config = FpxxConfig.float16() //If you want float32, change this.
    val FileDir = "rtl/FpxxAddTester"
    new File(FileDir).mkdirs()
//    val op     = slave Flow (Vec(Fpxx(config), 2))
//    val result = master Flow (Fpxx(config))

    val flag = VCSFlags (
    compileFlags = List("-kdb","-lca", "+notimingchecks"),
    elaborateFlags = List("-fgp", "-kdb", "-lca","+rad", "+notimingchecks"),
    runFlags = List("-l ./run.log")
    )

    val Spinalcfg=SpinalConfig (
    targetDirectory = FileDir,
    oneFilePerComponent = true,
    //defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
    bitVectorWidthMax = 20000, //disable internal bigvector limitation"Way too big signal Bits"
    )
    
    val compiled = SimConfig
        .withVCS(flag)
        .withVcdWave
        .withTimeScale(1 ns)
        .withTimePrecision(1 ns)
        .withConfig(Spinalcfg)
        .allOptimisation
        .compile(FpxxAddDut(config))
            
        compiled.doSim { dut =>
                SimTimeout(100000)
                val stimuli = parseHexCases(testfloatGen(Seq("f16_add")), 2, config, config, false)
                    // No denormals
                    .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }
                testOperation(stimuli, dut.op, dut.result, dut.clockDomain)
        }
    }

//     test("add float32") {
//         val config = FpxxConfig.float32()

//         SimConfig.withIVerilog.withWave.noOptimisation
//             .compile(BundleDebug.fpxxDebugBits(FpxxAddTester.FpxxAddDut(config)))
//             .doSim { dut =>
//                 SimTimeout(100000)
//                 val stimuli = parseHexCases(testfloatGen(Seq("f32_add")), 2, config, config, false)
//                     // No denormals
//                     .filter { a => !a._2.isDenormal && !a._1.map(_.isDenormal).reduce(_ || _) }
//                 testOperation(stimuli, dut.op, dut.result, dut.clockDomain)           
//     }

// }

package Cordic

import spinal.core._
import spinal.lib._
import spinal.lib.pipeline._

/**
 * cordicDoubleRate rotate the angle twice in one cycle.
 * @param cfg
 */
case class cordicDoubleRate(cfg: cordicConfig) extends Component {
  import cfg._

  val io = new Bundle {
    val xin = slave(Stream(SInt(xinFrac+2 bits)))
    val csOut = master(Stream(Vec(SInt(xoutFrac+2 bits), 2))) // 0 -> sin, 1 -> cos
  }
  noIoPrefix()

  def pow2(x: Double) = Math.pow(2, x)

  // generate the basic rotation list
  assert(rotate % 2 == 0, s"The rotation should be even number.")
  val theta = (0 until rotate).map(i => Math.round(Math.atan(1 / pow2(i)) / Math.PI * pow2(xinFrac).toInt))
  if (theta.last == 0) {
    SpinalInfo("Rotate setting warning -> last theat is zero")
    SpinalInfo("Theat is -> " + theta)
  }
  val k = (0 until rotate).map(i => 1 / Math.sqrt(1 + pow2(-2 * i))).product * pow2(xoutFrac)

  val pipe = new Pipeline{
    val stagePre = new Stage()
    val stages = List.fill(rotate/2)(new Stage())
    connect(stagePre, stages(0))(Connection.M2S())
    for (i <- 0 until rotate/2-1){
      connect(stages(i), stages(i+1))(Connection.M2S())
    }
    val sin_, cos_, angleRm = Stageable(SInt(xoutFrac+2 bits))
    val sgnC, sgnS = Stageable(Bool())

    // stagePre
    val xinRt = Stageable(UInt(xinFrac + 2 bits))
    stagePre.driveFrom(io.xin)
    when(io.xin.payload(xinFrac - 1)){
      stagePre(xinRt) := (~(io.xin.payload(xinFrac -1 downto 0)).asUInt +^ U(1)).resized
    } otherwise {
      stagePre(xinRt) := io.xin.payload(xinFrac -1 downto 0).asUInt.resize(xinFrac + 2)
    }
    switch(io.xin.payload(xinFrac downto xinFrac - 1).asUInt){
      is(U(0)) {
        stagePre(sgnC) := False
        stagePre(sgnS) := False
      }
      is(U(1)) {
        stagePre(sgnC) := True
        stagePre(sgnS) := False
      }
      is(U(2)) {
        stagePre(sgnC) := True
        stagePre(sgnS) := True
      }
      default {
        stagePre(sgnC) := False
        stagePre(sgnS) := True
      }
    }


    for (i <- 0 until rotate/2) {
      if ( i== 0 ) {
        when(stages(0)(xinRt).asSInt >= 0){
          stages(i)(cos_) := k.toInt
          stages(i)(sin_) := k.toInt
          stages(i)(angleRm) := stages(0)(xinRt).asSInt - theta(i).toInt
        } otherwise {
          stages(i)(cos_) := k.toInt
          stages(i)(sin_) := -k.toInt
          stages(i)(angleRm) := stages(0)(xinRt).asSInt + theta(i).toInt
        }
      } else {
        when(!stages(i)(angleRm).msb){ // angleRm >= 0
          when(!(stages(i)(angleRm) - theta(2*i-1)).msb){
            stages(i).overloaded(cos_) := stages(i)(cos_) - (stages(i)(sin_) >> (2*i-1)) - ((stages(i)(sin_) + (stages(i)(cos_) >> (2*i-1))) >> (2*i))
            stages(i).overloaded(sin_) := stages(i)(sin_) + (stages(i)(cos_) >> (2*i-1)) + ((stages(i)(cos_) - (stages(i)(sin_) >> (2*i-1))) >> (2*i))
            stages(i).overloaded(angleRm) := stages(i)(angleRm) - theta(2*i-1) - theta(2*i)
          } otherwise {
            stages(i).overloaded(cos_) := stages(i)(cos_) - (stages(i)(sin_) >> (2*i-1)) + ((stages(i)(sin_) + (stages(i)(cos_) >> (2*i-1))) >> (2*i))
            stages(i).overloaded(sin_) := stages(i)(sin_) + (stages(i)(cos_) >> (2*i-1)) - ((stages(i)(cos_) - (stages(i)(sin_) >> (2*i-1))) >> (2*i))
            stages(i).overloaded(angleRm) := stages(i)(angleRm) - theta(2*i-1) + theta(2*i)
          }
        } otherwise{
          when(!(stages(i)(angleRm) + theta(2 * i - 1)).msb) {
            stages(i).overloaded(cos_) := stages(i)(cos_) + (stages(i)(sin_) >> (2 * i - 1)) - ((stages(i)(sin_) - (stages(i)(cos_) >> (2 * i - 1))) >> (2 * i))
            stages(i).overloaded(sin_) := stages(i)(sin_) - (stages(i)(cos_) >> (2 * i - 1)) + ((stages(i)(cos_) + (stages(i)(sin_) >> (2 * i - 1))) >> (2 * i))
            stages(i).overloaded(angleRm) := stages(i)(angleRm) + theta(2 * i - 1) - theta(2 * i)
          } otherwise {
            stages(i).overloaded(cos_) := stages(i)(cos_) + (stages(i)(sin_) >> (2 * i - 1)) + ((stages(i)(sin_) - (stages(i)(cos_) >> (2 * i - 1))) >> (2 * i))
            stages(i).overloaded(sin_) := stages(i)(sin_) - (stages(i)(cos_) >> (2 * i - 1)) - ((stages(i)(cos_) + (stages(i)(sin_) >> (2 * i - 1))) >> (2 * i))
            stages(i).overloaded(angleRm) := stages(i)(angleRm) + theta(2 * i - 1) + theta(2 * i)
          }
        }
      }
    }

    val stageLast = new Stage()
    connect(stages.last, stageLast)(Connection.M2S())
    stageLast.internals.output.ready = Bool()
    stageLast.isReady
    io.csOut.valid := stageLast.internals.output.valid
    io.csOut.payload(0) := stageLast(sgnS) ? -stageLast(sin_) | stageLast(sin_)
    io.csOut.payload(1) := stageLast(sgnC) ? -stageLast(cos_) | stageLast(cos_)
    stageLast.internals.output.ready := io.csOut.ready
    stageLast.internals.arbitration.propagateReady = true

    build()
  }

}

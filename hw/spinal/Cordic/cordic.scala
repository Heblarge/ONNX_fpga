package Cordic

import spinal.core._
import spinal.lib._

case class cordicConfig (
                        xinFrac  : Int,
                        xoutFrac : Int,
                        rotate : Int
                        )

/**
 * cordic is a CORDIC module that read in an SInt data and compute the corresponding sine and cosine result.
 *
 * Notice:
 *
 * * This cordic can only compute the xin within [-2, 2). The result is sin(xin*pi) and cos(xin*pi)
 *
 * * xin -> [-2, 2) * 2 ** (xinFrac)
 *
 * * The rotation amount is based on rotate parameter.
 *
 * The cordic computation is divided into two steps:
 *  1. get the quadrant. (bit-0 and bit-1 is used to define the quadrant, bit-1 is the bit after fraction point):
 *    |---bit-0---|---bit-1---|-quadrant-|-sgn cos-|-sgn sin-|
 *    |     0     |     0     |     1    |    +    |    +    |
 *    |     0     |     1     |     2    |    -    |    +    |
 *    |     1     |     0     |     3    |    -    |    -    |
 *    |     1     |     1     |     4    |    +    |    -    |
 *  2. rotate the xin into (0, 1/2) which is corresponding to (0, 1/2*pi)
 *    xin change based on bit-1: 0 -> un-change, 1-> minus xin => (~xin+1)
 */
case class cordic(cfg: cordicConfig) extends Component {
  import cfg._

  val io = new Bundle {
    val xin = slave(Flow(SInt(xinFrac+2 bits)))  // plus 2 bits are the bit_sgb and bit-0
    val cosOut = master(Flow(SInt(xoutFrac+2 bits)))
    val sinOut = master(Flow(SInt(xoutFrac+2 bits)))
  }
  noIoPrefix()


  def pow2(x: Double) = Math.pow(2, x)

  // generate the basic rotation list as reference
  val theta = (0 until rotate).map(i => Math.round(Math.atan(1 / pow2(i)) / Math.PI * pow2(xinFrac)))//根据rotate次数先生成一个数列，然后将这个数列映射到tan^-1(1/2^(n))然后缩放(pi/2^(xinFrac))倍
  if (theta.last == 0) {
    SpinalInfo("Rotate setting warning -> last theta is zero")
    SpinalInfo("Theta is -> " + theta)
  }
  val k = (0 until rotate).map(i => 1 / Math.sqrt(1 + pow2(-2 * i))).product * pow2(xoutFrac)


  // 1. get the quadrant:
  // 1. 判断象限
  val validVec = Vec(Reg(Bool()), rotate + 1)
  validVec.map(_.init(False))
  val sgnSVec = Vec(Reg(Bool()), rotate + 1)
  val sgnCVec = Vec(Reg(Bool()), rotate + 1)
  validVec(0) := io.xin.valid

  when(io.xin.valid) {
    switch(io.xin.payload(xinFrac downto xinFrac - 1).asUInt) {
      is(U(0)) {
        sgnCVec(0) := False
        sgnSVec(0) := False
      }
      is(U(1)) {
        sgnCVec(0) := True
        sgnSVec(0) := False
      }
      is(U(2)) {
        sgnCVec(0) := True
        sgnSVec(0) := True
      }
      default {
        sgnCVec(0) := False
        sgnSVec(0) := True
      }
    }
  }

  validVec.reduceLeft((a, b) => {
    b := a; b
  })
  sgnCVec.reduceLeft((a, b) => {
    b := a; b
  })
  sgnSVec.reduceLeft((a, b) => {
    b := a; b
  })

  // 2. get the rotate radix within (0, 1/2)
  val xinRt = Reg(UInt(xinFrac + 2 bits))
  when(io.xin.valid) {
    when(io.xin.payload(xinFrac - 1)) {
      xinRt := (~(io.xin.payload(xinFrac - 1 downto 0)).asUInt +^ U(1)).resized
    } otherwise {
      xinRt := io.xin.payload(xinFrac - 1 downto 0).asUInt.resize(xinFrac + 2)
    }
  }


  // 3. rotate and generate sin&cos data
  val sinVec = Vec(Reg(SInt(xoutFrac + 2 bits)), rotate)
  val cosVec = Vec(Reg(SInt(xoutFrac + 2 bits)), rotate)
  val angleRm = Vec(Reg(SInt(xinFrac + 2 bits)), rotate)

  for (i <- 0 until rotate) {
    if (i == 0) {
      cosVec(i) := k.toInt
      sinVec(i) := 0
      angleRm(i) := xinRt.asSInt
    } else {
      when(!angleRm(i - 1).msb) {
        cosVec(i) := cosVec(i - 1) - (sinVec(i - 1) >> (i - 1))
        sinVec(i) := sinVec(i - 1) + (cosVec(i - 1) >> (i - 1))
        angleRm(i) := angleRm(i - 1) - theta(i - 1).toInt
      }.otherwise {
        cosVec(i) := cosVec(i - 1) + (sinVec(i - 1) >> (i - 1))
        sinVec(i) := sinVec(i - 1) - (cosVec(i - 1) >> (i - 1))
        angleRm(i) := angleRm(i - 1) + theta(i - 1).toInt
      }
    }
  }

  val OutVd = Reg(Bool()) init False
  OutVd := validVec.last
  val sinOut = Reg(SInt(xoutFrac + 2 bits))
  val cosOut = Reg(SInt(xoutFrac + 2 bits))

  when(validVec.last) {
    sinOut := sgnSVec.last ? -(sinVec.last) | sinVec.last
    cosOut := sgnCVec.last ? -(cosVec.last) | cosVec.last
  }

  io.sinOut.valid := OutVd
  io.sinOut.payload := sinOut

  io.cosOut.valid := OutVd
  io.cosOut.payload := cosOut


}

object cordic {
  def apply(xinfrac: Int, xoutfrac: Int, rotate: Int): cordic = {
    val cfg = cordicConfig(xinFrac = xinfrac, xoutFrac = xoutfrac, rotate = rotate)
    val cordic_ = cordic(cfg)
    cordic_
  }
}
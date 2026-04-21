package FloatingPoint

import spinal.core._
import spinal.lib._
import scala.collection.mutable

case class AttentionExpConfig(
    expLutFracBits: Int = 4,
    expLutMin: Double = -8.0
) {
    require(expLutFracBits > 0)
    val expLutStepsPerUnit: Int = 1 << expLutFracBits
    val expLutMaxIndex: Int = (-expLutMin * expLutStepsPerUnit).toInt
}

object AttentionOps {
    private case class EncodedValue(bits: Int, value: Double)
    private val quantLutCache = mutable.HashMap[FpxxConfig, Seq[EncodedValue]]()

    def mulConfigFor(c: FpxxConfig): FpxxConfig = {
        if (c == FpxxConfig.float16()) {
            FpxxConfig.float16_mul()
        } else if (c == FpxxConfig.float8_e5m2fnuz()) {
            FpxxConfig.float8_e5m2mul()
        } else if (c == FpxxConfig.float8_e4m3fnuz()) {
            FpxxConfig.float8_e4m3mul()
        } else {
            c
        }
    }

    def delayWhenValid[T <: Data](that: T, cycles: Int, valid: Bool): T = {
        require(cycles >= 0)
        if (cycles == 0) {
            that
        } else {
            var stageData = cloneOf(that)
            stageData := that
            var stageValid = valid
            for (_ <- 0 until cycles) {
                val nextData = Reg(cloneOf(that))
                when(stageValid) {
                    nextData := stageData
                }
                stageData = nextData
                stageValid = RegNext(stageValid) init (False)
            }
            stageData
        }
    }

    def delayBoolWhenValid(that: Bool, cycles: Int, valid: Bool): Bool = {
        delayWhenValid(that, cycles, valid)
    }

    private def decodeBitsToDouble(bits: Int, c: FpxxConfig): Double = {
        val expMask = (1 << c.exp_size) - 1
        val mantMask = (1 << c.mant_size) - 1
        val sign = (bits >> (c.exp_size + c.mant_size)) & 0x1
        val exp = (bits >> c.mant_size) & expMask
        val mant = bits & mantMask
        val signFactor = if (sign == 1) -1.0 else 1.0
        val frac = mant.toDouble / (1 << c.mant_size).toDouble

        c.nan_encoding match {
            case SpecialNan(encoding) if BigInt(bits) == encoding => return Double.NaN
            case _ =>
        }

        if (c.ieee_like && exp == expMask) {
            if (mant == 0) return if (sign == 1) Double.NegativeInfinity else Double.PositiveInfinity
            return Double.NaN
        }

        if (exp == 0) {
            if (mant == 0) {
                0.0
            } else {
                signFactor * scala.math.pow(2.0, 1 - c.bias) * frac
            }
        } else {
            signFactor * scala.math.pow(2.0, exp - c.bias) * (1.0 + frac)
        }
    }

    private def nearestBitsFor(value: Double, c: FpxxConfig): Int = {
        require(c.full_size <= 16, s"Nearest-quantization fallback is only intended for tiny formats, got full_size=${c.full_size}")
        val lut = quantLutCache.getOrElseUpdate(c, {
            val total = 1 << c.full_size
            (0 until total).map { bits =>
                EncodedValue(bits, decodeBitsToDouble(bits, c))
            }
        })

        val valid = lut.filterNot(_.value.isNaN)
        val clamped =
            if (value.isNaN) Double.NaN
            else if (value == Double.PositiveInfinity) Double.MaxValue
            else if (value == Double.NegativeInfinity) -Double.MaxValue
            else value

        if (clamped.isNaN) {
            c.nan_encoding match {
                case SpecialNan(encoding) => return encoding.toInt
                case IEEENan() =>
                    val maxExp = (1 << c.exp_size) - 1
                    return (maxExp << c.mant_size) | 1
            }
        }

        val minDist = valid.map(v => scala.math.abs(v.value - clamped)).min
        val ties = valid.filter(v => scala.math.abs(scala.math.abs(v.value - clamped) - minDist) < 1e-12)
        ties.find(v => (v.bits & 1) == 0).getOrElse(ties.head).bits
    }

    def fpxxHostFromDouble(value: Double, c: FpxxConfig): FpxxHost = {
        if (!c.ieee_like && c.full_size <= 16) {
            FpxxHost(BigInt(nearestBitsFor(value, c)), c)
        } else {
            require(c.ieee_like, s"Unsupported LUT-constant path for config exp=${c.exp_size}, mant=${c.mant_size}")

            val sign = if (java.lang.Double.doubleToRawLongBits(value) < 0) 1 else 0
            val maxExp = (1 << c.exp_size) - 1

            if (value.isNaN) {
                val mant = BigInt(1) << (c.mant_size - 1)
                val bits = (BigInt(sign) << (c.exp_size + c.mant_size)) | (BigInt(maxExp) << c.mant_size) | mant
                FpxxHost(bits, c)
            } else if (value.isInfinity) {
                val bits = (BigInt(sign) << (c.exp_size + c.mant_size)) | (BigInt(maxExp) << c.mant_size)
                FpxxHost(bits, c)
            } else if (value == 0.0) {
                FpxxHost(BigInt(0), c)
            } else {
                val abs = scala.math.abs(value)
                val expUnbiased = scala.math.floor(scala.math.log(abs) / scala.math.log(2.0)).toInt
                val normalized = abs / scala.math.pow(2.0, expUnbiased)
                var biasedExp = expUnbiased + c.bias

                if (biasedExp <= 0) {
                    FpxxHost(BigInt(0), c)
                } else if (biasedExp >= maxExp) {
                    val bits = (BigInt(sign) << (c.exp_size + c.mant_size)) | (BigInt(maxExp) << c.mant_size)
                    FpxxHost(bits, c)
                } else {
                    val scale = BigInt(1) << c.mant_size
                    var mant = BigInt(scala.math.round((normalized - 1.0) * scale.toDouble))
                    if (mant == scale) {
                        mant = BigInt(0)
                        biasedExp += 1
                    }
                    if (biasedExp >= maxExp) {
                        val bits = (BigInt(sign) << (c.exp_size + c.mant_size)) | (BigInt(maxExp) << c.mant_size)
                        FpxxHost(bits, c)
                    } else {
                        val bits = (BigInt(sign) << (c.exp_size + c.mant_size)) |
                          (BigInt(biasedExp) << c.mant_size) |
                          mant
                        FpxxHost(bits, c)
                    }
                }
            }
        }
    }

    def fpxxConst(value: Double, c: FpxxConfig): Fpxx = {
        val host = fpxxHostFromDouble(value, c)
        val ret = Fpxx(c)
        ret.sign := (if (host.sign == 0) False else True)
        ret.exp := host.exp
        ret.mant := host.mant
        ret
    }

    def formatName(c: FpxxConfig): String = {
        if (c == FpxxConfig.float32()) {
            "float32"
        } else if (c == FpxxConfig.float16()) {
            "float16"
        } else if (c == FpxxConfig.bfloat16()) {
            "bfloat16"
        } else if (c == FpxxConfig.float8_e4m3fnuz()) {
            "float8_e4m3fnuz"
        } else if (c == FpxxConfig.float8_e5m2fnuz()) {
            "float8_e5m2fnuz"
        } else if (c == FpxxConfig.float16_mul()) {
            "float16_mul"
        } else if (c == FpxxConfig.float8_e4m3mul()) {
            "float8_e4m3mul"
        } else if (c == FpxxConfig.float8_e5m2mul()) {
            "float8_e5m2mul"
        } else {
            s"exp${c.exp_size}_mant${c.mant_size}"
        }
    }
}

object FpxxCompare {
    def lt(a: Fpxx, b: Fpxx): Bool = {
        val aZero = a.is_zero()
        val bZero = b.is_zero()
        val magLt = (a.exp < b.exp) || ((a.exp === b.exp) && (a.mant < b.mant))
        val magGt = (a.exp > b.exp) || ((a.exp === b.exp) && (a.mant > b.mant))
        val result = Bool()
        result := False
        when(aZero && bZero) {
            result := False
        } elsewhen(a.sign =/= b.sign) {
            result := a.sign && !aZero
        } elsewhen(!a.sign) {
            result := magLt
        } otherwise {
            result := magGt
        }
        result
    }

    def lte(a: Fpxx, b: Fpxx): Bool = !lt(b, a)

    def maxOf(values: Seq[Fpxx]): Fpxx = {
        values.tail.foldLeft(values.head) { (acc, v) =>
            val next = Fpxx(acc.c)
            when(lt(acc, v)) {
                next := v
            } otherwise {
                next := acc
            }
            next
        }
    }
}

class FpxxSubCompatible(o: FpxxAdd.Options) extends Component {
    val io = new Bundle {
        val op = slave Flow(new Bundle {
            val a = Fpxx(o.c)
            val b = Fpxx(o.c)
        })
        val result = master Flow(Fpxx(o.c))
    }

    val opB = Fpxx(o.c)
    opB.sign := !io.op.b.sign
    opB.exp := io.op.b.exp
    opB.mant := io.op.b.mant

    val adder = new FpxxAddCompatible(o)
    adder.io.op.valid := io.op.valid
    adder.io.op.a := io.op.a
    adder.io.op.b := opB
    io.result <> adder.io.result
}

class FpxxDivCompatible(c: FpxxConfig, pipeStages: Int = 2) extends Component {

    val io = new Bundle {
        val input = slave Flow(new Bundle {
            val num = Fpxx(c)
            val den = Fpxx(c)
        })
        val result = master Flow(Fpxx(c))
    }

    val isEvenMant = (c.mant_size & 1) == 0

    if (isEvenMant) {
        val div = new FpxxDivEvenStreams(c, FpxxDivEvenConfig(pipeStages = pipeStages))
        div.io.input.valid := io.input.valid
        div.io.input.payload.a := io.input.payload.num
        div.io.input.payload.b := io.input.payload.den
        div.io.result.ready := True
        io.result.valid := div.io.result.valid
        io.result.payload := div.io.result.payload
    } else {
        val div = new FpxxDivStreams(c, FpxxDivConfig(pipeStages = pipeStages))
        div.io.input.valid := io.input.valid
        div.io.input.payload.a := io.input.payload.num
        div.io.input.payload.b := io.input.payload.den
        div.io.output.ready := True
        io.result.valid := div.io.output.valid
        io.result.payload := div.io.output.payload
    }
}

class FpxxExpNegLut(c: FpxxConfig, cfg: AttentionExpConfig, pipeStages: Int = 1) extends Component {
    private val lutSize = cfg.expLutMaxIndex + 1

    val io = new Bundle {
        val op = slave Flow(Fpxx(c))
        val result = master Flow(Fpxx(c))
    }

    val thresholds = Vec((0 to cfg.expLutMaxIndex).map(i => AttentionOps.fpxxConst(-i.toDouble / cfg.expLutStepsPerUnit.toDouble, c)))
    val values = Vec((0 to cfg.expLutMaxIndex).map(i => AttentionOps.fpxxConst(scala.math.exp(-i.toDouble / cfg.expLutStepsPerUnit.toDouble), c)))

    val idx = UInt(log2Up(lutSize) bits)
    idx := cfg.expLutMaxIndex
    for (i <- 0 to cfg.expLutMaxIndex) {
        when(FpxxCompare.lte(io.op.payload, thresholds(i))) {
            idx := i
        }
    }
    when(!FpxxCompare.lt(io.op.payload, thresholds(0))) {
        idx := 0
    }

    val outPayload = Reg(Fpxx(c))
    val outValid = Reg(Bool()) init (False)

    when(io.op.valid) {
        outPayload := values(idx)
    }
    outValid := RegNext(io.op.valid) init (False)

    io.result.valid := outValid
    io.result.payload := outPayload
}

class FpxxAddChain(values: Seq[Flow[Fpxx]], c: FpxxConfig) extends Area {
    require(values.nonEmpty)

    private val addLatencyRef = new FpxxAddCompatible(FpxxAdd.Options(c = c, pipeStages = 1))
    addLatencyRef.io.op.valid := False
    addLatencyRef.io.op.a.set_zero()
    addLatencyRef.io.op.b.set_zero()
    private val addLatency = LatencyAnalysis(addLatencyRef.io.op.valid, addLatencyRef.io.result.valid)

    var accPayload = values.head.payload
    var accValid = values.head.valid
    var delayCycles = 0

    values.tail.foreach { v =>
        val adder = new FpxxAddCompatible(FpxxAdd.Options(c = c, pipeStages = 1))
        adder.io.op.valid := accValid
        adder.io.op.a := accPayload
        adder.io.op.b := AttentionOps.delayWhenValid(v.payload, delayCycles, v.valid)
        accPayload = adder.io.result.payload
        accValid = adder.io.result.valid
        delayCycles += addLatency
    }

    val result = Flow(Fpxx(c))
    result.valid := accValid
    result.payload := accPayload
}

class FpxxDotProduct(vectorSize: Int, c: FpxxConfig) extends Component {
    require(vectorSize > 0)
    private val mulCfg = AttentionOps.mulConfigFor(c)

    val io = new Bundle {
        val input = slave Flow(new Bundle {
            val a = Vec.fill(vectorSize)(Fpxx(c))
            val b = Vec.fill(vectorSize)(Fpxx(c))
        })
        val result = master Flow(Fpxx(mulCfg))
    }

    val muls = Array.fill(vectorSize)(new FpxxMulCompatible(FpxxMul.Options(cIn = c, cOut = Some(mulCfg), pipeStages = 1)))
    for (i <- 0 until vectorSize) {
        muls(i).io.input.valid := io.input.valid
        muls(i).io.input.payload.a := io.input.payload.a(i)
        muls(i).io.input.payload.b := io.input.payload.b(i)
    }

    val chain = new FpxxAddChain(muls.map(_.io.result), mulCfg)
    io.result <> chain.result
}

class FpxxOnlineSoftmax(tileSize: Int, c: FpxxConfig, cfg: AttentionExpConfig = AttentionExpConfig()) extends Component {
    require(tileSize > 0)

    val io = new Bundle {
        val input = slave Flow(new Bundle {
            val scores = Vec.fill(tileSize)(Fpxx(c))
            val prevMax = Fpxx(c)
            val prevSum = Fpxx(c)
            val init = Bool()
        })
        val output = master Flow(new Bundle {
            val expScores = Vec.fill(tileSize)(Fpxx(c))
            val normScores = Vec.fill(tileSize)(Fpxx(c))
            val newMax = Fpxx(c)
            val prevScale = Fpxx(c)
            val newSum = Fpxx(c)
        })
    }

    val blockMax = FpxxCompare.maxOf(io.input.payload.scores.toSeq)
    val newMaxComb = Fpxx(c)
    newMaxComb := blockMax
    when(!io.input.payload.init && FpxxCompare.lt(blockMax, io.input.payload.prevMax)) {
        newMaxComb := io.input.payload.prevMax
    }

    val subLatencyRef = new FpxxSubCompatible(FpxxAdd.Options(c = c, pipeStages = 1))
    val zeroC = AttentionOps.fpxxConst(0.0, c)
    subLatencyRef.io.op.valid := False
    subLatencyRef.io.op.a := zeroC
    subLatencyRef.io.op.b := zeroC
    val subLatency = LatencyAnalysis(subLatencyRef.io.op.valid, subLatencyRef.io.result.valid)

    val expUnits = Array.fill(tileSize)(new FpxxExpNegLut(c, cfg, pipeStages = 1))
    val subUnits = Array.fill(tileSize)(new FpxxSubCompatible(FpxxAdd.Options(c = c, pipeStages = 1)))
    for (i <- 0 until tileSize) {
        subUnits(i).io.op.valid := io.input.valid
        subUnits(i).io.op.a := io.input.payload.scores(i)
        subUnits(i).io.op.b := newMaxComb
        expUnits(i).io.op <> subUnits(i).io.result
    }

    val prevScaleSub = new FpxxSubCompatible(FpxxAdd.Options(c = c, pipeStages = 1))
    prevScaleSub.io.op.valid := io.input.valid
    prevScaleSub.io.op.a := io.input.payload.prevMax
    prevScaleSub.io.op.b := newMaxComb
    val prevScaleExp = new FpxxExpNegLut(c, cfg, pipeStages = 1)
    prevScaleExp.io.op <> prevScaleSub.io.result

    val mulCfg = AttentionOps.mulConfigFor(c)
    val prevMul = new FpxxMulCompatible(FpxxMul.Options(cIn = c, cOut = Some(mulCfg), pipeStages = 1))
    prevMul.io.input.valid := prevScaleExp.io.result.valid
    prevMul.io.input.payload.a := AttentionOps.delayWhenValid(io.input.payload.prevSum, 2 + subLatency, io.input.valid)
    prevMul.io.input.payload.b := prevScaleExp.io.result.payload

    val expSumChain = new FpxxAddChain(expUnits.map(_.io.result), c)

    val zeroVal = AttentionOps.fpxxConst(0.0, c)
    val delayedInit = AttentionOps.delayBoolWhenValid(io.input.payload.init, 2 + subLatency, io.input.valid)

    val sumAdder = new FpxxAddCompatible(FpxxAdd.Options(c = c, pipeStages = 1))
    sumAdder.io.op.valid := expSumChain.result.valid
    sumAdder.io.op.a := expSumChain.result.payload
    sumAdder.io.op.b := prevMul.io.result.payload

    val prevScaleAligned = AttentionOps.delayWhenValid(prevScaleExp.io.result.payload, 1, prevScaleExp.io.result.valid)
    val newMaxAligned = AttentionOps.delayWhenValid(newMaxComb, 2 + subLatency + 1, io.input.valid)
    val expAligned = expUnits.map(u => AttentionOps.delayWhenValid(u.io.result.payload, 1, u.io.result.valid))
    val newSumFinal = Fpxx(c)
    newSumFinal := sumAdder.io.result.payload
    when(delayedInit) {
        newSumFinal := expSumChain.result.payload
    }
    val prevScaleFinal = Fpxx(c)
    prevScaleFinal := prevScaleAligned
    when(delayedInit) {
        prevScaleFinal := zeroVal
    }

    val normDivLatencyRef = new FpxxDivCompatible(c, pipeStages = 2)
    normDivLatencyRef.io.input.valid := False
    normDivLatencyRef.io.input.payload.num := zeroVal
    normDivLatencyRef.io.input.payload.den := zeroVal
    val normDivLatency = LatencyAnalysis(normDivLatencyRef.io.input.valid, normDivLatencyRef.io.result.valid)

    val normDivs = Array.fill(tileSize)(new FpxxDivCompatible(c, pipeStages = 2))
    for (i <- 0 until tileSize) {
        normDivs(i).io.input.valid := sumAdder.io.result.valid
        normDivs(i).io.input.payload.num := expAligned(i)
        normDivs(i).io.input.payload.den := newSumFinal
    }

    io.output.valid := normDivs(0).io.result.valid
    for (i <- 0 until tileSize) {
        io.output.payload.expScores(i) := AttentionOps.delayWhenValid(expAligned(i), normDivLatency, sumAdder.io.result.valid)
        io.output.payload.normScores(i) := normDivs(i).io.result.payload
    }
    io.output.payload.newMax := AttentionOps.delayWhenValid(newMaxAligned, normDivLatency, sumAdder.io.result.valid)
    io.output.payload.prevScale := AttentionOps.delayWhenValid(prevScaleFinal, normDivLatency, sumAdder.io.result.valid)
    io.output.payload.newSum := AttentionOps.delayWhenValid(newSumFinal, normDivLatency, sumAdder.io.result.valid)
}

class FpxxQKV(tileSize: Int, headDim: Int, c: FpxxConfig, cfg: AttentionExpConfig = AttentionExpConfig()) extends Component {
    require(tileSize > 0)
    require(headDim > 0)

    private val scoreCfg = AttentionOps.mulConfigFor(c)
    private val postSoftmaxLatency = 3

    val io = new Bundle {
        val input = slave Flow(new Bundle {
            val q = Vec.fill(headDim)(Fpxx(c))
            val k = Vec.fill(tileSize)(Vec.fill(headDim)(Fpxx(c)))
            val v = Vec.fill(tileSize)(Vec.fill(headDim)(Fpxx(c)))
            val prevMax = Fpxx(scoreCfg)
            val prevSum = Fpxx(scoreCfg)
            val prevAcc = Vec.fill(headDim)(Fpxx(scoreCfg))
            val init = Bool()
        })
        val output = master Flow(new Bundle {
            val scores = Vec.fill(tileSize)(Fpxx(scoreCfg))
            val expScores = Vec.fill(tileSize)(Fpxx(scoreCfg))
            val normScores = Vec.fill(tileSize)(Fpxx(scoreCfg))
            val newMax = Fpxx(scoreCfg)
            val newSum = Fpxx(scoreCfg)
            val newAcc = Vec.fill(headDim)(Fpxx(scoreCfg))
            val newAccNorm = Vec.fill(headDim)(Fpxx(scoreCfg))
        })
    }

    val qk = Array.fill(tileSize)(new FpxxDotProduct(headDim, c))
    for (i <- 0 until tileSize) {
        qk(i).io.input.valid := io.input.valid
        for (d <- 0 until headDim) {
            qk(i).io.input.payload.a(d) := io.input.payload.q(d)
            qk(i).io.input.payload.b(d) := io.input.payload.k(i)(d)
        }
    }

    val softmax = new FpxxOnlineSoftmax(tileSize, scoreCfg, cfg)
    softmax.io.input.valid := qk(0).io.result.valid
    for (i <- 0 until tileSize) {
        softmax.io.input.payload.scores(i) := qk(i).io.result.payload
    }
    softmax.io.input.payload.prevMax := AttentionOps.delayWhenValid(io.input.payload.prevMax, 3, io.input.valid)
    softmax.io.input.payload.prevSum := AttentionOps.delayWhenValid(io.input.payload.prevSum, 3, io.input.valid)
    softmax.io.input.payload.init := AttentionOps.delayBoolWhenValid(io.input.payload.init, 3, io.input.valid)

    val alignLatency = LatencyAnalysis(io.input.valid, softmax.io.output.valid)

    val vConverters = Array.tabulate(tileSize, headDim) { (i, d) =>
        val conv = FpxxConverter(FpxxConverter.Options(in_config = c, out_config = scoreCfg, pipeStages = List(true, true, true, true)))
        conv.io.a.valid := io.input.valid
        conv.io.a.payload := io.input.payload.v(i)(d)
        conv
    }
    val vConvLatency = LatencyAnalysis(io.input.valid, vConverters(0)(0).io.r.valid)
    val vAlignLatency = alignLatency - vConvLatency
    require(vAlignLatency >= 0, s"vAlignLatency must be non-negative, got $vAlignLatency")
    val vAligned = Array.tabulate(tileSize, headDim) { (i, d) =>
        AttentionOps.delayWhenValid(vConverters(i)(d).io.r.payload, vAlignLatency, vConverters(i)(d).io.r.valid)
    }
    val prevAccAligned = Array.tabulate(headDim) { d =>
        AttentionOps.delayWhenValid(io.input.payload.prevAcc(d), alignLatency, io.input.valid)
    }
    val initAligned = AttentionOps.delayBoolWhenValid(io.input.payload.init, alignLatency, io.input.valid)

    val zeroScore = AttentionOps.fpxxConst(0.0, scoreCfg)

    val accOutputs = Array.fill(headDim)(Flow(Fpxx(scoreCfg)))

    for (d <- 0 until headDim) {
        val termMuls = Array.fill(tileSize)(new FpxxMulCompatible(FpxxMul.Options(cIn = scoreCfg, cOut = Some(scoreCfg), pipeStages = 1)))
        for (i <- 0 until tileSize) {
            termMuls(i).io.input.valid := softmax.io.output.valid
            termMuls(i).io.input.payload.a := softmax.io.output.payload.expScores(i)
            termMuls(i).io.input.payload.b := vAligned(i)(d)
        }

        val termSum = new FpxxAddChain(termMuls.map(_.io.result), scoreCfg)

        val prevMul = new FpxxMulCompatible(FpxxMul.Options(cIn = scoreCfg, cOut = Some(scoreCfg), pipeStages = 1))
        prevMul.io.input.valid := softmax.io.output.valid
        prevMul.io.input.payload.a := softmax.io.output.payload.prevScale
        prevMul.io.input.payload.b := prevAccAligned(d)
        when(initAligned) {
            prevMul.io.input.payload.a := zeroScore
        }

        val accAdder = new FpxxAddCompatible(FpxxAdd.Options(c = scoreCfg, pipeStages = 1))
        accAdder.io.op.valid := termSum.result.valid
        accAdder.io.op.a := termSum.result.payload
        accAdder.io.op.b := prevMul.io.result.payload
        when(initAligned) {
            accAdder.io.op.b := zeroScore
        }

        accOutputs(d).valid := accAdder.io.result.valid
        accOutputs(d).payload := accAdder.io.result.payload
    }
    val accNormLatencyRef = new FpxxDivCompatible(scoreCfg, pipeStages = 2)
    accNormLatencyRef.io.input.valid := False
    accNormLatencyRef.io.input.payload.num := zeroScore
    accNormLatencyRef.io.input.payload.den := zeroScore
    val accNormLatency = LatencyAnalysis(accNormLatencyRef.io.input.valid, accNormLatencyRef.io.result.valid)

    val newSumAligned = AttentionOps.delayWhenValid(softmax.io.output.payload.newSum, postSoftmaxLatency, softmax.io.output.valid)
    val accNormDivs = Array.fill(headDim)(new FpxxDivCompatible(scoreCfg, pipeStages = 2))
    for (d <- 0 until headDim) {
        accNormDivs(d).io.input.valid := accOutputs(d).valid
        accNormDivs(d).io.input.payload.num := accOutputs(d).payload
        accNormDivs(d).io.input.payload.den := newSumAligned
    }

    io.output.valid := accNormDivs(0).io.result.valid
    for (i <- 0 until tileSize) {
        io.output.payload.scores(i) := AttentionOps.delayWhenValid(qk(i).io.result.payload, 8 + accNormLatency, qk(i).io.result.valid)
        io.output.payload.expScores(i) := AttentionOps.delayWhenValid(softmax.io.output.payload.expScores(i), postSoftmaxLatency + accNormLatency, softmax.io.output.valid)
        io.output.payload.normScores(i) := AttentionOps.delayWhenValid(softmax.io.output.payload.normScores(i), postSoftmaxLatency + accNormLatency, softmax.io.output.valid)
    }
    io.output.payload.newMax := AttentionOps.delayWhenValid(softmax.io.output.payload.newMax, postSoftmaxLatency + accNormLatency, softmax.io.output.valid)
    io.output.payload.newSum := AttentionOps.delayWhenValid(newSumAligned, accNormLatency, accOutputs(0).valid)
    for (d <- 0 until headDim) {
        io.output.payload.newAcc(d) := AttentionOps.delayWhenValid(accOutputs(d).payload, accNormLatency, accOutputs(d).valid)
        io.output.payload.newAccNorm(d) := accNormDivs(d).io.result.payload
    }
}

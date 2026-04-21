package FloatingPoint

import spinal.core._
import spinal.lib._

/**
 * FpxxExp: Hardware module for computing exp(x) (e^x) using LUT method
 * 
 * Algorithm:
 * 1. Use identity: exp(x) = 2^(x * log2(e)) where log2(e) ≈ 1.4427
 * 2. For x = n + f where n is integer part and f is fractional part (0 <= f < 1):
 *    exp(x) = 2^((n+f) * log2(e)) = 2^(n*1.4427 + f*1.4427)
 * 
 * For genOpacity, input x is typically in range [-inf, 0] since power = -0.5 * (...)
 * exp(power) where power <= 0, so result is in [0, 1]
 * 
 * Simplified approach for Gaussian splatting:
 * - Input power is typically in range [-10, 0] (beyond -10, exp ≈ 0)
 * - Use direct LUT for exp(x) with input quantized
 */
object FpxxExp {
    import caseapp._

    val fpxxConfigHelpMsg =
        "Floating point format. May be one of (float64, float32, float16, bfloat16)."

    case class Options(
        @HelpMessage(fpxxConfigHelpMsg)
        c: FpxxConfig = FpxxConfig.float16(),
        @HelpMessage("Number of pipeline stages (0 for combinational)")
        pipeStages: Int = 0,
        @HelpMessage("Log2 number of entries in lookup table (default 8 -> 256 entries)")
        tableSizeBits: Int = 8,
        @HelpMessage("Minimum input value (more negative = smaller exp result). Default -8.0")
        minInput: Double = -8.0
    )
}

/**
 * FpxxExp Component
 * 
 * Computes exp(x) for floating point input x.
 * For Gaussian splatting, x is typically negative (power term).
 * 
 * Method: Direct LUT with linear interpolation
 * - Quantize input x to table index
 * - Look up precomputed exp(x) value
 * - Optionally interpolate between adjacent entries
 */
case class FpxxExp(o: FpxxExp.Options) extends Component {

    assert(o.c.ieee_like, "Can only handle IEEE compliant floats")
    
    val tableSize     = 1 << o.tableSizeBits
    val minInputAbs   = -o.minInput  // e.g., 8.0 for minInput=-8.0
    
    val io = new Bundle {
        val op     = slave Stream (Fpxx(o.c))
        val result = master Stream (Fpxx(o.c))
    }

    // Precompute LUT: exp(x) for x in [minInput, 0]
    // Index 0 -> x = minInput, Index tableSize-1 -> x ≈ 0
    def expTableContents = for (i <- 0 until tableSize) yield {
        // Map index to input value: x = minInput + i * (|minInput| / tableSize)
        val x = o.minInput + i.toDouble * minInputAbs / tableSize.toDouble
        val expVal = scala.math.exp(x)
        
        // Convert to Fpxx representation using float32 first then converting
        val f32Host: FpxxHost = expVal.toFloat  // Implicit conversion from Float to FpxxHost
        // Now convert float32 host to target format (float16)
        val expBiasedF32 = f32Host.exp.toInt - FpxxConfig.float32().bias + o.c.bias
        val expClamped = if (expBiasedF32 < 0) 0 else if (expBiasedF32 > ((1 << o.c.exp_size) - 1)) ((1 << o.c.exp_size) - 1) else expBiasedF32
        val mantShift = FpxxConfig.float32().mant_size - o.c.mant_size
        val mantConverted = (f32Host.mant >> mantShift).toLong
        U((f32Host.sign.toLong << (o.c.exp_size + o.c.mant_size)) | 
          (expClamped.toLong << o.c.mant_size) | 
          mantConverted, o.c.full_size bits)
    }

    val expTable = Mem(UInt(o.c.full_size bits), initialContent = expTableContents)

    //============================================================
    // Stage 0: Input processing and address generation
    //============================================================
    val op_p0 = io.op.payload
    val p0_vld = io.op.valid
    
    // Default ready
    io.op.ready := True

    // Special cases
    val op_nan_p0     = op_p0.is_nan()
    val op_inf_pos_p0 = op_p0.is_infinite() && !op_p0.sign  // +inf
    val op_inf_neg_p0 = op_p0.is_infinite() && op_p0.sign   // -inf
    val op_zero_p0    = op_p0.is_zero()

    // Convert input to fixed-point for table lookup
    // We need to map x ∈ [minInput, 0] to index ∈ [0, tableSize-1]
    // 
    // For float16: sign(1) + exp(5) + mant(10)
    // exp_val = stored_exp - 15 (bias)
    // value = (-1)^sign * 2^exp_val * 1.mant
    
    // Simplified: clamp input to [minInput, 0] and compute index
    // index = (x - minInput) / |minInput| * tableSize
    //       = (x / |minInput| + 1) * tableSize
    
    // For negative x: table index = tableSize * (1 + x/|minInput|)
    // When x = 0: index = tableSize - 1 (or clamped)
    // When x = minInput: index = 0
    
    // Extract approximate value from float16
    // For simplicity, use exp and high bits of mantissa to compute approximate magnitude
    val expBiased_p0 = op_p0.exp.asSInt - S(o.c.bias, op_p0.exp.getWidth + 1 bits)
    
    // Compute approximate |x| in fixed-point (Q8.8 format for example)
    // |x| ≈ 2^exp * 1.mant
    // For LUT addressing, we map |x| ∈ [0, minInputAbs] to [0, tableSize]
    
    // Simplified addressing: use exp + high mant bits directly
    // For inputs in range [-8, 0], we need about 3-4 bits for integer part
    val tableAddr_p0 = UInt(o.tableSizeBits bits)
    
    // Compute scaled address: (|minInput| + x) / |minInput| * tableSize
    // For negative x: this gives index from 0 to tableSize-1
    // Approximation: use (exp + bias_offset) as coarse index, mant as fine index
    
    // Simple approach: quantize input using bit extraction
    // For float16 input in [-8, 0]:
    //   exp range: 15-3=12 (for 0.125) to 15+2=17 (for 4.0), effectively use exp-12 as coarse
    //   We want tableSize entries spanning 8 units, so each entry covers 8/256 = 0.03125
    
    // More practical: saturate and scale
    // index = clamp((x / minInput), 0, 1) * (tableSize - 1)
    //       = clamp(1 + x/|minInput|, 0, 1) * (tableSize - 1)   [for negative x]
    
    // Bit-level approximation for [-8, 0]:
    // When x is close to 0: exp is small (or x is denormal/zero), index -> tableSize-1
    // When x is -8: exp ≈ 2 (2^3 = 8), index -> 0
    
    // Use exp to determine magnitude, then mant for fine addressing
    val maxExpForTable = log2Up(minInputAbs.toInt.abs) + o.c.bias  // e.g., 3 + 15 = 18 for |-8|
    
    when(op_p0.sign && expBiased_p0 >= 0) {
        // Negative input with significant magnitude
        // index = tableSize * (1 - |x|/|minInput|)
        // Approximate: use (maxExp - exp) * fractional_step + mant contribution
        val expClamp = expBiased_p0.min(S(log2Up(minInputAbs.toInt.abs + 1), expBiased_p0.getWidth bits))
        val mantHigh = op_p0.mant >> (o.c.mant_size - (o.tableSizeBits - log2Up(minInputAbs.toInt.abs + 1)))
        
        // Coarse index from exponent, fine from mantissa
        val coarseShift = log2Up(minInputAbs.toInt.abs + 1)  // bits needed for integer part
        val fineShift = o.tableSizeBits - coarseShift
        
        tableAddr_p0 := ((U(tableSize - 1) - 
            (expClamp.asUInt.resize(o.tableSizeBits) << fineShift)) - 
            mantHigh.resize(o.tableSizeBits)).resize(o.tableSizeBits)
    } elsewhen (!op_p0.sign || expBiased_p0 < 0) {
        // Positive or very small negative -> exp(x) ≈ 1 for x ≈ 0
        tableAddr_p0 := U(tableSize - 1, o.tableSizeBits bits)  // exp(0) = 1
    } otherwise {
        // Large negative (x < minInput) -> exp(x) ≈ 0
        tableAddr_p0 := U(0, o.tableSizeBits bits)
    }

    //============================================================
    // Pipeline stage (optional)
    //============================================================
    val p1_pipe_ena = o.pipeStages >= 1
    val p1_vld      = if (p1_pipe_ena) RegNext(p0_vld, False) else p0_vld
    val op_nan_p1   = if (p1_pipe_ena) RegNext(op_nan_p0, False) else op_nan_p0
    val op_inf_pos_p1 = if (p1_pipe_ena) RegNext(op_inf_pos_p0, False) else op_inf_pos_p0
    val op_inf_neg_p1 = if (p1_pipe_ena) RegNext(op_inf_neg_p0, False) else op_inf_neg_p0
    val op_zero_p1  = if (p1_pipe_ena) RegNext(op_zero_p0, False) else op_zero_p0
    
    // LUT read
    val exp_val_p1 = if (p1_pipe_ena) {
        expTable.readSync(tableAddr_p0, p0_vld)
    } else {
        expTable.readAsync(tableAddr_p0)
    }

    //============================================================
    // Output generation
    //============================================================
    val sign_final = Bool()
    val exp_final  = UInt(o.c.exp_size bits)
    val mant_final = UInt(o.c.mant_size bits)

    when(op_nan_p1) {
        // NaN input -> NaN output
        sign_final := False
        exp_final.setAll()
        mant_final := (o.c.mant_size - 1 -> True, default -> False)
    }
    .elsewhen(op_inf_pos_p1) {
        // exp(+inf) = +inf
        sign_final := False
        exp_final.setAll()
        mant_final.clearAll()
    }
    .elsewhen(op_inf_neg_p1) {
        // exp(-inf) = 0
        sign_final := False
        exp_final.clearAll()
        mant_final.clearAll()
    }
    .elsewhen(op_zero_p1) {
        // exp(0) = 1
        sign_final := False
        exp_final  := U(o.c.bias, o.c.exp_size bits)  // 1.0
        mant_final.clearAll()
    }
    .otherwise {
        // Normal case: use LUT result
        sign_final := exp_val_p1(o.c.full_size - 1)
        exp_final  := exp_val_p1(o.c.mant_size, o.c.exp_size bits)
        mant_final := exp_val_p1(0, o.c.mant_size bits)
    }

    io.result.valid        := p1_vld
    io.result.payload.sign := sign_final
    io.result.payload.exp  := exp_final
    io.result.payload.mant := mant_final
}

/**
 * Combinational exp function for use in pipelines
 * Returns exp(x) with minimal latency (LUT-based)
 */
object FpxxExpComb {
    def apply(input: Fpxx, tableSizeBits: Int = 6, minInput: Double = -8.0): Fpxx = {
        val expModule = FpxxExp(FpxxExp.Options(
            c = input.c,
            pipeStages = 0,
            tableSizeBits = tableSizeBits,
            minInput = minInput
        ))
        expModule.io.op.valid := True
        expModule.io.op.payload := input
        expModule.io.result.ready := True
        expModule.io.result.payload
    }
}

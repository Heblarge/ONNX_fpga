package DMA

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.fsm._

// =============================================================================
// Stride DMA (矩阵分块语义版)
//
// 专为矩阵加速器设计的 DMA 引擎：
//   - 提供矩阵级语义：软件配置矩阵尺寸、分块尺寸和分块位置
//   - 硬件自动计算 DDR 地址、stride、边缘裁剪
//   - 支持非整除分块：边缘 tile 自动收缩为 actualCols × actualRows
//   - 内置 4KB AXI 突发边界分割
//   - 支持双向：DDR → Local（提取分块）和 Local → DDR（拼合分块）
//
// 寄存器映射 (AXI4-Lite):
//   0x00: CTRL        [0]=start (W1S), [1]=direction (0=DDR→Local, 1=Local→DDR)
//   0x04: STATUS      [0]=busy, [1]=done/intr
//   0x08: INTR_CLR    [0]=write 1 to clear
//   0x0C: MAT_BASE    大矩阵 DDR 基地址
//   0x10: LOCAL_BASE   BRAM 基地址
//   0x14: MAT_COLS    矩阵总列数 (beats)
//   0x18: MAT_ROWS    矩阵总行数
//   0x1C: TILE_COLS   分块标称宽度 (beats)
//   0x20: TILE_ROWS   分块标称高度
//   0x24: TILE_COL_POS 当前分块列起始位置 (beats)
//   0x28: TILE_ROW_POS 当前分块行起始位置
//   0x2C: ACTUAL_COLS  (只读) 实际传输宽度 = min(tileCols, matCols - tileColPos)
//   0x30: ACTUAL_ROWS  (只读) 实际传输高度 = min(tileRows, matRows - tileRowPos)
//
// 硬件在 start 时自动计算：
//   ddrAddr  = matBase + tileRowPos * matCols * bytesPerBeat + tileColPos * bytesPerBeat
//   stride   = matCols * bytesPerBeat
//   actualCols = min(tileCols, matCols - tileColPos)   (边缘裁剪)
//   actualRows = min(tileRows, matRows - tileRowPos)   (边缘裁剪)
// =============================================================================

case class StrideDmaConfig(
  ddrAddrWidth: Int = 32,
  localAddrWidth: Int = 20,
  dataWidth: Int = 256,
  maxBurstLen: Int = 256  // AXI4 最大 burst length
) {
  val bytesPerBeat: Int = dataWidth / 8
  // 4KB AXI 边界限制：单次突发最大 beats = min(maxBurstLen, 4096 / bytesPerBeat)
  val maxBeatsPerBurst: Int = scala.math.min(maxBurstLen, 4096 / bytesPerBeat)
}

case class StrideDma(cfg: StrideDmaConfig) extends Component {
  val bytesPerBeat = cfg.bytesPerBeat
  val byteShift    = log2Up(bytesPerBeat)  // bytesPerBeat = 2^byteShift

  val ddrAxiCfg = Axi4Config(
    addressWidth = cfg.ddrAddrWidth,
    dataWidth    = cfg.dataWidth,
    idWidth      = 4,
    useId        = true,
    useBurst     = true,
    useLock      = false,
    useRegion    = false,
    useQos       = false,
    useStrb      = true
  )

  val localAxiCfg = Axi4Config(
    addressWidth = cfg.localAddrWidth,
    dataWidth    = cfg.dataWidth,
    idWidth      = 4,
    useId        = true,
    useBurst     = true,
    useLock      = false,
    useRegion    = false,
    useQos       = false,
    useStrb      = true
  )

  val io = new Bundle {
    val mDdr   = master(Axi4(ddrAxiCfg))
    val mLocal = master(Axi4(localAxiCfg))
    val ctrl   = slave(AxiLite4(AxiLite4Config(addressWidth = 8, dataWidth = 32)))
    val intr   = out Bool()
  }

  // =========================================================================
  // 寄存器
  // =========================================================================
  val busCtrl = AxiLite4SlaveFactory(io.ctrl)

  val startPulse = Bool()
  startPulse := False
  val direction  = Reg(Bool()) init False

  val busy      = Reg(Bool()) init False
  val intrReg   = Reg(Bool()) init False
  val intrClear = Bool()
  intrClear := False

  val matBase    = Reg(UInt(cfg.ddrAddrWidth bits)) init 0
  val localBase  = Reg(UInt(cfg.localAddrWidth bits)) init 0
  val matCols    = Reg(UInt(16 bits)) init 0   // 矩阵总列数 (beats)
  val matRows    = Reg(UInt(16 bits)) init 0   // 矩阵总行数
  val tileCols   = Reg(UInt(16 bits)) init 0   // 分块标称宽度 (beats)
  val tileRows   = Reg(UInt(16 bits)) init 0   // 分块标称高度
  val tileColPos = Reg(UInt(16 bits)) init 0   // 分块列起始位置 (beats)
  val tileRowPos = Reg(UInt(16 bits)) init 0   // 分块行起始位置

  // 由硬件在 start 时计算的实际传输尺寸（只读）
  val actualCols = Reg(UInt(16 bits)) init 0
  val actualRows = Reg(UInt(16 bits)) init 0

  busCtrl.setOnSet(startPulse, address = 0x00, bitOffset = 0)
  busCtrl.readAndWrite(direction,  address = 0x00, bitOffset = 1)
  busCtrl.read(busy,    address = 0x04, bitOffset = 0)
  busCtrl.read(intrReg, address = 0x04, bitOffset = 1)
  busCtrl.setOnSet(intrClear, address = 0x08, bitOffset = 0)
  busCtrl.readAndWrite(matBase,    address = 0x0C)
  busCtrl.readAndWrite(localBase,  address = 0x10)
  busCtrl.readAndWrite(matCols,    address = 0x14)
  busCtrl.readAndWrite(matRows,    address = 0x18)
  busCtrl.readAndWrite(tileCols,   address = 0x1C)
  busCtrl.readAndWrite(tileRows,   address = 0x20)
  busCtrl.readAndWrite(tileColPos, address = 0x24)
  busCtrl.readAndWrite(tileRowPos, address = 0x28)
  busCtrl.read(actualCols, address = 0x2C)
  busCtrl.read(actualRows, address = 0x30)

  when(intrClear) { intrReg := False }
  io.intr := intrReg

  // =========================================================================
  // DMA 传输状态机
  // =========================================================================
  val rowIdx         = Reg(UInt(16 bits)) init 0
  val beatIdx        = Reg(UInt(16 bits)) init 0
  val curDdrAddr     = Reg(UInt(cfg.ddrAddrWidth bits)) init 0
  val curLocalAddr   = Reg(UInt(cfg.localAddrWidth bits)) init 0
  val curBurstLen    = Reg(UInt(8 bits)) init 0
  val beatsRemaining = Reg(UInt(16 bits)) init 0
  val rowDdrAddr     = Reg(UInt(cfg.ddrAddrWidth bits)) init 0
  val ddrStride      = Reg(UInt(cfg.ddrAddrWidth bits)) init 0  // 硬件计算的 stride

  val dataFifo = StreamFifo(Bits(cfg.dataWidth bits), cfg.maxBeatsPerBurst)

  // AXI 默认值
  // -- DDR 端 --
  io.mDdr.ar.valid := False
  io.mDdr.ar.addr  := 0
  io.mDdr.ar.len   := 0
  io.mDdr.ar.size  := log2Up(bytesPerBeat)
  io.mDdr.ar.burst := B"01"
  if (ddrAxiCfg.useId) io.mDdr.ar.id := 0

  io.mDdr.r.ready  := False

  io.mDdr.aw.valid := False
  io.mDdr.aw.addr  := 0
  io.mDdr.aw.len   := 0
  io.mDdr.aw.size  := log2Up(bytesPerBeat)
  io.mDdr.aw.burst := B"01"
  if (ddrAxiCfg.useId) io.mDdr.aw.id := 0

  io.mDdr.w.valid  := False
  io.mDdr.w.data   := 0
  io.mDdr.w.strb   := (BigInt(1) << bytesPerBeat) - 1
  io.mDdr.w.last   := False

  io.mDdr.b.ready  := True

  // -- Local 端 --
  io.mLocal.ar.valid := False
  io.mLocal.ar.addr  := 0
  io.mLocal.ar.len   := 0
  io.mLocal.ar.size  := log2Up(bytesPerBeat)
  io.mLocal.ar.burst := B"01"
  if (localAxiCfg.useId) io.mLocal.ar.id := 0

  io.mLocal.r.ready  := False

  io.mLocal.aw.valid := False
  io.mLocal.aw.addr  := 0
  io.mLocal.aw.len   := 0
  io.mLocal.aw.size  := log2Up(bytesPerBeat)
  io.mLocal.aw.burst := B"01"
  if (localAxiCfg.useId) io.mLocal.aw.id := 0

  io.mLocal.w.valid  := False
  io.mLocal.w.data   := 0
  io.mLocal.w.strb   := (BigInt(1) << bytesPerBeat) - 1
  io.mLocal.w.last   := False

  io.mLocal.b.ready  := True

  dataFifo.io.push.valid   := False
  dataFifo.io.push.payload := 0
  dataFifo.io.pop.ready    := False

  val fsm = new StateMachine {
    val IDLE        = new State with EntryPoint
    val COMPUTE     = new State   // 计算传输参数
    val ROW_START   = new State
    val DDR_RD_CMD  = new State
    val DDR_RD_DATA = new State
    val LOC_WR_CMD  = new State
    val LOC_WR_DATA = new State
    val LOC_RD_CMD  = new State
    val LOC_RD_DATA = new State
    val DDR_WR_CMD  = new State
    val DDR_WR_DATA = new State
    val ROW_DONE    = new State
    val DONE        = new State

    // =================================================================
    // IDLE → COMPUTE：软件触发 start，进入参数计算
    // =================================================================
    IDLE.whenIsActive {
      when(startPulse && !busy) {
        busy := True
        goto(COMPUTE)
      }
    }

    // =================================================================
    // COMPUTE：硬件自动计算实际传输参数（1拍完成）
    //   actualCols = min(tileCols, matCols - tileColPos)
    //   actualRows = min(tileRows, matRows - tileRowPos)
    //   ddrAddr    = matBase + (tileRowPos * matCols + tileColPos) * bytesPerBeat
    //   ddrStride  = matCols * bytesPerBeat
    // =================================================================
    COMPUTE.whenIsActive {
      // 边缘裁剪
      val colsRemaining = (matCols - tileColPos)  // 到矩阵右边缘的剩余列数
      val rowsRemaining = (matRows - tileRowPos)
      actualCols := Mux(tileCols > colsRemaining, colsRemaining, tileCols)
      actualRows := Mux(tileRows > rowsRemaining, rowsRemaining, tileRows)

      // 地址计算：matBase + (tileRowPos * matCols + tileColPos) << byteShift
      val linearOffset = (tileRowPos * matCols).resize(cfg.ddrAddrWidth bits) +
                         tileColPos.resize(cfg.ddrAddrWidth bits)
      val byteAddr     = (matBase + (linearOffset << byteShift)).resize(cfg.ddrAddrWidth bits)
      rowDdrAddr   := byteAddr
      curDdrAddr   := byteAddr
      curLocalAddr := localBase

      // stride = matCols << byteShift
      ddrStride := (matCols.resize(cfg.ddrAddrWidth bits) << byteShift).resize(cfg.ddrAddrWidth bits)

      rowIdx := 0
      goto(ROW_START)
    }

    // =================================================================
    // ROW_START：检查是否还有行，计算当前行首个子突发长度
    // =================================================================
    ROW_START.whenIsActive {
      when(rowIdx >= actualRows) {
        goto(DONE)
      }.otherwise {
        beatsRemaining := actualCols
        curDdrAddr     := rowDdrAddr
        beatIdx        := 0
        when(actualCols > cfg.maxBeatsPerBurst) {
          curBurstLen := U(cfg.maxBeatsPerBurst - 1, 8 bits)
        }.otherwise {
          curBurstLen := (actualCols - 1).resized
        }
        when(!direction) {
          goto(DDR_RD_CMD)
        }.otherwise {
          goto(LOC_RD_CMD)
        }
      }
    }

    // =================================================================
    // DDR → Local 方向
    // =================================================================
    DDR_RD_CMD.whenIsActive {
      io.mDdr.ar.valid := True
      io.mDdr.ar.addr  := curDdrAddr.resized
      io.mDdr.ar.len   := curBurstLen.resized
      when(io.mDdr.ar.ready) {
        beatIdx := 0
        goto(DDR_RD_DATA)
      }
    }

    DDR_RD_DATA.whenIsActive {
      io.mDdr.r.ready          := dataFifo.io.push.ready
      dataFifo.io.push.valid   := io.mDdr.r.valid
      dataFifo.io.push.payload := io.mDdr.r.data
      when(io.mDdr.r.fire) {
        beatIdx := beatIdx + 1
        when(io.mDdr.r.last || beatIdx === curBurstLen.resized) {
          beatIdx := 0
          goto(LOC_WR_CMD)
        }
      }
    }

    LOC_WR_CMD.whenIsActive {
      io.mLocal.aw.valid := True
      io.mLocal.aw.addr  := curLocalAddr.resized
      io.mLocal.aw.len   := curBurstLen.resized
      when(io.mLocal.aw.ready) {
        beatIdx := 0
        goto(LOC_WR_DATA)
      }
    }

    LOC_WR_DATA.whenIsActive {
      io.mLocal.w.valid := dataFifo.io.pop.valid
      io.mLocal.w.data  := dataFifo.io.pop.payload
      io.mLocal.w.strb  := (BigInt(1) << bytesPerBeat) - 1
      io.mLocal.w.last  := (beatIdx === curBurstLen.resized)
      dataFifo.io.pop.ready := io.mLocal.w.ready
      when(io.mLocal.w.fire) {
        beatIdx := beatIdx + 1
        when(beatIdx === curBurstLen.resized) {
          val subBurstBeats = curBurstLen.resize(16 bits) + 1
          val newRemaining = beatsRemaining - subBurstBeats
          beatsRemaining := newRemaining
          curDdrAddr     := curDdrAddr + (subBurstBeats << byteShift).resized
          curLocalAddr   := curLocalAddr + (subBurstBeats << byteShift).resized
          when(newRemaining > 0) {
            when(newRemaining > cfg.maxBeatsPerBurst) {
              curBurstLen := U(cfg.maxBeatsPerBurst - 1, 8 bits)
            }.otherwise {
              curBurstLen := (newRemaining - 1).resized
            }
            beatIdx := 0
            goto(DDR_RD_CMD)
          }.otherwise {
            goto(ROW_DONE)
          }
        }
      }
    }

    // =================================================================
    // Local → DDR 方向
    // =================================================================
    LOC_RD_CMD.whenIsActive {
      io.mLocal.ar.valid := True
      io.mLocal.ar.addr  := curLocalAddr.resized
      io.mLocal.ar.len   := curBurstLen.resized
      when(io.mLocal.ar.ready) {
        beatIdx := 0
        goto(LOC_RD_DATA)
      }
    }

    LOC_RD_DATA.whenIsActive {
      io.mLocal.r.ready        := dataFifo.io.push.ready
      dataFifo.io.push.valid   := io.mLocal.r.valid
      dataFifo.io.push.payload := io.mLocal.r.data
      when(io.mLocal.r.fire) {
        beatIdx := beatIdx + 1
        when(io.mLocal.r.last || beatIdx === curBurstLen.resized) {
          beatIdx := 0
          goto(DDR_WR_CMD)
        }
      }
    }

    DDR_WR_CMD.whenIsActive {
      io.mDdr.aw.valid := True
      io.mDdr.aw.addr  := curDdrAddr.resized
      io.mDdr.aw.len   := curBurstLen.resized
      when(io.mDdr.aw.ready) {
        beatIdx := 0
        goto(DDR_WR_DATA)
      }
    }

    DDR_WR_DATA.whenIsActive {
      io.mDdr.w.valid := dataFifo.io.pop.valid
      io.mDdr.w.data  := dataFifo.io.pop.payload
      io.mDdr.w.strb  := (BigInt(1) << bytesPerBeat) - 1
      io.mDdr.w.last  := (beatIdx === curBurstLen.resized)
      dataFifo.io.pop.ready := io.mDdr.w.ready
      when(io.mDdr.w.fire) {
        beatIdx := beatIdx + 1
        when(beatIdx === curBurstLen.resized) {
          val subBurstBeats = curBurstLen.resize(16 bits) + 1
          val newRemaining = beatsRemaining - subBurstBeats
          beatsRemaining := newRemaining
          curDdrAddr     := curDdrAddr + (subBurstBeats << byteShift).resized
          curLocalAddr   := curLocalAddr + (subBurstBeats << byteShift).resized
          when(newRemaining > 0) {
            when(newRemaining > cfg.maxBeatsPerBurst) {
              curBurstLen := U(cfg.maxBeatsPerBurst - 1, 8 bits)
            }.otherwise {
              curBurstLen := (newRemaining - 1).resized
            }
            beatIdx := 0
            goto(LOC_RD_CMD)
          }.otherwise {
            goto(ROW_DONE)
          }
        }
      }
    }

    // =================================================================
    // 行完成 → 下一行
    // =================================================================
    ROW_DONE.whenIsActive {
      rowIdx     := rowIdx + 1
      rowDdrAddr := rowDdrAddr + ddrStride
      goto(ROW_START)
    }

    DONE.whenIsActive {
      busy    := False
      intrReg := True
      goto(IDLE)
    }
  }
}

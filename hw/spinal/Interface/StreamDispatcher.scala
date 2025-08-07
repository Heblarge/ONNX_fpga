package Util

import spinal.core._
import spinal.lib.{Stream, Fragment, master, slave, OHMasking, OHToUInt, StreamDemux}

object StreamDispatcher {
  def apply[T <: Data](input: Stream[Fragment[T]], portCount: Int) = {
    val c = new StreamDispatcher(input.fragment, portCount)
    c.io.input << input
    c.io.outputs
  }
}

class StreamDispatcher[T <: Data](dataType: T, portCount: Int) extends Component {
  val io = new Bundle {
    val input = slave Stream Fragment(dataType)
    val outputs = Vec.fill(portCount)(master Stream Fragment(dataType))
  }

  val lock = RegNextWhen(!io.input.last, io.input.fire, False)
  val request = io.outputs.mapVec(_.ready).asBits
  val priority = Reg(Bits(portCount bits), B(1))
  when(!lock && io.input.valid) {
    priority := priority.rotateLeft(1)
  }
  val grant = OHMasking.roundRobin(request, priority)
  val selectNoLock = OHToUInt(grant)
  val select = lock ? RegNextWhen(selectNoLock, !lock, U(0)) | selectNoLock
  io.outputs <> StreamDemux(io.input, select, portCount)
}

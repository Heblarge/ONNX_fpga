package projectname

import spinal.core._
import spinal.lib._

class StreamDemoTop extends Component {
  val io = new Bundle {
    val inStream = slave(Stream(Vec.fill(4)(Vec.fill(4)(SInt(16 bits)))))
    val outStream = master(Stream(Vec.fill(4)(Vec.fill(4)(SInt(16 bits)))))
  }

  // 这里用StreamWidthAdapter将四行四列的矩阵“压扁”为Vec(4)的Bits流，方便FIFO传递
  val midStream_in = Stream(Vec.fill(4)(SInt(16 bits).asBits).asBits)
  val fifo = StreamFifo(Vec.fill(4)(SInt(16 bits).asBits).asBits, 2)
  val midStream_out = Stream(Vec.fill(4)(SInt(16 bits).asBits).asBits)

  // 输入矩阵每行压成Bits后通过StreamWidthAdapter转换成midStream_in
  val in2mid = StreamWidthAdapter(io.inStream, midStream_in)
  // 经过fifo缓冲
  midStream_in >> fifo.io.push
  fifo.io.pop >> midStream_out
  // 再转换回4x4矩阵流
  val mid2out = StreamWidthAdapter(midStream_out, io.outStream)
}

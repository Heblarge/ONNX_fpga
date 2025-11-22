package MatrixComputeUnit.SystolicArray2D

import MatrixComputeUnit.SystolicArray2D._
import spinal.core._
import spinal.lib._
import spinal.lib.tools
import spinal.core


case class ElementTranspose(cfg: SystolicArray2D_Config) extends Component{

  val io = new Bundle{
    val in_data = in(Vec.fill(cfg.in_MatA_row_num)(Vec.fill(cfg.in_MatB_col_num)(SInt_withFinalMark(cfg.out_MatZ_element_Width))))
    val out_data = out(Vec.fill(cfg.in_MatB_col_num)(Vec.fill(cfg.in_MatA_row_num)(SInt(cfg.out_MatZ_element_Width bits))))
    val need_transpose = in Bool()
  }
  when(io.need_transpose){
    for (i <- 0 until cfg.in_MatA_row_num){
      for (j <- 0 until cfg.in_MatB_col_num){
        io.out_data(j)(i) := io.in_data(i)(j).data
      }
    }
  }.otherwise{
    for (i <- 0 until cfg.in_MatA_row_num) {
      for (j <- 0 until cfg.in_MatB_col_num) {
        io.out_data(i)(j) := io.in_data(i)(j).data
      }
    }
  }
}

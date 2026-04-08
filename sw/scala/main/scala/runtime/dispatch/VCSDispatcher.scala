package runtime.dispatch

import Accelerator.{AcceleratorSimInterface, InstJavaTODO}
import runtime.engine._

/**
 * VCSDispatcher — HW backend using real VCS RTL simulation.
 *
 * Extends AcceleratorDispatcher with all its tiling, shift computation, and
 * dispatch logic, but overrides runOneInst to call AcceleratorSimInterface.runSimOneInst
 * (actual VCS simulation) instead of runRefOneInst (software reference model).
 *
 * Online cross-check: each tile is also computed by the SW reference model.
 * Any mismatch is printed immediately. If too many tiles diverge, the run aborts.
 */
class VCSDispatcher(dtype: String = "int32") extends AcceleratorDispatcher(dtype) {

  /** Enable online VCS vs SW cross-check per tile. */
  var crossCheck = true

  /** Maximum allowed per-element error (VCS vs SW reference). */
  var crossCheckMaxErr = 0L

  /** Abort after this many failed tiles. 0 = never abort, just warn. */
  var crossCheckAbortAfter = 5

  private var _tileCount = 0
  private var _failedTiles = 0
  private var _nodeStartTime = 0L

  override def execute(node: GraphNode, inputIds: Seq[String], graph: OnnxGraph): Seq[String] = {
    _tileCount = 0
    _failedTiles = 0
    _nodeStartTime = System.currentTimeMillis()
    val result = super.execute(node, inputIds, graph)
    val elapsed = System.currentTimeMillis() - _nodeStartTime
    // Print node-level summary with timing
    if (_tileCount > 0) {
      val status = if (_failedTiles == 0) "OK" else s"FAIL(${_failedTiles}/${_tileCount})"
      println(f"[VCS] HW#${_hwOpCount} ${node.opType}%-10s '${node.name}' ${_tileCount} tiles ${elapsed}ms $status")
    }
    result
  }

  override protected def runOneInst(matA: Array[Array[Long]], matB: Array[Array[Long]],
                                    inst: InstJavaTODO): Array[Array[Long]] = {
    _tileCount += 1
    // Run VCS simulation
    val intA = matA.map(_.map(_.toInt))
    val intB = matB.map(_.map(_.toInt))
    val vcsResult = AcceleratorSimInterface.runSimOneInst(intA, intB, inst)
    val result = vcsResult.map(_.map(_.toLong))

    // Online cross-check against SW reference
    if (crossCheck) {
      val refResult = AcceleratorSimInterface.runRefOneInst(matA, matB, inst)
      var maxErr = 0L
      var maxErrPos = (0, 0)
      var sumErr = 0L
      var count = 0
      val rows = math.min(result.length, refResult.length)
      for (i <- 0 until rows) {
        val cols = math.min(result(i).length, refResult(i).length)
        for (j <- 0 until cols) {
          val err = math.abs(result(i)(j) - refResult(i)(j))
          sumErr += err
          if (err > maxErr) { maxErr = err; maxErrPos = (i, j) }
          count += 1
        }
      }
      if (maxErr > crossCheckMaxErr) {
        _failedTiles += 1
        val meanErr = if (count > 0) sumErr.toDouble / count else 0.0
        val (ei, ej) = maxErrPos
        println(f"  [XCHK] TILE MISMATCH #${_tileCount} shape=(${inst.input0Shape0},${inst.input0Shape1},${inst.input1Shape1}) " +
          f"op=${inst.matrixOperation}+${inst.activationFunction} " +
          f"meanErr=$meanErr%.1f maxErr=$maxErr at($ei,$ej) vcs=${result(ei)(ej)} ref=${refResult(ei)(ej)}")
        if (crossCheckAbortAfter > 0 && _failedTiles >= crossCheckAbortAfter) {
          throw new RuntimeException(
            s"[XCHK] Aborting: ${_failedTiles} tiles exceeded error threshold (maxErr=$crossCheckMaxErr)")
        }
      }
    }
    result
  }
}

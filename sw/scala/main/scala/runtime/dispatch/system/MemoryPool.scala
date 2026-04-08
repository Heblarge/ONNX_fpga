package runtime.dispatch.system

import scala.collection.mutable

// =============================================================================
// MemoryPool — DDR 地址空间管理器
//
// 为张量分配 DDR 地址，跟踪张量生命周期，支持引用计数和地址回收。
//
// 特性：
//   - 对齐到 beat 边界（bytesPerBeat）
//   - 引用计数式张量生命周期管理
//   - 已释放空间的首次适配重用
//   - 统计信息（峰值使用量、分配次数）
//
// 使用方式：
//   val pool = new MemoryPool(baseAddr = 0x1_0000_0000L, size = 1L << 30, alignment = 32)
//   val addr = pool.allocate("tensor_A", 512 * 64 * 32)  // 1MB
//   pool.addRef("tensor_A")
//   pool.release("tensor_A")  // refCount -= 1
//   pool.release("tensor_A")  // refCount == 0 → 释放
// =============================================================================

case class TensorAllocation(
  tensorId: String,
  addr: Long,
  sizeBytes: Long,
  var refCount: Int = 1
)

class MemoryPool(
  val baseAddr: Long,
  val totalSize: Long,
  val alignment: Int = 32
) {

  // 活跃分配：tensorId → TensorAllocation
  private val allocations = mutable.LinkedHashMap.empty[String, TensorAllocation]

  // 空闲块列表：(addr, size)，按地址排序
  private val freeBlocks = mutable.TreeMap.empty[Long, Long]

  // 统计
  private var _nextAddr: Long = baseAddr
  private var _peakUsage: Long = 0L
  private var _totalAllocated: Long = 0L
  private var _allocCount: Int = 0

  // 初始化空闲块
  freeBlocks(baseAddr) = totalSize

  /** 分配 DDR 地址，自动对齐 */
  def allocate(tensorId: String, sizeBytes: Long): Long = {
    require(sizeBytes > 0, s"Cannot allocate $sizeBytes bytes for $tensorId")

    // 已存在则增加引用
    allocations.get(tensorId) match {
      case Some(alloc) =>
        alloc.refCount += 1
        return alloc.addr
      case None =>
    }

    val alignedSize = align(sizeBytes)

    // 首次适配查找空闲块
    val blockOpt = freeBlocks.find { case (_, blockSize) => blockSize >= alignedSize }
    val addr = blockOpt match {
      case Some((blockAddr, blockSize)) =>
        freeBlocks.remove(blockAddr)
        val remaining = blockSize - alignedSize
        if (remaining > 0) {
          freeBlocks(blockAddr + alignedSize) = remaining
        }
        blockAddr
      case None =>
        throw new OutOfMemoryError(
          f"MemoryPool exhausted: cannot allocate $alignedSize%,d bytes for '$tensorId'. " +
          f"Free: ${freeBlocks.values.sum}%,d / $totalSize%,d bytes, " +
          f"${allocations.size} active allocations")
    }

    val alloc = TensorAllocation(tensorId, addr, alignedSize)
    allocations(tensorId) = alloc
    _allocCount += 1
    _totalAllocated += alignedSize
    updatePeak()
    addr
  }

  /** 增加引用计数 */
  def addRef(tensorId: String): Unit = {
    allocations.get(tensorId) match {
      case Some(alloc) => alloc.refCount += 1
      case None => throw new NoSuchElementException(s"Tensor '$tensorId' not allocated")
    }
  }

  /** 减少引用计数，归零时释放地址空间 */
  def release(tensorId: String): Unit = {
    allocations.get(tensorId) match {
      case Some(alloc) =>
        alloc.refCount -= 1
        if (alloc.refCount <= 0) {
          allocations.remove(tensorId)
          returnBlock(alloc.addr, alloc.sizeBytes)
        }
      case None => // 忽略重复释放
    }
  }

  /** 获取张量的 DDR 地址 */
  def getAddress(tensorId: String): Option[Long] = {
    allocations.get(tensorId).map(_.addr)
  }

  /** 检查张量是否已分配 */
  def isAllocated(tensorId: String): Boolean = allocations.contains(tensorId)

  /** 获取分配信息 */
  def getAllocation(tensorId: String): Option[TensorAllocation] = allocations.get(tensorId)

  /** 当前使用量（字节） */
  def usedBytes: Long = allocations.values.map(_.sizeBytes).sum

  /** 峰值使用量（字节） */
  def peakUsage: Long = _peakUsage

  /** 总分配次数 */
  def allocCount: Int = _allocCount

  /** 当前活跃分配数 */
  def activeAllocations: Int = allocations.size

  /** 可用空间（字节） */
  def freeBytes: Long = freeBlocks.values.sum

  /** 重置所有分配 */
  def reset(): Unit = {
    allocations.clear()
    freeBlocks.clear()
    freeBlocks(baseAddr) = totalSize
    _nextAddr = baseAddr
    _peakUsage = 0L
    _totalAllocated = 0L
    _allocCount = 0
  }

  /** 打印内存使用摘要 */
  def printSummary(): Unit = {
    println(f"[MemoryPool] Base=0x${baseAddr}%X, Size=${totalSize / (1024*1024)}%d MB")
    println(f"  Used: ${usedBytes / 1024}%,d KB (${activeAllocations} tensors)")
    println(f"  Free: ${freeBytes / 1024}%,d KB (${freeBlocks.size} blocks)")
    println(f"  Peak: ${_peakUsage / 1024}%,d KB, Total allocs: ${_allocCount}")
  }

  // ====================== 内部实现 ======================

  private def align(size: Long): Long = {
    val rem = size % alignment
    if (rem == 0) size else size + (alignment - rem)
  }

  private def updatePeak(): Unit = {
    val used = usedBytes
    if (used > _peakUsage) _peakUsage = used
  }

  /** 归还内存块并合并相邻空闲块 */
  private def returnBlock(addr: Long, size: Long): Unit = {
    var mergedAddr = addr
    var mergedSize = size

    // 尝试与前一个空闲块合并
    val prevOpt = freeBlocks.rangeTo(addr).lastOption
    prevOpt match {
      case Some((prevAddr, prevSize)) if prevAddr + prevSize == addr =>
        freeBlocks.remove(prevAddr)
        mergedAddr = prevAddr
        mergedSize += prevSize
      case _ =>
    }

    // 尝试与后一个空闲块合并
    val nextOpt = freeBlocks.rangeFrom(mergedAddr + mergedSize).headOption
    nextOpt match {
      case Some((nextAddr, nextSize)) if nextAddr == mergedAddr + mergedSize =>
        freeBlocks.remove(nextAddr)
        mergedSize += nextSize
      case _ =>
    }

    freeBlocks(mergedAddr) = mergedSize
  }
}

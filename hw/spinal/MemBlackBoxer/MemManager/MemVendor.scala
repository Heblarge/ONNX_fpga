package MemBlackBoxer.MemManager

import spinal.core._
trait MemBlackBox extends BlackBox {
  //  setBlackBoxName(mem_cfg.name)

  def connectPort(): MemBlackBox
  protected var name: String

  addPrePopTask(() => setBlackBoxName(name))
}

abstract class SinglePortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "sp"
  SpinalInfo(s"single port ram: ${name}")
}

abstract class DualPortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "dp"
  SpinalInfo(s"dual port ram: ${name}")
}

abstract class TwoPortBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "tp"
  SpinalInfo(s"two port ram: ${name}")
}

abstract class RomBB(cfg: MemConfig) extends MemBlackBox {
  name = cfg.name + "rom"
}

/**
 * The memory vendor definition.
 * Provide a 'build' method to instantiate a memory blackbox, according to the memory wrapper type.
 */
trait MemVendor extends SpinalTag{
  val policy: MemBlackboxingPolicy = blackboxAll
  def prefixName: String
  def build(mw: MemWrapper) : MemBlackBox

}
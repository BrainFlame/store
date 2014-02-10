package com.treode.disk

class DisksConfig private (
    val superBlockBits: Int,
    val checkpointBytes: Int,
    val checkpointEntries: Int
) {

  val superBlockBytes = 1 << superBlockBits
  val superBlockMask = superBlockBytes - 1
  val diskLeadBytes = 1 << (superBlockBits + 1)

  def checkpoint (bytes: Int, entries: Int): Boolean =
    bytes > checkpointBytes || entries > checkpointEntries

  override def hashCode: Int =
    superBlockBits.hashCode

  override def equals (other: Any): Boolean =
    other match {
      case that: DisksConfig =>
        superBlockBits == that.superBlockBits
      case _ =>
        false
    }

  override def toString = s"DisksConfig($superBlockBits)"
}

object DisksConfig {

  def apply (
      superBlockBits: Int,
      checkpointBytes: Int,
      checkpointEntries: Int
  ): DisksConfig = {

    require (superBlockBits > 0, "A superblock must have more than 0 bytes")
    require (checkpointBytes > 0, "The checkpoint interval must be more than 0 bytes")
    require (checkpointEntries > 0, "The checkpoint interval must be more than 0 entries")

    new DisksConfig (superBlockBits, checkpointBytes, checkpointEntries)
  }

  val pickler = {
    import DiskPicklers._
    wrap (uint, uint, uint)
    .build ((apply _).tupled)
    .inspect (v => (v.superBlockBits, v.checkpointBytes, v.checkpointEntries))
  }}

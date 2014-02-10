package com.treode.disk

import org.scalatest.FlatSpec

class DiskGeometrySpec extends FlatSpec {

  implicit val config = DisksConfig (14, 1<<24, 1<<16)

  val block = 1<<12
  val seg = 1<<16

  def expectBounds (id: Int, pos: Long, limit: Long) (actual: SegmentBounds): Unit =
    expectResult (SegmentBounds (id, pos, limit)) (actual)

  "DiskGeometry" should "compute the segment count" in {
    val disk1 = 1<<20
    val disk2 = 1<<21
    def c (diskBytes: Long) = DiskGeometry (16, 12, diskBytes).segmentCount
    expectResult (16) (c (disk1))
    expectResult (17) (c (disk1 + 4*block))
    expectResult (32) (c (disk2))
    expectResult (32) (c (disk2 - seg + 4*block))
    expectResult (31) (c (disk2 - seg + 4*block - 1))
    expectResult (32) (c (disk2 + 4*block - 1))
    expectResult (33) (c (disk2 + 4*block))
  }

  it should "align block length" in {
    val c = DiskGeometry (16, 12, 1<<20)
    expectResult (0) (c.blockAlignLength (0))
    expectResult (block) (c.blockAlignLength (1))
    expectResult (block) (c.blockAlignLength (4095))
    expectResult (block) (c.blockAlignLength (4096))
    expectResult (2*block) (c.blockAlignLength (4097))
  }

  it should "compute the segment bounds" in {
    val c = DiskGeometry (16, 12, (1<<20) + 6*block)
    expectBounds (0, config.diskLeadBytes, seg) (c.segmentBounds (0))
    expectBounds (1, seg, 2*seg) (c.segmentBounds (1))
    expectBounds (2, 2*seg, 3*seg) (c.segmentBounds (2))
    expectBounds (2, 2*seg, 3*seg) (c.segmentBounds (2))
    expectBounds (16, 16*seg, 16*seg + 6*block) (c.segmentBounds (16))
  }}

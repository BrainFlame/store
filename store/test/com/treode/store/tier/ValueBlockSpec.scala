package com.treode.store.tier

import com.treode.pickle.{Picklers, pickle, unpickle}
import com.treode.store.{Bytes, TxClock}
import org.scalatest.WordSpec
import io.netty.buffer.Unpooled

class ValueBlockSpec extends WordSpec {

  val MaxTime = TxClock.MaxValue

  val Apple = Bytes ("apple")
  val Kiwi = Bytes ("kiwi")
  val Orange = Bytes ("orange")

  private def entry (key: Bytes, time: Int): ValueEntry =
    new ValueEntry (key, time, None)

  private def newBlock (entries: ValueEntry*): ValueBlock =
    new ValueBlock (Array (entries: _*))

  private def entriesEqual (expected: ValueEntry, actual: ValueEntry) {
    expectResult (expected.key) (actual.key)
    expectResult (expected.time) (actual.time)
    expectResult (expected.value) (actual.value)
  }

  private def blocksEqual (expected: ValueBlock, actual: ValueBlock) {
    expectResult (expected.entries.length) (actual.entries.length)
    for (i <- 0 until expected.entries.length)
      entriesEqual (expected.entries (i), actual.entries (i))
  }

  private def checkPickle (block: ValueBlock) {
    val buffer = Unpooled.buffer()
    pickle (ValueBlock.pickle, block, buffer)
    val result = unpickle (ValueBlock.pickle, buffer)
    blocksEqual (block, result)
  }

  "A ValueBlock" when {

    "empty" should {

      val block = newBlock ()

      "find nothing" in {
        expectResult (0) (block.find (Apple, MaxTime))
      }

      "pickle and unpickle to the same value" in {
        checkPickle (block)
      }}

    "holding one entry k=kiwi at t=7" should {

      val block = newBlock (entry (Kiwi, 7))

      "find apple before kiwi" in {
        expectResult (0) (block.find (Apple, MaxTime))
      }

      "find kiwi using kiwi at t=8" in {
        expectResult (0) (block.find (Kiwi, 8))
      }

      "find kiwi using kiwi at t=7" in {
        expectResult (0) (block.find (Kiwi, 7))
      }

      "find orange using kiwi at t=6" in {
        expectResult (1) (block.find (Kiwi, 6))
      }

      "find orange after kiwi" in {
        expectResult (1) (block.find (Orange, MaxTime))
      }

      "pickle and unpickle to the same value" in {
        checkPickle (block)
      }}

    "holding three keys at t=7" should {

      val block = newBlock (
          entry (Apple, 7),
          entry (Kiwi, 7),
          entry (Orange, 7))

      "find apple using (apple, 8)" in {
        expectResult (0) (block.find (Apple, 8))
      }

      "find apple using (apple, 7)" in {
        expectResult (0) (block.find (Apple, 7))
      }

      "find kiwi using (apple, 6)" in {
        expectResult (1) (block.find (Apple, 6))
      }

      "find kiwi using (kiwi, 8)" in {
        expectResult (1) (block.find (Kiwi, 8))
      }

      "find kiwi using (kiwi, 7)" in {
        expectResult (1) (block.find (Kiwi, 7))
      }

      "find orange using (kiwi, 6)" in {
        expectResult (2) (block.find (Kiwi, 6))
      }

      "find orange using (orange, 8)" in {
        expectResult (2) (block.find (Orange, 8))
      }

      "find orange using (orange, 7)" in {
        expectResult (2) (block.find (Orange, 7))
      }

      "find the end using (orange, 6)" in {
        expectResult (3) (block.find (Orange, 6))
      }

      "pickle and unpickle to the same value" in {
        checkPickle (block)
      }}

    "holding three entries of kiwi" should {

      val block = newBlock (
          entry (Kiwi, 21),
          entry (Kiwi, 14),
          entry (Kiwi, 7))

      "find (kiwi, 21) using (kiwi, 22)" in {
        expectResult (0) (block.find (Kiwi, 22))
      }

      "find (kiwi, 21) using (kiwi, 21)" in {
        expectResult (0) (block.find (Kiwi, 21))
      }

      "find (kiwi, 14) using (kiwi, 20)" in {
        expectResult (1) (block.find (Kiwi, 20))
      }

      "find (kiwi, 14) using (kiwi, 15)" in {
        expectResult (1) (block.find (Kiwi, 15))
      }

      "find (kiwi, 14) using (kiwi, 14)" in {
        expectResult (1) (block.find (Kiwi, 14))
      }

      "find (kiwi, 7) using (kiwi, 13)" in {
        expectResult (2) (block.find (Kiwi, 13))
      }

      "find (kiwi, 7) using (kiwi, 8)" in {
        expectResult (2) (block.find (Kiwi, 8))
      }

      "find (kiwi, 7) using (kiwi, 7)" in {
        expectResult (2) (block.find (Kiwi, 7))
      }

      "find the end using (kiwi, 6)" in {
        expectResult (3) (block.find (Kiwi, 6))
      }

      "pickle and unpickle to the same value" in {
        checkPickle (block)
      }}}}
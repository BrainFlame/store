package com.treode.store

import scala.language.implicitConversions

import com.treode.pickle.Picklers

class TxClock private (val time: Long) extends AnyVal with Ordered [TxClock] {

  def compare (that: TxClock): Int =
    this.time compare that.time

  override def toString = "TxClock:%X" format time
}

object TxClock extends Ordering [TxClock] {

  // Supports testing only.
  private [store] implicit def apply (time: Long): TxClock =
    new TxClock (time)

  val Zero = new TxClock (0)

  val MaxValue = new TxClock (Long.MaxValue)

  def now = new TxClock (System.currentTimeMillis * 1000)

  def compare (x: TxClock, y: TxClock): Int =
    x compare y

  val pickle = {
    import Picklers._
    wrap [Long, TxClock] (unsignedLong, TxClock (_), _.time)
  }}
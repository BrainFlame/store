/*
 * Copyright 2014 Treode, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.treode.store.locks

import scala.collection.SortedSet
import com.treode.async.Callback
import com.treode.async.implicits._
import com.treode.store.TxClock

// Tracks the acquisition of locks and invokes the callback when they have all been granted.
private class LockWriter (
    space: LockSpace,
    _ft: TxClock,
    private var ids: SortedSet [Int],
    private var cb: Callback [LockSet]) extends LockSet {

  // For testing mocks.
  def this() = this (null, TxClock.MinValue, SortedSet.empty, Callback.ignore)

  private val iter = ids.iterator
  private var max = _ft

  private def finish() {
    val cb = this.cb
    this.cb = null
    cb.pass (this)
  }

  // Attempt to acquire the locks.  Some of them will be granted immediately, then we will need
  // to wait for one, which will be granted later by a call to grant.  Do this in ascending order
  // of lock id to prevent deadlocks.
  private def acquire(): Boolean = {
    while (iter.hasNext) {
      val id = iter.next
      space.write (id, this) match {
        case Some (max) =>
          if (this.max < max)
            this.max = max
        case None =>
          return false
      }}
    true
  }

  def init() {
    val ready = synchronized {
      acquire()
    }
    if (ready)
      finish()
  }

  def grant (max: TxClock): Unit = {
    val ready = synchronized {
      if (this.max < max)
        this.max = max
      acquire()
    }
    if (ready)
      finish()
  }

  def ft = max

  def release() {
    require (cb == null, "Locks cannot be released until acquired.")
    require (ids != null, "Locks were already released.")
    ids foreach (space.release (_, this))
    ids = null
  }

  override def toString = s"LockWriter (ft=$ft, ready=${!iter.hasNext})"
}

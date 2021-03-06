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

import java.util
import scala.collection.JavaConversions._

import com.treode.store.TxClock

// A reader/writer lock that implements a Lamport clock.  It allows one writer at a time, and
// assists the writer in committing as of timestamps greater than any past reader.  It allows
// multiple readers at a time.  It even allows readers while a writer holds the lock, as long
// as the reader's timestamp is less than the one at which the writer will commit.
private class Lock {

  private val NoReaders = new util.ArrayList [LockReader] (0)

  // The forecasted minimum version timestamp.  All future writers shall commit a value with a
  // version timestamp above this.  Any current reader as of a timestamp at or below this may
  // proceed immediately since it is assured that no future writer will invalidate its read.  A
  // reader as of a timestamp greater than this must wait for the current writer to release the
  // lock since that writer could commit the value with timestamp forecast+1; then the reader must
  // raise the forecasted value to prevent later writers from invalidating its read.
  private var forecast = TxClock.MinValue

  // Does a writer hold the lock?  If non-null, a writer holds the lock.  If it commits values,
  // they will be timestamped greater than the forecasted timestamp.
  private var engaged: LockWriter = null

  // These readers want to acquire the lock at a timestamp greater than the forecasted timestamp
  // of the writer that currently holds the lock.
  private var readers = new util.ArrayList [LockReader]

  // These writers want to acquire the lock, but a writer already holds the lock.
  private val writers = new util.ArrayDeque [LockWriter]

  // A reader wants to acquire the lock; this means the reader ensures that no writer will commit
  // a value with a timestamp at or below the reader's timestamp.
  // - If its read timestamp is less than or equal to the forecasted one, the reader may proceed.
  // - If its read timestamp is greater than the forecasted one and the lock is free, then the
  //   reader may proceed.  First, it raises the forecast to ensure no writer commits a value with
  //   a lower timestamp.
  // - If its read timestamp is greater than the forecasted one and a writer holds the lock, then
  //   the reader must wait for the writer to release it since that writer could commit a value
  //   with a timestamp as low as forecast+1.
  //
  // If the reader may proceed immediately, this returns true.  Otherwise, it returns false and
  // queues the reader to be called back later.
  def read (r: LockReader): Boolean = synchronized {
    if (r.rt <= forecast) {
      true
    } else if (engaged == null) {
      forecast = r.rt
      true
    } else {
      readers.add (r)
      false
    }}

  // A writer wants to acquire the lock; it must ensure that it does not invalidate the read of
  // any past reader.  No past reader has read a value as of a timestamp greater than forecast,
  // so the writer must commit its values at a timestamp greater than forecast.
  //
  // If the writer may proceed immediately, returns Some (forecast).  Otherwise, it queues the
  // writer to be called back later.
  def write (w: LockWriter): Option [TxClock] = synchronized {
    if (engaged == null) {
      if (forecast < w.ft)
        forecast = w.ft
      engaged = w
      Some (forecast)
    } else {
      writers.add (w)
      None
    }}

  // A writer is finished with the lock.  If there are any waiting readers, raise the forecast to
  // the maximum of all of them and then let all of them proceed.  If there is a waiting writer,
  // next let it proceed with that forecast.
  def release (w0: LockWriter): Unit = {
    require (engaged == w0, "The writer releasing the lock does not hold it.")
    var rs = NoReaders
    var w = Option.empty [LockWriter]
    var ft = TxClock.MinValue
    synchronized {
      var rt = TxClock.MinValue
      var i = 0
      while (i < readers.length) {
        if (rt < readers(i).rt)
          rt = readers(i).rt
        i += 1
      }
      if (forecast < rt)
        forecast = rt
      if (!readers.isEmpty) {
        rs = readers
        readers = new util.ArrayList
      }
      if (!writers.isEmpty) {
        val _w = writers.remove()
        w = Some (_w)
        if (forecast < _w.ft)
          forecast = _w.ft
        engaged = _w
      } else {
        engaged = null
      }
      ft = forecast
    }
    rs foreach (_.grant())
    w foreach (_.grant (ft))
  }

  override def toString = s"Lock (ft=${forecast}"
}

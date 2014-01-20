package com.treode.store.disk2

import com.treode.async.{Callback, callback}
import com.treode.async.io.{File, Framer}
import com.treode.buffer.{Input, PagedBuffer, Output}
import com.treode.pickle.{Pickler, pickle, unpickle}

private class RecordRegistry {

  private val records = new Framer [TypeId, RecordHeader, Unit => Any] (RecordRegistry.framer)

  def read (file: File, pos: Long, buf: PagedBuffer, cb: Callback [records.FileFrame]): Unit =
    records.read (file, pos, buf, cb)

  def register [R] (p: Pickler [R], id: TypeId) (f: R => Any): Unit =
    records.register (p, id) (v => _ => f (v))
}

private object RecordRegistry {

  val framer: Framer.Strategy [TypeId, RecordHeader] =
    new Framer.Strategy [TypeId, RecordHeader] {

      def newEphemeralId = ???

      def isEphemeralId (id: TypeId) = false

      def readHeader (in: Input): (Option [TypeId], RecordHeader) = {
        val hdr = unpickle (RecordHeader.pickle, in)
        hdr match {
          case RecordHeader.Entry (time, id) => (Some (id), hdr)
          case _ => (None, hdr)
        }}

      def writeHeader (hdr: RecordHeader, out: Output) {
        pickle (RecordHeader.pickle, hdr, out)
      }}}

package com.treode.cluster

import java.net.SocketAddress
import java.util.concurrent.Executors
import scala.language.postfixOps
import scala.util.Random

import com.treode.async.{Scheduler, StubScheduler}
import com.treode.buffer.PagedBuffer
import com.treode.cluster.events.StubEvents
import com.treode.cluster.messenger.{MailboxRegistry, PeerRegistry}
import com.treode.pickle.{Pickler, pickle}

abstract class BaseStubCluster (seed: Long, nhosts: Int, multithreaded: Boolean = false) {

  private val emptyAddr = new SocketAddress {}

  class BaseStubHost (val localId: HostId) extends Host {

    val random: Random = BaseStubCluster.this.random

    val scheduler: Scheduler = BaseStubCluster.this.scheduler

    val mailboxes: MailboxRegistry = new MailboxRegistry () (StubEvents)

    val peers: PeerRegistry =
      new PeerRegistry (localId, new StubConnection (BaseStubCluster.this, _, localId)) (random)

    def locate (id: Int): Acknowledgements =
      BaseStubCluster.this.locate (id)

    def cleanup(): Unit = ()

    private [cluster] def deliver (id: MailboxId, from: HostId, msg: PagedBuffer): Unit =
      mailboxes.deliver (id, peers.get (from), msg, msg.readableBytes)
  }

  type StubHost <: BaseStubHost

  def newHost (id: HostId): StubHost

  val random = new Random (seed)

  val scheduler =
    if (multithreaded)
      StubScheduler.multithreaded (Executors.newScheduledThreadPool (8))
    else
      StubScheduler.random (random)

  var messageTrace = false
  var messageFlakiness = 0.0

  val hosts = Seq.fill (nhosts) (newHost (HostId (random.nextLong)))
  for (h1 <- hosts)
    for (h2 <- hosts)
      h1.peers.get (h2.localId) .address = emptyAddr

  private val hostById = hosts .map (h => (h.localId, h)) .toMap

  def locate (id: Int): Acknowledgements =
    Acknowledgements.settled (hosts map (_.localId): _*)

  def deliver [M] (p: Pickler [M], from: HostId, to: HostId, mbx: MailboxId, msg: M) {
    if (messageFlakiness == 0.0 || random.nextDouble > messageFlakiness) {
      val h = hostById.get (to)
      require (h.isDefined, s"$to does not exist.")
      if (messageTrace)
        println (s"$from->$to:$mbx: $msg")
      val buf = PagedBuffer (12)
      pickle (p, msg, buf)
      h.get.deliver (mbx, from, buf)
    }}

  def runTasks(): Unit = scheduler.runTasks()

  def cleanup(): Unit = {
    hosts.foreach (_.cleanup())
    scheduler.shutdown (200)
  }}
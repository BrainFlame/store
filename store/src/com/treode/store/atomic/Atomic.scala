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

package com.treode.store.atomic

import scala.util.Random

import com.treode.async.{Async, Scheduler}
import com.treode.cluster.Cluster
import com.treode.disk.Disk
import com.treode.store.{Atlas, Library, Store}
import com.treode.store.paxos.Paxos

private [store] trait Atomic extends Store {

  def rebalance (atlas: Atlas): Async [Unit]
}

private [store] object Atomic {

  trait Recovery {

    def launch (implicit launch: Disk.Launch, cluster: Cluster, paxos: Paxos): Async [Atomic]
  }

  def recover() (implicit
      random: Random,
      scheduler: Scheduler,
      library: Library,
      recovery: Disk.Recovery,
      config: Store.Config
  ): Recovery =
    new RecoveryKit
}
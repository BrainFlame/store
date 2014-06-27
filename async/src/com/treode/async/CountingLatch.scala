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

package com.treode.async

import scala.util.{Failure, Success, Try}

private class CountingLatch [A] (count: Int, cb: Callback [Unit])
extends AbstractLatch (count, cb) with Callback [A] {

  private var thrown = List.empty [Throwable]

  init()

  def value = ()

  def apply (v: Try [A]): Unit = synchronized {
    v match {
      case Success (v) => release()
      case Failure (t) => failure (t)
    }}}
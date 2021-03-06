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

import com.treode.async.implicits._
import com.treode.async.stubs.StubScheduler
import com.treode.async.stubs.implicits._
import org.scalatest.FlatSpec

import Async.supply
import AsyncIteratorTestTools._

class MergeIteratorSpec extends FlatSpec {

  private def merge [A] (xss: Seq [A] *) (implicit s: StubScheduler, ord: Ordering [A]) =
    AsyncIterator.merge (xss map (_.async))

  "The MergeIterator" should "yield nothing for []" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge [Int] ()
    assertSeq () (iter)
  }

  it should "yield nothing for [[]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge [Int] (Seq.empty)
    assertSeq () (iter)
  }

  it should "yield one thing for [[1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1))
    assertSeq (1) (iter)
  }

  it should "yield one thing for [[][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq.empty, Seq (1))
    assertSeq (1) (iter)
  }

  it should "yield one thing for [[1][]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq.empty)
    assertSeq (1) (iter)
  }

  it should "yield two things for [[1, 2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1, 2))
    assertSeq (1, 2) (iter)
  }

  it should "yield two things for [[1][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (2))
    assertSeq (1, 2) (iter)
  }

  it should "yield two things for [[][1][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq.empty, Seq (1), Seq (2))
    assertSeq (1, 2) (iter)
  }

  it should "yield two things for [[1][][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq.empty, Seq (2))
    assertSeq (1, 2) (iter)
  }

  it should "yield two things for [[1][2][]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (2), Seq.empty)
    assertSeq (1, 2) (iter)
  }

  it should "yield two things sorted for [[2][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (2), Seq (1))
    assertSeq (1, 2) (iter)
  }

  it should "yield things sorted for [[1][2][3]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (2), Seq (3))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[1][3][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (3), Seq (2))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[2][1][3]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (2), Seq (3))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[2][3][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (2), Seq (3), Seq (1))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[3][1][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (3), Seq (1), Seq (2))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[3][2][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (3), Seq (2), Seq (1))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[1, 2][3]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1, 2), Seq (3))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[1][2, 3]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1), Seq (2, 3))
    assertSeq (1, 2, 3) (iter)
  }

  it should "yield things sorted for [[1, 3][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1, 3), Seq (2))
    assertSeq (1, 2, 3) (iter)
  }

  it should "preserve duplicates in tier order with [[1][1][2]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1 -> "a"), Seq (1 -> "b"), Seq (2 -> "c"))
    assertSeq (1 -> "a", 1 -> "b", 2 -> "c") (iter)
  }

  it should "preserve duplicates in tier order with [[1][2][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (1 -> "a"), Seq (2 -> "b"), Seq (1 -> "c"))
    assertSeq (1 -> "a", 1 -> "c", 2 -> "b") (iter)
  }

  it should "preserve duplicates in tier order with [[2][1][1]]" in {
    implicit val scheduler = StubScheduler.random()
    val iter = merge (Seq (2 -> "a"), Seq (1 -> "b"), Seq (1 -> "c"))
    assertSeq (1 -> "b", 1 -> "c", 2 -> "a") (iter)
  }

  it should "report an exception from the first iterator" in {
    implicit val scheduler = StubScheduler.random()
    var c1 = Set.empty [Int]
    var c2 = Set.empty [Int]
    var provided = Set.empty [Int]
    assertFail [DistinguishedException] {
      val i1 = track (adapt (1, 3, 5, 7)) (c1 += _)
      val i2 = failWhen (i1) (_ == 5)
      val j1 = track (adapt (2, 4, 6, 8)) (c2 += _)
      track (AsyncIterator.merge (Seq (i2, j1))) (provided += _)
    }
    assertResult (Set (1, 3, 5)) (c1)
    assertResult (Set (2, 4)) (c2)
    assertResult (Set (1, 2, 3)) (provided)
  }

  it should "report an exception while opening the first iterator" in {
    implicit val scheduler = StubScheduler.random()
    var c2 = Set.empty [Int]
    var provided = Set.empty [Int]
    assertFail [DistinguishedException] {
      val i1 = failNow [Int]
      val j1 = track (adapt (2, 4, 6, 8)) (c2 += _)
      track (AsyncIterator.merge (Seq (i1, j1))) (provided += _)
    }
    assertResult (Set (2)) (c2)
    assertResult (Set.empty) (provided)
  }

  it should "report an exception from the second iterator" in {
    implicit val scheduler = StubScheduler.random()
    var c1 = Set.empty [Int]
    var c2 = Set.empty [Int]
    var provided = Set.empty [Int]
    assertFail [DistinguishedException] {
      val i1 = track (adapt (1, 3, 5, 7)) (c1 += _)
      val j1 = track (adapt (2, 4, 6, 8)) (c2 += _)
      val j2 = failWhen (j1) (_ == 6)
      track (AsyncIterator.merge (Seq (i1, j2))) (provided += _)
    }
    assertResult (Set (1, 3, 5)) (c1)
    assertResult (Set (2, 4, 6)) (c2)
    assertResult (Set (1, 2, 3, 4)) (provided)
  }

  it should "report an exception while opening the second iterator" in {
    implicit val scheduler = StubScheduler.random()
    var c1 = Set.empty [Int]
    var provided = Set.empty [Int]
    assertFail [DistinguishedException] {
      val i1 = track (adapt (1, 3, 5, 7)) (c1 += _)
      val j1 = failNow [Int]
      track (AsyncIterator.merge (Seq (i1, j1))) (provided += _)
    }
    assertResult (Set (1)) (c1)
    assertResult (Set.empty) (provided)
  }

  it should "report exceptions from both iterators" in {
    implicit val scheduler = StubScheduler.random()
    var c1 = Set.empty [Int]
    var c2 = Set.empty [Int]
    var provided = Set.empty [Int]
    assertFail [MultiException] {
      val i1 = track (adapt (1, 3, 5, 7)) (c1 += _)
      val i2 = failWhen (i1) (_ => true)
      val j1 = track (adapt (2, 4, 6, 8)) (c2 += _)
      val j2 = failWhen (j1) (_ => true)
      track (AsyncIterator.merge (Seq (i2, j2))) (provided += _)
    }
    assertResult (Set (1)) (c1)
    assertResult (Set (2)) (c2)
    assertResult (Set.empty) (provided)
  }

  it should "report exceptions while opening both iterators" in {
    implicit val scheduler = StubScheduler.random()
    var provided = Set.empty [Int]
    assertFail [MultiException] {
      val i1 = failNow [Int]
      val j1 = failNow [Int]
      track (AsyncIterator.merge (Seq (i1, j1))) (provided += _)
    }
    assertResult (Set.empty) (provided)
  }

  it should "report a failure returned from the body" in {
    implicit val scheduler = StubScheduler.random()
    val i1 = merge [Int] (Seq (1))
    val i2 = i1.foreach (_ => supply (throw new DistinguishedException))
    i2.fail [DistinguishedException]
  }

  it should "report an exception thrown from the body" in {
    implicit val scheduler = StubScheduler.random()
    val i1 = merge [Int] (Seq (1), Seq (2))
    val i2 = i1.foreach (x => supply (if (x == 2) throw new DistinguishedException))
    i2.fail [DistinguishedException]
  }}

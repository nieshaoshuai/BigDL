/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.analytics.sparkdl.torch

import com.intel.analytics.sparkdl.nn.CMinTable
import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.utils.RandomGenerator._
import com.intel.analytics.sparkdl.utils.Table
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.mutable.HashMap
import scala.util.Random


class CMinTableSpec extends FlatSpec with BeforeAndAfter with Matchers{
  before {
    if (!TH.hasTorch()) {
      cancel("Torch is not installed")
    }
  }

  "A CMaxTable Module" should "generate correct output and grad" in {
    val seed = 100
    RNG.setSeed(seed)
    val module = new CMinTable[Double]()

    val input1 = Tensor[Double](5).apply1(e => Random.nextDouble())
    val input2 = Tensor[Double](5).apply1(e => Random.nextDouble())
    val gradOutput = Tensor[Double](5).apply1(e => Random.nextDouble())
    val input = new Table()
    input(1.toDouble) = input1
    input(2.toDouble) = input2

    val start = System.nanoTime()
    val output = module.forward(input)
    val gradInput = module.backward(input, gradOutput)
    val end = System.nanoTime()
    val scalaTime = end - start

    val code = "torch.manualSeed(" + seed + ")\n" +
      "module = nn.CMinTable()\n" +
      "output = module:forward(input)\n" +
      "gradInput = module:backward(input,gradOutput)\n"

    val (luaTime, torchResult) = TH.run(code, Map("input" -> input, "gradOutput" -> gradOutput),
      Array("output", "gradInput"))
    val luaOutput1 = torchResult("output").asInstanceOf[Tensor[Double]]
    val luaOutput2 = torchResult("gradInput").asInstanceOf[HashMap[Double, Tensor[Double]]]

    luaOutput1 should be(output)
    luaOutput2.get(1.0).getOrElse(null) should be(gradInput[Tensor[Double]](1.0))
    luaOutput2.get(2.0).getOrElse(null) should be(gradInput[Tensor[Double]](2.0))

    println("Test case : CMinTable, Torch : " + luaTime +
      " s, Scala : " + scalaTime / 1e9 + " s")
  }
}
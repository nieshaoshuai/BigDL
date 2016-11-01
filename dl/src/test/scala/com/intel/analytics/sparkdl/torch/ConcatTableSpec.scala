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

import com.intel.analytics.sparkdl.nn.{ConcatTable, Linear}
import com.intel.analytics.sparkdl.tensor.Tensor
import com.intel.analytics.sparkdl.utils.T
import com.intel.analytics.sparkdl.utils.RandomGenerator._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.util.Random

class ConcatTableSpec extends FlatSpec with BeforeAndAfter with Matchers {
  before {
    if (!TH.hasTorch()) {
      cancel("Torch is not installed")
    }
  }

  "ConcatTable forward tensor" should "return right output" in {
    val seed = 100
    RNG.setSeed(seed)

    val ctable = new ConcatTable[Double]()
    ctable.add(new Linear(5, 2))
    ctable.add(new Linear(5, 3))
    val input = Tensor[Double](5).apply1(_ => Random.nextDouble())
    val gradOutput1 = Tensor[Double](2).apply1(_ => Random.nextDouble())
    val gradOutput2 = Tensor[Double](3).apply1(_ => Random.nextDouble())

    val output = ctable.forward(input)

    val gradOutput = T(gradOutput1, gradOutput2)
    val gradInput = ctable.updateGradInput(input, gradOutput)

    val code = "torch.manualSeed(" + seed + ")\n" +
      """module = nn.ConcatTable():add(nn.Linear(5, 2)):add(nn.Linear(5, 3))
        gradOutput = {gradOutput1, gradOutput2}
        output = module:forward(input)
        gradInput = module:backward(input, gradOutput)
        output1 = output[1]
        output2 = output[2]
      """

    val (luaTime, torchResult) = TH.run(code,
      Map("input" -> input, "gradOutput1" -> gradOutput1, "gradOutput2" -> gradOutput2),
      Array("output1", "output2", "gradInput"))
    val luaOutput1 = torchResult("output1").asInstanceOf[Tensor[Double]]
    val luaOutput2 = torchResult("output2").asInstanceOf[Tensor[Double]]
    val luaGradInput = torchResult("gradInput").asInstanceOf[Tensor[Double]]
    val luaOutput = T(luaOutput1, luaOutput2)

    output should be (luaOutput)
    gradInput should be (luaGradInput)
  }

}
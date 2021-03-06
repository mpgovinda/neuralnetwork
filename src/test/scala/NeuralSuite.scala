import neuralnetwork.NeuralNetwork._
import neuralnetwork._
import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NeuralSuite extends FunSuite {
    trait TestNetworks extends StringParserNeuralNetwork {
        def error(result: List[Double], exact: List[Double]): Double =
            (for ( (x, y) <- result zip exact) yield (x - y)*(x - y)).sum

        val epsilon = 0.0000001

        //neuron weights
        val neuronWithSingleInputWeight: NeuronWeights = List(0.5)
        val neuronWeights: NeuronWeights = List(0.0, 0.1, 0.2)
        val zeroNeuronWeights: NeuronWeights = List(0.0, 0.0, 0.0)
       
        
        //layer definitions
        val layerOfZeroes = new LinearLayer(List(zeroNeuronWeights))
        val layerWithOneNeuron = new LinearLayer(List(neuronWeights))
        val layerWithTwoNeurons = new LinearLayer(List(neuronWeights, neuronWeights))
        val layerWithNoBiasI2O1 = new LinearLayer(List(List(1.0, 0.5)), false)
        val layerWithIncorrectBiasI2O1 = new LinearLayer(List(List(666.0, 1.0, 0.5)), false)

        val endLayer = List(neuronWithSingleInputWeight)
        //weights definition
        val oneLayerOfZeroes = List(layerOfZeroes)
        val oneLayerWeights = List(layerWithOneNeuron)
        val oneLayerTwoNeuronsWeights = List(layerWithTwoNeurons)
        val twoLayersWithSingleNeuron = List(endLayer, layerWithOneNeuron)

        //neural network definitions
        val nn0 = new NeuralNetwork(oneLayerOfZeroes)
        val nn1 = new NeuralNetwork(oneLayerWeights)
        val nn12 = new NeuralNetwork(oneLayerTwoNeuronsWeights)
        val nnNoBiasI2O1 = new NeuralNetwork(List(layerWithNoBiasI2O1))
        val nnNoBiasIncorrectI2O1 = new NeuralNetwork(List(layerWithIncorrectBiasI2O1))

        //test inputs
        val input = List(0.0, 1.0, 2.0)

        val weightsString =
            """linear 0.0 0.1 0.2 | 0.0 0.1 0.2
              |0.0 0.1 0.2 | 0.0 0.1 0.2""".stripMargin
    }

    test("psp - scalar product") {
        new TestNetworks {
            assert(new LinearLayer(List()).psp(input, input) === 5)
        }
    }

    test("string parser") {
        new TestNetworks {
            object TestParser extends StringParserNeuralNetwork {
                assert(weights(weightsString) === List(layerWithTwoNeurons, layerWithTwoNeurons))
            }
        }
    }

    test("file parser") {
        new TestNetworks {
            val parser = new FileParserNeuralNetwork("src/test/resources/weights.txt")
            assert(parser.weightsFromFile === List(layerWithTwoNeurons, layerWithTwoNeurons))
        }
    }
 
    test("no bias weights") {
        new TestNetworks {
            val parser = new FileParserNeuralNetwork("src/test/resources/noBiasWeights.txt")
            val output = List(new SigmoidLayer(List(List(0.1, 0.2),
                                                   List(0.1, 0.2)), false),
                              new SigmoidLayer(List(List(0.0, 0.1, 0.2),
                                                 List(0.0, 0.1, 0.2))))
	    
            assert(parser.weightsFromFile === output)
        }
    }

    test("return zero when weights are zero") {
        new TestNetworks {
            override val weightsString =
                """t 0.0 0.0 0.0 | 0.0 0.0 0.0 
                   thresh 0.0 0.0 0.0 | 0.0 0.0 0.0""".stripMargin
            val w = weights(weightsString)
            val nn = new NeuralNetwork(w)
            assert(nn.calculate(List(1.0, 1.0)) === List(0.0, 0.0))
        }
    }

    test("test one layer network") {
        new TestNetworks {
            override val weightsString = "l -0.5 0.4 0.3 | -0.2 0.1 0.0"
            val w = weights(weightsString)
            val nn = new NeuralNetwork(w)
            val result = nn.calculate(List(1.0, 1.0))
            val exact = List(1.2, 0.3)
            assert(error(result, exact) < epsilon)
        }
    }

    test("test non zero values") {
        new TestNetworks {
            override val weightsString =
                """l -0.3 0.2 0.1 | -0.3 0.2 0.1
                  l -0.1 0.2 0.3 | -0.1 0.2 0.3""".stripMargin
            val w = weights(weightsString)
            val nn = new NeuralNetwork(w)
            val result = nn.calculate(List(1.0, 1.0))
            val exact = List(0.48, 0.48)
            assert(error(result, exact) < epsilon)
        }
    }

    test("test check weights") {
        new TestNetworks {
            override val weightsString =
                """l 0.3 0.2 0.1 | 0.3 0.2 0.1
                  l 0.1 0.2 0.3""".stripMargin
            val w = weights(weightsString)
            val fa = (x: Double) => x
            intercept[java.lang.AssertionError] {
                val nn = new NeuralNetwork(w)
            }
        }
    }

    test("parse input") {
        new TestNetworks {
            val inputParser = new InputParser("src/test/resources/inputs.txt")
            assert(inputParser.inputsFromFile === List(List(1.0, 0.0), List(0.5, 0.5)))
        }
    }

    test("test layer without bias") {
        new TestNetworks {
            val result = nnNoBiasI2O1.calculate(List(1.0, 1.0))
            val exact = List(1.5)
            assert(error(result, exact) < epsilon)
        }
    }

    test("test layer without bias, but with bias weight provided") {
        new TestNetworks {
            intercept[Exception] {
                nnNoBiasIncorrectI2O1.calculate(List(1.0, 1.0))
            }
        }
    }

    test("random layer generator") {
        new TestNetworks {
            val generated = new RandomWeightsGenerator().randomLayer(3, 2)
            assert(generated.length === 2)
            assert(generated(0).length === 3)
            assert(generated(1).length === 3)

        }
    }

    test("random network generator") {
        new TestNetworks {
            val generated = new RandomWeightsGenerator().randomLayers(List(1,2,3))
            assert(generated.length === 2)
            assert(generated(0).layer.length === 1)
            assert(generated(0).layer(0).length === 3)
            assert(generated(1).layer.length === 2)
            assert(generated(1).layer(0).length === 4)
        }
    }

       

}

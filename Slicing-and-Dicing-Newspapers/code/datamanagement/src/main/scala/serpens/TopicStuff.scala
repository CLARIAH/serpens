package serpens
// https://github.com/juanmirocks/mallet/blob/master/src/cc/mallet/pipe/iterator/CsvIterator.java
//
// http://mallet.cs.umass.edu/topics-devel.php
//import sun.security.jca.GetInstance.Instance
//import java.util._
import TopicModeling.createInstanceStream
import cc.mallet.pipe._
import cc.mallet.topics._
//import java.util._
import java.io._

import cc.mallet.types.Instance
//import java.util
import java.util.regex._

import cc.mallet.pipe.Pipe
import cc.mallet.types._

import scala.collection.JavaConversions._
import scala.xml._

// Lees ook even dit: https://github.com/rstats-gsoc/gsoc2016/wiki/Integration-of-Text-Mining-and-Topic-Modeling-Tools

case class MalletOptions(
                          numTopics: Int,
                          numIterations: Int,
                          lowerCase: Boolean,
                          beta: Double,
                          modelFilename: Option[String] = None,
                          instanceFilename: Option[String] = None
                          )

object DefaultOptions extends MalletOptions(100, 50, true, 0.01)

object TopicStuff
{
  def createTopicModel(zipFileName: String, options:MalletOptions = DefaultOptions):(InstanceList,ParallelTopicModel) =
  {
    val pipeList = new java.util.ArrayList[Pipe]
    // Pipes: lowercase, tokenize, remove stopwords, map to features
    if (options.lowerCase) pipeList.add(new CharSequenceLowercase)
    pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")))
    //pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false))
    pipeList.add(new TokenSequence2FeatureSequence)

    val instances = new InstanceList(new SerialPipes(pipeList))

    val rand = new scala.util.Random
    val instanceIterator = createInstanceStream(zipFileName).filter(_ => rand.nextDouble() < 2).iterator

    instances.addThruPipe(instanceIterator)

    //  Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
    //  Note that the first parameter is passed as the sum over topics, while
    //  the second is the parameter for a single dimension of the Dirichlet prior.

    val numTopics = options.numTopics
    val model = new ParallelTopicModel(numTopics,  1.0, options.beta)

    scala.Console.err.println("now add instances..")

    model.addInstances(instances)
    model.setNumThreads(6)

    // Run the model for 50 iterations and stop (this is for testing only,
    //  for real applications, use 1000 to 2000 iterations)

    model.setNumIterations(options.numIterations)
    model.estimate()

    if (options.modelFilename.isDefined) {

      scala.Console.err.println(s"Saving model to ${options.modelFilename.get}")
      model.write(new File(options.modelFilename.get))
    }
    if (options.instanceFilename.isDefined)
      instances.save(new File(options.instanceFilename.get))

    (instances, model)
  }

  @throws[IOException]
  def printTopicWordWeights(model: ParallelTopicModel, out: PrintWriter): Unit = { // Probably not the most efficient way to do this...
    var topic = 0
    while (topic < model.numTopics) {
      var typeID:Integer = 0
      while (typeID < model.numTypes)
      {
        val topicCounts:Array[Int] = model.typeTopicCounts(typeID)
        var weight = model.beta
        var index = 0
        var break = false
        while (index < topicCounts.length && topicCounts(index) > 0 && !break) {
          val currentTopic = topicCounts(index) & model.topicMask
          if (currentTopic == topic) {
            weight += topicCounts(index) >> model.topicBits
            break = true //todo: break is not supported
          }
          index += 1
        }
        if (weight > model.beta)
          out.println(topic + "\t" + model.alphabet.lookupObject(typeID) + "\t" + (weight - model.beta))
        typeID += 1
      }
      topic += 1
    }
    out.close
  }

  def printDoc(model: ParallelTopicModel, k:Int):String =
  {
    var out = new java.util.Formatter(new java.lang.StringBuilder, java.util.Locale.US)
    //val dataAlphabet = instances.getDataAlphabet
    val dataAlphabet = model.getAlphabet
    val tokens = model.getData.get(k).instance.getData.asInstanceOf[FeatureSequence]
    val topics = model.getData.get(k).topicSequence

    (0 until tokens.getLength).foreach(position =>
        out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)),
        topics.getIndexAtPosition(position): java.lang.Integer))

    out.toString
  }

  def testTopicZero(instances: InstanceList, model:ParallelTopicModel):Unit =  {
    val topicZeroText = new StringBuilder
    val iterator = model.getSortedWords.get(0).iterator
    var rank = 0
    while (iterator.hasNext && rank < 5) {
      val idCountPair = iterator.next
      topicZeroText.append(instances.getDataAlphabet.lookupObject(idCountPair.getID) + " ")
      rank += 1
    }

    // Create a new instance named "test instance" with empty target and source fields.

    val testing = new InstanceList(instances.getPipe)
    val instance: Instance = new Instance(topicZeroText.toString, null, "test instance", null)
    testing.addThruPipe(instance)
    val inferencer = model.getInferencer
    val testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5)
    System.out.println("0\t" + testProbabilities(0))
  }

  def printTopics(instances: InstanceList, model:ParallelTopicModel):Unit = {
    val topicSortedWords = model.getSortedWords
    val topicDistribution = model.getTopicProbabilities(0)
    val numTopics = model.getNumTopics
    (0 until numTopics).foreach(
      topic => {
        val iterator = topicSortedWords.get(topic).iterator
        val out = new java.util.Formatter(new java.lang.StringBuilder, java.util.Locale.US)
        out.format("%d\t%.3f\t", topic: java.lang.Integer, topicDistribution(topic): java.lang.Double)
        var rank = 0
        while (iterator.hasNext && rank < 5) {
          val idCountPair = iterator.next
          out.format("%s (%.0f) ", instances.getDataAlphabet.lookupObject(idCountPair.getID), idCountPair.getWeight: java.lang.Double)
          rank += 1
        }
        System.out.println(out)
      }
    )
  }
}
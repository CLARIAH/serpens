package serpens
import java.io.{File, FileWriter, Writer, PrintWriter}

import cc.mallet.topics.ParallelTopicModel
import cc.mallet.types.Instance

import scala.xml.{Node, XML}
import java.nio.file.Path

/*
Kijk nog eens naar
https://github.com/marziehf/TS_Embeddings
https://github.com/jhlau/hdp-wsi
 */

object TopicModeling {

  def getMetadataField(doc: Node, fieldName: String):String =
    ((doc \\ "interpGrp").filter(n => (n \ "@type").toString == fieldName) \\ "interp" \ "@value").mkString("|")

  def makeInstanceFromDocument(p:Path, doc: Node): Instance =
  {
    val data = (doc \\ "text").text.toString
    val target = null

    val idno = getMetadataField(doc,"idno")
    val coverage = getMetadataField(doc,"profile.lexiconCoverage")

    val uri = p.getFileName.toString

    val instance = new Instance(data, target, uri, null)

    instance.setProperty("idno", idno)
    instance.setProperty("lexiconCoverage", coverage)
    instance
  }

  def createDocumentStream(pathToZip: String):Stream[(Path, Node)] =
  {
    val l = zipUtils.find(pathToZip, p =>  p.getFileName.toString.endsWith("xml"))
      .map(p => (p,XML.load(java.nio.file.Files.newInputStream(p))))
    l
  }

  def createInstanceStream(pathToZip: String):Stream[Instance] = createDocumentStream(pathToZip).map({case (p,n) => makeInstanceFromDocument(p,n)})

  import java.io.{IOException, PrintWriter}

  // copied because I only want word topic combinations with weight > beta
  // val beta = 100

  type TopicWordWeights = List[(Int,Seq[(String,Double)])]

  def twwFromFile(fileName: String):TopicWordWeights =
  {
    val m = scala.io.Source.fromFile(fileName).getLines().map(
      l =>
        {
          val cols:Array[String] = l.split("\\t")
          val t = cols(0).toInt
          val word:String = cols(1)
          val weight = cols(2).toDouble
          (t, word, weight)
        }
    ).toList.groupBy(_._1).mapValues(l => l.map(tup => (tup._2, tup._3)))
    scala.Console.err.println("Yep...")
    m.toList
  }

  def getWeight(model:ParallelTopicModel, topic: Integer, typeID: Integer) =
  {
    val topicCounts = model.typeTopicCounts(typeID)
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
    weight
  }

  def topicWordWeights(model: ParallelTopicModel):TopicWordWeights =
  {
    val tww = (0 until model.numTopics).toList.map(topic => {
      Console.err.println(s"at topic $topic")
      topic ->
        {
          object findWordAndWeight extends PartialFunction[Int, (String,Double)]
          {
            def isDefinedAt(i:Int):Boolean = getWeight(model, topic, i) > model.beta
            def apply(i:Int):(String,Double) = {
              val weight = getWeight(model, topic, i)
              (model.alphabet.lookupObject(i).toString, weight - model.beta)
            }
          }

          (0 until model.numTypes).toList.collect(findWordAndWeight)
      }
    }).toList
    Console.err.println("tww constructed")
    tww.foreach({case (t,s) => Console.err.println(s"$t -> ${s.size}")})
    tww
  }

  val T1 = 3
  val T2:Double = Math.pow(Math.E, T1)
  val drempel = 3

  def haaltDeDrempel(w:WordInfo):Boolean = w.topicEntropy < drempel
  def doeMaarNiet(w:WordInfo) = false

  case class WordInfo(word: String, frequency: Double, topicEntropy: Double, topicWeights: Seq[(Int,Double)])
  {
    def attraction(topicNumber:Int):Double = topicWeights.find(_._1==topicNumber).map(_._2/ frequency).getOrElse(0)
  }

  def printKeywords(tww: TopicWordWeights, out: Writer, filter: WordInfo => Boolean = doeMaarNiet) =
  {
    val byTopic:Map[Int, Seq[(Int, Double, WordInfo)]] = infoByTopic(infoByWord(tww))

    val exponent = 0 // 0.66

    byTopic.keys.toList.sortBy(identity).foreach(
      t => {
        val l = byTopic(t)
        val topicWeight = l.map(_._2).sum / 1e6

        println(s"Topic: $t")

        val sorted: Seq[(Int, Double, WordInfo)]= l.sortBy({ case (t,weight,wi) => -1 * Math.pow(weight,exponent) * wi.attraction(t) })

        val topje = sorted.take(50)

        val doeOokMaar = sorted.drop(50).filter({case (t,w,winfo) => filter(winfo)})
        val selected:Seq[(Int, Double, WordInfo)] = topje ++ doeOokMaar

        def show(wi: WordInfo) = f"${wi.word}%s:${Math.round(wi.frequency)}:${wi.attraction(t)}%.2e"

        out.write(s"\nTopic $t ($topicWeight): ${topje.map(x => x._3.word).mkString(" ")}\n")
        out.write(s"\t\t\t${doeOokMaar.map(x => show(x._3)).mkString(" ")}\n")
      }
    )
    out.close()
  }


  def infoByWord(tww: TopicWordWeights):Map[String, WordInfo] =
  {
    scala.Console.err.println("start grouping by word ... ")

    val byWord = tww.flatMap({ case (t,s) => s.toList.map( { case (word,weight) => (t, word, weight)} )}).groupBy(_._2)

    scala.Console.err.println("group by word done ... ")

    val wordInfo =
      byWord.filter({case (w,l) => l.size > 1 || l.size ==1 && l.head._3 > 1}).mapValues(l =>
      {
        val f =l.map(_._3).sum
        val topicEntropy = -1 * l.map(_._3).map(w => {val p = w/f;   p* Math.log(p)}).sum
        val topicWeights:List[(Int,Double)] = l.map(x => (x._1,x._3))
        WordInfo(l.head._2, f,topicEntropy, topicWeights.toList)
      })
    wordInfo
  }

  val exponent = 0 // 0.66
  def infoByTopic(wordInfos: Map[String,WordInfo]):Map[Int, Seq[(Int, Double, WordInfo)]] =
  {
    val s = wordInfos.values.toSeq
    // unpack and group by topic number
    val byTopic:Map[Int, Seq[(Int, Double, WordInfo)]] = s.filter(wi => wi.topicEntropy < drempel)
      .flatMap(winfo => winfo.topicWeights.map({case (t,weight) => (t,weight, winfo)}))
      .groupBy(_._1)
    // sort keyword relevance by a rather heuristic criterion
    byTopic.mapValues(l => l.sortBy({ case (t,weight,wi) => -1 * Math.pow(weight,exponent) * wi.attraction(t) }))
  }



  def printTwwFromFile(f: String, out:Writer, filter: WordInfo => Boolean = doeMaarNiet) = printKeywords(twwFromFile(f), out, filter)

  def printTww(f: String, out:Writer, filter: WordInfo => Boolean = doeMaarNiet) =
  {
    val model = ParallelTopicModel.read(new File(f))
    printKeywords(topicWordWeights(model), out, filter)
  }

  case class TopicModelInfo(model: ParallelTopicModel)
  {
    lazy val tww = topicWordWeights(model)
    lazy val byWord = infoByWord(tww)
    lazy val byTopic = infoByTopic(byWord)

    def bestKeywords(topic:Int) = byTopic(topic).sortBy(_._3.attraction(topic)).take(50).map(_._3.word)
  }

  def getInfo(model: ParallelTopicModel) = TopicModelInfo(model)

  def topicKeywordInfo(model:ParallelTopicModel):Map[Int, Seq[(Int, Double, WordInfo)]] =
  {
    val tww = topicWordWeights(model)
    val byWord = infoByWord(tww)
    val byTopic = infoByTopic(byWord)
    byTopic
  }

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val nTopics = if (args.size > 2) args(2).toInt else 100

    val malletOptions = DefaultOptions.copy(lowerCase=false, modelFilename = Some(args(1)), numTopics = nTopics)

    val (instances, model) = TopicStuff.createTopicModel(args(0), malletOptions)

    if (false) {
      TopicStuff.printTopicWordWeights(model, new PrintWriter("/tmp/tww.txt"))
      //model.getSortedWords
      model.printDocumentTopics(new File("/tmp/dt.txt"))
    }
  }
}

object PruneKeywordsFromTwwFile
{
  def main(args: Array[String]) = TopicModeling.printTwwFromFile(args(0), new FileWriter(args(1)),
    w => TestRDF.beestWoorden.contains(w.word) )
}

object PruneKeywords
{
  def main(args: Array[String]) = TopicModeling.printTww(args(0), new FileWriter(args(1)),
    w => TestRDF.beestWoorden.contains(w.word) )
}

object PrintWordWeights
{
  def main(args: Array[String]): Unit =
  {
    val model = ParallelTopicModel.read(new File(args(0)))
    TopicStuff.printTopicWordWeights(model, new PrintWriter(args(1)))
  }
}

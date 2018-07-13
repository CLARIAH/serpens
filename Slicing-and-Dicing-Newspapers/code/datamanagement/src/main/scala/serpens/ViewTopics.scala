package serpens
import java.io.{File, FileWriter, PrintWriter, Writer}

import TopicModeling.TopicModelInfo
import cc.mallet.topics.{ParallelTopicModel, TopicAssignment}
import cc.mallet.types.Instance

import scala.collection.JavaConversions._
import scala.xml.{Node, XML}

// dit dumpt alleen wat plain text
// voor visualisaties zie bijvoorbeeld http://agoldst.github.io/dfr-browser/
// ideetje: collocatietermen erbij

class ViewTopics(zipFile: String, modelPath: String)
{
  lazy val zipje = new KBZipje(zipFile)
  lazy val zipRoot = zipje.root
  lazy val model = ParallelTopicModel.read(new File(modelPath))
  lazy val assignments = model.getData

  lazy val documentHash = zipUtils.findPath(zipRoot).map(p => p.getFileName.toString -> p).toMap

  lazy val modelInfo = TopicModelInfo(model)

  val minDocumentLength = 200

  def findDocument(name: String):Option[Node] =
  {
    // replacement is extremely ugly and should change
    // ToDo save filename in instance list before topic modeling

    //val f = name.replaceAll(":", "_").replaceAll("_mpeg21_a", "_") + "_articletext.xml"
    //Console.err.println(f)

    val p = documentHash.get(name)
    val doc = p.map(x => XML.load(java.nio.file.Files.newInputStream(x)))
    // println(doc)
    doc
  }

  def documentTopicScores(): Seq[(String, Int, Map[Int, Double])] =
    model.getData.zipWithIndex.filter({ case (ta, i) => ta.topicSequence.getLength > minDocumentLength })
      .map({ case (ta, i) =>
        val instance = ta.instance
        val topicSequence = ta.topicSequence
        val currentDocTopics = topicSequence.getFeatures

        val topicCounts: Map[Int, Int] =
          currentDocTopics.groupBy(identity).mapValues(l => l.size)

        val denominator = topicCounts.keys.map(t => topicCounts(t) + model.alpha(t)).sum
        val normalized = topicCounts.map({ case (t, c) => (t, (model.alpha(t) + c) / denominator) })

        // Console.err.println(s"${instance.getName} ${normalized.toString}")
        (instance.getName.toString, i, normalized)
        //ta.topicDistribution.toLabelVector.getLabelAlphabet
      })

  lazy val queryTerms:Set[String] = zipje.properties.getProperty("query").split(",").toSet
  lazy val highlightMe:Set[String] = queryTerms.map(s => s.toLowerCase)

  def shouldHighlight(s: String):Boolean = highlightMe.exists(x => s.toLowerCase.contains(x))

  def printTopicInfoHTML() =
  {
    val byTopic = documentTopicScores().flatMap({ case (name, i, m) => m.map({case (t,w) => (name,i, t,w) })} )
      .groupBy(_._3)
      .mapValues(l => l.sortBy(-1 * _._4))

    val divjes = byTopic.keySet.toList.sortBy(identity).map(t =>
    {
      val l = byTopic(t)
      val topje = l.take(5).toList

      val topDocNames = l.take(10).map(_._1)
      val topDocNumbers = l.take(10).map(_._2)

      val docs = topDocNames.map(findDocument)
        .filter(_.isDefined)
        .map(_.get)
        .flatMap(n => n \\ "text")
        .map(t => Highlighting.highlight(t, shouldHighlight, true))

      val keywords = modelInfo.bestKeywords(t).mkString(" ")

      (<div>
        <h3>Topic
          {t}
        </h3>
        <h4>{keywords}</h4>
        <a onClick={s"toggleDiv('docs.$t')"} style="text-decoration:underline">Toon/verberg voorbeelddocumentjes</a>

      </div>, <div id={s"docs.$t"} style="display:none">
        {docs.map(d => <div><hr/>{d}</div>)}
      </div>)
    })

    val topicDivjes = divjes.map(_._1)
    val exampleDivjes = divjes.map(_._2)

    val scriptje =
      """
        |var lastDiv = 'none';
        |function toggleDiv(divId)
        |        {
        |          var divje = document.getElementById(divId);
        |          if (divje.style.display!='block')
        |          {
        |            divje.style.display='block';
        |             //divje.scrollIntoView();
        |             if (lastDiv != 'none' && lastDiv != divId)
        |             {
        |                 var prev = document.getElementById(lastDiv);
        |                 prev.style.display='none';
        |              }
        |             lastDiv = divId;
        |          } else
        |          {
        |            divje.style.display='none' ;
        |          }
        |       }
      """.stripMargin
    val page =
      <html>
        <head>
          <script type="text/javascript">
          {scriptje}
          </script>
        </head>
        <body>
          <div style="float:left; margin-right: 2em; background-color: lightblue; width: 40%; overflow-y:scroll; height:100%">
          {topicDivjes}
          </div>
          <div style="overflow-y:scroll; height:100%">{exampleDivjes}</div>
        </body>
      </html>
    println(page.toString.replaceAll("<title/>", "").replaceAll("&amp;&amp;", "&&"))
  }

  def printTopicInfo() =
  {
    // findDocument("ddd:010585903:mpeg21:a0023")
    // val keywordInfo = TopicModeling.topicKeywordInfo(model)

    val byTopic = documentTopicScores().flatMap({ case (name, i, m) => m.map({case (t,w) => (name,i, t,w) })} )
      .groupBy(_._3)
      .mapValues(l => l.sortBy(-1 * _._4))

    byTopic.keySet.toList.sortBy(identity).foreach(t => {

      val l = byTopic(t)
      val topje = l.take(5).toList

      val topDocNames = l.take(10).map(_._1)
      val topDocNumbers = l.take(10).map(_._2)

      val docs = topDocNames.map(findDocument).filter(_.isDefined)

      val keywords = modelInfo.bestKeywords(t).mkString(" ")

      // val instance = model.getData.get(0).instance

      //val taggedDocContent = TopicStuff.printDoc(model, topDocNumber)

      println((0 until 80).map(i => "#").mkString)

      println(s"\n#### Topic $t:  topdocs $topje\n\tKeywords: $keywords")

      docs.foreach(doc => println(s"#######\n${(doc.get \\ "text").text.toString}"))
      //println(s"###\n$taggedDocContent")
    })
  }
}

object ViewTopics
{
  val zipje = "/vol1/Diamant/Corpora/Kranten/kranten_pd_1870-4.tei.zip"

  def main(args: Array[String]):Unit =
  {
    val vt = new ViewTopics(zipFile=args(0), modelPath=args(1))
    vt.printTopicInfoHTML()
  }
}

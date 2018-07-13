package serpens
/*
 * Stuff for querying KB newspapers
*/

import scala.xml._
import java.io.{File, FileReader, PrintWriter}
import java.nio.file.Path
import java.nio.file._
import scala.collection.JavaConverters._




case class Settings(defaultServer: String="?", batchSize: Int=500, maxDocuments:Int=Int.MaxValue,
                    defaultStartDate: String="01-01-1800", defaultEndDate:String = "31-12-1939")
{
  
}

object SettingsObject extends Settings()
{
  def fromFile(f: String):Settings =
  {
    val p = new java.util.Properties
    p.load(new FileReader(f))
    val props = p.stringPropertyNames().asScala // p.propertyNames()//.asScala.toSet.map(x => x.toString)
    val map: Map[String,String] =  props.map( n => n -> p.getProperty(n.toString)).toMap

    Settings(
      map.getOrElse("defaultServer", SettingsObject.defaultServer),
      map.get("batchSize").map(_.toInt).getOrElse(SettingsObject.batchSize),
      map.get("maxDocuments").map(_.toInt).getOrElse(SettingsObject.maxDocuments),
      map.getOrElse("defaultStartDate", SettingsObject.defaultStartDate),
      map.getOrElse("defaultEndDate", SettingsObject.defaultEndDate)
    )
  }
}

object QueryKB extends QueryKB(SettingsObject.fromFile("conf/settings.conf"))

case class QueryKB(settings: Settings)
{
  import SRU._


  val defaultServer = settings.defaultServer

  val batchSize = settings.batchSize // 500
  val maxDocuments = settings.maxDocuments // Int.MaxValue
  val defaultStartDate = settings.defaultStartDate /// "01-01-1800"
  val defaultEndDate = settings.defaultEndDate// "31-12-1939"

  val defaultCollection = "DDD_artikel"

  val beesten = List("Adder", "Bever", "Beverrat", "Boommarter", "Bunzing", "Das",
    "Dennensnuitkever", "Fret", "Hermelijn", "Huismuis", "Konijn", "Lynx", "Muskusrat",
    "Otter", "Raaf", "Spreeuw", "Vos", "Wezel", "Wolf")


  def expandedQuery(term:String):SRUQuery =
  {
    val l = LexiconService.getWordforms(term)
    val l1 = if (l.contains(term.toLowerCase)) l else term.toLowerCase :: l
    wrapTextQuery(ListDisjunction(l1.map( x => SingleTerm(x))))
  }

  def get(url: String) = scala.io.Source.fromURL(url).mkString

  def getNumberOfResults(q:SRUQuery):Int =
  {
    val q0 = q.copy(startRecord=0,maximumRecords=1)
    val url = q0.mkURL()
    Console.err.println(url)
    val n = getNumberOfResultsFromURL(url)
    Console.err.println("number of matching documents:" + n + " for " + q.query)
    n
  }

  def getNumberOfResultsFromURL(url:String):Int =
  {
    val xml = XML.load(url)
    val n = (xml \\ "numberOfRecords").text.toInt
    n
  }

  /**
    * Return a list of pairs: first is article id (actually resolver URI), second is recordData containing metadata for the article
    * There might be millions, so we do not want to keep the metadata record XML nodes in memory all at once,
    * so we need to return a stream instead of a List
    *
  **/

  def matchingDocumentIdentifiers(q:SRUQuery, startPosition:Int=0, max:Int=Int.MaxValue):Stream[(String,Node)] =
  {
    def getMatchingDocumentIdentifiersForBatch(q:SRUQuery, start:Int, maximum:Int):Stream[(String,xml.Node)] =
    {
      Console.err.println(s"Get metadata for batch ($start,$maximum)")
      val q1 = q.copy(startRecord=start, maximumRecords=maximum)
      val xml = XML.load(q1.mkURL)
      for  {
        r <- (xml \\ "recordData").toStream
        id <- r \\ "identifier"}
        yield
          (id.text, r)
    }

    val n = math.min(getNumberOfResults(q),max)
    val maxEnd = startPosition + max
    val x = (startPosition to startPosition + n by batchSize).toStream.flatMap(start => getMatchingDocumentIdentifiersForBatch(q, start,
      Math.min(maxEnd-start,batchSize)))
    x
  }


  def splitStream[A](seq: Iterable[A], n: Int) =
  {
    (0 until n).map(i => seq.drop(i).sliding(1, n).flatten)
  }
}

object countKB
{
  import QueryKB._
  import SRU._
  val d = KBDownloader(Paths.get("aapje"))
  def main(args: Array[String]):Unit =
  {
    println(getNumberOfResults(args(0)))
  }
}

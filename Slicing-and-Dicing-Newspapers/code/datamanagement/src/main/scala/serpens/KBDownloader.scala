package serpens
import java.io.{File, PrintWriter}
import java.nio.file.Path

import scala.xml.{Elem, Node, XML}

object Storage
{
  val dir = "/datalokaal/Corpus/KBKranten/Download"

  def makeDecent(s: String) = s.replaceAll("[^A-Za-z0-9]","_")

  def xstore(subdir: String, id:String, metadata:Node, text:String) =
  {
    val fileName = id.replaceAll(".*urn=","").replaceAll(":","_") + ".xml"
    val xml = XML.loadString(text)
    val doc = <doc>{metadata}{xml}</doc>
    val f = new File(dir + "/" + subdir)
    if (!f.isDirectory()) f.mkdir()
    new PrintWriter(dir + "/" + makeDecent(subdir) + '/' + fileName) { write(doc.toString()); close }
  }

  def storeToPath(path: Path, id:String, metadata: Node, text:String): Unit =
  {
    val fileName = id.replaceAll(".*urn=","").replaceAll(":","_") + ".xml"

    val xml = XML.loadString(text)
    val doc = <doc>{metadata}{xml}</doc>
    val tei = toTEI.makeTEI(metadata.asInstanceOf[Elem], None, doc, id)
    val os = zipUtils.getWriter(path, fileName)
    os.write(tei.toString)
    os.close
  }

  def exists(subdir: String, id:String):Boolean =
  {
    val fileName = id.replaceAll(".*urn=","").replaceAll(":","_") + ".xml"
    val oPath = dir + "/" + makeDecent(subdir)+ '/' + fileName
    return new File(oPath).exists
  }
}

case class KBDownloader(storePath: Path) {

  import QueryKB._
  import SRU._

  def download(id: String, metadataRecord: Node, subdir: String) =
    try {
      {
        val txt = get(id)
        Storage.storeToPath(storePath, id, metadataRecord, txt)
        // println(s"document length for $id:" + txt.length())
      }
    } catch {
      case e: Exception => Console.err.println(s"nou hoor..., kan $id niet afhalen: " + e)
    }

  def downloadForTermList(l: List[String]) = l.par.map(s => downloadQueryResults(s))

  //b => matchingDocumentIdentifiers(singleWordQuery(b)).map({ case (i,m) => download(i,m,b) })

  def downloadQueryResults(s: String, start:Int=0, max:Int = Int.MaxValue) =
    for ((id, metadataRecord) <- matchingDocumentIdentifiers(s))
      download(id, metadataRecord, s)

  def downloadParString(searchTerm: String, start:Int=0, max:Int = Int.MaxValue) {
    val s0 = matchingDocumentIdentifiers(searchTerm,start,max)
    val split = splitStream(s0, 5)
    split.par.foreach(
      x =>
        for ((id, metadataRecord) <- x) {
          download(id, metadataRecord, searchTerm)
        }
    )
    storePath.getFileSystem.close // beetje raar als geen zip
  }

  def downloadPar(s: SRUQuery, start:Int=0, max:Int = Int.MaxValue) {
    val s0 = matchingDocumentIdentifiers(s,start,max)
    val split = splitStream(s0, 5)
    split.par.foreach(
      x =>
        for ((id, metadataRecord) <- x) {
          download(id, metadataRecord, SRU.toString(s))
        }
    )
    storePath.getFileSystem.close // beetje raar als geen zip
  }

  def test = {
    val aantallen = beesten.map(b => (b, getNumberOfResults(singleWordQuery(b))))

    println(aantallen)

    val aantallen1 = beesten.map(b => (b, getNumberOfResults(expandedQuery(b))))
    println(aantallen1)

    System.exit(0)
    val n = getNumberOfResults(wrapTextQuery(Phrase("de", "kool", "en", "de", "geit")))

    for ((id, metadataRecord) <- matchingDocumentIdentifiers(singleWordQuery("Konijn"))) {
      val basicMeta = List("date", "papertitle", "title").map(x => (metadataRecord \\ x).text).mkString("\t")
      download(id, metadataRecord, "Test")
    }
  }
}


object Download
{
  import QueryKB._
  import SRU._
  lazy val d = KBDownloader(zipUtils.getRootPath("/tmp/testOutput.zip"))

  def main_old(args: Array[String]):Unit =
  {
    d.downloadForTermList(beesten.filter(s => {val x:Int = getNumberOfResults(s); x >  35000 && x < 200000 }))
  }
  
  val expandRestricted: String => List[String]  =
    x => (LexiconService.getWordforms(x) ++ MolexService.getWordforms(x)).filter(QueryKB.getNumberOfResults(_) > 0)

  def main(args: Array[String]):Unit =
  {
    if (args.size >= 2)
      {
        val zipje = new KBZipje(args(0))
        val downloader = KBDownloader(zipje.root)
        val searchTerm = args(1)
        val expandedQuery = SRU.expandQuery(expandRestricted)(SingleTerm(searchTerm))
        zipje.setProperty("query", SRU.termsIn(expandedQuery).mkString(",")) // save query info in zipje
        zipje.setProperty("dateDownload", new java.util.Date().toString)
        zipje.setProperty("expandedQuery",expandedQuery.toString)
        val start = if (args.size > 2) args(2).toInt else 0
        val max = if (args.size > 3) args(3).toInt else Int.MaxValue
        zipje.setProperty("start",start.toString)
        zipje.setProperty("max",max.toString)
        downloader.downloadPar(expandedQuery, start, max)
      } else
    d.downloadPar("stinkzwam")
  }
}

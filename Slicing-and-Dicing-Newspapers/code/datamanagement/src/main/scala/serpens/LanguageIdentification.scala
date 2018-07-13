package serpens
/**
  * Created by jesse on 7/19/17.
  */
import com.cybozu.labs.langdetect.{Detector,DetectorFactory}
import ResourceManagement._
import scala.collection.JavaConversions._

// use the well-known java Langdetect library

object LanguageIdentification
{
  loadProfilesFromJar("profiles")

  import com.cybozu.labs.langdetect.DetectorFactory
  import com.cybozu.labs.langdetect.LangDetectException
  import com.cybozu.labs.langdetect.util.LangProfile
  import net.arnx.jsonic.JSONException
  import java.io.IOException
  import java.io.InputStream
  import java.util
  import java.util.regex.Pattern

  import java.util
  import java.util.regex.Pattern


  def munchResource(f:String):String =
  {
    val is = classOf[DetectorFactory].getResourceAsStream("/" + f);
    scala.io.Source.fromInputStream(is).getLines().mkString("\n")
  }

  @throws[LangDetectException]
  def loadProfilesFromJar(profileDirectory: String): Unit =
  {
    val pattern = Pattern.compile(profileDirectory + ".*[a-z].*")
    System.err.println("loading from " + profileDirectory)

    val listOfProfiles = ResourceManagement.getResources(pattern).map(munchResource)
    Console.err.println(listOfProfiles.size)
    try {
      DetectorFactory.loadProfile(listOfProfiles)
    } catch { case e => () }
    val langsize = listOfProfiles.size
  }

  def detectLanguage(s: String): String = try
  {
    val detector = DetectorFactory.create
    //if (usePriors) detector.setPriorMap(priorMap)
    detector.append(s)
    val lang = detector.detect
    //detector.getProbabilities();
    lang
  } catch {
    case e: Exception =>
      // e.printStackTrace();
      null
  }

  def main(args: Array[String]) =
  {
    println(detectLanguage("Bist schon verrueckt?"))
    println(detectLanguage("Tu es folle!?"))
    println(detectLanguage("Ben je nou helemaal betoeterd!?"))
  }
}

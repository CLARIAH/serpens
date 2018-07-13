package serpens
/**
  * Created by jesse on 7/19/17.
  */

import java.io.File
import java.io.IOException
import java.util
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.io.IOException
import java.util
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import scala.collection.JavaConverters._

object ResourceManagement
{
  def getResources(pattern: Pattern): List[String] =
  {
    val classPath = System.getProperty("java.class.path", ".")
    val classPathElements = classPath.split(":").toList
    classPathElements.flatMap(element => getResources(element, pattern))
  }

  private def getResources(element: String, pattern: Pattern):List[String] =
  {
    val file = new File(element)
    if (file.isDirectory)
      getResourcesFromDirectory(file, pattern)
    else
      getResourcesFromJarFile(file, pattern)
  }

  private def getResourcesFromJarFile(file: File, pattern: Pattern):List[String] =
  {
    val zf:ZipFile = new ZipFile(file)

    val e = zf.entries.asScala.toStream.map(_.getName).filter(pattern.matcher(_).matches).toList
    zf.close
    e
  }

  private def getResourcesFromDirectory(directory: File, pattern: Pattern):List[String] =
  {
    val fileList = directory.listFiles
    val f1 = fileList.filter(_.isDirectory).flatMap(getResourcesFromDirectory(_,pattern))
    val f2 = fileList.filter(!_.isDirectory).map(_.getCanonicalPath).filter(pattern.matcher(_).matches)
    (f1 ++ f2).toList
  }
}

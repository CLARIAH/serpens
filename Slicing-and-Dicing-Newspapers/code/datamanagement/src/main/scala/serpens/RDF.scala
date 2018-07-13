package serpens
import org.apache.jena.graph.Triple
import org.apache.jena.query._
import org.apache.jena.arq._
import org.apache.jena.rdf.model.Model
import org.apache.jena.sparql.engine.http.QueryEngineHTTP
import scala.collection.JavaConverters._

trait RDFNode
trait RDFLiteral[+T] extends RDFNode
trait RDFResource

case class IntLiteral(i:Int) extends RDFNode with RDFLiteral[Int]
case class StringLiteral(s:String) extends RDFNode with RDFLiteral[String]
case class DoubleLiteral(s:Double) extends RDFNode with RDFLiteral[Double]
case class Resource(uri:String) extends RDFNode with RDFResource
case class Predicate(uri:String) extends RDFNode with RDFResource

trait Statement
case class ObjectProperty(subject:RDFResource, predicate:Predicate, obj:RDFResource) extends Statement
case class DataProperty(subject:RDFResource, predicate:Predicate, obj:RDFLiteral[Any]) extends Statement

class X[A]
{
  val truth:A=>Boolean = a => true
}

case class SelectQuery[A](query: String, mapping: Map[String,String] => A, filter: A => Boolean = new X[A].truth)

case class RDF(serviceURI: String)
{
  //val serviceURI = "http://dbpedia.org/sparql"

  def convertTriple(t:Triple):Statement =
  {
    val subject = Resource(t.getSubject.toString)
    val predicate = Predicate(t.getPredicate.toString)
    val statement =
      if (t.getObject.isLiteral)
      {
        val o = (t.getObject).asInstanceOf[org.apache.jena.graph.Node_Literal]
        val tp = o.getLiteralDatatype
        val v = o.getLiteralValue.toString()
        DataProperty (
          subject,
          predicate,
          if (tp.toString.contains("integer")) IntLiteral(v.toInt) else  StringLiteral(v))
      } else
      {
        ObjectProperty(subject,predicate, Resource(t.getObject().toString()))
      }

    statement
  }

  def constructQuery(service: String=this.serviceURI, queryString: String):Stream[Statement] =
  {
    val query = QueryFactory.create(queryString)
    try
    {
      val qexec = new QueryEngineHTTP(service, query)
      val i =	qexec.execConstructTriples()
      val i1 = i.asScala.map(x => convertTriple(x)).toStream
      i1
    } catch {case ex: Exception => ex.printStackTrace(); Stream.Empty}
  }

  def selectQuery(service: String=this.serviceURI, queryString: String):Stream[Map[String,String]] =
  {
    val q = QueryFactory.create(queryString)

    val qexec = new QueryEngineHTTP(service, q)
    val results = qexec.execSelect.asScala.toStream.map(soln => soln.varNames().asScala.map(n => n -> soln.get(n).toString).toMap)

    results
  }

  def select[A](service: String=this.serviceURI, query: SelectQuery[A]):Stream[A] =
  {
    selectQuery(service, query.query).map(query.mapping).filter(query.filter)
  }

  def select[A](query: SelectQuery[A]):Stream[A] = select(this.serviceURI, query)

  def query(service: String, queryString: String) =
  {
    val query = QueryFactory.create(queryString)
    val start = System.currentTimeMillis()
    try
    {
      val qexec = new QueryEngineHTTP(service, query)


      if (query.isSelectType)
      {
        val results = qexec.execSelect
        while (results.hasNext)
        {
          val soln = results.nextSolution()
          System.out.println(soln)
          //RDFNode x = soln.get("varName") ;       // Get a result variable by name.
          //Resource r = soln.getResource("VarR") ; // Get a result variable - must be a resource
          //Literal l = soln.getLiteral("VarL") ;   // Get a result variable - must be a literal
        }
      } else if (query.isConstructType)
      {
        val i =	qexec.execConstructTriples()
        def i1 = i.asScala.map(convertTriple).toStream

        for (statement <- i1.take(100))
          println(statement)

      }
    } catch {case ex: Exception => ex.printStackTrace();}

    val end =  System.currentTimeMillis()
    System.err.println("Time: " + (end-start))
  }
}


package serpens
object TestRDF
{
  val example = "construct  {?x a \"<http://dog>\" } where {?x a ?Concept} LIMIT 10000"

  val allPrefixes =
    """
      |PREFIX diamant:    <http://rdf.ivdnt.org/schema/diamant#>
      |PREFIX lemon:  <http://lemon-model.net/lemon#>
      |PREFIX lexinfo: <http://www.lexinfo.net/ontology/2.0/lexinfo#>
      |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      |PREFIX prov: <http://www.w3.org/ns/prov#>
      |PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>
      |PREFIX ud: <http://universaldependencies.org/u/pos/>
      |PREFIX relation: <http://rdf.ivdnt.org/schema/diamant#relation/>
      |PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
      |PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      |
    """.stripMargin

  def getVariants(l: String) =
    s"""
       |$allPrefixes
       |select ?wf where
       			 | {
       			 |   graph ?g
       			 |   {
       			 |    ?f ontolex:writtenRep ?wf .
       			 |    ?e ontolex:lexicalForm ?f .
       			 |    ?e ontolex:canonicalForm ?lf .
       			 |    ?lf ontolex:writtenRep ?l .
             |    ?e ud: ud:NOUN
       			 |    values  ?l { "$l" }
       			 |   }
       			 | }
  """.stripMargin

  val getBeesten =
    s"""
      |$allPrefixes
      |select ?x ?y ?lc ?e ?p ?w
      |where
      |{
      |  graph ?g
      |  {
      |    ?x a skos:Concept .
      |    ?x rdfs:label ?y .
      |    ?lc ?p ?x .
      |    ?e ontolex:evokes ?lc  .
      |    { { ?e ontolex:lexicalForm ?f } union {?e ontolex:canonicalForm ?f} } .
      |    ?f ontolex:writtenRep ?w .
      |    filter (contains(str(?x), "nederlandsesoorten")) .
      |  }
      |}
    """.stripMargin

  case class RDFBeest(nsr: String, label: String, diamant: String, wordform: String)

  val bq = SelectQuery[RDFBeest](getBeesten,
    m => RDFBeest(m("x"), m("y"), m("e"), m("w"))
  )

  lazy val alleBeesten:Stream[RDFBeest] = diamant.select(bq)
  lazy val beestWoorden = alleBeesten.map(_.wordform).toSet

  val getAllLemmata =
    s"""
       |$allPrefixes
       |select ?e ?lwf ?p where
       |{
       |  graph ?g
       |  {
       |  ?e ontolex:canonicalForm ?lf .
       |  ?lf ontolex:writtenRep ?lwf .
       |  ?e ud: ?pos .
       |  ?pos rdfs:label ?p
       |  }
       |} limit 100
     """.stripMargin

  val lq = SelectQuery[String](getAllLemmata, m => m("e") + "=" + m("p") + ":" + m("lwf"))

  val diamantURI = "http://svprre02:8080/fuseki/tdb/sparql"

  lazy val diamant = new RDF(diamantURI)

  def main(args:Array[String]) =
  {
    val r = new RDF(diamantURI)
    println(beestWoorden.size)
    alleBeesten.groupBy(_.nsr).foreach(
      {
        case (k,v) =>
          println(k)
          val varianten= v.map(_.wordform).toSet
          varianten.foreach(b => {
            import SRU._
            val k = QueryKB.getNumberOfResults(b)
            println(s"\t$b\t$k")
          }
          )
      }
    )

    List("aap", "noot", "tijger", "wolf", "wezel").foreach(
      w => {

        val expansieQuery = SelectQuery[String](getVariants(w), m => m.values.head.toLowerCase, w => w.matches("^(\\p{L})*$"))

      })
  }
}
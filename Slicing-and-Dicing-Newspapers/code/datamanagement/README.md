Wat scriptjes om met KB data om te gaan


Building:

* Ensure JDK 8 and the scala build tool sbt (https://www.scala-sbt.org/) is installed
* Build a jar including dependencies with "sbt assembly"

* Download zoekresultaten in pseudoTEI met metadata: ```java -classpath ./dist/SerpensDownloader.jar serpens.Download /tmp/bonobo.zip bonobo```
** Er staat nu een bestandje properties.txt in de zip met daarin informatie over de zoekopdracht
* Concordanties (output moet nog wat gefatsoeneerd):  ```java -classpath ./dist/SerpensDownloader.jar  serpens KBKwic wisent```
* TSV  bestand maken uit zip met pseudoTEI'tjes ```java -classpath dist/SerpensDownloader.jar serpens.TSVWriter /tmp/bonobo.zip``` 
* Sample maken van grotere zip ```java serpens.Sample complete.zip sample.zip 500``` 
* Topic model maken (100 topics default): ```java serpens.TopicModeling datasets/hermelijn.zip datasets/hermelijn.mallet 30```
* Topic keywords: ```java serpens.PruneKeywords datasets/hermelijn.mallet hermelijn.kw```  
* Bestandje met topics en voorbeeldtekstjes: ```java serpens.ViewTopics datasets/hermelijn.zip datasets/hermelijn.mallet```
 
Medata en bestandsformat
* PseudoTEI (title zou eigenlijk head moeten zijn, wellicht nog meer)
* Metadata naar interne INT velden geconverteerd in listBibl id="inlMetadata"
* Door simpel profiletooltje toegevoegde data:

```
<pre>
  <listBibl id="profile">
          <interpGrp type="profile.numberUppercase">
            <interp value="109"/>
          </interpGrp>
          <interpGrp type="profile.numberInLexicon">
            <interp value="115"/>
          </interpGrp>
          <interpGrp type="profile.lexiconCoverage">
            <interp value="0.35714285714285715"/>
          </interpGrp>
          <interpGrp type="profile.numberOfTokens">
            <interp value="354"/>
          </interpGrp>
          <interpGrp type="profile.numberNumerical">
            <interp value="32"/>
          </interpGrp>
          <interpGrp type="profile.lang">
            <interp value="nl"/>
          </interpGrp>
   </listBibl>
</pre>
```

* de originele KB metadata in listBibl id="articleMetadata"
* Voorbeeldbestandje https://github.com/INL/Serpens/blob/master/data/ddd_010336852_mpeg21_a0107_ocr.xml

Configuratie:

* Zet een bestandje in conf/settings.conf
* Voorbeeld:
```
batchSize = 500
defaultStartDate = 01-01-1800
defaultEndDate = 31-12-1939
defaultCollection = DDD_artikel
defaultServer =  ???
```

* Minimale invulling: defaultServer


// documentatie: https://www.kb.nl/sites/default/files/docs/snelstart-anp_en.pdf
// https://github.com/KBNLresearch/KB-python-API
// http://digitopia.nl/workshop/cheatsheet.html
// https://github.com/renevoorburg/oai2linerec
 

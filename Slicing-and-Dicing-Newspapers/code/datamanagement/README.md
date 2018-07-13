Small utilities for KB data management


Building:

* Ensure JDK 8 and the scala build tool sbt (https://www.scala-sbt.org/) is installed
* Build a jar including dependencies with "sbt assembly"


Command line:

* Download search results to a zip containing articles with metadata converted to pseudo-TEI: ```java -classpath ./dist/SerpensDownloader.jar serpens.Download /tmp/bonobo.zip bonobo```
* from a zip with downloaded artictles to tab-separated file: ```java -classpath dist/SerpensDownloader.jar serpens.TSVWriter /tmp/bonobo.zip``` 
* Draw a random sample from a larger zip: ```java serpens.Sample complete.zip sample.zip 500``` 
* Create topic model  (100 topics default): ```java serpens.TopicModeling datasets/hermelijn.zip datasets/hermelijn.mallet 30```
* Print topic keywords: ```java serpens.PruneKeywords datasets/hermelijn.mallet hermelijn.kw```  
* Create HTML with topics and example documents: ```java serpens.ViewTopics datasets/hermelijn.zip datasets/hermelijn.mallet```
 
Article file format and included metadata 

* Pseudo-TEI (title should have head, etc)
* Metadata (in internal INT style) in listBibl[@id="inlMetadata"]
* Some profiling data (language, lexicon coveraged) is added in a separate section 

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

* the original KB metadata is in listBibl id="articleMetadata"
* Example: https://github.com/INL/Serpens/blob/master/data/ddd_010336852_mpeg21_a0107_ocr.xml

Configuration:

* Config file is conf/settings.conf
* Example:
```
batchSize = 500
defaultStartDate = 01-01-1800
defaultEndDate = 31-12-1939
defaultCollection = DDD_artikel
defaultServer =  ???
```

* Minimally supply a value for defaultServer

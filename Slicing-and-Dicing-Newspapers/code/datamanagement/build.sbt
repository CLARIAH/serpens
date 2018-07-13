name := "Serpens"

organization := "org.ivdnt" 

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions += "-target:jvm-1.8"

libraryDependencies += "org.jdbi" % "jdbi" % "2.78"

// https://mvnrepository.com/artifact/org.scala-lang.modules/scala-xml_2.11
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.6"

// https://mvnrepository.com/artifact/com.cybozu.labs/langdetect
libraryDependencies += "com.cybozu.labs" % "langdetect" % "1.1-20120112"

libraryDependencies += "org.apache.jena"  % "jena-core"  % "3.3.0"  exclude("com.fasterxml.jackson.core", "jackson-databind")
libraryDependencies += "org.apache.jena" % "jena-arq" % "3.3.0" exclude("com.fasterxml.jackson.core", "jackson-databind")


// https://mvnrepository.com/artifact/cc.mallet/mallet
libraryDependencies += "cc.mallet" % "mallet" % "2.0.8" // exclude("org.slf4j", "jcl-over-slf4j")


libraryDependencies  ++= Seq(
  // Last stable release
  "org.scalanlp" %% "breeze" % "0.13.2",
  
  // Native libraries are not included by default. add this if you want them (as of 0.7)
  // Native libraries greatly improve performance, but increase jar sizes. 
  // It also packages various blas implementations, which have licenses that may or may not
  // be compatible with the Apache License. No GPL code, as best I know.
  "org.scalanlp" %% "breeze-natives" % "0.13.2",
  
  // The visualization library is distributed separately as well.
  // It depends on LGPL code
  "org.scalanlp" %% "breeze-viz" % "0.13.2"
)

excludeDependencies += "commons-logging" % "commons-logging"

mainClass := Some("KBKwic")

unmanagedResourceDirectories in Compile += { baseDirectory.value / "src/main/resources" }


        

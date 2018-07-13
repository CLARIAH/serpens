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



excludeDependencies += "commons-logging" % "commons-logging"

mainClass := Some("KBKwic")

unmanagedResourceDirectories in Compile += { baseDirectory.value / "src/main/resources" }


        

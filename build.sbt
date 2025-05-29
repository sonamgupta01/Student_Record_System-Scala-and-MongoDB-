name := "StudentRecordSystem"
organization := "com.bda.project"
version := "0.1.0"

scalaVersion := "2.12.15"

// MongoDB dependencies
libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.7.1",
  "org.mongodb" % "bson" % "4.7.1"
)

// Logging dependencies - only logback
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime
)

// JSON handling
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.9.3"
)

// Main class setting
Compile / mainClass := Some("StudentApp")

// Compiler options
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

// Set the run options with very restrictive logging
run / fork := true
run / connectInput := true
run / javaOptions ++= Seq(
  "-Dfile.encoding=UTF-8",
  
  // Logback specific settings
  "-Dlogback.configurationFile=logback.xml",
  
  // Set Java logging to minimal
  "-Djava.util.logging.config.file=none",
  
  // Silence all MongoDB driver logging
  "-Dorg.mongodb.driver.level=OFF",
  
  // Java util logging - silence everything
  "-Djava.util.logging.level=OFF",
  
  // Set Log4j logging levels (used by some dependencies)
  "-Dlog4j.logger.org.mongodb=OFF",
  
  // System level properties for silent startup
  "-Dorg.slf4j.simpleLogger.defaultLogLevel=OFF"
)


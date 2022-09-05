
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.defaultLocal

val AkkaVersion = "2.6.19"
val echopraxiaVersion = "2.2.3-SNAPSHOT"
val echopraxiaPlusScalaVersion = "1.1.1"

// Need to make this work on 2.12 as well

lazy val actor = (project in file("actor")).settings(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion,

  // different styles of logger
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
  //libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % echopraxiaVersion % Test,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,

  // logger implementation
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
)

lazy val actorTyped = (project in file("actor-typed")).settings(
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,

  libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  // different styles of logger
  //libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % echopraxiaVersion % Test,

  // logger implementation
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion % Test,
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
).dependsOn(actor)

lazy val akkaStream = (project in file("akka-stream")).settings(
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "nameof" % echopraxiaPlusScalaVersion % Test,

  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,

  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "echopraxia-plusakka"
  ).aggregate(actor, actorTyped, akkaStream)


ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.defaultLocal

val AkkaVersion = "2.6.19"
val echopraxiaVersion = "2.2.0"
val echopraxiaPlusScalaVersion = "1.1.0"

lazy val actor = (project in file("actor")).settings(
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion,

  // different styles of logger
  //libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % echopraxiaVersion % Test,

  // logger implementation
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
)

lazy val actorTyped = (project in file("actor-typed")).settings(
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,

  // different styles of logger
  //libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % echopraxiaVersion % Test,

  // logger implementation
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
).dependsOn(actor)

lazy val akkaStream = (project in file("akka-stream")).settings(
  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion
)

lazy val root = (project in file("."))
  .settings(
    name := "echopraxia-plusakka"
  ).aggregate(actor, actorTyped, akkaStream)

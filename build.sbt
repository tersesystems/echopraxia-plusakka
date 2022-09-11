
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.defaultLocal

val AkkaVersion = "2.6.19"
val echopraxiaVersion = "2.2.2"
val echopraxiaPlusScalaVersion = "1.1.1"

// XXX Need to make this work on 2.12 as well

// Ensure that there is one and only one logback-test.xml file visible in testing
// https://youtrack.jetbrains.com/issue/SCL-16316/Test-runners-calculate-incorrect-test-classpath-include-dependencies-test-classpaths
lazy val logging = (project in file("logging")).settings(
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,

  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion % Test,
  // logger implementation
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test
) // XXX needs to be NoPublish

lazy val actor = (project in file("actor")).settings(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion,

  // different styles of logger
  //libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % echopraxiaVersion % Test,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
).dependsOn(logging % "test->test")

lazy val actorTyped = (project in file("actor-typed")).settings(
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,

  libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
).dependsOn(actor, logging % "test->test")

lazy val stream = (project in file("akka-stream")).settings(
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  //
  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  //
  libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion % Test,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
).dependsOn(actor, logging % "test->test")

lazy val root = (project in file("."))
  .settings(
    name := "echopraxia-plusakka"
  ).aggregate(actor, actorTyped, stream)

import sbt.Keys._

initialize := {
  val _        = initialize.value // run the previous initialization
  val required = "11"
  val current  = sys.props("java.specification.version")
  assert(current >= required, s"Unsupported JDK: java.specification.version $current != $required")
}

ThisBuild / organization := "com.tersesystems.echopraxia.plusakka"
ThisBuild / homepage     := Some(url("https://github.com/tersesystems/echopraxia-plusakka"))

ThisBuild / startYear := Some(2022)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/tersesystems/echopraxia-plusakka"),
    "scm:git@github.com:tersesystems/echopraxia-plusakka.git"
  )
)

ThisBuild / versionScheme := Some("early-semver")

//ThisBuild / resolvers += Resolver.mavenLocal
//ThisBuild / resolvers += Resolver.defaultLocal

// https://github.com/akka/akka/issues/31064#issuecomment-1060871255
ThisBuild / libraryDependencySchemes +=
  "org.scala-lang.modules" %% "scala-java8-compat" % VersionScheme.Always

val AkkaVersion = "2.6.20"
val echopraxiaVersion = "2.2.2"
val echopraxiaPlusScalaVersion = "1.1.0"

lazy val scala213 = "2.13.9"
lazy val scala212 = "2.12.16"
val supportedScalaVersions = Seq(scala212, scala213)

val NoPublish = Seq(
  //Compile / doc := false,
  publish / skip := true
)

// Ensure that there is one and only one logback-test.xml file visible in testing
// https://youtrack.jetbrains.com/issue/SCL-16316/Test-runners-calculate-incorrect-test-classpath-include-dependencies-test-classpaths
lazy val logging = (project in file("logging")).settings(NoPublish).settings(
  crossScalaVersions := supportedScalaVersions,
  //
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11" % Test,
  libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2" % Test
)

lazy val actor = (project in file("actor")).settings(
  name := "akka-actor",
  crossScalaVersions := supportedScalaVersions,
  //
  libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion,
  //
  // different styles of logger
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.tersesystems.echopraxia" % "logger" % "2.2.2" % Test,
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
).dependsOn(logging % "test->test")

lazy val actorTyped = (project in file("actor-typed")).settings(
  name := "akka-actor-typed",
  crossScalaVersions := supportedScalaVersions,
  //
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,

  libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
).dependsOn(actor, logging % "test->test")

lazy val stream = (project in file("akka-stream")).settings(
  name := "akka-stream",
  crossScalaVersions := supportedScalaVersions,
  //
  libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
  //
  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  //
  libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion % Test,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
).dependsOn(actor, logging % "test->test")

lazy val root = (project in file(".")).settings(NoPublish)
  .settings(
    crossScalaVersions := Nil,
    name := "echopraxia-plusakka"
  ).aggregate(actor, actorTyped, stream)

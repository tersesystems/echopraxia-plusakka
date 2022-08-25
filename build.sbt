
ThisBuild / version := "1.0.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.defaultLocal

val AkkaVersion = "2.6.19"
val echopraxiaVersion = "2.2.0"
val echopraxiaPlusScalaVersion = "1.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "echopraxia-plusakka",

    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,

    // different styles of logger
    libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "api" % echopraxiaPlusScalaVersion,
    libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "logger" % echopraxiaPlusScalaVersion,
    libraryDependencies += "com.tersesystems.echopraxia.plusscala" %% "generic" % echopraxiaPlusScalaVersion,

    libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "7.2",

    // logger implementation
    libraryDependencies += "com.tersesystems.echopraxia" % "logstash" % echopraxiaVersion % Test,
    
  )

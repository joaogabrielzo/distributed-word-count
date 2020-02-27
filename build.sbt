name := "distributed-word-counter"

version := "0.1"

scalaVersion := "2.12.10"

logLevel := Level.Error

val akkaVersion = "2.5.13"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    )

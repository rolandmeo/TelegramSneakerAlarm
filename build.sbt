/*
 * Author: Roland Meo
 */

name := "AkkaStreams"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += Resolver.typesafeIvyRepo("releases")


libraryDependencies ++= Seq(
  // Akka Stuff
  "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.16" % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.5.16",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.16" % Test,
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.5" % Test,
  // Telegram Stuff
  "info.mukel" %% "telegrambot4s" % "3.0.15",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  // Browser und WebScrapping
  "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
  // Emojis!
  "com.lightbend" %% "emoji" % "1.2.0",
  // String diffs..
  "org.apache.commons" % "commons-lang3" % "3.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
)
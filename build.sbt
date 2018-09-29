name := "AkkaStreams"

version := "0.1"
/*
 * Author: Roland Meo
 */

scalaVersion := "2.12.6"

// Current version is an early RC.
// resolvers += Resolver.sonatypeRepo("staging")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.16",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.16" % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.5.16",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.16" % Test,
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.5" % Test,
  // Core with minimal dependencies, enough to spawn your first bot.
  // "com.bot4s" %% "telegram-akka" % "4.0.0-RC1",
  // Extra goodies: Webhooks, support for games, bindings for actors.
  // "com.bot4s" %% "telegram-akka" % "4.0.0-RC1"
  "info.mukel" %% "telegrambot4s" % "3.0.15",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
  "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
  "org.gnieh" %% "diffson-spray-json" % "3.0.0"
)
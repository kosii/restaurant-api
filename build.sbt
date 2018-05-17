name := "restaurant-api"

version := "0.1"

scalaVersion := "2.12.6"


val circeVersion = "0.9.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",
  "com.typesafe" % "config" % "1.3.3",
  "com.google.guava" % "guava" % "24.1-jre",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % "test"
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

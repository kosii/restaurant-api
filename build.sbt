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
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.3",
  "org.mockito" % "mockito-core" % "2.18.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % "test"
)

fork in Test := true

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

enablePlugins(JavaAppPackaging)

dockerExposedPorts += 8080
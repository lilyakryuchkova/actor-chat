name := "chat-tutorial"

version := "1.0"

scalaVersion := "2.12.3"

lazy val akkaVersion = "2.5.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,

  "com.typesafe.akka" %% "akka-http" % "10.0.10"
)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

libraryDependencies += "junit" % "junit" % "4.10" % "test"
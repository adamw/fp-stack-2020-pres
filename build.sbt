lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.fp",
  scalaVersion := "2.13.2"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.1.2" % "test"

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "fp-stack-2020-pres")
  .aggregate(core)

val doobieVersion = "0.9.0"
val tapirVersion = "0.14.5"

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-quill" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion,
      "dev.zio" %% "zio" % "1.0.0-RC18-2",
      "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC13",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      scalaTest
    )
  )

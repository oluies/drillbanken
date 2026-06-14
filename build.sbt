import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / organization := "drillbanken"

lazy val domain = project
  .in(file("modules/domain"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "drillbanken-domain",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % "1.0.4" % Test,
      "org.scalameta" %%% "munit-scalacheck" % "1.0.0" % Test
    )
  )

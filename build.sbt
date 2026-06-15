import org.scalajs.linker.interface.ModuleKind

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / organization := "drillbanken"

val laminarV = "17.2.0"
val munitV = "1.0.4"
val munitScalacheckV = "1.0.0"

// Pure, framework-free pedagogy core (Principle III). No Laminar/DOM imports.
lazy val domain = project
  .in(file("modules/domain"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "drillbanken-domain",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitScalacheckV % Test
    )
  )

// Lesson content as typed in-bundle objects (Principle V). Pure; depends only on domain.
lazy val content = project
  .in(file("modules/content"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domain)
  .settings(
    name := "drillbanken-content",
    libraryDependencies += "org.scalameta" %%% "munit" % munitV % Test
  )

// DuckDB-WASM service core (Laminar-free) behind the narrow EngineService (T010-T012).
// Interop via hand-written js.native facades (see ADR in research.md D2 — ScalablyTyped
// OOM'd the Scala compiler on the duckdb-wasm + Arrow facade surface).
lazy val engine = project
  .in(file("modules/engine"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domain)
  .settings(
    name := "drillbanken-engine",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0"
  )

// xterm.js console adapter (facade + Laminar component) behind ConsoleService (T013).
lazy val console = project
  .in(file("modules/console"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domain)
  .settings(
    name := "drillbanken-console",
    libraryDependencies += "com.raquo" %%% "laminar" % laminarV
  )

// The single Scala.js application Vite bundles. ESModule output for vite-plugin-scalajs.
lazy val app = project
  .in(file("modules/app"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(domain, engine, console, content)
  .settings(
    name := "drillbanken-app",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)),
    libraryDependencies += "com.raquo" %%% "laminar" % laminarV
  )

lazy val root = project
  .in(file("."))
  .aggregate(domain, content, engine, console, app)
  .settings(name := "drillbanken", publish / skip := true)

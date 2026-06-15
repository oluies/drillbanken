package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.engine.EngineService
import drillbanken.console.{ConsoleService, XtermConsoleService}
import drillbanken.content.{Curriculum, SeedData, SeedRef}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Application entrypoint and Laminar shell. Boots DuckDB-WASM, surfaces `EngineStatus` in
  * a Signal, then drives the first lesson's full loop through the console (User Story 1).
  */
object Main:

  private val language: Language = Language.En

  private def statusText(s: EngineStatus): String = s match
    case EngineStatus.Loading        => "Loading…"
    case EngineStatus.Ready(version) => s"Ready — DuckDB $version"
    case EngineStatus.Failed(msg)    => s"Failed — $msg"

  def main(args: Array[String]): Unit =
    val statusVar = Var[EngineStatus](EngineStatus.Loading)
    val engine = EngineService.duckDb(SeedData.forRef(SeedRef.TradingBook))
    val consoleSvc = new XtermConsoleService()
    var controller: Option[LessonController] = None

    val consoleMount = div(idAttr := "console")
    val view = div(
      h1("Drillbänken"),
      p("Konsol-SQL-tutor · console SQL tutor"),
      p(em(child.text <-- statusVar.signal.map(s => "Engine: " + statusText(s)))),
      consoleMount
    )
    val container = Option(dom.document.getElementById("app")).getOrElse {
      val el = dom.document.createElement("div")
      el.setAttribute("id", "app")
      dom.document.body.appendChild(el)
      el
    }
    render(container, view)
    consoleSvc.open(consoleMount.ref)

    // Route every submitted line to the active lesson controller (FR-009/FR-010).
    consoleSvc.onSubmit.foreach { line =>
      controller.foreach(_.handle(line))
    }(unsafeWindowOwner)

    engine.boot().foreach { _ =>
      statusVar.set(engine.currentStatus)
      engine.currentStatus match
        case EngineStatus.Ready(v) =>
          dom.console.log(s"SPIKE:READY $v")
          Curriculum.all.headOption match
            case Some(lesson) =>
              val c = new LessonController(engine, consoleSvc, lesson, language)
              controller = Some(c)
              c.start()
            case None =>
              consoleSvc.writeLine("No lessons available.")
        case EngineStatus.Failed(msg) =>
          consoleSvc.writeLine(s"engine failed: $msg"); dom.console.log(s"SPIKE:FAILED $msg")
        case EngineStatus.Loading => ()
    }

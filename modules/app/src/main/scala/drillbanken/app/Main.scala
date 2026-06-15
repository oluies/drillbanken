package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.engine.EngineService
import drillbanken.console.{ConsoleService, XtermConsoleService}
import drillbanken.content.{SeedData, SeedRef}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/** Milestone 0 interop spike (T014). Boots DuckDB-WASM, surfaces `EngineStatus` in a
  * Laminar `Signal`, runs the two acceptance queries into the xterm console, and wires
  * interactive SQL input. The spike's acceptance is in `quickstart.md` (T015).
  */
object Main:

  private def statusText(s: EngineStatus): String = s match
    case EngineStatus.Loading        => "Loading…"
    case EngineStatus.Ready(version) => s"Ready — DuckDB $version"
    case EngineStatus.Failed(msg)    => s"Failed — $msg"

  private def renderTable(c: ConsoleService, qr: QueryResult): Unit =
    c.writeLine(qr.cols.mkString(" | "))
    c.writeLine(qr.cols.map(_ => "---").mkString("-+-"))
    qr.rows.foreach(row => c.writeLine(row.map(_.getOrElse("∅")).mkString(" | ")))

  def main(args: Array[String]): Unit =
    val statusVar = Var[EngineStatus](EngineStatus.Loading)
    val engine = EngineService.duckDb(SeedData.forRef(SeedRef.TradingBook))
    val consoleSvc = new XtermConsoleService()

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

    // Interactive: every submitted line runs as SQL (FR-009).
    consoleSvc.onSubmit.foreach { sql =>
      if sql.trim.nonEmpty then
        engine.exec(sql).onComplete {
          case Success(qr) => renderTable(consoleSvc, qr)
          case Failure(e)  => consoleSvc.writeLine(s"error: ${e.getMessage}")
        }
    }(unsafeWindowOwner)

    // Boot, then the two spike acceptance checks (T015).
    engine.boot().foreach { _ =>
      statusVar.set(engine.currentStatus)
      engine.currentStatus match
        case EngineStatus.Ready(v) =>
          consoleSvc.writeLine(s"DuckDB $v ready — running spike checks…")
          dom.console.log(s"SPIKE:READY $v")
          engine.exec("SELECT 42 AS answer").onComplete {
            case Success(qr) =>
              renderTable(consoleSvc, qr)
              dom.console.log(s"SPIKE:QUERY ${qr.cols.mkString(",")}=${qr.rows.map(_.map(_.getOrElse("∅")).mkString).mkString}")
            case Failure(e) =>
              consoleSvc.writeLine(s"unexpected: ${e.getMessage}")
              dom.console.log(s"SPIKE:QUERY-FAIL ${e.getMessage}")
          }
          engine.exec("SELECT * FROM does_not_exist").onComplete {
            case Failure(e) =>
              consoleSvc.writeLine(s"typed error OK → ${e.getMessage}")
              dom.console.log(s"SPIKE:ERROR ${e.getMessage}")
            case Success(_) =>
              consoleSvc.writeLine("unexpected success on bad query")
              dom.console.log("SPIKE:ERROR-MISSING")
          }
        case EngineStatus.Failed(msg) =>
          consoleSvc.writeLine(s"engine failed: $msg")
          dom.console.log(s"SPIKE:FAILED $msg")
        case EngineStatus.Loading => ()
    }

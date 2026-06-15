package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.engine.EngineService

/** Application entrypoint and Laminar shell. This is the Milestone 0 starting point: it
  * mounts a placeholder and shows the (currently pending) engine status. The interop spike
  * replaces the placeholder with the live console + DuckDB-WASM boot:
  *   - TODO(T011): construct the real EngineService and lift `currentStatus` into a Signal
  *   - TODO(T014): mount the ConsoleService, run a query into a Laminar table, surface a
  *     deliberate error as a typed Failed value
  */
object Main:

  private def statusText(s: EngineStatus): String = s match
    case EngineStatus.Loading        => "Loading…"
    case EngineStatus.Ready(version) => s"Ready — DuckDB $version"
    case EngineStatus.Failed(msg)    => s"Failed — $msg"

  def main(args: Array[String]): Unit =
    val engine: EngineService = EngineService.pending // TODO(T011): real engine

    val view =
      div(
        h1("Drillbänken"),
        p("Konsol-SQL-tutor · console SQL tutor"),
        p(
          "Interop spike pending (Milestone 0). See ",
          code("specs/002-console-sql-tutor/quickstart.md"),
          "."
        ),
        p(em("Engine status: " + statusText(engine.currentStatus)))
      )

    val container = Option(dom.document.getElementById("app")).getOrElse {
      val el = dom.document.createElement("div")
      el.setAttribute("id", "app")
      dom.document.body.appendChild(el)
      el
    }
    render(container, view)

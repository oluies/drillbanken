package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.domain.progress.{ProgressState, LessonProgress, Progression}
import drillbanken.domain.loop.{LessonState, LessonStatus}
import drillbanken.engine.EngineService
import drillbanken.content.{Curriculum, SeedData, SeedRef}
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Entrypoint + Laminar shell (constitution v2.0.0 — guided web GUI). Boots DuckDB-WASM,
  * loads persisted progress, then mounts the lesson view (resuming if a snapshot exists).
  */
object Main:

  private def statusText(s: EngineStatus): String = s match
    case EngineStatus.Loading        => "Loading…"
    case EngineStatus.Ready(version) => s"Ready — DuckDB $version"
    case EngineStatus.Failed(msg)    => s"Failed — $msg"

  def main(args: Array[String]): Unit =
    val persist = new PersistenceService()
    val firstLessonId = Curriculum.first.map(_.id).getOrElse(LessonId("none"))
    var progress: ProgressState = persist.load(firstLessonId, Language.En)
    val language = progress.language

    val statusVar = Var[EngineStatus](EngineStatus.Loading)
    val engine = EngineService.duckDb(SeedData.forRef(SeedRef.TradingBook))
    val slot = Var[Element](div(cls := "loading", "Loading DuckDB-WASM…"))

    // Export/import hooks for the UI and for headless verification (FR-023).
    val g = js.Dynamic.global.window
    g.drillExport = (() => persist.exportJson(progress)): js.Function0[String]
    g.drillImport = ((json: String) =>
      persist.importFrom(json) match
        case Right(p) => progress = p; persist.save(p); true
        case Left(_)  => false
    ): js.Function1[String, Boolean]

    val view = div(
      cls := "app",
      h1("Drillbänken"),
      p(cls := "status", em(child.text <-- statusVar.signal.map(s => "Engine: " + statusText(s)))),
      child <-- slot.signal
    )
    val container = Option(dom.document.getElementById("app")).getOrElse {
      val el = dom.document.createElement("div"); el.setAttribute("id", "app")
      dom.document.body.appendChild(el); el
    }
    render(container, view)

    engine.boot().foreach { _ =>
      statusVar.set(engine.currentStatus)
      engine.currentStatus match
        case EngineStatus.Ready(v) =>
          dom.console.log(s"SPIKE:READY $v")
          Curriculum.all.headOption match
            case Some(lesson) =>
              val nextId = Curriculum.all.dropWhile(_.id != lesson.id).drop(1).headOption.map(_.id)
              val resume = progress.lessons.get(lesson.id).flatMap(_.resume)
              val lv = new LessonView(
                engine, lesson, language, resume,
                onTransition = st => {
                  val prev = progress.lessons.getOrElse(lesson.id, LessonProgress.empty)
                  val snap = if st.status == LessonStatus.InProgress then Some(st) else None
                  progress = progress.copy(lessons = progress.lessons.updated(lesson.id, prev.copy(resume = snap)))
                  persist.save(progress)
                },
                onGraded = grade => {
                  progress = Progression.applyGrade(progress, lesson.id, nextId, grade)
                  persist.save(progress)
                },
                onLanguageChange = l => { progress = progress.copy(language = l); persist.save(progress) }
              )
              slot.set(lv.node)
              lv.start()
            case None => slot.set(div("No lessons available."))
        case EngineStatus.Failed(msg) =>
          slot.set(div(s"Engine failed: $msg")); dom.console.log(s"SPIKE:FAILED $msg")
        case EngineStatus.Loading => ()
    }

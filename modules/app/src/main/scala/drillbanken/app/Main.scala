package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.domain.progress.{ProgressState, LessonProgress, Progression}
import drillbanken.domain.loop.{LessonState, LessonStatus}
import drillbanken.engine.EngineService
import drillbanken.console.{ConsoleService, XtermConsoleService}
import drillbanken.content.{Curriculum, SeedData, SeedRef}
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** Entrypoint + Laminar shell. Boots DuckDB-WASM, loads persisted progress, then drives
  * the first lesson — resuming mid-lesson if a snapshot exists (US3) and persisting
  * progress + export/import (US3/US6).
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
    val consoleSvc = new XtermConsoleService()
    var controller: Option[LessonController] = None

    // Export/import hooks for the UI and for headless verification (FR-023, T056).
    val g = js.Dynamic.global.window
    g.drillExport = (() => persist.exportJson(progress)): js.Function0[String]
    g.drillImport = ((json: String) =>
      persist.importFrom(json) match
        case Right(p) => progress = p; persist.save(p); true
        case Left(_)  => false
    ): js.Function1[String, Boolean]

    val consoleMount = div(idAttr := "console")
    val view = div(
      h1("Drillbänken"),
      p("Konsol-SQL-tutor · console SQL tutor"),
      p(em(child.text <-- statusVar.signal.map(s => "Engine: " + statusText(s)))),
      consoleMount,
      SchemaPanel.view
    )
    val container = Option(dom.document.getElementById("app")).getOrElse {
      val el = dom.document.createElement("div"); el.setAttribute("id", "app")
      dom.document.body.appendChild(el); el
    }
    render(container, view)
    consoleSvc.open(consoleMount.ref)

    consoleSvc.onSubmit.foreach { line =>
      dom.console.log(s"SUBMIT:$line")
      controller.foreach(_.handle(line))
    }(unsafeWindowOwner)

    engine.boot().foreach { _ =>
      statusVar.set(engine.currentStatus)
      engine.currentStatus match
        case EngineStatus.Ready(v) =>
          dom.console.log(s"SPIKE:READY $v")
          Curriculum.all.headOption match
            case Some(lesson) =>
              val nextId = Curriculum.all.dropWhile(_.id != lesson.id).drop(1).headOption.map(_.id)
              val resume = progress.lessons.get(lesson.id).flatMap(_.resume)
              val c = new LessonController(
                engine, consoleSvc, lesson, language, resume,
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
                onLanguageChange = l => {
                  progress = progress.copy(language = l) // persist choice; progress intact (SC-011)
                  persist.save(progress)
                }
              )
              controller = Some(c)
              c.start()
            case None => consoleSvc.writeLine("No lessons available.")
        case EngineStatus.Failed(msg) =>
          consoleSvc.writeLine(s"engine failed: $msg"); dom.console.log(s"SPIKE:FAILED $msg")
        case EngineStatus.Loading => ()
    }

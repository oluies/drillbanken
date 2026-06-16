package drillbanken.app

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import drillbanken.domain.*
import drillbanken.domain.loop.*
import drillbanken.domain.check.Check
import drillbanken.domain.grade.Grading
import drillbanken.engine.EngineService
import drillbanken.content.LessonDef
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The guided web UI for one lesson (constitution v2.0.0). Drives the pure domain `Loop`
  * with Laminar signals; replaces the xterm console. Reuses Check/Grading/Progression.
  */
final class LessonView(
    engine: EngineService,
    lesson: LessonDef,
    lang0: Language,
    resume: Option[LessonState] = None,
    onTransition: LessonState => Unit = _ => (),
    onGraded: Grade => Unit = _ => (),
    onLanguageChange: Language => Unit = _ => ()
):
  private val outline = lesson.outline
  private var state: LessonState = Loop.start(outline, None)
  private var partIdx = 0
  private var hintsUsed = 0
  private var provaAttempts = 0

  private val editor = new SqlEditor
  private val langV = Var(lang0)
  private val phaseV = Var(state.phase)
  private val promptV = Var[LocalizedText](lesson.title)
  private val feedbackV = Var("")
  private val resultV = Var[Option[QueryResult]](None)
  private val reflectV = Var[Option[ReflectionReport]](None)
  private val canProva = Var(false) // whole passed → show "to PRÖVA"

  private def lang: Language = langV.now()
  private def logc(s: String): Unit = dom.console.log(s)

  private def advance(ev: LoopEvent): Unit =
    Loop.advance(outline, state, ev) match
      case Right(s) => state = s; onTransition(s)
      case Left(_)  => ()

  private def enter(phase: Phase): Unit =
    phaseV.set(phase)
    logc(s"LESSON:PHASE $phase")

  /** Headless-test hooks (drive the same handlers the buttons call; avoids fighting
    * CodeMirror's input quirks from a driver). No effect on normal use.
    */
  private def registerTestHooks(): Unit =
    val g = js.Dynamic.global.window
    g.drillSetSql = ((s: String) => editor.setValue(s)): js.Function1[String, Unit]
    g.drillRun = (() => onRun()): js.Function0[Unit]
    g.drillContinue = (() => onContinue()): js.Function0[Unit]
    g.drillToProva = (() => onToProva()): js.Function0[Unit]
    g.drillAgain = (() => onDrillAgain()): js.Function0[Unit]
    g.drillLang = (() => toggleLang()): js.Function0[Unit]

  // --- lifecycle ---
  def start(): Unit =
    registerTestHooks()
    resume match
      case Some(s) if s.status == LessonStatus.InProgress &&
            Set(Phase.OvaParts, Phase.OvaWhole, Phase.Prova).contains(s.phase) =>
        state = s
        hintsUsed = s.hintsUsedTotal
        partIdx = outline.parts.indexWhere(p => !s.partResults.get(p).exists(_.passed)) match
          case -1 => math.max(0, outline.parts.size - 1)
          case i  => i
        canProva.set(s.wholePassed)
        logc(s"LESSON:RESUME ${s.phase}")
        phaseV.set(s.phase)
        setPromptForPhase()
      case _ =>
        logc(s"LESSON:PHASE ${state.phase}") // Visa

  private def setPromptForPhase(): Unit = state.phase match
    case Phase.OvaParts => promptV.set(lesson.parts(partIdx).prompt)
    case Phase.OvaWhole => promptV.set(lesson.whole.prompt)
    case Phase.Prova    => promptV.set(lesson.exam.prompt)
    case _              => ()

  // --- actions ---
  private def onContinue(): Unit = state.phase match
    case Phase.Visa =>
      advance(LoopEvent.AdvancePhase); enter(Phase.Instruera)
    case Phase.Instruera =>
      advance(LoopEvent.AdvancePhase); enter(Phase.OvaParts)
      partIdx = 0; setPromptForPhase(); editor.clear(); editor.focus()
    case _ => ()

  private def onRun(): Unit =
    val sql = editor.value.trim
    if sql.isEmpty then feedbackV.set("…")
    else
      state.phase match
        case Phase.OvaParts => runDrill(sql)
        case Phase.OvaWhole => runWhole(sql)
        case Phase.Prova    => runExam(sql)
        case _              => ()

  private def execAndCheck(sql: String, refSql: String, checker: Checker): Future[CheckOutcome] =
    engine
      .exec(sql)
      .flatMap { qr =>
        resultV.set(Some(qr))
        engine.exec(refSql).map(ref => Check.check(sql, qr, ref, checker))
      }
      .recover { case e => resultV.set(None); CheckOutcome.Fail(Option(e.getMessage).getOrElse("error")) }

  private def runDrill(sql: String): Unit =
    val part = lesson.parts(partIdx)
    execAndCheck(sql, part.reference.sql, part.checker).foreach { outcome =>
      advance(LoopEvent.DrillAttempt(part.id, outcome, usedHint = false))
      outcome match
        case CheckOutcome.Pass =>
          feedbackV.set(Messages.drillPass(lang)); logc(s"LESSON:DRILLPASS ${part.id.value}")
          if partIdx < lesson.parts.size - 1 then
            partIdx += 1; setPromptForPhase(); resultV.set(None); feedbackV.set(""); editor.clear()
          else
            advance(LoopEvent.AdvancePhase); enter(Phase.OvaWhole)
            setPromptForPhase(); resultV.set(None); feedbackV.set(""); editor.clear()
        case CheckOutcome.Fail(reason) =>
          feedbackV.set(s"${Messages.drillFail(lang)} ($reason)"); logc(s"LESSON:DRILLFAIL ${part.id.value}")
    }

  private def runWhole(sql: String): Unit =
    execAndCheck(sql, lesson.whole.reference.sql, lesson.whole.checker).foreach { outcome =>
      advance(LoopEvent.WholeAttempt(outcome, usedHint = false))
      outcome match
        case CheckOutcome.Pass =>
          feedbackV.set(Messages.drillPass(lang)); canProva.set(true); logc("LESSON:WHOLEPASS")
        case CheckOutcome.Fail(reason) =>
          feedbackV.set(s"${Messages.drillFail(lang)} ($reason)")
    }

  private def onToProva(): Unit =
    advance(LoopEvent.RequestProva)
    if state.phase == Phase.Prova then
      enter(Phase.Prova); setPromptForPhase(); resultV.set(None); feedbackV.set(""); editor.clear()

  private def runExam(sql: String): Unit =
    provaAttempts += 1
    execAndCheck(sql, lesson.exam.reference.sql, lesson.exam.checker).foreach { outcome =>
      val grade = Grading.grade(lesson.exam.rubric, provaAttempts, hintsUsed, None, outcome)
      onGraded(grade)
      logc(s"LESSON:GRADE ${grade.points} ${grade.passed}")
      val missed = if grade.passed then Nil else outline.parts
      val report = Grading.reflect(outline.parts, grade, missed, Nil)
      reflectV.set(Some(report))
      if grade.passed then
        advance(LoopEvent.ExamSubmitted(grade)); logc("LESSON:COMPLETE")
      else
        logc(s"LESSON:REFLECT ${report.drillAgain.map(_.value).mkString(",")}")
    }

  private def onDrillAgain(): Unit =
    val parts = reflectV.now().map(_.drillAgain).getOrElse(outline.parts)
    advance(LoopEvent.DrillAgain(parts))
    partIdx = 0; canProva.set(false); reflectV.set(None); resultV.set(None); feedbackV.set("")
    enter(Phase.OvaParts); setPromptForPhase(); editor.clear()

  private def onHint(): Unit =
    if state.phase == Phase.Prova then ()
    else
      val hints = state.phase match
        case Phase.OvaParts => lesson.parts.lift(partIdx).map(_.hints).getOrElse(Nil)
        case Phase.OvaWhole => lesson.whole.hints
        case _              => Nil
      hints.headOption match
        case Some(h) => hintsUsed += 1; feedbackV.set(h.text(lang))
        case None    => feedbackV.set(Messages.noHints(lang))

  private def onReplay(): Unit =
    if state.phase != Phase.Prova then logc("LESSON:REPLAY")

  private def toggleLang(): Unit =
    val nl = if lang == Language.Sv then Language.En else Language.Sv
    langV.set(nl); onLanguageChange(nl); logc(s"LESSON:LANG $nl")

  // --- rendering ---
  private def t(lt: LocalizedText): Signal[String] = langV.signal.map(lt(_))

  private def resultTable(qr: QueryResult): Element =
    table(
      cls := "rt",
      thead(tr(qr.cols.map(c => th(c)))),
      tbody(qr.rows.map(row => tr(row.map(c => td(c.getOrElse("∅"))))))
    )

  private def isPractice(p: Phase): Boolean =
    p == Phase.OvaParts || p == Phase.OvaWhole || p == Phase.Prova

  private def demoSteps(annotated: Boolean): Element =
    div(
      lesson.demo.steps.map { step =>
        div(
          cls := "demo-step",
          pre(cls := "sql", step.input),
          resultTable(QueryResult(step.output.cols, step.output.rows)),
          if annotated then step.annotation.map(a => p(cls := "ann", child.text <-- t(a))).getOrElse(emptyNode)
          else emptyNode
        )
      }
    )

  def node: Element =
    div(
      cls := "lesson",
      styleTag(LessonView.css),
      // header
      div(
        cls := "hd",
        span(cls := "title", child.text <-- t(lesson.title)),
        span(cls := "phase", child.text <-- phaseV.signal.map(p => s"· $p")),
        button(cls := "lang", "sv / en", onClick --> (_ => toggleLang()))
      ),
      // demo (VISA / INSTRUERA)
      div(
        display <-- phaseV.signal.map(p => if p == Phase.Visa || p == Phase.Instruera then "block" else "none"),
        p(cls := "intro", child.text <-- phaseV.signal.combineWith(langV.signal).map {
          case (Phase.Visa, l)      => Messages.visaIntro(l)
          case (Phase.Instruera, l) => Messages.instrueraIntro(l)
          case (_, _)               => ""
        }),
        child <-- phaseV.signal.map {
          case Phase.Visa      => demoSteps(annotated = false)
          case Phase.Instruera => demoSteps(annotated = true)
          case _               => emptyNode
        },
        button(cls := "primary", child.text <-- t(Messages.continue), onClick --> (_ => onContinue()))
      ),
      // practice (ÖVA parts/whole, PRÖVA)
      div(
        display <-- phaseV.signal.combineWith(reflectV.signal).map { (p, r) =>
          if isPractice(p) && r.isEmpty then "block" else "none"
        },
        p(cls := "prompt", child.text <-- promptV.signal.combineWith(langV.signal).map((lt, l) => lt(l))),
        editor.node,
        div(
          cls := "btns",
          button(cls := "primary", "Run ▸", onClick --> (_ => onRun())),
          button("Hint", disabled <-- phaseV.signal.map(_ == Phase.Prova), onClick --> (_ => onHint())),
          button(
            "Replay demo",
            disabled <-- phaseV.signal.map(_ == Phase.Prova),
            onClick --> (_ => onReplay())
          ),
          button(
            cls := "primary",
            child.text <-- langV.signal.map(l => if l == Language.Sv then "Till PRÖVA ▸" else "To PRÖVA ▸"),
            display <-- phaseV.signal.combineWith(canProva.signal).map { (p, ok) =>
              if p == Phase.OvaWhole && ok then "inline-block" else "none"
            },
            onClick --> (_ => onToProva())
          )
        ),
        p(cls := "feedback", child.text <-- feedbackV.signal),
        child <-- resultV.signal.map {
          case Some(qr) => resultTable(qr)
          case None     => emptyNode
        }
      ),
      // reflection (after PRÖVA)
      div(
        display <-- reflectV.signal.map(r => if r.isDefined then "block" else "none"),
        child <-- reflectV.signal.combineWith(langV.signal).map {
          case (Some(r), l) =>
            div(
              cls := "reflect",
              h3(Messages.reflectIntro(l)),
              p(cls := "grade", Messages.points(r.grade.points, l) + (if r.grade.passed then " ✓" else " ✗")),
              p(if r.grade.passed then Messages.passedExam(l) else Messages.failedExam(l)),
              if r.drillAgain.nonEmpty then
                p(cls := "again", Messages.drillAgain(l) + " " + r.drillAgain.map(_.value).mkString(", "))
              else emptyNode,
              if r.grade.passed then emptyNode
              else
                button(
                  cls := "primary",
                  if l == Language.Sv then "Öva igen ▸" else "Drill again ▸",
                  onClick --> (_ => onDrillAgain())
                )
            )
          case _ => emptyNode
        }
      ),
      SchemaPanel.view
    )

object LessonView:
  val css: String =
    """
    .lesson { font-family: ui-sans-serif, system-ui, sans-serif; color: #cdd6f4; max-width: 760px; }
    .lesson .hd { display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #45475a; padding-bottom: 8px; }
    .lesson .hd .title { font-weight: 700; color: #f5e0dc; }
    .lesson .hd .phase { color: #a6adc8; font-size: 13px; }
    .lesson .hd .lang { margin-left: auto; }
    .lesson button { background: #313244; color: #cdd6f4; border: 1px solid #585b70; border-radius: 6px; padding: 6px 12px; cursor: pointer; font-size: 13px; margin-right: 6px; }
    .lesson button.primary { background: #89b4fa; color: #11111b; border-color: #89b4fa; font-weight: 600; }
    .lesson button:disabled { opacity: 0.4; cursor: not-allowed; }
    .lesson .intro, .lesson .prompt { font-size: 15px; margin: 12px 0 8px; }
    .lesson .prompt { color: #f9e2af; }
    .lesson .sql-editor { border: 1px solid #45475a; border-radius: 6px; overflow: hidden; margin-bottom: 8px; }
    .lesson .cm-editor { max-height: 220px; }
    .lesson .btns { margin: 8px 0; }
    .lesson .feedback { min-height: 20px; color: #a6e3a1; font-size: 14px; }
    .lesson .demo-step .sql, .lesson pre.sql { background: #181825; padding: 8px 10px; border-radius: 6px; color: #cdd6f4; overflow-x: auto; }
    .lesson .demo-step .ann { color: #f9e2af; font-size: 13px; }
    .lesson table.rt { border-collapse: collapse; margin: 6px 0; font-family: ui-monospace, monospace; font-size: 13px; }
    .lesson table.rt th, .lesson table.rt td { border: 1px solid #45475a; padding: 3px 10px; text-align: left; }
    .lesson table.rt th { background: #313244; }
    .lesson .reflect .grade { font-size: 18px; font-weight: 700; }
    .lesson .reflect .again { color: #f9e2af; }
    """

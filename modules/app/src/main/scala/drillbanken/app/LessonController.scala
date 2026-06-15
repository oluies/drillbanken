package drillbanken.app

import drillbanken.domain.*
import drillbanken.domain.loop.*
import drillbanken.domain.check.Check
import drillbanken.domain.grade.Grading
import drillbanken.engine.EngineService
import drillbanken.console.{ConsoleService, Speed}
import drillbanken.content.LessonDef
import org.scalajs.dom
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/** Drives one lesson through the five-phase loop in the console (T029–T035).
  * Pure pedagogy lives in `domain` (Loop/Check/Grading); this wires it to engine + console.
  */
final class LessonController(
    engine: EngineService,
    console: ConsoleService,
    lesson: LessonDef,
    lang: Language,
    resume: Option[LessonState] = None,
    onTransition: LessonState => Unit = _ => (),
    onGraded: Grade => Unit = _ => ()
):
  private val outline = lesson.outline
  private var state: LessonState = Loop.start(outline, None)
  private var partIdx: Int = 0
  private var hintsUsed: Int = 0
  private var provaAttempts: Int = 0

  private def line(t: LocalizedText): Unit = console.writeLine(t(lang))
  private def logc(s: String): Unit = dom.console.log(s)

  private def advance(ev: LoopEvent): Unit =
    Loop.advance(outline, state, ev) match
      case Right(s) => state = s; onTransition(state)
      case Left(_)  => () // controller guards transitions; ignore illegal

  private def freshStart(): Unit =
    logc(s"LESSON:PHASE ${state.phase}")
    line(lesson.title)
    line(Messages.visaIntro)
    console.replay(lesson.demo.steps, Speed.FullSpeed, lang)
    console.writeLine(Messages.pressEnter(lang))

  def start(): Unit =
    resume match
      case Some(s) if s.status == LessonStatus.InProgress &&
            (s.phase == Phase.OvaParts || s.phase == Phase.OvaWhole || s.phase == Phase.Prova) =>
        state = s
        hintsUsed = s.hintsUsedTotal
        partIdx = outline.parts.indexWhere(p => !s.partResults.get(p).exists(_.passed)) match
          case -1 => math.max(0, outline.parts.size - 1)
          case i  => i
        logc(s"LESSON:RESUME ${state.phase}")
        line(lesson.title)
        promptCurrent()
      case _ => freshStart()

  /** Each submitted console line (FR-009/FR-010). */
  def handle(raw: String): Unit =
    MetaCommand.parse(raw) match
      case ConsoleInput.Meta(cmd)              => handleMeta(cmd)
      case ConsoleInput.Sql(sql) if sql.isEmpty => handleEnter()
      case ConsoleInput.Sql(sql)               => handleSql(sql)

  // --- meta-commands (FR-010) ---
  private def handleMeta(cmd: MetaCommand): Unit = cmd match
    case MetaCommand.Help     => line(Messages.helpText)
    case MetaCommand.Progress => console.writeLine(Messages.progress(state.phase, lang))
    case MetaCommand.Abort =>
      advance(LoopEvent.Abort); line(Messages.aborted); logc("LESSON:ABORT")
    case MetaCommand.RepeatDemo =>
      if state.phase == Phase.Prova then
        line(Messages.demoInProva); logc("LESSON:REPLAY-REFUSED")
      else
        val speed = if state.phase == Phase.Instruera then Speed.Stepwise else Speed.FullSpeed
        console.replay(lesson.demo.steps, speed, lang)
        logc("LESSON:REPLAY")
        promptCurrent() // return to the same drill, no score change (US4)
    case MetaCommand.Hint =>
      if state.phase == Phase.Prova then line(Messages.hintInProva)
      else giveHint()

  private def giveHint(): Unit =
    val hints = state.phase match
      case Phase.OvaParts => lesson.parts.lift(partIdx).map(_.hints).getOrElse(Nil)
      case Phase.OvaWhole => lesson.whole.hints
      case _              => Nil
    hints.headOption match
      case Some(h) => hintsUsed += 1; line(h.text)
      case None    => line(Messages.noHints)

  // --- Enter (empty line) advances demo / phase transitions ---
  private def handleEnter(): Unit = state.phase match
    case Phase.Visa =>
      advance(LoopEvent.AdvancePhase) // -> Instruera
      logc(s"LESSON:PHASE ${state.phase}")
      line(Messages.instrueraIntro)
      console.replay(lesson.demo.steps, Speed.Stepwise, lang)
      console.writeLine(Messages.pressEnter(lang))
    case Phase.Instruera =>
      advance(LoopEvent.AdvancePhase) // -> OvaParts
      logc(s"LESSON:PHASE ${state.phase}")
      line(Messages.ovaPartsIntro)
      promptPart()
    case Phase.OvaWhole if state.wholePassed =>
      advance(LoopEvent.RequestProva) // -> Prova
      logc(s"LESSON:PHASE ${state.phase}")
      line(Messages.provaIntro)
      promptExam()
    case _ => promptCurrent()

  // --- SQL submissions per phase ---
  private def handleSql(sql: String): Unit = state.phase match
    case Phase.OvaParts => attemptPart(sql)
    case Phase.OvaWhole => attemptWhole(sql)
    case Phase.Prova    => attemptExam(sql)
    case _              => promptCurrent() // SQL ignored during demo phases

  private def attemptPart(sql: String): Unit =
    val part = lesson.parts(partIdx)
    runCheck(sql, part.reference.sql, part.checker).foreach { outcome =>
      advance(LoopEvent.DrillAttempt(part.id, outcome, usedHint = false))
      outcome match
        case CheckOutcome.Pass =>
          line(Messages.drillPass); logc(s"LESSON:DRILLPASS ${part.id.value}")
          if partIdx < lesson.parts.size - 1 then
            partIdx += 1; promptPart()
          else
            advance(LoopEvent.AdvancePhase) // OvaParts -> OvaWhole
            logc(s"LESSON:PHASE ${state.phase}")
            line(Messages.ovaWholeIntro); promptWhole()
        case CheckOutcome.Fail(_) =>
          line(Messages.drillFail); logc(s"LESSON:DRILLFAIL ${part.id.value}"); promptPart()
    }

  private def attemptWhole(sql: String): Unit =
    runCheck(sql, lesson.whole.reference.sql, lesson.whole.checker).foreach { outcome =>
      advance(LoopEvent.WholeAttempt(outcome, usedHint = false))
      outcome match
        case CheckOutcome.Pass =>
          line(Messages.drillPass); logc("LESSON:WHOLEPASS")
          console.writeLine(Messages.pressEnter(lang))
        case CheckOutcome.Fail(_) =>
          line(Messages.drillFail); promptWhole()
    }

  private def attemptExam(sql: String): Unit =
    provaAttempts += 1
    runCheck(sql, lesson.exam.reference.sql, lesson.exam.checker).foreach { outcome =>
      val grade = Grading.grade(lesson.exam.rubric, provaAttempts, hintsUsed, None, outcome)
      onGraded(grade)
      console.writeLine(Messages.points(grade.points, lang))
      logc(s"LESSON:GRADE ${grade.points} ${grade.passed}")
      val missed = if grade.passed then Nil else outline.parts
      val report = Grading.reflect(outline.parts, grade, missed, Nil)
      line(Messages.reflectIntro)
      if grade.passed then
        advance(LoopEvent.ExamSubmitted(grade))
        line(Messages.passedExam); logc("LESSON:COMPLETE")
      else
        line(Messages.failedExam)
        console.writeLine(Messages.drillAgain(lang) + " " + report.drillAgain.map(_.value).mkString(", "))
        logc(s"LESSON:REFLECT ${report.drillAgain.map(_.value).mkString(",")}")
        // route back to the named drills (FR-007)
        advance(LoopEvent.DrillAgain(report.drillAgain))
        partIdx = 0
        logc(s"LESSON:PHASE ${state.phase}")
        promptPart()
    }

  /** Run learner + reference SQL and compare (FR-011/012). Engine errors → Fail. */
  private def runCheck(sql: String, refSql: String, checker: Checker): Future[CheckOutcome] =
    val checked =
      for
        learner <- engine.exec(sql)
        reference <- engine.exec(refSql)
      yield Check.check(sql, learner, reference, checker)
    checked.recover { case e => CheckOutcome.Fail(Option(e.getMessage).getOrElse("error")) }

  // --- prompts ---
  private def promptCurrent(): Unit = state.phase match
    case Phase.OvaParts => promptPart()
    case Phase.OvaWhole => promptWhole()
    case Phase.Prova    => promptExam()
    case _              => ()
  private def promptPart(): Unit =
    lesson.parts.lift(partIdx).foreach(p => line(p.prompt)); console.prompt("öva> ")
  private def promptWhole(): Unit = { line(lesson.whole.prompt); console.prompt("helhet> ") }
  private def promptExam(): Unit = { line(lesson.exam.prompt); console.prompt("pröva> ") }

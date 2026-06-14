package drillbanken.domain.loop

import drillbanken.domain.*

/** Per-part progress while drilling (ÖVA parts). Attempts/hints are tracked but do NOT
  * affect the grade (FR-014); only PRÖVA does.
  */
final case class PartResult(passed: Boolean, attempts: Int, hintsUsed: Int)

object PartResult:
  val zero: PartResult = PartResult(passed = false, attempts = 0, hintsUsed = 0)

enum LessonStatus:
  case InProgress, Aborted, Completed

/** The minimal lesson shape the engine needs — the ordered required part-drills. The full
  * content (transcript, prompts, rubric) lives in the `content` module's `LessonDef`.
  */
final case class LessonOutline(lessonId: LessonId, parts: List[PartId])

/** Resumable snapshot of a lesson in progress (FR-022). */
final case class LessonState(
    lessonId: LessonId,
    phase: Phase,
    partIndex: Int,
    partResults: Map[PartId, PartResult],
    wholePassed: Boolean,
    hintsUsedTotal: Int,
    status: LessonStatus
)

enum LoopEvent:
  case AdvancePhase
  case DrillAttempt(partId: PartId, outcome: CheckOutcome, usedHint: Boolean)
  case WholeAttempt(outcome: CheckOutcome, usedHint: Boolean)
  case RequestProva
  case ExamSubmitted(grade: Grade)
  case RepeatDemoRequested
  case DrillAgain(parts: List[PartId])
  case Abort

enum LoopError:
  case GateNotMet(reason: String)
  case IllegalTransition(from: Phase, event: String)
  case DemoNotAllowedInProva

/** The typed instruction-loop state machine (Principle I, FR-001, FR-005).
  * All transitions are total; illegal ones return a typed [[LoopError]].
  */
object Loop:

  def start(outline: LessonOutline, resume: Option[LessonState]): LessonState =
    resume.getOrElse(
      LessonState(
        lessonId = outline.lessonId,
        phase = Phase.Visa,
        partIndex = 0,
        partResults = Map.empty,
        wholePassed = false,
        hintsUsedTotal = 0,
        status = LessonStatus.InProgress
      )
    )

  /** All required parts have a passing result. */
  def partsGateMet(outline: LessonOutline, s: LessonState): Boolean =
    outline.parts.forall(p => s.partResults.get(p).exists(_.passed))

  private def ovaGatesMet(outline: LessonOutline, s: LessonState): Boolean =
    partsGateMet(outline, s) && s.wholePassed

  def advance(
      outline: LessonOutline,
      s: LessonState,
      ev: LoopEvent
  ): Either[LoopError, LessonState] =
    import Phase.*
    import LoopEvent.*

    // Abort is always legal and records no grade (Edge case).
    ev match
      case Abort => Right(s.copy(status = LessonStatus.Aborted))

      // Repeat-demo: legal in any ÖVA-or-demo phase, refused in PRÖVA (FR-010, US4).
      case RepeatDemoRequested =>
        if s.phase == Prova then Left(LoopError.DemoNotAllowedInProva)
        else Right(s) // no phase change, no score change

      case AdvancePhase =>
        s.phase match
          case Visa      => Right(s.copy(phase = Instruera))
          case Instruera => Right(s.copy(phase = OvaParts))
          case OvaParts =>
            if partsGateMet(outline, s) then Right(s.copy(phase = OvaWhole))
            else Left(LoopError.GateNotMet("not all part-drills passed"))
          case OvaWhole => Left(LoopError.IllegalTransition(OvaWhole, "AdvancePhase (use RequestProva)"))
          case Prova    => Left(LoopError.IllegalTransition(Prova, "AdvancePhase"))

      case DrillAttempt(partId, outcome, usedHint) =>
        if s.phase != OvaParts then Left(LoopError.IllegalTransition(s.phase, "DrillAttempt"))
        else
          val prev = s.partResults.getOrElse(partId, PartResult.zero)
          val updated = prev.copy(
            passed = prev.passed || CheckOutcome.isPass(outcome),
            attempts = prev.attempts + 1,
            hintsUsed = prev.hintsUsed + (if usedHint then 1 else 0)
          )
          Right(s.copy(partResults = s.partResults.updated(partId, updated)))

      case WholeAttempt(outcome, usedHint) =>
        if s.phase != OvaWhole then Left(LoopError.IllegalTransition(s.phase, "WholeAttempt"))
        else
          Right(
            s.copy(
              wholePassed = s.wholePassed || CheckOutcome.isPass(outcome),
              hintsUsedTotal = s.hintsUsedTotal + (if usedHint then 1 else 0)
            )
          )

      case RequestProva =>
        if s.phase != OvaWhole then Left(LoopError.IllegalTransition(s.phase, "RequestProva"))
        else if ovaGatesMet(outline, s) then Right(s.copy(phase = Prova))
        else Left(LoopError.GateNotMet("ÖVA gates not met"))

      case ExamSubmitted(grade) =>
        if s.phase != Prova then Left(LoopError.IllegalTransition(s.phase, "ExamSubmitted"))
        else if grade.passed then Right(s.copy(status = LessonStatus.Completed))
        else Right(s) // failed exam: stay in PRÖVA so the learner can DrillAgain

      // Route back to specific part-drills after a failed PRÖVA (FR-007). Named parts are
      // marked not-passed so the gate requires re-passing; attempt/hint history is kept.
      case DrillAgain(parts) =>
        if s.phase != Prova then Left(LoopError.IllegalTransition(s.phase, "DrillAgain"))
        else
          val named = parts.filter(outline.parts.contains)
          val cleared = named.foldLeft(s.partResults) { (m, p) =>
            val prev = m.getOrElse(p, PartResult.zero)
            m.updated(p, prev.copy(passed = false))
          }
          val firstIdx = outline.parts.indexWhere(named.contains) match
            case -1 => 0
            case i  => i
          Right(s.copy(phase = OvaParts, partIndex = firstIdx, partResults = cleared))

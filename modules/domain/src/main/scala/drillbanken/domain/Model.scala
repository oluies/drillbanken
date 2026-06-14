package drillbanken.domain

/** Stable identifiers (plain wrappers — friendly in Maps and tests). */
final case class LessonId(value: String)
final case class PartId(value: String)

/** The five-phase Försvarsmakten instruction loop. Order is fixed (FR-001, Principle I):
  * VISA -> INSTRUERA -> ÖVA(parts) -> ÖVA(whole) -> PRÖVA.
  */
enum Phase:
  case Visa, Instruera, OvaParts, OvaWhole, Prova

/** Result of running a checker (FR-011, FR-012). */
enum CheckOutcome:
  case Pass
  case Fail(reason: String)

object CheckOutcome:
  def isPass(o: CheckOutcome): Boolean = o match
    case Pass    => true
    case Fail(_) => false

/** Optional SQL-shape constraint: the learner SQL must contain all listed fragments
  * (case-insensitive). Applied only where a lesson declares it (FR-012).
  */
final case class SqlShapeRule(mustContain: List[String]):
  def satisfiedBy(sql: String): Boolean =
    val low = sql.toLowerCase
    mustContain.forall(frag => low.contains(frag.toLowerCase))

/** A checker compares a learner result (and optionally SQL shape) to a reference (FR-011). */
final case class Checker(orderSensitive: Boolean, shapeRule: Option[SqlShapeRule] = None)

/** Per-lesson PRÖVA rubric. Points only — no IG/G/VG (FR-013, D7). */
final case class Rubric(
    maxPoints: Int,
    correctnessWeight: Int,
    attemptPenalty: Int,
    hintPenalty: Int,
    timePenaltyPerSec: Int = 0,
    timeBoxSec: Option[Int] = None,
    passThreshold: Int
)

final case class ScoreLine(label: String, points: Int)

/** A numeric grade (FR-013). `passed` is `points >= rubric.passThreshold`. */
final case class Grade(points: Int, passed: Boolean, breakdown: List[ScoreLine])

/** Post-PRÖVA reflection: what was graded + the generated "drill again" list (FR-006, FR-007). */
final case class ReflectionReport(
    grade: Grade,
    whatGraded: List[String],
    drillAgain: List[PartId]
)

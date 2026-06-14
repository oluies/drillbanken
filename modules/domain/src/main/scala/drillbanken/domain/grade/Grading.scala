package drillbanken.domain.grade

import drillbanken.domain.*

/** PRÖVA grading + reflection (FR-006, FR-007, FR-013, FR-014, contracts/domain-api.md).
  *
  * Points model: correctness earns `correctnessWeight`; the remaining
  * `maxPoints - correctnessWeight` are "process" points eroded by attempt/hint/time
  * penalties. ÖVA repetition is NOT an input here (FR-014) — only PRÖVA attempts, hint
  * usage, and elapsed time matter.
  */
object Grading:

  def grade(
      rubric: Rubric,
      attempts: Int,
      hintsUsed: Int,
      elapsedSec: Option[Int],
      correctness: CheckOutcome
  ): Grade =
    val passedCorrect = CheckOutcome.isPass(correctness)
    val correctnessPts = if passedCorrect then rubric.correctnessWeight else 0
    val processMax = math.max(0, rubric.maxPoints - rubric.correctnessWeight)

    val attemptPen = rubric.attemptPenalty * math.max(0, attempts - 1)
    val hintPen = rubric.hintPenalty * math.max(0, hintsUsed)
    val timePen = (rubric.timeBoxSec, elapsedSec) match
      case (Some(box), Some(e)) if e > box => rubric.timePenaltyPerSec * (e - box)
      case _                               => 0

    val process = math.max(0, processMax - attemptPen - hintPen - timePen)
    val points = math.min(rubric.maxPoints, correctnessPts + process)
    val passed = points >= rubric.passThreshold

    val breakdown = List(
      ScoreLine("correctness", correctnessPts),
      ScoreLine("process", process),
      ScoreLine("attemptPenalty", -attemptPen),
      ScoreLine("hintPenalty", -hintPen),
      ScoreLine("timePenalty", -timePen)
    )
    Grade(points, passed, breakdown)

  /** Build the reflection: `drillAgain` is the missed parts intersected with the lesson's
    * parts (FR-006, FR-007) — it can never name a part outside the lesson.
    */
  def reflect(
      allParts: List[PartId],
      grade: Grade,
      missed: List[PartId],
      whatGraded: List[String]
  ): ReflectionReport =
    val drill = missed.filter(allParts.contains).distinct
    ReflectionReport(grade, whatGraded, drill)

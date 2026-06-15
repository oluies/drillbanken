package drillbanken.content

import drillbanken.domain.*
import drillbanken.domain.loop.LessonOutline

/** Lesson content DSL — typed, in-bundle, declarative (Principle V, FR-018,
  * contracts/lesson-dsl.md). Adding a lesson = adding a [[LessonDef]] value to
  * [[Curriculum]] and rebuilding; a malformed lesson is a compile error.
  */

/** Reference SQL run live for checking (and to drive the demo's expected output). */
final case class ReferenceSolution(sql: String)

/** A hint with its PRÖVA-irrelevant ÖVA cost / PRÖVA cost (FR-004, FR-014). */
final case class Hint(text: LocalizedText, cost: Int)

// QueryResultView / TranscriptStep / Transcript now live in `drillbanken.domain`
// (imported via `drillbanken.domain.*`) so the console module can replay them too.

final case class PartDrill(
    id: PartId,
    prompt: LocalizedText,
    checker: Checker,
    hints: List[Hint],
    reference: ReferenceSolution
)

final case class WholeTask(
    prompt: LocalizedText,
    checker: Checker,
    hints: List[Hint],
    reference: ReferenceSolution
)

final case class Exam(
    prompt: LocalizedText,
    checker: Checker,
    reference: ReferenceSolution,
    rubric: Rubric
)

/** Which seed dataset a lesson runs against. v1 ships only the trading book. */
enum SeedRef:
  case TradingBook

final case class LessonDef(
    id: LessonId,
    sequence: Int,
    title: LocalizedText,
    endRequirement: LocalizedText,
    seed: SeedRef,
    demo: Transcript,
    parts: List[PartDrill],
    whole: WholeTask,
    exam: Exam,
    timeBox: Option[Int] = None
):
  /** The minimal shape the loop engine consumes (domain `LessonOutline`). */
  def outline: LessonOutline = LessonOutline(id, parts.map(_.id))

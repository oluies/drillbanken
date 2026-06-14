package drillbanken.domain.progress

import drillbanken.domain.*
import drillbanken.domain.loop.LessonState

/** Per-lesson persisted progress (FR-017, FR-021, FR-022). */
final case class LessonProgress(
    bestGrade: Option[Grade],
    resume: Option[LessonState],
    completed: Boolean
)

object LessonProgress:
  val empty: LessonProgress = LessonProgress(None, None, completed = false)

/** Whole-device progress state (FR-015, FR-016, FR-021). No backend; serialized by the
  * app layer's PersistenceService.
  */
final case class ProgressState(
    schemaVersion: Int,
    language: Language,
    unlocked: Set[LessonId],
    lessons: Map[LessonId, LessonProgress],
    streak: Int,
    insignia: Set[String]
)

object ProgressState:
  val SchemaVersion: Int = 1

  /** First-run state: only the first lesson unlocked, no progress (Edge case, FR-015). */
  def initial(firstLesson: LessonId, language: Language = Language.Sv): ProgressState =
    ProgressState(
      schemaVersion = SchemaVersion,
      language = language,
      unlocked = Set(firstLesson),
      lessons = Map.empty,
      streak = 0,
      insignia = Set.empty
    )

/** Progression rules (FR-015, FR-016, FR-017, contracts/domain-api.md). */
object Progression:

  def isUnlocked(p: ProgressState, lesson: LessonId): Boolean =
    p.unlocked.contains(lesson)

  /** Apply a PRÖVA grade. A passing grade unlocks the next lesson, increments the streak,
    * and may award insignia; a failing grade resets the streak. `bestGrade` is monotonic
    * in points (FR-017). Streak/insignia change only on a pass (FR-016).
    */
  def applyGrade(
      p: ProgressState,
      lesson: LessonId,
      nextLesson: Option[LessonId],
      grade: Grade,
      insigniaFor: Grade => Set[String] = _ => Set.empty
  ): ProgressState =
    val prev = p.lessons.getOrElse(lesson, LessonProgress.empty)
    val best = prev.bestGrade match
      case Some(b) if b.points >= grade.points => Some(b)
      case _                                   => Some(grade)
    val nowCompleted = prev.completed || grade.passed
    val updatedLesson = prev.copy(bestGrade = best, completed = nowCompleted, resume = None)

    if grade.passed then
      p.copy(
        lessons = p.lessons.updated(lesson, updatedLesson),
        unlocked = p.unlocked ++ nextLesson.toSet,
        streak = p.streak + 1,
        insignia = p.insignia ++ insigniaFor(grade)
      )
    else
      p.copy(
        lessons = p.lessons.updated(lesson, updatedLesson),
        streak = 0
      )

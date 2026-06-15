package drillbanken.content

/** The ordered curriculum (FR-015, FR-019). Lessons are added here (TODO(T027) Lesson01,
  * TODO(T050) Lesson02). Startup validation enforces unique sequences and non-empty parts.
  */
object Curriculum:

  /** All lessons, sorted by unlock order. */
  val all: List[LessonDef] = List(
    lessons.Lesson01.lesson
  ).sortBy(_.sequence)

  /** Validate the curriculum at startup. A duplicate `sequence`, or a lesson with no
    * part-drills, is a hard error (FR-015, FR-019).
    */
  def validate(lessons: List[LessonDef] = all): Either[String, Unit] =
    val seqs = lessons.map(_.sequence)
    if seqs.distinct.size != seqs.size then Left("duplicate lesson sequence")
    else if lessons.exists(_.parts.isEmpty) then Left("lesson with no part-drills")
    else Right(())

  /** The first lesson by sequence, if any. */
  def first: Option[LessonDef] = all.headOption

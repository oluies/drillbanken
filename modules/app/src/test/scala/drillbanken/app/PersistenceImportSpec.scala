package drillbanken.app

import drillbanken.domain.*
import drillbanken.domain.progress.{ProgressState, LessonProgress}

/** T054 [US6] — export/import round-trip + non-mutating error handling (FR-023). Pure
  * (ProgressCodec), runs on node — no localStorage.
  */
class PersistenceImportSpec extends munit.FunSuite:

  private val sample: ProgressState =
    ProgressState
      .initial(LessonId("joins-and-nulls"), Language.En)
      .copy(
        unlocked = Set(LessonId("joins-and-nulls"), LessonId("nulls-and-aggregates")),
        lessons = Map(
          LessonId("joins-and-nulls") ->
            LessonProgress(Some(Grade(100, passed = true, List(ScoreLine("correctness", 70)))), None, completed = true)
        ),
        streak = 3,
        insignia = Set("ace")
      )

  test("export → import round-trips identically"):
    val json = ProgressCodec.encode(sample)
    assertEquals(ProgressCodec.decode(json), Right(sample))

  test("malformed JSON → BadJson"):
    assertEquals(ProgressCodec.decode("{ not json"), Left(ImportError.BadJson))

  test("wrong schemaVersion → UnsupportedSchema (state not adopted)"):
    val json = ProgressCodec.encode(sample.copy(schemaVersion = 999))
    assertEquals(
      ProgressCodec.decode(json),
      Left(ImportError.UnsupportedSchema(999, ProgressState.SchemaVersion))
    )

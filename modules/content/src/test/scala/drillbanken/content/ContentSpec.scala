package drillbanken.content

import drillbanken.domain.*

/** Verifies the pure content pieces: seed shape and curriculum validation (FR-019, FR-020). */
class ContentSpec extends munit.FunSuite:

  test("trading-book seed is a non-empty ordered DDL/DML script"):
    assert(SeedData.tradingBook.nonEmpty)
    assert(SeedData.tradingBook.head.toLowerCase.contains("create"))
    // deliberate NULL price + orphan rows are present (FR-020)
    assert(SeedData.tradingBook.exists(_.contains("NULL")))
    assert(SeedData.tradingBook.exists(_.contains("'Eve'"))) // trader with no trades
    assert(SeedData.tradingBook.exists(_.contains("'GOLD'"))) // instrument never traded

  test("validate accepts an empty curriculum and rejects duplicate sequences"):
    assertEquals(Curriculum.validate(Nil), Right(()))
    val rubric = Rubric(100, 70, 5, 5, 0, None, 60)
    val checker = Checker(orderSensitive = false)
    def lesson(id: String, seq: Int) = LessonDef(
      id = LessonId(id),
      sequence = seq,
      title = LocalizedText(id, id),
      endRequirement = LocalizedText("", ""),
      seed = SeedRef.TradingBook,
      demo = Transcript(Nil),
      parts = List(PartDrill(PartId("p1"), LocalizedText("", ""), checker, Nil, ReferenceSolution("select 1"))),
      whole = WholeTask(LocalizedText("", ""), checker, Nil, ReferenceSolution("select 1")),
      exam = Exam(LocalizedText("", ""), checker, ReferenceSolution("select 1"), rubric)
    )
    assertEquals(Curriculum.validate(List(lesson("a", 1), lesson("b", 2))), Right(()))
    assertEquals(Curriculum.validate(List(lesson("a", 1), lesson("b", 1))), Left("duplicate lesson sequence"))

  test("curriculum has both lessons in sequence and validates (US5)"):
    assertEquals(Curriculum.all.map(_.sequence), List(1, 2))
    assertEquals(Curriculum.all.map(_.id.value), List("joins-and-nulls", "nulls-and-aggregates"))
    assertEquals(Curriculum.validate(), Right(()))
    assertEquals(Curriculum.first.map(_.sequence), Some(1))

  test("a lesson with no parts is rejected"):
    val rubric = Rubric(100, 70, 5, 5, 0, None, 60)
    val checker = Checker(orderSensitive = false)
    val noParts = LessonDef(
      LessonId("x"), 1, LocalizedText("x", "x"), LocalizedText("", ""), SeedRef.TradingBook,
      Transcript(Nil), Nil,
      WholeTask(LocalizedText("", ""), checker, Nil, ReferenceSolution("select 1")),
      Exam(LocalizedText("", ""), checker, ReferenceSolution("select 1"), rubric)
    )
    assertEquals(Curriculum.validate(List(noParts)), Left("lesson with no part-drills"))

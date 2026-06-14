package drillbanken.domain

import drillbanken.domain.loop.*

/** T037 [US2] — fail PRÖVA → reroute to named drills; completion only on pass (FR-007). */
class RerouteSpec extends munit.FunSuite:

  private val o = LessonOutline(LessonId("L"), List(PartId("p1"), PartId("p2"), PartId("p3")))

  private def inProvaWithPartsPassed: LessonState =
    Loop
      .start(o, None)
      .copy(
        phase = Phase.Prova,
        wholePassed = true,
        partResults = o.parts.map(_ -> PartResult(passed = true, attempts = 1, hintsUsed = 0)).toMap
      )

  test("a failing exam keeps the lesson in progress (no completion)"):
    val s = inProvaWithPartsPassed
    val after = Loop.advance(o, s, LoopEvent.ExamSubmitted(Grade(10, passed = false, Nil))).toOption.get
    assertEquals(after.status, LessonStatus.InProgress)

  test("a passing exam completes the lesson"):
    val s = inProvaWithPartsPassed
    val after = Loop.advance(o, s, LoopEvent.ExamSubmitted(Grade(90, passed = true, Nil))).toOption.get
    assertEquals(after.status, LessonStatus.Completed)

  test("DrillAgain returns to OvaParts and clears only the named parts"):
    val s = inProvaWithPartsPassed
    val after = Loop.advance(o, s, LoopEvent.DrillAgain(List(PartId("p2")))).toOption.get
    assertEquals(after.phase, Phase.OvaParts)
    assertEquals(after.partIndex, 1) // p2 is index 1
    assert(!after.partResults(PartId("p2")).passed) // named part re-opened
    assert(after.partResults(PartId("p1")).passed) // untouched parts keep their pass
    assert(after.partResults(PartId("p3")).passed)

  test("DrillAgain outside PRÖVA is illegal"):
    val atParts = Loop.start(o, None).copy(phase = Phase.OvaParts)
    assert(Loop.advance(o, atParts, LoopEvent.DrillAgain(List(PartId("p1")))).isLeft)

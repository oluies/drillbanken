package drillbanken.domain

import drillbanken.domain.grade.Grading
import org.scalacheck.{Gen, Prop}
import org.scalacheck.Prop.forAll

/** T023 [US1] — points grading + reflection (FR-013, FR-014, FR-006, SC-005). */
class GradingSpec extends munit.ScalaCheckSuite:

  private val rubricGen: Gen[Rubric] =
    for
      maxPoints <- Gen.choose(10, 100)
      correctness <- Gen.choose(0, maxPoints)
      attemptPen <- Gen.choose(0, 20)
      hintPen <- Gen.choose(0, 20)
      threshold <- Gen.choose(0, maxPoints)
    yield Rubric(maxPoints, correctness, attemptPen, hintPen, 0, None, threshold)

  private val outcomeGen: Gen[CheckOutcome] =
    Gen.oneOf(CheckOutcome.Pass, CheckOutcome.Fail("x"))

  property("points are numeric and bounded to [0, maxPoints]"):
    forAll(rubricGen, Gen.choose(1, 10), Gen.choose(0, 10), outcomeGen) {
      (rubric, attempts, hints, outcome) =>
        val g = Grading.grade(rubric, attempts, hints, None, outcome)
        g.points >= 0 && g.points <= rubric.maxPoints
    }

  property("passed iff points >= passThreshold"):
    forAll(rubricGen, Gen.choose(1, 10), Gen.choose(0, 10), outcomeGen) {
      (rubric, attempts, hints, outcome) =>
        val g = Grading.grade(rubric, attempts, hints, None, outcome)
        g.passed == (g.points >= rubric.passThreshold)
    }

  property("more attempts never increases points"):
    forAll(rubricGen, Gen.choose(1, 9), Gen.choose(0, 10), outcomeGen) {
      (rubric, attempts, hints, outcome) =>
        val lo = Grading.grade(rubric, attempts, hints, None, outcome).points
        val hi = Grading.grade(rubric, attempts + 1, hints, None, outcome).points
        hi <= lo
    }

  property("more hints never increases points"):
    forAll(rubricGen, Gen.choose(1, 10), Gen.choose(0, 9), outcomeGen) {
      (rubric, attempts, hints, outcome) =>
        val lo = Grading.grade(rubric, attempts, hints, None, outcome).points
        val hi = Grading.grade(rubric, attempts, hints + 1, None, outcome).points
        hi <= lo
    }

  property("reflect.drillAgain is always a subset of the lesson's parts"):
    val partsGen = Gen.choose(1, 6).map(n => (1 to n).map(i => PartId(s"p$i")).toList)
    val missedGen = Gen.listOf(Gen.choose(1, 10).map(i => PartId(s"p$i")))
    forAll(partsGen, missedGen) { (parts, missed) =>
      val g = Grade(0, false, Nil)
      val report = Grading.reflect(parts, g, missed, Nil)
      report.drillAgain.forall(parts.contains)
    }

  test("a passing exam with no penalties scores full marks"):
    val rubric = Rubric(100, 70, 5, 5, 0, None, 60)
    val g = Grading.grade(rubric, attempts = 1, hintsUsed = 0, elapsedSec = None, CheckOutcome.Pass)
    assertEquals(g.points, 100)
    assert(g.passed)

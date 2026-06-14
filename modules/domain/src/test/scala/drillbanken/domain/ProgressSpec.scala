package drillbanken.domain

import drillbanken.domain.progress.*
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/** T041 [US3] — progression rules: unlock-on-pass, streak, best-grade monotonicity
  * (FR-015, FR-016, FR-017).
  */
class ProgressSpec extends munit.ScalaCheckSuite:

  private val l1 = LessonId("l1")
  private val l2 = LessonId("l2")

  test("first-run state unlocks only the first lesson"):
    val p = ProgressState.initial(l1)
    assert(Progression.isUnlocked(p, l1))
    assert(!Progression.isUnlocked(p, l2))
    assertEquals(p.streak, 0)

  test("a passing grade unlocks the next lesson and increments the streak"):
    val p = ProgressState.initial(l1)
    val after = Progression.applyGrade(p, l1, Some(l2), Grade(80, passed = true, Nil))
    assert(Progression.isUnlocked(after, l2))
    assertEquals(after.streak, 1)
    assertEquals(after.lessons(l1).completed, true)

  test("a failing grade does not unlock and resets the streak"):
    val p = ProgressState.initial(l1).copy(streak = 3)
    val after = Progression.applyGrade(p, l1, Some(l2), Grade(20, passed = false, Nil))
    assert(!Progression.isUnlocked(after, l2))
    assertEquals(after.streak, 0)

  property("bestGrade is monotonic in points across attempts"):
    forAll(Gen.choose(0, 100), Gen.choose(0, 100)) { (a, b) =>
      val p0 = ProgressState.initial(l1)
      val p1 = Progression.applyGrade(p0, l1, Some(l2), Grade(a, passed = a >= 60, Nil))
      val p2 = Progression.applyGrade(p1, l1, Some(l2), Grade(b, passed = b >= 60, Nil))
      p2.lessons(l1).bestGrade.get.points == math.max(a, b)
    }

  test("insignia are awarded only on a pass, via the provided rule"):
    val p = ProgressState.initial(l1)
    val rule: Grade => Set[String] = g => if g.points >= 90 then Set("ace") else Set.empty
    val pass = Progression.applyGrade(p, l1, Some(l2), Grade(95, passed = true, Nil), rule)
    val fail = Progression.applyGrade(p, l1, Some(l2), Grade(95, passed = false, Nil), rule)
    assert(pass.insignia.contains("ace"))
    assert(fail.insignia.isEmpty)

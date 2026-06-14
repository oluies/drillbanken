package drillbanken.domain

import drillbanken.domain.loop.*
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/** T046 [US4] — repeat-demo legality and score-neutrality (FR-010, US4). */
class RepeatDemoSpec extends munit.ScalaCheckSuite:

  private val o = LessonOutline(LessonId("L"), List(PartId("p1"), PartId("p2")))

  test("repeat-demo is refused during PRÖVA"):
    val s = Loop.start(o, None).copy(phase = Phase.Prova)
    assertEquals(Loop.advance(o, s, LoopEvent.RepeatDemoRequested), Left(LoopError.DemoNotAllowedInProva))

  property("repeat-demo in any non-PRÖVA phase never changes state"):
    val nonProva = Gen.oneOf(Phase.Visa, Phase.Instruera, Phase.OvaParts, Phase.OvaWhole)
    forAll(nonProva, Gen.choose(0, 5)) { (ph, hints) =>
      val s = Loop
        .start(o, None)
        .copy(phase = ph, hintsUsedTotal = hints, partResults = Map(PartId("p1") -> PartResult(true, 3, 1)))
      Loop.advance(o, s, LoopEvent.RepeatDemoRequested) == Right(s)
    }

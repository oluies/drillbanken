package drillbanken.domain

import drillbanken.domain.loop.*
import org.scalacheck.{Gen, Prop}
import org.scalacheck.Prop.forAll

/** T021 [US1] — loop state machine: fixed order + PRÖVA gate (FR-001, FR-005, SC-002). */
class LoopSpec extends munit.ScalaCheckSuite:

  private val outlineGen: Gen[LessonOutline] =
    Gen.choose(1, 5).map(n => LessonOutline(LessonId("L"), (1 to n).map(i => PartId(s"p$i")).toList))

  // Drive the loop to the start of ÖVA(parts).
  private def toOvaParts(o: LessonOutline): LessonState =
    val s0 = Loop.start(o, None)
    val s1 = Loop.advance(o, s0, LoopEvent.AdvancePhase).toOption.get // Visa -> Instruera
    Loop.advance(o, s1, LoopEvent.AdvancePhase).toOption.get // Instruera -> OvaParts

  private def passAllParts(o: LessonOutline, s: LessonState): LessonState =
    o.parts.foldLeft(s) { (st, p) =>
      Loop.advance(o, st, LoopEvent.DrillAttempt(p, CheckOutcome.Pass, usedHint = false)).toOption.get
    }

  property("the only path into PRÖVA requires all parts AND the whole passed"):
    forAll(outlineGen) { o =>
      val atParts = toOvaParts(o)
      // Advancing to OvaWhole before passing parts is gated.
      val gatedEarly = Loop.advance(o, atParts, LoopEvent.AdvancePhase)
      val earlyBlocked = gatedEarly.isLeft

      val allPassed = passAllParts(o, atParts)
      val atWhole = Loop.advance(o, allPassed, LoopEvent.AdvancePhase).toOption.get
      // RequestProva before whole passed is gated.
      val provaBlocked = Loop.advance(o, atWhole, LoopEvent.RequestProva).isLeft

      val wholeDone = Loop.advance(o, atWhole, LoopEvent.WholeAttempt(CheckOutcome.Pass, false)).toOption.get
      val inProva = Loop.advance(o, wholeDone, LoopEvent.RequestProva).toOption.get

      earlyBlocked && provaBlocked && inProva.phase == Phase.Prova
    }

  property("phase order never skips: Visa -> Instruera -> OvaParts"):
    forAll(outlineGen) { o =>
      val s0 = Loop.start(o, None)
      s0.phase == Phase.Visa &&
      Loop.advance(o, s0, LoopEvent.AdvancePhase).toOption.get.phase == Phase.Instruera &&
      toOvaParts(o).phase == Phase.OvaParts
    }

  property("Abort from any phase yields Aborted and no grade path"):
    val phaseGen = Gen.oneOf(Phase.values.toIndexedSeq)
    forAll(outlineGen, phaseGen) { (o, ph) =>
      val s = Loop.start(o, None).copy(phase = ph)
      Loop.advance(o, s, LoopEvent.Abort).toOption.get.status == LessonStatus.Aborted
    }

  test("DrillAttempt outside OvaParts is an illegal transition"):
    val o = LessonOutline(LessonId("L"), List(PartId("p1")))
    val s = Loop.start(o, None) // Visa
    assert(Loop.advance(o, s, LoopEvent.DrillAttempt(PartId("p1"), CheckOutcome.Pass, false)).isLeft)

  test("repeated drilling accumulates attempts but a single pass sticks"):
    val o = LessonOutline(LessonId("L"), List(PartId("p1")))
    val atParts = toOvaParts(o)
    val s1 = Loop.advance(o, atParts, LoopEvent.DrillAttempt(PartId("p1"), CheckOutcome.Fail("x"), false)).toOption.get
    val s2 = Loop.advance(o, s1, LoopEvent.DrillAttempt(PartId("p1"), CheckOutcome.Pass, false)).toOption.get
    val s3 = Loop.advance(o, s2, LoopEvent.DrillAttempt(PartId("p1"), CheckOutcome.Fail("x"), false)).toOption.get
    val pr = s3.partResults(PartId("p1"))
    assertEquals(pr.attempts, 3)
    assert(pr.passed) // a later fail does not un-pass

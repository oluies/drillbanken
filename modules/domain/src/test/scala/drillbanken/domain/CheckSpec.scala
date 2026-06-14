package drillbanken.domain

import drillbanken.domain.check.Check
import org.scalacheck.{Gen, Prop}
import org.scalacheck.Prop.forAll

/** T022 [US1] — checker order-sensitivity and SQL-shape behavior (FR-011, FR-012, SC-003). */
class CheckSpec extends munit.ScalaCheckSuite:

  private val cellGen: Gen[Option[String]] =
    Gen.oneOf(Gen.const(None), Gen.alphaNumStr.map(Some(_)))

  // Result with >=2 distinct rows of 2 columns.
  private val distinctResultGen: Gen[QueryResult] =
    for
      n <- Gen.choose(2, 6)
      rows <- Gen.listOfN(n, Gen.listOfN(2, cellGen))
    yield QueryResult(List("a", "b"), rows.distinct).ensuring(_ => true)

  property("order-insensitive: any row permutation matches the reference"):
    forAll(distinctResultGen) { ref =>
      val shuffledGen = Gen.const(ref.rows).flatMap(Gen.pick(ref.rows.size, _)).map(_.toList)
      forAll(shuffledGen) { shuffled =>
        val learner = ref.copy(rows = shuffled)
        Check.check("select", learner, ref, Checker(orderSensitive = false)) == CheckOutcome.Pass
      }
    }

  property("order-sensitive: a reversed (distinct) result fails"):
    forAll(distinctResultGen.suchThat(r => r.rows.distinct.size >= 2)) { ref =>
      val deduped = ref.copy(rows = ref.rows.distinct)
      val learner = deduped.copy(rows = deduped.rows.reverse)
      // reverse of >=2 distinct rows differs from the original order
      Prop(deduped.rows != learner.rows) ==> {
        Check.check("select", learner, deduped, Checker(orderSensitive = true)) match
          case CheckOutcome.Fail(_) => true
          case CheckOutcome.Pass    => false
      }
    }

  property("no shape rule: identical results pass, different results fail"):
    forAll(distinctResultGen) { ref =>
      val pass = Check.check("x", ref, ref, Checker(orderSensitive = false)) == CheckOutcome.Pass
      val different = ref.copy(rows = ref.rows.drop(1))
      val fail = Check.check("x", different, ref, Checker(orderSensitive = false)) != CheckOutcome.Pass
      pass && fail
    }

  test("SQL shape rule that is not satisfied fails regardless of results"):
    val r = QueryResult(List("n"), List(List(Some("1"))))
    val checker = Checker(orderSensitive = false, shapeRule = Some(SqlShapeRule(List("join"))))
    assertEquals(
      Check.check("select * from t", r, r, checker),
      CheckOutcome.Fail("SQL shape requirement not met")
    )

  test("SQL shape rule that is satisfied falls through to result comparison"):
    val r = QueryResult(List("n"), List(List(Some("1"))))
    val checker = Checker(orderSensitive = false, shapeRule = Some(SqlShapeRule(List("join"))))
    assertEquals(Check.check("select * from a JOIN b", r, r, checker), CheckOutcome.Pass)

package drillbanken.domain.check

import drillbanken.domain.*

/** Result-equality checker (FR-011, FR-012, research.md D5).
  * Canonicalize each result (lowercase cols, stringify rows), sort the rows unless the
  * task is order-sensitive, then compare. An optional SQL-shape rule is applied first.
  */
object Check:

  final case class CanonResult(cols: List[String], rows: List[String])

  private val Sep = ""
  private val Null = "∅" // ∅ marker so a NULL never collides with the literal string "null"

  def canonicalize(r: QueryResult, orderSensitive: Boolean): CanonResult =
    val cols = r.cols.map(_.toLowerCase)
    val rows0 = r.rows.map(_.map(_.getOrElse(Null)).mkString(Sep))
    val rows = if orderSensitive then rows0 else rows0.sorted
    CanonResult(cols, rows)

  def check(
      learnerSql: String,
      learner: QueryResult,
      reference: QueryResult,
      c: Checker
  ): CheckOutcome =
    c.shapeRule match
      case Some(rule) if !rule.satisfiedBy(learnerSql) =>
        CheckOutcome.Fail("SQL shape requirement not met")
      case _ =>
        if canonicalize(learner, c.orderSensitive) == canonicalize(reference, c.orderSensitive)
        then CheckOutcome.Pass
        else CheckOutcome.Fail("Result set does not match the reference")

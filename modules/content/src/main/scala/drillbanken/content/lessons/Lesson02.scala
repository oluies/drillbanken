package drillbanken.content
package lessons

import drillbanken.domain.*

/** Lesson 02 — NULL handling and aggregates on the trading-book seed (T050, FR-018, US5).
  * Demonstrates that a new lesson is added purely as data (no engine change).
  */
object Lesson02:

  private def t(sv: String, en: String) = LocalizedText(sv, en)
  private val looseCheck = Checker(orderSensitive = false)

  val lesson: LessonDef = LessonDef(
    id = LessonId("nulls-and-aggregates"),
    sequence = 2,
    title = t("NULL och aggregat", "NULL and aggregates"),
    endRequirement = t(
      "Du kan hantera NULL och aggregera korrekt över saknade värden.",
      "You can handle NULL and aggregate correctly over missing values."
    ),
    seed = SeedRef.TradingBook,
    demo = Transcript(
      List(
        TranscriptStep(
          input = "SELECT trade_id, price FROM trades",
          output = QueryResultView(
            cols = List("trade_id", "price"),
            rows = List(
              List(Some("1"), Some("12.50")),
              List(Some("5"), None)
            )
          ),
          delayMs = 800,
          annotation = Some(t("En affär saknar pris (NULL).", "One trade has no price (NULL)."))
        )
      )
    ),
    parts = List(
      PartDrill(
        id = PartId("null-price"),
        prompt = t("Vilka affärer saknar pris? (trade_id)", "Which trades have no price? (trade_id)"),
        checker = looseCheck,
        hints = List(Hint(t("WHERE price IS NULL.", "WHERE price IS NULL."), cost = 10)),
        reference = ReferenceSolution("SELECT trade_id FROM trades WHERE price IS NULL")
      ),
      PartDrill(
        id = PartId("avg-ignores-null"),
        prompt = t("Genomsnittligt pris över alla affärer (avg_price).", "Average price across all trades (avg_price)."),
        checker = looseCheck,
        hints = List(Hint(t("AVG hoppar över NULL.", "AVG skips NULL."), cost = 10)),
        reference = ReferenceSolution("SELECT AVG(price) AS avg_price FROM trades")
      )
    ),
    whole = WholeTask(
      prompt = t(
        "Per instrument: symbol, antal affärer och total kvantitet, även otraderade (symbol, n, total_qty).",
        "Per instrument: symbol, number of trades and total quantity, including untraded (symbol, n, total_qty)."
      ),
      checker = looseCheck,
      hints = List(Hint(t("LEFT JOIN + COUNT + COALESCE(SUM…,0).", "LEFT JOIN + COUNT + COALESCE(SUM…,0)."), cost = 10)),
      reference = ReferenceSolution(
        "SELECT i.symbol, COUNT(t.trade_id) AS n, COALESCE(SUM(t.qty), 0) AS total_qty " +
          "FROM instruments i LEFT JOIN trades t ON t.instrument_id = i.instrument_id GROUP BY i.symbol"
      )
    ),
    exam = Exam(
      prompt = t("Vilka handlare har en affär utan pris? (name)", "Which traders have a trade with no price? (name)"),
      checker = looseCheck,
      reference = ReferenceSolution(
        "SELECT DISTINCT tr.name FROM traders tr JOIN trades t ON t.trader_id = tr.trader_id WHERE t.price IS NULL"
      ),
      rubric = Rubric(
        maxPoints = 100,
        correctnessWeight = 70,
        attemptPenalty = 10,
        hintPenalty = 10,
        timePenaltyPerSec = 0,
        timeBoxSec = None,
        passThreshold = 60
      )
    )
  )

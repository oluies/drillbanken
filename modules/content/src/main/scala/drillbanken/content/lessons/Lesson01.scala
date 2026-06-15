package drillbanken.content
package lessons

import drillbanken.domain.*

/** Lesson 01 — joins and NULL/orphan handling on the trading-book seed (T027, FR-018).
  * Bilingual; checkers are order-insensitive (no ORDER BY required).
  */
object Lesson01:

  private def t(sv: String, en: String) = LocalizedText(sv, en)
  private val looseCheck = Checker(orderSensitive = false)

  val lesson: LessonDef = LessonDef(
    id = LessonId("joins-and-nulls"),
    sequence = 1,
    title = t("Joins och NULL", "Joins and NULL"),
    endRequirement = t(
      "Du kan koppla tabeller och hitta rader som saknar motpart.",
      "You can join tables and find rows with no counterpart."
    ),
    seed = SeedRef.TradingBook,
    demo = Transcript(
      List(
        TranscriptStep(
          input = "FROM trades SELECT trade_id, trader_id, qty LIMIT 3",
          output = QueryResultView(
            cols = List("trade_id", "trader_id", "qty"),
            rows = List(
              List(Some("1"), Some("1"), Some("100")),
              List(Some("2"), Some("1"), Some("50")),
              List(Some("3"), Some("2"), Some("1000"))
            )
          ),
          delayMs = 700,
          annotation = Some(t("Affärer har en trader_id.", "Trades carry a trader_id."))
        ),
        TranscriptStep(
          input = "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id",
          output = QueryResultView(
            cols = List("trade_id", "name"),
            rows = List(List(Some("1"), Some("Alice")), List(Some("2"), Some("Alice")))
          ),
          delayMs = 900,
          annotation = Some(t("JOIN kopplar affär till handlare.", "JOIN links a trade to its trader."))
        )
      )
    ),
    parts = List(
      PartDrill(
        id = PartId("inner-join"),
        prompt = t(
          "Lista varje affärs id och handlarens namn (trade_id, name).",
          "List each trade's id and the trader's name (trade_id, name)."
        ),
        checker = looseCheck,
        hints = List(
          Hint(t("Använd JOIN på trader_id.", "Use JOIN on trader_id."), cost = 10)
        ),
        reference = ReferenceSolution(
          "SELECT t.trade_id, tr.name FROM trades t JOIN traders tr ON tr.trader_id = t.trader_id"
        )
      ),
      PartDrill(
        id = PartId("orphan-traders"),
        prompt = t(
          "Vilka handlare har aldrig handlat? (name)",
          "Which traders have never traded? (name)"
        ),
        checker = looseCheck,
        hints = List(
          Hint(t("NOT IN (SELECT trader_id FROM trades).", "NOT IN (SELECT trader_id FROM trades)."), cost = 10)
        ),
        reference = ReferenceSolution(
          "SELECT name FROM traders WHERE trader_id NOT IN (SELECT trader_id FROM trades)"
        )
      )
    ),
    whole = WholeTask(
      prompt = t(
        "Visa varje handlares namn och total handlad kvantitet, även de utan affärer (name, total).",
        "Show each trader's name and total traded quantity, including those with none (name, total)."
      ),
      checker = looseCheck,
      hints = List(
        Hint(t("LEFT JOIN + COALESCE(SUM(qty),0).", "LEFT JOIN + COALESCE(SUM(qty),0)."), cost = 10)
      ),
      reference = ReferenceSolution(
        "SELECT tr.name, COALESCE(SUM(t.qty), 0) AS total FROM traders tr " +
          "LEFT JOIN trades t ON t.trader_id = tr.trader_id GROUP BY tr.name"
      )
    ),
    exam = Exam(
      prompt = t(
        "Vilka instrument har aldrig handlats? (symbol)",
        "Which instruments have never been traded? (symbol)"
      ),
      checker = looseCheck,
      reference = ReferenceSolution(
        "SELECT symbol FROM instruments WHERE instrument_id NOT IN (SELECT instrument_id FROM trades)"
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

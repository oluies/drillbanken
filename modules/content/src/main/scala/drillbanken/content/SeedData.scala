package drillbanken.content

/** The trading-book seed dataset (FR-020, research.md D6): an ordered list of DDL/DML
  * executed sequentially on connect; the reset path re-runs it. Deliberate NULLs and
  * orphan rows give join/NULL/anti-join lessons real teeth:
  *   - trader 'Eve' has no trades (orphan on the trades side)
  *   - instrument 'GOLD' is never traded (orphan on the trades side)
  *   - one trade has a NULL price
  * Relationships are conceptual — no FK constraints (so orphan-row lessons are meaningful).
  */
object SeedData:

  val tradingBook: List[String] = List(
    "CREATE OR REPLACE TABLE traders (trader_id INTEGER, name VARCHAR, desk VARCHAR);",
    "CREATE OR REPLACE TABLE instruments (instrument_id INTEGER, symbol VARCHAR, asset_class VARCHAR);",
    """CREATE OR REPLACE TABLE trades (
      trade_id INTEGER, trader_id INTEGER, instrument_id INTEGER, qty INTEGER, price DECIMAL(12,2));""",
    // traders — Eve will have no trades (orphan)
    """INSERT INTO traders VALUES
      (1, 'Alice', 'Equities'),
      (2, 'Bob',   'Rates'),
      (3, 'Carol', 'FX'),
      (4, 'Eve',   'Equities');""",
    // instruments — GOLD (4) is never traded (orphan)
    """INSERT INTO instruments VALUES
      (1, 'ACME', 'Equity'),
      (2, 'GLOBE','Equity'),
      (3, 'USDSEK','FX'),
      (4, 'GOLD', 'Commodity');""",
    // trades — trade 5 has a NULL price
    """INSERT INTO trades VALUES
      (1, 1, 1, 100, 12.50),
      (2, 1, 2,  50, 88.10),
      (3, 2, 3, 1000, 10.95),
      (4, 3, 3,  500, 11.02),
      (5, 2, 1,  -25, NULL);"""
  )

  def forRef(ref: SeedRef): List[String] = ref match
    case SeedRef.TradingBook => tradingBook

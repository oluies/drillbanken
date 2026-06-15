package drillbanken.app

import com.raquo.laminar.api.L.*

/** A visual reference for the seed schema, rendered below the console: one card per table
  * with columns/types, relationship arrows, and the deliberate NULL/orphan notes that give
  * the join/anti-join lessons teeth (FR-020). Static — the v1 seed shape is fixed.
  */
object SchemaPanel:

  private def card(name: String, cols: List[(String, String)], note: String): Element =
    div(
      cls := "tbl",
      div(cls := "tbl-h", name),
      ul(cols.map((c, t) => li(span(cls := "c", c), span(cls := "t", t)))),
      div(cls := "note", note)
    )

  private val css =
    """
    .schema { margin-top: 14px; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: #cdd6f4; }
    .schema h3 { margin: 0 0 4px; font-size: 14px; color: #89b4fa; }
    .schema .sub { margin: 0 0 10px; font-size: 12px; color: #a6adc8; }
    .schema .cards { display: flex; gap: 12px; flex-wrap: wrap; }
    .schema .tbl { border: 1px solid #45475a; border-radius: 8px; min-width: 200px; background: #1e1e2e; overflow: hidden; }
    .schema .tbl-h { background: #313244; padding: 6px 10px; font-weight: 700; color: #f5e0dc; }
    .schema .tbl ul { list-style: none; margin: 0; padding: 6px 10px; }
    .schema .tbl li { display: flex; justify-content: space-between; gap: 18px; font-size: 13px; padding: 2px 0; }
    .schema .tbl .t { color: #a6adc8; }
    .schema .tbl .note { padding: 6px 10px; font-size: 12px; color: #f9e2af; border-top: 1px dashed #45475a; }
    .schema .rel { font-size: 12px; color: #a6adc8; margin-top: 10px; }
    .schema .rel b { color: #94e2d5; }
    """

  def view: Element =
    div(
      cls := "schema",
      styleTag(css),
      h3("Datamodell · Data model — trading book"),
      p(cls := "sub", "Try ", b("tables"), " or ", b("describe trades"), " in the console."),
      div(
        cls := "cards",
        card(
          "traders",
          List("trader_id" -> "INTEGER (PK)", "name" -> "VARCHAR", "desk" -> "VARCHAR"),
          "Eve har inga affärer · Eve has no trades"
        ),
        card(
          "instruments",
          List("instrument_id" -> "INTEGER (PK)", "symbol" -> "VARCHAR", "asset_class" -> "VARCHAR"),
          "GOLD handlas aldrig · GOLD is never traded"
        ),
        card(
          "trades",
          List(
            "trade_id" -> "INTEGER",
            "trader_id" -> "→ traders",
            "instrument_id" -> "→ instruments",
            "qty" -> "INTEGER",
            "price" -> "DECIMAL · NULL i #5"
          ),
          "price är NULL för affär 5 · price is NULL for trade 5"
        )
      ),
      p(
        cls := "rel",
        b("trades.trader_id"), " → traders   ·   ", b("trades.instrument_id"), " → instruments",
        "   (konceptuellt, inga FK · conceptual, no foreign keys)"
      )
    )

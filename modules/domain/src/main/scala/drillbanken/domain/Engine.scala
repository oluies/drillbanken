package drillbanken.domain

/** Boundary value types the domain shares with the engine module (contracts/engine-service.md).
  * The domain only ever sees an already-materialized [[QueryResult]]; Arrow handling lives in
  * the `engine` module behind `EngineService`.
  */
final case class QueryResult(cols: List[String], rows: List[List[Option[String]]])

object QueryResult:
  val empty: QueryResult = QueryResult(Nil, Nil)

/** Lifecycle of the SQL engine, surfaced to the UI as a Signal in the app layer. */
enum EngineStatus:
  case Loading
  case Ready(version: String)
  case Failed(message: String)

/** Typed mapping of a thrown engine error (never an uncaught exception). */
final case class EngineError(message: String)

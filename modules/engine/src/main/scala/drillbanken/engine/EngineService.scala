package drillbanken.engine

import drillbanken.domain.*
import scala.concurrent.Future

/** Narrow, Laminar-free SQL engine contract (contracts/engine-service.md). The interop
  * spike (Milestone 0) implements this over DuckDB-WASM via ScalablyTyped facades:
  *   - TODO(T010): generate ScalablyTyped facades for `@duckdb/duckdb-wasm`
  *   - TODO(T011): bootstrap (selectBundle → ?url worker → AsyncDuckDB → connect → PRAGMA version)
  *   - TODO(T012): exec with Arrow → QueryResult materialization + typed error mapping
  *   - TODO(T018): resetSeed runs the content `SeedData`
  */
trait EngineService:
  /** Latest lifecycle status; the app lifts this into an Airstream Signal at the UI boundary. */
  def currentStatus: EngineStatus
  def exec(sql: String): Future[QueryResult]
  def resetSeed(): Future[Unit]

object EngineService:
  /** Placeholder until the spike lands — lets the app compile and wire the shell. */
  val pending: EngineService = new EngineService:
    def currentStatus: EngineStatus = EngineStatus.Failed("engine not implemented — interop spike pending")
    def exec(sql: String): Future[QueryResult] =
      Future.failed(new NotImplementedError("EngineService.exec — see tasks T011/T012"))
    def resetSeed(): Future[Unit] =
      Future.failed(new NotImplementedError("EngineService.resetSeed — see task T018"))

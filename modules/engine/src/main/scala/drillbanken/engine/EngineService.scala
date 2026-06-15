package drillbanken.engine

import drillbanken.domain.*
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.scalajs.dom

/** Narrow, Laminar-free SQL engine contract (contracts/engine-service.md). */
trait EngineService:
  /** Boot the engine; resolves once `currentStatus` is `Ready` (or `Failed`). */
  def boot(): Future[Unit]
  def currentStatus: EngineStatus
  def exec(sql: String): Future[QueryResult]
  def resetSeed(): Future[Unit]

object EngineService:
  /** Placeholder used by tooling/tests before/without a browser engine. */
  val pending: EngineService = new EngineService:
    def boot(): Future[Unit] = Future.successful(())
    def currentStatus: EngineStatus = EngineStatus.Failed("engine not booted")
    def exec(sql: String): Future[QueryResult] =
      Future.failed(new IllegalStateException("engine not booted"))
    def resetSeed(): Future[Unit] = Future.successful(())

  /** Live DuckDB-WASM engine. `seed` is the ordered DDL/DML run on connect (FR-020). */
  def duckDb(seed: List[String]): EngineService = new DuckDbEngineService(seed)

/** DuckDB-WASM implementation (T011/T012/T018, research.md D3/D4). */
final class DuckDbEngineService(seed: List[String]) extends EngineService:
  private var status0: EngineStatus = EngineStatus.Loading
  private var connection: Option[AsyncDuckDBConnection] = None

  def currentStatus: EngineStatus = status0

  def boot(): Future[Unit] =
    val bundles = js.Dictionary[js.Any](
      "mvp" -> js.Dynamic.literal(
        mainModule = DuckDbAssets.mvpWasm,
        mainWorker = DuckDbAssets.mvpWorker
      ),
      "eh" -> js.Dynamic.literal(
        mainModule = DuckDbAssets.ehWasm,
        mainWorker = DuckDbAssets.ehWorker
      )
    )
    val booted =
      for
        bundle <- DuckDb.selectBundle(bundles).toFuture
        worker = new dom.Worker(bundle.mainWorker)
        db = new AsyncDuckDB(new VoidLogger(), worker)
        _ <- db.instantiate(bundle.mainModule, bundle.pthreadWorker).toFuture
        conn <- db.connect().toFuture
        _ <- runSeed(conn, seed)
        version <- pragmaVersion(conn)
      yield
        connection = Some(conn)
        status0 = EngineStatus.Ready(version)
    booted.recover { case t =>
      status0 = EngineStatus.Failed(Option(t.getMessage).getOrElse(t.toString))
    }

  def exec(sql: String): Future[QueryResult] =
    connection match
      case None => Future.failed(new IllegalStateException("engine not ready"))
      case Some(conn) =>
        conn
          .query(sql)
          .toFuture
          .map(materialize)
          .recoverWith { case t => Future.failed(EngineErrorException(messageOf(t))) }

  def resetSeed(): Future[Unit] =
    connection match
      case None       => Future.failed(new IllegalStateException("engine not ready"))
      case Some(conn) => runSeed(conn, seed)

  private def runSeed(conn: AsyncDuckDBConnection, stmts: List[String]): Future[Unit] =
    stmts.foldLeft(Future.successful(())) { (acc, stmt) =>
      acc.flatMap(_ => conn.query(stmt).toFuture.map(_ => ()))
    }

  private def pragmaVersion(conn: AsyncDuckDBConnection): Future[String] =
    conn.query("PRAGMA version").toFuture.map { table =>
      val r = materialize(table)
      r.rows.headOption.flatMap(_.headOption).flatten.getOrElse("unknown")
    }

  /** Arrow → flat QueryResult (research.md D4). */
  private def materialize(table: ArrowTable): QueryResult =
    val cols = table.schema.fields.toList.map(_.name)
    val rows = table.toArray().toList.map { row =>
      val json = row.toJSON()
      cols.map(c => formatCell(json.get(c)))
    }
    QueryResult(cols, rows)

  /** Per-cell formatting (research.md D4): bigint→string, others→toString, null/undef→None. */
  private def formatCell(value: Option[js.Any]): Option[String] =
    value.flatMap { v =>
      if v == null || js.isUndefined(v) then None
      else js.typeOf(v) match
        case "bigint" => Some(v.asInstanceOf[js.Any].toString)
        case "object" => Some(JSON.stringify(v))
        case _        => Some(v.toString)
    }

  private def messageOf(t: Throwable): String =
    Option(t.getMessage).getOrElse(t.toString)

/** Typed engine error surfaced through the Future (contracts/engine-service.md). */
final case class EngineErrorException(message: String) extends RuntimeException(message)

@js.native
@JSGlobal("JSON")
private object JSON extends js.Object:
  def stringify(value: js.Any): String = js.native

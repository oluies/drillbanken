package drillbanken.engine

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import org.scalajs.dom

/** Minimal hand-written `js.native` facade for `@duckdb/duckdb-wasm` — only the surface
  * the engine uses (T010, research.md D2/D3). ScalablyTyped was the clarified choice but
  * OOM'd the Scala compiler on the full duckdb-wasm + Apache Arrow facade tree, so we
  * vendor only what we need behind the narrow `EngineService`.
  */

/** The `.wasm` modules and matching workers, imported as Vite `?url` assets so nothing is
  * fetched from a CDN at runtime (research.md D3). Each resolves to a bundled URL string.
  */
object DuckDbAssets:
  @js.native @JSImport("@duckdb/duckdb-wasm/dist/duckdb-mvp.wasm?url", JSImport.Default)
  val mvpWasm: String = js.native

  @js.native @JSImport("@duckdb/duckdb-wasm/dist/duckdb-browser-mvp.worker.js?url", JSImport.Default)
  val mvpWorker: String = js.native

  @js.native @JSImport("@duckdb/duckdb-wasm/dist/duckdb-eh.wasm?url", JSImport.Default)
  val ehWasm: String = js.native

  @js.native @JSImport("@duckdb/duckdb-wasm/dist/duckdb-browser-eh.worker.js?url", JSImport.Default)
  val ehWorker: String = js.native

@js.native
trait DuckDBBundle extends js.Object:
  val mainModule: String = js.native
  val mainWorker: String = js.native
  val pthreadWorker: js.UndefOr[String] = js.native

@js.native
trait Logger extends js.Object

@js.native
@JSImport("@duckdb/duckdb-wasm", "VoidLogger")
class VoidLogger() extends Logger

@js.native
@JSImport("@duckdb/duckdb-wasm", "AsyncDuckDB")
class AsyncDuckDB(logger: Logger, worker: dom.Worker) extends js.Object:
  def instantiate(mainModule: String, pthreadWorker: js.UndefOr[String]): js.Promise[Unit] = js.native
  def connect(): js.Promise[AsyncDuckDBConnection] = js.native
  def terminate(): js.Promise[Unit] = js.native

@js.native
trait AsyncDuckDBConnection extends js.Object:
  def query(text: String): js.Promise[ArrowTable] = js.native

// --- Minimal Apache Arrow result surface (research.md D4) ---

@js.native
trait ArrowTable extends js.Object:
  def schema: ArrowSchema = js.native
  def toArray(): js.Array[ArrowRow] = js.native

@js.native
trait ArrowSchema extends js.Object:
  def fields: js.Array[ArrowField] = js.native

@js.native
trait ArrowField extends js.Object:
  val name: String = js.native

@js.native
trait ArrowRow extends js.Object:
  def toJSON(): js.Dictionary[js.Any] = js.native

object DuckDb:
  /** `selectBundle` picks MVP vs EH at runtime from a bundle map. */
  @js.native @JSImport("@duckdb/duckdb-wasm", "selectBundle")
  def selectBundle(bundles: js.Dictionary[js.Any]): js.Promise[DuckDBBundle] = js.native

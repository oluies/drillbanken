# Contract: EngineService

The narrow Scala API the rest of the app uses to run SQL. Owns DuckDB-WASM bootstrap,
the Web Worker, and Arrow materialization behind it; consumers never see ScalablyTyped or
Arrow types. Lives in `modules/engine`. — Principle III, FR-009

## Types

```
QueryResult  = { cols: List[String], rows: List[List[String | Null]] }   // materialized
EngineStatus = Loading | Ready(version: String) | Failed(message: String)
EngineError  = { message: String }
```

## Interface

```
trait EngineService:
  def status: Signal[EngineStatus]                 // Loading → Ready(version) | Failed(msg)
  def exec(sql: String): Future[QueryResult]       // rejects with EngineError on failure
  def resetSeed(): Future[Unit]                    // re-runs the seed DDL/DML — FR-020
```

> `status` is exposed as an Airstream `Signal` for the UI, but the service core itself is
> Laminar-free; the Signal is produced at the thin Laminar boundary.

## Behavioral contract

- **Boot** (D3): `selectBundle` → import `.wasm` + worker `.js` as Vite `?url` assets (no
  runtime CDN) → `new Worker` → `new AsyncDuckDB(VoidLogger, worker)` →
  `instantiate(mainModule, pthreadWorker)` → `connect()` → run seed → `PRAGMA version`.
  On success `status` becomes `Ready(version)`; the version string IS the `PRAGMA version`
  value.
- **exec**: returns a fully materialized `QueryResult` (Arrow already flattened per D4:
  `schema.fields` for cols; `toArray().toJSON()` for rows; bigint→string, Date→ISO,
  non-integer number→fixed(6), null/undefined→null).
- **Error mapping**: a thrown engine error is mapped to a failed `Future[EngineError]`
  (and, during boot, to `status = Failed(msg)`) — never an uncaught console exception.
  — Milestone-0 deliberate-error acceptance.
- **No CDN at runtime**: assets are bundled; the contract forbids network fetches of the
  engine.

## Acceptance (Milestone 0 spike)

- `status` transitions `Loading → Ready(<PRAGMA version>)` on a clean boot.
- `exec("SELECT 1 AS n")` yields `QueryResult(cols=["n"], rows=[["1"]])`.
- `exec("SELECT * FROM nope")` yields a failed Future carrying `EngineError`, surfaced as
  a typed error value (not an exception).

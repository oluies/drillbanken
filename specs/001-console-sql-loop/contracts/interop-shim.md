# Contract — TypeScript interop shim

The single untyped surface between Scala.js and the browser libraries (xterm.js,
DuckDB-WASM). Lives in `ts/shim.ts`; Scala.js consumes it through a typed facade. Arrow
materialization stays here (D1/D6).

## SQL engine

```ts
// Result is already flattened off Apache Arrow — no Arrow types cross into Scala.js.
export interface QueryResult {
  cols: string[];               // column names, original case
  rows: (string | null)[][];    // every cell pre-formatted to string | null
}

export interface EngineError {
  message: string;              // engine error text, surfaced as a typed Failed value
}

// Boot the engine (selectBundle MVP/EH, ?url wasm + worker, AsyncDuckDB + VoidLogger,
// connect, run seed). Resolves with the PRAGMA version string.
export function boot(): Promise<string>;

// Run one statement. Rejects with EngineError on engine failure (mapped to a typed value).
export function exec(sql: string): Promise<QueryResult>;

// Re-run the seed DDL/DML (reset data).
export function seed(): Promise<void>;
```

**Cell formatting (in shim, from prior art)**: `bigint`→decimal string; `Date`→ISO
`"YYYY-MM-DD HH:MM:SS"`; non-integer `number`→fixed(6); `null`/`undefined`→`null`.

## Terminal

```ts
export interface Term {
  open(el: HTMLElement): void;        // attach xterm to a DOM element
  write(data: string): void;          // write output (supports ANSI)
  onData(cb: (s: string) => void): void;  // keystrokes / pasted input
  clear(): void;
  dispose(): void;
}

export function createTerminal(): Term;
```

## Scala.js facade expectations

- `boot()`/`exec()` map to `scala.concurrent.Future`; `exec` failure maps to a typed
  `EngineStatus.Failed(msg)` / error result — never an unhandled JS exception.
- `EngineStatus = Loading | Ready(version) | Failed(msg)` is owned by the `duckdb` module and
  exposed as an Airstream `Signal[EngineStatus]`.
- `QueryResult` maps to the domain's `{ cols: Seq[String], rows: Seq[Seq[String | Null]] }`.

## Spike acceptance (Milestone 0)

1. `boot()` resolves and the version renders into `Signal[EngineStatus] = Ready(version)`.
2. One `exec` result renders as a Laminar-bound table.
3. One deliberately invalid statement yields a typed error value shown in the UI (not a
   thrown console error).

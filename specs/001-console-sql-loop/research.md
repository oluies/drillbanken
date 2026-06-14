# Phase 0 Research — Console SQL Tutor (v1)

All decisions below are resolved; no NEEDS CLARIFICATION remain. Items marked *(settled
prior art)* are carried from the deployed sibling project **SQL Concepts Lab**
(`oluies/sql-concepts-lab`) and are cited, not re-derived.

## D1 — JS interop: thin TypeScript shim

- **Decision**: Reach xterm.js and DuckDB-WASM through one narrow TypeScript shim exposing
  `exec(sql): Promise<Result>` and terminal `open/write/onData`. Apache Arrow → `{cols,
  rows}` materialization happens in TS.
- **Rationale**: DuckDB-WASM returns Arrow tables; flattening them (`schema.fields` for
  names; `.toArray()`/`.toJSON()`; format `bigint`→string, `Date`→ISO, non-integer
  numbers→fixed) is awkward through a `js.native` Arrow facade in Scala.js. Keeping it in TS
  gives the Scala core a clean, fully-typed contract and the smallest untyped surface.
- **Alternatives considered**: hand-written `js.native` facades (forces Arrow handling into
  Scala.js dynamic types); ScalablyTyped full facades (heavy build, large generated surface,
  dependency-bump churn — overkill for the narrow API used).

## D2 — Lesson content format: structured data files

- **Decision**: One self-contained JSON artifact per lesson under `content/lessons/`,
  validated against a schema at load (see `contracts/lesson-schema.md`).
- **Rationale**: Honors Constitution V / FR-026..028 — non-Scala authors add lessons with
  no rebuild; malformed lessons are reported without breaking others.
- **Alternatives considered**: in-bundle Scala DSL (compile-time safety but requires Scala +
  rebuild; compiles content into the bundle — what SQL Concepts Lab did); markdown +
  frontmatter (nice prose but drills/checkers/rubrics are structured data that fit poorly).
- **Trade-off accepted**: runtime data needs explicit validation that a DSL would get free
  at compile time → addressed by a strict loader + clear error reporting.

## D3 — Grading scale: points internally, IG/G/VG displayed

- **Decision**: Rubric computes points from correctness, attempts, hint usage, and optional
  time-box; per-lesson thresholds map points → IG / G / VG for display.
- **Rationale**: Continuous internal scoring keeps the rubric expressive and the "drill
  again" prioritization fine-grained, while the learner sees the familiar Swedish grade.
- **Alternatives considered**: IG/G/VG-only (too coarse for remediation ranking); points-only
  (loses the IG/G/VG framing the brief prefers).

## D4 — VISA implementation: pre-recorded timed transcript

- **Decision**: VISA (and the slow INSTRUERA replay) play a pre-recorded, timed transcript
  (asciinema-style frames) stored in the lesson artifact.
- **Rationale**: Deterministic pacing and annotation; trivial to pause/rewind/replay
  mid-drill (US3); decoupled from live engine timing/state. Live execution is reserved for
  ÖVA/PRÖVA where the learner acts.
- **Alternatives considered**: scripted live execution (always truthful, engine boots fast
  so viable, but timing/pacing harder to control and a demo shouldn't depend on engine
  state).

## D5 — Internationalization: typed Scala catalog + native Intl, bilingual from v1

- **Decision**: `enum Locale { Sv, En }` (Sv default). UI strings in a typed message catalog
  keyed so missing keys are compile errors; active locale in an Airstream `Var[Locale]` for
  reactive Laminar re-render. Number/date/plural formatting via browser-native `Intl` behind
  a tiny facade. Lesson artifacts carry per-locale field variants with default-locale
  fallback.
- **Rationale**: First-class i18n from day one (user requirement) without bundle weight or
  giving up a fully-typed core; designing localized content/keys in now is far cheaper than
  retrofitting.
- **Alternatives considered**: JS i18n library (i18next / `@formatjs/intl`) wrapped behind
  the shim for ICU MessageFormat — deferred unless rich ICU pluralization across many
  locales becomes necessary.

## D6 — DuckDB-WASM bootstrap *(settled prior art)*

- **Decision**: `selectBundle` over MVP/EH bundles; `.wasm` + worker imported as Vite `?url`
  assets; `new AsyncDuckDB(VoidLogger, worker)` → `instantiate` → `connect` → run seed →
  `PRAGMA version`. `exec` materializes Arrow to `{cols, rows}` (see D1). No runtime CDN.
- **Rationale**: Proven in SQL Concepts Lab; the spike (`duckdb` module) reproduces it under
  Scala.js + the TS shim.

## D7 — Judging / checkers *(settled prior art)*

- **Decision**: Compare result sets by canonicalization — lowercase column names, JSON-encode
  rows, sort rows unless the task is order-sensitive — then equality. Optional SQL-shape
  constraint evaluated additionally where a task declares one.
- **Rationale**: Accepts equivalent phrasings (join vs subquery) while catching wrong
  results; proven approach. Order-sensitive tasks skip the sort.

## D8 — Sample dataset *(settled prior art)*

- **Decision**: Seed = ordered DDL/DML strings executed on connect (trading book: traders /
  instruments / trades) with deliberate gaps/NULLs (a trader with no trades, an instrument
  never traded, NULL prices) and no FK constraints, so join/NULL/anti-join skills are
  meaningful. A reset path re-runs the seed.

## D9 — Persistence & export

- **Decision**: Progress + locale preference in `localStorage` (single namespaced key);
  export/import a single JSON document (see `contracts/progress-schema.md`) with version
  field and validation on import. OPFS deferred.
- **Rationale**: Simplest durable client-side store; file is the only portability path with
  no backend; versioned schema guards against incompatible imports.

## D10 — Build & CI: sbt (Scala.js) + Vite, dual-toolchain CI *(extends prior art)*

- **Decision**: sbt + `@scala-js/vite-plugin-scalajs` → Vite static `dist/` → GitHub Pages
  via Actions, reusing sql-concepts-lab's exact Pages job versions. **Net-new**: add a JDK +
  sbt + Coursier toolchain (cached) alongside `setup-node` since sql-concepts-lab's workflow
  is Node-only. Enable Pages before first deploy (the "Pages site Not Found" gotcha).
- **Rationale**: Reuses a known-good deploy path; the only addition is the JVM build stage.

## DuckDB SQL dialect notes (for content/checker authors) *(settled prior art)*

`ANTI JOIN` / `SEMI JOIN` are DuckDB-specific (standard: `NOT EXISTS` / `EXISTS`); `[ ... ]`
is a LIST literal, not a quoted identifier; DuckDB supports FROM-first (`FROM t SELECT …`),
`QUALIFY`, and `FETCH FIRST n ROWS ONLY` alongside `LIMIT`. Lessons should teach the
standard form and note the DuckDB-specific sugar.

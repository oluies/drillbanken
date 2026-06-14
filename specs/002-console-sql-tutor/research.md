# Phase 0 Research: Console SQL Tutor (Drillbänken v1)

All five clarification decisions are resolved (see spec Clarifications, 2026-06-14), and
the engine/deploy facts below are **settled prior art** from the deployed sibling project
*SQL Concepts Lab* (`oluies/sql-concepts-lab`). They are cited as authoritative context,
not re-derived. No `NEEDS CLARIFICATION` markers remain.

---

## D1 — Frontend stack

- **Decision**: Pure Scala 3 + Scala.js, UI in Laminar (Airstream signals). No Vue/React.
- **Rationale**: Constitution Principle III; lets the pedagogy invariants live in a pure,
  property-tested domain module with a thin reactive UI over it.
- **Alternatives considered**: Vue bindings and a hybrid Vue/Scala split — both
  previously considered and rejected (recorded as a resolved decision in the spec).

## D2 — JS interop strategy

- **Decision**: **ScalablyTyped-generated facades** for `@duckdb/duckdb-wasm` and
  `@xterm/xterm`, wrapped behind narrow hand-authored services (`EngineService`,
  `ConsoleService`). Arrow→`{cols, rows}` materialization happens inside `EngineService`.
- **Rationale**: Clarified choice. Avoids hand-maintaining facades for two evolving
  libraries; the narrow service wrapper preserves the constitution's narrow-boundary
  intent so the rest of the app never touches generated types.
- **Alternatives considered**:
  - Hand-written minimal `js.native` facades — minimal surface, but the Arrow
    materialization (below) is awkward to express in Scala.js.
  - A thin hand-written TypeScript shim returning flattened rows — viable, but the team
    chose generated facades to keep all logic in Scala behind the service.
- **Risk to validate in Milestone 0**: ScalablyTyped conversion of the DuckDB-WASM and
  xterm type definitions must succeed and compile; the spike is the proof.

## D3 — DuckDB-WASM bootstrap (settled)

- **Decision**: Reuse the proven bootstrap, expressed in the Scala `engine` service:
  1. `duckdb.selectBundle({ mvp, eh })` to pick the bundle.
  2. Import the `.wasm` and matching worker `.js` as Vite **`?url`** assets (no runtime
     CDN).
  3. `new Worker(bundle.mainWorker)` → `new AsyncDuckDB(new VoidLogger(), worker)` →
     `db.instantiate(bundle.mainModule, bundle.pthreadWorker)`.
  4. `db.connect()`, run the seed, `PRAGMA version` → version string for
     `EngineStatus.Ready(version)`.
- **Rationale**: Verified in production in sql-concepts-lab; same Vite + Pages story.
- **Engine version**: `@duckdb/duckdb-wasm ^1.33.1-dev45.0` (DuckDB ~v1.5.x).

## D4 — Arrow result materialization (settled)

- **Decision**: Inside `EngineService.exec`, materialize Arrow to `QueryResult`:
  - Columns: `result.schema.fields.map(_.name)`.
  - Rows: `result.toArray().map(_.toJSON())`, then per cell:
    `bigint`→`toString`, `Date`→ISO `"YYYY-MM-DD HH:MM:SS"`, non-integer `number`→fixed(6),
    `null`/`undefined`→null.
- **Rationale**: This is the fiddly part the interop decision hinges on; reusing the
  proven formatting avoids subtle correctness bugs and feeds checkers consistent strings.

## D5 — Result-equality checker (settled)

- **Decision**: Run learner SQL and reference SQL; canonicalize each as
  `{ cols: cols.map(lowercase), rows: rows.map(stringify) }`; sort the row list **unless**
  the lesson marks the task order-sensitive; compare for equality. Optional SQL-shape
  check applies only where a lesson declares one.
- **Rationale**: Lets a join and an equivalent subquery both pass while catching wrong
  results (spec FR-011, FR-012). Order-sensitivity is a per-task flag.

## D6 — Seed dataset (settled)

- **Decision**: An ordered array of DDL/DML strings executed sequentially on connect;
  a reset path re-runs them. Domain: a small trading book — `traders`, `instruments`,
  `trades` — with deliberate gaps and NULLs (a trader with no trades, an instrument never
  traded, trades with NULL price). Relationships are conceptual (no FK constraints).
- **Rationale**: Gives join/NULL/anti-join lessons real teeth (spec FR-020); orphan rows
  are what make anti-join lessons meaningful.
- **Dialect notes for content/checkers**: `ANTI JOIN`/`SEMI JOIN` are DuckDB-specific (vs
  standard `NOT EXISTS`/`EXISTS`); `[ ... ]` is a LIST literal; DuckDB supports FROM-first,
  `QUALIFY`, and `FETCH FIRST n ROWS ONLY`.

## D7 — Grading scale

- **Decision**: **Points only** — a numeric score from the per-lesson rubric
  (correctness, attempts, hint usage, optional time-box); the rubric defines the passing
  threshold. No IG/G/VG letters in v1.
- **Rationale**: Clarified choice; granular points drive the reflection breakdown.
- **Alternatives considered**: IG/G/VG-only (too coarse); points→IG/G/VG mapping
  (recommended but not selected).

## D8 — UI & content language

- **Decision**: **Bilingual Swedish + English from the start**, learner-switchable at any
  time without losing progress (spec FR-027, SC-011). Phase names (VISA/INSTRUERA/ÖVA/
  PRÖVA) and SQL keywords stay untranslated. Each lesson definition supplies prose in both
  languages.
- **Rationale**: Clarified choice; an `I18n` keyed-string layer in `app`, and a
  `LocalizedText` type in the content DSL.
- **Alternatives considered**: Swedish-only (recommended, tighter scope); English-only.

## D9 — VISA/INSTRUERA realization

- **Decision**: A **declarative, timed transcript** authored as lesson data and replayed
  by the console; INSTRUERA replays the same transcript slowly with stepwise annotations.
  The lesson's reference solution is still executed **live** for checking, so the
  demonstrated target cannot silently diverge from real engine behavior.
- **Rationale**: Clarified choice; deterministic timing + precise annotation control,
  consistent with content-as-data.
- **Alternatives considered**: Scripted live execution (less deterministic timing);
  hybrid (more to build).

## D10 — Lesson content format

- **Decision**: **In-bundle typed Scala lesson-definition objects** (a content DSL) in the
  `content` module, compiled into the bundle. A malformed lesson is a compile error.
- **Rationale**: Clarified choice; type-safe authoring and zero-engine-change content
  growth (Principle V, FR-018/FR-019). The DSL keeps lessons declarative.
- **Alternatives considered**: External YAML/JSON data files (recommended, friendlier to
  non-Scala authors); markdown-with-frontmatter (awkward for structured drills/rubrics).

## D11 — Persistence & portability

- **Decision**: Progress in `localStorage` (OPFS considered if blobs grow); export/import
  as a single JSON file with a schema version; invalid imports rejected without mutating
  existing state (spec FR-021–FR-023). No backend, no cookies.
- **Rationale**: Constitution Principle IV; the only cross-device path is manual
  export/import.

## D12 — Build & deploy pipeline (settled recipe + the JVM gap)

- **Decision**: sbt (Scala.js plugin) integrated into Vite via
  `@scala-js/vite-plugin-scalajs`; `npm run build` produces static `dist/`. Vite
  `base: "./"`, `build.target: "es2022"`. GitHub Actions deploys to Pages.
- **Reused Pages recipe (verified)**: `permissions: contents:read, pages:write,
  id-token:write`; `concurrency: { group: pages, cancel-in-progress: false }`; build job
  (`actions/checkout@v6` → `actions/setup-node@v6` node 22 `cache: npm` → `npm ci` →
  `npm run build` → `actions/configure-pages@v6` → `actions/upload-pages-artifact@v5`
  path `dist`) + deploy job (`actions/deploy-pages@v5`, `environment: github-pages`). Add
  `workflow_dispatch`.
- **THE GAP (net-new vs reference)**: the reference workflow is Node-only. This project
  also needs a JVM build, so CI MUST add JDK + sbt + Coursier alongside `setup-node`, with
  **both** toolchains cached — `actions/setup-java` (Temurin) + `sbt/setup-sbt` (or
  `coursier/setup-action`) with sbt/Coursier/ivy caches, plus `cache: npm`. Sequence:
  Scala.js compile (sbt) → Vite build (npm) → upload `dist/`.
- **One-time Pages enablement gotcha**: `configure-pages` fails with "Pages site Not
  Found" if Pages isn't enabled first. Enable via Settings → Pages → Source: GitHub
  Actions, or `gh api repos/<owner>/<repo>/pages -X POST -f build_type=workflow`, before
  the first deploy. Tracked as an explicit one-time setup step.

## D13 — Supply-chain hygiene (settled)

- **Decision**: Dependabot for `npm` + `github-actions` (weekly); a TruffleHog secret-scan
  workflow (`--results=verified,unknown`, checkout `fetch-depth: 0`); `.gitignore` covering
  `node_modules/ dist/` plus Scala.js artifacts (`target/ .bsp/ .bloop/ .metals/`) — the
  existing `.gitignore` already covers these. Optional `FUNDING.yml`.

## D14 — Optional privacy analytics

- **Decision**: GoatCounter pattern is **OPTIONAL** and deferred unless it can guarantee:
  cookieless, no stored IP, a no-op under DNT/GPC, never transmits learner SQL, emitting
  only coarse virtual pageviews/event names (spec FR-025). If those cannot be met in v1,
  omit analytics entirely.
- **Rationale**: Privacy-first; not on the critical path.

## D15 — Diagram hygiene

- **Decision**: Any Mermaid diagrams in docs (e.g. ARCHITECTURE.md) MUST be validated
  locally with `mmdc` before commit — GitHub's renderer rejects reserved-word node IDs
  (`style`, `class`, `end`), semicolons in `sequenceDiagram` notes, and bare dotted IDs.

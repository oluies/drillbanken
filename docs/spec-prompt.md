# Project: "Drillbänken" — a gamified console tutor built on the Försvarsmakten instruction loop

You are running inside Claude Code with GitHub Spec Kit. Your job in this session is SPECIFICATION ONLY: constitution, spec, clarifications, plan, and tasks. Do NOT implement. Stop after the tasks + analyze steps and hand me the artifacts for review.

> Spec Kit installs its steps as **skills** named `/speckit-constitution`, `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-analyze` (hyphenated — not the older dotted `/speckit.*`). Run them from a Claude Code session rooted in this repo so the skills are loaded.

## Step 1 — /speckit-constitution

Create the constitution from these non-negotiable principles:

1. **Pedagogy is the architecture.** Every lesson is an instance of the Swedish Armed Forces instruction loop from Handbok Utbildningsmetodik (Försvarsmakten, 2013/2024): VISA -> INSTRUERA -> ÖVA (parts) -> ÖVA (whole) -> PRÖVA. This is a typed state machine, not a content convention. Phase semantics:
   - VISA: the system demonstrates the skill at full speed, no commentary — a replayed console session showing the target end state ("målbild").
   - INSTRUERA: the same demonstration replayed slowly, stepwise, annotated with keyword-level instruction. Short, imperative, no over-explaining.
   - ÖVA (parts): isolated drills of each sub-step in the console, with immediate pass/retry feedback. Repetition until fluent is a feature, not a failure path.
   - ÖVA (whole): the full task end to end, self-paced, optional hints with a score cost.
   - PRÖVA: graded examination, no hints, against the published end requirement ("slutkrav"). The learner may not enter PRÖVA until the ÖVA gates are met.
   The loop maps onto Kolb's experiential learning cycle (VISA/INSTRUERA = abstract conceptualization via observation, ÖVA = active experimentation, PRÖVA = concrete experience, post-grade feedback = reflective observation). The spec must state this mapping and the reflection step must exist: after PRÖVA the learner sees what was graded, why, and what to drill again.

2. **The console is the medium.** All learner interaction happens in a terminal-style interface (xterm.js or equivalent). No forms, no multiple choice. Research note you should treat as settled: no off-the-shelf gamified console-tutorial framework exists. Katacoda is dead; StackBlitz TutorialKit provides a tutorial runner but is Node/WebContainer-centric with no grading or drill loop; swirl (R) is prior art for the interaction model only. The drill engine is therefore first-party code.

3. **Typed core, Scala throughout.** The frontend is pure Scala 3 compiled with Scala.js, rendered with Laminar (Airstream signals for state). No Vue, no React. The lesson state machine, grading rules and progression model are pure, framework-free domain code in their own module with property-based tests (munit + ScalaCheck); the Laminar UI is a thin adapter over it. JavaScript interop (xterm.js, DuckDB-WASM) is confined to a narrow, explicitly typed facade layer.

4. **Static deployment.** The whole thing runs client-side and deploys to GitHub Pages. No backend. Progress persists locally (localStorage or OPFS), exportable as a file.

5. **Content as data.** Lessons are declarative artifacts (one file per lesson: demonstration script, keyword instructions, part-drills with checkers, whole-task, exam with rubric). Adding a lesson requires no engine changes.

## Step 2 — /speckit-specify

Specify the first product slice with this scope:

**Subject domain for v1: SQL, executed by DuckDB-WASM in the browser.** I have an existing, deployed DuckDB-WASM trainer, **"SQL Concepts Lab"** (repo `oluies/sql-concepts-lab`, live at oluies.github.io/sql-concepts-lab), whose engine bootstrap, Arrow result-materialization, seed-dataset shape and result-equality checker are PROVEN in production and are documented as settled facts in the *Reference* appendix below — cite them, do not re-derive them. The console replaces that project's editor-and-button UI; the engine/checker/seed core carries over. The console accepts SQL statements plus a small set of meta-commands (help, hint, progress, repeat-demo, abort). Checkers compare the learner's result sets, and where relevant their SQL shape, against reference solutions — order-insensitive unless the task demands ordering (the existing project's canonicalization rule, in the appendix, is the proven approach).

**Gamification, restrained and aligned with the pedagogy:**
- Grading on PRÖVA: a rubric producing a grade (e.g. IG / G / VG or points) from correctness, number of attempts, hint usage and time-boxing where the lesson defines one. Rubric is per-lesson data.
- Progression: lessons unlock in sequence; PRÖVA gates advancement. Drill repetitions in ÖVA never reduce score — repetition is how the method works.
- Streaks/insignia tied to completed PRÖVA only. No leaderboards in v1 (no backend).
- Every grade ends with the Kolb reflection screen: rubric breakdown plus a generated "drill again" list of the specific part-drills that cost points.

**User stories to cover at minimum:** learner runs a full lesson loop end to end; learner fails PRÖVA and is routed back to targeted ÖVA drills; learner replays VISA mid-drill; learner resumes a half-finished lesson after closing the browser; author adds a new lesson as a data file; learner exports/imports progress.

**Out of scope for v1:** accounts, server sync, multi-subject content (architecture must allow other engines later — e.g. a shell or kubectl simulator — but only SQL ships), AI-generated feedback. Privacy-first, cookieless usage analytics (the GoatCounter pattern in the appendix) is OPTIONAL and, if included, must be a no-op under DNT/GPC and never transmit learner SQL — otherwise defer it.

## Step 3 — /speckit-clarify

Run clarify. The frontend strategy is DECIDED: pure Scala.js + Laminar (the abandoned Vue bindings and the hybrid split were considered and rejected; record this in the spec as a resolved decision with that rationale). Among whatever you surface, explicitly put these open decisions to me — do not decide them silently:

1. **JS interop approach** for xterm.js and @duckdb/duckdb-wasm: hand-written minimal `js.native` facades covering only the used surface (recommend this and say why) vs ScalablyTyped-generated full facades vs a thin TypeScript shim module exposing a narrow API (e.g. `exec(sql): Promise<Result>`, terminal open/write/onData) that Scala.js consumes. **Informed input you must weigh (from the existing project, see appendix):** DuckDB-WASM returns Apache Arrow tables, and turning them into plain `{cols, rows}` requires non-trivial JS-side materialization — read column names off `result.schema.fields`, call `.toArray()` then `.toJSON()` per row, and hand-format `bigint`→string, `Date`→ISO, and non-integer numbers→fixed precision. That materialization is awkward to express through a `js.native` Arrow facade in Scala.js, which is a real argument for the TS-shim option returning already-flattened rows. Recommend an option but surface this trade-off explicitly rather than burying it.
2. **Lesson content format**: YAML/JSON vs a Scala DSL compiled into the bundle vs markdown-with-frontmatter à la TutorialKit. (Note: the existing project keeps lessons as plain typed objects in a single module — "content as data" worked well there, but that project compiles content into the bundle; weigh that against author ergonomics for non-Scala lesson authors.)
3. **Grading scale**: Swedish IG/G/VG vs points vs both.
4. **VISA implementation**: pre-recorded console transcript replayed with timing (asciinema-style data) vs scripted live execution against the real engine. State which you recommend and why. (The existing engine boots in ~hundreds of ms and runs every query live with sub-ms-to-low-ms latency, so live scripted execution is technically viable; weigh determinism/timing-control of a recorded transcript against it.)
5. **Language of the UI and content**: Swedish, English, or both from the start.

## Step 4 — /speckit-plan, /speckit-tasks, /speckit-analyze

After my clarification answers: produce the plan. The plan's first milestone MUST be an interop spike that gates everything else: a minimal Scala.js + Laminar app where DuckDB-WASM boots and reports its version into a `Signal[EngineStatus]` (Loading | Ready(version) | Failed(msg)), one query renders as a Laminar-bound results table, and one deliberate SQL error surfaces as a typed error value — proving the sbt + vite-plugin-scalajs + `?url` asset + worker pipeline end to end before any feature work is planned in detail. The spike's code becomes the project's `duckdb` module (Laminar-free service core + thin Laminar components).

**Spike acceptance, grounded in the proven sql-concepts-lab bootstrap (appendix):** the service core must (a) select the MVP/EH bundle via `duckdb.selectBundle`, (b) import the `.wasm` and the matching worker `.js` as Vite `?url` assets so nothing is fetched from a CDN at runtime, (c) instantiate `AsyncDuckDB` with a `VoidLogger`, connect, run the seed, and read `PRAGMA version`, and (d) expose `exec(sql): Promise[QueryResult]` where `QueryResult = { cols: Seq[String], rows: Seq[Seq[String | Null]] }` already materialized off Arrow (the EngineStatus version string is the `PRAGMA version` value). The deliberate-error case must map a thrown engine error to the typed `Failed`/error value, not a console exception.

**Build pipeline constraints for the plan:** Scala 3 + sbt with the Scala.js plugin, integrated into Vite via the official @scala-js/vite-plugin-scalajs; Laminar (com.raquo::laminar) for UI; domain module cross-compiled or JS-only but free of Laminar and DOM imports; munit + ScalaCheck tests for the domain module runnable in CI; `npm run build` produces a static `dist/`; GitHub Actions deploy to Pages.

**Concrete, verified deploy/build facts to reuse from sql-concepts-lab — and the one gap to close (appendix has full detail):**
- Reuse the exact working Pages setup: `permissions: contents:read, pages:write, id-token:write`; `concurrency: { group: pages, cancel-in-progress: false }`; build job (`actions/checkout@v6` → `actions/setup-node@v6` node 22 `cache: npm` → `npm ci` → `npm run build` → `actions/configure-pages@v6` → `actions/upload-pages-artifact@v5` path `dist`) + separate deploy job (`actions/deploy-pages@v5`, `environment: github-pages`). Add `workflow_dispatch`.
- **THE GAP:** sql-concepts-lab's workflow is Node-only. Drillbänken needs a JVM build too, so the plan's CI/deploy MUST add a JDK + sbt + Coursier toolchain alongside `setup-node`, with BOTH toolchains cached — e.g. `actions/setup-java` (Temurin) + `sbt/setup-sbt` (or `coursier/setup-action`) with sbt/Coursier/ivy caches, plus the existing `cache: npm`. Sequence: Scala.js compile (sbt) → Vite build (npm) → upload `dist/`. Plan this explicitly; it is net-new versus the reference repo.
- Vite config: `base: "./"` (works at the project-pages subpath and at a custom-domain root) and `build.target: "es2022"`. Carry these over.
- **One-time Pages-enablement gotcha (cost us a failed first deploy):** the deploy workflow's `configure-pages` step fails with "Pages site Not Found" if Pages isn't enabled FIRST. Enable it before the first run via Settings → Pages → Source: GitHub Actions, or `gh api repos/<owner>/<repo>/pages -X POST -f build_type=workflow`. The plan/tasks must list this as an explicit one-time setup step, not an afterthought.
- Supply-chain/hygiene to carry over (all verified working): Dependabot for `npm` + `github-actions` (weekly); a TruffleHog secret-scan workflow (`--results=verified,unknown`, checkout `fetch-depth: 0`); a `.gitignore` covering `node_modules/ dist/`, plus Scala.js artifacts (`target/ .bsp/ .bloop/ .metals/`); optional `FUNDING.yml`.
- Any Mermaid diagrams in the spec's docs (ARCHITECTURE.md etc.) must be validated locally with `mmdc` before committing — GitHub's renderer rejects reserved-word node IDs (`style`, `class`, `end`), semicolons in `sequenceDiagram` note text, and bare dotted IDs.

Then tasks, then run analyze and report inconsistencies. Then STOP. Print a summary of: branch name, spec path, the decisions taken, and the decisions still open.

## Reference — settled facts from sql-concepts-lab (cite, do not re-derive)

These are verified in the deployed sibling project. Treat as authoritative context for the spec and plan; do not spend research budget reconfirming them.

**Engine + versions:** `@duckdb/duckdb-wasm ^1.33.1-dev45.0` (DuckDB engine reported by `PRAGMA version`, ~v1.5.x). Toolchain in the reference repo: Vite 8, TypeScript 6 (Drillbänken replaces TS with Scala 3 / Scala.js; Vite + Pages story is unchanged).

**Bootstrap (TypeScript today; becomes the Scala.js `duckdb` service core):**
```
selectBundle({ mvp:{mainModule, mainWorker}, eh:{mainModule, mainWorker} })  // ?url imports
new Worker(bundle.mainWorker) → new AsyncDuckDB(new VoidLogger(), worker)
await db.instantiate(bundle.mainModule, bundle.pthreadWorker)
conn = await db.connect();  run seed;  PRAGMA version → version string
```
No runtime CDN dependency: the `.wasm` and worker `.js` are bundled by Vite via `?url` imports.

**Arrow result materialization (the fiddly part — informs the interop decision):** `result.schema.fields.map(f => f.name)` for columns; `result.toArray().map(r => r.toJSON())` for rows; then format per cell — `bigint`→`toString()`, `Date`→ISO `"YYYY-MM-DD HH:MM:SS"`, non-integer `number`→fixed(6), `null`/`undefined`→null.

**Result-equality checker (proven approach for part-drill + PRÖVA checkers):** run learner SQL and reference SQL; canonicalize each result as `{ cols: cols.map(lowercase), rows: rows.map(JSON.stringify) }`; sort the row list UNLESS the lesson marks the task order-sensitive; compare for equality. This lets a join and an equivalent subquery both pass while still catching wrong results.

**Seed dataset shape:** an ordered array of DDL/DML strings (`CREATE OR REPLACE TABLE …`, `INSERT …`) executed sequentially on connect; a "reset data" path re-runs them. Sample domain is a small trading book (traders / instruments / trades) with deliberate gaps and NULLs (a trader with no trades, an instrument never traded, trades with NULL price) so join/NULL/anti-join lessons have real teeth — relationships are conceptual (no FK constraints), which is what makes orphan-row lessons meaningful.

**DuckDB SQL dialect specifics (relevant to lesson content + checker design):** `ANTI JOIN` / `SEMI JOIN` are DuckDB-specific, NOT standard SQL (standard equivalents are `NOT EXISTS` / `EXISTS`); `[ ... ]` is a LIST literal, not a quoted identifier; DuckDB supports FROM-first (`FROM t SELECT …`), `QUALIFY`, and `FETCH FIRST n ROWS ONLY` alongside `LIMIT`.

**Privacy analytics pattern (optional, see scope):** GoatCounter, cookieless, no stored IP; SPA integration injects `count.js` with `no_onload` and emits virtual pageviews + coarse event names itself; a no-op when Do Not Track / Global Privacy Control is set; never transmits query text.

**Responsive/mobile lessons (if the console needs a non-xterm chrome around it):** use CSS grid `minmax(0,1fr)` columns to prevent text overflow, `100dvh` for viewport height, and 16px input font on mobile to stop iOS Safari from auto-zooming on focus.

## Rules

- Specification only. No application code, no npm/sbt scaffolding beyond what specify init itself creates.
- Where research above is stated as settled, cite it in the spec as context rather than re-researching. This includes the *Reference* appendix's engine/bootstrap/checker/deploy facts.
- Ask rather than assume on anything touching the pedagogy model — the loop's fidelity to the Handbok Utbildningsmetodik method matters more to me than feature count.

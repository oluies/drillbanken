# Implementation Plan: Console SQL Tutor (Drillbänken v1)

**Branch**: `002-console-sql-tutor` | **Date**: 2026-06-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-console-sql-tutor/spec.md`

## Summary

Drillbänken v1 is a gamified, console-only SQL tutor that runs entirely in the browser
and is built on the Försvarsmakten instruction loop **VISA → INSTRUERA → ÖVA(parts) →
ÖVA(whole) → PRÖVA**, expressed as a typed state machine and mapped onto Kolb's cycle.
The learner interacts only through an xterm.js terminal; SQL executes client-side in
DuckDB-WASM; PRÖVA produces a numeric points grade from a per-lesson rubric and ends with
a reflection screen that generates a targeted "drill again" list. Lessons are typed
in-bundle Scala objects (a content DSL); progress persists locally and is exportable.

**Technical approach**: A pure Scala 3 + Scala.js application rendered with Laminar.
A framework-free **domain** module holds the loop state machine, checker, grading, and
progression (tested with munit + ScalaCheck). JS interop with DuckDB-WASM and xterm.js is
generated with **ScalablyTyped** and wrapped behind narrow Scala service APIs
(`EngineService`, `ConsoleService`). The build is sbt (Scala.js plugin) integrated into
Vite via `@scala-js/vite-plugin-scalajs`, producing a static `dist/` deployed to GitHub
Pages by GitHub Actions. **Milestone 0 is a gating interop spike** proving the
sbt + Vite + `?url` asset + worker pipeline boots DuckDB-WASM end to end before any
feature work.

## Technical Context

**Language/Version**: Scala 3 (latest stable 3.x) compiled to JavaScript with Scala.js
(latest stable 1.x). No application TypeScript is hand-written; TypeScript type
definitions are consumed only as input to ScalablyTyped facade generation.

**Primary Dependencies**:
- UI: Laminar (`com.raquo::laminar`) + Airstream signals
- SQL engine: `@duckdb/duckdb-wasm ^1.33.1-dev45.0` (DuckDB ~v1.5.x via `PRAGMA version`)
- Console: xterm.js (`@xterm/xterm`)
- Interop: ScalablyTyped converter (sbt plugin) generating Scala facades for the above
- Build: sbt + Scala.js plugin, `@scala-js/vite-plugin-scalajs`, Vite 7 (the plugin
  supports `vite 4.1.4 - 7`; esbuild pinned to the patched 0.28.1 via npm `overrides`)
- Tests: munit + ScalaCheck (domain module)

**Storage**: Browser-local only — `localStorage` for progress (with OPFS considered for
larger blobs); export/import as a downloadable/uploadable JSON file. No server, no
cookies.

**Testing**: munit + ScalaCheck for the pure domain module (state machine invariants,
checker order-sensitivity, grading/rubric, progression gating), runnable headlessly in
CI. Interop and UI verified through the quickstart e2e recipe (boot, query, error).

**Target Platform**: Modern evergreen desktop browser with WebAssembly + Web Worker
support. Static site served from GitHub Pages (project-pages subpath or custom domain).

**Project Type**: Static, client-side single-page web application (no backend).

**Performance Goals**: Engine boot in the low hundreds of milliseconds; query execution
sub-millisecond to low-millisecond for seed-scale data (carried from sql-concepts-lab).
Console input echo and transcript replay must feel immediate (no perceptible lag).

**Constraints**: Fully offline-capable after first load — the `.wasm` and worker `.js`
are bundled via Vite `?url` imports, **never** fetched from a CDN at runtime. Vite
`base: "./"` and `build.target: "es2022"`. The domain module MUST NOT import Laminar or
the DOM.

**Scale/Scope**: Single learner per device; a small seeded SQL curriculum (enough to
exercise joins, NULL handling, anti-joins on the trading-book seed). Architecture keeps
loop/checker/grading/progression subject-independent so other engines can be added later.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | How the plan complies | Status |
|---|-----------|-----------------------|--------|
| I | Pedagogy Is the Architecture | The five phases, gates (no PRÖVA before ÖVA gates met), and the post-PRÖVA reflection are encoded as a typed state machine in the `domain` module; Kolb mapping documented; reflection step is a first-class transition. | PASS |
| II | The Console Is the Medium | All interaction is through the xterm.js console; SQL + meta-commands only; no forms or multiple-choice. Drill engine is first-party. | PASS |
| III | Typed Core, Scala Throughout | Scala 3 + Scala.js + Laminar; pure framework-free `domain` module with ScalaCheck; interop confined behind narrow `EngineService`/`ConsoleService` APIs. | PASS (see note) |
| IV | Static, Client-Side Deployment | No backend; DuckDB-WASM in-browser; GitHub Pages; progress in localStorage/OPFS, exportable. | PASS |
| V | Content as Data | Lessons are declarative typed lesson-definition objects (content DSL); adding one needs no engine change (only a content object + rebuild); malformed content is a compile error. | PASS |

**Note on Principle III (interop boundary):** The clarified decision uses
**ScalablyTyped-generated facades**, which are broad rather than minimal. To honor the
constitution's "narrow, explicitly typed facade" intent, the generated facades are an
internal detail of the `engine` and `console` modules only; the rest of the app depends
solely on the narrow hand-authored service APIs (`EngineService.exec`,
`ConsoleService.open/write/onData`). This is recorded in Complexity Tracking.

**Gate result: PASS.** No unjustified violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/002-console-sql-tutor/
├── plan.md              # This file
├── research.md          # Phase 0 output — consolidated decisions
├── data-model.md        # Phase 1 output — entities & state machine
├── quickstart.md        # Phase 1 output — setup, spike, build, deploy, e2e recipe
├── contracts/           # Phase 1 output — module/interface contracts
│   ├── engine-service.md
│   ├── console-service.md
│   ├── domain-api.md
│   ├── lesson-dsl.md
│   └── persistence.md
├── checklists/
│   └── requirements.md  # from /speckit-specify
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

A single sbt build with Scala.js subprojects, integrated into one Vite app. The `domain`
module is Laminar/DOM-free; `engine` and `console` own the ScalablyTyped facades behind
narrow services; `content` holds the typed lesson DSL + seed; `app` wires Laminar UI,
persistence, i18n, and the entrypoint.

```text
build.sbt
project/
├── build.properties           # sbt version
└── plugins.sbt                # sbt-scalajs, scalajs-vite-plugin link, scalablytyped converter

package.json                   # vite, @duckdb/duckdb-wasm, @xterm/xterm,
                               #   @scala-js/vite-plugin-scalajs, type defs for ScalablyTyped
vite.config.ts                 # base "./", build.target es2022, ?url asset wiring
index.html                     # mounts the Scala.js app
tsconfig.json                  # only to satisfy type-def resolution for ScalablyTyped

modules/
├── domain/                    # PURE Scala — no Laminar, no DOM
│   ├── src/main/scala/drillbanken/domain/
│   │   ├── loop/              # Phase, LessonState, transitions, gates
│   │   ├── check/             # Checker, result canonicalization, order-sensitivity
│   │   ├── grade/             # Rubric, points scoring, Grade, ReflectionReport
│   │   └── progress/          # Progression/unlock rules, ProgressState (pure model)
│   └── src/test/scala/drillbanken/domain/   # munit + ScalaCheck
├── engine/                    # DuckDB-WASM service core (Laminar-free) + ScalablyTyped facade
│   └── src/main/scala/drillbanken/engine/   # EngineService, QueryResult, EngineStatus
├── console/                   # xterm.js console adapter (facade + Laminar component)
│   └── src/main/scala/drillbanken/console/  # ConsoleService, ConsoleComponent
├── content/                   # typed lesson-definition objects (content DSL) + seed dataset
│   └── src/main/scala/drillbanken/content/  # LessonDef DSL, Curriculum, SeedData
└── app/                       # Laminar UI + persistence + i18n + Main
    └── src/main/scala/drillbanken/app/      # Main, views, PersistenceService, I18n, store

.github/
├── workflows/
│   ├── deploy.yml             # JDK+sbt + Node toolchains → sbt compile → vite build → Pages
│   └── secret-scan.yml        # TruffleHog (--results=verified,unknown, fetch-depth:0)
└── dependabot.yml             # npm + github-actions, weekly
```

**Structure Decision**: A single sbt multi-module build keeps the pure `domain` core
physically separate from any DOM/Laminar/interop code (enforcing Principle III), while
the `app` module is the only Scala.js *application* (the others are libraries it depends
on). Vite consumes the linked Scala.js output via `@scala-js/vite-plugin-scalajs`. This is
the smallest structure that satisfies the module-boundary constitution rules; it is not
"multiple projects" in the deployment sense — it is one static site.

## Complexity Tracking

| Decision | Why Needed | Simpler Alternative Rejected Because |
|----------|------------|-------------------------------------|
| ScalablyTyped facades (broad) wrapped behind narrow services | Clarified decision (2026-06-14); avoids hand-maintaining facades for two evolving libraries | Hand-written `js.native` facades were the minimal option but the Arrow→{cols,rows} materialization is awkward in Scala.js; ScalablyTyped + a narrow service wrapper preserves the narrow-boundary intent |
| Multi-module sbt build (5 modules) | Physically enforces the Laminar/DOM-free domain core (Principle III) and the content-as-data boundary (Principle V) | A single flat module cannot prevent the domain core from importing Laminar/DOM; the boundary would be convention-only |
| Dual toolchain in CI (JVM/sbt + Node) | sbt must compile Scala.js before Vite bundles; reference repo was Node-only | Node-only CI (the sql-concepts-lab workflow) cannot build a Scala.js project — this is net-new and unavoidable |

## Phase 0 — Research

See [research.md](./research.md). All spec clarifications are resolved; Phase 0
consolidates the decided approach plus the settled facts carried from `sql-concepts-lab`
(engine bootstrap, Arrow materialization, result-equality checker, seed dataset) and the
verified GitHub Pages build/deploy recipe, including the dual JVM+Node toolchain gap and
the one-time Pages-enablement step. No `NEEDS CLARIFICATION` markers remain.

## Phase 1 — Design & Contracts

Artifacts generated:
- [data-model.md](./data-model.md) — entities, the loop state machine and transitions,
  the rubric/grade model, and the progress model with validation rules from the spec.
- [contracts/](./contracts/) — narrow typed interfaces: `EngineService`,
  `ConsoleService`, the domain API (state machine + checker + grading + progression),
  the lesson content DSL, and persistence (progress + export/import).
- [quickstart.md](./quickstart.md) — environment setup, the gating interop-spike recipe
  and its acceptance, the build pipeline, the one-time Pages enablement, and the e2e
  verification recipe.

**Milestone 0 (GATING) — Interop spike.** Before any feature work: a minimal Scala.js +
Laminar app where DuckDB-WASM boots and reports its version into a
`Signal[EngineStatus]` (Loading | Ready(version) | Failed(msg)); one query renders as a
Laminar-bound results table; one deliberate SQL error surfaces as a typed `Failed`/error
value. Acceptance is detailed in quickstart.md. The spike's code becomes the `engine`
module (Laminar-free service core + thin Laminar components).

## Post-Design Constitution Re-Check

Re-evaluated after Phase 1 design: the data model encodes the five-phase loop and gates
as types (I); all interaction stays in the console contract (II); the domain module's
contracts contain no Laminar/DOM/interop types, and interop stays behind the narrow
services (III); persistence and deployment contracts are backendless and local (IV); the
lesson DSL is the sole content surface and requires no engine change (V).
**Result: PASS — no new violations introduced by the design.**

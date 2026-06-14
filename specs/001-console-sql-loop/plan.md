# Implementation Plan: Console SQL Tutor — the Instruction Loop (v1)

**Branch**: `001-console-sql-loop` | **Date**: 2026-06-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-console-sql-loop/spec.md`

## Summary

Deliver a static, client-side SQL tutor that runs the Försvarsmakten instruction loop
(VISA → INSTRUERA → ÖVA parts → ÖVA whole → PRÖVA) in a browser terminal. The pedagogy is
encoded as a typed state machine in a pure Scala 3 domain module (munit + ScalaCheck);
Laminar provides a thin reactive UI; DuckDB-WASM and xterm.js are reached through a single
narrow TypeScript shim (`exec(sql)`, terminal `open/write/onData`) so Apache Arrow
materialization stays on the JS side. Lessons are structured data files validated at load;
grading is points-internally / IG-G-VG-displayed; VISA is a pre-recorded timed transcript;
the UI and content are bilingual (Swedish default + English) on a first-class i18n
mechanism. The whole thing builds via sbt (Scala.js) + Vite into a static `dist/` and
deploys to GitHub Pages.

**Milestone 0 (gating): the interop spike** must prove the sbt + `@scala-js/vite-plugin-scalajs`
+ `?url` asset + worker pipeline end to end before any feature work proceeds (see Phases).

## Technical Context

**Language/Version**: Scala 3 (latest stable 3.x) compiled with Scala.js; a thin
TypeScript layer for the interop shim.

**Primary Dependencies**: Laminar (`com.raquo::laminar`) + Airstream for UI/state;
`@duckdb/duckdb-wasm` (`^1.33.1-dev45.0`, DuckDB engine ~v1.5.x) as the SQL engine;
`xterm` (xterm.js) for the console; Vite + `@scala-js/vite-plugin-scalajs` for the build.
Testing: munit + ScalaCheck.

**Storage**: Browser-local — learner progress and locale preference in `localStorage`
(OPFS considered, deferred); export/import to a single JSON file. No backend, no cookies.

**Testing**: munit + ScalaCheck for the pure domain module, runnable headless in CI on the
JVM and/or Node; the domain module carries no DOM/Laminar dependency so most behavior is
testable without a browser.

**Target Platform**: Evergreen desktop browsers (WASM + Web Workers required). Static site
served from GitHub Pages at a project-pages subpath.

**Project Type**: Single static web application (multi-module Scala build + thin TS shim).

**Performance Goals**: Engine boot in well under ~1 s on a warm load; query feedback in the
console perceived as instant (low-ms) for the seed-sized dataset; locale switch re-renders
without a reload.

**Constraints**: Fully client-side; no backend; no cookies; honor Do-Not-Track / Global
Privacy Control if optional analytics is ever added; engine `.wasm`/workers bundled via
Vite `?url` (no runtime CDN); `base: "./"` and `build.target: "es2022"` in Vite config.

**Scale/Scope**: v1 ships the SQL subject with a small lesson set; architecture must admit
additional subject engines later without engine rewrites.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Plan compliance |
|-----------|-----------------|
| I. Pedagogy is the Architecture | The loop is a typed `Phase` state machine in the domain module with gating (`ÖVA` gates before `PRÖVA`) and a mandatory post-PRÖVA reflection. Kolb mapping documented in spec/research. ✅ |
| II. The Console is the Medium | All answers entered in xterm.js; meta-commands (help/hint/progress/repeat-demo/abort) parsed by the domain; no forms/multiple-choice. ✅ |
| III. Typed Core, Scala Throughout | Pure Laminar-free, DOM-free `domain` module with ScalaCheck property tests; Laminar UI is a thin adapter; all JS interop confined to one typed shim facade. ✅ |
| IV. Static, Client-Side Deployment | Vite static `dist/` to GitHub Pages; progress in localStorage; export/import file. No backend. ✅ |
| V. Content as Data | Lessons are validated structured-data artifacts; adding a lesson is a data edit (FR-026/027/028). ✅ |

**Result**: PASS (initial). No violations → Complexity Tracking left empty. Re-evaluated
after Phase 1 design: still PASS (the TS shim is the only untyped surface and is bounded by
a contract; see `contracts/`).

## Project Structure

### Documentation (this feature)

```text
specs/001-console-sql-loop/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (interop + data-format contracts)
│   ├── interop-shim.md
│   ├── lesson-schema.md
│   └── progress-schema.md
├── checklists/
│   └── requirements.md  # spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

A single static web app built from a multi-module sbt (Scala.js) project plus a thin
TypeScript interop layer and a Vite host.

```text
build.sbt                      # sbt multi-module build (Scala.js)
project/                       # sbt plugins (scalajs, vite-plugin bridge)
package.json                   # Vite + @scala-js/vite-plugin-scalajs + xterm + duckdb-wasm
vite.config.ts                 # base "./", target es2022, ?url assets
index.html                     # mounts the Laminar app + the terminal element

modules/
├── domain/                    # PURE: no Laminar, no DOM
│   └── src/
│       ├── main/scala/        # Phase state machine, gating, grading, progression,
│       │                      #   checker model, lesson/progress types, i18n catalog types
│       └── test/scala/        # munit + ScalaCheck property tests
├── duckdb/                    # SQL engine service core (Laminar-free) + thin Laminar views
│   └── src/main/scala/        # EngineStatus signal, exec → QueryResult, error mapping
├── terminal/                  # xterm.js facade + console I/O adapter (thin)
│   └── src/main/scala/
├── i18n/                      # Locale, message catalog, Intl formatting facade
│   └── src/main/scala/
└── app/                       # Laminar UI: wires domain ⇄ terminal ⇄ duckdb ⇄ i18n
    └── src/main/scala/

ts/                            # thin TypeScript interop shim (the only untyped surface)
└── shim.ts                    # exec(sql): Promise<Result>; terminal open/write/onData

content/                       # lesson data artifacts (validated at load)
└── lessons/*.json             # one self-contained lesson per file (localized fields)

.github/workflows/             # deploy.yml (Pages), ci.yml, secret-scan.yml
.github/dependabot.yml
```

**Structure Decision**: Multi-module Scala build with a strict dependency rule — `domain`
depends on nothing project-internal; `duckdb`, `terminal`, `i18n` depend on `domain`
(types) only; `app` depends on all. The TS shim is the sole place untyped JS lives. This
directly encodes Constitution III (typed core, isolated interop). Lessons live in
`content/` as data per Constitution V.

## Phases

### Phase 0 — Outline & Research → `research.md`

Resolves the interop/build/i18n unknowns and records decisions (TS-shim interop, structured
lesson format, points→IG/G/VG, recorded VISA transcript, bilingual i18n approach, dual
sbt+Node CI toolchain, Pages-enablement-first). No NEEDS CLARIFICATION remain after Phase 0.

### Milestone 0 — Interop spike (GATING; planned in detail only after this passes)

A minimal Scala.js + Laminar app where:
1. DuckDB-WASM boots and reports its version into `Signal[EngineStatus]`
   (`Loading | Ready(version) | Failed(msg)`), version sourced from `PRAGMA version`.
2. One query renders as a Laminar-bound results table.
3. One deliberate SQL error surfaces as a typed error value (not a thrown console
   exception).

Proves sbt + `@scala-js/vite-plugin-scalajs` + `?url` `.wasm`/worker assets end to end. The
spike's code becomes the `duckdb` module (Laminar-free service core + thin Laminar views).

### Phase 1 — Design & Contracts → `data-model.md`, `contracts/`, `quickstart.md`

Entities and state transitions in `data-model.md`; the interop shim, lesson schema, and
progress/export schema in `contracts/`; an end-to-end dev/run walkthrough in `quickstart.md`.
Agent context (`CLAUDE.md`) updated to point at this plan.

### Phase 2 — Tasks (separate `/speckit-tasks` step) → `tasks.md`

## Build & Deploy Pipeline

- **Build**: sbt compiles Scala.js per module; `@scala-js/vite-plugin-scalajs` feeds output
  into Vite; the TS shim compiles alongside; `npm run build` emits static `dist/`.
- **Vite**: `base: "./"`, `build.target: "es2022"`; DuckDB `.wasm` + worker and xterm assets
  bundled via `?url` (no runtime CDN).
- **CI (`ci.yml`)**: checkout → set up BOTH toolchains — `actions/setup-java` (Temurin) +
  sbt (with Coursier/ivy/sbt caches) AND `actions/setup-node@v6` (node 22, `cache: npm`) →
  run domain tests (sbt) → `npm ci` → `npm run build`.
- **Deploy (`deploy.yml`)**: reuse the proven sql-concepts-lab Pages setup —
  `permissions: contents:read, pages:write, id-token:write`; `concurrency: {group: pages,
  cancel-in-progress: false}`; build job (checkout@v6 → setup-java + sbt + setup-node@v6 →
  sbt build → npm ci → npm run build → configure-pages@v6 → upload-pages-artifact@v5 path
  `dist`) + deploy job (deploy-pages@v5, environment github-pages); add `workflow_dispatch`.
  **The net-new bit vs. sql-concepts-lab is the JVM/sbt toolchain alongside Node.**
- **One-time**: enable Pages BEFORE the first deploy (Settings → Pages → Source: GitHub
  Actions, or `gh api repos/oluies/drillbanken/pages -X POST -f build_type=workflow`), else
  `configure-pages` fails "Pages site Not Found".
- **Hygiene**: Dependabot (npm + github-actions, weekly); TruffleHog secret scan
  (`--results=verified,unknown`, fetch-depth 0); `.gitignore` already covers Node + Scala.js
  artifacts. Any Mermaid docs validated with `mmdc` before commit.

## Internationalization Approach (decision)

- **UI chrome**: a typed Scala message catalog in the `i18n` module — `enum Locale { Sv, En
  }` with `Sv` default; messages keyed by a sealed/`enum` key type so missing keys are
  compile errors; current locale held in an Airstream `Var[Locale]`/`Signal[Locale]` so
  Laminar re-renders reactively (FR-031/033).
- **Formatting**: browser-native `Intl` (NumberFormat/DateTimeFormat/PluralRules) via a tiny
  facade — standards-based, zero bundle weight (FR-034).
- **Lesson content**: each user-facing field carries per-locale variants in the JSON
  artifact; the loader selects the active locale and falls back to `Sv` when a translation
  is missing, never showing a raw key (FR-032).
- **Alternative considered**: a JS i18n library (i18next / `@formatjs/intl`) wrapped behind
  the TS shim for ICU MessageFormat. Rejected for v1 to keep the bundle lean and the core
  fully typed; revisit only if rich ICU pluralization across many locales is needed.

## Complexity Tracking

> No constitution violations to justify. Section intentionally empty.

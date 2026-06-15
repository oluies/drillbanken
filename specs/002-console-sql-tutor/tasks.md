---
description: "Task list for Console SQL Tutor (Drillbänken v1)"
---

# Tasks: Console SQL Tutor (Drillbänken v1)

**Input**: Design documents from `specs/002-console-sql-tutor/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Property-based domain tests (munit + ScalaCheck) are REQUIRED by the
constitution (Principle III) and the spec, so domain test tasks are included and written
before the domain implementation they cover. UI/interop is verified via the quickstart
e2e recipe rather than automated browser tests in v1.

**Organization**: Tasks are grouped by user story. **Phase 2 (Foundational) includes the
gating interop spike (Milestone 0) that must pass before any user-story work** — per the
project brief and plan.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1–US6 (user-story phases only)
- All paths are repository-relative and follow plan.md → Project Structure.

## Path Conventions

- Domain (pure): `modules/domain/src/main/scala/drillbanken/domain/...`,
  tests `modules/domain/src/test/scala/drillbanken/domain/...`
- Engine: `modules/engine/src/main/scala/drillbanken/engine/...`
- Console: `modules/console/src/main/scala/drillbanken/console/...`
- Content: `modules/content/src/main/scala/drillbanken/content/...`
- App: `modules/app/src/main/scala/drillbanken/app/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: sbt + Scala.js + Vite project skeleton.

- [X] T001 Create sbt multi-module build: `build.sbt` defining `domain`, `engine`, `console`, `content`, `app` modules (app aggregates; domain has no Laminar/DOM deps), plus `project/build.properties`
- [X] T002 Configure `project/plugins.sbt` with `sbt-scalajs` (Scala.js linker output for Vite). NOTE: ScalablyTyped converter was evaluated in T010 and rejected — see research.md D2 (compiler OOM); interop uses hand-written facades instead
- [X] T003 [P] Create `package.json` with Vite 7 (plugin supports `4.1.4 - 7`; esbuild forced to patched 0.28.1 via `overrides`), `@duckdb/duckdb-wasm ^1.33.1-dev45.0`, `@xterm/xterm`, `@scala-js/vite-plugin-scalajs`, and TS type-defs for ScalablyTyped; add `dev`/`build` scripts
- [X] T004 [P] Create `vite.config.ts` with `base: "./"`, `build.target: "es2022"`, the `@scala-js/vite-plugin-scalajs` integration, and `?url` handling for the DuckDB `.wasm` + worker assets
- [X] T005 [P] Create `index.html` (mount node for the Scala.js app) and `tsconfig.json` (only to satisfy ScalablyTyped type-def resolution)
- [X] T006 [P] Verify `.gitignore` covers `node_modules/ dist/ target/ .bsp/ .bloop/ .metals/ .scala-build/` (append any missing) and create empty `modules/{domain,engine,console,content,app}` source trees

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared base types + the **gating interop spike**. ⚠️ **No user-story work may begin until T015 (spike acceptance) passes.**

**Shared base types (pure domain — no DOM/Laminar):**

- [X] T007 [P] Define boundary value types `QueryResult`, `EngineStatus` (Loading/Ready/Failed), `EngineError` in `modules/domain/src/main/scala/drillbanken/domain/Engine.scala`
- [X] T008 [P] Define `LocalizedText` and `Language` (Sv/En) in `modules/domain/src/main/scala/drillbanken/domain/I18n.scala`
- [X] T009 [P] Define core loop/grading model types (`Phase`, `LessonId`, `PartId`, `Checker`, `Rubric`, `Grade`, `ReflectionReport`, `CheckOutcome`) in `modules/domain/src/main/scala/drillbanken/domain/Model.scala` (no logic yet — types only)

**Interop spike (Milestone 0 — GATING):**

- [X] T010 Interop facades for `@duckdb/duckdb-wasm` (`engine/DuckDbFacade.scala`) and `@xterm/xterm` (`console/XtermFacade.scala`). ScalablyTyped OOM'd the compiler → hand-written minimal `js.native` facades instead (research.md D2 ADR)
- [X] T011 Implement DuckDB-WASM bootstrap in `modules/engine/src/main/scala/drillbanken/engine/EngineService.scala`: `selectBundle` → `?url` `.wasm`+worker imports → `new Worker` → `AsyncDuckDB(VoidLogger)` → `instantiate` → `connect` → `PRAGMA version`; expose `status: Signal[EngineStatus]` (research.md D3, contracts/engine-service.md)
- [X] T012 Implement `EngineService.exec(sql): Future[QueryResult]` with Arrow materialization (cols from `schema.fields`; rows via `toArray().toJSON()`; bigint→string, Date→ISO, non-int number→fixed(6), null→null) and typed error mapping to `EngineError` (research.md D4)
- [X] T013 Implement `ConsoleService` over xterm in `modules/console/src/main/scala/drillbanken/console/ConsoleService.scala` (`open/write/writeLine/clear/onSubmit/prompt`) (contracts/console-service.md)
- [X] T014 Create Laminar app shell + `Main` in `modules/app/src/main/scala/drillbanken/app/Main.scala` that mounts the console, renders `EngineStatus`, runs one query into a Laminar table, and surfaces one deliberate SQL error as a typed `Failed` value
- [X] T015 **Spike acceptance gate**: run `npm run dev` and verify per quickstart — `Loading → Ready(<PRAGMA version>)`, `SELECT 42 AS answer` renders a table, `SELECT * FROM does_not_exist` shows a typed error, and no runtime CDN fetch for the engine

**Content scaffolding (needed by all stories):**

- [X] T016 [P] Define the lesson content DSL types (`LessonDef`, `Transcript`, `TranscriptStep`, `PartDrill`, `WholeTask`, `Exam`, `Hint`, `ReferenceSolution`, `SeedRef`) in `modules/content/src/main/scala/drillbanken/content/LessonDef.scala` (contracts/lesson-dsl.md)
- [X] T017 [P] Implement the trading-book seed dataset (DDL/DML for `traders`, `instruments`, `trades` with deliberate NULLs + orphan rows) in `modules/content/src/main/scala/drillbanken/content/SeedData.scala` (research.md D6, FR-020)
- [X] T018 Implement `EngineService.resetSeed()` to run `SeedData` and wire seed execution into bootstrap (FR-020)
- [X] T019 Create `Curriculum` registry + startup validation (unique/ordered `sequence`) in `modules/content/src/main/scala/drillbanken/content/Curriculum.scala`
- [X] T020 [P] Implement the meta-command parser (`help|hint|progress|repeat-demo|abort`, else → SQL) in `modules/app/src/main/scala/drillbanken/app/MetaCommand.scala` (FR-009, FR-010)

**Checkpoint**: Foundation + spike green — user stories can begin.

---

## Phase 3: User Story 1 - Run a full lesson loop end to end (Priority: P1) 🎯 MVP

**Goal**: A learner plays one seeded lesson through VISA → INSTRUERA → ÖVA(parts) → ÖVA(whole) → PRÖVA and gets a points grade + reflection screen.

**Independent Test**: Load the first lesson, advance all five phases via SQL + meta-commands, pass PRÖVA, see a points grade and a reflection screen.

### Tests for User Story 1 (write first; must fail before implementation)

- [X] T021 [P] [US1] ScalaCheck properties for the loop state machine (phase order fixed; PRÖVA gate blocks until ÖVA gates met; abort never grades) in `modules/domain/src/test/scala/drillbanken/domain/LoopSpec.scala`
- [X] T022 [P] [US1] ScalaCheck properties for the checker (order-insensitive passes reordered rows; order-sensitive fails; no shapeRule ⇒ any correct SQL passes) in `modules/domain/src/test/scala/drillbanken/domain/CheckSpec.scala`
- [X] T023 [P] [US1] ScalaCheck properties for grading (numeric points only; ÖVA repetition not an input; `passed = points ≥ threshold`) and reflection (`drillAgain ⊆ parts`) in `modules/domain/src/test/scala/drillbanken/domain/GradingSpec.scala`

### Implementation for User Story 1

- [X] T024 [US1] Implement the loop state machine `Loop.start/advance` with typed transitions + gates in `modules/domain/src/main/scala/drillbanken/domain/loop/Loop.scala` (data-model.md, contracts/domain-api.md)
- [X] T025 [P] [US1] Implement `Check.canonicalize/check` (lowercase cols, stringify rows, sort unless order-sensitive; optional shapeRule) in `modules/domain/src/main/scala/drillbanken/domain/check/Check.scala`
- [X] T026 [P] [US1] Implement `Grading.grade` (points from correctness/attempts/hints/time) + `Grading.reflect` (drillAgain) in `modules/domain/src/main/scala/drillbanken/domain/grade/Grading.scala`
- [X] T027 [US1] Author the first seeded lesson (`LessonDef` with transcript, parts+checkers, whole, exam rubric, bilingual prose) in `modules/content/src/main/scala/drillbanken/content/lessons/Lesson01.scala` and register it in `Curriculum`
- [X] T028 [P] [US1] Implement bilingual i18n string lookup (UI chrome via `LocalizedText`) in `modules/app/src/main/scala/drillbanken/app/Messages.scala` (FR-027). NOTE: the reactive `Signal[Language]` live-toggle re-render is T064.
- [X] T029 [US1] Implement VISA phase view: replay the transcript at full speed via the console (FR-002) wired through `Loop`
- [X] T030 [US1] Implement INSTRUERA phase view: stepwise transcript replay with annotations (FR-002)
- [X] T031 [US1] Implement ÖVA(parts) phase view: per-drill prompt, SQL exec, live-checked pass/retry with immediate feedback (FR-003), unlimited repetition with no score change (FR-014)
- [X] T032 [US1] Implement ÖVA(whole) phase view: whole-task with `hint` meta-command applying a points cost (FR-004)
- [X] T033 [US1] Implement PRÖVA phase view: exam against slutkrav, hints disabled, grade computed via rubric (FR-005, FR-013)
- [X] T034 [US1] Implement the reflection screen: rubric breakdown + generated "drill again" list (FR-006)
- [X] T035 [US1] Wire `help`/`hint`/`progress` meta-commands into the loop controller in `modules/app/src/main/scala/drillbanken/app/LessonController.scala` (FR-010)
- [X] T036 [US1] Validate US1 end-to-end via quickstart recipe step 3 (full loop on Lesson01)

**Checkpoint**: A full lesson loop is playable end to end (MVP).

---

## Phase 4: User Story 2 - Fail PRÖVA and be routed to targeted drills (Priority: P2)

**Goal**: A failing PRÖVA shows a reflection that routes the learner back to exactly the responsible ÖVA(parts) drills.

**Independent Test**: Submit a wrong PRÖVA answer → failing grade → reflection lists the responsible parts → accepting returns to those drills; a later pass records the higher points.

### Tests for User Story 2 (write first)

- [X] T037 [P] [US2] ScalaCheck properties: failing PRÖVA does not advance; `drillAgain` maps only to missed-requirement parts; best-grade is monotonic in points (FR-007, FR-017) in `modules/domain/src/test/scala/drillbanken/domain/RerouteSpec.scala`

### Implementation for User Story 2

- [X] T038 [US2] Implement the re-entry transition `Loop.advance` for accepted "drill again" → re-enter named `OvaParts` drills (without resetting passed drills' best state) in `modules/domain/src/main/scala/drillbanken/domain/loop/Loop.scala`
- [X] T039 [US2] Wire the reflection screen's "drill again" acceptance to the re-entry transition in `LessonController.scala` (FR-007)
- [X] T040 [US2] Validate US2 end-to-end via quickstart recipe step 4

**Checkpoint**: Fail→reroute loop works; US1 still passes.

---

## Phase 5: User Story 3 - Resume a half-finished lesson (Priority: P2)

**Goal**: Closing and reopening the app resumes the in-progress lesson at the same phase/sub-step with state intact.

**Independent Test**: Start a lesson, complete some drills, reload, reopen — resumes at the same phase with attempts/hints/score preserved.

### Tests for User Story 3 (write first)

- [X] T041 [P] [US3] munit round-trip tests for `ProgressState`/`LessonState` (serialize → deserialize is identity; first-run state has only lowest-sequence lesson unlocked) in `modules/domain/src/test/scala/drillbanken/domain/ProgressSpec.scala`

### Implementation for User Story 3

- [X] T042 [P] [US3] Implement `ProgressState`, `LessonProgress`, and progression rules (`Progression.applyGrade/isUnlocked`, unlock-on-pass, streak/insignia) in `modules/domain/src/main/scala/drillbanken/domain/progress/Progression.scala` (FR-015, FR-016, FR-017)
- [X] T043 [US3] Implement `PersistenceService.load/save` over `localStorage` with JSON (de)serialization of `ProgressState` (incl. `LessonState` resume snapshot) in `modules/app/src/main/scala/drillbanken/app/PersistenceService.scala` (FR-021, FR-022)
- [X] T044 [US3] Wire load-on-start + save-on-transition into `Main`/`LessonController`; resume to saved phase/sub-step (FR-022)
- [X] T045 [US3] Validate US3 end-to-end via quickstart recipe step 6

**Checkpoint**: Resume works across reloads; US1–US2 still pass.

---

## Phase 6: User Story 4 - Replay the demonstration mid-drill (Priority: P3)

**Goal**: `repeat-demo` replays VISA during a drill and returns to the same drill; refused in PRÖVA.

**Independent Test**: During a drill, run `repeat-demo`, watch VISA, return to the same drill with no score change; confirm refusal in PRÖVA.

### Tests for User Story 4 (write first)

- [X] T046 [P] [US4] ScalaCheck properties: `RepeatDemoRequested` legal in any ÖVA phase, returns `DemoNotAllowedInProva` in PRÖVA, never mutates score (FR-010, US4) in `modules/domain/src/test/scala/drillbanken/domain/RepeatDemoSpec.scala`

### Implementation for User Story 4

- [X] T047 [US4] Implement `ConsoleService.replay(steps, speed)` (FullSpeed / Stepwise+annotations) in `modules/console/src/main/scala/drillbanken/console/ConsoleService.scala` (contracts/console-service.md)
- [X] T048 [US4] Wire the `repeat-demo` meta-command to replay VISA and restore the current drill state; refuse during PRÖVA in `LessonController.scala` (FR-010)
- [X] T049 [US4] Validate US4 end-to-end via quickstart recipe step 5

**Checkpoint**: Demo replay works; US1–US3 still pass.

---

## Phase 7: User Story 5 - Author adds a new lesson as a typed lesson definition (Priority: P3)

**Goal**: Adding a `LessonDef` to the curriculum (no engine change) makes a new lesson appear and be playable; malformed lessons fail to compile.

**Independent Test**: Add a second `LessonDef`, rebuild, confirm it appears in sequence and plays; confirm a malformed definition is a compile error.

### Implementation for User Story 5

- [X] T050 [P] [US5] Author a second seeded lesson (anti-join / NULL focus on the trading book) in `modules/content/src/main/scala/drillbanken/content/lessons/Lesson02.scala` and register it in `Curriculum`
- [X] T051 [US5] Confirm startup validation rejects a duplicate `sequence` and that omitting a required `LocalizedText`/field is a compile error (extend `Curriculum` validation messages) (FR-019)
- [X] T052 [P] [US5] Write the lesson-authoring guide in `docs/authoring-lessons.md` referencing `contracts/lesson-dsl.md`
- [X] T053 [US5] Validate US5 end-to-end via quickstart recipe step 8

**Checkpoint**: Curriculum grows via content only; US1–US4 still pass.

---

## Phase 8: User Story 6 - Export and import progress (Priority: P3)

**Goal**: Export progress to a file and import it to restore; invalid imports rejected without changing existing state.

**Independent Test**: Make progress, export, clear storage, import — unlocks/grades/streak restored; a corrupt import is rejected and leaves state unchanged.

### Tests for User Story 6 (write first)

- [X] T054 [P] [US6] munit tests for `export`/`importFrom`: round-trip identity; `BadJson` and `UnsupportedSchema` leave state unchanged (FR-023) in `modules/app/src/test/scala/drillbanken/app/PersistenceImportSpec.scala`

### Implementation for User Story 6

- [X] T055 [US6] Implement `PersistenceService.export` (JSON + `schemaVersion`) and `importFrom` (validate JSON → schema → structure; non-mutating on failure) in `PersistenceService.scala` (FR-023)
- [X] T056 [US6] Add export/import UI affordances (download/upload) wired to `PersistenceService` in `Main.scala`/console flow
- [X] T057 [US6] Validate US6 end-to-end via quickstart recipe step 7

**Checkpoint**: All user stories independently functional.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: CI/CD, hygiene, docs, deploy.

- [ ] T058 Create `.github/workflows/deploy.yml`: dual toolchain (Temurin JDK 21 + `sbt/setup-sbt` with sbt/Coursier/ivy caches; `setup-node@v6` node 22 `cache: npm`), sequence sbt Scala.js compile → `npm run build` → `configure-pages` → `upload-pages-artifact` (path `dist`) + deploy job (`deploy-pages@v5`, `environment: github-pages`); add `workflow_dispatch` and Pages permissions/concurrency (research.md D12)
- [ ] T059 [P] Create `.github/dependabot.yml` (`npm` + `github-actions`, weekly) and `.github/workflows/secret-scan.yml` (TruffleHog `--results=verified,unknown`, checkout `fetch-depth: 0`) (research.md D13)
- [ ] T060 One-time: enable GitHub Pages (Settings → Pages → Source: GitHub Actions, or `gh api repos/<owner>/<repo>/pages -X POST -f build_type=workflow`) BEFORE first deploy (research.md D12) — manual step, document in README
- [ ] T061 [P] Update `README.md` (status → in development; build/run/deploy) and add `docs/ARCHITECTURE.md` with any Mermaid diagrams validated locally via `mmdc` (research.md D15)
- [ ] T062 [P] Decide optional GoatCounter analytics: implement only if cookieless + DNT/GPC no-op + never transmits SQL (FR-025), else document as deferred in README (research.md D14)
- [ ] T063 Run the full quickstart e2e recipe (steps 1–9) against a production `npm run build`; confirm no runtime CDN fetch for the engine (SC-010)
- [ ] T064 Implement the language toggle (a control + a `lang` meta-command) that updates the reactive `Signal[Language]` and persists `language` to `ProgressState` via `PersistenceService`, re-rendering all active views with no progress loss in `modules/app/src/main/scala/drillbanken/app/Main.scala` (FR-027, SC-011) — depends on T028, T042, T043
- [ ] T065 Validate language switching mid-lesson via quickstart: toggle sv↔en during a drill, confirm all chrome + lesson prose switch and phase/sub-step/score are preserved (SC-011)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. **The spike gate (T015) blocks all user stories.**
- **User Stories (Phase 3–8)**: depend on Foundational. US1 is the MVP. US2/US4 build on US1's loop; US3/US6 share `PersistenceService` (US3 creates it, US6 extends it); US5 is content-only.
- **Polish (Phase 9)**: depends on the desired user stories being complete (T058–T060 can be prepared earlier but verified last).

### User Story Dependencies

- **US1 (P1)**: after Foundational. Foundation for the loop UI other stories reuse.
- **US2 (P2)**: after US1 (reuses reflection + loop).
- **US3 (P2)**: after Foundational; independent of US2. Creates `PersistenceService`.
- **US4 (P3)**: after US1 (reuses console + loop).
- **US5 (P3)**: after Foundational (content DSL + Curriculum); independent.
- **US6 (P3)**: after US3 (extends `PersistenceService`).

### Within Each User Story

- Domain tests (ScalaCheck/munit) written first and failing → then domain implementation.
- Domain model/logic before app/UI wiring.
- Each story ends with a quickstart e2e validation task.

### Parallel Opportunities

- Setup: T003, T004, T005, T006 in parallel after T001/T002.
- Foundational base types: T007, T008, T009 in parallel; content T016, T017 in parallel; T020 in parallel.
- US1 tests T021–T023 in parallel; domain impl T025/T026 in parallel (different files) after T024; T028 in parallel.
- US5 T050 and T052 in parallel.
- Polish T059, T061, T062 in parallel.

---

## Parallel Example: User Story 1

```text
# Tests first (parallel — different files):
T021 LoopSpec.scala
T022 CheckSpec.scala
T023 GradingSpec.scala

# Then domain implementation (T025, T026 parallel after T024):
T024 loop/Loop.scala
T025 check/Check.scala
T026 grade/Grading.scala
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 Setup → 2. Phase 2 Foundational **incl. the spike gate T015** → 3. Phase 3 US1
→ 4. STOP and validate the full loop (quickstart step 3) → 5. demo.

### Incremental Delivery

Foundation → US1 (MVP) → US2 → US3 → US4 → US5 → US6, validating each independently via
its quickstart step before moving on. Polish/CI (Phase 9) lands the GitHub Pages deploy.

> **Note (this session is specification-only)**: per the project brief, the pipeline stops
> after `/speckit-tasks` + `/speckit-analyze`. These tasks are the executable plan for a
> later implementation session, not work to be done now.

---

## Notes

- [P] = different files, no incomplete dependencies.
- The domain module MUST stay free of Laminar/DOM/interop imports (Principle III); domain
  tasks reference only `domain` packages.
- The interop spike (T010–T015) is the single highest-risk item and gates everything —
  do not parallelize feature work ahead of it.
- Commit after each task or logical group.

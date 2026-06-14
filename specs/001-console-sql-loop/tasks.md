---
description: "Task list for Console SQL Tutor — the Instruction Loop (v1)"
---

# Tasks: Console SQL Tutor — the Instruction Loop (v1)

**Input**: Design documents from `/specs/001-console-sql-loop/`

**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Tests**: INCLUDED. The constitution mandates a pure, test-driven domain module with
property-based tests (munit + ScalaCheck); domain test tasks are therefore required, not
optional.

**Organization**: Tasks are grouped by user story (US1–US6 from spec.md, priority order) so
each is independently implementable and testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable (different files, no incomplete dependency)
- **[Story]**: US1–US6; Setup/Foundational/Polish carry no story label
- Paths follow the multi-module layout in plan.md (`modules/{domain,duckdb,terminal,i18n,app}`,
  `ts/`, `content/lessons/`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project skeleton and toolchain.

- [ ] T001 Create the multi-module sbt Scala.js build (`build.sbt`, `project/plugins.sbt`) with modules `domain`, `duckdb`, `terminal`, `i18n`, `app` and the dependency rule (domain depends on nothing internal)
- [ ] T002 [P] Add Vite host: `package.json`, `vite.config.ts` (`base:"./"`, `build.target:"es2022"`), `index.html` with the terminal mount element, and `@scala-js/vite-plugin-scalajs`
- [ ] T003 [P] Add Laminar + Airstream to `app`/UI modules and munit + ScalaCheck to `domain` test scope in `build.sbt`
- [ ] T004 [P] Add `@duckdb/duckdb-wasm` and `xterm` npm deps and wire their `.wasm`/worker assets as Vite `?url` imports
- [ ] T005 [P] Configure scalafmt + formatting/lint config at repo root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The gating interop spike plus the shared cores every story needs.

**⚠️ CRITICAL**: No user-story work begins until this phase is complete. T006–T008 are the
Milestone 0 gate.

- [ ] T006 Implement the TypeScript interop shim in `ts/shim.ts` per `contracts/interop-shim.md` (`boot`, `exec`→flattened `{cols,rows}` off Arrow, `seed`, `createTerminal`)
- [ ] T007 Implement the `duckdb` service core in `modules/duckdb/src/main/scala/` — typed facade over the shim, `EngineStatus = Loading|Ready(version)|Failed(msg)` as an Airstream `Signal`, `exec` failure mapped to a typed error (NOT a thrown exception)
- [ ] T008 **[Milestone 0 GATE]** Minimal Laminar spike in `modules/app/`: boot reports version into `Signal[EngineStatus]`, one query renders as a Laminar-bound table, one deliberate SQL error renders as a typed error value — verify in-browser before proceeding
- [ ] T009 [P] Define core domain types in `modules/domain/src/main/scala/` — `Phase`, `Lesson`, `PartDrill`, `Task`, `Checker`, `Exam`, `Rubric`, `Grade`, `Attempt`, `Reflection`, `ProgressRecord` per data-model.md
- [ ] T010 [P] Implement the result-equality `Checker` in `modules/domain/` (canonicalize: lowercase cols, JSON rows, sort unless `ordered`; optional shape constraint) with munit + ScalaCheck property tests in `modules/domain/src/test/scala/`
- [ ] T011 [P] Implement the `i18n` module in `modules/i18n/src/main/scala/` — `enum Locale {Sv,En}` (Sv default), typed `MessageKey` catalog with Sv fallback, Airstream `Var[Locale]`, native `Intl` formatting facade
- [ ] T012 [P] Implement the `terminal` xterm.js facade + console I/O adapter in `modules/terminal/src/main/scala/` (open/write/onData, meta-command tokenizer: help/hint/progress/repeat-demo/abort)
- [ ] T013 [P] Implement the lesson content loader + schema validation in `modules/domain/` per `contracts/lesson-schema.md` (per-lesson error reporting, no abort on one bad file) with tests
- [ ] T014 [P] Implement the persistence layer in `modules/domain/` + a thin storage facade — `localStorage` read/write, export/import with `schemaVersion` validation per `contracts/progress-schema.md`, with tests
- [ ] T015 [P] Add the seed dataset (`content/` or `duckdb`) — trading-book DDL/DML with deliberate gaps/NULLs; wire `seed()`/reset

**Checkpoint**: Engine proven end to end; pure domain (state machine types, checker, i18n,
loader, persistence) test-covered. User stories can begin.

---

## Phase 3: User Story 1 — Complete a lesson end to end (Priority: P1) 🎯 MVP

**Goal**: A learner runs one lesson through all five phases and gets a grade + reflection.

**Independent Test**: Load lesson 1, advance VISA→INSTRUERA→ÖVA(parts)→ÖVA(whole)→PRÖVA,
submit a correct exam answer, see a grade and reflection breakdown.

### Tests for User Story 1

- [ ] T016 [P] [US1] Property tests for the `Phase` state machine + gating in `modules/domain/src/test/scala/` — legal transitions only; `OvaWhole→Prova` blocked until ÖVA gates met (SC-009)
- [ ] T017 [P] [US1] Tests for `Rubric` grading + points→IG/G/VG thresholds and `Reflection` derivation in `modules/domain/src/test/scala/`

### Implementation for User Story 1

- [ ] T018 [US1] Implement the lesson `Phase` state machine + gating in `modules/domain/` (depends on T009)
- [ ] T019 [US1] Implement rubric scoring (points, attempt/hint/time-box penalties), grade mapping, and `Reflection` builder in `modules/domain/` (depends on T009, T017)
- [ ] T020 [P] [US1] VISA/INSTRUERA transcript player (timed-frame replay) in `modules/app/` or `terminal/` per D4
- [ ] T021 [US1] ÖVA (parts) drill runner UI in `modules/app/` — submit via console, immediate pass/retry, unlimited repeats (uses T010, T012)
- [ ] T022 [US1] ÖVA (whole) UI in `modules/app/` — self-paced full task, optional hints with recorded cost
- [ ] T023 [US1] PRÖVA exam UI in `modules/app/` — no hints, runs the rubric, shows grade
- [ ] T024 [US1] Reflection screen in `modules/app/` — what/why graded + drill-again list (localized via i18n)
- [ ] T025 [US1] Author lesson 1 as a data artifact in `content/lessons/` per `contracts/lesson-schema.md` (sv + en), validated by the loader
- [ ] T026 [US1] Wire the full loop in `modules/app/` (nav across phases, engine ready gating of Run/Check, locale-aware strings)

**Checkpoint**: One lesson fully playable to a grade + reflection — MVP.

---

## Phase 4: User Story 2 — Fail PRÖVA → routed back to targeted drills (Priority: P1)

**Goal**: A failed/partial exam names the lost sub-skills and routes back to exactly those
part-drills (not a full restart).

**Independent Test**: Submit a deliberately partial exam answer; confirm the reflection names
failed sub-skills and offers one-click routing to those specific part-drills.

### Tests for User Story 2

- [ ] T027 [P] [US2] Tests in `modules/domain/` that a partial grade yields a `drillAgain` list of the exact part-drill ids tied to lost sub-skills; ÖVA repeats don't lower a later grade (SC-008)

### Implementation for User Story 2

- [ ] T028 [US2] Map failed rubric sub-skills → `drillAgain` part-drill ids in the `Reflection` builder in `modules/domain/` (extends T019)
- [ ] T029 [US2] "Drill again" routing in `modules/app/` — from reflection, jump directly into the named `OvaParts` drills, then allow re-attempting PRÖVA

**Checkpoint**: The remediation loop works; US1 still passes.

---

## Phase 5: User Story 3 — Replay VISA mid-drill (Priority: P2)

**Goal**: Re-watch VISA/INSTRUERA at any time during drilling without losing place.

**Independent Test**: From inside a part-drill, invoke repeat-demo, watch, return to the exact
step with progress intact.

### Tests for User Story 3

- [ ] T030 [P] [US3] Test in `modules/domain/` that a demo-replay action preserves drill state/position (no progress loss)

### Implementation for User Story 3

- [ ] T031 [US3] Wire the `repeat-demo` meta-command to the transcript player with state preservation in `modules/app/` (uses T012, T020)

**Checkpoint**: Replay works from any drilling state.

---

## Phase 6: User Story 4 — Resume after closing the browser (Priority: P2)

**Goal**: Reload/reopen resumes phase, step, attempts, hint usage; progression view reflects
lock/in-progress/complete.

**Independent Test**: Advance partway, fully close + reopen, confirm same phase/step + history.

### Tests for User Story 4

- [ ] T032 [P] [US4] Round-trip tests in `modules/domain/` — persist → restore yields identical progress; unlock rule (N unlocked iff N-1 Completed) holds (SC-005)

### Implementation for User Story 4

- [ ] T033 [US4] Persist progress on each meaningful transition and rehydrate on load in `modules/app/` (uses T014)
- [ ] T034 [US4] Progression/lesson-list view in `modules/app/` showing locked / in-progress / completed (localized)

**Checkpoint**: Sessions survive reloads; progression visible.

---

## Phase 7: User Story 5 — Author adds a lesson as a data file (Priority: P2)

**Goal**: A new lesson is a single validated data artifact; no engine changes; malformed files
reported, not silently broken.

**Independent Test**: Add a new conforming lesson → appears in order, fully playable; add a
malformed one → clear error, others unaffected.

### Tests for User Story 5

- [ ] T035 [P] [US5] Loader tests in `modules/domain/` — valid lesson loads + orders correctly; malformed lesson reports `{file, errors[]}` and is skipped (FR-028)

### Implementation for User Story 5

- [ ] T036 [P] [US5] Author a second lesson in `content/lessons/` (sv + en) exercising joins/NULLs to validate the format end to end
- [ ] T037 [US5] Document the lesson-authoring format in `docs/authoring-lessons.md` (mirrors `contracts/lesson-schema.md`)

**Checkpoint**: Content scales without engine edits.

---

## Phase 8: User Story 6 — Export / import progress (Priority: P3)

**Goal**: Export progress to a file; import restores it; bad import rejected without
clobbering existing progress.

**Independent Test**: Export, import into a clean browser → progression restored; import a
corrupt file → clear error, existing progress intact.

### Tests for User Story 6

- [ ] T038 [P] [US6] Tests in `modules/domain/` — export→import restores full state; version-mismatch/corrupt import rejected and existing progress preserved (FR-025)

### Implementation for User Story 6

- [ ] T039 [US6] Export (download JSON) + import (file picker, validate, apply-or-reject) UI in `modules/app/` (uses T014)

**Checkpoint**: Portability without a backend.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T040 [P] Add `.github/workflows/ci.yml` — checkout@v6 + setup-java (Temurin) + sbt (Coursier/ivy/sbt caches) + setup-node@v6 (node 22, cache npm) → `sbt domain/test` → `npm ci` → `npm run build`
- [ ] T041 [P] Add `.github/workflows/deploy.yml` — Pages job per plan.md (dual toolchain, configure-pages@v6, upload-pages-artifact@v5, deploy-pages@v5, `workflow_dispatch`); **enable Pages before first run** (gh api / Settings)
- [ ] T042 [P] Add `.github/dependabot.yml` (npm + github-actions, weekly) and `.github/workflows/secret-scan.yml` (TruffleHog `--results=verified,unknown`, fetch-depth 0)
- [ ] T043 [P] Add locale switcher UI in `modules/app/` and persist the choice (FR-033); verify no raw message keys appear for sv/en (SC-010)
- [ ] T044 [P] Write `docs/ARCHITECTURE.md` with Mermaid diagrams (component, phase state machine, build/deploy) — validate with `mmdc` before commit
- [ ] T045 Run `quickstart.md` validation end to end (dev, build, deploy dry-run) and confirm SC-001..SC-010

---

## Dependencies & Execution Order

- **Setup (P1)**: no deps.
- **Foundational (P2)**: depends on Setup. **T006→T007→T008 (Milestone 0 gate) MUST pass
  before any user story.** T009–T015 are mostly [P] after the gate.
- **User Stories (P3+)**: all depend on Foundational. US1 is the MVP; US2 extends US1's
  rubric/reflection; US3/US4/US6 build on the app shell + persistence; US5 is independent
  (content + loader). Within a story: tests before implementation; domain before UI.
- **Polish (P9)**: after the desired stories.

## Parallel Opportunities

- Setup: T002–T005 in parallel.
- Foundational after the gate: T009–T015 largely parallel (distinct modules/files).
- Per story, the `[P]` test tasks run in parallel; domain tasks precede the app UI tasks.
- Different stories can be staffed in parallel once Foundational is done.

## Implementation Strategy

1. Setup → Foundational, **stopping to validate the Milestone 0 gate (T008) in a browser**.
2. US1 → independently test the full loop → MVP demo.
3. Add US2, then US3/US4/US5/US6 incrementally, each independently testable.
4. Polish: CI/deploy/hygiene/docs + full quickstart validation.

## Notes

- Tests here are constitutional (pure domain, property-based), concentrated in
  `modules/domain/` so they run headless in CI.
- The TS shim (T006) is the only untyped surface; keep it minimal and contract-bound.
- Commit after each task or logical group; never enter PRÖVA paths that bypass ÖVA gating.

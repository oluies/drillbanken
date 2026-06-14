# Feature Specification: Console SQL Tutor (Drillbänken v1)

**Feature Branch**: `002-console-sql-tutor`

**Created**: 2026-06-14

**Status**: Draft

**Input**: User description: "Gamified, console-based SQL tutor (DuckDB-WASM in the browser) built on the Försvarsmakten instruction loop VISA→INSTRUERA→ÖVA(parts)→ÖVA(whole)→PRÖVA mapped onto Kolb; v1 subject domain is SQL."

## User Scenarios & Testing *(mandatory)*

The product teaches a skill (SQL in v1) through the Swedish Armed Forces instruction
loop, expressed entirely in a terminal-style console. A lesson always advances through
five phases in order — **VISA** (watch the target performed), **INSTRUERA** (watch it
again, slowly and annotated), **ÖVA (parts)** (drill each sub-step), **ÖVA (whole)**
(do the whole task with optional costed hints), **PRÖVA** (graded exam, no hints) — and
closes with a reflection screen mapping the result back to what to drill again.

### User Story 1 - Run a full lesson loop end to end (Priority: P1)

A learner opens a lesson and is carried through VISA → INSTRUERA → ÖVA(parts) →
ÖVA(whole) → PRÖVA, then sees a graded result and a reflection screen. This is the
minimum viable product: one complete loop on one lesson delivers the entire pedagogical
value of the product.

**Why this priority**: The instruction loop is the product. Without a complete loop
there is nothing to evaluate; every other story is an enhancement of, or a branch off,
this spine.

**Independent Test**: Load a single seeded lesson, advance through all five phases by
issuing SQL and meta-commands in the console, pass PRÖVA, and confirm a grade plus a
reflection screen are shown. Delivers a full learning session on its own.

**Acceptance Scenarios**:

1. **Given** a lesson the learner has unlocked, **When** they open it, **Then** the
   console plays the VISA demonstration at full speed showing the target end state
   ("målbild") with no commentary.
2. **Given** VISA has finished, **When** the learner proceeds, **Then** INSTRUERA
   replays the same demonstration slowly and stepwise with short, imperative
   keyword-level annotations.
3. **Given** INSTRUERA has finished, **When** the learner enters ÖVA(parts), **Then**
   each sub-step is presented as an isolated drill that gives immediate pass/retry
   feedback, and a failed or repeated drill never reduces any score.
4. **Given** all part-drills are passed, **When** the learner enters ÖVA(whole),
   **Then** they perform the whole task self-paced and may request hints, each of which
   carries a stated score cost.
5. **Given** the ÖVA gates are met, **When** the learner enters PRÖVA, **Then** the exam
   runs against the published end requirement ("slutkrav") with hints unavailable.
6. **Given** the learner submits the PRÖVA answer, **When** it is graded, **Then** a
   grade is shown followed by a reflection screen listing what was graded, why, and the
   specific part-drills to drill again.

### User Story 2 - Fail PRÖVA and be routed back to targeted drills (Priority: P2)

A learner who does not meet the end requirement on PRÖVA is shown a reflection that
names the sub-steps that cost points and offers to return them directly to those
specific ÖVA(parts) drills, not to the start of the lesson.

**Why this priority**: Closing the Kolb loop — concrete experience → reflective
observation → renewed active experimentation — is what distinguishes this from a quiz.
Targeted re-drilling is the mechanism that makes failure productive.

**Independent Test**: Submit a deliberately wrong PRÖVA answer; confirm the grade is a
fail, the reflection lists the responsible part-drills, and accepting the "drill again"
prompt returns the learner to exactly those drills.

**Acceptance Scenarios**:

1. **Given** a PRÖVA submission that misses the end requirement, **When** it is graded,
   **Then** the result is a failing grade and PRÖVA does not advance the learner.
2. **Given** a failed PRÖVA, **When** the reflection screen renders, **Then** it lists
   the specific part-drills associated with the missed requirements.
3. **Given** the reflection's "drill again" list, **When** the learner accepts it,
   **Then** they re-enter those ÖVA(parts) drills, and a later passing PRÖVA records the
   higher grade.

### User Story 3 - Resume a half-finished lesson (Priority: P2)

A learner who closes the browser mid-lesson returns later and continues from where they
left off, with phase position, completed drills, attempt counts and score state intact.

**Why this priority**: Lessons span multiple phases and repetitions; without durable
local resume the loop cannot realistically be completed in one sitting, undermining P1.

**Independent Test**: Begin a lesson, complete some part-drills, close/reload the page,
reopen the lesson, and confirm it resumes at the same phase with prior progress intact.

**Acceptance Scenarios**:

1. **Given** an in-progress lesson, **When** the learner closes and reopens the
   application, **Then** the lesson resumes at the same phase and sub-step.
2. **Given** a resumed lesson, **When** it reopens, **Then** completed drills, attempt
   counts, hint usage and accumulated score are preserved.
3. **Given** no prior progress exists, **When** the learner opens the application,
   **Then** they start at the first unlocked lesson with a clean state.

### User Story 4 - Replay the demonstration mid-drill (Priority: P3)

While practising, a learner can re-watch the VISA demonstration on demand without losing
their place in the current drill.

**Why this priority**: Re-observation is a legitimate, expected move in the loop, but it
enhances rather than gates the core flow.

**Independent Test**: During an ÖVA drill, issue the repeat-demo meta-command, watch VISA
replay, and confirm the learner returns to the same drill with progress intact.

**Acceptance Scenarios**:

1. **Given** the learner is in any ÖVA phase, **When** they issue the repeat-demo
   meta-command, **Then** the VISA demonstration replays.
2. **Given** the replay finishes, **When** it ends, **Then** the learner returns to the
   exact drill and state they left, with no score change.
3. **Given** the learner is in PRÖVA, **When** they attempt repeat-demo, **Then** it is
   refused because PRÖVA permits no assistance.

### User Story 5 - Author adds a new lesson as a data file (Priority: P3)

An author adds a complete lesson — demonstration script, keyword instructions,
part-drills with checkers, whole-task, and exam rubric — as a single declarative data
artifact, and it appears in the curriculum without any change to the engine.

**Why this priority**: Content-as-data is what lets the curriculum grow; valuable but not
required to validate the loop on the seeded lessons that ship.

**Independent Test**: Add one new lesson artifact following the documented shape, reload,
and confirm it appears in sequence and is fully playable through all five phases without
code changes.

**Acceptance Scenarios**:

1. **Given** a new lesson artifact in the documented format, **When** the application
   loads, **Then** the lesson appears in the curriculum in its defined sequence position.
2. **Given** the new lesson, **When** a learner plays it, **Then** all five phases,
   checkers, hints and the exam rubric behave as defined by the artifact.
3. **Given** a malformed lesson artifact, **When** the application loads, **Then** the
   problem is surfaced clearly rather than corrupting the curriculum or other lessons.

### User Story 6 - Export and import progress (Priority: P3)

A learner exports their progress to a file and later imports it — on the same or another
device — to continue, since there is no server-side account.

**Why this priority**: Provides portability and backup in a backendless product, but the
core single-device experience works without it.

**Independent Test**: Make progress, export to a file, clear local state, import the
file, and confirm progress is restored.

**Acceptance Scenarios**:

1. **Given** existing progress, **When** the learner exports, **Then** a portable file
   containing their progress is produced.
2. **Given** a previously exported file, **When** the learner imports it, **Then**
   lesson unlocks, grades, streaks and per-lesson state are restored.
3. **Given** an invalid or corrupted import file, **When** the learner imports it,
   **Then** the import is rejected with a clear message and existing progress is left
   unchanged.

### Edge Cases

- **Invalid SQL / engine error**: A syntactically invalid or failing statement is shown
  as a readable error in the console and counts only as an attempt where the phase scores
  attempts; it never crashes the session.
- **Order-sensitive vs order-insensitive results**: A correct result in a different row
  order passes when the task is order-insensitive, but fails when the task declares an
  ordering requirement.
- **Equivalent-but-different SQL**: Two different correct formulations (e.g. a join vs an
  equivalent subquery) both pass when results match and the task does not constrain SQL
  shape.
- **Attempting to skip ahead**: Entering PRÖVA before the ÖVA gates are met is refused.
- **Abort mid-lesson**: The abort meta-command leaves the lesson cleanly, preserving
  resume state, without recording a grade.
- **First run with no progress**: The learner sees only the first lesson unlocked.
- **Privacy signals present**: When Do Not Track / Global Privacy Control is set, optional
  usage analytics emit nothing.

## Requirements *(mandatory)*

### Functional Requirements

**Instruction loop & gating**

- **FR-001**: Every lesson MUST advance through the five phases VISA → INSTRUERA →
  ÖVA(parts) → ÖVA(whole) → PRÖVA in that fixed order.
- **FR-002**: VISA MUST present the target performance at full speed with no commentary;
  INSTRUERA MUST replay the same performance slowly, stepwise, with short imperative
  keyword-level annotations.
- **FR-003**: ÖVA(parts) MUST present each sub-step as an isolated drill with immediate
  pass/retry feedback and MUST allow unlimited repetition.
- **FR-004**: ÖVA(whole) MUST let the learner perform the whole task self-paced with
  optional hints, each carrying a defined score cost.
- **FR-005**: The system MUST prevent entry into PRÖVA until the lesson's ÖVA gates are
  met, and MUST run PRÖVA against the published end requirement with hints disabled.
- **FR-006**: After every PRÖVA grade (pass or fail) the system MUST show a reflection
  screen stating what was graded, why, and a generated "drill again" list of the specific
  part-drills that cost points.
- **FR-007**: On a failing PRÖVA the system MUST offer to route the learner directly to
  the specific ÖVA(parts) drills named in the reflection.

**Console & interaction**

- **FR-008**: All learner interaction MUST occur in a terminal-style console; the system
  MUST NOT use forms or multiple-choice inputs.
- **FR-009**: The console MUST accept SQL statements and execute them client-side in the
  browser, returning results or a readable error.
- **FR-010**: The console MUST support the meta-commands help, hint, progress,
  repeat-demo, and abort, with hint and repeat-demo refused during PRÖVA.

**Checking & grading**

- **FR-011**: Checkers MUST compare the learner's result set against a reference
  solution, treating row order as insignificant unless the task declares an ordering
  requirement, in which case order MUST be enforced.
- **FR-012**: Where a task defines a required SQL shape, the checker MUST also validate
  that shape; otherwise any formulation producing the correct result MUST pass.
- **FR-013**: PRÖVA MUST produce a grade from a per-lesson rubric that may weigh
  correctness, number of attempts, hint usage, and a per-lesson time-box where defined.
- **FR-014**: Repetition in any ÖVA phase MUST NOT reduce any score; only hint usage and
  PRÖVA performance affect the grade.

**Progression & gamification**

- **FR-015**: Lessons MUST unlock in sequence, with a passing PRÖVA the only gate that
  advances the learner to the next lesson.
- **FR-016**: Streaks and insignia MUST be earned solely from completed (passed) PRÖVA;
  the system MUST NOT provide leaderboards in v1.
- **FR-017**: A re-attempted lesson MUST record the learner's best grade.

**Content as data**

- **FR-018**: Each lesson MUST be a single declarative artifact containing its
  demonstration script, keyword instructions, part-drills with checkers, whole-task, and
  exam rubric.
- **FR-019**: Adding or editing a lesson MUST require no change to the engine, and a
  malformed lesson MUST be surfaced without corrupting other lessons.
- **FR-020**: The seeded dataset MUST be a small trading book (traders, instruments,
  trades) containing deliberate NULLs and orphan rows (a trader with no trades, an
  instrument never traded, trades with NULL price) so that join/NULL/anti-join lessons
  have real teeth; the system MUST provide a reset-data path that restores the seed.

**Persistence & portability**

- **FR-021**: Learner progress (lesson unlocks, phase position, completed drills, attempt
  counts, hint usage, grades, streaks, insignia) MUST persist locally on the device with
  no server.
- **FR-022**: The learner MUST be able to resume an in-progress lesson at the same phase
  and sub-step after closing the application.
- **FR-023**: The learner MUST be able to export progress to a portable file and import
  it to restore progress, with invalid imports rejected without altering existing state.

**Privacy & scope**

- **FR-024**: The system MUST NOT require accounts, server synchronization, or any
  backend.
- **FR-025**: Optional usage analytics, if present, MUST be cookieless, MUST be a no-op
  when Do Not Track or Global Privacy Control is set, and MUST never transmit the
  learner's SQL; if these guarantees cannot be met, analytics MUST be deferred.
- **FR-026**: The system MUST ship SQL as the only subject domain in v1 while keeping the
  loop, checking, grading and progression independent of the subject so other practice
  engines can be added later without re-architecting.

### Key Entities *(include if feature involves data)*

- **Lesson**: A single declarative unit of curriculum; holds its demonstration script,
  keyword instructions, ordered part-drills, whole-task, exam rubric, sequence position,
  and any time-box. The unit of content authoring.
- **Phase**: One of VISA, INSTRUERA, ÖVA(parts), ÖVA(whole), PRÖVA, with fixed order and
  entry gates.
- **Part-Drill**: An isolated sub-step practice item with its own checker and reference
  solution; the granularity at which the "drill again" list operates.
- **Checker**: The rule comparing a learner result (and optionally SQL shape) to a
  reference, parameterised by order sensitivity.
- **Exam / Rubric**: The PRÖVA definition plus the per-lesson scoring rubric (correctness,
  attempts, hint usage, time-box) yielding a grade.
- **Seed Dataset**: The trading-book data (traders, instruments, trades) with deliberate
  NULLs and orphan rows, restorable via reset.
- **Learner Progress**: Per-device state — lesson unlocks, current phase/sub-step,
  completed drills, attempt counts, hint usage, best grades, streaks, insignia.
- **Reflection Report**: The post-PRÖVA breakdown of what was graded and the generated
  "drill again" list.
- **Insignia / Streak**: Recognition earned only from passed PRÖVA.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A learner can complete a full lesson loop — all five phases plus the
  reflection screen — without leaving the console or encountering a dead end.
- **SC-002**: 100% of lessons enforce the phase order and the PRÖVA gate: PRÖVA cannot be
  entered before the ÖVA gates are met in any lesson.
- **SC-003**: For checked tasks, a correct result in a different row order passes
  order-insensitive tasks and fails order-sensitive ones, and two distinct correct SQL
  formulations both pass when no SQL shape is required — verified across the seeded
  lessons.
- **SC-004**: A failing PRÖVA always yields a reflection whose "drill again" list maps
  only to part-drills tied to the missed requirements, and accepting it returns the
  learner to exactly those drills.
- **SC-005**: Repetition in ÖVA never lowers a learner's score in any lesson.
- **SC-006**: A learner can close the application mid-lesson and resume at the same phase
  and sub-step with all prior progress intact.
- **SC-007**: A learner can export progress and restore it via import such that unlocks,
  grades and streaks match the pre-export state.
- **SC-008**: An author can add a new playable lesson by adding one data artifact, with
  zero engine code changes, and it appears in sequence.
- **SC-009**: With Do Not Track or Global Privacy Control set, no analytics events are
  emitted and no learner SQL is ever transmitted.
- **SC-010**: The entire experience runs client-side with no backend and no runtime
  dependency on an external content server.

## Assumptions

- **Reused, proven prior art (cited, not re-derived)**: The in-browser SQL engine
  bootstrap, result materialization, result-equality/canonicalization checker, and the
  trading-book seed dataset are carried over as settled facts from the deployed sibling
  project *SQL Concepts Lab* (`oluies/sql-concepts-lab`). This spec treats them as given
  context; the console replaces that project's editor-and-button UI.
- **Single-device, local-first**: With no backend, progress lives on the device;
  cross-device continuity is served only by manual export/import (US6).
- **Modern evergreen browser**: The learner uses a current desktop browser capable of
  running the in-browser SQL engine; broad legacy-browser support is not a v1 goal.
- **Curriculum size**: v1 ships a small set of seeded SQL lessons sufficient to exercise
  joins, NULL handling, and anti-joins against the seed dataset; exact lesson count is a
  content decision, not an engine constraint.
- **Frontend strategy is decided (recorded for downstream phases)**: A pure client-side
  Scala.js + Laminar implementation is the chosen approach; earlier Vue bindings and a
  hybrid split were considered and rejected. This is an implementation decision recorded
  here for traceability and does not affect the user-facing requirements above.

## Open Decisions (deferred to `/speckit-clarify`)

These do not block the spec but are flagged for explicit resolution in the clarify step
per the project brief; reasonable defaults are noted but NOT yet committed:

- **Grading scale**: Swedish IG/G/VG vs numeric points vs both. (Affects the rubric and
  reflection presentation.)
- **UI & content language**: Swedish, English, or both from the start. (Affects all
  learner-facing text and lesson content.)
- **VISA realization**: pre-recorded timed transcript vs scripted live execution against
  the real engine. (Affects determinism of the demonstration.)
- **Lesson content format**: data file (YAML/JSON) vs in-bundle typed objects vs
  markdown-with-frontmatter. (Affects author ergonomics for non-Scala authors.)
- **Time-box scope**: whether any v1 lesson actually defines a time-box, or whether it
  remains a rubric capability unused in v1.

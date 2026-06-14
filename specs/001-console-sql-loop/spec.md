# Feature Specification: Console SQL Tutor — the Instruction Loop (v1)

**Feature Branch**: `001-console-sql-loop`

**Created**: 2026-06-14

**Status**: Draft (awaiting clarification — see "Open Decisions")

**Input**: User description: "Console-based SQL tutor v1: the Försvarsmakten instruction loop (VISA/INSTRUERA/ÖVA/PRÖVA) over DuckDB-WASM in a terminal UI"

## Overview

Drillbänken v1 teaches SQL through a terminal. Each lesson runs the Swedish Armed Forces
instruction loop — **VISA → INSTRUERA → ÖVA (parts) → ÖVA (whole) → PRÖVA** — ending in a
graded examination and a reflection screen. The learner types real SQL at a prompt; the
system runs it against a real in-browser database and judges results against reference
solutions. Lessons unlock in sequence, progress is saved locally, and the whole product
is a static site with no backend.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Complete a lesson end to end through the full loop (Priority: P1)

A learner opens a lesson and is carried through every phase in order: watches the target
skill performed (VISA), sees it replayed slowly with keyword instruction (INSTRUERA),
drills each sub-step until fluent (ÖVA parts), performs the whole task self-paced (ÖVA
whole), then sits a graded, hint-free examination (PRÖVA) and receives a grade plus a
reflection screen.

**Why this priority**: This is the product. If a single lesson cannot be completed
through all five phases with a grade at the end, nothing else matters. It is the minimum
viable slice that delivers the core teaching value.

**Independent Test**: Load the first lesson, advance through VISA → INSTRUERA → ÖVA
(parts) → ÖVA (whole) → PRÖVA, submit a correct exam answer, and confirm a grade and a
reflection breakdown are shown. Fully exercisable with one lesson and no other features.

**Acceptance Scenarios**:

1. **Given** a fresh learner on lesson 1, **When** they begin, **Then** the console first
   presents the VISA demonstration of the target end state with no commentary.
2. **Given** the learner has watched VISA, **When** they continue, **Then** INSTRUERA
   replays the same demonstration slowly with stepwise keyword-level instruction.
3. **Given** the learner is in ÖVA (parts), **When** they submit a correct sub-step,
   **Then** they get immediate pass feedback; **When** they submit an incorrect one,
   **Then** they get retry feedback and may attempt again without penalty.
4. **Given** all part-drills are passed, **When** the learner reaches ÖVA (whole),
   **Then** they may attempt the full task self-paced and may request a hint.
5. **Given** the ÖVA gates are met, **When** the learner enters PRÖVA and submits a
   correct answer, **Then** the system grades it against the published end requirement
   and shows a grade.
6. **Given** a grade has been issued, **When** PRÖVA ends, **Then** a reflection screen
   shows what was graded, why, and a list of what to drill again.

---

### User Story 2 - Fail PRÖVA and be routed back to targeted drills (Priority: P1)

A learner who fails (or underperforms on) the examination is shown precisely which
sub-skills cost them and is routed back to the specific ÖVA part-drills for those
sub-skills, rather than restarting the whole lesson.

**Why this priority**: The remediation loop is the heart of the method (ÖVA exists to be
returned to). Without it the loop is just a linear lesson with a quiz at the end.

**Independent Test**: Submit a deliberately wrong/partial exam answer and confirm the
reflection screen names the failed sub-skills and offers a direct path back to exactly
those part-drills.

**Acceptance Scenarios**:

1. **Given** the learner submits an exam answer that fails part of the rubric, **When**
   the grade is computed, **Then** the reflection screen lists the specific sub-skills
   that lost points.
2. **Given** the reflection screen lists failed sub-skills, **When** the learner chooses
   to drill again, **Then** they are taken to those specific ÖVA part-drills, not to the
   start of the lesson.
3. **Given** the learner re-drills and re-attempts PRÖVA, **When** the new grade is
   computed, **Then** earlier ÖVA repetitions have not reduced their score.

---

### User Story 3 - Replay VISA mid-drill (Priority: P2)

While drilling, a learner can re-watch the demonstration (full-speed VISA or the slow
INSTRUERA replay) at any time without losing their place in the current drill.

**Why this priority**: Re-observation is part of the cycle; a learner who is stuck needs
the målbild again without abandoning progress. Valuable but not required for a first
graded run.

**Independent Test**: From within an ÖVA part-drill, invoke a "repeat demonstration"
action, watch it, then confirm the drill resumes at the same point with prior progress
intact.

**Acceptance Scenarios**:

1. **Given** a learner partway through a part-drill, **When** they request a demonstration
   replay, **Then** the demonstration plays and the drill state is preserved.
2. **Given** the replay has finished, **When** it ends, **Then** the learner returns to
   the exact drill step they left.

---

### User Story 4 - Resume a half-finished lesson after closing the browser (Priority: P2)

A learner who closes the browser mid-lesson returns later to find their phase, drill
progress, attempts, and hint usage exactly as they left them.

**Why this priority**: Lessons span minutes; losing progress on reload would make the
product frustrating to use. Important, but the core loop can be demonstrated in one sitting.

**Independent Test**: Advance partway into a lesson, reload the page (and re-open after
fully closing), and confirm the lesson resumes at the same phase and step with the same
attempt/hint history.

**Acceptance Scenarios**:

1. **Given** a learner partway through a lesson, **When** they reload the page, **Then**
   the lesson resumes at the same phase, step, and recorded attempt/hint state.
2. **Given** stored progress exists for a lesson, **When** the learner reopens the app,
   **Then** the progression view reflects which lessons are locked, in progress, and
   completed.

---

### User Story 5 - Author adds a new lesson as a data file (Priority: P2)

A content author adds a complete new lesson — demonstration script, keyword instructions,
part-drills with checkers, whole-task, and exam rubric — by adding a single declarative
lesson artifact, with no changes to engine code.

**Why this priority**: Content as data is a constitutional principle and the path to
scaling the product. Not required to ship the first lesson, but required before the
product is more than a demo.

**Independent Test**: Add a new lesson artifact following the documented format and
confirm it appears in the progression and is fully playable, without recompiling or
editing engine logic.

**Acceptance Scenarios**:

1. **Given** a new lesson artifact in the documented format, **When** the app loads,
   **Then** the lesson appears in the correct sequence position and is playable end to
   end.
2. **Given** a lesson artifact with a malformed or incomplete definition, **When** the app
   loads it, **Then** the problem is reported clearly rather than failing silently or
   corrupting other lessons.

---

### User Story 6 - Export and import progress (Priority: P3)

A learner can export their progress to a file and later import it (e.g. on another
machine or browser), since there are no accounts.

**Why this priority**: Without a backend, a file is the only portability mechanism.
Useful but the lowest priority of the set for a first release.

**Acceptance Scenarios**:

1. **Given** a learner with recorded progress, **When** they export, **Then** they receive
   a single file capturing their progression and per-lesson state.
2. **Given** a previously exported file, **When** the learner imports it, **Then** their
   progression and per-lesson state are restored, and the app indicates success or a clear
   error if the file is invalid.

### Edge Cases

- A learner attempts to enter PRÖVA before the ÖVA gates are met → entry is refused with a
  clear explanation of what remains.
- A submitted statement errors in the engine (syntax/semantic error) → the error is shown
  as feedback in the console; it does not crash the lesson or count as a graded failure
  outside the rubric's defined rules.
- A correct answer reached by a different but equivalent query (e.g. a join vs. a
  subquery) → accepted, because judging is by result equality (order-insensitive unless
  the task requires ordering).
- A query returns the right rows in the wrong order on an order-sensitive task → treated
  as not matching.
- The SQL engine fails to load → the app reports the failure clearly and does not present
  a broken console.
- Local storage is unavailable, full, or cleared between sessions → the app degrades
  gracefully and informs the learner that progress may not be saved.
- An imported progress file is corrupt or from an incompatible version → import is
  rejected with a clear message and existing progress is left intact.
- A learner requests a hint during ÖVA (whole) → the hint is provided and the score cost
  is recorded for that lesson's eventual grade where the rubric uses it.

## Requirements *(mandatory)*

### Functional Requirements

**The instruction loop**

- **FR-001**: The system MUST model each lesson as the ordered phase sequence VISA →
  INSTRUERA → ÖVA (parts) → ÖVA (whole) → PRÖVA, and MUST present phases in that order.
- **FR-002**: VISA MUST demonstrate the target end state of the skill at full speed with
  no commentary.
- **FR-003**: INSTRUERA MUST replay the same demonstration slowly and stepwise with
  short, imperative, keyword-level instruction.
- **FR-004**: ÖVA (parts) MUST present each sub-step as an isolated console drill giving
  immediate pass-or-retry feedback, and MUST allow unlimited repetition without penalty.
- **FR-005**: ÖVA (whole) MUST let the learner perform the full task self-paced and MUST
  offer optional hints, recording any hint usage.
- **FR-006**: The system MUST prevent entry to PRÖVA until the lesson's ÖVA gates are met,
  and MUST explain what remains when entry is refused.
- **FR-007**: PRÖVA MUST be a graded examination with no hints, judged against the
  lesson's published end requirement ("slutkrav").
- **FR-008**: After PRÖVA the system MUST show a reflection screen stating what was
  graded, why, and which specific sub-skills to drill again.
- **FR-009**: On a failed or partial PRÖVA, the system MUST route the learner back to the
  specific ÖVA part-drills tied to the sub-skills that lost points.
- **FR-010**: The learner MUST be able to replay the demonstration (VISA/INSTRUERA) at any
  time during drilling without losing current drill progress.

**Console interaction**

- **FR-011**: All learner input MUST be entered in a terminal-style console; the system
  MUST NOT use forms or multiple-choice widgets for answers.
- **FR-012**: The console MUST accept SQL statements and execute them against an in-browser
  database, displaying results or errors as console output.
- **FR-013**: The console MUST support a small set of meta-commands including at least:
  help, hint, progress, repeat-demo, and abort.
- **FR-014**: Engine errors from a submitted statement MUST be surfaced as readable console
  feedback without crashing the lesson.

**Judging**

- **FR-015**: The system MUST judge a learner's answer by comparing its result set against
  a reference solution, order-insensitive by default and order-sensitive only where the
  task declares it.
- **FR-016**: The system MUST accept any query whose result matches the reference,
  regardless of differing-but-equivalent SQL phrasing, except where a task additionally
  constrains the SQL shape.
- **FR-017**: Where a task constrains the SQL shape (beyond result equality), the system
  MUST evaluate that constraint as part of judging.

**Grading & progression**

- **FR-018**: PRÖVA grading MUST be driven by a per-lesson rubric producing a grade from
  correctness, number of attempts, hint usage, and time-boxing where the lesson defines
  one.
- **FR-019**: ÖVA repetitions MUST never reduce a learner's grade.
- **FR-020**: Lessons MUST unlock in sequence; advancement MUST be gated on passing PRÖVA
  for the preceding lesson.
- **FR-021**: The system MUST award streaks/insignia tied only to completed PRÖVA
  examinations.
- **FR-022**: The system MUST NOT include leaderboards or any feature requiring a backend.

**Persistence**

- **FR-023**: The system MUST persist learner progress locally on the device (phase, drill
  progress, attempts, hint usage, grades, unlock state).
- **FR-024**: A learner MUST be able to resume a half-finished lesson after reloading or
  reopening the app, with prior state intact.
- **FR-025**: The learner MUST be able to export all progress to a single file and import
  it again, with clear success/failure feedback and no corruption of existing progress on
  a failed import.

**Content as data**

- **FR-026**: Each lesson MUST be a single self-contained declarative artifact holding its
  demonstration script, keyword instructions, part-drills with checkers, whole-task, and
  exam rubric.
- **FR-027**: Adding or editing a lesson MUST NOT require changes to engine code.
- **FR-028**: The system MUST report a malformed or incomplete lesson artifact clearly,
  without breaking other lessons.

**Architecture (forward-looking, constitutional)**

- **FR-029**: The architecture MUST allow additional subject engines (e.g. a shell or
  kubectl simulator) to be added later, while v1 ships only the SQL subject.

### Key Entities *(include if feature involves data)*

- **Lesson**: a self-contained teaching unit; ordered position in the curriculum; owns a
  demonstration, keyword instructions, a set of part-drills, a whole-task, and an exam
  rubric.
- **Phase**: one of VISA, INSTRUERA, ÖVA-parts, ÖVA-whole, PRÖVA; the unit of progression
  within a lesson, governed by gating rules.
- **Part-drill**: an isolated sub-step exercise with a checker and pass/retry feedback;
  the target of "drill again" routing.
- **Checker**: the judging rule for a drill or exam answer (reference result; ordered or
  unordered; optional SQL-shape constraint).
- **Exam rubric**: per-lesson grading definition mapping correctness, attempts, hint
  usage, and optional time-box to a grade.
- **Attempt**: a single learner submission with its outcome (and, for ÖVA, no scoring
  impact).
- **Grade**: the outcome of a PRÖVA, with the rubric breakdown that produced it.
- **Progress record**: per-learner, per-lesson state — current phase/step, attempts, hint
  usage, grades, and unlock status — persisted locally and exportable.
- **Dataset**: the sample data a lesson's queries run against (seed tables with deliberate
  gaps/NULLs that make join/NULL/anti-join skills meaningful).
- **Reflection**: the post-PRÖVA summary of what was graded, why, and what to drill again.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A learner can complete a full lesson — all five phases — and receive a grade
  with a reflection breakdown, in a single sitting.
- **SC-002**: 100% of PRÖVA results produce a reflection screen that names the specific
  sub-skills that lost points (zero grades end without actionable reflection).
- **SC-003**: A learner who fails PRÖVA can reach the exact remedial part-drills in one
  action from the reflection screen.
- **SC-004**: Two different but result-equivalent correct queries both pass the same
  unordered task in 100% of cases.
- **SC-005**: After closing and reopening the browser, a half-finished lesson resumes at
  the same phase and step with the same attempt/hint history in 100% of cases where local
  storage is available.
- **SC-006**: A new lesson can be added as a single data artifact and played end to end
  with zero engine-code changes.
- **SC-007**: Exported progress re-imported into a clean browser restores the learner's
  full progression and per-lesson state.
- **SC-008**: ÖVA repetition never changes a subsequently computed grade for the same
  performance (repetition is provably cost-free).
- **SC-009**: A learner is never able to reach PRÖVA before the lesson's ÖVA gates are met.

## Resolved Decisions (recorded per constitution)

- **Frontend strategy is pure Scala 3 / Scala.js + Laminar.** Earlier candidates — Vue
  bindings, and a hybrid Scala-core / JS-UI split — were considered and **rejected** in
  favor of a single typed stack with a thin Laminar UI over a framework-free domain core.
  This is settled and not reopened by clarification.

## Open Decisions (to resolve in /speckit-clarify before planning)

These are surfaced for explicit decision rather than assumed silently. They are listed
here and asked in the clarify step:

1. JS interop approach for the terminal and the SQL engine (hand-written facades vs
   generated facades vs a thin TypeScript shim exposing a narrow typed API).
2. Lesson content format (structured data file vs in-bundle Scala DSL vs
   markdown-with-frontmatter).
3. Grading scale (Swedish IG/G/VG vs points vs both).
4. VISA implementation (pre-recorded timed transcript vs scripted live execution).
5. Language of UI and content (Swedish, English, or both).

## Assumptions

- The v1 subject is SQL only; the sample dataset and lesson content are SQL-oriented.
- "Locally" means the learner's browser storage on the current device; clearing browser
  data removes progress unless it was exported. Export/import is the only portability path
  in v1 (no accounts, no sync).
- The judging model and sample-dataset approach follow the proven sibling project *SQL
  Concepts Lab* (result-equality comparison; seed tables with deliberate gaps/NULLs);
  these are treated as settled prior art and not re-derived.
- Time-boxing applies only to lessons whose rubric defines one; lessons without a defined
  time-box are not timed.
- Privacy-respecting, cookieless usage analytics is optional and out of the v1 critical
  path; if added it must be a no-op under Do-Not-Track / Global Privacy Control and must
  never transmit learner query text.
- Accessibility and mobile-friendliness are desirable but the primary target is a
  desktop-class terminal experience; specific accessibility targets are not set in v1.

# Phase 1 Data Model — Console SQL Tutor (v1)

Entities live in the pure `domain` module (no Laminar, no DOM). Types below are conceptual;
field names are indicative. "Localized" means the field carries per-locale variants (D5).

## Lesson (content artifact)

- `id`: stable identifier (string)
- `order`: curriculum sequence position (int)
- `title`: localized
- `slutkrav` (end requirement): localized description of what PRÖVA grades against
- `dataset`: seed reference or inline seed (ordered DDL/DML strings)
- `demonstration`: timed transcript (VISA) — ordered frames `{ atMs, output }`
- `instruera`: ordered annotated steps `{ keyword, instruction(localized), transcriptRef }`
- `partDrills`: ordered list of **Part-drill**
- `wholeTask`: **Task** (full end-to-end) with optional hints (localized) and hint score cost
- `exam`: **Exam** (PRÖVA task + **Rubric**)
- **Validation**: required fields present; ≥1 part-drill; exam present; every rubric
  sub-skill maps to a real part-drill id; default-locale (`Sv`) variant present for each
  localized field. Loader reports violations per-lesson without aborting others (FR-028).

## Phase (enum)

`Visa | Instruera | OvaParts | OvaWhole | Prova` — the unit of progression. Illegal
transitions are unrepresentable; gating rules govern advancement (see State Transitions).

## Part-drill

- `id`: stable id (referenced by rubric sub-skills and "drill again")
- `prompt`: localized
- `task`: **Task** (the sub-step)
- `checker`: **Checker**
- Repetition is unlimited and never scored (FR-004/019).

## Task

- `referenceSolution`: SQL string producing the expected result
- `ordered`: bool — whether row order matters
- `shapeConstraint?`: optional SQL-shape rule evaluated in addition to result equality
- `hints?`: ordered localized hints (ÖVA-whole only), each with a score cost

## Checker

- Judges a submission: run learner SQL + reference SQL, canonicalize (lowercase cols,
  JSON rows, sort unless `ordered`), compare equality; if `shapeConstraint` present,
  evaluate it too (D7). Yields `Pass | Retry(reason)` for drills, or a per-sub-skill outcome
  for the exam.

## Exam (PRÖVA) + Rubric

- `task`: **Task** (no hints permitted)
- `rubric`:
  - `subSkills`: list of `{ id, partDrillId, points, criterion }` — points awarded per
    sub-skill from correctness
  - `attemptPenalty?`, `hintPenalty?`, `timeBox?` (ms) with its penalty — all optional
  - `gradeThresholds`: points → `IG | G | VG` mapping (D3)
- Produces a **Grade**.

## Grade

- `value`: `IG | G | VG`
- `points`: computed total
- `breakdown`: per-sub-skill points earned/possible + applied penalties → feeds Reflection

## Attempt

- `submission`: raw input text
- `phase`, `target` (drill id or exam)
- `outcome`: pass/retry/graded
- ÖVA attempts carry **no** scoring impact (FR-019).

## Reflection

- Derived from a Grade: `gradedItems` (what/why), `drillAgain`: list of part-drill ids whose
  sub-skills lost points (drives one-click routing back, FR-009).

## Progress record (persisted, per learner-device)

- `schemaVersion`: int (guards import compatibility)
- `locale`: `Sv | En`
- `perLesson`: map `lessonId → { phase, stepIndex, attempts, hintsUsed, bestGrade?, status }`
  where `status ∈ Locked | InProgress | Completed`
- `streak` / `insignia`: derived from completed PRÖVA only (FR-021)
- **Unlock rule**: lesson N unlocked iff lesson N-1 `status = Completed` (PRÖVA passed)
  (FR-020).

## Locale / Message catalog

- `Locale`: `enum { Sv, En }`, `Sv` default
- `MessageKey`: closed set (sealed/enum) → missing keys are compile errors (FR-031)
- `Catalog`: `Locale → (MessageKey → String)` with `Sv` fallback for missing entries
- Active locale: Airstream `Var[Locale]` → reactive re-render (FR-033)

## State Transitions (Phase machine)

```text
Visa → Instruera → OvaParts → OvaWhole → Prova → (Grade) → Reflection
```

- `OvaParts → OvaWhole`: allowed only when every part-drill has been passed at least once.
- `OvaWhole → Prova`: allowed only when the ÖVA gates are met; otherwise entry refused with
  an explanation (FR-006, SC-009).
- `Visa`/`Instruera` replay: reachable from any drilling state without losing position
  (FR-010, US3).
- Post-`Prova`: `Reflection` always shown; on fail/partial, Reflection offers routing back to
  the specific `OvaParts` drills named in `drillAgain` (FR-008/009).

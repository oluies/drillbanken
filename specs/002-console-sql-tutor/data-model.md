# Phase 1 Data Model: Console SQL Tutor (Drillbänken v1)

This model lives in the pure `domain` module (no Laminar/DOM/interop types). Types are
shown in Scala-ish pseudocode for intent; field names and the state machine are normative,
exact signatures are the implementer's. Validation rules are traced to spec FRs.

## Bilingual text

```
LocalizedText = { sv: String, en: String }   // FR-027; selected by the active language
Language = Sv | En
```

All learner-facing prose in content uses `LocalizedText`. SQL and phase names are plain
strings (untranslated domain terms).

## The instruction loop (state machine) — FR-001, FR-005, Principle I

```
Phase = Visa | Instruera | OvaParts | OvaWhole | Prova
```

Fixed order: `Visa → Instruera → OvaParts → OvaWhole → Prova`. Transitions are total and
typed; illegal transitions are unrepresentable or rejected with a typed error.

```
LessonState = {
  lessonId:       LessonId
  phase:          Phase
  partIndex:      Int                 // current drill within OvaParts
  partResults:    Map[PartId, PartResult]   // pass/attempts/hintsUsed per part-drill
  wholeResult:    Option[WholeResult]
  hintsUsedTotal: Int
  startedAt:      Instant
  status:         InProgress | Aborted | Completed
}
```

**Gate rules** (enforced in transitions):
- Cannot enter `Prova` until every required `PartId` in `partResults` is passed and the
  whole-task gate is met (the "ÖVA gates"). — FR-005, SC-002
- `Visa`/`Instruera` are advisory phases; replaying VISA (repeat-demo) is allowed from any
  ÖVA phase but **refused in `Prova`**. — FR-010, US4
- `abort` moves `status → Aborted`, preserves resume state, records no grade. — Edge case

**Kolb mapping** (documented, not a runtime field): Visa/Instruera = abstract
conceptualization (observation); Ova* = active experimentation; Prova = concrete
experience; the reflection screen = reflective observation. — Principle I

## Lesson (content) — FR-018, FR-020, US5

A `LessonDef` is a typed, in-bundle object (the content DSL). It is data, not engine code.

```
LessonDef = {
  id:           LessonId
  sequence:     Int                    // unlock order — FR-015
  title:        LocalizedText
  endRequirement: LocalizedText        // "slutkrav" shown in PRÖVA — FR-005
  seed:         SeedRef                // which seed dataset (v1: the trading book)
  demo:         Transcript             // VISA/INSTRUERA source — FR-002, D9
  parts:        List[PartDrill]        // ordered sub-step drills — FR-003
  whole:        WholeTask              // FR-004
  exam:         Exam                   // FR-013
  timeBox:      Option[Duration]       // optional — Open Decision (time-box scope)
}
```

**Validation**: `parts` non-empty; `sequence` unique across the curriculum; every
`Rubric` reference in `exam`/`parts` resolvable. Violations are compile errors (typed
DSL) — FR-019.

### Transcript (VISA/INSTRUERA) — D9

```
Transcript = { steps: List[TranscriptStep] }
TranscriptStep = {
  input:      String              // the SQL/command shown being "typed"
  output:     QueryResultView     // expected rendered output for the demo
  delayMs:    Int                 // full-speed timing for VISA
  annotation: Option[LocalizedText]  // shown stepwise in INSTRUERA only
}
```

The reference solution is still executed live for checking; the transcript only drives the
demonstration's timing and annotation.

### PartDrill / WholeTask — FR-003, FR-004

```
PartDrill = {
  id:        PartId
  prompt:    LocalizedText
  checker:   Checker
  hints:     List[Hint]           // each Hint has a points cost — FR-004, FR-014
  reference: ReferenceSolution    // SQL run live to produce the expected result
}
WholeTask = { prompt: LocalizedText, checker: Checker, hints: List[Hint], reference: ReferenceSolution }
Hint = { text: LocalizedText, cost: Int }
```

## Checker — FR-011, FR-012, D5

```
Checker = {
  orderSensitive: Boolean         // FR-011
  shapeRule:      Option[SqlShapeRule]   // FR-012 — applied only when present
}

CanonResult = { cols: List[String], rows: List[String] }   // cols lowercased, rows stringified
canonicalize(QueryResult, orderSensitive): CanonResult     // sorts rows unless order-sensitive
check(learner: QueryResult, reference: QueryResult, c: Checker): CheckOutcome
CheckOutcome = Pass | Fail(reason)
```

Two distinct correct SQL formulations both pass when results match and no `shapeRule` is
set (SC-003).

## Grading & reflection — FR-006, FR-007, FR-013, FR-014

```
Rubric = {
  maxPoints:        Int
  correctnessWeight: Int
  attemptPenalty:   Int           // per extra attempt in PRÖVA only
  hintPenalty:      Int           // per hint cost
  timePenalty:      Option[TimeRule]
  passThreshold:    Int           // FR-013 — points needed to pass slutkrav
}

Grade = {
  points:    Int                  // numeric only — FR-013, D7
  passed:    Boolean              // points >= passThreshold
  breakdown: List[ScoreLine]      // correctness, attempts, hints, time — for reflection
}

ReflectionReport = {
  grade:       Grade
  whatGraded:  List[LocalizedText]
  drillAgain:  List[PartId]        // parts tied to missed requirements — FR-006, FR-007
}
```

**Scoring rules**:
- Repetition in any ÖVA phase **never** reduces score — only `Prova` attempts, hint usage,
  and time affect the grade. — FR-014, SC-005
- A failing `Prova` does **not** advance; the reflection's `drillAgain` lists only parts
  tied to missed requirements, and accepting it re-enters exactly those `OvaParts` drills.
  — FR-007, SC-004
- Re-attempting a lesson records the best (highest-points) grade. — FR-017

## Exam (PRÖVA) — FR-005, FR-013

```
Exam = { prompt: LocalizedText, checker: Checker, reference: ReferenceSolution, rubric: Rubric }
```

Hints are unavailable in `Prova` (FR-010); grading uses `rubric`.

## Progress (persisted) — FR-021, FR-022, FR-023

```
ProgressState = {
  schemaVersion: Int                     // for import validation — FR-023
  language:      Language                 // FR-027
  unlocked:      Set[LessonId]            // sequence-gated — FR-015
  lessons:       Map[LessonId, LessonProgress]
  streak:        Int                      // consecutive passed PRÖVA — FR-016
  insignia:      Set[InsigniaId]          // earned only from passed PRÖVA — FR-016
}
LessonProgress = {
  bestGrade:    Option[Grade]            // best across attempts — FR-017
  resume:       Option[LessonState]      // in-progress snapshot — FR-022
  completed:    Boolean
}
```

**Validation / lifecycle**:
- First run: only the lowest-`sequence` lesson is `unlocked`; no progress. — Edge case
- A passing `Prova` unlocks the next lesson by `sequence` and updates `streak`/`insignia`.
  — FR-015, FR-016
- Resume restores `phase`, `partIndex`, `partResults`, `hintsUsedTotal`. — FR-022, SC-006
- Export = serialize `ProgressState` to JSON (with `schemaVersion`). Import validates
  `schemaVersion` + structure; on failure, reject and leave existing state unchanged.
  — FR-023, SC-007

## Engine-facing value types (boundary) — used by domain via the EngineService contract

```
QueryResult  = { cols: List[String], rows: List[List[String | Null]] }   // already materialized
EngineStatus = Loading | Ready(version: String) | Failed(message: String)
EngineError  = { message: String }    // typed mapping of a thrown engine error
```

`QueryResult`/`EngineStatus` are the only engine types the domain references; their
production (Arrow materialization, worker bootstrap) is owned by the `engine` module behind
the `EngineService` contract.

## Meta-commands — FR-008, FR-010

```
MetaCommand = Help | Hint | Progress | RepeatDemo | Abort
```

`Hint` and `RepeatDemo` are refused during `Prova`. Anything that is not a recognized
meta-command is treated as SQL and sent to `EngineService.exec`. — FR-009

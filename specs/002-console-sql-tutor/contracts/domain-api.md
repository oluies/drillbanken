# Contract: Domain API (loop state machine, checker, grading, progression)

The pure, Laminar/DOM/interop-free core in `modules/domain`. Everything here is
deterministic and property-testable with munit + ScalaCheck. — Principle III

## Loop state machine — FR-001, FR-005

```
object Loop:
  def start(lesson: LessonDef, resume: Option[LessonState]): LessonState
  def advance(state: LessonState, ev: LoopEvent): Either[LoopError, LessonState]

LoopEvent =
  | DrillSubmitted(partId: PartId, outcome: CheckOutcome)
  | WholeSubmitted(outcome: CheckOutcome)
  | RequestProva
  | ExamSubmitted(grade: Grade)
  | RepeatDemoRequested
  | Aborted

LoopError = GateNotMet(reason) | IllegalTransition(from: Phase, ev) | DemoNotAllowedInProva
```

**Invariants (test targets)**:
- Phase order is exactly `Visa → Instruera → OvaParts → OvaWhole → Prova`; no event skips
  a phase. — SC-002
- `RequestProva` returns `GateNotMet` unless all required parts pass and the whole gate is
  met. — FR-005, SC-002
- `RepeatDemoRequested` is legal in any ÖVA phase, illegal in `Prova`. — FR-010, US4
- `Aborted` yields `status = Aborted`, never a grade. — Edge case

## Checker — FR-011, FR-012

```
object Check:
  def canonicalize(r: QueryResult, orderSensitive: Boolean): CanonResult
  def check(learner: QueryResult, reference: QueryResult, c: Checker): CheckOutcome
```

**Invariants**:
- Order-insensitive: same rows in any order → `Pass`. — SC-003
- Order-sensitive: different row order → `Fail`. — SC-003
- No `shapeRule`: any SQL producing the matching result → `Pass`. — FR-012, SC-003

## Grading — FR-013, FR-014

```
object Grading:
  def grade(rubric: Rubric, attempts: Int, hintsUsed: Int, elapsed: Option[Duration],
            correctness: CheckOutcome): Grade
  def reflect(lesson: LessonDef, grade: Grade, missed: List[PartId]): ReflectionReport
```

**Invariants**:
- Output is numeric points only; `passed = points >= rubric.passThreshold`. — FR-013
- ÖVA repetitions are not inputs to `grade` (only PRÖVA attempts/hints/time). — FR-014,
  SC-005
- `reflect.drillAgain` ⊆ the lesson's parts and corresponds only to missed requirements.
  — FR-006, FR-007, SC-004

## Progression — FR-015, FR-016, FR-017

```
object Progression:
  def applyGrade(p: ProgressState, lesson: LessonDef, g: Grade): ProgressState
  def isUnlocked(p: ProgressState, lesson: LessonDef): Boolean
```

**Invariants**:
- A passing PRÖVA unlocks the next-`sequence` lesson; a failing one does not. — FR-015
- `streak`/`insignia` change only on a passing PRÖVA. — FR-016
- `bestGrade` is monotonic in points across attempts. — FR-017

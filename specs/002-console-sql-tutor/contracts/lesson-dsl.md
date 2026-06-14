# Contract: Lesson Content DSL

Lessons are declarative, typed, in-bundle Scala objects in `modules/content`. Adding a
lesson = adding a `LessonDef` value to the curriculum and rebuilding; no engine change.
A malformed lesson is a **compile error**. — FR-018, FR-019, US5, Principle V

## Author-facing shape

```scala
val joinsAndNulls: LessonDef = LessonDef(
  id        = LessonId("joins-nulls"),
  sequence  = 2,
  title     = LocalizedText(sv = "Joins och NULL", en = "Joins and NULL"),
  endRequirement = LocalizedText(sv = "...", en = "..."),
  seed      = SeedRef.TradingBook,
  demo      = Transcript(List(
    TranscriptStep(
      input = "FROM trades SELECT * LIMIT 5",
      output = QueryResultView(...),
      delayMs = 600,
      annotation = Some(LocalizedText(sv = "Visa alla affärer", en = "Show all trades"))
    ),
    // ...
  )),
  parts = List(
    PartDrill(
      id = PartId("inner-join"),
      prompt = LocalizedText(sv = "...", en = "..."),
      checker = Checker(orderSensitive = false, shapeRule = None),
      hints = List(Hint(LocalizedText(sv = "...", en = "..."), cost = 2)),
      reference = ReferenceSolution("SELECT ... FROM traders JOIN trades USING (trader_id)")
    )
  ),
  whole = WholeTask(...),
  exam  = Exam(
    prompt = LocalizedText(...),
    checker = Checker(orderSensitive = true, shapeRule = None),
    reference = ReferenceSolution("..."),
    rubric = Rubric(maxPoints = 100, correctnessWeight = 70, attemptPenalty = 5,
                    hintPenalty = 5, timePenalty = None, passThreshold = 60)
  ),
  timeBox = None
)
```

The curriculum is the ordered set of all `LessonDef`s:

```scala
object Curriculum:
  val all: List[LessonDef]            // sorted by `sequence`; sequences must be unique
```

## Compile-time guarantees (FR-019)

- Missing/mistyped fields → compile error (it is a typed constructor).
- `LocalizedText` requires both `sv` and `en` → bilingual completeness is enforced by the
  type. — FR-027
- A `Checker`/`Rubric` with the wrong shape won't compile.

## Runtime validation (still required)

- `Curriculum.all` sequences are unique and contiguous enough to define unlock order
  (validated at startup; a duplicate `sequence` is a startup error). — FR-015
- `parts` non-empty; every `ReferenceSolution` is non-blank.

## Seed dataset — FR-020, D6

```
SeedRef.TradingBook → ordered DDL/DML: traders, instruments, trades
  with deliberate NULLs + orphan rows (trader w/o trades, instrument never traded,
  trade with NULL price); conceptual relationships (no FK constraints).
```

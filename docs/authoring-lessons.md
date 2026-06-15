# Authoring lessons

Lessons are **typed, in-bundle Scala objects** (content-as-data, Principle V). Adding a
lesson requires no engine change — only a new `LessonDef` registered in `Curriculum`, then
a rebuild. A malformed lesson is a **compile error** (see `contracts/lesson-dsl.md`).

## Steps

1. Create `modules/content/src/main/scala/drillbanken/content/lessons/LessonNN.scala`
   (use `Lesson01.scala` / `Lesson02.scala` as templates).
2. Build a `LessonDef`:
   - `id` — unique `LessonId`.
   - `sequence` — unlock order; must be **unique** across the curriculum.
   - `title`, `endRequirement` — bilingual `LocalizedText(sv = …, en = …)`.
   - `seed` — `SeedRef.TradingBook` (the only v1 dataset).
   - `demo` — a `Transcript` of `TranscriptStep`s (VISA full speed / INSTRUERA annotated).
   - `parts` — ordered `PartDrill`s, each with a `Checker`, `hints` (with point `cost`),
     and a `ReferenceSolution` (SQL run live to produce the expected result).
   - `whole` — the whole-task; `exam` — the PRÖVA prompt + `Rubric` (points only).
3. Register it in `modules/content/.../Curriculum.scala` → `all`.
4. Rebuild. `Curriculum.validate()` runs at startup and rejects duplicate sequences or
   empty `parts`.

## Checker semantics

- `Checker(orderSensitive = false)` — row order ignored (most tasks). Set `true` only when
  the task demands ordering.
- `shapeRule = Some(SqlShapeRule(List("join")))` — also require the learner SQL to contain
  the given fragments (case-insensitive). Omit to accept any SQL that yields the right rows.

## Bilingual prose

Every learner-facing string is `LocalizedText(sv = …, en = …)`; the type enforces both
languages at compile time (FR-027). SQL keywords and phase names stay untranslated.

## Make it have teeth

The trading-book seed has deliberate NULLs and orphan rows (a trader with no trades, an
instrument never traded, a trade with NULL price) — lean on those for join / NULL /
anti-join lessons (see `SeedData.scala`).

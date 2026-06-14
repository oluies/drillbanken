# Drillbänken

A gamified, console-based SQL tutor built on the Swedish Armed Forces instruction
loop from *Handbok Utbildningsmetodik* (Försvarsmakten):
**VISA → INSTRUERA → ÖVA (parts) → ÖVA (whole) → PRÖVA**, mapped onto Kolb's
experiential learning cycle.

Everything runs client-side: SQL is executed by [DuckDB-WASM](https://github.com/duckdb/duckdb-wasm)
in the browser, the UI is a terminal (xterm.js), and the whole thing is a static
site deployed to GitHub Pages. No backend.

> **Status: specification phase.** This repository currently holds only the
> specification, scaffolded with [GitHub Spec Kit](https://github.com/github/spec-kit).
> No application code yet — by design. See [`docs/spec-prompt.md`](docs/spec-prompt.md)
> for the driving brief and the architectural principles.

## Stack (planned)

- **Scala 3 + Scala.js** for all frontend code; **Laminar** (Airstream signals) for UI.
- A pure, framework-free **domain module** (lesson state machine, grading, progression)
  with **munit + ScalaCheck** property tests.
- **DuckDB-WASM** as the SQL engine; **xterm.js** as the console; both behind a
  narrow, explicitly typed interop facade.
- **Vite** (via `@scala-js/vite-plugin-scalajs`) producing a static `dist/`,
  deployed to **GitHub Pages** via GitHub Actions.

## Pedagogy is the architecture

The instruction loop is a typed state machine, not a content convention. Lessons
are declarative data (demonstration script, keyword instructions, part-drills with
checkers, whole-task, exam rubric); adding a lesson requires no engine changes.

## Spec Kit workflow

The specification is produced with the `/speckit-*` skills (installed under
`.claude/skills/`): `constitution → specify → clarify → plan → tasks → analyze`.
Run them from a Claude Code session rooted in this repository.

## Prior art

The DuckDB-WASM engine bootstrap, Arrow result-materialization, seed-dataset shape
and result-equality checker are carried over from a deployed sibling project,
[SQL Concepts Lab](https://github.com/oluies/sql-concepts-lab)
([live](https://oluies.github.io/sql-concepts-lab/)). The console replaces that
project's editor-and-button UI.

## License

MIT

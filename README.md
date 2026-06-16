# Drillbänken

A gamified SQL tutor built on the Swedish Armed Forces instruction loop from
*Handbok Utbildningsmetodik* (Försvarsmakten):
**VISA → INSTRUERA → ÖVA (parts) → ÖVA (whole) → PRÖVA**, mapped onto Kolb's
experiential learning cycle.

Everything runs client-side: SQL is executed by [DuckDB-WASM](https://github.com/duckdb/duckdb-wasm)
in the browser, the UI is a **guided web interface** (instruction → SQL editor → run →
feedback), and the whole thing is a static site deployed to GitHub Pages. No backend.

> **Status: in development.** All six v1 user stories are implemented and verified
> (full lesson loop, fail→reroute, resume, replay, author-a-lesson, export/import) plus a
> bilingual toggle. As of constitution **v2.0.0** the interface is a guided web GUI
> (CodeMirror SQL editor) rather than a terminal — see
> [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Stack

- **Scala 3 + Scala.js** for all frontend code; **Laminar** (Airstream signals) for UI.
- A pure, framework-free **domain module** (lesson state machine, grading, progression)
  with **munit + ScalaCheck** property tests.
- **DuckDB-WASM** as the SQL engine; **CodeMirror** as the SQL editor; both behind a
  narrow, hand-written `js.native` interop facade.
- **Vite** (via `@scala-js/vite-plugin-scalajs`) producing a static `dist/`,
  deployed to **GitHub Pages** via GitHub Actions.

## Develop

```bash
npm install          # deps (Vite 7, DuckDB-WASM, CodeMirror)
npm run dev          # dev server (vite-plugin-scalajs drives the sbt link)
sbt domain/test content/test app/test   # unit tests (Scala.js on node)
npm run build        # static dist/
```

End-to-end checks run headless against the built app (Chrome via puppeteer-core):
`npm run e2e` (engine boot) and `npm run e2e:gui` (full loop, reroute, resume, language).

## Using it

A guided panel walks you through each phase: read the instruction, write SQL in the
editor, and press **Run**. Buttons cover **Hint**, **Replay demo**, **To PRÖVA**, and
**Drill again**; a **sv / en** toggle switches language. The seed schema (tables,
columns, relationships) is shown as cards below the lesson.

## Deploy

Pushing to `main` builds and deploys to GitHub Pages (`.github/workflows/deploy.yml`,
dual JVM+Node toolchain). Pages must be enabled once (Settings → Pages → Source: GitHub
Actions) before the first run.

## Privacy / analytics

No backend, no cookies; progress stays on the device (exportable as a file). Optional
cookieless analytics (GoatCounter) is **deferred** for v1 — it ships only if it can be a
no-op under DNT/GPC and never transmit learner SQL.

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

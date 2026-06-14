# Quickstart: Console SQL Tutor (Drillbänken v1)

Setup, the **gating interop spike**, the build/deploy pipeline, and the end-to-end
verification recipe. Engine/deploy facts are settled prior art from `sql-concepts-lab`
(see research.md); only the JVM toolchain and the spike are net-new.

## Prerequisites

- JDK 21 (Temurin) + sbt + Coursier
- Node 22 + npm
- A modern desktop browser (WebAssembly + Web Workers)

## Project layout (target)

See `plan.md` → Project Structure. Key files: `build.sbt`, `project/plugins.sbt`
(sbt-scalajs + scalablytyped converter), `package.json`, `vite.config.ts`
(`base: "./"`, `build.target: "es2022"`), `index.html`, and the `modules/` tree.

## Local development

```bash
npm ci                 # installs vite, @duckdb/duckdb-wasm, @xterm/xterm,
                       #   @scala-js/vite-plugin-scalajs, and type defs
npm run dev            # vite dev server; @scala-js/vite-plugin-scalajs triggers the
                       #   sbt Scala.js link on change
# open the printed localhost URL
```

```bash
sbt domain/test        # run the pure-domain munit + ScalaCheck suite (no browser)
```

---

## Milestone 0 — Interop spike (GATING; do this before any feature work)

A minimal Scala.js + Laminar app that proves the whole pipeline end to end. Its code
becomes the `engine` module (Laminar-free service core + thin Laminar components).

**Build it to do exactly this:**
1. Boot DuckDB-WASM via the settled bootstrap (research.md D3): `selectBundle` → `?url`
   imports of `.wasm` + worker `.js` (NO runtime CDN) → `new Worker` →
   `new AsyncDuckDB(VoidLogger, worker)` → `instantiate` → `connect` → run seed →
   `PRAGMA version`.
2. Expose `exec(sql): Future[QueryResult]` with Arrow already materialized (research.md D4).
3. Drive a `Signal[EngineStatus]` = `Loading | Ready(version) | Failed(msg)`.
4. Render one query's result as a Laminar-bound table.
5. Map one deliberate SQL error to the typed `Failed`/error value (not a console
   exception).

**Spike acceptance (all must pass):**
- [ ] Page loads; `EngineStatus` goes `Loading → Ready(<PRAGMA version>)`.
- [ ] `SELECT 42 AS answer` renders as a Laminar table showing `answer = 42`.
- [ ] `SELECT * FROM does_not_exist` surfaces a typed `Failed`/error value in the UI.
- [ ] No network request fetches the `.wasm` or worker at runtime (bundled via `?url`).
- [ ] ScalablyTyped facade generation for `@duckdb/duckdb-wasm` (and `@xterm/xterm`)
      compiles cleanly.

> If the spike cannot be made to pass, STOP and revisit the interop decision (research.md
> D2) before planning feature work in detail.

---

## Build (static site)

```bash
npm run build          # sbt Scala.js (full opt) → vite build → static dist/
npx vite preview       # optional local check of the production bundle
```

`dist/` is the deployable artifact. Vite `base: "./"` makes it work at a project-pages
subpath or a custom-domain root.

## CI / Deploy to GitHub Pages

Reuse the verified Pages recipe **plus** the JVM toolchain gap (research.md D12):

- Permissions: `contents:read, pages:write, id-token:write`;
  `concurrency: { group: pages, cancel-in-progress: false }`; add `workflow_dispatch`.
- **Build job**: `actions/checkout@v6` → `actions/setup-java` (Temurin 21) +
  `sbt/setup-sbt` (or `coursier/setup-action`) **with sbt/Coursier/ivy caches** →
  `actions/setup-node@v6` (node 22, `cache: npm`) → `npm ci` → `npm run build`
  (sbt Scala.js compile **then** Vite build) → `actions/configure-pages@v6` →
  `actions/upload-pages-artifact@v5` (path `dist`).
- **Deploy job**: `actions/deploy-pages@v5`, `environment: github-pages`.

### One-time setup (do BEFORE the first deploy run)

`configure-pages` fails with **"Pages site Not Found"** unless Pages is enabled first:

- GitHub UI: Settings → Pages → Source: **GitHub Actions**, **or**
- `gh api repos/<owner>/<repo>/pages -X POST -f build_type=workflow`

### Hygiene (carry over)

- `.github/dependabot.yml`: `npm` + `github-actions`, weekly.
- `secret-scan.yml`: TruffleHog `--results=verified,unknown`, checkout `fetch-depth: 0`.
- `.gitignore` already covers `node_modules/ dist/ target/ .bsp/ .bloop/ .metals/`.
- Validate any Mermaid docs locally with `mmdc` before commit (research.md D15).

---

## End-to-end verification recipe

Used to confirm a change works in the real app, not just in unit tests.

1. **Unit (fast, no browser)**: `sbt domain/test` — state machine, checker
   order-sensitivity, grading, progression all green.
2. **Dev server**: `npm run dev`, open the localhost URL.
3. **Full loop (US1)**: open the first lesson; confirm VISA plays full-speed, INSTRUERA
   replays slowly with annotations, ÖVA(parts) drills give pass/retry, ÖVA(whole) accepts
   hints (with cost), PRÖVA runs without hints, and a points grade + reflection screen
   appear.
4. **Fail→reroute (US2)**: submit a wrong PRÖVA answer; confirm a failing grade, a
   reflection `drill again` list of the responsible parts, and that accepting it returns
   to exactly those drills.
5. **Replay VISA (US4)**: during a drill, run `repeat-demo`; confirm VISA replays and you
   return to the same drill; confirm `repeat-demo`/`hint` are refused in PRÖVA.
6. **Resume (US3)**: mid-lesson, reload the page; confirm it resumes at the same
   phase/sub-step with progress intact.
7. **Export/import (US6)**: export progress, clear `localStorage`, import the file;
   confirm unlocks/grades/streak restored; confirm a corrupt import is rejected without
   changing state.
8. **Add a lesson (US5)**: add a `LessonDef` to `Curriculum`, rebuild, confirm it appears
   in sequence and is playable; confirm a malformed `LessonDef` fails to compile.
9. **Production bundle**: `npm run build` succeeds and `dist/` has no runtime CDN fetch for
   the engine.

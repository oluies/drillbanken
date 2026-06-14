# Quickstart — Console SQL Tutor (v1)

> Planning artifact. Describes the intended dev/run/deploy flow once Milestone 0 lands. No
> application code exists yet (specification phase).

## Prerequisites

- JDK (Temurin 21+) and sbt
- Node 22 + npm
- A modern desktop browser (WASM + Web Workers)

## Develop

```bash
npm install                 # Vite + @scala-js/vite-plugin-scalajs + xterm + duckdb-wasm
npm run dev                 # Vite dev server; vite-plugin-scalajs triggers sbt fastLinkJS
```

The domain module is pure and test-driven:

```bash
sbt domain/test             # munit + ScalaCheck — runs headless, no browser
```

## Build & preview

```bash
npm run build               # sbt fullLinkJS (Scala.js) + tsc shim + vite build → dist/
npm run preview             # serve the production build locally
```

Vite uses `base: "./"` and `build.target: "es2022"`; DuckDB `.wasm` + worker and xterm
assets are bundled via `?url` (no runtime CDN).

## Milestone 0 — interop spike (do this first)

Stand up the gating spike and verify, in the browser:

1. Engine boots → `Signal[EngineStatus]` becomes `Ready(<version>)` (from `PRAGMA version`).
2. A sample query renders as a Laminar-bound results table.
3. A deliberately invalid statement renders a typed error (no thrown console exception).

Only after this passes do the feature phases get planned in detail. The spike's code becomes
the `duckdb` module.

## Validate a lesson (content authoring)

Drop a JSON file in `content/lessons/` following `contracts/lesson-schema.md`. On load it
must appear in the correct `order` and be fully playable; a malformed file is reported and
skipped without affecting other lessons (FR-028).

## Deploy (GitHub Pages)

- **One-time**: enable Pages BEFORE the first deploy — Settings → Pages → Source: GitHub
  Actions, or `gh api repos/oluies/drillbanken/pages -X POST -f build_type=workflow`
  (otherwise `configure-pages` fails "Pages site Not Found").
- Push to `main` → `deploy.yml` sets up **both** JDK+sbt and Node toolchains (cached), runs
  `sbt domain/test` + the Scala.js build, `npm run build`, and publishes `dist/` to Pages.

## Map to acceptance

The end-to-end learner flow (US1) — VISA → INSTRUERA → ÖVA(parts) → ÖVA(whole) → PRÖVA →
Grade → Reflection — is the primary thing to exercise; SC-001..SC-010 in `spec.md` are the
measurable checks.

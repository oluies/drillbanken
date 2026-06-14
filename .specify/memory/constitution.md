<!--
Sync Impact Report
- Version change: (template) → 1.0.0
- Ratification: initial adoption
- Principles defined:
    I. Pedagogy is the Architecture
    II. The Console is the Medium
    III. Typed Core, Scala Throughout
    IV. Static, Client-Side Deployment
    V. Content as Data
- Added sections: Technology Constraints; Development Workflow & Quality Gates; Governance
- Removed sections: none
- Templates reviewed for alignment:
    ✅ .specify/templates/plan-template.md (Constitution Check gate compatible — no rename needed)
    ✅ .specify/templates/spec-template.md (user-focused sections compatible)
    ✅ .specify/templates/tasks-template.md (test-discipline tasks compatible)
- Deferred TODOs: none
-->

# Drillbänken Constitution

## Core Principles

### I. Pedagogy is the Architecture

Every lesson MUST be an instance of the Swedish Armed Forces instruction loop from
*Handbok Utbildningsmetodik* (Försvarsmakten, 2013/2024):
**VISA → INSTRUERA → ÖVA (parts) → ÖVA (whole) → PRÖVA.** This loop is a typed state
machine in the domain model, not a content convention or a UI suggestion. Phase
semantics are binding:

- **VISA** — the system demonstrates the skill at full speed, no commentary: a replayed
  console session showing the target end state ("målbild").
- **INSTRUERA** — the same demonstration replayed slowly, stepwise, annotated with
  keyword-level instruction. Short, imperative, no over-explaining.
- **ÖVA (parts)** — isolated drills of each sub-step in the console, with immediate
  pass/retry feedback. Repetition until fluent is a feature, never a failure path.
- **ÖVA (whole)** — the full task end to end, self-paced; hints are optional and carry a
  score cost.
- **PRÖVA** — graded examination, no hints, against the published end requirement
  ("slutkrav"). A learner MUST NOT enter PRÖVA until the ÖVA gates are met.

The loop MUST map explicitly onto Kolb's experiential learning cycle (VISA/INSTRUERA =
abstract conceptualization via observation; ÖVA = active experimentation; PRÖVA =
concrete experience; post-grade feedback = reflective observation). The reflection step
is mandatory: after PRÖVA the learner MUST see what was graded, why, and what to drill
again. Fidelity to this method outranks feature count in every trade-off.

**Rationale**: The product's reason to exist is the instruction method. Encoding the loop
as types makes illegal phase transitions unrepresentable and keeps the pedagogy from
eroding as features accrete.

### II. The Console is the Medium

All learner interaction MUST happen in a terminal-style interface (xterm.js or
equivalent). No forms, no multiple choice, no point-and-click answer widgets. The drill
engine is first-party code: no off-the-shelf gamified console-tutorial framework exists
(Katacoda is dead; StackBlitz TutorialKit is a Node/WebContainer tutorial runner with no
grading or drill loop; swirl (R) is prior art for the interaction model only).

**Rationale**: The skill being taught is operating a real tool at a prompt; the medium
must be the tool, not a description of it.

### III. Typed Core, Scala Throughout

The frontend MUST be pure Scala 3 compiled with Scala.js and rendered with Laminar
(Airstream signals for state). No Vue, no React. The lesson state machine, grading rules
and progression model MUST live in their own pure, framework-free domain module — no
Laminar, no DOM imports — covered by property-based tests (munit + ScalaCheck). The
Laminar UI MUST be a thin adapter over that domain. All JavaScript interop (xterm.js,
DuckDB-WASM) MUST be confined to a narrow, explicitly typed facade layer.

**Rationale**: A pure, well-typed core is testable without a browser and outlives any UI
or interop choice. Isolating interop keeps the untyped JS surface small and auditable.

### IV. Static, Client-Side Deployment

The application MUST run entirely client-side and deploy as a static site to GitHub
Pages. There MUST be no backend in v1. Learner progress MUST persist locally
(localStorage or OPFS) and MUST be exportable to, and importable from, a file.

**Rationale**: No server means no accounts, no data-handling liability, zero hosting cost,
and a site that keeps working indefinitely.

### V. Content as Data

Lessons MUST be declarative artifacts — one self-contained unit per lesson holding its
demonstration script, keyword instructions, part-drills with checkers, whole-task, and
exam with rubric. Adding or editing a lesson MUST NOT require engine code changes.

**Rationale**: Authors iterate on teaching content; engineers iterate on the engine.
Separating them lets content scale without growing the codebase or its test surface.

## Technology Constraints

- **Language/UI**: Scala 3 + Scala.js; Laminar (`com.raquo::laminar`) for UI. Domain
  module is JS-target (or cross-compiled) but free of Laminar and DOM dependencies.
- **SQL engine (v1 subject)**: DuckDB-WASM, executed in the browser. The engine
  bootstrap, Apache Arrow result-materialization, seed-dataset shape, and result-equality
  checker are carried over as proven prior art from the sibling project
  *SQL Concepts Lab* (`oluies/sql-concepts-lab`) and cited, not re-derived.
- **Build**: sbt with the Scala.js plugin, integrated into Vite via the official
  `@scala-js/vite-plugin-scalajs`; `npm run build` produces a static `dist/`.
- **Tests**: munit + ScalaCheck for the domain module, runnable in CI without a browser.
- **Extensibility**: the architecture MUST allow additional subject engines later (e.g. a
  shell or kubectl simulator), but only the SQL engine ships in v1.

## Development Workflow & Quality Gates

- **Spec-driven**: features proceed through the Spec Kit flow — constitution → specify →
  clarify → plan → tasks → analyze — before implementation. Specification artifacts are
  reviewed before any application code is written.
- **Domain-first testing**: the pure domain module's behavior (phase transitions, gating,
  grading, progression) MUST be specified by tests, including property-based tests, and
  these MUST pass in CI.
- **Interop is gated**: a Scala.js + Laminar + DuckDB-WASM interop spike MUST prove the
  end-to-end build/runtime pipeline before feature work is planned in detail.
- **Decisions are recorded**: resolved architectural decisions (e.g. the rejection of Vue
  and of a hybrid split in favor of pure Scala.js + Laminar) MUST be documented in the
  spec with their rationale, not silently assumed.

## Governance

This constitution supersedes other process conventions for the project. Amendments MUST
be made by editing this file, with a Sync Impact Report recorded at its top and a version
bump per the policy below; dependent templates and guidance docs MUST be checked for
alignment in the same change.

Versioning policy (semantic):
- **MAJOR** — backward-incompatible governance or principle removal/redefinition.
- **MINOR** — a new principle/section, or materially expanded guidance.
- **PATCH** — clarifications, wording, and non-semantic refinements.

Compliance: specification, planning, and review steps MUST verify that work conforms to
these principles. Any deviation MUST be justified explicitly in the relevant artifact, or
the work MUST be brought into compliance before proceeding.

**Version**: 1.0.0 | **Ratified**: 2026-06-14 | **Last Amended**: 2026-06-14

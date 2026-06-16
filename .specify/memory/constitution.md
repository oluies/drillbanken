<!--
SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 2.0.0 (2026-06-16)
Bump rationale: MAJOR — Principle II redefined. New-user feedback showed the raw
  terminal raised the floor too high, so the interaction medium changes from a
  console to a guided web interface. The learner still authors real SQL (not
  multiple-choice); only the chrome changes. Principles I, III, IV, V unchanged.
Modified: II. The Console Is the Medium → II. A Guided Web Interface.
Interop note: xterm.js removed; SQL editing now via a CodeMirror facade. Engine
  (DuckDB-WASM) interop unchanged.
Templates/docs requiring updates: spec.md FR-008/009/010 (✅ updated), README (✅).
-----
Version change: (template, unversioned) → 1.0.0
Bump rationale: Initial ratification. Constitution populated from the five
  non-negotiable principles in docs/spec-prompt.md (MAJOR baseline → 1.0.0).

Principles (all newly defined):
  I.   Pedagogy Is the Architecture        (was [PRINCIPLE_1_NAME])
  II.  The Console Is the Medium           (was [PRINCIPLE_2_NAME])
  III. Typed Core, Scala Throughout        (was [PRINCIPLE_3_NAME])
  IV.  Static, Client-Side Deployment      (was [PRINCIPLE_4_NAME])
  V.   Content as Data                     (was [PRINCIPLE_5_NAME])

Added sections:
  - Technology Constraints (was [SECTION_2_NAME])
  - Development Workflow & Quality Gates (was [SECTION_3_NAME])
  - Governance (populated)

Removed sections: none.

Templates requiring updates:
  ✅ .specify/templates/plan-template.md   — generic "Constitution Check" gate; compatible, no edit required
  ✅ .specify/templates/spec-template.md   — generic; compatible, no edit required
  ✅ .specify/templates/tasks-template.md  — generic; compatible, no edit required
  ✅ README.md                             — consistent with principles, no edit required

Follow-up TODOs: none. Ratification date set to first adoption (2026-06-14).
-->

# Drillbänken Constitution

## Core Principles

### I. Pedagogy Is the Architecture

Every lesson MUST be an instance of the Swedish Armed Forces instruction loop from
*Handbok Utbildningsmetodik* (Försvarsmakten): **VISA → INSTRUERA → ÖVA (parts) →
ÖVA (whole) → PRÖVA**. This loop is a typed state machine, not a content convention
— phases, gates, and transitions are encoded in types, not left to lesson authors'
discretion. Phase semantics are fixed:

- **VISA** — the system demonstrates the skill at full speed, no commentary: a
  replayed console session showing the target end state ("målbild").
- **INSTRUERA** — the same demonstration replayed slowly, stepwise, annotated with
  keyword-level instruction. Short, imperative, no over-explaining.
- **ÖVA (parts)** — isolated drills of each sub-step with immediate pass/retry
  feedback. Repetition until fluent is a feature, NEVER a failure path.
- **ÖVA (whole)** — the full task end to end, self-paced, with optional hints that
  carry a score cost.
- **PRÖVA** — graded examination, no hints, against the published end requirement
  ("slutkrav"). The learner MUST NOT enter PRÖVA until the ÖVA gates are met.

The loop MUST map explicitly onto Kolb's experiential learning cycle
(VISA/INSTRUERA = abstract conceptualization via observation; ÖVA = active
experimentation; PRÖVA = concrete experience; post-grade feedback = reflective
observation). The reflection step is mandatory: after PRÖVA the learner MUST see
what was graded, why, and what to drill again.

**Rationale:** Fidelity to the Handbok Utbildningsmetodik method is the product.
Encoding the loop as a typed state machine guarantees every lesson honors it and
makes deviation a compile error rather than a review comment.

### II. A Guided Web Interface

Learner interaction MUST happen in a structured, accessible web GUI: visible phase
and progress, instruction text, a SQL editor, a Run action, results shown as a
table, and explicit Hint/Replay/Next controls. The learner MUST still author real
SQL (typed into the editor) — no multiple-choice — so the rigor of writing SQL is
preserved; what changes versus a raw terminal is the surrounding guidance. The drill
engine remains first-party code; no off-the-shelf tutorial framework is adopted.

**Rationale (amended 2026-06-16, v2.0.0):** A bare terminal raised the floor too
high for new users. A guided GUI lowers the barrier to entry while keeping the
learner writing genuine SQL against the real engine. The demonstration
(VISA/INSTRUERA) is replayed in the GUI rather than a terminal.

### III. Typed Core, Scala Throughout

The frontend MUST be pure Scala 3 compiled with Scala.js and rendered with Laminar
(Airstream signals for state). No Vue, no React. The lesson state machine, grading
rules, and progression model MUST live in a pure, framework-free domain module with
property-based tests (munit + ScalaCheck); the Laminar UI is a thin adapter over it.
JavaScript interop (CodeMirror, DuckDB-WASM) MUST be confined to a narrow, explicitly
typed facade layer — the domain module MUST contain no Laminar or DOM imports.

**Rationale:** A framework-free typed core is independently testable, makes the
pedagogy invariants (Principle I) provable with ScalaCheck, and isolates JS-interop
churn behind a boundary.

### IV. Static, Client-Side Deployment

The application MUST run entirely client-side and deploy to GitHub Pages with no
backend. SQL is executed in the browser by DuckDB-WASM. Progress MUST persist
locally (localStorage or OPFS) and be exportable as a file. No server-side state,
no accounts.

**Rationale:** A static, backendless deployment keeps the project free to host,
private by default, and removes server operations from the cost of adding content.

### V. Content as Data

Lessons MUST be declarative artifacts — one self-contained unit per lesson holding
the demonstration script, keyword instructions, part-drills with checkers,
whole-task, and exam rubric. Adding or editing a lesson MUST require no engine
changes.

**Rationale:** Separating content from the engine lets the curriculum grow without
touching the typed core, and keeps lesson authoring open to contributors who are not
modifying the state machine.

## Technology Constraints

- **Language/UI:** Scala 3 + Scala.js; Laminar (`com.raquo::laminar`) for UI.
- **Domain module:** pure Scala, free of Laminar and DOM imports; tested with
  munit + ScalaCheck, runnable in CI.
- **SQL engine:** DuckDB-WASM, bundled via Vite `?url` assets — NO runtime CDN
  dependency. Arrow results are materialized to a flat `QueryResult { cols, rows }`.
- **Build:** sbt + the Scala.js plugin, integrated into Vite via
  `@scala-js/vite-plugin-scalajs`; `npm run build` produces a static `dist/`.
- **Interop:** CodeMirror (SQL editor) and DuckDB-WASM are reached only through the
  narrow typed facade (Principle III).
- **v1 subject domain:** SQL only. The architecture MUST allow other engines later
  (e.g. a shell or kubectl simulator) but only SQL ships in v1.
- **Out of scope for v1:** accounts, server sync, multi-subject content, and
  AI-generated feedback. Privacy-first cookieless analytics is OPTIONAL and, if
  included, MUST be a no-op under DNT/GPC and MUST never transmit learner SQL.

## Development Workflow & Quality Gates

- **Specification first:** features flow through the Spec Kit pipeline
  (constitution → specify → clarify → plan → tasks → analyze) before
  implementation. Pedagogy-model questions are raised, not assumed.
- **Interop spike gates feature work:** the first plan milestone MUST be a minimal
  Scala.js + Laminar app proving the sbt + vite-plugin-scalajs + `?url` asset +
  worker pipeline end to end (DuckDB-WASM boots, reports its version, one query
  renders, one deliberate error surfaces as a typed value) before detailed feature
  planning.
- **Tests:** the domain module's pedagogy and grading invariants MUST have
  property-based coverage; checkers MUST be tested against order-sensitive and
  order-insensitive cases.
- **Proven prior art is reused, not re-derived:** the DuckDB-WASM bootstrap, Arrow
  materialization, seed-dataset shape, and result-equality checker from
  `sql-concepts-lab` are treated as settled context and cited.

## Governance

This constitution supersedes other practices for the Drillbänken project. All plans,
specs, and task sets MUST demonstrate compliance with the five Core Principles; any
deviation MUST be justified in writing in the relevant artifact, or the artifact is
non-compliant.

**Amendment procedure:** amendments are proposed as edits to this file with a Sync
Impact Report, are reviewed alongside the artifacts they affect, and take effect
when merged.

**Versioning policy** (semantic):
- **MAJOR** — backward-incompatible governance changes, or removal/redefinition of
  a principle.
- **MINOR** — a new principle or section, or materially expanded guidance.
- **PATCH** — clarifications, wording, and non-semantic refinements.

**Compliance review:** every Spec Kit `analyze` run MUST check artifacts against
this constitution and report inconsistencies.

**Version**: 2.0.0 | **Ratified**: 2026-06-14 | **Last Amended**: 2026-06-16

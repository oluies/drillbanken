# Contract: PersistenceService (progress + export/import)

Browser-local persistence with no backend. Lives in `modules/app` (uses the DOM Storage
API behind a narrow service; the domain `ProgressState` model stays pure). — Principle IV,
FR-021, FR-022, FR-023

## Interface

```
trait PersistenceService:
  def load(): ProgressState                          // returns a clean first-run state if none
  def save(state: ProgressState): Unit               // persists to localStorage
  def export(state: ProgressState): String           // JSON blob (with schemaVersion)
  def importFrom(json: String): Either[ImportError, ProgressState]

ImportError = BadJson | UnsupportedSchema(found: Int, expected: Int) | Invalid(reason: String)
```

## Behavioral contract

- **Storage**: `localStorage` key (e.g. `drillbanken.progress.v1`); OPFS only if blobs
  outgrow localStorage. No cookies. — FR-021
- **First run**: `load()` with no stored state returns a clean `ProgressState` with only
  the lowest-`sequence` lesson unlocked and the default language. — Edge case, FR-015
- **Resume**: `load()` restores `LessonProgress.resume` so an in-progress lesson reopens at
  the same phase/sub-step with attempts/hints/score intact. — FR-022, SC-006
- **Export**: serializes the full `ProgressState` to JSON including `schemaVersion`. — FR-023
- **Import**: validates JSON, then `schemaVersion`, then structure. On any failure returns
  an `ImportError` and **does not mutate** the currently loaded/stored state; on success
  returns the imported `ProgressState` for the app to adopt and save. — FR-023, SC-007
- **Privacy**: nothing is transmitted off-device; export/import is user-driven file I/O.
  — FR-024, FR-025

## Acceptance

- Make progress → `export` → clear storage → `importFrom(exported)` restores unlocks,
  grades, streak, insignia identically. — SC-007
- `importFrom("{ not json")` → `Left(BadJson)`, existing state unchanged. — FR-023
- `importFrom` of a wrong-`schemaVersion` blob → `Left(UnsupportedSchema(...))`, unchanged.

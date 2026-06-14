# Contract — Lesson artifact schema

One self-contained JSON file per lesson under `content/lessons/`. Validated at load; a
malformed lesson is reported and skipped without breaking others (FR-026/028).

`LocalizedString` = `{ "sv": string, "en"?: string }` — `sv` (default) is REQUIRED; missing
`en` falls back to `sv` (D5/FR-032).

```jsonc
{
  "id": "select-basics",            // stable, unique
  "order": 1,                        // curriculum position
  "title": { "sv": "…", "en": "…" },
  "slutkrav": { "sv": "…", "en": "…" },   // what PRÖVA grades against

  "dataset": "trading-book",        // named seed (or inline { "ddl": ["…"] })

  "demonstration": {                 // VISA: pre-recorded timed transcript (D4)
    "frames": [ { "atMs": 0, "output": "duckdb> SELECT …" }, … ]
  },

  "instruera": [                     // slow annotated replay
    { "keyword": "SELECT", "instruction": { "sv": "…", "en": "…" }, "atMs": 0 }, …
  ],

  "partDrills": [
    {
      "id": "pd-projection",         // referenced by rubric.subSkills + drillAgain
      "prompt": { "sv": "…", "en": "…" },
      "task": {
        "referenceSolution": "SELECT a, b FROM t",
        "ordered": false,
        "shapeConstraint": null      // optional SQL-shape rule
      }
    }, …
  ],

  "wholeTask": {
    "prompt": { "sv": "…", "en": "…" },
    "task": { "referenceSolution": "…", "ordered": false },
    "hints": [ { "text": { "sv": "…", "en": "…" }, "cost": 5 } ]   // ÖVA-whole only
  },

  "exam": {                          // PRÖVA — no hints
    "task": { "referenceSolution": "…", "ordered": true },
    "rubric": {
      "subSkills": [
        { "id": "ss-projection", "partDrillId": "pd-projection", "points": 40,
          "criterion": { "sv": "…", "en": "…" } }, …
      ],
      "attemptPenalty": 0,
      "hintPenalty": 0,
      "timeBox": null,               // ms, optional; null = untimed
      "gradeThresholds": { "G": 60, "VG": 85 }   // points → grade; below G = IG
    }
  }
}
```

**Validation rules**

- `id` unique across loaded lessons; `order` present.
- Every localized field has a `sv` variant.
- ≥1 `partDrills` entry; `exam` present with ≥1 `subSkills`.
- Every `rubric.subSkills[].partDrillId` references an existing `partDrills[].id`
  (drives "drill again" routing).
- `gradeThresholds` monotonic and within reachable points.
- On any failure: report `{ lessonFile, errors[] }`; do not load the lesson; continue others.

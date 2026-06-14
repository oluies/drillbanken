# Contract — Progress persistence & export

Progress + preferences persist in `localStorage` under a single namespaced key, and
export/import as one JSON document. A `schemaVersion` guards incompatible imports
(FR-023/024/025, D9).

```jsonc
{
  "schemaVersion": 1,
  "locale": "sv",                    // "sv" | "en" — persisted preference (FR-033)
  "perLesson": {
    "select-basics": {
      "phase": "OvaParts",           // Visa|Instruera|OvaParts|OvaWhole|Prova
      "stepIndex": 2,                // position within the phase
      "attempts": [ { "target": "pd-projection", "outcome": "retry" }, … ],
      "hintsUsed": 0,
      "bestGrade": { "value": "G", "points": 72 },   // optional
      "status": "InProgress"         // Locked | InProgress | Completed
    }
  },
  "streak": 3,                       // derived from completed PRÖVA only (FR-021)
  "insignia": ["first-vg"]
}
```

**Rules**

- `status = Completed` requires a passed PRÖVA; lesson N unlocks iff N-1 is `Completed`
  (FR-020).
- **Export**: serialize the whole document to a downloadable file.
- **Import**: parse + validate against `schemaVersion`; on mismatch or invalid JSON, reject
  with a clear message and **leave existing progress intact** (FR-025).
- **Storage unavailable/full/cleared**: degrade gracefully; inform the learner that progress
  may not be saved (Edge Cases).
- ÖVA attempts recorded for routing/feedback but never affect grade (FR-019).

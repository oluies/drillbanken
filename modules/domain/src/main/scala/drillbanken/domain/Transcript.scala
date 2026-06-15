package drillbanken.domain

/** VISA/INSTRUERA demonstration data (FR-002, research.md D9). Pure domain data so both
  * the `content` DSL and the `console` replay can share it without a content↔console dep.
  */

/** Authored expected output for a demo step (not executed live). */
final case class QueryResultView(cols: List[String], rows: List[List[Option[String]]])

/** One step of a demonstration. `annotation` is shown only in INSTRUERA (stepwise);
  * `delayMs` drives VISA timing.
  */
final case class TranscriptStep(
    input: String,
    output: QueryResultView,
    delayMs: Int,
    annotation: Option[LocalizedText] = None
)

final case class Transcript(steps: List[TranscriptStep])

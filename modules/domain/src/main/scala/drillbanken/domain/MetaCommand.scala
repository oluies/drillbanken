package drillbanken.domain

/** Console meta-commands (FR-010). Anything that is not a recognized meta-command is
  * treated as SQL (FR-009). Pure + testable; parsing lives in the domain.
  */
enum MetaCommand:
  case Help, Hint, Progress, RepeatDemo, Abort, Lang, Tables
  case Describe(table: String)

/** A parsed console line. */
enum ConsoleInput:
  case Meta(command: MetaCommand)
  case Sql(text: String)

object MetaCommand:
  /** Recognized spellings (case-insensitive, optional leading `:` or `\`). Schema-explore
    * commands (`tables`, `describe <t>`) are read-only and don't affect the drill.
    */
  def parse(line: String): ConsoleInput =
    val trimmed = line.trim
    val lower = trimmed.toLowerCase
    val token = lower.stripPrefix(":").stripPrefix("\\")
    if token == "tables" || lower == "show tables" then ConsoleInput.Meta(Tables)
    else if lower.startsWith("describe ") then ConsoleInput.Meta(Describe(trimmed.substring(9).trim))
    else if lower.startsWith("desc ") then ConsoleInput.Meta(Describe(trimmed.substring(5).trim))
    else
      token match
        case "help"                   => ConsoleInput.Meta(Help)
        case "hint"                   => ConsoleInput.Meta(Hint)
        case "progress"               => ConsoleInput.Meta(Progress)
        case "repeat-demo" | "repeat" => ConsoleInput.Meta(RepeatDemo)
        case "abort" | "quit"         => ConsoleInput.Meta(Abort)
        case "lang" | "language"      => ConsoleInput.Meta(Lang)
        case "tables" | "schema"      => ConsoleInput.Meta(Tables)
        case _                        => ConsoleInput.Sql(trimmed)

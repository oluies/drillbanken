package drillbanken.domain

/** Console meta-commands (FR-010). Anything that is not a recognized meta-command is
  * treated as SQL (FR-009). Pure + testable; parsing lives in the domain.
  */
enum MetaCommand:
  case Help, Hint, Progress, RepeatDemo, Abort

/** A parsed console line. */
enum ConsoleInput:
  case Meta(command: MetaCommand)
  case Sql(text: String)

object MetaCommand:
  /** Recognized spellings (case-insensitive, optional leading `:` or `\`). */
  def parse(line: String): ConsoleInput =
    val trimmed = line.trim
    val token = trimmed.stripPrefix(":").stripPrefix("\\").toLowerCase
    token match
      case "help"                   => ConsoleInput.Meta(Help)
      case "hint"                   => ConsoleInput.Meta(Hint)
      case "progress"               => ConsoleInput.Meta(Progress)
      case "repeat-demo" | "repeat" => ConsoleInput.Meta(RepeatDemo)
      case "abort" | "quit"         => ConsoleInput.Meta(Abort)
      case _                        => ConsoleInput.Sql(trimmed)

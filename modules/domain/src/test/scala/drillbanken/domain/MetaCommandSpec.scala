package drillbanken.domain

/** T020 — meta-command parsing (FR-009, FR-010). */
class MetaCommandSpec extends munit.FunSuite:

  test("recognized meta-commands parse (case-insensitive, optional prefix)"):
    assertEquals(MetaCommand.parse("help"), ConsoleInput.Meta(MetaCommand.Help))
    assertEquals(MetaCommand.parse("  HINT "), ConsoleInput.Meta(MetaCommand.Hint))
    assertEquals(MetaCommand.parse(":progress"), ConsoleInput.Meta(MetaCommand.Progress))
    assertEquals(MetaCommand.parse("repeat-demo"), ConsoleInput.Meta(MetaCommand.RepeatDemo))
    assertEquals(MetaCommand.parse("\\abort"), ConsoleInput.Meta(MetaCommand.Abort))
    assertEquals(MetaCommand.parse("lang"), ConsoleInput.Meta(MetaCommand.Lang))
    assertEquals(MetaCommand.parse("LANGUAGE"), ConsoleInput.Meta(MetaCommand.Lang))
    assertEquals(MetaCommand.parse("tables"), ConsoleInput.Meta(MetaCommand.Tables))
    assertEquals(MetaCommand.parse("show tables"), ConsoleInput.Meta(MetaCommand.Tables))
    assertEquals(MetaCommand.parse("describe traders"), ConsoleInput.Meta(MetaCommand.Describe("traders")))
    assertEquals(MetaCommand.parse("DESC  trades"), ConsoleInput.Meta(MetaCommand.Describe("trades")))

  test("anything else is SQL (trimmed, original case preserved)"):
    assertEquals(MetaCommand.parse("  SELECT 1  "), ConsoleInput.Sql("SELECT 1"))
    assertEquals(
      MetaCommand.parse("select name from traders"),
      ConsoleInput.Sql("select name from traders")
    )
    // a word that merely contains a command name is still SQL
    assertEquals(MetaCommand.parse("helper"), ConsoleInput.Sql("helper"))

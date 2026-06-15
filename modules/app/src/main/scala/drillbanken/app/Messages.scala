package drillbanken.app

import drillbanken.domain.*

/** Bilingual UI chrome strings (FR-027). Lesson prose lives in the lesson definitions;
  * this is the controller/console chrome. Phase names stay Swedish (domain terms).
  */
object Messages:
  val visaIntro = LocalizedText(
    sv = "VISA — så här ser målet ut (full fart). Tryck Enter för INSTRUERA.",
    en = "VISA — this is the target (full speed). Press Enter for INSTRUERA."
  )
  val instrueraIntro = LocalizedText(
    sv = "INSTRUERA — samma sak, långsamt och förklarat. Tryck Enter för att öva.",
    en = "INSTRUERA — the same, slowly and explained. Press Enter to practise."
  )
  val ovaPartsIntro = LocalizedText(
    sv = "ÖVA (delar) — öva varje steg. Skriv SQL. (help för kommandon)",
    en = "ÖVA (parts) — drill each step. Type SQL. (help for commands)"
  )
  val ovaWholeIntro = LocalizedText(
    sv = "ÖVA (helhet) — gör hela uppgiften. hint kostar poäng. Tryck Enter.",
    en = "ÖVA (whole) — do the whole task. hint costs points. Press Enter."
  )
  val provaIntro = LocalizedText(
    sv = "PRÖVA — prov mot slutkravet. Inga tips.",
    en = "PRÖVA — exam against the end requirement. No hints."
  )
  val drillPass = LocalizedText(sv = "Rätt!", en = "Correct!")
  val drillFail = LocalizedText(sv = "Inte rätt än — försök igen.", en = "Not right yet — try again.")
  val pressEnter = LocalizedText(sv = "[Enter] för att fortsätta", en = "[Enter] to continue")
  val noHints = LocalizedText(sv = "Inga fler tips.", en = "No more hints.")
  val hintInProva = LocalizedText(sv = "Tips är avstängda i PRÖVA.", en = "Hints are disabled in PRÖVA.")
  val demoInProva = LocalizedText(sv = "Kan inte repetera demo i PRÖVA.", en = "Cannot replay the demo in PRÖVA.")
  val reflectIntro = LocalizedText(sv = "Reflektion:", en = "Reflection:")
  val drillAgain = LocalizedText(sv = "Öva igen:", en = "Drill again:")
  val passedExam = LocalizedText(sv = "Godkänd!", en = "Passed!")
  val failedExam = LocalizedText(sv = "Underkänd — öva på delarna nedan.", en = "Failed — drill the parts below.")
  val helpText = LocalizedText(
    sv = "Kommandon: help, hint, progress, repeat-demo, abort. Allt annat körs som SQL.",
    en = "Commands: help, hint, progress, repeat-demo, abort. Anything else runs as SQL."
  )
  val aborted = LocalizedText(sv = "Avbrutet.", en = "Aborted.")

  def points(p: Int, lang: Language): String =
    if lang == Language.Sv then s"Poäng: $p" else s"Points: $p"

  def progress(phase: Phase, lang: Language): String =
    val name = phase.toString
    if lang == Language.Sv then s"Fas: $name" else s"Phase: $name"

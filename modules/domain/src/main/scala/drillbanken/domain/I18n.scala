package drillbanken.domain

/** UI/content language. SQL keywords and the phase names stay untranslated (FR-027). */
enum Language:
  case Sv, En

/** Bilingual string; every learner-facing piece of content carries both languages (FR-027). */
final case class LocalizedText(sv: String, en: String):
  def apply(lang: Language): String = lang match
    case Language.Sv => sv
    case Language.En => en

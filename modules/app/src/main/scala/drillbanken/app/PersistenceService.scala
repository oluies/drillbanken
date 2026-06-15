package drillbanken.app

import drillbanken.domain.*
import drillbanken.domain.loop.{LessonState, PartResult, LessonStatus}
import drillbanken.domain.progress.{ProgressState, LessonProgress}
import upickle.default.*
import org.scalajs.dom

/** Why an import was rejected (contracts/persistence.md). */
enum ImportError:
  case BadJson
  case UnsupportedSchema(found: Int, expected: Int)

/** Pure JSON codec for ProgressState (no DOM — testable on node). */
object ProgressCodec:
  given ReadWriter[Phase] = readwriter[String].bimap[Phase](_.toString, Phase.valueOf)
  given ReadWriter[LessonStatus] = readwriter[String].bimap[LessonStatus](_.toString, LessonStatus.valueOf)
  given ReadWriter[Language] = readwriter[String].bimap[Language](_.toString, Language.valueOf)
  given ReadWriter[LessonId] = readwriter[String].bimap[LessonId](_.value, LessonId(_))
  given ReadWriter[PartId] = readwriter[String].bimap[PartId](_.value, PartId(_))
  given ReadWriter[PartResult] = macroRW
  given ReadWriter[ScoreLine] = macroRW
  given ReadWriter[Grade] = macroRW
  given ReadWriter[LessonState] = macroRW
  given ReadWriter[LessonProgress] = macroRW
  given ReadWriter[ProgressState] = macroRW

  def encode(s: ProgressState): String = write(s)

  def decode(json: String): Either[ImportError, ProgressState] =
    val parsed =
      try Right(read[ProgressState](json))
      catch case _: Throwable => Left(ImportError.BadJson)
    parsed.flatMap { st =>
      if st.schemaVersion != ProgressState.SchemaVersion then
        Left(ImportError.UnsupportedSchema(st.schemaVersion, ProgressState.SchemaVersion))
      else Right(st)
    }

/** Browser-local persistence (FR-021/022/023). Pure codec lives in [[ProgressCodec]]. */
final class PersistenceService(key: String = "drillbanken.progress.v1"):
  private def store = dom.window.localStorage

  def load(firstLesson: LessonId, lang: Language): ProgressState =
    Option(store.getItem(key))
      .flatMap(s => ProgressCodec.decode(s).toOption)
      .getOrElse(ProgressState.initial(firstLesson, lang))

  def save(s: ProgressState): Unit = store.setItem(key, ProgressCodec.encode(s))
  def clear(): Unit = store.removeItem(key)

  def exportJson(s: ProgressState): String = ProgressCodec.encode(s)
  def importFrom(json: String): Either[ImportError, ProgressState] = ProgressCodec.decode(json)

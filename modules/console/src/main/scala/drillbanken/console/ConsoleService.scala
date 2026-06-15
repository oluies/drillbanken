package drillbanken.console

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js
import drillbanken.domain.{TranscriptStep, Language}

/** VISA = full speed (no annotations); INSTRUERA = stepwise (annotations shown). */
enum Speed:
  case FullSpeed, Stepwise

/** Narrow console contract over xterm.js (contracts/console-service.md, Principle II). */
trait ConsoleService:
  def open(mount: dom.Element): Unit
  def write(text: String): Unit
  def writeLine(text: String): Unit
  def clear(): Unit
  def prompt(label: String): Unit

  /** Replay a demonstration transcript (FR-002, US4). `lang` selects annotation language. */
  def replay(steps: List[TranscriptStep], speed: Speed, lang: Language): Unit

  /** Emits a full line whenever the learner presses Enter (FR-008). */
  def onSubmit: EventStream[String]

/** xterm.js-backed console (T013). Line-oriented: buffers keystrokes and emits on Enter;
  * handles Backspace. Replay (T047) is added with the VISA work.
  */
final class XtermConsoleService extends ConsoleService:
  private val term = new Terminal()
  private val submitBus = new EventBus[String]
  private val buffer = new StringBuilder
  private var promptLabel = "> "

  term.onData { (data: String) =>
    data match
      case "\r" => // Enter
        term.write("\r\n")
        val line = buffer.toString
        buffer.clear()
        submitBus.writer.onNext(line)
        term.write(promptLabel)
      case "" => // Backspace (DEL)
        if buffer.nonEmpty then
          buffer.deleteCharAt(buffer.length - 1)
          term.write("\b \b")
      case ch if ch.nonEmpty && ch.charAt(0) >= ' ' =>
        buffer.append(ch)
        term.write(ch)
      case _ => () // ignore other control sequences
  }

  def open(mount: dom.Element): Unit =
    term.open(mount)
    term.write(promptLabel)
    term.focus()

  def write(text: String): Unit = term.write(text)
  def writeLine(text: String): Unit = term.writeln(text)
  def clear(): Unit = term.clear()

  def replay(steps: List[TranscriptStep], speed: Speed, lang: Language): Unit =
    steps.foreach { step =>
      term.writeln(s"sql> ${step.input}")
      term.writeln(step.output.cols.mkString(" | "))
      step.output.rows.foreach(r => term.writeln(r.map(_.getOrElse("∅")).mkString(" | ")))
      if speed == Speed.Stepwise then step.annotation.foreach(a => term.writeln(a(lang)))
    }
  def prompt(label: String): Unit =
    promptLabel = label
    term.write(label)

  def onSubmit: EventStream[String] = submitBus.events

package drillbanken.console

import com.raquo.laminar.api.L.*
import org.scalajs.dom
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

/** xterm.js-backed console (T013) with a cursor-aware line editor: insert/backspace at the
  * cursor, ←/→ to move, Home/End, Delete. Emits the full line on Enter.
  */
final class XtermConsoleService extends ConsoleService:
  private val term = new Terminal()
  private val submitBus = new EventBus[String]
  private val buffer = new StringBuilder
  private var cursor = 0 // 0..buffer.length
  private var promptLabel = "> "

  // ANSI helpers
  private def left(n: Int): String = if n > 0 then s"[${n}D" else ""
  private def right(n: Int): String = if n > 0 then s"[${n}C" else ""

  private def insert(s: String): Unit =
    val tail = buffer.substring(cursor)
    buffer.insert(cursor, s)
    term.write(s + tail) // print inserted text + the existing tail, then reposition
    term.write(left(tail.length))
    cursor += s.length

  private def backspace(): Unit =
    if cursor > 0 then
      buffer.deleteCharAt(cursor - 1)
      cursor -= 1
      val tail = buffer.substring(cursor)
      term.write("\b" + tail + " " + left(tail.length + 1))

  private def deleteAtCursor(): Unit =
    if cursor < buffer.length then
      buffer.deleteCharAt(cursor)
      val tail = buffer.substring(cursor)
      term.write(tail + " " + left(tail.length + 1))

  private def moveLeft(): Unit =
    if cursor > 0 then
      cursor -= 1
      term.write(left(1))

  private def moveRight(): Unit =
    if cursor < buffer.length then
      cursor += 1
      term.write(right(1))

  private def moveHome(): Unit =
    if cursor > 0 then
      term.write(left(cursor))
      cursor = 0

  private def moveEnd(): Unit =
    val d = buffer.length - cursor
    if d > 0 then
      term.write(right(d))
      cursor = buffer.length

  private def submit(): Unit =
    term.write("\r\n")
    val line = buffer.toString
    buffer.clear()
    cursor = 0
    submitBus.writer.onNext(line)
    term.write(promptLabel)

  term.onData { (data: String) =>
    data match
      case "\r" | "\n"             => submit()
      case "" | "\b"         => backspace()
      case "[D" | "OD" => moveLeft()
      case "[C" | "OC" => moveRight()
      case "[3~"             => deleteAtCursor()
      case "[H" | "OH" | "" => moveHome() // Home / Ctrl-A
      case "[F" | "OF" | "" => moveEnd()  // End / Ctrl-E
      case s if s.nonEmpty && !s.exists(_ < ' ') => insert(s) // printable (incl. paste)
      case _                       => () // ignore other escapes (↑/↓, etc.)
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
    // NB: do not reset the cursor/buffer here — submit() already does, and a late prompt()
    // (after an async drill check) must not disturb input the learner has already started.
    promptLabel = label
    term.write(label)

  def onSubmit: EventStream[String] = submitBus.events

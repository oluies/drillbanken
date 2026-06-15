package drillbanken.console

import com.raquo.laminar.api.L.*
import org.scalajs.dom

/** Narrow console contract over xterm.js (contracts/console-service.md, Principle II).
  * The interop spike implements this:
  *   - TODO(T010): ScalablyTyped facade for `@xterm/xterm`
  *   - TODO(T013): open/write/clear/prompt + onSubmit line stream
  *   - TODO(T047): replay(steps, speed) for VISA (full speed) / INSTRUERA (stepwise)
  */
trait ConsoleService:
  def open(mount: dom.Element): Unit
  def write(text: String): Unit
  def writeLine(text: String): Unit
  def clear(): Unit
  def prompt(label: String): Unit

  /** Emits a full line whenever the learner presses Enter (FR-008). */
  def onSubmit: EventStream[String]

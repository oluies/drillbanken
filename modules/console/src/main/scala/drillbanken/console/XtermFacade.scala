package drillbanken.console

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import org.scalajs.dom

/** Minimal hand-written facade for `@xterm/xterm` — only the surface ConsoleService uses
  * (T010). The xterm CSS is imported in `main.ts` on the Vite side.
  */
@js.native
@JSImport("@xterm/xterm", "Terminal")
class Terminal(options: js.UndefOr[js.Object] = js.native) extends js.Object:
  def open(parent: dom.Element): Unit = js.native
  def write(data: String): Unit = js.native
  def writeln(data: String): Unit = js.native
  def clear(): Unit = js.native
  def focus(): Unit = js.native
  /** Fires with raw input data (keystrokes); returns a disposable we ignore. */
  def onData(handler: js.Function1[String, Unit]): js.Any = js.native

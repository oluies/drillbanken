package drillbanken.app

import com.raquo.laminar.api.L.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

/** Minimal hand-written facade for CodeMirror 6 (constitution v2.0.0 — replaces xterm).
  * Only the surface the SQL editor uses.
  */
@js.native
@JSImport("codemirror", "EditorView")
class EditorView(config: js.Object) extends js.Object:
  val state: js.Dynamic = js.native
  def dispatch(tr: js.Object): Unit = js.native
  def focus(): Unit = js.native
  def destroy(): Unit = js.native

object CodeMirror:
  @js.native @JSImport("codemirror", "basicSetup")
  val basicSetup: js.Any = js.native
  @js.native @JSImport("@codemirror/lang-sql", "sql")
  def sql(): js.Any = js.native

/** A CodeMirror-backed SQL editor as a Laminar element. Created on mount, destroyed on
  * unmount; exposes read/clear/focus.
  */
final class SqlEditor:
  private var view: js.UndefOr[EditorView] = js.undefined

  val node: Element =
    div(
      cls := "sql-editor",
      onMountCallback { ctx =>
        val cfg = js.Dynamic
          .literal(
            doc = "",
            parent = ctx.thisNode.ref,
            extensions = js.Array(CodeMirror.basicSetup, CodeMirror.sql())
          )
          .asInstanceOf[js.Object]
        view = new EditorView(cfg)
      },
      onUnmountCallback { _ =>
        view.foreach(_.destroy())
        view = js.undefined
      }
    )

  /** Current editor contents. */
  def value: String =
    view.map(v => v.state.doc.applyDynamic("toString")().asInstanceOf[String]).getOrElse("")

  def setValue(text: String): Unit =
    view.foreach { v =>
      val len = v.state.doc.length.asInstanceOf[Int]
      val tr = js.Dynamic
        .literal(changes = js.Dynamic.literal(from = 0, to = len, insert = text))
        .asInstanceOf[js.Object]
      v.dispatch(tr)
    }

  def clear(): Unit = setValue("")
  def focus(): Unit = view.foreach(_.focus())

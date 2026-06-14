# Contract: ConsoleService

The narrow Scala API over xterm.js. Owns the xterm facade (ScalablyTyped) behind it; the
app sees only this surface. Lives in `modules/console`. — Principle II, FR-008

## Interface

```
trait ConsoleService:
  def open(mount: dom.Element): Unit               // attach the terminal to the DOM node
  def write(text: String): Unit                    // print output / prompts (ANSI allowed)
  def writeLine(text: String): Unit
  def clear(): Unit
  def onSubmit: EventStream[String]                // emits a full line when the user hits Enter
  def prompt(label: String): Unit                  // render the input prompt
  def replay(steps: List[TranscriptStep], speed: Speed): Future[Unit]  // VISA/INSTRUERA — D9
```

```
Speed = FullSpeed | Stepwise            // FullSpeed = VISA, Stepwise = INSTRUERA (with annotations)
```

## Behavioral contract

- **Input model**: all learner input is line-oriented text submitted on Enter; emitted on
  `onSubmit`. No forms, no multiple-choice. — FR-008
- **Command routing** (performed by the app, not this service): a submitted line beginning
  with a recognized meta-command (`help`, `hint`, `progress`, `repeat-demo`, `abort`) is a
  `MetaCommand`; otherwise it is SQL passed to `EngineService.exec`. — FR-009, FR-010
- **replay**: renders a `Transcript` with per-step timing. `FullSpeed` ignores annotations
  (VISA); `Stepwise` shows each step's `annotation` and waits for the learner to advance
  (INSTRUERA). `replay` is refused/short-circuited if invoked during `Prova`. — FR-002,
  FR-010, US4
- **Result rendering**: `QueryResult` is rendered as an aligned text table in the console;
  errors render as readable error lines. — FR-009, Edge case (invalid SQL)

## Acceptance (Milestone 0 spike)

- A query result renders as a readable table in the terminal.
- A submitted line round-trips through `onSubmit`.
- (Feature phase) `replay` of a two-step transcript shows both steps at FullSpeed and,
  at Stepwise, surfaces annotations.

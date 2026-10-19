# Completion Report: `cli-start` (Starting Work in the Terminal)

Companion to the execution plan [`cli-start-plan.md`](implemented/cli-start-plan.md), worked under
[`doc/agy/runbook.md`](runbook.md).
Branch: `feat/cli-start`; worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-cli-start`.

---

## 1. Executive Summary

This work package implements the terminal client of *starting work*: the direct verbs `plan add`,
`epic add`, and `recipe run`, their interactive dialogue, the `--no-input` gate, the dynamic source
list with its screening and failure states, epic adoption, the third-step decomposition review, and
the start chat. It sits on the `start-contract` and `control-contract` records and speaks the same
contract the web client will use.

The dialogue is one engine (`StartDialogue`) over the entry resource:

- `plan add` / `epic add` / `recipe run` open an entry under the project, apply every answer the
  command line carries, and ask only for what is missing.
- A state without an answer stops a `--no-input` run with exit code `3`, a text line naming the
  state and what it needs, and a compact JSON line naming the entry.
- Sources are listed with their glyph, kind, reference, state, trust, and — where they apply — the
  cause, the flagged spans as neutralized plain text, the rate limit's `resets_at` epoch second, and
  a pull request's changed-file count.
- Only the kinds the server offers are shown; the reasons of the withheld kinds (including
  `landing.files` withholding the local changes) are printed once.
- Document copy/move is asked only while the server says the machine's mode is `ask`, with `copy`
  preselected; `--copy` / `--move` answer it for one run.
- An epic's adoptable ledger is offered and adopted; a failed adoption prints its cause with
  *try again*, *back*, and *abandon*.
- After an epic is created, the decomposition draft is shown with its checks; a plan opens for its
  detail, the operator's words rewrite it, the read-back shows old against new, and *Enter* agrees
  the cut once every check passes. The parallelization scope is never asked.
- `h` opens the start chat at the source list once a source is stored and every outside source is
  screened; the role is routed by the kind (`refine` / `decompose`), a proposed description change
  is read back with accept/reject, and an empty line ends the chat.

---

## 2. Delivered Artifacts

### 2.1 `pm-operator` — production

| File | Role |
|---|---|
| `StartDialogue.java` (new) | The starting dialogue: entry opening/resumption, source gathering, documents mode, screening acts, ready/create, recipe choice and parameters, adoption, chat entry, and decomposition hand-off. |
| `StartAnswers.java` (new) | The answers a direct verb carries (description, documents, issues, lesson, local changes, pull request, copy/move, title, id, recipe, parameters, adopt, entry, instruction, `--no-input`). |
| `StartApi.java` (new) | The HTTP surface of the start contract: entries, acts, chat turns/proposals/end, recipes, and the decomposition verbs. |
| `StartPrompt.java` (new) | Catalogue-driven questions, the one terminal of the context, the `--no-input` `MissingInputException`, and the leave exception. |
| `StartRenderer.java` (new) | The source list, failures and flagged spans, kind offers and their reasons, ready step, recipes, adoption, and refusals. |
| `ChatDialogue.java` (new) | The reusable chat turn loop, transcript rendering, proposal read-back, accept/reject, and clean end. |
| `EpicDecompositionDialogue.java` (new) | The third-step decomposition: draft with checks, plan detail, instruction/read-back, draft again, and agreement. |
| `PlanCommand.java` | `plan add` now drives `StartDialogue` with its arguments and the new `--entry` resumption. |
| `EpicCommand.java` | `epic add` now drives `StartDialogue`, with documents/issues, copy/move, `--adopt`, `--entry`, and `--instruction`. |
| `RecipeCommand.java` | `recipe run` now chooses the recipe, resolves its parameters, gathers request sources, and passes the ready step. |

### 2.2 `pm-api` — message catalogues

`messages/start.en.properties` and `messages/start.de.properties` gained the terminal dialogue's
texts (step counters, source lines and options, failure/flag prompts, ready rows, recipe rows,
adoption lines, refusal actions, chat lines, decomposition rows, and the `--no-input` need). Both
packs carry the same keys.

### 2.3 Tests

| File | Coverage |
|---|---|
| `StartDialogueTest.java` (new) | 17 cases against the mock runtime: arguments with `--no-input`, missing-input exit 3, interactive source list, documents copy, pull request with untrusted files, local changes withheld under `landing.files`, setup refusal, rate limit `resets_at`, flagged spans as plain text, epic adoption, adoption failure and abandon, open-entry continuation, interactive epic decomposition with edit/read-back/commit, start chat, proposal accept/store. |
| `StartTestSupport.java` (new) | Shared scripted-terminal runner for the three direct verbs. |
| `StartTranscriptTest.java` (new) | The interactive `plan add` and the shell `plan add --no-input` transcripts. |
| `transcript/start-plan.transcript`, `start-plan-no-input.transcript` (new) | Recorded transcripts at the pinned 80×24 grid. |
| `TranscriptDialogues.java`, `TranscriptDialogueIT.java`, `NativeTranscriptIT.java` | The two start transcripts in the JVM and, for the shell form, on the native binary. |
| `PlanCommandTest`, `EpicCommandTest`, `RecipeCommandTest` | Updated to the new direct-verb contract (the old single-endpoint shortcut no longer exists). |
| `LiteralTextGuardTest` | Guards the five new rendering classes. |

### 2.4 Documentation and archival

- Plan moved to `doc/agy/implemented/cli-start-plan.md`.
- Work package moved to `doc/plans/implemented/cli-start.adoc` and marked `done` in
  `doc/plans/README.adoc`.
- This report stays in `doc/agy/`; moving it to `doc/agy/verified/` is the orchestrator's Step 8.

---

## 3. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` unit tests | 619 tests, 0 failures, 0 errors |
| `pm-operator` unit tests | 574 tests, 0 failures, 0 errors |
| `pm-mock-runtime` unit tests | 307 tests, 0 failures, 0 errors |
| `pm-exec` / `pm-relay` | 87 / 25 tests, 0 failures (3 pre-existing skips in `pm-exec`) |
| Whole reactor `./mvnw verify -Ppre-commit` | `BUILD SUCCESS` (4 min 57 s) |

The pre-commit run covered OpenRewrite formatting, license headers, checkstyle, enforcer rules,
Javadoc, and every module's test suite. It left no reformatted or uncommitted generated file.

---

## 4. Domain Invariants

- **Java 25 and the module boundary** — the new classes live in `pm-operator` and consume only
  `pm-api` records; the `clients-depend-on-pm-api-only` enforcer rule passes.
- **No uncatalogued text** — every operator-facing line is a `messages/*.properties` key;
  `LiteralTextGuardTest` scans the five new rendering classes and passes.
- **Bilingual catalogue** — every new key exists in English and German with the same name; the
  `pm-api` catalogue tests pass.
- **Terminal trust boundary** — every server value (titles, references, spans, causes, finding
  messages, chat turns, refusal detail) passes through `TerminalSanitizer`/`OutputSafety` before it
  reaches the terminal; a flagged span is printed between typographic quotes, never interpreted.
- **No local file traversal** — document paths are sent to the server's confined picker; the client
  never reads them itself.
- **Fail-closed screening** — a flagged or unscreened source blocks `Continue` and stops a
  `--no-input` run; accepting it records a reason and keeps the untrusted mark.
- **Rate limits are not retried by themselves** — the client prints the reset instant and only
  retries on the operator's explicit *try again*.
- **The mock runtime is a read-only dependency** — no shared `start-*.json` fixture changed.
- **Epic scope is never asked** — the ready step shows `epic.auto_start` in force with its origin;
  the decomposition never asks the parallelization scope.

---

## 5. Build & Quality Evidence

```
$ ./mvnw verify -Ppre-commit
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  9.002 s]
[INFO] pm-api ............................................. SUCCESS [01:09 min]
[INFO] pm-exec ............................................ SUCCESS [ 14.415 s]
[INFO] pm-relay ........................................... SUCCESS [ 13.886 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 46.943 s]
[INFO] pm-operator ........................................ SUCCESS [01:29 min]
[INFO] pm-web-app ......................................... SUCCESS [ 40.896 s]
[INFO] pm-web-e2e ......................................... SUCCESS [ 12.873 s]
[INFO] BUILD SUCCESS
```

---

## 6. Notes, Deviations, and Follow-ups

- **`--entry <entry_id>` resumes an open entry.** The start contract exposes an entry only by its
  id once it is open; it has no project-level listing route. The dialogue therefore offers
  resumption through `--entry`, and the `start.continue` *offer* itself needs a listing route in the
  contract before the client can discover an open entry on its own.
- **Progress is a repaint of the list.** The mock runtime settles each source synchronously, so the
  dialogue reprints the list after every act; the same renderer is ready for intermediate
  `checking`/`fetching`/`screening` states when the runtime streams them.
- **The decomposition's `h` chat is not wired in the mock.** The mock serves chat turns for plans,
  not for epic decompositions; the start chat (`h` at the source list) is fully implemented and
  tested. The decomposition prompt keeps the list/edit/agree/draft-again actions.
- **A recipe that takes a request gathers its sources before the recipe choice.** The mock does not
  model the post-parameter source step, so the dialogue applies the source arguments while the entry
  is still at `start.sources` and then chooses the recipe and resolves its parameters; the resulting
  ready step is the same.
- **A discarded decomposition edit re-reads the draft.** The control contract offers `instruct` but no discard operation; the client therefore re-reads the server's draft after *Discard* and says so, so the operator never agrees a stale view. A contractual discard/proposal endpoint would close the gap.
- **The project may be named by `PM_PROJECT`.** Without `--project`, the dialogue uses `PM_PROJECT` when set, then the working directory's project, then the only enrolled project; `--project ?` remains the explicit list.
- **Native coverage is the shell form.** The interactive native transcript would need a pseudo
  terminal that feeds a script; the existing `PseudoTerminal` helper sends no input. The native
  `plan add --no-input` transcript is added and compares with the JVM-recorded file.

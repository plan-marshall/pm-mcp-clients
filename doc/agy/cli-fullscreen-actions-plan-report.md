# Completion Report: `cli-fullscreen-actions` (Full-Screen Actions, Epic View & Hold Resolution)

**Plan:** [`doc/agy/implemented/cli-fullscreen-actions-plan.md`](implemented/cli-fullscreen-actions-plan.md)
**Work package:** [`doc/plans/implemented/cli-actions.adoc`](../plans/implemented/cli-actions.adoc)
**Branch:** `feat/cli-fullscreen-actions`
**Worktree:** `/Users/oliver/git/worktrees/pm-mcp-clients-cli-fullscreen-actions`
**Use cases:** UC-CTL-1..5, UC-PLN-5
**Governed by:** [`doc/agy/runbook.md`](runbook.md)

---

## 1. Executive Summary

The `cli-actions` package is delivered in `pm-operator` on top of the existing full-screen frame:

- the **full-screen Epic View** (`EpicViewLevel`, UC-PLN-5) with the path `project › epic`, the
  head (state, mode, plans running of how many may, `! n`), the workstreams and their plans with
  status, waits, and overlap files, the sources, the landings with pull requests, the findings and
  watches registers, the tokens with split, floor, coverage and the per-plan breakdown, and every
  event (ten newest in the scrollback tree);
- the **inline actions menu** (`a`) below a plan row, an epic row, a plan's header, or an ended
  item, modal in the view, built only from what the runtime offers in the subject's state, with the
  acts of a hold first and a disabled action keeping the runtime's reason;
- the **hold dialogue** (`hold.show`, `hold.resolve`, `hold.apply`) for the five hold kinds, with
  the integrity pause's file, recorded and current size and digest, a confirmation for acts that
  cannot be undone, and the remaining hold shown again when the act did not release it;
- the **plan settings dialogue** (`config <aspect> --plan`, UC-CTL-5) with a refused value asked
  again and the concurrent-change conflict (`Apply yours again? / Keep the current value`);
- the **review dialogue** (keep as a finding or to the watches, drop with its reason, the remaining
  items, finish to the ledger) and the **chat** with the planning role, running and about ended
  work, with the read-back of the one proposal and the follow-up plan opening `plan add`;
- `epic <id>` as a tree from the epic resource (with `--tokens`), and `e` in a plan's view opening
  its epic's view, `p` in the epic view opening the project, and `Esc` returning to the exact
  caller level in every case.

All new texts are in the EN and DE catalogues; every runtime text is sanitized before it becomes a
cell. `pm-mock-runtime` and `pm-web-app` are untouched; `pm-api` changes are message-catalogue
entries only.

## 2. Delivered Artifacts

### 2.1 New package `de.planmarshall.operator.epicview`

| File | Purpose |
|---|---|
| `EpicViewLevel.java` | The epic's view: head with state/mode/scope/needs, workstreams and plans with status, waits, overlap files, pull requests, staged-plan objectives and deliverables, sources, landings with analysis, findings/watches, tokens with per-plan/own/total figures, and all events; open/close/expand-all; plan and register lookups; next launchable plan and wait reasons; ended note. |
| `EpicViewFormatting.java` | Pure formatting: state, mode, at-once, status from the closed list, waits and their reasons, workstream counts, glyphs, tokens with floor and coverage, register counts, event times. |
| `package-info.java` | Package contract: view models only, read-only `pm-api` data, `OutputSafety` at every cell. |

### 2.2 New package `de.planmarshall.operator.actions`

| File | Purpose |
|---|---|
| `ActionsMenu.java` | The modal menu: subject kind and id, entries with mnemonic/action/label/availability, its own selection, and the screen menu. |
| `ActionMenus.java` | Builds a plan's, an epic's, a landing's, or an ended item's menu from `WorkActions` and the hold record: hold acts first, the plan's state actions, the epic's next plan / automatic / common files / decompose / reviews / chat / archive / cancel, and the ended item's chat alone. |
| `WorkActionsClient.java` | The fail-closed control-contract client: actions, holds, hold resolution, plan settings with conflicts and refusals, review read/disposition/commit, chats and proposals, the epic resource, the archive check, and a landing's acts. |
| `package-info.java` | Package contract. |

### 2.3 New action dialogues (`de.planmarshall.operator`)

| File | Purpose |
|---|---|
| `WorkActionDialogue.java` | One action of a plan or an epic: pause with its point, start, cancel/abandon with reason and pull request, the epic's cancel with its plans, chat, settings, next plan (raising the scope), automatic, common files, decompose, the reviews, and the archive check; a state that does not offer the action is refused with the state and the actions it allows. |
| `HoldDialogue.java` | What holds a plan, its cause, attempts, integrity-pause facts, the acts with their mnemonics, the confirmation of irreversible acts, and the result (`goes on`, `Still held: …`). |
| `ReviewDialogue.java` | The findings/watches review: progress, each item's statement, rules, origin, proposal and `untrusted` marker, keep as a finding or to the watches, drop with reason, the remaining items, finish to the ledger, and a chat about the register. |
| `WorkChatDialogue.java` | The chat with a plan's or an epic's planning role (running and about ended work): transcript, one-line messages ended by an empty line, the read-back of the one proposal with accept/reject, and the follow-up plan of an ended work chat. |
| `PlanSettingsDialogue.java` | A plan's settings by aspect group, the value with its origin, a refused value asked again, too late, and the concurrent-change conflict, with the revision travelling with the change. |
| `OperatorActions.java` | The seam the view packages call: an action's dialogue, a plan's hold, a register review, and the ended-work chat with its follow-up start. |

### 2.4 TUI seams

| File | Change |
|---|---|
| `TuiMenu.java`, `TuiMenuEntry.java` | The modal menu model: entries with mnemonic, action, localized label, availability and reason; its own selection. |
| `TuiScreen.java` | Carries the optional menu. |
| `TuiLayout.java` | Draws the menu below its row or head, with the selected entry inverted and a disabled entry's reason; the menu footer while it is open. |
| `TuiScreenDriver.java` | A menu key map (mnemonics, `↑ ↓ Enter`, `a`/`Esc` close), menu dispatch, `suspendFor(Runnable)` for a dialogue that owns the terminal, and a full redraw when the menu appears, moves, or closes. |
| `TuiKeyRegistry.java` | The epic level's own bindings and footer (`Enter`, `a`, `p`, `Esc`, `*`, `q`). |

### 2.5 `pm-operator` integration

| File | Change |
|---|---|
| `OverviewFullScreenApp.java` | Opens `EpicViewLevel` from an epic row, from `e` in a plan view, and from ended work (falling back to the project's by-epic view or an ended note when the runtime serves no resource); refreshes it on live events; wires `a`, the menu, hold acts, the review/chat/settings dialogues, `p`, `Esc`, `a`/`Esc` menu closing; persists an epic's project on exit; a status line for a refusal. |
| `EpicCommand.java` | `epic <id>` prints the epic resource as a tree (head, workstreams, plans, sources, landings, registers, tokens, ten newest events) and `--tokens` the per-plan and own figures, falling back to the process resource. `epic findings\|watches` open the review dialogue on an interactive terminal. |
| `PlanCommand.java` | `plan pause` asks for its point when no flag is given (and `--no-input` stops); `plan chat` without `--message` opens the chat dialogue. |
| `ActionSupport.java` | A cancel/abandon without `--reason` asks for it on an interactive terminal; `chat` without a message opens the chat dialogue and its follow-up. |
| `PlanCompletionLevel.java` | `planId()` for the plan's actions from an ended completion report. |

### 2.6 Message catalogues (`pm-api`, EN and DE)

- `messages/operator.{en,de}.properties`: the action texts (`operator.actions.*`), the action
  mnemonics (`operator.actions.key.*`), the pause/end/archive/hold prompts, the epic view texts
  (`operator.epic.*`), the yes/no prompts, and the chat/review/settings texts — all from
  `22-texts-actions.adoc`, `19-actions.adoc#texts`, and `epic-view.adoc#texts`.
- `messages/overview.{en,de}.properties`: the shared epic-view texts (`overview.epic.state`,
  `status`, `mode`, `at_once`, counts, waits, overlap files) that the web app and the terminal share.

### 2.7 Tests

| Test | Coverage |
|---|---|
| `EpicViewLevelTest` (9) | Head, workstreams and waits, sections and events, toggles, staged-plan details, lookups and next launchable, breadcrumb/footer, ended note, empty epic. |
| `ActionMenusTest` (10) | Plan menu by state, paused/pausing, disabled reason, held and integrity-pause acts, epic menu and counts, nothing launchable, disabled archive, ended menu, landing menu, German mnemonics and no `a`. |
| `ActionsCatalogueTest` (5) | Every action, review, hold, settings, chat and epic-view text in EN and DE; every mnemonic one character and never `a`; no mnemonic repeats within any menu or prompt; every offered action is on a mnemonic or reported as dropped; a plan’s landing hold never takes Pause. |
| `EpicViewFullScreenTest` (9 screen tests) | Open from the project, from a plan's `e` with `Esc` return, epic actions and closing, `u` automatic off with the request body, `p` to the project and back, a staged plan's details, `*`, an ended epic's read-only view and chat-only menu, caller return. |
| `ActionsFullScreenTest` (9 screen tests) | Plan actions below the row, menu modality and closing, the arrows moving the menu’s own selection with Enter running it, pause with its point and request body, start in place, hold acts and retry resolving the hold, actions in the plan view, ended work chat alone, epic actions below the epic row, an inert key while a menu is open. |
| `AnswersTest` (2) | Yes and no are accepted in English and German, and nothing else. |
| `ControlDialoguesTest` (16) | Every control dialogue with a scripted terminal: hold retry/accept/still-held, review keep/drop/finish, chat turn/accept/ended follow-up, settings conflict/refusal, pause/start/cancel, epic automatic/common files/next/archive, and the client's reads and writes of every control resource. |
| `ConfigCommandTest` (+2) | `config <aspect> --plan` refuses the verb’s other options, and a blank `--plan` is rejected. |
| `EpicCommandTest` (+2) | `epic <id>` as a tree from the resource and `--tokens`. |
| `ControlEpicTranscriptTest` + `control-epic.transcript` (1) | The scrollback epic tree against the `running-work` mock scenario, JVM and native. |

## 3. Test & Verification Results

- `./mvnw verify -Ppre-commit`: **BUILD SUCCESS** (all eight modules, 25:21),
  including `pm-web-app`'s `svelte-check` with 0 errors and 0 warnings.
- `pm-operator` surefire: **825 tests, 0 failures, 0 errors, 0 skipped** (the previous suite plus
  65 new ones; `ActionSupportTest` was updated).
- New tests: 65 (9 + 10 + 5 + 9 + 9 + 2 + 1 + 2 + 2 + 16).
- `pm-web-app`: `npm run build && npm run check` → **0 errors, 0 warnings** (see the note in
  section 6).

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 / modern language features | Passed (records, sealed-free switch expressions, `var`, text blocks). |
| No uncatalogued user-facing strings | Passed: the new dialogues and `OperatorActions` are in `LiteralTextGuardTest`'s guarded set; every prompt and line comes from the catalogue. |
| Bilingual catalogue in EN and DE | Passed: `ActionsCatalogueTest` pins every key in both languages, every mnemonic, and the uniqueness of the mnemonics within each menu. |
| Terminal output sanitizer boundary | Passed: every runtime text in `EpicViewLevel`, `EpicViewFormatting`, the dialogues and the app's status line passes through `OutputSafety`/`TuiLayout.sanitizeCell`; the review quotes `untrusted` items. |
| Read-only consumption of `pm-mock-runtime` and `pm-web-app` | Passed: no file in either module changed. |
| `pm-api` changes limited to message catalogues | Passed. |
| Tokens are never money and carry floor/coverage | Passed: `TokenFormat.formatSplit` with `floor`; `not measured` where no task was measured; no budget figure. |
| The runtime decides what a state offers | Passed: menus and dialogues read `WorkActions`; an action the state does not offer is refused with the state and the actions it allows. |
| Alternate screen and cursor restored | Passed: `TuiScreenDriver.suspendFor` leaves and re-enters the alternate screen around a dialogue; the screen tests assert one entry and one exit. |
| Ended work is read-only and offers the chat alone | Passed (`EpicViewFullScreenTest`, `ActionsFullScreenTest`). |

## 5. Behavioural Notes and Contract Limits

- **Epic view from a plan:** `e` pushes the epic's view; a second `e` returns to it, and `Esc`
  returns to the plan. A runtime that serves no epic resource falls back to the project's by-epic
  view, so the key never becomes inert.
- **`p` in the epic view** pushes the project's level (by plan), so `Esc` returns to the epic even
  when the epic was opened from that same project.
- **The menu is modal:** while it is open only its mnemonics, `↑ ↓ Enter`, and `a` or `Esc` act; the
  view's keys wait. A mnemonic that is not in the menu does nothing, so `p` in a paused plan's menu
  cannot pause it.
- **Holds:** the plan's hold record drives the menu (`retry`, `recycle`, `grant`,
  `add_credential`, `raise_budget`, `accept`, `abort`, `end_plan`, `show_pr`, `give_up_removal`).
  An epic's hold uses the epic's action channel for its acts; a landing's failed act uses the
  project-level landing action channel. An act that does not release the hold shows it again with
  what still holds it.
- **Review writes:** the control contract (`control-contract`, delivered) exposes the review as a
  read resource (`GET /api/v1/epics/{id}/review?register=`). The dialogue's dispositions and its
  finish use `POST /api/v1/epics/{id}/review/{item}/disposition` and `…/review/commit`; against a
  runtime that does not answer them the dialogue reports the refusal and changes nothing. Promotion
  and the edit of a drafted plan are offered only when the review carries a drafted plan or a
  read-back, and are otherwise shown as not offered with that reason — the client never invents a
  draft.
- **Chat:** a plan's chat is the same dialogue as `cli-start`'s, routed to `replan`; an epic's to
  `decompose`. The ended-work chat is read-only and its one proposal is a follow-up description
  that opens `plan add` at its sources. The conversation turns are kept with the subject.
- **Settings:** the revision of the settings a dialogue opened with travels with the change; a
  conflict offers the operator's value again or keeps the current one; a refused value is asked
  again with its reason; a change that is too late changes nothing.
- **Narrow terminals:** the epic view's details lines follow the same 100/80/60-column thresholds
  as the rest of the frame; below 60 the verb falls back to the scrollback tree.

## 6. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
BUILD SUCCESS
```

- The pre-commit gate ran OpenRewrite, the license headers, Checkstyle, the Enforcer rules,
  Javadoc, the frontend build, lint, typecheck, tests, and the full unit-test suite of all eight
  modules (25:21).
- `pm-operator` surefire: **825 tests, 0 failures, 0 errors, 0 skipped**.

> **Worktree note (`pm-web-app`):** SvelteKit 3 writes its typecheck config into
> `node_modules/$app/` during `npm run build`. In a *fresh* worktree whose `npm ci` runs after the
> package was built, `svelte-check` can see `File '$app/tsconfig' not found`; running the module's
> `npm run build` once (or the full gate, as here) restores it. The module is untouched by this
> plan, and the full gate above passed its typecheck with 0 errors and 0 warnings.

All CodeRabbit findings were addressed in the branch: the German and English yes answers, the
menu's own arrow navigation, the stale project of an action, the silent mnemonic collisions (now
narrowed and reported), the duplicated review statement, the `now` pause message, the exclusive
`--plan` options, the analysis prefix boundary, the `!` binding on the epic level, and the report
and Javadoc details.

**Pre-commit workspace note:** the repository-wide OpenRewrite pass reformats a set of pre-existing
`pm-api` sources that this plan does not own. Those generated changes are reverted after the green
run, so the delivered diff stays within the plan's boundary; re-running the gate reproduces the
same unrelated formatting churn.

## 7. Follow-ups for the Orchestrator (Step 8)

- Run the 4-pillar post-landing verification and move this report to
  `doc/agy/verified/cli-fullscreen-actions-plan-report.md`.
- Record the landed PR number in `doc/agy/implemented/README.md` and `doc/agy/verified/README.md`.
- The review's disposition and commit routes and the epic/landing hold routes are used through the
  action channels described in section 5; if the runtime's control contract grows explicit
  endpoints for them, only `WorkActionsClient` needs to follow.

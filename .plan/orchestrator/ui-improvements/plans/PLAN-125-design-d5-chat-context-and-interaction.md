# PLAN-125: Design D5: Chat Context, Acts and Events (One Working Surface Across Chat and UI)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one design session across three repositories, handed off as a direct Claude Code session (operator
> instruction 2026-11-09, like D4; not the runbook lane, not `/plan-marshall`). It builds on D4 (PLAN-115) as AGREED by the
> operator's review of 2026-11-09. Where this spec and an AGREED document differ, the AGREED document stands until the
> operator decides otherwise in this session.

## Objective

The operator's chat questions of 2026-11-09. D4 designed how a chat LOOKS and STREAMS. D5 designs what a chat KNOWS, what it
can DO, and how it STAYS CURRENT, so the operator can move between the chat and the views without a seam: start in a view, work
in the chat, go back to the view, and find the same state in both. The target is the inline chat of the two clients (web app
and terminal). Exposing the whole chat to a separate external harness over MCP is **out of scope** (operator, 2026-11-09).

The six questions, each answered first as the current state (with file:line), then as options with a recommendation, then
decided by the operator in the session, then written down:

1. **Context.** How is the context of each chat gathered today? Are there rules per subject type — project, epic, running
   plan, ended work, decision (worker / server rule / quorum / workspace), start entry, decomposition, review, project
   onboarding — so the role gets what it needs without spending unnecessary tokens? Push (assembled up front) versus pull
   (fetched by the role on demand), budgets, and how the context is refreshed across a long-lived chat (D4: a subject's chat
   stays until _New chat_). Includes the deferred "replan knows …" line (D4 review decision 7: it waits for a context summary
   in the chat contract).
2. **Interaction pattern for embedded controls.** When the chat touches a decision (or any act with its own control): is the
   control rendered inline in the chat flow, or does the chat link to the main view? Local rendering versus duplicating the
   view's control; one source of truth for the act's state; what the other surface shows meanwhile.
3. **Full interaction.** Can the chat do what the UI does by calling the same backend acts in the same way — the same guards,
   consents, read-back and audit — so a change made in the chat is indistinguishable from one made in the view? Today a chat
   only PROPOSES (D4: nothing changes without _Accept_). Which acts may a chat trigger, under which confirmation, and how does
   the operator move back to the view at the place the chat left off (addresses, `chat=open`, selection)?
4. **An MCP for the chat role.** Should the role answering a chat get an MCP server (from the runtime, through the relay
   `pm-mcp` its harness already uses) that provides context on demand (ties into 1), skills, and tools (ties into 3)? Which
   tools, read-only versus acting, and how they map onto the act surface and its guards.
5. **Events into the chat.** Is there a sensible, ideally narrative, event log that can be pushed into an open chat so its
   context and status stay correct (the plan moved on, a decision was answered elsewhere, a task failed)? What reaches the
   transcript as a notice, what reaches the role's next turn as context, and what is left out.
6. **The terminal client.** Is the chat already modelled in the CLI, and does it reach parity with 1–5? (It is: the terminal
   chat in `terminal-client/19-actions.adoc` (A Chat), `14-transcripts-decisions.adoc`, `18-starting.adoc`, with the D4 review
   decisions of 2026-11-09; the session checks code against design too.)

## Sources (read in full before scoping)

Decisions already made (read first; they bind this session):

- The epic tree (resolve with `python3 .plan/execute-script.py plan-marshall:plan-orchestrator:orchestrator resolve-path
  --slug ui-improvements`; read, never write): `epic.md` § Decisions, the entries **"2026-11-09 — D4 REVIEW DECIDED"** (nine
  operator decisions: terminal leave = hide and `new`; a decision's answers lock only while busy; `WizardPage` hosts the chat;
  ended work keeps its chat; the terminal chooses the speech method; _Accept_ primary; empty chat role + scope with the "knows"
  line deferred; terminal reply line by line with stop key `s`; values) and the D4 entries of 2026-11-06 / 2026-11-08 (the
  waiting mark, `marked` approved as lexer only); § Open Defects item (7) (the D4 daemon surface, ledger only, not filed).
- `landings/PLAN-115.md` (D4 landing: what shipped, the seams, the open points).
- `inbox/archive/design-review/design-review-009.md` (the operator's chat brief, all sections, incl. the contract / engine /
  daemon / mock part) and `design-review-010.md` (process, design sessions, per-plan chat adoption).

The design and specification as merged (D4 at plan-marshall-documentation `e141a2b6` + `f475e34d`, pm-mcp-core `21fc7e79` +
#334, pm-mcp-clients `354098bf` + #387):

- This repository: `doc/design/web-app/chat.adoc`, `web-app/components-chat.adoc`, `shared/work-actions.adoc` (#chat,
  #chat-ended), `shared/work-actions-chat-texts.adoc`, `shared/decisions.adoc` (#chat), `shared/starting-work.adoc` (#chat),
  `web-app/navigation.adoc` (#side-columns, addresses), `terminal-client/19-actions.adoc`, `14-transcripts-decisions.adoc`,
  `18-starting.adoc`, `22-texts-actions.adoc`; the code as it is (`pm-web-app` `ChatPanel`, `pm-api` chat resources,
  `pm-operator` chat handling, `pm-mock-runtime` chat).
- pm-mcp-core: `doc/specification/state-catalogue.adoc` (#chat, #chat-reply-stream, the `chat.*` states, routing and the facts
  per role), `phase-workflows/08-dynamic-re-planning.adoc#replan-chat`, `model-work/` (roles, configuration, what a job's
  context holds), `harness-profiles/` (worker activity), `workflow-dsl/`, `timeouts/` (#chat-page).
- plan-marshall-documentation: requirement PM-WF-24 (`doc/requirements/02-workflow.adoc`), PM-ARCH-11, the use cases
  UC-CTL-1 and the decision use cases, `doc/specification/runtime-model/01-processes-and-transport.adoc` (#chat-routes, the
  act routes), `06-event-catalogue.adoc` (all events, `chat.*`), `05-mock-runtime.adoc`, the relay / MCP specification of
  `pm-mcp` (the tools a worker reaches over MCP today), `cli-and-security/` (consents, guards, audit).

## Deliverables

1. **Current state and options** (no repository file): for each of the six questions the current state with file:line
   across the three repositories and the code, gaps and contradictions, two or three options with consequences, and a
   recommendation. Saved as `.plan/temp/d5-findings.md` in this worktree and **put to the operator in the session, one
   question at a time**; every answer is recorded in the documents it changes (and the session's report lists them).
2. **Requirement, roadmap and runtime specification** (`plan-marshall-documentation`, checked with `doc-review` and `trace.py`
   there): what the decisions require — e.g. PM-WF-24 (chat acts, context rules, events into a chat), the roadmap entry that
   places the work, the runtime model (act routes reached from a chat, the event feed of an open chat, a narrative event shape
   if decided), the relay / MCP specification if the chat role gets tools. Open points go to `doc/discussions/open-questions.adoc`.
3. **Engine specification** (`pm-mcp-core`): the context rules per subject type with their budgets and refresh, the facts a
   chat role receives versus fetches, the acts a chat may trigger and through which guards and confirmations, the chat role's
   MCP tools if decided, how events reach the running chat job; the work packages this needs (as D4 did with
   `chat-reply-stream`).
4. **Client design** (this repository): the embedded-control pattern (inline versus link) in `shared/work-actions.adoc`,
   `shared/decisions.adoc` and `web-app/chat.adoc` / `components-chat.adoc`; moving between chat and view (addresses,
   selection, what each surface shows while the other acts); the event notices in the transcript; the terminal's parity in
   `terminal-client/19-actions.adoc` and its transcripts; texts in the chat text catalogues. A clickable prototype on the
   realistic mock settles the chat ↔ view switching before `AGREED` (throw-away, ports 21250 app / 21251 mock, never
   committed).
5. **Review and pull requests** — one pull request per repository that changes, see Standing Constraints.

## Standing Constraints

- Design and specification only. Code packages (contract and mock, web, terminal) are staged by the orchestrator after the
  merge. The daemon surface (`plan-marshall/plan-marshall-mcp`) is listed in the report, not filed (operator, 2026-11-09:
  ledger only).
- Inline chat only; no external-harness exposure of the chat over MCP.
- A chat never acts without the operator: whatever acts are decided keep the act's own guards, consents, read-back and audit;
  an untrusted item stays marked and plain text (D4); the chat's untrusted turns never reach the markdown renderer.
- The D4 review decisions of 2026-11-09 stand unless the operator changes one in this session; a change is recorded with its
  date in the document it changes and in the report.
- Edit the three repositories only in their worktrees: this one, `../plan-marshall-documentation` and `../pm-mcp-core` (all
  three at `.plan/local/worktrees/`, on the branch `docs/125-design-d5-chat-context-and-interaction`). Never edit the main
  checkouts under `/Users/oliver/git/`.
- Every changed design document is `Status: DRAFT` in the pull request; the operator reviews; the orchestrator makes the
  AGREED commits; each pull request is merged only on the operator's word. **No auto-merge.** Stop once every pull request is
  green and its review comments are answered, and report.
- Review: `doc/design/` with `pm-documents:ref-documentation` (docs review) plus `pm-documents:ref-asciidoc` (`validate`,
  `verify-links`); `plan-marshall-documentation` and `pm-mcp-core` with `doc-review` and `trace.py` there. 500-line rule
  (`doc/design/README.adoc`): `web-app/README.adoc` is at 499, do not grow it.
- Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>` and no other attribution; pull request bodies carry no
  generator footer; GitHub through `gh` only.

## Expected Surface

- OBSERVED: `doc/design/web-app/chat.adoc`, `web-app/components-chat.adoc`, `shared/work-actions.adoc`,
  `shared/work-actions-chat-texts.adoc`, `shared/decisions.adoc`, `shared/decisions-texts.adoc`, `shared/starting-work.adoc`,
  `terminal-client/19-actions.adoc`, `14-transcripts-decisions.adoc`, `22-texts-actions.adoc` — present and AGREED at
  `85edf2a5`
- HYPOTHESIS: `web-app/navigation.adoc` (addresses for chat ↔ view) — only if the switching design needs an address change
- HYPOTHESIS: sibling files: PM-WF-24, the runtime model 01 / 05 / 06, the relay / MCP specification
  (plan-marshall-documentation); `state-catalogue.adoc`, `model-work/`, `doc/plans/` (pm-mcp-core) — confirm at the start

## Dependencies and Sequencing

- Depends on: PLAN-115 (D4) with its review PRs — all merged 2026-11-09: plan-marshall-documentation #169 `f475e34d`,
  pm-mcp-core #334 `96aaea5b`, pm-mcp-clients #387 `85edf2a5`. The chat design is AGREED.
- Blocks: the D4 code packages (contract and mock, web chat, voice) are re-scoped by D5's outcome before they are staged.
- Overlaps with: none running (lane plans PLAN-120, PLAN-124 are terminal code; none edits a D5 document).

## Worktrees (prepared by the orchestrator 2026-11-09)

All three on the branch `docs/125-design-d5-chat-context-and-interaction`, at origin/main, nothing to pull:

- this repository: `/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/125-design-d5-chat-context-and-interaction` (`85edf2a5`)
- `../plan-marshall-documentation` (`f475e34d`) and `../pm-mcp-core` (`96aaea5b`), real worktrees beside it, so the relative
  cross-repository links of `doc/design` resolve; `../plan-marshall-mcp` is a symlink to its main checkout (read only).

If a worktree is not on that branch or has fallen behind origin/main with commits of others, fast-forward it
(`git merge --ff-only origin/main`); if that is not possible, stop and tell the operator.

## Session Procedure

1. **Read.** This spec, then every source under Sources in full, the operator's decisions first (they bind you).
2. **Six questions, one at a time.** For each question of the Objective, in order: establish the current state with
   file:line across the three repositories and the code; write it with gaps, two or three options with consequences, and a
   recommendation into `.plan/temp/d5-findings.md`; put that question to the operator and **wait for the answer** before the
   next. Record every answer with its date in the findings file.
3. **Write**, only after all six are decided: deliverables 2 (plan-marshall-documentation), 3 (pm-mcp-core) and 4 (this
   repository, both clients), all `Status: DRAFT`.
4. **Prototype** the chat ↔ view switching on the realistic mock (ports 21250 app / 21251 mock, throw-away, never committed)
   and walk the operator through it; change the documents on what it shows.
5. **Review**: `pm-documents:ref-documentation` plus `pm-documents:ref-asciidoc` `validate` and `verify-links` here;
   `doc-review` and `trace.py` in the two sibling repositories.
6. **Pull requests**: one per repository that changes, no auto-merge; wait for CI and the bots, answer every review comment
   and every review body, fix what is right.
7. **Stop and report** when every pull request is green and answered: write one inbox message (`python3
   .plan/execute-script.py plan-marshall:plan-orchestrator:orchestrator inbox write …`, kind `finding`) and tell the operator:
   the pull requests with heads, the six decisions, the operator decisions still open, the daemon surface
   (`plan-marshall/plan-marshall-mcp`, listed, not filed), and the code packages D5 implies. The orchestrator makes the
   AGREED commits after the operator's review; the operator merges.

## Hand-Off Command

Run `claude` in `/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/125-design-d5-chat-context-and-interaction`, then:

```text
Implement the staged design plan PLAN-125 of the epic ui-improvements. Read the spec and follow its Session Procedure exactly: /Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-125-design-d5-chat-context-and-interaction.md
```

## Write-Boundary

The session writes only in its three worktrees and its own inbox message (`orchestrator inbox write`); nothing else under
`.plan/orchestrator/`. It does not merge.

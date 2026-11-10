# Landing Analysis: PLAN-125 — Design D5: Chat Context, Acts and Events

epic: ui-improvements
workstream: WS-06
pr: pm-mcp-clients #389 (https://github.com/plan-marshall/pm-mcp-clients/pull/389) merged as `0e79de7b`; pm-mcp-core #338 merged as `72b602e2`; plan-marshall-documentation #170 squashed as `a94ea60` — all 2026-11-09, at `Status: DRAFT`

> Direct Claude Code design session (the operator's "D5"). Inbox messages `plan-125-001.md` (mid-flight, drained as
> `observed`) and `plan-125-002.md` (final). Merged on the operator's word ("continue and do the merging"); the AGREED
> commits are the orchestrator's after the operator's review. Checked against git in all three repositories, `ci pr
> view`/`ci pr comments`, SonarCloud and a read-only leaf; key points re-read by the orchestrator. Analysed 2026-11-09.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| Six operator questions, one at a time | met (per the reports) | decisions 1-6 recorded in `plan-125-001.md`, stated in the merged text (`clients:web-app/chat.adoc:326` with date and operator) |
| DRAFT design in three repositories | met | 11 client files changed: 10 `DRAFT`, `terminal-client/README.adoc` kept `AGREED` with one index line (the report's "13 files" is not reproducible); docs UC-CTL-1/2/3, UC-PLN-2, UC-STA-1/2/6 `DRAFT` with open points; core state catalogue, model work, store schemas, two packages `chat-context`/`chat-narrative` |
| Prototype | not shown by reading | never committed (`.plan/temp/d5-prototype/`), stopped per the report |
| PRs green, reviewed, merged | met | see Routing |
| The orchestrator's 28-item analysis answered | met, 26 fully, 2 partly | items 10 and 19 — see Layer 2 |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`) over the three merge diffs.
- **Verdict:** layer 1 clean; layer 2 findings — 2 (items 10, 19 partly); layer 3 findings — 2 major, 12 minor (documentation consistency; no code).
- **Re-read by the orchestrator:** `clients:web-app/chat.adoc:165` vs `clients:shared/work-actions.adoc:405`; `clients:terminal-client/28-chat.adoc:20-26,46,56`; `clients:web-app/chat.adoc:178-179`; `core:state-catalogue.adoc:97,176`; the Status line of every changed client file. All confirmed.

### Layer 1 — Specified

All six decisions are stated in the merged text; no spec premise is contradicted. The spec carries no Done-when section; deliverables 2-5 and the decisions served as criteria. **Layer 1 verdict:** clean.

### Layer 2 — Implemented

| Item | State | Evidence |
|------|-------|----------|
| 1-9, 11-18, 20-28 | fixed | each in the merged text at the report's commits (leaf table); e.g. stale proposal `chat.adoc:~108` + `28-chat:79` + `core:state-catalogue:138`; overview row `docs:01-processes:~355`, `06-event-catalogue:62`; bound generation `core:model-work/02`; `hold` `docs:01-processes:~283`, `chat.adoc:249` |
| 10 "changed in the chat" | partly | `work-actions.adoc:405` "until the decomposition or the review is committed, the same on every device", but `chat.adoc:165` still "until the operator opens it" |
| 19 act → command | partly | `28-chat:56` "no further argument, the verb asks", but the sample at `28-chat:46` prints `plan pause fix-relay --after-task`, and `settings` takes `config <aspect> --plan` |

**Layer 2 verdict:** findings — 2

### Layer 3 — Correct

| Finding | File:line | Issue | Severity |
|---------|-----------|-------|----------|
| L3-1 | `clients:web-app/chat.adoc:165` vs `clients:shared/work-actions.adoc:405` | two lifetimes for the `changed in the chat` mark; the first needs per-device state item 10 ruled out | major |
| L3-2 | `clients:terminal-client/28-chat.adoc:24`, `clients:web-app/chat.adoc:179`, `core:state-catalogue.adoc:97,176`, `docs:01-processes` chat routes | the empty chat shows `replan knows … as of 10:42` before any turn, but `context` is what the role was last given and the core is rendered only when a generation is primed (the first turn); nothing defines `context` for a chat with no turn; `work-actions.adoc:372` still says `chat.open` gives the role the facts | major |
| L3-3 | `chat.adoc:150-165`, `core:146` | the queued revision's `ChatStatusCard` has no `refs` kind or narrative line as its source | minor |
| L3-4 | `28-chat:46,56` | sample and `settings` entry break the no-argument act rule (item 19) | minor |
| L3-5 | `docs:open-questions.adoc:153` vs `docs:01-processes:286`, `core:138` | another subject's outdated proposal "refused `conflict`" in the open question; elsewhere `conflict` is a plan's version refusal only | minor |
| L3-6 | `docs:01-processes:230` vs `core:140` | "every chat-led change records `origin`" vs the three forms of `#chat-origin` | minor |
| L3-7 | `components-chat.adoc:22`, `work-actions.adoc` chat picture | "the one picture shows all five pieces" — now six (status card, knows line missing in the picture) | minor |
| L3-8 | `work-actions-chat-texts.adoc:63,313,329` | argument names drift (`{plan}`/`{subject}`, `{task}`/`{task_id}`) | minor |
| L3-9 | `28-chat.adoc:89-117` | terminal keys defined in the part instead of `22-texts-actions.adoc` | minor |
| L3-10 | `28-chat.adoc:15,57`, `chat.adoc:132` | superseded-behaviour wording ("as today", "that D4 left open") | minor |
| L3-11 | `shared/plan-view.adoc` `#chat-revision` | "the way to the chat at the turn" with terminal `h) chat`; the terminal has no turn address | minor |
| L3-12 | `core:state-catalogue:138,86`, `decisions.adoc:20` | whether a revision from a called decision chat is queued like a hosted one is not stated | minor (not shown by reading) |
| L3-13 | `core:model-work/03`, `chat-context` Done-when, `work-actions.adoc:393` | fresh-form fallback (fresh worker per turn, `form_downgraded`) not in the clients' wording nor tested in the package | minor |
| L3-14 | `28-chat.adoc:34,55,77` | terminal adds `changes`, `o <id>`, `q`/`new` lines beyond "parity (`older` only)"; ended-work chat follows the workspace's stream while core says ended work has no narrative | info |
| Open to the operator | `docs:06-event-catalogue.adoc:62` | `activity.changed` (AGREED catalogue) now carries `queued_revision`; the clients' contract bullet lists the field, not the event or overview row | info |

**Layer 3 verdict:** findings — 2 major, 12 minor (all documentation consistency; for the AGREED pass)

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| `clients:doc/design/README.adoc` | a document below 500 lines; web-app README must not grow | conforms | largest changed `19-actions.adoc` 487; web-app README untouched |
| `clients:doc/design/README.adoc` | Status stated in the document | conforms | 10 `DRAFT`, terminal README `AGREED` (declared) |
| ref-documentation | no account of superseded behaviour | deviates | L3-10 |
| design texts convention | terminal texts in parts 20-22 | deviates | L3-9 |
| docs `trace.py` | broken links 0 | not shown by reading | reported by the session on the final commit |

## Metrics and Anomalies

- Tokens/duration: direct session, not measured by the orchestrator.
- Anomalies: the session's first CI watcher was faulty (zsh word splitting) and was stopped; the worktree cleanup it left to the orchestrator was refused by the permission classifier on 2026-11-09 and is left for the operator.

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments`):** core #338 — CodeRabbit 1 body (9 actionable, no nitpick/outside-diff), 9 threads, 0 unresolved, all replied; body answered (12:22Z); follow-ups per the report (5 hit CodeRabbit's chat rate limit, no verdict). Clients #389 — first run rate-limited, re-triggered once; 1 body on `a7cd395b` (6 actionable in 4 threads), 4 threads, 0 unresolved, all replied; body answered (12:40Z); later heads unreviewed (one review suffices). Docs #170 — no CI and no bot by repository rule. **Merge rule met** for #338 and #389. Sourcery: no comments.
- **CI/merge:** core and clients through the merge queue; docs squashed. All three merge commits are on their `main`.
- **Sonar:** core #338 0 issues, gate OK (read 2026-11-09); clients #389 docs only, Sonar skipped by change detection (`n/a`).

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped `389` (the clients PR; core #338 and docs #170 named above)
- [x] row `landing` stamped `landings/PLAN-125.md`
- [x] row `plan_marshall_plan_id` `plan-125` (already stamped)
- [x] epic.md: D5 Watch replaced (DRAFT merged, AGREED review owed with L3-1/L3-2 and the minors, open questions, code packages after AGREED, cleanup owed)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- The operator's AGREED review of the merged DRAFT documents; fix L3-1 and L3-2 (and the minors) in the AGREED pass, then the orchestrator's AGREED commits in all three repositories.
- Open questions `chat-bounds`, `chat-narrative-catalogue`, `chat-lapsed-repropose`, `chat-lapse-other-subjects`; the `activity.changed` event choice.
- After AGREED: stage the client packages (D4 re-scoped by D5: contract+mock, web chat, terminal chat) with the defects listed in `plan-125-001.md`; the engine packages `chat-context`/`chat-narrative` are pm-mcp-core's.
- Cleanup owed: three local worktrees, the `plan-marshall-mcp` symlink, local and remote `docs/125-design-d5-chat-context-and-interaction` branches.

---

# Addendum: the AGREED round (2026-11-09, `plan-125-003.md` observed, `plan-125-004.md` final)

pr: pm-mcp-core #340 merged as `fa1c11ef` (head `48152b61`); plan-marshall-documentation #171 squashed as `49f785d8`
(head `60a993a7`); pm-mcp-clients #392 merged as `0cc6a66d` (head `777a6091`) — in that order, on the operator's word.

## Fidelity

| Item | Verdict | Evidence |
|------|---------|----------|
| Landing findings L3-1 … L3-14, the `activity.changed` choice, the four open questions | met | leaf at the PR heads: 16/16 fixed; operator decisions 1-8 in `plan-125-003.md` |
| Orchestrator's pre-merge points N1-N8 | met, N1 partly | leaf on `main`: N2-N8 fixed; N1 fixed in core, plan resource, event and clients, not in the docs overview row (F1) |
| Status AGREED | met | 11 client files and UC-CTL-1/2/3, UC-PLN-2, UC-STA-1/2/6 `AGREED` on `main`; no D5 document `DRAFT` |
| Cleanup | met | no `docs/125*` branch on any remote, no 125 worktree (checked) |

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments` at merge):** core #340 — CodeRabbit 2 bodies (`ba2b8fb9` 5 actionable,
  incremental `113bcd2d` 4 actionable), 6 threads, 0 unresolved, all replied, both bodies answered. Clients #392 — 1 body
  (4 actionable), 4 threads, 0 unresolved, all replied, body answered. Docs #171 — no bot. **Merge rule met.**
- **Sonar:** core #340 gate OK, 0 issues; clients docs only (skipped).

## Layer 3 — residue on `main` (leaf, read-only; F1 and F2 re-read by the orchestrator)

| Finding | File:line | Issue | Severity |
|---------|-----------|-------|----------|
| F1 | docs `runtime-model/01-processes-and-transport.adoc:357` | the project overview row carries `queued_revision` "with its `applies`" only; `:178` (plan resource), clients `chat.adoc:249` and `plan-view.adoc:196` need `subject` and `chat_open` on the row | major |
| F2 | core `state-catalogue.adoc:138`; clients `work-actions-chat-texts.adoc:440-442` | `control.chat.already_queued` tells the operator to accept the second revision "once it has applied" — but applying bumps the instance version, so that accept is refused `conflict` (older version); only a refused/lapsed first revision leaves it acceptable | major |
| F3 | clients `web-app/chat.adoc:107` | already-queued card shown "as for an older version" (Accept disabled for good) although its accept returns when the queue clears | minor |
| F4 | clients `work-actions.adoc:420`, `terminal-client/28-chat.adoc:79`, `chat.adoc:249,251`, docs `05-mock-runtime.adoc:82` | the overtaken-proposal lists, the pm-api list and the mock list omit the already-queued refusal | minor |
| F5 | clients `work-actions-chat-texts.adoc:352-362` | `plan_pausing`/`plan_paused`/`plan_started` still `{subject}`; in a decision's chat that reads the decision | minor |
| F6 | core `phase-workflows/08-dynamic-re-planning.adoc:143` | `queued_revision` triple without `subject`/`chat_open`; no chat-end send of `plan.revision_queued` | minor |
| F7 | core `doc/plans/chat-narrative.adoc:19` | stale `{chat_id, turn_id, applies}` | minor |
| F8 | core `state-catalogue.adoc:213`, `phase-workflows/08:141` | accept/reject `conflict` list omits already-queued | minor |
| F9 | clients `chat.adoc:71`, `work-actions.adoc:395` vs core `state-catalogue.adoc:214,221,229` | ended work's "Start a plan with it" still called carried (`control.chat.carried`), the core applies it by `chat.apply` and no longer lists it as carried; whether it ends the chat is unstated | minor (needs a decision) |
| F10-F14 | docs `01:237` conflict description; terminal/WA only one boundary path illustrated; mock list without `chat_open` flip; `row_failed` `{outcome}` catalogue; two non-D5 `DRAFT` files | info |

**Verdict:** AGREED holds; F1 and F2 are small text fixes, both needed before the contract+mock package is built.

## Reconciliation Actions

- [x] row stays `shipped`, `pr` 389, `landing` this file (core #340, docs #171, clients #392 named above)
- [x] epic.md D5 Watch: AGREED merged, residue F1-F9, packages to stage; `plan-125-003.md`/`-004.md` archived
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- F1-F9 doc follow-up in three repositories (operator's choice of route).
- Daemon surface (plan-marshall-mcp), listed not filed: chat routes' `context`, notice turns with `lines[]`, refs `act`
  and `hold`, proposal states, `queued_revision` with subject and `chat_open`, `plan.revision_queued`, the chat worker's
  write set, chat resources and skill on the job connection.
- Stage the client packages (D4 re-scoped by D5): contract + mock, web chat, terminal chat; with the `plan-125-001.md`
  code defects. Engine packages `chat-context`, `chat-narrative` are pm-mcp-core's (`doc/plans/` there).

---

# Addendum 2: the follow-up round F1-F9 (2026-11-10, `plan-125-005.md`)

pr: pm-mcp-core #349 merged as `db02e443` (head `e71a661d`); plan-marshall-documentation #174 squashed as `10f98b73`
(head `f57491a`); pm-mcp-clients #395 merged as `cc0d62af` (head `60e617f4`) — in that order, on the operator's word
("merge all"). Every status stays `AGREED`.

- **Operator's F9 answer:** the chat stays — ended work's _Start a plan with it_ is applied by `chat.apply`, never
  carried; the chat goes on; `control.chat.follow_up.opened`.
- **Fidelity (leaf on `main`):** F1-F9 all fixed and consistent across core, docs and clients; the already-queued rule
  reads the same everywhere; no "carried; the chat has ended" wording left for a follow-up start; the info items
  (409 `conflict` description, mock scenarios, `row_failed` outcome incl. `handed_back`) in place; changed client files
  `AGREED`, all under 500 lines.
- **Review (re-read from `ci pr comments`):** core #349 — CodeRabbit 1 body (1 actionable), 1 thread, 0 unresolved,
  replied, body answered. Clients #395 — CodeRabbit reviewed `60e617f4` ("No actionable comments were generated"),
  0 threads, answered by PR comment. Docs #174 — no bot; `trace.py` 0 broken. **Merge rule met.** Sonar core 0 issues.
- **Cleanup:** no `docs/125*` branch on any remote, no 125 worktree (checked).
- **Residue (minor, re-read N2 and N3 by the orchestrator):**
  - N1 core `doc/plans/chat-narrative.adoc:37` (and `:8`) — tests list lacks the already-queued cases and the chat-end
    `plan.revision_queued` (core's own package).
  - N2 clients `web-app/components-chat.adoc:53` — ProposalCard knows only "Refused for an older version"; the
    already-queued variant (Accept re-enabled, lapse) is only in `chat.adoc:107` → fold into the web chat package.
  - N3 core `state-catalogue.adoc:132`, docs `01:359` — `plan.revision_queued` "when it is queued and when it leaves
    the queue", without the chat-end send that three other places state.
  - Info: N4 UC-CTL-1:124; N5 "a carried proposal (a decision's …)" reads exhaustive (`chat.adoc:71`,
    `work-actions.adoc:385`); N6 the web notice list has no rendering of `control.chat.follow_up.opened`; N7 the
    "taken again" rule stated absolutely (`state-catalogue:138`, `28-chat:79`).
- **D5 is closed.** Next: stage the client packages (contract + mock, web chat, terminal chat) from the AGREED
  documents with the `plan-125-001.md` code defects and N2/N3/N5/N6 folded in; the daemon surface stays listed, not filed.

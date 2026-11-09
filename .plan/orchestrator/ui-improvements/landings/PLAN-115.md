# Landing Analysis: PLAN-115 — Design D4: Chat and Voice

epic: ui-improvements
workstream: WS-06
pr: three, merged on the operator's word 2026-11-09, in order:

- plan-marshall/plan-marshall-documentation#167 → `e141a2b6` (squash, 15 files)
- plan-marshall/pm-mcp-core#331 → `21fc7e79` (merge queue, 15 files)
- plan-marshall/pm-mcp-clients#386 → `354098bf` (merge queue, 18 files, +877/-40, parent `9a52b89f`)

> Landing record for a direct Claude Code design session. Inbox message `plan-115-001.md`, 2026-11-09. Checked against
> git, the three PRs (gh) and a read-only leaf; the key points were re-read by the orchestrator at `354098bf`.

**THE DOCUMENTS MERGED AT `Status: DRAFT`.** That includes 12 client documents that were `AGREED` at `9a52b89f`; they
are DRAFT again. The AGREED commits (three repositories) are owed on the operator's review, as for D1 (PLAN-112 → PLAN-122).

## Deliverable Fidelity vs Spec

| Deliverable | Verdict | Evidence |
|-------------|---------|----------|
| Roadmap, requirement, specification first (plan-marshall-documentation) | shipped | `roadmap.adoc:524` (M8 streaming), `:693` (M11 voice); PM-WF-24 `02-workflow.adoc:384-386`; `01-processes-and-transport.adoc#chat-routes`, `#transcriptions`; `06-event-catalogue.adoc#chat-events`; `05-mock-runtime.adoc#mock-timing`; `03-web-server.adoc#web-chat-text`, `#web-voice`, `Permissions-Policy`; UC-ONB-6/10, UC-CTL-1; three open questions |
| Engine specification (pm-mcp-core) | shipped | `state-catalogue.adoc#chat-reply-stream`, `model-work/01-…#transcribe-role`, `timeouts/02-…#transcription-bounds`, `#chat-page`, identifiers (`chat_id`, `turn_id`, job prefix `s`), packages `doc/plans/chat-reply-stream.adoc`, `transcribe.adoc` |
| `web-app/chat.adoc` | shipped | new, 313 lines; untrusted turns plain text (`:81`, `:120`); `marked` approval `:220`. Prototype not verifiable from the repository |
| The other client documents | shipped | `work-actions.adoc:350-432`, `work-actions-chat-texts.adoc`, `decisions.adoc:132`, `consents.adoc#speech`, `components.adoc` + new `components-chat.adoc`, `machine-settings.adoc#speech`, `onboarding-steps-4-7.adoc#speech` (`:91-118`), `onboarding-texts.adoc#texts-speech`, terminal `07` (`#t-roles-speech`) and `08` (consent row) |
| Roles step location | shipped (spec was wrong) | the session called it a deviation; `onboarding.adoc:14` holds steps ②–③ only, ④ Roles is in `onboarding-steps-4-7.adoc`. The re-grounding of 2026-11-08 named the wrong file |
| Fold D3-S1 (working line, WaitingMark) | shipped | `chat.adoc#working` (`:89`), `control.chat.working`, `brand-mark.adoc:218-219`; `brand-waiting-mark.svg:26,47` now English |
| Fold D2-S8 (stale chat wording) | shipped | `decisions.adoc:132`, `work-actions.adoc:382,385`, `starting-work.adoc:149`; "panel" gone from `doc/design` |
| Fold D3-S9 (project onboarding done figure) | shipped | `shared/project-onboarding.adoc:38` (web only), link `brand-mark-mascot.adoc#surfaces` resolves |
| `navigation.adoc` not edited | respected | not in the diff |
| Daemon request (plan-marshall-mcp) | missing | not filed; the core packages only say "Left out: … the daemon" |
| Operator review change: _New chat_ replaces _End the chat_; a subject's chat stays | shipped | `work-actions.adoc:357,369,382,385`, `chat.adoc:44,69,71,143`, `components-chat.adoc:26`; no `control.chat.end` left. (`control.chat.end_confirm.*` never existed; `new_confirm.*` is new) |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`); documents of the three merged commits, own link resolver.
- **Verdict:** layer 1 met; layer 2 met (16 modified + 2 new + 1 SVG; terminal `07`/`08` are in-place edits, not splits; nothing over 500 lines, largest web README 499); layer 3 not clean: 3 major seams, 9 minor. All `link:`/`xref:`/`image::`/`<<>>` targets resolve, cross-repo included; cross-repo names (events, routes, roles, bounds) consistent.
- **Status reverts (12):** substantive — `consents`, `work-actions`, `work-actions-chat-texts`, `onboarding-steps-4-7`, `onboarding-texts`, `machine-settings`; one line or link — `decisions`, `project-onboarding`, `starting-work`, `brand-mark`, `components`; mixed — web `README` (+ the diff-colour exception, `:392`).
- **Re-read by the orchestrator at `354098bf`:** `19-actions.adoc:422,457`; `chat.adoc:69-73`; `decisions.adoc:126-132`; `navigation.adoc:104-118` (the table's rail is 232/64 px); `chat.adoc:83` vs `entry.adoc:136`; `starting-work.adoc:149`. All confirmed.

### Seams

| Id | File:line | Seam | Severity |
|----|-----------|------|----------|
| S1 | `terminal-client/19-actions.adoc:405,416,422,437,449,457`; `14-transcripts-decisions.adoc:201,230,239,242,246,254`; `18-starting.adoc:286,300` | AGREED terminal documents end the chat with an empty first line, `q` or `Ctrl+D`; contradicts "the chat stays … in the terminal client returns to the same chat" (`chat.adoc:71`, `work-actions.adoc:382`). Transcripts print `✓ The chat is kept with …` vs the new `control.chat.ended`. The session named only `19-actions` | major |
| S2 | `decisions.adoc:126,128,132`; `chat.adoc:73,236-238`; `decisions-texts.adoc:294` | a decision's answers stay disabled while its chat is open ("answer once the chat has ended"), but a chat now ends only by _New chat_, a carried proposal or the subject's end, so after _Hide_ the answers can stay disabled indefinitely | major (needs a decision) |
| S5 | `navigation.adoc:265,270-276`; `components.adoc:361`; `starting-work.adoc:149`; `chat.adoc:29,31` | no side column for `WizardPage` in the AGREED D2 documents; only `starting-work.adoc:149` asserts it (also decomposition, project setup) | major (D2's) |
| S3 | `decisions.adoc:127`; `work-actions.adoc:432`; `chat.adoc:71,98-106` | "nothing ends a chat by itself", yet _Answer in these words_ / _Start a plan with it_ end it; `chat.ended.ended_by` has four causes, the notices cover some | minor |
| S4 | `navigation.adoc:106-118`; `NavRail.svelte:189` | session's "280 px rail" claim REFUTED: the table uses 232/64 px. The prototype figures differ from the table by the 48 px page inset; the "400 where 480 would cost a column" rule still lacks an object-column-based form until list columns exist | minor |
| S6 | `chat.adoc:83` | `common.copy` does not exist; `web.common.copy` (`entry.adoc:136`) or `common.action.*` | minor |
| S7 | `chat.adoc:126`; `components-chat.adoc:51`; `README.adoc:392` | `--color-diff-removed/added` exception has no token values, no contrast pairs in `theme-readability.adoc`, no exception in `components-tags.adoc#tag-colour` | minor |
| S8 | `open-questions.adoc` `speech-terminal-choice` vs `02-workflow.adoc:386`, roadmap `:693`, UC-ONB-6, `consents.adoc#speech` | marked open, already written as option 1 | minor |
| S9 | core `state-catalogue.adoc#chat-reply-stream`; `06-event-catalogue.adoc` | reply over 64 KiB fails "with that reason", but `chat.turn.completed` reasons are only `infeasible`, `lost`; no client key | minor |
| S10 | core `#chat-reply-stream`; `chat.adoc:92-93,152` | replaced worker clears the turn and re-announces `chat.turn.started`; the client design does not say to reset | minor |
| S11 | `work-actions.adoc:373,418-429`; `chat.adoc:34,137,144` | ended-work sketch has no message field although ended work keeps one; ":373" reads like auto-send vs `chat.adoc:137` | minor |
| S12 | `onboarding-steps-4-7.adoc#auto` | the web automatic run does not say the speech role stays off | minor |

## Routing and Merge Behavior

- **Review:** pm-mcp-clients#386 — CodeRabbit 1 body (11 actionable), 11 threads all replied, 2 unresolved (resolution blocked by the session's permission classifier); body answered 23:25Z. pm-mcp-core#331 — CodeRabbit 1 body (20 actionable), 16 threads all replied, 4 unresolved; body answered 23:25Z; Sonar gate passed. plan-marshall-documentation#167 — no review bot in that repository (none on #164–168); its review is doc-review and `trace.py` (claimed 0 broken links, not re-run). The operator's _New chat_ change came after the bot reviews. Merge rule met.
- **Decisions:** six open per the session; only three are marked open in a document (`chat-terminal-reply`, `speech-terminal-choice`, `speech-local-model`); 1 (ended work keeps its field), 3 (_Accept_ secondary), 4 (empty-chat line) are written as settled; the bounds are fixed constants in the engine specification.
- **CI and merge:** green; merged on the operator's word, before the AGREED review.
- **Cleanup:** session worktrees and local branches gone in all three repositories; the remote branches `docs/115-design-d4-chat-and-voice` (clients) and `docs/d4-chat-and-voice` (documentation, core) remain. Ports 21150/21151 free. The three sibling-repo symlinks in `.plan/local/worktrees/` removed by the orchestrator.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 386; `landing` `landings/PLAN-115.md`; `plan_marshall_plan_id` `plan-115`
- [x] epic.md Watch; daemon request added to Open Defects (outside this repository)
- [ ] AGREED commits in three repositories after the operator's review (and S1/S2 decided first)

## Follow-Ups

- Operator: S1 (terminal leaving = hide, explicit `new`), S2 (answers while a chat is open), S5 (WizardPage side column), the six open decisions, then AGREED.
- Daemon request to plan-marshall-mcp: chat routes (`202`, `client_turn_id`), `POST …/chat/turns/{turn_id}/stop`, `GET …/chat?before=`, the `chat.*` events, `POST /api/v1/transcriptions`, `Permissions-Policy: microphone=(self)`.
- Packages after AGREED: D4 contract and mock; web chat (Chat, ChatTurn, ProposalCard, ChatMarkdown); voice (VoiceInput, roles.speech*).

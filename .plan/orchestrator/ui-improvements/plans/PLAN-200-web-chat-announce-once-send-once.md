# PLAN-200: Web Chat Reads a Reply Once and Sends a Message Once (PLAN-135 Landing Findings L3-1, L3-3, L3-4)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, for the `runbook` lane. Staged 2026-11-28 on the operator's word ("Do both"),
> from `landings/PLAN-135.md` L3-1 (major), L3-3 and L3-4 (minor). Grounded by a read-only sub-agent at `origin/main` `d25d1d76`.
> This spec is SELF-SUFFICIENT. **It adds no dependency.** Line numbers are at `d25d1d76`.

## Objective

PLAN-135 (#598, `bdaf6f26`) built the web chat's core. Its landing analysis found three defects in it:
- **L3-1:** a screen reader reads every finished reply twice, and the newest historic turn on opening a chat;
- **L3-3:** Send on the draft a failed send kept can send the message twice;
- **L3-4:** Try again on an ended or replaced chat loses the Not-sent turn and its text.

This plan fixes the three and nothing else. The proposal announcement, the client notices and the proposal card stay with PLAN-136.

## Deliverables

1. **The finished reply is read once, by the log alone (L3-1)**
 - What:
   - `Chat.svelte:266-281`: an effect finds the last turn that is neither the operator's nor streaming. When its `turn_id` changes it writes into `announcement` (`:75`):
     - `control.chat.stopped` for a stop with no text;
     - the first translated line of a notice with no text;
     - otherwise the reply's raw markdown `completed.text`.
   - `ChatTranscript.svelte:195-197` renders `announcement` in a visually hidden `aria-live="polite"` region beside the `role="log"` (`:212-218`). The log already reads every appended turn. So the reply and the notice are read twice.
   - `previousCompletedId` starts empty (`Chat.svelte:74`). So the newest historic turn of the first snapshot is announced on open.
   - The design: the log and `aria-busy` read "the finished reply once, never word by word" (`doc/design/web-app/chat.adoc:191`). The chat adds no live region for the working line (`:89`). The only catalogue announcement is a new proposal's, `control.chat.proposal.announce` (`chat.adoc:147`, `components-chat.adoc:55`), and PLAN-136 D1 owns it.
   - Remove the reply, notice and stopped branches of the effect. Nothing is written into the region on open, on a completed reply, on a notice or on a stop.
   - The region in `ChatTranscript` stays, empty, for PLAN-136's proposal announcement. `ChatTranscript.svelte` is not changed.
   - No new catalogue key: there is no "reply finished" key, and none is invented.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `Chat.test.ts:353-388` is flipped. It asserts that the region stays empty when a chat opens on a snapshot whose newest turn is a completed reply, a notice or a stopped turn (evidence order: `test-first`).
   - A `Chat.test.ts` case streams a reply to completion. It asserts the reply's text is in the log once and the region stays empty. It fails on a revert of the effect (evidence order: `test-first`).
2. **A message is sent once (L3-3, L3-4)**
 - What:
   - Today `send()` (`pm-web-app/src/lib/chat/store.ts:925-957`) always draws a new `client_turn_id` (`:931`) and appends a new optimistic turn. After a failed send, `postWithTurnId` (`:774-793`) marks the turn `not_sent` and keeps the draft (`chat.adoc:174`, `work-actions.adoc:382`). So the field and the Not-sent turn hold the same text, and Send draws a second id. If only the answer was lost, the message arrives twice. Only `retryTurn` (`:959-972`) reuses the id (`chat.adoc:249`: "Try again after a lost answer never sends twice").
   - **Send of an unchanged text:** when the text sent equals the last Not-sent operator turn's text, `send()` acts as Try again. It reuses that turn's `client_turn_id` and moves that turn back to sending, with no new turn.
   - **Send of an edited text:** the edited text supersedes the Not-sent turn. Remove the turn, as `editFailedTurn` (`:974-986`) does, and send with a new id.
   - `doc/design/web-app/chat.adoc:174` gains one sentence for the two cases, in the same pull request. The design has no word on them today.
   - **Try again on an ended or replaced chat (L3-4):**
     - `retryTurn` calls `postWithTurnId` without a `pendingTurn` (`:971`).
     - `ensureChatOpen` (`:619-633`) opens a new chat.
     - `applySnapshot` merges turns only within the same chat (`:339`), so it drops the Not-sent turn.
     - `resolveSnapshotDraft` clears the draft on the chat change (`:288-293`).
     - So on a failure the text is lost.
     - Fix: pass the retrying turn as `pendingTurn`, and keep it with its text across the chat change. On a failure it is Not sent again, with its text.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `store.test.ts` (beside `:253`) asserts: after a failed send, Send of the unchanged draft posts the first `client_turn_id` and leaves one operator turn; Send of an edited draft removes the Not-sent turn and posts a new id. Each fails on a revert (evidence order: `test-first`).
   - `store.test.ts` asserts that Try again on an ended chat whose re-send fails keeps one Not-sent turn with its text. It fails on a revert (evidence order: `test-first`).
   - `chat.adoc:174` states the unchanged-text and edited-text cases (evidence order: `none`).

## Standing Constraints

- **Build and test through Maven only** (`CLAUDE.md`). At the head:
  - `./mvnw -pl pm-web-app verify`;
  - the full `./mvnw verify`;
  - `./mvnw verify -Ppre-commit`, with every rewrite reviewed and committed.
- **Ports of this plan:** mock web port `22000`, app port `22001` (`AGENTS.md` § Ports). An assigned port in use is a blocker. Never stop or reuse a process this worktree did not start.
- **No new dependency or plugin.** Web standards of `CLAUDE.md`: no timer wait in a test; text only through text-treating sinks.
- **Tests never read documentation** (PLAN-198): no test reads a `.adoc` file.
- **Keep the change minimal:** `Chat.svelte`, `Chat.test.ts`, `store.ts`, `store.test.ts`, `chat.adoc`. `ChatTranscript.svelte` and `ChatTurn.svelte` stay untouched, because PLAN-136 builds on them.
- **ZERO Sonar findings** on the new code of the pull request (SonarCloud `plan-marshall_pm-mcp-clients`), read at the head after the push:
  - State `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`).
  - No `NOSONAR`.
- **The merge rule** (`CLAUDE.md`, Git Workflow): the plan merges its own pull request once the gates hold:
  - every review thread replied to and resolved;
  - one CodeRabbit review on some head;
  - build and Sonar green.
  - Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
  - If CodeRabbit refuses on quota, wait out the window and trigger it once more. Ask the operator only if it still refuses.
- **Other plans:** whichever merges second merges `main` and keeps both changes. Never touch another plan's branch.
  - PLAN-136 shares `Chat.svelte`, `Chat.test.ts`, `store.ts` and `store.test.ts`. It writes the proposal announcement into the region this plan leaves empty.
- **The landing message** states every claim against the merged tree.

## Claim Labels

- OBSERVED: the effect writes the reply, notice or stop into `announcement`, and `previousCompletedId` starts empty — read at `pm-web-app/src/lib/components/Chat.svelte:74-75,266-281`
- OBSERVED: the second polite region sits beside `role="log"` — read at `pm-web-app/src/lib/components/ChatTranscript.svelte:195-197,212-218`
- OBSERVED: `Chat.test.ts:353-388` pins an announcement on open — read at `pm-web-app/src/lib/components/Chat.test.ts:385-387`
- OBSERVED: `send()` always draws a new id; only `retryTurn` reuses one — read at `pm-web-app/src/lib/chat/store.ts:925-972`
- OBSERVED: the design wants the log alone for the reply, and the kept draft with Try again — read at `doc/design/web-app/chat.adoc:89,147,174,191,249`, `doc/design/shared/work-actions.adoc:382`
- HYPOTHESIS: `retryTurn` with a `pendingTurn` keeps the turn across the chat change without touching `applySnapshot`'s same-chat rule — confirm/refute at `store.ts:288-293,339,619-633` (verify-at-outline)

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/Chat.svelte` — present at `d25d1d76`
- OBSERVED: `pm-web-app/src/lib/components/Chat.test.ts` — present at `d25d1d76`
- OBSERVED: `pm-web-app/src/lib/chat/store.ts` — present at `d25d1d76`
- OBSERVED: `pm-web-app/src/lib/chat/store.test.ts` — present at `d25d1d76`
- OBSERVED: `doc/design/web-app/chat.adoc` — present at `d25d1d76`

## Dependencies and Sequencing

- Depends on: `PLAN-135` (landed, #598).
- Overlaps with `PLAN-136` (launched with this plan): the 4 code files above. That is below the five-file block. The regions differ: this plan removes the effect's branches and fixes `send`/`retryTurn`; PLAN-136 adds the proposal card, the notices and the proposal announcement.
- PLAN-62 (running), PLAN-138 and PLAN-198 (launched): no shared file.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-200-web-chat-announce-once-send-once.md
```

## Write-Boundary

The session touches only its own repository source and tests. It creates NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

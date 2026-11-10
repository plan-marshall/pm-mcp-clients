# PLAN-136: Web Chat: Proposals, Notices, Status Cards, the Queued Revision (D4 Web Chat, Part 2)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-135 built the chat's core. This plan adds what the chat does with the subject: the one change it proposes (the proposal
card with its diff and its refusals, the already-queued variant included), every notice the design lists, what happened to
the subject while the chat stays (the runtime's narrative notices and the `knows` line), the status cards that lead to what has
acts of its own (the chat carries out no act but its own proposal), and the plan's queued-revision line in the plan view and
its row. It also closes two residues of the D5 closing: the already-queued variant of the proposal card in the component
document (N2) and the rendering of `control.chat.follow_up.opened` (N6).

## Deliverables

Specification: `doc/design/web-app/chat.adoc` § `#proposal`, `#notices`, `#subject-notices`, `#status-cards`, `#composer` (knows);
`doc/design/web-app/components-chat.adoc` § `#proposal-card`, `#status-card`, `#chat-turn`; `doc/design/shared/work-actions.adoc`
§ `#chat-acts`, `#chat-narrative`, `#chat-ended`; `doc/design/shared/plan-view.adoc` § `#chat-revision`;
`doc/design/web-app/components.adoc` § `#hierarchy`; `doc/design/web-app/theme-readability.adoc`.

1. **`ProposalCard`, the pinned proposal and the refusals**
 - What:
   - New `ProposalCard.svelte`: the title of its kind (`control.chat.proposal.kind`), the summary, when it applies, and the diff
     (`−`/`+` lines on `--color-diff-removed` and `--color-diff-added`, changed words marked within a line, collapsed beyond 12
     lines behind _Show all_). The two diff tokens join `tokens.css`, are valid only inside a proposal's diff (a written
     exception to one colour one meaning) and owe the contrast of `theme-readability.adoc` in both themes; the signs carry the
     meaning without the colour.
   - _Accept_ primary with its check icon (the one exception to one primary per screen, `components.adoc` § `#hierarchy`) and
     _Reject_ secondary, each a `Button` with `loading`; in a decision _Answer in these words_ and in ended work _Start a plan
     with it_ take Accept's place. Accept and reject name the proposal's `turn_id`.
   - Pinned: while open, a compact copy above the composer (`control.chat.pinned`, `.pinned.show`) scrolls to the card; announced
     once (`control.chat.proposal.announce`); decided, it folds into its notice line; replaced, it folds with
     `The earlier proposal was replaced.`
   - Refusals: an accept refused `conflict` for an older version keeps the card open with `control.chat.changed_since` under it,
     Accept disabled with that reason, Reject enabled; a `lapsed` proposal folds into `control.chat.lapsed` (it wins where both
     hold); a revision accepted while a task runs stays a folded card `control.chat.at_boundary` until its applied line;
     **the already-queued variant (N2)**: a plan's revision accepted while another of that plan is queued shows
     `control.chat.already_queued` under its card with Accept disabled and Reject enabled only while the other is queued; when
     that one is not applied (refused at its boundary, `plan.revision_queued` clearing it with the version unchanged) the line goes
     and Accept is enabled again; when it is applied the card shows `control.chat.changed_since`; when it lapses this one lapses.
   - The component document `components-chat.adoc:53` ("Refused for an older version") gains the already-queued variant; `chat.adoc` is
     read for consistency and any line that disagrees is fixed in the same pull request.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ProposalCard.test.ts` covers every state above, including the already-queued variant through its four exits (cleared
     unapplied, applied, lapsed, still queued), and fails on a revert (evidence order: `test-first`).
   - A contrast test of the two diff tokens against their grounds in both themes (evidence order: `test-first`).
2. **The notices**
 - What:
   - Every notice of `chat.adoc` § `#notices` as a centred muted plain-text line, never a turn: `control.chat.infeasible`,
     `lost` with Try again, `too_long_reply` (the text so far kept), `stopped`, `replaced`, `applied`, `rejected`, `check_failed`,
     `at_boundary`, `changed_since`, `lapsed`, `already_queued`, and one for each of the five causes of `chat.ended`
     (`operator`, `proposal_carried`, `answered_elsewhere`, `subject_ended`, `entry_closed`), with the device where one is named.
   - **`control.chat.follow_up.opened` (N6)**: ended work's _Start a plan with it_ is applied by `chat.apply`, never carried; the chat goes
     on and says `A new plan is being started in … with this description; go on with its sources.` with the way to that start;
     `chat.adoc` § `#notices` gains the line.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
 - Done when:
   - A test per notice asserts its catalogue words in en and de; one asserts that a chat ended by `proposal_carried` and one by
     `subject_ended` read differently; one asserts the follow-up line does not end the chat (evidence order: `test-first`).
3. **What happened to the subject, and what the role knows**
 - What:
   - Notice turns with `lines[]`: each line in its catalogue words (`control.chat.line.*`) with its time; up to three shown and
     more folded into `5 changes to fix-relay · Show them` (`control.chat.lines.folded`, a disclosure `Button`, a screen reader reads the
     folded line once); a line that names a decision, a hold or a queued revision carries its status card, every other reference a
     reference chip. Lines that arrive while the chat is hidden count towards the opener's `CountBadge` only where they name a
     decision or a hold.
   - The `knows` line under the head (`control.chat.knows`, `control.chat.knows.*`) from `context`: before any turn what the role
     will be given at its first turn, then the latest turn's, with its time.
   - `ChatStatusCard`: the target's mark and id, its state in the words of its view, kept current by the events its view follows
     (a target no view has loaded is read once from its resource), one act _Open it_ (`control.chat.card.open`) as a `Link` with the
     real address, never the target's own acts; a target that ended keeps the card with its last state, muted. For an act the role
     names (`refs` kind `act`) the card reads `Pause fix-relay — in the acts of fix-relay` and opens the subject with its acts in view.
   - Where it opens: a target of the chat's own subject keeps `chat=open` and replaces the object column's address where today's
     routes allow it; an in-place `open=` form waits for the column engine (O2); a target of another subject is pushed without
     `chat`; in a one-column window every target is pushed without `chat` (`navigation.adoc` § `#side-columns`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests assert the fold at 3 and 4 lines, one status card per decision, hold, act and queued revision, no card for a task, a file
     or a plan, the badge rule, the `knows` line before the first turn and after a turn, and that `ChatStatusCard` never renders an
     answer or a hold act (evidence order: `test-first`).
4. **The queued revision in the plan view and the overview row**
 - What:
   - While `queued_revision` is set, the plan's head says `Revision from the chat queued · applies after TASK-4`
     (`control.chat.revision_queued`; a paused plan `applies when fix-relay starts again`) with the way to the chat that proposed
     it (the address of that chat's subject at the turn, from `subject`, `chat_id`, `turn_id`), or `from an earlier chat` /
     `from the chat of Q-007` and no way where `chat_open` is false (`control.chat.revision_origin`); the overview row carries the same
     words; once applied the version line reads `version 8 · from the chat` (`control.chat.from_chat`); once refused or lapsed the line
     goes. Also the "changed in the chat" mark vocabulary is C3's.
   - The off-catalogue event names in the chat parts of these views are replaced by the catalogue's (`plan.revision_queued`,
     `activity.changed`); the other off-catalogue listeners (`PlanView.svelte:122-133`) are listed in the pull request for PLAN-55 and PLAN-62.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `PlanView.test.ts` and the overview test assert the line in each of the cases above and its removal on each exit (evidence order: `test-first`).
   - Browser check (Playwright MCP) on `running-work` shows the proposal card with its diff (accept, reject, an older-version refusal,
     an already-queued refusal, a lapse), a notice turn folded and unfolded, a status card, and the plan head's line; German and English,
     light and dark, at `1440` and `390` px; baseline and after snapshots and screenshots recorded (evidence order: `none`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a failed read,
     never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (every rewrite reviewed and
  committed), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md` (no `innerHTML`/`{@html}` with model or runtime text, no timer waits,
  the npm scripts stay). The chat renders only catalogue words for runtime-written lines, never a model's text as markup; the diff is
  text nodes. No `style` attribute (CSP). Every new text from the catalogue, en and de.
- Ports: mock web port 21360 (`20000 + 10 × 136`), app port 21361 (`AGENTS.md` § Ports); never stop or reuse a process this
  worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had one
  CodeRabbit review; counts stated from `ci pr comments`; merge on the operator's word only; commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `components-chat.adoc:53` knows only "Refused for an older version"; the already-queued variant is only in `chat.adoc:107` — read at `doc/design/web-app/components-chat.adoc:53`; `doc/design/web-app/chat.adoc:107`
- OBSERVED: `chat.adoc` § `#notices` has no line for `control.chat.follow_up.opened` (it appears in `work-actions.adoc` § `#chat-ended`) — read at `doc/design/web-app/chat.adoc:96-109`
- OBSERVED: `PlanView.svelte` listens for `plan.changed` and other names outside the catalogue — read at `pm-web-app/src/lib/views/PlanView.svelte:122-133`
- OBSERVED: no `--color-diff-removed` or `--color-diff-added` token exists — `git grep -n "color-diff" -- pm-web-app/src` is empty at `1cc0e10e`
- HYPOTHESIS: the plan view's head renders through `WorkControlPanel` and `ActionBar` (PLAN-47), so the queued-revision line is a part of the head slot of `PlanView.svelte` — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte` § head and `pm-web-app/src/lib/components/WorkControlPanel.svelte` (verify-at-outline)
- HYPOTHESIS: the overview row has a slot for a second line of text beside the state — confirm/refute at `pm-web-app/src/lib/views/OverviewView.svelte` § plan row (verify-at-outline)
- HYPOTHESIS: a hold has a store or resource the status card can read without a view of its own — confirm/refute at `pm-web-app/src/lib/api/client.ts` § holds and `pm-web-app/src/lib/stores/runtime.ts` (verify-at-outline)
- Verify-first clause: re-read `chat.adoc` § `#proposal` and § `#notices` at the current HEAD before scoping; a notice added since is added here.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/ProposalCard.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ChatStatusCard.svelte` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Chat.svelte` — created by PLAN-135; extended here
- OBSERVED: `pm-web-app/src/lib/components/ChatTurn.svelte` — created by PLAN-135; extended here
- OBSERVED: `pm-web-app/src/lib/chat/` — created by PLAN-135; extended here
- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/chat.spec.js` — created by PLAN-135; extended here
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at `1cc0e10e`
- OBSERVED: `doc/design/web-app/components-chat.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/web-app/chat.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-135`, `PLAN-134`.
- Overlaps with:
  - `PLAN-45` (running): `PlanView.svelte`, `views/`.
  - `PLAN-55` (parked) and `PLAN-62` (parked): `PlanView.svelte`; they follow this plan and are re-grounded on the queued-revision line.
  - `PLAN-52` (parked): `OverviewView.svelte` (the row line); follows.
  - `PLAN-70` and `PLAN-72` (parked): `tokens.css` (the diff tokens, and their contrast audit); they follow.
- Adjacent to: the plan-view design (`shared/plan-view.adoc`), not edited.

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; check which of PLAN-45, 55, 62 landed.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-136-web-chat-proposals-and-status.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- Parked behind PLAN-135 (D6 side column, D1 building blocks).

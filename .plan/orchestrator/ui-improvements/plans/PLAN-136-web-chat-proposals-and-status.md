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
   - Where it opens (D6 Q5; `navigation.adoc#side-columns` :284, `chat.adoc#status-cards` :164): a target of the chat's own subject
     keeps `chat=open` and replaces the object column's address (a task at `/plans/fix-relay/tasks/TASK-4?chat=open`, where `chat=open`
     is the plan's chat; the queued revision at the plan's head); a target of another subject is pushed without `chat`; in a
     one-column window every target is pushed without `chat`. The in-place forms (`open=`, a decision beside the plan as
     `/plans/fix-relay?open=decision:Q-007&chat=open`) wait for the address model of `PLAN-143`: **until it lands the pushed address
     is the only form** (the decision's own address `/decisions/Q-007` is pushed); once it has landed, closing a decision opened so
     replaces the address with the one it replaced (`open` removed, `chat=open` kept), never a history back. A reference chip opens
     the same way.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests assert the fold at 3 and 4 lines, one status card per decision, hold, act and queued revision, no card for a task, a file
     or a plan, the badge rule, the `knows` line before the first turn and after a turn, and that `ChatStatusCard` never renders an
     answer or a hold act; _Open it_ of the own subject's target replaces and keeps `chat=open`, another subject's target pushes
     without `chat`, and without `PLAN-143` no `open=` address is written (evidence order: `test-first`).
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
     light and dark, at `1440` and `390` px; baseline and after snapshots and screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

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
  CodeRabbit review; counts stated from `ci pr comments`; the run merges itself once the gates hold (no further operator word); commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `components-chat.adoc:53` knows only "Refused for an older version"; the already-queued variant is only in `chat.adoc:107` — read at `doc/design/web-app/components-chat.adoc:53`; `doc/design/web-app/chat.adoc:107`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: components-chat.adoc:53 unchanged; chat.adoc:107 carries control.chat.already_queued (3dea04b2)
- OBSERVED: `chat.adoc` § `#notices` has no line for `control.chat.follow_up.opened` (it appears in `work-actions.adoc` § `#chat-ended`) — read at `doc/design/web-app/chat.adoc:96-109`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: chat.adoc:96-109 has no control.chat.follow_up.opened; it is in work-actions.adoc:468 and work-actions-chat-texts.adoc:111 (3dea04b2)
- OBSERVED: `PlanView.svelte` listens for `plan.changed` and other names outside the catalogue — read at `pm-web-app/src/lib/views/PlanView.svelte:122-133`
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: no plan.changed in non-test src; PlanView refresh list refreshEvents.ts:37-44 holds catalogue names (3dea04b2)
- OBSERVED: no `--color-diff-removed` or `--color-diff-added` token exists — `git grep -n "color-diff" -- pm-web-app/src` is empty at `1cc0e10e`
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: the --color-diff-* tokens exist: tokens.css:105-106,185-186,226-227,270-271; tokens.test.ts:83-84,233-240; PlanChanges.svelte:606,610 (PLAN-164 #477) (3dea04b2)
- HYPOTHESIS: the plan view's head renders through `WorkControlPanel` and `ActionBar` (PLAN-47), so the queued-revision line is a part of the head slot of `PlanView.svelte` — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte` § head and `pm-web-app/src/lib/components/WorkControlPanel.svelte` (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanView.svelte:839 plan-header, :963 WorkControlPanel with ActionBar (3dea04b2)
- HYPOTHESIS: the overview row has a slot for a second line of text beside the state — confirm/refute at `pm-web-app/src/lib/views/OverviewView.svelte` § plan row (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: OverviewView.svelte:1072 plan-row__sub; queued_revision unused in web src (3dea04b2)
- HYPOTHESIS: a hold has a store or resource the status card can read without a view of its own — confirm/refute at `pm-web-app/src/lib/api/client.ts` § holds and `pm-web-app/src/lib/stores/runtime.ts` (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: getPlanHolds client.ts:4800 reads /plans/{id}/holds; no per-plan hold store (3dea04b2)
- Verify-first clause: re-read `chat.adoc` § `#proposal` and § `#notices` at the current HEAD before scoping; a notice added since is added here.
  - verdict: unverifiable | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural; chat.adoc #proposal/#notices also carry at_start (:143,:148), which D1 omits (3dea04b2)

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
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/stores/refreshEvents.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/components/PlanChanges.svelte` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/components/intentEvents.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)

## Dependencies and Sequencing

- Depends on: `PLAN-135`, `PLAN-134`; through `PLAN-135`, `PLAN-142` (the engine's side column; D6: no interim slot). `PLAN-143` is
  soft: the `open=` forms of _Open it_ and the reference chips are written only once it has landed; until then the pushed address is
  the only form.
- Overlaps with:
  - `PLAN-45` (running): `PlanView.svelte`, `views/`.
  - `PLAN-55` (parked) and `PLAN-62` (parked): `PlanView.svelte`; they follow this plan and are re-grounded on the queued-revision line.
  - `PLAN-52` (parked): `OverviewView.svelte` (the row line); follows.
  - `PLAN-70` and `PLAN-72` (parked): `tokens.css` (the diff tokens, and their contrast audit); they follow.
- Adjacent to: the plan-view design (`shared/plan-view.adoc`), not edited.

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; check which of PLAN-45, 55, 62 landed.

## PLAN-161 Landing Residue (Amendment 2026-11-14)

- `control.chat.follow_up.opened` ("go on with its sources") is shown by the bridge with no path to the start: reword it until the contract Open Item lands. Notices with empty text must not draw an empty line.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 5 contradicted: Plan rows (OverviewView:1074-1107 nested, :1120-1157 without-epic) hold title and badges only; no second-line text slot exists today
- Overview plan rows have no second-line slot (`OverviewView:1074-1157`).

## Re-Grounding 2026-11-22 (cleanup, at `7b60cec0`)

Verdicts persisted per claim by `ui-improvements/cleanup` (`corpus set-verdict`, origin/main `7b60cec0`; sibling repos read at their local refs only). Contradicted claims are stamped `rescoped: yes` with the corrections below; read before the outline:

- Claims 2 and 3 contradicted (`rescoped: yes`). Claim 2: `PlanView` no longer listens to `plan.changed` (`plan.change_captured :209`); the process refresh list is `refreshEvents.ts:37-44` (all catalogue names); `plan.revision_queued` is read only via `intentEvents.ts:14`. Claim 3: **D1's token and contrast-test part is DONE**: diff tokens `tokens.css:103-104,183-184,224-225,268-269`, contrast pairs `tokens.test.ts:89-90,239-246`, `PlanChanges.svelte:606,610`, the colour exception `components-tags.adoc:321` (PLAN-164 #477).
- Stale: D3's `open=` conditional and test ("without PLAN-143 no `open=` address is written": `open=` is parsed and written, `routes.ts:146,533`); D4's overview row (PLAN-52 rebuilt it, `OverviewView.svelte:1071-1110`). The `control.chat.*` catalogue is present in `control.{en,de}.properties`; no `ProposalCard`, `ChatStatusCard` or `queued_revision` on the web side. Blocked through PLAN-135 (staged).


## Re-Grounding 2026-11-25

Re-grounded by the cleanup of 2026-11-25 (read-only sub-agent, contradictions spot-checked by the orchestrator) at `3dea04b2`, against the code and the design and specification changes since 2026-11-15: #570 (`20f971fe`: `over_size`, `epic.overlap_found`, `stale`→`unreadable`, the phase `sanity-check`), pm-mcp-core #607/#614 and plan-marshall-documentation #201/#202 ("observe facts instead of predicting them", two rounds), clients #578 (protected project configuration waits for the operator's decision) and the inbox messages `doc-review-2026-11-17-001` and `doc-review-2026-11-24-001`. **This section binds over the body where they differ.**

- Claims 2, 3 contradicted: no `plan.changed` listener exists (refresh list `refreshEvents.ts:37-44`); the `--color-diff-*` tokens and their contrast tests exist (PLAN-164 #477: `tokens.css:105-106,185-186,226-227,270-271`, `tokens.test.ts:83-84,233-240`).
- **D1**: drop the token and contrast part; `ProposalCard` reuses the tokens. Fix the stale restriction of the diff tokens to a chat proposal in `chat.adoc:144`, `components-chat.adoc:51` and `web-app/README.adoc:387` to match `components-tags.adoc:321`. Add `control.chat.at_start` beside `at_boundary` (`chat.adoc:143,148`) with its test.
- **D2**: N6 — the bridge already shows `control.chat.follow_up.opened` (`WorkControlPanel.svelte:764`); add the notices line to `chat.adoc` and the way to the start; coordinate with PLAN-137 (same block).
- **D3**: drop the "without PLAN-143 no `open=` address" conditional and its test (`routes.ts:566-570,628,718`); the PLAN-132 residue is half done (absent channel, `DecisionView.svelte:498-499`): rescope to the absent device and the outcome line, each with a revert-failing test.
- **D4**: the overview row has its second line (`OverviewView.svelte:1072`); remove the "off-catalogue names replaced" and "listed for PLAN-55/62" bullets; add `plan.revision_queued` to the plan view's refresh events.
- Still after PLAN-135.

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

- Parked behind PLAN-135 (D6 side column, D1 building blocks). *2026-11-11:* D6 merged (#403, `8568b56e`): the side column is
  `PLAN-142`'s (`side`, `sideOpen`, `onHideSide` on `TriagePage` and `WizardPage`); the `open=` forms wait for `PLAN-143`, the pushed
  address being the only form until then (deliverable 3 states it). The wait is now on `PLAN-135` and, through it, `PLAN-142`.

## PLAN-132 Landing Residue (Amendment 2026-11-11)

Routed here by the operator (AskUserQuestion, 2026-11-11) from `landings/PLAN-132.md`. `DecisionAnsweredEvent.channel` and `.device`
are now optional (PLAN-132, `572dc739`), but `DecisionView.svelte:375-381` (`closed_elsewhere`) maps an absent channel to "web" and
an absent device to "—". Carry "absent stays absent" to this consumer: no channel names no channel, no device selects the
`decision_answered_no_device` wording (`work-actions-chat-texts.adoc:17`). Done when a `DecisionView.test.ts` case for each absence
fails on revert. Expected surface added: `pm-web-app/src/lib/views/DecisionView.svelte`, `pm-web-app/src/lib/views/DecisionView.test.ts`.

## PLAN-134 Landing Residue (Amendment 2026-11-14)

PLAN-134 landed (#428, `7adb56c2`). The facts this plan builds on:
- The mock emits **notice turns** and `chat.turn.started.context`.
- The plan and its overview row carry `queued_revision`.
- Refusals arrive as `chat.finding.changed_since`, `already_queued` and `check_failed`.

Two things are still open, and the operator decided both on 2026-11-14. PLAN-140 D-R134 takes them:
- REFUSED stays **rejectable**: the UI offers reject on a refused proposal.
- Conflict and lapse get a catalogue line, emitted as a notice (named first in pm-mcp-core `state-catalogue.adoc`).

The scenario files carry no narrative and no decision chat yet; PLAN-140 D-R134 adds them. Until then, the browser
check drives events through `/mock/broadcast`. Re-ground :38-44 and :69-77 against `7adb56c2`.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-135` lands.
- Re-ground at outline: the Overview rows have no second-line slot (claim 5).
- The scenario files carry no narrative until `PLAN-140` D-R134; until then, drive the browser check through `/mock/broadcast`.

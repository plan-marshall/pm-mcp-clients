# PLAN-137: Web Chat in Its Places and the End of ChatPanel (D4 Web Chat, Part 3)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The AGREED design shows the same `Chat` in every place the operator talks to a role: a decision, the start, the decomposition,
the review of an epic's findings and watches, a project's setup question, and ended work. The web app has a different chat in
each of them, all drawn by `ChatPanel`, and the D5 defects live there (a decision's answers disabled the whole time its chat is
open, a review chat that ignores the item and never ends on the server). This plan puts `Chat` into those places, through the
side column `PLAN-142` builds on `WizardPage` and `TriagePage` (D6: no side column of the chat's own), and deletes `ChatPanel` and
the old chat client. The plan's and the epic's own chat acts were
done by PLAN-135.

## Deliverables

Specification: `doc/design/web-app/chat.adoc` § `#everywhere`, `#placement`, `#live`; `doc/design/web-app/navigation.adoc`
§ `#side-columns`, `#scope`; `doc/design/web-app/components.adoc` § `#templates`; `doc/design/shared/decisions.adoc` § `#chat`, `#setup`;
`doc/design/shared/starting-work.adoc` § `#chat`; `doc/design/shared/work-actions.adoc` § `#chat-places`, `#chat-ended`, `#review`;
`doc/design/shared/project-onboarding.adoc`.

1. **The chat in `WizardPage`'s side column; the start's and the project setup's chat**
 - What:
   - No side slot of this plan's own (D6 Q5). The wizard views pass `Chat` to `WizardPage`'s `side`, `sideOpen` (from `chat=open`)
     and `onHideSide` (_Hide the chat_ and `Escape`, a replace without `chat`), which `PLAN-142` builds with the API of
     `components.adoc#triage-page` (:374) and `#templates` (:361). Width (`480`, or `400` where `480` would drop the wizard's content
     below `480`), the take-the-space rule, _Hide the chat_ returning to the wizard's content, and the phone (the chat takes the screen
     and the bottom bar) are the template's, measured on its container; this plan asserts them in its views' tests.
   - The start's chat (`start.sources`, `start-epic.sources`): `refine` for a plan, `decompose` for an epic, not for a recipe, offered
     once a source is stored and each source from outside the machine is screened; proposals are a rewrite of or an addition to the
     description, stored through `start.store-description`, never another source; keys `control.chat.*`, never `start.*`.
   - The project onboarding's drawer (`ProjectOnboardingView.svelte:689` draws its own turns) and the decision chat of a question of the
     project's setup become `Chat` in the side column of the setup step; a question the setup's workflow raised itself has no role and says so
     (`decision.chat.no_role`).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `StartView.scenarios.test.ts` and `ProjectOnboardingView.test.ts` assert the chat in the wizard's side column (`sideOpen` from
     `chat=open`, hidden by `onHideSide`), the offering rule, a recipe without chat, an untrusted source marked, and the stored
     description (evidence order: `test-first`).
2. **The decomposition chat and the epic review chat**
 - What:
   - The decomposition draft's chat and the review chat of one item or of the whole register as `Chat` with the scope the design names
     (the draft, the item, the register); an accepted change becomes the item's disposition or rewrite, or a change of the draft; a draft
     row or a register item changed in the chat is marked `changed in the chat` (`control.chat.changed_in_chat`) until the decomposition
     or the review is committed, the same on every device; the chat ends on the server when the review ends.
   - Addresses of these two chats follow the operator's answer to O10; until then this deliverable is blocked.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests assert the item scope is sent (today it is ignored), the mark appears and goes on commit, and the chat ends on the server with
     the review (evidence order: `test-first`).
3. **The decision chat**
 - What:
   - `Chat` beside the decision, in the column engine's side column (`TriagePage`'s `side`, `PLAN-142`); a decision's own chat opens at
     the decision's own address (`/decisions/Q-007?chat=open`), never as the chat of a decision opened in place; the decision stays visible and answerable, its answers disabled only while the role writes a reply or a
     proposal waits for Accept or Reject, with the visible line that says why and the act that opens the chat (`web.chat.answers_wait`),
     enabled otherwise (today `DecisionView` disables them the whole time a chat is open). Answering closes the decision and ends its chat
     with it; a decision answered elsewhere ends the chat with `decision.chat.closed_elsewhere`; a waiver request offers no chat.
   - _Answer in these words_ ends the chat and sends the words to `decision.interpret`; the read-back shows in the decision's column, never
     in the chat. The role follows who asked (`implement`, `replan`, `decompose`). Another decision a notice names is a status card.
   - The web's answer sends no channel (the runtime derives it).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `DecisionView.test.ts` asserts: answers enabled while the chat is open and idle, disabled with the line while a reply is written and
     while a proposal waits, enabled again after; answering ends the chat; answered elsewhere ends it with the notice; the waiver has no chat
     (evidence order: `test-first`).
4. **Ended work and the follow-up plan**
 - What:
   - The ended plan's and the ended epic's chat (`replan`, `decompose`; from the row act and the ended view): the composer works (read
     only means that the archive never changes), the one proposal is the follow-up plan, _Start a plan with it_ opens the start of a new plan
     in the same project with the description stored and the chat goes on (`control.chat.follow_up.opened`, rendered by PLAN-136); the
     transcript is kept beside the archive entry.
   - The row act "Chat about it" of the ended work table stays PLAN-52's.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - A test asserts the composer is enabled, the archive is untouched by a turn, accepting the follow-up opens the start with the description
     and does not end the chat (evidence order: `test-first`).
5. **`ChatPanel` and the old chat client are gone**
 - What:
   - Delete `ChatPanel.svelte` and `ChatPanel.test.ts`; delete the old chat types and methods of `client.ts` (`ChatTurn`, `ChatProposal`,
     `ChatReadBack`, `getChat`, `chatWithSubject`, `answerSubjectChatProposal`, `endChat`, the decision and entry chat methods, `getEndedChat`
     as they are replaced) and the `start.chat.*` keys nothing uses; the e2e specs `control.spec.js`, `decisions.spec.js`, `workspace.spec.js`
     and `start.spec.js` follow.
   - Browser check in German and English, light and dark, at `1440`, `1024` and `390` px for every place.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `git grep -n "ChatPanel" -- pm-web-app pm-web-e2e` is empty (evidence order: `none`).
   - Browser check (Playwright MCP) shows each place (the start, the project setup, the decomposition, the review, a decision, ended work) on
     `projects-and-start`, `running-work` and `fully-onboarded`; baseline and after snapshots and screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (every rewrite reviewed and committed),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`; no `style` attribute; every new text from the catalogue, en and de; the chat never
  borrows a `start.*` key; the text guard baseline does not grow.
- The views are edited for the chat only; their redesign belongs to the parked plans (PLAN-51, 53, 55, 58, 59, 52). Keep each edit to the
  chat's slot, its wiring and the lines named above, and say in the pull request which lines of each view changed.
- Ports: mock web port 21370 (`20000 + 10 × 137`), app port 21371 (`AGENTS.md` § Ports); never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has had one CodeRabbit
  review; counts stated from `ci pr comments`; the run merges itself once the gates hold (no further operator word); commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`;
  no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `DecisionView.svelte` keeps `chatOpen`, `chatBusyId`, `chatBusyToken`, `chatGeneration` and reacts to `decision.created`, `decision.opened`, and the chat close lines — read at `pm-web-app/src/lib/views/DecisionView.svelte:52-69,320-394,504,629`
- OBSERVED: `ProjectOnboardingView.svelte` draws the setup question chat's turns itself (`chat-turn`, `chat_available`, `chatOffered`) — read at `pm-web-app/src/lib/views/ProjectOnboardingView.svelte:77,689`
- OBSERVED: `EpicView.svelte` renders `ChatPanel` for the review chat — read at `pm-web-app/src/lib/views/EpicView.svelte:973`
- OBSERVED: `StartView.svelte` renders `ChatPanel` with `entry?.chat_available` — read at `pm-web-app/src/lib/views/StartView.svelte:912,1116,1125`
- OBSERVED: `WizardPage.svelte` has the slots `steps`, default, `back`, `secondary`, `primary` and no side slot — read at `pm-web-app/src/lib/templates/WizardPage.svelte`
- OBSERVED: D6 fixed the side column on `TriagePage` and `WizardPage` (`side`, `sideOpen`, `onHideSide`, built by `PLAN-142`), none on `ObjectPage` and none in the chat plans; `open=` forms wait for `PLAN-143`; a decision's own chat is at `/decisions/{id}?chat=open` — read at `doc/design/web-app/components.adoc:361,374,381`, `doc/design/web-app/navigation.adoc:275-284,383-418`, `doc/design/web-app/chat.adoc:40,342` (`4a84f727`, D6 merged `8568b56e`)
- HYPOTHESIS: each place's view sits on `WizardPage` (the start, the decomposition, the project's setup step) or on `TriagePage` (a decision, the epic's review, ended work) at the then-current HEAD, so its `Chat` has a `side` to go into — confirm/refute at each view's template use (verify-at-outline; a place whose view is on neither stops and loops back for that place: no interim slot)
- OBSERVED: the docs define no route for the decomposition draft's chat or for a review chat — read at `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc:166-168,280`
- HYPOTHESIS: the decision answers are disabled while a chat is open at `DecisionView.svelte` ~1011, 1058, 1161 (lines from `plan-125-001.md`, 2026-11-09) — confirm/refute at `pm-web-app/src/lib/views/DecisionView.svelte` § `isChatting` (verify-at-outline)
- HYPOTHESIS: the review chat ignores the item at `EpicView.svelte` ~472 and never ends on the server at ~506 (same source) — confirm/refute at `pm-web-app/src/lib/views/EpicView.svelte` § review chat (verify-at-outline)
- HYPOTHESIS: `EpicDecompositionView.svelte` and `DecompositionReviewView.svelte` have no chat today — confirm/refute at those files § chat (verify-at-outline)
- HYPOTHESIS: the web decision answer sends no `channel` — confirm/refute at `pm-web-app/src/lib/api/client.ts` § `answerDecision` (verify-at-outline)
- Verify-first clause: re-read every view line above at the then-current HEAD (PLAN-45 is running and edits `views/`; PLAN-51, 53, 55, 58 and 59 may have landed since); a view already restructured by a landed plan is edited at its new lines.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/templates/WizardPage.svelte` — present at `1cc0e10e` (read only: its `side`, `sideOpen`, `onHideSide` are `PLAN-142`'s)
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/StartView.scenarios.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/DecompositionReviewView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` — present at `1cc0e10e`; deleted by this plan
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.test.ts` — present at `1cc0e10e`; deleted by this plan
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/startFixtures.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/start.spec.js` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-136` (and through it C1, B3); `PLAN-142` (the side column on `WizardPage` and `TriagePage`; D6); the operator's answers to O3 (who adopts the chat in the views) and O10 (the two chat addresses). `PLAN-143` is soft: the `open=` forms of status cards and reference chips are written only once it has landed; until then the pushed address is the only form.
- Overlaps with:
  - `PLAN-51` (parked): `DecisionView.svelte`; this plan supersedes its deliverable 6 ("chat guards") and the chat line of its design-review amendment.
  - `PLAN-53` (parked): `StartView.svelte`; its chat line of the amendment is superseded.
  - `PLAN-55`, `PLAN-58`, `PLAN-59` (parked): `PlanView`, `EpicView` and `FindingsReviewModal`, `ProjectOnboardingView`; their chat lines are superseded, their redesign follows this plan.
  - `PLAN-52` (parked): `OverviewView.svelte` ("Chat about it" row act stays its own).
  - `PLAN-45` (running): `views/`.
- Adjacent to: `client.ts` is edited only for chat methods; the non-chat off-catalogue listeners (`StartView.svelte:186-190`) are listed for PLAN-53.

## Re-Grounding at Restart

Re-read every named view at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; check which of PLAN-45, 51, 53, 55, 58, 59, 52 landed and adjust the call sites.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-137-web-chat-places.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption (O3):** this plan replaces every `ChatPanel` call site and the onboarding drawer, then deletes `ChatPanel`; the parked view plans only place `Chat` in their layouts.
- **Review and decomposition chats (O10):** own addresses, as PLAN-132 records.
- **Side column (O2):** per the D6 design session. *Settled 2026-11-11:* D6 merged (#403, `8568b56e`): no interim slot; the chat
  goes into `PLAN-142`'s `side` on `WizardPage` and `TriagePage` (`ObjectPage` has none); this plan builds no side slot on
  `WizardPage` (deliverable 1 is rewritten accordingly; `WizardPage.test.ts` left the surface); `480`/`400` are measured on the
  container; on a phone the chat takes the screen and the bottom bar.

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the views package parts this plan waits for are PLAN-155 (decomposition and start sub-steps) and PLAN-156 (modals and pickers).

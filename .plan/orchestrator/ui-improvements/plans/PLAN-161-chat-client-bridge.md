# PLAN-161: The Web and Terminal Chats Talk to the Chat Contract Again (PLAN-133 Residue)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-13
> ("New bridge plan now") from `landings/PLAN-133.md` (PR #424, squash `a139ec62`). This spec is SELF-SUFFICIENT.

## Objective

PLAN-133 moved the mock runtime onto the B1 chat contract (`pm-api` `ChatRoutes`, `ChatResource`, `ChatTurnRequest`,
`ProposalAnswerRequest`). A turn now carries `client_turn_id`, answering a proposal carries its `turn_id`, a turn is answered with
`202 {turn_id}`, and a read returns a `ChatResource`. The start state no longer carries a `chat` member. The web app and the terminal still
speak the old shape. Against the mock on `main`, these chats therefore fail: the entry chat, the plan chat, the epic chat, the review chat and
the ended-work chat. The decision chat address is unserved.

This plan is a BRIDGE. It makes the EXISTING client calls and views speak the B1 contract with the smallest change that works. It does not
build the new chat client (`PLAN-135` D1, `lib/chat/`), the new views (`PLAN-135`, `PLAN-137`) or the new terminal dialogue (`PLAN-138`,
`PLAN-140`). Those plans later replace what this plan adapts. Keep the adaptation thin and in one place per client, so that their deletion
is one step.

## Deliverables

The contract is the specification. Read first:
- `pm-api/src/main/java/de/planmarshall/api/chat/ChatRoutes.java`, `ChatResource.java`, `ChatTurnRequest.java`, `ProposalAnswerRequest.java`.
- The mock's `ChatRegistry` (`pm-mock-runtime/.../runtime/ChatRegistry.java`, routes around `:1141-1170`).
- `doc/design/shared/chat.adoc` and `doc/design/shared/work-actions.adoc` § "A Chat About Ended Work".
- `doc/design/web-client/` for the chat panel.

Line numbers are as read at `a139ec62`. Re-read every one at HEAD before editing.

1. **The web client calls speak B1**
 - What:
   - Adapt `pm-web-app/src/lib/api/client.ts` so that every chat call uses the `ChatRoutes` address and the B1 bodies:
     - Turn calls send `client_turn_id`, drawn per send and kept for the retry: `chatTurn` (`:2349-2385`, entry) and `chatWithSubject` (`:3484-3500`, plan/epic).
     - The proposal-answer calls send the proposal's `turn_id`: `answerChatProposal`, `answerSubjectChatProposal`, `acceptFollowUp`.
     - The end calls use the B1 end act: `endEntryChat` and the subject equivalents.
     - The reads (`getChat`, `getEndedChat`) parse a `ChatResource`: `id`, `subject{}`, `role{name,model}`, and turns with `author`/`status`.
   - Keep the existing view-facing types (`ChatReadBack`, `ChatTurn`, `client.ts:672-681`, `:1567-1572`) as the views' input, and map from `ChatResource` in ONE normaliser. Alternatively, change the types and their few readers.
   - A turn returns `202 {turn_id}`. The view then reads the chat again, or follows its events, until the reply turn is complete. Track progress by turn id and status, NOT by a count of turns (PLAN-133 lesson: replies are streamed and the transcript is paged).
   - Decision chat (`client.ts:3549-3594`, `/api/v1/decisions/{id}/chat…`): the mock serves a decision chat under its scope (plan, epic, entry, workspace, machine).
     - Verify first whether `DecisionView` knows the decision's scope from the decision resource. If it does, address the chat there.
     - If it does not, keep today's "no chat" notice and say so in the pull request. Do not invent a route.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `client.test.ts`: each chat call sends the B1 body and address, and parses a `ChatResource`. A body without `client_turn_id` / `turn_id` fails the test. Each fails on a revert. Evidence order: `test-first`.

2. **The web views open and run their chats against the mock**
 - What:
   - Entry chat (`StartView.svelte:104`, `:465-475`): the view reads `state?.chat` and calls `act('chat')`. The mock refuses that act (`StartRegistry.java:616-620`) and the state has no chat member. Open the entry chat through its `ChatRoutes` address instead.
   - Plan and epic chat (`WorkControlPanel.svelte:684,695`), review chat (`EpicView.svelte:489,500`): run on the adapted calls of D1.
   - Ended work (`WorkControlPanel.svelte:736`): accepting the follow-up proposal opens the new start. The answer is a `ChatResource` now, not a `StartStateResource`, so find the new entry the way the contract offers it. Verify first: a `start.*` event, the subject's resource, or a link. If none names the entry, stop and ask. `endedRole` takes `role.name`.
   - Update the unit tests that pin the old shapes: `StartView.test.ts`, `StartView.scenarios.test.ts`, `WorkControlPanel.test.ts`, `EpicView.test.ts`, `DecisionView.test.ts`. Rule: change the expectation to the B1 shape. Never delete a case without naming its replacement.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:css
 - Done when:
   - Unit tests of the four views drive a chat open → send → reply → proposal accept on B1 fixtures. Each fails on a revert. Evidence order: `test-first`.
   - `pm-web-e2e/tests/showroom.spec.js:65-80` passes: the entry chat opens on `ent-showroom` and shows the seeded proposal with `fsynced`. If `PLAN-148` marked that step `test.fixme`, remove the mark. Run with `-Pe2e` at the head. Evidence order: `none`.
   - Browser check against the mock (`fully-onboarded`, `running-work`) at 1280 × 800 and 390 × 844, in both themes:
     - the entry chat;
     - a plan chat with a streamed reply;
     - an epic chat;
     - the ended-work follow-up opening a new start;
     - the console is read from load, with no error.

     Screenshots are kept in the plan's working area and re-taken after any later visual fix. The pull request lists them after the push; they are not a gate before it. Evidence order: `none`.

3. **The terminal's plan, epic and ended-work chat speak B1**
 - What:
   - `plan chat`, `epic chat` and the ended-work chat (`WorkChatDialogue`, `WorkActionsClient.java:305-347`) POST `{"message":…}` and parse `ChatReadBackResource`. The mock now answers `202` and a `ChatResource`.
   - Make `WorkActionsClient` send `ChatTurnRequest` / `ProposalAnswerRequest` to the `ChatRoutes` addresses, and read `ChatResource`.
   - Follow the reply until it is complete, by turn id and status. Bound the wait, and check the interrupt flag.
   - `ActionSupport.java:380` and `ReviewDialogue` read `ChatReadBackResource`. Move them to `ChatResource` if they read the chat. Delete `ChatReadBackResource` only if nothing uses it afterwards.
   - The terminal's texts stay as they are. New texts go in en and de.
   - `PLAN-138` rebuilds this dialogue later. Do not redesign it here.
   - The tests ran on `FakeRuntimeServer`/`ControlFixture` with the old shape and stayed green. Move their fixtures to the B1 shape. Add one `*IT` against the mock runtime that runs a plan chat turn end to end.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `ControlDialoguesTest` and the new IT send the B1 body and render the reply. An old-shape body fails the IT. Each fails on a revert. Evidence order: `test-first`.
   - A terminal check of `plan chat <id>` on `running-work`, en and de, saved as `pm-operator/src/test/resources/screen-captures/PLAN-161.txt`. Evidence order: `none`.

4. **Consumers listed, nothing left behind**
 - What:
   - Before the first edit, list every consumer call site of the old chat shapes with a content search over `pm-web-app/src`, `pm-web-e2e/tests`, `pm-operator/src` and `pm-relay/src`. Search for these names: the old routes, `chat/turns`, `{text}` bodies, `ChatReadBackResource`, `StartChatResource`, `state.chat`, `act('chat')`.
   - Put the list in the pull request body, each entry with its disposition: migrated here, or owned by a named plan.
   - PLAN-133's own survey ("pm-web-e2e has no chat reference") was wrong. Do not rely on it.
   - Extend `doc/design/` only if an address in it is wrong for B1.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-java:java-core
 - Done when:
   - The pull request body carries the list. `git grep` at the head finds no old-shape call outside the list's "owned by" rows. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head these pass:
  - `./mvnw -pl pm-api,pm-mock-runtime,pm-operator,pm-web-app verify`
  - `./mvnw -pl pm-operator verify -Pintegration-tests`
  - `./mvnw verify -Ppre-commit`, with every rewrite reviewed and committed
  - `PM_MOCK_WEB_PORT=21610 PM_WEB_PORT=21611 ./mvnw -pl pm-web-e2e verify -Pe2e`
- No new dependency or plugin.
- Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `{@html}`/`innerHTML` with runtime or model text, no timer waits in tests.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; expected wire and catalogue text stays literal.
- Never catch or throw generic `Exception`/`RuntimeException`; no compatibility shims beyond the single normaliser named in D1.
- Ports of this plan: per `AGENTS.md` § Ports (`20000 + 10 × 161`): mock web port 21610, app port 21611. Never stop or reuse a process this worktree did not start. Never run `pm-operator` in the agent's own shell: use `ScreenCaptureIT`.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, "additional comments") answered on the pull request. Read the state from GitHub's thread state (`isResolved`, a reply present), never from a count kept in memory. An `unknown` count blocks the merge.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow).
  - Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues and 0 hotspots to review, read at the pull request head after the push (`unknown` for a failed read, never `0`). Every open issue is fixed in a commit of THIS pull request. A dismissal needs the operator's word. This is a merge gate, not a deliverable's Done-when.

## Claim Labels

- OBSERVED, by the PLAN-133 landing analysis at `a139ec62`, spot-checked by the orchestrator:
  - `client.ts:3484-3500` `chatWithSubject` posts `{ text }`.
  - `showroom.spec.js:76-80` clicks `source-chat` and expects `chat-proposal` with `fsynced`.
  - `StartRegistry.java:616-620` has no `chat` act.
- OBSERVED, from the analysis only: `ChatTurnRequest.java:92-98` refuses a body without `client_turn_id`; `ChatRegistry.java:1160-1170` serves decision chats under scopes only; `WorkActionsClient.java:305-347`; `WorkControlPanel.svelte:684,695,736`; `EpicView.svelte:489,500`; `StartView.svelte:104,465-475`.
- HYPOTHESIS: the decision resource names its scope (verify-at-outline, D1).
- HYPOTHESIS: the answer to an accepted follow-up proposal, or an event, names the new start entry (verify-at-outline, D2).

## Expected Surface

- OBSERVED (present at `a139ec62`):
  - `pm-web-app/src/lib/api/client.ts`
  - `pm-web-app/src/lib/api/client.test.ts`
  - `pm-web-app/src/lib/api/client.normalise.test.ts`
  - `pm-web-app/src/lib/views/StartView.svelte`
  - `pm-web-app/src/lib/views/StartView.test.ts`
  - `pm-web-app/src/lib/views/StartView.scenarios.test.ts`
  - `pm-web-app/src/lib/components/WorkControlPanel.svelte`
  - `pm-web-app/src/lib/components/WorkControlPanel.test.ts`
  - `pm-web-app/src/lib/views/EpicView.svelte`
  - `pm-web-app/src/lib/views/EpicView.test.ts`
  - `pm-web-app/src/lib/views/DecisionView.svelte`
  - `pm-web-app/src/lib/views/DecisionView.test.ts`
  - `pm-web-e2e/tests/showroom.spec.js`
  - `pm-operator/src/main/java/de/planmarshall/operator/WorkChatDialogue.java`
  - `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java`
  - `pm-operator/src/main/java/de/planmarshall/operator/ActionSupport.java`
  - `pm-operator/src/main/java/de/planmarshall/operator/ReviewDialogue.java`
  - `pm-operator/src/test/java/de/planmarshall/operator/ControlDialoguesTest.java`
  - `pm-api/src/main/java/de/planmarshall/api/control/ChatReadBackResource.java` (deleted only if unused)
  - `pm-api/src/main/java/de/planmarshall/api/chat/ChatRoutes.java` (read)
- OBSERVED (absent at `a139ec62`; created by this plan):
  - a new `pm-operator` `*IT` for the plan chat
  - `pm-operator/src/test/resources/screen-captures/PLAN-161.txt`

## Dependencies and Sequencing

- Depends on: `PLAN-133` (shipped, #424, `a139ec62`).
- Overlaps with:
  - `PLAN-148` (running): `showroom.spec.js` (its `test.fixme` of the chat step). The second to land rebases.
  - `PLAN-134` (staged, may run in parallel): mock `ChatRegistry` and the scenario fixtures. This plan changes no mock code. If the contract needs a mock change, stop and ask.
  - `PLAN-30` (staged): `operator.*.properties`, `LiteralTextGuardTest` names `ChatDialogue`/`StartDialogue`.
- Replaced later by: `PLAN-135` D1/D5, `PLAN-137` D1–D5 (web) and `PLAN-138` D1, `PLAN-140` (terminal). Those plans delete what this plan adapts.

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check whether `PLAN-148` landed its `showroom.spec.js` fixme.
- Check whether `PLAN-134` changed the chat routes or fixtures.
- Settle each HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-161-chat-client-bridge.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

# PLAN-134: Chat in the Mock Runtime: Context, Narrative, Queued Revision (D5, Part 3)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D5
> documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D5 (PLAN-125, AGREED, residue closed 2026-11-10) added what the chat knows (`context`), the subject's narrative as notice
turns, status refs, and the plan's queued revision with its refusals (`conflict` for an older version and for a revision
already queued, `lapsed`). This plan makes the mock play all of it on top of PLAN-133, so the web and terminal chat packages
can be built and tested against it. It also makes the mock record the answer channel from the listener.

## Deliverables

Specification: `doc/design/web-app/chat.adoc` § `#subject-notices`, `#status-cards`, `#composer` (knows), `#contract`;
`doc/design/terminal-client/28-chat.adoc`; `doc/design/shared/work-actions.adoc` § `#chat-knows`, `#chat-acts`,
`#chat-narrative`; `doc/design/shared/plan-view.adoc` § `#chat-revision`; `05-mock-runtime.adoc` § `#mock-timing` (the D5 text);
`06-event-catalogue.adoc` (the `plan.revision_queued` row, `activity.changed`); core `state-catalogue.adoc` § `#chat-context`,
`#chat-narrative`, `#chat-proposal-states`, `#chat-origin`.

1. **What the role knows**
 - What:
   - `context` (`knows[]` as message keys `chat.knows.*` with their arguments, and `as_of`) rendered from the mock's subject
     data in the answer of `POST …/chat` and `GET …/chat` before any turn, and refreshed by every role turn:
     `chat.turn.started` of a role's turn carries the context for that turn; a turn whose worker is primed anew changes it
     (a task's status, the phase, the version).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert the context before the first turn, after a turn on a changed subject, and in `chat.turn.started` (evidence
     order: `test-first`).
2. **The subject's narrative as notice turns**
 - What:
   - For every event a scenario plays about the chat's subject (a task ended, the phase changed, a decision opened or answered,
     a hold opened or cleared, a pause, a new version, the pull request merged) the chat gets a `notice` turn with
     `lines[]` between two replies, never inside one being written; several lines that come at once are one notice. Refs
     of the kinds `hold` and `act` appear on the lines and on a role's reply where scripted.
   - `/mock/broadcast` of a subject event produces the same notice. Ended work has no narrative.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test plays a scenario with five events at once and asserts one notice with five lines; another asserts no notice
     arrives inside a streaming reply and the notice waits for its end (evidence order: `test-first`).
3. **The queued revision**
 - What:
   - Accepting a plan's revision while a task runs makes the proposal `accepted` and the plan's `queued_revision` (`chat_id`,
     `turn_id`, `subject` with `kind` and `id`, `chat_open`, `applies`), announced by `plan.revision_queued`; the overview's
     plan row and `activity.changed` carry it. At the boundary it is `applied` (the plan's version moves, the change is shown
     with `origin` `{kind: chat, chat_id, turn_id}`), `refused` or `lapsed`, each announced by `plan.revision_queued`
     clearing `queued_revision`; the event is also sent when the chat that proposed it ends (`chat_open` turns false), as
     `06-event-catalogue.adoc` states.
   - A revision queued from a decision's chat has the decision as its `subject`, and its `chat_open` turns false when that
     decision is answered and its chat ends.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests assert the event order for accept, apply at the boundary, refusal at the boundary, lapse, the chat ending while
     queued, and the decision case; each asserts the overview row and `activity.changed` (evidence order: `test-first`).
4. **Refusals**
 - What:
   - An accept against an older version of the plan is refused `conflict` (the card stays, its reject is taken); an accept
     while another revision of that plan is queued is refused `conflict` the same way, and is possible again if the queued
     revision is refused or lapses, while an applied one makes the proposal one of an older version; a proposal whose plan
     left the revision window is `lapsed` and wins where both hold; another subject's proposal is refused by its own checks;
     an `act` ref naming an act the subject does not offer is refused `invalid_arguments`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - One test per refusal asserts the status, the proposal's state, the notice and that the reject is still taken
     (evidence order: `test-first`).
5. **The answer channel and the scenarios**
 - What:
   - The mock records an operator act's channel from the listener (the Unix socket is `cli`, a web listener is `web` with
     the device id), never from a request default (the fix of PLAN-132 deliverable 5), also for a decision answered after a
     chat (`after_chat`). `/mock/acts` shows it.
   - The scenarios (`running-work`, `fully-onboarded`) carry a plan with a running task and the events above, a decision chat
     and an ended plan; the scenario files stay valid against the load validation.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - A web-listener test and a socket test assert the recorded channel; the full build and integration tests pass at the head
     (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`, `-Ppre-commit`
  (every rewrite reviewed and committed), `-Pintegration-tests`.
- No new dependency or plugin; `jackson-core` only in `pm-api`; no Quarkus. Never catch or throw generic
  `Exception`/`RuntimeException`; package-private over public; new texts en and de.
- Tests: JUnit 5, no Mockito; input from `Generators` with `@EnableGeneratorController`; golden payloads literal.
- Ports: mock web port 21340 (`20000 + 10 × 134`), app port 21341 (`AGENTS.md` § Ports).
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

- OBSERVED: the mock sends events with `broadcastEvent` and applies the state effect of `decision.*` events only — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:6862-6902`
- OBSERVED: the plan resource and overview row have no `queued_revision` in `pm-api` today (the contract adds it in PLAN-132) — read at `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` (absence of the field)
- OBSERVED: the design says `plan.revision_queued` is sent when a revision is queued, when the chat that proposed it ends, and when it leaves the queue — read at `../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc:63`
- OBSERVED: core `state-catalogue.adoc:132` and docs `01-processes-and-transport.adoc:359` omit the chat-end send; this plan follows the event catalogue and the three places that state it, and the two lines are an out-of-repo doc fix (N3) — read at those lines
- HYPOTHESIS: the mock's plan data has a version and a current task from which `knows[]` can be computed without a new fixture — confirm/refute at `pm-mock-runtime/src/main/resources/scenarios/running-work.json` § plans (verify-at-outline)
- HYPOTHESIS: the engine can tell a request's listener (socket or web) at the point it records `/mock/acts` — confirm/refute at `MockRuntimeEngine.java` § `handleControlActs` and `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpRequest.java` (verify-at-outline)
- Verify-first clause: re-read the D5 text of `05-mock-runtime.adoc` § `#mock-timing` and the event catalogue at the current HEAD of `plan-marshall-documentation` before scoping; PLAN-133 and PLAN-45 must have landed.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatRegistry.java` — created by PLAN-133; extended here
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `1cc0e10e` (hook lines only)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockHttpRequest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/ScenarioStateContractTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-133` (and through it `PLAN-132`, `PLAN-45`).
- Overlaps with: the same engine hook lines and scenario files as `PLAN-133` (sequenced after it); `PLAN-45` (running).
- Adjacent to: core `chat-narrative` and `chat-context` packages (pm-mcp-core, not this repository).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; re-read the event catalogue row of `plan.revision_queued`.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

Operator decision 2026-11-13 (recommended split). Re-ground at outline against `a139ec62`:

- D1 is partly done: `ChatRegistry.context(address)` already writes `knows` for description, archive and decision, and `as_of`. Keep the
  133 states: `ProposalState.ACCEPTED` for a revision, `APPLIED` otherwise, the `ChatRegistry.observe` hook and the seed validation.
- **D-new-1 (m2):** `MockRuntimeEngine.applyChatProposal` (`:874-890`) only increments `revision` for an accepted row or draft proposal;
  `before`/`after` are not applied. Spec of PLAN-133 D4 says the change is "applied and shows in the subject's resource". Apply it;
  `ChatProposalsTest.java:216-232` ("moves on by one revision") becomes a check of the applied value. Test-first, fails on a revert.
- **D-new-2 (m3):** `starting-work.adoc:148` says abandoning an entry deletes its transcript; `ChatRegistry.java:691` ends it as
  `ENTRY_CLOSED` and keeps it, and no test pins either. Implement the design (delete) with a test, or, if the design is wrong for B1, stop
  and ask the operator before changing the document.
- **D-new-3 (m4):** `ChatRegistry.java:541-548` `playFailure` checks `isCurrent` under the lock and calls `replaceWorker` after releasing
  it (a stop plus a new message in between hits the new reply): make check-and-replace atomic. `ChatRegistry.java:1141` routes on
  `indexOf("chat")`, so an id literally `chat` is unroutable: parse by position from `ChatRoutes`. One test each (lesson
  `2026-11-12-23-001` items 6 and 7).
- Overlap: `PLAN-161` (chat bridge, may run in parallel) changes no mock code; if this plan changes a chat route or body, coordinate.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-134-chat-mock-context-narrative-queued.md
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

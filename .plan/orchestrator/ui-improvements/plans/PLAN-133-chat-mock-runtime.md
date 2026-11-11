# PLAN-133: Chat in the Mock Runtime: Routes, Streaming, Timing, Failures (D4 as Re-Scoped by D5, Part 2)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `plan-marshall` lane. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The mock runtime serves a small chat: a fixed reply to a turn, no stream, no stop, and a proposal that is dropped on every
plan turn and applies nothing on accept. The AGREED design (`chat.adoc` § `#contract`, the mock runtime specification
§ `#mock-timing`) needs a mock that plays a chat as the real runtime will: the message answered `202`, the reply as events at a
reading pace, steps, stop, proposals that act, a second device, failures on demand, and a deterministic `ordered` timing every
test uses. This plan builds that on the contract of PLAN-132. The context line, the narrative and the queued revision
are PLAN-134. Voice and transcription are not part of this plan.

## Deliverables

> AMENDED 2026-11-11 after PLAN-132 landed (#409, `572dc739`): read § _PLAN-132 Landing Residue (Amendment 2026-11-11)_ at the end
> FIRST. Its deliverable 0 settles the wire form of the chat's message keys before any deliverable below emits one, and its
> deliverable 6 carries the contract's residue.

Specification: `doc/design/web-app/chat.adoc` § `#contract` (the mock paragraph), `#working`, `#notices`, `#proposal`;
`../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc` § `#mock-timing`, `01-processes-and-transport.adoc`
§ `#chat-routes`, `06-event-catalogue.adoc` § `#chat-events`; `doc/design/shared/work-actions.adoc` § `#chat`.

1. **One chat registry for every subject**
 - What:
   - A new `ChatRegistry` beside `StartRegistry` (not inside the 10,298-line `MockRuntimeEngine`; the engine gets hook lines
     only) serving `GET` and `POST …/chat`, `POST …/chat/turns`, `…/turns/{turn_id}/stop`, `…/proposal/{accept|reject}`,
     `…/end` for a plan, an epic, an entry, a decision (including a project's setup decision) and ended work, on the B1 model.
   - `POST …/chat` returns the open chat or opens one (a subject has one chat at a time); `GET …/chat?before=<turn_id>` pages
     (a page of 50 turns); a message with a repeated `client_turn_id` returns the same turn and records nothing; a message
     while a reply is written is refused `act_not_available`; the turn limits of B1 are checked (`invalid_arguments`).
   - Move into it the chat the engine keeps today (`controlChats`, `handleGetChat`, `handleChatTurn`, `handleChatProposal`),
     the entry chat of `StartRegistry` and the ended chat (`handleEndedChat`); migrate the scenario fixtures
     `facts.runtime.control.chats` of `running-work.json`, `fully-onboarded.json` and `projects-and-start.json`; delete
     `ChatReadBackResource`, `ChatState`, `ChatTurnResource`, `ChatProposalResource` from the mock's use.
   - The decomposition draft's and the review's chat routes are left out until O10 is decided.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
 - Done when:
   - A route test per subject kind covers open, read, page, turn (`202`, repeated `client_turn_id`, refused while writing),
     accept, reject and end, and fails on a revert (evidence order: `test-first`).
   - `git grep -n "ChatReadBackResource" -- pm-mock-runtime/src/main` is empty (evidence order: `none`).
2. **Timing and the reply as events**
 - What:
   - `--timing realistic|ordered` (default `realistic`) on `MockRuntimeCli`, in `MockRuntimeConfig` and on `@EnableMockRuntime`
     (every test passes `ordered`): `realistic` sends events after their `after_ms`; `ordered` sends them in their order as
     soon as the state is entered, without a timer.
   - A reply plays `chat.turn.started` (with the model), `chat.turn.progress` steps, `chat.turn.delta` pieces at a reading
     pace, and `chat.turn.completed` (whole text, `refs`), on the subject's stream (plan, epic, entry, workspace, the scope of
     a decision) and on `/api/v1/events`; the operator's message is announced by `chat.turn.started` alone. Stop ends the
     playing reply with the text so far (`status: stopped`); a reply announced again for a replaced worker restarts from the
     working line; `chat.ended` carries `ended_by` for each of its five causes.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - An ordered-timing test collects the event sequence of one reply and asserts names and order without waiting on time
     (evidence order: `test-first`).
   - A realistic-timing test asserts the deltas are spaced (evidence order: `test-first`).
3. **Failures on demand**
 - What:
   - `POST /mock/inject` with `infeasible`, `lost`, `too_long` (the reply ends `failed` with that reason), `http_error` (with
     its status and code), `slow`, or a stop of the playing reply: the next matching request or reply fails that way once. A
     scenario state may name the same failures.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - One test per failure asserts the event or answer it produces and that the next request is clean again (evidence order:
     `test-first`).
4. **Scripted conversations and a second device**
 - What:
   - A scripted conversation per subject: a reply with lists, a code block, a table, references to a task, a file with its
     line, a decision and a plan, a fenced Mermaid block; steps; an untrusted turn; a proposal in a later turn (kinds: change to
     the description, to the plan, to the staged plans, a proposed answer in a decision's chat, a follow-up plan in ended work).
   - Accept and reject act: a replaced proposal is `replaced`, an accepted description or draft change is `applied` and shows in
     the subject's resource, a rejected one is gone. (The queued revision of a plan and its refusals are PLAN-134.)
   - `POST /mock/broadcast` sends a chat event as if another device had acted: a message, an accepted proposal, the end.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test per proposal kind asserts accept changes what the design says it changes and a second accept is refused; a broadcast
     test asserts the event reaches the subject's stream (evidence order: `test-first`).
5. **Scenarios, drift and the existing tests**
 - What:
   - Migrate `ControlScenarioTest`, `StartScenarioTest`, `StartContractTest`, `ScenarioStateContractTest` and the operator and
     web tests that read the old chat shape; the scenario load validation checks chat events and payloads against the B1
     closed catalogue.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
 - Done when:
   - `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify` and `-Pintegration-tests` pass at the head (evidence order: `none`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a failed
     read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime verify`, with `pm-operator` and `-Ppre-commit`
  (every rewrite reviewed and committed) and `-Pintegration-tests`.
- No new dependency or plugin. The mock stays on the JDK HTTP server of the module; no Quarkus; `pm-api` keeps `jackson-core` only.
  Never catch or throw generic `Exception`/`RuntimeException`; package-private over public; every new text en and de.
- Tests: JUnit 5, no Mockito, `Generators` with `@EnableGeneratorController` for input data; golden payloads stay literal.
- Ports: mock web port 21330 (`20000 + 10 × 133`), app port 21331 (`AGENTS.md` § Ports); never stop or reuse a process this
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

- OBSERVED: the mock keeps `controlChats` and answers a plan turn with a fixed sentence and drops the proposal on every turn — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:773,5075-5125`
- OBSERVED: an accepted plan proposal changes no state of the plan (it only sets `ChatState.APPLY` and clears the proposal) — read at `MockRuntimeEngine.java:5108-5125`
- OBSERVED: the mock has `/mock/broadcast`, `/mock/state`, `/mock/reset`, `/mock/scenario`, `/mock/goto/`, `/mock/acts`, `/mock/pairing` and no `/mock/inject` and no `--timing` — read at `MockRuntimeEngine.java:6736-6762`; `git grep -n -i "timing\|/mock/inject" -- pm-mock-runtime/src/main` finds none
- OBSERVED: subject event streams exist for plan, epic, entry and a project's workspace — read at `MockRuntimeEngine.java:6726-6732`
- OBSERVED: the entry chat is kept in `StartRegistry` (`entry.chat.turns`) — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java:707,1054,1337`
- OBSERVED: the chat routes of the engine are `/chat`, `/chat/turns`, `/chat/end`, `/chat/proposal/accept|reject` (entry) and the plan and epic variants — read at `MockRuntimeEngine.java:309-329,3819-3844,4662-4678`
- HYPOTHESIS: a decision's chat has no route in the mock today (the web client calls `/api/v1/decisions/{id}/chat…`) — confirm/refute at `MockRuntimeEngine.java` § route table and `pm-web-app/src/lib/api/client.ts:3433-3477` (verify-at-outline)
- HYPOTHESIS: `ChatRegistry` can reach the engine's event broadcast through the same hook `StartRegistry` uses — confirm/refute at `StartRegistry.java` § constructor and `MockRuntimeEngine.java` § `broadcastEvent` (verify-at-outline)
- HYPOTHESIS: the realistic timing can use the engine's existing scheduler for `after_ms` of scenario events — confirm/refute at `MockRuntimeEngine.java` § auto transitions and `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/EventDefinition.java` (verify-at-outline)
- Verify-first clause: PLAN-45 (running) edits `MockRuntimeEngine.java`; re-read every engine line above at the then-current HEAD before scoping, and keep this plan's edits of the engine to hook lines.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatRegistry.java` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `1cc0e10e` (hook lines only)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntimeConfig.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/cli/MockRuntimeCli.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/junit5/EnableMockRuntime.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/junit5/MockRuntimeExtension.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ControlScenarioTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/StartScenarioTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/StartContractTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/ScenarioStateContractTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatReadBackResource.java` — present at `1cc0e10e`; deleted by the last package that stops using it
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ControlFixture.java` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-132`; `PLAN-45` landing (it is running and edits `MockRuntimeEngine.java`).
- Overlaps with:
  - `PLAN-45` (running): `MockRuntimeEngine.java`.
  - `PLAN-32` (parked): `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/` (this plan may add an SSE flush there; it follows PLAN-32 or states the change in PLAN-32's re-ground note).
  - `PLAN-134`: the same engine hook lines and scenario files; B3 follows.
- Adjacent to: the daemon's real chat (Open Defects 7), which this mock models and does not wait for.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD; settle the HYPOTHESES; re-run `corpus surfaces`; re-read `05-mock-runtime.adoc` § `#mock-timing`.

## Hand-Off Command

Lane: `plan-marshall` (re-set 2026-11-11 by the operator's size rule: 16 files, five deliverables of new mock behaviour). Emitted
2026-11-11 on the operator's word as a fifth plan beside N=4 (PLAN-32, 126, 159, 147). Since staging: PLAN-132 landed (#409,
`572dc739`) and PLAN-45 landed (#399); overlaps now running: PLAN-126 (`MockRuntimeEngine.java`), PLAN-159 (`running-work.json`,
`fully-onboarded.json`), PLAN-32 (`mock/http/`, if an SSE flush is added) — whichever lands second rebases.

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-133-chat-mock-runtime.md"
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Voice (O7):** the mock's scripted transcription is not part of this plan.
- **Review and decomposition chats (O10):** served at their own addresses once PLAN-132 has the route builders.

## PLAN-132 Landing Residue (Amendment 2026-11-11)

Routed here by the operator (AskUserQuestion, 2026-11-11) from `landings/PLAN-132.md`. It supersedes any text above that differs.
Re-read every cited line at the then-current HEAD.

0. **The wire form of the chat's message keys (do this first)**
 - Finding (major, PLAN-132 layer 1): `work-actions-chat-texts.adoc:17` says the runtime's message keys are `chat.line.<name>`,
   `chat.knows.<item>` and `chat.applies.after_task|on_start`, which the clients render through `control.chat.line.<name>`
   (`decision_answered` without `device` as `control.chat.line.decision_answered_no_device`), `control.chat.knows.item` (with that
   `item`) and `control.chat.revision_queued` / `control.chat.at_boundary` / `control.chat.at_start`. `MessageText.java:19-24` and
   `ChatContext.java:23` say the same. But every golden and test value of PLAN-132 sends `control.chat.*` keys
   (`ChatEventsTest.java` ~45-95 and 117-468, `ChatReadModelTest.java:57-61`), and 11 of them exist in no catalogue:
   `control.chat.line.task_done`, `line.started`, `reason.lost`, `reason.too_long`, `step.reading`, `step.writing`,
   `applies.after_task`, `applies.on_start`, `proposal.refused`, `knows.plan`, `knows.epic` (the catalogue has e.g.
   `control.chat.lost`, `too_long_reply`, `infeasible`, `step.read|search|check`, `at_boundary`, `control.en.properties:227-322`).
   `MessageText` and `NarrativeLine` accept any dotted key, so nothing enforces the closed set.
 - What: the wire carries the runtime's `chat.*` keys per the design table. Correct the pm-api goldens and test values to real
   runtime keys whose rendering key exists in `control.*.properties`; add a test that every `chat.line.*`/`chat.knows.*`/
   `chat.applies.*` key the goldens and the mock use maps to an existing `control.chat.*` key (en and de). For the keys the table
   does not map (a step `chat.turn.progress` key, a failed turn's `reason`, a refused proposal's finding), state the mapping in
   the table's prose only if the design already implies it; otherwise STOP and ask the operator before choosing. The mock
   (deliverables 1–5) emits only keys this deliverable fixed.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when: no `control.chat.` value remains as a wire key in `pm-api/src/test` or the mock's scenarios; the mapping test fails
   when a golden names a key with no rendering key. Evidence order: `test-first`.

6. **The contract's residue (PLAN-132, minor)**
 - What:
   - `ChatTurn.java:117-140` accepts `model` and `steps` on an operator turn, which `ChatTurnStartedEvent.java:94-111` refuses: one
     rule for both.
   - `EventName.java:287-297`: the stream-set constructor accepts an empty set against its "never empty" Javadoc; refuse it.
     `EventNameTest.java:59-66` cannot fail: pin `RESET` and `STREAM_CLOSED` as derived from every `EventStream`, and a stream
     added later.
   - `EventStream.java:27-51` duplicates six of the eight `EventScopeKind` values by hand: derive one from the other or pin
     their correspondence in a test (no new dependency).
   - `ChatResource.java:85`: the constructor Javadoc still says "the open proposal" generally; align with `hasProposal`
     (`:108-110`).
   - Mock: `MockRuntimeEngine.java:9871-9881` writes an absent device as `""`, so `decision_answered_no_device` is never chosen;
     leave the field absent and pin it (CodeRabbit's last reply on #409, comment 4539796167, asks for this recheck).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when: each point has a test that fails on revert. Evidence order: `test-first`.

Expected surface added: `pm-api/src/main/java/de/planmarshall/api/chat/MessageText.java`,
`pm-api/src/main/java/de/planmarshall/api/chat/ChatTurn.java`, `pm-api/src/main/java/de/planmarshall/api/chat/ChatResource.java`,
`pm-api/src/main/java/de/planmarshall/api/event/EventName.java`, `pm-api/src/main/java/de/planmarshall/api/event/EventStream.java`,
`pm-api/src/test/java/de/planmarshall/api/chat/ChatEventsTest.java`, `pm-api/src/test/java/de/planmarshall/api/chat/ChatReadModelTest.java`,
`pm-api/src/test/java/de/planmarshall/api/event/EventNameTest.java`, `doc/design/shared/work-actions-chat-texts.adoc` (prose of the
mapping only, if deliverable 0 needs it). `MockRuntimeEngine.java` is already in the surface.

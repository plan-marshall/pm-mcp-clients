# PLAN-132: Chat Contract in pm-api (D4 as Re-Scoped by D5, Part 1)

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off through `/plan-marshall`. Drafted 2026-11-10 from the AGREED
> D4 and D5 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

D4 and D5 (PLAN-115, PLAN-125, AGREED) designed one chat for every subject: a transcript in pages, a reply that streams,
a notice the runtime writes about the subject, the context line, one proposal with its states, a plan's queued revision.
`pm-api` carries the older, smaller chat records (`ChatTurnResource` has four fields, `ChatReadBackResource` a state enum
and one proposal). This plan builds the client contract the web app, the terminal and the mock all share: the read model,
the requests and errors, the events and the shared texts. The mock that serves it is PLAN-133 and PLAN-134; the daemon
does not serve any of this yet (Open Defects 7 of the epic), so nothing here depends on it. Voice and transcription are
not part of this plan.

## Deliverables

Specification: `doc/design/web-app/chat.adoc` § `#contract` and `#rely`; `doc/design/shared/work-actions.adoc`
§ `#chat`, `#chat-knows`, `#chat-acts`, `#chat-narrative`, `#chat-places`, `#chat-ended`; `doc/design/shared/work-actions-chat-texts.adoc`;
`doc/design/shared/plan-view.adoc` § `#chat-revision`; `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc`
§ `#chat-routes`, `#chat-proposal-states`; `06-event-catalogue.adoc` § `#chat-events` and the row `plan.revision_queued`;
`../pm-mcp-core/doc/specification/state-catalogue.adoc` § `#chat`, `#chat-context`, `#chat-narrative`, `#chat-names-acts`.

1. **The chat read model**
 - What:
   - A new package `de.planmarshall.api.chat` (with `package-info.java` and `@NullMarked`) holding one chat model for every
     subject. The chat: id, subject (`kind`, `id`), routed role with its model, scope, state, `context` (`knows[]` of message
     key and arguments, `as_of`), the open proposal, the newest page of turns and the token for the page before. The turn:
     `turn_id`, `kind` (`message`, `proposal`, `notice`), author, `status` (`streaming`, `complete`, `stopped`, `failed`),
     `format` (`plain`, `markdown`), `untrusted`, `model`, `steps`, `refs` (kinds `task`, `file`, `decision`, `plan`, `hold`,
     `act`; an act as `{kind, act, subject}`), `at`, and for a notice `lines[]` of `{key, args, refs, at}`. The proposal:
     its own `turn_id`, `reply_turn_id`, `kind`, `summary`, `applies`, before and after, `state` (`open`, `replaced`,
     `accepted`, `rejected`, `applied`, `refused` with its finding, `lapsed`). The plan's `queued_revision` (`chat_id`,
     `turn_id`, `subject` with `kind` and `id`, `chat_open`, `applies`).
   - Closed enums that fail closed; JSON through the streaming API of `jackson-core` only (`JsonTree`), never
     `jackson-databind`; records, final fields, package-private where possible.
   - The old types stay until the package that migrates their last consumer deletes them: `ChatReadBackResource`,
     `ChatTurnResource`, `ChatProposalResource`, `ChatState`, `ChatScope`, `ChatRole` (control) and `StartChatResource`,
     `FollowUpProposalResource` (start) are deleted by PLAN-133 (mock) and PLAN-138 and PLAN-140 (terminal); this
     plan marks each with the package that deletes it in its Javadoc. This is a recorded transition, not a compatibility layer.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java-cui:cui-testing
 - Done when:
   - Round-trip tests per record with every field and every edge (absent, empty, one, many, duplicate, an unknown enum value
     refused, a `lines[]` with `refs` of each kind). Golden JSON from the documents stays literal (it is the expectation); input
     values come from `Generators` with `@EnableGeneratorController`. Each test fails on a field dropped from the codec
     (evidence order: `test-first`).
2. **Requests, answers and errors**
 - What:
   - The turn request (`text` 1 to 8000 characters and the client's own `client_turn_id`), the `202` answer with the turn's
     id, accept and reject naming the proposal's `turn_id`, stop, end, and the page read (`?before=<turn_id>`).
   - Error codes the chat uses in `ApiErrorCode` (`act_not_available`, `conflict`, `target_not_found`, `invalid_arguments`),
     with their catalogue texts in en and de.
   - Route builders in one class (like `MachineApiPaths`) for the subjects the runtime specification names: plan, epic,
     entry, decision (below its scope) and ended work, so the two clients never build a chat path by hand. The decomposition
     draft's and the review's chat addresses are open (O10); this deliverable leaves them out and says so.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:javadoc, pm-dev-java-cui:cui-testing
 - Done when:
   - Tests for the request limits (0, 1, 8000, 8001 characters, a repeated `client_turn_id`), each path builder for every
     subject, each error code's text in en and de (evidence order: `test-first`).
3. **The events**
 - What:
   - `EventName` gains `chat.turn.started`, `chat.turn.delta`, `chat.turn.progress`, `chat.turn.completed`, `chat.proposal`,
     `chat.ended` and `plan.revision_queued`, each with its typed payload record (`06-event-catalogue.adoc` § `#chat-events`
     and its `plan.revision_queued` row), its stream scopes (chat: P, E, W, N; the revision: R, P, E) and its `EventKind`.
   - `ActivityChangedEvent` and the overview's plan row carry `queued_revision` (with `subject` and `chat_open`); the plan
     resource carries `queued_revision`; where a resource shows who changed something, the `origin` forms (`{kind: chat,
     chat_id, turn_id}`, `after_chat`, the re-planning record's chat block) are in the records.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `EventNameTest` and `EventKindTest` assert the seven names, scopes and kinds, and that a name outside the list is
     refused; one golden payload per event from the catalogue table, including a `chat.turn.completed` with `reason` for
     `failed` and a notice with `lines[]` (evidence order: `test-first`).
4. **The shared texts**
 - What:
   - Every key of `control.chat.*` in `work-actions-chat-texts.adoc` (the table is the expectation: about 106 keys, among them
     `control.chat.knows.*`, `control.chat.step.*`, `control.chat.starter.<scope>`, the closed `control.chat.line.*`
     narrative catalogue, the proposal kinds, the notices) and the `decision.chat.*` keys that are not in the catalogue yet, in
     `control.*.properties` and `decisions.*.properties`, en and de, with matching placeholders.
   - The web (`web.chat.*`) and terminal (`operator.actions.chat.*`) keys stay with their packages.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - A catalogue test reads the key table of the design document and asserts every key exists in en and de with the same
     placeholder names (evidence order: `test-first`).
5. **The answer channel is never defaulted**
 - What:
   - `DecisionAnswerRequest.fromMap` sets the channel to `cli` when none is sent (`DecisionAnswerRequest.java:111-112`), so a
     web answer is recorded `cli`. The channel is the listener's, not the request's: remove the default (a `@Nullable`
     channel, absent stays absent), keep what the terminal sends, and say so in the Javadoc.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test fails if an absent channel reads back as `cli`; every existing sender of the terminal client still sends its
     channel explicitly (`git grep -n "channel" -- pm-operator/src/main` reviewed and listed in the PR; evidence order: `test-first`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api verify`, `./mvnw -pl pm-api,pm-operator,pm-mock-runtime
  verify` and `-Ppre-commit` (every rewrite reviewed and committed), and `-Pintegration-tests` for the modules touched.
- No new dependency or plugin. `pm-api` stays on `jackson-core` streaming only. Never catch or throw generic
  `Exception`/`RuntimeException`; package-private over public; every public type documented; `@NullMarked` on every package.
- Tests: JUnit 5 only, no Mockito; input data from `Generators` with `@EnableGeneratorController`; the expected value of a
  contract (wire name, catalogue text, golden JSON) stays literal.
- Logging (if any) through `CuiLogger` and the module's `LogMessages`; every new text in en and de.
- Ports (if any server is started): mock web port 21320 (`20000 + 10 × 132`), app port 21321 (`AGENTS.md` § Ports).
- Write-set discipline: the main checkout is never edited; only the session's own worktree and branch.
- The merge rule: merge only when every thread has a reply and is resolved, every review body is answered and some head has
  had one CodeRabbit review; counts stated from `ci pr comments`; merge on the operator's word only; commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project
  `plan-marshall_pm-mcp-clients`), every open issue fixed in a commit of THIS pull request. State `sonar_open_new_issues=N` in the
  landing message. A dismissal needs the operator's word.
- If CodeRabbit refuses on quota, stop and ask the operator: a lane's standing review authorization never replaces the one
  CodeRabbit review (PLAN-36 merged unreviewed; epic.md Open Defects).

## Claim Labels

- OBSERVED: `ChatTurnResource` has the four fields `role`, `text`, `at`, `untrusted` — read at `pm-api/src/main/java/de/planmarshall/api/control/ChatTurnResource.java:34-39`
- OBSERVED: `EventName` has no `chat.*` and no `plan.revision_queued` — read at `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` (every `("...")` constant)
- OBSERVED: `MachineApiPaths` holds four constants only; the chat paths are built in `WorkActionsClient`, `StartApi` and the web client — read at `pm-api/src/main/java/de/planmarshall/api/MachineApiPaths.java`; `pm-operator/src/main/java/de/planmarshall/operator/actions/WorkActionsClient.java`
- OBSERVED: `DecisionAnswerRequest.fromMap` defaults the channel to `"cli"` — read at `pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnswerRequest.java:111-112`
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` holds no `control.chat.*` key (only `control.plan.chat`, `control.epic.chat`, `control.review.chat`) — read at that file
- OBSERVED: the runtime specification names chat routes for a plan, an epic, an entry, a decision below its scope, and ended work, and none for the decomposition draft or a review — read at `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc:166-168,280`
- HYPOTHESIS: the typed event payload records live next to their resources (`DecisionAnsweredEvent` in `decision`, `ActivityChangedEvent` in `overview`), so the chat events go in `chat` — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/event/EventKind.java` § kind assignment (verify-at-outline)
- HYPOTHESIS: the stub daemon of the test scope (`FakeRuntimeServer`, `StagedDaemon`, `RuntimeFixture`) has no chat route and needs none for this plan — confirm/refute at `pm-api/src/test/java/de/planmarshall/api/testsupport/FakeRuntimeServer.java` § routes (verify-at-outline)
- HYPOTHESIS: the plan resource that gains `queued_revision` is `process.ProcessTree` or `overview.ActivePlanSummary`, and the plan row of the overview is `ActivePlanSummary` — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` and `pm-api/src/main/java/de/planmarshall/api/process/ProcessTree.java` (verify-at-outline)
- HYPOTHESIS: no pm-operator sender relies on the `cli` default of the answer channel — confirm/refute at `pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java` § request body (verify-at-outline)
- Verify-first clause: re-read the AGREED event catalogue and chat routes at the current HEAD of `plan-marshall-documentation` before scoping; a field added there since (`06-event-catalogue.adoc`, the proposal states) is added here.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/chat/` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/chat/` — absent at `1cc0e10e`; created by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventKind.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventScope.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/event/EventKindTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivityChangedEvent.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnswerRequest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ControlCatalogueTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/ChatTurnResource.java` — present at `1cc0e10e`; marked for deletion by the named packages

## Dependencies and Sequencing

- Depends on: none (the AGREED documents). O10 decides the two routes left out.
- Overlaps with:
  - `PLAN-45` (running): `ApiErrorCode.java` and `common.*.properties` (key blocks are disjoint, but both add to `ApiErrorCode`).
  - `PLAN-123` (running) and `PLAN-30` (staged): `operator.*.properties`, `common.*.properties`, `epic_view.*.properties` (this plan touches `control` and `decisions` only).
- Adjacent to: `pm-api` `EventKind` ("badge source, not a notice") which the chat events must respect; the daemon (Open Defects 7), which this plan does not depend on.

## Re-Grounding at Restart

Re-read every named file at the then-current HEAD; settle each HYPOTHESIS; re-run `corpus surfaces`; re-read the AGREED event catalogue and the chat routes; confirm O10 and O7.

## Hand-Off Command

Lane: `plan-marshall` (a normal plan: a new contract, five deliverables; operator 2026-11-10: normal and large plans through `/plan-marshall`).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-132-chat-contract.md"
```

## Write-Boundary

The session touches only its own repository source and tests and creates NO file under `.plan/orchestrator/` other than its
own `inbox/{sender}-{seq}` message; the orchestrator owns every other ledger write.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Voice (O7):** carved out: no transcription route, speech role, consent kinds or `web.voice.*` in this plan (later voice package).
- **Review and decomposition chats (O10):** each gets its own address (`…/epics/{id}/decomposition/chat`, `…/epics/{id}/review/{register}/chat` with the item as a field); the runtime specification (`plan-marshall-documentation` `01-processes-and-transport.adoc` § chat routes) and the core state catalogue name them FIRST; this plan's route builders for these two wait for those documents.

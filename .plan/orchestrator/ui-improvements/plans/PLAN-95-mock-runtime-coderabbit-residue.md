# PLAN-95: Mock Runtime: the CodeRabbit Findings Nobody Handled

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-02 from the audit of every CodeRabbit review in the ledger's history (`landings/coderabbit-audit/`). One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Close the findings of CodeRabbit on the mock runtime (`pm-mock-runtime`) that were never fixed and never answered with a reason: stale state after a reset or scenario switch, unlocked readers, a half-switched scenario on a malformed fact, find results that the mock gets wrong, and plan or project-setup handling that the real runtime does not share. All of them were found by reading review BODIES and unresolved threads (the earlier checks looked at threads only). All are Minor or Trivial against production semantics; the mock is the contract's test double, so each one changes what the clients are tested against. One pull request.

## Deliverables

1. **Reset and scenario switch clear every store the engine fills**
   - What: `initDefaultStart()` clears `startedPlanIds` but not `startedPlans` (PR 284 thread T14, PR 269), so after `reset()`, `goToState()` or a scenario switch `planSummary` and `/process` and `find` still serve the old plan (`MockRuntimeEngine.java` field ~643, puts ~3802/3926/4005, `initDefaultStart` ~2823-2833). `projectSetupStates` is likewise never cleared (PR 275; only `remove` at ~7954 for unenrol and `put` at ~7993). Plans created through the start-entry `create` act (`StartRegistry.create`, `StartRegistry.java` ~1085-1105, `handleEntryAct` ~3611) are recorded nowhere and are 404 at `/process`: the operator answered "no `create` act" on the plan control surface, and CodeRabbit replied that this misses the start-entry surface (PR 269).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - One test per store (started plans, project setup states, the plan a start-entry `create` makes) that fails before: after a reset and after a scenario switch the old content is gone; a plan made through the `create` act answers `/process` with 200 (evidence order: `test-first`)
2. **A scenario switch is atomic, and its facts are validated first**
   - What: `setScenario` validates only `validateRuntimeFixture(scenario)`; a malformed start-state `facts.job` fails later in `seedStateFixtures()` after `currentScenario` and the stores changed, and is reported as `scenario_not_found` (PR 253, Major in the later review; `MockRuntimeEngine.java` ~717-731 on main; `validateStateFacts` exists and is used only in `goToState` ~1183). `validateStateFacts` silently skips a present fact that is not an object (`"job":[]`, `"durations":[]`: `JsonTree.object` returns null; ~1204-1219) and `/mock/goto` serves the default cleanup job (PR 253, unresolved thread). A repeated cleanup delete takes the `DONE` branch and returns the original `summary().freedBytes()` instead of the adjusted one (~8399-8408, 8433-8439). `GET /api/v1/system` still serves the seeded snapshot with the old cleanup counts after a `system.running` jump and a cleanup delete or ignore (`handleGetSystem` ~8093-8100). CodeRabbit also asked for `broadcastReset()` after a successful `goToState` (marked "Unverified"; the operator added it in `4994ebd5` and removed it in `2fa04270` to keep the SSE order, without a reason). DECIDED by the operator (2026-11-05, proposal accepted): do NOT call `broadcastReset()` after `goToState`. Reason to record in the progress record and in a comment at the call site: `broadcastReset` clears every ring buffer and sends a `reset` to every open stream, and exists for a scenario switch, where the previous scenario's envelopes must not be replayed into the new one; a jump stays inside one scenario, and `transitionTo` schedules the new state's own events (`afterMs`), which a client that reconnects with a `Last-Event-ID` must still be able to replay in order. Add a test that pins it (a jump emits no `reset`, and the state's events keep their order); if the test shows the order is not kept without the reset, report it instead of adding the call.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a scenario with a malformed start-state fact is refused with the right error and leaves the engine exactly as it was; a non-object `job` or `durations` fact is rejected, not skipped; a repeated cleanup delete returns the adjusted `freed_bytes`; the system snapshot reflects the live cleanup job (evidence order: `test-first`)
     - The decision of `broadcastReset` (not called after a jump) is recorded with its reason at the call site and in the progress record, and a test pins it (evidence order: `none`)
3. **The find and needs handlers read under their locks and return what the contract says**
   - What: `collectRunningProjects` iterates `enrolledProjects` without `projectsLock` (PR 284 T13; ~5769-5772); `collectOpenDecisions` and `collectReports` read `openDecisions` and `mockReports` without `decisionsLock` and `reportsLock` (T15; ~5847-5866); `collectOpenDecisions` passes the raw decision subject ("plan fix-relay") as the plan location instead of the stripped plan id (T16; `extractPlan(decision)` exists ~8693; `location(decision.subject(), null)` ~5853). Find returns an unenrolled project from the static `/api/v1/projects` fixture fallback (~5778-5788) and misses an epic started by `handleStartEpic` (it is recorded only in `startedEpicIds`; `collectRunningEpics` ~5793-5802 reads the static route). `serveNeedsOperatorPage` serves the fixture items of the active route only, so answering a decision or resolving a hold does not change the list (~5655-5661; a fidelity gap, low value: do it or answer it with a reason). `NeedsOperatorItem` has no stable item id although the design says selection stays on the item's id (`shared/needs-and-find.adoc:46`; PR 284 T2 was resolved without a change or a reason). DECIDED by the operator (2026-11-05, proposal accepted): add NO id field. The contract does not have one: the specification (`plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc:320`) lists per item its kind, its link, where it belongs, since when it waits and what it blocks, and the `pm-api` record has the same members. Change the design sentence: the selection stays on the item's `link` (its own path), and on `kind` plus the project, epic and plan ids where the link is absent (the machine's own items `credential` and `cleanup_due` carry no plan, so `kind` plus the project); both clients get the same rule in that sentence. Say so in the pull request.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: a concurrent find during an enrol does not throw; a decision found by find carries the plan id as its location; an unenrolled project is not found; a started epic is found (evidence order: `test-first`)
     - `NeedsAndFindScenarioTest` asserts the order of every find match for FIX-RELAY (the `seenEnded` loop) and the ended predecessor `fix-relay-v1` in the "214" search (PR 284 nitpicks, `NeedsAndFindScenarioTest.java` ~193-216) (evidence order: `none`)
4. **The onboarding and project-setup handlers of the mock follow the real flow**
   - What: `handleSetupProject` stores `project.completed` but never updates the `EnrolledProject`, so explorer and project reads keep the old readiness (PR 275 thread 6de88f; ~7985-7995). It completes setup for any link name, not only the `set` link (PR 275 follow-up; ~7113-7116 at merge). `applyStateEffectOf` reads only the `decision` member and the inner `id`, not the `question` member and the outer `id` (PR 275; ~6561-6585). `DialogueSourceTest.java:96` still matches `"link"` instead of `"link":"set"`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: completing project setup makes the enrolled project ready in the explorer read; a link other than `set` does not complete the identity step; a state effect that carries a `question` member and an outer `id` is applied (evidence order: `test-first`)
5. **Small repeated code and the port handling the first review asked for**
   - What: `webPort > 0 ? webPort : 7420` appears at six sites of `MockRuntimeEngine` (PR 243; ~2141, 2169, 2183, 2235, 2291, 8128): extract it. Not in this plan: the `vite.config.ts` and `ports.js` items of the same review belong to PLAN-93.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
   - Done when:
     - One named helper holds the fallback; no other site spells it (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

6. **The mock's drain follows its withdrawal and its own list** (PLAN-96 residue, `landings/PLAN-96.md`)
   - What: (a) A scheduled `runtime.draining` event still fires after a drain is withdrawn: `syncDrainItems` (`MockRuntimeEngine.java` ~7260, called from `broadcastEvent` ~1330) has no lifecycle or generation guard and `handleDeleteStopRuntime` (~8420) does not cancel the scenario's `pendingTasks`; a withdrawn drain must cancel its scheduled drain events, and `syncDrainItems` ignores an event whose drain generation is not the current one (CodeRabbit thread `:1330` of PR 320, unresolved). (b) `scheduleDrainShrink` (~8380-8385) schedules `seconds <= 3`, three steps, while the default drain lists four items: it schedules one step per remaining item (CodeRabbit thread `:8383`, unresolved), so a four-item drain empties and ends `DRAINING`, and a two-blocker drain does not show "0 left: running work" before its last item left.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
   - Done when:
     - Tests that fail before: after a withdrawal no `runtime.draining` event reaches a client and `lifecycle_state` stays `running`; a four-item drain emits four shrink steps and ends; a two-item drain shows "1 left" and then empties on its last step (evidence order: `test-first`)
     - The two threads are replied to with the commit and resolved (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-mock-runtime -am verify` and `./mvnw -pl pm-operator -am verify` pass at the head (the operator and the web e2e suite run against this mock).
- No new dependency or plugin.
- CUI standards (`CLAUDE.md`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's `LogMessages` class with a `LogAsserts` test; `@NullMarked` on every package; JUnit 5 only, no Mockito, no Hamcrest.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`.
- The mock must keep serving what the contract says: a change here is checked against `pm-api` and against the design in `doc/design/` and the specifications in `plan-marshall-documentation`; the design document is updated in the same pull request.
- Ports of this plan: mock web port `20950`, app port `20951`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads.
- `MockRuntimeEngine.java` is one very large file: keep each change local, extract a helper before you duplicate, and do not reformat the file.

## Claim Labels

- OBSERVED: each finding of deliverables 1 to 5 is present on `main` at `94bbefe1` — read by the auditing agent against the merge commit and `main`, with the cited lines; the orchestrator did not re-read each one (verify-at-outline)
- HYPOTHESIS: line numbers are approximate (the file moved); confirm each finding at the symbol named before changing it
- OBSERVED: the contract has no item id for `NeedsOperatorItem` (kind, link, location, since, blocks) — read by the orchestrator in `runtime-model/01-processes-and-transport.adoc:320` and in the `pm-api` record on 2026-11-05; the design sentence `needs-and-find.adoc:46` is what changes
- Source: `landings/coderabbit-audit/` (PRs 243, 253, 269, 275, 284), the review bodies of `coderabbitai`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted finding re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/NeedsAndFindScenarioTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/DialogueSourceTest.java` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 94bbefe1
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/SystemScenarioTest.java` — present at HEAD 6cf1aac0
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/ScenarioStateContractTest.java` — present at HEAD 6cf1aac0

## Dependencies and Sequencing

- Depends on: `PLAN-82` (shipped), `PLAN-11` (shipped), `PLAN-03` (shipped)
- Overlaps with: `PLAN-47` (running; may touch the mock for actions by state), `PLAN-92` (running; `pm-operator` tests), `PLAN-94` (`DialogueSourceTest`: take it here, not there)
- Adjacent to: the real runtime in `plan-marshall-mcp` (not here)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-95-mock-runtime-coderabbit-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

# PLAN-194: The Mock's Chat Fixtures and the PLAN-134 Residue

epic: ui-improvements
workstream: WS-07

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Split out of PLAN-140 (its amendment
> "PLAN-134 Landing Residue", item list D-R134) on the operator's word of 2026-11-25 ("split as proposed"), after the cleanup of
> 2026-11-25 found the cycle: PLAN-139 needs these fixtures while PLAN-140 followed PLAN-139. This plan lands before PLAN-136, 137,
> 139 and 140 need the fixtures. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer. **It adds no dependency.**

## Objective

PLAN-134 (#428, `7adb56c2`) landed the mock's chat without its fixtures and with open review findings. Make the mock runtime's chat
complete enough that the web and terminal chat plans can test against scenarios instead of `/mock/broadcast`: the scenario fixtures,
the proposal application and refusal rules, the revision window, the narrative, and the test pins PLAN-134 left out. Mock and
`pm-api` contract only; no client view changes.

## Deliverables

Read first, at the then-current HEAD: `pm-mcp-core` `doc/specification/state-catalogue.adoc` § `chat-proposal-states` and
§ `chat-narrative` (read-only, `git -C ../pm-mcp-core show origin/main:…`), `pm-mock-runtime/.../runtime/ChatRegistry.java` (CR),
`.../MockRuntimeEngine.java` (ME), `ChatScripts.java`. The PLAN-134 line references (at `7adb56c2`) have moved: re-anchor each before
editing it.

1. **The scenario fixtures (PLAN-134 D5, never shipped)**
 - What: extend `running-work.json` and `fully-onboarded.json` with a plan with a running task and narrative events on it; a plan
   decision chat that carries a revision proposal; an ended plan; knows and narrative for an epic and an entry. All of it passes the
   load validation.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - A scenario test per fixture asserts it loads and the chat, proposal, narrative and knows it serves (evidence order: `test-first`).
2. **Applying and refusing a proposal**
 - What: `applyChatProposal` applies `after` (today it only bumps an epic's rows or the draft revision; PLAN-133 m2), and
   `ChatProposalsTest` follows. A proposal REFUSED for no plan, a passed boundary or a failed check still accepts `reject`
   (operator decision 2026-11-14): `ChatProposal.isOpen()` extends to REFUSED for reject only. A `chat.proposal` ACCEPTED sent through
   `/mock/broadcast` goes through `revisionRefusal`, does not overwrite a queued revision, and cannot throw past the catch (review m5).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - Tests for `after` applied, reject of each REFUSED cause taken, and the broadcast path refused/not overwritten fail before the
     change and pass after it (evidence order: `test-first`).
3. **The revision window and the narrative's truth**
 - What: `scope.ended` closes the revision window (`revisionWindowOpen` ignores `planActivities`), so a revision accepted from that
   plan's decision chat is refused as lapsed (review M1); `plan.paused` in state `pausing` is narrated as pausing and the later real
   `paused` is not deduplicated away (m1); `planFor` no longer binds an epic or entry decision `X/Q-9` to a plan named `X` by prefix (m2);
   an infeasible task outcome is not narrated as done (m4). A plan in `sanity-check` is outside the revision window (pm-mcp-core
   phase-workflows/08, 09; the phase arrives in the clients with its own package): if the mock has no such phase yet, state it and add
   no phase.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - One failing-then-passing test per item (evidence order: `test-first`).
4. **The race and the order pins**
 - What: a test for the replaced-worker race (the check and replace in `playFailure` under one lock; the fix shipped untested); for
   apply, refuse, lapse, chat-end and decision assert `plan.revision_queued`, the overview row, `activity.changed` and their order
   (today only accept is pinned, `ChatRevisionQueueTest`); assert "reject still taken" per refusal and add a test for "another subject".
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core
 - Done when:
   - The race test fails when the lock is split (mutation run quoted in the PR) and the order pins fail when two events are swapped
     (evidence order: `mutation-after`).
5. **Narrative completeness and the smaller items**
 - What: the "pull request merged" line; epic and entry lines (`row_*`, `source_*`, `auto_changed`); knows for epics and entries beyond
   `description` (sources, rows, items); pins for native `decision.opened` and for pausing; the hard-coded hold `H-1` in later plan
   replies (`ChatScripts`) goes; an applied revision emits `instance.changed`; check against the contract whether re-sending
   `chat.turn.completed` per notice extension is allowed and follow it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - Each line kind and knows field is asserted by a test that fails without it (evidence order: `test-first`).

NOT in this plan: the line key for the conflict and lapse notice (D-R134 item 6) — `pm-mcp-core` names it in `chat-narrative` first;
if it is named at the start, add it to `control.{en,de}.properties` and `ChatMessageKeysTest` and emit it on a conflict and a lapse,
otherwise leave it and say so.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-mock-runtime,pm-api -am verify` while working, then the full
  `./mvnw verify`, `./mvnw verify -Ppre-commit` (review every rewrite; revert those outside this plan) and `-Pcoverage` (80 % per module)
  at the head. No web change, so no e2e run.
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`, contract values literal. No `{@html}` with model or engine text, no timer wait in a test. Pre-1.0: no shim.
- ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`; a dismissal only on the operator's selection.
- The merge rule (`CLAUDE.md`, Git Workflow): the run merges its own pull request once every review thread has a reply and is resolved, every review-body item is answered, some head has had one CodeRabbit review, the build and Sonar are green. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Never touch, rebase or merge another plan's branch (the parked PLAN-42's included). Other plans run at the same time: never stop or reuse a process this worktree did not start.

## Claim Labels

- OBSERVED: `ChatRegistry.java` is unchanged since `7adb56c2`; `MockRuntimeEngine.applyChatProposal` (~`:1055-1075`) only bumps the epic or draft revision; `ChatProposal.isOpen` (`:103-105`) is true for OPEN only — read at `3dea04b2` (cleanup 2026-11-25)
- OBSERVED: the mock's `revisionWindowOpen` is at `MockRuntimeEngine.java:1131-1152` — read at `3dea04b2`
- HYPOTHESIS: the fixtures of D1 fit the existing scenario schema without a loader change — confirm/refute at `pm-mock-runtime` `ScenarioLoader` (verify-at-outline)
- Verify-first clause: before each deliverable re-read the named files and the PLAN-134 thread references at the then-current HEAD; a refuted item is reported and re-scoped, never silently worked around.

## Expected Surface

- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatRegistry.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/ChatScripts.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatProposalsTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatRevisionQueueTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatContextNarrativeTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ChatListenerAndResidueTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/chat/ChatProposal.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/chat/ChatMessageKeysTest.java` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 3dea04b2
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 3dea04b2

## Dependencies and Sequencing

- Depends on: nothing in the queue.
- Before: PLAN-139 (needs the fixtures), PLAN-136, PLAN-137 and PLAN-140 (their checks use the fixtures).
- Overlaps with: the running PLAN-55 (`running-work.json`, one code file) and the `over_size` removal package of `doc-review-2026-11-24-001`
  (`MockRuntimeEngine.java`, `running-work.json`, `fully-onboarded.json`); whichever merges second rebases.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-194-mock-chat-fixtures-and-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports
its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

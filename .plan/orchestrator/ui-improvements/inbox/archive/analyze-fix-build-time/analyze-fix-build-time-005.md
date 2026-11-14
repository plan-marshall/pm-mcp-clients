envelope_version=1
sender_type=plan
sender_id=analyze-fix-build-time
epic=ui-improvements
kind=finding
created=2026-11-14T20:28:16Z

# Recorded-only findings of analyze-fix-build-time, filed from its findings store

## Recorded in the plan's findings store and never filed

The findings store of the plan (`artifacts/findings/*.jsonl`, archived with it) closed with 141 bug records,
80 improvements, 37 insights, 13 triage items and 3 build errors still `pending`. The bug records describe defects
that the plan found while covering and fixed (`coverage-fix:`), so they are history, not work. The rest is
recorded-only. Everything below dates from 2026-11-11 to 2026-11-13, before the merges of #434 to #436 and before
main's later rewrites: **each item is a lead to verify against current main, not a fact.** The full text of the
triage items, the pending build errors and the design-gap insights is in `work/follow-up-findings-export.md`
of the archived plan.

### Changes the advice on the web coverage exclusion (triage b436db)

CI's Sonar job runs `verify -Psonar` (reusable workflow `cuioss-organization` `reusable-maven-build.yml` v0.44.0),
not `-Pcoverage`. In CI `pm-web-app` therefore writes no `target/coverage/lcov.info`, and
`sonar.javascript.lcov.reportPaths` points at nothing. Dropping `sonar.coverage.exclusions=**/*` would make Sonar
report every web source as 0 % covered. Prerequisite of TASK-023: make the web coverage run in the Sonar job,
for example a profile `sonar` in `pm-web-app/pom.xml` that carries the same npm-test override as the coverage
profile (module-local, no change to the CI files), and only then drop the exclusion. Also pending there: the
branch coverage is at 78.7 %, so the 80 % threshold cannot be enabled yet.

### Playwright (`-Pe2e`) is not known to be green

- Three pending build errors: the run `verify -Pe2e -pl pm-web-e2e -am` of 2026-11-13 13:56 failed in the
  `run-playwright` exec step (log `build-results/pm-web-e2e/maven-2026-11-13-135610.log` in the archived plan).
- Triage 66cfcb: `showroom.spec.js` "opens the start entry with its sources, refinement chat, and file tree" failed
  on main at a139ec62 (#424) without this plan's changes: `StartView.openChat` still posts the entry act `chat`
  and renders `ChatPanel` from `state.chat`, which #424 moved into `ChatRegistry`.
- Triage aef967: `designer.spec.js` is intermittent on main (2 failures in 88 repeats): the test fills
  `workflow-text` before `loadAll()` resolves, and `openUnit(units[0])` then overwrites the text and resets `dirty`.
- Whether any of it still fails after #428 and #436 is unknown: run `./mvnw verify -Pe2e -pl pm-web-e2e -am` first.

### Contract questions for the owner of the contract (triage)

- 833c74 and 5b70cd: `client.ts` `getProjectSettings` and `saveProjectSettings` use the route
  `/api/v1/projects/{id}/settings` and the member `expected_revision`; no record of pm-api, no mock route and no
  operator code knows either (the sibling contracts name the member `revision`). Remove them or add the contract.
- d5bfc7: `runtime.ts` counts `report.*` and `decision.*` on `/api/v1/events`, but `EventName` declares them on
  the reports stream and on the plan, epic, workspace, machine and entry streams, never on the runtime stream.
- 667601: `handleDecisionEvent` reads the title of `decision.opened` at the top level instead of under `decision`,
  and counts one answer as two decrements of the open count (known live-count defect 5c821f).
- 355a71: `runtime.draining` is read under two spellings (`who`/`draining_by`, `remaining_count`/`remaining_work_count`,
  `remaining_items`/`items`); the mock uses its own, pm-api's `RuntimeDrainingEvent` the other.
- 6b8092: the `needs_operator` member spells its kinds two ways (the served list normalised through
  `NeedsOperatorKind`, the store-derived count by raw kind): a question for the specification.
- 7f7129: three mock deviations pinned by existing assertions (an unknown epic process answered 200; a refused
  plan setting answered `refused`; an answer without a choice answered option 1). All three were fixed later in
  the plan on the operator's decision (1b0205, 76637f, e19f87); stub bodies of ControlCommandTest,
  ConfigCommandTest and WebDevicesCommandTest still write `refused` and were left unchanged.
- 3aca2b: the runtime store marked a restart's `runtime.stopping` as stopped (since fixed by `isRestart`), and
  `SystemView.upgradeNow` posts at once although `system.adoc#upgrade-progress` asks for a confirmation (open).
- 0808c4: the stopped page invented "operator, now" (fixed in #436 by showing `stoppedInfo` only where recorded).
- 62f695: how the 16 conflicts of the merge of #422, #424 and #425 were resolved; `ChatRegistry` does not consult
  `chat_available` (the scripted "chat role unqualified" fixture is gone: message 001, item 1).

### Design documents that disagree with the code (insights, `coverage-doc`)

`25-system.adoc` (Enter on the Work figure names a state filter the overview does not have), `16-plan-view` (the
example key line omits the overview keys that hold on the plan view), the decisions catalogue against
`03-dialogues` (keys, answer texts, exit codes, `q` exits 0), design gaps seen while covering the source and recipe
keys of `StartDialogue`, no line for a refused pairing code, no state `start.project`, a change-ledger row with a
wrong commit SHA (TASK-17). Each is in the export with its file and line.

### Dead code and redundant guards (80 improvement records)

Found while covering and left in place because removing them was outside the budget of the task: 33
`coverage-dead-code` (a branch no input can reach) and 44 `coverage-guard` (a check that repeats what the caller
or `List.copyOf` already proves), plus the three entries below. They cluster in `OverviewFullScreenApp` (14),
`client.ts` (5), `MockRuntimeEngine` (4), `ScenarioLoader` (3) and `runtime.ts` (3), the rest one or two per class
(`ReviewDialogue`, `StartRegistry`, `SystemView`, `ProcessDesigner`, the pm-api readers). The class of work is "remove the branch or state why it stays", one module per task; the list with the
branch and the reason is `artifacts/findings/improvement.jsonl`. One is a standing code-standard gap:
**`pm-api` packages lack `@NullMarked` (all but `chat`)**, which CLAUDE.md requires for every package of a module
that declares JSpecify (da4bf6).

### Decisions kept, do not reopen without a reason

`ProcessTree`, `ProcessNode`, `EpicProcess` and `TaskActivity` keep skipping non-object entries on purpose
(operator decision, documented in their Javadoc). `sonar.cpd.exclusions=**/*` is duplication, not coverage, and
was recorded, not changed. `pm-web-e2e` `sonar.skip` and the `pm-exec` `macos-coverage` exclusion of `LinuxCalls`
were reviewed and kept.

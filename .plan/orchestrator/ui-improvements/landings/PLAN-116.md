# Landing Analysis: PLAN-116 — Terminal: Console Fourth Residue

epic: ui-improvements
workstream: WS-04
pr: #368 (https://github.com/plan-marshall/pm-mcp-clients/pull/368), merged as `6c105a86` (squash, 16 files; branch commits `150a0cb8` D1, `1dacd006` D2, `9e7086ec` gate run)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-116-cli-console-fourth-residue-001.md`, 2026-11-08) after verifying the lane's claims against git, the PRs
> (read through the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Every open review item answered where the reviewer reads it | shipped-modified | Replies verified: PR 357 thread `PRRT_kwDOVBOtss60r6WT` replied and resolved, issue comment on 357 (outside-diff item + 7 nitpicks), issue comment on 341, PR 332 thread `PRRT_kwDOVBOtss60FnxO` replied and resolved. Code: `PmConsole.java:292` passes `--`, `ProjectSelector.java:173-175` stops at `--`; tests `ProjectSelectorTest:97`, `PmConsoleBehaviourTest:477,593`, `EntryFlowStaticsTest:62-63`, `QuestionsCommandTest:399`, `OnboardingCatalogueTest` (pm-api) `:121-125`. Gap: `ConsoleSettingsTranscriptTest:72-83` does not tie an age to its device row; the design document was not updated for the new argv |
| D2 One step total in the dialogue, expectations from the contract | shipped-modified | `TOTAL_STEPS` gone from both sources; `StateDialogue.java:705-711` reads `total_steps` (envelope, then top level) with fallback. Gaps: `ProjectOnboardingSource.java:80,94` now falls back to 0 (was 10); `OnboardingAspect.values()` remains in the 2-arg `OnboardingSource` constructor and `StateDialogue` 411, 648; no test shows `EntryFlow.java:292` / `PmConsole.java:314` pass the injected list; an unrequested top-level `step` fallback in `stepOf` (725-728) |
| D3 Formatting gate on the final head; landing facts | shipped-modified | The three Sonar fixes exist (`StateDialogue.java:80-82`; `ProjectSelector.java:183-197`, `:205`). The claimed formatting of `ConsoleBadge.java:28-33` and `PmConsole.java:238` is NOT in the merge: both still read as before (`9e7086ec` rewrote only `PmConsoleBehaviourTest` and `StallingSocketProxy`) |

Changed outside the Expected Surface and explained by D2: `EntryFlow.java`, `DialogueSourceTest`, `StallingSocketProxy`, and `pm-api` `OnboardingCatalogueTest` (the spec named the wrong `pm-operator` path).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`) — reason: every full ship gets its own leaf; 16 files across `pm-operator` and `pm-api` plus their callers.
- **Verdict:** layer 1 findings; layer 2 findings; layer 3 findings (minor, reported to the operator 2026-11-08).
- **Re-read by the orchestrator at `6c105a86`:** `ConsoleBadge.java:27-33`, `PmConsole.java:237-239, 290-293, 690-694`, `ConsoleSettingsTranscriptTest.java:72-83`, `PmConsoleBehaviourTest.java:755-763`, `ProjectOnboardingSource.java:78-95` (and `6c105a86^`: `TOTAL_STEPS = 10` at 37, 81, 95), `StateDialogue.java:272-279, 705-712`, `BeingAddedProject.java:49-51`, `04-console.adoc:26,112` (no `--`; no `.adoc` in the diff), `ProjectSelector.java:170-176`. All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | D3: the pre-commit gate rewrites `ConsoleBadge` components and the `PmConsole` `@Nullable` | checked | refutes | `ConsoleBadge.java:28-33`, `PmConsole.java:238` unchanged at the merge |
| L1-2 | Claim 1 (HYPOTHESIS): `asksForProject` misreads an entry id starting `--project` | checked | confirms (narrower) | only `--project` and `--project=…` are read as a selector (`ProjectSelector.java:41-42`); `--project-xyz` never was |
| L1-3 | Expected Surface names `pm-operator/.../dialogue/OnboardingCatalogueTest.java` | checked | refutes | the asserted file is `pm-api/src/test/.../message/OnboardingCatalogueTest.java:118-125` |
| L1-4 | GitHub-side and Sonar Done-when, test-first order | not_shown_by_reading | none | outside the repository; the orchestrator read the GitHub side itself (above) |

**Layer 1 verdict:** findings — 3; not shown by reading — 1

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 `--` end of options | `PmConsole.java:292`, `ProjectSelector.java:173-175, 204-207` | `ProjectSelectorTest:97` | `PmConsoleBehaviourTest:593` uses `--project-xyz`, which proves the picocli path, not the selector misread | checked |
| D1 device ages | — | `ConsoleSettingsTranscriptTest:72-83` | swapped Firefox/Safari ages still pass (CodeRabbit nitpick on #368, unanswered) | checked |
| D1 serialised refresh | `PmConsole.java:693` | `PmConsoleBehaviourTest:758-763` | the `TIMED_WAITING` check cannot tell a lock wait from `EntryFlow.bounded`'s `join(millis)`: can pass without the lock | checked |
| D2 total from the payload | `StateDialogue.java:705-711` | `DialogueSourceTest:158,177`, `StateDialogueTest` | no test covers the injected list reaching the dialogue (`EntryFlow.java:292`, `PmConsole.java:314`); unrequested `step` fallback (725-728) | checked |
| D2 contract expectations | `StateDialogue.java:668-687` | `StateDialogueTest:241-268` | `EXPECTED_PROJECT_STEP_BY_STATE` is a hand copy; "from the contract" met by `hasKey` only | checked |
| D3 formatting | — | — | claimed rewrite not in the diff | checked |
| Design document in the same PR | `04-console.adoc:26,112` | — | still `project setup --entry <entry_id>`, no `--` | checked |
| Lane claim "test-first" | — | — | fix and test share commits `150a0cb8`, `1dacd006` | not_shown_by_reading |

**Layer 2 verdict:** findings — 6 (device ages, vacuous lock test, wiring test, design doc, formatting claim, unrequested step fallback); not shown by reading — test-first order, IT runtime

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ProjectOnboardingSource.java:80,94` | a project-setup payload without a usable `total_steps` | total 0, `stepLine` drops `[n/m]` and shows the bare title (`StateDialogue.java:275-277`); before the change `[n/10]` | minor |
| L3-2 | `pm-api` `BeingAddedProject.java:49-51,121` | a runtime reporting a total other than 10 | the badge keeps the fixed default 10 while the dialogue shows the run's own total | minor |
| L3-3 | `OnboardingSource.java:47`; `StateDialogue.java:411,648` | an injected aspect list of another size | the intro says N steps, the overview lists every `OnboardingAspect` | minor |
| L3-4 | `StateDialogue.java:711` via `JsonTree.integer` | `total_steps` 3.9 or a long above int range | truncated / wrapped (4294967297 → 1) and accepted | info |

**Layer 3 not shown by reading:** picocli's handling of `--` as another option's value (`--note --`), a missed selector at worst; `ConsoleBadgeStallIT` runtime.

**Layer 3 verdict:** findings — 4 (no blocker, no major)

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | no generic Exception in production | conforms | none added |
| CLAUDE.md § Code Standards | package-private over public | deviates | `public OnboardingSource(RuntimeClient, OperatorGate, int)` has no outside caller (`OnboardingSource.java:64`) |
| CLAUDE.md § Code Standards | Generators for input data | deviates | new hand-written inputs in `DialogueSourceTest`, `ProjectSelectorTest`, `QuestionsCommandTest`, `StateDialogueTest`, `PmConsoleBehaviourTest` (existing class convention; PLAN-79 converts the module) |
| CLAUDE.md § Code Standards | no arbitrary timer wait in tests | conforms | condition spin with deadline (`PmConsoleBehaviourTest:758-760`); see the vacuity finding |
| CLAUDE.md § Code Standards | JUnit 5, @DisplayName, no Mockito | conforms | all new tests |
| CLAUDE.md § Build | `-Ppre-commit` leaves no diff | not_shown_by_reading | the claimed rewrite is absent from the merge |

## Metrics and Anomalies

- Tokens: `unknown` (opencode); model `gemini-2.5-pro`, effort `high`; `plugin_version` `unknown`.
- Anomalies: landing facts incomplete (`missing_keys` `total_tokens`, `plugin_version`) — recurrence on the standing Open Defect. The message carries its envelope header twice (two `created` stamps 10:03:00Z / 10:03:56Z); it still validated.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed #368 twice (00:36Z, 00:50Z); one thread `PRRT_kwDOVBOtss608Lxj` replied and resolved. **The first review body's nitpick (`ConsoleSettingsTranscriptTest.java:72-73`, ages on their own row) was NOT answered on #368**; the lane's `review_body_findings_handled=13` counts the old PRs only. Standing merge rule not met in full; the nitpick is confirmed by reading and folded into PLAN-79. Sourcery skipped on quota. Threads 1 resolved; review bodies 2.
- CI/merge: merged through the merge queue as `6c105a86`; ran beside PLAN-109 and PLAN-118 without a reported collision.
- Sonar: lane `open_issue_count=0`; the SonarCloud quality-gate comment on #368 reports 0 new issues. Consistent.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 368; `landing` `landings/PLAN-116.md`; `plan_marshall_plan_id` `rb-116-cli-console-fourth-residue`
- [x] claim 1 of the spec stamped `corroborated` (narrower than stated)
- [x] epic.md: incomplete-landing recurrence; Watch for the follow-ups
- [x] folds: PLAN-79 (device ages per row, lock-wait vacuity, aspect-list wiring test; surface unchanged), PLAN-84 (`04-console.adoc:26,112` argv with `--`; surface unchanged)
- [x] layer-3 findings L3-1..L3-4 reported to the operator
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- L3-1..L3-4 (step totals: project fallback 0, `BeingAddedProject` default 10, `OnboardingAspect.values()` statics, unguarded fraction/overflow) → reported to the operator; a fix plan only on the operator's word.
- `ConsoleSettingsTranscriptTest` ages on their own row; `PmConsoleBehaviourTest:758` lock-wait discrimination; a test that the injected aspect list reaches the dialogue → folded into PLAN-79.
- `04-console.adoc:26,112` still show `project setup --entry <entry_id>` without `--` → folded into PLAN-84.
- `ConsoleBadge.java:28-33` / `PmConsole.java:238` formatting → the next pre-commit run on `pm-operator` (any terminal plan); Watch.

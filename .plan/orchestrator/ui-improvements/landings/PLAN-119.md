# Landing Analysis: PLAN-119 — Terminal: One Rule for the Step Total (PLAN-116 Residue)

epic: ui-improvements
workstream: WS-04
pr: #377 (https://github.com/plan-marshall/pm-mcp-clients/pull/377), merged as `147309ae` (squash through the merge queue, 8 files, 2026-11-09T10:05Z; parent `fb27d4aa`)

> Runbook lane (harness antigravity, executor 0.1.2080). Inbox message `rb-119-cli-step-total-residue-001.md`. Checked
> against git, the PR (gh) and a read-only leaf; key points re-read by the orchestrator at `147309ae`.

## Deliverable Fidelity vs Spec

| Done-when | Verdict | Evidence |
|-----------|---------|----------|
| D1 one rule for a missing or invalid total, at both reading sites | met | one parser `JsonTree.positiveInteger` (new, outside the Expected Surface, justified) used by `BeingAddedProject.fromJson` (`:118,:120`) and `StateDialogue.fromJson` (`:725,:727`); one constant `BeingAddedProject.DEFAULT_TOTAL_STEPS` (`:45`), aliased by `ProjectOnboardingSource.TOTAL_STEPS` |
| D1 tests cover missing, 0, negative, fractional, over-range, non-numeric, custom | met, weak | seven sequential asserts in one `@Test` each (`ProjectRecordsTest:331-353`, `StateDialogueTest:289-310`); would fail on revert |
| D1 fallback 10 rendered | met | `StateDialogueTest#projectPayloadWithoutTotalStepsRendersStepLine`, `DialogueSourceTest#…FallsBackToDefaultTen`; the spec's `ScreenCaptureIT` terminal check has no evidence |
| D2 overview and machine step follow the injected aspect list | met | `StateDialogue.aspects`; `addStepOverview` `:429`, `machineStepOf` `:665` |
| D2 `values()` grep lists only no-list defaults | met | 4 sites: `EntryFlow:78`, `OnboardingSource:48,66`, `StateDialogue:182` (the last documented only by code shape) |
| Open-issue read | met | SonarCloud: gate passed, 0 new issues, 82.7% coverage on new code |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`); stopped once on the operator's pause and resumed.
- **Verdict:** layer 1 met; layer 2 complete (one extra file, `JsonTree.java`); layer 3 partly: production sound, tests break the CLAUDE.md test rules, one reviewer finding left unfixed and contested.
- **Re-read by the orchestrator at `147309ae`:** `StateDialogue.java:665-675` (an excluded aspect's state falls through to `DONE`); `StateDialogueTest` (0 `@ParameterizedTest` / `@EnableGeneratorController`); #377 PR comments (no answer to the review body's nitpick). All confirmed.

### Seams

| Id | File:line | Seam | Severity |
|----|-----------|------|----------|
| S1 | `StateDialogue.java:665-675` | with a reduced aspect list a state of an excluded aspect (`roles.review`) renders "Done"; the lane's reply "the runtime does not emit states for excluded aspects" has no source (the list is never sent to the runtime); the test pins it; the `aspects.contains(PROVIDERS)` guard is untested | medium |
| S2 | `StateDialogue.java:725-729`, `OnboardingSource` | the fallback differs by path (project 10, machine `aspects.size()`), unstated; `BeingAddedProject` keeps a compact-constructor `<=0 → 10` and a legacy `total` alias | low |
| S3 | `JsonTree.java:~419-421` | `positiveInteger` accepts `"12"`, `1e1`, `3.0` against its Javadoc; untested; `OnboardingProgress.fromMap` (`:124`) still reads `steps` truncating | low |
| S4 | `ProjectRecordsTest:331-353`, `StateDialogueTest:289-310` | 7 variants as sequential asserts (CLAUDE.md: `@ParameterizedTest` for 3+); literals; `StateDialogueTest`, `DialogueSourceTest` without `@EnableGeneratorController` | medium (project rule) |
| S5 | `StateDialogueTest:~330-345` | the two-aspect test asserts only the count, with a prefix list; the intro's "2" comes from the hand-built frame (CodeRabbit nitpick, still valid) | low |
| S6 | `StateDialogue.java:~173-185` | new public 5-arg constructor without a production caller; aspect choice by `instanceof`; `OnboardingSource.aspects()` public | low |

## Routing and Merge Behavior

- **Review:** CodeRabbit 1 body on `fe0a4591` (2 actionable, 1 nitpick); later heads `3386d44e`, `2e773c61`, `6538e97f`, `1aa8918f` rate-limited, never reviewed. Threads 2, both replied, both resolved: thread 1 withdrawn by CodeRabbit (its follow-up-issue question unanswered); thread 2 (S1) resolved by the lane while CodeRabbit's last reply asked for the runtime contract — not fixed. **The nitpick has no reply in any PR comment and is not fixed.** Sourcery approved `1aa8918f`.
- **Merge rule: NOT MET** — a review exists, but the body's nitpick is unanswered ("all review bodies answered"); thread 2 closed contested.
- **Sonar:** 0 new issues.
- **Report accuracy:** the `values()` line numbers are off (`:181`/`:47,56` vs `:182`/`:48,66`); "2 inline comments answered and resolved" hides the contested thread; the `ScreenCaptureIT` terminal check is not mentioned.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 377; `landing` `landings/PLAN-119.md`
- [x] epic.md Watch
- [ ] seams to the operator; fix route open (S1 needs a decision: fix `machineStepOf` for excluded states, or record the runtime contract)

## Follow-Ups

- PLAN-121 (cli-project-add-and-confirm) is no longer blocked by PLAN-119; PLAN-123 still waits for PLAN-120 and PLAN-124. Emits paused by the operator (2026-11-09).
- Residue candidates: S1 (decision), S4/S5 (tests to the rules), S3, S6; answer or fix the nitpick.

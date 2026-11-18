envelope_version=1
sender_type=plan
sender_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
kind=landing
created=2026-11-17T18:04:04Z

```landing-facts
schema=landing-facts/1
plan_id=rb-66-web-onboarding-frame-and-roles
epic=ui-improvements
pr=#467
merge_state=merged
cleanup_owed=false
deliverables_total=10
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2135
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=7
review_body_findings_handled=7
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced).

# PLAN-66 (D1–D4) landed

Merge commit `735318df95fabee678fc8538960b781a84b7568b` on `main`; pull request #467.

**Scope.** The spec's Scope note licenses splitting ten deliverables by deliverable ("1 to 4, 5 to 7, the rest"). This run delivered **1 to 4**; **5 to 10 stay open** (`harness.none`, `auto.result`, `language.choose`, `catalogue.select`, `store.choose`, `harness.choose`).

## 1. Deliverables

| # | Done-when | Order | Verdict | Test | Failing run | Commit |
|---|---|---|---|---|---|---|
| 1 | `onboarding.spec.js`: no navigation before onboarding is done | test-first | met | `offers no navigation and marks no step before the machine is onboarded` | `maven-2026-11-17-090246.log:844-865` (`onboarding.spec.js:39`, brand-inert not found), 1 failed / 450 passed | 5504f792, dfd0af23 |
| 1 | Browser check on the four states | none | met | 4 targets × 8 combinations + 4 baselines, `.plan/temp/playwright-mcp/plan-66-*` | — | 5504f792 |
| 2 | A screen test on `defaults.review`: no raw unit id | test-first | met | `shows each plan-default axis with plain names, a sentence of meaning and no raw unit id` (plus the greyed variant, the changed axis, the posture on a later run, its preselection, and the blocked chosen variant) | `maven-2026-11-17-091818.log:745-761`, 3 failed | 5504f792, 25a5e8d2, ef6a0bbf |
| 2 | Browser check | none | met, one element unrendered (residue 1) | `plan-66-defaults-review-after-*` | — | 5504f792 |
| 3 | A screen test on `roles.review` | test-first | met | `groups the roles by phase, marks a departure and shows the advisory of a role`, `decides a departure per role and records a role changed by hand`, `rejects a departure and returns the row to the default of the role`, `marks a role the validation refused with its reason`, `drops a decision whose proposal the state reports differently` | `maven-2026-11-17-092704.log:801-819`, 2 failed | 5504f792, 25a5e8d2, 58eb53e7, 44eb9c37, ef6a0bbf |
| 3 | Browser check | none | met | `plan-66-roles-review-after-*`, re-taken at 44eb9c37 | — | 44eb9c37 |
| 4 | A screen test on `providers.failed` | test-first | met | `shows the failed provider under Provider, with its token entry and the three acts`, `offers no token entry for a provider whose state names no id` | `maven-2026-11-17-094046.log:829-830`, 1 failed | 5504f792, 25a5e8d2 |
| 4 | Browser check | none | met | `plan-66-providers-failed-after-*` and `…-token` | — | 5504f792 |

`deliverables_done=4`: deliverables 1–4 meet every clause; 5–10 are not started.

Per-head tests: `maven-2026-11-17-090246.log` (base test at 090246), `091818`, `092704`, `093234`, `094046`, `113905`, `125723`, `150544` (module tests fail-first/iteration), green module runs at 71dddb69, 3a5f15a0, 9d27c33a, 2a090d39, 82085076, 91339dab, 844e8cfe, 9750e00f, c199695d, 5f72f9e3; gate runs c0ed97d1 (dfd0af23), 96e089df (25a5e8d2), df15a0b3 (58eb53e7), 6a6e9a9f (44eb9c37), 61f1afe5 (ef6a0bbf), 9b137e73 (c6e9a47c), f128e178 (92b60173); e2e runs 102550 (dfd0af23), 121944 (25a5e8d2), 132509 (58eb53e7), 142746 (44eb9c37), 153752 (ef6a0bbf), 174111 (92b60173) — all 451/453 passed.

## 2. Personas and skills

- D1: `persona-integration-tester` (the spec's line); D2–D4: `persona-implementer`. Self-review rounds: `persona-code-reviewer`. The review triage of the PR comments: `persona-code-reviewer`.
- Skills named and loaded as standards: `pm-dev-frontend:javascript`, `:css`, `:jest-testing`, `:javascript-security` (D1 names `javascript` and `javascript-security` only). No skill was added for surface drift; the mock scenario and the catalogue packs are inside the expected surface.

## 3. Hypotheses

The spec's Claim Labels carry no `HYPOTHESIS` line; its Verify-first clause is procedural. The Re-Grounding note (2026-11-14) is confirmed at HEAD: `views/onboarding/` exists, `HarnessChoice.svelte:151-152` still carries the stray duplicate `/>` (not this run's deliverable), and `SecretField`/`TextField`/`RadioGroup`/`CheckGroup` exist.

## 4. Findings

- Self-review round 1 (11 findings, all fixed): the plan-axes contract (`method.tests-after`, `ask.questions-only`, bare posture with `null`), the provider key, the command palette, the roles count line, the harness name, the dead `AspectChoice` branches, the doc counts and statements.
- Round 2 (9 findings, all fixed): the posture fixture on the states that render it, the departure's accept/reject with a real `default`, `effortOptions` derived, the role classes as words, the frame line width, the doc sample note, the dead role keys, the reconfigure roles, the written contract.
- Round 3 (9 findings, 8 fixed): the hand change after a decision, the departure fixture's default, the harness names, the first-run posture, the frame line width, the texts inventory, the provider sketch order. **Open: residue 1.**
- CodeRabbit (7 inline findings, all fixed in ef6a0bbf): the posture record in the mock, the header comment (refuted with a stated reason), the greyed chosen variant, the derived phase keys, the stale decision, the refused role's reason, the nav test ids.
- Sonar found `java:S3776` on `recordAspectAnswer` at 8b7010b4; fixed in c6e9a47c (`recordPlanDefaults`).
- Flakes, each re-run green: `pm-operator OverviewFullScreenTest` (gate at dfd0af23), `pm-api EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` (generator seed), the axe `settings and designer` audit and the chrome command-palette test (the app not initialized inside a 5 s assertion under load).

## 5. Reviewers

- `coderabbit` (required, `quota_limited=true`, `review_complete_after_handling=true`): triggered at 14:41 on 44eb9c37; reviewed (marker `final_review_risk_coverage` `sourceCommitId=44eb9c37`, `kind=reviewed`); 7 findings, all answered and resolved; not re-triggered for the later heads, per the exemption, and the gate's `review-gate` call passes with `--required-reviewer` absent because the done exemption applies.
- `sourcery` (optional): refused on its own quota ("used your own review budget of 250,000 diff characters for the last 7 days"); optional, so no gate.

## 6. Operator questions

1. "Self-review round 3 left one open item: D2's 'variants that need a missing provider disabled with the reason' is implemented and unit-tested, but no browser state can show it…" — options *Proceed and report the gap* / *Grey one variant in the fixture* / *One more review round first*; **selected "Proceed and report the gap"**. No free-text operator messages.

## 7. Rule check

- guard: none on this harness (unenforced), so "all kept" is not claimed; nothing was enforced.
- The run's own breaks: one shell call used `cd … && ./pm-mock-runtime/start.sh` (one command per call), and one used `./pm-web-app/stop.sh; ./pm-mock-runtime/stop.sh` (semicolon). Both were corrected afterwards (separate calls).
- Every build ran through the resolved maven canonical; every CI action through `ci`; every script through the executor. No build was backgrounded, none degraded, none was killed. Two invocation-level corrections: `-Dtest=` was refused by the executor (`test_selector_matched_nothing`) and replaced by the module run; the second gate run followed a flake.
- The spec was read from `ledger_worktree` (`spec_source`), with no `spec_differ` marker.
- `executor-skew` answered `match` (executor 0.1.2135, lane 0.1.2135).
- Restarts/re-dispatches: three self-review sub-agents (each report-only, none killed). No context compaction occurred.

## 8. Build server

- Setup preflight on the worktree: `ready` (registered, version 1).
- Builds: 39, all `daemon_longpoll`; `in_process_fallback`: 0; no degraded fallback, no launch-fault retry, no killed job.
- `runtime_stamp_reason`: none carried by resolve.

## 9. Residue

1. **D2's greyed variant has no rendered evidence** (the self-review budget-exhausted item, operator-decided): `unavailable` is implemented and unit-tested, and the design doc names the fact, but no mock state carries an entry — this fixture machine offers every landing, method and asking variant.
2. **Deliverables 5–10 of this spec are open** for later runs: `harness.none`, `auto.result`, `language.choose`, `catalogue.select`, `store.choose`, `harness.choose`.
3. The mock's step path does not persist `landing`/`method`/`asking` for `defaults.reconfigure` (found while writing the posture test; pre-existing, the PUT path does persist them) — the posture path does.
4. `roles.reconfigure` reports one role against `roles.review`'s four: the later run's grouping is exercised thinly.
5. `deriveStepFromState(undefined)` still returns 1, so the bar can mark step 1 for one frame before the first state arrives.

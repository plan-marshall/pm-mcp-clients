# PLAN-71 landing record

epic: ui-improvements
plan: rb-71-verify-web-onboarding (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` `unknown`, `plugin_version` `unknown`; executor 0.1.2183)
pr: #599, squash `3f0af941` (6 files) on `bac0222a`, merged through the queue 2026-11-27T20:32:01Z, head `0c2edf15`. The `skip-bot-review` label was set on the operator's word after CodeRabbit's quota refused four reviews. #596 was closed unmerged.
analysed: 2026-11-27, by sub-agent in three layers, by reading only (no build); the squash message spot-checked by the orchestrator

Messages:
- Findings `rb-71-verify-web-onboarding-001` (revision 1, `UC-V-02`) and `-002` (revision 5, `UC-V-05`).
- Landing `-003` (revision 2, 20:33:24Z): `merge_state=merged`, `deliverables_done=3` of 3, 3 of 3 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`.
- Four operator questions:
  - D2's `doc-review` clause is replaced by the AsciiDoc validators;
  - a confirming self-review round;
  - accept its two nit fixes;
  - stop and retry the reviewer.
- One operator-directed change outside the plan's boundary (see below).

**CI.** Real builds:
- merge group `3f0af941`: build 25 and 27 green;
- push to `main` (run 46064274310): build, `sonar-build` and the snapshot deploy green;
- Integration Tests green.

**Sonar.** `main` read 2026-11-27 after the task `AaIew9ze0uRB5MeaGUc9` (20:48:02Z, after this merge): gate OK, coverage on new code 90.2 %, `count_status: confirmed`, **3 open** (the PLAN-181 keys).

**The operator-directed key fix `2f7759a0`.** `main` at `5cb9cf0a` was latently red from the docs-only #600 (see Open Defects). This commit adds 7 keys:
- `plan.node.state.running_run_of`, `plan.leaf.job_running`, `plan.leaf.ci_wait`, `plan.view.replanned`, `plan.timeline.title`, `plan.timeline.open`, `overview.needs.overdue`.
- They go to `plan_view.{en,de}` and `overview.{en,de}`, verbatim from the design rows `plan-view.adoc:339,403,407,411,463,467` and `plan-overview.adoc:238`.
- en/de key sets are equal and the ICU arguments match. Both catalogue tests pass at `3f0af941`.
- No code uses the keys yet (L3-9).

## Layer 1: specified

| Done-when | Verdict |
|---|---|
| D1 `UC-V-02` verdict and evidence | met (`verify-web-onboarding.adoc:92-112`). "Fail" holds only against the check's regex `/[a-z]+\.[a-z-]+/`, which matches URLs and "e.g". No state id leaks on the 75 states captured as themselves; the 17 auto-advancing states are honestly `not verified`. The defect is in the check, not the app (L3-2, L3-3) |
| D1 `UC-V-03` | met as re-grounded (`:114-125`): `entry.*` has no web screen (`onboarding.adoc:45`); the fallback names `pm-operator` and shows no state id |
| D1 `UC-V-05` | met (`:127-141`, table `:151-226`). Header and language pass. The `harness.none` "fail" is literal: the app follows the design (`onboarding.adoc:197-217`) and the engine catalogue (core `14-machine-onboarding.adoc:277-283`); the stale artefact is `proposals.adoc:71-72`. Three clauses are `not verified` (the mock does not feed them) |
| D2 result file: every check, commit, date, environment | met (`:26-63,88-143`); one stale sentence (L3-4) |
| D2 matrix move | met: 9 rows (UC-ONB-1, 2, 3, 5, 6, 7, 8, 10 from partial; UC-ONB-9 from deviates); totals 14/12/2/1/10 = 39, consistent |
| D2 passes `doc-review` | met by substitution: the AsciiDoc validators, on the operator's word of 2026-11-26 |
| D3 one finding per fail | met: 2 fails, 2 findings |
| Standing constraint "no production change" | not held, on the operator's word (`2f7759a0`) |

**True count:** 3 of 3 deliverables met. The two `fail` verdicts are failures of the check's text, not of the app.

## Layer 2: implemented

6 files:
- Inside the boundary: `verify-web-onboarding.adoc` (new) and `analysis.adoc`.
- Outside the boundary, operator-directed: the four catalogue files.
- All cited SHAs exist.

**Stale or false claims:**
1. **The squash message and the PR body (major).**
   - They read `docs(review)` and "Nothing was fixed: this plan verifies only".
   - They name commits only up to `c3541d93` and never `2f7759a0`, `17dd25e4` or `3e2b4c22`.
   - So `main`'s history carries a production resource change under a docs commit that does not mention it. Only the landing message records it.
2. The PR body says -002 is at revision 4; it is at revision 5.
3. The landing message says CodeRabbit gave "one review" with 3 findings. It gave two: `c3541d93` with 2 findings and `16c91702` with 1.
4. The landing message says "Sourcery approved `d10b68ab`". GitHub shows only COMMENTED and DISMISSED reviews.
5. "Without it no head could be green" holds only for the local gate: CI skips docs-only builds.
6. The final head `0c2edf15`, which carries the code change, had no bot review (`skip-bot-review`).

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | major | The squash message and the PR body misstate the merge: a docs type, "nothing fixed", no `2f7759a0`. Permanent in `main`'s history | `3f0af941` |
| L3-2 | minor | The third `UC-V-02` "hit" is put on the model id `anthropic/claude-sonnet-4-6`, which has no dot. The regex matches only `e.g` | `verify-web-onboarding.adoc:105,190-191,294-295`; inbox -001 |
| L3-3 | minor | No regex sweep was run (the 92 literal ids were searched), so the three hits are not a proven complete list | `:99-101,108-112` |
| L3-4 | minor | "the branch adds only this file and the matrix edit" is false since `2f7759a0` | `:31-32` |
| L3-5 | minor | The count of `not verified` clauses differs: two in `analysis.adoc`, three in the check note and in -002, four in the rule paragraph | `analysis.adoc:14,17,230`; result `:139-141,269-272,200-203` |
| L3-6 | minor | The mock limits are named without linking the spec's `README.adoc#limits` | `:308-341` |
| L3-7 | info | The check links carry no anchor: `verification.adoc` has none per check | `:92,114,127` |
| L3-8 | info | `plan.view.replanned` de "{count} von {of} Mal neu geplant" is the design's verbatim text | `plan_view.de.properties` |
| L3-9 | info | None of the 7 keys is used by code yet | — |

## Routing of the two findings

PLAN-66 (`harness.none`) is shipped, and no staged, running or parked spec owns either finding.
- **-001 `UC-V-02`:** fix the check, not the app. In `doc/claude_review/use-cases/verification.adoc:38`, replace the regex with "any state id of the scenario", or exclude URLs, install hints and attribute text. An app change would go against the wireframe `onboarding.adoc:198-206`.
- **-002 `UC-V-05`:**
  - Correct `doc/claude_review/use-cases/proposals.adoc:71-72` to the design and the engine catalogue: the per-harness facts belong under `harness.blocked`/`harness.probe-failed`. Then UC-ONB-4 can become `conforms`.
  - Correct the stale `UC-V-03` text at `verification.adoc:43`.
- **Nearest vehicle for both:** PLAN-74 (parked, `web-design-docs-closure`, its surface holds `proposals.adoc` and `analysis.adoc`), as an amendment; or a trivial direct doc change with `skip-bot-review`. The operator decides.
- **The three `not verified` clauses** need a mock that feeds them (`settings.conflict`, catalogue advice, an unavailable variant). No plan owns that.

## Residue for the plans that touch the same surface

Code files: `plan_view.{en,de}.properties`, `overview.{en,de}.properties`.
- **PLAN-198 (staged):** its hypothesis "the 7 #600 keys are still absent" is refuted. The 7 keys are in the catalogue files, so their design rows become links, not "Texts to add" rows. Only `overview.needs.integrity_hold` goes to "Texts to add". Amended 2026-11-27.
- **PLAN-62 (running):** may touch `plan_view.*`. Its citation `plan.prune.*` at `:79-81` is now at `:85-87`. Additive, no conflict.
- **PLAN-135, PLAN-70:** no overlap.

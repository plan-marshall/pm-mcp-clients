envelope_version=1
sender_type=plan
sender_id=rb-54-web-reports-and-designer
epic=ui-improvements
kind=landing
created=2026-11-05T01:19:51Z

```landing-facts
schema=landing-facts/1
plan_id=rb-54-web-reports-and-designer
epic=ui-improvements
pr=#313
merge_state=merged
cleanup_owed=false
deliverables_total=1
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

**D1 Reports — met.** The pull request is #313, merged as `96286321`.

| Done-when | Verdict | Test | Order | Failing run |
|---|---|---|---|---|
| `reports.spec.js`: unread stand out; opening marks read | met with the order stated (the spec asked for test-first; the e2e test and the view were written in one working tree, so the committed fix was mutated instead) | `reports.spec.js` "sets unread rows apart with a dot and a bold subject, and opening reads them"; "marks a single report read from its row menu and removes it from view"; unit tests "moves the row it opens from unread to read…", "marks unread rows with a dot and the bold subject" | `mutation-after` | `8c2ff89` mutated: `pm-web-e2e/maven-2026-11-02-120258.log`, 1 failed / 333 passed, `data-unread="false"` at `reports.spec.js:81` |
| Browser check on `fully-onboarded` → `/reports` and a completion report; baseline and after snapshots plus screenshots | met | Playwright MCP, snapshots and screenshots `.plan/temp/playwright-mcp/54-{reports,completion}-{before,after}-{en,de}-{light,dark}-{desktop,phone}.{yml,png}` (before from the main checkout, after from the worktree) | `none` | — |
| Completion report shows the `Change` line with a size and none without | met | `CompletionReportView.test.ts` "shows the size of the change with its unplanned count", "leaves the Change line out when the plan carries no measured size", "leaves the unplanned part out when the change carries no unplanned count" | `test-first` | test-only `462f224`; `pm-web-app/maven-2026-11-02-104613.log`, 1 failed / 1127 passed |
| Open-issue read at the pull request head recorded | met | SonarCloud at the merge head `6a7f7dac`: confirmed 0 new issues, analysis revision = head revision | `none` | — |

Per-head test runs: `pm-web-app` 1134→1145 tests at each head; `pm-web-e2e` 334/358 tests green at `8c2ff89`, `0eeedb3`, `086bb3d`, `f5c01d3`, `9a477e9`, `6a7f7dac` (the merged head), with one pairing/designer timeout in three of the intermediate runs under other lanes' concurrent builds, each re-run green at the same head. Build gate at the merged head: `verify -Ppre-commit` green 2630 (`default/maven-2026-11-05-000734.log`), `verify` green 2630 (`default/maven-2026-11-05-001531.log`), e2e green 358 (`pm-web-e2e/maven-2026-11-05-002710.log`).

## Personas and skills

`plan-marshall:persona-integration-tester` for the e2e and the browser check; `persona-implementer` for the view and the mock; `persona-code-reviewer` for the three self-review rounds and the PR-comment triage. Domain skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security` (named by the deliverable), plus `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:lint-config` and `pm-dev-frontend-cui:cui-javascript-project` from the Standing Constraints. Surface drift: `pm-mock-runtime` (the size in the fully-onboarded fixture, named in the review) and `pm-operator` (the quality gate's import order and one aged transcript line) — reason: the browser check needed the size in the state it names, and the base's transcript had aged past its relative-time window with the machine's clock.

## Hypotheses

- The `Change` line was documented by PR #278, served by PLAN-82 in the contract and the `reports` mock, and never rendered — **confirmed**: `CompletionReportView.svelte:129-166`, `pm-api/.../overview/ChangeSize.java`, `reports-and-system.json:959-964`.
- ReportsView imported none of the new shared parts and used neither `DataTable` nor `TriagePage` — **confirmed**: `ReportsView.svelte:5-9` at `94bbefe1`.
- Unread and read looked alike, and the machine report sat in a project group — **confirmed**: `ReportsView.svelte:690` (subject always semibold), `:102` (machine keyed on a missing project); the fully-onboarded cleanup report carries `from.project` (`fully-onboarded.json:2779`) and sat under the project group in the before snapshot.

## Findings

Self-review round 1: the web-app README and the reports/`view-density` design sections were stale, the e2e active-row assertion was conditional, the row-menu keyboard contract and the reload merge were untested, the second flaky spec was untouched, three catalogue keys were orphaned, and the retained-row order was unasserted — all fixed. Round 2: the reload test was vacuous (the served page still carried the row), the retained row was prepended rather than kept in place, it could outlive its own detail, and two README sentences remained stale — all fixed. Round 3: the empty state repeated its title, the order assertion was still missing, and one intro sentence still attributed the layout modes to reports — fixed. CodeRabbit: six inline findings and two review-body nitpicks on the first review, then four more (retry clears its error, a disconnected successor, a retained row's repeated read, the body control where nothing is hidden) — all fixed and every thread answered and resolved; the browser height assertion and the `hasDetail` on a deleted subject were answered with reasons (no fixture body wraps at the tested widths and jsdom has no layout; the details action is disabled with its reason, not clickable).

## Reviewers

- **coderabbit** (required, quota-limited): reviewed `2f275a5a` at 16:31 and `2495daf8` at 23:10, both `COMMENTED`, marker `kind=reviewed`, merge risk Low. Every finding from both reviews is fixed in `0eeedb3`/`086bb3d`/`f5c01d3` and every thread resolved. Re-triggered after each push; the triggers at 21:11, 00:33 and 06a7f were refused on its own hourly allowance (recorded in the progress file). Standing review authorization applies: a qualifying review exists, every finding handled, one re-trigger after the last push, all other gates green on the current head.
- **sourcery** (optional): approved at `2495daf8`; its first trigger was refused on its own review budget.

## Operator questions

none asked.

## Rule check

guard: none on this harness (unenforced); no guard log line exists for the run. No section-1 rule was broken knowingly. Shell calls were one command each; files were read and searched with the file tools; temporary files live under `.plan/temp/`; every build ran through the build-maven verb (in-process, because the daemon's own `mvn` is absent from its PATH) and every CI action through the `ci` verb. One off-pattern command was issued and corrected: `python3 … ci pr thread-reply` was called before its body slot was prepared for that consumer, and the matching `prepare-comment --for thread-reply` was then run. Spec read: `spec_source=ledger_worktree`, `spec_differ=true` (the main-checkout copy differs; the ledger copy was read). `executor-skew`: `match` (executor 0.1.2036 = lane 0.1.2036). Browser servers started and stopped by this worktree only; no foreign process was touched.

## Build server

Setup preflight `ready`; the first routed build failed in the daemon (exit -1, empty Maven log — the daemon spawns `mvn`, which is not on its PATH) and every later build ran `in_process` through `./mvnw`. No `killed` job. `runtime_stamp_reason` was absent; `plugin_version` is `unknown` as resolve reports it.

## Residue

- The daemon cannot run this project's builds (its PATH lacks `mvn`); the run's builds are in-process. Nothing in the plan depends on it, but the machine's build server needs its PATH fixed by the operator.
- The e2e suite flakes about one test per run under concurrent builds from other lanes (pairing and one designer test); every failing head re-ran green.
- One golden transcript line in `pm-operator` was recorded because the fixture's age crossed its relative-time window — unrelated to the plan and reversible if the operator prefers the old fixture date.

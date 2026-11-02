envelope_version=1
sender_type=plan
sender_id=rb-35-web-data-components
epic=ui-improvements
kind=landing
created=2026-11-01T21:51:55Z

```landing-facts
schema=landing-facts/1
plan_id=rb-35-web-data-components
epic=ui-improvements
pr=#302
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
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

Guard state: `guard: none on this harness (unenforced)`.

# PLAN-35 — Web: Data Components: landing

PR #302, merged into `main` as `5650c41e0d205f9d2fb3b8a1d205bdd71c9ba41a` (merge queue, merge-group run 41353105517 succeeded). Branch `feature/35-web-data-components`, worktree `.plan/local/worktrees/35-web-data-components`, removed after the merge; the local and the remote branch are pruned.

## 1. Deliverables

| Deliverable | Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|---|
| D1 DataTable | Unit tests: column spec widths, numeric alignment, empty slot | met | `pm-web-app/src/lib/components/DataTable.test.ts` (11 cases) | `test-first` | base `418579a0`, log `efa19159d03444eb9f9a5b75c229c684`: `Failed Suites 1 — FAIL src/lib/components/DataTable.test.ts — Failed to resolve import "./DataTable.svelte"` | `d7ce0b7`, corrected in `417c44b` |
| D1 | Browser check: `/dev/components` shows DataTable, EmptyState, ErrorState, Skeleton in dark and light; baseline and after snapshots and screenshots | met | — | `none` | — | `d7ce0b7` |
| D2 EmptyState, ErrorState, Skeleton | Unit tests per component; reduced motion stops the shimmer | met | `EmptyState.test.ts` (6), `ErrorState.test.ts` (5), `Skeleton.test.ts` (10) | `test-first` | before `4f5ee0f`, log `143530be466d4ffd85ed4c510c7f7f41`: `Failed Suites 3 — Failed to resolve import ./EmptyState.svelte, ./ErrorState.svelte, ./Skeleton.svelte` | `4f5ee0f` |
| D2 | Browser check, same clause | met | — | `none` | — | `4f5ee0f` |
| D3 Components document | The document exists, is linked from the README, and passes the `doc-review` stand-ins clean | met | `pm-documents:ref-asciidoc:asciidoc validate` (0 errors, 0 warnings), `verify-links` (0 broken, 0 format violations), `pm-documents:ref-documentation:docs review` (0 issues) | `none` | — | `82ce1b6`, corrected in `5d96edc` |
| D3 | The open-issue read at the pull request head is recorded | met | `sonar read_open_issues --pr 302` at `eecf5a31`: `count_status: confirmed`, 0 open, 0 rules; `sonar-gate` `merge_allowed: true` | `none` | — | — |

`deliverables_done=3` of 3. Every Done-when whose order is not `none` carries the failing run's commit and its output identifier; the two `none` clauses are the browser check and the document/issue reads, whose evidence is the artifact set below.

Per-head test runs (`module-tests`, plus the gate's own tests): `418579a0` red (D1), `5895b1bb…` green after D1, `143530be…` red (D2), `76ac55d5…` green after D2, `5ce3d9e3…` green after the round-1 fixes, `a9af6a11…` green after the row-height fix, `5a7459cb…` green after the round-2 fixes, and `verify -Ppre-commit` green twice — `824fb3fa…` at `208ce8b6` and `9e3b010e…` at `eecf5a31` (589 s). Shas of the per-head Maven runs are the build-server job ids recorded in the run's progress file.

Browser artifacts (`.plan/temp/playwright-mcp/`, mock port 20350 scenario `fully-onboarded`, app port 20351): `plan-35-{en,de}-{1440,390}-{before,after}.png`, each with a `.yml` accessibility snapshot. Both themes are in every shot, because `/dev/components` draws its light and dark sections side by side. Measured in the browser at the final head: column widths 224/112/96/60 px, single-line rows 40 px default and 32 px compact, the sticky head stays at the top of its scroll area while the rows pass under it, and the document's scroll width is 1425 at a 1440 viewport and 375 at 390 — no horizontal page scroll.

## 2. Personas and skills

* D1 and D2: `plan-marshall:persona-implementer`, with `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing` and `pm-dev-frontend:javascript-security` — all four declared by the spec and all loaded.
* D3: `plan-marshall:persona-documenter`, with `pm-documents:ref-asciidoc` and `pm-documents:ref-documentation`.
* Self-review rounds 1 and 2, and the triage of the pull request's comments: `plan-marshall:persona-code-reviewer`.
* No deliverable declared a skill that does not exist; `resolve` reported none missing.
* `skills added`: `pm-dev-frontend:lint-config` was not needed (no lint configuration changed). The two `pm-api` message files were touched with no domain skill loaded: they carry catalogue keys only, no Java.

## 3. Hypotheses

The spec carries no `HYPOTHESIS` line. Its one `Claim Labels` entry is `OBSERVED` (the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-04` and `proposals.adoc#VD-P-05`). Confirmed at the head: `doc/claude_review/visual-design/plan.adoc:40-44` states VD-S-04 with VD-P-05 and VD-P-07 and names the four files, and `doc/claude_review/visual-design/proposals.adoc:113` carries `[#VD-P-05]` with the `DataTable` row at `:147` and the `EmptyState`/`ErrorState`/`Skeleton` row at `:163-165`. The spec's `Expected Surface` was confirmed at the base: the four components absent, the dev page and the README present.

## 4. Findings

Self-review round 1 (sub-agent, `persona-code-reviewer`) reported 18 findings; 15 were fixed and 3 accepted with reasons:

* Fixed: the sticky head had a scrollport that could never scroll; the compact row could not reach 32 px; every row menu carried the same accessible name; the menu column did not hold the phone's 44 px target; the skeleton's reduced motion was JavaScript-only; two comments and one prop contract were inaccurate; two test names claimed a height they do not measure; the empty slot's `colspan` with a menu column and the skeleton's listener cleanup were untested; the record's case counts were stale; four sentences of `components.adoc` were wrong.
* Accepted with reason: the prop lists in the document stay a summary; the row menu carries no advisory because its name is its act; the two `common.*.properties` are outside the spec's `Expected Surface` but load-bearing (the dev page renders every word through `$t(...)` and the catalogue guard fails on a missing key).
* Self-review round 2 verified every round-1 fix against the files and the CSS specificity, and found six low items; all were fixed in `eecf5a3` — the empty row's rule lost to the base cell rule, two density test names, the document's tokens rule, the untested `code`-without-`detailsLabel` arm, the stale counts, and the absence of a gate at the pushed head. Nothing was open after it: `self-review: clear` in two rounds.
* CI: every check green at `eecf5a31` (Maven Build, Integration Tests, Dependency Review, SonarCloud Code Analysis). One local module-test run failed on `src/lib/views/ProcessDesigner.test.ts > opens a pasted text in the editor` ("Test timed out in 5000ms"); the same command was green on the immediate re-run, in a file this plan does not touch. Recorded as a flake.
* Pull request comments: three, all bot status messages — Sourcery's quota notice, CodeRabbit's "review skipped: only excluded labels are configured: skip-bot-review", and SonarCloud's "Quality Gate passed, 0 new issues". No finding to fix, no thread to resolve.

## 5. Reviewers

| Reviewer | Kind | State | Head | Trigger and wait history |
|---|---|---|---|---|
| CodeRabbit | required (`quota_limited`) | refused (skipped by label) | none | No trigger was posted: the operator asked for the `skip-bot-review` label, and CodeRabbit's own comment names that label as its reason for skipping. |
| Sourcery | optional (`quota_limited`) | refused (quota) | none | It answered on its own with "you've used your own review budget of 250,000 diff characters for the last 7 days … request another review in 1 day and 2 hours". Never triggered, never waited on. |

Standing review authorization does not apply: it needs a qualifying review, and there is none. The merge rested on a question-tool selection that named the exact gap (§6 below), with CI green, `verify -Ppre-commit` green and a confirmed 0 open Sonar issues on the same head.

## 6. Operator questions

1. Asked: "The lane's gate (`verify -Ppre-commit`) just rewrote 178 Java files under `pm-api` and one under `pm-mock-runtime` … How should the run handle it?" — options: commit the rewrite separately (recommended) / drop the rewrite and keep it out of the PR / stop the run here. Selected: **commit the rewrite separately**. The free text that followed ("use skip-bot-review as pr label … Did you rebase on main? another plan already did this") led to merging `origin/main`, where the same rewrite already sits as `f5de8449`; the commit `d9014ab` therefore cancels out and the pull request carries only this plan's 31 files.
2. Asked: "PR #302 is ready except for one gap the lane's merge gate names: no required reviewer has reviewed head eecf5a31 … Do you authorize the merge without the bot review?" — options: merge now without the bot reviews / keep the PR open / trigger CodeRabbit and wait. Selected: **merge now without the bot reviews**.

Free text, listed separately: `cland separatly, use skip-bot-review as pr label  in order not to use another review. BUt beforey syou do so: Did you rebase on amin? another plan already did this` — answered by the merge of `origin/main` and by creating the pull request with the label.

An earlier question of the same shape was aborted by the operator ("re-ask") and carries no answer; it was asked again and answered as above.

## 7. Rule check

`guard: none on this harness (unenforced)` — no tool-call guard was installed for opencode, so nothing was enforced and no guard log can attest the run. What follows is the run's own account.

* Kept: every script went through `python3 .plan/execute-script.py …`; every build through `plan-marshall:build-maven:maven run` (never `./mvnw`, never `npm`/`vitest`); every CI action through `plan-marshall:tools-integration-ci:ci` (never `gh`/`glab`); no `python3 -c`, no `sleep`, no `git grep`.
* Not kept, counted against the run: shell file operations before the discipline held — `ls` (twice), `grep -n` (six times), `tail` (three times), `sed -n` (once), `head` (once), `find` (once) — where the file tools were owed. Two shell calls also carried a separator: one `lsof` pair joined by `;`, and one `ls` joined to a `find` by `;` and a redirect. No file was written through any of them.
* Read outside the repository root: two build logs the build tool itself returned (`~/.plan-marshall/marshalld/job-logs/…`) and the `build_server wait` results. No credential file, configuration directory or environment dump was read.
* Git: no `stash`, no `restore`, no `checkout --`, no `rebase`, no forced push. One `git add -u pm-api pm-mock-runtime` staged a module's tracked modifications as a set (179 files), on the operator's instruction to commit the pre-commit rewrite; every other commit was staged by name. One `commit --amend` re-worded an unpushed commit's subject.
* `.plan/` state: only `.plan/temp/` was written (progress, landing, commit message, the link report), plus the pull request body slot the lane prescribes.
* The specification was read from the shared ledger worktree (`spec_source: ledger_worktree`) and it differed from the main checkout's copy (`spec_differ: true`); the ledger copy is the one the run followed.
* `executor-skew`: `match`, executor `0.1.2036` against lane `0.1.2036`.
* Restarts and re-dispatches: the mock and the app were started, stopped and restarted twice for the browser check's before and after passes, and the mock once more after the catalogue keys were added; two self-review sub-agents were dispatched (rounds 1 and 2); no sub-agent was killed.

## 8. Build server

* Setup preflight on the worktree: `ready`, registered, version 1.
* Every routed build answered `resolved=routed`, `mechanism=daemon_longpoll`; none degraded. Two runs answered `status: running` and were re-attached with a bounded `build_server wait` on their job id.
* `build-routing`: `executor_in_worktree: present`, `execution_mode: auto`.
* One gate run hit the daemon's supervisory bound at 561 s (`timeout_source: explicit`, job `d69daf85…`; 2553 tests passed, 0 failed at the kill, inside pm-web-e2e 8/8). It was continued with a raised bound rather than re-submitted at the same one; the next two runs finished in 578 s and 589 s.
* The first gate run failed on two `pm-mock-runtime` `StartStopScriptsTest` cases because the browser check's mock held the worktree's pid file and `start.sh` took its "already running" path. Both servers were stopped and the same gate then passed; the failure is not this plan's code and was not retried blindly.
* A routed job submitted with a relative `--project-dir` fails, because the daemon runs inside the worktree; the absolute worktree path is used after that.
* No job was killed by the operator. `runtime_stamp_reason` was not reported by `resolve`.

## 9. Residue

* The four components have no caller in a view yet; the view plans of `VD-S-06` bring them in. The development page is their only fixture.
* On a phone the table keeps its table form and scrolls inside its own wrapper; the design's "a table becomes a list of rows" rule belongs to the view plans (`VD-S-06`), not to this component.
* `doc/design/web-app/README.adoc` was already non-compliant on four AsciiDoc header attributes before this change; the two rows this plan added introduce no issue, and the README's rewrite stays with `PLAN-74`.
* `verify-links` processes only `<<>>` and `xref:` links, so it reported 0 links for a document that links with `link:` and `image::`. All 17 pictures and every linked document and anchor were checked by hand instead.
* `total_tokens` and `plugin_version` could not be read on this harness.

envelope_version=1
sender_type=plan
sender_id=rb-185-web-shell-up-and-deletion-residue
epic=ui-improvements
kind=landing
created=2026-11-24T15:13:18Z

```landing-facts
schema=landing-facts/1
plan_id=rb-185-web-shell-up-and-deletion-residue
epic=ui-improvements
pr=#558
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=6
review_body_findings_handled=6
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

Guard: none on this harness (unenforced).

## Deliverables

| # | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| D1 — the e2e spec follows the removed Back | met | none | `overview.spec.js` 7/7 under `-Pe2e` | — | no change (PLAN-177) |
| D2 — a deletion is recognised by what the specification gives; an answered decision stays | met | test-first | `runtime.test.ts` five cases (invented event, answered decision, plan/epic/report/task, job binding) | `adb0d310` (branch start), `maven-2026-11-24-073150.log:1326/1342/1358` | `d7b6daaa`, `a58222fe`, `697e3225` |
| D3 — Up on a plan and an epic at every width | met | test-first (unit), mutation-after (the e2e phone assertion) | `PlanView.test.ts` 4, `EpicView.test.ts` 2, `work-list.spec.js` phone leg | unit `…074414.log:1399/:2201`; e2e `…082547.log:142` (breakpoint neutralised) | `206c6807`, `b8011d9d` |
| D4 — the work list shows the siblings of the open object | met | test-first | `WorkListColumn.test.ts` 4 (epic-scoped, project-scoped, epic-less plan, union) | `…074414.log:1361/:1380`; union red at `…094740.log:1158` | `c20b59cd`, `b8437043`, `87b42448` |
| D5 — what PLAN-144 left unused goes | met | none | `rg` over `pm-web-app/src` finds none of the four names; module `verify` green | — | `604a8785`, `3450064e` |

D2's breadth caveat: the failing run aborted at the plan leg, so the epic, report and task arms were not individually seen red; the test as written was seen failing against the unfixed code and the output is recorded. D2's other clause — an answered decision in view — was seen red (`…073150.log:1342`). The four destinations are four separate cases at the head (`runtime.test.ts`), all green.

Per-head test runs at pushed heads: `76eea166` (3403), `87b42448` (3405), `b8013…`/`c97f9f98` (3410 at the last web run), and the merge head `c97f9f98` with full `verify` green (187 web files, 3410 tests, 5637 surefire).

## Personas and skills

- Personas applied: `plan-marshall:persona-implementer` (all five deliverables, as the spec declares), `plan-marshall:persona-code-reviewer` (three self-review rounds and the PR-comment triage).
- Domain skills per deliverable: D1 `pm-dev-frontend:javascript, pm-dev-frontend:lint-config`; D2 `pm-dev-frontend:javascript, pm-dev-frontend:jest-testing`; D3 `pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:css`; D4 the same three; D5 `pm-dev-frontend:javascript, pm-dev-frontend:jest-testing`.
- Skills added for surface drift: none beyond one new module-local file, `pm-web-app/src/lib/utils/narrowLayout.ts` (+ its test), the mechanism D3 asks for and named in the PR body.

## Hypotheses

- D1's claim that `overview.spec.js` clicks a removed Back — **refuted**, already fixed by PLAN-177 (`pm-web-e2e/tests/overview.spec.js:182-187`); no code change.
- `cleanup.done` carries no ids — **confirmed** (`doc/specification/runtime-model/06-event-catalogue.adoc:45`).
- The cleanup text has a catalogue key — **confirmed** (`pm-api/src/main/resources/messages/system.en.properties:183`, `system.de.properties:183`); `web.nav.gone` has no other caller and stays as the no-decision notice (`common.en.properties:488`).
- `detail.plans` carries staged and ended plans — **partly refuted**: the record documents "active plans" (`pm-api/src/main/java/de/planmarshall/api/overview/ProjectDetailResponse.java:22-31`) while the mock's detail carries a `phase_state: ended` plan (`pm-mock-runtime/src/main/resources/scenarios/running-work.json:3971-3982`). The column lists what the detail serves; the gap is reported.

## Findings

Self-review, three rounds:

- D4 lost `relay-metrics` (the nested epic list omitted it while the flat list named the same epic) — fixed `b8437043`, its fixture seen red at `…094740.log:1158`.
- The cleanup sentence was not bound to its job and applied to `remind`/`ignore` — fixed `a58222fe`; the decider is read as `who ?? by`.
- The fallback took the project from any cached tree, could overwrite a navigation that began during the probe, and could overwrite a closed listener's own route — fixed `30f0bcbc` (matched resource, navigation intent + epoch, route check).
- `PlanView` passed the previous plan's epic while a new plan loaded — fixed `30f0bcbc`.
- An orphaned JSDoc and a dropped e2e visibility assertion — restored `3450064e`.
- A plan whose `epic_id` named an unserved epic was visible in no column — fixed `87b42448`.
- At narrow, Up vanished while the first read ran or had failed — fixed `b8011d9d`.
- Two Sonar new-code smells (optional chain, catch parameter name) — fixed `e1ae4896`.
- Still open and reported: the cleanup sentence's inputs (below), and the red-breadth gap in D2.

PR review (CodeRabbit, head `87b42448`): 5 inline findings + 1 outside-diff finding, all handled — `inline-1 … inline-5` in `30f0bcbc`, `outside-diff-1` in `b8011d9d`; every thread replied to and resolved. Sourcery: no review (its weekly budget was exhausted on the first trigger, and the later check passed without a review).

## Reviewers

- **coderabbit** (required, `quota_limited`, `review_complete_after_handling`): reviewed `87b42448` (marker-bearing summary, `coveredCommitId` = that head) and posted one follow-up review on `30f0bcbc`; later heads got rate-limit notices ("next included review" 27–41 minutes). Its findings were all handled, so the review stands at every head of this PR; no re-trigger was posted after the final pushes. Trigger history: `@coderabbitai review` at 11:39, "already reviewed the last commit" reply at 11:50, rate-limit notices at 12:11 and later.
- **sourcery** (optional, `quota_limited`): triggered at 11:39, refused on budget (250k diff characters, next in ~2 days); its check later reported success without a review. Optional, so no gate.

## Operator questions

1. "Self-review at round limit: O1 and O4 are open report items; how should I proceed?" — options {proceed and open the PR, run a fourth review round, stop here}; selected **"Proceed and open the PR"**.
   No free-text operator messages were part of this run.

## Rule check

- Guard: none on this harness (unenforced) — nothing to read at `.plan/temp/guard.log`; the checks below are the run's own account.
- Shell file operations: none for reading (every read and search went through the file tools). Shell writes were limited to one `git diff … > .plan/temp/plan-185/diff.txt` redirect, the screenshots the browser tool wrote under `.plan/temp/plan-185/`, and the mock/app start/stop scripts.
- Every build ran through `plan-marshall:build-maven:maven run` with `--project-dir` at the worktree, all routed (`daemon_longpoll`), no fallback and no retry.
- Todo-writes to `.plan/`: the progress file under `.plan/temp/`, the landing file, one `ci` body slot per body/comment (paths returned by `prepare-*`), and nothing under `.plan/orchestrator/` except the landing message this run files. The merge of `origin/main` carried five ledger commits from main; none was authored here.
- The `.plan/orchestrator` files changed in this branch's history came from the base branch merge, not from this session.
- Spec read: `resolve` read the **ledger worktree** copy (`spec_source: ledger_worktree`); no `spec_differ` was reported, so the main checkout's copy said the same.
- `executor-skew`: **match** (executor 0.1.2183, lane 0.1.2183).
- One rule was bent and is reported: the module build pushed at `30f0bcbc` carried a `svelte-check` error that the `package` canonical does not run (it is a `verify` phase), so CI turned red on that head. The fix was `b7a3a65c`; every later push ran `verify` first.
- Base evidence: a detached `185-…-base` worktree at `adb0d310` was created to demonstrate the D2 legs individually; the seam returned `executor_in_worktree: absent` ("generation exited 0 but wrote nothing new … copy-from-main is disabled"), which section 1 rules out as evidence. The tree was removed clean; no base tree remains. This is the blocker behind the D2 breadth caveat.

## Build server

- Setup preflight: `ready` (registered).
- Every build: `daemon_longpoll` (12 routed builds, 0 in-process fallbacks, 0 launch-fault retries), `queued_seconds` up to 254 when the daemon was busy.
- No `killed` job.
- `runtime_stamp_reason`: none.

## Residue

- The cleanup sentence cannot render against the catalogue's or the mock's `cleanup.decided` payload; it needs an engine/specification decision (carry `who` and `decided_at`). Reported in #558.
- `decision.closed` spells its field `closure` in the catalogue and the mock but `outcome` in the engine record and the client. Reported in #558.
- `detail.plans` is documented as active plans but the mock serves an ended plan. Reported in #558.
- The `-Pe2e` suite fails 11 tests on the base branch (the `/projects/onboard` → `/projects/onboarding` route rename in `projects.spec.js` and `theme-readability.spec.js`); untouched by this plan, not run by CI.
- `ReportsView.test.ts` exercises the close path by poking `runtimeStore.update(...)`; not rewritten, as the spec says.
- D2's per-subject red breadth is narrower than the test's four cases (the failing run aborted at the plan leg).

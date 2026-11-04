envelope_version=1
sender_type=plan
sender_id=rb-47-web-actions-by-state
epic=ui-improvements
kind=landing
created=2026-11-02T14:05:30Z
revision=1
amended=2026-11-02T16:48:55Z

```landing-facts
schema=landing-facts/1
plan_id=rb-47-web-actions-by-state
epic=ui-improvements
pr=#311
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
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:overridden,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-47 landing — amendment: the run has landed

This amendment replaces `rb-47-web-actions-by-state-001`, which recorded the run as stopped with PR #311 open. The PR was merged afterwards by a process outside this run, through the merge queue.

- PR: #311
- Merge commit: `f6504d56` (`feat(web): one primary act per state, driven by the act tables (#311)`), 2026-11-02T14:29:46Z
- Merge path: merge queue; merge-group run `41462533980` completed `success`; the PR carries no auto-merge request from this run
- Shape: a **squash** merge, so the branch commits (`f521428b`, `15672b40`, `3ba6d21e`, `38edefc9`, `b0da29b6`) are not ancestors of `main`; the content is
- Content check: the merge commit's diff against its parent is exactly the 35 files this run changed; against the run's head `b0da29b6` the only differences are three later-mainline lines that are not this run's (`Status: FINAL → AGREED` in `components.adoc` and `interaction-affordances.adoc`, `maven-antrun-plugin 3.1.0 → 3.2.0`)

## Review state at the merge

The merge went through **without the required CodeRabbit review** (PR #311 carries 0 reviews). This run never enqueued the PR and had not obtained a review: CodeRabbit refused on quota at 11:32Z (on #308), at 13:05Z after the 90-minute wait, at 13:15Z on the escalated `@coderabbitai full review`, and at 13:26Z on #311; Sourcery (optional) was quota-limited throughout. The run was parked at the operator's word (`Wait and resume later`) when the external merge happened. The lane's merge gate was therefore overridden by another actor, not satisfied by this run — recorded here so the epic's ledger does not read it as a reviewed landing.

Everything else that held before the merge still holds: CI green at `b0da29b6`, the full `./mvnw verify` green at `b0da29b6`, and Sonar at `b0da29b6` a confirmed 0 open new-code issues.

## Deliverables (unchanged)

- D1 — acts by state (`f521428b`): `byState.test.ts` met, order `test-first` (red at `6f4fd664`); browser check met, order `none`.
- D2 — ActionBar roles and ConfirmDialog (`15672b40`): unit tests met, order `test-first` (red at `f521428b`); browser check met, order `none`.
- D3 — system, devices and the shared panel (`3ba6d21e`, fixes `b0da29b6`): the three e2e specs per state and the danger-background assertion met, order `test-first` (red at `15672b40`); browser check met, order `none`; the open-issue read met, order `none` (0 confirmed at `b0da29b6`).

Browser evidence: `.plan/temp/playwright-mcp/47-<target>-<before|after>-<locale>-<theme>-<width>x<height>.png` plus one accessibility snapshot per target (`47-<target>-<before|after>.yml`), for `control-ready`, `decisions-open`, `system-draining`, `projects`, `paired-devices`, before at `6f4fd664` and after at `b0da29b6`.

## Cleanup

Owed work completed after the merge: the worktree was clean and was removed, the local and remote refs were pruned, and the main checkout was confirmed on `main` at `c7d9dcb8`.

## Residue

- The required CodeRabbit review of this change does not exist. If the epic wants the review on the record, it has to be requested against `main` or a follow-up branch; the landed PR is closed to further review credit.
- The behavioural notes of the first landing still stand: the drain banner lists items only where the runtime names them; `/projects` and `decisions.open` show no act change until PLAN-59 and PLAN-55 rebuild those views; the plan's end confirmation keeps its own reason-and-pull-request dialogue; and a waiting plan offers no "Open the decision" act because the API offers none.

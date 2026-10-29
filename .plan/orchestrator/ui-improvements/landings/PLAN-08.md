# Landing Analysis: PLAN-08 — Use-case document changes of both reviews

epic: ui-improvements
workstream: WS-01
pr: #259 (merge commit `eba84c17b313535be5c0de72d6d3da314ad7b9a7`, branch `feature/08-use-case-documents`); sibling `plan-marshall-documentation` PR #128 (squash `24eabd4`)

> Landing record for one shipped plan. Source: inbox message `rb-08-use-case-documents-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-29 against the merged PR, the CI
> checks, the merge diff, the Sonar read and the sibling repository's `main`. The work is documentation only; the
> run's own account of `doc-review` and of the build is not re-run.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 UC-PLN-7 and UC-PLN-8 as `DRAFT` | shipped-as-specified | In `plan-marshall-documentation`, `git show 24eabd4` (the tip of its `main` today) changes `doc/use-cases/03-plans.adoc` (+61), `doc/UseCases.adoc`, `01-onboarding.adoc` and `04-projects.adoc`. The wording, the template order and the index row are the lane's account; the file list matches. |
| D2 Variations neutral between the clients | shipped-as-specified | The same sibling commit touches `01-onboarding.adoc` (UC-ONB-1) and `04-projects.adoc` (UC-PRJ-1), with UC-PLN-2 and UC-PLN-4 in `03-plans.adoc`. Content not re-read. |
| D3 The operator's agreement and `doc-review` | shipped-as-specified | The agreement is the operator's answer "Agree as written" (question 2), recorded in the PR body. The use cases stay `DRAFT`: flipping them to `AGREED` is a separate act (lane F-09). `doc-review` results are the lane's claim. |
| D3b Open-issue read | met under the docs-only exception | See Routing. |

`deliverables_done=3` of 3 is consistent with that account.

**Surface delta** (computed here from the merge diff, because `inbox landing-check` answered `unmeasured`):
declared 2, realized 2, the two sets are equal: `doc/design/README.adoc` and `doc/design/shared/plan-overview.adoc`
were declared and are the only two files in `eba84c17`'s diff. No expansion, nothing declared and untouched. The
larger part of the work sits in the sibling repository, outside this repository's footprint by design (the spec's
write-boundary allows it where a deliverable says so).

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`, the version the registry pinned when the restart check read it earlier on 2026-10-29.
- Anomalies:
  - Executor skew at the start (0.1.2021 against 0.1.2028); the operator answered "already regenerated", the
    re-check read `match`. The plugin moved to 0.1.2032 during the run; no skew check ran against that version.
  - **A sibling pull request was opened and merged with `gh`** (`plan-marshall-documentation` #128). The lane's `ci`
    surface cannot target another checkout; the operator authorized it twice (questions 3 and 4). Recorded as an
    authorized rule bend, not a defect of the epic. It is the first sibling-repository change of this epic.
  - One server restart mid-wait (2026-10-29T09:23Z); the lane resumed from its progress file.
  - The first routed build failed on a relative `--project-dir` (the same failure as PLAN-10 and PLAN-25); one build
    hit the daemon's supervisory bound and was re-run with a longer timeout.
  - Shell file reads (`ls`, `wc -l`, `grep`, `which mvn`, `date`): guard `none` on this harness, recorded only.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `06a1dc2` and `2f6ac37` (one real finding, fixed in `2f6ac37`, thread resolved) and
  refused four times on quota. It never reviewed the merged head `3aceb315`. Sourcery refused on its budget. The
  merge ran under the standing review authorization and the operator's "Enqueue now". The lane left F-14 (no review
  of the final head) open at its round limit and says so.
- CI/merge: `ci checks status --pr-number 259` is `success`, 28 checks, all `pass` or `skipping`. `build / conclusion`
  passed; `build`, `quarkus-alignment` and `sonar-build` were skipped, as the docs-only rule says.
  `origin/main` carried `eba84c17` as the next commit after #264.
- Sonar: the landing reports `open_issue_count=unknown`, `open_issue_rules=unknown`. `sonar_rest gate-status --pr 259`
  answers HTTP 404, which is what a PR with no analysis gives. This is the `undecidable (docs-only skip)` outcome
  decided on 2026-10-28; the changed files (`doc/design/README.adoc`, `doc/design/shared/plan-overview.adoc`) are
  both `.adoc` under `doc/`, so they are inside the ignore list, and `build / conclusion` passed. Corroborated; this
  is not a blocker and not an anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-08 --status shipped`
- [x] row `pr` stamped `259`
- [x] row `landing` stamped `landings/PLAN-08.md`
- [x] row `plan_marshall_plan_id` stamped `08-use-case-documents` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-08-use-case-documents-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Docs-only Sonar watch:** PLAN-08's changed files stayed inside the ignore list and the outcome was `undecidable`,
  as decided. The watch stays open for PLAN-24 and PLAN-74.
- **UC-PLN-7 and UC-PLN-8 are `DRAFT`, agreed by the operator in the PR.** The dependents PLAN-36, PLAN-46 and
  PLAN-49 declare `PLAN-08` and the epic notes "UC-PLN-7 and UC-PLN-8 must be agreed first". The operator has
  agreed; the status flip to `AGREED` is a separate act and is not owned by any staged plan. Decide whether the
  dependents need the flip before they are emitted. Recorded as a Watch.
- **Follow-ups the drafts name** (lane residue): the `Changes to the Specification` part of UC-PLN-7 moves into the
  requirements and specifications (PM-ARCH-11, the runtime-model specification), and the two client designs gain the
  `credential` and `cleanup_due` kinds. No staged plan owns the client-design change. PLAN-36 (`needs`) and PLAN-49
  (attention inbox) are the nearest; check them at emit.
- **Sibling repository checker drift:** `plan-marshall-documentation`'s trace checker reports 18 broken watch anchors
  that come from `pm-mcp-core` commits, none in a file this change touched. Not this epic's.
- **Branch and worktree:** no `feature/08*` branch remains on the remote and no `08-` worktree is listed.

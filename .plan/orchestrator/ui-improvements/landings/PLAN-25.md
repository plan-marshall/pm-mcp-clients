# Landing Analysis: PLAN-25 — Web core components, controls

epic: ui-improvements
workstream: WS-02
pr: #260 (merge commit `8b19a4a7592ed07d4c62430e2b8a8137c4374ef7`, merge queue)

> Landing record for one shipped plan. Source: inbox message `rb-25-web-core-components-controls-001.md` (runbook
> lane, harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2028`), analysed 2026-10-29 against the
> merged PR, the CI checks, the Sonar gate, the merge diff and the declared Expected Surface. Test counts, the
> failing-first runs and the browser captures are the lane's claims; not re-run.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Button and IconButton | shipped-as-specified, additive as the re-grounded spec said | `Button.svelte` is modified (not created) and `IconButton.svelte` and `Button.test.ts` are added. The lane refuted the spec's HYPOTHESIS the same way the cleanup did (four variants present, 18 views import it) and found one more fact: ten call sites in three views already passed `size="small"`, a prop the component ignored; it now draws 28 px at `DecisionView` (6), `ReportsView` (2) and `ProjectExplorerView` (2). The browser baseline was the pre-change checkout answering HTTP 404 at `/dev/components`, which is a valid baseline for a page that did not exist. |
| D2 StatusLabel, CountBadge, Chip | shipped-as-specified | The three components and their three test files are in the diff. The lane replaced the attention colour pair after finding white on `#B86E00` at 3.99:1 (the enforced `--color-attention-text` on `--color-attention-subtle` now). |
| D3 ActionBar style | shipped-as-specified | `ActionBar.svelte` and `ActionBar.test.ts` are in the diff; the five-acts test and the focus contract (arrows, Home/End, Tab, Escape, skipped disabled entry) are the lane's account. |
| D4 Development page | shipped-as-specified | `routes/dev/components/+page.svelte`, `lib/dev/devRouteExclusion.ts` and its test, and `vite.config.ts` are in the diff. The production-build clause is met by unit tests plus a manual clean-build grep, not by an assertion on the built artefact (lane-stated residue). Sonar clause: see below. |

`deliverables_done=4` of 4 is consistent with that account.

**Surface delta** (computed here from the merge diff against the declared Expected Surface, because
`inbox landing-check` answered `unmeasured`): declared 8, realized 20, state `expansion_detected`.

- Declared and realized (8): `Button.svelte`, `IconButton.svelte`, `StatusLabel.svelte`, `CountBadge.svelte`,
  `Chip.svelte`, `ActionBar.svelte`, `Button.test.ts`, `routes/dev/components/+page.svelte`.
- Declared, untouched (0).
- Realized, never declared (12): the four test files `ActionBar.test.ts`, `Chip.test.ts`, `CountBadge.test.ts`,
  `StatusLabel.test.ts`; `lib/dev/devRouteExclusion.ts` and `devRouteExclusion.test.ts`; `lib/utils/uniqueId.ts`;
  `src/routes/+layout.svelte`; `pm-web-app/vite.config.ts`; `pm-api/.../messages/common.de.properties` and
  `common.en.properties`; `doc/design/web-app/interaction-affordances.adoc` (the spec's own constraint asked for
  it, so it is expected, not drift). The message names only `pm-api` and `+layout.svelte` as outside the surface;
  it understates by the build config (`vite.config.ts`) and the two new `lib/dev` and `lib/utils` files.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version`
  `unknown`. `executor_version` `0.1.2028`, equal to the registry pin.
- Anomalies:
  - Executor skew at session start (0.1.2021 against 0.1.2028). The operator answered in free text "already
    regenerated"; the lane re-ran `executor-skew`, read `match`, and only then started. So the executor has been
    regenerated: the cleanup's restart verdict `indeterminate` should now read clear (re-check).
  - **A rule the run broke and corrected:** it created `Button.test.ts` and `lib/utils/uniqueId.ts` in the main
    checkout before noticing that the edit tools resolve there, moved both into the worktree and checked the main
    checkout clean before any commit. No effect on the merged diff.
  - The first routed build with a relative `--project-dir` failed (the daemon resolved it inside the worktree and
    fell back to an uninstalled `mvn`); the same failure and workaround (absolute path) as PLAN-10's relative
    `--project-dir`; PLAN-19 met a related one (the daemon has no `mvn` on its PATH).
  - Two operator questions only (executor skew, the merge). Self-review: three rounds, 12 findings in round 1,
    all handled; `self-review:clear`.
  - Process deviations (`rm` five times, listed): guard `none` on this harness, recorded here only.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `df9fb08` and `1d494f9` (a `reviewed` marker names the head); all threads answered
  and resolved and CodeRabbit withdrew one finding. After the last push (`fa8c30e8`, the base merge) the
  re-trigger returned no review (the check stays `NEUTRAL`). The lane applied the standing review authorization
  (qualifying review on `1d494f9`, every finding handled, re-trigger recorded) and the operator confirmed the
  merge. Sourcery refused on its own budget and does not gate.
- CI/merge: `ci checks status --pr-number 260` is `success`, 30 checks, all `pass` or `skipping`, including
  `build (25)`, `build (26)`, `quarkus-alignment`, `sonar-build`, `integration-tests / test`. `origin/main` tip is
  `8b19a4a7`, the merge commit.
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none`. Measured at the merged PR:
  `sonar_rest gate-status --pr 260` is `OK` (reliability, security, maintainability ratings 1, duplication 0.0 %,
  hotspots 100 %; no coverage condition, as for PLAN-19). The two earlier findings (`S8786`, `S7770`) were fixed,
  not dismissed. Corroborated, no anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-25 --status shipped`
- [x] row `pr` stamped `260`
- [x] row `landing` stamped `landings/PLAN-25.md`
- [x] row `plan_marshall_plan_id` stamped `25-web-core-components-controls` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-25-web-core-components-controls-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Dependents unblocked on PLAN-25**: PLAN-31 (`web-core-components-containers`; also needs PLAN-76, shipped, so it
  is now ready) and PLAN-41 (also needs 31 and others). PLAN-35 follows PLAN-31.
- **PLAN-31 and PLAN-35 declare `routes/dev/components/+page.svelte` as "absent ... created by this plan"**. It now
  exists (PLAN-25 created it) and PLAN-31 says it overlaps with PLAN-25 on the page and "runs after it". Re-read both
  at HEAD before emitting: they extend the page, and `devRouteExclusion.ts` and `vite.config.ts` govern whether it is
  built.
- **Shared files PLAN-25 changed**: `common.{de,en}.properties` (PLAN-04, 06, 34, 39, 45, 46, 73 declare them),
  `+layout.svelte` (PLAN-04, 19, 39, 66), `interaction-affordances.adoc` (PLAN-47, 74). Re-read those specs at HEAD
  before emitting. The `dev.*` keys sit in the shared `common` pack, dormant without the page (a lane choice
  answered to CodeRabbit).
- **`size="small"` now renders** at ten call sites in `DecisionView`, `ReportsView`, `ProjectExplorerView`. The
  specs that declare those views (PLAN-43, 51, 57; PLAN-09, 54; PLAN-47, 59) should read their pages after the
  change; the 28 px height is deliberate.
- **Recurrence on the CodeRabbit watch**: the final head again carries no CodeRabbit review (here an unanswered
  re-trigger, not a quota refusal); the earlier qualifying review stands.
- **Lane rule slip**: files created in the main checkout before the worktree; corrected before any commit. Not a
  defect of the epic; recorded here only.
- **Executor regenerated** by the operator before this run: re-check the cleanup restart verdict.

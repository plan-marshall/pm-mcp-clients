# Landing Analysis: PLAN-19 — Web UI icon set

epic: ui-improvements
workstream: WS-02
pr: #255 (merge commit `cc5794c93d259127a122d5a5733f45e37ad760a3`, branch `feature/19-web-ui-icon-set`)

> Landing record for one shipped plan. Source: inbox message `rb-19-web-ui-icon-set-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2021`), analysed 2026-10-28 against the
> merged PR, the CI checks, the merge diff, the icon registry, the text-guard baseline and one of the lane's
> browser snapshots. The message was amended in place after the merge (`amended=2026-10-28T18:28:12Z`).

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 UI icons | shipped-as-specified, with one count corrected | `uiIcons.ts`, `Icon.svelte`, `uiIcons.test.ts` and the `ui/` directory are in the merge diff. The registry holds **41** names (10 nav, 14 act, 6 state, 11 kind) and the diff adds 41 SVG files; the PR body and the message say 42, which is wrong by one. The failing-first run, the grid / stroke assertions and the green runs are the lane's claims; not re-run. `19-header-cleanup-{before,after}.yml` and `19-harness-choose-{before,after}.yml` exist. |
| D2 No emoji | shipped-as-specified | `text-guard-baseline.json` carries no `emoji` entry at the merge commit (`git grep -c emoji` finds none; 431 lines removed). Views changed in the diff: `+layout`, `SystemView`, `PlanView`, `EpicView`, `DecisionView`, `ReportsView`, `ProjectExplorerView`, `ProcessDesigner`, `OnboardingView`, `ProjectOnboardingView`, `CompletionReportView`, `DecompositionReviewView`, `ControlActionsBar`, `NodeNarrative`, `SourceList`, `StepBar`, `WorkflowValidationResults`, `processNode.ts`. The 22-view × 2-locale zero-emoji run (`maven-2026-10-28-102239.log`) is the lane's claim; not re-run. |
| D3 Harness monograms | shipped-as-specified | `HarnessChoice.test.ts` covers the monogram, the served icon, the same-origin refusal and the load-failure fallback. In `19-harness-choose-after.yml` the monograms read `CC` (Claude Code), `C` (Codex) and `O` (OpenCode); `A` (agy) is not in that snapshot, and the other combinations were not re-read. |

**Surface delta** (computed here from the merge diff against the declared Expected Surface, because
`inbox landing-check` answered `unmeasured`): declared 9, realized 77, state `expansion_detected`.

- Declared and realized (8 entries, 48 files): the `ui/` directory (41 SVGs), `uiIcons.ts`,
  `uiIcons.test.ts`, `Icon.svelte`, `HarnessChoice.svelte`, `+layout.svelte`, `text-guard-baseline.json`,
  `workflow-visual-language.adoc`.
- Declared, untouched (1): `CleanupView.svelte`. The cleanup banner emoji sat in `SystemView.svelte`, which
  the plan's own hypotheses record (`SystemView.svelte:688`); `CleanupView` still exists and was not changed.
- Realized, never declared (29): 17 view, component and utility files (`SystemView`, `PlanView`, `EpicView`,
  `DecisionView`, `ReportsView`, `ProjectExplorerView`, `ProcessDesigner`, `OnboardingView`,
  `ProjectOnboardingView`, `CompletionReportView`, `DecompositionReviewView`, `ControlActionsBar`,
  `NodeNarrative`, `SourceList`, `StepBar`, `WorkflowValidationResults` and `processNode.ts`), three of their
  test files (`ControlActionsBar`, `DecisionView`, `ProjectExplorerView`), `HarnessChoice.test.ts`, `choice-screen.css`,
  `tokens.test.ts`, `advisoryTitle.test.ts`, `pm-api/.../messages/control.de.properties` and
  `control.en.properties`, `harness-icons.adoc`, and the two e2e specs `epic-view.spec.js` and
  `projects.spec.js`.
- The message says the only file outside the Expected Surface was "one e2e assertion main had broken". The
  diff shows two e2e specs (4 changed lines each) and the PR body names both: the project and epic count
  checks. Understated by one file, otherwise accurate.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version`
  `unknown`.
- Anomalies:
  - After merging `main`, the e2e run failed on assertions main had just changed (`epic-view.spec.js:170-171`);
    the operator chose "Fix main's assertion in this PR" (commit `503e2f38`), which is why two specs of other
    plans' surface are in this PR.
  - CI calls answered `Not authenticated` twice in a row; the operator answered "retry" and "trry now" and
    the run resumed.
  - The first routed build failed `execution_failed` because the daemon has no `mvn` on its PATH; every later
    build ran `in_process`.
  - Six operator questions in all (executor skew, dirty main checkout, authentication twice, the base-branch
    repair, the merge).
  - Reported and not changed by the plan: `act.favourite-on` reads as an outline star with a centre dot (the
    contract forbids a filled glyph), and 26 of the 42 (41) ids are delivered but not used by a view yet; the
    usage count was not re-checked.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `503e2f38` (four "Minor" findings, all judged real, fixed in `5de65f5e`, every
  thread answered and resolved). Five triggers; the first four were refused on quota and the one after the
  last push (`5de65f5e`) was refused too, so the merged head carries no CodeRabbit review. The lane applied the
  standing review authorization and the operator confirmed the merge (question 6, "Merge now under the
  standing authorization"). Sourcery refused on its budget and does not gate.
- CI/merge: `ci pr view 255` reads `state: merged`, merge commit `cc5794c9…`, the current `origin/main` tip.
  All 30 checks on #255 are `pass` or `skipping`, including `build / build (25)`, `build (26)`,
  `quarkus-alignment`, `sonar-build`, `integration-tests / test` and `build / conclusion`.
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none`. Measured at the merged PR:
  `sonar_rest gate-status --pr 255` is `OK` (five conditions, ratings 1, duplication 0.0 %, hotspots 100 %,
  no coverage condition), the SonarCloud check passes, and the Sonar MCP search for the PR returns no
  issues. Corroborated, no anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-19 --status shipped`
- [x] row `pr` stamped `255`
- [x] row `landing` stamped `landings/PLAN-19.md`
- [x] row `plan_marshall_plan_id` stamped `19-web-ui-icon-set` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Surface under-declared; one catalogue shared with the plan in flight.** PLAN-19 changed
  `control.de.properties` and `control.en.properties` in `pm-api` without declaring them. PLAN-10 (launched)
  declares both. Recorded as a Watch.
- **Staged specs that declare files PLAN-19 changed**, to be re-read at HEAD before they are emitted:
  PLAN-09 (five views, the baseline), PLAN-16 (the baseline), PLAN-73 (the baseline), PLAN-39 and PLAN-66
  (`+layout.svelte`), PLAN-43 (three views), PLAN-47, 51, 53, 54, 55, 57, 58, 59, 62 and 64 (a changed view,
  component or e2e spec each), and PLAN-68 and PLAN-69 (`HarnessChoice.svelte`). PLAN-16, 45, 72 and 73 also
  declare the `views/` and `components/` directories. PLAN-69 lists "Monogram when no icon is fetched" as its
  own deliverable, which PLAN-19 delivered; PLAN-68 and PLAN-69 are to be re-read for it. No spec was edited.
- **Dependents now unblocked on PLAN-19**: PLAN-25, PLAN-27 and PLAN-41 (each also needs others).
- **The `emoji` baseline entries are gone.** PLAN-09, PLAN-16 and PLAN-73 declare the baseline; whatever they
  assume about emoji entries to remove no longer applies.
- **The icon count is 41, not 42.** The landing, the PR body and the "26 of the 42" figure carry the wrong total.
- **Process deviations of the run** (`tail`, `sed -n` on build logs, one `grep -n`): recorded here only. The
  guard was `none` on this harness, so nothing enforced them.

# Landing Analysis: PLAN-01 — Web: Text Guard in Report Mode

epic: ui-improvements
workstream: WS-02
pr: #237 (https://github.com/plan-marshall/pm-mcp-clients/pull/237), merged as `1b39f219`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-01-web-text-guard-report-001.md`). The message was checked against the PR, the merge diff
> `5b76f1f8..1b39f219` and the code on `main`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Guard helper | shipped-modified | `pm-web-e2e/tests/helpers/textGuard.js` (+348) and `text-guard.spec.js` (+100): six named rules, proven by a test that failed first. The browser clause ("the raw words match the baseline") is unmet: the browser shows chips, aria text and theme-toggle glyphs that the guard does not scan |
| D2 Guard in the readability spec, report mode | shipped-modified | `theme-readability.spec.js` (+534) and `text-guard-baseline.json` (+753). The mutation check (a raw enum fails the spec) and the Sonar read are met. "Baseline lists CC-F-01 to CC-F-05" is unmet: CC-F-03 was fixed upstream by PLAN-04 before the baseline was taken, and CC-F-05 has no CC-P-09 rule |

The lane graded `deliverables_done=0` because of the unmet clauses. Both deliverables' code is on `main` and runs in report mode. The unmet clauses are spec wording, so they are folded into PLAN-73 (see Follow-Ups), not reopened here.

Beyond the declared surface: `data-model-text` markings in `pm-web-app/src/lib/components/ChatPanel.svelte`, `views/DecisionView.svelte`, `views/PlanView.svelte` and `views/ReportsView.svelte`. They are review-driven and exclude model-written text from the guard. These views are declared by later web plans (PLAN-09 and the view plans); none of those is in flight.

## Metrics and Anomalies

- Tokens: `unknown` (opencode). `plugin_version` `0.1.2021`, `executor_version` `0.1.2017`, read at landing time.
- Duration: spread over several sessions from 2026-10-26 to 2026-10-27T13:38Z. This run is a resume of earlier sessions.
- Model: `deepseek-v4.1-flash/high` on opencode.
- Anomalies:
  - **Port collision with PLAN-03**: the e2e suite and PLAN-03's mock both used `7420`. The operator chose twice to wait for the other lane. The run reports several wasted hours. PLAN-76 (in flight) is the fix.
  - The opencode flat install again failed to resolve: no bundle, empty architecture, missing scripts. The operator refreshed the install and the architecture, then had the executor regenerated. `.plan/execute-script.py` was rewritten by the run on operator instruction, a `.plan/` write outside `temp/`.
  - Spec defect, again: `-Pintegration-tests` for `pm-web-e2e` skips Playwright. PLAN-01 was in flight when the 27 staged specs were corrected; it ran `-Pe2e`.
  - Build server: both routed builds failed at process start (bare `mvn`). This is the fourth recurrence.
  - The `SystemView`/`CleanupView` light-narrow contrast case fails on `origin/main` without these changes; it was re-run green as a flake.

## Routing and Merge Behavior

- Review: CodeRabbit gave three rounds of findings, all fixed (`bc64d59`, `4c980b19`, `91edd5b6`, `b2bbe3db`). It last reviewed `e1cd72bc`, then was rate limited. Sourcery gave 15 findings over two reviews, fixed or declined with a reason. The operator merged on the standing authorization.
- CI/merge: 30 checks green at the merge head. Merged through the merge queue (merge-group run 40512142038). No conflicts with PLAN-05.
- Sonar: `open_issue_count=0`, `open_issue_rules=none` at `ab8fa07c`, the head analysed.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `237`
- [x] row `landing` stamped — `landings/PLAN-01.md`
- [x] row `plan_marshall_plan_id` stamped — `01-web-text-guard-report`
- [x] PLAN-73 folded: the guard gaps added to D1, and its Expected Surface corrected (the three guard files now exist; `text-guard.spec.js` added)
- [x] epic.md: Open Defect recurrence; watch recurrences (build server, contrast case now also `SystemView`)
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- Folded into PLAN-73 (`web-text-guard-failing`, CC-S-07): chips, aria labels and glyphs not scanned; no rule for CC-F-05; split tokens; the browser clause as "every violation the guard lists appears in the browser snapshot".
- CC-F-03 needs nothing: placeholders in titles are fixed by PLAN-04.
- The PLAN-01 dependency of PLAN-09 (`web-display-layer`) is now met.

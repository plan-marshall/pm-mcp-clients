# Landing Analysis: PLAN-129 — What the Reports Fourth Residue Plan Left Open (PLAN-107 Residue)

epic: ui-improvements
workstream: WS-03
pr: #406 (https://github.com/plan-marshall/pm-mcp-clients/pull/406), squash `d1a9e63a`

> Written by the `analyze` verb (2026-11-11) from `rb-129-web-reports-fifth-residue-001.md`, verified against git,
> `ci pr reviews`, `runbook_lane review-gate`, the progress file and the Sonar read. Runbook lane, opencode,
> `deepseek-v4.1-flash/high`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Mark-all stays truthful in its races | shipped-as-specified (in substance) | rollback keeps a counted read (`ReportsView.svelte:573-584`, test `:1966`); retention merge pinned (`:2005`); pending guard pinned (`:2056`, rejected path only); the Skeleton clause holds only while rows remain |
| D2 RowMenu, age text, vacuous asserts | shipped-as-specified | scroll guard `RowMenu.svelte:79-85` (test `:542-568`; its `scrollTop` asserts cannot fail in jsdom); advisory suppression; CompletionReportView focus (`:80-97`, test `:334-365`); 30/59 days `last month` (`format.test.ts:93-94`); disabled contrast |
| D3 Tests, texts and the CLI hint | shipped-modified (evidence order `mutation-after`, not `test-first`, as the lane says) | `ReportsScenarioTest.java:153-180`; `ReportsCommand.java:245` hint re-pointed to `reports`; theme labels; designs `reports.adoc:182`, `24-reports.adoc` |
| D4 Open review items answered | shipped-as-specified | the review body of #394 answered (comment 6772099913, per the lane) |

Added-unplanned: `common.de.properties:9` intro word `Themes` (small, unrequested).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`) — reason: race logic across `ReportsView`, `RowMenu`,
  `CompletionReportView` and the CLI. Re-read by the orchestrator at `d1a9e63a`: `ReportsView.svelte:240-247, 389-395,
  500-512, 565-590, 944-948`. Confirmed.
- **Verdict:** layer 1 holds (minor: the D1 wording mixes test-first with revert; overlap with PLAN-70 D3/D4 in
  `Button.test.ts`); layer 2 findings (5 minor: the in-flight test never runs the success response; no Skeleton test when a
  mark-all empties the list; jsdom-vacuous `scrollTop` asserts; no negative test for the CLI hint; `ReportsCommandTest`
  without Generators); layer 3 findings (4 minor: a failed single Mark read forgets a counted read `:506-510`; a
  counted-read row can outlive its detail `:243-246`/`:577`; no response-order guard in `loadReports` `:389-432`;
  `pendingReadIds` is a Set `:585-588`). No blocker or major.
- Confirmed sound: the RowMenu guard (capture-phase target containment), the focus logic, the age boundaries (elapsed ms,
  DST-proof), the CLI hint and exit code; no `{@html}`, no timer waits.

Routing (operator, 2026-11-11): all residue delivered to the running PLAN-158's mailbox
(`inbox/to/rb-158-web-error-pattern-residue/orchestrator-001.md`, `destination: mailbox`), which already edits
`ReportsView`; PLAN-158's row stamped `rb-158-web-error-pattern-residue` so the delivery resolves.

## Metrics and Anomalies

- Tokens / plugin version `unknown` (opencode) — `landing-check` `complete: false`; folded into the standing Open Defect.
- `self-review:budget-exhausted` (two items open, the operator chose to continue).
- Rule breaks reported by the lane: shell file operations (`ls`, `wc`, `tail`, `grep`, `sed`, `git show/log/diff`).
- The lane's `resolve` emits an inbox read with the unprefixed plan id, which the orchestrator refuses
  (`invalid_target_plan`); the operator chose the `rb-` form — the standing Open Defect on the mailbox id.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `396ef322` (merge rule met); later heads refused on quota; standing authorization.
  `review-gate --pr-number 406`: `unresolved_review_threads=0` (landing 0), `review_body_findings_total=3` (landing 3) —
  agree. `review_body_findings_handled=3`: the progress file disposes all three inline items and the outside-diff one
  (`0d515770`) — agrees.
- CI/merge: via the queue on the operator's word.
- Sonar: claimed `0` (confirmed by the lane at `bcbadae1`, gate OK). Re-read here: `sonar read_open_issues --pr 406`
  → `undecidable` (`head_revision_unavailable`, a merged PR); the Sonar MCP connection is down this session. Not
  re-measured — recorded as the lane's claim.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 406; `landing` `landings/PLAN-129.md`; `plan_marshall_plan_id` `rb-129-web-reports-fifth-residue`
- [x] residue mailed to PLAN-158 (operator)
- [x] epic.md: incomplete-landing recurrence
- [x] resume anchor; `queue-view.md` regenerated

## Follow-Ups

- PLAN-130 (`web-brand-mark-and-waiting-mark`) waited on PLAN-129: unblocked.

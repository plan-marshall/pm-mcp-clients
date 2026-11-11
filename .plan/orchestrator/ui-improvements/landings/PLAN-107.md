# Landing Analysis: PLAN-107 — Web: Reports, Fourth Residue

epic: ui-improvements
workstream: WS-03
pr: #394 (https://github.com/plan-marshall/pm-mcp-clients/pull/394), merged as `0913d6f3` (squash through the merge queue, 2026-11-10; parent `be8c8403`; 28 files, +815/-154)

> plan-marshall lane (harness opencode, model deepseek-v4.1-flash/high; 8.65 M tokens, 62 853 s). Inbox messages
> `web-reports-fourth-residue-001` … `-007` (candidate lessons) and `-008` (landing). Checked against git, the PRs
> (`ci pr comments` of #394, #348, #340, #325), SonarCloud and a read-only leaf; key points re-read by the orchestrator
> at `0913d6f3`. Analysed 2026-11-10.

## Deliverable Fidelity vs Spec (with the Amendments of 2026-11-07 and 2026-11-09)

| Deliverable | Verdict | Evidence |
|-------------|---------|----------|
| D1 mark-all marks the shown ids, reloads the first page, counts right | met in core | `ReportsView.svelte:561` reload; retained rows merged (`~:393-413`); deep-linked unread counted (`:543`, test can fail); e2e `reports.spec.js ~178-212` |
| D1 "no skeleton flash" (amendment) | not met | `loadReports` sets `isLoading` (`:387`) → Skeleton replaces the list (`:920`) on every mark-all |
| D1 rollback keeps a counted marker (D1d) | not met | catch block `:562-572` is the code of `be8c8403:547-554` unchanged (re-read); the PR body's "rollback deletes only the markers this request added" describes the old code; no test |
| D1 both count mechanisms fail when reverted; reload before the POST response | half met | `:543` pinned; the `:412` retention mechanism and the in-flight reload have no test |
| D2 CLI reads the list and posts ids; mock refuses `all`/no ids (422 `report_ids_required`) | met (separately) | `ReportsCommand.java:220-247`, `ReportsCommandTest ~120-185`; `MockRuntimeEngine.java:5603-5625`, `ReportsScenarioTest ~171-195`; `--all` marks only the first page with no "older remain" hint |
| D3a non-prefix selection in `ReportsScenarioTest` | not done | still the first 5 ids |
| D3b German theme labels asserted at page level | not done | German only in `MessageCatalogueTest`; German wording changed ("Farbschema") unasked |
| D3c, D3e, D3f | met | catalogue + `interaction-affordances.adoc:36,59`; duplicate test removed with coverage kept; adoc blank line, double loading text (no test), focus kept |
| D3d contrast test without pinned hex | partly | `RowMenu.test.ts ~285-310` hex regex asserts tautological; `bg-subtle` and the `prefers-color-scheme` block not read |
| D4 review answers on #348, #340, #325 | met | re-read: 6 / 10 / 10 threads, 0 unresolved, all replied |
| Amendment (a) RowMenu scroll guard, (b) `row-menu.spec.js` wait, (c) `format.test.ts` 30-59 days, (e) vacuous absence asserts | not done | no trace in the diff (`RowMenu.test.ts` +6/-4 only for the advisory; `ReportsView.test.ts:731-732`, `reports.spec.js:321` still present) |
| Amendment (d) e2e expects 7 | met | `reports.spec.js:61` |
| Evidence orders `test-first` (D1, D2), `mutation-after` (D3) | not met | PR body: failing-run traces "are not preserved"; only passing runs |

The lane reports `deliverables_met=9`, `criteria_unevidenced=0`; graded against the spec's own text this does not hold.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`). The leaf read the review body's
  wording through `gh` directly (outside its contract, used as data only) — noted.
- **Verdict:** layer 1 findings — 3 (D1 Done-when unsatisfiable as written; `shared/reports.adoc#read` not updated;
  Button's native `disabled` on `loading` elsewhere); layer 2 — 8 not or partly done; layer 3 — 1 major, 6 minor, 3 info.
- **Re-read by the orchestrator at `0913d6f3`:** `Button.test.ts:167-173` `blockOf` against `tokens.css:5` (comment
  names both selectors) and `:9` `:root {` — both theme blocks resolve to the light `:root` palette; the mark-all
  rollback identical at `be8c8403:547-554` and `0913d6f3:565-572`; the amended files untouched (`git diff --stat`).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `pm-web-app/src/lib/components/Button.test.ts:167-173,191-192` | any dark-palette regression | `blockOf` finds `[data-theme='…']` in the header comment of `tokens.css:5`, so the "light" and the "dark" block are both `:root`; the 4.5:1 test "in both themes" never evaluates dark | major (test validity) |
| L3-2 | `ReportsView.svelte:562-572` | mark-all in flight with the detail open; `report.read` for the retained row; POST fails | marker deleted, row stays and shows unread although counted read | minor |
| L3-3 | `ReportsView.svelte:387,920` | any successful mark-all | Skeleton flash, rows remount, `actionError` cleared | minor |
| L3-4 | `CompletionReportView.svelte ~71-88` | focus moved elsewhere while the report fetch is pending | the heading takes focus when the fetch resolves | minor |
| L3-5 | `RowMenu.svelte:34-38` | — | `triggerAdvisory` suppression without a test | minor |
| L3-6 | `ReportsCommand.java:220-247` | more than one page of unread reports | `reports read --all` marks the first page and says nothing of the rest | minor |
| L3-7 | `ReportsView.svelte:506-510` comment; `CompletionReportView.svelte:58-67`; `Button.svelte:99-107` disabled pair | — | comment overstates the race guarantee; detached invoker; disabled contrast unasserted | info |

### Conformance

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `{@html}`/`innerHTML` with untrusted text; no timer waits; ES modules | conforms | diff |
| CLAUDE.md Java | no generic Exception | conforms | `MockRuntimeEngine.java:5609` `IOException` |
| Spec | evidence orders | deviates | see above |

## Metrics and Anomalies

- 8 653 549 tokens, 62 853 s wall. `plugin_version` unknown (no `bundle_version` in the status stamp).
- Functional review closed by an operator override at `39c540e8` on three weakened-test removals (`accepted_test_removals`); the leaf found them spec-sanctioned (D3e duplicate, D1 server-driven rewrite, RowMenu hex pins).
- `pm-web-e2e` (375 Playwright tests) run locally only; CI covers the ports guard (Open Defect 5).

## Routing and Merge Behavior

- **Review (re-read from `ci pr comments`):** #394 — one CodeRabbit review body (4 nitpicks, base `0cc6a66d`, head
  `46f85869`), 0 threads; later heads skipped (allowance). All 4 nitpicks are FIXED in code (`Button.test.ts:144`,
  `CompletionReportView.svelte:44-67`, `:49-55` with tests `:311,:321`, `try/finally` `:341-405`), but NO PR comment
  answers the review body (the two triage comments answer the walkthrough and the Sonar comment). **Merge rule NOT met
  on the letter** (all review bodies answered); the lane's `review_body_findings_handled=4` counts code fixes.
- **Sonar:** PR #394 0 issues (one `java:S3776` fixed before merge), gate OK.

## Candidate Lessons (messages 001-007, archived)

- Ours (this repository's practice): 001 a narrowed flag is re-described on every surface in the same change; 002 an
  async button owes focus retention and an in-flight cue, both asserted; 003 read Sonar's new-code result before calling
  a review clean.
- plan-marshall-side (for the plan-marshall lessons epic, not routed from here): 004 re-verify residue claims at HEAD
  at outline; 005 a rewriting deliverable names the assertions it retires; 006 classify a script failure before
  retrying; 007 change-ledger row with the commit.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 394; `landing`; `plan_marshall_plan_id` `web-reports-fourth-residue`
- [x] epic.md Watch for PLAN-107; merge-rule miss (body unanswered) recorded
- [x] messages 001-008 archived; resume anchor; `queue-view.md` regenerated

## Follow-Ups

- Residue (operator's route): L3-1 (the Button test's theme blocks), D1d rollback + its test, the in-flight reload and
  `:412` mechanism tests, the Skeleton flash, D3a, D3b, amendments (a), (b), (c), (e), `shared/reports.adoc#read`,
  `--all` "older remain" hint.
- Unblocks PLAN-123 (operator 2026-11-10) and PLAN-45.

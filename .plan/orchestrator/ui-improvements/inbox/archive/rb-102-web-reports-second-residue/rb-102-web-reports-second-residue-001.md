envelope_version=1
sender_type=plan
sender_id=rb-102-web-reports-second-residue
epic=ui-improvements
kind=landing
created=2026-11-06T02:22:33Z

```landing-facts
schema=landing-facts/1
plan_id=rb-102-web-reports-second-residue
epic=ui-improvements
pr=#340
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

# Landing Report: PLAN-102 (Web: What the Reports Residue Plan Left Open)

- **Epic:** `ui-improvements`
- **Plan ID:** `102-web-reports-second-residue`
- **Sender ID:** `rb-102-web-reports-second-residue`
- **Pull Request:** [#340](https://github.com/plan-marshall/pm-mcp-clients/pull/340)
- **Merge State:** `merged`
- **Merge Commit:** `95a2d8555c3abd3603e2a91edcb336d87f7a51f8`

## Deliverables Summary

### Deliverable 1: Reports page keeps state across reload; headings true at every width (4/4 Done-when met)
- Unified dual `<TriagePage>` instances in `ReportsView.svelte` into a single stable instance with a `hasFilters` prop guard on the filter slot, ensuring open report details and query states survive reloads.
- Decided and documented heading outline in `doc/design/shared/completion-report.adoc`: `<1100px` uses an `h1` on the report title when detail replaces list; `>=1100px` keeps `h1` on Reports list heading and `h2` on report title. Implemented dynamic `titleLevel` prop on `CompletionReportView.svelte`.
- Unfiltered mark-all posts `markAllReportsRead(undefined)` sending `{ all: true }`, ensuring all unread reports on the server are marked as read without leaving unread reports behind.
- Unified query tokenization with alphanumeric filtering across `isFiltered` and `matchingItems`.
- **Evidence:** Order `test-first`; failing test commit `7ca69c6` (`maven-2026-11-05-164709.log`), passing commit `5ead803`.

### Deliverable 2: Row menu follows design (2/2 Done-when met)
- Visible focus ring with `--color-brand` outline with measured >3:1 contrast against background in light and dark themes.
- Enforced 44px phone touch target on menu items via `@media (max-width: 767px) { min-block-size: 44px }`.
- Restored focus to trigger on `choose()` item selection.
- Assigned accessible `aria-label` name to popup menu list.
- Scoped Escape key handling to menu container to prevent swallowing outside Escapes.
- Created `/dev/components` fixture section with sample menus across light and dark themes.
- Updated `doc/design/web-app/components.adoc` and added SVG diagram `doc/resources/diagrams/web-component-row-menu.svg`.
- Completed Playwright MCP browser check across 1440x900 and 390x844 viewports, English and German, light and dark themes, retaining 16 screenshots under `.plan/temp/playwright-mcp/`.
- **Evidence:** Order `test-first`; failing test commit `427217e` (`maven-2026-11-05-170902.log`), passing commit `07168bc`.

### Deliverable 3: Tests can fail and evidence is readable (1/1 Done-when met)
- Addressed nitpicks: asserted visible labels, mixed disabled/enabled keyboard navigation, and explicit `.report-title` wait.
- Added tests for `unreadCount` tracking preventing double decrement on duplicate SSE read events.
- Added test retaining active report beside list during filtered mark-all.
- Hardened focus restoration test waiting on DOM detachment.
- **Evidence:** Order `mutation-after`; mutation evidence in `maven-2026-11-05-172946.log` (13 test failures on reverted `markRead`), E2E test run in `maven-2026-11-05-173256.log` on assigned ports 21020/21021. Pushed in commit `77ffd42`.

### Deliverable 4: Every finding answered (2/2 Done-when met)
- Addressed all 10 review threads across reviews (8 from review 1, 2 from review 2):
  1. `PRRT_kwDOVBOtss60Vcgr` (Heading outline): Addressed in `9457c68d`.
  2. `PRRT_kwDOVBOtss60Vcgy` (Bulk mark-all retention pre-marking): Addressed in `9457c68d`.
  3. `PRRT_kwDOVBOtss60Vcg4` (Retention restricted to shownIdSet): Addressed in `9457c68d`.
  4. `PRRT_kwDOVBOtss60Vcg_` (Local unread count reconciliation): Answered with explanation; resolved.
  5. `PRRT_kwDOVBOtss60VchC` (Mark-all concurrency): Answered with explanation; resolved.
  6. `PRRT_kwDOVBOtss60VchI` (Loading/error state heading): Answered with design system explanation; resolved.
  7. `PRRT_kwDOVBOtss60VchM` (Duplicate event test payload): Fixed in `9457c68d`.
  8. `PRRT_kwDOVBOtss60VchU` (Dev fixture accessible names): Distinct accessible names assigned in `9457c68d`.
  9. `PRRT_kwDOVBOtss60WVrM` (Unfiltered mark-all payload): Explained spec D1c requirement; CodeRabbit withdrew request and resolved.
  10. `PRRT_kwDOVBOtss60WVrR` (Dev fixture popup viewport alignment): Explained out-of-scope for D2 component fixture; resolved.
- Total review threads resolved: 10/10 (29/29 inline comments resolved).
- Confirmed Sonar open issues on PR head: 0 open new-code issues (`count_status: confirmed`, `sonar_open_new_issues=0`).

## Quality Gates and CI
- Build gate (`verify -Ppre-commit`): 2719 tests passing.
- Full local verify passed cleanly.
- CI pipeline run `42146755624`: all 20 checks green.
- Merge queue settled merged: commit `95a2d8555c3abd3603e2a91edcb336d87f7a51f8`.

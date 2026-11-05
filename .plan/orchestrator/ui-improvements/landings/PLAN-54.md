# Landing analysis: PLAN-54 web-reports-and-designer

PR #313 `96286321` (squash-merged 2026-11-05T01:18Z; head `6a7f7dac`; 17 files). Lane `rb-54-web-reports-and-designer`: landing message filed 2026-11-05T01:19Z (`merge_state=merged`, deliverables 1 of 1, self-review `clear`, open Sonar issues 0). Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the two medium defects below were re-read by the orchestrator at `96286321`.

## Merge rule

CONFORMS. CodeRabbit reviewed several heads (`2f275a5a`, `0eeedb39`, `2495daf8`, and replies on `086bb3dd`, `f5c01d37`); GraphQL shows 0 unresolved review threads on the PR. Findings handled: 6 inline threads and 2 body nitpicks of `2f275a5a` (fixed), 3 threads and 1 body nitpick of `0eeedb39` (the browser-height assertion was answered with a reason; CodeRabbit replied that it will not require it), 1 thread of `2495daf8` (fixed in `086bb3dd`). After `f5c01d37` no review arrived (the 00:33Z trigger was rate limited). sourcery-ai: no findings. SonarCloud comment: Quality Gate passed, 0 new issues.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| 1 unread stand out, opening marks read (`reports.spec.js`, order `test-first`) | MET in code (`ReportsView.svelte`: `isUnread`, the dot, weight 600, `data-unread`, `markRead` from `openDetail`); ORDER NOT MET: the lane says `mutation-after` and the mutation proves the unread half only | yes |
| 2 browser check on `fully-onboarded` | MET: 64 snapshot and screenshot files under `.plan/temp/playwright-mcp/54-...` exist; two "after" snapshots opened (en and de `Change` line) | yes |
| 3 completion report `Change` line with size, none without (`test-first`) | MET (`CompletionReportView.svelte`, three tests, `report.completion.change` in en and de, `client.ts` `ChangeSizeRecord`) | yes |
| 4 Sonar open-issue read | MET (bot comment 0 new issues; "analysis revision = head" is the lane's word) | |

Also delivered: machine group, row-menu "mark read", triage list.

## Deviations

- The row menu is NOT taken from `DataTable` (as the spec says) but built from `IconButton` plus a raw `<button role="menuitem">` (`ReportsView.svelte` ~1176-1209); the spec's constraint says "no plain text button". Whether `DataTable` can carry a row menu for non-table rows was not checked.
- The lane message says a finding on `hasDetail` of a deleted subject was answered; no such finding exists on the PR. The lane's list of reviews omits `0eeedb39`. The `pm-operator` files the lane says it touched are not in the final 17-file diff.
- Files outside the Expected Surface (each plausibly needed): `completion-report.adoc`, `web-app/README.adoc`, `view-density-and-filtering.adoc`, `client.ts`, `MockRuntimeEngine.java`, `pageWidth.test.ts`, `mobile.spec.js`, `showroom.spec.js`, `theme-readability.spec.js`.

## Defects (at `96286321`)

1. **V** MED-LOW `ReportsView.svelte:164-178`: the overflow measurement runs on `items`, `query` and `activeDetailPlanId` only and there is no resize observer: after a breakpoint change a body that starts to wrap has no "show the text" control and its text stays hidden. Untested.
2. **V** MED-LOW `ReportsView.svelte:496-499`: `markAllShownRead` posts `items.map(...)` (every loaded item), not `matchingItems`: with a filter active it marks the hidden rows read and empties the list. The label says "shown".
3. LOW `ReportsView.svelte:887`: `<div slot="filters">` is always rendered, so `TriagePage`'s `$$slots.filters` is true and an empty filter container is drawn.
4. LOW two `<h1>` on screen when the detail is open beside the list (`ReportsView.svelte:904` and `h1.report-title`); not checked against the template's heading rules.
5. LOW a11y: the show/hide-the-text button has the same name on every row; the row menu has no label.

Test gaps: the control-only-where-text-hides branch (f5c01d3) cannot fail in jsdom (`scrollHeight` 0 keeps the control; the browser assertion was declined); the focus move to the successor row or heading after mark-read and the `isConnected` fallback are untested; the `'\u0000machine'` sentinel is untested against a project named `__machine__`; the "marks a single report read from its row menu" spec reads `initialCount` after only the row is visible, while its sibling waits for "6 unread reports" (the scenario announces a report and its read late): a possible flake (lead, not run).

## Leads (not verified)

- Whether `DataTable` can supply a row menu for the card layout.
- The lane's `mutation-after` log (`pm-web-e2e/maven-2026-11-02-120258.log`: 1 failed / 333 passed) and its build-gate figures were not opened.

## Routing

Defects 1 to 5, the test gaps, the flake lead and the `DataTable` question: PLAN-99 (`web-reports-residue`, staged 2026-11-05).

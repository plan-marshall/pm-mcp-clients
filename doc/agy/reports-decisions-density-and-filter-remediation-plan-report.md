# Completion Report: `reports-decisions-density-and-filter` Split Retain Race

**Plan**: [`implemented/reports-decisions-density-and-filter-remediation-plan.md`](implemented/reports-decisions-density-and-filter-remediation-plan.md)
**Branch**: `fix/reports-split-retain-race`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-reports-split-retain-race`
**Date**: 2026-10-24
**Step 8 status**: remediation complete — awaiting the orchestrator's post-landing re-verification.

---

## 1. Executive Summary

The split reports layout no longer loses its active card when the runtime answers a mark-read request faster than the HTTP response. `ReportsView.markRead` now records the retained-read marker **before** issuing `apiClient.markReportRead`, so a `report.read` event that overtakes the response still finds the row retained; a failed request deletes the marker and reassigns the record, so the row returns to unread.

The regression is reproduced by a component test that holds the mark-read promise open, emits `report.read` for the id while the request is pending, and asserts the card and its read state survive both the event and the resolution. The test fails against the previous ordering and passes against the fix. The live split Playwright case, the reported Step 8 failure, now passes against the mock runtime.

No API, record, server, layout, filter, or field-matrix change was made.

---

## 2. Delivered Artifacts

| File | Change |
|---|---|
| `pm-web-app/src/lib/views/ReportsView.svelte` | `markRead` sets `retainedReadIds[item.id]` before `await apiClient.markReportRead`; the catch path deletes the marker and reassigns the record; the non-retain removal is unchanged; the JSDoc records the ordering invariant |
| `pm-web-app/src/lib/views/ReportsView.test.ts` | New: `fixRelayCompletion()` fixture helper (replaces three inline duplicates); the event-before-resolution test; the failed-read rollback test |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanViewScenarioTest.java` | Baseline OpenRewrite reformat emitted by the pre-commit gate (one indentation line; committed as the gate built it) |
| `doc/agy/reports-decisions-density-and-filter-remediation-plan.md` | Recipe and workflow checkboxes checked; moved to `doc/agy/implemented/` |
| `doc/agy/reports-decisions-density-and-filter-plan-report.md` | Step 8 section records the remediation and its live evidence |
| `doc/agy/implemented/README.md`, `doc/agy/README.md` | Archive and dashboard rows for the remediation plan and its report |

---

## 3. Test & Verification Results

| Check | Result |
|---|---|
| `npm test -- ReportsView` (pm-web-app) | **24 tests passed** (23 before; 2 added) |
| Full `npm test` (pm-web-app) | **548 tests, 60 files, all passed** |
| `npm run check` (`svelte-check`) | **0 errors, 0 warnings** |
| `npm run lint` (ESLint) | **clean** |
| `npx playwright test tests/reports.spec.js` (live mock runtime) | **10 passed**, including _keeps the list beside the detail in split mode and remembers the layout_ |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** (8:16 min); `git status` shows only the intended files plus the gate's OpenRewrite reformat |

### Discriminating evidence

The new ordering test was run against the pre-fix `ReportsView.svelte` (source stashed, test kept): it fails at the read-state assertion, because the event filters the row out. With the fix restored it passes. The test therefore guards the race rather than merely describing the fixed code.

The failure-path test drives a rejected `markReportRead`, asserts the error banner, the still-present active card, the absence of the read marker, and an unchanged unread count of 3.

---

## 4. Domain Invariants

- **No API, record, or server change**: the diff touches one view, its test file, and documentation.
- **No layout, filter, or field-matrix change**: `viewLayout`, `filterText`, `FilterInput`, and the field matrices are untouched.
- **Retained-read lifecycle preserved**: `flushRetainedReads` still drops retained rows when the detail closes; non-retain reads still remove their row.
- **Reactivity**: the failure path reassigns `retainedReadIds` after `delete`, so Svelte 5 observes the change.
- **Bilingual catalogue**: no new user-visible string.
- **Accessibility**: `aria-current="true"` on the active card and the read-state label are unchanged and still asserted.

### Adjacent observation (pre-existing, not changed by this plan)

Live probing of the split flow shows the local unread pill drops by two for one opened report (7 → 5): the `report.read` event decrements it in `handleEvent`, then `markRead` decrements it again on success. This double-count predates the remediation and is outside this plan's stated scope ("this plan targets that race only"); it is recorded here for a future count-reconciliation plan rather than fixed here.

---

## 5. Review Window

Pending PR creation; results to be recorded here after the 300-second review window and the 100% findings-resolution bar.

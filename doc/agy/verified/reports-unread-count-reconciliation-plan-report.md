# Completion Report: `reports-unread-count-reconciliation`

**Plan**: [`doc/agy/implemented/reports-unread-count-reconciliation-plan.md`](../implemented/reports-unread-count-reconciliation-plan.md)
**Branch**: `fix/reports-unread-count`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-reports-unread-count`
**Date**: 2026-10-24
**Step 8 status**: **PASSED** — the count is reconciled once per read; the report is ready for `doc/agy/verified/`.

---

## 1. Executive Summary

One read now decrements the unread pill exactly once. The reported live flow — seven unread reports, one opened, pill at five — lands on six.

`ReportsView` counts each read once per report id: whichever of the runtime's `report.read` event and the mark-read HTTP response arrives first records the id in `countedReadIds` and subtracts, and the other finds the marker and leaves the count alone. The marker outlives the row it belongs to, so a late event cannot count a read a settled response already counted, and a read of an older report that this view has not loaded still moves the pill.

The fix is confined to `ReportsView.svelte`, its component test, and the live split Playwright case. No server, API, record, layout, filter, or catalogue change.

---

## 2. Delivered Artifacts

| File | Change |
|---|---|
| `pm-web-app/src/lib/views/ReportsView.svelte` | `countedReadIds` records every read the view has counted; `handleEvent('report.read')` counts only ids not yet recorded, whether or not the row is on the loaded page; `markRead` records its own accounting instead of consuming a marker, so a later event is a no-op; `pendingReadIds` covers every in-flight read, and `loadReports` drops only the markers of settled reads; `markAllShownRead` clears them |
| `pm-web-app/src/lib/views/ReportsView.test.ts` | +7 tests: event-before-response, response-before-event, no-event, replayed event, reload-then-event, read of a report outside the loaded page, and a reload that overtakes a pending read; the retained failure path keeps its existing test |
| `pm-web-e2e/tests/reports.spec.js` | The split case now asserts the live pill: `7 unread reports` before the read and `6 unread reports` after it |
| `doc/agy/reports-decisions-density-and-filter-plan-report.md` | §10 records the count reconciliation as delivered and points Step 8 at the follow-up |
| `doc/agy/reports-unread-count-reconciliation-plan.md` | Recipe and workflow checkboxes checked; moved to `doc/agy/implemented/` |

---

## 3. Reconciliation Rule

The unread count is reconciled per report id, not per message: a read is counted by the first of the event and the response, and the id stays recorded so the other never counts it again.

| Ordering | Event | Response | Decrements |
|---|---|---|---|
| Event first | The id is not recorded → count it, record it | The id is recorded → the response adds nothing | 1 |
| Response first | The id is recorded → the late event counts nothing | The id is not recorded → count it, record it | 1 |
| No event | — | The response counts it | 1 |
| Failure | — | The catch path restores the retained row and leaves the count alone | 0 |
| Replayed event | The id is recorded → no decrement | — | 0 |
| Read outside the loaded page | The id is not recorded and need not be listed → count it | — | 1 |

The event ids are taken **before** the read event filters the list, because a row that leaves in the same handler is still one the operator had not read yet.

---

## 4. Test & Verification Results

| Check | Result |
|---|---|
| `npm test -- ReportsView` | **33 tests passed** (26 before, 7 added) |
| Full `npm test` (pm-web-app) | **557 tests, 60 files, all passed** |
| `npm run check` (`svelte-check`) | **0 errors, 0 warnings** |
| `npm run lint` (ESLint) | **clean** |
| `npx playwright test tests/reports.spec.js` | **10 passed**, including the split case with the new pill assertion |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS**; clean `git status` after the gate |

### Non-vacuity evidence

- Reverting `ReportsView.svelte` to the revision before this plan fails **3 of the 5** original tests: event-before-response, response-before-event, and the replayed event. The two that still pass on the old code are the ones that must hold in both revisions — a read with no event, and a read event after a reload — so they guard the fix against over-correcting rather than reproducing the defect.
- The two review-fix tests fail against the intermediate revision that shipped the first cut: `counts a read of a report outside the loaded page` and `keeps a pending read counted when a reload lands before its response`.
- The live Playwright assertion reproduces the reported number against the pre-fix revision: **`Received string: "5 unread reports"`** where the fixed tree shows `6 unread reports`.

---

## 5. Domain Invariants

- **No API, record, or server change**: the diff touches one view, its test, one Playwright case, and documentation. `pm-api` and `pm-mock-runtime` are untouched.
- **No layout, filter, or field-matrix change**: `viewLayout`, `filterText`, `FilterInput`, and the published matrices are untouched.
- **No new user-visible string**: the pill, the read state, and the catalogue are unchanged; EN and DE stay in step by construction.
- **Retained-read lifecycle preserved**: the retained marker is still recorded before the request and rolled back on failure; closing the detail still flushes settled retained rows and keeps pending ones.
- **Server reconciliation kept**: `refreshReportsCount()` still runs after a successful read and after a reload, so the header badge follows the server.
- **Reactivity**: `countedReadIds` and `pendingReadIds` are read only by view logic, never by the template, so they stay plain `Set`s rather than `$state` records.

---

## 6. Risk & Rollback

- **Event replay**: a second `report.read` for a recorded id counts nothing, before or after the response.
- **Reload**: the reloaded server count already includes every read the view counted, so their markers are dropped; a read whose response is still in flight keeps its marker, so the response cannot subtract it a second time.
- **Batch reads**: `report_ids` are de-duplicated and only unrecorded ids count, so a batch that names an already-counted report cannot over-decrement.
- **Rollback**: restore the unconditional `unreadCount - 1` in `markRead` and the `unreadCount - ids.length` in `handleEvent`; the double count returns.

---

## 7. Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS` and left `git status` clean: OpenRewrite, license headers, Checkstyle, the Enforcer rules, Javadoc, and the reactor unit tests all passed on the committed tree.

---

## 8. Review Window

- **CI**: the build gate, config, Quarkus alignment, both JDK builds, integration tests, dependency review, and `build / conclusion` all pass. The first run's `sonar-build` job failed after its analysis report uploaded (`Failed to get Quality Gate status - Organization is not allowed to access data from non main branches`, a SonarCloud organization condition rather than a finding); the re-run on the rebased commit passed.
- **SonarCloud**: **Quality Gate passed — 0 new issues, 0 accepted issues, 0 security hotspots**.
- **Sourcery**: skipped — the organization's 250,000-character weekly review budget is exhausted (reset in 1 day 8 hours); no findings were emitted.
- **CodeRabbit** (two actionable findings, both fixed in the review-fix commit):
  1. _Reads outside the loaded page were dropped_ — `isStillUnread` rejected ids absent from `items`, so a read of an older report announced by the runtime left the pill stale. The count is now keyed on `countedReadIds` alone, and a test covers an id outside the loaded page.
  2. _A reload could un-count a pending read_ — `loadReports` cleared every marker, so a response that settled after the reload subtracted a read the fresh server count already included. `loadReports` now drops only settled markers and keeps the markers of reads still in flight, and a test holds a read open across a reload.
- **Inline threads**: both comments answered in place.

---

## 9. Step 8 Verification (Orchestrator)

**Verdict: PASSED.** The report moves to `doc/agy/verified/` and Step 8 is checked off in the plan.

| Pillar | Result | Evidence |
|---|---|---|
| **1 — Traceability** | Pass | One read decrements once in event-first, response-first, no-event, replayed-event, outside-page, and reload-pending orderings. The retained and non-retained paths are both covered; failure leaves the count unchanged. |
| **2 — Anti-patterns** | Pass | No API, record, server, layout, filter, field-matrix, or catalogue change; no new user-facing string; `countedReadIds`/`pendingReadIds` stay view logic; `refreshReportsCount()` remains the server reconciliation. |
| **3 — Subprocess lifecycle** | Pass | `reports.spec.js` is 10 passed, including the live split pill 7 → 6; the full pre-commit gate was green. No new subprocess path. |
| **4 — Independent diff** | Pass | One view, its test, one Playwright assertion, and records; no test weakening; no unrelated revert; commit trailer present. |

The adjacent parent reports/decisions plan is re-verified with this report and the split retain-race report; both remediations are now complete.

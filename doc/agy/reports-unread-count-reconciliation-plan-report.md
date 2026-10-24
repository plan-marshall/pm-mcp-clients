# Completion Report: `reports-unread-count-reconciliation`

**Plan**: [`doc/agy/implemented/reports-unread-count-reconciliation-plan.md`](implemented/reports-unread-count-reconciliation-plan.md)
**Branch**: `fix/reports-unread-count`
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-reports-unread-count`
**Date**: 2026-10-24
**Step 8 status**: Pending — the report moves to `doc/agy/verified/` only after the orchestrator's post-landing verification.

---

## 1. Executive Summary

One read now decrements the unread pill exactly once. The reported live flow — seven unread reports, one opened, pill at five — lands on six.

`ReportsView` has a single owner for the decrement: a report id is counted by whichever of the runtime's `report.read` event and the mark-read HTTP response arrives first, and the other is a no-op. `eventReadIds` records the ids the event has already accounted for, `markRead` consumes that marker instead of decrementing again, and a read event only counts an id the view still shows as unread.

The fix is confined to `ReportsView.svelte`, its component test, and the live split Playwright case. No server, API, record, layout, filter, or catalogue change.

---

## 2. Delivered Artifacts

| File | Change |
|---|---|
| `pm-web-app/src/lib/views/ReportsView.svelte` | `eventReadIds` tracking set; `isStillUnread(id)` decides whether a read event still owes a decrement; `handleEvent('report.read')` counts only the ids that were unread before the list drops them; `markRead` decrements only when the event has not already counted the id; `loadReports`, `markAllShownRead`, and `dropRetainedRow` clear the tracked ids |
| `pm-web-app/src/lib/views/ReportsView.test.ts` | +5 tests: event-before-response, response-before-event, no-event, replayed event, and reload-then-event; the retained failure path keeps its existing test |
| `pm-web-e2e/tests/reports.spec.js` | The split case now asserts the live pill: `7 unread reports` before the read and `6 unread reports` after it |
| `doc/agy/reports-decisions-density-and-filter-plan-report.md` | §10 records the count reconciliation as delivered and points Step 8 at the follow-up |
| `doc/agy/reports-unread-count-reconciliation-plan.md` | Recipe and workflow checkboxes checked; moved to `doc/agy/implemented/` |

---

## 3. Reconciliation Rule

The unread count is reconciled per report id, not per message.

| Ordering | Event | Response | Decrements |
|---|---|---|---|
| Event first | `isStillUnread(id)` is true (the row is listed; a retained row is still pending) → count it, record the id | `eventReadIds.delete(id)` returns `true` → `alreadyCounted` → no second decrement | 1 |
| Response first | The response decrements; a retained row stays listed but is no longer pending, and a dropped row is gone | `isStillUnread(id)` is false → the late event counts nothing | 1 |
| No event | — | The response decrements | 1 |
| Failure | — | The catch path restores the retained row and leaves the count alone | 0 |
| Replayed event | `eventReadIds` already holds the id → no decrement | — | 0 |

`isStillUnread` reads the list **before** the read event filters it, because a row that leaves in the same handler is still one the operator had not read yet.

---

## 4. Test & Verification Results

| Check | Result |
|---|---|
| `npm test -- ReportsView` | **31 tests passed** (26 before, 5 added) |
| Full `npm test` (pm-web-app) | **555 tests, 60 files, all passed** |
| `npm run check` (`svelte-check`) | **0 errors, 0 warnings** |
| `npm run lint` (ESLint) | **clean** |
| `npx playwright test tests/reports.spec.js` | **10 passed**, including the split case with the new pill assertion |
| `./mvnw verify -Ppre-commit` | **BUILD SUCCESS**; clean `git status` after the gate |

### Non-vacuity evidence

- Reverting `ReportsView.svelte` to the previous revision fails **3 of the 5** new component tests: event-before-response, response-before-event, and the replayed event. The two that still pass on the old code are the ones that must hold in both revisions — a read with no event, and a read event after a reload — so they guard the fix against over-correcting rather than reproducing the defect.
- The live Playwright assertion reproduces the reported number against the previous revision: **`Received string: "5 unread reports"`** where the fixed tree shows `6 unread reports`.

---

## 5. Domain Invariants

- **No API, record, or server change**: the diff touches one view, its test, one Playwright case, and documentation. `pm-api` and `pm-mock-runtime` are untouched.
- **No layout, filter, or field-matrix change**: `viewLayout`, `filterText`, `FilterInput`, and the published matrices are untouched.
- **No new user-visible string**: the pill, the read state, and the catalogue are unchanged; EN and DE stay in step by construction.
- **Retained-read lifecycle preserved**: the marker is still recorded before the request and rolled back on failure; closing the detail still flushes settled retained rows and keeps pending ones.
- **Server reconciliation kept**: `refreshReportsCount()` still runs after a successful read and after a reload, so the header badge follows the server.
- **Reactivity**: `eventReadIds` is read only by view logic, never by the template, so it is a plain `Set` beside `pendingReadIds` rather than a `$state` record.

---

## 6. Risk & Rollback

- **Event replay**: a second `report.read` for an already-counted id finds it in `eventReadIds` and counts nothing.
- **Reload**: `loadReports` clears the tracked ids together with the list it replaces, so a later event for a re-listed report counts again.
- **Batch reads**: `report_ids` are de-duplicated and only the ids still unread count, so a batch that names an already-read report cannot over-decrement.
- **Rollback**: restore the unconditional `unreadCount - 1` in `markRead` and the `unreadCount - ids.length` in `handleEvent`; the double count returns.

---

## 7. Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS` and left `git status` clean: OpenRewrite, license headers, Checkstyle, the Enforcer rules, Javadoc, and the reactor unit tests all passed on the committed tree.

---

## 8. Review Window

_To be recorded after the PR review cycle._

---

## 9. Step 8 Verification (Orchestrator)

Pending. The orchestrator re-runs the four-pillar verification for this plan together with the still-open parent report, then moves this report to `doc/agy/verified/`.

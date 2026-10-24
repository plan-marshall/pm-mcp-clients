# Remediation Plan: `reports-unread-count-reconciliation` (One Decrement per Read)

The split-view retain race is fixed and verified, but the same live flow exposed a second, pre-existing defect: the local unread pill drops by two for one opened report. The `report.read` event decrements `unreadCount` in `handleEvent`, then `markRead` decrements it again on success. Opening one of seven unread reports leaves the pill at five.

This plan makes one read produce exactly one decrement, in both the event-first and the response-first orderings. It is a records-and-code plan; it does not change the server, the report payloads, the layouts, or the filter.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Parent plan** | [`reports-decisions-density-and-filter-plan.md`](reports-decisions-density-and-filter-plan.md) — Step 8 still failed on this count |
| **Retain-race report** | [`../verified/reports-decisions-density-and-filter-remediation-plan-report.md`](../verified/reports-decisions-density-and-filter-remediation-plan-report.md) — §4 records the double decrement |
| **View** | [`ReportsView.svelte`](../../../pm-web-app/src/lib/views/ReportsView.svelte) — `handleEvent`, `markRead`, `markAllShownRead`, `unreadCount` |
| **Tests** | [`ReportsView.test.ts`](../../../pm-web-app/src/lib/views/ReportsView.test.ts), [`reports.spec.js`](../../../pm-web-e2e/tests/reports.spec.js) |
| **Use case** | UC-PLN-6 (catch up on what happened) |
| **Operational SOP** | [`runbook.md`](../runbook.md) |

### 1.1 Root cause

`handleEvent('report.read')` subtracts `ids.length` from `unreadCount`. `markRead` also subtracts one after `apiClient.markReportRead` resolves. When the runtime emits the read event for the same report, one read is counted twice. The existing tests assert unread counts for filtering and mark-all, but not the event-plus-response ordering.

---

## 2. Scope

1. Make `unreadCount` reconciliation single-owner:
   - track the ids whose read event has already been applied;
   - in `markRead`, decrement only when the event has not already accounted for that id;
   - clear the tracking when the id leaves the loaded set or the view reloads.
2. Apply the same rule to the split retained path and the non-retained path.
3. Add component tests:
   - event before response → one decrement;
   - response before event → one decrement;
   - no event → one decrement;
   - failure → no decrement (and the row restored for the retained path).
4. Keep `refreshReportsCount()` as the server reconciliation after the local update.
5. No layout, filter, API, record, server, or mock-scenario change.

### Non-Goals

- No redesign of the unread badge or its catalogue text.
- No change to `markAllReportsRead` semantics.
- No change to the report list ordering or the split retain behavior.

---

## 3. Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-reports-unread-count` | `fix/reports-unread-count` | `ReportsView.svelte`, `ReportsView.test.ts`, the parent report |

**No touch**: `DecisionView`, `OverviewView`, `EpicView`, `filterText`, `FilterInput`, `viewLayout`, `pm-api`, `pm-mock-runtime`, and the server.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-reports-unread-count` on branch `fix/reports-unread-count` off latest `main`.
- [x] 2. Personas loaded: `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript`; `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing`; `plan-marshall-persona-integration-tester`.
- [x] 3. Implement and test §5.
- [x] 4. Report written; plan moved to `implemented/`; dashboard updated.
- [x] 5. `npm test -- ReportsView`, `npm run check`, `npm run lint`, `npx playwright test tests/reports.spec.js`, and `./mvnw verify -Ppre-commit` pass.
- [x] 6. PR opened without `skip-bot-review`; findings resolved (PR #203; CodeRabbit's two findings fixed and both threads resolved).
- [x] 7. Landed on `main` as PR #203; worktree and branch cleaned up.
- [ ] 8. Post-landing verification and report archival by the orchestrator.

---

## 5. Technical Recipe

- [x] **5.1** Add an `eventReadIds = new Set<string>()` beside the other report state.
- [x] **5.2** In `handleEvent('report.read')`, after filtering, record each applied id in `eventReadIds` and decrement by the count that was actually still unread.
- [x] **5.3** In `markRead` success:
  ```ts
  const alreadyCounted = eventReadIds.delete(item.id);
  if (!alreadyCounted) unreadCount = Math.max(0, unreadCount - 1);
  ```
  Keep the retained/row-removal behavior unchanged.
- [x] **5.4** On `loadReports`, `markAllShownRead`, and when an id leaves both the list and the retained set, clear the matching `eventReadIds` entries.
- [x] **5.5** Tests: the four orderings in §2, each asserting the exact unread count and the visible pill.
- [x] **5.6** The live split Playwright case remains green; optionally assert the pill after one read.

---

## 6. Acceptance Criteria

| Criterion | Evidence |
|---|---|
| One read decrements the unread pill exactly once, whether the event or the response arrives first. | Component tests §5.5. |
| A failed read decrements nothing and leaves the row unread. | Failure-path test. |
| The split active card and retained-row behavior are unchanged. | Existing split tests and Playwright. |
| No server/API/record/layout/filter change. | Diff review. |

---

## 7. Risk & Rollback

- **Event replay**: a duplicate `report.read` for an already-applied id must not decrement again; the set makes the second event a no-op.
- **Reload**: `loadReports` replaces the list and the server count; clear the tracked ids so a later event is applied once.
- **Rollback**: restore the unconditional decrement in `markRead`; the double count returns.

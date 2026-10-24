# Remediation Plan: `reports-decisions-density-and-filter` (Retain the Split Active Card Across the Read Event)

The reports/decisions density plan passed the static pillars but failed Step 8 on one live case: opening a report in `split` mode can lose the active card. This plan targets that race only. It does not revisit the layouts, the filter, the field matrices, or the other views.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Failed plan** | [`reports-decisions-density-and-filter-plan.md`](reports-decisions-density-and-filter-plan.md) |
| **Completion report** | [`reports-decisions-density-and-filter-plan-report.md`](../reports-decisions-density-and-filter-plan-report.md) — Step 8 FAILED |
| **Evidence** | `pm-web-e2e/tests/reports.spec.js` › _keeps the list beside the detail in split mode and remembers the layout_: `.report-card--active` not found |
| **View** | [`ReportsView.svelte`](../../../pm-web-app/src/lib/views/ReportsView.svelte) — `markRead`, `handleEvent`, `retainedReadIds`, `reportCard` |
| **Test surfaces** | [`ReportsView.test.ts`](../../../pm-web-app/src/lib/views/ReportsView.test.ts), [`reports.spec.js`](../../../pm-web-e2e/tests/reports.spec.js) |
| **Operational SOP** | [`runbook.md`](../runbook.md) |

### 1.1 Root cause

`markRead(item, retain)` sets `retainedReadIds[item.id] = true` **after** awaiting `apiClient.markReportRead(item.id)`. The runtime can emit `report.read` before that promise resolves. `handleEvent` then filters the item out because `retainedReadIds[item.id]` is not set yet. In `split` mode the detail remains, but the list loses the row and `.report-card--active` disappears.

The unit test mocks `markReportRead` without emitting the event, so it cannot see the ordering. The Playwright split case opens the real mock runtime and fails.

---

## 2. Scope

1. In `markRead(item, retain)`:
   - when `retain` is true, set `retainedReadIds[item.id] = true` **before** the request;
   - on failure, remove the marker and reassign the record so the row returns to unread;
   - keep the existing removal for `retain === false`.
2. Add a component test where `markReportRead` triggers the `report.read` event before resolving; assert the active card remains, then resolve and assert it still remains.
3. Keep the existing split Playwright case as the acceptance test; it must go green against the live mock.
4. No API, record, server, layout, filter, or field-matrix change.

### Non-Goals

- No redesign of the split layout or the retained-read behavior.
- No change to `report.read` payloads or the mock runtime.
- No new layout/filter work.

---

## 3. Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-reports-split-retain-race` | `fix/reports-split-retain-race` | `ReportsView.svelte`, `ReportsView.test.ts`, report update |

**No touch**: `DecisionView`, `OverviewView`, `EpicView`, `filterText`, `FilterInput`, `viewLayout`, `pm-api`, `pm-mock-runtime`, and the server.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-reports-split-retain-race` on branch `fix/reports-split-retain-race` off latest `main`.
- [ ] 2. Personas loaded: `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript`; `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing`; `plan-marshall-persona-integration-tester`; `plan-marshall-persona-code-reviewer`.
- [x] 3. Fix and tests per §5.
- [x] 4. Completion report updated; plan moved to `implemented/`; dashboard updated.
- [x] 5. `./mvnw verify -Ppre-commit` and the reports Playwright spec pass.
- [ ] 6. PR opened without `skip-bot-review`; review findings resolved.
- [ ] 7. Landed on `main`; worktree and branch cleaned up.
- [ ] 8. Post-landing verification and report archival by the orchestrator.

---

## 5. Technical Recipe

- [x] **5.1 Set the retain marker before the request**
  ```ts
  async function markRead(item: ReportItem, retain = false) {
    if (retain) retainedReadIds[item.id] = true;
    try {
      await apiClient.markReportRead(item.id);
      if (!retain) items = items.filter((i) => i.id !== item.id);
      unreadCount = Math.max(0, unreadCount - 1);
      void refreshReportsCount();
    } catch (err: unknown) {
      if (retain) {
        delete retainedReadIds[item.id];
        retainedReadIds = { ...retainedReadIds };
      }
      errorMessage = (err as Error)?.message || $t('report.list.mark_read_failed');
    }
  }
  ```
- [x] **5.2 Component test**
  - Mock `markReportRead` with a pending promise.
  - Open split, click the detail, emit `report.read` for the id **before** resolving.
  - Assert `.report-card--active` remains; resolve; assert it still remains.
- [x] **5.3 Live acceptance**
  - `npx playwright test tests/reports.spec.js` must pass all cases, especially the split case.
- [x] **5.4 Verification**
  - `npm test -- ReportsView`, `npm run check`, `npm run lint`;
  - `./mvnw verify -Ppre-commit`.
- [ ] **5.5 Records**
  - Mark Step 8 in the original report and the remediation plan; move both to the verified archive only after the live gate passes.

---

## 6. Acceptance Criteria

| Criterion | Evidence |
|---|---|
| The active report card remains while the read event arrives before the request resolves. | Component test §5.2. |
| The split Playwright case passes against the live mock. | `reports.spec.js` green. |
| A failed read restores the row as unread. | Catch-path assertion in the component test. |
| No other view or contract changes. | Diff review. |

---

## 7. Risk & Rollback

- **Before-await marker**: if the request fails, the marker must be removed or the row disappears on the next event. The catch path and its test are the guard.
- **Reactivity**: reassign `retainedReadIds` after delete so Svelte 5 sees the change.
- **Rollback**: restore the after-await assignment; the split case then reproduces.

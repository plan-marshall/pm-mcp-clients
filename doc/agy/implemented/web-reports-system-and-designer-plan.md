# Implementation Plan: `web-reports-system-and-designer` (Reports, System View, Cleanup & Process Designer in the Web App)

This implementation plan governs the remediation of `web-reports` (`doc/plans/implemented/web-reports.adoc`), `web-system` (`doc/plans/implemented/web-system.adoc`), and `web-designer` (`doc/plans/implemented/web-designer.adoc`) in `pm-web-app` and `pm-web-e2e`.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-6`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Reports & Completion Report), [`UC-SYS-1..3`](../../../plan-marshall-documentation/doc/use-cases/07-system.adoc) (System & Cleanup), [`UC-DSG-1..5`](../../../plan-marshall-documentation/doc/use-cases/08-designer.adoc) (Process Designer) |
| **Specifications** | [`report-contract.adoc`](../plans/implemented/report-contract.adoc), [`system-contract.adoc`](../plans/implemented/system-contract.adoc), [`designer-contract.adoc`](../plans/implemented/designer-contract.adoc) |
| **Local Client Design** | [`doc/design/shared/reports.adoc`](../design/shared/reports.adoc), [`doc/design/shared/completion-report.adoc`](../design/shared/completion-report.adoc), [`doc/design/shared/system-view.adoc`](../design/shared/system-view.adoc), [`doc/design/shared/process-designer.adoc`](../design/shared/process-designer.adoc), [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/web-reports.adoc`](../plans/implemented/web-reports.adoc), [`doc/plans/implemented/web-system.adoc`](../plans/implemented/web-system.adoc), [`doc/plans/implemented/web-designer.adoc`](../plans/implemented/web-designer.adoc) |
| **Prerequisite Plans** | [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md), [`doc/agy/mock-runtime-reports-system-designer-plan.md`](mock-runtime-reports-system-designer-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Complete Reports View & Polite Live Notice (`ReportsView.svelte`, `CompletionReportView.svelte`)**:
   - Render a polite live-region (`aria-live="polite"`) coalesced quiet notice when new reports arrive over SSE (`report.arrived`), keeping `document.title` unchanged.
   - Render each segment of a report's `from` provenance (`project`, `epic`, `plan`) as an independent link that navigates to that project, epic, or plan **without** marking the report as read (*"with from (each part a link that reads nothing)"*).
   - Support opening detail (`openDetail(report)`, which marks the report read) across all report subject kinds:
     - `plan_completed` / `plan`: opens `CompletionReportView.svelte`.
     - `epic`: navigates to `/epics/[id]`.
     - `project`: navigates to `/projects` with that project selected.
     - `start`: navigates to `/start` preloaded at its sources.
     - `cleanup`: navigates to `/system/cleanup` highlighting the cleanup job result.
     - When `report.subject_deleted` is true (deleted by cleanup), render the detail action disabled with the catalogue explanation (`report.detail.deleted_by_cleanup`).
2. **Complete System View, Upgrade Workflow & Cleanup Read-Only Inspection (`SystemView.svelte`, `CleanupView.svelte`)**:
   - Persist `selectedWindow`, `selectedGroupBy`, and `selectedProject` in `localStorage` (`pm_system_filters`) and restore on mount.
   - Turn each figure in the `Work` summary (`running plans`, `epics`, `projects with running work`) into a link that navigates to `/overview` or `/projects` filtered to what it counts.
   - Implement the complete **Runtime Upgrade** state machine in `SystemView.svelte`:
     1. Upgrade available notice with target version and release notes summary.
     2. Pre-upgrade check list (running workers, active builds, dirty worktrees) and **Consent** screen (`system.upgrade.consent`).
     3. In-progress upgrade status with **Withdraw** action (`POST /api/v1/system/upgrade/withdraw`) while waiting for active tasks to drain.
     4. Upgrade failure state with cause and retry/dismiss actions.
     5. Post-upgrade reload handler that checks whether any input/textarea in the app holds unsent text (`hasUnsentInputStore`), prompting or waiting before calling `window.location.reload()`.
   - In `CleanupView.svelte`, store selected candidate IDs in `runtimeStore` so opening any cleanup candidate's read-only preview and returning to `/system/cleanup` preserves all checked items.
3. **Complete Process Designer (`ProcessDesigner.svelte`, `BlockPalette.svelte`, `WorkflowValidationResults.svelte`)**:
   - **Intake & Draft Persistence**: support opening a runtime unit, starting from a template, uploading a local file in the browser (`<input type="file">`), or pasting text; persist the active draft in `localStorage` (`pm_designer_draft`), restore on reload, and prompt for confirmation before leaving when dirty.
   - **Side-by-Side Editor & Bidirectional Sync**: render line numbers in the text pane; clicking a line highlights the corresponding block/step in the structure tree, and clicking a node in the structure tree scrolls/selects the corresponding line range in the text pane; clicking a validation finding jumps to its line.
   - **Frontmatter Panel**: dedicated Frontmatter editor kept in two-way sync with the document header; automatically updates `modified` date and `release` on edit, drops any cryptographic `signature` when content changes, prompts for `author` once per browser (`localStorage.getItem('pm_designer_author')`), and locks `interface` read-only when replacing an existing unit.
   - **Placing, Diff, Landing Progress & Phone Read-Only Mode**:
     - Before placing (`POST /api/v1/designer/place`), display a side-by-side diff (desktop width) and the list of affected projects.
     - Stream or poll landing progress lines per affected project, with a **Retry** button on failed landings and a **Conflict comparison** view when the target unit changed concurrently.
     - At phone width (`< 640px`), switch the editor to read-only inspection mode while keeping **Validate** enabled.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md) and [`doc/agy/mock-runtime-reports-system-designer-plan.md`](mock-runtime-reports-system-designer-plan.md).
- **Parallelizable with**: `web-onboarding-entry-and-projects-plan.md`, `web-overview-plan-and-epic-view-plan.md`, `web-start-plan.md`, `web-decisions-and-actions-plan.md`, and all `cli-*` plans.

### Boundary Rules
- **Sole Owner of**:
  - `pm-web-app/src/lib/views/ReportsView.svelte`
  - `pm-web-app/src/lib/views/CompletionReportView.svelte`
  - `pm-web-app/src/lib/views/SystemView.svelte`
  - `pm-web-app/src/lib/views/CleanupView.svelte`
  - `pm-web-app/src/lib/views/ProcessDesigner.svelte`
  - `pm-web-app/src/lib/components/BlockPalette.svelte`
  - `pm-web-app/src/lib/components/WorkflowValidationResults.svelte`
  - `pm-web-e2e/tests/reports.spec.js`, `system.spec.js`, `designer.spec.js`
- **No Touch**: `routes.ts` / `AppRouter.svelte` (already wired by `web-foundation-routing-and-catalogue-plan.md`), `pm-operator`, and `pm-mock-runtime` scenario files.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-reports-system-designer` on branch `feat/web-reports-system-and-designer` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `pm-dev-frontend-javascript` for `ReportsView`, `SystemView`, `CleanupView`, and `ProcessDesigner`.
  - `pm-dev-frontend-css` for side-by-side diff, line-numbered editor, and phone read-only responsive layout.
  - `persona-module-tester` & `pm-dev-frontend-jest-testing` for Vitest component tests and Playwright E2E tests.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/web-reports-system-and-designer-plan-report.md`) and initial plan moved to `doc/agy/implemented/web-reports-system-and-designer-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-reports-system-and-designer-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Reports & Live Notice** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Polite live region, `from` provenance links (without marking read), and all report detail targets (including deleted-by-cleanup state). |
| **Phase 2: System, Upgrade & Cleanup** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | `localStorage` filters, clickable `Work` counts, full upgrade flow + unsent-text reload guard, read-only cleanup preview keeping checkboxes. |
| **Phase 3: Process Designer** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-frontend-css` | File upload/paste, `localStorage` draft + leave guard, line numbers + bidirectional sync, Frontmatter panel, side-by-side diff, landing progress, phone read-only mode. |
| **Phase 4: Unit & E2E Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Expand `ReportsView.test.ts`, `SystemView.test.ts`, `ProcessDesigner.test.ts`, `reports.spec.js`, `system.spec.js`, and `designer.spec.js`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation, `catalogue.test.ts` check, and review bot triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Complete `ReportsView.svelte` & Polite Live Notice
- [x] **1.1 Polite Live-Region Notice & `from` Links**:
  - Render an `aria-live="polite"` coalesced report arrival notice without changing `document.title`.
  - Render `project`, `epic`, and `plan` in `report.from` as independent links that navigate without marking the report read.
- [x] **1.2 All Report Detail Targets**:
  - Support opening detail for `plan`, `epic`, `project`, `start`, and `cleanup` reports, and disable the button with `report.detail.deleted_by_cleanup` when `subject_deleted` is true.

### Phase 2: Complete `SystemView.svelte` & `CleanupView.svelte`
- [x] **2.1 Persisted System Filters, Clickable `Work` Figures & Upgrade Flow**:
  - Persist `(selectedWindow, selectedGroupBy, selectedProject)` in `localStorage`.
  - Make `Work` figures clickable links to `/overview` or `/projects`.
  - Implement upgrade consent, pre-upgrade checklist, withdraw, failure states, and unsent-input reload guard.
- [x] **2.2 Cleanup Read-Only Item Inspection**:
  - Allow opening a cleanup item read-only from `CleanupView.svelte` and returning with all checkbox selections intact.

### Phase 3: Complete `ProcessDesigner.svelte`
- [x] **3.1 File Upload, Draft Persistence & Frontmatter Panel**:
  - Add browser file upload and paste intake, `localStorage` draft persistence + unsaved-changes leave confirmation, and the two-way synced Frontmatter panel (auto-setting modification date & release, dropping signature, remembering author per device, read-only interface on replace).
- [x] **3.2 Bidirectional Structure Sync, Side-by-Side Diff & Phone Mode**:
  - Add line numbers and two-way selection sync between text and structure tree, desktop side-by-side placement diff, affected projects list, landing progress/retry/conflict views, and `< 640px` phone read-only mode with `Validate`.

### Phase 4: Verification
- [x] **4.1 Unit & E2E Tests**:
  - Run `cd pm-web-app && npm test`, `cd pm-web-e2e && npx playwright test tests/reports.spec.js tests/system.spec.js tests/designer.spec.js`, and `./mvnw verify -Ppre-commit`.

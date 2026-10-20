# Implementation Plan: `web-overview-plan-and-epic-view` (Running Work Overview, Caller-Aware Breadcrumbs & Token Breakdown)

This implementation plan governs the remediation of `web-overview` (`doc/plans/implemented/web-overview.adoc`) and `web-plan-view` (`doc/plans/implemented/web-plan-view.adoc`) in `pm-web-app` and `pm-web-e2e`: implementing caller-aware breadcrumb navigation and one-step project/epic links, one-step-away token breakdown by phase and role with raw components, ended-plan completion report navigation, and the browser start-point preference toggle (`Running Work` vs `Project Explorer`).

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-1`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1), [`UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4), [`UC-PLN-5`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5) |
| **Specifications** | [`overview-contract.adoc`](../plans/implemented/overview-contract.adoc), [`process-contract.adoc`](../plans/implemented/process-contract.adoc) |
| **Local Client Design** | [`doc/design/shared/plan-overview.adoc`](../design/shared/plan-overview.adoc), [`doc/design/shared/plan-view.adoc`](../design/shared/plan-view.adoc), [`doc/design/shared/epic-view.adoc`](../design/shared/epic-view.adoc), [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/web-overview.adoc`](../plans/implemented/web-overview.adoc), [`doc/plans/implemented/web-plan-view.adoc`](../plans/implemented/web-plan-view.adoc) |
| **Prerequisite Plans** | [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md), [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Caller-Aware Breadcrumb Path & One-Step Project/Epic Links (`PlanView.svelte`, `EpicView.svelte`, `OverviewView.svelte`)**:
   - Using the navigation history stack from `web-foundation-routing-and-catalogue`, render the caller breadcrumb trail in `PlanView.svelte` and `EpicView.svelte` so clicking `Back` returns to the exact view from which the plan/epic was opened (**Running Work**, **Project Explorer**, **Epic View**, **Decision View**, **Ended Work**, or **Reports**).
   - Clicking the **Project** link in `PlanView.svelte` or `EpicView.svelte` navigates in one step to that project's detail in `ProjectExplorerView.svelte` (`/projects?project={id}`).
   - Clicking the **Parent Epic** link in `PlanView.svelte` navigates in one step to `EpicView.svelte` (`/epics/{epic_id}`).
2. **One-Step-Away Token Breakdown by Phase and Role with Raw Components (`PlanView.svelte`)**:
   - In the **Tokens** section of `PlanView.svelte`, show the total tokens, `≥` floor indicator when partial, and the catalogue-formatted count of unmeasured tasks (`plan.tokens.unmeasured_tasks`).
   - Provide a one-step-away disclosure toggle (`Show breakdown by phase and role` / `Hide breakdown`) that reveals:
     - **By Phase** table: each phase with its raw `input`, `cached`, `output`, and `total` tokens.
     - **By Role** table: each role with its raw `input`, `cached`, `output`, and `total` tokens.
3. **Ended Plan View & Completion Report Navigation (`PlanView.svelte`, `OverviewView.svelte`)**:
   - In `PlanView.svelte`, clearly display ended plan status (`ended: <outcome>`, ended timestamp) and provide a one-step **Open Completion Report** button (`plan.ended.open_report`) that navigates to the plan's completion report (`/reports?plan={id}`).
   - In `OverviewView.svelte`, allow opening either the ended plan's `PlanView` or its `CompletionReportView` from the **Ended Work** section, and preserve filter selections across navigation.
4. **Browser Start-Point Preference Toggle (`OverviewView.svelte`)**:
   - Add a header toggle in `OverviewView.svelte` (`Set as browser start page` / `Default start page`, persisted in `localStorage.getItem('pm_start_route')`), honoured by `landingRoute()` on initial load.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md) and [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md).
- **Depended on by**: [`doc/agy/web-decisions-and-actions-plan.md`](web-decisions-and-actions-plan.md) (which mounts `ControlActionsBar`, `HoldResolutionModal`, `PlanSettingsModal`, `FindingsReviewModal`, and `PlanningChatPanel` into `OverviewView`, `PlanView`, and `EpicView` after this plan lands).
- **Parallelizable with**: `web-onboarding-entry-and-projects-plan.md`, `web-start-plan.md`, `web-reports-system-and-designer-plan.md`, and all `cli-*` plans.

### Boundary Rules
- **Sole Owner of**:
  - `pm-web-app/src/lib/views/OverviewView.svelte`
  - `pm-web-app/src/lib/views/PlanView.svelte`
  - `pm-web-app/src/lib/views/EpicView.svelte`
  - `pm-web-app/src/lib/views/OverviewView.test.ts`, `PlanView.test.ts`
  - `pm-web-e2e/tests/overview.spec.js`, `plan-view.spec.js`
- **No Touch**: `pm-operator` and `pm-mock-runtime` scenario files.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-overview-plan-view` on branch `feat/web-overview-plan-and-epic-view` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `pm-dev-frontend-javascript` for `OverviewView`, `PlanView`, and `EpicView`.
  - `pm-dev-frontend-css` for breadcrumb, token breakdown table, and ended-plan banner styling.
  - `persona-module-tester` & `pm-dev-frontend-jest-testing` for Vitest component tests and Playwright E2E tests.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/web-overview-plan-and-epic-view-plan-report.md`) and initial plan moved to `doc/agy/implemented/web-overview-plan-and-epic-view-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-overview-plan-and-epic-view-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Caller Breadcrumbs & Links** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Wire caller-aware back navigation and one-step project/epic links in `PlanView` and `EpicView`. |
| **Phase 2: Token Breakdown & Ended Plans** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-frontend-css` | One-step-away token breakdown by phase/role with raw components (`input`, `cached`, `output`), ended-plan completion report button. |
| **Phase 3: Overview Polish & Start Point** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Browser start-point preference toggle and preserved overview filters. |
| **Phase 4: Unit & E2E Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Expand `OverviewView.test.ts`, `PlanView.test.ts`, `overview.spec.js`, and `plan-view.spec.js`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation, `catalogue.test.ts` check, and review bot triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Caller-Aware Breadcrumb Navigation & One-Step Project/Epic Links
- [ ] **1.1 Update `PlanView.svelte` & `EpicView.svelte` Breadcrumbs**:
  - Replace hardcoded `navigateTo('overview')` with `goBack()` using the caller route from `runtimeStore`.
  - Wire the project breadcrumb link to open `/projects?project={project_id}` and the parent epic link to open `/epics/{epic_id}`.

### Phase 2: One-Step-Away Token Breakdown & Ended Plan Completion Report Link
- [ ] **2.1 Expandable Token Breakdown with Raw Components**:
  - Add a one-step disclosure in `PlanView.svelte` showing `by_phase` and `by_role` tables with raw `input`, `cached`, `output`, and `total` columns, plus the unmeasured task notice.
- [ ] **2.2 Ended Plan Completion Report Action**:
  - On ended plans in `PlanView.svelte` and `OverviewView.svelte`, add the action to open the plan's Completion Report (`/reports?plan={id}`).

### Phase 3: Browser Start-Point Preference & Verification
- [ ] **3.1 Start-Point Toggle & Filter Persistence**:
  - Add the `pm_start_route` start-page toggle in `OverviewView.svelte` and persist overview filters across route transitions.
- [ ] **3.2 Unit & E2E Tests**:
  - Run `cd pm-web-app && npm test`, `cd pm-web-e2e && npx playwright test tests/overview.spec.js tests/plan-view.spec.js`, and `./mvnw verify -Ppre-commit`.

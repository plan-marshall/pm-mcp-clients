# Implementation Plan: `web-onboarding-entry-and-projects` (10-Step Project Onboarding, Favourites Ordering & Settings Conflict Resolution)

This implementation plan governs the remediation of `web-onboarding` (`doc/plans/implemented/web-onboarding.adoc`) and `web-projects` (`doc/plans/implemented/web-projects.adoc`) in `pm-web-app` and `pm-web-e2e`: implementing the missing 10-step Project Onboarding flow (including terminal-captured build environment display, conflict resolution, and setup question chat), favourite project reordering, project settings conflict resolution, and direct aspect jumping from Machine Settings.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-ONB-1..10`](../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc), [`UC-ENT-1..2`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc), [`UC-PRJ-1..4`](../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc) |
| **Specifications** | [`onboarding-contract.adoc`](../plans/implemented/onboarding-contract.adoc), [`entry-contract.adoc`](../plans/implemented/entry-contract.adoc), [`project-contract.adoc`](../plans/implemented/project-contract.adoc) |
| **Local Client Design** | [`doc/design/shared/language-and-onboarding.adoc`](../design/shared/language-and-onboarding.adoc), [`doc/design/shared/entry.adoc`](../design/shared/entry.adoc), [`doc/design/shared/project-onboarding.adoc`](../design/shared/project-onboarding.adoc), [`doc/design/shared/project-explorer.adoc`](../design/shared/project-explorer.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/web-onboarding.adoc`](../plans/implemented/web-onboarding.adoc), [`doc/plans/implemented/web-projects.adoc`](../plans/implemented/web-projects.adoc) |
| **Prerequisite Plans** | [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md), [`doc/agy/mock-runtime-onboarding-and-entry-plan.md`](mock-runtime-onboarding-and-entry-plan.md), [`doc/agy/mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **10-Step Project Onboarding Flow (`ProjectOnboardingView.svelte`)**:
   - Create `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` (rendered at `/projects/onboard` mounted by `web-foundation-routing-and-catalogue`):
     - Renders the 10-step project onboarding progress bar and step screens driven by the runtime's project onboarding state (`GET /api/v1/projects/{id}/onboarding`, `POST /api/v1/projects/{id}/onboarding/step`):
       1. Project identity & repository root
       2. Build system & modules (displaying the **build environment last captured by the terminal client**, or a notice when not yet captured)
       3. Skill domains & profiles
       4. Quality & verification gates
       5. Architecture & boundary checks
       6. Credentials & providers for the project
       7. Review bots & CI integration
       8. Branching & worktree policy
       9. Cleanup & retention defaults
       10. Summary & completion
     - Handles concurrent value conflict (`conflict`) during project onboarding with a conflict card (`current_value` vs `wanted_value`, `Keep` / `Apply again`).
     - Integrates a setup question chat drawer on any onboarding step (or displays the disabled state with its catalogue reason when the required planning role is not configured).
2. **Favourite Projects Ordering (`ProjectExplorerView.svelte`)**:
   - In the `Favourites` tab and section of `ProjectExplorerView.svelte`, render move-up / move-down controls and drag-and-drop reordering for favourite projects.
   - Persist the updated favourite order via `PUT /api/v1/projects/favourites/order`.
3. **Project Settings Conflict Resolution (`ProjectExplorerView.svelte`)**:
   - Pass the current revision (`expected_revision`) when saving project settings in the detail drawer.
   - When the runtime responds with `conflict: true`, display a conflict resolution banner showing the other client's `current_value` and this browser's `wanted_value`, with actions to **Keep current value** or **Apply my change again**.
4. **Machine Settings Direct Aspect Jump & Settings Back Navigation (`MachineSettingsView.svelte`, `SettingsView.svelte`, `OnboardingView.svelte`)**:
   - In `MachineSettingsView.svelte`, clicking `Change` on an aspect (`aspect.step`) sets the selected step in `runtimeStore` and navigates to `/onboarding?step={aspect.step}`, opening `OnboardingView.svelte` directly at that aspect.
   - In `SettingsView.svelte`, clicking `Back` pops the navigation history stack (`goBack()`) instead of navigating to `'workspace'`.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md), [`doc/agy/mock-runtime-onboarding-and-entry-plan.md`](mock-runtime-onboarding-and-entry-plan.md), and [`doc/agy/mock-runtime-projects-and-start-plan.md`](mock-runtime-projects-and-start-plan.md).
- **Depended on by**: [`doc/agy/web-start-plan.md`](web-start-plan.md).
- **Parallelizable with**: `web-overview-plan-and-epic-view-plan.md`, `web-reports-system-and-designer-plan.md`, and all `cli-*` plans.

### Boundary Rules
- **Sole Owner of**:
  - `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` (new)
  - `pm-web-app/src/lib/views/ProjectExplorerView.svelte`
  - `pm-web-app/src/lib/views/MachineSettingsView.svelte`
  - `pm-web-app/src/lib/views/SettingsView.svelte`
  - `pm-web-app/src/lib/views/OnboardingView.svelte`
  - `pm-web-e2e/tests/projects.spec.js`, `onboarding.spec.js`, `devices.spec.js`, `entry.spec.js`
- **No Touch**: `routes.ts` / `AppRouter.svelte` (already wired by `web-foundation-routing-and-catalogue-plan.md`), `pm-operator`, and `pm-mock-runtime` scenario files.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-onboarding-projects` on branch `feat/web-onboarding-entry-and-projects` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `pm-dev-frontend-javascript` for `ProjectOnboardingView.svelte` and `ProjectExplorerView.svelte`.
  - `pm-dev-frontend-css` for responsive layout and step bar styling.
  - `persona-module-tester` & `pm-dev-frontend-jest-testing` for Vitest component tests and Playwright E2E tests.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/web-onboarding-entry-and-projects-plan-report.md`) and initial plan moved to `doc/agy/implemented/web-onboarding-entry-and-projects-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-onboarding-entry-and-projects-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: 10-Step Project Onboarding** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-frontend-css` | Build `ProjectOnboardingView.svelte` with terminal build-env display, conflict resolution, and setup question chat. |
| **Phase 2: Explorer Favourites & Conflicts** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Add favourite reordering and project settings conflict resolution (`Keep` vs `Apply again`) in `ProjectExplorerView.svelte`. |
| **Phase 3: Machine Settings Aspect Jump** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Wire direct aspect step navigation from `MachineSettingsView` to `OnboardingView` and `goBack()` in `SettingsView`. |
| **Phase 4: Unit & E2E Tests** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Add `ProjectOnboardingView.test.ts`, expand `ProjectExplorerView.test.ts`, and update `projects.spec.js` & `onboarding.spec.js`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation, `catalogue.test.ts` check, and review bot triage. |

---

## 6. Phased Technical Recipe

### Phase 1: 10-Step Project Onboarding View (`ProjectOnboardingView.svelte`)
- [ ] **1.1 Build `ProjectOnboardingView.svelte`**:
  - Render the 10 project onboarding steps with `StepBar.svelte`, displaying the terminal-captured build environment on step 2, handling concurrent `conflict` responses (`Keep current` / `Apply again`), and providing the setup question chat drawer (or disabled reason when the role is unconfigured).
  - Connect "Add Project" and "Continue setup" in `ProjectExplorerView.svelte` to `/projects/onboard`.

### Phase 2: Favourites Reordering & Settings Conflict in `ProjectExplorerView.svelte`
- [ ] **2.1 Favourites Reordering**:
  - Add up/down reorder buttons and drag-and-drop ordering for starred projects in `ProjectExplorerView.svelte`, persisting via `apiClient.reorderFavouriteProjects(orderedIds)`.
- [ ] **2.2 Project Settings Conflict Dialogue**:
  - Handle `conflict: true` in `saveProjectSettings()` by presenting `current_value` vs `wanted_value` with **Keep current** and **Apply again** buttons.

### Phase 3: Machine Settings Direct Aspect Selection & Back Navigation
- [ ] **3.1 Direct Aspect Jump**:
  - Update `MachineSettingsView.svelte` and `OnboardingView.svelte` so clicking `Change` on an aspect opens `OnboardingView` directly at that aspect's step.
  - Update `SettingsView.svelte` `Back` button to call `goBack()`.

### Phase 4: Verification
- [ ] **4.1 Unit & E2E Tests**:
  - Run `cd pm-web-app && npm test`, `cd pm-web-e2e && npx playwright test tests/projects.spec.js tests/onboarding.spec.js tests/devices.spec.js tests/entry.spec.js`, and `./mvnw verify -Ppre-commit`.

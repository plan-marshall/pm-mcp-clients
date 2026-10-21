# Implementation Plan: `web-decisions-and-actions` (Decisions, Plan & Epic Control Actions, Hold Resolution & Review in the Web App)

This implementation plan governs the remediation of `web-decisions` (`doc/plans/implemented/web-decisions.adoc`) and the execution of the `web-actions` work package (`doc/plans/implemented/web-actions.adoc`, epic clients, `done`) in `pm-web-app` and `pm-web-e2e`, consuming the shared `ChatPanel.svelte` built by the in-flight [`doc/agy/web-start-plan.md`](web-start-plan.md).

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-2`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-2) (Decisions), [`UC-PLN-5`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-5) (Epic View), [`UC-CTL-1` to `UC-CTL-5`](../../../plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) |
| **Specifications** | [`decision-contract.adoc`](../plans/implemented/decision-contract.adoc), [`control-contract.adoc`](../plans/implemented/control-contract.adoc), [`state-catalogue.adoc#chat`](../../../pm-mcp-core/doc/specification/state-catalogue.adoc) |
| **Local Client Design** | [`doc/design/shared/decisions.adoc`](../design/shared/decisions.adoc), [`doc/design/shared/work-actions.adoc`](../design/shared/work-actions.adoc), [`doc/design/shared/epic-view.adoc`](../design/shared/epic-view.adoc), [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/web-decisions.adoc`](../plans/implemented/web-decisions.adoc), [`doc/plans/implemented/web-actions.adoc`](../plans/implemented/web-actions.adoc) (`done`) |
| **Prerequisite Plans** | [`doc/agy/implemented/web-start-plan.md`](implemented/web-start-plan.md) (landed, PR #117; provides `ChatPanel.svelte`), [`doc/agy/implemented/web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md) (landed, PR #126), [`doc/agy/implemented/web-overview-plan-and-epic-view-plan.md`](implemented/web-overview-plan-and-epic-view-plan.md) (landed, PR #134), [`doc/agy/implemented/web-arrival-banners-and-i18n-cache-plan.md`](implemented/web-arrival-banners-and-i18n-cache-plan.md) (landed, PR #142), [`doc/agy/implemented/mock-runtime-running-work-plan.md`](implemented/mock-runtime-running-work-plan.md) (landed, PR #136) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Complete `DecisionView.svelte` & Browser Notifications (`web-decisions.adoc`)**:
   - Track pending decision count in `runtimeStore` and prefix `document.title` with `({count}) {title}` (`$t('decision.notify.tab_title', { count, title })`) while unread decisions await the operator.
   - Render a one-time browser notification permission banner (`decision.notify.ask`, `allow`, `deny`) when the first decision is raised, and dispatch desktop `Notification` alerts on `decision.created` while the tab is in the background.
   - Hide high-stakes recommendations behind a one-step reveal button (`decision.recommendation.on_request` / `decision.recommendation.show`).
   - Complete multi-part decision dependencies (`depends_on`, automatic `reopened` state when an earlier part's answer changes) and live `answered_elsewhere` / `revised` banners.
   - Wire **"Chat about it first"** in `DecisionView.svelte` to open `ChatPanel.svelte` (from `web-start-plan.md`) beside the decision, supporting proposed answers taken through read-back, decision withdrawal by revision, and chat closure when answered elsewhere.
2. **Mount & Complete Plan and Epic Control Actions (`web-actions.adoc`)**:
   - Mount the previously orphaned `ControlActionsBar.svelte`, `HoldResolutionModal.svelte`, and `PlanSettingsModal.svelte` into `PlanView.svelte`, `EpicView.svelte`, and `OverviewView.svelte`.
   - Enhance `ControlActionsBar.svelte`:
     - **Pause dialogue**: select pause point (*after the current task* vs *after the current phase*), or change the pause point while already pausing.
     - **End / Abort confirmation dialogue**: show what happens to the branch/worktree (`cancel`, `abandon`, or `abort` on an integrity pause).
     - **Epic controls**: raise scope limit (`raise_limit` or leave it), toggle `Automatic` execution mode (`auto` on/off, showing rows waiting for an overlap), run archive readiness check (`archive` with check lines), open decomposition (`decompose`), and cancel epic.
   - Enhance `PlanSettingsModal.svelte`:
     - Handle concurrent value changes (`conflict: true`) with the `Keep current value` vs `Apply my change again` conflict dialogue (`UC-CTL-5`).
   - Enhance `HoldResolutionModal.svelte`:
     - Display what holds a plan, an epic, or an alongside landing, with the runtime-offered resolution acts; after an act completes, either resume or show what still holds (`UC-CTL-4`).
3. **Complete Epic View Sections (`EpicView.svelte`, `web-actions.adoc` & `UC-PLN-5`)**:
   - Expand `EpicView.svelte` to show workstreams and member plans with status, stored sources, landings with PR links and landing analysis, findings and watches counts, per-plan + epic-own token breakdown (with `≥` floor and unmeasured task count, never money), and the full live event list.
4. **Findings & Watches Review Screen & Ended-Work Follow-Up (`FindingsReviewModal.svelte`, `web-actions.adoc`)**:
   - Create `pm-web-app/src/lib/components/FindingsReviewModal.svelte` to review findings and watches (keep as finding or watch, drop, edit drafted plan with read-back, commit, or open `ChatPanel.svelte`).
   - Dock `ChatPanel.svelte` beside a plan, an epic, or a review (applying accepted revisions at the next task boundary).
   - On ended plans and epics, offer read-only/follow-up chat via `ChatPanel.svelte` where accepting a follow-up plan opens `/start` with the description stored.

---

## 3. Concurrency & Isolation

- **Prerequisites Satisfied**:
  - [`doc/agy/implemented/web-start-plan.md`](implemented/web-start-plan.md) (provides `ChatPanel.svelte`)
  - [`doc/agy/implemented/web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md)
  - [`doc/agy/implemented/web-overview-plan-and-epic-view-plan.md`](implemented/web-overview-plan-and-epic-view-plan.md)
  - [`doc/agy/implemented/web-arrival-banners-and-i18n-cache-plan.md`](implemented/web-arrival-banners-and-i18n-cache-plan.md)
  - [`doc/agy/implemented/mock-runtime-running-work-plan.md`](implemented/mock-runtime-running-work-plan.md)
- **Parallelizable with**: `mock-runtime-projects-and-start-plan.md` and `cli-fullscreen-actions-plan.md`.

### Boundary Rules
- **Sole Owner of**:
  - `pm-web-app/src/lib/components/FindingsReviewModal.svelte` (new)
  - `pm-web-app/src/lib/components/ControlActionsBar.svelte`
  - `pm-web-app/src/lib/components/HoldResolutionModal.svelte`
  - `pm-web-app/src/lib/components/PlanSettingsModal.svelte`
  - `pm-web-app/src/lib/views/DecisionView.svelte`
  - Mounting control/chat/review components in `PlanView.svelte`, `EpicView.svelte`, and `OverviewView.svelte`
  - `pm-web-e2e/tests/decisions.spec.js`, `control.spec.js`
  - Moving `doc/plans/web-actions.adoc` to `doc/plans/implemented/web-actions.adoc` and setting `web-actions` to `done` in `doc/plans/README.adoc`.
- **No Touch**: `pm-operator` and `pm-mock-runtime` scenario files.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-decisions-actions` on branch `feat/web-decisions-actions` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `pm-dev-frontend-javascript` for `DecisionView`, `ControlActionsBar`, `HoldResolutionModal`, `PlanSettingsModal`, `FindingsReviewModal`, and `EpicView`.
  - `pm-dev-frontend-css` for modal dialogues, docked chat panel layout, and responsive styling.
  - `persona-module-tester` & `pm-dev-frontend-jest-testing` for Vitest unit tests and Playwright E2E tests.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/web-decisions-and-actions-plan-report.md`), initial plan moved to `doc/agy/implemented/web-decisions-and-actions-plan.md`, `doc/plans/web-actions.adoc` moved to `doc/plans/implemented/web-actions.adoc`, and `web-actions` marked `done` in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit -pl pm-web-app,pm-web-e2e -am`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-decisions-and-actions-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Decisions & Notifications** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Browser notification permission & dispatch, tab-title count badge, one-step recommendation reveal, multi-part reopen, decision chat. |
| **Phase 2: Plan & Epic Controls & Holds** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-frontend-css` | Mount `ControlActionsBar`, `HoldResolutionModal`, and `PlanSettingsModal`; add pause-point, abort, conflict, and epic action dialogues. |
| **Phase 3: Epic View, Review & Chat** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Complete `EpicView.svelte` sections, build `FindingsReviewModal.svelte`, wire running & ended-work chat follow-up to `/start`. |
| **Phase 4: Unit & E2E Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Expand `DecisionView.test.ts`, `ControlActionsBar.test.ts`, `decisions.spec.js`, and `control.spec.js`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation, `catalogue.test.ts` check, and review bot triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Complete `DecisionView.svelte` & Browser Notifications
- [x] **1.1 Tab Title Count & Browser Notifications**:
  - Prefix `document.title` with `({count})` when pending decisions exist, prompt once for browser `Notification` permission, and fire desktop notifications on `decision.created`.
- [x] **1.2 Recommendation Reveal, Multi-Part Reopen & Decision Chat**:
  - Hide high-stakes recommendations behind one click (`decision.recommendation.show`), handle multi-part `depends_on` / `reopened` and `answered_elsewhere` / `revised` states, and dock `ChatPanel.svelte` when clicking *"Chat about it first"*.

### Phase 2: Mount & Complete Control Actions, Settings Conflict & Hold Resolution
- [x] **2.1 Mount `ControlActionsBar`, `HoldResolutionModal`, and `PlanSettingsModal`**:
  - Mount the three components in `PlanView.svelte`, `EpicView.svelte`, and `OverviewView.svelte`.
  - Add the pause-point selector (`after_task` vs `after_phase`), end/abort confirmation with branch/worktree disposition, settings `conflict` resolution (`Keep` vs `Apply again`), and epic controls (`raise_limit`, `auto` toggle, `archive` check, `decompose`, `cancel`).

### Phase 3: Epic View Sections, Findings Review & Ended-Work Chat
- [x] **3.1 Complete `EpicView.svelte` & `FindingsReviewModal.svelte`**:
  - Render workstreams, sources, landings, findings/watches, per-plan + epic-own tokens, and events in `EpicView.svelte`.
  - Build `FindingsReviewModal.svelte` for keeping/dropping findings and watches, editing drafted plans with read-back, and chatting.
  - Wire ended-work chat and follow-up plan transition to `/start`.

### Phase 4: Verification & Package Completion
- [x] **4.1 Unit & E2E Tests**:
  - Run `cd pm-web-app && npm test`, `cd pm-web-e2e && npx playwright test tests/decisions.spec.js tests/control.spec.js`, and `./mvnw verify -Ppre-commit`.
  - Move `doc/plans/web-actions.adoc` to `doc/plans/implemented/web-actions.adoc` and update `doc/plans/README.adoc`.

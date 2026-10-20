# Implementation Plan: `web-start` (Starting Work in the Web App)

This implementation plan governs the execution of the `web-start` work package (`doc/plans/implemented/web-start.adoc`, epic clients) in `pm-web-app`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the start workflows for plans, epics, and recipes in `pm-web-app`, including dynamic source management, confined file picking, epic adoption and decomposition review, and the reusable planning chat panel, verified through view tests against `pm-mock-runtime`:

1. **Start Flow Routing & Screens**:
   - Start route views (`/start`, `/projects/{id}/start`, `/entries/{id}`) rendering the states reported by the runtime.
   - Dedicated screens for plan start, epic start, and recipe execution.
   - Resumption of open, unexpired entries (`start.continue`).
2. **Dynamic Source Management & Validation**:
   - Progressive source status indicators (`fetching`, `screening`, `ready`, `failed`).
   - Markdown description editor with preview.
   - Confined file picker consuming `/api/v1/projects/{project_id}/files`.
   - Document copy vs. move selector, displayed only while `sources.documents_mode` is `ask` (`copy` preselected).
   - Issue/PR input with forge detection, grant dialog, rate limit reset display (`resets_at`), flagged span highlighting, and unscreened source acceptance prompt.
   - PR changed files list with untrusted markers.
   - Enforcement of source-kind combination rules and withholding local changes under `landing.files`.
3. **Ready Step & Recipes**:
   - Plan and epic ready step showing summary, title, and target parameters.
   - Recipe execution screen: read-only derived plan ID (`recipe-{key}-{timestamp}`), editable title, and custom parameter inputs.
   - Epics: display project's `epic.auto_start` in force with origin; never prompt for parallelization scope.
4. **Epic Adoption & Decomposition Review**:
   - Epic adoption selection (`start-epic.adopt`) and failure screen (`start-epic.adopt-failed`).
   - Decomposition review interface: structured row display, verification checks, instruction input, and proposal read-back.
5. **Reusable Chat Panel**:
   - Reusable chat panel component used across all chat interactions (start chat, plan chat, epic chat): transcript stream, message input, assistant turns, and proposal cards with accept/reject buttons.
   - Start chat side-panel beside source list once stored and screened; stores accepted description via `start.store-description`.
   - Clean termination of chat before creating work or abandoning entry.
6. **Responsive Layout & Theme Compliance**:
   - Flawless rendering at desktop (>= 1024px) and phone (< 768px) viewport widths.
   - Complete light and dark mode styling conforming to `web-app-foundation` CSS variables.
7. **Internationalization & Testing**:
   - 100% of UI strings loaded from message catalogues (`messages/start.{en,de}.properties`, `messages/operator.{en,de}.properties`). Zero literal UI strings.
   - Automated component and view tests covering every scenario of `start-contract` against `pm-mock-runtime`.

### Target Work Package & References
- Specification: `doc/plans/implemented/web-start.adoc`.
- Use Cases: `UC-STA-1` through `UC-STA-9`, State Catalogue § Chatting with the Planning Role.
- Specifications: `start-contract.adoc`, `control-contract.adoc` (decomposition & chat).
- Design: `doc/design/shared/starting-work.adoc`, `starting-work-texts.adoc`, `starting-work-sources-texts.adoc`, `web-design`.
- Primary Module: `pm-web-app`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 10 alongside:
- `cli-start` (`pm-operator:src/**`)
- `intent-views` (`pm-api:de.planmarshall.api.intent.*`, `pm-mock-runtime:scenarios/intent-*.json`, `doc/design/shared/intent-and-outline.adoc`)

### Boundary Rules
- `pm-web-app`: Sole owner of web start views, source components, file picker UI, chat panel, and view tests.
- `pm-operator`: No touch (owned by `cli-start`).
- `pm-api`: Consumer of contract records from `de.planmarshall.api.start.*`, `de.planmarshall.api.control.*`.
- `pm-mock-runtime`: Read-only consumer of `start-*.json` scenarios.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-start` on branch `feat/web-start`.
- [x] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `pm-dev-frontend-javascript` for Svelte components, state store, and file picker.
  - `pm-dev-frontend-css` for responsive layout, chat panel side-sheet, and light/dark theme styling.
  - `pm-dev-frontend-jest-testing` & `persona-module-tester` for view test suites against mock runtime.
  - `persona-code-reviewer` for review bot resolution.
- [x] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/web-start-plan-report.md`), initial plan moved to `doc/agy/implemented/web-start-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` or `npm test`).
- [x] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-start-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Routing & Source Components** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Mount start routes, build progressive source list, description editor, and file picker. |
| **Phase 2: Validation, Trust & Recipes** | `plan-marshall-persona-security-expert` | `pm-dev-frontend-javascript-security` | Forge detection, grant modal, rate-limit timer, flagged span rendering, recipe inputs. |
| **Phase 3: Adoption & Decomposition** | `plan-marshall-persona-implementer` | `pm-dev-frontend-css` | Epic adoption screen, decomposition review layout, instruction read-back cards. |
| **Phase 4: Reusable Chat Panel & Tests** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Reusable chat panel, proposal accept/reject flows, automated scenario view tests. |
| **Phase 5: Quality Gate & PR Review** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation, zero literal string check, review bot finding triage. |

---

## 5. Phased Technical Recipe

### Phase 1: Start Flow Routing & Confined File Picker
- Create `/start`, `/entries/{id}` routes in `AppRouter.svelte`.
- Implement `SourceList.svelte` with reactive progress indicators and combination filtering.
- Implement `ConfinedFilePicker.svelte` querying `/api/v1/projects/{project_id}/files`.

### Phase 2: Trust Boundaries & Recipe Execution
- Implement `GrantModal.svelte` for external forge access with failure handling.
- Highlight flagged spans with plain-text tooltips; display rate-limit countdown.
- Build `RecipeReadyView.svelte` with read-only derived plan ID and dynamic parameter form.

### Phase 3: Epic Adoption & Decomposition Review Screens
- Implement `EpicAdoptView.svelte` with adoption failure fallback.
- Implement `DecompositionReviewView.svelte` displaying workstreams, rows, automated checks, and modification instructions.

### Phase 4: Reusable Planning Chat Panel & View Test Suites
- Author `ChatPanel.svelte`: collapsible side-sheet/embedded panel with transcript history, SSE streaming support, and proposal accept/reject controls.
- Connect chat panel to `/entries/{id}/events` and `start.store-description`.
- Author comprehensive view tests in `pm-web-app/src/lib/views/StartView.scenarios.test.ts` covering all `start-contract` scenarios.

### Phase 5: Verification & Review Resolution
- Run full pre-commit gate (`npm test && npm run lint`, `./mvnw verify`).
- Verify zero hardcoded English/German strings in UI components.
- Submit PR without `skip-bot-review` and resolve all bot findings.

# Implementation Plan: `web-foundation-routing-and-catalogue` (Web App Design Completion, URL Routing & Catalogue Purity)

This implementation plan governs the foundational remediation of `pm-web-app` (`doc/plans/implemented/web-design.adoc` and `doc/plans/implemented/web-app-foundation.adoc`): completing the `DRAFT` web app design document, establishing full URL routing and history across all views, retiring the temporary `WorkspaceView` landing stub, eliminating the ~780-line hardcoded `fallbackMessages` dictionary and all 184 literal/fallback violations, and enforcing `catalogue.test.ts` across all `.svelte` and `.ts` files.

It operationalizes [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](../../runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Specifications** | [`doc/plans/implemented/web-design.adoc`](../../plans/implemented/web-design.adoc), [`doc/plans/implemented/web-app-foundation.adoc`](../../plans/implemented/web-app-foundation.adoc), [`doc/plans/implemented/message-catalogue.adoc`](../../plans/implemented/message-catalogue.adoc) |
| **Local Client Design** | [`doc/design/web-app/README.adoc`](../../design/web-app/README.adoc), [`doc/design/shared/language-and-onboarding.adoc`](../../design/shared/language-and-onboarding.adoc), [`doc/design/shared/entry.adoc`](../../design/shared/entry.adoc) |
| **Prerequisite Plans** | [`doc/agy/implemented/web-start-plan.md`](web-start-plan.md) (landed), [`doc/agy/implemented/cli-start-plan.md`](cli-start-plan.md) (landed) |
| **Target Modules** | `doc/design/web-app`, `pm-api` (message bundles), `pm-web-app`, `pm-web-e2e` |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](../../runbook.md) |

---

## 2. Scope & Objectives

1. **Complete `doc/design/web-app/README.adoc` and Set `Status: FINAL` (`web-design.adoc`)**:
   - Complete `== Visual Language` in `doc/design/web-app/README.adoc`:
     - Layout grid, responsive breakpoints (`desktop >= 1024px`, `tablet 640-1023px`, `phone < 640px`), typography scale (`sans` UI vs `mono` identifiers/diffs/logs), semantic colour tokens, state and attention indicators, and light/dark theme rules.
     - Add dedicated web layout sections for **Reports View (`/reports`)**, **System View (`/system`)**, and **Cleanup View (`/system/cleanup`)** (required by `web-reports.adoc` and `web-system.adoc`).
     - Promote `:status: DRAFT` to `:status: FINAL`.
2. **Full URL Routing, Parameterized Paths & Retirement of `WorkspaceView` (`routes.ts`, `AppRouter.svelte`, `+layout.svelte`, `runtime.ts`)**:
   - Preserve and integrate the `/start`, `/projects/[id]/start`, and `/entries/[id]` routes added by `web-start-plan.md`, and map every remaining application route in `pm-web-app/src/lib/routes.ts` and `src/routes/**` to a canonical URL path:
     - `/pair`, `/entry`, `/onboarding`, `/settings`, `/settings/machine`
     - `/projects`, `/projects/onboard`
     - `/overview`
     - `/plans/[id]`, `/epics/[id]`, `/epics/decompose`
     - `/decisions`
     - `/reports`
     - `/system`, `/system/cleanup`
     - `/designer`
     - `/start`, `/entries/[id]`
   - Support parameterized path parsing (`routeForPath(pathname)`) and generation (`pathForRoute(route, params)`) plus a caller navigation stack (`pushRoute` / `goBack` in `runtime.ts`) so `syncPath` in `+layout.svelte` pushes history entries (`window.history.pushState`) and handles `popstate` (browser Back/Forward) without resetting the URL to `/`.
   - Retire `WorkspaceView.svelte` and any leftover debug source modal buttons in `+layout.svelte` (superseded by `web-start-plan.md`): update `landingRoute()` in `pm-web-app/src/lib/stores/runtime.ts` to land paired, onboarded browsers on their remembered start point (`localStorage.getItem('pm_start_route')` defaulting to `'projects'` when no work needs attention or `'overview'` when work is running/waiting).
3. **Pure Runtime Catalogue Loading & Removal of Hardcoded Text Across All Views**:
   - Move any missing web keys from `fallbackMessages` in `pm-web-app/src/lib/i18n/index.ts` into the Java resource bundles under **`pm-api/src/main/resources/messages/*.{en,de}.properties`** (served by `pm-mock-runtime` `GET /api/v1/catalogue` via `MessageCatalogue`).
   - Remove `fallbackMessages` from production `pm-web-app/src/lib/i18n/index.ts`. For Vitest unit tests that run without a live server, provide a test-only helper `pm-web-app/src/test/catalogue-fixture.ts` loaded by `src/test/setup.ts`.
   - Replace all hardcoded user-facing template/attribute strings and all `$t(...) || '...'` fallback expressions across all `.svelte` and `.ts` files in `pm-web-app/src/` (including any components landed by `web-start-plan.md`) with pure `$t('...')` lookups.
4. **Comprehensive `catalogue.test.ts` Enforcement Across All `.svelte` and `.ts` Files**:
   - Upgrade `pm-web-app/src/lib/i18n/catalogue.test.ts` to dynamically discover **every** `.svelte` file under `pm-web-app/src/` (replacing the 12-file `SCREEN_SOURCES` list).
   - Assert that:
     1. No `.svelte` file contains hardcoded user-facing text nodes or `placeholder`/`title`/`aria-label` string literals.
     2. No `.svelte` or `.ts` file uses `$t(...) || '...'` or `$t(...) ?? '...'` string fallbacks.
     3. `src/lib/i18n/index.ts` contains no embedded `fallbackMessages` dictionary.

---

## 3. Concurrency & Isolation

This plan is the **Wave 1 Web Foundation** for all subsequent `web-*` remediation plans (`web-onboarding-entry-and-projects-plan.md`, `web-overview-plan-and-epic-view-plan.md`, `web-decisions-and-actions-plan.md`, `web-reports-system-and-designer-plan.md`).

- **Must start after**: the currently in-flight [`doc/agy/web-start-plan.md`](web-start-plan.md) and [`doc/agy/cli-start-plan.md`](cli-start-plan.md) have landed on `main` (because `web-start-plan.md` edits `AppRouter.svelte`, `routes.ts`, `+layout.svelte`, and `pm-api/src/main/resources/messages/{start,operator}.{en,de}.properties`).

### Boundary Rules
- **Sole Owner of**:
  - `doc/design/web-app/README.adoc`
  - `pm-web-app/src/lib/routes.ts`, `pm-web-app/src/lib/AppRouter.svelte`, `pm-web-app/src/routes/**` (all SvelteKit route shells)
  - `pm-web-app/src/lib/i18n/index.ts`, `pm-web-app/src/lib/i18n/catalogue.test.ts`, `pm-web-app/src/test/setup.ts`, `pm-web-app/src/test/catalogue-fixture.ts`
  - Deletion/retirement of `pm-web-app/src/lib/views/WorkspaceView.svelte` and update of `pm-web-e2e/tests/workspace.spec.js`
  - Adding missing web keys to `pm-api/src/main/resources/messages/*.{en,de}.properties`
- **No Touch**: `pm-operator` and `pm-mock-runtime` scenario JSON files.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-foundation` on branch `feat/web-foundation-routing-and-catalogue` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-documenter` & `pm-documents:ref-asciidoc` for completing `doc/design/web-app/README.adoc`.
  - `persona-implementer` & `pm-dev-frontend-javascript` for URL routing, history stack, and catalogue loader cleanup.
  - `persona-module-tester` & `pm-dev-frontend-jest-testing` for `catalogue.test.ts` AST/regex static gate and route tests.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/web-foundation-routing-and-catalogue-plan-report.md`) and initial plan moved to `doc/agy/implemented/web-foundation-routing-and-catalogue-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify`; this repository defines no `pre-commit` profile, so Maven ignores `-Ppre-commit` and `verify` carries the gates).
- [x] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-foundation-routing-and-catalogue-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Web Design Completion** | `plan-marshall-persona-documenter` | `pm-documents:ref-asciidoc` | Complete `doc/design/web-app/README.adoc` (`Visual Language`, Reports & System layouts, `Status: FINAL`). |
| **Phase 2: URL Routing & History** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Map all routes in `routes.ts` & `src/routes/**`, add caller history stack in `runtime.ts`, retire `WorkspaceView`. |
| **Phase 3: Catalogue Purity** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-java:java-core` | Move web fallback keys to `pm-api/src/main/resources/messages/*.{en,de}.properties`, remove `fallbackMessages` & 184 literals/fallbacks. |
| **Phase 4: Static Gate & E2E** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Expand `catalogue.test.ts` to all 54+ `.svelte` & `.ts` files; update `workspace.spec.js`. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Full reactor pre-commit verification and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Complete `doc/design/web-app/README.adoc`
- [x] **1.1 Author Visual Language & View Layouts**:
  - Complete `== Visual Language` in `doc/design/web-app/README.adoc` covering layout grid, responsive breakpoints, typography, semantic colour variables, state/attention badges, light/dark themes, and the web layouts for Reports (`/reports`), System (`/system`), and Cleanup (`/system/cleanup`).
  - Set `:status: FINAL`.

### Phase 2: Full URL Routing, History Stack & `WorkspaceView` Retirement
- [x] **2.1 Expand `routes.ts`, `AppRouter.svelte` & `src/routes/**`**:
  - Define canonical paths and parameterized route matching (`routeForPath`, `pathForRoute`) for all views (`/pair`, `/entry`, `/onboarding`, `/settings`, `/settings/machine`, `/projects`, `/projects/onboard`, `/overview`, `/plans/[id]`, `/epics/[id]`, `/epics/decompose`, `/decisions`, `/reports`, `/system`, `/system/cleanup`, `/designer`, `/start`, `/entries/[id]`).
  - Create the missing SvelteKit route entry files under `pm-web-app/src/routes/`.
- [x] **2.2 Caller Navigation Stack & Start-Point Landing**:
  - Add `navigationStack` (`pushRoute`, `goBack`) in `pm-web-app/src/lib/stores/runtime.ts` and wire browser `pushState` / `popstate` in `+layout.svelte`.
  - Retire `WorkspaceView.svelte` and update `landingRoute()` to land paired, onboarded browsers on their remembered start point (`projects` or `overview`).

### Phase 3: Pure Runtime Catalogue Loading & Zero Hardcoded Strings
- [x] **3.1 Consolidate Keys in `pm-api` Message Bundles**:
  - Ensure every key used by `pm-web-app` exists in `pm-api/src/main/resources/messages/*.{en,de}.properties`.
  - Remove `fallbackMessages` from `pm-web-app/src/lib/i18n/index.ts` and move test-only fixture loading to `pm-web-app/src/test/catalogue-fixture.ts`.
- [x] **3.2 Replace All 184 Hardcoded Literals & `$t() ||` Fallbacks**:
  - Replace all hardcoded template text nodes, attributes, and `$t(...) || '...'` fallbacks across all `.svelte` and `.ts` files in `pm-web-app/src/` with pure `$t('...')` calls.

### Phase 4: All-File `catalogue.test.ts` Gate & Verification
- [x] **4.1 Upgrade `catalogue.test.ts`**:
  - Dynamically scan all `.svelte` and `.ts` files under `pm-web-app/src/` and fail on any hardcoded UI text literal, `$t() ||` fallback, or embedded `fallbackMessages`.
- [x] **4.2 Pre-Commit & E2E Gate**:
  - Run `cd pm-web-app && npm test && npm run build`, `cd pm-web-e2e && npx playwright test`, and `./mvnw verify -Ppre-commit`.

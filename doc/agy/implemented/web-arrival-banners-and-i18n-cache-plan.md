# Implementation Plan: `web-arrival-banners-and-i18n-cache` (Arrival Banner Removal, Resilient i18n Caching & Missing-Key Shield in the Web App)

This implementation plan governs two targeted enhancements to the web client (`pm-web-app`):
1. **Design / UI Streamlining**: Complete removal of top arrival notice banners (`unreadReportsArrivalNotice` and `newDecisionArrivalNotice`) which are redundant to header navigation badges, eliminating layout shifts and visual noise.
2. **i18n Offline Resilience & Label Caching**: Refactoring translation loading with client-side persistent caching (`localStorage` with Stale-While-Revalidate), synchronous cache hydration on boot, multi-locale fallback, and human-readable key shielding so raw translation keys are **never** displayed to the user when the server is unreachable or offline.

It operationalizes [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-PLN-4`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (Plan Overview & Frame), [`UC-PLN-6`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-6) (Reports), [`UC-PLN-2`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-2) (Decisions), [`UC-ONB-3`](../../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc#UC-ONB-3) (Languages and Message Catalogue) |
| **Specifications** | [`message-catalogue.adoc`](../../plans/implemented/message-catalogue.adoc), [`web-app-foundation.adoc`](../../plans/implemented/web-app-foundation.adoc), [`module-structure.adoc#i18n`](../../../../plan-marshall-documentation/doc/specification/module-structure.adoc#i18n) |
| **Local Client Design** | Shared: [`doc/design/shared/texts-and-languages.adoc`](../../design/shared/texts-and-languages.adoc), [`doc/design/shared/reports.adoc`](../../design/shared/reports.adoc), [`doc/design/shared/decisions.adoc`](../../design/shared/decisions.adoc)<br>Web: [`doc/design/web-app/README.adoc#frame`](../../design/web-app/README.adoc#frame) |
| **Roadmap Packages** | [`doc/plans/implemented/message-catalogue.adoc`](../../plans/implemented/message-catalogue.adoc), [`doc/plans/implemented/web-app-foundation.adoc`](../../plans/implemented/web-app-foundation.adoc), [`doc/plans/implemented/web-design.adoc`](../../plans/implemented/web-design.adoc) |
| **Prerequisite Plans** | [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](../runbook.md) |

---

## 2. Scope & Objectives

### Part A: Complete Arrival Banner Removal (Design / UI)
1. **Remove Arrival Banners from `+layout.svelte`**:
   - Delete `.reports-arrival-banner` and `.decisions-arrival-banner` HTML blocks (`unreadReportsArrivalNotice` and `newDecisionArrivalNotice`).
   - Remove corresponding CSS classes (`.reports-arrival-banner`, `.reports-arrival-btn`, `.reports-arrival-close`).
2. **Purge Arrival Notice Logic from `runtime.ts`**:
   - Remove `unreadReportsArrivalNotice` and `newDecisionArrivalNotice` fields from `RuntimeState` interface and initial state.
   - Clean up notice-setting logic in `refreshReportsCount()`, `refreshDecisionsCount()`, and live SSE event handlers.
3. **Preserve Badges & Accessibility**:
   - Header navigation buttons (`Decisions` and `Reports`) retain dynamic count badges (`$runtimeStore.openDecisionsCount` and `$runtimeStore.unreadReportsCount`).
   - Screen-reader announcements in `<div class="sr-only" aria-live="polite">` are preserved (`quietReportNotice`, `quietDecisionNotice`), ensuring zero loss of assistive feedback.

### Part B: Resilient i18n Caching & "Never Display Keys" Shield
1. **Persistent Client-Side Caching (`localStorage`)**:
   - Persist server-provided translation catalogues in browser `localStorage` keyed by locale (`pm_i18n_messages_${locale}`).
   - Include metadata (cache timestamp, locale) to support seamless cache updates and diagnostics.
2. **Synchronous Cache Hydration (Zero-FOTK)**:
   - On module load and at the immediate start of `initI18n()`, synchronously hydrate `messagesStore` from `localStorage` before any network request or DOM rendering occurs.
   - Completely eliminates Flash of Translation Keys (FOTK) on page refresh, navigation, or server downtime.
3. **Stale-While-Revalidate (SWR) Pattern**:
   - Render immediately from cached catalogues.
   - Asynchronously fetch `/api/v1/messages/{locale}/all` in the background. On HTTP 200, update both `messagesStore` and `localStorage`.
   - On network error, offline mode, or server shutdown, silently catch the error and retain the cached messages without interruption.
4. **Multi-Tier Fallback Hierarchy**:
   - *Tier 1 (Active Locale)*: Current language messages (from server or cache).
   - *Tier 2 (Fallback Locale)*: Cached English (`en`) catalogue if active locale (e.g. `de`) lacks the requested key.
   - *Tier 3 (In-Memory Historical Cache)*: Any previously resolved key across session history.
   - *Tier 4 (Human-Friendly Deserializer)*: If a key was never encountered in any catalogue, deserialize the key path into human-readable text (e.g. `web.offline.unreachable` ──▶ `Unreachable`) rather than exposing dot-separated technical keys, and log a warning in development mode.
5. **Architectural Guard Invariant**:
   - Preserve strict adherence to `pm-web-app/src/lib/i18n/catalogue.test.ts`: **no** hardcoded `fallbackMessages` dictionary in `index.ts`. All translations are sourced from server catalogues and persisted via browser storage.

---

## 3. Concurrency & Isolation

- **Depends on**: [`doc/agy/web-foundation-routing-and-catalogue-plan.md`](web-foundation-routing-and-catalogue-plan.md) (delivered).
- **Parallelizable with**: `cli-fullscreen-plan-view-plan.md`, `cli-fullscreen-actions-plan.md`, `cli-fullscreen-reports-system-plan.md`, `mock-runtime-*` plans.

### Boundary Rules
- **Sole Owner of**:
  - `pm-web-app/src/lib/i18n/index.ts`
  - `pm-web-app/src/lib/i18n/index.test.ts`
  - Arrival banner markup and CSS in `pm-web-app/src/routes/+layout.svelte`
  - Notice-setting state properties in `pm-web-app/src/lib/stores/runtime.ts`
- **No Touch**:
  - `pm-operator`, `pm-mock-runtime`, `pm-api` backend files.
  - Sub-views: `PlanView.svelte`, `EpicView.svelte`, `DecisionView.svelte`, `ReportsView.svelte`, `SystemView.svelte` (except mocking in tests).

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-banners-and-i18n-cache` on branch `feat/web-banners-and-i18n-cache` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend-javascript` for store, i18n cache engine, and Svelte layout changes.
  - `pm-dev-frontend-css` for clean removal of banner CSS and layout verification.
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend-jest-testing` for Vitest tests (caching, offline fallback, key masking).
  - `plan-marshall-persona-code-reviewer` for finding evaluation & adversarial refutation.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/web-arrival-banners-and-i18n-cache-plan-report.md`) and initial plan moved to `doc/agy/implemented/web-arrival-banners-and-i18n-cache-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-arrival-banners-and-i18n-cache-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Banner Removal** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript`<br>`pm-dev-frontend-css` | Purge arrival notices from `+layout.svelte`, remove CSS, clean up `runtime.ts` state and SSE hooks. |
| **Phase 2: i18n Cache Architecture** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Synchronous `localStorage` hydration, SWR network revalidation, atomic store & storage updates. |
| **Phase 3: Fallback & Key Shield** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Multi-locale fallback (e.g. `de` ──▶ `en`), human-readable deserializer for uncatalogued keys. |
| **Phase 4: Unit & E2E Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Unit tests for storage hydration, offline resilience, `catalogue.test.ts` guard, and Playwright verification. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit gate (`-Ppre-commit`), PR review, and 100% bot findings resolution. |

---

## 6. Phased Technical Recipe

### Phase 1: Complete Removal of Arrival Notice Banners (UI & Store Cleanup)
- [x] **1.1 Purge Banners & CSS from `+layout.svelte`**:
  - Remove `{#if $runtimeStore.unreadReportsArrivalNotice}` and `{#if $runtimeStore.newDecisionArrivalNotice}` blocks from `pm-web-app/src/routes/+layout.svelte`.
  - Remove styles `.reports-arrival-banner`, `.reports-arrival-btn`, and `.reports-arrival-close`.
  - Verify header navigation buttons (`Decisions` and `Reports`) retain count badges (`.reports-badge`, `.decisions-badge`).
  - Verify `<div class="sr-only" aria-live="polite">` retains `quietReportNotice` and `quietDecisionNotice` for screen readers.
- [x] **1.2 Clean Up `RuntimeState` & Handlers in `runtime.ts`**:
  - Remove `unreadReportsArrivalNotice` and `newDecisionArrivalNotice` from `RuntimeState` interface and `initialState`.
  - In `refreshReportsCount()` and `refreshDecisionsCount()`, remove arrival notice generation logic while keeping count updates.
  - Remove arrival notice mutations in decision and report SSE listeners.
- [x] **1.3 Clean Up Existing Test References**:
  - Update `pm-web-app/src/lib/views/ReportsView.test.ts` and `pm-web-e2e/tests/reports.spec.js` to eliminate stale banner references.

### Phase 2: Resilient Client-Side i18n Cache (`localStorage` SWR Architecture)
- [x] **2.1 Implement `localStorage` Catalogue Persistence**:
  - In `pm-web-app/src/lib/i18n/index.ts`, implement storage functions:
    - `loadCachedCatalogue(locale: Locale): Record<string, string> | null`
    - `persistCatalogue(locale: Locale, messages: Record<string, string>): void`
  - Safeguard storage calls with `try/catch` to handle disabled web storage, private mode, and quota exceptions.
- [x] **2.2 Synchronous Cache Hydration on Boot**:
  - Hydrate `messagesStore` synchronously from `loadCachedCatalogue(initialLocale)` during module initialization and at the start of `initI18n()`.
  - Ensure zero frames of unrendered or key-exposing UI on page refresh or initial mount.
- [x] **2.3 Stale-While-Revalidate (SWR) Network Updating**:
  - When `loadMessages(locale, area)` succeeds, merge/set into `messagesStore` and persist to `localStorage`.
  - When `loadMessages` fails (network error, server down, 502/504), catch the error without clearing or resetting `messagesStore`, preserving cached labels.

### Phase 3: Multi-Tier Fallback & "Never Display Keys" Shield
- [x] **3.1 Cross-Locale Fallback**:
  - If a key is not present in the active locale (e.g. `de`), check the cached default locale (`en`) before giving up.
- [x] **3.2 Key Masking / Human-Readable Deserializer**:
  - If a key cannot be found in any loaded or cached catalogue (e.g. first-ever boot while completely offline, or uncatalogued dynamic key):
    - In dev mode (`import.meta.env.DEV`), log a warning: `console.warn('[i18n] Missing translation for key:', key)`.
    - Format the key into a human-readable title/label (e.g. strip package prefixes and format leaf identifier: `web.offline.unreachable` ──▶ `Unreachable`) rather than emitting raw technical dot-separated strings.
- [x] **3.3 Verify Architectural Guard Invariant**:
  - Ensure `pm-web-app/src/lib/i18n/index.ts` contains **no** hardcoded `fallbackMessages` dictionary, remaining 100% compliant with `catalogue.test.ts`.

### Phase 4: Unit, Catalogue Guard & Integration Verification
- [x] **4.1 Unit Testing in `index.test.ts`**:
  - Add tests verifying:
    - Synchronous hydration from `localStorage` on boot.
    - SWR update: writing server responses to `localStorage`.
    - Server unreachable resilience: retaining cached messages when `apiClient.getMessages` rejects.
    - Switching locale to a previously cached locale while offline.
    - Key masking: verifying dot-separated technical keys are never rendered as raw keys.
- [x] **4.2 Catalogue Guard Check**:
  - Run `npm test src/lib/i18n/catalogue.test.ts` in `pm-web-app` to verify no literals or hardcoded dictionaries violate repository rules.
- [x] **4.3 Full Verification Gate**:
  - Run `cd pm-web-app && npm test`.
  - Run `./mvnw verify -Ppre-commit`.

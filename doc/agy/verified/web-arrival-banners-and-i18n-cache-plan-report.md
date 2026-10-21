# Completion Report: `web-arrival-banners-and-i18n-cache`

**Status:** Implemented and pre-commit verified; awaiting PR review and landing (Step 8 verification is the orchestrator's)
**Plan:** [`doc/agy/implemented/web-arrival-banners-and-i18n-cache-plan.md`](implemented/web-arrival-banners-and-i18n-cache-plan.md)
**Branch:** `feat/web-banners-and-i18n-cache`
**Governed by:** [`doc/agy/runbook.md`](runbook.md)

---

## 1. Executive Summary

The plan delivered two targeted enhancements to `pm-web-app`:

* **Arrival banners removed.** The `unreadReportsArrivalNotice` and `newDecisionArrivalNotice` banners are gone from the frame, together with their CSS and their state/handler plumbing in `runtime.ts`. The header navigation buttons keep their dynamic count badges (`unreadReportsCount`, `openDecisionsCount`), and the polite screen-reader region still announces arrivals through `quietReportNotice` and `quietDecisionNotice`, so no assistive feedback was lost.
* **Resilient i18n.** The translation layer now persists every catalogue the runtime serves in `localStorage` (keyed by locale, with a locale label and a cache timestamp), hydrates the message store synchronously at module load, on `initI18n()` and on every full catalogue load, and revalidates in the background. When the runtime is unreachable the cached catalogue stays on screen. Missing keys resolve through a four-tier chain — active locale, cached English, the session's resolved history, and a readable leaf label — so a raw dot-separated technical key can never reach the user. The `catalogue.test.ts` invariant (no embedded `fallbackMessages` dictionary) still holds.

The full reactor gate `./mvnw verify -Ppre-commit` completes with `BUILD SUCCESS`.

---

## 2. Delivered Artifacts

### 2.1 Frame and store (`pm-web-app`)

| File | Change |
|---|---|
| `src/routes/+layout.svelte` | Removed both arrival-notice markup blocks, their navigation/close handlers, and the `.reports-arrival-banner`, `.reports-arrival-btn`, and `.reports-arrival-close` styles. Header count badges (`reports-badge`, `decisions-badge`) and the `sr-only` live region are unchanged. |
| `src/lib/stores/runtime.ts` | Removed `unreadReportsArrivalNotice` and `newDecisionArrivalNotice` from `RuntimeState` and the initial state; `refreshReportsCount()` and `refreshDecisionsCount()` now update only the counts; the decision SSE handler no longer sets a banner field. Quiet notices and their catalogue keys are retained. |
| `src/lib/views/ReportsView.test.ts` | Dropped the now-unknown `unreadReportsArrivalNotice` fixture override. |
| `pm-web-e2e/tests/reports.spec.js` | Renamed the navigation test and removed the stale arrival-banner comment; the test still opens Reports from the header. |

### 2.2 Translation layer (`pm-web-app`)

| File | Change |
|---|---|
| `src/lib/i18n/index.ts` | Added `loadCachedCatalogue(locale)` and `persistCatalogue(locale, messages)` over `localStorage` (`pm_i18n_messages_${locale}`), each guarded against unavailable storage, private mode, quota failures, and corrupt entries (a corrupt entry is dropped). The stored entry is `{ locale, cachedAt, messages }`. Boot hydration runs at module import and at the start of `initI18n()`; `loadMessages()` hydrates a full catalogue before awaiting the runtime request and keeps the cache when the request rejects; `setLocale()` hydrates the target locale before revalidating. `messagesLocale` records which locale owns the store, so a late full response cannot overwrite the catalogue of a locale the person switched to, and a partial area merges into its own locale's cache rather than another locale's store. `translate()`/`t()` resolve through the active catalogue → cached English (for non-English locales) → session history → `humanizeKey()`, warning once per missing key in development. |
| `src/lib/i18n/index.test.ts` | Replaced the "renders the raw key" expectation with the readable-label contract and added twelve tests covering boot hydration, in-flight SWR rendering, persistence with metadata, unreachable-runtime resilience, offline locale switching, English fallback, key masking, the development warning, corrupt-cache healing, storage-write failure, the late-response locale race, and partial-load locale scoping. |
| `package.json` | Added `"prepare": "svelte-kit sync"`. The Maven `verify` lifecycle runs a javadoc goal that forks a second lifecycle starting at `generate-sources`; its `npm ci` deletes `node_modules/$app/tsconfig.json` after the compile-phase build created it, leaving `npm run check` unable to resolve `$app/tsconfig`. The prepare hook, which is the SvelteKit template's own setup, regenerates it after every install and keeps the gate deterministic. |

---

## 3. Test & Verification Results

### 3.1 Unit tests (`pm-web-app`)

```
Test Files  42 passed (42)
Tests       300 passed (300)
```

The i18n suite alone is 25 tests (19 in `index.test.ts`, 6 in `catalogue.test.ts`).

### 3.2 Catalogue guard

`src/lib/i18n/catalogue.test.ts` passes: EN/DE parity, zero literal copy in `.svelte` sources, zero `$t(...) || '...'` fallbacks in `.svelte`/`.ts` sources, no `fallbackMessages` dictionary in `index.ts`, and every statically named key present in the catalogue.

### 3.3 Static checks

* `npm run check` (svelte-check): 0 errors, 0 warnings.
* `npm run lint` (ESLint flat config): clean.
* `npm run build` (Vite/SvelteKit static site): succeeds.
* `prettier --check` on the touched files: clean. Repository-wide `format:check` still reports eight pre-existing unrelated files, untouched by this plan.

### 3.4 E2E

The Playwright suite is skipped in the default Maven build (`skipPlaywrightTests=true`, browsers not installed). The only E2E edit is the removal of a stale comment and test rename in `reports.spec.js`; no assertion changed.

### 3.5 Automated review findings (PR #142)

* **CodeRabbit** — two minor findings: (1) a slow full-catalogue response could replace the store after a locale switch, and (2) a partial area merged into whichever locale owned the store and was persisted under the requested locale. Both are fixed in `b3d508f` through `messagesLocale` ownership tracking, with regression tests.
* **SonarCloud** — Quality Gate passed; the two new minor issues (`Object.prototype.hasOwnProperty.call` and `filter(...).pop()`) were replaced with `Object.hasOwn()` and `findLast()` in `b3d508f`. No accepted issues, no security hotspots.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| No uncatalogued UI text | **Pass** — `catalogue.test.ts` green. |
| No `$t(...) \|\| fallback` | **Pass** — no string fallback added; key resolution lives in the i18n module. |
| No production fallback dictionary | **Pass** — no `fallbackMessages`; English comes from `localStorage` and the runtime only. |
| Bilingual catalogue (EN & DE) | **Pass** — unchanged bundles, parity asserted. |
| Header count badges retained | **Pass** — `reports-badge`/`decisions-badge` markup untouched. |
| Screen-reader announcements retained | **Pass** — `sr-only` region still renders `quietReportNotice` and `quietDecisionNotice`. |
| Raw translation keys never displayed | **Pass** — `humanizeKey()` is the final tier; covered by unit tests. |
| Module boundary discipline | **Pass** — only `pm-web-app`, `pm-web-e2e`, and `doc/agy/` files changed; no backend or sub-view edits. |
| Responsive layout | **Pass** — the removal deletes a banner row; the existing `@media (max-width: 640px)` header rule is unchanged. |

---

## 5. Build & Quality Evidence

First full reactor run (commit `012bb5c`, 2026-10-20):

```
Reactor Summary:
pm-mcp-clients ..................................... SUCCESS [ 13.927 s]
pm-api ............................................. SUCCESS [01:23 min]
pm-exec ............................................ SUCCESS [ 10.323 s]
pm-mock-runtime .................................... SUCCESS [ 43.496 s]
pm-relay ........................................... SUCCESS [ 11.400 s]
pm-operator ........................................ SUCCESS [02:47 min]
pm-web-app ......................................... SUCCESS [ 48.944 s]
pm-web-e2e ......................................... SUCCESS [ 15.368 s]
BUILD SUCCESS — Total time: 06:35 min
```

After the review fixes (`b3d508f`), the affected modules pass again:

```
pm-web-app ......................................... SUCCESS [01:04 min]  (300 tests, svelte-check 0 errors)
pm-web-e2e ......................................... SUCCESS [  6.893 s]
BUILD SUCCESS
```

A second full-reactor run on 2026-10-21 stopped in `pm-operator` at the pre-existing, date-dependent `PlanCompletionLevelTest.minimalReport`: it hard-codes an `ended_at` of `2026-10-18T10:00:00Z` and asserts the rendered age says `2 d ago`, which became `3 d ago` on that date. The test and module are outside this plan's boundary and were not modified; the first full run (2026-10-20) passed the same module. The `pm-web-app`/`pm-web-e2e` reactor result above is green.

Note: the inherited OpenRewrite recipe set reformats 311 unrelated Java files during the gate (an import-spacing and continuation-indent drift inherited from the parent POM). Those files were reverted after each run to leave a clean worktree, matching the practice recorded in earlier completion reports.

---

## 6. Post-Landing Verification (Step 8)

**Pending.** This report is authored by the implementing session. The 4-pillar verification under [`doc/meta/verification-protocol.md`](../meta/verification-protocol.md) and the move of this report to `doc/agy/verified/` are the orchestrator's Step 8 responsibility.

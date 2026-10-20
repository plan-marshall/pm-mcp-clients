# Completion Report: `web-foundation-routing-and-catalogue`

## 1. Executive Summary

This plan completed the foundational remediation of `pm-web-app` and `doc/design/web-app`:

* the web app design document is complete and `FINAL`, with a full visual language (frame, grid, breakpoints, typography, semantic colour tokens, states and attention, light and dark themes) and dedicated layouts for the reports, system, and cleanup views;
* every application view has a canonical URL, parameterized paths, a caller navigation stack, and browser `pushState`/`popstate` handling; the temporary `WorkspaceView` stub is retired, and a paired, onboarded browser lands on its remembered start point (`projects` or `overview`);
* the ~780-line `fallbackMessages` dictionary is gone from production code, replaced by a test-only catalogue fixture, and every hardcoded UI literal and `$t(...) || '...'` fallback in `pm-web-app/src/` is replaced by a pure catalogue look-up;
* `catalogue.test.ts` now discovers every `.svelte` source, rejects literal text and attribute copy, rejects fallbacks in every `.svelte`/`.ts` source, rejects an embedded fallback dictionary, and verifies that every statically named key exists in the catalogue.

The static catalogue scan reports **zero** literal-copy offenders and **zero** fallbacks.

## 2. Delivered Artifacts

### 2.1 Design (`doc/`)

| File | Change |
|---|---|
| `doc/design/web-app/README.adoc` | `Status: FINAL`; new `Visual Language` section: frame tiers, layout grid, responsive breakpoints (`desktop >= 1024px`, `tablet 640–1023px`, `phone < 640px`), typography scale (`sans` vs `mono`), semantic colour roles, states and attention, light/dark themes, reports/system/cleanup layouts, and the route table. |
| `doc/design/README.adoc` | Web App index entry now lists the completed visual language and routes. |
| `doc/plans/README.adoc` | `web-design` deliverable records the completed visual language and layouts. |

### 2.2 Routing (`pm-web-app/src`)

| File | Change |
|---|---|
| `src/lib/routes.ts` | Canonical paths for `pair`, `entry`, `onboarding`, `project-onboarding`, `settings`, `machine-settings`, `projects`, `overview`, `plan`, `epic`, `decomposition`, `decisions`, `reports`, `system`, `cleanup`, `designer`, `start`; parameterized `matchPath`/`routeForPath` and `pathForRoute` for plan, epic, decomposition, project start, and entry; safe segment decoding. |
| `src/lib/stores/runtime.ts` | Caller navigation stack (`pushRoute`, `goBack`, `routeParamsOf`), route application with persisted `pm_start_route`, history-API `syncUrl`, popstate adoption of plan/epic/start identifiers, and a `landingRoute` that honours the remembered start point or reports `overview`/`projects` from the projects summary. |
| `src/routes/+layout.svelte` | `syncPath` writes `pushState` entries (first write replaces), skips unknown paths instead of writing `/`, and loads the catalogue before the runtime so start-up notices translate. |
| `src/lib/views/AppRouter.svelte` | Removed the `WorkspaceView` branch; renders the onboarding for both machine and project onboarding. |
| `src/routes/**` | New route shells: `overview`, `plans/[id]`, `epics/[id]`, `epics/decompose`, `decisions`, `reports`, `system`, `system/cleanup`, `designer`, `projects/onboard`. |
| `src/lib/views/WorkspaceView.svelte` | **Deleted** (retired stub). |
| `SettingsView`, `ProcessDesigner`, `ProjectExplorerView` | Back/continue navigation now uses canonical routes (`projects`, `project-onboarding`). |
| `MachineSettingsView.svelte` | Added the process designer entry (its only remaining entry point after the stub's retirement). |

### 2.3 Catalogue Purity

| File | Change |
|---|---|
| `src/lib/i18n/index.ts` | Removed the 588-key `fallbackMessages` dictionary; a missing key renders its key; ICU plural parser now supports exact-count branches (`=0`, `=1`). |
| `src/test/catalogue-fixture.ts` | **New** test-only loader of `pm-api`'s message bundles for Vitest. |
| `src/setupTests.ts` | Loads the catalogue fixture. |
| `src/lib/i18n/testMessages.ts` | **Deleted** (superseded by the fixture). |
| `src/lib/i18n/catalogue.test.ts` | Rewritten: all `.svelte` discovery, literal/attribute scan, fallback scan across `.svelte`/`.ts`, embedded-dictionary guard, and static-key existence check. |
| `pm-api/src/main/resources/messages/*.{en,de}.properties` | 20 bundles extended with the web views' keys (frame, reports, decisions, system, cleanup, projects, overview, plan, epic, decomposition, notifications), with EN/DE parity. |
| 11 `.svelte` views and `+layout.svelte` | All literal text nodes and `placeholder`/`title`/`aria-label` values now come from `$t(...)`; all `$t(...) ||` fallbacks removed. |
| `src/lib/stores/runtime.ts` | Arrival notices and quiet notices are composed through `translate`. |

### 2.4 Tests & E2E (`pm-web-e2e`)

| File | Change |
|---|---|
| `tests/workspace.spec.js` | Rewritten as `Routing, History & Frame`: explorer landing without the retired stub, URL per view, Back/Forward, deep links to `/system` and `/settings/machine`, settings navigation, theme toggle. |
| `tests/designer.spec.js` | Opens the designer through the machine settings and asserts `/designer`. |
| `tests/projects.spec.js`, `tests/system.spec.js`, `tests/reports.spec.js`, `tests/overview.spec.js`, `tests/git-sources.spec.js`, `tests/lesson-source.spec.js`, `tests/plan-view.spec.js`, `tests/start.spec.js` | Header navigation locators scoped to `.app-header`; stale view selectors aligned with the current markup. |

## 3. Test & Verification Results

### 3.1 Unit tests (`pm-web-app`)

```
Test Files  41 passed (41)
Tests       248 passed (248)
```

`npm run lint` (ESLint flat config) completes with no findings. `npm run build` completes and writes the static site.

Route coverage added in `src/lib/routes.test.ts`: 13 cases, including every fixed path, plan/epic/decomposition parameters, start paths, escape decoding, malformed escapes, and the `/epics/decompose` vs `/epics/{id}` distinction.

### 3.2 Catalogue gate

`src/lib/i18n/catalogue.test.ts` passes with:

* EN/DE key parity (`2314` keys each at the time of writing);
* zero literal text nodes or `title`/`placeholder`/`aria-label` literals in all `.svelte` sources;
* zero `$t(...) || '...'` / `?? '...'` fallbacks in all `.svelte` and `.ts` sources;
* no `fallbackMessages` in `src/lib/i18n/index.ts`;
* every statically named `$t('key')` present in the catalogue.

### 3.3 E2E (`pm-web-e2e`, Playwright/Chromium)

The plan's E2E change (`tests/workspace.spec.js`) passes 5/5. The routing-related suites also pass: `designer` 4/5 (the remaining test is a pre-existing mock-data gap, see below), `projects` 4/4, `overview` 4/4, `git-sources` 4/4.

The repository's E2E baseline on `main` is not green: a comparison run of the unchanged `main` worktree produced **28 failed / 45 passed**. This branch produces **24 failed / 50 passed**; every remaining failure is one of the pre-existing `main` failures (the `reports`, `system`, `decisions`, `lesson-source`, `plan-view`, `start`, and one `designer` test). Two causes are outside this plan's boundary: the `reports` and `system` mock scenarios define only `GET /api/v1/status` (no reports/system/cleanup payloads), and the designer placement test selects a project option the `designer` scenario does not publish. The branch removes the four stale `workspace.spec.js` failures and adds no new one.

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| No uncatalogued UI text | **Pass** — static gate green across all `.svelte` sources. |
| No `$t(...) || fallback` | **Pass** — gate green across all `.svelte`/`.ts` sources. |
| No production fallback dictionary | **Pass** — `fallbackMessages` removed; a test-only fixture serves Vitest. |
| Bilingual catalogue (EN & DE) | **Pass** — `2314` keys in each locale, parity asserted. |
| Every view has a canonical URL | **Pass** — route table and route tests; E2E Back/Forward and deep links. |
| No route resets the URL to `/` | **Pass** — `pathForRoute` has no `/` fallback; `syncPath` skips routes without a path. |
| `WorkspaceView` retired | **Pass** — file deleted; dispatcher and navigation updated; E2E asserts its tab bar is absent. |
| Start-point landing | **Pass** — `pm_start_route` remembered, `overview` when work runs/waits, `projects` otherwise. |
| Module boundary discipline | **Pass** — only `pm-api` message resources, `pm-web-app`, `pm-web-e2e`, and `doc/` changed; no `pm-operator`/`pm-mock-runtime` scenario edits. |
| Philosophy: state-driven screens | **Pass** — route shells mount the same dispatcher; no new client-side step order. |

## 5. Build & Quality Evidence

The full reactor gate was executed as `MAVEN_OPTS="-Xmx1536m" ./mvnw verify -DforkCount=0`:

```
Reactor Summary:
pm-mcp-clients ..................................... SUCCESS [  0.270 s]
pm-api ............................................. SUCCESS [  5.622 s]
pm-exec ............................................ SUCCESS [  1.322 s]
pm-relay ........................................... SUCCESS [  4.894 s]
pm-mock-runtime .................................... SUCCESS [  7.123 s]
pm-operator ........................................ SUCCESS [01:28 min]
pm-web-app ......................................... SUCCESS [ 34.678 s]
pm-web-e2e ......................................... SUCCESS [  9.198 s]
BUILD SUCCESS — Total time: 02:31 min
```

Notes on the invocation:

* The repository's root POM defines no `pre-commit` profile; `verify` itself carries the quality gates (license header format, OpenRewrite recipes, Checkstyle/Enforcer, Javadoc, unit tests). Maven ignores the unknown `-Ppre-commit` and runs the normal `verify`.
* `-DforkCount=0` runs surefire in the Maven JVM. The default fork-per-class mode had its forked VMs terminated with exit `143` twice while another worktree ran a full reactor build (the machine's swap was exhausted: 10.1 GB of 11.2 GB used). In-process execution completes the same test set.
* The parent POM's OpenRewrite recipe rewrites 310 pre-existing Java sources repo-wide on every `verify` (import grouping and indentation). Those rewrites are unrelated to this plan and were reverted so the PR stays scoped; no Java source in this plan's footprint changed.

Web-app evidence: `npm test` 41 files / 248 tests passed; `npm run lint` clean; `npm run build` succeeded; `npm run format:check` clean for every changed file; the ASCII-diagram validator reports `misaligned_count: 0` for the design document.


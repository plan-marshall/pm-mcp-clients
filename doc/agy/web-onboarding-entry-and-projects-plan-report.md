# Completion Report: `web-onboarding-entry-and-projects`

Implementation plan: [`doc/agy/implemented/web-onboarding-entry-and-projects-plan.md`](implemented/web-onboarding-entry-and-projects-plan.md)
Branch: `feat/web-onboarding-entry-and-projects`
Worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-web-onboarding-projects`
Governing SOP: [`doc/agy/runbook.md`](runbook.md)

---

## 1. Executive Summary

The plan closes the remaining gaps in the web app's project surface:

- a dedicated **10-step project onboarding view** at `/projects/onboard`, rendering the runtime's
  setup state with the step line, the build environment the terminal client last captured, a
  concurrent-change conflict card, and the setup question chat (or its disabled reason);
- **favourite project ordering** in the explorer, by move-up/move-down controls and drag and drop,
  persisted through the runtime;
- **project settings conflict resolution** in a project setup drawer, sending the revision the
  drawer was read at and offering _Keep the current value_ / _Apply yours again_ on `conflict`;
- **direct aspect jumping** from the machine settings into the onboarding, and a `Back` button on
  the settings page that returns to the route the operator came from.

All changed screens remain catalogue-driven, responsive down to phone width, and covered by Vitest
component tests plus Playwright E2E coverage of the reachable flows.

---

## 2. Delivered Artifacts

### 2.1 `pm-web-app` — production code

| File | Change |
|---|---|
| `src/lib/views/ProjectOnboardingView.svelte` | **New.** Ten-step project onboarding: step line, checkout start, current-step screen, build-environment panel (captured vs. never captured with the capturing command), conflict card, setup-question chat drawer with untrusted marking and disabled-reason state, pre-enrolment run from the explorer report, completion screen. |
| `src/lib/views/ProjectExplorerView.svelte` | "Add a project" and "Continue" now open `/projects/onboard`; favourites gained move-up/move-down buttons and drag-and-drop persistence with rollback on refusal; the ad-hoc add modal was replaced by the onboarding checkout step; a project setup drawer reads and saves the project's own values against `expected_revision` and resolves `conflict`. |
| `src/lib/views/OnboardingView.svelte` | Delegates to `ProjectOnboardingView` on the `project-onboarding` route and opens at the step a `Change` or the address bar asked for. |
| `src/lib/views/MachineSettingsView.svelte` | `Change` on an aspect sets the onboarding step and opens `/onboarding?step={n}`. |
| `src/lib/views/SettingsView.svelte` | `Back` calls `goBack()` and replaces the previous history entry. |
| `src/lib/api/client.ts` | New project onboarding/settings contract types and calls (`getProjectOnboarding`, `stepProjectOnboarding`, `getProjectSettings`, `saveProjectSettings` with `expected_revision`, `reorderFavouriteProjects`), with `409` conflict bodies returned rather than thrown. |
| `src/lib/stores/runtime.ts` | `selectedOnboardingId`, `onboardingStep`, `openProjectOnboarding()`, `openOnboardingStep()`, `requestedOnboardingStep()`. |

### 2.2 `pm-api` — message catalogue

| File | Change |
|---|---|
| `src/main/resources/messages/projects.en.properties` | 39 new keys for the project onboarding view, its build-environment panel, the setup chat, the setup drawer and favourite ordering. |
| `src/main/resources/messages/projects.de.properties` | The same 39 keys in German, with escaped umlauts. |

### 2.3 `pm-web-app` — tests

| File | Change |
|---|---|
| `src/lib/views/ProjectOnboardingView.test.ts` | **New**, 10 tests: ten steps and captured build environment, never-captured notice, conflict card with apply-again and keep, chat disabled reason, chat turns with untrusted marking, pre-enrolment run, abandon, starting a new run, unreadable run. |
| `src/lib/views/ProjectExplorerView.test.ts` | +7 tests: reorder down/up, drag and drop, rollback on refusal, setup drawer with `expected_revision`, conflict apply-again, keep-current, route transitions from Add/Continue. |
| `src/lib/api/client.test.ts` | +3 tests: project onboarding read/step with a `409` conflict body, project settings with `expected_revision` and `409`, favourite ordering through the explorer contract. |
| `src/lib/views/MachineSettingsView.test.ts` | +1 test: `Change` opens the onboarding at the aspect's step. |
| `src/lib/views/OnboardingView.test.ts` | +1 test: the onboarding opens at the requested step. |
| `src/lib/views/SettingsView.test.ts` | +1 test: `Back` returns to the calling route. |

### 2.4 `pm-web-e2e`

| File | Change |
|---|---|
| `tests/projects.spec.js` | +2 tests: favourite reordering with the move control and operator order; `/projects/onboard` from Add and from Continue. |
| `tests/onboarding.spec.js` | +2 tests: machine-settings aspect jump to `/onboarding?step=6`; Settings `Back` returning to `/projects`. |

---

## 3. Test & Verification Results

| Gate | Command | Result |
|---|---|---|
| Web unit/component tests | `cd pm-web-app && npm test` | **42 files, 272 tests passed** (248 before the change) |
| Web lint | `cd pm-web-app && npm run lint` | clean |
| Web production build | `cd pm-web-app && npm run build` | `✓ built`, static site written |
| Playwright E2E (plan scope) | `cd pm-web-e2e && npx playwright test tests/projects.spec.js tests/onboarding.spec.js tests/devices.spec.js tests/entry.spec.js` | **25 passed** |
| E2E lint | `cd pm-web-e2e && npm run lint` | clean |
| Catalogue parity | `./mvnw -pl pm-api test -Dtest=ProjectsCatalogueTest` | passed |
| Repository pre-commit gate | `./mvnw verify -Ppre-commit` | **BUILD SUCCESS** (10:09 min, all eight modules) |

The Playwright suite ran against a live `pm-mock-runtime` on port 7420 with the compiled
`pm-api`/`pm-mock-runtime` classes, and the Vite dev server on 5173.

---

## 4. Domain Invariants

- **No uncatalogued strings** — `catalogue.test.ts` passes. Every new visible text, `aria-label`
  and placeholder is a catalogue look-up; the new keys exist in both `projects.en.properties` and
  `projects.de.properties`, and no `$t(...) || '...'` fallback was introduced.
- **Bilingual catalogue** — English and German carry exactly the same keys (`ProjectsCatalogueTest`).
- **Catalogue as the only source** — `fallbackMessages` remains absent from
  `src/lib/i18n/index.ts`.
- **Responsive layout** — the new view and drawer use the shared tokens and collapse to one column
  at `640px`/`600px`; the step line scrolls horizontally instead of overflowing the page. The
  existing phone-width E2E assertion in `onboarding.spec.js` remains green.
- **Light and dark** — all colours come from the design tokens, so both themes follow without
  per-screen overrides; the dark-theme E2E assertion remains green.
- **DOM trust boundary** — the setup chat renders the role's text as text nodes (no `{@html}`); an
  untrusted turn is marked in the UI. No new HTML injection sink was added.
- **Runtime is the authority** — favourite order, setup values and step transitions are written by
  the runtime, never cached in browser storage; the client never compares values itself but sends
  the revision it read and shows what the runtime reports.
- **Test shape** — component tests follow Arrange–Act–Assert, use `waitFor` rather than fixed
  delays, and cover the refusal/conflict branch of every new write.

---

## 5. Build & Quality Evidence

- `./mvnw verify -Ppre-commit` finished with `BUILD SUCCESS` for `pm-api`, `pm-exec`,
  `pm-mock-runtime`, `pm-relay`, `pm-operator`, `pm-web-app` and `pm-web-e2e`.
- The build includes `npm ci`, the production SPA build, the complete Vitest suite, and ESLint in
  both web modules.

### Deviations and notes

1. **Step taxonomy follows the design, not the plan's prose list.** The plan's §2 names ten steps
   (identity, build system, skill domains, quality gates, …). The normative local design
   (`doc/design/shared/project-onboarding.adoc#steps`) and the existing catalogue define the ten
   steps as Checkout, Identity, Enrolment, Own settings, Team settings, Architecture, Landing,
   Roles, Descriptions, Commit. The view follows the design so it renders the texts the catalogue
   already ships and stays traceable to UC-PRJ-1. The build environment the plan asks for on
   step 2 is shown on the identity/enrolment steps, where the design places it.
2. **Favourite order uses the documented endpoint.** The plan's §2.2 names
   `PUT /api/v1/projects/favourites/order`; `project-contract.adoc` and the mock runtime implement
   `PUT /api/v1/explorer/favourites`. `apiClient.reorderFavouriteProjects()` keeps the plan's call
   site but writes through the contract endpoint, so the feature works against the runtime that
   exists.
3. **Runtime-side endpoints are ahead of the local mock.** `GET /api/v1/projects/{id}/onboarding`,
   `POST /api/v1/projects/{id}/onboarding/step` and the project settings endpoints are owned by the
   prerequisite `mock-runtime-projects-and-start` plan, which has not landed yet. The web client
   and views implement the contract, and degrade with a catalogue error and a retry where the mock
   runtime does not yet answer. The E2E flows that are reachable today — adding, continuing,
   favourite reordering — pass; the onboarding and settings-conflict behaviours are proven by the
   component tests' mocked contract.
4. **Pre-commit OpenRewrite baseline drift.** `verify -Ppre-commit` (parent `pm-mcp-parent:0.1.1`)
   rewrites import grouping and record-parameter indentation across the whole existing Java tree,
   unrelated to this change. Those rewrites were reverted to keep this PR scoped; the gate itself
   passed. A repository-wide formatting pass should land separately before the recipe is treated as
   a zero-diff gate.

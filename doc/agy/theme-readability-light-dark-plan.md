# Implementation Plan: `theme-readability-light-dark` (Every View Readable in Both Themes)

The web app uses semantic colour tokens and declares a light and a dark theme, but the dark theme has never been audited across the views. The reported flow is concrete: on `/overview`, an epic's action area is hard to read in dark mode, and after opening the epic the findings action (“Prüfen” / “Review findings”) is nearly invisible. The same class of defect appears in badges, modal overlays, warnings, success and danger text, and native buttons whose colour is inherited from the browser rather than the theme.

The root causes are measurable:

- `button { background: none; }` in `base.css` does not set `color: inherit`, so native buttons without an explicit colour — including the epic review button — keep the browser's `buttontext` colour (near-black).
- Five semantic tokens are referenced but never defined in `tokens.css`: `--color-accent`, `--color-accent-hover`, `--color-bg-base`, `--color-bg-subtle-hover`, and `--color-warning-text`. An invalid `var()` colour falls back to inheritance or the browser default, which is exactly what dark mode exposes.
- The views contain hundreds of hard-coded light-theme colours and `var()` fallbacks; the design's own rule is that a view names a token and the theme resolves it.
- The existing dark-theme coverage checks the theme attribute and one onboarding screen; it does not measure computed contrast on every view.

This plan adds a real light/dark readability gate, fixes the token foundation and the affected components, and verifies the reported flow in a browser again.

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Design authority** | [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc) — [Visual Language](../design/web-app/README.adoc#visual-language), [Themes](../design/web-app/README.adoc#themes) (`4.5:1` for text, no view hard-codes a hex value) |
| **Tokens** | [`tokens.css`](../../pm-web-app/src/styles/tokens.css), [`base.css`](../../pm-web-app/src/styles/base.css) |
| **Reported surfaces** | [`OverviewView.svelte`](../../pm-web-app/src/lib/views/OverviewView.svelte), [`EpicView.svelte`](../../pm-web-app/src/lib/views/EpicView.svelte), [`ControlActionsBar.svelte`](../../pm-web-app/src/lib/components/ControlActionsBar.svelte), [`FindingsReviewModal.svelte`](../../pm-web-app/src/lib/components/FindingsReviewModal.svelte) |
| **Other views in scope** | `PlanView`, `ReportsView`, `DecisionView`, `ProjectExplorerView`, `SettingsView`, `MachineSettingsView`, `SystemView`, `CleanupView`, `StartView`, `OnboardingView`, `ProjectOnboardingView`, `ProcessDesigner`, `CompletionReportView`, `EpicDecompositionView`, `DecompositionReviewView`, `PairingView`, `RevokedView`, `ListenerOffView` |
| **Existing theme coverage** | [`chrome.spec.js`](../../pm-web-e2e/tests/chrome.spec.js), [`workspace.spec.js`](../../pm-web-e2e/tests/workspace.spec.js), [`onboarding.spec.js`](../../pm-web-e2e/tests/onboarding.spec.js), [`layout.test.ts`](../../pm-web-app/src/routes/layout.test.ts) |
| **Use cases** | UC-PLN-1/2/4/5/6 (running work, plan, epic, decisions, reports), UC-CTL-3 (findings/watches review), UC-ENT-1, UC-PRJ-1..4, UC-SYS-1..3, UC-ONB-1..10, UC-STA-1..9 |
| **Operational SOP** | [`runbook.md`](runbook.md) |

### 1.1 Finding Analysis

| # | Finding | Root cause | Correction |
|---|---|---|---|
| **F1** | The epic findings action (“Prüfen”) is nearly invisible in dark mode. | `.review-open` is a native `<button>` with no `color`; `base.css` resets background but not `color`, so the UA `buttontext` (near-black) paints on the dark card. | Set `color: inherit` in the button reset and/or give the review button an explicit `--color-text-primary`/`--color-primary` colour. Add a component/contrast test for it. |
| **F2** | The epic action area and related controls are unreadable in dark mode. | `--color-accent` and `--color-accent-hover` are used by `EpicView`/`PlanView` but are not defined; invalid `var()` colours inherit or fall back to the UA. `--color-bg-base`, `--color-bg-subtle-hover`, and `--color-warning-text` are also missing. | Define the missing semantic tokens in `:root`, `[data-theme='light']`, and `[data-theme='dark']` (or replace the references with the existing primary/text tokens), and add the token-completeness test. |
| **F3** | “The colours do not work in general.” | Views hard-code light-theme palettes: warning/success/danger foregrounds, badge backgrounds, overlays, the favourite star, diff/danger text, and `var(--token, #hex)` fallbacks. They do not follow the theme. | Replace hard-coded colours with semantic tokens; add `--color-*-text`, `--color-overlay`, and selected/hover tokens where the roles are missing. Audit every hard-coded literal in views/components; keep literals only in `tokens.css` (and documented diagram assets). |
| **F4** | No automated readability proof. | The current dark tests assert the theme attribute or a single onboarding render; no view matrix computes text/background contrast, and no test catches undefined tokens. | Add a token-completeness unit test and a Playwright light/dark contrast matrix that walks every view and reports the failing element, colour pair, ratio, and screenshot. |
| **F5** | The reported flow must be verified in the browser after the fix. | Manual light/dark checking is not repeatable. | Add the exact Overview → epic → actions → findings flow to the theme spec and capture both themes; verify in the local browser as the final step. |

---

## 2. Scope & Objectives

1. **Token foundation**
   - Define every referenced semantic token in both themes: `--color-accent`, `--color-accent-hover`, `--color-bg-base`, `--color-bg-subtle-hover`, `--color-warning-text`; add role-text tokens for success/warning/danger where a subtle background needs a readable foreground; add `--color-overlay` for modal backdrops if the existing `rgba` literals are replaced.
   - Set `color: inherit` in the global button reset.
   - Keep the light and dark blocks identical in key set (a key present in one is present in the other).
2. **View and component remediation**
   - Fix the reported epic action/review surfaces first.
   - Replace hard-coded foreground/background/border/overlay colours with tokens across every view and component.
   - Ensure badges, warnings, success, danger, disabled reasons, links, selected rows, diff states, and focus rings remain readable in both themes.
   - Preserve the existing visual hierarchy; the fix is colour and contrast, not a redesign.
3. **Readability gate**
   - Unit: every `var(--…)` in `pm-web-app/src` resolves to a token defined in `tokens.css` (local inline custom properties such as `--workflow-icon-size` are allowlisted).
   - Unit/static: no hard-coded colour literals in `views/` or `components/`; the only literals live in `styles/tokens.css`, with a small documented allowlist for data-only assets.
   - Playwright: every route/view in light and dark, desktop and phone widths, with computed contrast checks and screenshots.
4. **Exact reported flow**
   - `/overview` → epic tab → an epic's actions → findings review entry (“Prüfen”) must pass the contrast check in dark and light.
5. **Browser verification**
   - After the fix, run the app against the mock runtime and inspect the reported flow in both themes, plus the full view matrix.

### Non-Goals

- No layout redesign, navigation change, copy change, or new feature.
- No server, API, record, mock-scenario, operator, or designer-model change.
- No new theme (only light and dark).
- No colour-only meaning; text and icons keep their labels.
- No `prefers-contrast`/forced-colours support unless the audit shows it is needed.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-theme-readability` | `fix/theme-readability` | `pm-web-app/src/styles`, views/components, unit tests, Playwright theme spec/helpers, design note, report |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree.

**Sole-owner files**: `tokens.css`, `base.css`, the affected `.svelte` files, the new contrast helper/test, `pm-web-e2e/tests/theme-readability.spec.js`, a new `doc/design/web-app/theme-readability.adoc`, and the report.

**No touch**: `pm-api`, `pm-mock-runtime`, `pm-mcp-server`, `pm-operator`, feature behaviour, layouts, copy, and the existing roadmap plans.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-theme-readability` on branch `fix/theme-readability` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the token/component fixes;
  - `pm-dev-frontend:css` for the theme roles, contrast, and token architecture;
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for the token/component tests;
  - `plan-marshall-persona-integration-tester` for the Playwright light/dark matrix;
  - `plan-marshall-persona-security-expert` for untrusted/dynamic text rendering and no colour-only state;
  - `plan-marshall-persona-documenter` for the design note and report;
  - `plan-marshall-persona-code-reviewer` for review triage.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Completion report written (`doc/agy/theme-readability-light-dark-plan-report.md`), plan moved to `doc/agy/implemented/`, links rebased, dashboard updated.
- [ ] 5. Pre-commit passed: `./mvnw verify -Ppre-commit`.
- [ ] 6. PR opened (never `skip-bot-review` on worker PRs); review windows and all bot/Sonar findings resolved.
- [ ] 7. Landed on `main`; worktree and branch cleaned up.
- [ ] 8. Post-landing verification and report archival by the orchestrator.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../design`, `../../pm-web-app`, `../../pm-web-e2e`, `../../../plan-marshall-documentation`, and `implemented/<name>.md`. When Step 4 moves it, rebase every link and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Baseline audit** | `plan-marshall-persona-integration-tester` | `pm-dev-frontend:javascript` | Route/view inventory, theme toggle, failing element capture. |
| **Phase 2: Tokens and button reset** | `plan-marshall-persona-implementer` | `pm-dev-frontend:css` | Missing semantic tokens, light/dark parity, `color: inherit`. |
| **Phase 3: View remediation** | `plan-marshall-persona-implementer` | `pm-dev-frontend:css`, `pm-dev-frontend:javascript` | Hard-coded palettes, badges, overlays, statuses, review/action surfaces. |
| **Phase 4: Contrast gate** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing`, `plan-marshall-persona-integration-tester` | Token completeness, computed contrast, screenshots, exact reported flow. |
| **Phase 5: Verification & docs** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Theme contract note, browser evidence, report, finding triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Baseline and Failing Evidence

- [ ] **1.1 Inventory the theme surface**
  - Enumerate every route/view and its modal/dialogue states: overview (running/ended/epic), plan, epic + findings/watches, reports + completion, decisions, projects + settings, machine settings, system + cleanup, start, onboarding, project onboarding, designer, pairing/entry/revoked/listener-off, decomposition review.
  - Record the initial contrast failures in a short baseline section of the report, including the reported `Prüfen` element.
- [ ] **1.2 Build the contrast helper**
  - In `pm-web-e2e/tests/helpers/` add `contrast.js`:
    - parse `rgb()`/`rgba()` and hex from computed styles;
    - composite translucent backgrounds over ancestors;
    - compute the WCAG relative-luminance ratio;
    - consider `font-size`/`font-weight` for the large-text threshold (`3:1`), otherwise `4.5:1`;
    - skip `display:none`, `visibility:hidden`, `aria-hidden="true"`, and zero-size elements;
    - return `{text, selector, color, background, ratio, threshold}`.
  - Add a Playwright helper that sets `data-theme` explicitly (`light`/`dark`) instead of relying on OS preference.
- [ ] **1.3 Add the failing theme spec**
  - `pm-web-e2e/tests/theme-readability.spec.js` walks the view matrix in both themes and fails on the first element below the threshold.
  - Keep a bounded allowlist for intentionally hidden/large/decorative elements, with a reason per entry.
  - Store a screenshot per view/theme on failure.

### Phase 2: Token Foundation

- [ ] **2.1 Define the missing tokens**
  - Add to `:root`, `[data-theme='light']`, and `[data-theme='dark']`:
    - `--color-accent` and `--color-accent-hover` (map to primary/primary-hover, or the design's chosen semantic equivalent);
    - `--color-bg-base` (map to `--color-bg-app` or the intended base);
    - `--color-bg-subtle-hover` (a readable hover step in both themes);
    - `--color-warning-text` (a warning foreground that keeps 4.5:1 on `--color-warning-subtle`);
    - success/danger foreground tokens if the current `--color-success`/`--color-danger` values fail on their subtle backgrounds;
    - `--color-overlay` for modal backdrops.
  - Keep the light and dark key sets identical.
- [ ] **2.2 Button and link reset**
  - Add `color: inherit` to the global `button` reset (or to the specific review/action buttons); ensure controls with an explicit role colour keep it.
  - Check `input`, `select`, and `textarea` colour/background too.
- [ ] **2.3 Token completeness test**
  - Add `pm-web-app/src/styles/tokens.test.ts` (or a Vitest source test) that:
    - parses `tokens.css` definitions;
    - scans `src/**/*.{svelte,css}` for `var(--…)`;
    - allows local inline custom properties (`--workflow-icon-size`, `--workflow-icon-url`) and documents them;
    - fails with the file and token name for every undefined reference.
  - Confirm the baseline failures (accent, accent-hover, bg-base, bg-subtle-hover, warning-text) are gone.

### Phase 3: View and Component Remediation

- [ ] **3.1 The reported surfaces**
  - `EpicView.svelte`: `.review-open`, action/register headings, counts, badges, and the findings/watches entry.
  - `ControlActionsBar.svelte`: action buttons, disabled reasons, dialogue panels, hints.
  - `OverviewView.svelte`: epic/plan cards, attention bands, action menus, selected/ended states.
- [ ] **3.2 Core views**
  - `PlanView.svelte`: badges, selected rows, off-track banner, node/task states, token tables.
  - `ReportsView.svelte` / `DecisionView.svelte`: badges, unread/read states, split selection, forms, notices.
  - `CompletionReportView.svelte`: diff/added/removed roles, token tables, success/danger text.
- [ ] **3.3 Projects, settings, system**
  - `ProjectExplorerView.svelte`: favourite star, cards, modal overlays, filters, destructive actions.
  - `SettingsView.svelte`, `MachineSettingsView.svelte`: forms, consents, credential states, overlays.
  - `SystemView.svelte`, `CleanupView.svelte`: figures, drain/upgrade banners, cleanup states, destructive confirmations.
- [ ] **3.4 Start, onboarding, designer**
  - `StartView.svelte`, `OnboardingView.svelte`, `ProjectOnboardingView.svelte`, `RecipeReadyView.svelte`: steps, consent cards, warnings/errors, source states.
  - `ProcessDesigner.svelte`, `EpicDecompositionView.svelte`, `DecompositionReviewView.svelte`: diff/conflict roles, validation states, side-by-side rows.
- [ ] **3.5 Entry and edge views**
  - `PairingView.svelte`, `RevokedView.svelte`, `ListenerOffView.svelte`: QR surface, notices, buttons.
- [ ] **3.6 Hard-coded literal sweep**
  - Replace foreground/background/border/overlay literals with tokens; remove `var(--token, #hex)` fallbacks where the token is now guaranteed.
  - Keep any remaining literal in a short documented allowlist (e.g. data-only diagram assets, `PairingQr` black/white QR modules if required for scanning).
  - Do not change layout, spacing, or copy.

### Phase 4: Readability Tests

- [ ] **4.1 Token and source tests** (Phase 2.3) plus a static literal scan.
- [ ] **4.2 Component tests**
  - Add a `theme.test.ts` that renders representative components with `data-theme="light"`/`"dark"` and asserts the computed colour tokens or classes for review/action buttons, badges, warnings, and overlays.
  - Cover the `Prüfen` button explicitly.
- [ ] **4.3 Playwright matrix**
  - Light and dark, desktop (1280×800) and phone (375×667; 320×568 where the existing mobile spec covers it).
  - Each view loaded in a real state from the mock scenarios; every visible text element checked by the contrast helper.
  - Exact flow: `/overview` → epic tab → epic card → actions → findings review entry; assert the computed ratio and no console errors.
  - On failure, attach the view, theme, selector, colour pair, ratio, and screenshot.
- [ ] **4.4 Manual browser verification**
  - After the automated gate, run the app against the mock runtime and repeat the reported flow in both themes, with screenshots in the report.

### Phase 5: Verification, Docs, Report

- [ ] **5.1** `npm test`, `npm run check`, `npm run lint`.
- [ ] **5.2** `npx playwright test tests/theme-readability.spec.js` plus the affected existing specs.
- [ ] **5.3** `./mvnw verify -Ppre-commit`.
- [ ] **5.4** `doc/design/web-app/theme-readability.adoc`: the token contract, the contrast thresholds, the audit allowlist, and the light/dark test matrix.
- [ ] **5.5** Report `doc/agy/theme-readability-light-dark-plan-report.md`: baseline failures, token changes, view-by-view evidence, the reported `Prüfen` flow in both themes, and the commands/results.
- [ ] **5.6** Move the plan to `doc/agy/implemented/`, rebase links, update the dashboard.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | The epic findings/review action (“Prüfen”) is readable in dark mode at ≥4.5:1 and remains readable in light mode. | Component test + Playwright exact flow with computed ratio. |
| **F2** | The epic action area and its dialogue controls are readable in both themes; no referenced token is undefined. | Token-completeness test + action-area contrast check. |
| **F3** | Every view in the matrix passes the contrast thresholds in both themes; only documented, reasoned exceptions remain. | Playwright theme matrix + screenshot evidence. |
| **F4** | Every `var(--…)` resolves; no hard-coded colour literal remains in `views/`/`components/` outside the documented allowlist. | Token test + static literal scan. |
| **F5** | The reported Overview → epic → actions → findings flow passes in both themes in a real browser. | Playwright + manual browser screenshots. |
| **—** | Layout, copy, navigation, feature behaviour, and data contracts are unchanged; no server/API/mock change. | Diff review and existing suites. |
| **—** | `npm test`, `npm run check`, `npm run lint`, the theme spec, the affected specs, and `./mvnw verify -Ppre-commit` pass. | CI / report. |

---

## 8. Risk & Rollback Notes

- **Colour-only meaning**: the audit must not fix contrast by removing the text or icon that carries the state; colour remains an accent, never the only carrier.
- **Token aliases**: mapping `--color-accent` to primary is safe only if no view meant a different role; inspect each use before aliasing.
- **Large-text threshold**: the helper must use the correct font-size/weight threshold so a large heading is not failed for 4.5 when 3.0 is the requirement.
- **Translucent backgrounds**: composite the effective background through ancestors; do not compare against transparent and call it black.
- **Dynamic state**: action menus, modals, disabled reasons, hover/focus, and empty states must be included; a static list screenshot is not enough.
- **Theme persistence**: the current theme switch is session state; this plan does not change persistence. If the audit needs a stable theme, set `data-theme` explicitly in tests.
- **Regression risk**: the token aliases and `button { color: inherit; }` affect every view; the full test suite and screenshot matrix are the guard.
- **Rollback**: restore the previous token values and button reset; the readability gate remains as the regression record.

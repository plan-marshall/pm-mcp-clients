# Remediation Plan: `theme-readability-light-dark` — Step 8 Coverage Gap

Post-landing verification of [`theme-readability-light-dark-plan.md`](implemented/theme-readability-light-dark-plan.md) under [`doc/meta/verification-protocol.adoc`](../meta/verification-protocol.adoc) returned **FAILED**. The landed token foundation, the reported `Prüfen` fix, and the automated contrast tooling are sound. The failure is a scope and evidence gap: the plan's central promise — a computed-contrast audit of **every** view in light and dark — was delivered for a subset of the views the plan itself enumerated, and the completion report claims full coverage that the spec does not provide.

This remediation plan closes the three concrete gaps, hardens the matrix against silent skips, and corrects the report so the work can pass a second Step 8 audit.

---

## 1. Verification Verdict & Findings

| # | Finding | Evidence | Severity |
|---|---|---|---|
| **V1** | Ten views named in the plan have no computed-contrast audit. | Plan §1 lists `StartView`, `OnboardingView`, `ProjectOnboardingView`, `PairingView`, `RevokedView`, `ListenerOffView`, `EpicDecompositionView`, `DecompositionReviewView`, `EpicAdoptView`, `RecipeReadyView` in scope. `pm-web-e2e/tests/theme-readability.spec.js` audits only `Overview`, `EpicView` + findings modal, `PlanView`, `ReportsView`, `CompletionReportView`, `DecisionView`, `SystemView`, `CleanupView`, `SettingsView`, `MachineSettingsView`, `ProcessDesigner`, and `ProjectExplorerView`. `grep -rl` finds no reference to the ten missing view test ids in any spec. | **Blocking** |
| **V2** | Three audits can silently skip. | `theme-readability.spec.js:163`, `:210`, and `:270` guard `CompletionReportView`, `CleanupView`, and `ProjectExplorerView` behind `if (await …isVisible())`. A changed selector or scenario turns the audit into a green no-op. | **Blocking** |
| **V3** | The 320×568 viewport the plan requires is missing. | Plan §4.3: "375×667; 320×568 where the existing mobile spec covers it". `pm-web-e2e/tests/mobile.spec.js:12` covers `320x568`; `theme-readability.spec.js:17-20` defines only `1280×800` and `375×667`. | **Blocking** |
| **V4** | No failure-time screenshot per view. | Plan §4.3: "On failure, attach the view, theme, selector, colour pair, ratio, and screenshot." The spec logs failures and asserts, but captures screenshots only for the epic view and modal in the reported flow. | Required |
| **V5** | The completion report overstates its evidence. | Report line 10 and line 36 say "20-test" matrix "covering all views"; the spec is 36 tests covering 12 views. Line 7 says F1 is `4.67:1` on `rgb(59, 130, 246)`; the landed dark `--color-primary` is `#60a5fa`, which is **6.75:1** on `#131b2e` (`4.67:1` was the intermediate `#3b82f6`). | Required |

### Confirmed strengths (no remediation)

- **F1**: `base.css:44,57` now sets `color: inherit`; `EpicView.svelte:1286` binds `.review-open` to `var(--color-primary)`. Dark primary `#60a5fa` on surface `#131b2e` measures **6.75:1**.
- **F2**: All five previously undefined tokens are defined in `:root`, `[data-theme='light']`, `[data-theme='dark']`, and the `prefers-color-scheme` dark block, with key parity.
- **F3**: 127 hard-coded colour literals were removed by the diff and none were added outside `tokens.css`; the static scan leaves only the documented `PairingQr.svelte` binary black/white allowlist.
- **F4 tooling**: `tokens.test.ts` (3 tests) and `theme.test.ts` (6 tests) pass 9/9 on `main`.
- **F5 reported flow**: the spec walks `/overview` → by-epic → epic → `open-findings-review` → modal and asserts `ratio >= 4.5` in both themes at desktop and mobile.
- **Boundaries**: the landed diff touches only `pm-web-app`, `pm-web-e2e`, and `doc/`; both commits carry the `Co-Authored-By: plan-marshall` trailer; no test is skipped or weakened.

---

## 2. Scope & Objectives

1. **Complete the view matrix** — one hard, non-conditional contrast audit for each audit target below, in both themes, at 1280×800, 375×667, and 320×568.
2. **Make skips impossible** — replace every visibility-conditional audit with a hard assertion, and add a coverage guard that fails when the audited-view set diverges from the declared set.
3. **Capture failure evidence** — attach view, theme, viewport, selector, colour pair, ratio, threshold, and a screenshot whenever an audit fails.
4. **Fix only real contrast defects surfaced by the expanded matrix** — token/role/colour changes, no layout redesign, no copy, no behaviour change.
5. **Correct the report** — reconciled test counts, honest coverage statement, final F1 ratio, and a per-view result table.

### Audit-target inventory (22)

| # | View | Scenario / entry | Test id | Status after landing |
|---|---|---|---|---|
| 1 | `OverviewView` | `running-work` → Running work | `.overview` | Covered |
| 2 | `PlanView` | `plan-view` → plan card | `plan-view` | Covered |
| 3 | `EpicView` (+ findings/watches modal) | `running-work` → epic | `epic-view` | Covered |
| 4 | `ReportsView` | `reports` | `.reports-view` | Covered |
| 5 | `CompletionReportView` | `reports` → detail | `.completion-report` | Covered, **conditional** |
| 6 | `DecisionView` | `decisions` | `.decisions-view` | Covered |
| 7 | `SystemView` | `system` | `system-view` | Covered |
| 8 | `CleanupView` | `system` → Review… | `cleanup-view` | Covered, **conditional** |
| 9 | `SettingsView` | `designer` → Machine settings | `settings-view` | Covered |
| 10 | `MachineSettingsView` | `designer` → Machine settings → button | `machine-settings-view` | Covered |
| 11 | `ProcessDesigner` | `designer` → open designer | `process-designer` | Covered |
| 12 | `ProjectExplorerView` | `running-work` → projects | `project-explorer-view` | Covered, **conditional** |
| 13 | `StartView` | `projects-and-start` (`start`) → `/start` | `start-view` | **Missing** |
| 14 | `OnboardingView` | `onboarding` / `entry-onboarding` → `/onboarding` | `onboarding-view` | **Missing** |
| 15 | `ProjectOnboardingView` | `projects-and-start` → `/projects/onboard` | `project-onboarding-view` | **Missing** |
| 16 | `PairingView` | `web-pairing` (pre-pairing) → `/pair` | `pairing-view` | **Missing** |
| 17 | `RevokedView` | `devices.spec.js` revoke-this-browser flow | `revoked-view` | **Missing** |
| 18 | `ListenerOffView` | `devices.spec.js` listener-off flow | `listener-off-view` | **Missing** |
| 19 | `EpicDecompositionView` | `decomposition` → `/epics/decompose` | `epic-decomposition` | **Missing** |
| 20 | `DecompositionReviewView` | `decomposition` → review step | `decomposition-review` | **Missing** |
| — | `EpicAdoptView` | `running-work` / `projects-and-start` PR-adopt panel | `epic-adopt` | **Missing** |
| — | `RecipeReadyView` | `projects-and-start` recipe step | `recipe-ready` | **Missing** |

> `RevokedView` and `ListenerOffView` are store-driven, not path-driven: reuse the existing `devices.spec.js` flows rather than inventing a URL. `EpicAdoptView` and `RecipeReadyView` are sub-views of `StartView`; audit them in the state that renders them.

### Non-Goals

- No visual redesign, spacing, copy, navigation, or feature change.
- No server, API, mock-scenario, operator, or designer-model change.
- No new theme and no colour-only meaning.
- No weakening of the existing 36 tests; the same assertions stay and the matrix grows.

---

## 3. Isolation & Boundaries

| Repository | Worktree | Branch | Scope |
|---|---|---|---|
| `pm-mcp-clients` | `/Users/oliver/git/worktrees/pm-mcp-clients-theme-readability-remediation` | `fix/theme-readability-remediation` | `pm-web-e2e/tests/theme-readability.spec.js`, `pm-web-e2e/tests/helpers/contrast.js`, the completion report, and only those `pm-web-app` token/view files a newly surfaced contrast failure requires. |

> **Pre-flight**: `git checkout main && git pull --rebase origin main` before creating the worktree.

**Sole-owner files**: the theme spec, the contrast helper, the completion report, and any token/view file changed to fix a newly found failure.

**No touch**: `pm-api`, `pm-mock-runtime`, `pm-mcp-server`, `pm-operator`, feature behaviour, layouts, copy, or existing scenario fixtures.

---

## 4. Workflow Checklist (conforming to [`runbook.md`](runbook.md))

- [x] 1. Isolated worktree created off latest `main` on `fix/theme-readability-remediation`.
- [x] 2. Personas and skills loaded: `plan-marshall-persona-integration-tester` with `pm-dev-frontend:jest-testing`; `plan-marshall-persona-implementer` with `pm-dev-frontend:css` if a token/colour fix is needed; `plan-marshall-persona-security-expert` for no-colour-only meaning; `plan-marshall-persona-documenter`; `plan-marshall-persona-code-reviewer`.
- [x] 3. Matrix extended and hardened; every new audit green or every real failure fixed.
- [x] 4. Completion report corrected and the per-view result table added.
- [x] 5. `./mvnw verify -Ppre-commit` passes.
- [ ] 6. PR opened without `skip-bot-review`; review windows and all bot/Sonar findings resolved.
- [ ] 7. Landed on `main`; worktree and branch cleaned up.
- [ ] 8. Re-run Step 8 verification and, on PASS, archive the report in `doc/agy/verified/`.

---

## 5. Personas & Domain Skills Matrix

| Phase | Primary persona | Supporting skills | Focus |
|---|---|---|---|
| 1: Reachability | `plan-marshall-persona-integration-tester` | `pm-dev-frontend:javascript` | Route/scenario/test-id map for the ten missing views. |
| 2: Matrix hardening | `plan-marshall-persona-integration-tester` | `pm-dev-frontend:jest-testing` | Hard assertions, coverage guard, failure attachments, third viewport. |
| 3: Contrast fixes | `plan-marshall-persona-implementer` | `pm-dev-frontend:css` | Token/role corrections only where the matrix fails. |
| 4: Evidence & report | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Corrected counts, per-view table, review triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Reachability map

- [x] **1.1** For each of the ten missing views, record route, scenario, navigation steps, and test id in the table above; reuse the existing `start.spec.js`, `onboarding.spec.js`, `projects.spec.js`, `decomposition.spec.js`, `devices.spec.js`, and `pairing.spec.js` flows rather than inventing new ones.
- [x] **1.2** Confirm each view is reachable in the shipped app, not only in a unit render; a view that cannot be reached from a real scenario is recorded as an explicit, reasoned exclusion in the design note and the report.

### Phase 2: Matrix hardening

- [x] **2.1** Replace the three `if (await …isVisible())` guards with `await expect(locator).toBeVisible()` followed by the audit. If a scenario cannot provide the state, fail with a named reason instead of skipping.
- [x] **2.2** Add `{ name: 'narrow', width: 320, height: 568 }` to `VIEWPORTS` and apply it to the reported flow and every core view.
- [x] **2.3** Add a declared-view inventory in the spec (the 20 targets and their test ids) and a coverage assertion that the set of audited views equals the declared set. A view added to `src/lib/views/` without a matrix entry must fail the suite.
- [x] **2.4** Wrap each audit so that a failure calls `testInfo.attach` with the view, theme, viewport, selector, foreground/background, ratio, threshold, and a `page.screenshot({ fullPage: true })`; keep the existing console output.
- [x] **2.5** Extend the audit to the modal/dialogue states the plan names: `StartWorkModal`, `GitSourceIntakeModal`, `RecipeSelector`, `LessonPicker`, `LessonSourceModal`, `HoldResolutionModal`, `PlanSettingsModal`, and the findings/watches modal.

### Phase 3: Run, triage, fix

- [x] **3.1** Run `npx playwright test tests/theme-readability.spec.js` against the live mock runtime in all theme × viewport cells.
- [x] **3.2** Triage every failure: a real contrast defect is fixed with the smallest semantic-token change (tokens first, view second); a false positive is fixed in the helper with a regression test, never by loosening the threshold.
- [x] **3.3** If a new token is introduced, extend `tokens.test.ts` parity/completeness and `theme.test.ts` coverage in the same commit.

### Phase 4: Report correction and evidence

- [x] **4.1** Replace the stale "20-test" claims with the actual baseline (36) and the final count after the expansion; state the coverage truthfully as "22 audit targets × 2 themes × 3 viewports".
- [x] **4.2** Correct the F1 figure to the landed dark token (`#60a5fa` on `#131b2e` = **6.75:1**) and keep the original failing `1.22:1` baseline.
- [x] **4.3** Add a per-view result table: view, light desktop/phone/narrow, dark desktop/phone/narrow, failure count. Reference the attached screenshots actually produced by the run.
- [x] **4.4** Re-run `npm test`, `npm run check`, `npm run lint`, the theme spec, the affected existing specs, and `./mvnw verify -Ppre-commit`.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **V1** | All 22 audit targets have a non-conditional contrast audit in both themes at 1280×800, 375×667, and 320×568; any exclusion is explicit and reasoned. | Spec inventory + coverage assertion + per-view result table. |
| **V2** | No audit is guarded by a visibility conditional; a missing state fails the test. | Spec diff; no `if (await …isVisible())` remains around an audit. |
| **V3** | The 320×568 viewport is exercised by the reported flow and every core view. | `VIEWPORTS` diff + green run. |
| **V4** | Every failing audit attaches a screenshot and the colour/ratio/threshold record. | Helper/test diff + a deliberately failing control run. |
| **V5** | The report's counts, coverage statement, and F1 ratio match the landed spec and tokens. | Report diff against the spec and `tokens.css`. |
| **—** | No layout, copy, navigation, behaviour, server, API, or mock change beyond a contrast token/view fix demanded by a real failure. | Diff review. |
| **—** | The existing 36 tests still pass; the full pre-commit gate is green. | CI / report. |

---

## 8. Risks & Rollback

- **Entry-flow state**: pairing/revoked/listener-off views depend on runtime state, not URLs. Reuse the existing `devices.spec.js` and `pairing.spec.js` flows; do not add mock endpoints.
- **Conditional removal surfaces failures**: that is the purpose. A long-hidden failure is fixed, not waived.
- **320px layout**: the audit measures contrast, not layout; if wrapping is genuinely broken, fix wrapping as the parent plan already did for `.project-header__actions` — no redesign.
- **Threshold drift**: never relax `4.5:1`/`3:1` to make a view pass; fix the token pairing.
- **Rollback**: revert the matrix commit; the production token/colour fixes from PR #210/#211 stay untouched unless they caused a regression.

---

## 9. Scheduling

Stream I (Web App Theme Readability) in [`README.md`](README.md). This plan is the gate between the landed implementation and the archival of `theme-readability-light-dark-plan-report.md`; the report stays in `doc/agy/` until this remediation passes Step 8.

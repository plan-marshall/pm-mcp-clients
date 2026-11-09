# Landing Analysis: PLAN-109 — Web: Rail "Settings" Label, Cog Icon, and "Plan settings"

epic: ui-improvements
workstream: WS-03
pr: #367 (https://github.com/plan-marshall/pm-mcp-clients/pull/367), merged as `56b43a02` (squash; PR head `3b02d393`, base `157ef52d`)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-109-web-rail-settings-label-and-icon-001.md`, 2026-11-08) after verifying the lane's claims against git,
> the PR (read through the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Web label and cog icon | shipped-as-specified (one spec premise wrong, see layer 1) | `common.{de,en}.properties:100` Einstellungen / Settings, `:101` `.title` unchanged; `nav-settings.svg` is a 24×24 cog that meets `uiIcons.test.ts:56-90`; new `NavRail.test.ts:35,48`; e2e locators `designer.spec.js:19,135`, `showroom.spec.js:171`, `workspace.spec.js:63`, `theme-readability.spec.js:159`; `onboarding-texts.adoc:91` row |
| D2 `control.plan.settings` → Plan-Einstellungen / Plan settings | shipped-modified: the German value is untested | `control.{de,en}.properties:16`; `ActionMenusTest.java:58,78,105`, `ActionsFullScreenTest.java:59,64`, `WorkControlPanel.test.ts:102` assert the English text; `byState.ts` six sites keep the key; no test asserts `Plan-Einstellungen` |

All 14 changed files are explained by a deliverable. The two files outside the Expected Surface (`NavRail.test.ts`, `WorkControlPanel.test.ts`) are the tests D1 and D2 ask for.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`) — reason: every full ship gets its own leaf; reading the merged diff of 14 files and the catalogue consumers is that depth.
- **Verdict:** layer 1 findings; layer 2 findings; layer 3 clean.
- **Re-read by the orchestrator at `56b43a02`:** `NavRail.svelte:117`, the absence of any `web.frame.settings.title` reader in `pm-web-app/src`, `BottomBar.svelte:158`, `NavRail.test.ts:35,48`, `control.de.properties:16`, the absence of `Plan-Einstellungen` in every test file, `WorkControlPanel.test.ts:102`, `ActionMenusTest.java:58,78,105`, the `LABELS.` uses of `theme-readability.spec.js` (263, 338, 399, 419), the SVG root, and the three PR commits. All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | Objective / D1: "the tooltip `web.frame.settings.title` keeps the long form" | checked | refutes | `NavRail.svelte:117` sets `title={collapsed ? $t(item.labelKey) : undefined}`; no code reads `web.frame.settings.title`, so the collapsed tooltip is now the short label |
| L1-2 | Claim 2: `web.frame.settings` has no consumer outside the rail and `onboarding-texts.adoc` | checked | refutes | `BottomBar.svelte:158` renders it in the phone More sheet (harmless). Persisted as `contradicted` on claim 2 |
| L1-3 | D1 Done-when lists `theme-readability.spec.js` `LABELS.settings` as a site | checked | refutes | the entry is never read (`LABELS.` used at 263, 338, 399, 419 only); edited but not exercised |
| L1-4 | Claim 4 (HYPOTHESIS): `/Settings/i` in `devices.spec.js:20` / `onboarding.spec.js:178` clashes under strict mode | not_shown_by_reading | none | needs a Playwright run; the lane reports it refuted by run |

**Layer 1 verdict:** findings — 3 (premise errors, none blocks the landing); not shown by reading — 1

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 label | `common.{de,en}.properties:100` | `NavRail.test.ts:35,48` (exact role name, real catalogue) | none | checked |
| D1 icon | `nav-settings.svg` | `uiIcons.test.ts:56-90,92` | provenance: the path matches the Lucide "settings" cog (ISC); no licence notice in the repo | checked |
| D1 e2e locators | the five sites above | the e2e specs (run-only, not in CI) | `getByRole` name `'Settings'` without `exact: true` is a substring match | not_shown_by_reading |
| D2 English | `control.en.properties:16` | `ActionMenusTest`, `ActionsFullScreenTest`, `WorkControlPanel.test.ts:102` | none | checked |
| D2 German | `control.de.properties:16` | **none** — a revert to `Einstellungen` passes every test (CodeRabbit nitpick, confirmed) | — | checked |
| Lane claim "test-first" | — | — | not borne out by the history: `4936c9f3` and `37a70b16` each carry production and tests together | not_shown_by_reading |

**Layer 2 verdict:** findings — 2 (German plan label untested; test-first order not shown); not shown by reading — browser check, builds, e2e strict mode

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| none | — | — | — | — |

**Layer 3 not shown by reading:** the rendered cog through the CSS mask and its distinctness from the theme choice (browser); strict-mode safety of the substring `Settings` locators (Playwright run).

**Layer 3 verdict:** clean

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | JUnit 5, @DisplayName, no Mockito/Hamcrest | conforms | string changes in existing JUnit 5 tests; DisplayNames updated in `3b02d393` |
| CLAUDE.md § Code Standards | Generators for input, contract text literal | conforms | only expected catalogue text added |
| CLAUDE.md § Web Standards | ES modules, no innerHTML/{@html}, no timer waits | conforms | `NavRail.test.ts`; locator-only e2e edits; static SVG |
| Spec Standing Constraints | every changed text in de and en | conforms | both keys in both catalogues |
| Spec Done-when | German plan-act text tested | deviates | `control.de.properties:16` has no value assertion |
| Spec Done-when | tooltip keeps the long form | deviates | premise error L1-1, not a regression |

## Metrics and Anomalies

- Tokens: `unknown` (antigravity harness measures none); `model` and `effort` also `unknown`.
- Duration: not reported.
- Anomalies: landing facts incomplete (`missing_keys` `total_tokens`, `model`, `effort`) — folded into the standing Open Defect. The facts block's `plugin_version=0.1.0` against `executor_version=0.1.2080` looks like a placeholder.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed head `3b02d393` (two reviews, 23:19Z and 23:31Z). One thread (`PRRT_kwDOVBOtss607qje`, narrow-width clipping) answered and resolved. **The first review body's nitpick (assert `Plan-Einstellungen`) was NOT answered** — the lane's report counts one comment only. Standing merge rule (all review bodies answered) not met; the nitpick is confirmed by reading and routed to PLAN-79. Sourcery skipped on quota. Review threads 1, resolved 1; review bodies 2 (one carrying 1 actionable + 1 nitpick).
- CI/merge: all 20 checks green; merged through the merge queue as `56b43a02`; no rebase conflict reported. PLAN-116 and PLAN-118 ran alongside without a reported collision.
- Sonar: lane `step.sonar-roundtrip.open_issue_count=0`, `open_issue_rules=none`. The REST read after merge is `undecidable` (`head_revision_unavailable`), analysis revision `3b02d393` = PR head; the SonarCloud quality-gate comment on the PR reports **0 new issues**. Consistent with the lane.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-109 --status shipped`
- [x] row `pr` stamped — `queue --set-row PLAN-109 --field pr --value 367`
- [x] row `landing` stamped — `queue --set-row PLAN-109 --field landing --value landings/PLAN-109.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-109-web-rail-settings-label-and-icon` (the sender's lane name)
- [x] claim 2 of the spec stamped `contradicted` (`corpus set-verdict`, producer `ui-improvements/analyze`)
- [x] epic.md: incomplete-landing Open Defect recurrence; Watch for the PLAN-109 follow-ups
- [x] nitpick folded into PLAN-79 (surface unchanged: `pm-operator/src/test/java/` already claimed)
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- German `Plan-Einstellungen` value assertion → folded into PLAN-79 (cui-testing-operator).
- Tooltip long form (`web.frame.settings.title` unused, collapsed rail shows the short label) → Watch; a design question for the settings owner PLAN-60 (parked) or D2 navigation.
- Substring `'Settings'` e2e locators (no `exact: true`) and the dead `LABELS.settings` → Watch; Playwright is not in CI.
- Cog path matches Lucide (ISC), no provenance recorded → Watch; the icon-licence decision is open in `doc/claude_review`.

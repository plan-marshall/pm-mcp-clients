# Landing Analysis: PLAN-143 — Web: Addresses, History and the Remembered Place (D2 Navigation Engine, Part 2)

epic: ui-improvements
workstream: WS-02
pr: #446 (merge commit `71f84bb03ac0edfe49f617d157464ff79958ba69`, merged 2026-11-15T20:23:49Z through the merge queue; head at merge `0651b55e`; 34 files, +2395/−203)

> Landing record for one shipped plan. Written by the `analyze` verb from inbox message `rb-143-web-addresses-and-remembered-place-003` (kind `landing`, 2026-11-15T21:54Z) after verifying its claims against git, the CI abstraction, the lane's evidence set and the merged code. The message is a lead, not a fact.

## Deliverable Fidelity vs Spec

The lane reports **0 of 5 met as written**, and the reading agrees: no deliverable is fully met. The address model, the one writer, the remembered place, the new route pages and an e2e spec are on `main`; each deliverable has a gap that its Done-when names.

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The address model | shipped-modified (partly met) | `routes.ts` parses and serialises the owned routes and the typed query vocabulary canonically, `routes.test.ts:177-290`; the `/projects/onboard` round trip cannot hold (`routes.ts:263` fixed path before the project pattern at `:310`, so a project named `onboard` parses back as the onboarding), and `routes.test.ts:285` asserts only the written string |
| D2 One writer, push and replace rule | shipped-modified (not met) | `history.ts` holds the pure rules; `fallBackFromDeletedObject` (`runtime.ts:903`) and `switchTab` (`history.ts:247`) have no production caller; no function closes or replaces an `open=` object; `layout.test.ts` is not in the diff; `history.test.ts:61` parses strings and never drives popstate; `dropCodeFragment` (`runtime.ts:385`) writes `history.replaceState` itself, so the writer is not the only writer |
| D3 The remembered place and its entry points | shipped-modified (partly met) | start rule fills a mark-less `/` and bare `/overview` by replace, the rail click pushes, mode is not stored (`runtime.ts:722-745`, `rememberedPlace.test.ts:45`); `factsOf` sets `planEnded` false while the project runs and reads no plan liveness (`runtime.ts:481-492`), so the ended-plan fall-back is unit-test only; a removed project counts as running (`:486`); the Running-work click ignores the remembered grouping and plan (`rememberedPlace.ts:225`) |
| D4 The existing views read and write the scheme | shipped-modified (not met) | `ProjectExplorerView.svelte` is untouched and still reads `window.location.search` `?project=` (`:86`), so `/projects/{id}` no longer focuses a project; `DecisionView.svelte`, `PlanView.svelte` and their tests, `decisions.spec.js` and `projects.spec.js` are untouched; `OverviewView` reads the store only in `onMount` (`:229`) |
| D5 Browser evidence | shipped-modified (partly met) | `navigation.spec.js` exists and uses no timer; the marked-reload case runs with an empty place (`:114`) and the never-fill case never remembers a plan (`:123`), so both cannot fail; the tab-switch case only loads a URL (`:132-135`); the Playwright MCP browser check is not shown by reading |

Added unplanned (knock-ons of the D1 and D4 address change, outside the declared Expected Surface): `shell/nav.ts`, `components/WorkControlPanel.test.ts`, `views/EpicView.test.ts`, `chrome.spec.js`, `epic-view.spec.js`. Expected Surface files **not** touched by the diff: `DecisionView.svelte`, `ProjectExplorerView.svelte`, `PlanView.svelte` and their tests, `routes/layout.test.ts`, `decisions.spec.js`, `projects.spec.js`. Existing e2e assertions were loosened to `/reports(/[^/?]+)?` (`reports.spec.js:159`, `overview.spec.js:168`), so they no longer prove the `/reports/{id}` address.

## Three-Layer Analysis

The landing was read on three layers by one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`), and the orchestrator re-read the points recorded as defects below at their position on `71f84bb0`; the points it did not re-read are listed as unverified leads. The reading is by reading: no build, no test run, no write.

- **Dispatch:** one read-only leaf per landing — reason: reading 34 merged files against five deliverables is the depth the dispatch rule exists for, and the landing's own read must not share context with another.
- **Verdict:** layer 1 findings (refuted premises); layer 2 findings; layer 3 findings, including major defects.

Re-read by the orchestrator at `71f84bb0`: L1-1 (`routes.ts:263` against `:310`), L2-3 (`runtime.ts:903` only definition, no caller, no test), L2-4 (`history.ts:247` `switchTab` only defined and unit-tested), L2-7 (`runtime.ts:481-492`), L2-9 (`ProjectExplorerView.svelte:86` and the file absent from the diff), L3-3 (`ReportsView.svelte:404-410`, the requested report is looked up in the first loaded page only), L3-1 (`rememberedPlace.ts:205-212` `return present()` without a place). Not re-read, recorded as unverified leads: the minor layer-3 points on push-versus-replace, `openedFromOutside`, the brand click, `addressOfState` and the e2e vacuity claims.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | Spec D1 contradicts itself: `/projects/onboard` stays the onboarding route yet a project called `onboard` must be an object, so the round trip the Done-when demands cannot hold | checked (re-read) | refutes | `routes.ts:263`, `:310` |
| L1-2 | Spec D1: an epic called `decompose` is an object while `/epics/decompose` is also an old address replaced with `/decomposition`; one address has two meanings | checked | refutes | `routes.ts:307`, `:686`, `routes.test.ts:283,296` |
| L1-3 | Spec D5's Done-when needs a switch from `tab=changes` to Process dropping `file=`, but `main` has no tab control (PLAN-55/58/62 own it); unachievable as written | checked | refutes | `navigation.spec.js:132-135` |
| L1-4 | Spec D4 says each view keeps reading the store field it reads today; `ProjectExplorerView` reads no store field but `window.location.search`, so the premise is false for that view | checked (re-read) | refutes | `ProjectExplorerView.svelte:86` |
| L1-5 | Spec D1 says `/reports?plan=` is replaced when opened but names no replacement; the report id is not in the old address | checked | refutes | `routes.ts:693` |
| L1-6 | Spec D3 requires fills at `/` and `/?lens=waits` while `/` is registered only by PLAN-49; the fill at `/` is unobservable | checked | refutes | `runtime.ts:368` |
| L1-7 | The spec's premises about `landingRoute`, `syncUrl` and the caller stack held at the parent (`landingRoute` became `startRouteFor`, `syncUrl` is gone) | checked | confirms | `runtime.ts:460` |
| L1-8 | The bottom bar's click is deferred to PLAN-144, consistent with the spec | checked | confirms | `BottomBar.svelte:100` |
| L1-9 | The Playwright MCP browser check in German and English cannot be shown by reading | not_shown_by_reading | none | `navigation.spec.js:1` |

**Layer 1 verdict:** findings — 6 refuting (L1-1 to L1-6), 2 confirming, 1 not shown by reading.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `routes.ts:522` (canonical parse and serialise; node and file follow the tab rule `:543-551`) | `routes.test.ts:177-290`; `:285` cannot fail on the collision | none | checked, partly met |
| D2 | `runtime.ts:903` no caller; `history.ts:247` no caller; `runtime.ts:1245` no `open=` close or replace; `runtime.ts:385` second writer | `history.test.ts:61` parse only, `:51` never asserts the hash; `layout.test.ts` not in the diff | `runtime.ts:385` | checked, not met |
| D3 | `rememberedPlace.ts:198`, `runtime.ts:481-492` | `rememberedPlace.test.ts:63-83` injects `planEnded`; the fall-back is not live | `runtime.ts:486`, `rememberedPlace.ts:225` | checked, partly met |
| D4 | `OverviewView.svelte:208`; `ProjectExplorerView.svelte:86` untouched | no view test asserts selection from the address; `decisions.spec.js`, `projects.spec.js` untouched | views absent from the diff | checked, not met |
| D5 | `navigation.spec.js` | `:114` and `:123` vacuous; `reports.spec.js:159` loosened | five knock-on files | checked, partly met |

**Layer 2 verdict:** findings — 15 (3 major: D2 fall-back, `switchTab`, `open=`; D3 ended-plan fall-back; D4 `ProjectExplorerView`).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 (re-read) | `rememberedPlace.ts:198-212` | a marked reload of the bare lens list, a typed `/overview`, or the Running work click without a place, on a machine with no running or waiting work | `startAddress` returns `present()`, the address becomes `/projects`; `landingRoute` returned the requested route before | major |
| L3-2 | `OverviewView.svelte:229` | Running work click while Ended Work is open; Back and Forward between two overview addresses | the view reads lens fields only in `onMount`, so the bar changes and the view does not | major |
| L3-3 (re-read) | `ReportsView.svelte:404`, `:750` | `/reports/{id}` for a report outside the loaded first page | the view stays on the list with no notice; `showDetail` picks the first row of the plan, so a second report of one plan is rewritten to the first report's id | major |
| L3-4 | `ReportsView.svelte:758`, `:370` | open a detail from the list, press Back | list to detail is written as a replace, so Back leaves Reports | minor |
| L3-5 | `+layout.svelte:224`, `routes.ts:693` | an old `/reports?plan=P` link | replaced with `/reports`, the named detail is not opened | minor |
| L3-6 | `+layout.svelte:227` | reload or deep link to an epic called `decompose` | rewritten to `/decomposition` | minor |
| L3-7 | `EpicDecompositionView.svelte:47` | `/decomposition` with no epic in the store, fresh session | renders nothing (residue 3) | minor |
| L3-8 | `history.ts:55`, `runtime.ts:295` | `initRuntime` again on reconnect at a marked bare `/overview` | filled from the place; how often only a run could show | minor |
| L3-9 | `runtime.ts:537` | the brand click | start route shown by replace, Back skips the page left | minor |
| L3-10 | `runtime.ts:1209` | `parentOf` of a plan or task | loses the lens scope, falls back to the bare overview | minor |
| L3-11 | `+layout.svelte:277` | `/?lens=waits` | rewritten to `/overview` (pre-PLAN-49), the lens address does not survive a load | info |
| L3-12 | `runtime.ts:1514` | a remembered plan or epic deleted since | no live-list validation at fill time; depends on residue 1 and the unwired fall-back | minor |
| L3-13 | `PlanIntentPanel.svelte:49` | in-app link | still writes the old `/decisions?decision=` address, replaced only on a full load | info |

**Layer 3 not shown by reading:** residue 7 (the review gate's parser, confirmed separately below), residue 8 (Sonar re-analysis of the merge commit, the 7 e2e failures in `data-table`, `intent` and `onboarding` specs that `main` carries), popstate adoption order with the layout effect, real focus after a fill, German and English text, Prettier formatting of `OverviewView.svelte:365` and `ReportsView.svelte:1165`, and vitest and Playwright results.

**Layer 3 verdict:** findings — 13 (3 major re-read or leaf-checked, 8 minor, 2 info); layer 3 is reported to the operator at once and nothing is fixed inline.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web Standards | ES modules only | conforms | new files under `lib/navigation` and the new route pages use `import`/`export` |
| CLAUDE.md Web Standards | no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}` | conforms | none in the new navigation files, route pages or `routes.ts` |
| CLAUDE.md Web Standards | no arbitrary timer wait in a test | conforms | `navigation.spec.js` and the new unit tests wait on `toHaveURL` and the DOM |
| CLAUDE.md Web Standards | flat ESLint config, the npm scripts stay | conforms | no `.eslintrc`, no `package.json` or `eslint.config.js` in the diff |
| CLAUDE.md Code Standards | Java rules where Java is touched | conforms | no Java touched; `web.nav.gone` added to the two message properties files |
| CLAUDE.md Code Standards | every package documented | not_shown_by_reading | `navigation/` is new; a doc review was not run |
| pm-dev-frontend javascript | one writer of the address | deviates | `runtime.ts:385` calls `history.replaceState` with a literal `pmApp` key beside `history.ts` |
| pm-dev-frontend javascript | views react to the store, not only at mount | deviates | `OverviewView.svelte:208-232`; `ProjectExplorerView.svelte:86` |
| pm-dev-frontend jest-testing | a test can fail on the behaviour it names | deviates | `navigation.spec.js:114`, `:123`; `history.test.ts:61`; `routes.test.ts:285` |
| pm-dev-frontend lint-config | Prettier formatting of changed sources | not_shown_by_reading | `format:check` not run |

## Metrics and Anomalies

- Tokens: `unknown` (the lane reports `total_tokens=unknown`, `plugin_version=unknown`; `inbox landing-check` answers `complete: false`, `missing_keys` `total_tokens`, `plugin_version`). Recorded as unobserved, not as zero.
- Harness and model: `opencode`, `deepseek-v4.1-flash/high`; `executor_version` 0.1.2133 (the executor was behind the lane for a time and has since been regenerated to 0.1.2135).
- Duration: PR created before 2026-11-15 and merged 2026-11-15T20:23:49Z; the lane's landing message arrived 21:54Z, after the merge.
- Anomalies: `self-review` ended `budget-exhausted`; `deliverables_done=0` of 5 reported by the lane itself while the PR merged on the operator's instruction; the lane edited two test files in the main checkout at the start of D1 and restored them; the lane used read-class shell calls (`grep`, `tail`, `ls`, `awk`) it recorded as a rule break; two early e2e runs went to the default ports; `guard: none on this harness (unenforced)`.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `10930a04` and `f65e1341`; all five inline threads replied to and resolved. The orchestrator re-read the gate on the merged PR: `runbook_lane review-gate --pr-number 446` answers `undecidable_read` on the counted title `Minor comments: 22` (confirmed, no `minor comments` label in `runbook_lane.py:2120-2130`), so `unresolved_review_threads` and `review_body_findings_total` cannot be reproduced by the gate. The landing's `review_body_findings_total=31` and `handled=5` are therefore **not re-verified**: the lane's progress file (`.plan/temp/rb-143-web-addresses-and-remembered-place/progress.md`) holds no `finding:` line, so the handled number has no disposition record to read. Not a false claim, an unobserved one. The 22 minor and 2 nitpick findings are therefore open and unhandled by any record.
- CI/merge: merged through the merge queue on the operator's explicit instruction at `71f84bb0`; the lane reports the newest merge-group run green and its own build and e2e green at `10930a04` (414 passed); after merging `main` its local e2e showed 410 passed and 7 failed in specs the plan does not touch. Not re-run here.
- Sonar: the landing says `sonar_open_new_issues=0` with the last read at `f65e1341` and the head moved twice afterwards. The orchestrator's read: `sonar read_open_issues --pr 446` answers `undecidable` (`head_revision_unavailable`, analysis revision `0651b55e` = the merged head), and a Sonar MCP search for pull request 446 finds **no issues**. So 0 is corroborated at the merged head by the search, with the dedicated read undecidable because the PR has merged; `step.sonar-roundtrip.open_issue_rules` is `none`.
- Collisions: the lane merged `main` (`0651b55e`) late because #446 was `CONFLICTING` with main (PLAN-142 and PLAN-149 landed meanwhile); recorded for the pairing decision, the overlap was in `runtime.ts`, `routes.ts` and the views.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-143 --status shipped`
- [x] row `pr` stamped — `orchestrator queue --set-row PLAN-143 --field pr --value 446`
- [x] row `landing` stamped — `orchestrator queue --set-row PLAN-143 --field landing --value landings/PLAN-143.md`
- [x] row `plan_marshall_plan_id` stamped — `orchestrator queue --set-row PLAN-143 --field plan_marshall_plan_id --value rb-143-web-addresses-and-remembered-place`
- [x] epic.md narrative reconciled: the two Watches written for the mid-flight report are folded into this landing; one Open Defect opened for the PLAN-143 landing (layer-3 and the incomplete landing facts)
- [x] `inbox landing-check` answered `complete: false`, recorded as an Open Defect and in the decision log
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

None staged (the operator's word is needed to stage a layer-3 fix plan). Named, with their owners:

1. **A PLAN-143 fix plan** for D2 (the unwired deleted-object fall-back, `switchTab`, `open=` close and replace, the second history writer), D4 (`ProjectExplorerView` and the views and tests the spec lists but the diff left untouched) and the three major layer-3 defects (L3-1, L3-2, L3-3). It shares `runtime.ts`, `routes.ts`, `ReportsView.svelte`, `OverviewView.svelte` and `ProjectExplorerView.svelte` with **PLAN-166** (running), **PLAN-146** (running) and **PLAN-144** (staged), so it follows them. The operator decides whether to stage it, and as one plan or as residue folded into PLAN-144.
2. Residue 1 (`factsOf` liveness) and 4 (`fallBackFromDeletedObject` caller): inside that fix plan.
3. Residue 2 (`/reports/{id}` outside the first page) and 3 (`/decomposition` blank): L3-3 above; the decomposition entry belongs to the decomposition plans (PLAN-155 and PLAN-137).
4. Residue 5 (the 22 minor and 2 nitpick findings, the weak `history.test.ts` and `file=` cases): the `file=` case stays blocked on the plan page's tab control (PLAN-55 and PLAN-58); the rest goes to the fix plan.
5. Residue 6 (`/projects/onboard` ambiguity): a spec decision for PLAN-59 or the fix plan, since a project called `onboard` needs another spelling or the onboarding route another path.
6. Residue 7 (the gate's parser): a runbook-lane defect in the `plan-marshall` repository, already recorded in `epic.md` Decisions; a PR with a CodeRabbit "Minor comments" body needs the operator's explicit merge word until the label is added.
7. Residue 8 (the 7 failing e2e specs `main` carries): not read by this analysis; worth a read on `main`.
8. Spec corrections that this landing refutes (L1-1 to L1-6) belong in the fix plan's spec, not in the shipped PLAN-143 spec.

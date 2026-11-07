# Landing analysis: PLAN-111 web-drop-layout-modes

epic: ui-improvements
workstream: WS-03
pr: 363

PR #363 `bb30d71e` (merged 2026-11-07T14:03:54Z by the operator; final head `69d4e91c`, test-only; 10 files, +157/-511; base `ddcab793`). A DIRECT Claude Code session (operator-started, worktree `111-web-drop-layout-modes`, model claude-opus-5-5; tokens, effort `unknown`): landing message `inbox/plan-111-web-drop-layout-modes-001.md` (`deliverables_total=1`, `deliverables_done=0`: the browser step is blocked, honestly stated). Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); thread count, Sonar status, the doc lines and the spec references were re-read by the orchestrator (marked V*).

## Merge rule

SATISFIED. CodeRabbit reviewed `c3eb67de` (review `5811166369`, 08:55Z: one actionable item, no nitpick, follow-up or outside-diff section): a palette request made while the list was reloading was used up without scrolling. Fixed test-first in `793356e1`, replied in the thread (08:57Z), resolved by the bot; an empty confirmation review followed on `793356e1`. The final head `69d4e91c` (test-only, two Sonar smells) has no CodeRabbit review: fine under the one-review rule. Sourcery skipped (budget).

- Unresolved threads at the merge head (V*: GraphQL): 0 of 1. Body items 1/1 handled (the message does not state the lines by name).
- SonarCloud (V*: check run SUCCESS; bot comment 09:30Z on `69d4e91c`): Quality Gate passed, 0 new issues, 0 accepted; rules none. The two S5906 smells in the new test were fixed in `69d4e91c`, not dismissed.
- CI: Maven build, integration tests and dependency review green on the head; the merge-queue run on `ddcab793` and the post-merge run on `bb30d71e` green; the post-merge "Scorecard analysis" check failed (not examined: standing?). Playwright is not run in CI (`skipPlaywrightTests=true`): the decisions e2e (10/10, ports 21100/21101) is the session's local run only.
- Waiver: the BROWSER CHECK (de/en, light/dark, 1440×900 and 390×844, before and after) was not run (Claude in Chrome never connected) and the operator merged without it. Recorded, not judged. Unverified in a real browser: `scroll-margin-top`, the primary left border, the real scroll position (jsdom proves the behaviour with `scrollIntoView` mocked).

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| Grep of `pm_view_layout`, `ViewLayoutToggle`, `viewLayout`, `createViewLayout`, `VIEW_LAYOUTS`, `layout-compact`, `layout-split`, `decisions-split` empty in the three trees | MET (no output at `bb30d71e`; all seven `common.layout.*` keys and `decisions.split.select` gone from en and de; the common header comment now reads "Filter controls") | two absence asserts remain (`ReportsView.test.ts:650-651`, `reports.spec.js:309`): vacuous now, the spec allowed them |
| Palette deep-link test with no layout storage (test-first) | MET (`DecisionView.test.ts` "scrolls to and marks the decision in the single column": storage length 0 before and after, `aria-current="true"`, `decision-card--selected`, `scrollIntoView` called) | sound; the test and the code share `c3eb67de`, so the failing run is not visible in history |
| `npm run quality`, decisions e2e | session claim (1321 tests in the PR body, 1322 in the message) | `decisions.spec.js:217-235` checks the header has no buttons and the card is in `.decisions-list`; no e2e covers the deep-link |
| Browser check | NOT MET (waived by the operator) | |
| Sonar read at the head | MET (0 open, rules none) | |

Deep-link behaviour (V, `DecisionView.svelte` at `bb30d71e`): clears a hiding filter (`query=''` when the decision is not in `matchingDecisions`), then after `tick()` calls `scrollIntoView({block:'start'})` on the `[data-decision-id]` card, marks it with `aria-current="true"` and `.decision-card--selected` (inset 3px primary border), drops a mark that a filter hides; the request effect returns early while `isLoading` (the race fix). Each of the four tests fails by reading when its branch is reverted. No timer wait, no `{@html}`.

## Defects (at `bb30d71e`)

1. MED V design-vs-code drift that no plan owns: `doc/design/web-app/needs-and-find.adoc:57` still says the decision "is selected and its detail pane shows it rather than the placeholder"; PLAN-113 lists only `needs-and-find.adoc:29`. The `view-density-and-filtering.adoc` "Layout Modes" section (~46-73, the rollback paragraph ~151) and `web-app/README.adoc:19` still describe the modes (D2's text, not a defect of this PR).
2. LOW V two vacuous absence asserts (above).
3. LOW L the mark (`selectedDecisionId`) is not cleared when the route stops naming a decision; dropped only when a filter hides the card.
4. LOW V pre-existing prettier drift is still there: `DecisionView.svelte` (lines 833, 1068), `DecisionView.test.ts` (187, 526, 591), `pm-web-e2e` `port-guard.spec.js` (2, 24), `theme-readability.spec.js` (1159). `pm-web-e2e` `quality` runs `format:check`, so it would fail; `pm-web-app` `quality` does not.
5. LOW V the spec's ports (21100/21101) were one block below the `AGENTS.md` formula (21110/21111): my staging error, found by the session, corrected in PLAN-109; the session used the spec ports, which were free.
6. Tool finding V (not this epic): `sonar_rest search` calls `/api/issues/search` with no `resolved=false` filter, so it lists fixed issues; `sonar.py` `read_open_issues` filters correctly. The defect is in the plan-marshall plugin; the SonarCloud check and the MCP search read 0.

## Scope and interplay

No file outside the Expected Surface (V: the 10 files are exactly the surface). PLAN-106 interplay: the PR branch was never rebased on `d7ee5789`, but it touches nothing under `src/routes`; `+layout.svelte` and `layout.test.ts` hold no reference to the layout store; the merge-queue CI on the merged tree is green: the "merge main after `d7ee5789`" note was moot.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 363; `landing` = `landings/PLAN-111.md`; `plan_marshall_plan_id` = `111-web-drop-layout-modes`
- [x] inbox message archived; PLAN-113, PLAN-51, PLAN-107 amended; epic.md Decisions and Open Defects; anchor; view; ledger committed

## Follow-Ups

- **PLAN-113 (D2):** also `web-app/needs-and-find.adoc:57` ("scrolled to and marked, a hiding filter cleared"), the `view-density-and-filtering.adoc` layout-modes section and `web-app/README.adoc:19`; the `aria-current` + primary-left-border text of ~:64 now matches the single column.
- **PLAN-51 (parked):** drop `viewLayout.ts`/`ViewLayoutToggle.svelte` from its surface (deleted); `DecisionView.svelte` has no detail pane now (`selectedDecisionId` only marks a card); reuse that mark for its next-decision and `RowLink` work; re-ground line numbers (the file lost ~225 lines); the DecisionView prettier drift (defect 4) goes here; the browser check of `/decisions` (mark and scroll at 390×844, de/en, light/dark) joins its browser clause.
- **PLAN-107:** the vacuous absence asserts (defect 2) and the `pm-web-e2e` prettier drift (defect 4).
- **Plugin:** `sonar_rest search` missing `resolved=false` is a plan-marshall plugin finding (the operator may file it there).

# Landing analysis: PLAN-89 web-palette-hardening

PR #305 `730380ee` (squash-merged 2026-11-02T15:17Z by the operator; head `f42aac19`, same tree). Lane `rb-89-web-palette-hardening`: NO landing message exists (the lane filed none before or after the merge); the PR body and comments are the only lane account. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run), claims verified by the orchestrator against the review body and the code.

## Merge rule

CodeRabbit reviewed two heads: `4bf47513` (11:26Z) and `2bbe1206` (14:15Z). The first review's five findings were all handled (the operator's comment of 11:43Z; fixes in `9e2b5755`; one answered with a reason). **The second review is OUTSIDE the rule: its three findings are in the review BODY (no inline thread), no commit followed and no comment answers them** (verified: no issue comment after 14:15Z except the Sonar gate; `CommandPalette.test.ts:71-84` unchanged). The operator's re-trigger at 13:37Z was rate-limited; the review of 14:15Z answered an earlier trigger. Sonar: the quality gate comment at 15:04Z shows 0 new issues (the orchestrator did not re-read the project; a lane account).

## Three layers

| Deliverable | Specified | Implemented | Correct |
|---|---|---|---|
| D1 listbox owns only options | new `CommandPalette.test.ts`, four states, children, group name, `aria-expanded`, activedescendant, scroll | MET (`CommandPalette.svelte:145,399,425-433,466-502`; test file) | yes; the scroll test never asserts that `aria-activedescendant` changed |
| D1.2 browser check | de/en, light/dark, 1440/390 | PARTLY: the PR body records 1440 x 900 en only | not verifiable |
| D2 tests that prove the PR 296 findings | filter test, three chrome.spec cases | MET (`DecisionView.test.ts`, `chrome.spec.js`) | the Tab walk does not walk Close, field, Retry |
| D3 design and texts follow the code | behaviours stated, no uncited key | MET (`web-app/needs-and-find.adoc:47-57`, the `overview.find.plain` citation removed) | yes |
| D4 axe audit opens the palette | invalid children at the base, none after | MET in code (`a11y.js` `include`, `theme-readability.spec.js` 4 states x 1440/390 x de/en) | the red run at the base is a log claim only |
| D5 the three PLAN-37 leads | measure, fix or refute | (a) row menu: measured 318 px name in a 60 px cell, fixed with the spec's remedy (`visibleLabel`, `IconButton.svelte:22,38,58`, `DataTable.svelte:121`); (b) ChatPanel Escape guard: refuted in a browser, code left; (c) opener focus: Chromium only, Safari/Firefox recorded as not checked | (a) PARTLY: the operator's own measurement is 102 px in a 60 px cell, a 50 px overhang that remains; no test measures the fit |

All 12 changed files are declared; nothing beyond the Expected Surface.

## Leads (read, not run)

1. Row-menu overhang remains (`DataTable.svelte:121`, column 60 px under `table-layout: fixed`): answered, not fixed, recorded as residue.
2. Decisions are opened by `match.id` only (`CommandPalette.svelte:223-225`; `matchKey` is `kind:id`): the PR 296 body finding, still open (PLAN-93 D5 d).
3. Unhandled CodeRabbit review `2bbe1206`: (a) the group helper accepts a button or other interactive element inside a group (`CommandPalette.test.ts:71-84`); (b) the failed-read test never clicks Retry, so removing `onclick={retry}` still passes (`:157-168`); (c) option ids are not asserted unique (`:170-193`).
4. The Tab walk through Retry is untested; the stale docblock `IconButton.svelte:16` (label "accessible name and visible label").
5. Browser evidence, the test-first red/green runs and the seven pre-existing e2e failure names are lane claims.

## Routing

Findings 2, 3, 4 and the scroll-test weakness: PLAN-93 D5 (d), which edits the same file. Lead 1 (the fit of the row menu): PLAN-96 (web acts residue).

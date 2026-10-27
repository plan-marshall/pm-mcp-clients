# Landing Analysis: PLAN-04 — Web: Theme Remembered and Tab Titles

epic: ui-improvements
workstream: WS-02
pr: #231 (https://github.com/plan-marshall/pm-mcp-clients/pull/231), merged as `099a3b40`

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message
> `rb-04-web-theme-and-titles-001.md`) after checking the message against the PR, the merge diff
> `1864d8a3..099a3b40` and the code on `main`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Theme remembered | shipped-as-specified | `pm-web-app/src/lib/theme.ts` (+132) with `theme.test.ts` (+173), the pre-paint script in `src/app.html` (+29), the radio group in `routes/+layout.svelte`, the e2e case in `pm-web-e2e/tests/chrome.spec.js`. The browser check is recorded with before and after screenshots under `.plan/temp/playwright-mcp/04-web-theme-and-titles-*` |
| D2 Titles with arguments | shipped-as-specified | `pm-web-app/src/lib/routes.ts` (+103) with `routeTitles.test.ts` (+110), the title keys in `plan_view`, `epic_view`, `start` and `common` (de/en). Sonar read at `0d28aa1`: confirmed `0` |

Beyond the declared surface: `theme.ts`/`theme.test.ts`, `routeTitles.test.ts`, the `plan_view`/`epic_view`/`start` message areas (the keys live there, not in `common`), `pm-web-e2e/tests/workspace.spec.js` (the removed toggle's old test), and five design docs (`shared/plan-view.adoc`, `shared/starting-work-texts.adoc`, `web-app/README.adoc`, `interaction-affordances.adoc`, `theme-readability.adoc`). None of these touches the surface of the plan still running (PLAN-01: `pm-web-e2e/tests/helpers/textGuard.js`, `theme-readability.spec.js`, `text-guard-baseline.json`).

## Metrics and Anomalies

- Tokens: `unknown`; the opencode harness measured none.
- Duration: from setup at about 2026-10-26T18:56Z (first failing run) to the PR's last review at about 23:26Z, then the merge queue; the landing message is from 2026-10-27T05:58Z.
- Model: `deepseek-v4.1-flash/high` on opencode; executor 0.1.2017; `plugin_version` unknown.
- Anomalies:
  - `executor-skew` `could_not_check` again (operator: proceed). Guard `none`.
  - Main checkout dirty at setup (`queue/PLAN-01.json`). This recurs from PLAN-02 and has been fixed since by `use_worktree`.
  - The routed build failed with exit -1, because the daemon's `mvn` is not on its PATH. This recurs from PLAN-02. Every later build ran in-process.
  - Spec defect: the standing constraint named `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, which activates no profile in `pm-web-e2e` and skips Playwright silently. The run used `-Pe2e`. Corrected in the 27 staged web specs (see Follow-Ups).
  - The `main` baseline e2e run is `251 passed / 1 failed`: the `CleanupView` contrast case in light at narrow width fails at `65973291`, before this change.
  - Many shell read calls (grep/sed/ls/ps/curl) instead of file tools. They are disclosed in full by the run.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `a9491af` (4 findings), then was rate limited; it did not review the final head `0d28aa1`, and the standing authorization was applied by operator choice. Sourcery approved `0d28aa1`. 8 threads, all resolved. One reviewer point (tab titles use ids, not display titles) was answered and withdrawn.
- CI/merge: the first head's e2e run exposed a real regression (22 failures: the theme buttons collided by role with the header's System button). It was fixed with a radio group in `3c70e6e`. Merged through the merge queue as `099a3b40`, with no conflicts against PLAN-02.
- Sonar: `open_issue_count=0`, `open_issue_rules=none` at `0d28aa1`, after one `typescript:S7765` was fixed in `a9491af`.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped — `231`
- [x] row `landing` stamped — `landings/PLAN-04.md`
- [x] row `plan_marshall_plan_id` stamped — `04-web-theme-and-titles`
- [x] epic.md reconciled: Open Defect for the incomplete landing facts (recurrence folded into the PLAN-02 entry); recurrences recorded on the build-server and lane watches; new watches for the tab display titles and the failing `CleanupView` contrast case
- [x] resume anchor updated, `queue-view.md` regenerated

## Follow-Ups

- E2e command in the specs: `-Pintegration-tests` replaced by `-Pe2e` in the 27 staged web specs. PLAN-01 (running) and PLAN-04 (shipped) are left as they are.
- Tab titles show the identifier (`drop-legacy-api`), not the display title. No staged plan owns a title store. Recorded as a watch to decide at PLAN-09 (`web-display-layer`, the id-to-title rule) or at the PLAN-29 verification (`CC-V-04`).
- `epic.view.title.loading` has no documentation row: residue for PLAN-74 (`web-design-docs-closure`).
- The failing `CleanupView` contrast case in light/narrow on `main`: recorded as a watch for PLAN-70 (`web-light-theme-pass`) and PLAN-09 (which changes `CleanupView`).

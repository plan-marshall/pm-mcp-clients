# PLAN-195: The Contrast Tests Read the Theme They Name

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Split out of PLAN-70 (its "Residue Fold
> 2026-11-10", deliverable 4, and the `dataset.theme` clause option A of PLAN-85 assigned to PLAN-70) on the operator's word of
> 2026-11-25 ("split as proposed"): the test-only part has no dependency on the light pass and runs now. This spec is SELF-SUFFICIENT.
> **It adds no dependency.**

## Objective

The contrast checks must audit the theme they name. Today the e2e contrast matrix sets the `data-theme` attribute before `page.goto`,
while `app.html` re-derives the theme from `pm_theme`, which no e2e writes, so "dark" may audit the light theme; and the unit tests
read the token blocks with three copies of one helper and no guard that the two palettes differ. Test code only: no token value, no
view and no design change.

## Deliverables

1. **The e2e contrast matrix audits the requested theme**
 - What: `setTheme` (`pm-web-e2e/tests/helpers/contrast.js:14-19`) writes `pm_theme` in the page's origin (or applies the theme after
   `page.goto`), and every audited test asserts `document.documentElement.dataset.theme` equals the requested theme before it measures
   (`theme-readability.spec.js` ~`:664-667`).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The assertion fails when `setTheme` is reverted to setting the attribute before `goto` (mutation run quoted in the PR) and passes
     after (evidence order: `mutation-after`).
2. **The unit tests read the real blocks of both themes**
 - What: one reader of the theme blocks (`tokens.test.ts` `parseThemes`, ~`:354-368`, which already reads all four blocks by selector)
   is reused by `Button.test.ts` and `RowMenu.test.ts`; the other `blockOf` copies (`Button.test.ts` ~`:291`, `lib/test/stylesheet.ts:23`)
   go or delegate to it. `RowMenu.test.ts` reads the real light block, `--color-bg-subtle` and the `prefers-color-scheme` block (today it
   reads the first `:root` match and the last `[data-theme='dark']`). A test asserts the dark and light palettes differ.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The palettes-differ test fails when the dark block is made equal to the light one (evidence order: `test-first`).
   - Setting one dark `--color-*` of a tested pair to a low-contrast value makes the dark assertion fail, and restoring it passes; the
     failing assertion is quoted in the PR (evidence order: `mutation-after`).
   - If a pair then fails in a real theme, it is reported, not retuned here (the light pass is PLAN-70's).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, then
  `PM_MOCK_WEB_PORT=21950 PM_WEB_PORT=21951 ./mvnw -pl pm-web-e2e verify -Pe2e -am`, the full `./mvnw verify` and
  `./mvnw verify -Ppre-commit` at the head. main's own Playwright reds (`designer`, `projects`, `theme-readability`, 9 tests at
  `52aa9e4b`) are named and not counted against this plan, unless this plan's change turns them; any other red is.
- Ports of this plan: mock web port `21950`, app port `21951` (`AGENTS.md` § Ports); an assigned port in use is a blocker.
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with `@EnableGeneratorController`, contract values literal. No `{@html}` with model or engine text, no timer wait in a test. Pre-1.0: no shim.
- ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`; a dismissal only on the operator's selection.
- The merge rule (`CLAUDE.md`, Git Workflow): the run merges its own pull request once every review thread has a reply and is resolved, every review-body item is answered, some head has had one CodeRabbit review, the build and Sonar are green. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Never touch, rebase or merge another plan's branch (the parked PLAN-42's included). Other plans run at the same time: never stop or reuse a process this worktree did not start.

## Claim Labels

- OBSERVED: `contrast.js:14-19` `setTheme` sets only the `data-theme` attribute; `theme-readability.spec.js:664` calls it before `:667` `page.goto`; no `dataset.theme` assertion exists in that spec — read at `3dea04b2` (cleanup 2026-11-25)
- OBSERVED: `blockOf` exists three times (`Button.test.ts:291`, `tokens.test.ts:321`, `lib/test/stylesheet.ts:23`); `tokens.test.ts:354-368` `parseThemes` reads the four blocks by selector; no palettes-differ guard exists (only an elevation check, `tokens.test.ts:529-533`) — read at `3dea04b2`
- Verify-first clause: re-read the named lines at the then-current HEAD; a refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at HEAD 3dea04b2
- OBSERVED: `pm-web-app/src/lib/test/stylesheet.ts` — present at HEAD 3dea04b2

## Dependencies and Sequencing

- Depends on: nothing in the queue. Before PLAN-70 (its D1 builds on these tests) and PLAN-72 (it restructures `tokens.css`; the reader
  must not depend on the header comment).
- Overlaps with: `doc/plans/ui-rule-catalogue.adoc` and PLAN-73 (`theme-readability.spec.js`, `contrast.js`): whichever merges second rebases.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-195-web-contrast-tests-read-their-theme.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports
its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

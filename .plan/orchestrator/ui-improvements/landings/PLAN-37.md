# PLAN-37 — Web: Accessibility Baseline and axe-core — landing

PR #297, merged into `main` as `c5bfb1ca` (29 files, +653/−85). Lane message `rb-37-web-accessibility-baseline-002` (the earlier `-001` was the observation of the stop at the reviewer).
Analysed in three layers (specified / implemented / correct) by reading; nothing was built or run by the orchestrator.

## Verdict

| Layer | Verdict |
|---|---|
| 1 Specified | D1 and D2 met in substance; D3's first Done-when (the e2e command passes) is NOT met as written, which the lane reports. |
| 2 Implemented | All 7 declared files changed or created; 22 files beyond the declaration, all in the area (views, shell, components). One dependency, the approved one. |
| 3 Correct | Escape, focus return and the reasons are sound by reading; two leads and one browser caveat, not reproduced. |

## Layer 1 and 2 — what was specified, what the diff holds

- D1 Escape and focus: pause panel (`ControlActionsBar`: window keydown, `openDialog`/`closeDialog` remember the opener, fall back to the first enabled act when a confirm disabled the opener), chat (`ChatPanel` `onClose` seam, hosts `WorkControlPanel`, `EpicView`, `DecisionView`, `StartView`), overflow (`Dialog.svelte`, Escape predates the plan). Three e2e checks in `workspace.spec.js`; the overflow check is a regression guard, never seen failing, which the lane says (the `test-first` order is not producible for it).
- D2: `aria-describedby` to the visible reason on disabled acts (`ControlActionsBar`, `ChatPanel`); the send button no longer disables itself on an empty message (a behaviour change the lane chose so every disabled control states why); `IconButton` draws its label from 768 px; no `text-transform: uppercase` remains in `pm-web-app/src` (checked by grep, none); no font size under 12 px remains (the one `0.8rem` is 12.8 px); the phone's 44 px target is in `ControlActionsBar` (five places), `ChatPanel` (two), `WorkControlPanel` (one). The PR body names only the archive link; the code holds more. Claim holds.
- D3: `@axe-core/playwright ^4.14.0` added to `pm-web-e2e/package.json` — the one dependency the operator approved; the lock file grows by 27 lines (axe-core with it). `a11y.js` runs WCAG 2.0/2.1 A and AA, fails on `serious` and `critical`, disables no rule. `theme-readability.spec.js` runs it over `GUARD_JOURNEYS` and asserts the audited set equals the journey's views; a coverage guard counts the 22 view files. The violations the audit found were fixed in the views (names that include the visible text, a card that was a `role="button"` around a button, the onboarding step line, two unnamed controls).
- Surface delta (by hand): declared 7 (6 present, `a11y.js` created); all 7 present. Beyond the declaration: 22 (10 views, 2 shell files, 5 components — `BlockPalette`, `ControlActionsBar`, `IconButton`, `NodeNarrative`, `Tabs` —, `ChatPanel.test.ts` and `IconButton.test.ts`, `start.{en,de}.properties` one key each, `theme-readability.spec.js`). `expansion_detected`; every file is in the accessibility area and the lane names the reason for each group.
- The spec's pre-commit drift was not in the PR (29 files, none under `pm-api/src/main/java`).
- D3's first Done-when: the run at the merged head (`maven-2026-11-01-223530.log`) had 326 passed and 7 failed, the names equal to those recorded on `main` (`epic-view.spec.js:190`, `plan-view.spec.js` x3, `showroom.spec.js:88`, the `plan` text guard de/en). The audit's 15 tests are green. PLAN-90 (running) fixes those seven.

## Layer 3 — read, not run

Sound by reading:
- `ChatPanel` and `ControlActionsBar` handle Escape on `window`, stop the key's propagation, restore the opener after `tick()`, and `isConnected`/`disabled` guard the restore; `DecisionView` carries the decision id past the `await`; `Tabs` walks from the selected tab when the list itself has the focus.
- CodeRabbit's six findings of the first review are each visible in the diff (topmost-layer Escape, focus into the control dialog and the disabled-opener fallback, the tablist walk, the decision id, the onboarding Tab stop, the audit logging what it does not gate on). Read from the code, not from the threads.
- `IconButton.test.ts` and `ChatPanel.test.ts` assert the accessible name, the visible label span and the reason through `aria-describedby`.

Leads and gaps, none reproduced:
1. **DataTable row menu (PLAN-35 x PLAN-37).** `IconButton` now draws its `label` beside the glyph from 768 px (`nowrap`, `inline-size: auto`). `DataTable` names every row menu `Row menu: <first cell>` and gives the menu column 60 px under `table-layout: fixed`. Read together, each row would draw "Row menu: <plan name>" beside the glyph and spill into the neighbouring column at tablet width and up. The lane's browser checks were the control views, not `/dev/components`; `IconButton.test.ts` cannot see a media query. The same check is owed for `ActionBar`'s overflow `IconButton`. The lane's note that the label "cannot be exercised on a built app" holds only for shipped views; the showroom and the dev page are the only callers today.
2. **ChatPanel's Escape guard** returns whenever `document.querySelector('[role="dialog"]')` finds anything. `role="dialog"` is in ten components (`CommandPalette`, `SystemView` twice, `LanListenerModal`, `ProjectOnboardingView`, `Dialog`, `FindingsReviewModal`, `ProjectExplorerView`, `ControlActionsBar`). If one stays mounted while hidden, Escape never closes the chat there. Not read per component.
3. The opener is `document.activeElement` at the moment of opening. A mouse click does not focus a button on Safari or on Firefox for macOS, so the focus would then not return. The lane checked in the Playwright browser only.
4. The audit runs `de` at 1440 × 900 and the browser's own theme: an English-only label gap, a 390 px layout issue or a theme-specific contrast would not be caught (the lane says so; PLAN-89 deliverable 4 widens it for the palette).
5. The send button no longer disables on an empty message; `send()` ignores an empty text, so nothing is sent, but the control now looks active with an empty field.

Overlap now real: the PR changed `SystemView.svelte` (aria-labels of the work figures, around :860-930). PLAN-90 deliverable 3 and PLAN-43 (moves the view onto `DashboardPage`) both name that file; hunks differ, rebase when either lands.

## Evidence checked

- Sonar: the lane read `confirmed` 0 at `f5e00490` (analysis revision equals head). My read after the merge is `undecidable` (`head_revision_unavailable`, analysis revision `f5e00490…`): recorded as an unverified lead, not as 0.
- Reviewers: CodeRabbit reviewed `f27e1161` and `21640dc` (six inline findings and a follow-up, fixed in `21640dc`) and refused six triggers on quota; it never reviewed the heads after (`6a0cdeb`, `f5e00490`). Four of its six inline threads stay open (the lane's CI wrapper cannot list thread ids). Sourcery refused on quota. The merge rested on the operator's selection in the lane ("Merge now on the standing authorization"), with the head not reviewed and four threads open: the lane says it bent one clause; I record it as the operator's decision, not as covered by the standing authorization.
- The lane's account of the browser artifacts (`.plan/temp/playwright-mcp/PLAN-37-…`) was not re-opened by me.
- One routed `verify` failed at once (the daemon ran `mvn` instead of `./mvnw` in the main checkout); every later build ran `in_process`. Recorded in the lane's section 8; the cause is the known bare-`mvn` watch.

## Follow-ups

- PLAN-89 deliverable 5 (added): verify and fix leads 1 to 3 in a browser; deliverable 4 widens the audit.
- The seven red e2e tests: PLAN-90.
- Re-read PLAN-43 and PLAN-90 (SystemView), PLAN-41 (verify-web-design-system, now unblocked), PLAN-45, 47, 49, 51, 53, 55, 58 (they name views, `ChatPanel`, `ControlActionsBar`, `IconButton`).
- The operator's call: whether the standing merge authorization may cover a head the reviewer never saw (PLAN-17, PLAN-46, PLAN-37).

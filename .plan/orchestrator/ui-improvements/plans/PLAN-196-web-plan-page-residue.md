# PLAN-196: Web: The Plan Page Residue of PLAN-55

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. The fix plan for the landing analysis of
> PLAN-55 (#576, `163e2a49`, `landings/PLAN-55.md` § Layer 3), staged on the operator's word of 2026-11-25 ("stage a fix plan").
> This spec is SELF-SUFFICIENT. **It adds no dependency.**

## Objective

PLAN-55 put the plan page on the `ObjectPage` template with the tabs Process, Intent and Changes. Its landing analysis found no
blocker and no major, but ten minors and three infos. This plan fixes them:
- the decision and the completion report should open in place;
- the head's lines should be in the template's own slots;
- the settings dialogue has a stray Close button, raw ids and a revision race;
- a dead catalogue key is still documented;
- the Changes columns don't match the design;
- the browser evidence PLAN-55 left short.

`PlanView.svelte` is abbreviated PV below. Line numbers are at `163e2a49`.

## Deliverables

1. **The head opens its objects in place, and its lines sit in the template's slots**
 - What:
   - The waiting plan's decision head link (PV:1248-1255, today `href="/decisions/{id}"`) opens the decision in place as `open=decision:{id}` on the plan's address (`navigation.adoc:200,214`: opened by a push, closed by the history's back; a loaded `open=` closes by replacing the address with the caller's). The parser and writer exist (`routes.ts:628-629,718`, `runtime.ts:654` `openObject`). `EpicView.svelte:117,724,982` is the working model of an object opened in place.
   - The ended plan's _Completion report_ link (PV:~1238-1246, today the legacy `/reports?plan=`, `routes.ts:61`) becomes `open=completion-report:{plan}`, the same way.
   - The attention strip, the plan-level off-track banner, the queued-revision line and the ended banner move from the `tabs` slot (PV:1257-1262 and below) into `ObjectPage`'s `attention` slot (`ObjectPage.svelte:65-68`).
   - The `state` slot's content is phrasing content: today a `<div slot="state">` (PV:1199) sits inside the template's `<span>` (`ObjectPage.svelte:54-56`).
   - The open-decision read runs when the plan starts or stops waiting, not on every process reload. Today the `$effect` at PV:263-271 reads `plan`, so `loadOpenDecision` runs on every refresh while the plan waits.
   - "task N of M" (PV:457-487) gets the test it lacks.
   - `queuedRevisionOf` (PV:~299) no longer maps an unknown `applies.key` to `after_task` with an empty `task_id`. An unknown key shows no task, and a test pins it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
 - Done when:
   - `PlanView.test.ts` passes on all of these:
     - On a waiting plan, the decision link's target carries `open=decision:{id}` and no `/decisions/` path.
     - On an ended plan, the completion report link carries `open=completion-report:{plan}`.
     - The strip and the banners render inside `object-page-attention`.
     - No `div` is a descendant of `object-page-state`.
     - "task 3 of 5" renders for a running leaf task.
     - An unknown `applies.key` renders no task.
     - Each of these fails with its production change reverted (evidence order: `test-first`).
   - `PlanView.test.ts` counts the decision reads across two process refreshes of a waiting plan: one read, not three (evidence order: `test-first`).
   - `plan-view.spec.js` on `running-work` → `decisions.open` passes on both of these (evidence order: `mutation-after`, the revert run quoted in the PR):
     - The decision link opens the decision in place, and the history's back returns to the plan.
     - A reload of the `open=` address closes back to the plan.
2. **The Intent tab paints its loading state, and its criteria link to their deliverables**
 - What:
   - On the first paint of the Intent tab, `loadIntent` has not set `intentLoading` yet, so the tab says `intent.plan.none` ("not written yet") for a moment (PV:1460-1465). The tab starts in its loading state until the first read answers.
   - `PlanIntentPanel` gets a `deliverableHref` from PV (PV:1467 passes none), so a criterion's deliverable label links to `#deliverable-N`. `OutlinePanel.svelte:151` already gives `id=deliverable-N`, which refutes PLAN-55's "no fragment target". Do not edit the three intent panels' own files beyond what the link needs; if one needs more, report it.
   - Returning to Changes by the history's back shows the remembered file, but the address has no `file=` (`navigation/history.ts:291`, pre-existing). The address carries the `file=` it shows.
   - The activity feed's stop when Intent hides the inspector (PV:~589), claimed by PLAN-55, gets its test.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
 - Done when:
   - `PlanView.test.ts` passes on all of these, and each fails with its production change reverted (evidence order: `test-first`):
     - The Intent tab shows the spinner and not `plan-intent-none` before the first intent read resolves.
     - A criterion's deliverable label is a link to `#deliverable-N`.
     - Back to Changes restores `file=` in the address.
     - Switching to Intent stops the activity feed, and switching back restarts it.
3. **The settings dialogue: one Close, readable fields, no lost write, its texts documented**
 - What:
   - `PlanSettingsModal` keeps one Close: the footer `Button` (`PlanSettingsModal.svelte:260`). The legacy plain `<button data-testid="settings-close-button">` (:272-276) goes, with its test ids moved to the footer `Button`.
   - The conflict acts _Keep current_ and _Apply again_ (:165-185) become design-system `Button`s (VD-S-06).
   - A field's label is the aspect's display name through the display layer, not the raw aspect id (:203). Its options are display values, not raw choice strings (:209). The display layer is WS-02's formatter, and catalogue keys are added in en and de. If the runtime sends a choice that has no display name, show the raw value and report it in the landing message.
   - `applySetting(payload, true)` (`WorkControlPanel.svelte:1129`, `preferCurrent` at :623-635) sends the panel's freshest revision for the second and later fields. A write by someone else that lands between two of the dialogue's own applies is then overwritten without a conflict. Send the revision of the dialogue's own last applied answer instead, so a foreign write between them yields the conflict.
   - `control.settings.use_inherited` (`control.{en,de}.properties:56`) is read by no web or terminal code at `163e2a49` (the terminal reads `operator.actions.settings.use_inherited`). It goes, along with its row in `doc/design/shared/work-actions-texts.adoc:153`. That table gains the keys the dialogue reads and does not list: `control.settings.drop_own`, `control.settings.changed`, `control.settings.saved`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-documents:ref-asciidoc
 - Done when:
   - `PlanSettingsModal.test.ts` passes on all of these, and each fails with its production change reverted (evidence order: `test-first`):
     - The dialogue has exactly one Close control.
     - The conflict acts are `Button`s.
     - No field label or option text equals a raw aspect id or choice string that has a display name.
   - `WorkControlPanel.test.ts` passes on this, and it fails with the revision change reverted (evidence order: `test-first`): a revision bumped by a foreign write between two applied fields yields the conflict, not a silent overwrite.
   - `git grep control.settings.use_inherited` has no hit, and every `control.settings.*` key the dialogue reads has a row in `work-actions-texts.adoc` (evidence order: `none`).
4. **The Changes layout as designed, and the browser evidence PLAN-55 left short**
 - What:
   - In the wide form, the Changes list is `280`–`360 px` and the hunks take the rest (`plan-view-interaction.adoc#layout`). Today the grid is `minmax(0, 1fr) minmax(0, 1.4fr)` (`PlanChanges.svelte:484-487`). The Process tab's tree follows the same rule.
   - The browser check covers the plan page after every visual change of this plan:
     - targets: head, attention slot, decision in place, completion report in place, Intent, Changes, settings dialogue
     - states: `running`, `waiting`, `ended`, `intent`, `changes`
     - at 1440 × 900 and 390 × 844, in en and de, light and dark
   - This also completes the 8 of 40 combinations PLAN-55 did not take: `running` and `waiting` were taken only in en-light and de-dark.
   - The 390 px watch on `/plans/{id}` (`mobile.spec.js`) is confirmed and reported as retired, or as still open.
   - `doc/design/web-app/plan-view-interaction.adoc` and `doc/design/shared/plan-view.adoc` describe what this plan changes: the in-place decision and completion report, and the attention slot.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:javascript-security, pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - `PlanView.test.ts` or `pageWidth.test.ts` asserts the Changes list's width bounds at a `720 px` column (evidence order: `test-first`).
   - Browser check (Playwright MCP): 80 screenshots, one per state, width, language and theme. All are taken after the last visual commit and named `196-{state}-{1440|390}-{en|de}-{light|dark}-after.png`. Before screenshots of the same set come from the base, accessibility snapshots are kept for both, and the PR lists the screenshots (evidence order: `none`).
   - The two design documents read as built, checked against the screenshots (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, these pass: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=21960 PM_WEB_PORT=21961 ./mvnw -pl pm-web-e2e verify -Pe2e -am`, `./mvnw -pl pm-api verify`, the full `./mvnw verify` and `./mvnw verify -Ppre-commit`. main's own Playwright red at `163e2a49` (`designer.spec.js:264`) is named and not counted against this plan, unless this plan's change turns it. Any other red is counted.
- Ports of this plan: mock web port `21960`, app port `21961` (`AGENTS.md` § Ports). Pass them to every mock, app and e2e start. An assigned port that is in use is a blocker, never a reason to take another port.
- **No new dependency or plugin.**
- Frontend standards (`CLAUDE.md`): ES modules only. Text from the runtime or from a model reaches the DOM only through text-treating sinks: no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}`. No timer wait in a test. Vitest with Testing Library, with the `jest-testing` standards as method.
- Java, if touched (the mock): Java 25, JUnit 5 only, input from `Generators` with `@EnableGeneratorController`, contract values literal. Pre-1.0: no shim.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width. Every server value passes through the display layer of WS-02. The text-guard baseline (`pm-web-app/scripts/vocabulary-baseline.json`) may only shrink.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check: follow `AGENTS.md` § Browser checks and PLAN-55's Standing Constraints:
  - Start the mock on `21960` and the app on `21961`.
  - Pair with a fresh single-use code.
  - Reach the states with `POST /mock/goto/<state>` and a reload.
  - Read the page through `browser_snapshot`.
  - Write only to `.plan/temp/playwright-mcp/`.
  - Stop what you started.
  - If the Playwright MCP server is not available, the browser Done-when is NOT met: a blocker, never replaced.
- ZERO Sonar findings on the new code of the pull request (SonarCloud `plan-marshall_pm-mcp-clients`), read at the head after the push. State `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`. A dismissal is made only on the operator's selection, through `plan-marshall:workflow-integration-sonar:sonar_rest transition`.
- The merge rule (`CLAUDE.md`, Git Workflow). The run merges its own pull request once all of these hold:
  - every review thread has a reply and is resolved;
  - every review-body item is answered;
  - some head has had one CodeRabbit review;
  - the build and Sonar are green.

  The pull request body states the thread count, the resolved count and the review-body items from `ci pr comments`, never from memory. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator only if it still refuses.
- The landing message states every claim against the merged tree: file names, state names, counts of screenshots and tests. PLAN-55's message carried six stale claims (`landings/PLAN-55.md` § Layer 2).
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Never touch, rebase or merge another plan's branch. Other plans run at the same time: never stop or reuse a process this worktree did not start.

## Claim Labels

- OBSERVED: PV:1248-1255 renders the decision head link with `href={/decisions/${id}}`; `address.open` is parsed and written (`routes.ts:628-629,718`) and read into `openObject` (`runtime.ts:654`), consumed only by `EpicView.svelte:117`; PLAN-143 (the address model) shipped — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanView.svelte:1248-1255 href /decisions/; routes.ts:628-629,718; runtime.ts:654; EpicView.svelte:117 only consumer; PLAN-143 row shipped (orchestrator read at 163e2a49)
- OBSERVED: PV:1257-1262 puts the attention strip in the `tabs` slot; `ObjectPage.svelte:65-68` offers an `attention` slot PV does not fill; PV:1199 `<div slot="state">` inside `ObjectPage.svelte:54-56`'s `<span>` — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanView.svelte:1257-1262 strip in tabs slot; ObjectPage.svelte:65-68 attention slot unused; PlanView.svelte:1199 div in ObjectPage.svelte:54-56 span (orchestrator read at 163e2a49)
- OBSERVED: PV:263-271's `$effect` reads `plan` and `waiting` and calls `loadOpenDecision` while the plan waits — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanView.svelte:263-271 effect reads plan and waiting (orchestrator read at 163e2a49)
- OBSERVED: PV:1460-1465 shows `plan-intent-none` whenever `!intentLoading && !intentHasRecord`; `OutlinePanel.svelte:151` sets `id=deliverable-N`; PV:1467 passes no `deliverableHref` — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanView.svelte:1460-1465; OutlinePanel.svelte:151 id=deliverable-N; PlanView.svelte:1467 no deliverableHref; PlanIntentPanel.svelte:40,48,291 already takes deliverableHref (orchestrator read at 163e2a49)
- OBSERVED: `PlanSettingsModal.svelte:260` footer Close `Button` and `:272-276` a second plain `<button data-testid="settings-close-button">`; conflict acts plain `<button>` `:165-185`; `label={entry.aspect}` `:203`, `label: choice` `:209` — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanSettingsModal.svelte:165-185 plain buttons, :203 label=entry.aspect, :209 label: choice, :260 footer Button, :272-276 second close button (orchestrator read at 163e2a49)
- OBSERVED: `control.settings.use_inherited` has no reader in `pm-web-app/src` or `pm-operator` (the terminal reads `operator.actions.settings.use_inherited`, `PlanSettingsDialogue.java:238`); documented at `work-actions-texts.adoc:153` — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep: no reader in pm-web-app/src or pm-operator; PlanSettingsDialogue.java:238 reads operator.actions.settings.use_inherited; work-actions-texts.adoc:153 (orchestrator read at 163e2a49)
- OBSERVED: `PlanChanges.svelte:484-487` `grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr)` — read at `163e2a49`
  - verdict: corroborated | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: PlanChanges.svelte:484-487 (orchestrator read at 163e2a49)
- HYPOTHESIS: `applySetting(payload, true)` (`WorkControlPanel.svelte:1129`) with `preferCurrent` (:623-635) lets a foreign write between two applied fields pass without a conflict — confirm or refute at the outline; a refutation drops that clause and is reported
  - verdict: unverifiable | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: hypothesis for the outline; the landing analysis of PLAN-55 found it, not re-proven by reading
- HYPOTHESIS: "task N of M" (PV:457-487) and the inspector-feed stop (PV:~589) have no assertion in `PlanView.test.ts` (the landing analysis found none) — confirm at the outline
  - verdict: unverifiable | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: hypothesis for the outline; the landing analysis of PLAN-55 found it, not re-proven by reading
- Verify-first clause: re-read the named lines at the then-current HEAD before each deliverable. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 163e2a49 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/templates/pageWidth.test.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.test.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/PlanChanges.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/components/PlanIntentPanel.svelte` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/navigation/history.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 163e2a49
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 163e2a49
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 163e2a49
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 163e2a49
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 163e2a49
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at HEAD 163e2a49
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 163e2a49
- OBSERVED: `doc/design/shared/work-actions-texts.adoc` — present at HEAD 163e2a49

## Dependencies and Sequencing

- Depends on: `PLAN-55` (shipped, #576) and `PLAN-143` (shipped, the address model).
- Overlaps with:
  - running `PLAN-194` and `PLAN-184`: `control.{en,de}.properties`, two code files each, and `work-actions-texts.adoc` with PLAN-184 (doc). Both are under the overlap rule's threshold; whichever merges second rebases.
  - staged `PLAN-62` and `PLAN-190`: `PlanView.svelte`(+test) and `plan-view.spec.js`. This plan runs before them; they are re-read against its merge.
  - staged `PLAN-136` (the head's queued-revision line).
  - staged `PLAN-179` (`WorkControlPanel.svelte`).
- Adjacent to: WS-02's display layer (the aspect and choice names).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-196-web-plan-page-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file
under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The
session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` §
Ledger Write-Boundary).

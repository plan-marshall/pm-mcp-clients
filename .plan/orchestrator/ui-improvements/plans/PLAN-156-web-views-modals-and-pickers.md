# PLAN-156: Web Views: The Modals and Pickers on the D1 Vocabulary (Views Package, Part 4)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents and `shared/work-actions.adoc` § `#holds`, `#review`, `shared/starting-work.adoc` § `#documents`, `#pull-request`.
> SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

Seven modals and pickers are drawn by hand, on layers of their own (50, 60, 100): `HoldResolutionModal`, `FindingsReviewModal`, `ConfinedFilePicker`, `PrAdoptPanel`, `GrantModal`, `LanListenerModal`, and a dead `StartWorkModal`. This plan moves them onto `Dialog`/`ConfirmDialog`,
the buttons by the hierarchy, the field system and the choice controls, and drops the layer literals. It changes no behaviour. The first-open-item logic of `FindingsReviewModal` is PLAN-58's; `PlanSettingsModal` is PLAN-55's; the source modals are PLAN-53's.

## Deliverables

Read first: `doc/design/web-app/components.adoc` (§ `#dialog`, `#confirm-dialog`, `#hierarchy`, `#layers`), `components-fields.adoc` (§ `#choice`, `#compose`), `shared/work-actions.adoc` (§ `#holds`, `#review`), `shared/starting-work.adoc` (§ `#documents`, `#pull-request`, `#issue`),
`shared/work-actions-review-texts.adoc`.

Not in this plan: the first-open-item logic and the register behaviour of `FindingsReviewModal` (PLAN-58); `PlanSettingsModal` (PLAN-55); `GitSourceIntakeModal`, `LessonSourceModal`, `SourceList`, `SourceIntakePanel`, `RecipeSelector`, `StartView` (PLAN-53); the chat in the review (PLAN-137).

1. **`HoldResolutionModal`**
 - What: onto `Dialog`; the five holds' acts by the hierarchy (the act the hold names is secondary, _Retry now_ secondary, an act that cannot be undone opens its own `ConfirmDialog`); the integrity hold shows the tampered file with its recorded and current size and digest as a `KeyValueList`;
   the reason of an abort is a `TextField`; the 4 raw buttons and the `<input>` go; the layer literal (50) goes. The acts offered stay what the runtime offers.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `HoldResolutionModal.test.ts` and `control.spec.js` show: each hold kind with its acts, an irreversible act through `ConfirmDialog`, `Escape` returns the focus to `WorkControlPanel`'s act, a still-held result shows the cause. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 for the file; no `z-index` remains; it leaves the layer allowlist (evidence order: `none`).
 - Implements: `components.adoc` § `#dialog`, `#confirm-dialog`; `shared/work-actions.adoc` § `#holds`.
2. **`FindingsReviewModal` (restyle only)**
 - What: onto `Dialog`; 19 raw buttons, 2 text areas and an input become `Button` (by role: _Finish_ primary; _Keep_, _Drop_, _Promote to a plan_, _Edit_, _Chat_, _Accept the remaining proposals_ secondary), `Compose` for the operator's words, `TextField` for the one-line drop reason;
   the register list beside the item shows each disposition as a `StatusLabel`; an untrusted item stays text. The layer literal (50) goes. The order, the open-first selection and the register logic stay exactly as they are.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `FindingsReviewModal.test.ts` and `epic-view.spec.js` show the same sequence of items and dispositions as before through the new components, with the keyboard path (focus held, `Escape`). Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 for the file; no `z-index` remains; it leaves the layer allowlist (evidence order: `none`).
 - Implements: `components.adoc` § `#dialog`, `#hierarchy`; `components-fields.adoc` § `#compose`; `shared/work-actions.adoc` § `#review`.
3. **`ConfinedFilePicker` and `PrAdoptPanel`**
 - What: `ConfinedFilePicker.svelte` (7 raw buttons, 1 input, layer 60) onto `Dialog`, the listing as a `CheckGroup`, the path as `TextField`, a directory's summary (`12 files: 9 adoc, 3 svg`) as detail text, the disabled _Move_ and home-directory entries with their `disabledReason`;
   `PrAdoptPanel.svelte` (2 buttons, 2 inputs) uses `Field`/`TextField` and shows the pull request as `PrChip`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - New `ConfinedFilePicker.test.ts` and `PrAdoptPanel.test.ts` (neither exists): the checkout first, then the home directory; a disabled entry with its reason; a pick, a clear and a confirm; a pull request number or URL accepted. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 for both files; no `z-index` remains (evidence order: `none`).
 - Implements: `components.adoc` § `#dialog`; `components-fields.adoc` § `#choice`, `#field-set`; `shared/starting-work.adoc` § `#documents`, `#pull-request`.
4. **`GrantModal`, `LanListenerModal` and the dead `StartWorkModal`**
 - What: `GrantModal` (3 buttons, layer 60) and `LanListenerModal` (layer 100) onto `Dialog`/`ConfirmDialog` with the buttons by the hierarchy; where the design names no role for a button the choice is recorded in the pull request. `StartWorkModal.svelte` and `StartWorkModal.test.ts`
   have no importer (only their own test) and are removed (operator decision D-10, assumed A: the modal is dead code).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `LanListenerModal.test.ts` passes through `Dialog`; a new `GrantModal.test.ts` shows the three grant answers and `Escape`. `git grep -n StartWorkModal -- pm-web-app/src` is empty. Evidence order: `test-first` for the tests, `none` for the grep.
   - `npm run lint:vocabulary` reports 0 for both modals; the layer scan allowlist loses `GrantModal`, `LanListenerModal`, `StartWorkModal` (evidence order: `none`).
 - Implements: `components.adoc` § `#dialog`, `#hierarchy`.
5. **Browser evidence**
 - What: each modal opened in a browser from its owner view, desktop and phone, both locales and themes; the focus order and `Escape` walked by keyboard.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) on `running-work`, `projects-and-start` and `fully-onboarded` at 1440×900 and 390×844, `de` and `en`, both themes; baseline and after screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it); `theme-readability.spec.js` passes for the modals' owners (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text (finding texts and file names are untrusted), no timer waits in tests, the npm scripts stay.
- A pure restyle: no change to what a modal shows, sends or stores; the review's order and the holds' acts are the runtime's. Where a rule of the design cannot be met without a behaviour change, record it as a finding in the pull request.
- Every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- Ports of this plan: mock web port 21560, app port 21561 (`20000 + 10 × 156`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: raw element counts — `HoldResolutionModal` 4 buttons + 1 input, `FindingsReviewModal` 19 buttons + 2 textareas + 1 input, `ConfinedFilePicker` 7 buttons + 1 input, `PrAdoptPanel` 2 buttons + 2 inputs, `GrantModal` 3 buttons, `LanListenerModal` 0 raw (a hand-drawn dialog), `StartWorkModal` 6 buttons + 1 input; layer literals 50, 50, 60, none, 60, 100, 50 — read by `git grep -c` and `git grep -n z-index`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Counts identical: Hold 4b+1i, Findings 19b+2ta+1i, Picker 7b+1i, PrAdopt 2b+2i, Grant 3b, LanListener 0 raw, StartWork 6b+1i; z-index 50 50 60 none 60 100 50
- OBSERVED: owners — `HoldResolutionModal` and `PlanSettingsModal` are imported by `WorkControlPanel.svelte`; `FindingsReviewModal` by `EpicView.svelte`; `ConfinedFilePicker` and `GrantModal` by `StartView.svelte`; `PrAdoptPanel` by `GitSourceIntakeModal.svelte`; `LanListenerModal` by `SettingsView.svelte`; `StartWorkModal` by nothing — read by `git grep -l` of each name
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: WorkControlPanel imports Hold+PlanSettings; EpicView FindingsReview; StartView Picker+Grant; GitSourceIntakeModal PrAdoptPanel; SettingsView LanListener; StartWorkModal nothing
- OBSERVED: tests — `HoldResolutionModal.test.ts`, `FindingsReviewModal.test.ts`, `LanListenerModal.test.ts` exist; `ConfinedFilePicker`, `PrAdoptPanel`, `GrantModal` have none — read in `components/`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: HoldResolutionModal FindingsReviewModal LanListenerModal tests exist; none for ConfinedFilePicker PrAdoptPanel GrantModal
- OBSERVED: surfaces — `PLAN-58` and `PLAN-137` declare `FindingsReviewModal.svelte`; `PLAN-55` declares `PlanSettingsModal.svelte`; no staged plan declares `HoldResolutionModal`, `ConfinedFilePicker`, `PrAdoptPanel`, `GrantModal`, `LanListenerModal` — read by `corpus surfaces`
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Corpus surfaces of staged plans, a ledger fact not settleable from source
- HYPOTHESIS: the irreversible hold acts (_Accept the change_, _Abort the plan_, _Give up the removal_) already confirm first inside `HoldResolutionModal` — confirm/refute at `HoldResolutionModal.svelte` § act handlers (verify-at-outline); if not, record it as a finding, do not change the behaviour here
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: HoldResolutionModal.svelte:30 IRREVERSIBLE set (accept end_plan abort cancel_epic give_up_removal); inline hold-confirm step :153-170 before confirmAct
- HYPOTHESIS: `FindingsReviewModal` is one dialog with a list beside the item, not two — confirm/refute at `FindingsReviewModal.svelte` § markup (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: FindingsReviewModal has one role=dialog (:152) and a review-modal__list ul (:182) beside the item
- HYPOTHESIS: the `Dialog` footer slot takes more than two acts without a layout change — confirm/refute at `Dialog.svelte` § footer (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Dialog.svelte .dialog__footer is flex wrap justify-end, so more than two acts need no layout change
- Verify-first clause: re-read each modal after PLAN-45, PLAN-149, PLAN-150, PLAN-151 and PLAN-58/53/55 (if any landed); a modal a landed plan has already moved is dropped from this plan.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first instruction, not a code fact

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/HoldResolutionModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/HoldResolutionModal.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfinedFilePicker.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfinedFilePicker.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PrAdoptPanel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/PrAdoptPanel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/GrantModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/GrantModal.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/LanListenerModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/LanListenerModal.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/StartWorkModal.svelte` — present at HEAD 1cc0e10e; removed by this plan
- OBSERVED: `pm-web-app/src/lib/components/StartWorkModal.test.ts` — present at HEAD 1cc0e10e; removed by this plan
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/work-actions.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed; `PLAN-149` (`Dialog`, `ConfirmDialog`, hierarchy), `PLAN-150` (fields and choices, `Compose` via PLAN-147), `PLAN-151` (the count).
- Overlaps with:
  - `PLAN-58` (parked): `FindingsReviewModal.svelte` (its first-open-item logic). This plan lands FIRST; PLAN-58 is re-grounded on it.
  - `PLAN-137` (parked): `FindingsReviewModal.svelte` (the chat in the review); follows this plan.
  - `PLAN-53` (parked): `StartView.svelte` (imports `ConfinedFilePicker`, `GrantModal`), `GitSourceIntakeModal.svelte` (imports `PrAdoptPanel`); its own modals keep their own layers until it moves them.
  - `PLAN-55` (parked): `PlanSettingsModal.svelte` and `WorkControlPanel.svelte` (excluded here).
  - `PLAN-60` (parked): `SettingsView.svelte` (imports `LanListenerModal`).
  - `PLAN-45` (running): `views/` (not these component files).
- Adjacent to: `WorkControlPanel.svelte` (opens the hold modal; unchanged); `Dialog` (used, not changed).
- Unblocks: PLAN-58 and PLAN-137 on a restyled review; PLAN-53 and PLAN-60 on restyled modals; the gate "views package implemented" of PLAN-29, 41 and 65.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since. Settle each HYPOTHESIS. Re-check which plan claims each modal and the operator's answer to D-10.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- Premise: since `a139ec62` the web chat (entry, plan, epic, review, ended work) was broken against the mock until `PLAN-161`
  (chat bridge). RESOLVED 2026-11-14: `PLAN-161` (#429, `dc811fb3`) landed and removed `PLAN-148`'s `test.fixme`; no `fixme` remains in
  `pm-web-e2e/tests`. Still open: a chat that is not seeded is never OPENED by the clients (GET only) — `PLAN-135`/`PLAN-138` own that;
  an e2e failure on an unseeded chat is theirs, name it in the pull request. An e2e failure at this plan's head that traces
  to the chat contract is not this plan's to fix: name it in the pull request with `PLAN-161` as owner.
- The scenario fixtures are validated at load against the B1 catalogue (unknown chat keys fail closed): a fixture edit in a `chats` block
  must satisfy it.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-156-web-views-modals-and-pickers.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

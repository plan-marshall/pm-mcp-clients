# PLAN-179: Web: The Epic View and the Intent Panels Follow What They Show (Residue of PLAN-58, 145 and 146)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of
> 2026-11-23 (decision 3, proposal 1), which carries out the residue plan the operator decided on 2026-11-18 for the intent-panels
> and epic cluster (the exception to the fold-as-D0 rule: a user-visible major): the major of the PLAN-58 landing (#456), the
> layer-3 findings of the PLAN-146 landing (#449) and the panels residue of the PLAN-145 landing. This spec is SELF-SUFFICIENT: the
> emitted command is a one-line pointer and carries no brief. **It starts after PLAN-144 has landed** (PLAN-144 edits `EpicView`,
> `ProjectExplorerView` and `runtime.ts`) and after PLAN-176 (`ProjectExplorerView`).

## Objective

The epic view loads its epic once, on mount. When the route changes from one epic to another in place (the command palette's `openEpicView`, the single `EpicView` of `AppRouter`), the address and `epicId` change and the head, the rows, the intent and the acts stay the first epic's until an event refresh, and the intent panel of the epic reads its id once too. The intent panels refetch on every event of their name whichever record moved, replace a held record with an error on a failed refresh, and the over-size read fails open in Java where the TypeScript reader throws. This plan makes the epic view and its panels follow the epic and the record they show, and pins each behaviour with a test that fails when the production line is reverted.

## Deliverables

The documents are the specification; read them first: `doc/design/web-app/epic-view.adoc`, `intent-panels.adoc` (or the design's intent section) and `components.adoc` (#tabs), and `landings/PLAN-58.md`, `PLAN-146.md` and `PLAN-145.md` for every finding with its file and line. PLAN-58's L3-2 (a tab carried from one object into another) is already fixed on `main`: `applyRoute` resets `objectTab` (`runtime.ts:1521`, PLAN-59); it is not repeated here.

1. **The epic view follows its epic**
 - What:
   - `EpicView` loads in `onMount` only (`EpicView.svelte:237-246`, `loadEpic(true)`); `epicId` is derived from the route (`:38`) and `AppRouter` keeps one `EpicView` across routes, so an in-place change from epic A to epic B leaves A's head, rows, intent and acts on screen until an event refresh. Reload when `epicId` changes, with the old resource and error cleared and the loading state shown; `EpicIntentPanel` takes the epic as a key (`{#key epicId}` at `EpicView.svelte:796`) or reloads on the change.
   - Two overlapping loads: a rejected older read finishing last leaves the stale banner beside current data, and an older success can overwrite a newer resource (`:256,269,276`): guard each load by a sequence number.
   - The retry from the stale banner removes the banner at once with no busy state, and an event refresh on a failed initial read blanks the `ErrorState` while the bar says "still being read" (`:260,766-773`); with `?open=plan:x` and a failed read the compact form draws neither the banner nor the `ErrorState` and offers no retry (`:763,909`, `ObjectPage.svelte:29-44`); after a failed refresh the stale resource is kept, `scopeKnown` stays true and "Next plan" names `nextPlanId()` from the stale read (`WorkControlPanel.svelte:174`, `EpicView.svelte:277-279`): each state shows what is true.
   - The tabs carry no id and the panels no `aria-labelledby`, so `role=tabpanel` has no name (`Tabs.svelte:126`, `EpicView.svelte:777,781,799,845,891`); the retry and review-open buttons are plain `<button>`s where `Button.svelte` exists (`EpicView.svelte:766,736,857,877,950,972`); an invalid non-ISO `wait_since` becomes an invalid `datetime` attribute (`:988`); `FindingsReviewModal`'s refresh effect that removes the selected item mid-edit does not clear `dropReason`, `instruction`, `editing` and `promoting` the way `select()` does, so the next item can inherit the typed instruction (`FindingsReviewModal.svelte:72,100-108`; whether the daemon removes items is read at outline).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `EpicView.test.ts` switches the route from epic A to epic B in place and shows B's head, rows, intent and acts, A's gone; two overlapping loads with the older one last leave B's data and no stale banner; the retry shows a busy state; "Next plan" is disabled after a failed refresh; the tab panels have names; each fails with its production line reverted. Evidence order: `test-first`.
   - `epic-view.spec.js` shows the in-place switch through the command palette. Evidence order: `test-first`.
2. **The intent panels refresh by what moved**
 - What:
   - `refreshesEpicIntentView` and `refreshesIntentView` read the event name only (`components/intentEvents.ts:38-46`), so every `plan.revision_queued`, every plan's `phase.changed`, every epic's `epic.decomposition_changed` or `epic.register_changed` refetches every mounted panel, and a verdict, a builder answer or an in-phase revision shows only after the next phase change (PLAN-146 L3-1, PLAN-145 (2)). Read the event's subject (the closed catalogue's payload: the plan or epic the event names) and refetch only the panel whose record it names; settle at outline against `pm-api`'s `EventName` and the event record; if the stream carries no subject for an event, say so in the pull request and keep the name filter for that event.
   - A failed live refresh sets `errorCode`, which is checked first, so the panel is replaced by the error although the record is held (`EpicIntentPanel.svelte:85,112`; `PlanIntentPanel` and `OutlinePanel` the same, PLAN-145 L3-3): keep the held record and show the failure beside it with a retry.
   - Small residue of PLAN-145 read at outline and fixed where it is small: the "record changed" line is a plain `<p>` with no live region, the reason toggle has `aria-expanded` and no `aria-controls`; the head counts omit "not reviewed"; no-record states and the unknown `invalid_response` error have no catalogue text; the unused `apiClient` import of `BuilderCheckList.svelte:4` (re-read: its only occurrence) leaves.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `intentEvents.test.ts`, `EpicIntentPanel.test.ts`, `PlanIntentPanel.test.ts`, `OutlinePanel.test.ts` and `BuilderCheckList.test.ts`: an event naming another record triggers no read, an event naming the panel's record triggers one, a failed refresh keeps the record and offers a retry, the live region and the unused import are gone; each fails with its production line reverted. Evidence order: `test-first`.
3. **The over-size read fails closed and the chart is what the design says**
 - What:
   - `OverSizeDistributionResource` builds the buckets with `IntentJson.readObjects`, which returns `List.of()` when the key is absent (`OverSizeDistributionResource.java:188-192`, `IntentJson.java:317-320`), so the Java parser reads "no measured tasks" for a body that omits `buckets` and the mock's own validation accepts it, while the TypeScript reader throws `malformedRecord`: make the Java side refuse a missing `buckets` the way the design's empty state requires (an empty list is an answer, an absent key is not) and the two readers agree; the same for `distributions`.
   - `OverSizeChart`: the last bucket, bounded at 2 to 3 times, is labelled "2x and more" (`OverSizeChart.svelte:64-65`); `1 flagged of 300` reads `300 tasks · 1 flagged · 0 %` and 299 of 300 reads `100 %` (`:99`); `multiple()` uses `String(value)` and the layout widths are `rem` literals, not tokens (`:29-30,226`). A repeated role or `plan_id` in a daemon answer stops the render with a Svelte duplicate-key error (`routes/dev/components/+page.svelte:821`): refuse it where the answer is read.
   - The chart is not mounted: the project page's setup tab shows the over-size spread as hand-built text rows (`ProjectExplorerView.svelte:1590-1599`, PLAN-59 residue; PLAN-146: "chart on PLAN-59's setup tab"): mount `OverSizeChart` there.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `IntentJsonTest.java` / an `OverSizeDistributionResourceTest` (Generators, `@EnableGeneratorController`): a body without `buckets` is refused, an empty list is accepted; `OverSizeChart.test.ts` the labels and shares at 1 of 300 and 299 of 300, and a repeated role refused; `ProjectExplorerView.test.ts` shows the chart on the setup tab; each fails with its production line reverted. Evidence order: `test-first`.
4. **The tests that could not fail, and browser evidence**
 - What:
   - PLAN-145's tests that cannot fail: `OutlinePanel.test.ts:321-343` "keeps the newer outline when two reads answer out of order" (the read that resolves last is the first, so nothing is announced with or without the guard), the absence assertions `PlanIntentPanel.test.ts:283` `/vote/i` and `BuilderCheckList.test.ts:116,134-135`, the unproved "keeps an expanded reason open across a refresh", the unsubscribe on destroy and the non-404 rethrow of `readRecord`; run each against the production line removed and record the failing run; the e2e `intent.spec.js` never triggers a live refresh, an absent record or `not_run`: add those cases. The dev fixture renders once per theme and duplicates `data-testid`s (`routes/dev/components/+page.svelte:815-826`, the spec works around it with `.first()`/`.nth()`): one fixture per id.
   - Browser check (Playwright MCP) of the epic view switched in place, the intent panels with a live refresh, an absent record, `not_run` and a record with no non-goals, and the setup tab's chart, in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, after the last visual commit.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Each rewritten or added test is shown to fail with the production line it names reverted, the failing run cited; the full `-Pe2e` run at the head is green and cited. Evidence order: `mutation-after`.
   - The browser check is taken after the last visual commit and its files are named `{plan}-{target}-{before|after}`. Evidence order: `none`.
5. **A plan's intent record may carry no non-goals (the light lane)**
 - What:
   - The engine's intent record bounds `non_goals` at 0 to 20 and requires one only on the deep lane, through the server's check 7 on `submit:refine`; the light envelope's record may carry none (`pm-mcp-core` `doc/specification/phase-workflows/01-planning-phases-1-2.adoc:463,480`, `intent-record-v1.schema.json:29`, `IntentRecord.java:37`). The record carries no lane, so the client cannot tell a light record from a deep one, and both readers refuse the whole record when the list is empty: `PlanIntentResource` throws `non_goals must not be empty` (`PlanIntentResource.java:454-458`, read back at `:607`) and the TypeScript reader requires a non-empty array (`client.ts:2819`, `recordRequiredObjects` at `:2715-2721`). Decided by the operator on 2026-11-23 (decision 7, proposal 1), reversing the "at least one" of 2026-11-12 (PLAN-159): the client follows the engine's lane rule.
   - Accept an empty `non_goals` list (and an absent member, if the engine omits an empty list: settle at outline) on the plan intent record in both readers; keep the id and text rules for every item present. The plan panel shows the "Non-goals" section with a catalogue line ("none stated" / "keine genannt") when the list is empty, in place of an empty `<ul>` (`PlanIntentPanel.svelte:301-303`); the key is added to the design's texts table (`intent-and-outline-texts.adoc:65`) and to `intent.en.properties` and `intent.de.properties`.
   - The EPIC intent keeps its minimum of one (`model-work/08-epic-coordination.adoc:182`, `EpicIntentResource.java:150`, `client.ts:2945`): not touched. The engine's `EpicIntent.java:45` bounds it 0 to 30, looser than its own specification: a finding for the engine, reported in the pull request and the landing message, not decided here.
   - The design line becomes "at least one on the deep lane; the light lane's record may carry none, and the section then says so" (`doc/design/shared/intent-and-outline.adoc:104`); the other design files naming the minimum (`doc/design/README.adoc`, `starting-work.adoc`, `epic-view.adoc`) are read at outline and aligned where they name the PLAN rule, not the epic's.
   - The mock serves a light-lane record without non-goals (a scenario record: settle which at outline from `IntentScenarioTest.java`), so the browser check has one.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `IntentContractTest.java` (Generators, `@EnableGeneratorController`): an empty list round-trips and parses on a plan intent record, the test at `:912-920` that pins the refusal is rewritten, and the epic intent's refusal (`:311`) stays; `client.test.ts` reads a plan record with `non_goals: []`; `PlanIntentPanel.test.ts` shows the "none stated" line and no empty list; each fails with its production line reverted. Evidence order: `test-first`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite), `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head, and `-Pcoverage` at the head after the last code commit (minimum 80 % for each touched module).
- No new dependency or plugin. Web standards of `CLAUDE.md`; no static `style` attribute; every new text from the catalogue in English and German; the text guard baseline does not grow. Java: `CLAUDE.md` Code Standards (Generators for input data, `@EnableGeneratorController`, no Mockito, no generic `Exception`). Pre-1.0: no shim.
- A test is fed with the values the wire sends and the engine's event records; every new or rewritten assertion is shown to fail with the production line reverted, the failing run cited; a `test-first` clause cites a failing run at the plan's start, and where the base-sha instrument is unavailable, say so and do not relabel the clause.
- A finding that needs an engine or design decision (an event without a subject, the empty-state contract of the over-size body) is reported in the pull request and the landing message, never decided silently.
- Ports of this plan: mock web port 21790, app port 21791 (`20000 + 10 × 179`) (`AGENTS.md` § Ports). Other plans run at the same time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `EpicView` loads its epic in `onMount` only and renders `EpicIntentPanel` without a key — read at `pm-web-app/src/lib/views/EpicView.svelte:237-246,796`
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: EpicView.svelte:45-61 $effect reloads on epicId change and clears epic/resource/review (#542 43f0e416); :822 {#if resource} remounts EpicIntentPanel (:856) (3dea04b2)
- OBSERVED: `EpicIntentPanel` loads in `onMount` and refetches on every event whose name `refreshesEpicIntentView` accepts, with no look at the payload — read at `pm-web-app/src/lib/components/EpicIntentPanel.svelte:51-58` and `pm-web-app/src/lib/components/intentEvents.ts:38-46`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: EpicIntentPanel.svelte:51-58; intentEvents.ts:38-40,46-48 match the name only (3dea04b2)
- OBSERVED: `applyRoute` resets `objectTab` for every route, so PLAN-58 L3-2 is fixed — read at `pm-web-app/src/lib/stores/runtime.ts:1521`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: runtime.ts:1714-1721 objectTab: null in applyRoute (3dea04b2)
- OBSERVED: `BuilderCheckList.svelte` imports `apiClient` and uses it nowhere — read by `grep -c apiClient pm-web-app/src/lib/components/BuilderCheckList.svelte` (1)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: BuilderCheckList.svelte:4 imports apiClient, never uses it (3dea04b2)
- OBSERVED: `OverSizeDistributionResource` builds the buckets through `IntentJson.readObjects`, which returns an empty list for an absent key — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java:188-192`, `IntentJson.java:317-320`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: OverSizeDistributionResource.java:189; IntentJson.java:317-320 absent key -> List.of(); moot once over_size is removed (3dea04b2)
- OBSERVED: `OutlinePanel.test.ts` carries the out-of-order test at `:321-343` — read at `pm-web-app/src/lib/components/OutlinePanel.test.ts:319-324`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: OutlinePanel.test.ts:262-282 (file shrank to 284 lines in #566) (3dea04b2)
- HYPOTHESIS: the event stream carries a subject (the plan or epic id) for `plan.revision_queued`, `epic.decomposition_changed` and `epic.register_changed` — confirm/refute at `pm-api`'s `EventName` and the event record, `pm-web-app/src/lib/api/client.ts` `subscribeEvents` (verify-at-outline; if absent, deliverable 2's first item keeps the name filter for that event)
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: plan.revision_queued and phase.changed carry plan_id (06-event-catalogue.adoc:63,65); epic.register_changed / epic.decomposition_changed name no epic and are E-stream only, while the app subscribes /api/v1/events only (client.ts:4954-4984) (3dea04b2)
- HYPOTHESIS: the `OverSizeChart` is not mounted on the project page's setup tab — confirm/refute by `git grep -n OverSizeChart -- pm-web-app/src` (verify-at-outline)
  - verdict: contradicted | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: yes | evidence: premise gone: OverSizeChart.svelte and its test deleted by #566 (52aa9e4b); design dropped over_size (#570); over-size rows remain at ProjectExplorerView.svelte:1616-1637 (3dea04b2)
- HYPOTHESIS: the Java and the TypeScript readers of the over-size body disagree on an absent `buckets` — confirm/refute at `OverSizeDistributionResource.java:191` and `pm-web-app/src/lib/api/client.ts` (`getOverSize`) (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: OverSizeDistributionResource.java:189 accepts an absent buckets; client.ts:2992 recordObjects throws; moot once removed (3dea04b2)
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-144 (D0 and the Back removal), PLAN-176 (`ProjectExplorerView`), PLAN-85, 135 and 136 (`EpicView`, `routes.ts`) edit the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural (3dea04b2)
- OBSERVED: both readers refuse a plan intent record whose `non_goals` is empty — read at `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java:454-458` and `pm-web-app/src/lib/api/client.ts:2715-2721,2819`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanIntentResource.java:454-457 refuses empty non_goals; client.ts:2830 recordNonEmptyArray (3dea04b2)
- OBSERVED: the engine bounds `non_goals` at 0 to 20 and requires one on the deep lane only — read at `../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc:463,480` and `pm-core/src/main/java/de/planmarshall/core/store/IntentRecord.java:37`
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: pm-mcp-core phase-workflows/01-planning-phases-1-2.adoc:432 (0 to 20, at least one on the deep lane), :449 (3dea04b2)
- HYPOTHESIS: the engine writes an empty `non_goals` list as `[]` and not as an absent member, and the mock has a scenario record that can carry none — confirm/refute at `IntentCodec` in `pm-mcp-core` and `pm-mock-runtime/src/test/java/de/planmarshall/mock/IntentScenarioTest.java` (verify-at-outline; if the member is omitted, accept the absent member too)
  - verdict: unverifiable | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: IntentCodec and the scenario content not read; settle at outline (3dea04b2)

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/PlanIntentPanel.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/PlanIntentPanel.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/OutlinePanel.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/OutlinePanel.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/BuilderCheckList.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/BuilderCheckList.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/intentEvents.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/intentEvents.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Tabs.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/IntentJson.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentJsonTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/intent.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/intent.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentContractTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/IntentScenarioTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/shared/intent-and-outline-texts.adoc` — present at HEAD 3ae8a305
- OBSERVED: `doc/design/shared/epic-view.adoc` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `doc/design/README.adoc` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `doc/design/shared/starting-work.adoc` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)

## Dependencies and Sequencing

- Depends on: `PLAN-58`, `PLAN-145`, `PLAN-146` (shipped); `PLAN-144` and `PLAN-176` (landed first: they edit `EpicView`, `ProjectExplorerView` and `runtime.ts`).
- Overlaps with: `PLAN-135`, `PLAN-136`, `PLAN-137` (staged and parked: `EpicView.svelte`, `runtime.ts`), `PLAN-85` (`routes.ts`), `PLAN-178` (staged: no shared file). Sequenced after `PLAN-144` and `PLAN-176`.
- Adjacent to: PLAN-55 and PLAN-62 (parked: the `{#key planId}` wrapping of the plan panels is their call, PLAN-145 L3-2), the engine's event subject (`pm-mcp-core`, a finding if absent).


## Re-Grounding 2026-11-25

Re-grounded by the cleanup of 2026-11-25 (read-only sub-agent, contradictions spot-checked by the orchestrator) at `3dea04b2`, against the code and the design and specification changes since 2026-11-15: #570 (`20f971fe`: `over_size`, `epic.overlap_found`, `stale`→`unreadable`, the phase `sanity-check`), pm-mcp-core #607/#614 and plan-marshall-documentation #201/#202 ("observe facts instead of predicting them", two rounds), clients #578 (protected project configuration waits for the operator's decision) and the inbox messages `doc-review-2026-11-17-001` and `doc-review-2026-11-24-001`. **This section binds over the body where they differ.**

- Claims 0, 6, 7 contradicted: `EpicView` reloads on an epic change with a cleared resource and a generation guard (#542 `43f0e416`, `EpicView.svelte:45-61,300-336`); the plan events carry `plan_id` but `epic.register_changed`/`epic.decomposition_changed` name no epic and travel only on the epic stream, which the app does not subscribe to; `OverSizeChart` and its test were deleted by #566 and the design dropped the `over_size` distribution (#570).
- **D3 is void and deleted** (chart, its labels, the fail-closed Java reader, the duplicate-role refusal, the mount). The `over_size` removal is its own package (`doc-review-2026-11-24-001` § 1); this plan does not edit those files. `OverSizeChart.svelte`, `OverSizeChart.test.ts` and `OverSizeDistributionResourceTest.java` leave the surface.
- **D1** shrinks to what is open: the stale-banner retry as a busy-aware control (`EpicView.svelte:826-834`), `scopeKnown`/`nextPlan` from a stale resource (`:773,:776`), tab `id`s and `aria-labelledby` (`Tabs.svelte:126-135`), the `FindingsReviewModal` reset; a regression test of the existing reload effect is welcome. The wait reason lines (`:410-413`) are PLAN-191's: rebase.
- **D2**: plan panels filter on the payload's `plan_id` (`phase.changed`, `plan.revision_queued`); for the epic panel the runtime-wide stream cannot deliver the two epic events: subscribe to the epic stream or refetch on `epic.row_changed`/`plan.revision_queued`, said in the PR.
- **D2/D4 mount**: `PlanIntentPanel`, `OutlinePanel` and `BuilderCheckList` are mounted nowhere in the plan view; the Intent tab is PLAN-55's: depends on PLAN-55, or confine the browser check of the plan panels to the dev fixture and the epic view. D4 drops the chart items; the other tests-that-cannot-fail stay.
- **D5 survives whole**; also the design line "At least one is required" (`intent-and-outline.adoc:104`), `doc/design/README.adoc:64`, `shared/starting-work.adoc`, `shared/epic-view.adoc`; decide absent vs `[]` (`IntentJson.java:317-320`).
- **Order**: after PLAN-191, the `over_size`/`added_dependencies` removal package and PLAN-55; re-ground then.
- Removed from the Expected Surface (void or replaced, see above): pm-web-app/src/lib/components/OverSizeChart.svelte, pm-web-app/src/lib/components/OverSizeChart.test.ts, pm-api/src/test/java/de/planmarshall/api/intent/OverSizeDistributionResourceTest.java, pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-179-web-intent-panels-and-epic-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

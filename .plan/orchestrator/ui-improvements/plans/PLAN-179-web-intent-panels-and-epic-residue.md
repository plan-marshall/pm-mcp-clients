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
   - Browser check (Playwright MCP) of the epic view switched in place, the intent panels with a live refresh, an absent record and `not_run`, and the setup tab's chart, in `en` and `de`, light and dark, at `1440 × 900` and `390 × 844`, after the last visual commit.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Each rewritten or added test is shown to fail with the production line it names reverted, the failing run cited; the full `-Pe2e` run at the head is green and cited. Evidence order: `mutation-after`.
   - The browser check is taken after the last visual commit and its files are named `{plan}-{target}-{before|after}`. Evidence order: `none`.

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
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicView.svelte:237-246 onMount subscribes and calls loadEpic(true) once; :796 renders <EpicIntentPanel {epicId} /> and the file has no {#key} (origin/main 3ae8a305)
- OBSERVED: `EpicIntentPanel` loads in `onMount` and refetches on every event whose name `refreshesEpicIntentView` accepts, with no look at the payload — read at `pm-web-app/src/lib/components/EpicIntentPanel.svelte:51-58` and `pm-web-app/src/lib/components/intentEvents.ts:38-46`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EpicIntentPanel.svelte:51-58 onMount load() and subscribeEvents -> refreshesEpicIntentView(event); intentEvents.ts:38-40 returns INTENT_EPIC_EVENTS.includes(event), name only (origin/main 3ae8a305)
- OBSERVED: `applyRoute` resets `objectTab` for every route, so PLAN-58 L3-2 is fixed — read at `pm-web-app/src/lib/stores/runtime.ts:1521`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: runtime.ts:1521 objectTab: null in the applyRoute update (PLAN-59 #538); also :217 initial and :1556 settings-only variant (origin/main 3ae8a305)
- OBSERVED: `BuilderCheckList.svelte` imports `apiClient` and uses it nowhere — read by `grep -c apiClient pm-web-app/src/lib/components/BuilderCheckList.svelte` (1)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: BuilderCheckList.svelte: grep -c apiClient = 1, the import line (origin/main 3ae8a305)
- OBSERVED: `OverSizeDistributionResource` builds the buckets through `IntentJson.readObjects`, which returns an empty list for an absent key — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java:188-192`, `IntentJson.java:317-320`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: OverSizeDistributionResource.java:188-192 IntentJson.readEach(IntentJson.readObjects(map, FIELD_BUCKETS), ...); readObjects' empty-list answer for an absent key is the PLAN-146 landing's re-read (IntentJson.java:317-320), not re-read here
- OBSERVED: `OutlinePanel.test.ts` carries the out-of-order test at `:321-343` — read at `pm-web-app/src/lib/components/OutlinePanel.test.ts:319-324`
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: OutlinePanel.test.ts:319-324 declares resolveFirst and resolveSecond around the out-of-order case; the test's non-failing nature is the PLAN-145 landing's reading, not re-run
- HYPOTHESIS: the event stream carries a subject (the plan or epic id) for `plan.revision_queued`, `epic.decomposition_changed` and `epic.register_changed` — confirm/refute at `pm-api`'s `EventName` and the event record, `pm-web-app/src/lib/api/client.ts` `subscribeEvents` (verify-at-outline; if absent, deliverable 2's first item keeps the name filter for that event)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: EventName and the event record not re-read here; settled at outline
- HYPOTHESIS: the `OverSizeChart` is not mounted on the project page's setup tab — confirm/refute by `git grep -n OverSizeChart -- pm-web-app/src` (verify-at-outline)
  - verdict: corroborated | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep OverSizeChart over pm-web-app/src finds only the component, its test and routes/dev/components/+page.svelte:867; no ProjectExplorerView mount (origin/main 3ae8a305)
- HYPOTHESIS: the Java and the TypeScript readers of the over-size body disagree on an absent `buckets` — confirm/refute at `OverSizeDistributionResource.java:191` and `pm-web-app/src/lib/api/client.ts` (`getOverSize`) (verify-at-outline)
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: client.ts has malformedRecord throws (:2527-2621) for the TS readers; getOverSize's own absent-buckets branch is read at outline
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; PLAN-144 (D0 and the Back removal), PLAN-176 (`ProjectExplorerView`), PLAN-85, 135 and 136 (`EpicView`, `routes.ts`) edit the same files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 3ae8a305f6424f9b53c15ce81547ca675f6f9946 | by: ui-improvements/analyze | rescoped: n/a | evidence: procedural verify-first clause; PLAN-144 running, PLAN-176 staged

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
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/Tabs.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/IntentJson.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentJsonTest.java` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/OverSizeDistributionResourceTest.java` — absent at HEAD 3ae8a305; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/intent.en.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-api/src/main/resources/messages/intent.de.properties` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 3ae8a305
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — present at HEAD 3ae8a305

## Dependencies and Sequencing

- Depends on: `PLAN-58`, `PLAN-145`, `PLAN-146` (shipped); `PLAN-144` and `PLAN-176` (landed first: they edit `EpicView`, `ProjectExplorerView` and `runtime.ts`).
- Overlaps with: `PLAN-135`, `PLAN-136`, `PLAN-137` (staged and parked: `EpicView.svelte`, `runtime.ts`), `PLAN-85` (`routes.ts`), `PLAN-178` (staged: no shared file). Sequenced after `PLAN-144` and `PLAN-176`.
- Adjacent to: PLAN-55 and PLAN-62 (parked: the `{#key planId}` wrapping of the plan panels is their call, PLAN-145 L3-2), the engine's event subject (`pm-mcp-core`, a finding if absent).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-179-web-intent-panels-and-epic-residue.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

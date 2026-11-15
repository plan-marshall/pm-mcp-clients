# PLAN-146: Web: Intent and Outline, the Epic's Intent and the over_size Distribution

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 (the web intent and outline package, part 2). It follows `PLAN-145` (the client methods and the catalogue area) and
> `PLAN-159` (the distribution's absent proposal in the contract and the mock). The D6 design session it waited for has merged
> (#403, `8568b56e`, 2026-11-10). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

`PLAN-145` delivered the client methods and the plan's panels. This plan delivers the epic's side and the chart: the epic's
intent (destination, done-when checks with their verdicts and evidence, non-goals, what is not yet specified, each plan's
criterion) and the `over_size` distribution as a small bar chart readable without colour and at phone width. PLAN-58 places
the epic panel in the epic's Intent tab (the sources at its top are PLAN-58's); PLAN-59 mounts the chart on the project page's
setup tab (`/projects/{id}?tab=setup`), read-only, one per role whose multiple the project sets — D6 decided that home, not the
machine settings' roles aspect (PLAN-60). Until then both are shown on `/dev/components` with live mock data.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/intent-and-outline.adoc` (#epic :220-239, #oversize
:241-263, #web :273-279, #contract :286-289, #not), `doc/design/shared/intent-and-outline-texts.adoc` (the `intent.epic.*` and
`intent.oversize.*` keys, :29 and :237-296; #states), `doc/design/shared/project-explorer.adoc#project-page` (:108, the setup tab holds the
read-only distribution per role), `doc/design/web-app/components-tags.adoc#status-label`, `dataviz` guidance for the chart. D6 answer
Q4 (a) and (d) of `landings/PLAN-141-findings.md` § Answers settles the check's verdict and the chart's home.

1. **The epic's intent panel**
 - What: `EpicIntentPanel.svelte` (prop `epicId`) shows the destination (one sentence), the done-when checks each with its verdict
   as a `StatusLabel` state (`state.check.pending` neutral before the completion review, `state.check.met` success or
   `state.check.not_met` danger after, never a progress bar) and its evidence as text (a path or a `pm://` URI, never turned into a
   link), the non-goals, the not-yet-specified questions shown as questions, and each plan's done-when with the plan's title. Every
   text is plain text after `stripControlSequences`. The epic's sources are not this panel's: PLAN-58 puts them at the top of the tab.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `EpicIntentPanel.test.ts`: pending versus decided checks, evidence with several entries and none, an empty not-yet-specified
     list, a plan without a done-when, and a text with markup rendered as text. It fails on a revert. Evidence order: `test-first`.
2. **The `over_size` chart**
 - What: `OverSizeChart.svelte` (prop `projectId`, or the record) draws the buckets of the distribution in the runtime's order: an
   empty bucket is left out, the last is open-ended (`Nx and more`), the buckets the proposed multiple would flag are marked, and
   the counts, the flagged share (flagged tasks over all tasks, as a count and a percentage) and the direction (`3x → 2x`
   lowers the multiple and flags more) stand in text, so it reads without colour and at phone width. It uses tokens only.
   It never recomputes a bucket from raw tokens. The chart is **read-only**: no act proposes or changes a multiple (no client
   designs one yet). It is named by its resource (`OverSizeDistributionResource`, per project and role).
   - **Without a proposal** (`PLAN-159`: `proposed_multiple` absent): the title is `intent.oversize.title_current` (`{role} ·
     over_size {current}x`), the chart shows the distribution under the multiple in force, and there is no flagged comparison, no
     marked bucket, no flagged share and no direction; with a proposal the title is `intent.oversize.title`. A role without measured
     tasks shows `intent.oversize.empty` instead of a chart.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `OverSizeChart.test.ts`: the buckets of the mock record, an empty bucket left out, the open-ended bucket, the marked buckets,
     the share text, the lower and the raise direction, a record without a proposal (the current title, no mark, no share, no
     direction), a role with no tasks (`intent.oversize.empty`), no act, and no colour-only meaning (text for every mark).
     Evidence order: `test-first`.
3. **Fixtures and browser evidence**
 - What: fixtures of both components on `routes/dev/components/+page.svelte` reading the mock's `relay-hardening` and
   `pm-mcp-clients` records through the real client (the distribution with a proposal on `running-work`, without one on
   `fully-onboarded`, as `PLAN-159` serves them), in both themes and both locales; `intent.spec.js` (created by PLAN-145) gains the
   epic panel and the chart; an axe check of both at `390` px.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `intent.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set. Evidence order: `test-first`.
   - Browser check (Playwright MCP) of both fixtures on `running-work` at `1440 × 900` and `390 × 844`, German and English, both
     themes. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin (no chart library: the chart is markup and CSS from the tokens). Web standards of `CLAUDE.md`;
  no static `style` attribute; every text from the catalogue in English and German; the text guard baseline does not grow.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21460, app port 21461 (`20000 + 10 × 146`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in
  a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `EpicIntentResource` carries epic_id, destination, done_when (text, verdict `pending|met|not_met`, evidence), non_goals, not_yet_specified and plans (plan_id, title, done_when) — read at `pm-api/src/main/java/de/planmarshall/api/intent/EpicIntentResource.java:51-72`, `DoneWhenCheck.java`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: EpicIntentResource :41-47 epicId, destination, doneWhen, nonGoals, notYetSpecified, plans; DoneWhenCheck verdict pending|met|not_met with evidence
- OBSERVED: `OverSizeDistributionResource` carries project_id, role, current_multiple, proposed_multiple and buckets (label, from_multiple, to_multiple, task_count, flagged_tasks) and no total or share field — read at `OverSizeDistributionResource.java:50-54`, `TokenBucket.java:48-52`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: OverSizeDistributionResource carries projectId, role, currentMultiple, proposedMultiple, buckets; total and share are derived accessors (:99,:116), not wire fields
- OBSERVED: the mock serves `GET /api/v1/epics/relay-hardening/intent` and `GET /api/v1/projects/pm-mcp-clients/metrics/oversize` in `running-work.json` and `fully-onboarded.json` — read at `pm-mock-runtime/src/main/resources/scenarios/running-work.json:1298,1358`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Mock handles epic intent and oversize routes (MockRuntimeEngine:550-553,5587); fixtures running-work.json:1402,1462 and fully-onboarded.json:1061,1121
- OBSERVED: `machine-settings.adoc` has no role-multiple line, so the chart's home is D6's decision — read by `git grep -n -i "over_size\|oversize" -- doc/design/web-app` (empty at `1cc0e10e`)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: machine-settings.adoc has no over_size line; README.adoc:96,461 now name the chart (the earlier empty grep is stale)
- OBSERVED: D6 put the chart on the project page's setup tab, read-only, per role, named by its resource, with the absent-proposal case (`intent.oversize.title_current`, no flagged comparison) and `intent.oversize.empty` for a role without measured tasks; the done-when check's verdict is a `StatusLabel` state `state.check.*` — read at `doc/design/shared/intent-and-outline.adoc:244,278-279,289`, `doc/design/shared/project-explorer.adoc:108`, `doc/design/shared/intent-and-outline-texts.adoc:257-296,300-336` (`4a84f727`, D6 merged `8568b56e`)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: project-explorer.adoc:108 setup tab holds the read-only over_size per role; texts doc has title_current and empty (:261,:265)
- OBSERVED: at `main` `4a84f727` `OverSizeDistributionResource` requires a positive `proposed_multiple` and always writes it; `PLAN-159` makes it optional — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java:41-69,154`
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: Superseded by PLAN-159: proposedMultiple is @Nullable (OverSizeDistributionResource:48) and written only when present (:143) | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- HYPOTHESIS: the runtime serves one distribution per project and role, and the setup tab can read every role's distribution the project sets through the route `GET /api/v1/projects/{id}/metrics/oversize` (today one record with one `role`, no role parameter) — confirm/refute at `pm-api/.../intent/OverSizeDistributionResource.java`, `MockRuntimeEngine.java` § `handleIntentRoutes` and `PLAN-159`'s landing (verify-at-outline; a route without a role selector is reported to the orchestrator, not invented here)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: Route takes a project id only (MockRuntimeEngine:5587-5591) and serves one record with one role, no role selector, so one request cannot give every role | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- OBSERVED: no chart component exists in the web app — read by `git ls-files pm-web-app/src/lib/components` (no chart)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: No chart component under pm-web-app/src/lib/components
- HYPOTHESIS: the flagged share is the sum of `flagged_tasks` over the sum of `task_count`, which the design allows ("the sum of the flagged tasks over all tasks") without recomputing from raw tokens — confirm/refute at `doc/design/shared/intent-and-outline.adoc` § `buckets` (:260-263) (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: intent-and-outline.adoc:261 defines the share as sum of flagged tasks over all tasks; pm-api flaggedShare() (:116) does the same on buckets
- HYPOTHESIS: the plan's epic panel can be shown for an epic whose intent the daemon does not serve yet with an empty state, as for a plan — confirm/refute at `pm-api/.../intent/IntentContractTest.java` and the D6 documents (verify-at-outline)
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Whether the daemon serves an epic without an intent record cannot be settled by reading
- Verify-first clause: re-read `intent-and-outline.adoc` (#oversize, #web) and the landings of `PLAN-145` and `PLAN-159` at the then-current HEAD. The verdict is a `StatusLabel` state (no new component); if `proposed_multiple` is still required in `pm-api`, stop and loop back.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: proposed_multiple is optional in pm-api (PLAN-159 landed); PLAN-145 landing is pending (sequencing, not a premise)

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e (read; methods are PLAN-145's)
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `pm-api/src/main/resources/messages/intent.en.properties` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `pm-api/src/main/resources/messages/intent.de.properties` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java` — present at HEAD da8c8644 (D0: the roles list or the role parameter)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentContractTest.java` — present at HEAD da8c8644 (D0: its contract test)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD da8c8644 (D0: serve every role)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD da8c8644 (D0: one record per role)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD da8c8644 (D0: one record per role)

## Dependencies and Sequencing

- Depends on: `PLAN-145` landed (the client methods, the catalogue area, the e2e file); `PLAN-159` landed (the optional `proposed_multiple` and the mock's distribution without a proposal); the D6 design session merged — satisfied (#403, `8568b56e`: the chart's home is the project page's setup tab).
- Overlaps with: `routes/dev/components/+page.svelte` (PLAN-45 running, PLAN-130 staged; PLAN-145 before this plan).
- Adjacent to: `PLAN-58` (places the epic panel in the epic's Intent tab, the sources above it); `PLAN-59` (mounts the chart on the project page's setup tab `/projects/{id}?tab=setup`, read-only, one per role; D6 — not `PLAN-60`); the decomposition review, which shows the same epic intent (the views package; it reuses `EpicIntentPanel`).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since. Settle each HYPOTHESIS.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 5 contradicted: Superseded by PLAN-159: proposedMultiple is @Nullable (OverSizeDistributionResource:48) and written only when present (:143)
- Claim 6 contradicted: Route takes a project id only (MockRuntimeEngine:5587-5591) and serves one record with one role, no role selector, so one request cannot give every role
- Claim 5 superseded by PLAN-159. Claim 6 refuted: the oversize route takes a project id and serves one role; one request cannot give every role.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-146-web-intent-epic-and-oversize.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the D6 session is PLAN-141.

## D6 Merged 2026-11-11 (Amendment)

D6 (`PLAN-141`, #403, `8568b56e`) answered Q4 (a) and (d): a done-when check's verdict is a `StatusLabel` state `state.check.*`
(pending neutral, met success, not met danger); the `over_size` chart stands on `PLAN-59`'s project setup tab, read-only, one per role,
named by its resource (`OverSizeDistributionResource`, per project and role); without a proposal it shows the distribution under the
multiple in force and no flagged comparison; the contract grows to carry an absent proposal (`PLAN-159`). The deliverables above are
amended in place. Removed: "PLAN-59 or PLAN-60, as D6 decided" (PLAN-59 it is). New dependency: `PLAN-159`. Surface: unchanged.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-145` lands (`PLAN-159` and D6 met).

**Operator decision 2026-11-14: extend the contract and the mock.** Claim 6 is refuted: the over-size route takes a project id and serves one role (`MockRuntimeEngine` ~:5587), while the design wants one chart per role. Add **D0** as the first deliverable:
- give the over-size answer a roles list (or a `role` parameter) in `pm-api`, with its contract test;
- make the mock serve every role of the project;
- then draw one chart per role as designed.

D0's surface adds the over-size record in `pm-api`, `MockRuntimeEngine.java` and its scenario test. Do not run it alongside `PLAN-140` D-R134 or `PLAN-162`, which touch the engine.

## Updated from the PLAN-162 landing 2026-11-15

`PLAN-162` landed as `6b97acd2` (#444): **the hold named in the next section is released** (the engine and `running-work.json` are free of a running plan; the operator
decided 2026-11-15 to emit with `PLAN-30` treated as parked, so the two files `MockRuntimeEngine.java` and `fully-onboarded.json` it shares with `PLAN-30` are a
rebase for whichever lands second). The positions this spec cites moved; read them at HEAD, not from here: `handleIntentRoutes` is at `MockRuntimeEngine.java:6232`
and the over-size route at `:6258` (the claims above say 5587-5591), `FIXTURE_OVERSIZE` at `:619`; in `running-work.json` the epic intent route is at `1400` and the
over-size route at `1460` (shifted by the `model_differs` entry near `1054`). `applyStateEffectOf` now returns a refusal string (400 `invalid_arguments`) D0 can reuse,
and `ScenarioLoader` now runs `validatePlanChanges`: a new fixture that names a `change` for a captured plan stays consistent with its `plan_changes` entry.
`PLAN-163` D0 (staged, later) edits the same two mock files: do not run them at once.

## Folded from the PLAN-145 landing 2026-11-15

`PLAN-145` landed as `da8c8644` (#443; `landings/PLAN-145.md`). The orchestrator's analysis of it (leaf plus re-reads on the merged tree) leaves
these for this plan. **Hold:** PLAN-146 is not emitted while `PLAN-162` runs — D0 edits `MockRuntimeEngine.java` and `running-work.json`, both in
`PLAN-162`'s declared surface (the 2026-11-14 operator decision above says the same). The Expected Surface now names D0's files (5 added, 15 in all).

- The panels' client side exists: `getEpicIntent`, `getOverSize` and their readers (`client.ts`), tested against the mock only; there is no panel and no
  browser evidence. The area keys `intent.epic.*` and `intent.oversize.*` are in both packs and used nowhere; the catalogue test pins the design
  document's keys (65 `intent.*`, 7 verdict words), **not 68/75** — read the counts from the document, never from this spec.
- **Source-less clauses, check each against the contract before the outline** (the D3 of PLAN-145 failed on one): the design's `intent.epic.review`
  ("completion review") has no field in `EpicIntentRecord`; the design's `intent.oversize.empty` ("a role without measured tasks") has no contract form,
  because `OverSizeDistributionResource` refuses empty `buckets` and the TS reader (`recordRequiredObjects`) does the same — decide the representation
  (a 404 read as `null`, or relax the contract in D0 with a reader test) and say so.
- **One role per route:** `getOverSize(projectId)` serves one role; D0 settles the roles list or the `role` parameter and the client method follows.
- **The open-ended bucket** (`intent.oversize.bucket_open`) has no flag on the record (the runtime serves `{label: '3x and more', from: 3, to: 6}`): fix a
  rule that does not recompute (the last bucket, or its label) and test it. **The share** needs a rounding rule for the percentage; the direction
  (`lower` or `raise`) is derived from `current` and `proposed`, since `flagsMore` is not on the wire.
- **Refresh:** `EpicIntentRecord` has no `revision`, so `changedRevision` cannot serve the epic panel, and `INTENT_PLAN_EVENTS`
  (`phase.changed`, `plan.revision_queued`) is plan-scoped. The epic panel needs its own change detector and event set, each name a member of
  `EventName`, held by a test as `intentEvents.test.ts` does. Do not copy the plan panels' unfiltered reload (every plan's `phase.changed` refetches
  every mounted panel; see the Open Defects in `epic.md`).
- A done-when check's evidence is text only (a path or `pm://`), never a link, as a fact's evidence is in the plan panel. `state.check.*` words exist in
  `common.*.properties` but no `StatusLabel` mapping for `pending` (neutral), `met` or `not_met` is written yet.
- Do not copy two things from PLAN-145's code: the `{x: ''}` argument plus a literal `, ` in a message (an i18n workaround) and the dev fixture's duplicate
  ids per theme (`criterion-AC-001` twice in the DOM). Both plans edit `routes/dev/components/+page.svelte` and `client.ts`: rebase on `main` first.
- Evidence order: a fixture and an e2e spec the same plan writes cannot be `test-first` (PLAN-145 D4 was `mutation-after` for that reason); say which
  clause is `test-first` (the component tests) and which is `mutation-after` (the e2e extension).

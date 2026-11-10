# PLAN-145: Web: Intent and Outline, the Client Methods, the Plan's Intent and the Outline Panels

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 (the web intent and outline package, part 1; the epic's intent and the `over_size` chart are `PLAN-146`). It
> WAITS for the D6 design session to be merged with `intent-and-outline.adoc` AGREED. This spec is SELF-SUFFICIENT: the emitted
> command is a one-line pointer and carries no brief.

## Objective

The runtime serves a plan's intent record, its outline record, the epic's intent and the `over_size` distribution
(`pm-api/.../api/intent/*`, and the mock serves them), and `intent-and-outline.adoc#web` designs how the web app shows them. No
web code reads or draws any of it: `client.ts` has no call, and no component exists. This plan adds the client methods and the
panels of the plan's side: the intent (goal, criteria with verdicts, non-goals, choices by decider, open questions, deferred
items), the builder check and the outline (deliverables, files, dependencies, interfaces, key links). PLAN-55 and PLAN-62 only
place the panels in the plan's Intent tab. The panels are shown on `/dev/components` with live mock data until they are placed.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/intent-and-outline.adoc` (all; its web part is
#web), `doc/design/shared/intent-and-outline-texts.adoc` (written by D6), the D6 answers on the verdict tag and the left-out
fields, `doc/design/web-app/components-tags.adoc`, `doc/design/web-app/components.adoc` (#new-component).

1. **The client methods, the types and the catalogue area**
 - What:
   - `client.ts` gains `getPlanIntent(planId)`, `getPlanOutline(planId)`, `getEpicIntent(epicId)` and
     `getOverSize(projectId)` for `GET /api/v1/plans/{id}/intent`, `/api/v1/plans/{id}/outline`, `/api/v1/epics/{id}/intent` and
     `/api/v1/projects/{id}/metrics/oversize`, with types that mirror the `pm-api` records and fail closed on a malformed record
     as the client's other calls do.
   - A catalogue area `intent` in `pm-api/src/main/resources/messages/intent.{en,de}.properties` with the keys of the D6 texts
     document, and the web side's registration of the area.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `client.test.ts` pins the four requests and the shapes against the mock's records (every field of `PlanIntentResource`,
     `OutlineResource`, `EpicIntentResource`, `OverSizeDistributionResource`), an absent record and a malformed one. Evidence
     order: `test-first`.
   - The catalogue test passes with English and German agreeing key for key. Evidence order: `test-first`.
2. **The plan's intent panel**
 - What:
   - `PlanIntentPanel.svelte` (prop `planId`) shows the order of `intent-and-outline.adoc#intent`: the head (plan, revision, the
     goal, the counts by verdict, absent and `not reviewed yet` before the review), the criteria (id, the EARS sentence verbatim,
     the deliverables that satisfy it, the verdict in the tag D6 chose, the reason one expansion away; `no deliverable yet` where
     none), the non-goals (`NG-1…` by position), the choices grouped by decider (operator and quorum first, the model's calls
     apart and never styled like the operator's, the options weighed in the record's order, never sorted), the open questions and the
     deferred items with their reasons.
   - Every text of a record is plain text after `stripControlSequences`, never HTML; a field the record does not carry is left
     out; the client never derives a verdict from a deliverable's state.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `PlanIntentPanel.test.ts`: each verdict and `not reviewed yet`, a `not_met` and `not_verified` with their reason, a criterion
     without a deliverable, the deciders in order, a model call apart, a text with control sequences and markup rendered as
     text, and a record without criteria. It fails on a revert. Evidence order: `test-first`.
3. **The builder check and the outline**
 - What:
   - `BuilderCheckList.svelte`: the sorts (`answered by the record`, `the builder's call`, `needs you`), the builder's
     recommendation marked as the builder's and preselecting nothing, the resolution state (`open`, `answered`, `deferred`), and the
     check's state (`pending`, `answered`, `not_run`, the last shown as evidence and never as a pass or a hold).
   - `OutlinePanel.svelte` (prop `planId`): the deliverables (number and title, gloss, affected files with their intent
     `read`, `write-new`, `write-replace`, `write-conditional`, `delete` one per line, depends on, satisfies, verification), `satisfies no
     criterion`, `missing deliverable <n>`, the added dependencies with coordinates, version, the adding deliverable and the
     justification, the interfaces, the key links and a missing key link as `missing: <link>`. A path is never turned into a link.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `BuilderCheckList.test.ts` and `OutlinePanel.test.ts` cover each sort, state and edge above (empty, single, duplicate
     dependency, missing deliverable, missing key link). Evidence order: `test-first`.
4. **Live, one step away, and the browser evidence**
 - What:
   - The panels refetch when the plan's events say the plan changed and show `the record changed` once; an open question and the
     scroll position keep their place. Every reference is a step away: a criterion to its deliverables, a deliverable to its
     criteria, a decision's question to `/decisions/{id}` (PLAN-143); none flattens a record into prose.
   - A fixture of both panels on `routes/dev/components/+page.svelte` reading the mock's `fix-relay` records through the real
     client, in both themes and both locales; a new `pm-web-e2e/tests/intent.spec.js` on `running-work`.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `intent.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set. Evidence order: `test-first`.
   - Browser check (Playwright MCP) of the fixture on `running-work` at `1440 × 900` and `390 × 844`, German and English, both
     themes. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for
     a failed read, never `0`); a confirmed `0` or every open issue dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin. Web standards of `CLAUDE.md` (no `innerHTML`/`{@html}` with a model's text); no static `style`
  attribute; every text from the catalogue in English and German; the text guard baseline does not grow; the closed vocabulary of
  `components.adoc` (a new component only through `#new-component`). No new colour: the tokens only.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`); Java tests JUnit 5 only with input from `Generators`.
- Ports of this plan: mock web port 21450, app port 21451 (`20000 + 10 × 145`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the merge question is asked only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. Merge only on the operator's word. Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in
  a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `client.ts` has no intent, outline or over_size call and no web source outside tests mentions intent — read by `git grep -n -i "intent\|outline" -- pm-web-app/src/lib/api/client.ts` and `git grep -n -i intent -- pm-web-app/src ':!*.test.ts'` (both empty)
- OBSERVED: the contract is `PlanIntentResource` (plan_id, revision, goal, criteria, non_goals, decisions, open_questions, deferred, builder_check), `OutlineResource` (plan_id, revision, summary, deliverables, added_dependencies, interfaces, key_links, missing_key_link), `EpicIntentResource`, `OverSizeDistributionResource` — read at `pm-api/src/main/java/de/planmarshall/api/intent/*.java`
- OBSERVED: a decision carries id, text, decider, options_weighed and question_id only; the deciders are `operator|quorum|model|fact`; criterion statuses are `met|not_met|not_verified`; non-goals are strings; the builder check's resolution is `pending|answered|not_run` — read at `PlanIntentResource.java:60-97`, `IntentDecider.java:26-35`, `CriterionStatus.java:26-32`, `BuilderCheckResource.java:40-60`
- OBSERVED: the mock serves the four GET routes from fixtures in `running-work.json` and `fully-onboarded.json` for `fix-relay`, `relay-hardening` and `pm-mcp-clients`, answering any other id from the same fixture — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5386-5440`, `pm-mock-runtime/src/main/resources/scenarios/running-work.json:1089,1213,1298,1358`, `fully-onboarded.json:848,972,1057,1117`
- OBSERVED: `EventName` names no intent or outline event — read by `git grep -n -i intent -- pm-api/src/main/java/de/planmarshall/api/event/EventName.java` (empty)
- OBSERVED: no intent catalogue area exists — read by `git ls-files pm-api/src/main/resources/messages` (no `intent.*`)
- OBSERVED: `stripControlSequences` is the safe-text helper — read at `pm-web-app/src/lib/utils/outputSafety.ts:6`
- OBSERVED: no view uses `Tabs` — read by `git grep -n Tabs -- pm-web-app/src/lib/views` (non-test: empty)
- OBSERVED: `intent-and-outline-texts.adoc` is absent at HEAD; D6 creates it — read by `git ls-files doc/design/shared`
- HYPOTHESIS: the daemon answers a plan without an intent record with a not-found error the client can map to "no record yet" — confirm/refute at `../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc` § `intent-record` and `pm-api/.../intent/IntentContractTest.java` (verify-at-outline)
- HYPOTHESIS: the plan's events (`plan.changed`, `phase.changed`) signal a new intent or outline revision — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` (verify-at-outline)
- HYPOTHESIS: a new catalogue area needs a registration beyond the properties files — confirm/refute at `pm-web-app/src/lib/i18n/catalogue.test.ts` and `pm-web-app/src/lib/i18n/index.ts` (verify-at-outline)
- Verify-first clause: re-read the D6 documents at the then-current HEAD; if the verdict tag is a new component, add its deliverable first (`components.adoc#new-component`); if the record gained ADR relation, evidence or vote support, show them.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/PlanIntentPanel.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PlanIntentPanel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/BuilderCheckList.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/BuilderCheckList.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OutlinePanel.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OutlinePanel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/i18n/catalogue.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/utils/outputSafety.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/intent.en.properties` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/intent.de.properties` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java` — present at HEAD 1cc0e10e (read; changed only if D6 decides the contract grows)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: the D6 design session merged (`intent-and-outline.adoc` AGREED, its texts document, the verdict tag); the D1 foundation parts that deliver the tag family states and `Link`; `PLAN-143` for the `/decisions/{id}` address (soft: the link is a plain address until then).
- Overlaps with:
  - `pm-web-app/src/lib/api/client.ts`: PLAN-49, 51, 52, 53 and 55 each add methods; the blocks are disjoint.
  - `routes/dev/components/+page.svelte` (PLAN-45 running, PLAN-130 staged).
  - `pm-api/src/main/resources/messages/`: a new file, no clash.
- Adjacent to: `PLAN-55` and `PLAN-62` (they place the panels in the plan's Intent tab and link the phase narrative to it; they do not draw them); `PLAN-146` (the epic's side and the chart); the terminal client (a separate package, not here).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since. Settle each HYPOTHESIS. Re-check that `intent-and-outline.adoc` is `AGREED` and that its texts document exists.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-145-web-intent-client-and-plan-panels.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

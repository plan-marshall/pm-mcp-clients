# PLAN-145: Web: Intent and Outline, the Client Methods, the Plan's Intent and the Outline Panels

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 (the web intent and outline package, part 1; the epic's intent and the `over_size` chart are `PLAN-146`). The D6
> design session it waited for has merged (#403, `8568b56e`, 2026-11-10; `intent-and-outline.adoc` AGREED, its texts document
> written); it now WAITS for `PLAN-159` (the client contract and the mock grow to carry what D6 shows). This spec is
> SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

The runtime serves a plan's intent record, its outline record, the epic's intent and the `over_size` distribution
(`pm-api/.../api/intent/*`, and the mock serves them), and `intent-and-outline.adoc#web` designs how the web app shows them. No
web code reads or draws any of it: `client.ts` has no call, and no component exists. This plan adds the client methods and the
panels of the plan's side: the intent (goal, criteria with verdicts, non-goals, choices by decider, open questions, deferred
items), the builder check and the outline (deliverables, files, dependencies, interfaces, key links). PLAN-55 and PLAN-62 only
place the panels in the plan's Intent tab. The panels are shown on `/dev/components` with live mock data until they are placed.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/intent-and-outline.adoc` (all; its web part is
#web :273-279, #contract :286-289, #decisions :304-307), `doc/design/shared/intent-and-outline-texts.adoc` (#texts, 75 keys: 68 of the
area `intent`; #states :300-336, the verdict words), `doc/design/web-app/components-tags.adoc` (#status-label :68-104, the verdict
tones :89-100), `doc/design/shared/texts-and-languages.adoc#keys` (:121, the area `intent` registered by D6),
`doc/design/web-app/components.adoc` (#new-component). D6 answer Q4 (a)–(e) of `landings/PLAN-141-findings.md` § Answers settles the
verdict tag, the texts, the links and the shown fields.

1. **The client methods, the types and the catalogue area**
 - What:
   - `client.ts` gains `getPlanIntent(planId)`, `getPlanOutline(planId)`, `getEpicIntent(epicId)` and
     `getOverSize(projectId)` for `GET /api/v1/plans/{id}/intent`, `/api/v1/plans/{id}/outline`, `/api/v1/epics/{id}/intent` and
     `/api/v1/projects/{id}/metrics/oversize`, with types that mirror the `pm-api` records as `PLAN-159` grew them (a decision's
     `adr {adr_id, relation}` and `evidence`, the non-goals as `{id, text}`, the decider `role`, the builder check's `reason`, and
     the distribution's optional `proposed_multiple`) and fail closed on a malformed record as the client's other calls do.
   - The catalogue area `intent`: D6 registered it in the design (`texts-and-languages.adoc:121`) only; at `main` `4a84f727` the
     packs `intent.{en,de}.properties` do not exist and `MessageCatalogue.SUPPORTED_AREAS` (`MessageCatalogue.java:38-62`) does not
     list `intent`. This plan creates the two packs with the 68 `intent.*` keys of `intent-and-outline-texts.adoc#texts` in English and German,
     adds `intent` to `SUPPORTED_AREAS`, and adds the seven verdict words of `#states` (`state.criterion.{met,not_met,not_verified,not_reviewed}`,
     `state.check.{pending,met,not_met}`) to `common.{en,de}.properties` (the `state.*` family lives in `common`). The web side's
     registration of the area, if any is needed (HYPOTHESIS below).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `client.test.ts` pins the four requests and the shapes against the mock's records (every field of `PlanIntentResource`,
     `OutlineResource`, `EpicIntentResource`, `OverSizeDistributionResource`), an absent record and a malformed one. Evidence
     order: `test-first`.
   - A new `IntentCatalogueTest` (as `EpicViewCatalogueTest`) pins the 68 `intent.*` keys and the seven `state.*` verdict words in English and
     German, agreeing key for key and argument for argument; `MessageCatalogueTest` serves the area `intent`. Evidence order:
     `test-first`.
2. **The plan's intent panel**
 - What:
   - `PlanIntentPanel.svelte` (prop `planId`) shows the order of `intent-and-outline.adoc#intent`: the head (plan, revision, the
     goal, the counts by verdict from the verdict words, absent and `not reviewed yet` before the review), the criteria (id, the
     EARS sentence verbatim, the deliverables that satisfy it, the verdict, the reason one expansion away; `no deliverable yet` where
     none), the non-goals (each by the record's own id `NG-n` and its sentence, **never numbered by position**), the choices grouped
     by decider (operator and quorum first; the role's own calls — decider `role`, word `intent.decider.role`, never `model` —
     apart and never styled like the operator's; the options weighed in the record's order, never sorted; a choice's ADR relation
     `intent.choice.adr` (`follows ADR-…` / `contradicts ADR-…`), a `contradicts` choice being always the operator's; a fact's
     `evidence` (`intent.record.evidence`) as text, a path or a `pm://` URI, never turned into a link), the open questions and the
     deferred items with their reasons. **No vote support** is shown: the record carries none.
   - **The verdict** is a `StatusLabel` (`components-tags.adoc#status-label`), not a new component and not a `Chip`: a criterion's
     `state.criterion.met` (success), `.not_met` (danger), `.not_verified` and `.not_reviewed` (neutral — never a warning or an
     attention tone); an absent status is `not_reviewed`.
   - Every text of a record is plain text after `stripControlSequences`, never HTML; a field the record does not carry is left
     out; the client never derives a verdict from a deliverable's state.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `PlanIntentPanel.test.ts`: each verdict as its `StatusLabel` state and tone (`not_verified` neutral) and `not reviewed yet`, a
     `not_met` and `not_verified` with their reason, a criterion without a deliverable, the non-goals by their `NG-n` ids (ids out of
     sequence shown as given), the deciders in order, a `role` call apart, an ADR `follows` and a `contradicts` relation, a fact's
     evidence as text and not a link, no vote support, a text with control sequences and markup rendered as text, and a record
     without criteria. It fails on a revert. Evidence order: `test-first`.
3. **The builder check and the outline**
 - What:
   - `BuilderCheckList.svelte`: the sorts (`answered by the record`, `the builder's call`, `needs you`), the builder's
     recommendation marked as the builder's and preselecting nothing, the resolution state (`open`, `answered`, `deferred`), and the
     check's state (`pending`, `answered`, `not_run`; `not_run` shown with the record's reason as text, `intent.builder.state.not_run`
     "not run: {reason}", as evidence and never as a pass or a hold).
   - `OutlinePanel.svelte` (prop `planId`): the deliverables (number and title, gloss, affected files with their intent
     `read`, `write-new`, `write-replace`, `write-conditional`, `delete` one per line, depends on, satisfies, verification), `satisfies no
     criterion`, `missing deliverable <n>`, the added dependencies with coordinates, version, the adding deliverable and the
     justification, the interfaces, the key links and a missing key link as `missing: <link>`. A path is never turned into a link.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `BuilderCheckList.test.ts` and `OutlinePanel.test.ts` cover each sort, state and edge above (empty, single, duplicate
     dependency, missing deliverable, missing key link, `not_run` with its reason). Evidence order: `test-first`.
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
- OBSERVED (at `1cc0e10e`, still so at `main` `4a84f727`; `PLAN-159` changes it before this plan): a decision carries id, text, decider, options_weighed and question_id only; the deciders are `operator|quorum|model|fact`; criterion statuses are `met|not_met|not_verified`; non-goals are strings; the builder check's resolution is `pending|answered|not_run` without a reason; `proposed_multiple` is required — read at `PlanIntentResource.java:84-90`, `IntentDecider.java:23-35`, `CriterionStatus.java:26-32`, `BuilderCheckResource.java:37-56`, `OverSizeDistributionResource.java:41-69`
- OBSERVED: D6 decided the design shows the ADR relation, the fact's evidence, `NG-n` by id and the `not_run` reason, uses the engine's decider `role`, drops vote support, and makes the verdict a `StatusLabel` state — read at `doc/design/shared/intent-and-outline.adoc:104,121-130,165,278,289,307`, `doc/design/web-app/components-tags.adoc:89-100` (`4a84f727`, D6 merged `8568b56e`)
- OBSERVED: the area `intent` is registered in the design but not in code: `MessageCatalogue.SUPPORTED_AREAS` has no `intent` and no `intent.*.properties` exists; no `state.criterion.*`/`state.check.*` key exists — read at `doc/design/shared/texts-and-languages.adoc:121`, `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java:38-62`, `git ls-files pm-api/src/main/resources/messages`, `grep state.criterion pm-api/src/main/resources/messages/common.en.properties` (empty) (`4a84f727`)
- OBSERVED: the mock serves the four GET routes from fixtures in `running-work.json` and `fully-onboarded.json` for `fix-relay`, `relay-hardening` and `pm-mcp-clients`, answering any other id from the same fixture — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5386-5440`, `pm-mock-runtime/src/main/resources/scenarios/running-work.json:1089,1213,1298,1358`, `fully-onboarded.json:848,972,1057,1117`
- OBSERVED: `EventName` names no intent or outline event — read by `git grep -n -i intent -- pm-api/src/main/java/de/planmarshall/api/event/EventName.java` (empty)
- OBSERVED: no intent catalogue area exists — read by `git ls-files pm-api/src/main/resources/messages` (no `intent.*`)
- OBSERVED: `stripControlSequences` is the safe-text helper — read at `pm-web-app/src/lib/utils/outputSafety.ts:6`
- OBSERVED: no view uses `Tabs` — read by `git grep -n Tabs -- pm-web-app/src/lib/views` (non-test: empty)
- OBSERVED: `intent-and-outline-texts.adoc` was absent at `1cc0e10e`; D6 created it with 75 keys: 68 of the area `intent` and the seven `state.*` verdict words — read at `doc/design/shared/intent-and-outline-texts.adoc` (`4a84f727`)
- HYPOTHESIS: the daemon answers a plan without an intent record with a not-found error the client can map to "no record yet" — confirm/refute at `../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc` § `intent-record` and `pm-api/.../intent/IntentContractTest.java` (verify-at-outline)
- HYPOTHESIS: the plan's events (`plan.changed`, `phase.changed`) signal a new intent or outline revision — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` (verify-at-outline)
- HYPOTHESIS: a new catalogue area needs a registration beyond the properties files — confirm/refute at `pm-web-app/src/lib/i18n/catalogue.test.ts` and `pm-web-app/src/lib/i18n/index.ts` (verify-at-outline)
- Verify-first clause: re-read `intent-and-outline.adoc`, its texts document and `PLAN-159`'s landing at the then-current HEAD; the verdict is a `StatusLabel` state (no new component); if `PLAN-159` has not landed (no `adr`, `evidence`, `{id, text}` non-goals, `role` or builder `reason` in `pm-api`), stop and loop back.

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
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 4a84f727 (the seven `state.criterion.*`/`state.check.*` words)
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java` — present at HEAD 4a84f727 (`intent` in `SUPPORTED_AREAS`)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/MessageCatalogueTest.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/IntentCatalogueTest.java` — absent at HEAD 4a84f727; created by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java` — present at HEAD 1cc0e10e (read only: `PLAN-159` grows the contract)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 1cc0e10e (read only: `PLAN-159` grows the fixtures)
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-159` (the contract delta and the mock fixtures: `Decision.adr`, `Decision.evidence`, non-goals `{id, text}`, decider `role`, `BuilderCheckResource.reason`, the optional proposal); the D6 design session merged — satisfied (#403, `8568b56e`: `intent-and-outline.adoc` AGREED, its texts document, the verdict as a `StatusLabel` state); the D1 foundation parts that deliver the tag family states (`StatusLabel`) and `Link`; `PLAN-143` for the `/decisions/{id}` address (soft: the link is a plain address until then).
- Overlaps with:
  - `pm-web-app/src/lib/api/client.ts`: PLAN-49, 51, 52, 53 and 55 each add methods; the blocks are disjoint.
  - `routes/dev/components/+page.svelte` (PLAN-45 running, PLAN-130 staged).
  - `pm-api/src/main/resources/messages/`: two new files; `common.{en,de}.properties` gain seven `state.*` keys (other plans add `web.*` keys there: disjoint lines, rebase).
  - `MessageCatalogue.java`: one list entry.
- Adjacent to: `PLAN-55` and `PLAN-62` (they place the panels in the plan's Intent tab and link the phase narrative to it; they do not draw them); `PLAN-146` (the epic's side and the chart); the terminal client (a separate package, not here).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`, amended against `4a84f727` after D6). Re-run `corpus surfaces` against everything that landed since (`PLAN-159` first). Settle each HYPOTHESIS. `intent-and-outline.adoc` is `AGREED` and its texts document exists (D6, `8568b56e`); re-check that the key count is still 75 (68 `intent.*`, 7 `state.*`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-145-web-intent-client-and-plan-panels.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

## Staging Note 2026-11-10

The packages this spec names by placeholder were staged on 2026-11-10: the tag family is PLAN-147/148; Card, KeyValueList and Link are PLAN-147; the D6 session is PLAN-141.

## D6 Merged 2026-11-11 (Amendment)

D6 (`PLAN-141`, #403, `8568b56e`) answered Q4: (a) the verdict is a `StatusLabel` state; (b) the texts are
`intent-and-outline-texts.adoc`, area `intent`, English and German; (c) the records are linked from the plan's and the epic's Intent
tab, the decomposition review and a README row; (d) the `over_size` chart stands on the project page's setup tab (`PLAN-146`); (e) the
design shows the ADR relation, the evidence, `NG-n` by id and the `not_run` reason, uses the engine's decider `role`, drops vote
support, and the contract grows in a contract+mock step before this plan — staged as `PLAN-159`. The deliverables above are amended in
place to these answers. Removed: numbering the non-goals by position, the decider word `model`, any vote-support display, the
"verdict tag D6 chose" open point, and the condition "changed only if D6 decides the contract grows" (the contract change is
`PLAN-159`'s, not this plan's). Kept: the creation of the `intent` packs and their code registration — D6 registered the area only in
the design document.

Surface change: added `common.{en,de}.properties`, `MessageCatalogue.java`, `MessageCatalogueTest.java`, `IntentCatalogueTest.java`
(new); `PlanIntentResource.java` and `running-work.json` are read only.

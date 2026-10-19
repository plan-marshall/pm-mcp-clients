# Completion Report: `intent-views` (Intent, Outline, and Their Contracts)

## Executive Summary

Implemented the design and client-side contract phase of the `intent-views` work package (Milestones 8, 10, 11, and 12). The change adds:

* a shared design specification for the plan intent record, outline record, builder check, epic intent, and `over_size` token distribution in both clients;
* the immutable `de.planmarshall.api.intent` contract records in `pm-api`, with databind-free `jackson-core` streaming codecs (`writeTo`/`readFrom`) and fail-closed parsing;
* four mock-runtime scenarios and dynamic route handlers for plan intent, plan outline, epic intent, and a project's `over_size` distribution;
* contract and scenario test suites covering every record, every closed catalogue, negative parsing, and HTTP scenario behaviour.

No sibling `pm-api` package is modified. `pm-operator` and `pm-web-app` are untouched; their Wave 11 views bind against these contracts.

## Delivered Artifacts

### Design documentation

| File | Change |
|---|---|
| `doc/design/shared/intent-and-outline.adoc` | New shared design: intent structure (goal, EARS criteria, deliverable mapping, non-goals, decisions by decider, open questions, deferred items, self-review verdicts), builder check (sorts, recommendations, resolution), outline record (deliverables, affected files, criteria, justified dependencies, interfaces, key links, missing key link), epic intent (destination, done-when with evidence, non-goals, unspecified aspects, per-plan criteria), `over_size` distribution, and both clients' rendering. |
| `doc/design/README.adoc` | Added the `Intent and Outline` index row (`DRAFT`). |

### `pm-api` — new package `de.planmarshall.api.intent`

| File | Purpose |
|---|---|
| `package-info.java` | Package documentation and use-case trace. |
| `CriterionStatus.java` | Closed verdict catalogue: `met`, `not_met`, `not_verified`; a non-met verdict requires its reason. |
| `IntentDecider.java` | Closed decider catalogue: `operator`, `quorum`, `model`, `fact`; operator-facing separation. |
| `IntentCriterion.java` | EARS criterion with deliverable numbers, self-review verdict, and reason. |
| `PlanIntentResource.java` | Plan intent record: goal, criteria, non-goals, decisions (with nested `Decision`), open questions, deferred items (nested `DeferredItem`), builder check. |
| `BuilderQuestion.java` | Builder question with sort (`answered`, `minor`, `operator`), recommendation mark, resolution state, and answer. |
| `BuilderCheckResource.java` | Builder check: resolution (`pending`, `answered`, `not_run`) and questions. |
| `DeliverableRecord.java` | Outline deliverable with nested `AffectedFile` and `FileIntent` (`read`, `write-new`, `write-replace`, `write-conditional`, `delete`). |
| `DependencyJustification.java` | Added dependency with coordinates, optional version, justification, and the deliverable that adds it. |
| `OutlineResource.java` | Outline record: summary, deliverables, added dependencies, interfaces, key links, and a missing key link. |
| `DoneWhenCheck.java` | Epic done-when check with `pending`/`met`/`not_met` verdict and evidence. |
| `EpicIntentResource.java` | Epic intent: destination, done-when, non-goals, unspecified aspects, and each plan's done-when (nested `PlanDoneWhen`). |
| `TokenBucket.java` | One `over_size` bucket in multiples of predicted cost, with task and flagged counts. |
| `OverSizeDistributionResource.java` | Distribution of task tokens against predicted size with derived totals and flagged share. |
| `IntentJson.java` | Package-private streaming codec core: `JsonGenerator`/`JsonParser` helpers, closed-schema checks, strict scalar/array/number reads. |

### `pm-api` tests

| File | Purpose |
|---|---|
| `IntentContractTest.java` | 20 tests: catalogue resolution and refusal, derived properties, round-trip of every record through map/JSON/streaming codecs, and fail-closed parsing (unknown fields, unknown enums, missing reasons, duplicate ids, orphan dependencies, missing evidence, invalid buckets, non-object JSON). |

### `pm-mock-runtime`

| File | Change |
|---|---|
| `StandardScenarios.java` | Added `INTENT_PLAN`, `INTENT_OUTLINE`, `INTENT_EPIC`, `INTENT_OVERSIZE` and registered them in `ALL`. |
| `runtime/MockRuntimeEngine.java` | Added `handleIntentRoutes` with `GET /api/v1/plans/{plan_id}/intent`, `GET /api/v1/plans/{plan_id}/outline`, `GET /api/v1/epics/{epic_id}/intent`, and `GET /api/v1/projects/{project_id}/metrics/oversize`. Each fixture is validated through its record codec before it is served, and a request for another id is answered from the same fixture with the id replaced. |
| `resources/scenarios/intent-plan.json` | Plan intent with four criteria (`met`, `not met`, `not verified`), operator/quorum/model/fact decisions, non-goals, open question, deferred item, and a pending builder check with a recommendation. |
| `resources/scenarios/intent-outline.json` | Outline with three deliverables, affected files, dependency, justified added dependency, interfaces, key links, and a missing key link. |
| `resources/scenarios/intent-epic.json` | Epic intent with four done-when checks (met/not met/pending), evidence, non-goals, unspecified aspects, and two plans' criteria. |
| `resources/scenarios/intent-oversize.json` | Four token buckets and a proposed multiple that flags 7 of 20 tasks (35 %). |
| `IntentScenarioTest.java` | 9 tests: scenario registration, plan intent (including a dynamic plan id and scenario-scoped 404s), outline, epic intent, `over_size` (including a dynamic project id), and direct fixture shape validation. |

### Plan and roadmap maintenance

| File | Change |
|---|---|
| `doc/agy/implemented/intent-views-plan.md` | Moved from `doc/agy/intent-views-plan.md`; checklist steps 2–7 checked, step 8 left to the orchestrator. |
| `doc/agy/implemented/README.md` | Added the implemented-plan archive row linking this report. |
| `doc/plans/README.adoc` | `intent-views` row set to `done` and linked to `implemented/intent-views.adoc`. |
| `doc/plans/implemented/intent-views.adoc` | Moved from `doc/plans/intent-views.adoc`. |

## Test & Verification Results

| Suite | Result |
|---|---|
| `IntentContractTest` (`pm-api`) | 20 tests, 0 failures, 0 errors |
| `IntentScenarioTest` (`pm-mock-runtime`) | 9 tests, 0 failures, 0 errors |
| `StandardScenariosTest` (`pm-mock-runtime`) | Passed with the four new registered scenarios |
| `./mvnw test -pl pm-api,pm-mock-runtime -am` | All tests passed |
| `./mvnw verify -Ppre-commit` | `BUILD SUCCESS` (all eight reactor modules; 6 min 47 s) |

The gate's OpenRewrite run also rewrote import spacing in pre-existing `pm-api` siblings because the parent recipe version groups imports differently from the committed tree. Those unrelated rewrites were reverted to honour the plan's boundary rule (`pm-api` owns only the new `intent` package); the new `intent` files themselves were formatted by the same recipe and kept. `pm-mock-runtime` files were unaffected apart from the intended changes.

## Domain Invariants

| Invariant | Status |
|---|---|
| Java 25, immutable records, no databind | `pm-api` uses records and `jackson-core` streaming only; the `jackson-core-only` enforcer rule passes. |
| Every package documented | `de.planmarshall.api.intent` has `package-info.java`; every public type and method has Javadoc. |
| Fail-closed wire boundary | Unknown fields, unknown closed values, missing/blank required values, out-of-range numbers, duplicate ids, orphan dependencies, unexplained non-met verdicts, and decided checks without evidence are refused with `JsonFormatException`/`IllegalArgumentException`. |
| Decision separation | Decisions carry a closed `decider`; operator/quorum decisions require their `question_id`, model/fact decisions refuse one. |
| Client contract only | No rendered Markdown is read; the design states explicitly that the clients show the records, never the pull-request rendering. |
| Module boundaries | Only the new `de.planmarshall.api.intent` package and the two owned `pm-mock-runtime` files plus new scenarios/tests are changed. |
| No model-facing content | No `workflows/`, `roles/`, `bundles/`, or `skills/` added; the enforcer rule passes. |

## Build & Quality Evidence

```
./mvnw verify -Ppre-commit
...
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] BUILD SUCCESS
```

The PR is opened without the `skip-bot-review` label; every automated review finding is triaged through `plan-marshall-persona-code-reviewer`, and the change lands through the merge queue on `main`.

## Notes for Post-Landing Verification

* The design is `DRAFT` until the operator agrees it; the work package's _Done When_ asks for that agreement.
* Wave 11 (`cli-intent`, `web-intent`) binds the terminal and web views to these records; this change deliberately does not touch `pm-operator` or `pm-web-app`.
* Step 8 (move this report to `doc/agy/verified/`) remains the orchestrator's act.

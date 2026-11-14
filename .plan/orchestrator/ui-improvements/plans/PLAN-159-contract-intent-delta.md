# PLAN-159: Contract: The Intent Records Carry What the Engine Records (D6 Contract Delta)

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 (D6, answer Q4 (e): "the contract delta is a contract+mock step before PLAN-145") from `landings/PLAN-141.md` and
> `landings/PLAN-141-findings.md` (D6 merged as #403, `8568b56e`). A small contract and mock plan, so the runbook lane (operator
> 2026-11-10: small and residue plans on the runbook lane). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer
> and carries no brief.

## Objective

D6 found that the engine's intent record carries more than the client contract lets a client see: a choice's ADR relation and the
evidence of a fact, the non-goals as items with their own `NG-n` id, the decider word `role` (the contract says `model`), and the
reason of a builder check that did not run. D6 also decided that the `over_size` distribution can come without a proposal, which the
contract refuses today. The design (`intent-and-outline.adoc#contract`) shows all of it and states that "growing the contract and
the mock to that is a step of its own before the web app's intent panels". This plan is that step: the `pm-api` records, their
parsing and writing, their tests, and the mock's scenarios. Pre-1.0: the old shapes go, no compatibility shim (no `model` alias, no
string non-goals). Vote support is in neither record and is not added.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/intent-and-outline.adoc` (#non-goals :101-104, #choices
:106-133, #builder :141-165, #oversize :241-244, #contract :286-289), the engine's record
`../pm-mcp-core/pm-core/src/main/resources/de/planmarshall/core/store/intent-record-v1.schema.json` (`non_goals`, `choices`) and its
rules `../pm-mcp-core/pm-core/src/main/java/de/planmarshall/core/store/IntentRecord.java:533-552` (read only), and the builder check
`../pm-mcp-core/doc/specification/phase-workflows/01-planning-phases-1-2.adoc#builder-check` (:492-503: "recorded `not_run` with its
reason").

1. **A decision's ADR relation and evidence; the decider `role`**
 - What:
   - `PlanIntentResource.Decision` gains `@Nullable AdrReference adr` (wire `adr`, an object `{adr_id, relation}`, absent or `null`
     where the choice names none) and `@Nullable String evidence` (wire `evidence`, a path or a `pm://` URI). `AdrReference(String
     adrId, AdrRelation relation)` and the closed enum `AdrRelation` (`follows`, `contradicts`, with `wireValue`, `fromWire`,
     `fromWireOrNull`, `wireValues` like the other closed lists of the package) are nested in `PlanIntentResource` beside `Decision`
     and `DeferredItem`.
   - The rules mirror the engine's (`IntentRecord.java:539-551`): a `fact` decision names its evidence; any other decider carries
     none; a `contradicts` relation is only the operator's (decider `operator`); `adr_id` is not blank. A violation fails closed with
     `JsonFormatException` on read and `IllegalArgumentException` on construction, as the record's other rules do.
   - `IntentDecider.MODEL("model")` becomes `ROLE("role")` (the engine's decider kind: "role is the role's own call",
     `intent-record-v1.schema.json` `choices[].decider`); `"model"` is refused as unknown. `PlanIntentResource.modelCalls()` becomes
     `roleCalls()`; the Javadoc says the role's own call, never the model's.
   - `toMap`, `writeTo`, `fromMap` and `readFrom` carry the two fields; an absent field is not written.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `IntentContractTest` (gaining `@EnableGeneratorController`; input from `Generators`, the wire names stay literal as the
     contract's expectation): a decision with `adr` `follows` and one with `contradicts` round-trip through all four codecs; a
     `contradicts` decision of decider `role`, `quorum` or `fact` is refused; a `fact` decision without evidence and a `role` decision
     with evidence are refused; an unknown `relation` and the decider `model` are refused; `roleCalls()` holds exactly the `role`
     decisions. It fails on a revert. Evidence order: `test-first`.
2. **The non-goals as items**
 - What:
   - `PlanIntentResource.nonGoals` becomes `List<NonGoal>`, `NonGoal(String id, String text)` nested in `PlanIntentResource`, wire
     `non_goals: [{id, text}]`; `id` matches `NG-<n>` (`NG-[0-9]{1,4}`, the engine's `IntentRecord.NON_GOAL_ID`), ids are unique in the
     record, `text` is not blank. The record keeps its order; nothing numbers a non-goal by position.
   - The epic's non-goals (`EpicIntentResource.nonGoals`) stay strings: the engine's `epic-intent-v1.schema.json` `non_goals` are
     strings.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `IntentContractTest`: non-goals round-trip as `{id, text}` in the record's order, including ids out of sequence; a string entry,
     a malformed id, a duplicate id and a blank text are refused. Evidence order: `test-first`.
3. **The builder check's reason; the distribution without a proposal**
 - What:
   - `BuilderCheckResource` gains `@Nullable String reason` (wire `reason`): present and not blank exactly when the resolution is
     `not_run`, refused otherwise. The `NOT_RUN` Javadoc ("with its reason in the questions' absence and the plan's history") states
     the field. The reason is a text the clients show as it stands (`intent.builder.state.not_run`, "not run: {reason}"), so the
     contract does not close its values.
   - `OverSizeDistributionResource.proposedMultiple` becomes `@Nullable Double`: absent where the runtime serves the distribution
     under the multiple in force (`intent-and-outline.adoc#oversize` :244 "Without a proposal it shows the distribution under the
     multiple in force and no flagged comparison"). Without a proposal no bucket carries a flagged task (`flagged_tasks` `0`; a
     record with a flagged task and no proposal is refused), `flaggedTasks()` and `flaggedShare()` are `0`, `flaggedBuckets()` is
     empty, and `flagsMore()` is `false`; a present proposal stays positive and finite. `proposed_multiple` is not written when
     absent. `IntentJson` gains the optional readers it needs (an optional double, an optional object).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `IntentContractTest`: a `not_run` check with its reason round-trips, a `not_run` without a reason and a `pending` or `answered`
     with one are refused; a distribution without `proposed_multiple` round-trips with no flagged task, a share of `0` and
     `flagsMore()` false, one with a flagged task but no proposal is refused, and a non-positive proposal is still refused (the case
     `IntentContractTest:327-329` follows). Evidence order: `test-first`.
4. **The mock serves the new shapes, and the design says the contract carries them**
 - What:
   - `running-work.json` and `fully-onboarded.json` (`pm-mock-runtime/src/main/resources/scenarios/`): the plan intent fixtures of
     `fix-relay` write the non-goals as `{id: "NG-1", text}` items; the decision of decider `model` becomes `role`; the `fact` decision
     carries an `evidence` path; one operator decision carries `adr` `follows` and one carries `contradicts`. `running-work.json`
     keeps the builder check `pending` and the distribution with a proposal; `fully-onboarded.json` serves a builder check `not_run`
     with a reason and the distribution of `pm-mcp-clients` without a proposal (no `proposed_multiple`, every `flagged_tasks` `0`).
     Fixture texts are written for the test, never copied from the product's content.
   - `IntentScenarioTest` and `FullyOnboardedScenarioTest` follow (`modelCalls()` → `roleCalls()`, the non-goal items, the evidence,
     the ADR relations, the `not_run` reason, the distribution without a proposal).
   - `pm-operator` and `pm-relay` read none of these records (`git grep -n "api.intent" -- pm-operator pm-relay` is empty at
     `4a84f727`): they only compile against the changed `pm-api`. If a reader appears by the then-current HEAD, it is changed here.
   - `doc/design/shared/intent-and-outline.adoc#contract` (:289) is updated in the same pull request: the contract carries the
     fields now (drop "where the contract today says `model`" and "today it requires a positive `proposed_multiple`"). No other design
     line changes.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `IntentScenarioTest` serves the grown fixtures of both scenarios through `MockRuntime` and decodes them with the `pm-api`
     records; a fixture reverted to `"decider": "model"` or a string non-goal fails the scenario load (evidence order:
     `mutation-after`).
   - `./mvnw -pl pm-api,pm-mock-runtime,pm-operator,pm-relay verify` passes at the head, and `./mvnw -pl pm-api,pm-mock-runtime verify
     -Pcoverage` keeps 80 % instruction and branch coverage (evidence order: `none`).
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a
     failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`).

Not split: four deliverables, one contract delta of one design decision (D6 Q4 (e)); the orchestrator records the rationale (scope
guard).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime verify -Ppre-commit` (review every rewrite and
  commit it), then `./mvnw -pl pm-api,pm-mock-runtime,pm-operator,pm-relay verify` and `./mvnw -pl pm-api,pm-mock-runtime verify -Pcoverage`.
  The compiler runs with `failOnWarning`.
- No new dependency or plugin. `pm-api` stays on the streaming API of `jackson-core` only (no `jackson-databind`).
- Code standards (`CLAUDE.md`): Java 25, records, `@Nullable` under the package's `@NullMarked`, every public type documented, no
  generic `Exception`/`RuntimeException`, JUnit 5 only with `@DisplayName`/`@Nested`, no Mockito, PowerMock or Hamcrest; input test
  data from `Generators` and `@EnableGeneratorController` on every test class that takes input; a wire name or catalogue text stays
  literal as the expectation.
- Pre-1.0: no deprecation cycle and no compatibility shim — the decider `model` and the string non-goals are refused, not aliased.
- No web code: the web types and panels are `PLAN-145`'s and `PLAN-146`'s. No browser check (nothing rendered changes).
- Ports: none used (no mock web port, no app); the mock is exercised in-process by its tests.
- Every `test-first` clause cites a FAILING run and a PASSING run of the named test; every `mutation-after` clause cites the reverted
  code and a failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY (nitpicks and outside-diff sections
    included) answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask
    the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The
    pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues at merge, every open issue fixed in a commit of THIS pull
  request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: a decision carries `id`, `text`, `decider`, `options_weighed`, `question_id` only — read at `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java:84-99` (`4a84f727`)
- OBSERVED: the non-goals are `List<String>`, refused empty or blank — read at `PlanIntentResource.java:52,234-240,334,363` (`4a84f727`)
- OBSERVED: the decider list is `OPERATOR`, `QUORUM`, `MODEL("model")`, `FACT`, and `modelCalls()` filters `MODEL` — read at `pm-api/src/main/java/de/planmarshall/api/intent/IntentDecider.java:23-35`, `PlanIntentResource.java:314-316` (`4a84f727`)
- OBSERVED: `BuilderCheckResource` is `(resolution, questions)` with `NOT_RUN` and no reason — read at `pm-api/src/main/java/de/planmarshall/api/intent/BuilderCheckResource.java:37-56` (`4a84f727`)
- OBSERVED: `OverSizeDistributionResource` requires a positive `proposed_multiple` and always writes it — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java:41-69,131,154` (`4a84f727`)
- OBSERVED: the engine's record carries `choices[].adr {adr_id, relation: follows|contradicts}`, `choices[].evidence` (a path or `pm://` URI, for decider `fact`), the decider enum `operator|quorum|role|fact`, and `non_goals[]` as `{id: NG-n, text}` — read at `../pm-mcp-core/pm-core/src/main/resources/de/planmarshall/core/store/intent-record-v1.schema.json` (`non_goals`, `choices`) (`pm-mcp-core` `a2edd0f2`)
- OBSERVED: the engine refuses a `fact` decision without evidence, evidence on any other decider, and a `contradicts` decision whose decider is not `operator` — read at `../pm-mcp-core/pm-core/src/main/java/de/planmarshall/core/store/IntentRecord.java:539-551` (`a2edd0f2`)
- OBSERVED: the epic's non-goals are strings in the engine — read at `../pm-mcp-core/pm-core/src/main/resources/de/planmarshall/core/store/epic-intent-v1.schema.json:11` (`a2edd0f2`)
- OBSERVED: the design shows the ADR relation, the evidence, `NG-n` by id, the decider `role` and the `not_run` reason, and states the contract delta and the absent proposal as a step before the web panels — read at `doc/design/shared/intent-and-outline.adoc:104,121-130,165,244,289` (`4a84f727`, D6 merged `8568b56e`)
- OBSERVED: the mock's intent fixtures use `"decider": "model"`, string non-goals, a `fact` decision without evidence, a `pending` builder check and `proposed_multiple: 2.0` in both scenarios — read at `pm-mock-runtime/src/main/resources/scenarios/running-work.json:1133-1185,1363`, `fully-onboarded.json:892-944,1122` (`4a84f727`)
- OBSERVED: the mock serves the intent routes from the fixtures through the `pm-api` records' `fromMap` — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:5407-5440` (`4a84f727`)
- OBSERVED: no module outside `pm-api` and `pm-mock-runtime` reads the intent records (`pm-operator`, `pm-relay`, `pm-web-app` have no reader) — read by `git grep -n "api.intent\|IntentDecider\|PlanIntentResource\|OverSizeDistributionResource" -- pm-operator pm-relay pm-web-app/src` (empty at `4a84f727`)
- OBSERVED: `IntentContractTest` carries no `@EnableGeneratorController`; `IntentScenarioTest` does — read at `pm-api/src/test/java/de/planmarshall/api/intent/IntentContractTest.java:39-40`, `pm-mock-runtime/src/test/java/de/planmarshall/mock/IntentScenarioTest.java:48` (`4a84f727`)
- HYPOTHESIS: the daemon (`pm-mcpd`) serves the builder check's reason under the wire name `reason` and omits `proposed_multiple` when no proposal exists; neither the daemon nor a runtime-model specification defines these routes yet — confirm/refute at `../plan-marshall-mcp` (`git grep -n "metrics/oversize\|builder_check"`) and `../plan-marshall-documentation/doc/specification/runtime-model/` (verify-at-outline; a different wire name found there wins and is reported)
- HYPOTHESIS: a distribution without a proposal carries no flagged task (the design's "no flagged comparison") — confirm/refute at `doc/design/shared/intent-and-outline.adoc:244,262` (verify-at-outline; refuted → the bucket rule of deliverable 3 is dropped and reported)
- HYPOTHESIS: `ScenarioLoader` validates every intent fixture through the records at load, so a fixture in the old shape fails the scenario load — confirm/refute at `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java:578-640` (verify-at-outline)
- Not in scope, reported to the orchestrator: the engine allows zero non-goals on the light lane (`intent-record-v1.schema.json` `non_goals.minItems: 0`; `01-planning-phases-1-2.adoc:463`) while `PlanIntentResource` refuses an empty list and the design says "at least one" (`intent-and-outline.adoc:104`); this plan keeps the client's rule. Also the route serves one role per project with no role selector, while the design shows one chart per role (`project-explorer.adoc:108`).
- Verify-first clause: re-read the schema, `IntentRecord.java` and the cited design lines at the then-current HEADs before deliverable 1; a field the engine renamed since is taken as the engine names it.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/PlanIntentResource.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/IntentDecider.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/BuilderCheckResource.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/IntentJson.java` — present at HEAD 4a84f727
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/TokenBucket.java` — present at HEAD 4a84f727 (read; changed only if the flagged rule needs a bucket helper)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentContractTest.java` — present at HEAD 4a84f727
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at HEAD 4a84f727
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at HEAD 4a84f727
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/IntentScenarioTest.java` — present at HEAD 4a84f727
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/FullyOnboardedScenarioTest.java` — present at HEAD 4a84f727
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 4a84f727 (only the paragraph of #contract)

## Dependencies and Sequencing

- Depends on: the D6 design session merged — satisfied (`PLAN-141`, #403, `8568b56e`).
- Overlaps with: the mock-touching plans on `running-work.json` and `fully-onboarded.json` (the chat contract and mock plans
  `PLAN-132`..`PLAN-134` where still open; whichever starts second rebases — the intent blocks are disjoint from theirs).
- Blocks: `PLAN-145` (the web types and the plan's panels read the grown records) and `PLAN-146` (the chart without a proposal).
- Adjacent to: the terminal client's intent sections (`intent-and-outline.adoc#terminal`), which have no plan yet and will read the
  same records.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `4a84f727`, `pm-mcp-core` at `a2edd0f2`). Re-run
`corpus surfaces` against everything that landed since. Settle each HYPOTHESIS.

## Hand-Off Command

Lane: `runbook` (a small contract and mock plan; operator 2026-11-10: small and residue plans on the runbook lane).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-159-contract-intent-delta.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (it reads the sibling checkout `pm-mcp-core` and
never writes it). It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the
orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's
qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary.

## PLAN-134 Landing Residue (Amendment 2026-11-14)

PLAN-134 (#428, `7adb56c2`) moved `MockRuntimeEngine.java` line refs:
- refs past line ~5000 shift by about +430 (5386→5813, 5407→5834, 5459→5886);
- the ref at 491 shifts by +31 (→522).

`ChatRegistry.java` is now 2428 lines, with route parsing at :2054-2145. Re-ground every engine and registry line
ref in this spec at restart.

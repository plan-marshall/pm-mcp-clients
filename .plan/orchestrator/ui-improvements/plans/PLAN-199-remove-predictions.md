# PLAN-199: Remove What the Design No Longer Predicts: the `over_size` Distribution, the Outline's Added Dependencies, the Old Off-Track Signals

epic: ui-improvements
workstream: WS-01

> Staged plan spec, one shippable unit of work, emitted as a `/plan-marshall` plan (medium; the operator, 2026-11-27: "emit another
> plan-marshall plan"). Cut from the inbox message `doc-review-2026-11-24-001` § 1 (archived), the rest that PLAN-191 (#582) and
> PLAN-188 (#566) did not take. Grounded by a read-only sub-agent at `origin/main` `51b979ad` (pm-mcp-core read at `11ec6b72`).
> This spec is SELF-SUFFICIENT. **It adds no dependency.**

## Objective

The merged design (#570 `20f971fe`) and the engine's specification follow "observe facts instead of predicting them". So the clients drop:
- the `over_size` distribution (the measured token sizes stay in metrics; the behaviour signals stay);
- the outline's declared `added_dependencies` (the phase `sanity-check` reads them from the build);
- the off-track signals `over_size` and `outside_footprint`.

The signal set is `repeating`, `no_progress`, `failing_again`, `edit_churn`, `agent_started` (pm-mcp-core `doc/specification/model-work/03-supervision-and-recycling.adoc:71-85`; design `terminal-client/16-plan-view.adoc:108`, `shared/plan-view.adoc:168`).

Already done and not redone:
- PLAN-191 (#582) removed `epic.overlap_found` and renamed the wait reason `stale` to `unreadable`, with negative controls (`ControlCataloguesTest:60-61`, `ReportRecordsTest:54`). That is the precedent for the controls here.
- PLAN-188 (#566) removed the 9 `intent.oversize.*` keys, `intent.outline.dependenc{y,ies}`, `OverSizeChart`, the dev fixture's chart and `OutlinePanel`'s dependency block. Its PR left the wire contract, the resources and the project page's spread as follow-up work.
- The design is already clean: no `over_size`, `added_dependencies` or spread under `doc/design`.

The engine's code still sends the old signal set (pm-mcp-core `QueueTaskRecord.java:408-418` `SignalName`, `queue-task.schema.json:87`). So the client's wire stays lenient: `OffTrackInfo.signal` stays a `String`. That lag is a finding for pm-mcp-core, not a dependency of this plan.

Line numbers are at `origin/main` `51b979ad`.

## Deliverables

1. **The `over_size` distribution is gone**
 - What:
   - **`pm-api`:**
     - Delete `intent/OverSizeDistributionResource.java`, `OverSizeDistributionsResource.java` and `TokenBucket.java`.
     - `intent/package-info.java:11-13` drops "and `over_size` distribution" and PM-WORK-7.
     - Remove `project.page.oversize` from `projects.{en,de}.properties:208`.
   - **`pm-mock-runtime`:**
     - `StandardScenarios.java`: `INTENT_OVERSIZE` (`:422-426`) and its entry in `ALL` (`:491`).
     - `ScenarioRegistry.java:259`: the alias `intent-oversize`.
     - `MockRuntimeEngine.java`: the imports `:125-126`, `SEGMENT_METRICS_OVERSIZE` `:700`, `FIXTURE_OVERSIZE` `:704`, the javadoc `:7271`, the branch `:7307-7311` and `serveDistributionFixture` `:7342-7377`. `hasIdBetween` and `PREFIX_PROJECTS` stay.
     - Scenarios: the route blocks `fully-onboarded.json:1113` and `running-work.json:1464`, and the summary text `running-work.json:7390` ("…and the over_size spread").
   - **`pm-web-app`:**
     - `client.ts`: `OverSize{Bucket,Distribution,Distributions}Record` (`:2509-2539`), `readOverSize*` (`:2987-~3020`), `getOverSize` (`:4345-4356`).
     - `ProjectExplorerView.svelte`: the import `:47`, state `:123-125`, reset `:222`, `loadOverSize` `:263-277` with its call `:288`, `overSizeRows` `:919-931`, the setup-tab section `:1616-1636`, CSS `:2051-2066`.
   - **Tests that pin it are deleted:**
     - `IntentContractTest`: `:138-143`, `:193-205`, the TokenBucket case `:237-239`, `:326-340`, `@Nested DistributionWithoutProposal` and `DistributionsPerRole` `:550-~685`, the helpers.
     - `IntentScenarioTest`: `:40`, `:146-150`, `@Nested OverSize` `:246-310`, the oversize part of `:318-332`. Delete the vacuous `pathWithoutId` `:176`.
     - `FullyOnboardedScenarioTest:58,116-117,203-204`.
     - `client.test.ts`: `:2435`, `:2727-~2830`; the second half of `:2940-2941`, keeping the epic half, renamed.
     - `ProjectExplorerView.test.ts`: the `getOverSize` spies and the two over-size tests.
   - **`doc/Traceability.adoc:578,580` (PM-WORK-7):**
     - Implementation names `api/process` (`OffTrackSignal`, `OffTrackInfo`).
     - Verified-by names `PlanViewFormattingTest` and the signal test of deliverable 2.
     - Optional: `doc/plans/README.adoc:331` ("the spread of `over_size`").
   - Unrelated "oversized" (`DottedIdentifierTest`, `OverviewStateStore`, `ChoicePromptTest`, `lineDiff.ts`) stays.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - A test asserts that `GET /api/v1/projects/pm-mcp-clients/metrics/oversize` answers 404 `mock_no_route` in `running-work` and `fully-onboarded`, and that `intent-oversize` resolves to no scenario or alias (evidence order: `test-first`).
   - A test asserts that the setup tab of the project page renders no `project-oversize` section and sends no `/metrics/oversize` request (evidence order: `test-first`).
   - `git grep -n -E 'OverSize|TokenBucket|metrics/oversize|intent-oversize|project\.page\.oversize|getOverSize' -- ':!.plan' ':!doc/plans'` has no hit; the PR shows it (evidence order: `none`).
2. **The off-track signals are the closed set of five**
 - What:
   - `pm-api/.../process/OffTrackSignal.java:18-21` becomes `REPEATING, NO_PROGRESS, FAILING_AGAIN, EDIT_CHURN, AGENT_STARTED`.
   - An unknown value is refused the PLAN-191 way:
     - `fromValue` returns `@Nullable`, `parse` throws `IllegalArgumentException`.
     - Today `:33-41` maps every unknown value to `REPEATING`.
     - The enum has no production caller (`ProcessTreeTest:644-645` and `PlanViewScenarioTest:87,179` use `fromValue`), so nothing changes at run time.
     - The wire `OffTrackInfo.signal` stays a `String`.
   - `OffTrackInfo.java:24` javadoc lists the five.
   - **Texts:**
     - Remove `state.signal.over_size` and `state.signal.outside_footprint` (`common.{en,de}.properties:369-370`), and `plan.signal.over_size` and `plan.signal.outside_footprint` (`plan_view.{en,de}.properties:110-111`).
     - Add `state.signal.failing_again`, `edit_churn` and `agent_started` in `common`, and `plan.signal.*` with the same three in `plan_view`, en and de.
     - **No design row names any signal word today.** So the words go first into the design's text table of `doc/design/shared/plan-view.adoc` (the `plan.signal.*` family, `:516`), with their en/de text. One word per value (`texts-and-languages.adoc:132`), and `agent_started` glossed as at `plan-view.adoc:168`. Then they go into the packs.
     - Proposal, the outline may improve it: en "failing again", "edit churn", "agent started"; de "scheitert erneut", "wiederholte Änderung", "Agent gestartet". The PR states the chosen words for the operator.
     - If PLAN-198 has landed first, the rows follow its rule: the design links the catalogue files.
   - `PlanViewFormattingTest:145` (`"outside footprint"`) moves to a current signal.
   - Rendering needs no code change: `PlanViewFormatting.signal` (`:212-221`) and the web `displayState('signal', …)` (`display/index.ts:83-95`) look the word up.
   - **The mock serves one current signal beyond `repeating`:** an `agent_started` task with action `restart` in `running-work.json`. Then both clients render a new word, and PLAN-190 D2 has a current-signal fixture.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - A test asserts the enum values are exactly the five, in this order, and that `over_size`, `outside_footprint` and an unknown value are refused (`fromValue` null, `parse` throws) (evidence order: `test-first`).
   - A test asserts that the four retired `state.signal.*`/`plan.signal.*` keys are absent from both packs, and that the three new words exist and are not blank in en and de (evidence order: `mutation-after`).
   - A test asserts that `PlanViewFormatting` renders `edit_churn` with its catalogue word, and that the mock's `agent_started` task renders its word in the terminal plan view (evidence order: `test-first`).
3. **The outline's `added_dependencies` are gone**
 - What:
   - **`pm-api`:**
     - Delete `intent/DependencyJustification.java`.
     - `OutlineResource.java`: drop the javadoc `:27-35`, the component `:42`, `FIELD_ADDED_DEPENDENCIES` `:49`, its entry in `KNOWN_FIELDS` `:53`, `:70`, `requireDependencies` `:98-108`, and the `toMap`/`writeTo`/`fromMap` lines `:143-144,167-168,187-188`.
     - A record carrying the member is then refused as an unknown field. This matches the engine, which refuses it whole (`OutlineCodec.java:60-66`).
   - **Scenarios:** `fully-onboarded.json:1043-1050`, `running-work.json:1394-1401`.
   - **`pm-web-app` `client.ts`:** `AddedDependencyRecord` `:2465-2471`, `OutlineRecord.added_dependencies` `:2479`, the reader `:2947-2954`. The web reader ignores unknown fields.
   - **Tests:**
     - `IntentContractTest`: `:129-131`; in `invalidOutline` (`:277-299`) only the orphan-dependency case goes; the helpers `:1109-~1122`.
     - `IntentScenarioTest:196-198`.
     - `client.test.ts:2657-2664`.
     - The typed `OutlineRecord` literals in `OutlinePanel.test.ts:57`, `PlanView.test.ts:2565` and `routes/dev/components/+page.test.ts:349`.
   - `dependencyLabel` and the deliverable's `depends_on` stay.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - A test asserts that `OutlineResource.fromJson` refuses a record carrying `added_dependencies` (unknown field, naming the member), and that `toMap` carries exactly `plan_id`, `revision`, `summary`, `deliverables` (evidence order: `test-first`).
   - A test asserts that the mock's outline fixture (`/api/v1/plans/fix-relay/outline`, both scenarios) carries no `added_dependencies` and decodes, and that the web `getOutline` result has no `added_dependencies` key even when the wire carries one (evidence order: `test-first`).
   - `git grep -n -E 'added_dependencies|DependencyJustification|AddedDependencyRecord|addedDependencies|over_size|outside_footprint' -- ':!.plan' ':!doc/plans'` has no hit outside the negative controls. The PR shows it (evidence order: `none`).

## Standing Constraints

- **Build and test through Maven only** (`CLAUDE.md`). At the head:
  - `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify`
  - `./mvnw -pl pm-web-app verify`
  - `PM_MOCK_WEB_PORT=21990 PM_WEB_PORT=21991 ./mvnw -pl pm-web-e2e verify -Pe2e -am`
  - the full `./mvnw verify`
  - `./mvnw verify -Ppre-commit`
- **Ports of this plan:** mock web port `21990`, app port `21991` (`AGENTS.md` § Ports). An assigned port that is in use is a blocker, never a reason to take another port.
- **No new dependency or plugin.** Pre-1.0: no shim, no alias accepting a removed value, no deprecation.
- **Java:** Java 25, JUnit 5, input data from `Generators` with `@EnableGeneratorController`. Wire values and catalogue texts are contract expectations and stay literal. `@NullMarked`; no generic exception catch.
- **Web:** ES modules; no timer wait in a test; text only through text-treating sinks.
- **Design texts:** a word added to a catalogue comes from a design text row added in the same PR, en and de. A design document this plan changes is updated in the same pull request.
- **ZERO Sonar findings** on the new code of the pull request (SonarCloud `plan-marshall_pm-mcp-clients`), read at the head after the push. State `sonar_open_new_issues=N` in the landing message (`unknown` for a failed read, never `0`). No `NOSONAR`.
- **The merge rule** (`CLAUDE.md`, Git Workflow): the plan merges its own pull request once the gates hold:
  - every review thread replied to and resolved;
  - one CodeRabbit review on some head;
  - build and Sonar green.
  - Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; no generator footer.
  - If CodeRabbit refuses on quota, wait out the window and trigger it once more. Ask the operator only if it still refuses.
- **Running plans:** whichever merges second merges `main` and keeps both changes. Never touch another plan's branch.
  - PLAN-135 shares `client.ts`, `client.test.ts` and `common.{en,de}.properties`; its regions are disjoint.
  - PLAN-62 shares `running-work.json`, `PlanView.test.ts` and possibly `plan_view.*`.
- **The landing message states every claim against the merged tree:** file names, counts of tests and keys. It names the engine's lag (`SignalName`) as a finding for pm-mcp-core.

## Claim Labels

- OBSERVED: the four distribution and dependency classes, the mock route and alias, the web client functions and the setup-tab section exist on main — read at `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java`, `TokenBucket.java`, `DependencyJustification.java`, `MockRuntimeEngine.java:7307-7311`, `ScenarioRegistry.java:259`, `client.ts:4345-4356`, `ProjectExplorerView.svelte:1616-1636`
- OBSERVED: `OffTrackSignal` is `REPEATING, NO_PROGRESS, OVER_SIZE, OUTSIDE_FOOTPRINT` with an unknown value mapped to `REPEATING`, and no production caller — read at `OffTrackSignal.java:18-41`; `git grep OffTrackSignal`
- OBSERVED: the engine's specification names the five signals; its code still has the old set — read at pm-mcp-core `model-work/03-supervision-and-recycling.adoc:71-85`, `QueueTaskRecord.java:408-418`
- OBSERVED: no design text row names any `plan.signal.*`/`state.signal.*` word — read at `doc/design/shared/plan-view.adoc:516`, `texts-and-languages.adoc:112,132`
- OBSERVED: PLAN-188 (#566) already removed the intent keys, `OverSizeChart` and the `OutlinePanel` dependency block — read at `git show 52aa9e4b --stat`
- HYPOTHESIS: refusing the member in `OutlineResource.fromJson` breaks no fixture other than the two scenario blocks — confirm/refute at the outline by `git grep added_dependencies`

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionResource.java` — present at `51b979ad`; deleted by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OverSizeDistributionsResource.java` — present at `51b979ad`; deleted by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/TokenBucket.java` — present at `51b979ad`; deleted by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/DependencyJustification.java` — present at `51b979ad`; deleted by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/package-info.java` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/intent/OutlineResource.java` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/OffTrackSignal.java` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/process/OffTrackInfo.java` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at `51b979ad`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/intent/IntentContractTest.java` — present at `51b979ad`
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/process/ProcessTreeTest.java` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/FullyOnboardedScenarioTest.java` — present at `51b979ad`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/IntentScenarioTest.java` — present at `51b979ad`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/planview/PlanViewFormattingTest.java` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/components/OutlinePanel.test.ts` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at `51b979ad`
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at `51b979ad`
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at `51b979ad`
- OBSERVED: `doc/Traceability.adoc` — present at `51b979ad`

## Dependencies and Sequencing

- Depends on: none. PLAN-191 (#582) and PLAN-188 (#566) have landed.
- Overlaps, under the overlap rule (2026-11-25):
  - PLAN-135 (running): 4 code files (`client.ts`, `client.test.ts`, `common.{en,de}`), disjoint regions.
  - PLAN-62 (running): 2, up to 4 (`running-work.json`, `PlanView.test.ts`, maybe `plan_view.*`).
  - PLAN-70 and PLAN-138: 0.
  - PLAN-198 (staged): 0. Semantic link: the convention for the new signal words.
- Unblocks:
  - PLAN-179: 9 shared files; its Re-Grounding 2026-11-25 orders it after this package.
  - PLAN-190 D2: "test with a current signal".

## Hand-Off Command

Lane: `plan-marshall` (medium; the operator, 2026-11-27).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-199-remove-predictions.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source, tests and design documents. It creates and edits NO file
under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The plan
reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger
Write-Boundary).

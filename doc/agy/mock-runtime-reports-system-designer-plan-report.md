# Completion Report: `mock-runtime-reports-system-designer`

## Executive Summary

Scenarios 4 and 5 of the five-scenario mock-runtime architecture are now consolidated and strict:
`reports-and-system.json` carries the reports inbox, the completion reports of every outcome, the
runtime lifecycle, and the cleanup states; `designer.json` carries the process-designer fixtures in
standard `routes` and `OperatorStateResource` states. The transitional top-level `designer` and
`start_case` members are gone from `Scenario` and `ScenarioLoader`, and `ScenarioLoader.validateOperatorState`
no longer returns early: every state in every shipped scenario must decode as an `OperatorStateResource`.
`pm-web-app` gained a scenario-driven `fetch` helper that imports the mock runtime's own scenario
files and applies the same routing rules, so Vitest tests can exercise whole views against the same
files the started runtime serves.

The legacy `reports.json` and `system.json` files are deleted; `reports` and `system` remain resolvable
through their legacy aliases into `reports-and-system#reports.unread` and
`reports-and-system#system.running`.

## Delivered Artifacts

| Module | File | Change |
|---|---|---|
| `pm-mock-runtime` | `src/main/resources/scenarios/reports-and-system.json` | Rewritten: 7 unread reports across two projects, an epic, and plans of no epic; completion reports for `merged`/`no_change`/`aborted`/`failed` with waivers, phase timeline, and token breakdown; 18 operator states (`reports.*`, `system.*`, `cleanup.*`); routes for reports, completion reports, system, and cleanup; `report.sent`/`report.read` and `system.metrics`/`runtime.*`/`cleanup.*` events. |
| `pm-mock-runtime` | `src/main/resources/scenarios/reports.json`, `system.json` | Deleted; their names map to the consolidated states. |
| `pm-mock-runtime` | `src/main/resources/scenarios/designer.json` | Rewritten: machine and project unit lists, machine and project catalogues as routes; nine designer states (`designer.ready`, `.empty-catalogue`, `.no-project`, `.unit-invalid`, `.digest-conflict`, `.placed`, `.landed-pr`, `.landed-direct`, `.removed`). |
| `pm-mock-runtime` | `src/main/resources/scenarios/fully-onboarded.json` | Migrated to the same format: the single showroom state is an `OperatorStateResource`; the designer fixture is expressed as routes; the start fixture moved into `facts.runtime.start`. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/scenario/Scenario.java` | Removed the `designer` and `startCase` components and the transitional convenience constructor. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` | Removed `designer`/`start_case` parsing and root fields; `validateOperatorState` is mandatory and fails without `state.state_id`; the start state is checked before the states parse; route validation covers the workflow list wrapper, the workflow catalogue, and the cleanup job. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/scenario/ScenarioRegistry.java` | Removed the removed components from `atState`; aliases now target `reports.unread` and `system.running`. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/runtime/DesignerRegistry.java` | Seeds the machine/project units and catalogues from the scenario `routes`; the enrolled projects and runtime version may be overridden by `facts.runtime.designer` of the base and active states, with unit/block list overrides for the empty and no-project cases. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` | Removed the transitional `start_case` fallback. |
| `pm-mock-runtime` | `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Reports seed from an explicitly declared (including empty) `facts.runtime.reports`; completion reports seed from `facts.runtime.completion_reports` with the Java literals as the fallback; the new fixture is validated before a scenario switch. |
| `pm-mock-runtime` | `src/test/java/de/planmarshall/mock/StandardScenariosTest.java` | Added the exact canonical-file set assertion and the per-state `OperatorStateResource` decode over every primary scenario. |
| `pm-mock-runtime` | `src/test/java/de/planmarshall/mock/{ReportsScenarioTest,SystemScenarioTest,DesignerScenarioTest}.java` | Updated to the consolidated scenario names and states; added aborted/failed completion and empty-inbox coverage. |
| `pm-mock-runtime` | `src/test/java/de/planmarshall/mock/{FullyOnboardedScenarioTest,scenario/ScenarioLoaderTest,scenario/ScenarioRegistryTest,runtime/StartRegistryTest}.java` | Migrated every fixture to the operator-state form and removed the transitional-fixture assertions. |
| `pm-web-app` | `src/lib/api/scenarioMockFetch.ts` | New scenario router: static imports of the six canonical scenario files, link transitions by method and `href`, static routes, and `mock_no_route` refusals naming the state and its links. |
| `pm-web-app` | `src/lib/api/scenarioMockFetch.test.ts` | New Vitest suite: every scenario loads, routes serve, links transition, the designer catalogue is served, unknown requests are refused, and `reset` returns to the start state. |
| `pm-web-app` | `tsconfig.json`, `vite.config.ts` | `resolveJsonModule` and the `..` filesystem allowance for the sibling module's scenario resources. |

## Test & Verification Results

| Suite | Result |
|---|---|
| `pm-mock-runtime` unit suite (`./mvnw -pl pm-mock-runtime test`) | All tests pass, including the new strict-loader and canonical-file assertions. |
| `pm-web-app` Vitest (`npm run test:ci`) | 49 files, 400 tests pass, including the six new scenario-router tests. |
| `pm-web-app` ESLint (`npm run lint`) | Clean. |
| `pm-web-app` type check (`npm run check`) | 0 errors, 0 warnings. |

## Domain Invariants

- **Java 25+**: no language level change; the module compiles under the reactor's toolchain.
- **Closed scenario schema**: `designer` and `start_case` are unknown root members; the loader refuses them.
- **Operator state completeness**: every state of every primary scenario decodes as an `OperatorStateResource`, and its scenario links agree with the operator links it advertises.
- **No uncatalogued events**: all scenario events are catalogue members (`report.sent`, `report.read`, `system.metrics`, `runtime.draining`, `runtime.upgrading`, `runtime.upgraded`, `runtime.stopping`, `cleanup.progress`, `cleanup.done`, `cleanup.decided`).
- **Mock runtime completeness**: the six primary scenarios are the only delivered scenario JSON files; no legacy `reports.json`, `system.json`, or case file remains.
- **Web app**: no DOM changes; the new helper serves test data only.

## Build & Quality Evidence

`./mvnw verify -Ppre-commit` completed with `BUILD SUCCESS` across the reactor
(`pm-api`, `pm-exec`, `pm-mock-runtime`, `pm-relay`, `pm-operator`, `pm-web-app`, `pm-web-e2e`),
including OpenRewrite, license headers, Checkstyle, Enforcer, Javadoc, the full unit suites, the
web-app lint/type check, and the web-app Vitest suite.

## Notes

- The plan names five canonical files; the repository has six primary scenarios because
  `fully-onboarded.json` is the user-facing showroom added by the preceding wave. The
  canonical-file test therefore asserts exactly the six entries of
  `StandardScenarios.PRIMARY_SCENARIOS` (`onboarding`, `running-work`, `projects-and-start`,
  `reports-and-system`, `designer`, `fully-onboarded`), with no stray case file, and validates all
  of their states.
- The plan's shorthand cleanup paths are implemented under the contract's real paths,
  `/api/v1/system/cleanup` and `/api/v1/system/cleanup/kept`.

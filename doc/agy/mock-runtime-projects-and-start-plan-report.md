# Completion Report: `mock-runtime-projects-and-start` (Scenario 3: Consolidated `projects-and-start.json`)

**Plan:** [`doc/agy/implemented/mock-runtime-projects-and-start-plan.md`](implemented/mock-runtime-projects-and-start-plan.md)
**Branch:** `feat/mock-projects-start`
**Module:** `pm-mock-runtime`
**Status:** Implementation complete; PR review window pending (Step 6). Post-landing verification (Step 8) is reserved for the orchestrator.

---

## 1. Executive Summary

Scenario 3 of the five-scenario mock-runtime architecture is consolidated. The twenty fragmented scenario files of the project explorer, enrolment, git/lesson sources, and starting work now live in one canonical `projects-and-start.json` with 26 states. `StartRegistry` no longer reads the non-standard top-level `start_case` member for this scenario: it seeds the live entry from the active state's `OperatorStateResource.facts.runtime.start` merged over the scenario's base state, and reads the `GET /api/v1/recipes` route when a state names no recipes. The transitional `start_case` fallback remains only for the showroom scenario, whose removal belongs to Wave 3 (`mock-runtime-reports-system-designer-plan.md`).

All project and start contract cases are exercised against the consolidated scenario by the six owned test suites plus a new focused `StartRegistryTest`. The pre-commit gate passes for `pm-mock-runtime` and its reactor dependencies.

---

## 2. Delivered Artifacts

### 2.1 Production code

| File | Change |
|---|---|
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/StartRegistry.java` | Seeds from the active state's operator representation (`facts.runtime.start`) merged over the base state `running_work.active`; falls back to the `GET /api/v1/recipes` route when the state names no recipe; keeps the transitional top-level `start_case` fallback for the showroom. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | `declaresProjects()` recognises the consolidated scenario so the live project explorer and single-project handler stay authoritative (the static `GET /api/v1/projects` route is not shadowed); fixture-presence check allows an intentionally empty `facts.runtime.projects` list. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntime.java` | Registers the twenty legacy case names on the runtime's registry so a client that still names `start-epic-adopt`, `git_sources`, or `projects` resolves to its consolidated state instead of the registry's coarse `start.sources`/`projects.explorer` fallback. |

### 2.2 Scenario resource

| File | Change |
|---|---|
| `pm-mock-runtime/src/main/resources/scenarios/projects-and-start.json` | Rewritten as the canonical Scenario 3: 26 states, 3 routes, no `start_case` member. |
| 20 superseded files | Deleted: `projects.json`, `enrolment.json`, `git_sources.json`, `lesson_source.json`, `start.json`, and the 15 `start-<case>.json` files. |

State inventory:

- **Base / project explorer:** `running_work.active` (shared runtime fixture: projects, being-added projects, start defaults and recipes), `projects.explorer`, `projects.ready`, `projects.empty`, `projects.attention`, `projects.being-added`, `projects.unenrolled`, `enrol.scan`, `enrol.digest-mismatch`, `sources.git`, `sources.lessons`.
- **Starting work:** `start.sources`, `start.chat`, `start.documents`, `start.entry-continue`, `start.epic`, `start.epic-adopt`, `start.follow-up`, `start.issue-failed`, `start.lesson`, `start.local-changes`, `start.pull-request`, `start.rate-limited`, `start.recipe`, `start.refused`, `start.unscreened`.
- Every start state carries a full `OperatorStateResource` representation whose `facts.runtime.start` is the case fixture and whose operator links are the links the contract offers for that fixture; each advertised link has a matching scenario link, so `ScenarioLoader` validates the closed operator schema without waivers.
- The `GET /api/v1/recipes` route is the declarative recipe source; each start state inherits the project, landing, documents mode, epic axes, and recipes from `running_work.active` and declares only what differs.

### 2.3 Tests

| File | Change |
|---|---|
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/ProjectsScenarioTest.java` | Points at `projects-and-start#projects.explorer`; adds cases for `projects.empty`, `projects.attention`, `projects.being-added`, `projects.unenrolled`, and `projects.ready`. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/EnrolScenarioTest.java` | Points at `projects-and-start#enrol.digest-mismatch`; adds the `enrol.scan` case. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/GitSourceScenarioTest.java` | Points at `projects-and-start#sources.git`. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/LessonSourceScenarioTest.java` | Points at `projects-and-start#sources.lessons`. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/StartScenarioTest.java` | All 16 case scenarios now resolve to their consolidated start states; adds the state inventory/absence-of-superseded-files checks and fixture-based entry-id assertions. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/StartContractTest.java` | All contract cases run against the consolidated start states; the catalogue test iterates the states' fixtures instead of legacy files. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/StartRegistryTest.java` | New: proves state-fixture seeding, base-state merge, route-based recipe fallback, and the transitional top-level fallback. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/scenario/ScenarioRegistryTest.java` | The "a legacy file wins over its alias" premise moved from `projects` (now deleted) to `reports` (still a legacy file in this wave). |

---

## 3. Test & Verification Results

```
./mvnw verify -Ppre-commit -pl pm-mock-runtime -am
...
Tests run: 395, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **`pm-operator` compatibility check**: `./mvnw -pl pm-operator -am test` passes with the consolidated scenario, including `StartDialogueTest` (15 previously failing cases) and `EpicCommandTest`; the legacy case names now resolve to their consolidated states.

- **`ScenarioLoaderTest`**: loads the consolidated scenario against the closed schema, including operator-state link/event validation.
- **`ProjectsScenarioTest`**: explorer, favourites reordering/toggling, onboarding/abandon, release, unenrolment, empty/attention/being-added/unenrolled/ready cases.
- **`EnrolScenarioTest`**: mismatching and matching digests, accept-edit confirmation, enrol/alias enrol, unenrol, missing store, scan state.
- **`GitSourceScenarioTest`**: PR observe/adopt/refusals, local-changes preview/capture/consume/landing-files.
- **`LessonSourceScenarioTest`**: unclaimed ordering, premise holds/broken, claim/override/owned refusal, retirement, not-found, not-ready.
- **`StartScenarioTest` / `StartContractTest`**: every source kind and failure cause, screening dispositions, recipe choice/parameters, epic ready/adopt/adopt-failed, refusal, chat, follow-up, confined file picker, SSE events (`plan.started`, `epic.started`, `entry.opened`), and the two invariants (rate limit never retried by the runtime; untrusted PR files unless operator-authored).
- **`StartRegistryTest`**: the new seeding seam itself.

No test was skipped, weakened, or removed to obtain a green build. The malformed `accept-edit` refusal test was preserved when the scan-state test was added.

---

## 4. Domain Invariants

| Invariant | Status |
|---|---|
| Java 25+ and CUI code standards | Preserved; no new dependencies. |
| Closed scenario schema | Preserved; `projects-and-start.json` loads with operator-state validation. No `start_case` member in the consolidated file. |
| Standard-scenario architecture | Scenario 3 now hosts all project/start cases; the launcher's `primary.txt` is unchanged. |
| Legacy alias stability | The runtime maps every legacy Scenario 3 case name to its consolidated state, so existing clients (including `pm-operator`'s transcript tests) keep the fixture they name; `ScenarioRegistry`'s own coarse alias table is untouched. |
| Module boundaries | Only `pm-mock-runtime` production code, resources, and tests changed. `pm-operator` and `pm-web-app` untouched. `Scenario.java`, `ScenarioLoader.java`, `ScenarioRegistry.java`, and `StandardScenarios.java` untouched. |
| Wire contracts | No `pm-api` record changed. The operator states in the scenario decode with the existing `pm-api` codecs. |
| SSE integrity | Existing event assertions pass unchanged; no static timed events were added to start states, so they cannot inject spurious frames before an act's event. |

### Note on the pre-commit profile

`./mvnw verify -Ppre-commit` runs the inherited OpenRewrite recipe over the whole reactor. On this branch it reformatted unrelated `pm-api` sources (whitespace/indentation only) because `main` currently carries drift against the inherited parent recipe. Those unrelated reformat changes were reverted after the gate passed, keeping the PR inside the plan's module boundary. The gate itself completed with `BUILD SUCCESS` on the tree containing the changes reported here. The same drift will be visible to CI, which only evaluates build success.

---

## 5. Residual / Deferred

- The transitional top-level `start_case` fallback in `StartRegistry` exists solely for `fully-onboarded.json`; Wave 3 (`mock-runtime-reports-system-designer-plan.md`) removes the field from `Scenario` / `ScenarioLoader` and can then delete the fallback.
- Timed `events` blocks are not attached to start states: start acts are synchronous and the runtime's `StartStateResource` emits the catalogue events (`entry.opened`, `plan.started`, `epic.started`, `plan.abandoned`) through the live registry. Adding static timed events would risk injecting frames ahead of an act's event; event coverage remains assertion-based in `StartScenarioTest`.

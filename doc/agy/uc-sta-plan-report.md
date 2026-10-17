# Completion Report: UC-STA-1..5 Starting Work (Plan Start, Intake & Recipes)

This report documents the implementation of the plan [`doc/agy/uc-sta-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-sta-plan.md), executed under the discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe of [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

Branch: `feat/uc-sta-start-work` · Worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-sta`.

---

## 1. Executive Summary

The five "Starting Work" use cases were implemented end-to-end across the client and runtime modules:

- **UC-STA-1: Start a Plan** — a plan starts from a description, documents, an issue, or a recipe; the server derives and returns the plan id.
- **UC-STA-2: Start an Epic** — an epic starts the same way, with its auto-start flag in force.
- **UC-STA-3: Bring in Documents** — document paths and directories are carried as sources in the intake request.
- **UC-STA-4: Bring in an Issue** — an issue number or URL is carried as a source.
- **UC-STA-5: Run a Recipe** — the project's recipes are listed with their parameters/proposals; a recipe plan's id is derived from the recipe key and the time.

The implementation respects the repository invariants: strict dependency boundaries (`pm-operator`, `pm-mock-runtime`, and `pm-web-app` consume `pm-api` only), pure Java 25 records with Jackson-streaming (`JsonTree`) codecs, catalogue-only user-facing text in both English and German, and mock-first, deterministic flows with live SSE events (`plan.started`, `epic.started`, `intake.created`).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts, codecs & catalogue

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/api/start/SourceKind.java` | Closed enum of the six source kinds and their wire values. |
| `main/java/de/planmarshall/api/start/SourceInput.java` | A source: kind, location, optional title. |
| `main/java/de/planmarshall/api/start/StartPlanRequest.java` | Opens a plan's entry: project, objective, recipe, parameters, documents, issue, title. |
| `main/java/de/planmarshall/api/start/StartEpicRequest.java` | Opens an epic's entry: project, objective, auto-start flag, title. |
| `main/java/de/planmarshall/api/start/PlanStartResult.java` | The started work: id, kind, title, state, follow link. |
| `main/java/de/planmarshall/api/start/RecipeParameter.java` | A recipe parameter: name, value, resolved options, proposal, availability, reason. |
| `main/java/de/planmarshall/api/start/RecipeRef.java` | A recipe: key, name, description, origin, takes-request flag, parameters. |
| `main/java/de/planmarshall/api/start/IntakeDraft.java` | A draft intake session: id, project, prompt, sources, creation time. |
| `main/java/de/planmarshall/api/start/package-info.java` | Package documentation. |
| `main/resources/messages/start.en.properties` | English catalogue of the `start` area (steps, sources, causes, recipe, decomposition, web modal). |
| `main/resources/messages/start.de.properties` | German catalogue, key-for-key identical. |
| `main/java/de/planmarshall/api/message/MessageCatalogue.java` | Registers `start` as a supported area. |
| `test/java/de/planmarshall/api/start/StartRecordsTest.java` | Round-trip, defaulting, and negative-parsing contracts for every record. |
| `test/java/de/planmarshall/api/message/StartCatalogueTest.java` | EN/DE key parity, specification-key presence, ICU formatting. |

### 2.2 `pm-mock-runtime` — scenario, handlers & SSE

| File | Purpose |
|---|---|
| `main/resources/scenarios/start.json` | The `start` scenario: status, three projects (one not ready), and the start states. |
| `main/java/de/planmarshall/mock/StandardScenarios.java` | Registers `START = "start"` in the standard list. |
| `main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Dynamic handlers for `POST /api/v1/projects/{id}/plans/start`, `POST /api/v1/projects/{id}/epics/start`, `GET /api/v1/recipes`, `POST /api/v1/intake/draft`; the setup gate; id derivation; SSE broadcasts. |
| `test/java/de/planmarshall/mock/StartScenarioTest.java` | Scenario loading, recipes, plan/epic starts, recipe-id derivation, refusal, unknown project, draft intake. |

### 2.3 `pm-operator` — the console verb

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/operator/StartCommand.java` | `start plan`, `start epic`, and `start recipe` sub-verbs; posts the `pm-api` requests, resolves catalogue text, sanitizes ids via `OutputSafety`. |
| `main/java/de/planmarshall/operator/PmOperator.java` | Registers `start` in the command tree. |
| `test/java/de/planmarshall/operator/StartCommandTest.java` | Usage, plan/epic/recipe invocations, request bodies, defaults, refusals. |
| `test/java/de/planmarshall/operator/PmOperatorProcessIT.java` | Adds the `start` PM-SEC-3 terminal-refusal case. |

### 2.4 `pm-web-app` — the web views

| File | Purpose |
|---|---|
| `src/lib/api/client.ts` | Start/receipt/recipe/draft types and `getRecipes`, `startPlan`, `startEpic`, `draftIntake`. |
| `src/lib/components/StartWorkModal.svelte` | The modal that starts a plan/epic from sources or a recipe and reports the created work. |
| `src/lib/components/RecipeSelector.svelte` | The recipe list with resolvable parameters, their proposals, and their refusal reasons. |
| `src/lib/components/SourceIntakePanel.svelte` | Description, documents, and issue inputs building the source list. |
| `src/lib/stores/start.ts` | The store that opens the modal for a project and kind. |
| `src/lib/views/OverviewView.svelte` | `Start the plan` / `Start the epic` actions in the project header. |
| `src/routes/+page.svelte` | Mounts the start modal. |
| `src/lib/i18n/index.ts` | Catalogue fallbacks for the new `start.*` keys. |
| `src/lib/components/*.test.ts` | Vitest suites for the modal, the recipe selector, and the source panel. |

### 2.5 `pm-web-e2e` — the browser suite

| File | Purpose |
|---|---|
| `tests/start.spec.js` | Playwright suite: a plan from sources, a recipe plan, an epic, and a refusal. |

---

## 3. Test & Verification Results

| Layer | Suite | Result |
|---|---|---|
| `pm-api` | `StartRecordsTest`, `StartCatalogueTest` | 16 tests, 0 failures |
| `pm-mock-runtime` | `StartScenarioTest`, `StandardScenariosTest`, `ScenarioRegistryTest` | 16 tests, 0 failures |
| `pm-operator` | `StartCommandTest` | 8 tests, 0 failures |
| `pm-operator` (process) | `PmOperatorProcessIT` (incl. `start` refusal) | green under `verify` |
| `pm-web-app` | full Vitest suite | 131 tests, 0 failures (27 files) |
| `pm-web-e2e` | `start.spec.js` (Chromium) | 4 tests, 0 failures |

---

## 4. Domain Invariants

- **Java 25 / Lombok / packages** — records with compact constructors, `JsonTree` (Jackson streaming) codecs only; no `jackson-databind`, no CDI/Quarkus. ✅
- **No uncatalogued user-facing literals** — every client string binds to a `start.*` key; the operator prints via `MessageCatalogue`. ✅
- **Bilingual catalogue** — `start.en.properties` and `start.de.properties` carry identical key sets, asserted by `StartCatalogueTest`. ✅
- **Mock runtime completeness** — the `start` scenario loads through `ScenarioRegistry`, and every documented branch (plan, epic, recipe, refusal, draft) is served and asserted. ✅
- **Both clients, one state** — a start is broadcast on the SSE stream (`plan.started`, `epic.started`, `intake.created`) and the terminal and browser drive the same endpoints. ✅
- **`pm-operator` security** — the verb runs behind `OperatorGate`; ids are stripped of control sequences via `OutputSafety`. ✅
- **Responsive web layout** — the modal and panels collapse to full-screen below 480px with 44px touch targets. ✅

---

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
...
pm-api .......... SUCCESS
pm-exec ......... SUCCESS
pm-relay ........ SUCCESS
pm-operator ..... SUCCESS
pm-mock-runtime . SUCCESS
pm-web-app ...... SUCCESS
pm-web-e2e ...... SUCCESS
BUILD SUCCESS   (03:19 min)
```

`git status` was clean of build artifacts and reformatted sources after the build; the OpenRewrite/Checkstyle/Enforcer/Javadoc gates all passed with zero regressions.

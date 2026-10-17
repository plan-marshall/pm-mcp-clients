# Completion Report: UC-CTL-1..5 Controlling Work (Plan & Epic Actions, Holds, Settings)

Companion to [`doc/agy/implemented/uc-ctl-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-ctl-plan.md), executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the recipe in [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

Branch: `feat/uc-ctl-control` · Worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-ctl`

---

## 1. Executive Summary

This package implements the client contract for **Controlling Work** — the actions a plan and an epic offer per state, the five kinds of hold and the acts that resolve them, and a plan's own settings with their revision guard — across the four client/runtime modules, with a bilingual catalogue and a stateful mock scenario.

Delivered end-to-end (contract → runtime → clients → tests):

- **`pm-api`** — a new `de.planmarshall.api.control` package: the `PlanAction` and `HoldKind` closed enumerations, `ActionAvailability`, `WorkActions`, `HoldRecord`, `HoldResolutionRequest`/`Result`, `PlanSettingsRecord`/`Value`/`UpdateRequest`/`UpdateResult`, `PlanActionRequest`, and the SSE events `HoldChangedEvent` (`hold.opened`, `hold.changed`, `hold.cleared`) and `PlanControlEvent` (`plan.paused`, `plan.resumed`, `plan.cancelled`). Jackson-streaming codecs with round-trip tests. A new **`control` message area** in both English and German, plus the shared `common.conflict.*` keys.
- **`pm-mock-runtime`** — a new stateful `control` scenario and dynamic routes for plan/epic actions (`GET`/`POST …/actions[/{action}]`), holds (`GET …/holds`, `POST …/holds/{id}/resolve`), and settings (`GET`/`PUT …/settings` with the `409 conflict` guard and a refused-value `400`), broadcasting the hold and plan-control SSE events.
- **`pm-operator`** — `plan resolve <id> <holdId> --act <act>` and `plan settings <id> [--set aspect=value | --remove aspect]`, resolving holds and applying plan settings against the revision the dialogue was opened with, with conflict re-apply (`--yes`) and `--no-input` stop.
- **`pm-web-app`** — typed client methods (`getPlanActions`, `applyPlanAction`, `getEpicActions`, `applyEpicAction`, `getPlanHolds`, `resolveHold`, `getPlanSettings`, `updatePlanSettings` with `409`-tolerant conflict handling) and three Svelte 5 components: `ControlActionsBar`, `HoldResolutionModal`, `PlanSettingsModal`.
- **`pm-web-e2e`** — `control.spec.js`, the runtime-contract end-to-end suite for the action, hold, and settings endpoints.

### Bounded scope note (honest boundaries)

The plan's broadest surfaces — the chat sub-workflow (`chat.*`), the findings/watches **review** (UC-CTL-3) as an interactive client dialogue, the epic decomposition re-run, and embedding the three Svelte components into the existing `PlanView`/`EpicView` routes — are **represented at the contract and scenario level but not fully wired into a browser route** in this package. The `control` catalogue carries the review/chat keys so those surfaces can be built on the same contract. This boundary is flagged for the follow-up packages (`cli-actions`, `web-actions`, `control-contract` completion).

---

## 2. Delivered Artifacts

### `pm-api`

New (`src/main/java/de/planmarshall/api/control/`):

| Artifact | Purpose |
|---|---|
| `package-info.java` | Package contract for Controlling Work (UC-CTL-1..5). |
| `PlanAction.java` | Closed enum of plan actions (`pause`, `start`, `cancel`, `abandon`, `chat`, `settings`). |
| `HoldKind.java` | Closed enum of the five hold kinds. |
| `ActionAvailability.java` | One action with availability + disabled reason. |
| `WorkActions.java` | The actions of a plan or an epic in its state (`plan.actions` / `epic.actions`). |
| `HoldRecord.java` | A hold: kind, cause, attempts, integrity file facts, and its acts. |
| `HoldResolutionRequest.java` / `HoldResolutionResult.java` | `hold.resolve` request and `hold.apply` result. |
| `PlanSettingsRecord.java` / `PlanSettingsValue.java` | `plan-settings.open`: values per aspect with origin, revision-stamped. |
| `PlanSettingsUpdateRequest.java` / `PlanSettingsUpdateResult.java` | `plan-settings.value`/`.apply` with the conflict and too-late outcomes. |
| `PlanActionRequest.java` | Request payload of a plan/epic action (pause point, reason, PR close, scope, plans). |
| `HoldChangedEvent.java` | SSE `hold.opened` / `hold.changed` / `hold.cleared`. |
| `PlanControlEvent.java` | SSE `plan.paused` / `plan.resumed` / `plan.cancelled`. |

Modified:
- `pm-api/src/main/java/de/planmarshall/api/message/MessageCatalogue.java` — registered the `control` area.

Catalogue (new):
- `pm-api/src/main/resources/messages/control.en.properties`
- `pm-api/src/main/resources/messages/control.de.properties`
- `common.{en,de}.properties` — added `common.conflict.*` (changed meanwhile; apply again; keep).

Tests (new):
- `pm-api/src/test/java/de/planmarshall/api/control/ControlRecordsTest.java` (9 tests)
- `pm-api/src/test/java/de/planmarshall/api/message/ControlCatalogueTest.java` (4 tests)

### `pm-mock-runtime`

- `src/main/resources/scenarios/control.json` — stateful scenario: running/pausing/held/ended plans, an epic running and one landing, an integrity pause and a held item, and a plan with custom settings.
- `src/main/java/de/planmarshall/mock/StandardScenarios.java` — added `CONTROL`.
- `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — dynamic control routes and in-memory state (plan/epic states, holds, settings revision), broadcasting the hold and plan-control events.
- `src/test/java/de/planmarshall/mock/ControlScenarioTest.java` (7 tests)
- `src/test/java/de/planmarshall/mock/StandardScenariosTest.java` — updated for the new scenario.

### `pm-operator`

- `src/main/java/de/planmarshall/operator/PlanCommand.java` — added `ResolveCommand` (`plan resolve`) and `SettingsCommand` (`plan settings`).
- `src/test/java/de/planmarshall/operator/ControlCommandTest.java` (9 tests)

### `pm-web-app`

- `src/lib/api/client.ts` — control types and API methods (`requestWithConflict` handles the `409`).
- `src/lib/i18n/index.ts` — `control.*` and `common.conflict.*` fallbacks.
- `src/lib/components/ControlActionsBar.svelte` (+ `.test.ts`, 5 tests)
- `src/lib/components/HoldResolutionModal.svelte` (+ `.test.ts`, 6 tests)
- `src/lib/components/PlanSettingsModal.svelte` (+ `.test.ts`, 5 tests)

### `pm-web-e2e`

- `tests/control.spec.js` — the runtime-contract end-to-end suite (runnable with `-Pe2e`; skipped by default).

---

## 3. Test & Verification Results

| Suite | Tests | Result |
|---|---|---|
| `ControlRecordsTest` (pm-api) | 9 | pass |
| `ControlCatalogueTest` (pm-api) | 4 | pass |
| `ControlScenarioTest` (pm-mock-runtime) | 7 | pass |
| `StandardScenariosTest` (pm-mock-runtime) | updated | pass |
| `ControlCommandTest` (pm-operator) | 9 | pass |
| `ControlActionsBar.test.ts` (pm-web-app) | 5 | pass |
| `HoldResolutionModal.test.ts` (pm-web-app) | 6 | pass |
| `PlanSettingsModal.test.ts` (pm-web-app) | 5 | pass |

Coverage of the use-case happy paths and variations:

- **UC-CTL-1 / UC-CTL-2 (actions by state)** — a running plan offers pause/cancel/chat/settings; a paused plan offers start/abandon; an ended plan offers chat alone; an epic landing offers archive disabled with `the ledger is landing`; a disabled chat carries its reason. The pause point, the start (withdraw/resume), and the cancel with reason are covered end to end and by SSE assertions.
- **UC-CTL-4 (holds)** — an integrity pause exposes the tampered file with recorded and current size and digest and the acts `retry`/`accept`/`abort`; a `retry` still holds and broadcasts `hold.changed`, an `accept` releases and broadcasts `hold.cleared`, and the plan goes on.
- **UC-CTL-5 (settings)** — values per aspect with origin (`plan`/`project`/`machine`), the revision guard (`409 conflict` with the current value), re-apply, and a refused value (`landing.main` → `400 refused`).
- **Both clients, one state** — the terminal (`plan resolve`, `plan settings`) and the web (`client.ts` + components) resolve holds and change settings against the same contract and the same mock scenario.

---

## 4. Domain Invariants

- **Java 25 / Lombok / immutability** — all new `pm-api` types are immutable records or enums; JSON via Jackson streaming (`JsonTree`), no `jackson-databind`, no reflection.
- **No uncatalogued user-facing strings** — the new catalogue keys live in `control.{en,de}.properties` and `common.{en,de}.properties`; `MessageCatalogueTest` verifies area isolation and en/de key parity and `ControlCatalogueTest` asserts the specification keys.
- **Bilingual completeness** — `control.en` and `control.de` have byte-identical key sets (asserted); ICU select/plural templates are used for the parameterized texts.
- **Mock runtime completeness** — the `control` scenario loads through `ScenarioRegistry`, is registered in `StandardScenarios.ALL`, and serves every documented branch (running/pausing/held/ended plan, running/landing epic, integrity pause, held item, custom settings).
- **`PM-SEC-3` / output safety** — the operator verb renders values through `OutputSafety.stripControlSequences`; the mock runtime authorizes every control route through the existing runtime-token / device-secret gate.
- **Responsive layout** — the three components reflow below 480px (touch-sized 44px targets), matching the web app's 320px floor.
- **Integration tests** — the Playwright contract suite asserts the same endpoints the wiring will use.

---

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-api ............................................. SUCCESS
[INFO] pm-exec ............................................ SUCCESS
[INFO] pm-relay ........................................... SUCCESS
[INFO] pm-operator ........................................ SUCCESS
[INFO] pm-mock-runtime .................................... SUCCESS
[INFO] pm-web-app ......................................... SUCCESS
[INFO] pm-web-e2e ......................................... SUCCESS
[INFO] BUILD SUCCESS
[INFO] Total time:  03:55 min
```

OpenRewrite, the license-header check, Checkstyle, the Enforcer rules (`clients-depend-on-pm-api-only`, `jackson-core-only`, deployment-coordinate guards), JavaDoc, the full unit-test reactor, and the `npm` build/test steps for the web modules all pass; `git status` shows only the intended changes (no reformatting churn).

---

## 6. Personas Applied

| Phase | Personas | Where |
|---|---|---|
| Trace & design alignment | documenter | Mapped `06-controlling-work.adoc`, `work-actions.adoc`, `19-actions.adoc`, and the text specs onto the contract. |
| `pm-api` | implementer, module-tester | Immutable records, enums, bilingual catalogue, round-trip and catalogue tests. |
| `pm-mock-runtime` | implementer, integration-tester | Stateful scenario, dynamic routes, SSE events, HTTP contract tests. |
| `pm-operator` | implementer, security-expert | `plan resolve`/`plan settings`, output safety, revision-guarded writes. |
| `pm-web-app` | implementer | Typed client methods and three Svelte 5 components. |
| Verification | integration-tester | `./mvnw verify -Ppre-commit` and the Playwright contract suite. |
| Review gate | code-reviewer | Adversarial pass over the diff before the PR. |

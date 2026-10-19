# Implementation Plan: `mock-runtime-onboarding-and-entry` (Scenario 1: Consolidated `onboarding.json` with Full `OperatorStateResource` States)

This implementation plan governs **Scenario 1 (`onboarding.json`)** of the 5-scenario mock-runtime architecture: consolidating the 23 fragmented `onboarding*.json`, `entry*.json`, `paired-devices.json`, `web-pairing.json`, `operator-interaction.json`, and `status-only.json` files into a single, comprehensive, multi-state `onboarding.json` scenario where every state carries a valid `OperatorStateResource` decoded by `pm-api` codecs.

It operationalizes [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-ONB-1..10`](../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc), [`UC-ENT-1..2`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc) |
| **Specifications** | [`05-mock-runtime.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc) |
| **Local Client Design** | [`doc/design/shared/onboarding.adoc`](../design/shared/onboarding.adoc), [`doc/design/shared/entry.adoc`](../design/shared/entry.adoc), [`doc/design/shared/paired-devices.adoc`](../design/shared/paired-devices.adoc) |
| **Roadmap Packages** | [`doc/plans/implemented/onboarding-contract.adoc`](../plans/implemented/onboarding-contract.adoc), [`doc/plans/implemented/entry-contract.adoc`](../plans/implemented/entry-contract.adoc), [`doc/plans/implemented/operator-interaction-contract.adoc`](../plans/implemented/operator-interaction-contract.adoc) |
| **Prerequisite Plan** | [`doc/agy/mock-runtime-foundation-and-consolidation-plan.md`](mock-runtime-foundation-and-consolidation-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

---

## 2. Scope & Objectives

1. **Consolidate 23 Fragmented Files into Scenario 1 (`onboarding.json`)**:
   - Merge `onboarding.json`, `onboarding-happy.json`, `onboarding-harness-all-failed.json`, `onboarding-harness-not-logged-in.json`, `onboarding-harness-tokens-complete.json`, `onboarding-harness-tokens-not-reported.json`, `onboarding-harness-tokens-partial.json`, `onboarding-refusals.json`, `onboarding-settings-conflict.json`, `onboarding-unknown-harness.json`, `entry.json`, `entry-device-lifecycle.json`, `entry-fresh-code.json`, `entry-lan-conflict.json`, `entry-lan-credentials.json`, `entry-onboarding.json`, `entry-paired-browser.json`, `entry-ssh-session.json`, `entry-stream-close.json`, `entry-unonboarded.json`, `paired-devices.json`, `web-pairing.json`, `operator-interaction.json`, and `status-only.json` into **`pm-mock-runtime/src/main/resources/scenarios/onboarding.json`**.
   - Remove the 22 redundant JSON files from `src/main/resources/scenarios/` and map their legacy constants in `ScenarioRegistry` to `onboarding#<state>` so all existing unit, CLI, and E2E tests continue to resolve without cluttering the scenario directory.
2. **Migrate Every State to Full `OperatorStateResource` & `EventEnvelope`**:
   - Replace all flat legacy representations (`"scope": "machine", "state": "onboard.mode"`) and dummy stubs (`{"status": "ready"}`) with valid `OperatorStateResource` objects (`scope: {"kind": "machine"}`, `state: {"state_id": "...", "kind": "operator_wait" | "working" | "completed"}`, `summary`, `detail`, `links` array with `id`, `rel`, `method`, `href`, `label_key`, and `data`).
   - Ensure every state link in `states.<id>.links` matches an `OperatorLink.id` in `representation.links` and every scheduled event in `events` is a valid `EventEnvelope`.
3. **Complete All Missing `onboarding-<case>` and `entry-<case>` States**:
   - Add dedicated, reachable states in `onboarding.json` for all cases required by `onboarding-contract.adoc` and `entry-contract.adoc`:
     - Guided vs. automatic onboarding consent (`onboard.mode`, `auto.consent`, `auto.scan`)
     - Language selection (`language.choose`)
     - Harness scan consent, discovery, login probe, not-logged-in blocked state, all-failed state, unknown harness path addition, and token reporting levels (`complete`, `partial`, `not_reported`)
     - GitHub CLI & Sonar checks, credential store/refusal, and defaults (`posture`, `documents_mode`, `cleanup_durations`)
     - Settings revision conflict (`409 conflict` on stale revision)
     - Entry & pairing states: unonboarded entry, SSH session entry, paired browser entry, fresh pairing code issuance, wrong pairing code rejection (`400 pairing_invalid`), device rename, revoke current device (`stream.closed` with `device_revoked`), revoke other devices, LAN listener open/close/port conflict (`409 web_listener_conflict`), and LAN credentials.

---

## 3. Concurrency & Isolation

Depends on Wave 1 (`mock-runtime-foundation-and-consolidation`, which already pre-registers all legacy scenario aliases in `ScenarioRegistry`). Runs in parallel with Waves 2b–2d (`mock-runtime-running-work`, `mock-runtime-projects-and-start`, `mock-runtime-reports-system-designer`) with zero shared Java or JSON files.

### Boundary Rules
- **Sole Owner of**: `pm-mock-runtime/src/main/resources/scenarios/onboarding.json` (and deletion of the 22 superseded `onboarding-*.json`, `entry-*.json`, `paired-devices.json`, `web-pairing.json`, `operator-interaction.json`, `status-only.json` files), plus `OnboardingScenarioTest.java`, `EntryScenarioTest.java`, `PairedDevicesScenarioTest.java`, and `OperatorInteractionScenarioTest.java`.
- **No Touch**: `ScenarioRegistry.java` / `StandardScenarios.java` (owned by `mock-runtime-foundation-and-consolidation-plan.md`), `doc/plans/README.adoc`, `pm-operator`, and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-onboarding-entry` on branch `feat/mock-runtime-onboarding-and-entry` off latest `main`.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for trust boundaries & input validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/mock-runtime-onboarding-and-entry-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-onboarding-and-entry-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-onboarding-and-entry-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Onboarding States Migration** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Convert all onboarding states to `OperatorStateResource` with `links` keyed by `id` and `EventEnvelope` events. |
| **Phase 2: Entry & Pairing Consolidation** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Merge `entry*.json`, `paired-devices.json`, `web-pairing.json` into `onboarding.json` states & routes. |
| **Phase 3: Scenario & Client Test Suite** | `plan-marshall-persona-module-tester`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Verify every state in `onboarding.json` decodes with `pm-api` codecs and passes `OnboardingScenarioTest` & `EntryScenarioTest`. |
| **Phase 4: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit gate and 100% resolution of automated review findings. |

---

## 6. Phased Technical Recipe

### Phase 1: Migrate Onboarding States to `OperatorStateResource` in `onboarding.json`
- [ ] **1.1 Convert Guided & Automatic Onboarding States**:
  - Rewrite all states in `onboarding.json` (`onboard.mode`, `auto.consent`, `auto.scan`, `language.choose`, `harness.consent`, `harness.choose`, `harness.add`, `harness.probe-consent`, `harness.probe`, `harness.blocked`, `harness.all-failed`, `harness.unknown`, `harness.confirm`, `github.consent`, `github.check`, `sonar.consent`, `sonar.key`, `defaults.choose`, `onboard.summary`, `onboard.completed`, `settings.conflict`) to carry full `OperatorStateResource` representations (`scope`, `state`, `summary`, `detail`, `links`, `data`).
  - Ensure all `check.result` and `state.changed` events in `events` use complete `EventEnvelope` objects.
- [ ] **1.2 Align `MockRuntimeEngine` Onboarding Route Helpers**:
  - Update `handleOnboardingRoutes` and `handleMachineSettingsRoutes` in `MockRuntimeEngine` so state transitions and responses preserve the `OperatorStateResource` envelope from `onboarding.json` while applying stateful mutations (such as settings revision increments and `409 conflict` on stale revision).

### Phase 2: Consolidate Entry, Pairing & Device Lifecycle into `onboarding.json`
- [ ] **2.1 Merge Entry & Pairing States**:
  - Add entry/pairing states (`entry.ready`, `entry.unonboarded`, `entry.paired-browser`, `entry.ssh-session`, `entry.fresh-code`, `entry.lan-conflict`, `entry.lan-credentials`, `entry.device-lifecycle`, `entry.stream-close`, `status.only`, `wait.choose`, `wait.check`, `wait.moved`) into `onboarding.json` with valid `OperatorStateResource` representations and `OperatorLink` definitions.
- [ ] **2.2 Remove Superseded Scenario Files & Wire Registry Aliases**:
  - Delete the 22 superseded scenario files (`onboarding-*.json`, `entry*.json`, `paired-devices.json`, `web-pairing.json`, `operator-interaction.json`, `status-only.json`) from `pm-mock-runtime/src/main/resources/scenarios/`.
  - Map their constants in `ScenarioRegistry` to `onboarding#<state>` so `MockRuntime.start(StandardScenarios.ONBOARDING_HAPPY)` or `POST /mock/scenario` with a legacy case name activates `onboarding` at the corresponding start state.

### Phase 3: Verification & Scenario Contract Tests
- [ ] **3.1 Comprehensive Scenario Tests**:
  - Update and expand `OnboardingScenarioTest`, `EntryScenarioTest`, `PairedDevicesScenarioTest`, and `OperatorInteractionScenarioTest` to assert that every state in `onboarding.json` decodes via `OperatorStateResource.fromTree` and every contract case (including refusals, unknown harness, token reporting levels, settings conflict, wrong pairing code, device revocation, and LAN conflict) is verified.
- [ ] **3.2 Pre-Commit & Operational Gate**:
  - Run `./mvnw verify -Ppre-commit` and execute PR/review workflow per [`doc/agy/runbook.md`](runbook.md).

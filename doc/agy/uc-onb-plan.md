# Implementation Plan: UC-ONB-1..10 Machine Onboarding & Machine Settings

This plan coordinates the end-to-end implementation of use cases `UC-ONB-1: Onboard a Machine` through `UC-ONB-10: Change the Machine's Settings` across all client and runtime modules in `pm-mcp-clients`.

It operationalizes the technical recipe from [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`plan-marshall-documentation/doc/use-cases/01-onboarding.adoc#UC-ONB-1`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/01-onboarding.adoc) to [`#UC-ONB-10`](file:///Users/oliver/git/plan-marshall-documentation/doc/use-cases/01-onboarding.adoc) (Interactive & Automatic Onboarding, Languages, Harnesses, Models, Roles, Credential Store, Providers, Plan Defaults, Machine Settings) |
| **Specifications** | [`01-grammar.adoc#machine-scope`](file:///Users/oliver/git/pm-mcp-core/doc/specification/workflow-dsl/01-grammar.adoc), [`14-machine-onboarding.adoc#state-catalogue-onboarding`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/14-machine-onboarding.adoc), [`#state-catalogue-settings`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/14-machine-onboarding.adoc), [`#state-catalogue-auto`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/14-machine-onboarding.adoc), [`19-onboarding-aspects-models-providers.adoc#state-catalogue-onboarding-aspects-2`](file:///Users/oliver/git/pm-mcp-core/doc/specification/state-catalogue/19-onboarding-aspects-models-providers.adoc), [`12-machine-stores.adoc#consent`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc), [`#model-catalogue`](file:///Users/oliver/git/pm-mcp-core/doc/specification/store-schemas/12-machine-stores.adoc), [`01-profile-schema.adoc#harness-scan`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc), [`#token-usage`](file:///Users/oliver/git/pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc), [`01-cli-and-installer.adoc#auto`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/01-cli-and-installer.adoc), [`#two-channels`](file:///Users/oliver/git/plan-marshall-documentation/doc/specification/cli-and-security/01-cli-and-installer.adoc) |
| **Local Client Design** | Shared: [`doc/design/shared/consents.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/consents.adoc), [`work-actions.adoc#conflict`](file:///Users/oliver/git/pm-mcp-clients/doc/design/shared/work-actions.adoc#conflict)<br>Web: [`doc/design/web-app/onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/onboarding.adoc), [`machine-settings.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/web-app/machine-settings.adoc)<br>CLI: [`doc/design/terminal-client/README.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/design/terminal-client/README.adoc) |
| **Roadmap Packages** | [`doc/plans/onboarding-contract.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/onboarding-contract.adoc), [`doc/plans/cli-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/cli-onboarding.adoc), [`doc/plans/web-onboarding.adoc`](file:///Users/oliver/git/pm-mcp-clients/doc/plans/web-onboarding.adoc) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) |

---

## 2. Parallel Isolation & Non-Overlap Invariants (vs `UC-STA-1..5` & `UC-PLN-3`)

`UC-ONB-1..10` executes concurrently with `UC-STA-1..5: Starting Work` (`feat/uc-sta-start-work`) and `UC-PLN-3: Process Designer` (`feat/uc-pln-3-designer`). Strict non-overlap is guaranteed across all repository modules:

1. **Contracts & API (`pm-api`)**:
   - `UC-ONB-1..10`: Owns `de.planmarshall.api.onboarding.*` and `messages/onboarding.{en,de}.properties`, `messages/settings.{en,de}.properties`.
   - `UC-STA-1..5`: Owns `de.planmarshall.api.start.*` and `messages/start.{en,de}.properties`.
   - `UC-PLN-3`: Owns `de.planmarshall.api.designer.*` and `messages/designer.{en,de}.properties`.
   - Zero shared model classes, enums, or resource properties.
2. **Mock Runtime (`pm-mock-runtime`)**:
   - `UC-ONB-1..10`: Routes `/api/v1/onboarding/*`, `/api/v1/machine/*`, scenario `scenarios/onboarding.json`.
   - `UC-STA-1..5`: Routes `/api/v1/projects/{id}/plans/start`, `/api/v1/intake/*`, scenario `scenarios/start.json`.
   - `UC-PLN-3`: Routes `/api/v1/workflows/*`, scenario `scenarios/designer.json`.
   - Disjoint route dispatch tables and scenario files.
3. **CLI Client (`pm-operator`)**:
   - `UC-ONB-1..10`: Owns `OnboardCommand.java` (`onboard`, `settings machine`).
   - `UC-STA-1..5`: Owns `StartCommand.java` (`start plan`, `start epic`).
   - `UC-PLN-3`: Zero CLI changes (web-only).
   - Disjoint Picocli verb classes.
4. **Web Client (`pm-web-app`)**:
   - `UC-ONB-1..10`: Routes `/onboarding` and `/settings/machine` with `OnboardingView.svelte` and `MachineSettingsView.svelte`.
   - `UC-STA-1..5`: `StartWorkModal.svelte` (modal within existing views).
   - `UC-PLN-3`: Route `/designer` with `ProcessDesigner.svelte`.
   - Disjoint Svelte components and route views.
5. **E2E & Integration Tests**:
   - `UC-ONB-1..10`: `pm-web-e2e/tests/onboarding.spec.js` and `OnboardCommandTest.java`.
   - `UC-STA-1..5`: `pm-web-e2e/tests/start.spec.js` and `StartCommandTest.java`.
   - `UC-PLN-3`: `pm-web-e2e/tests/designer.spec.js`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/uc-onb-machine /Users/oliver/git/worktrees/pm-mcp-clients-uc-onb main`).
- [ ] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for code & unit tests
  - `persona-integration-tester` for multi-module/integration tests
  - `persona-security-expert` for credential store options and provider validation
  - `persona-documenter` for documentation & ADRs
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [ ] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/uc-onb-plan-report.md`) and initial plan moved to `doc/agy/implemented/uc-onb-plan.md`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` for code/config; skipped for standalone documentation changes).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/uc-onb-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Verify machine scope states (`onboard.entry` to `onboard.completed`), consent schema, harness scan, role assignment, and settings conflict rules. |
| **Phase 2: `pm-api`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Immutable Java 25 records, Jackson streaming parsing, bilingual resource bundles (`onboarding.{en,de}.properties`, `settings.{en,de}.properties`). |
| **Phase 3: Mock Runtime** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-integration-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:cui-cui-http-testing` | Stateful scenario JSON (`onboarding.json`), dynamic route handlers, SSE broadcast (`onboarding.step_completed`, `machine.settings_changed`). |
| **Phase 4: `pm-operator`** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-security-expert` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Subcommands `onboard [--auto]`, `settings machine`, harness table, model choice, credential store configuration, terminal confirmation gates (`PM-SEC-3`). |
| **Phase 5: `pm-web-app`** | `plan-marshall-persona-implementer`<br>`pm-dev-frontend:javascript` | `pm-dev-frontend:css`<br>`pm-dev-frontend:jest-testing` | Multi-step onboarding wizard (`/onboarding`), machine settings view (`/settings/machine`), consent toggle cards, Vitest tests. |
| **Phase 6: Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration` | `PmOperatorProcessIT` process execution, Playwright E2E browser test suite (`onboarding.spec.js`). |
| **Phase 7: Review Gate** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Adversarial refutation of candidate bot findings, 100% resolution bar. |

---

## 5. Phased Task Breakdown

### Phase 1: Trace Matrix & Specification Alignment
- Load `plan-marshall-persona-documenter`.
- Map onboarding aspects and settings:
  - Consents: Kind, description, rememberable flag, default.
  - Languages: Locale choice with `en` base and `de`.
  - Harnesses: Scan results, detected vs unknown, login probe diagnostics.
  - Models & Roles: Model catalogue, default assignments per role class.
  - Credential Store: Operator choice (keyring, encrypted file).
  - Providers: Declared provider configurations and verification checks.
  - Plan Defaults: Plan axes (`landing.pull-request`, `landing.main`, `landing.files`).
  - Machine Settings: Revision-guarded settings updates (`409 Conflict`).
- Tokens, not money: verify that consent and model estimates report token counts and coverage figures, never costs.

### Phase 2: Contracts, Models & Message Catalogue (`pm-api`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-core`.
- Create records in `de.planmarshall.api.onboarding.*`:
  - `MachineOnboardingState.java`: Current aspect, completed steps, auto flag.
  - `ConsentRecord.java`: Consent kind, status, remember flag.
  - `HarnessScanResult.java`: Harness name, path, version, login probe status, token reporting coverage.
  - `ModelCatalogueEntry.java`: Model ID, provider, context window, capabilities.
  - `RoleAssignment.java`: Role class, assigned model ID, temperature.
  - `MachineSettingsRecord.java`: Current settings (documents mode, cleanup durations, consents), revision.
  - `MachineSettingsUpdateRequest.java`: Settings update with revision conflict guard (`409 Conflict`).
- Bilingual catalogues: `messages/onboarding.{en,de}.properties` and `messages/settings.{en,de}.properties`.
- Unit tests with `plan-marshall-persona-module-tester`: `OnboardingRecordsTest.java`, `OnboardingCatalogueTest.java`.

### Phase 3: Mock Runtime Scenarios & Dynamic Endpoints (`pm-mock-runtime`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:cui-cui-http-testing`.
- Create scenario fixture `scenarios/onboarding.json`:
  - Machine uninitialized, step-by-step aspect progression, harness probe failures, auto-onboarding flow, concurrent settings conflict.
- Implement endpoints in `MockRuntimeEngine.java`:
  - `GET /api/v1/onboarding/state`
  - `POST /api/v1/onboarding/step`
  - `POST /api/v1/onboarding/auto`
  - `GET /api/v1/machine/settings`
  - `PUT /api/v1/machine/settings` (with `409 Conflict` check)
- Unit tests: `OnboardingScenarioTest.java`.

### Phase 4: Terminal Client Integration (`pm-operator`)
- Load `plan-marshall-persona-implementer` and `pm-dev-java:java-security`.
- Implement `OnboardCommand.java`:
  - `onboard [--auto]`: Interactive or automatic onboarding wizard.
  - `settings machine [--set aspect=value]`: Inspect and modify machine-level settings.
- Enforce PM-SEC-3 interactive terminal verification with `--yes` override.
- Unit and process tests: `OnboardCommandTest.java` and `PmOperatorProcessIT.java`.

### Phase 5: Web Application Views (`pm-web-app`)
- Load `plan-marshall-persona-implementer` and `pm-dev-frontend:javascript`.
- Extend `client.ts` with onboarding and machine settings API methods.
- Create `OnboardingView.svelte`: Guided multi-step wizard (`/onboarding`).
- Create `MachineSettingsView.svelte`: Dedicated view for machine configuration (`/settings/machine`).
- Vitest unit tests: `OnboardingView.test.ts`, `MachineSettingsView.test.ts`.

### Phase 6: Verification & End-to-End Suite
- Load `plan-marshall-persona-integration-tester`.
- Write Playwright E2E suite: `pm-web-e2e/tests/onboarding.spec.js`.
- Execute `./mvnw verify -Ppre-commit` and ensure 100% clean reactor compilation.

---

## 6. Definition of Done
1. All 10 use cases (`UC-ONB-1..10`) completely implemented across CLI and Web clients.
2. Invariant "Both clients, one state" verified via real-time SSE event subscriptions.
3. 100% of user-facing messages externalized in bilingual property bundles (`onboarding.{en,de}.properties`, `settings.{en,de}.properties`).
4. Full reactor build `./mvnw verify -Ppre-commit` passes with zero regressions.
5. Completion report documented at `doc/agy/uc-onb-plan-report.md` and initial plan archived to `doc/agy/implemented/uc-onb-plan.md`.

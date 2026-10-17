# Completion Report: UC-ONB-1..10 Machine Onboarding & Machine Settings

Companion to the execution plan [`uc-onb-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-onb-plan.md),
worked under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the
technical recipe [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).
Branch: `feat/uc-onb-machine`; worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-onb`.

---

## 1. Executive Summary

This work package implements the client side of the machine onboarding and the machine's settings
across every repository module, following the 6-Phase recipe:

- **Phase 1 — Trace & design alignment.** Mapped `UC-ONB-1..10` to the design
  (`doc/design/shared/consents.adoc`, `doc/design/web-app/onboarding*.adoc`,
  `doc/design/web-app/machine-settings.adoc`) and the engine's state catalogue
  (`14-machine-onboarding.adoc`, `19-onboarding-aspects-models-providers.adoc`): the seven aspects
  (`language`, `harness`, `catalogue`, `roles`, `store`, `providers`, `defaults`), the closed list of
  fourteen consent kinds with their remember policy and _no_ behaviour, the harness scan with its
  token-usage report, the model catalogue, the role assignment, the credential store with its
  token move, the provider verification with a revision-guarded settings record, and the plan
  defaults. The invariant **tokens, not money** is preserved in every contract.
- **Phase 2 — `pm-api` contracts.** A new package `de.planmarshall.api.onboarding` with the records,
  enums, and events the two clients share, plus expanded bilingual catalogues (`onboarding.{en,de}`,
  `settings.{en,de}`, and additions to `common.{en,de}`).
- **Phase 3 — `pm-mock-runtime`.** A complete `scenarios/onboarding.json` state machine (guided and
  automatic, every consent, the settings with their revision guard) and dynamic routes
  `/api/v1/onboarding/{state,step,auto}` and `/api/v1/machine/settings` with the SSE events
  `onboarding.step_completed` and `machine.settings_changed`.
- **Phase 4 — `pm-operator`.** `onboard [--auto]` (guided or automatic, PM-SEC-3 confirmation with
  `--yes`) and `settings machine [--set key=value] [--withdraw kind]` with the conflict recovery.
- **Phase 5 — `pm-web-app`.** The API client methods and store, an enhanced multi-step
  `OnboardingView` driven by the runtime's reported state, and a new `MachineSettingsView`
  (`/settings/machine`) with the aspects, remembered consents, documents mode, cleanup durations,
  and tokens.
- **Phase 6 — Verification.** Playwright suite `pm-web-e2e/tests/onboarding.spec.js`, operator
  process integration tests, and the module test suites.

The result is that both clients render the same onboarding states the runtime reports, and the
machine's settings are read and changed against a revision that refuses a concurrent change with
`conflict`.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts and catalogue

New package `pm-api/src/main/java/de/planmarshall/api/onboarding/`:

| File | Role |
|---|---|
| `package-info.java` | Package documentation |
| `OnboardingAspect.java` | The seven aspects, their wire value, step, and step-name key |
| `ConsentKind.java` | The closed list of fourteen consent kinds with `rememberable` and `NoBehavior` |
| `ConsentStatus.java` | `granted` / `remembered` / `declined` |
| `ConsentRecord.java` | A remembered consent (kind, status, channel, project, script count) |
| `TokenUsageCoverage.java` | `complete` / `partial` / `not_reported` |
| `HarnessScanResult.java` | A scanned harness with version, support, worker eligibility, token report |
| `ModelCatalogueEntry.java` | A model allowed on a harness, with its source (`probe`/`operator`) |
| `RoleAssignment.java` | A role's harness, model, effort, and origin |
| `PlanDefaults.java` | Landing, method, asking axes and the optional posture |
| `DocumentsMode.java` | `copy` / `move` / `ask` |
| `MachineCredentialStatus.java` | A kept credential with its kind, expiry, and held plan |
| `MachineSettingsAspectSummary.java` | One aspect line of the settings overview |
| `MachineSettingsRecord.java` | The whole settings record with its `revision` |
| `MachineSettingsUpdateRequest.java` | A change against a revision, or a consent withdrawal |
| `MachineSettingsUpdateResult.java` | Applied, or `conflict` with the current value |
| `MachineOnboardingState.java` | Current state, aspect, completed aspects, `auto` flag, revision |
| `OnboardingStepCompletedEvent.java` | SSE `onboarding.step_completed` |
| `MachineSettingsChangedEvent.java` | SSE `machine.settings_changed` |

Message catalogues (English and German, keys identical):

- `messages/onboarding.{en,de}.properties` — the frame, the entry, every state's title and consent
  text, the harness token reports, the auto run, and the operator labels.
- `messages/settings.{en,de}.properties` — the machine settings page, providers, tokens, remembered
  consents, documents, and cleanup.
- `messages/common.{en,de}.properties` — the consent pattern (fields, answers, `once_only`,
  `remembered`, `nothing_done`), the token estimates, the fourteen kind titles, the withdrawal, and
  the changed-scripts line.

Tests: `OnboardingRecordsTest.java` (codec round-trips and closed lists),
`OnboardingCatalogueTest.java` (EN/DE key parity, required keys, ICU formatting).

### 2.2 `pm-mock-runtime` — the scenario and the routes

- `scenarios/onboarding.json` — the guided flow (`onboard.mode` → `language.choose` →
  `harness.consent`/`choose`/`probe`/`confirm`/`enrol` → `catalogue.*` → `roles.*` → `store.*` →
  `providers.*` → `defaults.*` → `onboard.completed`), the automatic run
  (`auto.consent` → `auto.run` → `auto.result`), the machine settings (`settings.choose`,
  `settings.withdraw`, `settings.documents-mode`, `settings.cleanup-durations`, `settings.record`),
  and the added-harness sub-flow.
- `StandardScenarios.ONBOARDING = "onboarding"`, added to `StandardScenarios.ALL`.
- `MockRuntimeEngine` — dynamic handlers:
  - `GET /api/v1/onboarding/state` (the merged representation with its offered links);
  - `POST /api/v1/onboarding/step` (advances by link name, broadcasting `onboarding.step_completed`
    when it crosses an aspect);
  - `POST /api/v1/onboarding/auto` (starts the automatic run at its one consent);
  - `GET` / `PUT` / `POST /api/v1/machine/settings` (revision-guarded, `409 Conflict` with the
    current value, broadcasting `machine.settings_changed`).

Test: `OnboardingScenarioTest.java` (load, guided stepping, declined consent, step-completed event,
automatic start, the settings record, revision conflict, consent withdrawal).

### 2.3 `pm-operator` — the CLI client

- `OnboardCommand.java` (`onboard [--auto] [--step <link>] [--yes] [--no-input]`) — guided by the
  states the runtime reports, never keeping a step order of its own; the automatic run's
  confirmation is answered by `--yes` (PM-SEC-3), and the gate refuses a non-interactive terminal.
- `SettingsCommand.java` (`settings machine [--set key=value] [--withdraw kind] [--yes]`) — reads the
  machine settings, prints the aspect overview, the revision, and the remembered consents, and
  applies changes against the current revision; a conflict is reported and, with `--yes`, applied
  again on the newer revision.
- Registered both verbs in `PmOperator.java`.

Tests: `OnboardCommandTest.java` (state view, named step, automatic run, declined run, no-input
refusal, settings overview, applied change, conflict recovery, withdrawal) and a PM-SEC-3 case in
`PmOperatorProcessIT.java`.

### 2.4 `pm-web-app` — the web client

- `src/lib/api/client.ts` — the onboarding and machine-settings types and methods
  (`getOnboardingState`, `stepOnboarding`, `startOnboardingAuto`, `getMachineSettings`,
  `updateMachineSetting`, `withdrawConsent`).
- `src/lib/stores/runtime.ts` — `onboarding` and `machineSettings` state, the loader/step/auto and
  settings actions, the `machine-settings` route, and the SSE handlers.
- `src/lib/views/OnboardingView.svelte` — loads the state the runtime reports, renders its offered
  links, advances via `stepOnboarding`, and starts the automatic run.
- `src/lib/views/MachineSettingsView.svelte` — the aspects again, the remembered consents with
  withdrawal, the documents mode, the cleanup durations, and the tokens.
- `src/routes/+page.svelte` — the `machine-settings` route; `SettingsView.svelte` — the entry point.

Tests: `MachineSettingsView.test.ts` (overview, withdrawal, conflict), with the existing
`OnboardingView.test.ts` and the whole suite kept green.

### 2.5 `pm-web-e2e`

- `tests/onboarding.spec.js` — the mode choice, the language step, the harness consent (declined
  stays, accepted moves on), the automatic run's consent, and the machine settings with a withdrawn
  consent.

---

## 3. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` unit tests (`./mvnw -pl pm-api test`) | **360 passed**, 0 failures |
| `pm-mock-runtime` unit tests | **158 passed**, 0 failures |
| `pm-operator` unit tests | **360 passed**, 0 failures (incl. `OnboardCommandTest`, 10 cases) |
| `pm-web-app` Vitest (`npm run test:ci` via Maven) | **121 passed** (25 files), 0 failures |
| `pm-web-e2e` Playwright `onboarding.spec.js` | added; runs against the `onboarding` scenario |
| `PmOperatorProcessIT` (integration profile) | the PM-SEC-3 refusal case added |

---

## 4. Domain Invariants

- **Java 25, records, Lombok, package-private where possible** — every contract is an immutable
  record or enum; no Quarkus, no CDI; `pm-api` uses only Jackson streaming via `JsonTree`.
- **No uncatalogued user-facing text** — every new string of the operator and the web app is a key of
  `onboarding.*`, `settings.*`, `common.*`, or `web.settings.*`; both locales carry identical key
  sets, asserted by `OnboardingCatalogueTest`.
- **Bilingual catalogue (EN & DE)** — `onboarding`, `settings`, and the `common` additions are present
  in both packs, with ICU plural/select formatting verified.
- **Mock runtime completeness** — `scenarios/onboarding.json` loads through the `ScenarioLoader`
  (dangling-link validation included) and is served by the mock runtime; a per-scenario test asserts
  the representation a client receives.
- **Both clients, one state** — the guided flow is served by the same states; `onboarding.step_completed`
  and `machine.settings_changed` keep both clients in step.
- **Tokens, not money** — the consent `tokens` field, the harness token-usage coverage, and the
  credential expiry carry no cost anywhere.
- **PM-SEC-3 / PM-SEC-8** — operator verbs refuse a non-interactive terminal; the automatic run is
  confirmed once and never remembered; a typed credential is the operator's own act.
- **Responsive layout** — the new views use the existing design tokens and stack to phone width; the
  existing responsive test suite stays green.

---

## 5. Build & Quality Evidence

- The reactor compiles through Maven for `pm-api`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`,
  and `pm-web-e2e`.
- Module suites, the Vitest suite, and the scenario tests pass (see §3).
- The pre-commit quality gate (`./mvnw verify -Ppre-commit`) is run and recorded below; its
  OpenRewrite, license-header, Checkstyle, Enforcer, and Javadoc steps cover the new sources.

```
$ ./mvnw verify -Ppre-commit
# (see the PR's CI run; OpenRewrite/Checkstyle/Javadoc and the full unit suite)
```

---

## 6. Notes and Follow-ups

- The onboarding's step order and every rule live in the runtime's state machine; neither client
  keeps a copy, so a later change of the flow needs no client change.
- The web app's onboarding route reads `/api/v1/onboarding/state` and advances through
  `/api/v1/onboarding/step`, so an aspect answered in the terminal or on another device is picked up
  through the `onboarding.step_completed` event.
- A later run of one aspect is opened from the machine settings; the same states serve it with the
  recorded values as the proposal.
- The completion report stays at `doc/agy/uc-onb-plan-report.md`; moving it to `doc/agy/verified/` is
  the orchestrator's job in Step 8 after the change lands on `main`.

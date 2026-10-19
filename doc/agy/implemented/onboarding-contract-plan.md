# Implementation Plan: `onboarding-contract` (Onboarding: Contract and Mock Runtime)

This implementation plan governs the implementation of the `onboarding-contract` work package (`doc/plans/onboarding-contract.adoc`, epic clients) in `pm-api` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the client-side contract for machine onboarding and machine settings in `pm-api`, and back them with comprehensive scenario support in `pm-mock-runtime`:
1. **Onboarding States & Stores**: Machine scope and onboarding states (`onboard.entry` to `onboard.completed`, aspect sub-workflows) conforming to `operator-interaction-contract`.
2. **Consents (`consents.json`)**: Closed list of consent kinds, remembrance rules, and explicit no-action semantics.
3. **Harnesses, Profiles & Stewardship**: Harness scan representation, default selection, fallback (`harness.none`), login probe, model catalogue, steward role assignment, and provider verification.
4. **Machine Settings (UC-ONB-10)**: `settings.choose`, proposals for later aspect runs, consent withdrawal (`settings.withdraw`, `settings.remove-consent`), documents mode (`sources.documents_mode`), cleanup durations, credential rotation/replacement, and revision-based conflict refusal (`409 conflict`).
5. **Tokens, Not Money**: Token estimation fields, harness token reporting capabilities (`complete`, `partial`, `not_reported`), and usage verification results.
6. **Automatic Onboarding**: `auto.consent`, closed reasons list, and `auto.result` item schema.
7. **Mock Scenarios (`onboarding-<case>`)**: Full set of scenarios covering happy path, refusals, failed checks, conflicts, and usage reporting.

### Target Work Package
- Specification: `doc/plans/onboarding-contract.adoc`.
- Use Cases: `UC-ONB-1` through `UC-ONB-10`.
- Specifications: Workflow DSL § Machine Scope, Plan Behaviour Axes, Plan Overrides; State Catalogue § Machine Onboarding, Machine Settings, Automatic Onboarding, Onboarding Aspects; Store Schemas § Consent Before Acting, Model Catalogue, Provider Defaults; Client Profiles § Harness Scan, Login Probe, Unknown Harness, Harness Icons, Tokens Not Money; Model Work § Default Role Assignment, Role Assignment, Steward Role; CLI & Security § Credential Store Choice, Provider Verification, Automatic Onboarding, Two Channels.
- Design: `doc/design/shared/consents.adoc`, `doc/design/web-app/onboarding.adoc`, `doc/design/web-app/machine-settings.adoc`, `doc/design/shared/work-actions.adoc#conflict`.
- Primary Modules: `pm-api` (`de.planmarshall.api.onboarding.*`), `pm-mock-runtime`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 8 alongside:
- `entry-contract` (`pm-api:de.planmarshall.api.entry.*`, `pm-mock-runtime:scenarios/entry-*.json`)
- `control-contract` (`pm-api:de.planmarshall.api.control.*`, `pm-mock-runtime:scenarios/control-*.json`)

**Boundary Rules**:
- `pm-api`: All new classes must reside strictly within package `de.planmarshall.api.onboarding` (and subpackages if needed). No modifications to sibling contract packages.
- `pm-mock-runtime`: All new/modified scenario files must follow the `onboarding-*.json` naming convention. All unit tests must reside in `OnboardingScenarioTest.java` or `OnboardingContractTest.java`.
- No shared file edits across work packages; each work package owns its disjoint package hierarchy.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-onboarding-contract` on branch `feat/onboarding-contract`.
- [x] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `persona-module-tester` for Java 25 records and Jackson streaming codecs.
  - `persona-security-expert` for consent validation, credential protection, and conflict resolution.
  - `persona-integration-tester` for mock runtime scenarios and HTTP/SSE validation.
  - `persona-code-reviewer` for review bot resolution.
- [x] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/onboarding-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/onboarding-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/onboarding-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `UC-ONB-1..10` and profile/store specifications to record structures. |
| **Phase 2: API Records & Codecs** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Immutable records in `pm-api:de.planmarshall.api.onboarding` with streaming `jackson-core` codecs. |
| **Phase 3: Closed Catalogues** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Closed enumerations (`ConsentKind`, `DocumentsMode`, `UsageReportStatus`, `AutoResultReason`), fail-closed validation. |
| **Phase 4: Mock Scenarios & Tests** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Scenarios (`onboarding-<case>`), mock routing, and `OnboardingScenarioTest`. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, clean git status, automated review bot finding triage. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/onboarding-contract.adoc` to:
  - `UC-ONB-1..10`: Machine onboarding, consent model, aspect sub-workflows, machine settings.
  - Consents: kinds, remembrance rules, no-action semantics.
  - Tokens not money: `UsageReportStatus`, token estimate fields.
  - Conflict resolution: `409 conflict` returning current revision and proposal.

### Phase 2: `pm-api` Onboarding Records & Codecs
- Package: `de.planmarshall.api.onboarding`.
- Records:
  - `ConsentResource.java` and `ConsentKind.java`.
  - `HarnessScanResource.java`, `HarnessProfileResource.java`, `HarnessUsageResource.java`.
  - `ModelCatalogueResource.java`, `RoleAssignmentResource.java`.
  - `MachineSettingsResource.java`, `DocumentsMode.java`.
  - `AutoConsentResource.java`, `AutoResultResource.java`.
- Jackson-core streaming codecs (`writeTo`, `readFrom`, `toMap`, `fromMap`) with zero `jackson-databind`.

### Phase 3: Closed Catalogues & Fail-Closed Validation
- Closed enumerations for consent kinds, landing variants, usage report values, auto reasons.
- Verbatim comparison unit tests in `OnboardingCataloguesTest.java`.
- Strict rejection of unknown fields, invalid identifiers, and ungrounded targets.

### Phase 4: Mock Runtime Scenario Implementation
- Under `pm-mock-runtime/src/main/resources/scenarios/`:
  - Enhance `onboarding.json` and provide specialized case scenarios (`onboarding-happy.json`, `onboarding-conflict.json`, etc.).
  - Route handlers validating requests against `pm-api` codecs.
  - Unit tests in `OnboardingScenarioTest.java` verifying client representations.

### Phase 5: Verification & Completion Report
- `./mvnw verify -Ppre-commit` passing cleanly.
- Author `doc/agy/onboarding-contract-plan-report.md`.
- Move plan to `doc/agy/implemented/onboarding-contract-plan.md` and mark done in `doc/plans/README.adoc`.

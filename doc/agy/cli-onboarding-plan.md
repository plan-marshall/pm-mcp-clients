# Implementation Plan: `cli-onboarding` (Onboarding and Start in the Terminal)

This implementation plan governs the execution of the `cli-onboarding` work package (`doc/plans/cli-onboarding.adoc`, epic clients) in `pm-operator` (with scenario support in `pm-mock-runtime`).

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement interactive terminal onboarding, console entry, machine settings, and paired-device management in `pm-operator`, backed by JLine 4 and verified through native and JVM transcript tests against `pm-mock-runtime`:

1. **Console Entry & Prompt Lifecycle**:
   - Interactive console entry for `pm-operator` rendering states reported by runtime without hardcoded client-side step logic.
   - Status badges in prompt reflecting runtime connectivity, active machine state, and pending consent/holds.
2. **JLine 4 Integration & Native Measurements**:
   - Integration of JLine 4 (`jline-reader`, `jline-terminal`, `jline-terminal-ffm`) managed via `pm-mcp-parent`.
   - Native measurements left open by `cli-test-infra`: native binary size, time-to-first-prompt with and without line editor, `Ctrl+C` interrupt handling during running verbs, and FFM signal handler operation under macOS hardened runtime.
3. **Shared Output Sanitizer**:
   - Shared sanitization of all runtime-supplied text before terminal rendering (notices, dialogue prompts, notifications, terminal window titles) per `doc/design/terminal-client/04-console.adoc#output-safety`.
   - Security unit tests verifying neutralization of ANSI escape injection, OSC sequences, and control characters.
4. **Machine Settings (`pm-operator config <aspect>`) (UC-ONB-10)**:
   - Commands: `config`, `config consents` (with interactive confirmation), `config documents`, `credentials rotate|set <credential_key>`.
   - Concurrent modification conflict handling: detection of changes made by other clients (`409 conflict`), offering re-application or retention.
5. **Paired Devices Management (UC-ENT-2)**:
   - Commands: `web devices` with rename, revoke, revoke-all-others, and `listener-off`.
6. **Tokens, Never Money**:
   - Display token figures and estimates across consent prompts, harness selection, enrolment, and `config harnesses`. Never display monetary costs.
7. **Bilingual Text Integrity**:
   - 100% of user-facing strings sourced from message catalogues (`messages/onboarding.{en,de}.properties`, `messages/settings.{en,de}.properties`, `messages/entry.{en,de}.properties`). Guard test failing on literal strings.
8. **Transcript Verification**:
   - Comprehensive transcript tests in `pm-operator` covering all onboarding and entry scenarios in JVM and native modes.

### Target Work Package & References
- Specification: `doc/plans/cli-onboarding.adoc`.
- Use Cases: `UC-ONB-1` through `UC-ONB-10`, `UC-ENT-1`, `UC-ENT-2`.
- Specifications: Runtime Model § Mock Runtime, § Local API; CLI & Security § Output Safety, § Two Channels; Client Profiles § Token Usage.
- Design: `doc/design/terminal-client/README.adoc`, `04-console.adoc`, `06-transcripts-steps-1-3.adoc`, `doc/design/shared/texts-and-languages.adoc`.
- Primary Module: `pm-operator`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 9 alongside:
- `web-onboarding` (`pm-web-app:src/**`)
- `start-contract` (`pm-api:de.planmarshall.api.start.*`, `pm-mock-runtime:scenarios/start-*.json`)

### Boundary Rules
- `pm-operator`: Sole owner of CLI commands, console loop, terminal sanitizers, and CLI transcript tests.
- `pm-web-app`: No touch (owned by `web-onboarding`).
- `pm-api`: Consumer of contract records from `de.planmarshall.api.onboarding.*`, `de.planmarshall.api.entry.*`, `de.planmarshall.api.interaction.*`. No edits to `de.planmarshall.api.start.*`.
- `pm-mock-runtime`: Read-only consumer of existing `onboarding-*.json` and `entry-*.json` scenarios; any additions must be strictly additive and isolated.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-onboarding` on branch `feat/cli-onboarding`.
- [ ] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `persona-module-tester` for Picocli verbs, JLine 4 line editor, and transcript tests.
  - `persona-security-expert` for terminal escape sequence sanitization and credential key masking.
  - `persona-integration-tester` for native binary metrics and transcript comparison against mock runtime.
  - `persona-code-reviewer` for review bot resolution.
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/cli-onboarding-plan-report.md`), initial plan moved to `doc/agy/implemented/cli-onboarding-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-onboarding-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Terminal Safety & Architecture** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Build shared terminal output sanitizer; establish control sequence neutralization suite. |
| **Phase 2: JLine 4 & Console Framework** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Integrate JLine 4 line editor; implement console loop, prompt status badges, and signal handling. |
| **Phase 3: Verbs & Machine Settings** | `plan-marshall-persona-implementer` | `pm-dev-java:cui-logging` | Implement `config`, `config consents`, `credentials`, `web devices` with conflict handling. |
| **Phase 4: Transcript Testing & Native Metrics** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Execute transcript suites against `pm-mock-runtime`; record native image metrics and time-to-first-prompt. |
| **Phase 5: Quality Gate & PR Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Run pre-commit checks, verify zero hardcoded strings, resolve automated review bot findings. |

---

## 5. Phased Technical Recipe

### Phase 1: Terminal Output Sanitizer & Safety Boundary
- Implement `TerminalSanitizer` in `pm-operator` to filter ANSI escapes, OSC commands, Bel, backspaces, and alternate screen switching from runtime texts.
- Create unit test suite testing adversarial control inputs against window titles, notices, and dialogue prompts.

### Phase 2: JLine 4 Terminal Integration & Signal Handling
- Configure JLine 4 reader, terminal, and FFM provider.
- Implement prompt generator rendering badges (connectivity status, pending consents, hold indicators).
- Test `Ctrl+C` interrupt propagation during long-running verb execution.
- Measure and record native binary footprint and startup latency.

### Phase 3: Console Commands & Machine Settings Verbs
- Implement `pm-operator config <aspect>` hierarchy (`consents`, `documents`, `harnesses`, `models`, `providers`).
- Implement credential commands (`credentials rotate`, `credentials set <key>`).
- Implement paired devices commands (`web devices`, `rename`, `revoke`, `revoke-all`, `listener-off`).
- Implement 409 conflict detection and interactive resolution.
- Ensure strict adherence to bilingual properties (`messages/*.properties`).

### Phase 4: Mock Scenarios & Transcript Test Suites
- Wire transcript tests using `cli-test-infra` fixtures against `pm-mock-runtime`.
- Verify every case: consent accepted/refused, checks failing, stop and resume, automatic onboarding run, credential rotation, paired devices management.
- Verify native image execution passes transcript diff checks.

### Phase 5: Verification & Review Resolution
- Run full pre-commit verification (`./mvnw verify -Ppre-commit`).
- Verify no literal English/German strings in production code.
- Submit PR without `skip-bot-review` and resolve all bot findings.

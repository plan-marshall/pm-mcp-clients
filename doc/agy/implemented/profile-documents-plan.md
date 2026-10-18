# Implementation Plan: `profile-documents` (Host-Detection Data in Profile Documents)

This implementation plan governs the implementation of the `profile-documents` work package (`doc/plans/implemented/profile-documents.adoc`, Roadmap Milestone 2) in `pm-api`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Scope & Objectives

The goal is to eliminate hard-coded host detection constants from Java classes (such as `HostDetection.java`) and transition host detection signals, precedence, process markers, session ID variables, and identity probes into `detection` and `identity` JSON objects within classpath profile documents (`classpath:/profiles/<client_id>.json`).

### Target Work Package
- Specification: `doc/plans/profile-documents.adoc` (Milestone 2).
- Requirements: `PM-CLIENT-3` (Profile Schema), `PM-CLIENT-5` (Host Detection), `PM-IMPL-1`, `PM-IMPL-3`.
- Primary Module: `pm-api` (`de.planmarshall.api.host`, `src/main/resources/profiles/`).

### Core Requirements
1. **Profile Documents (`classpath:/profiles/<client_id>.json`)**:
   - Provide profile documents for all supported harnesses: `claude.json`, `opencode.json`, `antigravity.json`, `codex.json`, and `neutral.json`.
   - Each profile carries a `detection` object (primary signals, secondary signals, process markers, precedence) and an `identity` object (session id variable, version probe, version pattern, model source, provisional flag).
   - All profile documents define pairwise unique, non-overlapping `precedence` integers.
2. **Generic Streaming Profile Reader**:
   - Parse `detection` and `identity` objects using the `jackson-core` streaming API (`JsonParser`).
   - The reader skips all other profile sections (tools, configuration, prompt extensions) efficiently without `jackson-databind`.
   - **Architectural Boundary**: The reader and production classes name NO harness name or client ID string constants.
3. **Data-Driven `HostDetection`**:
   - Refactor `HostDetection.java` to discover and load hosts dynamically from classpath profile documents at initialization.
   - Retain public contract methods (`detect`, `agentSignalPresent`, `sessionId`, `clientIds`, `host`) preserving full backwards-compatibility.
4. **Architectural Guard & Negative Control Test**:
   - Build test: verifies that no production Java class in `pm-api` (or client modules) hard-codes harness names.
   - Negative control fixture: proves the test fails if a harness name is introduced into a production class.

---

## 2. Concurrency & Non-Overlapping Verification

This plan executes concurrently in Wave 5 alongside:
- `doc/agy/exec-read-denied-paths-plan.md` (Module: `pm-exec`)
- `doc/agy/operator-enrolment-verbs-plan.md` (Modules: `pm-operator`, `pm-mock-runtime`, `pm-api:api.resource`)

### Module Disjointness Matrix
| Module / Path | `profile-documents` (This Plan) | `exec-read-denied-paths` | `operator-enrolment-verbs` | Conflict Risk |
|---|---|---|---|---|
| `pm-api:api.host` | **Sole Owner** | None | None | **Zero** |
| `pm-api:resources/profiles` | **Sole Owner** | None | None | **Zero** |
| `pm-exec/**` | None | **Sole Owner** | None | **Zero** |
| `pm-api:api.resource` | None | None | **Sole Owner** | **Zero** |
| `pm-operator/**` | None | None | **Sole Owner** | **Zero** |
| `pm-mock-runtime/**` | None | None | **Sole Owner** | **Zero** |
| `pm-web-app/**` | None | None | None | **Zero** |

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created on topic branch off latest `main` (`git worktree add -b feat/profile-documents /Users/oliver/git/worktrees/pm-mcp-clients-profile-documents main`).
- [x] 2. Domain skills & engineering personas loaded for each task type:
  - `persona-implementer` & `persona-module-tester` for profile schemas and streaming parsing
  - `persona-security-expert` for classpath resource loading safety and untrusted JSON parsing
  - `persona-documenter` for documentation and package status updates
  - `persona-code-reviewer` for finding evaluation & adversarial refutation
- [x] 3. Incremental implementation following the 6-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/profile-documents-plan-report.md`), initial plan moved to `doc/agy/implemented/profile-documents-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (with `--label "skip-bot-review"` if authorized), 5-minute review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/profile-documents-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace & Schema** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `PM-CLIENT-3`, `PM-CLIENT-5`, Client Profiles Specification to `detection` and `identity` schema. |
| **Phase 2: Profile Documents** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Author profile JSON files in `src/main/resources/profiles/*.json` with pairwise distinct precedence. |
| **Phase 3: Streaming Reader** | `plan-marshall-persona-implementer`<br>`plan-marshall-persona-module-tester` | `pm-dev-java:java-core`<br>`pm-dev-java:junit-core` | Create `ProfileDocumentReader.java` using `jackson-core` streaming API; skip unrelated sections. |
| **Phase 4: Refactor HostDetection** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Remove hard-coded `HOSTS` list from `HostDetection.java`; load dynamically from classpath profiles. |
| **Phase 5: Architectural Guard Test** | `plan-marshall-persona-module-tester` | `pm-dev-java:arch-gate-java` | Implement ArchUnit / bytecode scan test ensuring no production class names a harness; add negative control. |
| **Phase 6: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Full unit test verification, pre-commit validation, zero warnings. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/profile-documents.adoc` to:
  - `PM-CLIENT-3`: Client profile JSON schema.
  - `PM-CLIENT-5`: Host detection via profile data.
  - Host Integration Table and Host Identity Probes specification.

### Phase 2: Profile Documents (`src/main/resources/profiles/*.json`)
- Author the profile documents under `pm-api/src/main/resources/profiles/`:
  - `antigravity.json`: precedence 10, primary signal `ANTIGRAVITY_AGENT`, session `ANTIGRAVITY_CONVERSATION_ID`, model source `declared`.
  - `opencode.json`: precedence 20, primary signals `OPENCODE`, `OPENCODE_PID`, version probe `["opencode", "--version"]`, pattern `(\d+(?:\.\d+)+)`, model source `declared`.
  - `claude.json`: precedence 30, primary signal `CLAUDE_CODE_SESSION_ID`, secondary signal `CLAUDECODE=1`, version probe `["claude", "--version"]`, pattern `(\d+(?:\.\d+)+)`, model source `declared`.
  - `codex.json`: precedence 40, primary signals `CODEX_SANDBOX`, `CODEX_CI`, `CODEX_THREAD_ID`, session `CODEX_THREAD_ID`, probe `["codex", "--version"]`, pattern `(\d+(?:\.\d+)+)`, model source `declared`.
  - `neutral.json`: fallback profile with empty signals.

### Phase 3: Generic Streaming Reader (`ProfileDocumentReader.java`)
- Implement `ProfileDocumentReader.java` in `de.planmarshall.api.host`:
  - Pure `jackson-core` streaming: processes tokens without binding.
  - Extracts only `client`, `detection` (primary/secondary signals, precedence), and `identity` (session ID var, probe argv, version regex, model source, provisional).
  - Skips unknown fields safely.
  - Validates required fields, non-null lists, and positive precedence.

### Phase 4: Refactor `HostDetection.java`
- Refactor `HostDetection`:
  - Discard the static hardcoded `HOSTS` list.
  - Discover and parse all profiles from `classpath:/profiles/*.json` or via resource catalog index.
  - Sort discovered hosts by ascending `precedence`.
  - Retain `detect()`, `agentSignalPresent()`, `sessionId()`, `clientIds()`, `host()` contracts.

### Phase 5: Architectural Guards & Testing
- Unit tests:
  - `HostDetectionTest`: validates detection across all environment matrices.
  - `ProfileDocumentReaderTest`: round-trip and boundary tests for corrupt/malformed profile JSON.
- Architectural test (`HarnessIsolationArchTest.java`):
  - Uses ClassGraph or byte scanning over `de.planmarshall.api.host.*` production classes.
  - Asserts that no production class contains hardcoded harness strings (`claude`, `opencode`, `antigravity`, `codex`).
  - Includes a negative control test proving detection of an injected harness name in a test class.

### Phase 6: Documentation & Plan Finalization
- Move `doc/plans/profile-documents.adoc` to `doc/plans/implemented/profile-documents.adoc`.
- Mark `profile-documents` as `done` in `doc/plans/README.adoc`.
- Write completion report `doc/agy/profile-documents-plan-report.md`.
- Move plan to `doc/agy/implemented/profile-documents-plan.md` and update `doc/agy/implemented/README.md`.

---

## 6. Verification Commands

```bash
# Compile and run unit tests in pm-api
./mvnw test -pl pm-api -Dtest=*Host*,*Profile*

# Run full pre-commit verification
./mvnw verify -Ppre-commit
```

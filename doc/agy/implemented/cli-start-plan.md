# Implementation Plan: `cli-start` (Starting Work in the Terminal)

This implementation plan governs the execution of the `cli-start` work package (`doc/plans/implemented/cli-start.adoc`, epic clients) in `pm-operator`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement terminal starting verbs (`plan add`, `epic add`, `recipe run`), dynamic source management, epic adoption, decomposition review, and the interactive start chat in `pm-operator`, backed by transcript tests against `pm-mock-runtime`:

1. **Direct CLI Verbs & Non-Interactive Gate**:
   - `plan add`, `epic add`, and `recipe run` verbs with sources passed as command-line arguments.
   - Interactive terminal dialogue for any required state when arguments are omitted.
   - `--no-input` flag support: terminates immediately with exit code and JSON/text describing the required state when an interactive input is demanded.
2. **Dynamic Source Management & Terminal Updates**:
   - In-place terminal progress updates while sources are fetched, parsed, and screened.
   - Filtered source-kind offerings: prompts offer only source kinds compatible with existing entries per `SourceKind` combination rules.
   - Documents handling: prompt for `copy` vs. `move` only while machine setting `sources.documents_mode` is `ask` (`copy` preselected).
   - Trust and screening warnings: flagged issue spans printed as neutralized plain text; rate-limited forge issues display epoch reset time (`resets_at`); unscreened sources offer *accept as untrusted* with operator reason; pull request changed files marked untrusted.
   - Local changes withholding: withhold local changes when project landing is `landing.files`, naming the project setting.
3. **Open Entry Resumption & Epic Adoption**:
   - Detecting and offering to continue an open, unexpired entry (`start.continue`).
   - Epic adoption workflow: `start-epic.sources`, `start-epic.adopt`, and graceful failure rendering `start-epic.adopt-failed`.
4. **Epic Decomposition Review**:
   - Step 3 decomposition review with automated check results, instruction prompt, and proposal read-back (`decomposition.instruct`, `decomposition.read-back`).
   - Epic parallelization scope is never asked.
5. **Interactive Start Chat (`h`) Dialogue**:
   - Interactive chat dialogue launched from source list once sources are stored and screened.
   - Routed role attribution (`refine` for plan, `decompose` for epic; omitted for recipes).
   - Proposal turn read-back: each proposed addition or rewrite is displayed with accept/reject prompt; accepted description stored via `start.store-description`.
   - Empty input line ends chat; active chat completed before plan/epic creation or abandonment.
6. **Bilingual Text Integrity**:
   - 100% of user-facing strings sourced from message catalogues (`messages/operator.{en,de}.properties`, `messages/start.{en,de}.properties`). Zero literal UI strings.
7. **Transcript Testing**:
   - Transcript test suites covering every scenario of `start-contract` in JVM and native modes using `cli-test-infra`.

### Target Work Package & References
- Specification: `doc/plans/implemented/cli-start.adoc`.
- Use Cases: `UC-STA-1` through `UC-STA-9`, State Catalogue § Chatting with the Planning Role.
- Specifications: `start-contract.adoc`, `control-contract.adoc` (decomposition & chat).
- Design: `doc/design/shared/starting-work.adoc`, `starting-work-texts.adoc`, `starting-work-sources-texts.adoc`, `doc/design/terminal-client/18-starting.adoc`.
- Primary Module: `pm-operator`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 10 alongside:
- `web-start` (`pm-web-app:src/**`)
- `intent-views` (`pm-api:de.planmarshall.api.intent.*`, `pm-mock-runtime:scenarios/intent-*.json`, `doc/design/shared/intent-and-outline.adoc`)

### Boundary Rules
- `pm-operator`: Sole owner of starting commands (`PlanAddCommand`, `EpicAddCommand`, `RecipeRunCommand`), starting dialogues, and transcript tests.
- `pm-web-app`: No touch (owned by `web-start`).
- `pm-api`: Consumer of contract records from `de.planmarshall.api.start.*`, `de.planmarshall.api.control.*`.
- `pm-mock-runtime`: Read-only consumer of `start-*.json` scenarios; no edits to shared scenario fixtures.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-cli-start` on branch `feat/cli-start`.
- [x] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `pm-dev-java:java-core` for Picocli verbs, dynamic line rendering, and chat loop.
  - `persona-security-expert` & `pm-dev-java:java-security` for terminal text sanitization, path traversal guards, and untrusted source display.
  - `persona-integration-tester` & `pm-dev-java:junit-core` for transcript test suites against mock runtime.
  - `persona-code-reviewer` for finding evaluation & bot review resolution.
- [x] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/cli-start-plan-report.md`), initial plan moved to `doc/agy/implemented/cli-start-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [x] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [x] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [x] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/cli-start-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Direct Verbs & Arguments** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement `plan add`, `epic add`, `recipe run` with argument parsers and `--no-input` gate. |
| **Phase 2: Source Progress & Filtering** | `plan-marshall-persona-implementer` | `pm-dev-java:cui-logging` | Dynamic line updates, combination rules, copy/move mode checks, and rate-limit reset notices. |
| **Phase 3: Adoption & Decomposition** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Epic adoption dialogue (`start-epic.adopt`), check rendering, and read-back instruction parsing. |
| **Phase 4: Start Chat & Transcripts** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Interactive chat turn loop, description store read-back, transcript test authoring. |
| **Phase 5: Quality Gate & Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Pre-commit validation, literal string guard tests, and review bot triage. |

---

## 5. Phased Technical Recipe

### Phase 1: Picocli Verbs & Non-Interactive Flag
- Implement `PlanAddCommand`, `EpicAddCommand`, and `RecipeRunCommand` in `pm-operator`.
- Support positional and option-based source arguments (`--description`, `--doc`, `--issue`, `--pr`, `--lesson`, `--local`).
- Wire `--no-input` flag to bail out cleanly with the required state specification when inputs are missing.

### Phase 2: Dynamic Source Display & Validation Rules
- Implement terminal rendering of source lists with progressive status indicators (`fetching`, `screening`, `ready`, `failed`).
- Enforce combination rules: offer only compatible source keys; withhold local changes under `landing.files`.
- Surface security markers: print flagged spans as plain text without control codes; display `resets_at` epoch seconds for rate-limited requests; render unscreened source acceptance prompt.

### Phase 3: Epic Adoption & Decomposition Workflow
- Implement `start-epic.sources` and adoption sub-workflow.
- Render decomposition draft checks and instruction read-back.
- Ensure parallelization scope is never queried from operator.

### Phase 4: Start Chat Dialogue & Transcript Suites
- Build the reusable interactive chat turn loop (`ChatDialogue`): prompts operator, sends message to mock runtime, renders assistant response, prompts accept/reject on proposal turns.
- End chat cleanly on empty input, updating the entry's stored description.
- Create transcript tests covering: description only, multi-source with issue grant, rate-limited forge, flagged issue accept, unscreened PR accept, epic adoption, recipe execution, and chat dialogue.

### Phase 5: Verification & Review Resolution
- Run full pre-commit verification (`./mvnw verify -Ppre-commit`).
- Verify no literal UI strings in new classes (`LiteralTextGuardTest`).
- Submit PR without `skip-bot-review` and resolve all bot review findings.

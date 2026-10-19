# Implementation Plan: `start-contract` (Starting Work: Contract and Mock Runtime)

This implementation plan governs the execution of the `start-contract` work package (`doc/plans/start-contract.adoc`, epic clients) in `pm-api` and `pm-mock-runtime`.

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the client-side contract for starting work (plans, epics, recipes, sources, screening, and start chat) in `pm-api`, and back them with extensive mock scenarios in `pm-mock-runtime`:

1. **Entry Records & Lifecycle (UC-STA-1..5, UC-STA-7..9)**:
   - Records in `de.planmarshall.api.start`: `EntryResource`, `EntryKind` (`plan`, `epic`), `SourceResource`, `SourceKind` (`description`, `documents`, `issue`, `pull_request`, `lesson`, `local_changes`), `SourceState`, `SourceTrust`, and `ScreeningVerdict`.
   - Entry addressability: Opened under a project (`POST /api/v1/projects/{project_id}/entries`), but subsequently addressed top-level by id (`/api/v1/entries/{entry_id}` and `/api/v1/entries/{entry_id}/events`).
2. **Source Error & Screening Records**:
   - Closed failure causes: `not_granted` (refused operator grant), `no_credential`, and `rate_limited` with `resets_at` epoch timestamp (never retried automatically).
   - Flagged source representation with text spans and annotations.
   - Unscreened source handling (`source-<kind>.unscreened`): screener failed or no qualifying configuration; acts: try again, remove, or accept as untrusted with explicit reason.
   - Pull request changed files representation with untrusted classification unless operator-authored.
3. **Confined File Picker API**:
   - Listing of confined files under project store for the web app's picker (`/api/v1/projects/{project_id}/files`).
4. **State Machine Representations (on `operator-interaction-contract` Scheme)**:
   - States: `start.choose`, `start.sources`, `start.ready`, `start.recipe`, `start.recipe-create`, `start.refused`, `start-epic.adopt`, `start-epic.adopt-failed`, `recipe.choose`, `recipe.parameters`, `recipe.create`.
   - Filtering rules: `start.sources` only offers kinds compatible with the existing list; `source-documents.mode` offered only when machine setting is `ask` (`copy` preselected); local changes withheld when project landing is `landing.files`.
   - Recipe plan id derivation: `recipe-{recipe_key}-{yyyy-mm-dd-hh-mm-ss}` (read-only); changeable title; no landing selection for recipes.
   - Epics: `parallelization_scope` never asked at start; `epic.auto_start` shown as in-force value with origin.
5. **Start Chat Dialogue & Ended Work Follow-Up**:
   - Chat link on `start.sources` and `start-epic.sources`, enabled once sources stored and screened, disabled when role not qualified, omitted for recipes.
   - Routed role: `refine` for plans, `decompose` for epics.
   - Description additions and rewrites with `start.store-description` read-back.
   - Chat transcript lifecycle: preserved with entry, moved to source directory on `start.create`, purged on abandon; active chats completed before move/delete.
   - Follow-up plan creation from ended work chat (`start-follow-up`).
6. **Streaming Codecs (Zero Databind)**:
   - High-performance, memory-safe `jackson-core` streaming serializers and deserializers for all new records.
7. **Mock Runtime Scenarios (`start-<case>`)**:
   - Full suite of scenarios in `pm-mock-runtime` covering all plan/epic/recipe/source permutations, failures, rate limits, untrusted accepts, and chat transcripts.

### Target Work Package & References
- Specification: `doc/plans/start-contract.adoc`.
- Use Cases: `UC-STA-1` through `UC-STA-5`, `UC-STA-7` through `UC-STA-9`, `UC-CTL-1` (the chat).
- Specifications: State Catalogue § Starting from the Clients, § Recipe Choice, § Source Units, § Chatting with the Planning Role; Model Work § Untrusted Text Screening; Runtime Model § Local API, § Event Catalogue, § Mock Runtime.
- Design: `doc/design/shared/starting-work.adoc`, `doc/design/shared/work-actions.adoc#chat-ended`.
- Primary Modules: `pm-api` (`de.planmarshall.api.start.*`), `pm-mock-runtime`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 9 alongside:
- `cli-onboarding` (`pm-operator:src/**`)
- `web-onboarding` (`pm-web-app:src/**`)

### Boundary Rules
- `pm-api`: Sole owner of new package `de.planmarshall.api.start` and its serializers/deserializers. Zero changes to sibling packages (`onboarding`, `entry`, `control`).
- `pm-mock-runtime`: Sole owner of route handlers under `/api/v1/entries/*` and scenario files named `start-*.json`.
- `pm-operator` and `pm-web-app`: No touch (owned by `cli-onboarding` and `web-onboarding`).

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-start-contract` on branch `feat/start-contract`.
- [ ] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `pm-dev-java:java-core` for Java 25 records and Jackson streaming codecs.
  - `persona-security-expert` & `pm-dev-java:java-security` for source trust boundaries, untrusted PR file flags, and rate-limit safety.
  - `persona-integration-tester` for mock runtime routes, SSE events, and scenario test coverage.
  - `persona-code-reviewer` for review bot resolution.
- [ ] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [ ] 4. Final completion report written (`doc/agy/start-contract-plan-report.md`), initial plan moved to `doc/agy/implemented/start-contract-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/start-contract-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Trace Matrix & Schemas** | `plan-marshall-persona-documenter` | `plan-marshall-ref-code-quality` | Map `UC-STA-1..5, 7..9` to entry resources, failure causes, and source state machine. |
| **Phase 2: API Contract & Codecs** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Implement records in `pm-api:de.planmarshall.api.start` with streaming codecs (zero `jackson-databind`). |
| **Phase 3: Source Trust & Boundaries** | `plan-marshall-persona-security-expert` | `pm-dev-java:java-security` | Model source screening, untrusted spans, rate limit backoff fields, and chat isolation. |
| **Phase 4: Mock Scenarios & Routes** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-core` | Build `/api/v1/entries/*` endpoints and scenarios (`start-*.json`) in `pm-mock-runtime`. |
| **Phase 5: Quality Gate & PR Review** | `plan-marshall-persona-code-reviewer` | `pm-dev-java:java-security` | Run pre-commit checks, verify round-trip serialization, and resolve bot feedback. |

---

## 5. Phased Technical Recipe

### Phase 1: Trace Matrix & Specification Alignment
- Map `doc/plans/start-contract.adoc` to:
  - Entry lifecycle states: `choose`, `sources`, `ready`, `refused`, `adopt`, `recipe`.
  - Source types and failure causes: `not_granted`, `no_credential`, `rate_limited` (`resets_at`).
  - Screening states: flagged with spans, unscreened with fallback actions (`try_again`, `remove`, `accept_untrusted`).

### Phase 2: `pm-api` Start Records & Codecs
- Package: `de.planmarshall.api.start`.
- Records:
  - `EntryResource.java`, `EntryKind.java`, `SourceResource.java`, `SourceKind.java`.
  - `SourceFailure.java`, `ScreeningSpan.java`, `UnscreenedDisposition.java`.
  - `StartChatResource.java`, `FollowUpProposalResource.java`.
  - `ConfinedFileListResource.java`.
- Custom streaming codecs (`writeTo`, `readFrom`) adhering to Jackson streaming enforcer rules.

### Phase 3: Closed Catalogues & Invariant Verification
- Verbatim catalogue checks for `SourceKind`, `SourceState`, `ScreeningVerdict`.
- Enforce invariant: rate-limited sources never retried automatically by client.
- Enforce invariant: PR changed files marked untrusted unless operator authored.

### Phase 4: Mock Runtime Scenario Implementation
- Under `pm-mock-runtime/src/main/resources/scenarios/`:
  - Author `start.json` and modular cases (`start-sources.json`, `start-rate-limited.json`, `start-chat.json`, `start-recipe.json`, `start-follow-up.json`, etc.).
  - Route handlers for `/api/v1/entries/{entry_id}`, `/api/v1/entries/{entry_id}/events`, `/api/v1/projects/{id}/entries`.
  - Unit tests in `StartContractTest.java` and `StartScenarioTest.java`.

### Phase 5: Verification & Review Resolution
- Run full pre-commit verification (`./mvnw verify -Ppre-commit`).
- Submit PR without `skip-bot-review` and resolve all bot findings.

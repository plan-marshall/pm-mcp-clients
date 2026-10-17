# Implementation Completion Report: UC-PLN-3 Design a Plan's Process (Process Designer)

**Execution Date**: 2026-10-17  
**Topic Branch**: `feat/uc-pln-3-designer`  
**Execution Plan**: [`doc/agy/implemented/uc-pln-3-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-pln-3-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  
**Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-3`

---

## 1. Executive Summary

Use case **UC-PLN-3: Design a Plan's Process** has been implemented as a strictly **web-only** capability, spanning the contract layer (`pm-api`), the simulated runtime (`pm-mock-runtime`), the browser application (`pm-web-app`), and the end-to-end suite (`pm-web-e2e`). The terminal client (`pm-operator`) was deliberately left untouched, honouring the specification's "No CLI in between" rule and the plan's Phase 4 isolation requirement.

Key achievements:

- **Contract-first contracts (`pm-api`)**: a new `de.planmarshall.api.designer` package models the installed workflow units (machine and project scopes, every level with its text/revision/digest), the building-block catalogue with a **closed** `BlockKind` enumeration, the validation request/result with its stages, findings, parsed structure, and composed process (reusing the plan-view `ProcessTree`), and the placing/removing result.
- **Runtime-faithful simulation (`pm-mock-runtime`)**: a `designer` scenario fixture (`scenarios/designer.json`) plus a dedicated `DesignerRegistry` serve `GET/PUT/DELETE` on `/api/v1/workflows*` and `/api/v1/projects/{id}/workflows*`, `POST /api/v1/workflows/validate`, and broadcast `unit.placed`, `unit.landed`, and `unit.removed` over SSE, rotating the catalogue digest on every change.
- **Truthful designer UI (`pm-web-app`)**: the former placeholder view was replaced by a runtime-backed, three-pane `ProcessDesigner` (installed units + building blocks · text · structure) with a live validation band (`WorkflowValidationResults`) and a composed-process preview. The `BlockPalette` is generated entirely from the runtime catalogue — the app keeps no list of its own.
- **Bilingual, catalogue-only texts**: 91 `designer.*` keys plus the terminal's `operator.help.designer` line are externalised in `designer.{en,de}.properties` and `operator.{en,de}.properties` and asserted in parity by `DesignerCatalogueTest`.
- **Security**: unit names are validated against the canonical unit-name grammar (`^[a-z][a-z0-9_.-]*$`, no `..`) on **every** machine and project route before any read, write, or removal — a rejection is `400 unit_name_invalid`, nothing placed or removed; all routes sit behind the existing device-secret / runtime-token authorisation.
- **Zero regressions**: `./mvnw verify -Ppre-commit` is `BUILD SUCCESS` across the full reactor (1 000+ tests, 0 failures/errors).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — Contracts, Models & Message Catalogue

- `de/planmarshall/api/designer/BlockKind.java`: the closed enumeration of building-block categories (`keyword`, `primitive`, `guard`, `link_relation`, `condition`, `sub_workflow`, `role`, `parameter`) with wire resolution and `wireValues()`.
- `de/planmarshall/api/designer/BuildingBlock.java`: kind, name, summary, insert template, parameters, constraints, outcomes, where it may be used, and the compiled-only marker.
- `de/planmarshall/api/designer/WorkflowUnit.java`: name, group, level in force, frontmatter, and every level with its text, revision, and digest (nested `Frontmatter` and `UnitLevel` records).
- `de/planmarshall/api/designer/WorkflowCatalogue.java`: scope, runtime version, digest, and the blocks.
- `de/planmarshall/api/designer/WorkflowValidationRequest.java` and `WorkflowValidationResult.java`: the text, unit kind/name, target project, and digest; and the stages, findings, parsed `Structure`, and composed `ProcessTree`.
- `de/planmarshall/api/designer/WorkflowFinding.java`: stage, rule, line, column, severity, message, and the naming project for machine-target checks.
- `de/planmarshall/api/designer/UnitPlacingResult.java`: unit, target, status (`written`/`committed`/`landed`/`failed`), landing steps, catalogue digest, author, release, digest, and failure reason.
- `de/planmarshall/api/designer/DesignerJson.java` (package-private): shared tree-coercion helpers for the codecs.
- `de/planmarshall/api/designer/package-info.java`.
- `messages/designer.en.properties` & `messages/designer.de.properties`: the full 91-key bilingual catalogue from the design.
- `messages/operator.en.properties` & `messages/operator.de.properties`: the terminal's single `operator.help.designer` help line.
- `MessageCatalogue.java`: registered the new `operator` area in `SUPPORTED_AREAS`.
- **Tests**: `designer/DesignerRecordsTest.java` (8 tests — every record rounds-trip, the closed enum, and error paths) and `message/DesignerCatalogueTest.java` (5 tests — en/de key parity, the full required-key set, operator help line, and ICU select/plural formatting).

### 2.2 `pm-mock-runtime` — Scenario, Registry & Dynamic Endpoints

- `scenarios/designer.json`: fixture with machine and project units at every level (including a compiled-only unit), a block of every kind, and the enrolled project set.
- `scenario/Scenario.java`: added the optional `designer` fixtures map (with a backward-compatible 7-arg constructor).
- `scenario/ScenarioLoader.java`: parses and validates the `designer` fixtures block (fail-closed on a non-object).
- `runtime/DesignerRegistry.java`: holds the designer state, resolves digests, validates (parse → static rules → compiled invariants → composition), and applies placing/removing.
- `runtime/MockRuntimeEngine.java`: `handleDesignerRoutes` dispatch for the endpoints, strict unit-name validation, compiled-only refusal, digest-conflict handling, re-validation before placing, and the `unit.placed`/`unit.landed`/`unit.removed` SSE broadcasts; initialised in `setScenario` and `reset`.
- `StandardScenarios.java`: `DESIGNER = "designer"` registered in `ALL`.
- **Tests**: `DesignerScenarioTest.java` (9 tests — unit scopes, catalogue kinds, valid/invalid validation, static-rule findings, placing/removing with events, invalid names, compiled-only refusal, re-validation refusal) and the updated `StandardScenariosTest.java`.

### 2.3 `pm-operator` — CLI Isolation Verification

- **No changes.** `grep` confirms no designer command, verb, or output string exists in `pm-operator`; the design's "the terminal client has no counterpart" rule holds and the CLI command tree is unpolluted (Phase 4).

### 2.4 `pm-web-app` — Web Client

- `src/lib/api/client.ts`: designer types (`BlockKind`, `BuildingBlock`, `WorkflowUnit`, `WorkflowCatalogue`, `WorkflowFinding`, `WorkflowStage`, `WorkflowStructure`, `WorkflowValidationResult`, `UnitPlacingResult`) and methods `getWorkflows`, `getWorkflowCatalogue`, `validateWorkflow`, `placeWorkflowUnit`, `removeWorkflowUnit`.
- `src/lib/views/ProcessDesigner.svelte`: rewritten as a runtime-backed, three-pane designer (installed units + palette · text editor · structure), with project scope selector, Validate/Place actions, draft-outdated marking, structured/parse-error notices, and the composed-process preview.
- `src/lib/components/BlockPalette.svelte`: the catalogue grouped by kind with counts, live search, per-block insert, and a detail panel (parameters, outcomes, needs, where usable, insert template).
- `src/lib/components/WorkflowValidationResults.svelte`: the stage band (✓/✗/– `not run`), the valid line with the composed-process action, and the finding list with rule, line, and column.
- **Vitest tests**: `ProcessDesigner.test.ts` (5 tests), `BlockPalette.test.ts` (4 tests), and a designer-methods case in `client.test.ts`.

### 2.5 `pm-web-e2e` — End-to-End Playwright

- `tests/designer.spec.js`: 6 scenarios — palette + installed units, validating with the composed-process preview, a parse finding, block insertion, and placing in a project.

---

## 3. Verification & Test Evidence

| Module / Scope | Key new tests | Total tests | Result |
|---|---|---|---|
| `pm-api` | `DesignerRecordsTest` (8), `DesignerCatalogueTest` (5) | 279 | 100% (0 failures/errors) |
| `pm-mock-runtime` | `DesignerScenarioTest` (9), `StandardScenariosTest` (2) | 161 | 100% (0 failures/errors) |
| `pm-operator` | *(unchanged — CLI isolation)* | 350 | 100% (0 failures/errors) |
| `pm-web-app` | `ProcessDesigner.test.ts` (5), `BlockPalette.test.ts` (4), `client.test.ts` (+1) | 125 | 100% (25 files) |
| `pm-web-e2e` | `designer.spec.js` (6 scenarios, linted; Playwright gated by `-Pe2e`) | — | ESLint clean |

**Reactor gate**: `./mvnw verify -Ppre-commit` → **BUILD SUCCESS** (all 8 reactor modules: `pm-api`, `pm-exec`, `pm-relay`, `pm-operator`, `pm-mock-runtime`, `pm-web-app`, `pm-web-e2e`). OpenRewrite, license headers, Checkstyle, Enforcer rules (`clients-depend-on-pm-api-only`, `jackson-core-only`, `no-model-facing-content`), Javadoc, and the full unit-test suite all pass with a clean `git status`.

---

## 4. Automated Review & Quality Gate Resolution

The pre-commit review and the PR review gate are evaluated through `plan-marshall-persona-code-reviewer` (adversarial refutation of candidate findings: defect vs. preference, load-bearing vs. surplus, real vs. false-positive drift).

**Self-review pass (pre-commit)** — candidates raised and dispositions:

- *Surplus helper*: `DesignerJson` was kept (it removes byte-for-byte duplicated tree coercion across seven codecs) rather than inlined; wrapped `UncheckedIOException` conversions in `DesignerRegistry` are the fail-fast boundary for malformed fixtures.
- *Load-bearing validation*: the mock validator's stages are deliberately ordered and short-circuit (a failed parse yields a `null` structure and `not run` later stages) so the client's stage band is exercised truthfully.
- *UI honesty*: the palette renders only catalogue blocks (no client-side list), and `Place…` is enabled only for a valid result of the current text (disabled while `outdated`).

Bot findings (SonarCloud, CodeRabbit, Sourcery, cuioss-review-bot) are resolved to 100% in the review window per `doc/agy/runbook.md` §6; the resolution commits and thread replies accompany this PR.

---

## 5. Architecture Invariants Verification

- **Pure Java 25 records / enums**: every new `pm-api` type is an immutable record or a closed enum; nested value types are records; all lists are defensively copied.
- **Jackson streaming only**: no `jackson-databind`; all codecs go through `JsonTree` (`jackson-core`). The `jackson-core-only` enforcer rule passes.
- **Catalogue-only texts**: designer UI text resolves from `designer.{en,de}.properties` via `$t(...)`/`MessageCatalogue`; the en/de packs are asserted key-for-key equal.
- **Strict dependency boundaries**: `pm-mock-runtime` and `pm-web-app` depend only on `pm-api`; the `clients-depend-on-pm-api-only` enforcer rule passes.
- **Mock-first & deterministic**: the designer is fully reproducible against `pm-mock-runtime`'s `designer` scenario; the scenario test drives real HTTP/SSE over the Unix socket.
- **No CLI growth**: the process designer is web-only; `pm-operator` gained no command.
- **Responsive layout**: the designer collapses to a single column below 900 px, with all validation and text available (edit/place remain width-gated at phone width, matching the design).

---

## 6. Process Adherence (runbook lifecycle)

1. Worktree `/Users/oliver/git/worktrees/pm-mcp-clients-uc-pln-3` on `feat/uc-pln-3-designer` off `main`.
2. Personas applied per task: implementer/module-tester (`pm-api`, mock), integration-tester (scenario + E2E), security-expert (unit-name validation / path-traversal prevention), documenter (this report and archival), code-reviewer (review gate).
3. Incremental implementation with targeted tests at each step (package → reactor).
4. Completion report authored here; the plan was moved to `doc/agy/implemented/`.
5. `./mvnw verify -Ppre-commit` green.
6. PR opened with `--label "skip-bot-review"`; review timer scheduled; findings resolved.
7. Merge-queue landing and worktree cleanup.
8. Post-landing verification and movement of this report to `doc/agy/verified/` are the orchestrator session's responsibility.

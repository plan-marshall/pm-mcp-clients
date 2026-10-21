# Implementation Plan: `mock-runtime-fully-onboarded-scenario` (Comprehensive Multi-Domain Showroom Scenario with All Elements in Place)

This implementation plan governs the creation of the comprehensive **`fully-onboarded.json`** scenario in `pm-mock-runtime`.

While the 5 consolidated scenarios partition domain-specific test fixtures (onboarding, running work, projects/start, reports/system, designer), developers and testers lack a single, fully populated "showroom" scenario where the machine is completely onboarded and **all UI views and runtime domains are active and populated simultaneously**.

This plan delivers a unified, realistic, fully onboarded mock environment enabling complete end-to-end UI testing, interactive walkthroughs, and Playwright verification across both `pm-web-app` and `pm-operator` without switching scenario files.

It operationalizes [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) under the execution discipline of [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Use Cases** | [`UC-ONB-1..10`](../../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc) (Machine Onboarding & Settings), [`UC-ENT-1..2`](../../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc) (Entry & Paired Devices), [`UC-PRJ-1..4`](../../../../plan-marshall-documentation/doc/use-cases/04-projects.adoc) (Projects & Setup), [`UC-PLN-1..6`](../../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc) (Plans, Epics, Decisions, Reports, Designer), [`UC-STA-1..9`](../../../../plan-marshall-documentation/doc/use-cases/05-starting-work.adoc) (Starting Work), [`UC-CTL-1..5`](../../../../plan-marshall-documentation/doc/use-cases/06-controlling-work.adoc) (Controlling Work), [`UC-SYS-1..3`](../../../../plan-marshall-documentation/doc/use-cases/07-system.adoc) (System & Cleanup) |
| **Specifications** | [`05-mock-runtime.adoc`](../../../../plan-marshall-documentation/doc/specification/runtime-model/05-mock-runtime.adoc), [`04-operator-api.adoc`](../../../../plan-marshall-documentation/doc/specification/runtime-model/04-operator-api.adoc), [`state-catalogue.adoc`](../../../../pm-mcp-core/doc/specification/state-catalogue.adoc) |
| **Contracts** | [`api-contract.adoc`](../../plans/implemented/api-contract.adoc), [`overview-contract.adoc`](../../plans/implemented/overview-contract.adoc), [`process-contract.adoc`](../../plans/implemented/process-contract.adoc), [`decision-contract.adoc`](../../plans/implemented/decision-contract.adoc), [`control-contract.adoc`](../../plans/implemented/control-contract.adoc), [`report-contract.adoc`](../../plans/implemented/report-contract.adoc), [`system-contract.adoc`](../../plans/implemented/system-contract.adoc), [`project-contract.adoc`](../../plans/implemented/project-contract.adoc), [`start-contract.adoc`](../../plans/implemented/start-contract.adoc), [`designer-contract.adoc`](../../plans/implemented/designer-contract.adoc) |
| **Local Client Design** | [`doc/design/web-app/README.adoc`](../../design/web-app/README.adoc), [`doc/design/terminal-client/README.adoc`](../../design/terminal-client/README.adoc), [`doc/design/shared/`](../../design/shared/) |
| **Prerequisite Plans** | [`doc/agy/mock-runtime-foundation-and-consolidation-plan.md`](mock-runtime-foundation-and-consolidation-plan.md), [`doc/agy/mock-runtime-running-work-plan.md`](mock-runtime-running-work-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.md`](../../meta/implement_use-cases.md) |
| **Operational SOP** | [`doc/agy/runbook.md`](../runbook.md) |

---

## 2. Scope & Objectives

### 2.1 The Omni-Domain `fully-onboarded.json` Scenario
1. **Full Workspace Initialization**:
   - Machine is fully onboarded (`machine.name: "mock-machine"`, `onboarded: true`).
   - Web listener enabled on port 7420 (`lan: true`, `open: true`), pairing code pre-configured (`KXQB-MRTN`).
   - At least two paired devices (`Primary Desktop Browser`, `Terminal Companion`) with last seen timestamps.
2. **Synchronized Interconnected Content Across All 8 UI Domains**:
   - **Projects Explorer (`/projects`)**:
     - 3 enrolled projects (`pm-mcp-clients` [favorite], `pm-mcp-core`, `plan-marshall`).
     - Project setups, git repository roots, active branches, build environments (Maven, Java 25, npm 10.8), and ended work archives.
     - Git file trees for browsing source code and attachable project files.
   - **Overview & Active Work (`/overview`, `/plans`, `/epics`)**:
     - Active plans in diverse lifecycle phases (`fix-relay` in execute, `docs-typo` in outline, `relay-token-store` waiting for overlap).
     - Active epics (`relay-hardening`) with workstream decompositions and token breakdowns (`≥ 1.24 M tokens`).
     - Paused plans, held plans with resolvable holds (`overlap`, `integrity`), and token counters with unmeasured task figures.
   - **Plan Process & Live Activity (`/plans/{id}/process`, `/activity`)**:
     - Full 6-phase hierarchical process tree (`init`, `refine`, `outline`, `plan`, `execute` with nested `land` sub-workflow, `finalize`).
     - Leaf details and live worker activity streaming sanitized plain text (`OutputSafety.stripControlSequences`).
   - **Decisions (`/decisions`)**:
     - Pending decisions in inbox:
       - High-stakes architectural decision with one-step recommendation reveal (`decision.recommendation.show`).
       - Multi-part question with `depends_on` dependency.
       - Question with **"Chat about it first"** linked to `ChatPanel` with proposal read-back.
   - **Reports & Completion Reports (`/reports`, `/completion`)**:
     - Reports inbox with count badge (5 unread reports):
       - Plan ended report (`fix-relay ended: merged (#214)`), plan started, epic overlap wait, cleanup completed, system notice.
     - Full `PlanCompletionReport` detail (`/api/v1/plans/fix-relay/completion`): deliverables, phase timeline, duration, and token breakdown (`380 k in · 32 k out`).
   - **System View & Cleanup (`/system`, `/system/cleanup`)**:
     - Runtime metrics: version `0.9.1`, memory, CPU load, storage, workers (5 active), build slots (1 used / 2 total).
     - Runtime upgrade simulation: newer version `0.10.0` available with release notes link.
     - Drain, stop, and restart controls.
     - Cleanup preview with active candidates (abandoned branches, dangling test worktrees, kept protected items).
   - **Machine Settings (`/settings`, `/api/v1/machine/settings`)**:
     - Revision-guarded settings resource: harnesses (Claude Code, OpenCode), models, roles (31 roles), providers (GitHub gh), documents mode, cleanup durations, plan defaults, and remembered consents.
   - **Starting Work (`/start`)**:
     - Recipes list, intake draft with request sources, local changes detection, PR review links, lesson sources, and refinement chat.
   - **Process Designer (`/designer`, `/api/v1/workflows`)**:
     - Workflow units catalogue, structure tree, side-by-side preview, validation engine with line-numbered findings, diff, and placement.
   - **Real-Time SSE Event Stream (`/api/v1/events`)**:
     - Background event emitter simulating periodic updates (token ticks, task transitions, report arrivals, decision updates) across all scopes.

### 2.2 Launcher & Tooling Integration
1. **Canonical Scenario Registration**:
   - Register `fully-onboarded` in `StandardScenarios.java` (`StandardScenarios.FULLY_ONBOARDED = "fully-onboarded"`).
   - Add `fully-onboarded` to `src/main/resources/scenarios/primary.txt` and update launcher menu in `start.sh` so developers can start it interactively via `./start.sh -s fully-onboarded`.
   - Update `expectedCount` in `StandardScenarios.java` and tests.

---

## 3. Concurrency & Isolation

- **Depends on**: Wave 1 (`mock-runtime-foundation-and-consolidation`) and Wave 2b (`mock-runtime-running-work`).
- **Parallelizable with**: `cli-fullscreen-plan-view-plan.md`, `web-arrival-banners-and-i18n-cache-plan.md`, `web-decisions-and-actions-plan.md`.

### Boundary Rules
- **Sole Owner of**:
  - `pm-mock-runtime/src/main/resources/scenarios/fully-onboarded.json` (new)
  - Scenario registration in `StandardScenarios.java`, `primary.txt`, and `start.sh`
  - Integration test `FullyOnboardedScenarioTest.java` in `pm-mock-runtime`
  - Showroom E2E test `pm-web-e2e/tests/showroom.spec.js` in `pm-web-e2e`
- **Read-Only / No Touch**:
  - `pm-api` contracts and message bundles.
  - Existing consolidated scenarios (`onboarding.json`, `running-work.json`, etc.).
  - Production code of `pm-operator` and `pm-web-app`.

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-mock-fully-onboarded` on branch `feat/mock-runtime-fully-onboarded` off latest `main`.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-java:java-core` for scenario composition, `MockRuntimeEngine` alignment, and Java constants.
  - `pm-dev-java:java-security` for output safety and mock token/secret handling.
  - `plan-marshall-persona-integration-tester` & `pm-dev-java:junit-integration` for `FullyOnboardedScenarioTest` and Playwright `showroom.spec.js`.
  - `plan-marshall-persona-code-reviewer` for review gate and 100% findings resolution.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/mock-runtime-fully-onboarded-scenario-plan-report.md`) and initial plan moved to `doc/agy/implemented/mock-runtime-fully-onboarded-scenario-plan.md`. (Do NOT move report to `verified/` — Step 8 is reserved for orchestrator).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on worker PRs; reserved for orchestrator docs PRs), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/mock-runtime-fully-onboarded-scenario-plan-report.md` by orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Architecture & Schema** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | `ScenarioLoader` compliance, `primary.txt` sync, and `StandardScenarios` registration. |
| **Phase 2: Omni-Domain Scenario Data** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core`<br>`pm-dev-java:java-security` | Authoring `fully-onboarded.json` with interconnected state definitions across all 8 UI areas. |
| **Phase 3: Engine Dynamic Handlers Wiring** | `plan-marshall-persona-implementer` | `pm-dev-java:java-core` | Ensure `MockRuntimeEngine` enables all domain dynamic routes for `fully-onboarded` (`declaresSystem`, `declaresDecisions`, etc.). |
| **Phase 4: Automated Verification** | `plan-marshall-persona-integration-tester` | `pm-dev-java:junit-integration`<br>`pm-dev-frontend:jest-testing` | Unit tests for scenario loading, endpoint coverage suite, and Playwright showroom smoke test. |
| **Phase 5: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Pre-commit validation (`-Ppre-commit`) and PR review triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Scenario Schema & Launcher Registration
- [x] **1.1 Register Scenario Constants & Primary Index**:
  - Add `public static final String FULLY_ONBOARDED = "fully-onboarded";` to `StandardScenarios.java`.
  - Add `fully-onboarded` to `pm-mock-runtime/src/main/resources/scenarios/primary.txt`.
  - Update `expectedCount` in `StandardScenarios.java` (increment count) and update `StandardScenariosTest.java`.
  - Update `start.sh` menu options and help text.
- [x] **1.2 Register Alias & Routing in `ScenarioRegistry`**:
  - Register `fully-onboarded` in `ScenarioRegistry.java`.

### Phase 2: Author Comprehensive `fully-onboarded.json` Fixture
- [x] **2.1 Machine, Web Listener & Paired Devices**:
  - `GET /api/v1/status`: status `ready`, machine onboarded, port 7420, LAN enabled.
  - `GET /api/v1/web/devices`: at least 2 paired devices.
  - `GET /api/v1/machine/settings`: comprehensive machine settings resource.
- [x] **2.2 Projects & Project Explorer**:
  - `GET /api/v1/projects`: 3 enrolled projects (`pm-mcp-clients`, `pm-mcp-core`, `plan-marshall`).
  - Project setups, git repository roots, files tree, and ended work archives.
- [x] **2.3 Plan Overview, Plans & Epics**:
  - `GET /api/v1/overview`: running work breakdown with held plans and attention reasons.
  - `GET /api/v1/plans`: active plans (`fix-relay`, `docs-typo`, `relay-token-store`).
  - `GET /api/v1/epics`: active epic (`relay-hardening`) with 3 workstreams.
  - Process trees, task activity streams, intent, and outline.
- [x] **2.4 Decisions**:
  - `GET /api/v1/decisions`: 3 pending decisions (high-stakes with recommendation reveal, multi-part, and chat-first).
- [x] **2.5 Reports & Completion Report**:
  - `GET /api/v1/reports`: 5 unread/read reports across plan, epic, cleanup, and system kinds.
  - `GET /api/v1/plans/fix-relay/completion`: full completion report with token breakdown, phase timeline, and PR outcome.
- [x] **2.6 System View & Cleanup**:
  - `GET /api/v1/system`: full `RuntimeMetrics`, upgrade available to `0.10.0`, release notes link.
  - `GET /api/v1/system/cleanup`: cleanup preview with active candidates and kept protected items.
- [x] **2.7 Starting Work & Process Designer**:
  - `GET /api/v1/recipes`, `/api/v1/intake/draft`, and start chat fixtures.
  - `GET /api/v1/workflows/catalogue` and `/api/v1/workflows` installed units.

### Phase 3: Engine Dynamic Route Alignment
- [x] **3.1 Domain Declaration in `MockRuntimeEngine`**:
  - Ensure `declaresSystem()`, `declaresDecisions()`, and other domain guards recognize `fully-onboarded` so all dynamic routes are served out of the box.

### Phase 4: Automated Verification & E2E Showroom
- [x] **4.1 Mock Runtime Suite (`FullyOnboardedScenarioTest.java`)**:
  - Test scenario loading via `ScenarioLoader`.
  - Assert that every domain route responds with HTTP 200 and valid JSON tree matching `pm-api` codecs.
  - Assert SSE events stream across all scopes.
- [x] **4.2 Playwright Showroom Walkthrough (`tests/showroom.spec.js`)**:
  - Boot `fully-onboarded` in mock runtime.
  - Execute end-to-end traversal across:
    1. Header navigation & badges (`Decisions: 3`, `Reports: 5`).
    2. Projects view: browse projects and file trees.
    3. Overview view: inspect plans and epics.
    4. Plan view: navigate 6-phase tree and leaf activity.
    5. Epic view: inspect workstreams and token splits.
    6. Decisions view: reveal recommendation and switch parts.
    7. Reports view: inspect unread reports and open completion report.
    8. System view: verify metrics, upgrade notice, and cleanup preview.
    9. Settings view: verify machine settings and paired devices.
- [x] **4.3 Pre-Commit Gate**:
  - Run `./mvnw verify -Ppre-commit`.

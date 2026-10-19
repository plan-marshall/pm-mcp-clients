# Implementation Plan: `web-onboarding` (Onboarding and Start in the Web App)

This implementation plan governs the execution of the `web-onboarding` work package (`doc/plans/web-onboarding.adoc`, epic clients) in `pm-web-app` (with scenario support in `pm-mock-runtime`).

It operates strictly under the rules and lifecycle defined in [`doc/agy/runbook.md`](runbook.md).

---

## 1. Scope & Objectives

The goal is to implement the onboarding route, start view, and pairing flows in `pm-web-app`, backed by the design system established in `web-app-foundation` and verified through Playwright component/view tests against `pm-mock-runtime`:

1. **Onboarding Route & Flow State Machine**:
   - Single onboarding route (`/onboarding`) rendering the states reported by the runtime.
   - Completed onboarding steps render in a clean, read-only summarized state per `web-design`.
2. **Entry & Pairing Pages (UC-ENT-1, UC-ENT-2)**:
   - Start and device pairing views (`/pair`, `/entry`).
   - Pairing code entry, QR code presentation for LAN connections, loopback credential entry.
   - Offline / lost connection indicators and reactive reconnection handling.
3. **Responsive & Themed Layout**:
   - Verification across desktop (>= 1024px) and phone (< 768px) viewport widths.
   - Flawless light and dark theme support using CSS custom properties from `web-app-foundation`.
4. **Machine Settings & Aspects (UC-ONB-10)**:
   - Machine settings route (`/settings/machine`) covering all aspects (harnesses, models, providers, documents mode, consents).
   - Interactive withdrawal of remembered consents.
   - Token rotation / replacement workflows.
   - Concurrent modification conflict dialog (`409 conflict`): review differences, apply again, or discard.
   - Network listener state handling, including warning view when listener is disabled.
5. **Paired Devices Management (UC-ENT-2)**:
   - Paired devices management in settings: device list, device renaming, individual revocation, and revoke-all-others action.
6. **Tokens, Never Money**:
   - Strict rendering of token counts and floor estimates; zero monetary cost representations.
7. **Internationalization & No Hardcoded Strings**:
   - 100% of UI strings loaded via the client message catalogue; test assertions ensuring zero hardcoded user-facing literals.
8. **View & Interaction Testing**:
   - Automated view tests covering every onboarding and pairing scenario against `pm-mock-runtime`.

### Target Work Package & References
- Specification: `doc/plans/web-onboarding.adoc`.
- Use Cases: `UC-ONB-1` through `UC-ONB-10`, `UC-ENT-1`, `UC-ENT-2`.
- Specifications: Runtime Model § Mock Runtime, § Local API; State Catalogue § Machine Onboarding, § Machine Settings.
- Design: `doc/design/web-app/README.adoc`, `onboarding.adoc`, `machine-settings.adoc`, `doc/design/shared/consents.adoc`.
- Primary Module: `pm-web-app`.

---

## 2. Concurrency & Isolation

This plan executes concurrently in Wave 9 alongside:
- `cli-onboarding` (`pm-operator:src/**`)
- `start-contract` (`pm-api:de.planmarshall.api.start.*`, `pm-mock-runtime:scenarios/start-*.json`)

### Boundary Rules
- `pm-web-app`: Sole owner of web components, Svelte views, CSS styles, and frontend view tests.
- `pm-operator`: No touch (owned by `cli-onboarding`).
- `pm-api`: Consumer of existing contract records (`de.planmarshall.api.onboarding.*`, `de.planmarshall.api.entry.*`).
- `pm-mock-runtime`: Read-only consumer of mock scenarios; if web-specific fixture refinements are needed, maintain strict non-collision with `start-*.json`.

---

## 3. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-onboarding` on branch `feat/web-onboarding`.
- [x] 2. Domain skills & engineering personas loaded:
  - `persona-implementer` & `pm-dev-frontend-javascript` for Svelte/HTML5/CSS component implementation.
  - `pm-dev-frontend-jest-testing` & `persona-module-tester` for component and view testing.
  - `persona-security-expert` for credential masking, token rotation forms, and input validation.
  - `persona-code-reviewer` for review bot resolution.
- [x] 3. Incremental implementation following the 5-Phase technical recipe with local tests passing.
- [x] 4. Final completion report written (`doc/agy/web-onboarding-plan-report.md`), initial plan moved to `doc/agy/implemented/web-onboarding-plan.md`, and package marked done in `doc/plans/README.adoc`. (Do NOT move report to `verified/` — post-landing verification moves it).
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit` or `npm test`).
- [ ] 6. PR opened (NEVER use `skip-bot-review`), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-onboarding-plan-report.md` by orchestrator session.

---

## 4. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Route & State Machine** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript` | Establish `/onboarding` route, client-side store, and state-driven step view progression. |
| **Phase 2: Onboarding & Pairing Views** | `plan-marshall-persona-implementer` | `pm-dev-frontend-css` | Implement desktop/mobile views, light/dark styling, checklists, consent cards, and token summaries. |
| **Phase 3: Machine Settings & Devices** | `plan-marshall-persona-implementer` | `pm-dev-frontend-javascript-security` | Implement `/settings/machine`, consent withdrawal, credential rotation, paired devices, and 409 conflict flow. |
| **Phase 4: View Tests & Scenario Suites** | `plan-marshall-persona-module-tester` | `pm-dev-frontend-jest-testing` | Author and execute automated view tests against `pm-mock-runtime` scenarios. |
| **Phase 5: Quality Gate & PR Review** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Run lint/build validations, verify zero literal UI strings, and resolve bot feedback. |

---

## 5. Phased Technical Recipe

### Phase 1: Onboarding State Machine & Routing Architecture
- Mount `/onboarding`, `/entry`, and `/settings/machine` routes within `pm-web-app`.
- Create reactive state store reflecting backend `/api/v1/onboarding` and `/api/v1/entry` resources.
- Guarantee read-only rendering of previously completed onboarding steps.

### Phase 2: Onboarding & Pairing UI Components
- Build step components: welcome/intro, language selection, harness discovery and token estimate review, model configuration, role assignment, and completion summary.
- Implement pairing interface (`/pair`) supporting pairing code entry and connection status heartbeats.
- Ensure strict responsiveness (desktop 1024px+, mobile <768px) and dark/light color palette compliance.

### Phase 3: Machine Settings & Paired Devices Interface
- Build `/settings/machine` views for documents mode (`copy`/`move`/`ask`), provider tokens, and remembered consents with one-click withdrawal.
- Build Paired Devices sub-view: list active devices, rename device inline, revoke session, revoke all other devices.
- Build modal / inline resolution for `409 conflict` when settings are concurrently modified.

### Phase 4: Mock Scenario Validation & View Testing
- Wire view tests against `pm-mock-runtime` scenario endpoints.
- Validate: happy path onboarding, refusal of consent, failing provider checks, token rotation, conflict handling, and device revocation.
- Validate bilingual text loading through i18n bundle without fallback leaks.

### Phase 5: Verification & Review Resolution
- Run complete test suite and code checks (`npm run test`, `npm run lint`, `./mvnw verify`).
- Submit PR without `skip-bot-review` and resolve all bot review comments.

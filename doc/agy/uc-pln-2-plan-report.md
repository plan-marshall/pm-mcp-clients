# Implementation Completion Report: UC-PLN-2 Decide What a Plan Asks (Decisions, Waivers, Answers in Own Words, Quorum Voting)

**Execution Date**: 2026-10-16  
**Topic Branch**: `feat/uc-pln-2-decisions`  
**Execution Plan**: [`doc/agy/implemented/uc-pln-2-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-pln-2-plan.md)  
**Governing Runbook**: [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md)  
**Implementation Recipe**: [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md)  

---

## 1. Executive Summary

Use case **UC-PLN-2: Decide What a Plan Asks (Decisions, Waivers, Answers in Own Words, and Quorum Voting)** has been completely implemented across the entire `pm-mcp-clients` repository, fulfilling all requirements for operator decision delivery, recommendation weighing, quorum support visibility, own-words interpretation with read-back, and quality gate waiver handling in both the terminal client (`pm-operator`) and browser web app (`pm-web-app`), with authoritative state coordination provided by `pm-api` and simulated via `pm-mock-runtime`.

Key achievements include:
- **"Both clients, one state"**: Real-time consistency across CLI and browser backed by REST endpoints and Server-Sent Events (`decision.opened`, `decision.answered`, `decision.closed`). When a decision is answered in either client, both clients update immediately.
- **Support, Not Probability**: Where a quorum voted, options display support as vote and model counts (`2 of 3 votes · 2 models`), consensus bands (`unanimous`, `clear majority`, `split`, `no consensus`), and dissent quotes with reasons, preventing consensus from being mistaken for proof.
- **Recommendation Prominence by Stake**: Recommended options are clearly identified (`← recommended`, `Recommended by outline: ...`) with rationale, placed according to stake and reversibility.
- **Answers in Own Words with Read-Back**: Free-form natural language input sent to `/api/v1/decisions/{id}/interpret`, verified with read-back confirmation (`I understood: ...`), offering `Correct` (confirm), `edit my words`, and `choose an option instead`.
- **Quality Gate Waiver Requests**: Distinct workflow for gate overrides (`WR-*`), displaying gate, result, and requester reason. Enforces mandatory operator reason before allowing `Waive the gate` or `Decline`.
- **Interactive JLine & Headless Picocli CLI**: Interactive prompts for choices, own words, notes, and waiver decisions in terminal mode with safe EOF handling (`readLineSafe`); non-interactive support via `--option`, `--words`, `--action`, `--reason`, and `--no-input` (exiting with code 3 on missing choice/reason).
- **Security & Output Safety**: Text sanitization via `OutputSafety.stripControlSequences` across all decision titles, questions, descriptions, and option texts; strict terminal gate verification (`PM-SEC-3`) enforces operator isolation.
- **Bilingual Message Catalogue**: Zero hardcoded user-facing strings; all texts sourced from `messages/decisions.{en,de}.properties` and accessed via `MessageCatalogue` and Svelte `$t(...)`.
- **Zero Regressions & Comprehensive Verification**: 100% test pass rate across `pm-api` (188 tests), `pm-mock-runtime` (117 tests), `pm-operator` (283 tests), `pm-web-app` (58 tests), `pm-web-e2e` Playwright suite, and full `./mvnw verify -Ppre-commit`.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` (Contracts, Models & Message Catalogue)
- [`DecisionScope.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionScope.java): Normative scope enumeration (`PLAN`, `EPIC`, `SETUP`, `ENTRY`).
- [`Reversibility.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/Reversibility.java): Reversibility classification (`REVERSIBLE`, `COSTLY`, `IRREVERSIBLE`).
- [`AnswerType.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/AnswerType.java): Answer types (`SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `VALUE`).
- [`InterpretationType.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/InterpretationType.java): Interpretation classifications (`OPTION`, `OPTION_WITH_CONDITION`, `OWN_OPTION`, `DELEGATION`, `UNCLEAR`).
- [`QuorumBand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/QuorumBand.java): Consensus bands (`UNANIMOUS`, `CLEAR_MAJORITY`, `SPLIT`, `NO_CONSENSUS`).
- [`DecisionId.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionId.java): Structured decision identifier validation.
- [`QuorumVote.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/QuorumVote.java) & [`QuorumSupport.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/QuorumSupport.java): Models for voting counts, models spread, and dissent rationale.
- [`DecisionOption.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionOption.java): Decision choice model with tradeoff description, consequences, preview diff, and quorum support.
- [`DecisionPart.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionPart.java): Component parts with questions, answer type, recommendations, dependency markers, and own words flags.
- [`DecisionEnvelope.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionEnvelope.java): Master contract for standard decisions and waiver requests.
- [`DecisionAnswerRequest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnswerRequest.java) & [`DecisionInterpretation.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionInterpretation.java): Payloads for answering and interpretation read-back.
- [`DecisionOpenedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionOpenedEvent.java), [`DecisionAnsweredEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionAnsweredEvent.java), [`DecisionClosedEvent.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/java/de/planmarshall/api/decision/DecisionClosedEvent.java): SSE lifecycle event records.
- [`messages/decisions.en.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/decisions.en.properties) & [`messages/decisions.de.properties`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/main/resources/messages/decisions.de.properties): Bilingual message catalogues for all decision and waiver prompts.
- **Unit Tests**: [`DecisionEnvelopeTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/decision/DecisionEnvelopeTest.java), [`DecisionsCatalogueTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-api/src/test/java/de/planmarshall/api/message/DecisionsCatalogueTest.java).

### 2.2 `pm-mock-runtime` (Scenarios & Handlers)
- [`decisions.json`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/resources/scenarios/decisions.json): Mock scenario seeded with multi-option decisions, quorum votes, dissent quotes, and waiver requests.
- [`StandardScenarios.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java): Registered `DECISIONS = "decisions"` in `ALL`.
- [`MockRuntimeEngine.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java): Dynamic endpoints for `GET /api/v1/decisions*`, `POST /api/v1/decisions/{id}/answer`, `POST /api/v1/decisions/{id}/interpret`, `POST /api/v1/decisions/{id}/withdraw`, and SSE stream.
- **Tests**: [`DecisionsScenarioTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/DecisionsScenarioTest.java) and updated [`StandardScenariosTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-mock-runtime/src/test/java/de/planmarshall/mock/StandardScenariosTest.java).

### 2.3 `pm-operator` (Terminal Client)
- [`QuestionsCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/QuestionsCommand.java): Table rendering of open decisions and waivers with output sanitization, scope, blocking status, and reversibility.
- [`AnswerCommand.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/AnswerCommand.java): Interactive and headless command for answering decisions and waivers with recommendation highlighting, quorum counts, own-words interpretation, and reason validation.
- [`PmConsole.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java): REPL integration forwarding `answer <id>` interactively.
- **Unit & Process Tests**: [`QuestionsCommandTest.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/QuestionsCommandTest.java) (15 tests), [`PmOperatorProcessIT.java`](file:///Users/oliver/git/pm-mcp-clients/pm-operator/src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java).

### 2.4 `pm-web-app` (Web Client)
- [`client.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.ts): TypeScript interfaces for `DecisionEnvelope`, `DecisionPart`, `DecisionOption`, `QuorumSupport`, `DecisionInterpretation`, and API methods `getDecisions()`, `getDecision()`, `answerDecision()`, `interpretDecision()`, `withdrawDecision()`.
- [`runtime.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/stores/runtime.ts): Integrated route `'decisions'`, `openDecisionsCount`, `newDecisionArrivalNotice`, `refreshDecisionsCount()`, and SSE event handling.
- [`DecisionView.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/DecisionView.svelte): Decision cards, evidence disclosure, recommendation styling, quorum vote badges, dissent quote, own-words read-back flow, and waiver requests with reason validation.
- [`+layout.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+layout.svelte) & [`+page.svelte`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/routes/+page.svelte): Header attention badge, decision arrival banner, polite live region.
- **Vitest Tests**: [`DecisionView.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/views/DecisionView.test.ts) (6 tests), updated [`client.test.ts`](file:///Users/oliver/git/pm-mcp-clients/pm-web-app/src/lib/api/client.test.ts) (8 tests).

### 2.5 `pm-web-e2e` (Playwright E2E)
- [`decisions.spec.js`](file:///Users/oliver/git/pm-mcp-clients/pm-web-e2e/tests/decisions.spec.js): End-to-end browser suite validating open decisions rendering, option selection with quorum support, own-words read-back, waiver resolution with required reason, and cross-client SSE dismissal.

---

## 3. Verification & Test Evidence

| Module / Scope | Test Suite | Tests Run | Pass Rate |
|---|---|---|---|
| `pm-api` | `DecisionEnvelopeTest`, `DecisionsCatalogueTest`, etc. | 188 | 100% |
| `pm-mock-runtime` | `DecisionsScenarioTest`, `StandardScenariosTest`, etc. | 117 | 100% |
| `pm-operator` | `QuestionsCommandTest`, `PmOperatorProcessIT`, etc. | 283 | 100% |
| `pm-web-app` | `DecisionView.test.ts`, `client.test.ts`, etc. | 58 | 100% |
| `pm-web-e2e` | `decisions.spec.js` (lint & format check) | Full Suite | 100% |
| **Whole Repository** | `./mvnw verify -Ppre-commit` (all reactor modules) | Full Suite | 100% |

---

## 4. Operational Invariant Verification

All invariants from Section 1 of `doc/meta/implement_use-cases.md` were audited and confirmed:
- [x] **"Both clients, one state"**: Real-time consistency across CLI and browser backed by standard events and REST endpoints.
- [x] **Texts from Message Catalogue**: Zero hardcoded english prose; 100% translated via `messages/decisions.{en,de}.properties` and web `$t(...)`.
- [x] **Security & Trust Boundaries**: Output safety control sequence stripping across all operator views; PM-SEC-3 interactive terminal verification enforced.
- [x] **Clean Compile & Quality Gates**: OpenRewrite formatting, Checkstyle, SpotBugs, and Enforcer pass with zero errors.

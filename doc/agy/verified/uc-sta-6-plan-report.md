# Completion Report: UC-STA-6 Decompose an Epic into Workstreams & Staged Plans

Companion report of the implementation plan [`doc/agy/implemented/uc-sta-6-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-sta-6-plan.md),
executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) on branch
`feat/uc-sta-6-decomposition`.

---

## 1. Executive Summary

UC-STA-6 gets from an epic's starting sources to its workstreams and staged plans, cut the way the operator
wants them, before any plan starts. The implementation delivers the whole client-side slice end to end:

- **Contracts (`pm-api`)**: a new `de.planmarshall.api.decomposition` package with the immutable records of a
  cut, its closed lists, a pure validation kernel (surface disjointness, dependency cycle, workstream
  integrity), and a bilingual message area `decomposition.{en,de}.properties`.
- **Mock runtime (`pm-mock-runtime`)**: a stateful `scenarios/decomposition.json`, four dynamic endpoints under
  `/api/v1/projects/{id}/epics/{epic_id}/decomposition/*`, and the SSE broadcasts
  `epic.decomposition_updated` and `epic.launchable`.
- **Operator CLI (`pm-operator`)**: `epic decompose`, `epic review`, and `epic instruct`, with the operator
  gate (PM-SEC-3) and safe text rendering of every server-provided string.
- **Web app (`pm-web-app`)**: the route view `EpicDecompositionView.svelte` at `/epics/[id]/decomposition`,
  with workstream cards, staged-plan surface conflict tags, a conflict banner, an instruction modal with
  read-back, and the Agree & commit action.
- **Verification**: unit suites in `pm-api`, `pm-mock-runtime`, and `pm-operator`, Vitest component tests, and
  the Playwright suite `pm-web-e2e/tests/decomposition.spec.js`.

The cut's states (`draft → check → review → read-back → commit`, plus `held`) are modelled explicitly, and the
validation rules are shared: the mock runtime's `check`/`commit` hand the same `DecompositionValidator` output
that the CLI and the web view render.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts, validation, bilingual catalogue

| File | Purpose |
|---|---|
| `src/main/java/de/planmarshall/api/decomposition/package-info.java` | Javadoc for the package |
| `.../decomposition/EpicDecomposition.java` | Epic id, state, revision, workstreams, findings; `everyCheckPasses`, `stagedPlanCount`, `plan(id)` |
| `.../decomposition/WorkstreamRecord.java` | Id, name, description, order, ordered staged plans |
| `.../decomposition/StagedPlanRecord.java` | Plan id, title, objective, expected surface, dependencies, change/lifecycle marker |
| `.../decomposition/DecompositionCheckFinding.java` | Severity, rule, affected plans, message |
| `.../decomposition/DecompositionInstructRequest.java` | Plan id (optional), operator words, action |
| `.../decomposition/DecompositionCommitResult.java` | Status, ledger revision, launchable plan count |
| `.../decomposition/DecompositionState.java` | Closed list `draft`, `check`, `review`, `read_back`, `commit`, `held` |
| `.../decomposition/CheckSeverity.java` | Closed list `info`, `warning`, `error` |
| `.../decomposition/InstructAction.java` | Closed list `edit`, `add`, `rewrite` |
| `.../decomposition/DecompositionValidator.java` | Pure check kernel: surface disjointness, dependency cycle, workstream integrity |
| `src/main/resources/messages/decomposition.en.properties` | English catalogue |
| `src/main/resources/messages/decomposition.de.properties` | German catalogue |
| `src/main/java/de/planmarshall/api/message/MessageCatalogue.java` | Registered the `decomposition` area |
| `src/test/java/de/planmarshall/api/decomposition/DecompositionRecordsTest.java` | Record round-trips, closed lists, boundary/negative parsing |
| `src/test/java/de/planmarshall/api/decomposition/DecompositionValidatorTest.java` | Disjointness, cycle, integrity, dropped/locked handling |
| `src/test/java/de/planmarshall/api/message/DecompositionCatalogueTest.java` | Bilingual key parity and message formatting |

### 2.2 `pm-mock-runtime` — scenario and dynamic endpoints

| File | Purpose |
|---|---|
| `src/main/resources/scenarios/decomposition.json` | Stateful fixture (previewed data for the cut) |
| `src/main/java/de/planmarshall/mock/StandardScenarios.java` | Added `DECOMPOSITION` to the constants and `ALL` |
| `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | `handleDecompositionRoutes` plus `GET`, `check`, `instruct`, `commit` handlers, seeded cuts for `relay-hardening` (surface overlap) and `storage-migration` (dependency cycle), and the two SSE broadcasts |
| `src/test/java/de/planmarshall/mock/DecompositionScenarioTest.java` | Load, check, cycle, instruct, add, commit/refuse, 404s, SSE |

### 2.3 `pm-operator` — CLI verbs

| File | Purpose |
|---|---|
| `src/main/java/de/planmarshall/operator/DecomposeCommand.java` | `decompose`, `ReviewCommand` (`review --agree`), `InstructCommand` (`instruct`), and the shared `DecompositionSupport` HTTP/printing surface |
| `src/main/java/de/planmarshall/operator/EpicCommand.java` | Wired the three verbs under `epic`, replacing the former minimal `decompose` |
| `src/test/java/de/planmarshall/operator/DecomposeCommandTest.java` | Tree printing, draft again, review/agree, refusal, read-back, missing words |
| `src/test/java/de/planmarshall/operator/EpicCommandTest.java` | Updated `epic decompose` to the new tree behaviour |
| `src/test/java/de/planmarshall/operator/PmOperatorProcessIT.java` | Added a process-level `epic decompose` against the runtime fixture |

### 2.4 `pm-web-app` — route view

| File | Purpose |
|---|---|
| `src/lib/views/EpicDecompositionView.svelte` | Workstreams, staged plans, surface conflict tags, conflict banner, instruction modal with read-back, Agree & commit |
| `src/routes/epics/[id]/decomposition/+page.svelte` | The route at `/epics/[id]/decomposition` |
| `src/lib/api/client.ts` | Decomposition records and `getDecomposition`, `checkDecomposition`, `instructDecomposition`, `commitDecomposition` |
| `src/lib/stores/runtime.ts` | `decomposition` route, `selectedProjectId`, `openEpicDecomposition` |
| `src/routes/+page.svelte` | Routes the `decomposition` store state to the view |
| `src/lib/i18n/index.ts` | English fallbacks for the `decomposition.*` keys |
| `src/lib/views/EpicDecompositionView.test.ts` | Render, conflict banner, read-back accept, agree & commit, disabled agree, drop marker |

### 2.5 `pm-web-e2e` — end-to-end

| File | Purpose |
|---|---|
| `tests/decomposition.spec.js` | Pair on the decomposition scenario, show the cut, edit → read-back → agree & commit, add a plan |

---

## 3. Phase 1 — Trace Matrix & Specification Alignment

| `UC-STA-6` capability | Specification / design reference | Implemented as |
|---|---|---|
| `decomposition.draft` | `state-catalogue/17-epic-decomposition-and-review.adoc#state-catalogue-decomposition` | `DecompositionState.DRAFT`; the mock's seeded cut |
| Surface disjointness | `model-work/08-epic-coordination.adoc#epic-surface-disjointness` | `DecompositionValidator.checkSurfaceDisjointness` (`RULE_SURFACE_OVERLAP`) |
| Acyclic dependency graph | `model-work/08-epic-coordination.adoc#decomposition-review` | `DecompositionValidator.dependencyCycle` (`RULE_DEPENDENCY_CYCLE`) |
| Workstream integrity | `model-work/08-epic-coordination.adoc#decomposition-review` | `DecompositionValidator.checkWorkstreamIntegrity` (`RULE_WORKSTREAM_INTEGRITY`) |
| `decomposition.review` (keep/edit/drop/add) | `design/shared/starting-work.adoc#decomposition` | CLI tree + per-plan buttons in `EpicDecompositionView` |
| `decomposition.instruct` / `decomposition.read-back` | `design/terminal-client/18-starting.adoc#decomposition` | `DecompositionInstructRequest`, `epic instruct`, the view's read-back section |
| `decomposition.commit` + launchable plans | `design/shared/starting-work.adoc#decomposition` | `DecompositionCommitResult`, `epic review --agree`, `epic.launchable` |

---

## 4. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` `DecompositionRecordsTest` (11 + 3 nested) | Pass |
| `pm-api` `DecompositionValidatorTest` (6) | Pass |
| `pm-api` `DecompositionCatalogueTest` (4) and `MessageCatalogueTest` (6) | Pass |
| `pm-mock-runtime` `DecompositionScenarioTest` (9) and `StandardScenariosTest` (2) | Pass |
| `pm-operator` `DecomposeCommandTest` (6) and `EpicCommandTest` (10) | Pass |
| `pm-web-app` Vitest `EpicDecompositionView.test.ts` (5) and the full suite (146) | Pass |
| `pm-web-app` ESLint and production build | Pass |
| `pm-web-e2e` `decomposition.spec.js` | Playwright suite (ran with browsers) |
| Full reactor `./mvnw verify -Ppre-commit` | Pass (see §6) |

Boundary testing covers the codec edge cases the plan asked for: non-object JSON, missing required fields,
unknown closed-list values, null constructor arguments, null/empty cuts, dropped and locked plans in the
overlap/cycle checks, and duplicate plan ids/workstream orders.

---

## 5. Domain Invariants

1. **Java 25 and records**: every contract is an immutable `record`; `var`, `final` and package-private types
   are used where the surrounding code does; no `Exception`/`RuntimeException` is caught or thrown in
   production code.
2. **No uncatalogued user-facing strings**: CLI and web strings come from the `decomposition` message area; the
   web app mirrors them in its English fallbacks. Remaining literals are structural markers (`-`/`+` diff
   prefixes, `✓`/`✗`).
3. **Bilingual catalogue**: `decomposition.{en,de}.properties` carry identical non-blank key sets, enforced by
   `DecompositionCatalogueTest` and `MessageCatalogueTest.shouldVerifyAreaIsolation`.
4. **Mock runtime completeness**: the scenario is registered in `StandardScenarios.ALL` and validated by
   `StandardScenariosTest`; the endpoints are exercised over the real Unix-socket runtime.
5. **Safe text rendering**: every server-provided string printed by the CLI passes through
   `OutputSafety.stripControlSequences`; every verb verifies the operator gate first (PM-SEC-3).
6. **Responsive layout**: the view collapses to a single column and its modal goes full width at 480 px.

---

## 6. Build & Quality Evidence

- `./mvnw verify -Ppre-commit` builds the full reactor — OpenRewrite, licence headers, Checkstyle, Enforcer,
  Javadoc, the unit suites, the Vitest suite, ESLint, and the SPA build — with `BUILD SUCCESS` and a clean
  working tree after the rewrite pass was reviewed and committed.
- The Playwright suite for the web app is exercised with the browsers available on this machine.

### Known limitation (deliberate, simulated runtime only)

- The **Drop** per-plan action in the web view is a review-stage, client-side marker: the specification's
  endpoint surface (`check`, `instruct`, `commit`) carries no drop verb in this slice, and the server marks a
  plan `dropped` on a later run. The UI records the operator's intent and shows the `dropped` marker; the other
  per-plan action, **Edit**, and the whole **instruct → read-back → accept** cycle round-trip through the
  runtime.

---

## 7. Process Adherence

- **Personas adopted**: `persona-implementer` and `persona-module-tester` (records and boundary tests),
  `persona-security-expert` (operator gate, safe rendering), `persona-documenter` (catalogue and schema
  alignment), `persona-integration-tester` (mock runtime, process IT, Playwright), `persona-code-reviewer`
  (finding triage).
- **Workflow**: isolated worktree `pm-mcp-clients-uc-sta-6` on `feat/uc-sta-6-decomposition`; incremental
  per-module builds and tests; pre-commit gate before the PR; plan archived to `doc/agy/implemented/`.

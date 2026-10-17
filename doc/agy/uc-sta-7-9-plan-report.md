# Completion Report: UC-STA-7 & UC-STA-9 Git Plan Sources (Adopt PR & Capture Local Changes)

This report documents the implementation of the plan [`doc/agy/uc-sta-7-9-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-sta-7-9-plan.md), executed under the discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe of [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

Branch: `feat/uc-sta-7-9-git-sources` · Worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-sta-7-9`.

---

## 1. Executive Summary

The two remaining "Starting Work" git-source use cases were implemented end-to-end across the client and runtime modules:

- **UC-STA-7: Adopt a Pull Request** — a pull request is observed through the provider (open status, writable head branch, rate limit) and its title/body are screened; changed files are carried as untrusted unless the operator authored them; the pull request is then taken over by a plan in the **`adopt`** lane. A flagged or unscreened pull request needs an explicit acceptance before it is adopted.
- **UC-STA-9: Capture Local Changes** — the checkout's uncommitted changes are previewed against their branch with per-file line counts, the landing precondition `PM-WF-25` is answered (a landing that does not commit, such as `landing.files`, refuses the capture), and the confirmed changes are recorded as a snapshot commit for a plan in the **`land`** lane.

The implementation respects the repository invariants: strict dependency boundaries (`pm-operator`, `pm-mock-runtime`, and `pm-web-app` consume `pm-api` only), pure Java 25 records with Jackson-streaming (`JsonTree`) codecs, catalogue-only user-facing text in both English and German, and mock-first, deterministic flows with live SSE events (`source.pr_observed`, `source.local_changes_captured`).

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts, codecs & catalogue

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/api/source/git/SourceLane.java` | Closed enum of the two lanes (`adopt`, `land`) and their wire values. |
| `main/java/de/planmarshall/api/source/git/ScreeningOutcome.java` | The screening verdicts (`clean`, `flagged`, `unscreened`); `needsAcceptance()` marks the ones that need an explicit acceptance. |
| `main/java/de/planmarshall/api/source/git/ChangedFile.java` | One changed file: path, additions, deletions, untrusted flag, operator-authored flag. |
| `main/java/de/planmarshall/api/source/git/RateLimitStatus.java` | The provider's rate limit: limit, remaining, reset instant, in-force flag. |
| `main/java/de/planmarshall/api/source/git/PullRequestObservation.java` | What the provider answered: url, number, title, author, branch, state, writable head, changed files, screening, rate limit. |
| `main/java/de/planmarshall/api/source/git/PrAdoptRequest.java` | Adopt request: project, PR number/URL, screening-bypass reason. |
| `main/java/de/planmarshall/api/source/git/PrAdoptResult.java` | Adopt result: plan id, lane (`adopt`), branch, status. |
| `main/java/de/planmarshall/api/source/git/LocalChangesPreview.java` | The preview: branch, changed files, landing in force, `landing_commits`, `eligible`, refusal reason. |
| `main/java/de/planmarshall/api/source/git/LocalChangesCaptureRequest.java` | Capture request: project, confirmation flag. |
| `main/java/de/planmarshall/api/source/git/LocalChangesCaptureResult.java` | Capture result: plan id, lane (`land`), snapshot commit SHA, status. |
| `main/java/de/planmarshall/api/source/git/package-info.java` | Package documentation. |
| `main/resources/messages/git_sources.en.properties` | English catalogue of the `git_sources` area (observation, screening, adopt, preview, capture, lanes). |
| `main/resources/messages/git_sources.de.properties` | German catalogue, key-for-key identical. |
| `main/java/de/planmarshall/api/message/MessageCatalogue.java` | Registers `git_sources` as a supported area. |
| `test/java/de/planmarshall/api/source/git/GitSourceRecordsTest.java` | Round-trip, defaulting, clamping, and negative-parsing contracts for every record and enum. |
| `test/java/de/planmarshall/api/message/GitSourceCatalogueTest.java` | EN/DE key parity, specification-key presence, ICU formatting. |

### 2.2 `pm-mock-runtime` — scenario, handlers & SSE

| File | Purpose |
|---|---|
| `main/resources/scenarios/git_sources.json` | The `git_sources` scenario: status, three projects (one not ready), and the intake state. |
| `main/java/de/planmarshall/mock/StandardScenarios.java` | Registers `GIT_SOURCES = "git_sources"` in the standard list. |
| `main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Dynamic handlers for `POST .../sources/pr/observe`, `POST .../sources/pr/adopt`, `GET .../sources/local-changes/preview`, `POST .../sources/local-changes/capture`; the open/writable/rate-limit/screening refusals; the `landing.files` refusal; snapshot-commit derivation; SSE broadcasts `source.pr_observed` and `source.local_changes_captured`. |
| `test/java/de/planmarshall/mock/GitSourceScenarioTest.java` | Scenario loading, observation, adoption, flagged acceptance, the three unadoptable cases, the not-ready project, the eligible/ineligible preview, the confirmation gate, and the `landing.files` refusal. |

### 2.3 `pm-operator` — the console verb

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/operator/GitSourceCommand.java` | `git-source adopt-pr` and `git-source local-changes`; the shared `adoptPr` / `captureLocalChanges` flows that `start plan --adopt-pr` / `--local-changes` reuse; PM-SEC-3 confirmation gates; catalogue text; `OutputSafety` sanitization. |
| `main/java/de/planmarshall/operator/StartCommand.java` | Adds the `--adopt-pr`, `--local-changes`, `--yes`, `--no-input`, and `--accept-screening` options of `start plan`, delegating to the git-source flows. |
| `main/java/de/planmarshall/operator/PmOperator.java` | Registers `git-source` in the command tree. |
| `test/java/de/planmarshall/operator/GitSourceCommandTest.java` | Usage, clean adoption, flagged acceptance, closed/writable/rate-limit refusals, preview/capture, confirmation gate, `landing.files` refusal, and the `start plan` delegations. |
| `test/java/de/planmarshall/operator/PmOperatorProcessIT.java` | Adds the `git-source` and `start plan --local-changes` PM-SEC-3 terminal-refusal cases. |

### 2.4 `pm-web-app` — the web views

| File | Purpose |
|---|---|
| `src/lib/api/client.ts` | The observation/adopt/preview/capture types and `observePullRequest`, `adoptPullRequest`, `previewLocalChanges`, `captureLocalChanges`. |
| `src/lib/components/GitSourceIntakeModal.svelte` | The modal with its "Adopt a pull request" / "Local changes" tabs and its completion report. |
| `src/lib/components/PrAdoptPanel.svelte` | The PR reference input, the live observation card, the screening warning badge, and the adopt action. |
| `src/lib/components/LocalChangesDiffPreview.svelte` | The file-tree diff preview with line additions/deletions, the land-lane eligibility, and the capture confirmation. |
| `src/lib/stores/gitSource.ts` | The store that opens the modal for a project. |
| `src/lib/views/OverviewView.svelte` | The `Adopt a pull request` action in the project header. |
| `src/routes/+page.svelte` | Mounts the git-source modal. |
| `src/lib/i18n/index.ts` | Catalogue fallbacks for the new `git_sources.*` keys. |
| `src/lib/components/GitSourceIntakeModal.test.ts` | Vitest suite: closing, titling, clean adoption, flagged acceptance, local capture, and the `landing.files` refusal. |

### 2.5 `pm-web-e2e` — the browser suite

| File | Purpose |
|---|---|
| `tests/git-sources.spec.js` | Playwright suite: PR observation/adoption, flagged acceptance, local preview/capture, and the `PM-WF-25` refusal. |

---

## 3. Test & Verification Results

| Layer | Suite | Result |
|---|---|---|
| `pm-api` | `GitSourceRecordsTest` | 20 tests, 0 failures |
| `pm-api` | `GitSourceCatalogueTest` | 4 tests, 0 failures |
| `pm-mock-runtime` | `GitSourceScenarioTest` | 8 tests, 0 failures |
| `pm-operator` | `GitSourceCommandTest` | 11 tests, 0 failures |
| `pm-operator` (process) | `PmOperatorProcessIT` (incl. the `git-source` refusals) | green under `verify` |
| `pm-web-app` | full Vitest suite | 147 tests, 0 failures (30 files) |
| `pm-web-e2e` | `git-sources.spec.js` (Chromium) | 4 tests (run under `-Pe2e`) |

---

## 4. Domain Invariants

- **Java 25 / Lombok / packages** — records with compact constructors, `JsonTree` (Jackson-streaming) codecs only; no `jackson-databind`, no CDI/Quarkus. ✅
- **No uncatalogued user-facing literals** — every client string binds to a `git_sources.*` key; the operator prints via `MessageCatalogue`. ✅
- **Bilingual catalogue** — `git_sources.en.properties` and `git_sources.de.properties` carry identical key sets, asserted by `GitSourceCatalogueTest` and `MessageCatalogueTest`. ✅
- **Mock runtime completeness** — the `git_sources` scenario loads through `ScenarioRegistry`, and every documented branch (observe, adopt, screened/flagged/unscreened, rate limited, not open, non-writable head, preview, eligible/ineligible capture, confirmation) is served and asserted. ✅
- **Both clients, one state** — an observation and a capture are broadcast on the SSE stream (`source.pr_observed`, `source.local_changes_captured`) and the terminal and browser drive the same endpoints. ✅
- **Untrusted PR content** — every changed file of an external pull request is marked untrusted unless the operator authored it, and flagged/unscreened text needs an explicit acceptance before adoption (PM-SEC-3). ✅
- **`PM-WF-25` landing axis** — local changes are refused where the landing does not commit (`landing.files`), in the runtime, the CLI, the web panel, and the E2E suite. ✅
- **`pm-operator` security** — the verbs run behind `OperatorGate`; ids and paths are stripped of control sequences via `OutputSafety`. ✅
- **Responsive web layout** — the modal and panels collapse to full-screen below 480px with 44px touch targets. ✅

---

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
...
pm-mcp-clients ... SUCCESS
pm-api ........... SUCCESS
pm-exec .......... SUCCESS
pm-relay ......... SUCCESS
pm-operator ...... SUCCESS
pm-mock-runtime .. SUCCESS
pm-web-app ....... SUCCESS
pm-web-e2e ....... SUCCESS
BUILD SUCCESS   (07:33 min)
```

`git status` was clean of build artifacts and reformatted sources after the build; the OpenRewrite/Checkstyle/Enforcer/Javadoc gates all passed with zero regressions.

---

## 6. Process & Persona Adherence

- `plan-marshall-persona-implementer` drove the Java 25 records, the mock handlers, the CLI verb, and the Svelte components.
- `plan-marshall-persona-module-tester` drove the proactive boundary tests of the codecs (`GitSourceRecordsTest`).
- `plan-marshall-persona-security-expert` drove the untrusted-content screening flags and the PM-SEC-3 confirmation gates.
- `plan-marshall-persona-documenter` drove the schema alignment and the catalogue parity.
- `plan-marshall-persona-integration-tester` drove the mock routes, the operator process IT, and the Playwright E2E suite.
- `plan-marshall-persona-code-reviewer` drove the review-window resolution under the runbook's 100% findings bar.

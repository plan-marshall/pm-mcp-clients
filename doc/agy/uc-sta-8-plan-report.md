# Completion Report: UC-STA-8 Bring in a Lesson Learned (Lesson Plan Sources)

This report documents the implementation of the plan [`doc/agy/uc-sta-8-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/uc-sta-8-plan.md), executed under the discipline of [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) and the technical recipe of [`doc/meta/implement_use-cases.md`](file:///Users/oliver/git/pm-mcp-clients/doc/meta/implement_use-cases.md).

Branch: `feat/uc-sta-8-lesson-source` · Worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-uc-sta-8`.

---

## 1. Executive Summary

**UC-STA-8: Bring in a Lesson Learned** was implemented end-to-end across the client and simulated-runtime modules. A plan can now start from a lesson recorded by a former plan, after its premise (the files and symbols the lesson cites) is verified against the current repository tree:

- **`source-lesson.choose`** — the project's unclaimed lessons are listed newest first; lessons already claimed by a plan and retired lessons are never offered.
- **`source-lesson.resolve`** — the premise of a chosen lesson is checked against the repository tree; the result is three-valued (`holds`, `broken`, `unavailable`).
- **`source-lesson.premise`** — a broken premise presents the missing files and symbols, and offers the three-way recovery: *use it anyway* (the evidence is carried into the plan), *retire* (the lesson is retired in the store), or *remove* (the lesson is left untouched).
- **`source-lesson.store`** — the lesson is claimed for the plan; the claim and retirement are broadcast on the event stream (`source.lesson_claimed`, `source.lesson_retired`).

The implementation respects the repository invariants: strict dependency boundaries (`pm-operator`, `pm-mock-runtime`, and `pm-web-app` consume `pm-api` only), pure Java 25 records with Jackson-streaming (`JsonTree`) codecs, catalogue-only user-facing text in both English and German (`lesson_source.{en,de}.properties`), and a mock-first, deterministic scenario with live SSE events.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts, codecs & catalogue

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/api/source/lesson/PremiseEvidenceKind.java` | Closed enum of what a lesson's premise cites: `file`, `symbol`. |
| `main/java/de/planmarshall/api/source/lesson/LessonPremiseEvidence.java` | One cited file or symbol that could not be found: kind, name, expected path, last verified commit. |
| `main/java/de/planmarshall/api/source/lesson/LessonPremiseCheckResult.java` | The outcome of a premise check: status (`holds`/`broken`/`unavailable`), evidence, cause. |
| `main/java/de/planmarshall/api/source/lesson/UnclaimedLessonSummary.java` | One unclaimed lesson as the choice offers it: id, title, summary, recorded time, citing plan, cited symbols. |
| `main/java/de/planmarshall/api/source/lesson/LessonClaimRequest.java` | Claims a lesson for a plan: project, lesson, use-anyway override. |
| `main/java/de/planmarshall/api/source/lesson/LessonClaimResult.java` | The claim outcome: `claimed` with the plan, or `refused`. |
| `main/java/de/planmarshall/api/source/lesson/LessonRetirementNotice.java` | The retirement outcome: lesson, retired flag, reason. |
| `main/java/de/planmarshall/api/source/lesson/package-info.java` | Package documentation. |
| `main/resources/messages/lesson_source.en.properties` | English catalogue of the `lesson_source` area (choice, premise, evidence, recovery, claim, retirement, web modal). |
| `main/resources/messages/lesson_source.de.properties` | German catalogue, key-for-key identical. |
| `main/java/de/planmarshall/api/message/MessageCatalogue.java` | Registers `lesson_source` as a supported area. |
| `test/java/de/planmarshall/api/source/lesson/LessonSourceRecordsTest.java` | Round-trip, defaulting, and negative-parsing contracts for every record. |
| `test/java/de/planmarshall/api/message/LessonSourceCatalogueTest.java` | EN/DE key parity, specification-key presence, ICU formatting. |

### 2.2 `pm-mock-runtime` — scenario, handlers & SSE

| File | Purpose |
|---|---|
| `main/resources/scenarios/lesson_source.json` | The `lesson_source` scenario: status, three projects (one not ready), and the choose/premise/stored states. |
| `main/java/de/planmarshall/mock/StandardScenarios.java` | Registers `LESSON_SOURCE = "lesson_source"` in the standard list. |
| `main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Dynamic handlers for `GET .../sources/lessons`, `POST .../sources/lessons/{id}/{premise-check,claim,retire}`; the readiness gate; per-project seeds (holding, broken, already-claimed, retired); SSE broadcasts; `handleLessonSourceRoutes` wired into the dispatch chain. |
| `test/java/de/planmarshall/mock/LessonSourceScenarioTest.java` | Scenario loading, newest-first listing, holding/broken premise, claim with and without override, refusal of an owned lesson, retirement, unknown lesson, not-ready project. |

### 2.3 `pm-operator` — the console verbs

| File | Purpose |
|---|---|
| `main/java/de/planmarshall/operator/LessonSourceCommand.java` | `lesson check <id>` and the shared premise/claim/retire helpers plus the interactive three-way recovery (`adoptForPlan`) used by `start plan --lesson <id>`. |
| `main/java/de/planmarshall/operator/StartCommand.java` | Adds `start plan --lesson <id> [--use-anyway]`, adopting the lesson before the start. |
| `main/java/de/planmarshall/operator/PmOperator.java` | Registers `lesson` in the command tree. |
| `test/java/de/planmarshall/operator/LessonSourceCommandTest.java` | Usage, id requirement, holding/broken check, claim-then-start, use-anyway override, three-way prompt (use anyway / retire / remove), request sequencing and bodies. |
| `test/java/de/planmarshall/operator/PmOperatorProcessIT.java` | Adds the `lesson` and `start plan --lesson` PM-SEC-3 terminal-refusal cases. |

### 2.4 `pm-web-app` — the web views

| File | Purpose |
|---|---|
| `src/lib/api/client.ts` | Lesson types and `getUnclaimedLessons`, `checkLessonPremise`, `claimLesson`, `retireLesson`. |
| `src/lib/components/LessonSourceModal.svelte` | The modal that lists unclaimed lessons, checks the premise, offers the recovery, claims, and starts the plan. |
| `src/lib/components/LessonPicker.svelte` | The list of available lessons with recorded date, citing plan, and cited-item count. |
| `src/lib/components/PremiseEvidenceView.svelte` | The broken-premise warning with the missing evidence and the *use anyway* / *retire* / *choose another* actions. |
| `src/lib/stores/lessonSource.ts` | The store that opens the lesson modal for a project. |
| `src/lib/components/StartWorkModal.svelte` | Adds the “Start from a lesson” entry point. |
| `src/routes/+page.svelte` | Mounts the lesson modal. |
| `src/lib/i18n/index.ts` | Catalogue fallbacks for the new `lesson_source.*` keys. |
| `src/lib/components/LessonSourceModal.test.ts` | Vitest suite: closed state, listing, holding claim+start, broken evidence, use-anyway override, retirement, failure. |

### 2.5 `pm-web-e2e` — the browser suite

| File | Purpose |
|---|---|
| `tests/lesson-source.spec.js` | Playwright suite: newest-first listing, a holding lesson, a broken premise carried in with *use anyway*, retirement, and *choose another*. |

---

## 3. Test & Verification Results

| Layer | Suite | Result |
|---|---|---|
| `pm-api` | `LessonSourceRecordsTest`, `LessonSourceCatalogueTest` | 19 tests, 0 failures |
| `pm-mock-runtime` | `LessonSourceScenarioTest`, `StandardScenariosTest` | 12 tests, 0 failures |
| `pm-operator` | `LessonSourceCommandTest` | 9 tests, 0 failures |
| `pm-operator` (process) | `PmOperatorProcessIT` (incl. `lesson` and `start plan --lesson` refusals) | green under `verify` |
| `pm-web-app` | full Vitest suite | 148 tests, 0 failures (30 files) |
| `pm-web-e2e` | `lesson-source.spec.js` (Chromium) | 5 tests, 0 failures |

---

## 4. Domain Invariants

- **Java 25 / Lombok / packages** — immutable records with compact constructors and `JsonTree` (Jackson streaming) codecs only; no `jackson-databind`, no CDI/Quarkus. ✅
- **No uncatalogued user-facing literals** — every client string binds to a `lesson_source.*` key; the operator prints via `MessageCatalogue`; untrusted runtime text is stripped of control sequences via `OutputSafety`. ✅
- **Bilingual catalogue** — `lesson_source.en.properties` and `lesson_source.de.properties` carry identical key sets, asserted by `LessonSourceCatalogueTest` and `MessageCatalogueTest` area isolation. ✅
- **Mock runtime completeness** — the `lesson_source` scenario loads through `ScenarioRegistry`, and every documented branch (choose, resolve, broken premise, three-way recovery, claim, retirement) is served and asserted. ✅
- **Both clients, one state** — a claim or retirement is broadcast on the SSE stream (`source.lesson_claimed`, `source.lesson_retired`) and the terminal and browser drive the same endpoints. ✅
- **`pm-operator` security** — both verbs run behind `OperatorGate` (PM-SEC-3); ids and evidence are sanitized via `OutputSafety`. ✅
- **Responsive web layout** — the modal, picker, and evidence view collapse to full-screen below 480px with 44px touch targets. ✅

---

## 5. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
...
pm-api .......... SUCCESS
pm-exec ......... SUCCESS
pm-relay ........ SUCCESS
pm-operator ..... SUCCESS
pm-mock-runtime . SUCCESS
pm-web-app ...... SUCCESS
pm-web-e2e ...... SUCCESS
BUILD SUCCESS   (08:17 min)
```

The OpenRewrite / Checkstyle / Enforcer / Javadoc gates all passed with zero regressions. OpenRewrite normalized the record-header formatting of `pm-mock-runtime` (a module whose pre-existing sources had not been passed through the current recipe); those reformats are committed alongside the feature. `git status` was clean of build artifacts after the build.

---

## 6. Notes & Follow-ups

- The three-way recovery lives in two places by design: the interactive prompt in `LessonSourceCommand.adoptForPlan` (CLI) and `PremiseEvidenceView` (web). Both drive the same `claim`/`retire` endpoints.
- Lesson premise evaluation is simulated by the two scenario seeds (a holding `les-0007` and a broken `les-0009`); the real runtime performs the AST/file check.
- Per the runbook, this report is **not** moved to `doc/agy/verified/`; post-landing verification is the orchestrator session's responsibility.

# Completion Report: `operator-enrolment-verbs` — Operator Enrolment Verbs & Store Integrity

Companion report of the implementation plan [`doc/agy/implemented/operator-enrolment-verbs-plan.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/implemented/operator-enrolment-verbs-plan.md),
executed under [`doc/agy/runbook.md`](file:///Users/oliver/git/pm-mcp-clients/doc/agy/runbook.md) on branch
`feat/operator-enrolment-verbs`.

---

## 1. Executive Summary

The work package delivers the operator verbs of project enrolment and store integrity — Roadmap Milestone 3 —
end to end across the three modules it owns:

- **Contracts (`pm-api`)**: seven immutable records in `de.planmarshall.api.resource` (`EnrolRequest`,
  `EnrolResult`, `UnenrolRequest`, `UnenrolResult`, `StoreDigestComparison`, `AcceptEditRequest`,
  `AcceptEditResult`) with Jackson-`core` streaming codecs, and a bilingual message area
  `enrolment.{en,de}.properties` registered in `MessageCatalogue`.
- **Mock runtime (`pm-mock-runtime`)**: a stateful `scenarios/enrolment.json` and four dynamic endpoints —
  `POST /api/v1/enrol` (and the project-scoped alias `/api/v1/projects/{id}/enrol`),
  `POST /api/v1/projects/{id}/unenrol`, `GET /api/v1/projects/{id}/stores/digest`, and
  `POST /api/v1/projects/{id}/stores/accept-edit` — with the SSE broadcasts `project.enrolled`,
  `project.unenrolled`, and `store.edit_accepted`.
- **Operator CLI (`pm-operator`)**: the operator verbs `enrol [--pilot] [path]`, `unenrol <project_id>`, and
  `accept-edit <store-path>` registered in `PmOperator`, all behind the PM-SEC-3 operator gate. `accept-edit`
  renders the store path and, side by side, the recorded and the current size and SHA-256 before it asks for the
  operator's confirmation; it restores nothing and shows no diff, and it **never sends an acceptance the operator
  did not give** — neither on a declined prompt nor in `--no-input` mode. The acceptance names the SHA-256 the
  operator saw, and the runtime refuses any other digest (`confirmation_mismatch`).

The acceptance gate is what PM-SEC-3 and the Store Digest Verification section require: the confirmation is the
operator's, it is interactive, and the recorded/current comparison is displayed before the confirmation, not
after.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts, streaming codecs, bilingual catalogue

| File | Purpose |
|---|---|
| `src/main/java/de/planmarshall/api/resource/EnrolRequest.java` | Checkout path + `pilot` flag; `toMap`/`toJson`/`fromMap`/`fromJson` |
| `.../resource/EnrolResult.java` | Status, project id, pilot flag |
| `.../resource/UnenrolRequest.java` | Project id |
| `.../resource/UnenrolResult.java` | Status (`unenrolled`) |
| `.../resource/StoreDigestComparison.java` | Store path, recorded/current size and SHA-256, derived `mismatch`, detail; the side-by-side input |
| `.../resource/AcceptEditRequest.java` | Store path + the confirmed SHA-256 |
| `.../resource/AcceptEditResult.java` | Status (`accepted`) + the new SHA-256 |
| `src/main/resources/messages/enrolment.en.properties` | English catalogue (28 keys) |
| `src/main/resources/messages/enrolment.de.properties` | German catalogue (28 keys) |
| `src/main/java/de/planmarshall/api/message/MessageCatalogue.java` | Registered the `enrolment` area in `SUPPORTED_AREAS` |
| `src/test/java/de/planmarshall/api/resource/EnrolRecordsTest.java` | Round-trips, fallback keys, negative parsing, mismatch derivation |
| `src/test/java/de/planmarshall/api/message/EnrolCatalogueTest.java` | Bilingual key parity, required keys, parameterised formatting |

### 2.2 `pm-mock-runtime` — scenario and dynamic endpoints

| File | Purpose |
|---|---|
| `src/main/resources/scenarios/enrolment.json` | Stateful fixture: status and project explorer routes, a start state |
| `src/main/java/de/planmarshall/mock/StandardScenarios.java` | Added `ENROLMENT` to the constants and `ALL` |
| `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | `handleEnrolmentRoutes`, seeded stores (a mismatching store for `pm-mcp-clients`, a matching one for `plan-marshall`), the enrol/unenrol/digest/accept-edit handlers, and the three SSE broadcasts |
| `src/test/java/de/planmarshall/mock/EnrolScenarioTest.java` | Load, digest mismatch/match, accept-edit refuse/accept/re-baseline, enrol top-level and alias, unenrol, `store_not_found`, malformed body |

### 2.3 `pm-operator` — CLI verbs

| File | Purpose |
|---|---|
| `src/main/java/de/planmarshall/operator/EnrolCommand.java` | Top-level `enrol [--pilot] [path]`, with root-conflict and store-root-mismatch reporting |
| `.../operator/UnenrolCommand.java` | Top-level `unenrol <project_id>` with the `scope_active` refusal |
| `.../operator/AcceptEditCommand.java` | Top-level `accept-edit <store-path>`, side-by-side table, confirmation gate, `--force`/`--no-input` |
| `.../operator/PmOperator.java` | Registered the three verbs in `subcommands` |
| `src/test/java/de/planmarshall/operator/EnrolCommandTest.java` | Enrol path/pilot, refusals, output sanitisation; unenrol success/confirm/refusals |
| `.../operator/AcceptEditCommandTest.java` | Table contents, request shapes, declined-confirmation silence, `--no-input`, matching store, stale confirmation, derived project id, sanitisation |
| `.../operator/PmOperatorProcessIT.java` | Three process-level PM-SEC-3 gates for `enrol`, `unenrol`, `accept-edit` |

---

## 3. Phase 1 — Trace Matrix & Specification Alignment

| Requirement / specification | Capability | Implemented as |
|---|---|---|
| `PM-EXEC-2` Operator Enrolment & Verify-Not-Resolve | Enrolment creation/modification only by an operator act | `EnrolCommand`/`UnenrolCommand` are `@OperatorVerb` and call `OperatorGate`; `POST /api/v1/enrol`, `…/unenrol` |
| `PM-EXEC-2` | Pilot flag recorded and reported | `EnrolRequest.pilot` → `EnrolResult.pilot`; `enrol --pilot` |
| `PM-SEC-3` Operator Channel Isolation | Interactive terminal + no agent signal before any act | `context.gate().verify()` first in all three verbs; `PmOperatorProcessIT` proves exit code 4 without a terminal |
| `PM-SEC-3` | An act that cannot be undone asks for interactive confirmation | `accept-edit` shows the comparison, then `Confirm edit acceptance? [y/N]`, defaulting to No |
| Store Schemas § Store Digest Verification | Accept-edit shows path and recorded/current size and SHA-256, restores nothing, re-baselines the digest | `StoreDigestComparison`; `store.edit_accepted`; `AcceptEditResult.newSha256` |
| `PM-IMPL-7` Data Store Invariants | The repair act runs through the runtime; the recorded digest is the one the operator confirmed | Request carries `confirmed_sha256`; the runtime re-baselines only on a match |
| Command Tree § Enrolment, Lifecycle Gate & Release | `pm-operator enrol [--pilot]`, unenrol, accept-edit | `EnrolCommand`, `UnenrolCommand`, `AcceptEditCommand` |

`accept-edit` is the JSON form documented as `POST /api/v1/projects/{project_id}/stores/accept-edit`; `enrol`
also accepts the project-scoped alias `/api/v1/projects/{id}/enrol` that the plan names as the alternative.

---

## 4. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` `EnrolRecordsTest` (7) | Pass |
| `pm-api` `EnrolCatalogueTest` (3) and `MessageCatalogueTest` (6) | Pass |
| `pm-mock-runtime` `EnrolScenarioTest` (7), `StandardScenariosTest` (2), `ProjectsScenarioTest` (5) | Pass |
| `pm-operator` `EnrolCommandTest` (9) and `AcceptEditCommandTest` (10) | Pass |
| `pm-operator` `PmOperatorProcessIT` (3 new PM-SEC-3 gates) | Pass under `-Pintegration-tests` |
| Full reactor `./mvnw verify -Ppre-commit` | Pass (see §6) |

The negative paths are covered explicitly: a declined confirmation and `--no-input` both assert that **only the
digest read was sent** (`server.requests().size() == 1`), a mismatched confirmation is refused, a matching store
sends nothing, and a missing project or store is reported rather than sent.

---

## 5. Domain Invariants

1. **Java 25 and records**: every contract is an immutable `record`; `var`, `final` and package-private types are
   used where the surrounding code does; no `Exception`/`RuntimeException` is caught or thrown in production code
   (`JsonFormatException` remains the JSON codec's checked exception; constructor guards throw
   `IllegalArgumentException`).
2. **No uncatalogued user-facing strings**: the enrolment, unenrol and accept-edit texts of the CLI come from the
   `enrolment` message area; remaining literals are structural (column separators, the `--project` misuse note).
3. **Bilingual catalogue**: `enrolment.{en,de}.properties` carry identical non-blank key sets, enforced by
   `EnrolCatalogueTest` and `MessageCatalogueTest.shouldVerifyAreaIsolation`.
4. **Mock runtime completeness**: `enrolment` is registered in `StandardScenarios.ALL` and validated by
   `StandardScenariosTest`; every endpoint is exercised over the real Unix-socket runtime.
5. **Safe terminal rendering**: every server-provided string (store path, SHA-256, mismatch detail, refusal
   reason) passes through `OutputSafety.stripControlSequences`; every verb verifies the operator gate first
   (PM-SEC-3).
6. **Spelling normalisation**: the plan lists the broadcast `project.enroled`; the implementation emits the
   correctly-spelled `project.enrolled`, matching the existing `ProjectEnrolledEvent.EVENT_NAME` contract, so the
   event streams stay consistent.

---

## 6. Build & Quality Evidence

- `./mvnw verify -Ppre-commit` builds the full reactor — OpenRewrite, licence headers, Checkstyle, Enforcer,
  Javadoc, and the unit suites — with `BUILD SUCCESS` and a clean working tree after the rewrite pass was
  reviewed and committed.
- The three-module compile and test runs (`pm-api`, `pm-mock-runtime`, `pm-operator`) are green locally before
  the pre-commit gate.

### Deliberate scope note

- `accept-edit` takes the store path plus an optional `--project`; when `--project` is omitted the leading
  segment of `<store-path>` (`<project_id>/<store>`) names the project. This is the minimal way to address the
  project-scoped digest/accept-edit endpoints the plan fixes while keeping the plan's `<store-path>` verb shape.
- The accept-edit verb addresses the workspace store of a project in this slice (enrolment integrity and the
  workspace/plan/epic/lesson selectors of the full specification are separate work packages).

---

## 7. Process Adherence

- **Personas adopted**: `persona-implementer` (records and commands), `persona-module-tester` (AAA unit suites,
  boundary and negative cases), `persona-security-expert` (PM-SEC-3 gate, confirmed-digest binding,
  sanitisation), `persona-integration-tester` (mock runtime endpoints, `PmOperatorProcessIT`), `persona-documenter`
  (roadmap status, catalogue, report), `persona-code-reviewer` (finding triage).
- **Workflow**: isolated worktree `pm-mcp-clients-operator-enrolment-verbs` on `feat/operator-enrolment-verbs`;
  incremental per-module builds and tests; the pre-commit gate before the PR; the plan archived to
  `doc/agy/implemented/` and the work package marked `done` in `doc/plans/README.adoc`.

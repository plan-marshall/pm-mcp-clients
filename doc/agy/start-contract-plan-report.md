# Completion Report: `start-contract` (Starting Work: Contract and Mock Runtime)

Companion to the execution plan [`start-contract-plan.md`](implemented/start-contract-plan.md),
worked under [`doc/agy/runbook.md`](runbook.md).
Branch: `feat/start-contract`; worktree: `/Users/oliver/git/worktrees/pm-mcp-clients-start-contract`.

---

## 1. Executive Summary

This work package completes the client-side contract of *starting work* (`pm-api`, package
`de.planmarshall.api.start`) and backs every case the work package names with scenarios and live
routes in `pm-mock-runtime` (UC-STA-1 to UC-STA-5, UC-STA-7 to UC-STA-9, and the start chat of
UC-CTL-1). An earlier package (`uc-sta`, `uc-sta-7-9`, `uc-sta-8`) had established the first source
records; this package delivers the entry as an addressable resource, the confined file picker, the
closed source/screening catalogues, the state representations on the operator-interaction scheme, the
start chat with its transcript lifecycle, and the follow-up plan of ended work.

Concretely, the package delivers:

- **The entry, addressed at the top level by its id.** `POST /api/v1/projects/{project_id}/entries`
  is the only request that names the project; from then on the entry is read at
  `GET /api/v1/entries/{entry_id}` and its events at `/api/v1/entries/{entry_id}/events`, exactly as
  plans and epics are. An entry never expires: a second read returns it unchanged and no sweep
  removes it.
- **Closed catalogues with fail-closed parsing.** `EntryKind`, `SourceKind`, `SourceState`,
  `SourceTrust`, `ScreeningVerdict`, `SourceFailureCause`, `UnscreenedDisposition`,
  `StartRefusalReason`, `AdoptionFailureReason`, `ConfinedRoot`, `StartTranscriptState`, and
  `StartState`, each with a wire value, a tolerant `fromWireOrNull`, a fail-closed `fromWire`, and a
  verbatim catalogue test.
- **Source errors and screening.** `SourceFailure` carries a closed cause; `rate_limited` carries
  `resets_at` as an **epoch second** and is never retried by client or server on its own; `flagged`
  sources carry their `ScreeningSpan`s; `accept_untrusted` records the operator's reason and keeps
  the untrusted mark; a pull request's changed files stay untrusted unless the operator authored
  them, enforced on construction and on the wire.
- **The confined file picker.** `GET /api/v1/projects/{project_id}/files?root=&path=` serves a
  relative-path listing confined to the checkout and the home directory, with a deterministic,
  total path guard (`ConfinedPath`) that refuses an absolute path, a drive prefix, a NUL, a
  traversal, and any real path that leaves its root; the home root is loopback-only.
- **The state representations on the operator-interaction scheme.** `StartStateResource` composes an
  `OperatorStateResource` (scope = the entry, the 11 start states, facts, items, links) with the
  start's own members (entry, ready, chat, refusal, adoption failure, recipes). The filtering rules
  are derived, not prose: `start.sources` offers only the kinds that combine with the list,
  `source-documents.mode` is asked only while the machine's mode is `ask` (copy preselected),
  `start.sources` withholds the local changes under `landing.files` and names the project's setting,
  an epic's ready step shows `epic.auto_start` in force with its origin and never asks the
  `parallelization_scope`, and a recipe's plan id is derived (`recipe-{key}-{yyyy-MM-dd-HH-mm-ss}`)
  and not editable.
- **The start chat and the follow-up plan.** `StartChatResource` carries the routed role (`refine`
  for a plan, `decompose` for an epic, absent for a recipe), availability with its reason, the
  transcript, and the one proposal; a create ends an open chat before the transcript moves, an
  abandon deletes it, and `FollowUpProposalResource` carries the follow-up plan of a chat about
  ended work.
- **Sixteen `start-<case>` scenarios** driven by a new `start_case` scenario fixture, each asserted
  through `StartScenarioTest`/`StartContractTest`.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — contracts (`de.planmarshall.api.start`)

New types (all immutable records or closed enums, with `jackson-core` tree codecs plus a streaming
`writeTo(JsonGenerator)`/`readFrom(JsonParser)` pair; no `jackson-databind`):

| File | Role |
|---|---|
| `EntryKind` | The closed list of the kinds of work a start opens (`plan`, `epic`) |
| `EntryResource` | The entry: kind, project, state, title, proposed id, sources, kind offers, continue, chat, recipe, adoptable epics, adoption failure |
| `EntryOpenRequest` | The one request that opens an entry under its project |
| `SourceKind` | Extended: external, documents, epic-offered, and the combination rules (`offered`, `onlyDescriptionsAndDocuments`) |
| `SourceState` | The closed list of a source's states (`checking` … `failed`), with `settled()`/`failed()` |
| `SourceTrust` | `trusted` / `untrusted` |
| `ScreeningVerdict` | `clean` / `flagged` / `unscreened` |
| `SourceFailureCause` | The closed list of the 19 failure causes, with `retryable()` and `namesReset()` |
| `SourceFailure` | A cause with its detail; `rate_limited` carries `resets_at` as an epoch second |
| `ScreeningSpan` | One flagged span (offsets, quoted text, reason) |
| `UnscreenedDisposition` | `try_again` / `remove` / `accept_untrusted` (`recordsReason()`) |
| `SourceResource` | The source: kind, reference, state, trust, verdict, failure, spans, disposition, changed files, digest |
| `SourceKindOffer` | One offered kind, with the message key and arguments of its refusal |
| `StartState` | The closed list of the 11 operator states of a start |
| `StartStateResource` | The entry's state on the operator-interaction scheme, with the link derivation |
| `StartReadyResource` | The ready step: title, derived id, id editability, recipe and parameters, landing (never asked), epic values in force with origin |
| `StartRefusal` / `StartRefusalReason` | The setup gate's refusal, with what is missing and the one fixing action |
| `AdoptableEpic` / `AdoptionFailure` / `AdoptionFailureReason` | Adoption offers and their closed failures |
| `StartChatResource` / `StartTranscriptState` | The start chat and the transcript's lifecycle |
| `FollowUpProposalResource` | The follow-up plan of a chat about ended work |
| `ConfinedRoot` / `ConfinedFile` / `ConfinedFileListResource` | The confined file picker's records |
| `ConfinedPath` | The deterministic, total path-confinement guard |
| `RecipePlanId` | The derivation and recognition of `recipe-{recipe_key}-{yyyy-mm-dd-hh-mm-ss}` |
| `StartJson` | Package-private codec helpers (strict array/object reading, unknown-field refusal) |

Modified: `SourceKind` (combination rules and tolerant/closed resolution), `package-info.java`
(the whole package's contract and its specification references).

### 2.2 `pm-api` — tests and catalogue

| File | Coverage |
|---|---|
| `StartContractTest.java` (new) | Round-trip and streaming codecs of every new record; the closed catalogues verbatim; the failure semantics (a rate limit names its reset as an epoch second and never retries by itself, `no_local_changes` offers no retry, an accepted unscreened source keeps the untrusted mark); the source-kind combination rules; the confinement guard (absolute, drive, NUL, traversal, containment); the recipe plan id; the state-resource link derivation; negative parsing |
| `StartRecordsTest.java` | The pre-existing source records still round-trip |
| `StartCatalogueTest.java` | Extended with the four new message keys |
| `start.en/de.properties` | `start.chat.role_unqualified`, `start.sources.no_local_changes`, `start.sources.no_lessons`, `start.sources.epic_kind` (bilingual) |

### 2.3 `pm-mock-runtime` — scenarios and route handlers

| File | Change |
|---|---|
| `scenarios/start*.json` | `start.json` reworked to carry a `start_case` fixture and an operator-interaction state representation; sixteen new `start-<case>` scenarios (`start-sources`, `start-documents`, `start-issue-failed`, `start-unscreened`, `start-lesson`, `start-rate-limited`, `start-local-changes`, `start-pull-request`, `start-epic`, `start-epic-adopt`, `start-refused`, `start-recipe`, `start-entry-continue`, `start-chat`, `start-follow-up`) |
| `StandardScenarios.java` | The constants of every new scenario, registered in `ALL` |
| `scenario/Scenario.java`, `ScenarioLoader.java` | An optional `start_case` fixture block |
| `runtime/StartRegistry.java` (new) | The live starts of the mock runtime: entries, sources, chats, recipes, ended-work chats, the confined file listing, and the act handling, seeded from the scenario fixture and derived through the `pm-api` records |
| `runtime/MockRuntimeEngine.java` | The top-level entry routes, the project's `entries` and `files` routes, the entry chat routes, and the ended-work chat and follow-up accept; the recipe listing now consults the start registry |
| `StartScenarioTest.java` | The pre-existing legacy start coverage kept, plus seven new suites: the opened entry, the sources and their states, ready/recipes/refusals, the start chat, the follow-up plan, and the file picker (33 tests) |
| `StartContractTest.java` (new) | The contract-level view over the mock runtime: the operator representation decodes with the `pm-api` codec, the invariants (rate limit never auto-retried, untrusted pull-request files, no expiry, local changes under `landing.files`), chat and follow-up, the codecs and the refusals |

---

## 3. Test & Verification Results

| Suite | Result |
|---|---|
| `pm-api` unit tests (`./mvnw -pl pm-api test`) | **610 tests, 0 failures** (incl. `StartContractTest`, `StartRecordsTest`, `StartCatalogueTest`) |
| `pm-mock-runtime` unit tests (`./mvnw -pl pm-mock-runtime test`) | **298 tests, 0 failures** (incl. `StartScenarioTest`, `StartContractTest`) |
| `./mvnw verify -Ppre-commit` | see §5 |

Every new scenario loads through `ScenarioLoader` (with its dangling-link, operator-state, and event
validation) and is served by the mock runtime; a test per scenario asserts the representation a client
receives. `StandardScenariosTest` proves `ALL` lists exactly the declared constants.

---

## 4. Domain Invariants

- **Java 25, immutable records, closed enums** — every new type is a record or a closed enum;
  `pm-api` still uses only `jackson-core` through `JsonTree`, proven by the `jackson-core-only`
  enforcer rule.
- **Closed lists fail closed** — an unknown entry kind, source kind, source state, trust, verdict,
  failure cause, disposition, refusal reason, adoption failure reason, root, transcript state, or
  start state is refused (`fromWire` throws, `fromWireOrNull` returns `null` and the codec refuses).
- **A rate-limited source is never retried on its own** — `SourceFailure` enforces that
  `rate_limited` carries `resets_at` and no other cause does; the mock runtime never retries it, and
  tests read it repeatedly to prove the state is unchanged.
- **A pull request's changed files are untrusted unless the operator authored them** — enforced in
  `SourceResource`'s constructor and asserted on the wire.
- **Fail-closed screening** — an unscreened source is untrusted, an accepted unscreened or flagged
  source keeps the untrusted mark and records a reason, and no model reads an unscreened source.
- **The file picker is confined** — a relative path is resolved against its root; an absolute path, a
  drive prefix, a NUL, a traversal, or a real path that leaves the root is refused; the home root is
  loopback-only.
- **An entry never expires** — no time limit, no sweep; a second read returns it unchanged.
- **Mock runtime completeness** — the new scenarios load and are served, and a test per scenario
  asserts the representation a client receives.
- **No uncatalogued user-facing text** — four new keys were added to both the English and German
  packs (`StartCatalogueTest` proves key parity and values).

---

## 5. Build & Quality Evidence

```
$ ./mvnw verify -Ppre-commit
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  8.655 s]
[INFO] pm-api ............................................. SUCCESS [01:19 min]
[INFO] pm-exec ............................................ SUCCESS [ 13.601 s]
[INFO] pm-relay ........................................... SUCCESS [ 14.445 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 45.453 s]
[INFO] pm-operator ........................................ SUCCESS [01:06 min]
[INFO] pm-web-app ......................................... SUCCESS [ 44.022 s]
[INFO] pm-web-e2e ........................................ SUCCESS [ 13.899 s]
[INFO] BUILD SUCCESS
```

No OpenRewrite, license-header, or import-order rewrite changed the committed state, so the verified
state is the committed state. On the pull request the Maven build (JDK 25 and 26), the
integration-tests suite, the `cuioss-review-bot` review, and the SonarCloud analysis are expected to
pass; all findings are triaged through `persona-code-reviewer`.

---

## 6. Notes, Deviations, and Follow-ups

- **A new optional scenario fixture (`start_case`).** The mock runtime needs the entry, its sources,
  the project's landing, the documents mode, and the ended work to seed a live start. Rather than
  overload the static `routes`, the scenario format gained an optional top-level `start_case` object,
  parsed by `ScenarioLoader` and never validated against a closed schema (the runtime decodes it
  through the `pm-api` records instead, so an invalid fixture fails the scenario's load in a test).
  Existing scenarios are unaffected.
- **Entry states use the operator-interaction scheme.** The representation is an
  `OperatorStateResource` whose scope is the entry; the start's own members (entry, ready, chat,
  refusal, adoption failure, recipes) sit under a `start` member beside it, so a client can read the
  state with the shared operator codec and the start members separately. This differs from the older
  start scenarios, which used the runtime's legacy representation; the migrated `start.json` and the
  new case scenarios all use the operator scheme.
- **The `/api/v1/files` alias is not implemented.** The runtime-model specification names
  `GET /api/v1/files?path=…`; this package implements the project-scoped
  `/api/v1/projects/{project_id}/files` the work package names. A later package that introduces a
  global picker can add the alias without touching this contract.
- **The recipe catalogue is per project in the mock.** `GET /api/v1/recipes` serves the project's
  recipes from the start registry and falls back to the built-in catalogue when a scenario names
  none, keeping the earlier recipe tests green.
- The completion report stays at `doc/agy/start-contract-plan-report.md`; moving it to
  `doc/agy/verified/` is the orchestrator's job in Step 8 after the change lands on `main`.

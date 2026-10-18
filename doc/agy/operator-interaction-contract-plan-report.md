# Completion Report: `operator-interaction-contract` (What Every Operator Interaction Shares)

**Work package:** `doc/plans/implemented/operator-interaction-contract.adoc` (epic clients)
**Planning document:** `doc/agy/implemented/operator-interaction-contract-plan.md`
**Governing process:** [`doc/agy/runbook.md`](runbook.md)
**Topic branch:** `feat/operator-interaction-contract`
**Requirement served:** `PM-ARCH-11`

---

## 1. Executive Summary

The foundation contracts shared by every operator interaction are now implemented in `pm-api` and
backed in `pm-mock-runtime`:

* The **operator representation** and **operator link scheme** in `de.planmarshall.api.interaction`:
  `OperatorStateResource` (scope, state id, node id, kind, level, progress, title, texts, facts,
  proposal, items, revision, links) and `OperatorLink` (`{id, rel, method, href, params, preselected,
  confirm}`), with the closed relation catalogue `OperatorLinkRelation` (the nine relations of the
  Hypermedia Format specification).
* The **event envelope** and **closed event catalogue** in `de.planmarshall.api.event`:
  `EventEnvelope` (`{event, id, data: {event, at, scope, …payload}}`), `EventScope`, the closed
  `EventName` catalogue (the 68 names of the Event Catalogue), and `StreamClosedEvent` (the closed
  `stream.closed` reasons).
* The **configuration levels and origins** in `de.planmarshall.api.config`: the closed `ConfigOrigin`
  (`default`, `derived`, `machine`, `team`, `project`, `plan`) with its resolution rank, and
  `ConfigValueResource` (key, value, origin, the origin it overrides, revision).
* **Mock runtime support** in `pm-mock-runtime`: the `operator-interaction` scenario whose states
  carry real operator representations with links keyed by their `id`, catalogue-conforming
  `check.result` events, and a moved state whose stale link is answered `act_not_available` (409) as
  the real runtime answers it; load-time validation of every operator representation and event
  against the `pm-api` codecs.

All codecs run on the `jackson-core` streaming API alone (`JsonParser` / `JsonGenerator`), never
`jackson-databind`; the `jackson-core-only` enforcer rule of `pm-api` still passes. Every record has a
round-trip test of its codec, and every closed list (event names, origins, stream-end reasons, link
relations, scope kinds, text sources) is an enumeration a test compares with the specification.

---

## 2. Trace Matrix (Phase 1)

| Source | Anchor | Destination |
|---|---|---|
| Requirement | `PM-ARCH-11` (the operator's two clients and their one contract) | `OperatorStateResource`, `OperatorLink`, `EventEnvelope`, `ConfigValueResource`, and the closed enums; the mock runtime scenario. |
| Hypermedia Format | § Operator Representation | `OperatorStateResource` and its supporting records (`OperatorScope`, `OperatorProgress`, `OperatorItem`). |
| Hypermedia Format | § Operator Links | `OperatorLink` and `OperatorLinkRelation`. |
| Hypermedia Format | § Texts of the Runtime | `OperatorText` (message key with arguments, or verbatim text with `TextSource`). |
| Hypermedia Format | Closed Link Relations | `OperatorLinkRelation` (the nine relations), citing the catalogue in the class Javadoc. |
| Runtime Model | § Local API (error codes) | `ACT_NOT_AVAILABLE` / `CONFLICT` of the already-delivered `ApiErrorCode` (used by the mock refusal); no new enumeration. |
| Runtime Model | § Events per Scope | `EventEnvelope`, `EventScope`. |
| Runtime Model | § Event Catalogue | `EventName` (closed catalogue) and `StreamClosedEvent` (closed `stream.closed` reasons). |
| Runtime Model | § Mock Runtime | The `operator-interaction` scenario, operator-link validation, keyed routing, and the moved-state refusal. |
| Store Schemas | § Project Values over Machine Values | `ConfigOrigin` (the six levels with rank) and `ConfigValueResource`. |
| Store Schemas | § Operator files and their revision | The `revision` member of `ConfigValueResource` and of `OperatorStateResource`. |
| Identifiers | Link Identifier, State Id, Message Key, Event Name grammars | Grammar validation in `OperatorLink`, `OperatorStateResource`, `OperatorText`, and `EventName`. |

Rows left out of the slice: none. The package's own scope (the shared state representation, the link
scheme, the event envelope and catalogue, the configuration levels, and the mock support) is covered
in full.

---

## 3. Decisions and Deviations from the Plan Text (adopted deliberately)

1. **`StreamClosedEvent` carries the specification's reasons, not the plan's illustrative list.** The
   plan's §1.2 and Phase 3 name the termination reasons `normal`, `reset`, `timeout`, `shutdown`.
   That list appears nowhere in the specification: the Runtime Model § Event Catalogue (Stream
   Control) defines `stream.closed`'s `reason` as the closed list `device_revoked`, `device_expired`,
   `listener_closed`, `runtime_stopping`, `scope_ended`, and the package's own *Done When* requires
   every stream-end reason to be an enumeration **a test compares with the specification**. The
   implemented `StreamClosedEvent` is therefore the specification's five reasons, and
   `EventEnvelopeTest.closedStreamClosedReasons` compares the enum with that list verbatim (and
   asserts that the plan's `normal` is *not* a member). The specification is the source of truth
   (runbook §2). The pre-existing `de.planmarshall.api.web.StreamClosedEvent` (a payload record of the
   device flow) is untouched.
2. **The operator state is a flattened record.** The plan's §1.1 lists `OperatorStateResource` with the
   top-level members `scope`, `state_id`, `node_id`, `kind`, `level`, … The Hypermedia Format § Operator
   Representation describes the same fields with `state_id`, `node_id`, `kind`, and `level` grouped
   under a `state` object. This package follows the plan's flattened member list (each field at the top
   level), which is also the shape the mock scenario uses. A reader of either shape is out of slice.
3. **`OperatorLinkRelation` is a new closed type.** It carries the nine shared relations
   (`next`, `alt`, `retry`, `fix`, `loop_back`, `submit`, `escalate`, `consult`, `abort`) the
   Hypermedia Format defines for a state's `links`. The `api-contract` report (§9.2) recorded this as
   its follow-up: the resource-navigation relations of `de.planmarshall.api.resource.LinkRelation`
   (`self`, `project`, `events`, …) are a different surface and stay separate.
4. **`EventName` is the catalogue, and load validation is scoped to operator representations.** The
   `EventName` enum is the closed 68-name catalogue and its test compares it verbatim with the
   specification. The mock loader validates an operator representation (and its events) whenever a
   state's representation carries a `state_id`; the legacy scenarios (whose representations name no
   operator link and whose events predate the catalogue, e.g. `check`) are left unchanged so this
   package does not rewrite a sibling package's fixtures. `EventName.matchesGrammar` is the
   event-name-grammar check the loader applies to every scenario.
5. **The stale-link `act_not_available` is history-based and opt-in.** The mock tracks the operator
   links offered by the states entered so far, but only for states whose representation carries an
   operator `links` array. An act against one of those links after the state moved is answered `409`
   `act_not_available`; a request that was never offered stays `404` `mock_no_route` (the pre-existing
   `MockRuntimeTest.hypermediaTransitions` expectation is preserved, since the legacy onboarding
   representations carry no operator `links` array).

---

## 4. Delivered Artifacts

### 4.1 `pm-api` — new production artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorStateResource.java` | The operator representation of a state (scope, state_id, node_id, kind, level, progress, title, texts, facts, proposal, items, revision, links) with strict unknown-field rejection, grammar validation, unique link ids, and a `jackson-core` codec (`toMap`/`fromMap`/`toJson`/`fromJson`/`writeTo`/`readFrom`). |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorLink.java` | The operator link `{id, rel, method, href, params, preselected, confirm}` with id-grammar, method, and message-key validation, and its codec; `fromArray`/`toMaps` link-array helpers. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorLinkRelation.java` | The closed nine-relation catalogue shared with the model-facing links; fail-closed `fromWire`. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorScope.java` + `OperatorScopeKind.java` | The scope (kind, id, project id and display name) and its closed kind set (`plan`, `epic`, `workspace`, `machine`, `entry`). |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorProgress.java` | The `{done, of}` step progress with bounded validation. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorText.java` + `TextSource.java` | A runtime text: a message key with arguments, or verbatim text with its closed source (`provider`, `harness`, `model`, `operator`, `repository`); the exact-one-of invariant. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorProposal.java` | A value proposed to the operator with its `ConfigOrigin`. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorItem.java` | A row of a state's list (id, facts, links). |
| `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` | The closed 68-name event catalogue with the event-name grammar and fail-closed resolution. |
| `pm-api/src/main/java/de/planmarshall/api/event/EventEnvelope.java` + `EventScope.java` | The stream envelope (`{event, id, data: {event, at, scope, …payload}}`) and its scope, read back fail-closed (catalogue member, `data.event` matches, integral non-negative id, RFC 3339 `at`). |
| `pm-api/src/main/java/de/planmarshall/api/event/StreamClosedEvent.java` | The closed `stream.closed` reasons (`device_revoked`, `device_expired`, `listener_closed`, `runtime_stopping`, `scope_ended`). |
| `pm-api/src/main/java/de/planmarshall/api/config/ConfigOrigin.java` | The closed six-level origin with its resolution rank and `overrides`; the operator-writable levels (`machine`, `project`, `plan`). |
| `pm-api/src/main/java/de/planmarshall/api/config/ConfigValueResource.java` | An effective value (key, value, origin, the origin it overrides, revision) with the rank monotonicity and revision-bound checks. |
| `package-info.java` × 3 | Package documentation naming the specification sections. |

### 4.2 `pm-api` — new test artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/test/java/de/planmarshall/api/interaction/OperatorStateResourceTest.java` | 7 tests: the closed relation, scope-kind, and text-source catalogues; the full operator-state and operator-link round-trip through the tree and the streaming codec; both text forms; unknown-field / duplicate-id / invalid-level / invalid-grammar refusals; and the supporting-record round-trips. |
| `pm-api/src/test/java/de/planmarshall/api/event/EventEnvelopeTest.java` | 7 tests: the closed event catalogue compared verbatim with the specification; the fail-closed unknown-name resolution and grammar; the closed `stream.closed` reasons compared with the specification (and `normal` refused); the envelope round-trip and scope; and the malformed-envelope refusals. |
| `pm-api/src/test/java/de/planmarshall/api/config/ConfigOriginTest.java` | 6 tests: the closed origin catalogue and resolution order; the `ConfigValueResource` round-trip; an arbitrary tree value and an absent value; the override-monotonicity and revision bounds; and the malformed-input refusals. |

### 4.3 `pm-mock-runtime` — modified production artifacts

| Artifact | Change |
|---|---|
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` | Added `validateOperatorState`: a state whose representation carries a `state_id` is decoded with `OperatorStateResource.fromMap` (fail-closed on an unknown field, an unknown relation, or an invalid state id); every scenario link must be an operator link the representation offers by its `id`; and every event must satisfy the event-name grammar and be an `EventName` catalogue member. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Records the operator links offered by the states entered (from a representation `links` array) and answers a request that matches no current link but one of these `409` `act_not_available`; the history is cleared on scenario switch and reset. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java` | Added the `OPERATOR_INTERACTION` constant and its `ALL` entry. |

### 4.4 `pm-mock-runtime` — new production and test artifacts

| Artifact | Purpose |
|---|---|
| `pm-mock-runtime/src/main/resources/scenarios/operator-interaction.json` | The scenario: four states with real operator representations (links keyed by `id`), two `check.result` events, and an `auto` move to a state whose predecessor's `yes` link is stale. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/OperatorInteractionScenarioTest.java` | 3 tests: the end-to-end serve (link follow, keyed links, `check.result` events, the moved-state `act_not_available`, and the never-offered `mock_no_route`); the scenario loads and validates; and an operator scenario with an unoffered link, an unknown event, or an unknown relation fails the load. |

### 4.5 Documentation

| Artifact | Change |
|---|---|
| `doc/agy/operator-interaction-contract-plan.md` → `doc/agy/implemented/operator-interaction-contract-plan.md` | Moved; the runbook and work-package links rebased; checklist items 1–6 checked. |
| `doc/plans/operator-interaction-contract.adoc` → `doc/plans/implemented/operator-interaction-contract.adoc` | Moved; every relative link rebased for the deeper location. |
| `doc/plans/README.adoc` | `operator-interaction-contract` marked `done`; its index link and every dependency link rebased to `implemented/operator-interaction-contract.adoc`. |
| `doc/plans/implemented/README.adoc` | Row added for the delivered package. |
| `doc/agy/implemented/README.md` | Archive row added, linking this report (pending post-landing verification). |
| `doc/agy/operator-interaction-contract-plan-report.md` | This report. |

---

## 5. Test & Verification Results

### 5.1 Targeted suites

```
./mvnw test -pl pm-api -Dtest='ConfigOriginTest,EventEnvelopeTest,OperatorStateResourceTest'
./mvnw test -pl pm-mock-runtime -Dtest=OperatorInteractionScenarioTest
```

| Suite | Tests | Result |
|---|---|---|
| `Configuration levels and origins` (`ConfigOriginTest`) | 6 | pass |
| `Event envelope and closed event catalogue` (`EventEnvelopeTest`) | 7 | pass |
| `Operator representation and link scheme` (`OperatorStateResourceTest`) | 7 | pass |
| `Operator interaction scenario` (`OperatorInteractionScenarioTest`) | 3 | pass |

### 5.2 Requirement set of the package (*Done When*)

| *Done When* criterion | Verification |
|---|---|
| Every record, link, and event type has a round-trip test of its codec | The three `pm-api` suites round-trip `OperatorStateResource`, `OperatorLink`, `OperatorScope`, `OperatorProgress`, `OperatorText`, `OperatorProposal`, `OperatorItem`, `EventEnvelope`, `EventScope`, and `ConfigValueResource` through both the tree (`toMap`/`fromMap`) and the streaming (`writeTo`/`readFrom`) codec. |
| Every closed list (event names, error codes, origins, stream-end reasons) is an enumeration a test compares with the specification | `EventEnvelopeTest.closedEventNameCatalogue` (68 names), `.closedStreamClosedReasons` (5 reasons), `ConfigOriginTest.closedOriginCatalogue` (6 origins), and `OperatorStateResourceTest.closedLinkRelationCatalogue` / `.closedScopeKindAndTextSourceCatalogues`; the error codes are compared in the delivered `ApiContractTest`. |
| The mock runtime serves a scenario with operator links, check events, and a moved state, and a test asserts each | `OperatorInteractionScenarioTest.operatorInteraction` follows a keyed operator link, reads two `check.result` events from the stream, and asserts the moved-state `act_not_available` and the never-offered `mock_no_route`. |
| `pm-api` depends on `jackson-core` only | The `jackson-core-only` enforcer rule passed in `verify -Ppre-commit`; the delivered `ApiContractTest.noJacksonDatabind` asserts the absence of `ObjectMapper`. |

### 5.3 Full reactor (unit tests, pre-commit run)

| Module | Tests | Failures | Errors |
|---|---|---|---|
| `pm-api` | 492 | 0 | 0 |
| `pm-mock-runtime` | 225 | 0 | 0 |

`pm-exec`, `pm-relay`, `pm-operator`, `pm-web-app` (Vitest), and `pm-web-e2e` built green in the same
reactor.

---

## 6. Domain Invariants

| Invariant | Status |
|---|---|
| `pm-api` depends on the streaming API of `jackson-core` only, never `jackson-databind` | **Verified** by the `jackson-core-only` enforcer rule and the streaming codecs of every new record. |
| A value read from the wire that is not in a closed catalogue is refused (fail-closed) | **Verified** for event names, stream-close reasons, configuration origins, link relations, scope kinds, text sources, HTTP methods, and unknown object fields (each raises `IllegalArgumentException` or `JsonFormatException`). |
| Every record is immutable and validates its input | **Verified** by the compact constructors (`Objects.requireNonNull`, grammar and bound checks) and `Map.copyOf`/`List.copyOf` copies. |
| Text of a model, a provider, or a harness is untrusted | **Verified** by `OperatorText`, which forces verbatim text to name its `TextSource` and refuses a message key that carries one. |
| An act against a state left meanwhile is refused `act_not_available` | **Verified** by `OperatorInteractionScenarioTest` (409 `act_not_available`), while a never-offered request stays `404` `mock_no_route`. |
| A link is keyed by its `id`, never its relation | **Verified** by `OperatorStateResource` (unique link ids) and the scenario's `links` map (keyed by id) with load validation that every key is an offered link id. |
| The event catalogue and the stream-end reasons are the specification's closed lists | **Verified** by the verbatim comparison in `EventEnvelopeTest`. |

---

## 7. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
```

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  8.170 s]
[INFO] pm-api ............................................. SUCCESS [01:10 min]
[INFO] pm-exec ............................................ SUCCESS [ 18.196 s]
[INFO] pm-relay ........................................... SUCCESS [ 16.008 s]
[INFO] pm-operator ........................................ SUCCESS [01:05 min]
[INFO] pm-mock-runtime .................................... SUCCESS [ 51.326 s]
[INFO] pm-web-app ......................................... SUCCESS [01:43 min]
[INFO] pm-web-e2e ......................................... SUCCESS [ 17.891 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  05:52 min
```

The gate validated the OpenRewrite formatting, the licence headers, the enforcer rules
(`clients-depend-on-pm-api-only`, `jackson-core-only`, `no-model-facing-content`), Javadoc
validation, and the complete unit-test reactor, and ended with a clean `git status` (no reformatted
files).

---

## 8. Cognitive Personas Applied

| Phase | Persona | Application |
|---|---|---|
| The records, the link scheme, and the codecs | `plan-marshall-persona-implementer` | Immutable Java 25 records, single responsibility (a record serializes itself, `JsonTree` is the tree/codec seam), command-query separation (pure `toMap`/`wire` queries), and fail-closed error handling. |
| Unit and scenario suites | `plan-marshall-persona-module-tester` | AAA structure, deterministic fixtures, boundary tests (progress bounds, revision bounds), malformed-input tests, and controls that the fail-closed boundary is exercised on the unknown value (e.g. `normal` is not a `stream.closed` reason). |
| Closed enumerations and the wire boundary | `plan-marshall-persona-security-expert` | Closed catalogues that fail closed, strict unknown-field rejection, grammar validation at the wire, and untrusted-text marking. |
| Mock runtime integration | `plan-marshall-persona-integration-tester` | A real HTTP/SSE scenario against the `pm-api` codecs, cross-component validation, and resilience of the stale-link answer. |
| Documentation and archival | `plan-marshall-persona-documenter` | Roadmap status, package/plan archival, and index updates. |
| Review readiness | `plan-marshall-persona-code-reviewer` | Adversarial pass over the change before the PR; the plan-versus-specification reconciliation of §3 is stated rather than hidden. |

---

## 9. Follow-up Items

1. **Reconcile the plan's `StreamClosedEvent` reason list** (§3.1) with the specification in
   `plan-marshall-documentation`, or record the specification list as the one source of truth; the
   plan's `normal`/`reset`/`timeout`/`shutdown` has no specification counterpart.
2. **Reconcile the nested-versus-flattened operator-state shape** (§3.2): the Hypermedia Format
   specification describes `state_id`/`node_id`/`kind`/`level` under a `state` object; this package
   implements the plan's flattened member list. A later package that serves the representation from
   the engine should confirm the wire shape and correct whichever document is wrong.
3. **Wire the contract into the consumers.** The records are the contract; the web app, the terminal
   client, and the engine's `pm-api`-backed routes adopt them in their own packages
   (`onboarding-contract`, `entry-contract`, `control-contract`, `web-*`, `cli-*`).
4. **Extend the mock runtime's catalogue enforcement** to the legacy scenarios (whose events predate
   the catalogue) when a sibling package owns those fixtures (§3.4).
5. **Carry the `de.planmarshall.api.web.StreamClosedEvent` payload record into the event package** (or
   document the two types) when the device flow is next touched; it predates this catalogue and is
   left untouched here.

---

## 10. Review Findings Resolved (CodeRabbit, SonarCloud, cuioss-review-bot)

_Pending: the automated reviewers run against the pull request (no `skip-bot-review` label). This
section records each finding and its resolution before the PR is enqueued._

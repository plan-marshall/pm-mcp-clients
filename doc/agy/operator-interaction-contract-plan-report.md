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
  `OperatorStateResource` (scope, a nested `state` with its id, node id, kind, and level, progress,
  title, texts, facts, proposal, items, revision, links) and `OperatorLink` (`{id, rel, method, href,
  params, preselected, confirm}`), with the closed relation catalogue `OperatorLinkRelation` (the nine
  relations of the Hypermedia Format specification).
* The **event envelope** and **closed event catalogue** in `de.planmarshall.api.event`:
  `EventEnvelope` (`{event, id, data: {event, at, scope, …payload}}`), `EventScope`/`EventScopeKind`,
  the closed `EventName` catalogue (the 68 names of the Event Catalogue), and `StreamClosedEvent` (the
  closed `stream.closed` reasons).
* The **configuration levels and origins** in `de.planmarshall.api.config`: the closed `ConfigOrigin`
  (`default`, `derived`, `machine`, `team`, `project`, `plan`) with its resolution rank, and
  `ConfigValueResource` (key, value, origin, the origin it overrides, revision).
* **Mock runtime support** in `pm-mock-runtime`: the `operator-interaction` scenario whose states
  carry real operator representations with links keyed by their `id`, catalogue-conforming
  `check.result` events, and a moved state whose stale link is answered `act_not_available` (409) as
  the real runtime answers it; load-time validation of every operator representation, link, and event
  against the `pm-api` codecs.

All codecs run on the `jackson-core` streaming API alone (`JsonParser` / `JsonGenerator`), never
`jackson-databind`; the `jackson-core-only` enforcer rule of `pm-api` still passes. Every record has a
round-trip test of its codec, and every closed list (event names, event-scope kinds, stream-end
reasons, origins, link relations, scope kinds, text sources) is an enumeration a test compares with
the specification. The wire boundary is fail-closed: an unknown member of any object, an unknown
enum member, a wrong-typed optional member, a link `href` that is not a local-API path, a duplicate
link id (item links included), or an event payload that shadows the envelope's own members is
refused with a `JsonFormatException`.

---

## 2. Trace Matrix (Phase 1)

| Source | Anchor | Destination |
|---|---|---|
| Requirement | `PM-ARCH-11` (the operator's two clients and their one contract) | `OperatorStateResource`, `OperatorLink`, `EventEnvelope`, `ConfigValueResource`, and the closed enums; the mock runtime scenario. |
| Hypermedia Format | § Operator Representation | `OperatorStateResource` and its supporting records (`OperatorScope`, `OperatorProgress`, `OperatorItem`); the state fields under the spec's `state` object. |
| Hypermedia Format | § Operator Links | `OperatorLink` and `OperatorLinkRelation`. |
| Hypermedia Format | § Texts of the Runtime | `OperatorText` (message key with arguments, or verbatim text with `TextSource`). |
| Hypermedia Format | Closed Link Relations | `OperatorLinkRelation` (the nine relations), citing the catalogue in the class Javadoc. |
| Runtime Model | § Local API (error codes) | `ACT_NOT_AVAILABLE` / `CONFLICT` of the delivered `ApiErrorCode` (used by the mock refusal); no new enumeration. |
| Runtime Model | § Events per Scope | `EventEnvelope`, `EventScope`, `EventScopeKind`. |
| Runtime Model | § Event Catalogue | `EventName` (closed catalogue) and `StreamClosedEvent` (closed `stream.closed` reasons). |
| Runtime Model | § Mock Runtime | The `operator-interaction` scenario, operator-link validation, keyed routing, envelope events, and the moved-state refusal. |
| Store Schemas | § Project Values over Machine Values | `ConfigOrigin` (the six levels with rank) and `ConfigValueResource`. |
| Store Schemas | § Operator files and their revision | The `revision` member of `ConfigValueResource` and of `OperatorStateResource`. |
| Identifiers | Link Identifier, State Id, Message Key, Event Name grammars | `DottedIdentifier` (a linear scan, no catastrophic regex) used by `OperatorLink`, `OperatorStateResource`, `OperatorText`, and `EventName`. |

Rows left out of the slice: none.

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
2. **The operator state is nested under `state` on the wire.** The plan's §1.1 lists
   `OperatorStateResource` with the members `scope`, `state_id`, `node_id`, `kind`, `level`, …; the
   Hypermedia Format § Operator Representation groups `state_id`, `node_id`, `kind`, and `level` under
   a `state` object. The specification's shape wins: the Java record keeps the flat components, and
   `toMap`/`fromMap` nest them (`"state": {"state_id": …, "node_id": …, "kind": …, "level": …}`), so a
   client following the documented shape can decode the representation.
3. **`OperatorLinkRelation` is a new closed type.** It carries the nine shared relations (`next`,
   `alt`, `retry`, `fix`, `loop_back`, `submit`, `escalate`, `consult`, `abort`) the Hypermedia Format
   defines for a state's `links`. The `api-contract` report (§9.2) recorded this as its follow-up: the
   resource-navigation relations of `de.planmarshall.api.resource.LinkRelation` are a different
   surface and stay separate.
4. **`EventName` is the catalogue, and load validation is scoped to operator representations.** The
   `EventName` enum is the closed 68-name catalogue and its test compares it verbatim with the
   specification. The mock loader validates an operator representation (detected by a `state` object
   carrying a `state_id`, its links, and its events) whenever a state uses one; the legacy scenarios
   (whose representations name no operator link and whose events predate the catalogue, e.g. `check`)
   are left unchanged so this package does not rewrite a sibling package's fixtures.
5. **The stale-link `act_not_available` is history-based, opt-in, and race-checked.** The mock
   records the operator links offered by the states entered (for states whose representation carries
   an operator `links` array) together with the offering state. An act against one of those links
   after the state moved is answered `409 act_not_available`, checked before a dynamic route can serve
   it and rechecked after a delayed link before the transition; a request that was never offered stays
   `404 mock_no_route`, and a link the current state still offers is left to normal routing (the
   pre-existing `MockRuntimeTest.hypermediaTransitions` expectation is preserved).
6. **The mock's operator events are envelope-shaped.** For an operator scenario the loader requires
   each event's `data` to be the envelope `data` object (`event`, `at`, `scope`, and the payload) and
   the `MockEventStream` writes a monotonically increasing SSE `id`, so a client can decode the mock
   stream with the shared event contract.

---

## 4. Delivered Artifacts

### 4.1 `pm-api` — new production artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorStateResource.java` | The operator representation of a state (scope, nested state, progress, title, texts, facts, proposal, items, revision, links) with strict unknown-field rejection (top level and `state`), grammar validation, unique link ids across the state and its items, a deep-frozen facts map, and a `jackson-core` codec. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorLink.java` | The operator link `{id, rel, method, href, params, preselected, confirm}` with id-grammar, method, local-API `href`, strict-typed `params`/`preselected`, and message-key validation, and its codec. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorLinkRelation.java` | The closed nine-relation catalogue shared with the model-facing links; fail-closed `fromWire`. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorScope.java` + `OperatorScopeKind.java` | The scope (kind, id, project id and display name) with its closed kind set and the kind's identifier rule. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorProgress.java` | The `{done, of}` step progress with bounded, strict-object validation. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorText.java` + `TextSource.java` | A runtime text: a message key with arguments, or verbatim text with its closed source; the exact-one-of invariant and unknown-member rejection. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorProposal.java` | A value proposed to the operator with its `ConfigOrigin`, deep-frozen. |
| `pm-api/src/main/java/de/planmarshall/api/interaction/OperatorItem.java` | A row of a state's list (id, facts, links) with strict-object facts and unknown-member rejection. |
| `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` | The closed 68-name event catalogue with the event-name grammar and fail-closed resolution. |
| `pm-api/src/main/java/de/planmarshall/api/event/EventEnvelope.java` + `EventScope.java` + `EventScopeKind.java` | The stream envelope, its scope and closed scope-kind set; reads back fail-closed (catalogue member, `data.event` matches, integral non-negative id, RFC 3339 `at`, reserved payload members refused, null payload members preserved). |
| `pm-api/src/main/java/de/planmarshall/api/event/StreamClosedEvent.java` | The closed `stream.closed` reasons. |
| `pm-api/src/main/java/de/planmarshall/api/config/ConfigOrigin.java` + `ConfigValueResource.java` | The closed six-level origin with its resolution rank and `overrides`; the effective value (key, value, origin, overridden origin, revision) with the rank-monotonicity check and an always-serialized `value`. |
| `pm-api/src/main/java/de/planmarshall/api/id/DottedIdentifier.java` | The linear event-name, state-id, and message-key grammars (replacing nested-quantifier regexes, `java:S5998`). |
| `pm-api/src/main/java/de/planmarshall/api/json/JsonTree.java` (modified) | Added `immutableCopy`/`immutableMap` (deep-frozen, null-preserving) and `objectOrNull` (strict optional-object type). |
| `pm-api/src/main/java/de/planmarshall/api/json/JsonFormatException.java` (modified) | Added the `(String, Throwable)` constructor so a wire decoder can rethrow a constructor's `IllegalArgumentException` as the documented format error. |
| `package-info.java` × 3 | Package documentation naming the specification sections. |

### 4.2 `pm-api` — new test artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/test/java/de/planmarshall/api/interaction/OperatorStateResourceTest.java` | The closed relation, scope-kind, and text-source catalogues; the nested-shape operator-state and operator-link round-trips; both text forms; unknown-field, duplicate-id, invalid-level, invalid-grammar, external-href, and wrong-typed-member refusals; and the supporting-record round-trips. |
| `pm-api/src/test/java/de/planmarshall/api/event/EventEnvelopeTest.java` | The closed event catalogue and event-scope-kind set compared verbatim with the specification; the closed `stream.closed` reasons (and `normal` refused); the envelope round-trip, scope, and null-member preservation; and the malformed-envelope and reserved-member refusals. |
| `pm-api/src/test/java/de/planmarshall/api/config/ConfigOriginTest.java` | The closed origin catalogue and resolution order; the `ConfigValueResource` round-trip; an arbitrary tree value and an explicit null; the override-monotonicity and revision bounds; and the malformed-input refusals. |
| `pm-api/src/test/java/de/planmarshall/api/id/DottedIdentifierTest.java` | The three grammars' accept/refuse cases and a long-input linearity check. |

### 4.3 `pm-mock-runtime` — modified production artifacts

| Artifact | Change |
|---|---|
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` | `validateOperatorState`: an operator representation is decoded with `OperatorStateResource.fromMap`; every scenario link must be an offered operator link whose `method`/`href` agree; every advertised operator link must have a scenario route; and every event must be a catalogue member whose `data` is a valid `EventEnvelope` `data` object (`event`, `at`, `scope`). |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` | Records the operator links offered by the states entered, with the offering state; answers a stale act `409 act_not_available` before a dynamic route and rechecks the state after a delayed link; the history is cleared on scenario switch and reset. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/MockEventStream.java` | Writes the monotonic SSE `id` line of the event envelope. |
| `pm-mock-runtime/src/main/java/de/planmarshall/mock/StandardScenarios.java` | Added the `OPERATOR_INTERACTION` constant and its `ALL` entry. |

### 4.4 `pm-mock-runtime` — new production and test artifacts

| Artifact | Purpose |
|---|---|
| `pm-mock-runtime/src/main/resources/scenarios/operator-interaction.json` | The scenario: states with operator representations (nested `state`, links keyed by `id`), envelope-shaped `check.result` events, and an `auto` move to a state whose predecessor's `yes` link is stale. |
| `pm-mock-runtime/src/test/java/de/planmarshall/mock/OperatorInteractionScenarioTest.java` | The end-to-end serve (link follow, keyed links, envelope `check.result` events, the moved-state `act_not_available`, and the never-offered `mock_no_route`); the scenario loads; and an operator scenario with an unoffered link, an unrouted link, an unknown event, an envelope-less event, or an unknown relation fails the load. |

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
./mvnw test -pl pm-api -Dtest='ConfigOriginTest,EventEnvelopeTest,OperatorStateResourceTest,DottedIdentifierTest'
./mvnw test -pl pm-mock-runtime -Dtest=OperatorInteractionScenarioTest
```

| Suite | Tests | Result |
|---|---|---|
| `Configuration levels and origins` (`ConfigOriginTest`) | 7 | pass |
| `Event envelope and closed event catalogue` (`EventEnvelopeTest`) | 9 | pass |
| `Operator representation and link scheme` (`OperatorStateResourceTest`) | 7 | pass |
| `Dotted identifier grammars` (`DottedIdentifierTest`) | 4 | pass |
| `Operator interaction scenario` (`OperatorInteractionScenarioTest`) | 3 | pass |

### 5.2 Requirement set of the package (*Done When*)

| *Done When* criterion | Verification |
|---|---|
| Every record, link, and event type has a round-trip test of its codec | The `pm-api` suites round-trip `OperatorStateResource`, `OperatorLink`, `OperatorScope`, `OperatorProgress`, `OperatorText`, `OperatorProposal`, `OperatorItem`, `EventEnvelope`, `EventScope`, and `ConfigValueResource` through both the tree and the streaming codec. |
| Every closed list (event names, error codes, origins, stream-end reasons) is an enumeration a test compares with the specification | `EventEnvelopeTest` (68 event names, 6 event-scope kinds, 5 stream-close reasons), `ConfigOriginTest` (6 origins), `OperatorStateResourceTest` (9 relations, 5 scope kinds, 5 text sources); the error codes are compared in the delivered `ApiContractTest`. |
| The mock runtime serves a scenario with operator links, check events, and a moved state, and a test asserts each | `OperatorInteractionScenarioTest.operatorInteraction` follows a keyed operator link, reads two envelope `check.result` events, and asserts the moved-state `act_not_available` and the never-offered `mock_no_route`. |
| `pm-api` depends on `jackson-core` only | The `jackson-core-only` enforcer rule passed in `verify -Ppre-commit`; `ApiContractTest.noJacksonDatabind` asserts the absence of `ObjectMapper`. |

### 5.3 Full reactor (unit tests, pre-commit run)

| Module | Tests | Failures | Errors |
|---|---|---|---|
| `pm-api` | 499 | 0 | 0 |
| `pm-mock-runtime` | 225 | 0 | 0 |

`pm-exec`, `pm-relay`, `pm-operator`, `pm-web-app` (Vitest), and `pm-web-e2e` built green in the same
reactor.

---

## 6. Domain Invariants

| Invariant | Status |
|---|---|
| `pm-api` depends on the streaming API of `jackson-core` only, never `jackson-databind` | **Verified** by the `jackson-core-only` enforcer rule and the streaming codecs of every new record. |
| A value read from the wire that is not in a closed catalogue is refused (fail-closed) | **Verified** for event names, event-scope kinds, stream-close reasons, configuration origins, link relations, scope kinds, text sources, HTTP methods, and unknown object members (each raises `JsonFormatException`). |
| A `fromMap` reports a malformed value as its documented `IOException`, never a bare runtime exception | **Verified** by the constructor checks wrapping into `JsonFormatException` (duplicate ids, `done > of`, invalid scope, invalid override order). |
| Every record is immutable and deep-freezes nested JSON | **Verified** by `JsonTree.immutableCopy` and the compact constructors; a nested map or list cannot be mutated after construction. |
| A JSON `null` member survives a round-trip | **Verified** by `EventEnvelopeTest.payloadMayCarryNullMembers` and the null-preserving `immutableCopy`. |
| Text of a model, a provider, or a harness is untrusted | **Verified** by `OperatorText`, which forces verbatim text to name its `TextSource`. |
| An operator link can never leave the local API boundary | **Verified** by `OperatorLink`, which requires `href` to begin with `/api/v1/`. |
| An act against a state left meanwhile is refused `act_not_available` | **Verified** by `OperatorInteractionScenarioTest` (409 `act_not_available`), while a never-offered request stays `404 mock_no_route` and a current link is not misreported as stale. |
| A link is keyed by its `id`, never its relation | **Verified** by the unique-id check (state and items) and the scenario's `links` map with load validation. |
| The event catalogue, event-scope kinds, and stream-end reasons are the specification's closed lists | **Verified** by the verbatim comparison in `EventEnvelopeTest`. |

---

## 7. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
```

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  8.096 s]
[INFO] pm-api ............................................. SUCCESS [ 53.217 s]
[INFO] pm-exec ............................................ SUCCESS [  9.001 s]
[INFO] pm-relay ........................................... SUCCESS [ 12.818 s]
[INFO] pm-operator ........................................ SUCCESS [ 49.134 s]
[INFO] pm-mock-runtime .................................... SUCCESS [ 41.485 s]
[INFO] pm-web-app ......................................... SUCCESS [ 30.544 s]
[INFO] pm-web-e2e ......................................... SUCCESS [  7.405 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  03:32 min
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
| Unit and scenario suites | `plan-marshall-persona-module-tester` | AAA structure, deterministic fixtures, boundary tests (progress, revision, grammar, length), malformed-input tests, and controls that the fail-closed boundary is exercised on the unknown value (e.g. `normal` is not a `stream.closed` reason). |
| Closed enumerations and the wire boundary | `plan-marshall-persona-security-expert` | Closed catalogues that fail closed, strict unknown-member rejection, a local-API-only link boundary, deep-frozen trees, and untrusted-text marking. |
| Mock runtime integration | `plan-marshall-persona-integration-tester` | A real HTTP/SSE scenario decoded with the `pm-api` codecs, cross-component validation, and a race-checked stale-link answer. |
| Documentation and archival | `plan-marshall-persona-documenter` | Roadmap status, package/plan archival, and index updates. |
| Review readiness and finding triage | `plan-marshall-persona-code-reviewer` | Adversarial pass over every Sourcery, CodeRabbit, and SonarCloud finding (§10); the plan-versus-specification reconciliation of §3 is stated rather than hidden. |

---

## 9. Follow-up Items

1. **Reconcile the plan's `StreamClosedEvent` reason list** (§3.1) with the specification in
   `plan-marshall-documentation`, or record the specification list as the one source of truth; the
   plan's `normal`/`reset`/`timeout`/`shutdown` has no specification counterpart.
2. **Wire the contract into the consumers.** The records are the contract; the web app, the terminal
   client, and the engine's `pm-api`-backed routes adopt them in their own packages
   (`onboarding-contract`, `entry-contract`, `control-contract`, `web-*`, `cli-*`).
3. **Extend the mock runtime's catalogue enforcement** to the legacy scenarios (whose events predate
   the catalogue) when a sibling package owns those fixtures (§3.4).
4. **Carry the `de.planmarshall.api.web.StreamClosedEvent` payload record into the event package** (or
   document the two types) when the device flow is next touched; it predates this catalogue and is
   left untouched here.

---

## 10. Review Findings Resolved (Sourcery, CodeRabbit, SonarCloud, cuioss-review-bot)

The automated reviewers ran against the pull request (no `skip-bot-review` label). Each finding was
evaluated with the adversarial-refute lenses and resolved; preference-only suggestions with no defect
were declined with a reason.

### 10.1 SonarCloud (Quality Gate: new-code reliability)

| # | Rule | Finding | Resolution |
|---|---|---|---|
| 1 | `java:S5998` × 3 | Nested-quantifier regexes on the event name, state id, and message key can backtrack super-linearly. | Replaced by the linear `DottedIdentifier` scan; `DottedIdentifierTest` pins it. |
| 2 | `java:S5778` × 2 | An `assertAll` lambda held several throwing invocations. | Split into sequential `assertThrows` statements. |
| 3 | `java:S1192` × 2 | Duplicate `"links"`/`"method"` literals in `MockRuntimeEngine`. | Reused the `KEY_LINKS`/`KEY_METHOD`/`KEY_HREF` constants. |
| 4 | `java:S9391` | A loop in `ScenarioLoader` should be a stream. | Converted to a stream `filter`/`findFirst`. |
| 5 | `java:S2699` | A test had no assertion. | Asserted the loaded scenario's name and start state. |

### 10.2 Sourcery

| # | Finding | Resolution |
|---|---|---|
| 1 | A link `href` could leave the API boundary. | `OperatorLink` requires an absolute `/api/v1/…` path; external, protocol-relative, and relative targets are refused. |
| 2 | A payload could replace the envelope metadata. | `EventEnvelope` refuses a payload member named `event`, `at`, or `scope`. |
| 3 | A matched link could transition from a state left during a delay. | `handleStateLink` rechecks the current state under the lock and answers `act_not_available`. |
| 4 | Mock events lacked the envelope fields. | The loader requires each operator event's `data` to be a valid envelope `data` object and `MockEventStream` writes the SSE `id`. |
| 5 | The operator state shape did not match the nested `state` object. | `OperatorStateResource` now nests the state fields under `state` and reads that shape. |
| 6 | JSON `null` members failed with `Map.copyOf`. | `JsonTree.immutableCopy` preserves nulls and freezes nested maps/lists. |
| 7 | Wrong-typed optional members were silently dropped or coerced. | Strict optional-object reads (`JsonTree.objectOrNull`) and a strict `preselected` boolean. |
| 8 | Unknown event-scope kinds were accepted. | Added the closed `EventScopeKind` and validated it. |
| 9 | An advertised link could have no scenario route. | The loader requires every advertised link to have a scenario link. |
| 10 | Some stale acts bypassed the refusal through a dynamic route. | The stale check runs before the dynamic routes. |
| 11 | Scopes omitted required identifiers. | `OperatorScope` and `EventScope` enforce the kind's identifier rule. |
| 12 | Unknown nested members were ignored. | Every nested decoder rejects unknown members. |
| 13 | An explicit null config value disappeared on serialization. | `ConfigValueResource` always serializes `value`. |
| 14 | Nested JSON values remained mutable. | Deep-freezing in the constructors. |
| 15 | `Map.copyOf` rejected JSON nulls (same as 6). | Covered by 6. |
| 16 | A scenario link's method/href could disagree with the advertised link. | The loader compares both. |
| 17 | A top-level and item link could share an id. | Uniqueness across the state's and its items' links. |
| 18 | The scenario-load result went unchecked. | Asserted in `scenarioValidatesAgainstTheContract`. |

### 10.3 CodeRabbit

| # | Finding | Resolution |
|---|---|---|
| 1 | An invalid override order escaped as `IllegalArgumentException` from `fromMap`. | `ConfigValueResource.fromMap` checks the order and raises `JsonFormatException`. |
| 2 | Reserved envelope keys were not rejected. | Covered by Sourcery 2. |
| 3 | `EventEnvelope` was not null-safe at the payload boundary. | Covered by Sourcery 6. |
| 4 | The closed-schema statement held only at the top level. | Every nested decoder rejects unknown members. |
| 5 | `fromMap` let `IllegalArgumentException` escape. | Decoders wrap constructor checks into `JsonFormatException` (duplicate ids, `done > of`, scope, override order). |
| 6 | The mock answered 409 for a link the current state still offers. | The history is keyed by the offering state and the current state is excluded. |

# Completion Report: `api-contract` (API Contract of `/api/v1` in `pm-api`)

**Work package:** `doc/plans/implemented/api-contract.adoc` (Roadmap Milestone 2)
**Planning document:** `doc/agy/implemented/api-contract-plan.md`
**Governing process:** [`doc/agy/runbook.md`](../runbook.md)
**Topic branch:** `feat/api-contract`
**Requirements served:** `PM-ARCH-1`, `PM-IMPL-1`, `PM-IMPL-3`

---

## 1. Executive Summary

The core contract of the daemon's local API `/api/v1` is now formalized in `pm-api`, package
`de.planmarshall.api.resource`:

* The closed **error-code enumeration** `ApiErrorCode`, the one catalogue the Runtime Model
  specification (Local API, Error codes) defines and `PM-WATCH-RUN-12` holds every `pm-api` refusal
  to, each member paired with its HTTP status, with a fail-closed `fromWire`.
* The closed **link-relation** type `LinkRelation` and the resource link record `ResourceLink`
  (`href`, `rel`, `method`, `templated`).
* Five **resource records** for the daemon resources of this milestone: `RuntimeStatusResource`,
  `WebAccessResource`, `RuntimeStopResource`, `CredentialStatusResource`, and
  `RuntimeEventStreamResource`, each with a `jackson-core` streaming codec (`JsonParser` /
  `JsonGenerator`) and no `jackson-databind`.

All codecs run on the `jackson-core` streaming API alone. The `jackson-core-only` enforcer rule of
`pm-api` still passes, and a unit test asserts that `com.fasterxml.jackson.databind.ObjectMapper` is
not on the classpath. The undecided point `PM-WATCH-MOD-4` (the TOON encoder in two modules) is
resolved to a JSON-only local API (see §3).

Outcome: every resource of the milestone has a record with a round-trip test of its codec; the closed
error-code set is one enumeration whose members a test compares with the specification catalogue; an
unknown code read from the wire is refused (`IllegalArgumentException`); and the dependency guard
still passes.

---

## 2. Trace Matrix (Phase 1)

Every requirement bullet, specification section, and watch item the package names, with its
destination in this change.

| Source | Anchor | Destination |
|---|---|---|
| Requirement | `PM-ARCH-1` (the always-on daemon and its local API) | `RuntimeStatusResource` (state, version, pid, start instant, socket path), the resource links, and the closed error codes of `/api/v1`. |
| Requirement | `PM-IMPL-1` (the client contract module `pm-api` depends only on the streaming API of `jackson-core`, never `jackson-databind`) | The codecs of every resource and of `ResourceLink` on `JsonParser`/`JsonGenerator`; `JsonTree.readValue`/`writeValue` streaming seam; the `jackson-core-only` enforcer rule unchanged; the `noJacksonDatabind` test. |
| Requirement | `PM-IMPL-3` (the deterministic native TOON encoder) | The `PM-WATCH-MOD-4` decision (JSON-only local API, §3); no TOON codec is added to `pm-api`. |
| Specification | Runtime Model § Local API (`/api/v1`) — resources | The five resource records of the milestone. |
| Specification | Runtime Model § Local API — error codes | `ApiErrorCode`, the closed catalogue with its HTTP statuses. |
| Specification | Runtime Model § Local API — the operator representation and its links | `LinkRelation` and `ResourceLink`. |
| Specification | Identifiers | The link `href` and relation forms the records carry. |
| Watch | `PM-WATCH-MOD-4` (the TOON encoder in two modules) | Decided: option (1), a JSON-only local API (§3); the watch item is deleted in the documentation repository. |
| Watch | `PM-WATCH-MOD-1` (clients never reach the engine) | Verified: no new dependency was added; the `jackson-core-only` and `clients-depend-on-pm-api-only` enforcer rules still pass. |

Rows left out of the slice: none. The package's own scope (the resource representations, the links,
the error codes, and the JSON codec) is covered in full by the five records, the two enumerations,
and `ResourceLink`.

---

## 3. Decision: `PM-WATCH-MOD-4` — a JSON-only local API

The open watch item asked whether `pm-api` should hold a TOON codec beside the JSON codec (`pm-core`
already holds `ToonEncoder`), or whether the local API stays JSON-only.

**Decision: the local API `/api/v1` is JSON-only.** The operator's two clients read and write JSON on
the Unix socket / loopback listener; the model-facing TOON representation remains the concern of the
engine's MCP surface, encoded by the single `ToonEncoder` of `pm-core` (`PM-IMPL-3`). No second
encoder is added to `pm-api`, so the two-encoder drift the watch item named cannot occur, and the
conformance fixtures of the pinned TOON version guard the one encoder that exists. This matches the
plan's stated resolution ("JSON-only local API over Unix socket").

Consequence for the documentation repository (not this pull request): the watch item
`PM-WATCH-MOD-4` is deleted and the `pm-api` row of the Module Structure specification drops the
"JSON/TOON codecs" phrasing to "the JSON codec", as the package file requires the decision to be a
correction task of the documentation pull request. That correction is recorded here for the
documentation session to land.

---

## 4. Delivered Artifacts

### 4.1 `pm-api` — new production artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` | The closed error-code enumeration of the specification catalogue (42 members), each with its wire value and HTTP status; fail-closed `fromWire`, lenient `fromWireOrNull`, `wireValues()`, `statuses()`. |
| `pm-api/src/main/java/de/planmarshall/api/resource/LinkRelation.java` | The closed link-relation enumeration (`self`, `status`, `stop`, `web`, `credentials`, `events`) with `wire()`/`fromWire`/`requireWire`/`wireValues()`. |
| `pm-api/src/main/java/de/planmarshall/api/resource/ResourceLink.java` | The link record `{href, rel, method, templated}` with `toMap`/`fromMap`/`toJson`/`fromJson` and the streaming `writeTo(JsonGenerator)` / `readFrom(JsonParser)`; the shared link-array and object helpers. |
| `pm-api/src/main/java/de/planmarshall/api/resource/RuntimeStatusResource.java` | `GET /api/v1/status`: state, version, pid, `started_at`, `socket_path`, links. |
| `pm-api/src/main/java/de/planmarshall/api/resource/WebAccessResource.java` | The web access: status, port, `loopback_only`, `paired_devices_count`, links. |
| `pm-api/src/main/java/de/planmarshall/api/resource/RuntimeStopResource.java` | A runtime stop: the nested closed `StopMode` (`immediate`/`drain`), `timeout_seconds`, links. |
| `pm-api/src/main/java/de/planmarshall/api/resource/CredentialStatusResource.java` | A credential entry's status: the nested closed `Availability` (`present`/`not_found`), `token_path`, `expiry`, links (never the secret). |
| `pm-api/src/main/java/de/planmarshall/api/resource/RuntimeEventStreamResource.java` | The runtime-wide event stream: status, `active_listeners`, `last_event_id`, links. |

### 4.2 `pm-api` — modified production artifacts

| Artifact | Change |
|---|---|
| `pm-api/src/main/java/de/planmarshall/api/json/JsonTree.java` | Added the streaming seam `readValue(JsonParser)` and `writeValue(JsonGenerator, Object)` so a record codec can read and write one value on an open `jackson-core` stream without materializing the whole document; the existing whole-input `parse`/`write` paths are unchanged. |

### 4.3 `pm-api` — test artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/test/java/de/planmarshall/api/resource/ApiContractTest.java` | 15 tests: the closed error-code catalogue equals the specification list and every member's status lies in the closed status set (and none carries a bare 401); an unknown code is refused fail-closed; the closed link-relation catalogue; the `ResourceLink` round-trip and the refusal of an unknown/missing relation; a round-trip of every resource through `toJson`/`fromJson`, `toMap`/`fromMap`, and the streaming `writeTo`/`readFrom`; both stop modes and both credential availabilities; malformed JSON and missing required fields; constructor validation; the streaming reader leaving trailing values readable; and the absence of `jackson-databind`. |

### 4.4 Documentation

| Artifact | Change |
|---|---|
| `doc/plans/README.adoc` | `api-contract` marked `done`; its index link and every dependency link rebased to `implemented/api-contract.adoc`. |
| `doc/plans/api-contract.adoc` → `doc/plans/implemented/api-contract.adoc` | Moved; relative links rebased for the deeper location. |
| `doc/plans/implemented/README.adoc` | Row added for the delivered package. |
| `doc/agy/api-contract-plan.md` → `doc/agy/implemented/api-contract-plan.md` | Moved; checklist items 2–6 checked and the runbook link rebased. |
| `doc/agy/implemented/README.md` | Archive row added, linking this report (pending post-landing verification). |
| `doc/agy/api-contract-plan-report.md` | This report. |

### 4.5 Incidental reformatting (OpenRewrite, pre-commit gate)

The `pre-commit` OpenRewrite recipes normalized one pre-existing import-block deviation unrelated to
the change, required for the gate to end with a clean `git status`:
`pm-api/src/main/java/de/planmarshall/api/host/ProfileDocumentReader.java` (the `com.fasterxml`
import block moved below the `java`/`lombok` block).

---

## 5. Test & Verification Results

### 5.1 Targeted unit suite

```
./mvnw test -pl pm-api -Dtest=ApiContractTest
```

| Suite | Tests | Result |
|---|---|---|
| `API contract of /api/v1` (`ApiContractTest`) | 15 | pass |

### 5.2 Requirement set of the package

| *Done When* criterion | Verification |
|---|---|
| Every resource of the list has a record with a round-trip test of its codec | `ApiContractTest` round-trips all five records through both the tree and the streaming codec. |
| The closed set of error codes is one enumeration whose members a test compares with the list of the specification | `closedErrorCodeCatalogue` compares `ApiErrorCode.wireValues()` with the specification catalogue verbatim, and the status set with `{400, 403, 404, 409, 423, 500, 503}`. |
| An unknown code read from the wire is refused | `unknownErrorCodeIsRefused` expects `IllegalArgumentException` from `fromWire` for `nope`, `null`, and `""`. |
| The dependency guard still passes (`pm-api` on `jackson-core` only) | `jackson-core-only` enforcer rule green in `verify -Ppre-commit`; `noJacksonDatabind` asserts the absence of `ObjectMapper`. |

### 5.3 Full reactor (unit tests, pre-commit run)

| Module | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `pm-api` | 442 | 0 | 0 | 0 |
| `pm-exec` | 87 | 0 | 0 | 3 |
| `pm-relay` | 25 | 0 | 0 | 0 |
| `pm-operator` | 418 | 0 | 0 | 0 |
| `pm-mock-runtime` | 222 | 0 | 0 | 0 |

`pm-web-app` (Vitest) and `pm-web-e2e` (Playwright) built green in the same reactor.

---

## 6. Domain Invariants

| Invariant | Status |
|---|---|
| `pm-api` depends on the streaming API of `jackson-core` only, never `jackson-databind` | **Verified** by the `jackson-core-only` enforcer rule and the `noJacksonDatabind` test. |
| A code read from the wire that is not in the closed catalogue is refused (fail-closed) | **Verified** by `ApiErrorCode.fromWire` and `unknownErrorCodeIsRefused`. |
| A link relation read from the wire that is not in the closed catalogue is refused | **Verified** by `ResourceLink.fromMap` (raising `JsonFormatException`) and `unknownLinkRelationIsRefused`. |
| Every resource record is immutable and validates its input | **Verified** by the compact constructors and `constructorsRejectInvalidValues`. |
| No endpoint, error answer, or representation carries a secret | **Verified** by `CredentialStatusResource`, which carries only `status`, `token_path`, and `expiry`, never the secret. |
| A malformed document yields a clean exception, never a partial value | **Verified** by `malformedJsonIsRefused` and the streaming reader's refusal of a non-object token. |
| The error catalogue is the specification's closed list | **Verified** by `closedErrorCodeCatalogue` against the Runtime Model § Error codes list. |

---

## 7. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
```

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [ 10.427 s]
[INFO] pm-api ............................................. SUCCESS [01:08 min]
[INFO] pm-exec ............................................ SUCCESS [ 13.041 s]
[INFO] pm-relay ........................................... SUCCESS [ 15.729 s]
[INFO] pm-operator ........................................ SUCCESS [01:18 min]
[INFO] pm-mock-runtime .................................... SUCCESS [01:05 min]
[INFO] pm-web-app ......................................... SUCCESS [ 37.275 s]
[INFO] pm-web-e2e ......................................... SUCCESS [ 10.126 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  05:00 min
```

The gate validated the OpenRewrite formatting, the licence headers, the enforcer rules
(`clients-depend-on-pm-api-only`, `jackson-core-only`, `no-model-facing-content`), Javadoc
validation, and the complete unit-test reactor. The one OpenRewrite rewrite of a pre-existing file is
committed (§4.5), so the gate ends with a clean `git status`.

---

## 8. Cognitive Personas Applied

| Phase | Persona | Application |
|---|---|---|
| Resource records, enumerations, codecs | `plan-marshall-persona-implementer` | Immutable Java 25 records, single responsibility (a record serializes itself, `JsonTree` is the tree/codec seam), command-query separation (pure `toMap`/`wire` queries), and fail-closed error handling. |
| Unit suite | `plan-marshall-persona-module-tester` | AAA structure, deterministic fixtures, boundary tests (bounds of port, counts, timeout), malformed-input tests, and a control that the fail-closed boundary is exercised on the unknown value rather than a valid one. |
| Trust boundaries and the closed catalogues | `plan-marshall-persona-security-expert` | A closed error-code and relation boundary that fails closed, strict input validation at the wire, and a credential representation that never carries the secret. |
| Documentation and archival | `plan-marshall-persona-documenter` | Roadmap status, package/plan archival, index updates, and the `PM-WATCH-MOD-4` decision record. |
| Review readiness | `plan-marshall-persona-code-reviewer` | Adversarial pass over the change before the PR; the plan-versus-specification reconciliation of §9 is stated rather than hidden. |

---

## 9. Deviations from the Plan Text (adopted deliberately)

1. **The error-code catalogue is the specification's, not the plan's summary list.** The plan's §1.3
   and Phase 2 enumerate a short, generic list (`bad_request`, `unauthorized`, `precondition_failed`,
   `rate_limit_exceeded`, `internal_error`, …). That list is not the specification's catalogue: the
   Runtime Model § Local API, *Error codes* defines a closed list of **42** domain codes across the
   statuses `400`, `403`, `404`, `409`, `423`, `500`, and `503`, and states that an unauthenticated
   request is answered `401` **without a code**. The package's own *Done When* requires the
   enumeration's members to be compared by a test with *the list of the specification*, and
   `PM-WATCH-RUN-12`/the Module Structure specification name that section as the one closed list of
   `pm-api`. The implemented `ApiErrorCode` is therefore the specification catalogue, and the test
   compares the enum with it verbatim. This is the one place the plan text and the specification
   disagreed; the specification is the source of truth (runbook §2).
2. **The link-relation set follows the plan.** `LinkRelation` carries the six resource relations the
   plan names (`self`, `status`, `stop`, `web`, `credentials`, `events`). The Hypermedia Format
   specification's *Closed Link Relations (`rel`)* enumerate the transition relations of a
   representation's `links[N]` (`next`, `alt`, `retry`, `fix`, `loop_back`, `submit`, `escalate`,
   `consult`, `abort`). They are a different surface (a resource's outgoing reference links versus a
   state's offering of transitions); folding the two into one type is out of this package's slice and
   is recorded as a follow-up.
3. **`JsonTree` gained a streaming seam.** The plan asks for codecs "over the streaming API of
   `jackson-core`"; rather than duplicate the tree reader/writer, `JsonTree.readValue`/`writeValue`
   expose the existing `JsonParser`/`JsonGenerator` path to one value at a time. No new dependency
   and no behavior change to the whole-input `parse`/`write`.

---

## 10. Follow-up Items

1. **Fold the resource-link relations into the specification's closed relation grammar** (or state
   the two surfaces explicitly) when the representation model of `pm-api` is next extended
   (§9.2).
2. **Delete `PM-WATCH-MOD-4` and correct the `pm-api` row** of the Module Structure specification in
   `plan-marshall-documentation` (the TOON decision of §3), as the package file requires.
3. **Align the mock runtime and operator error codes** with `ApiErrorCode` in a later package; the
   mock runtime still emits ad-hoc codes (`invalid_code`, `project_not_found`, `mock_no_route`) that
   do not appear in the closed catalogue, and `project_not_enrolled`/`scope_not_found` are the
   catalogue's forms.
4. **Wire the records into the mock runtime and the clients** (`overview-contract` and the control
   packages already reference the daemon resources); this package delivers the contract only, as the
   plan scopes it.

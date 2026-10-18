# Completion Report: `profile-documents` (Host-Detection Data in Profile Documents)

**Work package:** `doc/plans/implemented/profile-documents.adoc` (Roadmap Milestone 2)
**Planning document:** `doc/agy/implemented/profile-documents-plan.md`
**Governing process:** [`doc/agy/runbook.md`](../runbook.md)
**Topic branch:** `feat/profile-documents`
**Requirements served:** `PM-CLIENT-2`, `PM-CLIENT-3`, `PM-CLIENT-5`, `PM-IMPL-1`, `PM-IMPL-3`

---

## 1. Executive Summary

The host-detection table of `pm-api` is no longer written in Java. The detection signals and their
precedence, the process markers, the session ID variable, the version probes (argument vector and
pattern), the model source, and the provisional flag now live in one profile document per harness
below `classpath:/profiles`. A new generic reader, `ProfileDocumentReader`, extracts the
`detection` and `identity` objects of every shipped document with the `jackson-core` streaming API,
skips the remaining sections, and names no harness. `HostDetection` discovers and loads the hosts at
initialization and holds no host constant, while its public API (`detect`, `agentSignalPresent`,
`sessionId`, `clientIds`, `host`, and the `Signal`/`Host` records) is unchanged.

An architectural guard test, `HarnessIsolationArchTest`, derives the forbidden harness ids from the
shipped document file names and refuses any production class of the host package that names one. A
negative-control fixture (`HarnessNamingControlFixture`, a test class) proves the guard detects an
injected harness name; the guard was additionally shown to fail when a harness literal is placed in
`HostDetection` (see §4).

Outcome: `HostDetection` holds no host constant; the existing `HostDetectionTest` suite passes
unchanged against the documents; the build test fails for a harness name added to a production class.

---

## 2. Delivered Artifacts

### 2.1 `pm-api` — new production artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/main/resources/profiles/antigravity.json` | Profile document, `detection.precedence` 10, primary signal `ANTIGRAVITY_AGENT`, session variable `ANTIGRAVITY_CONVERSATION_ID`, no version probe. |
| `pm-api/src/main/resources/profiles/opencode.json` | Profile document, precedence 20, primary signals `OPENCODE`/`OPENCODE_PID`, process marker `OPENCODE_PID`, probe `["opencode","--version"]`. |
| `pm-api/src/main/resources/profiles/claude.json` | Profile document, precedence 30, primary signal `CLAUDE_CODE_SESSION_ID`, secondary `CLAUDECODE=1`, process marker `CLAUDE_PID`, probe `["claude","--version"]`. |
| `pm-api/src/main/resources/profiles/codex.json` | Profile document, precedence 40, primary signals `CODEX_SANDBOX`/`CODEX_CI`/`CODEX_THREAD_ID` (presence-only, as the table had them), session variable `CODEX_THREAD_ID`, probe `["codex","--version"]`. |
| `pm-api/src/main/resources/profiles/neutral.json` | Neutral fallback document, precedence 50, empty signals, no probe; not a host row. |
| `pm-api/src/main/resources/META-INF/pm-api/profiles.index` | The shipped catalog: one client id per line, named by `ProfileDocumentReader` for discovery. |
| `pm-api/src/main/java/de/planmarshall/api/host/ProfileDocumentReader.java` | Generic streaming reader: discovers the documents through the catalog, parses `client_id`, `detection`, and `identity` with `jackson-core`, skips every other token, validates the closed form, and refuses a document as a whole. |

### 2.2 `pm-api` — modified production artifacts

| Artifact | Change |
|---|---|
| `pm-api/src/main/java/de/planmarshall/api/host/HostDetection.java` | The hard-coded `HOSTS` literal and the `DECLARED_MODEL`, `VERSION_FLAG`, and `PROVISIONAL_VERSION_PATTERN` constants are gone; `HOSTS` is now a public, document-loaded table (public API preserved), sorted by ascending `detection.precedence`. |
| `pm-api/src/main/java/de/planmarshall/api/host/package-info.java` | Describes the table as read from the shipped profile documents; names no harness. |
| `pm-api/src/main/resources/META-INF/native-image/de.planmarshall/pm-api/reachability-metadata.json` | Registers `profiles/*.json` and the catalog `META-INF/pm-api/profiles.index` as native-image resources. |

### 2.3 `pm-api` — test artifacts

| Artifact | Purpose |
|---|---|
| `pm-api/src/test/java/de/planmarshall/api/host/ProfileDocumentReaderTest.java` | 19 tests: the shipped documents read in an ascending, unique precedence; the catalog matching the shipped documents exactly (drift guard); closed-form client ids; catalog-driven discovery from an archive, with a document the catalog does not name left unread; refusal of an empty catalog, an absent cataloged document, a file-name/client-id mismatch, a shared precedence, malformed JSON, trailing content after a valid document, a repeated field, a client id outside the canonical form, a missing detection, a non-positive precedence, a signal without `equals`, an identity without the session ID variable, and a version pattern without a probe; and skipping of unrelated sections. |
| `pm-api/src/test/java/de/planmarshall/api/host/HarnessIsolationArchTest.java` | 2 tests: no production class of `de.planmarshall.api.host` names a harness (ids derived from the shipped documents), and the negative control. |
| `pm-api/src/test/java/de/planmarshall/api/host/HarnessNamingControlFixture.java` | Negative-control fixture (test class) that names a harness; it proves the guard detects an injected name. |

### 2.4 Documentation

| Artifact | Change |
|---|---|
| `doc/plans/README.adoc` | `profile-documents` marked `done`; index link and the `operator-checks` dependency link point to `implemented/profile-documents.adoc`. |
| `doc/plans/profile-documents.adoc` → `doc/plans/implemented/profile-documents.adoc` | Moved; relative links rebased for the deeper location. |
| `doc/plans/implemented/README.adoc` | Row added for the delivered package. |
| `doc/agy/profile-documents-plan.md` → `doc/agy/implemented/profile-documents-plan.md` | Moved; checklist items 1–5 checked and relative links rebased. |
| `doc/agy/implemented/README.md` | Archive row added, linking this report. |
| `doc/agy/profile-documents-plan-report.md` | This report. |

### 2.5 Incidental reformatting (OpenRewrite, pre-commit gate)

The `pre-commit` OpenRewrite recipes normalized two pre-existing formatting deviations unrelated to
the change, required for the gate to end with a clean `git status`:
`pm-mock-runtime/.../MockRuntimeEngine.java` (continuation-line indentation) and
`pm-operator/src/test/.../PmOperatorProcessIT.java` (continuation-line indentation).

---

## 3. Document Shape and Mapping to the Detection Table

Each document carries the closed form of the two objects the reader consumes:

* `client_id` — the canonical client id, equal to the file name (the schema rule of
  `01-profile-schema.adoc`, *The Profile Is the Harness*).
* `detection` — `precedence` (a positive, pairwise distinct integer), `primary_signals`,
  `secondary_signals` (each a `{ "variable", "equals" }` object of the schema's `$defs.signal`),
  and `process_markers`.
* `identity` — `session_id_variable`, `version_probe`, `version_pattern`, `model_source`, and
  `provisional`.

The reader extracts `precedence`, `primary_signals`, and `secondary_signals` from `detection` and
every field of `identity`; it skips `process_markers` and any other detection member (for example
`ai_agent_prefixes`, `reaches`) and every unrelated top-level section by jumping over the tokens, as
the plan's Phase 3 prescribes. The `process_markers` of each harness are data in the document, kept
for the nested-host rule of the Host Integration Table, and are not consumed by the table rows yet
(`Host` carries no process-marker component today).

### 3.0 Discovery: the shipped catalog

Which documents are read is data: the catalog `classpath:/META-INF/pm-api/profiles.index`, one client
id per line (`#` comments and blank lines ignored). The reader never enumerates a classpath
directory — an archive built without directory entries, a shaded artifact, or a native image does not
expose one — and a document below `profiles/` that the catalog does not name is not read, so
unrelated content beside the documents cannot refuse the table. The catalog is the plan's sanctioned
*resource catalog index*; `catalogMatchesTheShippedDocuments` keeps it in step with the documents
below `profiles/`, so a new harness is a new document **and** its line, and a divergence fails the
build. Discovery is fail-closed: an absent or empty catalog, or a cataloged document that is not on
the classpath, refuses the table rather than yielding a partial one.

### 3.1 Precedence

| Document | `detection.precedence` |
|---|---|
| `antigravity.json` | 10 |
| `opencode.json` | 20 |
| `claude.json` | 30 |
| `codex.json` | 40 |
| `neutral.json` | 50 |

The reader refuses two documents that share a precedence. `neutral` is loaded like every document
but is not a table row: `HostDetection` filters the neutral client id, so `host("neutral")` is empty
and `clientIds()` lists it last.

### 3.2 Deviations from the plan text (adopted deliberately)

* **Field name `client_id`, not `client`.** The plan's Phase 3 shorthand says the reader extracts
  `client`; the normative profile schema names the field `client_id` and requires it to equal the
  file name. The schema name is used, and the file-name rule is validated by the reader.
* **The guard covers the host package, not the whole repository.** Phase 5 scopes the scan to
  `de.planmarshall.api.host.*` production classes, which is the surface this work package solely
  owns (see the module-disjointness matrix of the plan). Production classes of the sibling modules
  that still name a harness (`pm-operator` `OperatorGate`, `pm-mock-runtime` mock data) belong to
  their own work packages; widening the guard is a follow-up. The scan is dependency-free (plain
  byte scanning of the compiled classes), matching the plan's sanctioned alternative to ClassGraph
  and respecting the `jackson-core-only` enforcer rule of `pm-api`.
* **`identity` carries three fields beyond the current `pm-core` schema slice.**
  `session_id_variable`, `model_source`, and `provisional` are not yet members of the
  `identity` object in `pm-mcp-core/doc/specification/client-profiles/01-profile-schema.adoc`
  (whose `identity` today holds only `version_probe` and `version_pattern`). They are carried here
  because the plan's §1 and §5 require the session ID variable, model source, and provisional flag
  to move into `identity`, and because `HostDetection`'s public `Host` contract exposes them. The
  fields are additive to the schema and are the point of the trace matrix step; the schema is to
  follow in `pm-mcp-core`.

---

## 4. Test & Verification Results

### 4.1 Unit tests (targeted)

```
./mvnw test -pl pm-api -Dtest='HostDetectionTest,ProfileDocumentReaderTest,HarnessIsolationArchTest'
```

| Suite | Tests | Result |
|---|---|---|
| `HostDetection` (existing, unchanged) | 13 | pass |
| `ProfileDocumentReader` — the shipped documents (catalog-driven) | 8 | pass |
| `ProfileDocumentReader` — one document | 11 | pass |
| `harness isolation of the host package` | 2 | pass |

The reader discovers the documents through a `ClassLoader` seam (`readShippedProfiles(ClassLoader)`), so
the archive path is exercised by building a jar that holds the catalog and its documents and reading
it through a `URLClassLoader`; the shipped path is exercised against the module's own exploded
classpath, where the drift guard compares the catalog with the documents below `profiles/`.

The existing `HostDetectionTest` was not edited: it passes unchanged against the documents, which is
the work package's *Done When* criterion.

### 4.2 Guard falsification (negative control in vivo)

To prove the guard fails for a harness name in a production class, a literal `"claude"` was injected
into `HostDetection` and the guard re-run:

```
HarnessIsolationArchTest.productionClassesNameNoHarness:58
  a production class names a harness:
  [.../pm-api/target/classes/de/planmarshall/api/host/HostDetection.class]
```

The injection was reverted and the guard passed again. The committed
`HarnessNamingControlFixture` reproduces the same detection on every build.

### 4.3 Full reactor (unit tests, pre-commit run)

| Module | Tests | Failures | Errors |
|---|---|---|---|
| `pm-api` | 417 | 0 | 0 |
| `pm-exec` | 71 | 0 | 3 (skipped) |
| `pm-relay` | 25 | 0 | 0 |
| `pm-operator` | 399 | 0 | 0 |
| `pm-mock-runtime` | 215 | 0 | 0 |

### 4.4 Consumer verification

`pm-operator` (`OperatorGate`, `OperatorGateTest`, 15 host-detection tests) and `pm-relay`
(`ServeCommand`, which validates `--client` against `HostDetection.clientIds()`) compile and pass
unchanged against the data-driven table, confirming the public contract is preserved.

---

## 5. Domain Invariants

| Invariant | Status |
|---|---|
| `HostDetection` holds no host constant; no production class of `de.planmarshall.api.host` names a harness | **Verified** by `HarnessIsolationArchTest` (ids derived from the documents) and by the in-vivo falsification of §4.2. |
| Every shipped document has both objects in the closed form of the schema, with pairwise different `precedence` | **Verified** by `ProfileDocumentReaderTest` and by the reader's own validation (refuses a document that is not in the closed form or that shares a precedence). |
| The existing detection tests pass unchanged against the documents | **Verified** — `HostDetectionTest` unedited, 13/13 pass. |
| The build test fails for a harness name added to a production class (control fixture) | **Verified** by `HarnessNamingControlFixture` and §4.2. |
| Failure is closed | **Verified** — an absent or empty catalog, a cataloged document that is absent, a document that is malformed, incomplete, repeats a field, carries a non-canonical client id, shows a non-positive precedence, or shares a precedence each raises, and no partial table is used. |
| `pm-api` depends on the `jackson-core` streaming API only | **Verified** by the `jackson-core-only` enforcer rule (no new dependency was added; the reader uses `JsonFactory`/`JsonParser` only). |
| Discovery is portable and needs no directory enumeration | **Verified** by the catalog-driven reader and the drift guard; the archive path is exercised through a `URLClassLoader`. The catalog and the documents are registered as native-image resources. |

---

## 6. Build & Quality Evidence

```
./mvnw verify -Ppre-commit
```

```
[INFO] Reactor Summary for pm-mcp-clients 0.1.0-SNAPSHOT:
[INFO] pm-mcp-clients ..................................... SUCCESS [  9.226 s]
[INFO] pm-api ............................................. SUCCESS [01:06 min]
[INFO] pm-exec ............................................ SUCCESS [ 15.200 s]
[INFO] pm-relay ........................................... SUCCESS [ 19.128 s]
[INFO] pm-operator ........................................ SUCCESS [01:13 min]
[INFO] pm-mock-runtime .................................... SUCCESS [01:00 min]
[INFO] pm-web-app ......................................... SUCCESS [ 57.215 s]
[INFO] pm-web-e2e ......................................... SUCCESS [ 11.252 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  05:12 min
```

Validated by the gate: OpenRewrite formatting, licence headers, the enforcer rules
(`clients-depend-on-pm-api-only`, `jackson-core-only`, `no-model-facing-content`), Javadoc
validation, and the complete unit-test reactor.

---

## 7. Cognitive Personas Applied

| Phase | Persona | Application |
|---|---|---|
| Profile documents, reader, `HostDetection` refactor | `plan-marshall-persona-implementer` | SRP (reader parses, table holds the rows), CQS (`detect`/`host`/`clientIds` are pure queries), immutable records, fail-closed error handling. |
| Unit tests and the architectural guard | `plan-marshall-persona-module-tester` | AAA structure, deterministic fixtures, boundary and malformed-input tests, a negative control whose premise (the derived id set) is asserted alongside the finding. |
| Classpath resource loading and untrusted JSON | `plan-marshall-persona-security-expert` | Contained resource discovery, a bounded streaming parse that never binds, fail-closed refusal of any document not in the closed form, and a guard that keeps harness names out of the code (trust-boundary hygiene). |
| Documentation and archives | `plan-marshall-persona-documenter` | Plan/report archival, roadmap status, index updates, doc-drift prevention. |
| Review readiness | `plan-marshall-persona-code-reviewer` | Adversarial pass over the change before the PR (see §3.2 deviations, which are stated rather than hidden). |

---

## 8. Follow-up Items

1. **Schema alignment in `pm-mcp-core`.** Fold `session_id_variable`, `model_source`, and
   `provisional` into the `identity` object of
   `client-profiles/01-profile-schema.adoc`, and mirror `process_markers` consumption in the profile
   loader's validation, so the shipped documents validate as whole profiles.
2. **Widen the harness-isolation guard.** Extend `HarnessIsolationArchTest` to the client modules
   once `pm-operator` and `pm-mock-runtime` retire their harness constants and mock fixtures in
   their own work packages.
3. **Native-image confirmation.** Confirm in a GraalVM native image build that the catalog and the
   documents (both registered in `reachability-metadata.json`) are read; the catalog removes the
   dependence on classpath directory enumeration that a native image does not offer.
4. **Host Integration Table comparison test.** The plan's fourth deliverable bullet (compare the
   table with the documents using the documentation checkout) needs a sibling checkout; recorded as
   a work package of its own.

---

## 9. Review Findings Resolved (Sourcery AI, PR #90)

The review bot raised nine findings; each was evaluated with the adversarial-refute lenses and
resolved in code (commit 2) or answered where the concern is by design.

| # | Finding | Resolution |
|---|---|---|
| 1 | *High* — shipped profiles disappear when the classpath `profiles` root is not enumerable (archive without directory entries, native image). | **Fixed.** Discovery is now the shipped catalog `META-INF/pm-api/profiles.index`; no classpath directory enumeration remains. The catalog and the documents are registered as native-image resources. |
| 2 | *High* — making `HOSTS` private breaks source and binary compatibility. | **Fixed.** `HOSTS` is public again, now a document-loaded table; the hard-coded literal is what was removed. |
| 3 | *Medium* — an omitted `identity.session_id_variable` is silently accepted as null. | **Fixed.** The field must be declared (null allowed); an identity without it is refused. |
| 4 | *Medium* — duplicate JSON keys are accepted. | **Fixed.** The factory enables `StreamReadFeature.STRICT_DUPLICATE_DETECTION`; a repeated field is refused. |
| 5 | *High* — an unrelated top-level JSON below `profiles/` is treated as a profile and can refuse the table. | **Fixed.** Only cataloged documents are read; the archive test proves a document the catalog does not name is left unread. |
| 6 | *Medium* — a non-canonical `client_id` is accepted. | **Fixed.** `client_id` is validated against the schema pattern `[a-z][a-z0-9-]{0,31}`. |
| 7 | *Medium* — `CODEX_CI` required the exact value `1`, so other values go undetected. | **Fixed.** `CODEX_CI` is a presence-only signal again, matching the table this refactor replaces (the hard-coded `Signal.present("CODEX_CI")`). |
| 8 | *Medium* — `detection.process_markers` is not validated. | **By design.** The reader consumes the table's fields (`precedence`, `primary_signals`, `secondary_signals`, `identity`) and deliberately skips the rest, as the plan's Phase 3 ("skips the rest of the document") prescribes; whole-document validation is the profile loader of `pm-core`, not this reader. The `client_id` half of the finding is fixed (#6). |
| 9 | *Medium* — `refusesTrailingContent` passed vacuously (the first object was incomplete). | **Fixed.** The test now parses a valid document followed by trailing content, so only the trailing-content check can raise. |

# PLAN-77: Java: CUI Standards Foundation (Dependencies, Logging, Null Safety, First Test Conversions)

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make the Java modules follow the CUI standards the project is configured for (`java-cui` domain in `.plan/marshal.json`: `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`, with `pm-dev-java:java-null-safety` and `pm-dev-java:javadoc`). The plan lays the foundation and converts the small modules: it adds the approved test dependencies where they are missing, writes the rule into `CLAUDE.md`, moves the one logging site onto `LogRecord` constants with one message holder per module, marks the packages of the two modules that already carry JSpecify `@NullMarked`, and converts the tests of the two small modules `pm-relay` and `pm-exec` to generated input data as the pilot. `PLAN-78` (`pm-api` tests), `PLAN-79` (`pm-operator` tests) and `PLAN-87` (`pm-mock-runtime` tests) follow it. Every other Java plan names the CUI skills in its deliverables, so new and changed tests follow the rule from now on.

An analysis of the repository at HEAD 7e2b4b61 (2026-10-30) found: 0 of 267 test classes use `Generators` or `@EnableGeneratorController`; `pm-operator` declares `cui-test-generator` and `cui-test-juli-logger` and uses neither; `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime` declare neither; the only logger is `MockRuntimeExtension`, which logs `ERROR` with string literals and has no `LogAsserts` test; `@NullMarked` is on 4 of 49 packages. `cui-http-testing` is not applied: HTTP runs over a Unix socket (`UnixSocketHttpClient`, tested against `FakeRuntimeServer`) or tests the repository's own server (`MockHttpServer`), so no MockWebServer fits and no dependency for it is added.

## Deliverables

1. **Dependencies and the rule**
   - What: add `de.cuioss.test:cui-test-generator` and `de.cuioss.test:cui-test-juli-logger`, both test scope and versioned by the parent POM as `pm-operator` already does, to `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`. Add the standards to `CLAUDE.md` § Code Standards: input test data from `Generators` and `@EnableGeneratorController` on test classes; `CuiLogger` with `LogRecord` constants for INFO and above, one DSL-style `LogMessages` class per module that logs (never one for the repository), `LogAsserts` for every `LogRecord`; `@NullMarked` on every package of a module that declares JSpecify. Write that the expected value of a contract (catalogue text, wire name, golden capture) stays literal.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - `./mvnw verify` passes in the four modules with the dependencies in; the enforcer rules (no module depends on a product module other than `pm-api`; `pm-exec` on none) stay green (evidence order: `none`)
     - The rule text is in `CLAUDE.md` and names the one-holder-per-module clause (evidence order: `none`)
2. **Logging in pm-mock-runtime**
   - What: `MockRuntimeExtension` logs its two `ERROR` lines (setup and close failure) through `LogRecord` constants of one `LogMessages` class of `pm-mock-runtime` (DSL-style, `@UtilityClass`, nested `ERROR` class, identifiers unique in the class); exception first; `DEBUG` lines stay direct. A test with `@EnableTestLogger` and `LogAsserts` covers both records. The class is documented as the skill's `logmessages-documentation` standard asks, in the module's documentation location.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:junit-core, pm-dev-java-cui:cui-logging, pm-dev-java-cui:cui-testing
   - Done when:
     - `MockRuntimeExtensionTest` (or a new test beside it) asserts both records with `LogAsserts`, failing against the string-literal version (evidence order: `test-first`)
     - No `LogMessages` class exists outside `pm-mock-runtime` (evidence order: `none`)
3. **Null safety markers**
   - What: add `@NullMarked` to the 8 `pm-operator` and 5 `pm-mock-runtime` packages that lack it (the list is under Expected Surface). `pm-api`, `pm-exec` and `pm-relay` declare no JSpecify dependency and are NOT touched: adding one is a dependency the operator has not approved.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:javadoc
   - Done when:
     - Every `package-info.java` of `pm-operator` and `pm-mock-runtime` carries `@NullMarked` and `./mvnw verify` stays green with the compiler's `failOnWarning` (evidence order: `none`)
4. **Tests of pm-relay and pm-exec (the pilot)**
   - What: convert the 11 test classes of the two small modules (3 + 8) as the pilot of the rule; the 43 classes of `pm-mock-runtime` are `PLAN-87`, so that the test conversions stand as one plan per module (`PLAN-78` pm-api, `PLAN-79` pm-operator, `PLAN-87` pm-mock-runtime) and this plan keeps the foundation. Convert so that input data comes from `Generators` (ids, names, paths, counts, free text) and every class that takes input carries `@EnableGeneratorController`; value-object contract tests use `ShouldHandleObjectContracts` only where the type has hand-written equality (records need none, and `cui-test-value-objects` is not added). Expected values of a contract stay literal. A class that takes no input data is listed in the progress record with that reason, never silently skipped.
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-java:junit-core, pm-dev-java:junit-integration, pm-dev-java-cui:cui-testing
   - Done when:
     - Each converted class uses `Generators` for its input, and a count over the two modules is recorded before and after: classes with `@EnableGeneratorController`, classes listed as input-free (evidence order: `none`)
     - `./mvnw -pl pm-relay,pm-exec verify -Pintegration-tests` and `./mvnw -pl pm-mock-runtime verify` pass, and the test run is not weaker: the number of tests before and after is recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-relay,pm-exec,pm-mock-runtime verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- Dependencies: the operator approved on 2026-10-30 exactly `de.cuioss.test:cui-test-generator` and `de.cuioss.test:cui-test-juli-logger`, test scope, in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`. Nothing else is added: not `cui-test-value-objects`, not `cui-test-mockwebserver-junit5`, not `org.jspecify:jspecify` in a module that lacks it, no plugin. An addition the build seems to need is a blocker for the operator.
- Logging: one `LogMessages` class per module that logs; no repository-wide holder; a module that does not log gets none. Identifier numbers are unique inside a class. Exception first in every call; `%s` placeholders, never `{}`.
- Behaviour is unchanged: no production logic changes except the two log calls and the annotations. A test that fails after conversion exposes either a wrong assumption of the generated value or a real defect; report it, never loosen the assertion to make it pass.
- Write-set discipline: the main checkout of this repository is never edited by the run (a repeat slip of earlier runs); work only in the plan's worktree.
- No browser check and no terminal check: the plan changes no rendered output. No ports are assigned.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: no test class of the repository uses `Generators` or `@EnableGeneratorController` (0 of 267 test files in `pm-api`, `pm-exec`, `pm-relay`, `pm-operator`, `pm-mock-runtime`) — read by a repository-wide search at HEAD 7e2b4b61
- OBSERVED: `pm-operator/pom.xml` declares `cui-test-generator` and `cui-test-juli-logger` (test scope) and `cui-java-tools`; `pm-mock-runtime/pom.xml` declares `cui-java-tools` only; `pm-api`, `pm-exec`, `pm-relay` declare none of the CUI artifacts — read at the five `pom.xml` files
- OBSERVED: the only `CuiLogger` in the repository is `pm-mock-runtime/src/main/java/de/planmarshall/mock/junit5/MockRuntimeExtension.java`; its two `ERROR` calls (setup failure, close failure) pass string literals, the other calls are `DEBUG` — read at lines 57 to 134
- OBSERVED: `@NullMarked` is on 4 of 49 `package-info.java` files (3 in `pm-operator`, 1 in `pm-mock-runtime`); `pm-api`, `pm-exec` and `pm-relay` carry no JSpecify dependency — read at the `pom.xml` files and the package files
- OBSERVED: HTTP in this repository runs over a Unix socket (`pm-api/.../http/UnixSocketHttpClient.java`, tested against `pm-api/src/test/.../FakeRuntimeServer.java`) or tests the repository's own server (`pm-mock-runtime/.../http/MockHttpServer.java` with `java.net.http` in its tests); no external HTTP service is faked — read at those files
- HYPOTHESIS: converting the 11 tests of the two modules to generated input data does not change any assertion's expected value — confirm/refute at `pm-relay/src/test/java/` and `pm-exec/src/test/java/` (verify-at-outline); a test whose expected value derives from a literal input is rewritten to derive it from the generated value, and the case is reported
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; the test counts (3, 8) are the count of `*Test.java` and `*IT.java` files at HEAD 7e2b4b61 and are re-counted at outline. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `CLAUDE.md` — present at HEAD 7e2b4b61
- OBSERVED: `pm-api/pom.xml` — present at HEAD 7e2b4b61
- OBSERVED: `pm-exec/pom.xml` — present at HEAD 7e2b4b61
- OBSERVED: `pm-relay/pom.xml` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/pom.xml` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/junit5/MockRuntimeExtension.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/junit5/MockRuntimeLogMessages.java` — absent at HEAD 7e2b4b61; created by this plan
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/cli/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/http/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/junit5/MockRuntimeExtensionTest.java` — present at HEAD 07104678; this plan adds the `LogAsserts` test of deliverable 2 beside it (converting the module's 43 test classes is `PLAN-87`)
- OBSERVED: `pm-exec/src/test/java/` — present at HEAD 7e2b4b61
- OBSERVED: `pm-relay/src/test/java/` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/tui/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/layout/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/dialogue/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/system/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/actions/package-info.java` — present at HEAD 7e2b4b61
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/reports/package-info.java` — present at HEAD 7e2b4b61

## Dependencies and Sequencing

- Depends on: none (`PLAN-76` shipped)
- Overlaps with: `PLAN-80` (`CLAUDE.md`: both add a paragraph under § Code Standards, in different paragraphs; one after the other or merge by hand); the running terminal plans touch `pm-operator` packages (`dialogue/`, `actions/`) but only their `package-info.java` is shared in principle; no test of `pm-operator` is touched here. Highest priority: emitted at the operator's request as the next plan.
- Adjacent to: `PLAN-78` (`pm-api` tests) and `PLAN-79` (`pm-operator` tests) follow it

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-77-cui-standards-foundation.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

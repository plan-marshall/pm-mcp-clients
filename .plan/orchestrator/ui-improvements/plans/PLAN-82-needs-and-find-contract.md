# PLAN-82: Shared: The Runtime Contract and Mock for Order, Size, What Waits and Finding Work — With the Design of the Two Views

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

This plan is the one place where the pm-api contract and the mock runtime change for the work packages of `pm-mcp-clients` PR #278 (merged 2026-10-30) — `overview-order-and-size` (UC-PLN-4, UC-PLN-1) and the shared half of `needs-and-find` (UC-PLN-7 and UC-PLN-8, both `AGREED` since `plan-marshall-documentation` PR #135); PM-ARCH-11. It absorbs the former `PLAN-81` (the contract and mock half of `overview-order-and-size`), which touched the same overview records and the same mock engine; the structure review of 2026-10-30 merged them. The clients build on a settled contract:

- this plan: the stable order and the plan's size of change in the contract and the mock (no client code), then the two views ("what waits", "go to work by name") written into the shared design and accepted by the operator, then the contract plus the mock runtime for `GET /api/v1/machine/needs-operator`, `GET /api/v1/find?q=`, the machine's `needs_operator` summary with the new kinds `credential` and `cleanup_due`, and the runtime-wide event `needs_operator.changed` for the machine;
- `PLAN-34` (terminal overview keeps the served order and shows the size of change), `PLAN-52` (web running work keeps the served order), `PLAN-55` and `PLAN-54` (web plan page head and completion report show the size), `PLAN-36` (terminal `needs` verb and badges), `PLAN-84` (terminal `find` command), `PLAN-49` (web "what waits" route), `PLAN-46` (web search behind the top bar's search button) and `PLAN-83` (work list beside a plan) consume the contract. They are re-scoped to read the served order, size, list and matches instead of deriving them on the client.

The runtime's order is the contract: items that block running work first, then the oldest; a client keeps it. Find answers over running and ended work, running first, ended marked, read from the archives by the runtime, so no client loads an archive to search it.

## Deliverables

1. **The stable order and the size of change, in the contract and the mock**
   - What: the mock runtime serves projects, epics and plans of the overview resource in the stable order the specification states (`runtime-model/01-processes-and-transport.adoc#overview-resource`: needs the operator first, then `created_at` newest first; projects by the start of their oldest active work; activity moves no row); `pm-api` carries `change` on the plan and on the completion report (files, insertions, deletions, unplanned count; absent before the first capture) and the catalogue key `plan.change.size` in English and German as `doc/design/shared/plan-view.adoc` states it (`plan_view.{en,de}.properties`, plural forms, no unplanned part at zero); the mock serves a size for the plans of the scenarios that have a measured one and none for one that has none. No client file is edited here: the terminal client drops its own sort and shows the size in `PLAN-34`, the web running work keeps the served order in `PLAN-52`, the web plan page head and the completion report show the size in `PLAN-55` and `PLAN-54`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A mock test: an activity event changes no row's position; a row moves when its item starts, ends, or starts or stops needing the operator (evidence order: `test-first`)
     - A contract test (record round trip, absent size) and a catalogue test (both languages, plural forms, no unplanned part at zero) (evidence order: `test-first`)
     - `./mvnw -pl pm-api verify` and `./mvnw -pl pm-mock-runtime verify` pass (evidence order: `none`)
2. **The two views in the design, accepted**
   - What: write the design of the two views, in the shared design and in the web app's design: "what waits" (the list, each item with what it is, where it belongs as project › epic › plan, since when, what it blocks, and the act that resolves it; nothing waits: the list says so and names the running work; an item resolved in another client leaves with a quiet notice; reports are no items; the terminal's badge counts and the one act that lists every item) and "go to work by name" (a name, matches by kind with running work first, ended work marked, no match says so and keeps the name; the web app's search button of the top bar, disabled today with `web.frame.search.pending`; the terminal command). Web: the "what waits" view is a route of its own and a destination of the rail with its count; whether it is also the start page is decided here, with the operator, and written down (`PLAN-49` currently assumes the start page). Texts only from the message catalogue; a title is plain text. Ask the operator to accept the design before the contract is built.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The design documents exist, link from `doc/design/README.adoc`, and the operator's acceptance of both views is recorded in the progress record with the answer verbatim (evidence order: `none`)
     - The decision on the start page is recorded, and the specs `PLAN-36`, `PLAN-46`, `PLAN-49` and `PLAN-84` are listed with the lines of this design they rely on (evidence order: `none`)
3. **The what-waits and find contract and the mock**
   - What: `pm-api` carries the machine's `needs_operator` summary (kinds `credential` and `cleanup_due` join the closed list in its order: after `setup`), the list resource with its page records (kind, link, project/epic/plan each with id and title, since, what it blocks), the find resource with its match records (kind, id, title, where it belongs, ended), the path constants, the `needs_operator.changed` event for the machine, and the catalogue keys `overview.needs.credential` and `overview.needs.cleanup_due` (English and German) of the design. The mock runtime serves the list in the runtime's order for the scenarios (one with items of each kind, one with nothing waiting), serves find over running and ended work of its fixtures (case-insensitive part of an id or title; a pull request number finds its plan; an empty text is refused), and sends `needs_operator.changed` when a state change adds or removes an item.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - Contract tests: round trips of the records, the new kinds in the closed list and its order, the catalogue keys in both languages (evidence order: `test-first`)
     - Mock tests: the list in the runtime's order across projects; nothing waits; an item resolved leaves the list and the event is sent; find matches id, title and pull request number, running first, ended marked, no match, empty text refused (evidence order: `test-first`)
     - `./mvnw -pl pm-api verify` and `./mvnw -pl pm-mock-runtime verify` pass; the `pm-api` test jar signatures other modules use are unchanged or listed (evidence order: `none`)
     - The rows of the packages `overview-order-and-size` and `needs-and-find` in `doc/plans/README.adoc` stay `open`: the clients finish them, and the last client plan of each package sets its row to `done` (`overview-order-and-size`: `PLAN-34`, `PLAN-52`, `PLAN-55`, `PLAN-54`; `needs-and-find`: `PLAN-36`, `PLAN-84`, `PLAN-46`, `PLAN-49`) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-api verify`, `./mvnw -pl pm-mock-runtime verify`, `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- The daemon is not in this repository: the specification (`plan-marshall-documentation`, `runtime-model/01-processes-and-transport.adoc#needs-operator-list` and `#find-work`) is the contract; the mock follows it, and a point the specification leaves open is reported, never decided silently.
- Behaviour of the existing clients is unchanged by this plan: no client file is edited except where a compile needs it; the clients adopt the contract in their own plans.
- Every new text goes through the message catalogue (`overview.{en,de}.properties` in `pm-api`).
- Browser and terminal checks do not apply: no rendered output changes. No ports are assigned.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree. The deliverable 1 documents are written in this repository (`doc/design/`); a change to the sibling repository `plan-marshall-documentation` is a question to the operator, never made silently.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: UC-PLN-7 and UC-PLN-8 are `Status:: AGREED` in `plan-marshall-documentation/doc/use-cases/03-plans.adoc` (lines 317 and 339), and both name `needs-and-find` as the package that designs the web app's views — read at `53a514e`
- OBSERVED: `NeedsOperatorKind` lists `decision`, `waiver`, `waits_for_you`, `stalled`, `condition`, `integrity_pause`, `held`, `landing_failed`, `findings`, `setup` and no `credential` or `cleanup_due` — read at `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java` at HEAD 07104678
- OBSERVED: the web app's search button is disabled with `web.frame.search.pending` — stated by the ThinkRail analysis, shipped by `PLAN-39` (PR #274)
- OBSERVED: the specification serves the machine's summary on `GET /api/v1/machine`, the list on `GET /api/v1/machine/needs-operator` (paged) and find on `GET /api/v1/find?q=` (paged, empty text refused) — read at `plan-marshall-documentation` `53a514e`
- HYPOTHESIS: the closed list's order in the specification puts `credential` and `cleanup_due` after `setup`, so `NeedsOperatorKind` gets them last — confirm/refute at the specification section "What Needs the Operator, at a Glance" (verify-at-outline)
- HYPOTHESIS: `MachinePaths` is where the new resource paths belong and the mock's machine resource has no `needs_operator` field today — confirm/refute at `MachinePaths.java` and `MockRuntimeEngine` (verify-at-outline)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every claim above; `PLAN-34` (display layer in `pm-api`) and `PLAN-81` (the stable order, the same mock) edit neighbouring files. A refuted claim re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/ActivePlanSummary.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/report/PlanCompletionReport.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorKind.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/NeedsOperatorChangedEvent.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/MachinePaths.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/overview/` — present at HEAD 07104678; new records for the list and the matches are created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 07104678
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 07104678
- OBSERVED: `doc/design/README.adoc` — present at HEAD 07104678
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 07104678
- OBSERVED: `doc/design/web-app/` — present at HEAD 07104678; the new design documents are created by this plan
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-08` (`use-case-documents`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-34` (`pm-api` display layer; `overview/` records), `PLAN-06` (shipped)
- Adjacent to: `PLAN-36`, `PLAN-84`, `PLAN-49`, `PLAN-46` (consume the contract; they wait for this plan)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-82-needs-and-find-contract.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

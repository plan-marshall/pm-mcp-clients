# PLAN-86: Shared: One Text Set per API Error Code (Title, Cause, Fix) for Both Clients

epic: ui-improvements
workstream: WS-01

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Write the texts of every error code of the local API once. A review of the staged plans (2026-10-30) found the same texts planned three times: `PLAN-09` (`displayError(code)` of the web display layer), `PLAN-45` (`error.<code>.title`, `.cause`, `.fix` for the web) and `PLAN-30` (`operator.refusal.<code>`, the terminal's three-line refusal). This plan owns the one set; the clients only render it:

- the catalogue area `error` in `pm-api` (`error.en.properties`, `error.de.properties`): for each member of `ApiErrorCode` (`pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java`, 43 codes at HEAD 07104678) the keys `error.<code>.title` (what happened, one line), `error.<code>.cause` (why) and `error.<code>.fix` (what to do next), and the keys `error.unknown.title`, `error.unknown.cause` and `error.unknown.fix` for a code the client does not know;
- a build check that every member of `ApiErrorCode` has the three keys in English and German, and that no key of the area names a code that is not a member;
- a small renderer-independent lookup in `pm-api` (`ErrorTexts.title(code)`, `.cause(code)`, `.fix(code)`, with the unknown fallback), used by the terminal's `RefusalRenderer` (`PLAN-30`) and, through the display layer, by the web app (`PLAN-09`, `PLAN-45`).

The texts follow `doc/design/shared/texts-and-languages.adoc`: plain words, no code, no HTTP status, no class name in the title, a fix that names an act the person can do.

## Deliverables

1. **The texts and the lookup**
   - What: the area `error` for all 43 codes and the unknown fallback, in both languages, and `ErrorTexts` over `MessageCatalogue`; the terminal-only wording of `operator.refusal.*` (if any key exists at HEAD) is folded into this set and removed, never kept beside it.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-lombok, pm-dev-java:javadoc, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-dev-java-cui:cui-logging
   - Done when:
     - A catalogue test: every `ApiErrorCode` member has title, cause and fix in `en` and `de`; no key of the area names a non-member; the unknown fallback exists (evidence order: `test-first`)
     - A lookup test: a member, a wire string that is no member (unknown fallback), a null code (evidence order: `test-first`)
     - A review of the German and English wording against `texts-and-languages.adoc` is recorded in the progress record: no raw code, status or class name in any title (evidence order: `none`)
     - `./mvnw -pl pm-api verify` passes (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-api verify`, `./mvnw -pl pm-operator verify -Pintegration-tests`, `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- CUI standards (`CLAUDE.md`, `PLAN-77`): a test that takes input data draws it from `Generators` with `@EnableGeneratorController` (the expected value of a contract stays literal); a log call at INFO or above goes through a `LogRecord` of the module's own `LogMessages` class (one per module that logs, never one for the repository) with a `LogAsserts` test; `@NullMarked` applies in a module that declares JSpecify (`pm-operator`, `pm-mock-runtime`); until `PLAN-77` has landed the test-scope CUI dependencies are missing in `pm-api`, `pm-exec`, `pm-relay` and `pm-mock-runtime`, and a test there follows plain JUnit and reports the departure, never adds the dependency itself.
- Java 25, Lombok, records; no generic `Exception`/`RuntimeException`; JUnit 5 only, no Mockito (CLAUDE.md).
- The set of codes is the closed list of the Runtime Model specification as `ApiErrorCode` holds it; a code added to the enumeration later fails the build check until its texts exist.
- The wording of a text is the operator's to read: the progress record lists the 43 titles in both languages so the operator can review them in the pull request.
- No client file is edited except where a compile needs it; `PLAN-30`, `PLAN-09` and `PLAN-45` adopt the set in their own plans.
- Browser and terminal checks do not apply: no rendered output changes. No ports are assigned.
- Write-set discipline: the main checkout of this repository is never edited by the run; work only in the plan's worktree.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: `ApiErrorCode` is the closed enumeration of the error codes of `/api/v1`, 43 members counted by their wire literal — read at HEAD 07104678
- OBSERVED: no `error.*` or `operator.refusal.*` key exists in `pm-api/src/main/resources/messages/` — searched at HEAD 07104678 (`git grep`)
- OBSERVED: three staged plans plan the texts of the error codes: `PLAN-09` D1 (`displayError(code)`), `PLAN-45` D1 (`error.<code>.title/.cause/.fix`), `PLAN-30` D2 (`operator.refusal.<code>`) — read in the staged specs on 2026-10-30
- HYPOTHESIS: some of the 43 codes are never shown to a person (they refuse a malformed request of a client, not a user act), so their cause and fix are short and generic — confirm/refute per code at the specification table and report the list (verify-at-outline)
- Verify-first clause: before the deliverable, re-read `ApiErrorCode`, the message catalogue directory and the three consumer specs at the current HEAD and confirm or refute every claim above. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java` — present at HEAD 07104678
- OBSERVED: `pm-api/src/main/resources/messages/error.en.properties` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/error.de.properties` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/resource/ErrorTexts.java` — absent at HEAD 07104678; created by this plan
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/resource/ErrorTextsTest.java` — absent at HEAD 07104678; created by this plan
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-06` (`shared-display-vocabulary`, shipped), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-82` (`pm-api` catalogue, other areas), `PLAN-34` (`pm-api` display layer)
- Adjacent to: `PLAN-09`, `PLAN-45` (web consumers), `PLAN-30` (terminal consumer): all three wait for this plan

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-86-shared-error-texts.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

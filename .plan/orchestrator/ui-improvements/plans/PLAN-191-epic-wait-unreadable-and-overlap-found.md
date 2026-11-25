# PLAN-191: The Epic's Wait Reason `unreadable` and the Removal of `epic.overlap_found`

epic: ui-improvements
workstream: WS-04

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged by the orchestrator on
> 2026-11-25 from the inbox message `doc-review-2026-11-24-001` (§ 1, the two items with no surface shared with a running
> plan), on the operator's word "analyze landed, land and emit". The rest of the message (the `over_size` removal, the off-track
> signals, the outline's `added_dependencies`, the phase `sanity-check`, the two operator questions) is recorded in `epic.md`
> and staged after PLAN-182 and PLAN-55 land, because it shares files with both.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief. **It adds no dependency.**

## Objective

The design of the clients (#570, `20f971fe`) and the engine's specification (pm-mcp-core #607) dropped re-grounding and the
epic overlap re-check: a mechanism that predicted a fact is replaced by observing it. Two client contracts still carry them.
The epic's wait reason `stale` ("its row is stale", "waits for a fresh cut") becomes `unreadable`: a claim section of the row's
specification cannot be read. The report kind `epic.overlap_found` is removed; `epic.waits_overlap` stays. While the wait
reasons are touched, the existing defect that the terminal's overview reads a `held` (and today a `stale`) row as "can start"
is fixed.

## Deliverables

Read first, at the then-current HEAD: `doc/design/shared/work-actions.adoc` § the per-row reasons of *Next plan* (`:222`),
`doc/design/terminal-client/19-actions.adoc:190`, `doc/design/web-app/epic-view.adoc`, `doc/design/shared/reports.adoc`, and the
engine's specification of the reasons in `../pm-mcp-core/doc/specification/` (read-only; search `unreadable` and
`overlap_found`). The design is the source of the texts; a text the design does not name is added to the design first, in the
same pull request.

1. **The wait reason `unreadable` replaces `stale`, and every reason is shown**
 - What:
   - Design first: the design names `unreadable` among the reasons (`work-actions.adoc:222`, `19-actions.adoc:190`) but gives
     no text for it, and gives no overview text for `held`. Add the texts to the design where the other reasons' texts stand
     (the reasons' example lines in `work-actions.adoc`, and the web epic view's wait texts in `epic-view.adoc`), in the voice of
     the existing ones; the meaning is "a claim section of the row's specification cannot be read".
   - Contract: `EpicWaitReason.STALE("stale")` becomes `UNREADABLE("unreadable")` (`pm-api/.../control/EpicWaitReason.java:28`);
     the class Javadoc names it. No alias: `fromValue("stale")` answers `null` afterwards (pre-1.0, no shim).
   - Texts (en and de): `epic.views.wait.stale` → `epic.views.wait.unreadable` (`epic_view.*.properties:59`),
     `operator.actions.epic.reason.stale` → `operator.actions.epic.reason.unreadable` (`operator.*.properties:419`), worded as
     the design now says. Add `overview.epic.waits.held` and `overview.epic.waits.unreadable` (`overview.*.properties`, beside
     `:137-142`).
   - Rendering: `EpicViewFormatting.waitReasonLine` (`pm-operator/.../epicview/EpicViewFormatting.java:152`) and
     `EpicView.svelte` `waitLabel` (`pm-web-app/src/lib/views/EpicView.svelte:412-413`) switch on `unreadable`.
     `EpicViewFormatting.waitText` (`:111-131`) has no case for `held` or `stale`, so such a row reads "can start": it gains
     `held` and `unreadable`, and its `default` no longer turns an unknown non-`none` reason into "can start" (state what it
     shows instead, from the design; if the design says nothing, ask the operator with the question tool).
   - Tests: the value list in `ControlCataloguesTest:57` and the reasons in `EpicViewCatalogueTest:266` follow; a negative
     control asserts `EpicWaitReason.fromValue("stale")` is `null` and `parse("stale")` throws. `waitText` gets a test per reason
     (`held`, `unreadable`) that fails if the row reads "can start". `EpicView.test.ts` (or the existing web test of
     `waitLabel`) covers `unreadable` and asserts no `stale` case is left.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-frontend:javascript, pm-documents:ref-asciidoc
 - Done when:
   - `git grep -n -E "\bSTALE\b|wait\.stale|reason\.stale|'stale'" -- pm-api pm-operator pm-web-app pm-mock-runtime` names no
     wait-reason use (a `stale` of another meaning, e.g. the lesson retirement reason in `client.ts`, stays and is named in the
     landing message). Evidence order: `none`.
   - The negative control and the two `waitText` tests fail before the change and pass after it. Evidence order: test-first.
   - The design texts for `unreadable` and the overview's `held`/`unreadable` exist and the catalogue texts match them word
     for word (en; de translated). Evidence order: `none`.
2. **The report kind `epic.overlap_found` is removed**
 - What: `ReportKind.EPIC_OVERLAP_FOUND` (`pm-api/.../report/ReportKind.java:24`) and the Javadoc count (`:15`, 16 → 15);
   the texts `report.subject.epic_overlap_found` (`reports.*.properties:37`) and `overview.epic.event.overlap_found`
   (`epic_view.*.properties:18`); the tests that pin them (`ReportRecordsTest:45-46` 16 → 15 and its display name,
   `EpicViewCatalogueTest:73,216-218`). `EPIC_WAITS_OVERLAP` and its texts stay. Sweep the mock runtime's scenarios, the web
   app, the terminal client and the e2e tests for `overlap_found` and remove every use (none found at `2b6939fe` outside the
   files named; re-read at the start). A negative control asserts `ReportKind.fromValue("epic.overlap_found")` throws.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core
 - Done when:
   - `git grep -n -i "overlap_found" -- ':!doc/plans' ':!.plan'` names nothing outside the design's own history notes (if
     any), each named in the landing message. Evidence order: `none`.
   - The negative control fails before the change and passes after it. Evidence order: test-first.
3. **Traceability and the work package index**
 - What: `doc/Traceability.adoc` and `doc/ImplementationWatch.adoc`/`doc/implementation-watch/` name neither `stale` as a wait
   reason nor `epic.overlap_found` afterwards, where they name them today; `doc/plans/README.adoc` records the work package
   per `../plan-marshall-documentation/doc/developer/project-setup.adoc` (_Planning the Work_) if the repository's practice
   asks for one for a change of this size (read the practice; do not invent a package file it does not ask for).
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-requirements:traceability
 - Done when:
   - The traceability links stay valid (the build's link check passes). Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-operator,pm-web-app -am verify` while working, then
  the full `./mvnw verify`, `./mvnw verify -Ppre-commit` (review every rewrite; it is known not to be idempotent and touches
  files outside this plan: revert those), `-Pcoverage` (minimum 80 % per module) and
  `PM_MOCK_WEB_PORT=21910 PM_WEB_PORT=21911 ./mvnw -pl pm-web-e2e verify -Pe2e -am` at the head. main's own Playwright reds
  (`designer`, `projects`, `theme-readability`, 9 tests at `52aa9e4b`) are named and not counted against this plan; any other
  red is.
- Ports of this plan: mock web port `21910`, app port `21911` (`AGENTS.md` § Ports). Pass them to every mock, app and e2e
  start; other plans run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port
  that is in use is a blocker, never a reason to take another port.
- Browser check (`AGENTS.md` § Browser checks, Playwright MCP): the web epic view with a row waiting `unreadable` and one
  `held`, `de` and `en`, before and after, snapshots recorded in the progress record. If the mock has no row waiting
  `unreadable`, add one to the scenario that already carries the other reasons rather than a new scenario. If the Playwright
  MCP server is not available, the browser check is a blocker, never replaced by a unit test.
- **No new dependency or plugin.** Java 25, no generic `Exception`/`RuntimeException` caught or thrown in production code,
  `CuiLogger` through `LogMessages`, JUnit 5 only (no Mockito, PowerMock or Hamcrest), input test data from `Generators` with
  `@EnableGeneratorController`, contract values literal. No `{@html}`, no timer wait in a test. Pre-1.0: no shim, no alias.
- ZERO Sonar findings on the new code of the pull request; state `sonar_open_new_issues=N` in the landing message (`unknown`
  for a failed read, never `0`). No `NOSONAR`. A dismissal only on the operator's selection.
- The merge rule: the run merges only when every review thread has a reply and is resolved and every review body item is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and
  review-body items from `ci pr comments`, never from memory. The run merges the pull request itself once these gates hold
  (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body
  carries no generator footer. If CodeRabbit refuses on quota, wait out the window and trigger once more; ask the operator
  only if it still refuses.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. The branches
  of PLAN-182, PLAN-55 and the parked PLAN-42 are never touched, rebased or merged by this run.

## Claim Labels

- OBSERVED: `EpicWaitReason` declares `STALE("stale")` at `:28` and `HELD("held")`; `ControlCataloguesTest:57` pins the list
  `none, dependency, lock, scope, overlap, stale, held` — read at `2b6939fe`
- OBSERVED: `EpicViewFormatting.waitText` switches on `dependency`, `lock`, `scope`, `overlap` and defaults to
  `overview.epic.waits.none` ("can start"); `overview.*.properties:137-142` has no `held` or `stale` text — read at `2b6939fe`
- OBSERVED: `EpicViewFormatting.java:152` and `EpicView.svelte:412-413` render `stale` through `operator.actions.epic.reason.stale`
  and `epic.views.wait.stale` — read at `2b6939fe`
- OBSERVED: `ReportKind.java:24` declares `EPIC_OVERLAP_FOUND("epic.overlap_found")`, Javadoc "16 report kinds" at `:15`;
  `ReportRecordsTest:46` asserts 16; `fromValue` throws for an unknown value — read at `2b6939fe`
- OBSERVED: outside `pm-api` (`ReportKind`, `reports.*`, `epic_view.*` and two tests), no file under `pm-mock-runtime/src`,
  `pm-web-app/src`, `pm-web-e2e/tests` or `pm-operator/src` names `overlap_found` or a `stale` wait reason — read by `git grep`
  at `2b6939fe`
- OBSERVED: `doc/design/terminal-client/19-actions.adoc:190` names the reasons `dependency, lock, scope, overlap, unreadable,
  held`; the design gives no text key for `unreadable` (the inbox message `doc-review-2026-11-24-001`, corroborated by reading
  `work-actions.adoc:222`) — read at `2b6939fe`
- OBSERVED: none of the running PLAN-182 (`fix/182-…`) or PLAN-55 (`feature/55-…`) changes a file of this plan's surface; the
  parked PLAN-42 (`feature/42-…`) changes `operator.{en,de}.properties` (other keys) — read by `git diff --name-only
  origin/main...origin/{branch}` on 2026-11-25
- HYPOTHESIS: the engine (pm-mcp-core) emits `unreadable` and no longer `stale` or `epic.overlap_found` — confirm/refute in
  `../pm-mcp-core/doc/specification/` (read-only) at the start; a refutation is reported to the operator before D1/D2 are
  made, never worked around
- Verify-first clause: before each deliverable re-read the named files at the then-current HEAD; a refuted claim re-scopes the
  deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `doc/design/shared/work-actions.adoc` — present at HEAD 2b6939fe
- OBSERVED: `doc/design/web-app/epic-view.adoc` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/control/EpicWaitReason.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/report/ReportKind.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/epic_view.en.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/epic_view.de.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/control/ControlCataloguesTest.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/EpicViewCatalogueTest.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/report/ReportRecordsTest.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/epicview/EpicViewFormatting.java` — present at HEAD 2b6939fe
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 2b6939fe

## Dependencies and Sequencing

- Depends on: nothing in the queue (the design #570 and pm-mcp-core #607 are merged).
- Overlaps with: the parked `PLAN-42` (`operator.{en,de}.properties`, other keys; whichever merges second rebases). No running
  plan shares a file. The rest of `doc-review-2026-11-24-001` § 1 and § 2 is staged after PLAN-182 and PLAN-55 land
  (`client.ts`, `MockRuntimeEngine`, `ProjectExplorerView`, `running-work.json`, `IntentScenarioTest`, `plan_view.*`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-191-epic-wait-unreadable-and-overlap-found.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits
NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger
write. The session reports its outcome through its PR and its inbox message
(`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

# PLAN-165: Lists Derived From One Definition, Playwright Green and Web Coverage in Sonar (Follow-Up of the Build-Time Plan)

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-14 (`analyze`
> of `analyze-fix-build-time-002.md` and `-003.md`: "stage one follow-up") from the items the build-time plan deferred on purpose when it landed
> as #434, #435 and #436. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Three lists are kept by hand next to the code or the data they must agree with, so a new member leaves the code and its tests green while the
two disagree: the keys a terminal prompt offers, the kinds `cleanup` prints durations for, and the event names the web views refresh on. Each
becomes one definition that the code and its tests both read. Behaviour is unchanged: no text, key, exit code or event the user sees moves.
Beside that, the web app's end-to-end suite is made known green and its unit coverage is brought under Sonar's coverage condition: the build-time
plan (`analyze-fix-build-time`, closed 2026-11-14) left `sonar.coverage.exclusions=**/*` in `pm-web-app/pom.xml` because the web branch
coverage (78.7 % at its last measurement) is below the 80 % minimum and CI's Sonar job does not run the coverage run. Folded into this spec on the
operator's word of 2026-11-14 (`analyze` of `-004` and `-005`: "Fold into PLAN-165"); the scope-bloat guard is accepted unsplit on that word, see
the epic Decisions.

## Deliverables

Read first:
- `analyze-fix-build-time-002.md` items 2 and 3, and `-003.md` item 1 (archived under `inbox/archive/analyze-fix-build-time/`; the message bodies are the sender's account, the claims below are re-checked).
- `pm-operator/src/main/java/de/planmarshall/operator/EpicDecompositionDialogue.java` (`offered`, `notOffered`, the accepted-key branches).
- `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` (`durationsText`) and `pm-api/src/main/java/de/planmarshall/api/system/CleanupKind.java`.
- `pm-web-app/src/lib/stores/runtime.ts` (`PROJECT_EVENTS`), `pm-web-app/src/lib/views/StartView.svelte` (`ENTRY_REFRESH_EVENTS`), `ProjectExplorerView.svelte`, and the `EventName` catalogue of pm-api.

1. **The offered keys of `EpicDecompositionDialogue` come from the accepted set**
 - What: The list a prompt prints as offered (`offered(...)`, and the literal `"k, e, b, ..."` of the read-back prompt) is derived from the same
   collection of keys the prompt's branches accept, so adding or removing an accepted key changes the offered line with no second edit.
   The printed text of every prompt stays byte-identical for the existing keys, in `en` and `de`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test per prompt reads the accepted set and the offered line from the dialogue and fails when one key is in one and not the other (evidence order: `test-first`).
   - The dialogue's transcripts are byte-identical before and after (evidence order: `none`).
2. **`CleanupCommand.durationsText` derives its kinds from `CleanupKind`**
 - What: The hand-written `List.of("plans", "epics", "logs", "reports", "unenrolled")` and the parallel list of days are replaced by a
   mapping from `CleanupKind.values()` to the displayed label and to the duration it reads. The wire values (`plan`, `epic`, `logs`,
   `read_reports`, `unenrolled_project`) differ from the displayed labels (`plans`, `epics`, `logs`, `reports`, `unenrolled`), so the mapping
   is explicit and is total: a `CleanupKind` with no label is a test failure, not a missing column.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test over `CleanupKind.values()` fails when a constant has no label or no duration (evidence order: `test-first`).
   - The printed durations line is unchanged for the five kinds in `en` and `de`, against the existing `CleanupCommandTest` cases (evidence order: `none`).
3. **One authoritative refresh-event set per stream in the web app**
 - What: `ProjectExplorerView` (three project event names), `StartView` (`ENTRY_REFRESH_EVENTS`), `stores/runtime.ts` (`PROJECT_EVENTS`) and
   the `EventName` catalogue of pm-api each keep a list of event names. Export one set per stream from one module, use it in the views'
   predicates and in the parameterized tests (`PlanView.test.ts` has the same table), so a new event name is one edit and a test that names a
   stale member fails.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Each view's refresh predicate and its test read the exported set, and a test fails when an event name in the set is not in the `EventName` catalogue (evidence order: `test-first`).
   - `pm-web-app` `lint` and `test:ci` pass and the views refresh on exactly the events they refreshed on before (evidence order: `none`).
4. **The Playwright suite is run and green, or its failures fixed**
 - What: Run `./mvnw verify -Pe2e -pl pm-web-e2e -am` FIRST, at the head, before any other web deliverable, and record the result. The sender's run of
   2026-11-13 failed in the `run-playwright` step, and two specs were recorded as failing on `main`: `showroom.spec.js` "opens the start entry with
   its sources, refinement chat, and file tree" (`StartView.openChat` posts the entry act `chat` and renders `ChatPanel` from `state.chat`, which the
   chat work moved into `ChatRegistry`) and `designer.spec.js` (intermittent: the test fills `workflow-text` before `loadAll()` resolves and
   `openUnit(units[0])` then overwrites the text and resets `dirty`). Whether #428 or #436 fixed them is unknown. Fix what still fails: a test that
   waits on a condition the code produces (an awaited event or `waitFor`), never an arbitrary timer; a spec that tests a behaviour the code no
   longer has is rewritten to the new behaviour, with the reason in the progress record.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The progress record states the result of the first `-Pe2e` run (green, or the failing specs by name), and the run at the head is green (evidence order: `none`).
   - For each spec that failed in the first run, a revert of its fix makes it fail again (evidence order: `test-first`).
5. **Web unit coverage reaches the Sonar job**
 - What: In CI the Sonar job runs `verify -Psonar` (reusable workflow `cuioss-organization`, `reusable-maven-build.yml`), not `-Pcoverage`, so `pm-web-app`
   writes no `target/coverage/lcov.info` there and `sonar.javascript.lcov.reportPaths` points at nothing. Add a module-local Maven profile `sonar` to
   `pm-web-app/pom.xml` that carries the same `npm-test` override as the profile `coverage` (`run test:coverage`), with no change to the CI
   files. `sonar.coverage.exclusions` STAYS in this deliverable: dropping it first would make Sonar report every web source as 0 % covered.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript
 - Done when:
   - `./mvnw verify -Psonar -pl pm-web-app -am` writes `pm-web-app/target/coverage/lcov.info` (evidence order: `none`).
   - The pull request's own Sonar job on this plan reads a web LCOV report (the number of files with coverage data is stated, `unknown` for a failed read, never `0`) (evidence order: `none`).
6. **Web branch coverage at the 80 % minimum, thresholds on, exclusion dropped**
 - What: Branch coverage of `pm-web-app` was 78.7 % (lines 91.3 %, of 8,094 branches) at the sender's last measurement of 2026-11-13, so the gap is
   about 1.3 points; the views still below the threshold were ReportsView, SystemView, EpicView, EpicDecompositionView, CommandPalette,
   CompletionReportView, ProjectOnboardingView, MachineSettingsView, SettingsView, CleanupView and the small components. Re-measure at the head first
   (`./mvnw verify -Pcoverage -pl pm-web-app -am`, run alone, never with `integration-tests`), then add tests chosen for behaviour, not for lines, for the
   least covered branches. Then enable the 80 % thresholds in `pm-web-app/vite.config.ts` (`test.coverage`) and remove
   `sonar.coverage.exclusions=**/*` from `pm-web-app/pom.xml`. A branch no input can reach is not covered by a contrived test: report it (the
   sender's findings name 33 such dead branches and 44 redundant guards project-wide, clustered in `client.ts` and `runtime.ts` for the web).
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - The branch coverage before and after is recorded per changed file, and `./mvnw verify -Pcoverage -pl pm-web-app -am` passes with the 80 % thresholds on (evidence order: `none`).
   - The pull request's Sonar read after the push shows the web sources under the new-code coverage condition, 0 open issues (evidence order: `none`).
   - Deliverable 5 is merged or part of the same pull request: the exclusion is not dropped before the Sonar job reads the web LCOV report.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, `./mvnw verify -Ppre-commit` passes (every rewrite reviewed and committed), and so do `./mvnw -pl pm-operator verify -Pintegration-tests` and the `pm-web-app` build. Run `-Pcoverage` and `-Pintegration-tests` as separate invocations (they under-report together).
- No new dependency or plugin. Behaviour is unchanged: no catalogue text, wire name, key or event moves.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; the expected text of a contract stays literal. Never catch or throw generic `Exception`/`RuntimeException`.
- The quality gate rewrites `@CsvSource({"''"})` to `@ValueSource(strings = {"''"})`, which turns an empty answer into two apostrophes (epic Watch, owner `pm-mcp-parent`): a test that needs an empty answer avoids that form. Revert any file the gate reformats that this plan does not own.
- Web standards of `CLAUDE.md`: no `innerHTML`/`{@html}`, no timer wait in a test.
- Ports of this plan: per `AGENTS.md` § Ports (`20000 + 10 × 165`): mock web port 21650, app port 21651. Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings: 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`). A dismissal is made only on the operator's selection. This is a merge gate.
- The landing verdict is given per Done-when clause, never per deliverable.
- Pull request size: if the changed set passes 100 files, stop and ask the operator to split (CodeRabbit refuses above 100 files). Expected: well under.

## Claim Labels

- OBSERVED: the keys `EpicDecompositionDialogue` offers are built by hand beside the branches that accept them (`offered(...)`, `notOffered(answer, "k, e, b, ...")`) — read at `pm-operator/src/main/java/de/planmarshall/operator/EpicDecompositionDialogue.java:123-215` at `b6c90174`
  - verdict: corroborated | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: offered() at EpicDecompositionDialogue.java:138-139 hand-joins the offered keys; the read-back prompt passes a literal key list at :215 while the accepted branches sit at :208-211
- OBSERVED: `CleanupCommand.durationsText` keeps `List.of("plans", "epics", "logs", "reports", "unenrolled")` beside a parallel list of five durations, and `CleanupKind` wire values are `plan`, `epic`, `logs`, `read_reports`, `unenrolled_project` — read at `b6c90174`
  - verdict: corroborated | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: CleanupCommand.java:755-771 keeps List.of of five labels beside five durations; CleanupKind.java:21-25 wire values differ from the labels
- OBSERVED: the refresh-event sets named in deliverable 3 are on `main` since #436 merged (`StartView.svelte:117` `ENTRY_REFRESH_EVENTS`, `runtime.ts:814` `PROJECT_EVENTS`) — read at `83d9d150`
  - verdict: corroborated | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: git grep at origin/main 83d9d150 finds ENTRY_REFRESH_EVENTS (StartView.svelte:117) and PROJECT_EVENTS (runtime.ts:814) since #436 merged
- HYPOTHESIS: `ProjectExplorerView` selects its three project event names by a literal list that equals `PROJECT_EVENTS` — confirm/refute at `pm-web-app/src/lib/views/ProjectExplorerView.svelte` and `stores/runtime.ts` (verify-at-outline)
  - verdict: unverifiable | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline
- OBSERVED: `pm-web-app/pom.xml` declares one coverage profile, `coverage` (`npm-test` runs `test:coverage`), and no profile `sonar`; `sonar.coverage.exclusions=**/*` is at `:25` and `sonar.javascript.lcov.reportPaths` points at `target/coverage/lcov.info` at `:28`; `vite.config.ts` `test.coverage` (`:169-187`) sets no thresholds — read at `83d9d150`
  - verdict: corroborated | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: git show origin/main:pm-web-app/pom.xml: only profile id coverage at :189, exclusions at :25, lcov path at :28; vite.config.ts :169-187 has no thresholds
- HYPOTHESIS: CI's Sonar job runs `verify -Psonar`, not `-Pcoverage`, so the web app writes no LCOV report there — confirm/refute at `.github/project.yml` (its `maven-profiles-snapshot` is empty) and the reusable workflow `reusable-maven-build.yml` of `cuioss-organization` (outside this repository; verify-at-outline, and re-check on the pull request's own Sonar job)
  - verdict: unverifiable | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline
- HYPOTHESIS: `showroom.spec.js` (start entry opened with its chat) and `designer.spec.js` (race between `loadAll()` and the first fill) still fail on `main` after #428 and #436 — confirm/refute by the first `./mvnw verify -Pe2e -pl pm-web-e2e -am` run (verify-at-outline; the sender's evidence dates from 2026-11-13)
  - verdict: unverifiable | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline
- HYPOTHESIS: web branch coverage is 78.7 % (lines 91.3 %) and the listed views are the ones below the threshold — confirm/refute by `./mvnw verify -Pcoverage -pl pm-web-app -am` at the head (the figure predates the review fixes of #436)
  - verdict: unverifiable | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline
- Verify-first clause: before deliverable 3, re-read the three web files at the then-current HEAD (#436 merged as `83d9d150`; `PLAN-135` may have changed `StartView` since). Before deliverable 2, re-read `CleanupCommand.java` after PLAN-30 lands (it owns the cleanup texts).
  - verdict: unverifiable | checked_at: 83d9d150 | by: ui-improvements/analyze | rescoped: n/a | evidence: process clause

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/EpicDecompositionDialogue.java` — present at `b6c90174`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/CleanupCommand.java` — present at `b6c90174`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/CleanupCommandTest.java` — present at `b6c90174`
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/event/EventName.java` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at `b6c90174`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at `b6c90174`
- OBSERVED: `pm-web-app/pom.xml` — present at `83d9d150`
- OBSERVED: `pm-web-app/vite.config.ts` — present at `83d9d150`
- OBSERVED: `pm-web-e2e/tests/showroom.spec.js` — present at `83d9d150`
- OBSERVED: `pm-web-e2e/tests/designer.spec.js` — present at `83d9d150`
- HYPOTHESIS: new or extended test files beside the sources that deliverable 6 covers, below `pm-web-app/src/lib/views/` and `pm-web-app/src/lib/components/` (swept at outline from the measured coverage report, and every file named in the progress record; a directory is claimed here only so the gate sees the breadth: it overlaps every staged web plan)

## Dependencies and Sequencing

- Depends on: nothing unmerged (#434, #435, #436 are on `main`, `83d9d150`).
- Overlaps with:
  - `PLAN-30` (running): `CleanupCommand.java` and `operator.*.properties`. Start deliverable 2 after it lands, or the second rebases.
  - `PLAN-79` (staged): `pm-operator/src/test/java/` (directory claim). The test sweep rewrites `CleanupCommandTest`; start after it or in a different half.
  - `PLAN-135` (staged): `StartView.svelte` (`chat.ended` refresh) and `PlanView.test.ts`; `PLAN-143`/`PLAN-144`/`PLAN-149` touch `StartView` or the same stores: check at outline.
- Order inside the plan: deliverable 4 (the `-Pe2e` run) first, deliverable 5 (the `sonar` profile) before deliverable 6; deliverable 6 drops the exclusion only after 5 reads the report.
- Sequencing: deliverables 4 and 6 touch the web tests broadly, so this plan is emitted when no web plan runs, or in halves (operator decides at emit). The operator's call of 2026-11-14 stands: not emitted yet.
- Not in this plan (Watches in `epic.md`): D6 build-time report, D11 behavioural review of the new tests, the contract questions, design-document gaps, dead-code leads and the `@NullMarked` gap of `pm-api` (messages `-004` and `-005`).

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-165-derive-lists-from-one-definition.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

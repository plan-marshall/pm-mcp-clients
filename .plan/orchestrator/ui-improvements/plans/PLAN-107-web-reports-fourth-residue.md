# PLAN-107: Web and CLI: What the Reports Third Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off (a NORMAL plan: web, CLI, mock and e2e; the `plan-marshall` form, by the operator's rule of 2026-11-06). Start it AFTER `PLAN-97` has landed (shared files, see Dependencies).
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-06 from the landing analysis of PLAN-105 (`landings/PLAN-105.md`: PR #348 merged with 0 unresolved threads, Sonar 0 and, for the first time, both counts stated by the lane — but the page keeps a "Show N older" control whose next page is wrong, the CLI `reports read --all` still posts the removed `all` form and now marks nothing, two of three CodeRabbit nitpicks declined with false reasons, and a "tracked elsewhere" claim that tracks nothing). One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-105 made the web "Mark all shown read" post exactly the shown ids, but the list it leaves behind pages an offset into a shorter unread list, so reports are unreachable until a reload; it removed the `all` form from the client, the mock and Java while the terminal client still sends it; its count arithmetic breaks on a reload in flight and on a deep-linked report; and its answers to CodeRabbit were partly false. This plan makes the list after a mark-all truthful, the terminal client and the mock agree with the web on "by ids", the count correct in the races, the tests able to fail, and every open item answered where the reviewer reads it.

## Deliverables

1. **The list after "Mark all shown read" is the truth**
   - DECISION (the operator, 2026-11-06, "do as proposed"; no cursor): after the request succeeds the page reloads the FIRST page of the unread list (no cursor contract with the daemon is needed), then shows the real count and the real "Show N older" control; a report that arrived after the list was drawn is in that list and unread.
   - What: (a) `ReportsView.svelte` ~545 only calls `refreshReportsCount()` (the runtime store, not the list); `MockRuntimeEngine.paginateUnread` (~5331) pages the unread list by offset, so after the 5 shown of 7 unread are marked, "Show 2 older" fetches an offset page of a list of 2 and returns nothing (defect 1 of `landings/PLAN-105.md`; CodeRabbit raised it on `:543` and the PR 325 finding "reload the list after the request" asked the same). Reload the first page after success, keep the retained open row beside it, and make the test CLICK "Show N older". (b) A reload that lands before the POST response must not make the response subtract the shown reports again (`ReportsView.svelte` ~529-531: the shown ids are not in `countedReadIds` yet): derive the count from the server's figure after the request, or mark the ids as counted before the request (defect 4, CodeRabbit `:531`; the lane's replies "deep link calls `markRead`" and "tracked for future refinements" were false/empty). (c) The `newlyRead` filter `!retainedReadIds[id]` (~529) excludes the marker this request just set, so a report opened through a deep link (unread on the server) is never counted (defect 5). (d) The rollback (~548): a `report.read` for a pre-marked row followed by a failure must not delete the marker of a read already counted; a row retained before the request must not stay "read" in the list after a failure until the next `showDetail` (defect 6; CodeRabbit `:548` "I leave the decision with you"). (e) `pm-web-e2e/tests/reports.spec.js` ~189-211 "marks all shown reports read and shows empty state" expects the old all-form outcome (no `unread-count`, empty state): rewrite it for the shown-ids rule and run it (Playwright is skipped in CI; the lane runs it locally and cites the result) (defect 13). (f) The two masking mechanisms of the reload-then-filtered mark-all test (`:411` and `:529`: reverting either leaves it green): make the test fail when either is reverted.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: after a mark-all of 5 shown of 7 unread, "Show N older" yields the 2 remaining; a reload in flight does not double-subtract; a deep-linked unread report is counted; a failed request after a `report.read` keeps the counted read; each of the two count mechanisms fails the reload test when reverted (evidence order: `test-first`, one failing run and one passing run per test)
     - The e2e test passes at the head with the run cited and readable from the pull request (evidence order: `none`)
2. **The terminal client and the mock agree on "by ids"**
   - What: `pm-operator` `ReportsCommand.java:210` posts `{"all":true}` for `reports read --all`; the mock now ignores `all` and answers 200 with `marked_count` 0, so the CLI prints "✓ 0 reports marked read." and every report stays unread (defect 2). `doc/design/terminal-client/24-reports.adoc:28` says `--all` marks by the ids of the last listing (or reads the list first and marks what it read): make the code do that, and make the mock REFUSE the removed `all` form with a typed error instead of a silent 200. PLAN-105's spec said to remove the `all` form "if nothing else uses it (git grep first)": something did. CodeRabbit's thread `ReadReportsRequest.java:88` raised this; the lane's reply "orchestrator epics track follow-up CLI tasks" was false (no plan named it until this one). Tests use a stub daemon (`ReportsCommandTest:120`, `PmOperatorProcessIT:308`): add a test against the mock handler's real behaviour for the `--all` path. `PLAN-97` rewrites `OperatorGateScopeTest.java` and `PmOperatorProcessIT.java` (the lines 164 and 308 of `reports read --all` are there): start after it has landed, or rebase on it.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-java:java-core, pm-dev-java:java-null-safety, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - Tests that fail before: `reports read --all` against the mock marks the listed reports read and prints the true count; the mock answers the `all` form with a typed 4xx (evidence order: `test-first`)
     - `git grep -n '"all"' -- pm-operator/src/main pm-mock-runtime/src/main pm-api/src/main` names no mark-all use (evidence order: `none`)
3. **The tests and texts the third review asked for**
   - What: (a) CodeRabbit review `5797372610`, nitpicks the lane declined with false reasons: `ReportsScenarioTest.java` ~143-166 asserts WHICH reports stay unread with a non-prefix selection (requested and unrequested ids), not counts; a client-level fetch test that the bulk-read body is exactly `report_ids` (`client.test.ts` has none: the JS test the lane cited mocks the client); `CompletionReportView.test.ts` ~291-302 compares each heading with the PRECEDING one (`maxLevelSeen` lets h1, h2, h3, h2, h4 pass). (b) The fixture labels in `routes/dev/components/+page.svelte` ~227 and ~232 use the raw theme string: `$t('dev.theme.light')`/`$t('dev.theme.dark')` exist in en and de; add the German assertion (CodeRabbit `VchU`). (c) `report.action.mark_all_read.title` (en, de) says "Mark every loaded report as read; older reports stay unread": say what it does with and without a filter. (d) The N4 contrast test pins four hex values with `toBe`: read `--color-brand` and the neighbour (`bg-subtle`, `bg-surface`, `bg-raised`) from `tokens.css` for both themes including the prefers-color-scheme block, so a palette retune that keeps contrast stays green. (e) The test "tracks unreadCount accurately … (D3e)" ~1679 duplicates "counts a repeated read event only once" ~1076: remove or make it distinct. (f) `completion-report.adoc`: the blank line after the inserted paragraph splits the "The head" bullet list; the loading branch of `CompletionReportView.svelte` prints the same text twice (heading and `Spinner` label); the disabled mark-all button drops keyboard focus in flight (keep it focusable with `aria-disabled` or move focus deliberately).
   - Persona: plan-marshall:persona-module-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
   - Done when:
     - Each changed or new test fails when the code it covers is reverted, with the reverted code named in the pull request body and the failing assertion quoted from a run that exists (evidence order: `mutation-after`)
4. **Every open item is answered where the reviewer reads it, and the claims are true**
   - What: (a) PR 348 threads whose last word is CodeRabbit's: `ReadReportsRequest.java:88`, `ReportsView.svelte:531`, `:543`, `:548`: reply IN each with the commit or the reason after this plan's change. (b) PR 340 threads: `Vcgy`, `VchC`, `WVrR`, `VchU` end on a CodeRabbit non-acceptance: reply after the merge with the merged PR and commit. (c) PR 325: ten threads with zero replies and a PR-level disposition comment (`6680112693`) that lists the inline topics instead of the five body items of review `5782656169` (`choose()` focus, reload after unfiltered mark-all, visible-label assertion, disabled-item keyboard case, `waitFor` on `.report-title`): reply IN each thread. (d) Correct the false statements of PR 348 in a comment on it: "added backward-compatibility constructor" (removed in `9bfc39c4`), "All 10 threads were addressed in PR #325", the nitpick answers (a), (b) of review `5797372610`, and the embellished N5 failure quote. (e) The landing message states `unresolved_review_threads=N` and `review_body_findings_total/handled` as facts lines AND each "handled" is an answer a reader can check.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The pull request lists every thread and body item it answers with the commit or reason, and none of its own is unresolved at the head (list the threads through the GraphQL `reviewThreads` read for your own pull request and for PRs 348, 340 and 325) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only for the gate (CLAUDE.md): `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit` for the Java side (the reactor includes `pm-api`), the web unit tests through the module's npm scripts under Maven (`pm-web-app` `test:ci`), the browser tests through `pm-web-e2e`.
- No new dependency or plugin.
- Code standards (CLAUDE.md): Java 25, no generic `Exception`/`RuntimeException` catch or throw in production code, `@NullMarked` packages, JUnit 5 only, input test data from `Generators` (`@EnableGeneratorController`), `CuiLogger` through `LogRecord` constants, a documented public type. Web standards: ES modules, no `innerHTML`/`{@html}`, no arbitrary timer wait in a test, the npm scripts stay.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21070`, app port `21071`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Every `test-first` clause in your landing message cites a FAILING run and a PASSING run of the named test (a passing log is not a failing run, a ledger commit plus uncommitted tests is not "a failing run at a commit"); every `mutation-after` clause cites the reverted code and a failing assertion from a log that is quoted in the pull request body.
- ZERO Sonar findings (the operator, 2026-11-05): this project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. The pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request (never on a side branch), none dismissed without the operator's word, an undecidable read is a stop ("Wait and resume later"). State `sonar_open_new_issues=N` in the landing message.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait (PLAN-105 re-triggered four times). Handle the findings of EVERY CodeRabbit review that exists and read the review BODIES (Follow-ups, Nitpicks and the outside-diff section) as well as the threads. Reply IN the thread on GitHub; a thread whose last comment is CodeRabbit's is not resolved by you; a reason you give must be TRUE (check the test or the code you point at before you cite it); never write "tracked elsewhere" unless a staged plan or an issue names it. Before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read and state `unresolved_review_threads=N` at the head, and `review_body_findings_total/handled`, as lines of the landing-facts block.

## Claim Labels

- OBSERVED: `ReportsCommand.java:210` posts `{"all":true}`; `24-reports.adoc:28` says `--all` marks by the ids of the last listing; `MockRuntimeEngine.paginateUnread` (~5331, `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/`) pages the unread list by offset; `client.test.ts` holds no `markAllReportsRead` or `report_ids` test; `ReportsView.svelte` ~545 calls `refreshReportsCount()` only; PR 348 has 6 threads, 0 unresolved, PR 340 and PR 325 10 each, 0 unresolved; SonarCloud 0 new issues — read by the orchestrator at `f5ab6dd2` through git, GraphQL and the bot comment on 2026-11-06
- HYPOTHESIS: the real `pm-mcpd` pages the unread list by offset the same way (not readable: another repository, `plan-marshall/plan-marshall-mcp`; the first-page reload does not depend on it); a reload in flight double-subtracts, a deep-linked unread report is never counted, the rollback after a `report.read` deletes a counted marker, `reports.spec.js` ~189-211 fails at the head, the product `RowMenu` popup can open under the sticky `BottomBar` at ≤767 px (PLAN-74 deliverable 3 owns that), the loading branch is announced twice — by a read-only analysing agent's reading at `f5ab6dd2`, not run
- Source: `landings/PLAN-105.md`, `landings/PLAN-102.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD f5ab6dd2
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java` — present at HEAD f5ab6dd2
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java` — present at HEAD f5ab6dd2
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD f5ab6dd2
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ReportsScenarioTest.java` — present at HEAD f5ab6dd2
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/report/ReadReportsRequest.java` — present at HEAD f5ab6dd2
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD f5ab6dd2
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD f5ab6dd2
- OBSERVED: `doc/design/terminal-client/24-reports.adoc` — present at HEAD f5ab6dd2
- OBSERVED: `doc/design/shared/completion-report.adoc` — present at HEAD f5ab6dd2
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.svelte` — present at HEAD ddcab793
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — present at HEAD ddcab793
- OBSERVED: `pm-web-e2e/tests/row-menu.spec.js` — present at HEAD ddcab793
- OBSERVED: `pm-web-e2e/tests/port-guard.spec.js` — present at HEAD bb30d71e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD bb30d71e

## Dependencies and Sequencing

- Depends on: `PLAN-105` (shipped), `PLAN-102` (shipped)
- Overlaps with: `PLAN-97` (running: `MockRuntimeEngine.java`, `reports-and-system.json`, `OperatorGateScopeTest.java`, `PmOperatorProcessIT.java`; start this plan after it has landed), `PLAN-106` (running: `client.ts` is on its list, the lines near `:2881` do not overlap; merge main before landing), `PLAN-45` (deliverable 4 there keeps the rows across a reload: whichever starts second reads the other's landing)
- Should land before: `PLAN-42`, `PLAN-45`, `PLAN-72`, `PLAN-73`, `PLAN-75` (same files)
- Adjacent to: `PLAN-74` (deliverable 3 owns the `RowMenu` popup against the sticky bottom navigation at ≤767 px, the `components.adoc` leftovers and the shared filter rule)

## Amendment 2026-11-07 (PLAN-108 landing)

PLAN-108 merged (`ddcab793`), so this plan may now touch `RowMenu.test.ts`. Add to the reports-and-menu work: (a) a test that a scroll event INSIDE a capped `RowMenu` (not on `window`) keeps its `scrollTop` (`RowMenu.svelte` `place()` clears and re-applies `max-block-size` on every capturing `scroll`: HYPOTHESIS, confirm or refute); (b) `row-menu.spec.js` waits for the condition the menu needs (the report row rendered), not only the `unread-count` element; (c) a `format.test.ts` case for the 30-59 day age (`last month` / `letzten Monat`); (d) the `reports.spec.js` expectation `6 unread reports` fails at base (7 vs 6, PLAN-105 defect 13, reported by the PLAN-108 session): fix the test or the scenario. Surfaces added (same act): `pm-web-app/src/lib/components/RowMenu.svelte`, `pm-web-app/src/lib/display/format.test.ts`, `pm-web-e2e/tests/row-menu.spec.js`.

## Amendment 2026-11-07 (PLAN-111 landing)

Also: (e) the two now-vacuous absence asserts of the removed layout test ids, `ReportsView.test.ts:650-651` and `reports.spec.js:309`, are deleted; (f) the pre-existing prettier drift of `pm-web-e2e/tests/port-guard.spec.js` (lines 2, 24) and `theme-readability.spec.js` (~1159), which fails `pm-web-e2e`'s `quality` (`format:check`), is fixed by running the formatter. Surfaces added (same act): `pm-web-e2e/tests/port-guard.spec.js`, `pm-web-e2e/tests/theme-readability.spec.js`.

## Hand-Off Command

Lane: `plan-marshall` (a normal plan, by the operator's rule of 2026-11-06).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-107-web-reports-fourth-residue.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

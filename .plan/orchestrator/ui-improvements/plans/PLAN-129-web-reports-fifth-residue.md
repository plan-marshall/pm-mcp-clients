# PLAN-129: Web and CLI: What the Reports Fourth Residue Plan Left Open (PLAN-107 Residue)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-10 from
> `landings/PLAN-107.md`. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-107 (#394, `0913d6f3`) made mark-all post the shown ids and reload. Graded against its amended spec it left work open: the rollback
after a failed mark-all is unchanged, the in-flight reload and the retained-row mechanism have no test, the RowMenu scroll guard and three
other amendments were never done, two D3 items are open, `shared/reports.adoc#read` does not say what the page does, `reports read --all`
marks one page without saying so, and the review body of #394 (4 nitpicks, fixed in code) has no answer on the pull request. Close these.
(The theme-block reading of `Button.test.ts` is PLAN-70's new deliverable 3; the Skeleton flash is PLAN-45 D4.)

## Deliverables

1. **Mark-all stays truthful in its races**
 - What:
   - The rollback (`ReportsView.svelte ~558-570`) is the code of `be8c8403:545-554` unchanged: it deletes only `newlyRetainedIds` and ignores
     `countedReadIds`. A mark-all in flight with the detail open, a `report.read` for the retained row, then a failed POST leaves the row
     unread in the list although it was counted read (L3-2). Make a counted read stay read, with a test.
   - The retention mechanism (`loadReports`, `~:393-413`) and a reload in flight before the POST response have no test: pin both so
     each fails when its line is reverted.
   - The comment at `~:506-510` overstates what the race guard guarantees: state what it guarantees.
   - The Skeleton flash is PLAN-45 D4 (running): re-read `ReportsView` after it. If `ReportsView.svelte:387,920` still replaces the list with a
     Skeleton on a mark-all, add the missing test; do not duplicate PLAN-45's change.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Tests: a failed mark-all after a `report.read` keeps the counted read; reverting the retention line or the in-flight reload fails its
     test. Evidence order: `test-first` (one failing run and one passing run per test, quoted in the pull request body).
2. **RowMenu, the age text and the vacuous asserts**
 - What:
   - Amendment (a): `RowMenu.svelte:70-72` `onViewportChange` calls `place()` on every capturing `scroll`, so a scroll INSIDE a capped menu
     re-places it. Add a narrow guard (ignore scrolls whose target is inside the menu) and a test that the menu's `scrollTop` survives. Do not pin
     the literal `z-index: 30` or the `title` item field.
   - (b) `row-menu.spec.js` waits for the report row, not only the `unread-count` element. (c) `format.test.ts`: the 30-59 day age
     (`last month` / `letzten Monat`). (e) delete the two vacuous absence asserts of removed layout test ids
     (`ReportsView.test.ts:731-732`, `reports.spec.js:321`).
   - L3-5: `RowMenu.svelte:34-38` `triggerAdvisory` suppression has no test. L3-4: `CompletionReportView.svelte ~71-88` takes the heading's
     focus when the fetch resolves although the focus moved elsewhere: only take it if the focus is still where the act left it.
   - Amendment (d)-style: the disabled mark-all button's contrast pair is unasserted (`Button.svelte:99-107`): assert it.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Each change has a test that fails when its code is reverted, with the failing assertion quoted in the pull request body. Evidence order:
     `mutation-after`.
   - The Playwright run of `row-menu.spec.js` is cited from the pull request (Playwright is not run in CI: Open Defect 5). Evidence order: `none`.
3. **Tests, texts and the CLI hint the amended spec asked for**
 - What:
   - D3a: `ReportsScenarioTest.java:152-153` takes the first 5 ids as the selection; select a non-prefix subset and assert which reports stay unread.
   - D3b: assert the German theme labels of the dev page at page level in `+page.test.ts:22-23` (today English only: `Helles Farbschema`,
     `Dunkles Farbschema`); the German wording "Farbschema" was changed unasked (OPEN DECISION 9).
   - `doc/design/shared/reports.adoc#read` (`:177`) is updated to what the page does after a mark-all (ids of the shown rows, first page reloaded).
   - `reports read --all` (`ReportsCommand.java:220-247`) marks the first page and says nothing of the rest: print that older unread reports
     remain, or loop the pages (OPEN DECISION 10); update `24-reports.adoc`.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - `ReportsScenarioTest` fails when a requested id stays unread or an unrequested one is marked; `ReportsCommandTest` asserts the "older remain"
     line when the list has a second page. Evidence order: `test-first`.
4. **Open review items answered**
 - What: the one CodeRabbit review body of #394 (4 nitpicks) is fixed in code (`Button.test.ts:144`, `CompletionReportView.svelte:44-67`, `:49-55`
   with tests `:311,:321`, the `try/finally` at `:341-405`) but no PR comment answers it: answer each nitpick on #394 with the commit.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript
 - Done when:
   - `ci pr comments --pr-number 394` reads no unanswered review body. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
     (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only for the gate (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime,pm-operator verify -Ppre-commit` for the Java side,
  the web unit tests through the module's npm scripts under Maven (`pm-web-app` `test:ci`), the browser tests through `pm-web-e2e`.
- No new dependency or plugin. Code standards (`CLAUDE.md`): Java 25, no generic `Exception`/`RuntimeException`, `@NullMarked` packages, JUnit 5 only, input
  test data from `Generators`. Web standards: ES modules, no `innerHTML` / `{@html}`, no arbitrary timer wait in a test, the npm scripts stay.
- The design document is updated in the same pull request as the code.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 129`): mock web port 21290, app port 21291. Other plans run at the same time: never stop
  or reuse a process this worktree did not start.
- Every `test-first` clause cites a FAILING run and a PASSING run of the named test; every `mutation-after` clause cites the reverted code and a failing assertion
  from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY (nitpicks and outside-diff sections included) answered where the
    reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
    The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in a commit of THIS pull request.
  State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED, read at `1cc0e10e` on 2026-11-10:
  - `ReportsView.svelte ~558-570` catch block deletes only `newlyRetainedIds`; identical to `be8c8403:545-554`.
  - `ReportsView.svelte:387` sets `isLoading` in `loadReports`; `:920` shows the Skeleton while it is set.
  - `RowMenu.svelte:70-72` `onViewportChange` calls `place()` on every capturing scroll.
  - `ReportsScenarioTest.java:152-153` takes the 5 shown ids.
  - `ReportsView.test.ts:731-732` and `reports.spec.js:321` assert the absence of removed layout test ids.
  - `format.test.ts` has no `last month` / `letzten Monat` case; `+page.test.ts:22-23` asserts English theme labels only.
  - `ReportsCommand.java:220-247` posts the ids of the first page and prints "✓ N reports marked read." only.
- OBSERVED, from `landings/PLAN-107.md` and not re-read: the review body of #394 has 4 nitpicks, all fixed in code and not answered on the PR.
- HYPOTHESIS: the retention mechanism (`loadReports ~:393-413`) and the in-flight reload have no test. Confirm or refute at `ReportsView.test.ts`
  (verify-at-outline).
- HYPOTHESIS: `CompletionReportView.svelte ~71-88` takes focus after the fetch although it moved. Confirm or refute at that component (verify-at-outline).
- HYPOTHESIS: the real `pm-mcpd` pages the unread list by offset. Not readable here: another repository. The first-page reload does not depend on it.
- Verify-first clause: re-read the named files at the current HEAD before editing. PLAN-45 (running when this was staged) rewrote the loading and
  reload paths of `ReportsView`.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/components/Button.svelte` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-web-e2e/tests/row-menu.spec.js` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/ReportsCommand.java` — present at `1cc0e10e`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/ReportsScenarioTest.java` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at `1cc0e10e`
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at `1cc0e10e`
- OBSERVED: `doc/design/shared/reports.adoc` — present at `1cc0e10e`
- OBSERVED: `doc/design/terminal-client/24-reports.adoc` — present at `1cc0e10e`

## Dependencies and Sequencing

- Depends on: `PLAN-107` (shipped, #394), `PLAN-45` (running: `ReportsView`, `Skeleton`, `ReportsView.test.ts`)
- Overlaps with: `PLAN-42`, `PLAN-44` and `PLAN-48` (all touch `ReportsCommand`; this plan's change is small and lands first), `PLAN-72` and `PLAN-73`
  (the views and `tokens.css`: start after), `PLAN-75` (verifies the web as a product: after)
- Adjacent to: `PLAN-70` (new deliverable 3 owns the theme-block reading of `Button.test.ts` and `RowMenu.test.ts`), `PLAN-74`
  (`components.adoc` and the RowMenu diagram, docs only)

## Hand-Off Command

Lane: `runbook` (a residue plan; operator 2026-11-10: small and residue plans on the runbook lane).

/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-129-web-reports-fifth-residue.md

## Write-Boundary

The session implementing this spec touches only its own repository source and tests (and, where a deliverable says so, the sibling repository
`plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The
orchestrator owns every other ledger write. The qualifiers of the inbox exception, and the sole sanctioned write mechanism, are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **`reports read --all` (decision 10):** prints an "older reports remain" hint when more pages exist; no paging loop.
- **Theme labels (decision 9):** the AGREED design text wins: the German theme labels revert to the wording of the design's text table (PLAN-107 changed them unasked), pinned by a test.
- **Dark-theme contrast tests:** stay in `PLAN-70` deliverable 4 (operator); not this plan's.

# PLAN-158: Web: What the Error and Loading Pattern Left Open (PLAN-45 Residue)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-10 from
> `landings/PLAN-45.md` (three-layer analysis of PR #399, squash `9fb2e7fa`). This spec is SELF-SUFFICIENT: the emitted command is a one-line
> pointer and carries no brief.

## Objective

PLAN-45 (#399) put every load path on one error pattern (`ErrorState` through `displayError`) and one loading pattern (`Skeleton`, a refresh
that keeps the list), with a 15 s client limit (30 s for a check) and a closed client code set (`timeout`, `network_offline`,
`unauthorized`). Read against its spec it left the objective — no path ends in a raw error code — unmet on the ACT paths, broke the
dismissal of the decision page's act banner, left mock codes the catalogue does not have, and met its browser clauses on a reduced matrix
only. Close these. Read `landings/PLAN-45.md` of the epic for the evidence of every item (the line numbers below were read at `9fb2e7fa`).

## Deliverables

1. **No raw code on an act path**
 - What:
   - `ClientError`'s message is its code (`pm-web-app/src/lib/api/errors.ts:29`). The act lines still print `(err as Error)?.message`, so an act
     that outlives the 15 s limit shows the bare word `timeout` (and `network_offline`, `unauthorized`, or a daemon code): `ReportsView.svelte`
     `:456` (rendered `:922`; same form at `:510`, `:581`), `CleanupView.svelte:218-259`, `DecisionView.svelte:521,539,571,604,616` (rendered
     `:1290`), `ProjectExplorerView.svelte:175,191,215`, `ProjectOnboardingView.svelte:282,312,366`, `StartView.svelte` `messageOf` `:254-258`
     (used `:273-646`), `SystemView.svelte:294,450`.
   - An act failure shows the code's sentence through `displayError` (the `fix` sentence of the code, as `ErrorState`'s one sentence), with
     the act's own failure text where no code is known; never `err.message`. Use one helper beside `errorCodeOf` (`errors.ts`) rather than
     seven local ones. The act's retry stays where it is.
   - The `|| 'unknown'` sentinel the views pass to `ErrorState` is printed under _Details_ as if it were a code: pass no code instead.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:jest-testing
 - Done when:
   - Per view, a test: an act failing with `ClientError('timeout')` shows the `timeout` sentence and not the word `timeout` (evidence order:
     `test-first`).
   - `git grep -n "as Error)?.message" -- pm-web-app/src ':!*.test.ts'` names no view (evidence order: `none`).
2. **Banners that clear, a deadline that says so, a timer that fires**
 - What:
   - `DecisionView.svelte`: `loadDecisions` (`:403-405`) clears `loadError` only; before PLAN-45 it cleared `errorMessage` (`:401` of the
     first parent). A failed act leaves the banner `:1288-1293` for the life of the view, and its Retry cannot dismiss it. Clear the act
     banner on a reload and on the next successful act.
   - `ProjectExplorerView.svelte`: `actionError` (`:148`, rendered `:512`) is never cleared; clear it on a reload and on a successful toggle.
   - `client.ts` `readBody` (`:3160-3176`): a caller deadline (a `TimeoutError` reason on the caller's signal) failing mid-body ends in
     `network_offline`, while `failFetch` (`:3132-3146`) gives `timeout` for the same signal. Make both say `timeout`.
   - The D2 test of PLAN-45 (`client.test.ts:604`) mocks an already-aborted signal, so the 15 s timer never fires: add a test with fake
     timers in which a request that never answers ends in `timeout` when the limit passes and not before.
   - Retry on a failed reload in `ReportsView` (`:396`) removes the focused button and drops the focus to the body: keep the focus on the
     list or the error heading. `role="alert"` stays only on an error that appears after the page was shown, not on a static one.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Tests that fail before: a failed act then a reload leaves no banner (DecisionView); a failed toggle then a successful one leaves no
     banner (ProjectExplorerView); a caller deadline mid-body ends in `timeout`; the fake-timer request ends in `timeout` at the limit; the
     focus after Retry is not the body (evidence order: `test-first`).
3. **The mock answers codes the catalogue has**
 - What: PLAN-45's decision 2 moved `plan_not_found` / `project_not_found` to `scope_not_found` / `project_not_enrolled`, but the mock
   still answers `epic_not_found` (`MockRuntimeEngine.java:491`, used `:4790`, `:5014`) and `report_not_found`, `chat_not_found`,
   `device_not_found`, `hold_not_found`, `lesson_not_found`, `tokens_not_found`, `review_not_found`, none of which is an `ApiErrorCode`
   member (`ApiErrorCode.java:56-62`: `project_not_enrolled`, `scope_not_found`, `target_not_found`), so those views fall back to
   `error.unknown`. Apply decision 2's rule: each answers the member whose text fits — `scope_not_found` for a scope the address names (a
   plan, an epic), `target_not_found` for an item inside one — and the mock tests that pin the old codes follow. **No new `ApiErrorCode`
   member and no new error text**: a code with no fitting member is reported in the landing and left as it is.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - `git grep -n "_not_found\"" -- pm-mock-runtime/src/main` names only `ApiErrorCode` members (or the reported exceptions) (evidence
     order: `none`).
   - A mock test per changed code pins the new one (evidence order: `mutation-after`).
4. **Tests, keys and the design text PLAN-45 owed**
 - What:
   - `StartView.svelte:678-689` and `EpicDecompositionView.svelte` changed to the pattern with no unit test: add one each (load failure shows
     `ErrorState` with Retry; loading shows `Skeleton`).
   - `pm-web-e2e/tests/error-pattern.spec.js:61` asserts the hang case with `timeout: 25000`: the clause "within 15 s" of PLAN-45 cannot be
     strict when the limit is 15 s, so the clause is "once the 15 s limit passes, within 5 s": assert `≤ 20000`; the third test (`:66-76`)
     asserts the heading text.
   - Orphan keys left by the retired mappings: `plan.view.not_found` (`plan_view.*.properties:97`), `web.settings.devices.load_failed`
     (`settings.*:82`), `project.onboarding.retry` (`projects.*:175`) — remove each that no reader asks for; `decomposition.error` is pinned
     by `DecompositionCatalogueTest:79` although no view reads it: remove the key and the pin, or name its reader.
   - `doc/design/web-app/components.adoc` states only `ErrorState` (`:447-452`): add the loading pattern's refresh form (the list stays, a
     quiet in-place cue, a failed refresh shows the stale list and the error together) beside `Skeleton`.
   - Code standards: `DecompositionScenarioTest.shouldRefuseInstructForUnknownPlan` (literals `no-such-plan`, `Rewrite it.`) and
     `MockRuntimeEngineTest.shouldRefuseStartWithBlankProject` (a literal body) draw their input from `Generators`.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing, pm-documents:ref-asciidoc
 - Done when:
   - The new unit tests fail when the pattern is reverted in their view (evidence order: `mutation-after`).
   - The e2e hang case asserts `≤ 20000` and passes (evidence order: `none`).
   - `git grep` names no reader-less key of the four; `components.adoc` states the refresh form (evidence order: `none`).
5. **The browser matrix PLAN-45 did not take**
 - What: PLAN-45 checked its three targets (`/plans/does-not-exist`; `/start` for an unknown project; stopping the mock while a view loads)
   on de/light/1440 before and after and en/dark/390 after only. Take the after state of the three targets in the remaining six
   combinations (`de`/`en` × dark/light × 1440 × 900/390 × 844), and the act failure of deliverable 1 on one view (an act held past the
   limit) in de and en.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript
 - Done when:
   - Browser check (Playwright MCP) shows the pattern on every combination named; snapshots plus screenshots recorded (evidence order:
     `none`).
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed
     read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`).

Not split: five deliverables, each a residue of one landed plan on one pattern; the orchestrator records the rationale (scope guard).

## Standing Constraints

- Build and test through Maven only for the gate (`CLAUDE.md`): `./mvnw -pl pm-api,pm-mock-runtime verify -Ppre-commit` for the Java side,
  `./mvnw -pl pm-web-app verify` for the web unit tests (`test:ci`), and
  `PM_MOCK_WEB_PORT=21580 PM_WEB_PORT=21581 ./mvnw -pl pm-web-e2e verify -Pe2e` for the browser tests.
- No new dependency or plugin. Code standards (`CLAUDE.md`): Java 25, no generic `Exception`/`RuntimeException`, `@NullMarked` packages, JUnit 5
  only, input test data from `Generators`. Web standards: ES modules, no `innerHTML` / `{@html}`, no arbitrary timer wait in a test (fake
  timers or a condition), the npm scripts stay.
- Every server value on a touched view passes through the display layer; the text guard baseline only shrinks.
- The design document is updated in the same pull request as the code.
- Ports of this plan: per `AGENTS.md` § Ports formula (`20000 + 10 × 158`): mock web port 21580, app port 21581. Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Browser check of the rendered output with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks (pair with a
  fresh single-use code each session; `POST /mock/goto/<state>`; files named `{plan}-{target}-{after}` under `.plan/temp/playwright-mcp/`). If
  the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such.
- Every `test-first` clause cites a FAILING run and a PASSING run of the named test; every `mutation-after` clause cites the reverted code and a
  failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY (nitpicks and outside-diff sections included)
    answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull
    request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues at merge, every open issue fixed in a commit of THIS pull request.
  State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: act lines print `err.message` and `ClientError`'s message is the code — read at `pm-web-app/src/lib/api/errors.ts:29`, `pm-web-app/src/lib/views/ReportsView.svelte:456,922` (`9fb2e7fa`)
- OBSERVED: `loadDecisions` no longer clears `errorMessage` — read at `pm-web-app/src/lib/views/DecisionView.svelte:403-405,1288-1293` and the first parent's `:401` (`9fb2e7fa`)
- OBSERVED: `actionError` of the project explorer is never cleared — read at `pm-web-app/src/lib/views/ProjectExplorerView.svelte:25,148,512` (`9fb2e7fa`)
- OBSERVED: `readBody` maps a caller deadline to `network_offline` — read at `pm-web-app/src/lib/api/client.ts:3160-3176` (`9fb2e7fa`)
- OBSERVED: the mock answers `epic_not_found`, not an `ApiErrorCode` member — read at `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java:491`, `pm-api/src/main/java/de/planmarshall/api/resource/ApiErrorCode.java:59,62` (`9fb2e7fa`)
- OBSERVED: the e2e hang case waits 25 s — read at `pm-web-e2e/tests/error-pattern.spec.js:61` (`9fb2e7fa`)
- HYPOTHESIS: the other `*_not_found` codes of the mock (`report_`, `chat_`, `device_`, `hold_`, `lesson_`, `tokens_`, `review_`) are outside `ApiErrorCode` too — confirm/refute at `MockRuntimeEngine.java` and `ApiErrorCode.java` (verify-at-outline)
- HYPOTHESIS: the orphan keys have no reader — confirm/refute by `git grep` per key (verify-at-outline)
- Verify-first clause: re-read each cited range at the current HEAD before the deliverable that uses it; PLAN-129 (`ReportsView`) may land first.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/api/errors.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/StartView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/StartView.test.ts` — absent at HEAD 9fb2e7fa; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.svelte` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-app/src/lib/views/EpicDecompositionView.test.ts` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-web-e2e/tests/error-pattern.spec.js` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/runtime/MockRuntimeEngineTest.java` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/DecompositionScenarioTest.java` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.en.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/main/resources/messages/decomposition.de.properties` — present at HEAD 9fb2e7fa
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/DecompositionCatalogueTest.java` — present at HEAD 9fb2e7fa
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 9fb2e7fa

## Dependencies and Sequencing

- Depends on: `PLAN-45` (shipped, #399, `9fb2e7fa`).
- Overlaps with: `PLAN-129` (staged: `ReportsView.svelte`, `ReportsView.test.ts`; whichever starts second rebases), `PLAN-123` (running,
  terminal: `pm-api` catalogue files only if it touches the same areas), the mock-touching plans `PLAN-132`..`PLAN-134` (`MockRuntimeEngine.java`).
- Adjacent to: `PLAN-153`..`PLAN-157` (views package, later; they inherit the act-error helper of deliverable 1).

## Hand-Off Command

Lane: `runbook` (a residue plan; operator 2026-11-10: small and residue plans on the runbook lane).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-158-web-error-pattern-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR
and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

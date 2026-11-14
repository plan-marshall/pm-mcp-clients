# PLAN-152: Web Foundation: The Catalogue Clean-Up (D1 Foundation, Part 6)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from `shared/texts-and-languages.adoc` § `#keys` and the hand-off of the PLAN-112 landing
> (`landings/PLAN-112.md`). SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

D1 fixed "one key per concept" in the catalogue, and the PLAN-112 landing handed the clean-up on: the shared acts, kinds, filters and reasons are named twice or three times, and a client whose reload is deferred for unsent input
can fetch a missing locale from a newer runtime. This plan merges the duplicate keys together with their web readers in one release, and closes the deferred-reload hole. Keys that a `pm-operator` reader still uses stay,
named in a list for the terminal package. After it, the plain words of `CC-V-02` (PLAN-29) can be checked against one key per concept.

## Deliverables

Read first: `doc/design/shared/texts-and-languages.adoc` (§ `#keys` bullets "One word per value" to "Delivery", § `#kinds`, § `#field-texts`), `doc/design/web-app/components-tags.adoc` § `#tag-words`,
`doc/design/web-app/view-density-and-filtering.adoc` § `#filter-control`, `landings/PLAN-112.md` § Hand-offs, `pm-web-app/src/lib/i18n/index.ts`, `pm-web-app/src/lib/stores/unsentInput.ts`.

1. **Shared acts: one key each**
 - What:
   - Merge `web.common.back|close|save|cancel|retry`, `common.actions.back` and `common.actions.back.title`, and the two-part `common.back|close|save` into `common.action.back|close|cancel|save|retry|later` (en and de), and move every web reader.
     The readers at `1cc0e10e` are `ChatPanel`, `ConfinedFilePicker`, `HoldResolutionModal`, `CommandPalette`, the dev page, `CleanupView`, `CompletionReportView`, `DecisionView`, `DecompositionReviewView`, `EpicAdoptView`, `EpicDecompositionView`,
     `EpicView`, `MachineSettingsView`, `PlanView`, `ProcessDesigner`, `ProjectExplorerView`, `ProjectOnboardingView`, `ReportsView`, `SettingsView`, `SystemView` and `OverviewView`; the inventory is repeated at outline.
   - The old keys are removed in the same pull request as their last reader; `catalogue.test.ts` fails on a key that no reader asks for and on a reader of a missing key.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `git grep -nE "web\.common\.(back|close|save|cancel|retry)|common\.actions\.back"` over `pm-web-app/src` and `pm-api/src/main` is empty (the catalogue test file excluded: it names the retired keys in its absence assertion). Evidence order: `none`.
   - `catalogue.test.ts` and the Java `MessageCatalogueTest` pass; the web test fails when one old key is put back (a mutation). Evidence order: `mutation-after`.
 - Implements: `shared/texts-and-languages.adoc` § `#keys` ("a shared act").
2. **Kinds, filters, reasons and states: one key per concept**
 - What:
   - Kinds: `overview.find.kind.*` (reader `CommandPalette.svelte`) and the web readers of `overview.kind.*` (`EpicView`, `OverviewView`) move to `common.kind.*` (added by PLAN-147). The three Java readers of `overview.kind.*`
     (`StatusCommand`, `EndedWorkLevel`, `ProjectWorkLevel` in `pm-operator`) are in the terminal package's hands: `overview.kind.*` stays until they move (operator decision D-8, assumed A), and the keys that stay are listed in the pull request body for the terminal package.
   - Filters: `decisions.filter.label` and `report.filter.label` merge into `common.filter.label` with the list's name as argument (readers `DecisionView.svelte`, `ReportsView.svelte`); a view keeps a key only for a text that says more than the shared one.
   - Reasons: `overview.needs.operator_wait` has no reader outside its test and is removed; `overview.needs.waiver_request` has the reader `PmConsole.java` and stays until the terminal package moves it. The web reads the normalised spellings `waiver` and `waits_for_you` only.
   - States: where an area pack carries its own word for a value `state.*` names (`overview.state.*` except the variant that names the task or phase a plan pauses after), the web reader moves to `state.*` and the area key is removed. The exact list is the output of an inventory
     run at outline, limited to the web readers and to keys no `pm-operator` reader asks for; any other key is listed for the terminal package.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - Each removed key has no reader in `pm-web-app/src`, `pm-api/src/main` and `pm-operator/src/main` (`git grep` per key, test files naming the retired key excluded and listed). Evidence order: `none`.
   - The Java catalogue tests (`OverviewCatalogueTest`, `ReportCatalogueTest`, the decision pack test) pass with both locales equal in keys and arguments. Evidence order: `test-first`.
   - The list of keys that stay for the terminal package is in the pull request body and in the inbox message. Evidence order: `none`.
 - Implements: `shared/texts-and-languages.adoc` § `#keys` ("one word per value", "one key per concept"); `components-tags.adoc` § `#tag-words`.
3. **The deferred reload does not fetch a missing locale from a newer runtime**
 - What:
   - While `SystemView` holds the reload because a field has unsent input (`reloadWaitsForInput`, `SystemView.svelte:228`), the page is still the old release's code. A language change or the first open of a view then fetches a pack from the newer runtime (`loadMessages`, `setLocale`),
     whose keys the old code may no longer ask for. The rule (operator decision D-9, assumed A): while the reload is deferred, a requested locale or area that is not in the cache is not fetched; the choice is kept and applied at the reload.
   - The state is a small store (`reloadDeferred`) the system view sets and clears, read by `i18n/index.ts`. The rule is written into `texts-and-languages.adoc` § `#keys` "Delivery".
   - Do not restyle `SystemView` here (PLAN-153 does); touch only the lines that set and clear the state.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `index.test.ts` shows: with the reload deferred, `setLocale('de')` for an uncached locale makes no request and keeps the current locale; after the reload is released the choice is applied; a cached locale switches at once. Evidence order: `test-first`.
   - `SystemView.test.ts` shows the state is set while the reload waits for input and cleared when it proceeds or the input is sent. Evidence order: `test-first`.
 - Implements: `shared/texts-and-languages.adoc` § `#keys` ("Delivery", "Retention"); `shared/system.adoc` § `#mismatch`.
4. **The documents follow the keys**
 - What: the per-area texts documents that quote a removed or renamed key (`system-texts.adoc`, `decisions-texts.adoc`, `plan-overview.adoc` texts, `reports.adoc` texts, `work-actions-texts.adoc`, `components-tags.adoc` § `#tag-words`) are corrected in the same pull request;
   `texts-and-languages.adoc` § `#kinds` already lists the merged kinds.
 - Persona: plan-marshall:persona-documenter
 - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
 - Done when:
   - `git grep` of each removed key over `doc/design` is empty. Evidence order: `none`.
5. **Browser evidence**
 - What: the system view during a deferred reload, in both locales; the command palette (kinds); the decisions and reports filters; a language change before and after the reload. Text guard baseline unchanged.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) on `reports-and-system` and `running-work` at 1440×900 and 390×844, `de` and `en`, both themes; screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api verify`, `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md` (ES modules, flat ESLint, no `innerHTML`/`{@html}` with runtime text, no timer waits in tests, the npm scripts stay). Java: JUnit 5, input from `Generators`, `@NullMarked` packages, no generic `Exception`.
- Keys are removed together with their last reader in one release; a key a `pm-operator` reader still asks for is NOT removed here. `pm-operator` is not edited by this plan.
- The text guard baseline does not grow. The glossary is respected (a glossary term is never translated in German).
- Ports of this plan: mock web port 21520, app port 21521 (`20000 + 10 × 152`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `web.common.back|close|save|cancel|retry`, `common.actions.back`, `common.back|close|save` all exist in `common.en.properties` (lines 1-5, 89-94, 195-201), with web readers in the 21 files named in deliverable 1 — read at `pm-api/src/main/resources/messages/common.en.properties` and by `git grep -l`
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: common.en lines now 132-135 and 248-252; web.common.retry gone (common.action.retry:247 via #399); readers now 20 files: CleanupView ReportsView SystemView dropped; FindingsReviewModal PlanSettingsModal added | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- OBSERVED: `overview.kind.*` is read by `EpicView.svelte`, `OverviewView.svelte` and, in `pm-operator`, by `StatusCommand.java`, `overview/EndedWorkLevel.java`, `overview/ProjectWorkLevel.java`; `overview.find.kind.*` is read by `CommandPalette.svelte` only — read by `git grep -l`
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: overview.kind.* readers EpicView OverviewView plus StatusCommand EndedWorkLevel ProjectWorkLevel and a 4th operator reader EpicCommand.java:292; overview.find.kind only CommandPalette holds | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- OBSERVED: `overview.needs.waiver_request` is read by `PmConsole.java`; `overview.needs.operator_wait` only by `OverviewCatalogueTest` — read by `git grep -l`
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: overview.needs.waiver_request no longer exists in any pack (removed in #426 98cea294; OverviewCatalogueTest asserts absence); PmConsole does not read it; operator_wait has no reader at all | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- OBSERVED: `decisions.filter.label` is read by `DecisionView.svelte`, `report.filter.label` by `ReportsView.svelte`; `common.filter.label` exists (`common.en.properties:234`) — read by `git grep`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: DecisionView:1363 decisions.filter.label; ReportsView:961 report.filter.label; common.filter.label exists at common.en.properties:285 (was 234)
- OBSERVED: the deferred reload is `reloadWaitsForInput` in `SystemView.svelte` (lines 52, 211-237), over `hasUnsentInput` of `stores/unsentInput.ts`; `i18n/index.ts` has `loadMessages(locale, area)` and `setLocale(locale)` and fetches a missing pack from the runtime — read at the three files
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: SystemView reloadWaitsForInput :54 set/clear :212-244 over hasUnsentInput; i18n/index.ts loadMessages:453 setLocale:498 fetch missing pack via apiClient.getMessages
- OBSERVED: the `pm-operator` files `PmConsole`, `StatusCommand`, `ProjectWorkLevel` are in the surfaces of running and staged CLI plans (PLAN-123, 84, 126, 127, 128 and others) — read by `corpus surfaces`
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Corpus ledger fact (surfaces of staged CLI plans), not settleable by reading source
- OBSERVED: `landings/PLAN-112.md` states the hole in one sentence ("a client whose reload is deferred for unsent input may fetch a missing locale from a newer runtime") and no rule — read at the landing
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: landings/PLAN-112.md:31 states the hole in one sentence and no rule
- HYPOTHESIS: the fetch of a missing pack during a deferred reload is reachable through `setLocale` and the first open of a view, and through nothing else — confirm/refute at `pm-web-app/src/lib/i18n/index.ts` § `loadMessages`, `setLocale`, `adoptRecordedLocale` (verify-at-outline)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: getMessages is reached by setLocale and adoptRecordedLocale (runtime.ts:848) via loadCatalogueFor and by initI18n:673-677; no view calls loadMessages(area) so first-open-of-a-view path does not exist | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)
- HYPOTHESIS: the "states" part of deliverable 2 touches fewer than ten keys once the `pm-operator` readers are excluded — confirm/refute at the inventory run at outline: `overview.state.*` against `git grep` per key (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: overview.state.* is 6 keys total; all read by pm-operator (OverviewFormatting StatusCommand PlanViewFormatting SystemView.java); no web reader; far under ten
- Verify-first clause: re-run the reader inventory of every key of deliverables 1 and 2 at the current HEAD (PLAN-45, PLAN-147, PLAN-148 and the CLI plans may have changed it) before scoping; a key that gained a `pm-operator` reader stays.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first instruction (re-run the reader inventory), not a code fact

## Expected Surface

- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/overview.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/MessageCatalogueTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/OverviewCatalogueTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ReportCatalogueTest.java` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/i18n/index.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/i18n/catalogue.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/unsentInput.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD 1cc0e10e (the reload state lines only)
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `pm-web-app/src/lib/views/OverviewView.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e (key readers only)
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/system-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed (running; `common.*`, `catalogue.test.ts`, `views/`); `PLAN-123` landed (running; `common.*`); `PLAN-147` (adds `common.kind.*`) and `PLAN-148` (adds `phase.name.*`, `common.marker.*`).
- Overlaps with:
  - `PLAN-126`, `PLAN-129`, `PLAN-132`, `PLAN-127` (staged): `common.*`, `overview.*`, `reports.*`, `decisions.*` properties. Their key blocks are disjoint; the gate sequences the files.
  - `PLAN-51`, `PLAN-52`, `PLAN-49` (parked): `DecisionView`, `OverviewView` and the `overview.*` pack; they re-ground on the merged keys.
  - The terminal package (not staged): `PmConsole`, `StatusCommand`, `ProjectWorkLevel`, `EndedWorkLevel`; the keys that stay for it are listed.
  - `PLAN-153`: `SystemView.svelte` (this plan touches only the reload-state lines; PLAN-153 follows).
- Adjacent to: `operator.console.badge.*` (terminal package); the "console badge words" hand-off of the PLAN-112 landing.
- Unblocks: the gate "D1 foundation implemented" of PLAN-29, 41, 65; PLAN-153.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Re-run the reader inventory and `corpus surfaces`. Settle each HYPOTHESIS. Re-check the operator's answers to D-8 and D-9.

## PLAN-133 Landing Residue (Amendment 2026-11-13)

- Premise: since `a139ec62` the web chat (entry, plan, epic, review, ended work) was broken against the mock until `PLAN-161`
  (chat bridge). RESOLVED 2026-11-14: `PLAN-161` (#429, `dc811fb3`) landed and removed `PLAN-148`'s `test.fixme`; no `fixme` remains in
  `pm-web-e2e/tests`. Still open: a chat that is not seeded is never OPENED by the clients (GET only) — `PLAN-135`/`PLAN-138` own that;
  an e2e failure on an unseeded chat is theirs, name it in the pull request. An e2e failure at this plan's head that traces
  to the chat contract is not this plan's to fix: name it in the pull request with `PLAN-161` as owner.
- The scenario fixtures are validated at load against the B1 catalogue (unknown chat keys fail closed): a fixture edit in a `chats` block
  must satisfy it.

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- The reader inventory includes PLAN-148's keys: `phase.name.*`, `role.name.*`, `common.marker.*`, `common.chip.copy`, `dev.row_link.*`, `dev.section.*`, `dev.examples.*`.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 0 contradicted: common.en lines now 132-135 and 248-252; web.common.retry gone (common.action.retry:247 via #399); readers now 20 files: CleanupView ReportsView SystemView dropped; FindingsReviewModal PlanSettingsModal added
- Claim 1 contradicted: overview.kind.* readers EpicView OverviewView plus StatusCommand EndedWorkLevel ProjectWorkLevel and a 4th operator reader EpicCommand.java:292; overview.find.kind only CommandPalette holds
- Claim 2 contradicted: overview.needs.waiver_request no longer exists in any pack (removed in #426 98cea294; OverviewCatalogueTest asserts absence); PmConsole does not read it; operator_wait has no reader at all
- Claim 7 contradicted: getMessages is reached by setLocale and adoptRecordedLocale (runtime.ts:848) via loadCatalogueFor and by initI18n:673-677; no view calls loadMessages(area) so first-open-of-a-view path does not exist
- Already done: `web.common.retry` merged into `common.action.retry` (#399); `overview.needs.waiver_request` removed (#426). Readers changed: re-run the inventory. No view calls `loadMessages(area)`.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-152-web-foundation-catalogue-cleanup.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

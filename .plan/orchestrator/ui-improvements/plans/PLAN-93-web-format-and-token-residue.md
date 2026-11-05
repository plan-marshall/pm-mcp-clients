# PLAN-93: Web: Token Marks from the Catalogue, Locale-Safe Formatters and Order-Independent e2e Selectors

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-02 from the landing analysis of PLAN-90 (`landings/PLAN-90.md`). One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Close the residue PLAN-90 reported and the analysis confirmed: the token marks and the `in`/`out` labels of the formatter are still literals although the design fixes them in the packs, the formatters' `locale` argument does not select the words, the activity list has a few unvalidated and untested paths, and several e2e specs still pick a plan card by position — the pattern that made seven tests red. The last deliverable adds the web findings of CodeRabbit that no one handled (found by the audit of 2026-11-02). One pull request.

## Deliverables

1. **The token marks and labels come from the catalogue**
   - What: `format.ts:285-290` hard-codes `'Tsd.'`/`'k'` and `'Mio.'`/`'M'`, and `formatSplit` hard-codes `'rein'`/`'raus'` and `'in'`/`'out'`. `texts-and-languages.adoc:171` says these marks are fixed in the packs, and the packs carry `common.tokens.thousands`, `common.tokens.millions`, `common.tokens.split` and `common.tokens.at_least`; no code reads them (only `coverage` and `not_measured` are read). The Java side has its own `TokenFormat` for the terminal: leave it alone.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `format.test.ts` has one case per form (thousand mark, million mark, split with `in`/`out`, at-least prefix) in `de` and `en`, each failing against the literal; the keys are read, and a key no code reads is removed from both packs or its reader is named (evidence order: `test-first`)
     - Browser check (`running-work`): overview, epic view and plan view, token split and totals, `de`/`en`, before and after: the text is unchanged (evidence order: `none`)
2. **The formatters select the catalogue by their `locale` argument**
   - What: `format.ts` `word()` takes `locale`, but `translate()` (`i18n/index.ts` ~759) reads the active store and ignores it for selecting words; `formatTime(iso, 'relative', 'de')` outside the active German catalogue yields English words with German date formatting. `format.test.ts` calls `useCatalogue(locale)` first, so no test fails on a mismatch. Every production caller passes `$currentLocale`, so it is latent. The default `locale = 'en'` makes a caller that omits it read English on a German page.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test calls a formatter with the locale that differs from the active catalogue and asserts the words of the argument's locale (or, if the design says the argument is dropped, the argument and its default are removed and every caller adjusted); it fails before the change (evidence order: `test-first`)
3. **The activity list and the time helpers cover their edges**
   - What: `PlanView.svelte` `dateOnRead` (~335-342) does not validate the clock: `25:99` builds `...T25:99:00`, `formatTime(..., 'clock')` returns '', and `|| timestamp` prints the constructed ISO-like string, contradicting "no ISO ever"; a clock without seconds gets a fabricated `:00`. The `source.time` fallback (`PlanView.svelte:361`) is untested. `dayOf` has no direct test, and its clock-only branch (returns `now`) is dead in PlanView because the row is already dated. The `NOW`-today minutes/hours cases ("just now", "N min ago") were removed from the relative `it.each`, and `Intl.RelativeTimeFormat` is tested for months only. The key `project.explorer.last_active` has no reader and is pinned only by `ProjectsCatalogueTest`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - One test per edge (malformed clock, clock without seconds, `source.time` fallback, `dayOf` clock-only, a year-old age, today under a minute and under an hour), each failing when the code it covers is reverted (evidence order: `mutation-after`)
     - The dead key is removed from both packs and from `ProjectsCatalogueTest`, or its reader is named (evidence order: `none`)
4. **No e2e selector depends on the order the runtime serves**
   - What: `plan-view.spec.js:221` ("opens the project in the explorer") asserts `project=pm-mcp-clients` through `plan-card .first()`; the same positional pick is in `theme-readability.spec.js` (258, 278, 625, 722), `chrome.spec.js:85`, `showroom.spec.js:41` and `overview.spec.js:147`. PLAN-90 fixed three tests with `openPlan(page, title)`; these are the same pattern. `plan-overview.adoc:102-103`: the runtime serves the plans that need the operator first and a client does not sort again.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each spec that asserts something about the picked plan selects it by title (the `openPlan` helper); a spec that only needs "some card" says so in a comment or uses a helper named for that (evidence order: `none`)
     - `PM_MOCK_WEB_PORT=20930 PM_WEB_PORT=20931 ./mvnw -pl pm-web-e2e verify -Pe2e` reports no failure (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

5. **The web findings of CodeRabbit that nobody handled**
   - What: found by reading the review BODIES and unresolved threads of earlier pull requests (`landings/coderabbit-audit/`); all are present on `main` at `94bbefe1`. (a) PR 287, Major: `MachineSettingsView.svelte` binds `cleanupRemindDays` (input ~352) but `saveCleanup` sends only `cleanupDays` under `cleanup_durations` (~87-89): the reminder interval never reaches the server although the page shows "saved"; also `OnboardingView.scenarios.test.ts` asserts nothing on `p.state-card__summary`. (b) PR 297, Major: `DecisionView.svelte` `answerChatProposal` (~659) assigns the response to the shared `chat` even after the operator closed that decision's panel and opened another one's chat, so the new panel shows the earlier transcript: assign only when `chatDecisionId === decisionId`; `takeProposedAnswer` (~683) sets `chatOpen = false` unconditionally and hides chat B's panel when A's acceptance completes. (c) PR 297: `ProjectOnboardingView.svelte` `measureStepLine()` runs only on mount and resize (~85-87), not when `stepRows` or `$currentLocale` change, so after a locale switch the step line can overflow with `tabindex="-1"`; no unit test for the overflow-dependent tabindex; `Tabs.test.ts` has no arrow-key test with the tablist itself focused. (d) PR 296: `CommandPalette.svelte` passes only `match.id` for a decision (`pushRoute('decisions', { decisionId: match.id })`, ~204-205), discarding the plan location (CodeRabbit: unverified; depends on whether decision ids are unique across plans: read the contract, then pass the plan or say why not); `client.test.ts` (findWork abort test, ~714-729) does not assert that `signal` is forwarded to `fetch`; `runtime.test.ts` has no test for `adoptRouteFromLocation` reading `?decision=`. (e) PR 291: `formatTokens(count, floor=true)` returns `'0'` for `count <= 0` instead of the floor form `≥ 0` (`format.ts` ~264); `i18n/index.ts` ~317-318 evicts every sibling catalogue of the locale on a known version (CodeRabbit: retain older catalogues for rollback; optional, heavy: do it or answer it with a reason). (f) PR 237, unresolved thread: `theme-readability.spec.js` (~259-266, 337, 351-355) records `EpicView`, `CleanupView` and `MachineSettingsView` after only the container is visible, so a static loading placeholder passes the quiet check and is recorded into the baseline: wait for the loading state to end. (g) PR 243: `vite.config.ts` (~133) sets no `server.strictPort` for an assigned `PM_WEB_PORT` (AGENTS.md: an assigned port in use is a blocker); `helpers/ports.js` (~8-9) turns `abc` or `0` into 5173 through `Number(...) || 5173`; `scripts/guard-ports.js` and `tests/port-guard.spec.js` duplicate the guard and its allowlist. (h) PR 260: the build-output check that fails when emitted code still contains a development-page marker or a retained `/dev` route was asked for and never added (`devRouteExclusion.ts`). Not here: the `ControlActionsBar` finding of PR 297 (focus is not restored when `busy` clears) belongs to the running `PLAN-47`, which removes that component; it was delivered to its mailbox. (i) PLAN-89 residue (`landings/PLAN-89.md`): the three findings of CodeRabbit's second review of PR 305 (`2bbe1206`, in the review BODY) are NOT here: PLAN-89's own follow-up pull request #314 (`fix/89-palette-review-followup`, `CommandPalette.test.ts` only, open on 2026-11-04) carries them. Start deliverable 5 (d) and (i) after #314 has merged, or rebase on it: both edit `CommandPalette.test.ts`. What stays here: the scroll test (~212-221) never asserts that `aria-activedescendant` changed from the first option; the Tab walk through Close, field and Retry is untested in both the unit test and `chrome.spec.js`; the docblock of `IconButton.svelte:16` still calls `label` "the accessible name and the visible label" although `visibleLabel` exists. Item (d) (`match.id` only) is in the same file: do them together.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-dev-frontend:lint-config
   - Done when:
     - One test per item (a) to (d) and (e)'s zero floor that fails before it: the reminder interval is sent and the page shows it after a reload; a late chat answer does not replace another decision's panel; the step line is re-measured after a locale switch; the signal is forwarded; `?decision=` is adopted; `formatTokens(0, true)` reads `≥ 0` (evidence order: `test-first`)
     - Item (i): the `aria-activedescendant` assertion and the Tab walk through Close, field and Retry fail when the code they cover is reverted (make ArrowDown do nothing; drop Retry from the focus cycle) (evidence order: `mutation-after`)
     - (f) records only after the loading state ended (the three views); (g) a malformed `PM_WEB_PORT` fails loudly and `strictPort` is set; (h) a check over the real build output fails when a development marker is present; each fails when the code it covers is reverted (evidence order: `mutation-after`)
     - A browser check (`running-work` and `fully-onboarded`): the machine settings reminder interval saves and survives a reload; the decision chat across two decisions; the onboarding step line after a language switch, `de`/`en`, before and after (evidence order: `none`)
     - The optional items ((e) catalogue rollback) carry a recorded decision with the reason (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these pass at the head: `./mvnw -pl pm-web-app verify`, and the e2e command of deliverable 4.
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `20930`, app port `20931` (`AGENTS.md` § Ports). Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check of the rendered output with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario running-work --web-port 20930"`), then `PM_MOCK_WEB_PORT=20930 ./pm-web-app/start.sh -p 20931`; the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads: PR #303 merged with six findings that sat in a review body only.
- Closed by the operator, not in this plan: the `since` wording on cells that name a completed point (Ended, last used, last active) stays as the design words it (`texts-and-languages.adoc:149-150`, `:180`).

## Claim Labels

- OBSERVED: the token marks and `in`/`out` labels are literals (`format.ts:285-290`) and the `common.tokens.*` keys other than `coverage` and `not_measured` are read by no code in `pm-web-app/src` — read by `git grep` by the orchestrator at `3f26b6c2`
- OBSERVED: `plan-view.spec.js:221` picks `plan-card .first()` — read by the orchestrator at `3f26b6c2`
- HYPOTHESIS: the formatters' `locale` argument does not select the catalogue words (`translate()` reads the active store) — read by the analysing agent, not by the orchestrator: confirm at `i18n/index.ts` (verify-at-outline)
- HYPOTHESIS: `dateOnRead` prints a constructed ISO-like string for a malformed clock, and `project.explorer.last_active` has no production reader — confirm per file (verify-at-outline)
- Source: `landings/PLAN-90.md` (layer 3), the lane's residue list of `rb-90-web-green-baseline-and-time-residue-001`.
- OBSERVED: the findings of deliverable 5 are present on `main` at `94bbefe1` — read by the auditing agent against the merge commits and `main`; the orchestrator did not re-read each one (verify-at-outline). Source: `landings/coderabbit-audit/` (PRs 237, 243, 260, 287, 291, 296, 297). PRs 298, 300 and 302 carried `skip-bot-review`: the data components of PR 302 never had a bot review.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/display/format.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` and `common.de.properties`, `projects.en.properties` and `projects.de.properties` — present at HEAD 94bbefe1 (the packs live in `pm-api`)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/ProjectsCatalogueTest.java` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/showroom.spec.js` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/overview.spec.js` — present at HEAD 94bbefe1
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.svelte` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.scenarios.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/components/Tabs.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.test.ts` — present at HEAD 730380ee (PLAN-89)
- OBSERVED: `pm-web-app/src/lib/components/IconButton.svelte` — present at HEAD 730380ee
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/vite.config.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-app/src/lib/dev/devRouteExclusion.ts` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/tests/helpers/ports.js` — present at HEAD 94bbefe1
- OBSERVED: `pm-web-e2e/scripts/guard-ports.js` — present at HEAD 94bbefe1

## Dependencies and Sequencing

- Depends on: `PLAN-90` (shipped), `PLAN-16` (shipped), `PLAN-89` (shipped 2026-11-02, `730380ee`: read `landings/PLAN-89.md` for the palette and `theme-readability.spec.js` it changed)
- Overlaps with: `PLAN-96` (emitted together; disjoint surface by the gate), `PLAN-54` (running; disjoint), `PLAN-41`, `PLAN-45`, `PLAN-51` (name the formatters or the plan card). `PLAN-89` and `PLAN-47` have landed: the single file shared with PLAN-89, `theme-readability.spec.js`, is free for deliverable 4.
- Adjacent to: `TokenFormat` (Java, terminal; untouched)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-93-web-format-and-token-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

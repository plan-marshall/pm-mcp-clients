# PLAN-100: Web: What the Acts Residue Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-05 from the landing analysis of PLAN-96 (`landings/PLAN-96.md`: PR #320 merged with FOUR unresolved CodeRabbit threads, a never-handled second review and a review-body finding). The two findings in the mock runtime (the drain events after a withdrawal, the three-step shrink of a four-item drain) are PLAN-95 deliverable 6, not here. One pull request on purpose: each pull request costs a CodeRabbit review. Keep it under 100 files.

## Objective

PLAN-96 closed the acts PLAN-47 left open, but its second CodeRabbit review was never handled: the settings page reads devices without a bound or a sequence guard, a lost response to a drain withdrawal is not treated as unknown and the withdrawal request has no timeout, and tests of the busy guards pass without the guards. This plan finishes the bounded, ordered reads and requests, makes the guard tests able to fail, and fixes the small defects of the work panel.

## Deliverables

1. **The reads and requests of the settings page and the drain acts are bounded and ordered**
   - What: (a) `SettingsView.svelte` `loadData()` (~77-97) bounds `getDevices` and `getStatus` with an `AbortSignal` timeout (the way `+layout.svelte` bounds the runtime acts) and a failed or timed-out read of the revoke recovery ends in the error state, not a hang (CodeRabbit thread `:159`). (b) An older `loadData` cannot overwrite a newer one: a sequence number or an abort of the previous read (thread `:189`). (c) `client.ts` `withdrawStop()` (~2971) takes a signal and `+layout.svelte` (~94) passes a bounded one: a pending DELETE must not keep `drainingBusy` true and disable both drain acts (review-body finding of `5778430658`). (d) `+layout.svelte` (~122-125) treats `network_offline` as an unknown outcome like `TimeoutError` and `AbortError`: the act ends with the "unknown" notice and a bounded refresh of the state (comment `4475255422`, resolved without a reply).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Tests that fail before: a hung devices read ends in the error state within the bound; a stale read does not overwrite a newer list; a hung withdrawal ends and re-enables both drain acts; a lost response (`network_offline`) ends with the unknown notice and one bounded refresh (evidence order: `test-first`)
     - The paths of `stop_now_unknown` (the `TimeoutError` path) and the revoke-timeout `loadData` path each have a test (evidence order: `test-first`)
2. **The guard tests can fail**
   - What: `ConfirmDialog.svelte` `confirm()` (~47) has a busy guard, but `Button.svelte` (~47) already sets `disabled={disabled||loading}`, so the "double confirm" tests in `ConfirmDialog.test.ts`, `layout.test.ts` and `SettingsView.test.ts` pass without the guard. Make the guard reachable by a test that fires the handler without the button's `disabled` (call the confirm handler twice, or dispatch the event on the dialog) and fails when the guard is removed; keep the button-level test as it is. The layout shrink test sets the store directly: add one through `handleLifecycleEvent`. The Shift+Tab-from-dialog-box fix in the end-dialog trap (`WorkControlPanel.svelte` ~273) gets a test. The `data-table.spec.js` row-menu measurement runs on a product view as well as on `/dev/components`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`)
3. **The work panel's small defects and the act ids**
   - What: (a) `chooseEpic('cancel')` clears `failure` before it opens the dialog (`WorkControlPanel.svelte` ~382-386; shown at ~1038). (b) `openHold` does not open the dialog after a failed hold read (~759-768). (c) An unknown running count is not shown as `0` in the raise dialog (~366, ~895): say it is unknown. (d) `ActionBar.svelte` (~245) keeps a `span` out of `role="menu"`. (e) PLAN-96 D4a, "only ids the panel can handle are posted": only `decision` is filtered from the bar today (`WorkControlPanel.svelte` ~132, ~357); every other offered id other than the routed `report`, `resolve_hold` and `follow_up` is still posted through `handleAction`: post only the ids the panel handles, and drop or route the rest. (f) The design sentence "each one step to its view" was deleted from `system.adoc` rather than implemented (PLAN-96 D3): implement it in the draining state, or say in the design why it is not.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test per item (a) to (e) that fails before (evidence order: `test-first`)
     - The decision of (f) is in the design with its reason; a browser check (Playwright MCP, `AGENTS.md` § Browser checks) of the draining state and the raise dialog, `de` and `en`, light and dark, 1440 × 900 and 390 × 844, before and after (evidence order: `none`)
4. **Every open thread is answered, and the leads are read**
   - What: (a) Each of the four unresolved CodeRabbit threads of PR 320 (`MockRuntimeEngine.java:1330` and `:8383` are answered with the pointer to PLAN-95 deliverable 6 once that has merged, or fixed here if PLAN-95 has not; `SettingsView.svelte:159` and `:189` by deliverable 1) and the resolved-without-a-reply thread `+layout.svelte:122-125` get a reply naming the commit or a stated reason. (b) Read the leads and record the result: whether the real `pm-mcpd` accepts `{with_plans, reason}` for an epic cancel and sends `drain_remaining_items` (the daemon is another repository: say what was not checkable); whether `/dev/components` is reachable in the e2e dev server and `pm_locale` is the right storage key.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - The pull request lists the threads with the commit or reason of each, and none is unresolved at the head (list them through the GraphQL `reviewThreads` read) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-web-app verify` and, with `PM_MOCK_WEB_PORT=21000 PM_WEB_PORT=21001`, `./mvnw -pl pm-web-e2e verify -Pe2e` pass at the head (a failure beyond the base names is yours).
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `21000`, app port `21001`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario running-work --web-port 21000"`), then `PM_MOCK_WEB_PORT=21000 ./pm-web-app/start.sh -p 21001`; pair again every session with a fresh single-use code.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads. PLAN-96 merged with its second review unhandled: before you merge, list the unresolved threads of your own pull request through the GraphQL `reviewThreads` read, and state the count at the head in your landing message.

## Claim Labels

- OBSERVED: `SettingsView.svelte` `loadData()` has no timeout and no sequence guard, and `client.ts` `withdrawStop()` passes no signal — read by the orchestrator at `6cf1aac0`
- OBSERVED: four CodeRabbit threads of PR 320 are unresolved (`MockRuntimeEngine.java:1330`, `:8383`, `SettingsView.svelte:159`, `:189`) — read by the orchestrator through GraphQL on 2026-11-05
- OBSERVED: `scheduleDrainShrink` schedules `seconds <= 3` — read by the orchestrator at `6cf1aac0` (PLAN-95 deliverable 6)
- HYPOTHESIS: `network_offline` is not classified as an unknown outcome, the busy guard of `ConfirmDialog.confirm()` is not reachable through the button, and the work panel defects of deliverable 3 — by a read-only analysing agent's reading at `6cf1aac0`, not run
- HYPOTHESIS: the real `pm-mcpd` accepts `{with_plans, reason}` for an epic cancel and sends `drain_remaining_items` — not checked (another repository)
- Source: `landings/PLAN-96.md`, `landings/PLAN-47.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/routes/layout.test.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.test.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.test.ts` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-e2e/tests/data-table.spec.js` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD 6cf1aac0
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD 6cf1aac0

## Dependencies and Sequencing

- Depends on: `PLAN-96` (shipped)
- Overlaps with: `PLAN-95` (deliverable 6 edits the mock for the same drain feature; the two threads in `MockRuntimeEngine.java` are answered by whichever lands second), `PLAN-93` (running; `format.ts`, the catalogue, the e2e specs: `control.spec.js`, `system.spec.js` are not in its surface), `PLAN-99` (staged; `ReportsView`: disjoint)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-100-web-acts-second-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

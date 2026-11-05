# PLAN-96: Web: What the Acts-by-State Plan Left Open

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-02 from the landing analysis of PLAN-47 (`landings/PLAN-47.md`: PR #311 merged by the operator with NO CodeRabbit review of any head, so the read-only analyst's own review stands in) and of PLAN-89 (`landings/PLAN-89.md`: the row-menu overhang). One pull request on purpose: each pull request costs a CodeRabbit review (quota-limited). Keep it under 100 files (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

PLAN-47 gave the web app one primary act per state, but merged without the required review and with gaps against its own spec and design: an epic's cancel runs without a confirmation, focus drops to `body` after a confirmed act, the overflow acts lose their disabled reason and their 44 px target, the drain banner counts but does not list or count down, and a few guards and tests are weaker than they look. PLAN-89 left the row-menu overhang (50 px) as residue. This plan closes them. One pull request. The sources of the web acts are `pm-web-app/src/lib/actions/byState.ts`, `components/ActionBar.svelte`, `components/ConfirmDialog.svelte`, `components/WorkControlPanel.svelte`, `routes/+layout.svelte`.

## Deliverables

1. **Confirmation and focus keep their promises**
   - What: (a) An epic's `cancel` (and every act the table marks destructive) asks through `ConfirmDialog` before it runs: `chooseEpic` (`WorkControlPanel.svelte` ~263-282) sends everything but next, common_files and archive straight to `handleAction`. (b) Focus after a confirmed act: `confirmPause`, `confirmEnd`, `confirmRaise`, `confirmOverlap` and the archive confirm call `closeDialog()` and then `handleAction()`, which sets `busy` before the tick, so the opener is disabled and the first-enabled fallback finds nothing; focus must return to the opener (or the first enabled control) when `busy` clears. This is the PR 297 finding that PLAN-47 inherited by removing `ControlActionsBar`; the orchestrator's mailbox message to the lane required the fix and a failing test, and the lane report never mentions it. (c) `ConfirmDialog` takes a `busy` guard: a double confirm is ignored and Escape or cancel does not close it while the request runs; `SettingsView` passes it. (d) The draining acts in `routes/+layout.svelte` (~50): "Stop now" ends running work, so it asks (check the design sentence in `shared/system.adoc` and `work-actions.adoc` first; if the design says it asks nothing, record that and keep it), both acts take a busy state and a `.catch` that shows the refusal. (e) The plan's end dialogue (`WorkControlPanel.svelte` ~653-733) keeps its own reason-and-pull-request form (documented in `interaction-affordances.adoc`), but it has no focus trap, no `aria-modal` and no danger consequence list: give it the trap and `aria-modal`, and the consequence list the design asks for, or change the document with the reason.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A test per item (a) to (d) that fails before: an epic cancel opens the dialog and sends nothing until confirmed; the focus is on the opener after a confirmed pause, and again after `busy` clears when the opener was disabled; a second confirm while busy sends nothing; "Stop now" and withdraw show a refusal and ignore a second click (evidence order: `test-first`)
     - Browser check (Playwright MCP, `AGENTS.md` § Browser checks) of an epic cancel, a confirmed pause with the keyboard only, and the draining state, `de` and `en`, light and dark, 1440 x 900 and 390 x 844, before and after (evidence order: `none`)
2. **The overflow acts and the row menu are usable**
   - What: (a) A disabled act in the overflow menu carries its reason as `aria-describedby` text (visible or screen-reader-only), not only as `title`, and disabled items stay reachable by keyboard navigation (`ActionBar.svelte` ~72, ~215-220): an epic's archive in `landing` ("the ledger is landing") is unreachable today. (b) Overflow items get a 44 px target on phones (`.action-bar__item`, ~260-269; PLAN-37 gave the old bar 44 px). (c) The row-menu button of `DataTable` no longer overhangs its 60 px column (`DataTable.svelte` ~121 and the column at ~198-200 under `table-layout: fixed`): PLAN-89 measured 102 px in a 60 px cell and answered "outside this plan's surface"; the remedy is the plan's to choose (a wider column for rows that carry the visible label, the short label, or the icon) and it is measured.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests: the reason is the described-by text of a disabled overflow act and the item can be reached with the arrow keys; they fail before (evidence order: `test-first`)
     - A browser assertion in `pm-web-e2e` measures the row-menu button against its cell (`getBoundingClientRect`, no timer wait) at 1440 and 390 and fails when the overhang returns; a 44 px assertion on an overflow item at 390 (evidence order: `mutation-after`)
3. **The drain banner lists what runs and counts down**
   - What: PLAN-47 D3 specified "lists what runs and counts down, the count taken from the same list"; it counts only. `drain_remaining_items` exists in `pm-api` (`RuntimeStatus.java` ~64) but the mock never sends it, so the list is never drawn, and nothing counts down. (a) The mock sends `drain_remaining_items` for the draining scenario and shrinks it as work finishes (`MockRuntimeEngine.java`; PLAN-95 also edits this file: read its landing or sequence after it). (b) The banner (`routes/+layout.svelte` ~42-52, ~166-195, `stores/runtime.ts`) draws the list and the count from the same list, and updates as items leave; the countdown is the one the design states (read `shared/system.adoc` first and say which sentence it follows). (c) Remove the dead handlers in `SystemView.svelte` (`withdrawStop`, the `withdraw_stop`/`stop_now`/`start` cases ~368, ~404-428): `showRuntimeActs` hides the bar in draining, upgrading and stopped, so the `RUNTIME_ACTS.upgrading` and `stopped` rows are never drawn; either draw them or delete the rows, and say which.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:java-core
   - Done when:
     - A store/layout test and `system.spec.js` assert the list, the count, and that the count falls when an item leaves; they fail before (evidence order: `test-first`)
     - The dead handlers and rows are gone or drawn; no unreachable branch remains (`git grep` for the removed names) (evidence order: `none`)
4. **Act ids and the tests that guard them**
   - What: (a) `chooseAct` posts an API-offered `decision`, `resolve_hold`, `report` or `follow_up` as a plan act (`WorkControlPanel.svelte` ~259), and the ended path ignores `PLAN_ACTS.ended` (~115-117): only ids the panel can handle are posted; others are dropped from the bar or routed. (b) `devices.spec.js` ~113-118 was loosened (accepts `revoked-view` OR the pair button): restore the strict assertion for the revoke of this browser. (c) `expectNothingRedAtRest` (`pm-web-e2e/tests/helpers/acts.js` ~12-27) is a one-shot evaluate, returns `[]` whenever any `[role="dialog"]` is mounted, and compares only the exact `--color-danger` background: make it wait on the condition (no timer) and not skip silently. (d) The draining e2e test does not check the list, the count or the countdown and its "no overflow" assertion is true by construction (`SystemView.svelte` ~391); `actsFor` with offered acts is checked only for "an unknown act never becomes primary": add the missing assertions, including that an unknown offered act is never marked destructive by mistake and that a destructive offered act is never primary.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - Each item fails when the code it covers is reverted (evidence order: `mutation-after`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is fixed or dismissed with a stated reason (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md): `./mvnw -pl pm-web-app verify` and, with `PM_MOCK_WEB_PORT=20960 PM_WEB_PORT=20961`, `./mvnw -pl pm-web-e2e verify -Pe2e` pass at the head (the seven base failures were fixed by PLAN-90; a failure beyond the base names is yours).
- No new dependency or plugin.
- Web standards (CLAUDE.md): ES modules, flat ESLint config, no `innerHTML`/`{@html}`, no `waitForTimeout` and no `setTimeout` that only passes time; the npm scripts `lint`, `format`, `test`, `test:ci-strict` and `quality` stay.
- Every user-facing text goes through the catalogue (`$t(...)`) and sits in both `en` and `de`; the text guard (`text-guard-baseline.json`) only shrinks.
- The design document is updated in the same pull request as the code (UX standing rule of the epic).
- Ports of this plan: mock web port `20960`, app port `20961`. Other plans may run at the same time: never stop or reuse a process this worktree did not start.
- Browser check with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario running-work --web-port 20960"`), then `PM_MOCK_WEB_PORT=20960 ./pm-web-app/start.sh -p 20961`; pair again every session with a fresh single-use code.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`) — treat it as `sonar: configured`. An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason.
- Merge rule (the operator, 2026-11-02): at least one CodeRabbit review of some head with its findings handled; later heads need no review, re-trigger once and never wait. Handle the findings of EVERY CodeRabbit review that exists, a late one included, and read the review BODIES as well as the threads. PLAN-47 merged with no review at all: this plan's pull request is the first review of these files.

## Claim Labels

- OBSERVED: epic cancel goes straight to `handleAction` (`WorkControlPanel.svelte` `chooseEpic`) and the overflow item carries the reason only as `title` (`ActionBar.svelte` ~215-220) — read by the orchestrator at `f6504d56`
- OBSERVED: `confirmPause/End/Raise/Overlap` call `closeDialog()` and then `handleAction()` — read by the orchestrator at `f6504d56`; that the focus then drops to `body` is the analysing agent's reading of the `busy` path, not run (verify-at-outline)
- HYPOTHESIS: the mock never sends `drain_remaining_items`; the real `pm-mcpd` may send it or re-emit `runtime.draining` as work finishes — not checked (the daemon is another repository)
- HYPOTHESIS: the dead handlers of `SystemView` cannot run (`showRuntimeActs` hides the bar in those states) — read by the agent, confirm per branch
- HYPOTHESIS: the row-menu overhang is still 50 px at `730380ee` — the operator's own measurement in the PR 305 comment; PLAN-89 changed `DataTable` after it
- Source: `landings/PLAN-47.md` (defects 1 to 10), `landings/PLAN-89.md` (lead 1), the mailbox message `inbox/to/rb-47-web-actions-by-state/orchestrator-001.md`.
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.test.ts` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/components/DataTable.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/actions/byState.ts` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/actions/byState.test.ts` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-e2e/tests/helpers/acts.js` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-e2e/tests/devices.spec.js` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD c7d9dcb8
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD c7d9dcb8
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at HEAD c7d9dcb8
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at HEAD c7d9dcb8
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD c7d9dcb8

## Dependencies and Sequencing

- Depends on: `PLAN-47` (shipped), `PLAN-89` (shipped)
- Overlaps with: `PLAN-95` (edits `MockRuntimeEngine.java`; PLAN-95 is NOT emitted until this plan lands, so deliverable 3's mock edit comes first and PLAN-95 rebases on it), `PLAN-93` (D5 edits `CommandPalette`, `format.ts`, the e2e specs; one shared spec file `theme-readability.spec.js` is not in this plan's surface), `PLAN-54` (running: reports and designer; may touch `DataTable` consumers), `PLAN-59` (unenrol typed name and the projects page acts; deferred there by PLAN-47's spec), `PLAN-55` ("Open the decision" act)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-96-web-acts-residue.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

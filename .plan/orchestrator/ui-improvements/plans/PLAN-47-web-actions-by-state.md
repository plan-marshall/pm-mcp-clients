# PLAN-47: Web: One Primary Act per State

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Introduce the acts-by-state rule of UX-P-04 and UC-P-02 with `ActionBar` behaviour and `byState.ts`, apply it to the system, the devices and the shared control panel, put destructive acts in the overflow behind confirmation, and fix the draining acts and banner (UX-S-05, UC-S-03, the draining part of CC-S-06). The plan, epic and project views take their acts from `byState` in the plans that rebuild those views (`PLAN-55`, `PLAN-58`, `PLAN-59`).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-05)`
- `doc/claude_review/ux/proposals.adoc#UX-P-04`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-02`
- `doc/claude_review/use-cases/plan.adoc (UC-S-03)`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-09`

## Deliverables

1. **Acts by state**
   - What: `lib/actions/byState.ts` with one table per resource kind (the excerpt of UX-P-04 and the states the use cases name); where the API sends offered acts, they win.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `byState.test.ts`: every state has at most one primary act; destructive acts are never primary (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`, `decisions.open`; `reports-and-system` → `system.draining`; `/projects`; paired devices — one primary act, nothing red at rest, confirmations name consequences; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **ActionBar behaviour and ConfirmDialog**
   - What: `ActionBar` takes `acts: Act[]`; `ConfirmDialog` names the consequences in the danger colour; unenrol and "revoke all others" ask for the typed name.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: the confirmation for a destructive act; the typed-name check (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`, `decisions.open`; `reports-and-system` → `system.draining`; `/projects`; paired devices — one primary act, nothing red at rest, confirmations name consequences; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **System, devices and the shared control panel**
   - What: `SystemView`, `SettingsView` (devices) and `WorkControlPanel` use `ActionBar`; `ControlActionsBar` goes; release, unenrol and revoke sit in the overflow; in `system.draining` only stop now and withdraw, the banner lists what runs and counts down, and its count comes from the same list. The views that HOST the panel keep their own acts with the plans that rebuild them (the structure review of 2026-10-30): the plan page's acts, the completion-report control for an ended plan only and a waiting plan's primary act in `PLAN-55`; the epic page's acts in `PLAN-58`; the project acts in `PLAN-59` D4. They all take their acts from this plan's `byState`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `control.spec.js`, `system.spec.js`, `devices.spec.js` per state; e2e asserts no element with the danger background outside an open dialog (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready`; `reports-and-system` → `system.draining`; paired devices — one primary act, nothing red at rest, confirmations name consequences; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20470 PM_WEB_PORT=20471 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20470`, app port `20471` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20470"`), then `PM_MOCK_WEB_PORT=20470 ./pm-web-app/start.sh -p 20471` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20470/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-05)`, `doc/claude_review/ux/proposals.adoc#UX-P-04` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/ux/plan.adoc:40-43 UX-S-05 maps to UX-P-04; ux/proposals.adoc:176-177 '== UX-P-04: One Primary Act per State' exists
- HYPOTHESIS: the API does not yet send the offered acts for every resource — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/control/` (verify-at-outline)
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: pm-api/.../api/control/WorkActions.java:25,37 carries offered acts for plan and epic only (subject_kind plan|epic); no acts list on system, project, device or decision resources
- Re-grounded note at HEAD 2c25ed1d (2026-11-02, orchestrator, read not run): the shipped plans changed what this spec assumes. (1) `ActionBar.svelte` and `ActionBar.test.ts` EXIST (PLAN-25, `8b19a4a7`): one primary, up to three secondary, the rest in an overflow `IconButton` menu with arrow/Home/End/Tab/Escape handling; prop `acts` (`id`, `label`, `advisory`, `disabled`, `disabledReason`), `busy`, `onAct(id)`; `doc/design/web-app/components.adoc#action-bar` says "which acts a state offers arrives with the actions-by-state work". Deliverable 2 therefore EXTENDS this component, it does not create it; keep its overflow behaviour and tests. `ConfirmDialog.svelte`, `lib/actions/byState.ts` and its test are still absent; `Dialog.svelte` exists. (2) `ControlActionsBar.svelte` (619 lines) was reworked by PLAN-37 (`c5bfb1ca`): Escape on `window`, the opener remembered and restored (with a first-enabled fallback when a confirm disabled the opener), `aria-describedby` to the visible reason on disabled acts, 44 px phone targets (five places), covered by `ControlActionsBar.test.ts` and three `workspace.spec.js` checks. Deliverable 3 removes this component: carry those behaviours and keep those tests green (or move them to the replacement), never lose them silently. `WorkControlPanel.svelte` also carries PLAN-37's 44 px target. (3) `SystemView.svelte` now sits inside `DashboardPage` (PLAN-43, `2c25ed1d`: header in the template slots, `.system-container` as content block, `pageWidth.test.ts` guards the root) and PLAN-37 added aria-labels to the work figures; the running PLAN-90 deliverable 3 edits the same file (words and the frozen load-failure string), different hunks: rebase if it lands first. "No own page width" is now enforced by `pageWidth.test.ts`; a view this plan touches that is not listed there stays unchecked. (4) `IconButton` draws its label from 768 px (PLAN-37) and the running PLAN-89 deliverable 5 verifies how that reads in the `ActionBar` overflow and the `DataTable` row menu: do not change `IconButton` here. (5) The design-system v2 data components (PLAN-35: `DataTable`, `EmptyState`, `ErrorState`, `Skeleton`) exist for any list this plan touches. (6) The pm-api `WorkActions.java` HYPOTHESIS below was last corroborated at `cc5794c9`; re-confirm at the current HEAD as the verify-first clause says.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/actions/byState.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/actions/byState.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ActionBar.svelte` — present at HEAD 2c25ed1d (created by PLAN-25); this plan extends it
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ControlActionsBar.svelte` — present at HEAD c4dc9bb4 (reworked by PLAN-37; removed by this plan)
- OBSERVED: `pm-web-app/src/lib/components/ControlActionsBar.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/devices.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at HEAD c4dc9bb4; this plan owns its update for the acts and the confirmations (the former `PLAN-74` share)

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-76` (`shared-port-assignment`) — both shipped
- Overlaps with: PLAN-90 (running; `SystemView.svelte`, different hunks), PLAN-89 (running; `IconButton`, `DataTable`, `ChatPanel` verification, no shared declared file)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-47-web-actions-by-state.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

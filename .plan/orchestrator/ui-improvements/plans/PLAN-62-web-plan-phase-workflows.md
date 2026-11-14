# PLAN-62: Web: Plan Phases Show Their Workflows and Intent

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Make the plan tree keep its promise (UC-P-04): true expanders, phase nodes that expand to their workflow (taken and not taken), a phase narrative, the Intent tab content, "Not in this plan", and the breakdown control only when reported (UC-S-05).

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-05)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-04`
- `doc/claude_review/use-cases/findings-plans-control.adoc`

## Deliverables

1. **The process tree: rows, true expanders and phase workflows** (the tree rows come from `PLAN-55` D3, moved here: this plan owns the tree)
   - What: Tree rows: icon, name without the kind, state word, time; a "Jump to now" icon button in the tree head; an expander only for a node with children; a phase expands to its steps taken, not taken (muted, with reason) and sub-workflows, from the instance diagram the server draws (`PM-IMPL-5`).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `plan-view.spec.js` on `process.phase.*`: taken and not-taken steps; a leaf has no expander; tree rows without the kind in the name; "Jump to now" scrolls to the running node (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `process.phase.*`, `process.pruned-lane`, `intent.ready`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Phase narrative and intent**
   - What: A phase without task activity shows outcome, duration, loop count, and for outline and refine a link to the Intent tab; the Intent tab shows goal, criteria, non-goals.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `intent.ready` shows the three parts (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `process.phase.*`, `process.pruned-lane`, `intent.ready`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **Not in this plan; breakdown**
   - What: A list "Not in this plan" under the phases with the reason; "By phase and role" only when reported.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `process.pruned-lane` shows the list; no breakdown control without data (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `process.phase.*`, `process.pruned-lane`, `intent.ready`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20620 PM_WEB_PORT=20621 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20620`, app port `20621` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20620"`), then `PM_MOCK_WEB_PORT=20620 ./pm-web-app/start.sh -p 20621` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20620/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-05)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-04` — read at `doc/claude_review/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: UC-S-05 at use-cases/plan.adoc:50, UC-P-04 proposals.adoc:98, findings-plans-control.adoc present
- HYPOTHESIS: the instance diagram data of `PM-IMPL-5` is available through the API the plan view already reads — confirm/refute at `pm-api/src/main/java/de/planmarshall/api/process/` (verify-at-outline)
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: pm-api process/ has ProcessTree, ProcessNode, NodeState, PruneReason; client.ts:3424 getPlanProcess; PlanView.svelte:171 loadPlanProcess
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): client.ts line cited in older evidence moved (getPlanProcess now client.ts:3107); PLAN-43 shipped ObjectPage template, relevant to the PlanView rework; PlanView, NodeNarrative, processNode.ts and plan-view.spec.js changed (PLAN-90/a11y/token work).
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Drift only: getPlanProcess now client.ts:3424; templates/ObjectPage.svelte shipped; PlanView.svelte 1721 lines
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: procedural verify-first instruction; no code fact
- HYPOTHESIS: a leaf node renders no expander today — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte:885-896` § `toggle-btn` (verify-at-outline)
  - verdict: contradicted | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: yes | evidence: PlanView.svelte:899-921 renders the toggle button for every node; a leaf gets a blank glyph and aria-expanded undefined (hasChildren :622), so an inert expander button still exists | re-scoped by the spec section Re-Grounding 2026-11-14 (cleanup)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/NodeNarrative.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/NodeNarrative.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/utils/processNode.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/utils/processNode.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-55` (`web-plan-page`), `PLAN-03` (`mock-scenario-states`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: `web-plan-page` (same view): runs after it
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 006, 007). **Gate: D1 and
D2 merged** (documents `AGREED`) and the D1 foundation / D2 navigation engine packages. Queue status: `parked`; the
orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT widened now (a parked row is not in
the disjointness gate population). When restarted:

- The phase tree is a **column of the plan**; its tree icons must not clash with the kind marks (the icons of this plan stay
  the phase / task icons; `KindMark` icons are D1's) (003).
- `PhaseLabel` and `StatusLabel`; no "Phase phase …" in a node row (the node shows "init" with the phase icon) (006).
- A node row = `RowLink` + a disclosure button with `aria-current` for the selected node; **plain arrows stay with the tree**,
  `Ctrl+←/→` move between columns (007, 003 §7).

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- `RowLink` (PLAN-148) supports no tree role and has no covered-safe place for a disclosure button except `acts`; the spec's "RowLink + a
  disclosure button with aria-current; plain arrows stay with the tree" (`:95`) must be re-designed at outline (read `RowLink.svelte` at
  HEAD and `PLAN-49`'s changes to it).

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Claim 4 contradicted: PlanView.svelte:899-921 renders the toggle button for every node; a leaf gets a blank glyph and aria-expanded undefined (hasChildren :622), so an inert expander button still exists
- Already done: the not-in-plan box (#70). Claim 4 refuted: `PlanView.svelte:899-921` renders an inert toggle on every leaf (aria-expanded undefined).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-62-web-plan-phase-workflows.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/plan-view.adoc` (#levels, #states, #kinds, #leaf, #position, #tokens), `doc/design/web-app/navigation.adoc` (#levels, #url, #keys), `doc/design/web-app/components.adoc#fixed-places`, `doc/design/web-app/components-tags.adoc#phase-label`, `doc/design/shared/intent-and-outline.adoc#web`. HEAD 1cc0e10e. Deliverable 3 is done or dropped and deliverable 2 loses its Intent part, so two deliverables remain.

1. **The phase tree in the Process tab and as the plan's list column** (D6: the tree stands in the _Process_ tab of the plan's object column and is the plan's list column only while a task is open; see `## D6 Merged 2026-11-11 (Amendment)`). Rows: icon, name without the kind, state word, time (a node shows "init" with the phase icon, `PhaseLabel`/`StatusLabel`); an expander only for a node with children; a phase expands to its steps taken, not taken (muted, with the reason) and sub-workflows from the instance diagram the server draws; a node is selected by `node={path}` (a replace) and a task opens as its own level; "Jump to now" is an `IconButton` in the tree head (today a text button, `PlanView.svelte:683-687`); plain arrows move within the tree, `Ctrl+←/→` between columns.
   - Done when: `plan-view.spec.js` on `process.phase.*`: taken and not-taken steps, a leaf has no expander, tree rows without the kind in the name, "Jump to now" scrolls to the running node, the address carries `node=` (evidence order: `test-first`); the browser check on `running-work` → `process.phase.*`, `process.pruned-lane` (evidence order: `none`).
2. **Phase narrative.** A phase without task activity shows outcome, duration, loop count, and for outline and refine a link to the Intent tab. (The Intent tab content is the web intent and outline package's.)
   - Done when: a `NodeNarrative` test shows the three facts; the link opens the Intent tab (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).

Done already, with a positive account: "Not in this plan" with the reason (`PlanView.svelte:96-103,988-995`, key `plan.not_in_plan.group`; `plan-view.adoc#levels`). Dropped: "by phase and role only when reported": the one-step breakdown stays (`plan-view.adoc#tokens`; `PlanView.svelte:482-499`); a test pins that a running plan says the figure is not reported yet rather than hiding the control.

Implements: `doc/design/shared/plan-view.adoc` § Levels, Phase, Sub-Workflow, State, Where It Is; `doc/design/web-app/navigation.adoc` § Levels as Columns.

Dependencies: `PLAN-55` (same view; runs after it), D1 foundation (PhaseLabel, StatusLabel, IconButton, RowLink), D2 navigation engine (the plan's list column, `node=` and task addresses, keys), the web intent and outline package (the link target). `PLAN-03`, `PLAN-76` have shipped. Sequence after `PLAN-45`.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## D6 Merged 2026-11-11 (Amendment)

Source: the D6 design session `PLAN-141`, merged as #403 (`8568b56e`, 2026-11-10); answers Q1 and Q3 of
`landings/PLAN-141-findings.md` § Answers. Documents read on `main` at `4a84f727`: `doc/design/web-app/plan-view-interaction.adoc`
(#layout :14-40, #selection :42-68, #defaults :70-75, #node-narrative :77-87, #addresses :122-129),
`doc/design/web-app/navigation.adoc` (#url :178-222, the row "A node of the plan's phase tree"; #rely, the row of `PLAN-62`),
`doc/design/shared/plan-view.adoc#tabs`. The D6 wait is over; these supersede any text above that differs.

- **Where the tree stands.** The tree and the selected node's narrative stand inside the plan's _Process_ tab (the tab, its layout —
  side by side from an object column of `720 px`, tree `280`–`360 px` — and the tab switch are `PLAN-55`'s). While a task of the plan is
  open as the next level, the plan is a list column whose list is this tree. This plan's rows, expanders, narrative and "Jump to now"
  serve both forms.
- **`node=` only with Process.** `node={path}` is written by a replace only while the Process tab is shown, never with Intent or
  Changes and never on a task's address; the follow mode writes no node.
- **A tab switch drops the key, the selection stays in memory.** Switching away from Process replaces the address without `node=`;
  the view keeps the selected node in memory while the plan stays open, so going back to Process shows it again and writes `node=`
  again. (The switch itself is `PLAN-55`'s; this plan keeps the tree's selection in a state that survives it.)
- **Selecting a part in the list column closes the task**: a push of the plan's address with the part's `node=` and without
  `follow=on` (the selection ends the follow mode); the task's level unsubscribes from `subscribeTaskActivity`
  (`plan-view-interaction.adoc#selection` :67).
- **The narrative's link to the Intent tab**: for `refine` and `outline` the link is _Intent_ (`intent.tab.title`, area `intent`), a
  replace to `tab=intent` that drops `node=` (deliverable 2).
- Done when (added to deliverable 1): `PlanView.test.ts` or `processNode.test.ts`: `node=` absent from the address on Intent and
  Changes and restored on the return to Process; a part selected in the list column pushes `node=` without `follow=on` and closes the
  task (evidence order: `test-first`).

Dependencies added: `PLAN-143` (the one address writer, `node=`), `PLAN-145` (the catalogue area `intent` for `intent.tab.title`).
Expected Surface: unchanged.

## Unparked 2026-11-14 (cleanup, operator)

Emit after `PLAN-55`. **PLAN-55 stays parked on the plan-changes rows `PLAN-162` and `PLAN-164`, so this row waits behind it.**
- Re-ground at outline: `RowLink` has no tree role, so redesign the row and its disclosure.
- Claim 4 is refuted: the toggle is inert at `PlanView:899-921`.
- The not-in-plan box is done (#70).

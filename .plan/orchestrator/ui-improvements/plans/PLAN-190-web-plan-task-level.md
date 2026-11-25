# PLAN-190: Web: The Task as the Plan's Next Level

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Split off from `PLAN-55` at its re-grounding of 2026-11-25 (`459eca7d`), the scope-bloat guard: D6-1's list-column form and the activity rows' move were a separate unit, and the activity rows depend on it.

## Objective

Open a task of a plan as the next level of the column engine: the task's own address `/plans/{id}/tasks/{task}`, the plan as the list column with its compact head and the phase tree as the list, and the task's object column with its own activity rows (`doc/design/shared/plan-view.adoc#tabs` "Not a tab"; `doc/design/web-app/plan-view-interaction.adoc#layout` :20; D6-1 of `PLAN-55`'s `## D6 Merged 2026-11-11 (Amendment)`).

Source of this plan (read before scoping):

- `doc/design/shared/plan-view.adoc` (#tabs, "Not a tab")
- `doc/design/web-app/plan-view-interaction.adoc` (#layout, #addresses)
- `plans/PLAN-55-web-plan-page.md` § `## D6 Merged 2026-11-11 (Amendment)` (the list-column bullet) and § `## Re-Grounding 2026-11-25`

## Deliverables

1. **A task is the next level**
   - What: selecting a task in the plan's tree pushes `/plans/{id}/tasks/{task}` (today `selectTask`, `PlanView.svelte:758-760` at `459eca7d`, only sets local state; the route `routes/plans/[id]/tasks/[task]/+page.svelte` renders the same `PlanView` through `AppRouter`). With a task open, the plan is the **list column**: `ObjectPage` `compact` (`KindMark`, title, state word, the main act as a secondary `Button`) and the phase tree as its list, nothing of the other tabs; the task is the object column beside it. Selecting the plan's head closes the task (a push back to `/plans/{id}`) and the plan is the object column again with its tabs. A task's address carries no tab of the plan. At a narrow width the levels follow the column engine's one-column rule (Up from the task to the plan).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `PlanView.test.ts` (or the task view's own test): selecting a task pushes the task address; the plan shows the compact head and the tree as the list column and no tabs; selecting the head returns to the plan's tabs; the task address carries no `tab=`. It fails on a revert (evidence order: `test-first`).
     - `plan-view.spec.js` (scenario `plan-view`): open a task from the tree, reload on its address, return through the head and through Up (evidence order: `test-first`).
2. **The task's activity is the task's level**
   - What: the activity rows of a worker (today in the plan page's narrative, `PlanView.svelte:1302-1358`) stand in the task's object column, never as a tab of the plan; they keep the rules `PLAN-55` D3 sets (no row for a `link` without a target; the kind as an icon with its catalogue word as its name). The task-level off-track banner (`PlanView.svelte:1283-1295`) moves with them.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A test shows the activity rows and the task's off-track banner in the task's column and not in the plan's (evidence order: `test-first`).
3. **The browser check and the design documents**
   - What: the Playwright MCP check of the list-column form and the task column at `1440 × 900` and `390 × 844`, `de`/`en`, dark/light, after the last visual commit; `plan-view.adoc` and `plan-view-interaction.adoc` say what was built.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The snapshots and screenshots are kept as the Standing Constraints say (evidence order: `none`).
     - The two documents match the code (evidence order: `none`).

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=21900 PM_WEB_PORT=21901 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `21900`, app port `21901` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 21900"`), then `PM_MOCK_WEB_PORT=21900 ./pm-web-app/start.sh -p 21901` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:21900/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.
## Claim Labels

- HYPOTHESIS: selecting a task only sets local state and pushes no address — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte` § `selectTask` (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanView.svelte:758-760 selection is local; no navigation call (3dea04b2)
- HYPOTHESIS: the route `/plans/{id}/tasks/{task}` exists and renders `PlanView` — confirm/refute at `pm-web-app/src/routes/plans/[id]/tasks/[task]/+page.svelte` (verify-at-outline)
  - verdict: corroborated | checked_at: 3dea04b2419b7f1f164a7694f4b9aa445965e710 | by: ui-improvements/cleanup | rescoped: n/a | evidence: routes/plans/[id]/tasks/[task]/+page.svelte:3-13; AppRouter.svelte:63-64; runtime.ts:1729-1733 (3dea04b2)
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD (after `PLAN-55` and `PLAN-62` landed) and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/routes/plans/[id]/tasks/[task]/+page.svelte` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.svelte` — present at HEAD 459eca7d
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 459eca7d
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 459eca7d
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at HEAD 459eca7d
- OBSERVED: `pm-web-app/src/lib/navigation/chain.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/templates/columnWindow.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/components/UpLink.svelte` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/routes.test.ts` — present at HEAD 3dea04b2 (cleanup 2026-11-25, declared surface understated)

## Dependencies and Sequencing

- Depends on: `PLAN-55` (the plan on `ObjectPage` and its tabs), `PLAN-62` (the tree rows the list column shows). Not emitted before both land.
- Overlaps with: `PLAN-55`, `PLAN-62` (same view; sequenced, not parallel)


## Re-Grounding 2026-11-25

Re-grounded by the cleanup of 2026-11-25 (read-only sub-agent, contradictions spot-checked by the orchestrator) at `3dea04b2`, against the code and the design and specification changes since 2026-11-15: #570 (`20f971fe`: `over_size`, `epic.overlap_found`, `stale`→`unreadable`, the phase `sanity-check`), pm-mcp-core #607/#614 and plan-marshall-documentation #201/#202 ("observe facts instead of predicting them", two rounds), clients #578 (protected project configuration waits for the operator's decision) and the inbox messages `doc-review-2026-11-17-001` and `doc-review-2026-11-24-001`. **This section binds over the body where they differ.**

- No claim contradicted. Needs PLAN-55 and PLAN-62 landed.
- **D1** takes over from PLAN-62 D6: selecting a part in the tree closes the task by a push with that part's `node=` and without `follow=on`, and unsubscribes `subscribeTaskActivity` (`plan-view-interaction.adoc:20,67`).
- **D2**: the off-track banner and badge render the old closed list; the design's signals are `repeating`, `no_progress`, `failing_again`, `edit_churn`, `agent_started` (`plan-view.adoc:165`, core model-work/03): test with a current signal, never `over_size`/`outside_footprint`; sequence after the `OffTrackSignal` change or rebase.
- **D3** becomes a confirm-and-adjust pass (the design states the built form); include the seven-phase wireframe fix if PLAN-62 has not made it.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-190-web-plan-task-level.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

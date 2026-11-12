# PLAN-59: Web: Projects Grid and Project Onboarding

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Rework projects as a grid or table of cards with acts by state and keyboard ordering of favourites (UX-P-10), and give the project onboarding its identity fields and one "needs the terminal" pattern (projects part of UX-S-11, identity part of UC-S-09; UC-P-07).

Source of this plan (read before scoping):

- `doc/claude_review/ux/proposals.adoc#UX-P-10`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-07`
- `doc/claude_review/use-cases/findings-projects-starting.adoc#UC-F-31`

## Deliverables

1. **Projects grid**
   - What: Three columns from 1280 px, one on the phone, or a table (toggle); card: name, host and short path, state, needs, counts, last activity; favourites ordered by drag and keyboard, the arrow buttons go.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `projects.spec.js`: grid at 1440 px; release and unenrol only in the overflow (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` → `/projects`; `projects-and-start` → `projects.being-added`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Identity step**
   - What: `ProjectOnboardingView`: fields id, name, pilot with the proposal filled in.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `projects.spec.js` on `projects.being-added`: the three fields (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` → `/projects`; `projects-and-start` → `projects.being-added`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **Needs the terminal**
   - What: A step that needs the terminal shows what is missing, the command, "Check again", and what happens after; abandon is secondary and asks first.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A unit test of the pattern; "Check again" re-reads the state (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` → `/projects`; `projects-and-start` → `projects.being-added`; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
4. **Acts of a project, and "Add a plan" on the row** (the acts from the former `PLAN-47` D4, "Add a plan" from the package `web-moving-between-plans`, moved here from `PLAN-83`)
   - What: the project cards and rows use `ActionBar` with the acts of `byState` (`PLAN-47`): release and unenrol in the overflow behind the confirmation; in the web app the row of a ready project carries _Add a plan_ (`project.action.add_plan`) beside its marks, so a plan starts without selecting the project first (it opens the start flow of `PLAN-53` for that project); a project that is not ready shows it disabled with the reason (`project.action.disabled_not_ready`); _Add an epic_ stays among the actions. The terminal client keeps its command. The design is merged (`doc/design/shared/project-explorer.adoc#project-row`, PR #278).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `ProjectExplorerView.test.ts`: the action on a ready row, disabled with its reason on a not-ready row; release and unenrol only in the overflow (evidence order: `test-first`)
     - An e2e test: a plan is started from a project's row without selecting the project first (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded` and a scenario with a project that is not ready: the row with and without the action; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
     - The row of the package `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` if `PLAN-83` and `PLAN-51` D5 have landed (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20590 PM_WEB_PORT=20591 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20590`, app port `20591` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20590"`), then `PM_MOCK_WEB_PORT=20590 ./pm-web-app/start.sh -p 20591` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20590/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/proposals.adoc#UX-P-10`, `doc/claude_review/use-cases/proposals.adoc#UC-P-07` — read at `doc/claude_review/ux/proposals.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: `doc/claude_review/ux/proposals.adoc:339` `[#UX-P-10]` and `use-cases/proposals.adoc` UC-P-07 exist.; re-grounded vs D1-D5 2026-11-10 (Amendment)
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 changed `doc/design/shared/project-explorer.adoc` (`#project-row`): in the web app the row of a ready project carries _Add a plan_ (`project.action.add_plan`) beside its marks, a project that is not ready shows it disabled with the reason (`project.action.disabled_not_ready`), and _Add an epic_ stays among the actions. this plan builds it as deliverable 4 (moved here from `PLAN-83`, together with the project acts of the former `PLAN-47` D4), into the rebuilt grid and table cards. The design also now orders projects as the runtime serves them (needs the operator first, then the start of their oldest active work): "last activity" on a card is information, never a sort key.
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): PLAN-43 shipped (templates); project.action.add_plan and project.action.disabled_not_ready already exist in projects.en.properties (added with the PR #278 design); ProjectExplorerView/ProjectOnboardingView were touched by PLAN-90/a11y.
- RESCOPE at outline (design audit 2026-11-02): Done-when for a package row is 'the row stays done, no link' (Z2).
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
- HYPOTHESIS: `ProjectOnboardingView.svelte` has no id, name or pilot field (only the step key `identity` at :42) — confirm/refute at `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` § identity (verify-at-outline)
- HYPOTHESIS: the explorer does not render `project.action.add_plan` / `project.action.disabled_not_ready` although both keys exist — confirm/refute at `pm-web-app/src/lib/views/ProjectExplorerView.svelte` § actions (verify-at-outline)
- HYPOTHESIS: the project page route `/projects/{id}` does not exist; only `/projects/[id]/start` does — confirm/refute at `pm-web-app/src/routes/projects/` (verify-at-outline)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectExplorerView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProjectOnboardingView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/actions/byState.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/actions/byState.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/projects/` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/projects.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/project-explorer.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/project-onboarding.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/projects.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/projects.de.properties` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-47` (`web-actions-by-state`), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-53` (`web-start-wizard`: _Add a plan_ opens its flow)
- Overlaps with: `PLAN-83` and `PLAN-51` (the package `web-moving-between-plans`)
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 006, 007, 008, 009).
**Hold. Gate: D1 and D2 merged** (documents `AGREED`) and the D2 navigation engine; its chat waits for the D4 web chat
package. Queue status: `parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT
widened now (a parked row is not in the disjointness gate population). When restarted:

- The **one project page** (work, setup, release) and project rows as `RowLink`: Projects becomes list → one project page
  instead of a card grid (003).
- Readiness as `StatusLabel`, "setup needs" as `ReasonBadge` with a catalogue word (006).
- Acts from `PROJECT_ACTS` via `RowMenu` instead of the `ActionBar` on cards of its deliverable 4; "Füge einen Plan hinzu"
  visible on a ready project's row and disabled with its reason otherwise; release / unenrol through `ConfirmDialog` (007).
- `FilterInput`, the release reason in `ConfirmDialog` with `TextArea`, per-aspect settings by the settings-form rule (008).
- The project-onboarding chat in `Chat` (009).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-59-web-projects-and-onboarding.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/project-explorer.adoc` (#layout, #being-added, #project-row, #actions, #project-page, #release, #marks), `doc/design/shared/project-onboarding.adoc` (#steps, #build-environment, #not-ready, #several), `doc/design/web-app/navigation.adoc` (#lenses, #url), `doc/design/web-app/components.adoc` (#row-link, #row-menu, #fixed-places, #confirm-dialog), `doc/design/web-app/components-fields.adoc`. HEAD 1cc0e10e.

1. **Projects as list and one project page.** `ProjectExplorerView.svelte` becomes the Projects lens: the list column (favourites, recent, being added, all projects; rows are `RowLink` with `KindMark`, name and id, `StatusLabel` for readiness, `ReasonBadge` for what the setup needs, work counts, last active as an age; a `FilterInput`), and the project page at `/projects/{id}` as the object column: head with `KindMark`, name, readiness, the favourite `Toggle` and the `ActionBar` (_Add a plan_ primary while ready, _Continue the setup_ while not, _Enrol again_ while released), the work tab (running and ended work), and `?tab=setup` (a later run, release, unenrol). Favourites are ordered by drag and by a keyboard route; the arrow buttons go. A project link outside the chain opens the project page.
   - Done when: `projects.spec.js`: the list at 1440 px, the project page, release and unenrol only in the setup/overflow (evidence order: `test-first`); the browser check on `fully-onboarded` → `/projects` and `projects-and-start` → `projects.being-added` (evidence order: `none`).
2. **The identity step.** `ProjectOnboardingView` ②: the fields id, name and (only while the lifecycle gate holds) pilot, the proposal filled in, on the field system; the run has its own address `/project-onboarding/{entry}`.
   - Done when: `projects.spec.js` on `projects.being-added`: the fields; no pilot field once the gate is open (evidence order: `test-first`).
3. **Needs the terminal.** A step that needs the terminal shows what is missing, the command, _Check again_ and what happens after; _Abandon…_ is secondary and asks first.
   - Done when: a unit test of the pattern; _Check again_ re-reads the state (evidence order: `test-first`).
4. **Acts of a project and _Add a plan_ on the row.** Row acts are `PROJECT_ACTS` (`pm-web-app/src/lib/actions/byState.ts`, changed so that _Add a plan_ is the primary while ready) in a `RowMenu`; _Add a plan_ (`project.action.add_plan`) is visible before the menu and opens the start flow of `PLAN-53` for that project, disabled with `project.action.disabled_not_ready` when the project is not ready; release and unenrol confirm through `ConfirmDialog` (the reason in a `TextArea`). The row of the package `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` by the last of the D2 navigation engine, this deliverable and `PLAN-51` D5 (`PLAN-83` is superseded).
   - Done when: `ProjectExplorerView.test.ts`: the action on a ready row, disabled with its reason on a not-ready row (evidence order: `test-first`); an e2e: a plan is started from a project's row (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).
5. **The onboarding chat and the ✓ Ready figure.** The project-onboarding chat is the `Chat` side column of `WizardPage` (owner: see the ownership note); ✓ Ready shows the mascot's `done` figure (D3 brand).

Removed or replaced: the three-column card grid and the table toggle; `ActionBar` on cards; the arrow-button ordering.

Ownership: the chat of the project onboarding has two owners in the designs (`chat.adoc#rely`); the orchestrator names one.

Implements: `doc/design/shared/project-explorer.adoc` § A Project, The Project Page in the Web App, Release and Unenrol; `doc/design/shared/project-onboarding.adoc` § Steps, The Build Environment.

Dependencies: D1 foundation (RowLink, RowMenu, StatusLabel, ReasonBadge, KindMark, Field, TextArea, ConfirmDialog), D2 navigation engine (the project page route and `tab=`, `/project-onboarding/{entry}`, old addresses replaced), `PLAN-53` (_Add a plan_ opens its flow), D3 brand assets (deliverable 5 figure), D4 web chat package (deliverable 5 chat). Sequence after `PLAN-45`.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.

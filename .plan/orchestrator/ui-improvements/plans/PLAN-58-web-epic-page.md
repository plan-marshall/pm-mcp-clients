# PLAN-58: Web: The Epic Page

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Rework the epic as an object page (UX-P-10): workstreams as a tree with the plan summary beside it, waits with reason and since, tabs including Intent, counts from the rows, and the findings review opening on the first open item (epic part of UX-S-11, UC-S-07 and CC-S-06).

Source of this plan (read before scoping):

- `doc/claude_review/ux/proposals.adoc#UX-P-10`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-06`
- `doc/claude_review/use-cases/plan.adoc (UC-S-07)`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-09`

## Deliverables

1. **Tree and summary**
   - What: Workstreams as a tree on the left, the selected plan's summary on the right; each wait with its reason and a relative "since"; ids shown as chips beside titles; the epic's acts through `ActionBar` and `byState` of `PLAN-47` (`EpicView` and the `WorkControlPanel` it hosts); the page sits on the `ObjectPage` template of `PLAN-43` (this plan moves it there).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `epic-view.spec.js` on `control.epic`: waits show since; titles first (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.epic`, `control.review`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Tabs and Intent**
   - What: Tabs Workstreams, Intent (destination, done-when checks with evidence, non-goals), Landings, Findings and watches, Tokens, Events (timeline).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - The Intent tab is filled on `control.epic` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.epic`, `control.review`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Counts and findings review**
   - What: Head counts and "Landungen" come from the rows the view lists; the findings review opens with the first open item selected and its counts match the card.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: epic counts equal rows; `FindingsReviewModal` selects the first open item (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.epic`, `control.review`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
4. **A failed epic-resource read says so and can be retried (PLAN-100 residue, `landings/PLAN-100.md` defect 3)**
   - What: after `3efd10dc`, a failed initial `getEpicResource` leaves `resource = null` (`EpicView.svelte` ~158-160): Next plan then stays disabled with "The epic is still being read" although the read failed, with no error and no retry. Show the error state of deliverable 3's pattern for the resource, with "Try again", and let the bar say the true thing.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test that fails before: a rejected resource read shows the error and a retry that reloads it, and Next plan does not claim the epic is still being read (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20580 PM_WEB_PORT=20581 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20580`, app port `20581` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20580"`), then `PM_MOCK_WEB_PORT=20580 ./pm-web-app/start.sh -p 20581` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20580/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/proposals.adoc#UX-P-10`, `doc/claude_review/use-cases/proposals.adoc#UC-P-06` — read at `doc/claude_review/ux/proposals.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: `doc/claude_review/ux/proposals.adoc:339` `[#UX-P-10]` and `use-cases/proposals.adoc` UC-P-06 exist.; re-grounded vs D1-D5 2026-11-10 (Amendment)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): PLAN-43 shipped (templates in src/lib/templates/, ObjectPage for the epic page); EpicView was changed by PLAN-90/a11y and must be added to the template guard list. PLAN-47 (web-actions-by-state) has not landed: ActionBar.svelte exists (PLAN-25) but lib/actions/byState.ts does not.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
- HYPOTHESIS: the head "Landungen" count (`resource.landings`) can differ from the landing rows the view lists — confirm/refute at `pm-web-app/src/lib/views/EpicView.svelte:117,746` (verify-at-outline)
- HYPOTHESIS: `FindingsReviewModal` does not select the first open item when it opens — confirm/refute at `pm-web-app/src/lib/components/FindingsReviewModal.svelte:48` § `selectedId` (verify-at-outline)

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/EpicView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/EpicView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/epicEvents.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/epicEvents.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/FindingsReviewModal.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/epic-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/epic-view.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/epic_view.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/epic_view.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/control.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/control.de.properties` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-47` (`web-actions-by-state`), `PLAN-03` (`mock-scenario-states`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 006, 007, 009). **Hold.
Gate: D1 and D2 merged** (documents `AGREED`) and the D2 navigation engine; the chat column waits for the D4 web chat
package. Queue status: `parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT
widened now (a parked row is not in the disjointness gate population). When restarted:

- The epic page becomes an object / list column of the column chain (003); the epic's path in the head is replaced by the
  top-bar breadcrumb with titles, not slugs.
- Head slots as for the plan page (006); one `ActionBar` with the primary, head links, no "Zurück" (007).
- The chat as a side column (009).
- `WorkControlPanel` dialogs of the epic belong to this plan (with `PLAN-55`).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-58-web-epic-page.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/shared/epic-view.adoc` (#layout, #reactions), `doc/design/web-app/navigation.adoc` (#levels, #column-types, #side-columns), `doc/design/web-app/components-tags.adoc#tag-places`, `doc/design/shared/work-actions.adoc#review`, `doc/design/shared/intent-and-outline.adoc#epic`. HEAD 1cc0e10e.

1. **The epic in the column chain.** `EpicView.svelte` is the epic's column: as the list column its plans grouped by workstream (a workstream is no level) with status and what each waits for (reason and a relative _since_), the id as a `Chip`; selecting a plan opens the plan column to its right. As the object column it keeps the full head (`KindMark`, title, state sentence, one `ActionBar` with the primary from `byState`, no _Back_) and the lines for sources, landings, findings, watches, tokens and events. No path in the head.
   - Done when: `epic-view.spec.js` on `running-work` → `control.epic`: waits show since, titles first, selecting a plan opens it to the right (evidence order: `test-first`); the browser check on `control.epic`, `control.review` (evidence order: `none`).
2. **Tabs and the Intent tab.** The tab set is decided first (G1); the epic's Intent (destination, done-when checks with evidence, non-goals, each plan's criterion) is the web intent and outline package's content; this plan places the tab.
3. **Counts and findings review.** Head counts and _Landungen_ come from the rows the view lists (`EpicView.svelte:117,746`); `FindingsReviewModal` opens with the first open item selected and its counts match the card.
   - Done when: unit tests: epic counts equal rows; the modal selects the first open item (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).
4. **A failed epic-resource read says so and can be retried** (PLAN-100 residue). Unchanged: after a failed initial `getEpicResource` (`EpicView.svelte:157-161`) show `ErrorState` with _Try again_ and let _Next plan_ stop claiming the epic is still being read (`control.epic.next_waiting`).
   - Done when: a test that fails before: a rejected resource read shows the error and a retry that reloads it (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).
5. **The epic's chat** (D4 web chat package): the `Chat` side column of the epic; the `WorkControlPanel` dialogs of the epic belong here with `PLAN-55`.

Removed or replaced: the tree-with-summary-on-the-right layout; the epic's path in the head; _Zurück_.

Ownership: the logic of `FindingsReviewModal` (first open item) is this plan's; its restyle and its chat belong to the views package and the D4 web chat package.

Implements: `doc/design/shared/epic-view.adoc` § Layout, What the Epic Does by Itself; `doc/design/web-app/navigation.adoc` § Levels as Columns; `doc/design/shared/work-actions.adoc` § Reviewing Findings or Watches.

Dependencies: D1 foundation (ObjectPage regions, Chip, StatusLabel, ReasonBadge, RowLink), D2 navigation engine, the design line on tab sets (G1), D4 web chat package (deliverable 5), the intent package (deliverable 2). Sequence after `PLAN-45`, which provides `ErrorState`/`Skeleton`. `PLAN-43`, `PLAN-47`, `PLAN-03` have shipped.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.
- **Tab set:** the tabs of this page are decided by the D6 design session (WS-06, staged 2026-11-10); this plan stays parked until that session's documents are `AGREED`, and the Intent tab's content belongs to the web intent & outline package (this plan only reserves the tab).

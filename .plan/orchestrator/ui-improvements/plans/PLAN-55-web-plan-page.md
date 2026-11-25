# PLAN-55: Web: The Plan Page on the Object Template

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Rework the plan page of UX-P-05: head with title, id chip, state sentence and acts, an attention strip when the state asks, tabs (Process, Intent, Changes — the set D6 decided, see `## D6 Merged 2026-11-11 (Amendment)`), tree rows without repeated kinds, and activity rows without raw tags (UX-S-07).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-07)`
- `doc/claude_review/ux/proposals.adoc#UX-P-05`
- `doc/claude_review/ux/ux-plan-page.svg`
- `doc/claude_review/ux/findings.adoc#UX-F-07`
- `doc/claude_review/ux/findings.adoc#UX-F-08`
- `doc/claude_review/ux/findings.adoc#UX-F-09`

## Deliverables

> **Binding scope since 2026-11-25:** `## Re-Grounding 2026-11-25 (before emit, at 459eca7d)` at the end of this spec. It supersedes every deliverable text, Done-when and Expected Surface note above it where they differ; the earlier sections stay as history.

1. **Head and attention strip**
   - What: Kind, path, title, id chip, state sentence ("Running · execute · for 2 h 10 min · task 3 of 5"), PR chip, the acts of `byState` (`PlanView` and the `WorkControlPanel` it hosts use `ActionBar` of `PLAN-47`: the completion-report control renders only for an ended plan, a waiting plan's primary act opens its decision); the strip shows "Waits for your decision … · Open the decision" and the off-track banner. The page sits on the `ObjectPage` template of `PLAN-43` (this plan moves it there: `PLAN-43` no longer does).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `plan-view.spec.js` on `control.ready` and `decisions.open` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready` and `decisions.open`: head, attention strip, tabs, tree rows, activity rows; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Tabs** (superseded by D6: the tabs are Process, Intent and Changes, there is no Activity, Tokens or Settings tab, and `PlanSettingsModal` stays a dialogue; the binding text is deliverable D6-1 of `## D6 Merged 2026-11-11 (Amendment)`)
   - What: Tabs Process (default), Intent, Changes as D6-1 states; `PlanSettingsModal` stays the dialogue _Plan settings…_.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: a tab switch keeps the other tab's selection; the dialogue opens from the ActionBar overflow (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready` and `decisions.open`: head, attention strip, tabs, tree rows, activity rows; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
3. **Activity rows**
   - What: activity rows: local time, kind icon, tool and target; empty `LINK` rows do not render. (The tree rows and the "Jump to now" button moved to `PLAN-62` D1, which owns the process tree.)
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `NodeNarrative.test.ts` and a `PlanView` test: no `TOOL_CALL`/`PHASE` text; no empty row (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `control.ready` and `decisions.open`: head, attention strip, tabs, tree rows, activity rows; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20550 PM_WEB_PORT=20551 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20550`, app port `20551` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Keep the tree on the left and the narrative on the right (operator finding in `doc/findings-web.txt`) while the object column is at least `720 px` wide; in a narrower column the narrative stands below the tree (D6, `plan-view-interaction.adoc#layout`).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20550"`), then `PM_MOCK_WEB_PORT=20550 ./pm-web-app/start.sh -p 20551` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20550/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-07)`, `doc/claude_review/ux/proposals.adoc#UX-P-05` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: n/a | evidence: doc/claude_review/ux/{plan,proposals,findings}.adoc and ux-plan-page.svg present at 459eca7d
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 added a line to the plan view's head in `doc/design/shared/plan-view.adoc` (`change so far   12 files · +340 −27 · 2 unplanned`, below the tokens line, `plan.change.size`, absent before the first capture) and a work list beside the plan in a wide window (`PLAN-83`). `PLAN-82` serves the size in the contract and the mock and this plan builds the line into the head (the former `PLAN-81`, superseded, had it): it renders only when the plan carries a size. Also `PLAN-39` (PR #274) wrapped `PlanView.svelte` at 390 px, and its `mobile.spec.js` asserts no horizontal scroll on the ten routes: the 390 px watch on `/plans/{id}` is to be confirmed here and retired.
  - verdict: corroborated | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: n/a | evidence: plan-view.adoc:23,170; mobile.spec.js:176 covers /plans/fix-relay at 390 px; the line is built (PlanView.svelte:1060-1077)
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): No stale surface: PlanView.svelte, NodeNarrative.svelte, PlanSettingsModal.svelte, plan-view.adoc and plan_view properties exist; plan.change.size already exists in plan_view.en.properties (line 48), so the head's size line catalogue key is present and not to be created again; PlanView.test.ts and plan-view.spec.js changed since the anchor.
  - verdict: corroborated | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: n/a | evidence: all named files exist at 459eca7d; plan.change.size key present
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: n/a | evidence: procedural verify-first clause
- HYPOTHESIS: `PlanView.svelte` does not render `plan.change.size` though the key exists — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte` § head and `pm-api/src/main/resources/messages/plan_view.en.properties:48` (verify-at-outline)
  - verdict: contradicted | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: yes | evidence: PlanView.svelte:1060-1077 renders change so far as a button to the Changes tab (PLAN-164); settled in Re-Grounding 2026-11-25: the code wins
- HYPOTHESIS: a `LINK` activity entry with no target renders as an empty row — confirm/refute at `pm-web-app/src/lib/views/PlanView.svelte` § `activity-entry` (verify-at-outline)
  - verdict: corroborated | checked_at: 459eca7d8344c6d7035d5a14a7f6e99022264170 | by: ui-improvements/next | rescoped: n/a | evidence: PlanView.svelte:1302-1358, link kind falls to the generic branch :1351-1354 with no empty filter; mock fully-onboarded.json:737; now D3

## Expected Surface


- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/NodeNarrative.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/NodeNarrative.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/PlanSettingsModal.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/WorkControlPanel.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/AttentionStrip.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/plan-view.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/control.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/plan-view-interaction.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/templates/pageWidth.test.ts` — present at `dc811fb3` (added by the cleanup of 2026-11-14)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD 7b60cec0 (cleanup 2026-11-22, declared surface understated)
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.svelte` — present at HEAD 459eca7d (re-grounding 2026-11-25)
- OBSERVED: `pm-web-app/src/lib/templates/ObjectPage.test.ts` — present at HEAD 459eca7d (re-grounding 2026-11-25)
- OBSERVED: `pm-web-app/src/lib/components/AttentionStrip.test.ts` — present at HEAD 459eca7d (re-grounding 2026-11-25)
- OBSERVED: `pm-web-app/src/lib/stores/runtime.test.ts` — present at HEAD 459eca7d (re-grounding 2026-11-25)
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — present at HEAD 459eca7d (re-grounding 2026-11-25)
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD 459eca7d (re-grounding 2026-11-25)

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-47` (`web-actions-by-state`), `PLAN-76` (`shared-port-assignment`, shipped), `PLAN-82` (the size in the contract and the mock)
- Overlaps with: none known at staging (2026-11-25: `pm-web-app/src/lib/stores/runtime.ts` with the running PLAN-182; one file, not blocking)
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 006, 007, 009). **Hold.
Gate: D1 and D2 merged** (documents `AGREED`) and the D2 navigation engine; the chat column waits for the D4 web chat
package. Queue status: `parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT
widened now (a parked row is not in the disjointness gate population). When restarted:

- The plan page becomes the **object column** with its head and tabs. **Deliverable 1's head path is dropped** (the top-bar
  breadcrumb is the only path) and so is its rule that a waiting plan's primary act opens the decision (it is a head link,
  007 §3) (003).
- Head slots: `KindMark` · title / `Chip` · `PrChip` · head links · `PhaseLabel` · `StatusLabel` (006).
- One `ActionBar` with the primary; "Prozess gestalten" and "Abschlussbericht" as head links; the plan's `decision` act is a
  head link that opens the decision in place; "Spring dorthin" in the phase-tree head; no "Zurück" (007).
- The chat as a side column (009).
- `PlanSettingsModal` belongs to this plan (fields per 008; `control.plan.settings` is "Plan-Einstellungen" after `PLAN-109`).
- Running `PLAN-106` rewrites `WorkControlPanel` act lists; line references into it in messages 007 and 009 are re-read when used.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- The re-grounding's "new claims" were never added to Claim Labels.

## Re-Grounding 2026-11-22 (cleanup, at `7b60cec0`)

Verdicts persisted per claim by `ui-improvements/cleanup` (`corpus set-verdict`, origin/main `7b60cec0`; sibling repos read at their local refs only). Contradicted claims are stamped `rescoped: yes` with the corrections below; read before the outline:

- Claim 4 contradicted (`rescoped: yes`): `PlanView.svelte:1020-1033` renders the "change so far" line as a link to the Changes tab (PLAN-164 #477, #481); D6-1 is partly done (a `Tabs` strip with Process and Changes; `tab=changes` before a capture falls back, `:326`). Not done: the Intent tab (`PlanView:333` sends any other tab to process), `ObjectPage`, the head slots, the attention strip, in-page Back and path removal, the 720 px layout and compact head, the `link` activity row filter (`:1267-1306`).
- PLAN-144 D3 removes `goBackToCaller` and the Back (`PlanView.svelte:592,858-863`); this plan's D1 drops the path: landing order matters. Parked behind PLAN-144 and PLAN-137.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-55-web-plan-page.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

## Re-grounding 2026-11-10 against the agreed D1–D5 designs (Amendment)

Source: `doc/design/web-app/navigation.adoc` (#column-types, #up, #lenses, #side-columns), `doc/design/web-app/components.adoc` (#object-page, #fixed-places, #labels, #attention-strip), `doc/design/web-app/components-tags.adoc` (#status-label, #phase-label, #pr-chip, #kind-mark), `doc/design/shared/plan-view.adoc` (#levels, #change-size, #tokens, #chat-revision, #ended). HEAD 1cc0e10e.

1. **The plan as the object column.** `PlanView.svelte` is the object column of the plan: the `ObjectPage` regions, the head with `KindMark`, title, id `Chip`, `PrChip`, `PhaseLabel` and `StatusLabel`, the state sentence ("Running · execute · for 2 h 10 min · task 3 of 5"), the lines `tokens so far` and `change so far` (`plan.change.size`, absent before the first capture), one `ActionBar` with the primary from `byState`, the head links _Process designer_ and _Completion report_ (ended plan only), and the plan's `decision` act as a head link that opens the decision in place. No path and no _Back_ in the head. The attention strip (`AttentionStrip`) says "Waits for your decision …" and carries the off-track banner. Queued revision line: `control.chat.revision_queued`.
   - Done when: `plan-view.spec.js` on `running-work` → `control.ready` and `decisions.open` (evidence order: `test-first`); the browser check of head, strip and tabs (evidence order: `none`).
2. **Tabs.** Decided by D6 (merged, #403, `8568b56e`): Process (default), Intent, Changes — the full rule is D6-1 of `## D6 Merged 2026-11-11 (Amendment)`. The Intent tab content belongs to the web intent and outline package (`PLAN-145`); this plan only places it. The tab is `tab={id}` in the address (a replace). **Plan settings stay a dialogue** (_Plan settings…_, `control.plan.settings`): `PlanSettingsModal` stays, rebuilt on the field system and the settings-form rule (one _Save_ disabled until a change, a _changed_ mark, _Use the inherited value_).
   - Done when: unit tests: a tab switch keeps its state; the dialogue opens from the ActionBar overflow (evidence order: `test-first`).
3. **Activity rows.** Local time, kind icon, tool and target; a `LINK` row without a target does not render. (The tree rows and "Jump to now" belong to `PLAN-62`.) The activity of a worker is the task's own level (`/plans/{id}/tasks/{task}`, `shared/plan-view.adoc#tabs` "Not a tab"), never a tab of the plan: these rows stand in the task's object column.
   - Done when: `NodeNarrative.test.ts` and a `PlanView` test: no `TOOL_CALL`/`PHASE` text, no empty row (evidence order: `test-first`); the open-issue read at the head (evidence order: `none`).
4. **The plan's chat as a side column** (D4 web chat package): `chat=open`, the `Chat` beside the object column; `WorkControlPanel` dialogs of the plan belong to this plan.

Removed or replaced: the head path, "the waiting plan's primary act opens its decision" (a head link now), `PlanSettingsModal` becoming a tab ("the modal is gone"), the phase-tree rows (PLAN-62).

Implements: `doc/design/web-app/navigation.adoc` § Levels as Columns; `doc/design/shared/plan-view.adoc` § Levels, Change So Far, Tokens So Far, A Revision from the Chat; `doc/design/web-app/components.adoc` § Fixed Places, Labels.

Dependencies: D1 foundation (ObjectPage regions, PhaseLabel, PrChip, KindMark, Field system), D2 navigation engine (column chain, `open=` in place, `tab=`), the design line on the tab set, D4 web chat package (deliverable 4), the web intent and outline package (the Intent tab content). Sequence after `PLAN-45` (`PlanView.svelte`). `PLAN-47`, `PLAN-82`, `PLAN-106` have shipped. `PLAN-62` runs after this plan.

New claim bullets:

New claims of this re-grounding are in `## Claim Labels`.

## Operator Decisions 2026-11-10 (Amendment)

Recorded in the epic's decision log (AskUserQuestion, 2026-11-10). They settle the open points above and supersede any text here that differs.

- **Chat adoption:** `PLAN-137` (web chat places) replaces every `ChatPanel` call site and the project-onboarding chat drawer and deletes `ChatPanel`. This plan drops its own chat deliverable or chat lines; it only places the `Chat` side column in its redesigned layout where the design shows it.
- **Tab set:** the tabs of this page are decided by the D6 design session (WS-06, staged 2026-11-10); this plan stays parked until that session's documents are `AGREED`, and the Intent tab's content belongs to the web intent & outline package (this plan only reserves the tab). *Settled 2026-11-11:* D6 merged (#403, `8568b56e`), the documents are `AGREED`; the decided set is in the amendment below.

## D6 Merged 2026-11-11 (Amendment)

Source: the D6 design session `PLAN-141`, merged as #403 (`8568b56e`, 2026-11-10); answers Q1 and Q3 of
`landings/PLAN-141-findings.md` § Answers. Documents read on `main` at `4a84f727`: `doc/design/shared/plan-view.adoc` (#tabs
:49-58, #change-size :167-170, #changes :172-191, #tokens, #ended :232-235), `doc/design/web-app/plan-view-interaction.adoc`
(#layout :14-40, #selection, #intent :112-115, #changes :117-120, #addresses :122-129, #decisions :209-212),
`doc/design/web-app/navigation.adoc` (#url :178-222, #rely :383-418). These supersede any text above that differs, including the
original deliverable 2 and the candidate tab list of the re-grounding.

D6-1. **The tab set and the two forms of the plan's column**
 - What:
   - As the **object column** (no task open): the head, the lines _tokens so far_ and _change so far_, the attention strip, and the tabs
     **Process** (`process`, the default), **Intent** (`intent`) and **Changes** (`changes`). There is **no** Activity, Tokens or Settings tab:
     the tokens by phase and role stay the one-step disclosure of the head's _tokens so far_ line (`plan-view.adoc#tokens`), _Plan
     settings…_ stays a dialogue, and a worker's activity is the task's level.
   - **Process** holds the tree and the narrative of the selected node inside the tab: side by side while the object column is at least
     `720 px` wide (the tree `280`–`360 px`, the narrative the rest), the narrative below the tree in a narrower column; each keeps its own
     scroll (`plan-view-interaction.adoc#layout` :37). The tree's rows and the narrative's content are `PLAN-62`'s; this plan builds the
     tab's layout.
   - **Intent** hosts `PlanIntentPanel`, `BuilderCheckList` and `OutlinePanel` of `PLAN-145` in that order; before refine submitted a
     record it says `intent.plan.none` and shows no empty section (`plan-view-interaction.adoc#intent`).
   - **Changes** is absent before the first capture, as the size line is; the size line _change so far_ is a link to it. Its address is
     `/plans/{id}?tab=changes&file=<path>`, which replaces the former `changes=open`. An address naming `tab=changes` before the first
     capture opens the first tab and is replaced. The list and the hunks follow the Process rule (side by side from `720 px`). The tab's
     content (the file list, a file's hunks) is the repository work package `plan-changes` (`doc/plans/plan-changes.adoc`); this plan
     places the tab, its presence rule and the size line's link.
   - As a **list column** (a task of the plan open as the next level, `/plans/{id}/tasks/{task}`): the compact head (`KindMark`, title,
     state word, the main act as a secondary `Button`; `ObjectPage` `compact`) and the phase tree as its list; nothing of the other tabs.
     Selecting the plan's head closes the task and the plan is the object column again with all its tabs (`plan-view-interaction.adoc#layout` :20).
   - An **ended plan** has the same tabs, read-only (`plan-view.adoc#ended`).
   - **Addresses**: `tab={id}`, written by a replace; an address without it opens Process. `node={path}` is written only with Process
     (`PLAN-62` writes it) and `file={path}` only with Changes; switching the tab replaces the address and drops the other tab's key, and
     the view keeps the other tab's selection in memory while the plan stays open, so going back shows it again
     (`plan-view-interaction.adoc#addresses`). A task's address carries no tab of the plan.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
 - Done when:
   - `PlanView.test.ts`: the three tabs and no fourth; Changes absent before the first capture and present after; the size line links to
     Changes; `tab=changes` before a capture is replaced by the first tab; a switch Process → Changes → Process drops `node=` then `file=`
     from the address and restores the earlier node from memory; Process side by side at an object column of `720 px` and stacked at
     `719 px`; the compact head and the tree as the list column while a task is open; an ended plan read-only with the same tabs. It fails
     on a revert (evidence order: `test-first`).
   - `plan-view.spec.js` on `running-work` → `control.ready`: the tabs, the size line's link, the task opening with the plan as list column
     (evidence order: `test-first`).
   - Browser check (Playwright MCP) of the three tabs and the list-column form at `1440 × 900` and `390 × 844`, `de`/`en`, dark/light
     (evidence order: `none`).

Removed or replaced by D6: the candidate tabs Activity and Tokens; any Settings tab; the address `changes=open`; the size line opening
the changes "in the narrative panel".

Dependencies added: `PLAN-142`/`PLAN-144` (the plan on the column engine, `ObjectPage` `compact`), `PLAN-143` (`tab=`, `file=` and the
one address writer), `PLAN-145` (the Intent tab's panels), `plan-changes` (the Changes tab's content). Verify-first: if `plan-changes` has not landed
at the then-current HEAD, stop and loop back to the orchestrator for the order; the spec does not invent an interim tab body.

Expected Surface (when restarted; the parked rule above still holds until then): unchanged files; the tab's layout lives in
`PlanView.svelte` and `PlanView.test.ts`, already listed. No new file.

## Parked Gate Check 2026-11-14 (cleanup)

**Stays parked (operator, 2026-11-14: "Stage plan-changes rows").** The Changes tab needs the plan-changes contract, which is now staged as `PLAN-162` (contract and mock), `PLAN-163` (terminal) and `PLAN-164` (the web tab's content, mounted in today's `PlanView`).
- Unpark when `PLAN-164`, `PLAN-144` and `PLAN-145` have landed. This plan then carries the Changes tab into its tab set and does not build it.
- Re-ground at outline: the "new claims" this spec says were added were never added.

## Re-Grounding 2026-11-25 (before emit, at 459eca7d)

Unparked by the operator 2026-11-25 ("Unpark PLAN-55"): its gate (PLAN-164, 144, 145 landed) is met. Re-grounded by a read-only
sub-agent at `origin/main` `459eca7d`, the main claims re-read by the orchestrator. **This section is the binding scope**; where
the text above differs, this wins. `PlanView.svelte` is abbreviated PV; line numbers are at `459eca7d`.

**What is already on `main` (do not build again):** the tokens line and its disclosure (PV:972-1059); the _change so far_ line
as a link to the Changes tab (PV:1060-1077, PLAN-164); `Tabs` with Process and Changes (PV:141-162, 1080-1099), Changes absent
before the first capture, `tab=` written by replace (`runtime.ts:2126-2132`), `tab=changes` before a capture replaced by Process
(PV:370-382), `file=` (`selectPlanChangeFile`, `runtime.ts:2143`); the `ActionBar` through `WorkControlPanel` (PV:963-970); the
top-bar breadcrumb, `UpLink` (PV:808-812) and `WorkListColumn` (PV:800-806) of PLAN-144/185; no _Back_; the activity tag is a
catalogue word, not a raw `TOOL_CALL` (PV:1312-1314).

**Out of this plan:**
- The chat side column (old D4): `PLAN-137` owns the chat adoption (Operator Decisions 2026-11-10). Struck.
- "Jump to now", the tree rows, `node=` and the narrative content: `PLAN-62`.
- **The task as the next level** (D6-1's list-column form: `/plans/{id}/tasks/{task}`, the compact head, the phase tree as the
  list, selecting a task pushes the address, the task's own activity column, and the activity rows moving there): split off as
  **`PLAN-190` (`web-plan-task-level`, staged)**. This plan leaves `selectTask` (PV:758-760) as it is.

### Deliverables (binding)

1. **The plan's head on `ObjectPage`**
   - What: PV renders as the object column on `ObjectPage` (`templates/ObjectPage.svelte`; `EpicView.svelte:724-980` is the
     working model of the slots kind / title / id / state / primary / tabs / attention). Head: `KindMark`, title, id `Chip`, `PrChip`
     (replaces the plain link PV:886-896), `PhaseLabel` and `StatusLabel` (replace the `.badge` spans PV:912-918). The state
     sentence "{state} · {phase} · for {duration}" from what the plan read carries; the "task n of m" part only if the process tree
     gives it without a contract change (its leaf tasks), otherwise left out and named in the landing message. The in-page path
     `<nav class="plan-breadcrumb">` (PV:841-867) is removed with its assertions (`PlanView.test.ts:193,595`): the top bar's
     breadcrumb is the only path. Head links: _Process designer_ (today a `Button`, PV:870-877) and _Completion report_ (today
     always shown, PV:897-904; an ended plan only). The attention strip (`AttentionStrip`, today only on `/dev/components`) says
     "Waits for your decision …" when the plan waits, with the plan's `decision` act as a head link that opens the decision in place
     (`WorkControlPanel.svelte:136-155` excludes `decision` from the act bar: keep that), and carries the plan-level off-track
     banner (the task-level one at PV:1283-1295 stays). The queued-revision line `control.chat.revision_queued`
     (`control.en.properties:300`, no reader today) shows when the plan reports a queued revision. One `ActionBar`, as today.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `PlanView.test.ts`: the head slots (`KindMark`, id `Chip`, `PrChip`, `PhaseLabel`, `StatusLabel`); no `plan-breadcrumb`;
       _Completion report_ only on an ended plan; the attention strip and the decision head link on a waiting plan and neither
       on a running one; the off-track banner on an off-track plan; the revision line when queued. It fails on a revert
       (evidence order: `test-first`).
     - `plan-view.spec.js` (scenario `plan-view`, `plan-view.spec.js:7-9`) plus `running-work` → `decisions.open` for the waiting
       plan: head, no in-page path, the decision link opens the decision (evidence order: `test-first`).
2. **The tab set completed: Intent, the 720 px layout, the address, plan settings**
   - What: add the **Intent** tab (`intent`, catalogue key `plan.tab.intent` en/de) hosting `PlanIntentPanel`, `BuilderCheckList`
     and `OutlinePanel` (PLAN-145; today mounted only on `/dev/components`, `+page.svelte:43-45`) in that order; before refine
     submitted a record it says `intent.plan.none` and shows no empty section. PV today sends any tab but Process and Changes to
     Process (PV:127-129, 378-381): Intent becomes a valid tab. **Process** and **Changes** lay out side by side while the object
     column is at least `720 px` wide (tree or file list `280`–`360 px`, the rest the narrative or hunks), stacked below
     (`plan-view-interaction.adoc#layout` :37); each keeps its own scroll. Today only a `768 px` media query exists (PV:2024): use
     the column's width, not the viewport. Switching the tab drops the other tab's address key (`file=` leaving Changes) by a
     replace; the selection of the other tab is kept in memory while the plan stays open. Rebuild **`PlanSettingsModal`** on the
     field system and the settings-form rule: one _Save_ disabled until a change, a _changed_ mark per field, _Use the inherited
     value_ (today a per-row `<select>` and Save, `PlanSettingsModal.svelte:114-150`; it opens from the act bar's overflow,
     `WorkControlPanel.test.ts:106`, keep that). Do not edit the three intent panels' own files (PLAN-188 touches `OutlinePanel`
     on its branch); a defect found in them is reported.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `PlanView.test.ts`: exactly the three tabs; `tab=intent` opens Intent with the three panels in order, and
       `intent.plan.none` without a record; side by side at a `720 px` column and stacked at `719 px`, for Process and Changes;
       Changes → Process drops `file=` and going back restores the file from memory. `PlanSettingsModal.test.ts`: Save disabled until
       a change, the _changed_ mark, _Use the inherited value_. Each fails on a revert (evidence order: `test-first`).
     - `intent.spec.js` opens the Intent tab of a plan from its address (evidence order: `test-first`).
3. **Activity rows without empty links**
   - What: an activity entry of kind `link` without a target renders no row (today the generic branch prints it, PV:1351-1354;
     the mock carries such entries, `fully-onboarded.json:737`); the kind is shown as the kind's icon with its catalogue word as
     the accessible name, not as a text tag. The rows stay where they are (their move to the task level is PLAN-190).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `PlanView.test.ts`: a `link` entry without a target yields no row, one with a target does; the kind icon carries the
       catalogue word as its name. It fails on a revert (evidence order: `test-first`).
4. **The browser check and the design documents**
   - What: the Playwright MCP browser check of the head (running, waiting, ended plan) and the three tabs at `1440 × 900` and
     `390 × 844`, `de`/`en`, dark/light, after the last visual commit; `doc/design/shared/plan-view.adoc` and
     `doc/design/web-app/plan-view-interaction.adoc` say what was built, and name the task level as PLAN-190's (UX-S-13, VD-S-09).
     `mobile.spec.js` keeps `/plans/{id}` free of horizontal scroll at 390 px; the 390 px watch is then retired (report it).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The browser check's snapshots and screenshots are kept as the Standing Constraints say (evidence order: `none`).
     - The two documents match the code; `mobile.spec.js` passes at 390 px (evidence order: `none`).

**Main is red until PLAN-188 lands** (`IntentCatalogueTest`, #570; Open Defect 2026-11-25): the merge gate waits for a green
`main`; never fix `intent.*.properties` or `IntentCatalogueTest` here. If PLAN-188 has not landed by the merge, report it and stop.

Claims re-stamped at `459eca7d` (`corpus set-verdict`, by `ui-improvements/next`): 0-2 corroborated, 3 unverifiable (procedural),
4 contradicted and settled (the code wins: the line is built), 5 corroborated (PV:1302-1358).

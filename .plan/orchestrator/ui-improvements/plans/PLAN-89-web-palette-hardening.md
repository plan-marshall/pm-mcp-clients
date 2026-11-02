# PLAN-89: Web: Command Palette Hardening, Tests and Audit

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.
> Staged 2026-11-01 from the open findings of the landing analyses. One pull request on purpose: each pull request costs a
> CodeRabbit review (quota-limited), so the open aspects of one area are collected here. Keep the pull request under 100 files
> (CodeRabbit refuses more); if it would exceed that, split it by deliverable and say so in the progress record.

## Objective

Close what the three-layer analysis of PR 296 and PR 300 found open in the command palette: an invalid listbox in the empty and failed states, a constant `aria-expanded`, a selection that is never scrolled into view, tests that do not assert the harm they are named for, and a design document PR 300 did not update. Put the palette under a unit test it never had and under the axe audit of `PLAN-37`, so its markup gets one independent check. One pull request, because each pull request costs a CodeRabbit review.

## Deliverables

1. **The listbox owns only options**
   - What: In `CommandPalette.svelte` the results container (`role="listbox"`) holds nothing but options and groups of options. The "no match" paragraph and the failed-read `Retry` button move out of it into a sibling status region (the `Retry` stays a design-system `Button` and a Tab stop). A group is named once (`aria-labelledby` on its heading, no repeated `aria-label`). `aria-expanded` is `true` only while the listbox holds at least one option. An option's id comes from its row key through one map, not a `findIndex` per row, and `aria-activedescendant` never names an id that is not in the document. The selected option is scrolled into view when the selection moves. The two comments the move of the rows made stale ("a row, Retry keeps its own Enter"; "the field, the rows, and Retry") are corrected, and the Tab walk is what the code does: Close, field, Retry.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A new `CommandPalette.test.ts` (Vitest and Testing Library) fails on the palette of `1ce32d80` and passes after, in four states: empty field, results, no match, failed read. It asserts that every child of the listbox is an option or a group of options, that `Retry` and the empty text are outside it, that a group has one accessible name, that `aria-expanded` follows the rows, that `aria-activedescendant` names an element that exists after the answer changes, and that `scrollIntoView` is called on `ArrowDown` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `Ctrl+K`, the empty field, "relay", a text without a match, and a failed read (stop the mock for the read, never the app); the accessibility snapshot of each state; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Tests that prove the findings of PR 296**
   - What: `DecisionView.test.ts` `clears the local filter when it hides the requested decision` also asserts that `dec-101` is the selected decision and that the detail pane is not empty (today it asserts only that the filter input is empty and another decision is visible again). `scrolls to a decision whose card mounts after the request` restores `Element.prototype.scrollIntoView` after the test (an `afterEach`), so no later test of the file runs with the stub. `chrome.spec.js` gains the palette cases it lacks: Tab walks Close, field and back (the real walk), a text without a match shows the empty text outside the listbox, and a failed read shows `Retry` outside it (the read is made to fail with `page.route`, not with a timer).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - The strengthened filter test fails when the request effect clears the filter but does not select the decision (revert that line to see it red), and passes on the code (evidence order: `test-first`)
     - The three new `chrome.spec.js` cases pass; the failed-read case and the Tab case fail against `1ce32d80` where they cover deliverable 1 (evidence order: `test-first`)
3. **Design and texts follow the code**
   - What: `doc/design/web-app/needs-and-find.adoc` states what PR 300 changed and the panel did not say: the visible close control at every width (not only `Esc`), the combobox and listbox model with an active descendant, `Enter` opening the first match with the acts after the matches, the Tab walk, and where the empty and failed texts sit. `overview.find.plain`, cited in `doc/design/shared/needs-and-find.adoc` (line 77) with no Texts row and no catalogue key, is settled: either it gets its Texts row and catalogue entries in `overview.{en,de}.properties`, or the citation is removed, whichever the design's own rule for cited keys says (`doc/design/shared/texts-and-languages.adoc`); the choice and its reason are recorded in the progress record.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - Every behaviour deliverable 1 and 2 test pins is stated in the design document, and no key the design cites lacks a Texts row or a catalogue entry (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)
4. **The axe audit opens the palette**
   - What: The axe-core audit of `PLAN-37` (`pm-web-e2e/tests/helpers/a11y.js`, wired in `theme-readability.spec.js`) also audits the palette open in its four states, at 1440 and 390 px, in `de` and `en`. A serious or critical issue fails the run. This is the independent check of the palette's markup that PR 296 and PR 300 never had.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:lint-config
   - Done when:
     - The audit of the palette in the no-match and failed-read states reports the invalid listbox children on the palette of `1ce32d80` and none after deliverable 1 (evidence order: `test-first`)
     - `PM_MOCK_WEB_PORT=20890 PM_WEB_PORT=20891 ./mvnw -pl pm-web-e2e verify -Pe2e` shows no failure beyond the names recorded at the base (evidence order: `none`)

5. **Two leads of the PLAN-37 landing, verified first**
   - What: Both are read from the code at `c5bfb1ca` and not reproduced in a browser; confirm each in the browser before changing anything, and report a refuted lead instead of working around it. (a) `IconButton.svelte` now draws its `label` beside the glyph from 768 px up (`.icon-btn__label`, `white-space: nowrap`, `inline-size: auto`), and its `label` is also its accessible name. `DataTable.svelte` names each row menu `${menuLabel}: ${first cell}` and holds the menu in a column of `calc(44px + var(--space-2) * 2)` (60 px) under `table-layout: fixed`, so at 768 px and above each row would draw "Row menu: <plan name>" beside the glyph and overflow into the neighbouring column. If the browser shows it, give the control a short visible label apart from the longer accessible name (a second prop, `visibleLabel`, defaulting to `label`), keep the row's name in `aria-label`, and check `ActionBar`'s overflow `IconButton` the same way. (b) `ChatPanel.svelte` yields its Escape whenever `document.querySelector('[role="dialog"]')` finds any element (`closeOnEscape`, :74); `CommandPalette.svelte`, `SystemView.svelte`, `LanListenerModal.svelte`, `ProjectOnboardingView.svelte` and others carry `role="dialog"`. If one of them stays mounted while hidden, Escape never closes the chat there; if none does, say so and leave the code. (c) The opener the chat and the control dialog remember is `document.activeElement` at the moment the panel opens; on Safari and on Firefox for macOS a mouse click does not focus a button, so the focus would not return. Check it in the browsers the project supports, or record that it was not checked.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - For each of (a), (b) and (c) the progress record states confirmed or refuted with the browser evidence; for a confirmed lead a unit test fails on `c5bfb1ca` and passes after the change (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: `/dev/components` at 1440 and 390 px (the data table's row menus and the `ActionBar` overflow in `de` and `en`) and, for (b), the chat opened from a plan's control with the palette closed and then open; baseline and after snapshots plus screenshots recorded (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20890`, app port `20891` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20890"`), then `PM_MOCK_WEB_PORT=20890 ./pm-web-app/start.sh -p 20891` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20890/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: at `1ce32d80` the results container is `role="listbox"` (`CommandPalette.svelte:401`) and still holds the empty-state paragraph (:468) and the failed-read `Retry` `Button` (:478) — read at `pm-web-app/src/lib/shell/CommandPalette.svelte`
- OBSERVED: at `1ce32d80` `aria-expanded="true"` is constant (:379), `rowOptionId` is a `findIndex` per row (:134-143), the group carries `role="group"` and an `aria-label` equal to its `h3` (:405), and `scrollIntoView` appears nowhere in the file — read at `pm-web-app/src/lib/shell/CommandPalette.svelte`
- OBSERVED: no `CommandPalette.test.ts` exists at `1ce32d80`; the palette is tested by `chrome.spec.js` only — read at `pm-web-app/src/lib/shell/`
- OBSERVED: `DecisionView.test.ts:759` asserts the empty filter input and the other decision's visibility only, and :782 assigns `Element.prototype.scrollIntoView` with no restore (no `afterEach`, no `restoreAllMocks` in the file) — read at `pm-web-app/src/lib/views/DecisionView.test.ts`
- OBSERVED: PR 300 changed no file under `doc/`; `doc/design/web-app/needs-and-find.adoc:47` names `Esc` as the way to close — read at `git show 1ce32d80 --stat` and the design file
- OBSERVED: `overview.find.plain` is cited at `doc/design/shared/needs-and-find.adoc:77` and appears in no catalogue file — read by `git grep` at `1ce32d80`
- HYPOTHESIS: the mock cannot make the find read fail by a scenario, so the failed-read case needs `page.route` on `/api/v1/find` — confirm/refute at `pm-mock-runtime` scenarios and `pm-web-e2e/tests/helpers/` (verify-at-outline)
- OBSERVED: `PLAN-37` has landed (`c5bfb1ca`, PR 297): `pm-web-e2e/tests/helpers/a11y.js` exports `auditViewA11y(page)` (axe-core, WCAG 2.0/2.1 A and AA, `serious` and `critical` fail, nothing disabled) and `formatViolation(view, violation)`; `theme-readability.spec.js` wires it as the describe block "Accessibility audit (axe-core)" over `GUARD_JOURNEYS` in `de` at 1440 × 900 — read at `git show c5bfb1ca`. The helper takes a page, so it can be called on an open panel; deliverable 4 adds the `en` run and the 390 px width the audit lacks
- OBSERVED: at `c5bfb1ca` `IconButton.svelte` draws `<span class="icon-btn__label">{label}</span>` and shows it from 768 px (`:115-125`); `DataTable.svelte` (PLAN-35) passes `label={menuNameOf(row)}` and gives the menu column 60 px — read at those files; the combination is not run (deliverable 5)
- OBSERVED: at `c5bfb1ca` `ChatPanel.svelte:74` returns from its Escape handler when `document.querySelector('[role="dialog"]') !== null`, and `role="dialog"` appears in ten components — read by `grep` at `pm-web-app/src`; whether one stays mounted while hidden is not read (deliverable 5)
- Source: the three-layer analysis of PR 300 in `landings/PLAN-46.md` (findings C1 to C5, T1 to T3, the design gap) and the open point of `landings/PLAN-37` (the audit runs `de` at 1440 × 900 only).
- Not in this plan, the operator's call: a labelled clear action for the field (the browser's own clear stays; the design names no key).
- Verify-first clause: before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.svelte` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-app/src/lib/shell/CommandPalette.test.ts` — absent at HEAD 1ce32d80; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD 1ce32d80
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — the audit is wired here by `PLAN-37` (present at HEAD c5bfb1ca)
- OBSERVED: `pm-web-e2e/tests/helpers/a11y.js` — present at HEAD c5bfb1ca (created by PLAN-37)
- OBSERVED: `pm-web-app/src/lib/components/IconButton.svelte` and `IconButton.test.ts` — present at HEAD c5bfb1ca (deliverable 5)
- OBSERVED: `pm-web-app/src/lib/components/DataTable.svelte` and `DataTable.test.ts` — present at HEAD c5bfb1ca (PLAN-35; deliverable 5)
- OBSERVED: `pm-web-app/src/lib/components/ChatPanel.svelte` and `ChatPanel.test.ts` — present at HEAD c5bfb1ca (deliverable 5)
- OBSERVED: `doc/design/web-app/needs-and-find.adoc` — present at HEAD 1ce32d80
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 1ce32d80
- HYPOTHESIS: `pm-api/src/main/resources/messages/overview.en.properties` — only if `overview.find.plain` gets its entries (verify-at-outline)
- HYPOTHESIS: `pm-api/src/main/resources/messages/overview.de.properties` — the same condition (verify-at-outline)

## Dependencies and Sequencing

- Depends on: `PLAN-46` (shipped), `PLAN-37` (shipped, `c5bfb1ca`: the audit helper deliverable 4 uses)
- Overlaps with: `PLAN-35` (shipped; `DataTable.svelte` of deliverable 5), `PLAN-90` and `PLAN-43` (running; both touch web e2e and `SystemView.svelte`, this plan does not), `PLAN-51` and `PLAN-57` (name `DecisionView.svelte`), `PLAN-45` and `PLAN-85` (name `chrome.spec.js`)
- Adjacent to: `PLAN-84` (`cli-find` reads `overview.find.*`; this plan adds none unless `overview.find.plain` gets entries)

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-89-web-palette-hardening.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

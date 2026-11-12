# PLAN-70: Web: Light Theme as a Designed Theme

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Review every view in the light theme and tune surfaces and shadows so light is a designed theme, not an inversion (VD-S-07).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-07`
- `doc/claude_review/visual-design/findings.adoc#VD-F-10`

## Deliverables

1. **Light pass**
   - What: Tune `--color-bg-*`, borders and `--shadow-*` for light on every view; record tuned values.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `theme-readability.spec.js` passes in light for every declared view (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in light; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **Design record**
   - What: `theme-readability.adoc` states the light theme as designed, with the tuned values.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The document passes `doc-review` (under the runbook lane its checker `trace.py` does not run, so `pm-documents:ref-documentation` `docs review` and `pm-documents:ref-asciidoc` `validate` and `verify-links` stand in for it, clean, as PLAN-06 and PLAN-07 ran them) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20700 PM_WEB_PORT=20701 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20700`, app port `20701` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20700"`), then `PM_MOCK_WEB_PORT=20700 ./pm-web-app/start.sh -p 20701` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20700/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-07`, `doc/claude_review/visual-design/findings.adoc#VD-F-10` — read at `doc/claude_review/visual-design/plan.adoc`
  - verdict: corroborated | checked_at: 1cc0e10ec2ca617853b99a23dcfed8e5e2fed1bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: visual-design/plan.adoc:58 VD-S-07; findings.adoc:100 VD-F-10; tokens.css and theme-readability.spec.js present; scope widened to D1/D3/D4 tokens in the 2026-11-10 Amendment
- Re-grounded note (2026-10-30, operator request): `PLAN-85` (`web-theme-in-browser-settings`) moves the theme control from the rail foot and the More sheet to the pairing page and a new Settings view "This browser"; the storage stays `localStorage` `pm_theme`. The contrast audit's theme switch (`helpers/contrast.js` `setTheme`, `theme-readability.spec.js`) keeps working if it sets the storage; a check of this plan that "chooses" a theme through the rail control uses the new places or sets the storage.
- Re-grounded note at HEAD bf8ddb31 (2026-10-31): `web-standards-conformance` (PLAN-80, shipped, PR #281) changed `helpers/contrast.js` (a bounded quiet-frame wait) and `theme-readability.spec.js`, and its run reports that the contrast matrix calls `setTheme` before `page.goto` while `app.html` re-derives `data-theme` from a `localStorage.pm_theme` nothing writes, so the "dark" half of the audit may audit the light theme. Before this plan's light pass relies on the audit, it asserts that `document.documentElement.dataset.theme` equals the theme the test requested, in both themes, and reports any audit that ran under the wrong one. A light-theme verdict from a matrix that cannot show dark is not a verdict.
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): The wrong-theme concern is partly still live: helpers/contrast.js setTheme sets data-theme on documentElement after navigation and waits for transitions, but theme-readability.spec.js has no assertion that dataset.theme equals the requested theme, so the spec's planned assertion is still due; PLAN-37 axe-core (c5bfb1ca) also changed these files.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
- OBSERVED: tokens.css:5 names both [data-theme='light'] and [data-theme='dark'] in its header comment and :9 opens :root, so Button.test.ts:167-173 blockOf resolves both themes to :root — read at `pm-web-app/src/styles/tokens.css` :5-9 and `pm-web-app/src/lib/components/Button.test.ts` § `blockOf` at 1cc0e10e
- HYPOTHESIS: RowMenu.test.ts ~285-310 asserts hex values read by a regex that is true by construction and never reads bg-subtle or the prefers-color-scheme block — confirm/refute at `pm-web-app/src/lib/components/RowMenu.test.ts` (verify-at-outline)
- HYPOTHESIS: some pair of Button's 4.5:1 table fails in the real dark palette — confirm/refute by running the corrected test (verify-at-outline)

## Expected Surface


- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)
- OBSERVED: `pm-web-app/src/lib/components/RowMenu.test.ts` — present at `1cc0e10e` (added by the residue fold of 2026-11-10)

## Dependencies and Sequencing

- Depends on: `PLAN-52` (`web-running-work`), `PLAN-53` (`web-start-wizard`), `PLAN-58` (`web-epic-page`), `PLAN-59` (`web-projects-and-onboarding`), `PLAN-54` (`web-reports-and-designer`, reports only), `PLAN-88` (`web-designer-panes`), `PLAN-60` (`web-settings-one-place`), `PLAN-66` (`web-onboarding-frame-and-roles`, with the former `PLAN-68` and `PLAN-69`), `PLAN-62` (`web-plan-phase-workflows`), `PLAN-51` (`web-decisions-triage`, with the former `PLAN-57`), `PLAN-49` (`web-attention-inbox`), `PLAN-46` (`web-command-palette`), `PLAN-83` (`web-moving-between-plans`), `PLAN-85` (`web-theme-in-browser-settings`), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 001–009). **Gate: D1–D4 merged
and their implementation packages landed** (the pass checks the final visuals). Queue status: `parked`; the orchestrator
re-grounds this spec when it is restarted. The Expected Surface above is NOT widened now (a parked row is not in the
disjointness gate population). When restarted it checks **every visual of messages 001–009 in both themes**: kind tints, state
tones, reason colours, field look, the mascot (pairing page, empty states, rail foot — the rail-foot opacity against the text
above it), the chat (including the diff tokens), the new cog icon (`PLAN-109`, in its rail evidence) and the Running-work
heading without the start-page button (`PLAN-110`). The kind tints exist in both themes and fall under the token lint. **The
route baselines are re-taken after the views move** (003).

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-70-web-light-theme-pass.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/helpers/contrast.js` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at HEAD 1cc0e10e

## Residue Fold 2026-11-10 (Amendment)

Source: `landings/PLAN-107.md` (L3-1, layer 2 D3d), re-read at `1cc0e10e`. This section supersedes the spec text where they differ.

4. **The contrast unit tests read the theme they name**
 - What:
   - `Button.test.ts:167-173` `blockOf` finds `[data-theme='light']` / `[data-theme='dark']` in the HEADER COMMENT of
     `tokens.css:5`, and takes the next `{` (`:9`, `:root {`). Both "themes" resolve to the light `:root` palette, so the 4.5:1
     test "in both themes" never evaluates dark. Locate each block by its rule (selector at the start of a line followed by
     `{`; the `@media (prefers-color-scheme: dark)` block as well), and assert before using them that the two palettes differ.
     If a pair then fails in dark, report it and retune the token under deliverable 1.
   - `RowMenu.test.ts` (~285-310) pins four hex values by regex that is true by construction and does not read `bg-subtle`
     or the `prefers-color-scheme` block: read the tokens from the real blocks of both themes. Do not pin literal hex.
   - A third test with the same pattern is fixed the same way.
 - Persona: plan-marshall:persona-module-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Setting one dark `--color-*` of a tested pair to a low-contrast value makes the dark assertion fail, and restoring it
     passes; the failing assertion is quoted in the pull request body (evidence order: `mutation-after`)
   - A test fails if the "dark" block resolves to the same declarations as the light one (evidence order: `test-first`)

Sequencing: PLAN-72 restructures `tokens.css`; the selector-based reading must not depend on the header comment.
If the operator wants the fix before this plan's chain, move deliverable 3 to PLAN-129 (operator decision 2026-11-10, epic.md Decisions).

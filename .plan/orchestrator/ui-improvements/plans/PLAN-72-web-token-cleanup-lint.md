# PLAN-72: Web: Remove Old Tokens, Lint Raw Values

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Remove the old token aliases and add a stylelint rule that fails on a raw hex value or an old token name in a view (VD-S-08; stylelint approved by the operator).

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-08`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-02`

## Deliverables

1. **Aliases removed**
   - What: No old token name remains in `tokens.css` or any view.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-dev-frontend:lint-config, pm-dev-frontend-cui:cui-javascript-project
   - Done when:
     - A search finds no old token name; `./mvnw -pl pm-web-app verify` passes (evidence order: `none`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in dark and light look as before the alias removal; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)
2. **stylelint rule**
   - What: Add stylelint to `pm-web-app` with a rule that fails on a raw hex value or an old token in a `.svelte` style block, run in the Maven build.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security, pm-dev-frontend:lint-config, pm-dev-frontend-cui:cui-javascript-project
   - Done when:
     - A fixture file with a raw hex value fails the rule (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `fully-onboarded`: every route of the rail/header (overview, plan, epic, projects, decisions, reports, designer, system, cleanup, settings) in dark and light look as before the alias removal; baseline and after snapshots plus screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it) (evidence order: `none`)

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20720`, app port `20721` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- stylelint is the one new dependency the operator approved for this plan.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20720"`), then `PM_MOCK_WEB_PORT=20720 ./pm-web-app/start.sh -p 20721` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20720/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-08`, `doc/claude_review/visual-design/proposals.adoc#VD-P-02` — read at `doc/claude_review/visual-design/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: visual-design/plan.adoc:63 VD-S-08; proposals.adoc:28-29 VD-P-02; tokens.css aliases at :113-135 (comment :6) still present
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): Many listed components/views now exist or were changed by PLAN-25/35/43 and PLAN-90 (ErrorState, Skeleton, DataTable, Dialog, Tabs, Toast, Chip, etc.); tokens.css still carries the old aliases (line 100), no stylelint dependency yet (package.json), a styles/tokens.test.ts exists. Scope unchanged but the alias sweep now covers more files.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Aliases still at tokens.css:113+ (drift from :100); no stylelint in package.json/pom/eslint config, no .stylelintrc.json; tokens.test.ts ALIASES at :137. Amendment holds: success-subtle own value :88/:205
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first clause; no code fact to settle

## Expected Surface


- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/eslint.config.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/package.json` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/package-lock.json` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/pom.xml` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/.stylelintrc.json` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/test/stylelint-fixture.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `doc/design/web-app/theme-readability.adoc` — present at `dc811fb3` (added by the cleanup of 2026-11-14)

## Dependencies and Sequencing

- Depends on: `PLAN-70` (`web-light-theme-pass`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (messages 003, 004, 005, 006, 007, 008, 009).
**Gate: D1, D3 and D4 merged and the per-view packages landed, including "views no staged plan owns".** Queue status:
`parked`; the orchestrator re-grounds this spec when it is restarted. The Expected Surface above is NOT widened now (a parked
row is not in the disjointness gate population). When restarted:

- Tokens: the kind tints (Project, Epic, Plan only), `--color-success-subtle` with a value of its own, the diff tokens
  (`--color-diff-removed` / `--color-diff-added`, valid only inside a proposal diff), the `--color-mascot-*` tokens (003, 006,
  009, 005). The layout-mode spacing rules go away with `PLAN-111`'s CSS; nothing else to do for 004.
- **Lints that fail on:** `.badge` and ad-hoc tag classes; raw `<button>` / `<a>` / `<input>` / `<textarea>` / `<select>`;
  ad-hoc button classes; `href="#"`; `outline: none` outside `lib/components` (006, 007, 008). The D1 foundation adds them as
  **warnings**; this plan, after the per-view packages, switches them to **errors** and keeps its token and stylelint work.
- No `{@html}` in the markdown renderer (009).

## PLAN-148 Landing Residue (Amendment 2026-11-14)

- `--color-success-subtle` has its own value since `25137445` (`#E9F6EE` light, `#15271D` dark): drop it from this plan's list. The
  verdict evidence `tokens.css:102-126` shifted (alias line removed, token added at `:88`/`:205`); `ALIASES` in `tokens.test.ts:147-150` is
  one shorter. `theme-readability.adoc` says success ≥3:1 while the test enforces 4.5:1: align the document.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Already done: `--color-success-subtle` has its own value (25137445, #427). Aliases now at `tokens.css:113+`, `ALIASES` at `tokens.test.ts:137`.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-72-web-token-cleanup-lint.md
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
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/eslint.config.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/package.json` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/package-lock.json` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/pom.xml` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/.stylelintrc.json` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/test/stylelint-fixture.svelte` — absent at HEAD 1cc0e10e; created by this plan

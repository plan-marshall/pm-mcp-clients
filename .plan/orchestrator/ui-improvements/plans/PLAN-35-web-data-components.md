# PLAN-35: Web: Data Components

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Build `DataTable`, `EmptyState`, `ErrorState` and `Skeleton` of VD-P-05 (VD-S-04). The formatting helpers of VD-S-04 already came with `web-time-format-and-locale`.

Source of this plan (read before scoping):

- `doc/claude_review/visual-design/plan.adoc#VD-S-04`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-05`

## Deliverables

1. **DataTable**
   - What: Fixed column widths from a column spec, sticky head, right-aligned numbers, rows 40 px (compact 32 px), row menu in the last column, empty state slot.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: column spec widths, numeric alignment, empty slot (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: dev server: `/dev/components` shows DataTable, EmptyState, ErrorState, Skeleton in dark and light; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **EmptyState, ErrorState, Skeleton**
   - What: Icon or brand mark, a title, one sentence, one act; `ErrorState` folds the technical code under "Details"; skeleton shapes row, card, table, figure with a shimmer that stops under reduced motion.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests per component; reduced motion stops the shimmer (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: dev server: `/dev/components` shows DataTable, EmptyState, ErrorState, Skeleton in dark and light; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Components document** (moved here from `PLAN-74`: the plan that builds the last components writes their document)
   - What: New `doc/design/web-app/components.adoc` with the specifications of VD-P-05 and one picture per component (the controls of `PLAN-25`, the containers of `PLAN-31` and the data components of this plan), linked from `doc/design/web-app/README.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The document exists, is linked from the README, and passes `doc-review` (under the runbook lane its checker `trace.py` does not run, so `pm-documents:ref-documentation` `docs review` and `pm-documents:ref-asciidoc` `validate` and `verify-links` stand in for it, clean, as PLAN-06 and PLAN-07 ran them) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20350`, app port `20351` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20350"`), then `PM_MOCK_WEB_PORT=20350 ./pm-web-app/start.sh -p 20351` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20350/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/visual-design/plan.adoc#VD-S-04`, `doc/claude_review/visual-design/proposals.adoc#VD-P-05` — read at `doc/claude_review/visual-design/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/claude_review/visual-design/plan.adoc:40-42 states VD-S-04 with VD-P-05, VD-P-07; proposals.adoc:195 '== VD-P-07: Data Display' exists
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/DataTable.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/EmptyState.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/ErrorState.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Skeleton.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 07104678 (created by PLAN-31); this plan adds the data components to it
- OBSERVED: `doc/design/web-app/components.adoc` — absent at HEAD 07104678; created by this plan
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 07104678

## Dependencies and Sequencing

- Depends on: `PLAN-31` (`web-core-components-containers`), `PLAN-16` (`web-time-format-and-locale`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-35-web-data-components.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

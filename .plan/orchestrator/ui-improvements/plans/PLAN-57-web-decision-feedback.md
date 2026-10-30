# PLAN-57: Web: Correct Feedback for a Decision

> **SUPERSEDED on 2026-10-30 (structure review, operator decision): do not emit.** The two deliverables moved into `PLAN-51` (deliverables 3 and 4), because both plans work on the one `DecisionView` and `PLAN-57` could only run after `PLAN-51`. The text below is kept for the record.

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Name the recommendation by its option, render evidence as fields, and confirm the operator's own answer by request id so "answered elsewhere" appears only for answers given elsewhere (UC-S-06; UC-P-05).

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-06)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-05`
- `doc/claude_review/use-cases/findings-plans-control.adoc#UC-F-21`

## Deliverables

1. **Recommendation and evidence**
   - What: "Recommended: Rebase onto main (by implement)"; evidence as fields with plain labels, `file`/`lines` as a link where the project offers one, unknown structures as a folded JSON block (`EvidenceView.svelte`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: label, fields, the fallback (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Own answer confirmed**
   - What: The answer request carries a client request id; the `decision.answered` event with that id confirms "Decided: <option>"; other events produce "answered elsewhere" with channel, device and option label.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `decisions.spec.js`: own answer; a `POST /mock/broadcast` from `cli` shows "answered in the terminal" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20570 PM_WEB_PORT=20571 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20570`, app port `20571` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20570"`), then `PM_MOCK_WEB_PORT=20570 ./pm-web-app/start.sh -p 20571` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20570/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-06)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-05` — read at `doc/claude_review/use-cases/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/use-cases/plan.adoc:56 UC-S-06 (Decision feedback); use-cases/proposals.adoc:114 [#UC-P-05] Correct feedback for a decision
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 added the key `decision.notify.several` (`{count} decisions wait for you` / `{count} Entscheidungen warten auf dich`) to `doc/design/shared/decisions-texts.adoc`. `PLAN-83` (deliverable 3) puts it into `decisions.{en,de}.properties` with the burst rule; this plan's feedback texts must not duplicate or rename it.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/PremiseEvidenceView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/EvidenceView.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-51` (`web-decisions-triage`), `PLAN-76` (`shared-port-assignment`)
- Overlaps with: `web-decisions-triage` (same view): runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-57-web-decision-feedback.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

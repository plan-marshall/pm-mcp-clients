# PLAN-51: Web: Decisions as Triage

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Turn the decisions page into a triage list with the detail beside it, keyboard decisions, auto-advance and a toast (UX-S-08; UX-P-06).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-08)`
- `doc/claude_review/ux/proposals.adoc#UX-P-06`
- `doc/claude_review/ux/ux-decision-triage.svg`
- `doc/claude_review/ux/findings.adoc#UX-F-11`

## Deliverables

1. **Triage list and detail**
   - What: The page sits on the `TriagePage` template of `PLAN-43` (this plan moves it there: `PLAN-43` no longer does). Rows: title, plan path, blocks mark, reversible mark, since, recommended option; blocking first then oldest; group by project as a toggle; detail without the repeated description; the layout toggle is removed for decisions.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `decisions.spec.js` on `running-work` → `decisions.open`: eight decisions, blocking first (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open`: triage list, `1` and `Enter` decide, next selected, toast; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **Keyboard and advance**
   - What: `1`–`9` choose, `Enter` decides, `c` chat, `l` later; after deciding a toast "Decided: <option>" and the next decision is selected; phone: list then detail.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - e2e: `1`, `Enter` decide; the next is selected; the toast names the option (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open`: triage list, `1` and `Enter` decide, next selected, toast; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **Recommendation and evidence** (from the former `PLAN-57`, superseded: one view, one plan)
   - What: "Recommended: Rebase onto main (by implement)"; evidence as fields with plain labels, `file`/`lines` as a link where the project offers one, unknown structures as a folded JSON block (`EvidenceView.svelte`).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests: label, fields, the fallback (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
4. **Own answer confirmed** (from the former `PLAN-57`)
   - What: The answer request carries a client request id; the `decision.answered` event with that id confirms "Decided: <option>"; other events produce "answered elsewhere" with channel, device and option label.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security
   - Done when:
     - `decisions.spec.js`: own answer; a `POST /mock/broadcast` from `cli` shows "answered in the terminal" (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `running-work` → `decisions.open` and `decisions.answered-elsewhere` (plus a `POST /mock/broadcast` from `cli`); baseline and after snapshots plus screenshots recorded (evidence order: `none`)
5. **One notification for a burst, none after a reconnect** (from the package `web-moving-between-plans`, moved here from `PLAN-83`)
   - What: a notification fires only for a decision the client has not announced before (`doc/design/shared/decisions.adoc#announce`, merged with PR #278): the decision announcer keeps the set of decisions it has announced; the first read after a start, a reload or a reconnect primes that set silently; decisions that arrive within one second make one notification (several: `decision.notify.several`, `{count} decisions wait for you` / `{count} Entscheidungen warten auf dich`, which opens the list of open decisions; one: its own, which opens that decision); a decision answered before the notification shows is dropped from it. The key `decision.notify.several` goes into `decisions.{en,de}.properties`; a notification never carries a decision's content.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Unit tests with a fake clock and a fake notification: a reconnect raises none, a burst of three raises one that opens the list, a single decision raises one that opens it, an answered decision is not announced (evidence order: `test-first`)
     - An e2e test through the mock's decision events: the same two cases in the browser (evidence order: `test-first`)
     - The row of the package `web-moving-between-plans` in `doc/plans/README.adoc` is set to `done` if `PLAN-83` and `PLAN-59` D4 have landed (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `PM_MOCK_WEB_PORT=20510 PM_WEB_PORT=20511 ./mvnw -pl pm-web-e2e verify -Pe2e`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` (`javascript`, `css`, `jest-testing`, `lint-config`, `javascript-security`) and `pm-dev-frontend-cui` (`cui-javascript-project`) apply to `pm-web-app` and `pm-web-e2e`; text from the runtime or from a model reaches the DOM through text-treating sinks only (no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}`); a test waits on a condition, never on an arbitrary timer; the tests run on Vitest with Testing Library and the `jest-testing` standards apply as method.
- Ports of this plan: mock web port `20510`, app port `20511` (`AGENTS.md` § Ports, made passable by `shared-port-assignment`, PLAN-76). Pass them to every mock, app and e2e start; other plans may run at the same time, so never stop or reuse a process this worktree did not start, and an assigned port that is in use is a blocker, never a reason to take another port.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 20510"`), then `PM_MOCK_WEB_PORT=20510 ./pm-web-app/start.sh -p 20511` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:20510/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target; the server writes to its `--output-dir` `.plan/temp/playwright-mcp/` (`opencode.json`), so name every file `{plan}-{target}-{before|after}` and never write to the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-08)`, `doc/claude_review/ux/proposals.adoc#UX-P-06` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: cc5794c9 | by: ui-improvements/cleanup | rescoped: n/a | evidence: origin/main doc/claude_review/ux/plan.adoc:58 has UX-S-08 (Decisions as triage); ux/proposals.adoc:269 [#UX-P-06]
- Re-grounded note at HEAD 07104678 (2026-10-30): PR #278 added a rule to `doc/design/shared/decisions.adoc` (#announce): a notification fires only for a decision the client has not announced before (none after a reconnect or a reload), decisions arriving within one second make one notification, several open the list of open decisions (`decision.notify.several`) and one opens that decision. this plan builds it as deliverable 5 (moved here from `PLAN-83`); the triage view must not re-announce on its own re-read.
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/PremiseEvidenceView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/EvidenceView.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD 07104678
- OBSERVED: `doc/design/shared/decisions-texts.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/stores/viewLayout.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/ViewLayoutToggle.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/shared/decisions.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-76` (`shared-port-assignment`, shipped)
- Overlaps with: `PLAN-83` (package `web-moving-between-plans`), `PLAN-21` (the terminal answer dialogue; separate client)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-51-web-decisions-triage.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

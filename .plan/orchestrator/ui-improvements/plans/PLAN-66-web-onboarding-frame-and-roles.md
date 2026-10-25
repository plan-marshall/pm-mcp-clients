# PLAN-66: Web: Onboarding Variations, Frame to Providers

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Add the first onboarding variations of UC-P-03 in the review's order: the frame until onboarded, `defaults.review`, `roles.review`, `providers.failed` (UC-S-04, part 1).

Source of this plan (read before scoping):

- `doc/claude_review/use-cases/plan.adoc (UC-S-04)`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-03`
- `doc/claude_review/use-cases/findings-onboarding-entry.adoc`

## Deliverables

1. **Frame**
   - What: Until onboarded, the header shows brand, machine, language and theme only; the step bar marks no step on the mode screen.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `onboarding.spec.js`: no navigation before onboarding is done (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
2. **defaults.review**
   - What: Per axis: name, one sentence of meaning, a choice of its variants with plain names, variants that need a missing provider disabled with the reason; posture only on a later run.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A screen test on `defaults.review`: no raw unit id (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
3. **roles.review**
   - What: Roles grouped by phase; departures marked; an advisory per role; a change per role (harness, model, effort); "Ask the steward" with accept or reject per departure; an unmet role empty and assignable.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A screen test on `roles.review` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
4. **providers.failed**
   - What: Label "Provider"; acts "Enter the token again" (`CredentialEntry`), "Check again", "Continue without".
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A screen test on `providers.failed` (evidence order: `test-first`)
     - Browser check (Playwright MCP) shows this deliverable's change as specified on: `onboarding` → `onboard.mode`, `defaults.review`, `roles.review`, `providers.failed`; baseline and after snapshots plus screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- Browser check of the rendered output, with the Playwright MCP server of `opencode.json`, following `AGENTS.md` § Browser checks: start the mock (`./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"`), then `./pm-web-app/start.sh` (read the port from `pm-web-app/target/pm-web-app.port`); the MCP browser starts with an empty profile, so pair again every session with a fresh single-use code (`POST http://127.0.0.1:7420/mock/pairing`, then open `<app>/pair#code=<CODE>`), never through the `Network:` URL; reach a state with `POST /mock/goto/<state>` and a reload. Check each target in `de` and `en`, dark and light, at 1440 × 900 and 390 × 844 (`browser_resize`). Read the page through `browser_snapshot` (the accessibility tree is the evidence of text and structure) and keep one `browser_take_screenshot` per target, both saved under `.plan/temp/browser/{plan}/` — never the repository root. Take a baseline snapshot BEFORE the change and one AFTER, and record both paths in the progress record. The visible text of a checked view shows no raw enum, state id, `{placeholder}`, ISO time or emoji outside model-written blocks. Stop the app (`./pm-web-app/stop.sh`) and the mock afterwards. If the Playwright MCP server is not available in the run, the browser Done-when clauses are NOT met — a blocker reported as such, never replaced by a unit test or skipped.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/use-cases/plan.adoc (UC-S-04)`, `doc/claude_review/use-cases/proposals.adoc#UC-P-03` — read at `doc/claude_review/use-cases/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/onboarding/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/views/OnboardingView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/AspectChoice.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/CredentialEntry.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/onboarding.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/onboarding-texts.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/onboarding.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-64` (`web-onboarding-screen-registry`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-66-web-onboarding-frame-and-roles.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

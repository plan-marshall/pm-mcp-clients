# PLAN-39: Web: App Shell With Rail, Top Bar, Bottom Bar, Live Indicator

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Build the app shell of UX-P-02: a left rail with three groups, a top bar with path, search trigger, live indicator and neutral machine pill, a bottom bar on the phone, language and theme in the rail foot; every route keeps working (UX-S-01; UX-P-02, UX-P-12).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-01)`
- `doc/claude_review/ux/proposals.adoc#UX-P-02`
- `doc/claude_review/ux/proposals.adoc#UX-P-12`
- `doc/claude_review/ux/ux-shell-inbox.svg`

## Deliverables

1. **Shell split**
   - What: `routes/+layout.svelte` delegates to `lib/shell/AppShell.svelte`, `NavRail.svelte`, `TopBar.svelte`, `BottomBar.svelte`; the rail is 232 px, collapsible to 64 px, state kept per browser; the rail head carries `BrandMark`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `chrome.spec.js`: three groups; counts equal today's header counts; every route renders (evidence order: `test-first`)
2. **Phone**
   - What: Below 768 px: no rail; a bottom bar with Inbox, Running, Decisions, More (a sheet with the rest).
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `mobile.spec.js`: four labelled items; no horizontal scroll on any route at 390 px (evidence order: `test-first`)
3. **Live indicator**
   - What: "● Live" while the event stream is connected, "Reconnecting…" in amber, "Offline" in red with "Try again"; it replaces the offline banner.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test drives the stream states; e2e stops and restarts the mock (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Every view this plan touches uses the design system v2 components (VD-S-06): no plain text button, no acting link, no state pill, no uppercase label, no emoji, no own page width.
- Every server value on a touched view passes through the display layer and the formatter of WS-02.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- The design document of `doc/design/` that this plan changes is updated in the same pull request (UX-S-13, VD-S-09).
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-01)`, `doc/claude_review/ux/proposals.adoc#UX-P-02` — read at `doc/claude_review/ux/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/routes/+layout.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/shell/AppShell.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/NavRail.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/TopBar.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/BottomBar.svelte` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/shell/` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/stores/runtime.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/chrome.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/workspace.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/mobile.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-31` (`web-core-components-containers`), `PLAN-27` (`web-brand`), `PLAN-04` (`web-theme-and-titles`)
- Overlaps with: `web-theme-and-titles` edits `+layout.svelte`: runs after it
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-39-web-app-shell.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

# PLAN-60: Web: One Settings Place, One Save per Form

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Merge the two settings pages into one `/settings` with tabs Setup, Access, Credentials, Consents, Retention, give every form one save with "changed" marks and "reset to inherited", and show the conflict notice with the other client's value (UX-S-12, settings part of UC-S-04; UX-P-02, UX-P-11).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-12)`
- `doc/claude_review/ux/proposals.adoc#UX-P-02`
- `doc/claude_review/ux/proposals.adoc#UX-P-11`
- `doc/claude_review/use-cases/proposals.adoc#UC-P-03`
- `doc/claude_review/ux/findings.adoc#UX-F-03`
- `doc/claude_review/ux/findings.adoc#UX-F-16`

## Deliverables

1. **One settings view**
   - What: `SettingsView` and `MachineSettingsView` merge into one `SettingsView` with a tab per part; `/settings/machine` redirects to `/settings?tab=setup`.
   - Persona: plan-marshall:persona-integration-tester
   - Skills: pm-dev-frontend:javascript
   - Done when:
     - `onboarding.spec.js`, `devices.spec.js`: the redirect and every tab (evidence order: `test-first`)
2. **Forms**
   - What: One form, one "Save" disabled until a change, a "changed" mark per field, "Reset to inherited" for an override; plain labels with one sentence of help; no raw keys.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Unit tests: save disabled until change; reset to inherited (evidence order: `test-first`)
3. **Conflict notice**
   - What: A settings conflict shows the other client's value and "Apply mine again" (UC-ONB-10).
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test of the conflict notice (evidence order: `test-first`)
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

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-12)`, `doc/claude_review/ux/proposals.adoc#UX-P-02` — read at `doc/claude_review/ux/plan.adoc`
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/SettingsView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/SettingsView.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.svelte` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/MachineSettingsView.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/routes/settings/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/routes.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/devices.spec.js` — present at HEAD c4dc9bb4
- OBSERVED: `doc/design/web-app/machine-settings.adoc` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/settings.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/settings.de.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/devices.en.properties` — present at HEAD c4dc9bb4
- OBSERVED: `pm-api/src/main/resources/messages/devices.de.properties` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-43` (`web-page-templates`), `PLAN-47` (`web-actions-by-state`)
- Overlaps with: none known at staging
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-60-web-settings-one-place.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

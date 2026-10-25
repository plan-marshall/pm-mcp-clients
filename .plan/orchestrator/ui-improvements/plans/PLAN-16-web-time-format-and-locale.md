# PLAN-16: Web: One Time Format and a Full Locale Switch

epic: ui-improvements
workstream: WS-02

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

One formatter for time, duration, tokens and bytes on the shared rule, every raw time and inline number format replaced, a locale switch that refreshes every text, and the recorded interface language applied on first load (CC-S-04; CC-P-04, CC-P-05, VD-P-07). The formatting helpers of VD-S-04 are built here, once.

Source of this plan (read before scoping):

- `doc/claude_review/cross-cutting/plan.adoc#CC-S-04`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04`
- `doc/claude_review/cross-cutting/proposals.adoc#CC-P-05`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-07`
- `doc/claude_review/cross-cutting/findings.adoc#CC-F-10`

## Deliverables

1. **Formatter**
   - What: `lib/display/format.ts`: `formatTime(iso, style)` with `relative`, `short`, `clock` through `Intl`, `formatDuration`, `formatTokens(n, floor)`, `formatBytes`; replaces `lib/utils/tokenFormat.ts`.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - Table tests per style and locale, following `shared/texts-and-languages.adoc` (evidence order: `test-first`)
2. **Every view formatted**
   - What: Lists use `relative` with `short` on hover; details use `short`; the activity stream uses `clock` with the date on the first row of each day.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The text-guard baseline holds no ISO time (evidence order: `none`)
3. **Locale switch and recorded language**
   - What: `setLocale` replaces the cached catalogue only after the new one loaded; the cache key includes the catalogue version of `/api/v1/status`; summaries are catalogue messages with plural rules; the recorded interface language sets the locale on first load.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A unit test switches `de` → `en` → `de` and finds no word of the other locale in a summary (evidence order: `test-first`)
     - An e2e check: after onboarding with "Deutsch for both", a fresh browser opens in German (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order: `none`)

## Standing Constraints

- Build and test through Maven only (CLAUDE.md). Beside the lane's build gate, these module builds pass at the head: `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify -Pintegration-tests`.
- No new dependency or plugin unless this spec names it as approved by the operator.
- Views this plan touches pass the text guard of `web-text-guard-report`: the baseline file may only shrink, never grow.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/cross-cutting/plan.adoc#CC-S-04`, `doc/claude_review/cross-cutting/proposals.adoc#CC-P-04` — read at `doc/claude_review/cross-cutting/plan.adoc`
- HYPOTHESIS: `lib/i18n/index.ts` caches catalogues in `localStorage` (`pm_i18n_messages_<locale>`) and a stale cache causes CC-F-10 — confirm/refute at `pm-web-app/src/lib/i18n/index.ts` (verify-at-outline)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/display/format.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/display/format.test.ts` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-app/src/lib/i18n/index.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/i18n/index.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/utils/tokenFormat.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/utils/tokenFormat.test.ts` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/views/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-app/src/lib/components/` — present at HEAD c4dc9bb4
- OBSERVED: `pm-web-e2e/tests/text-guard-baseline.json` — absent at HEAD c4dc9bb4; created by this plan
- OBSERVED: `pm-web-e2e/tests/onboarding.spec.js` — present at HEAD c4dc9bb4

## Dependencies and Sequencing

- Depends on: `PLAN-06` (`shared-display-vocabulary`), `PLAN-09` (`web-display-layer`)
- Overlaps with: touches most views; runs after `web-display-layer` (same views)
- Adjacent to: none

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-16-web-time-format-and-locale.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

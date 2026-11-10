# PLAN-146: Web: Intent and Outline, the Epic's Intent and the over_size Distribution

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of
> 2026-11-10 (the web intent and outline package, part 2). It follows `PLAN-145` (the client methods and the catalogue area) and
> WAITS for the D6 design session. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

`PLAN-145` delivered the client methods and the plan's panels. This plan delivers the epic's side and the chart: the epic's
intent (destination, done-when checks with their verdicts and evidence, non-goals, what is not yet specified, each plan's
criterion) and the `over_size` distribution as a small bar chart readable without colour and at phone width. PLAN-58 places
the epic panel in the epic's Intent tab; PLAN-59 or PLAN-60, as D6 decided, mounts the chart. Until then both are shown on
`/dev/components` with live mock data.

## Deliverables

The documents are the specification; read them first: `doc/design/shared/intent-and-outline.adoc` (#epic, #oversize, #web, #not),
`doc/design/shared/intent-and-outline-texts.adoc`, the D6 answers on the chart's home, `dataviz` guidance for the chart.

1. **The epic's intent panel**
 - What: `EpicIntentPanel.svelte` (prop `epicId`) shows the destination (one sentence), the done-when checks each with its verdict
   (`pending` before the completion review, `met` or `not met` after, never a progress bar) and its evidence as text (a path or a
   `pm://` URI, never turned into a link), the non-goals, the not-yet-specified questions shown as questions, and each plan's done-when
   with the plan's title. Every text is plain text after `stripControlSequences`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `EpicIntentPanel.test.ts`: pending versus decided checks, evidence with several entries and none, an empty not-yet-specified
     list, a plan without a done-when, and a text with markup rendered as text. It fails on a revert. Evidence order: `test-first`.
2. **The `over_size` chart**
 - What: `OverSizeChart.svelte` (prop `projectId`, or the record) draws the buckets of the distribution in the runtime's order: an
   empty bucket is left out, the last is open-ended (`Nx and more`), the buckets the proposed multiple would flag are marked, and
   the counts, the flagged share (flagged tasks over all tasks, as a count and a percentage) and the direction (`3x → 2x`
   lowers the multiple and flags more) stand in text, so it reads without colour and at phone width. It uses tokens only.
   It never recomputes a bucket from raw tokens.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `OverSizeChart.test.ts`: the buckets of the mock record, an empty bucket left out, the open-ended bucket, the marked buckets,
     the share text, the lower and the raise direction, a record with no tasks, and no colour-only meaning (text for every mark).
     Evidence order: `test-first`.
3. **Fixtures and browser evidence**
 - What: fixtures of both components on `routes/dev/components/+page.svelte` reading the mock's `relay-hardening` and
   `pm-mcp-clients` records through the real client, in both themes and both locales; `intent.spec.js` (created by PLAN-145) gains the
   epic panel and the chart; an axe check of both at `390` px.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `intent.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set. Evidence order: `test-first`.
   - Browser check (Playwright MCP) of both fixtures on `running-work` at `1440 × 900` and `390 × 844`, German and English, both
     themes. Evidence order: `none`.
   - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open (`unknown` for
     a failed read, never `0`); a confirmed `0` or every open issue dismissed. Evidence order: `none`.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-api,pm-web-app verify`, `./mvnw -pl pm-api,pm-web-app verify -Ppre-commit` (review every rewrite),
  `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e` at the head.
- No new dependency or plugin (no chart library: the chart is markup and CSS from the tokens). Web standards of `CLAUDE.md`;
  no static `style` attribute; every text from the catalogue in English and German; the text guard baseline does not grow.
- Frontend tests on Vitest with Testing Library (`pm-dev-frontend:jest-testing`).
- Ports of this plan: mock web port 21460, app port 21461 (`20000 + 10 × 146`) (`AGENTS.md` § Ports). Other plans run at the same
  time: never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The merge rule: the merge question is asked only when every review thread has a reply and is resolved and every review body is
  answered, and some head has had one CodeRabbit review. The pull request body states thread count, resolved count and review-body
  items from `ci pr comments`, never from memory. Merge only on the operator's word. Commits end with
  `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer. If CodeRabbit refuses on
  quota, stop and ask the operator.
- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues, every open issue fixed in
  a commit of THIS pull request. State `sonar_open_new_issues=N` in the landing message. A dismissal needs the operator's word.

## Claim Labels

- OBSERVED: `EpicIntentResource` carries epic_id, destination, done_when (text, verdict `pending|met|not_met`, evidence), non_goals, not_yet_specified and plans (plan_id, title, done_when) — read at `pm-api/src/main/java/de/planmarshall/api/intent/EpicIntentResource.java:51-72`, `DoneWhenCheck.java`
- OBSERVED: `OverSizeDistributionResource` carries project_id, role, current_multiple, proposed_multiple and buckets (label, from_multiple, to_multiple, task_count, flagged_tasks) and no total or share field — read at `OverSizeDistributionResource.java:50-54`, `TokenBucket.java:48-52`
- OBSERVED: the mock serves `GET /api/v1/epics/relay-hardening/intent` and `GET /api/v1/projects/pm-mcp-clients/metrics/oversize` in `running-work.json` and `fully-onboarded.json` — read at `pm-mock-runtime/src/main/resources/scenarios/running-work.json:1298,1358`
- OBSERVED: `machine-settings.adoc` has no role-multiple line, so the chart's home is D6's decision — read by `git grep -n -i "over_size\|oversize" -- doc/design/web-app` (empty)
- OBSERVED: no chart component exists in the web app — read by `git ls-files pm-web-app/src/lib/components` (no chart)
- HYPOTHESIS: the flagged share is the sum of `flagged_tasks` over the sum of `task_count`, which the design allows ("the sum of the flagged tasks over all tasks") without recomputing from raw tokens — confirm/refute at `doc/design/shared/intent-and-outline.adoc` § `buckets` (:260-263) (verify-at-outline)
- HYPOTHESIS: the plan's epic panel can be shown for an epic whose intent the daemon does not serve yet with an empty state, as for a plan — confirm/refute at `pm-api/.../intent/IntentContractTest.java` and the D6 documents (verify-at-outline)
- Verify-first clause: re-read the D6 documents (the chart's home, the verdict tag) and `PLAN-145`'s landing at the then-current HEAD. If the verdict tag is a new component, reuse PLAN-145's.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/EpicIntentPanel.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/OverSizeChart.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at HEAD 1cc0e10e (read; methods are PLAN-145's)
- OBSERVED: `pm-web-e2e/tests/intent.spec.js` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `pm-api/src/main/resources/messages/intent.en.properties` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `pm-api/src/main/resources/messages/intent.de.properties` — absent at HEAD 1cc0e10e; created by `PLAN-145`, extended here
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-145` landed (the client methods, the catalogue area, the e2e file); the D6 design session merged (the chart's home).
- Overlaps with: `routes/dev/components/+page.svelte` (PLAN-45 running, PLAN-130 staged; PLAN-145 before this plan).
- Adjacent to: `PLAN-58` (places the epic panel in the epic's Intent tab); `PLAN-59` (the project page's setup tab) or `PLAN-60` (the settings' roles aspect), whichever D6 names, mounts the chart; the decomposition review, which shows the same epic intent (the views package; it reuses `EpicIntentPanel`).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD (this spec was grounded at `1cc0e10e`). Re-run `corpus surfaces` against everything that landed since. Settle each HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-146-web-intent-epic-and-oversize.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write. The session reports its outcome through its PR and its inbox message (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

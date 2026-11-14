# PLAN-164: A Plan's Changes in the Web App

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-14
> ("Stage plan-changes rows", cleanup of the parked gates) from the work package `doc/plans/plan-changes.adoc` (index status `open`). This spec is SELF-SUFFICIENT.

## Objective

The web plan view gets the tab _Changes_. It is absent before the first capture, and the size line opens it. A file's hunks open on the
diff tokens, and the open file sits in the address `/plans/{plan_id}?tab=changes&file=<path>`. A worker's activity shows `model_differs`.
This covers task 3 of the work package and the web side of task 4.

PLAN-55 (the plan page's tab set) is parked on this plan. This plan builds the tab's content and mounts it in today's `PlanView`; PLAN-55
then carries it into its tab set.

## Deliverables

Read first:
- `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc` § `plan-changes` (The Plan's Changes): the list, a file, read-never-run, untrusted text, `not_captured`, `unavailable`, live.
- `../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc:68`: `plan.change_captured` {`plan_id`, `head`, `files_total`, `insertions_total`, `deletions_total`, the unplanned count}.
- `doc/design/shared/plan-view.adoc` `#changes` (:172) and `#worker`.
- `doc/design/web-app/plan-view-interaction.adoc` `#changes` (:117).
- `pm-web-app/src/lib/views/PlanView.svelte`, `pm-web-app/src/lib/api/client.ts`.
- `doc/design/web-app/components.adoc` (the D1 vocabulary): use `StatusLabel`, `Chip`, `Link` and `KindMark` where they fit; draw no hand-made badges.

1. **Client calls and the Changes view**
 - What:
   - Add `client.ts` calls for the list and a file, and the `plan.change_captured` event (reload the list).
   - Add a `PlanChanges` component: the file list (status, counts, renamed-from, binary, unplanned, cut) and a file's hunks with old and new line numbers.
   - Mount it in `PlanView` as the tab _Changes_, absent before the first capture. The size line opens it.
   - The address `?tab=changes&file=` survives a reload.
   - Every path and line is text, rendered through a text sink only.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:css
 - Done when:
   - Component tests show the list, a file, a cut file and a binary row. A line holding markup renders as text, never as an element. Each fails on a revert. Evidence order: `test-first`.
   - A reload with `?tab=changes&file=` opens the same tab and file (`PlanView.test.ts`, plus an e2e step in a new `plan-changes.spec.js`).
   - `not_captured` hides the tab; `unavailable` shows its reason.

2. **`model_differs` in the worker's activity**
 - What: the worker's activity in `PlanView` shows both models, while the head keeps the launch model.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when: a `PlanView.test.ts` case against the PLAN-162 fixture pins both models and the head, and fails on a revert.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, `./mvnw verify -Ppre-commit` passes (every rewrite reviewed and committed), and so does `./mvnw -pl pm-web-app verify` and `PM_MOCK_WEB_PORT=21640 PM_WEB_PORT=21641 ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. In particular, NO diff library: the hunks are served data, and the mock serves them from fixtures.
- Web standards of `CLAUDE.md`. Every changed line and every path is untrusted text: render it through a text sink only, never `{@html}`/`innerHTML`.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; expected wire and catalogue text stays literal. Never catch or throw generic `Exception`/`RuntimeException`.
- Ports of this plan: per `AGENTS.md` § Ports (`20000 + 10 × 164`): mock web port 21640, app port 21641. Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item answered on the pull request, read from GitHub's thread state, never from a count kept in memory.
  - **Every reply answers its own finding.** Bind each reply to the finding's anchor. A bot's follow-up "not marking as addressed" means the finding is still open (lesson `2026-11-14-14-007`).
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings: 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`). This is a merge gate.
- The landing verdict is given per Done-when clause, never per deliverable (lesson `2026-11-14-14-001`).

## Claim Labels

- OBSERVED: `PlanView.svelte` has no Changes tab and `client.ts` no changes call — `git grep -n "changes" client.ts PlanView.svelte` at `7adb56c2` finds no route
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (client.ts changes hits are source previews only; KindMark/Link/RowLink/StatusLabel/Chip present)
- OBSERVED: `KindMark`, `Link`, `RowLink`, `StatusLabel` and `Chip` exist in `pm-web-app/src/lib/components/` — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (client.ts changes hits are source previews only; KindMark/Link/RowLink/StatusLabel/Chip present)
- HYPOTHESIS: `PlanView` already reads `tab=` from the address — confirm/refute at `PlanView.svelte` and `lib/routes.ts` (verify-at-outline)
  - verdict: unverifiable | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/api/client.ts` — present at `7adb56c2`
- OBSERVED: `pm-web-app/src/lib/api/client.test.ts` — present at `7adb56c2`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.svelte` — present at `7adb56c2`
- OBSERVED: `pm-web-app/src/lib/views/PlanView.test.ts` — present at `7adb56c2`
- OBSERVED: `pm-web-app/src/lib/components/PlanChanges.svelte` — absent; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/PlanChanges.test.ts` — absent; created by this plan
- OBSERVED: `pm-web-e2e/tests/plan-changes.spec.js` — absent; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at `7adb56c2`
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at `7adb56c2`

## Dependencies and Sequencing

- Depends on: `PLAN-162`.
- Overlaps with:
  - `PLAN-145` (unparked, plan panels): `PlanView.svelte`. Do not run the two at once.
  - `PLAN-142`/`PLAN-143` (navigation engine): `client.ts` and the addresses. If the engine has landed, use its address helpers for `?tab=changes`.
- Unblocks: `PLAN-55`, which carries the tab into its tab set.

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check whether PLAN-55, 142, 143 or 145 changed `PlanView`'s tab handling.
- Settle the HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-164-web-plan-changes-tab.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

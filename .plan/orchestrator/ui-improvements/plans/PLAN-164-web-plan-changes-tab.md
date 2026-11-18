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
   - The address `?tab=changes&file=` survives a reload. **Re-scoped 2026-11-15 (cleanup, claim 2 refuted):** the address model already parses `?tab=` (including `changes`) into the runtime store's `objectTab` (`routes.ts:90`, `:485-491`, `:569`; `runtime.ts:571`, PLAN-143 #446), and no view consumes it yet. `PlanView` therefore reads `objectTab` and writes the tab through the address helpers; it adds no address parsing of its own.
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
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanView.svelte has no Changes tab and client.ts no plan changes call (client.ts hits are ChangeSizeRecord :1009 and local-changes :3392-3405)
- OBSERVED: `KindMark`, `Link`, `RowLink`, `StatusLabel` and `Chip` exist in `pm-web-app/src/lib/components/` — read at `7adb56c2`
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: KindMark Link RowLink StatusLabel Chip all exist in pm-web-app/src/lib/components
- HYPOTHESIS: `PlanView` already reads `tab=` from the address — confirm/refute at `PlanView.svelte` and `lib/routes.ts` (verify-at-outline)
  - verdict: contradicted | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: yes | evidence: PlanView.svelte has no tab handling; the address model parses ?tab= incl. changes (routes.ts:90 :485-491 :569) into runtimeStore objectTab (runtime.ts:571) but no view consumes objectTab

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
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `6b97acd2` (D0: the first capture)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` — present at `6b97acd2` (D0: a scheduled `plan.change_captured`)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanChangesScenarioTest.java` — present at `6b97acd2` (D0)

## Dependencies and Sequencing

- Depends on: `PLAN-162` (shipped, #444) and, since the operator's decision of 2026-11-15, **`PLAN-163` landed** (its D0 adds the six `plan.changes.*` keys to the
  `plan_view` pack, corrects the fixtures and the contract's coherence rules, and unifies the file route's refusals; this plan adds none of them).
- Overlaps with:
  - `PLAN-145` (shipped, #443): the plan panels' client methods are in `client.ts`; `PlanView.svelte` is free again.
  - `PLAN-142`/`PLAN-143` (navigation engine): `client.ts` and the addresses. If the engine has landed, use its address helpers for `?tab=changes`.
- Unblocks: `PLAN-55`, which carries the tab into its tab set.

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check whether PLAN-55, 142, 143 or 145 changed `PlanView`'s tab handling.
- Settle the HYPOTHESIS.

## Emit Amendment 2026-11-17 (PLAN-163 landed)

Source: `landings/PLAN-163.md` (PR #463, `ae410264`). The dependency "`PLAN-163` landed" is met. It supersedes nothing above; it adds what the landing shows.

- **Re-ground first.** This spec was written before PLAN-163's D0. Re-read at the outline, at the then-current HEAD, the shipped contract: `PlanChangeFile`, `PlanChangedFile`, `PlanChangeHunk`, `PlanChanges` and their coherence rules (`pm-api/.../changes/`), the six `plan.changes.*` keys in the `plan_view` pack (en and de; this plan adds none), the fixtures at `pm-mock-runtime/src/main/resources/scenarios/running-work.json` (totals 12 files, +340, −27, 2 binary), and the file route's refusals (`scope_not_found` for an unknown plan, `target_not_found` for a gone or never-captured file). A claim of this spec the shipped contract contradicts is re-scoped and reported, never worked around.
- **A file answer carries no head** (decided in PLAN-163 D0, `PlanChangeFile.java:30-32`). The terminal client has an open race because of it: a capture that lands between a file read and its display leaves old hunks under new counts (`landings/PLAN-163.md` L3-2). This plan's web client reads a list and a file the same way: state in the outline how the tab avoids the race (re-read the list and compare the row's counts, or discard a file answer whose list identity changed), and pin it with a test that fails on a revert.
- **Paths.** The terminal one-shot abbreviates paths and so cannot feed `--file` (`landings/PLAN-163.md` L3-1). The web tab shows the full path in the row (an abbreviation only as a visual truncation with the full path as its text content), so a path can be copied.
- **Overlap with running PLAN-51 (operator, 2026-11-17, `AskUserQuestion`: emit now).** Two files are shared: `pm-web-app/src/lib/api/client.ts` and `pm-web-app/src/lib/api/client.test.ts`. Whichever pull request lands second merges `main` into its branch and keeps both sides' methods and tests; never overwrite the other plan's client methods. `PLAN-52`, `PLAN-49` and `PLAN-64` landed since this spec was written (`OverviewView.svelte`, the attention store and the route model changed): re-read `client.ts` and the address helpers (`?tab=changes`) at HEAD.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-164-web-plan-changes-tab.md
```

## Folded from the PLAN-162 landing 2026-11-15

`PLAN-162` landed as `6b97acd2` (#444; `landings/PLAN-162.md`). Operator decision 2026-11-15: its residue is folded into `PLAN-163` (D0: keys, fixtures, contract,
file route) and this plan (D0 below). Read each point at the then-current HEAD before the outline.

**D0 — the mock shows a first capture** (persona `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:junit-core`):
- `applyCapturedChange` (`MockRuntimeEngine.java:7897-7922`) refuses a capture for a plan whose answer is not `PlanChangesCaptured` and any size that differs from the
  fixture's files, so only the head moves: "the tab appears on the first capture" (`not_captured` to captured) and "captured to unavailable" cannot be
  exercised, and neither can a changed file list or size. Extend the mock so a `plan.change_captured` announcement carries (or names) the new capture, moves
  `not_captured` to captured, may move a captured plan to `unavailable`, and changes files and size coherently with the contract rules `PLAN-163` D0 adds; a
  scheduled `plan.change_captured` in a scenario is validated by the loader (today only `chat.*` payloads are, `ScenarioLoader.java:597`) and the README
  states the new limit. Each by a scenario test that fails on a revert, with the matched positive control. Evidence order: `test-first`.
- The design's Live bullet (an open list is read again on `plan.change_captured`, `plan-view.adoc` § Live) is then testable beyond a head change.

Leads for D1 and D2, from the landing's reading:
- **The `tab=` hypothesis looks refuted:** `PlanView.svelte` has no tab concept at HEAD (a grep for `tab=` finds only the token table). Settle it at the outline; if
  there is no tab, the tab's address belongs to `PLAN-55`/`PLAN-143`'s helpers, and say which.
- The TS `ActivityEntry` and the live `toActivityEntry` (`PlanView.svelte:~332-352`) copy fixed fields and drop `launch_model` and `served_model`: D2 extends both paths;
  the word `state.activity.model_differs` already exists in the common pack.
- The head's figures come from `ProcessTree.change` or the event, never summed on the client (`plan-view.adoc:170`).
- Strip more than ESC from a path or a line (U+0085, U+2028 and bidirectional controls reach the wire as UTF-8). Render a file's hunks only from fixtures `PLAN-163`
  D0 made coherent; do not build on the pre-D0 hunk headers.
- `planId` and the file path are encoded as URL segments by `PlanChangesRoutes` (`URLEncoder` plus `%20`); the client encodes the same way, and a test holds an id and a
  path with `/`, `.`, a space and a non-ASCII character.

Surface: the Expected Surface above gains the three D0 files (12 entries).

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

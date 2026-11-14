# PLAN-163: A Plan's Changes in the Terminal

epic: ui-improvements
workstream: WS-05

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's decision of 2026-11-14
> ("Stage plan-changes rows", cleanup of the parked gates) from the work package `doc/plans/plan-changes.adoc` (index status `open`). This spec is SELF-SUFFICIENT.

## Objective

The terminal shows a plan's changes. The full-screen plan view gets a level on `d`, a file's hunks open on `Enter`, and there is the
one-shot form `plan <plan_id> --changes [--file <path>]`. A worker's activity shows `model_differs`. This covers task 2 of the work package
and the terminal side of task 4.

## Deliverables

Read first:
- `../plan-marshall-documentation/doc/specification/runtime-model/01-processes-and-transport.adoc` § `plan-changes` (The Plan's Changes): the list, a file, read-never-run, untrusted text, `not_captured`, `unavailable`, live.
- `../plan-marshall-documentation/doc/specification/runtime-model/06-event-catalogue.adoc:68`: `plan.change_captured` {`plan_id`, `head`, `files_total`, `insertions_total`, `deletions_total`, the unplanned count}.
- `doc/design/shared/plan-view.adoc` `#changes` (:172) and `#worker`.
- `doc/design/terminal-client/16-plan-view.adoc` `#changes` (:111).
- `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java`, `planview/PlanViewLevel.java`, `planview/WorkerActivityLevel.java`.
- The PLAN-162 contract (`pm-api/.../api/changes/`).

1. **The changes level and the one-shot form**
 - What:
   - Add a changes level reached on `d` from the plan view, listing files with status, counts, renamed-from, and the binary and unplanned marks. `Enter` on a file shows its hunks.
   - Add `plan <plan_id> --changes [--file <path>]`.
   - `not_captured` and `unavailable` print their catalogue text, never an empty list.
   - Paths and lines go through the terminal's control-sequence stripping.
   - Catalogue keys go in `operator.*.properties` (en/de).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when:
   - Against the mock: the list and a file print as designed, a planted control sequence in a path and in a line prints removed, and `not_captured` prints its text. Each case is in `PlanCommandTest` or a new `ChangesLevelTest` and fails on a revert. Evidence order: `test-first`.
   - A `ScreenCaptureIT` capture of the level at 80 columns (`pm-operator/src/test/resources/screen-captures/PLAN-163.txt`).

2. **`model_differs` in the worker's activity**
 - What: `WorkerActivityLevel` shows both models, while the head keeps the launch model.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-java:java-core, pm-dev-java-cui:cui-testing
 - Done when: `WorkerActivityLevelTest` pins both models and the head's launch model against the PLAN-162 fixture, and fails on a revert.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`). At the head, `./mvnw verify -Ppre-commit` passes (every rewrite reviewed and committed), and so does `./mvnw -pl pm-operator verify -Pintegration-tests`.
- No new dependency or plugin. In particular, NO diff library: the hunks are served data, and the mock serves them from fixtures.
- Web standards of `CLAUDE.md`. Every changed line and every path is untrusted text: render it through a text sink only, never `{@html}`/`innerHTML`.
- Java: JUnit 5, input from `Generators` with `@EnableGeneratorController`, no Mockito; expected wire and catalogue text stays literal. Never catch or throw generic `Exception`/`RuntimeException`.
- Ports of this plan: per `AGENTS.md` § Ports (`20000 + 10 × 163`): mock web port 21630, app port 21631. Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item answered on the pull request, read from GitHub's thread state, never from a count kept in memory.
  - **Every reply answers its own finding.** Bind each reply to the finding's anchor. A bot's follow-up "not marking as addressed" means the finding is still open (lesson `2026-11-14-14-007`).
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings: 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`). This is a merge gate.
- The landing verdict is given per Done-when clause, never per deliverable (lesson `2026-11-14-14-001`).

## Claim Labels

- OBSERVED: `planview/` holds `PlanViewLevel`, `PlanCompletionLevel` and `WorkerActivityLevel`, and no changes level — read at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (planview/ has no changes level; PlanCommand has no --changes)
- OBSERVED: `PlanCommand.java` has no `--changes` option — `git grep -n "changes" PlanCommand.java` at `7adb56c2`
  - verdict: corroborated | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: read at origin/main 7adb56c2 while staging (planview/ has no changes level; PlanCommand has no --changes)
- HYPOTHESIS: `d` is free in the plan view's key map — confirm/refute at `PlanViewLevel.java` and `16-plan-view.adoc` (verify-at-outline)
  - verdict: unverifiable | checked_at: 7adb56c2 | by: ui-improvements/cleanup | rescoped: n/a | evidence: HYPOTHESIS, verify-at-outline

## Expected Surface

- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/PlanCommand.java` — present at `7adb56c2`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/PlanViewLevel.java` — present at `7adb56c2`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/WorkerActivityLevel.java` — present at `7adb56c2`
- OBSERVED: `pm-operator/src/main/java/de/planmarshall/operator/planview/ChangesLevel.java` — absent; created by this plan
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/PlanCommandTest.java` — present at `7adb56c2`
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/planview/WorkerActivityLevelTest.java` — present at `7adb56c2`
- OBSERVED: `pm-operator/src/test/resources/screen-captures/PLAN-163.txt` — absent; created by this plan
- OBSERVED: `pm-api/src/main/resources/messages/operator.en.properties` — present at `7adb56c2`
- OBSERVED: `pm-api/src/main/resources/messages/operator.de.properties` — present at `7adb56c2`

## Dependencies and Sequencing

- Depends on: `PLAN-162`.
- Overlaps with:
  - `PLAN-30` (running): `operator.*.properties`. Start after it lands, or the second rebases.
  - `PLAN-44`/`PLAN-48` (staged, terminal): `PlanCommand` only if they touch it; check at outline.
- Disjoint from `PLAN-164` (web).

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check that PLAN-162 shipped the contract as specified.
- Settle the HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-163-cli-plan-changes.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

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
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: planview/ holds PlanViewLevel PlanCompletionLevel WorkerActivityLevel PlanViewFormatting and no changes level
- OBSERVED: `PlanCommand.java` has no `--changes` option — `git grep -n "changes" PlanCommand.java` at `7adb56c2`
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: PlanCommand.java has no --changes option; only --local-changes :520 (start answers) and the settings text :1253
- HYPOTHESIS: `d` is free in the plan view's key map — confirm/refute at `PlanViewLevel.java` and `16-plan-view.adoc` (verify-at-outline)
  - verdict: corroborated | checked_at: 71f84bb | by: ui-improvements/cleanup | rescoped: n/a | evidence: d is unbound in the plan view: TuiKeyRegistry.planNavigation :716-750 binds no d; design 16-plan-view.adoc:114 and :149 already assign d to the changes level

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
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.en.properties` — present at `6b97acd2` (D0: the six `plan.changes.*` keys)
- OBSERVED: `pm-api/src/main/resources/messages/plan_view.de.properties` — present at `6b97acd2` (D0)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/message/PlanViewCatalogueTest.java` — present at `6b97acd2` (D0: derives its list from the design)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/changes/PlanChangeFile.java` — present at `6b97acd2` (D0: coherence rules)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/changes/PlanChangedFile.java` — present at `6b97acd2` (D0)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/changes/PlanChangeHunk.java` — present at `6b97acd2` (D0)
- OBSERVED: `pm-api/src/main/java/de/planmarshall/api/changes/PlanChanges.java` — present at `6b97acd2` (D0: the `fromJson` null case)
- OBSERVED: `pm-api/src/test/java/de/planmarshall/api/changes/PlanChangesContractTest.java` — present at `6b97acd2` (D0)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — present at `6b97acd2` (D0: the file route's refusals)
- OBSERVED: `pm-mock-runtime/src/main/java/de/planmarshall/mock/scenario/ScenarioLoader.java` — present at `6b97acd2` (D0: loader checks)
- OBSERVED: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` — present at `6b97acd2` (D0: the fixtures' hunks, counts and order)
- OBSERVED: `pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanChangesScenarioTest.java` — present at `6b97acd2` (D0)

## Dependencies and Sequencing

- Depends on: `PLAN-162`.
- Overlaps with:
  - `PLAN-30` (running): `operator.*.properties`. Start after it lands, or the second rebases.
  - `PLAN-44`/`PLAN-48` (staged, terminal): `PlanCommand` only if they touch it; check at outline.
- **Before `PLAN-164`** (operator decision 2026-11-15: the residue of `PLAN-162` is folded into 163 and 164; the shared keys, fixtures and contract go
  here as D0, so `PLAN-164` starts after this plan lands). No longer disjoint from it: see "Folded from the PLAN-162 landing" below.
- Overlaps with `PLAN-146` (D0 of that plan) on `MockRuntimeEngine.java` and `running-work.json`; `PLAN-30` on `operator.*.properties`, `PlanCommand.java` and
  the mock engine. A multi-file overlap is the operator's decision at emit; the second to land rebases.

## Re-Grounding at Restart

- Re-read every file above at the then-current HEAD.
- Check that PLAN-162 shipped the contract as specified.
- Settle the HYPOTHESIS.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-163-cli-plan-changes.md
```

## Folded from the PLAN-162 landing 2026-11-15

`PLAN-162` landed as `6b97acd2` (#444; `landings/PLAN-162.md`). Operator decision 2026-11-15: its residue is folded into this plan and `PLAN-164`, not staged
as a plan of its own. The shared part is this plan's **D0**, done first; D1 and D2 above are reconciled with it as the notes say. Read each point at the
then-current HEAD before the outline: the landing's reading was by reading, no run.

**D0 — the residue of the changes contract and its fixtures** (persona `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`,
`pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`):
- **The six keys.** `plan.changes.{title,unplanned,binary,truncated,not_captured,unavailable}` (design `plan-view.adoc:249-262`) are in no pack: add them, English
  and German, to the **`plan_view`** pack (one owner and one pack; the web tab reads the same pack), and make `PlanViewCatalogueTest` derive its key list
  from the design document as `ChatTextsCatalogueTest` does (`Files.readAllLines` and a regex; no dependency, so the lane's reason for not taking
  CodeRabbit's finding was wrong). The keys the CLI shows stay `operator.*` only where the design says so.
- **The fixtures.** `running-work.json` (`fix-relay`): each hunk's `old_len` and `new_len` agree with its lines (the first hunk of `PlanChanges.java` has `old_len` 0
  beside a context line), each file's `added` and `removed` agree with its hunks (`plan-changes.ts` says 0 and 0 against two added and one removed line), the files
  stand in path order, the totals stay 12 files, +340 −27, 2 unplanned. The loader (`ScenarioLoader.validatePlanChanges`) refuses each of those contradictions,
  checks that content paths are in the list and that every non-binary file has contents, refuses a duplicate `plan_changes` plan id, and checks `ChangeSize`
  for a `not_captured` or `unavailable` plan. Evidence order: `test-first` for each refusal, with the matched positive control.
- **Coherence on decode.** The records refuse: `renamed` without `renamed_from` and `renamed_from` on a file that is not renamed; a `binary` file with counts or
  hunks; `truncated` without `lines_omitted` and `lines_omitted` above 0 with `truncated: false`; a duplicate path in one list; a missing or non-boolean
  `binary`, `unplanned` or `truncated` (today `Boolean.TRUE.equals` reads it as `false`). `PlanChanges.fromJson` answers an `IOException`, not an NPE, for a
  non-object JSON. Each by a contract test with the literal wire; each fails on a revert.
- **The file route.** Its refusals agree with the list route (an unknown plan answers `scope_not_found` on the list route and `target_not_found` on the file route today) and a file answer names the
  `head` it belongs to, or the spec says why not (a client cannot otherwise tell which capture a read file came from after a capture). Decide at the outline
  and state it; the design (`plan-view.adoc` § Live) re-reads an open file only when its row changed.
- **`PlanCommand.printActivity`** (`PlanCommand.java:276-303`): a `model_differs` entry prints its kind and an empty content, both models lost, because the `default`
  branch takes it. This plan's own D2 ("`WorkerActivityLevel` shows both models") was **already done** by `PLAN-162` (`WorkerActivityLevel.java:162`, pinned by a test
  on inline JSON): rescope D2 to `PlanCommand.printActivity` and the activity read from the fixture.
- **The head's figures** come from `ProcessTree.change` or the event, never summed on the client (`plan-view.adoc:170`); `PlanChanges.totals()` is a
  contract accessor, not a client rule. `--file` meets a 404 for `not_captured` and `unavailable`: show the design's texts, not a transport error.
- **Untrusted text:** strip more than ESC when a path or a line is printed (U+0085, U+2028 and bidirectional controls reach the wire as UTF-8).
- Two pins the landing left open: the literal wire of `PlanChangeCapturedEvent.toMap`, and the file route's refusals.

Surface: the Expected Surface above gains the D0 files (13 entries). `PLAN-163` is no longer a two-deliverable plan: D0 is its first and largest; check the
scope-bloat guard (about six deliverables) at the outline and split D0 out if the outline grows past it.

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/`
other than its own `inbox/{sender}-{seq}` message. The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md`
§ Ledger Write-Boundary).

# PLAN-183 landing record

epic: ui-improvements
plan: rb-183-pm-api-and-mock-hygiene-residue (runbook lane, harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2183; `total_tokens` and `plugin_version` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #553, squash `1c646489` (6 files, +563/-42), merge queue, 2026-11-24
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); findings 2, 4, 5, 6, 7, 8, 9, 11 re-read by the orchestrator at `1c646489`; findings 3, 10, 12, 13 are the leaf's reading, not re-read

Message `rb-183-pm-api-and-mock-hygiene-residue-001` (revision 0): `merge_state=merged`, `deliverables_done=2` of 2, 0 unresolved threads, 13 of 13 review-body findings handled, `sonar_open_new_issues=0`, `self-review:budget-exhausted` (three rounds, no fourth). `landing-check`: `complete: false`, missing `total_tokens` and `plugin_version`. Three operator questions, all "merge on the recorded evidence / merge this head" for heads `0b121ac3`, `64eb8984`, `2e966088`.

**Review numbers re-read** (`review-gate --pr-number 553`, 2026-11-24T11:39Z, after the merge): 0 unresolved threads, 13 review-body findings — equal to the message. `review_body_findings_handled=13` **not re-verified**: the progress file carries no `finding:` lines, only the narrative "13/13 body findings". CodeRabbit reviewed `dc3608e0`, `94a50d19`, `2daee400` (the merge rule's one-review requirement holds); the final head `2e966088` is a base merge.

**Sonar open new issues: 0.** `sonar_rest gate-status --pr 553`: OK (new coverage 91.8, duplication 0.0, hotspots 100 %). `read_open_issues --pr 553`: `undecidable` (`head_revision_unavailable`). `sonar_rest search --pr 553` lists six issues of any status (four `java:S1192`, one `java:S3824` in `MockRuntimeEngine.java`, one `java:S1612` in `ControlScenarioTest.java`); by reading, the merged diff adds no duplicated `"paused"`/`"abandoned"` literal (`ACTIVITY_PAUSED` :546, `STATUS_ABANDONED` :561 are the only occurrences), consistent with the lane's 0 at `2e966088`.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 phase state is the closed vocabulary | met: reader refuses an off-set value (`EpicProcessPlan.java:80-87`, `JsonFormatException`), the constructor through `PlanActivity.parse` (:66-71); the mock's `planStateOf` serves `ended` for a landed row |
| D2 epic `cancel` and `next` do what they answer | met in the Done-when's terms: both `with_plans` arms, `next` starts the first launchable row with `plan.resumed` or answers `not_started` per row |
| Premises at the base | sound: every line reference held at `b8874772` |
| Weak point | the reader-validation decision was to be taken "from the engine's contract"; the lane did not open an engine catalogue and the leaf found no engine text naming exactly the seven states (not shown by reading). One off-set `phase_state` now fails the whole `EpicProcess` read. Every shipped scenario's values are in the set |
| Spec inconsistency | D1 says `phaseNodes`/`phaseNodeState` "stay", yet `planStateOf` serving `ended` forced them to treat `ended` as done (`planEnded(String)` :12554), a change the spec did not name |

True count by reading: 2 of 2.

## Layer 2: implemented

Both deliverables trace to code and to tests that can fail (`ProcessRecordsReadingTest` reads every `PlanActivity` value and refuses `done` on reader and constructor; `ControlScenarioTest` cancel both arms, `nextPlan`, `endedPlanIsNotStarted`). Gaps:

- the capacity branch of `next` (`countRunning < scope`, :6118) and the rule "paused/pausing count as running" have no test that fails when removed: the second `next` in `nextPlan` answers `not_started` for dependency/overlap, not for scope (leaf reading);
- `planStateOf` (:12444-12450): only shipped→ended and waiting-for-you are tested; completed, landed, returned, abandoned, cancelled, handed back have no failing test;
- the blank `phase_state` branch (:68, :81) is untested;
- hunks no deliverable explains: the control seed keeps the resource's scope instead of defaulting an omitted epic scope to 0 (:4048-4052, unreported, untested) and `EPIC_FLAT_ACTIONS` gains `cancel` (:383, the flat `/epics/{id}/cancel` the CLI posts to); the epic resource now overlays `state` and `workstreams` for every epic (:6496-6497);
- `PlanActivity.java` and `PlanActionRequest.java` are in the Expected Surface and untouched (fine, except the stale Javadoc, L3-4).

Report claims not borne out: "off-vocabulary row statuses mapped" (the `default` arm passes any other status through, :12450); "cancel brings a row in line with a plan that ended by its own act" (no such code, L3-1); "the epic resource carries the count of its running rows" and "nextPlan asserts the epic's running count" (the overlay sets only `state` and `workstreams`; `running` stays the fixture's value).

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where | in lane §9 |
|---|---|---|---|---|
| L3-1 | **major** | an epic `cancel` with `with_plans: true` skips a plan that ended by its own act (`planEnded` is true from `controlPlanStates`), so its row keeps `running`/`staged`/`paused` under an ended epic — against the method's own Javadoc ("an ended epic lists no row still to run"). Trigger: plan cancel on `relay-cursor-order`, then epic cancel on `relay-hardening`. Re-read. | `MockRuntimeEngine.java:6052-6058,6077-6081` | yes ("a plan that ended by its own act"), but the report's Changes text claims the opposite |
| L3-2 | minor | the epic cancel clears a held plan's holds with `removeHolds` and publishes no `hold.cleared`, while `endPlanByHold` publishes `clearHoldsOf` results; the plan-level cancel (:5960) has the same gap. Re-read. | `:6061`, `:5905`, `:6381` | no |
| L3-3 | minor | `next` on an epic with a control state but no resource answers 200 with neither `started_plan` nor `not_started`. No shipped scenario reaches it. Re-read. | `:6110-6114` | no |
| L3-4 | minor | `PlanActivity` Javadoc still says `STAGED` and `ENDED` are "the two values the plan's `phase_state` also carries". Re-read. | `PlanActivity.java:15-18` | no |
| L3-5 | minor | the old vocabulary survives: `planEnded(String)` and `revisionWindowOpen` still accept `done`; `planStateOf`'s `default` passes unknown statuses into `phase_state`; `ProcessTree.phaseState` stays a free string. Re-read (`ProcessTree` partly in lane §9). | `:12450`, `:12554-12556`, `:1132`, `ProcessTree.java:157` | partly |
| L3-6 | minor | `next` publishes only `plan.resumed`; the epic cancel only per-plan `plan.cancelled`. The runtime-wide stream's plan-started and epic-row-status events (`01-processes-and-transport.adoc:374`) are not modelled. Leaf reading. | `:6137` | no |
| L3-7 | info | `raise_scope` is read and never consulted (full scope: 200, nothing started); a cancelled epic carries no `terminal_reason` (`EpicTerminalReason.CANCELLED` exists); `stale` is not distinguished among the `not_started` reasons. | `:6496`, `PlanActionRequest` | yes (first two) |

Conformance: Java 25, exception rules, logging, JUnit 5, Generators and `@EnableGeneratorController`, public-type docs, wire values: conforms. "No change beyond what a deliverable names" and "a fix that changes another behaviour is reported, not made": **deviates** (the seed-scope default and the flat cancel route, unreported). Test-first order and Sonar: not shown by reading (the lane's run logs; Sonar read above).

Lane §9 checked: `raise_scope` confirmed; epic `running` count vs `next` rule confirmed; ended-by-own-act row confirmed (L3-1); half-covered plan reads partly refuted (`withPlanOverlay` :1311-1313 shows `activity=ended` on the process read after `plan.cancelled`; only `phase_state`, the plan list, the epic collection and `running_plans_count` stay unoverlaid); `ProcessTree#phaseState` confirmed; no `terminal_reason` confirmed; `state.plan.done`/`state.plan.planned` have no source reference outside the properties files (dynamic key construction not checked).

## Residue for the plans that touch the same surface

- The mock's epic and plan control (L3-1, L3-2, L3-3, L3-6, `raise_scope`, `terminal_reason`, the running-count overlay): no staged plan carries `MockRuntimeEngine.java`'s control routes; a fix plan only on the operator's word.
- The vocabulary leftovers (L3-4, L3-5) and the untested branches (capacity, `planStateOf` arms, blank `phase_state`): fold candidates for the same fix plan.
- `-Ppre-commit` not idempotent (recurred: `Libc.java`, `PosixPlatform.java`, nine `pm-operator` files) and the `inbox_read_command` defect (recurred): provider-side defects already recorded.
- CI flake `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` (seed-dependent, passed on rerun) and one Maven Central HTTP 429: reported by the lane, not re-read.

# PLAN-186 landing record

epic: ui-improvements
plan: rb-186-sonar-standing-debt-pm-operator (runbook lane, harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2183; `total_tokens` and `plugin_version` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #556, squash `81e2c1fc` (47 files, +1404/-960, all `pm-operator/**/*.java`), merge queue, 2026-11-24
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); the Sonar reads, the `tooLate` annotation, the S106 suppression and the `WorkActionDialogue` literals re-read by the orchestrator at `81e2c1fc`

Message `rb-186-sonar-standing-debt-pm-operator-001` (revision 0): `merge_state=merged`, `deliverables_done=2` of 3 (D3 "not met": the zero read was `unknown` when the lane read it), 0 unresolved threads, 1 of 1 review-body findings handled (CodeRabbit inline `countsKey`, fixed in `cf3ffb28`), `sonar_open_new_issues=0`, `self-review: clear`. `landing-check`: `complete: false`, missing `total_tokens` and `plugin_version`. Two operator questions: the one `java:S106` finding, answered "suppress using a suppression comment", then the form selected: `@SuppressWarnings` on the method.

**Review numbers re-read** (`review-gate --pr-number 556`, after the merge): 0 unresolved threads, 1 review-body finding, equal to the message. `review_body_findings_handled` reads 0 there (`body_findings_undisposed`, coderabbit inline-1): the gate reads no disposition line after the merge; the message records `finding: coderabbit inline inline-1 — cf3ffb28`. Not re-verified beyond that.

**Sonar.** PR: `sonar_rest gate-status --pr 556` OK (new coverage 87.9, duplication 0.0, hotspots 100 %); new-code open issues 0 by the lane. **Default branch, read here after the merge** (`sonar read_open_issues --project plan-marshall_pm-mcp-clients`, `count_status: confirmed`; the compute engine shows a `main` analysis at 2026-11-24T13:01:56Z, after the merge at 12:46; `analysis_revision` empty): **9 open**, down from 113:

| Key | Rule | Where | Status |
|---|---|---|---|
| `AaFfVbmag_p_FqfrTT28` | `java:S3776` | `pm-operator/.../HoldDialogue.java:102` (`execute`) | **not fixed, not named** |
| `AaFfVbnQg_p_FqfrTT3J` | `java:S1192` | `pm-operator/.../WorkActionDialogue.java:180` (`"findings"`, re-read: still three literals, :180 and :500) | **not fixed, not named** |
| `AaFfVbnQg_p_FqfrTT3K` | `java:S1192` | `pm-operator/.../WorkActionDialogue.java:181` (`"watches"`, same) | **not fixed, not named** |
| `AaFfVblVg_p_FqfrTT2o` | `java:S3776` | `pm-operator/.../WorkChatDialogue.java:125` (`execute`) | **not fixed, not named** |
| `AaFfVblug_p_FqfrTT20` | `java:S3398` | `pm-operator/.../overview/OverviewFullScreenApp.java:1374` | **not fixed, not named** |
| `AaFMIQUlEHNTGUcqN-Y-` | `java:S1075` | `pm-operator/.../LessonSourceCommand.java:45` | named by the lane as residue; no operator selection |
| `AaFGfsYupdXNQ-N4ArY-` | `java:S1313` | `pm-mock-runtime/.../MockRuntimeEngine.java:11006` | PLAN-181, the operator's to dismiss by hand |
| `AaGRMDtGxhXuOwwhaS0K` | `java:S4449` | `pm-mock-runtime/.../MockRuntime.java:245` | PLAN-181, same |
| `AaEcTdY0MJNX7b6L9QAy` | `java:S112` | `pm-exec/.../NativeKernel.java:229` | PLAN-181, same |

The PR read could not show these five: a pull-request analysis reports issues on new code only, and four of the five sit on lines the diff did not rewrite (`HoldDialogue` and `WorkChatDialogue` were partly extracted, still over the threshold). The lane's "met" for D1's default-branch arm was a forecast ("the expected default-branch answer is zero"), not a read.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 production code | **not met** on its default-branch arm: 5 open issues under `pm-operator/src/main` with no key named and no operator selection, plus `S1075` named without a selection. The PR arm and the module build: met by the lane's account (not re-run) |
| D2 tests | met: no open issue under `pm-operator/src/test` on main; `Thread.sleep` gone (leaf reading); no `@Test` removed, no assertion loosened |
| D3 zero read | **not met**: the read is 9, not 0; the remaining keys are named here, not by the lane |
| Premises | hold at the base (counts, `PmOperatorProcessIT.java:222` `Thread.sleep(100)`, Awaitility at test scope) |
| Spec flaws | "no `@SuppressWarnings`" is not a norm of main (about 30 on main, e.g. `ConfigCommand.java:68` `java:S1075`); D1 "S106 put to the operator for dismissal" vs the constraint: settled by the operator's selection; an `S1075` URI literal that is already a constant can only be met by an operator selection, which the spec never said; the Expected Surface omits six compile-forced files |

True count by reading: 1 of 3 (D2).

## Layer 2: implemented

All 47 files are `pm-operator/**/*.java`: 32 production files with issues, `OperatorActions.java` and five test files compile-forced by the grouped `ActContext` / `EpicLabels` signatures (confirmed), nine test files with issues. `LessonSourceCommand.java` listed and untouched. Exactly one suppression added: `@SuppressWarnings("java:S106")` on `OverviewFullScreenApp.dialogueOut()` (`:1071`, re-read), with Javadoc naming the operator's selection; no `NOSONAR`, no exclusion. No resource and no screen capture changed. Behaviour preservation rests on existing tests only: no new test for the extracted `ActionSupport` end paths (exit 3, 130, 1, 0), the null `ActContext`, the derived `countsKey` or the extracted `PlanCommand`/`StatusCommand` helpers; whether the existing tests reach each new branch is not shown by reading. Lane claims refuted: D1 "met" (above). Confirmed: every other checked claim.

## Layer 3: correct (reported to the operator; no fix plan staged)

No behaviour drift found in the 33 production files (leaf reading: effect and output order of every extraction, early returns, unfolded ternaries, constant values, removed `throws`, `PseudoTerminal`, the Awaitility rewrite, `countsKey`, the `ActContext`/`EpicLabels` call sites).

| # | Severity | Finding | Where | in lane §9 |
|---|---|---|---|---|
| L3-1 | minor | `tooLate(@Nullable String state)` passes `state` to `Map.of`, which throws on null; the one caller checks `restoreState() != null` first, so no runtime effect; the annotation misstates the contract (an S4449 fix by annotation, not by a checked local). Re-read. | `PlanSettingsDialogue.java:176-177,201-202` | no |
| L3-2 | minor | `S1192` constants merge roles that only share a spelling: `StatusCommand` `RUNNING` (state word, row default, fallback, parameter name), `VERSION`; `WorkActionDialogue` `KEY_REASON`/`KEY_ACTION` (message parameter and JSON wire field); `ACTION_ABORT` (plan action and hold act); `OverviewFullScreenApp:129-135` (menu action and runtime argv); `EpicViewFormatting` `HELD` (status and wait reason). No drift now; a later change to one meaning moves the others. Leaf reading. | `StatusCommand.java:53-69` and others | no |
| L3-3 | info | `DirectFileAccess`, `OperatorStateDirectory`, `ReportsLevel`, `SystemLevel` became records: value equality where they had identity; whether a caller relies on identity is not shown by reading. | `DirectFileAccess.java:35` | no |

Conformance: Java 25, exception rules, logging, JUnit 5, Generators, public-type docs, `@NullMarked`, no dependency: conforms. "No `@SuppressWarnings`": deviates by the operator's selection (one, documented). Build, coverage, integration and pre-commit results: the lane's, not re-run.

## Residue for the plans that touch the same surface

- **The five unfixed `pm-operator` issues** (`HoldDialogue:102` S3776, `WorkActionDialogue:180/181` S1192, `WorkChatDialogue:125` S3776, `OverviewFullScreenApp:1374` S3398) and the `LessonSourceCommand:45` S1075 selection: the zero policy is not met on main. No staged plan carries them; a fix plan or a fold into the next `pm-operator` plan only on the operator's word. PLAN-42 (parked) touches `OverviewFullScreenApp.java`; the CLI chain (44, 48, 50, 63, 184, 138-140) edits `pm-operator` and is re-grounded against `81e2c1fc`.
- The three PLAN-181 keys remain the operator's to dismiss by hand.
- L3-1 and L3-2: fold candidates for the same plan.
- Provider-side defects recurring, already recorded: `-Ppre-commit` not idempotent; `inbox read --plan-id` refuses a digit-leading plan id; the `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` seed flake.

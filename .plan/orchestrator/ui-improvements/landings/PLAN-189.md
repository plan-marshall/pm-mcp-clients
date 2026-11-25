# PLAN-189 landing record

epic: ui-improvements
plan: rb-189-sonar-pm-operator-residue (runbook lane, harness antigravity, `model`/`effort`/`total_tokens` `unknown`, plugin 0.1.0, executor 0.1.2183; the fifth concurrent run on the operator's word of 2026-11-24)
pr: #571, squash `2b6939fe` (9 files, all Java under `pm-operator`), merged 2026-11-25T12:43:41Z
analysed: 2026-11-25, by sub-agent in three layers, by reading only (no build); `HoldDialogue.java:104-155`, `WorkActionDialogue.java:176-184,500-503`, `ReviewDialogue.java:42-43`, `OverviewFullScreenApp.java:1104-1108`, `PlanSettingsDialogue.java:174-204` and `LessonSourceCommand.java:41-44` re-read by the orchestrator at `2b6939fe`

Message `rb-189-sonar-pm-operator-residue-001` (revision 0, 2026-11-25T13:28:48Z): `merge_state=merged`, `deliverables_done=2` of 2, 0 unresolved threads, 1 of 1 review-body finding handled, `sonar_open_new_issues=0`, quality gate ok, `self-review: clear`. The lane's own default-branch read (D2) is the same as the orchestrator's: task `AaIS4S1WZJZ1WS7XxNfv`, 3 open, by module `pm-operator` 0, `pm-mock-runtime` 2, `pm-exec` 1. `landing-check`: `complete: false`, missing `total_tokens`, `model`, `effort` (the 2026-10-27 Open Defect; antigravity). The message gives the merge as 13:01Z; the merge commit's time is 12:43:41Z; the task is later than both.

**Merge corroborated:** `ci pr view 571` reads `merged`, merge commit `2b6939fe3d3ad6f07b9cb77af81e895d0d979f0c`, the first commit on `origin/main` after the ledger land #577. The lane's progress record ends `merge: merged …` and `cleanup: done`.

**Review numbers re-read** (`runbook_lane review-gate --pr-number 571`, 2026-11-25T13:07Z, after the merge): 0 unresolved threads, 1 review-body finding, 0 handled, `merge_allowed: false`, reason `body_findings_undisposed` (`coderabbit, inline, inline-1`). The lane's record: CodeRabbit reviewed `70902cd8` (third trigger, after two quota refusals) with one inline thread on `WorkChatDialogueTest.java:136`, fixed in `668e2156`; the thread is resolved. The gate counts the summary's inline item as an undisposed body finding: the disposition was not posted as a body answer. The lane's gate read at `0749216c` before the merge was `merge_allowed: true, reason: all_handled` (message § 5); the post-merge re-read cannot see that disposition. The same shows for PLAN-182 (#569, 11 of 11 by the message, 0 handled on the re-read): a read-side artefact of the re-read after cleanup, not a merge against the rule.

**Sonar.** PR: 0 new issues, gate OK (`new_coverage` 87.6 %), the lane's read at `0749216c` (`analysis_revision = head_revision`). Default branch, read by the orchestrator 2026-11-25 after the `main` compute-engine task `AaIS4S1WZJZ1WS7XxNfv` (submitted 13:24:36Z, SUCCESS), which is later than the merge (12:43:41Z): quality gate per the read `confirmed`, **3 open** — exactly the three PLAN-181 keys `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`, `MockRuntimeEngine.java:11006`), `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`, `MockRuntime.java:245`), `AaEcTdY0MJNX7b6L9QAy` (`java:S112`, `NativeKernel.java:229`), which the operator dismisses by hand. None of the six `pm-operator` keys is open, so D1's Sonar Done-when and D2's target hold. The read names no analysed revision; that the task covers `2b6939fe` is inferred from its time (the only `main` task after the merge), not shown by the read.

**SNAPSHOT deploy of `2b6939fe`:** `unknown` (the `ci` surface has no commit-scoped run reader).

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 `HoldDialogue.execute` S3776 | met by reading: `execute` keeps the loop and delegates to `preselectedActFor` and `applyAct` (`StepOutcome` record, `:154`); about 8 by the sub-agent's count. The Sonar clear is not shown by reading |
| D1 `WorkChatDialogue.execute` S3776 | met by reading: `showTranscript`, `askMessage`, `reportNoProposalOnce` extracted |
| D1 `WorkActionDialogue` S1192 ×2 | met in effect: the register argument uses `ReviewDialogue.REGISTER_FINDINGS`/`REGISTER_WATCHES` (widened private → package-private, same values, the one register meaning); the case labels and the message-parameter keys at `:502` stay literals, two occurrences each, below the rule's threshold. The message-parameter role stays separate, as the spec asks |
| D1 `openWhatWaits` S3398 | met: `openWhatWaits` and its 7 wait-only helpers moved into `OverviewKeyHandler` as a pure relocation (token-identical bodies); `openActions` widened private → package-private (`:1106`) so the move raises no new S3398, reported in the message. Behaviour-neutral |
| D1 S1075 `LessonSourceCommand` | met per the operator's selection: class-level `@SuppressWarnings("java:S1075")` (`:43`), as `AcceptEditCommand:66`, `ConfigCommand:68`, `CredentialsCommand:55` |
| D1 nullness `PlanSettingsDialogue.tooLate` | met: the parameter is non-null, fed from the null-checked local `restoreState` (`:176-178`) |
| D1 Done-when (b) builds | the lane's runs (full verify at `64861fb8` and `0749216c`, IT `-pl pm-operator -am` at `0749216c`, 1107 tests); "no fewer tests" not shown by reading. `git diff` names no resource or screen-capture file (all 9 files are Java): met |
| D1 Done-when (c) tests named | the reaching tests exist (`ControlDialoguesTest` hold outcomes, `WorkChatDialogueTest` paths); added: `workActionWatches` (asserts `register=watches`), `noProposalNoticePrintedOnceAcrossTurns` |
| D2 default-branch read | see Sonar above |

True count: 2 of 2 (D1 by reading and the default-branch read; D2 by the lane's read, equal to the orchestrator's).

## Layer 2: implemented

Every one of the 9 files traces to a D1 row or the nullness fix; nothing unexplained. The PR narrative claims "Added `ACTION_FINDINGS` / `ACTION_WATCHES` in `WorkActionDialogue` for the switch cases": the merged code has no such constants (refuted by reading). The lane's record explains it: added, then removed in `64861fb8` to clear the two S1192 findings they raised on the PR. The narrative was not updated.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major, no behaviour change found: `HoldDialogue` and `WorkChatDialogue` are branch-for-branch equal to the old flow (same order of effects, same output lines, same exit codes, the once-only notice kept); the `OverviewKeyHandler` move resolves every name to the outer instance as before.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | info | `openActions` widened to package-private so the move raises no new S3398; its callers are now all in `OverviewKeyHandler`. Reported in the message (Hypotheses), not in the PR body | `OverviewFullScreenApp.java:1106` |
| L3-2 | minor | the PR narrative's `ACTION_FINDINGS`/`ACTION_WATCHES` claim is stale (see Layer 2) | PR #571 body |
| L3-3 | minor | no test fails if the `findings` arm sends `REGISTER_WATCHES`: `workActionFindings` asserts only the output text, while the added `workActionWatches` asserts the register | `ControlDialoguesTest.java:1034` |
| L3-4 | info | the nullness is not machine-checked (no NullAway/ErrorProne configured) | `pm-operator` |

Conformance: no generic exception, record and package-private, JUnit 5 with `Generators` and `@EnableGeneratorController`, no `NOSONAR`, the only suppression the operator's selection, no resource change, no dependency: conforms. S3398 "move or report why not": met with the `openActions` widening reported in the message (L3-1).

## Residue for the plans that touch the same surface

- **The parked PLAN-42** edits `OverviewFullScreenApp.java`; `openWhatWaits` and its helpers now live in `OverviewKeyHandler` (`:2386-2543`). Its branch rebases onto `2b6939fe` when it resumes.
- **The CLI chain** (`PLAN-44`, `48`, `50`, `63`, `184`, `138`–`140`) is re-grounded after this landing, as PLAN-189's spec says.
- **Mailbox address for a digit-led plan id:** the lane's three mailbox reads refused `invalid_target_plan` (the PLAN-188 recurrence; a plan-marshall tool defect).

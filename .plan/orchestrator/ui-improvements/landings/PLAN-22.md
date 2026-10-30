# Landing Analysis: PLAN-22 — Terminal: One State Dialogue for Both Onboardings

epic: ui-improvements
workstream: WS-04
pr: #275 (merge commit `fa178decabe030714a11b2388e30485cd7020cb9`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-22-cli-state-dialogue-onboarding-001.md` (runbook
> lane, harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-30 against the
> merged PR, the CI checks, the merge diff and the Sonar gate. The terminal capture sets, the browser snapshots and the
> three self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 `StateDialogue` (one dialogue over an operator state: frame, labelled facts, strict prompt) | shipped-as-specified, order departs | `dialogue/StateDialogue.java` (766 lines), `OnboardingSource`, `ProjectOnboardingSource` (new) and `StateDialogueTest` drive three states with a fake API. The spec named `test-first`; the run records `mutation-after`: the class is new, so the test landed with it and the failing run came from reverting a committed fix (`StateDialogueTest.drivesThreeStates:108`). The before/after capture set has 24 captures (120×30 and 80×24, `en_US` and `de_DE`). |
| D2 Machine onboarding (`OnboardingDialogueIT` enabled; guided path to `onboard.completed`; `--auto --yes` to the result table) | met, order departs | The pre-existing `@Disabled` IT was enabled and made green (`mainPathCompletes`, `automaticPathPrintsTheResult`, `qFirstSendsNothing`, `unknownInputReAsks`). No failing run was captured before the fix, so the spec's `test-first` note is not evidenced. `OnboardCommand` and `EntryFlow` rewritten. |
| D3 Project onboarding (`ProjectSetupDialogueIT` enabled; id, name, pilot) | met, order departs | Enabled and green (`identityShowsIdNameAndPilot`); same order note as D2. |
| D4 Catalogue completeness (a removed key fails the test) | shipped-as-specified | `OnboardingCatalogueTest` new: `everyLinkHasALabel` failed on a missing `harness.consent -> remember` key before the key was added (`test-first`), `everyReachableStateHasATitle` plus a non-vacuity guard. |
| D4 browser regression (`fully-onboarded`, `running-work`) | met without a baseline | 32 snapshots under `.plan/temp/playwright-mcp/`, after set only; the plan changes no web file, so a before set would be identical by construction. |
| D4 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`, `analysis_revision == head_revision` at `489e0204`; corroborated below. |

`deliverables_done=4` of 4 is consistent with the diff. The run re-grounded one hypothesis against the base: the
spec's "`EntryFlow` asks 'Begin with step 1?' and ignores the answer" and "`OnboardCommand` prints raw links" were
both confirmed at the base before the rewrite.

**Surface delta** (computed here from `git diff fa178dec^1 fa178dec`, because `inbox landing-check` answers
`unmeasured`): declared 13 entries, realized 28 files. Twelve declared entries were changed: `StateDialogue`,
`EntryFlow`, `OnboardCommand`, `ProjectCommand`, `StateDialogueTest`, `OnboardingDialogueIT`,
`ProjectSetupDialogueIT`, `OnboardCommandTest` and the four catalogue files `onboarding.{en,de}` and
`projects.{en,de}`. One declared entry is untouched: the `projects/` directory. Sixteen files are undeclared:
- the catalogue and the documents: `operator.{en,de}.properties` (three keys removed in each),
  `05-transcripts-entry.adoc`, `13-texts-operator.adoc`;
- new operator code and tests: `OnboardingSource`, `ProjectOnboardingSource`, `DialogueSourceTest`,
  `OnboardingCatalogueTest`, `ProjectCommandTest`;
- changed operator code and tests: `PmConsole`, `PmConsoleTest`, `EntryFlowTest`;
- the mock: `MockRuntimeEngine` and `MockRuntimeEngineTest`;
- the captures: `screen-captures/22-cli-state-dialogue-onboarding.txt` and `transcript/console-settings.transcript`.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`; `executor-skew` answered `match`.
- Anomalies:
  - **Rebase over PLAN-13:** the run met a semantic merge conflict with `main`'s new badge model (the PLAN-13 change
    to `EntryFlow`, named in the PLAN-13 landing) and resolved it by the merge `3b89bf7`.
  - **Self-review budget exhausted:** three sub-agent rounds; round 1 had high-severity items (lists and decision
    values not rendered, runtime text bypassing `TerminalSanitizer`, wire values leaking), all fixed in `5fb168d` to
    `590d03d`; round 2 fixed raw ids and an unguarded fallback in `064122d` to `1d8cc50`; round 3 found two leftovers
    that were NOT fixed in the plan's PR but moved to follow-up PR #276 (below). `self-review: budget-exhausted`.
  - **Shell reads and edits against the rules, by the run itself:** `grep`, `ls`, `head`, `sed` and `perl -pi` in shell
    calls several times, `rm -rf`, `mkdir -p` and `printf >>` for temp paths and the progress file, and
    `rm -rf pm-operator/.plan` in the worktree. The before captures came from a detached temp worktree added with
    `git worktree add`, not the lane's worktree verb. Guard `none` on this harness, so the count is the run's own.
  - **Build routing:** the first routed build failed with `execution_failed` (the daemon's PATH has no `mvn`), so every
    build ran `in_process`; one gate run hit the daemon's learned 555 s bound after 2250 passed tests and was re-run
    with `--timeout 1200`. Matches the open Build-server watch (the PATH part is not reliable).
  - CI failures on the PR were Sonar new-code coverage (78.2 %, then an issue count) and the semantic merge conflict,
    not test failures; both fixed.
  - `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs); the run followed the ledger
    copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed head `489e0204` (`kind=reviewed`, merge risk Low): six
  findings and three nitpicks fixed or answered, every thread resolved; two final nitpicks answered as not applicable.
  Sourcery (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 275` is `success`, 20 checks, none failed. `origin/main` carries `fa178dec`
  as the parent of the PLAN-39 merge.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 275` answers `gate_status: OK`, six
  conditions OK (new coverage 84.1, duplicated lines 0.7, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-22 --status shipped`
- [x] row `pr` stamped `275`
- [x] row `landing` stamped `landings/PLAN-22.md`
- [x] row `plan_marshall_plan_id` stamped `22-cli-state-dialogue-onboarding` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-22-cli-state-dialogue-onboarding-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Addendum 2026-10-30: PR #276 merged** as `44803a77` through the merge queue, at the operator's word; the branch and the
  local worktree are gone. The paragraph below is the state at the time of the analysis.
- **Follow-up PR #276 was open** (`fix/22-cli-state-dialogue-followup`, state `open`, `mergeable`, merge state `clean`,
  checked through `ci pr view`). It carries the two round-3 fixes: the German `--auto` line repeating the runtime's
  English summary, and `onboard.auto.consent.title` and `onboard.auto.result.title` defined twice per locale in
  `onboarding.{en,de}.properties`. The run reports CI and Sonar green at `eebbeeb6` and CodeRabbit refused on quota
  twice. The plan's row is shipped; the fix is owed. It is not merged without the operator's word. Recorded as a Watch.
- **Running plans that now rebase over this merge:** PLAN-14 declares `PmConsole`, `PmConsoleTest` and
  `operator.{en,de}.properties` (the merge removed three keys from each); PLAN-20 declares `dialogue/` (the
  directory that now holds `StateDialogue` and the two sources) and the operator catalogue pair. Their lanes read the
  new `main` at the next merge from `main`; no mailbox note sent (offered).
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): PLAN-30 (`OnboardCommand`,
  `13-texts-operator.adoc`); the operator catalogue pair plans PLAN-17, 21, 23, 26, 30, 32, 34, 36,
  38, 42, 44, 48, 50; the `onboarding.*` plans PLAN-09, 64, 66, 68, 69; no staged spec declares
  `MockRuntimeEngine` (only shipped ones do); the capture-set plans (`screen-captures/`) PLAN-17, 21, 23, 26, 30, 32, 34, 36, 38, 42, 44, 48, 50.
  `PLAN-09` no longer overlaps a running plan on `onboarding.*`.
- **Disabled integration tests:** `OnboardingDialogueIT` and `ProjectSetupDialogueIT` are enabled. Three
  `@Disabled` dialogue ITs remain at `main` (grep over `transcript/`): `RuntimeStopDialogueIT` (PLAN-17),
  `AnswerDialogueIT` (PLAN-21) and `CleanupDialogueIT` (PLAN-23). The `pm-operator` coverage bundle stays below 80 %
  until they are enabled.
- **Residue (accepted):**
  - the browser check has no before set (no web file changed);
  - the automatic section of `05-transcripts-entry.adoc` still shows the richer transcript it described before this
    plan (the implemented output is one line per state plus the result table);
  - the step counters are constants (7 for the machine run, 10 for a project run) because the runtime state carries
    no total;
  - the 60 s give-up path has no clock seam.
- **Branch and worktree:** no `feature/22*` branch remains on the remote (`git ls-remote`, checked after this
  record). The follow-up branch above is the one that stays, with its open PR.

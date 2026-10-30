# Landing Analysis: PLAN-14 — Terminal: The Operator Gate on Acts Only

epic: ui-improvements
workstream: WS-05
pr: #277 (merge commit `5348e89ec4b5e9f2051093cd262cc28c5a0bf092`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-14-cli-gate-scope-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-30 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The 34 terminal captures, the per-head build logs and the three
> self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Gate on act paths (read-only verbs run under a host marker; act verbs refuse with exit `4`) | shipped-as-specified | `OperatorGateScopeTest` (new; 29 read rows, 65 act rows, console-dispatcher, entry-flow and view-client tests). `test-first`: failing run on `7e2b4b61` plus the uncommitted test, `Tests run: 73, Failures: 28`. `OperatorActs` (new) classifies the acts from the command tree; `gate().verify()` calls were removed from the read verbs and added to the act paths in the shell, the console and the full-screen views (`WorkActionsClient`, `OverviewFullScreenApp`). `PmOperatorProcessIT` runs the real binary under each host marker. |
| D1 terminal check | met, exit code not in the captures | 34 captures (120×30 and 80×24, `en_US`, `de_DE`) before and after. The harness records no exit status, so "exit `4`" rests on the two test classes, not on the captures. |
| D2 Honest refusal (catalogue text naming the set marker, `en` and `de`) | shipped-as-specified | `OperatorGateTest` keeps one marker matrix and asserts the exact English and German text; `OperatorGate` rewritten (+180 lines); `operator.{en,de}.properties` +4 lines each. `test-first` (same failing run). |
| D3 PTY tests say why; the pull request states the open decision | met | Comments naming `PM-SEC-3` in `PtySession` and `PseudoTerminal`; the PR body carries the open `PM_OPERATOR_DEV_ALLOW_AGENT` decision. |
| D3 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`; corroborated below (17 → 4 → 1 → 0 across the heads, each fixed in a pushed commit). |

`deliverables_done=3` of 3 is consistent with the diff. The spec's hypothesis (the client calls the gate unconditionally on
read verbs, `QuestionsCommand:54`, `ReportsCommand:65`, `ProjectsCommand:68`, 22 further sites) was confirmed before the
change.

**Departures decided with the operator:**
- **`plan <id> --print` was added.** The spec's terminal check names `plan fix-relay --print`, which does not exist at HEAD
  and belongs to PLAN-42. The operator chose to add a minimal flag (documented in `01-commands`, `16-plan-view` and the
  `pm-operator` README). PLAN-42 now has it for `plan`.
- **The whole `cleanup` verb stays gated.** The spec's check says bare `cleanup` must refuse, while `26-cleanup.adoc`
  and CLI-CC-P-05 called it a read. The operator chose to keep the verb gated and fix the documents (`cleanup`,
  `--kept` and `--release` are `[op]` now).
- **`drain` is not gated** (its own documented exception, written into `04-console`).

**Surface delta** (computed here from `git diff 5348e89e^1 5348e89e`, because `inbox landing-check` answers
`unmeasured`): declared 9 entries, realized 45 files. Eight declared entries were changed (`OperatorGate`, `PmOperator`,
`PmConsole`, `OperatorGateTest`, `PtySession`, the new `OperatorGateScopeTest`, `operator.{en,de}.properties`). One
declared file is untouched: `OperatorContext.java`. Thirty-seven files are undeclared:
- eighteen command and flow classes in `pm-operator/src/main` (`CleanupCommand`, `ConfigCommand`, `CredentialsCommand`,
  `EntryFlow`, `EpicCommand`, `LessonSourceCommand`, `OnboardCommand`, `PlanCommand`, `ProjectCommand`,
  `ProjectsCommand`, `QuestionsCommand`, `ReportsCommand`, `RolesCommand`, `RuntimeCommand`, `SettingsCommand`,
  `WebCommand`, `OperatorRefusedException`, `OperatorActs`);
- four more production files: `actions/WorkActionsClient`, `dialogue/OnboardingSource`,
  `dialogue/ProjectOnboardingSource`, `overview/OverviewFullScreenApp`;
- four tests: `DialogueSourceTest`, `EnrolHarnessCommandTest`, `PmOperatorProcessIT`, `transcript/PseudoTerminal`;
- the capture file `screen-captures/14-cli-gate-scope.txt` and `pm-operator/README.md`;
- nine design documents: `01-commands`, `03-dialogues`, `04-console`, `05-transcripts-entry`, `16-plan-view`, `19-actions`,
  `24-reports`, `25-system`, `26-cleanup`.

The spec declared the three gate classes and not the 22 sites it said it would touch.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`; `executor-skew` answered `match`.
- Anomalies:
  - **Build routing:** the daemon could not find `mvn` on its own PATH (every routed job answered exit `-1` in 0 s), so
    every build ran `in_process`. The first full `verify -Ppre-commit` hit the 445 s bound inside the untouched
    `pm-web-app` (exit 143; the two touched modules were green); later gates ran with a raised bound. PLAN-15, PLAN-19
    and PLAN-22 reported the same bare-`mvn` failure (open Build-server watch).
  - **Self-review, three rounds** (16, 11 and 12 findings): the console entry flow still performed web-enable and pairing
    acts, the un-onboarded entry still acted, and the full-screen views acted with no gate at all (fixed at the shared
    `WorkActionsClient` and the three direct sends in `OverviewApp`, `c511cc0`). `self-review: clear`.
  - **One `--amend` of a merge commit** (`90cc2d8`) before it was pushed, because the staged file had lost the conflict
    resolution; the pushed history was not rewritten. The merge was with `main` over PLAN-22's change to `OnboardCommand`,
    `EntryFlow` and the sources (named in the PLAN-22 landing).
  - Shell file operations were limited to `cp` of the capture set and `git worktree add/remove` for the before-tree; the
    run says file reads went through the file tools. Guard `none` on this harness, so the account is the run's own.
  - `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs); the run followed the ledger
    copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed `e295cdb` (six actionable findings, all fixed in `175b784`) and
  `175b784` (two minor findings, fixed in `7a2a27a`); it refused on quota at the merge head `7a2a27a`, so the merge ran
  under the standing review authorization with every finding fixed. Sourcery (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 277` is `success`, 20 checks. `origin/main` carries `5348e89e`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 277` answers `gate_status: OK`, six
  conditions OK (new coverage 89.2, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-14 --status shipped`
- [x] row `pr` stamped `277`
- [x] row `landing` stamped `landings/PLAN-14.md`
- [x] row `plan_marshall_plan_id` stamped `14-cli-gate-scope` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] re-grounded notes added to PLAN-42 and PLAN-23
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-14-cli-gate-scope-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **The `PM_OPERATOR_DEV_ALLOW_AGENT` decision stays open** (CLI-CC-P-05, security). PLAN-14 did not build a switch; its
  PR body carries the question. The "re-check when PLAN-14 and PLAN-24 land" watch is half discharged.
- **PLAN-20 (running) is the one plan that must rebase over this merge in a declared file**: `19-actions.adoc` changed
  (PLAN-20 declares it), and `actions/WorkActionsClient` (same package as its `ActionMenus`, `ActionsMenu`, `HoldActs`)
  now carries the gate on the client's sends. No mailbox note sent (offered).
- **Specs whose declared files this merge changed** (re-read at HEAD before emit):
  - PLAN-42 (note added: `plan <id> --print` exists; `PmConsole`, `PlanCommand`, `EpicCommand`, the catalogue pair);
  - PLAN-23 (note added: the whole `cleanup` verb is an operator act; `CleanupCommand`, `26-cleanup.adoc`);
  - PLAN-17 and PLAN-30 (`RuntimeCommand`; PLAN-30 also `CleanupCommand`, `OnboardCommand`), PLAN-17 and PLAN-26
    (`25-system.adoc`), PLAN-36 and PLAN-44 (`PmConsole`), PLAN-48 (`PlanCommand`, `EpicCommand`), PLAN-32
    (`ConfigCommand`), PLAN-05 (shipped); the catalogue-pair plans PLAN-17, 21, 23, 26, 30, 32, 34, 36, 38, 42, 44, 48, 50
    and the capture-set plans.
  - PLAN-79 (`OperatorGateScopeTest`, `OperatorGateTest` now exist).
- **Residue (accepted):**
  - the capture harness records no exit status;
  - `plan <id> --print` is PLAN-42's flag, present here on the operator's decision;
  - the mock scenario used by the captures leaves Q-007 open.
- **Branch and worktree:** no `feature/14*` or `fix/14*` branch remains on the remote (`git ls-remote`, checked after
  this record; the run's branch was `fix/14-cli-gate-scope`).

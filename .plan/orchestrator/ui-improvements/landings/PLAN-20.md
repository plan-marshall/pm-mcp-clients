# Landing Analysis: PLAN-20 — Terminal: Action Menus by State

epic: ui-improvements
workstream: WS-04
pr: #273 (merge commit `a07a09b60202a0b89639a521e1ec7f1e7729a3af`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-20-cli-actions-by-state-001.md` (runbook lane, harness
> `opencode`, models `deepseek-v4.1-flash-free/high` and `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed
> 2026-10-30 against the merged PR, the CI checks, the merge diff and the Sonar gate. The per-head build logs, the
> capture sets and the six self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Menus from links (`ActionMenus` offers a waiting plan's decision first; one case per plan state of `19-actions.adoc`) | shipped-as-specified | `ActionMenusTest` grew from the base to 18 tests with a case per state (`runningPlan`, `waitingPlan`, `waitingForYouPlan`, `waitingPlanWithoutDecision`, `pausingPlan`, `pausedPlan`, `pausedPlanKeepsItsOrder`, `held`, `integrityPause`, `ended`, `landing`, `epic`) and `ActionsCatalogueTest` (the `!` key is one character, never `a`, unique in every menu, both languages). `test-first`: failing run at base plus the uncommitted test (`waitingPlan: expected 5 but was 4`). The hypothesis (hold acts first, no decision entry; the catalogue lacked `operator.actions.key.decide` and `operator.actions.decide`) was confirmed and both keys were added. |
| D1 terminal check (the `a` menu of a waiting plan and of a paused plan, before and after) | met | 8 captures (120×30 and 80×24, `en_US` and `de_DE`) in `20-cli-actions-by-state.txt`. After: `!  Decide Q-009…` first, then Pause, Cancel, Chat, Settings (German `!  Entscheide Q-009…`); the paused plan's menu is Start, Abandon, Chat, Settings in both. Re-taken at the merge head and byte-identical to the earlier run. |
| D1 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`, `analysis_revision == head_revision`; corroborated below. |

`deliverables_done=1` of 1 is consistent with the diff. The spec's check `pm-operator verify -Pintegration-tests` was run at
an earlier head (`2f9d680e`, 68 tests) and the full `verify` at the final head (`af18ec8e`, 2449 tests).

**Surface delta** (computed here from `git diff a07a09b6^1 a07a09b6`, because `inbox landing-check` answers `unmeasured`):
declared 5 entries, realized 11 files. Three declared entries were changed (`ActionMenus.java`, `ActionsMenu.java`,
`ActionMenusTest.java`). Two declared entries are untouched: `HoldActs.java` and `doc/design/terminal-client/19-actions.adoc`
(PLAN-14 had changed that document just before; this plan documented its texts in another file). Eight files are
undeclared: `22-texts-actions.adoc`, `operator.{en,de}.properties` (three keys each: the `!` key, the `Decide` entry and
`decision_unreadable`), `WorkActionsClient.java`, `OverviewFullScreenApp.java`, `ControlDialoguesTest.java`,
`ActionsCatalogueTest.java` and the capture file `20-cli-actions-by-state.txt`. The two undeclared production files are
the ones that read the decision and open it from the full-screen overview.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`; `executor-skew` answered `match`. Two models were used in the run (the multi-model form).
- Anomalies:
  - **Merge queue ejection on infrastructure:** PR #273 was dequeued once because the merge group failed on
    infrastructure, not on the change; the operator chose to re-enqueue it and it merged.
  - **Self-review budget exhausted after six rounds:** the operator authorised rounds four and five at the limit.
    Rounds one to three fixed real items (the dispatch route fallback and state gate, the client read, the paused versus
    pausing split, the `menuDecision` lifecycle, a German du-form); rounds four to six found only LOW items, listed
    under Residue. `self-review: budget-exhausted`.
  - **Build routing:** every build ran `in_process` (the routed build answered `execution_failed`; the daemon could not
    start `mvn`). The capture set built with `-pl pm-operator` alone resolved the registry's `pm-api` snapshot and
    rendered raw message keys, so it needs `-pl pm-api,pm-operator`. PLAN-15, 19, 22 and 14 reported the same bare-`mvn`
    failure (open Build-server watch).
  - **One CI failure that was not the change:** `sonar-build` failed once on "Organization is not allowed to access data
    from non main branches" and passed on re-run.
  - **Shell file operations by the run itself:** one `cat >>` to the progress file and one `rm -rf` of its own capture
    output. Guard `none` on this harness, so the account is the run's own.
  - `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs); the run followed the ledger
    copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) refused on quota eight times, then reviewed `2f9d680e` (a nitpick,
  answered as an accepted coverage gap), `e5669363` (one inline Minor, fixed in `8a3cbea5`, thread resolved), `8a3cbea5`
  (clean) and, in its summary marker, the merge head `af18ec8e` (Merge Risk Low, no actionable comments). Its last
  summary remark (derive the waiting state from an enum) was answered: `WorkActions` carries `state` as a plain string
  by contract. Sourcery (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 273` is `success`, 20 checks. `origin/main` carries `a07a09b6`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 273` answers `gate_status: OK`, six
  conditions OK (new coverage 82.1, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-20 --status shipped`
- [x] row `pr` stamped `273`
- [x] row `landing` stamped `landings/PLAN-20.md`
- [x] row `plan_marshall_plan_id` stamped `20-cli-actions-by-state` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] re-grounded notes added to PLAN-21 and PLAN-36
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-20-cli-actions-by-state-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **No plan is running now.** PLAN-79 (pm-operator test conversion) no longer waits for a running terminal plan.
- **The `decide` dispatch has no unit test** (the branch re-enters the static `PmOperator.execute` under
  `OperatorGate.verify()`); its PTY path belongs to PLAN-21. A note is added to that spec.
- **The `!` key:** PLAN-20 made `!` the key of the Decide entry in the `a` menu and tested it unique in every menu. PLAN-36
  gives `!` meaning at the prompt and on the overview root; a note is added to that spec so the two stay one rule.
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): none declares `ActionMenus`,
  `ActionsMenu`, `WorkActionsClient`, `ActionsCatalogueTest` or `22-texts-actions.adoc`; `OverviewFullScreenApp` is declared
  by shipped PLAN-05 only; the capture-set plans are unaffected (each writes its own capture file).
- **Residue (accepted, all LOW):** the paused capture cannot prove the state gate (a unit test does); `pausingPlan`'s
  assertions stay thin; the plan view keeps its pre-existing `-` placeholder (PLAN-34); the German captures show English
  runtime vocabulary (`finalize`, `phase init`) that the runtime sends.
- **Branch and worktree:** no `feature/20*` or `fix/20*` branch remains on the remote (`git ls-remote`, checked after this
  record).

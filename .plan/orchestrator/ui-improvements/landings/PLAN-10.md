# Landing Analysis: PLAN-10 — CLI choice prompt, pause

epic: ui-improvements
workstream: WS-04
pr: #257 (merge commit `81f84ee3e210bd97981dc6f02b53d72eb4d6c940`, branch `feature/10-cli-choice-prompt-pause`)

> Landing record for one shipped plan. Source: inbox message `rb-10-cli-choice-prompt-pause-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2028`), analysed 2026-10-29 against the merged
> PR, the CI checks, the Sonar gate, the merge diff and the spec's declared Expected Surface. Test counts, the
> failing-first runs and the terminal captures are the lane's claims; not re-run.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 ChoicePrompt | shipped-as-specified | `dialogue/ChoicePrompt.java`, `dialogue/package-info.java` and `ChoicePromptTest.java` are in the merge diff. The 28 tests (key, number, range, Enter with and without a preselection, `?`, `?n`, `q`, `v`, unknown input, echo, consent, two-column, wrap, German label, end of stream, read failure, single-choice list) and the failing-first run `2108ef7c` are the lane's claims. The clause "no terminal check of its own" is met: every capture runs a verb (`plan pause`, `plan cancel`). |
| D2 Pause, cancel, abandon | shipped with two clauses not met as written | `WorkActionDialogue`, `ActionSupport`, `PlanCommand`, `PauseDialogueIT`, `CancelDialogueIT` and `ControlDialoguesTest` are in the diff; the `@Disabled` ITs are enabled (seven tests, `assertNoAct` for `q`, `9`, Enter; `assertAct` for pause and cancel). The terminal check is evidenced: `ScreenCaptureIT` set `10-cli-choice-prompt-pause`, 24 captures (120x30 and 80x24, en and de), the committed `screen-captures/10-cli-choice-prompt-pause.txt` is in the diff. **Not met:** (1) the browser regression check on `fully-onboarded` and `running-work`, because the changed texts are terminal-only `operator.*` keys and no web view renders them (the spec says it targets "the changed catalogue areas (control)", but the plan changed no `control.*` key); the snapshots were taken anyway under `.plan/temp/playwright-mcp/10-cli-choice-prompt-pause-*`. (2) the per-point pause consequence, which the actions resource carries no data for. Both were approved by the operator during the run. The Sonar clause is met (see below). |

`deliverables_done=1` of 2 is consistent with that account.

**Surface delta** (computed here from the merge diff against the declared Expected Surface, because
`inbox landing-check` answered `unmeasured`): declared 11, realized 21, state `expansion_detected`.

- Declared and realized (9): `ChoicePrompt.java`, `dialogue/package-info.java`, `WorkActionDialogue.java`,
  `ChoicePromptTest.java`, `PauseDialogueIT.java`, `CancelDialogueIT.java`, `ControlDialoguesTest.java`,
  `operator.de.properties`, `operator.en.properties`.
- Declared, untouched (2): `control.de.properties` and `control.en.properties`. The plan changed texts in the
  `operator.*` catalogue only. PLAN-19 had changed the `control.*` pair; PLAN-10 did not need it.
- Realized, never declared (12): `ActionSupport.java`, `PlanCommand.java`, `StartPrompt.java`,
  `PlanCommandTest.java`, `ScreenCaptureIT.java`, `screen-captures/10-cli-choice-prompt-pause.txt`,
  `MockRuntimeEngine.java` and `MockRuntimeEngineTest.java` (mock routes the dialogue needed), and four design
  documents: `doc/design/shared/consents.adoc`, `doc/design/terminal-client/03-dialogues.adoc`,
  `19-actions.adoc`, `27-tui-testing.adoc`. The message names the mock route handler, `ActionSupport`,
  `PlanCommand`, `StartPrompt` and `ScreenCaptureIT` as the files outside the surface; it does not name
  `PlanCommandTest`, the capture text file or the four design documents (the PR body was not re-read here).

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version`
  `unknown`. `executor_version` `0.1.2028`, the registry pin.
- Anomalies:
  - `self-review` step state `budget-exhausted` (six rounds; rounds 3 to 6 by a separate reviewer; round 3 gave 21
    findings and the operator chose "fix everything").
  - The run was blocked once on the network (github.com:443); the operator answered "retry now".
  - Earlier operator questions: executor skew (answered "Proceed with the current executor" three times), the
    Playwright MCP output-dir failure (answered "Update mcp_config.json"), the two not-met clauses ("record them
    as not met").
  - One routed submit with a relative `--project-dir` failed in 2 s; one `verify -Ppre-commit` hit its 1037 s
    bound and was re-run with a longer timeout.
  - Process deviations reported by the run (shell `sed -n` and `grep` for reads, two `for` loops, one `curl` to
    a Sonar rule page): the guard was `none` on this harness, so nothing enforced them. Recorded here only.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed three times (12 findings, all fixed, every thread resolved); the newest reviewed
  head is `1c77501`, and the triggers after `0e9ba48` and `52275941` were refused "Review rate limited"
  (quota, `2026-10-28T23:40:01Z`). Sourcery refused on its hard quota (optional, does not gate). The merge ran
  under the standing review authorization.
- CI/merge: `ci checks status --pr-number 257` is `success`, 30 checks, all `pass` or `skipping`, including
  `build / build (25)`, `build (26)`, `quarkus-alignment`, `sonar-build`, `integration-tests / test` and both
  `build / conclusion`. `origin/main` tip before this analysis is `81f84ee3`, the merge commit.
- Sonar: the landing reports `open_issue_count=0`, `open_issue_rules=none`. Measured at the merged PR:
  `sonar_rest gate-status --pr 257` is `OK` (reliability, security and maintainability ratings 1,
  `new_coverage` 87.6 against the 80 threshold, duplication 0.0 %, hotspots 100 %). The run's own account of
  24, 8, 1, 0 open issues and one coverage failure at 78.5 % is consistent. Corroborated, no anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-10 --status shipped`
- [x] row `pr` stamped `257`
- [x] row `landing` stamped `landings/PLAN-10.md`
- [x] row `plan_marshall_plan_id` stamped `10-cli-choice-prompt-pause` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-10-cli-choice-prompt-pause-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **`ChoicePrompt` now exists** (`pm-operator/.../dialogue/ChoicePrompt.java`). PLAN-17, 22 and 23 name it; their
  specs are to be re-read against HEAD before emitting, to use it as built and not as outlined.
- **Operator catalogue is free again.** PLAN-10 no longer holds `operator.{de,en}.properties`. PLAN-13 and PLAN-14
  (and the eight others that declare the pair) are no longer blocked by it; re-read them at HEAD, because PLAN-10,
  15 and 19 changed the catalogue.
- **Mock engine changed by PLAN-10** (the message lists CodeRabbit fixes to the mock's epic action routes:
  `common_files`, `with_plans`, `archive`, `next`; `MockRuntimeEngine.java` and its test). PLAN-11 declares both and cites `MockRuntimeEngine:7663` for the `kind="item"` fix: re-read the
  line at HEAD, it has moved.
- **Mock defects the run found and left**: `/api/v1/epics/{id}/actions/cancel` ignores `with_plans`, and
  `/api/v1/epics/{id}/actions/next` is a no-op. No staged plan owns them. Recorded as a Watch for the operator to
  place (PLAN-11 is the natural owner).
- **Per-point pause consequence** cannot be shown: the actions resource carries no per-point data. A design
  decision (data in the daemon contract, or drop the clause), not a staged plan. Recorded as a Watch.
- **Browser regression clause on terminal-only plans.** The clause names "the web views that render the changed
  catalogue areas" and was generated from the declared `control.*` pair, which the plan did not change. PLAN-05,
  17, 21, 22, 23, 26, 34 and 36 carry the same clause; check each against what the plan changes before it is
  emitted. Recorded on the existing terminal-check Watch.
- **Closed-PR branch / test-results watches.** The run says nothing about `test-results/` or the main-checkout
  Playwright run; its browser evidence came through the Playwright MCP server (after the output-dir repair). The
  watch on PLAN-10's main-checkout dirtying is closed for PLAN-10 and stays for the recurring
  `test-results/.last-run.json`.
- **Review-bot quota** hit again (CodeRabbit refused twice, final head unreviewed): recurrence on the Watch.

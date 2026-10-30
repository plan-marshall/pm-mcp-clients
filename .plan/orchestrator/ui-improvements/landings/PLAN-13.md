# Landing Analysis: PLAN-13 — Terminal: One Event Contract, Pushed Notices, Prompt Badges

epic: ui-improvements
workstream: WS-04
pr: #270 (merge commit `7e2b4b61edc3963c40603fe3887e0a0348fbf2ea`, branch `feature/13-cli-event-contract`)

> Landing record for one shipped plan. Source: inbox message `rb-13-cli-event-contract-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-30 against the merged PR, the CI
> checks, the merge diff, the PR body and the Sonar gate. The terminal captures, the per-head build logs and the four
> self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Event kinds in pm-api (`fromEventMap` takes the event name first, `type` as fallback) | shipped-as-specified | `EventKind.java` (new, in `pm-api/.../event/`) and `EventKindTest`; `ConsoleNotice.java` and `ConsoleNoticeTest` changed. The spec offered "extend `EventName` or add `EventKind`"; the run added `EventKind` and left `EventName.java` untouched. `test-first` red run recorded (job named). Before/after captures under `.plan/temp/terminal/13-cli-event-contract/`. |
| D2 Answered elsewhere and badges (`pm [3 questions · 1 hold] ›`, at most three then `+N`, refreshed per event) | shipped-as-specified | `ConsoleNoticeIT` (new) through `/mock/broadcast`; `PmConsole`, `OverviewFullScreenApp`, `EntryFlow` changed. `test-first` red run recorded. The capture shows `pm [3 questions · 1 integrity pause · 1 held] ›`, the question notice above the prompt and the answered-elsewhere line. |
| D3 Web alignment note | met | The PR body names six web files that read event names (below). The web change itself is not part of this plan. |
| D3 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none` in the landing; earlier reads 12 → 4 → 1 → 0, all fixed. Corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff. One departure from the spec text: `landing.failed` is accepted as
an event name in the tests, but no `EventName` member carries it; the runtime reports a failed landing as `hold.opened`
with kind `landing_failed` (run's residue). The spec listed `landing.failed` among the five named events.

**Surface delta** (computed here from `git diff 7e2b4b61^1 7e2b4b61`, because `inbox landing-check` answers
`unmeasured`): declared 10 entries, realized 23 files. Seven declared entries were changed (the `event/` directory
with `EventKind.java`, `ConsoleNotice.java`, `ConsoleNoticeTest.java`, `ConsoleNoticeIT.java`, `04-console.adoc`,
`operator.{en,de}.properties`). Three declared files are untouched: `EventName.java`, `ConsoleNoticeBuffer.java`,
`ConsolePrompt.java`. Sixteen files are undeclared: `EventKindTest`, `EntryFlow` and `EntryFlowTest`, `PmConsole` and
its two tests, `OverviewFullScreenApp` and its test, `MockRuntimeEngine` and its test, `PtySession`, `ScreenCaptureIT`,
`SummaryLineIT`, `TuiCapabilityIT`, `27-tui-testing.adoc`, and the new capture set
`screen-captures/13-cli-event-contract.txt`. The lane named the surface added beyond the spec (badge clause and the
terminal checks need them). The two PLAN-22-relevant ones are `EntryFlow` and `EntryFlowTest`.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`.
- Anomalies:
  - **Shell edits against the rules, by the run itself:** one heredoc with inline `python3` to edit the capture set and
    one `perl -0pi` in-place edit of `ConsoleNotice.java`. Both re-read and correct. Guard `none` on this harness, so the
    count is the run's own.
  - Self-review ran four rounds (the operator chose a fourth at the lane's limit): 18 + 11 + 2 blockers, then none.
    `self-review: clear`. Round 1 and 2 blockers were real (waiver notice, notification leak, German template, the
    scope resolution, a dead mock branch).
  - One build hit the daemon bound (1362 s learned) and was re-run with `--timeout 3000`; one failed on a flaky shell
    test (`StartStopScriptsTest`, passes alone on base and head); `-pl pm-operator` without `-am` failed to resolve the
    new `EventKind` from the installed `pm-api` (the spec's own module command needs `-am` now).
  - `spec_differ: true` at setup: the main checkout's copy of the spec lacked the re-grounding note; the run followed
    the ledger copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) formally reviewed head `a0820cde` only (6 inline findings, 2
  follow-ups, 1 nitpick; five fixed in `f44b6e6`, one answered as out of scope, one withdrawn by the reviewer). It then
  refused on quota five times; after the last two pushes it was re-triggered once and refused again. The merge ran under
  the standing review authorization. Sourcery refused on its quota (optional). Later pushes (`f44b6e6`, `eeba16e5`,
  `8ce6aa7c` and after) were never reviewed by it.
- CI/merge: `ci checks status --pr-number 270` is `success`, 20 checks, all `pass` or `skipping` (CodeRabbit `NEUTRAL`).
  `origin/main` carries `7e2b4b61`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 270` answers `gate_status: OK`, six
  conditions OK (new coverage 81.4, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-13 --status shipped`
- [x] row `pr` stamped `270`
- [x] row `landing` stamped `landings/PLAN-13.md`
- [x] row `plan_marshall_plan_id` stamped `13-cli-event-contract` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-13-cli-event-contract-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **PLAN-22 is running and declares `EntryFlow`**, which this merge changed (with `EntryFlowTest`). The cross-spec
  declaration-currency read shows it as the one overlap with a running plan. Its lane has to rebase onto `7e2b4b61`; it
  is the plan that rewrites `EntryFlow`'s begin prompt, so the conflict is real. Offered to the operator as a mailbox
  note; nothing sent.
- **Staged specs whose declared files this merge changed** (re-read at HEAD before emit): PLAN-14 (`PmConsole`,
  `PtySession`, the operator catalogue), PLAN-34 (`OverviewFullScreenApp`, `ConsoleNoticeIT`, `PtySession`,
  `ScreenCaptureIT`), PLAN-36 (`PmConsole`, `OverviewFullScreenApp`, catalogue), PLAN-38, 50 (`OverviewFullScreenApp`),
  PLAN-42 (`PmConsole`, catalogue), PLAN-44 (`PmConsole`, `OverviewFullScreenApp`, `ConsoleNoticeIT`, `PtySession`,
  `ScreenCaptureIT`), PLAN-15 (`TuiCapabilityIT`), PLAN-05 (`OverviewFullScreenApp`), and the catalogue-pair plans
  PLAN-17, 21, 23, 30. Their gate reads are clean (no shared file with a running plan except PLAN-22 above).
- **Web alignment not planned:** the PR names six web files that still match event names themselves
  (`stores/runtime.ts`, `views/epicEvents.ts`, `OverviewView.svelte`, `DecisionView.svelte`, `ReportsView.svelte`,
  `api/client.test.ts`) and asks that they move onto the same table. No staged plan owns that move. Staged specs that
  name those files: PLAN-09, 39, 43, 45, 51, 52, 54, 57, 58. Recorded as a Watch; decide at the first of them to be emitted
  (PLAN-39 is running and its spec does not mention the table).
- **`landing.failed`** has no `EventName` member (the runtime sends `hold.opened`, kind `landing_failed`); check where a
  later spec names it.
- **Design text added by the run:** `04-console.adoc#prompt` states that a refreshed badge shows at the prompt's next
  draw (JLine cannot replace the prompt of a line being read; the window title changes at once), and
  `#answered-elsewhere` states that an answer over the `cli` channel prints no line.
- **Residue (accepted):** project-setup badges are pre-existing and unproduced; the terminal screens carry one
  pre-existing deviation, identical before and after (reports-arrival greeting prints an ISO timestamp and its German
  line keeps an English fragment, a text-quality watch for PLAN-30); `waitingCounts()` reads two locked collections on
  every status request.
- **Branch and worktree:** no `feature/13*` branch remains on the remote (checked after this record).

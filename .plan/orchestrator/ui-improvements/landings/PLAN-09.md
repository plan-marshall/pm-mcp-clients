# Landing Analysis: PLAN-09 — Web: Display Layer and Catalogue Fixes

epic: ui-improvements
workstream: WS-02
pr: #287 (merge commit `5f9e60da850c667b9f0b6e41299d07b81e76fb21`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-09-web-display-layer-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`), analysed 2026-10-31 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The 64 screenshots, the accessibility snapshots, the per-head build logs and
> the three self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Display module (`displayState`, `displayError`, `displayUnit`, `enumFields.ts`) | shipped-as-specified | `pm-web-app/src/lib/display/index.ts`, `enumFields.ts` and `index.test.ts` (17 tests, both languages, an unknown value). `test-first`: failing run on the unresolved import, green after. Browser check: 64 screenshots and 16 accessibility snapshots under `.plan/temp/playwright-mcp/` (before and after per target, locale, theme and width). |
| D2 Enum guard | shipped-as-specified | `enumGuard.test.ts` parses every `.svelte` file; it failed on `SystemView.svelte` (`w.state.toLowerCase()`) before the views changed and passes after. Three control tests prove it catches a bare read, an optional chain, a nullish fallback, a `||` fallback, a mixed `displayState(...) + node.state`, `String(node.state)` and an `aria-label` binding. |
| D3 Views through the display layer | met | The text-guard baseline lost exactly the raw-enum and state-id entries of the nine views; the e2e suite is green (307 tests). What remains in the baseline is `iso-time` (CC-F-04, PLAN-16's) and one `foreign-word: ago` in the German overview. |
| D4 Catalogue fixes | met | A catalogue test asserts each corrected string in both locales, `test-first`. The open-issue read at the head is a confirmed `0`; corroborated below. |

`deliverables_done=4` of 4 is consistent with the diff. Two hypotheses were partly refuted: the cleanup row of the
CC-F-05 table was not a catalogue defect (the mock served `ended_at: "ended 12 Sep"`, so the view added a second
"ended"), and the system row was not either (each message carried its own label). `displayError` has no production
caller yet: no view prints an error code until PLAN-45; the function is there because the spec requires it.

**Surface delta** (computed here from `git diff 5f9e60da^1 5f9e60da`, because `inbox landing-check` answers
`unmeasured`): declared 26 entries, realized 51 files. Twenty-two declared entries were changed. Four declared entries are
untouched: `ReportsView.svelte`, `StartView.svelte`, `overview.en.properties` and `overview.de.properties`. Twenty-nine
files are undeclared. By directory: 14 files in `pm-api/.../messages` (the catalogue files `common`, `decisions`,
`onboarding`, `settings`, `system`, `plan_view`, `start` in both languages, beside the declared ones), 13 in
`pm-web-app/src/lib/views` (views and their tests), 2 in `lib/components`, `i18n/index.ts` (`hasMessage`), 4 in
`pm-operator` (`StartCommand`, `StartDialogue`, `SystemLevel`, the `plan-view` transcript), 4 in `pm-mock-runtime` (three
scenario files and `MockRuntimeEngine`), 3 in `pm-web-e2e` (the baseline and two specs), 4 design documents and one
catalogue test; the directory counts overlap the declared entries, so they do not sum to 29. The run gives the reason: the shared messages were split into web and console keys, and the
mock's fixture titles carried display text.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2036`; `executor-skew` answered `match`.
- Anomalies:
  - **Reach into the terminal client:** the PR changed `SystemLevel`, `StartCommand`, `StartDialogue` and a transcript in
    `pm-operator`, because the shared catalogue messages were split into web and console keys. The running PLAN-17 declares
    `SystemLevel`; its lane merges `main` in. Recorded for PLAN-17, 26 and 30.
  - **Mock fixtures changed:** three scenarios and `MockRuntimeEngine` carry corrected display titles. Specs that assert
    fixture values (the epic's fixture-under-declaration watch) are listed under Follow-Ups.
  - **Self-review, three rounds** (10, 4, then clear of blocking findings; `self-review: clear`): the epic and plan state
    handling, the guard's blind spots for `?.`, `??` and `||`, the waiting plan reading "unknown", a raw off-track action,
    a missing change-map vocabulary.
  - **One early build used a relative `--project-dir`** and resolved to the main checkout; every later call used the
    absolute path (also seen in PLAN-77's run). Nothing was written there; recorded.
  - **Shell rule:** one `| rg` pipeline used twice while reading the PR comment list; recorded by the run. Guard `none`
    on this harness, so the account is the run's own.
  - `spec_source=ledger_worktree`, `spec_differ=true`; the run followed the ledger copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed `647d2370` (7 inline findings, 1 follow-up, 3 nitpicks), all fixed
  in `d5fd44e8` except the cleanup-reminder follow-up, which was answered as a server-contract gap this plan does not own;
  its threads read "Addressed in commit d5fd44e8". The re-trigger on the merge head was refused on quota. The merge ran
  under the standing review authorization, and the operator was asked at the gate and chose "Merge now". Sourcery
  (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 287` is `success`, 20 checks. `origin/main` carries `5f9e60da`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 287` answers `gate_status: OK`, six
  conditions OK (new coverage 100.0, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-09 --status shipped`
- [x] row `pr` stamped `287`
- [x] row `landing` stamped `landings/PLAN-09.md`
- [x] row `plan_marshall_plan_id` stamped `09-web-display-layer` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] re-grounded notes added to PLAN-16, PLAN-17 and PLAN-45
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-09-web-display-layer-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **PLAN-16 is unblocked** on PLAN-09 (it also needed PLAN-06 and 76, shipped). It owns the ISO times that remain in
  `/reports`, the completion report and `/settings/machine`. Its note: the text-guard baseline keeps those `iso-time`
  entries and the one German `foreign-word: ago`.
- **PLAN-45 gets its first `displayError` caller:** the function exists and is tested; PLAN-45's `ErrorState` is the caller.
  Note added.
- **PLAN-17 (running)** declares `SystemLevel`, which this merge changed (with `StartCommand` and `StartDialogue`): its lane
  merges `main` in. Note added to PLAN-17 so the landing check looks at it.
- **Cleanup reminder interval has no write path** (`cleanup_durations` is a single integer): a server-contract gap the run
  answered on the PR; no plan owns it. Recorded as a Watch.
- **Two message families for node kinds:** `state.node_kind.*` stands beside the older `plan.node.kind.*`; both are used and
  nothing keeps them in step. Recorded as a Watch for the plans that touch the plan tree (PLAN-55, 62).
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): the nine reworked views (`StartView`:
  PLAN-43, 53; `ReportsView`: PLAN-54; `SystemView`: PLAN-25, 43, 47; `PlanView`: PLAN-25, 34, 43, 55, 62, 73, 81, 83;
  `EpicView`: PLAN-43, 58, 83; `OverviewView`: PLAN-52, 83; `CompletionReportView`: PLAN-54, 73; `MachineSettingsView`:
  PLAN-60, 85), `i18n/index.ts` (PLAN-16, 85), `MockRuntimeEngine` and the scenarios (PLAN-82, 03, 11), `StartDialogue`
  (PLAN-30), `SystemLevel` (PLAN-17, 26), `NodeNarrative` (PLAN-55, 62; it now knows the node kinds). The text-guard
  baseline is shared by every web plan: the file may only shrink.
- **Residue (accepted):** ISO times in three views (PLAN-16); `displayError` without a caller until PLAN-45.
- **Branch and worktree:** no `feature/09*` branch remains on the remote (`git ls-remote`) and the lane's worktree is gone
  (`git worktree list`), both checked after this record.

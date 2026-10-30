# Landing Analysis: PLAN-39 — Web: App Shell with Rail, Top Bar, Bottom Bar and Live Indicator

epic: ui-improvements
workstream: WS-03
pr: #274 (merge commit `44b051ff0bf8da741e7f6e5c8785f972160cf6c0`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-39-web-app-shell-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2032`), analysed 2026-10-30 against the merged PR, the CI
> checks, the merge diff and the Sonar gate. The per-head build logs, the browser artifacts under
> `.plan/temp/playwright-mcp/` and the two self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Shell split (`+layout.svelte` keeps the boot; `lib/shell/` holds rail, top bar, nav model; three groups, the header's counts) | shipped-as-specified | `AppShell.svelte`, `NavRail.svelte`, `TopBar.svelte`, `nav.ts` (new) and a `+layout.svelte` reduced by about 500 lines. `chrome.spec.js` asserts the groups, the counts (3 and 5 on `fully-onboarded`), the collapsed rail across a reload and every one of the ten routes inside the shell. `test-first` red run recorded (job named). |
| D2 Phone (bottom bar with four labelled items, More sheet, no horizontal scroll at 390 px) | shipped-as-specified | `BottomBar.svelte` (new), `mobile.spec.js` extended; `PlanView`, `SystemView`, `ProjectExplorerView` and `Card` wrap or shrink. The More sheet uses the design-system `Dialog` (the first caller outside `/dev/components`). `test-first` red run recorded. One departure: the phone bar ships Running work, Decisions, Reports, More; the attention inbox item is deferred with its route to PLAN-49. |
| D3 Live indicator (UX-P-12; four stream states; `Try again`; replaces the offline banner) | shipped-as-specified | `LiveIndicator.svelte` (new), `client.ts` reports the stream state, `runtime.ts` mirrors it. `streamState.test.ts` and `LiveIndicator.test.ts` are new; `chrome.spec.js` stops and restarts the mock. `test-first` red run recorded (4 tests failed). |
| D3 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none` in the landing; corroborated below. |

`deliverables_done=3` of 3 is consistent with the diff. The search trigger is present and disabled with its reason;
PLAN-46 builds the palette and enables it (the spec's dependency order already says so).

**Surface delta** (computed here from `git diff 44b051ff^1 44b051ff`, because `inbox landing-check` answers
`unmeasured`): declared 13 entries, realized 37 files. All 13 declared entries were changed: the `lib/shell/`
directory with its four declared components, `+layout.svelte`, `stores/runtime.ts`, `chrome.spec.js`,
`workspace.spec.js`, `mobile.spec.js`, `doc/design/web-app/README.adoc` and `common.{en,de}.properties` of `pm-api`.
Twenty-five files are undeclared:
- the shell and stream code: `nav.ts`, `nav.test.ts`, `LiveIndicator.svelte`, `LiveIndicator.test.ts`,
  `lib/api/client.ts`, `lib/api/streamState.test.ts`, `lib/test/runtimeState.ts`, `routes/layout.test.ts`;
- shared surfaces that needed wrapping at 390 px: `Card.svelte`, `PlanView.svelte`, `SystemView.svelte`,
  `ProjectExplorerView.svelte`;
- the mock and the e2e harness: `MockRuntimeEngine.java` and `PlanDetailTest.java` (the run fixed a pre-existing e2e
  failure, below), `pm-web-e2e/global-setup.js`, `helpers/mockRuntime.js`, `helpers/pairing.js`, and the specs
  `epic-view`, `overview`, `pairing`, `plan-view`, `reports`, `showroom`, `system`, `theme-readability`.

The declaration was right about what the shell is and wrong about what it drags along: the e2e specs that name the
header or the offline banner changed with it, and the stream client is not in the list. PLAN-03, 11 and 13 did the same.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2032`; `executor-skew` answered `match` (lane `0.1.2032`).
- Anomalies:
  - **Mock fix inside a web plan:** the e2e module build (a Standing Constraint of the plan) failed one test on a
    defect from `origin/main`: the mock answered `plan_not_found` for a plan that only an epic's row lists, and gave a
    synthesized plan no epic link. The operator chose "fix the mock fallback here" (commit `92a02cf`):
    `MockRuntimeEngine` now looks a plan up among the epic's rows (`planListedByAnEpic`, `planRowSummary`,
    `planStateOf`) and a plan synthesized from a row names its epic. This is the mock-plan-detail area PLAN-11 owned;
    PLAN-11 fixed the per-id lookup but not plans that live only in an epic. Not in the mock watch's list.
  - **Shell reads against the rules, by the run itself:** repeated `grep`, `sed`, `ls`, `cat`, `tail`, `head`, `awk`
    on repository files and logs instead of the file tools, and redirects into `.plan/temp/`. No stash, restore,
    rebase, forced push or branch delete. Guard `none` on this harness, so the account is the run's own.
  - Two self-review rounds (sub-agent): 8 and 19 items, all fixed or answered; round 3 not dispatched because every
    remaining item was a reporting obligation or an answered decision. `self-review: clear`.
  - Build server: two bound expiries at the daemon's 300 s default early in the run, then `--timeout 1800`; one
    e2e job terminated by the run itself when its source changed mid-run, re-run. No degraded fallback.
  - CI flakes: `ConsoleNoticeIT.decisionOpenedNotice` (pm-operator, came in from main) failed once and passed on
    `ci checks rerun`; two theme-readability e2e tests (mobile EpicView, revoked text guard) failed once in one run and
    passed on the re-run.
  - `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs); the run followed the ledger
    copy.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) refused on quota five times across the run, then reviewed head `6d3eec3`
  (6 findings) and `80f00b60` (4 findings); every finding was fixed (`9bfa960`, `c01b358f`) or answered and every thread
  resolved. The merge head `8225582e` was not reviewed by it; the merge ran under the standing review authorization
  after the operator confirmed it for this PR. Sourcery (optional) refused on quota.
- CI/merge: `ci checks status --pr-number 274` is `success`, 20 checks, all `pass` or `skipping` (CodeRabbit
  `NEUTRAL`); `origin/main` carries `44b051ff`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 274` answers `gate_status: OK`, six
  conditions OK (new coverage 83.6, duplicated lines 0.0, hotspots reviewed 100). The run fixed `typescript:S7776`,
  `java:S1192`, `java:S3358` and `typescript:S2486` in pushed commits and read a confirmed `0` at `8225582e`.
  Corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-39 --status shipped`
- [x] row `pr` stamped `274`
- [x] row `landing` stamped `landings/PLAN-39.md`
- [x] row `plan_marshall_plan_id` stamped `39-web-app-shell` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-39-web-app-shell-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): PLAN-46 (declares `lib/shell/` and
  `chrome.spec.js`: the palette trigger, which this plan left disabled, and the shell it sits in), PLAN-49 and PLAN-66
  (`+layout.svelte`, now a boot-only file; PLAN-49 also adds the phone Inbox item), PLAN-45 and PLAN-57
  (`lib/api/client.ts`, which now owns the stream-state reporting; PLAN-45 also `chrome.spec.js`), PLAN-37
  (`workspace.spec.js`), PLAN-52 and PLAN-75 (`mobile.spec.js`), PLAN-09, 43, 47, 55, 59, 62, 73 (`PlanView`,
  `SystemView`, `ProjectExplorerView`), PLAN-47 (`system.spec.js`), PLAN-54 (`reports.spec.js`), PLAN-70, 73, 74
  (`theme-readability.spec.js`). PLAN-80 only names `+layout.svelte` in prose. None is a running plan:
  the gate reads are clean for the running set.
- **Retire or update watches:**
  - Container components without callers: `Dialog` now has a caller (the More sheet); `Toast` still has no host or
    store and `Card` is used by the shell's shared surfaces only incidentally. Narrowed, not retired.
  - Web event table: PLAN-39's `runtime.ts` and `client.ts` changed but do not read the PLAN-13 table; it stays owned
    by no plan.
  - Four `system.*` lifecycle states: the machine pill stays neutral, the states stay invisible; PLAN-39 did not take
    them. Stays open (decide at PLAN-09 or stage one; PLAN-41 at the latest).
  - `<html lang>` stays `en` (`src/app.html`); PLAN-39 did not take it. Stays open.
  - The `PLAN-31`/`PLAN-39` "check at PLAN-39" of the Dialog watch is discharged.
- **Residue (accepted):** the phone bar has no Inbox item until PLAN-49; the search trigger is disabled until PLAN-46;
  `isOffline` remains a write-only store field; the browser evidence lives under the main checkout's
  `.plan/temp/playwright-mcp/` (the worktree was removed after the merge).
- **Branch and worktree:** no `feature/39*` branch remains on the remote (`git ls-remote`, checked after this record).

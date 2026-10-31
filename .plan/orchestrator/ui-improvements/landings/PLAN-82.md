# Landing Analysis: PLAN-82 — Shared: The Runtime Contract and Mock for Order, Size, What Waits and Finding Work — With the Design of the Two Views

epic: ui-improvements
workstream: WS-01
pr: #284 (merge commit `7d7b4d7165b335c5cdbc3e11b00ec028f1504fc9`, squash merge onto `5f9e60da`)

> Landing record for one shipped plan. Source: inbox message `rb-82-needs-and-find-contract-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`), analysed 2026-10-31 against the merged PR,
> the merge diff (`git diff 5f9e60da 7d7b4d71`, 39 files, +3521/−75), the lane's progress record
> (`.plan/temp/rb-82-needs-and-find-contract/progress.md`) and the Sonar read. The per-head build logs and the three
> self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The stable order and the size of change (mock test, contract test, catalogue test) | shipped-as-specified | `RunningWorkScenarioTest` (+150), `ChangeSizeContractTest` (new, 100), `PlanViewCatalogueTest` (+28) and the production side (`ChangeSize.java`, `ActivePlanSummary.java`, `PlanCompletionReport.java`, `plan_view.*` keys, `MockRuntimeEngine.java`, `running-work.json`) are all in the merge diff. The message names the failing runs of the test-first order. |
| D2 The design of the two views, accepted by the operator | shipped-as-specified | `doc/design/shared/needs-and-find.adoc` (186 lines) and `doc/design/web-app/needs-and-find.adoc` (101 lines) exist at the merge commit, both linked from `doc/design/README.adoc` / `web-app/README.adoc`. The operator's answers ("Accept both views", "Yes — '/' is the list") are recorded in the message as operator questions 3 and 4; they are the operator's own words and are not re-asked here. |
| D3 The what-waits and find contract and the mock | shipped-as-specified | `NeedsOperatorItem/ItemRef/Page/Summary`, `FindKind/Match/Page`, `WorkLocation`, `MachineApiPaths` (new), `NeedsOperatorKind` (+credential, +cleanup_due), `NeedsOperatorChangedEvent` (machine summary), `NeedsAndFindContractTest` (240), `NeedsAndFindScenarioTest` (311), `reports-and-system.json` (+18). |
| The package rows in `doc/plans/README.adoc` stay `open` | met | `doc/plans/README.adoc` is not in the merge diff. |

`deliverables_done=3` of 3 is consistent with the diff.

**Surface delta** (`inbox landing-check` with the declared entries of the spec and the merge diff as the realized set,
base `5f9e60da`): declared 15 entries, realized 39 files, `expansion_detected`.

- **Added, never declared (17):** `pm-api/.../MachineApiPaths.java` (the spec declared `MachinePaths.java`; the run
  refuted that hypothesis — `MachinePaths` is the filesystem root, the HTTP paths needed a new class), the new design
  document `doc/design/shared/needs-and-find.adoc`, `Scenario.java`, `reports-and-system.json`, `running-work.json`, five
  `pm-mock-runtime` tests and four `pm-api` tests (the spec declares no test files at all), and three `pm-operator`
  files — `OverviewFormattingTest`, `ProjectWorkLevelTest`, `transcript/status.transcript` — which a contract change
  to `ActivePlanSummary` and the order forced to follow.
- **Declared, untouched (3):** `MachinePaths.java` (read, not changed), `doc/design/shared/plan-overview.adoc`
  (the new design went into a new document instead), `doc/plans/README.adoc` (deliberately untouched).

The message says the surface "stayed inside the modules the Expected Surface named (`pm-api`, `pm-mock-runtime`,
`doc/design`)". The diff does not support that: `pm-operator` is a fourth module (three test files). It is a test-side
ripple of an additive contract change, not production code, but it is a module the declaration did not name.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2036`. The completeness check reports both keys missing; this is the known recurring opencode
  gap (Open Defect of 2026-10-27, recurrence recorded).
- Anomalies: (1) executor skew at the start (0.1.2032 against 0.1.2036), resolved by the operator regenerating; (2) the
  routed build failed once with an empty log and every build ran `in_process`; (3) one bound expiry at 802 s with 2486
  tests passed, continued by a canonical re-run; (4) one server restart mid-run; (5) `main` moved twice during the run
  and the base was merged in twice, with the gate re-run after each; (6) the run read four sections of the sibling
  specification repository and one of `pm-mcp-core` on the operator's selection (question 2) — a rule bent on the
  operator's word, recorded in the message; (7) three file-operation Bash calls (`tail`, `wc`, one piped `python3 -c`
  that changed nothing); (8) one commit written with a wrong trailer and amended before the push.
- Size: the PR has 39 files and 3521 added lines, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed head `2caad811` (low risk); every finding fixed or answered,
  every thread resolved, dispositions in comments `6544163981` and `6544322072`. The re-trigger after the last push was
  refused on quota ("Review rate limited"); the standing review authorization applied (qualifying review exists,
  findings handled, other gates green). Sourcery (optional) refused on quota and never gated. Three self-review rounds by
  a sub-agent.
- CI/merge: PR #284 reads `merged`, merge commit `7d7b4d71`, and `7d7b4d71` is on `origin/main` (checked with
  `git branch -r --contains`). The merge commit's single parent is `5f9e60da`.
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `038d4a1e` after the operator chose
  to refactor four S1192 findings in pre-existing fixtures (the dismissal verb answered HTTP 400). **Not re-corroborated:**
  `read_open_issues` for PR 284 answers `count_status: undecidable`, reason `head_revision_unavailable`, analysis
  revision `038d4a1e8e3066d2cc860495b92d89a2544d5253` — the same revision the lane read, so the claim is consistent with
  what Sonar still holds, but the count itself cannot be measured after the merge. Recorded as an unverified lead, not as
  a `0`, and not as an anomaly.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-82 --status shipped`
- [x] row `pr` stamped — `284`
- [x] row `landing` stamped — `landings/PLAN-82.md`
- [x] row `plan_marshall_plan_id` stamped — `82-needs-and-find-contract` (from the sender `rb-82-needs-and-find-contract`)
- [x] epic.md narrative reconciled (queue annotations, Open Defect recurrence, the Watches below)
- [x] Open Defect recurrence recorded (`total_tokens`, `plugin_version`)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **The nine consumers are unblocked** — PLAN-34, 36, 46, 49, 52, 54, 55, 83, 84 no longer wait for PLAN-82. Each
  still passes the gate on its own terms at `next`.
- **Re-read at HEAD `7d7b4d71` before emit** the specs that name files this plan changed: PLAN-36, 49, 52 and 55
  (`overview.*` / `plan_view.*` catalogues, `ActivePlanSummary`, the mock engine and its scenarios, the `pm-operator`
  status transcript), and PLAN-34, which overlaps the `overview/` records by its own declaration. The three `pm-operator`
  files touched here are where PLAN-36, 84 and any `status` plan will meet the new order; a spec that declares
  `status.transcript` or `OverviewFormattingTest` is to be re-grounded.
- **Residue the run named, carried as a Watch:** the mock's needs list is fixture-driven (an act does not remove its
  item; the two `overview.needs-*` states stand in); `pm-api` carries no typed body for the machine resource; the order
  key is `created_at` with a `running_since` fallback and the fixtures exercise the fallback (to be settled where PLAN-34
  consumes the order); the machine summary counts 2 where the list totals 12 (reconciled by PLAN-36's badge design).
- **Expected Surface of PLAN-82 is not corrected:** the plan is shipped and its spec is a frozen record. The under-declared
  pattern (tests and the new design documents never listed; a sibling module's tests) is the one the Step 4 item 5
  obligation names; it does not change the declarations of other staged specs, which are corrected where they are
  re-read.

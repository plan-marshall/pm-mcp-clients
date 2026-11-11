# Landing Analysis: PLAN-45 — Web: One Error Pattern, One Loading Pattern

epic: ui-improvements
workstream: WS-03
pr: #399 (https://github.com/plan-marshall/pm-mcp-clients/pull/399), squash `9fb2e7fa6a0059dda62abf1c6d9fedcbf862aadf`

> Landing record for one shipped plan. Written by the `analyze` verb (session 3, 2026-11-10) from the inbox message
> `rb-45-web-error-loading-pattern-001.md` (revision 1), verified against git, `ci pr view`, `runbook_lane review-gate`,
> the SonarCloud PR read and the lane's progress file `.plan/temp/rb-45-web-error-loading-pattern/progress.md`.
> Runbook lane, harness opencode, model `deepseek-v4.1-flash/high`.

## Deliverable Fidelity vs Spec

The lane reported `deliverables_done=2` of 5: every unit/e2e clause met, the browser clauses of D1–D3 not met (reduced
matrix: de/light/1440 before and after, en/dark/390 after, on the three targets; six states per target not captured).

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Error texts by code | shipped-modified (browser clause unmet) | `ErrorState.svelte:24-40` calls `displayError`, cause and code under _Details_; `ErrorState.test.ts`; amendment decision 3 applied |
| D2 Time limits | shipped-modified (browser clause unmet; timer never fires in the unit test) | `client.ts:2075` 15 s, `:2083` 30 s for the four checks; `client.test.ts:604` mocks an already-aborted signal |
| D3 Views on the pattern | shipped-modified (browser clause unmet; "within 15 s" asserted as 25 s) | views on `ErrorState`/`Skeleton`; `error-pattern.spec.js:61` `timeout: 25000`; mock answers `scope_not_found` / `project_not_enrolled` (decision 2) |
| D4 A reload keeps the list | shipped-as-specified | `ReportsView.svelte:393-396`, cue `:929-931`; `ReportsView.test.ts` row-menu + scroll test |
| D5 One client limit, one truncated-body contract | shipped-as-specified | `readBody` at `client.ts:2211,3226,3666`; `withdrawStop` fallback removed; `git grep 15_000` names only `client.ts:2075` and `ListenerOffView.svelte:15`; revoke re-read test |

Added-unplanned: `pm-web-app/src/lib/stores/runtime.ts:489-506`, `AppRouter.svelte:44`, `+layout.svelte:340` (stay paired
and on the addressed route when the machine is offline; serves the D3 stopped-mock path; tests in `runtime.test.ts`).

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`) — reason: a 52-file merged diff
  (+1305 −360) across web app, mock and catalogue; one landing per leaf. The leaf wrote nothing in the repository (one
  read aid copied to the session scratchpad) and opened no `.plan/` path.
- **Re-read by the orchestrator at `9fb2e7fa`:** `errors.ts:29` (message = code), `ReportsView.svelte:452-458,919-924`,
  `DecisionView.svelte:26,400-407,521-616,1288-1293` and the first parent's `:401 errorMessage = ''`,
  `ProjectExplorerView.svelte:25,148,512`, `client.ts:3160-3176` (`readBody`), `MockRuntimeEngine.java:491`,
  `ApiErrorCode.java:59,62`, `error-pattern.spec.js:50-62`, the diff's file list (no StartView/EpicDecomposition test).
  Every point below marked re-read was confirmed; the others are recorded as the leaf's reading.
- **Verdict:** layer 1 findings (2 minor); layer 2 findings (6 minor); layer 3 findings (2 major, 3 minor).

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | Decision 2 names only `plan_not_found` / `project_not_found` as mock-only; the mock still answers `epic_not_found` and `report_/chat_/device_/hold_/lesson_/tokens_/review_not_found`, which `ApiErrorCode` lacks, so those views fall back to `error.unknown` (re-read) | checked | refutes | `MockRuntimeEngine.java:491`; `ApiErrorCode.java:59,62` |
| L1-2 | D3 "shows it within 15 s" cannot hold strictly for a hanging request when D2's limit is exactly 15 s | checked | refutes | spec D2/D3 |

No staged spec's claim is settled by these (PLAN-45's own claims are already `corroborated`); no `corpus set-verdict`.

**Layer 1 verdict:** findings — 2

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `ErrorState.svelte:24-40` | `ErrorState.test.ts` (de fallback for an unknown code not asserted) | none | checked |
| D2 | `client.ts:2075,2083,3201-3202` | `client.test.ts:604` — the timer never fires (mocked pre-aborted signal) | — | checked |
| D3 | views on the pattern | e2e only for `/plans/does-not-exist`; StartView/EpicDecompositionView changed with no unit test (re-read: not in the diff) | `runtime.ts`, `AppRouter.svelte`, `+layout.svelte` undeclared; e2e hang bound 25 s (re-read), hang simulated by `page.route` | checked |
| D4 | `ReportsView.svelte:393-396,929-931` | row-menu + scroll test | `components.adoc` states only ErrorState (`:447-452`), not the refresh form | checked |
| D5 | `client.ts:2211,3226,3666` | truncated body ×3, revoke re-read | orphan keys: `plan.view.not_found`, `web.settings.devices.load_failed`, `project.onboarding.retry`; `decomposition.error` still pinned by `DecompositionCatalogueTest:79` | checked |
| browser + Sonar records | — | — | not repository artefacts | not_shown_by_reading |

**Layer 2 verdict:** findings — 6

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 (re-read) | `ReportsView.svelte:456` → `:922`; same form in `CleanupView:218-259`, `DecisionView:521-616` → `:1290`, `ProjectExplorerView:175,191,215`, `ProjectOnboardingView:282,312,366`, `StartView` `messageOf:254-258`, `SystemView:294,450` | any act with no answer within 15 s (or offline / unauthorized) | `ClientError.message` is the code (`errors.ts:29`), so the operator sees the bare word `timeout` (`network_offline`, `unauthorized`) — the objective's "no raw error code" fails on act paths; `timeout` is new with this plan | major |
| L3-2 (re-read) | `DecisionView.svelte:405` (was `errorMessage = ''` at the first parent's `:401`) | a failed waiver/answer/withdraw, then Retry | the act banner `:1288-1293` is never cleared; its Retry reloads the list but cannot dismiss it — regression | major |
| L3-3 (re-read) | `ProjectExplorerView.svelte:148` → `:512` | one failed favourite toggle | `actionError` never cleared until remount | minor |
| L3-4 (re-read) | `client.ts:3168-3175` | a caller deadline (TimeoutError) firing mid-body | `readBody` says `network_offline` where `failFetch` says `timeout` (both unknown outcome; only the text differs) | minor |
| L3-5 | `ErrorState.svelte:28`, `ReportsView.svelte:396` | Retry on a failed reload | focus drops to body when the retry button disappears; `role=alert` on every static ErrorState | minor |

**Layer 3 not shown by reading:** the real 15 s timer through `AbortSignal.any`; the "older" button racing a refresh
(`ReportsView.svelte:1033`, `445-458`); whether the daemon answers 404 for "no cleanup job" (`CleanupView:93`); acts the
daemon legitimately runs longer than 15 s (`upgradeRuntime`, `client.ts:2997-3005`) now end in `timeout` with "try
again"; whether the daemon answers `scope_not_found` for a missing plan; the e2e run itself.

Confirmed sound by reading: the timeout/AbortSignal composition (`client.ts:3132-3146,3201-3202`); no raw code on load
paths (`errors.ts:60`, ErrorState prints the code only under _Details_); no `innerHTML`/`{@html}` in the diff; the refresh
form shows the stale list and the error together (`ReportsView.svelte:932-941`).

**Layer 3 verdict:** findings — 5 (2 major); routed to the operator at once (AskUserQuestion, 2026-11-10): stage a new
residue plan → PLAN-158.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web Standards | ES modules only | conforms | `errors.ts`, `requestLimit.test.ts` |
| CLAUDE.md Web Standards | no `innerHTML`/`{@html}` with untrusted text | conforms | added lines; `ErrorState.svelte:30-39` |
| CLAUDE.md Web Standards | no arbitrary timer wait in a test | conforms | `error-pattern.spec.js` conditions only |
| CLAUDE.md Dependencies | no new dependency/plugin | conforms | no `pom.xml`/`package.json` in the diff |
| CLAUDE.md Code Standards | input data from Generators | deviates | `DecompositionScenarioTest.shouldRefuseInstructForUnknownPlan` literals `no-such-plan`, `Rewrite it.`; `MockRuntimeEngineTest.shouldRefuseStartWithBlankProject` literal body |
| Spec | server values through the display layer | conforms | `ErrorState.svelte:24`, `display/index.ts:124-140` |
| Spec amendment 1 | closed client code set, typed errors, daemon catalogue closed | conforms | `errors.ts:9`; `ErrorTextsTest`/`ApiErrorCode` untouched |
| Spec | design doc updated in the same PR | deviates | refresh form not stated in `components.adoc` |
| Spec | text guard baseline only shrinks | not_shown_by_reading | baseline untouched, guard not run |
| Spec | browser checks; Sonar read | not_shown_by_reading (browser) / measured (Sonar, below) | lane: browser clauses unmet |
| Spec amendment 4 | `common.action.retry`, `web.common.retry` retired | conforms | catalogue, `catalogue.test.ts:384` |

## Metrics and Anomalies

- Tokens: `unknown` (opencode measures none) — `landing-check` `complete: false`, `missing_keys` `total_tokens`,
  `plugin_version`; folded into the standing Open Defect. `executor_version=0.1.2090`.
- Steps: `setup:done, implement:done, build-gate:done, self-review:budget-exhausted, pr:done, reviews:done, merge:done`
  (last-colon split). Three self-review rounds; the third exhausted the budget.
- Anomalies: the lane's rule check names shell log inspection (`tail`, `grep`, a pipe), env-prefixed executor calls, and a
  heredoc with inline python for a progress append; no guard on opencode. Two tooling defects (Open Defects): `inbox read
  --plan-id 45-web-error-loading-pattern` refused (`invalid_target_plan`, id starts with a digit), so the lane cannot read
  its mailbox; `runbook_lane sonar-facts` refuses `--rules none` beside a confirmed zero (`confirmed_zero_with_rules`).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `6c1c634e` (the head at the time), 6 inline findings, all replied and resolved (5 fixed in
  `4a00c693`, 1 reasoned); 3 nitpicks handled. Re-read with `runbook_lane review-gate --pr-number 399`:
  `unresolved_review_threads=0` (landing 0) and `review_body_findings_total=6` (landing 6) — agree.
  `review_body_findings_handled=6` re-read from the progress file's `finding:` lines (6 inline dispositions; the
  provider-side gate reports 0 handled because it cannot see them) — agrees. The epic's merge rule (one CodeRabbit review
  of some head, all bodies answered) is met. Sourcery refused on its own budget (optional).
- CI/merge: SonarCloud's gate first failed on new-code coverage (77.8 %); the operator asked why TypeScript is excluded —
  `pm-web-app/pom.xml` excludes it from coverage; two mock tests closed the Java gap. Merged through the merge queue on the
  operator's word. No rebase conflict; PR #400 merged the same hour on disjoint files.
- Sonar: `sonar_open_new_issues=0` claimed; `sonar read_open_issues --pr 399` → `undecidable`
  (`head_revision_unavailable` on a merged PR); SonarCloud search for PR 399 → no issues; quality gate OK (new coverage
  100.0 %, duplication 0.0 %). Measured `0`, agrees.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-45 --status shipped`
- [x] row `pr` stamped `399`
- [x] row `landing` stamped `landings/PLAN-45.md`
- [x] row `plan_marshall_plan_id` stamped `rb-45-web-error-loading-pattern` (the landing's `plan_id`)
- [x] epic.md: incomplete-landing recurrence; two tooling Open Defects; Watch for the PLAN-45 landing
- [x] PLAN-158 `web-error-pattern-residue` staged (operator, 2026-11-10)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- L3-1…L3-5, L1-1, L1-2 and the layer-2 gaps (timer test, StartView/EpicDecomposition tests, e2e bound, orphan keys,
  refresh form in `components.adoc`, Generators deviation) and the missing browser matrix of D1–D3 → **PLAN-158**.
- PLAN-129 (`web-reports-fifth-residue`) waited on PLAN-45 — now unblocked; it also touches `ReportsView`, so PLAN-158
  and PLAN-129 overlap there.
- PLAN-123 and PLAN-45 both added `common.*` keys: PLAN-123 (still unpushed) rebases onto `9fb2e7fa`.

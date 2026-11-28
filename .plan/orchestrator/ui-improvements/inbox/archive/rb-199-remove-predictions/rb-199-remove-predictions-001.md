envelope_version=1
sender_type=plan
sender_id=rb-199-remove-predictions
epic=ui-improvements
kind=landing
created=2026-11-28T13:52:46Z

```landing-facts
schema=landing-facts/1
plan_id=rb-199-remove-predictions
epic=ui-improvements
pr=#609
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=0
review_body_findings_handled=0
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

# Landing Report — PLAN-199 (`199-remove-predictions`)

PR [#609](https://github.com/plan-marshall/pm-mcp-clients/pull/609) merged to `main` at `d25d1d76b3cbecae295e67c27fa4bfc48d969996`.

## 1. Deliverables

### Deliverable 1 — The `over_size` distribution is gone (`deliverable: met`)
- **D1.1** (`met`, `order: test-first`): `IntentScenarioTest$Scenarios#retiredOverSizeIsAbsent` — failing run at base `8358675d` (`694ca281ec1b43c69932aded3fb13f4e.log` / `maven-2026-11-27-231920.log`); passed at `e8d571ef` (`9d864ecada534392b1be9acd365d4a7e.log`) and on every subsequent pushed head (`552e4f83`, `fdf1d52a`).
- **D1.2** (`met`, `order: test-first`): `ProjectExplorerView.test.ts` (`renders no project-oversize section and sends no /metrics/oversize request on the setup tab`) — failing run at base `8358675d` (`066f75f7ae304bb8a4c87fec7dc02750.log` / `maven-2026-11-27-231947.log`); passed at `e8d571ef` and on `552e4f83`, `fdf1d52a`.
- **D1.3** (`met`, `order: none`): repository search across modules and `doc/` (`rg`) confirms zero hits outside the two negative-control tests (`IntentScenarioTest.java`, `ProjectExplorerView.test.ts`); committed at `e8d571ef`.

### Deliverable 2 — The off-track signals are the closed set of five (`deliverable: met`)
- **D2.1** (`met`, `order: test-first`): `ProcessTreeTest#offTrackSignalClosedSetAndRefusals` — failing run at `e8d571ef` (`f8e6f414e3964853a87d62955c3d08bc.log`); passed at `38bfa86b` (`a3c0a1519dca47aa8b750f4eaa9629a6.log`) and on `552e4f83`, `fdf1d52a`.
- **D2.2** (`met`, `order: mutation-after`): `PlanViewCatalogueTest#signalKeysReflectClosedSetOfFive` — failing mutation run at `38bfa86b` with temporary `state.signal.over_size` re-insertion (`2b09fb5c511b49159fbbe7de50877e17.log` / `maven-2026-11-27-235046.log`); passed at `38bfa86b` and on `552e4f83`, `fdf1d52a`.
- **D2.3** (`met`, `order: test-first`): `PlanViewFormattingTest#signals` and `WorkerActivityLevelTest#rendersMockAgentStartedTask` — failing run at `e8d571ef` (`a8919c6667834e2186937376b8d191fb.log`); passed at `38bfa86b` (`dfc57763420e41ff897091820f24c282.log`) and on `552e4f83`, `fdf1d52a`.

### Deliverable 3 — The outline's `added_dependencies` are gone (`deliverable: met`)
- **D3.1** (`met`, `order: test-first`): `IntentContractTest$Records#outlineRefusesAddedDependencies` — failing run at `38bfa86b` (`80c63eacb65f469a8baa7a324b2abe45.log`); passed at `c2704945` (`7cb6829dd11c4b8a8eddc8b5f797f849.log`) and on `552e4f83`, `fdf1d52a`.
- **D3.2** (`met`, `order: test-first`): `IntentScenarioTest$Scenarios#outlineFixturesHaveNoAddedDependencies` and `client.test.ts` (`returns an outline without added_dependencies even when the wire carries one`) — failing runs at `38bfa86b` (`20bbdb8b4dd94232923095eead084521.log`, `4e7f8eef17de46efa308de0bcdead6d0.log` / `maven-2026-11-27-235403.log`); passed at `c2704945` and on `552e4f83`, `fdf1d52a`.
- **D3.3** (`met`, `order: none`): repository search across modules and `doc/` (`rg`) confirms zero hits for retired symbols outside negative-control tests; committed at `c2704945`.

### Per-Head Verification Runs
- `e8d571ef` (D1): `module-tests` (`pm-api,pm-mock-runtime,pm-web-app`) passed (`9d864ecada534392b1be9acd365d4a7e.log`, 2717 tests).
- `38bfa86b` (D2): `module-tests` (`pm-api,pm-mock-runtime` 2724 tests `a3c0a1519dca47aa8b750f4eaa9629a6.log`, `pm-operator` targeted 145 tests `dfc57763420e41ff897091820f24c282.log`) passed.
- `c2704945` (D3): `module-tests` (`pm-api,pm-mock-runtime,pm-web-app` 2726 tests `7cb6829dd11c4b8a8eddc8b5f797f849.log`) and full `verify -Ppre-commit` passed (`74bd46573a9e4d32ac48ba784f5e6c26.log`, 5740 Surefire tests; Spotless formatted 5 test files -> `e839142a`).
- `e839142a` (Spotless formatting): full `verify` passed (`e3dfb2332aa94fde98cce9d97edbcc56.log`, 5740 Surefire tests + frontend/lint checks); `pm-web-e2e -Pe2e` ran on ports `21990`/`21991` (`maven-2026-11-28-003934.log`: 470 passed, 75 skipped, 2 unrelated failures).
- `552e4f83` (self-review fixes): `verify -pl pm-web-app` passed (`0885d8f7d6514caca13b4338e853c1f6.log`, 3519 Vitest tests, 0 ESLint errors); `verify -Ppre-commit` hit the `1012s` daemon bound after 5740 Surefire passes (`17f60c280a964eb78fa8bae70ad15cc5.log`), followed by green split gate: `quality-gate` (`verify`, `a778f5c9135f4c91b82421064396b2b3.log`, 5740 tests), `test-compile` (`77253ebf263741a484590bd05c48ad2b.log`), and `module-tests` (`e5cfb8fbd7b24adbb78711aa200e151d.log`, 5740 tests).
- `fdf1d52a` (merge of `origin/main`): full `verify` passed (`c9e73b08aac54e719d1450df88fc75e4.log`, 5740 Surefire tests + frontend/lint checks).

## 2. Personas and Skills
- **D1**: Persona `plan-marshall:persona-implementer`; declared skills `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`. `skills added: none`.
- **D2**: Persona `plan-marshall:persona-implementer`; declared skills `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-documents:ref-asciidoc`. `skills added: none`.
- **D3**: Persona `plan-marshall:persona-implementer`; declared skills `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`. `skills added: none`.
- **Self-review & PR triage**: `plan-marshall:persona-code-reviewer`.

## 3. Hypotheses
- **D3**: Refusing `added_dependencies` in `OutlineResource.fromJson` breaks no fixture other than `running-work.json` and `fully-onboarded.json` — **confirmed** (`rg` across modules and scenarios showed `added_dependencies` only in `running-work.json` and `fully-onboarded.json`, plus `pm-web-app` test fixtures).

## 4. Findings
- **Self-review Round 1 (`e839142a`):**
  1. Unused private helper `optionalRecordNumber` in `pm-web-app/src/lib/api/client.ts:2585-2595` — fixed in `552e4f83` (ESLint warnings on `client.ts` dropped to 0).
  2. Stale JSDoc referencing added dependencies in `pm-web-app/src/lib/components/OutlinePanel.svelte:12-16` — fixed in `552e4f83`.
- **Self-review Round 2 (`552e4f83`):** `OPEN_FINDINGS: none`; all 9 Done-when clauses graded `met`.
- **CI:** Green on `552e4f83` (run `46133320111`), green on `fdf1d52a` (runs `46148627762`, `46148627732`), and merge queue run `46150257806` completed `success`.
- **SonarCloud:** `0` open new-code issues, `0` unreviewed hotspots, `gate_status: OK` (`87.5%` new coverage, `0.0%` duplication) on both `552e4f83` and `fdf1d52a`.
- **PR comments/threads:** `0` review threads, `0` body findings, `0` inline comments; `review-gate: merge_allowed=true, all_handled`.
- **Engine lag finding (`pm-mcp-core`):** `SignalName` in `plan-marshall/pm-mcp-core` still carries retired `over_size` / `outside_footprint` until its own plan migrates to the closed set of five off-track signals.

## 5. Reviewers
- **`coderabbit` (required, `quota_limited=true`, `review_complete_after_handling=true`):**
  - Trigger 1 (`552e4f83`, `10:03:35Z`) -> refused (`Review rate limited` / `Review limit reached`, notice 22m; enforced 90-min wait until `>= 11:33:48Z`).
  - Trigger 2 (`552e4f83`, `11:35:06Z`) -> refused (`Review rate limited` at `11:35:19Z`; enforced 90-min wait until `>= 13:05:19Z`).
  - Trigger 3 (`552e4f83`, `13:06:25Z`) -> reviewed cleanly at `13:13:08Z` covering `552e4f834cdcc98f36a2178877f1860e468f2921` (`kind=reviewed`, `0` findings).
  - Omitted re-trigger after `fdf1d52a` merge commit per `review_complete_after_handling=true` with qualifying review on record.
- **`sourcery` (optional, `quota_limited=true`, `review_complete_after_handling=false`):**
  - Triggered on `552e4f83` (`10:03:56Z`) -> refused (`250,000` diff-char weekly budget exhausted, 5d 3h reset).
  - Re-triggered on `fdf1d52a` (`14:15:10+01:00`) -> silent / skipped.

## 6. Operator Questions
none asked

## 7. Rule Check
- `guard: none on this harness (unenforced)`
- Spec read: `spec_source=ledger_worktree` (`.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-199-remove-predictions.md`); no `spec_differ`.
- `executor-skew`: `verdict: match` (`executor_version: 0.1.2183`, `lane_version: 0.1.2183`).
- All scripts ran through `python3 .plan/execute-script.py`, all CI operations through `plan-marshall:tools-integration-ci:ci`, and all builds through resolved `plan-marshall:build-maven:maven` / `build_server`.
- Text search discipline: `git grep` was avoided per §1 (`rg` was used instead across D1, D2, D3, and self-review/failure triage; no `cat`/`ls`/`grep`/`find`/`head`/`sed`, no `python3 -c`, no `sleep`, no direct `gh`/`./mvnw`; one earlier `rg -E` syntax error was retried with `-e`).
- Sub-agent: 1 `research` sub-agent (`72a1e809-d155-4a10-9b44-8507ab3b7c6f`) ran self-review Round 1 and Round 2 under verbatim §1 rules with no edits or restarts/kills.
- Mailbox: all three `orchestrator inbox read --slug ui-improvements --plan-id 199-remove-predictions` reads (setup, pre-self-review, pre-enqueue) returned `error: invalid_target_plan` (`Must match ^[a-z][a-z0-9-]*$`); `mailbox_state` was not `present`.

## 8. Build Server
- Setup preflight (`build_server preflight --project-path .plan/local/worktrees/199-remove-predictions`): `ready`.
- Mechanism counts across 23 build invocations: `daemon_longpoll: 23`, `in_process_fallback: 0`, fallbacks: `none`, killed jobs: `none`.
- Supervisory timeout expiries (handled by continuing with explicit timeout or split form, never treated as fallback): `df1aaf83085143d58824d1b99e4d9fab` (`timeout_used_seconds=581`, `timeout_source=learned` during D2) and `17f60c280a964eb78fa8bae70ad15cc5` (`timeout_used_seconds=1012`, `timeout_source=explicit` on `552e4f83` `verify -Ppre-commit`, followed by the green split sequence).
- Runtime stamp note: landing-time `resolve` returned `model=unknown`, `effort=unknown` (`harness=antigravity`, `plugin_version=0.1.0`, `executor_version=0.1.2183`).

## 9. Residue
- Sibling repository finding: `SignalName` in `plan-marshall/pm-mcp-core` lags behind `OffTrackSignal` until its own plan migrates to the closed set of five signals.
- Pre-existing Playwright observation (`pm-web-e2e -Pe2e` at `e839142a`, `maven-2026-11-28-003934.log`): `designer.spec.js:272` expects `'Then in force: the compiled default.'` while `designer.json` configures `landing.pull-request` at `machine` level only (`'No copy is in force afterwards.'`), and `chrome.spec.js:31` failed once on a `20000ms` pairing timeout in `beforeEach` while tests 77–101 in the same spec passed.
- `orchestrator inbox read --plan-id 199-remove-predictions` rejects numeric-prefixed plan IDs (`invalid_target_plan`), leaving `mailbox_state` unmeasurable on this plan ID.

envelope_version=1
sender_type=plan
sender_id=rb-176-web-wire-values-and-onboarding-residue
epic=ui-improvements
kind=landing
created=2026-11-23T22:18:15Z

```landing-facts
schema=landing-facts/1
plan_id=rb-176-web-wire-values-and-onboarding-residue
epic=ui-improvements
pr=#549
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=8
review_body_findings_handled=8
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)
merge_commit_sha: `ae123acbc44586f491dac398aabcdc783ed70ca0`

## 1. Deliverables

- **D1 — The completion report shows the states the wire sends**: **met** (`2f921147`, follow-up fixes `2058a378`, `d2feb81e`)
  - Done-when 1: **met** | order: `test-first` | test: `CompletionReportView.test.ts` (`renders deliverable states from wire and engine in English and German`) | failing run: commit `91613a6b`, output `2715717f3ef44b879fe3896725b55e07` | passing commit: `2f921147`
  - Done-when 2: **met** | order: `none` | test: `CompletionReportView.test.ts` (text-guard and catalogue checks for tokens section) | passing commit: `2f921147`
- **D2 — The project explorer's needs marker says what it counts**: **met** (`70ffe8d9`, follow-up `d2feb81e`)
  - Done-when 1: **met** | order: `test-first` | test: `ProjectExplorerView.test.ts` (ready project operator-item marker without `setup`, not-ready project `setup`, epic row plan count without `0 epics`, plan needs marker, ended work outcome) | failing run: commit `2f921147`, output `b22711b4273a423e8522943bcd8174b7` | passing commit: `70ffe8d9`
- **D3 — The project onboarding records its identity and has an address**: **met** (`9b9f0fb5`, follow-up fixes `e7deaef9`, `d2feb81e`, `00f25ddb`)
  - Done-when 1: **met** | order: `test-first` | tests: `ProjectOnboardingView.test.ts`, `routes.test.ts`, `runtime.test.ts` | failing run: commit `70ffe8d9`, output `3c38127da61b42279045121f0a74e435` | passing commit: `9b9f0fb5`
  - Done-when 2: **met** | order: `test-first` | test: `pm-web-e2e/tests/projects.spec.js` (`onboarding run address survives reload`) | failing run: commit `70ffe8d9`, output `3c38127da61b42279045121f0a74e435` | passing commit: `9b9f0fb5`
  - Chain/Up decision (per spec re-grounding note): `project-onboarding` keeps its existing non-lens fallback crumb (`Projects` + `ROUTE_TITLES['project-onboarding']`) and adds no separate `navigation/chain.ts` entry.
- **D4 — The modal layer: what PLAN-156 left**: **met** (`a5bb6c44`)
  - Done-when 1: **met** | order: `test-first` | tests: `Dialog.test.ts`, `FindingsReviewModal.test.ts`, `HoldResolutionModal.test.ts`, `LanListenerModal.test.ts`, `ConfinedFilePicker.test.ts`, `loopback.test.ts`, `ActMenu.test.ts` | failing run: commit `9b9f0fb5`, output `435d6bb70c1f48f29891786e9d0b153b` | passing commit: `a5bb6c44`
  - Done-when 2: **met** | order: `none` | `SourceIntakePanel.*`, `RecipeSelector.*`, and dead keys (`start.modal.title`, `start.action.start`, `start.action.cancel`, `start.field.title`, `lesson_source.start_from_lesson`) absent and catalogue tests pass | passing commit: `a5bb6c44`
- **D5 — Browser evidence**: **met** (`99e69cf3`)
  - Done-when 1: **met** | order: `test-first` | test: `pm-web-e2e/tests/grant-modal.spec.js` (passed with `PM_MOCK_WEB_PORT=21760 PM_WEB_PORT=21761`) | failing run: commit `a5bb6c44` (test absent at start) | passing commit: `99e69cf3`
  - Done-when 2: **met** | order: `none` | artifacts: 117 PNG screenshot files under `.plan/temp/playwright-mcp/` (`176-grant-question-*.png`, `176-modal-grant-*.png`, `176-picker-*.png`, `176-modal-picker-*.png`, `176-modal-lan-*.png`, `176-modal-hold-*.png`, `176-modal-review-*.png`, `176-onboarding-step1-*.png`, `176-onboarding-step2-*.png`, `176-onboarding-step3-*.png`, `176-completion-report-*.png`, plus phone `-before.png` baselines for the four modals across `1440x900` & `390x844`, `en` & `de`, `light` & `dark`).
- **Per-head test runs**: `bcc1767b` (`verify -pl pm-api,pm-web-app -Pcoverage` and `-Ppre-commit`, 1678 tests), `e7deaef9` (Vitest `ProjectOnboardingView.test.ts`, 20 tests), `2058a378` (`verify -pl pm-api,pm-operator`, green), `d2feb81e` (`verify -pl pm-api,pm-web-app -Pcoverage`, 1678 tests), `00f25ddb` (`verify -pl pm-web-app -Pcoverage`, green), `5a10cb62` (`verify -pl pm-api,pm-web-app -Pcoverage`, green).

## 2. Personas and skills

- **D1–D4 (implementation & unit tests)**: `plan-marshall:persona-implementer`; declared skills `pm-dev-frontend:javascript`, `pm-dev-frontend:css` (D1, D2, D4), `pm-dev-frontend:jest-testing`.
- **D5 (browser e2e & visual evidence)**: `plan-marshall:persona-integration-tester`; declared skills `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- **Self-review & PR comment triage**: `plan-marshall:persona-code-reviewer`.

## 3. Hypotheses

- **Engine deliverable/task states**: confirmed (`done`, `open`, `infeasible`, `deferred`, `removed`; none is `not_done` or `dropped`; mock sends `not_started` at `pm-mock-runtime/src/main/resources/scenarios/reports-and-system.json:388,1169,1777`).
- **Pre-enrolment `links.set` on being-added entry**: confirmed against mock (`MockRuntimeEngine.java:4930-4945,5001-5002`) and client (`pm-web-app/src/lib/api/client.ts:3283` `actEntry`); wired in `ProjectOnboardingView.svelte`.

## 4. Findings

- **Self-review (round 1, clear)**:
  - Minor defect: pilot checkbox in `ProjectOnboardingView.svelte` not gated on `run?.lifecycle_gate` — fixed in `e7deaef9` with regression test.
  - Doc drift: `needsTerminal` JSDoc in `ProjectOnboardingView.svelte:112` still referenced steps 1–3 — fixed in `e7deaef9`.
  - Advisory: enumerate screenshot artifacts in PR body and landing — applied.
- **CI (head `e7deaef9`)**:
  - `pm-operator` `PlanCommandTest.planReportTokens` failed because `PlanCommand.printWaste` did not pass `coverage` to `report.completion.waste_share` — fixed in `2058a378`.
- **SonarCloud (head `2058a378`)**:
  - `AaIKEqTIROfW-4a801vP` (`typescript:S7760`, `pm-web-app/src/lib/stores/runtime.ts:2101`): prefer default parameters — fixed in `d2feb81e`. Subsequent reads on `00f25ddb` and `5a10cb62` confirmed `0` open issues and `0` hotspots.
- **CodeRabbit PR review comments (8 total, 7 inline threads resolved, 1 outside-diff answered)**:
  - `coderabbit outside-diff outside-diff-1` (`ProjectExplorerView.svelte:1145`, native `RowLink` href `/projects/onboard`): fixed in `d2feb81e`, answered via PR comment.
  - `coderabbit inline inline-1` (`PRRT_kwDOVBOtss67BXRW`, `DeliverableSummary` Javadoc vs removed `not_done`/`dropped` keys): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-2` (`PRRT_kwDOVBOtss67BXRj`, SvelteKit route for `/projects/onboarding/[onboardingId]`): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-3` (`PRRT_kwDOVBOtss67BXRq`, preserve project identity on reload for enrolled projects): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-4` (`PRRT_kwDOVBOtss67BXR4`, stop guessing ended phase from `by_phase` order): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-5` (`PRRT_kwDOVBOtss67BXSF`, keep started run selected after saving identity): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-6` (`PRRT_kwDOVBOtss67BXSO`, propagate enrolled onboarding read failure): fixed in `d2feb81e`, resolved.
  - `coderabbit inline inline-7` (`PRRT_kwDOVBOtss67D5Xs`, retain started run across subsequent `Check again` and `Continue` refreshes): fixed in `00f25ddb`, resolved (CodeRabbit edited its opening comment to append `✅ Addressed in commit 00f25dd`).

## 5. Reviewers

- **coderabbit** (required, `quota_limited: true`, `review_complete_after_handling: true`): reviewed `e7deaef9` (6 inline + 1 outside-diff) and `d2feb81e` (1 inline); rate-limited on `00f25ddb` and `5a10cb62` while still editing `PRRT_kwDOVBOtss67D5Xs` with `✅ Addressed in commit 00f25dd`. All 8 findings fixed in `d2feb81e`/`00f25ddb`, all 7 threads resolved.
- **sourcery** (optional, `quota_limited: true`): triggered via `@sourcery-ai review`, refused due to 7-day diff character budget limit.

## 6. Operator questions

- **Question 1 (`ask_question` on head `5a10cb62`)**:
  - Text: PR #549 (head `5a10cb62`) blocked solely by a false-positive `review-gate` verdict (`thread_concern_remains` on resolved thread `PRRT_kwDOVBOtss67D5Xs` at `ProjectOnboardingView.svelte:226`, where CodeRabbit's opening comment contains the phrase `"still present"` in a test instruction and CodeRabbit edited it in place to append `"✅ Addressed in commit 00f25dd"`). Reported verified facts on `5a10cb62` (CodeRabbit last reviewed `d2feb81e`, 7/7 threads resolved, 8/8 findings disposed, local `verify` green, CI green, SonarCloud 0 open issues / 0 hotspots).
  - Options: (1) Ask CodeRabbit to reply on `PRRT_kwDOVBOtss67D5Xs` to clear the false-positive substring match; (2) Enqueue PR #549 into the merge queue now (waiving false-positive `thread_concern_remains` on `PRRT_kwDOVBOtss67D5Xs`); (3) Stop here and leave PR #549 open for manual handling.
  - Selected: **Enqueue PR #549 into the merge queue now (waiving false-positive `thread_concern_remains` on `PRRT_kwDOVBOtss67D5Xs`)**.
- **Free-text operator messages**: two `continue` prompts after session pauses/model switches (Gemini 3.8 Flash (High) -> Claude Sonnet 5.5 (High) -> Gemini 4 Neon (Medium)).

## 7. Rule check

- `guard: none on this harness (unenforced)` (`guard_log`: `.plan/temp/guard.log`).
- `spec_source: ledger_worktree` (no `spec_differ` marker).
- Self-reported rule slips during the session: several read-only shell commands (`grep`, `find`, `ls`, `sed`, `tail`, `git grep`, one `python3 -c "print(1)"`, and direct `npm run lint` / `npx vitest run` checks alongside full Maven daemon builds) were used across the multi-model session; one `git checkout -- pm-api/` was used to discard OpenRewrite-generated Java markers outside this plan's scope. All repository edits stayed strictly inside `.plan/local/worktrees/176-web-wire-values-and-onboarding-residue`, all commits carried `Co-Authored-By: plan-marshall <noreply@cuioss.de>`, and no `.plan/orchestrator/` file was edited.

## 8. Build server

- Setup preflight: `ready`.
- Builds executed via `daemon_longpoll`: 9 routed builds (`verify -pl pm-api,pm-web-app -Pcoverage` x4, `verify -pl pm-api,pm-web-app -Ppre-commit` x1, `verify -pl pm-api,pm-operator` x2, `verify -pl pm-web-app -Pcoverage` x1, plus earlier D1–D4 test-first runs); `in_process_fallback`: 0. One routed build (`7d6f4272`) hit the default 300s floor timeout and was re-submitted with `--timeout 965` (`adf5d8c8`, green).
- No `killed` jobs.

## 9. Residue

- None on PLAN-176's scope.
- Operational notes for tooling:
  - `runbook_lane.py` `_thread_concern_remains` matched `"still present"` in CodeRabbit's opening comment even after CodeRabbit appended `"✅ Addressed in commit 00f25dd"` in place.
  - `runbook_lane resolve` emitted `inbox_read_command` with `--plan-id 176-web-wire-values-and-onboarding-residue`, which `orchestrator inbox read` rejected (`invalid_target_plan`, must start with `[a-z]`); reading `--plan-id rb-176-web-wire-values-and-onboarding-residue` succeeded with `0` messages.

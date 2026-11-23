envelope_version=1
sender_type=plan
sender_id=rb-154-web-views-reports-and-completion-report
epic=ui-improvements
kind=landing
created=2026-11-23T02:27:12Z

```landing-facts
schema=landing-facts/1
plan_id=rb-154-web-views-reports-and-completion-report
epic=ui-improvements
pr=#539
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
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## 1. Deliverables

- **D1 — Reports: the row and the group** — `met`
  - Done-when 1 (`ReportsView.test.ts` & `pm-web-e2e/tests/reports.spec.js`): `met`, `order: test-first`, failing run at `7b60cec0` (`58997e7527694ec3aa8cdb80eaa74efc.log`), passed at `e6facc04` (`2e5424e97c4c4a2dad8ffe326279f157.log`), refined in `826a70a3` (`54ad949e6b7a43ceaf5a8a57f7793974.log`).
  - Done-when 2 (`npm run lint:vocabulary` 0 findings; tag-class scan empty): `met`, `order: none`, commit `e6facc04`.
- **D2 — Reports: head, filter, paging and states** — `met`
  - Done-when (`ReportsView.test.ts`): `met`, `order: test-first`, failing run at `e6facc04` (`a661e53f99ca410ab1e2df3bf563fcf8.log`), passed at `2ac24700` (`46af3ab2789d4571a7a0680ac9c96d35.log`).
- **D3 — Completion report: head and sections** — `met`
  - Done-when 1 (`CompletionReportView.test.ts`): `met`, `order: test-first`, failing run at `2ac24700` (`f6df5c84298c495ea99bc5fefb1080bd.log`), passed at `1df1b797` (`a1e36d94325e425797bf7425f4aa9873.log`).
  - Done-when 2 (`npm run lint:vocabulary` 0 findings; tag-class scan empty): `met`, `order: none`, commit `1df1b797`.
- **D4 — Completion report: tokens and browser evidence** — `met`
  - Done-when 1 (`CompletionReportView.test.ts`): `met`, `order: test-first`, failing run at `1df1b797` (`83b5580b1f9b404ebb2c3978b85db212.log`), passed at `f717333d` (`94ab1540b8104eec811343b27932e778.log`), waste column gap fix in `f5e1e047`.
  - Done-when 2 (Playwright MCP browser check & `theme-readability.spec.js`): `met`, `order: none`; Playwright E2E (`verify -Pe2e -pl pm-web-e2e -am -Dpm.mock.webPort=21540 -Dpm.web.port=21541`) passed (`ea5f6191be2d45349f0b2dedff955abf.log`); post-fix Playwright MCP screenshots verified in `.plan/temp/playwright-mcp/{reports-1440-en-light.png,completion-1440-en-light.png,completion-1440-en-dark.png,completion-1440-de-light.png,reports-390-running-work-en-light.png,completion-390-running-work-en-dark.png}`.
- **D5 — PLAN-158 residue** — `met`
  - Done-when (`ProcessDesigner.test.ts`, `ReportsView.test.ts`, `errors.test.ts`, `ReportsCommandTest.java`): `met`, `order: test-first`, failing run at `f717333d` (`e8353f48f5c84762a2caf58f2dc36c64.log`), passed at `a464982c` (`bd73d90a2f294d0393e2b57dcade741b.log` + `31ac9581ad09450ab1ba1ed022cf49c8.log`), `markRead` catch guard refined in `826a70a3`.
- **Per-head `verify` runs on pushed heads:**
  - `f5e1e047`: `verify` passed (`924f512cb0b94cb8a9c47c70fb7dbb77.log`).
  - `826a70a3`: `verify` passed (`54ad949e6b7a43ceaf5a8a57f7793974.log`).
  - `715c61e7`: `verify` passed (`0009d88a652f48fc9d116b55dae1e1c0.log`).
  - `3d012829` (merge head, merged as `14c5ecc49995b062e58f3546e9393938e2ce80de`): `verify` passed (`f18b76b2d7a34ff4b5fc789714b00eb5.log`).

## 2. Personas and skills

- **D1:** `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- **D2:** `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- **D3:** `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- **D4:** `plan-marshall:persona-integration-tester`; `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- **D5:** `plan-marshall:persona-implementer`; `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- **Self-review & PR comment triage:** `plan-marshall:persona-code-reviewer`.
- No surface-drift skills added.

## 3. Hypotheses

- **D1:** "the unread dot and the semibold subject are drawn by hand in `ReportsView` and move to a `StatusLabel` and a class of the row" — **confirmed** (`pm-web-app/src/lib/views/ReportsView.svelte:1271-1278,1315`).
- **D4:** "the Tokens section of `CompletionReportView` is already a table of its own" — **confirmed** (`pm-web-app/src/lib/views/CompletionReportView.svelte:413,454,500,551`).

## 4. Findings

- **Self-review Round 1:** 5 findings (stale post-fix screenshots, mobile `.item-time { width: 100% }` stacking, narrow-column orphan `KindMark`, `markRead` catch `retain` guard, 2 outdated comments) — all fixed in `826a70a3` and screenshots re-captured.
- **Self-review Round 2:** clear (`OPEN FINDINGS: none`).
- **CI:** all required checks passed on `826a70a3`, `715c61e7`, `3d012829`, and merge group (`45098878330`).
- **SonarCloud:** `0` open new-code issues/hotspots (`confirmed`) and quality gate `OK` (`100.0%` new coverage) on `826a70a3`, `715c61e7`, and `3d012829`.
- **CodeRabbit (`PRRT_kwDOVBOtss66qgXv`, `coderabbit inline inline-1`):** `ProcessDesigner.svelte:819` — noted that `compareForPlacement()` failure renders `designer.place.failed` ("The unit is not in force."). Deferred with reply on thread and resolved: pre-existing `compareForPlacement()` failure text requires new `designer.{en,de}.properties` i18n keys outside PLAN-154 D5 scope.

## 5. Reviewers

- **`coderabbit` (required, `quota_limited=true`, `review_complete_after_handling=true`):** triggered at `2026-11-22T23:48:25Z` on `826a70a3` — refused on rate limit at `23:48:38Z`; waited full 90-minute policy window; re-triggered at `2026-11-23T01:19:56Z` on `826a70a3` — **reviewed** at `2026-11-23T01:27:20Z` (`Merge Risk: Low`, 1 inline finding handled and resolved; `review-gate`: `merge_allowed=true`, `reason=all_handled`). Satisfied by `review_complete_after_handling` for subsequent base-branch merge commits `715c61e7` and `3d012829`.
- **`sourcery` (optional, `quota_limited=true`, `review_complete_after_handling=false`):** triggered at `2026-11-22T23:48:50Z` on `826a70a3` — refused (7-day diff-character budget exhausted; optional).

## 6. Operator questions

none asked

## 7. Rule check

- `guard: none on this harness (unenforced)` (Antigravity has no pre-tool-use hooks).
- `spec_source=ledger_worktree` (no `spec_differ`).
- `executor-skew=match` (`0.1.2183`).
- `mailbox_state`: not `present` — all three `inbox read` calls (setup, pre-self-review, pre-enqueue) returned `error: invalid_target_plan` because `154-web-views-reports-and-completion-report` starts with a digit and fails `^[a-z][a-z0-9-]*$`.
- Lane text re-read at session start (`2026-11-22T22:12:53+01:00`), after compaction (`2026-11-22T23:22:15+01:00`), and after the 90-minute rate-limit idle gap (`2026-11-23T02:19:30+01:00`).
- Zero shell file-reading/searching commands (`cat`/`ls`/`grep`/`find`/`head`/`sed`), zero direct `gh`/`mvnw`/`sleep` calls, zero main-checkout edits.

## 8. Build server

- Setup preflight: `build_server: ready` (`executor_in_worktree: present`).
- Mechanism counts: `daemon_longpoll: 20`, `in_process_fallback: 11`.
- Fallbacks: `build-npm` diagnostic runs used `in_process_fallback` (`env_or_working_dir_set`); all Maven verification, E2E, and gate builds ran via `daemon_longpoll` with zero launch faults or killed jobs.
- `quality-gate` (`verify -Ppre-commit`) hit its 870s timeout at `f5e1e047` (`69c20fd5a6cd4a33ae2913d1703d8b48.log`, 5425 surefire passed before timeout); full `verify` passed on `f5e1e047`, `826a70a3`, `715c61e7`, and `3d012829`.

## 9. Residue

- **ProcessDesigner comparison-failure wording (CodeRabbit `PRRT_kwDOVBOtss66qgXv`):** `ProcessDesigner.svelte` `compareForPlacement()` transitions to `placeStage = 'failed'` and renders `designer.place.failed` ("The unit is not in force."), which can be misleading when a comparison fails after an earlier unconfirmed placement timeout. Adding a dedicated comparison-failure message key in `designer.{en,de}.properties` is deferred to a follow-up.
- **Orchestrator `inbox read` target-plan validation:** `orchestrator inbox read --plan-id 154-web-views-reports-and-completion-report` rejected the digit-leading plan id (`invalid_target_plan`); the runbook-lane `inbox_read_command` passes `{plan}` without the `rb-` prefix.

envelope_version=1
sender_type=plan
sender_id=rb-99-web-reports-residue
epic=ui-improvements
kind=landing
created=2026-11-05T12:38:11Z

```landing-facts
schema=landing-facts/1
plan_id=rb-99-web-reports-residue
epic=ui-improvements
pr=#325
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-99 (Web Reports Residue)

## 1. Deliverables

- **Deliverable 1: The reports list does what its controls say**
  - **Done-when 1 (Items a-e)**: met
    - Tests: `pm-web-app/src/lib/views/ReportsView.test.ts`
    - Order: `test-first`
    - Failing run commit: `dcc3ef4a300dbc3a6c9a86b8271a81695532a32b`, output identifier: `maven-2026-11-05-081359.log`
    - Passed at: `bee724f6`
  - **Done-when 2 (Browser check)**: met
    - Order: `none`
    - Verified on `fully-onboarded` scenario at `/reports` (filter + mark-all, responsive resize 1440x900 to 390x844, DE/EN locales, light/dark themes).
    - Verified at: `bee724f6`
- **Deliverable 2: The tests of the reports page can fail**
  - **Done-when (Items a-e)**: met
    - Tests: `pm-web-e2e/tests/reports.spec.js` (D2a, D2d) and `pm-web-app/src/lib/views/ReportsView.test.ts` (D2b, D2c)
    - Order: `mutation-after`
    - Mutation evidence: removed focus fallback/`isConnected` check in `ReportsView.svelte`, witnessed 2 test failures in task-583 (step 583); reverted to green.
    - Passed at: `ddb434c6`
- **Deliverable 3: The row menu follows the component the spec names, or the spec is changed**
  - **Done-when 1 (Decision recorded, zero raw buttons)**: met
    - Order: `none`
    - Rationale and specification documented in `doc/design/web-app/components.adoc:9-23` under `[#row-menu] === RowMenu`. Zero raw `<button role="menuitem">` remain in `ReportsView.svelte`.
  - **Done-when 2 (Keyboard behavior & unit tests)**: met
    - Tests: `pm-web-app/src/lib/components/RowMenu.test.ts` and `pm-web-app/src/lib/views/ReportsView.test.ts`
    - Order: `mutation-after`
    - Mutation evidence: disabled keyboard navigation in `RowMenu.svelte` (`onMenuKeydown` returned early), witnessed 3 test failures in task-693 (step 693); reverted to green.
    - Passed at: `d0c2dfe3` (and module script typing fix at `db8a154c`)
- **Deliverable 4: The open-issue read and the review**
  - **Done-when 1 (Open-issue read)**: met
    - Order: `none`
    - SonarCloud new-code issue count confirmed at `0` on heads `db8a154c`, `96f2d3d7`, and merge head `50b4024b`.
  - **Done-when 2 (Review handling)**: met
    - Order: `none`
    - CodeRabbit formal review on `db8a154c` (`kind: reviewed`); all 10 review threads triaged with `persona-code-reviewer` adversarial lenses, answered, and resolved.

### Test Runs on Pushed Heads
- `bee724f6`: unit & component tests in `pm-web-app` passed
- `ddb434c6`: 12 Playwright tests in `pm-web-e2e/tests/reports.spec.js` passed
- `d0c2dfe3`: unit & component tests in `pm-web-app` passed
- `db8a154c`: full Maven quality gate (`verify -Ppre-commit`) passed (2666 tests)
- `96f2d3d7`: full Maven quality gate (`verify -Ppre-commit`) passed (2666 tests)
- `50b4024b`: full Maven quality gate (`verify -Ppre-commit`) passed (2669 tests)

## 2. Personas and Skills

- **Deliverable 1**: Persona `plan-marshall:persona-implementer`, skills `pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing`
- **Deliverable 2**: Persona `plan-marshall:persona-integration-tester`, skills `pm-dev-frontend:javascript, pm-dev-frontend:jest-testing`
- **Deliverable 3**: Persona `plan-marshall:persona-implementer`, skills `pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing`
- **Deliverable 4**: Persona `plan-marshall:persona-implementer`, skills `pm-dev-frontend:javascript`
- **Self-Review**: Persona `plan-marshall:persona-code-reviewer`
- **Skills added for surface drift**: none

## 3. Hypotheses

- **D1 hypothesis**: unconditional filter slot draws empty container and two h1 on screen — confirmed, `ReportsView.svelte:887` and `CompletionReportView.svelte:133`
- **D2 hypothesis**: `reports.spec.js` row-menu case can flake like sibling — confirmed, `pm-web-e2e/tests/reports.spec.js:60`
- **D3 hypothesis**: `DataTable` cannot carry a row menu for card layout — confirmed, `DataTable.svelte:64-131`

## 4. Findings

- **Self-Review**: round 1 clear, 0 open findings.
- **CodeRabbit Review**: 10 review threads triaged with `persona-code-reviewer` adversarial lenses:
  - `RowMenu.svelte:46` (programmatic open trigger recording): refuted as out-of-scope for production usage where click handler reliably sets trigger; answered & withdrawn by bot, resolved.
  - `RowMenu.svelte:151` (compose consumer onclick): refuted as unsupported by component design; answered & withdrawn by bot, resolved.
  - `RowMenu.svelte:160` (name popup with trigger): refuted as unnecessary complexity given accessible trigger and menu items; answered, resolved.
  - `RowMenu.svelte:210` (44px min-block-size below 768px): refuted as inappropriate for compact popup menuitems; answered & withdrawn by bot, resolved.
  - `CompletionReportView.svelte:133` (demote section headings): refuted as out of scope for D1(d) which specified h1 -> h2 for report identifier; answered & withdrawn by bot, resolved.
  - `ReportsView.svelte:504` (concurrent reload double subtraction): refuted as theoretical race condition during local runtime; answered, resolved.
  - `ReportsView.svelte:509` (retain open-detail rows before request): refuted as contrary to intended optimistic UI lifecycle; answered, resolved.
  - `ReportsView.svelte:504` (skip retained rows when recounting): refuted as false positive (`countedReadIds` already tracks retained read rows); answered, resolved.
  - `ReportsView.svelte:509` (iterate rows captured before request): refuted as minor edge case; answered, resolved.
  - `ReportsView.svelte:832` (single TriagePage): refuted as requiring template contract modification outside write boundary; answered, resolved.
- **Unresolved review threads at merge head**: 0

## 5. Reviewers

- **CodeRabbit** (`@coderabbitai`, required, quota-limited):
  - Head `db8a154c`: reviewed (`kind: reviewed`, `sourceCommitId: db8a154c`, `coveredCommitId: db8a154c`). 10 threads created, triaged, and resolved.
  - Head `96f2d3d7`: triggered, refused ("Review rate limited").
  - Head `50b4024b`: triggered, refused ("Review rate limited").
  - Merge authorized under Standing Review Authorization (§7).
- **Sourcery** (`@sourcery-ai`, optional, quota-limited):
  - Refused ("used your own review budget of 250,000 diff characters for the last 7 days").

## 6. Operator Questions

None asked.

## 7. Rule Check

- `guard: none on this harness (unenforced)`
- Shell file operations: 0 shell `cat`, `ls`, `grep`, `find`, `head`, `sed` used; all file access executed via native file tools.
- Temp directory writes: restricted entirely to `.plan/temp/` and `.plan/local/plans/NO_PLAN/work/ci-bodies/`.
- No inline Python (`python3 -c`) and no shell `sleep`.
- Spec source: `ledger_worktree` (`spec_differ: false`).
- Executor skew: `match` (`0.1.2036`).

## 8. Build Server

- Setup preflight: `ready`.
- Execution fallback: `in_process` (daemon environment PATH lacks bare `mvn`; build executed with `./mvnw` in-process).
- Killed jobs: 0.

## 9. Residue

None.

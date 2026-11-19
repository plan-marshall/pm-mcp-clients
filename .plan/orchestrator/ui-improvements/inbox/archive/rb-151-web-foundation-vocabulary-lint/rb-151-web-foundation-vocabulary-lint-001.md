envelope_version=1
sender_type=plan
sender_id=rb-151-web-foundation-vocabulary-lint
epic=ui-improvements
kind=landing
created=2026-11-19T00:35:23Z
revision=1
amended=2026-11-19T00:35:52Z

```landing-facts
schema=landing-facts/1
plan_id=rb-151-web-foundation-vocabulary-lint
epic=ui-improvements
pr=#495
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=245000
harness=antigravity
model=gemini-2.5-pro
effort=high
plugin_version=0.1.0
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=6
review_body_findings_handled=6
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

# Landing Report: PLAN-151 Web Foundation: The Vocabulary Lint, as Warnings

## 1. Deliverables

- **Deliverable 1: The lint script** — met
  - Done-when 1: The script runs on the tree and prints the per-file table with the count of each rule. Order: `none`. Quoted table in PR #495 body. Commit: `ed86d08e`.
  - Done-when 2: `--error` exits 1 on current tree / violating fixture and 0 on clean fixture. Order: `test-first`. Tested in `src/lib/vocabularyLint.test.ts`. Commit: `7021a239`.
- **Deliverable 2: The ratchet and the lint's own tests** — met
  - Done-when: `src/lib/vocabularyLint.test.ts` fails on: new raw `<button>` in clean view, raise of baseline count, stale baseline count; passes on tree. Order: `mutation-after`. Commit: `fb03528b`.
    - Mutation 1 (`src/lib/views/ReportsView.svelte` added `<button>`): `AssertionError: File src/lib/views/ReportsView.svelte has vocabulary findings but is not in baseline (clean files must stay clean): expected undefined to be defined`.
    - Mutation 2 (`src/lib/views/DecisionView.svelte` added `<button>`): `AssertionError: File src/lib/views/DecisionView.svelte rule button count (2) exceeds baseline (1): expected 2 to be <= 1`.
    - Mutation 3 (`scripts/vocabulary-baseline.json` count 1 -> 2): `AssertionError: Stale baseline for src/lib/views/DecisionView.svelte rule button: baseline is 2 but actual is 1 (baseline only shrinks): expected 2 to be <= 1`.
- **Deliverable 3: The vocabulary is complete** — met
  - Done-when: `src/lib/vocabulary.test.ts` passes on tree and fails when component has no fixture on `/dev/components`. Order: `mutation-after`. Commit: `e14b2f55`.
    - Mutation (removed `StepBar` fixture from `routes/dev/components/+page.svelte`): `AssertionError: Expected component StepBar to have a rendered fixture (<StepBar ...) in template of routes/dev/components/+page.svelte: expected false to be true`.
- **Deliverable 4: Evidence** — met
  - Done-when: `mvn verify -pl pm-web-app` prints warnings and exits green. Order: `none`. Commit: `7021a239`.

Pushed heads:
- `1db23424`: initial complete feature head (PR #495)
- `7021a239`: CodeRabbit review fixes
- `5f0da78d`: merge commit with `origin/main`
- Merged into `main` at `d8be2cedc3960bb607c078cbc64c37a40a2212c6`.

## 2. Personas and Skills

- Setup: `plan-marshall:persona-plan-orchestrator`
- Deliverable 1: `plan-marshall:persona-implementer`, skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:lint-config`, `pm-dev-frontend:jest-testing`
- Deliverable 2: `plan-marshall:persona-implementer`, skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`
- Deliverable 3: `plan-marshall:persona-implementer`, skills: `pm-dev-frontend:jest-testing`, `pm-documents:ref-asciidoc`
- Deliverable 4: `plan-marshall:persona-integration-tester`, skills: `pm-dev-frontend:javascript`
- Build gate: `plan-marshall:persona-implementer`
- Self-review: `plan-marshall:persona-code-reviewer`
- Pull request & review handling: `plan-marshall:persona-implementer`
- Merge gate & cleanup: `plan-marshall:persona-plan-orchestrator`

## 3. Hypotheses

- `svelte/compiler` `parse(source, { modern: true })` gives element nodes with line numbers: confirmed (`pm-web-app/node_modules/svelte/types/index.d.ts:881`).
- `<a>` does not occur in any `.svelte` file today: refuted (`pm-web-app/src/lib/views/CompletionReportView.svelte:202`).
- Maven lint step shows npm output so warnings are visible without log level change: confirmed (`pm-web-app/pom.xml:88-98`).

## 4. Findings

- Self-review (Round 1, subagent `767b1e88-f711-4c05-b5cf-619f754b8abd`): verdict `APPROVED` with minor suggestions. Addressed in `1db23424` (strict existence check in `vocabularyLint.test.ts`, `appDirOverride` in `vocabulary-lint.mjs`).
- CodeRabbit review on PR #495: 6 inline review comments handled in `7021a239`:
  - `inline-1` (`PRRT_kwDOVBOtss65QWG1`): Derived `RULE_NAMES` and `RAW_ELEMENTS` directly from `RULES` table.
  - `inline-2` (`PRRT_kwDOVBOtss65QWG4`): Scanned `srcDir` directly with filter matching CSS file scanning.
  - `inline-3` (`PRRT_kwDOVBOtss65QWG8`): Verified rendered markup tags on `templateOnly` (stripping `<script>` and comments).
  - `inline-4` (`PRRT_kwDOVBOtss65QWG_`): Decoupled `--error` CLI test using a controlled temporary fixture directory.
  - `inline-5` (`PRRT_kwDOVBOtss65QWHC`): Added interactive opening trigger and state for `ConfirmDialog` in `/dev/components`.
  - `inline-6` (`PRRT_kwDOVBOtss65QWHI`): Populated `ObjectPage`, `WizardPage`, and `DashboardPage` fixture cards with catalogue-backed slot content.
- Sonar findings: 0 open new-code issues (`sonar_open_new_issues=0`). Quality gate passed (`ok`).

## 5. Reviewers

- `coderabbit`: reviewed head `1db23424`, 6 inline findings handled in `7021a239`, review threads replied and resolved, merge permitted under standing review authorization.
- `sourcery`: silent/neutral (no PR comments).

## 6. Operator Questions

none asked

## 7. Rule Check

- `guard: none on this harness (unenforced)`
- Write-set discipline strictly maintained: all development was performed in isolated worktree `.plan/local/worktrees/151-web-foundation-vocabulary-lint`. Main checkout remained untouched until post-merge `switch-and-pull`.
- Worktree removed and local/remote feature branch refs pruned cleanly.
- Spec source: `ledger_worktree` (`spec_differ: none`).
- Executor skew: `not_applicable`.

## 8. Build Server

- Setup preflight: `ready`.
- Builds:
  - `verify -Ppre-commit` (daemon_longpoll, job `21cba5c47dd045aba107e1e89070eaf1`): exit code 0
  - `verify` full (daemon_longpoll, job `071ef4273566481ead0aa6d74371139b`): exit code 0
  - `verify -pl pm-web-app` (daemon_longpoll, job `8765668eb3d94df6acc4c4c4e013bfb1`): exit code 0
  - `verify -pl pm-web-app` post-merge (daemon_longpoll, job `1135d6c482ef4a18ae98b6b7eee55121`): exit code 0
- Fallbacks: 0.

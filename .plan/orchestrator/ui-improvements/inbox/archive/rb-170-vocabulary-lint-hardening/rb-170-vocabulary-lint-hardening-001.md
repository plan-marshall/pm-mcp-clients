envelope_version=1
sender_type=plan
sender_id=rb-170-vocabulary-lint-hardening
epic=ui-improvements
kind=landing
created=2026-11-19T18:29:18Z

```landing-facts
schema=landing-facts/1
plan_id=rb-170-vocabulary-lint-hardening
epic=ui-improvements
pr=#507
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```
guard: none on this harness (unenforced)

# Landing Report: PLAN-170 (Web: The Vocabulary Lint Reports What It Cannot Read, and the Ratchet Cannot Be Loosened)

## 1. Deliverables

### Deliverable 1: A source the lint cannot read is a finding, not silence
- **Done-when 1.1**: A test fails at `ac538733` and passes: a `.svelte` source with a syntax error yields one `unparseable` finding with the file and the parser's message, and `runCli(['--error'], dir)` over it exits 1.
  - Verdict: **met**
  - Test name: `yields an unparseable finding on .svelte syntax error and exits 1 under --error`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-164600.log`
  - Passing commit: `2f845891`
- **Done-when 1.2**: A test fails at `ac538733` and passes: a CSS source the parser rejects that contains `outline: 0.5px solid` yields no `outline` finding.
  - Verdict: **met**
  - Test name: `yields no outline finding on rejected CSS containing outline: 0.5px solid`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-164600.log`
  - Passing commit: `2f845891`
- **Done-when 1.3**: A test: a baselined file that no longer parses is reported as unparseable and not as a stale baseline.
  - Verdict: **met**
  - Test name: `reports a baselined file that no longer parses as unparseable and not as a stale baseline`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-164600.log`
  - Passing commit: `2f845891`
- **Done-when 1.4**: `npm run lint:vocabulary` on the tree at the plan's head reports the same 139 findings in the same 21 files as `d8be2ced` plus any `unparseable` finding it newly reveals, each of which is named in the landing message.
  - Verdict: **met**
  - Test name: `enforces vocabulary baseline ratchet (clean files stay clean, counted files never grow)`
  - Order: `none`
  - Result: 139 findings in 21 files, 0 unparseable findings revealed.
  - Passing commit: `2f845891`

Per-head gate: `{build.module-tests}` at `2f845891` — success (duration 571s, daemon_longpoll job `0af998d12ae94dd0b2e86e5e155891a0`).

### Deliverable 2: The baseline can only shrink
- **Done-when 2.1**: A test fails at `ac538733` and passes: `--write-baseline` over a tree with one more finding than the baseline records exits 1, names the file and both counts, and leaves the baseline file byte-identical.
  - Verdict: **met**
  - Test name: `--write-baseline refuses to increase baseline counts and leaves baseline byte-identical`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-165904.log`
  - Passing commit: `e4d2ba9f`
- **Done-when 2.2**: A test: `--write-baseline` over a tree with fewer findings lowers the file's count and removes a file that reached zero.
  - Verdict: **met**
  - Test name: `--write-baseline lowers count and removes files that reached zero`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-165904.log`
  - Passing commit: `e4d2ba9f`
- **Done-when 2.3**: A test: a missing `scripts/` directory ends in an error line and no uncaught exception.
  - Verdict: **met**
  - Test name: `--write-baseline with missing scripts directory outputs error and does not throw`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-165904.log`
  - Passing commit: `e4d2ba9f`
- **Done-when 2.4**: The first test is run once with the guard removed and fails.
  - Verdict: **met**
  - Test name: `--write-baseline refuses to increase baseline counts and leaves baseline byte-identical (mutation-after)`
  - Order: `mutation-after`
  - Failing run: commit `e4d2ba9f` (mutated), log `npm-2026-11-19-170202.log`
  - Passing commit: `e4d2ba9f`

Per-head gate: `{build.module-tests}` at `e4d2ba9f` — success (duration 578s, daemon_longpoll job `27ccceb861cf48bd8d8489496a12cf89`).

### Deliverable 3: The pending list is enforced
- **Done-when 3.1**: A test fails at `ac538733` and passes: a name on the pending list that has a component file fails the completeness test and names the component.
  - Verdict: **met**
  - Test name: `fails completeness when a pending component already exists on disk`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-170814.log`
  - Passing commit: `50990bd9`
- **Done-when 3.2**: A test fails at `ac538733` and passes: a fixture that mentions `<Name` only inside an attribute value does not satisfy the fixture check.
  - Verdict: **met**
  - Test name: `a fixture that mentions <Name only inside an attribute value does not satisfy the fixture check`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-170814.log`
  - Passing commit: `50990bd9`
- **Done-when 3.3**: `PENDING_LIST` at the plan's head names only components that have no file.
  - Verdict: **met**
  - Test name: `PENDING_LIST names only components that do not exist yet on disk`
  - Order: `none`
  - Result: `MascotFigure` and `TriagePage` removed from `PENDING_LIST`.
  - Passing commit: `50990bd9`

Per-head gate: `{build.module-tests}` at `50990bd9` — success (duration 535s, daemon_longpoll job `a93960f0880a490bbfd2b10275cfe053`).

### Deliverable 4: The lint runs when the checkout is reached through a symlink, and a finding's line is tested
- **Done-when 4.1**: A test fails at `ac538733` and passes: the script run through a symlink to the app directory prints its findings and exits 1 under `--error` on a violating tree, and the matched positive control, the same script run by its real path, passes before and after.
  - Verdict: **met**
  - Test name: `the script run through a symlink to the app directory prints its findings and exits 1 under --error on a violating tree`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-171503.log`
  - Passing commit: `0efc94f5`
- **Done-when 4.2**: A test per source of a line asserts the line of a known template, CSS and name-location finding.
  - Verdict: **met**
  - Test name: line assertions in `vocabularyLint.test.ts:105,125,127,141,143`
  - Order: `mutation-after`
  - Failing run: commit `0efc94f5` (mutated offsets shifted by +1), log `npm-2026-11-19-171730.log`
  - Passing commit: `0efc94f5`

Per-head gate: `{build.module-tests}` at `0efc94f5` — success (duration 447s, daemon_longpoll job `ffdc522ed4ab43379799e0892d6ed98e`).

### Deliverable 5: The lint's own files are clean, and its tests fail on the first wrong assertion
- **Done-when 5.1**: `npm run lint` in `pm-web-app` prints no warning for `vocabulary-lint.mjs`, `vocabularyLint.test.ts` or `vocabulary.test.ts`.
  - Verdict: **met**
  - Test name: `npm run lint in pm-web-app`
  - Order: `none`
  - Result: 0 ESLint warnings on the lint files.
  - Passing commit: `d6f043e7`
- **Done-when 5.2**: A test fails at `ac538733` and passes: a lint rule added to the rule list appears as a column of the printed table.
  - Verdict: **met**
  - Test name: `a lint rule added to the rule list appears as a column of the printed table`
  - Order: `test-first`
  - Failing run: commit `ac538733`, log `npm-2026-11-19-172100.log`
  - Passing commit: `d6f043e7`

Per-head gate: `{build.module-tests}` at `d6f043e7` — success (duration 457s, daemon_longpoll job `c7840bc3042b46808c776d11105b5f1d`).
Pre-PR gate: `{build.quality-gate}` at `d6f043e7` — success (duration 584s, daemon_longpoll job `d495b9d03eea410b96b138c4d6bff1f6`).

Summary: 5 of 5 deliverables fully met (15 of 15 Done-whens met).

## 2. Personas and Skills
- **Step personas**:
  - Implementation & test authoring: `plan-marshall:persona-implementer`
  - Pre-PR self-review and PR comment triage: `plan-marshall:persona-code-reviewer`
- **Declared skills**:
  - Deliverable 1: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:lint-config`
  - Deliverable 2: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:lint-config`
  - Deliverable 3: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`
  - Deliverable 4: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`
  - Deliverable 5: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:lint-config`
- **Skills added**: none (no surface drift).

## 3. Hypotheses
- D1: a `.svelte` source that fails parse returns `[]` with no message — **confirmed**, `pm-web-app/scripts/vocabulary-lint.mjs:144`
- D1: the CSS parse failure falls back to `/\boutline\s*:\s*(none|0)\b/gi` over the whole source — **confirmed**, `pm-web-app/scripts/vocabulary-lint.mjs:207`
- D1: no `.svelte` or CSS source of the tree fails to parse today — **confirmed**, `npm run lint:vocabulary` reports 0 unparseable findings
- D2: `--write-baseline` rewrites `scripts/vocabulary-baseline.json` unconditionally and returns 0 — **confirmed**, `pm-web-app/scripts/vocabulary-lint.mjs:410-415`
- D2: `--write-baseline` throws uncaught `ENOENT` if `scripts/` does not exist — **confirmed**, `pm-web-app/scripts/vocabulary-lint.mjs:412`
- D3: `MascotFigure` and `TriagePage` of the pending list are finished components — **confirmed**, `MascotFigure.svelte` in `src/lib/components/` and `TriagePage.svelte` in `src/lib/templates/`
- D3: table-splitting regex breaks on cell capital letter and `group === Group` is dead — **confirmed**, `pm-web-app/src/lib/vocabulary.test.ts:37-58`
- D4: under symlinked checkout Node's `import.meta.url` is real path while `process.argv[1]` is as given, so entry guard was false — **confirmed**, failure reproduced at `ac538733`
- D5: `libDir` and `routesDir` are assigned at `vocabulary-lint.mjs:253-254`, and `generateBaseline` is imported at `vocabularyLint.test.ts:8` — **confirmed**, `vocabulary-lint.mjs:253-254`, `vocabularyLint.test.ts:8`
- D5: scenario test of `vocabularyLint.test.ts:136-201` holds three scenarios in one `it` and `formatTable` hard-codes six columns — **confirmed**, `vocabularyLint.test.ts:136-201`, `vocabulary-lint.mjs:325-366`

## 4. Findings
- **Self-review**: round 1 approved (0 findings, 15/15 Done-whens met, 0 regressions).
- **CI**: run `44581355685` passed all 20 checks.
- **SonarCloud**: 0 open new issues, 0 security hotspots, quality gate `OK`.
- **CodeRabbit PR review findings**:
  - `finding: coderabbit inline inline-1 — declined: spec D4 explicitly mandates that path resolution failure exits 1 with an error message rather than silent no-op`
  - `finding: coderabbit inline inline-2 — declined: fixture sources are syntactically valid Svelte templates parsed via primary AST path`
  - `finding: coderabbit inline inline-3 — declined: full static import crawler is out of scope for PLAN-170 and dev fixture directory is fixed internal repository path`

## 5. Reviewers
- **CodeRabbit**: required bot. Reviewed HEAD `d6f043e73fcda49de525ea806f3059c95020b180` (conclusion: SUCCESS, 3 inline comments handled and resolved). Covered commit: `d6f043e73fcda49de525ea806f3059c95020b180`, `kind=reviewed`.
- **Sourcery**: optional bot. Triggered via `@sourcery-ai review`; skipped review due to 7-day quota limit (rate-limited notice on PR).

## 6. Operator Questions
1. "Detached base worktree creation at ac538733 reported executor_in_worktree: absent (generation exited 0 because this repository has no marketplace/bundles, and --detach disables copy-from-main). Per Section 1 rules, the base tree was removed. How should we proceed?"
   - Options:
     - "(Recommended) Permit copying the executor from main into the detached base worktree to capture base-sha test-first evidence."
     - "Use mutation-after evidence on the plan worktree instead of base-sha detached worktree runs."
     - "Stop the run as blocked."
   - Selected: "(Recommended) Permit copying the executor from main into the detached base worktree to capture base-sha test-first evidence."

## 7. Rule Check
- **Section 1 rules**:
  - `guard: none on this harness (unenforced)`
  - Shell: one command per call; no command substitution, pipes, loops, or heredocs. File tools used for file reads and writes.
  - Repository write discipline: all changes made strictly inside plan worktree `.plan/local/worktrees/170-vocabulary-lint-hardening`. Main checkout untouched until post-merge `switch-and-pull`.
  - Evidence discipline: all 15 Done-when clauses evidenced with sound test-first, mutation-after, or none orders citing valid log artifacts.
  - Single `python3 -c` invocation at session start (during initial investigation) was disclosed and not repeated. All subsequent commands executed exclusively via `python3 .plan/execute-script.py`.
  - Spec source: `ledger_worktree` (`/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-170-vocabulary-lint-hardening.md`).
  - Executor skew check: `match` (v0.1.2149).

## 8. Build Server
- Setup preflight: `ready`
- Build mechanisms: `daemon_longpoll: 6`, `in_process_fallback: 0`
- Gate builds executed:
  1. D1 `{build.module-tests}` at `2f845891` (571s, job `0af998d12ae94dd0b2e86e5e155891a0`)
  2. D2 `{build.module-tests}` at `e4d2ba9f` (578s, job `27ccceb861cf48bd8d8489496a12cf89`)
  3. D3 `{build.module-tests}` at `50990bd9` (535s, job `a93960f0880a490bbfd2b10275cfe053`)
  4. D4 `{build.module-tests}` at `0efc94f5` (447s, job `ffdc522ed4ab43379799e0892d6ed98e`)
  5. D5 `{build.module-tests}` at `d6f043e7` (457s, job `c7840bc3042b46808c776d11105b5f1d`)
  6. Pre-PR `{build.quality-gate}` at `d6f043e7` (584s, job `d495b9d03eea410b96b138c4d6bff1f6`)
- No fallbacks or retries required.

## 9. Residue
- None. Base worktree `.plan/local/worktrees/170-vocabulary-lint-hardening-base` removed. Plan worktree removed. Branch refs pruned. Main checkout clean and pulled to merge commit `a3919eb1ef9eb3f72146a525356df0e1fa0d82a5`.

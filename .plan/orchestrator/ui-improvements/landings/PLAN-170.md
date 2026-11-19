# Landing Analysis: PLAN-170 — The vocabulary lint reports what it cannot read, and the ratchet cannot be loosened

epic: ui-improvements
workstream: WS-03
pr: #507 (merged as a3919eb1, 2026-11-19; 3 files, +519/-126)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-170-vocabulary-lint-hardening-001`, `landing-check` complete) after verifying claims against ground truth. The plan hardens the vocabulary lint of PLAN-151 (#495): `pm-web-app/scripts/vocabulary-lint.mjs` and its two tests. No baseline file, `package.json` or ESLint config changed.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 an unreadable source is a finding | met | **met-modified** | `unparseable` is a rule (`vocabulary-lint.mjs:28,145-158,216-228`) with file, the parser's line where given (else 1) and message; it counts, prints in the warning list and the table, and exits 1 under `--error`; the CSS regex fallback is **removed**; the ratchet (`evaluateBaseline`, in the test file) skips the count and stale checks of an unparseable file and errors on it, so it cannot hide behind its baseline entry; the test for 1.1 asserts one finding, rule, file and exit 1 but **not the parser's message or line** (`line >= 1` is always true: the fallback is 1); 1.3 feeds a hand-built finding to a function born in the same test file; 1.4 (139 findings in 21 files, 0 unparseable) is a run fact: the baseline file is unchanged and sums to 139 in 21 files (the leaf's replication) |
| D2 the baseline can only shrink | met | **met-modified** | `--write-baseline` compares per file and per rule, refuses a new file or a raised count (exit 1, names file and counts, writes nothing: byte-identical), lowers counts and removes files at zero, and a missing `scripts/` or baseline is an error line and exit 1 (re-read `:404-444`); **not stated in the message:** the flag was kept and can no longer create a baseline (the spec asked the lane to say which); the test for 2.1 does not assert the file and counts it claims and there is no test for the new-file refusal; **2.2 is labelled `test-first` with a failing run at `ac538733`, but at that sha `--write-baseline` writes `{FileA:{button:1}}` and returns 0 (re-read), so every assertion of that test passes there** |
| D3 the pending list is enforced | met | **met-modified** | `MascotFigure` and `TriagePage` leave `PENDING_LIST` (both exist); `validatePendingList` and an AST-based fixture check (a tag in a markup position, not an attribute value, comment or script); the exact count `60` is hard-coded (`toHaveLength(60)`), which the table itself does not state (the spec: "a count the table itself states, or the reason written next to the bound"); a `lib/dev/` fixture source is added (needed for `TriagePage` only, not in the spec); the fixture check's `catch` falls back to a regex (L3-1) |
| D4 symlink guard and lines | met | **met** | the guard compares `fs.realpathSync` of both paths and a failed resolve is a message and exit 1 (`:497-502`, re-read); the test makes a real symlink, spawns `node` on the script (so the guard is reached) and has a matched positive control by the real path; line assertions are hard-coded numbers recounted by the leaf (6, 4, 7, 3, 6), not derived by the production helper; the resolve-failure branch has no test |
| D5 the lint's own files and tests | met | **met-modified** | `libDir`, `routesDir` and the `generateBaseline` import are gone; the scenario test is split into three `it`s (try/finally, not `afterEach`); `formatTable` takes its columns from `RULE_NAMES`; the 5.2 test asserts only `toContain('unparseable')` for a rule that is in both the list and the findings, so it does not isolate "from the rule list"; `npm run lint` not run |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, nothing run in Node); the orchestrator re-read the points marked "re-read" at `a3919eb1`: the unparseable rule, the regex removal, the `realpathSync` guard, the `--write-baseline` guard and its missing-file branch, the fixture check's fallback, the 2.2 test against the base behaviour, and the 1.1 assertion. **Not read by the leaf:** the five commits one by one, the lane's `npm-*.log` failing runs and its `lint:vocabulary` run (the worktree is gone), `npm run lint`, the mutation runs, the Svelte compiler's error shape, Windows behaviour of the symlink test.

- **Dispatch:** one read-only leaf — a 3-file diff whose tests had to be read for evidence the lane's claims need.
- **Verdict:** no blocker, no major. The code does what the spec asks on the main paths. **Several claims overstate the evidence**, and the printed format changed without being reported.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D2.2 failed at `ac538733` (`test-first`) | **refuted** | against the lane | re-read: the base `--write-baseline` produces exactly what the test expects, so the test passes at the base; D3.1, D3.2 and D1.3 "failed at base" only because the helper (`validatePendingList`, `evaluateBaseline`) is born in the test file |
| L1-2 | the spec's Standing Constraint: a change of the printed format is a finding in the landing message | **left out** | against the lane | three changes: the unparseable warning line has no "use" (`… unparseable — <message>`), the table gained an always-present `unparseable` column (8 columns plus Total on every run), the finding's `component` field carries the parser's message; the format of the existing rules and the exit semantics are unchanged |
| L1-3 | D2: "the first creation of a baseline is a separate act decided at outline; the lane says which" | **not said** | against the lane | the flag stays and a missing baseline is now an error, so the flag cannot create one |
| L1-4 | D3: the 30/25 bounds replaced by a count the table states or a reason | **modified** | against the lane | a hard-coded `60`, tighter than the old bounds, with a description; the table does not state 60 |
| L1-5 | the spec's Expected Surface and 'Not in this plan' | holds | with the lane | `package.json` and `eslint.config.js` untouched; no `<svelte:element>`, `style=""`, `outline: 0px` or `outline-style: none` detection added; no `--error` flip; PLAN-72 untouched; the baseline file is not in the diff, so nothing raised a count |
| L1-6 | the mutation claims (offsets "+1", guard removed) | not re-runnable | open | whether "+1" means a character offset (usually stays on the line) or a line is not said |

### Layer 2 — Implemented

- **L2-1:** the symlink test reaches the guard (a spawned `node` runs the script as the main module), has a matched positive control and creates its own symlink; it does not skip on platforms without symlinks (it would throw on Windows without privilege, loudly).
- **L2-2:** `--write-baseline` is a read-modify-write with a direct `writeFileSync` (no temp file, no lock); a raised count in an existing file, a new rule in an existing file and a new file are all refused; **a renamed file is refused as new**, so a rename needs a hand edit; **the write path sorts rules alphabetically, so an unchanged baseline would be rewritten with eight files reordered** (the leaf's replication; counts unchanged, no test).
- **L2-3:** the `unparseable` rule cannot be baselined away (`--write-baseline` sees `{unparseable:1}` against 0 and refuses). A file that fails to parse yields only the `unparseable` finding, so its real violations disappear from the counts; the message, the comment and the code say nothing about it, though the ratchet still fails.
- **L2-4:** `lintCssSource` wraps `walkCssAst` in the same `try`, so a bug in the walker would be reported as an `unparseable` finding carrying a `TypeError`.
- **L2-5 (CodeRabbit's declined findings, judged against the code):** inline-1 (the entry guard exits on an unresolvable `argv[1]`): the decline is right, the spec mandates message and exit 1; the cost is that an importer whose `argv[1]` does not resolve is killed at import time, untested. **inline-2 (the fixture check's fallback): the decline is wrong on substance** — it says valid fixtures always take the AST branch, but the fallback is reached exactly when a fixture does not parse, and the regex `<Name\b(?![^<]*>)` is inverted for normal tags and returns true for `<!-- <Button < -->` plus an unclosed `<div>`; it also swallows the parse error silently, the defect class the plan removes. inline-3 (orphan fixtures under `lib/dev/`): defensible as out of scope, but the loophole comes from this PR's own widening (a `lib/dev/Unused.svelte` holding `<Button />` now satisfies the guard for `Button`).
- **L2-6:** no `child_process` in the script; the test spawns with an argv array and paths it built; `realpathSync` on a missing path is caught.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `vocabulary.test.ts:84-94` (re-read) | a fixture that does not parse | the fallback regex is wrong in both directions and swallows the error; a comment-only mention satisfies the check | minor (a broken fixture also fails the build) |
| L3-2 | the tests of 1.1, 2.1, 2.3, 5.2 | a regression of the claimed property | the claimed assertion is absent (parser message and line, file and counts named, the error line, "from the rule list"); only the exit code or the presence of a string is checked | minor |
| L3-3 | `vocabulary-lint.mjs` `formatTable` | — | the third parameter `ruleNames` is dead (no caller passes it) | minor |
| L3-4 | `--write-baseline` | an unchanged baseline | eight files reordered; not atomic; a rename refused | minor |
| L3-5 | `vocabulary.test.ts:17,166-170` | a file under `src/lib/dev/` | the extra fixture source opens the orphan-fixture hole of L2-5 | minor |
| L3-6 | the entry guard | an unresolvable `argv[1]` at import | `process.exit(1)` at import time; the branch has no test | minor |
| L3-7 | the whole lint | a file that fails to parse | its real violations are not counted (expected, unstated) | info |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | the baseline may only shrink; this plan raises no count | conforms | the baseline file is not in the diff |
| spec Standing Constraints | a printed-format change is reported in the landing message | **deviates** | L1-2 |
| spec Standing Constraints | the order of each Done-when is the order the evidence has | **deviates** | L1-1 |
| CLAUDE.md Web Standards | no `{@html}`/`innerHTML`, no arbitrary timer wait | conforms | the three files |

## Metrics and Anomalies

- Tokens `unknown`, `model` and `effort` `unknown` (harness `antigravity`, plugin version 0.1.0, executor 0.1.2149): unobserved, not zero.
- Anomalies: the lane asked the operator how to capture base-sha failing runs (a detached base worktree had no executor); the operator chose to copy the executor from `main` into it; one `python3 -c` at session start was disclosed; five builds of 447 to 584 s, no fallback.

## Routing and Merge Behavior

- Review gate re-read with `runbook_lane review-gate --pr-number 507`: `unresolved_review_threads=0` (lane 0, **same**), `review_body_findings_total=3` (lane 3, **same**); `handled` reads 0 and `body_findings_undisposed` now, where the lane read all three declined with reasons: the disposition record is gone with its worktree (the counting gap again, now on #507). CodeRabbit's inline-2 decline is judged wrong on substance (L2-5).
- Sonar: lane `sonar_open_new_issues=0`, quality gate ok; the Sonar search for pull request 507 returns no issues (0, **same**).
- CI/merge: merged through the merge queue as `a3919eb1` (re-read: `MERGED`, 3 files, +519/-126); the lane's CI run `44581355685` passed 20 checks (not opened).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 507; `landing` `landings/PLAN-170.md`; `plan_marshall_plan_id` `170-vocabulary-lint-hardening`
- [x] inbox: `rb-170-vocabulary-lint-hardening-001` archived as consumed
- [x] epic.md: the PLAN-151 Open Defect entry carries a pointer (the lint defects are fixed except the findings below); an Open Defect entry for this landing
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: all minor. The one that matters most is L3-1/L2-5 inline-2 (a fixture check that can still be satisfied by a comment when a fixture does not parse); a small test-only follow-up (assert the claimed properties of 1.1, 2.1, 2.3 and 5.2, replace the fallback regex by an error, state the creation decision, pin the rule order) can be a D0 of the next web plan that touches the lint, or wait.
- The detection gaps of PLAN-151 L3-4 and the `--error` flip with PLAN-72 stay named, not staged.
- For plan-marshall: a "test-first" row whose test passes at the base (now five landings: PLAN-66, 30, 151, 167, 170); a "failing at base" that means "the helper does not exist yet"; a printed-format change left out of the landing message; the review-gate counting gap.

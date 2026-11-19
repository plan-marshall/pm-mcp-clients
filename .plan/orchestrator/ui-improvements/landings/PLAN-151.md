# Landing Analysis: PLAN-151 — Web Foundation: The Vocabulary Lint, as Warnings

epic: ui-improvements
workstream: WS-02
pr: #495 (merged as d8be2ced, 2026-11-19)

> Landing record for one shipped plan. Written by `analyze` (inbox scan, message `rb-151-web-foundation-vocabulary-lint-001`, revision 1, `landing-check` complete: true) after verifying claims against ground truth.

## Deliverable Fidelity vs Spec

All four deliverables are in the diff (10 files, +1007/-2, all under `pm-web-app/` plus `doc/design/web-app/components.adoc`). The lane's report is faithful on substance; its evidence-order claims are overstated (L2-1, L2-2, L2-3).

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 the lint script | shipped-as-specified | `pm-web-app/scripts/vocabulary-lint.mjs` (431 lines), `package.json:25,27` (`lint` runs ESLint then the script, `lint:vocabulary` alone), exit 0 unless `--error` (`vocabulary-lint.mjs:419-423`); scan scope is wider than the spec's text (all of `src`, `:272-273`), which the spec's own claim 4 needs |
| D2 the ratchet and the lint's own tests | shipped-as-specified | `src/lib/vocabularyLint.test.ts:159-201` (clean files stay clean, no count above its baseline, no baseline above its actual); `scripts/vocabulary-baseline.json` (139 findings, 21 files) |
| D3 the vocabulary is complete | shipped-modified | `src/lib/vocabulary.test.ts` (52 active names of 60, 8 pending) + three new component tests (`Icon`, `Spinner`, `StepBar`) + fixtures in `routes/dev/components/+page.svelte`; the three tests are not in the spec's Expected Surface (L1-5) |
| D4 evidence | not shown by reading | the Maven run is a run-only fact; the wiring that makes it print and stay green is read and holds (`pom.xml:88-98`, `package.json:25`) |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, `execution-context-level-3`, 24 findings, 11 conformance rows) and the orchestrator re-read the points recorded here as `re-read`. By reading: no build, no test run, no write.

- **Dispatch:** one read-only leaf per landing — reason: reading merged code against a spec and a narrative is the depth the dispatch rule exists for, and this landing's read must not share context with PLAN-30's.
- **Verdict:** layer 1 findings (5, all minor); layer 2 findings (evidence order overstated, 4 minor); layer 3 findings (no blocker, no major, 9 minor, one not shown by reading).

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | The spec scopes the scan to `src/lib` and `src/routes`, yet its claim 4 expects `src/styles/base.css` flagged; the landing scans all of `src` | checked | refutes | spec:20, :84; `vocabulary-lint.mjs:272-273`; `vocabulary-baseline.json` holds `base.css` (re-read) |
| L1-2 | The premise "`<a>` occurs in no `.svelte`" is false on main and contradicts the spec's own evidence line | checked | refutes | spec:91, :138; baseline holds 10 `<a>` findings in 7 views |
| L1-3 | D1's Done-when 2 says "test-first, see deliverable 2", but D2's Done-when names no `--error` assertion, so the clause has no home | checked | refutes | spec:28, :37-39 |
| L1-4 | The pending list "holds only what other packages build", but `MascotFigure.svelte` and `templates/TriagePage.svelte` already exist | checked | refutes | `ls` of both at 2efcaa40 (re-read); `vocabulary.test.ts:20-29` |
| L1-5 | The Expected Surface omits `Icon.test.ts`, `Spinner.test.ts`, `StepBar.test.ts`, which D3 requires to exist and which did not | checked | refutes | spec:42, :99 |

**Layer 1 verdict:** findings — 5 (all minor).

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `vocabulary-lint.mjs:419-431` | `--error` 1/0 on fixture dirs, `vocabularyLint.test.ts:136-157`; no test runs the real entry or asserts a line number | "test-first" not borne out (L2-1) | checked |
| D2 | `vocabularyLint.test.ts:159-201` | mutations 1 to 3 fail with the quoted message prefixes (re-read: `:174-178`, `:183-186`, `:195-198`); the vitest suffix `expected 2 to be <= 1` not verified (no `node_modules` here) | none | checked |
| D3 | `vocabulary.test.ts:109-113` | removing `<StepBar` from the dev page fails it (`templateOnly` strips `<script>` and comments) | the mutation quote is from the post-fix test, the PR body quotes the pre-fix wording (L2-3) | checked |
| D4 | `package.json:25,27`, `pom.xml:88-98` | n/a | the cited commit `7021a239` touches the lint, both tests and the dev page, not an evidence step (L2-2) | not_shown_by_reading |

- L2-1 (minor): "D1 Done-when 2 test-first, commit 7021a239": `ed86d08e` (script and `package.json`, no test) precedes `fb03528b` (the `--error` test); `7021a239` is the review-fix commit that rewrote the test onto fixture dirs; no failing run is cited. Test-first is not borne out. All six shas exist as objects.
- L2-2 (minor): D4's "Commit: 7021a239" is not an evidence commit; the evidence is the build job ids, a run-only fact.
- L2-3 (minor): the recorded D3 mutation text exists only from `7021a239`; the PR body quotes the older "to have a fixture"; the spec's rule that a mutation quotes its log (spec:66) is not met by a quote that predates the final test.
- L2-4 (minor): test titles overstate their assertions: `StepBar.test.ts:14` ("renders all 7 onboarding steps") asserts 8 items (`:19`), `Icon.test.ts:11` ("renders icon with mask-image") asserts only `.ui-icon` and `data-icon`.
- `--error` exits 1 only through `runCli`; no test runs it on the real tree after the review fix, although the spec says "on the current tree" (minor, `vocabulary-lint.mjs:419-422`).

**Layer 2 verdict:** findings — 4 (all minor); D4 not shown by reading.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `vocabulary-lint.mjs:144-146` | a `.svelte` file the compiler cannot parse | `catch (_err) { return [] }`: zero findings, no message, exit 0 even under `--error`; for a baselined file the ratchet then reports a misleading "stale baseline, actual is 0"; `parseCss` swallows the same way (`:205`) (re-read) | minor |
| L3-2 | `vocabulary-lint.mjs:400-406` | `--write-baseline` (not in the spec, no npm script) | rewrites the baseline from the tree unconditionally, so counts can rise and the ratchet then passes; ENOENT uncaught if `scripts/` is missing (re-read) | minor |
| L3-3 | `vocabulary.test.ts:20-29`, `:82` | a pending component lands (two already have) | the pending list is never validated, so a landed component is never forced onto the checked list; "shrinks as they land" is not enforced; the fixture check is a text regex (`:109-113`), so `<Name` in an attribute or a dead block satisfies it | minor |
| L3-4 | `vocabulary-lint.mjs:151` | `<svelte:element this="button">`, `style="outline:none"`, `outline: 0px`, `outline-style: none` | not detected (RegularElement only); a spec-literal gap, not a violation; `<button {...rest}>` and elements in `{#snippet}`, `{#if}`, `{#each}` are found | minor |
| L3-5 | `vocabulary-lint.mjs:207` | the regex fallback `/\boutline\s*:\s*(none|0)\b/` when `parseCss` throws | also matches `outline: 0.5px solid` and text in comments; untested; hides that parsing failed | minor |
| L3-6 | `vocabularyLint.test.ts:41` | a wrong line offset (`getLineNumber :45-48`, `name_loc :154`, CSS `node.start :92`) | no test asserts a finding's `.line`, though `warning: <file>:<line>` is D1's contract | minor |
| L3-7 | `vocabulary-lint.mjs:253-254`, `vocabularyLint.test.ts:8` | `npm run lint` | dead `libDir`, `routesDir` and an unused `generateBaseline` import (re-read: line 8 is its only occurrence) print ESLint warnings about the lint's own files (`no-unused-vars` is `warn`); exit stays 0 | minor |
| L3-8 | `vocabulary.test.ts:37-58` | a table cell starting with a capital, or a table reshape | `extractVocabularyComponents` splits on `/\n\|(?=[A-Z])/`, the `group === Group` guard is dead, the 30/25 sanity bounds are arbitrary, the test reads `../../../doc` outside the module; it fails loudly, not vacuously | minor |
| L3-9 | `vocabularyLint.test.ts:136-201` | a failing `expect` | three scenarios in one `it`, temp dir removed outside `finally` (`:155-156`), loops stop at the first violation; `formatTable` hard-codes six rule columns (`:325-366`), so a new rule counts in the total but gets no column | info |

**Layer 3 not shown by reading:** L3-10: the entry guard `path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)` (`vocabulary-lint.mjs:425-431`, re-read) would be a silent no-op, exit 0 even under `--error`, if the checkout is reached through a symlink (macOS `/tmp`, `/var`); Node's realpath behaviour of the main module is knowledge, not run-verified; it matters when PLAN-72 flips `--error`.

**Layer 3 verdict:** findings — 9 (8 minor, 1 info), 1 not shown by reading. No blocker, no major.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web Standards | ES modules only, flat ESLint, no HTML sink, no timer wait, npm scripts stay | conforms | `package.json:5,17-30`; no `setTimeout`/`innerHTML` in the new files |
| CLAUDE.md Dependencies | no dependency without asking | conforms | `package.json` diff is scripts only; `svelte/compiler` from the existing devDependency |
| spec Standing Constraint | the lint warns and never fails the build; only the ratchet test may | conforms | `vocabulary-lint.mjs:419-423`, `package.json:25` passes no `--error` |
| pm-dev-frontend:jest-testing | one behaviour per test, side effects cleaned | deviates | L3-9 |
| pm-dev-frontend:javascript | no swallowed errors | deviates | L3-1, L3-5 |
| eslint.config.js | `no-unused-vars` warn | deviates | L3-7 |
| CLAUDE.md Build | build and test through Maven | not_shown_by_reading | the cited Maven jobs cannot be re-observed |

## Metrics and Anomalies

- Tokens: 245000 (as reported, not measured here). Harness `antigravity`, model `gemini-2.5-pro`, effort high, executor 0.1.2149.
- Anomalies: the lane's evidence order overstated (L2-1 to L2-3); the self-review was one round (APPROVED with minors, addressed in `1db23424`).

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `1db23424`, 6 inline findings handled in `7021a239`; Sourcery silent. Re-read with `runbook_lane review-gate --pr-number 495`: `unresolved_review_threads=0` (claimed 0, **same**), `review_body_findings_total=6` (claimed 6, **same**); `review_body_findings_handled` reads 0 without a disposition record and the lane's progress file is gone with its worktree, so the claimed 6 is **not re-verified**, not a difference.
- CI/merge: merged through the merge queue as `d8be2ced` (single-parent squash; the lane's six shas are not reachable from main).
- Sonar: claimed `sonar_open_new_issues=0`, quality gate ok; re-read through the Sonar search for pull request 495: no issues, **same**.
- Collisions: none observed; no rebase conflict reported (a merge of `origin/main` at `5f0da78d`).

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-151 --status shipped`
- [x] row `pr` stamped 495
- [x] row `landing` stamped `landings/PLAN-151.md`
- [x] row `plan_marshall_plan_id` stamped `151-web-foundation-vocabulary-lint` (the `{plan}` the sender `rb-{plan}` carries)
- [x] epic.md: Open Defect entry for this landing
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: L3-1 (swallowed parse failure) and L3-10 (the entry guard under a symlink) matter before PLAN-72 flips `--error`; L3-2 (`--write-baseline` defeats shrink-only) and L3-3 (pending list never validated, two entries already exempt finished work) are small. Proposed: one small fix folded as a D0 of PLAN-72 (the plan that flips `--error`), or a trivial change; the operator words any staging.
- L1-2, L1-4, L1-5: the spec text is stale on `<a>`, the pending list and the Expected Surface; the spec is shipped, so only the PLAN-72 spec that reads this baseline needs the corrected facts (139 findings, 21 files, 10 `<a>` in 7 views).
- For plan-marshall: a lane report that names "test-first" with a commit whose diff holds no failing-first run (the same pattern as PLAN-66's and PLAN-30's reports).

# PLAN-170: Web: The Vocabulary Lint Reports What It Cannot Read, and the Ratchet Cannot Be Loosened

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 (handle-open question 3 of 5, proposal 2: a small plan of its own, not a D0 of the parked PLAN-72) from `landings/PLAN-151.md` (the three-layer analysis of #495, `d8be2ced`, layer 3 L3-1 to L3-10). This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-151 (#495) shipped the vocabulary lint (`pm-web-app/scripts/vocabulary-lint.mjs`), a baseline of 139 findings in 21 files that may only shrink, and the completeness test of the vocabulary. Its gate has four holes that make "zero findings" and "the baseline did not grow" claims the tool cannot back: a `.svelte` or CSS source the parser cannot read yields no finding and exit 0, `--write-baseline` rewrites the baseline from the tree whatever the counts, the list of components still pending is never checked against the components that have landed, and the entry guard would make the whole script a silent no-op when the checkout is reached through a symlink. This plan closes them before `PLAN-72` flips the lint to `--error` and before the V parts of the views packages rely on "`npm run lint:vocabulary` reports 0 findings for this file".

## Deliverables

Read first:
- `landings/PLAN-151.md` of this epic, layer 3 L3-1 to L3-10 (L3-1, L3-2, L3-3, L3-5, L3-7 and the entry guard of L3-10 were re-read at `d8be2ced` by the orchestrator and again at `ac538733` for this spec; the others are the analysis leaf's reading and are re-checked at outline).
- `pm-web-app/scripts/vocabulary-lint.mjs`, `pm-web-app/src/lib/vocabularyLint.test.ts`, `pm-web-app/src/lib/vocabulary.test.ts`, `pm-web-app/scripts/vocabulary-baseline.json`, and the vocabulary table in `doc/design/` that `vocabulary.test.ts` reads.

1. **A source the lint cannot read is a finding, not silence**
   - What: `lintSvelteSource` returns `[]` when `parse(...)` throws (`vocabulary-lint.mjs:142-146`) and `lintCssSource` falls back to a regex when `parseCss` throws (`:200-210`) without saying so (L3-1, L3-5). A source that cannot be parsed becomes a finding of its own rule (named at outline, for example `unparseable`) with the file, the line the parser reports where it gives one, and the parser's message; it counts in the total, it prints in the warning list and in the table, and under `--error` it exits 1. The CSS regex fallback is removed or reduced to a clearly labelled lower bound: a regex that also matches `outline: 0.5px solid` and text inside comments must not report as an `outline` finding. A baselined file that stops parsing is reported as unparseable, not as the "stale baseline, actual is 0" message.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:lint-config
   - Done when:
     - A test fails at `ac538733` and passes: a `.svelte` source with a syntax error yields one `unparseable` finding with the file and the parser's message, and `runCli(['--error'], dir)` over it exits 1 (evidence order: `test-first`).
     - A test fails at `ac538733` and passes: a CSS source the parser rejects that contains `outline: 0.5px solid` yields no `outline` finding (evidence order: `test-first`).
     - A test: a baselined file that no longer parses is reported as unparseable and not as a stale baseline (evidence order: `test-first`).
     - `npm run lint:vocabulary` on the tree at the plan's head reports the same 139 findings in the same 21 files as `d8be2ced` plus any `unparseable` finding it newly reveals, each of which is named in the landing message (evidence order: `none`).
2. **The baseline can only shrink**
   - What: `--write-baseline` (`vocabulary-lint.mjs:399-405`; in no deliverable of PLAN-151 and in no npm script) rewrites `scripts/vocabulary-baseline.json` from the tree unconditionally and returns 0, so a count can rise and the ratchet then passes (L3-2); it also throws an uncaught `ENOENT` if `scripts/` does not exist. It lowers counts and removes files that reached zero, and refuses (exit 1, naming the file and both counts) to write a count above the one on record or to add a file; the first creation of a baseline is a separate, explicit act decided at outline (the baseline exists today, so the lane may remove the flag if nothing needs it, and says which). `scripts/` missing is a message, not a stack trace.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:lint-config
   - Done when:
     - A test fails at `ac538733` and passes: `--write-baseline` over a tree with one more finding than the baseline records exits 1, names the file and both counts, and leaves the baseline file byte-identical (evidence order: `test-first`).
     - A test: `--write-baseline` over a tree with fewer findings lowers the file's count and removes a file that reached zero (evidence order: `test-first`).
     - A test: a missing `scripts/` directory ends in an error line and no uncaught exception (evidence order: `test-first`).
     - The first test is run once with the guard removed and fails (evidence order: `mutation-after`).
3. **The pending list is enforced**
   - What: `PENDING_LIST` of `vocabulary.test.ts:16-29` (eight names) is never validated, so a component that has landed is never forced onto the checked list and the list is not "shrinking as they land" (L3-3; the landing reads `MascotFigure` and `TriagePage` as already finished, to be confirmed at outline). A pending name that has a component file under `src/lib/components/` fails the test with the name, so the list can only name components that do not exist yet; the two finished entries leave. The fixture check (`vocabulary.test.ts` around `:82,109-113`) finds the component as a tag in a markup position and not as a text match (`<Name` in an attribute or in a dead block satisfied it). `extractVocabularyComponents` (`:37-58`) does not split on `/\n\|(?=[A-Z])/` in a way that a table cell starting with a capital breaks, and its dead `group === Group` guard goes (L3-8); the arbitrary 30/25 bounds are replaced by a count the table itself states, or kept with the reason written next to them.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `ac538733` and passes: a name on the pending list that has a component file fails the completeness test and names the component (the lane shows this with a fixture list, not by editing the real one; evidence order: `test-first`).
     - A test fails at `ac538733` and passes: a fixture that mentions `<Name` only inside an attribute value does not satisfy the fixture check (evidence order: `test-first`).
     - `PENDING_LIST` at the plan's head names only components that have no file (evidence order: `none`).
4. **The lint runs when the checkout is reached through a symlink, and a finding's line is tested**
   - What: the entry guard `path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)` (`vocabulary-lint.mjs:425-431`) compares a path as given with the module's own resolved path, so under a symlinked checkout (macOS `/tmp` and `/var` are symlinks) the guard is false and the CLI runs nothing and exits 0, even under `--error` (L3-10, not run-verified by the analysis). The guard compares real paths (`fs.realpathSync` of both) and a failure to resolve one is a message and exit 1, not a silent no-op. No test asserts a finding's `.line`, though `warning: <file>:<line>` is the contract of PLAN-151's deliverable 1 (L3-6): one test per source of a line (the template element, the CSS node, the element name location) asserts the number of a known finding.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - A test fails at `ac538733` and passes: the script run through a symlink to the app directory prints its findings and exits 1 under `--error` on a violating tree, and the matched positive control, the same script run by its real path, passes before and after (evidence order: `test-first`; if the run shows the old guard already works under a symlink, the hypothesis of the Claim Labels is refuted, the test stays as a regression test and the landing message says so).
     - A test per source of a line asserts the line of a known template, CSS and name-location finding (evidence order: `mutation-after`: the lane shifts each offset by one and records the failing run).
5. **The lint's own files are clean, and its tests fail on the first wrong assertion**
   - What: `npm run lint` prints ESLint warnings about the lint's own files: dead `libDir` and `routesDir` (`vocabulary-lint.mjs:253-254`) and an unused `generateBaseline` import (`vocabularyLint.test.ts:8`) (L3-7). The scenario test of `vocabularyLint.test.ts:136-201` holds three scenarios in one `it`, removes its temp directory outside a `finally` (`:155-156`) and loops until the first violation (L3-9): one scenario per `it`, the temp directory removed in `afterEach`. `formatTable` (`:325-366`) lists the rules the lint knows, not six hard-coded columns, so a new rule gets its column.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:lint-config
   - Done when:
     - `npm run lint` in `pm-web-app` prints no warning for `vocabulary-lint.mjs`, `vocabularyLint.test.ts` or `vocabulary.test.ts` (evidence order: `none`).
     - A test fails at `ac538733` and passes: a lint rule added to the rule list appears as a column of the printed table (evidence order: `test-first`).

Not in this plan, named so they are not lost (each stays in `epic.md` as an Open Defect): the detection gaps of L3-4 (`<svelte:element this="button">`, `style="outline:none"`, `outline: 0px`, `outline-style: none`): widening the detection raises the count and changes the baseline, so it is the operator's decision with its own count; the flip to `--error` and stylelint, which stay with `PLAN-72` (parked; the PLAN-72 spec does not yet say it flips this lint, PLAN-151's spec says it does, and the PLAN-72 spec is not edited here); the corrected facts for PLAN-72 (139 findings in 21 files, 10 `<a>` findings in 7 views, the pending list already exempting two finished components) stay in `landings/PLAN-151.md` L1-2, L1-4, L1-5.

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan_marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); a dismissal is made only on the operator's selection.
- Build and test through Maven only (`CLAUDE.md`). Beside the lane's build gate, `./mvnw verify -pl pm-web-app -am` passes at the head; `npm run lint` and the vitest suite run through it. The Playwright suite does not run: no view changes.
- No new dependency or plugin. ES modules only, no CommonJS; flat ESLint config only; no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}`; no arbitrary timer wait in a test (`CLAUDE.md` Web Standards).
- The lint's contract with the views packages stays: `warning: <file>:<line> <rule> — use <component>` per finding, `--error` exits 1 on any finding, a clean tree exits 0. A change of the printed format is a finding in the landing message, not a licence.
- The baseline file may only shrink: this plan's own commits never raise a count in `scripts/vocabulary-baseline.json`; a newly revealed `unparseable` finding (deliverable 1) is added to the baseline only if the operator accepts it at the time, otherwise the file is fixed or the finding is reported.
- Every behaviour change has a failing-first run in the progress record; the order of each Done-when is the order the evidence has: a `none` Done-when is never described as failing, and "fail-first" is used only for `test-first` and `mutation-after` rows.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met.
- Pull request size: about 4 to 6 files; if the changed set passes 100 files, stop and ask the operator.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: a `.svelte` source that fails `parse(...)` returns `[]` with no message — read at `pm-web-app/scripts/vocabulary-lint.mjs:140-146` at `ac538733` (`catch (_err) { return []; }`)
- OBSERVED: `--write-baseline` writes the baseline from the tree and returns 0, with no comparison to the baseline on record — read at `vocabulary-lint.mjs:395-405`; `runCli` reads `isErrorMode` from `--error` at `:392`, and `npm run lint:vocabulary` is `node scripts/vocabulary-lint.mjs` with no `--write-baseline` script — read at `pm-web-app/package.json:25-27`
- OBSERVED: the CSS parse failure falls back to `/\boutline\s*:\s*(none|0)\b/gi` over the whole source — read at `vocabulary-lint.mjs:200-210`
- OBSERVED: the pending list is a literal `Set` of eight names and nothing in the file reads it against the component files — read at `pm-web-app/src/lib/vocabulary.test.ts:16-29` (the use at `:82` is read at outline)
- OBSERVED: the entry guard is `path.resolve(process.argv[1]) === currentFile` with `currentFile = fileURLToPath(import.meta.url)` — read at `vocabulary-lint.mjs:425-431`
- OBSERVED: `libDir` and `routesDir` are assigned at `vocabulary-lint.mjs:253-254`, and `generateBaseline` is imported at `vocabularyLint.test.ts:8` — read at `ac538733`; whether either is used elsewhere in its file is read at outline (the analysis read the import's only occurrence as line 8)
- HYPOTHESIS: under a symlinked checkout Node's main module `import.meta.url` is the real path while `process.argv[1]` keeps the path as given, so the entry guard is false — the analysis states this as knowledge, not run-verified; confirm/refute by a test through a symlink (deliverable 4's first Done-when)
- HYPOTHESIS: `MascotFigure` and `TriagePage` of the pending list are finished components — confirm/refute by `git ls-files pm-web-app/src/lib/components` (verify-at-outline)
- HYPOTHESIS: the table-splitting regex of `extractVocabularyComponents` breaks on a cell that starts with a capital letter, and the `group === Group` guard is dead — the analysis read `vocabulary.test.ts:37-58`; confirm/refute at outline
- HYPOTHESIS: no `.svelte` or CSS source of the tree fails to parse today (so deliverable 1 reveals no new finding) — confirm/refute by running the lint with the swallow made visible once (verify-at-outline)
- HYPOTHESIS: `formatTable` hard-codes six rule columns at `vocabularyLint.test.ts:325-366` — confirm/refute at outline
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; a refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/scripts/vocabulary-lint.mjs`
- OBSERVED: `pm-web-app/scripts/vocabulary-baseline.json`
- OBSERVED: `pm-web-app/src/lib/vocabularyLint.test.ts`
- OBSERVED: `pm-web-app/src/lib/vocabulary.test.ts`
- HYPOTHESIS: `pm-web-app/package.json` (only if deliverable 2 removes `--write-baseline` and a script names it, or a script is added; verify-at-outline)
- HYPOTHESIS: `pm-web-app/eslint.config.js` (only if a rule or an ignore of the lint's own files needs a change; verify-at-outline)

## Dependencies and Sequencing

- Depends on: `PLAN-151` (shipped, #495 `d8be2ced`).
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-19; shipped, parked and superseded plans are not in the disjointness population): **no running plan** (PLAN-79 and PLAN-165 share no file with it), so it is emittable now. Staged: `PLAN-153` (`scripts/vocabulary-baseline.json` and `vocabularyLint.test.ts`: it shrinks the baseline counts of two files and may touch the test; whichever lands second rebases and keeps both sides, and the baseline only shrinks), `PLAN-135` (`package.json` only, the hypothesis of the surface). Parked: `PLAN-72` (`eslint.config.js`, `package.json`).
- This plan touches no view and no `pm-api` file.
- Adjacent to: `PLAN-72` (parked), which flips this lint to `--error`: this plan lands before PLAN-72 is restarted; every views package whose Done-when says "`npm run lint:vocabulary` reports 0 findings" (PLAN-155 and others) reads a gate this plan makes trustworthy.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-170-vocabulary-lint-hardening.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

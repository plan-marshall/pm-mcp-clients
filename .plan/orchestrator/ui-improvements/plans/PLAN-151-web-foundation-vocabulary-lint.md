# PLAN-151: Web Foundation: The Vocabulary Lint, as Warnings (D1 Foundation, Part 5)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

`components.adoc` § `#rules` says the vocabulary is closed and that "the package that builds this vocabulary adds a lint to the build that reports each [raw `<button>`, `<a>`, `<input>`, `<textarea>`, `<select>`, `outline: none`],
as a warning until every view has moved". This plan adds that lint without a new dependency, makes it a ratchet (a clean file stays clean, a counted file never gains a finding), and adds the completeness check that every
component of the vocabulary has its test and its fixture. PLAN-72 later flips the warnings to errors; the V parts bring their files to zero.

## Deliverables

Read first: `doc/design/web-app/components.adoc` (§ `#rules`, `#vocabulary`, `#new-component`), `README.adoc` § `#operator-decisions` (stylelint is PLAN-72's), `pm-web-app/eslint.config.js`, `pm-web-app/package.json`, `lib/i18n/catalogue.test.ts` (an existing scan by test).

1. **The lint script**
 - What:
   - `pm-web-app/scripts/vocabulary-lint.mjs` parses every `.svelte` file under `src/lib` and `src/routes` outside `src/lib/components/` with the Svelte compiler already in the build (`svelte/compiler`), and reports a raw `<button>`, `<a>`, `<input>`, `<textarea>`, `<select>`
     and an `outline: none` or `outline: 0` in a `<style>` block or a `.css` file outside `src/lib/components/`, each as `warning: <file>:<line> <rule> — use <Component>` and a per-file table with a total. It exits 0.
   - `npm run lint` runs ESLint and then the script, so Maven's lint step prints the warnings; `npm run lint:vocabulary` runs it alone; `--error` exits 1 on any finding (the switch PLAN-72 flips).
   - Scope: `shell/` and `templates/` are scanned like views (operator decision D-6, assumed A: the frame is built from the vocabulary too).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:lint-config, pm-dev-frontend:jest-testing
 - Done when:
   - The script runs on the tree and prints the per-file table with the count of each rule (evidence order: `none`; the table is quoted in the pull request body).
   - `--error` exits 1 on the current tree and 0 on a clean fixture (evidence order: `test-first`, see deliverable 2).
 - Implements: `components.adoc` § `#rules` ("The vocabulary is closed").
2. **The ratchet and the lint's own tests**
 - What:
   - `pm-web-app/scripts/vocabulary-baseline.json`: the count per file and rule at landing. `src/lib/vocabularyLint.test.ts` fails when any file exceeds its baseline count, when a file not in the baseline has any finding,
     and when a baseline entry is higher than the actual count (the baseline only shrinks).
   - Fixtures of Svelte source strings in the test: each rule fires once on a raw element, does not fire inside `src/lib/components/`, does not fire on a component that merely contains the word `button` (a `<Button>`), and survives `{#if}` and `{#each}` blocks.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `vocabularyLint.test.ts` fails on each of: a new raw `<button>` in a clean view, a raise of a baseline count, a stale (too high) baseline; and passes on the tree. Evidence order: `mutation-after` (three reverted lines, failing assertions quoted).
 - Implements: `components.adoc` § `#rules`.
3. **The vocabulary is complete**
 - What:
   - `vocabulary.test.ts` reads the table of `components.adoc` § `#vocabulary`, and for every component not on a named pending list asserts that `src/lib/components/<Name>.svelte`, its `.test.ts` and a use on `routes/dev/components/+page.svelte` exist.
     The pending list holds only what other packages build: `MascotFigure`, `Chat`, `ChatTurn`, `ProposalCard`, `ChatStatusCard`, `ChatMarkdown`, `VoiceInput`, `TriagePage` (column engine), and shrinks as they land.
   - `StepBar.svelte` is used by `WizardPage` and the onboarding and is not in the vocabulary table. Add it to § `#vocabulary` (Supporting) and § `#supporting`, and add one sentence that the feature composites (`ConsentBox`, `PairingQr`, `HarnessChoice`, `LessonPicker`
     and the like) are built from the vocabulary and are not part of it (operator decision D-7, assumed A).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:jest-testing, pm-documents:ref-asciidoc
 - Done when:
   - `vocabulary.test.ts` passes on the tree and fails when a component of the table has no fixture (a component name is removed from the dev page in a mutation). Evidence order: `mutation-after`.
 - Implements: `components.adoc` § `#vocabulary`, `#new-component`.
4. **Evidence**
 - What: the per-file table before and after, quoted in the pull request body; the order of the worst files (the views with the most findings) is the order the V parts and the parked plans follow.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript
 - Done when:
   - `./mvnw -pl pm-web-app verify` prints the warnings and exits green (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite).
- NO new dependency or plugin: the lint uses `svelte/compiler`, which is in the build. An ESLint plugin for Svelte, `svelte-eslint-parser` or stylelint is NOT added (stylelint is PLAN-72's; an ESLint Svelte plugin needs the operator's decision first).
- Web standards of `CLAUDE.md`: ES modules, flat ESLint (no `.eslintrc.*`), no `innerHTML`/`{@html}` with runtime text, no timer waits in tests, the npm scripts `lint`, `format`, `test`, `test:ci-strict`, `quality` stay.
- The lint is a warning: it never fails the build in this plan; only the ratchet test does, and only for a regression.
- Ports of this plan: mock web port 21510, app port 21511 (`20000 + 10 × 151`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `eslint.config.js` configures ESLint, typescript-eslint, prettier and security only; no Svelte plugin is in `devDependencies`, and no `stylelint` is present — read at `pm-web-app/eslint.config.js` and `pm-web-app/package.json`
- OBSERVED: `svelte` is a `devDependency` (`^5.57.1`), so `svelte/compiler` ships with the build — read at `pm-web-app/package.json`
- OBSERVED: `lint` is `eslint .`, and the Maven module runs the npm scripts through the frontend plugin — read at `package.json` and `pom.xml`
- OBSERVED: raw `<button>` counts outside `lib/components/` — `ProjectExplorerView` 27, `StartView` 16, `OverviewView` 13, `DecompositionReviewView` 13, `SystemView` 9, `PlanView` 8, `EpicDecompositionView` 7, `EpicView` 7, `CleanupView` 7, `ProcessDesigner` 7, `BottomBar` 5, `EpicAdoptView` 5, `NavRail` 4, `DecisionView` 3, `RecipeReadyView` 2 — read by `git grep -c "<button"`
- OBSERVED: `outline: none` is written in `CommandPalette`, `FilterInput`, `RowMenu`, `Dialog` (components and shell), in `DecisionView` and `ProcessDesigner` (views) and in `styles/base.css` — read by `git grep`
- OBSERVED: `StepBar.svelte` exists and is not in the table of `components.adoc` § `#vocabulary` — read at both
- HYPOTHESIS: `svelte/compiler`'s `parse(source, { modern: true })` gives the element nodes with line numbers for a Svelte 5 file with `{#if}` and snippets — confirm/refute at `node_modules/svelte/types/index.d.ts` § `parse` (verify-at-outline)
- HYPOTHESIS: `<a>` does not occur in any `.svelte` file today (a count of 0 on the line pattern `<a `) — confirm/refute at `git grep -n "<a\b" -- pm-web-app/src` (verify-at-outline)
- HYPOTHESIS: the Maven lint step shows npm output, so warnings are visible without a log level change — confirm/refute at `pm-web-app/pom.xml` § `npm-lint` execution (verify-at-outline)
- Verify-first clause: re-run the per-file count at the current HEAD (V parts and parked plans may have landed) before writing the baseline; the baseline is the count at THIS plan's head, not the table above.

## Expected Surface

- OBSERVED: `pm-web-app/scripts/vocabulary-lint.mjs` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/scripts/vocabulary-baseline.json` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/vocabularyLint.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/vocabulary.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/package.json` — present at HEAD 1cc0e10e (the scripts only)
- OBSERVED: `pm-web-app/eslint.config.js` — present at HEAD 1cc0e10e (read; unchanged unless the script needs an ignore entry)
- OBSERVED: `pm-web-app/pom.xml` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e (read)
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-150` (so every message of the lint can name a component that exists); `PLAN-45` landed (it changes the view set and the baseline).
- Overlaps with:
  - `PLAN-135` (parked): `package.json` (it adds `marked`; this plan changes `scripts` only).
  - `PLAN-72` (parked): `package.json`, `tokens.css`, the token lint; it flips this lint to `--error` and adds stylelint. This plan lands first.
  - `PLAN-80` (shipped web-standards conformance): read `.plan/marshal.json` lint settings, not changed here.
- Adjacent to: the stylelint of `README.adoc` § `#operator-decisions` (PLAN-72); `catalogue.test.ts` (the model for a scan by test).
- Unblocks: PLAN-153 to PLAN-157 (their Done-when reads the count); PLAN-72; the gate of PLAN-41 (VD-V-05b).

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since. Settle each HYPOTHESIS. Re-check the operator's answers to D-6 and D-7.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-151-web-foundation-vocabulary-lint.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

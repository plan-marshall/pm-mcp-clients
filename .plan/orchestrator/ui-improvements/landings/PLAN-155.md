# Landing Analysis: PLAN-155 — Web Views: The Decomposition and the Start Sub-Steps on the D1 Vocabulary (Views Package, Part 3)

epic: ui-improvements
workstream: WS-03
pr: #531 (merged as 9af86425, `feat(web): decompose epic view, review view, adopt and recipe ready on D1 vocabulary`; 23 files, +1058/-334 against the first parent)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-155-web-views-decomposition-and-start-steps-001.md`, runbook lane, analysed 2026-11-22). `landing-check` is **incomplete**: the message is narrative only, with no `landing-facts` block, so all 17 required keys are missing (schema, plan_id, pr, merge_state, cleanup_owed, deliverables_total, deliverables_done, total_tokens, harness, model, effort, plugin_version, steps and the four review-and-Sonar numbers). The message was read as a lead; the PR, the merge commit, the review state and Sonar were re-read through `ci`, `runbook_lane review-gate` and the Sonar read, and the lane's progress file `.plan/temp/rb-155-web-views-decomposition-and-start-steps/progress.md` was read as the runbook lane's evidence set.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The decomposition view | shipped-modified: the vocabulary move is delivered and tested; the spec's "check marks become StatusLabel" was a swap of nothing (the pre-state had no per-plan check mark, so the StatusLabel and icon is new content) | `EpicDecompositionView.svelte:111-130` (`checkStateOf`), `EpicDecompositionView.test.ts` (10 `it()` blocks against the lane's "9/9": the tenth, `dependency_cycle`, came with the review polish) |
| D2 The decomposition review | shipped-modified: restyle delivered, plus two things the spec's pure-restyle rule forbids: an editable Objective field whose text is merged into the instruction, and Deliverables and Order rows for data the wire does not carry | `DecompositionReviewView.svelte:43-65`, `:99-125`, `:259-276`; `StagedPlanRecord.java:40-47` |
| D3 Adopt and the recipe's ready step | shipped-modified: both views are on the vocabulary and have tests, but the recipe's primary act and its Enter submit are not reachable in the product (`StartView` never passes `onSubmit`), and a free-text parameter field is new behaviour | `StartView.svelte:1045-1075`, `RecipeReadyView.svelte:45-49,82-90,161-172`, `EpicAdoptView.svelte:30-58` |
| D4 Browser evidence | not shown by reading (32 screenshots, `theme-readability.spec.js` 192/192, `decomposition.spec.js` 3/3 live under `.plan/temp/` or in a run) | progress file lines `D4 browser` and `D4 done` (lane's word) |
| D5 PLAN-158 residue (StartView retry, per-site mock pins) | shipped-as-specified in effect, but the spec's premise was stale: `StartView.svelte` and `MockRuntimeEngine.java` are NOT in the diff; the retry was already a shared `Button` and the engine already carried the codes; only tests were added | `git diff --stat 9af86425^1 9af86425 -- StartView.svelte MockRuntimeEngine.java` is empty; `StartView.svelte:1147-1153`; `MockRuntimeEngine.java:3228,6077-6078` |
| added-unplanned | `running-work.json` (+19: a new epic `platform-setup` serving only the archive-check pin) and `Compose.svelte` (+8: `inputTestid`, `submitTestid`), neither named by a deliverable; `client.ts` (+2: optional `deliverables` and `order` on the TS plan type) | `ControlScenarioTest.java:587-590`; `Compose.svelte:58-61`; `client.ts:1940,1943` |

## Three-Layer Analysis

The landing was read on three layers (the spec, the deliverable trace, the merged code) by one read-only leaf, and the orchestrator re-read the points recorded as `checked` below at `9af86425`. The reading is **by reading, no build, no test run, no write**.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`, plan-less) — reason: every full ship gets its own leaf; five restyled views and a Dialog interaction contract are the reading depth the dispatch rule exists for.
- **Verdict:** layer 1 findings (7); layer 2 findings (7) and not shown by reading (3); layer 3 findings (5 major, 12 minor/info) and not shown by reading (2). No blocker.
- **Re-read by the orchestrator** (`file:line` at `9af86425`): `StagedPlanRecord.java:30-48`; `DecompositionReviewView.svelte:40-66,118-125,256-276`; `StartView.svelte:1043-1076,1145-1154`; `RecipeReadyView.svelte:42-50,78-90,158-174`; `EpicDecompositionView.svelte:108-130,188-216,310-318,462-466`; `EpicAdoptView.svelte:30-36,46-58`; `Dialog.svelte:88-100`; `Compose.svelte:156-168`; `MockRuntimeEngine.java:3226-3230,6075-6079`; the `it(` count of `EpicDecompositionView.test.ts`; the empty `git diff --stat` of `StartView.svelte` and `MockRuntimeEngine.java`; the raw-button count of the pre-state `DecompositionReviewView.svelte` (0). Points the leaf recorded as `checked` that the orchestrator did NOT re-read are marked **unverified lead** below.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 (major) | D2 premise that a plan carries deliverables and an order is not in the wire contract: `StagedPlanRecord` has planId, title, objective, expectedSurface, dependencies, status, origin only; the lane added optional `deliverables` and `order` to the TS type with nothing behind them | checked | refutes | `StagedPlanRecord.java:40-47` (re-read) |
| L1-2 (minor) | The D2 raw-button count (13) is false: at `9af86425^1` the view had no raw `<button>` (0, re-read); the real change was variants, `on:click` to `onclick` and the fields. The same is leaf-reported for `EpicAdoptView` (5) and `RecipeReadyView` (2) | checked (D2 re-read); unverified lead (the other two views) | refutes | `git show 9af86425^1:…/DecompositionReviewView.svelte` |
| L1-3 (info) | D1 premise "check marks are glyphs" is contradicted, as `cleanup` already persisted (claim 5, `contradicted`) | checked | refutes | `EpicDecompositionView.svelte:111-130` |
| L1-4 (minor) | D5 premises are stale: the retry was already a shared `Button` (`variant="secondary"`), the engine already pins the codes since `4a84f727`, and the spec's lines (3483..8250, 5015, 5044) are stale | checked | refutes | `StartView.svelte:1147-1153`; `MockRuntimeEngine.java:3228,6077-6078` (re-read) |
| L1-5 (minor) | D3 makes Try again ghost, but the design hierarchy says ghost is for leaving without acting; D1 names `Draft again…` with an ellipsis the catalogue key does not carry. The code follows the spec in both | checked (code side re-read: `EpicAdoptView.svelte:33`); design-doc lines (`components.adoc:316,335`) unverified lead | refutes | `EpicAdoptView.svelte:33`; `EpicDecompositionView.svelte:466` |
| L1-6 (minor) | The pure-restyle constraint conflicts with D2's objective field and D3's parameter fields: no deliverable says what to do where the vocabulary needs a field the view lacked | checked | refutes | see L3-3, L3-4 |
| L1-7 (info) | The D4 amendment cites `EpicDecompositionView.svelte:519` as the `--color-success-subtle` block; the block is `.committed` at `:564-571`; by token values both themes exceed 4.5:1 (estimated by reading, not measured) | unverified lead | confirms | `tokens.css:89,91,102,170,182` |

No corpus claim of a staged spec is named by a finding (the spec is landed), so no verdict is persisted by this run; the spec's two settled claims (0 and 5, `contradicted`) stand.

**Layer 1 verdict:** findings — 7.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `EpicDecompositionView.svelte` | `EpicDecompositionView.test.ts` (check states by word, Agree aria-disabled with its reason, Compose + Enter, Dialog role and Escape): fail on a revert; **the "focus held" clause is not asserted** in the view test or in `decomposition.spec.js:37-41` (Escape only), it rests on `Dialog.test.ts` | the lane's "9/9" against 10 `it()` in the file | findings (unverified lead for the focus point) |
| D2 | `DecompositionReviewView.svelte:43-65` | `DecompositionReviewView.test.ts` (6) | the Objective field changes what is sent (L3-4); Deliverables and Order rows have no wire data (L1-1) | checked |
| D3 | `EpicAdoptView.svelte`, `RecipeReadyView.svelte` | `EpicAdoptView.test.ts`, `RecipeReadyView.test.ts` (2 + 2): `RecipeReadyView.test.ts` passes only because it injects `onSubmit`; the adopt-failure test asserts that `epic-adopt-failed` exists, not the cause text (unverified lead) | the primary act and Enter are product-dead (L3-2) | checked |
| D4 | — | `decomposition.spec.js`, `theme-readability.spec.js` | not shown by reading | not_shown_by_reading |
| D5 | tests only: `DecompositionScenarioTest.java:221-285`, `EnrolScenarioTest.java:234-258`, `MockRuntimeEngineTest.java:810`, `ControlScenarioTest.java:556-559,587-590` | `StartView.test.ts:281-282`: the "mutation evidence" mutates the TEST's own expectation (progress file: `StartView.test.ts:282 mutated to btn--primary`; `MockRuntimeEngineTest.java:810 mutated to scope_not_found`), which fails trivially and does not show that reverting the production line is caught, which is what `mutation-after` asks | the chat-turns POST pin and the epic cancel pin already existed | findings |
| (unnamed) | `running-work.json:2196` new epic `platform-setup`; `Compose.svelte:58-61` | `Compose.test.ts` is not in the diff, the new props have no direct test (unverified lead) | effects on other consumers of the scenario (overview counts, web e2e, CLI tests) | not_shown_by_reading |

**Layer 2 verdict:** findings — 7, not shown by reading — 3 (D4's runs; the `running-work.json` consumers; the Button aria-disabled click, which rests on `Button.test.ts`).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `DecompositionReviewView.svelte:122` (re-read) with `:99-105` | any plan from a real daemon | the Order row shows `plan.order ?? stream.order ?? 1`, so every plan of a workstream shows the workstream's number as its own order; the Deliverables row is always `—` (the wire has no such field, L1-1): both rows mislead | major |
| L3-2 | `StartView.svelte:1045-1052,1054-1075`; `RecipeReadyView.svelte:45-49,161-172` (re-read) | the `parameters` and `ready` steps | neither call passes `onSubmit`, so the view's primary `ready-start-act` and its Enter handler are inert in the product; the real primaries stay in the WizardPage slot (`StartView.svelte:1444-1461`, unverified lead). Wiring `onSubmit` as written would give two primaries on one screen | major |
| L3-3 | `RecipeReadyView.svelte:82-90` (re-read) | a recipe parameter with no options and `available !== false` | a free-text `TextField` is rendered and its typed value flows through `onParameter` (`StartView.svelte:1050`) into the parameters map and the `set` act; the leaf reports the pre-state rendered the "no options" message (pre-state side: unverified lead); the pure-restyle rule forbids a new input path, and `RecipeReadyView.test.ts:29-35,48-52` pins it | major |
| L3-4 | `DecompositionReviewView.svelte:43-65,259-276` (re-read) | Edit a plan, change the Objective, press Enter | the Objective text is merged into the instruction as `objective\ninstruction` (or sent alone as the instruction); the pre-state sent only the instruction. And with a plan that already has an objective and no change, the send is enabled (`disabled={busy \|\| (!instruction.trim() && !editedObjective.trim())}`) but `submitInstruct` returns silently when `text` is empty (`:59`): Enter does nothing with no feedback (the prefill of `editedObjective` is the leaf's reading, unverified lead) | major |
| L3-5 (pre-existing, not introduced here) | `EpicDecompositionView.svelte:190-215` (re-read) | Drop a plan, then Agree on a passing cut | `drop` writes only the local `droppedPlans`; `agree` calls `checkDecomposition` and `commitDecomposition` with no drop information, so a plan marked dropped and dimmed is still committed. The restyle touched exactly this code | major |
| L3-6 | `EpicAdoptView.svelte:46-58` (re-read) | two or more adoptable epics | one primary Adopt per epic, several primaries on one screen and no WizardPage primary on the adopt step (`components.adoc:28,315`, unverified lead) | minor |
| L3-7 | `RecipeReadyView.svelte:46` against `Compose.svelte:156-168` (both re-read) | IME confirm in Safari (`isComposing` false, `keyCode` 229) | the Enter guard checks only `event.isComposing`, so the confirming Enter calls `onSubmit` once it is wired; the test covers only `isComposing: true` | minor |
| L3-8 | `Dialog.svelte:93` (re-read to `:100`: no `isComposing` check on the Escape path) with `EpicDecompositionView.svelte:495-503` (unverified lead) | Escape to cancel an IME candidate inside the Compose in a Dialog | the dialog closes and `openInstruct` resets `instruction`, so the typed text is lost | minor |
| L3-9 | `EpicDecompositionView.svelte:111-130` (re-read to `:130`) | a dropped or locked plan with no findings; a global error with empty `affected_plans` | `checkStateOf` has no branch for locked, dropped or committed (rest of the function: unverified lead): such a plan shows the success tone and "checked"; a plan with dependencies shows "waiting" though its checks passed | minor |
| L3-10 | `EpicDecompositionView.svelte:464` (re-read) | the footer | `+ ` glyph prefix on the Add a plan label (`components.adoc:337`, "no glyph as a label"); `Add a plan`, `Edit` and `Draft again` open a dialog without the ellipsis | minor |
| L3-11 | `DecompositionReviewView.svelte:270,273` (re-read) | the edit and draft panels | the Compose send is named `Edit` (and `Draft again`) beside buttons with the same accessible names; the send is disabled with no `disabledReason` (`components.adoc:152,336`) | minor |
| L3-12 | `EpicDecompositionView.svelte:312-316` (re-read: the error is drawn in the page); the dialog staying open on a failed `submitInstruct`: unverified lead | a failed instruct submit | the error is drawn behind the Dialog backdrop while the person is still in the dialog | minor |
| L3-13 (unverified leads, not re-read) | `EpicDecompositionView.svelte:397,329-337` (a failed plan's reason shown up to three times; duplicate keys for two errors with one message), `DecompositionReviewView.svelte:34,268,296` (edit and draft panels share one `instruction` binding; `draft-input` and `draft-submit` testids removed, consumers outside the changed tests unknown), `RecipeReadyView.svelte:79,94` (hidden duplicate `proposed-{name}` span, dead DOM), `EpicDecompositionView.svelte:661` (a second identical `.plan__actions` rule), `Button.svelte:40` | — | — | minor / info |

**Layer 3 not shown by reading:** whether the daemon ever sends a parameter with no options and `available=true` (L3-3); the new `running-work.json` epic's effect on other scenario consumers; the browser, theme and build results.

**Layer 3 verdict:** findings — 17 (5 major, 12 minor/info; none blocker), not shown by reading — 2. **Text sinks are clean** (a plan's objective and a check reason from the planning role reach the DOM only through `{expression}` or `KeyValueList`; no `{@html}` in the five views, `Compose`, `Dialog`, `StatusLabel` or `KeyValueList`, leaf-read, unverified lead) and the de/en catalogue keys are at parity.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Web Standards | ES modules only | conforms | the diff adds `.ts`, `.svelte` and ES-import `.js` only |
| CLAUDE.md § Web Standards | no `innerHTML`/`{@html}` with untrusted text | conforms | leaf read of the five views and the shared components |
| CLAUDE.md § Web Standards | no arbitrary timer wait in a test | conforms | the five test files and the e2e diff wait on events and `findBy` |
| CLAUDE.md § Web Standards | flat ESLint | conforms | no eslint config touched |
| spec | every new text from the catalogue in en and de; the baseline does not grow | conforms | `decomposition.*` and `start.decomposition.*` keys in both locales; `vocabulary-baseline.json` shrinks |
| CLAUDE.md § Code Standards | `@ParameterizedTest` for 3+ variants | deviates | `DecompositionScenarioTest.java:221-285` puts 8 epic/project and 4 target variants of identical request/assert pairs in one `@Test` |
| CLAUDE.md § Code Standards | input test data from `Generators` | deviates | `DecompositionScenarioTest.java:241,245,251,255` post the literal body `{"instruction":"test"}`; `EnrolScenarioTest` uses literals for project and store path |
| `components.adoc` | one primary act per screen; ghost only for leaving; ellipsis before a dialog; no glyph label; no two acts share a label; a disabled act says why | deviates | L3-6, L3-2, L1-5, L3-10, L3-11 |
| `components.adoc` | vocabulary closed: no raw button, input, textarea, select, own button class or z-index in a view | conforms | the four views contain none; `EpicDecompositionView` left the z-index allowlist (`tokens.test.ts`) |

## Metrics and Anomalies

- Tokens: `unknown` (the runbook lane wrote no measurement; recorded as unobserved, not as zero). Duration: not recorded by the message.
- Anomalies: the landing message carries narrative and test evidence but no `landing-facts` block (`complete: false`, 17 keys missing). The mutation evidence for D5 mutates the tests' own expectations (Layer 2). The lane's "D1 9/9" is 10 in the merged file.

## Routing and Merge Behavior

- Review: CodeRabbit; three inline findings, each dispositioned in the progress file (`coderabbit inline inline-1…3 — c0a379c2`), so `review_body_findings_handled` is re-verified at **3 of 3** from the run's own lines. `runbook_lane review-gate --pr-number 531` re-read on 2026-11-22: `unresolved_review_threads=0`, `review_body_findings_total=3`, `handled=0`, `merge_allowed=false (body_findings_undisposed)`: the bare call cannot see the progress file's dispositions, so the `handled` difference is the known bare-call artefact, not a contradiction. The GraphQL read shows 3 review threads (all resolved) and 5 reviews. The message states no number to compare (not supplied).
- CI/merge: merge commit `9af86425` (`ci pr view`: state merged, base `main`); the order on `main` is #528, #529 (ledger), #531, #530, #532 (`git log origin/main`). No rebase conflict or re-verify signal is recorded by the message.
- Sonar: the message states no `sonar_open_new_issues`. Re-read now: `sonar read_open_issues --pr 531` → `count_status: confirmed`, `open_issue_count: 0`, `analysis_revision` = `head_revision` = `c0a379c2`; `gate-status` OK. The lane's "0" is not contradicted.

## Reconciliation Actions

- [ ] row `status` → `shipped` — `orchestrator queue --transition PLAN-155 --status shipped`
- [ ] row `pr` stamped `531`
- [ ] row `landing` stamped `landings/PLAN-155.md`
- [ ] row `plan_marshall_plan_id` stamped `rb-155-web-views-decomposition-and-start-steps` (the sender's `{plan}`)
- [ ] epic.md: Open Defect for the incomplete landing and the layer-3 majors (PLAN-155 entry)
- [ ] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 majors L3-1, L3-2, L3-3, L3-4 (and the pre-existing L3-5) are reported to the operator at once and are not fixed inline.** A fix plan is staged only on the operator's word: new plan or fold into a staged web plan.
- PLAN-137 (parked) re-grounds on these views: its claim that it adopts the chat on `EpicDecompositionView` and `DecompositionReviewView` must be re-read at `9af86425` (the views have a Compose in a Dialog now; L3-8 and L3-11 bear on it).
- The spec's D1-D5 premises that proved stale (L1-1, L1-2, L1-4) are recorded here; the spec is landed, so nothing is corrected in the corpus.

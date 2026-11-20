# Landing Analysis: PLAN-169 — The onboarding choice screens say what the engine published, and keep what the operator typed

epic: ui-improvements
workstream: WS-03
pr: #512 (merged as 7ccacfc3, 2026-11-20; 20 files, +1347/-458)

> Landing record for one shipped plan, the web-side residue of PLAN-66 (#485). Written by `analyze` (inbox message `rb-169-web-onboarding-residue-001`, `landing-check` complete: false) after verifying claims against ground truth. **`deliverables_done=0` of 6 is the lane's own honest count**: the base-sha evidence worktree at `ac538733` was refused, so no `test-first` clause could be produced; every deliverable holds at least one such clause or an unshown `mutation-after`. The verdicts below judge the code and the tests by reading, which is more favourable than the evidence order.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 the contract of every fact, screens cut back | not met (no test-first) | **met-modified** | the PR body names every fact and its decision; the leaf checked each against the engine's rows at `pm-mcp-core` `origin/main` (`catalogue.select`, `catalogue.record`, `store.choose`, `harness.choose`: prose rows with no field names; no `harness.tokens-*` row; no hit for `protects_against`, `does_not_protect`, `cannot_list`, `models_listed`, `no_advice_reason`, `token_detail`): **each cut fact really is unpublished**; the cut tests fail on the old code (mutation runs 183702 and 184034: 7 and 8 failing); `TokensScreen` is not extended; **two facts are kept that no row names**: `stores[].protects_against` and `.does_not_protect_against` ("the row names the content", but no field is published, and the tests feed a made-up string), the same class as the `reason`/`rationale` facts that were cut (inconsistent); not reported as gaps: the state-level `default` (read by `ModelsScreen.svelte:43`) and `recommendation`; how the web adapter maps `facts`/`items` to the `stores[]`, `models[]`, `harnesses[]` arrays the screens read was not checked |
| D2 the store options | not met (no test-first) | **met-modified** | `protectionDetail` no longer reads `platform`, `backend` or `os`; the published row `{kind:'os', name:'GNOME Keyring', available, recommended}` renders no "macOS" (`AspectChoice.test.ts:124-130`, failed in run 184034); an unavailable OS store records `os_unavailable` (`StoreScreen.record()`, revert run 212050); an unknown kind records nothing (no `links[0]` fallback); not reported: the primary button for an unavailable OS store still says "Keep the tokens in GNOME Keyring" while the click only opens `store.unavailable` |
| D3 the model field and model-id hygiene | not met (no test-first) | **not met for c1; c2 and c3 met-modified** | **c1 (a cannot-list harness with zero rows in a state with no `harnesses[]` gets a field) is not fixed in the real view (re-read):** `buildGroups` names harnesses from `harnesses[]`, the proposal rows, `harnessNames` and the default harness; `harnessNames()` (`OnboardingView.svelte:202-214`) is built from `currentState.harnesses ?? $runtimeStore.onboarding?.harnesses`, so a state without `harnesses[]` yields an empty map and no group; `OnboardingView.svelte` is not in the diff; the c1 test (`ModelsChoice.test.ts:22-27`) passes `harnessNames: { opencode: 'OpenCode' }` directly, an input the real view cannot supply; for the published shape nothing changed; case-insensitive ids and `::` (tests failed in run 182211), `isComposing` on Enter, an ignored add says so (new `add_empty`/`add_duplicate` keys), accessible names "{model} on {harness}" with a role-query test; the spec asked for `compositionend` and "case-insensitive where the engine's ids are": the lowercase rule is the lane's own (the engine docs say nothing on id case), and Safari's Enter after composition end (`isComposing:false`) is not handled |
| D4 a refresh keeps the operator's edits | not met (no test-first) | **met-modified** | one function, `choiceProposal.ts` `proposalDelta(previous, next)`, documented, with `choiceProposal.test.ts`, used by the three components (outside the surface; the spec asked for one function); added models, unticks, drafts, ticks and a touched default are kept, a stale row drops its edit with a notice; the `ModelsChoice` and `HarnessChoice` tests failed on the old code (runs 182211, 183702, 221337); **the three `AspectChoice` resync tests are in none of the failing-run lists** (the run the lane cites does not include them); the "one flag changed" scenario is approximated; the store notice "Your change was dropped…" also fires when the operator changed nothing |
| D5 words, keys, design lines | not met (no test-first) | **met (c1, c2); met-modified (c3)** | `usage_partial_missing` en and de, expected values updated (run 184034); `onboard.catalogue.select.group` and others removed in both packs, `OnboardingCatalogueTest` updated; the design lines and `onboarding-texts.adoc` rows checked (about 60 keys exist); **`onboard.link.yes_remember` was removed as unreferenced by a literal search, but `linkLabel` (`OnboardingView.svelte:89-93`) builds `onboard.link.${id}` dynamically**, so a generic screen offering that link id would show "Yes Remember" (no scenario does today, not verified); the generic store wording is "Protects a secret against: anything the store itself guarantees / Does not protect against: anything the store itself does not guarantee" for every store (the German line is ungrammatical): what the spec asked for, but it tells the operator nothing |
| D6 tests and browser check | not met | **c2 met; c3 met-modified; c1 not met** | `vi.restoreAllMocks()` moved to `afterEach` in four describes; `AspectChoice.test.ts` asserts both protection lines for every store; tests that fed unpublished shapes are replaced; 24 screenshots exist (two read by the leaf; the narrow `de` models shot shows the English mock `summary`, pre-existing, unreported); **the `models` payload (`ModelsScreen.recordedModels`) is pinned only by `expect.objectContaining`** (`OnboardingView.scenarios.test.ts:1499-1507`), so a revert to the full row or a dropped `probe` default still passes |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3; nothing run); the orchestrator re-read the points marked "re-read" at `7ccacfc3`: `harnessNames()`, `OnboardingView.svelte` absent from the diff, `buildGroups` and the first `ModelsChoice` test, the `ModelsScreen` props. The leaf read five of the lane's failing-run logs (failure counts match: 5, 7, 8, 1, 2). **Not read:** the e2e specs beyond a search, the full `HarnessChoice` files, `OnboardingView.test.ts`, the CI coverage report, 22 of 24 screenshots, `ModelCatalogueEntry.java`.

- **Dispatch:** one read-only leaf, which also read the engine's catalogue rows to check every cut-back decision.
- **Verdict:** no regression found that a scenario or e2e test depends on (no e2e expectation changed, 456 passed at the merge head by the lane's account); **two majors, both about tests that pass while the real behaviour is unchanged**.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ModelsChoice.test.ts:22-27`, `OnboardingView.svelte:202-214` (re-read) | a `catalogue.select` state without `harnesses[]` and a harness that cannot list models | no group, no text field; the test passes through a prop the view cannot supply; the very defect PLAN-66's L3-2 named is open | **major** (a gate that can pass while wrong) |
| L3-2 | `OnboardingView.scenarios.test.ts:1499-1507` | `recordedModels` returns the full row or drops `source ?? 'probe'` | the test still passes (`objectContaining`) | major by the gate definition, production code correct |
| L3-3 | `stores[].protects_against`/`.does_not_protect_against` | the engine publishes no such field | kept as published, against the plan's own rule; tests feed a made-up string | minor |
| L3-4 | `AspectChoice` resync tests | a revert of the resync | no recorded failing run for the three store and tick tests | minor |
| L3-5 | `OnboardingView.svelte:89-93`, the removed `onboard.link.yes_remember` | a link id `yes_remember` | `humanizeKey` text | minor (not verified) |
| L3-6 | the coverage floor | the branch margin is 0.14 pt (80.14 vs 80 after PLAN-165) | `-Pcoverage` ran green at `21bd7ebb` and `48e2164f`, **not at the merge head `75b2497c`** (the review fix `58c6a274` added resync branches); candidates: `StoreScreen.record` store-undefined, `ModelsScreen` `source ?? 'probe'`, a newly preselected row in `syncProposal` | minor to unknown |
| L3-7 | `AspectChoice` primary button, the store notice, the generic store wording, `failedIconPaths` not cleared on resync, Safari composition Enter | — | small behaviour and wording gaps listed in the table | minor |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | the vocabulary baseline may only shrink | conforms | `vocabulary-baseline.json` is not in the 20 files |
| spec Standing Constraints | no file of `pm-mock-runtime` or the engine edited | conforms | the diff holds neither |
| spec Standing Constraints | every Done-when's order is the order the evidence has | conforms | the lane says `test-first` nowhere and `deliverables_done=0` |
| spec Standing Constraints | every fix has a test that fails before it | **partly** | L3-1, L3-2, L3-4 |
| CLAUDE.md Web Standards | no `{@html}`/`innerHTML`, no raw colour; every text in en and de | conforms | the diff is symmetric |

## Metrics and Anomalies

- Tokens: see the message (`unknown` where unread); harness and model as in the facts block. Anomalies: the base-sha evidence worktree at `ac538733` was refused (the executor could not be copied), so no failing run against the base exists for any clause; the plan-less CI body slot `NO_PLAN` was shared with the concurrent PLAN-171 run (PLAN-169 overwrote its body); the mailbox read was refused (a plan id with a leading digit).

## Routing and Merge Behavior

- Review gate re-read with `runbook_lane review-gate --pr-number 512`: `unresolved_review_threads=0` (lane 0, **same**), `review_body_findings_total=7` (lane 7, **same**); `handled` reads 0 and `body_findings_undisposed` now (the counting gap again).
- **Sonar, re-read through the repository's own provider (not the SonarQube MCP, which cannot see this project, see the Watch entry of this date):** `sonar_rest gate-status --pr 512` is `OK` (new coverage 100.0, new reliability, security and maintainability ratings A); `sonar_rest search --pr 512` returns 0 issues of any status (the open count by `sonar read_open_issues` is `undecidable`, `head_revision_unavailable`, once the branch is gone). The lane's `sonar_open_new_issues=0` stands.
- CI/merge: merged through the merge queue as `7ccacfc3` (re-read: `MERGED`, 20 files, +1347/-458).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 512; `landing` `landings/PLAN-169.md`; `plan_marshall_plan_id` `169-web-onboarding-residue`
- [x] inbox: `rb-169-web-onboarding-residue-001` archived as consumed
- [x] epic.md: the PLAN-66 third-landing entry carries a pointer (PLAN-169 is shipped, with the findings below); an Open Defect entry for this landing
- [x] resume anchor, `queue-view.md`, decision log; overlaps read: the open #518 (PLAN-172, 25 files) and the staged PLAN-173 share no file with #512's 20 files (semantic coupling only: `ModelsChoice` uses `TextField`'s props and `models-input-*` testids)

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: **L3-1 is the open defect of PLAN-66's L3-2 in the real view**, with a test that cannot show it; L3-2 pins nothing about the models payload. Proposed, the operator's word only: a small fix (the harness named from the published shape or reported as a contract gap, a scenario test through `OnboardingView` with a state that has no `harnesses[]`, the payload asserted exactly, a coverage run at the head) as a deliverable of a plan that touches these screens (PLAN-173 does not) or a plan of its own.
- Residue the lane names, not staged: the other design rows that still list nonexistent keys and `onboarding-steps-4-7.adoc` prose; `recommended`, per-row `tokens`/`tokens_floor`, `harness.reason` and `icon_url` as contract gaps (the engine side).
- For plan-marshall: the base-sha evidence worktree cannot get an executor (`--detach` disables copy-from-main); a landing that reports `test-first` evidence by logs the base cannot produce; `review-gate` counting gap (now on #512).

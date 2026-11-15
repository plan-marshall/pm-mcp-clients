# Landing Analysis: PLAN-145 — Web: the intent client and the plan panels

epic: ui-improvements
workstream: WS-03
pr: #443

> Landing record for PLAN-145 (web intent and outline package, part 1). Merged as `da8c8644` (23 files, +4225/−3). Lane: runbook,
> sender `rb-145-web-intent-client-and-plan-panels`; harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`, executor
> `0.1.2133`. Written by the `analyze` verb on 2026-11-15 from inbox message `rb-145-web-intent-client-and-plan-panels-001.md`, after
> verifying the claims against git, the provider and the merged tree.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 client methods, types, catalogue area | shipped-modified | Four methods, readers that fail closed, 404 → `null`, `SUPPORTED_AREAS` entry, both packs (65 `intent.*` keys, same lines in en and de, ICU arguments match). The test pins **65** keys and 7 verdict words read from the design document, not the spec's 68/75: the spec's counts are stale (re-counted by the leaf). The rethrow of a non-404 error is untested. |
| D2 plan intent panel | shipped-as-specified | `PlanIntentPanel.svelte` (500 lines), `PlanIntentPanel.test.ts` (15 tests); each Done-when clause present. Gaps in the tests, below. |
| D3 builder check and outline | shipped-modified | Everything the contract carries is drawn. "interfaces, key links, `missing: <link>`" is **not delivered and cannot be**: `OutlineResource` carries `plan_id, revision, summary, deliverables, added_dependencies` (`OutlineResource.java:37-43`) and the design names neither. `intent.builder.recommended` is pinned and rendered nowhere. |
| D4 live, one step away, browser | shipped-modified | Refetch and a "record changed" notice work; `intent.spec.js` 3 tests (EN, DE, themes). Evidence order is `mutation-after`, not the declared `test-first` (the fixture and the spec are written by the same plan; the clause is unsatisfiable as written). The refresh set is narrow (L3-1). Links are inert by default (L3-7). |
| D5 `pm-api` intent residue | shipped-as-specified | `IntentContractTest` (+146), `BuilderCheckResource` now refuses a `not_run` check with questions; the seven reader mutations are claimed `mutation-after`; not re-run here. Whether the engine specification was consulted for questions on `not_run` (spec: "if silent, stop and ask") is not evidenced. |

## Three-Layer Analysis

Read on three layers by one read-only leaf (`execution-context-level-3`), which returned a report per layer, a conformance table and
leads for PLAN-146. The orchestrator re-read on the merged tree (`da8c8644`) the points marked [R]. **By reading, no build, no test run,
no write.**

- **Verdict:** layer 1 findings (2 major, 5 minor); layer 2 findings (1 major for test quality, 9 minor); layer 3 findings (1 major lead,
  11 minor). No defect that corrupts data; the major items are a vacuous test, a stale-read gap and spec drift.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | The spec's key counts (68 `intent.*`, 75 with verdicts) are stale; the design carries 65 and 72. The claim stamped `corroborated` by the cleanup pass is wrong. | checked | refutes | `intent-and-outline-texts.adoc#texts`; both packs lines 3-67 |
| L1-2 | D3's "interfaces, key links, `missing: <link>`" has no source in the contract or the design. The spec's own OBSERVED claim listing `interfaces, key_links, missing_key_link` is false and was stamped `corroborated`. | checked [R via the lane's citation and the leaf] | refutes | `OutlineResource.java:37-43` |
| L1-3 | The design's `recommended: <answer>` has no source: `BuilderQuestion` carries a boolean. | checked | refutes | leaf |
| L1-4 | The design shows `intent.oversize.empty` for a role without measured tasks; the contract refuses empty `buckets`. A second source-less clause, for PLAN-146. The epic's "completion review" (`intent.epic.review`) has no field in `EpicIntentRecord`. | checked | refutes | leaf |
| L1-5 | D4 demands `test-first` for a fixture and an e2e spec the same plan writes: unsatisfiable. D5 sits inside `## Claim Labels`, so `resolve` names four deliverables. The spec left open how the panel learns deliverable titles (it added `deliverables`, `deliverableHref`, `questionHref`), and "an open question keeps its place" without a state to keep. | checked | refutes | leaf |

### Layer 2 — Implemented

| Check | Result |
|-------|--------|
| The ten CodeRabbit dispositions | Eight fixes landed as described (`2f8ae071`) [R for `intentEvents`, read sequence, derived catalogues]; #1 (Generators) and #10 (theme fragment targets) not taken, with reasons. |
| Sinks | No `{@html}` and no `innerHTML` in the three components; every model-written text goes through `safe()`. Ids (`criterion.id`, `nonGoal.id`, item ids) render unstripped as text nodes — no markup risk; control sequences in an id would reach the DOM because the reader validates no id format. |
| Events | `phase.changed` and `plan.revision_queued` are real `EventName` members, held by `intentEvents.test.ts`. |
| `readRecord` | `404` → `null` only; everything else rethrown (code re-read); no test pins the rethrow. |
| Surface | Four files outside the Expected Surface (`intentEvents.ts`, `intentEvents.test.ts`, `intentWireCatalogues.test.ts`, `routes/dev/components/+page.test.ts`); the lane reported three. Declared and untouched: `catalogue.test.ts`, `IntentJson.java`. |

Claims of the lane's report the tree does not bear out:
- **The review gate disagrees with the lane:** `review-gate --pr-number 443` answers `unresolved_review_threads: 0`, `review_body_findings_total: 10`,
  **`review_body_findings_handled: 0`**, `merge_allowed: false`, `reason: body_findings_undisposed`, naming `inline-1` … `inline-10`. The lane's
  progress file holds ten `finding:` lines and the landing message states 10 of 10 handled. Which store the gate reads is not settled here; the
  merge happened. Recorded as a discrepancy, not a false claim.
- "Three undeclared surface files": four (above).
- Sonar: `read_open_issues --pr 443` returned `count_status: undecidable` (`head_revision_unavailable`; analysis revision `f1c4a521`), so the
  lane's `sonar_open_new_issues=0` and quality gate `ok` are **not re-verified**.
- PR body/pairing code: the landing quotes `KQXB-MRTN`, the spec file `KXQB-MRTN`; one is a typo.

Test quality (the leaf's reading, [R] where marked):
- **M1 [R]:** `OutlinePanel.test.ts:321-343` "keeps the newer outline when two reads answer out of order" cannot fail: the read that resolves last is
  the panel's first, so no revision is announced with or without the `readSequence` guard, and the test asserts only the absence of the notice.
  Disposition #6 is half-verified (the intent twin asserts `revision 5` stays and is valid).
- `/vote/i` absence (`PlanIntentPanel.test.ts:283`) cannot fail; `BuilderCheckList.test.ts:116,134-135` assert `.status-label*` absent from a component
  that never renders one; the PR's "keeps an expanded reason open across a refresh" has no test; no test shows `unsubscribe` runs on destroy; two
  "contract-impossible" cases (`criteria: []`, `deliverables: []`) the client refuses.
- **Format [R]:** `intentWireCatalogues.test.ts:53` carries `});` fused onto an `expect` line (the module's `format:check` would catch it; Maven runs
  lint only). `BuilderCheckList.svelte:4` imports `apiClient` and uses it nowhere.
- `IntentCatalogueTest` has no `@EnableGeneratorController` while its `format(...)` cases pass hand-written literals; arguable under the project rule
  that a contract's expected value stays literal.

### Layer 3 — Correct

| Finding | Severity | Evidence |
|---------|----------|----------|
| L3-1 | major lead | The panels reload only on `phase.changed` and `plan.revision_queued`, the callback ignores the payload (`intentEvents.ts:13-21`, [R]); every plan's `phase.changed` refetches every mounted panel; verdicts, builder answers and in-phase record revisions refresh nothing until the next phase change. The stream is shared (`client.ts:4708-4720`, [R]), one subscriber set. |
| L3-2 | minor | `planId` and `deliverables` read once in `onMount`: a panel reused across plans shows the old record unless wrapped in `{#key planId}` (PLAN-55/62's call). |
| L3-3 | minor | A failed live refresh sets `errorCode`, which is checked first: the panel is replaced by the error although the record is held. |
| L3-4 | minor | "the record changed" is a plain `<p>`, no live region; the reason toggle has `aria-expanded` but no `aria-controls`, and its name is the verdict word only. |
| L3-5 | minor | The head counts omit "not reviewed" once one criterion has a status ("4 criteria · 2 met" hides two unreviewed). |
| L3-6 | minor | No-record states: the outline and the builder check show their title only; `pending` with zero questions says "questions wait"; no `intent.outline.none` key exists; `invalid_response: Type.field` is not in the error catalogue (generic fallback). |
| L3-7 | minor | Link defaults are asymmetric: the outline links criteria to `/plans/{id}?tab=intent#criterion-…`, a route PLAN-55/62 must provide; `deliverableHref` defaults to `''`, so the criterion-to-deliverable step is inert text outside the fixture. |
| L3-8 | minor | The dev fixture renders once per theme and duplicates ids (`criterion-AC-001`, `deliverable-1`); known to the lane. `dependencyLine` collapses the first double space; `{x:''}` message-composition arguments are an i18n workaround. |
| L3-9 | minor | The e2e never triggers a live refresh, an absent record or `not_run`; mobile width rests on manual screenshots. |

German/English parity of the 65 keys is complete.

## Leads handed to PLAN-146

`getEpicIntent` and `getOverSize` exist with reader tests only: no panel, no browser evidence. The leaf's eleven leads are folded into the PLAN-146
spec (section "Folded from the PLAN-145 landing 2026-11-15"): empty `buckets` refused by pm-api and the TS reader against the design's empty state;
one role per route; the open-ended bucket has no flag; the share's rounding and the direction (`flagsMore` is not on the wire); the epic has no
`revision` and the plan-scoped event set does not apply; the epic's "completion review" has no field; evidence is text only; the `state.check.*`
mapping; do not copy the `{x:''}` hack or the duplicated ids; shared `+page.svelte` and `client.ts`.

## Routing

Routed to `epic.md` (Decisions, Open Defects, Watches) and the PLAN-146 spec; no fix plan staged (the operator did not ask for one on this
landing). Sonar and the gate's handled count are open leads.

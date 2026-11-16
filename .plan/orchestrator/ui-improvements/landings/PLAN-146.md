# Landing Analysis: PLAN-146 — Web intent: the epic's intent and the over_size distribution

epic: ui-improvements
workstream: WS-03
pr: #449 (https://github.com/plan-marshall/pm-mcp-clients/pull/449)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message
> `rb-146-web-intent-epic-and-oversize-001.md` (a lead, not a fact), the lane's progress file
> `.plan/temp/rb-146-web-intent-epic-and-oversize/progress.md`, git, the provider, and a read-only
> three-layer leaf. The orchestrator re-read the points marked "re-read" on `a4709ef5`.

Merged 2026-11-16T01:34:33Z as `a4709ef5772ade07669f8f26ec1e3b33e400c581` (parent `a692def2`), head `1ecaa608`, 19 files, +1878/-106.
Sender `rb-146-web-intent-epic-and-oversize`; the landing message arrived at 01:36:24Z.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D0 — the over_size distributions of every role (operator decision 2026-11-14, unparked) | shipped-as-specified | `OverSizeDistributionsResource.java` (a list of roles, duplicate role refused), the mock serves every fixture role, empty buckets allowed on both readers; `IntentContractTest.DistributionsPerRole`, `IntentScenarioTest.servesEveryRole`, `client.test.ts` "reads the over_size distributions of every role" |
| D1 — the epic's intent panel | shipped-as-specified | `EpicIntentPanel.svelte` (verdicts through `StatusLabel`, no progress bar, evidence as text, `stripControlSequences` on every text), its own event set `INTENT_EPIC_EVENTS` and a change fingerprint (`intentEvents.ts:28-39`); 10 cases |
| D2 — the over_size chart | shipped-as-specified | `OverSizeChart.svelte`: direction from current vs proposed, `Math.round`, open end by position, no act, no chart library, dynamic `style:inline-size` only; 13 cases |
| D3 — fixtures and browser evidence | shipped-as-specified (not shown by reading) | `intent.spec.js`: 9 cases (3 from PLAN-145, 6 new), DE and both themes, axe at 390 px; the CodeRabbit fixes are present (`MockRuntimeEngine` `orElseGet` empty list; the axe case waits for the destination and the three charts) |

Every one of the 19 diff files is explained by D0 to D3. The design document `intent-and-outline.adoc:237,282` was edited by the lane to fit its contract choice (a list of roles, empty buckets allowed); it is in the Expected Surface and the D0 decision covers it, but it is a spec change made by its implementer.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; the change spans Svelte, TypeScript and the Java contract and mock. The orchestrator re-read the key points on `a4709ef5`.
- **Verdict:** layer 1 clean (premises confirmed, one refuted by the plan's own D0); layer 2 findings (two false counts, one fixture copy); layer 3 findings — no blocker, no major; 3 medium (two re-read, one latent), 6 low, 1 info.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | D0..D3 premises: list of roles, `StatusLabel` tones, text-only evidence, own event set, rounding rule, open end, no chart library, one chart per role, proposal on running-work and none on fully-onboarded | checked | confirms | the leaf's list at `a4709ef5` |
| L1-2 | every catalogue key the components use exists in both packs (`intent.*.properties:54-67`, `common.*:454-456`), so the packs did not need to grow | checked | confirms | catalogue files |
| L1-3 | "one role per route" (claim 6) | checked | refutes | resolved by D0 |
| L1-4 | the spec's fold "do not copy the plan panels' unfiltered reload and the dev fixture's duplicate ids per theme" | checked | refutes (partly) | see L3-1 and L3-6 |

**Layer 1 verdict:** clean — the one refuted claim is absorbed by D0; the fold is partly not honoured (L3-1, L3-6).

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|----------------------------------|---------|
| D0 | `OverSizeDistributionsResource.java`, `OverSizeDistributionResource.java`, `MockRuntimeEngine` | `IntentContractTest.DistributionsPerRole`; restoring the empty-buckets refusal fails `OverSizeRules.roleWithoutMeasuredTasks`; a **missing `distributions` member is never tested to be refused** | none | checked |
| D1 | `EpicIntentPanel.svelte`, `intentEvents.ts` | `EpicIntentPanel.test.ts` — **10** cases (the landing says "twelve"; re-read); `intentEvents.test.ts`; no test for an `epicId` change or a failing reload | none | checked |
| D2 | `OverSizeChart.svelte` | `OverSizeChart.test.ts` — **13** cases (the landing says "fourteen"; re-read); the record is a hand copy of the mock fixture, so a fixture change goes unnoticed (the e2e spec and `client.test.ts` cover the fixture path) | none | checked |
| D3 | `intent.spec.js` | run only with `-Pe2e` | none | not_shown_by_reading |

False claims of the landing: the two test counts (10 and 13, not 12 and 14). Confirmed: `intent.spec.js` nine cases, the two CodeRabbit fixes. Not shown by reading: the vitest and e2e runs, test-first failing runs, the Playwright MCP check at 1440x900 and 390x844, Sonar, CodeRabbit.

The residue the epic had named for PLAN-145 is **neither touched nor fixed** (re-read): the `EpicProcessPlan:26` Javadoc, `MockRuntimeEngine` `STATE_DONE`, the plan panels' unfiltered reload (`intentEvents.ts:14` `INTENT_PLAN_EVENTS` still lists `phase.changed`; re-read), the unused `apiClient` import (`BuilderCheckList.svelte`, file not in the diff) and the out-of-order test quality. All remain open.

**Layer 2 verdict:** findings — 2 false counts, 1 fixture copy, 1 missing refusal test.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `EpicIntentPanel.svelte:53-56` | any plan's `plan.revision_queued`, any epic's `epic.decomposition_changed` or `epic.register_changed` | `refreshesEpicIntentView` ignores the payload, so every mounted epic panel refetches whichever epic it shows; the fingerprint stops a false "changed" notice, so it costs load and shows nothing wrong | medium (partly the PLAN-145 lesson taken over) |
| L3-2 | `EpicIntentPanel.svelte:51-58,74` (re-read: `epicId` is read in `onMount` only; `load` calls `getEpicIntent(epicId)`) | a parent reuses the component with another `epicId` (likely when PLAN-58 moves between epics' Intent tabs) | the panel keeps the previous epic's intent and refreshes the old id; not reachable today (the dev fixture passes a constant) | medium, latent |
| L3-3 | `OverSizeDistributionResource.java:191` (re-read: `IntentJson.readObjects` returns `List.of()` when the key is absent, `IntentJson.java:317-320`) | a daemon or fixture answer that omits `buckets` (or `distributions`) | the Java parser reads "no measured tasks" and the mock's own validation accepts it, while the TypeScript reader throws `malformedRecord` — fail-open and asymmetric | medium |
| L3-4 | `EpicIntentPanel.svelte:85,112` | a transient read failure after a matching event | `errorCode` is set and the record disappears behind `ErrorState` until the next matching event; no retry; no test | low |
| L3-5 | `OverSizeChart.svelte:64-65` | the last bucket is bounded (ends at 2-3x) | labelled "2x and more"; the landing concedes it | low |
| L3-6 | `routes/dev/components/+page.svelte:815-826` | the two theme copies | `epic-fixture`, `oversize-fixture` and the inner `data-testid`s appear twice; the e2e spec works around it with `.first()`/`.nth()`; they are `data-testid`s, not `id`s | low |
| L3-7 | `OverSizeChart.svelte:99` | 1 flagged of 300 | `300 tasks · 1 flagged · 0 %`; 299 of 300 shows `100 %`; the count stands beside it | low, cosmetic |
| L3-8 | `routes/dev/components/+page.svelte:821` | a daemon answer with a repeated role or `plan_id` | keyed `each` blocks stop the render with a Svelte duplicate-key error; only the Java resource refuses a duplicate role | low, latent |
| L3-9 | `EpicIntentPanel.svelte:~139` | every check pending | the "Completion review" heading is drawn though the record has no review field; the landing concedes it | low, cosmetic |
| L3-10 | `OverSizeChart.svelte:29-30,226` | — | `multiple()` uses `String(value)`; layout widths are `rem` literals (5.5, 6, 2, 16rem), not tokens (spec: tokens only) | info |

**Layer 3 not shown by reading:** whether the epic contract refuses duplicate `plan_id`s (L3-8).

**Layer 3 verdict:** findings — 3 medium, 6 low, 1 info; no blocker or major.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer wait; ES modules; scripts kept | conforms | text through `{expression}` after `stripControlSequences`; `findBy`/`waitFor` only |
| CLAUDE.md Code | Javadoc on public types; no generic exceptions; records, no Mockito; `@NullMarked` | conforms | `OverSizeDistributionsResource` is a documented record; `IllegalArgumentException`/`JsonFormatException` only |
| CLAUDE.md Code | `Generators` for input, `@EnableGeneratorController` | deviates (partial) | `IntentContractTest:50` carries it and `:637` generates the absent role; the project and role inputs of the new contract cases are hand literals |
| spec Standing | no static style attribute; tokens only | partial | only dynamic `style:inline-size`; `rem` literals in the CSS |
| spec Standing | no new dependency; every text from the catalogue in EN and DE | conforms | no `pom.xml`/`package.json` in the diff; all keys present |
| spec fold | the epic panel has its own event set and a change detector, held by test | conforms | `intentEvents.ts`, `intentEvents.test.ts` |
| spec fold | do not copy the unfiltered reload or the per-theme duplicate ids | deviates (partial) | L3-1, L3-6 |

## Metrics and Anomalies

- Tokens: `unknown` (no token measurement; recorded as unobserved, not zero). Harness `opencode`, model `deepseek-v4.1-flash/high`, as stated, not corroborated. `plugin_version` `unknown`.
- Anomalies: CodeRabbit refused twice on quota (20:46Z, 22:28Z) before one full review at `086a239b`; the lane merged on that review plus the later heads, which is the standing rule (one CodeRabbit review on record, later unreviewed heads are fine).

## Routing and Merge Behavior

- Review: CodeRabbit full review at `086a239b`, Merge Risk Low, 2 inline findings, both fixed in `9d0345a`. Re-read: `review-gate --pr-number 449` with the two dispositions passed answers `all_handled`, `total=2`, `handled=2`, `unresolved_review_threads=0` — the landing's numbers agree. Without the dispositions the gate answers `body_findings_undisposed`.
- CI/merge: merge-group run `43785810251` success; `origin/main` merged into the branch at `f8e0c9c` (the base had moved by #452, #450, #451), clean; the recipe's indent rewrite committed as `1ecaa608`.
- Sonar: the lane's `sonar_open_new_issues=0`, `quality_gate_status=ok`, `open_issue_rules=none`; the Sonar MCP search for pull request 449 returns no issues. Seven Sonar findings of an earlier head (S5906, S6571, S5778 ×3, S1612) were fixed in `60f046cf`.
- Landing check: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`, both honestly `unknown`; recorded as an Open Defect. The landing is reconciled as far as it goes.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-146 --status shipped`
- [x] row `pr` stamped — `449`
- [x] row `landing` stamped — `landings/PLAN-146.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-146-web-intent-epic-and-oversize`
- [x] epic.md: Open Defect `PLAN-146 landed`; the PLAN-145 residue note re-pointed (nothing of it was fixed here)
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **PLAN-163 is released** (it was held until PLAN-146 landed, for the mock files).
- **The PLAN-145 residue plan** (named at handle-open, NOT staged) now has a ready predecessor: add L3-1 to L3-3 of this landing to it (the unfiltered reload in both panels, `epicId` as a prop change, the Java `readObjects` fail-open) — one residue plan for the intent panels.
- PLAN-58 (the epic page) must handle L3-2 when it navigates between epics; named in its spec at emit, if the operator confirms.

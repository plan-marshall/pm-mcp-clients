# Landing Analysis: PLAN-154 — Web Views: Reports and the Completion Report (Views Package)

epic: ui-improvements
workstream: WS-03
pr: #539 (merged as 14c5ecc4, `feat(web): move ReportsView and CompletionReportView to shared vocabulary and resolve PLAN-158 residue`; 13 files, +1074/-640)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-154-web-views-reports-and-completion-report-001.md`, runbook lane, harness `antigravity`, model `unknown`, executor `0.1.2183`; analysed 2026-11-23). `landing-check`: `total_tokens`, `model` and `effort` are `unknown`. The message was read as a lead. The PR, the merge commit and Sonar were re-read; the layer-3 wire finding was re-read by the orchestrator at `14c5ecc4`.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Reports: the row and the group | shipped-modified: the row uses `StatusLabel` (dot plus word) on both states, in the slot list, tone `running` for unread; `reports.adoc:146` says a dot only on the unread row (the spec contradicts itself, the lane chose the slot list). RowLink exclusion holds; tag-class scan empty | `ReportsView.svelte:1278-1281`; `StatusLabel.svelte:17-18` |
| D2 Reports: head, filter, paging, states | shipped; only the filter-empty `EmptyState` is new code, the head, filter label and paging code is unchanged (`:1070-1103,1216-1237` not in the diff), so "fails on a revert" holds for one assertion | leaf |
| D3 Completion report: head and sections | **met in part**: only the summary and the token Total/Coverage use `KeyValueList`; Deliverables, Re-planning, Decisions, Own calls are still hand-built `<ul>` in Cards | `CompletionReportView.svelte:417,435,453,471` |
| D4 Completion report: tokens and browser evidence | **met in part**: By phase is still a hand-made `<table class="breakdown-table">` (`:548-579`) and Waste a hand-made list (`:631-670`); only by role and by model are `DataTable`. `lint:vocabulary` lists no tables, so its 0 findings does not show it. Six post-fix PNGs, no baseline set | leaf |
| D5 PLAN-158 residue | shipped: `listEpoch`, retry focus (`await tick()`), `markRead` guard, `errors.ts`, `ReportsCommandTest`; the deferred ProcessDesigner wording matches the code | leaf |
| added-unplanned | `common.{de,en}.properties` (+2 keys each), `vocabulary-baseline.json` (-3 lines); `reports.*.properties`, the two design docs and `theme-readability.spec.js` are declared and untouched | `git show 14c5ecc4 --stat` |

**"5 of 5 met" is the lane's count;** by the spec's own rule D3 and D4 are met in part, and the Layer-3 finding below makes the deliverable-state text wrong on real data.

## Three-Layer Analysis

By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, plan-less) — a full ship, two views, a Java test and a store file.
- **Verdict:** layer 1 findings (6 minor, 1 unverified); layer 2 findings (5 minor); layer 3 findings (1 major, 7 minor). No blocker.
- **Re-read by the orchestrator at `14c5ecc4`:** `common.en.properties:326-332` (the `state.task.*` keys: `running, done, not_done, dropped, failed, waiting, planned`); `CompletionReportView.svelte:111-116,415-425` (`displayState('task', d.state)`, `dropped` mapped to `act.cancel`); `reports-and-system.json:388,1169,1777` (the mock sends `not_started`). The engine's outline states (`done, open, infeasible, deferred, removed`, leaf: `pm-mcp-core phase-workflows`) are an unverified lead of the orchestrator; the leaf cites `08-dynamic-re-planning.adoc:102,124`.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 (minor) | D3 sections are not `KeyValueList` rows throughout | leaf | refutes "met" | `CompletionReportView.svelte:417,435,453,471` |
| L1-2 (minor) | D4 By phase and Waste are hand-built | leaf | refutes "met" | `:548-579,631-670` |
| L1-3 (minor) | `reports.adoc:146` (dot only when unread) vs the code (both states); three keys dead: `report.from.open_project`, `open_epic`, `open_plan`; `reports.adoc` and `completion-report.adoc` declared, untouched | leaf | spec self-contradiction | `ReportsView.svelte:1278-1281`; `reports.*.properties:159-161` |
| L1-4 (minor) | D2's "fails on a revert" holds for one assertion | leaf | refutes | `ReportsView.svelte:1070-1103,1216-1237` |
| L1-5 (minor) | files outside the declared surface: `common.*` keys, baseline shrink | leaf | benign | diff |
| L1-6 (unverified) | the D4 matrix: six post-fix PNGs listed, no baseline | not shown by reading | | message section 1 |

### Layer 2 — Implemented

The `listEpoch` test and the failing-Retry focus test are meaningful (they fail without the fix). Untested: the other order of the marker guard (mark-all fails first), the changed condition `retainedReadIds[item.id]` (`:645`), the `machine/epic/decision/report` KindMarks (`kindMarkOf` `:804-816`), the not-measured arm of `formatBreakdownSplit` (`:216-218`), the "all measured" coverage arm (`:254-257`), the `ExternalLink` arm without a PR number (`:381`), `outcomeTone` danger, the Chat click, German; the `done` deliverable assertion is a substring match. Java: the five tests draw ids and counts from `Generators` and the class has `@EnableGeneratorController`; literals remain only for `--project my-project` and two subjects.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `CompletionReportView.svelte:111-116,423`; `common.en.properties:328-329` (re-read); mock `reports-and-system.json:388` (re-read) | a deliverable whose wire state is `not_started` (mock) or `open`, `infeasible`, `deferred`, `removed` (engine, leaf) | the PR adds `state.task.not_done` and `state.task.dropped` and the test feeds `not_done`, but no wire value maps to them; no `state.task.not_started` or `open` key exists, so a real not-done deliverable reads the unknown word; `dropped` is unverified either way. Same class as PLAN-157 L3-1 (invented wire values) | major |
| L3-2 | `CompletionReportView.svelte:252,656` | any report with a coverage reason or a waste class | raw wire codes reach the operator (`terminated`; the waste class except `no_product`); `report.tokens.unmeasured_reason.*` exists (`reports.en.properties:105-107`) and is unused; `waste_share` always says "of measured". Pre-existing | minor |
| L3-3 | `:415` | empty deliverables | `phase: 'refine'` hard-coded, pinned by the test; pre-existing | minor |
| L3-4 | `:354,362,378`; `client.ts:1082` | any report | the head heading is `data.plan_id`, `data.title` is never read, the id is shown three times; the design head is `report.completion.title` | minor |
| L3-5 | `:786-790` | German "nicht fertig", "verworfen" | `.deliverable-state { width: 60px }` may clip; the only German capture is all `done` | minor, suspected |
| L3-6 | `errors.ts:101`; `display/index.ts:131-141` | a half-defined error code | `actFailureText` checks a `.fix` key where `displayError` picks the base by `.title` | minor |
| L3-7 | `ReportsView.svelte:1157-1163` and others | filter-empty state; no deliverables | title and message say the same; an empty actions slot renders; a mis-indented block; `pm-web-app` `quality` has no `format:check` | minor |
| L3-8 | `reports.*.properties:159-161` | none | three dead keys (see L1-3) | minor |

**Checked and sound:** `markRead` ordering (the epoch is bumped only on success); the mark-all failure path; the new keys exist in en and de with sound German; no `innerHTML`/`{@html}`; no timer wait in the new tests; the phase link `/plans/{id}?mode=ended&node={phase}` fits the mock's node ids.

**Not shown by reading:** whether `reports.spec.js` and `theme-readability.spec.js` pass; the engine's real deliverable states; the screenshots; whether a `Link` click navigates client-side.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Web Standards | no `{@html}`; no timer wait | conforms | leaf |
| CLAUDE.md § Dependencies | no dependency without asking | conforms | no POM or package file in the diff |
| CLAUDE.md § Code Standards | generators for input data in Java tests | partly conforms | literals `my-project`, two subjects |
| spec | tag-class scan of both views empty | conforms | leaf |
| CLAUDE.md | zero Sonar findings | conforms | `sonar_rest gate-status --pr 539` is `OK` (new coverage 100.0, duplication 0.0, hotspots reviewed 100); the lane states `0` open new issues |
| runbook lane | the mailbox read works | deviates | `inbox read --plan-id 154-…` is refused `invalid_target_plan` (digit-leading id) three times |

## Metrics and Anomalies

- Tokens `unknown`, `model` and `effort` `unknown` (harness `antigravity` measures none); executor `0.1.2183`.
- `quality-gate` (`verify -Ppre-commit`) hit its 870 s timeout at `f5e1e047` (5425 tests passed before it); the full `verify` passed at four heads.
- CodeRabbit refused on quota, the lane waited the 90-minute window and reviewed at the retrigger; one inline finding handled and resolved. Sourcery refused (optional).
- `guard: none on this harness (unenforced)`.

## Routing and Merge Behavior

- Review: `review_body_findings_total=1`, `handled=1`, `unresolved_review_threads=0` (the lane's `review-gate`: `merge_allowed=true`, `reason=all_handled`).
- Merge: `14c5ecc4` (merge queue, merge group `45098878330`).
- Overlap observed: the PLAN-154 footprint is disjoint from every staged spec's declared surface by `declaration-currency` (only `common.*` and `vocabulary-baseline.json`, shared files). PLAN-174's branch diffs `ReportsCommandTest.java` by +121/-5 against its merge base, so **PLAN-174 rebases over #539** (the Re-Scope note "D5's Java part rebases over PLAN-174" is reversed); a hunk-level conflict was not checked.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 539; `landing` `landings/PLAN-154.md`; `plan_marshall_plan_id` `rb-154-web-views-reports-and-completion-report`
- [x] epic.md: Open Defect for the layer-3 major and the minors; the PLAN-158 residue entries retired (see the Decisions entry of 2026-11-23); runbook-lane mailbox defect gets a recurrence line
- [x] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 major L3-1 is reported to the operator at once and not fixed inline;** a fix plan is staged only on the operator's word. It belongs with the PLAN-157 L3-1 decision (invented wire values); the minors ride the same carrier.
- New open item: the ProcessDesigner comparison-failure wording needs `designer.{en,de}.properties` keys, and the new test pins the old text.
- PLAN-174 rebases over #539.

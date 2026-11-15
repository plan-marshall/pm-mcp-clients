# Landing Analysis: PLAN-142 — Web: the column engine, the gap strip and the side column

epic: ui-improvements
workstream: WS-02
pr: #442

> Landing record for PLAN-142 (D2 navigation engine, part 1). Merged as `0b9402ec` (merge queue, `merge_group` run
> 43683695800 success). Lane: runbook, sender `rb-142-web-column-engine`; harness `opencode`, model
> `deepseek-v4.1-flash/high`, effort `high`, executor `0.1.2118`. Written by the `analyze` verb on 2026-11-15 from inbox message
> `rb-142-web-column-engine-001.md`, after verifying the claims against git, the provider and the merged tree.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| 1. The column window as one function | shipped-as-specified | `columnWindow.ts` (357 lines) and `columnWindow.test.ts` (348); the leaf recomputed the 1024/1280/1440/1920 closed cells and the collapsed-rail and chat cells by hand from the code and they match the tables. Progress file: failing run `cb9cf58e`, passing `9c6f7b95`. |
| 2. `TriagePage` as the column engine | shipped-as-specified, one clause left open | `TriagePage.svelte` renders only the window's columns (`:122-126`, `:361`), widths by `style:` (`:337`, `:367`), pinned row (`:373-379`). `filters`, `selected`, `hasFilters`, the 1099 px query and the 40/60 grid are gone. **"Each column scrolls on its own" is declared (`:439-440`) but inert**, because `AppShell.svelte` gives only `min-block-size: 100vh` (`:53,63,78`); the lane reported it and left it open. |
| 3. Gap strip, peek, focus, keys | shipped-modified | `GapStrip.svelte`, peek in `TriagePage.svelte:250-305`, announcement through `announceNavigation` in `runtime.ts` and a region in `+layout.svelte`. Two defects below (L3-1, L3-3). Ctrl+Arrow is suspended in a field (`:316`), where `navigation.adoc#keys` suspends only `[` `]` (see Conformance). |
| 4. Object column and side column | shipped-as-specified | `ObjectPage` has `compact` and no `path` slot and no side prop; `SideColumn.svelte` is used by `TriagePage.svelte:400` and `WizardPage.svelte:139` only. The `up` slot of the spec text landed as the `up` prop of `TriagePage` (`:78`, `:369`), as the design says. |
| 5. Migration, fixtures, browser evidence | shipped-modified | `ReportsView.svelte` moved; fixtures updated; `column-window.spec.js` (181 lines) and `mobile.spec.js` follow. The e2e models only the two-level reports chain (see Layer 2). `reports.spec.js` was not touched. The showroom fixture is broken on a phone (L3-6). |

## Three-Layer Analysis

Read on three layers by one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`, one leaf per landing), which
returned 15 findings, 12 conformance rows and one verdict per layer. The orchestrator re-read the points named below at their
`file:line` on the merged tree (`0b9402ec`) before recording them. The reading is **by reading, no build, no test run, no
write**.

- **Dispatch:** one read-only leaf per landing — reason: reading 31 merged files (3,165 insertions) against a five-deliverable spec
  is the depth the dispatch rule exists for, and no other landing was read in this firing.
- **Verdict:** layer 1 findings (3, all minor); layer 2 findings (5, all minor); layer 3 findings (2 major, 5 minor). **Layer 3 carries two
  major defects (L3-1, L3-2).**

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | The spec's D4 names an `up` slot on `ObjectPage`; the design puts Up in the head of the first visible column (a list column too). The merge follows the design. | checked | refutes | `TriagePage.svelte:78,369`; design text `navigation.adoc#up` **not re-read** (lead) |
| L1-2 | Neither the spec nor `navigation.adoc#gap-strip` says whether a peek at a middle level keeps a strip for the levels left of it; the code drops it, which produces L3-1. | checked | refutes | `columnWindow.ts:264-282` (re-read) |
| L1-3 | "Each column scrolls on its own" cannot hold on `main` without a definite shell height, and the Done-when lists only "no horizontal overflow". | checked | refutes | `TriagePage.svelte:439-440`, `AppShell.svelte:53,63,78` (re-read) |

**Layer 1 verdict:** findings — 3. The window rule and both width tables recompute exactly; the spec is weak where it leaves peek-with-hidden-left
levels unstated and where it assumes a scroll the shell cannot yet deliver.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `columnWindow.ts` | `columnWindow.test.ts` pins both tables, 743/744, 835/836, the 320 cap | none | checked |
| D2 | `TriagePage.svelte` | `TriagePage.test.ts` | `Level.href` and `Level.state` are accepted and documented but nothing in `TriagePage.svelte` reads either (grep for `.href` and `.state` returns nothing, re-read) | checked |
| D3 | `GapStrip.svelte`, `TriagePage.svelte` | `GapStrip.test.ts`, `TriagePage.test.ts` (`:313-326` stops at the plan column, so L3-1 has no test) | none | checked |
| D4 | `SideColumn.svelte`, `WizardPage.svelte`, `ObjectPage.svelte` | `WizardPage.test.ts`, `ObjectPage.test.ts` (asserts "no side" by source text) | `inColumn`, `pinned`, `up` props are undeclared in `components.adoc:366-381`, and no view passes `pinned`, `up`, `compact` or `inColumn` (`ReportsView.svelte:996` passes levels, label, column, object), so D2 and D4 are shown at fixture level only | checked |
| D5 | `column-window.spec.js`, `mobile.spec.js` | `column-window.spec.js` | `expectedWindow` models only a two-level chain, so the e2e never sees the strip, a peek or three or four columns (**not re-read**, lead); `reports.spec.js` untouched | checked |

Files the diff touched that no deliverable explains: `pm-api/src/test/java/.../ReportRecordsTest.java:1271`, a whitespace-only re-indent of one
continuation line (re-read: the diff is that single line), apparently formatter output. Files the diff added that the spec's Expected Surface did not
declare but D3/D4/D5 explain: `SideColumn.svelte`, `ColumnEngineFixture.svelte`, `GapStripFixture.svelte`, `runtime.ts`, `+layout.svelte`,
`tokens.test.ts`. Declared and not touched: `reports.spec.js`, `showroom.spec.js`, `setupTests.ts`, `pageWidth.test.ts`, `components.adoc`
(`TriagePageNoFiltersFixture.svelte` was deleted).

**Claims of the lane's report the diff does not bear out:**
- "`reports.spec.js` follows the engine": the file is untouched (re-read, empty diff). It may need no edit; the claim is wrong as worded.
- "Open items then fixed" after self-review round 3: `TriagePage.test.ts:355` and `column-window.spec.js:163` carry a statement fused onto the
  `async () => {` line (re-read), a late edit never run through the formatter. Maven runs lint only, so the build did not catch it; the module's
  `quality` script (`format:check`) would.
- 18 of 18 review findings handled: see Routing; the progress file holds 17 dispositions.

**Layer 2 verdict:** findings — 5.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `TriagePage.svelte:296-299`, `columnWindow.ts:241-277` | A five-level chain at a three-column window; peek the lens (`peekAt`), then press `]` toward the object. `moveFocus` calls `peekAt(next - columns + 1, next)`, which sets `peek` to a value that leaves no level on the right. | `navWindow` returns `peek: true`, strip `none`, `returnStrip: false`: the window shows the object with no strip for the hidden levels 0-1 and with the object 136 px wider than the unpeeked window. No way back by mark; the strip is gone. No test reaches it. (Re-read: the arithmetic holds for a 5-level chain at 3 columns.) | major |
| L3-2 | `TriagePage.svelte:341,408` | Any change of `layout.start`: rail toggle, a resize across a threshold, opening a chat that costs a column, any peek. | `{#key layout.start}` remounts every column, the gap strip and the side column. Scroll position, a draft in the chat snippet, state inside a `CompletionReportView` and the focused control are lost (the focus falls to body). The key exists to replay the slide animation. (Re-read.) | major |
| L3-3 | `TriagePage.svelte:181`, `columnWindow.ts:241-242` | Peek, then narrow the container to one column, then widen it again. | The peek state stays (the effect clears it only on a chain change; `navWindow` returns `peeked: null` at one column), so widening jumps the window back to the stale peek. (Re-read.) | minor |
| L3-4 | `TriagePage.svelte:399-406` against `WizardPage.svelte:79-99` | Container 744-927 px, a list row focused, press Open the chat. | The side column takes the space and every column unmounts with the focus inside; `TriagePage` has no focus handling for this transition (grep: focus calls only on peek, return, key move and chain change), where `WizardPage` moves focus into the chat and back. The focus lands on body, against D3 "the focus never falls to the page". | minor |
| L3-5 | `WizardPage.svelte:149`, `TriagePage.svelte:310-315,349` | Escape pressed while a dialog or menu of the page is open. | `WizardPage` hides the chat from a window listener (`<svelte:window on:keydown>`); `TriagePage` listens inside its window group only. Same key, different scope by template; on the wizard an Escape meant for a page widget also hides the chat unless that widget stops propagation (not shown by reading). | minor |
| L3-6 | `ColumnEngineFixture.svelte:54-62` | `/dev/components` on a viewport below 768 px. | The fixture hard-codes `sideOpen` and passes no `onHideSide`, so each theme block mounts the fixed full-screen overlay whose Hide the chat does nothing; the showroom is unusable on a phone. (Re-read.) | minor |
| L3-7 | `SideColumn.svelte:62-78`, `TriagePage.svelte:433-437,470-479` | Phone, chat opened. | The overlay is `position: fixed` inside `.triage-page__window`, which animates `transform`; for `--duration-base` the transformed ancestor is its containing block rather than the viewport (re-read: the keyframes carry `transform`). The background stays tabbable and unmarked (no `inert`, no `aria-modal`); the lane defers this to PLAN-144. | minor |

**Layer 3 not shown by reading:** L3-2's effect on a `CompletionReportView` inside its own key and on a chat draft, and whether the Svelte compiler
accepts the misplaced `svelte-ignore` at `TriagePage.svelte:340` (it sits before `{#key}`, not the `div`) and the `async` callback without `await` at
`GapStrip.svelte:57`: both are Svelte-lint and Sonar matters that only a run shows. Sonar's own count is `not re-verified` (see Routing).

**Layer 3 verdict:** findings — 7 (2 major, 5 minor), reported to the operator in the same turn, none fixed inline.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web Standards | no static `style` attribute | conforms | `style:` directives throughout (`TriagePage.svelte:337,367`); `TriagePage.test.ts:210` guards it |
| CLAUDE.md Web Standards | no `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `{@html}` | conforms | none in the 31 files read |
| CLAUDE.md Web Standards | no arbitrary timer wait in a test | conforms | `expect.poll` on the DOM (`column-window.spec.js:113`); `requestAnimationFrame` only in production code |
| CLAUDE.md Web Standards | texts from the catalogue, English and German | conforms | 10 `web.nav.*` and 5 `dev.column_engine.*` keys in both files; the dynamic `web.nav.pinned.${reason}` (`:377`) is not provable to a static scan |
| CLAUDE.md Dependencies | no dependency or plugin without asking | conforms | no `pom.xml` or `package.json` change in the diff stat |
| components.adoc closed vocabulary | column head uses `KindMark`, `Button`, `IconButton` | deviates | `GapStrip.svelte:94,113,129,149` are raw `<button>` elements (re-read) where the icon-only Names and return controls could be `IconButton`; that the vocabulary requires it for these controls was **not re-read** (lead) |
| navigation.adoc#keys | only `[` and `]` are suspended in a field | deviates | `TriagePage.svelte:316` returns for a field before testing Ctrl+Arrow, pinned by `TriagePage.test.ts:371`; the design lines `:232-243` were **not re-read**, and the spec's parenthesis is ambiguous (lead) |
| navigation.adoc#window | `W` is the template's container, never the viewport | deviates | engine and wizard measure their container; `ReportsView.svelte:209,242` still pick heading levels from `window.innerWidth < 1100` (re-read; harmless today, no chat there) |
| Spec D1 | `columnWindow.test.ts` pins every cell | conforms | leaf recomputation; test file not run |
| Spec D1 / D2 | hidden column absent from the DOM; list takes the object width | conforms | `TriagePage.svelte:122-126,361` |
| Spec D4 | side column on `TriagePage` and `WizardPage` only | conforms | `SideColumn` used at `TriagePage.svelte:400`, `WizardPage.svelte:139` |
| pm-web-e2e `quality` (`format:check`) | web files stay prettier-formatted | deviates | the two fused lines above (re-read) |

## Metrics and Anomalies

- Tokens: `unknown` (opencode measured none); recorded as unobserved, not zero.
- Duration: about 8 h wall (first failing run in `maven-2026-11-14-221156.log`, merge 2026-11-15 06:02 UTC).
- Anomalies: the lane ran on `opencode` with `deepseek-v4.1-flash/high`, not the usual model set; no guard on this harness (unenforced), so the rule-check
  counts the run's own actions. Three shell file operations (two `cp`, one `perl -pi`) were declared by the lane as rule breaks. Self-review hit its three-round
  limit with items open (`self-review:budget-exhausted`). `designer.spec.js` flaked in three of eight local suite runs and is reported as unattributed.
  The lane's attempt to attribute it on a clean `main` checkout was inconclusive.
- **Incomplete landing:** `landing-check` reports `complete: false`, `missing_keys` `total_tokens` and `plugin_version` (both `unknown`).

## Routing and Merge Behavior

- Review: CodeRabbit (required) reviewed heads `806cce67`, `24362ca9`, `b022a86c`; sourcery-ai refused on its own budget (optional). **Re-read against the
  provider (`runbook_lane review-gate --pr-number 442`):** `unresolved_review_threads` 0, matching the claim; `review_body_findings_total` 18, matching.
  **`review_body_findings_handled`: the claim is 18, the run's own progress file holds 17 `finding:` lines** (inline-1 to inline-13 and nitpick-1 to
  nitpick-4; **`inline-14` has no disposition line**). The provider shows all threads resolved, so this is recorded as a **false claim** about the
  disposition record, not as an unhandled finding; one of the 17 (inline-4, the keyboard focus behind the phone overlay) is a deferral to PLAN-144, not a fix.
- CI/merge: merge queue, `merge_group` run 43683695800 success; no rebase conflicts reported. PLAN-142 shares 3 files each with PLAN-149 and PLAN-145;
  both were still running at the landing, so the collision, if any, comes to those plans as a rebase.
- Sonar: the landing claims `sonar_open_new_issues=0`, `quality_gate_status=ok`, no open rules. **Not re-verified:** `sonar read_open_issues --pr 442`
  returns `count_status: undecidable` (`head_revision_unavailable`, the PR is merged), with the analysis on the last head `50e921ab`. Recorded as `unknown`
  on my side, not as `0`, and not as a difference.

## Reconciliation Actions

- [x] row `status` → `shipped`
- [x] row `pr` stamped `442`
- [x] row `landing` stamped `landings/PLAN-142.md`
- [x] row `plan_marshall_plan_id` stamped `142-web-column-engine`
- [x] epic.md narrative reconciled (decision, Open Defect for the incomplete landing and the false claim, Open Defect for the layer-3 defects, Watches for the layer-2 leads)
- [x] PLAN-144 spec folded (shell height, modal boundary, `up` correction); `## Expected Surface` updated in the same edit (53 to 56 entries, re-read through `corpus surfaces`)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **L3-1 to L3-7** go to the operator, who decides whether to stage a fix plan. Nothing was fixed inline. L3-1 and L3-2 are the ones to fix before any plan builds on the engine
  (PLAN-143, 144 and the chat plans 135 to 137 all do).
- The shell height and the modal boundary were folded into PLAN-144 (spec section "Folded from the PLAN-142 landing 2026-11-15").
- The lane's claim that PLAN-144 owns these two items was wrong when made: the PLAN-144 spec carried neither. Corrected by the fold.
- A hand-written `GapStrip` button where `IconButton` exists, the Ctrl+Arrow in a field, the e2e that models two levels only and the unread `Level.href`/`Level.state`
  are leads, recorded as Watches.

# Landing Analysis: PLAN-166 — Web column engine fixes

epic: ui-improvements
workstream: WS-02
pr: #450 (https://github.com/plan-marshall/pm-mcp-clients/pull/450)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message
> `rb-166-web-column-engine-fixes-001.md` (a lead, not a fact), the lane's progress file
> `.plan/temp/rb-166-web-column-engine-fixes/progress.md`, git, and a read-only three-layer leaf.

Merged 2026-11-15T22:45:44Z as `89baa99222384c4103e4739a45f25f64900f2ba9` (parent `2a58ba06`), head `9fee03ad`, 15 files, +800/-93.
Sender `rb-166-web-column-engine-fixes`; stamped `plan_marshall_plan_id` is the same name.

## Deliverable Fidelity vs Spec

The spec names the seven layer-3 defects of the PLAN-142 landing (L3-1 to L3-7) as D1 to D4 and adds a browser case (D5).

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 — a peek that ends at the object, and a stale peek after a resize (L3-1, L3-3) | shipped-as-specified | `columnWindow.ts:245` `<=` became `<`; `TriagePage.svelte:190-196` drops a stale peek (also one the window does not render, a small addition `navigation.adoc` states); tests in `columnWindow.test.ts` and `TriagePage.test.ts` fail on revert |
| D2 — no `{#key layout.start}`, slide only on the shifting columns (L3-2, L3-7) | shipped-as-specified | key removed (`TriagePage.svelte:481-552`); animation moved to `.triage-page__column--shift` (`:584-592`); scroll and chat-draft tests fail on revert. Residual: the "slide in progress" flag clears only on `animationend`, which jsdom never fires, so the clear is untested |
| D3 — focus into the chat and back, Escape order (L3-4, L3-5) | shipped-modified | effect at `TriagePage.svelte:409-433`; `defaultPrevented` guards on both templates. Partly met: the focus test starts from a column heading, not a list row; no `ConfirmDialog` witness for Escape (a `preventDefault` widget stands in); one real gap, L3-A below |
| D4 — fixture hides the chat; `svelte-ignore` placement; `GapStrip` async (L3-6) | shipped-as-specified | `ColumnEngineFixture.svelte:68-69` state plus `hideSide`; `GapStrip.svelte:57`; svelte-ignore on the window div. The lane's claim that the two fused statements were split here is wrong (see layer 2) |
| D5 — browser evidence for three- and four-column windows | shipped-modified (partly met) | `column-window.spec.js:183-333` adds a five-level chain case; it asserts "more than one column" and the measured count, so neither cell is pinned. The lane says so itself |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule, reading merged code is the depth it exists for, and one landing's read must not share context with another's. The orchestrator re-read the points marked "re-read" below at their positions on `89baa992`.
- **Verdict:** layer 1 clean (premises confirmed); layer 2 findings (one false claim, two weak tests, two untested halves); layer 3 findings — one major-in-effect, medium by the leaf (L3-A), three low.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | the old peek test used `<=` | checked | confirms | `columnWindow.ts:245` |
| L1-2 | `{#key layout.start}` wrapped window, columns, strips and side column | checked | confirms | `TriagePage.svelte:481-552` |
| L1-3 | the peek was cleared only on a change of the chain | checked | confirms | `TriagePage.svelte:190-196` |
| L1-4 | the animation sat on the window, the side column is a sibling of the columns | checked | confirms | `TriagePage.svelte:584-592` |
| L1-5 | the wizard already moved the focus into the chat, the triage page did not | checked | confirms | `WizardPage.svelte:79-104` |
| L1-6 | the fixture's `sideOpen` was a constant | checked | confirms | `ColumnEngineFixture.svelte:68-69` |

**Layer 1 verdict:** clean — 6 premises confirmed, none refuted.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|----------------------------------|---------|
| D1 | `columnWindow.ts:245`, `TriagePage.svelte:190-196` | "returns the object its own window when a peek ends at the object", "clears a peek when a resize leaves one column", e2e deep chain | none | checked |
| D2 | `TriagePage.svelte:481-592` | scroll-position and chat-draft tests; "slides a shift on the columns" (source check). The "no transformed ancestor" test passes in jsdom at the base too (its own comment says so); the reduced-motion test **passes at the base** | none | checked, two weak tests |
| D3 | `TriagePage.svelte:409-433`, both `defaultPrevented` guards | "moves the focus into the chat and back", "leaves an Escape the page already handled". Moving the wizard's Escape off the window listener has no test that fails | `WizardPage.svelte:107` adds a `svelte-ignore a11y_no_static_element_interactions` | checked |
| D4 | `ColumnEngineFixture.svelte`, `GapStrip.svelte` | fixture test; the GapStrip and svelte-ignore changes have no test, only lint or Sonar | the narrative's "two fused lines split in D4" — **not in this diff**: they were split on `main` by #447 (`6e31ae0a`, an ancestor of `2a58ba06`; re-read, `git merge-base --is-ancestor`) | checked |
| D5 | `column-window.spec.js:183-333` | the case fails if D1 is reverted; does not pin 3 or 4 columns | none | checked |

Files outside the spec's Expected Surface, each named by the lane: `.gitignore`, `components.adoc`, `navigation.adoc`, `ColumnEngineFixture.test.ts`, `TriagePageFixture.svelte`, `onboarding.spec.js`. `onboarding.spec.js:104-105` repairs a regression PLAN-166's own reformat PR #447 put on `main` (prettier lowercased `#0E1116` in `tokens.css:153` and `:230`); #447 was merged with `skip-bot-review` and the default build does not run the e2e suite. `SideColumn.svelte` is in the Expected Surface and untouched, which is fine.

False or overstated claims of the landing message: the D4 split (above); the reduced-motion test as test-first evidence (it cannot fail at `0b9402ec`). Not shown by reading: 406 and 415 passed, the failing-log lines, `sonar_open_new_issues=0`, the 117- and 4-file format counts, the `designer.spec` flake result.

**Layer 2 verdict:** findings — 2 weak tests, 1 false claim, 2 halves with no failing test, D3 and D5 partly met.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-A (re-read) | `TriagePage.svelte:409-427` | (a) the page loads with the chat already open where it takes the space and nothing in the page had focus; (b) the chat is docked, the person types in it, then the container narrows so the chat takes the space. Saved focus and saved column are empty | on "Hide the chat" the effect returns early at `:423` (`remembered === null && index === null`), the Hide button disappears and the focus lands on the body — the thing D3 forbids. `WizardPage` has the same gap, inherited. No test: every focus test focuses a column heading first | medium (leaf) |
| L3-B | `TriagePage.svelte:412` | the chat starts taking the space while the focus is in the docked chat editor or a shell control (rail toggle) | the focus is moved to the chat's first button regardless of where it was; the way back (`:428-433`) checks `stillOurs`, this step does not | low |
| L3-C | `TriagePage.svelte:333-357` | a second change of the window's start during a running slide | the in-progress flag is already set, so the shift does not replay on the columns that stay | low |
| L3-D | `ColumnEngineFixture.svelte:12-18` | the showroom after `hideSide` | the chat cannot be reopened without a reload; enough for the Done-when | info |

**Layer 3 not shown by reading:** the Playwright suite at the head had 4 failures in `data-table.spec.js` (the lane's own note says they are outside the plan; the cause needs a run). The PR body reports the full verify and the pre-commit gate green at `0d012da7`; the CodeRabbit fixes landed afterwards in `d217decc` with no local verify stated.

**Layer 3 verdict:** findings — 1 medium, 2 low, 1 info.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | ES modules; no `innerHTML`/`{@html}`; no timer wait in tests; scripts kept | conforms | none added; e2e uses `expect.poll` |
| CLAUDE.md Code | fix warnings, do not suppress | deviates (minor) | `WizardPage.svelte:107` adds a `svelte-ignore` |
| CLAUDE.md Git | commit trailer | conforms | squash message ends with the plan-marshall trailer |
| spec Standing | no static style attribute; no new dependency; no new text; templates keep their props | conforms | widths stay `style:` directives; no `package.json`/`pom.xml` in the diff |
| jest-testing | assertions must be able to fail | deviates | the computed-transform loop and the reduced-motion test, above |

## Metrics and Anomalies

- Tokens: `unknown` (the lane measured none; recorded as unobserved, not zero).
- Duration: not stated by the landing; PR created and merged on 2026-11-15.
- Anomalies: the reformat PR #447 (`skip-bot-review`) reached `main` with an e2e assertion red; the e2e suite is not in the default build, so only a `-Pe2e` run shows it.

## Routing and Merge Behavior

- Review: CodeRabbit reviewed `0d012da7` (2 inline findings, both fixed in `d217decc`); not re-triggered after the push, with a qualifying review on record. Re-read: `review-gate --pr-number 450` with the two dispositions passed answers `all_handled`, `total=2`, `handled=2`, `unresolved_review_threads=0` — the landing's numbers agree. Without the dispositions the gate answers `body_findings_undisposed` (it has no store).
- CI/merge: merged by the lane; merge-group path; no conflicts recorded.
- Sonar: the lane claims `sonar_open_new_issues=0`; the Sonar MCP search for pull request 450 returns no issues; the dedicated `sonar read_open_issues` read was not repeated here.
- Landing check: `complete: false`, `missing_keys` `total_tokens` and `plugin_version`, both honestly `unknown`; recorded as an Open Defect. The landing is reconciled as far as it goes.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-166 --status shipped`
- [x] row `pr` stamped — `450`
- [x] row `landing` stamped — `landings/PLAN-166.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-166-web-column-engine-fixes`
- [x] epic.md: the PLAN-142 layer-3 defects L3-1 to L3-7 marked fixed by #450, with L3-A to L3-C as new residue; Open Defect `PLAN-166 landed`
- [x] resume anchor and `queue-view.md`
- [ ] the inbox message archived on consume

## Follow-Ups

- **Residue plan (named, NOT staged; the operator decides):** L3-A (the focus falls to the body on Hide the chat, `TriagePage` and `WizardPage`), L3-B, the two weak tests, the untested Escape move off the window listener, and the 4 `data-table.spec.js` e2e failures and the `onboarding.spec.js` regression class. It belongs after PLAN-144 on `TriagePage.svelte`.
- PLAN-144 is no longer waiting on PLAN-166; it still waits on PLAN-143's open defects only if the operator folds them (see the PLAN-143 report).

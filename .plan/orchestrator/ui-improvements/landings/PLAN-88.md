# Landing Analysis: PLAN-88 — Web: Process Designer (rescoped to the D1 component sweep)

epic: ui-improvements
workstream: WS-03
pr: #528 (merged as 5c1c2730, `feat(designer): component sweep of process designer (D1)`; 12 files, +351/-481 against the first parent)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-88-web-designer-panes-001.md`, runbook lane, harness `antigravity`; `landing-check` **incomplete**: `total_tokens`, `model` and `effort` are `unknown`). The message was read as a lead and corroborated against git, the provider (`ci pr view`, `review-gate`, `sonar_rest`), the lane's progress file and one read-only leaf.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Designer — as amended (2026-11-06 amendment; operator decisions 2026-11-10: pane layout DROPPED, the plan is the D1 component sweep only) | shipped-modified (the sweep is delivered; the objective's panes and overflow menu are dropped by the operator's word, and the design-system-v2 constraint is only partly met) | SegmentedControl for the four intake modes `ProcessDesigner.svelte:1037-1045`; leave Dialog `:1384-1400`, place Dialog `:1402-1539`; `open_paste` / `open_template` `:1075`, `:1082`; frontmatter TextFields `:1200-1283`; project SelectField with `ariaLabel` `:988-994`; `designer.back`, `designer-back` and `leaveDesigner` are gone (whole-tree search at `5c1c2730` hits only two prose notes in the ledger); en and de property diffs are symmetric; z-index allowlist entry removed `tokens.test.ts:58` |
| added-unplanned | accepted: shared `SelectField` gains an `ariaLabel` prop (`SelectField.svelte:43`, `SelectField.test.ts`), additive; `DesignerCatalogueTest.java`, `vocabulary-baseline.json`, `tokens.test.ts` are forced by the rename and the constraints | the lane's "no surface drift outside the expected surface" is false for these five files |
| Declared surface not touched | `WorkflowValidationResults.svelte` (declared, untouched; still an acting-link text button `:68-72` and `on:click` on a ghost Button `:56`) | see layer 2 |

**Done-when 1 as written cannot hold** after the operator's decision of 2026-11-10 (the AGREED layout puts validation as a band BELOW the workspace, `ProcessDesigner.svelte:1349-1355`, re-read). Its only reading that still holds is "the jump-to-line behaviour keeps working in the designer spec flow", and that is met at unit level only (`ProcessDesigner.test.ts:493-519`); the e2e test `designer.spec.js:176-198` asserts `line > 0` (always true) and never names `designer.unit-invalid` (zero occurrences, re-read). **The amended criterion, recorded here: Done-when 1 = "the sweep's components and the jump-to-line unit test"; "results above the text" is dropped, not delivered.** Done-when 2 (the Playwright MCP baseline and after snapshots) is met by the lane's report; the artefacts live under `.plan/temp/playwright-mcp/` and were not read (an unverified lead).

## Three-Layer Analysis

The landing was read on three layers — the spec, the deliverable trace, and the merged code — by one read-only leaf, and the orchestrator re-read the points recorded here as `checked` at `5c1c2730` (the ones named with a line below; the leaf's remaining `info` confirmations were not re-read). The reading is **by reading, no build, no test run, no write**: what only a run could show is tagged `not shown by reading`.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`, plan-less) — reason: every full ship gets its own leaf; a 759-line rewrite of one view with two dialogs is the reading depth the dispatch rule exists for.
- **Verdict:** layer 1 findings (4) and not shown by reading (1); layer 2 findings (6 minor) and not shown by reading (2); layer 3 findings (8 minor, no blocker, no major) and not shown by reading (1).

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 (major, spec) | Done-when 1 contradicts the 2026-11-10 decision: results above the text vs the AGREED bottom band | checked | refutes | `ProcessDesigner.svelte:1349-1355` (band after the workspace, re-read); `process-designer.adoc` Layout |
| L1-2 (minor) | `designer.unit-invalid` is a mock state id; the spec does not say how it maps to a UI path, and no landed test names it | checked | refutes | `designer.spec.js` (0 occurrences, re-read); `designer.json:426` |
| L1-3 (minor) | the objective's "installed units as a compact list with an overflow menu" is not delivered and the amendment does not say it was dropped | checked | refutes | `ProcessDesigner.svelte:1103-1138` (rows are icon, raw text button, level, inline Remove button) |
| — | the PLAN-149 leads hold: SegmentedControl throws outside 2..5 (the designer passes a static 4); Dialog's capture-phase Escape; the z-index allowlist entry existed and is removed | checked | confirms | `SegmentedControl.svelte:23-25`, `ProcessDesigner.svelte:77-82`, `tokens.test.ts:58` |
| — | Done-when 2 browser artefacts | not_shown_by_reading | none | under `.plan/temp/`, outside the leaf's reach |

**Layer 1 verdict:** findings — 4, not shown by reading — 1. No corpus claim is named by a finding, so no verdict is persisted (the spec's two claims already carry `corroborated` and `unverifiable`).

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| Sweep (SegmentedControl, Dialogs, open buttons, fields) | `ProcessDesigner.svelte:1037-1539` | `ProcessDesigner.test.ts` (button → radio role queries only, 5 lines in and 5 out) | `SelectField` prop, `DesignerCatalogueTest` (explained above) | checked |
| `closePlace` guard against dismissal while placing | `ProcessDesigner.svelte:921` (`if (isPlacing) return;`, re-read) | **none**: no test mentions `isPlacing`, `closePlace`, Escape or Cancel; deleting the guard breaks nothing | — | checked, refutes the claim "tested" |
| Escape and keyboard on the two Dialogs, intake keyboard use | `Dialog.svelte:93-107` | **none** | — | checked |
| e2e "jumps to the line of a validation finding" | `designer.spec.js:176-198` | **cannot fail**: `split('\n').length` is at least 1, the assertion is `> 0` (re-read) | — | checked, refutes |

Further layer-2 findings (all minor, all `checked`, the first four re-read): the design-system-v2 constraint ("no plain text button, no acting link") is not met: plain text buttons at `ProcessDesigner.svelte:1112` and `:1327`, a raw icon input `:1245`, raw radios `:1411`, `:1426`, a raw file input `:1054`, and the untouched acting link in `WorkflowValidationResults.svelte:68`; the baseline ratchet records the remaining raw tags (select 1→0, button 7→3, input 13→4, textarea 2→1) instead of removing them, and it only shrinks as required. "`on:click` replaced by `onclick`" is only partly true (`ProcessDesigner.svelte:1130` still uses `on:click`). The design document is updated but drifts: the mockup labels are not the catalogue strings, per-row buttons it shows were never rendered, `:112` still says leaving the designer asks first, `:267` promises a Remove confirmation the code does not have. "58 tests" is a total, not an addition (45 `it()` declarations, four `it.each`): `not_shown_by_reading` whether they pass, and Sonar and the green build likewise.

**Layer 2 verdict:** findings — 6 minor; not shown by reading — 2.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `ProcessDesigner.svelte:816-832` (re-read; the guard at `:921` covers `place()` only) | Open Place…, click Compare with a project target (a network fetch), press Escape | `compareForPlacement` awaits with `isPlacing` false and no loading state, then sets `placeStage` to `'diff'` or `'failed'` unconditionally: the dismissed dialog reopens. Cancel allowed this on the base; Escape (new with Dialog) adds a path | minor |
| L3-2 | `designer.en.properties:25`, `:33` (re-read) | open another unit with a changed draft | the leave dialog says "Leave the designer?" and "…and leave the designer", but with "Back to the designer" gone the designer is never left; the e2e test pins the old wording (`designer.spec.js:204`) | minor |
| L3-3 | `ProcessDesigner.svelte:1962-1964` (re-read) | open the place dialog with a side-by-side diff | the `:global([data-testid="place-dialog"])` width has specificity (0,1,0); `Dialog.svelte:195`'s scoped rule is (0,2,0) and wins, so the diff dialog most likely stays 560px (by the cascade, not run); it is also an "own width" by the spec's rule | minor |
| L3-4 | `ProcessDesigner.svelte:929-932` (re-read) | click Remove on an installed unit | the unit's copy is deleted at once; `designer.remove.title` / `.confirm` exist unused and the doc promises a confirmation; predates the landing | minor |
| L3-5 | `ProcessDesigner.svelte:993`, `:1040` (re-read) | screen reader | the project SelectField and the SegmentedControl carry the same accessible name "Where does the unit come from?", wrong for a project selector | minor |
| L3-6 | `ProcessDesigner.svelte:1242-1243` (re-read) | screen reader | the icon `Field` has no id, so its label is not tied to the input; the file Field's hint is not linked to its raw input | minor |
| L3-7 | `ProcessDesigner.svelte:1384-1386` (re-read) | discard a draft | the leave Dialog does not use Dialog's danger/consequences for the destructive act | minor |
| L3-8 | `ProcessDesigner.svelte:1499` | place dialog, progress stage | an empty footer row renders (cosmetic) | minor |

**Layer 3 not shown by reading:** L3-9: German label width. The four intake labels are the long catalogue strings (about 72 characters English, 96 German) in a `white-space: nowrap` inline-flex control with no overflow handling (`SegmentedControl.svelte:98`); overflow between 640 and 767px is likely by character count, pixels need a run.

**Layer 3 verdict:** findings — 8 minor, no blocker or major; not shown by reading — 1. The leaf also confirmed (info, not re-read): Escape with both dialogs open closes one per press (`Dialog.svelte:93-104`); the roving-tabindex arrows are harmless here; the project-select sync is correct; no `{@html}`, `innerHTML` or numeric z-index remains.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Web Standards | ES modules only; no `innerHTML` / `{@html}`; no arbitrary timer wait | conforms | `designer.spec.js:1-2`, `:176-195`; `ProcessDesigner.svelte` renders through `{expression}` |
| CLAUDE.md § Dependencies | no dependency added | conforms | no `pom.xml` or `package.json` in the diff |
| CLAUDE.md § Code Standards | JUnit 5 only, literal contract values, en and de carry the same keys | conforms | `DesignerCatalogueTest.java:21-31` (passing needs a run) |
| Spec standing constraint | text-guard baseline only shrinks | conforms | `vocabulary-baseline.json` diff, counts match the landed file |
| Spec standing constraint | every touched view uses design system v2 components | **deviates** | `ProcessDesigner.svelte:1112`, `:1327`, `:1245`, `:1054`, `:1411`, `:1426`; `WorkflowValidationResults.svelte:68` |
| Spec standing constraint | the design document updated; no own page width | **deviates** | doc drift listed in layer 2; `ProcessDesigner.svelte:1962` |
| Spec standing constraint | server values through the display layer; 0 Sonar issues; green build | not_shown_by_reading (Sonar read separately, below) | `unit.name`, `state.id`, `node.title` rendered raw at `:1118`, `:1332`, `:1374` (predates the landing) |

## Metrics and Anomalies

- Tokens: `unknown` (the harness measured none; recorded as unobserved, not zero). Model and effort `unknown` as well: **the landing is incomplete** (`missing_keys`: `total_tokens`, `model`, `effort`).
- Duration: not stated by the message (the review thread times run 21:43Z to 21:58Z on 2026-11-21; the message was filed 22:17:51Z).
- Builds: quality-gate job 664 s and module-tests job 527 s, both through the build server (`daemon_longpoll`, 5418 tests executed); the lane's own report, not re-run.
- Anomalies: the lane's "no surface drift" and "tested" claims do not survive the reading (above). `guard: none on this harness (unenforced)`.

## Routing and Merge Behavior

- Review (re-read 2026-11-22T09:46Z): `unresolved_review_threads` 0, matches the landing; `review_body_findings_total` 4, matches. `review_body_findings_handled` 4 is **re-verified from the progress file** (four `finding:` lines, `progress.md:26-29`); the bare `review-gate` call reports `handled 0` and `merge_allowed: false` because it reads dispositions from nowhere on a re-run, so that output is not a difference. All four CodeRabbit findings carry a resolved thread. Observation: finding 1 (the catalogue test's hand-written key list mirrors the design table; the project path instruction calls that a defect unless derived) was resolved with the reason "mirrors without parsing AsciiDoc", which restates the finding rather than answering it, and the leaf confirms the list is still hand-written. Sourcery was rate-limited and skipped.
- CI/merge: 20 checks green, merged by PR state (`ci pr view`: `merged`, `merge_commit_sha 5c1c2730`).
- Sonar: `sonar_open_new_issues=0` **confirmed** by `sonar_rest search --pr 528` (0 issues) and `gate-status` (`OK`, all conditions OK), read 2026-11-22 at the merged PR.
- Collisions: none observed against PLAN-153, PLAN-174 or PLAN-175 (their worktrees show no conflict signal; PLAN-153 shares no file with the 12).

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-88 --status shipped`
- [x] row `pr` stamped — `queue --set-row PLAN-88 --field pr --value 528`
- [x] row `landing` stamped — `queue --set-row PLAN-88 --field landing --value landings/PLAN-88.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-88-web-designer-panes` (the sender, per the runbook-lane rule)
- [x] epic.md: Open Defect "PLAN-88 landed" (incomplete landing folded into it; no fix plan staged)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- The layer-3 minors (L3-1 to L3-8), the unmet design-system-v2 constraint, the stale leave-dialog copy, the design-document drift and the three test gaps are **reported to the operator and not staged**; a fix plan (or a fold into a staged web plan: `designer.back`-free copy, a compare-reopens guard, `WorkflowValidationResults` swapped to `Button`/`Link`, the leave dialog on Dialog's danger form, three tests that can fail) follows only on the operator's word.
- The pane layout stays dropped (operator, 2026-11-10); the amended Done-when 1 is recorded above.

# PLAN-35 — Web: Data Components — landing

PR #302, merged into `main` as `5650c41e` (merge queue). Lane message `rb-35-web-data-components-001`. 31 files, +1628/−0.
Analysed in three layers (specified / implemented / correct) by reading; nothing was built or run by the orchestrator.

## Verdict

| Layer | Verdict |
|---|---|
| 1 Specified | The spec is met in every deliverable and the lane followed it. |
| 2 Implemented | All 7 declared surface files are present; 24 files beyond the declaration, all explained. |
| 3 Correct | Sound by reading; the tests prove the component's markers, not its CSS. Four leads, none reproduced. |

## Layer 1 and 2 — what was specified, what the diff holds

- D1 `DataTable`: fixed column widths from the spec (`<col style="width">`), sticky head in its own scroll area, right-aligned tabular numbers, rows 40 px / 32 px compact, row-menu column, `empty` slot spanning every column. All present in `DataTable.svelte`.
- D2 `EmptyState` (icon or brand mark, title, one sentence, one act), `ErrorState` (code folded under `Details`), `Skeleton` (row, card, table, figure; shimmer off under reduced motion). All present.
- D3 `doc/design/web-app/components.adoc`: 16 component sections (3 controls beyond the spec's list are in too), 17 pictures, linked from the README. The document's claim "a test beside every component" holds (IconButton's tests sit in `Button.test.ts`, as the document says); `tokens.test.ts` and `catalogue.test.ts` exist.
- Surface delta (by hand, the verb could not measure): declared 7 of 7 present. Beyond the declaration, 24: 17 SVG pictures (D3 asked for one per component), 4 test files, 1 fixture (`DataTableEmptyFixture.svelte`), 2 catalogue files `common.{en,de}.properties` (load-bearing: the dev page reads every word through `$t`). `expansion_detected`, no unexplained file.
- The spec's pre-commit drift (≈178 `pm-api` files) was not in the PR: `main` already carried it as `f5de8449` (#298); the squash holds 31 files and none under `pm-api/src/main/java`. Claim verified. PR #294 is therefore redundant (open decision).
- Nothing calls the four components yet (no view imports them); the view plans do. By design, reported by the lane.

## Layer 3 — read, not run

Sound by reading: every token the components use is declared; `Button` has the `secondary`/`primary` variants used; icons `act.more` and `state.error` exist; text reaches the DOM through text sinks only; the empty-slot `colspan` includes the menu column; `ErrorState` draws no `Details` without both code and label; the `Skeleton` listener is removed on unmount (tested).

Gaps and leads:

1. The numeric-alignment, density (40/32 px) and reduced-motion tests assert attributes the component itself writes (`data-align`, `data-density`, `data-motion`), not the CSS that acts on them. The test names say "in the browser" for the heights and are honest about it; the browser artifacts carry the rest. Weak as regression protection.
2. Lead, not reproduced: `border-collapse: collapse` with a sticky head — the head's bottom border usually stays behind when the rows scroll; cosmetic.
3. Lead, not reproduced: the column type allows `20%` (`width: '16rem' | '20%'`) while the table is `max-inline-size: max-content` with `table-layout: fixed`; only `rem` widths were checked in the browser.
4. The row-menu name is `${menuLabel}: ${first cell}`, composed in the component with a fixed colon rather than a catalogue string. Same in en and de, so not a defect today. `onMenu` only reports the row; the menu itself is the caller's.

## Evidence checked

- Browser artifacts exist: 16 files `plan-35-{en,de}-{1440,390}-{before,after}.{png,yml}`; the `en` 1440 after-snapshot shows the three tables with their column headers and the per-row menu names, the 390 shot is 375 px wide (no sideways page scroll).
- Sonar: lane read `confirmed` 0 at `eecf5a31`. My read now is `undecidable` (`head_revision_unavailable`, analysis revision `eecf5a31…`, the lane's head), so it cannot be re-confirmed after the merge — recorded as an unverified lead, not as 0. The analysis did run on the lane's head.
- Reviewers: CodeRabbit skipped by label `skip-bot-review`, Sourcery refused on quota; no qualifying review. The merge rested on the operator's explicit selection in the lane, named in the message (§6), not on the standing authorization.
- Lane flake: `ProcessDesigner.test.ts > opens a pasted text in the editor` timed out once, green on re-run, file untouched by the plan.

## Follow-ups

- Re-read the specs that name these components: PLAN-43, 51, 53, 55, 58 (templates and views), against `DataTable`'s prop surface (`columns`, `rows`, `menuLabel`, `onMenu`) and `components.adoc`.
- Close PR #294 (redundant).
- Strengthen the CSS-level assertions where a view plan touches the table (leads 1–3).

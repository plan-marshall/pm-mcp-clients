# PLAN-90 — Web: the Green e2e Baseline and the Time Residue — landing

PR #304, merged into `main` as `3f26b6c2` (21 files, +615/−115) through the merge queue. Lane message `rb-90-web-green-baseline-and-time-residue-001`.
Analysed in three layers (specified / implemented / correct) by a read-only sub-agent that read the merge diff before the lane's report; the orchestrator
verified the findings it records against `main`. Nothing was built or run.

## Verdict

| Layer | Verdict |
|---|---|
| 1 Specified | D1, D2 and D3 met in code. The green e2e run, the Sonar `0` and the browser checks are the lane's account (logs not opened). |
| 2 Implemented | Every changed file is in the Expected Surface or justified. One undeclared behaviour (`source.time` fallback, finding 6). `theme-readability.spec.js` and `text-guard-baseline.json` were declared and not touched, rightly: the guard went green through a code fix. |
| 3 Correct | No defect found. The e2e edits correct stale expectations against a design sentence; none weakens an assertion. Seven leads and nits, none reproduced. |

## Layer 1 and 2 — what was specified, what the diff holds

- **D1 e2e green.** The `epic-view.spec.js:199` change (`340 k out` to `0.34 M out`) follows `texts-and-languages.adoc:221` (the split takes the unit of its total), implemented in `format.ts` `formatSplit`. The `plan-view.spec.js` (`openPlan(page, 'Fix Relay Transport')`, lines 27-41) and `showroom.spec.js:89-95` edits replace a positional `.first()` with a title filter; `plan-overview.adoc:102-103` says the runtime serves the plans that need the operator first and a client does not sort again. `openPlan` asserts the card, then the plan view, and the breadcrumb and token assertions after it are unchanged. The text-guard failure was fixed in code (`processNode.ts`, `getNodeStateText` printed `waiting_for_operator` raw), not in `text-guard-baseline.json`, so the baseline-only-shrinks rule holds.
- **D2 time residue.** The 13 new `common.time.*` keys are in both `en` and `de`. `since` (`format.ts` ~144), `dayOf` and the clock-only day header (`PlanView.svelte` ~555-577) are present. `project.time.*` is removed and `git grep "project\.time\."` finds no reader.
- **D3 SystemView.** 18 lines of `SystemView.svelte`: `system.models.error` and `retry` reused, `system.models.ok` added to both packs, `loadError` is a boolean and the text is read through `$t` in the markup.
- **Beyond the declaration, all justified:** `processNode.ts` and `processNode.test.ts` (the code-wrong fix for the text guard), `PlanView.svelte` and `PlanView.test.ts` (the day header lives there), three doc-comment-only edits (`OverviewView`, `ProjectExplorerView`, `SettingsView`), `texts-and-languages.adoc` (the design moves in the same PR).
- **Merge rule.** CodeRabbit reviewed heads `c41df5cd`, `dc52c617` and `88846137` (the last at 08:27); every finding of it is fixed or answered, per the lane; later heads were re-triggered and refused on quota. Conforming. No label.
- **Sonar.** Lane: `0` confirmed at the merge head. My read after the merge is `undecidable` (analysis revision `c5469b41`): an unverified lead, not a `0`.

## Layer 3 — read, not run

Sound by reading: no `waitForTimeout`, `setTimeout`-only wait, `innerHTML` or `{@html}` in the changed files; the `ok` case overrides the pack (`einverstanden`) and can fail against a literal; the streamed-row midnight test separates the read day from the arrival day (two headers, the second "today"); future-instant and cross-midnight handling look right.

Leads and nits, none reproduced:
1. **Locale argument does not select the words.** `format.ts` `word()` takes `locale`, but `translate()` (`i18n/index.ts` ~759) reads the active store. `format.test.ts` calls `useCatalogue(locale)` first, so no test fails on a mismatch. Production callers pass `$currentLocale`, so it is latent.
2. **Token marks are still literals** (`format.ts:285-290`: `Tsd.`/`k`, `Mio.`/`M`; `formatSplit`: `rein`/`raus`, `in`/`out`) while the design (`:171`) says the packs fix them. The keys `common.tokens.thousands/millions/split/at_least` exist and no code reads them (verified: only `coverage` and `not_measured` are read). The spec scoped D2 to `common.time.*`, so not a defect of the plan; open design debt.
3. **`since` on completed-event cells** (Ended, last used, last active): "Last used: since 14:02". The design (`:180`) says it, and the operator kept the wording in the lane. Closed unless the operator reopens it.
4. **Dead key** `project.explorer.last_active` in the project packs: no reader, pinned only by `ProjectsCatalogueTest`.
5. **`dateOnRead` does not validate the clock** (`PlanView.svelte` ~335-342): `25:99` builds `...T25:99:00`, `formatTime` returns '', and `|| timestamp` prints the constructed ISO-like string. A clock without seconds gets a fabricated `:00`.
6. **`source.time` fallback** (`PlanView.svelte:361`) is untested; `dayLabel` scans backwards per render (quadratic only for many undated rows).
7. **`dayOf`** has no direct test; its clock-only branch is dead in PlanView (the row is already dated) and would shift at midnight for another caller. Under-a-minute ages for today now read `since HH:MM`; the old "just now"/"N min ago" cases for today are gone, and `Intl.RelativeTimeFormat` is tested for months only.
8. **e2e positional pattern remains** (verified): `plan-view.spec.js:221` (asserts `project=pm-mcp-clients` through `plan-card .first()`), and the same `.first()` in `theme-readability.spec.js` (258, 278, 625, 722), `chrome.spec.js:85`, `showroom.spec.js:41`, `overview.spec.js:147`.

## Evidence checked

- Diffstat and diff by `git show 3f26b6c2`; `git grep` for the removed `project.time.*` keys and the unread `common.tokens.*` keys; `plan-view.spec.js:221` on `main`.
- PR #304: CodeRabbit reviews at 06:21, 07:28 and 08:27 (heads `c41df5cd`, `dc52c617`, `88846137`) plus empty acknowledgements; no label; merged 10:06.
- Not checked (lane account): the e2e run `333 passed`, the browser captures, the lane's Sonar `0`.

## Follow-ups

- Findings 1, 2, 4, 5, 6, 7 and 8 collected into PLAN-93 (one pull request).
- The `since` wording (3) is the operator's closed decision; the lane's residue list repeats it.

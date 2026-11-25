# PLAN-55 landing record

epic: ui-improvements
plan: rb-55-web-plan-page (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` and `plugin_version` `unknown`; executor 0.1.2183)
pr: #576, squash `163e2a49` (25 files) on `7a4e3b67`, merge queue (merge-group run 45647218636 success per the message), merged 2026-11-25T15:50:32Z
analysed: 2026-11-25, by sub-agent in three layers, by reading only (no build); `OutlinePanel.svelte:151` (`id={deliverable-N}`, present at base), `PlanView.svelte:1250` (the decision head link's `href`) and the count of `.plan/temp/playwright-mcp/55-*-after.png` (32) re-read by the orchestrator at `163e2a49`

Message `rb-55-web-plan-page-001` (revision 0, 2026-11-25T15:53:13Z): `merge_state=merged`, `deliverables_done=4` of 4, 0 unresolved threads, 26 of 26 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`. Binding scope: the spec's `## Re-Grounding 2026-11-25 (before emit, at 459eca7d)` (four deliverables). `landing-check`: `complete: false`, missing `total_tokens` and `plugin_version` (the 2026-10-27 Open Defect, opencode; decision 8).

**Merge corroborated:** `ci pr view 576` reads `merged`, merge commit `163e2a49771267d2d30a9dba8eab509970997bd4`, the first commit on `origin/main` after the ledger land #581 (`7a4e3b67`). Non-ledger diff `3dea04b2..163e2a49` is exactly these 25 files.

**Review numbers re-read** (`runbook_lane review-gate --pr-number 576`, after the merge and the lane's cleanup): 0 unresolved threads, 26 review-body findings, 0 handled (`body_findings_undisposed`). The message gives 26 of 26 and the lane's gate at `88820839` `merge_allowed: true`; the re-read after cleanup does not see the dispositions (the same read-side artefact as PLAN-182 and PLAN-189). The thread count agrees.

**Sonar.** PR: 0 open new issues at `88820839` (`read_open_issues`, `confirmed`, analysis revision = head; gate OK, per the message). Default branch, read by the orchestrator 2026-11-25 after the `main` task `AaITfCslyu25SKZIn4-g` (submitted 16:13:54Z, SUCCESS), later than the merge (15:50:32Z): gate OK (`new_coverage` 90.2 %), `count_status: confirmed`, **3 open** — the three PLAN-181 keys only (`AaFGfsYupdXNQ-N4ArY-`, `AaGRMDtGxhXuOwwhaS0K`, `AaEcTdY0MJNX7b6L9QAy`), none in a file this PR touched. The read names no analysed revision; the task is the only `main` task after the merge.

**SNAPSHOT deploy of `163e2a49`:** `unknown` (the `ci` surface has no commit-scoped run reader).

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 the head on `ObjectPage` | met: `PlanView.test.ts:1776-2098` keyed on new test ids (`plan-kind`, `plan-attention-strip`, `plan-revision-queued`), each fails on a revert; the failing log `pm-web-app/maven-2026-11-25-090824.log` precedes the fix `c6a0314f`, so `test-first` is plausible. The e2e clause (`plan-view.spec.js:39-58,96-123`) is `mutation-after`, stated. "task N of M" (`PlanView.svelte:486`) has **no assertion** (the test checks only `for 2 h 10 min`). The decision link pushes `/decisions/{id}`, not "in place" (Layer 3) |
| D2 Intent tab, 720 px, address, settings | met: `PlanView.test.ts:2339-2453` (three tabs, Intent's three panels, 720/719), `PlanSettingsModal.test.ts:48-216`, `intent.spec.js:45-69` (`mutation-after`). The `file=` drop and restore existed before (`history.ts:291`, `runtime.ts:2178`). "The feed stops when Intent hides the inspector" (`PlanView.svelte:589`) has **no assertion** although claimed |
| D3 activity rows | met: `PlanView.test.ts:2165-2216`; `PlanView.svelte:1026-1031,1695-1702` |
| D4 browser check and documents | **partly**: 32 after screenshots, not the 40 claimed (`running` and `waiting` only en-light and de-dark), 7 before; all taken before the visual commits `f97ced69` and `b9e438e5`, against the "re-taken after any later visual fix" clause. The two design documents are updated; the 390 px watch retirement is not reported. The mock's ended plan is `process.ended-plan` (`IntentScenarioTest` 41→42) |

True count by reading: 3 of 4 (D4 partly). Commit `88820839` adds tests after the fix `b9e438e5` (test-after for those rows); the "+2 settings tests at `88820839`" are in `b9e438e5`.

## Layer 2: implemented

All 25 files trace to a deliverable; outside the Expected Surface but explained: `Link.svelte` (`testid`, `advisory`), `client.ts` (`QueuedRevisionRecord` only), `PlanChanges.svelte` (`wide`), `vocabulary-baseline.json` (shrinks only), `running-work.json`, `IntentScenarioTest.java`, `chrome`/`control`/`epic-view`/`navigation.spec.js`. Unannounced: `plan-view.spec.js` lost its "navigates to epic view … and returns" test (covered by `epic-view.spec.js`, `control.spec.js`). Stale or false claims: the PR body names a `plan55.ended` state (it is `process.ended-plan`), says the gate fails on `IntentCatalogueTest` (CI was green) and that the decisions are read once (re-read on every process refresh, `:266`); the message says `client.ts` gained `getPlanIntent`/`getPlanOutline` (both at base, `client.ts:4303,4313`), that `intent.*` catalogues were touched (none in the diff) and 40 screenshots (32).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. Clean: no `{@html}`/`innerHTML`/`outerHTML`; model text through `stripControlSequences` into text sinks; no timer waits; every new key in `en` and `de`; every activity icon id exists.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | the decision head link pushes `/decisions/{id}` instead of opening the decision in place (`open=`, `routes.ts:628`); not reported | `PlanView.svelte:1250`, `plan-view.spec.js:116` |
| L3-2 | minor | the message's "`OutlinePanel` offers no fragment target" is **refuted**: `OutlinePanel.svelte:151` gives `id=deliverable-N` (at base); `PlanView` passes no `deliverableHref`, so criterion labels stay text | `PlanView.svelte:1467` |
| L3-3 | minor | the decision `$effect` reads `plan`, so `getDecisions` runs on every process reload while the plan waits | `PlanView.svelte:264-272` |
| L3-4 | minor | the Intent tab's first paint shows `intent.plan.none` ("not written yet") before loading starts | `PlanView.svelte:1460-1465` |
| L3-5 | minor | strip, off-track banner, revision line, ended banner, tokens and size line sit in the `tabs` slot; `ObjectPage`'s `attention` slot is unused; `<div slot="state">` inside the template's `<span>` is invalid nesting | `PlanView.svelte:1200,1258-1262`; `ObjectPage.svelte:54,67` |
| L3-6 | minor | `PlanSettingsModal` draws two Close buttons (footer `Button` and the legacy plain `<button data-testid="settings-close-button">`); the conflict buttons are plain `<button>`s (VD-S-06) | `PlanSettingsModal.svelte:165-185,260,274` |
| L3-7 | minor | raw aspect ids as the visible `SelectField` label and raw choice strings as options (pre-existing, survived the rebuild; display-layer constraint) | `PlanSettingsModal.svelte:204,210` |
| L3-8 | minor | `control.settings.use_inherited` is no longer read but still documented; `drop_own`, `changed`, `saved` missing from `work-actions-texts.adoc` (~:141-170) | `control.*.properties:56` |
| L3-9 | minor | "task N of M" untested; `queuedRevisionOf` maps any unknown `applies.key` to `after_task` with an empty `task_id`, untested | `PlanView.svelte:299,457-487` |
| L3-10 | minor | Changes list and hunks use `1fr/1.4fr`, not the design's list at 280–360 px | `PlanChanges.svelte:485` |
| L3-11 | info | `preferCurrent` sends the panel's freshest revision for later fields: a concurrent write between them is overwritten without a conflict | `WorkControlPanel.svelte:635,1129` |
| L3-12 | info | the completion-report link uses the legacy `/reports?plan=` address | `PlanView.svelte:1242`, `routes.ts:61` |
| L3-13 | info | back on Changes, the remembered file shows but the address has no `file=` (pre-existing) | `history.ts:291` |

Dropped by the sub-agent as unconfirmed: a `runningTaskPosition` order differing from the tree; a mid-Save reseed resetting the _Saved_ line.

Reported, not changed (message § 4): the OutlinePanel point is refuted (L3-2); the chat-revision way and `control.chat.revision_origin` belong to **PLAN-136 D4** (`PLAN-136-…md:95-100`), not PLAN-137 — PLAN-136's claim "queued_revision unused in web src" is now stale.

## Residue for the plans that touch the same surface

Shared files with this merge (code / `doc/`), none at the blocking threshold of 5 code files except PLAN-179 (6), which is staged and re-grounded before its emit:

| Plan | Code | doc/ |
|---|---|---|
| PLAN-135 | `PlanView.svelte`, `client.ts`, `vocabulary-baseline.json`, `control.spec.js` | — |
| PLAN-62 | `PlanView.svelte`(+test), `plan-view.spec.js` | `plan-view.adoc`, `plan-view-interaction.adoc` |
| PLAN-190 | `PlanView.svelte`(+test), `plan-view.spec.js` | the same two |
| PLAN-179 | `running-work.json`, `IntentScenarioTest.java`, `client.ts`, `WorkControlPanel.svelte`, `epic-view.spec.js`, `intent.spec.js` | — |
| PLAN-184 | `control.{en,de}.properties` (+3 keys appended) | — (its surface holds `work-actions-texts.adoc`, the L3-8 site) |
| PLAN-194 | `control.{en,de}.properties`, `running-work.json` | — |
| PLAN-85 | `chrome.spec.js` | — |
| PLAN-136 | `PlanView.svelte` (the head's queued-revision line, D4) | — |
| PLAN-71, PLAN-195 | — | — |

PLAN-62, 190, 179, 136 and 135 waited for this landing: each is re-read against `163e2a49` before its emit.

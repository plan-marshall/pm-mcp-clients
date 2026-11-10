# PLAN-153: Web Views: System and Cleanup on the D1 Vocabulary (Views Package, Part 1)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents and `shared/system.adoc`, `shared/cleanup.adoc`.
> SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

`SystemView` (1881 lines) and `CleanupView` (917 lines) were built before the D1 vocabulary: hand-written toggle buttons, a `↻` glyph button, a `<select>`, coloured `badge` and `status-tag` spans, hand-drawn tables and hand-drawn modals
on their own layer. This plan moves both onto the D1 components, the hierarchy and the labels of `components.adoc`, with no change to what they show or do. The column engine, the addresses and the heading outline are D2's.

## Deliverables

Read first: `doc/design/web-app/components.adoc` (§ `#rules`, `#hierarchy`, `#labels`, `#segmented-control`, `#data-table`, `#dialog`, `#confirm-dialog`), `components-tags.adoc`, `components-fields.adoc` (§ `#choice`),
`shared/system.adoc` (§ `#view`, `#stop`, `#restart`, `#upgrade-confirm`), `shared/cleanup.adoc` (§ `#job`, `#commands`), `shared/system-texts.adoc`, `shared/cleanup-texts.adoc`.

Not in this plan: the column engine, `?open=` addresses and the heading outline of the object column (D2); the deferred-reload state of `SystemView` (PLAN-152 touches those lines); the `Back` of a page (D2 owns up and the breadcrumb).

1. **System: head, figures and tags**
 - What:
   - The window selector (hour, today, 7 days; `SystemView.svelte:530-550`) and the per-model/per-role and all/per-project choices become `SegmentedControl`; the project `<select>` (`:515`) becomes `SelectField`; the `↻` glyph button (`:556`) becomes an `IconButton`
     with `act-refresh` and the label `system.view.refresh`.
   - Every `badge`, `badge--warning`, `badge--danger` and `status-tag` span becomes a tag of the closed family: a worker's busy or idle and a state become `StatusLabel`; what needs the operator becomes `ReasonBadge`; counts in a group head become `CountBadge`; the rest is detail text.
     The plan or epic a worker works for is a `Link`.
   - The hand-written `<table class="data-table">` blocks (`:994`, `:1101`) become `DataTable` where the figures are tabular (workers, models); a part whose lines do not fit its column spec becomes a `KeyValueList`. A figure not available keeps `not available` with its reason.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `SystemView.test.ts` and `pm-web-e2e/tests/system.spec.js` on `reports-and-system` show the same figures and the same selections as before (window, per model or role, all or per project survive a reload via the stored choice), through the new components. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for `SystemView.svelte` and the tag-class scan `git grep -nE 'class="[^"]*(badge|status-tag)' -- pm-web-app/src/lib/views/SystemView.svelte` is empty (evidence order: `none`).
   - The text guard baseline is unchanged. Evidence order: `none`.
 - Implements: `components.adoc` § `#segmented-control`, `#data-table`, `#labels`; `components-tags.adoc` § `#tag-set`; `shared/system.adoc` § `#view`.
2. **System: the confirmations on the dialogs**
 - What:
   - The hand-drawn modals (`:1181`, `:1293`) with their `<input>` choices become `Dialog` or `ConfirmDialog`. The choice of when ("When what runs is done", "Now") is a `RadioGroup` with the drain preselected; the primary button names the choice
     (`Stop when done`, `Stop now`, `Restart when done`, `Upgrade when done`); the cancel act is ghost on the left. A stop, restart or upgrade is an entry of the `ActionBar` overflow in the danger text colour with `…`; the confirming button of a destructive act is the one filled danger control.
   - The upgrade consent (`check_updates`) keeps its three fields; the consent box stays `ConsentBox`.
   - The state machine of the phases (idle, consent, progress, withdrawn, failed, reload) is not touched.
   - The layer literal at `:1811` goes; the dialog layer comes from `Dialog`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `SystemView.test.ts` and `system.spec.js`: stop when done and stop now, restart, upgrade and its withdraw run through the new dialogs, `Escape` closes and returns the focus, the drain is preselected, a second confirm is ignored while it runs. Evidence order: `test-first`.
   - `git grep -n "z-index\|role=\"dialog\"" -- pm-web-app/src/lib/views/SystemView.svelte` is empty, and `SystemView` leaves the allowlist of the layer scan. Evidence order: `none`.
 - Implements: `components.adoc` § `#dialog`, `#confirm-dialog`, `#hierarchy`; `shared/system.adoc` § `#stop`, `#restart`, `#upgrade-confirm`.
3. **Cleanup on the vocabulary**
 - What:
   - The job's items are a `CheckGroup` per project group (the checks stay the client's until _Delete_ sends them); _Check all_ and _None_ are secondary `Button`s that act on every item of the job; the item badges (`badge--kind`, `freed-badge`) become detail text
     (item kinds such as logs and read reports are not kinds of the closed set); a plan or epic row carries its `KindMark`, and an outcome `StatusLabel`.
   - Commands by the hierarchy: _Delete…_ the one primary, disabled with the reason `Nothing is checked; Ignore keeps everything listed.` (as `disabledReason`); _Ask me again in N days_ and _Ignore_ secondary. The confirmation (`cleanup.confirm`, the hand-drawn modal at `:559`)
     is a `ConfirmDialog` with the counts, the space, what stays and the number kept for good; its confirming button is the one filled danger control.
   - The back button (`:268`) becomes a `Link` to `/system` (navigation is a link); no Back of its own is added (D2). The layer literal at `:874` goes.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `CleanupView.test.ts` shows: all checked on open; _Check all_ and _None_ on every item; _Delete_ disabled with its reason at none checked; the confirmation names the counts; a second confirm is ignored while deleting. Fails on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for `CleanupView.svelte`; no `z-index` or `role="dialog"` remains in it; it leaves the layer allowlist. Evidence order: `none`.
 - Implements: `components.adoc` § `#hierarchy`, `#confirm-dialog`; `components-fields.adoc` § `#choice`; `shared/cleanup.adoc` § `#job`, `#commands`.
4. **Browser evidence**
 - What: both views checked in a browser, desktop and phone, both locales and themes, including the stop, restart and upgrade dialogs and the cleanup confirmation.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) on `reports-and-system` at 1440×900 and 390×844, `de` and `en`, both themes; baseline and after screenshots recorded; the contrast audit `theme-readability.spec.js` passes for both views (evidence order: `none`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a failed read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text (the names in the system view are untrusted text, shown as text only), no timer waits in tests, the npm scripts stay.
- A pure restyle: no change to what a view shows, sends or stores; where a rule of the design cannot be met without a behaviour change, record it as a finding in the pull request and leave the behaviour.
- Every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- Ports of this plan: mock web port 21530, app port 21531 (`20000 + 10 × 153`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The pull request is merged only on the operator's word. Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `SystemView.svelte` draws three `btn-toggle` buttons for the window (lines 530-550), a `↻` button (556-562), a project `<select>` (515), `badge`/`status-tag` spans (885, 900, 929, 984, 1033, 1149-1156), two `<table class="data-table">` (994, 1101), two hand-drawn dialogs (1181, 1293) with `<input>` elements (1237, 1253, 1316, 1332) and `z-index: 100` (1811) — read at the file
- OBSERVED: `CleanupView.svelte` has a `back-link` button (268), `badge`, `badge--kind` and `freed-badge` spans (366, 385, 451, 548), checkboxes (443, 532), a hand-drawn modal (559) and `z-index: 100` (874) — read at the file
- OBSERVED: `SystemView` uses `ActionBar`, `Button`, `Icon`, `Spinner` and sits on a page template; `CleanupView` uses `Button` and `Spinner` and sits on none — read at the imports and by the PLAN-45/batch-1 report (templates)
- OBSERVED: `SystemView.svelte` carries the deferred-reload state (`reloadWaitsForInput`, lines 52, 211-237), which PLAN-152 touches — read at the file
- OBSERVED: `SystemView.test.ts`, `CleanupView.test.ts` and `pm-web-e2e/tests/system.spec.js` exist; there is no `cleanup` e2e spec — read in the directories
- HYPOTHESIS: `DataTable`'s column spec (`id`, `header`, `width`, `numeric`) carries the unit and the split columns (in, out) of the models table — confirm/refute at `DataTable.svelte` § props and `SystemView.svelte:1100-1150` (verify-at-outline)
- HYPOTHESIS: `SystemView` sits on `DashboardPage` or `TriagePage`; its page cap needs no change — confirm/refute at `SystemView.svelte` § template wrapper (verify-at-outline)
- Verify-first clause: re-read both views after PLAN-45, PLAN-149 to PLAN-152 landed; re-run `lint:vocabulary` for both and take its finding list as the work list of deliverables 1 to 3.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/SystemView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/SystemView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CleanupView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/system.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-web-app/scripts/vocabulary-baseline.json` — absent at HEAD 1cc0e10e; created by PLAN-151 (its counts for these two files shrink here)
- OBSERVED: `pm-web-app/src/lib/vocabularyLint.test.ts` — absent at HEAD 1cc0e10e; created by PLAN-151
- OBSERVED: `pm-api/src/main/resources/messages/system.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/system.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/system.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/cleanup.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed (it holds the `views/` directory); `PLAN-149` (`SegmentedControl`, `Dialog` layer, `Button`), `PLAN-150` (`RadioGroup`, `CheckGroup`, `SelectField`), `PLAN-151` (the count to bring to zero), `PLAN-152` (the reload lines of `SystemView`).
- Overlaps with:
  - `PLAN-45` (running): `views/`.
  - `PLAN-152`: `SystemView.svelte` reload lines (PLAN-152 first).
  - `PLAN-60` (parked): the machine settings; `SettingsView`, not these two views.
  - `PLAN-65`, `PLAN-29`, `PLAN-41` (parked verification plans): they check these views afterwards.
- Adjacent to: `ActionBar` and `ConsentBox` (used, not changed); the terminal system view; D2 (the cleanup job as a column beside the system view, `?open=plan:{id}`).
- Unblocks: the gate "views package implemented" of PLAN-29, 41 and 65.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since (PLAN-45, PLAN-149 to PLAN-152). Settle each HYPOTHESIS. Re-run the lint for the two views.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-153-web-views-system-and-cleanup.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

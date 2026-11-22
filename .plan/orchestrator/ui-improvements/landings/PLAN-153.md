# Landing Analysis: PLAN-153 — Web Views: System and Cleanup on the D1 Vocabulary (Views Package, Part 1)

epic: ui-improvements
workstream: WS-03
pr: #532 (merged as 9abb2958, `feat(web-app): put the system and cleanup views on the vocabulary (PLAN-153)`; 14 files, +1055/-1044 against the first parent)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-153-web-views-system-and-cleanup-001.md`, runbook lane, harness `opencode`, model `deepseek-v4.1-flash/high`, effort `high`, executor `0.1.2183`; analysed 2026-11-22). `landing-check` is **incomplete**: `total_tokens` and `plugin_version` are `unknown`. The message was read as a lead; the PR, the merge commit, the review gate and Sonar were re-read, and the lane's progress file `.plan/temp/rb-153-web-views-system-and-cleanup/progress.md` was read as the runbook lane's evidence set.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 System: head, figures and tags | shipped-modified: SegmentedControl, SelectField, IconButton, DataTable, StatusLabel, CountBadge are in; four spec premises are NOT met and only three are recorded by the lane: a worker's busy or idle is a text cell (DataTable cells are `Record<string,string>`), the worker's plan is text (`WorkerMetric` has no id), the head sentences became the accessible name of a CountBadge, and **what needs the operator is not a `ReasonBadge`** (no `ReasonBadge` anywhere in `SystemView.svelte`; "waiting for you" is a `StatusLabel` only) — the last one is not in the lane's list of recorded deviations | `DataTable.svelte:16-22`; `SystemView.svelte` (grep: `CountBadge` at `:1085,1114,1159,1185`, no `ReasonBadge`) |
| D2 System: the confirmations on the dialogs | shipped-modified: stop, restart and the upgrade consent run through `ConfirmDialog` and `Dialog` with the drain preselected, a second confirm ignored, the lifecycle and reload state machines unchanged; **but "Escape closes and returns the focus" is not met for the upgrade consent** (L3-1) | `SystemView.svelte:304-312,335`; `SystemView.test.ts:368,388` |
| D3 Cleanup on the vocabulary | shipped-modified, recorded: a `CheckField` per item instead of a `CheckGroup` (the per-item Open act has no place in a group), no outcome `StatusLabel` (`CleanupItemEntry` has no such field); the delete request, the confirmation counts and the second-confirm guard are unchanged in behaviour | `CleanupView.svelte:180-188`; `client.ts:1309-1316` |
| D4 Browser evidence | not shown by reading (24 screenshots under `.plan/temp/playwright-mcp/pm153-*.png`; `theme-readability.spec.js` "system and cleanup" is unchanged and walks only the views at rest, so no automated contrast check covers an open dialog or the open act menu) | progress file; `theme-readability.spec.js:352-372` (leaf) |
| D5 The lint's and the event sets' tests | shipped-as-specified: `--write-baseline` keeps the baseline's key order and returns before writing when the content is equal; the unparseable fixture throws with the parser's message; the refusal tests assert file and both counts; the header comment states the printed format; the three refresh sets are pinned as literal inventories (`toEqual`, order-sensitive); the mutation runs are the lane's account | `vocabulary-lint.mjs:341,478-482`; `vocabulary.test.ts:59`; `refreshEvents.test.ts:116` (leaf) |
| added-unplanned | `DataTable.svelte` (+13: a keyboard stop on the scroll region, axe `scrollable-region-focusable`), `byState.ts` (+10: stop, restart and upgrade marked destructive in `RUNTIME_ACTS`), `doc/plans/ui-rule-catalogue.adoc` (one sentence): all three named in the PR body; no consumer outside `SystemView` and the layout reads the runtime roles (`byState.ts:517-543`, leaf) | `git diff --stat 9abb2958^1 9abb2958` |

**"5 of 5 met" is the lane's count; by the spec's own rule (every Done-when clause met) D2 is not met** (the focus clause for the upgrade consent) and D1 and D3 carry recorded design deviations; the landing's `deliverables_done=5` overstates by that reading.

## Three-Layer Analysis

The landing was read on three layers by one read-only leaf, and the orchestrator re-read the points recorded as `checked` below at `9abb2958`. By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`, plan-less) — reason: every full ship gets its own leaf; two views of 1940 and 937 lines rewritten with three dialog flows is the reading depth the dispatch rule exists for.
- **Verdict:** layer 1 findings (5, minor); layer 2 findings (partial: 6 minor) and not shown by reading (3); layer 3 findings (1 major, 4 minor) and not shown by reading (3). No blocker.
- **Re-read by the orchestrator** (`file:line` at `9abb2958`): `SystemView.svelte:282-292,304-312,404-424,752,950-960`; `Dialog.svelte:50-62`; `CleanupView.svelte:180-188,358-362`; `DataTable.svelte:16-22,64-70`; the `CountBadge`/`ReasonBadge` grep of `SystemView.svelte`; the pre-state line counts (1940 and 937); the diff stat; `Tabs.svelte` absent from the diff. The leaf's remaining points (the confirm payloads, storage keys, the deferred-reload identity, the byState consumers, D5 internals, text sinks, `CleanupView.svelte:204,210,222`) are **unverified leads** of the orchestrator.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 (minor) | The size premise (1881 and 917 lines) is stale: 1940 and 937 at the base | checked | refutes | `git show 9abb2958^1:…` line counts (re-read) |
| L1-2 (minor) | D1's premises contradict each other: a worker's busy or idle is a `StatusLabel` AND the workers are a `DataTable` whose cells are text sinks | checked | confirms (the lane's deviation is genuine) | `DataTable.svelte:16-22` (re-read) |
| L1-3 (minor) | D3's premises contradict each other: a `CheckGroup` per project AND the per-item Open act stays (`CheckOption` has no place for an act); `CleanupItemEntry` has no outcome field, `WorkerMetric` has no id | checked | confirms | `CheckGroup.svelte:6`, `client.ts:1258,1309-1316` (unverified lead) |
| L1-4 (minor) | D1's "what needs the operator becomes a `ReasonBadge`; group-head counts become `CountBadge`" was dropped without a record: no `ReasonBadge`, no attention `CountBadge` | checked | refutes | grep of `SystemView.svelte` (re-read) |
| L1-5 (info) | D4's browser evidence cannot be established by reading | not_shown_by_reading | none | — |

No corpus claim of a staged spec is named by a finding (the spec is landed), so no verdict is persisted; the spec's claim 2 (`contradicted`, imports) and hypothesis 1 (`contradicted`, DataTable column spec) stand.

**Layer 1 verdict:** findings — 4, not shown by reading — 1.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 | `SystemView.svelte` | `SystemView.test.ts`, `system.spec.js:164-175` (radio checked after a reload); stored keys unchanged (leaf) | the ReasonBadge omission (L1-4) | findings |
| D2 | `SystemView.svelte:304-312` (`const when = modalWhen`), `:335` unchanged | `SystemView.test.ts:388` (second confirm; fails if `busy={isExecutingAction}` is reverted, leaf); **the `modalWhen` fix has no test that fails on revert**; Escape and focus tested only for the stop dialog (`SystemView.test.ts:368`), not for the upgrade consent | no test pins the new runtime destructive roles (`byState.test.ts` untouched) | findings (minor) |
| D3 | `CleanupView.svelte:180-188` (`executeCleanup(job.job_id, Object.keys(uncheckedIds))`, unchanged) | `CleanupView.test.ts` (all checked on open, Check all, None, Delete disabled with its reason, a second confirm); **the back `Link` to `/system` is asserted by no test** (whether anchor navigation hands the route over as `navigateTo('system')` did is not shown by reading) | **the lane's "German `items` literal fixed" is half true**: only the Delete label uses the catalogue phrases (`deleteWhat`, `:360`); the success notice still passes `` `${checkedItems.length} items` `` as `{what}` (`:184`, re-read), so the German view reads "3 items gelöscht"; and `checkedItems` is read after the `await`, the same pattern as the `modalWhen` fix | findings (minor) |
| D4 | — | `theme-readability.spec.js` | not shown by reading | not_shown_by_reading |
| D5 | `vocabulary-lint.mjs`, `vocabulary.test.ts`, `vocabularyLint.test.ts`, `refreshEvents.test.ts` | non-vacuous by reading; the mutation runs are the lane's account | — | checked (leaf) |
| visible changes in a "pure restyle" | the Delete label went from "Delete N items" to "Delete 1 plan · 1 unenrolled project…" (catalogue phrases; `system.spec.js:131` regexes changed with it); Check all and None went from per project to job-wide (D3); the head sentences ("3 busy · 2 idle") are visible only as a bare number, the split is screen-reader text | | the PR body lists only the `items` literal as "pre-existing, not introduced" while the button was changed: the two narratives contradict each other | findings (minor) |

**Layer 2 verdict:** findings — 6 (minor), not shown by reading — 3 (D4; the back-link route hand-over; whether the strengthened assertions fail on mutation).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `SystemView.svelte:407-418,950-960,752`; `Dialog.svelte:50-62` (all re-read) | open Upgrade… from the overflow menu or the "update available" panel Button, then press Escape or choose "Not now" | `openUpgradeConsent` sets `upgradePhase = 'consent'` (`:288`), `runtimeState` becomes `upgrading` (`:407-414`), so `showRuntimeActs` is false and the ActionBar unmounts (`:953`); the "update available" panel needs `upgradePhase === 'idle'` (`:752`) and unmounts too. `Dialog.takeFocus` recorded `document.activeElement` (`:52`) and focuses it at destroy; a removed element takes no focus, so **focus drops to the body**. The spec's Done-when "Escape closes and returns the focus" holds only for stop and restart | major |
| L3-2 | `DataTable.svelte:64-70` (re-read) | any `DataTable` | `tabindex="0"` sits on a bare `div` with no role and no name (the `svelte-ignore a11y_no_noninteractive_tabindex` line suppresses the warning): the scroll region is announced as nothing and every table gets the extra tab stop, scrollable or not; `role="region"` with `aria-label` is the usual form; no unit test pins the tabindex or the focus ring; consumers are `SystemView` and `routes/dev/components` only | minor |
| L3-3 | `SystemView.svelte:1085,1114,1159,1185` (re-read for the badge sites) | a sighted operator on the Work card | `CountBadge` is used in metric rows (the doc says rail, tabs and group heads, never a row) and the workers' and jobs' head sentences keep only the bare number visibly: the "3 busy · 2 idle" split and "locks N held" are screen-reader text; the lane recorded the head-sentence change but it counts against "no change to what it shows" | minor |
| L3-4 | `CleanupView.svelte:184` (re-read) | German locale, delete succeeds | the success notice reads "N items gelöscht" (English literal in the German view) | minor |
| L3-5 | `SystemView.svelte:~690` (unverified lead) | a model entry with the wire id empty or two entries with one id | `DataTable` keys rows by `row.id`; the models rows use `model.id` (the old `each` was unkeyed) and `modelIdOf` returns `''` when the wire carries no id: duplicate or empty keys throw `each_key_duplicate` in dev and mis-render in production; whether the runtime sends such rows is not shown by reading | minor (not shown) |
| L3-6 (not shown, unverified lead) | `SystemView.svelte:165` | arrow-key stepping through the window radios | `loadSnapshot` has no sequence guard and `SegmentedControl` selects on every arrow key, so overlapping requests can leave an older window's snapshot labelled with the newer one (timing-dependent) | minor (not shown) |

**Layer 3 not shown by reading:** the forced-colors case for the box-shadow focus ring of `DataTable` (`DataTable.svelte:149`); the visibility of a failure notice behind the modal overlay (the same flow as the old hand-drawn modals); L3-5 and L3-6 above. Pre-existing and untouched: the English literals "in 3 days" and "tomorrow 03:00", the untracked `setTimeout(navigateTo('system'), 1500)` at `CleanupView.svelte:210,222`, no in-flight guard on Remind and Ignore (leaf, unverified lead).

**Layer 3 verdict:** findings — 5 (1 major, 4 minor), not shown by reading — 3. The confirm payloads, the delete request body, the stored-choice keys and values, the deferred-reload state and the upgrade phase machine are identical to the old behaviour (leaf, unverified leads); text sinks are clean (`releaseNotesHref` through `ExternalLink` refuses non-http(s), stricter than the old raw `href`); no `@html` in either view.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Dependencies | no dependency or plugin without asking | conforms | `package.json`, lock file and POMs are not in the 14-file diff |
| CLAUDE.md § Web Standards | no `innerHTML`/`{@html}` with untrusted text; no timer wait in a test; flat ESLint; ES modules | conforms | leaf grep of both views and the new tests; `system.spec.js` waits on `expect` conditions |
| spec | every new text from the catalogue in en and de; the text guard baseline does not grow | conforms | one new key (`system.cleanup.action.nothing_checked`) exists in both locales; `text-guard-baseline.json` not in the diff |
| spec | no `z-index` literal and no `role="dialog"` in the two views; they leave the allowlist | conforms | grep empty; `tokens.test.ts` lines removed |
| spec D1 | a worker's busy or idle is a `StatusLabel`; the plan a worker works for is a `Link`; what needs the operator is a `ReasonBadge` | deviates | text cell, text, and no `ReasonBadge` (the last unrecorded) |
| spec D2 | Escape closes and returns the focus | deviates | not for the upgrade consent (L3-1) |
| spec D3 | a `CheckGroup` per project; an outcome `StatusLabel` | deviates | `CheckField`, no outcome label (both recorded and substantiated) |
| CLAUDE.md | zero Sonar findings; `failOnWarning`; full verify | not_shown_by_reading | no run |

## Metrics and Anomalies

- Tokens: `unknown` (harness `opencode` measured none); `plugin_version` `unknown`; executor `0.1.2183` (skew `match` at session start per the message). Duration: not recorded by the message.
- Anomalies: two operator questions were asked by the lane for the detached base tree's executor (`worktree-create --detach` answered `executor_in_worktree: absent` twice; the operator selected a hand-run `generate_executor generate`); the lane records its own rule breaks (`ls`, `grep`, `tail`, `wc` on logs and `cp`/`rm`/`mkdir` under `.plan/temp`, a backtick pair executed by the shell in a commit message, amended) with `guard: none on this harness (unenforced)`, so nothing is reported as "all kept"; one `pm-api` randomised-input flake and one `pm-mock-runtime` timing flake, both green on the rerun; Sourcery refused on quota (optional bot, no gate).

## Routing and Merge Behavior

- Review: CodeRabbit (required) reviewed head `6aae0716` with four findings, all fixed in `85195bc1` (the German `items` literal, `modalWhen` read after an `await`, the missing `Finding.message`, `destructive` not forwarded to the act menu); three threads answered and resolved, the outside-diff one disposed. `runbook_lane review-gate --pr-number 532` re-read on 2026-11-22: `unresolved_review_threads=0` (the landing says 0, equal), `review_body_findings_total=4` (the landing says 4, equal), `handled=0` from the bare call (`merge_allowed=false, body_findings_undisposed`): the bare call cannot see the run's dispositions, so the difference is the known artefact; `review_body_findings_handled` is re-verified at **4 of 4** from the progress file's four `finding:` lines.
- CI/merge: merge commit `9abb2958` (`ci pr view`: merged, base `main`); the landing's last `verify` head is `f9b14c1d` and Sonar's `analysis_revision` for the PR is `f9b14c1d`.
- Sonar: the landing states `sonar_open_new_issues=0` and `step.sonar-roundtrip.quality_gate_status=ok`. Re-read now: `sonar read_open_issues --pr 532` is `undecidable` (`head_revision_unavailable`, a merged PR), `sonar_rest gate-status` for the PR is `OK`: the gate verdict is re-verified, the open count of a merged PR cannot be re-measured; nothing contradicts the lane's `0`.
- Overlap observed: PLAN-153 collides with no running plan; the corpus names 5 files shared with the staged PLAN-144 (`CleanupView.svelte`, its test, `system.*.properties`, `system.spec.js`), now merged here, which PLAN-144 must re-read; and one file (`theme-readability.spec.js`) with PLAN-154 and PLAN-156.

## Reconciliation Actions

- [ ] row `status` → `shipped` — `orchestrator queue --transition PLAN-153 --status shipped`
- [ ] row `pr` stamped `532`
- [ ] row `landing` stamped `landings/PLAN-153.md`
- [ ] row `plan_marshall_plan_id` stamped `rb-153-web-views-system-and-cleanup` (the sender's `{plan}`)
- [ ] epic.md: Open Defect for the incomplete landing and the layer-3 findings (PLAN-153 entry); the PLAN-153 mention in the PLAN-173 `DECIDED 2026-11-20` entry (deliverable 5 folded in) is satisfied by the landing
- [ ] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Layer-3 major L3-1 is reported to the operator at once and is not fixed inline;** a fix plan is staged only on the operator's word (new plan, or a fold into a staged web plan; PLAN-144 and PLAN-156 touch the dialogs and the system views by surface). The minors (L3-2 to L3-6, the success-notice literal, the missing tests for the `modalWhen` fix, the back link and the runtime destructive roles, the unrecorded `ReasonBadge` omission) ride the same decision.
- The four recorded design deviations stay for D2 or a later view plan (the lane's Residue); the `ReasonBadge` omission is added to them here.
- PLAN-144 (staged) must re-read `CleanupView.svelte`, `system.*.properties` and `system.spec.js` at `9abb2958`: its spec predates this landing.

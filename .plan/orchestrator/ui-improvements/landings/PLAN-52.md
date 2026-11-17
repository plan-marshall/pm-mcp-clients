# Landing Analysis: PLAN-52 — Web: Running Work Across Projects

epic: ui-improvements
workstream: WS-03
pr: #459 (https://github.com/plan-marshall/pm-mcp-clients/pull/459)

> Landing record, written by `analyze` on 2026-11-17 from the inbox message
> `rb-52-web-running-work-001.md` (a lead, not a fact), git, the provider, and a read-only three-layer leaf.
> The orchestrator re-read the points marked "re-read" on `d79cc5f1`.

Merged as `d79cc5f13f0a43b87c819c1f7dd3a25120157a59` (2026-11-17T11:38:23Z), 13 files, +1114/-383. Sender `rb-52-web-running-work`. Lane: runbook. Harness `opencode`, model `deepseek-v4.1-flash/high` (as stated, not corroborated). The lane reports `deliverables_done=2` of 3 and `self-review: clear`.

## Deliverable Fidelity vs Spec

The spec's original three deliverables were amended four times (re-grounding 2026-11-10, operator decisions, D6 merge, PLAN-148 residue). The effective Done-when list is the amended one.

| Deliverable (spec, amended) | Verdict | Evidence |
|-----------------------------|---------|----------|
| D1 — rows and groups | partly shipped | the explicit Done-whens are met: columns align at 1440 px, the row menu is an icon button (`OverviewView.svelte:1175-1185`, grid `:1562-1577`, `overview.spec.js:213-254`); the address modes `group=epic`, `mode=ended`, `?project=` (`:219-279,448-571`). **Not delivered, not reported by the lane:** the amended clause "each row is a `RowLink` in the slots of every row, with its acts in a `RowMenu`" (re-read: `OverviewView.svelte` has no `RowLink`, `RowMenu`, `pinned` or `follow` hit; rows are a `div` with `on:click`, `:1060-1070`); the follow mode (`lensFollow` exists only in `runtime.ts`) |
| D2 — one list, one count | shipped | one `filteredPlans` list; `getEpicRunningCount(epic, filteredPlans)` `:1255-1260`; `OverviewView.test.ts:327-383` is set-equal plus count and non-vacuous (the fixture's `running_plans_count: 5` against `2 running · 3 staged`) |
| D3 — ended work and alongside | partly shipped | delivered: paging (`:573-595`, `client.ts:3671-3676`), outcome/PR/reason/end-time columns, an ended plan opens read-only (`:487-500`, `runtime.ts:1638-1641`), the rail item opens running work after ended work (`runtime.ts:588-602`), the two localStorage keys gone (non-vacuous spy, `OverviewView.test.ts:591`). **Not delivered:** the pinned row for a selection the list does not hold (`web.nav.pinned.*` is used only in `TriagePage.svelte:519`; `refreshAll` jumps silently to the first project, `:422-423`, and `OverviewView.test.ts:452-477` codifies the jump); the `alongside` running onboarding or setup run (contract-blocked: `AlongsideLanding` carries `unit, state, kind, holdKind` only, re-read). The lane's own "aligned ended-work columns not met as written" is confirmed |

**Files the diff touches that the Expected Surface does not list:** `history.ts` and `history.test.ts`, `BottomBar.svelte`, `PhaseLabel.test.ts`, `components-tags.adoc`, `epic-view.spec.js`, `theme-readability.spec.js` (the lane names `history.ts`, `runtime.ts`).

`text-guard-baseline.json` is not in the diff: no finding grandfathered, the baseline did not grow.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf — reason: the Dispatch Decision Rule; 13 files and an 892-line view rewrite. The leaf did not open the browser-check artifacts or any failing-run log: the `test-first` evidence and the 390 px checks are the lane's claim here.
- **Verdict:** layer 1 major (spec clauses absent); layer 2 one contradicted claim; layer 3: no blocker, **2 major (conformance), 1 conditional**, 9 minor, 2 leads.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | each row is a `RowLink` with a `RowMenu` (re-grounding 1; "Unparked" waits for PLAN-149's `RowMenu` `disabledReason`) | checked | refutes | re-read: no `RowLink`/`RowMenu` in `OverviewView.svelte`; an `IconButton` labelled `control.actions.open` opens a side panel (`:1175`) |
| L1-2 | a selection the list does not hold is a pinned row | checked | refutes | see D3 |
| L1-3 | the follow mode is a mode of the address | checked | refutes | `OverviewView` never reads `lensFollow` |
| L1-4 | `alongside` shows a running onboarding or setup run | checked | refutes | the contract has none (`AlongsideLanding.java`, re-read); the clause needs a contract change first |
| L1-5 | PLAN-148 residue: an unknown role shown with the catalogue's unknown word | checked | confirms | `PhaseLabel.svelte:92-93` (`state.unknown`), `PhaseLabel.test.ts:86-98` en and de; the role set was not settled with the runtime docs and an unknown phase still draws nothing |
| L1-6 | PLAN-148 residue: list the consumers of `RowLink` and `StatusLabel` | checked | refutes | nothing in the diff or the PR body |
| L1-7 | VD-S-06: no plain text button, no state pill | checked | refutes | re-read: `<button>` at `OverviewView.svelte:770,889,932,1019,1076,1233`; `badge badge--outcome`, `badge--state`, `badge--warning` (`:899-905,1125,1333`) |
| L1-8 | no client sort; the served order | checked | confirms | no `.sort` in the view; `applyFilter` keeps the order (`:194-203`) |
| L1-9 | the design document is updated in the same PR | checked | partly | only `components-tags.adoc:262` changed; no document records the aligned row grid or the icon row menu |
| L1-10 | the HYPOTHESIS "the runtime pages ended work" | checked | confirms | `client.ts:3671-3676` `getEndedWork(projectId, page)` |

### Layer 2 — Implemented

| Item | Verdict | Evidence |
|------|---------|----------|
| the BottomBar "pending-navigation" change in the PR body and the lane's findings | **contradicted** | re-read: the net diff of `BottomBar.svelte` is the de-indentation of three imports to column 0 (`:3-5`); `openRunningWork` was already imported and called. The behaviour is in `runtime.ts` (`noteNavigationAdopted()` `:1188-1190`, called `:1449`; guards `:532,:597`). The edit is a formatting regression (`format:check` is not in `pm-web-app`'s `quality`) |
| the new `runtime.test.ts` cases | non-vacuous | both fail without the guard: the place holds `p-1` and an empty project list would push `/projects` or a project address |
| `adoptRouteFromLocation` bumps the navigation epoch | confirmed | re-read: `runtime.ts:1446-1449`. This **closes PLAN-49's L3-2** (the Back race), which the ledger recorded as open |
| the e2e alignment assertion | cannot fail | re-read: `overview.spec.js:154-155` compare `th` and `td` x and width of a six-cell fixed table, true by construction; only `tableLayout`, `borderCollapse` and `paddingLeft` are real |
| the reload-of-unfilled-`/overview` test | seeds the mark by hand | `overview.spec.js:256-286` uses `pushState({pmApp:true})`: it does not prove the product writes the mark; `history.test.ts` "the mark of a load" does |
| the ended-reload comment "on the page the operator left" | vacuous | `overview.spec.js:202-205` leaves from page 1 and asserts page 1 |
| `deliverables_done=2` and the report's D1 heading "not fully met (every Done-when met)" | internally inconsistent | the lane's table says D1 met on every clause; its residue lists D3 only |
| lane claims otherwise (epic head `{running} running · {staged} staged`, unique-title rule, row and title open the plan, `history.ts` docstring) | borne out | `:1255-1260`, `:719-721`, `:1065-1069,1079-1082`, `history.ts:16-19` |

Not shown by reading: the failing-run logs, the browser-check artifacts, the 390 px layout, the runtime's `activity.changed` event rate.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `OverviewView.svelte:422-423` (re-read: no pinned-row code), `OverviewView.test.ts:452-477` | the selected project's work ends | `refreshAll` silently switches to `projects[0]` instead of a pinned ended row; the test pins the jump | major (spec conformance) |
| L3-2 | `OverviewView.svelte:889-896,932-939,1019,1076,1233` and the badges `:899-905,1125,1333` | any ended or alongside row | VD-S-06 unmet on a touched view: plain text buttons, an underlined text button for the report, state pills; no gate flags them | major (standing constraint) |
| L3-3 | `OverviewView.svelte:390-409,1042-1045` (re-read: `isLoadingDetail = true` on every call, the list replaced by a skeleton) | every background `refreshAll` on `activity.changed`, `phase.changed` (`:331-343`) | the focused row is destroyed and the list flashes; the design says a row never moves under the pointer | major if the runtime emits these often, else minor (event rate not read; pre-existing) |
| L3-4 | `OverviewView.svelte:135-145` against `:115-127`, `:1289-1293` (re-read) | filter text "execute" by epic | `epicFilterFields` omits the child plans' phase, state and off-track, plans match on them: a plan that belongs to an epic is listed under "without epic" | minor |
| L3-5 | `OverviewView.svelte:1203-1206,1216-1218,1055-1056` | a project with no plans or epics and no filter | the empty state reads "Nothing matches the filter."; `none_active` is reached only when `detail` is null | minor |
| L3-6 | `OverviewView.svelte:805-808,764-766` | the projects read fails, or no projects | `ErrorState` in the sidebar, "Nothing runs" in the main panel; with no projects the same sentence twice | minor |
| L3-7 | `runtime.ts:1304,1238-1278`, `SystemView.svelte:192` (lead, not executed) | open Ended work, go to System, click a figure | `addressOfState` emits `mode` from the store for any route and `applyRoute` does not clear `lensMode`: the address becomes `?mode=ended`, against D6 "never the mode" | minor (lead) |
| L3-8 | `OverviewView.svelte:294` (lead) | `/overview?project=A`, a by-plan address | the remembered place's group applies when the address has a query but no `group`: a by-plan link renders by epic | minor (lead) |
| L3-9 | `runtime.ts:553`, `OverviewView.svelte:308,318-324` (lead) | an in-app Up | `openLensList` leaves `selectedProjectId` in the store, so Up selects the stale project while a reload of the same unfilled address selects the first | minor (lead) |
| L3-10 | `OverviewView.svelte` ended paging | leave ended work and return | the page number is not in the address and no key holds it: always page 1 (the spec's D6 removes the key; no design line says where the page lives) | minor |
| L3-11 | `runtime.ts` in-view address writes (`selectProject`, `setViewMode`, `openEndedWork`) | a click on Running work, then a project click before the summary answers | these writes do not move the epoch: the late push overrides the click | minor (narrow window) |
| L3-12 | `OverviewView.svelte:1166-1169`, `:1092` | every plan row | `overview.time.in_phase` is fed the wire phase token beside `PhaseLabel` (against `components-tags.adoc:263` "Never twice"); `pausing_point` not shown; the epic name is plain text where `plan-overview.adoc:36` says a link | minor |
| L3-13 | `OverviewView.svelte:1178,1265,770,779-785,916,1099` | screen reader | every row's menu button is named "Actions"; the sidebar `tablist` has no panel or arrow keys (pre-existing); the needs marker `! N` is hard-coded text with a `title`; `href={url \|\| '#'}` on `target=_blank` | minor (a11y) |
| L3-14 | `PhaseLabel.svelte:92-93` | a runtime role outside the six | reads "· unknown"; the set was not settled with the runtime docs | minor |

**Layer 3 verdict:** findings — no blocker; two major conformance defects (L3-1, L3-2), one major only on an event rate (L3-3); 9 minor, 3 of them leads.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer waits | conforms | no hit in the added lines |
| CLAUDE.md Dependencies | none added | conforms | no `pom.xml` or `package.json` in the diff |
| spec Standing | VD-S-06 v2 components only | **deviates** | L3-2 |
| spec Standing | the design document of this plan is updated in the same PR | partly | L1-9; stale lines below |
| spec Standing | text guard baseline only shrinks | conforms | not in the diff |
| spec Standing | the row order is the runtime's | conforms | L1-8 |
| spec Write-Boundary | no write under `.plan/orchestrator/` | conforms | the lane states none; #459's file list has no `.plan` path (re-read: `git show --stat d79cc5f1`) |

**Stale design lines:** `doc/design/web-app/navigation.adoc:78` ("lists every project with ended work": the view lists the selected project's); `doc/design/shared/plan-overview.adoc:81` and `terminal-client/15-overview.adoc:103` (`in execute 25 min`) against `components-tags.adoc:263`; `plan-overview.adoc:36` (a link to the epic) against `OverviewView.svelte:1092`.

## Metrics and Anomalies

- Tokens `unknown`; `plugin_version` `unknown`; executor `0.1.2135`. `landing-check`: `complete: false`, `missing_keys` `total_tokens`, `plugin_version`; its `surface_delta` is `unmeasured`.
- Self-review: four rounds (10, 4, 2 items, then a confirmation round); the operator chose one confirmation round past the three-round limit.
- The merge queue dequeued #459 once on a Maven Central 429 in the merge-group `quarkus-alignment` job; the operator chose "re-enqueue now".
- One routed `verify -Ppre-commit` hit the daemon's 1098 s supervisory bound at 18:14 min while `pm-web-app`'s `npm run test:ci` ran (5102 passed up to the kill); the split form was green. A second e2e run timed out 23 `waitForApp` checks under daemon load.
- The lane's rule breaks, by its own count: one heredoc `python3`, shell `ls`/`stat`/`wc`/`find`/`grep`, two reads outside the repository (the installed lane text, the daemon job logs); git, `.plan/` and the spec read kept. No guard was enforced on this harness.
- The mailbox read with a digit-leading plan id is refused (`invalid_target_plan`): reported by the PLAN-24, 60, 64, 49 and 51 lanes, not by this one.

## Routing and Merge Behavior

- Review: CodeRabbit, three reviews, 7 findings fixed (`22205088`, `48f39137`, `5e3c67fb`), threads answered and resolved; re-read: `review-gate --pr-number 459 --required-reviewer coderabbit` shows `unresolved_review_threads=0`, `review_body_findings_total=4` (its `handled:0` is the gate lacking `--disposition`; the lane states all four were passed as dispositions). Sourcery refused on quota, optional.
- Sonar: `sonar_open_new_issues=0`; the Sonar MCP search for pull request 459 returns no issues.
- Merged through the queue (merge-group run 44073849188).

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-52 --status shipped`
- [x] row `pr` stamped — `459`
- [x] row `landing` stamped — `landings/PLAN-52.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-52-web-running-work`
- [x] epic.md: Open Defect `PLAN-52 landed`
- [x] PLAN-49 L3-2 (the Back race) recorded as closed by #459 in the epic's PLAN-49 entry
- [x] resume anchor and `queue-view.md`
- [x] the inbox message archived on consume

## Follow-Ups

- **A PLAN-52 residue plan** (named, NOT staged): the `RowLink`/`RowMenu` rows with the row-naming act label, the pinned row, the follow mode, VD-S-06 on the ended and alongside rows (L3-2), the skeleton on a background refresh (L3-3), the by-epic filter and the empty states (L3-4..6), the three leads (L3-7..9), the ended page's home (L3-10), the `in_phase` double token and `pausing_point` (L3-12), the stale design lines, the BottomBar indentation, the e2e assertions that cannot fail. The `alongside` onboarding or setup run waits on a contract change in `pm-api` and the daemon.
- PLAN-51 and PLAN-152 still share four files with each other (`decisions.*.properties`, `DecisionView.svelte`, `decisions-texts.adoc`); both lose their overlap with PLAN-52.
- Whether a residue plan precedes anything that builds on the running-work rows (PLAN-144's Up and lens segment, PLAN-83's work list that "reuses these rows and markers") is the operator's decision.

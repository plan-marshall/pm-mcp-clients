# PLAN-185 landing record

epic: ui-improvements
plan: rb-185-web-shell-up-and-deletion-residue (runbook lane, harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2183; `total_tokens` and `plugin_version` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #558, squash `b26be837` (16 files, +882/-252), merge queue, merged 2026-11-24T15:06Z
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); the event catalogue (`06-event-catalogue.adoc:42-45`), `projectOfShownObject` (`runtime.ts:1418-1428`), the cleanup-sentence time (`:1488-1496`) and PlanView's Up gate and epic prop (`PlanView.svelte:799-808`) re-read by the orchestrator at `b26be837`

Message `rb-185-web-shell-up-and-deletion-residue-001` (revision 0): `merge_state=merged`, `deliverables_done=5` of 5, 0 unresolved threads, 6 of 6 review-body findings handled (CodeRabbit inline-1..5 in `30f0bcbc`, outside-diff-1 in `b8011d9d`), `sonar_open_new_issues=0`, `self-review: budget-exhausted` (operator: "Proceed and open the PR"). Not run through `landing-check` this time; the facts block lacks only `total_tokens` and `plugin_version` as before.

**Review numbers re-read** (`review-gate --pr-number 558`, after the merge): 0 unresolved threads, 6 review-body findings, equal to the message; `handled` reads 0 there (`body_findings_undisposed`: the gate reads no disposition line after the merge). Not re-verified beyond the message.

**Sonar.** PR: `sonar_rest gate-status --pr 558` OK (new coverage 81.7). Default branch: still the 9 keys of `landings/PLAN-186.md` (none in `pm-web-app`), but the last `main` analysis is 13:01:56Z, before this merge, so the read **after** #558 is `unknown`, not 0.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 the e2e spec follows the removed Back | met without change: PLAN-177 already fixed `overview.spec.js:182-187` (premise stale at staging) |
| D2 deletion recognised by what the specification gives | met for the recognition (`cleanup.done` + a re-probe; four invented per-subject events and their test deleted); **the cleanup sentence's Done-when is unsatisfiable on the wire**: the catalogue's `cleanup.decided` carries `job_id`, `decision`, `remind_at` (re-read `:43`), the mock sends `by`/`channel`/`device` and no time, only the `pm-api` record `CleanupDecidedEvent` has `who` and `decided_at`. The spec named only "`cleanup.done` carries no ids" as a hypothesis. The report and task destinations are the lane's choice; `cleanup.adoc` names ended work only for a plan or an epic |
| D3 Up on a plan and an epic at every width | met in tests at narrow and wide; partly refuted (below) |
| D4 the work list shows siblings | met |
| D5 unused names removed | met |
| Design drift | the one-column rule of `navigation.adoc:111` is a content width below 744 px; spec and code use the viewport query `max-width: 1023px` (the column's own `@media`) |

True count by reading: 4 of 5 (D2's sentence half unmet by the wire, not by the lane).

## Layer 2: implemented

No unexplained file: `narrowLayout.ts` and its test, the `PlanView`, `EpicView`, `WorkListColumn` edits and `work-list.spec.js` trace to D3/D4, `OverviewView` and `ReportsView` to D5. Gaps: no test fails when the fall-back's navigation guards (intent, epoch, route, still-shown, `runtime.ts:1479-1484`) are reverted; none for the stale-epic prop (`PlanView.svelte:803`); none for Up during the first read. The decision tests feed a nested `{decision:{id,subject}}` the engine does not send (it sends top-level `id`/`outcome`); `cleanup.decided` is fed the record's shape, which neither catalogue nor mock carries.

Lane claims not borne out: "Up does not vanish at narrow while the first read runs" (the gate is `$narrowLayout && !isLoading && …`, re-read `PlanView.svelte:808`; `EpicView.svelte:704` the same); "the fall-back takes the project only from the matched resource" (on a mismatch it falls back to `selectedProjectId ?? lensProject`, re-read `runtime.ts:1422,1427`). Confirmed: every other D1-D5 claim and the self-reported residue.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker. The fall-back's race handling is sound as read (epoch and intent captured before the await; any route change aborts; non-404 errors never navigate); `narrowLayout` is sound (per-subscriber `matchMedia`, missing `matchMedia` reads wide, one Up per width).

| # | Severity | Finding | Where | in lane residue |
|---|---|---|---|---|
| L3-1 | **major** (contract) | the cleanup sentence ("deleted by {who}, {time}") never renders against the catalogue's or the mock's `cleanup.decided`; only unit tests that feed the record's `who`/`decided_at` reach it, so the web shows the generic `web.nav.gone` in practice. Three shapes disagree: catalogue (`decision`, `remind_at`), mock `MockRuntimeEngine.java:11501` (`by`, `channel`, `device`), scenario `reports-and-system.json:3268` (`at`, `scope`), record (`who`, `decided_at`). Needs an engine/specification decision. | `06-event-catalogue.adoc:43`, `runtime.ts:1519-1535` | yes |
| L3-2 | minor | Up is absent at narrow during every first read (`!isLoading` in the gate), against the lane's claim. Re-read. | `PlanView.svelte:808`, `EpicView.svelte:704` | no (claimed fixed) |
| L3-3 | minor | on a cache mismatch the fall-back takes the project from a stale explorer selection or lens, so the operator can land on another project's ended work. Re-read. | `runtime.ts:1418-1428` | no (claimed fixed) |
| L3-4 | minor | a deep link to a deleted or unreadable plan with no selected project shows no work list and so no Up at ≥ 1024 px (the column renders only `{#if planProjectId}`), while narrow shows PlanView's own Up | `PlanView.svelte:799`, `EpicView.svelte:700` | no |
| L3-5 | minor | in-place plan switch A→B: `parentEpicId` drops to null until B loads, so the column flips to the project list and back, and the column's own Up still names A's epic from the stale tree | `PlanView.svelte:803`, `WorkListColumn.svelte:166-176` | no |
| L3-6 | minor | the notice names the object from `currentProcessTree.title` / `currentEpicResource.display_name` without checking that the cache is the shown object; after a switch it can name the previous one | `runtime.ts:1373` | no |
| L3-7 | minor | an unparsable `decided_at` gives `formatTime` = "" and the sentence "(by X, )"; the test asserts only the prefix. Re-read. | `runtime.ts:1493` | no |
| L3-8 | info | one cleanup slot bound by job id: interleaved jobs lose attribution and degrade to the generic notice (safe) | `runtime.ts:1349` | no |

Also confirmed, reported by the lane and left as the spec required: `decision.closed` spells its field `closure` in the catalogue (`:87`) and the mock (`running-work.json:7270`) but `outcome` in the engine record and `DecisionView.svelte:617`; `detail.plans` is documented as active plans (`ProjectDetailResponse.java:22-31`) while the mock serves an ended plan; `theme-readability.spec.js:408,910` still expects `/projects/onboard$` against `routes.ts:299` `/projects/onboarding` (the lane's "11 `-Pe2e` failures on the base"; count not shown).

Conformance: ES modules, flat ESLint, no `{@html}` (the decider name renders as text, `+layout.svelte:415`), no timer waits, no catalogue change, en/de parity, no dependency, no shim: conforms. "Tests fed with wire values": **deviates** (the record-shaped `cleanup.decided`, the nested decision shape). Red runs, browser evidence, test-count delta: not shown by reading.

## Residue for the plans that touch the same surface

- **Contract decisions for the engine** (`pm-mcp-core`, not here): the `cleanup.decided` payload (L3-1) and `decision.closed` `closure` vs `outcome`. Until decided, the client's cleanup sentence is dead code in practice.
- **Shell residue** (L3-2 to L3-7): no staged plan carries `PlanView`/`EpicView`/`WorkListColumn`/`runtime.ts`'s fall-back; PLAN-182, PLAN-85 (`runtime.ts`) and PLAN-179 (`EpicView`) waited for this landing and are fold candidates on the operator's word.
- The `-Pe2e` route rename (`theme-readability.spec.js`, `projects.spec.js`): failing on `main`, not run by CI; a trivial direct change (skip-bot-review) or a fold.
- `detail.plans` documentation vs the mock's ended plan: report only.

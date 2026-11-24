# PLAN-177 landing record

epic: ui-improvements
plan: rb-177-web-start-and-decomposition-views-residue (runbook lane, harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2183; `total_tokens` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #552, squash `e0e95c05` (35 files, +796/-333), merge queue, 2026-11-24
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); L3-1 re-read by the orchestrator at origin/main `e0e95c05`

Message `rb-177-web-start-and-decomposition-views-residue-001` (revision 1): `merge_state=merged`, `deliverables_done=5` of 5, 0 unresolved threads, 0 review-body findings, `sonar_open_new_issues=0`, `self-review:budget-exhausted`. `landing-check`: `complete: false`, missing `total_tokens`. One operator question: the pre-commit rewrites of 11 files outside the plan — "commit separately and use skip-bot-review as PR label" (operator's free text).

**Process gaps, recorded, not excused:** CodeRabbit never reviewed (skip-bot-review on the operator's word, so the merge rule's one-review requirement was waived by that instruction); the self-review round ran **after** the merge, so its findings are residue, not fixes.

**Sonar open new issues: 0 (by the lane; gate read here).** `sonar_rest gate-status --pr 552`: OK (new coverage 94.7, duplication 0.0, hotspots 100%). `read_open_issues --pr 552`: `undecidable` (`head_revision_unavailable`). `sonar_rest search --pr 552` lists four issues of any status (`java:S1135` Libc.java, `java:S1612` DecomposeCommandTest.java, `java:S1135` ExitCodesTest.java, `typescript:S1874` ime.ts); none is in the default-branch open set (113 keys, identical to the 2026-11-23 read).

## Layer 1: specified

| D | Verdict |
|---|---|
| D0 start gate, Abandon, Continue reason | partly met: the `{seconds}` test expects the old hard-coded "20 seconds" and cannot fail (StartView.test.ts:359); PLAN-53 L3-1's second half (a retry leaves a second entry) neither fixed nor reported |
| D1 ready step works | partly met: `onSubmit={create}` now passed (StartView.svelte:1110), but RecipeReadyView.test.ts:53,64-84 still inject `onSubmit`; the "one primary" check at StartView.test.ts:493 passes at base; EpicAdoptView met |
| D2 decomposition review | **not met in substance for Drop** (L3-1, L3-2); Order/Deliverables rows removed, objective no longer merged, error inside the dialog: met |
| D3 tests that can fail | partly met: StartView.scenarios.test.ts:651-659 asserts fixture absences and cannot fail on the named line; focus asserted in jsdom only |
| D4 browser evidence | e2e not checkable by reading; ready and adopt screenshots taken before the last visual commit (lane admits it) |
| Constraint: wire values only | violated (EpicDecompositionView.test.ts:376 feeds a cut the engine never sends) |
| Constraint: engine decisions not made in code | violated (the Drop contract, L3-1) |

True count by reading: 2 of 5 fully met (D1 adopt part and D4 aside, D0/D1/D3 partial, D2 not met for Drop).

## Layer 2: implemented

17 files outside the Expected Surface: the 11 pre-commit rewrites (`b2a2ed21`, `e5ae2b88`: pm-operator main whitespace only — PlanCommand.java:1431,1461-1462, ReportsCommand.java:171, RolesCommand.java:237 — reformatting code main itself brought in, so the pre-commit gate is not idempotent on main; Libc.java:90-93 moves a `cui-rewrite:disable` comment; ExitCodesTest.java:657,692,737 widens `throws IOException` to `throws Exception`, test-only, gratuitous; PicocliLanguageTest import order), plus `decomposition.{en,de}.properties`, `SourceList.svelte`, `start/sourceLabels.ts`, `utils/ime.ts`, `overview.spec.js`. No mock-runtime change (the `scope_not_found` work was a temporary mutation). Claims not borne out: the PR says Drop is "not decided in code"; `ime.ts:7` claims a held-key repeat guard with no `event.repeat` check.

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where | in lane §9 |
|---|---|---|---|---|
| L3-1 | **major** | Drop + Agree corrupts the cut: the drop is sent as `instruct {plan_id: null, action: 'rewrite', "Drop the plan X from the cut."}`; the mock resolves a null plan id to the **first open plan** and rewrites its objective to the drop sentence, the dropped plan stays, and commit commits everything; `droppedPlans` is cleared regardless. The CLI already settled the contract: "The decomposition contract has no act that drops a plan, so no drop key is offered" (EpicDecompositionDialogue.java:287-289), i.e. the spec's "do not offer Drop" arm. Re-read by the orchestrator. | `EpicDecompositionView.svelte:237-247`, `MockRuntimeEngine.java:4673-4679,4688-4700` | yes (without the mock and CLI evidence) |
| L3-2 | **major** | the drop test cannot catch L3-1: it mocks `instructDecomposition` to return a cut without the plan (never sent by engine or mock) and asserts the English prompt, not the harm; the e2e drop test never clicks Agree | `EpicDecompositionView.test.ts:366-399`, `decomposition.spec.js:76-88` | no |
| L3-3 | minor | Drop cannot unblock Agree: the gate counts findings of the dropped plan | `EpicDecompositionView.svelte:507` | no |
| L3-4 | minor | PLAN-53 L3-1 (retry leaves a second entry) unaddressed; `gateRequest?.abort()` does nothing for an already timed-out gate | `StartView.svelte:289` | no |
| L3-5 | minor | Continue's reason wrong: "waits for 0 sources" when all settled; a sources reason while only `busy`; "1 source" with none | `StartView.svelte:165-169,1475` | partly |
| L3-6 | minor | the ellipsis on the shared `decomposition.edit` leaks into the CLI's no-input need text | `EpicDecompositionDialogue.java:295` | no |
| L3-7 | minor | the abandon-failure test is confounded (fails at base on the missing `More` button) and possibly racy | `StartView.test.ts:398` | no |
| L3-8 | minor | recipe-row `aria-label` concatenated outside the catalogue ("Ein Rezept wählen code-review"); German `übernommen` vs `committet` mismatch | `StartView.svelte:1068` | no |
| L3-9 | minor | scenario rewrite asserts fixture absences, cannot fail on the named line | `StartView.scenarios.test.ts:657-658` | no |
| L3-10 | nit | gate sentence lost its live region; review panel repeats the plan id under its heading; footer Add/Draft again not disabled while busy; `client.ts` StagedPlanRecord lacks the wire's `origin` | `StartView.svelte:988`, `DecompositionReviewView.svelte:236`, `EpicDecompositionView.svelte:494-499` | no |

Lane §9 checked: Drop contract confirmed (worse than stated); `Compose.svelte:103` dead `IME_KEY_CODE` confirmed; dimming reads local state only (`:402`) confirmed; lost `onTitle`/`onParameter` coverage confirmed; starting-work-texts.adoc drift **partly refuted** (`:57-59` "20 seconds" and the 4 missing keys confirmed; `:313/321/349` refuted, those `start.decomposition.*` keys did not change); four unreferenced keys: 3 of 4 confirmed, **`decomposition.locked` refuted** (DecomposeCommand.java:393, EpicDecompositionDialogue.java:446); "0 sources" confirmed; ExitCodesTest widening confirmed (test-only); stale design-doc pointers confirmed.

Checked and fine: gate abort keeps the TimeoutError reason (`client.ts:4086`), stale generation ignored; Abandon's `accepts()` holds against StartRegistry.java:1045-1050; `isConfirmingEnter` shared; the no-option message restored; Order/Deliverables rows gone and `client.ts` matches StagedPlanRecord.java; `locked`/`dropped`/`committed` exist on the wire; en/de parity of the new keys; no `{@html}`, no time-passing waits.

## Residue for the plans that touch the same surface

- **Drop** (EpicDecompositionView and the pre-existing rewrite-drop in `StartView.svelte:852-854`): withdraw Drop, as the CLI does, unless the engine gains a drop act (engine decision, `pm-mcp-core`). Needs a home: no staged plan carries the decomposition view; a fix plan only on the operator's word.
- **StartView** (PLAN-137 parked touches it next): L3-4, L3-5, L3-7, L3-8, the title/parameter wiring tests, the PLAN-53 residue the spec kept open.
- `Compose.svelte:103` dead constant; three unreferenced catalogue keys; split `decomposition.edit` for the CLI.
- `doc/design/shared/starting-work-texts.adoc:57-59` and the four new `start.decomposition.*` keys: a trivial direct change (skip-bot-review), together with the stray `+` of `starting-work.adoc:25`.
- PLAN-186 meets the pre-commit whitespace in PlanCommand/ReportsCommand/RolesCommand; `-Ppre-commit` not idempotent on main (provider-side defect already recorded).
- `inbox_read_command` defect recurs (already recorded).

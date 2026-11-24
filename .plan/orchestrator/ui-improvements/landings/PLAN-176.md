# PLAN-176 landing record

epic: ui-improvements
plan: rb-176-web-wire-values-and-onboarding-residue (runbook lane, harness antigravity, models Gemini 3.8 Flash / Claude Sonnet 5.5 / Gemini 4 Neon across pauses, executor 0.1.2183; `total_tokens`, `model`, `effort` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #549, merge `ae123acb` (42 files, +1085/-498, 11 commits plus a merge from main), merge queue, 2026-11-23
analysed: 2026-11-24, by sub-agent in three layers, by reading only (no build); the two major findings re-read by the orchestrator at origin/main `e0e95c05`

Message `rb-176-web-wire-values-and-onboarding-residue-001`: `merge_state=merged`, `deliverables_done=5` of 5, 0 unresolved threads, 8 of 8 review-body findings handled, `sonar_open_new_issues=0`. `landing-check`: `complete: false`, missing `total_tokens`, `model`, `effort`. One operator question: enqueue on a false-positive `thread_concern_remains` (`PRRT_kwDOVBOtss67D5Xs`, "still present" in CodeRabbit's edited opening comment) — selected.

**Sonar open new issues: 0 (by the lane; gate read here).** `sonar_rest gate-status --pr 549`: OK (new coverage 81.8, duplication 0.0, hotspots reviewed 100%). `read_open_issues --pr 549`: `undecidable` (`head_revision_unavailable`, branch merged). `sonar_rest search --pr 549` lists one issue of any status, `typescript:S7760` `AaIKEqTIROfW-4a801vP`, fixed in `d2feb81e` per the message; it is not in the default-branch open set (113 keys, identical to the 2026-11-23 read).

## Layer 1: specified

| D | Done-when | Verdict |
|---|---|---|
| D1 | completion report fed with wire and engine states, en+de, neutral word for unknown | met (test fails at start: the keys did not exist) |
| D1 | no raw wire code in the tokens section | met with caveats (L3-6, L3-7) |
| D2 | needs marker per project kind; epic and plan rows; ended outcome | met in the test, weak in substance (L3-4) |
| D3 | identity recorded through the entry link; reload restores the run; card at step 3 | **met in unit tests only, not against the wire** (L3-1, L3-2, L3-3) |
| D3 | `projects.spec.js` address survives reload | not checkable by reading, probably fails (L3-2); no e2e run listed at the head |
| D4 | each modal item has a failing-at-start test | partly: two tests cannot fail (L3-8) |
| D4 | removed components and keys absent | met (`git grep` at `ae123acb`) |
| D5 | GrantModal e2e; screenshots after the last visual commit | e2e present (loose URL regex, "grant once" asserts no outcome); screenshots taken at `99e69cf3`, before the visual commits `d2feb81e`, `00f25ddb` |

True count by reading: 4 of 5 (D3 not met against the wire).

## Layer 2: implemented

Outside the Expected Surface, all justified: `DeliverableSummary.java` (Javadoc), `overview.{en,de}` (`completed` outcome), the removed `start.*` / `lesson_source.*` keys, `PlanCommand.java:1459-1462` (passes `coverage` to the select-valued `report.completion.waste_share`; CI's `PlanCommandTest` failed without it — **PLAN-186 overlap now on main**), `ActMenu.test.ts`, the route rename `projects/onboard` → `projects/onboarding/+page.svelte` plus `[onboardingId]/+page.svelte`, `mobile.spec.js:186`. Declared but untouched: `HoldResolutionModal.svelte`, `history.ts`, `nav.ts`, several specs. PR body claims a focus trap added in `FindingsReviewModal.svelte`; the file is not in the diff (only its test). `client.ts` gained fields no wire record carries: `EnrolledProject.needs_operator_kind` (:141), `BeingAddedProject.links` / `.build_environment` (:157-158), `ProjectOnboardingState.lifecycle_gate` (:237), `PlanCompletionReport.phase` (:1106).

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | **major** | a being-added run cannot save its identity on the real wire: `canSave` needs `run.links.set`, `runOfBeingAdded` takes links only from `being_added`, and `BeingAddedProject` (pm-api) carries no `links` (fields: checkoutPath, currentStep, stepNumber, totalSteps, continueLink, abandonLink); the mock serves `links.set` on `GET /api/v1/entries/{id}` (`MockRuntimeEngine.java:4941,5002`), which the view never reads. The unit tests feed an invented `links.set`, against the standing constraint "never with a value the code invented"; `actEntry(entryId,'set')` rebuilds the path instead of following `href`. Re-read by the orchestrator. | `ProjectOnboardingView.svelte:125,417-444,223` |
| L3-2 | **major** (structural; needs a run) | the run address is overwritten: the layout's `$effect` → `syncPath` replaces the URL with `addressOfState` on every store change, and `routeParamsOf` carries no `onboardingId`, so `/projects/onboarding/{id}` collapses to `/projects/onboarding` and a reload loses the run. Re-read by the orchestrator. | `runtime.ts:1576-1591,2103-2110`, `routes/+layout.svelte:263-277` |
| L3-3 | minor | the needs-terminal card can never clear for a being-added run (no `build_environment` on the wire); the pilot checkbox gate on `lifecycle_gate` is inert (field never sent) | `ProjectOnboardingView.svelte:122-124,730` |
| L3-4 | minor | ready-project marker reads "decision" for any operator item (`|| 'decision'` fallback); the test feeds an invented `needs_operator_kind` | `ProjectExplorerView.svelte:894` |
| L3-5 | minor | "(most used)" left the heading but the sort select still offers "most used", which sorts nothing | `ProjectExplorerView.svelte:1059,162-169` |
| L3-6 | minor | `unmeasured_reason` is prose on the wire; the catalogue lookup misses it and shows English in the German UI; the test feeds a code the mock never sends | `CompletionReportView.svelte:239-243` |
| L3-7 | nit | new keys `report.completion.waste.{no_product,retries,remediation}` are dead (`report.tokens.waste.*` wins at :647); the `produced_nothing` branch is dead | `CompletionReportView.svelte:647-656` |
| L3-8 | nit | tests that cannot fail: Dialog overflow restore for a single owner; Dialog focus-outside-menu and FindingsReviewModal Tab-wrap pass at start (Dialog already traps) | `Dialog.test.ts`, `FindingsReviewModal.test.ts` |
| L3-9 | nit | the explorer passes the onboarding id undecoded and `pathForRoute` encodes again; the address slot mixes entry ids, checkout paths and project ids | `ProjectExplorerView.svelte:398-400` |

Checked and fine: Dialog `isComposing` guard and module-level overflow restore; LanListenerModal danger variant plus ConfirmDialog off-loopback; Picker `aria-pressed`; en/de parity of every added key; `/projects/onboard` still parses as project `onboard`; title fallback; empty-deliverables text guesses no phase; no `{@html}`, `innerHTML` or `waitForTimeout`.

## Residue for the plans that touch the same surface

- **PLAN-182** (onboarding contract cut-back, staged; edits `ProjectOnboardingView`, `runtime.ts`, `routes.ts`) is the natural home for L3-1, L3-2, L3-3: read the entry for `links` and `build_environment`, follow `href`, drop the invented client fields, add `onboardingId` to `routeParamsOf`, run `projects.spec.js`. Folding needs the operator's word (layer-3 rule).
- `navigation.adoc:222` and `web-app/README.adoc:494` still list `/projects/onboard` as an old address and name no `/projects/onboarding/{id}`.
- PLAN-185 re-reads `runtime.ts`, `history.ts`, `CompletionReportView.svelte` at the new HEAD (shared surface with this plan).
- PLAN-186 meets `PlanCommand.java:1459-1462` changed on main.
- Tooling (provider defects, owner plan-marshall repo): `_thread_concern_remains` matches "still present" in an edited CodeRabbit comment; `resolve` writes an invalid `inbox_read_command` (already recorded).

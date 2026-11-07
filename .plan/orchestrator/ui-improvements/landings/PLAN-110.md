# Landing analysis: PLAN-110 web-overview-drop-start-page-toggle

epic: ui-improvements
workstream: WS-03
pr: 364

PR #364 `1d8c6197` (merged 2026-11-07T14:05:03Z; final head `f14aa8d1`, a merge of `origin/main` only; reviewed head `a24ecde7`). Lane `rb-110-web-overview-drop-start-page-toggle` (runbook lane, harness antigravity; model, effort, tokens `unknown`): landing message `inbox/rb-110-web-overview-drop-start-page-toggle-001.md`. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); thread count, review list, Sonar status and the residue grep were re-read by the orchestrator (marked V*).

## Merge rule

SATISFIED, on thin evidence. (V*) The PR has 0 review threads and `pulls/364/reviews` is EMPTY: no CodeRabbit review object, so no body items. The only trace of a review is one check run on `a24ecde7` ("Success - Review completed", 0 annotations, 11:55Z; issue comment "Review finished" 11:50Z); the walkthrough comment was overwritten later with the rate-limit text for `f14aa8d1`, so the clean review's text is not recoverable. CodeRabbit refused twice for quota (10:20Z on `a24ecde7`, 12:09Z on `f14aa8d1`; check run "Rate limited", neutral); the lane's recorded cooldown (13:38Z) had passed at the merge (14:05Z), so one more re-trigger was possible; it does not break the rule (one review, later heads need none).

- Unresolved threads: 0 of 0. Body findings 0/0 (nothing to count).
- SonarCloud (V*: check run SUCCESS; bot comment 12:24Z on `f14aa8d1`): Quality Gate passed, 0 new issues, 0 accepted, 0 hotspots; 0.0% coverage on new code (a removal). Rules left open: not read (`unknown`; the lane says none).
- CI: 20 check runs, no failures; deploy-snapshot ×2, covering-run-check, auto-merge and Sourcery skipped; CodeRabbit neutral. The lane's "20/20 passed" overstates. Playwright is NOT in CI (`skipPlaywrightTests=true`): "`overview.spec.js` passed (5 tests)" exists only in the lane's progress file.
- Lane message: the facts block carries the `step.sonar-roundtrip.*` pair only (no `unresolved_review_threads`, `review_body_findings_total/handled`, `sonar_open_new_issues` by name); section 5 says CodeRabbit "reviewed, 0 actionable comments" while no review object exists.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| DW1 `git grep` of `pm_start_route`, `rememberedStartRoute`, `setRememberedStartRoute`, `start_page.(set\|is_default)`, `start-page-toggle` empty | MET (V*: no hit in `pm-web-app/src`, `pm-web-e2e/tests`, `pm-api/src` at the merge head; also clean in `pm-relay`, `pm-operator`, `doc/design`) | true only by obfuscation: `runtime.test.ts:364` builds the key as `['pm','start','route'].join('_')`. The spec contradicts itself (DW1 forbids the literal, DW2 needs a stale key written); the lane worked around it instead of raising it |
| DW2 stale start route in `localStorage` is ignored (test-first) | MET (`runtime.test.ts:362-405`: `landingRoute`, `pairBrowser`, `openStartPage` assertions) | all three assertions fail on the old code by reading. No test-only commit (`795802a1` holds test and code); the "failing run at `3bdd02a2`" cannot come from that commit (`landingRoute` was not exported there): no preserved artefact. Three Act/Assert phases in one `it`, hand-written literals `KXQB-MRTN`/`pmw_*` |
| DW3 `npm run quality`, `overview.spec.js` | MET for `npm run quality` (CI green) | the e2e run is session-local |
| DW4 browser check, de and en, light and dark, 1440×900 and 390×844 | MET: 16 screenshots exist (`baseline-*`, `after-*` in the main checkout `.plan/temp/playwright-mcp/`, outside the PR); the button is gone, the heading is on one line | the de baseline shows English row text under a German UI (a language-switch race, not like for like); ~1 minute between baseline and after |
| DW5 open Sonar issues confirmed 0 at the head | MET | the rule list `unknown` |

## Defects (at `1d8c6197`)

1. MED V DW1 and DW2 contradict each other in the spec; the lane split the key string instead of raising it; DW1 is "met" only by that trick. A spec-authoring lesson for the orchestrator (a grep Done-when must exclude the test that has to write the stale key, or name the allowed literal).
2. MED V the test-first evidence is not preserved or reproducible from the PR history.
3. LOW V landing message: facts lines missing by name; "20/20 passed" and "reviewed, 0 actionable comments" are stronger than the GitHub record.
4. LOW V test shape: three Act/Assert phases in one `it`, hand-written literals.
5. Standing gap (not this plan): the Playwright suite is not run in CI; `overview.spec.js` changes are unproven by CI.

## Scope

Exactly the seven files of the Expected Surface (`OverviewView.svelte`/`.test.ts`, `runtime.ts`/`.test.ts`, `overview.{de,en}.properties`, `overview.spec.js`); `landingRoute` newly exported for the test (the spec allows). No design document names the toggle (V: grep clean; the surviving "remembered place" prose is the different concept the spec names). `runtime.ts` changes only the landing region; the `advanceLifecycleEpoch` count is unchanged (8 before and after), `drain_withdrawn` and `withdrawConsent` intact: no regression of PLAN-106.

## Metrics and anomalies

- 20 check runs; one CodeRabbit round of which no text survives; the operator was asked about the quota and chose merge under the standing authorisation (lane progress file, L).
- Sixth lane in a row whose `done` report outruns what GitHub shows, this time by being empty rather than hiding findings.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 364; `landing` = `landings/PLAN-110.md`; `plan_marshall_plan_id` = `110-web-overview-drop-start-page-toggle`
- [x] lane message archived; epic.md Decisions; anchor; view; ledger committed
- [x] PLAN-109 and PLAN-117 unblocked by this landing (their gate was PLAN-110)

## Follow-Ups

- Test-only gaps (obfuscated key, AAA split, literals): accepted as LOW; fold into PLAN-117 if it runs on `runtime.test.ts` anyway.
- Spec-authoring rule recorded in the epic Decisions: a grep Done-when and a test that must write the same literal are reconciled in the spec.
- Playwright not in CI: the epic Open Defects entry 5 (PLAN-108 landing) covers it.
- Plans 34, 109, 112-116: nothing to fold from this PR.

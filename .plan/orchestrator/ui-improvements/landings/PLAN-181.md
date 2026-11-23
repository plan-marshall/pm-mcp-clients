# PLAN-181 landing record

epic: ui-improvements
plan: rb-181-sonar-standing-debt-modules-part-3 (runbook lane, harness opencode, model deepseek-v4.1-flash/high, executor 0.1.2183; `plugin_version` and `total_tokens` `unknown`: the 2026-10-27 Open Defect, not re-reported)
pr: #544, merge `3db842df` (31 files, +1136/-855) and the follow-up #547, merge `76606841` (2 files, +51/-23), both merge queue, 2026-11-23
analysed: 2026-11-23, by sub-agent in three layers, by reading only (no build)

Message `rb-181-sonar-standing-debt-modules-part-3-001`: `merge_state=merged`, `deliverables_done=2` of 4, 0 unresolved threads, `review_body_findings_total=0`, `sonar_open_new_issues=0`, six questions to the operator answered (one pull request plus a follow-up instead of four; the three dismissals).

**Sonar open new issues: 0.** The lane reports 0. Read independently through the Sonar MCP: gate of PR 544 OK (all ratings 1, new coverage 88.2, duplication 0.4, hotspots reviewed 100%) and of PR 547 OK (new coverage 94.0, duplication 0.0, hotspots 100%); the PR-scoped issue searches of both returned none. The lane's default-branch read after #547 (113 open: `pm-operator` 110, `pm-mock-runtime` 2, `pm-exec` 1, `pm-relay` 0, `pm-web-app` 0) is the lane's claim, not repeated here. The analysing sub-agent's unscoped query returned nothing even for the three named keys, so it was not used.

## Layer 1: specified

| Deliverable | Done-when | Message | Verdict |
|---|---|---|---|
| D1 | no open issue under `pm-mock-runtime` | not met (2 open) | accurate: `java:S1313` at `MockRuntimeEngine.java:10695` and `java:S4449` at `MockRuntime.java:245`; D1 has no "or named" arm (only D4 has) |
| D1 | module verify, test count not lower, no `src/main/resources` | met, 965 vs 964 | accurate by the diff: one `@Test` added (`OnboardingScenarioTest.java:~627-645`), none removed; no file under `src/main/resources`, no POM or JSON in #544 or #547; build runs not checkable by reading |
| D2 | no open issue under `pm-relay` and `pm-exec` | not met (`pm-exec` 1) | accurate: `java:S112` still at `NativeKernel.java:229` |
| D2 | verify and `-Pintegration-tests` | met | the diff matches (ITs edited, none removed); runs not checkable |
| D3 | no open issue under `pm-web-app` | met | 8 of the spec's 9 files changed; `PairingView.scenarios.test.ts` untouched, as the message says; the read is the lane's |
| D3 | same test count, baselines unchanged, e2e | met | only two assertion re-spellings (`CleanupView.test.ts:161`, `PlanView.test.ts:248`), no test added or removed, nothing under `scripts/` changed; the 459/75 e2e run is not checkable |
| D4 | zero read, or every remaining issue named | met by the second arm | key, rule and file of all three are in message § 4; the reasons are in the body of #547, not in the message |

True count by reading: **2 of 4** (D3, D4). D1 and D2 are each missing only the operator-selected dismissals that were never transmitted (`sonar_rest transition` answered HTTP 400; D1 has 2, D2 has 1). The spec's "four deliverables, four pull requests" was not followed: one pull request plus a follow-up, on the operator's selection (message § 7, question 2).

## Layer 2: implemented

Sizes: D2's 7 files +38/-26 is exact; D1's "16 files +890/-695" is the first commit only (the pull request's `pm-mock-runtime` part is 16 files +940/-726); D3's "+153" is +158; the message says 32 files, the diff has 31. Both pull requests are far under the 100-file and 20k-line limits. Nothing outside the spec's Expected Surface; #547 adds only `DesignerRegistry.java` and `MockRuntimeEngine.java`. No dependency, plugin or POM change; no Hamcrest, Mockito or `System.out` in the diff; the spec's "Awaitility in `pm-api` only" is outdated and not violated (the lane did not touch it). All three dismissals are still present at HEAD.

## Layer 3: correct (reported to the operator; no fix plan staged)

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | seven `/*TODO ... Suppress: ... */` comments became real `// cui-rewrite:disable InvalidExceptionUsageRecipe` markers; each site still catches `RuntimeException` in production code, which `CLAUDE.md` forbids (pre-existing). The PR body's "no finding is suppressed" is true for Sonar annotations only; message F8 says "five pre-existing markers only reformatted" | `MockRuntimeEngine.java:1569,5860,10807,11080,11226,11408`, `ScenarioLoader.java:1413` |
| L3-2 | nit | at `:1569` and `ScenarioLoader.java:1413` the marker sits just before `} catch` (at `:1569` after a `return true;` in the try block); whether the recipe honours that position is unverified (the lane reports a green pre-commit run) | same |
| L3-3 | minor | disclosed behaviour change (F7): `StartRegistry.runtimeFixtureOf` returns null for a null `stateId` (was `scenario.state(stateId)`); `List.copyOf` in `StandardScenarios` now rejects null elements; what the old call did with null is unverified | `StartRegistry.java:~322`, `StandardScenarios.java:~445` |
| L3-4 | nit | `Thread.sleep(200)` became `ProcessHandle.of(pid)...completeOnTimeout(200ms).join()`: still waits the full 200 ms when the daemon survives; with an empty handle there is no wait at all | `PmMcpProcessIT.java:~237` |
| L3-5 | nit | PR body and message F4 speak of `new Promise<Response>(...)`, the final code has none | `scenarioMockFetch.ts:~139-146` |
| L3-6 | nit | `StartRegistry` still carries a third `"outcome"` literal (message § 10) | |
| L3-7 | minor | review gap: #547 merged on an operator-selected head without a satisfied review gate | #547 |

Checked and fine: every constant folded in `MockRuntimeEngine` matches its literal (`KEY_*`, `PREFIX_PROJECTS`, `PATH_*`, `STATE_ENDED`); every extraction read keeps the order of effects (decomposition dispatch, control routes, hold resolution, cleanup-decision split, ended page, process routes; `handleControlRead` merges the actions and tokens reads before the plan routes, the suffixes are mutually exclusive); `DesignerRegistry` constants byte-identical; `readBodyOrEmpty` sound (`MockHttpRequest.java:51` normalises a null body); `PmMcp` private constructor made S2440 real, nested `Root` keeps the command; the removed `Thread.sleep(50)` in `RunningWorkScenarioTest` is justified; the onboarding split keeps every assertion; the web-app changes keep behaviour (`pathForRoute` all 14 arms, `replaceAll('#', ...)`, the SSE helpers, type-only union widening); no markup, i18n key, text, wire name or `src/main/resources` change; no `{@html}`, `innerHTML` or `waitForTimeout`.

## Residue for the plans that touch the same surface

- Three Sonar issues stay open on the default branch until the operator dismisses them by hand in the Sonar UI: `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`), `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`), `AaEcTdY0MJNX7b6L9QAy` (`java:S112`); the reasons (wontfix, wontfix, false positive) are in the body of #547. Provider defect recorded already: `sonar_rest transition` answers HTTP 400.
- The `pm-operator` Sonar part is not staged (110 standing issues, no breakdown); `PmOperatorProcessIT` still has `Thread.sleep(100)`; Awaitility is in `pm-api`, `pm-relay` and `pm-operator`.
- Anyone editing `MockRuntimeEngine.java` (`handleControlRoutes`, the process routes, cleanup, decomposition), `ScenarioRegistry`, `routes.ts` (`pathForRoute`), `client.ts` (the SSE loop) or `scenarioMockFetch.ts` reads the new helper structure first: PLAN-183 cites line numbers of `MockRuntimeEngine.java` that moved.
- `inbox read --plan-id` rejects digit-leading plan ids (provider defect, recorded).
- `backup/plan-175-full` (local only) is deleted at this landing: both PLAN-180 and PLAN-181 have landed.

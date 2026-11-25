# PLAN-182 landing record

epic: ui-improvements
plan: rb-182-web-onboarding-contract-cut-back-residue (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` and `plugin_version` `unknown`; executor 0.1.2183)
pr: #569, squash `bb1e523b` (36 files), merge queue (`merge_group` run 45620149864 success per the message), merged 2026-11-25T13:05:39Z
analysed: 2026-11-25, by sub-agent in three layers, by reading only (no build); `i18n/index.ts:673-731` with the test inventory (`git grep` of `adoptRecordedLocaleBeforeStep`, `ADOPTION_BOUND_MS`, `withinBound`: no test hit), `MockRuntimeEngine.java:9159-9172`, `client.ts:3297-3314` with `request`/`assertRequestPath`, `RolesReview.svelte:259`, `OnboardingView.svelte:344-352` and the `projects.spec.js` diff (empty) re-read by the orchestrator at `bb1e523b`

Message `rb-182-web-onboarding-contract-cut-back-residue-001` (revision 1, amended 2026-11-25T13:22:51Z; revision 0 of 2026-11-24 said `merge_state=open`): `merge_state=merged`, `deliverables_done=2` of 5, 0 unresolved threads, 11 of 11 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear` (two rounds). The lane counts D1, D4 and D5 as not met as written: their Done-whens ask `test-first`, the evidence is `mutation-after` (behaviour evidenced, order not). `landing-check`: `complete: false`, missing `total_tokens` and `plugin_version` (the 2026-10-27 Open Defect, opencode).

**Merge corroborated:** `ci pr view 569` reads `merged`, merge commit `bb1e523b4f38335c2c3a641206f2c8d838f9aec1`, the first commit on `origin/main` after #571 (`2b6939fe`).

**Review numbers re-read** (`runbook_lane review-gate --pr-number 569`, 2026-11-25T13:29Z, after the merge and the lane's cleanup): 0 unresolved threads, 11 review-body findings, 0 handled (`body_findings_undisposed`). The message gives 11 of 11 handled, every thread replied and resolved; the re-read after cleanup does not see the lane's dispositions (the same for PLAN-189, `landings/PLAN-189.md`). The thread count agrees.

**Sonar.** PR: 0 new issues (the message; SonarCloud 87.9 % new-code coverage at `f93a0f58`). Default branch, read by the orchestrator 2026-11-25 after the `main` task `AaIS8CGVjEZg6XD6BEKY` (submitted 13:40:57Z, SUCCESS), later than the merge (13:05:39Z): quality gate OK, `confirmed`, **3 open** — the three PLAN-181 keys only, none in a file this PR touched. The read names no analysed revision; the task is the first after the merge, and `origin/main` has since moved to `359c1bbd` (#578, docs), so it covers `bb1e523b` or that later commit, which contains it.

**SNAPSHOT deploy of `bb1e523b`:** `unknown` (the `ci` surface has no commit-scoped run reader).

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 a Change runs through `onboard.next`, the seven `*.reconfigure-result` states go | met by reading for the removal (no `reconfigure-result` left but a negative assertion) and for the mock mapping of all seven aspects (`onboardingTarget`, `MockRuntimeEngine.java:9159-9172`: `change:*` from `auto.result` sets the flag, a `*.reconfigure` state returns to `auto.result` unless `back`); the round-trip Done-when is **tested for `language` only** (Java, web, e2e) — the six others, the two multi-hop ones (`harness`, `store`) included, by reading only; the lane's order `mutation-after`, not `test-first` |
| D2 `harness.none` | met: `known[{harness_id, install_hint}]` as the engine's `HarnessNone` and the catalogue row; only `recheck` and `add`; no `checks`/`path`/`check`/`later`. The raw `harness_id` is shown (the engine row carries no profile name) |
| D3 roles review and minors | **not met for one item**: the record step's bounded language adoption (`adoptRecordedLocaleBeforeStep`, `ADOPTION_BOUND_MS` 2000, `index.ts:689-731`) has no test; the test "gives up the wait when no runtime answers" calls the unbounded form. Reverting the bound fails nothing, against the Done-when "each item has a test that fails with its production line reverted". The message says D3 met |
| D4 design and browser | design lines met by reading; `onboarding-texts.adoc` no longer names the seven kept `onboard.link.change:<aspect>` keys (they stay: the terminal dialogue's `everyLinkHasALabel` needs them, refuting the spec's "dead") and does not list the new `onboard.error.step_refused`; browser evidence is the lane's (34 or 32 screenshots: message and PR body disagree) |
| D5 identity and address | (a) identity met by reading on the mock's entry (`actByHref` with the entry's `links.set`; invented fields gone; real daemon fields unverified); (b) address met for a being-added run (`runtime.test.ts` test-first); the e2e reload test `projects.spec.js:112-126` is **not in the diff** (pre-existing from #549) |

True count by reading: 2 of 5 as the lane counts (order); by content D1 partly (one aspect tested), D3 one item untested.

## Layer 2: implemented

All 36 files trace to a deliverable; none unexplained. Outside the Expected Surface but in scope by content: `MockRuntimeEngine.java`, `CompletionReportView.svelte` (+test), `ProjectExplorerView.svelte` (+test), `ProvidersFailedScreen.svelte`, `theme-readability.spec.js`. The PR body says the base-sha instrument was available and not used, so the `mutation-after` order was a choice, not an instrument gap. The squash body keeps the placeholder "Thread count, resolved count and review-body items are filled from `ci pr comments`…" unfilled.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker. Model and engine text reach text sinks only (no `{@html}`).

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | major | the bounded locale adoption the record step now calls has no test; the only test exercises the unbounded form | `pm-web-app/src/lib/i18n/index.ts:689-731`, `runtime.ts:2005` |
| L3-2 | major | the Change round trip is tested for `language` only; the multi-hop `harness` (consent → reconfigure) and `store` paths and the `back` branch are untested | `OnboardingScenarioTest.java`, `OnboardingView.scenarios.test.ts`, `onboarding.spec.js` |
| L3-3 | minor | `back` after a Change goes to `settings.choose`, which the onboarding frame cannot draw (`OTHER_FRAME_STATES`, `screens.ts:170-187`): catalogue-faithful, a dead end in the web | `MockRuntimeEngine.java:9171` |
| L3-4 | minor | `harness.scan` is a mock working state with a `continue` link the operator must click; the catalogue's `harness.scan` is a composite job with no links | `onboarding.json` (~1029-1058) |
| L3-5 | minor | the refusal alert stays after a failed reload or a `StepBar` step change; no test asserts it is cleared; `startOnboardingAuto` still swallows a refusal | `runtime.ts` (~1936-2085) |
| L3-6 | minor | the roles edit session is module state never cleared on record or leaving (declined by the lane with reason); `decide` signature line merged with its first statement (format defect, also `i18n/index.test.ts` ~31) | `RolesReview.svelte:259` |
| L3-7 | minor | `openProjectOnboarding(null, projectId)` writes `/projects/onboarding/{projectId}`, but `routeParamsOf` reads only `selectedOnboardingId`, so the layout rewrites it to the bare path and a reload loses the run (callers not shown) | `runtime.ts:1684-1706`, `+layout.svelte:264-268` |
| L3-8 | minor | new Java tests feed hand-written literals as input (`OnboardingScenarioTest` ~269-293), against `CLAUDE.md`'s `Generators` rule | as named |
| L3-9 | minor | the design's "the segment after `/projects/` is always a project's id; no id can collide" is overstated: `/projects/onboarding` shadows a project whose id is `onboarding` | `navigation.adoc:221`, `routes.ts:295-312` |
| L3-10 | info | the lane's residue "the spelling after `change:` is unspecified" is half right: the catalogue spells the link forms `change:<aspect>` (`14-machine-onboarding.adoc:207`) and `reconfigure:<aspect>` (`:95`); the aspect vocabulary is unspecified, and the mock mixes `harness` and `harnesses` | engine catalogue; `onboarding.json:253-257` |

Refuted on re-read: the sub-agent's "`actByHref` promises a same-origin guard the body lacks" — `request()` calls `assertRequestPath` (`client.ts`), which refuses a path that does not start with `/`, or that starts with `//` or `/\`.

Conformance: text sinks, no timer wait, ES modules, no dependency, JUnit 5, Javadoc, trailers: conforms. `Generators` for input: deviates (L3-8).

## Residue for the plans that touch the same surface

- **PLAN-55** (running) shares `client.ts` and `runtime.ts` with this merge: it rebases onto `bb1e523b`.
- **PLAN-85** and **PLAN-179** no longer wait for PLAN-182, but both share several files with the running PLAN-55 (PLAN-85: `routes.ts`, `runtime.ts`(+test), `chrome.spec.js`, `mobile.spec.js`); re-read against `bb1e523b` before their emit.
- **The doc-review remainder** (`over_size`, `added_dependencies`) no longer overlaps PLAN-182; it still overlaps PLAN-55.
- **Contract gap for the engine:** the aspect vocabulary after `change:`/`reconfigure:` (L3-10), for pm-mcp-core's catalogue.
- **Mailbox address for a digit-led plan id:** recurs (the lane read at `rb-182-…`).

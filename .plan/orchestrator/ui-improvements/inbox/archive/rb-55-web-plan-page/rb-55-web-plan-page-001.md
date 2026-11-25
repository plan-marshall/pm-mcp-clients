envelope_version=1
sender_type=plan
sender_id=rb-55-web-plan-page
epic=ui-improvements
kind=landing
created=2026-11-25T15:53:13Z

```landing-facts
schema=landing-facts/1
plan_id=rb-55-web-plan-page
epic=ui-improvements
pr=#576
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
unresolved_review_threads=0
review_body_findings_total=26
review_body_findings_handled=26
sonar_open_new_issues=0
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done,cleanup:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-55 — Web: the plan page on the object page

Merged into `main` as `163e2a49` through the merge queue (merge-group run `45647218636`, success). Pull request #576 from `feature/55-web-plan-page`; the branch was rebased onto `3dea04b2` (#569 among it) before the final gates, eight commits, force-pushed with lease to `ec9399ba`, then two review commits (`b9e438e5`, `88820839`). Base `main`.

The binding scope is the spec's `## Re-Grounding 2026-11-25 (before emit, at 459eca7d)`: four deliverables. The older `## Deliverables` and `## D6 Merged 2026-11-11` text is superseded by it and was not treated as scope.

## 1. Deliverables

**D1 — the head on `ObjectPage`**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| The plan view renders its head through `ObjectPage` (kind mark, title, id chip, PR chip, phase and status labels, the duration line with "task N of M"), and `plan-breadcrumb` is gone | met | `test-first` | `PlanView.test.ts` "PlanView head on the object page (PLAN-55 D1)" | `82cb7aa0`, `pm-web-app/maven-2026-11-25-090824.log` — "6 failed" in the D1 block, "Tests 19 failed \| 3440 passed" | `c6a0314f` (with D2, D3), `0ff49dd7` (self-review), `f97ced69` (review), `b9e438e5` (rebased review), `88820839` (tests) |
| A waiting plan shows the attention strip "Waits for your decision" and the decision link; an ended plan shows the ended banner and the completion-report link | met | `test-first` | same block ("waiting plan", "ended plan") | `82cb7aa0`, same log | `c6a0314f` |
| The head actions explain themselves on hover | met | `test-first` | `PlanView.test.ts` "explains the head actions on hover" | new test at `88820839` | `b9e438e5`, `88820839` |
| A decision that blocks an epic or a project is not the plan's wait | met | `test-first` | `PlanView.test.ts` "does not take a decision that blocks another object for the plan wait" | new test at `88820839` | `b9e438e5`, `88820839` |

**D2 — the Intent tab, the 720 px layout, the address and the plan settings**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| The tabs are Process, Intent and Changes; Intent renders the three intent panels; Changes appears from the first capture | met | `test-first` | `PlanView.test.ts` "PlanView tab set and column layout (PLAN-55 D2)" (6 tests) | `82cb7aa0`, same log (the 11 D2 names are listed in it) | `c6a0314f`, `0ff49dd7` |
| The address carries the tab and the file, and an unknown tab or Changes-before-capture opens Process | met | `test-first` | same block (address cases) + `intent.spec.js:48` | `9dddbdaa` with `c6a0314f` reverted, `pm-web-e2e/maven-2026-11-25-093050.log` — "plan-panel-host … not found" | `c6a0314f`, `9dddbdaa` (spec + mock) |
| The wide column is measured on `.plan-view` and the 720 px line is the column's own width | met | `test-first` | `PlanView.test.ts` reports the box on `plan-view` and asserts the layout on `plan-panel-host` | `82cb7aa0`, same log | `f97ced69` |
| The settings form applies each field on its own, reseeds on a new revision, keeps a pending edit, and treats Keep-current as the field's answer | met | `test-first` | `PlanSettingsModal.test.ts` (5 tests, +2 at `88820839`) | `82cb7aa0`, same log | `c6a0314f`, `f97ced69`, `b9e438e5`, `88820839` |
| The menu, the selection and the panel dispatch derive from the router's `PLAN_TABS` | met | `test-first` | `PlanView.test.ts` tab block | new test at `88820839` | `b9e438e5` |

**D3 — the activity rows**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| A link row with no address renders no row; a row's target is judged on the text it renders (after `stripControlSequences`) | met | `test-first` | `PlanView.test.ts` "PlanView activity rows (PLAN-55 D3)" (2 tests) | `82cb7aa0`, same log | `c6a0314f`, `f97ced69` |
| A task row names the task's place whenever a leaf task runs, and the duration only when one is known | met | `test-first` | same block | new assertion at `f97ced69` | `f97ced69` |
| The feed stops when Intent hides the inspector, and the selection survives the tab switch | met | `test-first` | `PlanView.test.ts` (activity effect) | new assertion at `88820839` | `b9e438e5` |

**D4 — the browser check and the design documents**

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| The browser check covers the head, tabs, strip, banner, Intent and Changes at 1440 and 390, light and dark, en and de, with before/after screenshots and accessibility snapshots | met | `none` (browser evidence) | 40 after screenshots `55-{running,waiting,ended,intent,changes}-{1440,390}-{en,de}-{light,dark}-after.png`, 5 before/after accessibility snapshots, 3 first-pass before screenshots — under `.plan/temp/playwright-mcp/` | — | `582972d3` (documents), evidence from the branch app on ports 20550/20551, base app `82cb7aa0` for the before set |
| `doc/design/shared/plan-view.adoc` and `doc/design/web-app/plan-view-interaction.adoc` describe the page as built; the task level is left to PLAN-190 | met | `none` (documents) | rendered pages read against the screenshots | — | `582972d3` |
| The mock carries the ended plan as a process state that reaches its resource | met | `test-first` | `IntentScenarioTest` (state count 42), `ScenarioStateContractTest` | `0ff49dd7` CI run — two pm-mock-runtime contract tests failed on the fixture | `39145484` |
| The browser suite carries the plan page's head, tabs and decision link | met | `mutation-after` | `plan-view.spec.js` head and waiting-plan tests, `intent.spec.js` address test | `9dddbdaa` with `c6a0314f` reverted, `pm-web-e2e/maven-2026-11-25-093050.log` — `plan-view.spec.js:39` "plan-kind … not found", `:96` "plan-attention-strip … not found", `intent.spec.js:48` "plan-panel-host … not found" | `9dddbdaa`, `a0daa000`, `f386bbae`, `ef7ef6cc` |

Order deviation: the e2e clause's evidence is `mutation-after`, not the `test-first` the spec words for that clause — the spec itself asks the revert form for the browser suite, and the log above is that revert. Stated in the PR body as well.

## 2. Personas and skills

- D1 declared `plan-marshall:persona-integration-tester` with `pm-dev-frontend:javascript` and `pm-dev-frontend:javascript-security`; D2 and D3 declared `plan-marshall:persona-implementer` with `pm-dev-frontend:javascript`, `:css`, `:jest-testing` and `:javascript-security`. All loaded as standards before work began.
- `persona-code-reviewer` drove self-review round 1 (15 findings: 1 blocker, 2 major, the rest minor) and round 2 (dispositions verified).
- Surface beyond the spec's Expected Surface, with reasons: `pm-mock-runtime/src/main/resources/scenarios/running-work.json` (D4's mock clause: `decisions.open` and `process.ended-plan`), `pm-mock-runtime`'s `IntentScenarioTest` (the fixture changed the state count), `pm-web-app/src/lib/api/client.ts` (`getPlanIntent`, `getPlanOutline`, the settings payload), `pm-web-app/src/lib/templates/pageWidth.test.ts` (the 720 px line), `pm-web-app/src/lib/components/Link.svelte` (the head actions' advisory), `pm-web-app/scripts/vocabulary-baseline.json` (the new keys), the message catalogues `plan_view.*`, `control.*`, `intent.*` (D1's and D2's text) and the four e2e specs the head change moved (`chrome.spec.js`, `control.spec.js`, `epic-view.spec.js`, `navigation.spec.js`).

## 3. Gates

- Full `verify` on the rebased head (before the last review fixes) green: job `8872f5163cb04f90b30f86d9825eab6b`.
- `pm-web-app` module tests at the final tree green: `/Users/oliver/.plan-marshall/marshalld/job-logs/1c44962f37894158b8468781c1049d73.log`.
- CI at `88820839`: all checks success (Maven Build, Integration Tests, SonarCloud Code Analysis, dependency review); `checks wait` returned `final_status: success`.
- Sonar: configured (`plan-marshall_pm-mcp-clients`); `read_open_issues` at `88820839` returned `count_status: confirmed`, `open_issue_count: 0`, analysis revision equal to the head; the quality gate passed.
- e2e (`-Pe2e`, ports 20550/20551, `in_process` because the pom reads the ports from the environment): `.plan/local/plans/NO_PLAN/build-results/pm-api,pm-mock-runtime,pm-web-app,pm-web-e2e/maven-2026-11-25-112722.log` — 460 passed, 12 failed, every failure pre-existing on the base branch in a file this plan does not touch (`designer.spec.js:264`, `projects.spec.js:112`, 10 theme-readability project-explorer/coverage-guard entries). After the rebase: `…/maven-2026-11-25-141301.log` — 471 passed, 1 failed (`designer.spec.js:264`).
- Review: CodeRabbit, required. First batch (16 inline + 1 nitpick on `39145484`) fixed in `f97ced69` and answered thread by thread; the rebased head drew a second batch (9 inline on `ec9399ba`), all fixed in `b9e438e5` with tests in `88820839` and answered. `review-gate` at `88820839`: `unresolved_review_threads: 0`, body findings 26/26 handled, `merge_allowed: true`. Sourcery is optional and quota-limited (`2026-11-26T11:29Z`); it did not gate.
- `IntentCatalogueTest` is red locally on this machine at the branch base and inside `pm-api` alone, but green in CI at `88820839` (`build (25)` log: "Tests run: 9, Failures: 0") and `main`'s Maven Build was green before the merge — the PLAN-188 warning did not hold at the merge head. Nothing in `intent.*.properties` or `IntentCatalogueTest` was touched here, as the Constraints demand.

## 4. Reported, not changed

- **`OutlinePanel` offers no fragment target.** A criterion's deliverable label in `PlanIntentPanel` stays text because `OutlinePanel` gives its deliverables a `data-testid` but no `id`, so an address would land nowhere. The spec forbids editing the three intent panel files here (PLAN-188 touches `OutlinePanel` on its branch); this is the defect reported for that branch.
- **The queued-revision chat origin belongs to PLAN-137.** D1 asks the head for `control.chat.revision_queued` and nothing more; the chat adoption (the side column, the way into a chat at a turn) is struck from this plan by the Operator Decisions of 2026-11-10 and lives in PLAN-137. The head line names the point it waits for only.

## 5. Cleanup

Worktree `.plan/local/worktrees/55-web-plan-page` removed after a clean status; `feature/55-web-plan-page` deleted locally and its remote-tracking ref pruned; the main checkout pulled to `163e2a49`. `cleanup_owed=false`.

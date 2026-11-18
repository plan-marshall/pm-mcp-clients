envelope_version=1
sender_type=plan
sender_id=rb-164-web-plan-changes-tab
epic=ui-improvements
kind=landing
created=2026-11-18T07:39:29Z

# Landing — PLAN-164 web-plan-changes-tab

```landing-facts
schema=landing-facts/1
plan_id=rb-164-web-plan-changes-tab
epic=ui-improvements
pr=#477
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=13
review_body_findings_handled=13
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## Deliverables

Merged as `744755b5` (PR #477). The spec carries three deliverables: D0 (folded from the PLAN-162 landing,
outside the `## Deliverables` section, so `resolve` listed only the numbered D1 and D2).

**D0 — the mock's first capture** (three files + README)

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| a capture announcement carries the new capture and moves `not_captured`→captured | met | `movesAPlanBeforeItsFirstCapture` | test-first | `5922ca22`, `.plan/temp/rb-164-web-plan-changes-tab/d0-test-first.log` | `c717ab46` |
| a captured plan may turn `unavailable`, with its reason | **not fully met** | `movesACapturedPlanToUnavailable` | test-first | same run; the fix disabled at `c717ab46`, `.plan/local/plans/NO_PLAN/build-results/pm-mock-runtime/maven-2026-11-17-203301.log` | `c717ab46`, later `820fb8a3` |
| files and size change coherently — `/process` and `/changes` are one measurement | met | `servesANewListAndSize` (reads `/process`), `keepsTheLastCaptureSize` | test-first + mutation-after | `5922ca22`; the size-overlay rule re-pinned under review in `048fc99a` | `c717ab46`, `820fb8a3`, `048fc99a` |
| a contradiction is refused, with the matched positive control | met | `refusesAContradictingTotals`, `refusesACaptureOfAnotherPlan`, `refusesGoingBackToNotCaptured`, `refusesBackFromUnavailable` | test-first | `5922ca22` | `c717ab46`, `820fb8a3` |
| a scheduled capture is validated by the loader (carried and head-only, in play order) | met | `validatesAScheduledCapture`, `refusesDecreasingEventDelays` | test-first | `5922ca22`; the sequence rules added under review in `048fc99a`/`4f74c2e4` | `c717ab46`, `048fc99a`, `4f74c2e4` |
| the README states the limit | met | `pm-mock-runtime/README.adoc` | none | — | `c717ab46`, `4f74c2e4` |

**D1 — the client and the Changes view**

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| component tests show the list, a file, a cut file and a binary row | met | `PlanChanges.test.ts` (list, hunks, cut, binary) | mutation-after | `233801.log` (markup under `{@html}`, identity guard removed) | `7289dc06`, `e2c1695f` |
| a line holding markup renders as text, never as an element | met | `PlanChanges.test.ts` "renders a line that holds markup as text" | mutation-after | `233801.log` | `7289dc06`, `e2c1695f` |
| a reload with `?tab=changes&file=` opens the same tab and file | met | `PlanView.test.ts` (address read, size-line no-op keeps the file); e2e `plan-changes.spec.js` (real reload) | test-first (unit) / none (e2e) | `1d12bea1`, `d1-test-first.log`; e2e green at `55564760` | `7289dc06`, `e2c1695f`, `b749fe75` |
| `not_captured` hides the tab; `unavailable` shows its reason | met | `PlanView.test.ts` (both), `PlanChanges.test.ts` | test-first | `1d12bea1`, `d1-test-first.log` | `7289dc06`, `820fb8a3` |
| the file/list race is stated and pinned (Emit Amendment) | met | `PlanChanges.test.ts` "discards a file answer the list moved under", "reads the hunks of the row a person selects, even where two rows look alike", "drops one plan's file answer" | mutation-after | `233801.log` | `e2c1695f`, `b749fe75`, `048fc99a` |

**D2 — `model_differs`**

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| both models pinned, and the head keeps the launch model | met | `PlanView.test.ts` (`activity-model` is exactly `claude sonnet`; live-stream `model-differs` shows both) | test-first (fetched feed) / mutation-after (live path) | `1d12bea1`, `d1-test-first.log`; `233801.log` for the live path | `7289dc06`, `e2c1695f` |

Browser check (files in `.plan/temp/playwright-mcp/`): `plan-changes-01-paired.png`, `-02-list.png`,
`-03-hunks.png`, `-04-reload.png` — the size line opens the tab, the row opens 84 diff lines, the reload keeps
the tab, the file and the selected row; 0 console errors.

Per-head test runs at the merged head `55564760` (tree `744755b5`): `package -pl pm-mock-runtime,pm-web-app -am`
green (954 mock tests, 2961 web tests); `verify -Ppre-commit` green, tree clean; `verify -Pe2e -pl pm-web-e2e`
454 passed / 75 skipped; CI green including SonarCloud; the merge-queue merge-group run green.

## Personas and skills

D0, D1 and D2 declare `Persona: plan-marshall:persona-implementer`; the mock's tests were written as module
tests under `persona-module-tester`, the web component and view tests under the same, and every self-review round
under `persona-code-reviewer`. Named skills: D0 `pm-dev-java:java-core`, `pm-dev-java:junit-core` (folded D0);
D1 `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:css`; D2
`pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`. Surface drift: `pm-web-app/src/lib/stores/runtime.ts`
and `pm-web-app/src/lib/display/index.ts` and `pm-web-app/src/lib/utils/outputSafety.ts` are changed and are not in
the spec's Expected Surface — the spec tells the view to write the tab through the address helpers, and the
shared formatter and the stripping live where the codebase keeps them. Skills added: none needed; the existing
`pm-dev-frontend:*` set covers the files.

## Hypotheses

- D0: the mock moves only the head of a capture the fixture already holds and refuses every other announcement —
  **confirmed**, `MockRuntimeEngine.applyCapturedChange` before the change.
- D1/D2, spec HYPOTHESIS "`PlanView` already reads `tab=`": **refuted** — `PlanView.svelte` had no tab handling;
  the address model parses `?tab=` into `runtimeStore.objectTab` (`routes.ts:81-103`, `:513-525`, `:567-577`,
  `:688-690`; `runtime.ts:573`, `:1278`, `:1308`, `:1443`) and no view consumed it. `PlanView` now reads
  `objectTab` and writes through `selectObjectTab`/`selectPlanChangeFile`, as the spec's rescope requires.

## Findings

Self-review round 1 (code-reviewer sub-agent): H1 vacuous race pin, M2 stale size on `/process`, M3 scheduled
semantics, M4 diff tokens, M5 stripping, M6 unretryable file error, L1-L13. All fixed in `e2c1695f` and
`820fb8a3`; the low items L2, L3, L6, L9, L10, L11, L12, L13 fixed in `e2c1695f`. Round 2 verified them and found
the M2 follow-on (a gone capture losing its size) — fixed in `820fb8a3`. Round 3 ran on the merged diff.

CodeRabbit reviewed at `62b81ccc` (9 findings) and at `d06d818d` (4 findings); every one is fixed and resolved:
`finding: coderabbit inline inline-1 … inline-9 — b749fe75`, `finding: coderabbit inline inline-10 … inline-13 —
048fc99a`. Its follow-up on the scheduled sequence drove the sequential simulation, the non-decreasing `after_ms`
rule and the retained `unavailable` (`048fc99a`, `4f74c2e4`). Sourcery refused on its hard quota and is optional.

Sonar new-code findings were cleared in three rounds: 6 (`f3d1a5b3`), 1 (`62b81ccc`), 0 at `55564760`.

Self-review round 3 (final, on the merged diff) reports **open** findings. At the three-round limit they are
reported as open, not fixed:

- **F1 (medium)** `PlanChanges.svelte:246` — reselecting a row whose read failed shows the previously loaded
  file's hunks (or a blank panel) with no error and no retry, because the `failedIdentity` guard returns before
  the panel is cleared and the error is gone. D1 is delivered but carries this defect in its retry path.
- **F2 (medium)** `MockRuntimeEngine.java:8160-8164` + `ScenarioLoader.java:1095-1100` — a carried `unavailable`
  capture must announce `0/0/0/0`, while the plan keeps and serves its last capture's figures (the design's "the
  tab, which is present then"). The web app adopts the announcement's figures, so the head line reads `0 files`
  for that transition. D0's "files and size coherently" is therefore not fully met.
- **F3 (low)** no test emits `plan.change_captured` into `PlanView`; reverting the subscription, the `plan_id`
  filter or the token bump keeps every suite green.
- **F4 (low)** `PlanChanges.svelte:275-281` — a superseded read's `finally` clears `isFileLoading` for the read
  still open (cosmetic).
- **F5 (trivial)** the `outputSafety.ts` header overclaims ("every control or format character"): the spec's set
  (C1, U+2028/9, the bidi controls, a BOM) is met and tested; U+00AD, U+200B, U+2060-64, U+FFF9-FB and the tag
  block are not stripped.
- **F6 (trivial)** `tokens.test.ts` guards only `--color-text-primary` on the new diff grounds, while the
  component also draws muted line numbers and success/danger counts there.

The operator was asked whether to continue with a fix; the answer is recorded below.

The operator was asked: "The third (final) self-review round on the merged diff reports two medium defects —
F1 … F2 … plus four low/trivial items … How should I proceed?" — options {fix F1/F2 in a follow-up PR now
(Recommended), record as residue and stop}; selected **"Fix F1/F2 in a follow-up PR now"**.

## Reviewers

| Reviewer | Obligation | Result | Head | History |
|---|---|---|---|---|
| `coderabbit` | required (`quota_limited`, `review_complete_after_handling`) | reviewed | `62b81ccc`, `d06d818d` | triggered 00:54, re-triggered 01:04 (refused on quota), 03:40 (accepted after the 90-minute window, review at 03:52), automatic review at 05:10 on the next head; 13 findings, all handled |
| `sourcery` | optional | refused | — | hard quota (250 k diff characters per 7 days), `1 day 11 hours` to the next request; no obligation |

Standing review authorization: `coderabbit` holds a qualifying review with every finding handled, so the review
covers any head of this PR; no further review is owed and none was asked after the last push.

## Operator questions

1. "The executor-skew preflight answered `skew` … How should I proceed?" — options
   {regenerate executor and continue (Recommended), stop the run as blocked}; selected "Regenerate executor and
   continue".
2. "The spec's Standing Constraints require the local `-Pe2e` run with the plan's ports … How should I run it?" —
   options {run E2E in-process with the plan's ports (Recommended), skip the local E2E run}; selected "Run E2E
   in-process with the plan's ports".
3. "The third (final) self-review round on the merged diff reports two medium defects … plus four low/trivial
   items … How should I proceed?" — options {fix F1/F2 in a follow-up PR now (Recommended), record as residue and
   stop}; selected "Fix F1/F2 in a follow-up PR now".

No free-text operator messages.

## Rule check

`guard: none on this harness (unenforced)` — the resolve result reported no tool-call guard for OpenCode, so no
guard log line exists for this run and "all kept" is not claimed.

The run's own actions, counted: every repository edit and every file read went through the file tools except the
following shell reads, which this record owns — `grep -c "BUILD SUCCESS"`, `wc -l` on two files, `tail`/`head` on
the build logs, `find pm-web-e2e -name` and `ls` on `test-results`, and one `grep -E … | head` pipeline early in
the run. The lane forbids shell file reads; they were used to read build evidence and were not repeated as a
habit. One edit was made to the main checkout by mistake
(`pm-mock-runtime/src/test/java/de/planmarshall/mock/PlanChangesScenarioTest.java`) and reverted with the file
tools before anything else ran; `git status --porcelain` in the main checkout was empty afterwards. Every build
went through the resolved `build` commands except the deliberate E2E routing the operator selected. No `sleep`,
no CI CLI, no `python3 -c`, no repository script run by path. One operator-visible restart: none; two sub-agent
rounds and one final round, no killed work. The spec was read from the ledger worktree (`spec_source:
ledger_worktree`), no `spec_differ` marker. `executor-skew` verdict this run: `skew` (regenerated under the
operator's decision), then `match` at `0.1.2149`.

## Build server

Setup preflight: `ready`. Builds: `daemon_longpoll` for every Maven build except the E2E suite, which ran
`in_process` on the operator's decision (the daemon cannot carry `PM_MOCK_WEB_PORT`/`PM_WEB_PORT`); no degraded
fallback and no launch-fault retry. Two live `running` submissions were re-attached with `build_server wait`
(one queued behind another plan's builds for ~18 minutes), and one E2E run hit the explicit 827 s bound and was
re-submitted at 1800 s. No `killed` job. `runtime_stamp_reason`: none.

## Residue

- The spec sits in the orchestrator ledger worktree and main's copy; the plan's queue row still says `launched`.
- `plugin_version` resolved as `unknown` on the runtime stamp; `total_tokens` was not measured by the harness.
- The Changes tab is mounted in today's `PlanView`; PLAN-55 carries it into the full tab set.
- No base-evidence worktree was used (the D0 revert ran on the plan's own worktree), so nothing is owed there.

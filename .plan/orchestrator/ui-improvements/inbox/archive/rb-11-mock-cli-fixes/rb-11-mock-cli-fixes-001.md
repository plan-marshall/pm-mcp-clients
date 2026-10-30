envelope_version=1
sender_type=plan
sender_id=rb-11-mock-cli-fixes
epic=ui-improvements
kind=landing
created=2026-10-29T22:22:47Z

```landing-facts
schema=landing-facts/1
plan_id=rb-11-mock-cli-fixes
epic=ui-improvements
pr=#269
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

### D1 — Plan details per id
- **DW1 — met, order `test-first`.** `PlanDetailTest.servesThePlanPerId`. Failing at `53718209` (job `2b6707ef619a4f40963714147425d447`, `maven-2026-10-29-111924.log`: title `drop-legacy-api`, phase `execute`, state `running`); green at `59552b82`. Later re-pointed to the started-plan and held-plan cases in `9731069e`, `6bc4edb4`, `f86f4846`.
- **DW2 — met, order `none`.** Playwright `running-work` → `/plans/nope`: before (a plan) vs after (`Plan not found`), 8 locale/theme/width snapshots each + `11-unknown-plan-{before,after}.png` under `.plan/temp/playwright-mcp/`. `reports-and-system` → `system.draining` shows the draining banner (`Stopping when what runs is done · 2 left`, `noch 2`); before had no banner. 8+8 snapshots + 2 png.

### D2 — Routes and lifecycle state
- **DW1 — met, order `test-first`.** `ScenarioStateContractTest.everyScenarioAnswersThePlanList` and `FixtureConsistencyTest.keptItemKindIsFromTheClosedSet`. Failing at `53718209` (job `287d606bcd71479a8765ce3652dccc61`: onboarding `404` on `/api/v1/plans`; kept kind `item`); green at `a6ff4e8c`. `everySystemStateServesItsLifecycleState` was green at base: the spec's re-grounding frames the lifecycle half as a check (`system.restarting` → `upgrading`, the closed enum has no `restarting`), and the test now pins every `system.*` state and the draining blocker count.
- **DW2 — met, order `none`.** Same draining evidence as D1.2, re-captured at `fd171392` and unchanged at the merged head.

### D3 — README socket name
- **DW1 — met, order `none`.** `pm-mock-runtime/README.adoc:18,104` name `run/runtime.sock`; `MachinePaths.socket()` and the mock's start output name the same path. The root and operator READMEs corrected too.
- **DW2 — met, order `none`.** Open Sonar read at the head `b6701025`: `count_status: confirmed`, `open_issue_count: 0`, analysis revision = head revision. No dismissal needed.

Per-head test runs: the gate canonical ran at `92fcae2b`, `763eb49`, `73b562a1`, `293c7ecc`, `fd171392`, `d03c8790`, `b6701025`; `verify` green at each. `module-tests` is not published by this project's architecture.

## Personas and skills

- D1, D2: `plan-marshall:persona-implementer`; skills `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:junit-core`.
- D3: `plan-marshall:persona-documenter`; skills `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- Every self-review round and PR-comment triage: `plan-marshall:persona-code-reviewer`.
- Surface drift: `pm-dev-frontend:javascript` added for the `pm-web-app` changes (`PlanView.svelte`, `client.ts`, `runtime.ts`), reason: the D1/D2 browser clauses require the client to render "not found" and the draining lifecycle.
- `skills: none named` for any step without a declared line.

## Hypotheses

- "the mock returns the same plan for every plan id" — confirmed, `MockRuntimeEngine.java:8308-8323` at `53718209`.
- "no routes for `/api/v1/config/<aspect>` and `/api/v1/roles`" — confirmed (grep empty).
- "`system.restarting` exists and carries `lifecycle_state: upgrading`" — confirmed, `reports-and-system.json:2820-2845`; `/api/v1/status` serves the goto facts (`MockRuntimeEngine.java:5326`, `:6565`).
- "a kept cleanup item carries `kind: item`, outside `CleanupKind`" — confirmed, `MockRuntimeEngine.java:7708` at `53718209`.
- "the mock binds `run/runtime.sock`" — confirmed, `MachinePaths.java:126` and the start output; the README said `pm-mcpd.sock`.

## Findings

- Self-review round 1: synthesized plan served another plan's epic/PR/tree; unknown id `200` on the activity route; class doc mismatch — fixed `763eb49`. Refuted: duplicated test parsing, `CleanupView` raw kind (out of scope).
- Round 2: activity route reported `docs-typo` unknown; draining banner "0 left"; kept-item test vacuous; root/operator README socket — fixed `73b562a1`.
- Round 3: `control.spec.js` used `relay-cursor-order`, broken by the new 404 — fixed with its own route in `fd171392`; dead `draining_by` branch, third facts lookup, `remaining_work_count` vs `drain_remaining_count`, template `project` fallback, imprecise comment — fixed `293c7ecc`/`fd171392`.
- Confirmation round: fixture drift from listing `relay-cursor-order` — fixed by the static route (`fd171392`); nit comment fixed.
- Sonar: 6 → 1 → 2 → 0 issues across `fd171392`/`5dbe9aa6`/`6bc4edb4`/`f86f4846`; all fixed (`5dbe9aa6`, `2fabdbe2`, `f86f4846`).
- CodeRabbit: started plans 404; planned `run_count`; `isStopped` not cleared; `knowsPlan` status; `uniquePlanId` shadowing fixture ids; adopt title; kept-item id/title; non-running node state; template-absent phase; held without template; drain payload — all fixed in `9731069e`, `6bc4edb4`, `f86f4846`, `65d201ef`, `d03c8790`; every inline thread resolved. Answered, not fixed: the drain-reconcile race after the event stream connects (needs a connection-established signal the SSE client does not expose).

## Reviewers

- **coderabbit** (required, `quota_limited: true`): reviewed `2fabdbe2` (17:39), `71b86c65` (18:33), `f86f4846` (19:45), `65d201ef` (20:46). Quota refusals at 16:29 ("next in 23 min"), 18:41 ("already reviewed"), 19:19 (rate limited), 21:29 (rate limited). Standing review authorization: qualifying review on `65d201ef` with `kind=reviewed`, every finding fixed/answered and every thread resolved, re-triggered after the merge push (rate-limited), all other gates green on `b6701025`.
- **sourcery** (optional, `quota_limited: true`): hard quota, next review in ~4 days; not gating.

## Operator questions

1. "The executor is older than the lane text… How should I proceed?" — options: regenerate now, stop blocked, regenerate via `/marshall-steward`. Selected: **Regenerate the executor now**.
2. "The self-review has used its 3 rounds… How should I proceed?" — options: run one confirmation round, proceed with budget-exhausted. Selected: **Run one confirmation round**.

No merge question was asked: the standing authorization applied. Free text: none.

## Rule check

- `guard: none on this harness (unenforced)`; no guard log to read.
- Shell rule: a few read-class file operations were issued through the shell (`grep`, `tail`, `ls`) and two commands joined with `;`/a pipe while inspecting build logs and PR output. They were read-only and changed no repository file. Everything else used the file tools, the executor, and the `build`/`ci` commands.
- No git stash/restore/rebase/force-push; no stage-all; no edits outside the worktree; no `.plan/orchestrator/` write.
- Spec read: `resolve` read the ledger worktree copy (`spec_source: ledger_worktree`) at setup and at landing; no `spec_differ` marker.
- `executor-skew`: `skew` (0.1.2028 vs 0.1.2032) → operator-selected regeneration → `match` at 0.1.2032.

## Build server

- Preflight at setup: `ready`; fallback: none.
- Builds ran routed (`auto`); a routed build needs an absolute `--project-dir` (a relative one returned `execution_failed`).
- One bound expiry: the quality gate at `92fcae2b` hit the 822 s daemon bound; re-run with `--timeout 1800` and green. No `killed` jobs.
- `runtime_stamp_reason`: none.

## Residue

- The drain-reconcile race after the event stream connects (CodeRabbit follow-up) is answered, not fixed: the status read is the load-time truth, and closing the gap needs a connection-established signal the SSE client does not expose.
- A drain started through `POST /api/v1/operator/runtime/stop` leaves the status without a `drain_remaining_count` (only the `goto` state path carries it).
- A paused plan's synthesized current node is `waiting_for_operator`; `NodeState` has no paused value.
- The draining banner's `what` stays generic ("running work").
- `lifecycleState.name().toLowerCase()` is default-locale (pre-existing).
- The synthesized plan's activity body is the template's for a listed plan without its own activity fixture.

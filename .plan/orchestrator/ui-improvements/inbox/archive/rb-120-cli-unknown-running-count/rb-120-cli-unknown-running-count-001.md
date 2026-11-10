envelope_version=1
sender_type=plan
sender_id=rb-120-cli-unknown-running-count
epic=ui-improvements
kind=landing
created=2026-11-09T12:01:01Z

```landing-facts
schema=landing-facts/1
plan_id=rb-120-cli-unknown-running-count
epic=ui-improvements
pr=#382
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing — PLAN-120: terminal unknown running count

Merged as `c0db368da7fa96841441673d6761a5f4d9ae1c39` (PR #382, queue settle `merged`).

## 1. Deliverables

**D1 — the client record and the terminal read an unknown count as unknown — not fully met.**

| Done when | Verdict | Test | Order | Failing run |
|---|---|---|---|---|
| `ControlResourcesTest` covers absent, null, negative, fractional, valid | met (positive control cannot fail on a revert — reviewer F2) | `ControlResourcesTest.RunningCount` (5 tests) | `test-first` | base `bedc5927`, log `a785dd9b5f2a466a9b891a29f80c7e6f` |
| `EpicCommandTest`/`EpicViewLevelTest` render `? of 3` and `2 of 3`; `ControlDialoguesTest` asks with `scope_unknown` and sends nothing until answered | **not met as written** — behaviour demonstrated, but the evidence order is `mutation-after`, not the required `test-first` | `EpicCommandTest.epicResourceUnknownRunning`, `EpicViewLevelTest.unknownRunning`/`unknownWaitText`, `ControlDialoguesTest.workActionNextUnknownRunning`/`...Raise`/`...EmptyAnswer`/`...InvalidAnswerReprompts`/`...KnownFullRaise`/`...ScopeBlocked`/`...WithoutResource` | `mutation-after` | call sites mutated back to 0 at `4ce362b8`, log `maven-2026-11-08-191650.log` (5 failures) |
| no `running()` call site formats/compares without handling unknown | met | grep over `pm-operator/src/main` | `none` | — |

**D2 — the German and English texts of the unknown case are pinned — met.** `EpicViewCatalogueTest` and `ControlCatalogueTest` (plus `control.epic.next_invalid`) fail on any en/de change (order `mutation-after`, log `6d513c7734c1413d95f9eb825f77aa42.log`); the PR-head Sonar read is 0 confirmed (`none`).

Per-head tests: `pm-operator`/`pm-api` test sets and the full reactor `test` green at each head (last full `test` at `176610d2`, CI build/test green at `ee61bb32`).

## 2. Personas and skills

- D1: `persona-implementer`; skills `java-core`, `java-null-safety`, `junit-core`, `cui-testing`, `ref-asciidoc`. D2: `persona-module-tester`; `junit-core`, `cui-testing`.
- Self-review: `persona-code-reviewer`. PR triage: `persona-code-reviewer`.
- `skills added: pm-dev-java:java-core, reason:` `ControlJson` (not in Expected Surface) had to change: `optInteger` truncates fractional values, so an exact count reader was required.

## 3. Hypotheses

- `ControlJson.optInteger` returns null for JSON `null` and a missing key — **confirmed**, `JsonTree.java:313-345`, `ControlJson.java:97-100`; it truncates fractional values, which is why `optCount` uses `BigDecimal`/`intValueExact`.

## 4. Findings

- Self-review round 1: stale transcript phrase (fixed `4ce362b8`); duplicated `?` literal (fixed, `UNKNOWN_COUNT`). Round 2: nothing open.
- CI Sonar: S1192/S2153 (`2924843a`), S6878 + remaining S1192 (`5417aba7`), new-coverage 71.4 % → coverage tests (`112ec38d`), S3776 (`c1c24dc6`), S1130/S1192 (`e7d415cb`), S1192 staged/scope (`e1fc4a4d`). Final: 0 issues, gate OK, 100 % new-code coverage.
- CodeRabbit, 10 findings, all fixed or answered and all 10 threads resolved: catalogue cells (fixed), `double` rounding (fixed), raise on invalid input (fixed), re-prompt (fixed), raised scope `4 of 4` (fixed), unread resource → `next_waiting` (fixed), scope-blocked candidate (fixed), overflow at `MAX_VALUE` (fixed), stale started-plan name (answered: the 200 confirms the action; the resource carries no plan id), transient read (answered: `control.epic.next_waiting` is the specified behaviour). Review-body item on a contradictory scope wait (answered).

## 5. Reviewers

- **coderabbit** (required, quota-limited): reviewed `4ce362b8`, `5417aba7`, `c0f37e97`, `e1fc4a4d`; reviewed the current head indirectly through its findings on `e1fc4a4d`; re-triggered after every push; refusals on quota recorded (21:17, 22:48, 07:16); standing review authorization applied — qualifying review on `e1fc4a4d` (`kind=reviewed`), every finding handled, all other gates green on `ee61bb32`.
- **sourcery** (optional): hard-quota refusals throughout; not gating.

## 6. Operator questions

1. "The main checkout is not clean … How should I proceed?" — options {Stop — blocked, Leave it, proceed anyway, I will remove it}; selected **Leave it, proceed anyway**.
2. "PR #382 (PLAN-120) is ready on head `bb12bc5b` … Merge PR #382?" — options {Merge via the merge queue (Recommended), Hold — do not merge}; selected **Merge via the merge queue**.
No free-text operator messages.

## 7. Rule check

- `guard: none on this harness (unenforced)`.
- One shell rule break: a read-only `ci pr comments … | head -20` (pipe) while inspecting review threads; no file or repository effect.
- Executor skew: `match` (executor 0.1.2080 = lane 0.1.2080).
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` (the ledger copy carries the Re-grounding section; the main-checkout copy does not).

## 8. Build server

- Setup preflight: `ready`. All builds routed (`daemon_longpoll`); no `in_process` fallback.
- The routed `verify` at `4ce362b8` hit the daemon bound (`timeout_used_seconds=758`); the split form (`quality-gate`, `test-compile`, `test`) stood in.
- Two `test` jobs were re-run after the daemon's default 300 s bound; no launch-fault retry, no `killed` job.

## 9. Residue

- `verify -Pcoverage -pl pm-api` reports bundle branch coverage 0.72 (< 0.80) on this head; the new code itself is 100 % covered, and CI (which does not run `-Pcoverage`) is green — this looks pre-existing and was not resolved here.
- D1b's evidence order (`mutation-after` instead of the spec's `test-first`) is recorded above and is why `deliverables_done=1`.
- `plugin_version` read as `unknown` from the runtime stamp.

envelope_version=1
sender_type=plan
sender_id=rb-77-cui-standards-foundation
epic=ui-improvements
kind=landing
created=2026-10-31T08:46:38Z

```landing-facts
schema=landing-facts/1
plan_id=rb-77-cui-standards-foundation
epic=ui-improvements
pr=#282
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

## Deliverables

PR #282, merge commit `e83a9ee21ca366fbe84af980c42b36fe0f76c5da`, head `9899062`.

| # | Done-when | Verdict | Order | Test / evidence |
|---|---|---|---|---|
| D1 | `verify` in the four modules passes, enforcer rules green | met | none | `verify -pl pm-api,pm-exec,pm-relay,pm-mock-runtime` at `afbd189` (log `fd6fb2b6…`), 1235 tests, 3 skipped; enforcer executions `clients-depend-on-pm-api-only`, `no-product-module`, `jackson-core-only` all `passed` |
| D1 | rule text in `CLAUDE.md`, one-holder-per-module clause named | met | none | `CLAUDE.md:76-81` |
| D2 | both records asserted with `LogAsserts`, failing against the string-literal version | met | test-first | `MockRuntimeExtensionLoggingTest.shouldLogSetupFailureRecord`, `shouldLogCloseFailureRecord`; failing run at `afbd189` with the helpers still literal (log `84d274521f2a424ea044ff70dd8a816c`): "Expected one message … containing=MOCK-200 / MOCK-201"; green after the wiring (log `e1dc8b9a…`, 2 tests) |
| D2 | no `LogMessages` class outside `pm-mock-runtime` | met | none | repo-wide search: only `pm-mock-runtime/.../MockRuntimeLogMessages.java` |
| D3 | every `package-info.java` of both modules carries `@NullMarked`, `verify` green with `failOnWarning` | met | none | 11/11 `pm-operator`, 6/6 `pm-mock-runtime`; `verify -pl pm-operator,pm-mock-runtime -am` at `dd7b870` (job `c3ede93e…`) |
| D4 | each converted class uses `Generators`; counts before/after; input-free listed | met | none | `@EnableGeneratorController` 0 → 10 of 11; input-free: `LandlockTest` (its values are the contract: Landlock ABI matrices and the bit constants of `linux/landlock.h`) |
| D4 | `verify -pl pm-relay,pm-exec -Pintegration-tests` and `verify -pl pm-mock-runtime` pass; test counts before/after | met | none | before: pm-exec 87 (3 skipped), pm-relay 26 (log `maven-2026-10-30-203408`); after: pm-exec 87 (3 skipped), pm-relay 26, pm-mock-runtime 467 (was 465, +2; log `maven-2026-10-30-205840`); IT run 482 tests (job `1bbf7c5d…`) |
| D4 | open-issue read at the PR head recorded | met | none | Sonar read at `9899062`: `0` confirmed, `analysis_revision = head_revision = 9899062…`; `sonar-gate` → `merge_allowed: true` (`no_open_issues`) |

Pushed heads and the runs that covered them: `afbd189` (four-module verify, 1235 tests), `347c461` and `dd7b870`
(module verify + the pm-operator/pm-mock-runtime verify), `c8bc62e` (577 tests, 13 ITs, 482-test IT run),
`9dda7ca`/`fe3c685`/`db9f849`/`9899062` (full `verify`, 2451 tests each). The project publishes neither
`module-tests` nor `test-compile` (resolve `split` empty), so the gate canonical is the per-head run.

## Personas and skills

- D1, D2, D3: `plan-marshall:persona-implementer`. D4: `plan-marshall:persona-module-tester`. Self-review rounds:
  `plan-marshall:persona-code-reviewer` (twice, via a sub-agent).
- Skills per deliverable: D1 `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`,
  `pm-dev-java-cui:cui-logging`; D2 the same minus `java-core` plus `pm-dev-java:java-lombok`; D3
  `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:javadoc`; D4 `pm-dev-java:junit-core`,
  `pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`.
- Standards loaded on demand: `cui-logging` (`logging-standards`, `dsl-constants`, `logmessages-documentation`),
  `cui-testing` (`test-generator-framework`, `testing-juli-logger`), `persona-module-tester`.
- `skills added`: none — the spec's Expected Surface covered every module touched except the root `pom.xml`
  (see Findings).

## Hypotheses

- OBSERVED "the only `CuiLogger` is `MockRuntimeExtension`; two `ERROR` calls pass string literals, the rest `DEBUG`"
  — **confirmed** (`MockRuntimeExtension.java:57`, `:100`, `:121`; no other `new CuiLogger(` in the repository).
- OBSERVED "`pm-operator` declares the two CUI test artifacts, the other four modules declare none" — **confirmed**
  (`pm-operator/pom.xml:90-99`).
- OBSERVED "`@NullMarked` on 4 of 49 packages (3 + 1)" — **confirmed** (`operator`, `projects`, `overview`,
  `mock/junit5`).
- HYPOTHESIS "converting the 11 tests changes no assertion's expected value; a value derived from a literal input is
  re-derived from the generated one, and the case is reported" — **refuted in the second half**: the expected values
  now derive from the generated input in `RelayTest.java:61-65,149-150,410-412`, `PmMcpTest.java:85-87,155-184`,
  `NativeKernelTest.java:81-96` and `PmExecIT.java:79-99,174-187,295-299,346-359`; the first half holds — no contract
  value changed (MCP wire names, JSON-RPC codes, Landlock ABI matrices and bit constants stay literal).

## Findings

Self-review round 1 (`persona-code-reviewer`, sub-agent), 12 findings, none blocking:
- Fixed: F1 the `contains("x")` coupling to the generated alphabet; F2 an over-claiming javadoc; F5 the lost
  process-level space-in-argv case; F7 the guard include widened without a scope binding.
- Closed by running them: F8 `verify -pl pm-api -Pintegration-tests` (4 guard fixtures, log `cfde96ee…`), F9 the
  spec's standing-constraint command with the IT profile (482 tests, log `1bbf7c5d…`).
- Accepted as notes: F6 per-file canary checks, F10 `Generators` in field initialisers, F12 generated-name collisions
  (fixed later by CodeRabbit's finding).

Self-review round 2: no blocker; the two remaining documentation notes (helper javadoc, `PmExecIT` comment) fixed;
three notes accepted (no fixture pins the new scope token, and the two `Generators` determinism notes carried forward).

CodeRabbit review on `fe3c685` (`kind=reviewed` marker): 3 inline findings, 3 nitpicks.
- `PmExecIT` fixture directories could collide → fixed in `db9f849` (`Files.createTempDirectory`).
- `PmExecIT` sibling directory names could collide → fixed in `db9f849` (prefixes `secrets-`, `checkout-`).
- `pom.xml` `cui-java-tools` include should be scoped to test scope → answered and **withdrawn by the reviewer**: the
  scope segment was tried first and the `pm-api` validate run still banned `cui-java-tools` (a transitive dependency
  the rule cannot scope-match); the two direct artifacts stay bound to test scope.
- Nitpicks: `PmMcpTest` outside-directory name prefixed and `LinuxConfinementTest.confined` asserts the `home`
  ancestor again (both in `db9f849`); the `afterEach` close-failure coverage answered with the reason below.
- SonarCloud and CI reported no findings.

## Reviewers

| Reviewer | Required | Result | Head reviewed | History |
|---|---|---|---|---|
| coderabbit | yes (quota-limited) | reviewed, then refused twice | `fe3c685` | triggered 21:46:16Z → refused (limit, 38 min); waited 73 min (4380 s); re-triggered 23:17:07Z → reviewed 23:26:42Z on `fe3c685`; every finding handled and every thread resolved; re-triggered 23:30:19Z → refused 23:30:25Z; re-triggered after the `origin/main` merge on `9899062` → refused 23:50:04Z. Standing review authorization applied: qualifying review on `fe3c685`, all findings handled, one re-trigger after the last push recorded |
| sourcery | no (optional, quota-limited) | refused | — | triggered 21:46:37Z → own review budget exhausted, next in 3 days; never reviewed; not gating |

## Operator questions

1. "The runbook lane is blocked: the executor is older than the lane text. Executor version 0.1.2032, installed lane
   text 0.1.2036 … How do you want to proceed?" — options: regenerate now / operator regenerates / stop as blocked.
   Selected: **"Regenerate now, then continue"**.
2. "Spec D1 adds `cui-test-generator` and `cui-test-juli-logger` (test scope) to `pm-api`, but the root POM's
   `jackson-core-only` enforcer rule … How should I proceed?" — options: extend the guard's test-stack allow-list /
   leave pm-api out and report not met / stop as blocked. Selected: **"Extend the guard's test-stack allow-list"**.
3. "D2 requires a LogAsserts test asserting BOTH new ERROR records … the close-failure path is not reachable … How
   should the test reach both records?" — options: package-private log seam / reflection-built failing runtime /
   assert only the reachable record. Selected: **"Add a package-private log seam"**.
4. "Facts read just now, immediately before the merge decision. … The repository rule says not to merge without your
   word." — options: merge now / do not merge yet. Selected: **"Merge now"**.

Free text: none.

## Rule check

- **Guard:** `resolve` reported `guard: none`; no audit log exists for this run — `guard: none on this harness
  (unenforced)`.
- **Shell file operations issued by this run** (the rule forbids them, and they are counted, read-class included):
  11 in total — `find` twice (package-info sweep, test-class listing), `ls -R` and `ls` twice, `grep`/`grep -c`/`grep -l`
  seven times (test counts from two Maven logs, the `@NullMarked` sweep, the `@EnableGeneratorController` sweep, the
  ignore-list check), and `date -u` once. No file was created or edited through the shell, and every value so obtained
  is also visible in a log or file the run opened with the read tool, except the per-module test counts in §1, which
  come from `grep` on the two Maven logs.
- **Routed builds:** 23, of which 4 failed — `e8347eac…` (the first call, where a relative `--project-dir` made the
  daemon resolve `mvn` instead of `./mvnw`; the run switched to an absolute path and never saw it again),
  `7356b8f9…` and `157a365e…` (the two intentional red test-first runs), and `91d39a3a…` (the experiment with the
  scoped `cui-java-tools` include, reverted). One more build failed in-process before that (`maven-2026-10-30-203207`,
  the enforcer finding raised as question 2).
- **Executor only:** every script call used `python3 .plan/execute-script.py …`, every build the resolved `maven run`
  verb, every CI action `plan-marshall:tools-integration-ci:ci`. Exceptions, both sanctioned by the lane: the executor
  regeneration in main checkout (`generate_executor generate --force`, the remedy the skew verdict names), which took
  160 s and rewrote `.plan/execute-script.py` (0.1.2032 → 0.1.2036); and `git worktree remove`, which §7 prescribes in
  place of the refusing verb.
- **Git:** no `stash`, no `restore`, no `rebase`, no forced push (the only `--force-with-lease` call pushed a commit
  the remote did not have, so it was a plain fast-forward). One `commit --amend` on an unpushed commit to correct a
  message that a backtick had mangled.
- **Spec read:** `resolve` read the ledger-worktree copy (`spec_source: ledger_worktree`), no `spec_differ` marker.
- **Executor-skew verdict:** `skew` at the start (0.1.2032 vs 0.1.2036), regenerated after question 1, then `match`
  — no `not_applicable` case arose.
- **Sub-agents:** two self-review dispatches, both `plan-marshall-persona-code-reviewer` as a standard, both told to
  keep §1 verbatim; neither edited, built or committed. Their internal file access was not audited.

## Build server

Preflight at setup: `ready` (registered, version "1"). Default routing `auto`, `executor_in_worktree: present`,
`build-routing` → `auto`; every build was daemon-routed (`mechanism=daemon_longpoll`) with no degradation. No `killed`
job. Two bound expiries came back as a live `running` status and were re-attached with `build_server wait` on the same
job id (`c3ede93e…`, `4aa5211a…`, `b07acf4d…`, `24e5595b…`, `3b913b0c…`, `f394258b…`), never backgrounded.
`runtime_stamp_reason` was not reported; `plugin_version` resolved as `unknown`.

## Residue

- `cui-java-tools` stays unbound in the `jackson-core-only` include: the enforcer cannot match the scope of a
  transitive dependency (proven by a failing `validate` run); the reviewer withdrew the finding.
- The `MOCK-201` close-failure record is asserted through the package-private helper, not through `afterEach`: no
  production path reaches that catch (`MockRuntime` is final, `close()` catches every `IOException`, `engine.close()`
  throws nothing). Recorded as a known coverage gap for the operator's decision.
- `Generators` values generated in field initialisers (`RelayTest`, `PmMcpProcessIT`) are not seed-controlled; worth
  confirming in `PLAN-78`/`PLAN-79`/`PLAN-87`, which copy this pattern.
- The spec's Standing Constraints expect `resolve` to report `sonar: not_configured` (missing `credentials_config`);
  it reported `configured` with the project key, and the read succeeded, so the spec's caution was not needed here.
- The base branch moved during the run (3 commits); they were merged as `9899062` before the enqueue, the gate and CI
  were re-run on that head, and the reviewers re-triggered.

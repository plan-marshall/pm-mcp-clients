# Landing Analysis: PLAN-77 — Java: CUI Standards Foundation (Dependencies, Logging, Null Safety, First Test Conversions)

epic: ui-improvements
workstream: WS-01
pr: #282 (merge commit `e83a9ee21ca366fbe84af980c42b36fe0f76c5da`, squash merge)

> Landing record for one shipped plan. Source: inbox message `rb-77-cui-standards-foundation-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`), analysed 2026-10-31 against the merged PR,
> the CI checks, the merge diff and the Sonar gate. The per-head build logs and the two self-review rounds are the run's
> account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Dependencies and the rule | shipped-as-specified, one operator decision | `cui-test-generator` and `cui-test-juli-logger` (test scope) added to `pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime` (10 lines each); `CLAUDE.md` carries the rule (6 lines, one-holder-per-module clause named). `verify` over the four modules passes (1235 tests, 3 skipped); the enforcer executions stay green. The root POM's `jackson-core-only` rule banned the new test artifacts in `pm-api`; the run asked, and the operator chose to extend the guard's test-stack allow-list (root `pom.xml` +8/−3 lines). |
| D2 Logging in `pm-mock-runtime` | met | `MockRuntimeLogMessages` (new, 56 lines, the one holder of the module); `MockRuntimeExtension` logs two `ERROR` records (`MOCK-200`, `MOCK-201`) through it. `MockRuntimeExtensionLoggingTest` asserts both with `LogAsserts`; `test-first`: it failed against the string-literal version ("Expected one message … containing=MOCK-200 / MOCK-201") and passed after the wiring. A repository-wide search finds no other `LogMessages` class. |
| D3 Null safety markers | met | `@NullMarked` on all 11 `package-info.java` of `pm-operator` and all 6 of `pm-mock-runtime` (13 files added: 8 + 5); `verify` green with `failOnWarning`. |
| D4 Tests of pm-relay and pm-exec (pilot) | met, one hypothesis half-refuted | `@EnableGeneratorController` went from 0 to 10 of 11 classes; `LandlockTest` is listed as input-free (its values are the contract). Test counts: pm-exec 87 (3 skipped) before and after, pm-relay 26 before and after, pm-mock-runtime 467 (was 465, +2 from the logging test), the integration run 482. The HYPOTHESIS "no assertion's expected value changes" is true for contracts (wire names, JSON-RPC codes, Landlock matrices stay literal) and refuted for derived values: expected values now derive from the generated input in `RelayTest`, `PmMcpTest`, `NativeKernelTest` and `PmExecIT`, as the spec allowed. |
| D4 open-issue read | met | `open_issue_count=0`, `open_issue_rules=none`, analysis revision = head revision; corroborated below. |

`deliverables_done=4` of 4 is consistent with the diff.

**Surface delta** (computed here from `git diff e83a9ee2^1 e83a9ee2`, because `inbox landing-check` answers
`unmeasured`): declared 23 entries, realized 33 files. Twenty-two declared entries were changed (`CLAUDE.md`, the four
module POMs, `MockRuntimeExtension`, `MockRuntimeLogMessages`, the five `pm-mock-runtime` and the eight `pm-operator`
`package-info.java`, and the test directories of `pm-exec` and `pm-relay`: seven and three files). One declared entry is
untouched: `MockRuntimeExtensionTest.java` (the run wrote the logging test beside it). Three files are undeclared: the
root `pom.xml` (the enforcer allow-list), `pm-mock-runtime/README.adoc` (13 lines) and the new
`MockRuntimeExtensionLoggingTest.java`. The spec named every module POM and none of the root POM.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness, recorded as unobserved, not as zero). `plugin_version` `unknown`.
  `executor_version` `0.1.2036`.
- Anomalies:
  - **Executor skew, resolved by regenerating:** `skew` at the start (`0.1.2032` against `0.1.2036`). The operator chose
    "Regenerate now, then continue" in this run (a second operator answer after PLAN-80's "already regenerated"); the run
    regenerated the executor in the main checkout (`generate_executor generate --force`, 160 s), the remedy the verdict
    names, and the re-check read `match`. It rewrote the untracked `.plan/execute-script.py` there.
  - **A guard conflict the spec did not foresee:** the root `jackson-core-only` enforcer rule forbade the new test
    artifacts in `pm-api`. Three options were put to the operator (extend the allow-list, leave `pm-api` out, stop); the
    first was chosen. The run tried a scoped `cui-java-tools` include and reverted it, because the rule cannot match the
    scope of a transitive dependency (proven by a failing `validate`).
  - **A reachability problem the spec did not foresee:** the `MOCK-201` close-failure path cannot be reached by
    production code. The operator chose a package-private log seam, so the record is asserted through the helper and not
    through `afterEach`.
  - **Self-review, two rounds** (12 findings then none that blocks; `self-review: clear`). Fixed: a `contains("x")`
    coupling to the generated alphabet, an over-claiming Javadoc, a lost process-level case, an unscoped guard include.
  - **Builds:** 23 routed builds, all through the daemon (`mechanism=daemon_longpoll`, no fallback); 4 failed, two of
    them the intended red test-first runs and one a relative `--project-dir` the daemon resolved to `mvn` instead of
    `./mvnw` (fixed by an absolute path). Every build ran on the daemon with no in-process fallback, so the bare-`mvn`
    failure of the open Build-server watch did not occur in this run. Two bound expiries were re-attached with `build_server wait`.
  - **Shell file operations by the run itself:** eleven read-class calls (`find`, `ls`, `grep`, `date`); none created
    or edited a file. Guard `none` on this harness, so the count is the run's own.
  - `spec_source=ledger_worktree`, no `spec_differ`.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) refused twice, waited 73 minutes, then reviewed `fe3c685` (three inline
  findings, three nitpicks; the generated fixture directory names in `PmExecIT` fixed in `db9f849`, the `cui-java-tools`
  scope finding answered and withdrawn by the reviewer); it refused again on the later heads. The merge head `9899062`
  differs from the reviewed head by those fixes and the `origin/main` merge; the merge ran under the standing review
  authorization, and the operator was asked immediately before the merge ("Merge now"). Sourcery (optional) refused on
  quota.
- CI/merge: `ci checks status --pr-number 282` is `success`, 20 checks. `origin/main` carries `e83a9ee2`.
- Sonar: `sonar_rest gate-status --project plan-marshall_pm-mcp-clients --pr 282` answers `gate_status: OK`, six
  conditions OK (new coverage 93.8, duplicated lines 0.0, hotspots reviewed 100). A confirmed `0`, corroborated.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-77 --status shipped`
- [x] row `pr` stamped `282`
- [x] row `landing` stamped `landings/PLAN-77.md`
- [x] row `plan_marshall_plan_id` stamped `77-cui-standards-foundation` (the sender's derived name)
- [x] epic.md narrative reconciled against the queue rows
- [x] defects and watches opened, updated or retired (see Follow-Ups)
- [x] re-grounded notes added to PLAN-78, PLAN-79 and PLAN-87
- [x] resume anchor updated in `resume_anchor.md`
- [x] `queue-view.md` regenerated
- [x] inbox message `rb-77-cui-standards-foundation-001.md` archived after this record

## Follow-Ups

- **Incomplete landing facts** (`total_tokens`, `plugin_version` unknown): recurrence on the Open Defect.
- **PLAN-78, 79 and 87 are unblocked** and copy this pattern. A note is added to each: the test-scope CUI dependencies
  are declared (`pm-api` and `pm-mock-runtime` now too); the guard's allow-list admits the test stack; draw `Generators`
  values inside the test or a `@BeforeEach`, not in field initialisers (the pilot's `RelayTest` and `PmMcpProcessIT` do
  the latter, which the run flags as not seed-controlled); `LandlockTest`-style classes whose values are the contract are
  listed input-free, not converted.
- **Coverage gap, for the operator:** the `MOCK-201` close-failure record is asserted through a package-private helper;
  no production path reaches it. Accept as is, or ask for a production seam. Recorded as a Watch.
- **The executor was regenerated in the main checkout by a lane** (sanctioned by the skew verdict). The other running
  lanes (PLAN-82, 86, 17) now start on `0.1.2036`.
- **Specs whose declared files this merge changed** (re-read at HEAD before emit): the root `pom.xml` and the module POMs
  (PLAN-02, 72, 76, 78, 79, 87), `MockRuntimeExtension` (PLAN-87), `CLAUDE.md` (every spec lists it as a constraint source;
  PLAN-85 and PLAN-88 are the newest), `README.adoc` (PLAN-82 among others). None of the staged specs declares the test
  classes this plan converted.
- **Residue (accepted):** `cui-java-tools` stays unbound in the enforcer include (the rule cannot match a transitive
  scope); per-file canary checks and `Generators` determinism notes carried forward.
- **Branch and worktree:** no `feature/77*` branch remains on the remote (`git ls-remote`) and the lane's worktree is gone
  (`git worktree list`), both checked after this record.

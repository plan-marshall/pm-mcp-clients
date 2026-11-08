# Landing Analysis: PLAN-118 — Terminal: Command-Tree Residue

epic: ui-improvements
workstream: WS-04
pr: #369 (https://github.com/plan-marshall/pm-mcp-clients/pull/369), merged as `2fc2c214` (squash, 20 files, +790/-71; merge head `032e23e2`)

> Landing record for one shipped plan. Written by the `analyze` verb (inbox scan, message
> `rb-118-cli-command-tree-residue-001.md`, 2026-11-08) after checking the lane's claims against git, the PRs
> (read through the `ci` abstraction) and Sonar. The lane's report is a lead, never a fact.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 `drain` signals only a process it can identify | shipped | `RuntimeRecord.identifies` (`RuntimeRecord.java:83-87`): same pid, a recorded start instant, and a reported start equal to the second, `orElse(false)`. `DrainCommand.java:100-127` resolves the handle once, checks `isAlive`, then `identifies`, then signals. Tests `DrainCommandTest:204,220,236,252`. The read side is unchanged: the `processAlive` body is byte-identical to the parent, and its four callers are untouched. Bound tests at `DrainCommandTest:619-650` |
| D2 One source for the verb list, a test of `help` | shipped | `OperatorVerbs.registered()` reads the `@Command` subcommands; the completer (`PmConsole.java:137`) and `help.more` (`:539`) are derived from it. `PmOperatorTest#registersTheDesignsVerbs` (`:249-272`) reads `01-commands.adoc` by walking up the directory tree and throws if the file is missing (not vacuous): 22 design verbs = 22 registered. `git grep DESIGN_VERBS` over `pm-operator/src/test` is empty |
| D3 Every item answered where the reviewer reads it | shipped | Per the lane: 12 of 12 review-body items answered on #358, 3 threads on #327, Sonar 0 at `032e23e2`. The orchestrator re-read #369 itself (below). #358/#327 were not re-read in this pass |
| D4 Verb-folding decisions and owner questions | shipped | `01-commands.adoc:149-154` and `17-projects.adoc:36,38` match the code (`PlanCommand`, `DecomposeCommand` under `EpicCommand`, `RuntimeCommand --drain`, `ProjectCommand.AddCommand` = `POST /api/v1/enrol`, release/unenrol `--yes`). Every open item names its owner |

Changed outside the Expected Surface, each tied to a deliverable:
- D1: `25-system.adoc` (the exit code the design names), and `pm-mock-runtime` `MockRuntime.java:259,273`. The mock now records its real `ProcessHandle` start instant instead of `Instant.now()`. It has no test of its own, and the lane report does not mention it.
- D2: `OperatorVerbs` and `OperatorVerbsTest`; `ErrorTexts.java`, `ErrorTextsTest` and `error.{en,de}.properties`.

## Three-Layer Analysis

- **Dispatch:** one read-only leaf (`execution-context-level-3`, `orchestrator.analyze`). Reason: every full ship gets its own leaf; 20 files across `pm-api`, `pm-operator`, `pm-mock-runtime` and the design.
- **Verdict:**
  - layer 1: minor gaps;
  - layer 2: minor gaps;
  - layer 3: no defect shown. The leaf's one major unproven point (the daemon's `start_instant` writer) was settled by the orchestrator outside the repository: corroborated.
- **Re-read by the orchestrator at `2fc2c214`:**
  - `RuntimeRecord.java:50-87` (parsed by `Instant.parse`, both sides truncated to seconds);
  - `DrainCommand.java:98-128`;
  - `MockRuntime.java:255-275`;
  - `01-commands.adoc:188-193`;
  - `plan-marshall/plan-marshall-mcp` `origin/main` `ad5f608`, `pm-mcp-server/.../runtime/SocketBinding.java:95`. This is the daemon's writer, which records `ProcessHandle.current().info().startInstant()` (or `null`).
  - `pm-mcp-core` `doc/specification/runtime-lifecycle/01-startup-recovery-drain.adoc:70` ("confirms that the process with that PID has that start instant (never signalling a reused PID)").

  All confirmed.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | OBSERVED: `processAlive` returns true for a null start instant or a missing start info, and drain signalled the filtered handle | checked | confirms | `RuntimeRecord.java:66` and `DrainCommand.java:99` at `2fc2c214^` |
| L1-2 | D1 orders `processAlive` to fail closed but leaves open whether the read-side callers change | checked | refutes (gap) | the lane split read (`processAlive`) from act (`identifies`); a CodeRabbit Major came back on that and was resolved in `3d44fab` |
| L1-3 | A mismatching start instant (dead runtime plus a reused PID) gets the same exit 1 as "no start info", while the read paths call it "not running" | checked | refutes (tension) | `25-system.adoc:39`; spec-intended, and documented |
| L1-4 | The Expected Surface and the `-pl pm-api,pm-operator` gate omit files the deliverables need | checked | refutes | `25-system.adoc`, `ErrorTexts*`, `error.*.properties`, `MockRuntime.java`, `OperatorVerbs*`; the gate leaves out `pm-mock-runtime` |

**Layer 1 verdict:** findings — 3 minor; the premise holds.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 identity | `RuntimeRecord.java:83-87`, `DrainCommand.java:106-127` | `DrainCommandTest:204,220,236,252` (real child plus fakes) | none | checked |
| D1 bound | `DrainCommand.java:178-216` | `DrainCommandTest:619-650` | expectations are built from production constants (`DEFAULT_DRAIN_TIMEOUT_SECONDS`, `GRACE_PERIOD`, `MARGIN`), so a changed constant passes; the invalid case covers `-1` only, not the String branch (`:205-208`) or a non-object file | checked |
| D2 single source | `OperatorVerbs.java:46`, `PmConsole.java:137,539` | `PmOperatorTest:272`, `OperatorVerbsTest:38`, `PmConsoleTest:162` | nothing pins that `PmConsole` uses `OperatorVerbs.completer()`: a completer reverted to a hand list passes. `CONSOLE_ONLY` exists twice and disagrees (`PmOperatorTest:268` {help, exit} vs `OperatorVerbs.java:32` {exit, quit, help, tui}); `quit` and `tui` are not named in `01-commands.adoc`. The documented-unimplemented set is read from the same document (`01-commands.adoc:152`), so a verb can leave the comparison by an edit of that bullet | checked |
| D2 `enrolment_tampered` | `ErrorTexts.java:138-168` | `ErrorTextsTest:145` | `ErrorTexts` has no production caller: no real output gets the id (lane residue) | checked |
| D4 design | `01-commands.adoc:149-154`, `17-projects.adoc:36,38` | — | the exit-code table row `1` (`01-commands.adoc:191`) still says "refused by the runtime"; drain's local `not_identifiable` (and `signal_failed`) use exit 1 without being a runtime refusal | checked |
| Lane narrative | — | — | the key is `operator.drain.not_identifiable`, not `not_identified` (code, properties and design agree) | checked |
| Test-first / mutation-after runs | — | — | marshalld job ids cannot be shown from git | not_shown_by_reading |

**Layer 2 verdict:** findings — 5 minor (bound expectations, completer wiring, two `CONSOLE_ONLY` sets, the exit-code table, `MockRuntime` untested); not shown by reading — the run evidence.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| — | `RuntimeRecord.java:83-87` | a real `pm-mcpd` record | the daemon writes its own `ProcessHandle` start instant (`SocketBinding.java:95`), and both sides are compared to the second: drain identifies the real runtime. Residue: a sub-second value that straddles a second boundary between two derivations would not match (no tolerance). Every known writer uses the same derivation | none (corroborated) |
| L3-1 | `SocketBinding.java:95` (daemon) | a platform without start info | the daemon writes `start_instant: null`: `status` reports running (`.orElse(true)`), while `drain` exits 1 "not identifiable". This is fail-closed by design | info |
| L3-2 | `DrainCommand.java:106-127` | PID reuse between the check and `destroy()` | a narrow TOCTOU window (the lane declares it as the pidfd window); whether the JDK's `destroy()` re-verifies is run-only | info |

**Layer 3 verdict:** no defect shown (2 info).

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md § Code Standards | no generic Exception in production | conforms | `DrainCommand.java:156-165,181-216` catch specific types only |
| CLAUDE.md § Code Standards | package-private over public | conforms | `OperatorVerbs` is package-private; `boundOfMachine` is package-private (`:178`) |
| CLAUDE.md § Code Standards | Generators for input data | deviates | hand-written `Instant.parse('2001-01-01T00:00:00Z')` (`DrainCommandTest:239,287`) and the `-1` / `0` configs (`:636-650`) |
| CLAUDE.md § Code Standards | no arbitrary timer wait in tests | conforms | the ready-file condition with a deadline (`DrainCommandTest:146`) |
| CLAUDE.md § Code Standards | JUnit 5, @DisplayName, no Mockito | conforms | hand-written `ProcessHandle` fakes |
| Spec Write-Boundary | stay within the surface | deviates (explained) | the 6 files outside the surface are each tied to D1/D2; `MockRuntime.java` is not mentioned by the lane |

## Metrics and Anomalies

- Tokens: `unknown` (opencode, `deepseek-v4.1-flash/high`); `plugin_version` `unknown`; executor `0.1.2080` (regenerated from 0.1.2036 at start on the operator's word).
- Anomalies:
  - The landing facts are incomplete (`missing_keys` `total_tokens`, `plugin_version`): a recurrence on the standing Open Defect.
  - `self-review: budget-exhausted` (items declined with reasons; none blocking).
  - The lane broke the shell-read rule (`grep`/`ls`/`test` on build logs and tool output) and reported it itself.
  - `ci pr edit` would not consume a prepared body, so the PR body still says the Sonar read is "to be read". The read is recorded in `#issuecomment-6715013411`.

## Routing and Merge Behavior

- **Review.** CodeRabbit reviewed `88b13d5` (01:46Z) and `d5371124` (10:09Z); the merge head `032e23e2` was not reviewed (refused on quota and recorded). The merge rule is met (one review of some head).
  - Threads: 8, all resolved (5 from the first review, 3 from the second).
  - Review bodies: 2, all items answered:
    - body 1 (6 actionable: 5 threads, 2 nitpicks, 1 additional item) in `IC_kwDOVBOtss8AAAABkEzIZw`;
    - body 2 (3 actionable, 2 nitpicks) in `IC_kwDOVBOtss8AAAABkJHeew`.
  - The orchestrator re-read both reply comments against the bodies.
  - Sourcery skipped on quota.
- **CI and merge.** Merged through the merge queue as `2fc2c214`. It ran beside PLAN-116, PLAN-117, PLAN-21 and PLAN-34; the operator was asked twice and answered "merge" twice.
- **Sonar.** Lane `open_issue_count=0` at `032e23e2`. `read_open_issues --pr 369` returned `undecidable` (`head_revision_unavailable`, analysis revision `032e23e2`). The SonarCloud gate comment on #369 reports 0 new issues. Consistent: 0.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 369; `landing` `landings/PLAN-118.md`; `plan_marshall_plan_id` `rb-118-cli-command-tree-residue`
- [x] claim 0 of the spec stamped `corroborated`
- [x] epic.md: incomplete-landing recurrence, `ci pr edit` defect, and a Watch for the follow-ups
- [x] folds:
  - PLAN-79: the bound tests with literal expectations and the String / non-object branches; a test that `PmConsole` uses `OperatorVerbs.completer()`. Surface unchanged.
  - PLAN-42: one `CONSOLE_ONLY` set, with `quit` and `tui` named in the design. Surface adds `OperatorVerbs.java` and `PmOperatorTest.java`.
  - PLAN-30: the exit-code table row `1` covers local refusals. Surface unchanged.
- [x] resume anchor updated; `queue-view.md` regenerated and committed

## Follow-Ups

- **Bound tests and completer wiring:** folded into PLAN-79.
- **Two `CONSOLE_ONLY` sets; `quit`/`tui` missing from the design:** folded into PLAN-42.
- **Exit-code table row `1`** (`01-commands.adoc:191`): folded into PLAN-30.
- **Lane residue, listed in the PR with owners:**
  - the pidfd window;
  - the `SystemScenarioTest` boundary;
  - coverage of `LessonSourceCommandTest`, `MockRuntimeEngineTest`, `AcceptEditCommandTest` and `GitSourceCommandTest`;
  - `ErrorTexts.fix(..., Map)` has no production caller;
  - five design verbs are unregistered (`profile`, `decision-log`, `waive`, `grant-access`, `sweep`);
  - the specification's `enrol` line (owner `plan-marshall-documentation`).

  Watch.
- **Operator questions in the PR body:** `project add` enrols directly while the design says it onboards; `project release` / `project unenrol` confirm by flag only. Watch; open for the operator.
- **Fail-closed tension:** a stale record with a reused PID exits 1, where the read paths say "not running". Watch.

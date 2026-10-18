# Completion Report: `cli-test-infra` (Transcript and Dialogue Test Infrastructure)

**Work package**: `doc/plans/cli-test-infra.adoc` (epic clients)
**Requirement**: `PM-TEST-7`
**Status**: Implemented, verified locally and against the native image
**Branch**: `feat/cli-test-infra`

---

## 1. Executive Summary

The package delivers the transcript and dialogue test infrastructure of the terminal client
(`PM-TEST-7`):

1. **A secure, test-only terminal seam.** `TerminalTestDriver` supplies an
   `OperatorContext` whose `TerminalProvider` is a scripted JLine 4 terminal and whose
   interactivity flag is passed by the test alone. Production derives that flag from
   `System.console()` in `OperatorContext.system()`; the seam adds no production code and no
   bypass, because `OperatorGate` still refuses a non-interactive process and every coding-agent
   environment signal.
2. **A transcript engine.** `TranscriptTerminal` (a JLine terminal pinned to 80x24),
   `ScriptedInput` (blocking, bounded key script), `TranscriptNormalizer` (ANSI/OSC escapes,
   instants, clock times, durations, process ids, machine roots), `TranscriptAssert` (strict
   compare, opt-in recording under `-Dpm.transcript.update=true`, never a silent update), and
   `UnifiedDiff` (LCS-based unified diff with context) form the recording and comparison engine.
3. **Initial transcript coverage** against `pm-mock-runtime` scenarios: `pm-operator status`
   (`status.transcript`), `pm-operator web pair` (`web-pair.transcript`), and a typed console
   dialogue with `status`/`exit` (`console-status.transcript`).
4. **JVM and native verification.** The same transcripts run in the unit phase, under
   `-Pintegration-tests`, and against the native binary through `script(1)` as its pseudo
   terminal (`NativeTranscriptIT`, skipped unless `target/pm-operator` was built with `-Pnative`).

The native run of the spike found and fixed a real defect: the native image did not bundle the
message catalogue, so every runtime text rendered as its key. The native profile now includes
`messages/.*` only, keeping the native libraries of `jline-native` out of the binary.

## 2. Delivered Artifacts

### 2.1 `pm-operator` (test scope)

| File | Purpose |
|---|---|
| `src/test/java/de/planmarshall/operator/transcript/TranscriptTerminal.java` | JLine 4 terminal over scripted input and captured output, pinned to 80x24 |
| `src/test/java/de/planmarshall/operator/transcript/ScriptedInput.java` | Scripted keystrokes; a bounded wait turns an unanswered read into end-of-file instead of a hang |
| `src/test/java/de/planmarshall/operator/transcript/TerminalTestDriver.java` | Types the script, projects an `OperatorContext`, captures the render |
| `src/test/java/de/planmarshall/operator/transcript/TranscriptNormalizer.java` | Removes transient parts (escapes, instants, clock times, durations, PIDs, machine root) |
| `src/test/java/de/planmarshall/operator/transcript/TranscriptAssert.java` | Strict transcript comparison, readable diff on drift, opt-in recording |
| `src/test/java/de/planmarshall/operator/transcript/UnifiedDiff.java` | LCS-based unified diff with three context lines |
| `src/test/java/de/planmarshall/operator/transcript/MockMachine.java` | Writes the truthful live runtime record of the mock runtime host process |
| `src/test/java/de/planmarshall/operator/transcript/PseudoTerminal.java` | Runs a binary through `script(1)`, the pseudo terminal the native binary needs |
| `src/test/java/de/planmarshall/operator/transcript/TranscriptDialogues.java` | The three scenarios, shared by the unit tests and the IT |
| `src/test/java/de/planmarshall/operator/transcript/StatusDialogueTranscriptTest.java` | `status` dialogue transcript |
| `src/test/java/de/planmarshall/operator/transcript/WebPairDialogueTranscriptTest.java` | `web pair` dialogue transcript |
| `src/test/java/de/planmarshall/operator/transcript/ConsoleDialogueTranscriptTest.java` | Typed console dialogue transcript (JLine line reader) |
| `src/test/java/de/planmarshall/operator/transcript/TranscriptDialogueIT.java` | The same dialogues under `-Pintegration-tests` |
| `src/test/java/de/planmarshall/operator/transcript/NativeTranscriptIT.java` | The same transcripts against the native binary through a pseudo terminal |
| `src/test/java/de/planmarshall/operator/transcript/TranscriptHarnessTest.java` | Unit tests of normalizer, diff, recorded-transcript failure, scripted terminal, resize pinning, end of script |
| `src/test/java/de/planmarshall/operator/TranscriptVerbRunner.java` | Test-scope bridge from the `transcript` package to the package-private `PmOperator.execute` |
| `src/test/resources/transcript/status.transcript` | Recorded `status` overview (running-work scenario) |
| `src/test/resources/transcript/web-pair.transcript` | Recorded `web pair` dialogue (web-pairing scenario) |
| `src/test/resources/transcript/console-status.transcript` | Recorded typed console dialogue (status-only scenario) |

### 2.2 Build and dependency changes

| File | Change |
|---|---|
| `pm-operator/pom.xml` | JLine `3.30.15` → `4.4.8` (the specification's line editor); test-scoped dependency on `pm-mock-runtime`; native profile includes the message catalogue (`-H:IncludeResources=messages/.*`) |
| `pom.xml` | The `clients-depend-on-pm-api-only` guard allows `de.planmarshall:pm-mock-runtime:*:jar:test` — test scope only, so no mock or engine code can reach a binary |

### 2.3 Documentation

| File | Change |
|---|---|
| `doc/design/terminal-client/README.adoc` | The line-editor open point records the decision (test seam in the JVM, `script(1)` for the native binary, JLine 4, catalogue in the image, `jline-native` out) and the measurements still owed |
| `doc/agy/implemented/cli-test-infra-plan.md` | The plan, moved after completion |
| `doc/plans/implemented/cli-test-infra.adoc` | The package, moved after completion |
| `doc/plans/README.adoc` | The package row is `done` and links the archived file |

### 2.4 Deliberately not changed

* `pm-mock-runtime` — a read-only test fixture in this package; the scenarios (`running-work`,
  `web-pairing`, `status-only`) are consumed as delivered.
* `pm-operator/src/main/**` — no production seam was added; the existing
  `OperatorContext.TerminalProvider` and interactivity flag suffice. The one-main-source
  ownership of the concurrent `operator-harness-and-roles-verbs` package is respected.

## 3. Test & Verification Results

### 3.1 Unit and transcript tests (`pm-operator`, JVM)

`./mvnw test -pl pm-operator -am`: **435 tests, 0 failures, 0 errors, 0 skipped**.
That is the 423 pre-existing tests plus 12 new ones:

* `TranscriptHarnessTest` (9): normalizer escapes/transients/layout, unified diff equal and
  changed, recorded-transcript drift fails with a readable diff, fixed 80x24 grid survives a
  resize, scripted line reaches a JLine reader and is echoed, an exhausted script ends the stream.
* `StatusDialogueTranscriptTest` (1): `status` against `running-work`.
* `WebPairDialogueTranscriptTest` (1): `web pair` against `web-pairing`.
* `ConsoleDialogueTranscriptTest` (1): typed `status`/`exit` against `status-only`.

### 3.2 Integration tests

`./mvnw verify -Pintegration-tests -pl pm-operator -am -Dit.test=NativeTranscriptIT`:

* `NativeTranscriptIT` (2): `status` and `web pair` match the **same** `.transcript` files when
  driven against the native binary through `script(1)`.
* `TranscriptDialogueIT` (3) covers the same three dialogues in-process under the profile.

### 3.3 Native image (Phase 4 spike)

* `JAVA_HOME=<graalce-25> ./mvnw -Pnative package -pl pm-operator -am` — `BUILD SUCCESS`,
  `target/pm-operator` 24.05 MiB, built from JLine 4.4.8 with the FFM provider.
* The first native transcript run **failed** with raw catalogue keys (`overview.project.counts`,
  `overview.state.waiting`, …): the native image had no `/messages/*.properties`. The native
  profile now includes `messages/.*`; the rerun passes.
* No `libjlinenative` artifact is produced or shipped (`target/` has only the JDK's AWT/JVM
  libraries beside the executable), so the JNI provider's native libraries stay out; the process
  uses the FFM provider.
* Still to measure (recorded in the design): size and time to the first prompt with and without
  the line editor, `Ctrl+C` during a running verb, and the FFM signal handler under the macOS
  hardened runtime.

### 3.4 Drift detection

`TranscriptHarnessTest.failsWithDiff` feeds a deliberately changed buffer to
`TranscriptAssert.assertMatches("status", …)` and asserts that the failure message is a unified
diff naming `status.transcript`, with the removed `Runtime pm-mcpd` line and the added
`runtime   not running` line. A changed transcript is therefore a reviewable diff, never a silent
update.

## 4. Domain Invariants

* **Java 25 / JLine 4**: the module still compiles for release 25; the line editor is JLine 4.4.8
  across `jline-terminal`, `jline-reader`, and `jline-terminal-ffm`; the existing 423 tests pass
  unchanged on the new version.
* **Operator channel isolation (`PM-SEC-3`)**: the test seam is a test-scoped `OperatorContext`
  only. `OperatorGate` is unchanged and still refuses a non-interactive terminal and every
  coding-agent host signal; `PmOperatorTest`/`PmOperatorProcessIT` continue to prove that.
* **No product module in a binary**: the mock runtime is reachable in `test` scope only. The
  enforcer pattern pins the scope, and the build guard control (`product-module`) still fails as
  expected.
* **Message catalogue in both clients**: the native image now ships the English and German
  catalogue; the transcript fixtures are English and the normalizer leaves the texts untouched.
* **No model-facing content**: no `workflows/`, `roles/`, `bundles/`, or `skills/` resource was
  added.

## 5. Build & Quality Evidence

* `./mvnw verify -Ppre-commit` — `BUILD SUCCESS` across all eight reactor modules
  (`pm-api`, `pm-exec`, `pm-relay`, `pm-mock-runtime`, `pm-operator`, `pm-web-app`, `pm-web-e2e`),
  with OpenRewrite, licence headers, Checkstyle, Enforcer, Javadoc, and the full unit suite.
* `git status` after the gate shows only the intended files (no reformatted sources, no untracked
  build artifacts).
* `./mvnw verify -Pintegration-tests -pl pm-operator` (focused on the new ITs) — `BUILD SUCCESS`.

## 6. Notes and Follow-ups

* **`report.arrival.line` placeholder**: the console transcript records `since {time}` exactly as
  the current `EntryFlow.printReportsArrivalNotice` renders it — the catalogue expects a `time`
  parameter while the code passes `since`. This is a pre-existing defect in `pm-operator` main
  code (owned by another package); the transcript now guards the behaviour and will show the fix
  as a reviewable diff.
* **Operator Channel Isolation, item 2 (external repository)**: the package file asks the seam to
  be stated in `plan-marshall-documentation`. No new production seam was introduced (the test
  uses the existing `OperatorContext` injection point), so the specification text is unchanged
  here; if the maintainers want the test seam named explicitly, that is a documentation-only
  change in `plan-marshall-documentation`.
* **Native measurements**: the remaining open-point measurements (with/without line editor,
  `Ctrl+C`, hardened-runtime signals) need a benchmark harness and a hardened runtime; they are
  recorded in `doc/design/terminal-client/README.adoc` for `cli-onboarding`.

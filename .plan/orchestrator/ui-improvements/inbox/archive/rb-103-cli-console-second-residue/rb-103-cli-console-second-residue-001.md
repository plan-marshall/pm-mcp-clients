envelope_version=1
sender_type=plan
sender_id=rb-103-cli-console-second-residue
epic=ui-improvements
kind=landing
created=2026-11-05T22:04:57Z

```landing-facts
schema=landing-facts/1
plan_id=rb-103-cli-console-second-residue
epic=ui-improvements
pr=#341
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Runbook Lane Landing: PLAN-103 (103-cli-console-second-residue)

## 1. Deliverables

### Deliverable 1: The badge reads, the lock and the step list
- **Clause 1 (a 200 without onboardings keeps cached badges):** MET. Test: `PmConsoleBehaviourTest.a200WithoutOnboardingsKeepsTheCachedProjectBadges`. Order: `test-first`. Failing run: commit `e8ef5f8d` (`maven-2026-11-05-164548.log`, expected `<[setup cui-http 5/10]>` but was `<[]>`). Passed at `e8ef5f8d`.
- **Clause 2 (prompt not delayed past deadline):** MET. Test: `PmConsoleBehaviourTest.thePromptIsNotDelayedPastTheDeadlineByAStalledRefresh`. Order: `test-first`. Failing run: commit `e8ef5f8d` (`maven-2026-11-05-164548.log`, execution timed out after 6000 ms). Passed at `e8ef5f8d`.
- **Clause 3 (badge total follows aspects of another size):** MET. Test: `EntryFlowTest.theBadgeTotalFollowsTheAspects`. Order: `test-first`. Failing run: commit `e8ef5f8d` (`maven-2026-11-05-164548.log`, intro line must say '8 steps.'). Passed at `e8ef5f8d`.
- **Clause 4 (Enter on project badge addresses daemon project_id):** MET. Test: `PmConsoleBehaviourTest.enterOnProjectSetupBadgeAddressesDaemonProjectId`. Order: `test-first`. Failing run: commit `e8ef5f8d` (`maven-2026-11-05-164548.log`, expected `custom-daemon-id` but was `other-dir-name`). Passed at `e8ef5f8d`.
- **Committed at:** `e8ef5f8d` (D1).

### Deliverable 2: The design, the German row and the clock
- **Clause 1 (German row maps to same <ago>):** MET. Test: `TranscriptHarnessTest.replacesGermanAbsoluteLastUsed`. Order: `test-first`. Failing run: commit `390f2581` (`maven-2026-11-05-172358.log`, expected `<ago>` but was `<time>`). Passed at `390f2581`.
- **Clause 2 (clock test detects system clock reading):** MET. Test: `ConsoleSettingsTranscriptTest.machineSettings`. Order: `test-first`. Failing run: commit `390f2581` (`maven-2026-11-05-172358.log`, raw output should reflect absolute date with clock 120 days ahead). Passed at `390f2581`.
- **Clause 3 (design names Enter rule & raw-mode decision):** MET. Verified in `doc/design/terminal-client/04-console.adoc` under `[#console-raw-mode-decision]`. Order: `none`.
- **Committed at:** `390f2581` (D2).

### Deliverable 3: The tests can fail, and the evidence is readable
- **Clause 1 (rename refusal):** MET. Test: `WebDevicesCommandTest.aRefusedInteractiveRenameLeavesWithOne`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-175720.log`). Passed at `d656c59d`.
- **Clause 2 (listener-off refusal):** MET. Test: `WebDevicesCommandTest.aRefusedInteractiveListenerOffLeavesWithOne`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-175810.log`). Passed at `d656c59d`.
- **Clause 3 (act_failed exit):** MET. Test: `WebDevicesCommandTest.anInteractiveActionFailureReportsActFailed`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-175908.log`). Passed at `d656c59d`.
- **Clause 4 (? help prompt):** MET. Test: `WebDevicesCommandTest.questionMarkAtRevokePromptShowsHelp`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-175959.log`). Passed at `d656c59d`.
- **Clause 5 (clearTerminal safety):** MET. Test: `OperatorContextTerminalTest.clearTerminalFailureDoesNotMaskTryExceptionAndClosesOwnedTerminal`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-180040.log`). Passed at `d656c59d`.
- **Clause 6 (floor assert):** MET. Test: `SystemViewConsistencyTest.theSummaryNamesItsFloor`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-180158.log`). Passed at `d656c59d`.
- **Clause 7 (leaveBack sites):** MET. Test: `ActionSupportTest.chatFollowUpInterruptLeavesBack` and `leaveBackPreservesNonInterruptExitCodes`. Order: `mutation-after`. Failing run: commit `d656c59d` (`maven-2026-11-05-180240.log`). Passed at `d656c59d`.
- **Clause 8 (terminal capture before/after differ):** MET. Test: `ScreenCaptureIT` capture set `103-cli-console-second-residue.txt`. Order: `test-first`. Failing run: commit `d656c59d` (`maven-2026-11-05-181345.log`). Passed at `d656c59d`.
- **Committed at:** `d656c59d` (D3).

### Deliverable 4: Every item is answered where the reviewer reads it
- **Clause 1 (PR 321 threads answered & resolved):** MET. All 8 review threads replied to and resolved on GitHub. Order: `none`.
- **Clause 2 (Review 5785729266 answered):** MET. Findings F1-F4 and N1-N6 answered in PR comment. Order: `none`.
- **Clause 3 (PR 341 review threads answered & resolved):** MET. All 4 CodeRabbit review threads replied to and resolved in commit `5968a986`. Order: `none`.
- **Clause 4 (0 open Sonar issues at head):** MET. Verified 0 open issues, 0 hotspots, 81.1% coverage on head `42034a6f`. Order: `none`.
- **Committed at:** `6f0252fe`, `09b1aa1c`, `ccf9e063`, `5968a986` (D4).

### Test Runs on Pushed Heads
- `d656c59d`: quality gate / verify clean.
- `6f0252fe`: quality gate / verify clean (2102 tests green).
- `09b1aa1c`: quality gate / verify clean (2102 tests green).
- `ccf9e063`: quality gate / verify clean (2102 tests green).
- `5968a986`: quality gate / verify clean (`maven-2026-11-05-202908.log`, 2103 tests green).
- `e1e05fe5`: quality gate / verify clean (`maven-2026-11-05-205943.log`, 2103 tests green).
- `42034a6f`: quality gate / verify clean (`maven-2026-11-05-212416.log`, 2103 tests green).

## 2. Personas and Skills
- **Personas applied:**
  - D1: `persona-implementer`
  - D2: `persona-implementer`
  - D3: `persona-module-tester`
  - D4: `persona-implementer`
  - Self-review & PR comment triage: `persona-code-reviewer`
- **Domain skills declared & loaded:**
  - D1: `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`
  - D2: `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-documents:ref-asciidoc`
  - D3: `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`
  - D4: `pm-dev-java:java-core`
  - Surface drift: none.

## 3. Hypotheses
- D1: "a 200 without onboardings empties project badges" — confirmed, `EntryFlow.java:708-711`
- D1: "badge refresh lock wait can delay prompt" — confirmed, `PmConsole.java:324, 652`
- D1: "badge projectId is checkout directory name and ignores daemon project_id" — confirmed, `EntryFlow.java:730`
- D2: "the clock test reads the wall clock" — confirmed, `ConsoleSettingsTranscriptTest.java:37`

## 4. Findings
- Self-review (Round 1): all Done-when met as written; assertion updates verified against spec; no doc drift; disposition: clean.
- Sonar findings on initial PR head `d656c59d`: 6 code smells (`java:S1135`, `java:S2093`, `java:S2925`, `java:S8491`). Fixed in commits `6f0252fe` and `09b1aa1c`.
- Sonar coverage deficit on `09b1aa1c`: coverage was 72.4%. Added test coverage in `MessageCatalogueTest`, `ConsoleBadgeTest`, `EntryFlowTest`, and `PmConsoleBehaviourTest` in commit `ccf9e063`, raising new-code coverage to 81.1%.
- CodeRabbit PR 341 review on `ccf9e063`:
  1. `PRRT_kwDOVBOtss60QRgI` (`04-console.adoc`): Scoped raw-mode `stty` explanation to `exec` provider. Fixed in `5968a986`, thread replied & resolved.
  2. `PRRT_kwDOVBOtss60QRgV` (`EntryFlow.java`): Caught `IllegalArgumentException` from `URI.create` in `tryOpenBrowser`. Fixed in `5968a986`, thread replied & resolved.
  3. `PRRT_kwDOVBOtss60QRgp` (`EntryFlow.java`): Clamped thread join bound to >= 1 ms. Fixed in `5968a986`, thread replied & resolved.
  4. `PRRT_kwDOVBOtss60QRgu` (`StateDialogueTest.java`): Restored exact state-to-step assertions. Fixed in `5968a986`, thread replied & resolved.

## 5. Reviewers
- `coderabbit` (required, quota-limited):
  - Initial head `d656c59d`: triggered, rate limit notice.
  - Head `6f0252fe`: triggered, rate limit notice.
  - Head `09b1aa1c`: triggered, rate limit notice.
  - Head `ccf9e063`: reviewed (`PRR_kwDOVBOtss8AAAABWTPBAQ` at `2026-11-05T20:22:16Z`) with 4 actionable comments.
  - Head `5968a986`: triggered, rate limit notice (`IC_kwDOVBOtss8AAAABjXzr0w`). All 4 threads resolved.
  - Head `e1e05fe5`: reviewed (`PRR_kwDOVBOtss8AAAABWTPBAQ` / comment `IC_kwDOVBOtss8AAAABjYzx9g` at `2026-11-05T21:17:41Z`), check status: SUCCESS pass.
  - Head `42034a6f`: triggered, rate limit notice (`IC_kwDOVBOtss8AAAABjZFzow`).
  - Standing review authorization: valid qualifying review on PR heads, all findings fixed and threads resolved, re-triggered after push, all other gate conditions green.
- `sourcery` (optional, quota-limited):
  - Triggered at each head; responded with review character quota exhausted notice. Non-gating.

## 6. Operator Questions
none asked

## 7. Rule Check
- Section 1 rules: all kept.
- Guard refusals: none.
- Guard state: `guard: none on this harness (unenforced)`.
- Spec copy read: `spec_source: ledger_worktree`.
- Executor skew: `verdict: match` (`executor_version: 0.1.2036`, `lane_version: 0.1.2036`).

## 8. Build Server
- Preflight at setup: `ready`.
- Builds routed: in-process fallback (`in_process_fallback`, `unserialized`).
- Killed jobs: none.
- Runtime stamp: `harness=antigravity`, `plugin_version=0.1.0`, `executor_version=0.1.2036`.

## 9. Residue
none

# Landing analysis: PLAN-92 cli-interrupt-completion

PR #309 `c7d9dcb8` (squash-merged 2026-11-02T15:33Z; head `bb43243b`; 39 files, +1036/-112). Lane `rb-92-cli-interrupt-completion`: landing message filed 15:47Z (`merge_state=merged`, deliverables 2 of 3, self-review `budget-exhausted`). Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run). Nothing of the PR head is missing from the merge commit (`git diff bb43243b c7d9dcb8 -- pm-operator doc/design/terminal-client` is empty; the difference is web files from #305).

## Merge rule

CodeRabbit reviewed `99b38f45` (12:59Z): two body findings plus two inline threads, all handled (fixed in `b7ffdc00`, threads resolved and confirmed; the `refusedReads` derivation ANSWERED with a stated reason). The two reviews at `b7ffdc00` (13:14Z) have empty bodies (reply records). **No head after `99b38f45` was reviewed**: CodeRabbit's summary comment (updated 14:52Z) says "Review limit reached" for `99b38f45..bb43243b`. The rule allows this (one review of some head, findings handled), but **the lane's claim "a later incremental run covered 10fdedc to bb43243b with no new findings" is false**. Sonar: gate passed, 0 new issues, 80.7% coverage on new code (the lane lifted it over 80 after 77.3% and 79.0%).

## Three layers

| Deliverable | Implemented | Correct |
|---|---|---|
| D1 a typed Ctrl+C reaches a view-opened dialogue | MET: `ViewDialogueInterruptIT` (four kinds, 130, `assertNoAct`), `useTerminal`/`clearTerminal` in the five view verbs, `OperatorContext` scope stack, `TerminalLease`, all 14 closers and `CredentialsCommand:149` use `openTerminal()`; hold capture recaptured with `\c` | yes; the keystroke is typed right after `awaitText` (flake risk, inferred); the hypothesis is refuted as stated (the key arrived at the dialogue's own second terminal) |
| D2 every handler leaves with 130 | MET: `HandlerInterruptExitCodeTest` (start, settings, selector, onboard --auto, credential secret and confirm, roles, enrol, revoke), `DirectDialogueInterruptIT` for answer; fixes at `StartDialogue:125`, `PlanSettingsDialogue:77`, `ProjectSelector:299`, `AnswerCommand:222`, `CredentialsCommand:153/237`, `EnrolHarnessCommand:241`, `RolesCommand:388`; the three documents state mapping, view-ending set and leave-back set, the cross-reference is a link | yes; the only catches that do not give 130 are the documented leave-back set and the ones the PR added on purpose |
| D3 the tests prove their claims | **NOT MET AS WRITTEN** (the lane says so): `exit_ca_mode` sent once (MET, `TuiScreenDriver:407,440`), token from `Generators` (MET), "Ctrl+C" tests renamed (MET), Sonar read (MET); **unmet**: no `projects` verb test, no terminal-attribute assertion, the 700 ms settle of `InterruptDialogueIT` unchanged, `refusedReads()` still a hand list, the token-row tie and summary assertion of `SystemViewConsistencyTest` untouched | |

Carried over from PR 303's late review `0c92ada1`: FIXED start/settings to 130, onboard --auto, token from Generators, tests renamed, failed-system test, the `04-console.adoc:195` sentence, `dev-2`/`dev-3` (`refusedActs()`); PARTLY entry-point tests (no `projects`) and `refusedReads`; OPEN the two `SystemViewConsistencyTest` findings.

## Leads (V = verified by the orchestrator)

1. **V** `WebCommand.confirm()` (`:642-657`, existed before): `answer == null` and `IOException | RuntimeException` return `true`, so an end of input (Ctrl+D) or a read failure at the revoke prompt CONFIRMS the destructive DELETE; only the interrupt was carved out.
2. The lane's CodeRabbit claim is false (above).
3. `StartPrompt.reader()` `:211` opens a provider terminal that is never closed on a direct verb without a scope (existed before).
4. `ownsTerminal` compares only the innermost scope and `clearTerminal()` pops without checking which terminal; harmless today (every scope pushes the same terminal); `OperatorContextTerminalTest.scopesNest` cannot fail on its last assertion.
5. The `web devices` action-prompt rethrow (`WebCommand:473`) has no test; only revoke is tested.
6. `WebDevicesCommandTest:410` is malformed (fully qualified types, odd spacing); `Generators` called in a static `@MethodSource` outside the controller seed; `"iPad Oliver"` literal input remains.
7. F15 (lane): the ended-chat leave-back mapping to 0 (`OperatorActions:108`, `ActionSupport:288`) has no test.
8. `SystemFullScreenTest.aFailedSystemReadKeepsTheStatusLine` does not assert that the 500 read happened; both new System tests set the console active, so they are not pure direct-verb runs.
9. `project <id>` refusal coverage: answered, not fixed.

## Routing

All open items (D3 clauses, leads 1, 3 to 9): PLAN-94 deliverable 5 (the same area and tests). Lead 1 is a functional defect: it leads the deliverable.

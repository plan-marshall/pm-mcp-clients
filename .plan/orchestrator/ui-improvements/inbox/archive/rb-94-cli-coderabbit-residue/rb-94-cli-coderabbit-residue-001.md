envelope_version=1
sender_type=plan
sender_id=rb-94-cli-coderabbit-residue
epic=ui-improvements
kind=landing
created=2026-11-05T05:00:04Z

```landing-facts
schema=landing-facts/1
plan_id=rb-94-cli-coderabbit-residue
epic=ui-improvements
pr=#321
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
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

| Done-when | Verdict | Test | Order | Failing run | Commit |
|---|---|---|---|---|---|
| D1 (a) the draining line leaves on upgrading, stopping and stopped | met | `SystemFullScreenTest.theDrainingLineGivesWayToTheStatus` (parameterized over the three states) | mutation-after (the spec asks test-first; the earlier session's fail-first run was lost with its progress file, so this session re-established failure by reverting the fix) | `a32a6ab`, maven-2026-11-04-225842.log | `7893a4fa`, `009bc02`, `bd3caa0` |
| D1 (b) a state key with a control sequence prints clean | met | `SystemViewConsistencyTest.stateKeysPrintClean` | mutation-after, same note | `a32a6ab`, maven-2026-11-04-225842.log | `7893a4fa` |
| D1 (c) a failed device act exits 1 with the refusal | met | `WebDevicesCommandTest.aRefusedInteractiveActLeavesWithOne`, `aFailedReadAtTheRevokePromptSendsNoDelete`, `aFailedReadInTheInteractiveListLeavesCleanly` | mutation-after, same note | `a32a6ab`, maven-2026-11-04-225723.log | `7893a4fa`, `009bc02`, `bd3caa0` |
| D1 (d) derived from `figureOf`'s mapping; (e) and (f) added | met | `SystemLevelTest.derivesTheRootRowIdsFromTheFigureMapping`, `SystemRecordsTest.runtimeStatusResolvesBothWireNamesWhenTheMapCarriesBoth`, `SystemFullScreenTest.drillsDownIntoASection` | mutation-after | `a32a6ab`, maven-2026-11-04-225814.log and -225842.log | `7893a4fa` |
| D2 one test per item, fails before | met | `EntryFlowTest.aFailedRefreshKeepsTheEntryResult`, `PmConsoleBehaviourTest.theTitleFollowsTheSetupBadge`, `PmConsoleBehaviourTest.aReconnectReadsTheState`, `EntryFlowTest.anOpenProjectOnboardingIsABadge`, `theBadgeTotalFollowsTheAspects`, `ProjectSetupDialogueIT` | mutation-after, same note | `a32a6ab`, maven-2026-11-04-230010.log | `a8c9ceaf`, `1ed01e9a`, `009bc02` |
| D2 the two unverified/low-value items carry a decision | met (recorded; both were fixed instead, with the reason at the code and in the progress record) | `PmConsole.continueOnboarding`, `OnboardCommand.call`, `ProjectCommand.call` | none | — | `a8c9ceaf`, `bd3caa0` |
| D2 terminal check at 120×30 and 80×24, en_US and de_DE, before and after | met | capture set `pm-operator/src/test/resources/screen-captures/94-cli-coderabbit-residue.txt`; artefacts `.plan/temp/terminal/94-cli-coderabbit-residue/{before,after}` (32 files per side) | none | — | `aadd4c4` |
| D3 a silent runtime leaves `stop` with a stated refusal within the bound | met | `RuntimeActDialogueTest.aSilentRuntimeDoesNotHangTheStop` | mutation-after, same note | `a32a6ab`, maven-2026-11-04-225607.log | `ecac2476` |
| D4 the re-ask assertion counts a prompt event | met | `OnboardingDialogueIT.unknownInputReAsks` (counts `zzz is not offered here` once and asserts the leave line) | mutation-after | `a32a6ab`, maven-2026-11-04-230118.log | `cb18ba92`, `bd3caa0` |
| D4 the open-issue read at the pull request head is recorded | met | Sonar read at the merge head `a40862a`: confirmed 0 open issues, rules left open `none`; earlier heads 1c616f3 (3 issues, fixed in the next commit) and 3e47012 (0) | none | — | — |
| D5 (a) end of input and a failed read send no DELETE | met | `WebDevicesCommandTest.endOfInputAtTheRevokePromptSendsNoDelete`, `aFailedReadAtTheRevokePromptSendsNoDelete` | mutation-after, same note | `a32a6ab`, maven-2026-11-04-225723.log | `4a8c9e84`, `009bc02` |
| D5 (b)–(g) a revert-failing test or a recorded decision | met | `ActionSupportTest.theLeaveBackMapping`, `OperatorContextTerminalTest.refusesToClearTheWrongScope`, `ViewDialogueInterruptIT.projectsExplorerDialogueInterrupt`, `TuiCapabilityIT` (inverted-row attribute), `ReadVerbExitCodesTest`, `SystemViewConsistencyTest` | mutation-after | `a32a6ab`, maven-2026-11-04-225723.log | `4a8c9e84`, `65e700a`, `097a844`, `43c595a`, `bd3caa0`, `7ec4cd2` |

Per-head runs against a pushed head: `verify -Ppre-commit` at `a32a6ab` (2650), `5497f33` (2656), `3e47012` (2662), `7ec4cd2` (2665), `a40862a` (2665); `verify -Pintegration-tests -pl pm-operator -am` at `a32a6ab`, `972fc96`, `1c616f3`, `3e47012`, `7ec4cd2`, `a40862a` (584 each). CI green on `1c616f3`, `3e47012`, `7ec4cd2`, `a40862a`; the merge-group run 41969853537 green.

## Personas and skills

- D1: `plan-marshall:persona-implementer` with `pm-dev-java:java-core, java-lombok, javadoc, java-null-safety, junit-core`, `pm-dev-java-cui:cui-testing, cui-logging`.
- D2: `persona-implementer` with `java-core, java-null-safety, junit-core, junit-integration`, `cui-testing`.
- D3: `persona-implementer` with `java-core, junit-core`, `cui-testing`.
- D4: `persona-module-tester` with `junit-core, junit-integration`, `cui-testing`.
- D5: `persona-implementer` with `java-core, junit-core, junit-integration`, `cui-testing`.
- Every self-review round and every PR comment: `persona-code-reviewer`.
- No skill was added for surface drift: every touched module is in the spec's Expected Surface.

## Hypotheses

- OBSERVED: each D1–D4 finding present on `main` at `94bbefe1` — confirmed at the named symbols (line numbers approximate, symbols matched).
- HYPOTHESIS: line numbers approximate — confirmed; the symbols were located at HEAD.
- HYPOTHESIS: a linkless non-final frame lets `onboard` exit 0 — confirmed and fixed: `OnboardCommand.call` returned 0 for a non-final frame (`OnboardCommand.java:116` before `a8c9ceaf`); it now returns 3, and the automatic path does too (`bd3caa0`).
- HYPOTHESIS: `OnboardingDialogueIT` is still `@Disabled` — refuted: the class carries no `@Disabled` at HEAD.
- OBSERVED: `WebCommand.confirm()` confirmed the revoke on a null answer and on `IOException` — confirmed at `WebCommand.java:649-665`; both now leave with 0.
- HYPOTHESIS: D5 items (b)–(g) — all confirmed at HEAD; (f)'s `@MethodSource` and the `"iPad Oliver"` literal were exactly as described.

## Findings

- Self-review round 1 (sub-agent, `persona-code-reviewer`): 18 findings (7 M, 11 L). Fixed: the project-setup exit code (1 → 3), the two untested failure paths (new failed-read tests), the one-state draining test (parameterized), the unsanitised project badge in the prompt, the stale project badge after a verb, two stale Javadoc blocks, the surplus `rowIdOf`. Recorded as residue/decisions: the root-row order coupling, the fallback badge list, the partial 7-literal derivation, the catalogue-word regex, the floor assertion's scope, the `projectStepOf` test tying to the catalogue, the transcript golden repair.
- Self-review round 2: 10 items. Fixed: `refreshSetupBadge` no longer starts the runtime (`runtime(false)`), Enter's project rule recorded as a decision. Residue: q-before-done exits 0 (pre-existing, `OnboardCommand.StartLeftException`), the `act_failed` wording (fixed later), phantom question counts from badge text, the snapshot-reader handle, the direct-verb System tests.
- CodeRabbit review of `1c616f3` (13 actionable + 6 nitpicks): all fixed or answered, every thread resolved — unboarded-entry project badges, automatic-onboarding exit 3, `plan findings`/`plan watches`, `?` at the revoke prompt, `%` escaping at the prompt only, reconnect refresh on every open, clean-close back-off, Ctrl+D leaving 0, the neutral `act_failed` wording, and the re-ask leave-line assertion; the redundant styled-cell assertion removed.
- CodeRabbit review of `3e47012` (2 findings): `plan settings <id>` added to the read matrix; the outside-diff Major (bound the status read) answered as systemic and recorded as residue.
- Sonar: 3 issues on `1c616f3` fixed (S5778, S135, S3776); 0 on `3e47012`, `7ec4cd2`, `a40862a`.
- No CI failure remained unhandled.

## Reviewers

| Bot | Required | Result | Head reviewed | History |
|---|---|---|---|---|
| coderabbit | yes (`quota_limited`) | reviewed | `1c616f3`, then `3e47012` | refused on quota 23:52Z and 01:17Z, escalated trigger refused 01:18Z, accepted 02:43Z; re-triggered after each push (23:52Z, 00:13Z, 01:22Z, 02:43Z, 04:27Z). Standing authorization: qualifying review at `1c616f3` (marker `kind=reviewed`, coveredCommitId `1c616f3`), every finding fixed or answered with the thread resolved, re-triggered after the last push, and every other gate green on `a40862a`. |
| sourcery | no (`quota_limited`) | refused | none | refused on its own review budget (250,000 diff characters per 7 days, reset in 6 days 21 hours) at 23:52Z and 00:13Z; optional, not gating. |

## Operator questions

none asked.

Free text from the operator: none during this run.

## Rule check

- **Guard:** `guard: none on this harness (unenforced)` — no tool-call guard was installed, so nothing is reported as "all kept".
- **Shell file operations the run issued (rule break, recorded):** `git log -L47,47:…` to read a spec line the Read tool truncates at 2000 characters; `grep`, `ls`, `tail`, `cat`, `cp`, and one `sed -i` (to rename two helper calls in `ViewDialogueInterruptIT`). Each is a read or an edit through the shell where the lane prescribes the file tools; they changed no file content except that `sed` rename, which was reviewed in the diff.
- **Git:** `git worktree add --detach` of a temporary tree under `.plan/temp/before-main` for the "before" screen captures, removed with `git worktree remove --force` because it held the copy of the capture set. The plan's own worktree was removed without force. No `git stash`, `git restore`, `git checkout -- <path>`, `git rebase`, or forced push.
- **Routed builds:** every build ran in-process (`mechanism=in_process_fallback`); the one routed attempt failed before Maven started (`execution_failed`, exit -1: the daemon ran `mvn`, which is not on its PATH, instead of `./mvnw`).
- **Restarts/re-dispatches:** none. One self-review sub-agent per round (two rounds), each read-only.
- **Spec read:** `spec_source=ledger_worktree`, no `spec_differ` marker.
- **Executor skew:** `match` (executor `0.1.2036`, lane `0.1.2036`).
- A run of `-pl pm-operator` without `-am` resolved a stale `pm-api` artifact from `~/.m2` and failed catalogue-key tests; every gate afterwards used `-am` or the full reactor.

## Build server

- Setup preflight: `ready`.
- Every build: `in_process_fallback` — the routed attempt answered `execution_failed` (exit -1) because the daemon invoked `mvn` rather than `./mvnw`.
- No `killed` job.
- `runtime_stamp_reason`: none reported; `plugin_version` is `unknown` (the resolve result carries no version for this install).

## Residue

- The console's status/project-onboarding reads are unbounded on the unix-socket client (CodeRabbit Major, outside diff): a stalled response body can hold the prompt. The correction belongs to the transport in `pm-api` (one read deadline for request and response), not to this plan.
- `q` before a setup's done state still exits 0 for `onboard` and `project setup`, where `01-commands.adoc` says 3; the plan's item covered the timed-out frame only.
- `PmConsole.handleEmptyEnter` continues the machine's onboarding only; Enter on a project's setup badge stays inert (recorded decision at the code: the transcripts pin Enter to the prompt, and changing it is an epic design decision).
- `SystemLevel.rootRows` still pairs `Figure.values()[i]` with `SystemView.root(...).get(i)` positionally; the ids are shared, the order is a comment.
- `StateDialogue.projectStepOf` remains a hand switch; its test ties the list to catalogue keys, not to the state definitions.
- The `console-settings.transcript` golden carries `last used 8 Sep <time>` instead of `<ago>`: the fixture's fixed device date aged out of the relative window; unrelated to PLAN-94.

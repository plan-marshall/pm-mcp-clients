# Landing analysis: PLAN-94 cli-coderabbit-residue

PR #321 `159c310c` (squash-merged 2026-11-05T04:57Z; head `a40862a5` = a merge of main; 46 files). Lane `rb-94-cli-coderabbit-residue`: landing message filed 2026-11-05T05:00Z (`merge_state=merged`, deliverables 5 of 5, self-review `clear`, open Sonar issues 0). Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); the review-thread state was re-read by the orchestrator (GraphQL).

## Merge rule

FORMALLY MET, NOT IN SUBSTANCE. CodeRabbit reviewed `1c616f32` (review `5779338709`: 13 inline comments, 5 body follow-ups, 6 nitpicks) and `3e470127` (`5779659487`: one outside-diff Major, answered with a reason and recorded as residue; the `ReadVerbExitCodesTest` thread fixed in `7ec4cd24`). **EIGHT inline threads of `5779338709` are unresolved with no reply (V: GraphQL `isResolved=false`, one comment each)**, against the lane message "every thread resolved": `EntryFlow.java:63`, `:551`, `:568`, `:570`, `:578` and `PmConsole.java:65`, `:268`, `:318`. The final head `a40862a5` has no CodeRabbit review (the 04:28Z trigger was rate limited). The 5 body follow-ups are fixed (unboarded-entry project badges, exit 3 on an automatic-onboarding timeout, `plan findings`/`plan watches` in `refusedReads`, `?` at the revoke prompt, `%` escaping); 5 inline comments are answered and resolved; the 6 nitpicks were answered in one issue comment (`6648885809`) with weak reasons for all but `TuiCapabilityIT`. Sonar: Quality Gate passed, 0 new issues.

## Three layers

| Deliverable | Implemented | Correct |
|---|---|---|
| D1a draining line leaves on upgrading, stopping, stopped | MET | yes |
| D1b state key sanitised (`SystemView.stateLabel` calls `clean`) | MET | yes |
| D1c failed interactive act exits 1 with the refusal | PARTLY: revoke only; rename and listener-off have no interactive test; the new `act_failed` catch (`WebCommand.java:485-490`) has no test | |
| D1d `rootRows` derived from `figureOf` | PARTLY: ids from `Figure.rowId()`, the pairing with `SystemView.root` stays positional (`SystemLevel.java:360-363`); the new test also passes on the old hard-coded list | |
| D1e, D1f | MET | |
| D2 entry result kept, title follows, reconnect reads state, project badge, badge total, final frame names the project | MET with caveats; `projectStepOf` tie PARTLY (catalogue keys tested, not the state definitions; pins `default -> done`); terminal-capture artefacts `.plan/temp/terminal/94-...` not in the checkout (unverified) | |
| D3 silent runtime bounded (5 s `SNAPSHOT_BOUND`) | MET (`aSilentRuntimeDoesNotHangTheStop`) | |
| D4 re-ask counts a prompt event; Sonar record | MET (a comment says "not by a leave" while the code asserts the leave line) | |
| D5a EOF and failed read at the revoke prompt send no DELETE | code MET (`WebCommand.java:660-673`); NO recorded failing run for the two tests (the cited `225723` log never ran `WebDevicesCommandTest`) | |
| D5b, D5f | MET / recorded decision (comment in `StartPrompt.reader()`) | |
| D5c `clearTerminal`/`ownsTerminal` | MET (`scopesNest` can now fail) | |
| D5d untested rethrows | PARTLY: `leaveBack` tested as a helper, not at its two call sites | |
| D5e | projects Ctrl+C IT, `invertedRows`, token-row tie done; `refusedReads` reason and `SETTLE` decision recorded; the floor assertion is weak | |
| D5g | PARTLY: `ViewDialogueInterruptIT` has a decision; `DirectDialogueInterruptIT` untouched and undocumented | |

Evidence order: `mutation-after` is plausible (local failing runs `225607`, `225723`, `225842`, `230010`, `230118` under `.plan/local/plans/NO_PLAN/build-results/pm-operator/`); the D1c and D5a cells cite `225723`, which does not contain them. Files outside the Expected Surface (46 in all): `operator.en/de.properties` (the spec listed `projects.*.properties`, untouched), `PlanCommand`, `PlansCommand`, `PmOperator`, `ProjectsCommand`, `SystemCommand`, `ProjectCommand` (exit 3), `ConsoleNoticeIT`, `TuiCapabilityIT`, `ProjectSetupDialogueIT`, `console-settings.transcript`, several unit tests.

## The eight open threads

1. `EntryFlow:63`: `STEP_KEYS`, `STEP_WHAT_KEYS` and the resume total `Math.min(completedStep+1, 7)` / `"/7"` stay hand-written (`EntryFlow.java:154,157`); only the badge total was derived.
2. `EntryFlow:551`: project badges are scanned as text by `hasOpenQuestions` and `questionCount` (`PmConsole.java:218,228`).
3. `EntryFlow:568`: the project-onboardings read is unbounded.
4. `EntryFlow:570`: a failed read returns `List.of()`, so every project badge vanishes (`:584-585`).
5. `EntryFlow:578`: one malformed entry drops every project badge.
6. `PmConsole:65`: `MACHINE_SETUP_BADGE` is a regex copied from the en and de catalogue templates (introduced by this PR).
7. `PmConsole:268`: Enter on a project badge is inert (a code comment records the decision); `04-console.adoc` line 24 still says Enter continues "the most recent" unfinished onboarding.
8. `PmConsole:318`: badge refreshes are not serialized between the SSE thread and the REPL thread (`:313,633`).

## Defects (at `159c310c`)

- MED `PmConsole.java:218-254`: a project badge whose checkout name contains "question", "frage" or "waiver" raises the Ctrl+D open-questions warning; `leadingCount` then reads the step digits as the count; a very long digit run throws an uncaught `NumberFormatException` in the EOF handler.
- MED `PmConsole.java:313,633` and `followEventStream:518`: refreshes now also run after every ordinary verb and on every stream open, each an unbounded status plus project-onboardings read on the prompt path: a stalled runtime blocks the prompt after every verb.
- MED `EntryFlow.java:566-585`: a failed or malformed read blanks all project badges (`refreshSetupBadge`, `refreshBadges` replace the old ones with the empty list).
- LOW-MED `PmConsole.java:65` (thread 6). LOW `WebCommand.java:660-663`: `?` at the revoke prompt leaves silently with 0 although the prompt advertises `?` as help. LOW `OperatorContext.clearTerminal` throws `IllegalStateException` inside callers' `finally` blocks (skips `terminal.close()`, masks the original; no trigger found).
- Tests: `SystemViewConsistencyTest.theSummaryNamesItsFloor` (~247) accepts a `≥` on any line; `theBadgeTotalFollowsTheAspects` is circular and pins 7; `aReconnectReadsTheState` spins on a shared `ByteArrayOutputStream` and does not separate first-open from reconnect; `aFailedRefreshKeepsTheEntryResult` does not assert the second status read; neither failed-read DELETE test asserts that the prompt was reached.

Interrupt rules: no violation (Ctrl+C rethrown in `handleInteractive` and `confirm`; EOF and failed read at the revoke prompt do not confirm; the `leaveBack` mapping keeps 0).

## Leads (not verified)

- `StateDialogue.titleOf` guesses the project fact keys `name`, `project_id`, `project`, `id`; the IT passes against the mock only: check against the real `project.completed` facts of `pm-mcpd`.
- Whether `reader.interrupt()` in `RuntimeActDialogue.snapshot` really closes the blocked unix-socket read.
- The two "failed read" WebDevices tests probably exercise JLine's EOF path, not the `IOException` catch.
- The `ReadVerbExitCodesTest` Javadoc says "PR 92" for the missed `project <id>` read (likely wrong).

## Routing

The eight threads, the defects, the partly-met clauses and the weak tests: PLAN-98 (`cli-console-badges-residue`, staged 2026-11-05). The CodeRabbit threads are answered in PLAN-98's own pull request (and resolved there), since #321 is merged.

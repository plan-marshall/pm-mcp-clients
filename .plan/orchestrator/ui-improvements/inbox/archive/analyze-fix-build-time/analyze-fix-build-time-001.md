envelope_version=1
sender_type=plan
sender_id=analyze-fix-build-time
epic=ui-improvements
kind=finding
created=2026-11-14T11:27:06Z

# Open questions after rebasing analyze-fix-build-time onto main

Plan `analyze-fix-build-time` (branch `feature/analyze-fix-build-time`, PR #432) was rebased onto main
after #422–#429 landed. Where the branch and main's rewrites changed the same code, main's design
stands and the branch's change was adapted to it. The adaptations below need a decision; the PR is
landing as it is and these belong to a follow-up plan.

## Decisions needed

1. **Scripted "chat unavailable: no role qualifies" start fixture is gone.** Main moved start chat
   into `ChatRegistry`; the old `StartRegistry` chat, including the fixture member
   `chat.available: false` that produced `start.chat.role_unqualified`, went with it, and so did the
   branch's tests for it. `StartStateResource.CHAT_ROLE_UNQUALIFIED` still exists in pm-api and
   nothing in the mock sets it. Rebuild it on `ChatRegistry`, or retire the constant?

2. **`AnswerCommand` merges two designs.** The branch's locale-keyed keys and read-before-send
   (a decision the runtime does not return, or one with another id, is its refusal) sit on top of
   #426's rewrite. Consequences to settle against `03-dialogues.adoc`:
   - `operator.decision.parts_unreadable` is now unreachable (a failed first read is an
     `OperatorRefusedException`, HTTP status shown); main's catalogue test still requires the key.
     Remove the text and the test, or restore a fail-closed second read?
   - End of input at any prompt leaves the dialogue (`Left Q-21`), as main's `readLineSafe` does.

3. **Cleanup has two message catalogues for one key.** `operator.cleanup.not_in_job` exists in
   `system.*.properties` (`{id}`) and in `operator.*.properties` (`{item}`); the code uses the second.
   Delete the stale copy. `--exclude` without `--delete` now exits 2 for every other form (one guard,
   main's); confirm against `26-cleanup.adoc#t-remind`.

4. **`!` on a needs item runs the command the row prints (#426).** The branch's reads of the
   subject's act (plan holds, plan decisions, opening the plan view in the view) no longer apply; its
   tests now assert the printed command. Confirm that opening nothing in the view is intended for a
   plan's hold, failed landing and condition.

5. **Start view and chat events.** `StartView` refreshes the entry on the entry stream's events only;
   `chat.turn.*` and `chat.proposal` go to the chat's own read, and `chat.ended` refreshes nothing.
   Does the entry change when a chat ends?

## Build and test findings

6. **The quality gate rewrites `@CsvSource({"''"})` to `@ValueSource(strings = {"''"})`.** That turns
   an empty answer (Enter) into two apostrophes and broke `ControlDialoguesTest.planBackToCut`. The
   recipe lives in the parent POM (`pm-mcp-parent`). Report it there, or add a guard.
7. **`-Pcoverage,integration-tests` in one invocation under-reports.** The agent is prepared twice
   and the second run overwrites `jacoco.exec`; pm-operator read 63% instead of its real 94.2%
   instructions / 81.8% branches. Document the two profiles as separate runs, or fix the build.
8. **pm-operator branch coverage has a thin margin** (81.8% against 80%). Targeted tests for the
   least covered branches, chosen for behaviour and not for lines, belong in the follow-up plan.
9. **`/dev/components` page test timeout.** The file now sets `testTimeout` 20 s because it renders
   the whole showroom and exceeded 5 s beside the full suite. A suite-wide policy for heavy render
   tests may be better than a per-file setting.
10. **Route table in `client.test.ts` lost its chat rows.** Main's dedicated chat tests cover the entry
    and subject routes with real responses; check that the epic and reject variants of the subject
    chat routes are covered there too.

envelope_version=1
sender_type=plan
sender_id=analyze-fix-build-time
epic=ui-improvements
kind=finding
created=2026-11-14T19:04:12Z

# Deferred after landing analyze-fix-build-time as three pull requests

The branch of `analyze-fix-build-time` (PR #432) was too large for CodeRabbit (226 files, limit 100) and for
Sourcery (20,000 lines) to review, so it landed as three pull requests: #434 (build, pm-api, pm-mock-runtime),
#435 (pm-operator) and #436 (pm-web-app). The functional findings of the reviews were fixed with tests. The
items below were deferred on purpose; they are style or maintenance work, not defects, and belong in one
follow-up plan.

## From the review of #435 (pm-operator)

1. **Test input through `Generators`.** About 28 findings ask for hand-written literals that are input, not
   expectations, to come from `Generators` (`de.cuioss.test:cui-test-generator`), with
   `@EnableGeneratorController` on the class. Sweep the pm-operator tests once instead of finding by finding.
2. **Offered keys in `EpicDecompositionDialogue`.** The keys a prompt offers are listed by hand next to the
   code that accepts them; derive the offered list from the accepted set so the two cannot drift.
3. **Duration kinds in `CleanupCommand.durationsText`.** The sequence of kinds is a hand-written list. Deriving
   it from `CleanupKind.values()` needs a mapping, because the wire values (`plan`, `read_reports`,
   `unenrolled_project`) differ from the displayed labels (`plans`, `reports`, `unenrolled`).
4. **Parameterize-test nitpicks** (three or more variants as `@ParameterizedTest`).
5. **Console settings transcripts.** `console-settings(-de).transcript` is captured with onboarding still open
   when `config` runs; capture it after leaving onboarding.
6. **Test for `refreshPoppedLevel()` after the plan and epic jumps** in `OverviewFullScreenApp`
   (`jumpToPlanProject`, `jumpToPlanEpic`): the call was added, its effect on the level shown has no test.

## Carried over from the first message (001)

7. The quality gate also reformats files it has no business with (for example
   `pm-api/.../ReportRecordsTest.java`, whitespace only); each full gate run dirties unrelated files, so
   they have to be reverted by hand. Same recipe family as item 6 of message 001, in the parent POM.
8. The decisions of message 001 (items 1 to 5 and 10) are still open if they were not taken on drain.

## Open points of the landing itself

9. CodeRabbit's free review quota ran out while #436 waited; one review per pull request is the merge rule, so
   a part that lands after the quota is used waits for its reset (about 30 minutes). Consider a quota-aware
   order of the parts, or a smaller first part.

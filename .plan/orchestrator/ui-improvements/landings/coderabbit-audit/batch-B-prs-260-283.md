# CodeRabbit audit, batch B (PRs 260, 262, 266, 269, 270, 273, 274, 275, 276, 277, 281, 282, 283)

Method: read every `coderabbitai` review BODY (nitpicks, "Additional comments", "Follow-up(s)", "Follow-up Comments"),
every inline thread (all 13 PRs: no unresolved thread anywhere), and every operator/bot issue comment. Each finding was
checked against the PR merge commit and current `main` by `git show` / `git grep`. Nothing was built or run.
"Handled" = fixed in a pushed commit (verified in code), or answered with a stated reason in a reply/PR comment.
No PR carries the label `skip-bot-review` (all label lists are empty).

Summary of unhandled findings still present on `main`:

| PR | findings checked | unhandled | still on main |
|----|------------------|-----------|---------------|
| 260 | 5 | 1 (partial, Trivial) | 1 |
| 262 | 8 | 0 | 0 |
| 266 | 8 | 0 | 0 |
| 269 | 17 | 2 | 2 |
| 270 | 9 | 1 | 1 |
| 273 | 4 | 0 | 0 |
| 274 | 21 inline + 7 body | 0 | 0 |
| 275 | ~24 | 12 | 12 |
| 276 | 0 (no CodeRabbit review exists) | 0 | 0 |
| 277 | 9 | 0 | 0 |
| 281 | 3 | 0 | 0 |
| 282 | 6 | 0 | 0 |
| 283 | 2 | 0 | 0 |

PR 275 is the one with the real gap: the operator posted one blanket "Fixed." comment on the threads, CodeRabbit
answered three of those threads with "still not fixed" (and the review bodies carried six follow-ups), and nothing
followed; the threads were then resolved.

---------------------------------------------------------------------------------------------------------------------

## PR 260 (PLAN-25 control components) merge 8b19a4a7

CodeRabbit reviews: df9fb083 (2 findings), 1d494f91 (1 nitpick); two empty reviews at 7d47877a. 5 findings checked
(2 inline threads, 1 nitpick, 1 "Additional comments" Major, 1 nitpick in review 2).

Handled (verified): `devRoute` gated on `import.meta.env.DEV` (+layout.svelte:49); `dev.*` keys answered with reason in
thread (CodeRabbit withdrew); Chip clipboard fallback (`Chip.svelte` uses `document.execCommand('copy')`) and its two
tests (`Chip.test.ts:73,98`); `GENERATED_NODE` regex aligned with `CLIENT_MANIFEST` and fixture added
(`devRouteExclusion.ts:24,27`, test line 161).

### NOT HANDLED (partial)
- PR 260, review df9fb083, `pm-web-app/src/lib/dev/devRouteExclusion.ts:24-30`, Trivial, Stability & Availability
  (cr-comment cf115ea6b114edb27954a9fb).
  Finding has two asks: (1) align the node matcher with the manifest matcher; (2) "Add a build-output check that fails
  when emitted code still contains a development-page marker or a retained `/dev` route".
  (1) is done. (2) is not: the operator's 23:12 comment lists the finding as "fixed" but only describes (1). At merge
  the tests only exercise `stripDevRoutes`/`isGeneratedNode` on fixtures plus a check that `devRouteExclusion()` is
  registered in `vite.config.ts`; nothing inspects real build output. On main: `git grep` for a bundle/dist scan in
  `pm-web-app` (`vite.config.ts`, `package.json`, `src/lib/dev`) and `pm-web-e2e` finds none. Still present, low impact.

---------------------------------------------------------------------------------------------------------------------

## PR 262 (brand mark, favicon) merge 4c39c0f8

Four CodeRabbit reviews, all test-only nitpicks (Trivial): 8 findings checked, 0 inline threads.
All handled: a69bc1e2 (QR payload: `PairingView.scenarios.test.ts:200-202` compares drawn modules with
`qrcode-generator` output, reason given for not mocking `addData`), f8c36f25, 36767d4d, 36b73339, ae7b8e6b fixed in
`favicon.test.ts` (verified: `dotOf(href)`, `FAVICON_*_HREF`, radius/viewBox bounds, painted colours); b6726aef, bf2be272,
427de384 answered with stated reasons in the 14:52 PR comment ("filed as a follow-up"). No unhandled findings.

## PR 266 (core container components) merge 0351ce78

8 findings (4 body nitpicks, 4 inline threads; CodeRabbit marked all 4 threads "Addressed in commit 2b16afe").
Verified at merge: `Card.test.ts` nested heading/body and `Refresh` button assertions; `KeyValueList.test.ts` DT/DD
order assertion; `Breadcrumb.test.ts:124` "keeps the current page as text even when it carries an address" covers the
final-crumb `href` case; `Dialog.svelte:74` filters `:disabled`; dev page grid `minmax(min(260px, 100%), 1fr)`. No operator
comments were needed. No unhandled findings.

## PR 269 (mock plan details per id, lifecycle) merge bae3a8c8

Reviews 2fabdbe2, 71b86c65, f86f4846, 65d201ef. 17 findings checked (8 threads, 2 follow-ups in 2fabdbe2, 1 follow-up
in f86f4846, 2 in 65d201ef, 9 nitpicks incl. duplicates). Most fixed and verified (phase node run_count, `isStopped`
cleared on drain, `knowsPlan` status < 400, held-phase mapping, no-template phase node, test assertions, `uniquePlanId`
vs fixture ids, adopt PR title). Draining reconcile follow-up answered with a reason (operator 18:08 comment).

### NOT HANDLED 1
- PR 269, review 71b86c65, `pm-mock-runtime/.../MockRuntimeEngine.java:~596` (thread 223773e69e0b85b5fb0f988e),
  Minor, Functional Correctness: "Clear `startedPlans` when the run resets". `initDefaultStart` clears `startedPlanIds`
  but not `startedPlans`, so after `reset()` / `goToState()` / scenario switch `planSummary` still serves the old plan and
  `/process` returns 200.
  Evidence at merge: `startedPlans` only has `put` (3749, 3873, 3952) and `get` (8421); `initDefaultStart` (2770-2778) has no
  `startedPlans.clear()`. The operator replied only to the second finding in the same comment ("Not applicable",
  see below); the reset half got no answer. Still on main: field line 643, puts 3802/3926/4005, `initDefaultStart`
  2823-2833 clears `startedPlanIds` only; no `startedPlans.clear()/remove` anywhere in `pm-mock-runtime/src/main`.

### NOT HANDLED 2
- Same thread, second finding in the same comment: "Register plans created through the start-entry `create` act".
  `StartRegistry.create` (main `StartRegistry.java:1085-1105`) assigns `entry.planId` and broadcasts `plan.started` but
  nothing records it in `startedPlans`; `handleEntryAct` (main `MockRuntimeEngine.java:3611`) just returns the state.
  So a plan created by that path is 404 at `/process`. The operator answered "Not applicable: the plan control surface has
  no `create` act" (19:16). CodeRabbit replied (19:17) that this misread the finding (it concerns the start-entry
  surface, `StartRegistry.java:658/1090/1100-1104`) and "the finding still applies". The thread was then resolved with
  no further reply and no code change. The reason given does not address the finding. Still present on main (code above).
  Severity Minor, test-fidelity only (mock runtime).

---------------------------------------------------------------------------------------------------------------------

## PR 270 (event contract, pushed notices, prompt badges) merge 7e2b4b61

Reviews a0820cde (+6 empty). 9 findings checked (6 threads, 2 follow-ups, 1 nitpick). Handled/verified: waiver
counting, envelope scope for hold notices, waiver refinement on type fallback, raw `type` for report refresh, mock decision
event names now derived from `EventName` (MockRuntimeEngine.java:323-325 at merge), `ConsoleNoticeIT` now asserts the
3 to 4 badge growth and re-broadcasts until drawn (`broadcastUntilDrawn`, the race CodeRabbit re-raised is covered by the
retry), `ScreenCaptureIT` recorder answered with reason (CodeRabbit withdrew). No PR comments exist for 270; all
answers are in-thread.

### NOT HANDLED
- PR 270, review a0820cde, `pm-operator/src/main/java/de/planmarshall/operator/EntryFlow.java:451-458` (merge), Minor,
  Functional Correctness, "Heavy lift": "Add badges for open project onboardings". `EntryFlow.badges` builds only the
  machine setup badge and the `needs_operator` badges; it never reads open project onboarding runs or calls
  `ConsolePrompt.formatProjectSetupBadge`, so the prompt can omit required project setup badges. CodeRabbit noted the
  behaviour predates the PR, but the spec (04-console.adoc#prompt) is what the PR claims to implement. No reply, no fix.
  Still on main: `EntryFlow.badges` (~505-518) unchanged; `git grep formatProjectSetupBadge main` hits only its
  definition in `ConsolePrompt.java:153/166` and two tests, i.e. the method is never used by production code.

## PR 273 (waiting plan decision first in menu) merge a07a09b6

4 findings, 0 unhandled. `d3d70f2e`/`faa3a955` (full-screen test for the Decide entry, same finding twice): answered with a
detailed reason (static `PmOperator.execute` seam, OperatorGate, recorded as open item) at 09:10; `30a0679d` (derive
WAITING_STATES): answered with reason at 16:24; thread 977d83f3 (failed decision read silent) fixed in 8a3cbea
(CodeRabbit confirmed).

## PR 274 (app shell) merge 44b051ff

3 body-bearing review rounds + 21 inline threads; 28 findings checked, 0 unhandled. All 21 threads fixed (replies
cite commits; CodeRabbit marked them "Addressed"); one declined with reason (stream `connected` after first byte).
Review-body items verified in code: TopBar latest-request guard (`TopBar.svelte:57-68`), NavRail `canNavigate` gate in
forced states, pill test renamed (layout.test.ts:147), `mobile.spec.js` waits for route view before measuring overflow
and covers start/decomposition, offline-to-retry test added (operator comment 00:32), teardown-PID follow-up answered with
reason (00:32), SSE connect deadline added (comment 07:34, c01b358).

---------------------------------------------------------------------------------------------------------------------

## PR 275 (one state dialogue for both onboardings) merge fa178dec

Reviews 56e7dc6d, 27626c56, 351c15d5, 489e0204 (+5 empty). ~24 findings checked: 10 threads, 7 follow-ups
(1 in 27626c56, 6 in 351c15d5), 6 nitpicks, 1 "Additional comments" (unverified).

What happened: the operator pasted ONE identical comment ("Fixed. The setup badge is rebuilt ... `projectStepOf` maps ...
the two nitpicks are covered by the new assertions") into threads 2fed38, 6de88f, 92c2f4, 4a028f, 9fb6eb. CodeRabbit then
replied in each that its specific concern was still not fixed (05:24-05:27). No further commit or reply followed; threads
were resolved. Follow-up findings in review bodies got no reply at all, except the two test nitpicks answered at 06:26.

Handled/verified: sanitized state line (`OnboardCommand.printStateLine`), unenrol clears setup state
(`MockRuntimeEngine` 7398), id decoding in both setup branches (URLDecoder at 7060/7114), badge refresh after dialogue,
overview steps derived from `OnboardingAspect` (`StateDialogue` 399), `OnboardCommandTest` link assertion, pilot assertion
in `ProjectSetupDialogueIT`, `projectStepOf` extended for checkout/enrol/own states, two nitpicks (POST case, HTTP
method) answered with reasons at 06:26.

### NOT HANDLED (all verified still present on main)

1. Thread 2fed38, `MockRuntimeEngine.java:605` (merge) / `:637` (main), Minor, Functional Correctness:
   "Clear `projectSetupStates` when the scenario resets or switches". CodeRabbit re-stated it (05:24); no answer. Main:
   only `remove` at 7954 (unenrol) and `put` at 7993; no clear in `initDefaultProjects`/`reset`/`setScenario`.
2. Thread 6de88f, `MockRuntimeEngine.java:7450` (merge), Minor, Functional Correctness: "Sync enrolled readiness when
   identity setup completes". Main `handleSetupProject` (~7985-7995) stores `project.completed` but never updates the
   `EnrolledProject`; explorer/project reads keep old readiness. CodeRabbit re-stated (05:25); no answer.
3. Thread 92c2f4, `StateDialogue.java:287` (merge), Minor, Functional Correctness: "Name the project on the final
   frame". `content()` (line 286) skips `frame.summary()` when the state has a catalogue title; `project.completed.title`
   is generic ("The project is ready", `projects.en.properties:127`); the `{project} is ready` text exists
   (`project.ready.title`, line 126) but no production code uses it (`git grep` hits only `ProjectsCatalogueTest`).
   `ProjectSetupDialogueIT:90` asserts only the generic text. CodeRabbit re-stated (05:26); no answer.
4. Thread 4a028f, `EntryFlow.java:209` (merge), Minor, Stability & Availability: "Keep the entry result when the
   post-dialogue status refresh fails". Main `refreshedAfterDialogue` (213-217) calls `StatusCommand.fetch` which throws
   `IOException`; not caught. CodeRabbit re-stated (05:26); no answer.
5. Thread 9fb6eb, `PmConsole.java:285` (merge), Minor, Functional Correctness: "Update the window title after the setup
   dialogue". Main `refreshSetupBadge` (286-292) replaces badges only; `updateWindowTitle` is called at 138 and 575
   (`refreshBadges`) but not from `continueOnboarding`/`refreshSetupBadge`. CodeRabbit re-stated (05:27); no answer.
6. Follow-up, review 351c15d5, `PmConsole.java:495-496`, Minor, Functional Correctness: "Read the runtime state after the
   event stream reconnects". Main `startEventListener` (~450-475) only flips `reconnecting`; no status read on reconnect.
   No reply.
7. Follow-up, review 351c15d5, `MockRuntimeEngine.java:6060-6070`, Minor, Functional Correctness: "Read the `question`
   member and the outer `id` in `applyStateEffectOf`". Main (6561-6585) still reads only the `decision` member and the
   inner `id`. No reply.
8. Follow-up, review 351c15d5, `PmConsole.java:266-268`, Minor, Potential issue (Low value): "`refreshSetupBadge` runs only if
   `dialogue.run()` returns; leaving with `q` after a step bypasses the refresh". Main `continueOnboarding` unchanged
   (refresh at 267 inside the same try, `StartLeftException` caught at 269). No reply.
9. Follow-up, review 351c15d5, `EntryFlow.java:483-484`, Minor, Maintainability: "Derive the badge total from
   `OnboardingAspect`, not the hardcoded `STEP_KEYS`". Main `EntryFlow.badges` (515) still uses `STEP_KEYS.size()`; no test
   ties `STEP_KEYS` to `OnboardingAspect.values()` (the operator's "steps are derived from OnboardingAspect" applies to
   `StateDialogue` only). No reply.
10. Follow-up, review 351c15d5, `StateDialogue.java:621-626`, Minor, Maintainability ("Poor tradeoff"): "Derive the project
    step mapping from its state definition, not a hardcoded switch". The switch was extended but is still hand-listed with
    `default -> DONE` (main line 629), no doc/derivation test. Weak: a related thread was fixed, so classify as partial.
11. Follow-up, review 351c15d5, `MockRuntimeEngine.java:7113-7116`, Minor, Functional Correctness: "Accept only the `set`
    link in project setup". Main `handleSetupProject`: `if (linkName != null && PROJECT_SETUP_IDENTITY.equals(current))`
    completes for any link name. No reply.
12. Nitpick, review 351c15d5, `DialogueSourceTest.java:97-101` (cr-comment 4c18009365397f44cc1db8a0), Trivial: "Match
    the sent link value in the project setup fixture". Main line 96 is still `contains("\"link\"")`, not
    `contains("\"link\":\"set\"")`. Not among the two nitpicks the operator answered (06:26).
13. "Additional comments" (unverified finding), review 56e7dc6d, `StateDialogue.java:177-183`, Minor: a linkless non-final
    frame times out after 60 s and `run()` returns it normally, so `onboard`/`project setup` can exit 0 before setup
    completes. Merge/main `OnboardCommand.java:112-113` returns 0 after `dialogue.run()` regardless of the returned frame.
    CodeRabbit marked it unverified and it prints a "waits" notice, so this may be intended; no reply either way.

Count: 12 listed as unhandled (items 1-9, 11, 12, 13), item 10 is partial. All are Minor/Trivial; none Critical/Major.

## PR 276 (one state line per onboarding state) merge 44803a77

No CodeRabbit review was ever posted (only "Review rate limited" notices on `@coderabbitai review` requests; `reviews`
empty). Nothing to audit. Note: no review means nothing was reviewed by CodeRabbit for this PR at all.

## PR 277 (operator gate on acts only) merge 5348e89e

9 findings (6 threads, 1 nitpick, 2 follow-ups), 0 unhandled. Operator comments 12:43 and 13:07 map each to a commit; verified at
merge: `OperatorActs.webAct` uses `equalsIgnoreCase` (line 136) and derives subcommands from the `@Command` annotations
(`subcommandsOf`, 72-84, 99); `OnboardingSource` takes and verifies `OperatorGate`; `WebCommand.java:304`
`changesADevice() || interactive`; `OperatorGateTest` asserts both refusal phrases.

## PR 281 (conform web modules) merge bf8ddb31

3 findings, 0 unhandled. Two textGuard threads fixed (CodeRabbit confirmed); the follow-up "Correct the Maven note in
`//test:ci-strict`" is already correct at merge and on main (`pm-web-e2e/package.json:16` says Maven runs
`test:ports-guard`; `pom.xml:73-79` confirms).

## PR 282 (CUI standards foundation) merge e83a9ee2

6 findings, 0 unhandled. Three threads fixed/answered (cui-java-tools scope declined with a verified reason, CodeRabbit
withdrew). Nitpicks: `PmMcpTest.java:200` `plain-` prefix and `LinuxConfinementTest.confined` `home` assertion fixed
(verified); `afterEach` close-failure test answered with a reason in the 23:30 comment.

## PR 283 (one text set per API error code) merge 1b6c6ae3

2 follow-ups, 0 unhandled: `ErrorTextsTest.java:27-28` derives `LOCALES` from `MessageCatalogue.SUPPORTED_LOCALES`;
`nullCodeFallsBack` (lines 133-135) asserts the null `String` wire value for title, cause and fix (verified at merge).

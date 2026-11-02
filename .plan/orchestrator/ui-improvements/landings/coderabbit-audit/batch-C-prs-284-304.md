# CodeRabbit audit, batch C (PRs 284 287 288 291 292 293 296 297 298 299 300 301 302 303 304)

Method: all CodeRabbit review bodies (author coderabbitai) read, each body finding (cr-comment marker or section without marker) and each inline thread (resolved/unresolved, resolvedBy, "Addressed" tag appended by CodeRabbit) compared to code at the merge commit and at current main. Thread replies: none of the audited threads carry a human reply unless stated. "ADDR" = CodeRabbit itself appended "Addressed in commit" to the thread (accepted as fixed unless stated otherwise).

## PR 284 (merge 7d7b4d71) feat(api) needs-and-find
Reviews: 3b5e96eb (10 inline + 1 follow-up + 1 nitpick + 1 "additional comments" finding), 9ef43724 (1 follow-up + 2 inline), 4150b8bb (4 inline + 2 follow-ups + 2 nitpicks). 17 inline threads (13 resolved, 4 unresolved), ~25 findings checked.

UNHANDLED (all still present on main; no reply, no fix):
1. 4150b8bb, MockRuntimeEngine.java:5764 (T13, UNRESOLVED thread), Minor / Stability & Availability. `collectRunningProjects` iterates `enrolledProjects` without `projectsLock` (writers hold it) -> ConcurrentModificationException possible during find. Merge commit: `for (var project : enrolledProjects)` at ~5764 with no synchronized block. Main: same (MockRuntimeEngine.java:5769-5772).
2. 4150b8bb, MockRuntimeEngine.java:5801 (T14, UNRESOLVED), Minor / Functional Correctness. `initDefaultStart()` clears `startedPlanIds` but not `startedPlans`, so find returns plans from the previous run after reset()/setScenario(). Merge: initDefaultStart (line 2816) has no startedPlans.clear; main: initDefaultStart at 2823-2833 still no clear; `git grep startedPlans` shows only put/get/iterate.
3. 4150b8bb, MockRuntimeEngine.java:5842 (T15, UNRESOLVED), Minor / Stability. `collectOpenDecisions`/`collectReports` read `openDecisions` and `mockReports` without `decisionsLock`/`reportsLock`. Merge and main (5847-5866) identical, unlocked.
4. 4150b8bb, MockRuntimeEngine.java:5846 (T16, UNRESOLVED), Minor / Functional Correctness. `collectOpenDecisions` passes raw decision subject ("plan fix-relay") as the plan location id instead of the stripped plan id (`extractPlan(decision)` exists, MockRuntimeEngine main:8693). Main still `location(decision.subject(), null)` at 5853.
5. 4150b8bb review body (follow-up), MockRuntimeEngine.java:5773-5779, Minor / Functional Correctness. Find returns an unenrolled project from the static GET /api/v1/projects fixture fallback in `collectRunningProjects` (the fallback loop admits any fixture row not in `seen`). Same code on main (5778-5788). Body-only, never answered.
6. 4150b8bb review body (follow-up), MockRuntimeEngine.java:5799-5800, Minor / Functional Correctness. Epics started via `handleStartEpic` are recorded only in `startedEpicIds`; `collectRunningEpics` reads only the static /api/v1/epics route, so find misses a started epic. Main collectRunningEpics (5793-5802) unchanged, no started-epic store (git grep startedEpic* only startedEpicIds).
7. 4150b8bb review body (nitpick, Trivial), NeedsAndFindScenarioTest.java:193-200. Assert order of every find match for FIX-RELAY (loop with seenEnded). Merge test matchesByIdAndTitle has only first/first-ended asserts; main same (the seenEnded loop at main:255 belongs to a different, older test).
8. 4150b8bb review body (nitpick, Trivial), NeedsAndFindScenarioTest.java:213-216. Assert the ended predecessor `fix-relay-v1` in the "214" search. Merge and main: findsByPullRequestNumber has no assertion on the ended match.
9. 3b5e96eb review body (follow-up), MockRuntimeEngine.java:5655-5661, Minor / Functional Correctness (CodeRabbit: "mock fidelity gap, not a production defect"). `serveNeedsOperatorPage` serves only the fixture items of the active route, so answering a decision / resolving a hold does not change the list. Main unchanged. Low value, but never answered.
10. 3b5e96eb inline T2 (resolved by cuioss-oliver, NO "Addressed", no reply), NeedsOperatorItem.java:47, Minor / Data Integrity, Heavy lift. "Expose the stable item identifier required by the needs-list contract" (design says selection stays on the item's id). `NeedsOperatorItem` record at merge and on main has no id field (kind, link, project, epic, plan, since, blocks). Resolved without code change and without a stated reason. Caveat: the contract text lives in the other repository; the design doc here (shared/needs-and-find.adoc:46) still says "selection stays on the item's id".

HANDLED (verified): ChangeSize partial-object finding (fromMap now returns null unless all four figures, ChangeSize.java at merge); findKindRank (replaced by FindKind ordinal sort in serveFind); 792ce00a serveFind project matches from enrolledProjects (collectRunningProjects reads enrolledProjects first); T0-T12 carry CodeRabbit's "Addressed" tag (spot-checked: undated rows, decisions/reports in find, started plans in find, ended pages, ended-row kind mapping, needs-changed override, needs rows locations, oldestActiveWorkStart skip ended, non-2xx status passthrough at serveNeedsOperatorPage, invalid_query on malformed q at serveFind all present at merge).

## PR 287 (merge 5f9e60da) feat(web) display layer
Review 647d2370: 7 inline (all resolved, 6 ADDR, 1 resolved by oliver) + 1 Major follow-up + 3 nitpicks = 11 findings checked.

UNHANDLED, still present on main:
1. Review body follow-up, MachineSettingsView.svelte:350-352, Major / Functional Correctness / Heavy lift. The reminder input binds `cleanupRemindDays` but `saveCleanup` sends only `cleanupDays` under `cleanup_durations`; the reminder value never reaches the server although the page shows "saved". CodeRabbit noted it predates the PR. Main: MachineSettingsView.svelte:87-89 `await applySetting('cleanup_durations', String(cleanupDays));`, input still bound at :352. No reply, no later fix.
2. Review body nitpick, OnboardingView.svelte:189-194 (test: OnboardingView.scenarios.test.ts completed-card test), Trivial. No assertion on `p.state-card__summary` (summary paragraph differs from title). Merge and main test only assert heading + completed-steps + no re-run button.

HANDLED: DecisionView.test.ts:429 (key `rebase` is sent, main/merge line 425); enumGuard.test.ts:49-52 (aria-label/title/placeholder/alt scanning present, enumGuard.test.ts:183); T4 gap activity label (resolved by oliver, no tag, but `state.activity.gap` in common.*.properties and `displayState('activity','gap')` asserted in index.test.ts:49); T0-T3,T5,T6 ADDR.

## PR 288 (merge 1651554b) test(pm-api) Generators
One CodeRabbit review (0860ba13, body-only, 2 Trivial follow-ups). No inline threads. 2 findings checked, both HANDLED:
- DecompositionRecordsTest.java:136-141 secondId made different by construction (`firstId + "-second"`, at merge).
- GitSourceRecordsTest.java:117-119 deterministic exhausted-limit round trip added (`new RateLimitStatus(limit, 0, ...)`, present at merge and main).
No unhandled findings.

## PR 291 (merge 9890d74b) feat(web) one time format and locale switch
Reviews 12bf4612 (12 inline + 1 follow-up), bd70e3a9 (1 follow-up), 79af7e40 (6 inline + 4 nitpicks), 1f10f52b (empty, rate-limit). 18 inline threads (all resolved) + 6 body findings = 24 checked. Note CodeRabbit's rate-limit issue comment of 19:38 carries the same six findings as the 79af7e40 review (no extra findings).

UNHANDLED (still present on main), both Trivial, never answered:
1. 79af7e40 body nitpick, pm-web-app/src/lib/display/format.ts:212-214 (now :264), Trivial / Functional Correctness. `formatTokens(count, floor=true)` returns '0' for count<=0 instead of the floor form '≥ 0' (CodeRabbit: predates the PR, "follow-up"). Merge and main: `if (count <= 0) return '0';`.
2. 79af7e40 body nitpick, pm-web-app/src/lib/i18n/index.ts:317-318, Trivial / Functional Correctness / Heavy lift ("Optionally retain older catalogues for rollback recovery"; predates the PR). Merge and main: with a known version, every sibling key of the locale is evicted (`dropCachedCatalogue(storage, stale)` at main:317-318). No reply.

HANDLED: bd70e3a9 Generators/@EnableGeneratorController finding for OnboardingScenarioTest: answered with a stated reason by cuioss-oliver in a PR comment (2026-10-31 21:38, "not applied in this plan ... left to a follow-up"). Note: OnboardingScenarioTest carries @EnableGeneratorController on main (line 55) anyway.
HANDLED (verified in code at merge): ReportsView relative ages (clock state + setInterval, ReportsView.svelte:30-31, 234), sent_at fallback (`formatInstant(...) || item.sent`), PlanView day-header clock (clock/ setInterval, dayLabel uses `new Date(clock)`), PlanView different-years test (2026-09-01 vs 2025-09-01 entries), OnboardingScenarioTest 'Deutsch, models Deutsch' summary assertion, adoptRecordedLocale whole operation bounded (withinBound around adoptRecordedLocaleUntimed), readCatalogueVersion keeps the learned version on failure, SettingsView last-used precise timestamp as visually-hidden text (SettingsView.svelte:314), T17 SystemView year finding: answered with a reason by cuioss-oliver and withdrawn by CodeRabbit. T0-T6, T8-T11 ADDR.

## PR 292 (merge 66c1495a) feat(operator) runtime acts confirm
Reviews 54f21b40 (7 inline + 3 body), 62490b7a (1 inline + 2 body), ac62e64f (4 inline + 3 body), b28e0187 (0 inline + 2 body), a67ef02e (2 inline + 3 body). 14 inline threads (all resolved, all tagged Addressed) + 13 body findings = 27 checked.

UNHANDLED, still present on main:
1. b28e0187 review body (follow-up), RuntimeActDialogue.java:129 (now :160), Minor / Stability & Availability. "Add a read deadline to the snapshot request before the confirmation prompt": `ask()` reads GET /api/v1/system before drawing the timing prompt; the unix-socket client has no read deadline, so a runtime that accepts and never answers blocks the stop/restart/upgrade confirmation. Merge: RuntimeActDialogue.snapshot() = `context.runtime(false).send(HttpRequest.get("/api/v1/system"))`, no timeout; main: same call (RuntimeActDialogue.java:160, 277), `git grep -i timeout` over pm-api http package and RuntimeActDialogue finds nothing. No reply. (The sibling finding 62490b7a/6f767308 for the --no-input path is moot because --no-input no longer reads the snapshot; only the interactive path remains open.)

ANSWERED WITH A REASON (counted as handled, but the residue is real): d45ca865 (a67ef02e body, "Ctrl+C exits 0 in the other ChoicePrompt verbs": WorkActionDialogue, ReviewDialogue, HoldDialogue, ProjectCommand, PlanCommand, OnboardCommand, ActionSupport x2). Answered by cuioss-oliver 2026-11-01 10:40: out of this plan's surface, recorded as landing residue. Taken up by PR 303 (see there for what that PR still left open).

HANDLED (verified at merge): 821f5564/3f2d20bd refused socket reports not_answering with exit 1 (RuntimeActDialogue.notStopped/provenStopped, StopCommand returns refusal.exitCode()); 48f56c92 shell stop return documented (doc/design/terminal-client/25-system.adoc:164, "the verb returns when the runtime accepted the stop, exits 0"); e7057bc6 Ctrl+C -> 130 for the runtime acts (ChoicePrompt.InterruptException caught first, StartPrompt.EXIT_INTERRUPTED); test nitpicks 0a28cb6f (PmOperatorTest no stop request), 133632ac (restart no-input `server.requests().isEmpty()`), 9ee9972d (`Chosen:` negative assertion), 5e6a29d9 (`deploy` job kind in snapshotJson), 9f9ddf6b (RuntimeStopDialogueIT `shown` offset + assertFalse), 9cd52304 (`"now":true` negative assertions). Also CodeRabbit's rate-limit issue comment (10-31 19:46) repeats two findings (snapshot IOException catch, leading-separator in running()); both fixed (catch IOException in ask(); `DOT.strip()` loop).

## PR 293 (merge 418579a0) test(mock-runtime) Generators
Reviews 5f7792e2 (12 inline, no body findings), 5965c0d3 (empty), 80634078 (3 body nitpicks). 12 inline threads (all resolved, all Addressed, one carries the human reply "Fixed in 5965c0d3") + 3 body = 15 checked. No unhandled findings.
Body findings verified at merge: ScenarioRegistryTest.java:72-73 and :130-133 (state-/scenario- prefixes present, lines 73, 130, 131); MockRuntimeEngineTest.webProtectedAuthentication runtime token on web listener -> 401 (res4, lines 405-406).

## PR 296 (merge d7f7ffc8) feat(web) command palette
One review 42aee47d: 6 inline threads (ALL STILL UNRESOLVED on GitHub, no replies) + 3 body findings (1 Minor follow-up, 2 Trivial nitpicks). The PR merged believing the review had no findings. PR 300 (fix PR) fixed the six inline findings; its body maps each to the thread comment id.
INLINE (6), verified fixed on main by PR 300 (threads were never resolved or answered on PR 296 itself; the PR 300 description is the stated disposition, but it is on another PR):
 - CommandPalette.svelte:129 rows order (matches first, acts after: CommandPalette.svelte:125-128 on main)
 - :350 touch close control (`data-testid="command-palette-close"`, :368)
 - :365 combobox/listbox/option + aria-activedescendant (:382, :401, :415)
 - DecisionView.svelte:105 requestedDecisionId reset (:103-108)
 - :112 filter cleared when it hides the requested decision (:114-120)
 - :120 scroll deferred with tick() (:125)

UNHANDLED, still present on main (all in the review BODY, never answered, not in PR 300):
1. 99b60263, CommandPalette.svelte:190-193 (now :204-205), Minor / Potential issue / Heavy lift, marked "Unverified" by CodeRabbit. Decision navigation passes only `match.id` (`pushRoute('decisions', { decisionId: match.id })`), discarding the plan location; if decision ids are plan-scoped, DecisionView's id-only lookup could select the wrong decision. Main CommandPalette.svelte:204-205 unchanged. Speculative: depends on whether decision ids are unique across plans.
2. 713e6586, pm-web-app/src/lib/api/client.test.ts (findWork abort test, was :718-724, now :714-729), Trivial. Test does not assert that `findWork` forwards its `signal` to `fetch`; main test still only checks the rejection and `onOffline` not called (no `.signal).toBe(controller.signal)`).
3. 44e14b6a, pm-web-app/src/lib/stores/runtime.test.ts (:76-78 of the review), Trivial. No unit test for `adoptRouteFromLocation` reading `?decision=` on /decisions. `git grep adoptRouteFromLocation` in *.test.ts on main: no hit; runtime.ts:923-926 holds the branch.

## PR 297 (merge c5bfb1ca) feat(a11y) accessibility baseline
Reviews f27e1161 (6 inline + 3 body), 21640dc5 (3 inline + 3 body; two empty re-posts). 9 inline threads (6 resolved+Addressed, 3 UNRESOLVED) + 6 body findings = 15 checked. Review f27e1161 was fully dispositioned by cuioss-oliver (comment 2026-11-01 18:40; two nitpicks answered with reasons: axe per theme, IconButton tablet label). The LAST review (21640dc5, 20:53) came after the last code commit of the PR (21640dc5 at 18:37; later commits are only merges of origin/main) and none of its six findings was answered or fixed. All six are still present on main.

UNHANDLED, still present on main:
1. 21640dc5 inline T7 (UNRESOLVED), DecisionView.svelte:659, MAJOR / Functional Correctness. `answerChatProposal` assigns the response to the shared `chat` even after the operator closed that decision's panel and opened another one's chat, so the new panel shows the earlier decision's transcript/proposal. Fix proposed: assign only when `chatDecisionId === decisionId`. Main DecisionView.svelte:659: `chat = await apiClient.answerDecisionChatProposal(decisionId, accept);` unconditional. (CodeRabbit: predates PR, same function edited by it.)
2. 21640dc5 body (Minor), DecisionView.svelte:633-634 (`takeProposedAnswer`). `chatOpen = false;` unconditional, so completing A's acceptance hides chat B's panel if B was opened meanwhile. Main DecisionView.svelte:683 unchanged.
3. 21640dc5 inline T6 (UNRESOLVED), ControlActionsBar.svelte:118, Minor. When a confirm sets `busy`, every action button is disabled; `closeDialog`'s fallback selects only `button:not([disabled])`, so focus stays on body and is not restored when busy clears. Main closeDialog (fallback `'[data-testid="control-actions"] button:not([disabled])'`) unchanged; no focus move after busy clears.
4. 21640dc5 inline T8 (UNRESOLVED), ProjectOnboardingView.svelte:102, Minor. `measureStepLine()` runs only on mount and window resize, not when `stepRows` or `$currentLocale` change, so after a locale switch the step line can overflow with tabindex="-1" (keyboard users cannot reach hidden steps). Main :85-87 only mount + resize listener.
5. 21640dc5 body nitpick, ProjectOnboardingView.test.ts (for :420), Trivial. No unit test for the overflow-dependent tabindex. `git grep scrollWidth|clientWidth` over *.test.ts on main: no hit.
6. 21640dc5 body nitpick, Tabs.test.ts (for Tabs.svelte:81-83), Trivial. No test for arrow keys with the tablist itself focused. Main Tabs.test.ts has no key event on the tablist element (only the `getByRole('tablist')` presence check, :33).

ANSWERED WITH REASON (counted handled): f27e1161 nitpicks d5a260a1 (axe in each theme) and a7bfb110 (icon-button label at tablet width); both answered in cuioss-oliver's 18:40 comment.
HANDLED: c41bcca3 (dialog takes focus when it opens, per the 18:40 comment); T0-T5 (Escape topmost panel, tablist selected start, decision id carried past await, tabindex only when overflow, non-gating axe violations logged) all Addressed; spot-checked Tabs.svelte:81-83 `from` fallback and DecisionView.svelte:655 decisionId capture on main.

## PR 298 (merge f5de8449) chore(pm-api) import order — label skip-bot-review
No CodeRabbit review exists; the bot posted "Review skipped: excluded label skip-bot-review" (2026-11-01 13:25). It was NOT a refusal for size (>100 files); it is the label. 0 findings, nothing to check. (Formatting-only PR.)

## PR 300 (merge 1ce32d80) fix(web) palette findings of PR 296 — label skip-bot-review
No CodeRabbit review (skipped by label, 15:13). 0 findings. Its content is the disposition of the six PR 296 inline findings (see PR 296).

## PR 302 (merge 5650c41e) feat(web) data components (PLAN-35) — label skip-bot-review
No CodeRabbit review (skipped by label, 20:39: "only excluded labels are configured"). 0 findings. Note: this is a feature PR with new web components that never got a bot review at all.

## PR 299 (merge be4995e5) feat(system) one system model (PLAN-26)
Reviews bf85c005 (13 inline + 3 body), 9498f1d8 (3 inline + 6 body), 1213f86a (0 inline + 2 body), four empty. 16 inline threads (13 resolved, 3 UNRESOLVED) + 11 body findings = 27 checked. cuioss-oliver posted two disposition comments (2026-11-01 15:54 for bf85c00, 18:35 for 9498f1d and 1213f86) that fix or answer-with-reason most findings (worker folding above eight workers, running count for job kinds without slots, LAN listener fallback, non-running status full-screen test, role-group fallback label, unmeasured count on lines with no measured tokens: all answered with reasons, treated as handled). The comments do NOT mention the six findings below; the first comment claims "the remaining findings of that review are covered by the list above", which is not true for cb7c3e65, 97e4306e, f34ec204.

UNHANDLED, still present on main:
1. 9498f1d8 inline T13 (UNRESOLVED), OverviewFullScreenApp.java:1715 (now ~1742), Minor / Functional Correctness. After a RuntimeDrainingEvent, a later status read of upgrading/stopping/stopped keeps the event-sourced "draining" line; the guard `systemBanner == null || !systemBannerFromEvent` keeps it for every non-running status. Fix proposed: keep the event line only when status is DRAINING. Main OverviewFullScreenApp.java:1742 unchanged.
2. 9498f1d8 inline T14 (UNRESOLVED), SystemView.java:319, Minor / Security & Privacy. `stateLabel` fallback returns the runtime state key unsanitised (`state.replace('_',' ')`) while the other runtime names pass through `clean(...)`; an escaped control sequence in a state key reaches the printed work line. Main SystemView.java:316-321 unchanged.
3. 9498f1d8 inline T15 (UNRESOLVED), WebCommand.java:451, Minor / Functional Correctness. `handleInteractive` wraps `processInteractiveChoice` in `catch (IOException | RuntimeException _) { /* ignore */ }`, so a failed rename/revoke/listener-off in `web devices list --interactive` prints nothing and exits 0. Main WebCommand.java:472-475 unchanged. (Different from 427977bb, which was fixed for the non-interactive path.)
4. bf85c005 body follow-up cb7c3e65, SystemLevel.java:348-353, Minor / Maintainability. `rootRows` pairs `SystemView.root` figures with a hardcoded `ids` list by index, duplicating `figureOf`'s ID-to-figure mapping (project rule: a list mirroring another set must be derived). Main SystemLevel.java:350 `var ids = List.of(ROW_RUNTIME, ROW_WORK, ROW_WORKERS, ROW_JOBS, ROW_MODELS, ROW_CLEANUP);`.
5. bf85c005 body nitpick 97e4306e, SystemRecordsTest.java:374-379, Trivial. No test where both `state`/`lifecycle_state` and both `drain_withdrawable`/`can_withdraw` are present with conflicting values. Main SystemRecordsTest.java:374-375 only passes `lifecycle_state`/`can_withdraw`.
6. bf85c005 body nitpick f34ec204, SystemFullScreenTest.java (drillsDownIntoASection), Trivial. No rendered-screen assertion for the Jobs build/test leaves; main drillsDownIntoASection (SystemFullScreenTest.java:357-387) asserts only "locks  3 held".

HANDLED (verified or dispositioned): T0-T3, T7, T8, T10-T12 Addressed; T4 (lifecycle line wording, fixed in e1c4a96, CodeRabbit confirmed), T9 (unmeasured call count, fixed + confirmed); T5 (worker folding) and T6 (running count without slots) answered with reasons and accepted/withdrawn by CodeRabbit; 79f8f5d3 (status read before snapshot early returns: OverviewFullScreenApp.java:1731-1745 on main), 5c60be2e, 427977bb (exit 1 for refused device acts in non-interactive handlers), b95fe7b7 (fixed per comment); 3edeb8ac, 688da595, 81d60d57, 9ff1eafe answered with reasons in the 18:35 comment ("recorded as an open item" in the landing, not tracked here).

## PR 301 (merge 2c25ed1d) feat(web) four page templates (PLAN-43)
Reviews 878212c2 (4 body nitpicks), 3d5c47c3 (3 body), 721fbebe (2 inline + 3 body), 82a8d1ce (4 inline + 1 body), one rate-limit comment. 6 inline threads (5 resolved/Addressed, 1 unresolved T0) + 11 body findings = 17 checked. The last code commit (ba2d3bc8, "read every root rule, directive and comment-free markup") came after the last review, so it closed the late findings. No unhandled findings.
Verified on main: TriagePage.test.ts rerender in both directions (:60-65), no-filters fixture (:75, TriagePageNoFiltersFixture.svelte), base `.triage-page__filters` rule asserted (:82-84); templateStyle.ts handles max-inline-size except 100%, CSS comments and `--max-width`, `style:` directives (:189-193), inline root style, `pageRootCarriedByMarkup` (the unresolved T0 "selector exists on a markup element" is covered by the GhostRootView control, pageWidth.test.ts:123-127); pageWidth.test.ts derives TEMPLATES from the directory (:25-26) and scans views for template imports incl. barrel (:40-45, :64-70); negative control InlineChildView. T1 (parse with svelte/compiler) answered with a stated reason by cuioss-oliver (23:29 comment, raw scan kept deliberately).
(T0 remains unresolved on GitHub although the code covers it.)

## PR 303 (merge 94bbefe1) fix(operator) Ctrl+C status in every handler (PLAN-91)
Reviews 78148600 (7 inline + 1 body), 2586a83a (empty), 0c92ada1 (2 inline + 10 body). 10 inline threads (8 resolved, 2 UNRESOLVED) + 11 body findings = 21 checked. Only merges of origin/main came after the last review (0c92ada1 itself is a merge commit); no disposition comment exists; no replies except T0. The six findings the caller already knew are confirmed on main, plus the items below.

UNHANDLED, all still present on main (file refs are main):
1. aea237c2 (0c92ada1 body, Minor / Maintainability), ReadVerbExitCodesTest.java:69-91. `refusedReads()` is a hand list of 19 verbs; registered read verbs `plans` (PlansCommand.java:34) and `lesson check` (LessonSourceCommand.java:267) are missing although the Javadoc says "Every read verb"; not derived from registrations.
2. 136570a8 (Minor), InterruptExitCodeTest.java:81 `fixture.writeToken("test-token");` literal, in a class that otherwise uses Generators.
3. 35159e2b (Minor / Functional), OnboardCommand.java:114-115 (confirm at :243-249). `onboard --auto` confirmation: `confirm()` reads via JLine and does not catch `UserInterruptException`; `call()` catches only IOException, so Ctrl+C reaches PmOperator.execute and exits 1 instead of 130.
4. beb7a490 (Minor / Functional, "predates the change, follow-up acceptable"), StartDialogue.java:125 and PlanSettingsDialogue.java:77 catch only `StartPrompt.StartLeftException`, never `InterruptException`, so Ctrl+C at a start/settings prompt exits 0. Main design 04-console.adoc says the view's own start/settings dialogues "still leave back to the view as q does", so the view-opened case is intentional; the standalone verb paths are what the finding names.
5. 7f327e3d (Trivial / Heavy lift), ActionsFullScreenTest.java:317-340. Tests named "Ctrl+C ..." only `raise(Terminal.Signal.INT)`; none types ^C; neither renamed nor extended.
6. f59fb037 (Trivial), no interrupt exit-status test for the `projects`, `system` and `--tui` entry points (only the `plans`/`plan` path in ActionsFullScreenTest and the dialogue cases in InterruptExitCodeTest:133-232; ProjectsFullScreenTest/SystemFullScreenTest have no interrupt test).
7. b7f0f200 (Trivial), SystemFullScreenTest: no test with a failing `/api/v1/system` and a successful status (no systemCode switch); the "status read after a failed snapshot" path in refreshSystem is untested.
8. 4d873979 + 13b9e305 + a7ef7baa (Trivial; a7ef7baa is a "publication withheld, unfinished correction of the earlier published finding" record), SystemViewConsistencyTest.java:208-225. `bothViewsShowTheSameTokenUnits` checks only first and second model splits with `contains` on the whole text, never the summary split, and never ties a split to its own model row (swapped splits would pass). The earlier inline T5 on the same test is tagged Addressed, but CodeRabbit's final review says the model/row-tied part is still missing, and main confirms it.
9. T8 inline (UNRESOLVED, 0c92ada1), doc/design/terminal-client/04-console.adoc:195, Minor. The doc still says an error in a view-opened dialogue "leaves the terminal in the normal screen; the view is not re-entered, and the console reports the error at the prompt"; CodeRabbit says the code re-enters the alternate screen and shows the message in the status row. Text unchanged on main (04-console.adoc:195-196). I did not re-verify the code side of the claim.
10. T9 inline (UNRESOLVED, 0c92ada1), WebDevicesCommandTest.java:404-407, Minor. `@CsvSource` hard-codes device ids `dev-2`, `dev-3` as command input; project rule says generated input. Unchanged on main.

HANDLED: 4c29bc6d (ConfigCommandTest now returns 500 only for /api/v1/config, 404 otherwise, ConfigCommandTest.java:85-91); T0 (refreshes skipped after an interrupt, replied "Fixed in 2586a83a"), T1 (console doc / exit path), T2-T7 (Generators + @EnableGeneratorController, distinct token counts, etc.) tagged Addressed.


## PR 304 (merge 3f26b6c2) fix(web) green e2e baseline and time residue (PLAN-90)
Reviews c41df5cd (3 inline + 2 body), dc52c617 (1 body... plus inline), 88846137 (1 inline + 2 body), four empty. 6 inline threads (all resolved, 5 by cuioss-oliver with a reply, 1 by CodeRabbit) + 5 body findings = 11 checked. No unhandled findings.
Inline: T0, T1, T3, T4, T5 fixed and replied ("Fixed in db1bccd / f56a06e / 42b63ed", CodeRabbit confirmed); T2 (`ok` fallback) answered with a reason (catalogue guard forbids inline fallback), CodeRabbit withdrew it.
Body findings, verified on main: SystemView.test.ts row-scoped retry/error badges (:257-258, `within(retryRow)` / `within(errorRow)`); language switch of loaded status words (:261-276) incl. the `1 error` badge after the switch (:275); plural form `2 Wiederholungen` with `retries_count: 2` (:231, :242); PlanView.test.ts streamed-row test loads the snapshot on 15 Oct 21:00 and advances to 16 Oct 00:30, asserts two day groups (:320-355).

# Summary table (unhandled findings still present on main)
| PR | checked | unhandled & present | notes |
|---|---|---|---|
| 284 | ~25 | 10 | 4 unresolved threads (T13-T16) + 2 body follow-ups + 2 body nitpicks + 1 body (mock fidelity) + T2 resolved without change |
| 287 | 11 | 2 | Major MachineSettingsView reminder never saved; 1 Trivial test |
| 288 | 2 | 0 | |
| 291 | 24 | 2 | both Trivial, body-only |
| 292 | 27 | 1 | snapshot read deadline; Ctrl+C residue answered with reason |
| 293 | 15 | 0 | |
| 296 | 9 | 3 | 6 inline fixed by PR 300; 3 body findings never handled |
| 297 | 15 | 6 | 1 Major (DecisionView chat), 3 unresolved threads, 2 test nitpicks; last review came after last code commit |
| 298 | 0 | 0 | skip-bot-review label |
| 299 | 27 | 6 | 3 unresolved threads + 3 body |
| 300 | 0 | 0 | skip-bot-review label |
| 301 | 17 | 0 | |
| 302 | 0 | 0 | skip-bot-review label; feature PR never bot-reviewed |
| 303 | 21 | 10 distinct (aea237c2, 136570a8, 35159e2b, beb7a490, 7f327e3d, f59fb037, b7f0f200, 4d873979/13b9e305/a7ef7baa as one, T8, T9) | |
| 304 | 11 | 0 | |

# CodeRabbit audit, batch A (PR 231, 232, 237, 238, 243, 246, 247, 249, 253, 255, 256, 257, 259)

Method: all `coderabbitai` review bodies (inline-thread summaries, "Outside diff range", "Nitpick", "Additional comments") read in full; all review threads read with resolution state, replies and CodeRabbit's own "Addressed in commit" marker; operator comments on the PR read; every finding without a reply, and every body-only finding, checked against the merge commit (`git show <merge>:<path>`) and against `main`. No PR carries the label `skip-bot-review` (no labels at all in this batch). All merges are squash merges; the tree of the last PR head equals the merge tree for the files checked.

Caveat: where a thread carries only CodeRabbit's own "Addressed in commit X" marker and no operator reply, I took the marker as a pointer and verified the code only for the findings flagged below or where the marker was missing; the others were not independently re-verified line by line.

## Summary table

| PR | Findings checked | Unhandled at merge | Still on main |
|----|------------------|--------------------|---------------|
| 231 | 8 threads + 1 outside-diff | 0 | - |
| 232 | 23 threads + 3 body-only (2 outside-diff, 1 nitpick) | 2 (Onboarding re-ask marker, ProjectSetup re-ask marker; both Trivial/Minor, in `@Disabled` tests) | Onboarding: yes in changed form; ProjectSetup: no (superseded) |
| 237 | 22 threads + 1 outside-diff | 1 (unresolved thread, Minor) | yes |
| 238 | 5 threads (+1 follow-up in a thread) | 0 | - |
| 243 | 1 thread + 5 nitpicks (body-only) | 4 (Trivial nitpicks) | yes, all 4 |
| 246 | 4 threads | 0 (all answered with a stated reason) | - |
| 247 | 2 threads + 1 nitpick | 0 | - |
| 249 | 4 threads | 0 | - |
| 253 | 10 threads + 6 body entries (4 distinct) | 5 (4 body-only + 1 unresolved thread) | yes, all 5 |
| 255 | 4 threads | 0 | - |
| 256 | 4 threads + 1 nitpick + 1 outside-diff | 0 | - |
| 257 | 9 threads + 1 nitpick + 2 body-only | 0 | - |
| 259 | 1 thread | 0 | - |

---

## PR 231 (theme + tab titles) - no unhandled findings (9 checked)
8 inline threads, all resolved, each with an operator reply giving a fix commit (07c950a) or a reason (display titles: "Not applied, with the reason", CodeRabbit withdrew it). Body-only finding (review a9491afc, outside diff, Minor, `app.html:22-24` derive bootstrap choices from `THEME_CHOICES`): answered in an issue comment (2026-10-26T23:16Z) and fixed in 0d28aa1; at merge `pm-web-app/src/app.html` keeps no choice list (any stored value other than `system` is the theme).

## PR 232 (PTY dialogue harness) - 2 unhandled (both minor, in tests that were `@Disabled` at merge)
Checked: 23 threads (all resolved; each either fixed with commit named, answered with a reason - /mock/* unauthenticated by design, act-log cap, scope of PLAN-02, stub daemon vs. mock runtime, `@Disabled` mandated by spec - or marked "Addressed in" by CodeRabbit; AnsiScreen wide-glyph attributes confirmed fixed at merge via `continuation` cells copying bold/dim/colour). Body-only findings of review 67f748a4 (2026-10-27T02:24Z):

1. `AnswerDialogueIT.java:58`, Minor, Functional Correctness: "assert prompt events, not final screen text". HANDLED in code (not by reply): commit 33db0a0f (before the review's head) already counts re-asks in `session.rawOutput()`; only the failure message still uses `screenText()`. No reply, but the finding is stale.
2. NOT HANDLED: `OnboardingDialogueIT.java:60`, review 67f748a4, Minor, Functional Correctness. Re-ask assertion counts `PROMPT = "Machine onboarding"` (a title) in the transcript, so a title redraw can produce the second occurrence without a re-ask. At merge (1864d8a3): `PROMPT = "Machine onboarding"`, line 60 `times(rawOutput(), PROMPT) >= 2`, unchanged. No reply, no comment. On main: `PROMPT = "Set up this machine for plan-marshall"` = `onboard.frame.title` (a frame title, `onboarding.en.properties:10`), still counted with `>= 2` on the raw transcript (`OnboardingDialogueIT.java:84`): the weakness is still present in another form. Mitigation at merge: the class was `@Disabled`.
3. NOT HANDLED at merge, SUPERSEDED on main: `ProjectSetupDialogueIT.java:62`, Trivial, "use a project-choice prompt marker, not `cui-parent`". At merge `PROMPT = "cui-parent"` (also in the context line and result). No reply. On main `PROMPT = "How should the project be named?"` (rewritten by the later state-dialogue plan), so no longer an issue.

## PR 237 (CC-P-09 text guard) - 1 unhandled, still present on main
Checked 22 threads + 1 outside-diff. Replies with reasons for the split-token (measured, rejected) and concurrent-scenario threads; others "Addressed in" commits 4c980b19/3a65a5b2/91edd5b/b2bbe3d, spot-verified in `textGuard.js` at merge (opacityProperty, MutationObserver settle with `observing`, throw at retry limit, placeholder/value scan, temp-file+rename, locale-union test). Outside-diff (review 3a65a5b2, `theme-readability.spec.js:294-299`, wait for `.report-title`): fixed (line 300 at merge). Thread "Wait for view data before accepting a settled scan" (ReportsView): fixed (`.report-card` wait, line 293).

NOT HANDLED: review ab8fa07c (last, on the final head), thread UNRESOLVED, `pm-web-e2e/tests/theme-readability.spec.js:1011`, Minor, Functional Correctness: "Wait for the loading state to end before recording EpicView, CleanupView, MachineSettingsView". A static loading placeholder passes the 3x150 ms quiet check and gets recorded into the baseline. Evidence at merge (1b39f219): `record('EpicView')` follows only `epic-view` visible (line 264-265); `record('CleanupView')` follows only `cleanup-view` visible (line 336-337); `record('MachineSettingsView')` follows only `machine-settings-view` visible (line 354-355); no `epic-loading` / `cleanup-loading` / `.loading-text` wait for these three (the `.loading-text` wait at line 350 is for SettingsView). No reply, no later commit. On main: identical (`theme-readability.spec.js:259-266`, 337, 351-355; `git grep epic-loading|cleanup-loading` in pm-web-e2e = no hits).

## PR 238 (summary line after q, PID file) - no unhandled findings (5 checked)
5 inline threads, all resolved with operator replies naming fixes in 353813aa/f016c11 plus an issue comment summarising; spot-checked `levelBreadcrumb` shared with `buildScreen`, `PtySession.awaitReader`, `stop.sh` falling through to `pgrep`. No body-only sections.

## PR 243 (port assignment) - 4 unhandled (all Trivial nitpicks, body-only), all still on main
Review 24d2cd20 (2026-10-27T14:45Z), body "Nitpick comments: 5". No operator comment on any nitpick (only `@coderabbitai review` triggers). The 1 inline finding (`StartStopScriptsTest`) is fixed in cffb5d1 with reply.
- HANDLED: `MockRuntimeCli.java:260-280` validate port range: `parsePortArg` and `resolveFallbackWebPort` reject < -1 and > 65535 at merge.
- NOT HANDLED: `MockRuntimeEngine.java:2058-2061`, Trivial, Maintainability: extract repeated `webPort > 0 ? webPort : 7420`. Merge (15b7780c): six sites (1965, 1993, 2007, 2059, 2115, 7158). Main: still six (lines 2141, 2169, 2183, 2235, 2291, 8128).
- NOT HANDLED: `pm-web-app/vite.config.ts:131`, Trivial, Functional Correctness: set `server.strictPort` when `PM_WEB_PORT` is present (AGENTS.md says an assigned port in use is a blocker). Merge and main: `port: resolvePorts().webPort` with no `strictPort` (main line 133).
- NOT HANDLED: `pm-web-e2e/scripts/guard-ports.js:10-17`, Trivial, Maintainability: duplicated port guard / two allowlists (this file allows nonexistent `guard-ports.test.js`, basename match). Merge and main: both `scripts/guard-ports.js` and `tests/port-guard.spec.js` exist.
- NOT HANDLED: `pm-web-e2e/tests/helpers/ports.js:8-9`, Trivial, Functional Correctness: `Number(process.env.PM_WEB_PORT) || 5173` silently turns `abc`/`0` into the default. Merge and main: unchanged (`ports.js` lines 8-9).

## PR 246 (presentation rules in design docs) - no unhandled findings (4 checked)
4 threads, each answered by the operator with a stated reason (spec follows CLI-VD-P-01 / P-03 verbatim; production `Style.java`/`Glyphs.java` belong to the later package CLI-VD-S-02; wording follows `operator.overview.too_narrow`); CodeRabbit withdrew 3, and for the "too narrow" wording said it "will not press" it. Counted as handled (answered with a stated reason).

## PR 247 (design system v2 tokens) - no unhandled findings (3 checked)
2 threads (README radius table, hovered links in test): "Addressed in" ccb2943 / 816de3f; radius/breakpoint doc updates confirmed (`doc/design/web-app/README.adoc` in ccb2943). Nitpick (review aacb7238, `tokens.test.ts:166-189`, align `TEXT_PAIRS` comment): at merge the comment reads "Every text pair the interface renders: the three text roles on every surface, and each accent role on the surfaces it actually appears on." - reworded, so effectively fixed (no reply).

## PR 249 (shared display vocabulary) - no unhandled findings (4 checked)
4 threads, all with operator replies and "Addressed in e74237a". No body-only findings.

## PR 253 (mock: scenario facts on goto) - 5 unhandled, ALL still present on main
Operator comments on the PR are only `@coderabbitai review` triggers; no reply to any of the following. Threads 0-8 (hold clearing, goToState rollback, spec count assertions, etc.) are fixed per replies/markers (thread 8 has an operator reply, "Addressed in 4994ebd").

1. NOT HANDLED, body-only: reviews 2fa04270 (Minor) and 34cb1ef8 (Major), outside diff, `MockRuntimeEngine.java:632-638` / `:634`, Data Integrity: "Validate start-state cleanup facts before switching scenarios". `setScenario` calls `validateRuntimeFixture(scenario)` only; a malformed start-state `facts.job` fails in `seedStateFixtures()` after `currentScenario` and the stores were changed (reported as `scenario_not_found`). Merge (4efa0314) `MockRuntimeEngine.java:635-649` and main `717-731`: no `validateStateFacts(scenario, scenario.start())` call (the method exists, used only in `goToState`, merge 1100 / main 1183).
2. NOT HANDLED, body-only: same two reviews, outside diff, `:7614-7619` / `:7623`, Minor, Data Integrity/Functional Correctness: "Persist the adjusted cleanup result for retries" (first delete subtracts unchecked entries from `freed_bytes`; a repeated delete takes the `DONE` branch and returns the original `summary().freedBytes()`). Merge `MockRuntimeEngine.java:7636-7645`, 7666-7672 (DONE job built with `activeCleanupJob.summary()` unchanged); main `8399-8408`, `8433-8439` identical.
3. NOT HANDLED, body-only: review 2fa04270 (Minor), 34cb1ef8 (Major, Heavy lift), outside diff, `:7308-7315` / `:7316-7317`: "Keep the system snapshot consistent with the live cleanup job". After `system.running` jump and a cleanup delete/ignore, plain `GET /api/v1/system` still serves the seeded snapshot with the old cleanup counts. Merge `handleGetSystem` lines 7330-7337 returns `systemSnapshotFixture` unchanged; main 8093-8100 identical.
4. NOT HANDLED, body-only: review 6a001a9d ("Additional comments", marked "Unverified finding" by CodeRabbit), `MockRuntimeEngine.java:1096`, Major, Data Integrity: "Broadcast a reset after a successful state reseed". The operator ADDED `broadcastReset()` to `goToState` in 4994ebd5 ("... reset subscribers after it") and REMOVED it again in 2fa04270 ("keep the jump's SSE event order unchanged") with no stated reason. Merge `goToState` (1091-1112) and main (1174-1195) end with `transitionTo(stateId)` only.
5. NOT HANDLED, thread UNRESOLVED (outdated), review 2fa04270, `MockRuntimeEngine.java` (facts validation, also "1127"), Minor, Functional Correctness: "Reject present cleanup facts that are not objects" (`"job":[]` / `"durations":[]` make `JsonTree.object` return null, so `validateStateFacts` and `applyCleanupFacts` skip them and `/mock/goto` serves the default cleanup job). Merge and main `validateStateFacts`: `var job = JsonTree.object(facts, FIXTURE_JOB); if (job != null) {...}` - null is silently skipped (main lines 1204-1219).

Note: items 1-4 are body-only, which a thread-only check misses; item 4 carries CodeRabbit's "Unverified finding" caveat.

## PR 255 (UI icon set) - no unhandled findings (4 checked)
4 threads, all with operator replies (fixed in 5de65f5) and CodeRabbit confirmation. No other body sections.

## PR 256 (TUI footer/help overlay, PLAN-15) - no unhandled findings (6 checked)
4 threads: replies + "Addressed in 24493fc..3823c62". Nitpick (review 3fe48cf7, `TuiKeyRegistry.java:187-197`, extract shared footer-cell loop): done - `footerCells` exists at merge (lines 169-204) and on main. Outside-diff (review f11fb4c4, `TuiKeyRegistry.java:366-378`, guard overlong `more` fallback): answered in an issue comment as fixed in 467598a5; merge line 354 `return displayWidth(more) <= width ? more : "";`.

## PR 257 (strict choice prompt and safe pause) - no unhandled findings (12 checked)
9 threads (4 had no ADDR marker or reply at thread level) plus nitpick (`PlanCommand.java:86-88`, derive pause values from `PausePoint`), "Additional comments" (flat `next` route, "Unverified") and outside-diff (reject `next` in the mock). All covered by the operator's issue comment at 2026-10-28T21:40Z ("All six findings are fixed in 1c77501", with a disposition table; nitpick "taken too"; flat `next`: "refused rather than reported as a success"). Verified at merge (81f84ee3): `EPIC_FLAT_ACTIONS = Set.of(ACTION_AUTO)` (so `next`/`cancel`/`archive` flat routes refused), `PausePoint.NOW.value()` etc. used in `PlanCommand`, `PLAN_CONTROL_ACTIONS` derived from `PlanAction` enum values (thread "derive the flat action list").

## PR 259 (use-case drafts) - no unhandled findings (1 checked)
1 thread, fixed in 2f6ac370 with reply (design line now says Plan Overview shows only part of UC-PLN-7).

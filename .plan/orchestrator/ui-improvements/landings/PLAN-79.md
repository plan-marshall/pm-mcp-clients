# Landing Analysis: PLAN-79 — Java: pm-operator tests on generated input data

epic: ui-improvements
workstream: WS-04
pr: #502 (merged as ac538733, 2026-11-19; 29 files, +430/-104)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-79-cui-testing-operator-001`, `landing-check` complete: false, `total_tokens`, `model`, `effort` `unknown`) after verifying claims against ground truth. The plan converts tests only; the analysis checks that nothing was loosened, that the counts hold and that the folded items are real.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 unit tests on generated input | met | **met-modified** | counts: 139 classes before and after, annotated 70 → **82** (the lane says 83; re-read by `git grep -l` at `ac538733^1` and `ac538733`), input-free 57 (lane 56); of the 82, 78 actually call `Generators` (the leaf's count) because `OverviewFullScreenAppTest` carries a dead annotation (`@EnableGeneratorController` twice, no `Generators.` call: re-read) and `OperatorVerbsTest` is listed as converted but has neither (re-read); tests 2643 → 2649 (+6, the six new tests, reproduced as an approximation); the list of input-free classes with their reasons is not in the repository (progress record) |
| D1 `verify` green, count not lower | met | not evidenced by reading | the count rises by 6; the green build is the lane's account (jobs named, not opened) |
| D2 integration and transcript tests | met | **met-modified** | 3 → 12 annotated of 33, all 12 generate; 21 input-free; the byte-identical clause holds except the two console-settings transcripts, a fold licensed by the spec (2026-11-14 item 3) because the scripted dialogue now types `q` before `config` and `exit`; no generated value is in a golden file |
| Folds: PLAN-109 German assertion, PLAN-116 items 2 and 3, PLAN-118 items 1 and 2, 2026-11-14 items 3 and 4, PLAN-127 #382 | met | met (one with a weak pin, L3-1) | each is licensed by the spec and has a test that reads as able to fail; PLAN-127's `workActionNextInvalidAnswerThenLeaveSendsNothing` and PLAN-116 item 2 are in the diff but not in the lane's message |
| Spec items the message does not mention | — | **not evidenced / not met** | 2026-11-14 item 2 (parameterize repeats: no `@ParameterizedTest` added), the #432 coverage finding (no test, no before/after), and the re-grounding of 2026-11-10 "delete or re-home `EnrolCommandTest`, `StartCommandTest`, `OnboardCommandTest`" (re-read: all three still exist at `ac538733` and no `EnrolCommand`, `StartCommand` or `OnboardCommand` class exists in `src/main`) |

## Three-Layer Analysis

Read on three layers by one read-only leaf (level 3, 7 + 8 + 8 numbered groups); the orchestrator re-read the points marked "re-read" at `ac538733`: the annotated-class counts, the `PmConsole` production change, the dead annotation, the completer test, the three old test classes. By reading and the leaf's own read-only scripts: no build, no test run, no mutation. **Not read by the leaf:** the progress record, CodeRabbit's thread text, build and Sonar output, 47 of the 57 unannotated classes (10 sampled), `PlanCommand`'s `--message` option, the cui-test-generator version the build resolves.

- **Dispatch:** one read-only leaf — a 29-file test diff that has to be read for loosened assertions and vacuous generated input.
- **Verdict:** no blocker, no major that can fail a build today. Layer 1: counts off by one, folds and items the message omits. Layer 2: **no assertion loosened** (every changed assertion is re-derived from the input or strengthened), no generated value in a golden capture, no field-initialiser draw, every generating class annotated; one production change. Layer 3: one weak pin, minor findings.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | 83 annotated, 56 input-free | **refuted** (82 / 57) | against the lane | re-read: 70 → 82 by `git grep -l EnableGeneratorController` outside `transcript/`; 78 really generate |
| L1-2 | the hypothesis "the share of classes that can use generated input is small" is "Confirmed" | **mis-stated** | against the lane | 82 of 139 unit classes (59 %) and 12 of 33 IT classes (36 %) now carry the annotation, and 56+21 = 77 of 172 accounted input-free is 44.8 %: the data does not say "small"; the spec's threshold (fewer than a third: report) is not triggered either way |
| L1-3 | the spec: behaviour unchanged, tests only | **not quite** | against the lane | `PmConsole.completer()` is a new production method (L2-1), not disclosed in the message |
| L1-4 | the byte-identical captures | confirmed with the licensed exception | with the lane | two files, `console-settings.transcript` and `console-settings-de.transcript`; the message says only "console-settings transcripts"; the German golden holds English runtime-supplied summaries from the mock ("English, models English", "4 models on 2 harnesses"): whether that is intended was not checked |
| L1-5 | no spec item left over | **refuted** | against the lane | the three items of the verdict table above |
| L1-6 | sequencing: a file a running plan changes is left out | holds | with the lane | PLAN-165's own commits touch `CleanupCommand`, `PmConsole`, `CleanupCommandTest` and two others; #502's only overlap is `PmConsole.java`; staged plans that now rebase over it: PLAN-42 (`PmConsole`, `OperatorVerbsTest`), PLAN-44 (`PmConsole`), PLAN-48 (`NeedsCommandTest`), PLAN-138 and PLAN-168 (`ControlDialoguesTest`) |
| L1-7 | the 100-file split question | does not apply | with the lane | 29 files; the spec's "about 86 + 29" estimate was stale (about 70 classes were already annotated) |

### Layer 2 — Implemented

- **L2-1 (re-read):** one non-test file changed: `PmConsole.java` gains `static StringsCompleter completer()` and `run()` calls it (`:131-133,155`); behaviour-neutral, added so PLAN-118 item 2 has something to pin.
- **L2-2:** every changed assertion either re-derives its expected value from the generated input or is strengthened (`DrainCommandTest` literals 615, `seconds + 15` and 15 instead of production constants; `[2/8]` → `[2/8] Harnesses` in `a7186796`; a 50 ms window plus a blocker check for the lock wait); no `@Disabled`, no widened tolerance, no `contains` for `assertEquals`. Contract text (catalogue strings, `Plan-Einstellungen`, scenario ids, scripted answers) stays literal.
- **L2-3:** no generated draw in a field initialiser; `@EnableGeneratorController` on every class that calls `Generators`; the generated values are safe where they are typed into a prompt (`ChoicePrompt` accepts only a key or a 1-based number; `PtySession.quote` single-quotes every argument; `AnswersTest` stays under the four-character limit).
- **L2-4:** the five folds can fail, by reading: PLAN-109 (`startsWith("Plan-Einstellungen")`), PLAN-118 item 1 (the String-valued number branch decides 615 vs 135), 2026-11-14 item 4 (the step-6 assertion fails without `refreshPoppedLevel()`; the two `rows().isEmpty()` assertions would not), PLAN-116 item 3 (`[2/8] Harnesses`; the lane's observed `[2/7]` mutation is not reproducible by reading). None was mutation-run here.
- **L2-5:** sampled input-free classes: `NeedsIT`, `ProjectsFullScreenTest`, `OverviewFullScreenTest` are contract only; seven others hold incidental input the spec's own definition covers (`PmOperatorTest:396-405` port 8443, `OperatorGateScopeTest:114-121,171` `--message hello` and `--pull-request 4711`, `PmOperatorProcessIT:447,488-502`, `CredentialsCommandTest:246` secret `s3cret`, `RolesCommandTest:148-211`, `ControlCommandTest:71-261`, `DirectFileAccessTest:93-97`).

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `OperatorVerbsTest.java:39-48`, `PmConsole.java:131-133,155` (re-read) | `run()` reverted to a hand-built `new StringsCompleter(handList)` with the helper left in place | the test pins the helper, not `run()`; it still passes — the very regression PLAN-118 item 2 described | minor, by definition major for a pin |
| L3-2 | `OverviewFullScreenAppTest.java:18,49` (re-read), `OperatorVerbsTest` | the count of converted classes | a dead annotation and an unannotated class are counted as converted; three older dead annotations (`ConfigSettingsTest`, `RuntimeActDialogueTest`, `dialogue/ChoicePromptTest`) are in the "70 before" | minor |
| L3-3 | the squash commit text on `main` | — | CodeRabbit's release notes in the commit message say "Bug Fixes: improved access to console configuration settings ... fixed onboarding dialogue content"; the diff holds no such fix | minor |
| L3-4 | the seven classes of L2-5 | — | hand-typed incidental input remains in classes accounted input-free | minor |
| L3-5 | `PlanChatIT.java:42-44` | — | the generated message is never asserted to reach the wire (it was not before either) | minor |
| L3-6 | `RuntimeStopDialogueIT.java:60` | the dialogue gains a third choice | `3..9` is "unknown" only while there are two choices; `PauseDialogueIT` was widened to 4..9 after the review, this one was not | minor |
| L3-7 | `PmConsoleBehaviourTest.java:790-793` | a different blocker | the lock-wait check accepts any non-`Thread` blocker, not only `PmConsole`'s lock | minor |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | a failing test is reported, never loosened | conforms | L2-2 |
| spec Standing Constraints | behaviour unchanged | conforms with one behaviour-neutral seam, undisclosed | L2-1, L1-3 |
| CLAUDE.md Code Standards | `@EnableGeneratorController` on every class that takes input; input from `Generators` | partly | seven classes of L2-5 take input and carry neither |
| spec Done-when D1 | a class that takes no input data is listed in the progress record with that reason | not evidenced | the record is not in the repository; the PR body gives only the counts |

## Metrics and Anomalies

- Tokens `unknown`, `model` `unknown`, `effort` `unknown` (harness `antigravity`, plugin version `0.1.0`, executor 0.1.2149): unobserved, not zero.
- Anomalies: one self-review round that added a missing dialogue-level test, a second that cleared; no operator question; builds routed by the daemon (6 split gate jobs on 2 heads, no fallback, none killed — the lane's account, not opened).

## Routing and Merge Behavior

- Review gate re-read with `runbook_lane review-gate --pr-number 502`: `unresolved_review_threads=0` (lane: 0, **same**), `review_body_findings_total=5` (lane: 5, **same**); `review_body_findings_handled` reads 0 and `merge_allowed: false, body_findings_undisposed` now, where the lane read `all_handled` at its merge: the disposition record lives in the lane's progress file, which went with its worktree, so the gate cannot see it (the same counting gap as #497, #500 and #438); the handled count is the lane's account and not re-verified.
- Sonar: lane `sonar_open_new_issues=0`, quality gate ok; the Sonar search for pull request 502 returns no issues (0, **same**).
- CI/merge: merged through the merge queue as `ac538733` (re-read: `MERGED`, `changedFiles` 29); the worktree and branch are gone locally (the leaf: no worktree, no local branch; the remote branch not checked).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 502; `landing` `landings/PLAN-79.md`; `plan_marshall_plan_id` `79-cui-testing-operator`
- [x] inbox: `rb-79-cui-testing-operator-001` archived as consumed
- [x] epic.md: an Open Defect entry for this landing; PLAN-79's overlaps with PLAN-168 and the staged terminal plans are released
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: L3-1 is the only one that pins less than it claims; the rest are minor. Proposed, the operator's word only: a small test-only fix (pin `run()` through the completer, remove the two dead annotations, convert the seven classes of L2-5 or list them with their reasons, delete or re-home the three old test classes) as a direct session or as a D0 of the next terminal plan that edits the same tests (PLAN-168 touches `ControlDialoguesTest`).
- The spec items the lane did not do (parameterize repeats, the #432 coverage finding, the three old test classes) stay named and are not staged.
- For plan-marshall: a landing that counts a dead annotation as converted and labels a refuted hypothesis "Confirmed"; the lane does not list the production seam it added; `review-gate` counts a disposition it cannot see as undisposed once the progress file is gone (now on #438, #497, #500, #502); `landing-check` `unknown` for tokens, model and effort.

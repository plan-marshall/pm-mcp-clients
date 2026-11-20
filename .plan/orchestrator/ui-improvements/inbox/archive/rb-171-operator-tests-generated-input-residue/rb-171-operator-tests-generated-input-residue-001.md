envelope_version=1
sender_type=plan
sender_id=rb-171-operator-tests-generated-input-residue
epic=ui-improvements
kind=landing
created=2026-11-19T23:30:48Z

```landing-facts
schema=landing-facts/1
plan_id=rb-171-operator-tests-generated-input-residue
epic=ui-improvements
pr=#510
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=7
review_body_findings_handled=7
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

## 1. Deliverables

Every clause of the five deliverables is met. Branch `fix/171-operator-tests-generated-input-residue`, merged as `76315c630ac09b3e79e7110aad49d5231d54b252` (PR #510).

**D1 — the completer pin covers `run()`** (commit `c9af03fa`)
- "the test is run once with `run()` reverted to a hand-built completer and fails" — **met**, order `mutation-after`, test `OperatorVerbsTest.consoleCompleterMatchesOperatorVerbs`, failing run at `c9af03fa` with the mutation, log `24b22a0159754e9d9a015493ee77796b` (`expected <28 verbs> but was <[quit, exit, help]>`, OperatorVerbsTest.java:106); the committed fix passes at the same commit (`e40c2fa6d24d49f9a34441eb7d1133d5`).
- "`git diff` of `PmConsole.java` shows only the seam, and the landing names it" — **met**, order `none`. The seam is one field set where `run()` binds the completer, plus a package-private accessor; the only other line is the quality gate's `@Nullable` line break.

**D2 — no annotation without a `Generators.` call** (commit `0ca7a34d`, plus `fcfb6362` for the review)
- "every class carrying `@EnableGeneratorController` contains a `Generators.` call, checked by a recorded search" — **met**, order `none`. At the merged head `git grep -l EnableGeneratorController -- pm-operator/src/test` names 97 files and each of them is in `git grep -l "Generators\." -- pm-operator/src/test`; the annotated set has no member without a call.
- "the landing carries the restated counts and the input-free list with one reason per class" — **met**, order `none`. Counts, from the two searches: before 95 annotated / 91 generating / 4 annotated-without-generating; after 97 annotated / 97 generating / 0 without. Per group after: transcript package 12/12, root IT (`PmOperatorProcessIT`) 1/1, unit classes 84/84. Input-free with a reason (3): `OverviewFullScreenAppTest` (screens driven from fixed runtime JSON; the ids and texts it types are the wire contract), `dialogue/ChoicePromptTest` (choice keys, numbers and deliberately-not-offered probes — the values are what the prompt accepts or must refuse), `OperatorVerbsTest` (contract list comparisons; the console it drives in D1 types `exit` only). `RuntimeActDialogueTest` and `ConfigSettingsTest` are generating rather than input-free.

**D3 — the six classes take incidental input from `Generators`** (commit `1ed6044f`, plus `fcfb6362` and `06a43596`)
- "each converted class keeps the same behaviour and no assertion is loosened, read in the diff" — **met**, order `none`. Read hunk by hunk: exact body/output/revision strings derived from the drawn value, `contains(firstPlan)` in place of `contains("a-plan")`, nothing turned into `contains`/`isNotNull`, nothing removed.
- "one failing run per converted class with the production behaviour mutated" — **met**, order `mutation-after`, six runs, each reverted to the committed fix: `PmOperatorTest` (port dropped from the web-enable body) `e109f58f1bd143cfbdb3d002ec7af614`; `OperatorGateScopeTest` (`roles` no longer an act) `7516abba4b2a459790fb449b250985c7`; `PmOperatorProcessIT` (terminal refusal removed) `1e43a37887fb4048bf0f3fd32880cb35`; `ControlCommandTest` (settings revision +1) `6ede34cf07e4417db43721c850cc734d`; `DirectFileAccessTest` (config read answered `{}`) `fd2721b351e040a492c8ef72d730df5a`; `EpicDecompositionDialogueKeysTest` (`CUT_KEYS` without `r`) `09a5e9afb6994c07ba3c18e5d59a3fca`.
- "`./mvnw -pl pm-operator verify` passes and the test count is not lower than before" — **met**, order `none`. 2665 tests at the D3 head `1ed6044f` (log `b65d445b5228450686c89aa3d832c7bf`) against 2665 at `cf8a1692` (log `48bce45400bd41978d54e97a8e01f4e8`, which carries one base-only flake, `ReportsFullScreenTest.marksAllShownRead`, that passes at this head). `verify -pl pm-operator` green at `28b8580e` (log `e515752ea2c144369bf2f03d6b0da61c`).
- `RolesCommandTest` and `CredentialsCommandTest` are left out and named here, owned by the running PLAN-168.

**D4 — the three classes of removed commands** (commit `9a953c5a`)
- "`git ls-files pm-operator/src/test` names none of the three" — **met**, order `none`.
- "tests after = before − named duplicates, recorded; `verify` passes" — **met**, order `none`. 2665 before, 2653 after (log `a042666abf9244989cbfca598925b5c4`); 2665 − 2653 = 12, exactly the named duplicates (`EnrolCommandTest.enrolProject`, `.enrolPilot`, `.enrolRootConflict`, `.enrolStoreRootMismatch`, `.enrolStripsControlSequences`, `.unenrolProject`, `.unenrolRequiresConfirmation`, `.unenrolScopeActive`, `StartCommandTest.planAddStartsAPlan`, `.epicAddStartsAnEpic`, `.recipeRunStartsARecipe`, `OnboardCommandTest.ConfigAspect.runsOneStepAgain`), each covered by an existing test of the same behaviour; 9 further tests were re-homed (`PlanCommandTest`, `ProjectCommandTest`, `EntryFlowTest`, `ConfigCommandTest`, `ConfigSettingsTest`). `verify -pl pm-operator` green.

**D5 — the unconvertible assumptions** (commit `28b8580e`, plus `5064af17`, `fcfb6362`, `06a43596`)
- "(a), (b), (c) each run once with the production behaviour mutated and fail" — **met**, order `mutation-after`, each reverted:
  - (a) a third choice added to the stop prompt — `265a8a5f5cbb45608b18044071a3c23c`, `RuntimeStopDialogueIT.unknownInputReAsks:70 "the stop offers exactly what this test assumes: [1, 2, 3] expected [1, 2]"`. The test derives its unknown answer from the numbers the dialogue printed and asserts the assumed set; it now waits for the line after the last choice before reading them.
  - (b) the message replaced by a fixed text in the turn's body — `a5bcd99e042e480ab63820fff3de5d74`, `PlanChatIT.planChatTurn:119 "the turn's body must carry the message, got {"text":"placeholder",…}"`. The assertion was later made parse-based (the body's `text` compared exactly and `client_turn_id` required non-blank) on the review's finding; the recorded failing run predates that rewrite, and the mutation fails it for the same reason.
  - (c) the badge refresh taking a fresh local lock — `e545c99f375d4a98a6082540b5f2b752`, `PmConsoleBehaviourTest.twoConcurrentRefreshesNeverInterleave:790` (the console's lock not held, no queued thread, wrong state).
- "`verify -Pintegration-tests` passes and the screen-capture set is byte-identical" — **met**, order `none`. Green at the head `5064af17` (log `e6fe43591a2247cd9b68836d75c52956`, 341 s) with `PlanChatIT` executed, not skipped (`TEST-…PlanChatIT.xml`: `tests="1" skipped="0" failures="0"`), and `git status --porcelain` empty afterwards, so `pm-operator/src/test/resources` is unchanged.

Per-head test runs (module tests): `c9af03fa` 4, `0ca7a34d` 84, `1ed6044f` 2665, `9a953c5a` 2653, `28b8580e` 2653, `5064af17` 2653, `9ab57bec` 2653 (CI), `06a43596` 2653 (CI). Integration suite: `28b8580e` and `5064af17` (138 ITs, 7 skipped by their own design).

## 2. Personas and skills

Personas: `persona-implementer` for D1's seam, `persona-module-tester` for D2–D4, `persona-integration-tester` for D5, `persona-documenter` for the `doc/plans` correction the change made necessary, `persona-code-reviewer` for the three self-review rounds and for triaging every PR comment.

Domain skills: D1 `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`; D2–D4 `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`; D5 `pm-dev-java:junit-integration`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`. `skills added: none`, reason: no deliverable left its declared surface.

## 3. Hypotheses

- "the seven classes of PLAN-79's landing take incidental input at the named lines" — **confirmed** for the six in scope (D3), line by line; `RolesCommandTest` and `CredentialsCommandTest` are the remaining two and are PLAN-168's.
- "the bodies of `StartCommandTest` and `OnboardCommandTest` exercise verbs that exist, so each test has a home class" — **confirmed** by each test's first call (`plan add`, `epic add`, `recipe run`, the bare console, `config <aspect>`).
- "`PmConsole.run()` can hand its completer to a seam a test reaches without a pty" — **confirmed**: the completer is bound once in `run()` and the console records it there; no pty is needed to read it.
- "PLAN-168 and PLAN-165 change `RolesCommandTest`, `CredentialsCommandTest` and `CleanupCommandTest`" — **confirmed** for PLAN-168 (`git worktree list` showed it running; its Expected Surface names both files); PLAN-165 had shipped and owns nothing.
- The eighth class, `EpicDecompositionDialogueKeysTest`, was OBSERVED and is converted (D3).

## 4. Findings

Self-review, three rounds (`persona-code-reviewer`, adversarial-refute lenses):
- Round 1: `PlanChatIT` had lost the `POST` condition on the turn collector — fixed; its Javadoc retold a refusal the runtime no longer proves — fixed; `doc/plans/operator-command-tree.adoc` named the two deleted classes — fixed; the D2 counts were stale and the D3 count clause had no measured pair — re-measured (2665/2665).
- Round 2: F1, D5's integration gate unevidenced at the current head — closed with a full IT run at the head; F2 (`ConfigSettingsTest`'s input-free reason) and F3 (the ICU singular form is no longer asserted) — stated, F2 then removed by the review's own fix (the class is generating again).
- Round 3: F1 confirmed closed (`PlanChatIT` executed), no remaining finding; verdict "clean for the pull request".

CodeRabbit review at `f8b9504a` (7 findings, all handled in `fcfb6362`):
- `ConfigCommandTest` — "refuse the step POST, not the initial state read": the fixture answers the state read and refuses only the step POST, and the test asserts the POST happened.
- `ConfigSettingsTest` — "generate the round-trip inputs and restore the controller": revision and summary drawn, both asserted, `@EnableGeneratorController` restored.
- `EpicDecompositionDialogueKeysTest` — "derive refused-key candidates": the probes are the alphabet minus the union of the three key lists and the leave keys.
- `PmConsoleBehaviourTest` — "queue membership is not a parked thread": the wait ends only on queued AND blocked AND `TIMED_WAITING`.
- `PlanChatIT` — "assert the payload values": the body is parsed, `text` compared exactly, `client_turn_id` required non-blank.
- `RuntimeStopDialogueIT` — "wait for the complete menu": the snapshot is taken after `KEEP`.
- Nitpick, `RuntimeActDialogueTest` — "keep capacity distinct from usage": every slot group now has one spare slot.
Each thread was answered with the fixing commit and resolved; the nitpick was answered in a PR comment.

Sonar: one new-code issue after the first head (`java:S1130`, the needless `throws`), fixed in `9ab57bec`; the head reads 0 issues and 0 hotspots confirmed.

## 5. Reviewers

- `coderabbit` (required): reviewed `f8b9504a` — a qualifying review (its summary marker names that commit, `kind=reviewed`), 7 findings, all handled. Resolve marks it `review_complete_after_handling`; it is quota-limited and its later heads were not re-reviewed (its check answered `NEUTRAL`/skipping and its bot message said "Next included review available in 24 minutes"). The run did not re-trigger it after the later pushes: the review on record is the review, and every finding of it is handled.
- `sourcery` (optional): refused on its own budget ("you've used your own review budget … request another in 6 days"). Recorded; the bot is optional and does not gate.

## 6. Operator questions

None asked. No rule had to bend, no blocker arose, and no reviewer quota question was asked (CodeRabbit's refusal came after a qualifying review existed, so the quota policy did not apply).

## 7. Rule check

guard: none on this harness (unenforced) — there is no `{guard_log}` on this harness, so this section counts the run's own actions.

- **One break, corrected within the same minute.** While fixing the doc drift the review found, the run edited `doc/plans/operator-command-tree.adoc` in the **main checkout** instead of the worktree, then reverted that edit and applied it in the worktree; `git status --porcelain` in the main checkout was empty afterwards and the change landed inside the worktree. No main-checkout file remains touched.
- Two commands were tried and refused before the sanctioned path was used: `git branch -d chore/171-d3-count` (refused as unmerged; the run then used `git-workflow prune-local-and-remote-ref`, which deleted it) and `ci pr thread-reply` against a `--for reply` slot (refused; re-allocated with `--for thread-reply`). Neither left a state change.
- The build gate was run with the default routing throughout (18 routed runs; 6 re-attached with `build_server wait` past the 300 s foreground bound). No in-process degradation was taken, no job was killed, no launch fault occurred.
- The spec read was the ledger worktree's copy (`spec_source: ledger_worktree`); no `spec_differ` marker was raised.
- `executor-skew`: `match` (executor 0.1.2149, lane 0.1.2149), recorded at setup.
- Every script went through `python3 .plan/execute-script.py …`; every build through the resolved `build` commands; every CI action through `pm-mcp-clients`' `ci`; no `gh`/`glab`/wrapper/direct-binary call, no `sleep`, no `python3 -c`, no shell file read.
- Files were read and searched with the file tools (one `git grep` per search, which is a git command, not a shell grep).
- `.plan/` writes: `.plan/temp/` only, plus the `ci` body slots the `prepare-*` verbs returned (the default `NO_PLAN` slot was found overwritten by the concurrently running PLAN-169, so this run used its own `rb-171*` slots) and the one inbox landing write.
- The mailbox read was attempted three times (setup, before self-review, before the enqueue); each answered `invalid_target_plan` because the plan id starts with a digit. That is the spec's own known limitation: reported, never read as no mail.
- No `git stash`, `restore`, `checkout --`, `rebase`, force-push or `git branch -D`.

## 8. Build server

Setup preflight: `ready` (project registered, verified handshake). Every build took the default routing: `mechanism=daemon_longpoll` for all 18 runs, `in_process_fallback` 0. Six runs exceeded the client's 300 s foreground bound and were re-attached with `build_server wait` (never backgrounded); their `queued_seconds` ranged from 1 to 885 because other plans' builds held the machine-global slot. No `degraded` fallback, no `launch-fault-retry`, no `killed` job. No `runtime_stamp_reason` was returned; `plugin_version` is `unknown` because the resolve result could not read it.

## 9. Residue

- `pm-mock-runtime`'s `CONTROL_CHAT` scenario has no consumer any more: `PlanChatIT` was its last user, and a chat turn is not a decision answer, so the mock's act log cannot carry its body. Recorded for the epic; not fixed here (it would be a change outside this plan's surface).
- Accepted narrowing: `RuntimeActDialogueTest` draws every count from 2..9, so the ICU singular form of `system.confirm.what` is asserted nowhere now (the base asserted `"1 epic"`), and no other test reads `runningNow`. A draw that lands on `1` would make the assertion pass or fail by chance, which the test contract forbids; the narrowing is stated rather than papered over.
- `PlanChatIT` keeps its own three-line live-record writer because `PmOperatorTest.writeLiveRecord` is package-private to `de.planmarshall.operator` and the IT sits in the `transcript` subpackage; a shared test-support helper would widen this plan's surface.
- The `NO_PLAN` CI body slot is shared by every plan-less run on this machine; PLAN-169 overwrote this run's first body there. The run switched to its own slot and reports the collision so the harness owner can see it.

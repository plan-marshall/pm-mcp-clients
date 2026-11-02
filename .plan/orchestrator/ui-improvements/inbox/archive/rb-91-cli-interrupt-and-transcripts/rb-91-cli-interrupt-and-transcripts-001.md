envelope_version=1
sender_type=plan
sender_id=rb-91-cli-interrupt-and-transcripts
epic=ui-improvements
kind=landing
created=2026-11-02T10:19:47Z

```landing-facts
schema=landing-facts/1
plan_id=rb-91-cli-interrupt-and-transcripts
epic=ui-improvements
pr=#303
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=1
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing: PLAN-91 — Ctrl+C status in every handler and the tests PLAN-26 left out

Merged as `94bbefe1` (merge group run 41426316297, success). One of three deliverables is fully met
as written; the two that are not are reported below with their reason, not worked around.

## 1. Deliverables, with every Done-when graded

### D1 — Ctrl+C leaves with the interrupt status in every handler

- **Done-when 1** (one test per reproduced handler, the PTY harness where a prompt is involved, each
  asserting `130` and no act, each failing before the change) — order `test-first` — **not met as
  written**. Eight tests exist and all eight handlers return `130` with nothing sent; seven are
  proven on a real pseudo-terminal. `WorkActionDialogue` and `HoldDialogue` both prompt and are
  covered by the unit test instead: a `Ctrl+C` typed into a dialogue the full-screen view has
  suspended never reaches the dialogue's line reader (see §9), so the PTY harness cannot drive them.
  - Tests: `InterruptDialogueIT.{pauseInterrupt,cancelReasonInterrupt,cancelConsentInterrupt,
    reviewInterrupt,onboardInterrupt,projectSetupInterrupt}` (PTY, `assertEquals(130,
    session.exitCode())` + `harness.assertNoAct()`), and `InterruptExitCodeTest.{workActionDialogue
    Interrupt,holdDialogueInterrupt,reviewDialogueInterrupt,planPauseInterrupt,
    planCancelReasonInterrupt,onboardInterrupt,projectSetupInterrupt}` (in-process).
  - Failing run: tests committed alone at `b4a0b00`; 2/2 unit failures (`maven-2026-11-01-225346.log`)
    and 6/6 IT failures (`maven-2026-11-01-225410.log`). Green after the fix
    (`maven-2026-11-01-230457.log`).
- **Done-when 2** (terminal check on `fully-onboarded`: hold, review and project setup each
  interrupted at its first prompt; before and after captures recorded) — order `none` — **not met as
  written**. The review and the project setup are interrupted at their first prompt in all four
  sizes and locales; the hold dialogue's prompt is captured but its run leaves with `q`, because the
  interrupt cannot reach a dialogue the view has suspended (the same residue). The project setup
  states live in `projects-and-start`, not in `fully-onboarded`; that substitution is necessary and
  is recorded in the capture set's own header.
  - Captures: `pm-operator/src/test/resources/screen-captures/91-cli-interrupt-and-transcripts.txt`,
    12 captures at 120×30 and 80×24 in `en_US.UTF-8` and `de_DE.UTF-8`.
  - Before/after: the "before" run was taken with both fix patches reverse-applied to the working
    tree (`maven-2026-11-01-233440.log`), the "after" run with them in place
    (`maven-2026-11-01-233231.log`); the `.txt`/`.attrs.txt` pairs are identical — the change is the
    exit status, not the rendering.
- Commits: `b4a0b00` (tests), `4e44a5f` (the eight handlers), `c7ac812` (the view leaves with the
  dialogue it interrupted), `760ad6e`, `3f10761`, `0b9338d`, `db61a7c`, `5cb693e`, `2586a83` (tests
  the review and the coverage gate asked for; no runtime read after the interrupt).

**D1 is not fully met.**

### D2 — The three transcript goldens match

- **Done-when 1** (the failing names at the base recorded first with the log path, each classified
  with the deciding design sentence) — order `none` — **not met as written, and the premise is
  refuted**: there are no failing names. `StatusDialogueTranscriptTest`,
  `ConsoleDialogueTranscriptTest` and `StartTranscriptTest` are green at the base
  (`maven-2026-11-01-235811.log`, 4/4). The last commit that touched the three goldens is `7d7b4d71`
  (PLAN-82, PR #284), which updated them together with the texts. The spec's verify-first clause
  authorises this: a refuted cause re-scopes the deliverable and is reported.
- **Done-when 2** (`-Pintegration-tests` reports no transcript failure) — `none` — met
  (`maven-2026-11-01-233653.log`, 578 green; `maven-2026-11-02-092306.log`, 578 green at the final
  head).
- **Done-when 3** (the open-issue read at the pull-request head recorded) — `none` — met: the read
  at the merge head `bcfd4297` is `0` confirmed, no rules left open, taken after the last push;
  `sonar-gate` answers `merge_allowed: true` (`no_open_issues`), and the SonarCloud quality gate is
  `OK` on the same head (new-code coverage 80.6 %).

**D2 is not fully met** (Done-when 1 has no subject), though nothing in it needed a code change.

### D3 — The tests PLAN-26 left out

- **Done-when 1** (one test per case of (a) and (b), each failing when the code it covers is
  reverted) — order `mutation-after` — **met**. (a) `SystemFullScreenTest.LifecycleLine`:
  `aStatusReadReplacesTheLineTheStatusWrote`, `aStatusReadLeavesTheEventLineAlone`,
  `aStatusReadClearsTheLineOnceTheRuntimeRunsAgain`, `aFailedStatusReadLeavesTheLine`. (b)
  `WebDevicesCommandTest.aRefusedActLeavesWithOne[rename|revoke|listener-off]` and
  `ConfigCommandTest.aRefusedSettingsReadLeavesWithOne` (which drives PLAN-26's `ShowCommand`).
  - Mutations at `ea60cff`: replace `maven-2026-11-02-002013.log`; clear
    `maven-2026-11-02-001647.log`; device refusals `maven-2026-11-02-002115.log`; config refusal
    `maven-2026-11-02-004320.log`. Re-run at the final tests: leave
    `maven-2026-11-02-014653.log`, failed read `maven-2026-11-02-014729.log`.
  - (c) `ReadVerbExitCodesTest` now answers `500`, `401` and `404` for all 19 read verbs; it carries
    no Done-when mutation of its own (the parameter extension covers it).
- **Done-when 2** (the three classes of (d) draw their input from `Generators`, every
  `@EnableGeneratorController` used) — `none` — **met**. Residual literals remain in the runtime
  metrics of both fixture builders (a preference the reviewer raised; not a criterion).

**D3 is fully met.**

## 2. Personas and skills

| Step | Persona applied |
|---|---|
| production code (StartPrompt, the eight handlers, the view plumbing) | `plan-marshall:persona-implementer` |
| the PTY and in-process module tests | `plan-marshall:persona-module-tester` |
| the transcript measurement | `plan-marshall:persona-integration-tester` |
| every self-review round and the PR-comment triage | `plan-marshall:persona-code-reviewer` |

Domain skills, per deliverable as the spec declares them: D1 `pm-dev-java:java-core`,
`pm-dev-java:java-lombok`, `pm-dev-java:javadoc`, `pm-dev-java:java-null-safety`,
`pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-dev-java-cui:cui-logging`; D2
`pm-dev-java:junit-integration`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`; D3
`pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`. `skills added: none` — no deliverable
touched a module the Expected Surface did not name.

## 3. Hypotheses

- **Ctrl+C ends with exit `0` in the eight handlers** — confirmed, one catch site each:
  `WorkActionDialogue.java:93-95`, `ReviewDialogue.java:79-81`, `HoldDialogue.java:90-92`,
  `ProjectCommand.java:194-196`, `PlanCommand.java:614-616`, `OnboardCommand.java:114-116`,
  `ActionSupport.java:202-204` (the end reason) and `ActionSupport.java:242-244` (the end consent).
- **The three goldens fail on `main` because PLAN-82 and PLAN-09 changed texts the goldens still
  carry** — **refuted**; the goldens were updated with the texts in `7d7b4d71`, and all three
  transcript classes are green at the base.
- **The plan's own HYPOTHESIS about `PLAN-26`'s missing tests** — confirmed by reading the merge:
  the four lifecycle-line cases, the four refusal paths, the 401/404 gap and the three
  `Generators` gaps are all as the spec describes.

## 4. Findings

| Finding | Disposition |
|---|---|
| Self-review r1: the view-exit path had no test | fixed (`760ad6e`, `ActionsFullScreenTest`) |
| Self-review r1: the config refusal evidence targeted pre-PLAN-26 code | fixed; the test now runs `config show` |
| Self-review r1: a mutation run predated the final test | re-run at the final tests |
| Self-review r1: `suspendFor` re-entered the alternate screen after `stop()` | fixed |
| Self-review r2: the design sentence was over-general | narrowed, then corrected again for the console path |
| Self-review r2/r3: the hold capture and the two view-only handlers | reported as not met, see §1 |
| Self-review r3: `Generators` input, the token split, the fixture target | fixed at `5cb693e` |
| CodeRabbit: `PmConsole` discards a view verb's `130` | the design sentence now says what happens: the verb exits `130`, the console returns to its prompt |
| CodeRabbit: `refreshActiveLevels`/`refreshSystem` read the runtime after the interrupt | fixed at `2586a83` |
| CodeRabbit: keystroke literals in `InterruptDialogueIT` | the typed reason now comes from `Generators`, the class carries the annotation |
| CodeRabbit nitpick: the config fixture answered every target | fixed; only `/api/v1/config` is refused |
| Sonar: `java:S1068` unused constant, `java:S3358` nested ternary | fixed at `b39f9f5` |
| Sonar: new-code coverage 64.2 % then 73.1 % against 80 % | the operator chose to keep adding unit coverage; `db61a7c` brought it to 80.6 % and the gate to `OK` |

No thread is open on the pull request.

## 5. Reviewers

| Reviewer | Required | State | Head reviewed | History |
|---|---|---|---|---|
| `coderabbitai` | yes (quota-limited) | reviewed, then refused on quota for every later head | `7814860` (`kind=reviewed`, merge risk Low) | triggered at 01:49 and after each push; refused 08:26 ("52 minutes") and 09:24 ("13 minutes"); the review of `7814860` is the qualifying review the standing authorization rests on |
| `sourcery-ai` | no (optional, quota-limited) | refused | — | refused at 01:49 on its own 7-day budget (250 000 diff characters), next review in ~20 hours; optional, does not gate |

**Standing review authorization** (quota-limited reviewer, head `7814860`, current head
`bcfd4297`): a qualifying review exists; each of its 8 actionable findings and its nitpick is fixed
in a pushed commit or answered, and every thread is resolved; it was re-triggered after the last push
and the refusal is recorded; every other gate condition holds on the current head. The operator
selected "Merge now" in a question that named this exact gap.

## 6. Operator questions

1. *"PR #303 is green on every build and test job, but SonarCloud's quality gate fails on ONE
   condition: new-code coverage 73.1 % against 80 % … How should I proceed?"* — options: **Keep
   adding unit coverage (Recommended)** / Allow a targeted Sonar exclusion / Stop and leave the PR
   open. Selected: **Keep adding unit coverage**.
2. *"PR #303 is green on the current head `0c92ada1` and every gate condition holds except that the
   required reviewer has not reviewed this exact head … Shall I merge?"* — options: **Merge now
   (Recommended)** / Wait for the next CodeRabbit review / Leave it open. Selected: **Merge now**.

No free-text operator messages were sent.

## 7. Rule check

- **Guard:** `resolve` reported `guard: none`; the rule check reports `guard: none on this harness
  (unenforced)`. Nothing was enforced, so "all kept" is not claimed.
- **Kept:** every script through `.plan/execute-script.py`; every build through the resolved
  `build` commands; every CI action through `plan-marshall:tools-integration-ci:ci`; git through the
  resolved git-workflow verbs; one command per shell call; files read and searched with the file
  tools; temp files only under `.plan/temp/`; every repository edit inside the worktree; no
  `git stash`/`restore`/`checkout --`/`rebase`, no forced push; nothing under `.plan/` written
  except the temp area, the two `ci` body slots and the one landing message.
- **Executor skew:** `match` (executor 0.1.2036, lane 0.1.2036), recorded here as the lane asks.
- **Spec read:** `spec_source: ledger_worktree`, no `spec_differ` marker at the start or at landing.
- **Deviations the run recorded rather than hid:** builds ran with `--execution-mode in_process`
  because every routed build of this worktree failed daemon-side (`execution_failed`, exit `-1`,
  empty maven log); the two timeout retries of the wall-clock bound on a machine at load ~87 are
  recorded in the progress file; the "before" capture run used reverse-applied patches, not an older
  commit; the coverage question and the merge question were put to the operator, never assumed.
- No sub-agent broke a rule: the two review sub-agents were given section 1 verbatim and reported
  only.

## 8. Build server

Setup preflight: `ready`. Routing: `auto` resolved to the daemon and the daemon answered
`execution_failed` with exit `-1`, 1 s and an empty maven log at 22:28:47 — the same daemon-side
condition PLAN-86 recorded; every build of this run therefore used the `in_process` fallback
(`mechanism: in_process_fallback`). No job was `killed`. Final gate at the merged head: 2609 tests
green; the module's integration tests 578 green; `runtime_stamp_reason` was absent.

## 9. Residue

- **The view-suspended reader drops `Ctrl+C`.** A dialogue the full-screen view opens reads through
  its own terminal while the view's reader still holds the file descriptor, so the byte is consumed
  there and never reaches the dialogue. The mapping is correct and proven
  (`InterruptExitCodeTest`, `ActionsFullScreenTest`), but a real operator's `Ctrl+C` in those two
  dialogues does nothing. This is why D1's two Done-whens are not met as written, and why the hold
  capture leaves with `q`. Fixing it means one terminal for the view and its dialogues, which reaches
  fourteen `createTerminal()` closers; `terminal.pause()`/`resume()` around `suspendFor` was tried and
  changed nothing.
- **D2's hypothesis was refuted**; the three goldens are green on `main` since `7d7b4d71`.
- **`OverviewFullScreenApp.askYes`** still folds `Ctrl+C` into "no" (an act confirmation the spec's
  eight do not name); the design now says which dialogues map the interrupt and which still leave
  back to the view.
- **Residual literal input** in `SystemLifecycleLineTest`/`SystemViewConsistencyTest`'s runtime
  metrics (a reviewer preference, not the Done-when).
- The three goldens and the design documents were measured and, where stale, corrected; the sibling
  transcript ITs' scripted keystrokes were left as they are, so the package keeps one convention.

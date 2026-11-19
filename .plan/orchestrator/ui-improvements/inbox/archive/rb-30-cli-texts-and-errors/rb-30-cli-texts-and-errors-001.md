envelope_version=1
sender_type=plan
sender_id=rb-30-cli-texts-and-errors
epic=ui-improvements
kind=landing
created=2026-11-19T01:11:48Z

```landing-facts
schema=landing-facts/1
plan_id=rb-30-cli-texts-and-errors
epic=ui-improvements
pr=#438
merge_state=merged
cleanup_owed=false
deliverables_total=9
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=29
review_body_findings_handled=29
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-30 — terminal texts, one error pattern, exit codes and dialogue residue

PR #438 merged through the merge queue as `2efcaa4058743c0b6090f0db68f4c469860d299d`. This is a resumed run: the first
run (two lane sessions, 2026-11-14) delivered D1–D9 with recorded test-first evidence, opened the pull request at
`8c6d3fd6` and ended without a landing message; its CodeRabbit and Sourcery triggers never produced a review, and CI was
red on Sonar's new-code coverage (78.3 %). This run executed the spec's Resume Amendment 2026-11-18: the size question,
the merge of `origin/main`, the catalogue of what `main` added, the coverage lift, the builds, the captures, the review,
the Sonar read and the merge.

## 1. Deliverables

D1, D3 and D9 are fully met. D2, D4, D5, D6, D7 and D8 are not; each verdict is graded as written, with the order the
evidence has.

| Deliverable | Done-when | Order | Verdict | Evidence |
|---|---|---|---|---|
| D1 strings | `LiteralTextGuardTest` fails on a literal passed to print/println/printf/readLine/askRaw; guarded list gains `AnswerCommand`, `CleanupCommand`, `RuntimeCommand` (+ sub-packages) | test-first | met | failing run job log `fd0e63d109b747c699388477f80f9fdd.log` at `018cb1b7`; `LiteralTextGuardTest.java:66-75` lists the files, `:197` walks the tree |
| D1 strings | terminal check, `running-work`, both locales, four commands, exit codes, before/after | none | met | `.plan/temp/terminal/PLAN-30/{before,after}/` (16 cases) and `screen-captures/PLAN-30.txt` |
| D2 renderer | unit test: three lines for a known code, unknown fallback, both languages, no HTTP/JSON/exception class | test-first | met | log `5f2f1ae74dfc4dd68a5c874ae6e4d153.log` at `28298ae0`; `RefusalRendererTest.java:32-98,143-153` |
| D2 renderer | terminal check: `answer Q-999 1` shows the change, both locales, exit codes | none | met, re-taken | captured again at this head with `-Dpm.screens=PLAN-30`; `.plan/temp/terminal/PLAN-30/after/PLAN-30-answer-120x30-{en,de}.txt` read the three catalogue lines (`This entry does not exist` / `Diesen Eintrag gibt es nicht`), no status, no body |
| D3 picocli | in `de_DE.UTF-8`, `bogus` and `-h` print no English word outside the glossary | test-first | met | log `e6adfa90b49141cb8b649dfe83d94729.log` at `c3a9dfb2`; `PicocliLanguageTest.java:73-134`; captures `PLAN-30-bogus-80x24-de.txt`, `PLAN-30-help-120x30-de.txt` |
| D3 picocli | terminal check, four commands, both locales | none | met | the same capture set |
| D4 exit codes | `ExitCodesTest` checks success, refusal and usage error of every verb | test-first | **not met as written** | failing log `2b36580ce61b4fdd897437ea06e9770a.log`, green `29a74b0d7d5b4e2ca3106d3fd518ca40.log`, commit `25227ed8`; but the test covers every verb for *usage* only, 24 hand-listed invocations for refusal, and five verbs for success (`ExitCodesTest.java:109-115,122-149,179-201`); `130` is not covered |
| D4 exit codes | terminal check | none | met | `PLAN-30.txt` expected exits 1/2/1/0 |
| D5 display residue | a test per item fails before the change | test-first | **not met as written** | logs `9132f080e389463eb0a9f24ea12f7c61.log`, `27ade3bfa38e4bb79c0ea3085b35a255.log` at `2e0b3554`; the item "the printed `plan report` is English" is neither done nor tested (`PlanCommand.java:1053,1061,1064,1096,1099,1115,1265-1267,1302,1305` still hold literals) |
| D5 display residue | `git grep -n "MessageCatalogue.get(" -- pm-operator/src/main` names no call without a locale on a screen this plan touches | none | **not met as written** | `PlanCommand.java:620,624` still call `get(key)` with no locale |
| D6 config residue | a test per item fails before the change | test-first | **not evidenced** | the fix commit `dc96a9f5` is recorded; its progress line names no failing-run log or commit. The four tests exist |
| D7 PLAN-160 residue | failed rotate check exits 1 and shows the failure; consent asked; ready provider verified and unready not; second conflict prints the line | test-first | **not evidenced and not met as written** | fixes in `cc51536d`, but no failing-run identifier is recorded, and no test asserts that an *unready* provider is not recorded |
| D7 PLAN-160 residue | the three thread replies are posted (#421 `4550425785`, #415 `4543426060`, `4545310611`) | — | **not met** | only `PRRT_kwDOVBOtss62neL_` on #421 is recorded; no #415 reply is on record |
| D8 PLAN-128 residue | the listed tests, each failing on a revert | test-first | **not met as written** | logs `ab1524412fd2448992d222266f440daf.log`, `ea331db24bdb49dc810488085df2f22f.log` at `aacb1859`. `--project .` was neither tested nor fixed before this run (fixed here in `7f1d4f19`); no test pins "remind offers no never" or the mock's `project_id` event; the reminder label read the default instead of the configured value (fixed here in `95936b6f`) |
| D8 PLAN-128 residue | terminal check of the choice bar, the delete confirmation and four areas, both sizes, both languages | none | **not met as written** | 24 `PLAN-128` captures exist and ran; no capture contains the choice bar or the delete confirmation |
| D8 PLAN-128 residue | the #425 replies posted and threads resolved | — | **not evidenced** | only the progress claim |
| D9 PLAN-127 residue | launchable plan with an unknown count launches without the raise question; `n` prints the leave line; U+2028 a space; no English literal in German; a credential-path act body not recorded; each fails on a revert | test-first | met | logs `d83ca0a44e9f4589843de50b5919f00a.log`, `0455bcbc4cbc462982dfcd43b79e3803.log` at `014bd8c4`/`400aa1ad`; `EpicCommandTest:441-454,486-497`, `ControlDialoguesTest:852-866`, `TerminalSanitizerTest:134-143`, `StatusCommandTest:81-138`, `MockRuntimeEngineTest:2603-2637` |

Resume obligations of this run: size question asked and answered; `origin/main` merged (`855e9cfc`, three conflicts kept
both sides); `main`'s literals catalogued (`348e9c44`) and three more rounds of review findings fixed; coverage lifted
from 78.3 % to 83.8–84.6 % on new code measured by Sonar; the three Standing-Constraint builds and the capture set
re-run; the review held; the merge done.

The words "fail-first" and "observed failing before the fix" are used only for the orders above; a `none` Done-when is
never described as failing.

## 2. Personas and skills

`persona-implementer` for D1–D8 and D9's code; `persona-module-tester` for the tests added in this run;
`persona-code-reviewer` for the self-review and for triaging every review finding. Declared domain skills, per
deliverable: D1–D4 `java-core, java-lombok, javadoc, java-null-safety, junit-core, cui-testing, cui-logging`; D5–D7 and
D9 `java-core, junit-core, cui-testing`; D8 the same plus `pm-documents:ref-asciidoc`. No skill was added for surface
drift; `skills: none named` does not apply.

## 3. Hypotheses

- "`LiteralTextGuardTest` does not cover dialogues and `askRaw`" — **refuted** as recorded in the spec: the pattern at
  `LiteralTextGuardTest.java:78-84` already carries `prompt.askRaw` and `session.out()`, and `ChatDialogue`,
  `StartDialogue`, `WorkActionDialogue` and `HoldDialogue` are in the list. D1 narrowed to the guarded list, as the spec
  says.
- "the answer route's refusal body carries the four design codes" — **confirmed** against the mock's answer route
  (`MockRuntimeEngine.java:11386-11394, 11531-11533`): `conflict`, `already_answered`, `invalid_answer`,
  `dependency_unanswered` are the codes the client renders, and this run's tests drive all four.
- "`OperatorRefusedException` builds the English literal 'refused by the runtime (HTTP n)'" — **confirmed before the
  change**, gone after it (`git grep "refused by the runtime" -- pm-operator/src/main` is empty, recorded by the first
  run).
- "`decision.refused.*` has two keys and no `pm-operator` reader" — **confirmed then superseded**: the renderer reads all
  four, and both packs carry them.
- "`EpicCommand.NextCommand` holds English option descriptions and the success literal" — **refuted**: the options are
  catalogue keys with English `description=` fallbacks and the success line comes from `WorkActionDialogue` (PLAN-127
  amendment, item 1).

## 4. Findings

Self-review round 1 (sub-agent, `general` with `persona-code-reviewer`): 8 majors, 15 minors. Fixed in this run: `cleanup
--project .` paged as the unfiltered view; a refusal reads the body's message and a JSON body never reaches the title; the
two false design lines (refusal reads `error.*`, exit 1 includes local refusals); `config cleanup d)` prints its refusal
and exits 1; a kept conflict value is not reported as written; `durations.after` pluralises; all four decision refusals are
covered in German. Round 2 confirmed those closed and found two new: the kept line said "nothing was written" in a mixed
run (fixed), an orphaned key (removed). Still open from the pre-existing residue: the design's `epic next` sentence
against the code, the English `plan report` body, the dead cleanup keys that freeze invented figures, the literal guard
not inverted, the invented leave line with a raw state id, local refusals printed through the runtime's prefix, "Answer
part 1 first", the dead exit-4 branch, and the minor catalogue/doc items — all reported, none re-done (the resume
amendment forbids redoing D1–D9).

PR review (CodeRabbit, four reviews): 29 body findings, 29 handled — 26 fixed in the branch (`9a1f113c`, `7b9d5951`,
`56a86791`, `e9436289`, `95936b6f`, `de9c0bb4`, `391cf1f6`, `238a9d29`, `3b55f861`) and 3 answered with the reason stated
on the pull request (a shared cleanup-kind definition in `pm-api`; the dependency refusal naming no part the client can
derive; the guard and help-test derivation, which is D1's follow-up). Each thread was replied to with its commit or reason
and then resolved; `review-gate` reads `unresolved_review_threads=0`, `review_body_findings_total=29`,
`review_body_findings_handled=29`.

Two defects the run's own tests found: an `invalid_answer` re-sent the same answers forever (a hang; fixed in `2e4b3271`
with the regression test), and the first run's captures were never regenerated because `ScreenCaptureIT` only runs with
`-Dpm.screens=<set>` (the run re-took them).

## 5. Reviewers

- **coderabbit** (required, `quota_limited`, `review_complete_after_handling`): refused on quota at 2026-11-18T17:03:06Z
  with no review on record; per §6 step 7 the run waited and re-triggered no sooner than 90 minutes after the refusal
  (posted 18:02:58Z). It then reviewed four heads — `e23d1279` (18 findings), `de9c0bb4` (5 + 2 outside-diff + 2
  nitpicks), `9a1f113c` (2 + 2 outside-diff), `6f6a3deb` (1 + 4 outside-diff) — with its marker on `6f6a3deb`
  (`kind=reviewed`). Every finding handled; later heads (`e9436289`, `9f67f858`, `213b0b02`) were not re-triggered
  because the flag applies and the review is on record.
- **sourcery** (optional): refused structurally (150,000 diff characters) on 2026-11-14; recorded, does not block.

## 6. Operator questions

1. "How should the resumed run proceed on the size rule (84 files, +3990/-551, 46 behind / 15 ahead origin/main
   9a0f24e8)?" — options "Finish #438 as ONE pull request (Recommended)" and "Split the remainder into a follow-up plan";
   selected **"Finish #438 as ONE pull request (Recommended)"**. No other question was asked. No free-text operator
   message was sent to this run.

## 7. Rule check

- **guard: none on this harness (unenforced)** — the resolve result reported `guard: none`, so no guard-log line exists
  and none is claimed.
- The run's own shell usage that bent the shell rule: `cp -R` twice (the capture set into `.plan/temp`, twice), `ls -l`
  once (the capture timestamps), and `grep -c`/`grep -rl` once each, the second in the main checkout against the wrong
  directory. The self-review sub-agent disclosed one `git … grep` for a repository-wide search. Everything else was read
  with the file tools.
- Every build went through the resolved build canonicals on the default routing; no build was backgrounded, none
  fell back. One job was `timeout` (job `fc10072a028b48c9be039ab157f43a2c`, 901 s): a newly added interactive test hung
  the suite, so the daemon killed it at its bound — not a launch fault, no blind retry; the cause was fixed in `2e4b3271`
  and the build re-run.
- Restarts/kills: none of this run's sub-agents was killed; the two self-review rounds ran to completion.
- The spec read: `spec_source=ledger_worktree`, no `spec_differ` marker.
- `executor-skew`: **match** (executor 0.1.2149, lane 0.1.2149).
- The mailbox was never readable: `orchestrator inbox read --plan-id 30-cli-texts-and-errors` answers
  `invalid_target_plan` (the leading digit is refused by `^[a-z][a-z0-9-]*$`). Reported here, never read as no mail.

## 8. Build server

Setup preflight on the worktree: `ready`. Every build ran `daemon_longpoll` (no `in_process_fallback`, no `degraded`
result, no `executor_absent_from_worktree`); `build-routing` answered `auto`. Queued time was observed once (615 s behind
another slot) and handled by the client's own wait. The gate at the merged head (`213b0b02`) is green, job log
`a0d6c826330d4068a10482b2a7eebeba.log`; the integration-test run green (`-Pintegration-tests` job log
`d1de7ea9a15b49db8b85eeca41e0573e.log`), the module verify green (`9db96f8b942646a3a52a6c7694377591.log` at `7dfff034`).
`runtime_stamp_reason` was not carried; `plugin_version` is `unknown` (the resolve result names none).

## 9. Residue

Reported, not re-done: D2/D4/D5/D6/D7/D8's own gaps above; the design's `epic next` sentence against the code (say which
wins before changing it); the English `plan report` body; the dead cleanup keys that freeze invented figures; the literal
guard's inversion and the derived lists the review asked for (aspect list, duration kinds, project kinds' mirror,
`PROJECT_CLEANUP_KEYS`); a local-refusal rendering path that is not "the runtime refused"; the dependency refusal's part
number (needs the runtime to name it); the entry-link ids for two checkouts sharing a basename; the mailbox read's plan-id
validator; D7's unready-provider test and its three thread replies; D8's #425 replies and its choice-bar captures. The
self-review is reported `budget-exhausted` for exactly this residue; the operator's one decision on it was the size
question, answered.

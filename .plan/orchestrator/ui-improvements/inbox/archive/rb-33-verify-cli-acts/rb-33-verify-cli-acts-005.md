envelope_version=1
sender_type=plan
sender_id=rb-33-verify-cli-acts
epic=ui-improvements
kind=landing
created=2026-11-10T19:40:39Z
revision=1
amended=2026-11-10T19:41:19Z

```landing-facts
schema=landing-facts/1
plan_id=rb-33-verify-cli-acts
epic=ui-improvements
pr=#398
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2090
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=7
review_body_findings_handled=7
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

Two required facts carry `unknown`, and `inbox landing-check` reports them as `missing_keys`: `total_tokens` (this
harness reports no token count, so nothing was measured) and `plugin_version` (the flat install's `resolve` answers
`unknown`; the executor's own version, `0.1.2090`, is stamped above). Neither is a guess and neither is `n/a`.

## Deliverables

Verified commit `0913d6f3` (`main`), the base of the PR; the merge commit of the landing is
`344515f91dd362d7dd7268896294377a26b6bc30`. Every Done-when carries `evidence order: none` (this is a verification
plan: the deliverables are observations, not fail-first tests).

**D1 — Run the checks: met.** Eleven checks, twelve rows (V-05 split into its two parts).
* `CLI-UC-V-01` pass — order none — `PauseDialogueIT` (3) green; `10-cli-choice-prompt-pause/pause-q|9|enter-*` plus
  `PLAN-33/pause-q|9-80x24-{en,de}` and `pause-q|9-120x30-de`; all three options carry their consequence; `q`/`v` prints
  "Left the pause. Nothing was sent." / "Verlassen: Das Anhalten. Es wurde nichts gesendet."; `9` re-asks with the keys;
  Enter prints "✓ fix-relay pausing after the task".
* `CLI-UC-V-02` pass — `RuntimeStopDialogueIT` (8) green; `17-cli-runtime-act-confirm/stop-screen-{120x30,80x24}-{en,de}`
  name the three choices (the first two with their consequence); `PmOperatorTest.stopNoInputStopsAtTheChoice` = exit 3.
* `CLI-UC-V-03` pass — `ActionMenusTest` (15) green; `20-cli-actions-by-state/actions-waiting-*` shows `!  Decide Q-009…`
  first, `actions-paused-80x24-de` shows Start, Abandon, Chat, Plan settings.
* `CLI-UC-V-04` **fail** — `AnswerDialogueIT` (9) green and the screen shows votes, dissent, evidence and prompt, but the
  German run carries the English decision payload, so "no English word outside the glossary" fails. Finding
  `rb-33-verify-cli-acts-004`.
* `CLI-UC-V-05` **not verified** — notices/badge part pass (`ConsoleNoticeIT` (2) green; `PLAN-33/notice-opened-*`,
  `notice-answered-*`, `badge-*` in both locales and sizes); the typed-text clause has no harness that can type before
  a broadcast.
* `CLI-UC-V-06` **fail** — `OnboardingDialogueIT` (4) and `ProjectSetupDialogueIT` (4) green; `h`→Enter gives
  `[1/7] Language`; the walk reaches `onboard.completed`; `project setup` shows id/name/pilot — but `onboard --auto --yes`
  is not a verb (exit 2). Finding `rb-33-verify-cli-acts-001`.
* `CLI-UC-V-07` pass — `CleanupDialogueIT` (6) green; `PLAN-23/PLAN-23-cleanup-choose|confirm-*` (four arms) show the
  `d r i x o q` prompt and the `y`/`n` confirmation; `q` deletes nothing (asserted over `/mock/acts`).
* `CLI-UC-V-08` pass — `draining` on the first line; `SystemViewConsistencyTest` (5) green (same listeners, same token
  units in both views); `config show` exits 1 in all four arms.
* `CLI-UC-V-09` **fail** — interactive `plan nope` opens an empty view with no refusal line; `plan nope --print` says
  `Plan nope not found.` (not `No plan nope`); the full-screen overview shows `Nothing runs` when the projects read is
  refused. Finding `rb-33-verify-cli-acts-002`.
* `CLI-UC-V-10` **not verified** — the operator's agreement is not a check a plan can run (spec Amendment).
* `CLI-CC-V-06` **fail** — seven of the guard's eight dialogues have the three PTY cases; the waiver has only its main
  path, and the check's "every dialogue that acts" also reaches accept-edit. Finding `rb-33-verify-cli-acts-003`,
  with the accept-edit gap recorded in the result file (the filed message is archived and cannot be amended).

**D2 — Record the result: met.** `doc/claude_review/verification-results/verify-cli-acts.adoc` created, one row per
check with verdict, evidence and note, the check-to-use-case mapping, the findings, the residue and the review record.
`doc/claude_review/cli/use-cases/analysis.adoc` moved: 15 → 8 `conforms`, 8 → 15 `partial`, 7 → 1 `deviates`, 7 → 0
`missing`, 13 `not verified`. `asciidoc validate`: result file 0/0; analysis keeps exactly its four pre-existing
attribute errors and five pre-existing list warnings. `asciidoc verify-links`: 6 and 37 links, 0 broken. `doc-review`:
its concept-corpus phases do not apply to a verification result file; the operator accepted the applicable checks
(validators + read-through) as the pass (question of 2026-11-10, option "Accept the substitution").

**D3 — Route what failed: met.** Four findings for four `fail` verdicts: `rb-33-verify-cli-acts-001` … `-004`.

Pushed heads tested: every head carried a green `mvn test -pl pm-operator` (118 classes) and a green
`mvn verify -Pintegration-tests -pl pm-operator` (`failsafe-summary: completed 126, skipped 7, failures 0, errors 0` =
119 executed), repeated in-reactor (`-am`, 656 tests) after the review named the SNAPSHOT risk; plus eleven green
`ScreenCaptureIT` runs.

## Personas and skills

* D1: `plan-marshall:persona-auditor`; domain skills: none named by the spec.
* D2: `plan-marshall:persona-documenter`; `pm-documents:ref-asciidoc` (declared) — used for the validators.
* D3: `plan-marshall:persona-auditor`; domain skills: none named.
* Every self-review round and every PR-comment triage: `plan-marshall:persona-code-reviewer`.
* `skills added`: none — no surface drift.

## Hypotheses

The spec's Claim Labels: the checks are stated in the review's verification documents — **confirmed** at
`doc/claude_review/cli/use-cases/verification.adoc:30-87` and `doc/claude_review/cli/cross-cutting/verification.adoc:45`
(both unchanged at the verified commit). The verify-first clause — all named dependency plans landed — **confirmed** (a
landing record for each, and no `@Disabled` in `pm-operator/src/test`).

## Findings

Self-review rounds: 1 (11 items), 2 (15), 3 (15), 4 (8, operator-authorised) — all fixed, the last round opened the
`CLI-UC-V-04` fail and the fourth finding. PR comments: three inline threads from the first review (walk: answered with a
stated reason; accept-edit and plan-nope: fixed in `920d7444`) — replied and resolved; the review-body "preserve the
captures" item answered with a stated reason; the second review's four items fixed in `0a82991`/`9b228f3b` — replied and
resolved; the third review's outside-diff citation fixed in `1132e622` — answered. `review-gate` on the merge head: 0
unresolved threads, 7 of 7 review-body findings handled.

## Reviewers

| Reviewer | State | Head(s) | History |
|---|---|---|---|
| coderabbit (required, quota-limited) | reviewed, then refused on the merge head | `60b42372`, `920d7444`, `9b228f3b`, `1132e622` (clean) | refused 10:14Z, 11:32Z, 13:03Z on quota (90-minute rule honoured after 13:03Z); escalated `@coderabbitai full review` 14:34Z; reviewed 14:45Z; reviewed 16:00Z; reviewed 17:06Z; reviewed 18:00Z clean ("No actionable comments"); re-triggered 18:43:57Z on `1b2afd18`, refused 18:44Z — standing authorization (quota-limited, qualifying review exists, all findings handled, every other condition green on the head) |
| sourcery (optional) | reviewed / approved | `60b42372`, `920d7444`, `9b228f3b`, `1132e622` (approved) | no findings |

## Operator questions

1. "How should D2's `doc-review` clause be graded?" — options: accept the substitution / grade not met as written / run
   the full skill. Selected: **"Accept the substitution"**.
2. "The self-review has used its three rounds … how do you want to proceed?" — options: run a fourth review round /
   proceed with `budget-exhausted`. Selected: **"Run a fourth review round"**.
3. "Do you want me to merge PR #398?" — options: merge / leave open. Selected: **"Merge PR #398"**.

No free-text operator messages beyond these answers.

## Rule check

* `guard: none on this harness (unenforced)` — no guard log line exists, and none is claimed.
* Deviations this run records: (a) the first capture of `plan nope` used `--print`, which the review correctly called
  out; the interactive path was captured in `0a82991`; (b) the CodeRabbit re-trigger at 11:32Z came 78 minutes after the
  first refusal, 12 minutes inside the 90-minute rule — recorded, and the rule honoured from the second refusal on;
  (c) the self-review ran a fourth round beyond the three the lane names, on the operator's selection; (d) the run read
  `../plan-marshall-documentation/.claude/skills/doc-review/SKILL.md` (outside the repository root) to judge what
  `doc-review` covers — read-only, no secrets, reported here as a departure from the "stay inside the repository" rule;
  (e) two `date` calls and one `test -d` shell call (the latter a file test) were used — no file content was read or
  written through the shell.
* Spec read: `spec_source = ledger_worktree`, `spec_differ = true` (the worktree's own `.plan` copy predates the
  Amendment; the ledger copy was read throughout, and one self-review round's "missing Amendment" report came from the
  stale copy and was refuted).
* `executor-skew`: `match`, executor `0.1.2090`, lane `0.1.2090`.
* Server restart: the lane text was re-read at 12:22Z; the in-flight wait call was cancelled and re-issued, nothing
  else was lost.

## Build server

Preflight at setup: `ready`. Every build was submitted to the daemon and ran as `daemon_longpoll`: one `mvn test` run,
three IT runs (two `-am`), eleven `ScreenCaptureIT` runs and four whole-tree gate runs. Fallbacks: `none` — no build
degraded and no launch fault occurred.

## Residue

* `config show` prints an HTTP status on the operator's screen (`Unable to load settings: HTTP 404`; German
  `Einstellungen konnten nicht geladen werden: HTTP 404` — only the status is raw). The check allows the exit-1 arm; the
  line is the shape PLAN-124's landing records as its own residue.
* The German cleanup body is English (hard-coded literals in `CleanupCommand.printJob`, `CleanupCommand.java:633-637` and
  the confirmation), the German console prints `reports shows them`, the paused plan's menu prints `(no configuration of
  replan qualifies yet)`, and the German `plan nope` refusal is English. All outside the eleven checks; recorded in the
  result file's residue.
* Finding `rb-33-verify-cli-acts-002` names PLAN-20 for the overview defect, which is wrong (the overview entered with
  PR #130; PLAN-05 declares the file, PLAN-20 touched it, neither authored `fetchProjects`). An `inbox amend` was refused
  (`not_live`, archived); the correction is in the result file and the PR body.
* `CLI-UC-V-05`'s typed-text clause and `CLI-UC-V-10` remain `not verified` with their reasons.

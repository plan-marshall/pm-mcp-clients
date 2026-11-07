# Landing analysis: PLAN-97 cli-command-tree-drain-and-accept-edit

epic: ui-improvements
workstream: WS-04
pr: 358

PR #358 `d8efe35f` (squash-merged 2026-11-07T14:38:46Z through the merge queue; 25 commits, 72 files, +5248/-3397). A /plan-marshall plan (plan-marshall lane); landing message `inbox/plan-97-cli-command-tree-drain-and-accept-edit-007.md` created 14:51Z, with six earlier messages (001-006: candidate lessons, mostly plan-marshall tooling: merge-queue reroute, PR_BODY squash, a made-up SHA). The lane used 35,974,305 tokens and 184,058 s. The first pull request for the branch, #356, was CLOSED 2026-11-06 (the PR body: at the operator's request so CodeRabbit saw a fresh PR; the landing message: after the re-review-timeout question); it never received a CodeRabbit review. Analysis by a read-only sub-agent (code read at the merge commit, nothing built or run); thread counts, the review, Sonar and the central code claim were re-read by the orchestrator (marked V*).

## Merge rule

MET ON THE LETTER, WITH OPEN HANDLING GAPS. One CodeRabbit review (`5810938147` on `c31f2090`, 2026-11-07T08:03Z: "Actionable comments posted: 24" plus a body with Major, Minor and nitpick items; a second review `5810943271` has a 227-character body); the later heads (`84832aee`, `e483c22e`) got no review (re-triggers refused or "already reviewed"; the `rereview-timeout-override` authorisation matches the standing rule).

- Unresolved threads at the merge head (V*: GraphQL): PR 358 0 of 24; the PLAN-95 threads of PR 327 are resolved too.
- Review BODY items: the body carries 12 items beyond the 24 threads (1 Major, 3 Minor, 8 nitpicks, per the sub-agent's count); NONE has a reply anywhere, and the PR body and the landing message say "24 comments triaged" without them. Still unchanged at the merge: the Major (`DrainCommand:84-85`, verify process identity before signalling), the two "derive from the design / command model" items (`PmOperatorTest` keeps the literal `DESIGN_VERBS`, the `PmConsole` completer a literal list), and the test of the machine-derived drain bound (`DrainCommandTest` never mentions `machine-config` or `drain_timeout_seconds`). The same class of miss as PLAN-104.
- SonarCloud (V*: check run SUCCESS; bot comment on `e483c22e`): Quality Gate passed, 0 new issues, 0 accepted, 0 hotspots, 86.8% new-code coverage; rules none; the landing message carries `count_status=confirmed`, 0.
- CI: Build 25 and 26, integration tests and Sonar green at the head and on `d8efe35f`; the only red check is `Scorecard`, which also fails on `1d8c6197` (standing). Playwright is not run in CI.
- Landing facts: `unresolved_review_threads`, `review_body_findings_total/handled`, `sonar_open_new_issues` missing (only `step.sonar-roundtrip.*`).
- Review waivers: `functional-review`, `code-review` and `simplify` were closed by `loop-back close` as an operator override with 0 findings at `e483c22e`; the PR body says 13 advisory code-review findings stay open, recorded only in the plan's decision log.

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1 `DrainCommandTest` (signal, bound, no record, no HTTP, `--withdraw` exit 2) | MET (`DrainCommand` ~95-130; tests `DrainCommandTest:153,272,287,306,317,332,346`) | PARTLY: the identity check is weak (defect 1) and the machine-derived bound is untested (defect 2) |
| D1 mock tests withdraw / signal refusal / too late | MET (`RuntimeStopDialogueIT:118,132,147`) | texts are the design's |
| D1 screen captures | PARTLY (`screen-captures/PLAN-97.txt`, 12 captures) | the PR's terminal-check checkbox is unchecked; before/after not seen |
| D2 `AcceptEditCommandTest` (confirm first, old form exit 2); `enrolment_tampered` through to the project served again | MET (`AcceptEditCommandTest:163,351`; `AcceptEditDialogueIT.resolvesATamperedEnrolment`; Ctrl+C and end of input send nothing, `:198,210,228`) | sound; a `parkNanos` condition poll with a deadline (LOW) |
| D3 `PmOperatorTest` registry vs design, folded verbs exit 2; decisions per verb in the PR; design lines; `operator.key.*` keys | MET (22 verbs; 7 folded verbs expect exit 2; `01-commands.adoc`, `17-projects.adoc`; 4 keys en and de, parity 0 diff) | the expected verb set is a hand copy of the design list; the `--help` test (`help.contains("project")`) is trivially true; Q1 (`02-operator-channel.adoc:42` still names `enrol`) is a companion PR "not yet opened"; Q3 sub-verbs listed without a decision |
| D3 "transcript test of `help` lists the design's verbs only" | NOT MET (the en/de `help.more` text was rewritten; `PmConsoleTest:144` asserts only `"Commands:"`) | one verb list in three places: the property, the completer, the test |
| D4 lock order, generation, drain-scoped cancel, atomic switch | MET (`MockRuntimeEngineTest:1292,1341,1393,1423,1467,1509,1544`) | "red on revert" runs are the lane's claim |
| D4(f) `serveNeedsOperatorPage` | NOT MET (code and Javadoc unchanged, `MockRuntimeEngine:5946`; no reason given) | the reason may sit in the decision log |
| D4(i) replies on threads `60EE-P`, `60EE9o`, `60EE-Z` and follow-up issues | NOT MET (PR body: "not sent yet"; `gh issue list` empty) | |
| D5 generic catches and markers in `EpicCommand`/`PlanCommand` | MET (`git grep "catch (Exception\|TODO: Catch specific"` on both files: none) | the catches became `catch (IOException _)`; `FailingOnceWriter` and six tests prove the swallow is gone; other runtime exceptions of the parsers may now escape (L) |

## Defects (at `d8efe35f`)

1. HIGH V* `drain` can signal an unrelated process. `RuntimeRecord.processAlive` (`pm-api/…/runtime/RuntimeRecord.java:66`) returns true when the record has no start instant (`startInstant == null`) or when the process info has none (`.orElse(true)`); `DrainCommand` (`call()`, `readRuntimeRecord().filter(RuntimeRecord::processAlive)`) then `destroy()`s the handle (SIGTERM): a reused pid is signalled. This is the unhandled CodeRabbit Major; the PR body's "signals only a live recorded process whose start time matches" is not true in those cases.
2. MED V the drain bound is derived from `machine-config.json` (default 600 + 5 s + 10 s, `boundOfMachine`, `drainTimeout`); no test covers zero, a non-default value or an invalid config (the tests inject a duration).
3. MED V the 12 review body items are unanswered and the "24 handled" claim omits them.
4. MED V the `help` Done-when has no test; verb lists exist in three places.
5. MED V D4(f) and D4(i) are not done; no follow-up issue was opened.
6. MED V the real daemon's behaviour is recorded nowhere in the PR: the withdraw `DELETE /api/v1/operator/runtime/stop` is answered 405 by the real server (the spec says so) and `runtime stop` POST moved from `/api/v1/runtime/stop` to the operator route; whether the real `pm-mcpd` serves that route is unverified (L); the web app still posts the legacy path (PR body).
7. MED V `project add` is only the old `enrol` (a direct enrol request): the onboarding dialogue left the shell with `onboard`; the CodeRabbit Major (`ProjectCommand:198`) is "accepted as absorbed behaviour"; the design line says `project add` onboards a project; no open item, no issue.
8. MED V other threads "accepted as unchanged behaviour" with no issue: `project release`/`project unenrol` have no interactive confirmation or reason prompt (`--yes` required, else exit `confirm_required`); lesson claim at plan creation, relative checkout path, store digest on unenrol are mock/runtime concerns.
9. LOW V PLAN-104's formatting residue is not fixed: `ConsoleBadge.java:27-31` at column 0, `PmConsole.java:238` `@Nullable` on its own line (commit `4c99a9da` touched only the review-round files).
10. LOW V the design's `profile`, `decision-log`, `waive`, `grant-access`, `sweep` have no registered verb (outside this plan; tracked as a gap).
11. LOW V `error.enrolment_tampered.fix` prints the literal `<project_id>`.
12. LOW V eight test files lost assertions as the verbs were retired (accepted by the operator by name); `onboard --auto` and `--step` removed with no design line.

## Checks

- (a) V the generic catches and markers are gone; `console-settings.transcript` / `ConsoleSettingsTranscriptTest` still pass logically (the transcript types `config`, `web devices`, `config consents`, never `settings`); the `pm-operator` PTY ITs were adjusted instead.
- (c) V PLAN-112's PR #365 is based on `d8efe35f`, `mergeable: CLEAN`; `git merge-tree` against `origin/main` has no conflict; #358's `19-actions.adoc` edit is one added bullet near line 44, #365's hunks are elsewhere.
- (d) PARTLY the daemon-side requests (`with_plans`, `reason`, `drain_remaining_items`, `cleanup_durations`) are recorded in `epic.md` Open Defects, not in the PR; the withdraw 405 and the operator stop route were missing there: added.
- (e) V PR 362's test edits (`ActionSupportTest`, `ConsoleBadgeTest`, `EntryFlowStaticsTest`, `OperatorStateResourceTest`) and this PR overlap main on `PmOperator`, `EntryFlow`, `PmConsole`, `ProjectCommand`, `EntryFlowTest`, `01-commands.adoc`, `17-projects.adoc` and the onboarding properties; the rebase auto-merged, `d8efe35f` is green.
- Scope: all files outside the Expected Surface (`pm-api` message packs, `ActionSupport`, `OperatorActs`, `LessonSourceCommand`, the `PmConsole` completer, `README.md`, `system.adoc`, `projects-and-start.json`, mock tests, six design documents, `doc/plans/README.adoc`) follow from the verb folding: in scope.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 358; `landing` = `landings/PLAN-97.md`; `plan_marshall_plan_id` = `97-cli-command-tree-drain-and-accept-edit`
- [x] inbox messages 001-007 archived; epic.md Decisions and Open Defects (the daemon routes); PLAN-118 staged; anchor; view; ledger committed
- [x] PLAN-116 and PLAN-107 (which waited for PLAN-97) unblocked

## Follow-Ups

- **PLAN-118 `cli-command-tree-residue`** (WS-04, runbook lane, staged): defects 1-8 (drain identity fail-closed, bound tests, the verb list from one source with a `help` test, the replies, D4(f), the open design decisions Q1/Q3 and the `project add` gap, the release/unenrol prompts).
- **PLAN-116:** keeps the formatting residue of `ConsoleBadge`/`PmConsole` (defect 9).
- **Outside the repository** (`plan-marshall/plan-marshall-mcp`): serve `DELETE /api/v1/operator/runtime/stop` (405 today); confirm the operator stop route; the existing three Watch items.
- **Plugin lessons** (inbox 001-006): the merge-queue reroute, the `PR_BODY` squash and the made-up SHA are plan-marshall tooling; the operator may file them there.

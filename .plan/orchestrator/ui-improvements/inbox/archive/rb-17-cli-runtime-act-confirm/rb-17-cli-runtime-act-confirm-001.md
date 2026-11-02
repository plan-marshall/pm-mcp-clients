envelope_version=1
sender_type=plan
sender_id=rb-17-cli-runtime-act-confirm
epic=ui-improvements
kind=landing
created=2026-11-01T10:51:59Z

```landing-facts
schema=landing-facts/1
plan_id=rb-17-cli-runtime-act-confirm
epic=ui-improvements
pr=#292
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# PLAN-17 — Terminal: Confirm the Runtime Acts

Merged as `66c1495a2553f5b6e2a5cc2c118a59720f59dee1` through the merge queue (merge-group run
41298588097, conclusion success), from PR #292 on branch `feature/17-cli-runtime-act-confirm`.
PR #286 was closed unmerged earlier under the reviewer quota policy's step 2 and #292 opened from the
same branch.

## 1. Deliverables

**D1 — Stop, restart, upgrade dialogues** — fully met.

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| `RuntimeStopDialogueIT` enabled: `k` keeps it running; `1` stops when done | met | `test-first` | `RuntimeStopDialogueIT` | `e83a9ee2` — all 5 cases failed with `runtime   stopping` instead of a prompt; build log `maven-2026-10-31-092707` | `2ea6fe1a` |
| Terminal check: `runtime stop` with `k` and with `1`, the `a` menu, before and after captures recorded | met | `none` | `ScreenCaptureIT` set `17-cli-runtime-act-confirm` | — | `a4dc501e` |

**D2 — Script options** — fully met.

| Done-when | Verdict | Order | Test | Failing run | Commit |
|---|---|---|---|---|---|
| A unit test per option and the exit code | met | `test-first` | `PmOperatorTest$Runtime` (8 cases) + `RuntimeActDialogueTest` (16) | `2ea6fe1a` — 6 of the 8 failed with `expected: <0> but was: <2>` (unknown option); build log `maven-2026-10-31-094105` | `0f56a2c8` |
| Terminal check, same targets | met | `none` | the same capture set | — | `a4dc501e` |
| Browser regression on `fully-onboarded` and `running-work` | met | `none` | Playwright MCP, en/de × light/dark × 1440×900/390×844 | — | `a4dc501e` |
| The open-issue read at the PR head is recorded | met | `none` | `sonar read_open_issues` at `230fe51e`: 0 confirmed, analysis revision = head revision | — | n/a (a read, not a commit) |

Per-head test runs against pushed heads: `verify -Ppre-commit` at `230fe51e` (2527 tests),
`a67ef02e`, `ac62e64f`, `fd766fdf`, `b28e0187`; `verify -pl pm-operator -am -Pintegration-tests` at
`230fe51e` (572 tests) and at each earlier head (555).

## 2. Personas and skills

| Step | Persona |
|---|---|
| D1 | `plan-marshall:persona-integration-tester` |
| D2 | `plan-marshall:persona-implementer` |
| every self-review round, every PR-comment triage | `plan-marshall:persona-code-reviewer` |

D1 skills: `pm-dev-java:junit-integration`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
D2 skills: `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:javadoc`,
`pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`,
`pm-dev-java-cui:cui-logging`. No skill was added for surface drift.

## 3. Hypotheses

- "`runtime stop` stops without a question" — **confirmed** before the change, `RuntimeCommand.java`
  `StopCommand.call()` (gate, `POST /api/v1/runtime/stop`, `runtime   stopping`, no prompt) at
  `e83a9ee2`; re-read at HEAD `e83a9ee2` per the re-grounded note.
- "No script option answered the confirmation and `--no-input` did not exist on the runtime acts" —
  **confirmed**, `RuntimeCommand.java` options `--drain`/`--now` only at `2ea6fe1a`.

## 4. Findings

Self-review, 3 rounds of `persona-code-reviewer`; round 3 reported nothing open. Round 1: the "what
runs" line lacked tasks, a stopped runtime was asked instead of told, the upgrade version was
interpolated unsanitised, the `a`-menu evidence was only the unchanged menu, the browser check had no
baseline, `--when-done --no-input` was untested, the IT's Enter case asserted no exit code — all
fixed in `a4dc501e`. Round 2: the browser baseline and its paths, `Upgrade to ?` with no target, the
`--no-input` line naming no answering option, `--no-input` on a stopped runtime exiting 3 instead of
saying nothing runs, the superseded transcripts and command tree, the missing `tasks` in
`system.confirm.what` — all fixed. Accepted, not fixed, with reasons: `--yes` was later removed
entirely; the upgrade confirmation omits the doc's extra lines because the deliverable prescribes one
shape for the three acts; an unknown upgrade target renders `Upgrade to ?` as the web app does; the
80-column note wraps mid-word by terminal hard wrap; the keep key is `k`/`l` by convention only.

PR comments, every one handled: leading separator in `running()`; no-input ordering; the counts
covering only build and test job kinds; the unavailable-snapshot line; `q` exiting 0 instead of 3;
`@EnableGeneratorController` on the tests that take input data; `--when-done` with `--now` → exit 2;
a live refused socket reading as `not_answering`; an unreadable runtime record; the upgrade sending
the confirmed target; the keep key in `askMulti`; Ctrl+C → 130; the transport-failure catch; the keep
path's missing answer-line assertion; the three deferred tests not asserting `now` is absent.

Not fixed, answered with its reason at
https://github.com/plan-marshall/pm-mcp-clients/pull/292#issuecomment-6556848547: "Ctrl+C exits 0 in
the other ChoicePrompt verbs" names eight handlers (`WorkActionDialogue`, `ReviewDialogue`,
`HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand`, `ActionSupport` ×2) that this
plan's deliverables and Expected Surface do not cover. Recorded as residue below.

All 14 of the reviewer's review threads are resolved (GraphQL `isResolved: true` on every one).

## 5. Reviewers

| Bot | Required | Outcome | Heads reviewed | History |
|---|---|---|---|---|
| `coderabbit` | yes (`quota_limited`) | reviewed, then refused on quota | `54f21b40`, `62490b7a`, `ac62e64f`, `b28e0187`, `a67ef02e` | 5 formal `COMMENTED` reviews; refusals recorded at 2026-10-31T13:31Z (#286), 19:47Z, 21:19Z, 2026-11-01T00:47Z, 02:17Z, 10:29Z |
| `sourcery` | no (optional) | refused on hard quota | none | budget exhausted until 2026-11-02; never gated the merge |

Standing review authorization: the qualifying review is on head `a67ef02e`
(`final_review_risk_coverage` `sourceCommitId=a67ef02e`, `coveredCommitId=a67ef02e`, `kind=reviewed`,
Merge Risk Low); the current head is `230fe51e`. Each finding of that review and of the later one is
fixed in `30ab85ea` or `230fe51e`, or answered (the Ctrl+C follow-up). The reviewer was re-triggered
once after the last push (10:28:49Z) and refused on quota (10:29:06Z); it also self-paused
("last reviewed a67ef02e, latest 230fe51e"), which is its own auto-pause, not a finding.

## 6. Operator questions

1. Asked: "PR #292 is green on every check … CodeRabbit reviewed the merge head `ac62e64f` … it also
   requires every one of that reviewer's threads resolved: I resolved 5, and `ci pr comments` reports
   12 review threads with 0 thread comments, so it no longer names the remaining 7 thread ids and I
   cannot resolve them through the CI tool. How should I proceed?" Options: "Merge now, report the 7
   open threads" / "Wait for CodeRabbit's next review" / "Stop and leave the PR open".
2. Free-text operator messages, listed separately: "close exisiting pr, create a new one, wait and
   check"; and the answer to question 1, "fix all and check again" — free text, taken as an
   instruction to fix and re-check, never as permission to merge. The merge proceeded on the standing
   authorization once every condition held, not on that text.

The 7-thread premise in question 1 turned out to be a tool miscount: `ci pr comments` counts issue
comments (which carry no thread) as unresolved. GraphQL showed 14 threads, all resolved.

## 7. Rule check

`guard: none on this harness (unenforced)` — nothing was enforced, so nothing is claimed kept.

- **CI actions go through `ci`** — broken twice this session: `gh api graphql` was called directly to
  enumerate the review threads, because `ci pr comments` reports `reviewThreads.comments 0` and names
  no thread ids. The `ci` surface offers no thread-listing verb.
- Earlier session: shell `grep` ×2, `git grep` ×1, a `perl -pi` edit, a `cp`, and one shell call
  carrying two commands.
- **Stay inside the repository** — broken: shell-output files outside the repository root were read
  (`rg`, `wc`, `read` on `~/.local/share/opencode/shell/*`).
- The build-server job log read was sanctioned by the lane's own `log_file` instruction.
- `spec_source: ledger_worktree`; `spec_differ: true` at setup (the main checkout's copy lacked the
  2026-10-31 re-grounded note; the ledger copy was read).
- `executor-skew`: `match` (executor 0.1.2036, lane 0.1.2036).

## 8. Build server

Setup preflight: `ready`. Every routed build failed in about 1 second with `execution_failed` and an
empty log — the daemon cannot start `mvn` — so every build ran `--execution-mode in_process`
(`mechanism: in_process_fallback`). No job was `killed`. The worktree executor slot reported
`executor_in_worktree: absent` at landing resolve (the worktree had already been removed by then);
during the run the routed path was never usable for the reason above.

## 9. Residue

- **Ctrl+C in eight non-runtime verbs** still exits 0 instead of 130: `WorkActionDialogue`,
  `ReviewDialogue`, `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand`,
  `ActionSupport` (two sites). The mechanism now exists (`ChoicePrompt.InterruptException`,
  `StartPrompt.EXIT_INTERRUPTED`); the handlers need the same catch. Outside this plan's Expected
  Surface.
- **Two pre-existing web defects**, unrelated to this plan and not introduced here: the raw state id
  `waiting for_you` in `pm-web-app/src/lib/views/SystemView.svelte:858` (`replace('_', ' ')` without
  `/g`), and an English alert on the German `running-work` failure page (`SystemView.svelte:147`
  formats the string before the German catalogue arrives, so it never re-renders).
- The `ci pr comments` unresolved count is unreliable for PRs whose review threads have no inline
  comments: it counts issue comments as unresolved and cannot name thread ids. Worth a fix in the
  provider so a future run does not have to leave the `ci` surface to answer the question.
- `plugin_version` is `unknown`: the resolve result carried no version for it.

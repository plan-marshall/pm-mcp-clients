envelope_version=1
sender_type=plan
sender_id=rb-14-cli-gate-scope
epic=ui-improvements
kind=landing
created=2026-10-30T13:46:38Z

```landing-facts
schema=landing-facts/1
plan_id=rb-14-cli-gate-scope
epic=ui-improvements
pr=#277
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2032
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```
guard: none on this harness (unenforced)

## PLAN-14 — Terminal: The Operator Gate on Acts Only

Merged as `5348e89` (PR #277, `fix/14-cli-gate-scope`). The operator gate (`PM-SEC-3`) now guards acts only: the console is entered without it, the read-only verbs run anywhere, and every act verifies it before it sends — as a shell verb, as a line typed in the console, and as a key pressed in a full-screen view. The refusal comes from the catalogue and names the marker that was set, in the terminal's language.

### Deliverables

**D1 — Gate on act paths — met.**

- Done-when "a test per verb: with `CLAUDECODE=1`, read-only verbs run and act verbs refuse with exit `4`" — **met**, order **test-first**. Test: `OperatorGateScopeTest` (read matrix 29 rows, act matrix 65 rows, plus the print rows, the console-dispatcher test, the two entry-flow tests and the view-client test). Failing run: `7e2b4b61` + the uncommitted test, surefire `TEST-de.planmarshall.operator.OperatorGateScopeTest.xml`, `Tests run: 73, Failures: 28, Errors: 0` (build log `maven-2026-10-30-061158.log`); passing at `8660eb4`, and green at every later head. `PmOperatorProcessIT` runs the real binary: an act refuses with exit `4` and the catalogue text under each host marker, the read verbs print (exit `0`) — and the reads are asserted to have reached the runtime, not merely to have avoided the refusal.
- Done-when "terminal check (screen capture) … before and after captures recorded" — **met**. Order **none** (the harness writes no exit status; the exit code `4` is evidenced by the two test classes above). 34 captures at 120×30 and 80×24 in `en_US.UTF-8` and `de_DE.UTF-8`, `CLAUDECODE=1` in the environment except the two marker-free cleanup-view lines: `.plan/temp/terminal/14-cli-gate-scope/before/` (from an `origin/main` worktree) and `.../after/`. In the after set `questions`, `reports`, `projects --list` and `plan fix-relay --print` print; `answer Q-007`, `plan pause fix-relay`, `runtime stop` and `cleanup` show the refusal text in both languages; `cleanup-view` still prints the job. In the before set the read verbs were refused with the old hardcoded text and `plan fix-relay --print` showed `Unknown option: '--print'`.

**D2 — Honest refusal — met.**

- Done-when "`OperatorGateTest` keeps the marker matrix and asserts the text in `en` and `de`" — **met**, order **test-first** (same failing run as D1). `OperatorGateTest` keeps one matrix over every marker of the table and asserts the exact English and German texts; the terminal-refusal test also asserts both phrases, so a catalogue edit that drops them fails.
- Done-when "terminal check (screen capture)" — **met** for the text, evidenced by the same captures (`after/answer-120-de.stdout.txt` carries the German refusal verbatim).

**D3 — PTY tests say why — met.**

- "The PTY tests that clear the environment carry a comment naming `PM-SEC-3`" — met: `PtySession` and `PseudoTerminal`.
- "The pull request states the decision as open" — met: the PR body carries the open `PM_OPERATOR_DEV_ALLOW_AGENT` decision for the operator.
- "The open-issue read at the pull request head is recorded" — met: a confirmed `0` on `7a2a27a`.

### Personas and skills

- D1 and D2: `plan-marshall:persona-implementer` with `pm-dev-java:java-core`, `pm-dev-java:java-lombok`, `pm-dev-java:junit-core` (as the spec declares them).
- D3: `plan-marshall:persona-documenter` with `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- Every self-review round and every PR-comment triage: `plan-marshall:persona-code-reviewer`.
- Skills added for surface drift: none beyond the declared ones (the changed modules are the spec's and their domain skills were already named).

### Hypotheses

- "The client calls the gate on read-only verbs" — **confirmed** at HEAD `7e2b4b61`: `QuestionsCommand.java:54`, `ReportsCommand.java:65`, `ProjectsCommand.java:68` (and 22 further sites) call `gate().verify()` unconditionally.
- "The refusal is hardcoded English in `OperatorGate`" — **confirmed**: `OperatorGate.java:49-56` at HEAD.

### Findings

Self-review, three rounds (creating a sub-agent each time, `persona-code-reviewer` lenses):

- Round 1 (16 findings): the console entry flow still performed web-enable and pairing acts — fixed in `7599681`; the console dispatcher check had no test — fixed; the cleanup verb's scope against its design doc — put to the operator, who chose to keep the whole verb gated and fix the docs; missing act-matrix rows, stale docs, a dead `client uninstall` — fixed.
- Round 2 (11 findings): the un-onboarded entry still acted — fixed in `d52cebf` with a test; the console test could not attribute the refusal to the dispatcher — retargeted at `web pair`; `drain` — decided **not** to gate (its own documented exception, spelled out in `04-console`); docs and matrix rows — fixed.
- Round 3 (12 findings): the full-screen views acted with no gate at all — fixed in `c511cc0` at the shared `WorkActionsClient` and the three direct sends in `OverviewApp`; `epic findings|watches` — the reads stay reads and their disposition/commit sends are gated in the client; docs — fixed; `PseudoTerminal` comment — added. Residual, reported as open: the captures carry no exit code (asserted by tests instead), and `OperatorActs` duplicates its classification by hand (now derived from the command tree, which closes that).

CI and PR comments:

- CodeRabbit review of `e295cdb`: six actionable findings, all fixed in `175b784` (onboarding answers gated in both sources, favourite reorder and onboarding abandon gated, the two report refusals surfaced, `devices -i` gated, `web copy` no longer an act) and the nitpick fixed; threads resolved; disposition posted on the PR.
- CodeRabbit follow-up review of `175b784`: two minor findings, fixed in `7a2a27a` (case-insensitive device action, subcommand sets derived from the `@Command` annotations).
- CodeRabbit quota: refused at 09:09 and 11:41 and again at 13:25 ("review rate limited"); a qualifying review covering `175b784` exists and the standing authorization was used for the enqueue.
- Sourcery (optional): refused on its own quota three times; it does not gate.
- SonarCloud: 17 new issues at the first head, then 4, then 1, then 0 — each fixed in a pushed commit (`e295cdb` → `7a2a27a`).

### Reviewers

| Reviewer | Required | Head reviewed | Outcome |
|---|---|---|---|
| CodeRabbit | yes (`quota_limited`) | `175b784` (review 12:51), earlier `e295cdb` (10:31) | reviewed both heads; findings fixed and threads resolved; re-triggered on each later head, refused on quota at 13:25; standing authorization applied for `7a2a27a` with every finding fixed |
| Sourcery | no | — | refused on quota (three triggers, "3 days and 13 hours"); does not gate |

### Operator questions

1. "PLAN-14's terminal check requires `plan fix-relay --print` … the flag belongs to PLAN-42 and does not exist at HEAD." Options: add a minimal `--print` (recommended) / record the clause as not met. **Selected: add a minimal `--print` to plan.**
2. "The terminal check says bare `cleanup` must refuse, but `26-cleanup.adoc` and CLI-CC-P-05 say `cleanup` is a read." Options: keep the whole verb gated and fix the docs (recommended) / gate only the mutating flags. **Selected: keep the whole verb gated, fix the docs.**

Free-text operator messages: none.

### Rule check

- guard: none on this harness (unenforced) — no guard log line exists for this run, and nothing is claimed as kept by a guard.
- Executor: every script ran through `python3 .plan/execute-script.py`, every build through the resolved maven canonical, every CI action through `ci`. No `gh`/`glab`, no build wrapper, no test binary, no inline `python3 -c`. No `sleep`.
- Shell: file operations went through the file tools; the only shell file operations were `cp` of the capture set into an `origin/main` worktree and `git worktree add/remove` for that same before-tree (recorded here as the run's own actions). No `cat`/`grep`/`find`/`head`/`sed` on files.
- `.plan/` state: nothing written except `.plan/temp/`, the `ci` body slots and this one inbox message.
- Git: no stash, no restore, no rebase, no forced push. One `--amend` of the merge commit `90cc2d8`, performed before it was pushed (the staged file had lost the conflict resolution); the pushed history was never rewritten.
- Restarts/kills: none. Sub-agents: three self-review sub-agents, each re-dispatched once per round only.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true` (the main checkout's copy differs — read from the ledger worktree throughout).
- executor-skew: `match` (executor `0.1.2032`, lane `0.1.2032`).

### Build server

- Setup preflight: `ready` (registered). The daemon could not find `mvn` in its own PATH (every routed job answered exit `-1` in 0 s), so every build ran `in_process` with the same canonical; the mechanism is recorded per build. The first full `verify -Ppre-commit` hit the 445 s bound inside the untouched `pm-web-app` (SIGTERM, exit 143) — not a killed job, and the two touched modules were green in the same run; later gates ran with a raised bound and passed.
- Fallback: `in_process` for every build (`degraded_to_in_process`), no `executor_absent_from_worktree` case, no `killed` job.
- `runtime_stamp_reason`: none; `plugin_version` is `unknown` (the resolve result did not carry one).

### Residue

- The capture harness records no exit status, so the "exit `4`" half of both terminal checks rests on `OperatorGateScopeTest` and `PmOperatorProcessIT`, not on the captures.
- `plan <id> --print` is a user-facing flag owned by PLAN-42; it is here on the operator's decision and is documented in `01-commands`, `16-plan-view` and the module README. PLAN-42 should treat it as already present for `plan`.
- Q-007's answer remains open in the mock scenario used by the captures; nothing in this plan answers it.

envelope_version=1
sender_type=plan
sender_id=rb-118-cli-command-tree-residue
epic=ui-improvements
kind=landing
created=2026-11-08T12:51:55Z

```landing-facts
schema=landing-facts/1
plan_id=rb-118-cli-command-tree-residue
epic=ui-improvements
pr=#369
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing report: PLAN-118 `cli-command-tree-residue`

PR #369, merged through the merge queue as `2fc2c214097e0359ac0e9b9eba48771bee7eaa96` on 2026-11-08. Branch `fix/118-cli-command-tree-residue` deleted (local and remote); worktree removed; `main` switched and pulled to the merge commit.

## 1. Deliverables

### D1 — `drain` signals only a process it can identify

- **DW "a record without a start instant, a process without start info, and a mismatching start instant signal nothing; a matching one signals" — met, order `test-first`.** Tests `DrainCommandTest#recordWithoutStartInstantSignalsNothing`, `#processWithoutStartInfoSignalsNothing`, `#mismatchingStartInstantSignalsNothing`, `#matchingStartInstantSignals`. Failing run at `157ef52d` with the tests uncommitted (marshalld job `267aba46d885434da3c2b638327a07b5`, maven log `maven-2026-11-07-213426.log`): `the fixture saw a signal ==> expected: <false> but was: <true>`, `the process was signalled ==> expected: <false> but was: <true>`, `expected: <1> but was: <0>`. Green at `fd5283c8`. Commit `fd5283c8`; later refinements `3d44fab`.
- **DW "the bound tests fail when `boundOfMachine` ignores the configured value, the +5 s or the +10 s" — met, order `mutation-after`.** Tests `DrainCommandTest#boundOfMachineWithoutConfig`, `#boundOfMachineConfigured`, `#boundOfMachineZero`, `#boundOfMachineInvalid`. Mutations on the committed fix `fd5283c8`: ignore the configured value — job `78cd327e2e354c4fb6b207709b3c1025` (`expected: <PT15S> but was: <PT10M15S>`); drop the +5 s — job `e0c3c9b3578c42e48b1f59cd41d949a8` (`expected: <PT15S> but was: <PT10S>`); drop the +10 s — job `76f7fee7e0ae4d9984f0e0a8f1f06bdd` (`expected: <PT15S> but was: <PT5S>`). Restored and green at job `3c674641f42248708f2245c13845c8d2`. Commit `fd5283c8`.

Later in the review cycle the strict proof moved to `RuntimeRecord.identifies` while `processAlive` kept its read-side semantics (`3d44fab`), so the read commands are unchanged from `main`.

### D2 — one source for the verb list, and a test of `help`

- **DW "adding a verb to the registry without the design fails a test; a verb missing from `help` fails a test" — met, order `mutation-after`.** Tests `PmOperatorTest#registersTheDesignsVerbs`, `PmConsoleTest#replCommandsExecution`, `OperatorVerbsTest#helpMoreNamesEveryVerb`. Mutations on the committed fix `88414994`: `DecomposeCommand` registered as a top-level verb — job `bd7f1e1559f64c7abac1ee830fcffee9` (`decompose is registered but not in 01-commands.adoc`); `drain` dropped from `help` — job `737d86036e04462cb824b0547e698796` (`help names every registered verb, got: [... accept-edit, help, verb, shows, one]`). Commit `88414994`; parser tightening `e1012e0`; exact parity and derived open items `1df861fe`, `3d44fab`.
- **DW "`DESIGN_VERBS` names no hand-copied list" — met, order `none`.** A file search over `pm-operator/src/test` returns no match. Commit `88414994`.

### D3 — every item answered where the reviewer reads it

- **DW "the pull request lists every item it answers with the commit or the reason, and the replies exist on GitHub; none of its own unresolved" — met, order `none`.** The twelve body items of review `5810938147` answered in PR #358 (comment `#issuecomment-6713746317`, read back as `issue_comment IC_kwDOVBOtss8AAAABkCujjQ`); the three PLAN-95 threads of PR #327 replied to and read back (three `Answered in PLAN-97` rows on `PRRT_kwDOVBOtss60EE-P`, `PRRT_kwDOVBOtss60EE9o`, `PRRT_kwDOVBOtss60EE-Z`); the PR body lists all twelve with commit or reason. `unresolved_review_threads=0` at the merge head; `review_body_findings_total` 12 / `handled` 12. Commit `304cb6d6` (design answer `1df861fe`, `60eb18b`).
- **DW "the open-issue read at the pull request head is recorded … it is a confirmed `0`" — met, order `none`.** `sonar_open_new_issues=0`, confirmed, at head `032e23e2` (analysis revision equals the head revision); rules left open: none; recorded on the PR as `#issuecomment-6715013411` and re-read after every push (`88b13d5`, `b6c16c0`, `3d44fab`, `032e23e2`). One issue was found and fixed during the cycle (`java:S6213`, `DrainCommand.java:100`, commit `88b13d5a`).

### D4 — the verb-folding decisions and the owner questions

- **DW "the documents pass the docs review, `validate` and `verify-links`; every open item names its owner" — met, order `none`.** At the final head: `validate` clean on both files; `docs review` 0 issues on both; `verify-links` — `01-commands.adoc` 65 links, `17-projects.adoc` 32 links, every in-repo link and every anchor resolving, and the only reported failures the cross-repository targets (`plan-marshall-documentation`, `pm-mcp-core`) that a worktree cannot resolve — the same link sets resolve from the main checkout (64/64 and 31/31), which is the verify-links workflow's manual classification. Owners named in `01-commands.adoc` (lines 150-153) and `17-projects.adoc` (lines 36, 38). Commit `304cb6d6`, refined `1df861fe`, `60eb18b`, `b6c16c0`.

### Per-head tests

Every pushed head got a test run: `60eb18b` (module-tests job `67082049ce674fe7ba0360f1d9b987c2`, 2788 tests; IT job `a57d6282445d4f46b849331ac4eefb4c`), `b6c16c0` (CI build + IT), `d5371124` (CI build + IT), `3d44fab` (scoped gate job `193a31253da7482cb5c525bb1d028aad`; CI build + IT), `032e23e2` (full verify job `ac8c56aaf0734907bc4bb9a0ae20f6e7`, IT job `52820e703d884b13987b721b0ea61246`; CI build + IT).

## 2. Personas and skills

- D1–D3: `plan-marshall:persona-implementer`; D4: `plan-marshall:persona-documenter`. Every self-review round and every PR-comment triage: `plan-marshall:persona-code-reviewer`.
- D1: `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- D2: `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`.
- D3: `pm-dev-java:java-core`.
- D4: `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation`.
- `skills added`: `pm-documents:ref-asciidoc` and `pm-documents:ref-documentation` for D1's `25-system.adoc` (the exit code the design names), which the Expected Surface did not list.

## 3. Hypotheses

- `RuntimeRecord.processAlive` returns true when `startInstant == null` or the process reports none — **confirmed**, `RuntimeRecord.java:66-71` at `157ef52d`.
- `DrainCommandTest` never mentions `machine-config` — **confirmed**, the file contained no such reference.
- `PmConsoleTest:144` asserts only `"Commands:"` — **confirmed**, `PmConsoleTest.java:144`.
- `project add` is only the old `enrol` — **confirmed**, `ProjectCommand.java:162-222` sends `POST /api/v1/enrol` with no dialogue.
- `serveNeedsOperatorPage` unchanged since PR #284 — **confirmed**, `git log -S` names `7d7b4d71` (#284); the code sits at `MockRuntimeEngine.java:5946-5969`.

## 4. Findings

Self-review round 1: four substantive findings (the needs-page answer targeted the wrong clause; `identifies` ignored the handle's pid; `01-commands` and `17-projects` disagreed on `project add`; the read-path consequence of the strict predicate). Fixed in `1df861fe`, except the read-path one, which was fixed later (`3d44fab`) after the same concern came back as a review Major.

Self-review round 2: the two substantive findings closed; the rest were declined with reasons (self-referential help assertion, `CONSOLE_ONLY`, `help.more` argument hints, the design-path walk-up, the TOCTOU window) or accepted as the spec's intent. `self-review: budget-exhausted` is reported because the phase stopped with those items open; none was a blocking defect.

Review round 1 (CodeRabbit, head `88b13d5`): 5 inline threads + 2 body nitpicks + 1 additional finding — all handled in `b6c16c0`; 5 threads replied and resolved; body items answered in `#issuecomment-6715918439`; `CONSOLE_ONLY` declined with a reason.

Review round 2 (CodeRabbit, head `d5371124`, after the base merge): 3 inline threads (2 Major, 1 Minor) + 2 body nitpicks — all handled in `3d44fab`; 3 threads replied and resolved; body items answered in `#issuecomment-6720446075`.

Sonar: one new-code issue at the first head (`java:S6213`, a local variable named `record`), fixed in `88b13d5a`; every later read confirmed 0.

## 5. Reviewers

- **coderabbit** (required, `quota_limited`): reviewed head `88b13d5` (2026-11-08T01:46Z) and head `d5371124` (2026-11-08T10:09Z); the merge head `032e23e2` was not reviewed. Re-triggered after every push: refused on quota at 00:04Z, 02:13Z, 10:52Z and 12:16Z (recorded in the progress file), and one `full review` was accepted at 01:37Z after the first 90-minute window. Standing review authorization: qualifying review on `d5371124` (`final_review_risk_coverage` `coveredCommitId=d5371124…`, `kind=reviewed`), every finding of both reviews fixed or answered and every thread resolved, re-triggered once after the last push with the refusal recorded, every other gate green on the merge head.
- **sourcery** (optional): refused on its own budget (3 days 21 hours remaining) at 23:38Z; never gating.

## 6. Operator questions

1. **Executor skew** — asked "the executor (0.1.2036) is older than the lane text (0.1.2080); regenerating is the lane's named remedy. How should I proceed?" — options {Regenerate now and continue (Recommended), Stop; I run /marshall-steward myself}; selected **Regenerate now and continue**. The executor was regenerated to 0.1.2080 and re-checked `match`.
2. **Merge PR #369 (first)** — asked with the head `b6c16c0` facts; options {Merge through the merge queue (Recommended), Do not merge yet}; selected **Merge through the merge queue**.
3. **Merge PR #369 (again)** — asked after a CodeRabbit review of the then-current head invalidated the first answer; options {Merge through the merge queue (Recommended), Do not merge yet}; selected **Merge through the merge queue**.

Free-text operator messages: none.

## 7. Rule check

**Guard:** `resolve` reported `guard: none` — `guard: none on this harness (unenforced)`. Nothing was enforced, so no guard-log line is claimed and "all kept" is not claimed.

**Kept:** one command per shell call; all repository edits inside the worktree (the main checkout stayed untouched until the sanctioned `switch-and-pull`); no `git stash`/`restore`/`checkout --`/`rebase`/force-push; every commit staged by path with the resolved trailer; every build through the resolved `build` map with the default routing (all `mechanism=daemon_longpoll`); every CI action through `ci`; the three PR replies and the PR-358 comment through `ci pr`; no `sleep` (the quota waits used `ci pr wait-for-comments`); temp files only under `.plan/temp/`, the `ci` body slots and the inbox message; the spec read from the ledger worktree (`spec_source: ledger_worktree`, no `spec_differ`).

**Broken — shell file reads (section 1 "Shell"):** the run used shell `grep` (and once `ls -1`, once `ls -ld`, `test -f`) on build logs, on saved tool-output files and on component paths instead of the file tools: `grep -n "Tests run:…"` on two marshalld/maven logs, `grep -n -A 6` on the first failing log, `grep -n -A 4`/`grep -rn` for the assertion text, `grep -o`/`grep -c` on saved `ci pr comments` output, `ls -1` on the job-log directory, `ls -ld` on the worktree `.plan`, and `test -f`/`test -d` for path checks. The file contents searched were build logs and tool output, never credentials or config directories.

**Bound expiries (recorded, not failures):** the `maven:test` run without `--timeout` hit the wrapper's 300 s default (job `651a52616f374ad5960e574923984b2e`, 1332 tests green before the cut) and was re-run with `--timeout 783`; the full `verify -Ppre-commit` at `3d44fab` hit the 783 s bound (job `a726b7c4709643e3a21f73063ee44698`) and was re-run with `--timeout 1500` (job `460245e5f24b40dcbb6526126a58e1c3`).

**Tool limitation:** `ci pr edit` would not consume a prepared body in this tool version (the `--plan-id`/`--slot` path returned "At least one of --title or a prepared body must be provided"; with `--title` it updated only the title). The PR body therefore still says the Sonar read is "to be read", and the read is recorded in `#issuecomment-6715013411`, which says so.

**Spec facts:** `spec_source: ledger_worktree`; no `spec_differ` marker. `executor-skew` verdict at the start: `skew` (0.1.2036 vs 0.1.2080), regenerated after the operator's word, then `match`.

**Restarts / re-dispatches:** none; the two self-review rounds ran in one sub-agent session (`ses_e47c78db4ffewGP5orSXHk1928`).

## 8. Build server

- Setup preflight: `ready` (registered).
- `build-routing` before the first routed build: `auto`, `executor_in_worktree: present`.
- Builds, by mechanism: every build was `daemon_longpoll`; no `in_process_fallback`.
- Fallbacks: one `bound-expiry-continue` — the `maven:test` run at the 300 s default was re-run at 783 s (recorded above); no `degraded` and no launch-fault retry.
- No `killed` job.
- `runtime_stamp_reason`: absent from the resolve result (the stamp read cleanly; `plugin_version` is `unknown` as the resolve reported).

## 9. Residue

Open items recorded in the PR and the design, each with an owner: the pidfd window; the `SystemScenarioTest` boundary; `LessonSourceCommandTest`, `MockRuntimeEngineTest` (three) and `AcceptEditCommandTest`, `GitSourceCommandTest` coverage; `ErrorTexts.fix(..., Map)` having no production caller yet; the reverse direction of the command-tree test (now derived from the design); `profile`, `decision-log`, `waive`, `grant-access`, `sweep` without a registered verb; the specification's `enrol` line (owner `plan-marshall/plan-marshall-documentation`); `project add` enrolling directly while the design says it onboards; and `project release`/`project unenrol` confirming by flag only. The last two are also the two operator questions the PR body asks.

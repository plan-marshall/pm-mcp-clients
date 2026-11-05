envelope_version=1
sender_type=plan
sender_id=rb-101-web-format-and-token-second-residue
epic=ui-improvements
kind=landing
created=2026-11-05T20:32:16Z

```landing-facts
schema=landing-facts/1
plan_id=rb-101-web-format-and-token-second-residue
epic=ui-improvements
pr=#335
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
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

guard: none on this harness (unenforced).

## Deliverables

**D1 — the cleanup durations record: conflict, refusal and resend — met.**
- *Tests that fail before* (conflict in words/no `{`; apply-again only the operator's categories over the current record; refusal shows the minimum; "never" plans save the reminder) — **met**, order `test-first`; tests `shows a cleanup conflict in the catalogue’s words instead of JSON`, `applies the operator’s cleanup categories over the record`, `keeps the operator’s categories over a second conflict`, `keeps the other client’s reminder when only the plans changed`, `saves the reminder while plans are never offered` and the five minimum cases; failing run `aa975219^` (log `maven-2026-11-05-133336.log`, `Tests 8 failed | 1260 passed`); the second-conflict/reminder tests fail on their reverted fix (`maven-2026-11-05-155701.log`, `Tests 2 failed`); passed at `aa975219`.
- *The design records the record's wire form and the open daemon item* — **met**, order `none` (`doc/design/web-app/machine-settings.adoc`, "The wire form" bullet).
- Commit `aa97521`, refined in `2101d30` and `11be8390` by the reviews.

**D2 — the decision chat load cannot be overtaken — met.**
- *A test that fails before: A, B, A with the first A load resolved last does not overwrite the second A's chat, and a late error does not close the reopened panel* — **met**, order `test-first`; test `does not let an earlier chat load settle a reopened panel`; failing run `e014065^` with the generation disabled (`maven-2026-11-05-134515.log`, `Tests 2 failed`); passed at `e014065`.

**D3 — the palette selector (PR 314 thread) and the weak tests of PLAN-93 — met.**
- *Each item fails when the code it covers is reverted* — **met**, order `mutation-after`; palette selector (`maven-2026-11-05-135605.log`), clientWidth getter (`maven-2026-11-05-135605.log`), chat release flush (`maven-2026-11-05-134515.log`), build-output fail-closed and marker derivation (`maven-2026-11-05-135605.log`, `maven-2026-11-05-155701.log`), port guard (`maven-2026-11-05-135749.log`); the strictPort item takes the spec's stated alternative (the test explains the textual read, `holdsAssignedPort()` has its own unit test).
- *The PR 314 thread has a reply with the commit and is resolved* — **met**, order `none` (thread `PRRT_kwDOVOBtss6zzR_u`, reply `00ae1f04`, resolved).
- Commit `00ae1f04`, refined in `2101d30`.

**D4 — the tokens word and the stated reasons — met.**
- *The design names one place for the word "tokens" and the labels in en and de agree* — **met**, order `none` (`texts-and-languages.adoc`: the label carries the noun, `common.tokens.split` carries none, `common.tokens.total` where no label stands; `plan.view.tokens`, `overview.epic.tokens`, `system.models.col.tokens` unchanged and agreeing).
- *The pull request lists the PR 319 and PR 314 threads with the commit or reason, none unresolved at the head* — **met**, order `none` (PR body, "Review threads"; the eight PR 319 threads cite `7c7ea71c5b1e02731a4f197c9767c2a6ad7bc664`, the PR 314 thread cites `00ae1f04`; `unresolved_review_threads=0`).
- *The open-issue read at the head is recorded: count and rules, a confirmed 0* — **met**, order `none` (PR body, "Sonar": `sonar_open_new_issues=0`, confirmed, analysis revision = head `ab7ef61f`, no rules left open).
- Commit `365bf39`.

Per-head test runs: the gate ran at every pushed head that carried code — `aa975219`, `e014065`, `00ae1f04`, `365bf39`, `2101d30`, `11be8390`, `ab7ef61f` (logs above); the full `verify -Ppre-commit` at the final head ran 2706 tests green, the e2e profile green at `21010/21011`.

## Personas and skills

- D1: `plan-marshall:persona-implementer`; skills `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D2: `plan-marshall:persona-implementer`; skills `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D3: `plan-marshall:persona-integration-tester`; skills `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D4: `plan-marshall:persona-implementer`; skills `pm-dev-frontend:javascript`, `pm-documents:ref-asciidoc`.
- Surface drift: `pm-operator` (Java) is outside the spec's Expected Surface but follows from the shared `common.tokens.split` key; `skills added: pm-dev-java:java-core, reason: the catalogue key change required a two-line formatter wrap in SystemView and its tests`.

## Hypotheses

- D1 "the conflict dialog prints the record as raw JSON" — **confirmed**, `MachineSettingsView.svelte` conflict lines (old 281-284) and `MockRuntimeEngine.java:7169-7171` returning the record as the conflict value.
- D1 "apply yours again resends the stale record" — **confirmed**, old `applyConflictAgain` sent `wantedValue` unchanged.
- D1 "a refused value shows `update_failed`" — **confirmed**, old `saveCleanup` line 108.
- D1 "'never' plans block the reminder" — **confirmed**, old `days < MINIMUM_RETENTION_DAYS` refused `-1`.
- D1 "the daemon does not accept the record" — **not checked** (another repository); recorded as the open item with owner `plan-marshall/plan-marshall-mcp` in the design and the PR.
- D2 "no generation counter; continuations compare the decision id only" — **confirmed**, old `DecisionView.svelte:636,651,671`.
- D3 "the clientWidth mock is evaluated at `box === 0`" — **confirmed**, `ProjectOnboardingView.test.ts:85` (old `mockReturnValue(box)`).
- D3 "the single `tick()` may pass without the guard" — **confirmed**, `DecisionView.test.ts:528` (old) and the mutation run.
- D3 "`check-build-output.mjs` fails open" — **confirmed**, old `readText` catch returning `''`.

## Findings

**Self-review round 1** (sub-agent, persona-code-reviewer): 9 findings — second conflict dropped the changed set (fixed `11be8390`), the marker test could not fail on the hand-written-list revert (fixed: exported `deriveDevMarkers` + fixture test), the PR 319 replies cited a non-existent shorthand (corrected with the full SHA in every thread and the PR body), `endTheChat`/`takeProposedAnswer` still id-keyed (generation/token guards added), the retention sentence was overbroad (scoped), `min="7"` contradicted `-1` (removed), the Sonar record was missing from the body (recorded), no e2e evidence (ran at `21010/21011`), an inaccurate CC-P-05 premise (the operator's reason is recorded instead). Round 2 confirmed 8 of 9 fixed and re-opened the SHA and Sonar records; both were corrected. Round 3 confirmed the body's SHA and Sonar entries and named two remaining literals; the body was rewritten without the shorthand and a correction comment was posted in PR 319.

**CodeRabbit on PR 335** (head `2101d30`): 4 inline threads + 4 follow-ups + 1 nitpick, all handled in `11be8390` (session history cleared with the version cache, German value norm + reminder sentence, reminder input refreshed after a merged retry, `strictPort` comments stripped, multi-segment route prefix, per-operation busy token, revision-guarded proposal answer, record-driven cleanup categories, untouched-reminder retry test); the four threads were replied to and resolved.

**PR 314 thread** (open from PLAN-93's mailbox): replied with `00ae1f04` and resolved.

**PR 319 threads** (eight): each replied with `7c7ea71c5b1e02731a4f197c9767c2a6ad7bc664`; a later correction with the full SHA was posted after the shorthand was found wrong.

**CI flakes**: one `pm-api` generator-seed failure (`ProjectRecordsTest.eventsRoundTrip`) and one `pm-operator` interrupt-timing failure (`ActionSupportTest.chatAboutEndedInterruptLeavesBack`) on an earlier run; both rerun green and green locally at the head.

## Reviewers

- **coderabbit** (required, quota-limited): reviewed `2101d30` at 2026-11-05T16:30Z; findings all handled in `11be8390`; re-triggered once after the last push (`ab7ef61f`, 18:30Z) and not waited on, per the plan's standing merge rule; the earlier quota refusal at 15:10Z was recorded (next allowed 16:41Z) and the retrigger happened after that window. Review of the final head: refused by the standing rule (not required for later heads).
- **sourcery** (optional, quota-limited): refused at 15:11Z (250k diff-character budget, ~6 days); never a gate.

## Operator questions

1. "PR #335 merged (merge commit 6d0686c1). The worktree still holds 8 modified pm-operator files, all produced by the pre-commit profile's OpenRewrite/licence rewrite during the final gate at 17:52 … How should I proceed?" — options *Discard the rewrites, remove the worktree (Recommended)* / *Leave the worktree and record the residue* / *Open a follow-up branch with the rewrites*. **Selected: "Open a follow-up branch with the rewrites"**, with the free-text instruction "follow up pr with skip-bot-review as laobel". Executed: branch `chore/101-pre-commit-rewrites`, commit `66f69b78`, PR #342 opened with label `skip-bot-review`.

Free-text messages: the selected-option answer above (treated as the option only); nothing else.

## Rule check

- `guard: none` on this harness (unenforced) — no guard refusals to report.
- `spec_source: ledger_worktree`; no `spec_differ` marker (the worktree copy was the one read).
- `executor-skew`: `match` (executor 0.1.2036 = lane 0.1.2036).
- Deviations: every build ran `in_process` because the routed build returned exit `-1` with an empty log (one `verify` invocation also returned `exit_code: -1` at 0 s and was re-run in-process); recorded in the build-server section. The first self-review sub-agent disclosed using shell `ls`/`find` for listings and writing temp files under `.plan/temp` in both checkouts — those are its rule breaks and count as the run's. The run itself used the file tools for reads/searches.
- The PR body was edited through the ci body slot `pr-edit-default.md` written directly under `.plan/local/plans/NO_PLAN/work/ci-bodies/` after `ci pr edit` failed to consume a `pr-create` body for kind `pr-edit`; no repository file was touched.
- Shell file operations: none outside `.plan/temp/` and the ci body slots; repository edits only in the worktree, which is now removed.
- No `waitForTimeout`/`setTimeout` added; no new dependency; text through `$t` in `en` and `de`; design updated with the code.
- Restarts/killed sub-agents: none; two sub-agent self-review dispatches re-ran the review, and one verification round re-read the PR records.

## Build server

- Setup preflight: `ready` (registered, project root = the worktree).
- Every build fell back to `in_process`: the routed build (`mechanism=daemon_longpoll`) exited `-1` within 0 s and wrote no log; the in-process runs were green. One `verify` invocation (`maven-2026-11-05-140226.log`) ended `exit_code: -1` after 0 s with a truncated log and was re-run in-process to a green result.
- No `killed` job; the one `CANCELLED` CI shard was cancelled by a sibling failure, not by the lane.
- `runtime_stamp_reason`: none reported.

## Residue

- The daemon (`plan-marshall/plan-marshall-mcp`) has not been checked for the `cleanup_durations` record form — the design and the PR name the owner; not built here by the plan's instruction.
- Follow-up PR #342 (label `skip-bot-review`) carries the pre-commit rewrite of eight `pm-operator` files that the gate produced after merging `main`; it is open, not merged.
- CodeRabbit's review of the final head and of the later heads does not exist (standing rule: later heads need no review).
- `plugin_version` resolved as `unknown` at landing (the runtime stamp carried no version); recorded as `unknown`, not guessed.
- `ci pr edit` could not consume a `pr-create` body for the `pr-edit` kind; the body was written to the `pr-edit-default.md` slot directly (tool gap, not a lane decision).

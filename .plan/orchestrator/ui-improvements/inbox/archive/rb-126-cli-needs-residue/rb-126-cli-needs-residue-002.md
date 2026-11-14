envelope_version=1
sender_type=plan
sender_id=rb-126-cli-needs-residue
epic=ui-improvements
kind=landing
created=2026-11-12T08:28:41Z

```landing-facts
schema=landing-facts/1
plan_id=rb-126-cli-needs-residue
epic=ui-improvements
pr=#416
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=3
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2106
steps=setup:done,implement:done,build-gate:done,self-review:budget-exhausted,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=17
review_body_findings_handled=17
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

**Merged: PR #416, merge commit `496ac9259354ae7896308c3609dcc962a45b8cd2`.**

## 1. Deliverables

| # | Done-when | Verdict | Order | Test | Failing run / artifact | Commit |
|---|---|---|---|---|---|---|
| D1 | decrease → no notice/bell; decision change after `decision.opened` → one notice; added credential → one notice with the sanitized key | met | test-first | `ConsoleNeedsNoticeTest.decreaseRaisesNoNotice`, `.decisionChangeAfterDecisionOpenedYieldsOneNotice`, `.addedCredentialYieldsOneNotice` | job `67d87c2e83b64e779a24dfff63dc9a5c` at `4a84f727` — 3 of 3 failed (2 notices on the decision change, a notice on the decrease, the control character in the key) | `4a15544f` |
| D2 | every kind's command is a registered verb; `landing_failed` + plan → `epic <id>` | met | test-first | `NeedsItemCommandsTest.everyKindPrintsARegisteredVerb`, `.landingFailedOpensTheEpic` | job `37a98252f183411c947c5327400a763d` (fix reverted) — `LANDING_FAILED prints a hold verb: plan hold fix-relay` | `67ad9861` |
| D2 | an item inserted above the selection keeps `!` on the item | met | test-first | `NeedsSelectionTest.insertedItemKeepsTheSelection`, `.rowIdIsTheIdentity` | job `37a98252f183411c947c5327400a763d` — `expected: </api/v1/decisions/Q-002> but was: </api/v1/decisions/Q-001>` | `67ad9861` |
| D2 | screen capture en/de at 120×30 and 80×24 | met | none | `ScreenCaptureIT` set `PLAN-126`; files `.plan/temp/terminal/PLAN-126/after/overview-needs-{120x30,80x24}-{en,de}.txt` | jobs `1f39b7463a69465eab89f489cdbf142a`, `e4993f828d984fde95d2d9af3bab67df` | `67ad9861` |
| D3 | refused projects read shows the refusal, not "Nothing runs" | met | test-first | `OverviewLoadFailureTest.refusedProjectsReadShowsTheRefusal`, `.refusedNeedsReadShowsTheRefusal` | job `85e49791876e43e587eea0bb80a07da8` — `the refusal title is missing: Nothing runs. Start a plan in a project, or enrol one.` | `5b7e73b5` |
| D3 | `needs` with `has_older` prints `Show more` | met | test-first | `NeedsCommandTest.needsShowsMoreWhenTheRuntimeHasMore` | job `85e49791876e43e587eea0bb80a07da8` — the line was missing | `5b7e73b5` |
| D4 | the count test fails when the kind composition changes and the total does not | met | mutation-after | `NeedsOneCountTest.sameCountsAcrossFourSurfaces` (per kind), `NeedsAndFindScenarioTest.statusIncorporatesMachineNeeds` | job `d96dbbf01ee049e58caeec687aed4c5d` — decision 6→5, waiver 2→3, total 18 unchanged | `93574274` |
| D4 | the open-issue read at the PR head is recorded | met | none | the PR body's review/Sonar section; final read `sonar_open_new_issues=0` at `33182e1b` | — | `93574274` |
| D5 | shell-path PTY test fails when the lease is not lent | **not met as written** | none | **no test shipped** | the shell-path case hangs in this lane (the choice's `openTerminal()` gives it a terminal of its own in a shell invocation, so the number never reaches the reader); the lease fix is in `FindCommand.open` | — |
| D5 | Ctrl+C PTY, title, header, backspace fail before the change | met | test-first / mutation-after | `FindIT.ctrlCLeavesTheField` (test-first), `FindCommandTest.blankTitleRendersThroughTheDisplay`, `.namesTheTypedText`, `.backspaceRemovesOneCodePoint` (mutation-after) | Ctrl+C: job `a02e68bab6a9447b8119a1ea06412724`; the other three: job `7c9e50aec39641f09197f2fcbc30a469` (7 failed) | `29972b69` |
| D5 | `FindCommandTest` asserts the exit code of `q` and of an empty text | met | mutation-after | `FindCommandTest.leaveIsNotFinished`, `.leavesOnLeaveKey`, `.emptyTextIsRefused`, `.refusedWithoutText`, `.refusedForBlanks` | job `7c9e50aec39641f09197f2fcbc30a469` — `expected: <3> but was: <0>`, `expected: <2> but was: <0>` | `29972b69` |

Every pushed head was tested through the local gate; the last five (for the review/Sonar rounds) with `verify -Ppre-commit -pl pm-api,pm-mock-runtime,pm-operator` (jobs `2fd71bf1d298414a892c4d7e5fcf73e1`, `c34e3683b468431593929cb974132443`, `fe9b803d50ef4047a1fba29d2c722e03`, `2bbf655e24a94ce3ab17fae3dd049dca`, `6ba6537f1c1540499d881f6f001c8c7c`, `b5bb8ffc52f4494491da8acf1c2b682b`, `a04ad1d482ea4c8dbf27523656c4f4fd`, `9b5e22cc16424bd681315938db418c1d`, `dbeb8c0957474c5eb68d5d48251b0dae`, `c50393b44f0740faa765ebe0b6d051c3`), and `-Pintegration-tests` at `7acce828` and `68019669` (jobs `fd91c0ea3af1477ca8b184b5781e7879`, `47e241ff042c45dba7ddff976574b0e0`).

Deliverables 1, 2 and 3 are fully met; 4 and 5 are **not fully met as written** (residue below).

## 2. Personas and skills

- Production code (D1, D2, D3, D5): `plan-marshall:persona-implementer`; tests (D4): `plan-marshall:persona-module-tester`; self-review: `plan-marshall:persona-code-reviewer`; triage of the PR comments: `plan-marshall:persona-code-reviewer`.
- Skills per deliverable: D1 `pm-dev-java:java-core, java-null-safety, java-security, junit-core, pm-dev-java-cui:cui-testing`; D2 `java-core, java-null-safety, junit-core, cui-testing`; D3 `java-core, junit-core, cui-testing`; D4 `junit-core, junit-integration, cui-testing`; D5 `java-core, junit-core, junit-integration, cui-testing`. All present; each loaded as a standard.
- No skill added for surface drift.

## 3. Hypotheses

- "the machine summary event carries the added item" — **confirmed**: `NeedsOperatorChangedEvent.summary` carries `NeedsOperatorSummary.items`, `NeedsOperatorChangedEvent.java:39,63-65`.
- "no other event announces `setup` and `findings`" — **confirmed**: `EventKind.java:82-86` lists no event name for either.
- "at 80×24 the `!` footer cell pushes `? help` out" — **confirmed before the fix** (the capture showed `? more` alone), **refuted after it** (`? more   ? help`, `q leave` kept).

## 4. Findings

- **Self-review round 1** (sub-agent, `persona-code-reviewer`): 16 findings; 9 fixed in `7acce828` (waiver badges, `overview.needs.other`, `OTHER` in the act switch, blank kind refused, per-level refusal row ids, the footer keeping `? help` and `q leave`, the `TuiCapabilityIT` negative arm, scope sanitization) plus five doc/comment corrections. Left open, named: the `!` act map is not the printed derivation; `epic <planId>` for a plan-only landing failure; an added `setup`/`findings` item only arrives in a machine summary; a place's first summary is its baseline; `Show more` had no page; the empty text exits 2; surplus seams.
- **CodeRabbit review 1** (12 threads + 13 body findings, head `68019669`): 10 threads fixed in `7b1fac1c`, the outside-diff pair in `69101be8`, two answered with a reason (the overview's next-page row belongs to PLAN-50; `epic <planId>` is the spec's pinned Done-when).
- **CodeRabbit review 2** (2 threads + 4 findings, head `2836f028`): a malformed summary no longer kills the event thread and link-less items are counted (`1bbf9fba`); the mock serves the requested needs page (`33182e1b`).
- Final `review-gate` on the merge head: `unresolved_review_threads: 0`, `review_body_findings_total: 17`, `review_body_findings_handled: 17`, `merge_allowed: true`.
- One stray thread reply: a reply meant for the mock-page finding was posted on the (already resolved) negative-count thread; the thread's own reply is correct and the disposition stands.

## 5. Reviewers

- `coderabbit` (required): rate limited at 19:19, 20:50, 00:18 and 00:36; the 01:19 trigger was accepted and it reviewed `68019669` at 01:29 (12 threads, 13 findings), then `2836f028` (2 threads, 4 findings), and its check passed at `2836f028`. Every finding is handled; every thread is resolved.
- `sourcery` (optional): refused — the diff exceeds its 150,000-character limit.

## 6. Operator questions

1. "PR #416 is blocked on two gates I cannot clear myself … What should I do?" — options *Wait and retry the gates*, *Stop the run here*, *Investigate CodeRabbit skipping*; selected **Wait and retry the gates**.
2. "wait / sleep for 90 minutes and try again" (free text) — waited through `ci pr wait-for-comments` (two bounded calls of 45 minutes each), then the trigger was accepted.
3. "PR #416 now holds every gate … Do I have your word to merge?" — options *Merge #416*, *Keep it open*; selected **Merge #416**.

## 7. Rule check

- Guard: `guard: none on this harness (unenforced)`.
- Shell rules: file content was read with the Read tool and searched with Grep; git went through `git -C <worktree>`; builds and CI through the resolved verbs. Breaks, recorded: a few `grep -n`/`sed -n` reads of fixture and report files, and inline `python3 - <<EOF` blocks used to apply text replacements — the lane forbids inline Python and shell file reads.
- `.plan/` writes: this progress file, the capture directory, the `ci` body slots, and this landing message. The spec and the ledger were never edited.
- Restarts/killed sub-agents: none. The lane text was re-read at the session start and again after the long waits.
- Spec read: `spec_source=ledger_worktree`, `spec_differ=true`. `executor-skew`: `match` (0.1.2106 both).

## 8. Build server

Setup preflight `ready`; every build was submitted and ran on the daemon (`daemon_longpoll`), 30+ builds, no fallback, no `killed` job. One `indeterminate` run was a selector miss (`-Dtest=A,B` with a module that holds neither), re-run with a selector matching every module. `runtime_stamp_reason`: none.

## 9. Residue

- **Merged-file defect:** `pm-operator/src/main/java/de/planmarshall/operator/PmConsole.java` carries a stray `/**` inside the `notifyOperator` Javadoc (a pre-commit rewrite artifact committed in `1bbf9fba`). It compiles and CI passed; a one-line follow-up is needed.
- **D5 shell-path PTY test** is not shipped; the lease fix itself is in `FindCommand.open`.
- **D4**: `NeedsIT` keeps its project-level start; `needsItemFormattingExhaustive` stays one `@Test`; the `openNeedsItemAct` path is covered through the project-level IT.
- The overview's "Needs you" group has no next-page row (PLAN-50 owns the rows).
- `epic <planId>` for a plan-only landing failure resolves no epic; the spec pins the word.
- An added `setup`/`findings` item is announced only when it arrives in a machine summary.
- `doc/design/terminal-client/04-console.adoc:74-82` still names the badge key `waiver_request` while the contract's kind is `waiver` (both render).
- `SecretReader.java:56-66` has the same Ctrl+C-after-raw-mode pattern; named, not changed.
- The merge queue dequeued the first enqueue because `main` moved; the branch was merged with `origin/main` (one conflict in `MockRuntimeEngine`, resolved with main's `OPERATOR_FIELD`), re-gated and re-enqueued.

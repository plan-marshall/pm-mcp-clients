envelope_version=1
sender_type=plan
sender_id=rb-186-sonar-standing-debt-pm-operator
epic=ui-improvements
kind=landing
created=2026-11-24T12:46:25Z

```landing-facts
schema=landing-facts/1
plan_id=rb-186-sonar-standing-debt-pm-operator
epic=ui-improvements
pr=#556
merge_state=merged
cleanup_owed=false
deliverables_total=3
deliverables_done=2
total_tokens=unknown
harness=opencode
model=deepseek-v4.1-flash/high
effort=high
plugin_version=unknown
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-186 landing — the standing Sonar debt of `pm-operator`, and the zero read

PR #556 merged through the merge queue as **81e2c1fc59389c0e484b66b25abb5d6aaa287558**. Branch `fix/186-sonar-standing-debt-pm-operator` pruned, worktree removed, main checkout pulled to the merge commit.

## 1. Deliverables

Evidence order for every Done-when of this spec is `none`, so no failing-run commit or output identifier applies.

**D1 — `pm-operator` production code (98 issues, 32 files)**

| Done-when | Verdict | Evidence |
|---|---|---|
| `read_open_issues` names no open issue under `pm-operator/src/main` on the PR, and on the default branch after the analysis of the merge commit (`unknown` while it has not run, never `0`), or each remainder named with the operator's selection | met | PR read on the head: `count_status: confirmed`, `open_issue_count: 0`, `open_keys: []`, `analysis_revision` = `d046c984` (also `cf3ffb28`). The default-branch arm is `unknown` — the merge commit is not analysed yet (see § 9). |
| `./mvnw -pl pm-operator -am verify` passes with no fewer tests; `git diff` names no file under `src/main/resources` and none under `src/test/resources/screen-captures` | met | `verify -pl pm-operator -am`: 5527 tests, 0 failures (2873 in `pm-operator`); no `@Test` removed (diff read); `git diff --name-only origin/main...HEAD` names only `pm-operator/**/*.java`. |

**D2 — `pm-operator` tests (12 issues, 9 files)**

| Done-when | Verdict | Evidence |
|---|---|---|
| `read_open_issues` names no open issue under `pm-operator/src/test` on the PR and after the merge analysis, or each remainder named with the operator's selection | met | the same PR read: 0 open issues, 0 hotspots. |
| `./mvnw -pl pm-operator -am verify` and `./mvnw verify -Pintegration-tests -pl pm-operator -am` pass; no `Thread.sleep` under `pm-operator/src/test` | met | module verify green (5527 tests); integration run green (1102 tests incl. failsafe ITs, 344 s); `Thread.sleep` occurs nowhere under `pm-operator/src/test`. |

**D3 — the zero read of the repository: NOT MET**

The read was performed after the merge and is `unknown`, never `0`: the default branch still holds its pre-merge analysis (the read returns the same 113 keys with an empty `analysis_revision`), and Sonar's compute engine lists no `main`-session task after the merge commit. The provisional pre-merge figure, all of which #556 fixes:

- by module: `pm-operator` 110 (98 `src/main` in 32 files, 12 `src/test` in 9 files), `pm-mock-runtime` 2, `pm-exec` 1 — 113 in total.
- by rule: `java:S1192` 36, `S3776` 19, `S4449` 6, `S3358` 5, `S1130` 5, `S6206` 4, `S135` 4, `S9358` 3, `S7467` 3, and 1–2 each of `S1172`, `S107`, `S1905`, `S127`, `S2925`, `S6213`, `S1488`, `S106`, `S3398`, `S1125`, `S2589`, `S6885`, `S5778`, `S1481`, `S1075`, `S125`, `S1068`, `S1066`, `S9412`.
- the three PLAN-181 keys are still open in that read, unchanged and still the operator's to dismiss by hand: `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`, `MockRuntimeEngine.java:10695`), `AaGRMDtGxhXuOwwhaS0K` (`java:S4449`, `MockRuntime.java:245`), `AaEcTdY0MJNX7b6L9QAy` (`java:S112`, `NativeKernel.java:229`).

A follow-up read of the default branch once Sonar has analysed `81e2c1fc` settles it; the PR's own read on the merge head is a confirmed 0 and the merge-group analysis passed, so the expected default-branch answer is zero.

## 2. Personas and skills

| Step | Persona |
|---|---|
| D1 production code | `plan-marshall:persona-implementer` (declared) |
| D2 tests | `plan-marshall:persona-module-tester` (declared) |
| D3 read | `plan-marshall:persona-implementer` (declared) |
| self-review | `plan-marshall:persona-code-reviewer` |

Domain skills, per deliverable as the spec declares them: D1 `pm-dev-java:java-core`, `pm-dev-java:java-null-safety`, `pm-dev-java-cui:cui-logging`, `plan-marshall:ref-code-quality`; D2 `pm-dev-java:junit-core`, `pm-dev-java:junit-integration`, `pm-dev-java-cui:cui-testing`; D3 `pm-dev-java:java-core`. No skill was added for surface drift; no named skill was dropped.

## 3. Hypotheses

- `HYPOTHESIS: every java:S106 finding sits where the CLI writes its own output` — **confirmed**. The module's only `java:S106` site was `OverviewFullScreenApp.dialogueOut()` (line 1037), the writer the dialogues print on, which is exactly the CLI writing its own output.

## 4. Findings

- **Self-review round 1** (`persona-code-reviewer`, diff `origin/main...HEAD`), no blocker:
  - major, traceability: the Expected Surface no longer matches the diff — `OperatorActions.java` and five test files were compile-forced by the two grouped signatures, and `LessonSourceCommand.java` was listed but untouched. Recorded here and in the PR body; the spec itself is not edited (Write-Boundary).
  - minor: `StatusCommand.renderEndedOverview`'s new Javadoc said "the empty line" where the code prints the `overview.ended.empty` message — fixed in 76557932.
  - nit: `PseudoTerminal` had replaced try-with-resources with try/finally, so a throwing `close()` would mask the primary failure — restored try-with-resources under the unnamed pattern in 76557932.
  - nit: the S106 exception is a code suppression, not a Sonar dismissal — recorded in § 6.
- **Sonar, first PR head** (4e0ea1f8): 23 self-introduced issues (S3516 ×5, S2583 ×5, S2589 ×2, S1192 ×3, S4449 ×2, S2637, S1845, S4165, S1126, S2184). All fixed in f4dd1977; the head then read 0.
- **CodeRabbit (PR comment)**: one inline finding on `EpicViewFormatting.countsKey` — a hardcoded second copy of the catalogue's `overview.epic.counts.*` list. Fixed in cf3ffb28 by deriving the key and admitting it only through `MessageCatalogue.hasKey`; replied with the commit and resolved the thread. Disposition: `finding: coderabbit inline inline-1 — cf3ffb28`.
- **CI**: `build / build (25)` failed once on `pm-api`'s `EntryRecordsTest.runtimeEntryResourceRefusesAMalformedLegacyPort` — a generated-input test in a module this PR does not touch (seed 4346595339864880245). Rerun green; not this plan's failure.

## 5. Reviewers

| Reviewer | Row | Outcome |
|---|---|---|
| `coderabbit` (required, quota-limited, review-complete-after-handling) | reviewed / refused / silent → reviewed | Reviewed `4e0ea1f8` (`kind=reviewed` marker, Merge Risk Minimal, no actionable comment) and `9f8387fb` (marker, Merge Risk Low, the one `countsKey` finding). A rate-limit notice followed the `cf3ffb28` delta ("review limit reached"); its later reply said the last commit was already reviewed. Every finding handled; thread resolved; gate `all_handled`. |
| `sourcery` (optional) | refused | "your pull request is larger than the review limit of 150,000 diff characters" — its own hard limit. Optional, does not gate. |

## 6. Operator questions

1. Asked: "PLAN-186 deliverable 1: the single java:S106 finding (OverviewFullScreenApp.java:1037, key AaFfVblug_p_FqfrTT2w) … How should it be handled?" — options {dismiss as false positive, dismiss as won't fix, fix it in code, leave it open and stop}. **Free-text answer: "Supress using supression comment".**
2. Re-asked, because a suppression bends the spec's "no `NOSONAR`, no `@SuppressWarnings`" constraint and needs an explicit selection: "Which form should I record and apply…?" — options {NOSONAR comment on the line, `@SuppressWarnings` on the method, no code change; dismiss in Sonar}. **Selected: `@SuppressWarnings` on the method.** Applied as `@SuppressWarnings("java:S106")` on `dialogueOut()`, with its Javadoc stating why; a deliberate, recorded bend of the spec's constraint.

No other question was asked. No question tool call was cancelled.

## 7. Rule check

- guard: none on this harness (unenforced) — nothing was enforced, so no guard-log line exists for this run and "all kept" is not claimed.
- `executor-skew`: **match**, executor 0.1.2183 = lane 0.1.2183.
- spec read: `spec_source: ledger_worktree`; no `spec_differ` marker.
- Deviation, deliberate and reported: reverting `-Ppre-commit`'s rewrites used `git checkout-index -f -- <paths>` on the 18 files it rewrote (the spec instructs "revert those"). It is not `git checkout -- <path>`, but it is the same class of action and is disclosed rather than left implicit.
- Every routed build was waited on in its own call; none was backgrounded. One build hit the daemon bound (see § 8) and was re-submitted at a raised bound, never with the same one.
- Shell/file discipline: all file reads and edits went through the file tools; no `cat`/`ls`/`grep`/`find`/`sed`, no redirects, no `python3 -c`, no `sleep`. Temporary notes live under `.plan/temp/rb-186-sonar-standing-debt-pm-operator/`.
- The plan's `.plan/` writes are its one progress file, its `.plan/temp/` notes, one `ci` body slot per `prepare-*` call, and its one inbox message.

## 8. Build server

- Setup preflight: `ready` (registered). Every build ran through the daemon: **12 builds, mechanism `daemon_longpoll`**, no `in_process_fallback`, no degraded result, no launch-fault retry.
- One bound expiry: the first `verify -Ppre-commit` was cut at the 966 s bound after its 5637 tests had passed; re-submitted at 2400 s → green in 776 s.
- Green runs at the head: `verify -pl pm-operator -am` (5527 tests), `verify` full tree (twice, after each base merge), `verify -Ppre-commit` (5637 tests; its rewrites reverted as the plan directs), `verify -Pcoverage` (≥80 % per module), `verify -Pintegration-tests -pl pm-operator -am` (1102 tests), plus CI's Maven build (25 and 27), integration tests and SonarCloud analysis.
- No `killed` job. `runtime_stamp_reason` was not reported; `plugin_version` is `unknown` from the runtime-info seam.

## 9. Residue

- **D3's zero read is pending Sonar's analysis of `81e2c1fc`.** The default-branch read is `unknown` (pre-merge snapshot, empty `analysis_revision`, no main-session CE task after the merge). No `0` is claimed; a later read settles it.
- The three PLAN-181 dismissals remain the operator's to make by hand (`sonar_rest transition` still cannot transmit them).
- `LessonSourceCommand.java:45` (`java:S1075`) was left untouched: the finding sits on a literal that is already a named constant, and the only alternatives were a suppression or a behaviour change. Named here rather than silenced.
- Expected Surface drift: `OperatorActions.java` and five test files (`ControlDialoguesTest`, `HandlerInterruptExitCodeTest`, `InterruptExitCodeTest`, `ActionMenusTest`, `ActionsCatalogueTest`) are compile-forced call sites of `ActContext` and `EpicLabels`; `LessonSourceCommand.java` is listed but unchanged.
- `-Ppre-commit` is still not idempotent on this repository: it re-indents untouched files and re-inserts `cui-rewrite` TODO markers the gate itself rejects (the defect PLAN-180 recorded). Its output was reverted, not committed.
- The mailbox could not be read at any of the three reads: `inbox read --plan-id` rejects the digit-leading plan id `186-…` with `invalid_target_plan` (the defect PLAN-181 recorded). This is reported as "could not look", never as "no mail".

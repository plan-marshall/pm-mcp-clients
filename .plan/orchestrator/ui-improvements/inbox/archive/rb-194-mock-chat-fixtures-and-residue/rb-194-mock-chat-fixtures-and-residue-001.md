envelope_version=1
sender_type=plan
sender_id=rb-194-mock-chat-fixtures-and-residue
epic=ui-improvements
kind=landing
created=2026-11-26T11:40:43Z

```landing-facts
schema=landing-facts/1
plan_id=rb-194-mock-chat-fixtures-and-residue
epic=ui-improvements
pr=#589
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=3
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=22
review_body_findings_handled=22
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# Landing Report — PLAN-194 (PR #589)

Merged PR `#589` (`feature/194-mock-chat-fixtures-and-residue` -> `main`) at merge commit `4fde136a351e6cac0affe40eb81b246334e40e5f`.

## 1. Deliverables

- **D1 — The scenario fixtures (`running-work.json`, `fully-onboarded.json`): met**
  - Tests: `ChatFixtureScenarioTest` (+ 11 related suites).
  - Order: `test-first`. Pre-fix failing run before `b9841364`: daemon job `584a643452db41c1b5f136ae3176bf8d` (`exit_code: 1`, 404 on `/api/v1/plans/session-log/chat` in `running-work` and `fully-onboarded`). Passing runs: `b9f9f4b72c7c44d7af6c809ad18c174c` (`257` tests, `exit_code: 0`), `649cec5582c945a481ee29dcbcc92b0e` (`2698` tests, `exit_code: 0`), full `verify` `cdb4cc6cadac493e94bd9561f4fdc7d0` (`5685` tests, `exit_code: 0`), and final post-merge `verify` at `2eef1264` (`c6d73bb7e6684db0ba75b4db673e9958`, `exit_code: 0`). Commits: `b9841364`, `e3a166cb`.
- **D2 — Applying and refusing a proposal: not met as written (partial `test-first` on `pm-api`; `order: none` on `pm-mock-runtime`)**
  - Tests: `ChatReadModelTest`, `ChatProposalsTest`, `ChatProposalRefusalTest`, `ChatBroadcastTest`.
  - Order: `test-first` on `ChatReadModelTest` compile failure (`a162605ce3084815b91624fda3654d7e`, `exit_code: 1`, missing `ChatProposal.isOpen(boolean)` aborted reactor before `pm-mock-runtime`); `order: none` on pre-fix runtime suites (a detached base-worktree attempt failed with `executor_in_worktree: absent` and was removed). Passing after `76bf68a2`: `e40010f439b645029939d8fc000c3c64` (`121` tests, `exit_code: 0`), `cdb4cc6cadac493e94bd9561f4fdc7d0`, `0d5a63c9` pre-commit (`c19547de9310408cbbf8b4a49d1c5fc7`, `exit_code: 0`), and `2eef1264` (`c6d73bb7e6684db0ba75b4db673e9958`, `exit_code: 0`). Commits: `76bf68a2`, `efe58325`, `0d5a63c9`.
- **D3 — The revision window and the narrative's truth: not met as written (`test-first` on `scope.ended` queued-revision gap; `order: none` on initial 4 items)**
  - Test: `ChatWindowAndNarrativeTruthTest`.
  - Order: `none` on initial 4 items before `464ef46b` (jobs `da6520f94add4782b0837d1b0c3364e1` stale `pm-api` and `cfc42ecf8cc144d0a94b391c6c27d86a` `indeterminate`); `test-first` on `scopeEndedWithQueuedRevisionKeepsWindowClosed` before `89d9c934` (`e622e76735bb41479ad98aa9a18ecc64`, `exit_code: 1`, `MultipleFailuresError`). Passing after `464ef46b`/`89d9c934`/`0d5a63c9`: `267f406c1788424ca4d28f466425745d` (`61` tests, `exit_code: 0`), `27ca5fc787ca476b0c42b38b4f3f126` (`38` tests, `exit_code: 0`), `c19547de9310408cbbf8b4a49d1c5fc7`, and `c6d73bb7e6684db0ba75b4db673e9958` (`exit_code: 0`). Commits: `464ef46b`, `89d9c934`, `efe58325`, `0d5a63c9`.
- **D4 — The race and the order pins: met**
  - Tests: `ChatRegistryRaceTest`, `ChatRevisionOrderTest`.
  - Order: `mutation-after` against `f912bffb`/`f9a96a4e`. Mutation 1 (`ChatRegistry.playFailure` split synchronized block) failed `ChatRegistryRaceTest.stopNeverResurrectedByReplacedWorker` (`a8075e9d4e9b4ce88a141cf41d184aab`, `exit_code: 1`, `NullPointerException`); Mutation 2 (`ChatRegistry.settle` swapped `revisionQueued`/`activityChanged`) failed `ChatRevisionOrderTest.applyOrderAndOverview` (`a401df5cd568498ab06a8665c8701bc8`, `exit_code: 1`). Passing after `f9a96a4e`/`890b180e`/`0d5a63c9`: `7de735e293c843108f8741d532e8406e` (`107` tests, `exit_code: 0`), `beb82236d11f49e58f331fdf2de69aaf` (`10` tests, `exit_code: 0`), `c19547de9310408cbbf8b4a49d1c5fc7`, and `c6d73bb7e6684db0ba75b4db673e9958` (`exit_code: 0`). Commits: `f912bffb`, `f9a96a4e`, `890b180e`, `efe58325`, `0d5a63c9`.
- **D5 — Narrative completeness and the smaller items: met**
  - Tests: `ChatNarrativeCompletenessTest`, `ChatContextNarrativeTest`.
  - Order: `test-first`. Failing pre-fix runs: before `b7810c6c` (`bde62327cd8944aeb03fcd793530c925`, `exit_code: 1`, 8/9 tests failed in `ChatNarrativeCompletenessTest`); before `89d9c934` (`e622e76735bb41479ad98aa9a18ecc64`, `exit_code: 1`, `prMergedEmittedWhenScopeEndsDuringReply`). Passing after `b7810c6c`/`89d9c934`/`ad701ee4`: `5a05a637d01a4ed093e241f3c456a850` (`289` tests, `exit_code: 0`), `27ca5fc787ca476b0c42b38b4f3f126` (`38` tests, `exit_code: 0`), and `c6d73bb7e6684db0ba75b4db673e9958` (`exit_code: 0`). Commits: `b7810c6c`, `7e1f2346`, `89d9c934`, `ad701ee4`, `96c4c92a`.
- Final pushed verification on `2eef1264` (`verify`, daemon job `c6d73bb7e6684db0ba75b4db673e9958`, `exit_code: 0`) and prior pre-commit + coverage run (`c19547de9310408cbbf8b4a49d1c5fc7`, `exit_code: 0`).

## 2. Personas and skills

- Setup / build / PR / review / merge / landing: default lane workflow; Self-Review: `plan-marshall:persona-code-reviewer`.
- **D1**: `plan-marshall:persona-implementer`; declared skills: `pm-dev-java:java-core`, `pm-dev-java:junit-core`; added: `pm-dev-java-cui:cui-testing` (from `CLAUDE.md` project CUI test standards).
- **D2**: `plan-marshall:persona-implementer`; declared skills: `pm-dev-java:java-core`, `pm-dev-java:junit-core`.
- **D3**: `plan-marshall:persona-implementer`; declared skills: `pm-dev-java:java-core`, `pm-dev-java:junit-core`.
- **D4**: `plan-marshall:persona-module-tester`; declared skills: `pm-dev-java:junit-core`.
- **D5**: `plan-marshall:persona-implementer`; declared skills: `pm-dev-java:java-core`, `pm-dev-java:junit-core`.

## 3. Hypotheses

- Scenario fixtures fit existing scenario schema without loader change — **confirmed** (`ScenarioLoader.java:1100-1180`).
- `sanity-check` phase in `pm-mock-runtime` / `pm-api` — **refuted** (neither module defines a `sanity-check` phase, so no phase constant was added).

## 4. Findings

- **Self-review** (`651d68b6-806c-49a3-b5ce-d63774b0f91f`): Round 1 findings fixed (including `ChatTextsCatalogueTest` / `MockRuntimeEngine` / `ChatRegistry` cleanups); Round 2 clear (`ad701ee4`, 0 open findings).
- **CI**: All 20 GitHub checks green on final pushed head `2eef1264`. Earlier `ChatTextsCatalogueTest` blocker on `ad701ee4` resolved by merging `origin/main` (`9011e6d1`) at `1493aa14` and aligning `ChatNarrativeCompletenessTest` at `96c4c92a`.
- **SonarCloud**: 22 issues raised on `efe58325` fixed in `da26bc21` and `cdf9c750`; confirmed `0` open issues, `0` unreviewed hotspots, and Quality Gate `OK` (`86.6%` new coverage, `0.0%` duplication) on final head `2eef1264`.
- **CodeRabbit PR #589 (22/22 disposed, 0 unresolved threads)**:
  - `inline-1` (`PRRT_kwDOVBOtss68P4TV`): out of scope (`pm-web-app` REFUSED UI rendering belongs outside PLAN-194; API contract verified in `ChatReadModelTest` and `ChatProposalRefusalTest`); resolved.
  - `inline-2`..`inline-16`, `outside-diff-1`, `nitpick-1`: fixed in `efe58325` (including follow-ups on `P4Tr`, `P4Tv`, `P4UU` completed in `0d5a63c9`); all threads replied and resolved.
  - `inline-17` (`PRRT_kwDOVBOtss68RXvG`), `inline-18` (`PRRT_kwDOVBOtss68RXvK`), `outside-diff-2` (`MockRuntimeEngine.java:5995`): fixed in `0d5a63c9`; threads replied and resolved.
  - `minor-1`: declined — `ChatProposalsTest` intentionally verifies HTTP 200 `ProposalState.APPLIED` + `MOCK-100` WARN for blank/unapplicable best-effort hooks.

## 5. Reviewers

- **`coderabbit`** (`required`, `quota_limited: true`, `review_complete_after_handling: true`):
  - Reviewed `96c4c92a` (`2026-11-26T06:44:01Z`, formal review `PRR_kwDOVBOtss8AAAABacc8Lg` with followup reviews at `efe58325` and `cdf9c750`).
  - Trigger history: initial manual trigger on `ad701ee4` at `2026-11-25T23:06:53Z` refused on rate limit; automatically reviewed on subsequent push `96c4c92a`; skipped (`NEUTRAL`) after `aa69e95e`/`2eef1264` per `review_complete_after_handling: true` once all 22 findings were handled.
- **`sourcery`** (`optional`, `quota_limited: true`, `review_complete_after_handling: false`):
  - Refused / skipped (PR diff exceeds 150,000-character limit).

## 6. Operator questions

- **`ad701ee4` CI blocker (`ChatTextsCatalogueTest`)**: Asked how to proceed after `ChatTextsCatalogueTest` failed against `origin/main` `#587`; operator free-text response `"recheck main and rebase"` -> fetched and merged `origin/main` (`9011e6d1`) at `1493aa14` (without rebase, per standing rule) and aligned `ChatNarrativeCompletenessTest` (`96c4c92a`). No further questions asked.

## 7. Rule check

- `guard: none on this harness (unenforced)`
- Spec read: `spec_source: ledger_worktree` (`.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-194-mock-chat-fixtures-and-residue.md`, no `spec_differ` marker).
- Executor skew: `executor_in_worktree: present` (`executor_version: 0.1.2183`).
- All git operations, Sonar checks, CI waits, and Maven builds executed through `python3 .plan/execute-script.py` or `git -C` without raw `mvn`, `gh`, `cat`, `ls`, `grep`, `head`, `sed`, rebase, or force push. One detached base-worktree `D2` fail-first attempt was cleaned up cleanly after encountering `executor_in_worktree: absent`.
- Mailbox reads: 3 reads performed (`rb-194-mock-chat-fixtures-and-residue` -> `no_mailbox`, `count: 0`; `194-mock-chat-fixtures-and-residue` -> `invalid_target_plan`).

## 8. Build server

- Setup preflight: `build_server: ready`.
- Mechanism: all builds ran via `daemon_longpoll` (`in_process_fallback: 0`, `killed: 0`).
- Runtime stamp note: `resolve` returned `model=unknown`, `effort=unknown` for `harness=antigravity` without a separate `runtime_stamp_reason` field.

## 9. Residue

- None on the shipped code (`PR #589` merged cleanly at `4fde136a351e6cac0affe40eb81b246334e40e5f`, Sonar `0` open issues, CodeRabbit `22/22` findings disposed, feature branch/worktree cleaned up).
- `pm-mcp-core` `state-catalogue.adoc#chat-narrative` had no conflict or lapse narrative line key (`ChatMessageKeys.LINES` defines none), so none was fabricated in `pm-mock-runtime`.

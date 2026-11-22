# Landing Analysis: PLAN-175 — The Standing Sonar Debt: 493 Open Issues Down to Zero (PART 1 OF 3 ONLY)

epic: ui-improvements
workstream: WS-04
pr: #530 (merged as f00da06e, `fix(pm-api): clear the standing Sonar debt in the production code (PLAN-175, 1/3)`; 86 files, +881/-652 against the first parent: 83 `pm-api/src/main` files and three new characterisation tests)

> Landing record for one shipped plan, **a partial one**. Written by `analyze` (inbox message `plan-175-sonar-standing-debt-009.md`, plan-marshall lane, `landing-facts` block present; analysed 2026-11-22). `landing-check` is **incomplete**: `unresolved_review_threads`, `review_body_findings_total` and `review_body_findings_handled` are `unknown` (the lane measured none). The message was read as a lead; the PR, the merge commit, the review gate and Sonar were re-read, and the archived plan's work log (`.plan/local/archived-plans/2026-11-22-plan-175-sonar-standing-debt/logs/work.log`) was read for the `script_failure` candidates. The lane's runtime stamp is the start-time self-report (`refresh-runtime-stamp` returned `model_unknown`).

## Deliverable Fidelity vs Spec

The spec has six deliverables (the outline of the run had eight). **Only deliverable 1 landed**, by the operator's cut of 2026-11-22 into three pull requests (P1 `pm-api` production code, this one; P2 the `pm-api` tests, Awaitility and `JsonTree`, stacked on P1; P3 `pm-mock-runtime`, `pm-web-app`, `pm-relay`, `pm-exec`, independent). The other deliverables were retired from THIS plan by recorded operator decisions, not completed; P2's commits are kept on the local branch `backup/plan-175-full`; the `pm-operator` deliverable and the final zero read move to a follow-up after PLAN-174.

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 The blocker and the `pm-api` production code | **shipped-modified, Done-when 1 NOT met**: the BLOCKER and 140-odd issues are fixed, but three issues remain open under `pm-api/src/main` on `main` | `sonar read_open_issues` (default branch, 2026-11-22): `S6878 HttpResponse.java:48`, `S1168 JsonTree.java:276`, `S1075 RuntimeClient.java:34` |
| D2 The `pm-api` tests | not landed (retired from this plan; P2) | message "Residue"; default branch still carries 42 open issues under `pm-api/src/test` |
| D3 `pm-mock-runtime` | not landed (P3) | 165 open on `main` |
| D4 `pm-operator` | not landed (follow-up after PLAN-174, which is running) | 117 open on `main` |
| D5 `pm-web-app`, `pm-relay`, `pm-exec` | not landed (P3) | 17, 9 and 3 open on `main` |
| D6 Zero is read | not landed; the zero state is NOT reached | 356 open on `main` in total (493 at the 2026-11-20 read) |
| added-unplanned | three characterisation tests (`SseReaderTest` +33, `SpawnPlanTest` +14, `RuntimeClientTest` +20) pin the three spots whose fix touched logic: accepted, they are what the spec's "add a test first" standing constraint asks for | `git diff f00da06e^1 f00da06e -- '*Test.java'`: 3 files, +67/-0 (re-read) |

**The plan title's goal ("493 open issues down to zero") is NOT met by this landing**: the default branch shows **356 open code smells, no blocker** (`pm-api` 45 of which 3 in main and 42 in test, `pm-mock-runtime` 165, `pm-operator` 117, `pm-web-app` 17, `pm-relay` 9, `pm-exec` 3), read from the provider on 2026-11-22 (`count_status: confirmed`).

## Three-Layer Analysis

The landing was read on three layers by one read-only leaf, and the orchestrator re-read the points recorded as `checked` at `f00da06e` (named below). By reading, no build, no test run, no write.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`, plan-less) — reason: an 83-file mechanical refactor with native-struct records and a state-machine rewrite is the reading depth the dispatch rule exists for.
- **Verdict:** layer 1 findings (3); layer 2 findings (5) and not shown by reading (2); layer 3 findings (2 minor) with 6 confirmed-equivalent traces, not shown by reading (1). **No behaviour defect found in the 83 files**; no blocker, no major at layer 3.
- **Re-read by the orchestrator** (`file:line` at `f00da06e`): `HttpResponse.java:44-52`, `JsonTree.java:270-278`, `RuntimeClient.java:32-36`, `Libc.java:86-95`, `EpicProcess.java:45-53`, `RuntimeTokenFile.java:35-42` (and the pre-state `:35-44`), `UnixSocketHttpClient.java:36-40` (path corrected: the leaf wrote `runtime/`, the file is in `http/`; `public final class` before, `public record` after), `SpawnPlan.java:41-45`; the `NOSONAR`/`@SuppressWarnings` grep at `f00da06e` (33 sites in `pm-api/src/main`, none added; the only added suppression-like line is `// cui-rewrite:disable InvalidExceptionUsageRecipe`); the test diff (3 files, +67/-0, no removed line); the `throws IOException` removals in the diff. The leaf's remaining `info` traces (the `SseReader`, `RuntimeClient.tryOpen`, `PosixPlatform` records, `fromMap` rewrites, ~118 constant substitutions) are **unverified leads** of the orchestrator: they were not re-read line by line.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 (major) | Done-when 1 ("no open issue under `pm-api/src/main`") cannot hold together with the deliverable's own clause that an issue whose fix would change behaviour or a public contract is NOT fixed by that route and the lane says why: Done-when 1 has no carve-out. `S1075` on a public wire-path constant is exactly such a case | checked | refutes | `RuntimeClient.java:34` (re-read: `public static final String STATUS_PATH = "/api/v1/status"`) |
| L1-2 (minor) | The spec lists `S1168` as "a method returns an empty collection and not `null`", but `JsonTree.objectOrNull` returns a `Map` where `null` means absent and callers branch on `!= null`; the lane's route (an `Optional` in P2) changes a public signature, against the standing "no public API change". By the spec's own exception it should have been named as not fixable by that route | checked | refutes | `JsonTree.java:270-277` (re-read: javadoc "or null when absent", `return null;` at `:276`) |
| L1-3 (minor) | Done-when 1 can be met by masking: `pm-api/src/main` holds 33 pre-existing `@SuppressWarnings` and `NOSONAR` sites (re-read by grep; the leaf counted 5 + 2, the grep of both tokens finds 33, most `@SuppressWarnings("unchecked")`), all untouched; the spec's "no suppression" does not require removing them, so "no open issue" does not mean "no issue" | checked | refutes | `git grep -n -E "NOSONAR\|@SuppressWarnings" f00da06e -- pm-api/src/main` |

**Layer 1 verdict:** findings — 3. No corpus claim of a staged spec is named (the spec's claim labels are not touched by a finding), so no verdict is persisted.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D1 (BLOCKER) | `SpawnPlan.java:43` `DAEMON_LOG_MODE = OWNER_READ \| OWNER_WRITE` (re-read; 384 = octal 0600, value unchanged) | `SpawnPlanTest.java:100` `daemonLogMode` compares against `Integer.parseInt("600", 8)` with a literal expectation: non-vacuous (leaf) | — | checked |
| D1 (the rest) | 83 production files | `SseReaderTest` (CR/LF terminator states; leaf traced the expected events against the old loop), `RuntimeClientTest` (`startedRuntimeRefusesCredential` pins 401-after-start; the `started` flag itself is pinned only by the pre-existing test at `:160`): partly non-vacuous | **the landing message's residue omits `S6878` at `HttpResponse.java:48`**: the file was touched by one whitespace-only change and the type pattern left, although the same record-pattern fix was applied in `PosixSpawn.java:99-118` and `JsonTree.java:203,324`; it needs no wire or API change. The message's "after PR 2 nothing else in `pm-api`" is therefore false | **findings (major)**: `HttpResponse.java:48` re-read: `return other instanceof HttpResponse that && status == that.status …` |
| D1 | `RuntimeTokenFile.java:37,41` `DIRECTORY_MODE` and `FILE_MODE` went from a mutable set (pre-state re-read: `PosixFilePermissions.fromString(...)`) to `Collections.unmodifiableSet`; `UnixSocketHttpClient` went from `final class` to a `record` (structural `equals`/`hashCode`/`toString`); eight public `fromMap` signatures dropped `throws IOException` | none: the spec says a test is added first where none covers the code; none was added for these | the narrative ("no behaviour change, public API unchanged") omits these; S1130 and S6206/S6878 authorise the last two; no in-tree caller mutates the sets (leaf, unverified lead) | findings (minor) |
| D1 | `files/package-info.java` deleted | — | deletion is sound (the package held nothing else, no reference left, the file carried no `@NullMarked`) — leaf, unverified lead | checked (leaf) |
| claim | "86 production files" | — | the diff has 83 production files (82 modified, 1 deleted) and 3 test files; the spec says 83 | findings (minor) |
| not shown | the unit-test total (1671), the whole-tree verify, "test count not lower", Sonar 0 at the merge head, 43 S1845 + 1 S3415 fixed, the operator override of the functional review, 17 done and 3 infeasible tasks; the `Libc.java` marker placement (a trailing comment inside the `catch (Error)` block, not the prefix of `catch (Throwable e)`, re-read at `:88-92`) and whether the recipe honours it | | | not_shown_by_reading |

**Layer 2 verdict:** findings — 5, not shown by reading — 2.

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `Libc.java:91-92` (re-read) | the pre-commit OpenRewrite run | a live `// cui-rewrite:disable InvalidExceptionUsageRecipe` now suppresses the project's own rule against broad catches on `catch (Throwable e)` (the comment sits inside the previous block); not a Sonar form, so the spec's "no suppression" list does not name it; behaviour is unchanged (`Error` is rethrown first). Whether the recipe honours the placement cannot be shown by reading | minor |
| L3-2 | `EpicProcess.java:47,51` (re-read) | a future rename of the `"project"` wire key | `project = project != null ? project : PROJECT_KEY` reuses the wire-key constant as a DEFAULT value (the string is still `"project"`, so nothing changes today); a rename of the key would silently change the default. `ProcessTree.java:67-68` (leaf) keeps `PROJECT_KEY` and `DEFAULT_PROJECT_ID` apart, which is the form to follow. This is the code-review advisory of the run, answered | minor |

**Layer 3 not shown by reading:** the compile effect of the narrowed `throws` on callers in other modules (`CleanupCommand.java:745` in `pm-operator`; `MockRuntimeEngine.java:1949,5776,5841,9389`): covered only by the lane's claim that the whole-tree verify passed; the leaf found no `catch (IOException)` that depends on the removed declarations.

**Layer 3 verdict:** findings — 2 (minor), not shown by reading — 1. The leaf traced the `SseReader`, `RuntimeClient.open/tryOpen`, `PosixPlatform` native-layout records (order and values of `SpawnFlags`, `StructSizes`, `OpenFlags` against both platforms), `PosixSpawn` record patterns, the ternary and null-handling rewrites and ~118 added constants (each equal to the literal it replaced) as behaviour-preserving (unverified leads of the orchestrator, see above).

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec | no suppression (`@SuppressWarnings`, `NOSONAR`, exclusion) | deviates | no Sonar form added; the live `cui-rewrite:disable` at `Libc.java:91` is added; 33 pre-existing sites untouched |
| spec | behaviour does not change: no wire name, message key, JSON field, public API | deviates | no wire or default value changed; small public-surface changes unstated in the narrative (immutable mode sets, record conversion, narrowed `throws`) |
| spec | no test assertion edited | conforms | 3 test files, +67/-0 (re-read) |
| spec | Done-when 1: no open issue under `pm-api/src/main` | deviates | three open (S6878 undeclared and fixable, S1168 deferred, S1075 declared contract-bound) |
| CLAUDE.md § Code Standards | every package has a `package-info.java` with Javadoc; `@NullMarked` on every package of a module that declares JSpecify | conforms (this landing) / deviates (pre-existing) | only `api/files` (empty) lost its file; only `api/chat` and `api/changes` carry `@NullMarked` while `@Nullable` is used module-wide (leaf, unverified lead); the landing adds more `@Nullable` in unmarked packages |
| CLAUDE.md § Code Standards | never catch generic `Exception`/`RuntimeException`; no `System.out/err`; JUnit 5, `Generators` with `@EnableGeneratorController`, no Mockito | conforms | no added violating line (leaf) |
| CLAUDE.md § Dependencies | no dependency or plugin; no change outside `pm-api` production code in this PR | conforms | the diff names only `pm-api/src/main` and three `pm-api` tests |
| global CLAUDE.md | commit trailer `Co-Authored-By: plan-marshall` | conforms | merge commit message (leaf) |

## Metrics and Anomalies

- Tokens: 27,369,718 (`landing-facts`); wall seconds 103,635 (about 28.8 h of elapsed time, mostly waiting on the shared build daemon and the review loop). Harness `claude`, model `claude-sonnet-5-5`, effort `medium` (start-time self-report, not measured).
- Anomalies: the first push of the PR carried **44 self-introduced Sonar issues** (43 `java:S1845` and 1 `java:S3415`, from the plan's own `java:S1192` constant extraction) fixed in four fix tasks before the merge (lesson `2026-11-22-16-001`); `functional-review` looped back three times and was closed by operator override (`loop-back close`, heads `d5198e88` and `6ce6a953`), with no functional verdict facts recorded; one voluntary checkpoint was reclassified as an error for no progress (TASK-4 over a 10,000-line file; lesson `2026-11-22-16-005`); build-daemon queueing of 5 to 15 minutes made three leaves hand back on a running compile (lesson `2026-11-22-16-004`); the PR cut and the seven retired deliverables (lesson `2026-11-22-16-003`).
- The `script_failure` candidates of the message (`manage-architecture`, `manage-findings`, `manage-status`) were read in the archived work log: argparse rejections of invocation shape (`qgate query` is not a verb; `architecture search --content --pattern`; the lessons-capture leaf's `get-plan-dir`) and one `manage-status` change-ledger guard refusal (`8 of 8 worktree-branch commits carry no kind=change ledger row`, after the branch was rewritten), which names its own remedy (`manage-change-ledger remap`). No new defect; the three messages are discarded.

## Routing and Merge Behavior

- Review: CodeRabbit. `runbook_lane review-gate --pr-number 530` re-read on 2026-11-22: `unresolved_review_threads=0`, `review_body_findings_total=2` (both `coderabbit,inline`), `handled=0`, `merge_allowed=false (body_findings_undisposed)`. The message carries `unknown` at all three keys (and `automatic-review` reported 1 comment found, 1 reviewed, 1 refused-structural), so there is nothing to compare, and the run's disposition record for the two body findings was not re-read: `review_body_findings_handled` is **not re-verified**. The bare `review-gate` call cannot see a run's own dispositions, so `handled=0` is no evidence of an undisposed finding.
- CI/merge: merged through the merge queue as `f00da06e` (`step.branch-cleanup.merge_mechanism=merge_queue`; `ci pr view`: merged); `cleanup_owed=false`.
- Sonar: the message states `sonar_open_new_issues=0`. The PR's own read is `undecidable` now (`count_status_reason: head_revision_unavailable`, `analysis_revision` `6ce6a953…`: the merged PR has no head to compare with), so **the `0` cannot be re-measured**; `gate-status` for PR 530 is `OK`; on the default branch no `java:S1845` is open and no BLOCKER is open. The raw PR issue list still names the 44 first-push issues (closed since). Not contradicted, not re-verified.
- The `landing-check` is incomplete at the three review numbers: recorded as an Open Defect, the landing is still reconciled.

## Reconciliation Actions

- [ ] row `status` → `shipped` — `orchestrator queue --transition PLAN-175 --status shipped` (a PARTIAL landing: the row records what landed; the unlanded scope is carried by the Open Defect and the follow-ups below)
- [ ] row `pr` stamped `530`
- [ ] row `landing` stamped `landings/PLAN-175.md`
- [ ] row `plan_marshall_plan_id` stamped `plan-175-sonar-standing-debt`
- [ ] epic.md: Open Defect for the partial landing, the incomplete `landing-check` and the three open `pm-api` issues
- [ ] five lessons promoted (`2026-11-22-16-001` to `-005`); three `script_failure` candidates discarded
- [ ] resume anchor and `queue-view.md` regenerated

## Follow-Ups

- **Operator decision (open):** the remaining scope is NOT carried by any queue row: P2 (the `pm-api` tests, Awaitility, `JsonTree.objectOrNull`; commits on the local branch `backup/plan-175-full`), P3 (`pm-mock-runtime`, `pm-web-app`, `pm-relay`, `pm-exec`), the `pm-operator` deliverable (after PLAN-174 lands) and the zero read; plus `S6878` at `HttpResponse.java:48`, which is fixable now and named by no one. Each could be staged as a plan on the operator's word.
- The spec's Done-when 1 (a carve-out for a contract-bound issue; the masked sites) and the `JsonTree.objectOrNull` route (`Optional` vs a documented nullable return) are decisions for the follow-up spec.
- Surfaces: P3 includes `pm-web-app/src/lib/api/client.ts`; PLAN-153 and PLAN-155 (landed) and PLAN-156/157 (staged) touch web views, so P3 must be paired by the disjointness gate when it is staged.

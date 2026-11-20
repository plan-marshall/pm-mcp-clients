# Mid-Flight Analysis: PLAN-168 — The refusal path told straight, exit codes that can fail, and the rest of PLAN-30's residue

epic: ui-improvements
workstream: WS-05
pr: #511 (**OPEN**, head `bd60be5e` at the analysis, 64 files, +1619/-529, 9 commits, `mergeStateStatus: DIRTY`)

> **Not a landing.** Written by `analyze` (inbox message `rb-168-cli-refusal-and-exit-residue-001`, `merge_state=open`, `deliverables_done=4` of 6, the lane's own words: D4 not met, D6's capture clause not met, the review gate `undecidable_read`) after verifying claims against ground truth. The row stays `running`; no ship semantics. The lane's final landing message, when it comes, extends this file. The lane's session was still working when the analysis ran: **the head had moved from `66081ca7` (the report) to `bd60be5e` (07:41Z)**, a commit that fixes the 24 open minors, so the report's "the 24 minors are open" is stale in code but not on GitHub (nothing on the PR answers them).

## Deliverable Fidelity vs Spec (at `bd60be5e`)

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 one refusal path | met | **met** | `OperatorRefusedException.Kind` (`RUNTIME`, `LOCAL`, `ROUTE_MISSING`, `UNREADABLE`); a local refusal renders as itself; the body's arguments are passed (no `{project_id}`) and sanitised; a non-JSON body is `UNREADABLE`, a plain body capped at 200 characters (`MAX_MESSAGE_LENGTH`); no audit-id promise in `03-dialogues.adoc` or `error.internal_fault.*` en and de (operator choice A2); the prefix is `operator.refusal.prefix`, no literal remains; unverified: the body field names `project_id`/`part` against the engine contract, what the real daemon answers for a missing plan; the cap does not apply to a JSON `message` or an argument; a code-less 404 is `ROUTE_MISSING` only when the body is not a JSON object (a heuristic; the PR changed fixtures from an empty body to JSON to fit it) |
| D2 every verb through one handling, to stderr | met | **met-modified** | `upgrade --check`, `--withdraw` and the release notes throw the refusal and exit 1; the machine-settings read throws; the eight listed stdout sites moved to `err` (`PlanCommand:186,213,307,1037`, `EpicCommand:123`, `ConfigCleanupCommand:307,424`), `EntryFlow:351` and `PmConsole:344` keep `out`; `ProjectCommand:127` refuted (it throws): holds; **a status without `pid` printing a line and exiting 0 is production code with no test** (the Done-when says `test-first`); **three `config` paths still print a refusal to `out` with the cause dropped** (`ConfigCommand.java:147-151`, `:1360-1363`, `:1501-1504`, re-read); the `Failed to …` family prints no cause any more; `enrolment.*.failed` still prints "HTTP {status}"; `RefusalStreamTest` covers 3 of the 8 moved sites (the rest in their command tests) |
| D3 dialogue lines | met | **met** | the part from the body or the unnamed text; no colon with an empty reason; the leave line without `epic.raise-scope`; `--remind never` its own text (**the exit code of every invalid cleanup duration changed 3 → 1, beyond the spec, to answer CodeRabbit**); choice B1: `work-actions.adoc:222` and `19-actions.adoc:190` now match the code, but `19-actions.adoc` states "the runtime lists a row as launchable only when the scope has room", a HYPOTHESIS of the spec that was never confirmed in `pm-mcp-core` |
| D4 the tests made able to fail | **not met** | **not met (confirmed)** | `ExitCodesTest`, `MockRuntimeEngineTest`, `OnboardingScenarioTest`, `PicocliLanguageTest` are not in the diff (re-read: no `ExitCodesTest` among the 64 files); of the six items of D4 only the `--remind` pin of D3 is in the PR |
| D5 display residue and a guard | met | **met-modified** | `plan report` in German (`planReportInGerman`), the twelve dead cleanup keys gone, the guard covers every `*Command` and sees a ternary, `String.format` and a literal across a line break; **the report's "no `MessageCatalogue.get(` without a locale" is false** (`PlanCommand.java:668,672`, a picocli converter, and about fifteen `format(key, …)` calls without a locale remain in `PlanCommand`, `ReportsCommand`, `ProjectsCommand`); **the guard has a real false negative**: a single-word literal passes (`isUserFacing("config")` is asserted false by design), so the very labels of the residue (`"outcome"`, `"reason"`, `"ran"` in `plan report`) would not have been flagged; a literal assigned to a variable, a receiver other than the listed names, and text passed to `new OperatorRefusedException("…")` are not seen |
| D6 design lines and captures | not fully met | **not met (partly)** | four documents corrected and read; the capture set `PLAN-168.txt` has 14 rows (`plan nope`, `epic nope`, `config cleanup --remind never --yes`, `answer Q-999 1`, `epic next`, en and de at **80x24 only**, plus four console rows): **no cleanup choice bar, no delete confirmation, no 120x30**; `ScreenCaptureIT` unchanged; the capture outputs are not committed, so "captures read" is not evidenced in the PR; the set is internally consistent |

`deliverables_done` by the plan's own rule (every Done-when clause met): the lane says 4; the leaf's reading gives **at most 2** (D1 and D3); D2's status-without-pid clause and D5's locale clause are not met as written.

## Analysis (one read-only leaf; the orchestrator re-read the points marked "re-read" at `bd60be5e`)

- **Re-read:** the head and its state (`bd60be5e`, `DIRTY`, 64 files, 9 commits); `ExitCodesTest` and the other D4 files absent from the file list; the three `ConfigCommand` paths on `out`; the guard's single-word rule; no test for a pid-less status; `76315c63` (PLAN-171) is **not** an ancestor of the head.
- **Not read by the leaf:** the lane's progress record and logs, the capture outputs (uncommitted, in the lane's worktree), the engine contract of `pm-mcp-core`, a CodeRabbit review of the new head (not posted yet), CI at the head, the full `EnrolHarnessCommand` and `LessonSourceCommand` diffs.

### The review surfaces and the merge state

- The only CodeRabbit review is of `bbefb54b`; its body (38,775 characters) holds 2 outside-diff minors, 24 minors and 2 nitpicks; **none has a GitHub thread**; the one inline Major has a thread, replied and resolved. The gate answers `undecidable_read` (the `Minor comments: 24` title, the known plan-marshall label gap), so the surfaces are read by hand.
- **The leaf read all 28 body items against `bd60be5e`:** the Major (control sequences in the fix line) is fixed with a test that can fail; of the 24 minors and 2 outside-diff items **every one is fixed in code at the head**, three of them real behaviour defects fixed **without a regression test** (a `{broken` 404 bypassing the route guard, a JSON message containing "Error:" or a tag being discarded, a `part` beyond `int` throwing `NumberFormatException`); one open nitpick-level residue (`AnswerRefusalTest.interactiveDependencyUnansweredAsksAgain` keeps a loose disjunct; `requiredKeys()` is itself a mirrored list). **None blocks the merge; no security hole is open at the head** (the path and ANSI injection through the fix line is closed).
- **The spec's merge rule needs the 28 body items answered on the pull request; no reply exists.** CodeRabbit's check is `IN_PROGRESS` on `bd60be5e`: a new review body will appear whose findings nobody has read.
- **The head is 7 commits behind `main` and conflicts** (`mergeStateStatus: DIRTY`): `merge-tree` reports one textual conflict in `ControlCommandTest.java` (PLAN-171 rewrote it to generated plan and hold ids; the PR changed two 404 fixtures from an empty body to `{"error":"scope_not_found"}` and one stdout-to-stderr assertion; the PR's JSON bodies must win inside main's generated-id version, or two tests fail). `ConfigCommandTest`, `PlanCommandTest`, `PmOperatorTest`, `EntryFlowTest` auto-merge; main's `configAspectReportsARefusedStep` (409, empty body, expects "The runtime refused" on `err`) should still hold by reading.

### Defects not in the lane's report

| Defect | File:line | Effect | Severity |
|--------|-----------|--------|----------|
| L3-1 | `LiteralTextGuardTest` `isUserFacing` | the guard can pass while wrong: single-word labels, variables, other receivers, exception messages (above) | **major** (a gate that can pass while wrong) |
| L3-2 | `ConfigCommand.java:147-151,1360-1363,1501-1504` (re-read) | `config` read failure, consents withdraw and documents update print a cause-less text to `out` and exit 1, against the stream rule the PR itself states | minor to moderate |
| L3-3 | the `Failed to …` family (`Failed to revoke.`, `…rename device.`, `…turn listener off.`, `…store {key}.`, `…remove {key}.`, `…read roles.`, `…qualify {role}.`, `…enrol harness`) | the operator no longer sees why (they lost the HTTP status and none goes through the renderer) | minor |
| L3-4 | `RuntimeCommand.java:132-139`; the release-notes 404 | a pid-less status has no test; a release-notes 404 used to say "No release notes found." and exit 0, now a generic refusal (the known code's message is not printed) | minor |
| L3-5 | `PlanCommandTest` (about `:1020`), `ReportsCommandTest` | `assertFalse(unknownText.contains("Plan not-a-plan not found."))` still reads the stdout capture while the refusal moved to `err` (vacuous); `contains("✓ N report")` matches singular and plural | minor |
| L3-6 | `OperatorRefusedExceptionTest` | three new tests with hand-written input and no `@EnableGeneratorController` | minor |
| L3-7 | `EpicCommand.java:123` vs `PlanCommand` | `epic <unknown>` renders `SCOPE_NOT_FOUND` from the status alone, `plan <unknown>` classifies the response: the two verbs differ on a code-less 404 | minor |
| L3-8 | `operator.dialogue.left_raise_scope` (de) | an infinitive against the English past tense | minor |
| L3-9 | the report's rule check | shell file reads, pipes, `perl -pi` in-place edits and a `python3` heredoc editing two files (all admitted); the PR contains **no** `.plan/` path and no temp file (re-read by the leaf); a stray `pm-operator/.plan/` directory is left uncommitted in the lane's worktree | process finding |
| L3-10 | the operator's free-text "try now" | the lane did not treat it as a selection and re-triggered the reviewer: correct | none |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | merge rule: every review body item answered on the PR | **not yet** | no reply to the 28 body items |
| spec Standing Constraints | the landing verdict per Done-when clause | the lane's 4 of 6 is generous | above |
| spec Standing Constraints | a changed exit code or visible format is named | **deviates** | the cleanup-duration exit 3 → 1, the plural `reports marked read` text and the `WebCommand` stream choice are not in the report |
| lane rules | file tools only, one command per call | **deviates, self-reported** | L3-9 |
| CLAUDE.md Code Standards | generated input, `@EnableGeneratorController` | partly | L3-6 |

## Metrics and Anomalies

- Tokens `unknown`, plugin version `unknown` (harness `opencode`, model `deepseek-v4.1-flash/high`, effort high, executor 0.1.2149): unobserved, not zero.
- Anomalies: CodeRabbit refused on quota on the first head and was re-triggered; two builds hit the 300 s floor and were re-run with `--timeout 1800`; the lane's report was filed before its last commit; the plan-less CI body slot `NO_PLAN` collision of the earlier runs is not repeated here.

## Reconciliation Actions

- [x] row `status` stays `running`; no `pr` or `landing` stamped (the PR is open)
- [x] inbox: `rb-168-cli-refusal-and-exit-residue-001` archived as consumed; the lane's final landing message is owed (`rb-168-…` stays in `awaiting_plans`)
- [x] epic.md: a Watch entry for this report; resume anchor, `queue-view.md`, decision log

## Follow-Ups

- **For the operator, one decision (see the Watch entry):** how #511 goes on. The lane stopped at the merge gate with D4 and D6's captures undone, the head behind `main` with one conflict, and the review body unanswered.
- Residue named, not staged: D4 (the `ExitCodesTest` table for every verb, 130, 3, 4, the three-line assertion, `RANDOM_ID` per test, the mock engine credential-path assertion, the unready-provider test, `PicocliLanguageTest`), D6's captures (the cleanup choice bar and delete confirmation, 120x30, the outputs committed), L3-1 to L3-8 above, the `enrolment.*` HTTP text, the locale-less `format(key, …)` calls, the three fixes without a regression test, the unconfirmed engine facts (body field names, "launchable only when the scope has room", the real 404 body).
- For plan-marshall: a report filed before the lane's last commit (the head moved after it); `review-gate` `undecidable_read` for `Minor comments: N` (now blocking a merge for the second time: #480, #511); a lane that reads files with shell tools and edits with `perl -pi`; the mailbox refused for plan ids with a leading digit.

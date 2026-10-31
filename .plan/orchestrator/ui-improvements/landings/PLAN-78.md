# Landing Analysis: PLAN-78 — Java: pm-api Tests on Generated Input Data

epic: ui-improvements
workstream: WS-01
pr: #288 (merge commit `1651554b3c984b4a7f8035dbfa4a81809675fc8f`, merge-queue merge onto `6372185a`)

> Landing record for one shipped plan. Source: inbox message `rb-78-cui-testing-api-003.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, merged state), analysed 2026-10-31 against the merged
> PR, the merge diff (`git show 1651554b`, 25 files, +1077/−571), the lane's progress record
> (`.plan/temp/rb-78-cui-testing-api/progress.md`) and the Sonar read. This is the third message of the plan: `-001`
> (PR #285, open) and `-002` (PR #288, open, blocked on the reviewer) were archived as observations; `-003` carries the
> landing. The per-head build logs and the self-review round are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 pm-api tests on generated input | shipped-as-specified | The merge diff is 25 files, every one under `pm-api/src/test/java/`, no production file and no fixture. `@EnableGeneratorController` stands on 25 classes at `1651554b` and on 0 at `7d7b4d71` (`git grep -l`), which agrees with the message's 0 → 25. The message accounts for the rest as 48 contract-only classes (73 = 25 + 48); the `**/*Test.java` glob finds 74 files at the merge because it also matches the support class `testsupport/BinaryUnderTest.java`, as the message says. The test count (660 → 689) and the green builds are the run's account, not re-run here. |

`deliverables_done=1` of 1 is consistent with the diff.

**Hypothesis of the spec** ("fewer than a third of the classes take incidental input is reported, not forced"): the share is
25 of 73, 34 %, so it sits just above the third; the message records it as confirmed ("well below 71") and reports the
count. Nothing was forced: the contract-only classes are listed with a reason in the progress record.

**Surface delta** (`inbox landing-check`, declared `pm-api/src/test/java/`, realized the 25 merge-diff files): declared 1
entry, realized 25, added 0, missing 0, `state: clean`. The `footprint_base_*` error in the output belongs to the base
ref I passed (`1651554b^`, rejected by the shape check); the realized set was given explicitly and checked against
`git show --name-only`. Unlike PLAN-82, the declaration held.

## Metrics and Anomalies

- Tokens: `unknown` (not measured on this harness). `plugin_version` `unknown`. The completeness check reports both keys
  missing; the known recurring opencode gap (Open Defect of 2026-10-27, recurrence recorded).
- Anomalies: (1) the run spans three messages and two PRs: #285 was closed unmerged (197 files, refused by the required
  reviewer, then quota refusals) and #288 was opened from the same branch; (2) the formatter collateral of the
  `pre-commit` gate (rewrites of main sources that drifted on `main`) was restored from `origin/main` twice, under the
  operator's recorded decision (two `git restore --source=origin/main`, once for `pm-api/src/main`, once for that plus
  `pm-mock-runtime/src/main`); (3) `main` moved three times during the run and the base was merged in, one conflict in
  `OverviewRecordsTest` resolved (the two trailing record components `ChangeSize`, `NeedsOperatorSummary` passed `null`);
  (4) one bound expiry at the applied 570 s with 1237 operator tests already green, continued with a raised bound;
  (5) the lane lists its own shell file-operation breaks (`wc -l`, `tail`, `git diff … | grep -c`, `git status --porcelain
  | head`, `git diff --stat … | tail`, `javap` over a `find`, `test -f`). Recorded as the run's account of its own rule
  breaks; not independently verified.
- Size: 25 files and 1077 added lines, under the 100-file limit of the required reviewer. The PR body still says "26 test
  files" (an earlier head); the merged diff has 25.
- Operator interaction in the run: five questions, all answered (formatter rewrite handling, the reviewer blocked at 197
  files, the retry refused on quota, "try again", and the merge word "Merge now under the standing authorization"). They
  are the operator's own words and are not re-asked here.

## Routing and Merge Behavior

- Review: CodeRabbit (required) refused every trigger across #285 and #288 on quota or size until the operator-directed
  retry after the wait window; the review then ran on head `0860ba13` (25 files) and filed two low findings (the
  decomposition fixture drew two plan ids independently; the rate-limit test did not exercise a zero `remaining`
  deterministically), both fixed in `11bec145`. The re-trigger on the final head `7854b8f4` was refused ("rate limit").
  Sourcery (optional) refused on "150,000 diff characters" each time and never gated. This closes the open decision of the
  Watches (PLAN-78 blocked on the required reviewer): the capacity cleared enough for one review, the qualifying review
  exists on a head of the PR, and the standing review authorization applied for the last head.
- CI/merge: PR #288 reads `merged` with merge commit `1651554b` (`ci pr view`), and `1651554b` is on `origin/main`
  (`git branch -r --contains`); its single parent is `6372185a`. The merge-queue run is the run's account.
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `7854b8f4`. **Not re-corroborated:**
  `read_open_issues` for PR 288 answers `count_status: undecidable`, reason `head_revision_unavailable`, analysis revision
  `7854b8f410b43e9cb31aa494557a50b703575bda` — the revision the lane read, so the claim is consistent with what Sonar still
  holds, but the count cannot be measured after the merge. Recorded as an unverified lead, not as a `0` (same as PLAN-82).

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-78 --status shipped`
- [x] row `pr` stamped — `288`
- [x] row `landing` stamped — `landings/PLAN-78.md`
- [x] row `plan_marshall_plan_id` stamped — `78-cui-testing-api` (from the sender `rb-78-cui-testing-api`)
- [x] epic.md narrative reconciled (Decisions entry, Open Defect recurrence, the Watches below)
- [x] the Watch "PLAN-78 blocked on the required reviewer" closed; the "over 100 files" Watch carries the result
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **`main`'s formatting drift remains.** The message says the `pre-commit` gate still rewrites 170-odd main sources and the
  lane restored them from `origin/main` twice; nobody has cleaned them on `main`. PLAN-87 (running) and PLAN-79 meet the
  same rewrite: their specs already carry the instruction to restore it. The operator's decision stays open (a small chore
  commit on `main`, or each plan restoring).
- **PLAN-79** (`pm-operator`, about 116 test classes per the epic's Watch) is the remaining conversion, exposed to the
  100-file limit and to the reviewer quota that kept this run waiting (the progress record runs from about 10:55 to
  20:21 across the two PRs); its spec says to keep the PR under 100 files or split. It stays held while the terminal
  PLAN-17 runs.
- **No staged spec needs a re-read for this landing:** none names any of the 25 converted classes (checked with
  `grep -w` of the class names over `plans/`). PLAN-34 declares a new `api/display/` test directory and PLAN-45 declares
  `ErrorTextsTest`, neither touched.
- **Evidence left by the run:** its worktree and branch are removed (`cleanup_owed=false`); the message says `main` of the
  main checkout was switched and pulled to `1651554b`, which agrees with the checkout's log.

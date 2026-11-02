# Landing Analysis: PLAN-87 — Java: pm-mock-runtime Tests on Generated Input Data

epic: ui-improvements
workstream: WS-01
pr: #293 (merge commit `418579a0b004968033bb7360c0e731d50b28fb6a`, merge-queue merge onto `9890d74b`)

> Landing record for one shipped plan. Source: inbox message `rb-87-cui-testing-mock-runtime-001.md` (runbook lane,
> harness `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, written 2026-11-01T05:30Z), analysed
> 2026-11-01 against the merged PR (`ci pr view`), the merge diff (`git show 418579a0`, 40 files, +790/−420) and the Sonar
> read. The per-head test runs and the three self-review rounds are the run's account and are not re-run here.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 pm-mock-runtime tests on generated input | shipped-as-specified | The merge diff is 40 files, every one under `pm-mock-runtime/src/test/java/`, no production file and no fixture. `@EnableGeneratorController` stands on 40 classes at `418579a0` and on 0 at `1651554b` (`git grep -l`), which agrees with the message's 0 → 40. The tree holds 45 test classes at the merge (the spec's re-count: 45); the message lists the remaining 5 as input-free, each with a reason. The test count (483 → 484) and the green builds are the run's account, not re-run here. |

`deliverables_done=1` of 1 is consistent with the diff. The scenario fixtures (`*.json`) are untouched, as the Standing
Constraints require.

**Hypothesis of the spec** ("fewer than a third of the classes take incidental input is reported, not forced"): the message
**refutes** it — 40 of 45 classes (89 %) take at least one arbitrary value, against 25 of 73 (34 %) in `pm-api`. The five
input-free classes are `StandardScenariosTest`, `scenario/StateVocabularyScenarioTest`, `junit5/MockRuntimeExtensionTest`,
`junit5/MockRuntimeExtensionLoggingTest` (the `LogAsserts` test, kept as written) and `cli/StartStopScriptsTest`, each with a
stated reason. The spec's hypothesis is reported, not forced either way.

**Surface delta** (`inbox landing-check`, declared `pm-mock-runtime/src/test/java/`, realized the 40 merge-diff files):
declared 1 entry, realized 40, added 0, missing 0, `state: clean`. The declaration held.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown` (completeness check: both missing, the known opencode gap, recurrence
  recorded).
- Anomalies: (1) the `-Ppre-commit` gate rewrote 178 unrelated main sources in the run's worktree; the lane asked the operator,
  who answered (free text) "land that changes in a separate pr with skip-bot-review as pr label", executed as **PR #294**
  (open, see Follow-Ups); (2) the lane restored the rewrite with `git checkout HEAD -- pm-api/src/main
  pm-mock-runtime/src/main/java`, which it names as a sanctioned exception to its own rule `git checkout -- <path>`, and once
  restored the same paths from the rewrite branch, which staged the rewrite and needed a second `git checkout HEAD --` — the
  run calls it "self-inflicted"; (3) one bound expiry at 520 s on the first full `verify -Ppre-commit`, re-submitted once
  with `--timeout 1500`; (4) a relative `--project-dir` failed at daemon spawn (empty log), corrected to an absolute one.
  Recorded as the run's account; not independently verified.
- **Merge without a separate operator word:** the message lists no merge question and applies the standing authorization
  (qualifying review on `80634078`, the head before the merged head `7f7249fc`; one re-trigger after the last push refused
  on quota). Same standing practice as PLAN-16 (see its landing).
- Size: 40 files, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) refused four triggers (21:18Z to 01:55Z), accepted at 03:28Z and reviewed
  `5f7792e2` (12 minor findings, all fixed in `5965c0d3`) and `80634078` (3 nitpicks, fixed in `7f7249fc`); the final head
  `7f7249fc` was refused on quota. Sourcery (optional) refused every trigger above 150,000 diff characters.
- CI/merge: PR #293 reads `merged`, merge commit `418579a0`, on `origin/main` (checked with `git branch -r --contains`);
  its single parent is `9890d74b` (the PLAN-16 merge).
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `7f7249fc` (7 `java:S1117` issues at an
  earlier head, fixed in `5f7792e2`). **Not re-corroborated:** `read_open_issues` for PR 293 answers `count_status:
  undecidable`, reason `head_revision_unavailable`, analysis revision `7f7249fc797cc2987302675ba5de476494b7c618` — the
  revision the lane read. Recorded as an unverified lead, not as a `0`.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-87 --status shipped`
- [x] row `pr` stamped — `293`
- [x] row `landing` stamped — `landings/PLAN-87.md`
- [x] row `plan_marshall_plan_id` stamped — `87-cui-testing-mock-runtime` (from the sender `rb-87-cui-testing-mock-runtime`)
- [x] epic.md narrative reconciled (Open Defect recurrence, Decisions, Watches)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **PR #294 is open and needs the operator's word.** `style: apply the pre-commit import grouping to main sources` (head
  `chore/pre-commit-import-grouping`): the rewrite of 177 `pm-api` main sources and one `pm-mock-runtime` main source that
  the pre-commit gate produces. It answers the epic's open question about the drift on `main`: until it merges, every
  `-Ppre-commit` run keeps producing the same rewrite. It is not a plan of this epic; the operator opened the route.
- **PLAN-79 is the last conversion** (`pm-operator`, about 116 classes). The share of classes that take generated input
  differs by module (40 of 45 here, 25 of 73 in `pm-api`), so the spec's "well below" estimate for `pm-operator` is a
  hypothesis to check at its outline, not a figure to carry over. The message records the discriminator this lane used
  (generate the values the production resolves or echoes; keep contract values literal).
- **No staged spec names a file this plan changed** (path-level match of the 40 files against every staged spec's Expected
  Surface: none).
- **Pre-existing, outside this plan:** `pm-operator verify -Pintegration-tests` shows three transcript-golden mismatches
  (`status.transcript`, `start-plan.transcript`, `start-plan-no-input.transcript`) that the message says reproduce on the
  untouched `main` checkout. Not verified here. `status.transcript` is one of the three `pm-operator` files PLAN-82 touched; carried as a Watch.

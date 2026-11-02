# Landing Analysis: PLAN-17 — Terminal: Confirm the Runtime Acts

epic: ui-improvements
workstream: WS-04
pr: #292 (merge commit `66c1495a2553f5b6e2a5cc2c118a59720f59dee1`, merge-queue merge onto `418579a0`)

> Landing record for one shipped plan. Source: inbox message `rb-17-cli-runtime-act-confirm-001.md` (runbook lane, harness
> `opencode`, model `deepseek-v4.1-flash/high`, executor `0.1.2036`, written 2026-11-01T10:51Z), analysed 2026-11-01 against
> the merged PR (`ci pr view`), the merge diff (`git show 66c1495a`, 16 files, +1225/−84) and the Sonar read. The per-head
> test runs, the three self-review rounds, the terminal captures and the browser check are the run's account and are not
> re-run here. PR #286 was closed unmerged earlier (the lane's reviewer policy closes and reopens to re-trigger the
> required reviewer); #292 carries the landing.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 Stop, restart, upgrade dialogues | shipped-as-specified (the run's account for the tests) | `RuntimeActDialogue.java` (322 lines, new) and `RuntimeCommand.java` (+158) are in the merge diff. `RuntimeStopDialogueIT` is in the diff and the `@Disabled("PLAN-17-cli-runtime-act-confirm …")` it carried at `418579a0` is gone at `66c1495a` (`git show` / `git grep`), as the spec's Standing Constraints require. The failing run of the test-first order (`e83a9ee2`, all 5 cases) is the run's account. |
| D2 Script options | shipped-as-specified (the run's account for the tests) | `--when-done` and `--now` stand on all three acts in `RuntimeCommand.java` (stop, restart, upgrade; `git grep` at the merge). `PmOperatorTest` (+165) and the new `RuntimeActDialogueTest` (360 lines) are in the diff. The exit-code claim (`--no-input` without an answer exits 3) and the counts (8 + 16 cases, 2527 and 572 tests) are the run's account. The terminal capture `17-cli-runtime-act-confirm.txt` (43 lines) is in the diff; the browser regression (en/de × light/dark × 2 sizes) is the run's account, screenshots not opened here. |

`deliverables_done=2` of 2 is consistent with the diff.

**Surface delta** (`inbox landing-check`, declared = the 9 backticked paths of the spec's Expected Surface, realized = the 16
merge-diff files): `expansion_detected`, 9 added, 2 declared and untouched.

- **Added, never declared (9):** `doc/design/shared/system-texts.adoc`, `doc/design/terminal-client/01-commands.adoc`,
  `RuntimeActDialogue.java` (the new dialogue class), `StartPrompt.java`, `dialogue/ChoicePrompt.java` (the interrupt mechanism,
  see below), and four tests / captures: `PmOperatorTest`, `RuntimeActDialogueTest`, `ChoicePromptTest`,
  `17-cli-runtime-act-confirm.txt`. The tests and the capture follow from the deliverables; the two design documents and the
  three production files are drift of the declaration.
- **Declared, untouched (2):** `DrainCommand.java` and `system/SystemLevel.java`, both in the spec's Expected Surface; the lane did not touch
  either, and its message gives no reason. Whether `drain` should ask is not decided by this landing.
- `ChoicePrompt` changed (+67) beyond the spec: it now throws `ChoicePrompt.InterruptException` and `StartPrompt` carries
  `EXIT_INTERRUPTED` (130 for Ctrl+C). That is production behaviour of a shared prompt used by other verbs.

## Metrics and Anomalies

- Tokens: `unknown`; `plugin_version` `unknown` (completeness check: both missing, the known opencode gap, recurrence recorded).
- Anomalies, the run's account: (1) every routed build failed in about 1 s with `execution_failed` and an empty log (the
  daemon cannot start `mvn`), so every build ran `--execution-mode in_process`; recurrence of the build server's bare-mvn failure
  Watch (PLAN-22, PLAN-14); (2) PR #286 closed unmerged and #292 opened from the same branch under the reviewer quota policy;
  (3) the lane's tool miscount: `ci pr comments` counts issue comments as unresolved and names no thread ids, so the lane
  believed 7 review threads were open and asked the operator; GraphQL showed 14 threads, all resolved. The lane called
  `gh api graphql` directly to enumerate them, a break of its own rule "CI actions go through `ci`", which it names; (4) the
  lane lists its other rule breaks (shell `grep` ×2, `git grep`, a `perl -pi` edit, a `cp`, a two-command shell call, reads of
  shell-output files outside the repository root). Recorded as the run's account; not independently verified.
- **Merge without a separate operator word:** the lane asked the operator one question (merge now / wait / stop); the answer
  was free text ("fix all and check again"), which the lane read as an instruction to fix and re-check, not as permission
  to merge. The merge then proceeded on the standing review authorization once its conditions held. Same practice as PLAN-16
  and PLAN-87 (see their landings); the operator may want to confirm that the authorization covers a merge after a question
  that did not grant it.
- Size: 16 files, under the 100-file limit of the required reviewer.

## Routing and Merge Behavior

- Review: CodeRabbit (required, quota-limited) reviewed `54f21b40`, `62490b7a`, `ac62e64f`, `b28e0187` and `a67ef02e`; the
  last two heads (`230fe51e` merged, and the re-trigger of 10:28Z) were refused on quota. The lane records every finding of
  the reviews as fixed in `30ab85ea` / `230fe51e` or answered, and one answered-not-fixed (Ctrl+C in other verbs, below).
  Sourcery (optional) refused on its hard quota and never gated.
- CI/merge: PR #292 reads `merged`, merge commit `66c1495a` (`ci pr view`), which is on `origin/main` (`git branch -r
  --contains`); its single parent is `418579a0` (the PLAN-87 merge). The merge-queue run id is the run's account.
- Sonar: the landing claims `open_issue_count=0`, `open_issue_rules=none`, read at `230fe51e`. **Not re-corroborated:**
  `read_open_issues` for PR 292 answers `count_status: undecidable`, reason `head_revision_unavailable`, analysis revision
  `230fe51ee0acc9f47a28c2292a626b340d1531cd`, the revision the lane read. Recorded as an unverified lead, not as a `0`
  (same as PLAN-78, 16, 87).

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-17 --status shipped`
- [x] row `pr` stamped — `292`
- [x] row `landing` stamped — `landings/PLAN-17.md`
- [x] row `plan_marshall_plan_id` stamped — `17-cli-runtime-act-confirm` (from the sender `rb-17-cli-runtime-act-confirm`)
- [x] epic.md narrative reconciled (Open Defect recurrence, Decisions, Watches)
- [x] resume anchor updated; `queue-view.md` regenerated

## Follow-Ups

- **Terminal plans PLAN-21, 23, 26, 30, 36, 42, 48 and 84 name a file this plan changed** (path-level match of the 16 files
  against every staged spec): `operator.{en,de}.properties` (PLAN-21, 23, 30, 36, 42, 84), `system.{en,de}.properties`
  (PLAN-23, 26), `01-commands.adoc` (PLAN-26, 30, 36, 42, 48, 84), `25-system.adoc` (PLAN-26), `RuntimeCommand.java`
  (PLAN-30). Re-read each at HEAD `66c1495a` before emit. PLAN-23 also reads `ChoicePrompt`.
- **PLAN-79 is no longer held by a terminal plan** (PLAN-17 was the one terminal plan running). Its spec says to keep the pull
  request under 100 files or split; check the reviewer capacity and the PR #294 question at its outline.
- **Residue the run named, carried as a Watch:** Ctrl+C still exits 0 in eight non-runtime handlers (`WorkActionDialogue`,
  `ReviewDialogue`, `HoldDialogue`, `ProjectCommand`, `PlanCommand`, `OnboardCommand`, `ActionSupport` ×2); the mechanism
  exists now and the handlers need the same catch. No staged plan owns it. Two pre-existing web defects in
  `SystemView.svelte` (the raw state id `waiting for_you` at line 858 from `replace('_', ' ')` without `/g`; an English alert
  on the German `running-work` failure page at line 147), also unowned: PLAN-43 and 47 declare `SystemView` and are the
  natural owners, to be checked at their re-read. These locations are the run's account, not read here.
- **`ci pr comments` unresolved count is unreliable** where review threads carry no inline comments (counts issue comments,
  names no thread ids). A provider defect for the plan-marshall side, not for this epic.
- **`DrainCommand`** was declared and untouched: whether `drain` needs the same confirmation is open.

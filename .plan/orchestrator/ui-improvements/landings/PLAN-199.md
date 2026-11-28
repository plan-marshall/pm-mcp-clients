# PLAN-199 landing record

epic: ui-improvements

**Plan:** `rb-199-remove-predictions`, on the runbook lane.
- Harness `antigravity`; model, effort and `total_tokens` are `unknown`.
- `plugin_version=0.1.0` is the antigravity placeholder; the installed version is 0.1.2183. The executor is 0.1.2183.

**PR:** #609, squash `d25d1d76` (37 files, +301/−1608) on `5737753`, merged 2026-11-28T13:33:49Z.
- Merge group run `46150257806` succeeded.
- Head `fdf1d52a` is a merge of `origin/main`.
- Commits: `e8d571ef` D1, `38bfa86b` D2, `c2704945` D3, `e839142a` formatting, `552e4f83` self-review fixes.

**Analysed:** 2026-11-28, by a sub-agent in three layers, by reading only (no build). The sub-agent opened the red-run logs: they still exist (`~/.plan-marshall/marshalld/job-logs/`, `.plan/local/plans/NO_PLAN/build-results/`), and every named red run was confirmed.

Message `rb-199-remove-predictions-001` (2026-11-28T13:52:46Z):
- `merge_state=merged`, `deliverables_done=3` of 3.
- 0 unresolved threads; 0 review-body findings.
- `sonar_open_new_issues=0`, `self-review: clear`.

**Sonar.**
- PR 609: gate OK, 0 new issues, 0 hotspots, coverage on new code 87.5 %, duplication 0.0 %. The open-issue count read now is `undecidable` (`head_revision_unavailable`). The PR comment and the message both say 0.
- Default branch, read 2026-11-28 after the `main` task `AaIifh7XrEcOj1c1yl54` (14:10:20Z, after this merge): gate OK, coverage on new code 90.3 %, `count_status: confirmed`, **3 open**. They are the three PLAN-181 keys.

**CodeRabbit.**
- Two triggers were refused on the rate limit.
- The trigger at 13:06:25Z got "Review finished" at 13:06:31Z, and the release notes are in the PR body. No GitHub review object exists.
- The message's "13:13:08Z covering 552e4f83" cannot be confirmed. A finished review with 0 findings is plausible, so the one-review gate holds.

## Layer 1: specified (the body binds; the spec has no later section)

| Done-when | Verdict |
|---|---|
| D1.1 `/metrics/oversize` answers 404 in both scenarios; `intent-oversize` resolves to nothing (test-first) | met (`IntentScenarioTest.java:291-306`; red run job-log `694ca281…`) |
| D1.2 the setup tab has no `project-oversize` section and sends no request (test-first) | met (`ProjectExplorerView.test.ts:900-929`; red run `maven-2026-11-27-231947.log`) |
| D1.3 the removal grep has **no hit**, and the PR shows it | **partly**. Hits remain in the negative controls (`IntentScenarioTest.java:292-305`, `ProjectExplorerView.test.ts:900,928`), and this Done-when allows no control exception, unlike D3.3. The PR states the result in prose and does not show the grep. There is no hit outside tests |
| D2.1 the enum is exactly the five, in order; old and unknown values are refused (test-first) | met (`OffTrackSignal.java:17-67`, `ProcessTreeTest.java:657-673`; red run `f8e6f414…`) |
| D2.2 the four retired keys are absent; the three new words are present, en and de (mutation-after) | met (`PlanViewCatalogueTest.java:235-258`; mutation red `2b09fb5c…`) |
| D2.3 `edit_churn` renders its word; the mock's `agent_started` task renders in the terminal plan view (test-first) | met (`PlanViewFormattingTest.java:142-151`, `WorkerActivityLevelTest.java:204-225`; red run `a8919c66…`). The terminal test covers the worker-activity level, not the tree row |
| D3.1 `fromJson` refuses `added_dependencies`; `toMap` carries exactly 4 keys (test-first) | met (`IntentContractTest.java:168-179`; red run `80c63eac…`) |
| D3.2 the mock outline carries no `added_dependencies`; web `getOutline` drops the key (test-first) | met (`IntentScenarioTest.java:196-210`, `client.test.ts:2737-2753`; red runs verified) |
| D3.3 the second grep has no hit outside the controls; the PR shows it | met (controls only; stated in prose) |
| D2 "What": "the PR states the chosen words for the operator" | **not met**: the PR body names the keys only |

**True count:** 3 of 3 deliverables; 8 of 9 Done-whens met, 1 partly (D1.3).

The words now on screen:
- en: "failing again", "edit churn", "agent started";
- de: "scheitert erneut", "wiederholte Änderung", "Agent gestartet".

They are the design table `plan-view.adoc:518-543`.

## Layer 2: implemented

37 files. All 33 Expected Surface files were touched. Four files outside the surface, each explained:
- `doc/plans/README.adoc` (the optional D1 item);
- `PlanViewCatalogueTest.java` (the D2.2 test);
- `WorkerActivityLevelTest.java` (the D2.3 test);
- `OutlinePanel.svelte` (a stale JSDoc, fixed by the self-review).

Every named SHA exists.

**Stale or weak claims:**
1. `plugin_version=0.1.0` (the antigravity placeholder).
2. CodeRabbit "reviewed cleanly at 13:13:08Z" cannot be confirmed (see above).
3. D1.3 is reported `met`; by its literal text it is partly met.
4. The build gate at the final heads is weaker than reported:
   - `verify -Ppre-commit` passed only at `c2704945`. At `552e4f83` it hit the 1012 s bound and was replaced by a plain `verify`.
   - The e2e ran only at `e839142a`, with 2 failures (`designer.spec.js:272`, `chrome.spec.js:31`). They were called unrelated but not proven pre-existing, and the e2e was not re-run after `552e4f83` or the main merge. `designer.spec.js:272` is the known red of PLAN-178 #557, which has no owner. `chrome.spec.js:31` is new to the ledger.
5. The mailbox reads used the plan id `199-remove-predictions`, while the `sender_id` is `rb-199-remove-predictions`. The `invalid_target_plan` error came from the wrong id; it is not a tool defect.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker and no major. Clean:
- No leftover of a removed name outside the negative controls, in code, packs, scenarios, e2e or `doc/`.
- Unknown values are refused the PLAN-191 way: a copy of `EpicWaitReason`. The wire `OffTrackInfo.signal` stays a `String`.
- en/de parity: 3 keys removed and 3 added in each of `common` and `plan_view`, matching the design table.
- `agent_started` is wired end to end in the terminal, through the mock task `running-work.json:1522,1589` (`storage-v2` TASK-002, action `restart`).
- The outline side is consistent with the engine, which refuses the member (`OutlineCodec.java:64`).

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | The new design table makes a test read more of the design. `PlanViewCatalogueTest.designRows()` reads every `plan.*` row of `plan-view.adoc` through `SpecificationTables`, now including the new `plan.signal.*` rows. That is the pattern PLAN-198 removes; no new reader was added | `PlanViewCatalogueTest.java:40,49-52,77`; `plan-view.adoc:518-541` |
| L3-2 | minor | The `state.signal.*` words are covered in the design by a sentence ("the same word per value"), not by rows, against the spec's rule "a word comes from a design text row" | `plan-view.adoc:543`; `common.{en,de}:369-371` |
| L3-3 | minor | The engine's lag now reaches the screen. pm-mcp-core `11ec6b72` still has `SignalName.OVER_SIZE`/`OUTSIDE_FOOTPRINT` (`QueueTaskRecord.java:414,416`; `RoleArtifact.java:192`). A real daemon sending them renders the raw wire word, because the keys are gone. The spec accepts this as a pm-mcp-core finding | pm-mcp-core |
| L3-4 | minor | The e2e was not green at the final head and pre-commit was not re-proven at it (L2 item 4) | message §1, §9 |
| L3-5 | minor | The PR does not state the chosen en/de words, as D2 asks | PR #609 body |
| L3-6 | info | New tests use hand-written inputs (`"p"`, `"s"`, `"unknown"`) where `Generators` are the norm. The wire names stay literal, which is correct | `IntentContractTest.java:171-175`, `ProcessTreeTest.java:665,670` |
| L3-7 | info | `parse(String)` takes a non-null parameter and the test passes `null`. Harmless, because `api.process` has no `@NullMarked`, which is an older gap | `ProcessTreeTest.java:671`; `process/package-info.java` |
| L3-8 | info | A stale display name ("…dependencies…") now means `depends_on` only | `IntentScenarioTest.java:175` |
| L3-9 | info | No web test uses a new signal word; PLAN-190 D2 owns that | web |

## Residue

**Shared code files:**
- PLAN-62 (running): `running-work.json` and `PlanView.test.ts` (2), plus `plan-view.adoc`. The regions are disjoint.
- **PLAN-198** (its PR **#611 is open**, based on `bdaf6f26`, before this merge):
  - Shared code: `PlanViewCatalogueTest.java` (1), which its branch rewrites while deleting `SpecificationTables`.
  - Shared doc: `plan-view.adoc`, where its branch deletes the design tables.
  - It must merge `main`, keep `signalKeysReflectClosedSetOfFive`, and turn this plan's new `plan.signal.*` table (`plan-view.adoc:518-541`) into its link-to-catalogue form. Otherwise a hand-written table survives against ADR 002.
  - A textual conflict is likely.
- PLAN-136: `client.ts`, `client.test.ts`, `PlanView.test.ts` (3). Its `client.ts` lines have shifted by about 100.
- PLAN-138 and PLAN-200: none.

**Followers:**
- PLAN-179 (staged): unblocked, but it needs a re-ground. Its over-size bullets and hypotheses are moot, and its surface lines are stale.
- PLAN-190 (staged): D2 is unblocked. The fixture is `storage-v2` TASK-002 `agent_started`.
- PLAN-139 (staged): the overlap note `:175` is stale; drop it at its next re-ground.
- PLAN-29 (parked): its CC-V-02/V-03 dependency on the signal package is met; only the phase-set change remains open.

**Routes:**
- To pm-mcp-core: `SignalName` and `RoleArtifact` still carry the retired signals (L3-3).
- To the ledger: `chrome.spec.js:31` is a second e2e red on main besides `designer.spec.js:264/272`. CI runs no Playwright. It needs an owner.

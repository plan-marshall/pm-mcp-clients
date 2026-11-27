# PLAN-194 landing record

epic: ui-improvements
plan: rb-194-mock-chat-fixtures-and-residue (runbook lane, harness antigravity, model and effort `unknown`; `total_tokens` `unknown`; executor 0.1.2183)
pr: #589, squash `4fde136a` (18 files, +4381/−170; 19 branch commits, head `2eef1264`), merge queue, merged 2026-11-26T11:39:04Z
analysed: 2026-11-27, by sub-agent in three layers, by reading only (no build). The orchestrator re-read the following at `be1affd1`:
- the `pr_merged` #198 line of C-0006 (`running-work.json:2962-2966`) against `session-log`'s PR 198 (`:545-547`);
- the lapse `activityChanged` (`ChatRegistry.java:1436`);
- the commit `464ef6ab`.

Message `rb-194-mock-chat-fixtures-and-residue-001` (revision 0, 2026-11-26T11:40:43Z):
- `merge_state=merged`, `deliverables_done=3` of 5
- 0 unresolved threads, 22/22 review findings disposed
- `sonar_open_new_issues=0`, `self-review: clear`

One operator question: CI was red on `ChatTextsCatalogueTest`. The answer was "recheck main and rebase". The run merged `origin/main` (`1493aa14`) instead, citing the no-rebase rule, and disclosed it. Binding scope: the spec body (no later section).

**Merge corroborated.** `gh pr view 589`: merged 11:39:04Z, merge commit `4fde136a351e6cac0affe40eb81b246334e40e5f`. Diff: `pm-api` (2 files) and `pm-mock-runtime` (16 files), no `doc/` file, no web file, so no e2e run was owed.

**CI.** 14 checks pass and 6 were skipped (CodeRabbit, Sourcery, deploy-snapshot ×2, covering-run-check, auto-merge). Nothing failed. Review threads: 18 threads, all resolved, of which 7 were auto-resolved by CodeRabbit with no reply. Sourcery refused: the diff is over 150 k characters.

**Sonar.**
- PR 589: 0 issues; gate OK (new coverage 86.6 %).
- Default branch, read by the orchestrator 2026-11-27 after the `main` task `AaIXuNmvALT5l7RQ_5TB` (submitted 11:58:40Z, SUCCESS, after this merge at 11:39:04Z):
  - gate OK, `new_coverage` 90.2 %, `count_status: confirmed`;
  - **3 open**, the three PLAN-181 keys only (`AaFGfsYupdXNQ-N4ArY-`, `AaGRMDtGxhXuOwwhaS0K`, `AaEcTdY0MJNX7b6L9QAy`).

**SNAPSHOT deploy of `4fde136a`:** `unknown`.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 fixtures | met, with fixture defect L3-1. `running-work.json`:<br>- the running `storage-v2` with process and task activity (`:1537-1666`);<br>- decision chat C-0005 `fix-relay/Q-007` with an open revision (`:2750`);<br>- plan chat C-0006 with notice lines (`:2857`);<br>- entry chat C-0007 `ent-running` (`:2974`);<br>- epic C-0002 knows rows, items and a notice (`:2547-2633`).<br>`fully-onboarded.json` has the same set. The archive chat C-0003 `docs-typo` already existed before this merge. Test: `ChatFixtureScenarioTest.java:62-96`. The test-first log `171912` fits an earlier version of the test. No loader change. |
| D2 apply/refuse | partly. Implemented:<br>- `ChatProposal.isOpen(boolean)` (`ChatProposal.java:107-114`);<br>- `applyChatProposal` applies `after` (`MockRuntimeEngine.java:1066-1251`);<br>- a queued revision is refused `conflict` (`ChatRegistry.java:2073-2083,1912-1928`).<br>Tests: `ChatProposalsTest`, `ChatProposalRefusalTest.java:67-110` (3 causes), `ChatBroadcastTest.java:301`.<br>**Fail-first only for `pm-api`** (log `175048`). The runtime suites have none, which the run disclosed. |
| D3 window/narrative | partly. Implemented:<br>- `revisionWindowOpen` reads `planActivities` (`MockRuntimeEngine.java:1336-1338`);<br>- pausing vs paused (`ChatRegistry.java:1342-1348`);<br>- the infeasible outcome (`:1258-1263`);<br>- `planFor` checks `DecisionScope.PLAN` (`MockRuntimeEngine.java:1381-1391`), but the prefix fallback remains (`:1392-1402`).<br>The sanity-check phase is stated absent. Tests: `ChatWindowAndNarrativeTruthTest`.<br>**Fail-first only for the queued-revision gap** (log `221745`). The first four items have none (disclosed). |
| D4 race/order | met. `ChatRegistryRaceTest.java:76-149`; `ChatRevisionOrderTest` (5 outcomes, reject after each refusal, other subject). Mutation logs `202343`, `195349`. The first is killed by an NPE, not by the resurrection assertion (L3-7). |
| D5 narrative | met, with quality gaps L3-3, L3-4, L3-6. Lines in `ChatRegistry.java`:<br>- `pr_merged` `:1129-1157`;<br>- `row_*` `:1159-1178`;<br>- `auto_changed` `:1180`;<br>- `source_*` `:1187-1213`;<br>- `instance.changed` `:1491-1499`.<br>Elsewhere: knows rows and items (`MockRuntimeEngine.java:1273-1296`), H-1 removed (`ChatScripts.java:147-149`).<br>Pins: `ChatNarrativeCompletenessTest:95-363`. Fail-first log `203307`: 8 of 9 fail, some on a fixture 404 rather than on missing behaviour. |
| Gates | partly. Whole-tree `verify` was green at `ad701ee4` and at `2eef1264` (before the merge, not after). `-Ppre-commit,coverage` ran only as `-pl pm-api,pm-mock-runtime`; there was no whole-tree pre-commit and none at the head, against the Standing Constraints. |

True count: 3 of 5 (D1, D4, D5). This matches the message.

## Layer 2: implemented

All 18 files trace to a deliverable. Outside the Expected Surface but explained:
- `ChatFacts.java` and `StartRegistry.java` (D5, plus a CodeRabbit lock-order fix);
- `ChatReadModelTest.java`;
- six new test classes.

The decomposition-lock rework in `MockRuntimeEngine` (`handleCheck`/`Instruct`/`Commit` broadcast outside `decompositionLock`) traces only to CodeRabbit inline-8 and is explained only in its thread.

**Stale or false claims:**
1. Commit "`464ef46b`" does not exist; it is `464ef6ab`.
2. The message's job id is truncated to 31 characters; the PR body has the right one.
3. "All 20 GitHub checks green": 14 pass, 6 were skipped.
4. "D1 extends … an ended plan archive chat (docs-typo)": C-0003 already existed.
5. The PR body states no thread or review-body counts.
6. "22/22 disposed": 7 threads were auto-resolved with no reply, and the 4 review-body items got no reply on GitHub.
7. "`planFor` … instead of prefix matching": the fallback remains.
8. "Final post-merge verify": it ran after merging `main` into the branch, before the PR merge.
9. "3 mailbox reads": the message lists 2.
10. "Residue: none on the shipped code": false (Layer 3).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. No new message key (parity trivial), no generic catch, no `System.out`, no Mockito or Hamcrest.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | The running `storage-v2`'s plan chat C-0006 narrates `chat.line.pr_merged` #198, but PR 198 belongs to the still-running `session-log`. The fixture contradicts itself, and PLAN-139 and PLAN-136 test against it. | `running-work.json:2962-2966` vs `:545-547`; `fully-onboarded.json:2977` vs `:270` |
| L3-2 | minor | A lapse at `scope.ended` sends `activity.changed` with `activity: "ended"`. The catalogue allows `running`, `pausing` and `paused` only. A test pins `"ended"`. | `ChatRegistry.java:1436`; `MockRuntimeEngine.java:1358-1364,2673-2674`; `06-event-catalogue.adoc:64`; `ChatWindowAndNarrativeTruthTest.java` ~`:120` |
| L3-3 | minor | `row_landed`/`row_failed` fire on `landed`/`failed`, which are not in the `WorkRow` vocabulary, so a contract row (`shipped`, `returned`, `abandoned`) narrates nothing. `row_failed`'s `{outcome}` falls back to the row status, which the design forbids. The test feeds those off-contract values. | `ChatRegistry.java:1159-1177`; `WorkRow.java:26-27`; `work-actions-chat-texts.adoc:17`; `ChatNarrativeCompletenessTest.java:282-284` |
| L3-4 | minor | Source lines name a source inconsistently: `added` prefers `kind`, removed and flagged prefer `source`. The label can take `title`, though pm-mcp-core shows no untrusted text and names a flagged source by id and kind. The flag reason falls back to the literals `verdict`/`flagged`. The lines are reachable only through `/mock/broadcast`. | `ChatRegistry.java:1187-1213`; pm-mcp-core `state-catalogue.adoc:148` |
| L3-5 | minor | Dead branch: `applies == null` is already refused by `revisionAnswer` (`:1862-1864`). If it were reached, it would return after the proposal had already been set ACCEPTED. | `ChatRegistry.java:2084-2100` |
| L3-6 | minor | Test quality: an items count asserted `>= 0`, which cannot fail; hand-written literal input (`198`, `H-77`, `p-*`) in `@EnableGeneratorController` classes (Generators rule); fixture assertions on keys only. | `ChatNarrativeCompletenessTest.java:102,254,278-286,360`; `ChatFixtureScenarioTest.java:126-135` |
| L3-7 | minor | The race test passes time with `parkNanos` jitter, against "no timer wait". It is probabilistic (400 rounds) and asserts only `stopped > 0`. The mutation was killed by an NPE. | `ChatRegistryRaceTest.java:88,135`; log `202343` |
| L3-8 | info | `lapseOpenRevisions` uses `isOpen()`, so a REFUSED revision proposal does not lapse and stays rejectable. | `ChatRegistry.java:1546` |
| L3-9 | info | `narratePrMerged` writes its notice while the reply turn still streams. `endChat` then marks the reply STOPPED with no `chat.turn.completed`. | `ChatRegistry.java:1129-1157,1681-1683` |
| L3-10 | info | `planFor` keeps the prefix fallback when the decision is not open. | `MockRuntimeEngine.java:1392-1402` |
| L3-11 | info | `endedTasks`/`endedWork` are cleared only on seed (unbounded growth, mock only). | `ChatRegistry.java:273,279,358-365,932` |
| L3-12 | info | The ROWS `after` free-text handling invents semantics with no contract. Event names are hard-coded strings, not `EventName` constants. | `MockRuntimeEngine.java:1119-1180,6543,6729`; `ChatRegistry.java:203-204` |
| L3-13 | info | `storage-v2` is `running` in the start state but `waiting` in later states (`running-work.json:1972-1973` vs `:8363-8364`). No detail resource exists for it, so `revisionWindowOpen` returns `true` by its fallback. | `MockRuntimeEngine.java:1341-1345` |
| L3-14 | info | A REFUSED proposal can be rejected only through the API: the web keeps only OPEN proposals and the terminal uses `isOpen()`. CodeRabbit inline-1 raised it; declared out of scope. | `client.ts:1784`; `ChatDialogue.java:81` |

**Message residue:**
- "None on shipped code": dropped (L3-1 to L3-7).
- No conflict/lapse line key: confirmed. pm-mcp-core names no `chat.line.*` key, but `state-catalogue.adoc:140` says a lapse and a conflict are "told by a notice". That key is an open item for pm-mcp-core.
- Sanity-check phase absent: confirmed.

## Residue for the plans that touch the same surface

- **PLAN-197** (running) shares `ChatRegistry.java`, `running-work.json` and `fully-onboarded.json`: a rebase onto this merge.
- **PLAN-139** (staged): its gate "after PLAN-194" is open. It shares `ChatRegistry.java` and `MockRuntimeEngine.java`. It must not rely on the `pr_merged` line of the `storage-v2` fixture (L3-1). Rejecting a REFUSED proposal in the terminal (`ChatDialogue.java:81`, L3-14) is its surface. Re-read it against `4fde136a` before its emit.
- **PLAN-193** (parked) shares `MockRuntimeEngine.java`.
- **PLAN-135** (running) and **PLAN-136**/**PLAN-137** (staged/parked) own `client.ts` and `lib/chat/`. Rendering and rejecting a REFUSED proposal (L3-14) and an `activity.changed` with `"ended"` (L3-2) meet them.
- **PLAN-196**, **PLAN-71**, **PLAN-70**: no shared file.

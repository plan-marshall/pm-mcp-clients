# PLAN-197 landing record

epic: ui-improvements
plan: rb-197-blocks-one-vocabulary (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` and `plugin_version` `unknown`; executor 0.1.2183)
pr: #591, squash `86f31c88` (33 files, +665/−147; 8 branch commits, head `169d0eac`, tree identical to the squash) on `4fde136a` (PLAN-194), merge queue, merged 2026-11-27T11:42:51Z
analysed: 2026-11-27, by sub-agent in three layers, by reading only (no build). The orchestrator re-read at `origin/main` `a3898a4b`:
- `DecisionEnvelope.java:180` (the `"plan"` default);
- `decisionBlocks.ts:31-33` (unguarded `.trim()`).

Message `rb-197-blocks-one-vocabulary-001` (revision 1, amended 2026-11-27T11:45:54Z):
- `merge_state=merged`, `deliverables_done=2` of 2;
- 0 unresolved threads (5 threads, all resolved, each answered);
- `review_body_findings` 5/5 (these are the 5 inline threads; the review body had no separate items);
- `sonar_open_new_issues=0`;
- `self-review: clear` (17 findings, 7 fixed).

One operator question: the merge queue dropped the PR after a green merge-group run (45826084281, 11-26). The answer was "re-enqueue the PR". It merged on 11-27 through runs 45990498285 and 45990498210, both green.

**Merge corroborated:** `gh pr view 591` merged 11:42:51Z, merge commit `86f31c88bb5b55e08b9f53d97a5eacbc7481b716`. #592 (`c3049464`) followed it in the queue.

**CI:** 15 checks pass and 5 skip (CodeRabbit, covering-run-check, deploy-snapshot ×2, auto-merge); none failed. No CI workflow runs Playwright (`integration-tests.yml:26` is `-Pintegration-tests`).

**Sonar.**
- PR 591: 0 issues; gate OK (new coverage 96.6 %).
- Default branch, read by the orchestrator 2026-11-27 after the `main` task `AaIc54NtZy_gQyG5LRmJ` (submitted 12:07:44Z, SUCCESS, after this merge at 11:42:51Z and #592):
  - gate OK, `new_coverage` 90.2 %, `count_status: confirmed`;
  - **3 open**, the three PLAN-181 keys only. The `MockRuntimeEngine.java` key `AaFGfsYupdXNQ-N4ArY-` (`java:S1313`) now sits at `:11491`, moved by the growth of the file, not at this PR's `:8009-8010`.

**SNAPSHOT deploy of `86f31c88`:** `unknown`.

## Layer 1: specified

| Done-when | Verdict |
|---|---|
| D1(a) contract round-trip; a missing `blocks` fails | met:<br>- `NeedsOperatorItem.java:54,63,69,93,137-140` (required, always written, an unknown value read as `other`);<br>- tests `NeedsAndFindContractTest.java:138,153,162,172`;<br>- `test-first`, log `d9d653c7…`. |
| D1(b) every scenario decision and needs item carries a vocabulary value | met:<br>- `ScenarioBlocksVocabularyTest.java:59-102`;<br>- `test-first`, log `a98956ec…`;<br>- mock lines `running-work.json:91,155,175,195,219,3466,4716-5044,7860,8258`. |
| D1(c) no `none`/`project`/`yes`/`no` left in the mock; `blocksOf` falls back to `nothing` | met:<br>- the grep is empty at `86f31c88`;<br>- the scenarios hold plan 39, nothing 13, epic 11, setup 5;<br>- `ChatRegistry.java:1384`, test `ChatRegistryBlocksTest`. |
| D2(d) `OverviewFormattingTest` en/de | met:<br>- `:376-386` (en, de, and the `fr` fallback);<br>- `test-first`, log `dc104df6…`. |
| D2(e) the terminal readers show `blocks nothing` and the unknown word, never "blocks the plan" | **partly**:<br>- one helper, `DecisionBlockWords.java:30-55`, used by `AnswerCommand.java:234`, `ConsoleNotice.java:584`, `QuestionsCommand.java:138` and `OverviewFormatting.java:402`;<br>- but `answer` still says "blocks the plan" for a decision with no `blocks`, through `DecisionEnvelope.java:180` (L3-1);<br>- `ConsoleNoticeTest` has no `nothing` case. |
| D2(f) `InboxView` and `DecisionView` show the decision words | met:<br>- `DecisionView.test.ts:354-378`, `InboxView.test.ts:123-152`;<br>- fail log `071736` (4 of 3483), green log `094139`;<br>- `decisionBlocks.ts:14-33`. |
| D2(g) `overview.needs.blocks*` gone; en/de parity | met in substance: 2 keys left each catalogue. The only two hits are the new guard itself (`NeedsAndFindContractTest.java:174-175`). |
| D2(h) 8 screenshots | met: all 8 exist; the sub-agent viewed two (`needs-en-after`, `decision-de-after`). |
| Gates | partly:<br>- full verify: green in CI and the merge group;<br>- whole-tree pre-commit: `122453.log` at `8d11b79c`, 5737 Java + 3483 vitest; only an indentation commit followed;<br>- `-Pcoverage`: not run locally (Sonar new code 96.6 %);<br>- **`-Pe2e`: not green at any head**. `091337.log` has 2 failures (`designer.spec.js:264`, main's known red, and this plan's own `inbox.spec.js:27`). The rerun `094504.log` passed `inbox.spec.js:27` but was killed (exit 143). |

True count: 7 of 8 Done-whens met (D2(e) partly). Gates: e2e not met. The message's "2 of 2" overstates.

## Layer 2: implemented

All 33 files trace to D1 or D2. Outside the Expected Surface, explained:
- `MockRuntimeEngine.java:8009-8010` (the needs sort);
- the helpers `DecisionBlockWords.java` and `display/decisionBlocks.ts`;
- `stores/attention.ts`;
- the test files `ChatTextsCatalogueTest`, `DecisionsScenarioTest`, `NeedsAndFindScenarioTest`, `ScenarioBlocksVocabularyTest`, `ChatRegistryBlocksTest`, `AnswerDirectFormTest`, `ConsoleNoticeTest`, `QuestionsCommandTest`, `InboxView.test.ts`;
- `inbox.spec.js`.

Correctly untouched: `fully-onboarded.json` (its decisions already carry `plan`/`epic`) and `needs-and-find.adoc` (`:54-56` already states the vocabulary). **PLAN-194 is preserved:** against `4fde136a`, only the `blocksOf` hunk of `ChatRegistry.java` and `blocks` lines of `running-work.json` change.

**Stale or false claims:**
1. "The `common.kind.*` block words are deleted": all 7 keys remain; only their use for "blocks" went.
2. "No hit for `overview.needs.blocks`": 2 hits, the guard itself.
3. "`InboxView.test.ts` … (incl. `constructor`) test-first, failing run `071736`": the `constructor` case came in review at `ef6cbdce`, after both cited runs.
4. "CI ran the suite green on the final head" (about e2e): CI runs no Playwright.
5. "Surface drift: none": the message itself then lists files outside the surface, and still omits `DecisionBlockWords.java` and 7 test files.
6. `review_body_findings_total=5`: these are inline threads.
7. "Green … in 5624/5734-test runs": 5734 is the gate run at `8d11b79c`, the pre-rewrite head. No 5624 run was found.

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. The design is consistent (`needs-and-find.adoc:54-56`, `texts-and-languages.adoc:137`, `components-tags.adoc:203`, `decisions-texts.adoc:30,41-42`). `decision.blocks.*` and `common.unknown` are in en/de parity. `nothing`→`none` is mapped only in the two helpers.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | A decision with no `blocks` defaults to `"plan"`, so `answer` prints "blocks the plan" while `questions` prints the unknown word. The spec says never "blocks the plan". The self-review declined it as out of scope. | `DecisionEnvelope.java:180`; `AnswerCommand.java:234` vs `QuestionsCommand.java:138` |
| L3-2 | minor | The web calls `.trim()` on `dec.blocks` without a guard. `getDecisions` does not validate the response, so a decision with no `blocks` would throw and break the list (before, it showed a raw key). Every scenario decision carries `blocks` today. | `decisionBlocks.ts:32`; `DecisionView.svelte:285,318,1504`; `client.ts:3957-3968`; contrast `attention.ts:143` (`?? ''`) |
| L3-3 | minor | The clients treat a needs item without `blocks` differently. The terminal refuses the whole page; the web shows the unknown word for that row. Undocumented. | `NeedsOperatorItem.java:139-140`; `attention.ts:143` |
| L3-4 | minor | Test gaps:<br>- `ConsoleNoticeTest` has no `nothing` case;<br>- `QuestionsCommandTest`'s unknown assertion already passed at base;<br>- the absent-value path is untested in both readers. | `ConsoleNoticeTest`, `QuestionsCommandTest`, `QuestionsCommand.java:140` |
| L3-5 | minor | Generators rule: hand-written input in new tests, including a class without `@EnableGeneratorController`. The inputs are ids, links and unknown values (`"/api/v1/decisions/Q-007"`, `"credential"`, `"Q-31"`, `"landing-train"`, `"Q-1"`/`"Q-2"`). | `NeedsAndFindContractTest.java:140,155-166`; `AnswerDirectFormTest.java:318-331`; `QuestionsCommandTest`; `ChatRegistryBlocksTest` |
| L3-6 | minor (gate) | No green `-Pe2e` run at any head, and CI runs none. The plan's own `inbox.spec.js:27` failed once, then passed once in a run that was killed. | `pm-web-e2e` logs `091337`, `094504` |
| L3-7 | info | `DecisionBlockWords` takes its value set from `NeedsOperatorItem.BLOCKS_VALUES`. No guard pairs that set with the `decision.blocks.*` keys. | `DecisionBlockWords.java:46` |
| L3-8 | info | The mock sorts `task`/`other` as blocking, while `DecisionView.isBlocking` treats `task` as not blocking. | `MockRuntimeEngine.java:8009-8010`; `DecisionView.svelte:241` |
| L3-9 | info | `ScenarioBlocksVocabularyTest.isDecision` skips SSE `decision.opened` payloads. | `ScenarioBlocksVocabularyTest.java:183-186` |

**Message residue:**
- (1) The landing check is incomplete (`unknown` fields): a known framework limit; nothing to do.
- (2) The plan's mailbox read is refused with `invalid_target_plan` because the id starts with a digit: a framework defect to file (not reproduced).
- (3) The dropped queue entry: confirmed and answered.
- (4) The local e2e is not green: confirmed (L3-6). The message's "CI ran it" is false.
- (5) The engine hypothesis (pm-mcp-core sends `nothing`/`setup` and never omits `blocks`): still unverified. It matters more now that the terminal refuses a needs item without `blocks` (L3-3). Open cross-repo check.

## Residue for the plans that touch the same surface

**General rule from now on:** every `NeedsOperatorItem`/`AttentionItem` fixture (Java `requireNonNull`) and every web `DecisionEnvelope` fixture needs a string `blocks`. A scenario needs item without it fails `ScenarioBlocksVocabularyTest`.

- **PLAN-135** (running): `client.ts`. Its re-grounding calls `client.ts:113` disjoint, which it is, but line refs above `:113` taken at `da3cafdf` are now +3.
- **PLAN-139** (staged) shares 5 code files: `ChatRegistry.java`, `MockRuntimeEngine.java`, `AnswerCommand.java`, `OverviewFormatting.java`, `OverviewFormattingTest.java`. That is at the overlap limit, and nothing is running against it.
  - `OverviewFormatting` lost `kindKey`/`COMMON_KIND_PREFIX` (about −17 lines around `:390-420`).
  - `ChatRegistry.blocksOf` is now package-private.
  - Re-read against `86f31c88`.
- **PLAN-136** (staged) and **PLAN-137** (parked): `client.ts`, `DecisionView.svelte` (+1 after `:5`; decision rows go through `blockWordText`) and `DecisionView.test.ts`.
- **PLAN-179** (staged) and **PLAN-193** (parked): any needs item they add must carry `blocks`. `running-work.json` early sections are +11 lines.
- **PLAN-196, 71, 70, 62, 190, 192**: no shared file.

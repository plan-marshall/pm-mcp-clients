# PLAN-191 landing record

epic: ui-improvements
plan: rb-191-epic-wait-unreadable-and-overlap-found (runbook lane, harness opencode, `deepseek-v4.1-flash/high`, effort high; `total_tokens` `unknown`, `plugin_version` 0.1.2183 from the lane text; executor 0.1.2183)
pr: #582, squash `458f9e3c` (28 files) on `ce7dc289`, merge queue, merged 2026-11-26T00:17:29Z
analysed: 2026-11-26, by sub-agent in three layers, by reading only (no build); `EpicView.svelte:401-418` (`waitLabel`, no echo) and the German `held` texts (`epic_view.de.properties:59`, `overview.de.properties:143`) re-read by the orchestrator at `origin/main` `9011e6d1`

Message `rb-191-epic-wait-unreadable-and-overlap-found-001` (revision 1, 2026-11-26T00:19:13Z): `merge_state=merged`, `deliverables_done=3` of 3, 0 unresolved threads, 8 of 8 review-body findings handled, `sonar_open_new_issues=0`, `self-review: clear`. Two operator questions: an unknown wait reason → "Echo the reason" (`overview.epic.waits.other`); a stored report of the retired kind → "Skip it at the page" (`ReportKind.knows`).

**Merge corroborated:** `gh pr view 582` merged 00:17:29Z, merge commit `458f9e3c55921cc49e48a3ffde201636f7e17bc8`. Diff: 26 code files, 2 `doc/` files.

**Sonar.** PR: 0 open new issues at the head (two found and fixed in `c9dd71cd`: `java:S1192`, `java:S5961`). Default branch read 2026-11-26 after the `main` task `AaIWTII6yu25SKZIp0Bi` (05:20:42Z, after all three merges; the task right after this merge is `AaIVS2jwmAmvnIwqAPUI`, 00:39:53Z): gate OK, coverage on new code 90.2 %, **3 open**, the three PLAN-181 keys, none in a file this PR touched.

**SNAPSHOT deploy of `458f9e3c`:** `unknown`.

## Layer 1: specified

| D | Verdict |
|---|---|
| D1 `unreadable` replaces `stale`, every reason shown | met: the `stale` grep leaves no wait-reason use; negative control `ControlCataloguesTest.java:56-61`, `waitText` tests `EpicViewFormattingTest.java:55,72,78,85` — `test-first` by the pre-fix logs `8e9b1430…`, `b729fa45…` (tests and fix share `035b9390`); design `epic-view.adoc:155-166` = `overview.{en,de}.properties:142-144`, pinned in `EpicViewCatalogueTest.java:178-203`; web `EpicView.svelte:412-413`, test `EpicView.test.ts:335-384`; mock row `fully-onboarded.json:1877-1890`. Browser check: 4 of 4 screenshots (`191-{before,after}-{en,de}.png`), content as claimed |
| D2 `epic.overlap_found` removed | met: `ReportKind.java` 15 kinds; negative control `ReportRecordsTest.java:54` (`test-first`, log `a59b41ef…`); `ReportKind.knows` guards both parse paths (`ReportsPage.java:113,173`), tested `ReportRecordsTest.java:166-195` |
| D3 traceability | met vacuously: no link changed, no traceability file names either value |
| Gates | partly: the last `-Pe2e` ran at `c84a0df3` (18:48Z), none after `8dc030c0`, which changed the mock's `ChatRegistry` (no e2e test asserts that line); coverage last at `7a85f183`, pre-commit at `815bc756` |

True count: 3 of 3.

## Layer 2: implemented

26 of 28 files trace to a deliverable; `control.{en,de}.properties`, `ChatRegistry.java` and `ChatContextNarrativeTest.java` repair the break #585 brought in with the merge of `main` (explained). 11 files lie outside the Expected Surface (`WorkRow`, `ReportsPage`, `control.*` ×2, `ReportCatalogueTest`, `ChatRegistry`, `fully-onboarded.json`, `ChatContextNarrativeTest`, `EpicViewScenarioTest`, `EpicViewFormattingTest`, `EpicView.test.ts`). The surface's `doc/design/web-app/epic-view.adoc` never existed (the message reports it). **Stale or false claims:** S-1 the message's "no changed file lies outside the Expected Surface" (11 do); S-2 "three hits" of `overlap_found` (5, line numbers from `735aa6c1`); S-3 the list of surviving `stale` uses (misses `DecisionView.test.ts:1278,1282`, names keys the grep does not return, names `client.ts` for `client.test.ts`); S-4 the design texts are pinned by `shouldFormatHeadStatusAndWaitTexts` (moved to `shouldFormatAWaitTextForEveryReason` in `c9dd71cd`); S-5 the PR body's "coverage and package at `815bc756`" (at `7a85f183`, `4eae07b3`).

## Layer 3: correct (reported to the operator; no fix plan staged)

No blocker, no major. Clean: en/de parity for every new key, design rows word for word, no generic catch, no `System.out`, no `{@html}`, no timer wait, no dependency.

| # | Severity | Finding | Where |
|---|---|---|---|
| L3-1 | minor | the operator's "echo the reason" is in the design table shared by both clients (`epic-view.adoc:163`), but the web's `waitLabel` returns `''` for an unknown reason; only the terminal echoes | `EpicView.svelte:416-417` |
| L3-2 | minor | German `held` reads `wartet: aufgehalten` in the web, `wartet: zurückgehalten` in the terminal and in the new design row | `epic_view.de.properties:59`, `overview.de.properties:143`, `operator.de.properties:420` |
| L3-3 | minor | `waitReasonLine`'s `UNREADABLE` and `HELD` branches are untested (a wrong key would print the key itself) | `EpicViewFormatting.java:167-168` |
| L3-4 | minor | `nextLaunchable()` treats an unknown wait word as launchable (the live path `WorkActionDialogue.java:549-553` does not; only a test calls it) | `EpicViewLevel.java:367-375` |
| L3-5 | minor | the new test takes literal input in an `@EnableGeneratorController` class (CLAUDE.md Generators rule) | `ReportRecordsTest.java:166-195` |
| L3-6 | minor | "skip a retired report kind at the page" is recorded in no `doc/` document | `ReportsPage.java:123-133` |
| L3-7 | info | `ReportSentEvent` still throws on a retired kind (no production caller) | `ReportSentEvent.java:61-73` |
| L3-8 | info | `case NONE` unreachable, kept for exhaustiveness | `EpicViewFormatting.java:134` |
| L3-9 | info | the German parameterised test asserts only non-blank, so it passed on the old code too | `EpicViewFormattingTest.java:65` |
| L3-10 | info | `epic.event.overlap` is a dead key (residue item 1) | `epic_view.{en,de}.properties:11` |
| L3-11 | info | new test without `@DisplayName` (the class's existing style) | `ReportCatalogueTest.java:73` |
| L3-12 | info | literal format parameters, CodeRabbit nitpick declined with a reason | `EpicViewCatalogueTest.java:178-203` |

Residue § 9 of the message: all six items confirmed (L3-1, L3-4, L3-10; `waitReasonLine` returns empty for an unknown word; `epic.views.*` has no design table; the unread count declined). Dropped by the sub-agent as unconfirmed: whether `running-work.json`'s `blocks: "project"` feeds a chat line — PLAN-197 moves it to `setup` either way.

## Residue for the plans that touch the same surface

Shared code files with this merge: PLAN-194 (running: `ChatRegistry.java`, `ChatContextNarrativeTest.java`, `fully-onboarded.json`, `control.{en,de}.properties`), PLAN-197 (staged: `ChatRegistry.java`, `fully-onboarded.json`, `overview.{en,de}.properties`), PLAN-135 (`EpicView.svelte`, one file). PLAN-135 is re-read against `9011e6d1` before its emit.

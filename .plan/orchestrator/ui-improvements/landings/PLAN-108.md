# Landing analysis: PLAN-108 web-design-docs-residue

epic: ui-improvements
workstream: WS-06
pr: 362

PR #362 `ddcab793` (squash-merged 2026-11-07T10:25:27Z; final head `629e33ce`; 8 commits, 45 files, +830/-392). A direct Claude Code session (not a lane), started by the operator 2026-11-06 in worktree `108-web-design-docs-residue`. Landing message `inbox/plan-108-web-design-docs-residue-001.md` (written before the merge: says "not merged", omits the merge with main and the three flaky-test fixes; the operator's note at the merge supplies them). Analysis by a read-only sub-agent (code read at the merge commit; nothing built or run, the Playwright suite not run); thread counts, resolver, merge facts and the Sonar status were re-read by the orchestrator (marked V*).

## Merge rule

SATISFIED. One substantive CodeRabbit review (`5807660296` on `fa561b16`, 2026-11-06T22:05Z: 4 inline items, 1 nitpick); three threads fixed in `93c67d2b` and resolved by the bot, the nitpick (`filterText.test.ts` non-Latin tokens) answered in a comment and present in the code; the fourth thread (`texts-and-languages.adoc:181`, English-only "just now" in `StatusCommand`/`ProjectsCommand`) was declined as PLAN-34 scope and resolved by the operator's account on 2026-11-07T08:39Z with "tracked as PLAN-34 deliverable 4". Later bot reviews are empty confirmations; no review exists on a head after `93c67d2b` (the CodeRabbit check on the final head is `success`).

- Unresolved threads at the merge head (V*: GraphQL): 0 of 4.
- SonarCloud (V*: check run SUCCESS; bot comment 10:10Z on head `629e33ce`): Quality Gate passed, 0 new issues, 0 accepted, 0 hotspots; REST read `total: 0`, project status OK; rules left open: none. A confirmed 0.
- CI at `629e33ce`: all green (build 25, 26, sonar-build, integration tests, dependency-review, CodeRabbit). Playwright is NOT in CI: `pm-web-e2e/pom.xml:22` sets `skipPlaywrightTests=true`, so the 390 px test and every e2e spec are session-local evidence.
- `reports.spec.js` "marks all shown reports read" (PLAN-105 defect 13): untouched by this PR, still expects `6 unread reports` at `ddcab793`; the session reported it failing at base too (7 vs 6; not reproduced here). The defect stays open and CI cannot see it.
- Session claims: merge with main (`0016896a`, conflict in `EpicView.test.ts`), three flaky-test fixes, all threads resolved, Sonar 0, every check green: all true. The PR body promises a Sonar read "in a comment" that was never posted (the inbox message holds it for the earlier head); "both are fixed" then lists three flakes (typo). `ref-asciidoc validate` was not literally clean (pre-existing generic header-template findings and one `<<…>>` xref, left as is).

## Three layers

| Done-when | Implemented | Correct |
|---|---|---|
| D1(a) no plan number in lasting design text | MET (`git grep "PLAN-"` on `components.adoc` and `doc/design/shared` empty) | |
| D1(b) arrow-key divergence named | MET (`components.adoc` row-menu text, back-pointer in the ActionBar paragraph; Home/End wording and a missing blank line before the key list fixed) | true: only `ReportsView:1145` uses `RowMenu` and passes no disabled item; only `/dev/components` does |
| D1(c) SVG redrawn from the shipped menu | MET ("Report actions", "Mark read"; "Copy link", "Archive" gone; upward case above the bar added) | text nodes ≤ 336 in the 360 viewBox, nothing clipped; width 180 = `min-inline-size`; caption says "44 px entries" over 28 px drawn entries (cosmetic) |
| D1(e) popup placement decided, measured test at 390 px | MET (`#row-menu-placement`, `RowMenu.svelte` `z-index:30`, `place()`, `.row-menu__list--above`; `row-menu.spec.js` samples `elementFromPoint` at 5 points per entry) | PARTLY: the design says dialogs sit at 50, but `Dialog.svelte`, `CommandPalette`, `LanListenerModal` use 100. The test is not test-first in history (fix and test in one commit `cf2d741a`; "5 of 5 covered before" is the session's word) and does not isolate flip from z-index (bar ≈ 53 px, one-entry menu ≈ 54 px: the flip is 1 px; the flip itself is covered only by jsdom tests with stubbed rects); the spec is outside CI |
| D1(d) shared token rule in the design and `filterText.ts` | MET (`tokenize` drops words with no `\p{L}\|\p{N}`; `view-density-and-filtering.adoc#filter`; `DecisionView:80`, `EpicView:61,683`, `ReportsView:89`, `OverviewView:173` use it) | consistent; `ProjectExplorerView:423` keeps its own filter (the design says "every adopted view") |
| D2 rule decided, rows added (en, de), wireframes rewritten | MET (`texts-and-languages.adoc:164-177`; `work-actions:332`, `paired-devices:24`, `project-explorer:26,31,71`, `plan-overview:141`, `needs-and-find:35`) | matches `format.ts` (`just_now` < 60 s, `min`/`h`/`d` < 30 d, then `Intl.RelativeTimeFormat` months and years; node `Intl` gives "last month / vor 2 Monaten / letztes Jahr"). `format.test.ts` pins months, years, just now but not the 30-59 day `last month` row |
| D2 grep Done-when | MET (4 hits, all forms the rule has) | |
| D2 terminal code binding | NOT code-changed, by design | `ProjectsCommand:317-330`, `StatusCommand:475-488`, `WebCommand:623-643` still print weeks, `yesterday`, `1 month ago`, English-only `just now`: the shared design contradicts them until PLAN-34 deliverable 4 (amended 2026-11-07) |
| Sonar, docs review | MET / PARTLY (validator literal) | |

## Defects (at `ddcab793`)

1. MED L `RowMenu.svelte` `place()` re-runs on every capturing `scroll` and clears `max-block-size` before re-applying it: a capped menu may lose its `scrollTop`; tests dispatch scroll on `window` only. Rare today (one entry).
2. MED V process: Playwright is outside CI; the 390 px proof is session-local and not isolating.
3. LOW L `row-menu.spec.js` waits only for the `unread-count` element; `reports.spec.js` waits for "6 unread reports" because the scenario announces late: possible flake.
4. LOW V the design says dialogs are at z-index 50; the code uses 100.
5. LOW V PR body: the promised Sonar comment never posted; "both are fixed" for three flakes.
6. LOW V test-first order not visible in history.
7. LOW V no `format.test.ts` case for 30-59 days.
8. Not a defect of this PR: PLAN-105 defect 13 (`reports.spec.js` 7 vs 6) stays open.

## Scope: the flaky-test fixes (at the operator's word)

28 of 45 files are test-only: 25 `pm-api` tests (exactly 309 swaps of `nonEmptyStrings` for `nonBlankStrings`, no other change) and 3 `pm-operator` tests. No `nonEmptyStrings` remains in code; PRs 358, 363 and 364 touch none of these files. `EntryFlowStaticsTest` now awaits a `workerLeft` latch (5 s) before asserting the flag: sound and mutation-safe, and it fixes the race of the unresolved PR 357 thread `EntryFlowStaticsTest:116` (PLAN-116 deliverable 1(a)); its "assert the worker cause" nitpick stays open. `ActionSupportTest` now repeats Ctrl+C every 20 ms until the dialogue leaves (15 s deadline fails the test otherwise): sound, but it can no longer detect a first dropped Ctrl+C. `ConsoleBadgeTest` +4 swaps.

**Possible product defect (Ctrl+C, HYPOTHESIS, not observed in a real terminal):** `StartPrompt.ask`/`askRaw`/`askInline` print and flush the question before `reader.readLine()` starts (`StartPrompt.java` ~123-146, ~200); `ChoicePrompt.renderFrame()` prints before `nextLine()`; `pm-operator/src/main` has no `Signal.INT` handler (JLine installs its own inside `readLine`). A Ctrl+C in the gap would take the JVM default and end the process with 130 instead of leaving the dialogue (in `PmConsole`'s REPL it would kill the console). The window is microseconds to milliseconds; in the test `VirtualScreenTerminal.raise` has no default disposition, so the signal is simply dropped: the flake is mainly a test artefact. Recorded as a note in PLAN-116, not staged as a bug.

## Metrics and anomalies

- 8 commits; 62% of the files are test-only flake fixes; the product diff is `RowMenu.svelte`, `filterText.ts`, one line each in `EpicView.svelte` and `ReportsView.svelte`.
- Direct session: a thread the session could not resolve (permission classifier) stayed open about 10 hours until the operator's account resolved it; waiting for the operator's merge word and the queue accounts for the rest.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 362; `landing` = `landings/PLAN-108.md`; `plan_marshall_plan_id` = `108-web-design-docs-residue`
- [x] inbox message `plan-108-web-design-docs-residue-001` archived
- [x] PLAN-34 deliverable 4 (ages in the terminal) already carries the open thread; PLAN-116, PLAN-107 and PLAN-112 amended (below)
- [x] D1 (PLAN-112) unblocked and emitted; epic.md Decisions and Open Defects; anchor; view; ledger committed

## Follow-Ups

- **PLAN-116:** the `EntryFlowStaticsTest:116` race is fixed in `629e33ce` (reply on PR 357 citing it and resolve the thread; the line numbers of the file moved); note the Ctrl+C window (hypothesis).
- **PLAN-107** (web reports fourth residue, now free of PLAN-108): add defects 1, 3 and 7 (capped menu scroll test, `row-menu.spec.js` wait, `format.test.ts` 30-59 days) and the `reports.spec.js` expectation (PLAN-105 defect 13).
- **PLAN-112 (D1):** correct the dialog z-index wording (50 vs 100) and settle the layering of the menu over the bar; decide `disabledReason` for `RowMenu`.
- **Epic Open Defects:** Playwright is not run in CI.
- **PLAN-34 deliverable 4:** applies the age rule to `ProjectsCommand`, `StatusCommand`, `WebCommand` and `terminal-client/08, 15, 17, 18` (amended 2026-11-07).

# Landing analysis: PLAN-112 design-d1-component-vocabulary

epic: ui-improvements
workstream: WS-06
pr: 365

PR #365 `aa3a9be3` (squash-merged 2026-11-07T16:13:52Z through the merge queue; final head `836cf806`; 33 files, +1402/-364). A DIRECT Claude Code design session (operator-started, worktree `112-design-d1-component-vocabulary`), documents only. Landing message `inbox/plan-112-001.md` (revision 2, amended through `orchestrator inbox amend`). SCOPE OF THIS ANALYSIS: lighter than a code landing. The orchestrator verified the merge, the thread count and the document status; the content of the design (the decisions, the tints, the component set) is the operator's to judge and was NOT re-read here; review counts below are the session's own report.

## Merge rule

SATISFIED, on the operator's word ("resolve all findings, after that merge if all is green"). Verified (V*): merged `aa3a9be3`; 29 review threads, 0 unresolved; `main` matches the final reviewed commit; required checks green. Session report (L): CodeRabbit raised 29 comments over two rounds, 23 fixed, 6 answered without a change; `ref-documentation` review 0 issues; `ref-asciidoc` `validate` clean for the new documents (the other files only their pre-existing header-attribute findings, identical to `main`); `verify-links` 657 links, 0 broken. No Sonar read applies to a documents-only PR.

- Session error, corrected before the merge: CodeRabbit added 12 new comments on the rebased commits and the session's resolve loop closed them before it had read them; it reopened the work, fixed or answered all 12 in `836cf806`, waited for green, then merged.
- One decision reversed by the second review round: the code already normalises the two reason spellings to `waiver` and `waits_for_you` (`NeedsOperatorKind.fromValue`); the design follows the code, so `overview.needs.waiver` and `overview.needs.waits_for_you` stay.

## THE GATE IS STILL OPEN

The documents merged at `Status: DRAFT` (V*: `components.adoc:15` and `components-fields.adoc:15` read `Status: \`DRAFT\``). The specs of D2, D3 and D4 and the epic's design-review rule gate those sessions and the web plans waiting on them on "the merge of this session's pull request with the documents at `AGREED`". No `AGREED` commit exists on `main`. The session offered a small pull request that only flips the status. The operator owns `AGREED`.

## Decisions the session took for the operator's review (from the message; not verified)

Three kind tints with a measured rule (ΔE00 ≥ 20, hue ≥ 30°, ≥ 30 between tints, contrast ≥ 3:1); `RowMenu` gains `disabledReason` (one act menu); layers as tokens (1/20/30/90/100: this also corrects the stacking wording PLAN-108 left); reason `setup` for a project's setup need, not `credential` (deviation from message 006: `shared/needs-and-find.adoc` reserves `credential` for the machine's own items); the plan act is "Plan settings…" in both clients; a decision part with several answers uses `CheckGroup` cards; a disabled button with a reason stays reachable by keyboard; an act the client cannot run is shown disabled with its reason; "To the decision" only as a head link; terminal project rows show the first reason. Terminal files after PLAN-97: `19-actions.adoc` ("End the plan…", "End the epic…", "Plan settings…"), `17-projects.adoc` ("setup open" with its `!` count).

## Scope beyond the listed lines (reported by the session)

`shared/work-actions.adoc` and `work-actions-review-texts.adoc` (they quote the renamed end texts; D4's :350-432 untouched), `web-app/README.adoc` :180, the Reports row-menu bullet and the cleanup layout.

## Hand-offs the session passed on

- Terminal package (not yet staged): align the console badge words (`operator.console.badge.*`: "question" → "decision", "waiver request" → "waiver") with the shared keys, and besides `PmConsole.java:741` and `overview/OverviewFormatting.java:197-199` also `ConsolePrompt.java` (the full list is in the message).
- Catalogue clean-up package (not yet staged): a client whose reload is deferred for unsent input may fetch a missing locale from a newer runtime; remove keys together with their readers in one release.

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` = 365; `landing` = `landings/PLAN-112.md`; `plan_marshall_plan_id` = `112-design-d1-component-vocabulary`
- [x] inbox message `plan-112-001` archived; epic.md Decisions and Watch (the AGREED gate, the two hand-offs); anchor; view; ledger committed
- [ ] D2 (PLAN-113) and D3 (PLAN-114) are NOT emitted: blocked by the open `AGREED` gate until the operator decides (AGREED PR, or a recorded waiver)

## Follow-Ups

- Stage the terminal package and the catalogue clean-up package from the AGREED D1 documents (the Watch "Design review 2026-11-06" lists the packages).
- PLAN-107 (RowMenu tests) may now be re-read against D1's `disabledReason` and layer tokens before it is emitted.

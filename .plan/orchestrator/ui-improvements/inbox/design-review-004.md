envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T16:11:08Z
revision=2
amended=2026-11-06T19:36:11Z

# Drop the layout selector "Comfortable / Compact / Split" completely

Sender: operator design review in the browser, 2026-11-06. Follow-up to `design-review-003.md` (one list → content navigation).

## Operator feedback
With the unified list → content navigation, the selector "Komfortabel / Kompakt / Geteilt" ("Comfortable / Compact / Split") that some views carry can be dropped.

## Findings
- **The design has already decided this.** `doc/design/web-app/view-density-and-filtering.adoc:17-44` ("Page Templates Replace the Layout Modes", UX-P-03): "no view keeps a layout toggle or a `pm_view_layout_<view>` choice … the plan that moves the view removes the mode from it." The page template owns the split, and the list → content pattern of message 003 is exactly that split as the only mode.
- **What is left in the code:** only `pm-web-app/src/lib/views/DecisionView.svelte` still uses it (seen in the browser on `/decisions`, top right). The parts:
  - `pm-web-app/src/lib/components/ViewLayoutToggle.svelte`
  - `pm-web-app/src/lib/stores/viewLayout.ts` + `viewLayout.test.ts` (keys `pm_view_layout_<view>`; the tests still name `reports` and `epic` as well)
  - the texts `common.layout.label`, `common.layout.{comfortable,compact,split}` and their `.title` variants in `pm-api/src/main/resources/messages/common.{de,en}.properties:~237-245`
  - the decision view's mode-dependent CSS/markup (`comfortable`/`compact`/`split` branches) and any e2e or unit test that switches modes.
  - `components/DataTable.svelte` is not affected: its `compact` is a property of its own (VD-P-05), not driven by the layout store, and stays.
- **Design text to update:** the section `[#layouts] Layout Modes` (:46-73, including the split breakpoint) and the layout parts of `[#rollback]` (:148ff) are removed, replaced by one sentence saying that the template's split is the only layout. The filter contract (`[#filter]`) stays. Also: `web-app/README.adoc:19` (the index line naming "the three layout modes"), `web-app/needs-and-find.adoc:29` (the table / list switch of the view density design), and the key `decisions.split.select`.
- Pre-1.0: no shim. Stale `pm_view_layout_*` keys in browsers are harmless and are no longer read.

## Affected plans
- **PLAN-51 web-decisions-triage (staged)** moves Decisions onto `TriagePage` and already removes the layout toggle *for Decisions*. It waits for D1 and D2 (003 rescopes it). The code removal of this message does not wait: it ships now as its own runbook, and PLAN-51 then starts from a Decisions view without layout modes.
- **The design text** (the `Layout Modes` and `rollback` sections, `README.adoc:19`, `web-app/needs-and-find.adoc:29`) is removed by design session D2 (`design-review-010.md`), not by an implementation plan.
- PLAN-72 token-cleanup-lint: the layout-mode spacing rules (`--space-2 --space-3` compact padding) go away with the CSS. Nothing else to do.

## Suggested action (the epic decides)
A small runbook package (`web-drop-layout-modes`), now: delete `ViewLayoutToggle.svelte`, `stores/viewLayout.ts` and its test, the `common.layout.*` keys and `decisions.split.select`, and the mode branches of `DecisionView` (Decisions keeps its single column until PLAN-51). Checks: `npm run quality`, the decisions e2e, no `pm_view_layout` / `common.layout` / `decisions.split.select` left in `git grep`.

## Process
The code part of this message needs no design session and can be implemented now; the design text is D2's (see `design-review-010.md`). Every other message of this review is designed first, in its own Claude Code session, before its implementation.

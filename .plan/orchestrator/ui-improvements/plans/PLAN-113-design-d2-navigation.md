# PLAN-113: Design D2: Unified Navigation (List → Content, Column Window, URL Scheme)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one design session, handed off as a direct Claude Code session (operator instruction 2026-11-06;
> not the runbook lane, not `/plan-marshall`).
> The DESIGN of this plan is written in the archived operator messages named under Sources; this spec fixes the
> scope, the owned documents, the process and the done criteria. Where the two differ in detail, the messages govern the
> design and this spec governs the process.

## Objective

Operator design review of 2026-11-06, session **D2 (navigation)** of `design-review-010.md`: the left-to-right navigation of
Running work (rail → select an item → its content on the right) becomes the one pattern of every kind. D2 writes the new
`doc/design/web-app/navigation.adoc` (sections 1–10 of message 003, with the width table) and the document changes that
follow from it: lenses and the rail, levels as columns, the column window with its gap strip, Up / one breadcrumb / browser
Back instead of the caller stack, one URL scheme with the push/replace rule, kinds and `KindMark` usage, keyboard and
focus, the phone, scope, and the chat as a side column. It also removes the layout modes from the design text (message 004:
the code part is PLAN-111) and sets the phone bottom bar to What waits · Running work · Decisions · More. D2 changes
documents only. PLAN-55, 58, 59, 52, 51, 49, 62 and the navigation engine wait for the merge of this session's pull request
with the documents at `AGREED`.

## Sources (read in full before scoping)

All under the epic tree `inbox/archive/design-review/` (resolve the tree with `orchestrator resolve-path --slug
ui-improvements`; read, never write):

- `design-review-003.md` (the design, §1–§10, the agreed documents, the affected plans, the suggested action)
- `design-review-004.md` (the layout selector: the design-text part belongs to D2)
- `design-review-008.md` § "Agreed documents that change" (the Projects filter, live region and key pattern in
  `view-density-and-filtering.adoc` are D2's edit)
- `design-review-009.md` §2 (the chat column) and `design-review-003.md` §10 (the side column D4 reads)
- `design-review-010.md` § Process, § Design sessions (row D2), § Operator decisions, § Per-plan instructions
  (PLAN-83, PLAN-85, PLAN-60 routes)

## Deliverables

1. **`web-app/navigation.adoc` (new)** — sections 1–10 of message 003 with the **width table** (rail expanded and collapsed,
   at 360 / 768 / 1024 / 1280 / 1440 / 1920 px; at 1024 px two columns fit only with a narrow list column, so the gap strip
   shows its marks only and names them in a popover; the table states the rule per width). The width table is the one open
   point of the message: it is **settled with the operator from a throw-away column-window prototype** built on the mock
   runtime (`mock-machine`) that checks the width table, focus and the gap strip at 1024 / 1280 / 1440 px and on the phone,
   before `AGREED`. The prototype is not committed to production paths.
2. **`web-app/components.adoc` — the column templates only** (after D1 has merged): `TriagePage` as the column engine,
   `ObjectPage` regions inside the object column. KindMark and the kind tints are D1's.
3. **`web-app/README.adoc`** — :19 (index line naming the layout modes), :138 (rail entries), :144 (bottom bar), :182 (body
   scrolling), :211-239 (page widths), :481ff (routes table in one address scheme, including the `/settings` tabs of
   PLAN-60 and `/settings/browser` of PLAN-85).
4. **`web-app/view-density-and-filtering.adoc`** — the `[#layouts] Layout Modes` section (:46-73) and the layout parts of
   `[#rollback]` (:148ff) are removed and replaced by one sentence saying the template's split is the only layout; the filter
   contract `[#filter]` stays; the Projects filter, the always-present live region and the key pattern of message 008 are
   added.
5. **Shared documents (web part only)** — `shared/plan-view.adoc:43,182` (head path → breadcrumb), `shared/plan-overview.adoc:161-169`
   (persistence vs URL; after D1's edit of :65-69), `shared/cleanup.adoc:139` (in place), `shared/completion-report.adoc:104`
   ("its plan view"), `shared/needs-and-find.adoc` and `web-app/needs-and-find.adoc` (:29, bottom bar, rows = KindMark +
   reason, the removed table/list switch), `shared/project-explorer.adoc` (the one project page: work, setup, release; after
   D1's row edit), `web-app/interaction-affordances.adoc` (keys: Ctrl+←/→, Up; after D1's `#acts` edit).
6. **`doc/plans/web-moving-between-plans.adoc` and its index row** — PLAN-83's only deliverable is folded into the engine;
   anchors are updated; the row is set `done` by the last of the D2 engine, PLAN-51 and PLAN-59, not by this session.
7. **Review and pull request** — see Standing Constraints.

## Standing Constraints

- Design only: no production code, no test, no catalogue change. The column engine, the URL sync, Up and the kind-marked
  breadcrumb in `TopBar`, `activeNavRoute` = the route's lens and the removal of `goBack` / `navigationStack` are the D2
  navigation engine package, staged after this session's merge. The prototype of deliverable 1 is throw-away.
- **Order of edits.** D2 starts after D1's pull request has merged for every document D1 also owns (`README.adoc`,
  `components.adoc`, `plan-overview.adoc`, `project-explorer.adoc`, `interaction-affordances.adoc`,
  `view-density-and-filtering.adoc` is D2's alone). D3 runs beside D2 for its own documents; D3's `README.adoc` and
  `components.adoc` edits come after D2's pull request has merged, and **D3's pull request merges after D2's**.
- Each session edits only the lines it owns. Do not write D1's tag/interaction/field rules or D3's brand edits.
- Every document created or changed is `Status: DRAFT` in the pull request; the operator sets `AGREED` as the last commit
  and the pull request is merged only on the operator's word. **No auto-merge**: the session's finalize stops once the pull
  request is green and the review comments are answered, reports, and waits.
- Review with `pm-documents:ref-documentation` (docs review) plus `pm-documents:ref-asciidoc` (`validate`, `verify-links`),
  clean. Open points go to `plan-marshall-documentation/doc/discussions/open-questions.adoc`.
- No dependency or plugin is added. Work in the session's own worktree and branch; ports are passed explicitly.
- Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`; the pull request body carries no generator footer.

## Claim Labels

- OBSERVED: `doc/design/web-app/view-density-and-filtering.adoc` carries the section `Page Templates Replace the Layout
  Modes` (UX-P-03, :17-44) and `[#layouts] Layout Modes` (:46-73) — quoted by message 004 at its HEAD; present as a file at
  HEAD 60b4bfa5
- HYPOTHESIS: only `DecisionView.svelte` still uses the layout toggle, so the design text can say the template's split is the
  only layout without contradicting a shipped view — confirm/refute at `pm-web-app/src/lib/views/DecisionView.svelte` §
  `ViewLayoutToggle` import and a repo-wide search for `viewLayout` (verify-at-outline; PLAN-111 removes the code, and the
  design sentence must be true by the time PLAN-111 has merged)
- HYPOTHESIS: `goBack()` / `navigationStack` live in `pm-web-app/src/lib/stores/runtime.ts` (~890-915) and the route-param
  `pushState` in `routes/+layout.svelte:228-241` — confirm/refute at those two symbols (verify-at-outline)
- HYPOTHESIS: `activeNavRoute` (`pm-web-app/src/lib/shell/nav.ts:136-152`) maps plan/epic onto Running work always —
  confirm/refute at that symbol (verify-at-outline)
- Verify-first clause: re-read each cited range at the current HEAD before writing the rule it supports; PLAN-97, 104, 106
  and 107 land code while this session runs.

## Expected Surface

- OBSERVED: `doc/design/web-app/navigation.adoc` — absent at HEAD 60b4bfa5; created by this plan
- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 60b4bfa5 (column templates only; after D1)
- OBSERVED: `doc/design/web-app/README.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/view-density-and-filtering.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/needs-and-find.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/web-app/interaction-affordances.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/plan-view.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/cleanup.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/completion-report.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/needs-and-find.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/project-explorer.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/plans/web-moving-between-plans.adoc` — present at HEAD 60b4bfa5 (named by message 010; listing of `doc/plans/` is cut at three entries here, existence of this file is quoted from the message, not listed)
- OBSERVED: `doc/plans/README.adoc` — present at HEAD 60b4bfa5 (its index row for PLAN-83 is updated)

## Dependencies and Sequencing

- Depends on: `PLAN-112` (D1: KindMark, RowLink, row slots, `#acts`, the row edit of `project-explorer.adoc`), and the
  column-window prototype (inside this session)
- Blocks: the D2 navigation engine package; PLAN-49, 51, 52, 55, 58, 59, 60, 62, 65, 85 (and through them 70, 72, 73, 74);
  PLAN-115 (D4 reads §10)
- Overlaps with: `PLAN-114` (D3: `README.adoc`, `components.adoc`, `onboarding.adoc`; D3's merge is after D2's),
  `PLAN-111` (the code removal of the layout modes; its design sentence is true only after it merges)
- Adjacent to: none

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository, after PLAN-112 has merged, and give it exactly this prompt (the orchestrator prints the absolute spec path
at emit time):

```text
Implement the staged design plan PLAN-113 (design D2: navigation) of the epic ui-improvements. Read the whole spec first:
<absolute path of this file>. Then read every source named under "Sources" in full. Follow the spec's Standing Constraints
exactly: design documents only, Status: DRAFT, build the throw-away column-window prototype on the mock runtime and settle
the width table with the operator before AGREED, ref-documentation docs review plus ref-asciidoc validate and verify-links,
one pull request, no auto-merge, stop when the pull request is green and its review comments are answered, and report. Do
not edit anything under .plan/orchestrator/ except your own inbox message (orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only this repository's design documents. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write —
and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write
mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

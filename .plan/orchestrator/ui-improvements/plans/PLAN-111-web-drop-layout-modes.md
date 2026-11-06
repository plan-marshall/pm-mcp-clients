# PLAN-111: Web: Drop the Layout Selector "Comfortable / Compact / Split" (Code)

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, handed off as a direct Claude Code session (operator instruction
> 2026-11-06; not the runbook lane, not `/plan-marshall`). Code-only: the design text is removed by design session D2
> (`PLAN-113`), not here.
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.

## Objective

Operator design review of 2026-11-06 (`design-review-004.md`, archived under `inbox/archive/design-review/`): the selector
"Komfortabel / Kompakt / Geteilt" is dropped completely. The design has already decided this
(`doc/design/web-app/view-density-and-filtering.adoc:17-44`, UX-P-03: "no view keeps a layout toggle or a
`pm_view_layout_<view>` choice … the plan that moves the view removes the mode from it"). Only
`pm-web-app/src/lib/views/DecisionView.svelte` still uses it. This plan removes the toggle component, the layout store with
its test, the texts and the mode branches of the decision view, so that `PLAN-51` (Decisions on the column engine) later
starts from a Decisions view without layout modes. Decisions keeps its single column until then. Pre-1.0: no shim; stale
`pm_view_layout_*` keys in browsers are harmless and are no longer read. **Scope correction from the verification:** the
removal is more than markup and CSS — the split selection state is shared with the command-palette deep-link feature, whose
scroll-to-decision logic must be reworked for the single column, and three palette tests pre-set the split layout.

## Deliverables

1. **Remove the layout modes from the decision view, their store and texts**
   - What: delete `pm-web-app/src/lib/components/ViewLayoutToggle.svelte`, `pm-web-app/src/lib/stores/viewLayout.ts` and
     `viewLayout.test.ts`; in `DecisionView.svelte` remove the imports (:10, :19, :29), the header toggle (~:1246-1250) and
     its row wrapper, the `class:` / `data-layout` attributes (~:1232-1235), the split branch (~:1330-1368), the split
     placeholder (~:1466), `.layout-compact` (~:1474-1483) and the `.decisions-split*` rules (~:1999 media); rework the
     selection effects and the command-palette request effect (:98-134, `selectedDecisionId` / `selectedDecision`, which
     today mount the card only after the selection changes "in the split layout") so a palette deep-link still scrolls to and
     highlights the decision in the single column. Remove `common.layout.label`, `common.layout.{comfortable,compact,split}`
     and their `.title` variants (`common.{de,en}.properties` ~238-244) and `decisions.split.select`
     (`decisions.{de,en}.properties:154`, used at `DecisionView.svelte:1368`). `DataTable.svelte`'s `compact` is its own
     property (VD-P-05) and stays. Tests: `DecisionView.test.ts` loses the four layout tests (:829-887: split list, split
     filter clear, compact actions, layout remembered) and the three palette tests (:900, :918, :946), which pre-set
     `pm_view_layout_decisions` to `split`, are rewritten for the single column; `pm-web-e2e/tests/decisions.spec.js`
     (:217-235, "keeps the actions in compact mode", clicks `layout-compact`) is rewritten or removed. The absence asserts in
     `ReportsView.test.ts` (:650-651) and `reports.spec.js` (:309) are harmless and may stay.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - `git grep -n "pm_view_layout\|common\.layout\|decisions\.split\.select\|ViewLayoutToggle\|viewLayout"` finds nothing in
       `pm-web-app`, `pm-web-e2e` and `pm-api` (historic mentions in `doc/` are D2's and `doc/claude_review/ux/plan.adoc:61`) (evidence order: `none`)
     - A component test shows a command-palette deep-link to a decision scrolls to and marks that decision in the single
       column, without any layout storage set (evidence order: `test-first`)
     - `npm run quality` in `pm-web-app` and the decisions e2e spec pass (evidence order: `none`)
     - Browser check shows `/decisions` in `de` and `en`, light and dark, at 1440 × 900 and 390 × 844 with one column and no
       selector, and a palette deep-link landing on its decision; baseline and after screenshots recorded (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order:
       `none`)

## Standing Constraints

- Build and test through Maven and npm only (`CLAUDE.md`); beside the session's own checks these builds pass at the head:
  `./mvnw -pl pm-web-app verify`, `./mvnw -pl pm-web-e2e verify`, `./mvnw -pl pm-api verify`.
- No new dependency or plugin. Frontend standards (`CLAUDE.md`, `PLAN-80`) apply; a test waits on a condition, never on a
  timer. Both language packs lose the same keys (catalogue parity).
- Ports of this plan: mock web port `21100`, app port `21101` (`AGENTS.md` § Ports, `PLAN-76`). Never stop or reuse a process
  this worktree did not start.
- Browser check following `AGENTS.md` § Browser checks (the session's browser tool; none available means the browser clause
  is NOT met — a blocker, never replaced by a unit test).
- **Sequencing:** `common.{de,en}.properties` is also edited by `PLAN-109` and `PLAN-107` (different keys: lines ~238-244
  here against ~100-101 there): do not build in parallel with `PLAN-109` without a rebase. `PLAN-51` (staged, gated on D1 and
  D2) lists nearly the same files — this plan lands first, and PLAN-51 then starts from a Decisions view without layout modes.
- The design text (`view-density-and-filtering.adoc` `Layout Modes` and `rollback`, `README.adoc:19`,
  `web-app/needs-and-find.adoc:29`) is **not** changed here — D2 (`PLAN-113`) owns it.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`): an open Sonar issue is
  fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: the only importers of `ViewLayoutToggle` / `viewLayout` are `DecisionView.svelte` (:10, :19, :29, :1246); there is
  no `ViewLayoutToggle` test file and no barrel or showroom use — verified by a read-only search at HEAD 60b4bfa5 on 2026-11-06
- OBSERVED: `common.layout.*` is at `common.{de,en}.properties:238-244` (label, three modes, three titles) and
  `decisions.split.select` at `decisions.{de,en}.properties:154`; no Java test or catalogue guard names them — verified at
  HEAD 60b4bfa5 on 2026-11-06 (the message said ~237-245: the last line is 244)
- OBSERVED: `DecisionView.test.ts` has the layout tests at :829-887 and the three palette tests at :900, :918, :946 that
  pre-set `pm_view_layout_decisions`; `decisions.spec.js:217-235` clicks `layout-compact` — verified at HEAD 60b4bfa5 on
  2026-11-06 (neither was named by the message)
- OBSERVED: `DecisionView.svelte:98-134` couples the selection effects and the palette-request effect to the split mode —
  verified at HEAD 60b4bfa5 on 2026-11-06 (the message said the removal is markup / CSS only: refuted, understated)
- HYPOTHESIS: the single column keeps the palette deep-link usable after the rework — confirm/refute at
  `DecisionView.svelte` § the palette-request effect and the rewritten tests (verify-at-outline)
- Verify-first clause: re-read the named ranges at the current HEAD before editing; `PLAN-51` (staged, gated) edits the same
  files after this plan.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/components/ViewLayoutToggle.svelte` — present at HEAD 60b4bfa5; deleted by this plan
- OBSERVED: `pm-web-app/src/lib/stores/viewLayout.ts` — present at HEAD 60b4bfa5; deleted by this plan
- OBSERVED: `pm-web-app/src/lib/stores/viewLayout.test.ts` — present at HEAD 60b4bfa5; deleted by this plan
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.svelte` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-app/src/lib/views/DecisionView.test.ts` — present at HEAD 60b4bfa5
- OBSERVED: `pm-web-e2e/tests/decisions.spec.js` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/decisions.de.properties` — present at HEAD 60b4bfa5
- OBSERVED: `pm-api/src/main/resources/messages/decisions.en.properties` — present at HEAD 60b4bfa5

## Dependencies and Sequencing

- Depends on: none (no running plan lists these files; `PLAN-97`, `PLAN-104`, `PLAN-106` share nothing)
- Blocks: `PLAN-51` (starts from a Decisions view without layout modes)
- Overlaps with: `PLAN-109` (`common.*.properties`), `PLAN-107` (`common.*.properties`, different keys), `PLAN-51` (near-total
  overlap; staged and gated), `PLAN-113` (D2 removes the design text)
- Adjacent to: none

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository and give it exactly this prompt (the orchestrator prints the absolute spec path at emit time):

```text
Implement the staged plan PLAN-111 (drop the layout modes) of the epic ui-improvements. Read the whole spec first:
<absolute path of this file>. Follow its deliverable, Done-when clauses and Standing Constraints exactly, working test-first
where a clause says so, through Maven and npm only. Run the browser check it names. Open one pull request, answer its review
comments, and report. Do not edit anything under .plan/orchestrator/ except your own inbox message (orchestrator inbox
write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under
`.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write —
and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write
mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

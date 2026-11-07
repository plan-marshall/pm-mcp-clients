# PLAN-108: Web: Design Documents Residue (Reports Landing Leftovers, Time Wording of the Shared Wireframes)

epic: ui-improvements
workstream: WS-06

> Staged plan spec — one shippable unit of work, handed off as a direct Claude Code session (operator instruction
> 2026-11-06; not the runbook lane, not `/plan-marshall`).
> This spec is SELF-SUFFICIENT: the prompt below is a one-line pointer and carries no brief.

## Objective

Split off `PLAN-74` by the design review of 2026-11-06 (`design-review-010.md` § Per-plan instructions, PLAN-74): PLAN-74's
deliverables 3 and 4 are residue fixes with **no dependency on PLAN-72/73**. They run now, **before design session D1**
(PLAN-112), so D1 rewrites `components.adoc` from a clean base and the two do not edit it in parallel. They do **not** run
in parallel with PLAN-107 (both touch `RowMenu.test.ts`). PLAN-74 keeps deliverable 2 (the proposal-status list) and
setting `FINAL`, after D1–D4 and their implementations; its deliverable 1 is dropped (D1 and D2 write the README).

## Deliverables

1. **Design leftovers of the reports landing (from PLAN-102; PLAN-74 deliverable 3)**
   - What: (a) `doc/design/web-app/components.adoc` ~59 names `PLAN-100` in lasting design text: state the reason without a
     plan number. (b) `RowMenu` skips disabled entries on the arrow keys while `components.adoc` ~80 says the ActionBar's
     arrow keys reach disabled entries with their reason: name the divergence or remove it. (c)
     `doc/resources/diagrams/web-component-row-menu.svg` draws "Copy link" and "Archive", which the product menu does not
     have, and its annotation labels (x=290 in a 360-wide viewBox) probably clip: redraw from the shipped menu. (e) From
     the landing of PLAN-105 (`landings/PLAN-105.md` defect 14): the product `RowMenu` popup is `position:absolute;
     z-index:10` with no collision handling while `BottomBar` is sticky at `z-index:20` at ≤ 767 px, so a row near the
     bottom of the viewport can open its menu under the bar: decide in `components.adoc` and fix with a measured test at
     390 px. (d) The reports token filter (`queryTokens`, a token with a letter or digit) is local to `ReportsView`: state
     the shared filter rule once in the design and in `utils/filterText.ts`, so other filterable views treat "..." the same
     way (`landings/PLAN-102.md` defects 14 and 16).
   - Persona: plan-marshall:persona-implementer (design text and the 390 px fix)
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation, pm-documents:ref-svg-diagrams,
     pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - The documents pass `pm-documents:ref-documentation` docs review and `pm-documents:ref-asciidoc` `validate` and
       `verify-links`, clean, and the diagram matches the shipped menu (evidence order: `none`)
     - A measured test at 390 px shows a row at the bottom of the viewport opening its menu where the sticky `BottomBar` does
       not cover it (evidence order: `test-first`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order:
       `none`)
2. **Shared designs write times in the form of the rule (from the verification of PLAN-18; PLAN-74 deliverable 4)**
   - What: `doc/design/shared/texts-and-languages.adoc#time` (lines ~127-194) specifies an age in a list as `9 d ago` /
     `vor 9 T` with the units `s`, `min`, `h`, `d` and no unit for weeks, but the shared wireframes write other forms
     (verified by the orchestrator at `origin/main` 2026-11-06): `work-actions.adoc:332` ("2 days ago"),
     `paired-devices.adoc:24` ("last used 5 weeks ago"), `project-explorer.adoc:31` ("5 weeks ago"). The rule for ages is
     decided by PLAN-34 deliverable 4; **PLAN-34 has not landed, so this plan decides it here** and PLAN-34 deliverable 4
     is then reduced to applying the decided rule to the client designs. The rule is silent beyond 30 days and on
     `yesterday` and `just now`, while the web `format.ts` renders months through `Intl.RelativeTimeFormat` and
     `ProjectsCommand` prints `yesterday`, `N weeks ago`, `N months ago`, `just now` (`landings/PLAN-18.md` defect 4):
     either add rows for months, `yesterday` and `just now` (and weeks if wanted), in the German pack too, binding
     `format.ts` and `ProjectsCommand`, or rewrite everything to `N d ago`; state the choice in the pull request and in the
     operator's Open Defects entry. Then rewrite the shared wireframes in that form, including `project-explorer.adoc:26, 71`
     (a second statement of the rule) and `plan-overview.adoc:141`. The client designs (`terminal-client/08`, `15`, `17`,
     `18`, `19`) stay PLAN-34's deliverable 4 (and `19-actions.adoc` is PLAN-97's until it lands).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `git grep -nE "(weeks?|months?|years?) ago|[ (]yesterday[ )]?$" -- doc/design/shared` names no form the decided
       rule does not have (a retention setting in days is not one) (evidence order: `none`)
     - The open-issue read at the pull request head is recorded: the open Sonar issue count and the rules left open
       (`unknown` for a failed read, never `0`); it is a confirmed `0` or every open issue is dismissed (evidence order:
       `none`)

## Standing Constraints

- Build and test through npm scripts and Maven only (`CLAUDE.md`); beside the session's own checks these module builds pass
  at the head: `./mvnw -pl pm-web-app verify` (deliverable 1 touches `RowMenu`).
- No new dependency or plugin.
- Frontend standards (`CLAUDE.md`, `PLAN-80`): `pm-dev-frontend` and `pm-dev-frontend-cui` apply to `pm-web-app`; no
  `innerHTML` / `{@html}`; a test waits on a condition, never on an arbitrary timer.
- Sequencing: **before PLAN-112 (D1)**; **not in parallel with PLAN-107** (`RowMenu.test.ts`); the 001 / 004 runbooks
  (PLAN-109, PLAN-111) touch only `common.*.properties` and are not in this plan's surface.
- Document changes use `Status` per `doc/design/README.adoc:20`: documents that are already `AGREED` stay `AGREED`; residue
  edits are corrections, not new design.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch. Ports are
  passed explicitly (`AGENTS.md` § Ports); never stop a process this worktree did not start.
- The pull request is merged only on the operator's word; commits end with `Co-Authored-By: plan-marshall
  <noreply@cuioss.de>`; the pull request body carries no generator footer.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`): an open Sonar issue is
  fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` and transmitted through
  `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`.
  The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed.

## Claim Labels

- OBSERVED: `doc/design/web-app/components.adoc` and `doc/design/shared/{texts-and-languages,work-actions,paired-devices,project-explorer,plan-overview}.adoc`
  exist — listed at HEAD 60b4bfa5
- HYPOTHESIS: `components.adoc` ~59 names `PLAN-100`, ~80 says the ActionBar's arrow keys reach disabled entries, and the
  `RowMenu` popup is `position:absolute; z-index:10` under a sticky `BottomBar` at `z-index:20` — quoted from PLAN-74 and
  `landings/PLAN-105.md` defect 14 — confirm/refute at `doc/design/web-app/components.adoc` §§ cited and
  `pm-web-app/src/lib/components/RowMenu.svelte` § popup CSS (verify-at-outline)
- HYPOTHESIS: `doc/resources/diagrams/web-component-row-menu.svg` draws "Copy link" and "Archive" — confirm/refute at that
  file § text nodes (verify-at-outline)
- HYPOTHESIS: `queryTokens` is local to `ReportsView` and `utils/filterText.ts` exists — confirm/refute at
  `pm-web-app/src/lib/views/ReportsView.svelte` § `queryTokens` and `pm-web-app/src/lib/utils/filterText.ts` (verify-at-outline)
- HYPOTHESIS: the wireframe lines `work-actions.adoc:332`, `paired-devices.adoc:24`, `project-explorer.adoc:26,31,71`,
  `plan-overview.adoc:141` carry the forms the deliverable names — confirm/refute at those lines (verify-at-outline)
- HYPOTHESIS: PLAN-34 deliverable 4 has not landed (its queue row is `staged`) — confirm/refute at `queue/PLAN-34.json` (observed
  `staged` in this epic's ledger at 2026-11-06) and `doc/design/shared/texts-and-languages.adoc#time` (verify-at-outline)
- Verify-first clause: re-read each cited range at the current HEAD; PLAN-97, 104, 106 and 107 land code while this plan runs.

## Expected Surface

- OBSERVED: `doc/design/web-app/components.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/texts-and-languages.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/work-actions.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/paired-devices.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/project-explorer.adoc` — present at HEAD 60b4bfa5
- OBSERVED: `doc/design/shared/plan-overview.adoc` — present at HEAD 60b4bfa5
- HYPOTHESIS: `doc/resources/diagrams/web-component-row-menu.svg` — named by PLAN-74 deliverable 3; verify at outline
- HYPOTHESIS: `pm-web-app/src/lib/components/RowMenu.svelte` and `RowMenu.test.ts` — named by PLAN-74 deliverable 3 (the 390 px fix); verify at outline
- HYPOTHESIS: `pm-web-app/src/lib/utils/filterText.ts` and `ReportsView.svelte` — named by PLAN-74 deliverable 3 (d); verify at outline
- HYPOTHESIS: `pm-web-app/src/lib/display/format.ts` and the operator's `ProjectsCommand` class (path not resolved at staging) — bound by the time-rule choice of deliverable 2 only if rows are added; verify at outline

## Dependencies and Sequencing

- Depends on: `PLAN-102`, `PLAN-105` (shipped), `PLAN-18` (shipped); `PLAN-97` for any edit of `terminal-client/19-actions.adoc`
  (none planned here)
- Blocks: `PLAN-112` (D1 starts after this merged)
- Overlaps with: `PLAN-107` (`RowMenu.test.ts` — not in parallel), `PLAN-34` (deliverable 4: the time rule is decided here),
  `PLAN-74` (deliverables 3 and 4 moved out)
- Adjacent to: none

## Hand-Off Command

Lane: none — a direct Claude Code session (operator instruction 2026-11-06). Start it in its own worktree and branch of
this repository and give it exactly this prompt (the orchestrator prints the absolute spec path at emit time):

```text
Implement the staged plan PLAN-108 (web design documents residue) of the epic ui-improvements. Read the whole spec first:
<absolute path of this file>. Follow its deliverables, Done-when clauses and Standing Constraints exactly, working test-first
where a clause says so, through Maven and npm only. Open one pull request, answer its review comments, and report. Do not
edit anything under .plan/orchestrator/ except your own inbox message (orchestrator inbox write), and do not merge.
```

## Write-Boundary

The session implementing this spec touches only its own repository source, tests and design documents. It creates and edits
NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other
ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole
sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

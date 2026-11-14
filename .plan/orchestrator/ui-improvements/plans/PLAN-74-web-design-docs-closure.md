# PLAN-74: Web: Design Documents Closed

epic: ui-improvements
workstream: WS-03

> Staged plan spec — one shippable unit of work, ready for hand-off on the `runbook` lane.
> This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

Close the design documents of the web review: rewrite "Visual Language", update view density and interaction affordances, add `components.adoc`, add the next-action rule to the principles, and mark every proposal done or dropped with a reason (UX-S-13, VD-S-09).

Source of this plan (read before scoping):

- `doc/claude_review/ux/plan.adoc (UX-S-13)`
- `doc/claude_review/visual-design/plan.adoc#VD-S-09`
- `doc/claude_review/visual-design/proposals.adoc#VD-P-09`

## Deliverables

1. **Web-app design**
   - What: `doc/design/web-app/README.adoc`: structure, principles with the next-action rule, Visual Language v2. (The other design documents are written by the plans that do the work, as the repository rule says: `view-density-and-filtering.adoc` by `PLAN-43` (templates replace layout modes), `interaction-affordances.adoc` by `PLAN-47` (overflow menu, keyboard), `components.adoc` by `PLAN-35`; this plan only checks that they exist and are linked from the README.)
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - The documents pass `doc-review` (under the runbook lane its checker `trace.py` does not run, so `pm-documents:ref-documentation` `docs review` and `pm-documents:ref-asciidoc` `validate` and `verify-links` stand in for it, clean, as PLAN-06 and PLAN-07 ran them), and `components.adoc`, `view-density-and-filtering.adoc` and `interaction-affordances.adoc` are linked from the README and state what the shipped views do (evidence order: `none`)
2. **Proposal status**
   - What: Every proposal of `doc/claude_review/{ux,visual-design,cross-cutting,use-cases}/proposals.adoc` is listed as done (with its plan) or dropped (with a reason) in `doc/design/web-app/README.adoc`.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - No proposal is unaccounted for (evidence order: `none`)
3. **Design leftovers of the reports landing (from PLAN-102)**
   - What: (a) `doc/design/web-app/components.adoc` ~59 names `PLAN-100` in lasting design text: state the reason without a plan number. (b) `RowMenu` skips disabled entries on the arrow keys while `components.adoc` ~80 says the ActionBar's arrow keys reach disabled entries with their reason: name the divergence or remove it. (c) `doc/resources/diagrams/web-component-row-menu.svg` draws "Copy link" and "Archive", which the product menu does not have, and its annotation labels (x=290 in a 360-wide viewBox) probably clip: redraw from the shipped menu. (e) From the landing of PLAN-105 (`landings/PLAN-105.md` defect 14): the product `RowMenu` popup is `position:absolute; z-index:10` with no collision handling while `BottomBar` is sticky at `z-index:20` at ≤767 px, so a row near the bottom of the viewport can open its menu under the bar (L: the landing's "refuted" rests on a fixture CSS change only): decide in `components.adoc` and fix with a measured test at 390 px. (d) The reports token filter (`queryTokens`, a token with a letter or digit) is local to `ReportsView`; state the shared filter rule once in the design and in `utils/filterText.ts`, so other filterable views treat "..." the same way (`landings/PLAN-102.md` defects 14 and 16).
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation, pm-documents:ref-svg-diagrams
   - Done when:
     - The documents pass the checks of deliverable 1 and the diagram matches the shipped menu (evidence order: `none`)
4. **Shared designs write times in the form of the rule (from the verification of PLAN-18)**
   - What: `doc/design/shared/texts-and-languages.adoc#time` (lines ~127-194) specifies an age in a list as `9 d ago` / `vor 9 T` with the units `s`, `min`, `h`, `d` and no unit for weeks, but the shared wireframes write other forms (`inbox/plan-18-verify-shared-foundation-001.md`, verified by the orchestrator at `origin/main` 2026-11-06): `work-actions.adoc:332` ("2 days ago"), `paired-devices.adoc:24` ("last used 5 weeks ago"), `project-explorer.adoc:31` ("5 weeks ago"). The rule for ages is decided by PLAN-34 deliverable 4 (it lands first: this plan waits on a long chain); if it has not landed, decide it here. The rule is silent beyond 30 days and on `yesterday` and `just now`, while the web `format.ts` renders months through `Intl.RelativeTimeFormat` and `ProjectsCommand` prints `yesterday`, `N weeks ago`, `N months ago`, `just now` (`landings/PLAN-18.md` defect 4): either add rows for months, `yesterday` and `just now` (and weeks if wanted), in the German pack too, binding `format.ts` and `ProjectsCommand`, or rewrite everything to `N d ago`; state the choice in the pull request and in the operator's Open Defects entry. Then rewrite the shared wireframes in that form, including `project-explorer.adoc:26, 71` (a second statement of the rule) and `plan-overview.adoc:141`. The client designs (`terminal-client/08`, `15`, `17`, `18`, `19`) are PLAN-34's deliverable 4.
   - Persona: plan-marshall:persona-documenter
   - Skills: pm-documents:ref-asciidoc, pm-documents:ref-documentation
   - Done when:
     - `git grep -nE "(weeks?|months?|years?) ago|[ (]yesterday[ )]?$" -- doc/design/shared` names no form the decided rule does not have (a retention setting in days is not one) (evidence order: `none`)
## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): the pull request merges with 0 open new-code issues (SonarCloud project `plan-marshall_pm-mcp-clients`), read at the pull request head after the push (`unknown` for a failed read, never `0`); every open issue is fixed in a commit of THIS pull request, and a dismissal is made only on the operator's selection. State `sonar_open_new_issues=N` in the landing message. This is a merge gate, not a deliverable's Done-when.
- No new dependency or plugin unless this spec names it as approved by the operator.
- This project analyses its pull requests with SonarCloud (project `plan-marshall_pm-mcp-clients`, `.github/project.yml`; provider `plan-marshall:workflow-integration-sonar` and step `default:sonar-roundtrip` in `.plan/marshal.json`) — treat it as `sonar: configured` even where `runbook_lane resolve` reports `not_configured` (its `credentials_config` block is missing; see the epic's Watches). An open Sonar issue is fixed in a pushed commit, or dismissed with a stated reason recorded as `{issue-key}: {reason}` in the run's progress record and transmitted through the plan-less transition verb, `plan-marshall:workflow-integration-sonar:sonar_rest transition --issue-key {key} --transition {falsepositive|wontfix}`. A dismissal the run cannot transmit is a blocker, never a silent pass. The merge gate reads the open-issue count at the head — `0` confirmed, or every remaining issue dismissed — and an `undecidable` read blocks, with one exception. CI skips `build`, `quarkus-alignment` and `sonar-build` when every changed file of the pull request matches the organisation's docs-only ignore list (`**/*.adoc`, `**/*.md`, `doc/**`, `.plan/**` and the further globs of `build-paths-filter-spec`), because such files change nothing a build or an analysis produces. When the pull request's changed files all match that list, `build / conclusion` passed and `build / sonar-build` is skipped, the `undecidable` read is the expected outcome: record it as `undecidable (docs-only skip)` and it does not block. A changed file outside the list runs the full build, and the rule above applies unchanged.

## Claim Labels

- OBSERVED: the review of 2026-10-25 states this work in `doc/claude_review/ux/plan.adoc (UX-S-13)`, `doc/claude_review/visual-design/plan.adoc#VD-S-09` — read at `doc/claude_review/ux/plan.adoc`
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: ux/plan.adoc:86 UX-S-13; visual-design/plan.adoc:69 VD-S-09; proposals.adoc:217-218 VD-P-09; deliverables 3,4 done by PLAN-108 ddcab793, 1 dropped
- Re-grounded note at HEAD 94bbefe1 (cleanup 2026-11-02): doc/design/web-app/components.adoc now exists (added by PLAN-35, 5650c41e), so the 'add components.adoc' part of the deliverables is already partly done and must be reworked as 'update'; README.adoc, brand-mark.adoc and theme-readability.adoc exist and changed since the anchor.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: doc/design/web-app/components.adoc exists (plus components-tags/fields/brand/chat), README.adoc, brand-mark.adoc, theme-readability.adoc all present
- RESCOPE at outline (design audit 2026-11-02): no done/dropped list with PLAN ids in web-app/README (Z2); the web-app design status FINAL becomes AGREED (R7-3/R7-4), and the same rule covers the new web-app/components.adoc:15 FINAL.
  - verdict: corroborated | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: README.adoc:12 and components.adoc:15 Status AGREED; README has no done/dropped proposal list (only PLAN ids in routes row :485)
- Verify-first clause: the review was written against HEAD `c4dc9bb4` and read code only to confirm causes. Before each deliverable, re-read the named files at the current HEAD and confirm or refute every HYPOTHESIS above; a refuted cause re-scopes the deliverable and is reported, never silently worked around.
  - verdict: unverifiable | checked_at: dc811fb3 | by: ui-improvements/cleanup | rescoped: n/a | evidence: Procedural verify-first clause; no code fact to settle

## Expected Surface


- OBSERVED: `doc/design/web-app/` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/ux/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/visual-design/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/cross-cutting/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/use-cases/analysis.adoc` — present at HEAD 1cc0e10e

## Dependencies and Sequencing

- Depends on: `PLAN-72` (`web-token-cleanup-lint`), `PLAN-73` (`web-text-guard-failing`)
- Overlaps with: none known at staging
- Adjacent to: none

## Design Review 2026-11-06 (Amendment)

Source: `inbox/archive/design-review/design-review-010.md` § Per-plan instructions (PLAN-74). **Scope change:**

- **Deliverable 1 is dropped** (writing the `web-app/README.adoc` structure, principles and visual language): D1 and D2 write
  the README.
- **Deliverables 3 and 4 move out** to the new runbook `PLAN-108` (`web-design-docs-residue`), which has no dependency on
  `PLAN-72` / `PLAN-73` and runs before D1; not in parallel with `PLAN-107` (both touch `RowMenu.test.ts`).
- **It keeps deliverable 2** (the proposal-status list) **and setting `FINAL`**, after D1–D4 and their implementations: it
  closes the docs; it does not write them. The Dependencies line above is replaced by: D1–D4 merged and their implementation
  packages landed, then `PLAN-72`, `PLAN-73`.

Queue status: `parked`; the orchestrator re-grounds this spec (deliverables, Expected Surface) when it is restarted.

## Re-Grounding 2026-11-14 (cleanup)

Verdicts persisted per claim at `dc811fb3` (`corpus set-verdict`, by `ui-improvements/cleanup`). Read before the outline:

- Already done: D3 and D4 (ddcab793, PLAN-108 #362); README and `components.adoc` already read AGREED. Re-scope at outline to what is left.

## Hand-Off Command

Lane: `runbook` (run with opencode).

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-74-web-design-docs-closure.md
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests (and, where a deliverable says so,
the sibling repository `plan-marshall-documentation`). It creates and edits NO file under `.plan/orchestrator/` other than its own
`inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports
its outcome through its PR and its inbox message. The inbox exception's qualifiers and the
sole sanctioned write mechanism are stated in
`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

- OBSERVED: `doc/design/web-app/` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/intent-and-outline.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/ux/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/visual-design/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/cross-cutting/analysis.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/claude_review/use-cases/analysis.adoc` — present at HEAD 1cc0e10e

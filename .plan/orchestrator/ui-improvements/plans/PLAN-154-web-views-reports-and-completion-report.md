# PLAN-154: Web Views: Reports and the Completion Report on the D1 Vocabulary (Views Package, Part 2)

epic: ui-improvements
workstream: WS-03

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents and `shared/reports.adoc`, `shared/completion-report.adoc`.
> SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

`ReportsView` and `CompletionReportView` already use `Button`, `EmptyState`, `ErrorState`, `FilterInput`, `RowMenu`, `Skeleton` and `StatusLabel`, but their rows, group heads, head lines, sections and token tables still carry hand-drawn
tag-like spans and glyph marks, and their rows do not follow the fixed place of each tag. This plan moves both onto the D1 vocabulary and the row and head slots of `components-tags.adoc` § `#tag-places`, without changing what they show.
The mascot's calm empty state is PLAN-131's, the column engine and `/reports/{id}` are D2's.

## Deliverables

Read first: `doc/design/web-app/components-tags.adoc` (§ `#tag-set`, `#kind-mark`, `#tag-places`, `#tag-words`), `components.adoc` (§ `#row-menu`, `#fixed-places`, `#labels`, `#containers`, `#data`), `shared/reports.adoc` (§ `#fields`, `#list`, `#read`),
`shared/completion-report.adoc` (§ `#layout`, `#tokens`), `view-density-and-filtering.adoc` § `#filter-control`.

Not in this plan: the `RowLink` row and `aria-current` selection of the Reports lens (they need the address `/reports/{id}`, D2); the `mascot` poses of the empty states (PLAN-131); the mark-all and read-race logic (PLAN-129); the column engine, `?open=` and the heading outline (D2).

1. **Reports: the row and the group**
 - What:
   - A row follows the fixed place: `KindMark` of the subject's kind (project, epic or plan; icon only, `wordHidden`), the subject, then on the right the `StatusLabel` read or unread word, the time (relative style), and the `RowMenu` last. "Details" stays a secondary `Button`
     (`report.action.details`). The unread dot and semibold weight stay; the read state of a retained row names `read` in words.
   - A group head is a heading with the project, epic or plan name and no count and no act (`reports.adoc` § `#list`). The path `project › epic › plan` is `Link`s with their `href`.
   - The row menu is the `IconButton ⋯` with the row's subject in its accessible name; its entries and their disabled reasons come from the state table. The word beside the glyph is gone (PLAN-149 removed `visibleLabel`).
   - The hand-drawn spans and glyphs of the file become tags of the closed family or detail text; the `↑ ↓ × +` style glyph labels become icons with a catalogue name.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `ReportsView.test.ts` and `reports.spec.js` on `reports-and-system` show the slot order of a row, the read state in words, the group heads without a count, and the same marking behaviour as before. Fail on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for `ReportsView.svelte`; a tag-class scan of the file is empty (evidence order: `none`).
 - Implements: `components-tags.adoc` § `#tag-places`, `#kind-mark`; `components.adoc` § `#fixed-places`, `#row-menu`; `shared/reports.adoc` § `#fields`, `#list`.
2. **Reports: head, filter, paging and states**
 - What:
   - The head shows _Mark all shown read_ as a secondary `Button` and the unread count as text; the filter is `FilterInput` with the shared label key (`common.filter.label`, PLAN-152); _Show older_ names how many more there are, as a secondary `Button`.
   - Loading, empty and failed states use `Skeleton`, `EmptyState` and `ErrorState` in the pattern of PLAN-45; the empty state keeps an icon (the `mascot` prop is PLAN-131's and the four calm empty states are placed by their own plans).
   - No change to the order, the unread-only rule, the retained row or the paging.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `ReportsView.test.ts`: the head's acts by role, the filter label and count, the paging line, the three states. Fails on a revert. Evidence order: `test-first`.
 - Implements: `components.adoc` § `#hierarchy`, `#data`; `view-density-and-filtering.adoc` § `#filter-control`.
3. **Completion report: head and sections**
 - What:
   - The head follows the object head: `KindMark` (plan), the title, `Chip` for the plan id, `PrChip` for its pull request, the outcome as `StatusLabel`, the end time; _Its plan view_ is a `Link`, _Chat_ a secondary `Button`.
   - The sections (Outcome, Reason, Ran, Change, Deliverables, Re-planning, Decisions, Own calls, Change map, Tokens) are `KeyValueList` rows inside `Card`s without a card in a card; the deliverables' `✓ ○ –` marks are icons with their word; the change map stays preformatted text exactly as delivered.
   - The 6 hand-drawn tag-like spans of the file become tags of the family or detail text.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - `CompletionReportView.test.ts`: head slots, every section's label and value, a deliverable in each of done, not done and dropped (icon and word), the change map unchanged, a plan with no deliverables yet. Fails on a revert. Evidence order: `test-first`.
   - `npm run lint:vocabulary` reports 0 findings for `CompletionReportView.svelte`; a tag-class scan of the file is empty (evidence order: `none`).
 - Implements: `components.adoc` § `#containers`; `components-tags.adoc` § `#tag-places`; `shared/completion-report.adoc` § `#layout`.
4. **Completion report: tokens and browser evidence**
 - What: the Tokens section (total, coverage, by phase, by role, by model, waste) as `DataTable` or `KeyValueList`, numbers right-aligned in tabular figures, the floor sign `≥` and `not measured` as delivered; each _By phase_ line is a `Link` to that phase in the plan view as it ended.
   Both views are checked in a browser.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `CompletionReportView.test.ts`: the floor sign, `not measured`, a model with unmeasured tasks, the coverage line, the phase link. Evidence order: `test-first`.
   - Browser check (Playwright MCP) on `reports-and-system` and `running-work` at 1440×900 and 390×844, `de` and `en`, both themes; baseline and after screenshots kept in the plan's working area, re-taken after any later visual fix (the pull request lists them after the push; not a gate before it); `theme-readability.spec.js` passes for both views (evidence order: `none`).

5. **What PLAN-158 left in the reports and the designer (from its landing, folded 2026-11-11 on the operator's decision)**
 - What: read `landings/PLAN-158.md` of the epic; lines as read at `4a84f727`. Three CodeRabbit findings of #408 that were
   never answered, and the rest of the residue:
   - `ProcessDesigner.svelte`: `placeOutcomeUnknown` is reset only in `place()` (`:821`); after a placement timeout, Close,
     reopen and a failed comparison, the failed stage (`:1550`) reads as an unconfirmed placement. Reset it when a
     comparison starts (`compareForPlacement`, `:783-795`).
   - `ReportsView.svelte`: a successful single Mark read (`:571-584`) does not advance `listEpoch` (only the events at
     `:390`, `:407` do), so a list read issued before the POST and answered after it restores the read row and the old
     `unread_count`. Advance the epoch (or discard the older snapshot) for a local read.
   - `ReportsView.svelte`: a retained row whose `markRead` fails while a mark-all holds it keeps its marker (`:589-590`), and
     the mark-all's rollback skips it (`:622-629`, `:662-665`); if both fail, the row shows read though nothing was read.
     When the last hold settles, clear an unconfirmed marker whoever created it.
   - The failing-Retry arm of the focus rule (`:507-514`: focus on the error title) has no test.
   - `errors.ts` `actFailureText` (`:90-92`): a wire-shaped code the catalogue lacks yields `error.unknown.fix` ("look at the
     details") on an act line that shows no details, and the act's own text is lost: use the act's fallback for an unknown code.
   - `ReportsCommandTest.java`: four of five tests keep hand-written ids and counts (`:177-230`); draw them from `Generators`.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-java:junit-core, pm-dev-java-cui:cui-testing
 - Done when:
   - A test per item fails before the change (evidence order: `test-first`); the `ReportsCommandTest` change is test-only
     (evidence order: `none`).

Not split: five deliverables; deliverable 5 is PLAN-158's residue the operator folded here (2026-11-11). Rationale recorded by the orchestrator.

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text (a report body, a title, a reason or a file name is untrusted text), no timer waits in tests, the npm scripts stay.
- A pure restyle: no change to what a view shows, sends or stores; where a rule of the design cannot be met without a behaviour change, record it as a finding in the pull request.
- Every new text comes from the catalogue in English and German; the text guard baseline does not grow.
- Ports of this plan: mock web port 21540, app port 21541 (`20000 + 10 × 154`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: `ReportsView.svelte` (1509 lines) uses `Button`, `EmptyState`, `ErrorState`, `FilterInput`, `Icon`, `IconButton`, `RowMenu`, `Skeleton` and has no raw `<button>`, `<a>` or `<input>` — read at the imports and by `git grep -c`
- OBSERVED: `CompletionReportView.svelte` (989 lines) uses `Button`, `Icon`, `Spinner`, `StatusLabel`, has no raw tag, and carries 6 lines with tag-like class names — read at the file and by `git grep -cE 'class="[^"]*(badge|pill|chip|tag|state-dot|status)'`
- OBSERVED: `routes.ts` knows `/reports` and `/reports?plan=` and no `/reports/{id}` — read at `pm-web-app/src/lib/routes.ts:312-315`
- OBSERVED: `PLAN-129` (staged) rewrites the mark-all rollback and retention in `ReportsView.svelte`, and its D2 touches `CompletionReportView.svelte` (focus); `PLAN-131` (staged) places the mascot in `ReportsView` — read at both specs
- OBSERVED: `ReportsView.svelte:1171` passes `visibleLabel` and nine `size="small"` values that PLAN-149 removes — read by `git grep`
- HYPOTHESIS: the unread dot and the semibold subject are drawn by hand in `ReportsView` and move to a `StatusLabel` and a class of the row — confirm/refute at `ReportsView.svelte` § row markup (verify-at-outline)
- HYPOTHESIS: the Tokens section of `CompletionReportView` is already a table of its own — confirm/refute at `CompletionReportView.svelte` § tokens (verify-at-outline)
- Verify-first clause: re-read both views after PLAN-45, PLAN-129, PLAN-131, PLAN-149 to PLAN-152 landed; take the PLAN-151 lint list and a tag-class scan as the work list. If PLAN-129 moved the retention code, rebase deliverable 1 on it.

## Expected Surface

- OBSERVED: `pm-web-app/src/lib/views/ReportsView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ReportsView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/CompletionReportView.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/reports.spec.js` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-e2e/tests/theme-readability.spec.js` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-api/src/main/resources/messages/reports.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/reports.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/reports.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/shared/completion-report.adoc` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/views/ProcessDesigner.svelte` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-web-app/src/lib/views/ProcessDesigner.test.ts` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-web-app/src/lib/api/errors.ts` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-web-app/src/lib/api/errors.test.ts` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)
- OBSERVED: `pm-operator/src/test/java/de/planmarshall/operator/ReportsCommandTest.java` — present at `4a84f727` (PLAN-158 residue, folded 2026-11-11)

## Dependencies and Sequencing

- Depends on: `PLAN-45` landed; `PLAN-129` landed (same two views); `PLAN-131` landed (`ReportsView` empty state); `PLAN-148` (`KindMark` via PLAN-147, `ReasonBadge`, `PhaseLabel`, `StatusLabel`), `PLAN-149`, `PLAN-150`, `PLAN-151` (the count), `PLAN-152` (`common.filter.label`).
- Overlaps with:
  - `PLAN-45` (running): `views/`, `ReportsView.test.ts`.
  - `PLAN-129` (staged): `ReportsView`, `CompletionReportView`, `RowMenu`.
  - `PLAN-131` (staged): `ReportsView`.
  - `PLAN-49`, `PLAN-51`, `PLAN-52` (parked): their calm empty states take `mascot` at their own re-ground.
  - `PLAN-136` (parked): `OverviewView` and `PlanView`, not these views.
- Adjacent to: D2 (the Reports lens, `/reports/{id}`, the completion report as an object column); the terminal reports.
- Unblocks: the gate "views package implemented" of PLAN-29, 41 and 65.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since (PLAN-45, PLAN-129, PLAN-131, PLAN-148 to PLAN-152). Settle each HYPOTHESIS. Re-run the lint for both views.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-154-web-views-reports-and-completion-report.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

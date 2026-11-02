# PLAN-43 — Web: Four Page Templates and One Reference View — landing

PR #301, merged into `main` as `2c25ed1d` (28 files, +1465/−79) through the merge queue. Lane message `rb-43-web-page-templates-001`.
Analysed in three layers (specified / implemented / correct) by reading; nothing was built or run by the orchestrator.

## Verdict

| Layer | Verdict |
|---|---|
| 1 Specified | D1, D2 and D3 met in substance. D3's "fails before the move" is met by the control fixtures, not by the order the spec words (the lane says so). |
| 2 Implemented | All declared paths present (the templates directory with nine files, `SystemView.svelte`, `pageWidth.test.ts`, `view-density-and-filtering.adoc`). 17 files beyond the declaration, one of them outside the area (a pm-api test, operator-approved in the lane). No dependency added. |
| 3 Correct | The four templates match UX-P-03 by reading. The reference view uses two of the template's four regions; the tests read stylesheet text, so no template but the dashboard has been drawn by a browser. Three leads, none reproduced. |

## Layer 1 and 2 — what was specified, what the diff holds

- **D1 templates.** `lib/templates/{Triage,Object,Wizard,Dashboard}Page.svelte` with slots, against UX-P-03: Triage is full width, columns `2fr 3fr` (40 / 60) and below 1100 px (`max-width: 1099px`) the detail replaces the list through a `selected` prop; Object is 1440 px centred with kind, path, title (`h1`), id chip, state, three act slots, attention, tabs and body; Wizard is 720 px centred with the step bar, one card and a footer (Back, secondary, primary) that is left out when no footer slot is filled; Dashboard is 1440 px centred with title, controls, a card grid and an "updated" slot. Every template takes the 12 px inset below 480 px. `view-density-and-filtering.adoc` (+29) states that the templates replace the layout modes. Four test files, one per template (slots, width, collapse, inset, the no-filters and steps-only variants). The red run is recorded: four files failed to resolve the missing modules at `1ce32d80`.
- **D2 reference view.** `SystemView` renders inside `DashboardPage`: title in the title slot, the project filter, window switch and refresh in the controls slot, the content in one `.system-container` block. Its own padding, `max-width: 1200px`, `margin` and the `.system-header` rules are gone. `SystemView.test.ts` and `pm-web-e2e/tests/system.spec.js` are not in the diff, so they pass unchanged by construction; the lane reports `system.spec.js` 8/8 at the head. The PLAN-37 hunks (aria-labels of the work figures, around :860-930) are untouched by this diff, so the `-X theirs` merge the lane describes did not cost them: the squash holds only the template move.
- **D3 guard.** `pageWidth.test.ts` lists `SystemView.svelte` with root `.system-container`, checks that each listed view renders inside a `*Page` template, that every view importing a template is listed, and that no `max-width` (also `max-inline-size`, inline style, `style:` directive, media-query cap) sits on the root, with eight control fixtures and a "no rule found" report so it cannot pass vacuously. `src/test/templateStyle.ts` (241 lines) is the scanner.
- **Browser evidence.** The artifacts exist: 23 files `43-system-{1440,390}-{before,after}-{de,en}-{light,dark}.yml`, `43-system-{1440,390}-{before,after}.png`, `43-system-d2-1440-after*` in `.plan/temp/playwright-mcp/` of the main checkout. I did not open them.
- **Surface delta (by hand):** declared 4 entries (the directory, `SystemView.svelte`, `pageWidth.test.ts`, `view-density-and-filtering.adoc`); realized 28 files. Declared and present: 11 (nine in `lib/templates/`, `SystemView.svelte`, the adoc). Beyond the declaration: 17 — `doc/design/web-app/README.adoc` (+18, the template widths), `src/test/templateStyle.ts`, 14 fixtures (six template fixtures, eight control views under `templateViews/`) and `pm-api/.../OnboardingRecordsTest.java`. `expansion_detected`; 16 of the 17 are in the area.
- **The pm-api file is outside the plan.** The first merge-group run failed on `OnboardingRecordsTest.shouldReportUnassigned`: `Generators.nonEmptyStrings()` can yield a blank string and `RoleAssignment.assigned()` treats blank as unassigned. The lane asked the operator twice ("investigate the flake first", then "fix the pm-api test in this PR") and fixed it with a `nonBlankString()` helper (a loop of up to 100 draws). The cause reads right: `assigned()` is `harnessId != null && !isBlank() && modelId != null && !isBlank()`. The fix covers that one test.

## Layer 3 — read, not run

Sound by reading:
- The four templates carry only token CSS (`var(--space-*)`, `--color-*`), no own typography beyond tokens, `min-inline-size: 0` on every flex and grid child, `overflow-wrap: anywhere` on the object title. The Triage collapse hides the pane that is not selected with `display: none`, so the hidden pane leaves the accessibility tree.
- `pageWidth.test.ts` goes beyond the spec in a way that holds: it finds the page root by class, drops comments, and fails on a selector that matches no rule.

Leads and gaps, none reproduced:
1. **The tests read stylesheet text.** Width, the 1099 px collapse, the inset and the "no second surface" check assert strings in the `<style>` block (the helper says jsdom applies no media query). Only the dashboard was drawn by a browser (the spec scopes the browser check to it); Triage, Object and Wizard have never been rendered at 1440, 1099/1100 or 390 px. The first view plan that moves onto one (PLAN-51, 53, 55, 58) is the first real check.
2. **The reference view uses two of four regions.** `SystemView` puts all its content in one block in the default slot, so `.dashboard-page__grid` (`repeat(auto-fit, minmax(min(320px,100%),1fr))`) holds exactly one child and the `updated` slot is never filled (the view's own "updated" line stays in the content). The card-grid rule and the updated line of the dashboard template are therefore proved by a unit test of the CSS text only. The spec said "without changing its content", so this is a consequence of the spec, not a defect; it means the grid is still unproven.
3. **An unfilled `id` slot draws an empty pill.** `ObjectPage` renders the `object-page__id` span unconditionally and styles it with padding, border and background; `kind`, `path` and `state` also render their wrappers unconditionally. `attention` and `tabs` are guarded by `$$slots`, and the tests cover a no-filters Triage and a steps-only Wizard, but not an `ObjectPage` without an id. A view without an id (the Project or completion-report object the proposal names) would show an empty bordered capsule.
4. The title heading is in the template for `ObjectPage` (`h1`) and in the view for `DashboardPage` (`SystemView` keeps its own `h1` in the title slot). The lane accepted it; a second view on `DashboardPage` can double it or omit it.
5. The templates use `$$slots` and `<slot>`, which Svelte 5 marks legacy; the app mixes both styles today (eight files use `<slot>`, six use `{@render}`). Not a defect.

D3's wording: the red run (`SystemView.svelte (.system-container): max-width: 1200px`, log `maven-2026-11-01-170321.log`) was taken at `88a35413`, after the move commit but before the width left the view. The spec says "before the move". The lane records it as a wording deviation; the detection proof at the head is the control fixtures.

## Evidence checked

- Sonar: the lane read `confirmed` 0 at `3049c450` (analysis revision equals the PR head, which is the head `gh` reports). My read after the merge is `undecidable` (`head_revision_unavailable`, analysis revision `3049c450…`): recorded as an unverified lead, not as 0. The unfiltered `search` for the PR lists six issues on `templateStyle.ts` (S3776, S6594 x2, S7780 x3), which the lane says it fixed one by one; the verb shows no status, so I cannot tell open from closed and do not count them.
- Reviewers: CodeRabbit (required, quota-limited) reviewed four heads, the last `82a8d1ce`, refused six triggers on quota, and its findings were fixed or answered; the heads after `82a8d1ce` (`3049c450` and the merge-commit refresh) are unreviewed, which the standing authorization covers when the re-trigger was refused (the lane records six refusals). Sourcery approved `b42a5621`, later dismissed as stale. The PR carries no labels. `gh` shows six inline review comments.
- The base moved three times (PLAN-26, 35, 37); two merge commits. The lane notes `-X theirs` on the `SystemView.svelte` conflict with the template move re-applied in `82a8d1c`; the squash diff above shows the final state is right.
- Build server: every routed job failed (`execution_failed`, empty log), so all builds ran `in_process` (the known bare-`mvn` cause).
- Lane rule breaks, reported by the lane: an early `git grep`, one `ls -la`, a `cp` from a temp copy, shell redirects to `.plan/temp/`, `pgrep`, one mistyped executor call. D1 was written before four of the declared skills were loaded (a recorded omission). A 900 s expiry left `pm-mock-runtime/target` inconsistent for the next run.
- Flaky specs seen once each: `designer.spec.js:157`, `reports.spec.js:34` and `:89`. The seven red e2e tests on `main` equal the base list; PLAN-90 (running) fixes them.

## Follow-ups

- PLAN-90 deliverable 3 touches `SystemView.svelte`: rebase over this move (the header block and the `.system-container` wrapper changed).
- PLAN-51, 53, 55, 58: each moves its view onto a template and adds it to `VIEWS_ON_TEMPLATES`; each is the first browser check of its template (lead 1) and must fill the `id` slot or guard it (lead 3). Re-read their specs for the `ObjectPage`/`TriagePage` slot names (`kind`, `path`, `title`, `id`, `state`, `primary`, `secondary`, `overflow`, `attention`, `tabs`; `filters`, `list`, `detail`, `selected`).
- A pm-api owner may sweep `Generators.nonEmptyStrings()` where a record treats blank as absent (the lane's residue); `OnboardingRecordsTest` lines 134, 167-168 and 517 and `SystemRecordsTest` 153-154 use the same generator, not read for blank sensitivity.
- The flaky `designer` and `reports` specs have no owner (PLAN-90 is the closest).

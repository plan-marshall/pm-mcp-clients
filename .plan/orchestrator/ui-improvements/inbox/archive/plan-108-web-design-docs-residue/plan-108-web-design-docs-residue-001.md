envelope_version=1
sender_type=plan
sender_id=plan-108-web-design-docs-residue
epic=ui-improvements
kind=landing
created=2026-11-06T22:37:31Z

# PLAN-108 web-design-docs-residue: PR 362 open, not merged

PR: https://github.com/plan-marshall/pm-mcp-clients/pull/362 (branch `fix/108-web-design-docs-residue`, head `93c67d2b`). Not merged: waiting for the operator's word.

## Deliverable 1 (design leftovers of the reports landing)
- (a) `components.adoc` RowMenu rationale carries no plan number.
- (b) Divergence NAMED, not removed: RowMenu arrow keys skip a disabled entry (no `disabledReason`, `title` is only a hover advisory); ActionBar text points to it. No product view passes a disabled entry; only the `/dev/components` fixture does.
- (c) `web-component-row-menu.svg` redrawn from the shipped menu ("Report actions" / "Mark read") plus the upward case above the bottom bar; rendered on light and dark, nothing clipped.
- (e) DECIDED (`components.adoc#row-menu-placement`): popup layer z-index 30 (above sticky chrome 20, below dialog 50); opens upward when it would pass the viewport bottom and there is more room above; re-placed on scroll/resize; capped and scrollable when taller than its side's room. May lie over the bar, never under it. Test-first: `pm-web-e2e/tests/row-menu.spec.js` at 390 px, 5/5 probe points covered by the bar before, 0 after.
- (d) `tokenize` (`utils/filterText.ts`) drops words with no letter or digit for every adopted view; ReportsView's local filter removed; EpicView's fold check now uses tokens. Rule stated once in `view-density-and-filtering.adoc#filter`.
- Extra fix: the RowMenu key list had no blank line and rendered as paragraph text; it is a list now.

## Deliverable 2 (age rule, decided here; PLAN-34 D4 reduced)
Choice: ADD ROWS (Open Defects entry for PLAN-18 defect 4). Age in a list: `just now` / `gerade eben` under a minute; `N min|h|d ago` / `vor N min|h|T` under 30 d; from 30 d `last month`, `N months ago`, `last year`, `N years ago` (German `letzten Monat`, `vor N Monaten`, `letztes Jahr`, `vor N Jahren`) from the language's relative-time data. No weeks, no bare `yesterday` as an age (yesterday stays a moment, `yesterday 18:40`). Web `format.ts` already renders exactly this, so it needs no change.
Shared wireframes rewritten: work-actions:332, paired-devices:24, project-explorer:26,31,71 (71 now points to the rule), plan-overview:141, needs-and-find:35 (`since yesterday 18:40`). Done-when grep names only forms the rule has.

## For PLAN-34 D4 (not done here)
- `ProjectsCommand.formatRelativeTime` prints `yesterday`, `N week(s) ago`, `N month(s) ago` and an English-only `just now`; `StatusCommand.formatRelativeTime` likewise prints an English-only `just now` in a German session (CodeRabbit on PR 362, declined here as PLAN-34's scope).
- The client designs `terminal-client/08, 15, 17, 18, 19` still carry week and yesterday ages.

## Evidence
- `./mvnw -pl pm-web-app verify` passes at head; vitest 107 files / 1346 tests.
- Playwright (ports 21080/21081): row-menu.spec.js passes. `reports.spec.js` "marks all shown reports read and shows empty state" FAILS AT BASE TOO (7 vs 6 unread): PLAN-105 landing defect 13, untouched.
- ref-asciidoc verify-links: 0 broken in 8 docs; asciidoctor renders all 8 with no warning. ref-documentation review: 0 issues.
- ref-asciidoc validate: no new finding. Pre-existing: generic header template (`:sectnums:` etc.), which 0 of 25 shared docs carry by convention, and one `<<…>>` xref in work-actions.adoc. Left as is; not "clean" in the validator's literal sense.
- Review: CodeRabbit 4 threads + 1 body nitpick, each answered in its thread; 3 fixed and the nitpick added (93c67d2b), 1 declined (StatusCommand, PLAN-34). CodeRabbit resolved the 3 fixed threads; the declined thread is still OPEN (my reply-and-resolve on it was blocked by the session's permission classifier). CodeRabbit asked there whether to open an issue: not answered on GitHub; the hand-off is this message. Sourcery skipped (budget). All 14 CI checks pass at 93c67d2b (6 skipped).
- Sonar at head 93c67d2b (SonarCloud check run completed 2026-11-06T22:36:21Z): open issues 0, rules left open: none, quality gate OK. A confirmed 0, not a failed read.

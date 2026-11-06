# Landing analysis: PLAN-18 verify-shared-foundation

PR #349 `02efb96b` (squash-merged 2026-11-06T19:42Z; head `ac967d7a`; 6 commits, ONE file: `doc/claude_review/verification-results/verify-shared-foundation.adoc`, new). Plan lane `plan-marshall` (plan id `plan-18-verify-shared-foundation`); landing message 19:52Z. A VERIFICATION plan: five checks run, verdicts recorded, nothing fixed. Analysis by a read-only sub-agent (docs and code read at the merge commit, the sibling `plan-marshall-documentation` at `be5750dd`/`376492c9`, nothing built or run); the counts and the central findings (V*) were re-read by the orchestrator.

## Merge rule

PARTLY SATISFIED. Threads: 0 of 2 unresolved, both outdated (V*: GraphQL). Sonar: NO analysis exists (docs-only PR, CI skips `sonar-build`): the epic's decision of 2026-10-28 treats `undecidable (docs-only skip)` as not blocking, but the PLAN-18 spec and the 13 verify specs lack the clause, so the lane escalated to the operator twice (candidate lesson 005). CodeRabbit: review `5801026676` (3 body items, all FIXED in the file and answered truthfully in issue comment `6687434568`); inline `4493144500` fixed (`:docs:` pinned to `be5750dd`, real); review `5803697800` has no body items, its inline `4495103099` ("mark the full client checks `not verified`", and the `plan nope` behaviour) is only HALF handled: the thread was resolved with CodeRabbit's request open and the record never mentions `plan nope`. Heads `55bac8cb` and `ac967d7a` unreviewed (quota), allowed by the rule. `@coderabbitai review` posted three times (rule: once). The landing carries no `unresolved_review_threads` line (a `plan-marshall`-lane message, not a runbook lane).

## Three layers

| Check | Recorded | Correct at `02efb96b` |
|---|---|---|
| UC-V-01 | pass | HOLDS (6 tests of the contract test, 29 states "equal by declaration", prefix mapping is weak: defect 8) |
| CLI-UC-V-09 mock part | pass | HOLDS (handler `MockRuntimeEngine:9390-9400`, tests exist) |
| CLI-UC-V-09 first clause (`plan nope` shows "No plan nope", exit 1) | unrecorded | MISSED FAIL (defect 5) |
| CLI-CC-V-03 rule part | fail | RIGHT, but undercounts (defect 4); the 13 weeks/days occurrences listed are complete (V) |
| CLI-CC-V-03 table-test part | not verified ("no test was run") | misdescribed: `Display` does not exist until PLAN-34 (defect 6) |
| UC-V-09, CLI-UC-V-10 | not verified | substance holds (all five changes `AGREED` at `be5750dd`); the stated reason is not the real one (defect 1) |
| Verify-first | pass | HOLDS (7 dependency hashes are ancestors) |

## Defects (at `02efb96b`)

1. HIGH V The spec criterion "the result file passes `doc-review`" names a skill that cannot do it: `doc-review` (sibling repo) reviews requirements, specifications, roadmap and implementation watch, never `doc/claude_review/` results or use cases, and its fix pass edits documents, which the plan forbids. It cost three operator rounds, one per head. V*: the clause is in the staged specs 29, 33, 41, 42, 48, 63, 65, 70, 71, 75.
2. MED V UC-V-09 and CLI-UC-V-10 are `not verified` and owned by no plan (`grep` finds them only in PLAN-18). The substance would pass today.
3. MED V The PR body and the squash message say "One check fails; the other four pass" (UC-V-09 and CLI-UC-V-10 as `pass`); the record has 2 pass, 1 fail, 5 not verified.
4. MED V* The CLI-CC-V-03 fail undercounts: `2 months ago` (`17-projects.adoc:54, 367`), bare `yesterday` as an age (`project-explorer.adoc:26, 71`, `plan-overview.adoc:141`, `17-projects.adoc:50, 364`; `project-explorer.adoc:71` states a second rule), `since yesterday`, `just now`. The rule is silent beyond 30 days; the web `format.ts` renders months through `Intl.RelativeTimeFormat` and `ProjectsCommand.formatRelativeTime` prints `yesterday`, `N weeks ago`, `N months ago`, `just now`. The done-when `git grep (weeks?|days?) ago` of PLAN-34 D4 and PLAN-74 D4 would pass with months and `yesterday` left; rewriting to `60 d ago` puts the terminal design against the web.
5. MED V The first clause of CLI-UC-V-09 is in no row (advisory 1, unfixed). Reading shows it would FAIL: `PlanCommand.java:187, 884, 1293` print `Plan %s not found.` (hard-coded English, exit 1; V*), the catalogue key `plan.view.not_found` is unused, the check expects "No plan nope". Owned by neither PLAN-33 nor PLAN-30.
6. LOW V "no test was run" for the `Display` table tests: `Display` does not exist until PLAN-34; PLAN-40 and PLAN-63 need PLAN-34 first.
7. LOW V Advisory 2: "the time rule is specified once" is not a clause of the check text (it comes from step CLI-CC-S-04 and the spec); the record does not say so. The `fail` stands.
8. LOW L UC-V-01 strength: `concernedResource` maps by prefix and 29 states are only "equal by declaration".
9. LOW V The result file is orphaned (no README or link; 12 more result files follow).
10. LOW V The spec says PLAN-82 and PLAN-86 are "checked here"; none of the five checks covers them.

## Candidate lessons (inbox 002-006)

002 head-bound operator acceptance re-blocks per head: spec rule for this orchestrator (name only criteria the executing repo can run: defect 1) plus a plan-marshall change (carry an acceptance forward). 003 countable claims in verification records: a rule for the verify spec template. 004 CodeRabbit quota and a barrier that refused an earlier answer: plan-marshall change; the policy exists here only as a user memory. 005 Sonar on docs-only PRs: our rule `zero-sonar-findings-at-landing` and `epic.md` ("an undecidable Sonar read is a stop") lack the 2026-10-28 exception; fix them and the verify spec template. 006 pre-push "no build owed" shortcut: plan-marshall change.

## Lane deviations

Operator rounds for an inapplicable criterion; PR body misstates two verdicts; advisory 1 left open and a CodeRabbit request open when the thread was resolved; three review triggers. Positive: all 13 age occurrences correct, UC-V-09 corrected before writing, `:docs:` pinned with real commits, no overclaim about the mock. Eighth consecutive lane with a real defect behind "all handled".

## Routing

PLAN-34 D4 and PLAN-74 D4 widened (defect 4: PLAN-34 decides the rule in `texts-and-languages.adoc#time` first because PLAN-74 waits on the 70-72-73 chain; widened regex). For the drain after restart: the `plan nope` text and clause (5: PLAN-33/PLAN-30), PLAN-40/63 wait for PLAN-34 (6). Open for the drain after restart: the `doc-review` clause in ten staged specs (1), a home for UC-V-09 and CLI-UC-V-10 (2), the docs-only Sonar clause in the verify template and the zero-Sonar rule (005), a link for `verification-results/` (9). Result file kept as is.

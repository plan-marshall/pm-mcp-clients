envelope_version=1
sender_type=plan
sender_id=rb-70-web-light-theme-pass
epic=ui-improvements
kind=landing
created=2026-11-27T22:29:54Z

```landing-facts
schema=landing-facts/1
plan_id=rb-70-web-light-theme-pass
epic=ui-improvements
pr=#601
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=4
review_body_findings_handled=4
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

Merged PR #601 into `main` at `693d0597e202f4405b218f23a5415c7a6b0de9df` (merge group run `46075233726`).

## 1. Deliverables

1. **Deliverable 1 — Light pass: met** (commits `4211f901`, `f327b800`, `ea8b30c1`, `e42c360d`, `69a25ccf`, `74073a86`, merged onto `main` at `693d0597e202f4405b218f23a5415c7a6b0de9df`).
   - Done-when (control border non-text contrast): **met** — `tokens.test.ts` `--control-border` vs `--control-bg` / control surfaces; `order: test-first`; failing run at `da3cafdf` in `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-27-114850.log` (job `6a95aac8a29141ccb50d97edbf2849b0`), passing run at `4211f901` in `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-27-115357.log` (job `d020911254c64a5da15392adbe033207`). Source-derived Svelte scanner + `FilterInput.test.ts` contract added in `4211f901`/`f327b800`/`ea8b30c1`/`e42c360d`/`69a25ccf`/`74073a86` (passing at `74073a86` in `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-27-213014.log`, job `99536519e8a74f90afd9f8da699bc259`).
   - Done-when (component theme token read): **met** — `theme.test.ts` (`--color-primary` -> `--color-brand` on `--color-bg-surface` read from `tokens.css`); `order: mutation-after`; failing mutation run at `4211f901` (`--color-brand` mutated to `#cccccc`) in `.plan/local/plans/NO_PLAN/build-results/pm-web-app/maven-2026-11-27-115918.log` (job `89226944be04e3da5328e32b4419b1b`), restored at `4211f901`.
   - Done-when (browser light pass & E2E audit): **met** — `order: none`; Playwright MCP `fully-onboarded` before/after visual & accessibility captures (18 views across EN/DE desktop `1440x900`, `390x844` phone, and dark spot-check under `.plan/temp/playwright-mcp/70-web-light-theme-pass-*-{before,after}.{png,yml}`); `pm-web-e2e -Pe2e` `tests/theme-readability.spec.js` passed all 192 tests (`#344-#535`, including light matrix across all 23 `DECLARED_VIEWS` and axe-core audits) at `ea8b30c1` (`.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-27-145216.log`), `69a25ccf` (`.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-27-202853.log`), and `e21bd24c` (`.plan/local/plans/NO_PLAN/build-results/pm-web-e2e/maven-2026-11-27-211905.log`; 1 unrelated pre-existing failure in `tests/designer.spec.js:264:3` from `e22b302d`).
   - Full-repository `verify` (`quality-gate`) passed at every pushed head: `7cb37306` (`maven-2026-11-27-130321.log`), `f327b800` (`maven-2026-11-27-134853.log`), `ea8b30c1` (`maven-2026-11-27-144140.log`), `e42c360d` (`maven-2026-11-27-195748.log`), `69a25ccf` (`maven-2026-11-27-201539.log`), `e21bd24c` (`maven-2026-11-27-210538.log`), and final head `74073a86` (`maven-2026-11-27-213209.log`).
2. **Deliverable 2 — Design record: met** (commits `7cb37306`, `f327b800`, `ea8b30c1`, `e42c360d`).
   - Done-when (`doc/design/web-app/theme-readability.adoc` updated and checked): **met** — `order: none`; `asciidoc validate` (0 errors, 0 warnings), `docs review` (0 tone/completeness issues), `asciidoc verify-links` (16 valid; 2 cross-repo relative links on line 11 only flagged due to worktree nesting depth, `.plan/temp/rb-70-web-light-theme-pass/links.md`).

## 2. Personas and skills

- Per step: `implement` / `build-gate` -> Deliverable 1 `plan-marshall:persona-integration-tester`, Deliverable 2 `plan-marshall:persona-documenter`; `self-review` -> `plan-marshall:persona-code-reviewer`; `pr` / `reviews` / `merge` -> runbook lane rules.
- Per deliverable:
  - Deliverable 1 declared skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:javascript-security` (`skills added: none`).
  - Deliverable 2 declared skills: `pm-documents:ref-asciidoc`, `pm-documents:ref-documentation` (`skills added: none`).

## 3. Hypotheses

- **Confirmed:** `--color-border-strong` (`#CFCFC8` light / `#3A4356` dark) fell below the WCAG 1.4.11 3:1 non-text floor (`1.57:1` light / `1.74:1` dark against `--control-bg`, verified by `pm-web-app/src/styles/tokens.test.ts:618`), and active bordered form controls in `FilterInput.svelte:135`, `BlockPalette.svelte:199`, `CredentialEntry.svelte:149`, `WorkControlPanel.svelte:1272`, `ProcessDesigner.svelte:2007`, `ProjectExplorerView.svelte:1819,1992`, `ProjectOnboardingView.svelte:1227`, and `SettingsView.svelte:1843,2008` used subtle surface divider `var(--color-border)`. Tuning `--color-border-strong` to `#888880` (light) / `#65718B` (dark) yields `3.57:1` against `--control-bg`.
- **Refuted:** No light-mode surface tokens (`--color-bg-app`, `--color-bg-surface`, `--color-bg-subtle`) or shadow tokens required modification; card/surface separation held across all 18 visually inspected views and all 23 declared views in `theme-readability.spec.js`.
- **Confirmed:** `pm-web-app/src/lib/views/theme.test.ts` asserted invented RGB literals instead of tokens parsed from `pm-web-app/src/styles/tokens.css`.

## 4. Findings

- **Self-review Round 1 (`FINDINGS_OPEN`):** 3 findings (unmigrated controls across 5 Svelte files; missing `.yml` snapshots / un-opened `plan-chat` screenshot / bare log IDs; stale prose & shadow token names in `theme-readability.adoc`) -> fixed in `f327b800`.
- **Self-review Round 2 (`FINDINGS_OPEN`):** 2 findings (`ProjectOnboardingView.svelte:1227` `.field-input` on `--color-border`; 6 missing `-after.yml` snapshots & unrecorded post-edit doc-checks) -> fixed in `ea8b30c1`.
- **Self-review Round 3 (`CLEAR`):** 0 open defects.
- **CI:** Runs at `e42c360d` and `69a25ccf` failed in `pm-api` (`OverviewCatalogueTest.shouldContainAllSpecificationKeys` + 6 `PlanViewCatalogueTest.designKeyIsInBothPacks` assertions) due to upstream `origin/main` commit `7e5382c8` (#600); resolved once `origin/main` advanced to `3f0af941` (#599) and was merged at `e21bd24c`. Final CI on `74073a86` (and merge group run `46075233726`) passed all checks; local `pm-web-e2e` had 1 unrelated pre-existing failure in `tests/designer.spec.js:264:3` (`e22b302d` on `main`).
- **PR comments (all 4 CodeRabbit threads resolved):**
  - `PRRC_kwDOVBOtss8AAAABGYXJBA` (`PRRT_kwDOVBOtss686tny`): hardcoded control list in `tokens.test.ts` -> replaced with Svelte source scanner + fixed `WorkControlPanel.svelte:1272` in `e42c360d`.
  - `PRRC_kwDOVBOtss8AAAABGYbHNg` (`PRRT_kwDOVBOtss686tny`): zero-width regex matching `0.0625rem` -> extracted `isBorderlessDeclaration` + unit test in `69a25ccf`.
  - `PRRC_kwDOVBOtss8AAAABGY5kKA` (`PRRT_kwDOVBOtss688BgS`): `:not(:disabled)` skipped -> stripped `:not(...)` clauses in `selectorTargetsControl` + unit test in `74073a86`.
  - `PRRC_kwDOVBOtss8AAAABGY5kNA` (`PRRT_kwDOVBOtss688Bga`): partial transparent `border-color` skipped -> property-aware `isBorderlessDeclaration` + unit test in `74073a86`.
  - `PRRC_kwDOVBOtss8AAAABGY5kNw` (`PRRT_kwDOVBOtss688Bgd`): side-specific border declarations unchecked -> extracted `collectBorderDeclarations` + unit test in `74073a86`.

## 5. Reviewers

- **`coderabbit` (required, `quota_limited: true`, `review_complete_after_handling: true`):** Rate-limited on `15:10:14Z`, retry 1/10 (`16:42:18Z`), retry 2/10 (`18:13:49Z`); retry 3/10 (`19:44:58Z`) succeeded and posted qualifying review `PRR_kwDOVBOtss8AAAABayh1gA` on `ea8b30c1` (Merge Risk Low, 1 inline thread) and follow-up review `PRR_kwDOVBOtss8AAAABazKgiw` on `e21bd24c` (Merge Risk Low, 3 inline threads); `CodeRabbit` check was `NEUTRAL` (skipping) on `74073a86`. All 4 findings were fixed (`e42c360d`, `69a25ccf`, `74073a86`), replied to, and resolved (`review-gate` on `74073a86`: `merge_allowed=true`, `4/4` handled).
- **`sourcery` (optional, `quota_limited: true`):** Rate-limited at `15:10:12Z` (`IC_kwDOVBOtss8AAAABrnKflQ`); subsequently posted **Approved** (`PRR_kwDOVBOtss8AAAABayl3OA` at `19:58:14Z` on `e42c360d`), `review_complete_after_handling: false`.

## 6. Operator questions

- Question tool call 1 (`ask_question`): *"PR #601 passes local full-repo verify at 69a25ccf, has 0 unresolved review threads, and all 192 theme-readability E2E tests pass, but GitHub CI (build (27) and sonar-build) fails in pm-api because origin/main commit 7e5382c8 (#600) added design keys not yet in the pm-api message packs (leaving Sonar stale at ea8b30c1). How would you like to proceed?"* — Options: *(Recommended) Pause here with PR #601 ready at 69a25ccf...*, *Wait for origin/main to fix pm-api...*, *Stop the runbook lane now...*. Free-text answer: `main is fixed` (resolved by merging `origin/main` `3f0af941` `#599`).

## 7. Rule check

- `guard: none on this harness (unenforced)`.
- `spec_source`: `ledger_worktree` (`/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-70-web-light-theme-pass.md`), `executor-skew`: `not_applicable` / `executor_in_worktree: absent`.
- No subagents were killed (`Code Reviewer Round 1`, `Round 2`, `Round 3` ran cleanly via `invoke_subagent`). All 3 required mailbox reads returned empty (`inbox read --plan-id 70-web-light-theme-pass` errored `invalid_target_plan` due to leading digit `7`; `rb-70-web-light-theme-pass` returned `mailbox_state=no_mailbox`, `count=0`). `progress.md` and `landing` artifacts stayed under `.plan/temp/`, main checkout updated only via `git-workflow switch-and-pull`.

## 8. Build server

- Setup preflight: `ready`.
- Builds executed via `build_server`: `daemon_longpoll: 21`, `in_process_fallback: 0` (covering `da3cafdf`, `4211f901`, `7cb37306`, `f327b800`, `ea8b30c1`, `e42c360d`, `69a25ccf`, `e21bd24c`, `74073a86`, including `test-first`, `mutation-after`, `pm-web-app`, `pm-web-e2e`, and 7 full-repo `quality-gate` runs).
- Runtime stamp note: landing-time `runbook_lane resolve` reported `model: unknown` and `effort: unknown` on `harness: antigravity`.

## 9. Residue

- None in PLAN-70 scope (`cleanup_owed=false`; feature worktree and local/remote branches deleted cleanly). Pre-existing unrelated `pm-web-e2e/tests/designer.spec.js:264:3` (`UC-PLN-3` confirm-remove copy mismatch from `e22b302d` on `main`) remains unchanged.

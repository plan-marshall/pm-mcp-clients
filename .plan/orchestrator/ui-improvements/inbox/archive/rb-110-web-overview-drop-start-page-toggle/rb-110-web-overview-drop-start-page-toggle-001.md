envelope_version=1
sender_type=plan
sender_id=rb-110-web-overview-drop-start-page-toggle
epic=ui-improvements
kind=landing
created=2026-11-07T14:07:19Z

```landing-facts
schema=landing-facts/1
plan_id=rb-110-web-overview-drop-start-page-toggle
epic=ui-improvements
pr=#364
merge_state=merged
cleanup_owed=false
deliverables_total=1
deliverables_done=1
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-110 (Web: Remove "Set as Browser Start Page" and Every Part of It)

## 1. Deliverables

### Deliverable 1: Remove the toggle, its store and its texts
- **DW1 (order: `none`):** `git grep` for `pm_start_route`, `rememberedStartRoute`, `setRememberedStartRoute`, `start_page.(set|is_default)`, `start-page-toggle`, and `startPageIsOverview` across `pm-web-app`, `pm-web-e2e`, and `pm-api` finds 0 matches. Verdict: met.
- **DW2 (order: `test-first`):** Unit test `lands by the rule alone when localStorage holds a stale start route` in `pm-web-app/src/lib/stores/runtime.test.ts`. Failing run at commit `3bdd02a2` with output `AssertionError: expected 'projects' to be 'overview' at runtime.test.ts:368:44`. Passed at `795802a1`, preserved at `a24ecde7`. Verdict: met.
- **DW3 (order: `none`):** `npm run quality` in `pm-web-app` passed (exit code 0); `pm-web-e2e/tests/overview.spec.js` passed (5 tests) on assigned ports 21090/21091. Full Maven quality gate passed cleanly. Verdict: met.
- **DW4 (order: `none`):** Browser check conducted across `de` and `en`, light and dark themes at 1440×900 and 390×844 viewports. Heading rendered on natural lines without wrapping. 16 baseline and after screenshots recorded under `.plan/temp/playwright-mcp/`. Verdict: met.
- **DW5 (order: `none`):** Open Sonar issue read at PR head: confirmed `0` open issues on heads `a24ecde7` and `f14aa8d1`. Verdict: met.

Tested heads:
- `a24ecde7`: Quality gate `verify -Ppre-commit` passed (574s, job `8cef929abc444bd396207667a2918371`).
- `f14aa8d1`: Quality gate `verify -Ppre-commit` passed (645s, job `cac284a7703540f3a76c3886f6716bfc`).

## 2. Personas and Skills

- **Personas applied:**
  - Setup & Implementation: `plan-marshall:persona-implementer`
  - Unit & E2E Testing: `plan-marshall:persona-module-tester`, `plan-marshall:persona-integration-tester`
  - Self-Review & PR Triage: `plan-marshall:persona-code-reviewer`
- **Deliverable 1 skills:**
  - Declared: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-dev-frontend:javascript-security`
  - Added for surface drift: none

## 3. Hypotheses

- **Hypothesis:** "the heading block `.sidebar-header` (an `h2.sidebar-title` plus the Spinner) renders the title on one line once the button is gone" — confirmed, `pm-web-app/src/lib/views/OverviewView.svelte:570-577`, `1214-1220`.

## 4. Findings

- **Self-Review (Round 1):** Addressed recording of browser check screenshots and overview e2e test execution in `progress.md`. Open: none.
- **CI Checks:** 20/20 checks passed on head `a24ecde7` and on merged head `f14aa8d1`.
- **PR Review Comments:** 0 review threads, 0 findings.

## 5. Reviewers

- **`coderabbit` (required, quota-limited):**
  - Triggered at 2026-11-07T11:20:08+01:00 on `a24ecde7` — refused (hourly rate limit reached).
  - Re-triggered at 2026-11-07T12:50:22+01:00 on `a24ecde7` — reviewed (`final_review_risk_coverage` marker, `kind=reviewed`, 0 actionable comments).
  - Re-triggered at 2026-11-07T13:08:56+01:00 on `f14aa8d1` — refused (hourly rate limit reached).
  - Standing Review Authorization: qualifying clean review on `a24ecde7`, re-triggered once on `f14aa8d1` and refusal recorded; build gate, CI, and Sonar confirmed green on `f14aa8d1`.
- **`sourcery` (optional, quota-limited):**
  - Triggered at 2026-11-07T11:20:20+01:00 on `a24ecde7` — refused (7-day budget exceeded). Non-gating.

## 6. Operator Questions

1. Text: "All checks are green on PR #364 (head f14aa8d1). CodeRabbit holds a qualifying clean review on prior head a24ecde7 and was re-triggered on f14aa8d1 where it refused on quota. How would you like to proceed?"
   - Options:
     1. "Merge PR #364 under standing review authorization (CodeRabbit reviewed qualifying head a24ecde7 with 0 findings, re-triggered on f14aa8d1 and refused on hourly quota; build gate, CI, and Sonar are green on f14aa8d1)"
     2. "Wait until 14:38:56 for CodeRabbit hourly rate-limit cooldown to elapse and re-trigger CodeRabbit on f14aa8d1"
   - Selected: Option 1 ("Merge PR #364 under standing review authorization...")
- Free text: none

## 7. Rule Check

- Scope, Evidence, Executor, Shell, and Git rules strictly maintained.
- Guard: `guard: none on this harness (unenforced)`.

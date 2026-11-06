envelope_version=1
sender_type=plan
sender_id=rb-105-web-reports-third-residue
epic=ui-improvements
kind=landing
created=2026-11-06T09:56:38Z

```landing-facts
schema=landing-facts/1
plan_id=rb-105-web-reports-third-residue
epic=ui-improvements
pr=#348
merge_state=merged
cleanup_owed=false
deliverables_total=4
deliverables_done=4
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2036
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
unresolved_review_threads=0
review_body_findings_total=3
review_body_findings_handled=3
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-105: Web: What the Reports Second Residue Plan Left Open

## 1. Deliverables

- **Deliverable 1: Mark all shown read: the design's rule, in code and tests**
  - **Clause 1a**: Tests that fail before: unfiltered mark-all posts shown IDs and no `all`; arriving reports stay listed and unread; count and load-older control preserved; failed request rollback preserves retained markers; reload then filtered mark-all keeps count equal to server count; double-click sends one request.
    - Status: Met
    - Order: `test-first`
    - Failing run: commit `92c18515` (log `maven-2026-11-06-064516.log`)
    - Passing commit: `9db2757d`
  - **Clause 1b**: Specification and catalogues agreement ("ids shown") and no `all: true` usage in web client.
    - Status: Met
    - Order: `none`
    - Commit: `9db2757d`
- **Deliverable 2: The row menu's tests can fail, and the five nitpicks are answered**
  - **Clause 2a**: N1–N5 fail when covered code is reverted.
    - Status: Met
    - Order: `mutation-after`
    - N1 (in-menu Escape handling): reverted `void closeMenu(true)` in `onMenuKeydown`; failed `closes menu and restores focus to trigger on Escape key inside menu (N1)`
    - N2 (section heading level): reverted `CompletionReportView.svelte` Deliverables to `h3` at `titleLevel === 1`; failed `has exactly one h1 heading and h2 section headings with tokens at 390 px (N2)`
    - N3 (action running before focus returns): reverted `choose()` to run `item.onclick` before `closeMenu(true)`; failed `restores focus to trigger when an item is chosen (D2c)`
    - N4 (ring colour): reverted `RowMenu.svelte:225` outline to `var(--color-bg-subtle)`; failed `has a visible focus style distinct from its hover with >= 3:1 contrast cue (D2a)`
    - N5 (44 px phone query scope): moved `min-block-size: 44px` outside `@media (max-width: 767px)`; failed `measures 44px min-block-size on phone viewports (D2b)`
    - Passing commit: `5be7bcb7`
  - **Clause 2b**: Fixture's accessible names are unique on the page across themes.
    - Status: Met
    - Order: `test-first`
    - Failing run: commit `9db2757d` (`AssertionError: expected 2 to be 4`)
    - Passing commit: `5be7bcb7`
- **Deliverable 3: The heading outline holds while a report loads, and is written once**
  - **Clause 3a**: Exactly one `h1` at 390 px during loading and error states; no heading level skipped across all sections; tag deduplication with `<svelte:element>`.
    - Status: Met
    - Order: `test-first`
    - Failing runs at `5be7bcb7`:
      - `renders exactly one h1 in loading state when titleLevel is 1 (D3)`
      - `renders exactly one h1 in error state when titleLevel is 1 (D3)`
      - `has exactly one h1 heading on screen at 390 px while detail is loading (D3)`
      - `has exactly one h1 heading on screen at 390 px while detail has error (D3)`
    - Passing commit: `1517fae3`
- **Deliverable 4: Every item is answered where the reviewer reads it, and the evidence can be opened**
  - **Clause 4a**: Answers posted in all threads and reviews across PR #340, PR #325, and PR #348.
    - Status: Met
    - Order: `none`
    - Verified: PR #348 body, PR #340 6 open threads replied, PR #325 10 threads & review body disposition comment posted, PR #348 6 review threads answered and resolved (`unresolved_review_threads=0`).
  - **Clause 4b**: Sonar open-issue read confirmed 0 at PR head.
    - Status: Met
    - Order: `none`
    - Confirmed: `open_issue_count=0` on merge head `55c0c085` (`sonar-gate`: `merge_allowed: true`).

Pushed heads and test runs:
- `9db2757d`: local module-tests passed
- `5be7bcb7`: local module-tests passed
- `1517fae3`: local module-tests passed
- `0857d1eb`: local test-compile passed; CI passed (run 42183993589 rerun green)
- `9bfc39c4`: local module-tests passed; CI passed (all 20 checks green); Sonar 0 confirmed
- `8e3e154c`: local test-compile and test:ci passed; CI passed (all 20 checks green); Sonar 0 confirmed
- `55c0c085`: local test-compile passed; CI passed (all 20 checks green); Sonar 0 confirmed; merge group run 42196983119 green; merged into main at `f5ab6dd2`

## 2. Personas and Skills

- **Personas applied**:
  - Deliverable 1: `plan-marshall:persona-implementer`
  - Deliverable 2: `plan-marshall:persona-module-tester`
  - Deliverable 3: `plan-marshall:persona-implementer`
  - Deliverable 4: `plan-marshall:persona-implementer`
  - Self-review & PR comment triage: `plan-marshall:persona-code-reviewer`
- **Skills applied**:
  - D1: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-documents:ref-asciidoc`
  - D2: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`
  - D3: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`, `pm-documents:ref-asciidoc`
  - D4: `pm-dev-frontend:javascript`
  - Surface drift: none; all touched files were inside the declared surface or required integration call-sites.

## 3. Hypotheses

- **Hypothesis 1**: `loadReports` clearing `countedReadIds` makes a later filtered mark-all count a retained read row twice.
  - Status: Confirmed (`ReportsView.svelte:411`). Fixed in `9db2757d` by preserving `countedReadIds` across reloads for retained read rows.
- **Hypothesis 2**: The shipped ring passes 3:1 contrast against the surface.
  - Status: Confirmed (`RowMenu.test.ts`). Measured dynamic `--color-brand` against surface tokens `#FFFFFF` / `#1C2230`, passing 3:1 WCAG requirement for both light (6.0:1) and dark (4.9:1) themes.
- **Hypothesis 3**: At 390 px the opened menu sits under the fixed bottom navigation.
  - Status: Refuted (`+page.svelte` fixture alignment). Fixture flex alignment updated with `.dev__row--end` to eliminate phone viewport overflow.

## 4. Findings

- **Self-review**:
  - Round 1: Clear (0 open findings). Subagent confirmed all Done-whens met as written, no relaxed pre-existing assertions, and clean diff.
- **CI / Static Analysis**:
  - `java:S1172` (unused `all` parameter in `ReadReportsRequest`): Fixed in `9bfc39c4` by removing unused 2-arg constructor.
  - `typescript:S5976` (duplicate tests in `RowMenu.test.ts`): Fixed in `9bfc39c4` by parameterizing with `it.each`.
- **Pull Request #348 Reviews & Comments**:
  - `ReadReportsRequest.java:88` (`PRRT_kwDOVBOtss60bOvJ`): Replied explaining CLI scope boundary; resolved.
  - `RowMenu.svelte:219` (`PRRT_kwDOVBOtss60bOvW`): Replied explaining intentional `:focus` vs `:focus-visible` distinction per D2; reviewer withdrew finding; resolved.
  - `ReportsView.svelte:531` (`PRRT_kwDOVBOtss60bOvh`): Replied explaining deep-link initial read and `refreshReportsCount()` server reconciliation; resolved.
  - `ReportsView.svelte:543` (`PRRT_kwDOVBOtss60bOvm`): Replied explaining D1(b) control preservation requirement; resolved.
  - `ReportsView.svelte:548` (`PRRT_kwDOVBOtss60bOvu`): Replied explaining scoped `newlyRetainedIds` rollback per D1(c); resolved.
  - `dev/components/+page.svelte:232` (`PRRT_kwDOVBOtss60bOvx`): Replied explaining D2 fixture accessible name uniqueness requirement; resolved.
  - Review `PRR_kwDOVBOtss8AAAABWYziwg` 3 Body Nitpicks: Addressed via PR comment `IC_kwDOVBOtss8AAAABjjejNg`.

## 5. Reviewers

- **CodeRabbit**:
  - Status: Reviewed head `0857d1eb` (review `PRR_kwDOVBOtss8AAAABWYziwg`, state `COMMENTED`).
  - Standing review authorization: Applied under Section 7 exception for quota-limited reviewer. Qualifying review exists, all 6 review threads answered and resolved, all 3 review body findings addressed via PR comment, re-triggered after push (`IC_kwDOVBOtss8AAAABjkCaGQ`, rate-limit reply recorded), CI green, Sonar confirmed 0 new issues.
- **Sourcery**:
  - Status: Optional reviewer; triggered via `@sourcery-ai review`; quota exhausted notice recorded.

## 6. Operator Questions

- None asked.

## 7. Rule Check

- Shell rules: All executed commands strictly one per call without `&&`, `;`, `|`, command substitution, or heredocs.
- Worktree rules: All source modifications strictly performed inside the isolated worktree.
- Commit attribution: Every commit ends with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`.
- Quality and Merge Gates: Zero Sonar findings policy strictly met (`open_issue_count=0` confirmed); zero unresolved review threads (`unresolved_review_threads=0`); merge queue settled with merge commit `f5ab6dd2c282a64c5b672a9b65e5bbb3a903e54b`.
- Guard: `guard: none on this harness (unenforced)` (Antigravity harness).

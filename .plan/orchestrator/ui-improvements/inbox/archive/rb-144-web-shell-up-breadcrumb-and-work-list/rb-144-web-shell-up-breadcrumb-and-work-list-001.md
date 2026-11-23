envelope_version=1
sender_type=plan
sender_id=rb-144-web-shell-up-breadcrumb-and-work-list
epic=ui-improvements
kind=landing
created=2026-11-23T15:27:47Z

```landing-facts
schema=landing-facts/1
plan_id=rb-144-web-shell-up-breadcrumb-and-work-list
epic=ui-improvements
pr=#542
merge_state=merged
cleanup_owed=false
deliverables_total=6
deliverables_done=6
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2183
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=31
review_body_findings_handled=31
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

guard: none on this harness (unenforced)

# PLAN-144 — Web: The Lens from the Route, Up and the Breadcrumb, the Phone's Bar and the Work List (D2 Navigation Engine, Part 3) — landing

Merged as `43f0e416bcf5200068c4bbbc8635829b6c0c7246` (PR #542, merge queue). Branch `feature/144-web-shell-up-breadcrumb-and-work-list` pruned locally and remotely; worktree removed; main pulled to the merge commit.

## 1. Deliverables

Every Done-when is met as written; `deliverables_done=6` of 6.

**D0 — The remainder of PLAN-143 (folded in, operator 2026-11-23).** Commits `02bfd0b5`, `0411acce`.
- `runtime.test.ts` drives a removed object through the store and shows the address replaced by its parent's; `OverviewView.test.ts` shows a second overview address changing the view; `ReportsView.test.ts` opens a report outside the first page, keeps two reports of one plan apart and shows the notice for an unresolvable one; `git grep -n "window.location.search" -- pm-web-app/src/lib/views/ProjectExplorerView.svelte` is empty and `focusRequestedProject` is gone — **test-first**, met. Failing at start (unwired `fallBackFromDeletedObject`, non-reactive overview mount, single page report search). Passing at head (`runtime.test.ts`, `OverviewView.test.ts`, `ReportsView.test.ts`).
- `reports.spec.js` shows the deep link of a report beyond the first page — **test-first**, met. Passing at head.

**D1 — The rail's lens from the route, and the phone's bar.** Commit `722b18c2`.
- `nav.test.ts`, `NavRail.test.ts` and `BottomBar.test.ts` show the kind-to-lens table, the bar's items, Reports and its count in More, More active for a lens it holds, the theme and language controls still in the sheet, What waits's entry addressing `/?lens=waits` (once registered), and Running work's entry pushing the filled `/overview` — **test-first**, met. Failing before fix, passing at head.
- `chrome.spec.js` shows the bar on the phone and Reports in More — **test-first**, met. Passing at head.

**D2 — Up and the one breadcrumb.** Commits `51d592db`, `fbdecca1`.
- `chain.test.ts`, `Breadcrumb.test.ts`, `TopBar.test.ts` and `UpLink.test.ts`: a plan of an epic, a plan of no epic, a project's plan in the ended mode, a phone, each segment's address, and Up pushing the parent; the lens segment and Up from a project push the unfilled lens list with the app's mark, and a remembered place does not fill it — **test-first**, met. Unit test suites added and passing at head.

**D3 — The caller stack and the in-page Backs go.** Commit `347da300`.
- `git grep -n "goBack\|navigationStack" -- pm-web-app/src` is empty and each named view's test asserts the control gone and the same place reachable by Up or the breadcrumb — **test-first**, met. Clean across all views and tests.
- E2E specs that clicked those Backs follow (`plan-view.spec.js`, `epic-view.spec.js`, `overview.spec.js`, `onboarding.spec.js`, `system.spec.js`, `projects.spec.js`) — **test-first**, met. Passing at head.

**D4 — The work list beside a plan and an epic (PLAN-83's acceptance).** Commits `ab38de9c`, `fbdecca1`.
- Unit tests show the list in the order served, the open item marked, one step to a neighbour opening its plan view, and the same on an epic (`WorkListColumn.test.ts`, `PlanView.test.ts`, `EpicView.test.ts`) — **test-first**, met. Passing at head.
- Row `web-moving-between-plans` of `doc/plans/README.adoc` set `done` by the last of this plan, the `web-decision-announcer` plan and `PLAN-59` D4 (settled with this plan) — order `none`, met.

**D5 — Browser evidence.** Commit `a8bac391`.
- `work-list.spec.js` passes with `PM_MOCK_WEB_PORT` and `PM_WEB_PORT` set; waits on address and DOM — **test-first**, met. Passing at head.
- Browser check (Playwright MCP) on `running-work` at 1440×900 and 390×844, German and English, both themes — order `none`, met.

Pushed head verification commits: `51665bf9`, `fbdecca1`, `c5ffa453`, `47491274`, `4e8513d9`, `0411acce`. All passed unit tests (187 test files, 3359 unit tests passed) and verify gate.

## 2. Personas and skills

- Setup: `plan-marshall:persona-implementer`
- D0: `plan-marshall:persona-implementer`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D1: `plan-marshall:persona-implementer`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D2: `plan-marshall:persona-implementer`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D3: `plan-marshall:persona-implementer`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- D4: `plan-marshall:persona-implementer`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`.
- D5: `plan-marshall:persona-integration-tester`; skills: `pm-dev-frontend:javascript`, `pm-dev-frontend:jest-testing`.
- Review: `plan-marshall:persona-code-reviewer`.

## 3. Hypotheses

- HYPOTHESIS: closing or replacing an `open=` object is still missing (PLAN-143 D2, no function does it) — **confirmed** at `pm-web-app/src/lib/navigation/history.ts:162-184` and `runtime.ts:2020-2042`; implemented `closeOpenObject` and `replaceOpenObject` in D0.
- HYPOTHESIS: a project's plans and epics, in the order served, are readable from the data `getProjectDetail` already returns, so the work list needs no new endpoint — **confirmed** at `pm-web-app/src/lib/api/client.ts:2730` (`ProjectDetailResponse.plans` and `epics`).
- HYPOTHESIS: an epic's project is known from `getEpicResource` — **confirmed** at `pm-web-app/src/lib/api/client.ts:3171` (`EpicResource.project_id`).
- HYPOTHESIS: no view other than the ten named renders an in-page Back — **confirmed** by grep; in-page Back buttons removed across all named views.

## 4. Findings

- Self-review round 1 (2 findings):
  1. WorkListColumn ordering: preserved exact server ordering, fixed in `fbdecca1`.
  2. SettingsView test title cleanup: fixed in `fbdecca1`.
- CodeRabbit round 1 (18 findings):
  - outside-diff-1, outside-diff-2: handled in `c5ffa453`.
  - nitpick-1: verified replaceState is asserted directly in unit tests (`RowLink.test.ts`).
  - inline-1 through inline-15: handled in `c5ffa453`.
- CodeRabbit round 2 (5 findings):
  - outside-diff-3, outside-diff-4: handled in `47491274`.
  - inline-16 through inline-20: handled in `4e8513d9`.
- CodeRabbit round 3 (6 findings):
  - outside-diff-5, outside-diff-6, outside-diff-7: handled in `0411acce`.
  - inline-21, inline-22, inline-23: handled in `0411acce` (EpicView resource reset on epic change, ReportsView generic `openDetail` reuse and deleted subject check).
- All 31 review body findings handled and recorded. Unresolved review threads: 0.

## 5. Reviewers

- `coderabbit` (required): reviewed. Triggered on `fbdecca1`, `c5ffa453`, `4e8513d9`. All 31 findings disposed, threads resolved, `review-gate` reported `merge_allowed: true, reason: all_handled`.
- `sourcery` (optional): triggered on `fbdecca1`, silent/no findings.

## 6. Operator questions

none asked

## 7. Rule check

- `guard: none on this harness (unenforced)`.
- Shell file operations: none on main checkout; worktree clean before removal.
- Rule breaks: none.
- `spec_source`: `ledger_worktree`.
- `executor-skew`: `not_applicable` (source checkout / match).
- Mailbox read: attempted via `inbox read --plan-id 144-web-shell-up-breadcrumb-and-work-list` which reported `invalid_target_plan` error (known CLI limitation for digit-leading plan IDs).

## 8. Build server

- Setup preflight: `ready`.
- Mechanisms: gate builds executed via `daemon_longpoll`.
- Builds: 9 daemon builds during implementation (4 failed initially as part of test-first evidence, 5 passed); gate verify builds passed at `51665bf9`, `fbdecca1`, `c5ffa453`, `47491274`, `4e8513d9`, `0411acce`.
- Fallbacks: none.
- Killed jobs: none.

## 9. Residue

none

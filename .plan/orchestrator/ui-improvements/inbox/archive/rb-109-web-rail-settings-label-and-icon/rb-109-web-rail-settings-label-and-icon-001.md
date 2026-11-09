envelope_version=1
sender_type=plan
sender_id=rb-109-web-rail-settings-label-and-icon
epic=ui-improvements
kind=landing
created=2026-11-07T23:46:53Z

```landing-facts
schema=landing-facts/1
plan_id=rb-109-web-rail-settings-label-and-icon
epic=ui-improvements
pr=#367
merge_state=merged
cleanup_owed=false
deliverables_total=2
deliverables_done=2
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2080
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
step.sonar-roundtrip.open_issue_count=0
step.sonar-roundtrip.open_issue_rules=none
```

guard: none on this harness (unenforced)

# Landing Report: PLAN-109 Web Rail Settings Label and Cog Icon

## 1. Deliverables

### Deliverable 1: Web label and cog icon (Met)
- **DW 1.1:** E2E specs updated; strict mode checked on `devices.spec.js` and `onboarding.spec.js`. Unit test `NavRail.test.ts` introduced.
  - Order: `test-first`
  - Failing run: Commit `157ef52d`, Run `139a8341be9045cbb2609ba33ac62cbf` (`Unable to find an accessible element with the role "button" and name "Settings"`).
  - Passing commit: `4936c9f3`.
- **DW 1.2:** Icon SVG replaced at `pm-web-app/src/lib/icons/ui/nav-settings.svg` with 24×24 viewBox, stroke 1.5 currentColor cog icon; `uiIcons.test.ts` passing.
  - Order: `none`
  - Passing commit: `4936c9f3`.
- **DW 1.3:** Browser checks across `de` and `en`, dark and light themes, at 1440×900 and 390×844 on ports 21090/21091. Rendered text shows "Settings" / "Einstellungen" without truncation and no raw keys. Baseline and after screenshots saved under `.plan/temp/playwright-mcp/`.
  - Order: `none`
  - Passing commit: `4936c9f3`.

### Deliverable 2: `control.plan.settings` rename (Met)
- **DW 2.1:** Terminal action menu tests `ActionMenusTest.java` and `ActionsFullScreenTest.java` assert `"Plan settings"`, `"Plan settings…"`.
  - Order: `test-first`
  - Failing run: Commit `4936c9f3`, Run `80d1dad9233c4a5f8198e51d0e7a0868`.
  - Passing commit: `37a70b16` (and displayName polish in `3b02d393`).
- **DW 2.2:** `byState.ts` retains key `control.plan.settings`; web component test `WorkControlPanel.test.ts` asserts "Plan settings".
  - Order: `test-first`
  - Failing run: Commit `4936c9f3`, Run `80d1dad9233c4a5f8198e51d0e7a0868`.
  - Passing commit: `37a70b16`.
- **DW 2.3:** Sonar issue read at PR head confirms 0 open new-code issues.
  - Order: `none`
  - Passing commit: `3b02d393`.

Per-head gate runs:
- `37a70b16`: `quality-gate` passed (2,775 tests passed).
- `3b02d393`: `ControlCatalogueTest,ActionMenusTest` passed; GitHub Actions CI passed on all matrix jobs.

## 2. Personas and Skills

- **Personas applied:**
  - Setup: `persona-plan-marshall-agent`
  - Deliverable 1: `plan-marshall:persona-implementer`
  - Deliverable 2: `plan-marshall:persona-implementer`
  - Self-review: `plan-marshall:persona-code-reviewer`
  - PR comment triage: `plan-marshall:persona-code-reviewer`
- **Domain skills declared:**
  - Deliverable 1: `pm-dev-frontend:javascript`, `pm-dev-frontend:css`, `pm-dev-frontend:jest-testing`, `pm-documents:ref-asciidoc`
  - Deliverable 2: `pm-dev-java:java-core`, `pm-dev-java:junit-core`, `pm-dev-java-cui:cui-testing`, `pm-documents:ref-asciidoc`
  - Skills added: none (no surface drift).

## 3. Hypotheses

- **D1 Hypothesis:** `uiIcons.test.ts` checks asset names against the registry only, not SVG content.
  - **Verdict:** Refuted (`pm-web-app/src/lib/icons/uiIcons.test.ts:56-90` explicitly parses the XML, checks 24x24 viewBox, stroke 1.5 currentColor, round stroke caps/joins, and bans forbidden elements).
- **D1 Hypothesis:** `devices.spec.js:20` and `onboarding.spec.js:178` match more than one button under strict mode.
  - **Verdict:** Refuted (`devices.spec.js:20` runs on `/` and `onboarding.spec.js:178` runs on `/projects` where only `nav-settings-btn` exists; strict mode confirmed without duplicate matches).

## 4. Findings

- **Self-review Round 1:**
  - Finding 1: Browser check artifacts needed clarification in report — clarified, screenshots verified on disk.
  - Finding 2: `ActionMenusTest.java` display names retained old "Settings" text at lines 48, 67, 91 — fixed in commit `3b02d393`.
- **Self-review Round 2:** Clear (no open findings).
- **CI Findings:** None (all 20 checks succeeded).
- **Review Comments:**
  - CodeRabbit review thread `PRRT_kwDOVBOtss607qje`: Suggested handling narrow terminal resizing so action labels are not clipped with ellipsis. Refuted as standard `TuiLayout` behavior across all menu actions and out of scope for message bundle rename; replied and resolved.

## 5. Reviewers

- `coderabbit` (required, quota-limited): Reviewed commit `3b02d393`, check concluded SUCCESS. 1 comment triaged and resolved.
- `sourcery` (optional, quota-limited): Triggered at 2026-11-08T00:13:04+01:00; replied with weekly quota exhaustion notice (skipped, non-blocking).

## 6. Operator Questions

- None asked (all workflows ran without escalation).

## 7. Rule Check

- Guard: `guard: none on this harness (unenforced)`
- Spec copy read: `ledger_worktree` (`/Users/oliver/git/pm-mcp-clients/.plan/local/worktrees/_orchestrator/.plan/orchestrator/ui-improvements/plans/PLAN-109-web-rail-settings-label-and-icon.md`).
- `executor-skew`: matched (`v0.1.2080`).
- Rules kept: All section 1 rules kept. Worktree isolated in `.plan/local/worktrees/109-web-rail-settings-label-and-icon`. Clean git staging by path, conventional commits with `Co-Authored-By` trailer. Main checkout touched only via `switch-and-pull`.

## 8. Build Server

- Setup preflight: `ready`
- Routing: `auto` via `marshalld`
- Build mechanisms:
  - `daemon_longpoll`: 5 jobs (`139a8341be9045cbb2609ba33ac62cbf`, `6c18725c7d684914aa39bf7069aec576`, `80d1dad9233c4a5f8198e51d0e7a0868`, `51aa22ab5d9545a9ae996e0ef4263717`, `19b21f306e5a45c694f71939ba71028c`, `b25dcdb04ab4474891130376b0cf724b`)
  - `in_process_fallback`: 0
- Fallbacks: none
- Killed jobs: none

## 9. Residue

- None. Worktree deleted, branches pruned, `main` up to date at merge commit `56b43a02404b95023b16dc4304afe6998767f8bd`.

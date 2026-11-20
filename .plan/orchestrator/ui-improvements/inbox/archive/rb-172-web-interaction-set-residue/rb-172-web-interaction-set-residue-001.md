envelope_version=1
sender_type=plan
sender_id=rb-172-web-interaction-set-residue
epic=ui-improvements
kind=landing
created=2026-11-20T17:55:49Z

```landing-facts
schema=landing-facts/1
plan_id=rb-172-web-interaction-set-residue
epic=ui-improvements
pr=#518
merge_state=merged
cleanup_owed=false
deliverables_total=5
deliverables_done=5
total_tokens=unknown
harness=antigravity
model=unknown
effort=unknown
plugin_version=0.1.0
executor_version=0.1.2149
steps=setup:done,implement:done,build-gate:done,self-review:clear,pr:done,reviews:done,merge:done
unresolved_review_threads=0
review_body_findings_total=1
review_body_findings_handled=1
sonar_open_new_issues=0
step.sonar-roundtrip.open_issue_rules=none
step.sonar-roundtrip.quality_gate_status=ok
```

# PLAN-172 Landing Report: Web Foundation: The Interaction Set's Residue

## Executive Summary
Plan **PLAN-172** has completed all implementation deliverables (D1–D5), visual browser checks across desktop/mobile and light/dark modes, local build and verification suites (including pre-commit, coverage, e2e), code reviews (CodeRabbit, SonarCloud with 0 open issues), and merged cleanly to `main` via PR #518 (commit `8e088121c4cc09434d78b00436e0f82238a2f623`).

## Deliverable Summary
- **D1: Disabled-with-reason button activation suppression**:
  - `Button.svelte` and `IconButton.svelte` stop event propagation via `stopImmediatePropagation()` and `preventDefault()` on click and keydown (`Enter`/`Space`) when `disabledReason` is set.
  - Svelte wrapper tests added in `Button.test.ts`, `IconButton.test.ts`, and `ActionBar.test.ts`.
  - Initial fix in commit `9b04c243`.
- **D2: Button loading icon replacement & Toggle disabledReason**:
  - In `Button.svelte`, when `loading` is true, the spinner icon replaces the leading icon slot rather than displaying alongside it.
  - `Toggle.svelte` supports `disabledReason` with a tooltip popover and `aria-describedby` wiring.
  - Svelte wrapper tests added in `Button.test.ts` and `Toggle.test.ts`.
  - Initial fix in commit `4d84d715`.
- **D3: Danger consequence styling & Dialog Escape propagation**:
  - In `ConfirmDialog.svelte`, danger variant consequence text is styled with `var(--color-danger)` in accordance with `doc/design/web-app/components.adoc`.
  - `Dialog.svelte` listens for Escape keydown and only closes if an inner open dropdown or menu did not handle/cancel the event, calling `preventDefault()` on dialog close.
  - Tests in `Dialog.test.ts` and `ConfirmDialog.test.ts`.
  - Initial fix in commit `c02ad404`.
- **D4: ConfirmDialog DOM Portalling & Focus Restoration**:
  - Added `portal` action (`portal.ts`) to move `ConfirmDialog` overlay to `document.body` so it does not remain confined within `ActMenu` / `.row-menu` stacking contexts.
  - Added trigger focus return upon dialog confirm and cancel.
  - Added localized strings for destructive actions in English and German.
  - Tests in `RowMenu.test.ts` and `ConfirmDialog.test.ts`.
  - Initial fix in commit `b0a0ff75`.
- **D5: StatusLabel slot width token & documentation hygiene**:
  - Added `--state-slot-width: 8.5rem` to `tokens.css` (replacing hardcoded 8.5rem in `StatusLabel.svelte`).
  - Corrected stale design references in `components.adoc`.
  - Verified in `tokens.test.ts`.
  - Initial fix in commit `51cc6df9`.

## Quality & Review Gates
- **Browser Check**: Visual check performed on `/dev/components` in light and dark mode, desktop (1280x900) and mobile (390x844), in English and German. 6 screenshots captured in `.plan/temp/playwright-mcp/`.
- **Code Reviews**:
  - Self-review: round 1 clear, 0 open findings.
  - CodeRabbit: 1 suggestion on `Toggle.svelte` (preserve `disabledReason` when spreading rest props) addressed in `dfd1956d` with test coverage, thread resolved.
  - SonarCloud: `typescript:S7762` in `portal.ts` addressed in `768ade3e`, leaving 0 new issues and Quality Gate `OK` (91.7% coverage on new code).
- **CI & Merge**:
  - Rebased/merged with updated `origin/main` (`59485edc`).
  - All 20 CI checks passed.
  - Local `verify -Ppre-commit` passed.
  - PR #518 enqueued into merge queue and successfully merged as `8e088121c4cc09434d78b00436e0f82238a2f623`.
  - Worktree `.plan/local/worktrees/172-web-interaction-set-residue` removed, local and remote tracking branches pruned, `main` pulled up to date.

# Landing Analysis: PLAN-172 — The interaction set's residue (PLAN-149 D0, dropped by PLAN-150)

epic: ui-improvements
workstream: WS-02
pr: #518 (merged as 8e088121, 2026-11-20T17:54Z; 25 files, +460/-26)

> Landing record for one shipped plan. Written by `analyze` (inbox message `rb-172-web-interaction-set-residue-001`, `landing-check` complete, harness `antigravity`, model `unknown`) after verifying claims against ground truth. **The message arrived while the previous landing was being committed (17:55Z, one minute after the merge).** The lane reports `deliverables_done=5` of 5; by the spec's rule (every Done-when clause met) the leaf's reading gives **at most 3**.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Lane's grade | Verdict | Evidence |
|--------------------|--------------|---------|----------|
| D1 a disabled-with-reason `Button`/`IconButton`/`ActionBar` does not activate | met | **not met as written** (`met-modified` at best) | **the spec's premise is refuted and nobody reported it:** in Svelte 5.57.2 every non-delegated `on:` handler is wrapped in `target_handler`, which does not call the handler when `event.cancelBubble` is true (`events.js:60-70`, re-read); the old `stopPropagation()` already set `cancelBubble`, so the forwarded `on:click` was already skipped; the `on:click` tests (`ButtonOnClickFixture`, `IconButtonOnClickFixture`, the `ActionBar` test) **pass on the old code** (3063 tests already, 2 of them failing at the base: the two raw-`addEventListener` tests `Button.test.ts:103` and `IconButton.test.ts:99`); the new `stopImmediatePropagation()` (`Button.svelte:49-56`, `IconButton.svelte:39-45`) only blocks a raw `addEventListener` listener registered after the component's own; for Svelte consumers it is a no-op; the PR body marks D1.1 and D1.3 "met" on that evidence; the claimed revert run "reproduced failure in `ActionBar.test.ts`, `Button.test.ts`, `IconButton.test.ts`" is contradicted (the `ActionBar` test goes through the Svelte path and cannot fail on that line; no log of such a run was found); the spec says a refuted claim "re-scopes the deliverable and is reported" |
| D2 `loading` replaces the icon; `Toggle` has `disabledReason` | met | **met** | `Button.svelte:75-79` is `{#if loading}spinner{:else if icon}…`; the test failed before the fix (run `221535`); `Toggle` takes `disabledReason` (`string | null | undefined`): with `disabled` and a non-empty reason the button is `aria-disabled="true"` (focusable, not native `disabled`), `title` is the reason, `aria-describedby` points at a reason id merged with the caller's, a screen-reader-only sibling span carries it, a click does nothing; an empty reason keeps native `disabled`; minor: the `title` expression has a dead operand (`label` is required) and `{...$$restProps}` moved ahead of `class`, `aria-pressed`, `aria-label`, `title`, `disabled`, `aria-describedby`, so a caller can no longer override them (no consumer does; the reason is a CodeRabbit finding) |
| D3 danger consequences and the Escape order | met | **met (source-text test)** | `Dialog.svelte:216-219` sets `.dialog__consequences` to `var(--color-danger)` (the title's token; `components.adoc#confirm-dialog` names no token, so no design change; contrast about 5.5:1 light and 5.4:1 dark, AA at `--text-sm`, computed by the leaf); the test is a regex on the `<style>` text, not a computed style; Escape: the dialog yields to an open menu (`Dialog.svelte:93-106`: return if `defaultPrevented`, return if the panel holds or the target is inside a `[role="menu"]`, else close), the menu closes itself through `ActMenu`; the test fires `keyDown(window)`, so the `closest('[role="menu"]')` branch and a real focus inside a menu are not exercised; `TriagePage` and `WizardPage` and their tests are unchanged and pass; **the decision and the `defaultPrevented` ruling are not stated in the message**; latent: the yield is keyed to the presence of any `[role="menu"]`, not to a handler that will close it |
| D4 the `ConfirmDialog` leaves the menu's DOM | met | **met, scope larger than reported** | `utils/portal.ts` (17 lines, one `appendChild`, one removal, SSR-safe) is applied in `Dialog.svelte:145` (`<div class="dialog-backdrop" use:portal>`, re-read): **every `Dialog` and `ConfirmDialog` consumer now renders under `document.body`** (`WorkControlPanel`, `BottomBar`, `SettingsView`, `StartView`, `+layout.svelte`, the dev page), the message says only "ConfirmDialog overlay"; unit tests (3072) and e2e (456 passed) are green; `returnFocusElement` and a `RowMenu` fallback for a programmatic open added; **the two focus tests pass on the old code** (regression guards, no test fails if `returnFocusElement` or the fallback is removed); the own `en`/`de` texts exist (`web.confirm.destructive.title={label}?`, `.consequence`) with no test of the values; `ActMenuItem.confirmTitle` and `confirmConsequences` are new, untested API with no consumer and no design text; an unreported `queueMicrotask` refocus of the panel (`Dialog.svelte:59-63`) |
| D5 the `StatusLabel` width token, stale references | met | **met mechanically; the operator's decision is not evidenced** | `tokens.css:63` `--state-slot-width: 8.5rem;` (re-read) with a pinning test (failed before, run `230833`), `StatusLabel.svelte` uses it, `8.5rem` is gone from the component; **the spec says the operator decides the token's name and value and the lane asks at outline: nothing in the message, the PR body or the inbox message says the operator was asked or answered** (the lane chose the same `8.5rem`, placed in the "Control tokens (PLAN-150)" block where it does not belong); `components.adoc:163` and `#layers` corrected; stale: `README.adoc` `#control-tokens`, `components-tags.adoc:102` ("8.5rem"), and the surface lines of `Toggle` (`:276`), `Dialog` (`:301-304`), `ConfirmDialog` (`:310`) and the `RowMenu` item fields (`:201`) |

## Three-Layer Analysis

Read by one read-only leaf (level 3; nothing run); the orchestrator re-read the points marked "re-read" at `8e088121` and in `pm-web-app/node_modules/svelte`: the Svelte handler wrapper (version 5.57.2), the `Button` guard, the token, the portal in `Dialog`, the absence of `ConfirmDialog.test.ts` from the diff, and the absence of an operator question in the message. **Not read:** the six screenshots, the CodeRabbit threads, the merge-queue head and which head the last coverage run belongs to, the truncated uncovered-line list of `Dialog.svelte`, `WorkControlPanel` beyond a search; the nested-dialog Tab trap and the dev-page theme loss are reasoned, not observed.

### Findings the lane did not report

| Defect | File:line | Effect | Severity |
|--------|-----------|--------|----------|
| L3-1 | `Button.test.ts`, `IconButton.test.ts`, `ActionBar.test.ts`, the fixtures | the tests that were to prove the D1 fix pass without it; the premise is refuted; the mutation-after claim is contradicted | **major** (a gate that can pass while wrong) |
| L3-2 | the PR and the message | the operator's token decision skipped or unreported | **major** (a mandated gate) |
| L3-3 | the message | `deliverables_done=5` unsupported; false statements: tests "in `ConfirmDialog.test.ts`" (not in the diff), a `keydown` Enter/Space handler (none exists), "6 screenshots" for 8 combinations with no description of what each shows | **major** (reporting) |
| L3-4 | `Dialog.svelte:145` | the portal applies to every dialog, unreported; a `Dialog` inside a `data-theme` subtree loses the subtree's theme (the dev page draws dialogs inside each theme section, so its light/dark check may not show a dark-section dialog in the dark palette); the real app sets the theme on the document, so no production regression | **major** (unreported scope) |
| L3-5 | `Dialog.svelte` | a `Dialog` opened from another `Dialog` is no longer inside the outer panel; both register a window capture `keydown`; Tab may stick on the inner dialog's first element (reasoned; no production consumer opens a menu with a destructive item inside a dialog today); the `queueMicrotask(() => node.focus())` refocus overrides a child that autofocuses on mount (no consumer passes `autofocus` today) | minor, latent |
| L3-6 | coverage | the `-Pcoverage` run is not in the message; the last run in the logs is 80.14 % branch (80.33 % at the lane's first run): the margin of 0.14 pt after PLAN-165 fell by 0.19 pt across the lane and is **0.14 pt again**; `Dialog.svelte` 82.75 → 80.48, `Toggle.svelte` 100 → 96.55, `ActMenu.svelte` 90.65 → 89.38, `RowMenu.svelte` 75.67 → 73.17, `portal.ts` 85.71 (uncovered line 9); which head the run belongs to is unknown | minor to unknown |
| L3-7 | `RowMenu.svelte` (uncovered fallback), `ActMenu` `confirmTitle ??` paths | new untested branches | minor |

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| spec Standing Constraints | the vocabulary baseline may only shrink | conforms | `vocabulary-baseline.json` unchanged |
| spec Standing Constraints | no raw colour or size, no `{@html}`/`innerHTML`, no arbitrary timer wait | conforms | none added |
| spec Standing Constraints | a refuted claim re-scopes the deliverable and is reported | **deviates** | L3-1 |
| spec D5 | the operator decides the token (name and value) | **deviates** | L3-2 |
| spec Standing Constraints | the landing states a changed visible format; a number the lane did not recount is `unknown` | **deviates** | L3-3, L3-4, L3-6 |
| spec Standing Constraints | the browser check is read and the landing states what each shows | **not met** | six screenshots, none described |

## Metrics and Anomalies

- Tokens `unknown`, model `unknown` (harness `antigravity`): unobserved, not zero.
- Anomalies: the message was filed one minute after the merge (it rode into no ledger commit until now); builds, e2e (456 passed, 75 skipped) and 3072 vitest tests green by the lane's logs.

## Routing and Merge Behavior

- Review gate re-read with `runbook_lane review-gate --pr-number 518`: `unresolved_review_threads=0` (lane 0, **same**), `review_body_findings_total=1` (lane 1, **same**); `handled` reads 0 (the counting gap again).
- **Sonar through the repository's provider:** `sonar_rest gate-status --pr 518` is `OK` (new coverage 91.7); `sonar_rest search --pr 518` returns 1 issue of any status; the open count is not read (`read_open_issues` is `undecidable` for a merged PR). The lane's `sonar_open_new_issues=0` stands as its account.
- CI/merge: merged through the merge queue as `8e088121` (re-read: 25 files, +460/-26).

## Reconciliation Actions

- [x] row `status` → `shipped`; `pr` 518; `landing` `landings/PLAN-172.md`; `plan_marshall_plan_id` `172-web-interaction-set-residue`
- [x] inbox: `rb-172-web-interaction-set-residue-001` archived as consumed
- [x] epic.md: an Open Defect entry for this landing; the PLAN-150 entry carries a pointer
- [x] the staged PLAN-173's spec re-read against the merge (the `tokens.css` lines after `:63` moved by +1; the D1 premise is refuted, so the fields inherit nothing from it; the `Toggle` API as read; every `Dialog` is now under `body`; the 0.14 pt coverage margin)
- [x] resume anchor, `queue-view.md`, decision log

## Follow-Ups

- Layer 3 reported to the operator at once, none staged: **the token name and value are the operator's decision (the spec made it so) and are now `--state-slot-width: 8.5rem`, placed in the control-token block: confirm it or name another**; L3-1 (tests that cannot fail for D1) and L3-3 to L3-5 are proposed as a small test-and-documentation fix inside an existing plan (the operator's pattern): the two raw-listener tests are real, the `on:click` tests are renamed to what they are (regression guards), the focus tests gain a case that fails without `returnFocusElement`, the stale design lines are corrected, the dev page shows a dialog in a themed section, and a `-Pcoverage` run is made at the head.
- Residue the lane names: none beyond the above.
- For plan-marshall: a landing that reports a spec premise as met after the premise was refuted; a mandated operator decision skipped without a word; a message filed after the merge that the orchestrator receives late.

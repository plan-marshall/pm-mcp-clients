# Landing Analysis: PLAN-173 — Web Foundation: The Field System's Defects (PLAN-150 Layer 3)

epic: ui-improvements
workstream: WS-02
pr: #525 (merged by the merge queue as 88cc8cb3, 2026-11-21T18:35Z; 32 files, +2934/-278 against the first parent)

> Landing record for one shipped plan. Written by `analyze` (inbox message `web-field-system-fixes-005.md`, `landing-check` complete, plan-marshall lane, harness `claude`, model `claude-opus-5-5`, effort `low`) after verifying claims against ground truth. The lane reports `deliverables_done=6` of 6 (functional review: 4 met, 2 met with residuals). **By the leaf's reading, re-read by the orchestrator, deliverable 4 is not met (its part (b) is unfixed in the form module), and deliverables 3 and 6 carry clauses met only in intent.** By the spec's rule (every Done-when clause met), at most 3 of 6 are met as written.

## Deliverable Fidelity vs Spec

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D1 the control tokens follow the theme | shipped-as-specified (browser cascade not shown by reading) | `tokens.css:238-241` and `:282-285` re-declare `--control-bg` and `--control-border` in `[data-theme='light']` and `[data-theme='dark']` (re-read); `tokens.test.ts` pins the declarations by parsing the CSS text, not a computed style |
| D2 `NumberField` takes what is typed; empty is not "never" | shipped-modified | the range is checked in `commit()` (`NumberField.svelte:170-180`, re-read) on blur, Enter and step; **`commit()` writes `input.value` and calls `onchange` but fires no `input` event**, so an `oninput`-wired consumer keeps the unclamped value (L3-2) |
| D3 `SecretField` keeps what the operator did | shipped-modified | paste inserts at the selection and fires `oninput` once; the store-generation guard is correct (`SecretField.svelte:134-149`, re-read); **Done-when 3.2 "a rejected `onstore` shows the failure" is not implemented**: the `catch {}` restores the secret and shows nothing (the doc comment hands the report to the caller's `error`); **Done-when 3.3**: `git grep super-secret-token -- pm-web-app/src` still names `SecretField.test.ts:149-160` (input literals, not an absence assertion; re-read): met in intent, not in its literal form |
| D4 a pointer click, an IME confirm and a checkbox Enter each count once | **not met** | (a) `RadioGroup` fires `onchange` once, root-scoped lookup and `uniqueId` ids: met. **(b) `forms/index.ts:320-322` still builds `` `[name="${firstInvalid}"], #${firstInvalid}, [data-testid="${firstInvalid}"]` `` with no `CSS.escape` (re-read; `git grep CSS.escape` at 88cc8cb3 finds nothing in `pm-web-app/src`); no test uses a key like `2fa`, `a.b` or one with a quote, and the defect is not reported as "not reproduced".** (c) the IME handling reads correct in both event orders (`forms/index.ts:38-52`, re-read); the checkbox-Enter test `CheckField.test.ts:37` passes at the base too (CheckField's keydown handles Space only and its diff only replaces `Math.random`; re-read), so 4(c)'s checkbox half is fixed in `createFormKeyboard`, not test-first in the component |
| D5 focus visible in forced-colors mode; ids from one place | shipped-modified | `uniqueId` replaces every `Math.random` (e.g. `CheckField.svelte:42`, re-read); the outline guards are per field file in seven test files; **PLAN-150's layer-2 test #1, the `Field.test.ts:57-59` regex that allows `outline: none` followed by a box-shadow, is untouched (re-read; the file is not in the diff)**, so no role-derived guard covers a new field component; the forced-colors rendering is not shown by reading |
| D6 the form module speaks the operator's language; its guard asks | shipped-modified | `web.form.*` keys in `common.en`/`common.de`, the guard asks through `ConfirmDialog` (`UnsavedChangesGuard.svelte`); **Done-when 6.3 clause 1 ("moves focus by Tab") is not met as written**: `DemoFormFixture.test.ts:50` asserts enabled state, `tabIndex` and `compareDocumentPosition` and never moves focus (re-read); it fails if the order changes |
| added-unplanned | accepted scope deviation | `plan.activity.kind.egress` in `plan_view.{en,de}.properties` (`:68`, re-read), added because `main` was red after #522; nothing reads it (`ActivityEntryKind` has no `EGRESS` constant; CodeRabbit's note is borne out) |

## Three-Layer Analysis

The landing was read on three layers — the spec, the deliverable trace, and the merged code — by one read-only leaf, and the orchestrator re-read every point recorded as `checked` before writing it here. The reading is **by reading, no build, no test run, no write**: what only a run could show is tagged `not shown by reading`, never left as clean.

- **Dispatch:** one read-only leaf (`execution-context-level-3`, resolved for `orchestrator.analyze`) — reason: every full ship gets its own leaf; a 32-file merged diff is the reading depth the dispatch rule exists for. The leaf reported two harmless read-only slips (a failed Read of a non-existent `.plan/temp/` path; one `git grep` over `.` that printed `.plan/orchestrator` lines it did not use).
- **Re-read by the orchestrator at 88cc8cb3:** `forms/index.ts:38-52` and `:314-325`, `SecretField.svelte:134-150`, `NumberField.svelte:165-182`, `CheckField.svelte` (whole diff) and `CheckField.test.ts:35-45`, `DemoFormFixture.test.ts:44-62`, `Field.test.ts:55-59` (and its absence from the diff), `plan_view.en.properties:67-69`, `tokens.css:236-242`, `git grep super-secret-token` and `git grep CSS.escape`. Every checked point held.
- **Verdict:** layer 1 findings (2, minor/info); layer 2 findings (1 major, 5 minor); layer 3 findings (2 minor) and 4 not shown by reading.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence (file:line) |
|---------|-------|---------|----------|----------------------|
| L1-1 | Done-when 3.2 "shows the failure" names no mechanism or owner; Done-when 3.3's literal-grep clause conflicts with SecretField tests that need a literal secret as input — both read two ways | checked | confirms | spec `PLAN-173-…md:33` |
| L1-2 | the header says runbook lane, the Hand-Off block says plan-marshall; the hypothesis names `src/lib/styles/tokens.test.ts`, the file is `src/styles/tokens.test.ts`; every other premise holds at the first parent (clamp on input, paste replace, tokens at `:root` only, one `Math.random` per field, English defaults in `forms/index.ts`) | checked | confirms | spec `:130` |

**Layer 1 verdict:** findings — 2 (wording only; the premises held)

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|---------------------------------|---------|
| D4(b) | `forms/index.ts:321` unescaped selector | none for `2fa`/`a.b`/quoted keys | "deliverables 6 of 6" not borne out | checked — **major** |
| D6.3 clause 1 | `DemoFormFixture.test.ts:50` | order test, no Tab move | "moves focus by Tab" unmet | checked — minor |
| D3.2 | `SecretField.svelte:143` `catch {}` | restore-only test | "shows the failure" unmet | checked — minor |
| D4(c) checkbox | `forms/index.ts:46-51` | `CheckField.test.ts:37` passes at the base | not test-first in the component | checked — minor |
| D5 (PLAN-150 L2 #1) | `Field.test.ts:57` untouched | per-file guards in 7 tests | no role-derived guard | checked — minor |
| D3.3 | `SecretField.test.ts:149-160` | — | literal still named by the grep | checked — minor |
| added | `plan_view.en.properties:68` | none reads it | accepted scope deviation | checked — info |
| all | every deliverable traces to code and a test; `stores/unsentInput.ts` is in the Expected Surface and untouched (no Done-when needs it); the forms module has no production consumer besides `DemoFormFixture` | — | the report's accepted residue is borne out | checked — info |

**Layer 2 verdict:** findings — 1 major, 5 minor, 2 info

### Layer 3 — Correct

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `forms/index.ts:320-322` | a form key such as `2fa` (makes `#2fa` an invalid selector) or a key with a double quote, with at least one invalid field | `querySelector` throws `SyntaxError` inside `submit()` before `publish()`: the errors are never shown, focus does not move, and the rejected promise is unhandled at `DemoFormFixture.svelte:65` (`void form.submit().then(...)`); same defect as the L2 major | minor (keys are developer-chosen) |
| L3-2 | `NumberField.svelte:170-180` | a form wired by `oninput` (as `DemoFormFixture` wires `TextField`), `min=5`, typed `3`, Enter | the box shows 5, the form holds 3, Enter submits 3; `bind:value` and `onchange` consumers are correct; the onchange-or-bind contract is not stated | minor |

**Layer 3 not shown by reading:** L3-3 the composition and checkbox/radio Enter paths `return` without `preventDefault` (`forms/index.ts:44`), so a browser's native implicit submission may still reach the form's own `onsubmit`; L3-4 the guard flow (`UnsavedChangesGuard.svelte:54`) reads correct but SvelteKit's real `beforeNavigate`, history restore and popstate cancel are mocked in the tests; L3-5 the theme-subtree cascade, the forced-colors focus outline and the 1280/390 px, en/de, light/dark screenshots; L3-6 a multi-line interior paste and the native undo stack after `setRangeText` (`SecretField.svelte:107`). Info, not new in the diff: holding Enter repeats `onSubmit` (no `e.repeat` check, no in-flight guard in `createForm.submit`).

**Layer 3 verdict:** findings — 2 minor; not shown by reading — 4

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web Standards | ES modules only | conforms | new files use `import`/`export` |
| CLAUDE.md Web Standards | flat ESLint config only | conforms | `pm-web-app/eslint.config.js` only; no lint config touched |
| CLAUDE.md Web Standards | no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` | conforms | none in the `+` lines |
| CLAUDE.md Web Standards | no arbitrary timer wait in a test | conforms | no `setTimeout`/`waitForTimeout`; `vi.waitFor`, `tick()`, awaited events |
| CLAUDE.md Web Standards | the five npm scripts stay | conforms | `package.json` untouched |
| CLAUDE.md Code Standards | generated input, literal expectations | not_shown_by_reading | the vitest tests use hand-written inputs; the generator rule names the Java library, its web scope is unsettled |
| spec Standing Constraints | texts from the en/de catalogue | conforms | `web.form.*` in `common.en`/`common.de` |
| spec Standing Constraints | zero Sonar, coverage floor, verify and e2e green, screenshots | not_shown_by_reading | needs a run or a provider read (Sonar: see below) |

## Metrics and Anomalies

- Tokens: 39,630,413 (spans populations; 5-execute 24.9 M, 6-finalize 8.2 M); billing 172 M.
- Duration: 24h10m wall, 4h26m worked; idle 19h43m (4-plan idle 12h58m, 5-execute idle 5h24m — operator waits).
- Anomalies: 7 code-review loop-back rounds (9 firings, 4 operator-granted); one loop-back caused by the run's own squash-message comment counted by the pre-merge gate (lesson `2026-11-21-19-001` in the plan-marshall store); `archive-plan` reported `pending` in `steps`, but the plan is archived at `.plan/local/archived-plans/2026-11-21-web-field-system-fixes`.

## Routing and Merge Behavior

- Review: `runbook_lane review-gate --pr-number 525` re-read: `unresolved_review_threads=0` (landing 0, **same**); `review_body_findings_total=16` (landing **1** — **a false claim**: the provider shows 16 CodeRabbit body/inline findings: 1 minor, 1 nitpick, 14 inline). `review_body_findings_handled`: the landing says 1; the plan's own `pr-comment` findings ledger holds 19 findings, every one dispositioned (12 fixed, 5 taken into account, 2 accepted), so the handled count is **re-verified as all handled** while the landing's number is wrong. CodeRabbit reviewed the PR (its findings exist), so the one-review merge rule holds.
- CI/merge: merged through the merge queue as `88cc8cb3` after 913 s; no rebase conflict; the squash commit carries the PR body including the CodeRabbit summary (repository default `PR_BODY`, left unchanged by operator decision).
- Sonar: landing `sonar_open_new_issues=0`. `sonar read_open_issues --pr 525` is now `undecidable` (`head_revision_unavailable`, merged PR); the plan's own `sonar-scan-summary` recorded `count_status: confirmed`, `new_code_issue_count: 0` at `5bf56b6e` (the last analysed head, equal to the provider's `analysis_revision`). The 0 is measured, by the lane, at the last analysed head.

## Reconciliation Actions

- [x] row `status` → `shipped` — `orchestrator queue --transition PLAN-173 --status shipped`
- [x] row `pr` stamped `525`
- [x] row `landing` stamped `landings/PLAN-173.md`
- [x] row `plan_marshall_plan_id` stamped `web-field-system-fixes` (the sender id)
- [x] epic.md: Open Defect for the D4(b) major and the layer-3 minors; Decisions entry for the four promoted lessons
- [x] inbox: `web-field-system-fixes-001..005` archived as consumed
- [x] resume anchor updated; `queue-view.md` regenerated and committed with the row change

## Follow-Ups

- The D4(b) selector escape, L3-2 (NumberField commit fires no `input`), D3.2 (a rejected store shows nothing), D6.3 (Tab), D5's role-derived outline guard and the unread `plan.activity.kind.egress` key: **reported to the operator; no fix plan staged** (layer-3 and layer-2 findings become a staged spec only on the operator's word).
- Unblocked by this landing: PLAN-153, 88, 155, 156, 157 (they waited for PLAN-173).

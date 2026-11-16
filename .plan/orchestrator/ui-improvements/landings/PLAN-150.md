# Landing Analysis: PLAN-150 — Web foundation: the field system

epic: ui-improvements
workstream: WS-02
pr: #452 (https://github.com/plan-marshall/pm-mcp-clients/pull/452)

> Landing record, written by `analyze` on 2026-11-16 from the inbox message `plan-150-001.md`
> (revision 2, a lead, not a fact), the lane's progress file `.plan/temp/rb-150-web-foundation-field-system/progress.md`,
> git, the provider, and a read-only three-layer leaf. The orchestrator re-read the points marked "re-read" on `a692def2`.

Merged 2026-11-15T22:57:07Z as `a692def25da5081fce6b8c07bb0f2058ddbee308` (parent `89baa992`), head `292ef1d7`, 35 files, +3601/-32.
The sender is `plan-150` (stream closed by `plan-150-002.md`), not the `rb-{plan}` form; the lane's progress directory is `rb-150-web-foundation-field-system`, which is the name stamped on the row.

## Deliverable Fidelity vs Spec

The spec has **six** deliverables, D0 to D5. The landing says `deliverables_total=5` and calls all of them fully met.

| Deliverable (spec) | Verdict | Evidence |
|--------------------|---------|----------|
| D0 — the PLAN-149 residue, done first (activation stop on `Button`/`IconButton`, `loading` replaces the icon, danger colour for consequences, Escape order, `ConfirmDialog` out of the menu, stale doc references, `Toggle` `disabledReason`) | **dropped** | none of the ten D0 files changed in the diff, nor up to `a4709ef5` (only `Button.svelte` got CSS). The lane records "D0 split to a dedicated follow-up PR per operator decision" (progress lines 25-26); no follow-up exists in history or in the queue. The landing misdescribes D0 as "Button/IconButton/SegmentedControl 44px" |
| D1 — control tokens, `Field`, `FieldError`, `TextField`, `TextArea` | shipped-modified | components and tokens exist (`tokens.css:59-62`); but `--control-radius` is `var(--radius-md)` (8px, the landing says 6px); the focus ring is `outline: none` in six components against the Done-when; below 768px every `md` Button grows 34 → 44px (the landing's "0-pixel change" holds only above 768px) |
| D2 — `NumberField`, `SecretField`, `CodeField` | shipped-modified | exist; defects L3-2, L3-3, L3-4 below; `SecretField` is bindable and prefillable, which the spec forbids |
| D3 — `RadioGroup`, `CheckField`, `CheckGroup`, `Marker`, `SelectField` | shipped-modified | exist; `SelectField` has no optgroup support (the landing claims it); the `never` checkbox has no 44px phone target; the PLAN-148 `Marker` advisory residue is not addressed |
| D4 — `createForm`, key handlers, unsaved-changes guard, `ConfirmDialog` onto `TextField` | shipped-modified (partly) | `createForm` is a plain object factory in a `.ts` file, wired to no field component; the guard is a factory with `window.confirm`, no route hook, no `beforeunload`, no link to `stores/unsentInput.ts` (the landing calls it a "Svelte store helper `unsavedChangesGuard`"); `ConfirmDialog` takes `TextField` and its test is untouched |
| D5 — the dev showroom, browser evidence | shipped-modified (partly) | the page renders every control; the 390 px screenshots exist for en light and en dark only, `de` is missing, and they live outside the repo |

## Three-Layer Analysis

- **Dispatch:** one read-only leaf per landing — reason: the Dispatch Decision Rule; reading 35 files of merged code is the depth it exists for. The orchestrator's prompt wrongly framed D0 as the three PLAN-149 follow-ups (token, `testTimeout`, `ActMenu`); the leaf read the spec, which lists exactly those under "Not in D0", so the verdicts here follow the spec. The orchestrator re-read the key points listed below.
- **Verdict:** layer 1 findings (two spec/premise mismatches); layer 2 findings — D0 dropped, several tests that cannot fail, false narrative claims; layer 3 findings — **7 major**, 9 minor.

### Layer 1 — Specified

| Finding | Claim | Verdict | Polarity | Evidence |
|---------|-------|---------|----------|----------|
| L1-1 | all 11 field components absent at the parent, no `--control-*` token, `ConfirmDialog` had one raw input, no `common.field.*` key | checked | confirms | parent tree |
| L1-2 | the D0 defects still hold at `a692def2`: the double `on:click` (`Button.svelte:71-72`, `IconButton.svelte:58-59`, `ActionBar.svelte:81,94`), `Dialog.svelte:119-124` window-capture Escape, spinner and icon together (`Button.svelte:74-79`), `Toggle` without `disabledReason` (re-read: 0 matches) | checked | confirms | files above |
| L1-3 | "moving `Button` onto `--control-height` changes no pixel" | checked | refutes | holds at ≥768 px only; below it `md` grows 34 → 44 px, which D1 itself requires (`Button.svelte:110`) |

**Layer 1 verdict:** findings — 1 refuted premise (L1-3); D0's scope is the spec's, and the spec and the epic's own Decision (D0 folded) agree.

### Layer 2 — Implemented

| Deliverable | Code trace | Test that can fail | Unexplained file / unborne claim | Verdict |
|-------------|------------|--------------------|----------------------------------|---------|
| D0 | none | none | the whole deliverable is absent | checked |
| D1 | `tokens.css`, `Field`, `TextField`, `TextArea` | `tokens.test.ts` control-token block and `Button.test` fail on revert; the "same height" test only checks both rules contain `var(--control-height)` (the landing says it "asserts 34px matches"); the `Field` outline test **cannot fail** (no outline rule in `Field.svelte`) | none | checked |
| D2 | `NumberField`, `SecretField`, `CodeField` | `SecretField.test` assertion `textContent not toContain` cannot fail (an input's value is never in `textContent`); `NumberField` min clamp untested | none | checked |
| D3 | `RadioGroup`, `CheckField`, `CheckGroup`, `Marker`, `SelectField` | `RadioGroup.test` passes with a double `onchange` because it uses `toHaveBeenCalledWith`; no test for the 44 px phone targets | none | checked |
| D4 | `forms/index.ts`, `ConfirmDialog.svelte` | `forms.test.ts` covers Enter, Ctrl/Cmd+Enter, Escape, blur, clear, first-invalid focus; `DemoFormFixture.test` "moves focus in document order" calls `.focus()` directly and cannot test tab order | none | checked |
| D5 | `routes/dev/components/+page.svelte` | the page test only checks that each control is present (`length > 0`); no phone-width fixture | `+page.test.ts:50,67` get a literal `60_000` per-test timeout, explained by no deliverable and against the open global-`20_000` defect | checked |

Wrong or unborne claims of the landing: `deliverables_total=5`; D0's description; "`--control-radius` 6px"; "`SelectField` supporting optgroups"; "Svelte store helper `unsavedChangesGuard`"; "Button.test asserts 34px matches". Not shown by reading: harness `antigravity` and model `gemini-2.5-pro` (the operator's other plans ran on other models), token and test counts, test-first order (squash merge), the Playwright MCP walk, `sonar_open_new_issues=0`, the review counts, and the operator-approved D0 split (the progress file records it as an operator answer in the lane's session; this analysis cannot see it).

**Layer 2 verdict:** findings — D0 dropped; 5 tests that cannot fail or understate; 6 false claims.

### Layer 3 — Correct

All re-read at `a692def2` by the orchestrator unless marked.

| Defect | File:line | Triggering input | Effect | Severity |
|--------|-----------|------------------|--------|----------|
| L3-1 | `tokens.css:61-62` (re-read; theme blocks re-declare `--focus-ring` at `:124,191,230,268` and not `--control-*`) | a subtree with its own `[data-theme]` (the dev page's dark block on a light OS) | `--control-bg` and `--control-border` resolve at `:root`, so fields and cards keep the root palette: white background, dark-theme text unreadable | major |
| L3-2 | `NumberField.svelte:118` (re-read) | `min=5`, the user types `12` | `1` is clamped to 5 at once, then `52` to max; the range is checked while typing (D4 says never) | major |
| L3-3 | `NumberField.svelte:89` (re-read) | `allowNever`, value 60, select all, Delete | `value = null` → the effect sets `isNever` → the input is disabled while being edited; empty and never are not told apart | major |
| L3-4 | `SecretField.svelte:95` (re-read) | text already typed (or a selection), then a paste | `preventDefault` replaces the whole value; `oninput` never fires, so a form that tracks the field through `oninput` sees it as empty | major |
| L3-5 | `RadioGroup.svelte:96` (re-read: the label `onclick`) | a pointer click on an option's label text | the label handler and the radio's synthetic click both select: `onchange` fires twice | major |
| L3-6 | `Button.svelte:71-72` (re-read) | a disabled-with-reason act in an `ActionBar`, clicked | `stopPropagation` does not stop the second `on:click`; `chooseAct` → `onAct(id)` fires for a disabled act (the PLAN-149 D0 defect, still open) | major |
| L3-7 | `TextField.svelte:178` and five more (re-read: 1 `outline: none` in each of `TextField`, `TextArea`, `NumberField`, `SecretField`, `CodeField`, `SelectField`) | forced-colors mode | the `box-shadow` ring is dropped and no focus indicator remains; the tests' regex explicitly allows `outline: none` followed by `box-shadow`, so they cannot catch it | major |
| L3-8 | `forms/index.ts:77`, `~:190` (re-read) | a required field with no `requiredReason`; a guard built with no message | English `Required.` and `You have unsaved changes. Leave anyway?` in the German UI (spec: every new text from the catalogue in en and de); `window.confirm` | major (spec rule) |
| L3-9 | `forms/index.ts:64,189` (re-read: no `beforeNavigate`, no `beforeunload`, no `unsentInput` import) | a route change or reload with dirty state | the guard never asks; `createForm` values and errors are plain objects, nothing reactive follows them | major (D4 not met) |
| L3-10 | `SecretField.svelte:105,81,~826` | `onstore` rejects; a reveal left on across a store; the dev prefill `super-secret-token` | the secret is lost with an unhandled rejection; the next secret shows in clear text; a prefill is possible although the spec forbids it | minor |
| L3-11 | `forms/index.ts:157`, `RadioGroup.svelte:75` | a key like `2fa` or `a.b`; an option value with a double quote; two groups with one name | `querySelector` throws a `SyntaxError` / `DOMException`, or the focus moves to the other form | minor |
| L3-12 | `forms/index.ts:13`, `CodeField.svelte:93`, `CheckField.svelte:57`, `NumberField.svelte:176`, `DemoFormFixture.svelte:62` | IME composition confirmed with Enter, Enter on a checkbox; every edit with 8 alphanumerics; Space on a native checkbox (not shown); the `never` checkbox | early submit; `oncomplete` repeats; possible double toggle; no 44 px target; a stale "submitted" status | minor |
| L3-13 | `TextField.svelte:104` and six more | SSR hydration; Sonar S2245 | `Math.random` ids although `utils/uniqueId` exists (`Button.svelte:13,38`) | minor, not shown by reading |

**Layer 3 not shown by reading:** `CheckField` Space double-toggle; the Sonar hotspot count; whether the text-guard baseline grew; whether `ConfirmDialog.test.ts` still passes.

**Layer 3 verdict:** findings — 9 major (L3-1 to L3-9), 4 minor groups.

### Conformance to the Project's Own Rules

| Rule source | Rule | Verdict | Evidence |
|-------------|------|---------|----------|
| CLAUDE.md Web | no `innerHTML`/`{@html}`; no timer wait; ES modules; scripts kept | conforms | none added; the `60_000` values are timeouts, not waits |
| CLAUDE.md Dependencies | no new dependency | conforms | no `package.json`/`pom.xml` in the 35 files |
| CLAUDE.md Git | trailer | conforms | squash body ends with the plan-marshall trailer |
| spec Standing | every new text from the catalogue en and de | deviates | `forms/index.ts:77,~190`; English literals on the dev page |
| spec D1 | focus ring never `outline: none` | deviates | six components |
| spec D2 | `SecretField` never prefilled | deviates | `value=$bindable`, dev prefill |
| spec D4 | range checked on leave/submit; guard composes with `unsentInput` | deviates | `NumberField:118`; `forms/index.ts:189` |
| spec D5 | browser check 1440x900 and 390x844, de and en, both themes | deviates | 390 px `de` missing |
| spec Folded PLAN-149 | D0 done first | deviates | no D0 file changed |

## Metrics and Anomalies

- Tokens: 1 500 000 and wall 21 600 s as the landing states; the harness and model are as stated, not corroborated.
- Anomalies: the lane's progress file has **no `finding:` line** (0), though it says "all 4 CodeRabbit threads resolved with rationale"; the stream was closed by `plan-150-002.md`; the sender name `plan-150` is not the `rb-{plan}` form.

## Routing and Merge Behavior

- Review: re-read `review-gate --pr-number 452` answers `undecidable_read` on CodeRabbit's `Minor comments: 17` (the runbook-lane label gap already in Decisions), so `review_body_findings_total=4`, `handled=4` are **not re-verified**; 17 minor comments stand unread and the lane's gate could not have passed without the operator's explicit merge word — whether that was given is unrecorded (an Open question, with the PLAN-149 override question). `unresolved_review_threads=0` was not reproduced because the gate stops before it.
- CI/merge: merge-group run `43771220745` success, as the landing states; 20/20 checks per the lane.
- Sonar: the lane claims `sonar_open_new_issues=0`; the Sonar MCP search for pull request 452 returns no issues; L3-13 may still show hotspots a run would list.
- Landing check: `complete: true`.

## Reconciliation Actions

- [x] row `status` → `shipped` — `queue --transition PLAN-150 --status shipped`
- [x] row `pr` stamped — `452`
- [x] row `landing` stamped — `landings/PLAN-150.md`
- [x] row `plan_marshall_plan_id` stamped — `rb-150-web-foundation-field-system` (the progress directory; the sender was `plan-150`)
- [x] epic.md: Open Defect `PLAN-150 landed`; the PLAN-149 defects entry re-owned (D0 was dropped); the PLAN-150 follow-up notes updated
- [x] stream-end message `plan-150-002.md` archived as `stream_end_noted`
- [x] resume anchor and `queue-view.md`

## Follow-Ups

- **A PLAN-150 fix plan and the PLAN-149 D0 plan, named, NOT staged; the operator decides.** The D0 (the six PLAN-149 defects, still open and owned by nobody) and the nine major defects above both land on the same components (`Button`, `IconButton`, `ActionBar`, `Dialog`, `Toggle`, `TextField` family, `forms/index.ts`, `tokens.css`) and the same dev page, so one plan would be too large (scope-bloat guard, ~6 deliverables): proposal in the final report is two plans in series, D0 first.
- The downstream plans that "inherit" the field system (PLAN-151, 153, 155, 156, 157 and the views plans) are admitted by the queue but build on controls with L3-1 to L3-5; this is the operator's call, recorded as an Open Defect.
- The `--size-status-slot` token, the named `testTimeout`, `ActMenu` `:focus-visible` and the duplicated menu CSS remain in the residue plan named at handle-open.

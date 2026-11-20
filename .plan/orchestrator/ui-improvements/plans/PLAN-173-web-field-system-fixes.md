# PLAN-173: Web Foundation: The Field System's Defects (PLAN-150 Layer 3)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Staged on the operator's word of 2026-11-19 ("do 1", the PLAN-150 residue) from the operator's decision of 2026-11-18 (handle-open: two plans in series, the D0 plan `PLAN-172` first, then this field fix) and `landings/PLAN-150.md` (layer 2, layer 3 L3-1 to L3-13). The plans that build on the field system (`PLAN-88`, `PLAN-153`, `PLAN-155`, `PLAN-156`, `PLAN-157`) wait for this plan only. This spec is SELF-SUFFICIENT: the emitted command is a one-line pointer and carries no brief.

## Objective

PLAN-150 (#452) shipped the field system (`TextField`, `TextArea`, `NumberField`, `SecretField`, `CodeField`, `SelectField`, `CheckField`, `RadioGroup`, the `forms` module) with nine major defects: the control tokens are declared at `:root` only so a themed subtree keeps the root palette, `NumberField` clamps the range on every keystroke and locks when emptied, `SecretField` replaces the value on a paste and never fires `oninput`, `RadioGroup` fires `onchange` twice per click, six fields drop the focus indicator in forced-colors mode, and the form module shows English texts in the German UI and has an unsaved-changes guard that never asks. This plan fixes them, each with a test that fails before the fix and a test that can fail, so the views packages can rely on a field that does what its design says.

## Deliverables

Read first:
- `landings/PLAN-150.md` of this epic (layer 3 L3-1 to L3-13, layer 2: the five tests that cannot fail), `doc/design/web-app/components-fields.adoc`, `README.adoc` § `#control-tokens`.
- Every position below was re-read at `origin/main` `5f7074b7` (2026-11-19) by the orchestrator for the defects named "re-read"; the others are the analysis leaf's reading and are re-checked at outline. **L3-6 (a disabled act still fires) is `PLAN-172`'s, not this plan's.**

1. **The control tokens follow the theme** (L3-1)
   - What: `--control-bg` and `--control-border` are declared in `:root` only (`tokens.css:61-62`, re-read) while the theme blocks (`[data-theme='light']` at `:199`, `[data-theme='dark']` at `:239`, the system-dark media block) re-declare `--focus-ring` (`:126,195,236,276`) and not the control tokens, so a subtree with its own theme keeps the root palette: white fields, dark-theme text unreadable. The control tokens are re-declared where the palette they derive from is, or are derived in a way that follows the subtree (the lane decides at outline); `tokens.test.ts` pins both themes.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: in a `[data-theme='dark']` subtree on a light root, the resolved control background and border are the dark palette's (evidence order: `test-first`).
     - The same test for a `[data-theme='light']` subtree on a dark root (evidence order: `test-first`).
2. **`NumberField` takes what is typed, and empty is not "never"** (L3-2, L3-3)
   - What: `NumberField.svelte:115-120` clamps to `min` and `max` on every keystroke: with `min=5` the operator who types `12` gets `1` clamped to 5 at once, then `52` clamped to `max` (D4 of PLAN-150 said never while typing). The range is checked on commit (blur, Enter, step), and the value in between is what was typed. With `allowNever`, emptying the field (select all, Delete) sets `value = null`, the effect at `:89` sets `isNever` and the input is disabled while it is being edited: empty and never are told apart, and the input stays editable.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: with `min=5` typing `1` then `2` leaves `12` and fires no clamp before commit; commit with `3` clamps to 5 (evidence order: `test-first`).
     - A test fails at `5f7074b7` and passes: with `allowNever` and value 60, select all and Delete leaves an enabled, empty input and `isNever` false (evidence order: `test-first`).
     - The existing `NumberField` min-clamp test, which asserts nothing today (landing layer 2), asserts the commit clamp (evidence order: `mutation-after`).
3. **`SecretField` keeps what the operator did, and a secret is never lost or shown by accident** (L3-4, L3-10)
   - What: `SecretField.svelte:94-98` `handlePaste` calls `preventDefault` and sets `value = pasted.trim()`: it replaces the whole value even when text is typed or a selection is made, and `oninput` never fires, so a form that tracks the field through `oninput` sees it as empty. A paste inserts at the selection and `oninput` fires. A rejected `onstore` does not lose the secret and the rejection is handled (no unhandled rejection); `revealed` is reset after a store; the dev prefill `super-secret-token` is removed (the spec forbids a prefillable secret). The test `SecretField.test` assertion that `textContent` does not contain the secret cannot fail (an input's value is never in `textContent`): it asserts the input's `type` and value instead.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing, pm-dev-frontend:javascript-security
   - Done when:
     - Tests fail at `5f7074b7` and pass: a paste over a selection replaces only the selection; a paste after typed text appends at the caret; `oninput` fires with the new value (evidence order: `test-first`).
     - A test: a rejecting `onstore` keeps the secret in the field, shows the failure and raises no unhandled rejection; after a successful store `revealed` is false (evidence order: `test-first`).
     - `git grep -n "super-secret-token" -- pm-web-app/src` names no line outside a test that asserts its absence (evidence order: `none`).
4. **A pointer click, an IME confirm and a checkbox Enter each count once** (L3-5, L3-11, L3-12)
   - What: (a) `RadioGroup.svelte:96` (the label `onclick`) and the radio's own synthetic click both select, so `onchange` fires twice per pointer selection; once. `RadioGroup.test` passes with a double `onchange` because it uses `toHaveBeenCalledWith`: it asserts the count. (b) A key like `2fa` or `a.b` and an option value with a double quote throw in `querySelector` (`forms/index.ts:157`, `RadioGroup.svelte:75`), and two groups with one name move the focus to the other form: selectors are escaped, ids are unique per group. (c) IME composition confirmed with Enter submits early (`forms/index.ts:13`), Enter on a checkbox (`CheckField.svelte:57`) double-toggles or submits, `CodeField.svelte:93` `oncomplete` repeats on every edit once eight alphanumerics are present, and the `never` checkbox of `NumberField.svelte:176` has no 44 px target. The lane confirms each at outline (the analysis read them, not run) and fixes the ones that reproduce, naming those that do not.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: a pointer click on an option's label text calls `onchange` exactly once (evidence order: `test-first`).
     - Tests fail at `5f7074b7` and pass for each of (b) and (c) that reproduces; the others are listed as not reproduced with the run (evidence order: `test-first`).
5. **Focus is visible in forced-colors mode, and ids come from one place** (L3-7, L3-13)
   - What: `TextField.svelte:178`, and the same line in `TextArea`, `NumberField`, `SecretField`, `CodeField` and `SelectField` (re-read: one `outline: none` in each of the six), drop the outline and rely on a `box-shadow` ring, which forced-colors mode removes, so no focus indicator remains; the tests' regex explicitly allows `outline: none` followed by `box-shadow`, so they cannot catch it. The outline becomes `transparent` (or an equivalent that forced-colors mode shows) and the regex test fails on `outline: none`. Seven field components build ids with `Math.random` (`TextField.svelte:104` and six more, re-read: one each in `CheckField`, `CodeField`, `NumberField`, `SecretField`, `SelectField`, `TextArea`, `TextField`) although `utils/uniqueId` exists (`Button.svelte:13,38`), which keeps SSR hydration and Sonar S2245 quiet: they use it.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:css, pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: no field component's style holds `outline: none` without a replacement that forced-colors mode shows (evidence order: `test-first`).
     - `git grep -n "Math.random" -- pm-web-app/src/lib/components` names no field component (evidence order: `none`).
6. **The form module speaks the operator's language and its guard asks** (L3-8, L3-9)
   - What: `forms/index.ts:77` returns the English `Required.` and `:189` the English `You have unsaved changes. Leave anyway?` in the German UI (every new text from the catalogue in `en` and `de`), and the guard falls back to `window.confirm`. The texts come from the catalogue; the guard asks through the design's confirmation (`ConfirmDialog`), not `window.confirm`. **The unsaved-changes guard never asks:** `createUnsavedChangesGuard` (`:187-192`) has no route hook (`beforeNavigate`), no `beforeunload` and no link to `stores/unsentInput.ts`, and `createForm` (`:62-66`) values and errors are plain objects that nothing reactive follows. The guard is wired: a route change or a reload with dirty state asks; the dirty state is the form's own and is cleared by a submit; `createForm` is used by at least one field in the dev page and is reactive. The tests `DemoFormFixture.test` "moves focus in document order" (calls `.focus()` directly) and the dev page test (`length > 0`) are made to test what they name.
   - Persona: plan-marshall:persona-implementer
   - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
   - Done when:
     - A test fails at `5f7074b7` and passes: a required field with no `requiredReason` shows the German catalogue text in `de` and the English one in `en`; the guard's message is the catalogue text (evidence order: `test-first`).
     - A test fails at `5f7074b7` and passes: a navigation or `beforeunload` with a dirty form asks, and after a submit it does not (evidence order: `test-first`).
     - The document-order test moves focus by Tab and not by `.focus()`, and fails when the order is changed (evidence order: `mutation-after`).

Not in this plan, named so they are not lost (each stays in `epic.md` as an Open Defect): L3-6 and the whole interaction set (`PLAN-172`); the false claims of the PLAN-150 landing (a spec wording, not code: `--control-radius` 6px is 8px, no `SelectField` optgroups, "Svelte store helper"); the `+page.test.ts:50,67` literal `60_000` timeouts and the global `testTimeout: 20_000` (one defect, the operator's decision); the missing `de` screenshots at 390 px (the browser check of this plan makes them for the changed fields); CodeRabbit's 17 `Minor comments` of #452, which the review-gate parser cannot read (a plan-marshall label gap): the lane reads them on the provider once and lists any that this plan's fixes do not cover.

## Standing Constraints

- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues and 0 hotspots, read at the pull request head after the push (`unknown` for a failed read, never `0`); a dismissal is made only on the operator's selection. This is a merge gate.
- Build and test through Maven only (`CLAUDE.md`): `./mvnw verify -pl pm-web-app -am` and `./mvnw verify -Pe2e -pl pm-web-e2e -am` pass at the head; `-Pcoverage` runs as its own invocation and keeps the 80 % floor (the module's branch margin is 0.14 pt: every new branch is covered).
- No new dependency or plugin. ES modules only; no `innerHTML`, `outerHTML`, `insertAdjacentHTML` or `{@html}`; no arbitrary timer wait in a test (`CLAUDE.md` Web Standards).
- Components use the design system's tokens only; texts from the catalogue in `en` and `de` (`pm-api` packs); the vocabulary lint (`npm run lint:vocabulary`) stays at its baseline and may only shrink; the text guard's baseline may only shrink.
- **Every fix has a test that fails before it, and every test of a touched component can fail:** the five tests of PLAN-150's landing layer 2 that cannot fail or understate (the `Field` outline test, the `SecretField` `textContent` assertion, the `NumberField` min-clamp test, the `RadioGroup` count, the focus-order test) are repaired or replaced; the order of each Done-when is the order the evidence has, and a `none` Done-when is never described as failing.
- Browser check of the changed fields with the dev page (light and dark, en and de, 1280 px and 390 px), the screenshots read; the landing message states what each shows, and a changed visible format is named there.
- **The merge rule:**
  - At least one CodeRabbit review of some head, with its findings handled. If CodeRabbit refuses on quota, stop and ask the operator.
  - Every review thread replied and resolved, and every review body item (outside-diff, nitpick, additional comments) answered on the pull request, read from GitHub's thread state, never from a count kept in memory. Every reply answers its own finding.
  - The run merges the pull request itself once these gates hold (`CLAUDE.md`, Git Workflow). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- The landing verdict is given per Done-when clause, never per deliverable, and the landing message reports `deliverables_done` as the number of deliverables whose every Done-when clause is met; a number the lane did not recount is `unknown`.
- Scope: six deliverables, the split guard's limit, on one surface (the field components and the form module); proceeding unsplit is the recorded rationale. Pull request size: about 40 files; if the changed set passes 100 files, stop and ask the operator to split.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- The mailbox read refuses a plan id with a leading digit (`invalid_target_plan`): report it, never read it as no mail.

## Claim Labels

- OBSERVED: `--control-bg` and `--control-border` are declared once, at `:root`, and the theme blocks re-declare `--focus-ring` only — read at `pm-web-app/src/styles/tokens.css:61-62,126,195,236,276` at `5f7074b7`
- OBSERVED: `NumberField.svelte` clamps `num` to `min` and `max` and sets `value = num` in the input handler — read at `:115-120`; the `isNever` effect is at `:89`
- OBSERVED: `SecretField.svelte` `handlePaste` calls `e.preventDefault()` and sets `value = pasted.trim()` — read at `:94-98`
- OBSERVED: `RadioGroup.svelte` has `onclick={() => selectOption(opt)}` on the label — read at `:92-96`
- OBSERVED: each of `TextField`, `TextArea`, `NumberField`, `SecretField`, `CodeField` and `SelectField` holds one `outline: none`, and the seven field components (those six and `CheckField`) hold one `Math.random` each — read by `git grep -c` at `5f7074b7`
- OBSERVED: `forms/index.ts:77` returns `f.requiredReason ?? 'Required.'` and `:189` defaults `message` to `'You have unsaved changes. Leave anyway?'` — read at `5f7074b7`
- HYPOTHESIS: the guard has no `beforeNavigate`, no `beforeunload` and no `unsentInput` import — the analysis read `forms/index.ts:64,189`; the orchestrator re-read the two default texts only; confirm/refute at `forms/index.ts` and `stores/unsentInput.ts` (verify-at-outline)
- HYPOTHESIS: the IME, checkbox-Enter, `oncomplete` and 44 px defects of deliverable 4(c) and the selector defects of 4(b) — the analysis leaf's reading, not shown by running; confirm/refute by a failing test each (verify-at-outline; a defect that does not reproduce is reported)
- HYPOTHESIS: the rejected `onstore`, the `revealed` flag and the prefill of `SecretField` (`:105,81`, the dev prefill) — the leaf's reading; confirm/refute at outline
- Verify-first clause: before each deliverable, re-read the named lines at the then-current HEAD; `PLAN-172` changes `Button`, `IconButton`, `Toggle` and `Dialog` first. A refuted claim re-scopes the deliverable and is reported.

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css`
- OBSERVED: `pm-web-app/src/lib/components/TextField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/TextField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/TextArea.svelte`
- OBSERVED: `pm-web-app/src/lib/components/TextArea.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/NumberField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/NumberField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/SecretField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/SecretField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/CodeField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/CodeField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/SelectField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/SelectField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/CheckField.svelte`
- OBSERVED: `pm-web-app/src/lib/components/CheckField.test.ts`
- OBSERVED: `pm-web-app/src/lib/components/RadioGroup.svelte`
- OBSERVED: `pm-web-app/src/lib/components/RadioGroup.test.ts`
- OBSERVED: `pm-web-app/src/lib/forms/index.ts`
- OBSERVED: `pm-web-app/src/lib/stores/unsentInput.ts`
- HYPOTHESIS: `pm-web-app/src/lib/forms/forms.test.ts` and `pm-web-app/src/lib/components/DemoFormFixture.test.ts` (the focus-order and guard tests; the paths are read at outline)
- HYPOTHESIS: `pm-web-app/src/lib/styles/tokens.test.ts` (the token test; the path is read at outline)
- HYPOTHESIS: `pm-web-app/src/routes/dev/components/+page.svelte` and `+page.test.ts` (the dev page that carries the form and the browser check)
- HYPOTHESIS: `pm-api/src/main/resources/messages/common.en.properties` and `common.de.properties` (the form module's `Required.` and unsaved-changes texts; verify-at-outline which pack)

## Dependencies and Sequencing

- Depends on: `PLAN-150` (shipped, #452) and **`PLAN-172` (this plan is emitted after it has landed)**: `SecretField` needs `Toggle`'s `disabledReason`, and every field that uses `Button` inherits the activation fix.
- Overlaps with (read by `corpus cross-check` at staging, 2026-11-19; shipped, parked and superseded plans are not in the disjointness population): `PLAN-172` (`tokens.css`, `tokens.test.ts`, `common.en.properties`; this plan is emitted after it has landed, so no clash), and, staged, `PLAN-135` (`common.en.properties`, the dev page), `PLAN-136` (the dev page, `tokens.css`), `PLAN-144` (`common.en.properties`): whichever lands second rebases and keeps both sides. No running plan shares a file with it.
- Adjacent to: the views packages that wait for this plan (`PLAN-88`, `PLAN-153`, `PLAN-155`, `PLAN-156`, `PLAN-157`).

## Hand-Off Command

Lane: `plan-marshall` (the operator's word of 2026-11-20: plan-marshall plans, not runbook lanes; the spec below is read at `phase-1-init`, where "the lane" means the executing run).

```text
/plan-marshall task="implement .plan/orchestrator/ui-improvements/plans/PLAN-173-web-field-system-fixes.md"
```

## Write-Boundary

The plan implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message — the orchestrator owns every other ledger write — and reports its outcome through its PR and its inbox message. The inbox exception's qualifiers and the sole sanctioned write mechanism are stated in `persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary.

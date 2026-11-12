# PLAN-150: Web Foundation: The Field System (D1 Foundation, Part 4)

epic: ui-improvements
workstream: WS-02

> Staged plan spec, one shippable unit of work, ready for hand-off on the `runbook` lane. Drafted 2026-11-10 from the AGREED D1 documents. SELF-SUFFICIENT: the emitted command is a one-line pointer.

## Objective

No view may draw a raw `<input>`, `<textarea>` or `<select>`; the field components are the only place one is written. They do not exist yet (38 `.svelte` files carry a raw input element). This part builds the field system of
`components-fields.adoc`: the control tokens, `Field`, `FieldError`, `TextField`, `TextArea`, `NumberField`, `SecretField`, `CodeField`, `SelectField`, `RadioGroup`, `CheckField`, `CheckGroup`, and the one shape of a form
(blur checks, first invalid field, required-empty reason, unsaved-changes guard). `Compose` and `FormStatus` came with PLAN-147. Views adopt the fields in their own plans; this plan moves no view.

## Deliverables

Read first: `doc/design/web-app/components-fields.adoc` (all), `README.adoc` § `#control-tokens`, `components.adoc` § `#button`, `#dialog`, `shared/texts-and-languages.adoc` § `#field-texts`, `entry.adoc` § `#pairing`,
`machine-settings.adoc` (durations), and the pictures `web-component-{field,choice,special-fields,compose}.svg`.

1. **Control tokens, `Field`, `FieldError`, `TextField`, `TextArea`**
 - What:
   - `tokens.css`: `--control-height` (`34px`, `44px` below `768px`), `--control-radius` (`--radius-md`), `--control-bg` (`--color-bg-surface`), `--control-border` (`--color-border-strong`; `--color-danger` while invalid).
     `Button` draws its `md` height and radius from them, so a field and its button stand on one line.
   - `Field.svelte`: the label above the control, `(optional)` after an optional label (`common.field.optional`), the hint under it, the error in the hint's place while it shows, `aria-describedby` and `aria-invalid`, the disabled state with its reason.
   - `FieldError.svelte`: one message from the catalogue under one field. `TextField.svelte` (a monospace variant for paths, ids and references) and `TextArea.svelte` own the `<input>` and `<textarea>`.
   - Keys: `common.field.optional`, `common.field.reason_required` (_Give a reason._), `common.field.token` (_Token_).
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing, pm-dev-java:junit-core
 - Done when:
   - `tokens.test.ts` asserts the four control tokens and the phone value; `Button.test.ts` asserts the `md` button and a `TextField` have the same height. Evidence order: `test-first`.
   - `Field.test.ts`, `FieldError.test.ts`, `TextField.test.ts`, `TextArea.test.ts`: label, optional mark, hint, error, `aria-describedby`, `aria-invalid`, disabled reason, the focus ring (never `outline: none`), the monospace variant. Fail on a revert. Evidence order: `test-first`.
 - Implements: `components-fields.adoc` § `#field-set`, `#form-shape`, `#field-texts`; `README.adoc` § `#control-tokens`.
2. **`NumberField`, `SecretField`, `CodeField`**
 - What:
   - `NumberField`: the unit beside the control, `min`, `max`, `step`, `inputmode="numeric"`, and an explicit _never_ choice where a design offers one (a boolean `allowNever`; the value is then `null`, never `-1`). Key `common.field.never`.
   - `SecretField`: masked, a reveal `Toggle` (PLAN-149), `autocomplete="off"`, `spellcheck="false"`, `autocapitalize="off"`, pasted white space trimmed, never prefilled, never echoed, and forgotten once stored. One component for entering, rotating and replacing a secret.
   - `CodeField`: monospace and large, `autocomplete="one-time-code"`, accepted in any case with or without the dash (`KXQB-MRTN`), reports "complete" at eight characters.
   - `CredentialEntry.svelte` and the rotate dialog of the machine settings become `SecretField` in the plans that own them (PLAN-66, PLAN-60); this plan leaves both untouched.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:javascript-security, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests per component: the number bounds and the never case; the secret is masked, revealed only while the toggle is pressed, trimmed on paste, absent from the DOM after `onstore`; the code is accepted in each of lower case, upper case, with and without the dash.
     Each fails on a revert. Evidence order: `test-first`.
 - Implements: `components-fields.adoc` § `#special-fields`, `#field-set`; `entry.adoc` § `#pairing`.
3. **Choice controls**
 - What:
   - `SelectField` (the native `<select>` for more than five options or a list the runtime sends), `RadioGroup` (a `fieldset` whose legend is the question; an option is a full-width labelled row or a card, the whole row or card the target; a `Marker` snippet after an option's label),
     `CheckField` (the label row is the target) and `CheckGroup` (a `fieldset` with legend; row and card forms for a decision part that accepts several answers).
   - Each choice component is keyboard-complete (arrow keys in a radio group, `Space` on a check) and 44 px on a phone.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:css, pm-dev-frontend:jest-testing
 - Done when:
   - Tests: the legend, the card form, the whole card as the target, arrow-key movement and wrapping in `RadioGroup`, a `Marker` after a label, `CheckGroup` with none, one and all checked, disabled option with reason. Fail on a revert. Evidence order: `test-first`.
 - Implements: `components-fields.adoc` § `#choice`.
4. **The shape of a form**
 - What:
   - A `lib/forms/` module the fields and views share: `Enter` submits a one-line form, `Ctrl+Enter` (`⌘+Enter`) a text area, `Escape` cancels an inline edit; format and range are checked when the field is left and on submit (never while typing), the error goes as soon as the value is valid,
     and on submit the focus moves to the first invalid field.
   - A required empty field disables the submit with its `disabledReason`; the same reason is the form's `FormStatus` line and the empty field's described-by text.
   - The unsaved-changes guard: a store/helper a form uses to ask before a route change, a close or a reload while it is changed (operator decision D-5, assumed: this plan builds the reusable guard with tests; views adopt it in their own plans).
     It composes with `stores/unsentInput.ts` and does not replace it.
   - `ConfirmDialog`'s typed-name `<input>` becomes a `TextField`; its props are unchanged.
 - Persona: plan-marshall:persona-implementer
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - `forms` tests: Enter and Ctrl+Enter, blur check not on input, error cleared when valid, first-invalid focus, required-empty reason in the three places, the guard asking on change and silent when unchanged. Fail on a revert. Evidence order: `test-first`.
   - `ConfirmDialog.test.ts` still passes unchanged for the typed name (evidence order: `test-first`).
   - A demo form on the dev page is walked by keyboard in a browser (evidence order: `none`).
 - Implements: `components-fields.adoc` § `#form-shape`, `#validation`, `#submit`.
5. **Fixtures and browser evidence**
 - What: fixtures of every field component (rest, filled, invalid with error, disabled with reason, phone width), a radio group of cards with a recommended marker, a check group, a secret, a code, a number with unit and _never_, and the demo form, on the dev page in both themes and locales.
 - Persona: plan-marshall:persona-integration-tester
 - Skills: pm-dev-frontend:javascript, pm-dev-frontend:jest-testing
 - Done when:
   - Browser check (Playwright MCP) of `/dev/components` at 1440×900 and 390×844, `de` and `en`, both themes; screenshots recorded (evidence order: `none`).
   - The open-issue read at the pull request head is recorded (open Sonar issue count and rules left open; `unknown` for a failed read, never `0`); a confirmed `0` or every open issue dismissed (evidence order: `none`).

## Standing Constraints

- Build and test through Maven only (`CLAUDE.md`): `./mvnw -pl pm-web-app verify`, `-Ppre-commit` (review every rewrite), `./mvnw -pl pm-api verify`, and at the head `PM_MOCK_WEB_PORT=<mock port> PM_WEB_PORT=<app port> ./mvnw -pl pm-web-e2e verify -Pe2e`.
- No new dependency or plugin. Web standards of `CLAUDE.md`: ES modules, flat ESLint, no `innerHTML`/`outerHTML`/`insertAdjacentHTML`/`{@html}` with runtime or model text, no timer waits in tests, the npm scripts stay; no static `style` attribute (`lib/csp.test.ts`).
- Every component obeys `components.adoc` § `#rules` and § `#new-component`; every new text comes from the catalogue in English and German; the text guard baseline does not grow. Java tests: JUnit 5, input from `Generators`, no Mockito.
- A secret never reaches a log, a test snapshot, a URL or the DOM after it is stored.
- Ports of this plan: mock web port 21500, app port 21501 (`20000 + 10 × 150`) (`AGENTS.md` § Ports). Never stop or reuse a process this worktree did not start.
- Write-set discipline: the main checkout is never edited; work only in the session's own worktree and branch.
- Every `test-first` clause cites a FAILING and a PASSING run; every `mutation-after` clause cites the reverted code and the failing assertion from a log quoted in the pull request body.
- **The merge rule:**
  - At least one CodeRabbit review of some head with its findings handled; every review BODY answered where the reviewer reads it; every thread replied and resolved. If CodeRabbit refuses on quota, stop and ask the operator.
  - The pull request body states `unresolved_review_threads=N` and `review_body_findings_total/handled`, read through `ci pr comments`.
  - The run merges the pull request itself once these gates hold, without a further word of the operator (`CLAUDE.md`, Git Workflow, #423). Commits end with `Co-Authored-By: plan-marshall <noreply@cuioss.de>`. The pull request body carries no generator footer.
- ZERO Sonar findings (the operator, 2026-11-05): 0 open new-code issues, every open issue fixed in a commit of THIS pull request; state `sonar_open_new_issues=N` in the landing message.
  A dismissal is made only on the operator's selection through `sonar_rest transition`; the lane never records one of its own.

## Claim Labels

- OBSERVED: none of `Field`, `FieldError`, `TextField`, `TextArea`, `NumberField`, `SecretField`, `CodeField`, `SelectField`, `RadioGroup`, `CheckField`, `CheckGroup` exists; `FilterInput.svelte` and `CredentialEntry.svelte` do — read in `pm-web-app/src/lib/components/` at `1cc0e10e`
- OBSERVED: raw `<input>`, `<textarea>` or `<select>` appear in 38 `.svelte` files (views, shell and components) — read by `git grep -c "<input\|<textarea\|<select" -- pm-web-app/src`
- OBSERVED: `tokens.css` declares no `--control-*` token — read at `pm-web-app/src/styles/tokens.css`
- OBSERVED: `ConfirmDialog.svelte` has one raw `<input>` (the typed name) and the props `title`, `consequences`, `actionLabel`, `cancelLabel`, `typedName`, `busy`, `confirmDisabled`, `testid`, `onConfirm`, `onClose` — read at `ConfirmDialog.svelte:16-36`
- OBSERVED: `common.en.properties` has no `common.field.*` key; `stores/unsentInput.ts` exports `hasUnsentInput` and `hasUnsentInputStore` — read at both files
- OBSERVED: `Button.svelte` sizes are `sm` 16-px icon, `md` and `lg` 20-px icon and no height token — read at `Button.svelte`
- HYPOTHESIS: `Button` reaches its `md` height of 34 px by its own padding and line height, and moving it onto `--control-height` changes no pixel — confirm/refute at `Button.svelte` § `.btn--md` and `Button.test.ts` (verify-at-outline)
- HYPOTHESIS: no view reads `CredentialEntry`'s props through anything but its declared props, so a later rebuild on `SecretField` is possible without this plan — confirm/refute at `git grep CredentialEntry -- pm-web-app/src` (verify-at-outline)
- Verify-first clause: re-read `components-fields.adoc` and the consumers named at the current HEAD; if a parked plan's re-ground changed a field's prop, settle it before scoping. If the operator rules D-5 otherwise, drop the guard from deliverable 4.

## Expected Surface

- OBSERVED: `pm-web-app/src/styles/tokens.css` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/styles/tokens.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Button.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/Button.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/components/ConfirmDialog.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/lib/stores/unsentInput.ts` — present at HEAD 1cc0e10e (read)
- OBSERVED: `pm-web-app/src/lib/components/Field.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/Field.test.ts` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/FieldError.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/TextField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/TextArea.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/NumberField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/SecretField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/CodeField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/SelectField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/RadioGroup.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/CheckField.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/components/CheckGroup.svelte` — absent at HEAD 1cc0e10e; created by this plan
- OBSERVED: `pm-web-app/src/lib/forms/` — absent at HEAD 1cc0e10e; created by this plan (the form shape module and its tests)
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.svelte` — present at HEAD 1cc0e10e
- OBSERVED: `pm-web-app/src/routes/dev/components/+page.test.ts` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.en.properties` — present at HEAD 1cc0e10e
- OBSERVED: `pm-api/src/main/resources/messages/common.de.properties` — present at HEAD 1cc0e10e
- OBSERVED: `doc/design/web-app/components-fields.adoc` — present at HEAD 1cc0e10e (changed only if a prop differs)

## Dependencies and Sequencing

- Depends on: `PLAN-149` (`Toggle` for `SecretField`, `Dialog`), `PLAN-147` (`Compose`, `FormStatus`), `PLAN-148` (`Marker`); `PLAN-45` landed.
- Overlaps with:
  - `PLAN-45` (running): the dev page, `common.*`.
  - `PLAN-66`, `PLAN-64`, `PLAN-60` (parked): `CredentialEntry`, the rotate dialog and the onboarding choices that adopt the fields.
  - `PLAN-135` (parked): `Compose` consumer.
  - `PLAN-70`, `PLAN-72` (parked): `tokens.css`.
- Adjacent to: `FilterInput.svelte` (unchanged); `CredentialEntry.svelte` and `MachineSettingsView.svelte` (unchanged here).
- Unblocks: 51 (`RadioGroup`, `CheckGroup`, `TextArea`, `FormStatus`), 53 (`Field`, `RadioGroup`), 59, 60 (`SecretField`, `NumberField`, `SelectField`), 64, 66 (every choice, `SecretField`, `TextField`); PLAN-153, PLAN-155, PLAN-156, PLAN-157.

## Re-Grounding at Restart

Re-read every file above at the then-current HEAD. Run `corpus surfaces` against what landed since (PLAN-147 to PLAN-149, PLAN-129). Settle each HYPOTHESIS. Re-check the operator's answer to D-5 and that `components-fields.adoc` is still `AGREED`.

## Hand-Off Command

Lane: `runbook`.

```text
/plan-marshall-runbook-lane plan=.plan/orchestrator/ui-improvements/plans/PLAN-150-web-foundation-field-system.md
```

## Write-Boundary

The session implementing this spec touches only its own repository source and tests. It creates and edits NO file under `.plan/orchestrator/` other than its own `inbox/{sender}-{seq}` message.
The orchestrator owns every other ledger write (`persona-plan-orchestrator/standards/orchestration-model.md` § Ledger Write-Boundary).

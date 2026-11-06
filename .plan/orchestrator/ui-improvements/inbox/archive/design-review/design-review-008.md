envelope_version=1
sender_type=orchestrator
sender_id=design-review
epic=ui-improvements
kind=finding
created=2026-11-06T18:33:01Z
revision=4
amended=2026-11-06T20:06:28Z

# Forms and input fields: one field system with fixed labelling, validation and submit rules

Sender: operator design review, 2026-11-06. A full review of every form and input element (text fields, text areas, selects, radios, checkboxes, numbers, secrets, codes, filters, compose fields) in the web app, in the browser against the mock runtime and in the code, checked against the agreed designs. It builds on `design-review-007.md` (buttons, dialogs).

## Operator feedback
Review forms and input fields the same way as tags (006) and interaction elements (007): are structure, location and content consistent? (First checked: no staged plan owns this. PLAN-60 defines a form rule for the one settings view: one Save disabled until a change, a "changed" mark per field, reset to inherited, plain labels with one sentence of help. PLAN-66 covers the onboarding fields, and PLAN-45 covers load errors, not field errors.)

## Current state
**There is no form layer.**
- There is no `Field` / `TextField` / `TextArea` / `Select` / `Checkbox` / `Radio` component. About **100 raw `<input>` / `<textarea>` / `<select>` sit in 36 files**, with about 25 separate style blocks.
- Global CSS (`styles/base.css:59-86`) sets only the font, the background and the focus ring.
- No agreed document defines a field, hint or error component, input heights, error colours, required marking or the Enter rule.
- The shared pieces are `FilterInput` (4 views), `CredentialEntry` (1 use), `ConsentBox`, the onboarding choice screens, and `ConfirmDialog`'s type-the-name field.
- Only **2 `<form>` elements** exist (`PairingView:106`, `CredentialEntry:59`). Everything else is a button with `on:click`, so **Enter does nothing** in rename, issue / PR reference, every reason field, release reason, checkout path, instruct, hold and drop. Only `ChatPanel` sends on Enter.
- **`aria-invalid` appears 0 times, and no field has `aria-describedby`** for its hint or error. `required` / `inputmode` are not used (`maxlength` only on the pairing code).

**Seen in the browser:**
- **Decisions:**
  - the options are cards with tiny native radios in the corner;
  - the note field's label is an instruction ("Füge eine Notiz hinzu") with the placeholder "Optionaler Hinweis";
  - "Entscheide" is greyed out with no reason;
  - "Antworte in eigenen Worten" opens a placeholder-only text area.
- **Machine settings:** every section has its own button with its own word ("Speichern" beside inline radios, "Übernimm diese Fristen"). The durations are number fields inside a sentence ("Beendete Arbeit [30] Tage aufbewahren · Erinnerung nach [3] Tagen"). `machine-settings.adoc:174-179` draws one row per category, five of them with a "never" choice ("ask me again after" has none), while the code uses one number for every category that has no recorded value (`MachineSettingsView:218-225`).
- **Device rename** (`/settings`): the field opens in the name cell, too narrow for the name ("Primary Desktop Browse…"). "Speichern" sits beside it, "Abbrechen" wraps below it, Escape does nothing, Save with an empty name is ignored silently (`SettingsView:145`), and nothing confirms success. The page title "Maschinen-Einstellungen" disagrees with the breadcrumb and tab "Einstellungen der Maschine" (see `design-review-001.md`).

**From the code (main points):**
- **Labelling:**
  - **Placeholder as the only name:** the Projects filter (`ProjectExplorerView:454`, placeholder = aria-label = "filter"), own words (`DecisionView:1078`), the onboarding chat (`ProjectOnboardingView:703`).
  - **Placeholder repeating the hint:** `DecisionView:846`, `WorkControlPanel:916`.
  - **No accessible name at all:** the `PlanSettingsModal:123` selects, the instruct text areas (`EpicDecompositionView:429`, `DecompositionReviewView:197,226`), `cleanup-remind-days`.
  - **Machine ids as labels:** `param.name`, `value.aspect`, `entry.aspect`.
  - **Case and punctuation:** "filter" vs "Filter", "add a note" lowercase, "Checkout Path" / "Start Onboarding" title case (against `README.adoc:297`), colons in keys and in code.
  - **A sentence split around an input**, which German word order breaks.
  - **Optional / required** are never marked.
- **Layout:**
  - control heights not set or 40 / 44 px, radius sm / md / none, three backgrounds;
  - the label above, wrapped in a column, inline in a sentence or in a two-column row;
  - the button below, beside or per row;
  - hints in the card header instead of under the field (`ProjectOnboardingView:493`).
- **Validation and errors:**
  - Text is never checked client-side. "Disable until non-empty" in some places, error after the click in others (Start issue / PR), and on submit with the message at the **page top** in Machine settings.
  - About **ten different error classes**, most without `role="alert"`.
  - Raw `e.message` on screen (`PrAdoptPanel`, `ConfinedFilePicker`).
  - **`CredentialEntry:33`** shows its own Save label as the error when the message is empty.
  - **"Reason required"** worded four ways.
  - **Pairing** shows its error as a banner *above* the form, with a text that differs from `entry.adoc:48-51` ("the line under the input", "This code does not work. Get a new one …").
  - No invalid look on the field itself.
- **Submit and state:**
  - Save is never disabled until a change.
  - An unsaved-changes warning exists only in the Designer (`beforeunload`).
  - Busy is a bare `disabled` on the raw buttons (no spinner, no reason).
  - Success shows a notice in one place, a credential notice in another, and nothing for rename.
  - `size="small"` (`DecisionView:1090`) is not a defined size.
  - Save has three keys plus "Keep these durations", "Store the secret", "Store the new token" and "Remember my name".
- **Special inputs:**
  - **Secrets:** two implementations (`CredentialEntry`, the hand-rolled rotate modal `MachineSettingsView:602-660`), no reveal, no paste-trim, labelled "Secret" in one and "Token" in the other.
  - **Pairing code:** no `inputmode` and no `autocomplete="one-time-code"`.
  - **Numbers:** no `step` / `max` / unit, and "never" is `-1`.
  - **Choice controls:** a two-option choice is radios, a select or a button group with no rule. Documents mode is asked in two places with different controls (Machine settings vs Start). There are no switches.
- **Filters:** `FilterInput` follows the contract (`view-density-and-filtering.adoc:77-98`).
  - The Projects filter is hand-rolled (no count, Esc, `/` or live region), against ":77 one component".
  - Reports inserts its live region only while filtering, so it may not announce.
  - The keys are `decisions.filter.*` vs `report.filter.*` vs `common.filter.*`.
- **Accessibility:**
  - `FilterInput:138` and `DecisionView:1817` replace the focus ring with `outline: none`.
  - Native radios and checkboxes are unsized.
  - fieldset / legend are missing for documents mode (twice), the store / language radios and `ConsentBox` (its legend is the "Yes" text).
  - `ConfinedFilePicker` is a dialog without a name, with tabs without panels.
  - A stray `/>` after the icon in `HarnessChoice.svelte:151-152` renders as text.

## The design

### 1. One field system (components)
| Component | For | Owns |
|---|---|---|
| `Field` | the wrapper of every control | label, optional mark, hint, error, `aria-describedby`, `aria-invalid`, disabled |
| `TextField` / `TextArea` | free text, references, paths, reasons | `Field` + the control. Mono variant for paths, ids and references. |
| `NumberField` | counts and durations | unit beside the field, `min` / `max` / `step`, `inputmode="numeric"`, an explicit "never" option where a design asks for one (never a magic `-1`) |
| `SecretField` | tokens and secrets | masked, reveal toggle, `autocomplete="off"`, `spellcheck="false"`, `autocapitalize="off"`, pasted whitespace trimmed, never prefilled or echoed. One component for entering, rotating and replacing. |
| `CodeField` | the pairing code | mono, large, `autocomplete="one-time-code"`, any case, with or without the dash (`entry.adoc`) |
| `SelectField` | one of many (> 5 or a dynamic list) | `Field` + native `<select>` |
| `RadioGroup` | one of few (2–5, short labels), and option cards (decisions) | `fieldset` + `legend` = the question, each option a full-width labelled row or card (the whole card is the target) |
| `CheckField` / `CheckGroup` | independent yes/no, and multi-select lists | label row as the target, `fieldset` for groups |
| `FilterInput` | every list filter | the filter contract (unchanged), now also Projects |
| `Compose` | chat and instruct | one text area + send: Enter sends, Shift+Enter new line, no send during an IME / dictation composition. `Compose` is owned here; the `Chat` of `design-review-009.md` uses it and adds Stop, the draft, the counter, `#` references and the microphone. Also used by the instruct fields. |
| `FieldError` | one message under a field | text from the catalogue, linked by `aria-describedby` |
| `FormStatus` | the form's own result line above its buttons | "Gespeichert" / the form-level error, `aria-live="polite"` |

- No raw `<input>` / `<textarea>` / `<select>` outside `lib/components`.
- View modes stay `SegmentedControl` (007). A setting is never a segmented control.

### 2. One form shape
- **Every form is a `<form>`.** Enter submits a single-line form. In a text area, Enter adds a line and Ctrl/⌘+Enter submits, except in `Compose` (Enter sends). **Escape** cancels an inline edit and closes a dialog form (007: dialogs).
- **Layout:**
  - the label above the control, then the control, the hint under the control, and the error under the control (in place of the hint while it shows);
  - one column;
  - field width follows the content (code, number, date short; text up to the reading measure; paths and text areas full width);
  - a group (radios, checks) has its question as legend.
- **Footer:** `FormStatus` line, then the buttons right-aligned, cancel (ghost) left of submit (primary), as in dialogs (007). Inline single-field edits (rename) keep field, Save and Cancel on one line, wide enough for the value, and the row's other acts are hidden while editing.
- **Look (tokens, shared):** one control height, equal to `Button` md (34 px, `components.adoc:22,39`) on desktop and 44 px below 768 px, so a field and its button align on one line, one radius, one background, one border. Invalid = danger border + `FieldError`. Disabled = muted with its reason. The focus ring is never replaced by `outline: none`.

### 3. Labels, hints, placeholders
- **A label names the value as a noun phrase**, sentence case, no colon, no instruction: "Notiz", "Grund", "Pfad des Checkouts", "Neuer Name". It does not say "Füge eine Notiz hinzu".
- **Optional fields carry "(optional)"** after the label. Required is the default and is not marked.
- **A hint is one sentence** under the field, with the format or the consequence ("Verschieben löscht die Dateien im Checkout."). A hint is never repeated in the placeholder.
- **A placeholder is only an example value** (`KXQB-MRTN`, `~/git/my-service`, `#214 oder URL`), never the label or the hint.
- **Units sit beside the field** ("[30] Tage"). A sentence is never split around a field.
- **No machine ids as labels:** aspects, recipe parameters and settings get catalogue names.
- **One key per concept:** filter labels follow one pattern on `common.filter.*` ("Filter: Entscheidungen" or one agreed form), and there is one "Save", one "reason required" text ("Gib einen Grund an."), one secret label ("Token").

### 4. Validation and errors
- **Required and empty:** the submit button is disabled **with its reason** (`disabledReason`, `components.adoc:39`), e.g. "Gib einen Grund an.". This is the pattern most forms use already, and every form now follows it.
- **Format and range** (code, number, reference, path): checked on blur and on submit, never while typing. The error clears as soon as the value is valid. The field gets `aria-invalid` and a `FieldError`, and on submit focus moves to the first invalid field.
- **Server errors** are mapped through the shared error codes (`displayError` of PLAN-09, shipped; PLAN-45 is its first caller for load errors). An error that belongs to a field shows under it; otherwise it shows in the form's `FormStatus`. Never at the page top, never a raw `e.message`, never a button label.
- **One wording per rule**, from the catalogue.
- **Pairing** follows `entry.adoc:48-51`: the error is the line under the input, with its agreed text.

### 5. Submit and state
- **Settings forms** (Machine settings, project setup, plan settings, the one settings view of PLAN-60):
  - one Save per form (section), labelled "Speichere" (du-form, 007 §4), disabled until a change;
  - a "changed" mark per field and "Setze auf den geerbten Wert zurück" for an override (the PLAN-60 rule, made general);
  - after saving, `FormStatus` says "Gespeichert".
  - The browser's own preferences (theme, language) apply at once, without Save.
- **Unsaved changes:** leaving a changed form (route change, close, reload) asks first. This is a new per-form dirty state with a leave guard (route change, close, `beforeunload`), for every form, not only the Designer. The existing `unsentInput` store keeps its own role (it delays the self-reload after a runtime upgrade while any field holds unsent text, `stores/unsentInput.ts`).
- **Busy:** submit uses `Button loading`, the form's fields are read-only while it runs, and a second submit is impossible.
- **Act forms** (reason dialogs, decide, start, adopt): the submit label names the act (007), and success is the result itself (the dialog closes, the row changes). There is no extra notice beyond the form's own `FormStatus` line (for a decision, PLAN-51's "Entschieden: <Option>" is that line).

### 6. Choice controls
- **2–5 short options** → `RadioGroup`. **Option cards** (decisions, recipes) are `RadioGroup` cards where the whole card is the target. **More or dynamic options** → `SelectField`. **Independent yes/no** → `CheckField`. **Several of a list** → `CheckGroup` (file picker, cleanup items).
- **One question, one control everywhere:** documents mode is the same `RadioGroup` in Machine settings and in Start.
- **Durations** follow `machine-settings.adoc:174-179`: one `NumberField` per category, with "never" where the design offers it (not for "ask me again after").

### 7. Accessibility (in the components)
- Every control has a visible label (or, for the filter and search, a visible label or an `aria-label` from the catalogue). Groups have fieldset and legend.
- Targets are at least 24 px (44 px on the phone), via the label row for radios and checks.
- Errors are announced: `FieldError` via `aria-describedby` + `aria-invalid`, and the form result via the polite `FormStatus`. The filter's live region is always present.
- Dialog forms have a name (007: `Dialog`). Tabs are only real tabs.

## Agreed documents that change
- `web-app/components.adoc`: a new "Fields and forms" section with the components of 1 and the rules of 2–7.
- `web-app/components.adoc` `Button`: unchanged sizes; the field height follows `Button` md.
- `web-app/README.adoc`: control height and look tokens, the label rule (:297) extended (noun phrase, "(optional)", no colon).
- `shared/texts-and-languages.adoc`: label / hint / placeholder / error rules, one key per concept.
- `web-app/view-density-and-filtering.adoc` (edited by session D2, which owns that document): Projects joins the field matrix, the live region is always present, the key pattern.
- `web-app/entry.adoc` and `web-app/machine-settings.adoc`: unchanged (the code moves to them).

## Affected plans
- **PLAN-60 web-settings-one-place:** its form rule is the settings-form rule of 5. It builds on `Field`, `RadioGroup`, `NumberField`, `SecretField`. Device rename moves to the inline-edit rule.
- **PLAN-66 onboarding frame and roles:** the token entry is `SecretField`, the per-harness model text field is `TextField`, the choice screens are `RadioGroup` / `CheckGroup`. The stray `/>` in `HarnessChoice` goes.
- **PLAN-51 decisions triage:** option cards as `RadioGroup` cards, note as `TextField` "Notiz (optional)", own words as `Compose`, waiver reason as `TextArea` with the one "reason required" text, "Entscheide" disabled with its reason.
- **PLAN-59 projects (held per 003):** filter → `FilterInput`, release reason in `ConfirmDialog` with `TextArea`, per-aspect settings via the settings-form rule.
- **PLAN-53 start wizard:** fields via `Field`, Enter submits, errors under the field, documents mode as the shared `RadioGroup`.
- **PLAN-45 error and loading pattern:** field and form errors use the same `displayError` codes (PLAN-09) that PLAN-45 uses for load errors (4).
- **PLAN-88 designer panes:** frontmatter fields via `Field`, the file / paste inputs, the project select as `SelectField`.
- **PLAN-85 theme in browser settings:** the theme / language radios as `RadioGroup` (applied at once).
- **PLAN-72 token-cleanup-lint:** a lint that fails on raw `<input>` / `<textarea>` / `<select>` and on `outline: none` outside `lib/components`.
- **PLAN-73 text-guard-failing:** machine ids as labels, raw `e.message`, labels with colons or title case as expected failures.
- **PLAN-70 light theme / PLAN-74 docs:** the field look in both themes, and closing the docs.

## Suggested action (the epic decides)
1. **Design** (session D1 of `design-review-010.md`, together with 006 and 007): "Fields and forms" in `components.adoc` and the other document changes, then operator review to `AGREED`.
2. **Foundation (web):**
   - the components of 1 and the form shape of 2;
   - the catalogue clean-up (labels, one key per concept);
   - `CredentialEntry` + the rotate modal → `SecretField`;
   - the lint of PLAN-72.
3. **Apply per view** through the plans above. The forms no staged plan owns go into one package: Pairing (error under the input), the reason dialogs of `WorkControlPanel`, `HoldResolutionModal`, `FindingsReviewModal`, the decomposition instruct fields (one `Compose`), Cleanup and System dialogs, `ConfinedFilePicker`, `PrAdoptPanel`, `RecipeSelector`. Machine settings and device rename belong to PLAN-60, `PlanSettingsModal` to PLAN-55 (`design-review-010.md`).

## Process
The design of this message is produced **before any implementation**, in its own Claude Code session: design session **D1 (component vocabulary)** of `design-review-010.md`.
- That session sets the documents it creates or changes to `Status: DRAFT`, reviews them (`ref-documentation` docs review, `ref-asciidoc` validate and verify-links) and opens a pull request.
- The operator sets them to `AGREED` as its last commit, and the pull request is merged on the operator's word.
- Only after that merge do the implementation plans named above start or restart.

Order, document ownership, open points, what each plan waits for, and the merged per-plan instructions are in `design-review-010.md`.

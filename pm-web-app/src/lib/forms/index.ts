/**
 * The shape of a form (components-fields.adoc, One Form Shape, Checking and Errors, Submitting).
 *
 * Every form is a <form>. Enter submits a one-line form, Ctrl+Enter (Cmd+Enter) a text area,
 * Escape cancels an inline edit. Format and range are checked on blur and submit (never on input).
 * Required-empty field places reason in disabledReason, FormStatus and field description.
 * Unsaved-changes guard asks before route change, close or reload while dirty.
 */

import { derived, get, writable } from 'svelte/store';
import { t } from '../i18n';

/** The keyboard listeners of one one-line form, attached to the form element itself. */
export interface FormKeyboard {
  /** The form's `keydown` listener: Enter submits. */
  keydown: (e: KeyboardEvent) => void;
  /** The form's `keyup` listener: ends the window of a composition's confirming Enter. */
  keyup: () => void;
  /** The form's `compositionend` listener: opens the window of a composition's confirming Enter. */
  compositionend: () => void;
}

/**
 * @param value an attribute value a selector names
 * @returns the value escaped for a double-quoted CSS string, so a key with a quote or a backslash
 *          names its control instead of breaking the selector
 */
function cssStringValue(value: string): string {
  return value.replaceAll('\\', String.raw`\\`).replaceAll('"', String.raw`\"`);
}

/**
 * Keyboard listeners for a one-line form: Enter submits.
 *
 * The Enter that confirms an IME composition only ends the composition, and an Enter on a checkbox
 * or radio is not a submit: neither submits the form. Browsers order the confirming Enter
 * differently: Chromium and Firefox fire its `keydown` with `isComposing` set and `compositionend`
 * after it, Safari fires `compositionend` first and the `keydown` with `isComposing` cleared. So a
 * `compositionend` marks the composition as just ended: the next Enter `keydown` is the confirming
 * one and does not submit, and the next `keyup` or other key ends the mark, so in the Chromium order
 * (whose confirming `keydown` came before `compositionend`) the next genuine Enter submits.
 *
 * @param onSubmit called for an Enter that submits
 * @returns the listeners to attach to the form's `keydown`, `keyup` and `compositionend`
 */
export function createFormKeyboard(onSubmit: () => void): FormKeyboard {
  /** Whether a composition has just ended and the Enter that may have confirmed it is still due. */
  let compositionJustEnded = false;
  return {
    keydown(e: KeyboardEvent): void {
      const confirmsComposition = compositionJustEnded;
      compositionJustEnded = false;
      if (e.key !== 'Enter' || e.isComposing || confirmsComposition) return;
      const target = e.target as HTMLElement | null;
      if (target?.tagName === 'TEXTAREA' || target?.tagName === 'BUTTON') return;
      if (
        target instanceof HTMLInputElement &&
        (target.type === 'checkbox' || target.type === 'radio')
      ) {
        return;
      }
      e.preventDefault();
      onSubmit();
    },
    keyup(): void {
      compositionJustEnded = false;
    },
    compositionend(): void {
      compositionJustEnded = true;
    },
  };
}

/**
 * Keyboard handler for text areas: Ctrl+Enter (and Cmd+Enter on Mac) submits. Plain Enter adds a line,
 * and the Enter that confirms an IME composition (`isComposing`) never submits.
 */
export function handleTextAreaKeyDown(e: KeyboardEvent, onSubmit: () => void): void {
  if (e.isComposing) return;
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault();
    onSubmit();
  }
}

/**
 * Keyboard handler for inline editing: Escape cancels.
 */
export function handleInlineEditKeyDown(e: KeyboardEvent, onCancel: () => void): void {
  if (e.key === 'Escape') {
    e.preventDefault();
    onCancel();
  }
}

export interface FieldConfig<T = unknown> {
  initial: T;
  required?: boolean;
  requiredReason?: string;
  validate?: (value: T) => string | null | undefined;
}

export interface FormConfig<T extends Record<string, unknown>> {
  fields: {
    [K in keyof T]: FieldConfig<T[K]>;
  };
  onSubmit?: (values: T) => void | Promise<void>;
}

export interface FormStatusResult {
  message: string | null;
  tone: 'neutral' | 'danger' | 'success';
}

/** What a form holds at one moment: the values as typed and the errors its checks produced. */
export interface FormState<T extends Record<string, unknown>> {
  values: T;
  errors: Record<string, string | null>;
}

/**
 * @param val a field value
 * @returns whether a required field holding it counts as empty
 */
function isEmptyValue(val: unknown): boolean {
  return val === null || val === undefined || (typeof val === 'string' && val.trim() === '');
}

/**
 * Stands for the catalogue's default required reason in the errors a form holds. It is resolved
 * only when the errors are read, so a shown error follows the locale the screen shows then.
 */
const DEFAULT_REQUIRED_REASON = Symbol('web.form.required');

/** The catalogue key of the default required reason. */
const REQUIRED_REASON_KEY = 'web.form.required';

/** An error as a form holds it: a message, the unresolved default required reason, or none. */
type HeldError = string | typeof DEFAULT_REQUIRED_REASON | null;

/** The catalogue lookup of the active locale, as the `t` store hands it. */
type Translate = (key: string) => string;

/**
 * @param field the field's configuration
 * @returns the reason a required-empty field names: its own, or the catalogue's default, unresolved
 */
function heldRequiredReasonOf(
  field: Pick<FieldConfig, 'requiredReason'>,
): string | typeof DEFAULT_REQUIRED_REASON {
  return field.requiredReason ?? DEFAULT_REQUIRED_REASON;
}

/**
 * @param error an error as the form holds it
 * @param translate the catalogue lookup of the locale the screen shows
 * @returns the error as a reader shows it
 */
function resolveError(error: HeldError, translate: Translate): string | null {
  return error === DEFAULT_REQUIRED_REASON ? translate(REQUIRED_REASON_KEY) : error;
}

/**
 * @param errors the errors as the form holds them
 * @param translate the catalogue lookup of the locale the screen shows
 * @returns the errors as a reader shows them
 */
function resolveErrors(
  errors: Record<string, HeldError>,
  translate: Translate,
): Record<string, string | null> {
  const resolved: Record<string, string | null> = {};
  for (const [key, error] of Object.entries(errors)) {
    resolved[key] = resolveError(error, translate);
  }
  return resolved;
}

/**
 * @param field the field's configuration
 * @returns the reason a required-empty field names: its own, or the catalogue's default, read in
 *          the locale the screen shows when the reason is asked for
 */
function requiredReasonOf(field: Pick<FieldConfig, 'requiredReason'>): string {
  return field.requiredReason ?? get(t)(REQUIRED_REASON_KEY);
}

/**
 * Manages form state, blur checking, required-empty reason, and first invalid focus.
 *
 * The values and errors are held in a store the form exposes with `subscribe`, so a component that
 * reads it re-renders on input, blur, submit and a finished save. A shown catalogue reason is read in
 * the active locale whenever the errors are read, so it follows a language switch; the store follows
 * the locale only while it has a subscriber. A submit whose save succeeded makes the submitted values
 * the new baseline `isDirty` compares against.
 */
export function createForm<T extends Record<string, unknown>>(config: FormConfig<T>) {
  const fields = config.fields;
  const values = {} as T;
  const errors: Record<string, HeldError> = {};

  for (const [key, fieldConfig] of Object.entries(fields)) {
    values[key as keyof T] = (fieldConfig as FieldConfig<T[keyof T]>).initial;
  }

  let baseline: T = { ...values };
  /** The number of the latest save handed to `onSubmit`. */
  let latestSave = 0;
  /** The number of the newest save that has succeeded; only a newer success moves the baseline. */
  let appliedSave = 0;
  const held = writable<{ values: T; errors: Record<string, HeldError> }>({
    values: { ...values },
    errors: { ...errors },
  });
  const state = derived(
    [held, t],
    ([$held, $t]): FormState<T> => ({
      values: $held.values,
      errors: resolveErrors($held.errors, $t),
    }),
  );

  /** Hands the current values and errors to every subscriber. */
  function publish(): void {
    held.set({ values: { ...values }, errors: { ...errors } });
  }

  function validateFieldValue(key: string, val: unknown): HeldError {
    const f = fields[key as keyof T];
    if (!f) return null;
    if (f.required && isEmptyValue(val)) {
      return heldRequiredReasonOf(f);
    }
    if (f.validate) {
      return (f.validate as (value: unknown) => string | null | undefined)(val) ?? null;
    }
    return null;
  }

  function onFieldInput(field: keyof T & string, val: unknown): void {
    values[field] = val as T[keyof T & string];
    if (errors[field] && !validateFieldValue(field, val)) {
      errors[field] = null;
    }
    publish();
  }

  function onFieldBlur(field: keyof T & string): void {
    errors[field] = validateFieldValue(field, values[field]);
    publish();
  }

  function getErrors(): Record<string, string | null | undefined> {
    return resolveErrors(errors, get(t));
  }

  /**
   * Values are compared by identity (`Object.is`), so a field value is replaced, never mutated: a
   * caller that changes an array or an object field in place and reports the same reference through
   * `onFieldInput` is not seen as a change. Every field this repository passes through a form is a
   * scalar.
   *
   * @returns whether a value differs from the one the form started with or last submitted
   */
  function isDirty(): boolean {
    return Object.keys(fields).some((key) => !Object.is(values[key], baseline[key]));
  }

  function isSubmitDisabled(): boolean {
    for (const [key, f] of Object.entries(fields)) {
      if ((f.required && isEmptyValue(values[key as keyof T])) || errors[key]) {
        return true;
      }
    }
    return false;
  }

  function getDisabledReason(): string | undefined {
    for (const [key, f] of Object.entries(fields)) {
      if (f.required && isEmptyValue(values[key as keyof T])) {
        return requiredReasonOf(f);
      }
    }
    return undefined;
  }

  function getFormStatus(): FormStatusResult {
    const reason = getDisabledReason();
    if (reason) {
      return { message: reason, tone: 'danger' };
    }
    return { message: null, tone: 'neutral' };
  }

  function getFieldDescribedByText(field: keyof T & string): string | undefined {
    const f = fields[field];
    if (f?.required && isEmptyValue(values[field])) {
      return requiredReasonOf(f);
    }
    return undefined;
  }

  /**
   * Checks every field and, when all pass, hands the values to `onSubmit`. The values become the
   * baseline `isDirty` compares against only once the save has succeeded: while an async save is
   * pending and after a save that rejects or throws, the form stays dirty, so the unsaved-changes
   * guard still asks. Of saves that overlap, the newest one that has succeeded holds the baseline,
   * whatever order they resolve in: an older save that succeeds first moves the baseline to its
   * values (the values typed since stay dirty), and it stays there when a newer save fails; an
   * older save that succeeds after a newer one has succeeded leaves the baseline alone.
   *
   * @returns a promise of `false` for a refused submit (the first invalid field takes the focus),
   *          of `true` once the save has succeeded, superseded or not; it rejects with the error of
   *          a failed save
   */
  async function submit(): Promise<boolean> {
    let firstInvalid: string | null = null;

    for (const key of Object.keys(fields)) {
      const err = validateFieldValue(key, values[key as keyof T]);
      errors[key] = err;
      if (err && !firstInvalid) {
        firstInvalid = key;
      }
    }

    if (firstInvalid) {
      if (typeof document !== 'undefined') {
        // A field key is the operator's data, not a CSS identifier: `2fa`, `a.b` and `a"b` are
        // ordinary keys, and a selector built from them unescaped throws before the error is shown.
        // The id is read through its own lookup, which needs no escaping at all.
        const el =
          document.querySelector<HTMLElement>(`[name="${cssStringValue(firstInvalid)}"]`) ??
          document.getElementById(firstInvalid) ??
          document.querySelector<HTMLElement>(
            `[data-testid="${cssStringValue(firstInvalid)}"]`,
          );
        el?.focus();
      }
      publish();
      return false;
    }

    const submitted = { ...values };
    const sequence = ++latestSave;
    publish();
    // onSubmit gets its own copy: one that keeps and changes its argument never moves the baseline.
    await config.onSubmit?.({ ...submitted });
    // A success moves the baseline only when no newer save has succeeded yet: an older success
    // resolving after a newer one never moves the baseline back to the values that one replaced.
    if (sequence > appliedSave) {
      appliedSave = sequence;
      baseline = submitted;
    }
    publish();
    return true;
  }

  return {
    subscribe: state.subscribe,
    onFieldInput,
    onFieldBlur,
    getErrors,
    isDirty,
    isSubmitDisabled,
    getDisabledReason,
    getFormStatus,
    getFieldDescribedByText,
    submit,
  };
}

export interface UnsavedChangesGuardOptions {
  /** Whether the form holds changes that are not saved. */
  isDirty: () => boolean;
  /** Asks the operator whether to leave anyway; resolves `true` to leave, `false` to stay. */
  ask: () => Promise<boolean>;
}

/**
 * Guards against losing unsaved changes on navigation, closing or reload.
 *
 * Whether to ask is the form's own dirty state, read at each navigation: a form is dirty while a
 * value differs from the one last saved, and a successful submit moves that baseline itself, so a
 * submit never tells the guard anything and edits made while a save is pending are still asked for.
 * A route change asks through the caller's `ask` (the view's ConfirmDialog); a reload or a closed
 * tab is asked through the browser's own `beforeunload` prompt.
 *
 * The one thing the guard holds is a confirmed leave: `allowLeave()` lets the very next route check
 * pass unasked — the navigation the confirmed leave repeats — and that check consumes it; `endLeave()`
 * withdraws it once that navigation has settled, so a leave that did not happen never lets a later
 * navigation through. A `beforeunload` is not a route check and leaves the confirmed leave alone.
 */
export function createUnsavedChangesGuard(options: UnsavedChangesGuardOptions) {
  const { ask } = options;
  let leaving = false;

  /** @returns whether this navigation is to be asked for; consumes a confirmed leave */
  function shouldAsk(): boolean {
    if (leaving) {
      leaving = false;
      return false;
    }
    return options.isDirty();
  }

  return {
    shouldAsk,

    /** Lets the next navigation pass unasked: the operator confirmed leaving. */
    allowLeave(): void {
      leaving = true;
    },

    /** Withdraws a confirmed leave the next navigation has not consumed. */
    endLeave(): void {
      leaving = false;
    },

    async canLeave(): Promise<boolean> {
      if (!shouldAsk()) return true;
      return ask();
    },

    /**
     * Asks through the browser's prompt whenever the form is dirty. An unload is a departure of its
     * own, so it neither uses nor consumes a confirmed leave: that stays for the route change it
     * repeats.
     */
    handleBeforeUnload(e: BeforeUnloadEvent): void {
      if (!options.isDirty()) return;
      e.preventDefault();
    },
  };
}

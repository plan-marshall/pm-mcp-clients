/**
 * The shape of a form (components-fields.adoc, One Form Shape, Checking and Errors, Submitting).
 *
 * Every form is a <form>. Enter submits a one-line form, Ctrl+Enter (Cmd+Enter) a text area,
 * Escape cancels an inline edit. Format and range are checked on blur and submit (never on input).
 * Required-empty field places reason in disabledReason, FormStatus and field description.
 * Unsaved-changes guard asks before route change, close or reload while dirty.
 */

/**
 * Keyboard handler for one-line forms: Enter submits.
 */
export function handleFormKeyDown(e: KeyboardEvent, onSubmit: () => void): void {
  if (e.key === 'Enter') {
    const target = e.target as HTMLElement | null;
    if (target?.tagName === 'TEXTAREA' || target?.tagName === 'BUTTON') return;
    e.preventDefault();
    onSubmit();
  }
}

/**
 * Keyboard handler for text areas: Ctrl+Enter (and Cmd+Enter on Mac) submits. Plain Enter adds a line.
 */
export function handleTextAreaKeyDown(e: KeyboardEvent, onSubmit: () => void): void {
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

/**
 * Manages form state, blur checking, required-empty reason, and first invalid focus.
 */
export function createForm<T extends Record<string, unknown>>(config: FormConfig<T>) {
  const fields = config.fields;
  const values = {} as T;
  const errors: Record<string, string | null> = {};

  for (const [key, fieldConfig] of Object.entries(fields)) {
    values[key as keyof T] = (fieldConfig as FieldConfig<T[keyof T]>).initial;
  }

  function validateFieldValue(key: string, val: unknown): string | null {
    const f = fields[key as keyof T];
    if (!f) return null;
    if (f.required && (val === null || val === undefined || (typeof val === 'string' && val.trim() === ''))) {
      return f.requiredReason ?? 'Required.';
    }
    if (f.validate) {
      return (f.validate as (value: unknown) => string | null | undefined)(val) ?? null;
    }
    return null;
  }

  function onFieldInput(field: keyof T & string, val: unknown): void {
    values[field] = val as T[keyof T & string];
    if (errors[field]) {
      const err = validateFieldValue(field, val);
      if (!err) {
        errors[field] = null;
      }
    }
  }

  function onFieldBlur(field: keyof T & string): void {
    errors[field] = validateFieldValue(field, values[field]);
  }

  function getErrors(): Record<string, string | null | undefined> {
    return { ...errors };
  }

  function isSubmitDisabled(): boolean {
    for (const [key, f] of Object.entries(fields)) {
      const val = values[key as keyof T];
      if (f.required && (val === null || val === undefined || (typeof val === 'string' && val.trim() === ''))) {
        return true;
      }
      if (errors[key]) {
        return true;
      }
    }
    return false;
  }

  function getDisabledReason(): string | undefined {
    for (const [key, f] of Object.entries(fields)) {
      const val = values[key as keyof T];
      if (f.required && (val === null || val === undefined || (typeof val === 'string' && val.trim() === ''))) {
        return f.requiredReason;
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
    const val = values[field];
    if (f?.required && (val === null || val === undefined || (typeof val === 'string' && val.trim() === ''))) {
      return f.requiredReason;
    }
    return undefined;
  }

  function submit(): boolean {
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
        const el = document.querySelector<HTMLElement>(
          `[name="${firstInvalid}"], #${firstInvalid}, [data-testid="${firstInvalid}"]`,
        );
        el?.focus();
      }
      return false;
    }

    void config.onSubmit?.(values);
    return true;
  }

  return {
    values,
    onFieldInput,
    onFieldBlur,
    getErrors,
    isSubmitDisabled,
    getDisabledReason,
    getFormStatus,
    getFieldDescribedByText,
    submit,
  };
}

export interface UnsavedChangesGuardOptions {
  isDirty: () => boolean;
  message?: string;
}

/**
 * Guards against losing unsaved changes on navigation, closing or reload.
 */
export function createUnsavedChangesGuard(options: UnsavedChangesGuardOptions) {
  const { isDirty, message = 'You have unsaved changes. Leave anyway?' } = options;

  return {
    canLeave(): boolean {
      if (!isDirty()) return true;
      if (typeof window !== 'undefined' && typeof window.confirm === 'function') {
        return window.confirm(message);
      }
      return false;
    },

    handleBeforeUnload(e: BeforeUnloadEvent): void {
      if (!isDirty()) return;
      e.preventDefault();
    },
  };
}

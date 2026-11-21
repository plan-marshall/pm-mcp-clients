import { describe, it, expect, vi, afterEach } from 'vitest';
import {
  handleFormKeyDown,
  handleTextAreaKeyDown,
  handleInlineEditKeyDown,
  createForm,
  createUnsavedChangesGuard,
  type FormState,
} from './index';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

afterEach(() => {
  currentLocale.set('en');
  messagesStore.set(loadMessages('en'));
});

describe('lib/forms: The shape of a form', () => {
  describe('keyboard navigation and submits', () => {
    it('Enter submits a one-line form', () => {
      const onSubmit = vi.fn();
      const input = document.createElement('input');
      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      input.dispatchEvent(event);

      handleFormKeyDown(event, onSubmit);
      expect(onSubmit).toHaveBeenCalledOnce();
      expect(event.defaultPrevented).toBe(true);
    });

    it('Ctrl+Enter (and Cmd+Enter) submits a text area while plain Enter does not', () => {
      const onSubmit = vi.fn();
      const textarea = document.createElement('textarea');

      // Plain Enter in textarea does not submit
      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      textarea.dispatchEvent(enterEvent);
      handleTextAreaKeyDown(enterEvent, onSubmit);
      expect(onSubmit).not.toHaveBeenCalled();

      // Ctrl+Enter submits
      const ctrlEnterEvent = new KeyboardEvent('keydown', {
        key: 'Enter',
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      });
      textarea.dispatchEvent(ctrlEnterEvent);
      handleTextAreaKeyDown(ctrlEnterEvent, onSubmit);
      expect(onSubmit).toHaveBeenCalledOnce();
      expect(ctrlEnterEvent.defaultPrevented).toBe(true);

      // Cmd+Enter submits
      const cmdEnterEvent = new KeyboardEvent('keydown', {
        key: 'Enter',
        metaKey: true,
        bubbles: true,
        cancelable: true,
      });
      textarea.dispatchEvent(cmdEnterEvent);
      handleTextAreaKeyDown(cmdEnterEvent, onSubmit);
      expect(onSubmit).toHaveBeenCalledTimes(2);
      expect(cmdEnterEvent.defaultPrevented).toBe(true);
    });

    it('does not submit on the Enter that confirms an IME composition, in a one-line form or a text area', () => {
      const onSubmit = vi.fn();
      const input = document.createElement('input');
      const composing = new KeyboardEvent('keydown', {
        key: 'Enter',
        isComposing: true,
        bubbles: true,
        cancelable: true,
      });
      input.dispatchEvent(composing);
      handleFormKeyDown(composing, onSubmit);

      const textarea = document.createElement('textarea');
      const composingCtrl = new KeyboardEvent('keydown', {
        key: 'Enter',
        ctrlKey: true,
        isComposing: true,
        bubbles: true,
        cancelable: true,
      });
      textarea.dispatchEvent(composingCtrl);
      handleTextAreaKeyDown(composingCtrl, onSubmit);

      expect(onSubmit).not.toHaveBeenCalled();
      expect(composing.defaultPrevented).toBe(false);
      expect(composingCtrl.defaultPrevented).toBe(false);
    });

    it.each(['checkbox', 'radio'])(
      'does not submit on an Enter whose target is a %s input',
      (type) => {
        const onSubmit = vi.fn();
        const choice = document.createElement('input');
        choice.type = type;
        const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
        choice.dispatchEvent(event);

        handleFormKeyDown(event, onSubmit);

        expect(onSubmit).not.toHaveBeenCalled();
        expect(event.defaultPrevented).toBe(false);
      },
    );

    it.each(['TEXTAREA', 'BUTTON'])('does not submit on an Enter whose target is a %s', (tag) => {
      const onSubmit = vi.fn();
      const element = document.createElement(tag);
      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      element.dispatchEvent(event);

      handleFormKeyDown(event, onSubmit);

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('ignores a key other than Enter in a one-line form', () => {
      const onSubmit = vi.fn();
      const event = new KeyboardEvent('keydown', { key: 'a', bubbles: true, cancelable: true });
      document.createElement('input').dispatchEvent(event);

      handleFormKeyDown(event, onSubmit);

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('Escape cancels an inline edit', () => {
      const onCancel = vi.fn();
      const escEvent = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true });
      handleInlineEditKeyDown(escEvent, onCancel);
      expect(onCancel).toHaveBeenCalledOnce();
      expect(escEvent.defaultPrevented).toBe(true);
    });
  });

  describe('validation: blur checks, format/range, and first invalid focus', () => {
    it('checks format and range on blur and on submit, never while typing', () => {
      const form = createForm({
        fields: {
          code: {
            initial: '',
            validate: (val: string) => (val.length < 5 ? 'Code must be at least 5 chars.' : null),
          },
        },
      });

      // Typing does NOT produce error
      form.onFieldInput('code', 'abc');
      expect(form.getErrors().code).toBeUndefined();

      // Leaving field (blur) performs check
      form.onFieldBlur('code');
      expect(form.getErrors().code).toBe('Code must be at least 5 chars.');

      // Error clears as soon as value is valid
      form.onFieldInput('code', 'abcdef');
      expect(form.getErrors().code).toBeNull();
    });

    it('moves focus to the first invalid field on submit', async () => {
      const form = createForm({
        fields: {
          first: {
            initial: '',
            validate: (val: string) => (!val ? 'First is required.' : null),
          },
          second: {
            initial: '',
            validate: (val: string) => (!val ? 'Second is required.' : null),
          },
        },
      });

      const firstInput = document.createElement('input');
      firstInput.setAttribute('name', 'first');
      const focusSpy = vi.spyOn(firstInput, 'focus');
      document.body.appendChild(firstInput);

      const secondInput = document.createElement('input');
      secondInput.setAttribute('name', 'second');
      document.body.appendChild(secondInput);

      const submitted = await form.submit();
      expect(submitted).toBe(false);
      expect(focusSpy).toHaveBeenCalledOnce();

      document.body.removeChild(firstInput);
      document.body.removeChild(secondInput);
    });

    it('required-empty field places reason in disabledReason, FormStatus, and field description', () => {
      const form = createForm({
        fields: {
          reason: {
            initial: '',
            required: true,
            requiredReason: 'Give a reason.',
          },
        },
      });

      // 1. Submit button is disabled with disabledReason
      expect(form.isSubmitDisabled()).toBe(true);
      expect(form.getDisabledReason()).toBe('Give a reason.');

      // 2. FormStatus result line has the same reason
      expect(form.getFormStatus().message).toBe('Give a reason.');
      expect(form.getFormStatus().tone).toBe('danger');

      // 3. Field described-by text receives the reason
      expect(form.getFieldDescribedByText('reason')).toBe('Give a reason.');

      // Once filled, submit is enabled and reason clears
      form.onFieldInput('reason', 'Fixes issue #123');
      expect(form.isSubmitDisabled()).toBe(false);
      expect(form.getDisabledReason()).toBeUndefined();
      expect(form.getFormStatus().message).toBeNull();
      expect(form.getFieldDescribedByText('reason')).toBeUndefined();
    });

    it.each([
      ['en', 'Fill in this field.'],
      ['de', 'Fülle dieses Feld aus.'],
    ] as const)(
      'names the catalogue reason of a required field that sets none, in %s',
      (locale, reason) => {
        currentLocale.set(locale);
        messagesStore.set(loadMessages(locale));
        const form = createForm({ fields: { title: { initial: '', required: true } } });

        form.onFieldBlur('title');

        expect(form.getErrors().title).toBe(reason);
        expect(form.getDisabledReason()).toBe(reason);
        expect(form.getFormStatus().message).toBe(reason);
        expect(form.getFieldDescribedByText('title')).toBe(reason);
      },
    );

    it('hands its values and errors to a subscriber on input, blur, submit and save', async () => {
      const form = createForm({
        fields: {
          code: {
            initial: '',
            validate: (val: string) => (val.length < 5 ? 'Code must be at least 5 chars.' : null),
          },
        },
      });
      const seen: FormState<{ code: string }>[] = [];
      const unsubscribe = form.subscribe((state) => seen.push(state));

      form.onFieldInput('code', 'abc');
      form.onFieldBlur('code');
      form.onFieldInput('code', 'abcdef');
      const submitted = await form.submit();
      unsubscribe();

      expect(submitted).toBe(true);
      // The submit hands the checked values on, and the finished save hands them on once more.
      expect(seen.map((state) => state.values.code)).toEqual([
        '',
        'abc',
        'abc',
        'abcdef',
        'abcdef',
        'abcdef',
      ]);
      expect(seen.map((state) => state.errors.code)).toEqual([
        undefined,
        undefined,
        'Code must be at least 5 chars.',
        null,
        null,
        null,
      ]);
    });

    it('hands the errors of a refused submit to a subscriber', async () => {
      const form = createForm({ fields: { title: { initial: '', required: true } } });
      const seen: FormState<{ title: string }>[] = [];
      const unsubscribe = form.subscribe((state) => seen.push(state));

      const submitted = await form.submit();
      unsubscribe();

      expect(submitted).toBe(false);
      expect(seen.at(-1)?.errors.title).toBe('Fill in this field.');
    });

    it('is dirty once a value changes, and a successful submit makes the values its baseline', async () => {
      const onSubmit = vi.fn();
      const form = createForm({ fields: { title: { initial: '' } }, onSubmit });
      expect(form.isDirty()).toBe(false);

      form.onFieldInput('title', 'Release notes');
      expect(form.isDirty()).toBe(true);

      await expect(form.submit()).resolves.toBe(true);
      expect(onSubmit).toHaveBeenCalledWith({ title: 'Release notes' });
      expect(form.isDirty()).toBe(false);

      form.onFieldInput('title', 'Release notes, second draft');
      expect(form.isDirty()).toBe(true);
    });

    it('makes the values clean without an onSubmit of the caller', async () => {
      const form = createForm({ fields: { title: { initial: '' } } });
      form.onFieldInput('title', 'Release notes');

      await expect(form.submit()).resolves.toBe(true);

      expect(form.isDirty()).toBe(false);
    });

    it('stays dirty while an async save is pending and is clean once it has resolved', async () => {
      let resolveSave: () => void = () => {};
      const onSubmit = vi.fn(
        () =>
          new Promise<void>((resolve) => {
            resolveSave = resolve;
          }),
      );
      const form = createForm({ fields: { title: { initial: '' } }, onSubmit });
      form.onFieldInput('title', 'Release notes');

      const saving = form.submit();
      expect(onSubmit).toHaveBeenCalledExactlyOnceWith({ title: 'Release notes' });
      expect(form.isDirty()).toBe(true);

      resolveSave();
      await expect(saving).resolves.toBe(true);
      expect(form.isDirty()).toBe(false);
    });

    it('keeps the submitted values as the baseline when the operator types on during the save', async () => {
      let resolveSave: () => void = () => {};
      const onSubmit = () =>
        new Promise<void>((resolve) => {
          resolveSave = resolve;
        });
      const form = createForm({ fields: { title: { initial: '' } }, onSubmit });
      form.onFieldInput('title', 'Release notes');

      const saving = form.submit();
      form.onFieldInput('title', 'Release notes, second draft');
      resolveSave();
      await saving;

      expect(form.isDirty()).toBe(true);
      form.onFieldInput('title', 'Release notes');
      expect(form.isDirty()).toBe(false);
    });

    /**
     * @returns a form whose saves stay pending until the test resolves (`saves`) or rejects
     *          (`failures`) each by its position
     */
    function formWithPendingSaves() {
      const saves: Array<() => void> = [];
      const failures: Array<(error: Error) => void> = [];
      const form = createForm({
        fields: { title: { initial: '' } },
        onSubmit: () =>
          new Promise<void>((resolve, reject) => {
            saves.push(resolve);
            failures.push(reject);
          }),
      });
      return { form, saves, failures };
    }

    it('keeps the values of an older successful save as the baseline when the newer save rejects', async () => {
      const { form, saves, failures } = formWithPendingSaves();
      form.onFieldInput('title', 'Release notes');
      const older = form.submit();
      form.onFieldInput('title', 'Release notes, second draft');
      const newer = form.submit();
      const failure = new Error('save failed');

      saves[0]();
      await expect(older).resolves.toBe(true);
      failures[1](failure);
      await expect(newer).rejects.toBe(failure);

      // The second draft was never saved; the first one was.
      expect(form.isDirty()).toBe(true);
      form.onFieldInput('title', 'Release notes');
      expect(form.isDirty()).toBe(false);
      form.onFieldInput('title', '');
      expect(form.isDirty()).toBe(true);
    });

    it('keeps the newer values as the baseline when an older save resolves after the newer one', async () => {
      const { form, saves } = formWithPendingSaves();
      form.onFieldInput('title', 'Release notes');
      const older = form.submit();
      form.onFieldInput('title', 'Release notes, second draft');
      const newer = form.submit();

      saves[1]();
      await expect(newer).resolves.toBe(true);
      saves[0]();
      await expect(older).resolves.toBe(true);

      expect(form.isDirty()).toBe(false);
      form.onFieldInput('title', 'Release notes');
      expect(form.isDirty()).toBe(true);
    });

    it('moves the baseline to the newer values when the saves resolve in order', async () => {
      const { form, saves } = formWithPendingSaves();
      form.onFieldInput('title', 'Release notes');
      const older = form.submit();
      form.onFieldInput('title', 'Release notes, second draft');
      const newer = form.submit();

      saves[0]();
      await expect(older).resolves.toBe(true);
      // The newer save is still pending: the form holds values not yet saved.
      expect(form.isDirty()).toBe(true);
      saves[1]();
      await expect(newer).resolves.toBe(true);

      expect(form.isDirty()).toBe(false);
      form.onFieldInput('title', 'Release notes');
      expect(form.isDirty()).toBe(true);
    });

    it('stays dirty after a rejected save and hands the rejection to the caller', async () => {
      const failure = new Error('save failed');
      const form = createForm({
        fields: { title: { initial: '' } },
        onSubmit: () => Promise.reject(failure),
      });
      form.onFieldInput('title', 'Release notes');

      await expect(form.submit()).rejects.toBe(failure);

      expect(form.isDirty()).toBe(true);
    });

    it('stays dirty after an onSubmit that throws and hands the error to the caller', async () => {
      const failure = new Error('save refused');
      const form = createForm({
        fields: { title: { initial: '' } },
        onSubmit: () => {
          throw failure;
        },
      });
      form.onFieldInput('title', 'Release notes');

      await expect(form.submit()).rejects.toBe(failure);

      expect(form.isDirty()).toBe(true);
    });

    it('lets the unsaved-changes guard still ask after a rejected save', async () => {
      const form = createForm({
        fields: { title: { initial: '' } },
        onSubmit: () => Promise.reject(new Error('save failed')),
      });
      const ask = vi.fn<() => Promise<boolean>>().mockResolvedValue(false);
      const guard = createUnsavedChangesGuard({ isDirty: form.isDirty, ask });
      form.onFieldInput('title', 'Release notes');

      await expect(form.submit()).rejects.toThrow('save failed');

      await expect(guard.canLeave()).resolves.toBe(false);
      expect(ask).toHaveBeenCalledOnce();
    });
  });

  describe('unsaved-changes guard', () => {
    it('asks through the supplied ask when changed, and stays silent when unchanged', async () => {
      let dirty = false;
      const ask = vi.fn<() => Promise<boolean>>().mockResolvedValue(false);
      const guard = createUnsavedChangesGuard({ isDirty: () => dirty, ask });
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

      // Silent when unchanged
      await expect(guard.canLeave()).resolves.toBe(true);
      expect(ask).not.toHaveBeenCalled();

      const beforeUnloadUnchanged = new Event('beforeunload', { cancelable: true });
      guard.handleBeforeUnload(beforeUnloadUnchanged as BeforeUnloadEvent);
      expect(beforeUnloadUnchanged.defaultPrevented).toBe(false);

      // Asks when changed, and the answer decides
      dirty = true;
      await expect(guard.canLeave()).resolves.toBe(false);
      expect(ask).toHaveBeenCalledOnce();

      ask.mockResolvedValue(true);
      await expect(guard.canLeave()).resolves.toBe(true);

      const beforeUnloadChanged = new Event('beforeunload', { cancelable: true });
      guard.handleBeforeUnload(beforeUnloadChanged as BeforeUnloadEvent);
      expect(beforeUnloadChanged.defaultPrevented).toBe(true);

      // The browser's own confirm is never the question.
      expect(confirmSpy).not.toHaveBeenCalled();
    });

    it('lets exactly the next check pass after a confirmed leave, then asks again', async () => {
      const ask = vi.fn<() => Promise<boolean>>().mockResolvedValue(false);
      const guard = createUnsavedChangesGuard({ isDirty: () => true, ask });

      guard.allowLeave();
      expect(guard.shouldAsk()).toBe(false);
      expect(guard.shouldAsk()).toBe(true);

      guard.allowLeave();
      const leaving = new Event('beforeunload', { cancelable: true });
      guard.handleBeforeUnload(leaving as BeforeUnloadEvent);
      expect(leaving.defaultPrevented).toBe(false);
      await expect(guard.canLeave()).resolves.toBe(false);
      expect(ask).toHaveBeenCalledOnce();
    });

    it('asks again once a confirmed leave is withdrawn unconsumed', () => {
      const guard = createUnsavedChangesGuard({
        isDirty: () => true,
        ask: () => Promise.resolve(true),
      });

      guard.allowLeave();
      guard.endLeave();

      expect(guard.shouldAsk()).toBe(true);
    });

    it('follows the form: silent on a clean form, asking for each later edit', () => {
      const form = createForm({ fields: { title: { initial: '' } } });
      const guard = createUnsavedChangesGuard({
        isDirty: form.isDirty,
        ask: () => Promise.resolve(true),
      });
      expect(guard.shouldAsk()).toBe(false);

      form.onFieldInput('title', 'Release notes');

      expect(guard.shouldAsk()).toBe(true);
    });

    it('asks for an edit typed while the save was pending, once the save has resolved', async () => {
      let resolveSave: () => void = () => {};
      const form = createForm({
        fields: { title: { initial: '' } },
        onSubmit: () =>
          new Promise<void>((resolve) => {
            resolveSave = resolve;
          }),
      });
      const ask = vi.fn<() => Promise<boolean>>().mockResolvedValue(false);
      const guard = createUnsavedChangesGuard({ isDirty: form.isDirty, ask });
      form.onFieldInput('title', 'Release notes');

      const saving = form.submit();
      form.onFieldInput('title', 'Release notes, revised');
      resolveSave();
      await expect(saving).resolves.toBe(true);

      await expect(guard.canLeave()).resolves.toBe(false);
      expect(ask).toHaveBeenCalledOnce();
    });
  });
});

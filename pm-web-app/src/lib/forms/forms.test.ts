import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  handleFormKeyDown,
  handleTextAreaKeyDown,
  handleInlineEditKeyDown,
  createForm,
  createUnsavedChangesGuard,
} from './index';

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

    it('moves focus to the first invalid field on submit', () => {
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

      const submitted = form.submit();
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
  });

  describe('unsaved-changes guard', () => {
    it('prompts before leaving when changed, and stays silent when unchanged', () => {
      let dirty = false;
      const guard = createUnsavedChangesGuard({
        isDirty: () => dirty,
      });

      // Silent when unchanged
      expect(guard.canLeave()).toBe(true);

      const beforeUnloadUnchanged = new Event('beforeunload', { cancelable: true });
      guard.handleBeforeUnload(beforeUnloadUnchanged);
      expect(beforeUnloadUnchanged.defaultPrevented).toBe(false);

      // Asks when changed
      dirty = true;
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
      expect(guard.canLeave()).toBe(false);
      expect(confirmSpy).toHaveBeenCalled();

      confirmSpy.mockReturnValue(true);
      expect(guard.canLeave()).toBe(true);

      const beforeUnloadChanged = new Event('beforeunload', { cancelable: true });
      guard.handleBeforeUnload(beforeUnloadChanged);
      expect(beforeUnloadChanged.defaultPrevented).toBe(true);
    });
  });
});

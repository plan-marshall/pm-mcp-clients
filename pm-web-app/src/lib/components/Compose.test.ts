import { describe, it, expect, vi } from 'vitest';
import path from 'node:path';
import { createRawSnippet, tick } from 'svelte';
import { fireEvent, render, screen } from '@testing-library/svelte';
import Compose, { DEFAULT_MAX_LINES } from './Compose.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `Compose` (components-fields.adoc, Compose): one text area that grows to `maxLines` and then
 * scrolls, and the secondary send act with a visually hidden label. `Enter` sends, `Shift+Enter`
 * adds a line, nothing is sent while a composition is open, and while `stop` is set the act stops
 * the answer with the field still enabled and focused.
 */

const STYLE = styleOf(path.resolve(__dirname, 'Compose.svelte'));

/** A stand-in for the frame's acts before the send act. */
const TOOLS = createRawSnippet(() => ({ render: () => '<span data-testid="compose-tools">mic</span>' }));

/** A stand-in for the frame's line under the field. */
const FOOTER = createRawSnippet(() => ({ render: () => '<span data-testid="compose-footer">0 / 4000</span>' }));

/**
 * @param props the props beyond the send label and the send callback
 * @returns the rendered field, its act and the send callback
 */
function renderCompose(props: Record<string, unknown> = {}) {
  const onsend = vi.fn();
  const result = render(Compose, { props: { ariaLabel: 'Message', sendLabel: 'Send', onsend, ...props } });
  const field = screen.getByRole('textbox', { name: 'Message' }) as HTMLTextAreaElement;
  return { ...result, field, onsend };
}

/** @param count the number of lines @returns a text of that many lines */
function linesOf(count: number): string {
  return Array.from({ length: count }, (_, index) => `line ${index + 1}`).join('\n');
}

describe('Compose', () => {
  describe('sending', () => {
    it('sends the text on Enter and keeps Enter from adding a line', async () => {
      const { field, onsend } = renderCompose({ value: 'Start the review' });

      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter' });

      expect(onsend).toHaveBeenCalledExactlyOnceWith('Start the review');
      expect(notPrevented).toBe(false);
    });

    it('adds a line on Shift+Enter and sends nothing', async () => {
      const { field, onsend } = renderCompose({ value: 'First line' });

      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter', shiftKey: true });

      expect(onsend).not.toHaveBeenCalled();
      expect(notPrevented).toBe(true);
    });

    it('sends nothing on an Enter that confirms a composition', async () => {
      const { field, onsend } = renderCompose({ value: 'にほんご' });

      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter', isComposing: true });

      expect(onsend).not.toHaveBeenCalled();
      expect(notPrevented).toBe(true);
    });

    it('sends nothing while a composition is open, and sends again on an ordinary Enter once it has ended', async () => {
      const { field, onsend } = renderCompose({ value: 'Diktat' });

      await fireEvent.compositionStart(field);
      await fireEvent.keyDown(field, { key: 'Enter', keyCode: 13 });
      expect(onsend).not.toHaveBeenCalled();

      await fireEvent.compositionEnd(field);
      await fireEvent.keyDown(field, { key: 'Enter', keyCode: 13, isComposing: false });
      expect(onsend).toHaveBeenCalledExactlyOnceWith('Diktat');
    });

    it('sends nothing on the Enter Safari delivers after the composition ended, marked by keyCode 229', async () => {
      const { field, onsend } = renderCompose({ value: 'にほんご' });

      await fireEvent.compositionStart(field);
      await fireEvent.compositionEnd(field);
      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter', keyCode: 229, isComposing: false });

      expect(onsend).not.toHaveBeenCalled();
      expect(notPrevented).toBe(true);
    });

    it('sends the text once for a held Enter, and adds no line for its repeats', async () => {
      const { field, onsend } = renderCompose({ value: 'Held key' });

      await fireEvent.keyDown(field, { key: 'Enter', repeat: false });
      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter', repeat: true });
      await fireEvent.keyDown(field, { key: 'Enter', repeat: true });

      expect(onsend).toHaveBeenCalledExactlyOnceWith('Held key');
      expect(notPrevented).toBe(false);
    });

    it('still adds a line for a held Shift+Enter', async () => {
      const { field, onsend } = renderCompose({ value: 'Lines' });

      const notPrevented = await fireEvent.keyDown(field, { key: 'Enter', shiftKey: true, repeat: true });

      expect(onsend).not.toHaveBeenCalled();
      expect(notPrevented).toBe(true);
    });

    it('sends the typed text through the send act', async () => {
      const { field, onsend } = renderCompose();

      await fireEvent.input(field, { target: { value: 'Typed text' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Send' }));

      expect(onsend).toHaveBeenCalledExactlyOnceWith('Typed text');
    });

    it('sends nothing while the act is held back', async () => {
      const { field, onsend } = renderCompose({ value: 'Held', disabled: true, disabledReason: 'Type a message.' });

      await fireEvent.keyDown(field, { key: 'Enter' });
      await fireEvent.click(screen.getByRole('button', { name: 'Send' }));

      expect(onsend).not.toHaveBeenCalled();
    });
  });

  describe('the send act', () => {
    it('is the secondary Button with the send icon and a visually hidden label', () => {
      renderCompose();
      const act = screen.getByRole('button', { name: 'Send' });
      const label = act.querySelector('.sr-only') as HTMLElement;

      expect(act).toHaveClass('btn--secondary');
      expect(act.querySelector('[data-icon="act.send"]')).not.toBeNull();
      expect(label).toHaveTextContent('Send');
    });

    it('carries the reason it is held back as its description, with the field still editable', () => {
      const { field } = renderCompose({ disabled: true, disabledReason: 'Type a message.' });
      const act = screen.getByRole('button', { name: 'Send' });

      expect(act).toBeDisabled();
      expect(act).toHaveAccessibleDescription('Type a message.');
      expect(field).toBeEnabled();
    });

    it('carries the same reason as the description of the field, so a keyboard user hears it there', () => {
      const { field } = renderCompose({ disabled: true, disabledReason: 'Type a message.' });

      expect(field).toHaveAccessibleDescription('Type a message.');
    });

    it('describes the field only while sending is held back', async () => {
      const { field, rerender } = renderCompose({ disabled: true, disabledReason: 'Type a message.' });
      expect(field).toHaveAttribute('aria-describedby');

      await rerender({ disabled: false });
      expect(field).not.toHaveAttribute('aria-describedby');
      expect(field).toHaveAccessibleDescription('');

      await rerender({ disabled: true, disabledReason: '' });
      expect(field).not.toHaveAttribute('aria-describedby');
    });
  });

  describe('the stop act', () => {
    it('replaces the send act while an answer runs, with the field enabled and focused', async () => {
      const onstop = vi.fn();
      const { field, rerender, onsend } = renderCompose({ value: 'Question' });
      field.focus();

      await rerender({ stop: { label: 'Stop the answer', onstop } });
      const act = screen.getByRole('button', { name: 'Stop the answer' });

      expect(screen.queryByRole('button', { name: 'Send' })).toBeNull();
      expect(act.querySelector('[data-icon="act.stop"]')).not.toBeNull();
      expect(field).toBeEnabled();
      expect(field).toHaveFocus();

      // A pointer press focuses the act, as a browser does; the act hands the focus back to the field.
      act.focus();
      await fireEvent.click(act);

      expect(onstop).toHaveBeenCalledOnce();
      expect(onsend).not.toHaveBeenCalled();
      expect(field).toHaveFocus();
    });

    it('hands the focus back to the field even where the stop callback throws', async () => {
      const failure = new Error('stop failed');
      const onstop = vi.fn(() => {
        throw failure;
      });
      const { field } = renderCompose({ value: 'Question', stop: { label: 'Stop the answer', onstop } });
      const act = screen.getByRole('button', { name: 'Stop the answer' });
      // jsdom reports an error of an event handler on the window; a listener keeps it from failing the run.
      const reported: unknown[] = [];
      const collect = (event: ErrorEvent): void => {
        event.preventDefault();
        reported.push(event.error);
      };
      window.addEventListener('error', collect);

      act.focus();
      await fireEvent.click(act);
      window.removeEventListener('error', collect);

      expect(onstop).toHaveBeenCalledOnce();
      expect(reported).toContain(failure);
      expect(field).toHaveFocus();
    });

    it('does not describe the field with the reason of a send act that is not shown', () => {
      const { field } = renderCompose({
        disabled: true,
        disabledReason: 'Type a message.',
        stop: { label: 'Stop', onstop: vi.fn() },
      });

      expect(field).not.toHaveAttribute('aria-describedby');
    });

    it('stays active even where sending is held back', () => {
      renderCompose({ disabled: true, disabledReason: 'Type a message.', stop: { label: 'Stop', onstop: vi.fn() } });

      expect(screen.getByRole('button', { name: 'Stop' })).toBeEnabled();
    });

    it('sends nothing on Enter while an answer runs', async () => {
      const { field, onsend } = renderCompose({ value: 'Next question', stop: { label: 'Stop', onstop: vi.fn() } });

      await fireEvent.keyDown(field, { key: 'Enter' });

      expect(onsend).not.toHaveBeenCalled();
    });
  });

  describe('the height of the field', () => {
    it('grows with its lines', async () => {
      const { field } = renderCompose({ value: linesOf(3) });
      await tick();

      expect(field.style.getPropertyValue('--compose-lines')).toBe('3');
      expect(field).not.toHaveClass('compose__field--scrolls');
    });

    it('stops at maxLines and then scrolls', async () => {
      const { field } = renderCompose({ value: linesOf(6), maxLines: 4 });
      await tick();

      expect(field.style.getPropertyValue('--compose-lines')).toBe('4');
      expect(field).toHaveClass('compose__field--scrolls');
    });

    it('grows to eight lines unless the caller names another limit', async () => {
      const { field } = renderCompose({ value: linesOf(12) });
      await tick();

      expect(DEFAULT_MAX_LINES).toBe(8);
      expect(field.style.getPropertyValue('--compose-lines')).toBe('8');
    });

    it('shrinks again when the text is cleared', async () => {
      const { field } = renderCompose({ value: linesOf(5) });
      await tick();

      await fireEvent.input(field, { target: { value: '' } });

      expect(field.style.getPropertyValue('--compose-lines')).toBe('1');
      expect(field).not.toHaveClass('compose__field--scrolls');
    });

    it('takes its height from the line count and hides the scroll bar until it overflows', () => {
      expect(ruleOf(STYLE, '.compose__field')).toContain(
        'block-size: calc(var(--compose-lines, 1) * var(--line-height-base))',
      );
      expect(ruleOf(STYLE, '.compose__field')).toContain('overflow-y: hidden');
      expect(ruleOf(STYLE, '.compose__field--scrolls')).toContain('overflow-y: auto');
    });
  });

  describe('labels and snippets', () => {
    it('names the field with its visible label where one is given', () => {
      render(Compose, { props: { label: 'Instruction', sendLabel: 'Send', onsend: vi.fn() } });

      expect(screen.getByRole('textbox', { name: 'Instruction' })).not.toHaveAttribute('aria-label');
    });

    it.each([
      ['a label alone', { label: 'Instruction' }, 'Instruction', false],
      ['an aria label alone', { ariaLabel: 'Message' }, 'Message', true],
      ['both, where the label wins', { label: 'Instruction', ariaLabel: 'Message' }, 'Instruction', false],
      ['an empty label and an aria label', { label: '', ariaLabel: 'Message' }, 'Message', true],
    ] as const)('gives the field an accessible name from %s', (_case, name, accessibleName, viaAriaLabel) => {
      render(Compose, { props: { ...name, sendLabel: 'Send', onsend: vi.fn() } });
      const field = screen.getByRole('textbox', { name: accessibleName });

      expect(field).toHaveAccessibleName(accessibleName);
      expect(field.hasAttribute('aria-label')).toBe(viaAriaLabel);
    });

    it('renders the tools before the send act and the footer under the field', () => {
      const { field } = renderCompose({ tools: TOOLS, footer: FOOTER });
      const tools = screen.getByTestId('compose-tools');
      const footer = screen.getByTestId('compose-footer');
      const act = screen.getByRole('button', { name: 'Send' });

      expect(tools.compareDocumentPosition(act) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
      expect(field.compareDocumentPosition(footer) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('renders no tools and no footer where the frame brings none', () => {
      const { container } = renderCompose();

      expect(container.querySelector('.compose__tools')).toBeNull();
      expect(container.querySelector('.compose__footer')).toBeNull();
    });
  });

  it('moves the focus into the field on request', () => {
    const { component, field } = renderCompose();

    (component as unknown as { focus: () => void }).focus();

    expect(field).toHaveFocus();
  });
});

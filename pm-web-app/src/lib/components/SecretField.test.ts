import path from 'node:path';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { tick } from 'svelte';
import SecretField from './SecretField.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';
import {
  REMOVED_OUTLINE,
  TRANSPARENT_OUTLINE,
  sourceOf,
  uniqueIdShape,
} from '../../test/fieldContract';

describe('SecretField', () => {
  it('masks secret by default and sets autocomplete/spellcheck/autocapitalize off', () => {
    const { container } = render(SecretField, {
      props: { label: 'API Token', id: 'token-input' },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    expect(input).toBeInTheDocument();
    expect(input.type).toBe('password');
    expect(input).toHaveAttribute('autocomplete', 'off');
    expect(input).toHaveAttribute('spellcheck', 'false');
    expect(input).toHaveAttribute('autocapitalize', 'off');
  });

  it('reveals secret only while reveal toggle is pressed', async () => {
    const { container } = render(SecretField, {
      props: { label: 'Secret key' },
    });
    const input = container.querySelector('input') as HTMLInputElement;
    const toggle = container.querySelector('.toggle') as HTMLButtonElement;

    expect(toggle).toBeInTheDocument();
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(input.type).toBe('password');

    // Press toggle
    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(input.type).toBe('text');

    // Press toggle again
    await fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    expect(input.type).toBe('password');
  });

  it('trims whitespace on paste', async () => {
    const { container } = render(SecretField, {
      props: { label: 'Secret' },
    });
    const input = container.querySelector('input') as HTMLInputElement;

    const pasteData = {
      getData: (format: string) => (format === 'text' || format === 'text/plain' ? '   my-secret-key   \n' : ''),
    };
    await fireEvent.paste(input, { clipboardData: pasteData });
    expect(input.value).toBe('my-secret-key');
  });

  describe('paste at the selection', () => {
    const clipboard = (text: string) => ({
      getData: (format: string) => (format === 'text' || format === 'text/plain' ? text : ''),
    });

    it('inserts the trimmed paste at the caret, places the caret after it and reports one input', async () => {
      const oninput = vi.fn();
      const { container } = render(SecretField, { props: { label: 'Secret', oninput } });
      const input = container.querySelector('input') as HTMLInputElement;
      await fireEvent.input(input, { target: { value: 'abcd' } });
      oninput.mockClear();
      input.setSelectionRange(2, 2);

      await fireEvent.paste(input, { clipboardData: clipboard('  XY \n') });

      expect(input.value).toBe('abXYcd');
      expect(input.selectionStart).toBe(4);
      expect(input.selectionEnd).toBe(4);
      expect(oninput).toHaveBeenCalledOnce();
    });

    it('replaces a selection in the middle with the paste and reports one input', async () => {
      const oninput = vi.fn();
      const { container } = render(SecretField, { props: { label: 'Secret', oninput } });
      const input = container.querySelector('input') as HTMLInputElement;
      await fireEvent.input(input, { target: { value: 'abcd' } });
      oninput.mockClear();
      input.setSelectionRange(1, 3);

      await fireEvent.paste(input, { clipboardData: clipboard('Z') });

      expect(input.value).toBe('aZd');
      expect(input.selectionStart).toBe(2);
      expect(oninput).toHaveBeenCalledOnce();
    });

    it.each([
      ['a clipboard without text (an image)', clipboard(''), true],
      ['a paste event without clipboard data', undefined, true],
      ['a clipboard of whitespace only', clipboard('  \t\n '), false],
    ])(
      'keeps the selected secret and its selection unchanged for %s',
      async (_case, data, leftToBrowser) => {
        const oninput = vi.fn();
        const { container } = render(SecretField, { props: { label: 'Secret', oninput } });
        const input = container.querySelector('input') as HTMLInputElement;
        await fireEvent.input(input, { target: { value: 'abcd' } });
        oninput.mockClear();
        input.setSelectionRange(1, 3);

        const notCancelled = await fireEvent.paste(
          input,
          data === undefined ? {} : { clipboardData: data },
        );

        expect(notCancelled).toBe(leftToBrowser);
        expect(input.value).toBe('abcd');
        expect(input.selectionStart).toBe(1);
        expect(input.selectionEnd).toBe(3);
        expect(oninput).not.toHaveBeenCalled();
      },
    );

    it('cancels the browser paste when it inserts text itself', async () => {
      const { container } = render(SecretField, { props: { label: 'Secret' } });
      const input = container.querySelector('input') as HTMLInputElement;

      const notCancelled = await fireEvent.paste(input, { clipboardData: clipboard('tok') });

      expect(notCancelled).toBe(false);
      expect(input.value).toBe('tok');
    });
  });

  describe('storing the secret', () => {
    it('clears the secret on store and no input of the field holds it afterwards', async () => {
      const secret = `tok-${crypto.randomUUID()}`;
      const onstore = vi.fn();
      const { container } = render(SecretField, {
        props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
      });
      const input = container.querySelector('input') as HTMLInputElement;
      const storeBtn = screen.getByRole('button', { name: 'Save Secret' });

      // Initially store button is disabled when empty
      expect(storeBtn).toBeDisabled();

      await fireEvent.input(input, { target: { value: secret } });
      expect(input.value).toBe(secret);
      expect(storeBtn).not.toBeDisabled();

      await fireEvent.click(container.querySelector('.toggle') as HTMLButtonElement);
      await fireEvent.click(storeBtn);
      expect(onstore).toHaveBeenCalledWith(secret);

      expect(input.type).toBe('password');
      expect(input.value).toBe('');
      const holders = [...container.querySelectorAll('input')].filter(
        (el) => el.value === secret,
      );
      expect(holders).toEqual([]);
    });

    it('masks the field again after a store made while revealed', async () => {
      const onstore = vi.fn().mockResolvedValue(undefined);
      const { container } = render(SecretField, {
        props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
      });
      const input = container.querySelector('input') as HTMLInputElement;
      const toggle = container.querySelector('.toggle') as HTMLButtonElement;

      await fireEvent.input(input, { target: { value: 'tok-123' } });
      await fireEvent.click(toggle);
      expect(input.type).toBe('text');

      await fireEvent.click(screen.getByRole('button', { name: 'Save Secret' }));

      await vi.waitFor(() => expect(onstore).toHaveBeenCalledOnce());
      expect(input.type).toBe('password');
      expect(toggle).toHaveAttribute('aria-pressed', 'false');
    });

    it('puts the secret back, still masked, when the store is rejected, and leaves no unhandled rejection', async () => {
      const unhandled = vi.fn();
      process.on('unhandledRejection', unhandled);
      try {
        const onstore = vi.fn().mockRejectedValue(new Error('store refused'));
        const { container } = render(SecretField, {
          props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
        });
        const input = container.querySelector('input') as HTMLInputElement;

        await fireEvent.input(input, { target: { value: 'tok-456' } });
        await fireEvent.click(container.querySelector('.toggle') as HTMLButtonElement);
        await fireEvent.click(screen.getByRole('button', { name: 'Save Secret' }));

        await vi.waitFor(() => expect(input.value).toBe('tok-456'));
        expect(input.type).toBe('password');
        expect(onstore).toHaveBeenCalledOnce();
        expect(unhandled).not.toHaveBeenCalled();
        // The person is told why the secret came back, not left to guess at the restored field.
        expect(screen.getByText('The secret was not stored. Try again.')).toBeInTheDocument();
      } finally {
        process.off('unhandledRejection', unhandled);
      }
    });

    it('keeps what the operator typed while the store was pending when the store is then rejected', async () => {
      let rejectStore: (reason: Error) => void = () => {};
      const storing = new Promise<void>((_resolve, reject) => {
        rejectStore = reject;
      });
      const onstore = vi.fn(() => storing);
      const { container } = render(SecretField, {
        props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
      });
      const input = container.querySelector('input') as HTMLInputElement;

      await fireEvent.input(input, { target: { value: 'tok-old' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Save Secret' }));
      expect(onstore).toHaveBeenCalledExactlyOnceWith('tok-old');
      expect(input.value).toBe('');

      await fireEvent.input(input, { target: { value: 'tok-new' } });
      rejectStore(new Error('store refused'));
      // The field awaited the store first, so its rejection branch has run once this settles.
      await storing.catch(() => undefined);
      await tick();

      expect(input.value).toBe('tok-new');
      expect(input.type).toBe('password');
    });

    it('does not restore an older rejected secret after a newer store succeeded', async () => {
      let rejectFirst: (reason: Error) => void = () => {};
      let resolveSecond: () => void = () => {};
      const first = new Promise<void>((_resolve, reject) => {
        rejectFirst = reject;
      });
      const second = new Promise<void>((resolve) => {
        resolveSecond = resolve;
      });
      const onstore = vi.fn().mockReturnValueOnce(first).mockReturnValueOnce(second);
      const { container } = render(SecretField, {
        props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
      });
      const input = container.querySelector('input') as HTMLInputElement;
      const storeBtn = screen.getByRole('button', { name: 'Save Secret' });

      await fireEvent.input(input, { target: { value: 'tok-a' } });
      await fireEvent.click(storeBtn);
      await fireEvent.input(input, { target: { value: 'tok-b' } });
      await fireEvent.click(storeBtn);
      expect(onstore).toHaveBeenNthCalledWith(1, 'tok-a');
      expect(onstore).toHaveBeenNthCalledWith(2, 'tok-b');

      resolveSecond();
      await second;
      await tick();
      rejectFirst(new Error('store refused'));
      // The field awaited the first store, so its rejection branch has run once this settles.
      await first.catch(() => undefined);
      await tick();

      expect(input.value).toBe('');
    });

    it('restores a rejected secret masked when the reveal toggle was pressed while the store was pending', async () => {
      let rejectStore: (reason: Error) => void = () => {};
      const storing = new Promise<void>((_resolve, reject) => {
        rejectStore = reject;
      });
      const onstore = vi.fn(() => storing);
      const { container } = render(SecretField, {
        props: { label: 'New Secret', onstore, storeLabel: 'Save Secret' },
      });
      const input = container.querySelector('input') as HTMLInputElement;
      const toggle = container.querySelector('.toggle') as HTMLButtonElement;

      await fireEvent.input(input, { target: { value: 'tok-789' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Save Secret' }));
      await fireEvent.click(toggle);
      expect(input.type).toBe('text');

      rejectStore(new Error('store refused'));
      await storing.catch(() => undefined);
      await tick();

      expect(input.value).toBe('tok-789');
      expect(input.type).toBe('password');
      expect(toggle).toHaveAttribute('aria-pressed', 'false');
    });
  });

  describe('focus in forced-colors mode and generated ids', () => {
    const file = path.resolve(__dirname, './SecretField.svelte');

    it('uses --control-height and keeps a transparent focus outline beside the focus ring', () => {
      const style = styleOf(file);
      expect(style).toContain('var(--control-height)');
      expect(style).not.toMatch(REMOVED_OUTLINE);
      const focus = ruleOf(style, '.secret-field__input:focus');
      expect(focus).toMatch(TRANSPARENT_OUTLINE);
      expect(focus).toContain('box-shadow: var(--focus-ring)');
    });

    it('gives two instances without an id distinct ids of the uniqueId shape', () => {
      const idOf = (label: string) =>
        render(SecretField, { props: { label } }).container.querySelector('.secret-field__input')
          ?.id;
      const first = idOf('First');
      const second = idOf('Second');
      expect(first).toMatch(uniqueIdShape('sf'));
      expect(second).toMatch(uniqueIdShape('sf'));
      expect(first).not.toBe(second);
    });

    it('allocates its id without Math.random', () => {
      expect(sourceOf(file)).not.toContain('Math.random');
    });
  });
});

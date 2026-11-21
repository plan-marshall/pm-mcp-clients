import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, fireEvent, screen } from '@testing-library/svelte';
import DemoFormFixture from './DemoFormFixture.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

afterEach(() => {
  currentLocale.set('en');
  messagesStore.set(loadMessages('en'));
});

// The fixture's unsaved-changes guard registers with SvelteKit's router, which a unit test has not.
vi.mock('$app/navigation', () => ({ beforeNavigate: () => {}, goto: () => Promise.resolve() }));

describe('DemoFormFixture keyboard walk and validation (Done-when 4.3)', () => {
  it('renders form with initial invalid state and disabled submit button', () => {
    render(DemoFormFixture);

    const submitBtn = screen.getByRole('button', { name: /save/i });
    expect(submitBtn.getAttribute('aria-disabled')).toBe('true');
    expect(screen.getByRole('status')).toHaveTextContent(/reason/i);
  });

  it('moves focus in document order from text field to textarea to buttons', () => {
    render(DemoFormFixture);

    const order = [
      screen.getByTestId('demo-form-text'),
      screen.getByTestId('demo-form-textarea'),
      screen.getByRole('button', { name: /cancel/i }),
      screen.getByRole('button', { name: /save/i }),
    ];

    // Tab follows the document where no control carries a positive tabindex.
    expect(order.map((control) => control.tabIndex > 0)).toEqual([false, false, false, false]);
    for (const [index, control] of order.slice(0, -1).entries()) {
      expect(
        control.compareDocumentPosition(order[index + 1]) & Node.DOCUMENT_POSITION_FOLLOWING,
        `control ${index + 1} follows control ${index}`,
      ).toBeTruthy();
    }
  });

  it.each([
    ['en', 'Fill in this field.'],
    ['de', 'Fülle dieses Feld aus.'],
  ] as const)(
    'checks the title on blur with the catalogue reason and clears it as one types, in %s',
    async (locale, reason) => {
      currentLocale.set(locale);
      messagesStore.set(loadMessages(locale));
      render(DemoFormFixture);
      const title = screen.getByTestId('demo-form-text') as HTMLInputElement;

      await fireEvent.blur(title);

      expect(title).toHaveAttribute('aria-invalid', 'true');
      expect(title.closest('.field')).toHaveTextContent(reason);

      await fireEvent.input(title, { target: { value: 'Release notes' } });

      expect(title).not.toHaveAttribute('aria-invalid');
      expect(title.closest('.field')).not.toHaveTextContent(reason);
    },
  );

  it('submits on Enter in TextField when valid', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    const textArea = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;

    await fireEvent.input(textInput, { target: { value: 'Feature title' } });
    await fireEvent.input(textArea, { target: { value: 'Valid reason' } });

    const submitBtn = screen.getByRole('button', { name: /save/i });
    expect(submitBtn.getAttribute('aria-disabled')).toBeNull();

    // Press Enter in TextField
    await fireEvent.keyDown(textInput, { key: 'Enter' });
    expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
  });

  it('inserts newline on Enter in TextArea and submits on Ctrl+Enter when valid', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    const textArea = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;

    await fireEvent.input(textInput, { target: { value: 'Title' } });
    await fireEvent.input(textArea, { target: { value: 'Line 1' } });

    // Enter in TextArea does not submit
    await fireEvent.keyDown(textArea, { key: 'Enter' });
    expect(screen.queryByText(/submitted/i)).toBeNull();

    // Ctrl+Enter in TextArea submits
    await fireEvent.keyDown(textArea, { key: 'Enter', ctrlKey: true });
    expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
  });

  it('cancels on Escape', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    await fireEvent.input(textInput, { target: { value: 'Some text' } });

    // Press Escape
    await fireEvent.keyDown(textInput, { key: 'Escape' });
    expect(screen.getByRole('status')).toHaveTextContent(/cancelled/i);
  });
});

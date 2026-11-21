import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, fireEvent, screen } from '@testing-library/svelte';
import { tick } from 'svelte';
import type * as AppNavigation from '$app/navigation';
import DemoFormFixture from './DemoFormFixture.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

afterEach(() => {
  currentLocale.set('en');
  messagesStore.set(loadMessages('en'));
});

/** The navigation SvelteKit hands a `beforeNavigate` callback. */
type BeforeNavigate = Parameters<Parameters<typeof AppNavigation.beforeNavigate>[0]>[0];

// The fixture's unsaved-changes guard registers with SvelteKit's router, which a unit test has not:
// the test holds the callback it registers and plays a route change through it.
const router = vi.hoisted(() => ({ callbacks: [] as Array<(nav: BeforeNavigate) => void> }));
vi.mock('$app/navigation', () => ({
  beforeNavigate: (callback: (nav: BeforeNavigate) => void) => {
    router.callbacks.push(callback);
  },
  goto: () => Promise.resolve(),
}));

/** @returns the route change to another page, played through the guard, with its cancel spied */
function changeRoute(): BeforeNavigate {
  const nav = {
    type: 'link',
    from: null,
    to: { url: new URL('http://localhost/projects'), params: {}, route: { id: null } },
    willUnload: false,
    cancel: vi.fn(),
    complete: Promise.resolve(),
  } as unknown as BeforeNavigate;
  router.callbacks.at(-1)?.(nav);
  return nav;
}

describe('DemoFormFixture keyboard walk and validation (Done-when 4.3)', () => {
  it('renders form with initial invalid state and disabled submit button', () => {
    render(DemoFormFixture);

    const submitBtn = screen.getByRole('button', { name: /save/i });
    expect(submitBtn.getAttribute('aria-disabled')).toBe('true');
    expect(screen.getByRole('status')).toHaveTextContent(/reason/i);
  });

  it('moves focus in document order from text field to textarea to buttons', async () => {
    render(DemoFormFixture);
    // Both fields are filled, so Save is enabled too and every control can take the focus.
    await fireEvent.input(screen.getByTestId('demo-form-text'), {
      target: { value: 'Release notes' },
    });
    await fireEvent.input(screen.getByTestId('demo-form-textarea'), {
      target: { value: 'Ship it' },
    });

    const order = [
      screen.getByTestId('demo-form-text'),
      screen.getByTestId('demo-form-textarea'),
      screen.getByRole('button', { name: /cancel/i }),
      screen.getByRole('button', { name: /save/i }),
    ];

    // Tab reaches a control only when it is enabled and its tabindex is 0 (not -1, not positive);
    // the document order then is the Tab order.
    for (const control of order) {
      expect(control).toBeEnabled();
      expect(control.tabIndex).toBe(0);
    }
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

  it('shows the title error in the language the operator switches to while it is shown', async () => {
    render(DemoFormFixture);
    const title = screen.getByTestId('demo-form-text') as HTMLInputElement;
    await fireEvent.blur(title);
    expect(title.closest('.field')).toHaveTextContent('Fill in this field.');

    currentLocale.set('de');
    messagesStore.set(loadMessages('de'));
    await tick();

    expect(title.closest('.field')).toHaveTextContent('Fülle dieses Feld aus.');
    expect(title.closest('.field')).not.toHaveTextContent('Fill in this field.');
  });

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

  describe('unsaved-changes guard', () => {
    /** Fills both fields and returns them. */
    async function fill(): Promise<{ title: HTMLInputElement; reason: HTMLTextAreaElement }> {
      const title = screen.getByTestId('demo-form-text') as HTMLInputElement;
      const reason = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;
      await fireEvent.input(title, { target: { value: 'Release notes' } });
      await fireEvent.input(reason, { target: { value: 'Ship it' } });
      return { title, reason };
    }

    it('lets a route change through on a clean form, and asks once it is edited', async () => {
      render(DemoFormFixture);
      expect(changeRoute().cancel).not.toHaveBeenCalled();

      await fill();

      expect(changeRoute().cancel).toHaveBeenCalledOnce();
      expect(await screen.findByRole('dialog')).toBeInTheDocument();
    });

    it('lets a route change through once the save has succeeded', async () => {
      render(DemoFormFixture);
      const { title } = await fill();

      await fireEvent.keyDown(title, { key: 'Enter' });

      expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
      expect(changeRoute().cancel).not.toHaveBeenCalled();
    });

    it.each([
      ['title', 'demo-form-text'],
      ['reason', 'demo-form-textarea'],
    ] as const)(
      'asks on a route change for the %s typed while the save was pending',
      async (_field, testid) => {
        render(DemoFormFixture);
        const { title } = await fill();

        // The save is pending until the next microtask; the operator types on before it resolves.
        const saving = fireEvent.keyDown(title, { key: 'Enter' });
        await fireEvent.input(screen.getByTestId(testid), { target: { value: 'Second draft' } });
        await saving;

        expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
        expect(changeRoute().cancel).toHaveBeenCalledOnce();
        expect(await screen.findByRole('dialog')).toBeInTheDocument();
      },
    );

    it('keeps the reason of the newer save when two saves overlap', async () => {
      render(DemoFormFixture);
      const { title, reason } = await fill();

      // The first save is still pending when the operator edits the reason and saves again: the
      // events are dispatched at once and awaited only afterwards.
      const older = fireEvent.keyDown(title, { key: 'Enter' });
      const edit = fireEvent.input(reason, { target: { value: 'Second draft' } });
      const newer = fireEvent.keyDown(title, { key: 'Enter' });
      await Promise.all([older, edit, newer]);

      expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
      expect(changeRoute().cancel).not.toHaveBeenCalled();
      await fireEvent.input(reason, { target: { value: 'Ship it' } });
      expect(changeRoute().cancel).toHaveBeenCalledOnce();
    });
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

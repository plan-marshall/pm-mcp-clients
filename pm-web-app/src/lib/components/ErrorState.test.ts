import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import ErrorState from './ErrorState.svelte';

/**
 * `ErrorState` (VD-P-05, VD-S-04, UX-S-06): the title, the one sentence and the one act of the
 * error a code stands for, with the cause and the technical code folded under `Details`.
 *
 * The expected texts are the contract of the shared catalogue (`error.*`, `common.*`), so they stay
 * literal.
 */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('ErrorState', () => {
  it('shows the title and the fix of a known code, with the cause under Details, in English', () => {
    useCatalogue('en');
    const { container } = render(ErrorState, { props: { code: 'scope_not_found' } });
    const details = container.querySelector('details');

    expect(screen.getByTestId('error-state')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'This entry does not exist' })).toBeInTheDocument();
    expect(
      screen.getByText('Check the id or pick the entry from the list again.'),
    ).toBeInTheDocument();
    expect(details).not.toBeNull();
    expect(details).not.toHaveAttribute('open');
    expect(details?.querySelector('summary')?.textContent?.trim()).toBe('Details');
    expect(details?.textContent).toContain(
      'No plan, epic, decision, report or entry carries this id.',
    );
    expect(details?.querySelector('code')?.textContent).toBe('scope_not_found');
  });

  it('shows the same three texts in German', () => {
    useCatalogue('de');
    const { container } = render(ErrorState, { props: { code: 'scope_not_found' } });

    expect(
      screen.getByRole('heading', { name: 'Diesen Eintrag gibt es nicht' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Prüfe die Kennung oder wähle den Eintrag erneut aus der Liste.'),
    ).toBeInTheDocument();
    expect(container.querySelector('details')?.textContent).toContain(
      'Kein Plan, kein Epic, keine Entscheidung, kein Bericht und kein Eintrag trägt diese Kennung.',
    );
    expect(container.querySelector('details')?.querySelector('summary')?.textContent?.trim()).toBe(
      'Details',
    );
  });

  it('shows a client code through the client area of the catalogue', () => {
    useCatalogue('en');
    const { container } = render(ErrorState, { props: { code: 'timeout' } });

    expect(screen.getByRole('heading', { name: 'The request took too long' })).toBeInTheDocument();
    expect(container.querySelector('details')?.textContent).toContain('timeout');
  });

  it('falls back to the unknown texts and still shows the code under Details', () => {
    useCatalogue('en');
    const { container } = render(ErrorState, { props: { code: 'hyperdrive_broken' } });

    expect(
      screen.getByRole('heading', { name: 'The runtime refused the request' }),
    ).toBeInTheDocument();
    expect(container.querySelector('details')?.textContent).toContain(
      'The refusal carries a code this client does not know.',
    );
    expect(container.querySelector('details')?.querySelector('code')?.textContent).toBe(
      'hyperdrive_broken',
    );
  });

  it('draws its act and reports the click', () => {
    useCatalogue('en');
    const onAct = vi.fn();
    render(ErrorState, { props: { code: 'scope_not_found', actionLabel: 'Try again', onAct } });

    screen.getByRole('button', { name: 'Try again' }).click();

    expect(onAct).toHaveBeenCalledTimes(1);
  });

  it('draws no act when the view names none', () => {
    useCatalogue('en');
    render(ErrorState, { props: { code: 'scope_not_found' } });

    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });

  it("is not announced when it is the page's own first state", () => {
    useCatalogue('en');
    const { container } = render(ErrorState, {
      props: { code: 'scope_not_found', announce: false },
    });

    expect(container.querySelector('[data-testid="error-state"]')).not.toHaveAttribute('role');
  });

  it('draws no code line when the view read no code', () => {
    useCatalogue('en');
    const { container } = render(ErrorState, { props: { code: '' } });

    expect(
      screen.getByRole('heading', { name: 'The runtime refused the request' }),
    ).toBeInTheDocument();
    expect(container.querySelector('details')?.querySelector('code')).toBeNull();
  });
});

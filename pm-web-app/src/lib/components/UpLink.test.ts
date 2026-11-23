import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import UpLink from './UpLink.svelte';
import * as runtime from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('UpLink component (PLAN-144 D2)', () => {
  const address = { route: 'epic' as const, epicId: 'relay-hardening' };

  it('renders with parent KindMark and name in English', () => {
    useCatalogue('en');
    render(UpLink, {
      props: {
        kind: 'epic',
        title: 'Relay Hardening',
        address,
      },
    });

    const link = screen.getByTestId('up-link');
    expect(link).toHaveAttribute('aria-label', 'Up to epic Relay Hardening');
    expect(link).toHaveTextContent('Relay Hardening');
    expect(link.querySelector('.kind-mark')).toBeInTheDocument();
  });

  it('renders accessible name in German: Nach oben: {kind} {name}', () => {
    useCatalogue('de');
    render(UpLink, {
      props: {
        kind: 'epic',
        title: 'Relay Hardening',
        address,
      },
    });

    const link = screen.getByTestId('up-link');
    expect(link).toHaveAttribute('aria-label', 'Nach oben: Epic Relay Hardening');
  });

  it('pushes the parent address when clicked', async () => {
    useCatalogue('en');
    const openAddressSpy = vi.spyOn(runtime, 'openAddress').mockImplementation(() => {});

    render(UpLink, {
      props: {
        kind: 'plan',
        title: 'Fix Relay Transport',
        address: { route: 'plan', planId: 'fix-relay' },
      },
    });

    const link = screen.getByTestId('up-link');
    await fireEvent.click(link);

    expect(openAddressSpy).toHaveBeenCalledWith({ route: 'plan', planId: 'fix-relay' });
  });
});

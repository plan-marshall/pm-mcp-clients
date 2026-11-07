import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import NavRail from './NavRail.svelte';
import { runtimeStore } from '../stores/runtime';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('NavRail component', () => {
  beforeEach(() => {
    runtimeStore.update((state) => ({
      ...state,
      isPaired: true,
      isOnboarded: true,
      isInitialized: true,
      currentRoute: 'projects',
    }));
  });

  it('renders Settings in English and Einstellungen in German for the machine settings rail button', () => {
    useCatalogue('en');
    const { unmount } = render(NavRail, {
      props: {
        collapsed: false,
        onToggle: () => {},
        themeChoice: 'system',
        onChooseTheme: () => {},
      },
    });

    expect(screen.getByRole('button', { name: 'Settings' })).toBeInTheDocument();
    unmount();

    useCatalogue('de');
    render(NavRail, {
      props: {
        collapsed: false,
        onToggle: () => {},
        themeChoice: 'system',
        onChooseTheme: () => {},
      },
    });

    expect(screen.getByRole('button', { name: 'Einstellungen' })).toBeInTheDocument();
  });
});

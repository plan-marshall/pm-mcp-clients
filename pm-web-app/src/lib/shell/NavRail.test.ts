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

  describe('the head', () => {
    /**
     * @param collapsed whether the rail shows icons only
     */
    function renderRail(collapsed: boolean): void {
      render(NavRail, {
        props: { collapsed, onToggle: () => {}, themeChoice: 'system', onChooseTheme: () => {} },
      });
    }

    /** Leaves the browser unpaired, so the head draws the inert brand. */
    function unpair(): void {
      runtimeStore.update((state) => ({ ...state, isPaired: false }));
    }

    it.each([
      ['brand-home', false],
      ['brand-inert', true],
    ] as const)('draws the 28 px colour mark without a plate in the %s branch', (testId, unpaired) => {
      useCatalogue('en');
      if (unpaired) unpair();
      renderRail(false);

      const brand = screen.getByTestId(testId);
      const marks = brand.querySelectorAll('[data-testid="brand-mark"]');

      expect(marks).toHaveLength(1);
      expect(marks[0]).toHaveAttribute('data-size', '28');
      expect(marks[0]).toHaveAttribute('data-rendering', 'colour');
      expect(marks[0]).toHaveAttribute('aria-hidden', 'true');
      expect(screen.queryByTestId('brand-mark-plate')).not.toBeInTheDocument();
      expect(brand.querySelector('.brand-mark-plate')).toBeNull();
    });

    it.each(['en', 'de'] as const)('names the brand on the collapsed home control in %s', (locale) => {
      useCatalogue(locale);
      renderRail(true);

      expect(screen.getByTestId('brand-home')).toHaveAccessibleName(/plan-marshall/);
    });

    it.each(['en', 'de'] as const)('names the brand by the mark of the collapsed inert head in %s', (locale) => {
      useCatalogue(locale);
      unpair();
      renderRail(true);

      const inert = screen.getByTestId('brand-inert');
      const mark = inert.querySelector('[data-testid="brand-mark"]');

      expect(mark).toHaveAttribute('role', 'img');
      expect(mark).toHaveAccessibleName(/plan-marshall/);
      expect(mark).not.toHaveAttribute('aria-hidden');
    });
  });
});

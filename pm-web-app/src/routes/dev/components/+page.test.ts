import { describe, it, expect, afterEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import DevComponentsPage from './+page.svelte';
import { currentLocale, messagesStore } from '../../../lib/i18n';
import { loadMessages } from '../../../test/catalogue-fixture';

describe('/dev/components page fixture', () => {
  afterEach(() => {
    currentLocale.set('en');
    messagesStore.set({});
  });
  it('assigns unique accessible names to RowMenu fixture instances across themes (D2)', () => {
    render(DevComponentsPage);

    const moreMenuButtons = screen.getAllByRole('button', { name: /Row menu \(More/i });
    expect(moreMenuButtons).toHaveLength(2);

    const markReadButtons = screen.getAllByRole('button', { name: /Row menu \(Mark read/i });
    expect(markReadButtons).toHaveLength(2);

    const labels = [...moreMenuButtons, ...markReadButtons].map(
      (button) => button.getAttribute('aria-label') ?? '',
    );
    // One unique accessible name per fixture per theme: dropping the theme label collapses a pair.
    expect(new Set(labels).size).toBe(4);

    // Each fixture carries the theme's own label, never the raw theme key.
    expect(labels.filter((label) => label.endsWith(', Light theme)'))).toHaveLength(2);
    expect(labels.filter((label) => label.endsWith(', Dark theme)'))).toHaveLength(2);
    expect(labels.some((label) => label.endsWith(', light)') || label.endsWith(', dark)'))).toBe(
      false,
    );
  });

  it('names both themes in German on the page itself (D3b)', () => {
    currentLocale.set('de');
    messagesStore.set(loadMessages('de'));

    render(DevComponentsPage);

    expect(screen.getByRole('heading', { level: 2, name: 'Helles Theme' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Dunkles Theme' })).toBeInTheDocument();

    // The fixture labels carry the page's own theme words, not the raw keys and not the English.
    const labels = screen
      .getAllByRole('button', { name: /Zeilenmenü/i })
      .map((button) => button.getAttribute('aria-label') ?? '');
    expect(labels.filter((label) => label.endsWith(', Helles Theme)'))).toHaveLength(2);
    expect(labels.filter((label) => label.endsWith(', Dunkles Theme)'))).toHaveLength(2);
  });
});

import { describe, it, expect, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import DevComponentsPage from './+page.svelte';
import { currentLocale, messagesStore } from '../../../lib/i18n';
import { loadMessages } from '../../../test/catalogue-fixture';

describe('/dev/components page fixture', () => {
  afterEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
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

  it.each([
    ['en', 'Brand', 'replan is reading the whole plan…'],
    ['de', 'Marke', 'replan liest den ganzen Plan…'],
  ] as const)('draws a WaitingMark with the catalogue text in each theme, in %s', (locale, section, waiting) => {
    currentLocale.set(locale);
    messagesStore.set(loadMessages(locale));

    render(DevComponentsPage);

    for (const theme of ['light', 'dark']) {
      const block = document.querySelector(`[data-theme="${theme}"]`);
      expect(block, `${theme} block`).not.toBeNull();

      const marks = block?.querySelectorAll('[data-testid="waiting-mark"]') ?? [];
      expect(marks).toHaveLength(1);
      expect(marks[0]).toHaveAttribute('role', 'status');
      expect(marks[0]).toHaveTextContent(waiting);

      const titles = Array.from(block?.querySelectorAll('h3') ?? []).map((title) => title.textContent);
      expect(titles).toContain(section);
    }
  });

  describe('the kinds, links, compose field and form result line (D1)', () => {
    /** The catalogue copy each language draws, the expectation of the fixture. */
    const COPY = {
      en: {
        kinds: {
          project: 'project',
          epic: 'epic',
          plan: 'plan',
          side_job: 'side job',
          decision: 'decision',
          report: 'report',
          machine: 'machine',
        },
        chip: 'PR #214',
        compose: 'Instruction',
        stop: 'Stop the answer',
        statuses: {
          neutral: 'Give a reason.',
          success: 'Saved',
          danger: 'The settings could not be saved.',
        },
      },
      de: {
        kinds: {
          project: 'Projekt',
          epic: 'Epic',
          plan: 'Plan',
          side_job: 'Nebenarbeit',
          decision: 'Entscheidung',
          report: 'Bericht',
          machine: 'Maschine',
        },
        chip: 'PR #214',
        compose: 'Anweisung',
        stop: 'Die Antwort anhalten',
        statuses: {
          neutral: 'Gib einen Grund an.',
          success: 'Gespeichert',
          danger: 'Die Einstellungen konnten nicht gespeichert werden.',
        },
      },
    } as const;

    /** The six grounds every kind is drawn on. */
    const GROUNDS = ['app', 'surface', 'raised', 'subtle', 'rail', 'brand-subtle'];

    /**
     * @param locale the language of the page
     * @returns the light and the dark block of the rendered page
     */
    function renderThemes(locale: 'en' | 'de'): HTMLElement[] {
      currentLocale.set(locale);
      messagesStore.set(loadMessages(locale));
      render(DevComponentsPage);
      return ['light', 'dark'].map((theme) => document.querySelector(`[data-theme="${theme}"]`) as HTMLElement);
    }

    it.each(['en', 'de'] as const)('draws every kind with its word and hidden word on the six grounds, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        const grounds = Array.from(block.querySelectorAll<HTMLElement>('[data-ground]'));
        expect(grounds.map((ground) => ground.dataset.ground)).toEqual(GROUNDS);

        for (const ground of grounds) {
          for (const [kind, word] of Object.entries(COPY[locale].kinds)) {
            const marks = ground.querySelectorAll(`.kind-mark[data-kind="${kind}"]`);
            expect(marks, `${kind} on ${ground.dataset.ground}`).toHaveLength(2);
            expect(marks[0]).toHaveTextContent(word);
            expect(marks[0].querySelector('.kind-mark__word')).not.toHaveClass('sr-only');
            expect(marks[1].querySelector('.kind-mark__word')).toHaveClass('sr-only');
            expect(marks[1]).toHaveTextContent(word);
          }
        }
      }
    });

    it.each(['en', 'de'] as const)('draws the three link looks and the external link in each theme, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        for (const look of ['text', 'button', 'chip']) {
          expect(block.querySelectorAll(`a.link--${look}`).length, look).toBeGreaterThan(0);
        }
        expect(block.querySelector('a.external-link')).toHaveAttribute('target', '_blank');
      }
    });

    it.each(['en', 'de'] as const)('draws the PR chip with and without an address in each theme, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        const linked = Array.from(block.querySelectorAll('a.link--chip')).filter((anchor) =>
          anchor.textContent?.includes(COPY[locale].chip),
        );
        const plain = Array.from(block.querySelectorAll('span.link--chip')).filter((chip) =>
          chip.textContent?.includes(COPY[locale].chip),
        );

        expect(linked).toHaveLength(1);
        expect(plain).toHaveLength(1);
        expect(block.querySelector('a[href="#"]')).toBeNull();
      }
    });

    it.each(['en', 'de'] as const)('draws a Compose and one with the stop act in each theme, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        const fields = within(block).getAllByRole('textbox');

        expect(within(block).getByRole('textbox', { name: COPY[locale].compose })).toBeEnabled();
        expect(fields).toHaveLength(3);
        expect(within(block).getByRole('button', { name: COPY[locale].stop })).toBeEnabled();
      }
    });

    it.each(['en', 'de'] as const)('draws the three FormStatus tones in each theme, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        const statuses = Array.from(block.querySelectorAll('.form-status'));

        expect(statuses).toHaveLength(3);
        for (const [index, [tone, sentence]] of Object.entries(COPY[locale].statuses).entries()) {
          expect(statuses[index]).toHaveClass(`form-status--${tone}`);
          expect(statuses[index]).toHaveTextContent(sentence);
          expect(statuses[index]).toHaveAttribute('aria-live', 'polite');
        }
        expect(statuses[2].querySelector('[data-icon="state.error"]')).not.toBeNull();
      }
    });
  });
});

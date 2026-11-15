import { describe, it, expect, afterEach, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/svelte';
import DevComponentsPage from './+page.svelte';
import { KIND_ICONS, type Kind } from '../../../lib/components/KindMark.svelte';
import { REASONS } from '../../../lib/components/ReasonBadge.svelte';
import { PHASES } from '../../../lib/components/PhaseLabel.svelte';
import { MARKERS } from '../../../lib/components/Marker.svelte';
import { apiClient, type OutlineRecord, type PlanIntentRecord } from '../../../lib/api/client';
import { currentLocale, messagesStore } from '../../../lib/i18n';
import { loadMessages } from '../../../test/catalogue-fixture';

// Each test renders the whole component showroom; run beside the rest of the suite it can pass
// the five seconds of vitest's default, though it takes about one second alone.
vi.setConfig({ testTimeout: 20_000 });

describe('/dev/components page fixture', () => {
  beforeEach(() => {
    // The intent, outline and builder-check fixtures read the runtime through the client. A read that
    // never settles keeps them in their waiting state, so the counts of the other fixtures stay the
    // fixture's own and do not depend on timing.
    vi.spyOn(apiClient, 'getPlanIntent').mockReturnValue(new Promise(() => {}));
    vi.spyOn(apiClient, 'getPlanOutline').mockReturnValue(new Promise(() => {}));
    vi.spyOn(apiClient, 'subscribeEvents').mockReturnValue(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
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
          // Every kind of the authoritative mapping needs a localized expectation; a new kind fails here.
          for (const kind of Object.keys(KIND_ICONS) as Kind[]) {
            const word = COPY[locale].kinds[kind];
            expect(word, `a ${locale} expectation for ${kind}`).toBeTypeOf('string');
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

  it.each(['light', 'dark'])('draws the column engine with its side column in the %s block', (theme) => {
    render(DevComponentsPage);
    const block = document.querySelector(`[data-theme="${theme}"]`);
    const engine = block?.querySelector('[data-testid="column-engine-fixture"]');

    expect(engine).not.toBeNull();
    expect(engine?.querySelector('[data-testid="triage-page"]')).not.toBeNull();
    // The chain is five levels deep; the chat docks beside the window as its side column.
    expect(engine?.querySelector('[data-testid="side-column"]')).not.toBeNull();
  });

  it.each(['light', 'dark'])('draws the mascot figure, full and bust, in the %s block', (theme) => {
    render(DevComponentsPage);
    const block = document.querySelector(`[data-theme="${theme}"]`);
    const figures = Array.from(block?.querySelectorAll('[data-testid="mascot-figure"]') ?? []);
    const crops = figures.map((figure) => figure.getAttribute('data-crop'));

    expect(crops).toContain('full');
    expect(crops).toContain('bust');
    expect(block?.querySelectorAll('[data-testid="brand-mark"]')).toHaveLength(0);
  });

  it.each(['light', 'dark'])('draws the all-clear bust on the calm empty state only, in the %s block', (theme) => {
    render(DevComponentsPage);
    const block = document.querySelector(`[data-theme="${theme}"]`);
    // In page order: the data table's empty slot, the calm empty state, the icon empty state.
    const [tableEmpty, calm, iconEmpty] = Array.from(
      block?.querySelectorAll('[data-testid="empty-state"]') ?? [],
    );

    expect(calm?.querySelector('[data-testid="mascot-figure"]')).toHaveAttribute('data-pose', 'all-clear');
    expect(tableEmpty?.querySelector('[data-testid="mascot-figure"]')).toBeNull();
    expect(tableEmpty?.querySelector('[data-icon]')).toHaveAttribute('data-icon', 'kind.plan');
    expect(iconEmpty?.querySelector('[data-testid="mascot-figure"]')).toBeNull();
  });

  describe('the intent and outline fixtures (D4)', () => {
    /** The plan record the mock serves, as the fixture reads it. */
    const PLAN_INTENT: PlanIntentRecord = {
      plan_id: 'fix-relay',
      revision: 3,
      goal: 'Make the relay reconnect without losing queued events.',
      criteria: [
        {
          id: 'AC-001',
          text: 'When a relay connection drops, the relay shall reconnect within five seconds.',
          satisfies: [1],
          status: 'met',
        },
      ],
      non_goals: [{ id: 'NG-1', text: 'Change the on-disk queue format.' }],
      decisions: [
        {
          id: 'DC-001',
          text: 'Retry with exponential backoff.',
          decider: 'operator',
          options_weighed: ['fixed interval', 'exponential backoff'],
          question_id: 'Q-007',
        },
      ],
      open_questions: ['Does the relay need IPv6 support?'],
      deferred: [],
      builder_check: {
        resolution: 'pending',
        questions: [
          {
            id: 'BQ-001',
            text: 'Which timeout applies when the queue is full?',
            sort: 'operator',
            recommended: true,
            state: 'open',
          },
        ],
      },
    };

    /** The outline the mock serves. */
    const PLAN_OUTLINE: OutlineRecord = {
      plan_id: 'fix-relay',
      revision: 2,
      summary: 'Reconnect the relay and keep the queue durable.',
      deliverables: [
        {
          number: 1,
          title: 'Reconnect the relay',
          intent_gloss: 'Reconnect with exponential backoff.',
          affected_files: [
            { path: 'pm-relay/src/main/java/RelayConnection.java', intent: 'write-replace' },
          ],
          depends_on: [],
          satisfies: ['AC-001'],
        },
      ],
      added_dependencies: [],
    };

    it.each(['en', 'de'] as const)(
      'draws both panels in both themes, in %s',
      async (locale) => {
        vi.spyOn(apiClient, 'getPlanIntent').mockResolvedValue(PLAN_INTENT);
        vi.spyOn(apiClient, 'getPlanOutline').mockResolvedValue(PLAN_OUTLINE);

        currentLocale.set(locale);
        messagesStore.set(loadMessages(locale));
        render(DevComponentsPage);

        for (const theme of ['light', 'dark']) {
          const block = document.querySelector(`[data-theme="${theme}"]`) as HTMLElement;
          const fixture = within(block).getByTestId('intent-fixture');

          await waitFor(() =>
            expect(within(fixture).getByTestId('plan-intent-panel')).toBeInTheDocument(),
          );
          expect(within(fixture).getByTestId('outline-panel')).toBeInTheDocument();
          expect(within(fixture).getByTestId('intent-head')).toHaveTextContent('fix-relay');
          expect(within(fixture).getByTestId('outline-deliverable-1')).toBeInTheDocument();
          // A criterion names the deliverable that satisfies it by number and title, one step away.
          await waitFor(() =>
            expect(within(fixture).getByTestId('criterion-deliverables-AC-001')).toHaveTextContent(
              '1 · Reconnect the relay',
            ),
          );
          // The criterion and the deliverable point at each other, one step away.
          expect(
            within(fixture).getByTestId('outline-satisfies-1').querySelector('a'),
          ).toHaveAttribute('href', '/dev/components#criterion-AC-001');
        }
      },
    );
  });

  describe('the tag fixtures (D5)', () => {
    /**
     * @param locale the language of the page
     * @returns the light and the dark block of the rendered page
     */
    function renderThemes(locale: 'en' | 'de'): HTMLElement[] {
      currentLocale.set(locale);
      messagesStore.set(loadMessages(locale));
      render(DevComponentsPage);
      return ['light', 'dark'].map(
        (theme) => document.querySelector(`[data-theme="${theme}"]`) as HTMLElement,
      );
    }

    it.each(['en', 'de'] as const)('draws every reason, phase and marker in both themes, in %s', (locale) => {
      for (const block of renderThemes(locale)) {
        // The thirteen reasons of the closed list, the counted form, the danger tone and the row's own.
        expect(block.querySelectorAll('.reason-badge')).toHaveLength(REASONS.length + 3);
        // The six phases, the form that names the asking role and the two rows' phases.
        expect(block.querySelectorAll('.phase-label')).toHaveLength(PHASES.length + 3);
        expect(block.querySelectorAll('.marker')).toHaveLength(MARKERS.length);
        // The three rows of the RowLink fixture and the closed example row.
        expect(block.querySelectorAll('.row-link')).toHaveLength(4);
      }
    });

    it('draws the closed tag set once in one row, with its kind, reason, phase, state and menu', () => {
      const [light] = renderThemes('en');
      const examples = light.querySelectorAll('.row-link')[3];

      expect(examples.querySelector('.kind-mark')).toHaveTextContent('plan');
      expect(examples.querySelector('a.row-link__title')).toHaveTextContent('Drop the legacy endpoints');
      expect(examples.querySelector('.reason-badge')).toHaveTextContent('! decision');
      expect(examples.querySelector('.phase-label')).toHaveTextContent('finalize');
      expect(examples.querySelector('.status-label')).toHaveTextContent('waiting for you');
      expect(examples.querySelector('.row-link__acts button')).not.toBeNull();
    });

    it('draws the three conformed tags once each', () => {
      const [light] = renderThemes('en');
      // The five tones of the status section, the row fixture's state, the selected row's and the example row's.
      const STATUS_LABELS = 5 + 1 + 1 + 1;

      expect(light.querySelectorAll('.status-label')).toHaveLength(STATUS_LABELS);
      expect(light.querySelectorAll('[data-testid="count-badge"]').length).toBeGreaterThanOrEqual(3);
      expect(light.querySelectorAll('button.chip').length).toBeGreaterThanOrEqual(1);
    });
  });
});

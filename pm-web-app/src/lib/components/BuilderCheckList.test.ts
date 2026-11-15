import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/svelte';
import BuilderCheckList from './BuilderCheckList.svelte';
import type { BuilderCheckRecord } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `BuilderCheckList` (design `intent-and-outline.adoc#builder`): the questions a builder would still
 * need answered, grouped by who can answer them; the builder's recommendation marked as the
 * builder's and preselecting nothing; each question's resolution; and the check's own state, with a
 * `not_run` shown as evidence with the plan's reason and never as a pass or a hold.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de' = 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

/**
 * @param overrides the members this case changes
 * @returns a builder check with one question of each sort and each state
 */
function check(overrides: Partial<BuilderCheckRecord> = {}): BuilderCheckRecord {
  return {
    resolution: 'pending',
    questions: [
      {
        id: 'BQ-001',
        text: 'Which timeout applies when the queue is full during a reconnect?',
        sort: 'operator',
        recommended: true,
        state: 'open',
      },
      {
        id: 'BQ-002',
        text: 'Use the existing retry helper from cui-http.',
        sort: 'minor',
        recommended: false,
        state: 'open',
      },
      {
        id: 'BQ-003',
        text: 'AC-002 answers the event-retention question.',
        sort: 'answered',
        recommended: false,
        state: 'answered',
        answer: 'AC-002',
      },
      {
        id: 'BQ-004',
        text: 'Does the relay need IPv6 support?',
        sort: 'operator',
        recommended: false,
        state: 'deferred',
      },
    ],
    ...overrides,
  };
}

describe('BuilderCheckList', () => {
  beforeEach(() => {
    useCatalogue();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('groups the questions by who can answer them, in the design order', () => {
    const { container } = render(BuilderCheckList, { props: { check: check() } });

    const groups = within(container)
      .getAllByTestId(/^builder-sort-/)
      .map((group) => group.getAttribute('data-sort'));

    expect(groups).toEqual(['answered', 'minor', 'operator']);
    expect(screen.getByTestId('builder-sort-answered')).toHaveTextContent('answered by the record');
    expect(screen.getByTestId('builder-sort-minor')).toHaveTextContent("the builder's call");
    expect(screen.getByTestId('builder-sort-operator')).toHaveTextContent('needs you');
  });

  it('marks the recommendation as a property of the question and preselects nothing', () => {
    const { container } = render(BuilderCheckList, { props: { check: check() } });

    const question = screen.getByTestId('builder-question-BQ-001');
    expect(question).toHaveTextContent('recommended');
    expect(container.querySelector('input')).toBeNull();
    expect(container.querySelector('[aria-checked]')).toBeNull();
    expect(container.querySelector('[aria-selected]')).toBeNull();
  });

  it('shows each resolution state, an answer beside the question it answers', () => {
    render(BuilderCheckList, { props: { check: check() } });

    expect(screen.getByTestId('builder-resolution-BQ-001')).toHaveTextContent('open');
    expect(screen.getByTestId('builder-resolution-BQ-003')).toHaveTextContent('answered: AC-002');
    expect(screen.getByTestId('builder-resolution-BQ-004')).toHaveTextContent(
      'deferred: the plan continues without it',
    );
  });

  it.each([
    ['pending', 'questions wait'],
    ['answered', 'resolved'],
  ] as const)('says the check is %s when its resolution is %s', (resolution, word) => {
    render(BuilderCheckList, { props: { check: check({ resolution }) } });

    expect(screen.getByTestId('builder-state')).toHaveTextContent(word);
    expect(screen.getByTestId('builder-state').querySelector('.status-label')).toBeNull();
  });

  it("shows a not_run check as evidence with the plan's reason, never as a pass or a hold", () => {
    render(BuilderCheckList, {
      props: {
        check: {
          resolution: 'not_run',
          questions: [],
          reason: 'The builder check task was lost before it could start.',
        },
      },
    });

    const state = screen.getByTestId('builder-state');
    expect(state).toHaveTextContent(
      'not run: The builder check task was lost before it could start.',
    );
    expect(state.querySelector('.status-label--success')).toBeNull();
    expect(state.querySelector('.status-label--danger')).toBeNull();
  });

  it('renders a reason with control sequences and markup as text', () => {
    render(BuilderCheckList, {
      props: {
        check: {
          resolution: 'not_run',
          questions: [],
          reason: 'lost\u001b[31m <b>before</b>\u001b[0m it started',
        },
      },
    });

    const state = screen.getByTestId('builder-state');
    expect(state.textContent).toContain('not run: lost <b>before</b> it started');
    expect(state.querySelector('b')).toBeNull();
  });

  it('renders a check without questions without failing', () => {
    render(BuilderCheckList, {
      props: { check: { resolution: 'pending', questions: [] } },
    });

    expect(screen.getByTestId('builder-check')).toBeInTheDocument();
    expect(screen.queryAllByTestId(/^builder-question-/)).toEqual([]);
  });

  it('renders nothing but the title where the plan carries no builder check', () => {
    render(BuilderCheckList, { props: { check: null } });

    expect(screen.getByTestId('builder-check')).toHaveTextContent('Builder check');
    expect(screen.queryAllByTestId(/^builder-question-/)).toEqual([]);
  });
});

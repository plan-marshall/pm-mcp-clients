import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import EvidenceView, { plainLabel } from './EvidenceView.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `EvidenceView` (decisions.adoc#order): the evidence a decision names, as fields with plain labels,
 * a `file`/`lines` pair, and any structure the view does not know folded into a JSON block that is
 * shown expanded. The description is the view's, not this component's, so it never appears twice.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('EvidenceView', () => {
  it('names the block and every field with a plain label', () => {
    useCatalogue('en');
    render(EvidenceView, {
      props: { evidence: { endpoints: '/api/v1/legacy', waiver_gate: 'coverage' } },
    });

    expect(screen.getByRole('heading', { name: 'Evidence' })).toBeInTheDocument();
    expect(screen.getByText('Endpoints')).toBeInTheDocument();
    expect(screen.getByText('Waiver gate')).toBeInTheDocument();
    expect(screen.getByText('/api/v1/legacy')).toBeInTheDocument();
  });

  it('shows a file with the lines it names', () => {
    useCatalogue('en');
    render(EvidenceView, {
      props: {
        evidence: { file: { file: 'pm-relay/src/main/java/EventQueue.java', lines: '12-20' } },
      },
    });

    expect(
      screen.getByText('pm-relay/src/main/java/EventQueue.java:12-20'),
    ).toBeInTheDocument();
  });

  it('folds a structure it does not know, expanded and as JSON', () => {
    useCatalogue('en');
    render(EvidenceView, {
      props: { evidence: { votes: { quorum: { a: 1, b: 2 } } } },
    });

    const fold = screen.getByTestId('evidence-structure-votes');
    expect(fold).toBeVisible();
    expect(fold.hasAttribute('open')).toBe(true);
    expect(fold.textContent).toContain('"quorum"');
  });

  it('renders every value as text, never as markup', () => {
    useCatalogue('en');
    const { container } = render(EvidenceView, {
      props: { evidence: { note: '<img src=x onerror="alert(1)">' } },
    });

    expect(container.querySelector('img')).toBeNull();
    expect(screen.getByText('<img src=x onerror="alert(1)">')).toBeInTheDocument();
  });

  it('shows an app path the evidence offers as a link', () => {
    useCatalogue('en');
    render(EvidenceView, {
      props: {
        evidence: {
          diff: { file: 'pm-relay/src/main/java/EventQueue.java', lines: 4, link: '/plans/fix-relay' },
        },
      },
    });

    expect(screen.getByRole('link', { name: 'Open' })).toHaveAttribute('href', '/plans/fix-relay');
  });

  it('draws a plain label out of the runtime key', () => {
    expect(plainLabel('waiverGate')).toBe('Waiver gate');
    expect(plainLabel('waiver_gate')).toBe('Waiver gate');
    expect(plainLabel('endpoints')).toBe('Endpoints');
  });

  it('renders nothing where the decision names no evidence', () => {
    useCatalogue('en');
    const { container } = render(EvidenceView, { props: { evidence: {} } });

    expect(container.querySelector('[data-testid="evidence-view"]')).toBeNull();
  });
});

import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render } from '@testing-library/svelte';
import PhaseLabel, { PHASES, ROLES } from './PhaseLabel.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `PhaseLabel` (components-tags.adoc, phase-label): the phase's icon and its word, no box, in
 * `--color-text-secondary`; where a decision names the role that asks, the role's word follows after
 * `·`. Every word is the catalogue's, so no wire token reaches the screen.
 */

const STYLE = styleOf(path.resolve(__dirname, 'PhaseLabel.svelte'));

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'en' | 'de'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('PhaseLabel', () => {
  it('carries the six phases of the workflow visual language', () => {
    expect([...PHASES]).toEqual(['init', 'refine', 'outline', 'plan', 'execute', 'finalize']);
  });

  it.each(['en', 'de'] as const)('draws each phase with its icon and its word in %s', (locale) => {
    useCatalogue(locale);

    for (const phase of PHASES) {
      const { container, unmount } = render(PhaseLabel, { props: { phase } });

      const icon = container.querySelector('[data-workflow-icon]');
      expect(icon, `${phase} icon`).toHaveAttribute('data-workflow-icon', `phase.${phase}`);
      expect(container.querySelector('.phase-label__word')).toHaveTextContent(phase);
      expect(container.querySelector('.phase-label')).not.toHaveTextContent('phase');
      unmount();
    }
  });

  it.each([
    ['en', 'execute · implement'],
    ['de', 'execute · implement'],
  ] as const)('names the asking role after the phase in %s', (locale, text) => {
    useCatalogue(locale);
    const { container } = render(PhaseLabel, { props: { phase: 'execute', role: 'implement' } });

    expect(container.querySelector('.phase-label')).toHaveTextContent(text);
  });

  it('carries the roles the runtime sends as the asking role', () => {
    expect([...ROLES]).toEqual([
      'implement',
      'architecture',
      'outline',
      'enrich',
      'decompose',
      'settings',
    ]);
  });

  it('shows no role when the runtime names none', () => {
    useCatalogue('en');
    const { container } = render(PhaseLabel, { props: { phase: 'execute' } });

    expect(container.querySelector('.phase-label__role')).toBeNull();
    expect(container.querySelector('.phase-label')).toHaveTextContent('execute');
  });

  it('repeats no kind before the phase', () => {
    useCatalogue('en');
    const { container } = render(PhaseLabel, { props: { phase: 'init' } });

    expect(container.querySelector('.phase-label')?.textContent?.trim()).toBe('init');
  });

  it('lets no wire token reach the DOM for an unknown phase', () => {
    useCatalogue('en');
    const { container } = render(PhaseLabel, { props: { phase: 'teleported' } });

    expect(container.innerHTML).not.toContain('teleported');
    expect(container.textContent?.trim()).toBe('');
  });

  it('lets no wire token reach the DOM for an unknown role', () => {
    useCatalogue('en');
    const { container } = render(PhaseLabel, { props: { phase: 'execute', role: 'teleported' } });

    expect(container.innerHTML).not.toContain('teleported');
    expect(container.querySelector('.phase-label')).toHaveTextContent('execute');
  });

  it('draws no box and takes the secondary text colour', () => {
    expect(ruleOf(STYLE, '.phase-label')).toContain('color: var(--color-text-secondary)');
    expect(STYLE).not.toContain('background-color');
    expect(STYLE).not.toContain('border:');
  });
});

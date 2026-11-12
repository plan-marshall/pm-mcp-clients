import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import PrChip from './PrChip.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * `PrChip` (components-tags.adoc, PrChip): a pull request as an `ExternalLink` in the chip look, its
 * text `common.pull_request.chip`; without an address the same text and no link.
 */

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

const HREF = 'https://github.com/plan-marshall/pm-mcp-clients/pull/214';

describe('PrChip', () => {
  it.each(['en', 'de'] as const)('opens the pull request in the chip look in %s', (locale) => {
    useCatalogue(locale);
    render(PrChip, { props: { number: 214, href: HREF } });
    const link = screen.getByRole('link');

    expect(link).toHaveAttribute('href', HREF);
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveClass('link--chip');
    expect(link.querySelector('.external-link__label')?.textContent).toBe('PR #214');
  });

  it.each(['en', 'de'] as const)('draws the same text without a link when the address is unknown in %s', (locale) => {
    useCatalogue(locale);
    const { container } = render(PrChip, { props: { number: 214 } });

    expect(screen.queryByRole('link')).toBeNull();
    expect(container.querySelector('a')).toBeNull();
    expect(container.querySelector('.pr-chip')?.textContent).toBe('PR #214');
  });

  it('never draws href="#"', () => {
    useCatalogue('en');
    const withAddress = render(PrChip, { props: { number: 7, href: HREF } });
    expect(withAddress.container.querySelector('a[href="#"]')).toBeNull();
    withAddress.unmount();

    const withoutAddress = render(PrChip, { props: { number: 7, href: undefined } });
    expect(withoutAddress.container.querySelector('a[href="#"]')).toBeNull();
  });
});

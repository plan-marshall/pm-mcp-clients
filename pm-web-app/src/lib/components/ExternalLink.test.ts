import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import ExternalLink from './ExternalLink.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { ruleOf, rulesOf, styleOf } from '../../test/templateStyle';

/**
 * `ExternalLink` (components.adoc, ExternalLink): a link to a place outside the app that opens in a
 * new tab, with the external-link glyph after its label and "opens in a new tab" in its name. An
 * address whose scheme is not http or https is drawn as text.
 */

const STYLE = styleOf(path.resolve(__dirname, 'ExternalLink.svelte'));

/** @param locale the language the catalogue is loaded in */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('ExternalLink', () => {
  it('opens in a new tab without handing the opener over', () => {
    useCatalogue('en');
    render(ExternalLink, { props: { href: 'https://github.com/plan-marshall/pm-mcp-clients/pull/214', label: 'Pull request' } });
    const link = screen.getByRole('link');

    expect(link).toHaveAttribute('href', 'https://github.com/plan-marshall/pm-mcp-clients/pull/214');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('draws the external-link glyph after its label', () => {
    useCatalogue('en');
    render(ExternalLink, { props: { href: 'https://example.org/docs', label: 'Documentation' } });
    const link = screen.getByRole('link');
    const label = link.querySelector('.external-link__label') as HTMLElement;
    const glyph = link.querySelector('[data-icon="act.external"]') as HTMLElement;

    expect(glyph).not.toBeNull();
    expect(glyph).toHaveAttribute('aria-hidden', 'true');
    expect(label.compareDocumentPosition(glyph) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it.each([
    ['en', 'Documentation (opens in a new tab)'],
    ['de', 'Documentation (öffnet in einem neuen Tab)'],
  ] as const)('says it opens a new tab in its accessible name in %s', (locale, name) => {
    useCatalogue(locale);
    render(ExternalLink, { props: { href: 'https://example.org/docs', label: 'Documentation' } });

    expect(screen.getByRole('link')).toHaveAccessibleName(name);
  });

  it('draws the text look by default and the chip look on request', () => {
    useCatalogue('en');
    const plain = render(ExternalLink, { props: { href: 'https://example.org', label: 'Site' } });
    expect(screen.getByRole('link')).toHaveClass('link--text');
    expect(screen.getByRole('link')).not.toHaveClass('link--chip');
    plain.unmount();

    render(ExternalLink, { props: { href: 'https://example.org', label: 'Site', chip: true } });
    expect(screen.getByRole('link')).toHaveClass('link--chip');
  });

  it.each(['javascript:alert(1)', 'data:text/html,<b>x</b>', 'file:///etc/passwd', 'not a url', ''])(
    'draws the label as text and no anchor for %j',
    (href) => {
      useCatalogue('en');
      const { container } = render(ExternalLink, { props: { href, label: 'Pull request' } });

      expect(container.querySelector('a')).toBeNull();
      expect(container).toHaveTextContent('Pull request');
    },
  );

  it.each([
    'https:example.org/pull/3',
    'http:example.org',
    'HTTPS:example.org',
    '  https:example.org/pull/3',
    'http:example.org  ',
    '\thttps:example.org\n',
    'https:/example.org',
    ' http:/example.org ',
  ])('draws the label as text for the scheme without its two slashes %j', (href) => {
    useCatalogue('en');
    const { container } = render(ExternalLink, { props: { href, label: 'Pull request' } });

    expect(container.querySelector('a')).toBeNull();
    expect(container).toHaveTextContent('Pull request');
  });

  /*
   * The address forms PLAN-147 left open: a special scheme skips every slash and backslash after it
   * (WHATWG URL spec, "special authority ignore slashes state"), so `https:///host` names `host` and
   * `https:\\host` names it too; a control character in front is stripped by the same parser.
   */
  it.each([
    ['https:\\\\host', 'a backslash pair instead of the two slashes'],
    ['https:/\t/host', 'a tab between the two slashes'],
    ['\u0000https://example.org', 'a leading NUL control character'],
    ['\u0001https://example.org/x', 'a leading start-of-heading control character'],
    ['\thttps://example.org', 'a leading tab'],
    ['\nhttps://example.org', 'a leading line feed'],
    ['\rhttps://example.org/x', 'a leading carriage return'],
    ['https:///evil.example/x', 'a host behind a third slash'],
    ['https://\\\\evil.example/x', 'a host behind two backslashes'],
  ])('refuses %j (%s)', (href, _why) => {
    useCatalogue('en');
    const { container } = render(ExternalLink, { props: { href, label: 'Pull request' } });

    expect(container.querySelector('a')).toBeNull();
    expect(container).toHaveTextContent('Pull request');
  });

  it.each(['http://example.org', 'HTTPS://EXAMPLE.ORG/A'])('accepts the web address %s', (href) => {
    useCatalogue('en');
    render(ExternalLink, { props: { href, label: 'Site' } });

    expect(screen.getByRole('link')).toHaveAttribute('href', href);
  });

  it('renders the address that was checked, without the white space around it', () => {
    useCatalogue('en');
    render(ExternalLink, { props: { href: '  https://example.org/docs\n', label: 'Documentation' } });

    expect(screen.getByRole('link')).toHaveAttribute('href', 'https://example.org/docs');
  });

  it('keeps the focus ring', () => {
    expect(ruleOf(STYLE, '.link:focus-visible')).toContain('box-shadow: var(--focus-ring)');
  });

  it('gives the text and the chip look a target of at least 24 px, and 44 px on a phone', () => {
    const [desktop, phone] = rulesOf(STYLE, '.link');

    expect(desktop).toContain('min-width: 24px');
    expect(desktop).toContain('min-height: 24px');
    expect(phone).toContain('min-width: 44px');
    expect(phone).toContain('min-height: 44px');
  });

  it('exempts only the inline text link of a sentence from the target size', () => {
    const [desktop, phone] = rulesOf(STYLE, ':global(:is(p, li, dd, td, figcaption, blockquote)) > .link--text');

    for (const rule of [desktop, phone]) {
      expect(rule).toContain('min-width: 0');
      expect(rule).toContain('min-height: 0');
    }
    expect(STYLE.match(/min-width: 0/g)).toHaveLength(2);
  });

  it('keeps the text drawn without an address free of the target size', () => {
    useCatalogue('en');
    const { container } = render(ExternalLink, { props: { href: '', label: 'Pull request', chip: true } });
    const text = container.querySelector('.external-link__text') as HTMLElement;

    expect(text).not.toHaveClass('link');
  });
});

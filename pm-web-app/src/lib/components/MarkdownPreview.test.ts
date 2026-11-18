import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import MarkdownPreview from './MarkdownPreview.svelte';

/**
 * `MarkdownPreview`: the plain-text preview of the operator's description on the start page. It
 * shapes headings, list items, fenced code and paragraphs, and renders every one of them as text.
 */
describe('MarkdownPreview', () => {
  it('shapes headings, items, code and paragraphs, skipping blank lines', () => {
    const { container } = render(MarkdownPreview, {
      value: [
        '# Relay fix',
        '### Scope',
        '',
        '- the relay',
        '* the docs',
        '```',
        '  mvn verify',
        '- not an item inside code',
        '```',
        'Keep the wire format.',
      ].join('\n'),
    });

    const headings = container.querySelectorAll('.markdown-preview__heading');
    expect([...headings].map((h) => [h.textContent, h.getAttribute('data-level')])).toEqual([
      ['Relay fix', '1'],
      ['Scope', '3'],
    ]);
    expect(
      [...container.querySelectorAll('.markdown-preview__item')].map((i) => i.textContent),
    ).toEqual(['• the relay', '• the docs']);
    expect(
      [...container.querySelectorAll('.markdown-preview__code')].map((c) => c.textContent),
    ).toEqual(['  mvn verify', '- not an item inside code']);
    expect(
      [...container.querySelectorAll('.markdown-preview__paragraph')].map((p) => p.textContent),
    ).toEqual(['Keep the wire format.']);
  });

  it('shows markup in a description as text and runs none of it', () => {
    const { container } = render(MarkdownPreview, {
      value: '# <img src=x onerror="alert(1)">\n<script>alert(2)</script>',
    });

    expect(screen.getByText('<img src=x onerror="alert(1)">')).toBeInTheDocument();
    expect(screen.getByText('<script>alert(2)</script>')).toBeInTheDocument();
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('script')).toBeNull();
  });

  it('renders nothing for an empty description', () => {
    render(MarkdownPreview, { value: '' });

    const preview = screen.getByTestId('markdown-preview');
    expect(preview.children).toHaveLength(0);
    expect(preview.textContent?.trim()).toBe('');
  });
});

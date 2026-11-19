import { describe, it, expect } from 'vitest';
import { tick } from 'svelte';
import { fireEvent, render } from '@testing-library/svelte';
import HarnessChoice from './HarnessChoice.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * @param over the fields to override
 * @returns one harness row as the runtime serves it
 */
function harness(over: Record<string, unknown>) {
  return {
    harness_id: 'claude',
    name: 'Claude Code',
    found: true,
    supported: true,
    worker_eligible: true,
    ...over,
  };
}

describe('HarnessChoice monograms', () => {
  it('draws a monogram for a harness the runtime serves no icon for', () => {
    const { getByTestId } = render(HarnessChoice, {
      props: {
        harnesses: [harness({}), harness({ harness_id: 'opencode', name: 'OpenCode' })],
      },
    });

    const claude = getByTestId('harness-monogram-claude');
    expect(claude.textContent?.trim()).toBe('CC');
    expect(claude.getAttribute('aria-hidden')).toBe('true');
    expect(getByTestId('harness-monogram-opencode').textContent?.trim()).toBe('O');
  });

  it('draws the fetched icon instead of a monogram where the runtime has one', () => {
    const { container, queryByTestId } = render(HarnessChoice, {
      props: { harnesses: [harness({ icon_url: '/api/v1/harness-icons/claude.svg' })] },
    });

    const icon = container.querySelector('img.choice-screen__icon') as HTMLImageElement | null;
    expect(icon).not.toBeNull();
    expect(icon?.getAttribute('src')).toBe('/api/v1/harness-icons/claude.svg');
    expect(queryByTestId('harness-monogram-claude')).toBeNull();
    expect(container.textContent).not.toContain('/>');
  });

  it('refuses an icon from a maker server and keeps the monogram', () => {
    const { container, getByTestId } = render(HarnessChoice, {
      props: {
        harnesses: [
          harness({ icon_url: 'https://example.invalid/claude.svg' }),
          harness({ harness_id: 'codex', name: 'Codex', icon_url: '/\\maker.example/i.svg' }),
          harness({
            harness_id: 'opencode',
            name: 'OpenCode',
            icon_url: '/\t/maker.example/i.svg',
          }),
        ],
      },
    });

    expect(container.querySelector('img.choice-screen__icon')).toBeNull();
    expect(getByTestId('harness-monogram-claude').textContent?.trim()).toBe('CC');
    expect(getByTestId('harness-monogram-codex').textContent?.trim()).toBe('C');
    expect(getByTestId('harness-monogram-opencode').textContent?.trim()).toBe('O');
  });

  it('falls back to the monogram when the served icon does not load', async () => {
    const { container, getByTestId, queryByTestId } = render(HarnessChoice, {
      props: { harnesses: [harness({ icon_url: '/api/v1/harness-icons/claude.svg' })] },
    });

    const icon = container.querySelector('img.choice-screen__icon') as HTMLImageElement;
    expect(icon).not.toBeNull();
    await fireEvent.error(icon);

    expect(queryByTestId('harness-icon-claude')).toBeNull();
    expect(getByTestId('harness-monogram-claude').textContent?.trim()).toBe('CC');
  });

  it('falls back to the harness id when the name carries no letter', () => {
    const { getByTestId } = render(HarnessChoice, {
      props: { harnesses: [harness({ name: '   ' })] },
    });

    expect(getByTestId('harness-monogram-claude').textContent?.trim()).toBe('C');
  });

  it('updates token usage wording reactively when switching between English and German', async () => {
    try {
      currentLocale.set('en');
      messagesStore.set(loadMessages('en'));

      const { container } = render(HarnessChoice, {
        props: { harnesses: [harness({ token_usage: 'complete' })] },
      });
      expect(container.querySelector('[data-harness="claude"]')).toHaveTextContent('usage complete');

      currentLocale.set('de');
      messagesStore.set(loadMessages('de'));
      await tick();

      expect(container.querySelector('[data-harness="claude"]')).toHaveTextContent(
        'Tokenverbrauch vollständig',
      );
    } finally {
      currentLocale.set('en');
      messagesStore.set(loadMessages('en'));
    }
  });

  it('keeps complete wording without detail even when token_detail is also set', () => {
    const { container } = render(HarnessChoice, {
      props: {
        harnesses: [harness({ token_usage: 'complete', token_detail: 'input, output' })],
      },
    });

    const row = container.querySelector('[data-harness="claude"]');
    expect(row).toHaveTextContent('usage complete');
    expect(row).not.toHaveTextContent('input, output');
  });

  it('resyncs chosen harnesses and default when the harnesses prop changes', async () => {
    const { rerender, getByTestId, queryByTestId } = render(HarnessChoice, {
      props: {
        harnesses: [
          harness({ harness_id: 'claude', name: 'Claude Code', default: true }),
          harness({ harness_id: 'opencode', name: 'OpenCode' }),
        ],
      },
    });

    expect(getByTestId('harness-toggle-claude')).toBeChecked();
    await fireEvent.click(getByTestId('harness-default-opencode'));
    expect(getByTestId('harness-check')).toHaveTextContent('OpenCode the default');

    await rerender({
      harnesses: [harness({ harness_id: 'codex', name: 'Codex', default: true })],
    });

    expect(queryByTestId('harness-toggle-claude')).toBeNull();
    expect(getByTestId('harness-toggle-codex')).toBeChecked();
    expect(getByTestId('harness-check')).toHaveTextContent('Check 1 harness, Codex the default');
  });

  it('keeps pending harness and default choices when selection-relevant content is unchanged', async () => {
    const base = [
      harness({ harness_id: 'claude', name: 'Claude Code', default: true }),
      harness({ harness_id: 'opencode', name: 'OpenCode' }),
    ];
    const { rerender, getByTestId } = render(HarnessChoice, {
      props: { harnesses: base.map((h) => ({ ...h })) },
    });

    await fireEvent.click(getByTestId('harness-default-opencode'));
    await fireEvent.change(getByTestId('harness-toggle-claude'));
    expect(getByTestId('harness-toggle-claude')).not.toBeChecked();
    expect(getByTestId('harness-check')).toHaveTextContent('Check 1 harness, OpenCode the default');

    await rerender({
      harnesses: base.map((h) => ({ ...h, token_detail: 'refreshed-metadata-only' })),
    });

    expect(getByTestId('harness-toggle-claude')).not.toBeChecked();
    expect(getByTestId('harness-toggle-opencode')).toBeChecked();
    expect(getByTestId('harness-check')).toHaveTextContent('Check 1 harness, OpenCode the default');
  });
});

import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import HarnessChoice from './HarnessChoice.svelte';

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
  });

  it('refuses an icon from a maker server and keeps the monogram', () => {
    const { container, getByTestId } = render(HarnessChoice, {
      props: {
        harnesses: [
          harness({ icon_url: 'https://example.invalid/claude.svg' }),
          harness({ harness_id: 'codex', name: 'Codex', icon_url: '/\\maker.example/i.svg' }),
        ],
      },
    });

    expect(container.querySelector('img.choice-screen__icon')).toBeNull();
    expect(getByTestId('harness-monogram-claude').textContent?.trim()).toBe('CC');
    expect(getByTestId('harness-monogram-codex').textContent?.trim()).toBe('C');
  });

  it('falls back to the harness id when the name carries no letter', () => {
    const { getByTestId } = render(HarnessChoice, {
      props: { harnesses: [harness({ name: '   ' })] },
    });

    expect(getByTestId('harness-monogram-claude').textContent?.trim()).toBe('C');
  });
});

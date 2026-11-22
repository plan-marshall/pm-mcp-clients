import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import EpicAdoptView from './EpicAdoptView.svelte';

describe('EpicAdoptView', () => {
  it('renders adoptable epics with primary Adopt button and secondary Start a new epic button (acts by role)', async () => {
    const onAdopt = vi.fn();
    const onNew = vi.fn();

    render(EpicAdoptView, {
      props: {
        epics: [
          {
            epic_id: 'relay-hardening',
            title: 'relay-hardening',
          },
        ],
        projectId: 'pm-mcp-clients',
        onAdopt,
        onNew,
      },
    });

    const adoptBtn = screen.getByRole('button', { name: 'Adopt relay-hardening' });
    expect(adoptBtn).toHaveClass('btn--primary');
    await fireEvent.click(adoptBtn);
    expect(onAdopt).toHaveBeenCalledWith('relay-hardening');

    const newBtn = screen.getByRole('button', { name: 'Start a new epic' });
    expect(newBtn).toHaveClass('btn--secondary');
    await fireEvent.click(newBtn);
    expect(onNew).toHaveBeenCalled();
  });

  it('renders a failed adoption under ErrorState with cause and ghost Try again / Back buttons', async () => {
    const onRetry = vi.fn();
    const onBack = vi.fn();
    const onAbandon = vi.fn();

    render(EpicAdoptView, {
      props: {
        failure: {
          epic_id: 'relay-hardening',
          reason: 'ledger_invalid',
          detail: 'overlaps with existing epic',
        },
        projectId: 'pm-mcp-clients',
        onRetry,
        onBack,
        onAbandon,
      },
    });

    expect(screen.getByTestId('error-state')).toBeInTheDocument();
    expect(screen.getByTestId('epic-adopt-failed')).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: 'Try again' });
    expect(retryBtn).toHaveClass('btn--ghost');
    await fireEvent.click(retryBtn);
    expect(onRetry).toHaveBeenCalled();

    const backBtn = screen.getByRole('button', { name: 'Back' });
    expect(backBtn).toHaveClass('btn--ghost');
    await fireEvent.click(backBtn);
    expect(onBack).toHaveBeenCalled();

    const abandonBtn = screen.getByRole('button', { name: 'Abandon' });
    expect(abandonBtn).toHaveClass('btn--ghost');
    await fireEvent.click(abandonBtn);
    expect(onAbandon).toHaveBeenCalled();
  });
});

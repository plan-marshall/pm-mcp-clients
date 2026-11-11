import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import StartView from './StartView.svelte';
import { apiClient } from '../api/client';
import { ClientError } from '../api/errors';
import { PROJECT, sources } from './startFixtures';

/**
 * The start view's two patterns: the error block of a failed read with its act, the skeletons of a
 * read still in flight, and the act line of a failure the act itself reported.
 */

/**
 * @param repr the representation the entry is opened with
 * @returns nothing; the client is stubbed for one open entry
 */
function arrangeOpen(repr = sources()): void {
  vi.spyOn(apiClient, 'openEntry').mockResolvedValue(repr);
  vi.spyOn(apiClient, 'getEntry').mockResolvedValue(repr);
  vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});
  vi.spyOn(apiClient, 'getProjectExplorer').mockResolvedValue({
    favourites: [],
    recent: [],
    all_projects: [],
    being_added: [],
  });
}

describe('StartView', () => {
  it("shows the sentence of the act failure's code instead of the code", async () => {
    arrangeOpen();
    vi.spyOn(apiClient, 'actEntry').mockRejectedValue(new ClientError('timeout'));

    render(StartView, { projectId: PROJECT, kind: 'plan' });

    await screen.findByTestId('source-list');
    await fireEvent.input(screen.getByTestId('description-input'), {
      target: { value: 'Add a Codeberg provider for issues' },
    });
    await fireEvent.click(screen.getByTestId('description-add'));

    const banner = await screen.findByTestId('start-action-error');
    expect(banner.textContent?.trim()).toBe(
      'The start failed: Try again; if it keeps taking this long, check that the runtime is running.',
    );
  });

  it('draws the error pattern with its act when the entry cannot be read', async () => {
    vi.spyOn(apiClient, 'getEntry').mockRejectedValue(new ClientError('network_offline'));
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { entryId: 'ent-3f9a2c41' });

    const error = await screen.findByTestId('start-error');
    expect(error).toHaveTextContent('The machine is not reachable');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('draws skeletons while the entry is still being read', async () => {
    vi.spyOn(apiClient, 'openEntry').mockReturnValue(new Promise(() => {}));
    vi.spyOn(apiClient, 'subscribeEntryEvents').mockReturnValue(() => {});

    render(StartView, { projectId: PROJECT, kind: 'plan' });

    const loading = await screen.findByTestId('start-loading');
    expect(loading.querySelector('[data-testid="skeleton"]')).not.toBeNull();
  });
});

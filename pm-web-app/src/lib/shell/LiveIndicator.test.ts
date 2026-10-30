import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import LiveIndicator from './LiveIndicator.svelte';
import { runtimeStore, retryEventStream } from '../stores/runtime';
import { apiClient } from '../api/client';
import { messagesStore, currentLocale } from '../i18n';
import { makeRuntimeState } from '../test/runtimeState';
import { loadMessages } from '../../test/catalogue-fixture';

vi.mock('../stores/runtime', async () => {
  const actual = await vi.importActual<typeof import('../stores/runtime')>('../stores/runtime');
  return { ...actual, retryEventStream: vi.fn() };
});

/**
 * The live indicator of UX-P-12: the dot and the word of every stream state, and the one act of
 * the offline state.
 */
describe('the live indicator', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
    runtimeStore.set(makeRuntimeState({ streamState: 'connected', isOffline: false }));
    vi.mocked(retryEventStream).mockClear();
  });

  it('reads Live while the stream is connected', () => {
    render(LiveIndicator);
    const indicator = screen.getByTestId('live-indicator');

    expect(indicator).toHaveAttribute('data-state', 'connected');
    expect(indicator).toHaveTextContent('Live');
    expect(screen.queryByTestId('live-retry')).not.toBeInTheDocument();
  });

  it('reads Reconnecting while the stream retries', async () => {
    render(LiveIndicator);
    runtimeStore.update((state) => ({ ...state, streamState: 'reconnecting' }));

    await waitFor(() =>
      expect(screen.getByTestId('live-indicator')).toHaveAttribute('data-state', 'reconnecting'),
    );
    expect(screen.getByTestId('live-indicator')).toHaveTextContent('Reconnecting…');
    expect(screen.queryByTestId('live-retry')).not.toBeInTheDocument();
  });

  it('reads Offline with Try again after the stream gave up, and offers the retry', async () => {
    render(LiveIndicator);
    runtimeStore.update((state) => ({ ...state, streamState: 'offline' }));

    await waitFor(() =>
      expect(screen.getByTestId('live-indicator')).toHaveAttribute('data-state', 'offline'),
    );
    expect(screen.getByTestId('live-indicator')).toHaveTextContent('Offline');

    const retry = screen.getByTestId('live-retry');
    expect(retry).toHaveTextContent('Try again');
    await fireEvent.click(retry);
    expect(retryEventStream).toHaveBeenCalledTimes(1);
  });

  it('keeps Live while the stream is connected, even when a request failed', async () => {
    // A failed request is not a dead connection: the stream is the authority of UX-P-12.
    runtimeStore.update((state) => ({ ...state, isOffline: true }));

    render(LiveIndicator);
    await waitFor(() =>
      expect(screen.getByTestId('live-indicator')).toHaveAttribute('data-state', 'connected'),
    );
    expect(screen.getByTestId('live-indicator')).toHaveTextContent('Live');
  });

  it('keeps the app from claiming live while the stream only connects', async () => {
    runtimeStore.update((state) => ({ ...state, streamState: 'connecting' }));

    render(LiveIndicator);
    await waitFor(() =>
      expect(screen.getByTestId('live-indicator')).toHaveAttribute('data-state', 'connecting'),
    );
    expect(screen.getByTestId('live-indicator')).not.toHaveTextContent('Live');
  });

  it('is not wired to a request of its own', () => {
    // The indicator reads the store the stream updates; it never polls the runtime itself.
    const getStatus = vi.spyOn(apiClient, 'getStatus');
    render(LiveIndicator);
    expect(getStatus).not.toHaveBeenCalled();
  });
});

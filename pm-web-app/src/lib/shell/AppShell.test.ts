import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import { createRawSnippet } from 'svelte';
import AppShell from './AppShell.svelte';
import { runtimeStore } from '../stores/runtime';
import { makeRuntimeState } from '../test/runtimeState';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * The frame's own gate: the command palette opens no page while the onboarding fills the app.
 *
 * The palette searches the pages of a machine that is set up; on the onboarding frame there are
 * none, so the frame does not mount it at all (PLAN-66 residue, D3).
 */
const content = createRawSnippet(() => ({ render: () => '<p>page</p>' }));

function arrange(isOnboarded: boolean): void {
  runtimeStore.set(
    makeRuntimeState({
      isInitialized: true,
      isPaired: true,
      isOnboarded,
      currentRoute: isOnboarded ? 'overview' : 'onboarding',
    }),
  );
}

describe('AppShell', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
    vi.restoreAllMocks();
  });

  it('opens no command palette while the onboarding fills the app', () => {
    arrange(false);

    render(AppShell, {
      props: {
        themeChoice: 'system',
        onChooseTheme: () => {},
        content,
      },
    });
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', metaKey: true }));

    expect(screen.queryByTestId('command-palette')).toBeNull();
  });

  it('opens the command palette once the machine is onboarded', async () => {
    arrange(true);

    render(AppShell, {
      props: {
        themeChoice: 'system',
        onChooseTheme: () => {},
        content,
      },
    });
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', metaKey: true }));

    expect(await screen.findByTestId('command-palette')).toBeInTheDocument();
  });
});

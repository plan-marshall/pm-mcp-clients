import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import CredentialEntry from './CredentialEntry.svelte';
import { apiClient } from '../api/client';

describe('CredentialEntry', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('stores a secret on the loopback connection', async () => {
    const setSpy = vi.spyOn(apiClient, 'setCredential').mockResolvedValue(undefined);

    render(CredentialEntry, { props: { credentialKey: 'github', label: 'GitHub' } });

    await fireEvent.input(screen.getByTestId('credential-secret-input'), {
      target: { value: 'ghp_secret' },
    });
    await fireEvent.click(screen.getByRole('button', { name: /store the secret/i }));

    await waitFor(() => expect(setSpy).toHaveBeenCalledWith('github', 'ghp_secret'));
    expect(screen.getByTestId('credential-notice')).toHaveTextContent('Stored on this machine.');
  });

  it('names the terminal command when the endpoint refuses with loopback_only', async () => {
    vi.spyOn(apiClient, 'setCredential').mockRejectedValue(new Error('loopback_only'));

    render(CredentialEntry, { props: { credentialKey: 'github', label: 'GitHub' } });

    await fireEvent.input(screen.getByTestId('credential-secret-input'), {
      target: { value: 'x' },
    });
    await fireEvent.click(screen.getByRole('button', { name: /store the secret/i }));

    await waitFor(() =>
      expect(screen.getByTestId('credential-error')).toHaveTextContent(
        'pm-operator credentials set github',
      ),
    );
  });
});

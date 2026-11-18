import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import AspectChoice from './AspectChoice.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

const STORES = [
  { kind: 'file', name: 'the file store', available: true },
  { kind: 'os', name: 'the keychain', available: true, recommended: true },
  { kind: 'vault', name: 'Vault', available: false },
];

const PROVIDERS = [
  { provider: 'github', name: 'GitHub', available: true, variant: 'gh', token_stored: true },
  { provider: 'gitlab', name: 'GitLab', available: false, cause: 'glab not found' },
  { provider: 'sonar', name: 'SonarCloud', available: true },
];

describe('AspectChoice', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  describe('store', () => {
    it('preselects the recommended store and names it on the button', () => {
      render(AspectChoice, { kind: 'store', stores: STORES });

      expect(screen.getByRole('radio', { name: 'the keychain' })).toBeChecked();
      expect(screen.getByText('recommended')).toBeVisible();
      expect(screen.getByRole('radio', { name: 'Vault' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Keep the tokens in the keychain' })).toBeVisible();
    });

    it('preselects the first store where none is recommended', () => {
      render(AspectChoice, { kind: 'store', stores: [STORES[0], STORES[2]] });

      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();
    });

    it('records the store the operator chose', async () => {
      const onRecord = vi.fn();
      render(AspectChoice, { kind: 'store', stores: STORES, onRecord });

      await fireEvent.change(screen.getByRole('radio', { name: 'the file store' }));
      await fireEvent.click(
        screen.getByRole('button', { name: 'Keep the tokens in the file store' }),
      );

      expect(onRecord).toHaveBeenCalledWith('file');
    });

    it('falls back to the first available store when an unavailable store is marked recommended', () => {
      render(AspectChoice, {
        kind: 'store',
        stores: [
          { kind: 'os', name: 'the keychain', available: false, recommended: true },
          { kind: 'file', name: 'the file store', available: true },
        ],
      });

      expect(screen.getByRole('radio', { name: 'the keychain' })).toBeDisabled();
      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();
      expect(screen.getByRole('button', { name: 'Keep the tokens in the file store' })).toBeEnabled();
    });

    it('disables the confirm button when no store is available', () => {
      render(AspectChoice, {
        kind: 'store',
        stores: [
          { kind: 'os', name: 'the keychain', available: false, recommended: true },
          { kind: 'file', name: 'the file store', available: false },
        ],
      });

      expect(screen.getByRole('radio', { name: 'the keychain' })).not.toBeChecked();
      expect(screen.getByRole('radio', { name: 'the file store' })).not.toBeChecked();
      const recordBtn = screen.getByTestId('store-record');
      expect(recordBtn).toBeDisabled();
      expect(recordBtn).toHaveTextContent('No store available');
    });

    it('resyncs the selected store when stores prop changes', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'store',
        stores: STORES,
      });
      await fireEvent.change(screen.getByRole('radio', { name: 'the file store' }));
      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();

      await rerender({
        kind: 'store',
        stores: [
          { kind: 'file', name: 'the file store', available: true },
          { kind: 'os', name: 'Secret Service', available: true, recommended: true },
        ],
      });

      expect(screen.getByRole('radio', { name: 'Secret Service' })).toBeChecked();
      expect(screen.getByTestId('store-record')).toHaveTextContent(
        'Keep the tokens in Secret Service',
      );
    });

    it('keeps a pending store choice when rerendered with identical rows in a new array', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'store',
        stores: STORES.map((s) => ({ ...s })),
      });
      await fireEvent.change(screen.getByRole('radio', { name: 'the file store' }));
      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();

      await rerender({
        kind: 'store',
        stores: STORES.map((s) => ({ ...s })),
      });

      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();
      expect(screen.getByTestId('store-record')).toHaveTextContent(
        'Keep the tokens in the file store',
      );
    });

    it('renders the generic other protection copy for non-os, non-file stores', () => {
      const en = loadMessages('en');
      const { container } = render(AspectChoice, {
        kind: 'store',
        stores: [{ kind: 'vault', name: 'Vault', available: true }],
      });

      const vaultOption = screen.getByRole('radio', { name: 'Vault' }).closest('label, li');
      expect(vaultOption ?? container).toHaveTextContent(en['onboard.store.choose.other.protects']);
      expect(vaultOption ?? container).toHaveTextContent(
        en['onboard.store.choose.other.does_not_protect'],
      );
      expect(container).not.toHaveTextContent(en['onboard.store.choose.file.protects']);
    });
  });

  describe('providers', () => {
    it('shows each provider with its variant, availability, token and cause', () => {
      const { container } = render(AspectChoice, { kind: 'providers', providers: PROVIDERS });

      expect(container.querySelector('[data-provider="github"]')).toHaveTextContent(
        'gh · available · token stored',
      );
      expect(container.querySelector('[data-provider="gitlab"]')).toHaveTextContent(
        'not available · glab not found',
      );
      expect(screen.getByRole('checkbox', { name: 'GitLab' })).toBeDisabled();
    });

    it('selects the available providers and records what the operator left on', async () => {
      const onRecord = vi.fn();
      render(AspectChoice, { kind: 'providers', providers: PROVIDERS, onRecord });
      expect(screen.getByRole('button', { name: 'Use 2 providers' })).toBeVisible();

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));
      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();
      await fireEvent.click(screen.getByRole('button', { name: 'Use 1 provider' }));
      expect(onRecord).toHaveBeenLastCalledWith(['github']);

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));
      await fireEvent.click(screen.getByRole('button', { name: 'Use 2 providers' }));
      expect(onRecord).toHaveBeenLastCalledWith(['github', 'sonar']);
    });

    it('resyncs selected providers when the providers prop changes', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'providers',
        providers: PROVIDERS,
      });

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));
      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();

      await rerender({
        kind: 'providers',
        providers: [
          { provider: 'gitlab', name: 'GitLab', available: true, variant: 'glab', token_stored: true },
        ],
      });

      expect(screen.getByRole('checkbox', { name: 'GitLab' })).toBeChecked();
      expect(screen.getByRole('button', { name: 'Use 1 provider' })).toBeVisible();
    });

    it('keeps pending provider selections when rerendered with identical rows in a new array', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'providers',
        providers: PROVIDERS.map((p) => ({ ...p })),
      });

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));
      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();

      await rerender({
        kind: 'providers',
        providers: PROVIDERS.map((p) => ({ ...p })),
      });

      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();
      expect(screen.getByRole('button', { name: 'Use 1 provider' })).toBeVisible();
    });
  });
});

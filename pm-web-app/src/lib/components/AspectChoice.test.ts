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
      expect(screen.getByRole('button', { name: 'Keep the tokens in the keychain' })).toBeVisible();
      for (const store of STORES) {
        const row = screen.getByRole('radio', { name: store.name }).closest('li') as HTMLElement;
        expect(row).toHaveTextContent('Protects a secret against: anything the store itself guarantees');
        expect(row).toHaveTextContent(
          'Does not protect against: anything the store itself does not guarantee',
        );
      }
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

      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();
      expect(screen.getByRole('button', { name: 'Keep the tokens in the file store' })).toBeEnabled();
    });

    it('keeps an unavailable OS store selectable and recordable as the way into store.unavailable', async () => {
      const onRecord = vi.fn();
      render(AspectChoice, {
        kind: 'store',
        stores: [
          { kind: 'file', name: 'the file store', available: true },
          { kind: 'os', name: 'GNOME Keyring', available: false },
        ],
        onRecord,
      });

      const os = screen.getByRole('radio', { name: 'GNOME Keyring' });
      expect(os).not.toBeDisabled();
      await fireEvent.change(os);
      expect(os).toBeChecked();

      const button = screen.getByTestId('store-record');
      expect(button).toBeEnabled();
      await fireEvent.click(button);
      expect(onRecord).toHaveBeenCalledWith('os');
    });

    it('disables the confirm button while no store is selected', () => {
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

    it('shows the generic protection line and no platform claim for a store without its own text', () => {
      render(AspectChoice, {
        kind: 'store',
        stores: [{ kind: 'os', name: 'GNOME Keyring', available: true, recommended: true }],
      });

      const row = screen.getByRole('radio', { name: 'GNOME Keyring' }).closest('li') as HTMLElement;
      expect(row).toHaveTextContent('Protects a secret against: anything the store itself guarantees');
      expect(row).toHaveTextContent(
        'Does not protect against: anything the store itself does not guarantee',
      );
      expect(row).not.toHaveTextContent('macOS');
      expect(row).not.toHaveTextContent('Secret Service');
      expect(row).not.toHaveTextContent('mode 0600');
    });

    it('shows a store\u2019s own protection text where the state carries one', () => {
      render(AspectChoice, {
        kind: 'store',
        stores: [
          {
            kind: 'os',
            name: 'the keychain',
            available: true,
            protects_against: 'a program you allow in the dialog',
            does_not_protect_against: 'a program you allow in the dialog',
          },
        ],
      });

      const row = screen
        .getByRole('radio', { name: 'the keychain' })
        .closest('li') as HTMLElement;
      expect(row).toHaveTextContent('Protects a secret against: a program you allow in the dialog');
      expect(row).toHaveTextContent('Does not protect against: a program you allow in the dialog');
    });

    it('associates both protection statements with each store radio via aria-describedby', () => {
      render(AspectChoice, { kind: 'store', stores: STORES });

      for (const store of STORES) {
        const radio = screen.getByRole('radio', { name: store.name });
        const describedBy = radio.getAttribute('aria-describedby') ?? '';
        const ids = describedBy.split(/\s+/).filter(Boolean);
        expect(ids).toHaveLength(2);
        const texts = ids.map((id) => document.getElementById(id)?.textContent?.trim() ?? '');
        expect(texts[0]).toMatch(/^Protects a secret against:/);
        expect(texts[1]).toMatch(/^Does not protect against:/);
      }
    });

    it('keeps the store the operator chose when the proposal changes', async () => {
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

      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();
      expect(screen.getByTestId('store-record')).toHaveTextContent(
        'Keep the tokens in the file store',
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

    it('says so when the chosen store is no longer offered', async () => {
      const { rerender, queryByTestId } = render(AspectChoice, {
        kind: 'store',
        stores: STORES.map((s) => ({ ...s })),
      });
      await fireEvent.change(screen.getByRole('radio', { name: 'the file store' }));
      expect(queryByTestId('store-dropped')).toBeNull();

      await rerender({
        kind: 'store',
        stores: [
          { kind: 'os', name: 'the keychain', available: true, recommended: true },
          { kind: 'vault', name: 'Vault', available: false },
        ],
      });

      expect(screen.getByTestId('store-dropped')).toHaveTextContent('the file store');
      expect(screen.getByRole('radio', { name: 'the keychain' })).toBeChecked();
    });

    it('moves the choice to the proposal when the chosen store is no longer available', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'store',
        stores: STORES.map((s) => ({ ...s })),
      });
      await fireEvent.change(screen.getByRole('radio', { name: 'the file store' }));
      expect(screen.getByRole('radio', { name: 'the file store' })).toBeChecked();

      await rerender({
        kind: 'store',
        stores: STORES.map((s) => (s.kind === 'file' ? { ...s, available: false } : { ...s })),
      });

      expect(screen.getByRole('radio', { name: 'the keychain' })).toBeChecked();
      expect(screen.getByTestId('store-record')).toHaveTextContent(
        'Keep the tokens in the keychain',
      );
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

    it('keeps the operator\u2019s ticks when the proposal changes', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'providers',
        providers: PROVIDERS,
      });

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));
      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();

      await rerender({
        kind: 'providers',
        providers: [
          ...PROVIDERS,
          { provider: 'forgejo', name: 'Forgejo', available: true },
        ],
      });

      expect(screen.getByRole('checkbox', { name: 'SonarCloud' })).not.toBeChecked();
      expect(screen.getByRole('checkbox', { name: 'Forgejo' })).toBeChecked();
      expect(screen.getByRole('button', { name: 'Use 2 providers' })).toBeVisible();
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

    it('says so when a provider the operator unticked is no longer offered', async () => {
      const { rerender } = render(AspectChoice, {
        kind: 'providers',
        providers: PROVIDERS,
      });

      await fireEvent.change(screen.getByRole('checkbox', { name: 'SonarCloud' }));

      await rerender({
        kind: 'providers',
        providers: [PROVIDERS[0], PROVIDERS[1]],
      });

      expect(screen.getByTestId('providers-dropped')).toHaveTextContent('SonarCloud');
      expect(screen.getByRole('button', { name: 'Use 1 provider' })).toBeVisible();
    });
  });
});

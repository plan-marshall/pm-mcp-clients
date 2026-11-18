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
      expect(screen.getByText('Recommendation by steward')).toBeVisible();
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
  });
});

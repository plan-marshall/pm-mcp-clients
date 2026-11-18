import { describe, it, expect, beforeEach } from 'vitest';
import { tick } from 'svelte';
import { fireEvent, render, screen } from '@testing-library/svelte';
import ModelsChoice from './ModelsChoice.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

describe('ModelsChoice', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('resyncs selections and drafts when models or harnesses props change', async () => {
    const { rerender, queryByTestId, getByTestId } = render(ModelsChoice, {
      props: {
        models: [{ harness_id: 'claude', model_id: 'opus-4.6', source: 'probe', recommended: true }],
        harnesses: [{ harness_id: 'opencode', name: 'OpenCode', cannot_list: true }],
      },
    });

    await fireEvent.input(getByTestId('models-input-opencode'), { target: { value: 'draft-id' } });
    expect((getByTestId('models-input-opencode') as HTMLInputElement).value).toBe('draft-id');

    await rerender({
      models: [{ harness_id: 'codex', model_id: 'gpt-5', source: 'probe' }],
      harnesses: [{ harness_id: 'gemini', name: 'Gemini', cannot_list: true }],
    });

    expect(queryByTestId('model-toggle-claude-opus-4.6')).toBeNull();
    expect(queryByTestId('models-input-opencode')).toBeNull();
    expect(getByTestId('model-toggle-codex-gpt-5')).toBeChecked();
    expect((getByTestId('models-input-gemini') as HTMLInputElement).value).toBe('');
  });

  it('gives each operator Remove button a model-specific accessible name and updates token figures on locale switch', async () => {
    try {
      const { container } = render(ModelsChoice, {
        props: {
          models: [
            { harness_id: 'opencode', model_id: 'model-a', source: 'operator', tokens: 2000 },
            { harness_id: 'opencode', model_id: 'model-b', source: 'operator', tokens_floor: true, tokens: 5000 },
          ],
          harnesses: [{ harness_id: 'opencode', cannot_list: true }],
        },
      });

      expect(screen.getByRole('button', { name: 'Remove model-a' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Remove model-b' })).toBeVisible();
      expect(container).toHaveTextContent('model-b');

      currentLocale.set('de');
      messagesStore.set(loadMessages('de'));
      await tick();

      expect(screen.getByRole('button', { name: 'model-a entfernen' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'model-b entfernen' })).toBeVisible();
    } finally {
      currentLocale.set('en');
      messagesStore.set(loadMessages('en'));
    }
  });
});

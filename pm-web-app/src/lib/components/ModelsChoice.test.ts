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

      expect(screen.getByRole('button', { name: 'Remove model-a from OpenCode' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Remove model-b from OpenCode' })).toBeVisible();
      expect(container).toHaveTextContent('2.0 k');
      expect(container).toHaveTextContent('≥ 5.0 k');

      currentLocale.set('de');
      messagesStore.set(loadMessages('de'));
      await tick();

      expect(screen.getByRole('button', { name: 'model-a bei OpenCode entfernen' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'model-b bei OpenCode entfernen' })).toBeVisible();
      expect(container).toHaveTextContent('2,0 Tsd.');
      expect(container).toHaveTextContent('≥ 5,0 Tsd.');
    } finally {
      currentLocale.set('en');
      messagesStore.set(loadMessages('en'));
    }
  });

  it('gives duplicate model ids in different harnesses distinct Remove accessible names', () => {
    render(ModelsChoice, {
      props: {
        models: [
          { harness_id: 'claude', model_id: 'shared-model', source: 'operator' },
          { harness_id: 'opencode', model_id: 'shared-model', source: 'operator' },
        ],
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code', cannot_list: true },
          { harness_id: 'opencode', name: 'OpenCode', cannot_list: true },
        ],
      },
    });

    expect(screen.getByRole('button', { name: 'Remove shared-model from Claude Code' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Remove shared-model from OpenCode' })).toBeVisible();
  });

  it('shows an enrolled models_listed harness with zero models and shows no model allowed when all models are unchecked', async () => {
    const { container, getByTestId } = render(ModelsChoice, {
      props: {
        models: [{ harness_id: 'claude', model_id: 'opus-4.6', source: 'probe' }],
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code', models_listed: true },
          { harness_id: 'codex', name: 'Codex', models_listed: true },
        ],
      },
    });

    const codexGroup = container.querySelector('[data-harness="codex"]');
    expect(codexGroup).not.toBeNull();
    expect(codexGroup).toHaveTextContent('no model allowed');
    expect(container.querySelector('[data-testid="models-input-codex"]')).toBeNull();

    await fireEvent.click(getByTestId('model-toggle-claude-opus-4.6'));
    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    expect(getByTestId('models-none-claude')).toHaveTextContent('no model allowed');
  });

  it('preserves operator edits when rerendered with identical proposal content in new arrays', async () => {
    const models = [{ harness_id: 'claude', model_id: 'opus-4.6', source: 'probe' }];
    const harnesses = [
      { harness_id: 'claude', name: 'Claude Code', models_listed: true },
      { harness_id: 'opencode', name: 'OpenCode', cannot_list: true },
    ];
    const { rerender, getByTestId } = render(ModelsChoice, {
      props: {
        models: models.map((m) => ({ ...m })),
        harnesses: harnesses.map((h) => ({ ...h })),
        recommendation: 'Pick Opus',
      },
    });

    await fireEvent.click(getByTestId('model-toggle-claude-opus-4.6'));
    await fireEvent.input(getByTestId('models-input-opencode'), { target: { value: 'qwen-32b' } });
    await fireEvent.click(getByTestId('models-add-opencode'));
    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    expect(getByTestId('model-toggle-opencode-qwen-32b')).toBeChecked();

    await rerender({
      models: models.map((m) => ({ ...m })),
      harnesses: harnesses.map((h) => ({ ...h })),
      recommendation: 'Pick Opus',
    });

    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    expect(getByTestId('model-toggle-opencode-qwen-32b')).toBeChecked();
  });
});

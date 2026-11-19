import { describe, it, expect, beforeEach } from 'vitest';
import { tick } from 'svelte';
import { fireEvent, render, screen, within } from '@testing-library/svelte';
import ModelsChoice from './ModelsChoice.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';

/**
 * @param over the fields to override
 * @returns one published `catalogue.select` row
 */
function model(over: Record<string, unknown>) {
  return { harness_id: 'claude', model_id: 'opus-4.6', source: 'probe', ...over };
}

describe('ModelsChoice', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('offers a text field to a harness with no listed row when the state carries no harnesses[]', () => {
    render(ModelsChoice, {
      props: {
        models: [],
        harnesses: [],
        harnessNames: { opencode: 'OpenCode' },
      },
    });

    const group = screen.getByRole('group', { name: 'OpenCode' });
    const input = within(group).getByTestId('models-input-opencode') as HTMLInputElement;
    expect(input).toBeVisible();
    expect(input.id).toBeDefined();
  });

  it('builds the groups from the harnesses the state names, not only from harnesses[]', () => {
    const { container } = render(ModelsChoice, {
      props: {
        models: [model({ harness_id: 'codex', model_id: 'gpt-5' })],
        harnesses: [{ harness_id: 'claude', name: 'Claude Code' }],
        defaultHarness: 'opencode',
      },
    });

    expect(container.querySelector('[data-harness="claude"]')).not.toBeNull();
    expect(container.querySelector('[data-harness="codex"]')).not.toBeNull();
    expect(container.querySelector('[data-harness="opencode"]')).not.toBeNull();
  });

  it('treats two ids that differ only in case as one row and says why a second add is ignored', async () => {
    render(ModelsChoice, {
      props: {
        models: [],
        harnesses: [{ harness_id: 'opencode', name: 'OpenCode' }],
      },
    });

    const input = screen.getByTestId('models-input-opencode');
    await fireEvent.input(input, { target: { value: 'Opus-5.5' } });
    await fireEvent.click(screen.getByTestId('models-add-opencode'));
    expect(screen.getByTestId('model-toggle-opencode-Opus-5.5')).toBeChecked();

    await fireEvent.input(input, { target: { value: 'opus-5.5' } });
    await fireEvent.click(screen.getByTestId('models-add-opencode'));

    expect(screen.getAllByTestId(/^model-toggle-opencode-/)).toHaveLength(1);
    expect(screen.getByTestId('models-add-notice-opencode')).toHaveTextContent(
      'That model id is already listed for this harness.',
    );
  });

  it('keeps an id carrying the row separator apart from another harness row', async () => {
    const { container } = render(ModelsChoice, {
      props: {
        models: [],
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code' },
          { harness_id: 'claude::x', name: 'Other' },
        ],
      },
    });

    await fireEvent.input(within(container.querySelector('[data-harness="claude"]') as HTMLElement).getByTestId('models-input-claude'), {
      target: { value: 'x::y' },
    });
    await fireEvent.click(screen.getByTestId('models-add-claude'));
    await fireEvent.input(
      within(container.querySelector('[data-harness="claude::x"]') as HTMLElement).getByTestId(
        'models-input-claude::x',
      ),
      { target: { value: 'y' } },
    );
    await fireEvent.click(screen.getByTestId('models-add-claude::x'));

    expect(screen.getByTestId('model-toggle-claude-x::y')).toBeChecked();
    expect(screen.getByTestId('model-toggle-claude::x-y')).toBeChecked();
  });

  it('adds nothing half-typed while an IME composition is open and says why an empty add is ignored', async () => {
    render(ModelsChoice, {
      props: {
        models: [],
        harnesses: [{ harness_id: 'opencode', name: 'OpenCode' }],
      },
    });

    const input = screen.getByTestId('models-input-opencode');
    await fireEvent.input(input, { target: { value: 'qwen' } });
    await fireEvent.keyDown(input, { key: 'Enter', isComposing: true });
    expect(screen.queryByTestId('model-toggle-opencode-qwen')).toBeNull();

    await fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('model-toggle-opencode-qwen')).toBeChecked();

    await fireEvent.input(input, { target: { value: '   ' } });
    await fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('models-add-notice-opencode')).toHaveTextContent(
      'Type a model id first.',
    );
  });

  it('names each checkbox by its model and its harness, and ties it to its group', () => {
    render(ModelsChoice, {
      props: {
        models: [
          model({ harness_id: 'claude', model_id: 'shared-model', source: 'operator' }),
          model({ harness_id: 'opencode', model_id: 'shared-model', source: 'operator' }),
        ],
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code' },
          { harness_id: 'opencode', name: 'OpenCode' },
        ],
      },
    });

    const claudeGroup = screen.getByRole('group', { name: 'Claude Code' });
    const opencodeGroup = screen.getByRole('group', { name: 'OpenCode' });
    expect(within(claudeGroup).getByRole('checkbox', { name: 'shared-model on Claude Code' })).toBeVisible();
    expect(within(opencodeGroup).getByRole('checkbox', { name: 'shared-model on OpenCode' })).toBeVisible();
  });

  it('gives duplicate model ids in different harnesses distinct Remove accessible names', () => {
    render(ModelsChoice, {
      props: {
        models: [
          model({ harness_id: 'claude', model_id: 'shared-model', source: 'operator' }),
          model({ harness_id: 'opencode', model_id: 'shared-model', source: 'operator' }),
        ],
        harnesses: [
          { harness_id: 'claude', name: 'Claude Code' },
          { harness_id: 'opencode', name: 'OpenCode' },
        ],
      },
    });

    expect(screen.getByRole('button', { name: 'Remove shared-model from Claude Code' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Remove shared-model from OpenCode' })).toBeVisible();
  });

  it('reads no allowed flag: every listed row is preselected where the proposal marks none', () => {
    render(ModelsChoice, {
      props: {
        models: [model({ model_id: 'opus-5.5', allowed: false })],
      },
    });

    expect(screen.getByTestId('model-toggle-claude-opus-5.5')).toBeChecked();
  });

  it('reads no models_listed or cannot_list flag: a group without a probed row offers the field', () => {
    // The state may carry facts no source publishes; the screen must ignore them.
    const harnesses = [
      { harness_id: 'codex', name: 'Codex', models_listed: true, cannot_list: false },
    ] as unknown as { harness_id: string; name: string }[];
    render(ModelsChoice, {
      props: {
        models: [],
        harnesses,
      },
    });

    expect(screen.getByTestId('models-input-codex')).toBeVisible();
  });

  it('reads no display_name or harness_name: the group is named by the published name or the id', () => {
    const harnesses = [
      { harness_id: 'claude', name: 'Claude Profile', display_name: 'Profile Custom' },
    ] as unknown as { harness_id: string; name: string }[];
    render(ModelsChoice, {
      props: {
        models: [model({ harness_id: 'custom', model_id: 'small-1', source: 'operator', harness_name: 'Row Name' })],
        harnesses,
      },
    });

    expect(screen.getByRole('heading', { name: 'Claude Profile' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Profile Custom' })).toBeNull();
    expect(screen.getByRole('heading', { name: 'Custom' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Row Name' })).toBeNull();
  });

  it('reads no per-row reason or rationale', () => {
    const { container } = render(ModelsChoice, {
      props: {
        models: [
          model({ model_id: 'opus-5.5', reason: 'Best for planning and review', rationale: 'Best for planning' }),
        ],
      },
    });

    expect(container).not.toHaveTextContent('Best for planning');
  });

  it('keeps added models, unticked rows and drafts across a refresh that changes the proposal', async () => {
    const harnesses = [
      { harness_id: 'claude', name: 'Claude Code' },
      { harness_id: 'opencode', name: 'OpenCode' },
    ];
    const { rerender, getByTestId } = render(ModelsChoice, {
      props: {
        models: [model({ harness_id: 'claude', model_id: 'opus-4.6', recommended: true })],
        harnesses: harnesses.map((h) => ({ ...h })),
      },
    });

    await fireEvent.click(getByTestId('model-toggle-claude-opus-4.6'));
    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    await fireEvent.input(getByTestId('models-input-opencode'), { target: { value: 'qwen-32b' } });
    await fireEvent.click(getByTestId('models-add-opencode'));
    await fireEvent.input(getByTestId('models-input-opencode'), { target: { value: 'draft-id' } });

    await rerender({
      models: [
        model({ harness_id: 'claude', model_id: 'opus-4.6', recommended: true }),
        model({ harness_id: 'claude', model_id: 'haiku-4.5', recommended: false }),
      ],
      harnesses: harnesses.map((h) => ({ ...h })),
    });

    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    expect(getByTestId('model-toggle-opencode-qwen-32b')).toBeChecked();
    expect((getByTestId('models-input-opencode') as HTMLInputElement).value).toBe('draft-id');
  });

  it('says so when a refresh removes a row the operator had changed', async () => {
    const { rerender, getByTestId, queryByTestId } = render(ModelsChoice, {
      props: {
        models: [
          model({ harness_id: 'claude', model_id: 'opus-4.6', recommended: true }),
          model({ harness_id: 'claude', model_id: 'sonnet-4.6' }),
        ],
      },
    });

    await fireEvent.click(getByTestId('model-toggle-claude-opus-4.6'));
    expect(queryByTestId('models-dropped')).toBeNull();

    await rerender({
      models: [model({ harness_id: 'claude', model_id: 'sonnet-4.6' })],
    });

    expect(getByTestId('models-dropped')).toHaveTextContent('opus-4.6');
    expect(queryByTestId('model-toggle-claude-opus-4.6')).toBeNull();
  });

  it('preserves operator edits when rerendered with identical proposal content in new arrays', async () => {
    const models = [model({ harness_id: 'claude', model_id: 'opus-4.6' })];
    const harnesses = [
      { harness_id: 'claude', name: 'Claude Code' },
      { harness_id: 'opencode', name: 'OpenCode' },
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

  it('gives each operator Remove button a model-specific accessible name and updates token figures on locale switch', async () => {
    try {
      const { container } = render(ModelsChoice, {
        props: {
          models: [
            model({ harness_id: 'opencode', model_id: 'model-a', source: 'operator', tokens: 2000 }),
            model({
              harness_id: 'opencode',
              model_id: 'model-b',
              source: 'operator',
              tokens_floor: true,
              tokens: 5000,
            }),
          ],
          harnesses: [{ harness_id: 'opencode', name: 'OpenCode' }],
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

  it('shows no model allowed when all models of a group are unchecked', async () => {
    const { container, getByTestId } = render(ModelsChoice, {
      props: {
        models: [model({ harness_id: 'claude', model_id: 'opus-4.6' })],
        harnesses: [{ harness_id: 'claude', name: 'Claude Code' }],
      },
    });

    await fireEvent.click(getByTestId('model-toggle-claude-opus-4.6'));
    expect(getByTestId('model-toggle-claude-opus-4.6')).not.toBeChecked();
    expect(getByTestId('models-none-claude')).toHaveTextContent('no model allowed');
    expect(container.querySelector('[data-harness="claude"]')).toHaveTextContent('no model allowed');
  });

  it('preselects recommended models even when sibling rows omit recommended', () => {
    render(ModelsChoice, {
      props: {
        models: [
          model({ model_id: 'opus-4.6', recommended: true }),
          model({ model_id: 'haiku-4.5' }),
        ],
      },
    });

    expect(screen.getByTestId('model-toggle-claude-opus-4.6')).toBeChecked();
    expect(screen.getByTestId('model-toggle-claude-haiku-4.5')).not.toBeChecked();
    expect(screen.getByTestId('models-record')).toHaveTextContent('Allow 1 model');
  });

  it('handles a harness id named constructor with zero or listed models without inheriting Object prototype', async () => {
    const { container, rerender, getByTestId } = render(ModelsChoice, {
      props: {
        harnesses: [{ harness_id: 'constructor', name: 'Constructor' }],
        models: [],
      },
    });

    expect(container.querySelector('[data-harness="constructor"]')).toHaveTextContent(
      'no model allowed',
    );
    expect((getByTestId('models-input-constructor') as HTMLInputElement).value).toBe('');

    await rerender({
      harnesses: [{ harness_id: 'constructor', name: 'Constructor' }],
      models: [model({ harness_id: 'constructor', model_id: 'c-1' })],
    });

    expect(getByTestId('model-toggle-constructor-c-1')).toBeChecked();
  });
});

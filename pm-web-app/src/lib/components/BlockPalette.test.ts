import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import BlockPalette from './BlockPalette.svelte';
import type { WorkflowCatalogue } from '../api/client';

const catalogue: WorkflowCatalogue = {
  scope: 'pm-mcp-clients',
  runtime_version: '0.9.0',
  digest: 'sha256:cat',
  blocks: [
    {
      kind: 'keyword',
      name: 'state',
      summary: 'A state of the unit.',
      template: 'state <name>',
      parameters: [],
      constraints: [],
      outcomes: [],
      used_in: ['any unit body'],
      compiled_only: false,
    },
    {
      kind: 'sub_workflow',
      name: 'git.commit_and_push',
      summary: 'Commits the changes and pushes the branch.',
      template: 'call git.commit_and_push entry commit',
      parameters: ['message_mode enum'],
      constraints: ['a push remote'],
      outcomes: ['done (success)', 'stepped_out'],
      used_in: ['the call clause of a state'],
      compiled_only: false,
    },
  ],
};

describe('BlockPalette component', () => {
  it('groups the blocks by kind and counts each group', () => {
    render(BlockPalette, { props: { catalogue } });

    expect(screen.getByTestId('block-group-keyword')).toHaveTextContent('keywords and clauses');
    expect(screen.getByTestId('block-group-sub_workflow')).toHaveTextContent('sub-workflows');
    expect(screen.getByTestId('block-select-state')).toBeInTheDocument();
    expect(screen.getByTestId('blocks-scope')).toHaveTextContent('Building blocks of pm-mcp-clients');
  });

  it('filters the blocks by the search query and reports no match', async () => {
    render(BlockPalette, { props: { catalogue } });

    await fireEvent.input(screen.getByTestId('blocks-search'), { target: { value: 'commit' } });
    expect(screen.getByTestId('block-select-git.commit_and_push')).toBeInTheDocument();
    expect(screen.queryByTestId('block-select-state')).not.toBeInTheDocument();

    await fireEvent.input(screen.getByTestId('blocks-search'), { target: { value: 'zzz' } });
    expect(screen.getByTestId('blocks-no-match')).toHaveTextContent('Nothing matches "zzz".');
  });

  it('calls onInsert with the block when Insert is clicked', async () => {
    const onInsert = vi.fn();
    render(BlockPalette, { props: { catalogue, onInsert } });

    await fireEvent.click(screen.getByTestId('block-insert-git.commit_and_push'));

    expect(onInsert).toHaveBeenCalledTimes(1);
    expect(onInsert.mock.calls[0][0].name).toBe('git.commit_and_push');
  });

  it('shows the detail of a selected block with its template', async () => {
    const onSelect = vi.fn();
    render(BlockPalette, { props: { catalogue, onSelect } });

    await fireEvent.click(screen.getByTestId('block-select-git.commit_and_push'));

    expect(onSelect).toHaveBeenCalledWith(catalogue.blocks[1]);
    expect(screen.queryByTestId('block-detail')).not.toBeInTheDocument();
  });
});

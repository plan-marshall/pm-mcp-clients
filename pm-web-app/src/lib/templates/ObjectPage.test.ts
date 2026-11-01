import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import path from 'node:path';
import ObjectPageFixture from '../../test/fixtures/ObjectPageFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `ObjectPage` (UX-P-03, template B): the head with kind, path, title, id, state and the acts, an
 * attention strip when the state asks for something, the tabs and the body. The page is capped at
 * 1440 px and centred.
 */

const SOURCE = path.resolve(__dirname, './ObjectPage.svelte');

const PROPS = {
  kind: 'Plan',
  path: 'pm-mcp-clients / PLAN-43',
  title: 'Web: Four Page Templates',
  idChip: 'PLAN-43',
  state: 'Running, three tasks left',
  primaryAct: 'Open the decision',
  secondaryAct: 'Chat',
  overflowAct: 'More',
  attention: 'Waiting for your answer',
  tabs: 'Overview · Tasks',
  body: 'Tabs body',
};

describe('ObjectPage', () => {
  it('renders every part of the head through its slots', () => {
    render(ObjectPageFixture, { props: PROPS });

    expect(screen.getByTestId('object-page-kind')).toHaveTextContent('Plan');
    expect(screen.getByTestId('object-page-path')).toHaveTextContent('pm-mcp-clients / PLAN-43');
    expect(
      screen.getByRole('heading', { name: 'Web: Four Page Templates', level: 1 }),
    ).toBeInTheDocument();
    expect(screen.getByTestId('object-page-id')).toHaveTextContent('PLAN-43');
    expect(screen.getByTestId('object-page-state')).toHaveTextContent('Running, three tasks left');
    expect(screen.getByRole('button', { name: 'Open the decision' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chat' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'More' })).toBeInTheDocument();
  });

  it('renders the attention strip, the tabs and the body through their slots', () => {
    render(ObjectPageFixture, { props: PROPS });

    expect(screen.getByTestId('object-page-attention')).toHaveTextContent(
      'Waiting for your answer',
    );
    expect(screen.getByTestId('object-page-tabs')).toHaveTextContent('Overview · Tasks');
    expect(screen.getByTestId('object-page-body')).toHaveTextContent('Tabs body');
  });

  it('caps the page at 1440 px and centres it', () => {
    const root = ruleOf(styleOf(SOURCE), '.object-page');

    expect(root).toContain('max-width: 1440px');
    expect(root).toContain('margin-inline: auto');
    expect(root).toContain('width: 100%');
  });
});

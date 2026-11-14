import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import fs from 'node:fs';
import path from 'node:path';
import ObjectPageFixture from '../../test/fixtures/ObjectPageFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `ObjectPage` (UX-P-03, template B): the head with kind, title, id, state and the acts, an
 * attention strip when the state asks for something, the tabs and the body. The page is capped at
 * 1440 px and centred on a page of its own; inside a column engine its `compact` form draws the head
 * of a list column only, with no cap and no path.
 */

const SOURCE = path.resolve(__dirname, './ObjectPage.svelte');

const PROPS = {
  kind: 'Plan',
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

  it('draws the compact head of a list column, and nothing below it', () => {
    render(ObjectPageFixture, { props: { ...PROPS, compact: true } });

    expect(screen.getByTestId('object-page-kind')).toHaveTextContent('Plan');
    expect(
      screen.getByRole('heading', { name: 'Web: Four Page Templates', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByTestId('object-page-state')).toHaveTextContent('Running, three tasks left');
    // The main act is the view's secondary button; the id chip, the strip, the tabs and the body go.
    expect(screen.getByRole('button', { name: 'Open the decision' })).toBeInTheDocument();
    expect(screen.queryByTestId('object-page-id')).toBeNull();
    expect(screen.queryByTestId('object-page-attention')).toBeNull();
    expect(screen.queryByTestId('object-page-tabs')).toBeNull();
    expect(screen.queryByTestId('object-page-body')).toBeNull();
  });

  it('caps a page of its own at 1440 px and centres it, with no cap inside a column', () => {
    const style = styleOf(SOURCE);

    expect(ruleOf(style, '.object-page')).toContain('max-width: 1440px');
    expect(ruleOf(style, '.object-page')).toContain('margin-inline: auto');
    expect(ruleOf(style, '.object-page--compact')).toContain('max-width: none');
  });

  it('carries no path slot and no side column of its own', () => {
    const source = fs.readFileSync(SOURCE, 'utf-8');

    expect(source).not.toContain('name="path"');
    expect(source).not.toContain('object-page-path');
    expect(source).not.toContain('side');
  });

  it('adds no second surface around the strip and the tabs', () => {
    const style = styleOf(SOURCE);

    expect(style).not.toMatch(/\.object-page__attention\s*\{/);
    expect(style).not.toMatch(/\.object-page__tabs\s*\{/);
  });

  it('takes the 12 px inset on the phone', () => {
    const style = styleOf(SOURCE);
    const index = style.indexOf('@media (max-width: 480px)');

    expect(index).toBeGreaterThanOrEqual(0);
    expect(style.slice(index)).toMatch(/padding: var\(--space-3\)/);
  });
});

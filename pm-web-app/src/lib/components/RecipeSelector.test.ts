import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import RecipeSelector from './RecipeSelector.svelte';
import type { RecipeRef } from '../api/client';

describe('RecipeSelector component', () => {
  const recipes: RecipeRef[] = [
    {
      key: 'code-review',
      name: 'code-review',
      description: 'a structural review',
      origin: 'domain',
      takes_request: false,
      parameters: [
        {
          name: 'depth',
          value: 'standard',
          options: ['quick', 'standard', 'deep'],
          proposed: true,
          available: true,
        },
      ],
    },
    {
      key: 'audit',
      name: 'audit',
      description: 'audit against a request',
      origin: 'project',
      takes_request: true,
      parameters: [],
    },
    {
      key: 'no-modules',
      name: 'no-modules',
      description: 'a recipe without an option',
      origin: 'project',
      takes_request: false,
      parameters: [
        {
          name: 'modules',
          options: [],
          proposed: false,
          available: false,
          reason: 'none the recipe can work on',
        },
      ],
    },
  ];

  it('lists the offered recipes', () => {
    render(RecipeSelector, { props: { projectId: 'pm-mcp-clients', recipes } });

    const select = screen.getByTestId('recipe-select') as HTMLSelectElement;
    expect(select.querySelectorAll('option')).toHaveLength(4);
    expect(select.textContent).toContain('code-review');
    expect(select.textContent).toContain('audit');
  });

  it('emits the chosen recipe with its proposed parameter values', async () => {
    const onSelect = vi.fn();
    render(RecipeSelector, { props: { projectId: 'pm-mcp-clients', recipes, onSelect } });

    await fireEvent.change(screen.getByTestId('recipe-select'), {
      target: { value: 'code-review' },
    });

    expect(onSelect).toHaveBeenCalledTimes(1);
    const [recipe, values] = onSelect.mock.calls[0];
    expect(recipe.key).toBe('code-review');
    expect(values).toEqual({ depth: 'standard' });
    expect(screen.getByTestId('recipe-proposed-depth')).toHaveTextContent('proposed');
  });

  it('shows the reason of a parameter without an option', async () => {
    render(RecipeSelector, { props: { projectId: 'pm-mcp-clients', recipes } });

    await fireEvent.change(screen.getByTestId('recipe-select'), {
      target: { value: 'no-modules' },
    });

    expect(screen.getByTestId('recipe-no-option-modules')).toHaveTextContent(
      'none the recipe can work on',
    );
  });

  it('shows the empty message when no recipe is offered', () => {
    render(RecipeSelector, { props: { projectId: 'pm-mcp-clients', recipes: [] } });
    expect(screen.getByTestId('recipe-empty')).toHaveTextContent('No recipe is offered');
  });
});

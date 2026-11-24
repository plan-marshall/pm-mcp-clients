import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import RecipeReadyView from './RecipeReadyView.svelte';

/**
 * The recipe and ready steps as the view fills them: the parameters the server resolved, the
 * proposed title and derived id, and the one Enter that confirms the field. The step's own act is
 * the wizard's, so the view draws none.
 */

const RECIPE = {
  key: 'free',
  name: 'free',
  description: 'A recipe the project offers',
  origin: 'project',
  takes_request: false,
  parameters: [],
};

describe('RecipeReadyView', () => {
  it('draws a parameter with options as a choice and one without options as its reason, with no field', () => {
    render(RecipeReadyView, {
      props: {
        mode: 'parameters',
        recipe: RECIPE,
        parameters: [
          {
            name: 'module',
            options: ['pm-api', 'pm-relay'],
            value: 'pm-api',
            proposed: false,
            available: true,
          },
          { name: 'target', options: [], proposed: false, available: true },
        ],
      },
    });

    expect(screen.getByTestId('parameter-module')).toBeInTheDocument();
    expect(screen.queryByTestId('parameter-target')).toBeNull();
    expect(screen.getByTestId('no-option-target')).toHaveTextContent(
      'target: none the recipe can work on; the recipe cannot run',
    );
  });

  it('draws no act of its own: the step has one primary, and it is the wizard footer’s', () => {
    render(RecipeReadyView, {
      props: {
        mode: 'ready',
        recipe: RECIPE,
        title: 'Review relay',
        planId: 'review-relay',
        onSubmit: vi.fn(),
      },
    });

    expect(screen.getByTestId('ready-title')).toHaveValue('Review relay');
    expect(screen.getByTestId('ready-id')).toHaveValue('review-relay');
    expect(screen.queryByTestId('ready-start-act')).toBeNull();
    expect(screen.getByTestId('recipe-ready').querySelector('.btn--primary')).toBeNull();
  });

  it('submits on Enter through the shared guard, and sends nothing on an IME confirm', async () => {
    const onSubmit = vi.fn();
    render(RecipeReadyView, {
      props: {
        mode: 'ready',
        recipe: RECIPE,
        title: 'Review relay',
        planId: 'review-relay',
        onSubmit,
      },
    });
    const title = screen.getByTestId('ready-title');

    await fireEvent.keyDown(title, { key: 'Enter', isComposing: true });
    await fireEvent.keyDown(title, { key: 'Enter', keyCode: 229 });
    await fireEvent.keyDown(title, { key: 'Enter', shiftKey: true });

    expect(onSubmit).not.toHaveBeenCalled();

    await fireEvent.keyDown(title, { key: 'Enter' });

    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import RecipeReadyView from './RecipeReadyView.svelte';

describe('RecipeReadyView', () => {
  it('renders parameters through SelectField and TextField in parameters mode, and submits with Enter', async () => {
    const onParameter = vi.fn();
    const onSubmit = vi.fn();

    render(RecipeReadyView, {
      props: {
        mode: 'parameters',
        recipe: {
          key: 'code-review',
          name: 'code-review',
          description: 'A code review recipe',
          origin: 'builtin',
          takes_request: false,
          parameters: [],
        },
        parameters: [
          {
            name: 'module',
            options: ['pm-api', 'pm-relay'],
            value: 'pm-api',
            proposed: false,
            available: true,
          },
          {
            name: 'target',
            options: [],
            value: 'src/main',
            proposed: false,
            available: true,
          },
        ],
        onParameter,
        onSubmit,
      },
    });

    // SelectField for module
    const select = screen.getByTestId('parameter-module');
    expect(select).toBeInTheDocument();
    await fireEvent.change(select, { target: { value: 'pm-relay' } });
    expect(onParameter).toHaveBeenCalledWith('module', 'pm-relay');

    // TextField for target (a parameter without fixed options)
    const targetInput = screen.getByTestId('parameter-target');
    expect(targetInput).toBeInTheDocument();
    await fireEvent.input(targetInput, { target: { value: 'src/core' } });
    expect(onParameter).toHaveBeenCalledWith('target', 'src/core');

    // Pressing Enter while IME is composing does NOT submit
    await fireEvent.keyDown(targetInput, { key: 'Enter', isComposing: true });
    expect(onSubmit).not.toHaveBeenCalled();

    // Submitting parameter with ordinary Enter
    await fireEvent.keyDown(targetInput, { key: 'Enter' });
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it('renders ready mode with TextField for title and id, and primary button naming the act', async () => {
    const onTitle = vi.fn();
    const onId = vi.fn();
    const onSubmit = vi.fn();

    render(RecipeReadyView, {
      props: {
        mode: 'ready',
        recipe: {
          key: 'code-review',
          name: 'code-review',
          description: 'A code review recipe',
          origin: 'builtin',
          takes_request: false,
          parameters: [],
        },
        title: 'Review relay code',
        planId: 'review-relay',
        onSubmit,
        onTitle,
        onId,
      },
    });

    const titleInput = screen.getByTestId('ready-title');
    expect(titleInput).toBeInTheDocument();
    await fireEvent.input(titleInput, { target: { value: 'Review relay and api code' } });
    expect(onTitle).toHaveBeenCalledWith('Review relay and api code');

    const idInput = screen.getByTestId('ready-id');
    expect(idInput).toBeInTheDocument();

    // Primary button naming the act
    const submitBtn = screen.getByTestId('ready-start-act');
    expect(submitBtn).toHaveClass('btn--primary');
    await fireEvent.click(submitBtn);
    expect(onSubmit).toHaveBeenCalled();

    // Pressing Enter while IME is composing does NOT submit
    await fireEvent.keyDown(titleInput, { key: 'Enter', isComposing: true });
    expect(onSubmit).toHaveBeenCalledTimes(1);

    // Pressing Enter in title submits
    await fireEvent.keyDown(titleInput, { key: 'Enter' });
    expect(onSubmit).toHaveBeenCalledTimes(2);
  });
});

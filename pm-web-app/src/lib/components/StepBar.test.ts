import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import StepBar from './StepBar.svelte';

/**
 * StepBar (components.adoc § #supporting).
 *
 * Tests the step bar of the onboarding: renders steps, marks completed and active,
 * and allows opening done steps read-only.
 */

describe('StepBar', () => {
  it('renders all 7 onboarding steps with navigation aria-label', () => {
    render(StepBar, { props: { activeStep: 1 } });
    const nav = screen.getByRole('navigation');
    expect(nav).toBeInTheDocument();

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(8);
  });

  it('marks active step and done steps correctly', () => {
    const { container } = render(StepBar, {
      props: { activeStep: 3, completedSteps: [1, 2] },
    });

    const items = container.querySelectorAll('.step-bar__item');
    expect(items[0]).toHaveClass('step-bar__item--done');
    expect(items[1]).toHaveClass('step-bar__item--done');
    expect(items[2]).toHaveClass('step-bar__item--active');
  });

  it('invokes onSelectStep when clicking a done step button', async () => {
    const onSelectStep = vi.fn();
    render(StepBar, {
      props: { activeStep: 3, completedSteps: [1, 2], onSelectStep },
    });

    const button1 = screen.getByTestId('step-button-1');
    await fireEvent.click(button1);

    expect(onSelectStep).toHaveBeenCalledWith(1);
  });
});

import { describe, it, expect } from 'vitest';
import { render, fireEvent, screen } from '@testing-library/svelte';
import DemoFormFixture from './DemoFormFixture.svelte';

describe('DemoFormFixture keyboard walk and validation (Done-when 4.3)', () => {
  it('renders form with initial invalid state and disabled submit button', () => {
    render(DemoFormFixture);

    const submitBtn = screen.getByRole('button', { name: /save/i });
    expect(submitBtn.getAttribute('aria-disabled')).toBe('true');
    expect(screen.getByRole('status')).toHaveTextContent(/reason/i);
  });

  it('moves focus in document order from text field to textarea to buttons', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    const textArea = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;
    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    const submitBtn = screen.getByRole('button', { name: /save/i });

    textInput.focus();
    expect(document.activeElement).toBe(textInput);

    // Tab to textarea
    textArea.focus();
    expect(document.activeElement).toBe(textArea);

    // Tab to cancel button
    cancelBtn.focus();
    expect(document.activeElement).toBe(cancelBtn);

    // Tab to submit button
    submitBtn.focus();
    expect(document.activeElement).toBe(submitBtn);
  });

  it('submits on Enter in TextField when valid', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    const textArea = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;

    await fireEvent.input(textInput, { target: { value: 'Feature title' } });
    await fireEvent.input(textArea, { target: { value: 'Valid reason' } });

    const submitBtn = screen.getByRole('button', { name: /save/i });
    expect(submitBtn.getAttribute('aria-disabled')).toBeNull();

    // Press Enter in TextField
    await fireEvent.keyDown(textInput, { key: 'Enter' });
    expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
  });

  it('inserts newline on Enter in TextArea and submits on Ctrl+Enter when valid', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    const textArea = screen.getByTestId('demo-form-textarea') as HTMLTextAreaElement;

    await fireEvent.input(textInput, { target: { value: 'Title' } });
    await fireEvent.input(textArea, { target: { value: 'Line 1' } });

    // Enter in TextArea does not submit
    await fireEvent.keyDown(textArea, { key: 'Enter' });
    expect(screen.queryByText(/submitted/i)).toBeNull();

    // Ctrl+Enter in TextArea submits
    await fireEvent.keyDown(textArea, { key: 'Enter', ctrlKey: true });
    expect(screen.getByRole('status')).toHaveTextContent(/submitted/i);
  });

  it('cancels on Escape', async () => {
    render(DemoFormFixture);

    const textInput = screen.getByTestId('demo-form-text') as HTMLInputElement;
    await fireEvent.input(textInput, { target: { value: 'Some text' } });

    // Press Escape
    await fireEvent.keyDown(textInput, { key: 'Escape' });
    expect(screen.getByRole('status')).toHaveTextContent(/cancelled/i);
  });
});

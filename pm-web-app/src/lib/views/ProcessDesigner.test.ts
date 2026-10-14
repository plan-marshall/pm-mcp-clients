import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import ProcessDesigner from './ProcessDesigner.svelte';

describe('ProcessDesigner component', () => {
  it('renders process designer view title and source editor', () => {
    render(ProcessDesigner);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Process Designer');
    expect(screen.getByText('Workflow Source (.pmwf)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /validate/i })).toBeInTheDocument();
  });

  it('validates workflow successfully with valid header line', async () => {
    render(ProcessDesigner);

    const validateBtn = screen.getByRole('button', { name: /validate/i });
    await fireEvent.click(validateBtn);

    expect(screen.getByText('The process is valid.')).toBeInTheDocument();
  });

  it('inserts building block when block button is clicked', async () => {
    render(ProcessDesigner);

    const qualityGateBtn = screen.getByRole('button', { name: /quality gate/i });
    await fireEvent.click(qualityGateBtn);

    const editor = screen.getByRole('textbox', { name: '' }) as HTMLTextAreaElement;
    expect(editor.value).toContain('gate quality-check:');
  });
});

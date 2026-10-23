import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import WorkflowIcon from './WorkflowIcon.svelte';
import { workflowIconUrl } from '../icons/workflowIcons';

describe('WorkflowIcon component', () => {
  it('renders a known id as a decorative masked icon', () => {
    const { container } = render(WorkflowIcon, { props: { id: 'phase.execute', size: 16 } });
    const icon = container.querySelector('.workflow-icon') as HTMLElement;

    expect(icon).not.toBeNull();
    expect(icon.getAttribute('aria-hidden')).toBe('true');
    expect(icon.getAttribute('role')).toBeNull();
    expect(icon.getAttribute('data-workflow-icon')).toBe('phase.execute');
    expect(icon.getAttribute('style')).toContain(workflowIconUrl('phase.execute'));
    expect(icon.getAttribute('style')).toContain('--workflow-icon-size: 16px');
  });

  it('renders the fallback when the id is unknown', () => {
    const { container } = render(WorkflowIcon, { props: { id: 'future.unknown' } });
    const icon = container.querySelector('.workflow-icon') as HTMLElement;

    expect(icon.getAttribute('data-workflow-icon')).toBe('future.unknown');
    expect(icon.getAttribute('style')).toContain(workflowIconUrl('workflow.default'));
  });

  it('carries an accessible name when it is not decorative', () => {
    const { container } = render(WorkflowIcon, {
      props: { id: 'flow.land', label: 'Landing' },
    });
    const icon = container.querySelector('.workflow-icon') as HTMLElement;

    expect(icon.getAttribute('role')).toBe('img');
    expect(icon.getAttribute('aria-label')).toBe('Landing');
    expect(icon.hasAttribute('aria-hidden')).toBe(false);
  });
});

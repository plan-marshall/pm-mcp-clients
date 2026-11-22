import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import GrantModal from './GrantModal.svelte';

describe('GrantModal', () => {
  it('is the design dialog and asks for the read of the repository', () => {
    render(GrantModal, { props: { open: true, repository: 'cuioss/cui-http' } });

    const dialog = screen.getByRole('dialog', { name: 'Read issues of cuioss/cui-http?' });

    expect(within(dialog).getByTestId('grant-once')).toHaveTextContent('this time');
    expect(within(dialog).getByTestId('grant-project')).toHaveTextContent('for this project');
    expect(within(dialog).getByTestId('grant-no')).toHaveTextContent('No');
  });

  it('gives the three answers by the hierarchy, the narrowest grant primary', () => {
    render(GrantModal, { props: { open: true, repository: 'cuioss/cui-http' } });

    expect(screen.getByTestId('grant-once').className).toContain('btn--primary');
    expect(screen.getByTestId('grant-project').className).toContain('btn--secondary');
    expect(screen.getByTestId('grant-no').className).toContain('btn--ghost');
  });

  it('reports each of the three answers', async () => {
    const onGrant = vi.fn();
    const onRefuse = vi.fn();
    render(GrantModal, {
      props: { open: true, repository: 'cuioss/cui-http', onGrant, onRefuse },
    });

    await fireEvent.click(screen.getByTestId('grant-once'));
    await fireEvent.click(screen.getByTestId('grant-project'));
    await fireEvent.click(screen.getByTestId('grant-no'));

    expect(onGrant).toHaveBeenNthCalledWith(1, 'once');
    expect(onGrant).toHaveBeenNthCalledWith(2, 'project');
    expect(onRefuse).toHaveBeenCalledOnce();
  });

  it('closes on Escape as the refusal does', async () => {
    const onRefuse = vi.fn();
    render(GrantModal, { props: { open: true, repository: 'cuioss/cui-http', onRefuse } });

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onRefuse).toHaveBeenCalledOnce();
  });

  it('renders nothing while it is closed', () => {
    render(GrantModal, { props: { open: false, repository: 'cuioss/cui-http' } });

    expect(screen.queryByTestId('grant-modal')).not.toBeInTheDocument();
  });
});

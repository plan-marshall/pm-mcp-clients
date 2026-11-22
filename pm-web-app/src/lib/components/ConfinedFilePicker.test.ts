import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import ConfinedFilePicker from './ConfinedFilePicker.svelte';
import { apiClient, type ConfinedFileList } from '../api/client';

/** Whether the session is a loopback one; a test on the network listener turns it off. */
const session = vi.hoisted(() => ({ loopback: true }));

vi.mock('../utils/loopback', () => ({
  isLoopbackHost: () => session.loopback,
  isLoopbackSession: () => session.loopback,
}));

const LISTING: ConfinedFileList = {
  project_id: 'p-1',
  root: 'checkout',
  path: '',
  parent: null,
  truncated: false,
  entries: [
    { name: 'docs', path: 'docs', directory: true, size: 0, readable: true },
    { name: 'README.md', path: 'README.md', directory: false, size: 120, readable: true },
    { name: 'GUIDE.adoc', path: 'GUIDE.adoc', directory: false, size: 900, readable: true },
    {
      name: 'huge.bin',
      path: 'huge.bin',
      directory: false,
      size: 12_000_000,
      readable: false,
      refusal: { cause: 'too_large', detail: '12 MB' },
    },
  ],
};

describe('ConfinedFilePicker', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    session.loopback = true;
  });

  it('is the design dialog, browses the checkout first and shows the summary and a refused entry with its reason', async () => {
    const list = vi.spyOn(apiClient, 'listProjectFiles').mockResolvedValue(LISTING);
    render(ConfinedFilePicker, { props: { open: true, projectId: 'p-1' } });

    const dialog = await screen.findByRole('dialog', { name: 'Documents' });

    expect(list).toHaveBeenCalledWith('p-1', 'checkout', '');
    const roots = within(dialog)
      .getAllByRole('button')
      .filter((button) => button.dataset.testid?.startsWith('picker-root-'));
    expect(roots.map((button) => button.dataset.testid)).toEqual([
      'picker-root-checkout',
      'picker-root-home',
    ]);
    expect(roots[0]).toHaveAttribute('aria-pressed', 'true');
    // The path the listing shows is a TextField of the field system.
    expect(within(dialog).getByRole('textbox')).toHaveValue('/');
    // The listing is a CheckGroup whose legend says what the directory holds.
    expect(within(dialog).getByText(/3 files: 1 md, 1 adoc, 1 bin/)).toBeInTheDocument();
    // A file the runtime refuses is disabled and says why.
    expect(screen.getByRole('checkbox', { name: /huge\.bin/ })).toBeDisabled();
    expect(within(dialog).getByText(/larger than/)).toBeInTheDocument();
  });

  it('enters a directory and goes back up', async () => {
    const list = vi
      .spyOn(apiClient, 'listProjectFiles')
      .mockResolvedValueOnce(LISTING)
      .mockResolvedValueOnce({
        ...LISTING,
        path: 'docs',
        parent: '.',
        entries: [{ name: 'a.md', path: 'docs/a.md', directory: false, size: 1, readable: true }],
      } as ConfinedFileList)
      .mockResolvedValueOnce(LISTING);
    render(ConfinedFilePicker, { props: { open: true, projectId: 'p-1' } });

    await fireEvent.click(await screen.findByTestId('picker-enter-docs'));

    expect(list).toHaveBeenLastCalledWith('p-1', 'checkout', 'docs');
    await fireEvent.click(await screen.findByTestId('picker-up'));

    expect(list).toHaveBeenLastCalledWith('p-1', 'checkout', '');
  });

  it('picks a file, clears the pick and confirms the documents', async () => {
    vi.spyOn(apiClient, 'listProjectFiles').mockResolvedValue(LISTING);
    const onAdd = vi.fn();
    render(ConfinedFilePicker, { props: { open: true, projectId: 'p-1', onAdd } });

    const add = await screen.findByTestId('picker-add');
    expect(add).toBeDisabled();

    const readme = screen.getByRole('checkbox', { name: 'README.md' });
    await fireEvent.click(readme);
    expect(add).not.toBeDisabled();
    await fireEvent.click(readme);
    expect(add).toBeDisabled();

    await fireEvent.click(readme);
    await fireEvent.click(add);

    expect(onAdd).toHaveBeenCalledWith(['README.md']);
    expect(add).toBeDisabled();
  });

  it('disables the home directory off the loopback listener and says why', async () => {
    session.loopback = false;
    vi.spyOn(apiClient, 'listProjectFiles').mockResolvedValue(LISTING);
    render(ConfinedFilePicker, { props: { open: true, projectId: 'p-1' } });

    const home = await screen.findByTestId('picker-root-home');

    expect(home).toHaveAttribute('aria-disabled', 'true');
    const reason = home.getAttribute('aria-describedby');
    expect(document.getElementById(reason as string)).toHaveTextContent(/only on this machine/);
  });

  it('closes on Escape and through its ghost act', async () => {
    vi.spyOn(apiClient, 'listProjectFiles').mockResolvedValue(LISTING);
    const onClose = vi.fn();
    render(ConfinedFilePicker, { props: { open: true, projectId: 'p-1', onClose } });

    await screen.findByRole('dialog', { name: 'Documents' });
    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(onClose).toHaveBeenCalledOnce();

    await fireEvent.click(screen.getByTestId('picker-close'));

    expect(onClose).toHaveBeenCalledTimes(2);
  });
});

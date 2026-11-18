import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/svelte';
import { tick } from 'svelte';
import PlanChanges from './PlanChanges.svelte';
import { apiClient } from '../api/client';
import type { ChangeSizeRecord, PlanChangesAnswer, PlanChangeFile } from '../api/client';

/**
 * The size line and the tab read the same measurement; the component holds the list and one file's
 * hunks, and never adds a figure up of its own.
 */
const CHANGE: ChangeSizeRecord = { files: 5, insertions: 142, deletions: 18, unplanned: 1 };

const CAPTURED: Extract<PlanChangesAnswer, { state: 'captured' }> = {
  plan_id: 'fix-relay',
  state: 'captured',
  head: '9f3c2ab41d7e5',
  base_ref: 'main',
  files: [
    {
      path: 'pm-relay/src/main/java/Transport.java',
      status: 'modified',
      added: 48,
      removed: 12,
      binary: false,
      unplanned: false,
    },
    {
      path: 'pm-relay/src/main/java/Frame.java',
      status: 'renamed',
      renamed_from: 'pm-relay/src/main/java/Packet.java',
      added: 3,
      removed: 3,
      binary: false,
      unplanned: false,
    },
    {
      path: 'doc/relay.png',
      status: 'modified',
      added: 0,
      removed: 0,
      binary: true,
      unplanned: false,
    },
    {
      path: 'pom.xml',
      status: 'modified',
      added: 1,
      removed: 1,
      binary: false,
      unplanned: true,
    },
    {
      path: 'pm-relay/src/main/java/Big.java',
      status: 'modified',
      added: 90,
      removed: 2,
      binary: false,
      unplanned: false,
    },
  ],
};

/**
 * @param overrides the members the test changes
 * @returns a file answer of the capture
 */
function fileAnswer(overrides: Partial<PlanChangeFile> = {}): PlanChangeFile {
  return {
    path: 'pm-relay/src/main/java/Transport.java',
    hunks: [
      {
        old_start: 10,
        old_len: 2,
        new_start: 10,
        new_len: 2,
        lines: [
          { kind: 'context', text: 'private final Transport transport;' },
          { kind: 'removed', text: 'return transport.send(oldRequest);' },
          { kind: 'added', text: 'return transport.send(newRequest);' },
          { kind: 'context', text: '}' },
        ],
      },
    ],
    binary: false,
    truncated: false,
    ...overrides,
  };
}

describe('PlanChanges', () => {
  it('shows the list with its status, counts, renamed-from, binary and unplanned rows', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('changes-title')).toHaveTextContent('changes · at 9f3c2ab');
    const rows = await screen.findAllByTestId('change-row');
    expect(rows).toHaveLength(5);
    expect(rows[0]).toHaveTextContent('M');
    expect(rows[0]).toHaveTextContent('Transport.java');
    expect(rows[0]).toHaveTextContent('+48');
    expect(rows[0]).toHaveTextContent('−12');
    expect(rows[1]).toHaveTextContent('R');
    expect(rows[1]).toHaveTextContent('Packet.java');
    expect(rows[2]).toHaveTextContent('B');
    expect(rows[2]).toHaveTextContent('binary');
    expect(rows[3]).toHaveTextContent('unplanned');
  });

  it('shows a file’s hunks with the old and the new line numbers', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue(fileAnswer());

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('change-hunks')).toBeInTheDocument();
    const oldNumbers = screen.getAllByTestId('line-old').map((cell) => cell.textContent);
    const newNumbers = screen.getAllByTestId('line-new').map((cell) => cell.textContent);
    expect(oldNumbers).toEqual(['10', '11', '', '12']);
    expect(newNumbers).toEqual(['10', '', '11', '12']);
    expect(screen.getByTestId('hunk-header')).toHaveTextContent('@@ -10,2 +10,2 @@');
    // The grounds of the diff are the design's own tokens, one class per side.
    const lines = screen.getAllByTestId('diff-line');
    expect(lines[1]).toHaveClass('diff-line--removed');
    expect(lines[2]).toHaveClass('diff-line--added');
  });

  it('renders a line that holds markup as text, never as an element', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue(
      fileAnswer({
        hunks: [
          {
            old_start: 1,
            old_len: 0,
            new_start: 1,
            new_len: 1,
            lines: [{ kind: 'added', text: '<b onmouseover="alert(1)">bold</b>' }],
          },
        ],
      }),
    );

    // Act
    const { container } = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByText('<b onmouseover="alert(1)">bold</b>')).toBeInTheDocument();
    expect(container.querySelector('b')).toBeNull();
  });

  it('names the lines a cut file left out', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue(
      fileAnswer({ truncated: true, lines_omitted: 120 }),
    );

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('changes-truncated')).toHaveTextContent(
      '120 more lines not shown',
    );
  });

  it('answers a binary file with its row only, and no lines', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);
    vi.spyOn(apiClient, 'getPlanChangeFile').mockResolvedValue({
      path: 'doc/relay.png',
      hunks: [],
      binary: true,
      truncated: false,
    });

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'doc/relay.png',
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('change-file-binary')).toHaveTextContent('binary');
    expect(screen.queryAllByTestId('diff-line')).toHaveLength(0);
  });

  it('says the capture is gone, with its reason', async () => {
    // Arrange
    const reason = 'the captured commit 4c1d7e0 is no longer in the repository';
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      plan_id: 'fix-relay',
      state: 'unavailable',
      reason,
    });

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('changes-unavailable')).toHaveTextContent(reason);
  });

  it('says no changes were captured yet, never showing an empty list', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      plan_id: 'fix-relay',
      state: 'not_captured',
    });

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('changes-not-captured')).toHaveTextContent(
      'No changes captured yet',
    );
    expect(screen.queryAllByTestId('change-row')).toHaveLength(0);
  });

  it('drops a selected file the new list does not name', async () => {
    // Arrange
    const onSelectFile = vi.fn();
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      ...CAPTURED,
      files: CAPTURED.files.slice(0, 1),
    });

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pom.xml',
      onSelectFile,
    });

    // Assert
    await waitFor(() => expect(onSelectFile).toHaveBeenCalledWith(null));
  });

  it('discards a file answer the list moved under, and reads the row again', async () => {
    // Arrange: the first file read is still open when the list names the row with other counts.
    const firstFile = deferred<PlanChangeFile>();
    const secondFile = deferred<PlanChangeFile>();
    const readFile = vi
      .spyOn(apiClient, 'getPlanChangeFile')
      .mockReturnValueOnce(firstFile.promise)
      .mockReturnValueOnce(secondFile.promise);
    const readList = vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);

    const view = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });
    await screen.findAllByTestId('change-row');

    // Act: a capture moves the row's counts, so the tab reads the list again.
    readList.mockResolvedValue({
      ...CAPTURED,
      head: 'a1b2c3d4e5f6a',
      files: [{ ...CAPTURED.files[0], added: 60 }, ...CAPTURED.files.slice(1)],
    });
    await view.rerender({
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
      reloadToken: 1,
    });
    await waitFor(() => expect(readFile).toHaveBeenCalledTimes(2));

    // The answer of the row the list no longer holds arrives while the new row's read is still open:
    // it must be dropped, or it would show hunks the list has already replaced.
    firstFile.resolve(fileAnswer({ hunks: [hunkWith('stale line')] }));
    await Promise.resolve();
    await tick();

    // Assert the stale answer was never shown
    expect(screen.queryByText('stale line')).toBeNull();

    // The new row's answer is the one the view shows.
    secondFile.resolve(fileAnswer({ hunks: [hunkWith('fresh line')] }));
    expect(await screen.findByText('fresh line')).toBeInTheDocument();
  });

  it('offers a retry when a file read fails, and reads the row again', async () => {
    // Arrange
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue(CAPTURED);
    vi.spyOn(apiClient, 'getPlanChangeFile')
      .mockRejectedValueOnce(new Error('unavailable'))
      .mockResolvedValueOnce(fileAnswer());

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });
    const retry = await screen.findByRole('button', { name: 'Try again' });
    retry.click();

    // Assert
    expect(await screen.findByTestId('change-hunks')).toHaveTextContent(
      'return transport.send(newRequest);',
    );
  });

  it('reads the list once for one plan, however the answer objects differ', async () => {
    // Arrange: each read answers a fresh object, as a real response parse does.
    const readList = vi
      .spyOn(apiClient, 'getPlanChanges')
      .mockImplementation(async () => ({ ...CAPTURED }));

    // Act
    render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });
    await screen.findByTestId('changes-title');

    // Assert
    expect(readList).toHaveBeenCalledTimes(1);
  });

  it('reads the hunks of the row a person selects, even where two rows look alike', async () => {
    // Arrange: two files of the same status and counts at one head are still two files.
    vi.spyOn(apiClient, 'getPlanChanges').mockResolvedValue({
      plan_id: 'fix-relay',
      state: 'captured',
      head: '9f3c2ab41d7e5',
      base_ref: 'main',
      files: [
        { path: 'src/a.ts', status: 'modified', added: 1, removed: 1, binary: false, unplanned: false },
        { path: 'src/b.ts', status: 'modified', added: 1, removed: 1, binary: false, unplanned: false },
      ],
    });
    vi.spyOn(apiClient, 'getPlanChangeFile').mockImplementation(async (_planId, path) =>
      fileAnswer({ path, hunks: [hunkWith(`${path} line`)] }),
    );

    const view = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'src/a.ts',
      onSelectFile: () => {},
    });
    expect(await screen.findByText('src/a.ts line')).toBeInTheDocument();

    // Act
    await view.rerender({
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'src/b.ts',
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByText('src/b.ts line')).toBeInTheDocument();
    expect(screen.queryByText('src/a.ts line')).toBeNull();
  });

  it('hides the previous row’s hunks while the row it now stands on is read', async () => {
    // Arrange
    const second = deferred<PlanChangeFile>();
    vi.spyOn(apiClient, 'getPlanChanges')
      .mockResolvedValueOnce(CAPTURED)
      .mockResolvedValueOnce({
        ...CAPTURED,
        head: 'a1b2c3d4e5f6a',
        files: [{ ...CAPTURED.files[0], added: 60 }, ...CAPTURED.files.slice(1)],
      });
    vi.spyOn(apiClient, 'getPlanChangeFile')
      .mockResolvedValueOnce(fileAnswer({ hunks: [hunkWith('first line')] }))
      .mockReturnValueOnce(second.promise);

    const view = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
    });
    expect(await screen.findByText('first line')).toBeInTheDocument();

    // Act: the capture moves the row, and the new read is still open.
    await view.rerender({
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pm-relay/src/main/java/Transport.java',
      onSelectFile: () => {},
      reloadToken: 1,
    });

    // Assert: no line of the row the tab no longer stands on is left on screen.
    await waitFor(() => expect(screen.queryAllByTestId('diff-line')).toHaveLength(0));
    second.resolve(fileAnswer({ hunks: [hunkWith('second line')] }));
    expect(await screen.findByText('second line')).toBeInTheDocument();
  });

  it('applies no list completion after the view is gone', async () => {
    // Arrange
    const list = deferred<PlanChangesAnswer>();
    vi.spyOn(apiClient, 'getPlanChanges').mockReturnValueOnce(list.promise);
    const onSelectFile = vi.fn();
    const view = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: 'pom.xml',
      onSelectFile,
    });

    // Act: the view goes before its list arrives, and the answer no longer names the file.
    view.unmount();
    list.resolve({ ...CAPTURED, files: CAPTURED.files.slice(0, 1) });
    await tick();

    // Assert
    expect(onSelectFile).not.toHaveBeenCalled();
  });

  it('drops the previous plan’s list when another plan is opened', async () => {
    // Arrange: the first plan's list is served, then the view is asked for another plan.
    const readList = vi
      .spyOn(apiClient, 'getPlanChanges')
      .mockResolvedValueOnce(CAPTURED)
      .mockResolvedValueOnce({
        plan_id: 'relay-metrics',
        state: 'not_captured',
      });

    const view = render(PlanChanges, {
      planId: 'fix-relay',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });
    expect(await screen.findByTestId('changes-title')).toBeInTheDocument();

    // Act
    await view.rerender({
      planId: 'relay-metrics',
      change: CHANGE,
      selectedFile: null,
      onSelectFile: () => {},
    });

    // Assert
    expect(await screen.findByTestId('changes-not-captured')).toBeInTheDocument();
    expect(screen.queryByTestId('changes-title')).toBeNull();
    expect(readList).toHaveBeenCalledTimes(2);
  });
});

/**
 * @param text the added line's text
 * @returns a one-line hunk that adds it
 */
function hunkWith(text: string): PlanChangeFile['hunks'][number] {
  return {
    old_start: 1,
    old_len: 0,
    new_start: 1,
    new_len: 1,
    lines: [{ kind: 'added', text }],
  };
}

/**
 * @returns a promise a test settles by hand, so the ordering of two answers is its own choice
 */
function deferred<T>(): { promise: Promise<T>; resolve: (value: T) => void } {
  let resolve: (value: T) => void = () => {};
  const promise = new Promise<T>((settle) => {
    resolve = settle;
  });
  return { promise, resolve };
}

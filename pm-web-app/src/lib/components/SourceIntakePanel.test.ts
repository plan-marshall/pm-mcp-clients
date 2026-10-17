import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import SourceIntakePanel from './SourceIntakePanel.svelte';

describe('SourceIntakePanel component', () => {
  it('builds sources from the description, the documents, and the issue', async () => {
    const onChange = vi.fn();
    render(SourceIntakePanel, { props: { onChange } });

    await fireEvent.input(screen.getByTestId('source-description'), {
      target: { value: 'Add a Codeberg provider' },
    });
    await fireEvent.input(screen.getByTestId('source-documents'), {
      target: { value: 'doc/a.adoc\ndoc/b.adoc' },
    });
    await fireEvent.input(screen.getByTestId('source-issue'), { target: { value: '4711' } });

    const last = onChange.mock.calls.at(-1)?.[0];
    expect(last).toEqual([
      { kind: 'description', location: 'Add a Codeberg provider' },
      { kind: 'documents', location: 'doc/a.adoc' },
      { kind: 'documents', location: 'doc/b.adoc' },
      { kind: 'issue', location: '4711' },
    ]);
  });

  it('ignores blank lines and trims the paths', async () => {
    const onChange = vi.fn();
    render(SourceIntakePanel, { props: { onChange } });

    await fireEvent.input(screen.getByTestId('source-documents'), {
      target: { value: '  doc/a.adoc  \n\n  ' },
    });

    const last = onChange.mock.calls.at(-1)?.[0];
    expect(last).toEqual([{ kind: 'documents', location: 'doc/a.adoc' }]);
  });

  it('counts the sources so far', async () => {
    render(SourceIntakePanel, { props: {} });
    await fireEvent.input(screen.getByTestId('source-description'), { target: { value: 'x' } });
    expect(screen.getByTestId('source-count')).toHaveTextContent('waits for 1 source');
  });
});

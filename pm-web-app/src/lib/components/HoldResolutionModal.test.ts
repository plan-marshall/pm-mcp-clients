import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import HoldResolutionModal from './HoldResolutionModal.svelte';
import HoldResolutionModalOwnerFixture from '../../test/fixtures/HoldResolutionModalOwnerFixture.svelte';
import { formatTime } from '../display/format';
import type { HoldRecord } from '../api/client';

/** The instant the runtime sends for a hold's last attempt. */
const LAST_TRIED_AT = '2026-10-16T10:14:00Z';

/** The words of a hold's acts, as the catalogue carries them. */
const ACT_WORDS = {
  retry: 'Retry now',
  recycle: 'Recycle the worker',
  accept: 'Accept the change…',
  abort: 'Abort the plan…',
  end_plan: 'End the plan…',
  cancel_epic: 'Cancel the epic…',
  show_pr: 'Show the pull request #211',
  give_up_removal: 'Give up the removal…',
} as const;

describe('HoldResolutionModal component', () => {
  const conditionHold: HoldRecord = {
    hold_id: 'H-0001',
    subject_id: 'fix-relay',
    subject_kind: 'plan',
    kind: 'condition',
    cause: 'the plan reads cui-http, which this project may not read',
    attempts: 3,
    last_tried_at: LAST_TRIED_AT,
    acts: ['retry', 'grant', 'end_plan'],
  };

  const integrityHold: HoldRecord = {
    hold_id: 'H-0002',
    subject_id: 'relay-cursor-order',
    subject_kind: 'plan',
    kind: 'integrity_pause',
    cause: "the plan's store file references.json changed outside the runtime",
    attempts: 0,
    file_path: 'references.json',
    recorded_size: 4812,
    recorded_digest: '3f9a6b2cc21e',
    current_size: 5034,
    current_digest: 'b70d41aa9a44',
    acts: ['retry', 'accept', 'abort'],
  };

  /** The five kinds of a hold, each with acts the runtime offers for it. */
  const HOLDS: { kind: string; acts: string[] }[] = [
    { kind: 'stalled', acts: ['retry', 'recycle', 'end_plan'] },
    { kind: 'condition', acts: ['retry', 'grant', 'end_plan'] },
    { kind: 'integrity_pause', acts: ['retry', 'accept', 'abort'] },
    { kind: 'held', acts: ['retry', 'end_plan'] },
    { kind: 'landing_failed', acts: ['retry', 'show_pr', 'give_up_removal'] },
  ];

  it('is the design dialog: its title names it and its acts sit in it', () => {
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true, project: 'a-project' } });

    const dialog = screen.getByRole('dialog', { name: 'fix-relay holds: a condition' });

    expect(within(dialog).getByTestId('hold-act-grant')).toHaveTextContent(
      'Grant read access to a-project',
    );
  });

  it('draws its facts as the one list of figures, the cause among them', () => {
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true } });

    const facts = screen.getByTestId('key-value-list');

    expect(facts).toHaveTextContent('the plan reads cui-http, which this project may not read');
    expect(facts).toHaveTextContent(`Tried 3 times, last ${formatTime(LAST_TRIED_AT, 'short', 'en')}`);
  });

  it('shows the tampered file of an integrity pause with its recorded and current facts', () => {
    render(HoldResolutionModal, { props: { hold: integrityHold, open: true } });

    const facts = screen.getByTestId('key-value-list');

    expect(facts).toHaveTextContent('references.json');
    expect(facts).toHaveTextContent('recorded: 4812 · 3f9a6b2cc21e');
    expect(facts).toHaveTextContent('current: 5034 · b70d41aa9a44');
  });

  it.each(HOLDS)('offers the acts of a $kind hold by the hierarchy', async ({ kind, acts }) => {
    render(HoldResolutionModal, {
      props: { hold: { ...conditionHold, kind, acts }, open: true, project: 'pm-mcp-clients' },
    });

    for (const act of acts) {
      const button = screen.getByTestId(`hold-act-${act}`);

      expect(button.className).toContain('btn--secondary');
    }
    // Leaving the hold without acting is the ghost act of the footer, never a secondary one.
    expect(screen.getByTestId('hold-close-button').className).toContain('btn--ghost');
  });

  it('calls onResolve for a reversible act at once', async () => {
    const onResolve = vi.fn();
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true, onResolve } });

    await fireEvent.click(screen.getByTestId('hold-act-retry'));

    expect(onResolve).toHaveBeenCalledOnce();
    expect(onResolve).toHaveBeenCalledWith({ act: 'retry' });
  });

  it('opens the confirmation of an act that cannot be undone as its own ConfirmDialog', async () => {
    const onResolve = vi.fn();
    render(HoldResolutionModal, { props: { hold: integrityHold, open: true, onResolve } });

    await fireEvent.click(screen.getByTestId('hold-act-accept'));

    const confirm = screen.getByRole('dialog', { name: ACT_WORDS.accept });

    expect(onResolve).not.toHaveBeenCalled();
    expect(confirm).toHaveTextContent('The earlier content is not kept');
    expect(within(confirm).getByRole('textbox', { name: 'Reason for this act' })).toBeInTheDocument();
  });

  it('takes the operator’s reason on a TextField and sends it with the act', async () => {
    const onResolve = vi.fn();
    render(HoldResolutionModal, { props: { hold: integrityHold, open: true, onResolve } });

    await fireEvent.click(screen.getByTestId('hold-act-accept'));
    await fireEvent.input(screen.getByTestId('hold-reason'), {
      target: { value: 'the change is mine' },
    });
    await fireEvent.click(screen.getByTestId('confirm-dialog-confirm'));

    expect(onResolve).toHaveBeenCalledWith({ act: 'accept', reason: 'the change is mine' });
  });

  it('explains an epic cancel with the epic text', async () => {
    render(HoldResolutionModal, {
      props: { hold: { ...conditionHold, subject_kind: 'epic', acts: ['cancel_epic'] }, open: true },
    });

    await fireEvent.click(screen.getByTestId('hold-act-cancel_epic'));

    expect(screen.getByRole('dialog', { name: ACT_WORDS.cancel_epic })).toHaveTextContent(
      'the epic is archived as cancelled',
    );
  });

  it('closes on Escape and gives the focus back to the act of the view that opened it', async () => {
    render(HoldResolutionModalOwnerFixture, {
      props: { hold: conditionHold, openLabel: 'Resolve the hold' },
    });
    const opener = screen.getByRole('button', { name: 'Resolve the hold' });
    opener.focus();
    await fireEvent.click(opener);
    await screen.findByRole('dialog', { name: 'fix-relay holds: a condition' });

    await fireEvent.keyDown(window, { key: 'Escape' });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.activeElement).toBe(opener);
  });

  it('calls onClose from its ghost act', async () => {
    const onClose = vi.fn();
    render(HoldResolutionModal, { props: { hold: conditionHold, open: true, onClose } });

    await fireEvent.click(screen.getByTestId('hold-close-button'));

    expect(onClose).toHaveBeenCalledOnce();
  });

  it('renders nothing while it is closed', () => {
    render(HoldResolutionModal, { props: { hold: conditionHold, open: false } });

    expect(screen.queryByTestId('hold-modal')).not.toBeInTheDocument();
  });

  it('names what still holds after an act that did not release the subject', () => {
    render(HoldResolutionModal, {
      props: {
        hold: conditionHold,
        open: true,
        outcome: {
          hold_id: 'H-0001',
          released: false,
          cause: 'the plan reads cui-http, which this project may not read',
          subject: 'fix-relay',
        },
      },
    });

    expect(screen.getByTestId('hold-still-held')).toHaveTextContent(
      'Still held: the plan reads cui-http, which this project may not read',
    );
  });

  it('says the subject goes on after a releasing act', () => {
    render(HoldResolutionModal, {
      props: {
        hold: conditionHold,
        open: true,
        outcome: { hold_id: 'H-0001', released: true, subject: 'fix-relay' },
      },
    });

    expect(screen.getByTestId('hold-released')).toHaveTextContent('fix-relay goes on');
  });

  it('names the project of a grant and the number of a pull request', () => {
    render(HoldResolutionModal, {
      props: {
        hold: {
          ...conditionHold,
          acts: ['retry', 'grant', 'show_pr'],
          pull_request_url: 'https://example.test/relay/pull/211',
        },
        open: true,
        project: 'pm-mcp-clients',
      },
    });

    expect(screen.getByTestId('hold-act-grant')).toHaveTextContent(
      'Grant read access to pm-mcp-clients',
    );
    expect(screen.getByTestId('hold-act-show_pr')).toHaveTextContent(ACT_WORDS.show_pr);
  });
});

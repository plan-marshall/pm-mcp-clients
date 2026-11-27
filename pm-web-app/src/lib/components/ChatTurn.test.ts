import path from 'node:path';
import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ChatTurn from './ChatTurn.svelte';
import ChatTranscript from './ChatTranscript.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { ruleOf, rulesOf, styleOf } from '../../test/templateStyle';
import type { LocalChatTurn } from '../chat/types';

function makeTurn(overrides: Partial<LocalChatTurn> = {}): LocalChatTurn {
  return {
    turn_id: 't-1',
    kind: 'message',
    author: 'planner',
    status: 'complete',
    format: 'markdown',
    untrusted: false,
    model: 'sonnet',
    text: 'Role response with **markdown**.',
    steps: [],
    refs: [],
    at: '2026-11-27T10:15:00Z',
    reason: null,
    lines: [],
    ...overrides,
  };
}

const TURN_STYLE = styleOf(path.resolve(__dirname, 'ChatTurn.svelte'));
const TRANSCRIPT_STYLE = styleOf(path.resolve(__dirname, 'ChatTranscript.svelte'));

describe('ChatTurn', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('renders an operator turn right-aligned on --color-bg-subtle as plain text', () => {
    const turn = makeTurn({
      turn_id: 't-op',
      author: 'operator',
      format: 'plain',
      model: null,
      text: '**literal** not bold',
    });

    const { container } = render(ChatTurn, { props: { turn } });

    const el = screen.getByTestId('chat-turn');
    expect(el).toHaveClass('chat-turn--operator');
    expect(screen.getByText('You')).toBeInTheDocument();
    expect(container.querySelector('strong')).toBeNull();
    expect(container.textContent).toContain('**literal** not bold');
    expect(ruleOf(TURN_STYLE, '.chat-turn--operator')).toMatch(/var\(--color-bg-subtle\)/);
  });

  it('labels operator turns from another device as You · {device}', () => {
    const turn = makeTurn({
      turn_id: 't-op-remote',
      author: 'operator',
      format: 'plain',
      model: null,
      text: 'From another screen',
    });

    render(ChatTurn, { props: { turn, deviceName: 'iPad' } });
    expect(screen.getByText('You · iPad')).toBeInTheDocument();
  });

  it('renders sending and not_sent operator turns with Try again and Edit actions', async () => {
    const onRetry = vi.fn();
    const onEdit = vi.fn();
    const sendingTurn = makeTurn({
      turn_id: 'c-1',
      client_turn_id: 'c-1',
      author: 'operator',
      format: 'plain',
      status: 'streaming',
      delivery: 'sending',
      text: 'Hello',
    });

    const { rerender } = render(ChatTurn, {
      props: { turn: sendingTurn, onretry: onRetry, onedit: onEdit },
    });
    expect(screen.getByText('sending…')).toBeInTheDocument();

    await rerender({
      turn: { ...sendingTurn, status: 'failed', delivery: 'not_sent' },
      onretry: onRetry,
      onedit: onEdit,
    });
    expect(screen.getByText('Not sent')).toBeInTheDocument();

    await fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledWith('c-1');

    await fireEvent.click(screen.getByRole('button', { name: 'Edit the message' }));
    expect(onEdit).toHaveBeenCalledWith('c-1');
  });

  it('marks untrusted role turns as From outside · not checked and renders plain text only', () => {
    const turn = makeTurn({
      untrusted: true,
      format: 'markdown',
      text: '<script>evil()</script> **plain**',
    });

    const { container } = render(ChatTurn, { props: { turn } });

    expect(screen.getByText('From outside · not checked')).toBeInTheDocument();
    expect(container.querySelector('[data-testid="chat-markdown"]')).toBeNull();
    expect(container.querySelector('strong')).toBeNull();
    expect(container.textContent).toContain('<script>evil()</script> **plain**');
  });

  it('shows working line and muted steps before text arrives, folds steps once text streams, and clears on re-start', async () => {
    const waitingTurn = makeTurn({
      status: 'streaming',
      text: '',
      steps: [
        { key: 'chat.step.read', args: { target: 'Chat.svelte' } },
        { key: 'chat.step.search', args: { target: 'refs' } },
        { key: 'chat.step.check', args: { check: 'tests' } },
      ],
    });

    const { container, rerender } = render(ChatTurn, {
      props: { turn: waitingTurn, scope: 'plan' },
    });

    // Working indicator stays visible even before any text piece arrives.
    expect(screen.getByText(/planner is reading the whole plan/)).toBeInTheDocument();
    expect(screen.getByText('reads Chat.svelte')).toBeInTheDocument();
    expect(screen.getByText('searches refs')).toBeInTheDocument();
    expect(screen.getByText('runs tests')).toBeInTheDocument();

    // Soft caret has a still fallback under prefers-reduced-motion.
    const caretRules = rulesOf(TURN_STYLE, '.chat-turn__caret');
    expect(caretRules.some((r) => /animation:\s*none/.test(r) && /opacity:\s*1/.test(r))).toBe(
      true,
    );
    expect(TURN_STYLE).toContain('@media (prefers-reduced-motion: reduce)');

    // Once reply text arrives, WaitingMark clears and steps fold to a 3 steps summary toggle.
    await rerender({
      turn: { ...waitingTurn, text: 'Partial answer.' },
      scope: 'plan',
    });
    expect(screen.queryByTestId('waiting-mark')).toBeNull();
    expect(screen.queryByText('reads Chat.svelte')).toBeNull();
    const foldBtn = screen.getByRole('button', { name: '3 steps' });
    await fireEvent.click(foldBtn);
    expect(screen.getByText('reads Chat.svelte')).toBeInTheDocument();

    // Stopped before any text keeps steps visible while clearing WaitingMark.
    await rerender({
      turn: { ...waitingTurn, status: 'stopped', text: '' },
      scope: 'plan',
    });
    expect(screen.queryByTestId('waiting-mark')).toBeNull();
    expect(screen.getByText('reads Chat.svelte')).toBeInTheDocument();

    // Re-announced started turn replaces partial text and steps cleanly.
    await rerender({
      turn: makeTurn({ status: 'streaming', text: '', steps: [] }),
      scope: 'plan',
    });
    expect(container.textContent).not.toContain('Partial answer.');
    expect(screen.queryByRole('button', { name: '3 steps' })).toBeNull();
  });

  it('folds notices with more than 3 narrative lines without breaking and reveals on toggle', async () => {
    const turn = makeTurn({
      kind: 'notice',
      format: 'plain',
      text: '',
      lines: [
        {
          key: 'chat.line.task_ended',
          args: { task_id: 'T-1', outcome: 'done' },
          refs: [],
          at: '2026-11-27T10:00:00Z',
        },
        {
          key: 'chat.line.task_ended',
          args: { task_id: 'T-2', outcome: 'done' },
          refs: [],
          at: '2026-11-27T10:01:00Z',
        },
        {
          key: 'chat.line.task_ended',
          args: { task_id: 'T-3', outcome: 'done' },
          refs: [],
          at: '2026-11-27T10:02:00Z',
        },
        { key: 'chat.line.pr_opened', args: { number: 42 }, refs: [], at: '2026-11-27T10:03:00Z' },
      ],
    });

    render(ChatTurn, { props: { turn, subjectLabel: 'fix-relay' } });

    expect(screen.queryByText('#42 opened')).toBeNull();
    const toggle = screen.getByRole('button', { name: '4 changes to fix-relay · Show them' });
    await fireEvent.click(toggle);
    expect(screen.getByText('#42 opened')).toBeInTheDocument();
  });

  it('exposes a <time> element and Copy act button per message turn', () => {
    const turn = makeTurn({ text: 'Copyable reply' });
    const { container } = render(ChatTurn, { props: { turn } });

    const timeEl = container.querySelector('time');
    expect(timeEl).toBeTruthy();
    expect(timeEl).toHaveAttribute('datetime', '2026-11-27T10:15:00Z');
    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument();
  });
});

describe('ChatTranscript', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('exposes role="log", sets aria-busy while streaming, groups same-side turns, and separates days', () => {
    const turns: LocalChatTurn[] = [
      makeTurn({
        turn_id: 't-day1',
        author: 'planner',
        model: 'sonnet',
        text: 'Yesterday message',
        at: '2026-11-26T20:00:00Z',
      }),
      makeTurn({
        turn_id: 't-day2-a',
        author: 'planner',
        model: 'sonnet',
        text: 'Today first',
        at: '2026-11-27T09:00:00Z',
      }),
      makeTurn({
        turn_id: 't-day2-b',
        author: 'planner',
        model: 'sonnet',
        status: 'streaming',
        text: 'Today grouped reply',
        at: '2026-11-27T09:01:00Z',
      }),
    ];

    const { container } = render(ChatTranscript, { props: { turns } });

    const log = screen.getByRole('log');
    expect(log).toHaveAttribute('aria-busy', 'true');
    // Day divider separates 2026-11-26 from 2026-11-27.
    expect(container.querySelectorAll('[data-testid="chat-day-divider"]')).toHaveLength(2);
    // Second same-day role turn is grouped and omits the repeated author header.
    const renderedTurns = container.querySelectorAll('[data-testid="chat-turn"]');
    expect(renderedTurns[2]).toHaveClass('chat-turn--grouped');
    expect(renderedTurns[2].querySelector('.chat-turn__author')).toBeNull();
  });

  it('uses a positioned-box visually-hidden rule for announcements rather than display:none', () => {
    render(ChatTranscript, {
      props: {
        turns: [],
        announcement: 'Reply completed',
      },
    });

    const srRule = ruleOf(TRANSCRIPT_STYLE, '.sr-only');
    expect(srRule).toMatch(/position:\s*absolute/);
    expect(srRule).toMatch(/width:\s*1px/);
    expect(srRule).toMatch(/height:\s*1px/);
    expect(srRule).toMatch(/overflow:\s*hidden/);
    expect(srRule).not.toMatch(/display:\s*none/);
    expect(srRule).not.toMatch(/visibility:\s*hidden/);

    const region = screen.getByTestId('chat-announcement');
    expect(region).toHaveAttribute('aria-live', 'polite');
    expect(region.textContent).toContain('Reply completed');
  });

  it('shows the New reply pill when scrolled away from bottom instead of auto-scrolling', async () => {
    const initialTurns = [makeTurn({ turn_id: 't-1', text: 'Initial' })];
    const { rerender } = render(ChatTranscript, { props: { turns: initialTurns } });

    const log = screen.getByRole('log');
    Object.defineProperty(log, 'scrollHeight', { value: 1000, configurable: true });
    Object.defineProperty(log, 'clientHeight', { value: 300, configurable: true });
    Object.defineProperty(log, 'scrollTop', { value: 100, writable: true, configurable: true });
    await fireEvent.scroll(log);

    await rerender({
      turns: [...initialTurns, makeTurn({ turn_id: 't-2', text: 'Incoming reply' })],
    });

    const pill = screen.getByRole('button', { name: 'New reply' });
    expect(pill).toBeInTheDocument();
  });

  it('loads an older page without shifting visible turns in scrollTop', async () => {
    const initialTurns = [makeTurn({ turn_id: 't-2', text: 'Current turn' })];
    let currentTurns = initialTurns;
    let height = 400;
    const onLoadOlder = vi.fn(async () => {
      height = 650;
      currentTurns = [makeTurn({ turn_id: 't-1', text: 'Older turn' }), ...currentTurns];
    });

    const { rerender } = render(ChatTranscript, {
      props: { turns: currentTurns, hasOlder: true, onloadolder: onLoadOlder },
    });

    const log = screen.getByRole('log');
    Object.defineProperty(log, 'scrollHeight', { get: () => height, configurable: true });
    Object.defineProperty(log, 'scrollTop', { value: 10, writable: true, configurable: true });

    await fireEvent.click(screen.getByRole('button', { name: 'Show it' }));
    expect(onLoadOlder).toHaveBeenCalledTimes(1);
    await rerender({ turns: currentTurns, hasOlder: false, onloadolder: onLoadOlder });
    expect(log.scrollTop).toBe(260);
  });

  it('scrolls focusTurnId into view once present', async () => {
    const turns = [
      makeTurn({ turn_id: 't-older', text: 'Target turn' }),
      makeTurn({ turn_id: 't-latest', text: 'Latest turn' }),
    ];
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;

    render(ChatTranscript, { props: { turns, focusTurnId: 't-older' } });
    await vi.waitFor(() => expect(scrollIntoView).toHaveBeenCalled());
  });
});

import { fireEvent, render, screen } from '@testing-library/svelte';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import Chat from './Chat.svelte';
import { apiClient } from '../api/client';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import type { ChatResource, ChatTarget } from '../chat/types';

function makeChat(overrides: Partial<ChatResource> = {}): ChatResource {
  return {
    id: 'chat-101',
    subject: { kind: 'plan', id: 'fix-relay' },
    role: { name: 'planner', model: 'sonnet' },
    scope: 'plan',
    state: 'chat.open',
    context: {
      knows: [
        { key: 'chat.knows.description', args: {} },
        { key: 'chat.knows.tasks', args: { count: 4 } },
      ],
      as_of: '2026-11-27T10:42:00Z',
    },
    proposal: null,
    turns: [],
    before: null,
    ...overrides,
  };
}

describe('Chat', () => {
  const subject: ChatTarget & { title?: string } = {
    kind: 'plan',
    id: 'fix-relay',
    title: 'Fix Relay Transport',
  };

  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders head, knows line, OS dictation hint, focuses composer on open, and hides returning focus to opener', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'request').mockResolvedValue(makeChat());
    const opener = document.createElement('button');
    opener.textContent = 'Open chat';
    document.body.appendChild(opener);
    const onhide = vi.fn();

    render(Chat, {
      props: {
        subject,
        scope: 'plan',
        opener,
        onhide,
      },
    });

    const chatEl = screen.getByTestId('chat');
    expect(chatEl).toBeInTheDocument();
    expect(await screen.findByText('Fix Relay Transport')).toBeInTheDocument();
    expect(screen.getByText('with planner · sonnet')).toBeInTheDocument();
    expect(screen.getByText(/planner knows:.*as of/)).toBeInTheDocument();
    expect(screen.getByText(/Dictate with your system:/)).toBeInTheDocument();

    const field = screen.getByRole('textbox', { name: 'Your message' });
    await vi.waitFor(() => expect(field).toHaveFocus());

    await fireEvent.keyDown(chatEl, { key: 'Escape' });
    expect(onhide).toHaveBeenCalledTimes(1);
    expect(opener).toHaveFocus();
    opener.remove();
  });

  it('renders empty chat line and starter chips that fill the draft without sending', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockResolvedValue(makeChat());

    render(Chat, { props: { subject, scope: 'plan' } });

    expect(await screen.findByText('Ask planner about the whole plan.')).toBeInTheDocument();
    const starter = screen.getByRole('button', { name: 'What is the riskiest open task?' });
    await fireEvent.click(starter);

    const field = screen.getByRole('textbox', { name: 'Your message' }) as HTMLTextAreaElement;
    expect(field.value).toBe('What is the riskiest open task?');
    expect(req).not.toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/turns'),
      expect.anything(),
    );
  });

  it('persists draft in localStorage across reload, enforces 7000 counter and 8000 send limit, and sends on Enter', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockImplementation(async (method, path) => {
      if (method === 'GET') return makeChat();
      if (method === 'POST' && path.endsWith('/turns')) return { turn_id: 't-op-1' };
      return makeChat();
    });

    const first = render(Chat, { props: { subject, scope: 'plan' } });
    const field1 = (await first.findByRole('textbox', {
      name: 'Your message',
    })) as HTMLTextAreaElement;

    await fireEvent.input(field1, { target: { value: 'a'.repeat(7005) } });
    expect(screen.getByText('7005 of 8000')).toBeInTheDocument();

    await fireEvent.input(field1, { target: { value: 'a'.repeat(8001) } });
    expect(screen.getByRole('button', { name: 'Send' })).toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByRole('button', { name: 'Send' })).toHaveAccessibleDescription(
      'At most 8000 characters.',
    );
    expect(field1).toHaveAccessibleDescription('At most 8000 characters.');

    await fireEvent.input(field1, { target: { value: 'Persisted question' } });
    first.unmount();

    // Reload mounts a fresh instance and restores draft from localStorage.
    render(Chat, { props: { subject, scope: 'plan' } });
    const field2 = (await screen.findByRole('textbox', {
      name: 'Your message',
    })) as HTMLTextAreaElement;
    expect(field2.value).toBe('Persisted question');

    await fireEvent.keyDown(field2, { key: 'Enter' });
    await vi.waitFor(() =>
      expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat/turns', {
        text: 'Persisted question',
        client_turn_id: expect.any(String),
      }),
    );
    expect(field2.value).toBe('');
  });

  it('shows Stop while a role reply streams and keeps focus in composer on stop', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockImplementation(async (method, path) => {
      if (method === 'GET') {
        return makeChat({
          state: 'chat.reply',
          turns: [
            {
              turn_id: 't-reply',
              kind: 'message',
              author: 'planner',
              status: 'streaming',
              format: 'markdown',
              untrusted: false,
              model: 'sonnet',
              text: 'Working on it',
              steps: [],
              refs: [],
              at: '2026-11-27T10:43:00Z',
              reason: null,
              lines: [],
            },
          ],
        });
      }
      return makeChat();
    });

    render(Chat, { props: { subject, scope: 'plan' } });

    const stopBtn = await screen.findByRole('button', { name: 'Stop the reply' });
    const field = screen.getByRole('textbox', { name: 'Your message' });
    await fireEvent.click(stopBtn);

    await vi.waitFor(() =>
      expect(req).toHaveBeenCalledWith(
        'POST',
        '/api/v1/plans/fix-relay/chat/turns/t-reply/stop',
        {},
      ),
    );
    expect(field).toHaveFocus();
  });

  it('offers # references list and inserts selected reference id, closing on Escape without hiding chat', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    vi.spyOn(apiClient, 'request').mockResolvedValue(
      makeChat({
        turns: [
          {
            turn_id: 't-1',
            kind: 'message',
            author: 'planner',
            status: 'complete',
            format: 'markdown',
            untrusted: false,
            model: 'sonnet',
            text: 'Check TASK-4.',
            steps: [],
            refs: [{ kind: 'task', id: 'TASK-4', subject: { kind: 'plan', id: 'fix-relay' } }],
            at: '2026-11-27T10:43:00Z',
            reason: null,
            lines: [],
          },
        ],
      }),
    );
    const onhide = vi.fn();

    render(Chat, { props: { subject, scope: 'plan', onhide } });
    const field = (await screen.findByRole('textbox', {
      name: 'Your message',
    })) as HTMLTextAreaElement;

    await fireEvent.input(field, { target: { value: 'Check #' } });
    expect(screen.getByText('Refer to')).toBeInTheDocument();

    await fireEvent.keyDown(screen.getByTestId('chat'), { key: 'Escape' });
    expect(screen.queryByText('Refer to')).toBeNull();
    expect(onhide).not.toHaveBeenCalled();

    await fireEvent.input(field, { target: { value: 'Check #TA' } });
    await fireEvent.click(screen.getByRole('option', { name: /TASK-4/ }));
    expect(field.value).toBe('Check TASK-4 ');
  });

  it('confirms New chat only while a proposal is open and marks archive mode read-only with active composer', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockResolvedValue(
      makeChat({
        proposal: {
          turn_id: 't-prop',
          reply_turn_id: 't-1',
          kind: 'revision',
          summary: 'Update tasks',
          applies: null,
          before: 'a',
          after: 'b',
          state: 'open',
          finding: null,
        },
      }),
    );
    const onnew = vi.fn();

    render(Chat, { props: { subject, scope: 'archive', archive: true, onnew } });

    expect(await screen.findByText('read only: the archive stays as it is')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Your message' })).toBeEnabled();

    await fireEvent.click(screen.getByRole('button', { name: 'New chat' }));
    expect(await screen.findByText('Start a new chat?')).toBeInTheDocument();

    const buttons = screen.getAllByRole('button', { name: 'New chat' });
    await fireEvent.click(buttons[buttons.length - 1]);
    await vi.waitFor(() => {
      expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat/end', {});
      expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat');
    });
    expect(onnew).toHaveBeenCalledTimes(1);
  });

  it('starts a new chat immediately without confirmation when no proposal is open', async () => {
    vi.spyOn(apiClient, 'subscribePathEvents').mockReturnValue(() => {});
    const req = vi.spyOn(apiClient, 'request').mockResolvedValue(makeChat({ proposal: null }));
    const onnew = vi.fn();

    render(Chat, { props: { subject, scope: 'plan', onnew } });
    await screen.findByText('Fix Relay Transport');

    await fireEvent.click(screen.getByRole('button', { name: 'New chat' }));
    expect(screen.queryByText('Start a new chat?')).toBeNull();
    await vi.waitFor(() =>
      expect(req).toHaveBeenCalledWith('POST', '/api/v1/plans/fix-relay/chat'),
    );
    expect(onnew).toHaveBeenCalledTimes(1);
  });
});

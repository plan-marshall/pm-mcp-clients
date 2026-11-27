import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { render, screen } from '@testing-library/svelte';
import { describe, it, expect, beforeEach } from 'vitest';
import ChatMarkdown from './ChatMarkdown.svelte';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import type { TurnRef } from '../chat/types';

const source = readFileSync(
  resolve(process.cwd(), 'src/lib/components/ChatMarkdown.svelte'),
  'utf8',
);

describe('ChatMarkdown', () => {
  beforeEach(() => {
    currentLocale.set('en');
    messagesStore.set(loadMessages('en'));
  });

  it('contains no raw HTML sinks ({@html) in source', () => {
    expect(source).not.toContain('{@html');
    expect(source).not.toContain('innerHTML');
  });

  it('renders closed-subset formatting safely as DOM nodes', () => {
    const md = [
      'Paragraph with **bold**, _italic_, and `inline code`.',
      '',
      '> Quoted text with **emphasis**',
      '',
      '- First item',
      '- Second item',
    ].join('\n');

    const { container } = render(ChatMarkdown, { props: { text: md } });

    expect(container.querySelector('strong')?.textContent).toBe('bold');
    expect(container.querySelector('em')?.textContent).toBe('italic');
    expect(container.querySelector('code')?.textContent).toBe('inline code');
    expect(container.querySelector('blockquote')).toBeTruthy();
    expect(container.querySelectorAll('ul li')).toHaveLength(2);
  });

  it('renders code blocks with language label, Copy act, and sideways scroll wrapper without syntax colours', () => {
    const md = '```typescript\nconst value = 42;\n```';
    const { container } = render(ChatMarkdown, { props: { text: md } });

    expect(screen.getByText('typescript')).toBeInTheDocument();
    expect(container.querySelector('pre code')?.textContent).toContain('const value = 42;');
    expect(screen.getByRole('button', { name: /copy/i })).toBeInTheDocument();
    expect(container.querySelector('[class*="hljs"]')).toBeNull();
  });

  it('renders fenced mermaid blocks behind the Mermaid hook as a mermaid-labelled code block', () => {
    const md = '```mermaid\ngraph TD\nA-->B\n```';
    const { container } = render(ChatMarkdown, { props: { text: md } });

    expect(screen.getByText('mermaid')).toBeInTheDocument();
    expect(container.querySelector('pre code')?.textContent).toContain('graph TD');
  });

  it('renders hostile inputs (javascript:, data:, <script>, nested HTML, images) strictly as text nodes or safe links', () => {
    const hostile = [
      'Click [bad](javascript:alert(1)) or [data](data:text/html,<script>1</script>).',
      '',
      '<script>alert("xss")</script>',
      '',
      '<div><span><b>nested</b></span></div>',
      '',
      '![alt text](https://example.com/pic.png)',
      '',
      '[Docs](https://docs.example.org/guide)',
    ].join('\n');

    const { container } = render(ChatMarkdown, { props: { text: hostile } });

    // No raw script, injected HTML elements, or loaded <img> tags ever exist.
    expect(container.querySelector('script')).toBeNull();
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('b')).toBeNull();
    expect(container.querySelectorAll('a')).toSatisfy((links: NodeListOf<HTMLAnchorElement>) =>
      Array.from(links).every(
        (link) => link.href.startsWith('https://') || link.href.startsWith('http://'),
      ),
    );
    expect(container.textContent).toContain('javascript:alert(1)');
    expect(container.textContent).toContain('<script>alert("xss")</script>');
    expect(container.textContent).toContain('<div><span><b>nested</b></span></div>');
    // Host is visible beside link text that omits it.
    expect(container.textContent).toContain('docs.example.org');
    expect(container.textContent).toContain('example.com');
  });

  it('wraps over-long tables in a horizontal scroll region', () => {
    const table = [
      '| ColA | ColB | ColC | ColD |',
      '| ---- | ---- | ---- | ---- |',
      '| verylongvalueone | verylongvaluetwo | verylongvaluethree | verylongvaluefour |',
    ].join('\n');

    const { container } = render(ChatMarkdown, { props: { text: table } });

    const wrap = container.querySelector('.chat-markdown__table-wrap');
    expect(wrap).toBeTruthy();
    expect(wrap?.querySelector('table thead tr th')).toBeTruthy();
    expect(wrap?.querySelector('table tbody tr td')?.textContent).toBe('verylongvalueone');
  });

  it('renders unfinished fences as plain text while streaming, then as a code block once complete', async () => {
    const unfinished = 'Here is code:\n\n```js\nconst x = 1;';
    const { container, rerender } = render(ChatMarkdown, {
      props: { text: unfinished, streaming: true },
    });

    expect(container.querySelector('pre')).toBeNull();
    expect(container.textContent).toContain('```js');

    await rerender({ text: `${unfinished}\n\`\`\``, streaming: false });
    expect(container.querySelector('pre code')?.textContent).toContain('const x = 1;');
  });

  it('turns matching turn refs into Link chips outside code, keeping refs inside code plain', () => {
    const refs: TurnRef[] = [
      { kind: 'task', id: 'TASK-4', subject: { kind: 'plan', id: 'fix-relay' } },
      { kind: 'file', id: 'src/App.svelte:12', subject: { kind: 'plan', id: 'fix-relay' } },
    ];
    const md = 'Check TASK-4 in `TASK-4` and edit src/App.svelte:12.';

    const { container } = render(ChatMarkdown, {
      props: { text: md, refs, subject: { kind: 'plan', id: 'fix-relay' } },
    });

    const chipLinks = container.querySelectorAll('a.link--chip');
    expect(chipLinks).toHaveLength(2);
    expect(chipLinks[0].textContent).toContain('TASK-4');
    expect(chipLinks[0]).toHaveAttribute('href', '/plans/fix-relay/tasks/TASK-4?chat=open');
    expect(chipLinks[1].textContent).toContain('src/App.svelte:12');
    expect(chipLinks[1]).toHaveAttribute(
      'href',
      '/plans/fix-relay?tab=changes&file=src%2FApp.svelte&chat=open',
    );
    expect(container.querySelector('code')?.textContent).toBe('TASK-4');
  });
});

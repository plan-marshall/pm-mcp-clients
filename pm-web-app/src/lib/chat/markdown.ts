import { marked, type Token, type Tokens } from 'marked';
import type { ChatSubject, TurnRef } from './types';

export interface TextSegment {
  kind: 'text' | 'ref';
  text: string;
  ref?: TurnRef;
}

export interface MermaidRenderResult {
  kind: 'code';
  lang: string;
  code: string;
}

const REFERENCE_CHIP_KINDS = new Set<TurnRef['kind']>(['task', 'file', 'decision', 'plan']);

/**
 * Checks whether a URL uses an allowed external web protocol (`http:` or `https:`).
 *
 * @param raw raw address from Markdown link or image token
 * @returns parsed URL when safe, or `null` otherwise
 */
export function parseSafeWebUrl(raw: string): URL | null {
  if (!raw || raw.startsWith('//')) {
    return null;
  }
  try {
    const url = new URL(raw);
    if (url.protocol === 'http:' || url.protocol === 'https:') {
      return url;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Ensures an external link shows the target's host when its label omits it (`chat.adoc#markdown`).
 *
 * @param label visible link or alt label
 * @param url parsed `http:` / `https:` target URL
 * @returns label with `(host)` appended if not already named
 */
export function formatExternalLabel(label: string, url: URL): string {
  const clean = label.trim() || url.href;
  const host = url.host;
  if (!host) {
    return clean;
  }
  if (clean.toLowerCase().includes(host.toLowerCase())) {
    return clean;
  }
  return `${clean} (${host})`;
}

function openQueryParam(kind: string, id: string): string {
  const combined = `${kind}:${id}`;
  return encodeURIComponent(combined);
}

function planRefHref(id: string, fallbackSubject?: ChatSubject | null): string {
  if (fallbackSubject?.kind === 'epic' && id) {
    return `/epics/${encodeURIComponent(fallbackSubject.id)}?open=${openQueryParam('plan', id)}&chat=open`;
  }
  return id ? `/plans/${encodeURIComponent(id)}` : '/overview';
}

function decisionRefHref(id: string, fallbackSubject?: ChatSubject | null): string {
  if (fallbackSubject?.kind === 'plan' && id) {
    return `/plans/${encodeURIComponent(fallbackSubject.id)}?open=${openQueryParam('decision', id)}&chat=open`;
  }
  if (fallbackSubject?.kind === 'epic' && id) {
    return `/epics/${encodeURIComponent(fallbackSubject.id)}?open=${openQueryParam('decision', id)}&chat=open`;
  }
  return id ? `/decisions/${encodeURIComponent(id)}` : '/decisions';
}

function taskRefHref(id: string, target?: ChatSubject | null): string {
  if (target?.kind === 'plan' && id) {
    return `/plans/${encodeURIComponent(target.id)}/tasks/${encodeURIComponent(id)}?chat=open`;
  }
  return id ? `/plans?open=${openQueryParam('task', id)}` : '/overview';
}

function fileRefHref(id: string, target?: ChatSubject | null): string {
  const colon = id.lastIndexOf(':');
  const filePath = colon > 0 && /^\d+$/.test(id.slice(colon + 1)) ? id.slice(0, colon) : id;
  if (target?.kind === 'plan' && filePath) {
    return `/plans/${encodeURIComponent(target.id)}?tab=changes&file=${encodeURIComponent(filePath)}&chat=open`;
  }
  return '/overview';
}

function subjectChatHref(target?: ChatSubject | null): string {
  if (target?.kind === 'plan') {
    return `/plans/${encodeURIComponent(target.id)}?chat=open`;
  }
  if (target?.kind === 'epic') {
    return `/epics/${encodeURIComponent(target.id)}?chat=open`;
  }
  return '/overview';
}

/**
 * Computes the route href for a structured `TurnRef` chip (`PrChip`-style `Link`).
 *
 * @param ref turn reference
 * @param fallbackSubject optional enclosing chat subject
 * @returns app-internal href
 */
export function hrefForTurnRef(ref: TurnRef, fallbackSubject?: ChatSubject | null): string {
  const target = ref.subject ?? fallbackSubject;
  const id = ref.id ?? '';
  switch (ref.kind) {
    case 'plan':
      return planRefHref(id, fallbackSubject);
    case 'decision':
      return decisionRefHref(id, fallbackSubject);
    case 'task':
      return taskRefHref(id, target);
    case 'file':
      return fileRefHref(id, target);
    case 'hold':
    case 'act':
      return subjectChatHref(target);
  }
}

type RefWithId = TurnRef & { id: string };

function findEarliestRefMatch(
  text: string,
  cursor: number,
  candidates: readonly RefWithId[],
): { index: number; ref: RefWithId } | null {
  let bestIndex = -1;
  let bestRef: RefWithId | null = null;

  for (const candidate of candidates) {
    const idx = text.indexOf(candidate.id, cursor);
    if (idx < 0) continue;
    const isEarlier = bestIndex === -1 || idx < bestIndex;
    const isLongerTie =
      idx === bestIndex && bestRef !== null && candidate.id.length > bestRef.id.length;
    if (isEarlier || isLongerTie) {
      bestIndex = idx;
      bestRef = candidate;
    }
  }

  return bestIndex >= 0 && bestRef ? { index: bestIndex, ref: bestRef } : null;
}

/**
 * Splits a plain text string into plain segments and matched `TurnRef` chips using linear substring
 * matching (no regex compilation from untrusted inputs).
 *
 * @param text text content from a non-code Markdown token
 * @param refs turn references to surface as chips
 * @returns ordered text/ref segments
 */
export function splitTextWithRefs(text: string, refs: readonly TurnRef[]): TextSegment[] {
  if (!text || refs.length === 0) {
    return text ? [{ kind: 'text', text }] : [];
  }

  const candidates = refs
    .filter((ref): ref is RefWithId => Boolean(ref.id && REFERENCE_CHIP_KINDS.has(ref.kind)))
    .sort((a, b) => b.id.length - a.id.length);

  if (candidates.length === 0) {
    return [{ kind: 'text', text }];
  }

  const segments: TextSegment[] = [];
  let cursor = 0;

  while (cursor < text.length) {
    const match = findEarliestRefMatch(text, cursor, candidates);
    if (!match) {
      segments.push({ kind: 'text', text: text.slice(cursor) });
      break;
    }
    if (match.index > cursor) {
      segments.push({ kind: 'text', text: text.slice(cursor, match.index) });
    }
    segments.push({ kind: 'ref', text: match.ref.id, ref: match.ref });
    cursor = match.index + match.ref.id.length;
  }

  return segments;
}

/**
 * Detects whether a Markdown code block targets Mermaid.
 *
 * @param lang fence language string
 * @returns `true` for `mermaid`
 */
export function isMermaidLanguage(lang?: string): boolean {
  return (lang ?? '').trim().toLowerCase() === 'mermaid';
}

/**
 * Single hook for rendering fenced Mermaid blocks; currently falls back to a labelled code block
 * ready to be swapped for the SVG diagram renderer without touching token traversal.
 *
 * @param code Mermaid source text
 * @returns interim code-block descriptor labelled `mermaid`
 */
export function renderMermaidBlock(code: string): MermaidRenderResult {
  return {
    kind: 'code',
    lang: 'mermaid',
    code,
  };
}

function fenceCharOf(trimmed: string): '`' | '~' | '' {
  if (trimmed.startsWith('```')) return '`';
  if (trimmed.startsWith('~~~')) return '~';
  return '';
}

function countFenceRun(trimmed: string, ch: string): number {
  let count = 0;
  while (count < trimmed.length && trimmed[count] === ch) {
    count += 1;
  }
  return count;
}

function isClosingFenceLine(trimmed: string, ch: string, openLen: number): boolean {
  return (
    trimmed.startsWith(ch.repeat(openLen)) &&
    trimmed.slice(openLen).replaceAll(ch, '').trim() === ''
  );
}

/**
 * Finds the start index of a trailing unclosed code fence (` ``` ` or `~~~`) so streaming turns
 * keep unfinished fences as plain text until closing markers arrive.
 */
function findUnclosedFenceStart(source: string): number {
  const lines = source.split('\n');
  let offset = 0;
  let openStart = -1;
  let openChar = '';
  let openLen = 0;

  for (const line of lines) {
    const trimmed = line.trimStart();
    if (openStart < 0) {
      const ch = fenceCharOf(trimmed);
      if (ch) {
        openStart = offset;
        openChar = ch;
        openLen = countFenceRun(trimmed, ch);
      }
    } else if (isClosingFenceLine(trimmed, openChar, openLen)) {
      openStart = -1;
      openChar = '';
      openLen = 0;
    }
    offset += line.length + 1;
  }

  return openStart;
}

/**
 * Tokenizes reply Markdown using `marked.lexer` cleanly without constructing HTML strings. When
 * `streaming` is true, any trailing unclosed code fence stays plain text until closed.
 *
 * @param source raw Markdown string
 * @param streaming whether deltas are still arriving
 * @returns top-level `marked` tokens
 */
export function tokenizeMarkdown(source: string, streaming = false): Token[] {
  if (!source) {
    return [];
  }
  if (streaming) {
    const unclosedStart = findUnclosedFenceStart(source);
    if (unclosedStart >= 0) {
      const completedPrefix = source.slice(0, unclosedStart);
      const pendingTail = source.slice(unclosedStart);
      const prefixTokens = completedPrefix
        ? Array.from(marked.lexer(completedPrefix, { gfm: true, breaks: false }))
        : [];
      const tailToken: Tokens.Paragraph = {
        type: 'paragraph',
        raw: pendingTail,
        text: pendingTail,
        tokens: [{ type: 'text', raw: pendingTail, text: pendingTail }],
      };
      return [...prefixTokens, tailToken];
    }
  }
  return Array.from(marked.lexer(source, { gfm: true, breaks: false }));
}

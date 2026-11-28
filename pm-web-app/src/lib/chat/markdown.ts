import { marked, type Token, type Tokens } from 'marked';
import { isWebAddress } from '../components/ExternalLink.svelte';
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
const HOST_CHAR_RE = /[a-z0-9.-]/i;
const IDENTIFIER_CHAR_RE = /[A-Za-z0-9_-]/;
const FILE_PREFIX_BOUNDARY_RE = /[A-Za-z0-9_./-]/;

/**
 * Checks whether a URL uses an allowed canonical external web protocol (`http://` or `https://`).
 *
 * @param raw raw address from Markdown link or image token
 * @returns parsed URL when safe and canonical, or `null` otherwise
 */
export function parseSafeWebUrl(raw: string): URL | null {
  if (!isWebAddress(raw)) {
    return null;
  }
  try {
    return new URL(raw.trim());
  } catch {
    return null;
  }
}

function labelContainsHost(clean: string, host: string): boolean {
  const lowerClean = clean.toLowerCase();
  const lowerHost = host.toLowerCase();
  let start = 0;
  while (start < lowerClean.length) {
    const idx = lowerClean.indexOf(lowerHost, start);
    if (idx < 0) return false;
    const before = idx > 0 ? lowerClean[idx - 1] : '';
    const afterIndex = idx + lowerHost.length;
    const after = afterIndex < lowerClean.length ? lowerClean[afterIndex] : '';
    if ((!before || !HOST_CHAR_RE.test(before)) && (!after || !HOST_CHAR_RE.test(after))) {
      return true;
    }
    start = idx + 1;
  }
  return false;
}

/**
 * Ensures an external link shows the target's host when its label omits it (`chat.adoc#markdown`).
 *
 * @param label visible link or alt label
 * @param url parsed `http:` / `https:` target URL
 * @returns label with `(host)` appended if not already named as a distinct hostname
 */
export function formatExternalLabel(label: string, url: URL): string {
  const clean = label.trim() || url.href;
  const host = url.host;
  if (!host) {
    return clean;
  }
  if (labelContainsHost(clean, host)) {
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

function hasRefBoundaries(
  text: string,
  index: number,
  length: number,
  kind: TurnRef['kind'],
): boolean {
  const before = index > 0 ? text[index - 1] : '';
  const afterIndex = index + length;
  const after = afterIndex < text.length ? text[afterIndex] : '';
  const beforeInvalid =
    kind === 'file' ? FILE_PREFIX_BOUNDARY_RE.test(before) : IDENTIFIER_CHAR_RE.test(before);
  if (before && beforeInvalid) {
    return false;
  }
  if (after && IDENTIFIER_CHAR_RE.test(after)) {
    return false;
  }
  if (
    kind === 'file' &&
    (after === '/' || (after === '.' && IDENTIFIER_CHAR_RE.test(text[afterIndex + 1] ?? '')))
  ) {
    return false;
  }
  return true;
}

function findNextValidRefIndex(text: string, cursor: number, candidate: RefWithId): number {
  let searchFrom = cursor;
  while (searchFrom <= text.length - candidate.id.length) {
    const idx = text.indexOf(candidate.id, searchFrom);
    if (idx < 0) return -1;
    if (hasRefBoundaries(text, idx, candidate.id.length, candidate.kind)) {
      return idx;
    }
    searchFrom = idx + 1;
  }
  return -1;
}

function isPreferredRefMatch(
  idx: number,
  candidate: RefWithId,
  bestIndex: number,
  bestRef: RefWithId | null,
): boolean {
  if (bestIndex === -1 || idx < bestIndex) return true;
  return idx === bestIndex && bestRef !== null && candidate.id.length > bestRef.id.length;
}

function findEarliestRefMatch(
  text: string,
  cursor: number,
  candidates: readonly RefWithId[],
): { index: number; ref: RefWithId } | null {
  let bestIndex = -1;
  let bestRef: RefWithId | null = null;

  for (const candidate of candidates) {
    const idx = findNextValidRefIndex(text, cursor, candidate);
    if (idx >= 0 && isPreferredRefMatch(idx, candidate, bestIndex, bestRef)) {
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
  const run = countFenceRun(trimmed, ch);
  return run >= openLen && trimmed.slice(run).trim() === '';
}

function countLeadingSpaces(text: string): number {
  let spaces = 0;
  while (spaces < text.length && text[spaces] === ' ') {
    spaces += 1;
  }
  return spaces;
}

function stripFenceContainerPrefix(line: string): string | null {
  let rest = line;
  while (true) {
    const spaces = countLeadingSpaces(rest);
    if (spaces > 3) return null;
    const trimmed = rest.slice(spaces);
    if (!trimmed.startsWith('>')) {
      return trimmed;
    }
    rest = trimmed.startsWith('> ') ? trimmed.slice(2) : trimmed.slice(1);
  }
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
    const candidate = stripFenceContainerPrefix(line);
    if (candidate !== null) {
      if (openStart < 0) {
        const ch = fenceCharOf(candidate);
        if (ch) {
          openStart = offset;
          openChar = ch;
          openLen = countFenceRun(candidate, ch);
        }
      } else if (isClosingFenceLine(candidate, openChar, openLen)) {
        openStart = -1;
        openChar = '';
        openLen = 0;
      }
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
      const tailToken = {
        type: 'code_pending',
        raw: pendingTail,
        text: pendingTail,
      } as unknown as Token;
      return [...prefixTokens, tailToken];
    }
  }
  return Array.from(marked.lexer(source, { gfm: true, breaks: false }));
}

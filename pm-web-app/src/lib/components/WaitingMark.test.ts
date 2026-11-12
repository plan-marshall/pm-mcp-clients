import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import WaitingMark from './WaitingMark.svelte';

/**
 * `WaitingMark` (components-brand.adoc, WaitingMark): the clipboard whose three lines are ticked
 * one after another, beside the text that says what runs. The text sits in a polite status line,
 * the mark is decorative, and the motion lives in the component's own stylesheet.
 */

const COMPONENT_PATH = path.resolve(__dirname, 'WaitingMark.svelte');

/** The waiting line the chat shows before the role's first word (brand-mark.adoc, Where It Appears). */
const LABEL = 'replan is reading the whole plan…';

/**
 * Generated label input: a random word set inside markup a careless sink would interpret.
 *
 * The web app has no generator library, and a test dependency needs the operator's decision, so
 * the input is composed here from random parts rather than written as one literal.
 *
 * @returns a label that carries an element and an event handler as plain characters
 */
function generatedMarkupLabel(): string {
  const word = Array.from({ length: 6 + Math.floor(Math.random() * 6) }, () =>
    String.fromCodePoint(0x61 + Math.floor(Math.random() * 26)),
  ).join('');
  return `<img src="x" onerror="window.${word}=1"><b>${word}</b>`;
}

/**
 * @param css a stylesheet
 * @param opener the at-rule whose block is wanted
 * @returns the block's content, braces matched
 */
function blockOf(css: string, opener: string): string {
  const at = css.indexOf(opener);
  if (at < 0) throw new Error(`no "${opener}" block`);
  const open = css.indexOf('{', at + opener.length);
  let depth = 0;
  for (let i = open; i < css.length; i += 1) {
    if (css[i] === '{') depth += 1;
    if (css[i] === '}') {
      depth -= 1;
      if (depth === 0) return css.slice(open + 1, i);
    }
  }
  throw new Error(`unbalanced "${opener}" block`);
}

/**
 * @param css a stylesheet without nested blocks
 * @param selector the selector list of the rule, as written
 * @returns the declarations of the first rule with exactly that selector list
 */
function ruleOf(css: string, selector: string): string {
  const escaped = selector
    .split(',')
    .map((part) => part.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    .join('\\s*,\\s*');
  // The selector list must open the rule, so a selector never matches the tail of a longer list.
  const match = new RegExp(`(^|[{}])\\s*${escaped}\\s*\\{([^}]*)\\}`).exec(css);
  if (!match) throw new Error(`no rule for "${selector}"`);
  return match[2];
}

describe('WaitingMark', () => {
  const source = fs.readFileSync(COMPONENT_PATH, 'utf-8');
  // Comments are dropped, so a rule is found by its selector alone.
  const stylesheet = (/<style>([\s\S]*?)<\/style>/.exec(source)?.[1] ?? '').replace(/\/\*[\s\S]*?\*\//g, '');

  it('carries its label as the text of a polite status line', () => {
    render(WaitingMark, { props: { label: LABEL } });
    const line = screen.getByRole('status');

    expect(line).toHaveTextContent(LABEL);
    expect(line).toBe(screen.getByTestId('waiting-mark'));
  });

  it('draws the mark decoratively, at 20 px, in the current colour', () => {
    render(WaitingMark, { props: { label: LABEL } });
    const mark = screen.getByTestId('waiting-mark-glyph');

    expect(mark).toHaveAttribute('aria-hidden', 'true');
    expect(mark).toHaveAttribute('width', '20');
    expect(mark).toHaveAttribute('height', '20');
    expect(mark).toHaveAttribute('viewBox', '0 0 24 24');
    expect(mark).toHaveAttribute('stroke', 'currentColor');
    expect(mark).toHaveAttribute('stroke-width', '1.5');
    expect(mark).toHaveAttribute('stroke-linecap', 'round');
    expect(mark).toHaveAttribute('stroke-linejoin', 'round');
    expect(ruleOf(stylesheet, '.waiting-mark')).toMatch(/color:\s*var\(--color-text-secondary\)\s*;/);
  });

  it('draws each open dot and each tick of the three lines as an element of its own', () => {
    render(WaitingMark, { props: { label: LABEL } });
    const mark = screen.getByTestId('waiting-mark-glyph');

    for (const line of [1, 2, 3]) {
      expect(mark.querySelectorAll(`.waiting-mark__dot--${line}`)).toHaveLength(1);
      expect(mark.querySelectorAll(`.waiting-mark__tick--${line}`)).toHaveLength(1);
      expect(ruleOf(stylesheet, `.waiting-mark__tick--${line}`)).toMatch(
        new RegExp(`animation:\\s*waiting-mark-tick-${line}\\s+3\\.6s\\b`),
      );
      expect(ruleOf(stylesheet, `.waiting-mark__dot--${line}`)).toMatch(
        new RegExp(`animation:\\s*waiting-mark-dot-${line}\\s+3\\.6s\\b`),
      );
    }
  });

  it('stands still and unticked where reduced motion is asked for', () => {
    // jsdom computes no media, so the rule is read from the component's stylesheet.
    const reduced = blockOf(stylesheet, '@media (prefers-reduced-motion: reduce)');

    expect(ruleOf(reduced, '.waiting-mark__tick, .waiting-mark__dot')).toMatch(/animation:\s*none\s*;/);
    expect(ruleOf(reduced, '.waiting-mark__tick')).toMatch(/stroke-dashoffset:\s*1\s*;/);
    expect(ruleOf(reduced, '.waiting-mark__tick')).toMatch(/opacity:\s*0\s*;/);
    expect(ruleOf(reduced, '.waiting-mark__dot')).toMatch(/opacity:\s*1\s*;/);
  });

  describe('the start frame at 20 px', () => {
    /** The rest state of reduced motion: every dot shown, every tick undrawn. */
    const reduced = (): string => blockOf(stylesheet, '@media (prefers-reduced-motion: reduce)');

    /**
     * @param keyframes the keyframes of one dot or tick
     * @returns the declarations of the keyframe that opens a cycle, at 0% alone or in a list
     */
    function frameAtZero(keyframes: string): string {
      const match = /(^|[{}])\s*0%\s*(,[^{]*)?\{([^}]*)\}/.exec(keyframes);
      if (!match) throw new Error('no 0% keyframe');
      return match[3];
    }

    it('starts the cycle from the mark the operator sees at 20 px', () => {
      render(WaitingMark, { props: { label: LABEL } });
      const mark = screen.getByTestId('waiting-mark-glyph');

      expect(mark).toHaveAttribute('width', '20');
      expect(mark).toHaveAttribute('height', '20');
    });

    it.each([1, 2, 3])('opens the cycle of tick %i undrawn, as reduced motion rests', (line) => {
      const rest = ruleOf(reduced(), '.waiting-mark__tick');
      const first = frameAtZero(blockOf(stylesheet, `@keyframes waiting-mark-tick-${line}`));

      expect(rest).toMatch(/stroke-dashoffset:\s*1\s*;/);
      expect(rest).toMatch(/opacity:\s*0\s*;/);
      expect(first).toMatch(/stroke-dashoffset:\s*1\s*;/);
      expect(first).toMatch(/opacity:\s*0\s*;/);
    });

    it.each([1, 2, 3])('opens the cycle of dot %i shown, as reduced motion rests', (line) => {
      const rest = ruleOf(reduced(), '.waiting-mark__dot');
      const first = frameAtZero(blockOf(stylesheet, `@keyframes waiting-mark-dot-${line}`));

      expect(rest).toMatch(/opacity:\s*1\s*;/);
      expect(first).toMatch(/opacity:\s*1\s*;/);
    });
  });

  it('sets no style attribute anywhere in the rendered tree and no literal colour in the component', () => {
    const { container } = render(WaitingMark, { props: { label: LABEL } });

    expect(container.querySelectorAll('[style]')).toHaveLength(0);
    expect(source).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(source).not.toMatch(/\b(rgb|rgba|hsl|hsla)\(/);
  });

  it('renders the label as text, never as markup', () => {
    const label = generatedMarkupLabel();
    const { container } = render(WaitingMark, { props: { label } });
    const line = screen.getByRole('status');

    expect(line.textContent).toContain(label);
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('b')).toBeNull();
  });
});

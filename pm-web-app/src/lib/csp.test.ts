import { describe, it, expect } from 'vitest';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, relative } from 'node:path';
import config from '../../vite.config';

/*
 * The daemon serves the built app under `default-src 'self'` with `img-src 'self' blob:` and no
 * `style-src` (plan-marshall-documentation: doc/specification/cli-and-security/03-web-server.adoc,
 * Web App). Two things the browser then blocks are easy to reintroduce in this repository and are
 * otherwise only seen downstream, by `WebAppIT` of `pm-mcp-server`: a `data:` URL that Vite inlines
 * for an asset, and a static `style` attribute in markup. A style bound with `style={...}` or a
 * `style:` directive is applied through the CSSOM, which the policy permits. The inline theme script
 * of `src/app.html` is admitted by its hash, which the daemon adds to `script-src`.
 */

// The suite runs from `pm-web-app` under npm and the repository root under a direct call.
const projectRoot = existsSync(join(process.cwd(), 'src/lib'))
  ? process.cwd()
  : join(process.cwd(), 'pm-web-app');

/** The app's own source; `src/test` holds fixtures that model violations on purpose. */
const SOURCE_ROOTS = ['src/lib', 'src/routes'].map((root) => join(projectRoot, root));

/** The page shell every route is rendered into. */
const APP_SHELL = join(projectRoot, 'src/app.html');

/**
 * A static `style` attribute in any HTML form: double or single quoted with no `{` expression
 * inside, or unquoted. Attribute names are case-insensitive in HTML.
 */
const STATIC_STYLE_ATTRIBUTE = /\sstyle\s*=\s*(?:"[^"{]*"|'[^'{]*'|[^\s"'`{>=]+)/i;

/**
 * Any `style` attribute of the page shell. Svelte does not compile `src/app.html`, so a `{` in its
 * value is literal text, not an expression, and every such attribute is static.
 */
const SHELL_STYLE_ATTRIBUTE = /\sstyle\s*=/i;

/**
 * An import that asks Vite for an inlined asset, which `assetsInlineLimit` does not govern. Vite
 * honours the query in any form (`?inline`, `&inline`, `?inline=true`), so the word boundary is
 * the whole predicate, as in Vite's own check.
 */
const INLINE_ASSET_IMPORT = /['"][^'"\n]*[?&]inline\b/;

/**
 * @param directory the directory to walk
 * @param accept whether a file name belongs to the result
 * @returns every accepted file below it, recursively
 */
function filesBelow(directory: string, accept: (name: string) => boolean): string[] {
  const found: string[] = [];
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const full = join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...filesBelow(full, accept));
    } else if (accept(entry.name)) {
      found.push(full);
    }
  }
  return found;
}

/**
 * @param accept whether a file name belongs to the result
 * @returns the accepted files of every source root
 */
function sourceFiles(accept: (name: string) => boolean): string[] {
  return SOURCE_ROOTS.filter((root) => existsSync(root)).flatMap((root) =>
    filesBelow(root, accept),
  );
}

/**
 * @param source the component source or page shell
 * @returns the markup only, without its `<script>` and `<style>` blocks and comments
 */
function markupOf(source: string): string {
  return source
    .replace(/<script\b[\s\S]*?<\/script>/gi, '')
    .replace(/<style\b[\s\S]*?<\/style>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '');
}

/** @param name a file name @returns whether it is app code rather than a test */
function isAppCode(name: string): boolean {
  return /\.(svelte|ts|js)$/.test(name) && !/\.(test|spec)\.(ts|js)$/.test(name);
}

describe('the app under the daemon CSP', () => {
  it('emits every asset as a file, never as an inlined data: URL', () => {
    expect(config.build?.assetsInlineLimit).toBe(0);
  });

  it('imports no asset with the inline query, which bypasses the limit', () => {
    const offenders = sourceFiles(isAppCode)
      .filter((file) => INLINE_ASSET_IMPORT.test(readFileSync(file, 'utf8')))
      .map((file) => relative(projectRoot, file));

    expect(offenders).toEqual([]);
  });

  it('carries no static style attribute in its markup', () => {
    const offenders = sourceFiles((name) => name.endsWith('.svelte'))
      .filter((file) => STATIC_STYLE_ATTRIBUTE.test(markupOf(readFileSync(file, 'utf8'))))
      .map((file) => relative(projectRoot, file));

    expect(offenders).toEqual([]);
  });

  it('carries no style attribute in its page shell', () => {
    expect(existsSync(APP_SHELL)).toBe(true);
    expect(SHELL_STYLE_ATTRIBUTE.test(markupOf(readFileSync(APP_SHELL, 'utf8')))).toBe(false);
  });

  it('treats a brace in the page shell as literal text', () => {
    const shell = `<body style="--label: '{'; color: red"></body>`;

    expect(STATIC_STYLE_ATTRIBUTE.test(shell)).toBe(false);
    expect(SHELL_STYLE_ATTRIBUTE.test(shell)).toBe(true);
  });

  it.each([
    ['double quoted', '<span style="inline-size: 28px"></span>'],
    ['single quoted', "<span style='inline-size: 28px'></span>"],
    ['the opposite quote inside', `<span style="font-family: 'Inter'"></span>`],
    ['unquoted', '<span style=display:none></span>'],
    ['upper case', '<span STYLE="display: none"></span>'],
  ])('detects a static style attribute, %s', (_form, markup) => {
    expect(STATIC_STYLE_ATTRIBUTE.test(markup)).toBe(true);
  });

  it.each([
    ['an expression', '<span style={`--size: ${size}px`}></span>'],
    ['an interpolated value', '<span style="width: {width}px"></span>'],
    ['a directive', '<div style:max-width={limit}></div>'],
    ['a data attribute', '<div data-style="compact"></div>'],
  ])('admits a style that is not static, %s', (_form, markup) => {
    expect(STATIC_STYLE_ATTRIBUTE.test(markup)).toBe(false);
  });

  it.each([
    ["import icon from './icon.svg?inline';", true],
    ["import icon from './icon.svg?url&inline';", true],
    ["import icon from './icon.svg?inline=true';", true],
    ["import icon from './icon.svg?url';", false],
    ["import { inline } from './layout';", false],
  ])('recognises an inline asset import: %s', (line, expected) => {
    expect(INLINE_ASSET_IMPORT.test(line)).toBe(expected);
  });
});

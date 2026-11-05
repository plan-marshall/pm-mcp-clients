import { describe, it, expect } from 'vitest';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, relative } from 'node:path';
import config from '../../vite.config';

/*
 * The daemon serves the built app under `default-src 'self'` with `img-src 'self' blob:` and no
 * `style-src` (plan-marshall-documentation: doc/specification/cli-and-security/03-web-server.adoc,
 * Web App). Two things the browser then blocks are easy to reintroduce in this repository and are
 * otherwise only seen downstream, by `WebAppIT` of `pm-mcp-server`: a `data:` URL that Vite inlines
 * for a small asset, and a static `style` attribute in markup. A style bound with `style={...}` or a
 * `style:` directive is applied through the CSSOM, which the policy permits.
 */

// The suite runs from `pm-web-app` under npm and the repository root under a direct call.
const projectRoot = existsSync(join(process.cwd(), 'src/lib'))
  ? process.cwd()
  : join(process.cwd(), 'pm-web-app');

/** The app's own markup; `src/test` holds fixtures that model violations on purpose. */
const MARKUP_ROOTS = ['src/lib', 'src/routes'].map((root) => join(projectRoot, root));

/** A static `style="..."` attribute: quoted, with no `{` expression inside. */
const STATIC_STYLE_ATTRIBUTE = /\sstyle\s*=\s*(["'])[^"'{]*\1/;

/**
 * @param directory the directory to walk
 * @returns every `.svelte` file below it, recursively
 */
function svelteFiles(directory: string): string[] {
  const found: string[] = [];
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const full = join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...svelteFiles(full));
    } else if (entry.name.endsWith('.svelte')) {
      found.push(full);
    }
  }
  return found;
}

/**
 * @param source the component source
 * @returns the markup only, without its `<script>` and `<style>` blocks and comments
 */
function markupOf(source: string): string {
  return source
    .replace(/<script\b[\s\S]*?<\/script>/g, '')
    .replace(/<style\b[\s\S]*?<\/style>/g, '')
    .replace(/<!--[\s\S]*?-->/g, '');
}

describe('the app under the daemon CSP', () => {
  it('emits every asset as a file, never as an inlined data: URL', () => {
    expect(config.build?.assetsInlineLimit).toBe(0);
  });

  it('carries no static style attribute in its markup', () => {
    const offenders = MARKUP_ROOTS.filter((root) => existsSync(root))
      .flatMap(svelteFiles)
      .filter((file) => STATIC_STYLE_ATTRIBUTE.test(markupOf(readFileSync(file, 'utf8'))))
      .map((file) => relative(projectRoot, file));

    expect(offenders).toEqual([]);
  });

  it('detects a static style attribute but admits a bound style', () => {
    expect(STATIC_STYLE_ATTRIBUTE.test('<span style="inline-size: 28px"></span>')).toBe(true);
    expect(STATIC_STYLE_ATTRIBUTE.test('<span style={`--size: ${size}px`}></span>')).toBe(false);
    expect(STATIC_STYLE_ATTRIBUTE.test('<span style="width: {width}px"></span>')).toBe(false);
    expect(STATIC_STYLE_ATTRIBUTE.test('<div style:max-width={limit}></div>')).toBe(false);
  });
});

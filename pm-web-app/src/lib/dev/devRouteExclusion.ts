import fs from 'node:fs';
import type { Plugin } from 'vite';

/**
 * The development page is built only in development (VD-P-05, VD-S-03).
 *
 * The page lives at `src/routes/dev/components/+page.svelte` so the development server serves it
 * from source. A production build would carry it in two ways: SvelteKit's client router keeps the
 * route in its generated manifest, and the generated route node imports the page component. This
 * plugin removes both — the manifest loses its development entries, and the import points at an
 * empty component — so neither the path nor the fixture's code reaches the production bundle.
 *
 * It applies to the production build only (`apply: 'build'`): the development server keeps the
 * route, which is the whole point of the page.
 */

/** The path prefix every development route carries. */
export const DEV_ROUTE_PREFIX = '/dev';

/** The module id the build substitutes for the development page. */
export const EMPTY_DEV_PAGE_ID = '/@dev-route-exclusion/EmptyDevPage.svelte';

/** The generated client manifests: the development one and the ones a build generates. */
const CLIENT_MANIFEST = /generated[\\/](?:[^\\/]+[\\/])?client(?:-optimized)?[\\/]app\.js$/;

/** A generated route node module, under the same directory layout as the manifest. */
const GENERATED_NODE = /generated[\\/](?:[^\\/]+[\\/])?client(?:-optimized)?[\\/]nodes[\\/]\d+\.js$/;

/** An import specifier that names a file of the development pages. */
const DEV_PAGE_IMPORT = /(['"])([^'"]*src[\\/]routes[\\/]dev[\\/][^'"]*\.svelte)\1/g;

/** One lazy node import of the generated manifest. */
const NODE_IMPORT = /\(\)\s*=>\s*import\('\.\/nodes\/(\d+)'\)/g;

/** One entry of the generated route dictionary, as the build parses it. */
interface DictionaryEntry {
  path: string;
  indices: number[];
}

/**
 * Parses one line of the generated route dictionary, `"/path": [1, 2],`.
 *
 * The line is walked character by character rather than matched by a regular expression: the
 * expression this replaced could backtrack super-linearly on a long line, and SvelteKit writes the
 * dictionary from whatever route names the checkout holds.
 *
 * @param line one line of the generated manifest
 * @returns the entry it writes, or null for a line that writes none
 */
function parseDictionaryEntry(line: string): DictionaryEntry | null {
  const text = line.trim();
  if (!text.startsWith('"')) {
    return null;
  }
  const separator = text.indexOf('":');
  if (separator < 2) {
    return null;
  }
  const path = text.slice(1, separator);
  const list = text.slice(separator + 2).trim();
  const closing = list.lastIndexOf(']');
  if (!list.startsWith('[') || closing < 0) {
    return null;
  }
  const indices = list
    .slice(1, closing)
    .split(',')
    .map((part) => part.trim())
    .filter((part) => part !== '')
    .map(Number)
    .filter((index) => Number.isInteger(index));
  return { path, indices };
}

/**
 * @param path a route path of the generated dictionary
 * @returns whether it belongs to the development page
 */
function isDevRoute(path: string): boolean {
  return path === DEV_ROUTE_PREFIX || path.startsWith(`${DEV_ROUTE_PREFIX}/`);
}

/**
 * @param id a module id Vite is about to load
 * @returns whether it is a generated client manifest
 */
export function isClientManifest(id: string): boolean {
  return CLIENT_MANIFEST.test(id);
}

/**
 * @param id a module id Vite is about to load
 * @returns whether it is a generated route node module
 */
export function isGeneratedNode(id: string): boolean {
  return GENERATED_NODE.test(id);
}

/**
 * Points a generated route node's imports of the development pages at the empty component.
 *
 * SvelteKit writes one node module per route; the development page's node imports it by path. The
 * substitution happens on the node's source, which the build always reads, rather than on the
 * import alone: the node is the one place the page is imported from.
 *
 * @param code a generated node module's source
 * @returns the source with the development page replaced
 */
export function stripDevPageImports(code: string): string {
  return code.replace(
    DEV_PAGE_IMPORT,
    (_match, quote: string) => `${quote}${EMPTY_DEV_PAGE_ID}${quote}`,
  );
}

/**
 * Removes the development routes from a generated client manifest.
 *
 * A node is dropped only when no remaining route names it, because a layout node can be shared with
 * a route that stays. Dropping it means replacing its lazy import with an empty module, which keeps
 * every other node at its index and takes the page's module out of the bundle graph.
 *
 * @param source the generated client manifest
 * @returns the manifest without its development routes
 */
export function stripDevRoutes(source: string): string {
  const kept: string[] = [];
  const referenced = new Set<number>();
  const devNodes: number[] = [];

  for (const line of source.split('\n')) {
    const entry = parseDictionaryEntry(line);
    if (entry && isDevRoute(entry.path)) {
      devNodes.push(...entry.indices);
      continue;
    }
    kept.push(line);
    if (entry) {
      for (const index of entry.indices) {
        referenced.add(index);
      }
    }
  }

  const dropped = new Set(devNodes.filter((index) => !referenced.has(index)));
  if (dropped.size === 0) {
    return kept.join('\n');
  }

  return kept
    .join('\n')
    .replace(NODE_IMPORT, (match, index: string) =>
      dropped.has(Number(index)) ? '() => Promise.resolve({})' : match,
    );
}

/**
 * @returns the Vite plugin that keeps the development page out of the production build
 */
export function devRouteExclusion(): Plugin {
  return {
    name: 'dev-route-exclusion',
    apply: 'build',
    resolveId(source) {
      // The node's rewritten import names a module no file provides; claiming it here is what lets
      // the load hook below hand over the stand-in.
      return source === EMPTY_DEV_PAGE_ID ? source : null;
    },
    load(id) {
      if (id === EMPTY_DEV_PAGE_ID) {
        return '<!-- the development page is not part of a production build -->';
      }
      if (!isClientManifest(id)) {
        return null;
      }
      return stripDevRoutes(fs.readFileSync(id, 'utf-8'));
    },
    transform(code, id) {
      if (!isGeneratedNode(id) || !code.includes('routes/dev')) {
        return null;
      }
      return { code: stripDevPageImports(code), map: null };
    },
  };
}

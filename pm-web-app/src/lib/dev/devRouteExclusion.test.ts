import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  EMPTY_DEV_PAGE_ID,
  devRouteExclusion,
  isClientManifest,
  isGeneratedNode,
  stripDevPageImports,
  stripDevRoutes,
} from './devRouteExclusion';

/**
 * The development page is built only in development (VD-P-05, VD-S-03): the production bundle must
 * hold no `/dev` route. SvelteKit builds its client router from the routes directory, so the
 * production build drops the development entries from the generated manifest — and with them the
 * node module that imports the page, which is what takes the page out of the bundle.
 */

/** A generated client manifest, in the shape SvelteKit writes it. */
const MANIFEST = `export const nodes = [
\t() => import('./nodes/0'),
\t() => import('./nodes/1'),
\t() => import('./nodes/2'),
\t() => import('./nodes/3')
];

export const server_loads = [];

export const dictionary = {
\t\t"/": [1, 2],
\t\t"/dev/components": [1, 3]
};
`;

describe('the development route exclusion', () => {
  it('drops the development route from the client manifest', () => {
    const stripped = stripDevRoutes(MANIFEST);

    expect(stripped).not.toContain('/dev/components');
    expect(stripped).toContain('"/": [1, 2]');
  });

  it('drops the node module only the development route imported', () => {
    const stripped = stripDevRoutes(MANIFEST);

    expect(stripped).not.toContain("import('./nodes/3')");
  });

  it('keeps a node another route shares, at its index', () => {
    const stripped = stripDevRoutes(MANIFEST);

    expect(stripped).toContain("import('./nodes/0')");
    expect(stripped).toContain("import('./nodes/1')");
    expect(stripped).toContain("import('./nodes/2')");
  });

  it('leaves a manifest with no development route as it is', () => {
    const plain = MANIFEST.replace('\t\t"/dev/components": [1, 3]\n', '');

    expect(stripDevRoutes(plain)).toBe(plain);
  });

  it('drops a bare /dev route as well as its children', () => {
    const withRoot = MANIFEST.replace('"/dev/components": [1, 3]', '"/dev": [1, 3]');

    expect(stripDevRoutes(withRoot)).not.toContain('"/dev"');
  });

  it('applies to the production build only', () => {
    expect(devRouteExclusion().apply).toBe('build');
  });

  it('reads the manifest SvelteKit generates for a build', () => {
    expect(
      isClientManifest(
        '/repo/pm-web-app/.svelte-kit/generated/build/client-optimized/app.js',
      ),
    ).toBe(true);
    expect(isClientManifest('/repo/pm-web-app/.svelte-kit/generated/client/app.js')).toBe(true);
    expect(isClientManifest('/repo/pm-web-app/src/lib/routes.ts')).toBe(false);
  });

  it('substitutes a real Svelte module, so the compiler handles it like any route component', () => {
    expect(EMPTY_DEV_PAGE_ID.endsWith('.svelte')).toBe(true);
  });

  it('is registered in the production build', () => {
    const config = fs.readFileSync(path.resolve(__dirname, '../../../vite.config.ts'), 'utf-8');

    expect(config).toContain('devRouteExclusion(),');
  });

  it('rewrites the development page import of a generated node, and only that', () => {
    const plugin = devRouteExclusion() as {
      transform: (code: string, id: string) => { code: string } | null;
    };
    const nodePath =
      '/repo/pm-web-app/.svelte-kit/generated/build/client-optimized/nodes/5.js';
    const node =
      'export { default as component } from "../../../../../src/routes/dev/components/+page.svelte";';

    expect(plugin.transform(node, nodePath)?.code).toContain(EMPTY_DEV_PAGE_ID);
    expect(plugin.transform(node, '/repo/pm-web-app/src/routes/overview/+page.svelte')).toBeNull();
  });

  it('points the development route node at the empty component', () => {
    const node =
      'export { default as component } from "../../../../../src/routes/dev/components/+page.svelte";';
    const stripped = stripDevPageImports(node);

    expect(stripped).toContain(EMPTY_DEV_PAGE_ID);
    expect(stripped).not.toContain('routes/dev');
  });

  it('leaves the node of another route alone', () => {
    const node =
      'export { default as component } from "../../../../../src/routes/overview/+page.svelte";';

    expect(stripDevPageImports(node)).toBe(node);
  });

  it('recognizes the generated node modules', () => {
    expect(
      isGeneratedNode(
        '/repo/pm-web-app/.svelte-kit/generated/build/client-optimized/nodes/5.js',
      ),
    ).toBe(true);
    expect(isGeneratedNode('/repo/pm-web-app/src/lib/components/Button.svelte')).toBe(false);
  });
});

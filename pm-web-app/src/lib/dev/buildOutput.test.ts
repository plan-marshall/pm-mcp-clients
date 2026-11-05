import { describe, it, expect, vi, afterEach } from 'vitest';
import fs from 'node:fs';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  findDevMarkers,
  DEV_MARKERS,
  deriveDevMarkers,
  checkBuildOutput,
} from '../../../scripts/check-build-output.mjs';

/**
 * The build-output check (PR 260's second ask): `devRouteExclusion` proves its transformations over
 * fixtures, and this proves that what the build actually wrote is read — a bundle that still holds
 * the development page fails the build instead of passing in silence.
 */

describe('the build-output check', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('passes a build without the development page', () => {
    expect(
      findDevMarkers({
        'index.html': '<!doctype html><div id="app"></div>',
        '_app/immutable/entry/app.js': 'const routes={"/overview":[0]};',
      }),
    ).toEqual([]);
  });

  it('fails a build that still carries the development page', () => {
    const found = findDevMarkers({
      'index.html': '<!doctype html>',
      '_app/immutable/entry/app.js': 'const routes={"/dev/components":[1]};',
    });

    expect(found).toHaveLength(1);
    expect(found[0]).toContain('/dev/components');
  });

  it('looks for every marker the page leaves behind', () => {
    const files: Record<string, string> = {};
    DEV_MARKERS.forEach((marker, index) => {
      files[`node-${index}.js`] = `export const page = "${marker}";`;
    });

    expect(findDevMarkers(files)).toHaveLength(DEV_MARKERS.length);
  });

  it('derives its markers from the route the exclusion plugin guards', () => {
    // The route path the manifest wrote, the chunk name the route compiles to, and the node's
    // import path: the three places the page can survive a build.
    expect(DEV_MARKERS).toEqual(['/dev/components', 'dev-components', 'routes/dev']);
  });

  it('derives the markers from the route source, not from a list beside it', () => {
    // A fixture whose route and prefix differ from this checkout's: a hand-written list cannot
    // answer for it, so replacing the derivation with one makes this test fail.
    const root = mkdtempSync(join(tmpdir(), 'pm-dev-route-'));
    try {
      const page = join(root, 'routes', 'devx', 'components');
      mkdirSync(page, { recursive: true });
      writeFileSync(join(page, '+page.svelte'), '<!-- the page -->');
      const plugin = join(root, 'devRouteExclusion.ts');
      writeFileSync(plugin, "export const DEV_ROUTE_PREFIX = '/devx';\n");

      expect(deriveDevMarkers(join(root, 'routes'), plugin)).toEqual([
        '/devx/components',
        'devx-components',
        'routes/devx',
      ]);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });

  it('finds the page below a multi-segment route prefix', () => {
    const root = mkdtempSync(join(tmpdir(), 'pm-dev-route-'));
    try {
      const page = join(root, 'routes', 'internal', 'dev', 'components');
      mkdirSync(page, { recursive: true });
      writeFileSync(join(page, '+page.svelte'), '<!-- the page -->');
      const plugin = join(root, 'devRouteExclusion.ts');
      writeFileSync(plugin, "export const DEV_ROUTE_PREFIX = '/internal/dev';\n");

      expect(deriveDevMarkers(join(root, 'routes'), plugin)).toEqual([
        '/internal/dev/components',
        'internal-dev-components',
        'routes/internal',
      ]);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });

  it('finds a development page emitted as a file of its own', () => {
    // The route can survive as a file path even when every body is clean.
    expect(findDevMarkers({ 'dev/components.html': '<!doctype html>' })).toEqual([
      'dev/components.html: /dev/components',
    ]);
  });

  it('reads a build directory and stops the build on a marker', () => {
    const root = mkdtempSync(join(tmpdir(), 'pm-build-output-'));
    const build = join(root, 'build');
    mkdirSync(join(build, '_app', 'immutable', 'entry'), { recursive: true });
    writeFileSync(join(build, 'index.html'), '<!doctype html>');
    writeFileSync(
      join(build, '_app', 'immutable', 'entry', 'app.js'),
      'const routes={"/dev/components":[1]};',
    );
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const exit = vi.spyOn(process, 'exit').mockImplementation((code) => {
      throw new Error(`exit ${String(code)}`);
    });

    try {
      expect(() => checkBuildOutput(build)).toThrow('exit 1');
      expect(exit).toHaveBeenCalledWith(1);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  });

  it('stops the build when an emitted file cannot be read', () => {
    const root = mkdtempSync(join(tmpdir(), 'pm-build-output-'));
    const build = join(root, 'build');
    mkdirSync(build, { recursive: true });
    writeFileSync(join(build, 'index.html'), '<!doctype html>');
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {});
    const exit = vi.spyOn(process, 'exit').mockImplementation((code) => {
      throw new Error(`exit ${String(code)}`);
    });
    // An unreadable file could be the one carrying a marker; a check that skips it fails open.
    vi.spyOn(fs, 'readFileSync').mockImplementation(() => {
      throw new Error('permission denied');
    });

    try {
      expect(() => checkBuildOutput(build)).toThrow('exit 1');
      expect(exit).toHaveBeenCalledWith(1);
      expect(errors).toHaveBeenCalledWith(expect.stringContaining('cannot be read'));
    } finally {
      vi.restoreAllMocks();
      rmSync(root, { recursive: true, force: true });
    }
  });
});

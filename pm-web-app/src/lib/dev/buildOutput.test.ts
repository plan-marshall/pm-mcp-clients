import { describe, it, expect } from 'vitest';
import { findDevMarkers, DEV_MARKERS } from '../../../scripts/check-build-output.mjs';

/**
 * The build-output check (PR 260's second ask): `devRouteExclusion` proves its transformations over
 * fixtures, and this proves that what the build actually wrote is read — a bundle that still holds
 * the development page fails the build instead of passing in silence.
 */

describe('the build-output check', () => {
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

  it('finds a development page emitted as a file of its own', () => {
    // The route can survive as a file path even when every body is clean.
    expect(findDevMarkers({ 'dev/components.html': '<!doctype html>' })).toEqual([
      'dev/components.html: /dev/components',
    ]);
  });
});

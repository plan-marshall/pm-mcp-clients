import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

/**
 * The self-hosted Inter face of VD-P-03.
 *
 * `static/` is the directory the build copies into the packaged app, so this asserts the two files
 * the app serves from its own origin: the variable `woff2` and the licence that travels with it.
 */
describe('self-hosted Inter', () => {
  const fontsDir = path.resolve(__dirname, '../../static/fonts');

  it('ships the variable woff2 the build copies into the app', () => {
    const font = fs.readFileSync(path.join(fontsDir, 'InterVariable.woff2'));

    expect(font.length).toBeGreaterThan(100_000);
    expect(font.subarray(0, 4).toString('latin1')).toBe('wOF2');
  });

  it('ships the SIL Open Font License beside the file', () => {
    const licence = fs.readFileSync(path.join(fontsDir, 'OFL.txt'), 'utf8');

    expect(licence).toContain('SIL OPEN FONT LICENSE');
    expect(licence).toContain('The Inter Project Authors');
  });
});

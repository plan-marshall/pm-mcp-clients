import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

/**
 * The one client-level time limit (D5, UX-S-06): the client bounds every request at 15 s, so no
 * view, route or act carries a 15-second constant of its own. The only other 15-second figure in the
 * package is the listener view's retry interval, which is not a request limit.
 */

const SOURCE_DIR = path.resolve(__dirname, '../..');

/**
 * @param directory the directory to walk
 * @returns every file below it, recursively
 */
function walk(directory: string): string[] {
  const found: string[] = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...walk(full));
    } else {
      found.push(full);
    }
  }
  return found;
}

const FIFTEEN_SECONDS = /\b15_?000\b|\b15\s*\*\s*1_?000\b|\b15e3\b/;

describe('the one client-level request limit', () => {
  it('keeps 15_000 only in the client limit and the listener retry interval', () => {
    const offenders: string[] = [];
    for (const file of walk(SOURCE_DIR)) {
      if (!/\.(ts|js|svelte)$/.test(file) || /\.(test|spec)\.(ts|js)$/.test(file)) continue;
      const text = fs.readFileSync(file, 'utf-8');
      if (!FIFTEEN_SECONDS.test(text)) continue;
      const relative = path.relative(SOURCE_DIR, file).split(path.sep).join('/');
      if (relative === 'lib/api/client.ts' || relative === 'lib/views/ListenerOffView.svelte') {
        continue;
      }
      offenders.push(relative);
    }

    expect(offenders).toEqual([]);
  });
});

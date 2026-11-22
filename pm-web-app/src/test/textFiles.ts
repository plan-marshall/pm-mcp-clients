import fs from 'node:fs';
import path from 'node:path';

/**
 * Recursively walks a directory and returns absolute paths to every file within it.
 *
 * @param directory the root directory to walk
 * @returns array of absolute file paths
 */
export function walk(directory: string): string[] {
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

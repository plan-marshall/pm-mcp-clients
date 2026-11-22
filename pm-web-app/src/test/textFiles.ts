import fs from 'node:fs';
import path from 'node:path';

/** Raster images (mascot figures, icons): binary files, not text a character can sit in. */
export const RASTER_IMAGE = /\.(webp|png|jpe?g|gif|ico)$/i;

/**
 * Recursively walks a directory and returns absolute paths to every text file within it,
 * excluding raster binary images.
 *
 * @param directory the root directory to walk
 * @returns array of absolute file paths to text files
 */
export function walk(directory: string): string[] {
  const found: string[] = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const full = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      found.push(...walk(full));
    } else if (!RASTER_IMAGE.test(entry.name)) {
      found.push(full);
    }
  }
  return found;
}

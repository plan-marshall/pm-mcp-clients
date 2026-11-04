import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * The development page must not reach a production bundle (VD-P-05, VD-S-03).
 *
 * `devRouteExclusion` removes it from the generated route manifest and from the route node that
 * imports it, and its own tests prove both transformations over fixtures. Neither looks at what
 * the build actually wrote: a plugin that silently stopped matching would leave the page in the
 * bundle and no test would notice. This check reads the emitted files instead and fails when a
 * marker of the page is still there.
 */

/** The strings the development page leaves in a build that still carries it. */
export const DEV_MARKERS = ['/dev/components', 'dev-components', 'routes/dev'];

/**
 * @param files the emitted files, by path
 * @returns every marker found, as `path: marker`
 */
export function findDevMarkers(files) {
  const found = [];
  for (const [file, content] of Object.entries(files)) {
    for (const marker of DEV_MARKERS) {
      if (content.includes(marker)) {
        found.push(`${file}: ${marker}`);
      }
    }
  }
  return found;
}

/**
 * @param file the file an emitted path names
 * @returns its text, or an empty string for a file that is not text
 */
function readText(file) {
  try {
    const content = fs.readFileSync(file, 'utf-8');
    // A null byte is how a binary file announces itself; the markers are text.
    return content.includes('\u0000') ? '' : content;
  } catch {
    return '';
  }
}

/**
 * @param dir a directory
 * @returns every file below it, recursively
 */
function filesOf(dir) {
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...filesOf(full));
    } else if (entry.isFile()) {
      files.push(full);
    }
  }
  return files;
}

/**
 * Fails the build when its own output carries the development page.
 *
 * @param buildDir the directory the build wrote, `build` by default
 */
export function checkBuildOutput(buildDir = path.resolve('build')) {
  if (!fs.existsSync(buildDir)) {
    console.error(`check-build-output: ${buildDir} does not exist; run the build first.`);
    process.exit(1);
  }
  const files = {};
  for (const file of filesOf(buildDir)) {
    files[path.relative(buildDir, file)] = readText(file);
  }
  const found = findDevMarkers(files);
  if (found.length > 0) {
    console.error('check-build-output: the development page reached the build:');
    for (const line of found) {
      console.error(`  ${line}`);
    }
    process.exit(1);
  }
  console.info('check-build-output: no development-page marker in the build.');
}

// The check runs when the script is the program the build started, never when a test imports it.
const invokedDirectly =
  process.argv[1] !== undefined &&
  path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url));

if (invokedDirectly) {
  checkBuildOutput();
}

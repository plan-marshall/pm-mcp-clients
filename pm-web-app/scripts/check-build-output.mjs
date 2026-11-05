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

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const appDir = path.resolve(scriptDir, '..');
const routesDir = path.join(appDir, 'src', 'routes');
const pluginFile = path.join(appDir, 'src', 'lib', 'dev', 'devRouteExclusion.ts');

/**
 * @param pluginPath the plugin file that defines the route prefix
 * @returns the route prefix the plugin excludes
 */
function devRoutePrefix(pluginPath) {
  const source = fs.readFileSync(pluginPath, 'utf-8');
  const match = /export const DEV_ROUTE_PREFIX\s*=\s*['"]([^'"]+)['"]/.exec(source);
  if (!match) {
    throw new Error(`${pluginPath} no longer exports DEV_ROUTE_PREFIX`);
  }
  return match[1].replace(/^\/+|\/+$/g, '');
}

/**
 * @param dir a directory below the route root
 * @param routePrefix the route prefix the plugin excludes
 * @param prefix its path from that root
 * @returns the route directory of the development page, or null
 */
function devPageRoute(dir, routePrefix, prefix) {
  const wanted = `${routePrefix}/`;
  const entries = fs
    .readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .sort((left, right) => left.name.localeCompare(right.name));
  for (const entry of entries) {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
    // A directory is on the way to the prefix when it is the prefix, below it, or an ancestor of
    // a multi-segment prefix (`internal` on the way to `internal/dev`).
    const withinPrefix = relative === routePrefix || relative.startsWith(wanted);
    const ancestorOfPrefix = routePrefix.startsWith(`${relative}/`);
    if (!withinPrefix && !ancestorOfPrefix) continue;
    const full = path.join(dir, entry.name);
    if (fs.existsSync(path.join(full, '+page.svelte'))) return relative;
    const nested = devPageRoute(full, routePrefix, relative);
    if (nested !== null) return nested;
  }
  return null;
}

/**
 * The strings the development page leaves in a build that still carries it, derived from the route
 * the plugin excludes: the route path, the chunk name the route compiles to, and the node's import
 * path. A hand-written list went stale in silence when the page moved.
 *
 * @param routesRoot the route source root
 * @param pluginPath the plugin file that names the excluded prefix
 * @returns the markers the emitted files are searched for
 */
export function deriveDevMarkers(routesRoot, pluginPath) {
  const route = devPageRoute(routesRoot, devRoutePrefix(pluginPath), '');
  if (route === null) {
    throw new Error('the development page is not below the route root: this check guards nothing');
  }
  return [`/${route}`, route.replaceAll('/', '-'), `routes/${route.split('/')[0]}`];
}

/** The markers of this checkout's development page. */
export const DEV_MARKERS = deriveDevMarkers(routesDir, pluginFile);

/**
 * @param files the emitted files, by path
 * @returns every marker found, as `path: marker`
 */
export function findDevMarkers(files) {
  const found = [];
  for (const [file, content] of Object.entries(files)) {
    for (const marker of DEV_MARKERS) {
      if (content.includes(marker) || file.includes(marker) || `/${file}`.includes(marker)) {
        found.push(`${file}: ${marker}`);
      }
    }
  }
  return found;
}

/**
 * @param file the file an emitted path names
 * @returns its text, or an empty string for a file that is not text
 * @throws when the file cannot be read: a check that cannot read its input must fail, never pass
 */
function readText(file) {
  const content = fs.readFileSync(file, 'utf-8');
  // A null byte is how a binary file announces itself; the markers are text.
  return content.includes('\u0000') ? '' : content;
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
    const name = path.relative(buildDir, file);
    try {
      files[name] = readText(file);
    } catch (error) {
      // An unreadable emitted file hides whatever marker it carries; the check fails rather than
      // passing on the files it could read.
      console.error(
        `check-build-output: ${name} cannot be read: ${error instanceof Error ? error.message : String(error)}`,
      );
      process.exit(1);
    }
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

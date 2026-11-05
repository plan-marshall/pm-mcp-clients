import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { DEFAULT_WEB_PORT, DEFAULT_MOCK_WEB_PORT } from '../tests/helpers/ports.js';

/**
 * The port guard of pm-web-e2e: no test file names a port of its own, a port that is not a number
 * stops the run, and an assigned web port is held instead of being moved.
 *
 * The checks are exported so the Playwright spec runs the same ones the `test:ports-guard` script
 * runs; the script is the CLI over them.
 */

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const moduleDir = path.resolve(__dirname, '..');
const testsDir = path.resolve(moduleDir, 'tests');

/** The files allowed to name a port: the resolver and this guard's own spec. */
export const ALLOWED_FILES = new Set(['ports.js', 'port-guard.spec.js']);

/** The default ports no other file may write, derived from the resolver's own defaults. */
export const FORBIDDEN_PORTS = [
  new RegExp(`\\b${DEFAULT_WEB_PORT}\\b`),
  new RegExp(`\\b${DEFAULT_MOCK_WEB_PORT}\\b`),
];

/**
 * @param fullPath a file
 * @param violations the list to add to
 */
function checkFile(fullPath, violations) {
  const content = fs.readFileSync(fullPath, 'utf8');
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    for (const pattern of FORBIDDEN_PORTS) {
      if (pattern.test(lines[i])) {
        violations.push(`${path.relative(moduleDir, fullPath)}:${i + 1}: ${lines[i].trim()}`);
      }
    }
  }
}

/**
 * @param dir a directory below the module
 * @param violations the list to add to
 */
function scanDir(dir, violations) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      scanDir(fullPath, violations);
    } else if (entry.isFile() && !ALLOWED_FILES.has(entry.name)) {
      checkFile(fullPath, violations);
    }
  }
}

/**
 * @returns every forbidden port literal below `tests/`, in playwright.config.js and in
 *   global-setup.js
 */
export function findPortViolations() {
  const violations = [];
  scanDir(testsDir, violations);
  checkFile(path.join(moduleDir, 'playwright.config.js'), violations);
  checkFile(path.join(moduleDir, 'global-setup.js'), violations);
  return violations;
}

/**
 * Runs the resolver in a child process with a value that is not a port.
 *
 * @param value the PM_WEB_PORT value to try
 * @returns the process result: a non-zero status and the message on stderr are the loud failure
 */
export function malformedPortRun(value = 'abc') {
  const helper = pathToFileURL(path.join(testsDir, 'helpers', 'ports.js')).href;
  return spawnSync(process.execPath, ['--input-type=module', '-e', `import('${helper}')`], {
    cwd: moduleDir,
    env: { ...process.env, PM_WEB_PORT: value },
    encoding: 'utf8',
  });
}

/**
 * @returns whether the dev server holds an assigned PM_WEB_PORT instead of moving to another one
 */
export function strictPortIsBound() {
  const config = fs.readFileSync(path.join(moduleDir, '..', 'pm-web-app', 'vite.config.ts'), 'utf8');
  // The behaviour is `holdsAssignedPort()` in pm-web-app/src/lib/ports.ts, covered by its own unit
  // test; this checks the wiring, so a config that stops binding it fails here.
  return /^\s*strictPort:\s*holdsAssignedPort\(\),\s*$/m.test(config);
}

/**
 * Runs every check and exits non-zero on the first that fails.
 */
function main() {
  const violations = findPortViolations();
  if (violations.length > 0) {
    console.error(`Found ${violations.length} forbidden port literal(s) in pm-web-e2e:`);
    for (const violation of violations) {
      console.error(`  ${violation}`);
    }
    process.exit(1);
  }

  const malformed = malformedPortRun();
  if (malformed.status === 0 || !(malformed.stderr ?? '').includes('Invalid PM_WEB_PORT')) {
    console.error('A malformed PM_WEB_PORT was read as the default instead of stopping the run.');
    process.exit(1);
  }

  if (!strictPortIsBound()) {
    console.error('pm-web-app/vite.config.ts does not hold an assigned PM_WEB_PORT (strictPort).');
    process.exit(1);
  }

  console.info(
    'No forbidden port literals in pm-web-e2e; a malformed PM_WEB_PORT stops the run; the assigned web port is held.',
  );
  process.exit(0);
}

const invokedDirectly =
  process.argv[1] !== undefined &&
  path.resolve(process.argv[1]) === path.resolve(__filename);

if (invokedDirectly) {
  main();
}

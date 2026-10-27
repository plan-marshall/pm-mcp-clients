import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const moduleDir = path.resolve(__dirname, '..');
const testsDir = path.resolve(moduleDir, 'tests');

const allowedFiles = new Set([
  'ports.js',
  'guard-ports.js',
  'guard-ports.test.js',
  'port-guard.spec.js',
]);

const forbiddenPatterns = [/\b7420\b/, /\b5173\b/];

function checkFile(fullPath, violations) {
  const content = fs.readFileSync(fullPath, 'utf8');
  const lines = content.split('\n');
  for (let i = 0; i < lines.length; i++) {
    for (const pattern of forbiddenPatterns) {
      if (pattern.test(lines[i])) {
        violations.push({
          file: path.relative(moduleDir, fullPath),
          line: i + 1,
          content: lines[i].trim(),
        });
      }
    }
  }
}

function scanDir(dir, violations) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      scanDir(fullPath, violations);
    } else if (entry.isFile() && !allowedFiles.has(entry.name)) {
      checkFile(fullPath, violations);
    }
  }
}

const violations = [];
scanDir(testsDir, violations);
checkFile(path.join(moduleDir, 'playwright.config.js'), violations);
checkFile(path.join(moduleDir, 'global-setup.js'), violations);

if (violations.length > 0) {
  console.error(`Found ${violations.length} forbidden port literal(s) in pm-web-e2e:`);
  for (const v of violations) {
    console.error(`  ${v.file}:${v.line}: ${v.content}`);
  }
  process.exit(1);
}

console.log('No forbidden port literals (7420 / 5173) found in pm-web-e2e tests, playwright.config.js, or global-setup.js.');
process.exit(0);

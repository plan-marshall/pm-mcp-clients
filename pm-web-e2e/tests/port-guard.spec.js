import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const moduleDir = path.resolve(__dirname, '..');
const testsDir = __dirname;

const allowedFiles = new Set(['ports.js', 'port-guard.spec.js']);
const forbiddenPatterns = [/\b7420\b/, /\b5173\b/];

test.describe('Port Isolation & Parameterization Guard', () => {
  test('no test file below tests/ except ports.js, nor playwright.config.js or global-setup.js contains hardcoded port literals 7420 or 5173', () => {
    const violations = [];

    function checkFile(fullPath) {
      const content = fs.readFileSync(fullPath, 'utf8');
      const lines = content.split('\n');
      for (let i = 0; i < lines.length; i++) {
        for (const pattern of forbiddenPatterns) {
          if (pattern.test(lines[i])) {
            violations.push(`${path.relative(moduleDir, fullPath)}:${i + 1}: ${lines[i].trim()}`);
          }
        }
      }
    }

    function scan(dir) {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scan(fullPath);
        } else if (entry.isFile() && !allowedFiles.has(entry.name)) {
          checkFile(fullPath);
        }
      }
    }

    scan(testsDir);
    checkFile(path.join(moduleDir, 'playwright.config.js'));
    checkFile(path.join(moduleDir, 'global-setup.js'));

    expect(violations, `Found forbidden port literals:\n${violations.join('\n')}`).toEqual([]);
  });

  test('a malformed PM_WEB_PORT stops the run instead of falling back to the default', () => {
    const helper = pathToFileURL(path.join(moduleDir, 'tests', 'helpers', 'ports.js')).href;
    const result = spawnSync(process.execPath, ['--input-type=module', '-e', `import('${helper}')`], {
      cwd: moduleDir,
      env: { ...process.env, PM_WEB_PORT: 'abc' },
      encoding: 'utf8',
    });

    expect(result.status, 'a port that is not a number must not be read as the default').not.toBe(0);
    expect(result.stderr).toContain('Invalid PM_WEB_PORT');
  });

  test('holds the assigned web port instead of moving to another one', () => {
    const config = fs.readFileSync(path.join(moduleDir, '..', 'pm-web-app', 'vite.config.ts'), 'utf8');

    // Vite increments the port when it is taken unless strictPort is set; AGENTS.md makes an
    // assigned port that is in use a blocker, not a reason to take another one.
    expect(config).toMatch(/strictPort:\s*\(process\.env\.PM_WEB_PORT/);
  });
});

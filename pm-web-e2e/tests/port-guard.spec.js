import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

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
});

import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

/**
 * Allowlisted local custom properties that are component-scoped and dynamically bound in templates.
 */
const ALLOWLISTED_LOCAL_TOKENS = new Set([
  '--workflow-icon-size', // Component-local size bound inline on WorkflowIcon
  '--workflow-icon-url', // Component-local mask-image URL bound inline on WorkflowIcon
]);

/**
 * Allowlisted files permitted to contain hard-coded color literals, with justification.
 */
const ALLOWLISTED_COLOR_LITERAL_FILES = new Set([
  path.normalize('lib/components/PairingQr.svelte'), // High-contrast black/white scannable QR modules
]);

function extractTokensFromBlock(cssContent: string, selectorPattern: RegExp): Set<string> {
  const match = selectorPattern.exec(cssContent);
  if (!match) return new Set();
  const block = match[1];
  const tokenRegex = /(--[a-zA-Z0-9_-]+)\s*:/g;
  const tokens = new Set<string>();
  let m;
  while ((m = tokenRegex.exec(block)) !== null) {
    tokens.add(m[1]);
  }
  return tokens;
}

function walkFiles(dir: string, extensions: string[]): string[] {
  const result: string[] = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      result.push(...walkFiles(fullPath, extensions));
    } else if (extensions.some((ext) => entry.name.endsWith(ext))) {
      result.push(fullPath);
    }
  }
  return result;
}

describe('tokens.css completeness and theme parity', () => {
  const tokensPath = path.resolve(__dirname, 'tokens.css');
  const tokensCss = fs.readFileSync(tokensPath, 'utf8');

  it('defines all color, shadow, and ring tokens identically in light and dark themes', () => {
    const rootTokens = extractTokensFromBlock(tokensCss, /:root\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}/);
    const lightTokens = extractTokensFromBlock(
      tokensCss,
      /\[data-theme=['"]light['"]\]\s*\{([^}]+)\}/,
    );
    const darkTokens = extractTokensFromBlock(
      tokensCss,
      /\[data-theme=['"]dark['"]\]\s*\{([^}]+)\}/,
    );
    const mediaDarkTokens = extractTokensFromBlock(
      tokensCss,
      /@media\s*\(prefers-color-scheme:\s*dark\)\s*\{\s*:root\s*\{([^}]+)\}/,
    );

    // Filter to theme-varying tokens (--color-*, --shadow-*, --focus-ring)
    const filterThemed = (tokens: Set<string>) =>
      new Set(
        Array.from(tokens).filter(
          (t) => t.startsWith('--color-') || t.startsWith('--shadow-') || t === '--focus-ring',
        ),
      );

    const rootThemed = filterThemed(rootTokens);
    const lightThemed = filterThemed(lightTokens);
    const darkThemed = filterThemed(darkTokens);
    const mediaDarkThemed = filterThemed(mediaDarkTokens);

    expect(rootThemed.size).toBeGreaterThan(0);
    expect(lightThemed.size).toBeGreaterThan(0);
    expect(darkThemed.size).toBeGreaterThan(0);
    expect(mediaDarkThemed.size).toBeGreaterThan(0);

    // Parity checks
    const missingInLight = Array.from(rootThemed).filter((t) => !lightThemed.has(t));
    const missingInDark = Array.from(rootThemed).filter((t) => !darkThemed.has(t));
    const missingInMediaDark = Array.from(rootThemed).filter((t) => !mediaDarkThemed.has(t));
    const surplusInLight = Array.from(lightThemed).filter((t) => !rootThemed.has(t));
    const surplusInDark = Array.from(darkThemed).filter((t) => !rootThemed.has(t));
    const surplusInMediaDark = Array.from(mediaDarkThemed).filter((t) => !rootThemed.has(t));

    expect(missingInLight).toEqual([]);
    expect(missingInDark).toEqual([]);
    expect(missingInMediaDark).toEqual([]);
    expect(surplusInLight).toEqual([]);
    expect(surplusInDark).toEqual([]);
    expect(surplusInMediaDark).toEqual([]);
    expect(surplusInLight).toEqual([]);
    expect(surplusInDark).toEqual([]);
    expect(Array.from(lightThemed).sort()).toEqual(Array.from(darkThemed).sort());
  });

  it('ensures every var(--...) referenced in src/**/*.{svelte,css} is defined in tokens.css or allowlisted', () => {
    const allDefinedTokens = new Set<string>();
    const defRegex = /(--[a-zA-Z0-9_-]+)\s*:/g;
    let m;
    while ((m = defRegex.exec(tokensCss)) !== null) {
      allDefinedTokens.add(m[1]);
    }

    const srcDir = path.resolve(__dirname, '..');
    const sourceFiles = walkFiles(srcDir, ['.svelte', '.css']);

    const varRegex = /var\(\s*(--[a-zA-Z0-9_-]+)/g;
    const undefinedUsages: { file: string; token: string }[] = [];

    for (const file of sourceFiles) {
      const content = fs.readFileSync(file, 'utf8');
      while ((m = varRegex.exec(content)) !== null) {
        const token = m[1];
        if (!allDefinedTokens.has(token) && !ALLOWLISTED_LOCAL_TOKENS.has(token)) {
          undefinedUsages.push({
            file: path.relative(srcDir, file),
            token,
          });
        }
      }
    }

    expect(undefinedUsages).toEqual([]);
  });

  it('ensures no hard-coded color literals exist in views or components outside the allowlist', () => {
    const srcDir = path.resolve(__dirname, '..');
    const sourceFiles = walkFiles(srcDir, ['.svelte', '.css']).filter(
      (f) => !f.endsWith('tokens.css'),
    );

    const hexRegex = /#([0-9a-fA-F]{3,8})\b/g;
    const rgbRegex = /rgba?\([^)]+\)/g;
    const hslRegex = /hsla?\([^)]+\)/g;
    const violations: { file: string; line: number; literal: string; snippet: string }[] = [];

    for (const file of sourceFiles) {
      const relPath = path.normalize(path.relative(srcDir, file));
      if (ALLOWLISTED_COLOR_LITERAL_FILES.has(relPath)) continue;

      const lines = fs.readFileSync(file, 'utf8').split('\n');
      lines.forEach((line, idx) => {
        const trimmed = line.trim();
        if (
          trimmed.startsWith('/*') ||
          trimmed.startsWith('//') ||
          trimmed.startsWith('*') ||
          trimmed.startsWith('<!--')
        ) {
          return;
        }

        let m;
        while ((m = hexRegex.exec(line)) !== null) {
          violations.push({
            file: relPath,
            line: idx + 1,
            literal: m[0],
            snippet: trimmed,
          });
        }
        while ((m = rgbRegex.exec(line)) !== null) {
          violations.push({
            file: relPath,
            line: idx + 1,
            literal: m[0],
            snippet: trimmed,
          });
        }
        while ((m = hslRegex.exec(line)) !== null) {
          violations.push({
            file: relPath,
            line: idx + 1,
            literal: m[0],
            snippet: trimmed,
          });
        }
      });
    }

    expect(violations).toEqual([]);
  });
});

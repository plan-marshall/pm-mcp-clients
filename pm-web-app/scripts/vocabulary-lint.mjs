import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse, parseCss } from 'svelte/compiler';

/**
 * Vocabulary Lint for Web App (components.adoc § #rules).
 *
 * Scans every .svelte file outside src/lib/components/ for raw HTML elements:
 * <button>, <a>, <input>, <textarea>, <select>, and any `outline: none` or `outline: 0`
 * in <style> blocks or .css files outside src/lib/components/.
 *
 * Reports each violation as:
 * warning: <file>:<line> <rule> — use <Component>
 *
 * and prints a per-file table with totals.
 * Exits 0 by default (warnings).
 * Exits 1 on any finding when run with --error.
 */

export const RULES = {
  button: { type: 'element', component: 'Button' },
  a: { type: 'element', component: 'Link' },
  input: { type: 'element', component: 'TextField' },
  textarea: { type: 'element', component: 'TextArea' },
  select: { type: 'element', component: 'SelectField' },
  'outline: none': { type: 'css', component: 'tokens.css (--focus-ring)' },
  unparseable: { type: 'syntax', component: 'valid syntax' },
};

export const RULE_NAMES = Object.keys(RULES);

const RAW_ELEMENTS = new Set(
  Object.entries(RULES)
    .filter(([_, rule]) => rule.type === 'element')
    .map(([name]) => name),
);

/**
 * Computes 1-based line number from a character offset.
 *
 * @param {string} source
 * @param {number} offset
 * @returns {number}
 */
export function getLineNumber(source, offset) {
  if (offset === null || offset === undefined || offset < 0) return 1;
  return source.slice(0, offset).split('\n').length;
}

/**
 * Normalizes a file path to use forward slashes and be relative to the app root.
 *
 * @param {string} filePath
 * @param {string} appDir
 * @returns {string}
 */
export function normalizePath(filePath, appDir) {
  const rel = appDir ? path.relative(appDir, filePath) : filePath;
  return rel.replace(/\\/g, '/');
}

/**
 * Checks whether a declaration specifies outline: none or outline: 0.
 *
 * @param {string} property
 * @param {string} value
 * @returns {boolean}
 */
function isOutlineNone(property, value) {
  if (!property || property.toLowerCase() !== 'outline') return false;
  if (!value) return false;
  const val = value.trim().replace(/!important/i, '').trim().toLowerCase();
  return val === 'none' || val === '0';
}

/**
 * Traverses a Svelte CSS AST looking for outline: none / 0.
 *
 * @param {object} cssAst
 * @param {string} relPath
 * @param {string} source
 * @param {Array<object>} findings
 */
function walkCssAst(cssAst, relPath, source, findings) {
  function visit(node) {
    if (!node || typeof node !== 'object') return;

    if (node.type === 'Declaration') {
      if (isOutlineNone(node.property, node.value)) {
        findings.push({
          file: relPath,
          line: getLineNumber(source, node.start),
          rule: 'outline: none',
          component: RULES['outline: none'].component,
        });
      }
    }

    for (const key of Object.keys(node)) {
      if (key === 'parent') continue;
      const child = node[key];
      if (Array.isArray(child)) {
        for (const item of child) {
          visit(item);
        }
      } else if (child && typeof child === 'object') {
        visit(child);
      }
    }
  }

  visit(cssAst);
}

/**
 * @typedef {Object} Finding
 * @property {string} file
 * @property {number} line
 * @property {string} rule
 * @property {string} component
 */

/**
 * Lints Svelte source code.
 *
 * @param {string} source
 * @param {string} relPath
 * @returns {Finding[]} findings
 */
export function lintSvelteSource(source, relPath = 'anonymous.svelte') {
  const normalized = relPath.replace(/\\/g, '/');
  if (
    normalized.includes('src/lib/components/') ||
    normalized.startsWith('lib/components/') ||
    normalized.startsWith('src/lib/components/')
  ) {
    return [];
  }

  const findings = [];
  let ast;
  try {
    ast = parse(source, { modern: true });
  } catch (err) {
    const line = err.start?.line ?? (err.position ? getLineNumber(source, err.position) : 1);
    const message = err.message || 'Syntax error';
    return [
      {
        file: normalized,
        line,
        rule: 'unparseable',
        component: message,
        message,
      },
    ];
  }

  function walkTemplate(node) {
    if (!node || typeof node !== 'object') return;

    if (node.type === 'RegularElement') {
      const name = node.name?.toLowerCase();
      if (name && RAW_ELEMENTS.has(name)) {
        const line = node.name_loc ? node.name_loc.start.line : getLineNumber(source, node.start);
        findings.push({
          file: normalized,
          line,
          rule: name,
          component: RULES[name].component,
        });
      }
    }

    for (const key of Object.keys(node)) {
      if (key === 'parent' || key === 'css') continue;
      const child = node[key];
      if (Array.isArray(child)) {
        for (const item of child) {
          walkTemplate(item);
        }
      } else if (child && typeof child === 'object') {
        walkTemplate(child);
      }
    }
  }

  if (ast.fragment) {
    walkTemplate(ast.fragment);
  }

  if (ast.css) {
    walkCssAst(ast.css, normalized, source, findings);
  }

  return findings;
}

/**
 * Lints CSS source code.
 *
 * @param {string} source
 * @param {string} relPath
 * @returns {Finding[]} findings
 */
export function lintCssSource(source, relPath = 'anonymous.css') {
  const normalized = relPath.replace(/\\/g, '/');
  if (normalized.includes('src/lib/components/')) {
    return [];
  }

  const findings = [];
  try {
    const cssAst = parseCss(source);
    walkCssAst(cssAst, normalized, source, findings);
  } catch (err) {
    const line = err.start?.line ?? (err.position ? getLineNumber(source, err.position) : 1);
    const message = err.message || 'CSS syntax error';
    findings.push({
      file: normalized,
      line,
      rule: 'unparseable',
      component: message,
      message,
    });
  }

  return findings;
}

/**
 * Recursively collects files matching a filter.
 *
 * @param {string} dir
 * @param {(filePath: string) => boolean} filter
 * @returns {string[]}
 */
export function collectFiles(dir, filter) {
  if (!fs.existsSync(dir)) return [];
  const results = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.name.startsWith('.') || entry.name === 'node_modules') continue;
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...collectFiles(fullPath, filter));
    } else if (entry.isFile() && filter(fullPath)) {
      results.push(fullPath);
    }
  }
  return results;
}

/**
 * Scans the project directory for vocabulary violations.
 *
 * @param {string} appDir
 * @returns {Finding[]} findings sorted by file and line
 */
export function scanProject(appDir) {
  const srcDir = path.join(appDir, 'src');
  const libDir = path.join(srcDir, 'lib');
  const routesDir = path.join(srcDir, 'routes');

  const svelteFilter = (filePath) => {
    if (!filePath.endsWith('.svelte')) return false;
    const rel = normalizePath(filePath, appDir);
    if (rel.includes('src/lib/components/')) return false;
    if (rel.includes('src/test/')) return false;
    return true;
  };

  const cssFilter = (filePath) => {
    if (!filePath.endsWith('.css')) return false;
    const rel = normalizePath(filePath, appDir);
    if (rel.includes('src/lib/components/')) return false;
    if (rel.includes('src/test/')) return false;
    return true;
  };

  const svelteFiles = collectFiles(srcDir, svelteFilter);
  const cssFiles = collectFiles(srcDir, cssFilter);

  const findings = [];

  for (const file of svelteFiles) {
    const source = fs.readFileSync(file, 'utf-8');
    const relPath = normalizePath(file, appDir);
    findings.push(...lintSvelteSource(source, relPath));
  }

  for (const file of cssFiles) {
    const source = fs.readFileSync(file, 'utf-8');
    const relPath = normalizePath(file, appDir);
    findings.push(...lintCssSource(source, relPath));
  }

  findings.sort((a, b) => {
    const fileComp = a.file.localeCompare(b.file);
    if (fileComp !== 0) return fileComp;
    return a.line - b.line;
  });

  return findings;
}

/**
 * Groups findings into a baseline object.
 *
 * @param {Array<object>} findings
 * @returns {Record<string, Record<string, number>>}
 */
export function generateBaseline(findings) {
  const baseline = {};
  for (const f of findings) {
    if (!baseline[f.file]) baseline[f.file] = {};
    baseline[f.file][f.rule] = (baseline[f.file][f.rule] || 0) + 1;
  }
  const sorted = {};
  for (const file of Object.keys(baseline).sort()) {
    sorted[file] = baseline[file];
  }
  return sorted;
}

/**
 * Formats findings as a Markdown table.
 *
 * @param {Array<object>} findings
 * @returns {string}
 */
export function formatTable(findings) {
  const countsByFile = {};
  for (const f of findings) {
    if (!countsByFile[f.file]) {
      countsByFile[f.file] = {
        total: 0,
        button: 0,
        a: 0,
        input: 0,
        textarea: 0,
        select: 0,
        'outline: none': 0,
      };
    }
    countsByFile[f.file][f.rule] = (countsByFile[f.file][f.rule] || 0) + 1;
    countsByFile[f.file].total += 1;
  }

  // Sort files by total descending (worst files first), then by file name
  const sortedFiles = Object.keys(countsByFile).sort((a, b) => {
    const diff = countsByFile[b].total - countsByFile[a].total;
    if (diff !== 0) return diff;
    return a.localeCompare(b);
  });

  const totals = {
    total: 0,
    button: 0,
    a: 0,
    input: 0,
    textarea: 0,
    select: 0,
    'outline: none': 0,
  };

  for (const f of sortedFiles) {
    for (const rule of RULE_NAMES) {
      totals[rule] += countsByFile[f][rule] || 0;
    }
    totals.total += countsByFile[f].total;
  }

  const lines = [
    '| File | button | a | input | textarea | select | outline: none | Total |',
    '| :--- | :---: | :-: | :---: | :------: | :----: | :-----------: | :---: |',
  ];

  for (const file of sortedFiles) {
    const c = countsByFile[file];
    lines.push(
      `| ${file} | ${c.button} | ${c.a} | ${c.input} | ${c.textarea} | ${c.select} | ${c['outline: none']} | ${c.total} |`,
    );
  }

  lines.push(
    `| **Total** | **${totals.button}** | **${totals.a}** | **${totals.input}** | **${totals.textarea}** | **${totals.select}** | **${totals['outline: none']}** | **${totals.total}** |`,
  );

  return lines.join('\n');
}

/**
 * Main CLI runner.
 *
 * @param {string[]} [argv]
 * @param {string} [appDirOverride]
 * @returns {number} exit code
 */
export function runCli(argv = process.argv.slice(2), appDirOverride = null) {
  const isErrorMode = argv.includes('--error');
  const isWriteBaseline = argv.includes('--write-baseline');

  const scriptDir = path.dirname(fileURLToPath(import.meta.url));
  const appDir = appDirOverride ?? path.resolve(scriptDir, '..');

  const findings = scanProject(appDir);

  if (isWriteBaseline) {
    const baseline = generateBaseline(findings);
    const baselinePath = path.join(appDir, 'scripts', 'vocabulary-baseline.json');
    fs.writeFileSync(baselinePath, JSON.stringify(baseline, null, 2) + '\n', 'utf-8');
    console.info(`Updated baseline with ${findings.length} findings across ${Object.keys(baseline).length} files.`);
    return 0;
  }

  for (const f of findings) {
    if (f.rule === 'unparseable') {
      console.warn(`warning: ${f.file}:${f.line} ${f.rule} — ${f.component}`);
    } else {
      console.warn(`warning: ${f.file}:${f.line} ${f.rule} — use ${f.component}`);
    }
  }

  if (findings.length > 0) {
    console.info('\n' + formatTable(findings) + '\n');
    console.info(`Total vocabulary findings: ${findings.length} (${isErrorMode ? 'error' : 'warning'})`);
  } else {
    console.info('No vocabulary findings. All files conform to the vocabulary.');
  }

  if (isErrorMode && findings.length > 0) {
    return 1;
  }
  return 0;
}

const currentFile = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === currentFile) {
  const exitCode = runCli();
  if (exitCode !== 0) {
    process.exit(exitCode);
  }
}

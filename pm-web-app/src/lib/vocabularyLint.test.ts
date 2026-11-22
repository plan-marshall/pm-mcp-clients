import { describe, it, expect, afterEach, vi } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { parse } from 'svelte/compiler';
import {
  RULES,
  lintSvelteSource,
  lintCssSource,
  scanProject,
  formatTable,
  runCli,
} from '../../scripts/vocabulary-lint.mjs';

/**
 * Answers what the Svelte compiler itself reports for a source that does not parse, so a test can
 * state the parser's message and line as the expectation instead of a placeholder.
 *
 * @param source the source the lint reads
 * @returns the error the parser threw
 */
function parserErrorOf(source: string): Error & { start?: { line: number } } {
  try {
    parse(source, { modern: true });
  } catch (err) {
    return err as Error & { start?: { line: number } };
  }
  throw new Error('the source parsed, so there is no parser error to compare against');
}

/**
 * Collects what the CLI writes to `console.error`, so a test can assert the message it claims.
 *
 * @returns the lines the run reports, and the spy to restore
 */
function captureErrors(): { lines: string[]; restore: () => void } {
  const lines: string[] = [];
  const spy = vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
    lines.push(args.map(String).join(' '));
  });
  return { lines, restore: () => spy.mockRestore() };
}

const APP_DIR = path.resolve(__dirname, '../..');
const BASELINE_PATH = path.join(APP_DIR, 'scripts', 'vocabulary-baseline.json');

interface Finding {
  file: string;
  line: number;
  rule: string;
  component: string;
}

function parseFindings(raw: unknown): Finding[] {
  return raw as Finding[];
}

export function evaluateBaseline(
  baseline: Record<string, Record<string, number>>,
  actualFindings: Finding[],
): { valid: boolean; errors: string[] } {
  const errors: string[] = [];
  const actualCounts: Record<string, Record<string, number>> = {};
  const unparseableFiles = new Set<string>();

  for (const f of actualFindings) {
    if (!actualCounts[f.file]) actualCounts[f.file] = {};
    actualCounts[f.file][f.rule] = (actualCounts[f.file][f.rule] || 0) + 1;
    if (f.rule === 'unparseable') {
      unparseableFiles.add(f.file);
    }
  }

  for (const file of unparseableFiles) {
    errors.push(`File ${file} has unparseable syntax (${actualCounts[file].unparseable} error(s))`);
  }

  for (const file of Object.keys(actualCounts)) {
    if (unparseableFiles.has(file)) continue;
    if (!baseline[file]) {
      errors.push(`File ${file} has vocabulary findings but is not in baseline (clean files must stay clean)`);
      continue;
    }
    for (const rule of Object.keys(actualCounts[file])) {
      const actualCount = actualCounts[file][rule];
      const baselineCount = baseline[file]?.[rule] ?? 0;
      if (actualCount > baselineCount) {
        errors.push(`File ${file} rule ${rule} count (${actualCount}) exceeds baseline (${baselineCount})`);
      }
    }
  }

  for (const file of Object.keys(baseline)) {
    if (unparseableFiles.has(file)) continue;
    for (const rule of Object.keys(baseline[file])) {
      const baselineCount = baseline[file][rule];
      const actualCount = actualCounts[file]?.[rule] ?? 0;
      if (baselineCount > actualCount) {
        errors.push(
          `Stale baseline for ${file} rule ${rule}: baseline is ${baselineCount} but actual is ${actualCount} (baseline only shrinks)`,
        );
      }
    }
  }

  return { valid: errors.length === 0, errors };
}

describe('vocabularyLint', () => {
  it('fires once on each raw HTML element outside components', () => {
    const svelte = `
      <script>
        let x = 1;
      </script>
      <div>
        <button onclick={() => {}}>Click</button>
        <a href="/somewhere">Link</a>
        <input type="text" />
        <textarea></textarea>
        <select><option>1</option></select>
      </div>
    `;

    const findings = parseFindings(lintSvelteSource(svelte, 'src/lib/views/SampleView.svelte'));
    expect(findings).toHaveLength(5);

    const rules = findings.map((f) => f.rule);
    expect(rules).toContain('button');
    expect(rules).toContain('a');
    expect(rules).toContain('input');
    expect(rules).toContain('textarea');
    expect(rules).toContain('select');

    const buttonFinding = findings.find((f) => f.rule === 'button');
    expect(buttonFinding?.component).toBe('Button');
    expect(buttonFinding?.line).toBe(6);
  });

  it('fires on outline: none and outline: 0 in <style> block and .css file', () => {
    const svelte = `
      <style>
        .strip:focus-visible {
          outline: none;
        }
        .zero:focus-visible {
          outline: 0;
        }
      </style>
      <div>Hello</div>
    `;

    const svelteFindings = parseFindings(lintSvelteSource(svelte, 'src/lib/views/StyleView.svelte'));
    expect(svelteFindings).toHaveLength(2);
    expect(svelteFindings[0].rule).toBe('outline: none');
    expect(svelteFindings[0].component).toBe('tokens.css (--focus-ring)');
    expect(svelteFindings[0].line).toBe(4);
    expect(svelteFindings[1].rule).toBe('outline: none');
    expect(svelteFindings[1].line).toBe(7);

    const css = `
      button:focus-visible {
        outline: none;
      }
      a:focus-visible {
        outline: 0 !important;
      }
    `;

    const cssFindings = parseFindings(lintCssSource(css, 'src/styles/custom.css'));
    expect(cssFindings).toHaveLength(2);
    expect(cssFindings[0].rule).toBe('outline: none');
    expect(cssFindings[0].line).toBe(3);
    expect(cssFindings[1].rule).toBe('outline: none');
    expect(cssFindings[1].line).toBe(6);
    expect(cssFindings[1].rule).toBe('outline: none');
  });

  it('does not fire inside src/lib/components/', () => {
    const svelte = '<button>Click</button><style>.btn { outline: none; }</style>';
    const findings = parseFindings(lintSvelteSource(svelte, 'src/lib/components/CustomButton.svelte'));
    expect(findings).toHaveLength(0);

    const css = '.btn { outline: none; }';
    const cssFindings = parseFindings(lintCssSource(css, 'src/lib/components/Button.css'));
    expect(cssFindings).toHaveLength(0);
  });

  it('does not fire on components that merely contain the word button or component names', () => {
    const svelte = `
      <Button variant="primary">Save</Button>
      <IconButton icon="act.copy" label="Copy" />
      <RadioButton name="choice" />
      <Link href="/path" label="Path" />
      <TextField label="Name" />
      <TextArea label="Description" />
      <SelectField label="Type" options={[]} />
    `;

    const findings = parseFindings(lintSvelteSource(svelte, 'src/lib/views/CleanView.svelte'));
    expect(findings).toHaveLength(0);
  });

  it('survives {#if} and {#each} blocks', () => {
    const svelte = `
      {#if open}
        <button>Click</button>
      {/if}
      {#each items as item}
        <a href={item.href}>{item.label}</a>
      {/each}
      {#if nested}
        <div>
          {#each items as item}
            <input type="text" />
          {/each}
        </div>
      {/if}
    `;

    const findings = parseFindings(lintSvelteSource(svelte, 'src/lib/views/BlockView.svelte'));
    expect(findings).toHaveLength(3);
    expect(findings.map((f) => f.rule)).toEqual(['button', 'a', 'input']);
  });

  describe('CLI error mode and clean project handling', () => {
    const violatingDir = path.join(APP_DIR, 'target', 'test-violating-app');
    const emptyDir = path.join(APP_DIR, 'target', 'test-empty-app');

    afterEach(() => {
      fs.rmSync(violatingDir, { recursive: true, force: true });
      fs.rmSync(emptyDir, { recursive: true, force: true });
    });

    it('--error exits 1 on project with findings', () => {
      const violatingSrc = path.join(violatingDir, 'src');
      fs.mkdirSync(violatingSrc, { recursive: true });
      fs.writeFileSync(path.join(violatingSrc, 'Violating.svelte'), '<button>Violating</button>');

      const exitCode = runCli(['--error'], violatingDir);
      expect(exitCode).toBe(1);
    });

    it('exits 0 on project with findings when --error is omitted', () => {
      const violatingSrc = path.join(violatingDir, 'src');
      fs.mkdirSync(violatingSrc, { recursive: true });
      fs.writeFileSync(path.join(violatingSrc, 'Violating.svelte'), '<button>Violating</button>');

      const exitCode = runCli([], violatingDir);
      expect(exitCode).toBe(0);
    });

    it('--error exits 0 on clean project without findings', () => {
      const emptySrc = path.join(emptyDir, 'src');
      fs.mkdirSync(emptySrc, { recursive: true });

      const exitCode = runCli(['--error'], emptyDir);
      expect(exitCode).toBe(0);
    });
  });

  it('names the parser message and the line it reports on an unparseable .svelte source', () => {
    const brokenSvelte = '<script>\n  let x = ;\n</script>\n<div>Broken</div>';
    const parserError = parserErrorOf(brokenSvelte);
    expect(parserError.start?.line).toBeGreaterThan(1);

    const findings = parseFindings(lintSvelteSource(brokenSvelte, 'src/lib/views/Broken.svelte'));
    expect(findings).toHaveLength(1);
    expect(findings[0].rule).toBe('unparseable');
    expect(findings[0].file).toBe('src/lib/views/Broken.svelte');
    expect(findings[0].line).toBe(parserError.start?.line);
    expect(findings[0].component).toBe(parserError.message);
    expect(findings[0].message).toBe(parserError.message);

    const tempDir = path.join(APP_DIR, 'target', 'test-unparseable-svelte-app');
    const tempSrc = path.join(tempDir, 'src');
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.writeFileSync(path.join(tempSrc, 'Broken.svelte'), brokenSvelte);
      const exitCode = runCli(['--error'], tempDir);
      expect(exitCode).toBe(1);
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('yields no outline finding on rejected CSS containing outline: 0.5px solid', () => {
    const invalidCss = `
      .broken {
        outline: 0.5px solid red;
        color: ;
      }
    `;
    const findings = parseFindings(lintCssSource(invalidCss, 'src/styles/broken.css'));
    const outlineFindings = findings.filter((f) => f.rule === 'outline: none');
    expect(outlineFindings).toHaveLength(0);
    const unparseableFindings = findings.filter((f) => f.rule === 'unparseable');
    expect(unparseableFindings).toHaveLength(1);
  });

  it('reports a baselined file that no longer parses as unparseable and not as a stale baseline', () => {
    const unparseableFindings: Finding[] = [
      {
        file: 'src/lib/views/PlanView.svelte',
        line: 1,
        rule: 'unparseable',
        component: 'Syntax error in Svelte file',
      },
    ];
    const baseline = {
      'src/lib/views/PlanView.svelte': { button: 9 },
    };
    const result = evaluateBaseline(baseline, unparseableFindings);
    expect(result.valid).toBe(false);
    expect(result.errors.some((e) => e.includes('unparseable'))).toBe(true);
    expect(result.errors.some((e) => e.includes('Stale baseline'))).toBe(false);
  });

  it('--write-baseline refuses to increase baseline counts, names the file and both counts, and leaves the baseline byte-identical', () => {
    const tempDir = path.join(APP_DIR, 'target', 'test-increase-baseline-app');
    const tempSrc = path.join(tempDir, 'src');
    const tempScripts = path.join(tempDir, 'scripts');
    const errors = captureErrors();
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.mkdirSync(tempScripts, { recursive: true });
      const initialBaseline = {
        'src/Sample.svelte': { button: 1 },
      };
      const baselineFile = path.join(tempScripts, 'vocabulary-baseline.json');
      const initialContent = JSON.stringify(initialBaseline, null, 2) + '\n';
      fs.writeFileSync(baselineFile, initialContent, 'utf-8');

      fs.writeFileSync(
        path.join(tempSrc, 'Sample.svelte'),
        '<button>1</button>\n<button>2</button>',
      );

      const exitCode = runCli(['--write-baseline'], tempDir);
      expect(exitCode).toBe(1);

      const reported = errors.lines.join('\n');
      expect(reported).toContain('src/Sample.svelte');
      expect(reported).toContain('rule button');
      expect(reported).toContain('new count is 2');
      expect(reported).toContain('baseline is 1');

      const contentAfter = fs.readFileSync(baselineFile, 'utf-8');
      expect(contentAfter).toBe(initialContent);
    } finally {
      errors.restore();
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('--write-baseline refuses a file the baseline does not hold, names it and both counts', () => {
    const tempDir = path.join(APP_DIR, 'target', 'test-new-file-baseline-app');
    const tempSrc = path.join(tempDir, 'src');
    const tempScripts = path.join(tempDir, 'scripts');
    const errors = captureErrors();
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.mkdirSync(tempScripts, { recursive: true });
      const initialBaseline = {
        'src/Sample.svelte': { button: 1 },
      };
      const baselineFile = path.join(tempScripts, 'vocabulary-baseline.json');
      const initialContent = JSON.stringify(initialBaseline, null, 2) + '\n';
      fs.writeFileSync(baselineFile, initialContent, 'utf-8');

      fs.writeFileSync(path.join(tempSrc, 'Sample.svelte'), '<button>1</button>');
      fs.writeFileSync(path.join(tempSrc, 'Newcomer.svelte'), '<button>fresh</button>');

      const exitCode = runCli(['--write-baseline'], tempDir);
      expect(exitCode).toBe(1);

      const reported = errors.lines.join('\n');
      expect(reported).toContain('src/Newcomer.svelte');
      expect(reported).toContain('found 1 finding(s)');
      expect(reported).toContain('baseline had 0');

      expect(fs.readFileSync(baselineFile, 'utf-8')).toBe(initialContent);
    } finally {
      errors.restore();
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('--write-baseline lowers count and removes files that reached zero', () => {
    const tempDir = path.join(APP_DIR, 'target', 'test-shrink-baseline-app');
    const tempSrc = path.join(tempDir, 'src');
    const tempScripts = path.join(tempDir, 'scripts');
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.mkdirSync(tempScripts, { recursive: true });
      const initialBaseline = {
        'src/FileA.svelte': { button: 2 },
        'src/FileB.svelte': { button: 1 },
      };
      const baselineFile = path.join(tempScripts, 'vocabulary-baseline.json');
      fs.writeFileSync(baselineFile, JSON.stringify(initialBaseline, null, 2) + '\n', 'utf-8');

      fs.writeFileSync(path.join(tempSrc, 'FileA.svelte'), '<button>A1</button>');
      fs.writeFileSync(path.join(tempSrc, 'FileB.svelte'), '<div>No buttons</div>');

      const exitCode = runCli(['--write-baseline'], tempDir);
      expect(exitCode).toBe(0);

      const updated = JSON.parse(fs.readFileSync(baselineFile, 'utf-8'));
      expect(updated['src/FileA.svelte']).toEqual({ button: 1 });
      expect(updated['src/FileB.svelte']).toBeUndefined();
      expect(Object.keys(updated)).toEqual(['src/FileA.svelte']);
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('--write-baseline with missing scripts directory reports the missing directory and does not throw', () => {
    const tempDir = path.join(APP_DIR, 'target', 'test-missing-scripts-app');
    const tempSrc = path.join(tempDir, 'src');
    const errors = captureErrors();
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.writeFileSync(path.join(tempSrc, 'Sample.svelte'), '<button>1</button>');

      let exitCode: number | undefined;
      expect(() => {
        exitCode = runCli(['--write-baseline'], tempDir);
      }).not.toThrow();
      expect(exitCode).toBe(1);

      const reported = errors.lines.join('\n');
      expect(reported).toContain('scripts directory does not exist');
      expect(reported).toContain(path.join(tempDir, 'scripts'));
    } finally {
      errors.restore();
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('--write-baseline leaves an unchanged baseline byte-identical, rule order included', () => {
    const tempDir = path.join(APP_DIR, 'target', 'test-unchanged-baseline-app');
    const tempSrc = path.join(tempDir, 'src');
    const tempScripts = path.join(tempDir, 'scripts');
    try {
      fs.mkdirSync(tempSrc, { recursive: true });
      fs.mkdirSync(tempScripts, { recursive: true });
      // Eight files, as the committed baseline holds, each with its rules in the order a hand
      // wrote them rather than alphabetically: select before button, textarea before input.
      const files: Array<[string, string, string]> = [
        ['Alpha', '<select><option>1</option></select><button>a</button>', 'src/Alpha.svelte'],
        ['Beta', '<select><option>1</option></select><button>b</button>', 'src/Beta.svelte'],
        ['Gamma', '<textarea></textarea><input type="text" />', 'src/Gamma.svelte'],
        ['Delta', '<textarea></textarea><input type="text" />', 'src/Delta.svelte'],
        ['Epsilon', '<a href="/x">x</a><button>e</button>', 'src/Epsilon.svelte'],
        ['Zeta', '<a href="/y">y</a><button>z</button>', 'src/Zeta.svelte'],
        ['Eta', '<select><option>1</option></select><a href="/z">z</a>', 'src/Eta.svelte'],
        ['Theta', '<textarea></textarea><button>t</button>', 'src/Theta.svelte'],
      ];
      const initialBaseline: Record<string, Record<string, number>> = {};
      for (const [name, source, rel] of files) {
        fs.writeFileSync(path.join(tempSrc, `${name}.svelte`), source);
        const findings = parseFindings(lintSvelteSource(source, rel));
        const counts: Record<string, number> = {};
        for (const finding of findings) {
          counts[finding.rule] = (counts[finding.rule] || 0) + 1;
        }
        initialBaseline[rel] = counts;
      }
      const baselineFile = path.join(tempScripts, 'vocabulary-baseline.json');
      const initialContent = JSON.stringify(initialBaseline, null, 2) + '\n';
      fs.writeFileSync(baselineFile, initialContent, 'utf-8');

      const exitCode = runCli(['--write-baseline'], tempDir);
      expect(exitCode).toBe(0);

      expect(fs.readFileSync(baselineFile, 'utf-8')).toBe(initialContent);
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('the script run through a symlink to the app directory prints its findings and exits 1 under --error on a violating tree', () => {
    const tempTarget = path.join(APP_DIR, 'target', 'symlink-real-app');
    const tempSymlink = path.join(APP_DIR, 'target', 'symlink-linked-app');
    const realScripts = path.join(tempTarget, 'scripts');
    const realSrc = path.join(tempTarget, 'src');

    try {
      fs.mkdirSync(realScripts, { recursive: true });
      fs.mkdirSync(realSrc, { recursive: true });

      const realScriptPath = path.join(APP_DIR, 'scripts', 'vocabulary-lint.mjs');
      fs.copyFileSync(realScriptPath, path.join(realScripts, 'vocabulary-lint.mjs'));

      fs.writeFileSync(path.join(realSrc, 'Violating.svelte'), '<button>Violate</button>');

      fs.symlinkSync(tempTarget, tempSymlink, 'dir');

      const symlinkScript = path.join(tempSymlink, 'scripts', 'vocabulary-lint.mjs');
      const targetScript = path.join(realScripts, 'vocabulary-lint.mjs');

      // Matched positive control: run by real path
      const posControl = spawnSync(process.execPath, [targetScript, '--error'], {
        cwd: tempTarget,
        encoding: 'utf-8',
      });
      expect(posControl.status, 'Positive control must exit 1 on violating tree').toBe(1);
      expect(posControl.stderr).toContain('warning:');

      // Test run: run through symlinked directory
      const symlinkRun = spawnSync(process.execPath, [symlinkScript, '--error'], {
        cwd: tempSymlink,
        encoding: 'utf-8',
      });
      expect(symlinkRun.status, 'Symlinked run must exit 1 on violating tree').toBe(1);
      expect(symlinkRun.stderr).toContain('warning:');
    } finally {
      fs.rmSync(tempSymlink, { recursive: true, force: true });
      fs.rmSync(tempTarget, { recursive: true, force: true });
    }
  });

  it('a lint rule added to the rule list appears as a column of the printed table without a finding', () => {
    const addedRule = 'aria-hidden-on-focusable';
    expect(RULES[addedRule]).toBeUndefined();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (RULES as any)[addedRule] = { type: 'element', component: 'SomeComponent' };
    try {
      // The only finding is a `button` one: no finding carries the added rule, so its column can
      // only come from the rule list itself.
      const table = formatTable([
        { file: 'src/Sample.svelte', line: 1, rule: 'button', component: 'Button' },
      ]);
      const rows = table.split('\n');
      const header = rows[0].split('|').map((cell) => cell.trim());
      const column = header.indexOf(addedRule);
      expect(column).toBeGreaterThan(0);
      // The finding row and the totals row both carry a zero in that column.
      expect(rows[2].split('|')[column].trim()).toBe('0');
      expect(rows[rows.length - 1].split('|')[column].trim()).toBe('**0**');
    } finally {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      delete (RULES as any)[addedRule];
    }
  });

  it('enforces vocabulary baseline ratchet (clean files stay clean, counted files never grow)', () => {
    expect(fs.existsSync(BASELINE_PATH), 'vocabulary-baseline.json must exist').toBe(true);

    const baselineRaw = fs.readFileSync(BASELINE_PATH, 'utf-8');
    const baseline = JSON.parse(baselineRaw) as Record<string, Record<string, number>>;

    const actualFindings = parseFindings(scanProject(APP_DIR));
    const result = evaluateBaseline(baseline, actualFindings);
    expect(result.errors).toEqual([]);
    expect(result.valid).toBe(true);
  });
});

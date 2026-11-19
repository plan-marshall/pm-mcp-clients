import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import {
  lintSvelteSource,
  lintCssSource,
  scanProject,
  generateBaseline,
  runCli,
} from '../../scripts/vocabulary-lint.mjs';

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
    expect(svelteFindings[1].rule).toBe('outline: none');

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

  it('--error exits 1 on findings and 0 on clean project', () => {
    const violatingDir = path.join(APP_DIR, 'target', 'test-violating-app');
    const violatingSrc = path.join(violatingDir, 'src');
    fs.mkdirSync(violatingSrc, { recursive: true });
    fs.writeFileSync(path.join(violatingSrc, 'Violating.svelte'), '<button>Violating</button>');

    const exitCodeError = runCli(['--error'], violatingDir);
    expect(exitCodeError).toBe(1);

    const exitCodeNormal = runCli([], violatingDir);
    expect(exitCodeNormal).toBe(0);

    const emptyDir = path.join(APP_DIR, 'target', 'test-empty-app');
    const emptySrc = path.join(emptyDir, 'src');
    fs.mkdirSync(emptySrc, { recursive: true });

    const exitCodeClean = runCli(['--error'], emptyDir);
    expect(exitCodeClean).toBe(0);

    fs.rmSync(violatingDir, { recursive: true, force: true });
    fs.rmSync(emptyDir, { recursive: true, force: true });
  });

  it('yields an unparseable finding on .svelte syntax error and exits 1 under --error', () => {
    const brokenSvelte = '<script>\n  let x = ;\n</script>\n<div>Broken</div>';
    const findings = parseFindings(lintSvelteSource(brokenSvelte, 'src/lib/views/Broken.svelte'));
    expect(findings).toHaveLength(1);
    expect(findings[0].rule).toBe('unparseable');
    expect(findings[0].file).toBe('src/lib/views/Broken.svelte');
    expect(findings[0].line).toBeGreaterThanOrEqual(1);
    expect(findings[0].component).toBeDefined();
    expect(typeof findings[0].component).toBe('string');
    expect(findings[0].component.length).toBeGreaterThan(0);

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

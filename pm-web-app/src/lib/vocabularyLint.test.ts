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
    const exitCodeError = runCli(['--error']);
    expect(exitCodeError).toBe(1);

    const exitCodeNormal = runCli([]);
    expect(exitCodeNormal).toBe(0);

    const emptyDir = path.join(APP_DIR, 'target', 'test-empty-app');
    const exitCodeClean = runCli(['--error'], emptyDir);
    expect(exitCodeClean).toBe(0);
  });

  it('enforces vocabulary baseline ratchet (clean files stay clean, counted files never grow)', () => {
    expect(fs.existsSync(BASELINE_PATH), 'vocabulary-baseline.json must exist').toBe(true);

    const baselineRaw = fs.readFileSync(BASELINE_PATH, 'utf-8');
    const baseline = JSON.parse(baselineRaw) as Record<string, Record<string, number>>;

    const actualFindings = parseFindings(scanProject(APP_DIR));
    const actualCounts: Record<string, Record<string, number>> = {};

    for (const f of actualFindings) {
      if (!actualCounts[f.file]) actualCounts[f.file] = {};
      actualCounts[f.file][f.rule] = (actualCounts[f.file][f.rule] || 0) + 1;
    }

    // 1. Every file with findings must be in the baseline
    for (const file of Object.keys(actualCounts)) {
      expect(
        baseline[file],
        `File ${file} has vocabulary findings but is not in baseline (clean files must stay clean)`,
      ).toBeDefined();

      for (const rule of Object.keys(actualCounts[file])) {
        const actualCount = actualCounts[file][rule];
        const baselineCount = baseline[file]?.[rule] ?? 0;
        expect(
          actualCount,
          `File ${file} rule ${rule} count (${actualCount}) exceeds baseline (${baselineCount})`,
        ).toBeLessThanOrEqual(baselineCount);
      }
    }

    // 2. The baseline must only shrink: no baseline count may exceed actual count
    for (const file of Object.keys(baseline)) {
      for (const rule of Object.keys(baseline[file])) {
        const baselineCount = baseline[file][rule];
        const actualCount = actualCounts[file]?.[rule] ?? 0;
        expect(
          baselineCount,
          `Stale baseline for ${file} rule ${rule}: baseline is ${baselineCount} but actual is ${actualCount} (baseline only shrinks)`,
        ).toBeLessThanOrEqual(actualCount);
      }
    }
  });
});

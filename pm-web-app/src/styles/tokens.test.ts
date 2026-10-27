import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

/**
 * The design system v2 contract of `tokens.css` (VD-P-02, VD-P-03, VD-P-04).
 *
 * The file is read as text and asserted declaration by declaration, so a token that is renamed,
 * retyped or dropped fails here instead of quietly falling back in the browser. The colour pairs
 * listed below — every text-on-surface and state-dot-on-surface pair the views render, and the
 * documented exceptions such as ink on a brand fill — are audited for WCAG 2.1 AA in both themes.
 */

const TOKENS_PATH = path.resolve(__dirname, 'tokens.css');

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

/** The colour palette of VD-P-02, with the two tuned brand values and the two tokens the proposal does not name documented in `theme-readability.adoc`. */
const COLOUR_TOKENS: Record<string, { dark: string; light: string }> = {
  '--color-bg-app': { dark: '#0E1116', light: '#F7F7F5' },
  '--color-bg-rail': { dark: '#12161D', light: '#EFEFEB' },
  '--color-bg-surface': { dark: '#161B22', light: '#FFFFFF' },
  '--color-bg-raised': { dark: '#1C2230', light: '#FFFFFF' },
  '--color-bg-subtle': { dark: '#1E2433', light: '#F0F0EC' },
  '--color-border': { dark: '#2A3140', light: '#E3E3DE' },
  '--color-border-strong': { dark: '#3A4356', light: '#CFCFC8' },
  '--color-text-primary': { dark: '#E6EAF2', light: '#16181D' },
  '--color-text-secondary': { dark: '#AEB6C4', light: '#3E4450' },
  '--color-text-muted': { dark: '#8B95A7', light: '#5F6672' },
  '--color-brand': { dark: '#8B7EF9', light: '#5B4BDB' },
  '--color-brand-hover': { dark: '#A093FB', light: '#4A3BC4' },
  '--color-brand-subtle': { dark: '#1E2240', light: '#EEEBFF' },
  '--color-attention': { dark: '#F2A93B', light: '#B86E00' },
  '--color-attention-subtle': { dark: '#2A2112', light: '#FFF4E0' },
  '--color-attention-text': { dark: '#F6C46B', light: '#8A5200' },
  '--color-running': { dark: '#4C9AFF', light: '#1F5FBF' },
  '--color-danger': { dark: '#F25F5C', light: '#C0362F' },
  '--color-danger-subtle': { dark: '#2C1517', light: '#FDECEA' },
  '--color-success': { dark: '#3FB37F', light: '#1E7A4E' },
  '--color-neutral': { dark: '#8B95A7', light: '#7A808B' },
  '--color-epic': { dark: '#B58CFF', light: '#7A3FD1' },
  /** The ink on a brand-coloured fill; the proposal names the brand fill but not the text on it. */
  '--color-on-brand': { dark: '#0E1116', light: '#FFFFFF' },
  '--color-overlay': { dark: 'rgb(0 0 0 / 0.7)', light: 'rgb(15 23 42 / 0.5)' },
};

/** The type scale of VD-P-03, in the px the proposal states; the file declares the rem equivalents. */
const TYPE_SCALE: Record<string, { size: number; line: number }> = {
  '--text-xs': { size: 12, line: 16 },
  '--text-sm': { size: 13, line: 18 },
  '--text-base': { size: 14, line: 20 },
  '--text-md': { size: 16, line: 24 },
  '--text-lg': { size: 18, line: 26 },
  '--text-xl': { size: 22, line: 28 },
  '--text-2xl': { size: 28, line: 34 },
  '--text-3xl': { size: 36, line: 42 },
};

/** The weights of the type scale of VD-P-03. */
const WEIGHT_TOKENS: Record<string, string> = {
  '--weight-regular': '400',
  '--weight-medium': '500',
  '--weight-semibold': '600',
};

/** The spacing scale of VD-P-04: the 4 px scale of today plus 48 px and 64 px. */
const SPACING_TOKENS: Record<string, number> = {
  '--space-1': 4,
  '--space-2': 8,
  '--space-3': 12,
  '--space-4': 16,
  '--space-5': 20,
  '--space-6': 24,
  '--space-8': 32,
  '--space-10': 40,
  '--space-12': 48,
  '--space-16': 64,
};

/** The radius scale of VD-P-04. */
const RADIUS_TOKENS: Record<string, string> = {
  '--radius-sm': '6px',
  '--radius-md': '8px',
  '--radius-lg': '12px',
  '--radius-full': '9999px',
};

/** The elevation steps of VD-P-04, declared per theme. */
const ELEVATION_TOKENS = ['--shadow-1', '--shadow-2', '--shadow-overlay'];

/** The motion tokens of VD-P-04. */
const MOTION_TOKENS: Record<string, string> = {
  '--duration-fast': '120ms',
  '--duration-base': '200ms',
  '--ease-standard': 'cubic-bezier(0.2, 0, 0, 1)',
};

/** The breakpoints of VD-P-04: phone below 768 px, tablet 768–1279 px, desktop from 1280 px. */
const BREAKPOINT_TOKENS: Record<string, string> = {
  '--breakpoint-tablet': '768px',
  '--breakpoint-desktop': '1280px',
};

/** Every old token name the views still use, and the v2 token it now reads. */
const ALIASES: Record<string, string> = {
  '--color-bg-base': '--color-bg-app',
  '--color-bg-inset': '--color-bg-subtle',
  '--color-bg-subtle-hover': '--color-bg-raised',
  '--color-border-subtle': '--color-border',
  '--color-text-inverse': '--color-on-brand',
  '--color-primary': '--color-brand',
  '--color-primary-hover': '--color-brand-hover',
  '--color-primary-subtle': '--color-brand-subtle',
  '--color-primary-text': '--color-on-brand',
  '--color-accent': '--color-brand',
  '--color-accent-hover': '--color-brand-hover',
  '--color-success-hover': '--color-success',
  '--color-success-subtle': '--color-bg-subtle',
  '--color-success-border': '--color-success',
  '--color-success-text': '--color-success',
  '--color-warning': '--color-attention',
  '--color-warning-hover': '--color-attention',
  '--color-warning-subtle': '--color-attention-subtle',
  '--color-warning-border': '--color-attention',
  '--color-warning-text': '--color-attention-text',
  '--color-danger-hover': '--color-danger',
  '--color-danger-border': '--color-danger',
  '--color-danger-text': '--color-danger',
  '--color-danger-contrast': '--color-on-brand',
  '--color-epic-subtle': '--color-bg-subtle',
  '--color-epic-text': '--color-epic',
  '--color-star': '--color-attention-text',
  '--shadow-sm': '--shadow-1',
  '--shadow-md': '--shadow-2',
};

/** The surfaces text is rendered on. */
const SURFACES = [
  '--color-bg-app',
  '--color-bg-rail',
  '--color-bg-surface',
  '--color-bg-raised',
  '--color-bg-subtle',
];

/** Every foreground the interface renders as text, against every surface. */
const TEXT_PAIRS: Array<[string, string]> = [
  ...SURFACES.flatMap((surface) =>
    ['--color-text-primary', '--color-text-secondary', '--color-text-muted'].map(
      (text): [string, string] => [text, surface],
    ),
  ),
  ['--color-brand', '--color-bg-app'],
  ['--color-brand', '--color-bg-rail'],
  ['--color-brand', '--color-bg-surface'],
  ['--color-brand', '--color-bg-raised'],
  ['--color-brand', '--color-brand-subtle'],
  ['--color-brand-hover', '--color-bg-app'],
  ['--color-brand-hover', '--color-bg-rail'],
  ['--color-brand-hover', '--color-bg-surface'],
  ['--color-brand-hover', '--color-bg-raised'],
  ['--color-brand-hover', '--color-bg-subtle'],
  ['--color-text-primary', '--color-brand-subtle'],
  ['--color-text-primary', '--color-danger-subtle'],
  ['--color-on-brand', '--color-brand'],
  ['--color-on-brand', '--color-brand-hover'],
  ['--color-on-brand', '--color-danger'],
  ['--color-attention-text', '--color-attention-subtle'],
  ['--color-text-primary', '--color-attention-subtle'],
  ['--color-danger', '--color-danger-subtle'],
  ['--color-success', '--color-success-subtle'],
  ['--color-star', '--color-bg-surface'],
  ['--color-running', '--color-bg-surface'],
  ['--color-danger', '--color-bg-surface'],
  ['--color-success', '--color-bg-surface'],
  ['--color-epic', '--color-bg-surface'],
];

/** The state dots and edges, which are graphical objects and owe WCAG's 3:1, not the text ratio. */
const STATE_DOTS = [
  '--color-attention',
  '--color-running',
  '--color-danger',
  '--color-success',
  '--color-neutral',
  '--color-epic',
];

/** The dotted pairs of the state colours on every surface. */
const DOT_PAIRS: Array<[string, string]> = SURFACES.flatMap((surface) =>
  STATE_DOTS.map((dot): [string, string] => [dot, surface]),
);

/**
 * @param css the stylesheet
 * @returns the stylesheet without its comments, so a commented-out declaration never counts
 */
function withoutComments(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, '');
}

/**
 * @param source the stylesheet
 * @param openIndex the index of the opening brace
 * @returns the block's content, braces matched
 */
function matchBraces(source: string, openIndex: number): string {
  let depth = 0;
  for (let i = openIndex; i < source.length; i += 1) {
    if (source[i] === '{') {
      depth += 1;
    } else if (source[i] === '}') {
      depth -= 1;
      if (depth === 0) return source.slice(openIndex + 1, i);
    }
  }
  throw new Error('Unbalanced braces in tokens.css');
}

/**
 * @param source the stylesheet
 * @param opener the at-rule or selector whose block is wanted
 * @returns the block's content
 */
function blockOf(source: string, opener: string): string {
  const at = source.indexOf(opener);
  if (at < 0) throw new Error(`tokens.css has no "${opener}" block`);
  const brace = source.indexOf('{', at + opener.length);
  return matchBraces(source, brace);
}

/**
 * @param block a declaration block
 * @returns every custom property the block declares, keyed by its name
 */
function declarations(block: string): Record<string, string> {
  const found: Record<string, string> = {};
  const pattern = /(--[a-zA-Z0-9_-]+)\s*:\s*([^;]+);/g;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(block)) !== null) {
    found[match[1]] = match[2].trim();
  }
  return found;
}

/**
 * @param block a declaration block
 * @returns the names of the custom properties the block declares
 */
function declaredNames(block: string): Set<string> {
  return new Set(Object.keys(declarations(block)));
}

/**
 * @param source the stylesheet
 * @returns the parsed themes, with the `:root` declaration as the light default
 */
function parseThemes(source: string): {
  root: Record<string, string>;
  light: Record<string, string>;
  dark: Record<string, string>;
  mediaDark: Record<string, string>;
} {
  const css = withoutComments(source);
  const root = declarations(blockOf(css, ':root'));
  const light = declarations(blockOf(css, "[data-theme='light']"));
  const dark = declarations(blockOf(css, "[data-theme='dark']"));
  const mediaDark = declarations(blockOf(blockOf(css, '@media (prefers-color-scheme: dark)'), ':root'));
  return { root, light, dark, mediaDark };
}

/**
 * @param px a length in CSS pixels, at the 16 px root the design assumes
 * @returns the rem length the stylesheet declares
 */
function rem(px: number): string {
  return `${px / 16}rem`;
}

/**
 * @param palette the theme's declarations, aliases included
 * @returns the palette with every `var(--token)` alias replaced by the token it names
 */
function resolveAliases(palette: Record<string, string>): Record<string, string> {
  const resolved = { ...palette };
  for (let pass = 0; pass < 5; pass += 1) {
    for (const [name, value] of Object.entries(resolved)) {
      const alias = /^var\((--[a-zA-Z0-9_-]+)\)$/.exec(value.trim());
      const target = alias ? resolved[alias[1]] : undefined;
      if (target && !target.startsWith('var(')) {
        resolved[name] = target;
      }
    }
  }
  return resolved;
}

/**
 * @param colour a `#rrggbb` literal
 * @returns its channels
 */
function rgb(colour: string): { r: number; g: number; b: number } {
  const match = /^#([0-9a-fA-F]{6})$/.exec(colour.trim());
  if (!match) throw new Error(`Not a six-digit hex colour: ${colour}`);
  return {
    r: parseInt(match[1].slice(0, 2), 16),
    g: parseInt(match[1].slice(2, 4), 16),
    b: parseInt(match[1].slice(4, 6), 16),
  };
}

/**
 * @param colour a `#rrggbb` literal
 * @returns the WCAG 2.1 relative luminance
 */
function luminance(colour: string): number {
  const { r, g, b } = rgb(colour);
  const linear = (channel: number): number => {
    const value = channel / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
}

/**
 * @param foreground the text or state colour
 * @param background the surface it is rendered on
 * @returns the WCAG 2.1 contrast ratio
 */
function contrast(foreground: string, background: string): number {
  const a = luminance(foreground);
  const b = luminance(background);
  const lighter = Math.max(a, b);
  const darker = Math.min(a, b);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * @param dir the directory to walk
 * @param extensions the file extensions to collect
 * @returns every file below `dir` with one of the extensions
 */
function walkFiles(dir: string, extensions: string[]): string[] {
  const result: string[] = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      result.push(...walkFiles(fullPath, extensions));
    } else if (extensions.some((ext) => entry.name.endsWith(ext))) {
      result.push(fullPath);
    }
  }
  return result;
}

describe('tokens.css: the design system v2 contract', () => {
  const css = fs.readFileSync(TOKENS_PATH, 'utf8');
  const themes = parseThemes(css);

  describe('VD-P-02 colour tokens', () => {
    it.each(Object.entries(COLOUR_TOKENS))(
      'declares %s in both themes',
      (token, values) => {
        expect(themes.root[token]?.toLowerCase()).toBe(values.light.toLowerCase());
        expect(themes.light[token]?.toLowerCase()).toBe(values.light.toLowerCase());
        expect(themes.dark[token]?.toLowerCase()).toBe(values.dark.toLowerCase());
        expect(themes.mediaDark[token]?.toLowerCase()).toBe(values.dark.toLowerCase());
      },
    );

    it('keeps the set of theme-varying tokens identical in light, dark and the system-dark block', () => {
      const themed = new Set([...Object.keys(COLOUR_TOKENS), '--focus-ring', ...ELEVATION_TOKENS]);
      const isThemedName = (name: string): boolean =>
        name.startsWith('--color-') || name.startsWith('--shadow-') || name === '--focus-ring';
      const stylesheet = withoutComments(css);

      const rootNames = declaredNames(blockOf(stylesheet, ':root'));
      const rootThemed = [...rootNames].filter(isThemedName);
      expect(rootThemed.filter((name) => !themed.has(name) && !(name in ALIASES))).toEqual([]);
      expect([...themed].filter((name) => !rootNames.has(name))).toEqual([]);

      for (const [label, block] of [
        ['light', blockOf(stylesheet, "[data-theme='light']")],
        ['dark', blockOf(stylesheet, "[data-theme='dark']")],
        ['media-dark', blockOf(blockOf(stylesheet, '@media (prefers-color-scheme: dark)'), ':root')],
      ] as const) {
        const declared = [...declaredNames(block)];
        expect(declared.filter(isThemedName).sort(), `${label} is missing a palette token`).toEqual(
          [...themed].sort(),
        );
      }
    });

    it('keeps every old token name as an alias of its v2 token', () => {
      for (const [oldName, newName] of Object.entries(ALIASES)) {
        expect(themes.root[oldName], `${oldName} is not declared`).toBe(`var(${newName})`);
      }
    });
  });

  describe('VD-P-03 type scale', () => {
    it.each(Object.entries(TYPE_SCALE))('declares the size and line of %s', (token, scale) => {
      const lineToken = token.replace('--text-', '--line-height-');
      expect(themes.root[token]).toBe(rem(scale.size));
      expect(themes.root[lineToken]).toBe(rem(scale.line));
    });

    it.each(Object.entries(WEIGHT_TOKENS))('declares the weight %s', (token, value) => {
      expect(themes.root[token]).toBe(value);
    });

    it('declares a sans stack that starts with the self-hosted Inter face', () => {
      expect(themes.root['--font-sans']).toMatch(/^'Inter'/);
      expect(themes.root['--font-sans']).toContain('sans-serif');
    });

    it('declares the tabular-numeral feature settings', () => {
      expect(themes.root['--font-feature-settings']).toBe("'tnum' 1, 'cv11' 1");
    });
  });

  describe('VD-P-04 spacing, radius, elevation, motion and breakpoints', () => {
    it.each(Object.entries(SPACING_TOKENS))('declares the spacing %s', (token, px) => {
      expect(themes.root[token]).toBe(rem(px));
    });

    it.each(Object.entries(RADIUS_TOKENS))('declares the radius %s', (token, value) => {
      expect(themes.root[token]).toBe(value);
    });

    it.each(ELEVATION_TOKENS)('declares the elevation %s in both themes', (token) => {
      expect(themes.light[token]).toContain('rgb(');
      expect(themes.dark[token]).toContain('rgb(');
      expect(themes.dark[token]).not.toBe(themes.light[token]);
    });

    it.each(Object.entries(MOTION_TOKENS))('declares the motion token %s', (token, value) => {
      expect(themes.root[token]).toBe(value);
    });

    it('turns motion off where the person asked for reduced motion', () => {
      const reduced = blockOf(withoutComments(css), '@media (prefers-reduced-motion: reduce)');
      expect(reduced).toContain('--duration-fast: 0ms');
      expect(reduced).toContain('--duration-base: 0ms');
    });

    it.each(Object.entries(BREAKPOINT_TOKENS))('declares the breakpoint %s', (token, value) => {
      expect(themes.root[token]).toBe(value);
    });
  });

  describe('WCAG 2.1 AA contrast of every pair, in both themes', () => {
    it.each(['light', 'dark'] as const)('keeps every text pair at or above 4.5:1 in the %s theme', (theme) => {
      const palette = resolveAliases({ ...themes.root, ...(theme === 'light' ? themes.light : themes.dark) });
      const failures = TEXT_PAIRS.map(([foreground, background]) => {
        const ratio = contrast(palette[foreground], palette[background]);
        return { foreground, background, ratio: Math.round(ratio * 100) / 100 };
      }).filter((pair) => pair.ratio < 4.5);

      expect(failures).toEqual([]);
    });

    it.each(['light', 'dark'] as const)('keeps every state dot at or above 3:1 in the %s theme', (theme) => {
      const palette = resolveAliases({ ...themes.root, ...(theme === 'light' ? themes.light : themes.dark) });
      const failures = DOT_PAIRS.map(([foreground, background]) => {
        const ratio = contrast(palette[foreground], palette[background]);
        return { foreground, background, ratio: Math.round(ratio * 100) / 100 };
      }).filter((pair) => pair.ratio < 3);

      expect(failures).toEqual([]);
    });
  });
});

describe('tokens.css: static scans over the source tree', () => {
  const css = fs.readFileSync(TOKENS_PATH, 'utf8');

  it('ensures every var(--...) referenced in src/**/*.{svelte,css} is defined in tokens.css or allowlisted', () => {
    const allDefinedTokens = new Set<string>();
    const defRegex = /(--[a-zA-Z0-9_-]+)\s*:/g;
    let m;
    while ((m = defRegex.exec(css)) !== null) {
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
    const sourceFiles = walkFiles(srcDir, ['.svelte', '.css']).filter((f) => !f.endsWith('tokens.css'));

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

        let match;
        while ((match = hexRegex.exec(line)) !== null) {
          violations.push({ file: relPath, line: idx + 1, literal: match[0], snippet: trimmed });
        }
        while ((match = rgbRegex.exec(line)) !== null) {
          violations.push({ file: relPath, line: idx + 1, literal: match[0], snippet: trimmed });
        }
        while ((match = hslRegex.exec(line)) !== null) {
          violations.push({ file: relPath, line: idx + 1, literal: match[0], snippet: trimmed });
        }
      });
    }

    expect(violations).toEqual([]);
  });
});

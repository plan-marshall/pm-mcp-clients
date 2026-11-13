import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';
import { contrastRatio, deltaE00, hueDistance, toOklch } from '../test/colourMetrics';

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
  '--ui-icon-size', // Component-local size bound inline on Icon
  '--ui-icon-url', // Component-local mask-image URL bound inline on Icon
  '--workflow-icon-size', // Component-local size bound inline on WorkflowIcon
  '--workflow-icon-url', // Component-local mask-image URL bound inline on WorkflowIcon
  '--compose-lines', // Component-local line count Compose sets from script on its text area
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
  '--color-kind-project': { dark: '#ADCD4A', light: '#606700' },
  '--color-kind-epic': { dark: '#EF8BC5', light: '#BB2D8C' },
  '--color-kind-plan': { dark: '#3BCDDC', light: '#007785' },
  /** The ink on a brand-coloured fill; the proposal names the brand fill but not the text on it. */
  '--color-on-brand': { dark: '#0E1116', light: '#FFFFFF' },
  '--color-overlay': { dark: 'rgb(0 0 0 / 0.7)', light: 'rgb(15 23 42 / 0.5)' },
};

/**
 * The brand palette of the mascot (`theme-readability.adoc`, Mascot Palette Tokens): declared once
 * in `:root`, overridden by no theme block, so both themes resolve the same value.
 */
const MASCOT_TOKENS: Record<string, string> = {
  '--color-mascot-fur': '#E17512',
  '--color-mascot-gold': '#F2B630',
  '--color-mascot-hat': '#A0602E',
  '--color-mascot-cream': '#F6DDB0',
  '--color-mascot-outline': '#2A1A10',
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

/**
 * Every text pair the interface renders: the three text roles on every surface, and each accent
 * role on the surfaces it actually appears on.
 */
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
  ['--color-kind-epic', '--color-bg-subtle'],
];

/** The state dots and edges, which are graphical objects and owe WCAG's 3:1, not the text ratio. */
const STATE_DOTS = [
  '--color-attention',
  '--color-running',
  '--color-danger',
  '--color-success',
  '--color-neutral',
];

/** The tints of the three tinted kinds (`components-tags.adoc`, The Three Kind Tints). */
const KIND_TINTS = ['--color-kind-project', '--color-kind-epic', '--color-kind-plan'];

/**
 * The colours a kind tint must keep clear of: the state tones, the brand and the mascot's orange and
 * gold. The state tones are `STATE_DOTS` itself, so a state token added there is checked here too.
 */
const TINT_NEIGHBOURS = [...STATE_DOTS, '--color-brand', '--color-mascot-fur', '--color-mascot-gold'];

/** Every ground a kind icon stands on, the selected row's brand-subtle among them. */
const TINT_GROUNDS = [...SURFACES, '--color-brand-subtle'];

/** The dotted pairs of the state colours on every surface. */
const DOT_PAIRS: Array<[string, string]> = SURFACES.flatMap((surface) =>
  STATE_DOTS.map((dot): [string, string] => [dot, surface]),
);

/**
 * @param rows measurements, each with its raw `value`
 * @param minimum the least value that passes
 * @returns the rows whose raw value is below the minimum, with the value rounded for the report only
 */
function belowMinimum<T extends { value: number }>(rows: readonly T[], minimum: number): T[] {
  return rows.filter((row) => row.value < minimum).map((row) => ({ ...row, value: Math.round(row.value * 100) / 100 }));
}

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
      expect(
        rootThemed.filter((name) => !themed.has(name) && !(name in ALIASES) && !(name in MASCOT_TOKENS)),
      ).toEqual([]);
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

    it('declares the mascot palette tokens in :root only, overridden by no theme block', () => {
      const stylesheet = withoutComments(css);
      for (const [label, block] of [
        ['light', blockOf(stylesheet, "[data-theme='light']")],
        ['dark', blockOf(stylesheet, "[data-theme='dark']")],
        ['media-dark', blockOf(blockOf(stylesheet, '@media (prefers-color-scheme: dark)'), ':root')],
      ] as const) {
        const declared = declaredNames(block);
        expect(
          Object.keys(MASCOT_TOKENS).filter((name) => declared.has(name)),
          `${label} overrides a mascot token`,
        ).toEqual([]);
      }
    });

    it('keeps every old token name as an alias of its v2 token', () => {
      for (const [oldName, newName] of Object.entries(ALIASES)) {
        expect(themes.root[oldName], `${oldName} is not declared`).toBe(`var(${newName})`);
      }
    });
  });

  describe('mascot palette tokens', () => {
    it.each(Object.entries(MASCOT_TOKENS))('resolves %s to the same value in every palette', (token, value) => {
      const palettes = {
        light: resolveAliases({ ...themes.root, ...themes.light }),
        dark: resolveAliases({ ...themes.root, ...themes.dark }),
        'media-dark': resolveAliases({ ...themes.root, ...themes.mediaDark }),
      };
      for (const [label, palette] of Object.entries(palettes)) {
        expect(palette[token]?.toLowerCase(), `${token} in the ${label} palette`).toBe(value.toLowerCase());
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
      const failures = belowMinimum(
        TEXT_PAIRS.map(([foreground, background]) => ({
          foreground,
          background,
          value: contrastRatio(palette[foreground], palette[background]),
        })),
        4.5,
      );

      expect(failures).toEqual([]);
    });

    it.each(['light', 'dark'] as const)('keeps every state dot at or above 3:1 in the %s theme', (theme) => {
      const palette = resolveAliases({ ...themes.root, ...(theme === 'light' ? themes.light : themes.dark) });
      const failures = belowMinimum(
        DOT_PAIRS.map(([foreground, background]) => ({
          foreground,
          background,
          value: contrastRatio(palette[foreground], palette[background]),
        })),
        3,
      );

      expect(failures).toEqual([]);
    });
  });

  describe('the three kind tints keep clear of every other colour (components-tags.adoc, kind-tints)', () => {
    const palettes = {
      light: resolveAliases({ ...themes.root, ...themes.light }),
      dark: resolveAliases({ ...themes.root, ...themes.dark }),
    };
    const tintPairs = KIND_TINTS.flatMap((tint, index) =>
      KIND_TINTS.slice(index + 1).map((other): [string, string] => [tint, other]),
    );

    it.each(['light', 'dark'] as const)(
      'keeps a ΔE00 of at least 20 to each state, brand and mascot colour in the %s theme',
      (theme) => {
        const palette = palettes[theme];
        const failures = belowMinimum(
          KIND_TINTS.flatMap((tint) =>
            TINT_NEIGHBOURS.map((other) => ({ tint, other, value: deltaE00(palette[tint], palette[other]) })),
          ),
          20,
        );

        expect(failures).toEqual([]);
      },
    );

    it.each(['light', 'dark'] as const)(
      'keeps an OKLCH hue at least 30° from each of those that has a hue in the %s theme',
      (theme) => {
        const palette = palettes[theme];
        const hued = TINT_NEIGHBOURS.filter((other) => toOklch(palette[other]).c > 0.06);
        const failures = belowMinimum(
          KIND_TINTS.flatMap((tint) =>
            hued.map((other) => ({
              tint,
              other,
              value: hueDistance(toOklch(palette[tint]).h, toOklch(palette[other]).h),
            })),
          ),
          30,
        );

        expect(hued).not.toContain('--color-neutral');
        expect(failures).toEqual([]);
      },
    );

    it.each(['light', 'dark'] as const)('keeps a ΔE00 of at least 30 between the tints in the %s theme', (theme) => {
      const palette = palettes[theme];
      const failures = belowMinimum(
        tintPairs.map(([tint, other]) => ({ tint, other, value: deltaE00(palette[tint], palette[other]) })),
        30,
      );

      expect(failures).toEqual([]);
    });

    it.each(['light', 'dark'] as const)('keeps every tint at or above 3:1 on every ground in the %s theme', (theme) => {
      const palette = palettes[theme];
      const failures = belowMinimum(
        KIND_TINTS.flatMap((tint) =>
          TINT_GROUNDS.map((ground) => ({ tint, ground, value: contrastRatio(palette[tint], palette[ground]) })),
        ),
        3,
      );

      expect(failures).toEqual([]);
    });

    it('declares none of the retired epic tokens in :root or any theme block', () => {
      const retired = ['--color-epic', '--color-epic-subtle', '--color-epic-text'];
      const declaredRetired = Object.entries(themes).flatMap(([block, declared]) =>
        retired.filter((name) => name in declared).map((name) => `${block}: ${name}`),
      );

      expect(declaredRetired).toEqual([]);
    });
  });

  describe('the reason tones (components-tags.adoc, reason-badge)', () => {
    /** The two pairs a `ReasonBadge` draws: amber for every reason, red for the two that stop work. */
    const REASON_TONE_PAIRS: Array<[string, string]> = [
      ['--color-attention-text', '--color-attention-subtle'],
      ['--color-danger', '--color-danger-subtle'],
    ];

    it('draws each tone from the pair audited below', () => {
      const source = fs.readFileSync(path.resolve(__dirname, '../lib/components/ReasonBadge.svelte'), 'utf8');

      expect(source).toContain('background-color: var(--color-attention-subtle)');
      expect(source).toContain('color: var(--color-attention-text)');
      expect(source).toContain('background-color: var(--color-danger-subtle)');
      expect(source).toContain('color: var(--color-danger)');
    });

    it.each(['light', 'dark'] as const)('keeps both pairs at or above 4.5:1 in the %s theme', (theme) => {
      const palette = resolveAliases({ ...themes.root, ...(theme === 'light' ? themes.light : themes.dark) });
      const failures = belowMinimum(
        REASON_TONE_PAIRS.map(([foreground, background]) => ({
          foreground,
          background,
          value: contrastRatio(palette[foreground], palette[background]),
        })),
        4.5,
      );

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

  it('ensures no mascot colour literal appears in a .svelte or .css file under src/ other than tokens.css', () => {
    const srcDir = path.resolve(__dirname, '..');
    const sourceFiles = walkFiles(srcDir, ['.svelte', '.css']).filter(
      (f) => path.resolve(f) !== TOKENS_PATH,
    );
    const literals = Object.values(MASCOT_TOKENS).map((value) => value.toLowerCase());
    const violations: { file: string; line: number; literal: string }[] = [];

    for (const file of sourceFiles) {
      const lines = fs.readFileSync(file, 'utf8').toLowerCase().split('\n');
      lines.forEach((line, idx) => {
        for (const literal of literals) {
          if (line.includes(literal)) {
            violations.push({ file: path.relative(srcDir, file), line: idx + 1, literal });
          }
        }
      });
    }

    expect(violations).toEqual([]);
  });
});

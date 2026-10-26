import { readFileSync } from 'node:fs';
import path from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_STORAGE_KEY,
  applyTheme,
  readThemeChoice,
  resolveTheme,
  systemPrefersDark,
  watchSystemTheme,
  writeThemeChoice,
} from './theme';

/**
 * The theme a person chooses is a choice, not a guess: it survives a reload, the third choice
 * follows the operating system, and a browser that cannot read its storage still gets a theme.
 */
describe('the remembered theme', () => {
  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('keeps a chosen light theme across a reload', async () => {
    writeThemeChoice('light');

    // A reload is a fresh module in a browser that still holds the stored choice.
    vi.resetModules();
    const reloaded = await import('./theme');

    expect(reloaded.readThemeChoice()).toBe('light');
    expect(reloaded.resolveTheme(reloaded.readThemeChoice(), true)).toBe('light');
  });

  it.each([
    ['follows a dark system', true, 'dark'],
    ['follows a light system', false, 'light'],
  ])('%s while the system choice is selected', (_name, prefersDark, expected) => {
    expect(resolveTheme('system', prefersDark)).toBe(expected);
  });

  it('takes the system choice when the storage cannot be read', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage disabled');
    });

    expect(readThemeChoice()).toBe('system');
  });

  it('takes the system choice when the stored value names no theme', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'sepia');

    expect(readThemeChoice()).toBe('system');
  });

  it('applies the resolved theme to the document', () => {
    expect(applyTheme('dark')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    expect(applyTheme('light')).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('keeps the page usable when the choice cannot be stored', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage full');
    });

    expect(() => writeThemeChoice('dark')).not.toThrow();
  });

  it('reads the system preference from the media query', () => {
    vi.stubGlobal('matchMedia', () => ({
      matches: true,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }));

    expect(systemPrefersDark()).toBe(true);
  });

  it('keeps the pre-paint script in app.html in step with this module', () => {
    // The script cannot import the module: it has to run before any script is fetched. This test
    // is what keeps its key, its three choices, its fallback and its attribute the same as the
    // module's, so changing one of them cannot silently disable the mechanism that prevents the
    // flash.
    const html = readFileSync(path.resolve(__dirname, '../app.html'), 'utf-8');

    expect(html).toContain(`'${THEME_STORAGE_KEY}'`);
    expect(html).toContain("'light'");
    expect(html).toContain("'dark'");
    // The default and the fallback of an unreadable or unknown choice are both the system choice.
    expect(html).toContain("var choice = 'system';");
    expect(html).toContain("choice = 'system';");
    expect(html).toContain('document.documentElement.dataset.theme');
    // In the head, and before the app's own head content: no paint can precede it.
    expect(html.indexOf('dataset.theme')).toBeGreaterThan(-1);
    expect(html.indexOf('dataset.theme')).toBeLessThan(html.indexOf('%sveltekit.head%'));
  });

  it('follows the machine while the system choice is in force', () => {
    let listener: (() => void) | null = null;
    const removeEventListener = vi.fn();
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: (_type: string, handler: () => void) => {
        listener = handler;
      },
      removeEventListener,
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }));
    const onChange = vi.fn();

    const stopWatching = watchSystemTheme(onChange);
    expect(listener).not.toBeNull();
    (listener as unknown as () => void)();
    expect(onChange).toHaveBeenCalledTimes(1);

    stopWatching();
    expect(removeEventListener).toHaveBeenCalledTimes(1);
  });
});

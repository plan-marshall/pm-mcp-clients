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
    // The script keeps no list of the choices: it applies whatever the module stored.
    expect(html).not.toContain("'light' &&");
    // The default and the fallback of an unreadable choice are both the system choice.
    expect(html).toContain("var choice = 'system';");
    expect(html).toContain("choice = 'system';");
    expect(html).toContain('document.documentElement.dataset.theme');
    // In the head, and before the app's own head content: no paint can precede it.
    expect(html.indexOf('dataset.theme')).toBeGreaterThan(-1);
    expect(html.indexOf('dataset.theme')).toBeLessThan(html.indexOf('%sveltekit.head%'));
  });

  it.each([
    ['a stored light choice while the system asks for dark', 'light', true, 'light'],
    ['a stored dark choice while the system asks for light', 'dark', false, 'dark'],
    ['the system choice following a dark system', 'system', true, 'dark'],
    ['the system choice following a light system', 'system', false, 'light'],
    // A choice added after this script was written needs no change here: the script repeats the
    // stored value instead of matching it against a list of its own.
    ['a stored choice this script has never heard of', 'high-contrast', false, 'high-contrast'],
  ])(
    'sets the theme before the first paint for %s',
    (_name, stored: string, prefersDark: boolean, expected: string) => {
      // The script is executed, not merely matched: a script that is present but never runs would
      // leave the attribute unset and the first paint in the wrong theme.
      const html = readFileSync(path.resolve(__dirname, '../app.html'), 'utf-8');
      const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
      expect(script).toBeTruthy();
      localStorage.setItem(THEME_STORAGE_KEY, stored);
      vi.stubGlobal('matchMedia', () => ({
        matches: prefersDark,
        media: '(prefers-color-scheme: dark)',
        onchange: null,
        addEventListener: () => {},
        removeEventListener: () => {},
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => false,
      }));
      document.documentElement.removeAttribute('data-theme');

      new Function(script as string)();

      expect(document.documentElement.getAttribute('data-theme')).toBe(expected);
    },
  );

  it('follows the machine while the system choice is in force', () => {
    let listener: (() => void) | null = null;
    let registeredFor: string | null = null;
    const removeEventListener = vi.fn();
    vi.stubGlobal('matchMedia', () => ({
      matches: false,
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: (type: string, handler: () => void) => {
        registeredFor = type;
        listener = handler;
      },
      removeEventListener,
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }));
    const onChange = vi.fn();

    const stopWatching = watchSystemTheme(onChange);
    // The browser only reports a preference change on `change`; a listener on any other event
    // would never fire, so the registration itself is part of what the test proves.
    expect(registeredFor).toBe('change');
    expect(listener).not.toBeNull();
    (listener as unknown as () => void)();
    expect(onChange).toHaveBeenCalledTimes(1);

    stopWatching();
    expect(removeEventListener).toHaveBeenCalledTimes(1);
  });
});

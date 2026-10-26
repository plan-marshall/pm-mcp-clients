import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_STORAGE_KEY,
  applyTheme,
  readThemeChoice,
  resolveTheme,
  systemPrefersDark,
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
});

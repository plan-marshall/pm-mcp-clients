/**
 * The theme a person chose, and the theme the document shows.
 *
 * The choice belongs to the browser, not to the session: it is stored under `pm_theme` and read
 * again on every load, so a reload, a new tab or a restart keeps it. `system` is the third choice
 * the control offers; it follows `prefers-color-scheme`, and it is also what a browser with no
 * choice, no readable storage, or a stored value that names no theme gets.
 */

/** The three choices the theme control offers. */
export type ThemeChoice = 'light' | 'dark' | 'system';

/** The theme the document actually shows. */
export type ResolvedTheme = 'light' | 'dark';

/** The storage key of the chosen theme. */
export const THEME_STORAGE_KEY = 'pm_theme';

/** The choices, in the order the control shows them. */
export const THEME_CHOICES: readonly ThemeChoice[] = ['light', 'dark', 'system'];

/** The media query the `system` choice follows. */
const DARK_MEDIA_QUERY = '(prefers-color-scheme: dark)';

/**
 * @returns the browser's local storage, or null when it is unavailable (server rendering,
 * disabled storage, private mode)
 */
function themeStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage;
  } catch {
    return null;
  }
}

/**
 * @param value a stored value
 * @returns whether it names one of the three choices
 */
export function isThemeChoice(value: unknown): value is ThemeChoice {
  return value === 'light' || value === 'dark' || value === 'system';
}

/**
 * Reads the theme this browser chose.
 *
 * A browser that keeps no choice, cannot read its storage, or holds a value that names no choice
 * follows the system: that is the only answer that never contradicts the machine.
 *
 * @returns the stored choice, `system` when there is none
 */
export function readThemeChoice(): ThemeChoice {
  const storage = themeStorage();
  if (!storage) return 'system';
  try {
    const stored = storage.getItem(THEME_STORAGE_KEY);
    return isThemeChoice(stored) ? stored : 'system';
  } catch {
    return 'system';
  }
}

/**
 * Stores the chosen theme for the next visit.
 *
 * Storage can be full, disabled, or refuse writes in private mode; the choice still applies to
 * this visit, so a failed write is not an error and never reaches the screen.
 *
 * @param choice the chosen theme
 */
export function writeThemeChoice(choice: ThemeChoice): void {
  const storage = themeStorage();
  if (!storage) return;
  try {
    storage.setItem(THEME_STORAGE_KEY, choice);
  } catch {
    // Full, disabled, or private-mode storage: this visit keeps the choice in memory.
  }
}

/**
 * @returns whether the operating system asks for the dark theme
 */
export function systemPrefersDark(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false;
  return window.matchMedia(DARK_MEDIA_QUERY).matches;
}

/**
 * @param choice the chosen theme
 * @param prefersDark whether the operating system asks for dark
 * @returns the theme the document shows
 */
export function resolveTheme(choice: ThemeChoice, prefersDark: boolean): ResolvedTheme {
  if (choice === 'system') return prefersDark ? 'dark' : 'light';
  return choice;
}

/**
 * Applies the chosen theme to the document.
 *
 * The attribute is what the stylesheets switch on, so it is set before anything is read from it:
 * the inline script in `app.html` does the same before the first paint, and this keeps the
 * document and the choice in step afterwards.
 *
 * @param choice the chosen theme
 * @returns the theme the document now shows
 */
export function applyTheme(choice: ThemeChoice): ResolvedTheme {
  const resolved = resolveTheme(choice, systemPrefersDark());
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = resolved;
  }
  return resolved;
}

/**
 * Subscribes to the machine's theme changes, so the `system` choice follows it while the app runs.
 *
 * @param onChange called after the system preference changed
 * @returns the unsubscribe function
 */
export function watchSystemTheme(onChange: () => void): () => void {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return () => {};
  }
  const media = window.matchMedia(DARK_MEDIA_QUERY);
  const listener = (): void => onChange();
  media.addEventListener('change', listener);
  return () => media.removeEventListener('change', listener);
}

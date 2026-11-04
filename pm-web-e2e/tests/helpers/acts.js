import { expect } from '@playwright/test';

/**
 * Asserts that no element carries the danger background at rest (UX-P-04).
 *
 * A destructive act is never red at rest: the colour belongs to the confirmation dialog alone.
 * The check measures the theme's own `--color-danger`, so it holds in the light and the dark theme
 * alike, and it only runs while no dialog is open — an open confirmation is allowed to be red.
 *
 * A dialog that is mounted is waited for, not skipped: a page that never closes it fails the
 * check instead of reading as "nothing is red". The wait samples the condition the page produces,
 * never a fixed duration.
 *
 * @param page the page to read
 * @param {{ timeout?: number }} [options] how long the dialog may still be open
 */
export async function expectNothingRedAtRest(page, { timeout = 5000 } = {}) {
  await expect
    .poll(() => page.evaluate(() => document.querySelector('[role="dialog"]') !== null), {
      timeout,
      message: 'a dialog is still mounted; a red confirmation is not an act at rest',
    })
    .toBe(false);

  const offenders = await page.evaluate(() => {
    const probe = document.createElement('div');
    probe.style.backgroundColor = 'var(--color-danger)';
    document.body.appendChild(probe);
    const danger = getComputedStyle(probe).backgroundColor;
    probe.remove();
    return Array.from(document.querySelectorAll('*'))
      .filter((element) => getComputedStyle(element).backgroundColor === danger)
      .map((element) => `${element.tagName.toLowerCase()}.${String(element.className ?? '')}`)
      .slice(0, 10);
  });

  expect(offenders).toEqual([]);
}

/**
 * Opens an act bar's overflow, so the acts behind it can be reached.
 *
 * @param scope the row or panel that carries the bar; the page when omitted
 * @returns the opened menu
 */
export async function openOverflow(scope) {
  await scope.getByTestId('action-bar-overflow').click();
  return scope.getByRole('menu');
}

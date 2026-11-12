import { expect } from '@playwright/test';

/**
 * Puts the runtime out of reach of a paired page without stopping the mock the serial suite shares.
 *
 * Every request to the runtime's API fails from here on, and the app reads the runtime again. Any
 * request that still answered would mark the runtime reachable again, so the whole API is cut off,
 * not only the status read; the catalogue was read when the page started and stays in memory. The
 * read is repeated until the page of a runtime that is not reachable stands, because a request that
 * was already on its way when the API was cut off may still answer once.
 *
 * @param {import('@playwright/test').Page} page the paired page
 */
export async function makeRuntimeUnreachable(page) {
  await page.route(
    (url) => url.pathname.startsWith('/api/'),
    (route) => route.abort('connectionrefused'),
  );
  await expect(async () => {
    await page.evaluate(async () => {
      const { initRuntime } = await import('/src/lib/stores/runtime.ts');
      await initRuntime();
    });
    await expect(page.getByTestId('unreachable-view')).toBeVisible({ timeout: 1000 });
  }).toPass({ timeout: 15000 });
}

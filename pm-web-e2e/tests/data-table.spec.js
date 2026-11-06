import { test, expect } from '@playwright/test';
import { pair } from './helpers/pairing.js';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The row menu of a table (VD-P-05, VD-S-04): the control stands in its own column and never
 * overhangs the cell that holds it, at the desktop width and on a phone.
 *
 * Two pages are measured: the development page, which draws the design system's table with the row
 * menu of the component, and the paired-devices view, a product view whose rows carry their acts in
 * the last column.
 */

/**
 * @param page the browser page
 * @param cellSelector the cell that must hold the control
 * @param controlSelector the control inside it; the first button by default
 * @returns the overhang on both sides, or `null` when the cell or the control is missing
 */
const overhangOf = async (page, cellSelector, controlSelector = 'button') => {
  return page.evaluate(
    ({ cell, control }) => {
      const cellElement = document.querySelector(cell);
      const controlElement = cellElement?.querySelector(control);
      if (!cellElement || !controlElement) {
        return null;
      }
      const cellRect = cellElement.getBoundingClientRect();
      const controlRect = controlElement.getBoundingClientRect();
      return {
        // A positive value is an overhang: the control reaches past its cell.
        left: cellRect.left - controlRect.left,
        right: controlRect.right - cellRect.right,
      };
    },
    { cell: cellSelector, control: controlSelector },
  );
};

const SIZES = [
  { width: 1440, height: 900 },
  { width: 390, height: 844 },
];

test.describe('the row menu of the data table', () => {
  test.beforeEach(async ({ page, request }) => {
    await pair(page, request);
  });

  for (const locale of ['en', 'de']) {
    for (const size of SIZES) {
      test(`keeps the row-menu button inside its cell at ${size.width} in ${locale}`, async ({
        page,
      }) => {
        await page.setViewportSize(size);
        // The visible word beside the glyph is the thing the column must hold: it is longer in
        // German, so both locales are measured.
        await page.evaluate((value) => localStorage.setItem('pm_locale', value), locale);
        await page.goto('/dev/components');
        // The word beside the glyph is what the column has to hold; wait for the localised one,
        // so a catalogue that has not arrived cannot be measured as the humanised key.
        await expect(
          page
            .locator('[data-testid="data-table"] tbody tr:first-child td.data-table__menu button')
            .first(),
        ).toContainText(locale === 'de' ? 'Zeilenmenü' : 'Row menu');

        const overhang = await overhangOf(
          page,
          '[data-testid="data-table"] tbody tr:first-child td.data-table__menu',
        );

        expect(overhang).not.toBeNull();
        expect(overhang.left).toBeLessThanOrEqual(0.5);
        expect(overhang.right).toBeLessThanOrEqual(0.5);
      });
    }
  }
});

test.describe('the row menu of a product view', () => {
  test.beforeEach(async ({ page, request }) => {
    // The paired-devices scenario draws the settings table with its acts in the last column.
    const switched = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'paired-devices' },
    });
    expect(switched.ok()).toBeTruthy();
    await pair(page, request);
    await page.goto('/settings');
    await expect(page.locator('.devices-table')).toBeVisible();
  });

  for (const locale of ['en', 'de']) {
    for (const size of SIZES) {
      test(`keeps the devices row menu inside its cell at ${size.width} in ${locale}`, async ({
        page,
      }) => {
        await page.setViewportSize(size);
        await page.evaluate((value) => localStorage.setItem('pm_locale', value), locale);
        await page.goto('/settings');
        await expect(page.locator('.devices-table')).toBeVisible();
        // A hidden control would measure zero on both sides: it must be visible before it is
        // measured, or the check would pass on nothing.
        await expect(
          page.locator(
            '.devices-table tbody tr:first-child td:last-child [data-testid="action-bar-overflow"]',
          ),
        ).toBeVisible();
        // The catalogue of the chosen locale is what labels the control: wait for that name, so a
        // fallback label cannot be measured as the German one.
        await expect(
          page.locator(
            '.devices-table tbody tr:first-child td:last-child [data-testid="action-bar-overflow"]',
          ),
        ).toHaveAccessibleName(locale === 'de' ? 'Mehr' : 'More');

        const overhang = await overhangOf(
          page,
          '.devices-table tbody tr:first-child td:last-child',
          '[data-testid="action-bar-overflow"]',
        );

        expect(overhang).not.toBeNull();
        expect(overhang.left).toBeLessThanOrEqual(0.5);
        expect(overhang.right).toBeLessThanOrEqual(0.5);
      });
    }
  }
});

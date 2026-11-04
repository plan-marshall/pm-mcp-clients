import { test, expect } from '@playwright/test';
import { pair } from './helpers/pairing.js';

/**
 * The row menu of the design system's table (VD-P-05, VD-S-04): the control stands in its own
 * column and never overhangs the cell that holds it, at the desktop width and on a phone.
 *
 * The development page is the one place that draws the table with its row menu; it is not a view
 * the operator navigates to, so a browser check can only measure it there.
 */
test.describe('the row menu of the data table', () => {
  const measured = async (page, selector) => {
    const label = page.getByTestId('data-table').first();
    await expect(label).toBeVisible();
    return page.evaluate((cellSelector) => {
      const cell = document.querySelector(cellSelector);
      const button = cell?.querySelector('button');
      if (!cell || !button) {
        return null;
      }
      const cellRect = cell.getBoundingClientRect();
      const buttonRect = button.getBoundingClientRect();
      return {
        // A positive value is an overhang: the control reaches past its cell.
        left: cellRect.left - buttonRect.left,
        right: buttonRect.right - cellRect.right,
      };
    }, selector);
  };

  test.beforeEach(async ({ page, request }) => {
    await pair(page, request);
  });

  for (const locale of ['en', 'de']) {
    for (const size of [
      { width: 1440, height: 900 },
      { width: 390, height: 844 },
    ]) {
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

        const overhang = await measured(
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

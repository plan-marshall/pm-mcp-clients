import { test, expect } from '@playwright/test';
import {
  continueAndStart,
  openProject,
  openStartSources,
  pair,
  switchScenario,
} from './helpers/pairing.js';

test.describe('Web Client Lesson Sources (UC-STA-8)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'lesson_source');
    await pair(page, request);
    await openProject(page, 'pm-mcp-clients');
  });

  test('lists the unclaimed lessons newest first', async ({ page }) => {
    await openStartSources(page);
    await page.getByTestId('add-lesson').click();

    const panel = page.getByTestId('lessons-panel');
    await expect(panel).toBeVisible();
    await expect(page.getByTestId('lesson-select-les-0007')).toBeVisible();
    await expect(page.getByTestId('lesson-select-les-0009')).toBeVisible();
    // The claimed (les-0011) and retired (les-0012) lessons are never offered.
    await expect(page.getByTestId('lesson-select-les-0011')).toHaveCount(0);
    await expect(page.getByTestId('lesson-select-les-0012')).toHaveCount(0);

    // The panel orders the two unclaimed lessons newest first.
    const lessons = await panel
      .locator('[data-testid^="lesson-select-"]')
      .evaluateAll((buttons) => buttons.map((button) => button.dataset.testid));
    expect(lessons).toEqual(['lesson-select-les-0007', 'lesson-select-les-0009']);
  });

  test('adds a lesson as a source and starts the plan', async ({ page }) => {
    await openStartSources(page);
    await page.getByTestId('add-lesson').click();
    await expect(page.getByTestId('lessons-panel')).toBeVisible();

    await page.getByTestId('lesson-select-les-0007').click();

    await expect(page.getByTestId('lessons-panel')).toHaveCount(0);
    await expect(page.getByTestId('source-list')).toContainText('Retire lessons when a plan lands');

    await continueAndStart(page);

    await expect(page.getByTestId('started-title')).toBeVisible();
  });
});

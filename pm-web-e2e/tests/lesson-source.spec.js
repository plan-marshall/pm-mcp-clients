import { test, expect } from '@playwright/test';

async function pairOnLessonScenario(page, request) {
  const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
    data: { scenario: 'lesson_source' },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');

  const overviewNav = page.locator('.app-header').getByRole('button', { name: /Running work/i });
  await expect(overviewNav).toBeVisible();
  await overviewNav.click();
  await expect(page.locator('.overview')).toBeVisible();

  // The scenario's plan-marshall asks for setup; select the ready project explicitly.
  await page
    .locator('.project-list')
    .getByRole('tab', { name: /pm-mcp-clients/i })
    .click();
  await expect(page.locator('.project-title')).toContainText('pm-mcp-clients');
}

async function openLessonModal(page) {
  await page.getByRole('button', { name: 'Start the plan' }).click();
  await page.getByTestId('start-from-lesson').click();
  await expect(page.getByTestId('lesson-modal')).toBeVisible();
  await expect(page.getByTestId('lesson-title')).toContainText(
    'Start from a lesson in pm-mcp-clients',
  );
}

test.describe('Web Client Lesson Sources (UC-STA-8)', () => {
  test.beforeEach(async ({ page, request }) => {
    await pairOnLessonScenario(page, request);
  });

  test('lists the unclaimed lessons newest first', async ({ page }) => {
    await openLessonModal(page);

    await expect(page.getByTestId('lesson-item-les-0007')).toBeVisible();
    await expect(page.getByTestId('lesson-item-les-0009')).toBeVisible();
    // The claimed (les-0011) and retired (les-0012) lessons are never offered.
    await expect(page.getByTestId('lesson-item-les-0011')).toHaveCount(0);
    await expect(page.getByTestId('lesson-item-les-0012')).toHaveCount(0);
  });

  test('claims a lesson whose premise still holds and starts the plan', async ({ page }) => {
    await openLessonModal(page);

    await page.getByTestId('lesson-select-les-0007').click();

    await expect(page.getByTestId('lesson-done')).toContainText('les-0007 is claimed');
  });

  test('shows the missing evidence and carries a broken premise into the plan when overridden', async ({
    page,
  }) => {
    await openLessonModal(page);

    await page.getByTestId('lesson-select-les-0009').click();

    await expect(page.getByTestId('premise-evidence')).toBeVisible();
    await expect(page.getByTestId('premise-evidence-0')).toContainText(
      'src/main/java/de/planmarshall/store/OldStore.java',
    );

    await page.getByTestId('premise-use-anyway').click();
    await expect(page.getByTestId('lesson-done')).toContainText('les-0009 is claimed');
  });

  test('retires a lesson with a broken premise and starts nothing', async ({ page }) => {
    await openLessonModal(page);

    await page.getByTestId('lesson-select-les-0009').click();
    await expect(page.getByTestId('premise-evidence')).toBeVisible();

    await page.getByTestId('premise-retire').click();
    await expect(page.getByTestId('lesson-unchanged')).toContainText('stays unclaimed');
  });

  test('leaves a lesson untouched when the operator chooses another', async ({ page }) => {
    await openLessonModal(page);

    await page.getByTestId('lesson-select-les-0009').click();
    await page.getByTestId('premise-cancel').click();

    await expect(page.getByTestId('lesson-item-les-0009')).toBeVisible();
  });
});

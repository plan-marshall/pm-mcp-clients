import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

test.describe('Web Client Pairing Flow', () => {
  test.beforeEach(async ({ page, request }) => {
    // A pairing code is single use, so each test starts from a fresh scenario.
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'web-pairing' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage to ensure clean state
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test('displays pairing form when not paired', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('h1')).toHaveText('Pair this browser');
    await expect(page.locator('#pairing-code')).toBeVisible();

    const pairBtn = page.getByRole('button', { name: 'Pair this browser' });
    await expect(pairBtn).toBeDisabled();
  });

  test('presents the mark, the wordmark, the code and the QR on the pairing page', async ({
    page,
  }) => {
    await page.goto('/pair');

    // The page carries the mark at its pairing-page size, beside the visible wordmark.
    const pairView = page.getByTestId('pairing-view');
    await expect(pairView.getByTestId('brand-mark')).toHaveAttribute('data-size', '64');
    await expect(pairView.getByText('plan-marshall')).toBeVisible();

    // The code is entered in the page's largest monospace figure (VD-P-08, --text-3xl).
    const code = page.locator('#pairing-code');
    await expect(code).toBeVisible();
    const codeStyle = await code.evaluate((el) => {
      const style = getComputedStyle(el);
      return { fontSize: style.fontSize, fontFamily: style.fontFamily };
    });
    expect(codeStyle.fontSize).toBe('36px');
    expect(codeStyle.fontFamily.toLowerCase()).toContain('mono');

    // The complete code is also presented as a QR code, drawn locally.
    await code.fill('KXQB-MRTN');
    const qr = page.getByTestId('pairing-qr');
    await expect(qr).toBeVisible();
    expect(await qr.locator('svg rect').count()).toBeGreaterThan(20);
  });

  test('enables pair button and pairs successfully with code', async ({ page }) => {
    await page.goto('/');

    const input = page.locator('#pairing-code');
    await input.fill('KXQB-MRTN');

    const pairBtn = page.getByRole('button', { name: 'Pair this browser' });
    await expect(pairBtn).not.toBeDisabled();

    await pairBtn.click();

    // After pairing against web-pairing scenario, should transition to onboard.mode or the start point
    await expect(page.locator('.nav-rail__title')).toHaveText('plan-marshall');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('automatically pairs when URL contains fragment #code=...', async ({ page }) => {
    await page.goto('/#code=KXQB-MRTN');

    // Should automatically pair, strip the fragment, and open the app's route.
    await expect(page).not.toHaveURL(/#code=/);
    await expect(page).toHaveURL(/\/(onboarding|workspace|projects|overview)$/);
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('keeps a paired browser in the app when the same code URL is opened again', async ({
    page,
  }) => {
    // Arrange: pair once, which consumes the single-use code.
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Act: open the very same URL in the already paired browser.
    await page.goto('/#code=KXQB-MRTN');

    // Assert: no re-redemption, no refusal, no revoked page; the fragment is dropped.
    await expect(page).not.toHaveURL(/#code=/);
    await expect(page.getByTestId('pairing-error')).toHaveCount(0);
    await expect(page.getByText('This browser is no longer paired')).toHaveCount(0);
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('refuses the consumed code in a second browser with the single-use message', async ({
    page,
    browser,
  }) => {
    // Arrange: the first browser consumes the scenario's code.
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    // Act: a second browser redeems the same, already consumed code.
    const secondContext = await browser.newContext();
    const secondPage = await secondContext.newPage();
    await secondPage.goto('/#code=KXQB-MRTN');

    // Assert: a single-use explanation, not a revoked pairing.
    await expect(secondPage.getByTestId('pairing-error')).toContainText(
      'A code works once. Get a fresh one in a terminal of the machine.',
    );
    await expect(secondPage.getByText('This browser is no longer paired')).toHaveCount(0);

    await secondContext.close();
  });

  test('pairs a second browser with the fresh code the development loop issues', async ({
    page,
    browser,
    request,
  }) => {
    // Arrange: the first browser consumes the scenario's code, then the mock issues the next one.
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');

    const issued = await request.post(`${MOCK_RUNTIME_URL}/mock/pairing`);
    expect(issued.ok()).toBeTruthy();
    const { code } = await issued.json();
    expect(code).toBeTruthy();
    expect(code).not.toBe('KXQB-MRTN');

    // Act: the second browser pairs with the fresh code.
    const secondContext = await browser.newContext();
    const secondPage = await secondContext.newPage();
    await secondPage.goto(`/#code=${code}`);

    // Assert
    await expect(secondPage.locator('.app-header__machine')).toContainText('Machine:');
    await expect(secondPage).not.toHaveURL(/#code=/);

    await secondContext.close();
  });
});

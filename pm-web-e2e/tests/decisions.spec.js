import { test, expect } from '@playwright/test';

test.describe('Decisions, Waivers, Own Words & Quorum Voting (UC-PLN-2)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to decisions scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'decisions' },
    });
    expect(res.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  test('displays open decisions and badges in decisions view', async ({ page }) => {
    const decisionsBtn = page.getByRole('button', { name: /Decisions/i });
    await expect(decisionsBtn).toBeVisible();
    await decisionsBtn.click();

    await expect(page.locator('.decisions-view')).toBeVisible();
    await expect(page.locator('.decisions-heading')).toContainText('Decisions');

    // Verify presence of standard decision and waiver request
    await expect(page.getByText('Select merge strategy')).toBeVisible();
    await expect(page.getByText('fix-relay asks for a waiver')).toBeVisible();
    await expect(page.getByText('Coverage of pm-relay at least 80%')).toBeVisible();
  });

  test('answers a multi-option decision with recommendation and quorum voting', async ({
    page,
  }) => {
    await page.getByRole('button', { name: /Decisions/i }).click();
    await expect(page.locator('.decisions-view')).toBeVisible();

    const decisionCard = page.locator('article[data-decision-id="Q-001"]');
    await expect(decisionCard).toBeVisible();

    // Check recommended marking and quorum info
    await expect(decisionCard.getByText('Rebase onto main')).toBeVisible();
    await expect(decisionCard.getByText('← recommended')).toBeVisible();
    await expect(decisionCard.getByText(/2 of 3 votes · 2 models/)).toBeVisible();

    // Select option rebase
    await decisionCard.locator('input[value="rebase"]').click();

    // Click Decide button
    const decideBtn = decisionCard.getByRole('button', { name: 'Decide' });
    await expect(decideBtn).toBeEnabled();
    await decideBtn.click();

    // The card Q-001 should be removed from open decisions
    await expect(decisionCard).not.toBeVisible();
  });

  test('answers a decision using own words with read-back confirmation', async ({ page }) => {
    await page.getByRole('button', { name: /Decisions/i }).click();
    await expect(page.locator('.decisions-view')).toBeVisible();

    const decisionCard = page.locator('article[data-decision-id="Q-001"]');
    await expect(decisionCard).toBeVisible();

    // Click "in my own words"
    await decisionCard.getByText(/in my own words/i).click();

    // Fill in own words
    const textarea = decisionCard.locator('.form-textarea');
    await textarea.fill('Please use rebase to maintain clean history');

    // Click Read back
    const readBackBtn = decisionCard.getByRole('button', { name: 'Read back' });
    await readBackBtn.click();

    // Verify read-back card appears
    await expect(decisionCard.locator('.readback-box')).toBeVisible();
    await expect(decisionCard.getByText(/I understood:/i)).toBeVisible();

    // Confirm
    const confirmBtn = decisionCard.getByRole('button', { name: 'Correct' });
    await confirmBtn.click();

    // The card should be closed
    await expect(decisionCard).not.toBeVisible();
  });

  test('resolves a waiver request with reason', async ({ page }) => {
    await page.getByRole('button', { name: /Decisions/i }).click();
    await expect(page.locator('.decisions-view')).toBeVisible();

    const waiverCard = page.locator('article[data-decision-id="WR-2"]');
    await expect(waiverCard).toBeVisible();

    const waiveBtn = waiverCard.getByRole('button', { name: 'Waive the gate' });
    const declineBtn = waiverCard.getByRole('button', { name: 'Decline' });

    // Both disabled without reason
    await expect(waiveBtn).toBeDisabled();
    await expect(declineBtn).toBeDisabled();

    // Type reason
    const reasonInput = waiverCard.locator('textarea.form-textarea');
    await reasonInput.fill('Temporary waiver approved for generated classes');

    await expect(waiveBtn).toBeEnabled();
    await expect(declineBtn).toBeEnabled();

    // Click Waive
    await waiveBtn.click();

    // The waiver should disappear
    await expect(waiverCard).not.toBeVisible();
  });

  test('cross-client update removes decision when answered elsewhere via SSE', async ({
    page,
    request,
  }) => {
    await page.getByRole('button', { name: /Decisions/i }).click();
    await expect(page.locator('.decisions-view')).toBeVisible();

    const decisionCard = page.locator('article[data-decision-id="Q-001"]');
    await expect(decisionCard).toBeVisible();

    // Answer Q-001 via external REST call
    const res = await request.post('http://127.0.0.1:7420/api/v1/decisions/Q-001/answer', {
      data: {
        option: 'rebase',
        channel: 'cli',
        device: 'terminal-01',
      },
    });
    expect(res.ok()).toBeTruthy();

    // Card should automatically be removed via SSE
    await expect(decisionCard).not.toBeVisible({ timeout: 5000 });
  });
});

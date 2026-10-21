import { test, expect } from '@playwright/test';

const BASE = 'http://127.0.0.1:7420';

test.describe('Decisions, Waivers, Own Words & Quorum Voting (UC-PLN-2)', () => {
  test.beforeEach(async ({ page, request }) => {
    // Switch mock runtime to decisions scenario
    const res = await request.post('http://127.0.0.1:7420/mock/scenario', {
      data: { scenario: 'decisions' },
    });
    expect(res.ok()).toBeTruthy();
    const reset = await request.post('http://127.0.0.1:7420/mock/reset', { data: {} });
    expect(reset.ok()).toBeTruthy();

    // Clear localStorage and pair with code
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine:');
  });

  async function openDecisions(page) {
    await page.getByRole('button', { name: /Decisions/i }).click();
    await expect(page.locator('.decisions-view')).toBeVisible();
  }

  test('displays open decisions and counts them in the tab title', async ({ page }) => {
    await openDecisions(page);

    await expect(page.locator('.decisions-heading')).toContainText('Decisions');
    await expect(page.getByText('Resolve rebase conflict')).toBeVisible();
    await expect(page.getByText('fix-relay asks for a waiver')).toBeVisible();
    await expect(page.getByText('Delete published v1 endpoints?')).toBeVisible();

    // The tab title carries the count, so another tab shows the open work.
    await expect(page).toHaveTitle(/^\(\d+\) Decisions$/);
    await expect(page.locator('.unread-pill')).toContainText('open');
  });

  test('answers a multi-option decision with recommendation and quorum voting', async ({
    page,
  }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-007"]');
    await expect(decisionCard).toBeVisible();

    await expect(decisionCard.getByText('Rebase onto main', { exact: true })).toBeVisible();
    await expect(decisionCard.getByText('← recommended')).toBeVisible();
    await expect(decisionCard.getByText(/2 of 3 votes · 2 models/)).toBeVisible();

    await decisionCard.locator('input[value="2"]').click();

    const decideBtn = decisionCard.getByRole('button', { name: 'Decide' });
    await expect(decideBtn).toBeEnabled();
    await decideBtn.click();

    await expect(decisionCard).not.toBeVisible();
  });

  test('answers a decision using own words with read-back confirmation', async ({ page }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-007"]');
    await expect(decisionCard).toBeVisible();

    await decisionCard.getByText(/in my own words/i).click();

    const textarea = decisionCard.locator('.form-textarea');
    await textarea.fill('Please use rebase to maintain clean history');

    await decisionCard.getByRole('button', { name: 'Read back' }).click();

    await expect(decisionCard.locator('.readback-box')).toBeVisible();
    await expect(decisionCard.getByText(/I understood:/i)).toBeVisible();

    await decisionCard.getByRole('button', { name: 'Correct' }).click();

    await expect(decisionCard).not.toBeVisible();
  });

  test('resolves a waiver request with reason', async ({ page }) => {
    await openDecisions(page);

    const waiverCard = page.locator('article[data-decision-id="WR-2"]');
    await expect(waiverCard).toBeVisible();

    const waiveBtn = waiverCard.getByRole('button', { name: 'Waive the gate' });
    const declineBtn = waiverCard.getByRole('button', { name: 'Decline' });

    await expect(waiveBtn).toBeDisabled();
    await expect(declineBtn).toBeDisabled();

    await waiverCard
      .locator('textarea.form-textarea')
      .fill('Temporary waiver approved for generated classes');

    await expect(waiveBtn).toBeEnabled();
    await expect(declineBtn).toBeEnabled();

    await waiveBtn.click();
    await expect(waiverCard).not.toBeVisible();
  });

  test('keeps an irreversible recommendation one step away until asked', async ({ page }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-009"]');
    await expect(decisionCard).toBeVisible();
    await expect(decisionCard.getByTestId('recommendation-hidden-Q-009')).toBeVisible();
    await expect(decisionCard.getByText('← recommended')).not.toBeVisible();

    await decisionCard.getByRole('button', { name: 'Show the recommendation' }).click();

    await expect(decisionCard.getByTestId('recommendation-Q-009')).toContainText(
      'Recommended by architecture',
    );
    await expect(decisionCard.getByText('← recommended')).toBeVisible();
  });

  test('waits for part 1 before it shows the dependent part 2', async ({ page }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-012"]');
    await expect(decisionCard).toBeVisible();
    await expect(decisionCard.getByText('Answer part 1 first.')).toBeVisible();
    await expect(decisionCard.getByText('At the first start of the new release')).not.toBeVisible();

    await decisionCard.locator('input[value="1"]').first().click();

    await expect(decisionCard.getByText('depends on part 1: A file per plan')).toBeVisible();
    await expect(decisionCard.getByText('At the first start of the new release')).toBeVisible();
  });

  test('offers the chat about a decision beside it', async ({ page }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-007"]');
    await decisionCard.getByRole('button', { name: 'chat about it first' }).click();

    // The mock runtime offers no decision chat; the panel says so instead of pretending.
    await expect(decisionCard.getByTestId('decision-chat-Q-007')).toBeVisible();
    await expect(decisionCard.getByTestId('decision-chat-notice')).toContainText(
      'No chat is available for this decision',
    );
  });

  test('asks once whether the browser may notify, and remembers the answer', async ({ page }) => {
    // The headless browser reports its own permission; the app asks once for the decision.
    await page.addInitScript(() => {
      class FakeNotification {
        static permission = 'default';
        static requestPermission = async () => 'denied';
        constructor() {}
      }
      Object.defineProperty(window, 'Notification', { value: FakeNotification });
    });
    await page.reload();
    await openDecisions(page);
    await expect(page.getByTestId('notify-banner')).not.toBeVisible();

    // A decision arriving raises the one-time permission question, not the app's start.
    await page.request.post(`${BASE}/mock/broadcast`, {
      data: {
        event: 'decision.opened',
        data: {
          id: 'Q-099',
          revision: 1,
          title: 'A decision that arrived',
          subject: 'plan fix-relay',
          scope: 'plan',
          asking_phase: 'execute',
          asking_role: 'implement',
          blocks: 'plan',
          reversibility: 'reversible',
          parts: [],
        },
      },
    });

    await expect(page.getByTestId('notify-banner')).toBeVisible();
    await page.getByRole('button', { name: 'Not now' }).click();

    await expect(page.getByTestId('notify-banner')).not.toBeVisible();
    await expect(page.getByTestId('notify-message')).toContainText('Notifications stay off');
    await expect(page).toHaveTitle(/^\(\d+\) Decisions$/);
  });

  test('cross-client update removes the decision and says where it was answered', async ({
    page,
    request,
  }) => {
    await openDecisions(page);

    const decisionCard = page.locator('article[data-decision-id="Q-007"]');
    await expect(decisionCard).toBeVisible();

    const deviceSecret = await page.evaluate(() => localStorage.getItem('pm_device_secret'));
    expect(deviceSecret).toBeTruthy();
    const res = await request.post(`${BASE}/api/v1/decisions/Q-007/answer`, {
      headers: { Authorization: `Bearer ${deviceSecret}` },
      data: {
        option: '2',
        channel: 'cli',
        device: 'terminal-01',
      },
    });
    expect(res.ok()).toBeTruthy();

    await expect(decisionCard).not.toBeVisible({ timeout: 5000 });
    await expect(page.getByTestId('decision-notices')).toContainText('answered in the console');
  });
});

import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';

/**
 * The web chrome, navigation and action advisories: the header is the only always-on status row,
 * the project mark and the brand open the start page, an action explains itself on hover only where
 * the label does not already say it, and the designer is reachable from the header and from a plan.
 */
test.describe('Web chrome, navigation and action advisories', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
    } catch {
      // Best-effort cleanup.
    }
  });

  test('keeps the connection status in the header and drops the full-width banner', async ({
    page,
  }) => {
    await expect(page.getByTestId('connection-status-banner')).toHaveCount(0);

    const badge = page.getByTestId('header-machine');
    await expect(badge).toBeVisible();
    await expect(badge).toHaveAttribute('data-connection-mode', /loopback|lan/);
    await expect(badge).toHaveAttribute('title', /paired device/);
  });

  test('gives an action a hover advisory only where it adds something', async ({ page }) => {
    const brand = page.getByTestId('brand-home');
    await expect(brand).toHaveAttribute('title', /wherever you usually start/);
    await expect(brand).not.toHaveAttribute('title', 'Start page');

    // A text button whose label says it all carries no tooltip of its own.
    await expect(page.getByTestId('nav-projects-btn')).not.toHaveAttribute('title');
    await expect(page.getByTestId('nav-designer-btn')).not.toHaveAttribute('title');

    // An icon-only control still explains itself.
    await expect(page.getByRole('button', { name: 'Toggle light/dark theme' })).toHaveAttribute(
      'title',
      /light and dark theme/,
    );

    // No control repeats its own visible label on hover.
    const repeated = await page.locator('.app-header__actions button').evaluateAll((buttons) =>
      buttons
        .map((button) => ({
          title: (button.getAttribute('title') ?? '').trim(),
          label: (button.textContent ?? '').trim(),
        }))
        .filter(({ title, label }) => title.length > 0 && title === label),
    );
    expect(repeated).toEqual([]);

    await page.getByTestId('nav-projects-btn').click();
    await expect(page.locator('.project-card').first()).toBeVisible();

    // The icon-only favourite control explains itself; the list's text buttons do not.
    await expect(page.locator('[data-testid^="star-btn-"]').first()).toHaveAttribute(
      'title',
      /favourite/,
    );
    await expect(page.locator('[data-testid^="running-work-btn-"]').first()).not.toHaveAttribute(
      'title',
    );
  });

  test('gives the plan actions an advisory that differs from their label', async ({ page }) => {
    await page.goto('/overview');
    await page.locator('[data-testid="plan-card"]').first().click();
    await expect(page.getByTestId('plan-view')).toBeVisible();

    const jump = page.getByTestId('jump-btn');
    await expect(jump).toHaveAttribute('title', /position that runs now/);
    await expect(jump).not.toHaveAttribute('title', /^to where it is$/i);
    await expect(page.getByTestId('back-btn')).toHaveAttribute(
      'title',
      /Return to the view you came from/,
    );
  });

  test('shows the project mark in the header at desktop and phone width', async ({ page }) => {
    const mark = page.getByTestId('brand-mark');
    await expect(mark).toBeVisible();
    await expect(mark).toHaveAttribute('aria-hidden', 'true');

    await page.setViewportSize({ width: 320, height: 568 });
    await expect(mark).toBeVisible();
    const box = await mark.boundingBox();
    expect(box).not.toBeNull();
    expect(box.width).toBeGreaterThanOrEqual(20);
    expect(box.height).toBeGreaterThanOrEqual(20);
  });

  test('opens the start page from the brand', async ({ page }) => {
    await page.goto('/reports');
    await expect(page.getByTestId('brand-home')).toBeVisible();

    await page.getByTestId('brand-home').click();

    await expect(page).toHaveURL(/\/(overview|projects)$/);
  });

  test('opens the designer from the header and from a plan, keeping the project across a reload', async ({
    page,
  }) => {
    await page.getByTestId('nav-designer-btn').click();
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page).toHaveURL(/\/designer$/);

    await page.goto('/overview');
    await page.locator('[data-testid="plan-card"]').first().click();
    await expect(page.getByTestId('plan-view')).toBeVisible();

    await page.getByTestId('design-process-btn').click();
    await expect(page).toHaveURL(/\/designer\?project=pm-mcp-clients$/);

    await page.reload();
    await expect(page.getByTestId('process-designer')).toBeVisible();
    await expect(page.getByTestId('designer-checked')).toContainText('pm-mcp-clients');
  });
});

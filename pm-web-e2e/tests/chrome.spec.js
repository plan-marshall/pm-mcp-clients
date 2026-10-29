import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';
import { startMockRuntime, stopMockRuntime } from './helpers/mockRuntime.js';

/**
 * The web chrome, navigation and action advisories: the frame is a rail with three groups and a
 * top bar with the path, the search trigger, the live indicator and the machine pill; the brand
 * opens the start page, an action explains itself on hover only where the label does not already
 * say it, and the designer is reachable from the rail and from a plan.
 */
test.describe('Web chrome, navigation and action advisories', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'plan-view');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  test('keeps the connection status in the top bar and drops the full-width banner', async ({
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

    // The theme is three explicit choices in one radio group, each with a visible label that
    // needs no tooltip.
    const theme = page.getByRole('group', { name: 'Theme' });
    await expect(theme.getByRole('radio')).toHaveCount(3);
    for (const name of ['Light', 'Dark', 'System']) {
      await expect(theme.getByRole('radio', { name })).toBeVisible();
      await expect(page.getByTestId(`theme-${name.toLowerCase()}`)).not.toHaveAttribute('title');
    }

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

  test('keeps the chosen theme across a full load', async ({ page }) => {
    // The choice is this browser's, not the operating system's. The system preference is set
    // against the choice in each direction, so a start-up that follows the machine instead of the
    // stored choice fails here instead of passing by accident.
    await page.emulateMedia({ colorScheme: 'light' });
    await page.getByTestId('theme-dark').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    await page.emulateMedia({ colorScheme: 'dark' });
    await page.getByTestId('theme-light').click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');
  });

  test('draws the project mark in the rail head at desktop width', async ({ page }) => {
    const rail = page.getByTestId('nav-rail');
    const mark = rail.getByTestId('brand-mark');
    await expect(mark).toBeVisible();
    await expect(mark).toHaveAttribute('aria-hidden', 'true');
    await expect(rail.getByTestId('brand-mark-plate')).toBeVisible();

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

  test('opens the designer from the rail and from a plan, keeping the project across a reload', async ({
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

test.describe('The app shell on fully-onboarded', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'fully-onboarded');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  test('draws three rail groups with the counts the header carried', async ({ page }) => {
    const rail = page.getByTestId('nav-rail');
    await expect(rail).toBeVisible();

    const groups = rail.getByTestId(/^rail-group-/);
    await expect(groups).toHaveCount(3);
    await expect(rail.getByTestId('rail-group-work')).toContainText('Work');
    await expect(rail.getByTestId('rail-group-records')).toContainText('Records');
    await expect(rail.getByTestId('rail-group-machine')).toContainText('Machine');

    // The counts are the ones the header carried before the shell: 3 open decisions and 5 unread
    // reports in this scenario.
    await expect(page.getByTestId('nav-decisions-btn-count')).toHaveText('3');
    await expect(page.getByTestId('nav-reports-btn-count')).toHaveText('5');
  });

  test('keeps the collapsed rail across a reload', async ({ page }) => {
    const rail = page.getByTestId('nav-rail');
    await expect(rail).not.toHaveClass(/nav-rail--collapsed/);

    await page.getByTestId('rail-toggle').click();
    await expect(rail).toHaveClass(/nav-rail--collapsed/);
    await expect(rail.getByTestId('nav-projects-btn')).toHaveAttribute('title', 'Projects');

    await page.reload();
    await expect(page.getByTestId('nav-rail')).toHaveClass(/nav-rail--collapsed/);
    await expect(page.getByTestId('nav-projects-btn')).toHaveAttribute('title', 'Projects');

    await page.getByTestId('rail-toggle').click();
    await expect(page.getByTestId('nav-rail')).not.toHaveClass(/nav-rail--collapsed/);
  });

  test('renders every route of the rail and the top bar inside the shell', async ({ page }) => {
    const routes = [
      { path: '/overview', view: '[data-testid="overview-view"]' },
      { path: '/plans/fix-relay', view: '[data-testid="plan-view"]' },
      { path: '/epics/relay-hardening', view: '[data-testid="epic-view"]' },
      { path: '/projects', view: '[data-testid="project-explorer-view"]' },
      { path: '/decisions', view: '.decisions-view' },
      { path: '/reports', view: '.reports-view' },
      { path: '/designer', view: '[data-testid="process-designer"]' },
      { path: '/system', view: '[data-testid="system-view"]' },
      { path: '/system/cleanup', view: '[data-testid="cleanup-view"]' },
      { path: '/settings', view: '[data-testid="settings-view"]' },
    ];

    for (const route of routes) {
      await page.goto(route.path);
      await expect.soft(page).toHaveURL(new RegExp(`${route.path}$`));
      await expect.soft(page.locator(route.view)).toBeVisible();
      await expect.soft(page.getByTestId('nav-rail')).toBeVisible();
      await expect.soft(page.getByTestId('top-bar')).toBeVisible();
    }
  });

  test('names where the person stands in the top bar', async ({ page }) => {
    await page.goto('/system/cleanup');
    const topBar = page.getByTestId('top-bar');
    await expect(topBar).toContainText('Machine');
    await expect(topBar).toContainText('System');
    await expect(topBar).toContainText('Cleanup');

    await page.goto('/plans/fix-relay');
    await expect(topBar).toContainText('Work');
    await expect(topBar).toContainText('fix-relay');
  });
});

test.describe('The live indicator', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'fully-onboarded');
    await pair(page, request);
  });

  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup.
    }
  });

  test('reads Live while the event stream is connected', async ({ page }) => {
    const indicator = page.getByTestId('live-indicator');
    await expect(indicator).toBeVisible();
    await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 15000 });
    await expect(indicator).toContainText('Live');
  });

  test('turns amber when the mock stops and live again when it restarts', async ({ page }) => {
    const indicator = page.getByTestId('live-indicator');
    await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 15000 });

    await stopMockRuntime();
    await expect(indicator).toHaveAttribute('data-state', 'reconnecting', { timeout: 15000 });

    await startMockRuntime('fully-onboarded');
    await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 45000 });
    await expect(indicator).toContainText('Live');
  });
});

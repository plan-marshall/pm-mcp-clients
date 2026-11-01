import { test, expect } from '@playwright/test';
import { pair, switchScenario } from './helpers/pairing.js';
import {
  startMockRuntime,
  stopMockRuntime,
  ensureMockRuntime,
  mockRuntimeOwned,
} from './helpers/mockRuntime.js';

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

test.describe('The command palette', () => {
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

  test('opens the first match for a name with Ctrl+K, the name, and Enter', async ({ page }) => {
    await page.goto('/overview');
    const palette = page.getByTestId('command-palette');
    const field = page.getByTestId('command-palette-field');

    // The search button of the top bar is no longer disabled and opens the panel.
    const trigger = page.getByTestId('search-trigger');
    await expect(trigger).toBeEnabled();
    await trigger.click();
    await expect(palette).toBeVisible();
    await expect(field).toBeFocused();

    // The panel is modal: Tab walks its own controls and never reaches the page behind it.
    await page.keyboard.press('Tab');
    expect(
      await page.evaluate(
        () => document.activeElement?.closest('[data-testid="command-palette"]') !== null,
      ),
    ).toBe(true);
    await page.keyboard.press('Shift+Tab');
    expect(
      await page.evaluate(
        () => document.activeElement?.closest('[data-testid="command-palette"]') !== null,
      ),
    ).toBe(true);

    // Esc returns to the view the panel was opened from.
    await page.keyboard.press('Escape');
    await expect(palette).toHaveCount(0);
    await expect(page).toHaveURL(/\/overview$/);

    // Ctrl+K opens the same panel from anywhere, and `/` does where no field has the focus.
    await page.keyboard.press('Control+K');
    await expect(palette).toBeVisible();
    await page.keyboard.press('Escape');
    await page.keyboard.press('/');
    await expect(palette).toBeVisible();

    // The runtime answers the name; the first match is the running epic, and Enter opens it.
    await expect(field).toBeFocused();
    await field.fill('relay');
    const first = page.getByTestId('command-palette-match').first();
    await expect(first).toContainText('Relay Hardening');
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(/\/epics\/relay-hardening$/);
    await expect(page.getByTestId('epic-view')).toBeVisible();
    await expect(palette).toHaveCount(0);
  });

  test('finds the plan of pull request 214 by its number', async ({ page }) => {
    await page.goto('/overview');
    // The panel is part of the shell: it answers once the frame is up.
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const field = page.getByTestId('command-palette-field');
    await field.fill('214');

    const match = page.getByTestId('command-palette-match').first();
    await expect(match).toContainText('Fix Relay Transport');
    await match.click();

    await expect(page).toHaveURL(/\/plans\/fix-relay$/);
    await expect(page.getByTestId('plan-view')).toBeVisible();
  });

  test('opens a decision where it belongs', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const field = page.getByTestId('command-palette-field');
    await field.fill('rebase');

    const match = page.getByTestId('command-palette-match').first();
    await expect(match).toContainText('Resolve rebase conflict');
    await match.click();

    // The decision opens in the decisions view, scrolled to its card, in any layout.
    await expect(page).toHaveURL(/\/decisions\?decision=Q-007$/);
    await expect(page.locator('[data-decision-id="Q-007"]')).toBeVisible();
  });

  test('keeps the matched project ahead of its start act', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const field = page.getByTestId('command-palette-field');
    await field.fill('pm-mcp-core');

    // The project is the first match, and every act follows the matches it belongs to.
    const match = page.getByTestId('command-palette-match').first();
    await expect(match).toContainText('pm-mcp-core');
    const order = await page.evaluate(() =>
      [
        ...document.querySelectorAll(
          '[data-testid="command-palette-match"],[data-testid="command-palette-act"]',
        ),
      ].map((row) => row.getAttribute('data-testid')),
    );
    const firstAct = order.indexOf('command-palette-act');
    expect(firstAct).toBeGreaterThan(0);
    expect(order.slice(0, firstAct).every((kind) => kind === 'command-palette-match')).toBe(true);

    // Enter opens the matched project, not its start route.
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/projects\?project=pm-mcp-core$/);
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });

  test('closes the panel from a labelled control on a phone', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/overview');
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const palette = page.getByTestId('command-palette');
    await expect(palette).toBeVisible();
    const close = page.getByTestId('command-palette-close');
    await expect(close).toBeVisible();
    await expect(close).toHaveAccessibleName('Close');
    await close.click();

    await expect(palette).toHaveCount(0);
    await expect(page).toHaveURL(/\/overview$/);
  });

  test('exposes the arrow-key selection as a combobox', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const field = page.getByTestId('command-palette-field');
    await expect(field).toHaveAttribute('role', 'combobox');
    await expect(field).toHaveAttribute('aria-expanded', 'true');
    await field.fill('relay');
    await expect(page.getByTestId('command-palette-match').first()).toBeVisible();

    // The selected row is announced through the field's active descendant.
    const selected = await field.getAttribute('aria-activedescendant');
    expect(selected).toBeTruthy();
    const option = page.locator(`#${selected}`);
    await expect(option).toHaveAttribute('role', 'option');
    await expect(option).toHaveAttribute('aria-selected', 'true');
    await expect(page.getByRole('listbox')).toBeVisible();

    // ArrowDown moves the selection and the announcement together.
    await page.keyboard.press('ArrowDown');
    const moved = await field.getAttribute('aria-activedescendant');
    expect(moved).not.toBe(selected);
    await expect(page.locator(`#${moved}`)).toHaveAttribute('aria-selected', 'true');
  });

  test('keeps the name when nothing matches', async ({ page }) => {
    await page.goto('/overview');
    await expect(page.getByTestId('top-bar')).toBeVisible();
    await page.keyboard.press('Control+K');

    const field = page.getByTestId('command-palette-field');
    await field.fill('relayy');

    await expect(page.getByTestId('command-palette-empty')).toContainText(
      'Nothing matches \u201crelayy\u201d.',
    );
    await expect(field).toHaveValue('relayy');
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

  test('reaches Offline after the retries and comes back on Try again', async ({ page }) => {
    test.skip(
      !mockRuntimeOwned(),
      'the mock runtime was already running; this suite did not start it',
    );
    // Ten attempts at two seconds, then the restart and the reconnect: the default is too short.
    test.setTimeout(90_000);

    const indicator = page.getByTestId('live-indicator');
    await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 15000 });

    await stopMockRuntime();
    try {
      await expect(indicator).toHaveAttribute('data-state', 'offline', { timeout: 40000 });

      await startMockRuntime('fully-onboarded');
      await page.getByTestId('live-retry').click();
      await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 40000 });
      await expect(indicator).toContainText('Live');
    } finally {
      await ensureMockRuntime('fully-onboarded');
    }
  });

  test('turns amber when the mock stops and live again when it restarts', async ({ page }) => {
    // A suite that found a mock already running (the developer loop) must never stop it.
    test.skip(
      !mockRuntimeOwned(),
      'the mock runtime was already running; this suite did not start it',
    );
    // The bounded waits below add up to 120 seconds; the default 30-second deadline is too short.
    test.setTimeout(120_000);

    const indicator = page.getByTestId('live-indicator');
    await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 15000 });

    await stopMockRuntime();
    try {
      await expect(indicator).toHaveAttribute('data-state', 'reconnecting', { timeout: 15000 });

      await startMockRuntime('fully-onboarded');
      await expect(indicator).toHaveAttribute('data-state', 'connected', { timeout: 45000 });
      await expect(indicator).toContainText('Live');
    } finally {
      // The suite runs serially: a failure here must not leave the following specs without a mock.
      await ensureMockRuntime('fully-onboarded');
    }
  });
});

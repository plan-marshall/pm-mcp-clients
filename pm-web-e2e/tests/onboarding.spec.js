import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { openPairingCode } from './helpers/pairing.js';

/**
 * The onboarding route (UC-ONB-1..10) at desktop and phone width, in light and dark.
 *
 * One route renders the state the runtime reports; the done steps are read-only summaries.
 */

/** The shell's destinations, read from the one list that defines them (`nav.ts`). */
function destinationTestIds() {
  const source = fs.readFileSync(
    fileURLToPath(new URL('../../pm-web-app/src/lib/shell/nav.ts', import.meta.url)),
    'utf8',
  );
  return [...source.matchAll(/testId:\s*'([^']+)'/g)].map((match) => match[1]);
}

async function openOnboarding(page, request, scenario = 'onboarding') {
  const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
    data: { scenario },
  });
  expect(res.ok()).toBeTruthy();

  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/pair#code=KXQB-MRTN');
  await expect(page.locator('.app-header__machine')).toContainText('Machine:');
}

test.describe('Onboarding route (UC-ONB-1..10)', () => {
  test.beforeEach(async ({ page, request }) => {
    await openOnboarding(page, request);
    // A machine that is not onboarded opens the onboarding on its own route.
    await expect(page).toHaveURL(/\/onboarding$/);
  });

  test('opens the onboarding at the mode choice on its own route', async ({ page }) => {
    await expect(page.getByTestId('onboarding-view')).toBeVisible();
    await expect(page.locator('.step-bar')).toBeVisible();
    await expect(page.getByRole('button', { name: /Begin with step 1/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Decide everything for me/i })).toBeVisible();
  });

  test('offers no navigation and marks no step before the machine is onboarded', async ({ page }) => {
    // The frame carries the brand, the machine, the language and the theme, and nothing else.
    await expect(page.getByTestId('brand-inert')).toBeVisible();
    await expect(page.getByTestId('header-machine')).toBeVisible();
    await expect(page.getByTestId('language-toggle')).toBeVisible();
    await expect(page.getByRole('group', { name: 'Theme' })).toBeVisible();

    // No destination, no way into the pages of the app and no search before onboarding is done.
    const destinations = destinationTestIds();
    expect(destinations.length).toBeGreaterThan(0);
    for (const control of [
      'brand-home',
      'search-trigger',
      'live-indicator',
      ...destinations,
    ]) {
      await expect(page.getByTestId(control)).toHaveCount(0);
    }

    // The mode screen is the onboarding's step 0: the bar marks no step at all.
    await expect(page.locator('.step-bar__item--active')).toHaveCount(0);
  });

  test('walks from the mode choice into the language step', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();

    await expect(page.getByRole('heading', { name: 'Choose your languages' })).toBeVisible();
    await expect(page.getByTestId('language-screen')).toBeVisible();
  });

  test('asks the harness-scan consent and holds on not now', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    // The consent's flow is read in English here: the interface language is chosen explicitly so
    // the recorded language does not turn the very next screen into German.
    await page.locator('input[name="interface-language"][value="en"]').check();
    await page.getByTestId('language-record').click();

    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();
    await expect(page.getByText('claude, opencode, codex, agy')).toBeVisible();

    // A declined required consent stays in the same state.
    await page.getByRole('button', { name: /^Not now$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();

    // Accepting it moves on to the harness choice.
    await page.getByRole('button', { name: /^Look for harnesses$/i }).click();
    await expect(
      page.getByRole('heading', { name: 'Choose the harnesses to enrol' }),
    ).toBeVisible();
  });

  test('lists the found harnesses with their tokens and the default', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await page.locator('input[name="interface-language"][value="en"]').check();
    await page.getByTestId('language-record').click();
    await page.getByRole('button', { name: /^Look for harnesses$/i }).click();

    await expect(page.getByTestId('harness-toggle-claude')).toBeChecked();
    await expect(page.getByTestId('harness-toggle-codex')).toBeDisabled();
    await expect(page.getByText('cannot run workers')).toBeVisible();
    await expect(page.getByTestId('harness-check')).toContainText(
      'Check 2 harnesses, Claude Code the default',
    );
  });

  test('opens the automatic run at its one consent', async ({ page }) => {
    await page.getByRole('button', { name: /Decide everything for me/i }).click();

    await expect(page.getByRole('heading', { name: 'Decide everything for me?' })).toBeVisible();
    await expect(page.getByText('This consent is for this run only.')).toBeVisible();
  });

  test('renders at phone width without horizontal overflow', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });

    await expect(page.getByTestId('onboarding-view')).toBeVisible();
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(1);
  });

  test('renders in the dark theme from the custom properties', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.reload();
    await expect(page.getByTestId('onboarding-view')).toBeVisible();

    const background = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue('--color-bg-app').trim(),
    );
    // The catalogue's hex spelling is the formatter's; the token's value is what matters here.
    expect(background.toUpperCase()).toBe('#0E1116');
  });

  test('shows the screen after "Use Deutsch for both" in German', async ({ page }) => {
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await expect(page.getByTestId('language-screen')).toBeVisible();

    // The machine's proposal is Deutsch for the interface and "same as above" for the models, so
    // the button is the design's "Use Deutsch for both".
    await page.locator('input[name="interface-language"][value="de"]').check();
    await page.locator('input[name="model-language"][value="same"]').check();
    await expect(page.getByTestId('language-record')).toHaveText('Use Deutsch for both');
    await page.getByTestId('language-record').click();

    // The next screen is the harness-scan consent, in the language the step just recorded — no
    // reload, and no frame still in English. The runtime's own sentence in the card is its
    // fixture's, so the check reads the words the app itself supplies: the frame's step bar, the
    // page title and the consent's acts.
    await expect(page.getByRole('button', { name: 'Nicht jetzt' })).toBeVisible();
    await expect(page).toHaveTitle('Maschine für plan-marshall einrichten');
    await expect(page.locator('.step-bar')).toContainText('Sprache');
  });
});

test.describe('The recorded interface language', () => {
  test('a fresh browser opens in German after onboarding recorded Deutsch for both', async ({
    page,
    request,
    browser,
  }) => {
    // Arrange: onboarding records German for the interface and the models.
    await openOnboarding(page, request);
    await page.getByRole('button', { name: /Begin with step 1/i }).click();
    await page.locator('input[name="interface-language"][value="de"]').check();
    await page.locator('input[name="model-language"][value="de"]').check();
    await page.getByTestId('language-record').click();
    await expect(
      page.getByRole('heading', { name: 'Look for harnesses on this machine?' }),
    ).toBeVisible();

    // Act: a fresh browser — empty storage — pairs with the same machine.
    const freshContext = await browser.newContext();
    const freshPage = await freshContext.newPage();
    const code = await openPairingCode(request);
    await freshPage.goto('/');
    await freshPage.evaluate(() => localStorage.clear());
    await freshPage.goto(`/#code=${code}`);

    // Assert: it opens in the recorded language, without a reload. The onboarding state is shared
    // with the browser above, so the page may stand on any step; the frame is German either way.
    await expect(freshPage.getByTestId('onboarding-view')).toBeVisible({ timeout: 20000 });
    // The frame carries the brand, and the brand opens nothing before onboarding is done.
    await expect(freshPage.getByTestId('brand-inert')).toBeVisible({ timeout: 20000 });
    await expect(freshPage.getByRole('button', { name: 'Sprache wechseln' })).toBeVisible();
    // The frame's own text is German too, not only the control's accessible name.
    await expect(freshPage.locator('.step-bar')).toContainText('Sprache');
    const stored = await freshPage.evaluate(() => localStorage.getItem('pm_locale'));
    expect(stored).toBeNull();
    await freshContext.close();
  });
});

test.describe('The one settings view', () => {
  test('replaces the old machine settings address with the setup tab', async ({ page, request }) => {
    // The settings are reachable once the machine is onboarded.
    await openOnboarding(page, request, 'entry-device-lifecycle');

    await page.goto('/settings/machine');

    await expect(page).toHaveURL(/\/settings\?tab=setup$/);
    await expect(page.getByTestId('settings-view')).toBeVisible();
    await expect(page.getByTestId('settings-panel-setup')).toBeVisible();
  });

  test('shows every tab of the one settings view', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/settings');
    await expect(page.getByTestId('settings-view')).toBeVisible();

    for (const tab of ['Setup', 'Access', 'Credentials', 'Consents', 'Retention']) {
      await page.getByRole('tab', { name: tab }).click();
      await expect(page.getByTestId(`settings-panel-${tab.toLowerCase()}`)).toBeVisible();
      await expect(page).toHaveURL(new RegExp(`tab=${tab.toLowerCase()}$`));
    }
  });

  test('keeps the recorded setup on the setup tab', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/settings');

    await expect(page.getByTestId('aspect-list')).toBeVisible();
    await expect(page.locator('[data-aspect="language"]')).toBeVisible();
    await expect(page.locator('[data-aspect="defaults"]')).toBeVisible();
  });

  test('withdraws a remembered consent on the consents tab', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/settings');
    await page.getByRole('tab', { name: 'Consents' }).click();

    await page.getByTestId('withdraw-scan_harnesses').click();
    await expect(page.getByText('Withdrawn: you will be asked again next time.')).toBeVisible();
  });

  test('opens the onboarding at the aspect whose Change was clicked', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/settings');
    await page.getByRole('tab', { name: 'Credentials' }).click();
    await expect(page.getByTestId('settings-panel-credentials')).toBeVisible();

    await page.getByTestId('change-aspect-providers').click();

    await expect(page).toHaveURL(/\/onboarding\?step=6$/);
    await expect(page.getByTestId('onboarding-view')).toBeVisible();
  });

  test('returns from the settings to the route it was opened from', async ({ page, request }) => {
    await openOnboarding(page, request, 'entry-device-lifecycle');
    await page.goto('/projects');
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();

    await page.getByRole('button', { name: /Settings/i }).click();
    await expect(page.getByTestId('settings-view')).toBeVisible();

    await page.goBack();
    await expect(page).toHaveURL(/\/projects$/);
    await expect(page.getByTestId('project-explorer-view')).toBeVisible();
  });
});

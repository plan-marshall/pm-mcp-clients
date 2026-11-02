import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { setTheme, auditViewContrast } from './helpers/contrast.js';
import {
  pair,
  switchScenario,
  openProject,
  openStartSources,
  addDescription,
  continueAndStart,
} from './helpers/pairing.js';
import {
  collectTextGuardViolations,
  normalizeViolations,
  diffViolations,
  formatViolation,
} from './helpers/textGuard.js';
import { auditViewA11y, formatViolation as formatA11yViolation } from './helpers/a11y.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Declared inventory of the 22 view targets in `pm-web-app/src/lib/views/`
 * (excluding the top-level dispatcher `AppRouter.svelte`).
 */
export const DECLARED_VIEWS = [
  'OverviewView',
  'PlanView',
  'EpicView',
  'ReportsView',
  'CompletionReportView',
  'DecisionView',
  'SystemView',
  'CleanupView',
  'SettingsView',
  'MachineSettingsView',
  'ProcessDesigner',
  'ProjectExplorerView',
  'StartView',
  'OnboardingView',
  'ProjectOnboardingView',
  'PairingView',
  'RevokedView',
  'ListenerOffView',
  'EpicDecompositionView',
  'DecompositionReviewView',
  'EpicAdoptView',
  'RecipeReadyView',
];

/** Tracks views that have been audited during test execution. */
const AUDIT_CACHE_FILE = path.resolve(__dirname, '../.audited-views.json');
const auditedViews = new Set();

/** The two interface locales every declared view is read in. */
const GUARD_LOCALES = ['de', 'en'];

/** The committed text-guard baseline: one violation list per view and locale. */
const GUARD_BASELINE_FILE = path.resolve(__dirname, 'text-guard-baseline.json');

/**
 * Reads the committed baseline, or an empty one while the baseline is still being bootstrapped.
 *
 * @returns {{ schema: string, views: Record<string, Record<string, Array<{ rule: string, word: string }>>> }}
 *   the baseline document
 */
function readGuardBaseline() {
  if (!fs.existsSync(GUARD_BASELINE_FILE)) {
    return { schema: 'text-guard-baseline/1', views: {} };
  }
  return JSON.parse(fs.readFileSync(GUARD_BASELINE_FILE, 'utf8'));
}

/**
 * Stores one view's entries in the baseline document and writes it back, views and locales in
 * declaration order so the committed file has one stable shape.
 *
 * @param {{ schema: string, views: object }} baseline the baseline document to extend
 * @param {string} view the declared view
 * @param {string} locale the interface locale
 * @param {Array<{ rule: string, word: string }>} violations the collected entries
 */
function writeGuardBaselineEntry(baseline, view, locale, violations) {
  baseline.views[view] = { ...(baseline.views[view] ?? {}), [locale]: violations };
  const ordered = { schema: baseline.schema, views: {} };
  for (const name of DECLARED_VIEWS) {
    const byLocale = baseline.views[name];
    if (!byLocale) {
      continue;
    }
    ordered.views[name] = {};
    for (const active of GUARD_LOCALES) {
      if (byLocale[active]) {
        ordered.views[name][active] = byLocale[active];
      }
    }
  }
  // Written beside the target and renamed, so an interrupted write can never leave half a
  // baseline behind.
  const scratch = `${GUARD_BASELINE_FILE}.tmp`;
  fs.writeFileSync(scratch, `${JSON.stringify(ordered, null, 2)}\n`);
  fs.renameSync(scratch, GUARD_BASELINE_FILE);
}

/**
 * Holds the views one journey collected against the committed baseline: a view that gained a
 * violation fails with the view and the word, and an entry for a defect that is gone asks for
 * the baseline to shrink. Only an explicit `TEXT_GUARD_BOOTSTRAP=1` run writes a missing entry;
 * every other run fails on one, so the baseline never grows behind a review.
 *
 * @param {Record<string, Array<{ rule: string, word: string }>>} collected the violations per view
 * @param {string} locale the interface locale the views were read in
 */
function verifyGuardViews(collected, locale) {
  const baseline = readGuardBaseline();
  const bootstrap = process.env.TEXT_GUARD_BOOTSTRAP === '1';
  const failures = [];
  for (const [view, violations] of Object.entries(collected)) {
    const recorded = baseline.views?.[view]?.[locale];
    if (!recorded) {
      if (bootstrap) {
        writeGuardBaselineEntry(baseline, view, locale, violations);
        console.log(`[text-guard] baseline entry written: ${view} (${locale})`);
      } else {
        failures.push(
          `${view} (${locale}) has no entry in ${path.basename(GUARD_BASELINE_FILE)}; run with TEXT_GUARD_BOOTSTRAP=1 to record it`,
        );
      }
      continue;
    }
    const { added, removed } = diffViolations(recorded, violations);
    for (const violation of added) {
      failures.push(`${view} (${locale}) gained ${formatViolation(violation)}`);
    }
    for (const violation of removed) {
      failures.push(
        `${view} (${locale}) no longer shows ${formatViolation(violation)}; shrink the baseline`,
      );
    }
  }
  expect(
    failures,
    `The text guard baseline differs from the page.\n${failures.join('\n')}`,
  ).toEqual([]);
}

/** Interface labels of the routes and radio buttons, in both interface languages. */
const LABELS = {
  overview: /Laufende Arbeit|Running work/i,
  projects: /^Projekte$|^Projects$/i,
  decisions: /^Entscheidungen$|^Decisions$/i,
  reports: /^Berichte$|^Reports$/i,
  designer: /Prozess-Designer|Process designer/i,
  system: /^System$/,
  settings: /Einstellungen der Maschine|Machine settings/i,
  byEpic: /nach Epic|by epic/i,
  review: /Review…|Sieh es dir an…/i,
  startPlan: /Starte den Plan|Start the plan/i,
  startEpic: /Starte das Epic|Start the epic/i,
};

/**
 * Waits until the app has initialized its runtime, which follows the catalogue load, so the
 * page is rendered in the locale the browser stored before the navigation.
 *
 * @param {import('@playwright/test').Page} page the page to wait for
 */
async function waitForApp(page) {
  await page.waitForFunction(
    async () => {
      const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
      let initialized = false;
      const unsubscribe = runtimeStore.subscribe((state) => {
        initialized = state.isInitialized;
      });
      unsubscribe();
      return initialized;
    },
    undefined,
    { timeout: 20000 },
  );
}

/**
 * Stores the interface language before the next document load, so the app boots in it.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {string} locale the interface locale
 */
async function storeLocale(page, locale) {
  await page.evaluate((active) => localStorage.setItem('pm_locale', active), locale);
}

/**
 * Switches the mock scenario, pairs, and reloads the shell in the requested language.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {import('@playwright/test').APIRequestContext} request the request fixture
 * @param {string} scenario the mock scenario
 * @param {string} locale the interface locale
 */
async function pairInLocale(page, request, scenario, locale) {
  await switchScenario(request, scenario);
  await pair(page, request);
  await storeLocale(page, locale);
  await page.reload();
  await waitForApp(page);
}

/**
 * Opens a route of the paired app in the requested language.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {string} locale the interface locale
 * @param {string} route the path to open
 */
async function gotoInLocale(page, locale, route) {
  await storeLocale(page, locale);
  await page.goto(route);
  await waitForApp(page);
}

/**
 * Opens the overview of one project through the header, in either language.
 *
 * @param {import('@playwright/test').Page} page the page of the running app
 * @param {string} projectId the project to select
 */
async function openProjectOverview(page, projectId) {
  await page.goto('/overview');
  await expect(page.locator('.overview')).toBeVisible();
  await page
    .locator('.project-list')
    .getByRole('tab', { name: new RegExp(projectId, 'i') })
    .click();
  await expect(page.locator('.project-title')).toContainText(projectId);
}

/**
 * One route through the app that ends at declared views, in the order the pages are shown.
 *
 * `open` prepares the app for the locale, `walk` reaches every view and hands each one to the
 * recorder. Every declared view of `DECLARED_VIEWS` is reached exactly once per locale.
 */
const GUARD_JOURNEYS = [
  {
    title: 'overview and epic',
    views: ['OverviewView', 'EpicView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'running-work', locale);
    },
    async walk(page, record) {
      await page.goto('/overview');
      await expect(page.locator('.overview')).toBeVisible();
      await expect(page.locator('[data-testid="plan-card"]').first()).toBeVisible();
      await record('OverviewView');

      await page.getByRole('radio', { name: LABELS.byEpic }).click({ force: true });
      const epicCard = page.getByTestId('epic-card').first();
      await expect(epicCard).toBeVisible();
      await epicCard.click();
      await expect(page.getByTestId('epic-view')).toBeVisible();
      await record('EpicView');
    },
  },
  {
    title: 'plan',
    views: ['PlanView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'plan-view', locale);
    },
    async walk(page, record) {
      await page.goto('/overview');
      await expect(page.locator('.overview')).toBeVisible();
      await page.locator('[data-testid="plan-card"]').first().click();
      await expect(page.getByTestId('plan-view')).toBeVisible();
      await expect(page.getByTestId('plan-loading')).toHaveCount(0, { timeout: 15000 });
      await expect(page.getByTestId('plan-title')).toBeVisible();
      await record('PlanView');
    },
  },
  {
    title: 'reports and completion report',
    views: ['ReportsView', 'CompletionReportView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'reports', locale);
    },
    async walk(page, record) {
      await page.goto('/reports');
      await expect(page.locator('.reports-view')).toBeVisible();
      await expect(page.locator('.report-card').first()).toBeVisible({ timeout: 15000 });
      await record('ReportsView');

      const card = page.locator('.report-card', { hasText: 'fix-relay' });
      await expect(card).toBeVisible();
      await card.locator('.action-btn--detail').click();
      await expect(page.locator('.completion-report')).toBeVisible();
      await expect(page.locator('.report-title')).toBeVisible();
      await record('CompletionReportView');
    },
  },
  {
    title: 'decisions',
    views: ['DecisionView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'decisions', locale);
    },
    async walk(page, record) {
      await page.goto('/decisions');
      await expect(page.locator('.decisions-view')).toBeVisible();
      await expect(page.locator('.decision-card, .standard-decision-card').first()).toBeVisible({
        timeout: 15000,
      });
      await record('DecisionView');
    },
  },
  {
    title: 'system and cleanup',
    views: ['SystemView', 'CleanupView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'system', locale);
    },
    async walk(page, record) {
      await page.goto('/system');
      await expect(page.getByTestId('system-view')).toBeVisible();
      await expect(page.locator('[data-testid="system-view"] .card').first()).toBeVisible({
        timeout: 15000,
      });
      await record('SystemView');

      const review = page.getByRole('button', { name: LABELS.review });
      await expect(review).toBeVisible();
      await review.click();
      await expect(page.getByTestId('cleanup-view')).toBeVisible();
      await record('CleanupView');
    },
  },
  {
    title: 'settings, machine settings and designer',
    views: ['SettingsView', 'MachineSettingsView', 'ProcessDesigner'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'designer', locale);
    },
    async walk(page, record) {
      await page.goto('/settings');
      await expect(page.getByTestId('settings-view')).toBeVisible();
      await expect(page.getByTestId('listener-bar')).toBeVisible({ timeout: 15000 });
      await expect(page.locator('.loading-text')).toHaveCount(0, { timeout: 15000 });
      await record('SettingsView');

      await page.getByTestId('machine-settings-button').click();
      await expect(page.getByTestId('machine-settings-view')).toBeVisible();
      await record('MachineSettingsView');

      await page.getByTestId('open-designer-btn').click();
      await expect(page.getByTestId('process-designer')).toBeVisible();
      await record('ProcessDesigner');
    },
  },
  {
    title: 'project explorer and project onboarding',
    views: ['ProjectExplorerView', 'ProjectOnboardingView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'projects', locale);
      await gotoInLocale(page, locale, '/projects');
    },
    async walk(page, record) {
      await expect(page.getByTestId('project-explorer-view')).toBeVisible();
      await expect(page.getByTestId('section-all-projects')).toBeVisible({ timeout: 15000 });
      await record('ProjectExplorerView');

      const add = page.getByTestId('add-project-btn');
      await expect(add).toBeVisible();
      await add.click();
      await expect(page).toHaveURL(/\/projects\/onboard$/);
      await expect(page.getByTestId('project-onboarding-view')).toBeVisible();
      await record('ProjectOnboardingView');
    },
  },
  {
    title: 'start and recipe ready',
    views: ['StartView', 'RecipeReadyView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'start', locale);
    },
    async walk(page, record) {
      await openProjectOverview(page, 'pm-mcp-clients');
      await page.getByRole('button', { name: LABELS.startPlan }).click();
      await expect(page.getByTestId('start-view')).toBeVisible();
      await expect(page.getByTestId('start-loading')).toHaveCount(0, { timeout: 15000 });
      await record('StartView');

      await page.getByTestId('choose-recipe').click();
      await page.getByTestId('recipe-choose-code-review').click();
      await page.getByTestId('recipe-continue').click();
      await expect(page.getByTestId('recipe-ready')).toBeVisible();
      await record('RecipeReadyView');
    },
  },
  {
    title: 'decomposition review',
    views: ['DecompositionReviewView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'start', locale);
    },
    async walk(page, record) {
      await openProjectOverview(page, 'pm-mcp-clients');
      await page.getByRole('button', { name: LABELS.startEpic }).click();
      await expect(page.getByTestId('start-view')).toBeVisible({ timeout: 15000 });
      await expect(page.getByTestId('start-loading')).toHaveCount(0, { timeout: 15000 });
      if (await page.getByTestId('choose-sources').isVisible()) {
        await page.getByTestId('choose-sources').click();
      } else if (await page.getByTestId('epic-adopt').isVisible()) {
        await page.getByTestId('adopt-new').click();
      }
      await expect(page.getByTestId('source-list')).toBeVisible({ timeout: 15000 });
      await addDescription(page, 'Relay hardening');
      await continueAndStart(page);

      const decompose = page.getByTestId('started-decompose');
      await expect(decompose).toBeVisible();
      await decompose.click();
      await expect(page.getByTestId('decomposition-review')).toBeVisible();
      await record('DecompositionReviewView');
    },
  },
  {
    title: 'epic adopt',
    views: ['EpicAdoptView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'start-epic-adopt', locale);
      await gotoInLocale(page, locale, '/entries/ent-7a2e4f01');
    },
    async walk(page, record) {
      await expect(page.getByTestId('start-view')).toBeVisible();
      await expect(page.getByTestId('epic-adopt')).toBeVisible();
      await record('EpicAdoptView');
    },
  },
  {
    title: 'onboarding',
    views: ['OnboardingView'],
    async open(page, request, locale) {
      await switchScenario(request, 'onboarding');
      await page.goto('/');
      await page.evaluate(() => localStorage.clear());
      await storeLocale(page, locale);
      await page.goto('/pair#code=KXQB-MRTN');
      await waitForApp(page);
    },
    async walk(page, record) {
      await expect(page).toHaveURL(/\/onboarding$/);
      await expect(page.getByTestId('onboarding-view')).toBeVisible();
      await expect(page.getByTestId('steps-overview')).toBeVisible({ timeout: 15000 });
      await record('OnboardingView');
    },
  },
  {
    title: 'pairing',
    views: ['PairingView'],
    async open(page, request, locale) {
      await switchScenario(request, 'web-pairing');
      await page.goto('/');
      await page.evaluate(() => localStorage.clear());
      await storeLocale(page, locale);
      await page.goto('/pair');
      await waitForApp(page);
    },
    async walk(page, record) {
      await expect(page.getByTestId('pairing-view')).toBeVisible();
      await record('PairingView');
    },
  },
  {
    title: 'revoked',
    views: ['RevokedView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'paired-devices', locale);
    },
    async walk(page, record) {
      await page.goto('/settings');
      await expect(page.locator('.settings-view')).toBeVisible();

      const revoke = page.getByTestId('revoke-btn-dev-1');
      await expect(revoke).toBeVisible();
      await revoke.click();
      await expect(page.getByTestId('revoke-modal')).toBeVisible();
      await page.getByTestId('revoke-confirm-button').click();
      await expect(page.getByTestId('revoked-view')).toBeVisible();
      await record('RevokedView');
    },
  },
  {
    title: 'listener off',
    views: ['ListenerOffView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'paired-devices', locale);
    },
    async walk(page, record) {
      await expect(async () => {
        await page.evaluate(async () => {
          const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
          runtimeStore.update((state) => ({
            ...state,
            isPaired: true,
            isRevoked: false,
            isListenerClosed: true,
            currentRoute: 'listener-off',
          }));
        });
        await expect(page.getByTestId('listener-off-view')).toBeVisible({ timeout: 1000 });
      }).toPass({ timeout: 15000 });
      await record('ListenerOffView');
    },
  },
  {
    title: 'epic decomposition',
    views: ['EpicDecompositionView'],
    async open(page, request, locale) {
      await pairInLocale(page, request, 'decomposition', locale);
      await gotoInLocale(
        page,
        locale,
        '/epics/relay-hardening/decomposition?project=pm-mcp-clients',
      );
    },
    async walk(page, record) {
      await expect(page.getByTestId('epic-decomposition')).toBeVisible();
      await expect(page.getByTestId('decomposition-title')).toBeVisible({ timeout: 15000 });
      await record('EpicDecompositionView');
    },
  },
];

/**
 * The elements exempt from the contrast audit.
 *
 * Every entry names the selector it exempts and the reason the pair behind it cannot meet the
 * WCAG threshold. The list is empty: the audit judges every text and state pair the views render.
 * An entry without a reason fails the guard test below, so a future exemption cannot stay silent.
 */
export const CONTRAST_ALLOWLIST = [];

/**
 * @param failure one contrast failure of the audit
 * @returns whether an allowlist entry that states its reason exempts the failure
 */
function isExempt(failure) {
  return CONTRAST_ALLOWLIST.some(
    (entry) =>
      typeof entry.selector === 'string' &&
      entry.selector.trim().length > 0 &&
      typeof entry.reason === 'string' &&
      entry.reason.trim().length > 0 &&
      failure.selector.includes(entry.selector),
  );
}

async function auditView(page, testInfo, { view, theme, viewport }, options = {}) {
  auditedViews.add(view);
  try {
    let recorded = [];
    if (fs.existsSync(AUDIT_CACHE_FILE)) {
      recorded = JSON.parse(fs.readFileSync(AUDIT_CACHE_FILE, 'utf8'));
    }
    if (!recorded.includes(view)) {
      recorded.push(view);
      fs.writeFileSync(AUDIT_CACHE_FILE, JSON.stringify(recorded));
    }
  } catch {
    // Non-fatal cache write
  }
  const result = await auditViewContrast(page, testInfo, { view, theme, viewport }, options);
  const failures = result.failures.filter((failure) => !isExempt(failure));
  expect(failures).toEqual([]);
  return result;
}

test.describe('Theme Readability and Contrast Audit Matrix', () => {
  test.afterAll(async ({ request }) => {
    try {
      await switchScenario(request, 'web-pairing');
    } catch {
      // Best-effort cleanup
    }
  });

  const THEMES = ['light', 'dark'];
  const VIEWPORTS = [
    { name: 'desktop', width: 1280, height: 800 },
    { name: 'mobile', width: 375, height: 667 },
    { name: 'narrow', width: 320, height: 568 },
  ];

  test('every contrast allowlist entry carries its selector and its reason', () => {
    const incomplete = CONTRAST_ALLOWLIST.filter(
      (entry) =>
        typeof entry.selector !== 'string' ||
        entry.selector.trim().length === 0 ||
        typeof entry.reason !== 'string' ||
        entry.reason.trim().length === 0,
    );

    expect(incomplete).toEqual([]);
  });

  for (const theme of THEMES) {
    test.describe(`Reported Flow: Findings Review ("Prüfen") in ${theme} mode`, () => {
      for (const vp of VIEWPORTS) {
        test(`exact flow on ${vp.name} (${vp.width}x${vp.height})`, async ({
          page,
          request,
        }, testInfo) => {
          await page.setViewportSize({ width: vp.width, height: vp.height });
          await switchScenario(request, 'running-work');
          await pair(page, request);
          await setTheme(page, theme);

          // 1. Overview page
          await page.goto('/overview');
          await expect(page.locator('.overview')).toBeVisible();
          await expect(page.locator('[data-testid="plan-card"]').first()).toBeVisible();

          // 2. Switch to by epic
          const byEpicBtn = page.getByRole('radio', { name: /by epic/i });
          await byEpicBtn.click();
          const epicCard = page.getByTestId('epic-card').first();
          await expect(epicCard).toBeVisible();

          // 3. Open epic
          await epicCard.click();
          await expect(page.getByTestId('epic-view')).toBeVisible();

          // 4. Assert findings review button ("Review" / "Prüfen")
          const reviewBtn = page.getByTestId('open-findings-review');
          await expect(reviewBtn).toBeVisible();

          // Audit epic page contrast
          const { failures: epicFailures, all: epicElements } = await auditViewContrast(
            page,
            testInfo,
            { view: 'EpicView', theme, viewport: vp },
          );
          expect(epicFailures).toEqual([]);

          // Specifically check review button contrast >= 4.5:1
          const btnAudit = epicElements.find((r) => r.selector.includes('open-findings-review'));
          expect(btnAudit).toBeDefined();
          expect(btnAudit?.passed).toBe(true);
          expect(btnAudit?.ratio).toBeGreaterThanOrEqual(4.5);

          // Capture screenshot of epic view
          await page.screenshot({
            path: `test-results/theme-epic-${theme}-${vp.name}.png`,
            fullPage: false,
          });

          // 5. Open Findings Review Modal
          await reviewBtn.click();
          const modal = page.getByTestId('review-modal');
          await expect(modal).toBeVisible();

          // Audit modal contrast
          const { failures: modalFailures } = await auditViewContrast(page, testInfo, {
            view: 'FindingsReviewModal',
            theme,
            viewport: vp,
          });
          expect(modalFailures).toEqual([]);

          // Capture screenshot of review modal
          await page.screenshot({
            path: `test-results/theme-modal-${theme}-${vp.name}.png`,
            fullPage: false,
          });

          // Close modal
          await page.getByTestId('review-close').click();
          await expect(modal).not.toBeVisible();
        });
      }
    });

    for (const vp of VIEWPORTS) {
      test.describe(`Core Views Contrast Audit Matrix (${theme} / ${vp.name})`, () => {
        test.beforeEach(async ({ page }) => {
          await page.setViewportSize({ width: vp.width, height: vp.height });
        });

        test(`Overview (by plan & by epic) in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'running-work');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/overview');
          await expect(page.locator('.overview')).toBeVisible();

          // Audit by-plan
          await page.getByRole('radio', { name: /by plan/i }).click();
          await auditView(page, testInfo, { view: 'OverviewView', theme, viewport: vp });

          // Audit by-epic
          const byEpicRadio = page.getByRole('radio', { name: /by epic/i });
          await byEpicRadio.click({ force: true });
          await auditView(page, testInfo, { view: 'OverviewView', theme, viewport: vp });
        });

        test(`PlanView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'plan-view');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/overview');
          await expect(page.locator('.overview')).toBeVisible();

          const planCard = page.locator('[data-testid="plan-card"]').first();
          await expect(planCard).toBeVisible();
          await planCard.click();

          await expect(page.getByTestId('plan-view')).toBeVisible();
          await auditView(page, testInfo, { view: 'PlanView', theme, viewport: vp });
        });

        test(`EpicView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'running-work');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/overview');
          await expect(page.locator('.overview')).toBeVisible();

          const byEpicRadio = page.getByRole('radio', { name: /by epic/i });
          await byEpicRadio.click({ force: true });
          const epicCard = page.getByTestId('epic-card').first();
          await expect(epicCard).toBeVisible();
          await epicCard.click();

          await expect(page.getByTestId('epic-view')).toBeVisible();
          await auditView(page, testInfo, { view: 'EpicView', theme, viewport: vp });
        });

        test(`ReportsView and CompletionReport in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'reports');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/reports');
          await expect(page.locator('.reports-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'ReportsView', theme, viewport: vp });

          // Open and audit CompletionReportView without silent skips
          const planCard = page.locator('.report-card', { hasText: 'fix-relay' });
          await expect(planCard).toBeVisible();
          const detailBtn = planCard.locator('.action-btn--detail');
          await expect(detailBtn).toBeVisible();
          await detailBtn.click();
          await expect(page.locator('.completion-report')).toBeVisible();

          await auditView(page, testInfo, { view: 'CompletionReportView', theme, viewport: vp });
        });

        test(`DecisionsView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'decisions');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/decisions');
          await expect(page.locator('.decisions-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'DecisionView', theme, viewport: vp });
        });

        test(`SystemView and CleanupView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'system');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/system');
          await expect(page.getByTestId('system-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'SystemView', theme, viewport: vp });

          // Navigate to CleanupView and audit it without silent skips
          const reviewBtn = page.getByRole('button', { name: /Review…/i });
          await expect(reviewBtn).toBeVisible();
          await reviewBtn.click();
          await expect(page.getByTestId('cleanup-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'CleanupView', theme, viewport: vp });
        });

        test(`SettingsView and MachineSettingsView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'designer');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/settings');
          await expect(page.getByTestId('settings-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'SettingsView', theme, viewport: vp });

          await page.getByTestId('machine-settings-button').click();
          await expect(page.getByTestId('machine-settings-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'MachineSettingsView', theme, viewport: vp });
        });

        test(`ProcessDesigner in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'designer');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/settings');
          await page.getByTestId('machine-settings-button').click();
          await expect(page.getByTestId('machine-settings-view')).toBeVisible();
          await page.getByTestId('open-designer-btn').click();
          await expect(page.getByTestId('process-designer')).toBeVisible();

          await auditView(page, testInfo, { view: 'ProcessDesigner', theme, viewport: vp });
        });

        test(`ProjectExplorerView and ProjectOnboardingView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'projects');
          await pair(page, request);

          await page.goto('/projects');
          await setTheme(page, theme);
          await expect(page.getByTestId('project-explorer-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'ProjectExplorerView', theme, viewport: vp });

          const addBtn = page.getByTestId('add-project-btn');
          await expect(addBtn).toBeVisible();
          await addBtn.click();
          await expect(page).toHaveURL(/\/projects\/onboard$/);
          await expect(page.getByTestId('project-onboarding-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'ProjectOnboardingView', theme, viewport: vp });
        });

        test(`StartView and RecipeReadyView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'start');
          await pair(page, request);
          await setTheme(page, theme);

          await openProject(page, 'pm-mcp-clients');
          await page.getByRole('button', { name: 'Start the plan' }).click();
          await expect(page.getByTestId('start-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'StartView', theme, viewport: vp });

          await page.getByTestId('choose-recipe').click();
          await page.getByTestId('recipe-choose-code-review').click();
          await page.getByTestId('recipe-continue').click();
          await expect(page.getByTestId('recipe-ready')).toBeVisible();

          await auditView(page, testInfo, { view: 'RecipeReadyView', theme, viewport: vp });
        });

        test(`DecompositionReviewView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'start');
          await pair(page, request);
          await setTheme(page, theme);

          await openProject(page, 'pm-mcp-clients');
          await openStartSources(page, 'epic');
          await addDescription(page, 'Relay hardening');
          await continueAndStart(page);

          const decomposeBtn = page.getByTestId('started-decompose');
          await expect(decomposeBtn).toBeVisible();
          await decomposeBtn.click();
          await expect(page.getByTestId('decomposition-review')).toBeVisible();

          await auditView(page, testInfo, { view: 'DecompositionReviewView', theme, viewport: vp });
        });

        test(`EpicAdoptView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'start-epic-adopt');
          await pair(page, request);

          await page.goto('/entries/ent-7a2e4f01');
          await setTheme(page, theme);
          await expect(page.getByTestId('start-view')).toBeVisible();
          await expect(page.getByTestId('epic-adopt')).toBeVisible();

          await auditView(page, testInfo, { view: 'EpicAdoptView', theme, viewport: vp });
        });

        test(`OnboardingView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'onboarding');
          await page.goto('/');
          await page.evaluate(() => localStorage.clear());
          await page.goto('/pair#code=KXQB-MRTN');
          await setTheme(page, theme);

          await expect(page).toHaveURL(/\/onboarding$/);
          await expect(page.getByTestId('onboarding-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'OnboardingView', theme, viewport: vp });
        });

        test(`PairingView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'web-pairing');
          await page.goto('/');
          await page.evaluate(() => localStorage.clear());
          await page.goto('/pair');
          await setTheme(page, theme);

          await expect(page.getByTestId('pairing-view')).toBeVisible();

          await auditView(page, testInfo, { view: 'PairingView', theme, viewport: vp });
        });

        test(`RevokedView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'paired-devices');
          await pair(page, request);
          await setTheme(page, theme);

          await page.goto('/settings');
          await expect(page.locator('.settings-view')).toBeVisible();

          const revokeBtn = page.getByTestId('revoke-btn-dev-1');
          await expect(revokeBtn).toBeVisible();
          await revokeBtn.click();

          const modal = page.getByTestId('revoke-modal');
          await expect(modal).toBeVisible();
          const confirmBtn = page.getByTestId('revoke-confirm-button');
          await confirmBtn.click();

          await expect(page.getByTestId('revoked-view')).toBeVisible();
          await auditView(page, testInfo, { view: 'RevokedView', theme, viewport: vp });
        });

        test(`ListenerOffView in ${theme} (${vp.name})`, async ({ page, request }, testInfo) => {
          await switchScenario(request, 'paired-devices');
          await pair(page, request);

          await page.evaluate(async () => {
            const { runtimeStore } = await import('/src/lib/stores/runtime.ts');
            runtimeStore.update((s) => ({
              ...s,
              isPaired: true,
              isRevoked: false,
              isListenerClosed: true,
              currentRoute: 'listener-off',
            }));
          });

          await setTheme(page, theme);
          await expect(page.getByTestId('listener-off-view')).toBeVisible();
          await auditView(page, testInfo, { view: 'ListenerOffView', theme, viewport: vp });
        });

        test(`EpicDecompositionView in ${theme} (${vp.name})`, async ({
          page,
          request,
        }, testInfo) => {
          await switchScenario(request, 'decomposition');
          await pair(page, request);

          await page.goto('/epics/relay-hardening/decomposition?project=pm-mcp-clients');
          await setTheme(page, theme);
          await expect(page.getByTestId('epic-decomposition')).toBeVisible();

          await auditView(page, testInfo, { view: 'EpicDecompositionView', theme, viewport: vp });
        });
      });
    }
  }

  test.describe('Text Guard, report mode', () => {
    test('every declared view is reached by exactly one journey', () => {
      const reached = GUARD_JOURNEYS.flatMap((journey) => journey.views);
      expect(reached.slice().sort()).toEqual(DECLARED_VIEWS.slice().sort());
    });

    test('the baseline holds every declared view and no retired one', () => {
      if (process.env.TEXT_GUARD_BOOTSTRAP === '1') {
        return;
      }
      const baseline = readGuardBaseline();
      expect(Object.keys(baseline.views).sort()).toEqual(DECLARED_VIEWS.slice().sort());
    });

    test('the guard covers every locale the application declares', () => {
      const source = fs.readFileSync(
        path.resolve(__dirname, '../../pm-web-app/src/lib/i18n/index.ts'),
        'utf8',
      );
      const declaration = source.match(/export type Locale\s*=\s*([^;]+);/);
      expect(
        declaration,
        'the application declares its interface locales as `export type Locale = ...`',
      ).not.toBeNull();
      const declared = [...declaration[1].matchAll(/'([^']+)'/g)].map((match) => match[1]);
      expect(GUARD_LOCALES.slice().sort()).toEqual(declared.sort());
    });

    for (const locale of GUARD_LOCALES) {
      test.describe(`collected interface text in ${locale}`, () => {
        for (const journey of GUARD_JOURNEYS) {
          test(`${journey.title} (${locale})`, async ({ page, request }) => {
            await page.setViewportSize({ width: 1440, height: 900 });
            await journey.open(page, request, locale);
            const collected = {};
            await journey.walk(page, async (view) => {
              const violations = normalizeViolations(
                await collectTextGuardViolations(page, { locale }),
              );
              collected[view] = violations;
              const listed =
                violations.map((violation) => formatViolation(violation)).join(', ') || 'none';
              console.log(`[text-guard] ${view} (${locale}): ${violations.length} - ${listed}`);
            });
            expect(Object.keys(collected).sort()).toEqual(journey.views.slice().sort());
            verifyGuardViews(collected, locale);
          });
        }
      });
    }
  });

  test.describe('Accessibility audit (axe-core)', () => {
    /**
     * Every declared view is opened by the journey that already reaches it and audited where it
     * stands, so the audit runs on the same rendered page the text guard reads.
     */
    for (const journey of GUARD_JOURNEYS) {
      test(`${journey.title}`, async ({ page, request }) => {
        await page.setViewportSize({ width: 1440, height: 900 });
        await journey.open(page, request, 'de');
        const audited = {};
        await journey.walk(page, async (view) => {
          const { violations, failing } = await auditViewA11y(page);
          for (const violation of violations) {
            if (!failing.includes(violation)) {
              console.log(`[a11y] ${formatA11yViolation(view, violation)}`);
            }
          }
          audited[view] = failing;
        });

        expect(Object.keys(audited).sort()).toEqual(journey.views.slice().sort());
        const failures = Object.entries(audited).flatMap(([view, violations]) =>
          violations.map((violation) => formatA11yViolation(view, violation)),
        );
        expect(failures, `serious accessibility issues found:\n${failures.join('\n')}`).toEqual([]);
      });
    }
  });

  test.describe('Declared View Coverage Guard', () => {
    test('all 22 declared views in src/lib/views/ are present and were audited', () => {
      const viewsDir = path.resolve(__dirname, '../../pm-web-app/src/lib/views');
      const viewFiles = fs
        .readdirSync(viewsDir)
        .filter((f) => f.endsWith('.svelte') && f !== 'AppRouter.svelte')
        .map((f) => f.replace('.svelte', ''));

      // 1. Every Svelte view in src/lib/views/ must be declared in DECLARED_VIEWS
      expect(DECLARED_VIEWS.slice().sort()).toEqual(viewFiles.sort());

      // 2. Every declared view must have been audited across the matrix execution
      let recorded = Array.from(auditedViews);
      try {
        if (fs.existsSync(AUDIT_CACHE_FILE)) {
          const fromFile = JSON.parse(fs.readFileSync(AUDIT_CACHE_FILE, 'utf8'));
          recorded = Array.from(new Set([...recorded, ...fromFile]));
        }
      } catch {
        // Fallback to in-memory set
      }
      const missingAudits = DECLARED_VIEWS.filter((v) => !recorded.includes(v));
      expect(missingAudits).toEqual([]);
    });
  });
});

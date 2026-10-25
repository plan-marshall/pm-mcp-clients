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
  expect(result.failures).toEqual([]);
  return result;
}

test.describe('Theme Readability and Contrast Audit Matrix', () => {
  test.afterAll(async ({ request }) => {
    try {
      await request.post('http://127.0.0.1:7420/mock/scenario', {
        data: { scenario: 'web-pairing' },
      });
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
          await page
            .locator('.app-header')
            .getByRole('button', { name: /Running work/i })
            .click();
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

          await page
            .locator('.app-header')
            .getByRole('button', { name: /Running work/i })
            .click();
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

          await page
            .locator('.app-header')
            .getByRole('button', { name: /Running work/i })
            .click();
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

          await page
            .locator('.app-header')
            .getByRole('button', { name: /Running work/i })
            .click();
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

          const reportsBtn = page.locator('.app-header').getByRole('button', { name: /Reports/i });
          await reportsBtn.click();
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

          await page.getByRole('button', { name: /Decisions/i }).click();
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

          const systemNavBtn = page.getByRole('button', { name: /System/i });
          await systemNavBtn.click();
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

          await page.getByRole('button', { name: 'Machine settings' }).click();
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

          await page.getByRole('button', { name: 'Machine settings' }).click();
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

          const settingsBtn = page.getByRole('button', { name: /Settings/i });
          await settingsBtn.click();
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

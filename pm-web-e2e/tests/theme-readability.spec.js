import { test, expect } from '@playwright/test';
import { setTheme, auditPageContrast } from './helpers/contrast.js';
import { pair, switchScenario } from './helpers/pairing.js';

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
  ];

  for (const theme of THEMES) {
    test.describe(`Reported Flow: Findings Review ("Prüfen") in ${theme} mode`, () => {
      for (const vp of VIEWPORTS) {
        test(`exact flow on ${vp.name} (${vp.width}x${vp.height})`, async ({ page, request }) => {
          await page.setViewportSize({ width: vp.width, height: vp.height });
          await switchScenario(request, 'running-work');
          await pair(page, request);
          await setTheme(page, theme);

          // 1. Overview page
          await page.locator('.app-header').getByRole('button', { name: /Running work/i }).click();
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
          const { failures: epicFailures, all: epicElements } = await auditPageContrast(page);
          if (epicFailures.length > 0) {
            console.error(`Epic view contrast failures (${theme}, ${vp.name}):`, epicFailures);
          }
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
          const { failures: modalFailures } = await auditPageContrast(page);
          if (modalFailures.length > 0) {
            console.error(`Modal contrast failures (${theme}, ${vp.name}):`, modalFailures);
          }
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

    test.describe(`Core Views Contrast Audit Matrix in ${theme} mode`, () => {
      test(`Overview (by plan & by epic) in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'running-work');
        await pair(page, request);
        await setTheme(page, theme);

        await page.locator('.app-header').getByRole('button', { name: /Running work/i }).click();
        await expect(page.locator('.overview')).toBeVisible();

        // Audit by-plan
        await page.getByRole('radio', { name: /by plan/i }).click();
        const { failures: planFailures } = await auditPageContrast(page);
        if (planFailures.length > 0) {
          console.error(`Overview (by plan) failures (${theme}):`, planFailures);
        }
        expect(planFailures).toEqual([]);

        // Audit by-epic
        const byEpicRadio = page.getByRole('radio', { name: /by epic/i });
        await byEpicRadio.click({ force: true });
        const { failures: epicFailures } = await auditPageContrast(page);
        if (epicFailures.length > 0) {
          console.error(`Overview (by epic) failures (${theme}):`, epicFailures);
        }
        expect(epicFailures).toEqual([]);
      });

      test(`PlanView in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'plan-view');
        await pair(page, request);
        await setTheme(page, theme);

        await page.locator('.app-header').getByRole('button', { name: /Running work/i }).click();
        await expect(page.locator('.overview')).toBeVisible();

        const planCard = page.locator('[data-testid="plan-card"]').first();
        await expect(planCard).toBeVisible();
        await planCard.click();

        await expect(page.getByTestId('plan-view')).toBeVisible();
        const { failures } = await auditPageContrast(page);
        if (failures.length > 0) {
          console.error(`PlanView failures (${theme}):`, failures);
        }
        expect(failures).toEqual([]);
      });

      test(`ReportsView and CompletionReport in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'reports');
        await pair(page, request);
        await setTheme(page, theme);

        const reportsBtn = page.locator('.app-header').getByRole('button', { name: /Reports/i });
        await reportsBtn.click();
        await expect(page.locator('.reports-view')).toBeVisible();

        const { failures: reportsFailures } = await auditPageContrast(page);
        if (reportsFailures.length > 0) {
          console.error(`ReportsView failures (${theme}):`, reportsFailures);
        }
        expect(reportsFailures).toEqual([]);
      });

      test(`DecisionsView in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'decisions');
        await pair(page, request);
        await setTheme(page, theme);

        await page.getByRole('button', { name: /Decisions/i }).click();
        await expect(page.locator('.decisions-view')).toBeVisible();

        const { failures } = await auditPageContrast(page);
        if (failures.length > 0) {
          console.error(`DecisionsView failures (${theme}):`, failures);
        }
        expect(failures).toEqual([]);
      });

      test(`SystemView and CleanupView in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'system');
        await pair(page, request);
        await setTheme(page, theme);

        const systemNavBtn = page.getByRole('button', { name: /System/i });
        await systemNavBtn.click();
        await expect(page.getByTestId('system-view')).toBeVisible();

        const { failures: sysFailures } = await auditPageContrast(page);
        if (sysFailures.length > 0) {
          console.error(`SystemView failures (${theme}):`, sysFailures);
        }
        expect(sysFailures).toEqual([]);
      });

      test(`SettingsView and MachineSettingsView in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'designer');
        await pair(page, request);
        await setTheme(page, theme);

        await page.getByRole('button', { name: 'Machine settings' }).click();
        await page.getByTestId('machine-settings-button').click();
        await expect(page.getByTestId('machine-settings-view')).toBeVisible();

        const { failures: machineFailures } = await auditPageContrast(page);
        if (machineFailures.length > 0) {
          console.error(`MachineSettingsView failures (${theme}):`, machineFailures);
        }
        expect(machineFailures).toEqual([]);
      });

      test(`ProcessDesigner in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'designer');
        await pair(page, request);
        await setTheme(page, theme);

        await page.getByRole('button', { name: 'Machine settings' }).click();
        await page.getByTestId('machine-settings-button').click();
        await expect(page.getByTestId('machine-settings-view')).toBeVisible();
        await page.getByTestId('open-designer-btn').click();
        await expect(page.getByTestId('process-designer')).toBeVisible();

        const { failures: designerFailures } = await auditPageContrast(page);
        if (designerFailures.length > 0) {
          console.error(`ProcessDesigner failures (${theme}):`, designerFailures);
        }
        expect(designerFailures).toEqual([]);
      });

      test(`ProjectExplorerView in ${theme}`, async ({ page, request }) => {
        await switchScenario(request, 'running-work');
        await pair(page, request);
        await setTheme(page, theme);

        const projBtn = page.getByTestId('nav-projects-btn');
        if (await projBtn.isVisible()) {
          await projBtn.click();
          await expect(page.getByTestId('project-explorer-view')).toBeVisible();

          const { failures } = await auditPageContrast(page);
          if (failures.length > 0) {
            console.error(`ProjectExplorerView failures (${theme}):`, failures);
          }
          expect(failures).toEqual([]);
        }
      });
    });
  }
});

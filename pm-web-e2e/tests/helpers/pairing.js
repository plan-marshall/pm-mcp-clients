import { expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './ports.js';

/** The mock runtime the suite starts in its global setup. */
const MOCK_RUNTIME = MOCK_RUNTIME_URL;

/**
 * Switches the mock runtime to a scenario and waits for the answer.
 *
 * @param request the Playwright request fixture
 * @param scenario the scenario name or legacy case name
 */
export async function switchScenario(request, scenario) {
  const response = await request.post(`${MOCK_RUNTIME}/mock/scenario`, { data: { scenario } });
  expect(response.ok()).toBeTruthy();
}

/**
 * The pairing code the active scenario accepts right now.
 *
 * A scenario switch resets the code; a consumed code is replaced through the mock's development
 * endpoint. Reading it here keeps every spec independent of the literal code a scenario carries.
 *
 * @param request the Playwright request fixture
 * @returns the open pairing code
 */
export async function openPairingCode(request) {
  const state = await request.get(`${MOCK_RUNTIME}/mock/state`);
  expect(state.ok()).toBeTruthy();
  const { pairing_code: openCode } = await state.json();
  if (openCode) {
    return openCode;
  }
  const issued = await request.post(`${MOCK_RUNTIME}/mock/pairing`);
  expect(issued.ok()).toBeTruthy();
  const { code } = await issued.json();
  expect(code).toBeTruthy();
  return code;
}

/**
 * Pairs a fresh browser with the active scenario's code and waits for the app shell.
 *
 * @param page the Playwright page
 * @param request the Playwright request fixture
 */
export async function pair(page, request) {
  const code = await openPairingCode(request);
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.goto(`/#code=${code}`);
  await expect(page.locator('.app-header__machine')).toContainText('Machine:', {
    timeout: 20000,
  });
}

/**
 * Opens the operator dashboard and selects one project.
 *
 * @param page the Playwright page
 * @param projectId the project to select
 */
export async function openProject(page, projectId) {
  await page
    .locator('.app-header')
    .getByRole('button', { name: /Running work/i })
    .click();
  await expect(page.locator('.overview')).toBeVisible();
  await page
    .locator('.project-list')
    .getByRole('tab', { name: new RegExp(projectId, 'i') })
    .click();
  await expect(page.locator('.project-title')).toContainText(projectId);
}

/**
 * Starts a plan or an epic from the overview and opens its sources.
 *
 * A plan first chooses between sources and a recipe; an epic opens at sources, or at its
 * adoptable ledgers, where "Start from scratch" goes on to the sources.
 *
 * @param page the Playwright page
 * @param kind `plan` or `epic`
 */
export async function openStartSources(page, kind = 'plan') {
  const label = kind === 'epic' ? 'Start the epic' : 'Start the plan';
  await page.getByRole('button', { name: label }).click();
  await expect(page.getByTestId('start-view')).toBeVisible({ timeout: 15000 });
  // The view renders its loading status first; the entry's own panel follows.
  await expect(page.getByTestId('start-loading')).toHaveCount(0, { timeout: 15000 });

  if (await page.getByTestId('start-choose').isVisible()) {
    await page.getByTestId('choose-sources').click();
  } else if (await page.getByTestId('epic-adopt').isVisible()) {
    await page.getByTestId('adopt-new').click();
  }

  await expect(page.getByTestId('source-list')).toBeVisible({ timeout: 15000 });
}

/**
 * Fills the description source and stores it.
 *
 * @param page the Playwright page
 * @param text the description
 */
export async function addDescription(page, text) {
  await page.getByTestId('add-description').click();
  await page.getByTestId('description-input').fill(text);
  await page.getByTestId('description-add').click();
  await expect(page.getByTestId('source-list')).toContainText(text);
}

/**
 * Continues the open entry to the ready step and starts it.
 *
 * @param page the Playwright page
 */
export async function continueAndStart(page) {
  await page.getByTestId('source-continue').click();
  await expect(page.getByTestId('recipe-ready')).toBeVisible({ timeout: 15000 });
  await page.getByTestId('ready-start').click();
  await expect(page.getByTestId('start-started')).toBeVisible({ timeout: 15000 });
}

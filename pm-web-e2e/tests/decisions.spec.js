import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { pair, switchScenario } from './helpers/pairing.js';

const BASE = MOCK_RUNTIME_URL;

/**
 * The decisions lens as a triage list (`decisions.adoc#order`, `navigation.adoc#keys`): the list of
 * open decisions beside the decision itself, `Enter` deciding, the answer lines of the own and the
 * elsewhere answer, and one notification for a burst (`decisions.adoc#announce`).
 *
 * The list is what every test starts from; a test that answers a decision opens it first, as a
 * click on its row does.
 */
test.describe('Decisions as a triage list (UC-PLN-2)', () => {
  test.beforeEach(async ({ page, request }) => {
    await switchScenario(request, 'decisions');
    await pair(page, request);
  });

  /**
   * Opens the decisions lens on its list, at a desktop window the list and the detail both fit in.
   *
   * @param page the Playwright page
   */
  async function openDecisions(page) {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/decisions');
    await expect(page.getByTestId('triage-page')).toBeVisible();
    await expect(page.getByTestId('triage-list')).toBeVisible();
    await expect(page.locator('.decisions-row')).toHaveCount(8);
  }

  /**
   * Opens one decision in the object column, as a click on its row does.
   *
   * @param page the Playwright page
   * @param id the decision to open
   * @returns the detail's locator
   */
  async function openDecision(page, id) {
    await page.locator(`.decisions-row[data-decision-id="${id}"] a.row-link__title`).click();
    const detail = page.getByTestId('decision-detail');
    await expect(detail).toHaveAttribute('data-decision-id', id);
    return detail;
  }

  /**
   * @param page the Playwright page
   * @returns the decision ids of the rows, in the order the list draws them
   */
  async function rowIds(page) {
    return page
      .locator('.decisions-row')
      .evaluateAll((rows) => rows.map((row) => row.getAttribute('data-decision-id')));
  }

  /**
   * @param page the Playwright page
   * @param index the group's position in the list
   * @returns the decision ids of that group's rows, in the order the group draws them
   */
  async function groupRowIds(page, index) {
    return page
      .getByTestId('decisions-group')
      .nth(index)
      .locator('.decisions-row')
      .evaluateAll((rows) => rows.map((row) => row.getAttribute('data-decision-id')));
  }

  test('lists the eight open decisions, blocking first and grouped by project', async ({ page }) => {
    await openDecisions(page);

    await expect(page.locator('.decisions-header').getByTestId('count-badge')).toContainText('8');

    const ids = await rowIds(page);
    expect(ids).toHaveLength(8);
    // The oldest blocking decision stands first; within its project group the blocking decisions
    // come before the one that blocks nothing, and oldest first among themselves.
    expect(ids[0]).toBe('WR-2');
    expect(await groupRowIds(page, 0)).toEqual([
      'WR-2',
      'Q-007',
      'Q-009',
      'Q-012',
      'Q-031',
      'Q-015',
      'Q-016',
    ]);

    const groups = page.getByTestId('decisions-group');
    await expect(groups).toHaveCount(2);
    await expect(groups.nth(0)).toContainText('pm-mcp-clients');
    await expect(groups.nth(1)).toContainText('pm-mcp-core');
    await expect(groups.nth(1)).toContainText('session-log asks for a waiver');

    // The view names itself in the tab, with the count the tab title carries (needs-and-find.adoc).
    await expect(page).toHaveTitle(/Decisions$/);

    // A row carries the subject, the phase and the one line under it.
    const row = page.locator('.decisions-row[data-decision-id="Q-007"]');
    await expect(row).toContainText('Resolve rebase conflict');
    await expect(row).toContainText('plan fix-relay');
    await expect(row).toContainText('blocks the plan');
    await expect(row).toContainText('can be undone');
  });

  test('opens the decision beside the list at its own address', async ({ page }) => {
    await openDecisions(page);

    const detail = await openDecision(page, 'Q-007');

    await expect(detail.getByRole('heading', { name: 'Resolve rebase conflict' })).toBeVisible();
    await expect(page).toHaveURL(/\/decisions\/Q-007$/);
    await expect(page.getByTestId('triage-list')).toBeVisible();
    await expect(page.getByTestId('triage-detail')).toBeVisible();
    // The one selected row is marked, and the row stays the one line it was: the description is
    // the detail's to say, not the row's.
    await expect(page.locator('.decisions-row[data-decision-id="Q-007"]')).toContainText(
      'Resolve rebase conflict',
    );
    await expect(page.locator('.decisions-row[data-decision-id="Q-007"]')).not.toContainText(
      'conflicts with main in RelayServer.java',
    );
  });

  test('Enter decides a decision with a chosen option, names the answer and selects the next', async ({
    page,
  }) => {
    await openDecisions(page);
    const detail = await openDecision(page, 'Q-007');

    await detail.getByText('Rebase onto main', { exact: true }).click();
    // `Enter` belongs to the decision's own surface, not to the option it was chosen on.
    await page.locator('.decision-title').focus();
    await page.keyboard.press('Enter');

    // The success line is the form's own status: this client's answer, named by its option.
    await expect(page.getByTestId('decision-outcome')).toContainText('Decided: Rebase onto main');
    await expect(page.locator('.decisions-row')).toHaveCount(7);

    // The next decision stands in the object column by a replace, so Back does not return into the
    // decision that was just answered.
    await expect(page.getByTestId('decision-detail')).toHaveAttribute(
      'data-decision-id',
      'WR-3',
    );
    await expect(page).toHaveURL(/\/decisions\/WR-3$/);
  });

  test('a decision answered from the console says where it was answered', async ({ page, request }) => {
    await openDecisions(page);
    await openDecision(page, 'Q-009');

    const answered = await request.post(`${BASE}/mock/broadcast`, {
      data: {
        event: 'decision.answered',
        data: {
          id: 'Q-009',
          answer: '2',
          channel: 'cli',
          device: 'terminal-01',
        },
      },
    });
    expect(answered.ok()).toBeTruthy();

    await expect(page.getByTestId('decision-outcome')).toContainText(
      'answered in the console, terminal-01: Deprecate them, delete in 0.11',
    );
    await expect(page.locator('.decisions-row')).toHaveCount(7);
  });

  test('answers a decision in the operator’s own words with a read-back', async ({ page }) => {
    await openDecisions(page);
    const detail = await openDecision(page, 'Q-007');

    await detail.getByRole('button', { name: 'in my own words' }).click();
    await detail.getByTestId('own-words-Q-007-0').fill('Use a rebase to keep the history linear');
    await detail.getByRole('button', { name: 'Read back' }).click();

    await expect(detail).toContainText('I understood:');
    await detail.getByRole('button', { name: 'Correct' }).click();

    await expect(page.getByTestId('decision-outcome')).toContainText('Decided:');
    await expect(page.locator('.decisions-row')).toHaveCount(7);
  });

  test('keeps an irreversible recommendation one step away until asked', async ({ page }) => {
    await openDecisions(page);
    const detail = await openDecision(page, 'Q-009');

    await expect(detail.getByTestId('recommendation-hidden-Q-009')).toBeVisible();
    await expect(detail.getByTestId('recommendation-Q-009')).toHaveCount(0);

    await detail.getByRole('button', { name: 'Show the recommendation' }).click();

    await expect(detail.getByTestId('recommendation-Q-009')).toContainText(
      'Recommended by architecture',
    );
  });

  test('waits for part 1 before it offers part 2', async ({ page }) => {
    await openDecisions(page);
    const detail = await openDecision(page, 'Q-012');

    await expect(detail).toContainText('Answer part 1 first.');
    await expect(detail.getByText('At the first start of the new release')).toHaveCount(0);

    await detail.getByText('A file per plan', { exact: true }).click();

    await expect(detail).toContainText('depends on part 1: A file per plan');
    await expect(detail.getByText('At the first start of the new release')).toBeVisible();
  });

  test('waives a gate only with its reason', async ({ page }) => {
    await openDecisions(page);
    const detail = await openDecision(page, 'WR-2');

    const waive = detail.getByRole('button', { name: 'Waive the gate' });
    const decline = detail.getByRole('button', { name: 'Decline' });
    await expect(waive).toBeDisabled();
    await expect(decline).toBeDisabled();

    await detail.getByTestId('waiver-reason').fill('The CI has no second machine');
    await expect(waive).toBeEnabled();
    await expect(decline).toBeEnabled();

    await waive.click();
    await expect(page.locator('.decisions-row')).toHaveCount(7);
    await expect(page.locator('.decisions-row[data-decision-id="WR-2"]')).toHaveCount(0);
  });

  test('filters the list by an option phrase', async ({ page }) => {
    await openDecisions(page);

    await page.getByTestId('filter-input').fill('Rebase onto main');

    await expect(page.getByTestId('filter-results')).toContainText('1 match');
    await expect(page.locator('.decisions-row')).toHaveCount(1);
    await expect(page.locator('.decisions-row[data-decision-id="Q-007"]')).toBeVisible();
  });

  /**
   * Replaces the browser's Notification with a recorder that may show one, before the app loads.
   *
   * @param page the Playwright page
   */
  async function recordNotifications(page) {
    await page.addInitScript(() => {
      // The announcer notifies only for a tab nobody is looking at (`decisions.adoc#announce`),
      // so the checked page reports itself in the background.
      Object.defineProperty(document, 'visibilityState', { get: () => 'hidden' });
      Object.defineProperty(document, 'hidden', { get: () => true });
      window.__notifications = [];
      window.__notificationClicks = [];
      class FakeNotification {
        static permission = 'granted';
        static requestPermission = async () => 'granted';
        constructor(title) {
          window.__notifications.push(title);
          window.__notificationClicks.push(() => this.onclick?.());
        }
      }
      Object.defineProperty(window, 'Notification', { value: FakeNotification });
    });
  }

  test('makes one notification for a burst of decisions, and it opens the list', async ({ page }) => {
    await recordNotifications(page);
    await page.reload();
    await openDecisions(page);
    await openDecision(page, 'Q-007');

    // Two decisions arrive inside one second: the posts go together, so they share the window.
    const opened = await Promise.all(
      ['Q-098', 'Q-099'].map((id) =>
        page.request.post(`${BASE}/mock/broadcast`, {
          data: {
            event: 'decision.opened',
            data: {
              id,
              revision: 1,
              title: `A decision that arrived (${id})`,
              subject: 'plan fix-relay',
              scope: 'plan',
              asking_phase: 'execute',
              asking_role: 'implement',
              blocks: 'plan',
              reversibility: 'reversible',
              parts: [],
            },
          },
        }),
      ),
    );
    for (const response of opened) expect(response.ok()).toBeTruthy();

    await page.waitForFunction(() => window.__notifications.length === 1);
    expect(await page.evaluate(() => window.__notifications)).toEqual([
      '2 decisions wait for you',
    ]);

    // The several form opens the list of open decisions, never one of them.
    await page.evaluate(() => window.__notificationClicks[0]());
    await expect(page).toHaveURL(/\/decisions$/);
  });

  test('announces none for what a reload finds by reading everything again', async ({ page }) => {
    await recordNotifications(page);
    await page.reload();
    await openDecisions(page);

    // A decision the reload already read is not news, however it is named again.
    const opened = await page.request.post(`${BASE}/mock/broadcast`, {
      data: {
        event: 'decision.opened',
        data: { id: 'Q-007', revision: 1, subject: 'plan fix-relay' },
      },
    });
    expect(opened.ok()).toBeTruthy();

    // A single decision that really is new makes its own notification; the primed one adds none.
    const fresh = await page.request.post(`${BASE}/mock/broadcast`, {
      data: {
        event: 'decision.opened',
        data: {
          id: 'Q-097',
          revision: 1,
          title: 'A decision after the reload',
          subject: 'plan storage-v2',
          scope: 'plan',
          asking_phase: 'execute',
          asking_role: 'implement',
          blocks: 'plan',
          reversibility: 'reversible',
          parts: [],
        },
      },
    });
    expect(fresh.ok()).toBeTruthy();

    await page.waitForFunction(() =>
      window.__notifications.includes('storage-v2 asks a decision'),
    );
    expect(await page.evaluate(() => window.__notifications)).toEqual([
      'storage-v2 asks a decision',
    ]);
  });
});

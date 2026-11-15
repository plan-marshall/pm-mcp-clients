import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';

/**
 * The plan's intent and its outline on the development page (design
 * `intent-and-outline.adoc#intent`, `#outline`, `#builder`).
 *
 * The fixture there reads the mock runtime's `fix-relay` records through the real client, so this
 * walk covers the four read routes, the panels that draw them and the words of both languages. The
 * panels are shown on `/dev/components` until PLAN-55 places them in the plan's Intent tab.
 */

/** The fixture's own hook: it is drawn once per theme, so its panels appear twice. */
const FIXTURE = 'intent-fixture';

/**
 * @param {import('@playwright/test').Page} page the running app
 * @param {string} locale the interface locale the app boots in
 */
async function openFixtureIn(page, locale) {
  await page.evaluate((active) => localStorage.setItem('pm_locale', active), locale);
  await page.goto('/dev/components');
  await expect(page.getByTestId(FIXTURE)).toHaveCount(2);
}

test.describe('The plan intent and outline panels', () => {
  test.beforeEach(async ({ page, request }) => {
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'running-work' },
    });
    expect(res.ok()).toBeTruthy();

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/#code=KXQB-MRTN');
    await expect(page.locator('.app-header__machine')).toContainText('Machine: mock-machine');
  });

  test('draws the intent, the builder check and the outline in English', async ({ page }) => {
    await openFixtureIn(page, 'en');
    const light = page.getByTestId(FIXTURE).first();

    // The head: the plan, its revision, the goal and the counts by verdict.
    const intent = light.getByTestId('plan-intent-panel');
    await expect(intent.getByTestId('intent-head')).toHaveText('Intent · fix-relay · revision 3');
    await expect(intent.getByTestId('intent-goal')).toContainText(
      'Make the relay reconnect without losing queued events.',
    );
    await expect(intent.getByTestId('intent-counts')).toHaveText(
      '4 criteria · 2 met · 1 not met · 1 not verified',
    );

    // The criteria carry their verdict and the reason one expansion away.
    await expect(intent.getByTestId('criterion-verdict-AC-004')).toHaveText('not met');
    await intent.getByTestId('criterion-reason-AC-004').click();
    await expect(intent.getByTestId('criterion-reason-text-AC-004')).toContainText(
      'The compatibility test still fails for the legacy event shape.',
    );
    await expect(intent.getByTestId('criterion-verdict-AC-003')).toHaveText('not verified');

    // The non-goals by the record's own ids, and the choices by who made them.
    await expect(intent.getByTestId('non-goal-NG-2')).toContainText(
      'Support more than one relay per machine.',
    );
    await expect(intent.getByTestId('choice-group-role')).toContainText("the model's call");
    await expect(intent.getByTestId('choice-DC-004')).toContainText(
      'evidence: pm-relay/src/main/java/de/planmarshall/relay/EventQueue.java',
    );
    await expect(intent.getByTestId('choice-adr-DC-005')).toHaveText('contradicts ADR-0002');

    // The builder check waits, marks its recommendation and preselects nothing.
    const builder = light.getByTestId('builder-check');
    await expect(builder.getByTestId('builder-state')).toHaveText('questions wait');
    await expect(builder.getByTestId('builder-question-BQ-001')).toContainText('recommended');
    await expect(builder.locator('input')).toHaveCount(0);

    // The outline: the deliverables, the files with their intents, the dependencies.
    const outline = light.getByTestId('outline-panel');
    await expect(outline.getByTestId('outline-deliverable-1')).toContainText(
      '1 · Reconnect the relay',
    );
    await expect(outline.getByTestId('outline-file-1-0')).toContainText('replaced');
    await expect(outline.getByTestId('outline-depends-2')).toContainText(
      'depends on 1 · Reconnect the relay',
    );
    await expect(outline.getByTestId('outline-dependencies')).toHaveText('Added dependencies');
    await expect(outline.getByTestId('outline-dependency-0')).toHaveText(
      'de.cuioss:cui-http 2.1.0 · added by 1: The retry adapter carries the backoff policy.',
    );
  });

  test('draws both panels in the German catalogue', async ({ page }) => {
    await openFixtureIn(page, 'de');
    const light = page.getByTestId(FIXTURE).first();

    await expect(light.getByTestId('intent-head')).toHaveText('Absicht · fix-relay · Revision 3');
    await expect(light.getByTestId('intent-counts')).toHaveText(
      '4 Kriterien · 2 erfüllt · 1 nicht erfüllt · 1 nicht verifiziert',
    );
    await expect(light.getByTestId('non-goal-NG-1')).toContainText(
      'Change the on-disk queue format.',
    );
    await expect(light.getByTestId('builder-state')).toHaveText('Fragen warten');
    await expect(light.getByTestId('outline-dependency-0')).toContainText('hinzugefügt von 1');
  });

  test('draws the panels in both themes', async ({ page }) => {
    await openFixtureIn(page, 'en');

    for (const index of [0, 1]) {
      const fixture = page.getByTestId(FIXTURE).nth(index);
      await expect(fixture.getByTestId('plan-intent-panel')).toBeVisible();
      await expect(fixture.getByTestId('outline-panel')).toBeVisible();
      await expect(fixture.getByTestId('builder-check')).toBeVisible();
    }
  });
});

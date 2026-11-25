import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { auditViewA11y, formatViolation } from './helpers/a11y.js';

/**
 * The plan's intent and its outline on the development page (design
 * `intent-and-outline.adoc#intent`, `#outline`, `#builder`), and the epic's intent (design
 * `#epic-destination`, `#epic-rest`).
 *
 * The fixture there reads the mock runtime's `fix-relay` and `relay-hardening` records through the
 * real client, so this walk covers the read routes, the panels that draw them and the words of both
 * languages. The panels are shown on `/dev/components` until PLAN-55 places them in the plan's Intent
 * tab and PLAN-58/PLAN-59 in the epic's and the project's.
 */

/** The fixture's own hook: it is drawn once per theme, so its panels appear twice. */
const FIXTURE = 'intent-fixture';

/** The epic's intent panel inside the fixture, drawn once per theme. */
const EPIC_FIXTURE = 'epic-fixture';

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

  test('opens the Intent tab of a plan from its address', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/plans/fix-relay?tab=intent');

    // The address opens the tab, and the tab list reports it as selected.
    await expect(page.getByTestId('plan-panel-host')).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Intent' })).toHaveAttribute(
      'aria-selected',
      'true',
    );

    // The three panels stand in the order the design names: the intent, the builder check, the outline.
    await expect(page.getByTestId('plan-intent-panel')).toBeVisible();
    await expect(page.getByTestId('builder-check')).toBeVisible();
    await expect(page.getByTestId('outline-panel')).toBeVisible();
    const order = await page.evaluate(() =>
      Array.from(
        document.querySelectorAll(
          '[data-testid="plan-intent-panel"], [data-testid="builder-check"], [data-testid="outline-panel"]',
        ),
      ).map((element) => element.getAttribute('data-testid')),
    );
    expect(order).toEqual(['plan-intent-panel', 'builder-check', 'outline-panel']);
    await expect(page.getByTestId('intent-head')).toContainText('Intent · fix-relay');
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

  test("draws the epic's intent with its verdicts, evidence and plans", async ({ page }) => {
    await openFixtureIn(page, 'en');
    const epic = page.getByTestId(FIXTURE).first().getByTestId(EPIC_FIXTURE);

    await expect(epic.getByTestId('epic-destination')).toHaveText(
      'The relay survives a dropped connection without losing an event.',
    );

    // The two decided checks, the one the review refused, and the one still pending.
    await expect(epic.getByTestId('epic-check-verdict-0')).toHaveText('met');
    await expect(epic.getByTestId('epic-check-verdict-1')).toHaveText('met');
    await expect(epic.getByTestId('epic-check-verdict-2')).toHaveText('not met');
    await expect(epic.getByTestId('epic-check-verdict-3')).toHaveText('pending');

    // The evidence stays text, one entry per line, and the last check carries none.
    await expect(epic.getByTestId('epic-check-evidence-0-0')).toHaveText(
      'evidence: pm://plans/fix-relay/report',
    );
    await expect(epic.getByTestId('epic-check-evidence-0-1')).toHaveText(
      'evidence: pm-relay/src/test/java/de/planmarshall/relay/RelayConnectionTest.java',
    );
    await expect(epic.getByTestId('epic-check-0').locator('a')).toHaveCount(0);
    await expect(epic.getByTestId('epic-check-evidence-3-0')).toHaveCount(0);

    // The non-goals, the questions not yet specified, and each plan's own criterion.
    await expect(epic.getByTestId('epic-non-goal-1')).toHaveText(
      'Support more than one relay per machine.',
    );
    await expect(epic.getByTestId('epic-question-1')).toHaveText(
      'Should reconnects be rate-limited per client?',
    );
    await expect(epic.getByTestId('epic-plan-fix-relay')).toContainText('Fix Relay Transport');
    await expect(epic.getByTestId('epic-plan-fix-relay-done-when-1')).toHaveText(
      'The queue keeps every accepted event.',
    );
    await expect(epic.getByTestId('epic-plan-queue-format')).toContainText('Keep the Queue');
  });

  test('draws the epic panel in the German catalogue', async ({ page }) => {
    await openFixtureIn(page, 'de');
    const fixture = page.getByTestId(FIXTURE).first();
    const epic = fixture.getByTestId(EPIC_FIXTURE);

    await expect(epic.getByTestId('epic-destination')).toHaveText(
      'The relay survives a dropped connection without losing an event.',
    );
    await expect(epic.getByTestId('epic-check-verdict-3')).toHaveText('ausstehend');
    await expect(epic.getByTestId('epic-non-goals')).toHaveText('Nicht-Ziele');
  });

  test('draws the epic panel in both themes', async ({ page }) => {
    await openFixtureIn(page, 'en');

    for (const index of [0, 1]) {
      const fixture = page.getByTestId(FIXTURE).nth(index);
      await expect(fixture.getByTestId(EPIC_FIXTURE).getByTestId('epic-destination')).toBeVisible();
    }
  });

  test('passes the axe audit of both fixtures at phone width', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await openFixtureIn(page, 'en');

    // The audit reads the DOM as it stands, so both fixtures wait for their served content first:
    // a wrapper alone can be there while the record is still loading.
    for (const index of [0, 1]) {
      const fixture = page.getByTestId(FIXTURE).nth(index);
      await expect(fixture.getByTestId(EPIC_FIXTURE).getByTestId('epic-destination')).toBeVisible();
    }

    const { violations, failing } = await auditViewA11y(page, {
      include: [`[data-testid="${EPIC_FIXTURE}"]`],
    });
    const label = 'epic intent (en, phone)';
    for (const violation of violations) {
      if (!failing.includes(violation)) {
        console.log(`[a11y] ${formatViolation(label, violation)}`);
      }
    }
    const failures = failing.map((violation) => formatViolation(label, violation));
    expect(failures, `serious accessibility issues found:\n${failures.join('\n')}`).toEqual([]);
  });
});

import { test, expect } from '@playwright/test';
import { MOCK_RUNTIME_URL } from './helpers/ports.js';
import { auditViewA11y, formatViolation } from './helpers/a11y.js';

/**
 * The plan's intent and its outline on the development page (design
 * `intent-and-outline.adoc#intent`, `#outline`, `#builder`), and the epic's intent with the
 * `over_size` distribution (design `#epic-destination`, `#epic-rest`, `#oversize`, `#web`).
 *
 * The fixture there reads the mock runtime's `fix-relay`, `relay-hardening` and `pm-mcp-clients`
 * records through the real client, so this walk covers the read routes, the panels that draw them
 * and the words of both languages. The panels are shown on `/dev/components` until PLAN-55 places
 * them in the plan's Intent tab and PLAN-58/PLAN-59 in the epic's and the project's.
 */

/** The fixture's own hook: it is drawn once per theme, so its panels appear twice. */
const FIXTURE = 'intent-fixture';

/** The epic's intent panel inside the fixture, drawn once per theme. */
const EPIC_FIXTURE = 'epic-fixture';

/** The `over_size` charts inside the fixture, one per role, drawn once per theme. */
const CHART_FIXTURE = 'oversize-fixture';

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

  test('draws one over_size chart per role, the marks in words and not in colour alone', async ({
    page,
  }) => {
    await openFixtureIn(page, 'en');
    const charts = page.getByTestId(FIXTURE).first().getByTestId(CHART_FIXTURE);

    await expect(charts.getByTestId('over-size-chart')).toHaveCount(3);

    const implement = charts.getByTestId('over-size-chart').first();
    await expect(implement.getByTestId('oversize-title')).toHaveText(
      'implement · over_size 3x → 2x',
    );
    await expect(implement.getByTestId('oversize-bucket-0')).toContainText('0–1x');
    await expect(implement.getByTestId('oversize-bucket-0')).toContainText('8 tasks');
    await expect(implement.getByTestId('oversize-bucket-3')).toContainText('3x and more');
    await expect(implement.getByTestId('oversize-flagged-2')).toHaveText('3 flagged');
    await expect(implement.getByTestId('oversize-flagged-3')).toHaveText('4 flagged');
    await expect(implement.getByTestId('oversize-share')).toHaveText('20 tasks · 7 flagged · 35 %');
    await expect(implement.getByTestId('oversize-direction')).toHaveText(
      '3x → 2x lowers the multiple and can flag more',
    );

    // A role the runtime serves without a proposal: the multiple in force, no comparison.
    const tester = charts.getByTestId('over-size-chart').nth(1);
    await expect(tester.getByTestId('oversize-title')).toHaveText('module-tester · over_size 3x');
    await expect(tester.getByTestId('oversize-share')).toHaveCount(0);
    await expect(tester.getByTestId('oversize-direction')).toHaveCount(0);
    await expect(tester.locator('.oversize-bucket--flagged')).toHaveCount(0);

    // A role without measured tasks: the empty text instead of bars.
    const architecture = charts.getByTestId('over-size-chart').nth(2);
    await expect(architecture.getByTestId('oversize-empty')).toHaveText(
      'No measured tasks of this role yet.',
    );
    await expect(architecture.getByTestId('oversize-bucket-0')).toHaveCount(0);

    // No act: the chart is read-only.
    await expect(charts.locator('button')).toHaveCount(0);
    await expect(charts.locator('a')).toHaveCount(0);
  });

  test('draws the distribution under the multiple in force without a proposal', async ({
    page,
    request,
  }) => {
    const res = await request.post(`${MOCK_RUNTIME_URL}/mock/scenario`, {
      data: { scenario: 'fully-onboarded' },
    });
    expect(res.ok()).toBeTruthy();

    await openFixtureIn(page, 'en');
    const implement = page
      .getByTestId(FIXTURE)
      .first()
      .getByTestId(CHART_FIXTURE)
      .getByTestId('over-size-chart')
      .first();

    await expect(implement.getByTestId('oversize-title')).toHaveText('implement · over_size 3x');
    await expect(implement.getByTestId('oversize-bucket-3')).toContainText('3x and more');
    await expect(implement.getByTestId('oversize-share')).toHaveCount(0);
    await expect(implement.getByTestId('oversize-direction')).toHaveCount(0);
    await expect(implement.locator('.oversize-bucket--flagged')).toHaveCount(0);
  });

  test('draws the epic panel and the charts in the German catalogue', async ({ page }) => {
    await openFixtureIn(page, 'de');
    const fixture = page.getByTestId(FIXTURE).first();
    const epic = fixture.getByTestId(EPIC_FIXTURE);

    await expect(epic.getByTestId('epic-destination')).toHaveText(
      'The relay survives a dropped connection without losing an event.',
    );
    await expect(epic.getByTestId('epic-check-verdict-3')).toHaveText('ausstehend');
    await expect(epic.getByTestId('epic-non-goals')).toHaveText('Nicht-Ziele');

    const implement = fixture.getByTestId(CHART_FIXTURE).getByTestId('over-size-chart').first();
    await expect(implement.getByTestId('oversize-share')).toHaveText(
      '20 Tasks · 7 markiert · 35 %',
    );
    await expect(implement.getByTestId('oversize-direction')).toHaveText(
      '3x → 2x senkt den Faktor und kann mehr markieren',
    );
    await expect(fixture.getByTestId(CHART_FIXTURE).getByTestId('oversize-empty')).toHaveText(
      'Noch keine gemessenen Tasks dieser Role.',
    );
  });

  test('draws the epic panel and the charts in both themes', async ({ page }) => {
    await openFixtureIn(page, 'en');

    for (const index of [0, 1]) {
      const fixture = page.getByTestId(FIXTURE).nth(index);
      await expect(fixture.getByTestId(EPIC_FIXTURE).getByTestId('epic-destination')).toBeVisible();
      await expect(
        fixture.getByTestId(CHART_FIXTURE).getByTestId('over-size-chart').first(),
      ).toBeVisible();
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
      await expect(fixture.getByTestId(CHART_FIXTURE).getByTestId('over-size-chart')).toHaveCount(3);
    }

    const { violations, failing } = await auditViewA11y(page, {
      include: [`[data-testid="${EPIC_FIXTURE}"]`, `[data-testid="${CHART_FIXTURE}"]`],
    });
    const label = 'epic intent and over_size (en, phone)';
    for (const violation of violations) {
      if (!failing.includes(violation)) {
        console.log(`[a11y] ${formatViolation(label, violation)}`);
      }
    }
    const failures = failing.map((violation) => formatViolation(label, violation));
    expect(failures, `serious accessibility issues found:\n${failures.join('\n')}`).toEqual([]);
  });
});

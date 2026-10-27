import { test, expect } from '@playwright/test';
import { collectTextGuardViolations, TEXT_GUARD_RULES } from './helpers/textGuard.js';

/**
 * One violation per rule of CC-P-09 on a synthetic page: a raw enum, a state id, a
 * placeholder, an ISO time, an emoji and an English word inside the German interface.
 */
const ONE_VIOLATION_PER_RULE = `<!doctype html>
<html lang="de">
  <head><title>Start</title></head>
  <body>
    <p>waiting_for_you</p>
    <p>landing.pull-request</p>
    <p>Einen Plan in {project} starten</p>
    <p>2026-10-16T11:25:00Z</p>
    <p>🧹</p>
    <p>running</p>
  </body>
</html>`;

/**
 * The same violations, in the places the guard must skip and in one container it must still
 * read: model-written content, a chip, a code block and unrendered text stay silent, while a
 * `.stage` wrapper is no chip and is read.
 */
const SKIPPED_AND_READ = `<!doctype html>
<html lang="de">
  <head><title>Start</title></head>
  <body>
    <p>running</p>
    <p data-model-text>running done failed</p>
    <span class="badge">waiting_for_you</span>
    <code>landing.pull-request</code>
    <div class="stage">waiting_for_you</div>
    <p hidden>running</p>
  </body>
</html>`;

test.describe('Text guard rules', () => {
  test('reports one violation per rule, named by rule', async ({ page }) => {
    await page.setContent(ONE_VIOLATION_PER_RULE);

    const violations = await collectTextGuardViolations(page, { locale: 'de' });

    expect([...new Set(violations.map((violation) => violation.rule))].sort()).toEqual(
      [...TEXT_GUARD_RULES].sort(),
    );
    expect(violations.map((violation) => violation.word)).toEqual(
      expect.arrayContaining([
        'waiting_for_you',
        'landing.pull-request',
        '{project}',
        '<iso-time>',
        '🧹',
        'running',
      ]),
    );
  });

  test('skips model-written content, chips, code and unrendered text', async ({ page }) => {
    await page.setContent(SKIPPED_AND_READ);

    const violations = await collectTextGuardViolations(page, { locale: 'de' });

    expect(violations).toEqual([
      expect.objectContaining({ rule: 'foreign-word', word: 'running' }),
      expect.objectContaining({ rule: 'raw-enum', word: 'waiting_for_you' }),
    ]);
  });

  test('reports the German interface words in the English locale', async ({ page }) => {
    await page.setContent(
      '<!doctype html><html lang="en"><head><title>Start</title></head><body><p>läuft</p><p>fertig</p><p>running</p></body></html>',
    );

    const violations = await collectTextGuardViolations(page, { locale: 'en' });

    expect(violations.filter((violation) => violation.rule === 'foreign-word')).toHaveLength(2);
    expect(violations.map((violation) => violation.word).sort()).toEqual(['fertig', 'läuft']);
  });
});

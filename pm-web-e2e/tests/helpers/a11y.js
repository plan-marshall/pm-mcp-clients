import AxeBuilder from '@axe-core/playwright';

/**
 * The automated accessibility audit of CC-S-05 (CC-P-07): axe-core over a rendered view, with the
 * rules of WCAG 2.0 and 2.1 at level A and AA.
 *
 * Only an issue that keeps a person from using the page fails the run — the `serious` and
 * `critical` impacts. A `moderate` or `minor` issue is reported by the audit's own output and does
 * not gate, which is the line the plan draws.
 */
const AUDIT_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

/** The impacts that fail the run. */
export const FAILING_IMPACTS = ['serious', 'critical'];

/**
 * Runs axe-core over the page as it stands and keeps the issues that fail the run.
 *
 * @param {import('@playwright/test').Page} page the page of the view to audit
 * @returns {Promise<{ violations: Array<object>, failing: Array<object> }>} the audit's own result
 *   and the subset that gates
 */
export async function auditViewA11y(page) {
  const result = await new AxeBuilder({ page }).withTags(AUDIT_TAGS).analyze();
  const failing = result.violations.filter((violation) =>
    FAILING_IMPACTS.includes(violation.impact ?? ''),
  );
  return { violations: result.violations, failing };
}

/**
 * Renders one failing issue as the line a test reports.
 *
 * @param {string} view the declared view the issue was found in
 * @param {object} violation the axe-core violation
 * @returns {string} one line naming the view, the rule and the targets it was found on
 */
export function formatViolation(view, violation) {
  const targets = violation.nodes.map((node) => node.target.join(' ')).join(', ');
  return `${view}: ${violation.id} (${violation.impact}) — ${violation.help} — on ${targets}`;
}

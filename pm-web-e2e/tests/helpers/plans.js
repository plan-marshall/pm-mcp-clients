import { expect } from '@playwright/test';

/**
 * The plan cards of the overview, chosen by what the test means.
 *
 * The runtime serves the plans that need the operator first and a client does not sort them again
 * (`doc/design/shared/plan-overview.adoc`), so a card's position is not the plan's identity: a test
 * that asserts something about a plan names it, and a test that only needs some plan says so.
 */

/**
 * Opens a plan from the overview by its title.
 *
 * @param page the Playwright page
 * @param title the plan's title in its card
 */
export async function openPlan(page, title) {
  const planCard = page.locator('[data-testid="plan-card"]').filter({ hasText: title });
  await expect(planCard).toBeVisible();
  await planCard.click();
  await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();
}

/**
 * Opens any plan from the overview, for a test that only needs a plan view on screen.
 *
 * The test asserts nothing about which plan it is, so the first card the runtime serves is as good
 * as any other.
 *
 * @param page the Playwright page
 */
export async function openAnyPlan(page) {
  const planCard = page.locator('[data-testid="plan-card"]').first();
  await expect(planCard).toBeVisible();
  await planCard.click();
  await expect(page.locator('[data-testid="plan-view"]')).toBeVisible();
}

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import path from 'node:path';
import WizardPageFixture from '../../test/fixtures/WizardPageFixture.svelte';
import WizardPageStepsOnlyFixture from '../../test/fixtures/WizardPageStepsOnlyFixture.svelte';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `WizardPage` (UX-P-03, template C): the compact step bar, the one card and the footer with Back,
 * the secondary act and the primary act. The page is 720 px wide and centred.
 */

const SOURCE = path.resolve(__dirname, './WizardPage.svelte');

const PROPS = {
  steps: 'Step 2 of 5 · Language',
  body: 'One card of the wizard',
  backAct: 'Back',
  secondaryAct: 'Skip',
  primaryAct: 'Continue',
};

describe('WizardPage', () => {
  it('renders the step bar, the one card and the footer acts through its slots', () => {
    render(WizardPageFixture, { props: PROPS });

    expect(screen.getByTestId('wizard-page-steps')).toHaveTextContent('Step 2 of 5 · Language');
    expect(screen.getByTestId('wizard-page-card')).toHaveTextContent('One card of the wizard');
    expect(screen.getByRole('button', { name: 'Back' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skip' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument();
  });

  it('is 720 px wide and centred', () => {
    const root = ruleOf(styleOf(SOURCE), '.wizard-page');

    expect(root).toContain('max-width: 720px');
    expect(root).toContain('margin-inline: auto');
    expect(root).toContain('width: 100%');
  });

  it('draws no footer row when the view fills no footer slot', () => {
    render(WizardPageStepsOnlyFixture, {
      props: { steps: 'Step 1 of 5 · Language', body: 'One card of the wizard' },
    });

    expect(screen.queryByTestId('wizard-page-footer')).not.toBeInTheDocument();
  });

  it('takes the 12 px inset on the phone', () => {
    const style = styleOf(SOURCE);
    const index = style.indexOf('@media (max-width: 480px)');

    expect(index).toBeGreaterThanOrEqual(0);
    expect(style.slice(index)).toMatch(/padding: var\(--space-3\)/);
  });
});

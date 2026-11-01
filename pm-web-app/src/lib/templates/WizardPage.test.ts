import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import path from 'node:path';
import WizardPageFixture from '../../test/fixtures/WizardPageFixture.svelte';
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
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/svelte';
import { tick } from 'svelte';
import path from 'node:path';
import WizardPageFixture from '../../test/fixtures/WizardPageFixture.svelte';
import WizardPageStepsOnlyFixture from '../../test/fixtures/WizardPageStepsOnlyFixture.svelte';
import { reportBox } from '../test/resizeObserver';
import { styleOf, ruleOf } from '../../test/templateStyle';

/**
 * `WizardPage` (UX-P-03, template C): the compact step bar, the one card and the footer with Back,
 * the secondary act and the primary act. The page is 720 px wide and centred; beside it, the chat
 * docks as a side column of `480` or `400 px` by the width of the page's own container, and takes
 * the space where the two do not fit side by side (`navigation.adoc#side-columns`).
 */

const SOURCE = path.resolve(__dirname, './WizardPage.svelte');
const SIDE_SOURCE = path.resolve(__dirname, './SideColumn.svelte');

const PROPS = {
  steps: 'Step 2 of 5 · Language',
  body: 'One card of the wizard',
  backAct: 'Back',
  secondaryAct: 'Skip',
  primaryAct: 'Continue',
  sideBody: 'The chat',
};

/**
 * Renders the wizard and reports its container's width, as a browser's observer would.
 *
 * @param container the container's width in px
 * @param sideOpen whether the chat's side column is open
 * @param onHideSide the act the side column reports
 * @returns the rendered page
 */
async function renderWizard(container: number, sideOpen = true, onHideSide = () => {}) {
  const result = render(WizardPageFixture, { props: { ...PROPS, sideOpen, onHideSide } });
  const frame = screen.getByTestId('wizard-page');
  reportBox(frame, container, 800);
  await tick();
  return result;
}

/**
 * @param testId the element's test id
 * @returns the width the side column takes from its bound custom property
 */
function sideWidthOf(testId: string): number {
  return Number.parseFloat(
    screen.getByTestId(testId).style.getPropertyValue('--side-width'),
  );
}

/**
 * @param testId the element's test id
 * @returns the inline width it carries
 */
function widthOf(testId: string): number {
  return Number.parseFloat(screen.getByTestId(testId).style.inlineSize);
}

describe('WizardPage', () => {
  it('renders the step bar, the one card and the footer acts through its slots', async () => {
    await renderWizard(1048, false);

    expect(screen.getByTestId('wizard-page-steps')).toHaveTextContent('Step 2 of 5 · Language');
    expect(screen.getByTestId('wizard-page-card')).toHaveTextContent('One card of the wizard');
    expect(screen.getByRole('button', { name: 'Back' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skip' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument();
  });

  it('keeps its content at 720 px and centred without a side column', async () => {
    await renderWizard(1048, false);

    const style = styleOf(SOURCE);

    expect(ruleOf(style, '.wizard-page__content')).toContain('max-inline-size: 720px');
    expect(ruleOf(style, '.wizard-page__content')).toContain('margin-inline: auto');
    expect(screen.queryByTestId('side-column')).toBeNull();
  });

  it('draws no footer row when the view fills no footer slot', () => {
    render(WizardPageStepsOnlyFixture, {
      props: { steps: 'Step 1 of 5 · Language', body: 'One card of the wizard' },
    });

    expect(screen.queryByTestId('wizard-page-footer')).not.toBeInTheDocument();
  });

  it('draws no side column while the chat is closed', async () => {
    await renderWizard(1048, false);

    expect(screen.queryByTestId('side-column')).toBeNull();
    expect(screen.getByText('One card of the wizard')).toBeInTheDocument();
  });

  it('docks the chat at 480 px where the container keeps both side by side', async () => {
    await renderWizard(1048);

    expect(sideWidthOf('side-column')).toBe(480);
    expect(widthOf('wizard-content')).toBeGreaterThanOrEqual(480);
  });

  it('narrows the chat to 400 px where 480 would drop the content below 480', async () => {
    await renderWizard(952);

    expect(sideWidthOf('side-column')).toBe(400);
    expect(widthOf('wizard-content')).toBeGreaterThanOrEqual(480);
  });

  it('lets the chat take the space where the content and the chat do not fit', async () => {
    await renderWizard(800);

    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');
    // The content stays mounted — a form keeps its state — and is hidden from layout.
    const content = screen.getByTestId('wizard-content');
    expect(content).toHaveAttribute('hidden');
  });

  it('moves the focus into the chat on takeover and back to the content on return', async () => {
    const rendered = await renderWizard(1048);
    const content = screen.getByRole('button', { name: 'Continue' });
    content.focus();
    expect(document.activeElement).toBe(content);

    // The container narrows until the chat takes the space: the focus goes into the chat.
    reportBox(screen.getByTestId('wizard-page'), 800, 800);
    await tick();
    expect(document.activeElement).toHaveTextContent('Hide the chat');

    // It widens again: the focus returns to the control the content held.
    reportBox(screen.getByTestId('wizard-page'), 1048, 800);
    await tick();
    expect(document.activeElement).toBe(content);
    void rendered;
  });

  it('hides the chat from anywhere on the page', async () => {
    const onHideSide = vi.fn();
    await renderWizard(800, true, onHideSide);

    // The content is hidden, so the focus may sit on the page; Escape still reaches the page.
    await fireEvent.keyDown(document.body, { key: 'Escape' });

    expect(onHideSide).toHaveBeenCalledOnce();
  });

  it('leaves the width to the stylesheet, so the phone rule can take the screen', async () => {
    await renderWizard(1048);

    const side = screen.getByTestId('side-column');

    expect(side.style.inlineSize).toBe('');
    expect(side.style.getPropertyValue('--side-width')).toBe('480px');
  });

  it('hides the chat on its own act and on Escape', async () => {
    const onHideSide = vi.fn();
    await renderWizard(1048, true, onHideSide);

    await fireEvent.click(screen.getByRole('button', { name: 'Hide the chat' }));
    expect(onHideSide).toHaveBeenCalledOnce();

    await fireEvent.keyDown(screen.getByTestId('wizard-page'), { key: 'Escape' });
    expect(onHideSide).toHaveBeenCalledTimes(2);
  });

  it('covers the bottom bar on a phone', async () => {
    await renderWizard(390);

    expect(screen.getByTestId('side-column')).toHaveClass('side-column--taken');
    const style = styleOf(SIDE_SOURCE);
    const phone = style.slice(style.indexOf('@media (max-width: 767px)'));

    expect(phone).toContain('position: fixed');
    expect(phone).toContain('inset: 0');
  });

  it('takes the 12 px inset on the phone', () => {
    const style = styleOf(SOURCE);
    const index = style.indexOf('@media (max-width: 480px)');

    expect(index).toBeGreaterThanOrEqual(0);
    expect(style.slice(index)).toMatch(/padding-block: var\(--space-3\)/);
  });
});

import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { render, screen } from '@testing-library/svelte';
import FormStatus from './FormStatus.svelte';
import { ruleOf, styleOf } from '../../test/templateStyle';

/**
 * `FormStatus` (components-fields.adoc, One Field System): the result line of a form. Its polite
 * live region is always in the document, and the danger tone carries an icon beside its sentence.
 */

const STYLE = styleOf(path.resolve(__dirname, 'FormStatus.svelte'));

describe('FormStatus', () => {
  it('keeps an empty polite live region in the document without a message', () => {
    render(FormStatus, { props: { id: 'settings-status' } });
    const region = screen.getByRole('status');

    expect(region).toHaveAttribute('aria-live', 'polite');
    expect(region).toHaveAttribute('id', 'settings-status');
    expect(region).toBeEmptyDOMElement();
  });

  it('puts the first message into the region that was already in the document', async () => {
    const { rerender } = render(FormStatus, { props: {} });
    const region = screen.getByRole('status');

    await rerender({ message: 'Saved', tone: 'success' });

    expect(screen.getByRole('status')).toBe(region);
    expect(region).toHaveTextContent('Saved');
  });

  it('draws the error icon beside the sentence in the danger tone', () => {
    render(FormStatus, { props: { message: 'The settings could not be saved.', tone: 'danger' } });
    const region = screen.getByRole('status');
    const icon = region.querySelector('[data-icon="state.error"]');

    expect(icon).not.toBeNull();
    expect(icon).toHaveAttribute('aria-hidden', 'true');
    expect(region).toHaveTextContent('The settings could not be saved.');
    expect(region).toHaveClass('form-status--danger');
  });

  it.each(['neutral', 'success'] as const)('draws the %s tone as its sentence alone', (tone) => {
    render(FormStatus, { props: { message: 'Give a reason.', tone } });
    const region = screen.getByRole('status');

    expect(region.querySelector('[data-icon]')).toBeNull();
    expect(region).toHaveClass(`form-status--${tone}`);
    expect(region).toHaveTextContent('Give a reason.');
  });

  it('draws the neutral tone when none is asked for', () => {
    render(FormStatus, { props: { message: 'Saved' } });

    expect(screen.getByRole('status')).toHaveClass('form-status--neutral');
  });

  it('colours each tone from its token', () => {
    expect(ruleOf(STYLE, '.form-status--neutral')).toContain('color: var(--color-text-secondary)');
    expect(ruleOf(STYLE, '.form-status--success')).toContain('color: var(--color-success)');
    expect(ruleOf(STYLE, '.form-status--danger')).toContain('color: var(--color-danger)');
  });
});

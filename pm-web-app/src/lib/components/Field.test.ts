import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import Field from './Field.svelte';
import TextField from './TextField.svelte';
import TextArea from './TextArea.svelte';
import NumberField from './NumberField.svelte';
import SelectField from './SelectField.svelte';
import CheckField from './CheckField.svelte';
import SecretField from './SecretField.svelte';
import CodeField from './CodeField.svelte';
import { styleOf } from '../../test/templateStyle';
import { REMOVED_OUTLINE } from '../../test/fieldContract';

describe('Field', () => {
  it('renders label above control', () => {
    const { container } = render(Field, {
      props: { label: 'Path of checkout', id: 'field-1' },
    });
    const label = container.querySelector('label');
    expect(label).toBeInTheDocument();
    expect(label?.textContent).toContain('Path of checkout');
    expect(label).toHaveAttribute('for', 'field-1');
  });

  it('renders (optional) mark after optional label', () => {
    const { container } = render(Field, {
      props: { label: 'Note', optional: true, id: 'field-2' },
    });
    const label = container.querySelector('label');
    expect(label?.textContent).toContain('Note');
    expect(label?.textContent).toContain('(optional)');
  });

  it('renders hint under control', () => {
    const { container } = render(Field, {
      props: { label: 'Note', hint: 'One sentence hint.', id: 'field-3' },
    });
    const hint = container.querySelector('.field__hint');
    expect(hint).toBeInTheDocument();
    expect(hint?.textContent).toContain('One sentence hint.');
  });

  it('renders error in place of hint while showing', () => {
    const { container } = render(Field, {
      props: { label: 'Note', hint: 'One sentence hint.', error: 'Give a reason.', id: 'field-4' },
    });
    const hint = container.querySelector('.field__hint');
    const err = container.querySelector('.field-error');
    expect(hint).toBeNull();
    expect(err).toBeInTheDocument();
    expect(err?.textContent).toContain('Give a reason.');
  });

  it('sets disabled state with reason', () => {
    const { container } = render(Field, {
      props: { label: 'Note', disabled: true, disabledReason: 'Not available', id: 'field-5' },
    });
    const reason = container.querySelector('.field__disabled-reason');
    expect(reason).toBeInTheDocument();
    expect(reason?.textContent).toContain('Not available');
    expect(container.querySelector('.field')).toHaveClass('field--disabled');
  });

  it('leaves the focus outline of every field control to the design, never removing it without the focus ring', async () => {
    // The population is the field components of components-fields.adoc that draw a control of their
    // own: a component added there has to be added here, which is a visible decision and not a
    // silently skipped one. The design draws the ring in the component's own `:focus` rule, a block
    // apart from the one that removes the outline, so the ring is looked for in the whole sheet.
    const fields = [
      {
        name: 'Field',
        draw: () => render(Field, { props: { label: 'Path', id: 'field-guard' } }),
      },
      { name: 'TextField', draw: () => render(TextField, { props: { label: 'Name' } }) },
      { name: 'TextArea', draw: () => render(TextArea, { props: { label: 'Note' } }) },
      { name: 'NumberField', draw: () => render(NumberField, { props: { label: 'Count' } }) },
      {
        name: 'SelectField',
        draw: () =>
          render(SelectField, {
            props: { label: 'Pick', options: [{ value: '1', label: 'One' }], value: '1' },
          }),
      },
      { name: 'CheckField', draw: () => render(CheckField, { props: { label: 'Agree' } }) },
      { name: 'SecretField', draw: () => render(SecretField, { props: { label: 'Secret' } }) },
      { name: 'CodeField', draw: () => render(CodeField, { props: { label: 'Code' } }) },
    ];

    for (const field of fields) {
      const rendered = field.draw();
      const control = rendered.container.querySelector('input, textarea, select') as
        | HTMLElement
        | null;
      if (field.name !== 'Field') {
        expect(control, `${field.name} renders a control`).not.toBeNull();
        control?.focus();
        expect(document.activeElement, `${field.name}'s control takes the focus`).toBe(control);
      }

      const style = styleOf(path.resolve(__dirname, `./${field.name}.svelte`));
      if (REMOVED_OUTLINE.test(style)) {
        expect(style, `${field.name} removes its outline and must draw the focus ring`).toContain(
          'var(--focus-ring)',
        );
      }
      rendered.unmount();
    }
  });
});

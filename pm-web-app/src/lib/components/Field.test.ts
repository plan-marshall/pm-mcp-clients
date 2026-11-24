import path from 'node:path';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/svelte';
import Field from './Field.svelte';
import TextField from './TextField.svelte';
import TextArea from './TextArea.svelte';
import NumberField from './NumberField.svelte';
import SelectField from './SelectField.svelte';
import CheckField from './CheckField.svelte';
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
    // The population is derived from the roles the field components render, so a component that
    // gains, loses or renames its control is re-checked rather than silently left out. The Field
    // wrapper is in it too: it draws no control of its own, but a rule of its own could still take
    // the outline away from the control it wraps.
    const fields = [
      { name: 'Field', component: Field, props: { label: 'Path', id: 'field-guard' } },
      { name: 'TextField', component: TextField, props: { label: 'Name' } },
      { name: 'TextArea', component: TextArea, props: { label: 'Note' } },
      { name: 'NumberField', component: NumberField, props: { label: 'Count' } },
      {
        name: 'SelectField',
        component: SelectField,
        props: { label: 'Pick', options: [{ value: '1', label: 'One' }], value: '1' },
      },
      { name: 'CheckField', component: CheckField, props: { label: 'Agree' } },
    ];

    for (const field of fields) {
      const rendered = render(field.component, { props: field.props });
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

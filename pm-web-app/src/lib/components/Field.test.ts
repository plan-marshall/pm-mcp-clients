import fs from 'node:fs';
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
import FilterInput from './FilterInput.svelte';
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
    // silently skipped one. A rule that takes the outline away must draw the ring in the same block:
    // forced-colors mode drops a box-shadow-less focus mark entirely.
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
      {
        name: 'FilterInput',
        draw: () => render(FilterInput, { props: { label: 'Filter', onchange: () => {} } }),
      },
    ];

    // The population is not hand-picked: every field component of the directory has to be in it, so
    // a new one cannot be added to the design and left out of this guard.
    const rendered = new Set(fields.map((field) => `${field.name}.svelte`));
    const present = fs
      .readdirSync(__dirname)
      .filter((name) => (name.endsWith('Field.svelte') && name !== 'FieldError.svelte') ||
        name === 'FilterInput.svelte');
    expect(present.filter((name) => !rendered.has(name))).toEqual([]);

    for (const field of fields) {
      const drawn = field.draw();
      const control = drawn.container.querySelector('input, textarea, select') as
        | HTMLElement
        | null;
      if (field.name !== 'Field') {
        expect(control, `${field.name} renders a control`).not.toBeNull();
        control?.focus();
        expect(document.activeElement, `${field.name}'s control takes the focus`).toBe(control);
      }

      const style = styleOf(path.resolve(__dirname, `./${field.name}.svelte`));
      // Rule by rule: a block that takes the outline away must draw the ring in that same block.
      for (const block of style.split('}')) {
        if (REMOVED_OUTLINE.test(block)) {
          expect(
            block,
            `${field.name} removes its outline without drawing the focus ring in the same rule`,
          ).toContain('var(--focus-ring)');
        }
      }
      drawn.unmount();
    }
  });
});

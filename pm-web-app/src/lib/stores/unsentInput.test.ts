import { describe, it, expect, beforeEach } from 'vitest';
import { get } from 'svelte/store';
import {
  hasUnsentInput,
  hasUnsentInputStore,
  scanUnsentInput,
  startUnsentInputTracking,
} from './unsentInput';

describe('unsent input', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
    hasUnsentInputStore.set(false);
  });

  it('sees no unsent text in an empty document', () => {
    expect(scanUnsentInput()).toBe(false);
  });

  it('sees text in an input and in a textarea', () => {
    const input = document.createElement('input');
    input.value = 'half an answer';
    document.body.appendChild(input);
    expect(scanUnsentInput()).toBe(true);

    input.value = '   ';
    expect(scanUnsentInput()).toBe(false);

    const textarea = document.createElement('textarea');
    textarea.value = 'a draft';
    document.body.appendChild(textarea);
    expect(hasUnsentInput()).toBe(true);
  });

  it('ignores disabled and read-only fields, and the checkboxes', () => {
    const disabled = document.createElement('input');
    disabled.value = 'kept';
    disabled.disabled = true;
    const readOnly = document.createElement('textarea');
    readOnly.value = 'kept';
    readOnly.readOnly = true;
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = true;
    document.body.append(disabled, readOnly, checkbox);

    expect(scanUnsentInput()).toBe(false);
  });

  it('keeps the store current as the operator types', () => {
    startUnsentInputTracking(document);
    const field = document.createElement('textarea');
    document.body.appendChild(field);
    field.value = 'typed';

    field.dispatchEvent(new Event('input', { bubbles: true }));

    expect(get(hasUnsentInputStore)).toBe(true);
  });
});

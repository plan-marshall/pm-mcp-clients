import { describe, it, expect, afterEach } from 'vitest';
import { portal, themeScope } from './portal';

/**
 * `portal` (PLAN-172, PLAN-156 D6): moves a node under its theme scope, keeps the focus it holds
 * inside it, and removes the node on destroy.
 */

/** @param html the markup of a scratch element appended to the body */
function append(html: string): HTMLElement {
  const host = document.createElement('div');
  host.innerHTML = html;
  document.body.appendChild(host);
  return host;
}

describe('themeScope', () => {
  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('answers the document body when no ancestor carries a theme', () => {
    const host = append('<div id="slot"></div>');
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;

    expect(themeScope(slot)).toBe(document.body);
  });

  it('answers the nearest themed section the node is drawn in', () => {
    const host = append('<section data-theme="dark"><div id="slot"></div></section>');
    const section = host.querySelector<HTMLElement>('section') as HTMLElement;
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;

    expect(themeScope(slot)).toBe(section);
  });

  it('does not answer the document element, whose theme is the page’s own', () => {
    document.documentElement.setAttribute('data-theme', 'dark');
    const host = append('<div id="slot"></div>');
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;

    try {
      expect(themeScope(slot)).toBe(document.body);
    } finally {
      document.documentElement.removeAttribute('data-theme');
    }
  });
});

describe('portal', () => {
  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('moves the node under its theme scope and removes it when the action is destroyed', () => {
    const host = append('<section data-theme="dark"><div id="slot"></div></section>');
    const section = host.querySelector<HTMLElement>('section') as HTMLElement;
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;
    const node = document.createElement('div');
    slot.appendChild(node);

    const action = portal(node);

    expect(node.parentElement).toBe(section);

    action.destroy();

    expect(node.parentElement).toBeNull();
  });

  it('takes the target the caller names', () => {
    const host = append('<div id="slot"></div>');
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;
    const target = document.createElement('aside');
    document.body.appendChild(target);
    const node = document.createElement('div');
    slot.appendChild(node);

    portal(node, target);

    expect(node.parentElement).toBe(target);
  });

  it('keeps the focus of a control inside the node it moves', () => {
    const host = append('<div id="slot"><button id="inner">Act</button></div>');
    const slot = host.querySelector<HTMLElement>('#slot') as HTMLElement;
    const button = host.querySelector<HTMLButtonElement>('#inner') as HTMLButtonElement;
    button.focus();

    portal(slot);

    expect(document.activeElement).toBe(button);
  });
});

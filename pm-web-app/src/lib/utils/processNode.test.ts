import { describe, it, expect } from 'vitest';
import { currentLocale, messagesStore, translate } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import type { ProcessNode } from '../api/client';
import type { Translate } from './processNode';
import { getNodeStateIcon, getNodeStateText } from './processNode';

/**
 * The words of a process state come from the catalogue, and every value of the runtime's
 * `NodeState` reads as one of them: a raw wire value never reaches the operator.
 */

function useCatalogue(locale: 'de' | 'en'): Translate {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
  return (key, params) => translate(locale, key, params);
}

/**
 * @param state a process state, from the runtime's closed list
 * @param overrides the counts the state's word needs
 * @returns the node the tree and the narrative render
 */
function nodeIn(state: string, overrides: Partial<ProcessNode> = {}): ProcessNode {
  return {
    node_id: 'execute',
    title: 'phase execute',
    kind: 'phase',
    state,
    run_count: 1,
    current_run: 1,
    duration_seconds: 0,
    sub_nodes: [],
    leaf_tasks: [],
    ...overrides,
  };
}

describe('getNodeStateText', () => {
  it('names the waiting state the runtime sends as waiting_for_operator', () => {
    const english = useCatalogue('en');
    expect(getNodeStateText(nodeIn('waiting_for_operator'), english)).toBe('waiting for you');

    const german = useCatalogue('de');
    expect(getNodeStateText(nodeIn('waiting_for_operator'), german)).toBe('wartet auf dich');
  });

  it.each([
    ['done', 'done', 'fertig'],
    ['running', 'running', 'läuft'],
    ['failed', 'failed', 'fehlgeschlagen'],
    ['planned', 'planned', 'geplant'],
    ['not_taken', 'not taken', 'nicht genommen'],
    ['not_in_plan', 'not in this plan', 'nicht in diesem Plan'],
  ])('names the %s state', (state, english, german) => {
    expect(getNodeStateText(nodeIn(state), useCatalogue('en'))).toBe(english);
    expect(getNodeStateText(nodeIn(state), useCatalogue('de'))).toBe(german);
  });

  it('names a state the catalogue carries no word for with the unknown word', () => {
    expect(getNodeStateText(nodeIn('waiting_for_operator_forever'), useCatalogue('en'))).toBe(
      'unknown',
    );
    expect(getNodeStateText(nodeIn('waiting_for_operator_forever'), useCatalogue('de'))).toBe(
      'unbekannt',
    );
  });

  it('names the run a node is in', () => {
    const english = useCatalogue('en');
    expect(getNodeStateText(nodeIn('done', { run_count: 2 }), english)).toBe('done ×2');
    expect(getNodeStateText(nodeIn('running', { current_run: 3 }), english)).toBe(
      'running · run 3',
    );
  });
});

describe('getNodeStateIcon', () => {
  it('marks the waiting state with the alert glyph, whichever name it arrives under', () => {
    expect(getNodeStateIcon('waiting_for_operator')).toBe('state.alert');
    expect(getNodeStateIcon('waiting_for_you')).toBe('state.alert');
  });
});

import { describe, it, expect } from 'vitest';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
import { displayError, displayState, displayUnit } from './index';

/**
 * The display layer reads the shared vocabulary of the catalogue (`state.*`, `unit.*`) and the
 * error texts of `PLAN-86` (`error.*`). The expectations below are the contract of those texts,
 * so they stay literal.
 */
function useCatalogue(locale: 'de' | 'en'): void {
  currentLocale.set(locale);
  messagesStore.set(loadMessages(locale));
}

describe('displayState', () => {
  it('names a plan state through the shared vocabulary', () => {
    useCatalogue('en');
    expect(displayState('plan', 'running')).toBe('running');
    expect(displayState('plan', 'waiting_for_you')).toBe('waiting for you');

    useCatalogue('de');
    expect(displayState('plan', 'running')).toBe('läuft');
    expect(displayState('plan', 'waiting_for_you')).toBe('wartet auf dich');
  });

  it('names a worker state, a task state and a cleanup kind', () => {
    useCatalogue('en');
    expect(displayState('worker', 'busy')).toBe('busy');
    expect(displayState('task', 'done')).toBe('done');
    expect(displayState('cleanup', 'read_reports')).toBe('Read reports');

    useCatalogue('de');
    expect(displayState('worker', 'busy')).toBe('beschäftigt');
    expect(displayState('task', 'done')).toBe('fertig');
    expect(displayState('cleanup', 'read_reports')).toBe('Gelesene Berichte');
  });

  it('names an activity kind, including the wire names it aliases', () => {
    useCatalogue('en');
    expect(displayState('activity', 'tool_call')).toBe('tool');
    expect(displayState('activity', 'tool')).toBe('tool');
    expect(displayState('activity', 'assistant_text')).toBe('wrote');
    expect(displayState('activity', 'file_changed')).toBe('changed');

    useCatalogue('de');
    expect(displayState('activity', 'assistant_text')).toBe('schrieb');
    expect(displayState('activity', 'file_changed')).toBe('geändert');
  });

  it('names a node kind, which the API sends as a wire value', () => {
    useCatalogue('en');
    expect(displayState('node_kind', 'sub_workflow')).toBe('Sub workflow');
    expect(displayState('node_kind', 'phase')).toBe('Phase');

    useCatalogue('de');
    expect(displayState('node_kind', 'sub_workflow')).toBe('Teilablauf');
    expect(displayState('node_kind', 'phase')).toBe('Phase');
  });

  it('reads a value without regard to its case', () => {
    useCatalogue('en');
    expect(displayState('plan', 'RUNNING')).toBe('running');
  });

  it('answers an unknown value with the unknown word, never the wire value', () => {
    useCatalogue('en');
    expect(displayState('plan', 'hyperdrive')).toBe('unknown');
    expect(displayState('plan', '')).toBe('unknown');
    expect(displayState('plan', null)).toBe('unknown');

    useCatalogue('de');
    expect(displayState('plan', 'hyperdrive')).toBe('unbekannt');
    expect(displayState('plan', undefined)).toBe('unbekannt');
  });
});

describe('displayError', () => {
  it('reads the title, cause and fix of a known code in English', () => {
    useCatalogue('en');
    expect(displayError('scope_not_found')).toEqual({
      title: 'This entry does not exist',
      cause: 'No plan, epic, decision, report or entry carries this id.',
      fix: 'Check the id or pick the entry from the list again.',
    });
  });

  it('reads the title, cause and fix of a known code in German', () => {
    useCatalogue('de');
    expect(displayError('project_not_enrolled')).toEqual({
      title: 'Das Projekt ist nicht eingetragen',
      cause: 'Zu dem angegebenen Namen oder Pfad gehört kein eingetragenes Projekt.',
      fix: 'Trage das Projekt zuerst ein und wiederhole die Aktion.',
    });
  });

  it('reads a wire code the API sends in upper case', () => {
    useCatalogue('en');
    expect(displayError('SCOPE_NOT_FOUND').title).toBe('This entry does not exist');
  });

  it('answers an unknown code with the shared unknown fallback', () => {
    useCatalogue('en');
    expect(displayError('hyperdrive_broken')).toEqual({
      title: 'The runtime refused the request',
      cause: 'The refusal carries a code this client does not know.',
      fix: 'Update the client and try again; if the refusal stays, look at the details.',
    });

    useCatalogue('de');
    expect(displayError('hyperdrive_broken').title).toBe(
      'Die Runtime hat die Anfrage abgelehnt',
    );
  });
});

describe('displayUnit', () => {
  it('names a workflow unit in both locales', () => {
    useCatalogue('en');
    expect(displayUnit('landing.pull-request')).toBe('Pull request');
    expect(displayUnit('remediation.ci-failure')).toBe('CI failure');

    useCatalogue('de');
    expect(displayUnit('landing.pull-request')).toBe('Pull Request');
    expect(displayUnit('remediation.ci-failure')).toBe('CI-Fehler');
  });

  it('answers an unknown unit with the unknown word, never the id', () => {
    useCatalogue('en');
    expect(displayUnit('execute.envelope')).toBe('unknown');
    expect(displayUnit('')).toBe('unknown');

    useCatalogue('de');
    expect(displayUnit('execute.envelope')).toBe('unbekannt');
    expect(displayUnit(null)).toBe('unbekannt');
  });
});

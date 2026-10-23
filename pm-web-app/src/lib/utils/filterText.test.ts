import { describe, it, expect } from 'vitest';
import {
  normalizeText,
  tokenize,
  buildHaystack,
  matchesHaystack,
  matchesQuery,
} from './filterText';

describe('filterText', () => {
  it('folds case and collapses whitespace', () => {
    expect(normalizeText('  Fix   The   Relay ')).toBe('fix the relay');
  });

  it('strips diacritics so Ärger matches arger', () => {
    expect(normalizeText('Ärger über Öfen')).toBe('arger uber ofen');
    expect(matchesQuery('arger', ['Ärger mit dem Relay'])).toBe(true);
  });

  it('treats null and undefined as empty text', () => {
    expect(normalizeText(null)).toBe('');
    expect(normalizeText(undefined)).toBe('');
    expect(tokenize(null)).toEqual([]);
    expect(tokenize(undefined)).toEqual([]);
  });

  it('splits a query into normalized tokens', () => {
    expect(tokenize('  Fix   RELAY ')).toEqual(['fix', 'relay']);
  });

  it('matches everything for an empty or blank query', () => {
    expect(matchesQuery('', ['anything'])).toBe(true);
    expect(matchesQuery('   ', ['anything'])).toBe(true);
    expect(matchesQuery(null, [])).toBe(true);
  });

  it('demands every token of a multi-token query', () => {
    expect(matchesQuery('fix relay', ['fix-relay ended: merged'])).toBe(true);
    expect(matchesQuery('fix missing', ['fix-relay ended: merged'])).toBe(false);
  });

  it('lets one token match one field and another token match another field', () => {
    expect(matchesQuery('relay merged', ['fix-relay', 'ended: merged'])).toBe(true);
  });

  it('does not assemble a token from the end of one field and the start of the next', () => {
    expect(matchesQuery('relaymerged', ['fix-relay', 'merged'])).toBe(false);
  });

  it('builds one haystack of the non-empty normalized values', () => {
    expect(buildHaystack(['Fix-Relay', null, '  ', 'Merged'])).toBe('fix-relay\nmerged');
  });

  it('matches a token against a prepared haystack', () => {
    const haystack = buildHaystack(['fix-relay', 'today 11:20']);
    expect(matchesHaystack(tokenize('relay 11:20'), haystack)).toBe(true);
    expect(matchesHaystack(tokenize('relay 12:20'), haystack)).toBe(false);
  });
});

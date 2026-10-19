import { describe, it, expect } from 'vitest';
import { formatString } from '../i18n';
import { failureMessage, readForgeReference, repositoryOf, resetTime } from './sourceLabels';

describe('reading an issue or pull-request reference', () => {
  it('recognises the forges this machine knows', () => {
    expect(readForgeReference('https://codeberg.org/forgejo/forgejo/issues/4711')).toEqual({
      forge: 'Codeberg',
      repository: 'forgejo/forgejo',
      number: '4711',
      url: 'https://codeberg.org/forgejo/forgejo/issues/4711',
    });
    expect(
      readForgeReference('https://github.com/plan-marshall/pm-mcp-clients/pull/214')?.number,
    ).toBe('214');
    expect(readForgeReference('https://gitlab.com/group/project/issues/7')?.forge).toBe('GitLab');
  });

  it('reads a bare number as the project repository', () => {
    expect(readForgeReference('212', 'plan-marshall/pm-mcp-clients')).toEqual({
      forge: 'GitHub',
      repository: 'plan-marshall/pm-mcp-clients',
      number: '212',
      url: 'https://github.com/plan-marshall/pm-mcp-clients/issues/212',
    });
    expect(readForgeReference('#212', 'plan-marshall/pm-mcp-clients')?.number).toBe('212');
  });

  it('names no reference for an unknown forge or a plain word', () => {
    expect(readForgeReference('https://example.com/a/b/issues/1')).toBeNull();
    expect(readForgeReference('not a reference')).toBeNull();
    expect(readForgeReference('https://github.com/a/b')).toBeNull();
  });

  it('names the repository of a reference', () => {
    expect(repositoryOf('https://codeberg.org/forgejo/forgejo/issues/4711')).toBe(
      'forgejo/forgejo',
    );
    expect(repositoryOf('nonsense')).toBe('');
  });
});

describe('the text of a failed source', () => {
  it('names the reset time of a rate limit and the forge that may raise it', () => {
    const message = failureMessage({
      cause: 'rate_limited',
      detail: 'Codeberg',
      resets_at: 1792507200,
    });
    expect(message.key).toBe('start.cause.rate_limited');
    expect(message.args['credential']).toBe('present');
    expect(message.args['forge']).toBe('Codeberg');
    expect(String(message.args['time'])).toMatch(/\d{1,2}:\d{2}/);
  });

  it('asks for a credential only where the fetch ran without one', () => {
    const message = failureMessage({ cause: 'rate_limited', resets_at: 1792507200 });
    expect(message.args['credential']).toBe('none');
    expect(
      formatString(
        'rate limited until {time}{credential, select, none {; add a credential for {forge} for a higher limit} other {}}',
        message.args,
      ),
    ).toContain('add a credential');
  });

  it('carries the cause detail of a failed screening', () => {
    expect(failureMessage({ cause: 'screening_failed', detail: 'the screener timed out' })).toEqual(
      {
        key: 'start.cause.screening_failed',
        args: { cause: 'the screener timed out' },
      },
    );
  });

  it('shows no time where no reset is named', () => {
    expect(resetTime(null)).toBe('');
  });
});

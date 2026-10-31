import { describe, it, expect } from 'vitest';
import {
  formatBytes,
  formatDay,
  formatDuration,
  formatSplit,
  formatTime,
  formatTokens,
} from './format';

/**
 * The tables of `doc/design/shared/texts-and-languages.adoc`: one row per example the design
 * states, in both interface languages.
 *
 * Every instant is built from local time, because the rule shows local time: a test that pinned a
 * `Z` literal would pass in one timezone and fail in the next.
 */

/** A fixed local noon, so "today", "yesterday" and the age rows are stable. */
const NOW = new Date(2026, 9, 16, 12, 0, 0);

/**
 * @param month the month, 1-based
 * @param day the day of the month
 * @param hour the hour of the day
 * @param minute the minute
 * @param second the second
 * @returns the instant as the runtime sends it, an ISO string
 */
function at(month: number, day: number, hour: number, minute: number, second = 0): string {
  return new Date(2026, month - 1, day, hour, minute, second).toISOString();
}

/** @param ms the distance before {@link NOW} @returns that instant as an ISO string */
function ago(ms: number): string {
  return new Date(NOW.getTime() - ms).toISOString();
}

describe('formatTime', () => {
  describe('relative', () => {
    it.each([
      ['just now', ago(20_000), 'just now', 'gerade eben'],
      ['minutes', ago(5 * 60_000), '5 min ago', 'vor 5 min'],
      ['hours', ago(2 * 3_600_000), '2 h ago', 'vor 2 h'],
      ['days', at(10, 7, 12, 0), '9 d ago', 'vor 9 T'],
    ])('renders %s', (_label, iso, english, german) => {
      expect(formatTime(iso, 'relative', 'en', NOW)).toBe(english);
      expect(formatTime(iso, 'relative', 'de', NOW)).toBe(german);
    });

    it('renders a moment earlier today in the unit that distance has', () => {
      expect(formatTime(at(10, 16, 11, 25), 'relative', 'en', NOW)).toBe('35 min ago');
      expect(formatTime(at(10, 16, 11, 25), 'relative', 'de', NOW)).toBe('vor 35 min');
    });
  });

  describe('short', () => {
    it.each([
      ['today', at(10, 16, 11, 25), 'today 11:25', 'heute 11:25'],
      ['yesterday', at(10, 15, 18, 40), 'yesterday 18:40', 'gestern 18:40'],
      ['within seven days', at(10, 13, 18, 40), 'Tue 18:40', 'Di 18:40'],
      ['before that', at(8, 12, 11, 25), '12 Aug, 11:25', '12. Aug., 11:25'],
    ])('renders %s', (_label, iso, english, german) => {
      expect(formatTime(iso, 'short', 'en', NOW)).toBe(english);
      expect(formatTime(iso, 'short', 'de', NOW)).toBe(german);
    });
  });

  describe('clock', () => {
    it('renders the moment to the second in both languages', () => {
      expect(formatTime(at(10, 16, 11, 25, 3), 'clock', 'en', NOW)).toBe('11:25:03');
      expect(formatTime(at(10, 16, 11, 25, 3), 'clock', 'de', NOW)).toBe('11:25:03');
    });
  });

  it('renders nothing for a missing or unreadable value', () => {
    expect(formatTime(null, 'short')).toBe('');
    expect(formatTime(undefined, 'relative')).toBe('');
    expect(formatTime('not a date', 'clock')).toBe('');
  });

  describe('formatDay', () => {
    it.each([
      ['today', at(10, 16, 11, 25), 'today', 'heute'],
      ['yesterday', at(10, 15, 18, 40), 'yesterday', 'gestern'],
      ['before that', at(8, 12, 11, 25), '12 Aug', '12. Aug.'],
    ])('renders %s', (_label, iso, english, german) => {
      expect(formatDay(iso, 'en', NOW)).toBe(english);
      expect(formatDay(iso, 'de', NOW)).toBe(german);
    });
  });
});

describe('formatDuration', () => {
  it.each([
    ['under a minute', 30_000, '30 s', '30 s'],
    ['under an hour', 25 * 60_000, '25 min', '25 min'],
    ['under two days', 130 * 60_000, '2 h 10 min', '2 h 10 min'],
    ['two days or more', (9 * 24 + 1) * 3_600_000, '9 d 1 h', '9 T 1 h'],
  ])('renders %s', (_label, ms, english, german) => {
    expect(formatDuration(ms, 'en')).toBe(english);
    expect(formatDuration(ms, 'de')).toBe(german);
  });

  it('renders zero below the first second of a span', () => {
    expect(formatDuration(0, 'en')).toBe('0 s');
    expect(formatDuration(-500, 'en')).toBe('0 s');
  });
});

describe('formatTokens', () => {
  it.each([
    ['below one thousand', 412, false, '412', '412'],
    ['thousands with one decimal', 4200, false, '4.2 k', '4,2 Tsd.'],
    ['whole thousands', 830_000, false, '830 k', '830 Tsd.'],
    ['millions with two decimals', 1_240_000, false, '1.24 M', '1,24 Mio.'],
  ])('renders %s', (_label, count, floor, english, german) => {
    expect(formatTokens(count, floor, 'en')).toBe(english);
    expect(formatTokens(count, floor, 'de')).toBe(german);
  });

  it('rounds a floor down at the precision shown', () => {
    expect(formatTokens(412_680, true, 'en')).toBe('≥ 412 k');
    expect(formatTokens(4_280, true, 'en')).toBe('≥ 4.2 k');
    expect(formatTokens(1_249_000, true, 'en')).toBe('≥ 1.24 M');
  });

  it('renders zero for no count', () => {
    expect(formatTokens(0, false, 'en')).toBe('0');
  });
});

describe('formatSplit', () => {
  it('takes the unit of its total, in both languages', () => {
    expect(formatSplit(412_000, 380_000, 32_000, true, 'en')).toBe(
      '≥ 412 k (≥ 380 k in · ≥ 32 k out)',
    );
    expect(formatSplit(412_000, 380_000, 32_000, false, 'de')).toBe(
      '412 Tsd. (380 Tsd. rein · 32 Tsd. raus)',
    );
  });

  it('shows every part in the total’s unit, even a part far below it', () => {
    expect(formatSplit(1002, 998, 4, false, 'en')).toBe('1.0 k (1.0 k in · 0.0 k out)');
  });

  it('shows plain numbers when the total is below a thousand', () => {
    expect(formatSplit(412, 380, 32, false, 'en')).toBe('412 (380 in · 32 out)');
  });
});

describe('formatBytes', () => {
  it.each([
    [512, '512 B'],
    [2048, '2 KB'],
    [412 * 1024 * 1024, '412 MB'],
    [1825361100, '1.7 GB'],
    [1932735283, '1.8 GB'],
    [1073741824, '1 GB'],
    [38 * 1024 * 1024 * 1024, '38 GB'],
  ])('renders %i bytes', (bytes, expected) => {
    expect(formatBytes(bytes)).toBe(expected);
  });
});

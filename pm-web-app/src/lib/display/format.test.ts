import { describe, it, expect, beforeEach } from 'vitest';
import { currentLocale, messagesStore } from '../i18n';
import { loadMessages } from '../../test/catalogue-fixture';
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

/**
 * Puts one catalogue in place for a test.
 *
 * The words and the unit marks of a time are catalogue texts (`common.time.*`), so a test that
 * pins them proves the pack is read: an override changes what the formatter says, which a word
 * written in code could never follow.
 *
 * @param locale the interface locale
 * @param overrides the entries this test changes
 */
function useCatalogue(locale: 'de' | 'en', overrides: Record<string, string> = {}): void {
  currentLocale.set(locale);
  messagesStore.set({ ...loadMessages(locale), ...overrides });
}

/**
 * Renders one value in both interface languages, each against its own catalogue.
 *
 * The formatter reads the active catalogue, so a test asks for a locale only after putting that
 * locale's pack in place.
 *
 * @param render the formatter call, given the locale
 * @param english the text the English pack yields
 * @param german the text the German pack yields
 */
function expectBothLocales(
  render: (locale: 'de' | 'en') => string,
  english: string,
  german: string,
): void {
  useCatalogue('en');
  expect(render('en')).toBe(english);
  useCatalogue('de');
  expect(render('de')).toBe(german);
}

describe('formatTime', () => {
  beforeEach(() => {
    useCatalogue('en');
  });

  describe('relative', () => {
    it.each([
      ['days', at(10, 7, 12, 0), '9 d ago', 'vor 9 T'],
      ['months', at(7, 16, 12, 0), '3 months ago', 'vor 3 Monaten'],
    ])('renders %s', (_label, iso, english, german) => {
      expectBothLocales((locale) => formatTime(iso, 'relative', locale, NOW), english, german);
    });

    it('renders a moment of today with the since form the design names', () => {
      expectBothLocales(
        (locale) => formatTime(at(10, 16, 11, 25), 'relative', locale, NOW),
        'since 11:25',
        'seit 11:25',
      );
    });

    it('renders the age in the unit that distance has when the moment is not today', () => {
      // The same distances, read across midnight: not today, so the age applies.
      const justAfterMidnight = new Date(2026, 9, 17, 0, 0, 30);
      expectBothLocales(
        (locale) => formatTime(at(10, 16, 23, 59, 50), 'relative', locale, justAfterMidnight),
        'just now',
        'gerade eben',
      );
      const halfAnHourLater = new Date(2026, 9, 17, 0, 30, 0);
      expectBothLocales(
        (locale) => formatTime(at(10, 16, 23, 55), 'relative', locale, halfAnHourLater),
        '35 min ago',
        'vor 35 min',
      );
      expectBothLocales(
        (locale) => formatTime(at(10, 16, 22, 30), 'relative', locale, halfAnHourLater),
        '2 h ago',
        'vor 2 h',
      );
    });
  });

  describe('short', () => {
    it.each([
      ['today', at(10, 16, 11, 25), 'today 11:25', 'heute 11:25'],
      ['yesterday', at(10, 15, 18, 40), 'yesterday 18:40', 'gestern 18:40'],
      ['within seven days', at(10, 13, 18, 40), 'Tue 18:40', 'Di 18:40'],
      ['before that', at(8, 12, 11, 25), '12 Aug, 11:25', '12. Aug., 11:25'],
    ])('renders %s', (_label, iso, english, german) => {
      expectBothLocales((locale) => formatTime(iso, 'short', locale, NOW), english, german);
    });
  });

  describe('clock', () => {
    it('renders the moment to the second in both languages', () => {
      expectBothLocales(
        (locale) => formatTime(at(10, 16, 11, 25, 3), 'clock', locale, NOW),
        '11:25:03',
        '11:25:03',
      );
    });
  });

  it('renders nothing for a missing or unreadable value', () => {
    expect(formatTime(null, 'short')).toBe('');
    expect(formatTime(undefined, 'relative')).toBe('');
    expect(formatTime('not a date', 'clock')).toBe('');
  });

  describe('the words come from the catalogue', () => {
    it('takes the framing word of an age from the pack', () => {
      useCatalogue('en', { 'common.time.days': '{count} days back' });
      expect(formatTime(at(10, 7, 12, 0), 'relative', 'en', NOW)).toBe('9 days back');
    });

    it('takes the since word from the pack', () => {
      useCatalogue('de', { 'common.time.since': 'seit {time} Uhr' });
      expect(formatTime(at(10, 16, 11, 25), 'relative', 'de', NOW)).toBe('seit 11:25 Uhr');
    });

    it('takes the day words from the pack', () => {
      useCatalogue('en', { 'common.time.today': 'this very day' });
      expect(formatDay(at(10, 16, 11, 25), 'en', NOW)).toBe('this very day');
    });
  });

  describe('formatDay', () => {
    it.each([
      ['today', at(10, 16, 11, 25), 'today', 'heute'],
      ['yesterday', at(10, 15, 18, 40), 'yesterday', 'gestern'],
      ['before that', at(8, 12, 11, 25), '12 Aug', '12. Aug.'],
    ])('renders %s', (_label, iso, english, german) => {
      expectBothLocales((locale) => formatDay(iso, locale, NOW), english, german);
    });

    it('opens the day of a row that carries only a clock', () => {
      expectBothLocales((locale) => formatDay('14:02:11', locale, NOW), 'today', 'heute');
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
    expectBothLocales((locale) => formatDuration(ms, locale), english, german);
  });

  it('takes the unit marks from the catalogue', () => {
    useCatalogue('de', { 'common.time.unit.minute': '{count} Min.' });
    expect(formatDuration(25 * 60_000, 'de')).toBe('25 Min.');
  });

  it('renders zero below the first second of a span', () => {
    expectBothLocales((locale) => formatDuration(0, locale), '0 s', '0 s');
    expectBothLocales((locale) => formatDuration(-500, locale), '0 s', '0 s');
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

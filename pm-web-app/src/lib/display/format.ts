/**
 * The one format for time, duration, tokens and bytes of CC-P-04, following
 * `doc/design/shared/texts-and-languages.adoc` § Time and Duration and § Numbers.
 *
 * Every view reads a moment, a span or a count through this module, so no view writes a second
 * format of its own and no ISO string reaches the operator.
 */

import type { Locale } from '../i18n';
import { translate } from '../i18n';

/** The interface languages the app ships; the one closed set, owned by the catalogue layer. */
export type { Locale };

/** The three renderings of a moment the design defines. */
export type TimeStyle = 'relative' | 'short' | 'clock';

/**
 * A bare clock as the runtime sends it: `14:02` or `14:02:11`, without a date.
 *
 * The activity stream may carry a clock alone, and a clock is the day it was read.
 */
const CLOCK_ONLY = /^(\d{1,2}):(\d{2})(?::(\d{2}))?$/;

/** The shape of a value that claims to be a clock, whether or not the clock can exist. */
const CLOCK_SHAPED = /^\d{1,2}:\d{2}/;

/** The parts of a bare clock. */
export interface ClockParts {
  /** The hour of the day, 0 to 23. */
  hours: number;
  /** The minute of the hour, 0 to 59. */
  minutes: number;
  /** The second of the minute, 0 to 59; `:00` when the clock carries none. */
  seconds: number;
}

/**
 * Reads a bare clock a row carries instead of an instant.
 *
 * A clock that cannot exist (`25:99`) is not a clock: nothing is built from it, so no constructed
 * instant reaches a screen.
 *
 * @param timestamp the value a row carries
 * @returns the clock's parts, or `null` when the value is not a clock the runtime could have sent
 */
export function clockParts(timestamp: string | null | undefined): ClockParts | null {
  const match = CLOCK_ONLY.exec((timestamp ?? '').trim());
  if (!match) return null;
  const parts = {
    hours: Number(match[1]),
    minutes: Number(match[2]),
    seconds: Number(match[3] ?? '0'),
  };
  if (parts.hours > 23 || parts.minutes > 59 || parts.seconds > 59) return null;
  return parts;
}

/**
 * @param locale the locale whose words are wanted
 * @param key the catalogue key of the word or the unit mark
 * @param params the ICU parameters of the text
 * @returns the text the catalogue of that locale yields for the key
 */
function word(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  return translate(locale, key, params);
}

/** The language tag `Intl` formats with; day-first for English, as the design's examples are. */
function languageTag(locale: string): string {
  return locale.startsWith('de') ? 'de-DE' : 'en-GB';
}

/**
 * @param iso the runtime's instant @returns the date, or `null` when the value is not an instant
 */
function parseInstant(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** @param date a date @returns midnight of that day, in local time */
function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

/** @param date a date @param locale the interface locale @returns the local wall time, `14:02` */
function clockText(date: Date, locale: string): string {
  return new Intl.DateTimeFormat(languageTag(locale), {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(date);
}

/** @param date a date @param locale the interface locale @returns the local wall time to the second */
function secondText(date: Date, locale: string): string {
  return new Intl.DateTimeFormat(languageTag(locale), {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).format(date);
}

/** @param date a date @param locale the interface locale @returns the short weekday, `Mon` or `Mo` */
function weekdayText(date: Date, locale: string): string {
  return new Intl.DateTimeFormat(languageTag(locale), { weekday: 'short' }).format(date);
}

/** @param date a date @param locale the interface locale @returns day and month, `16 Oct` */
function dayMonthText(date: Date, locale: string): string {
  return new Intl.DateTimeFormat(languageTag(locale), {
    day: 'numeric',
    month: 'short',
  }).format(date);
}

/**
 * @param ms the distance to now, in milliseconds
 * @param locale the interface locale
 * @returns the age in the unit that distance has
 */
function ageText(ms: number, locale: Locale): string {
  if (ms < 60_000) return word(locale, 'common.time.just_now');
  const minutes = Math.floor(ms / 60_000);
  if (minutes < 60) return word(locale, 'common.time.minutes', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return word(locale, 'common.time.hours', { count: hours });
  const days = Math.floor(hours / 24);
  if (days < 30) return word(locale, 'common.time.days', { count: days });
  // Months and years take the language's own words: the design fixes only the unit marks of
  // minutes, hours and days in the packs.
  const relative = new Intl.RelativeTimeFormat(languageTag(locale), { numeric: 'auto' });
  if (days < 365) return relative.format(-Math.floor(days / 30), 'month');
  return relative.format(-Math.floor(days / 365), 'year');
}

/**
 * Dates a row the runtime sends with a clock only.
 *
 * The wire may carry `14:02:11` instead of an instant; such a row belongs to the day it was read,
 * so the row takes that day with the clock it sent. The day is thus fixed when the row arrives,
 * and a page left open over midnight keeps its rows where they belong.
 *
 * @param timestamp the instant or the bare clock the runtime sends
 * @param readAt the moment the row was read
 * @returns the timestamp with a date, the value unchanged when it already carries one, and an
 *   empty string for a value shaped like a clock that cannot exist — no constructed instant ever
 *   reaches a row
 */
export function dateOnRead(timestamp: string, readAt: Date): string {
  const clock = clockParts(timestamp);
  if (!clock) {
    return CLOCK_SHAPED.test((timestamp ?? '').trim()) ? '' : timestamp;
  }
  const pad = (value: number): string => String(value).padStart(2, '0');
  const day = `${readAt.getFullYear()}-${pad(readAt.getMonth() + 1)}-${pad(readAt.getDate())}`;
  return `${day}T${pad(clock.hours)}:${pad(clock.minutes)}:${pad(clock.seconds)}`;
}

/**
 * Renders a moment in one of the three styles the design defines.
 *
 * `relative` is a moment of today as `since 14:02` and any older moment as the distance to now in
 * the unit that distance has (the age a list shows); `short` is the moment itself to the minute;
 * `clock` is the moment to the second, for an activity stream. The words that frame the figure
 * (`today`, `since`, `ago`) and the unit marks are catalogue texts of `common.time.*`.
 *
 * @param iso the instant the runtime sends
 * @param style the rendering the view needs
 * @param locale the interface locale
 * @param now the instant "now" means, so a test can pin it
 * @returns the rendering, or an empty string when the value is not an instant
 */
export function formatTime(
  iso: string | null | undefined,
  style: TimeStyle,
  locale: Locale = 'en',
  now: Date = new Date(),
): string {
  const date = parseInstant(iso);
  if (!date) return '';

  if (style === 'clock') {
    return secondText(date, locale);
  }
  const distance = dayDistance(now, date);
  if (style === 'short') {
    return shortText(date, locale, distance);
  }
  if (distance === 0 && date.getTime() <= now.getTime()) {
    return word(locale, 'common.time.since', { time: clockText(date, locale) });
  }
  return ageText(Math.max(0, now.getTime() - date.getTime()), locale);
}

/**
 * @param now the instant "now" means @param date the moment @returns the calendar days between
 */
function dayDistance(now: Date, date: Date): number {
  return Math.round((startOfDay(now).getTime() - startOfDay(date).getTime()) / 86_400_000);
}

/**
 * @param date the moment @param locale the interface locale @param distance the calendar days
 *   between today and the moment @returns the moment to the minute, `today 14:02`
 */
function shortText(date: Date, locale: Locale, distance: number): string {
  const time = clockText(date, locale);
  if (distance === 0) return `${word(locale, 'common.time.today')} ${time}`;
  if (distance === 1) return `${word(locale, 'common.time.yesterday')} ${time}`;
  if (distance > 1 && distance < 7) return `${weekdayText(date, locale)} ${time}`;
  return `${dayMonthText(date, locale)}, ${time}`;
}

/**
 * The calendar day a runtime timestamp belongs to, for the header of a group of rows.
 *
 * A row may carry a bare clock instead of an instant (`14:02:11`): a clock is read from the live
 * stream, so it belongs to the day it was read.
 *
 * @param timestamp the instant or the bare clock the runtime sends
 * @param now the instant "now" means, so a test can pin it
 * @returns midnight of that day, in local time, or `null` when the value carries neither
 */
export function dayOf(
  timestamp: string | null | undefined,
  now: Date = new Date(),
): Date | null {
  const moment = parseInstant(timestamp) ?? (clockParts(timestamp) ? now : null);
  return moment === null ? null : startOfDay(moment);
}

/**
 * Renders the day a group of activity rows belongs to, for the header of that group.
 *
 * It is the date part of the `short` style: `today` / `heute`, `yesterday` / `gestern`, and the
 * day and month (`16 Oct` / `16. Okt.`) for anything older. The rows below it carry the `clock`
 * rendering of their own moments.
 *
 * @param iso the instant or the bare clock the runtime sends
 * @param locale the interface locale
 * @param now the instant "now" means, so a test can pin it
 * @returns the day label, or an empty string when the value carries neither a date nor a clock
 */
export function formatDay(
  iso: string | null | undefined,
  locale: Locale = 'en',
  now: Date = new Date(),
): string {
  const day = dayOf(iso, now);
  if (!day) return '';
  const distance = dayDistance(now, day);
  if (distance === 0) return word(locale, 'common.time.today');
  if (distance === 1) return word(locale, 'common.time.yesterday');
  return dayMonthText(day, locale);
}

/**
 * Renders a span as the design's three duration rows.
 *
 * The unit marks (`s`, `min`, `h`, and `d` / `T`) are catalogue texts (`common.time.unit.*`), so
 * no view writes a second format of its own.
 *
 * @param ms the span in milliseconds
 * @param locale the interface locale
 * @returns `30 s`, `25 min`, `2 h 10 min`, or `9 d 1 h` (`9 T 1 h` in German)
 */
export function formatDuration(ms: number, locale: Locale = 'en'): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  if (totalSeconds < 60) return word(locale, 'common.time.unit.second', { count: totalSeconds });
  const minutes = Math.floor(totalSeconds / 60);
  if (minutes < 60) return word(locale, 'common.time.unit.minute', { count: minutes });
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    const rest = minutes % 60;
    return rest > 0
      ? word(locale, 'common.time.unit.hour_minute', { hours, minutes: rest })
      : word(locale, 'common.time.unit.hour', { count: hours });
  }
  const days = Math.floor(hours / 24);
  const restHours = hours % 24;
  return restHours > 0
    ? word(locale, 'common.time.unit.day_hour', { days, hours: restHours })
    : word(locale, 'common.time.unit.day', { count: days });
}

/**
 * @param value a rounded figure @param digits the decimals to show @param locale the interface
 *   locale @returns the figure with the language's decimal separator, without grouping
 */
function decimalText(value: number, digits: number, locale: string): string {
  return new Intl.NumberFormat(languageTag(locale), {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
    useGrouping: false,
  }).format(value);
}

/**
 * Renders a count of tokens as the design's numbers table says.
 *
 * @param count the exact count the runtime sends
 * @param floor whether the figure is a floor over partly measured records
 * @param locale the interface locale
 * @returns `412`, `4.2 k`, `830 k`, `1.24 M`, or the floor form `≥ 412 k`
 */
export function formatTokens(count: number, floor = false, locale: Locale = 'en'): string {
  return renderTokens(count, tokenUnit(count), floor, locale);
}

/** The unit a count of tokens is shown in, or `null` for a count below a thousand. */
interface TokenUnit {
  /** The divisor from a token count to the shown figure. */
  factor: number;
  /** The catalogue key that fixes the unit mark, with `{value}` for the figure. */
  key: string;
  /** The decimals the unit shows. */
  digits: number;
}

/**
 * @param count the exact count @returns the unit it is shown in
 */
function tokenUnit(count: number): TokenUnit | null {
  if (count < 1000) return null;
  if (count < 10_000) {
    return { factor: 1000, key: 'common.tokens.thousands', digits: 1 };
  }
  if (count < 1_000_000) {
    return { factor: 1000, key: 'common.tokens.thousands', digits: 0 };
  }
  return { factor: 1_000_000, key: 'common.tokens.millions', digits: 2 };
}

/**
 * @param count the exact count @param unit the unit to show it in @param floor whether it is a
 *   floor @param locale the interface locale @returns the figure with its unit
 */
function renderTokens(
  count: number,
  unit: TokenUnit | null,
  floor: boolean,
  locale: Locale,
): string {
  const figure = unit
    ? word(locale, unit.key, {
        value: decimalText(
          floor
            ? Math.floor((count / unit.factor) * 10 ** unit.digits) / 10 ** unit.digits
            : Math.round((count / unit.factor) * 10 ** unit.digits) / 10 ** unit.digits,
          unit.digits,
          locale,
        ),
      })
    : String(Math.max(0, count));
  return floor ? word(locale, 'common.tokens.at_least', { value: figure }) : figure;
}

/**
 * Renders the total and its input/output split in the unit of the total.
 *
 * The parts are shown in the total's own unit, so that they read against it: a total of `1.0 k`
 * shows its parts as `1.0 k in · 0.0 k out`, never as `998 in · 4 out`.
 *
 * @param total the total count
 * @param input the input, cache-read and cache-write tokens
 * @param output the output and reasoning tokens
 * @param floor whether the figures are floors
 * @param locale the interface locale
 * @returns `412 k (380 k in · 32 k out)`, `412 Tsd. (380 Tsd. rein · 32 Tsd. raus)`
 */
export function formatSplit(
  total: number,
  input: number,
  output: number,
  floor = false,
  locale: Locale = 'en',
): string {
  const unit = tokenUnit(total);
  return word(locale, 'common.tokens.split', {
    total: renderTokens(total, unit, floor, locale),
    in: renderTokens(input, unit, floor, locale),
    out: renderTokens(output, unit, floor, locale),
  });
}

/**
 * Renders a byte count.
 *
 * @param bytes the exact count
 * @returns `512 B`, `2 KB`, `412 MB`, `1.7 GB`
 */
export function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${Math.round(kb)} KB`;
  }
  const mb = kb / 1024;
  if (mb < 1024) {
    return `${Math.round(mb)} MB`;
  }
  const gb = mb / 1024;
  if (gb >= 10) {
    return `${Math.round(gb)} GB`;
  }
  const rounded = Math.round(gb * 10) / 10;
  if (rounded === Math.floor(rounded)) {
    return `${rounded} GB`;
  }
  return `${rounded.toFixed(1)} GB`;
}

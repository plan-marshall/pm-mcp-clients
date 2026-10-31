/**
 * The one format for time, duration, tokens and bytes of CC-P-04, following
 * `doc/design/shared/texts-and-languages.adoc` § Time and Duration and § Numbers.
 *
 * Every view reads a moment, a span or a count through this module, so no view writes a second
 * format of its own and no ISO string reaches the operator.
 */

import type { Locale } from '../i18n';

/** The interface languages the app ships; the one closed set, owned by the catalogue layer. */
export type { Locale };

/** The three renderings of a moment the design defines. */
export type TimeStyle = 'relative' | 'short' | 'clock';

/** The language tag `Intl` formats with; day-first for English, as the design's examples are. */
function languageTag(locale: string): string {
  return locale.startsWith('de') ? 'de-DE' : 'en-GB';
}

/** @param locale the interface locale @returns whether it is German */
function isGerman(locale: string): boolean {
  return locale.startsWith('de');
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
function ageText(ms: number, locale: string): string {
  const german = isGerman(locale);
  if (ms < 60_000) return german ? 'gerade eben' : 'just now';
  const minutes = Math.floor(ms / 60_000);
  if (minutes < 60) return german ? `vor ${minutes} min` : `${minutes} min ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return german ? `vor ${hours} h` : `${hours} h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return german ? `vor ${days} T` : `${days} d ago`;
  // Months and years take the language's own words: the design fixes only the unit marks of
  // minutes, hours and days in the packs.
  const relative = new Intl.RelativeTimeFormat(languageTag(locale), { numeric: 'auto' });
  if (days < 365) return relative.format(-Math.floor(days / 30), 'month');
  return relative.format(-Math.floor(days / 365), 'year');
}

/**
 * Renders a moment in one of the three styles the design defines.
 *
 * `relative` is the distance to now in the unit that distance has (the age a list shows); `short`
 * is the moment itself to the minute; `clock` is the moment to the second, for an activity stream.
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
  if (style === 'short') {
    return shortText(date, locale, dayDistance(now, date));
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
function shortText(date: Date, locale: string, distance: number): string {
  const german = isGerman(locale);
  const time = clockText(date, locale);
  if (distance === 0) return `${german ? 'heute' : 'today'} ${time}`;
  if (distance === 1) return `${german ? 'gestern' : 'yesterday'} ${time}`;
  if (distance > 1 && distance < 7) return `${weekdayText(date, locale)} ${time}`;
  return `${dayMonthText(date, locale)}, ${time}`;
}

/**
 * Renders the day a group of activity rows belongs to, for the header of that group.
 *
 * It is the date part of the `short` style: `today` / `heute`, `yesterday` / `gestern`, and the
 * day and month (`16 Oct` / `16. Okt.`) for anything older. The rows below it carry the `clock`
 * rendering of their own moments.
 *
 * @param iso the instant the runtime sends
 * @param locale the interface locale
 * @param now the instant "now" means, so a test can pin it
 * @returns the day label, or an empty string when the value is not an instant
 */
export function formatDay(
  iso: string | null | undefined,
  locale: Locale = 'en',
  now: Date = new Date(),
): string {
  const date = parseInstant(iso);
  if (!date) return '';
  const distance = dayDistance(now, date);
  if (distance === 0) return isGerman(locale) ? 'heute' : 'today';
  if (distance === 1) return isGerman(locale) ? 'gestern' : 'yesterday';
  return dayMonthText(date, locale);
}

/**
 * Renders a span as the design's three duration rows.
 *
 * @param ms the span in milliseconds
 * @param locale the interface locale
 * @returns `30 s`, `25 min`, `2 h 10 min`, or `9 d 1 h` (`9 T 1 h` in German)
 */
export function formatDuration(ms: number, locale: Locale = 'en'): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  if (totalSeconds < 60) return `${totalSeconds} s`;
  const minutes = Math.floor(totalSeconds / 60);
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    const rest = minutes % 60;
    return rest > 0 ? `${hours} h ${rest} min` : `${hours} h`;
  }
  const days = Math.floor(hours / 24);
  const restHours = hours % 24;
  const dayUnit = isGerman(locale) ? 'T' : 'd';
  return restHours > 0 ? `${days} ${dayUnit} ${restHours} h` : `${days} ${dayUnit}`;
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
  if (count <= 0) return '0';

  const german = isGerman(locale);
  const thousands = german ? 'Tsd.' : 'k';
  const millions = german ? 'Mio.' : 'M';
  const round = (value: number, factor: number): number =>
    floor ? Math.floor(value * factor) / factor : Math.round(value * factor) / factor;

  let formatted: string;
  if (count < 1000) {
    formatted = String(count);
  } else if (count < 10_000) {
    formatted = `${decimalText(round(count / 1000, 10), 1, locale)} ${thousands}`;
  } else if (count < 1_000_000) {
    formatted = `${decimalText(round(count / 1000, 1), 0, locale)} ${thousands}`;
  } else {
    formatted = `${decimalText(round(count / 1_000_000, 100), 2, locale)} ${millions}`;
  }

  return floor ? `≥ ${formatted}` : formatted;
}

/**
 * Renders the total and its input/output split in the unit of the total.
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
  const german = isGerman(locale);
  const inLabel = german ? 'rein' : 'in';
  const outLabel = german ? 'raus' : 'out';
  return `${formatTokens(total, floor, locale)} (${formatTokens(input, floor, locale)} ${inLabel} · ${formatTokens(output, floor, locale)} ${outLabel})`;
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

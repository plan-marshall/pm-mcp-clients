import { writable, derived } from 'svelte/store';
import { en } from './messages/en';
import { de } from './messages/de';

export type Locale = 'en' | 'de';

const messages: Record<Locale, Record<string, string>> = {
  en,
  de,
};

export const currentLocale = writable<Locale>('en');

export function setLocale(locale: Locale): void {
  currentLocale.set(locale);
}

export function formatString(template: string, params?: Record<string, string | number>): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (match, key) => {
    if (key in params) {
      return String(params[key]);
    }
    return match;
  });
}

export function translate(
  locale: Locale,
  key: string,
  params?: Record<string, string | number>,
): string {
  const pack = messages[locale] || messages.en;
  const template = pack[key] || messages.en[key] || key;
  return formatString(template, params);
}

export const t = derived(
  currentLocale,
  ($locale) =>
    (key: string, params?: Record<string, string | number>): string =>
      translate($locale, key, params),
);

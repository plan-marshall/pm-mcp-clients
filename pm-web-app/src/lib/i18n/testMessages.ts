import fs from 'node:fs';
import path from 'node:path';

/**
 * The message packs of `pm-api`, read for the view tests.
 *
 * The tests assert the texts the catalogue carries, so they load the same properties files the
 * runtime serves instead of keeping a second copy of the strings.
 */

const MESSAGES_DIR = path.resolve(__dirname, '../../../../pm-api/src/main/resources/messages');

function decodePropertiesValue(value: string): string {
  // Java properties escape non-ASCII as \uXXXX.
  return value.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex: string) =>
    String.fromCharCode(parseInt(hex, 16)),
  );
}

/**
 * @param filePath the properties file
 * @returns its key/value pairs, with the escapes decoded
 */
export function loadPropertiesFile(filePath: string): Record<string, string> {
  const content = fs.readFileSync(filePath, 'utf-8');
  const result: Record<string, string> = {};
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith('!')) continue;
    const eqIdx = trimmed.indexOf('=');
    if (eqIdx > 0) {
      const key = trimmed.slice(0, eqIdx).trim();
      result[key] = decodePropertiesValue(trimmed.slice(eqIdx + 1).trim());
    }
  }
  return result;
}

/**
 * @param locale the locale, `en` or `de`
 * @returns every message of that locale, across the areas
 */
export function loadMessages(locale: string): Record<string, string> {
  if (!fs.existsSync(MESSAGES_DIR)) {
    return {};
  }
  const suffix = `.${locale}.properties`;
  const all: Record<string, string> = {};
  for (const file of fs.readdirSync(MESSAGES_DIR)) {
    if (file.endsWith(suffix)) {
      Object.assign(all, loadPropertiesFile(path.join(MESSAGES_DIR, file)));
    }
  }
  return all;
}

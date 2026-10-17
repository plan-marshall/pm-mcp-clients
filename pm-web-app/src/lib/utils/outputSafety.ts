/**
 * Output safety utility for stripping ANSI escape sequences and non-printable control characters.
 */

/* eslint-disable no-control-regex */
export function stripControlSequences(input?: string | null): string {
  if (!input) return '';
  return input
    .replace(/\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])/g, '')
    .replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
}

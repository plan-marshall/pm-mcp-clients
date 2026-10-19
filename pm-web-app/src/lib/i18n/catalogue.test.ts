import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { loadMessages } from './testMessages';

/**
 * The catalogue guard: every user-facing text of the onboarding, the entry and the machine
 * settings is loaded from the message catalogue, in English and in German.
 *
 * The check reads the view and component sources and asserts that no user-facing literal is
 * written there: a text node with more than a symbol in it, a `title`/`placeholder`/`aria-label`
 * string, or a button label, must come from `$t(...)`.
 */

const SOURCE_DIR = path.resolve(__dirname, '..');

/** The sources this package owns. */
const SCREEN_SOURCES = [
  'views/OnboardingView.svelte',
  'views/PairingView.svelte',
  'views/MachineSettingsView.svelte',
  'views/SettingsView.svelte',
  'views/ListenerOffView.svelte',
  'components/ConsentBox.svelte',
  'components/LanguageChoice.svelte',
  'components/HarnessChoice.svelte',
  'components/ModelsChoice.svelte',
  'components/AspectChoice.svelte',
  'components/PairingQr.svelte',
  'components/StepBar.svelte',
];

/**
 * Texts that are not UI copy: they are wire values, ids, or the punctuation of a layout.
 */
const ALLOWED_LITERALS = new Set([
  'en',
  'de',
  'same',
  'copy',
  'move',
  'ask',
  'roles',
  'store',
  'providers',
  'defaults',
  'light',
  'dark',
  'on',
  'off',
  'none',
  'complete',
  'partial',
  'not_reported',
  'button',
  'submit',
  'reset',
  'primary',
  'secondary',
  'danger',
  'ghost',
  'dialog',
  'alert',
  'status',
  'img',
  'text',
  'password',
  'number',
  'radio',
  'checkbox',
  'numeric',
  'short',
  '2-digit',
  'multipart',
]);

function readSource(relativePath: string): string {
  return fs.readFileSync(path.join(SOURCE_DIR, relativePath), 'utf-8');
}

/** Strips the script block, so only the markup's own text is inspected. */
function markupOf(source: string): string {
  return source.replace(/<script[\s\S]*?<\/script>/g, '').replace(/<style[\s\S]*?<\/style>/g, '');
}

/**
 * @param source the markup
 * @returns the text nodes and attribute values that are literal copy
 */
function literalTexts(source: string): string[] {
  const found: string[] = [];

  // A text node between tags that carries more than punctuation or a single symbol.
  const textNodes = source.match(/>([^<>{}]+)</g) ?? [];
  for (const node of textNodes) {
    const text = node.slice(1, -1).trim();
    if (!text) continue;
    if (!/[A-Za-z]{3,}/.test(text)) continue;
    if (text.startsWith('{') || text.endsWith('}')) continue;
    found.push(text);
  }

  // User-facing attributes.
  const attributes = source.matchAll(
    /\b(title|placeholder|aria-label|alt|label|summary|legend)\s*=\s*"([^"]*)"/g,
  );
  for (const match of attributes) {
    const value = match[2].trim();
    if (!value || value.includes('{') || value.startsWith('$t(')) continue;
    if (!/[A-Za-z]{2,}/.test(value)) continue;
    if (ALLOWED_LITERALS.has(value)) continue;
    found.push(`${match[1]}="${value}"`);
  }

  return found;
}

describe('the catalogue carries every text of the package', () => {
  it('has the same keys in English and in German', () => {
    const en = loadMessages('en');
    const de = loadMessages('de');

    expect(Object.keys(en).length).toBeGreaterThan(0);
    const missingInDe = Object.keys(en).filter((key) => !(key in de));
    const missingInEn = Object.keys(de).filter((key) => !(key in en));

    expect(missingInDe).toEqual([]);
    expect(missingInEn).toEqual([]);
  });

  it('carries the keys this package renders', () => {
    const en = loadMessages('en');
    const required = [
      'onboard.entry.title',
      'onboard.entry.intro',
      'onboard.entry.step.language',
      'onboard.entry.step.plans',
      'onboard.mode.auto',
      'onboard.mode.guided',
      'onboard.language.interface_label',
      'onboard.language.model_label',
      'onboard.harness.choose.found',
      'onboard.harness.choose.action',
      'onboard.harness.choose.default_label',
      'onboard.harness.probe.cause',
      'onboard.catalogue.select.action',
      'onboard.roles.review.action',
      'onboard.store.choose.action',
      'onboard.providers.select.action',
      'onboard.defaults.review.action',
      'onboard.auto.result.aspect',
      'onboard.completed.title',
      'onboard.completed.readonly',
      'onboard.completed.show_step',
      'onboard.completed.to_projects',
      'onboard.link.set',
      'onboard.link.accept',
      'web.pair.page.title',
      'web.pair.page.invalid',
      'web.pair.qr.label',
      'web.settings.page.title',
      'web.settings.conflict.title',
      'web.settings.conflict.apply_again',
      'web.settings.conflict.keep',
      'web.settings.credentials.rotate_title',
      'web.settings.credentials.secret',
      'web.settings.devices.loading',
      'web.settings.devices.other' + 's_title',
      'web.entry.lan_off.title',
      'web.entry.lan_off.reason',
    ];
    for (const key of required) {
      expect(en[key], `Key '${key}' must exist in the catalogue`).toBeTruthy();
    }
  });

  it('writes no user-facing literal in the screens', () => {
    const offenders: string[] = [];
    for (const source of SCREEN_SOURCES) {
      for (const literal of literalTexts(markupOf(readSource(source)))) {
        offenders.push(`${source}: ${literal}`);
      }
    }
    expect(offenders).toEqual([]);
  });
});

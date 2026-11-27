import type { MessageText } from './types';

export type ChatTranslator = (key: string, args?: Record<string, string | number>) => string;

function toTranslatorArgs(
  args: Record<string, string | number | boolean> | undefined,
): Record<string, string | number> | undefined {
  if (!args) return undefined;
  return Object.fromEntries(
    Object.entries(args).map(([k, v]) => [k, typeof v === 'boolean' ? String(v) : v]),
  );
}

const LINE_PREFIX = 'chat.line.';
const KNOWS_PREFIX = 'chat.knows.';
const APPLIES_PREFIX = 'chat.applies.';
const STEP_PREFIX = 'chat.step.';

const REASON_KEYS: Record<string, string> = {
  'chat.reason.lost': 'control.chat.lost',
  'chat.reason.too_long': 'control.chat.too_long_reply',
  'chat.reason.infeasible': 'control.chat.infeasible',
};

const FINDING_KEYS: Record<string, string> = {
  'chat.finding.check_failed': 'control.chat.check_failed',
  'chat.finding.changed_since': 'control.chat.changed_since',
  'chat.finding.lapsed': 'control.chat.lapsed',
  'chat.finding.already_queued': 'control.chat.already_queued',
};

/**
 * Resolves a wire `MessageText` (from `ChatMessageKeys`) to a display string through `t`.
 *
 * @param translate catalogue lookup callback
 * @param message wire key and optional named arguments
 * @returns translated sentence
 */
export function translateChatMessage(translate: ChatTranslator, message: MessageText): string {
  const { key } = message;
  const args = toTranslatorArgs(message.args);
  if (key in REASON_KEYS) {
    return translate(REASON_KEYS[key], args);
  }
  if (key in FINDING_KEYS) {
    return translate(FINDING_KEYS[key], args);
  }
  if (key === 'chat.line.decision_answered' && !args?.device) {
    return translate('control.chat.line.decision_answered_no_device', args);
  }
  if (key.startsWith(LINE_PREFIX)) {
    return translate(`control.${key}`, args);
  }
  if (key.startsWith(KNOWS_PREFIX)) {
    const item = args?.item ?? key.slice(KNOWS_PREFIX.length);
    return translate('control.chat.knows.item', { item: String(item) });
  }
  if (key.startsWith(APPLIES_PREFIX)) {
    const applies = args?.applies ?? key.slice(APPLIES_PREFIX.length);
    return translate('control.chat.revision_queued', { applies: String(applies) });
  }
  if (key.startsWith(STEP_PREFIX)) {
    return translate(`control.${key}`, args);
  }
  return translate(key, args);
}

import { displayError } from '../display';

/**
 * The closed set of error codes the web client raises itself.
 *
 * The daemon's error catalogue (`ApiErrorCode`, `error.*`) names the refusals the runtime makes.
 * The client's own failures — a request that ran out of its time, a machine that could not be
 * reached, a pairing the runtime no longer accepts — are a separate, closed set: their texts live
 * in the `common.error.*` area of the catalogue, and every view reads them through the same
 * `displayError` seam as the daemon's codes.
 */
export const CLIENT_ERROR_CODES = ['timeout', 'network_offline', 'unauthorized'] as const;

/** One code of the client's own closed set. */
export type ClientErrorCode = (typeof CLIENT_ERROR_CODES)[number];

/**
 * A failure the client itself raises, carrying one of {@link CLIENT_ERROR_CODES}.
 *
 * The message is the code, so a caller that logs the error and a caller that reads `code` tell the
 * same story; the code is a typed field so no view has to spell the string.
 */
export class ClientError extends Error {
  /** The code of the client's own closed set. */
  readonly code: ClientErrorCode;

  /**
   * @param code the code of the client's own closed set
   */
  constructor(code: ClientErrorCode) {
    super(code);
    this.name = 'ClientError';
    this.code = code;
  }
}

/**
 * @param value the failure a caller caught
 * @returns whether it is one the client raised itself
 */
export function isClientError(value: unknown): value is ClientError {
  return value instanceof ClientError;
}

/**
 * The shape of a wire error code: a lower-case identifier with underscores (`scope_not_found`,
 * `timeout`). Anything else — a JavaScript message, an HTTP status, prose from a DOMException — is
 * not a code and is reported as unknown, so no view renders prose as a code.
 */
const ERROR_CODE_PATTERN = /^[a-z][a-z0-9_]*$/;

/**
 * Reads the code a failure stands for.
 *
 * A failure the client raised carries its own code; a daemon refusal carries the runtime's wire code
 * in its message (`httpError` keeps it there), which is what `displayError` resolves. An unreadable
 * failure answers the empty string, so the display layer's unknown fallback is drawn instead of a
 * JavaScript error's text.
 *
 * @param value the failure a caller caught
 * @returns the error code, or the empty string when none can be read
 */
export function errorCodeOf(value: unknown): string {
  if (isClientError(value)) {
    return value.code;
  }
  let message = '';
  if (value instanceof Error) {
    message = value.message;
  } else if (typeof value === 'string') {
    message = value;
  }
  const trimmed = message.trim();
  return ERROR_CODE_PATTERN.test(trimmed) ? trimmed : '';
}

/**
 * The one sentence an act's failure shows.
 *
 * An act that fails carries either one of the client's own codes or a daemon refusal, and both name
 * a sentence in the catalogue; that sentence is what the operator reads, never the failure's own
 * message — which is the bare code for a {@link ClientError} and a technical string otherwise. Where
 * no code can be read, the act's own failure text stands in, so an unfamiliar failure still says
 * what failed without printing its message.
 *
 * @param value the failure the act caught
 * @param fallback the act's own failure text, resolved from the view's catalogue key
 * @returns the sentence to show
 */
export function actFailureText(value: unknown, fallback: string): string {
  const code = errorCodeOf(value);
  return code.length > 0 ? displayError(code).fix : fallback;
}

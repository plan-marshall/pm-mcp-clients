import { describe, it, expect } from 'vitest';
import { ClientError, errorCodeOf, isClientError } from './errors';

/**
 * The containment boundary of the display layer: only a wire-shaped code may become a code, so no
 * prose a JavaScript error carries can be rendered as one.
 */
describe('errorCodeOf', () => {
  it('reads the code of a failure the client raised itself', () => {
    expect(errorCodeOf(new ClientError('timeout'))).toBe('timeout');
    expect(errorCodeOf(new ClientError('network_offline'))).toBe('network_offline');
    expect(errorCodeOf(new ClientError('unauthorized'))).toBe('unauthorized');
  });

  it('reads a wire code a runtime refusal carries in its message', () => {
    expect(errorCodeOf(new Error('scope_not_found'))).toBe('scope_not_found');
    expect(errorCodeOf(new Error('invalid_code'))).toBe('invalid_code');
    expect(errorCodeOf('not_withdrawable')).toBe('not_withdrawable');
  });

  it('refuses prose, statuses and a DOMException as codes', () => {
    expect(errorCodeOf(new Error('HTTP 500'))).toBe('');
    expect(errorCodeOf(new Error('Failed to fetch'))).toBe('');
    expect(errorCodeOf(new Error('Invalid request path: //evil'))).toBe('');
    expect(errorCodeOf(new DOMException('The operation timed out.', 'TimeoutError'))).toBe('');
    expect(errorCodeOf('')).toBe('');
    expect(errorCodeOf(null)).toBe('');
    expect(errorCodeOf(undefined)).toBe('');
  });
});

describe('isClientError', () => {
  it('tells a failure the client raised from any other', () => {
    expect(isClientError(new ClientError('timeout'))).toBe(true);
    expect(isClientError(new Error('timeout'))).toBe(false);
    expect(isClientError(null)).toBe(false);
  });
});

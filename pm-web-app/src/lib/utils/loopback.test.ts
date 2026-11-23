import { describe, it, expect, afterEach } from 'vitest';
import { isLoopbackHost, isLoopbackSession } from './loopback';

describe('loopback utilities', () => {
  it('identifies loopback hostnames correctly', () => {
    expect(isLoopbackHost('127.0.0.1')).toBe(true);
    expect(isLoopbackHost('localhost')).toBe(true);
    expect(isLoopbackHost('::1')).toBe(true);
    expect(isLoopbackHost('[::1]')).toBe(true);

    expect(isLoopbackHost('example.com')).toBe(false);
    expect(isLoopbackHost('192.168.1.10')).toBe(false);
    expect(isLoopbackHost('mbp-oliver.local')).toBe(false);
    expect(isLoopbackHost('')).toBe(false);
  });

  describe('isLoopbackSession', () => {
    const originalLocation = window.location;

    afterEach(() => {
      Object.defineProperty(window, 'location', {
        writable: true,
        value: originalLocation,
      });
    });

    it('returns true when hostname is localhost or 127.0.0.1', () => {
      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, hostname: 'localhost' },
      });
      expect(isLoopbackSession()).toBe(true);

      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, hostname: '127.0.0.1' },
      });
      expect(isLoopbackSession()).toBe(true);
    });

    it('returns false when hostname is a network or domain name', () => {
      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, hostname: 'mbp-oliver.local' },
      });
      expect(isLoopbackSession()).toBe(false);

      Object.defineProperty(window, 'location', {
        writable: true,
        value: { ...originalLocation, hostname: '192.168.1.42' },
      });
      expect(isLoopbackSession()).toBe(false);
    });
  });
});

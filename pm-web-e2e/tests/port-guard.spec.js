import { test, expect } from '@playwright/test';
import {
  findPortViolations,
  malformedPortRun,
  strictPortIsBound,
} from '../scripts/guard-ports.js';

/**
 * The port guard: the same checks `npm run test:ports-guard` runs before the suite (AGENTS.md
 * § Ports). A run that names its own port, or reads a malformed one as the default, would test a
 * server nobody assigned.
 */

test.describe('Port Isolation & Parameterization Guard', () => {
  test('no test file below tests/ except ports.js, nor playwright.config.js or global-setup.js contains hardcoded port literals 7420 or 5173', () => {
    const violations = findPortViolations();

    expect(violations, `Found forbidden port literals:\n${violations.join('\n')}`).toEqual([]);
  });

  test('a malformed PM_WEB_PORT stops the run instead of falling back to the default', () => {
    const result = malformedPortRun();

    expect(result.status, 'a port that is not a number must not be read as the default').not.toBe(0);
    expect(result.stderr).toContain('Invalid PM_WEB_PORT');
  });

  for (const value of ['0', '-1', '65536']) {
    test(`a PM_WEB_PORT of ${value} stops the run`, () => {
      const result = malformedPortRun(value);

      // The range check is what this covers: the value is a number, and still no port.
      expect(result.status).not.toBe(0);
      expect(result.stderr).toContain('Invalid PM_WEB_PORT');
    });
  }

  test('holds the assigned web port instead of moving to another one', () => {
    expect(strictPortIsBound()).toBe(true);
  });
});

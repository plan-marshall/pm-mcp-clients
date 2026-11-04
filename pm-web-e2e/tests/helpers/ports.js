/**
 * Centralized port resolution for pm-web-e2e.
 *
 * Reads PM_WEB_PORT (default 5173) and PM_MOCK_WEB_PORT (default 7420).
 * Tests and test helpers must import URLs from here rather than hardcoding port literals.
 *
 * A value that is not a port stops the suite: `Number(env) || default` would turn `abc` into the
 * default and run against a port nobody assigned (AGENTS.md § Ports).
 */

/**
 * @param name the variable's name, for the error
 * @param raw the raw value the environment carries
 * @param fallback the port the variable falls back to when it is unset or empty
 * @returns the resolved port
 * @throws Error when the value is not a port between 1 and 65535
 */
function parsePort(name, raw, fallback) {
  const trimmed = (raw ?? '').trim();
  if (trimmed === '') {
    return fallback;
  }
  if (!/^\d+$/.test(trimmed)) {
    throw new Error(`Invalid ${name}: "${raw}". Expected an integer between 1 and 65535.`);
  }
  const port = Number.parseInt(trimmed, 10);
  if (port < 1 || port > 65535) {
    throw new Error(`Invalid ${name}: "${raw}". Expected an integer between 1 and 65535.`);
  }
  return port;
}

export const WEB_PORT = parsePort('PM_WEB_PORT', process.env.PM_WEB_PORT, 5173);
export const MOCK_WEB_PORT = parsePort('PM_MOCK_WEB_PORT', process.env.PM_MOCK_WEB_PORT, 7420);
export const BASE_URL = `http://localhost:${WEB_PORT}`;
export const APP_URL = BASE_URL;
export const MOCK_RUNTIME_URL = `http://127.0.0.1:${MOCK_WEB_PORT}`;
export const MOCK_URL = MOCK_RUNTIME_URL;

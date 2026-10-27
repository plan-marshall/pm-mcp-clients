/**
 * Centralized port resolution for pm-web-e2e.
 *
 * Reads PM_WEB_PORT (default 5173) and PM_MOCK_WEB_PORT (default 7420).
 * Tests and test helpers must import URLs from here rather than hardcoding port literals.
 */

export const WEB_PORT = Number(process.env.PM_WEB_PORT) || 5173;
export const MOCK_WEB_PORT = Number(process.env.PM_MOCK_WEB_PORT) || 7420;
export const BASE_URL = `http://localhost:${WEB_PORT}`;
export const APP_URL = BASE_URL;
export const MOCK_RUNTIME_URL = `http://127.0.0.1:${MOCK_WEB_PORT}`;
export const MOCK_URL = MOCK_RUNTIME_URL;

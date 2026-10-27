/**
 * Port configuration resolution for pm-web-app and dev server.
 */
export interface ResolvedPorts {
  webPort: number;
  mockWebPort: number;
  mockTarget: string;
}

function parsePort(name: string, rawValue: string | undefined, defaultPort: number): number {
  if (rawValue === undefined || rawValue.trim() === '') {
    return defaultPort;
  }
  const trimmed = rawValue.trim();
  if (!/^\d+$/.test(trimmed)) {
    throw new Error(`Invalid ${name}: "${rawValue}". Expected an integer between 1 and 65535.`);
  }
  const port = parseInt(trimmed, 10);
  if (port < 1 || port > 65535) {
    throw new Error(`Invalid ${name}: "${rawValue}". Expected an integer between 1 and 65535.`);
  }
  return port;
}

/**
 * Resolves web app and mock runtime ports from environment variables or defaults.
 *
 * @param env environment map, defaults to process.env
 * @returns resolved port numbers and mock target URL
 * @throws Error if an environment variable holds an invalid port value
 */
export function resolvePorts(env: NodeJS.ProcessEnv | Record<string, string | undefined> = process.env): ResolvedPorts {
  const webPort = parsePort('PM_WEB_PORT', env.PM_WEB_PORT, 5173);
  const mockWebPort = parsePort('PM_MOCK_WEB_PORT', env.PM_MOCK_WEB_PORT, 7420);

  return {
    webPort,
    mockWebPort,
    mockTarget: `http://127.0.0.1:${mockWebPort}`,
  };
}

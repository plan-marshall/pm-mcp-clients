import { spawn, execSync } from 'child_process';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { MOCK_WEB_PORT } from './tests/helpers/ports.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let mockProcess = null;
const AUDIT_CACHE_FILE = path.resolve(__dirname, '.audited-views.json');

async function isPortReachable(port) {
  try {
    const res = await fetch(`http://127.0.0.1:${port}/mock/state`);
    return res.ok;
  } catch {
    return false;
  }
}

export default async function globalSetup() {
  try {
    fs.writeFileSync(AUDIT_CACHE_FILE, JSON.stringify([]));
  } catch {
    // Non-fatal cache init
  }

  const cleanupAuditCache = () => {
    try {
      if (fs.existsSync(AUDIT_CACHE_FILE)) {
        fs.unlinkSync(AUDIT_CACHE_FILE);
      }
    } catch {
      // Best-effort cleanup
    }
  };

  const isUp = await isPortReachable(MOCK_WEB_PORT);
  if (isUp) {
    if (process.env.PM_MOCK_WEB_PORT) {
      throw new Error(`Port ${MOCK_WEB_PORT} is already in use by another process. Assigned ports cannot reuse an existing server.`);
    }
    console.log(`[pm-web-e2e] Mock runtime is already running on default port ${MOCK_WEB_PORT}`);
    return cleanupAuditCache;
  }

  const rootDir = path.resolve(__dirname, '..');
  const mockClasses = path.resolve(rootDir, 'pm-mock-runtime/target/classes');

  if (!fs.existsSync(mockClasses)) {
    console.log('[pm-web-e2e] Building pm-mock-runtime classes...');
    execSync('mvn -pl pm-api,pm-mock-runtime compile -DskipTests', {
      cwd: rootDir,
      stdio: 'inherit',
    });
  }

  console.log('[pm-web-e2e] Resolving classpath for pm-mock-runtime...');
  let cpOutput = '';
  try {
    cpOutput = execSync(
      'mvn -q -pl pm-mock-runtime exec:exec -Dexec.executable="echo" -Dexec.args="%classpath"',
      { cwd: rootDir },
    )
      .toString()
      .trim();
  } catch {
    // Fallback if exec:exec is not available
    cpOutput = [
      path.resolve(rootDir, 'pm-mock-runtime/target/classes'),
      path.resolve(rootDir, 'pm-api/target/classes'),
    ].join(path.delimiter);
  }

  const cp = [
    path.resolve(rootDir, 'pm-mock-runtime/target/classes'),
    path.resolve(rootDir, 'pm-api/target/classes'),
    cpOutput,
  ].join(path.delimiter);

  console.log(`[pm-web-e2e] Starting pm-mock-runtime on port ${MOCK_WEB_PORT}...`);
  mockProcess = spawn(
    'java',
    [
      '-cp',
      cp,
      'de.planmarshall.mock.cli.MockRuntimeCli',
      '--web-port',
      String(MOCK_WEB_PORT),
      '--scenario',
      'web-pairing',
    ],
    {
      cwd: rootDir,
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );

  mockProcess.stderr.on('data', (data) => {
    console.error(`[pm-mock-runtime] ${data}`);
  });

  // Wait for mock runtime to be ready
  let ready = false;
  for (let i = 0; i < 30; i++) {
    if (await isPortReachable(MOCK_WEB_PORT)) {
      ready = true;
      break;
    }
    await new Promise((r) => setTimeout(r, 500));
  }

  if (!ready) {
    if (mockProcess) mockProcess.kill();
    throw new Error(`Timed out waiting for pm-mock-runtime to start on port ${MOCK_WEB_PORT}`);
  }

  console.log(`[pm-web-e2e] pm-mock-runtime is ready on port ${MOCK_WEB_PORT}`);

  // Return teardown function
  return () => {
    cleanupAuditCache();
    if (mockProcess) {
      console.log('[pm-web-e2e] Stopping pm-mock-runtime...');
      mockProcess.kill('SIGTERM');
      mockProcess = null;
    }
  };
}

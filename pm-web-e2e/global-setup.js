import { spawn, execSync } from 'child_process';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { MOCK_WEB_PORT } from './tests/helpers/ports.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let mockProcess = null;
const AUDIT_CACHE_FILE = path.resolve(__dirname, '.audited-views.json');
/** The fixed device secret of the suite run, so a restarted mock accepts the paired browser. */
const DEVICE_SECRET = 'pmw_e2e_fixed_device_secret';
/** What the restart helper needs: the process, its classpath, its root and the fixed secret. */
const DESCRIPTOR_FILE = path.resolve(
  process.env.PLAYWRIGHT_OUTPUT_DIR || path.resolve(__dirname, 'target'),
  'mock-runtime.json',
);

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
    try {
      fs.rmSync(DESCRIPTOR_FILE, { force: true });
    } catch {
      // Non-fatal: the restart helper refuses instead of stopping a process it did not start.
    }
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
      '--device-secret',
      DEVICE_SECRET,
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

  // The restart helper stops and starts this process for the live-indicator check, so it needs
  // the classpath, the root and the fixed secret the pairing depends on — never a fresh one.
  try {
    fs.mkdirSync(path.dirname(DESCRIPTOR_FILE), { recursive: true });
    fs.writeFileSync(
      DESCRIPTOR_FILE,
      JSON.stringify(
        {
          pid: mockProcess.pid,
          classpath: cp,
          root: rootDir,
          deviceSecret: DEVICE_SECRET,
          port: MOCK_WEB_PORT,
        },
        null,
        2,
      ),
      'utf8',
    );
  } catch (err) {
    // Without the descriptor the live check cannot restart the mock, so a run that cannot write it
    // must fail here instead of in an unrelated assertion later.
    if (mockProcess) {
      mockProcess.kill('SIGTERM');
      mockProcess = null;
    }
    throw new Error(`Could not write the mock descriptor: ${err}`);
  }

  // Return teardown function
  return () => {
    cleanupAuditCache();
    // The live-indicator check restarts the mock; the descriptor names the process that runs now.
    let pid = mockProcess ? mockProcess.pid : null;
    try {
      pid = JSON.parse(fs.readFileSync(DESCRIPTOR_FILE, 'utf8')).pid ?? pid;
    } catch {
      // No descriptor: the process this setup started is the one to stop.
    }
    try {
      fs.rmSync(DESCRIPTOR_FILE, { force: true });
    } catch {
      // Best-effort cleanup.
    }
    if (pid) {
      console.log('[pm-web-e2e] Stopping pm-mock-runtime...');
      try {
        process.kill(pid, 'SIGTERM');
      } catch {
        // Already stopped.
      }
      mockProcess = null;
    }
  };
}

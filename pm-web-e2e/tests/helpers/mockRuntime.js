import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { MOCK_RUNTIME_URL, MOCK_WEB_PORT } from './ports.js';

/**
 * Stops and restarts the mock runtime the suite started.
 *
 * The live indicator § UX-V-01 asks for the state of the stream while the machine goes away and
 * comes back, which a browser-level simulation cannot show: the stream is the machine's. The
 * global setup writes what a restart needs — the process id, the classpath and the fixed device
 * secret — so the pairing survives the restart and the page recognises the machine again.
 */

const DESCRIPTOR = path.resolve(
  process.env.PLAYWRIGHT_OUTPUT_DIR || path.resolve(process.cwd(), 'target'),
  'mock-runtime.json',
);

/** @returns the descriptor the global setup wrote, or `null` when it left no process behind */
function readDescriptor() {
  try {
    return JSON.parse(fs.readFileSync(DESCRIPTOR, 'utf8'));
  } catch {
    return null;
  }
}

/** @returns whether the mock answers on its control route */
async function isUp() {
  try {
    const response = await fetch(`${MOCK_RUNTIME_URL}/mock/state`);
    return response.ok;
  } catch {
    return false;
  }
}

/**
 * Waits until the mock answers, or the budget runs out.
 *
 * @param ready whether the mock should be up or down
 * @param timeoutMs the budget
 */
async function waitFor(ready, timeoutMs) {
  const until = Date.now() + timeoutMs;
  while (Date.now() < until) {
    if ((await isUp()) === ready) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`the mock runtime did not become ${ready ? 'reachable' : 'unreachable'}`);
}

/**
 * Stops the mock runtime the suite started, and waits until the port answers nothing.
 *
 * A mock the suite did not start is refused: an assigned port that some other process owns is
 * never taken over.
 */
export async function stopMockRuntime() {
  const descriptor = readDescriptor();
  if (!descriptor?.pid) {
    throw new Error('the mock runtime was not started by this suite; it is not stopped here');
  }
  try {
    process.kill(descriptor.pid, 'SIGTERM');
  } catch {
    // The process is already gone: the port check below is the authority.
  }
  await waitFor(false, 15000);
}

/**
 * Starts the mock runtime again from the descriptor the global setup wrote.
 *
 * @param scenario the scenario to open with
 */
export async function startMockRuntime(scenario = 'fully-onboarded') {
  const descriptor = readDescriptor();
  if (!descriptor) {
    throw new Error('the mock runtime descriptor is missing; it cannot be restarted');
  }
  const child = spawn(
    'java',
    [
      '-cp',
      descriptor.classpath,
      'de.planmarshall.mock.cli.MockRuntimeCli',
      '--web-port',
      String(MOCK_WEB_PORT),
      '--scenario',
      scenario,
      '--device-secret',
      descriptor.deviceSecret,
    ],
    { cwd: descriptor.root, stdio: 'ignore', detached: false },
  );
  child.unref();
  fs.writeFileSync(
    DESCRIPTOR,
    JSON.stringify({ ...descriptor, pid: child.pid }, null, 2),
    'utf8',
  );
  await waitFor(true, 30000);
}

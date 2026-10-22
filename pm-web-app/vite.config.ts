import os from 'node:os';
import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

/** The addresses a request from this machine itself carries. */
const LOOPBACK_ADDRESSES = new Set(['127.0.0.1', '::1', '::ffff:127.0.0.1']);

/**
 * The names a phone can use to reach this machine.
 *
 * `PM_WEB_HOST` names the host `start.sh` prints; `os.hostname()` is often the DNS name
 * (`box.example.net`) while the name a phone uses is the short Bonjour name (`box.local`), so
 * every form of the name is listed.
 *
 * @returns the host name, the short name, and both `.local` forms, without duplicates
 */
function machineHostNames(): string[] {
  const host = process.env.PM_WEB_HOST?.trim() || os.hostname();
  const short = host.split('.')[0];
  return [...new Set([host, `${host}.local`, short, `${short}.local`])];
}

/**
 * The allowed host list: every name of this machine plus the operator's additions.
 *
 * `PM_WEB_ALLOWED_HOSTS` adds names (comma separated); it does not replace the machine's own
 * names, so a single addition cannot make the printed Network URL unreachable.
 *
 * @returns the allowed host names, without duplicates
 */
function allowedHostNames(): string[] {
  const additions = (process.env.PM_WEB_ALLOWED_HOSTS ?? '')
    .split(',')
    .map((host) => host.trim())
    .filter(Boolean);
  return [...new Set([...machineHostNames(), ...additions])];
}

/**
 * Whether a proxied request arrives from this machine itself.
 *
 * @param request the proxied request
 * @returns `true` for a loopback client
 */
function isLoopbackRequest(request: { socket: { remoteAddress?: string | null } }): boolean {
  return LOOPBACK_ADDRESSES.has(request.socket.remoteAddress ?? '');
}

export default defineConfig({
  plugins: [
    sveltekit({
      adapter: adapter({
        pages: 'build',
        assets: 'build',
        fallback: 'index.html',
        precompress: false,
        strict: false,
      }),
      preprocess: vitePreprocess(),
    }),
  ],
  resolve: {
    conditions: ['browser'],
  },
  server: {
    // A development-only server, bound to the network on purpose so a phone on the same network
    // can pair. The `/api` proxy keeps the mock runtime's own listener on loopback, so this does
    // not widen the runtime's listener security; `/mock` is restricted to loopback clients.
    host: process.env.PM_WEB_HOST ?? true,
    allowedHosts: allowedHostNames(),
    // The scenario tests read the mock runtime's scenario files from the sibling module.
    fs: {
      allow: ['..'],
    },
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:7420',
        changeOrigin: true,
      },
      '/mock': {
        target: 'http://127.0.0.1:7420',
        changeOrigin: true,
        // `/mock` is development tooling: a LAN client must not change the scenario or read/issue
        // pairing codes through the proxy. `/api` stays reachable as the app needs it.
        bypass(request, response) {
          if (isLoopbackRequest(request) || !response) {
            return undefined;
          }
          response.statusCode = 403;
          response.end('mock controls are loopback only');
          return false;
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/setupTests.ts'],
  },
});

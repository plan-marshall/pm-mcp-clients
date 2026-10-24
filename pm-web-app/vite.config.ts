import os from 'node:os';
import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

/** The addresses a request from this machine itself carries. */
const LOOPBACK_ADDRESSES = new Set(['127.0.0.1', '::1', '::ffff:127.0.0.1']);

/**
 * Canonicalizes a host name to the form Vite compares against.
 *
 * A browser sends the `Host` header lower case, while `os.hostname()` and an operator's
 * `PM_WEB_HOST` may carry upper case; a trailing dot is the same name in DNS. Vite compares the
 * header by exact string equality, so every name the list holds must be in this form.
 *
 * @param value the configured or resolved host name
 * @returns the trimmed, lower-case name without a trailing dot
 */
function normalizeHost(value: string): string {
  return value.trim().toLowerCase().replace(/\.$/, '');
}

/**
 * Whether a host is an IP literal, for which no `.local` name forms exist.
 *
 * @param host the normalized host
 * @returns `true` for an IPv4 or IPv6 literal
 */
function isIpLiteral(host: string): boolean {
  return /^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host.includes(':');
}

/**
 * The names a phone can use to reach this machine.
 *
 * `PM_WEB_HOST` names the host `start.sh` prints; `os.hostname()` is often the DNS name
 * (`box.example.net`) while the name a phone uses is the short Bonjour name (`box.local`), so
 * every form of the name is listed. An IP literal has no name forms and stays as it is.
 *
 * @returns the host name, the short name, and both `.local` forms, without duplicates
 */
function machineHostNames(): string[] {
  const host = normalizeHost(process.env.PM_WEB_HOST?.trim() || os.hostname());
  if (isIpLiteral(host)) {
    return [host];
  }
  const short = host.split('.')[0];
  return [...new Set([host, `${host}.local`, short, `${short}.local`])];
}

/**
 * The allowed host list: every name of this machine plus the operator's additions.
 *
 * `PM_WEB_ALLOWED_HOSTS` adds names (comma separated); it does not replace the machine's own
 * names, so a single addition cannot make the printed Network URL unreachable. An addition is
 * canonicalized like the machine's own names, so an operator may type either case.
 *
 * @returns the allowed host names, without duplicates
 */
function allowedHostNames(): string[] {
  const additions = (process.env.PM_WEB_ALLOWED_HOSTS ?? '')
    .split(',')
    .map(normalizeHost)
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
    {
      name: 'strip-announcer-inline-style',
      enforce: 'post',
      transform(code, id) {
        if (id.includes('root.svelte')) {
          return code.replace(
            /style="position:\s*absolute;[^"]*"/g,
            'class="svelte-announcer"',
          );
        }
      },
      renderChunk(code) {
        if (code.includes('svelte-announcer')) {
          return code
            .replace(
              /style=\\"position:\s*absolute;[^\\"]*\\"/g,
              'class=\\"svelte-announcer\\"',
            )
            .replace(
              /style="position:\s*absolute;[^"]*"/g,
              'class="svelte-announcer"',
            );
        }
      },
    },
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

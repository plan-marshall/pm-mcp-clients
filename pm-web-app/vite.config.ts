import os from 'node:os';
import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

/**
 * The names a phone can use to reach this machine.
 *
 * @returns the DNS name, the short name, and both `.local` forms, without duplicates
 */
function machineHostNames(): string[] {
  const host = os.hostname();
  const short = host.split('.')[0];
  return [...new Set([host, `${host}.local`, short, `${short}.local`])];
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
    // can pair. The `/api` and `/mock` proxies keep the mock runtime's own listener on loopback,
    // so this does not widen the runtime's listener security.
    host: process.env.PM_WEB_HOST ?? true,
    // Vite rejects an unknown host name: the machine's own names are allowed here, while IP
    // addresses stay allowed. `os.hostname()` is often the DNS name (`box.example.net`) while the
    // name a phone uses is the short Bonjour name (`box.local`), so both forms are listed.
    // `PM_WEB_ALLOWED_HOSTS` overrides the list (comma separated).
    allowedHosts:
      process.env.PM_WEB_ALLOWED_HOSTS?.split(',')
        .map((host) => host.trim())
        .filter(Boolean) ?? machineHostNames(),
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
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/setupTests.ts'],
  },
});

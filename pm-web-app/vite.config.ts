import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
import { defineConfig } from 'vitest/config';

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

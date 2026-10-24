# Completion Report: `web-app-static-export-csp` (SPA Boot Under Strict CSP)

## Executive Summary

The static SvelteKit single-page application (`pm-web-app`) is served by the packaged daemon (`pm-mcp-server`) under its strict Content Security Policy without relaxing the policy to `'unsafe-inline'`.

Previously, Step 8 of the parent plan failed because the browser blocked:
1. SvelteKit's inline bootstrap script (`<script>{ __sveltekit_... }</script>`) under `script-src 'self'`.
2. Inline `style="..."` attributes (`app.html`, `Spinner.svelte`, `BrandMark.svelte`, `CleanupView.svelte`, and SvelteKit's built-in `svelte-announcer`) under `default-src 'self'`.
3. Vite-inlined SVG data URI (`data:image/svg+xml,...`) loaded as a CSS mask in `BrandMark.svelte` under `default-src 'self'`.

This remediation closed all three gaps:
- **Server inline script hash admission**: `StaticWebApp` extracts the inline `<script>` body from `META-INF/resources/index.html` at startup, computes its SHA-256 base64 digest, and `WebRequestHandler` injects `'sha256-<hash>'` dynamically into `script-src`.
- **Client inline style elimination**: The inline wrapper in `app.html` was removed; `Spinner.svelte` sizes via `data-size` attributes and CSS classes; `BrandMark.svelte` renders an inline `<svg>` with `currentColor` eliminating both the inline style and the `data:` URI mask; `CleanupView.svelte` uses `.cleanup-kept-empty`; and a Vite build plugin strips the hardcoded inline style on SvelteKit's `svelte-announcer` while supplying styling via `base.css`.
- **Browser-level integration proof**: `WebAppIT.shouldBootInHeadlessBrowserUnderCsp` launches headless Chromium via Chrome DevTools Protocol (CDP), loads `/` from the running Vert.x web listener, boots the SvelteKit SPA, executes an in-page fetch to `/api/v1/status` (returning `401` as expected), and proves zero CSP console warnings or violations.

## Delivered Artifacts

### `pm-mcp-clients` repository

| File | Change |
|---|---|
| `pm-web-app/src/app.html` | Removed `<div style="display: contents">` wrapper around `%sveltekit.body%`. |
| `pm-web-app/src/lib/components/Spinner.svelte` | Replaced inline `style="--size: {size}"` with `data-size={size}` and CSS selectors. |
| `pm-web-app/src/lib/components/BrandMark.svelte` | Replaced CSS mask (`mask-image: var(--brand-mark-url)` with Vite `data:` URI) and inline `style` with pure inline SVG markup inheriting `currentColor`. |
| `pm-web-app/src/lib/components/BrandMark.test.ts` | Updated assertions to verify `width`, `height`, and `data-size` attributes instead of inline `style`. |
| `pm-web-app/src/lib/views/CleanupView.svelte` | Replaced inline `style="padding: var(--space-4);"` with CSS class `.cleanup-kept-empty`. |
| `pm-web-app/src/styles/base.css` | Added styling for `#svelte-announcer` and `.svelte-announcer`. |
| `pm-web-app/vite.config.ts` | Added `strip-announcer-inline-style` plugin to strip the hardcoded inline style from SvelteKit's route announcer during build. |
| `doc/agy/web-app-static-export-csp-remediation-plan.md` | Checked off recipe items, updated verification evidence, and moved to `implemented/`. |
| `doc/agy/web-app-static-export-and-server-import-plan-report.md` | Updated Step 8 blocker section with resolution details. |
| `doc/agy/implemented/README.md` | Recorded remediation plan and linked to this report. |

### `plan-marshall-mcp` repository

| File | Change |
|---|---|
| `pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/StaticWebApp.java` | Added `extractScriptHash`, `sha256Hash`, and cached `inlineScriptHash()` extracting the inline `<script>` SHA-256 base64 digest. |
| `pm-mcp-server/src/main/java/de/planmarshall/mcp/server/web/WebRequestHandler.java` | Added dynamic `securityHeaders(String inlineScriptHash)` and `securityHeaders(StaticWebApp app)` appending the hash to `script-src`. |
| `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/StaticWebAppTest.java` | Added unit tests verifying script hash extraction, external script omission, and empty script behavior. |
| `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebRequestHandlerTest.java` | Added unit tests verifying CSP contains `'sha256-...'` when an inline script is present. |
| `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebListenerTest.java` | Updated security header test to pass `StaticWebApp.fromContextClassLoader()`. |
| `pm-mcp-server/src/test/java/de/planmarshall/mcp/server/web/WebAppIT.java` | Added `shouldBootInHeadlessBrowserUnderCsp` headless Chrome CDP smoke test asserting DOM boot, API connectivity, and zero CSP console errors. |

## Test & Verification Results

### 1. Headless Browser Smoke Test (`WebAppIT.java`)
- **Test execution**: `WebAppIT.shouldBootInHeadlessBrowserUnderCsp`
- **Browser**: Google Chrome / Chromium headless (`--headless=new`, `--remote-debugging-port`)
- **Protocol**: Chrome DevTools Protocol (`Runtime.enable`, `Log.enable`, `Runtime.evaluate`)
- **Assertions**:
  - `result.cspViolations()`: `[]` (expected empty list, 0 violations)
  - `result.title()`: `"plan-marshall"`
  - `result.hasVisibleElement()`: `true` (renders `.app-header`, `[data-testid="brand-inert"]`, `.app-body`, pairing card)
  - `result.apiStatus()`: `401` (successful in-page `fetch('/api/v1/status')` under `connect-src 'self'`)
- **Verification record**: `target/verification-results/web-app-browser-proof.json` written with `"browser_csp_clean": true`.

### 2. Client Test Suite (`pm-web-app`)
- **Vitest**: 60 test files, 557 tests passed (0 failures)
- **Svelte Check**: 0 errors, 0 warnings
- **ESLint**: Clean (0 lint errors)
- **Build inspection**: `build/index.html` contains exactly 1 inline `<script>`, 0 `style=` attributes.

### 3. Server Unit & Integration Tests (`pm-mcp-server`)
- **Unit tests**: 50 tests run, 0 failures, 0 errors
- **Integration tests (`WebAppIT`)**: 3 tests run, 0 failures, 0 errors:
  - `shouldServePackagedApp`: 200 on index, 200 on immutable assets, 200 on client route, 404 on missing, security headers verified
  - `shouldServeHeadWithoutBody`: 200 on HEAD with empty body and security headers
  - `shouldBootInHeadlessBrowserUnderCsp`: Real browser boots SPA under CSP with 0 violations

## Domain Invariants

| Invariant | Status | Evidence |
|---|---|---|
| No `'unsafe-inline'` in `script-src` | Preserved | Verified in `WebRequestHandlerTest` and `WebAppIT` assertions. |
| No `'unsafe-inline'` in `style-src` | Preserved | All inline styles removed across client source and SvelteKit runtime components. |
| No `data:` image URIs under CSP | Preserved | `BrandMark.svelte` renders inline SVG; zero external image requests. |
| Trusted Types policy | Preserved | `require-trusted-types-for 'script'` satisfied by SvelteKit's `svelte-trusted-html` policy. |
| Browser clean console | Verified | Chrome CDP `Log.entryAdded` and `Runtime.consoleAPICalled` report 0 CSP violations. |
| Static path confinement | Preserved | `NOT_NORMALIZED` and directory traversal checks intact. |
| API / MCP route restriction | Preserved | `/api/v1/**` reaches runtime; `/mcp` returns 404. |

## Build & Quality Evidence

```text
# pm-web-app test suite
$ cd pm-web-app && npm test && npm run check && npm run lint
Test Files  60 passed (60)
     Tests  557 passed (557)
svelte-check found 0 errors and 0 warnings
eslint . clean

# pm-mcp-server integration test suite
$ ./mvnw -o -pl pm-mcp-server -Pintegration-tests verify -Dit.test=WebAppIT -DskipTests
[INFO] Running Packaged web app served by the packaged daemon
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

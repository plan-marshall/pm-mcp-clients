# pm-web-app

`pm-web-app` is the browser-based operator web interface for [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp). It is built with [Svelte 5](https://svelte.dev), [SvelteKit](https://kit.svelte.dev), and [Vite](https://vite.dev).

---

## Capabilities & Views

- **Onboarding route**: `/onboarding` renders the state the runtime reports — start, consents, language, harnesses with their token reporting, models, roles, credentials, providers, plan defaults, the automatic run, and the done screen with read-only step summaries.
- **Entry & pairing**: `/pair` and `/entry` pair a browser with a code (or the URL fragment), present the pairing URL as a QR code, and hold the loopback credential entry. The browser's own TLS warning carries the certificate fingerprint before pairing; the listener fingerprint is shown in Settings after pairing.
- **Machine settings**: `/settings/machine` shows the seven aspects, the remembered consents with their withdrawal, the documents mode, the cleanup durations, the tokens with rotate/replace, and the paired devices; a value changed in another client opens the 409 conflict dialogue.
- **Paired devices**: `/settings` lists the browsers that can act on the machine, renames one inline, revokes one or all others, and turns the network listener off.
- **Operator dashboard**: real-time status overview of active projects, epics, running plans, questions, and reports.
- **Mock Runtime Integration**: connects to `pm-mock-runtime` via the Vite reverse-proxy for local development, the scenario view tests, and offline manual testing.

Every string is loaded from the message catalogue (English and German); no user-facing text is written in the app's code.

---

## Prerequisites

- **Node.js**: Version 22.17 or later (`node --version`). This is the SvelteKit 3 floor; 22.0–22.16 are rejected.
- **npm**: Version 10.x or later (`npm --version`).
- **Java 25 & Maven**: For running `pm-mock-runtime` during development.

---

## Static Export and Daemon Serving

`npm run build` writes the static site to `build/` (`index.html` and `_app/immutable/**`). The Maven build
copies that tree to `META-INF/resources` and packages it, so `pm-web-app-<version>.jar` is the deployable
artifact. A `verify` execution fails the build when the JAR carries either no
`META-INF/resources/index.html` or no `META-INF/resources/_app/immutable/...` asset.

```bash
# Build the SPA and its resource JAR
./mvnw -pl pm-web-app package

# Inspect what the artifact ships
jar tf pm-web-app/target/pm-web-app-*.jar | grep -E 'META-INF/resources/(index.html|_app/immutable/)'

# Install it for the server build: plan-marshall-mcp resolves this SNAPSHOT
./mvnw -pl pm-web-app install -DskipTests
```

The app is same-origin: it calls `/api/v1` on the origin it was loaded from, so `pm-mcp-server` (in
[plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp)) serves the app and the API on the same
web listener. There is no API base URL, no CORS configuration, and no separate static web server. The server
depends on this JAR and serves its `META-INF/resources` tree; its README describes the routing, caching, and
native-resource requirement.

---

## Manual Testing & Local Development

### Step 1: Start the Mock Runtime Server
In a terminal, start `pm-mock-runtime` with the web listener enabled on port 7420:
```bash
./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario onboarding-happy --web-port 7420"
```
The mock server will listen on `http://127.0.0.1:7420` and provide scenario mock data for `/api/v1/*` and `/mock/*`.

### Step 2: Run the Web Client (`start.sh` & `stop.sh`)

`pm-web-app` includes smart lifecycle scripts that perform incremental dependency setup (`./mvnw -am` without `clean`) automatically:

```bash
# Start Vite development server in background (default)
./pm-web-app/start.sh

# Or start in foreground
./pm-web-app/start.sh --foreground

# Stop background instance
./pm-web-app/stop.sh
```

Alternatively, you can run directly via npm:
```bash
cd pm-web-app
npm install
npm run dev
```

The development server starts at `http://localhost:5173`. `start.sh` prints every coordinate a phone can use, and the direct paired URL with a code that works right now:

```text
pm-web-app started successfully (PID: 65428).
Local:       http://localhost:5173
Network:     http://mbp-oliver.local:5173
Network (IP): http://192.168.1.20:5173
Paired:      http://mbp-oliver.local:5173/pair#code=KXQB-MRTN
```

When the resolved host is a loopback address (`127.0.0.1`, `localhost`, `::1`, or `[::1]`), only the `Local:` line and a loopback `Paired:` line are printed: no `Network` coordinate is offered, even when the machine has a primary IPv4.

Vite is configured to automatically proxy `/api` and `/mock` calls to `http://127.0.0.1:7420`.

### Reaching the app from a phone

The Vite development server binds to all interfaces on purpose, so a phone on the same network opens the `Network` URL. This is a development-only exposure: the `/api` proxy keeps the mock runtime's own listener on loopback, and the real runtime keeps its loopback/LAN listener security. The `/mock` control proxy is restricted to clients on this machine, so a phone cannot change the scenario or read/issue pairing codes through the dev server. Set `PM_WEB_HOST=127.0.0.1` when no network exposure is wanted; `start.sh` then prints only the local URL and a loopback `Paired:` URL.

The `Paired:` URL is printed on the resolved host, never on a substituted primary IPv4: a phone that opens it stays on the address the operator chose. With a loopback `PM_WEB_HOST`, that URL stays on the loopback host.

The machine's name is preferred over its IP: the name survives DHCP address changes and resolves through mDNS/Bonjour, while `Network (IP)` is the fallback for networks that block name resolution. `PM_WEB_HOST` sets the name explicitly, and `PM_WEB_ALLOWED_HOSTS` (comma separated) adds further names Vite accepts; the machine's own names stay allowed. IP addresses are allowed by Vite without an entry. Every name is printed and compared in its canonical lower-case form without a trailing dot, because the browser's `Host` header is lower case and Vite compares it with the allowed list by exact string equality; `scripts/verify-dev-host.sh` proves the printed name returns `200` and an unknown host still returns `403`.

The mock's web device secret is one file per machine at `~/.plan-marshall-mcp/mock-web-device-secret` (mode `0600`; `PM_MOCK_DEVICE_SECRET_FILE` overrides the path), so a paired browser survives `mvn clean`, a mock restart, and a switch to another worktree. To reset the development session, stop the mock and delete that file. Deleting `pm-mock-runtime/target/machine` (or the printed `Base` when the checkout is too deep) resets the scenario but **not** the device secret.

A pairing code works once. When the code was already consumed, `start.sh` asks `POST /mock/pairing` for the replacement and prints it as the `Paired:` URL; re-running `start.sh` while a code is open returns the same code. The header is the only always-on status row: its machine badge carries the listener mode and the paired-device count as a hover title, the brand opens the start page, and the header offers the Process Designer. Every action explains itself on hover.

### Step 3: Interactive Testing in the Browser
1. **Pairing Flow**: Navigate to `http://localhost:5173/pair#code=TEST-CODE` to test browser pairing and device pairing.
2. **Onboarding**: Open `http://localhost:5173/onboarding` and step through the states (language, harnesses, models, roles, credentials, providers, plans).
3. **Machine settings**: Open `http://localhost:5173/settings/machine` for the aspects, the consents, the documents mode, the tokens, and the paired devices.
4. **Switching Scenarios**: You can change the mock server scenario on the fly via the mock control API or restarting `pm-mock-runtime` with another scenario:
   ```bash
   ./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario status-only --web-port 7420"
   ```

---

## Available Scripts

| Command | Description |
|---|---|
| `npm run dev` | Starts Vite development server at `http://localhost:5173` with hot module replacement (HMR) and `/api` proxying. |
| `npm run build` | Compiles production assets with SvelteKit static adapter. |
| `npm run preview` | Serves the production build locally for verification. |
| `npm test` | Runs unit and component tests using Vitest and Testing Library. |
| `npm run test:urls` | Runs the Bash matrix test for the development connection URLs printed by `start.sh`. |
| `npm run test:dev-host` | Starts Vite on loopback and proves the printed lower-case name returns `200` while an unknown host returns `403` (`curl --resolve`, no mDNS). |
| `npm run test:watch` | Runs Vitest in interactive watch mode. |
| `npm run lint` | Lints TypeScript and Svelte components with ESLint. |
| `npm run format:check` | Checks code formatting with Prettier. |
| `npm run quality` | Runs both lint and CI test suite. |
| `npm run audit:fix` | Runs `npm audit fix` and regenerates the SvelteKit `$app` configuration (see the editor note below). |

### Editor: generated TypeScript configuration

`tsconfig.json` extends `$app/tsconfig`, which `svelte-kit sync` writes to `node_modules/$app/tsconfig.json`. A fresh checkout has no `node_modules`, so install before opening the project in an editor: `npm ci` (its `prepare` script runs the sync) or a direct `npm run prepare`. If VS Code still reports `File '$app/tsconfig' not found` after an install, reload the window (`Developer: Reload Window`) so its TypeScript server re-reads the generated file.

The TypeScript project covers `src`, `test`, and `vite.config.ts`. `eslint.config.js` is deliberately outside it — ESLint checks that file — and `noEmit` is set explicitly, so a missing generated config can never turn a type check into an emit over a source file.

`npm audit fix` and `npm prune` treat the generated `node_modules/$app` directory as an extraneous package and remove it. Use `npm run audit:fix` (audit fix followed by `npm run prepare`), or run `npm run prepare` afterward, so the generated TypeScript configuration stays in place.

---

## End-to-End Testing (`pm-web-e2e`)

Playwright end-to-end tests for the web interface reside in the sibling module `pm-web-e2e`.

To run E2E tests against the web app and mock runtime:
```bash
cd ../pm-web-e2e
npm install
npx playwright install --with-deps

# Headless run (default CI mode)
npm test

# Headed run (opens visible browser windows)
npm run test:headed

# Interactive UI mode
npm run test:ui
```
*Note: `pm-web-e2e`'s global setup will automatically start `pm-mock-runtime` on port 7420 if it is not already running.*

# pm-web-app

`pm-web-app` is the browser-based operator web interface for [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp). It is built with [Svelte 5](https://svelte.dev), [SvelteKit](https://kit.svelte.dev), and [Vite](https://vite.dev).

---

## Capabilities & Views

- **Onboarding route**: `/onboarding` renders the state the runtime reports — start, consents, language, harnesses with their token reporting, models, roles, credentials, providers, plan defaults, the automatic run, and the done screen with read-only step summaries.
- **Entry & pairing**: `/pair` and `/entry` pair a browser with a code (or the URL fragment), present the pairing URL as a QR code, show the network listener's fingerprint, and hold the loopback credential entry.
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

The development server starts at `http://localhost:5173`. When `pm-mock-runtime` is running, `start.sh` automatically prints the direct paired URL beneath the plain local URL:
```text
pm-web-app started successfully (PID: 65428).
Local:   http://localhost:5173
Paired:  http://localhost:5173/pair#code=KXQB-MRTN
```
Vite is configured to automatically proxy `/api` and `/mock` calls to `http://127.0.0.1:7420`.

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
| `npm run test:watch` | Runs Vitest in interactive watch mode. |
| `npm run lint` | Lints TypeScript and Svelte components with ESLint. |
| `npm run format:check` | Checks code formatting with Prettier. |
| `npm run quality` | Runs both lint and CI test suite. |

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

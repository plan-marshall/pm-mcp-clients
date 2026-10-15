# pm-web-app

`pm-web-app` is the browser-based operator web interface for [plan-marshall-mcp](https://github.com/plan-marshall/plan-marshall-mcp). It is built with [Svelte 5](https://svelte.dev), [SvelteKit](https://kit.svelte.dev), and [Vite](https://vite.dev).

---

## Capabilities & Views

- **Device Pairing**: Web pairing flow (`/pair#code=...`) with PIN and token validation.
- **Onboarding Setup**: Multi-step wizard covering language, harnesses, models, roles, credentials, providers, and plan defaults.
- **Operator Dashboard**: Real-time status overview of active projects, epics, running plans, questions, and reports.
- **Settings & Consents**: View and modify machine configurations, credential rotation, and document sources.
- **Mock Runtime Integration**: Connects seamlessly to `pm-mock-runtime` via Vite reverse-proxy for local development and offline manual testing.

---

## Prerequisites

- **Node.js**: Version 22.x or later (`node --version`).
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

### Step 2: Install Dependencies & Run the Dev Server
In another terminal, navigate to `pm-web-app` and start Vite:
```bash
cd pm-web-app
npm install
npm run dev
```

The development server starts at `http://localhost:5173`.
Vite is configured to automatically proxy `/api` and `/mock` calls to `http://127.0.0.1:7420`:
```text
  VITE v6.1.0  ready in 250 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### Step 3: Interactive Testing in the Browser
1. **Pairing Flow**: Navigate to `http://localhost:5173/pair#code=TEST-CODE` to test browser pairing and device onboarding.
2. **Onboarding Wizard**: Step through the onboarding pages (languages, harnesses, roles).
3. **Switching Scenarios**: You can change the mock server scenario on the fly via the mock control API or restarting `pm-mock-runtime` with another scenario:
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

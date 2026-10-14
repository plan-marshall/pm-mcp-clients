# Web Client Implementation Plan (`pm-web-app` & `pm-web-e2e`)

## Overview

Implementation plan for the web client module (`pm-web-app`) and its end-to-end testing module (`pm-web-e2e`) in `pm-mcp-clients`, complying with `doc/design/web-app/`, the use cases in `doc/plans/`, and running against `pm-mock-runtime`.

---

## Tasks

- [x] **Task 1: Worktree & Environment Setup**
  - [x] 1.1 Verify git worktree `feat/web-app` at `/Users/oliver/git/worktrees/pm-web-app`.
  - [x] 1.2 Inspect Maven parent POM and dependencies for `frontend-maven-plugin` (Node `v22.23.1`, npm `10.9.2`).
  - [x] 1.3 Review and align with frontend skills (`pm-dev-frontend:javascript`, `javascript-security`, `jest-testing`, `lint-config`, `css`).

- [x] **Task 2: Root Maven Configuration**
  - [x] 2.1 Update root `pom.xml` to include `<module>pm-web-app</module>` and `<module>pm-web-e2e</module>`.
  - [x] 2.2 Verify Maven enforcer rules (`clients-depend-on-pm-api-only`, `no-model-facing-content`) and configure module properties.

- [x] **Task 3: Create `pm-web-app` Maven Module & Node Toolchain Setup**
  - [x] 3.1 Create `pm-web-app/pom.xml` configuring `frontend-maven-plugin` (install node/npm, npm install, build, test, lint) and `maven-resources-plugin` to package the static SPA into `META-INF/resources` in the output JAR.
  - [x] 3.2 Create `pm-web-app/package.json` with `"type": "module"`, Svelte 5, Vite, TypeScript, Vitest, ESLint flat config, Prettier.
  - [x] 3.3 Create `vite.config.ts` (configured with proxy to `http://127.0.0.1:7420` for `/api` and `/mock`, static single-page app build output to `build/`).
  - [x] 3.4 Create `tsconfig.json` and `svelte.config.js`.
  - [x] 3.5 Create `eslint.config.js` following `pm-dev-frontend:lint-config` (flat config with `@eslint/js`, `security`, `sonarjs`, etc.).
  - [x] 3.6 Create `.prettierrc` and `.gitignore`.
  - [x] 3.7 Install npm dependencies and generate consistent `package-lock.json`.

- [x] **Task 4: Design System & Styling (`pm-dev-frontend:css`)**
  - [x] 4.1 Create CSS design tokens (`styles/tokens.css`) for light and dark themes (surfaces, text, borders, accents, status colors).
  - [x] 4.2 Create reset & base typography (`styles/base.css`, `styles/layout.css`).
  - [x] 4.3 Create shared UI components (Button, Input, Card, Modal/Dialog, StepBar, Spinner, StatusBadge, ConsentBox, CodeBlock).

- [x] **Task 5: Localization & Message Catalogue**
  - [x] 5.1 Implement message catalogue loader and string formatting utility (`src/lib/i18n/`) supporting English and German locale packs.
  - [x] 5.2 Extract message packs for `web.*`, `onboard.*`, `designer.*`, `common.*` from `doc/design/web-app/onboarding-texts.adoc`, `entry.adoc`, `process-designer-texts.adoc`.
  - [x] 5.3 Ensure zero hardcoded strings in UI components.

- [x] **Task 6: Runtime Client & Hypermedia State Machine**
  - [x] 6.1 Implement `ApiClient` (`src/lib/api/client.ts`):
    - `localStorage`-backed device secret (`pmw_...`).
    - Pairing redemption: `POST /api/v1/pair` with `{"code": code}`.
    - Authenticated requests with `Authorization: Bearer <secret>`.
    - SSE streaming over `fetch` / `ReadableStream` (with `Last-Event-ID` reconnection).
    - Status checking (`GET /api/v1/status`).
    - Hypermedia link traversal (`POST` to link href).
    - Revocation and 401 handling.
  - [x] 6.2 Implement runtime state store (`src/lib/stores/runtime.ts`): reactive state representing pairing status, onboarded flag, current hypermedia state, machine metadata, connected status.

- [x] **Task 7: Implement Web App Views (Complying with `doc/design/web-app/` and `doc/plans/`)**
  - [x] 7.1 **Entry & Pairing (`UC-ENT-1`, `UC-ENT-2`, `doc/design/web-app/entry.adoc`)**:
    - URL fragment code extraction (`#code=...`) & auto-redemption.
    - Manual code input form with copyable terminal helper commands.
    - Revoked device landing page with specific revocation reason.
    - Network listener offline page with auto-reconnect.
  - [x] 7.2 **Onboarding (`UC-ONB-1` to `UC-ONB-9`, `doc/design/web-app/onboarding.adoc`)**:
    - Onboarding frame with machine header and 7-step progress bar.
    - Start screen (`onboard.mode`): "Decide everything for me" vs "Begin with step 1".
    - Standard consent screen (`*.consent`): looks for, in, runs, never, tokens.
    - Model at work screen (`[M]` states): animated indicator, token estimate, "Continue without the advice".
    - Steps: Language, Harnesses (probe & confirm), Models, Roles, Credentials, Providers, Plans, and Completed.
  - [x] 7.3 **Machine Settings (`UC-ONB-10`, `doc/design/web-app/machine-settings.adoc`)**:
    - Aspect configuration re-run.
    - Paired devices view: list, label, last used, revoke action.
  - [x] 7.4 **Main Workspace Navigation & Plan Overview (`UC-PLN-4`, `UC-PRJ-2`, `UC-SYS-1`, `UC-SYS-2`)**:
    - Navigation bar (Explorer, Plans, Reports, System, Settings).
    - Project explorer & plan overview dashboard.
    - System view with runtime status, listeners, version.
  - [x] 7.5 **Process Designer (`UC-PLN-3`, `doc/design/web-app/process-designer.adoc`)**:
    - Process designer route (`/designer`).
    - Input sources: upload `.pmwf` file, paste text, load installed unit.
    - Side-by-side text editor with line numbers & structure tree.
    - Frontmatter inspector & editing.
    - Building blocks library panel.
    - Validation trigger & findings highlighting.
    - Composed process preview.

- [x] **Task 8: Unit & Component Testing (`pm-dev-frontend:jest-testing` / Vitest)**
  - [x] 8.1 Setup Vitest with jsdom and `@testing-library/svelte`.
  - [x] 8.2 Unit tests for `ApiClient` (pairing, bearer auth, error handling, SSE stream handling).
  - [x] 8.3 Unit tests for i18n message catalogue and formatting.
  - [x] 8.4 Component tests for Pairing view (fragment redemption, manual entry, invalid code, revoked state).
  - [x] 8.5 Component tests for Onboarding views (step bar, consent, model working, state transitions).
  - [x] 8.6 Component tests for Process Designer (loading file, editing, validation findings).
  - [x] 8.7 Component tests for Main Workspace & System view.

- [x] **Task 9: Linting & Code Quality Validation**
  - [x] 9.1 Run ESLint flat config validation on all files (`npm run lint`).
  - [x] 9.2 Run Prettier formatting check (`npm run format:check`).
  - [x] 9.3 Validate DOM security rules (`pm-dev-frontend:javascript-security` - zero `{@html}` or unescaped HTML sinks).

- [x] **Task 10: Create `pm-web-e2e` Maven Module (Playwright E2E Tests)**
  - [x] 10.1 Create `pm-web-e2e/pom.xml` patterned after `nifi-extensions/e-2-e-playwright`.
  - [x] 10.2 Create `pm-web-e2e/package.json` with `@playwright/test`, `eslint`, `prettier`.
  - [x] 10.3 Create `pm-web-e2e/playwright.config.js` with webServer starting the web app and `pm-mock-runtime`.
  - [x] 10.4 Create E2E test specs:
    - `tests/pairing.spec.js`: E2E pairing flow against `pm-mock-runtime`.
    - `tests/onboarding.spec.js`: E2E onboarding navigation and step transitions.
    - `tests/designer.spec.js`: E2E process designer workflow.
    - `tests/workspace.spec.js`: E2E workspace and system view checks.
  - [x] 10.5 Verify Playwright tests pass against running `pm-mock-runtime`.

- [x] **Task 11: End-to-End Build & Verification Sweep**
  - [x] 11.1 Build full Maven reactor (`mvn clean install` / `mvn verify`).
  - [x] 11.2 Verify static JAR artifact packaging (`pm-web-app` `META-INF/resources`).
  - [x] 11.3 Review all changes, commit on worktree branch `feat/web-app`, update plan with check-markers.

# Implementation Plan: `web-session-pairing-and-dev-host` (Keep Web Sessions Alive, Reach the Dev App from a Phone, Make Single-Use Pairing Codes Understandable, and Close the Web Chrome, Navigation & Action-Advisory Gaps)

This implementation plan resolves the eight live findings recorded in [`doc/findings-web.txt`](../findings-web.txt) for the browser client (`pm-web-app`), the mock runtime (`pm-mock-runtime`), and the development launch scripts.

The first four findings are not independent bugs. Finding 1 has two root causes: the mock runtime generates a fresh device secret on every start, and the browser treats every `401` as a revocation. Findings 3 and 4 share one root cause: a pairing code is single use by contract, while the web-only development loop never issues or shows the next code. Finding 2 is an independent development-host defect.

The four additional findings are web-chrome and discoverability gaps: a full-width connection banner duplicates the header, the brand/logo is inert, action buttons carry no hover advisory, and the process designer has no visible entry point from the app shell or a plan. They are independent of the session/pairing work but share the same web-app shell and catalogue surfaces.

The single-use rule of [`03-web-server.adoc#web-pairing`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-pairing) is load-bearing security and is **not** changed by this plan. What changes is the surrounding development experience: sessions survive a mock restart, a phone reaches the Vite development server by the machine's real name, a paired browser never re-redeems a code, a second device can always obtain a fresh one, the shell carries only the status that is not already in the header, and every action is discoverable and explains itself on hover.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Findings Source** | [`doc/findings-web.txt`](../findings-web.txt) (all eight findings are planned here; the finding list is emptied when the plan is updated) |
| **Use Cases** | [`UC-ENT-1`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect), [`UC-ENT-2`](../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-2) (Manage Paired Devices), [`UC-ONB-3`](../../../plan-marshall-documentation/doc/use-cases/01-onboarding.adoc#UC-ONB-3) (Languages and Message Catalogue), [`UC-PLN-1`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-1) (Follow a Plan Down to Its Parts), [`UC-PLN-3`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-3) (Design a Plan's Process), [`UC-PLN-4`](../../../plan-marshall-documentation/doc/use-cases/03-plans.adoc#UC-PLN-4) (See What Runs, Across Projects) |
| **Specifications** | [`03-web-server.adoc#web-pairing`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-pairing), [`#fresh-code`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#fresh-code), [`#devices`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#devices), [`#two-listeners`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#two-listeners), [`#web-listener`](../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-listener) |
| **Local Client Design** | [`doc/design/web-app/README.adoc`](../design/web-app/README.adoc), [`doc/design/terminal-client/05-transcripts-entry.adoc`](../design/terminal-client/05-transcripts-entry.adoc) |
| **Roadmap Packages** | [`entry-contract.adoc`](../plans/implemented/entry-contract.adoc), [`web-app-foundation.adoc`](../plans/implemented/web-app-foundation.adoc) (delivered) |
| **Prerequisite Plans** | [`implemented/entry-contract-plan.md`](implemented/entry-contract-plan.md), [`implemented/web-onboarding-plan.md`](implemented/web-onboarding-plan.md), [`implemented/web-foundation-routing-and-catalogue-plan.md`](implemented/web-foundation-routing-and-catalogue-plan.md), [`implemented/mock-runtime-onboarding-and-entry-plan.md`](implemented/mock-runtime-onboarding-and-entry-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](runbook.md) |

### 1.1 Finding Analysis (verbatim finding -> root cause -> correction)

| # | Finding (trimmed) | Root cause in the current code | Correction in this plan |
|---|---|---|---|
| **1** | *Automatic logout / Drop of the connection: Why? As long the window is open, and the token is not revoked, it must be kept open* | `pm-mock-runtime/start.sh` starts the mock with no `--base`, so `MockRuntimeConfig.effectiveDeviceSecret()` generates a fresh `pmw_mock_secret_<uuid>` on every process. After any mock restart, the browser's stored `pm_device_secret` receives `401` on the next request or SSE reconnect. `ApiClient.request`, `requestWithConflict`, and `subscribeStream` clear the pairing and call `onRevoked('unauthorized')` on **any** `401`; `runtimeStore` then moves to `revoked`. In addition, `PairingView` calls the authenticated `GET /api/v1/status/connection` on a non-loopback page before a secret exists, so an unpaired phone is flipped to `revoked` by its own fingerprint probe. | Persist the mock device secret under a stable `PM_MCP_BASE`; only treat a `401` on an **authenticated** request as auth loss; never probe an authenticated route from the unpaired pairing page; share one runtime event stream so a duplicate stream cannot independently invalidate the session. |
| **2** | *The host (from the start-script) says localhost, so it can not be tested with a phone. Always use the actual system name as a host: Consider what works best here: ip vs. name* | `vite.config.ts` sets no `server.host`, so Vite binds loopback only. `pm-web-app/start.sh` and `MockRuntimeCli` print `localhost` URLs. `start.sh` has no notion of a reachable machine name, and Vite's host check would reject an unknown system name. | Bind Vite to all interfaces for development, allow the machine's real host names, resolve the best host (system name first, IP fallback) in `start.sh`, print `Local` and `Network`/`Paired` URLs, and update the mock CLI and READMEs. |
| **3** | *When I call http://localhost:5173/pair#code=KXQB-MRTN again, the code is not accepted a second time, why? No more valid?* | The code is single use by specification, and `MockRuntimeEngine.handlePairing` sets `currentPairingCode = null` after redemption (`03-web-server.adoc#web-pairing`). `runtime.ts#checkUrlHashForCode` redeems **before** checking `isPaired()`, so an already paired browser re-redeems a consumed code. The mock issues the next code only when the terminal client calls `POST /api/v1/web/pairings`; the web-only development loop has no terminal client, and `/mock/state` correctly reports an empty code after redemption, so `start.sh` prints no replacement. | Probe an existing valid pairing before redeeming (spec `#Redeem`: a paired browser does not redeem); add a mock control endpoint that returns the open code or issues a fresh one; make `start.sh` use it and print the replacement; state the single-use rule in the pairing error copy. |
| **4** | *A second connection using http://localhost:5173/pair#code=KXQB-MRTN does not work at all, blaming an invalid code. An issue of the mock system or a general failure?* | Same single-use rule as finding 3, plus no fresh code is surfaced in the web-only loop. On a phone/system-name URL the unpaired pairing page's authenticated status probe also returns `401`, and the browser is shown as revoked rather than as an invalid code. | Same fresh-code mechanism as finding 3, the unauthenticated-probe guard of finding 1, a second-browser Playwright test, and documentation that a second device needs a **fresh** code (never the consumed one). |
| **5** | *There is a banner saying the same like the header bar: connected to mock... What is the benefit?* | `+layout.svelte` renders `ConnectionStatusBanner` under the header for every paired browser. It repeats the header's machine identity with `Connected over loopback` / `Connected over the network`, a detail sentence, and a device count, consuming a full-width row for status that is either already implied by a usable app or available in Settings → Paired devices. The only non-redundant facts are the listener mode (loopback vs. network) and the paired-device count. | Remove the full-width banner. Move the listener mode and device count into the existing header machine badge as a `title` plus a mode marker, keeping the LAN-off view and `listener.changed` handling. Update the component test and catalogue copy. |
| **6** | *Clicking on the logo on the upper left does nothing. Should link to the start page* | The `.app-header__brand` is a plain `div` with a `span` logo and a `span` title; it has no click handler, link, or route. | Render the brand as a button/link that opens the operator's start page: the remembered start route when one is set, otherwise the default landing route (overview while work runs, projects otherwise). Give it an `aria-label` and a `title`; keep it inert while unpaired. Add the catalogue keys and tests. |
| **7** | *Actions, like button: Must all provide an advisory text that will be shown as hover (title attribute should be enough here)* | Only a handful of buttons set `title` (disabled reasons, the file picker, some source badges). The shared `Button.svelte` and the many native `<button>` elements carry no hover advisory; icon-only controls (theme, language, logo, favourite/star, list actions, modal closes) have no explanation at all. | Add a shared advisory layer: a Svelte action for `Button.svelte` plus an `installAdvisoryTitles()` observer for native buttons, filling `title` from an explicit `data-advisory`, an existing `title`/`aria-label`, or the visible label. Add specific hints for icon-only or ambiguous actions, catalogue keys in EN and DE, and source plus rendered tests that every action has a non-empty title. |
| **8** | *Where is the designer for plans? I cannot see / access them* | The `/designer` route and `ProcessDesigner` exist and work, but the only entry is a button inside `MachineSettingsView`. The header has no Designer action (the catalogue key `web.frame.nav.designer` is unused), and a plan view offers no route to design the plan's process. | Add a `Process Designer` header action visible while paired, and a `Design process` action in the plan header that opens `/designer` with the plan's project preselected. Carry the project in the designer route (`/designer?project=<id>`) so reload and bookmarks keep it. Add tests for the header, the plan action, and the preselection. |

> [!NOTE]
> The single-use rule and the bounded failed-redemption rule are security properties of the runtime, not defects. The defect is that the mock/development loop did not expose the replacement code and that the app re-redeemed from an already paired browser.

---

## 2. Scope & Objectives

### Part A: Keep the Web Session Alive (No Spurious Logout)

1. **Persist the mock web device secret across restarts**:
   - When no explicit secret is configured, `MockRuntime` reads `<PM_MCP_BASE>/state/mock-web-device-secret` (`0600`) and writes a generated secret there atomically on first start.
   - `pm-mock-runtime/start.sh` defaults to a stable `--base "$SCRIPT_DIR/target/machine"` unless the operator passes `--base`/`-b`. `rm -rf target/machine` remains the explicit way to start a fresh machine.
   - Add a `-d`/`--device-secret` CLI option for deterministic manual runs.
2. **Treat a `401` as auth loss only when a secret was actually sent**:
   - `ApiClient.request`, `requestWithConflict`, and the SSE reader must not clear a pairing or raise `onRevoked` for an unauthenticated request. An unauthenticated `401` means "this route needs pairing", not "the device was revoked".
3. **Never probe an authenticated route before pairing**:
   - `PairingView` reads the connection status only when `apiClient.isPaired()`; an unpaired LAN/phone page must not turn itself into `revoked`. The browser's TLS warning remains the fingerprint source before pairing.
4. **One authenticated runtime event stream**:
   - `ApiClient.subscribeEvents` shares a single `/api/v1/events` connection among subscribers (start on first listener, abort on last, keep the existing reconnect), so a duplicate `ConnectionStatusBanner` stream cannot independently observe a `401` and log the browser out.
5. **Clear the offline state when the connection returns**:
   - `ApiClient` reports a successful request and a successful SSE reconnect through a new `onOnline` callback; `runtimeStore` clears `isOffline`, so a transient network blip does not leave the red offline banner up until a manual reload.

### Part B: Reach the Development App from a Phone

1. **Bind Vite to the network**:
   - `pm-web-app/vite.config.ts` sets `server.host` (env `PM_WEB_HOST`, default all interfaces for development) and `server.allowedHosts` containing the machine's real host names (env `PM_WEB_ALLOWED_HOSTS`, otherwise `os.hostname()` and `<os.hostname()>.local`; IP addresses remain allowed by Vite).
2. **Resolve and print the real host**:
   - `pm-web-app/start.sh` resolves the reachable host name in this order: `PM_WEB_HOST`, macOS `LocalHostName.local`, `hostname -f`, `hostname`, primary IPv4 (`ipconfig getifaddr en0`/`en1` on macOS, `hostname -I` on Linux), `localhost`. It also resolves the primary IPv4 separately.
   - It prints `Local:` (loopback) and `Network:`/`Paired:` URLs using the real host name, plus a `Network (IP):` line when the primary IPv4 differs from the name. The name is preferred because it survives DHCP changes and works through mDNS/Bonjour; the IP is the fallback for networks where name resolution is blocked. When the only available host is `localhost`, the `Network` lines are omitted.
   - The already-running branch prints the same lines.
3. **Align the mock CLI**:
   - `MockRuntimeCli` prints the `Web App URL` with the machine's real host (env override included) and keeps the loopback listener line as the local coordinate.
4. **Document the development-only exposure**:
   - READMEs state that the Vite dev server is a development tool bound to the LAN on purpose, and that the real runtime keeps its loopback/LAN listener security.

### Part C: Make the Single-Use Pairing Lifecycle Understandable

1. **Do not redeem from an already paired browser**:
   - `checkUrlHashForCode` first probes `apiClient.getStatus()` when a secret is stored. On `200` it drops the fragment and returns to normal startup without consuming the code (the spec's `#Redeem` rule). On `401` it falls through to redemption. On a network error it keeps the fragment and reports offline, never redeeming blindly.
2. **Always have a fresh code available in development**:
   - Add `POST /mock/pairing` to `MockRuntimeEngine`: it returns the currently open code or issues a fresh one via the existing `handleCreatePairing`, without changing the scenario's single-use semantics.
   - `pm-web-app/start.sh` reads `/mock/state`; when the code is empty, it calls `POST /mock/pairing` and prints the fresh `Paired:` URL.
3. **Explain the refusal**:
   - `PairingView` maps `pairing_invalid` to a single-use explanation, `network_offline` to an offline explanation, and `unauthorized` to the existing pairing-lost copy.
   - Update `web.pair.page.invalid` (EN & DE) to say a code works once and a fresh one is needed. Add `web.pair.page.offline` (EN & DE) and its `catalogue.test.ts` key.
4. **Verify a second device**:
   - A Playwright test pairs one browser, verifies a second browser is refused the consumed code with the clear message, obtains a fresh code from `/mock/pairing`, and pairs the second browser successfully.

### Part D: Close the Web Chrome, Navigation & Action-Advisory Gaps

1. **Remove the redundant connection banner**:
   - Stop rendering the full-width `ConnectionStatusBanner` under the header. Move its two genuinely useful facts — listener mode (loopback vs. network) and paired-device count — into the existing header machine badge's `title` (and a `data-connection-mode` marker for styling). Keep the LAN-off view and `listener.changed` handling untouched.
   - Delete the banner markup/CSS and its component test; cover the header badge instead.
2. **Make the brand/logo open the start page**:
   - Turn `.app-header__brand` into a button/link. It opens the operator's remembered start route when one is set, otherwise the default landing route (overview while work runs, projects otherwise). It is inert while unpaired.
   - Give it a catalogue-driven `aria-label` and `title`; add the keys in EN and DE and test the navigation.
3. **Give every action a hover advisory**:
   - Add a shared advisory layer: a Svelte action for `Button.svelte` and an `installAdvisoryTitles()` observer for native `<button>` elements. `title` comes from an explicit `data-advisory`, an existing `title`/`aria-label`, or the visible label; it never overwrites an explicit title.
   - Add explicit hints for icon-only or ambiguous controls (theme, language, logo, favourite/star, list actions, modal closes) through catalogue keys in EN and DE.
   - Add a source guard for glyph-only buttons (no readable text) that lack `title`, `aria-label`, or `data-advisory`, and a rendered test that actions in the main shell and list views expose a non-empty `title` after the observer runs.
4. **Make the process designer discoverable**:
   - Add a `Process Designer` header action visible while paired, using the existing `web.frame.nav.designer` catalogue key.
   - Add a `Design process` action to the plan header that opens the designer with the plan's project preselected.
   - Carry the selection in the designer route (`/designer?project=<id>`), so reload, bookmark, and the browser's back keep it. Update `routes.ts`, `runtime.ts`, `AppRouter.svelte`, and `ProcessDesigner.svelte`.
   - Keep the existing machine-settings entry; the header and plan actions are additional, not replacements.

### Non-Goals (Explicitly Out of Scope)

- Changing the single-use pairing contract, the failed-redemption limit, or the device-secret format.
- Adding TLS, OAuth, or a LAN listener to the Vite development server.
- Changing the real runtime's `web.enabled`/`web.lan` listener configuration.
- Redesigning the views, the navigation model, or the visual language beyond the four chrome/navigation/advisory findings above.
- Turning every button label into a bespoke help sentence; the advisory layer uses the visible label as the fallback and specific hints only where the label alone is ambiguous.

---

## 3. Concurrency & Isolation

- **Branch**: `fix/web-session-pairing-and-dev-host` off latest `main`.
- **Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-pairing-dev-host`.
- **Parallelizable with**: nothing currently active; the plan touches both the web client and the mock runtime's development plumbing.

### Boundary Rules

- **Sole Owner of**:
  - `pm-web-app/src/lib/api/client.ts` and `client.test.ts`
  - `pm-web-app/src/lib/stores/runtime.ts` and a new `runtime.test.ts`
  - `pm-web-app/src/lib/views/PairingView.svelte` and `PairingView.scenarios.test.ts`
  - `pm-web-app/src/routes/+layout.svelte`
  - `pm-web-app/src/lib/components/ConnectionStatusBanner.svelte` and its test (removed or folded into the header badge)
  - `pm-web-app/src/lib/components/Button.svelte`
  - `pm-web-app/src/lib/utils/advisoryTitle.ts` and its test (new)
  - `pm-web-app/src/lib/views/PlanView.svelte` and `PlanView.test.ts`
  - `pm-web-app/src/lib/views/ProcessDesigner.svelte` and `ProcessDesigner.test.ts`
  - `pm-web-app/src/lib/views/AppRouter.svelte`
  - `pm-web-app/src/lib/routes.ts` and `routes.test.ts`
  - `pm-web-app/vite.config.ts`, `pm-web-app/start.sh`, `pm-web-app/README.md`
  - `pm-mock-runtime/src/main/java/de/planmarshall/mock/MockRuntime.java`, `MockRuntimeConfig.java`
  - `pm-mock-runtime/src/main/java/de/planmarshall/mock/cli/MockRuntimeCli.java`
  - `pm-mock-runtime/src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` (only the `/mock/pairing` control route)
  - `pm-mock-runtime/start.sh`, `pm-mock-runtime/README.adoc`
  - `pm-api/src/main/resources/messages/pairing.en.properties`, `pairing.de.properties`
  - `pm-api/src/main/resources/messages/common.en.properties`, `common.de.properties` (home/advisory/designer hints)
  - `pm-api/src/main/resources/messages/entry.en.properties`, `entry.de.properties` (header status title)
  - `pm-api/src/main/resources/messages/designer.en.properties`, `designer.de.properties` (plan-view entry hints)
  - the owning area catalogue (`overview`, `projects`, `reports`, `decisions`, `system`) for each new action hint
  - `pm-mock-runtime/src/test/java/de/planmarshall/mock/MockRuntimeTest.java`, `MockRuntimeCliTest.java`, `runtime/MockRuntimeEngineTest.java`
  - `pm-web-app/src/lib/i18n/catalogue.test.ts`
  - `pm-web-e2e/tests/pairing.spec.js` and a new browser test for the logo, designer, and action advisories
  - `README.md` and [`doc/agy/README.md`](README.md)
- **No Touch**:
  - Other web views (`OverviewView`, `ReportsView`, `SystemView`, `ProjectExplorerView`, etc.) except a test mock if a shared component changes.
  - `pm-operator` production verbs and the real runtime's pairing implementation.
  - Scenario JSON fixtures (the new code works with the existing `pairing_code` and `device.paired` contract).
  - `doc/plans/` (no new roadmap package is needed).

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [ ] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-session-pairing-dev-host` on branch `fix/web-session-pairing-and-dev-host` off latest `main`; Step 1 committed and pushed.
- [ ] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the web session, pairing, navigation, advisory layer, and Vite changes.
  - `pm-dev-frontend:css` for the header badge, banner removal, and hover/focus styling.
  - `plan-marshall-persona-security-expert` & `pm-dev-java:java-security` for the mock device-secret store, atomic `0600` publication, and the unauthenticated-probe boundary.
  - `plan-marshall-persona-module-tester` & `pm-dev-java:junit-core` for the mock-runtime tests.
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for Vitest and Playwright coverage.
  - `plan-marshall-persona-documenter` for the README and runbook-linked documentation.
  - `plan-marshall-persona-code-reviewer` for finding evaluation and adversarial refutation.
- [ ] 3. Incremental implementation with tests following the phased technical recipe.
- [ ] 4. Final completion report written (`doc/agy/web-session-pairing-and-dev-host-plan-report.md`), initial plan moved to `doc/agy/implemented/web-session-pairing-and-dev-host-plan.md`, and the dashboard updated (no `doc/plans` package applies; this is a finding-driven remediation). (Do NOT move the report to `verified/` — Step 8 is reserved for the orchestrator.)
- [ ] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`; required because this plan changes Java, TypeScript, and shell files).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on a worker PR), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-session-pairing-and-dev-host-plan-report.md` by the orchestrator session.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Session Persistence** | `plan-marshall-persona-implementer` | `pm-dev-java:java-security`<br>`pm-dev-frontend:javascript` | Atomic `0600` secret store, stable base, authenticated-only `401` handling, pre-pairing guard, one event stream, offline recovery. |
| **Phase 2: Reachable Dev Host** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-documenter` | Vite network binding, `allowedHosts`, host resolution order, printed URLs, README accuracy. |
| **Phase 3: Pairing Lifecycle & UX** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-security-expert` | Already-paired probe, mock control endpoint, fresh-code display, single-use copy, second-device behavior. |
| **Phase 4: Web Chrome, Navigation & Action Advisories** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`pm-dev-frontend:css` | Banner removal and header status, clickable brand, shared advisory layer, designer header/plan entry, route-carried project. |
| **Phase 5: Unit, Java & E2E Verification** | `plan-marshall-persona-module-tester` | `pm-dev-java:junit-core`<br>`pm-dev-frontend:jest-testing` | Secret persistence tests, `401` matrix tests, Vitest pairing/chrome tests, two-browser and navigation Playwright tests. |
| **Phase 6: Review Gate** | `plan-marshall-persona-code-reviewer` | `plan-marshall-ref-code-quality` | Adversarial review of the security boundary, the single-use contract, the dev-only network exposure, and the global advisory mechanism. |

---

## 6. Phased Technical Recipe

### Phase 1: Keep the Web Session Alive

- [ ] **1.1 Persist the mock device secret in a stable base**:
  - In `MockRuntime.start` (after `setupFilesystem`), resolve the effective device secret as follows: an explicit `MockRuntimeConfig.deviceSecret` wins; otherwise read `<PM_MCP_BASE>/state/mock-web-device-secret`; if absent, generate it with `config.effectiveDeviceSecret()` and write it atomically (`*.tmp` in the same directory, mode `0600`, then `ATOMIC_MOVE`).
  - Keep the auto-created temporary base's cleanup behavior unchanged. The persistence matters only for an explicit stable base, which `start.sh` now supplies.
  - In `pm-mock-runtime/start.sh`, detect an explicit `--base`/`-b`; when absent, append `--base "$SCRIPT_DIR/target/machine"` (creating the directory). Document `rm -rf "$SCRIPT_DIR/target/machine"` as the "fresh machine" action.
  - Add `-d`/`--device-secret <secret>` to `MockRuntimeCli` and its usage text; it feeds `MockRuntimeConfig.deviceSecret` and bypasses the persisted file.
  - Tests: `MockRuntimeTest` starts two runtimes on the same explicit base, asserts `runtime.deviceSecret()` is equal, asserts the file is `rw-------`, and asserts a web request with the first secret still returns `200` after the restart. Add a test that an explicit config secret overrides the file.
- [ ] **1.2 Only an authenticated `401` is auth loss**:
  - In `ApiClient.request`, capture the secret that was sent. On `401`: if `secret` is non-null, `clearAuth()` and `onRevoked('unauthorized')`; if `secret` is null, throw `unauthorized` without touching storage or the store. Apply the same rule in `requestWithConflict` and in `subscribeStream`.
  - Tests in `client.test.ts`: an unauthenticated `401` leaves a stored secret untouched and does not call `onRevoked`; an authenticated `401` still clears and calls it.
- [ ] **1.3 Never probe an authenticated route before pairing**:
  - In `PairingView.svelte`, call `getConnectionStatus()` only when `apiClient.isPaired()`; otherwise leave `status` null. Add a comment that an unpaired page has no device secret and must not turn a required-auth `401` into a revocation.
  - Update `PairingView.scenarios.test.ts`: the network-fingerprint test arranges a stored secret; add a test that an unpaired non-loopback render makes no `getConnectionStatus` call and never sets `isRevoked`.
- [ ] **1.4 Share one runtime event stream**:
  - Refactor `ApiClient.subscribeEvents` to keep a listener set and one `/api/v1/events` connection: start on the first listener, abort on the last, keep the existing 2-second reconnect and `stream.closed` handling, and route the auth-loss callback through 1.2 once.
  - Keep `subscribeEntryEvents` per-entry and unchanged.
  - Tests: two subscribers cause one `fetch` to `/api/v1/events`; both receive an event; the last unsubscribe aborts the reader; a `401` on the shared stream triggers one revocation.
  - `ConnectionStatusBanner` keeps its existing public calls (`subscribeEvents`, `getConnectionStatus`) and needs no behavioral change.
- [ ] **1.5 Recover from a transient connection drop**:
  - Add `setOnOnline(callback)` to `ApiClient` and call it on every successful `request` and `requestWithConflict` response and after a successful SSE connect/reconnect. Do not call it on an error response.
  - In `runtime.ts#initRuntime`, register `apiClient.setOnOnline(() => runtimeStore.update((s) => ({ ...s, isOffline: false })))` beside the existing `setOnRevoked`/`setOnOffline` registrations.
  - Tests in `client.test.ts`: `onOffline` is called on a rejected fetch; a subsequent successful `getStatus` calls `onOnline`; a stream reconnect calls it too. In the store test, an offline state returns to online after `onOnline`.

### Phase 2: Reach the Development App from a Phone

- [ ] **2.1 Bind Vite to the network and allow the real host**:
  - In `pm-web-app/vite.config.ts`, add `import os from 'node:os'` and set:
    - `server.host: process.env.PM_WEB_HOST ?? true` (default: all interfaces for development).
    - `server.allowedHosts: process.env.PM_WEB_ALLOWED_HOSTS?.split(',').map((h) => h.trim()).filter(Boolean) ?? [os.hostname(), \`${os.hostname()}.local\`]`.
  - Keep `server.port: 5173`, the `/api` and `/mock` proxies to `127.0.0.1:7420`, and `server.fs.allow: ['..']` unchanged. Add a comment that this is a development-only server and the proxy keeps the mock's listener on loopback.
- [ ] **2.2 Resolve and print the real host in `pm-web-app/start.sh`**:
  - Add `reachable_host()` before `web_port_from_args`: `PM_WEB_HOST` -> `scutil --get LocalHostName` + `.local` -> `hostname -f` (when it is not `localhost`) -> `hostname` -> `primary_ipv4` -> `localhost`.
  - Add `primary_ipv4()`: macOS tries `ipconfig getifaddr en0` then `en1`; Linux uses `hostname -I | awk '{print $1}'`; empty on failure.
  - Print in both the already-running and the start branches:
    ```text
    Local:       http://localhost:<port>
    Network:     http://<host>:<port>
    Network (IP): http://<ipv4>:<port>
    Paired:      http://<host>:<port>/pair#code=<code>
    ```
    Print `Network (IP)` only when `<ipv4>` is non-empty and differs from `<host>`; omit the `Network`/`Paired` lines when `<host>` is `localhost` and no code is available. Prefer the name for `Paired`; use the IP only when no name resolves. `print_paired_url_if_available` uses the resolved name, not `localhost`.
  - Keep `--host` passthrough to Vite if the operator supplies it; the config default already binds the network.
  - Follow the shell invariants of [`doc/agy/runbook.md`](runbook.md): Bash 3.2-safe empty-array expansion, no direct empty-array expansion under `set -u`, and atomic temporary-file publication where the script writes shared state.
- [ ] **2.3 Align `MockRuntimeCli` and its test**:
  - Add a small `systemHost()` helper in `MockRuntimeCli`: `PM_WEB_HOST` -> `InetAddress.getLocalHost().getCanonicalHostName()` -> `getHostName()` -> `localhost`, with a `try/catch` fallback.
  - Print `Web App URL: http://<host>:5173/pair#code=<code>` and keep the loopback listener line (`http://127.0.0.1:<port>`) as the local coordinate.
  - Update `MockRuntimeCliTest` to assert the URL matches `http://[^/]+:5173/pair#code=KXQB-MRTN` and does not hardcode `localhost`.
- [ ] **2.4 Update the development documentation**:
  - `pm-web-app/README.md`: add a "Reaching the app from a phone" subsection with the `start.sh` `Local`/`Network`/`Paired` output, the system-name-vs-IP rationale (name is stable across DHCP changes; IP is the fallback), the LAN binding, and the single-use code note.
  - `pm-mock-runtime/README.adoc` and the root `README.md`: update the example `Web App URL` to the system host and document `POST /mock/pairing`.

### Phase 3: Make the Single-Use Pairing Lifecycle Understandable

- [ ] **3.1 Do not redeem from an already paired browser**:
  - In `runtime.ts#checkUrlHashForCode`, when `apiClient.isPaired()`:
    - Probe `apiClient.getStatus()`.
    - On success: `history.replaceState` removes the fragment, clear `pairingFailed`, and return `false` so normal startup continues. Do **not** call `pairBrowser`.
    - On `unauthorized`: fall through to redemption (the stored secret is no longer accepted).
    - On any other error (offline): keep the fragment, mark `isOffline`, and return `false` without redeeming.
  - Add `runtime.test.ts`: a valid paired secret never calls `pairBrowser` and removes the fragment; a `401` falls through to `pairBrowser`; a network error keeps the fragment and does not redeem.
- [ ] **3.2 Add the mock fresh-code control endpoint**:
  - In `MockRuntimeEngine.handleControlRequest`, add `POST /mock/pairing` -> `handleCreatePairing(response)`. It returns the currently open code, or a fresh deterministic code when the previous one was consumed. It changes no scenario state and keeps the single-use rule.
  - Tests in `MockRuntimeEngineTest` (or `EntryScenarioTest`): after a redemption, `POST /mock/pairing` returns a code different from the consumed one; the consumed code still yields `400 pairing_invalid`; the fresh code pairs successfully.
  - Document the endpoint in `pm-mock-runtime/README.adoc` under `Control Endpoints`.
- [ ] **3.3 Make `start.sh` print the replacement code**:
  - In `print_paired_url_if_available`, after reading `/mock/state`: if `pairing_code` is empty, `curl -s -m 1 -X POST http://127.0.0.1:7420/mock/pairing` and parse the `code` member. Use the returned code for the `Paired:` line.
  - The call is idempotent while a code is open; repeated `start.sh` runs do not invalidate an unconsumed code.
- [ ] **3.4 Explain the refusal in the pairing UI**:
  - In `PairingView.svelte`, map the caught error: `network_offline` -> `web.pair.page.offline`; `unauthorized` -> the existing pairing-lost copy; everything else -> `web.pair.page.invalid`. Keep the `pairingFailed` path on `web.pair.page.invalid`.
  - Update `pm-api/src/main/resources/messages/pairing.en.properties` and `pairing.de.properties`:
    - `web.pair.page.invalid`: a code works once; get a fresh one in a terminal of the machine.
    - `web.pair.page.offline`: the machine cannot be reached right now; check the connection and try again.
  - Add the new key to `pm-web-app/src/lib/i18n/catalogue.test.ts`.
  - Update `PairingView.scenarios.test.ts`: the existing invalid-code test asserts the single-use copy; add an offline test; add the no-authenticated-probe test from 1.3.
- [ ] **3.5 Prove the second-device behavior end to end**:
  - In `pm-web-e2e/tests/pairing.spec.js`, add:
    1. **Already paired re-open**: pair via `/#code=KXQB-MRTN`, then navigate again to the same URL; the app must stay in the start view, the fragment is removed, and neither `pairing-error` nor the revoked view appears.
    2. **Second browser, consumed code**: open a second `browser.newContext()` and navigate to the consumed code; expect the single-use `pairing-error` message, not the revoked view.
    3. **Second browser, fresh code**: `POST /mock/pairing` for a fresh code, pair the second context, and assert the app header/machine line appears.
  - Keep the existing scenario reset in `beforeEach` so the initial code is deterministic.

### Phase 4: Close the Web Chrome, Navigation & Action-Advisory Gaps

- [ ] **4.1 Remove the redundant connection banner and move its two useful facts into the header**:
  - Remove `<ConnectionStatusBanner />` from `pm-web-app/src/routes/+layout.svelte` and delete the full-width banner markup/CSS (or reduce the component to a headless status helper). Keep the status loading, the 30-second heartbeat, and the `listener.changed` / `device.paired` / `device.revoked` subscriptions.
  - Render the listener mode and paired-device count in the existing `.app-header__machine` badge: `title` carries the `entry.connection.loopback.detail` / `entry.connection.lan.detail` text plus `entry.connection.devices`, and `data-connection-mode="loopback|lan"` drives a small colour marker. The header remains the only always-on status row.
  - Keep the LAN-off route/view and the listener-closed handling unchanged.
  - Tests: replace `ConnectionStatusBanner.test.ts` with a header-status test that asserts the title and mode; update any scenario/E2E expectation that looked for `connection-status-banner`.
- [ ] **4.2 Make the brand/logo open the start page**:
  - In `+layout.svelte`, turn `.app-header__brand` into a `<button type="button">` (or an anchor that calls the same handler) with `aria-label` and `title` from the catalogue. Add `web.frame.home` and `web.frame.home.hint` in EN and DE.
  - The handler opens the operator's start page: `rememberedStartRoute()` when set, otherwise the default landing route (`overview` while work runs or waits, `projects` otherwise). Use the existing `landingRoute`/`hasWork` logic rather than a new rule.
  - Keep the brand non-interactive while the browser is unpaired (pairing, onboarding, revoked, listener-off) and when it would land on the route already shown.
  - Tests: a layout test asserts a paired brand click navigates to the remembered/default route; an unpaired brand exposes no action.
- [ ] **4.3 Give every action a hover advisory**:
  - Add `pm-web-app/src/lib/utils/advisoryTitle.ts` with:
    - `advisory(node, text?)`: sets `title` from the explicit text, else an existing `title`, else `aria-label`, else the trimmed visible label; never overwrites an explicit `title`.
    - `installAdvisoryTitles(root = document)`: a `MutationObserver` that applies the same rule to every current and future `<button>` and disconnects cleanly.
  - Use `use:advisory` in `Button.svelte` so shared buttons are covered immediately; call `installAdvisoryTitles()` from `+layout.svelte` on mount and disconnect on destroy so native buttons and later-rendered views are covered.
  - Add `data-advisory={$t('...')}` to icon-only or ambiguous controls: header theme/language, the brand, favourite/star toggles, list-row actions, modal close buttons, and any glyph-only control. Use visible labels as the fallback everywhere else.
  - Add the new hint keys to the EN and DE catalogues (common/overview/designer areas as appropriate) and to `catalogue.test.ts`.
  - Tests: `advisoryTitle.test.ts` covers explicit text, `aria-label`, visible-label fallback, no-overwrite, and dynamic insertion; a source guard fails any glyph-only button (no readable text) that lacks `aria-label`, `data-advisory`, or `title`; rendered tests for the shell and the main list views assert every button has a non-empty `title` after the observer runs.
- [ ] **4.4 Make the process designer discoverable**:
  - Add a `Process Designer` header action in `+layout.svelte`, visible while paired, using `web.frame.nav.designer`, with `aria-label`/`title`. It calls `navigateTo('designer')`.
  - Add a `Design process` action to `PlanView.svelte`'s header actions. It calls `navigateTo('designer', { projectId: plan.project.project_id })`.
  - Extend the designer route: `pathForRoute('designer', { projectId })` returns `/designer?project=<id>`; `adoptRouteFromLocation` reads the `project` query for the designer; `runtimeStore` carries `designerProjectId`; `AppRouter.svelte` passes it to `ProcessDesigner.svelte` as a prop; the designer uses it as the initial `projectId` when no draft overrides it.
  - Keep the existing `MachineSettingsView` designer button. Add `designer.plan.open` / `designer.plan.hint` catalogue keys in EN and DE.
  - Tests: `routes.test.ts` covers the designer query; `PlanView.test.ts` asserts the action navigates with the plan's project; `ProcessDesigner.test.ts` asserts the preselected project; a Playwright test opens the designer from the header and from a plan and survives a reload.
- [ ] **4.5 Browser-level verification for the chrome findings**:
  - Add a Playwright spec (or extend the existing overview/navigation spec) that asserts:
    1. The full-width connection banner is gone and the header machine badge carries the connection title/mode.
    2. The brand click reaches the start page.
    3. Header, list, and modal actions expose a non-empty `title` on hover.
    4. The header `Process Designer` action and the plan `Design process` action open `/designer` (with `?project=` from a plan).

### Phase 5: Verification & Archival

- [ ] **5.1 Targeted tests**:
  - `./mvnw -q test -pl pm-mock-runtime -am` (secret persistence, CLI URL, control endpoint, existing entry/pairing tests).
  - `cd pm-web-app && npm test` (client `401` matrix, shared stream, `checkUrlHashForCode`, pairing view, advisory layer, chrome/navigation, catalogue guard).
  - `cd pm-web-e2e && npx playwright test tests/pairing.spec.js` (already-paired re-open, second browser, fresh code) plus the chrome/navigation spec (banner, brand, advisories, designer).
- [ ] **5.2 Full pre-commit gate**:
  - `./mvnw verify -Ppre-commit` with `BUILD SUCCESS` and a clean `git status`.
- [ ] **5.3 Manual evidence for the findings**:
  - Pair in a browser, stop `pm-mock-runtime`, start it again via `pm-mock-runtime/start.sh`, reload the browser: the session remains paired (no revoked page).
  - Run `pm-web-app/start.sh` and open the printed `Network`/`Paired` URL from a phone on the same network; pair and reach the start view.
  - Re-open the same `#code=` URL in the paired browser: it stays in the app.
  - Use the printed fresh code in a second browser/phone and pair it; the consumed code remains refused with the single-use message.
  - Confirm the full-width connection banner is gone, the header badge carries the connection title/mode, the logo opens the start page, hover text appears on header/list/modal actions, and the designer opens from the header and from a plan.
- [ ] **5.4 Documentation and report**:
  - Update `pm-web-app/README.md`, `pm-mock-runtime/README.adoc`, and the root `README.md` with the final output, the single-use/fresh-code explanation, and the new header/designer/advisory behavior.
  - Write `doc/agy/web-session-pairing-and-dev-host-plan-report.md` with the eight findings, their root causes, delivered artifacts, test evidence, and the manual phone/session/chrome checks.
  - Mark the plan done in [`doc/agy/README.md`](README.md) when archiving, and move it to `doc/agy/implemented/`.

---

## 7. Acceptance Criteria (Mapped to the Findings)

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **1. Session drop** | A paired browser survives a mock-runtime stop/start when launched via `pm-mock-runtime/start.sh`; no `revoked` route, no re-pair, and background polls/SSE continue with the same secret. | `MockRuntimeTest` secret persistence test + manual restart check. |
| **1. Session drop** | An unauthenticated `401` (e.g. an unpaired LAN pairing page) never clears the pairing or sets `isRevoked`; an authenticated `401` still does. | `client.test.ts` `401` matrix + `PairingView` no-probe test. |
| **1. Session drop** | A transient request/SSE failure sets the offline banner, and the next successful request or stream reconnect clears it without a reload. | `client.test.ts` `onOnline` test + `runtime.ts` store wiring. |
| **2. localhost host** | `start.sh` prints a `Network`/`Paired` URL using the machine's real host name and a `Network (IP)` fallback when the IP differs; Vite accepts that host, and a phone on the same network opens and pairs. | `start.sh` output + manual phone check + Playwright remains green on `localhost`. |
| **3. Code re-use** | Re-opening the `#code=` URL in the already paired browser never re-redeems; the browser stays in the app. | `runtime.test.ts` + Playwright "already paired re-open". |
| **3. Code re-use** | After a redemption, `start.sh` prints a fresh code; the consumed code remains refused with a single-use message. | `MockRuntimeEngineTest` control endpoint test + Playwright consumed-code test. |
| **4. Second connection** | A second browser with the consumed code sees the single-use explanation, and pairs successfully with the fresh code from `start.sh`/`POST /mock/pairing`. | Playwright two-browser test + manual phone check. |
| **5. Banner duplication** | The full-width connection banner is gone; the header machine badge carries the listener mode and paired-device count as a `title` and mode marker, and the LAN-off view still works. | Header status test + Playwright chrome check. |
| **6. Inert logo** | Clicking the brand opens the operator's start page (remembered or default landing) and does nothing while unpaired. | Layout test + Playwright brand check. |
| **7. Missing hover advisories** | Every action button exposes a non-empty `title` on hover; icon-only/glyph-only controls carry a meaningful hint; the source guard rejects a glyph-only button without one. | `advisoryTitle.test.ts` + source guard + rendered/Playwright checks. |
| **8. Hidden designer** | The header offers `Process Designer`, a plan offers `Design process`, and both open `/designer` with the plan's project preselected and carried in the URL. | `routes.test.ts` + `PlanView.test.ts` + `ProcessDesigner.test.ts` + Playwright designer check. |

---

## 8. Risk & Rollback Notes

- **Dev-only LAN exposure**: binding Vite to all interfaces exposes the development app and its source maps to the local network. This is intentional for phone testing, is documented, and does not change the real runtime's listeners. If the exposure is unacceptable, set `PM_WEB_HOST=127.0.0.1`; the host resolver then prints only the local URL.
- **Secret persistence file**: the mock stores a development secret in `target/machine/state/` with `0600`. It is not a production credential, and `mvn clean`/`rm -rf target/machine` removes it.
- **Single-use contract**: the plan does not relax the single-use rule. A reviewer must reject any change that makes a consumed code redeemable again; the correct recovery is always a fresh code.
- **Already-paired probe**: the probe adds one authenticated `GET /api/v1/status` before normal startup when the URL carries a code. It must not consume the code and must not fail startup when the runtime is offline; the fragment is kept for a later retry.
- **Fingerprint before pairing**: the unpaired pairing page no longer calls the authenticated `GET /api/v1/status/connection`, because a `401` there was the phone's spurious revocation path. On the real LAN listener the browser's certificate warning carries the fingerprint before pairing; an unauthenticated fingerprint read on the pairing page would need a specification change and is a follow-up, not a regression introduced silently here.
- **Banner removal**: the only facts that leave the always-on shell are the listener mode and the paired-device count; both remain in the header badge's `title`. The LAN-off view and the listener-closed route are unchanged, so a loss of network access still has its dedicated page.
- **Global advisory observer**: the observer must never overwrite an explicit `title`, must prefer `data-advisory` and `aria-label` over visible text, and must disconnect on destroy. It is a safety net, not a replacement for meaningful hints on icon-only controls.
- **Designer route**: `/designer?project=<id>` must be readable on reload and through the browser's back. The project is a UI selection, not a new authorization scope; the existing device secret remains the only access control.
- **Brand navigation**: the brand click must not push a duplicate history entry when it targets the route already shown, and must stay inert while unpaired or in the revoked/listener-off states.

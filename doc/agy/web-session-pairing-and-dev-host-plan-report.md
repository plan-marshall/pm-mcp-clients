# Completion Report: `web-session-pairing-and-dev-host`

## Executive Summary

The eight live findings in [`doc/findings-web.txt`](../findings-web.txt) are resolved across `pm-web-app`, `pm-mock-runtime`, `pm-api` (catalogue), `pm-web-e2e`, and the development launch scripts. The plan's security contract is preserved: a pairing code is still single use, the bounded failed-redemption rule is unchanged, and the real runtime's `web.enabled`/`web.lan` listener configuration is untouched. What changed is the surrounding development experience:

- **Sessions survive a mock restart.** The mock keeps its web device secret on a stable base, and the browser treats a `401` as auth loss only when it actually sent a secret.
- **The app is reachable from a phone.** Vite binds the network for development, accepts the machine's real names, and `start.sh` prints `Local`, `Network`, `Network (IP)`, and a working `Paired` URL.
- **Single-use codes are understandable.** An already paired browser probes instead of re-redeeming, the mock issues a replacement code through `POST /mock/pairing`, `start.sh` prints it, and the pairing page explains the single-use and offline cases.
- **The chrome gaps are closed.** The duplicate banner is gone, the brand opens the start page, every action explains itself on hover, and the process designer is reachable from the header and from a plan.

## Delivered Artifacts

### `pm-mock-runtime`

- `src/main/java/de/planmarshall/mock/MockRuntime.java` — resolves the device secret from an explicit config, else `<base>/state/mock-web-device-secret`, else generates one and publishes it atomically with `0600`, then `ATOMIC_MOVE`.
- `src/main/java/de/planmarshall/mock/cli/MockRuntimeCli.java` — `-d`/`--device-secret`, a machine-host `Web App URL`, and a `systemHost()` helper (`PM_WEB_HOST` → local host name → `localhost`, without a reverse lookup).
- `src/main/java/de/planmarshall/mock/runtime/MockRuntimeEngine.java` — `POST /mock/pairing`, returning the open code or issuing a fresh one through the existing `handleCreatePairing`, without touching scenario state.
- `start.sh` — detects an explicit `--base`; otherwise supplies a stable base (`target/machine`, or a short stable temp base when the checkout path is too deep for the Unix socket), and documents `rm -rf target/machine` as the fresh-machine action.
- `README.adoc` — machine-host example URL, stable base and fresh-code notes, `POST /mock/pairing` under Control Endpoints.
- Tests: `MockRuntimeTest` (secret persistence across two starts, file mode, old secret still `200`, explicit override), `MockRuntimeCliTest` (`-d` parsing, machine-host URL), `MockRuntimeEngineTest` (fresh code after redemption, idempotence, consumed code still `400`).

### `pm-web-app`

- `src/lib/api/client.ts` — one shared `/api/v1/events` connection; `401` clears and revokes only when a secret was sent; `setOnOnline` reports every successful request and SSE connect.
- `src/lib/stores/runtime.ts` — `checkUrlHashForCode` probes an existing pairing before redeeming, keeps the fragment when offline, and falls through only on `401`; `onOnline` clears the offline state; `openStartPage`; `designerProjectId` carried by the designer route.
- `src/lib/views/PairingView.svelte` — no authenticated probe before pairing; single-use, offline, and pairing-lost error mapping.
- `src/routes/+layout.svelte` — banner removed, connection mode/device count in the header badge `title` with `data-connection-mode`, clickable brand with `title`/`aria-label`, header `Process Designer` action, shared advisory observer installed and disconnected.
- `src/lib/utils/advisoryTitle.ts` — Svelte action and `installAdvisoryTitles` observer (`data-advisory` → `title` → `aria-label` → visible label, never overwriting an explicit title).
- `src/lib/components/Button.svelte` — `use:advisory`; `ConnectionStatusBanner.svelte` and its test removed.
- `src/lib/views/PlanView.svelte` — `Design process` action opening `/designer?project=<id>`; `AppRouter.svelte` and `ProcessDesigner.svelte` accept the preselected project.
- `src/lib/routes.ts` — designer query path; `routes.test.ts`, `layout.test.ts`, `runtime.test.ts`, `advisoryTitle.test.ts`, `PlanView.test.ts`, `ProcessDesigner.test.ts`, `PairingView.scenarios.test.ts` extended.
- `vite.config.ts` — network host, allowed host names (the `PM_WEB_HOST` name, the DNS name, the short name, and both `.local` forms, plus `PM_WEB_ALLOWED_HOSTS` additions), and a `/mock` proxy bypass that refuses non-loopback clients so the development server's LAN exposure does not expose the mock's unauthenticated control routes.
- `start.sh` — host resolution order, `primary_ipv4`, printed `Local`/`Network`/`Network (IP)`/`Paired`, and the `/mock/pairing` fallback for a consumed code.
- `README.md` — phone-reaching section, printed output, name-vs-IP rationale, LAN exposure note, single-use note, header/designer/advisory behavior.

### `pm-api` (message catalogue, EN & DE)

- `pairing.en/de.properties` — single-use `web.pair.page.invalid`, new `web.pair.page.offline`.
- `common.en/de.properties` — `web.frame.home`, `web.frame.home.hint`.
- `designer.en/de.properties` — `designer.plan.open`, `designer.plan.hint`.

### `pm-web-e2e`

- `tests/pairing.spec.js` — already-paired re-open, second browser refused the consumed code with the single-use message, second browser paired with the fresh code.
- `tests/entry.spec.js` — header badge replaces the removed banner assertion.
- `tests/chrome.spec.js` (new) — no banner, header mode/title, hover advisories in the header and list, brand navigation, header and plan designer entries with the project surviving a reload.

### Documentation

- Root `README.md`, `pm-mock-runtime/README.adoc`, `pm-web-app/README.md`.
- This report; the plan is archived at [`implemented/web-session-pairing-and-dev-host-plan.md`](implemented/web-session-pairing-and-dev-host-plan.md).

## Finding-by-Finding Resolution

| # | Root cause | Correction delivered | Evidence |
|---|---|---|---|
| **1** | Fresh mock device secret per start; every `401` treated as revocation; unpaired page probed an authenticated route; two event streams | Stable `0600` secret store; authenticated-only `401`; no pre-pairing probe; one shared event stream; `onOnline` clears offline | `MockRuntimeTest`, `client.test.ts`, `runtime.test.ts`, `PairingView.scenarios.test.ts`, manual restart check |
| **2** | Vite loopback-only; `localhost` in printed URLs; no allowed host | Network bind, real-name allow list, host resolution and `Local/Network/Network (IP)/Paired` output, CLI machine host | `start.sh` output, `MockRuntimeCliTest`, Playwright on `localhost`, manual `curl` through the network name |
| **3** | Single-use code redeemed again from a paired browser; no replacement in the web-only loop | Probe before redeem; `POST /mock/pairing`; `start.sh` prints the replacement | `runtime.test.ts`, `MockRuntimeEngineTest`, Playwright re-open and fresh-code tests |
| **4** | Same single-use rule, plus no fresh code surfaced and a spurious `401` on a phone | Fresh-code mechanism, pre-pairing guard, second-browser coverage, documentation | Playwright two-browser tests, `PairingView` no-probe test |
| **5** | Full-width banner repeated the header | Banner removed; listener mode and device count in the header badge `title` and `data-connection-mode`; heartbeat and listener/device subscriptions kept | `layout.test.ts`, `entry.spec.js`, `chrome.spec.js` |
| **6** | Inert brand `div` | Brand button with catalogue `aria-label`/`title`, opening the remembered or default start page; inert while unpaired and on listener-off | `layout.test.ts` (paired click and unpaired inert), `chrome.spec.js` |
| **7** | No hover advisory on most buttons | Shared advisory action + observer; explicit advisories for header theme/language/designer/brand; fallbacks from `aria-label` and visible text | `advisoryTitle.test.ts` (source guard, dynamic insertion), `layout.test.ts`, `chrome.spec.js` |
| **8** | Designer only reachable from machine settings | Header `Process Designer` action; plan `Design process` action; `/designer?project=<id>` carried through reload/back | `routes.test.ts`, `PlanView.test.ts`, `ProcessDesigner.test.ts`, `chrome.spec.js` |

## Test & Verification Results

### Unit, component, and module tests

- `pm-web-app`: `npm test` — **52 files, 437 tests passed**.
- `npm run check` — **0 errors, 0 warnings**; `npm run lint` — clean.
- `npm run format:check` was checked for the changed files and they are Prettier-clean.
- `pm-mock-runtime`: `./mvnw -q test -pl pm-mock-runtime -am` — **all tests passed** (415 tests in the reactor for that module's run), including the new secret-persistence, CLI, and `/mock/pairing` tests.

### End-to-end (Playwright, Chromium)

Executed against the mock runtime started by the suite's global setup and the Vite development server:

- `tests/pairing.spec.js`, `tests/chrome.spec.js`, `tests/entry.spec.js`, `tests/designer.spec.js`, `tests/mobile.spec.js` — **all passed** (14/14 in the final targeted run; designer 8/8 and entry/mobile in their runs).
- The added coverage: paired re-open, second browser with the consumed code, second browser with the fresh code, header status badge, brand navigation, header/list hover advisories, header/plan designer entry with `?project=` surviving reload.

> **Pre-existing E2E drift (not introduced here):** the full suite still has failures in specs that were already stale against the consolidated scenarios — e.g. `git-sources.spec.js` pairs with `GTSR-CMTR` and `decomposition.spec.js` with `DCMP-2606`, while the consolidated `projects-and-start` scenario serves `KXQB-MRTN`; `start.spec.js` uses a `/plan-marshall/i` locator that now matches three project tabs. Those specs and scenarios are untouched by this plan. All specs touched or added by this plan pass.

### Full pre-commit gate

```
./mvnw verify -Ppre-commit
...
pm-api ............. SUCCESS
pm-exec ............ SUCCESS
pm-mock-runtime .... SUCCESS
pm-relay ........... SUCCESS
pm-operator ........ SUCCESS
pm-web-app ......... SUCCESS
pm-web-e2e ......... SUCCESS
BUILD SUCCESS   (07:33 min)
```

The working tree was checked before and after: the intended 38-file footprint is byte-identical to the change set, and only this plan's files are modified. The pre-commit profile's OpenRewrite pass also reformats 226 untouched `pm-api`/`pm-operator` files in this checkout (pre-existing drift between `main` and the parent POM's import recipe); those unrelated rewrites were reverted and are **not** part of this change.

## Domain Invariants

| Invariant | Status |
|---|---|
| Java 25 toolchain | Preserved (no language/POM change) |
| No uncatalogued user-facing strings | Preserved; new keys in EN & DE; `catalogue.test.ts` passes |
| Bilingual catalogue parity | `catalogue.test.ts` passes; EN/DE key sets match |
| Single-use pairing contract | Unchanged and verified: consumed code stays `400 pairing_invalid` |
| Real runtime listener security | Untouched; only the Vite development server binds the LAN |
| Mock runtime completeness | Stable base, `0600` secret, `/mock/pairing` covered by Java tests |
| Responsive layout down to 320 px | `mobile.spec.js` passes; header/badge markup wraps as before |
| Module boundaries | `pm-operator` verbs and the real runtime pairing implementation untouched; scenario JSON untouched |

## Manual Evidence

Recorded against the real launch scripts on this machine:

1. **Mock restart keeps the session** — started `pm-mock-runtime/start.sh --scenario web-pairing --web-port 7420`, paired with `KXQB-MRTN`, confirmed the consumed code returned `400`, obtained `CKKK-KKKK` from `POST /mock/pairing`, stopped and restarted the mock, and received `200` for `GET /api/v1/status` with the **old** device secret. The printed base was identical across the restart.
2. **Phone-usable URLs** — `pm-web-app/start.sh` printed:
   ```text
   Local:        http://localhost:5173
   Network:      http://Mac-Mini.local:5173
   Network (IP): http://192.168.178.126:5173
   Paired:       http://Mac-Mini.local:5173/pair#code=KXQB-MRTN
   ```
   Both the name URL and the IP URL answered `200`. (The first run exposed that `os.hostname()` is the DNS name while `start.sh` prefers the short Bonjour name; the allowed-host list now carries both, and the re-run answered `200`.)
3. **Fresh code after redemption** — `POST /mock/pairing` returned a new code while the consumed `KXQB-MRTN` stayed refused.
4. **Browser behavior** — covered by the Playwright runs above: paired re-open stays in the app, a second browser gets the single-use message for the consumed code and pairs with the fresh one, the banner is absent, the badge carries the mode/device title, the brand opens the start page, actions have titles, and both designer entries open with the project preselected.

## Notes and Deviations (deliberate)

- **`systemHost()` uses `getHostName()`, not `getCanonicalHostName()`.** The canonical lookup performs a reverse-DNS query that blocked the printed URL for ~30 seconds on this network. `getHostName()` returns the same reachable name (`Mac-Mini.fritz.box`) immediately.
- **Vite allows the short name and its `.local` form in addition to `os.hostname()` and `<hostname>.local`.** `os.hostname()` is usually the DNS name, so the plan's two-entry list alone rejected the `Mac-Mini.local` URL that `start.sh` prints. The list is now the union of the DNS name, the short name, and both `.local` forms.
- **Stable base fallback.** A Unix socket path is limited to 104 bytes; this worktree's `pm-mock-runtime/target/machine/run/runtime.sock` is 119. `start.sh` keeps `target/machine` where it fits and otherwise uses a short stable base under the temp directory, derived from the repository path, so the paired-session guarantee holds in deep worktrees too. The printed `Base` names the directory actually used.
- **`onOnline` notifies only on a real change.** The first implementation reported online on every successful request, which re-notified the store, restarted the observers, and looped the connection-status reads. It now writes only when `isOffline` is set.
- **The `/mock` proxy is loopback-only.** Vite is network-bound for the app, but the mock's control routes are unauthenticated development tooling. A proxy `bypass` refuses non-loopback clients with `403`, while `/api` remains reachable; the web app itself never calls `/mock`. Verified with a request through the machine's LAN address.
- **`PM_WEB_ALLOWED_HOSTS` adds names.** It is merged with the machine's own name forms rather than replacing them, and the `PM_WEB_HOST` name is part of the machine names, so every non-loopback host `start.sh` prints is accepted by Vite.
- **Advisories follow their source.** The advisory layer remembers the title it generated, so a changed `data-advisory`, `aria-label`, or visible label updates the hover text, while a view-authored `title` is still never overwritten.
- **A designer route change while mounted reloads the selection.** `/designer?project=A` → `/designer?project=B` follows the same leave guard as the project selector instead of keeping the old project.

## Workflow Status

- Step 1 (worktree) — complete (orchestrator).
- Step 2 (personas, implementation, tests) — complete.
- Step 3 (this report, plan archived, dashboard) — complete.
- Step 4 (pre-commit gate) — `BUILD SUCCESS`; see the OpenRewrite note above.
- Steps 5–7 (PR, review window, merge queue, cleanup) — PR [#173](https://github.com/plan-marshall/pm-mcp-clients/pull/173) opened; the CodeRabbit review's ten findings were evaluated with the code-reviewer lens and addressed (checklist consistency, dashboard entry, fresh-machine instructions, the allowed-host override, `/mock` LAN exposure, subscriber isolation, advisory updates, designer same-route navigation, and the async header-status test).
- Step 8 (post-landing verification) is performed by the orchestrator session after the merge.

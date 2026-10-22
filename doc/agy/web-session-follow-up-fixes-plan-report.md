# Completion Report: `web-session-follow-up-fixes`

## Executive Summary

The three findings left open by the Step 8 verification of [`web-session-pairing-and-dev-host`](verified/web-session-pairing-and-dev-host-plan-report.md) (PR #173) are closed in `pm-web-app` and its launch scripts. The follow-up does not change the pairing contract, the mock runtime's device-secret lifecycle, the Vite network binding, or the message catalogues.

- **F1 — the dev-host output honours the loopback choice.** The URL rendering moved into the sourceable `scripts/connection-urls.sh`, and `start.sh` renders through it in both the already-running and the start branches. A loopback host (`127.0.0.1`, `localhost`, `::1`, `[::1]`) now prints only `Local:` and a loopback `Paired:` URL; no `Network` coordinate is printed even when a primary IPv4 exists, and the `Paired:` URL never swaps in that IP. A real name still prints `Local`, `Network`, and `Network (IP)` when the IP differs.
- **F2 — the designer route and a dirty draft are reconciled.** Route adoption is deferred until the leave guard commits. `Stay` restores the project, the selector, and the address bar to the loaded project; `Discard` adopts the new route project; a plain `/designer` is machine scope rather than "keep the previous project"; and a local selector change now writes `/designer?project=…` with `replaceState`, so reload, bookmark, and back all keep the selection.
- **F3 — the dead pre-pairing probe is retired.** `PairingView` no longer imports `ConnectionStatus`, keeps no `status` state, runs no `onMount` probe, and renders no fingerprint block or CSS. The Settings view's LAN fingerprint is untouched, and the browser's TLS warning remains the pre-pairing fingerprint source.
- **F4 — the failing matrix has regression coverage.** A Bash matrix test covers the loopback/name/IP/no-code/no-IPv4 cases, and four Vitest cases cover dirty stay, dirty discard, dirty machine-scope discard, and local selection surviving a rerender and reload.

## Delivered Artifacts

### `pm-web-app` — launch scripts

- `scripts/connection-urls.sh` **(new)** — `primary_ipv4`, `reachable_host`, `is_loopback_host`, and the pure `render_connection_urls <web_port> <pairing_code>`. The renderer always prints `Local:`, prints `Network:`/`Network (IP):` only for a non-loopback host, and prints `Paired:` on the resolved host with the supplied code or omits it when no code is available.
- `scripts/connection-urls.test.sh` **(new, executable)** — `set -euo pipefail`; sources the helper, stubs `reachable_host`/`primary_ipv4`, and asserts eight cases: `127.0.0.1`, `localhost`, `::1`, `[::1]`, name+IP, host equal to IP, no pairing code, and no IPv4.
- `start.sh` — sources the helper after `SCRIPT_DIR`; the three moved functions and both old print functions are deleted; `current_pairing_code` stays here (it owns `REPO_ROOT`, `/mock/state`, `POST /mock/pairing`, and the log fallback); all three call sites render through `render_connection_urls "$port" "$(current_pairing_code)"`.
- `package.json` — `test:urls` runs the Bash matrix.

### `pm-web-app` — store and views

- `src/lib/stores/runtime.ts` — `replaceDesignerProject(projectId)` updates `designerProjectId` and writes `/designer?project=<id>` (or `/designer`) with `syncUrl(path, true)`.
- `src/lib/stores/runtime.test.ts` — two cases: the project reaches the store and the address bar by replacement, and machine scope writes the plain path.
- `src/lib/views/ProcessDesigner.svelte` — the route effect adopts through one `adoptProject` helper only after the guard commits; the `next === null` early return is gone; `stayInDesigner` restores the loaded project and replaces the route; `onProjectChange` loads the selected scope and keeps it in the URL.
- `src/lib/views/ProcessDesigner.test.ts` — four new cases in a dedicated route-reconciliation describe block: dirty `A -> B -> Stay` (and a later `B` asks again), dirty `A -> B -> Discard`, dirty `A -> /designer -> Discard`, and a clean local selection surviving rerender and reload.
- `src/lib/views/PairingView.svelte` — removed the `onMount`/`apiClient`/`ConnectionStatus` imports, the `status` state, the probe, the fingerprint markup, and the fingerprint CSS. Pairing error mapping, the single-use/offline/revoked copy, the form, the QR, and the command copy are unchanged.
- `src/lib/views/PairingView.scenarios.test.ts` — removed the impossible paired-pairing fingerprint test; the no-probe, invalid-code, offline, and revoked-reason tests stay. The QR test's now-dead `getConnectionStatus` stub was dropped.
- `README.md` — the pairing capability line no longer claims an in-page pre-pairing fingerprint; the printed-output section states the loopback rule; the phone section states that `Paired:` uses the resolved host and never substitutes the IP; `test:urls` is listed under Available Scripts.

### Documentation

- This report.
- The plan archived at [`implemented/web-session-follow-up-fixes-plan.md`](implemented/web-session-follow-up-fixes-plan.md), with every relative link rebased for the new location.
- [`doc/agy/README.md`](README.md) dashboard row and the [`implemented/README.md`](implemented/README.md) archive index updated.
- [`doc/findings-web.txt`](../findings-web.txt) — the follow-up pointer now names the archived plan.

## Finding-by-Finding Resolution

| # | Root cause in the landed code | Correction delivered | Evidence |
|---|---|---|---|
| **F1** | `print_connection_urls` printed `Network (IP)` whenever a primary IPv4 existed, independent of the resolved host, and `print_paired_url_if_available` replaced a loopback host with that IP. | Sourceable renderer: `Network`/`Network (IP)` only for a non-loopback host; `Paired:` on the resolved host, never a substituted IP; omitted without a code. | `connection-urls.test.sh` eight-case matrix; live `start.sh` output with and without `PM_WEB_HOST=127.0.0.1`. |
| **F2** | `ProcessDesigner` set `appliedRouteProjectId = next` before the leave guard resolved; `stayInDesigner` restored only `projectId`; `onProjectChange` loaded the project without replacing the route; `next === null` returned early. | Deferred adoption via `adoptProject`; `Stay` restores project/selector/route; `Discard` adopts the new route project; plain `/designer` is machine scope; local selection replaces the query. | `ProcessDesigner.test.ts` dirty stay/discard/machine-scope cases and the local-selection rerender/reload case; `runtime.test.ts` replacement semantics. |
| **F3** | `PairingView` rendered only when `!isPaired` but probed only when `isPaired()`, so the probe and its fingerprint block were unreachable; the unit test arranged the impossible paired-pairing state. | Probe, imports, state, markup, and CSS removed; impossible-state test removed; Settings fingerprint and its E2E coverage untouched. | `grep` for `ConnectionStatus`/`pairing-fingerprint` in the pairing surface; `PairingView` suites still pass. |
| **F4** | No sourceable seam in `start.sh`; designer tests covered only `null -> project`. | Bash matrix test and four designer route cases. | `npm run test:urls`; `npm test`. |

## Test & Verification Results

### Unit, component, and script tests

- `npm run test:urls` — **all eight cases passed**.
- `npm test` — **52 files, 448 tests passed** (the suite was 443 before this plan; five new tests).
- `npm run check` — **0 errors, 0 warnings**.
- `npm run lint` — clean.
- Prettier on every file this plan touched — **all clean**. `npm run format:check` repo-wide still reports **seven files that are byte-identical to `main`** (`src/lib/api/scenarioMockFetch.test.ts`, `scenarioMockFetch.ts`, `components/BlockPalette.test.ts`, `GitSourceIntakeModal.test.ts`, `LocalChangesDiffPreview.svelte`, `PrAdoptPanel.svelte`, `views/CompletionReportView.svelte`). That is pre-existing baseline drift outside this plan's No-Touch boundary; it is not introduced or worsened here, and it was left untouched deliberately.

### End-to-end (Playwright, Chromium)

- `tests/designer.spec.js` — **11/11 passed** against the mock runtime started by the suite's global setup and the Vite development server. This is the existing designer regression surface; `pm-web-e2e` is untouched by this plan.

### Full pre-commit gate

```text
./mvnw verify -Ppre-commit
...
pm-api ............. SUCCESS
pm-exec ............ SUCCESS
pm-mock-runtime .... SUCCESS
pm-relay ........... SUCCESS
pm-operator ........ SUCCESS
pm-web-app ......... SUCCESS
pm-web-e2e ......... SUCCESS
BUILD SUCCESS   (10:13 min)
```

`git status` before and after the gate showed only this plan's files plus the two new script files; the gate did not rewrite any unrelated file.

## Domain Invariants

| Invariant | Status |
|---|---|
| Single-use pairing contract, failed-redemption limit, device-secret format | Unchanged; no file under `pm-mock-runtime` or the pairing contract was modified |
| Vite bind, allowed-host list, `/mock` loopback proxy | Unchanged; `vite.config.ts` is untouched |
| Message catalogue (EN & DE) | Unchanged; no new or renamed user-facing string |
| Loopback is an explicit no-exposure choice | Enforced in the renderer and proven by the matrix test and live output |
| Bash 3.2 + `set -u` compatibility | Preserved; the helper adds no array expansion, and `start.sh` keeps its existing empty-array idiom |
| Responsive layout / phone behaviour | Unchanged; no markup besides the removed fingerprint block |
| Module boundaries | Only `pm-web-app` shell, store, designer, and pairing view; `pm-api`, `pm-mock-runtime`, `pm-operator`, `pm-relay`, `pm-exec`, and `pm-web-e2e` are untouched |
| No plan-marshall tooling dependency | Followed `doc/agy/runbook.md` directly with `./mvnw` and native tools |

## Manual Evidence

Recorded against the real launch scripts on this machine, with the mock runtime on the `web-pairing` scenario:

1. **Loopback with a live pairing code** — `PM_WEB_HOST=127.0.0.1 pm-web-app/start.sh` printed:

   ```text
   pm-web-app started successfully (PID: 66089).
   Local:       http://localhost:5173
   Paired:      http://127.0.0.1:5173/pair#code=KXQB-MRTN
   ```

   No `Network` or `Network (IP)` line; the paired URL is on the loopback host. The code `KXQB-MRTN` was the live `pairing_code` from `GET /mock/state`.

2. **Without the override** — `pm-web-app/start.sh` printed:

   ```text
   Local:       http://localhost:5173
   Network:     http://Mac-Mini.local:5173
   Network (IP): http://192.168.178.126:5173
   ```

3. **Both `start.sh` branches** — the already-running branch printed the same coordinates as the start branch for each case.

The interactive dirty-route walkthrough (edit a draft, navigate to another plan's designer, choose Stay/Discard, change the selector, reload) was **not performed in a live browser**: this environment offers no browser tool. The same state transitions are driven by the four Vitest cases through the real component, including the address bar, the selector, the loaded project, and the store; the existing Playwright designer suite passed as a regression check.

## Plan Archival & Link Rebase

The plan moved from `doc/agy/web-session-follow-up-fixes-plan.md` to `doc/agy/implemented/web-session-follow-up-fixes-plan.md`. Every relative link was rebased for the new depth:

| Link class | Before | After |
|---|---|---|
| Meta and design documents | `../meta/...`, `../design/...` | `../../meta/...`, `../../design/...` |
| Use cases and specifications | `../../../plan-marshall-documentation/...` | `../../../../plan-marshall-documentation/...` |
| Runbook, sibling reports, verified archive | `runbook.md`, `verified/...` | `../runbook.md`, `../verified/...` |
| Predecessor plans | `implemented/<name>.md` | `<name>.md` |

An internal-link check over the moved plan resolved every relative path; the report's own links were checked in place. The dashboard row and the implemented index now point at the archived plan, and the report is left at `doc/agy/web-session-follow-up-fixes-plan-report.md` for the orchestrator's Step 8.

# Implementation Plan: `web-dev-host-and-session-persistence` (Allow the Printed Dev Host and Keep the Dev Session Secret Across Builds and Checkouts)

This implementation plan resolves two live findings from the web development loop:

1. The `Paired:` URL printed by `pm-web-app/start.sh` uses the machine name (`http://mac-mini.local:5173/pair#code=…`), but Vite answers `403 Blocked request. This host ("mac-mini.local") is not allowed` because the allowed-host list and the browser's lower-case `Host` header do not match by case.
2. The browser loses its paired session after a build or checkout change: the mock's device secret is stored under the checkout-local `target/machine` (or a temp path derived from the checkout), so `mvn clean`, a second worktree, or a temp cleanup changes the accepted secret. The next authenticated heartbeat or SSE connect gets a `401`, the app clears the pairing, and the now-consumed/expired single-use code forces a new code.

The single-use pairing contract, the failed-redemption limit, the pairing-code lifetime, the real runtime's 90-day device idle expiry, and the Vite network binding are **not** changed. This plan makes the dev-host allow-list match the name it prints, and gives the dev loop one machine-wide device secret that survives builds and checkouts.

It operationalizes [`doc/meta/implement_use-cases.adoc`](../../meta/implement_use-cases.adoc) under the execution discipline of [`doc/agy/runbook.md`](../runbook.md).

---

## 1. Traceability & Context

| Dimension | Reference |
|---|---|
| **Findings Source** | Operator report of the `403` blocked host and the recurring session/pairing loss, against the verified [`web-session-pairing-and-dev-host`](../verified/web-session-pairing-and-dev-host-plan-report.md) and [`web-session-follow-up-fixes`](../verified/web-session-follow-up-fixes-plan-report.md) landings |
| **Use Cases** | [`UC-ENT-1`](../../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-1) (Enter and Connect), [`UC-ENT-2`](../../../../plan-marshall-documentation/doc/use-cases/02-entry.adoc#UC-ENT-2) (Manage Paired Devices) |
| **Specifications** | [`03-web-server.adoc#web-listener`](../../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-listener) (Host validation), [`#devices`](../../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#devices) (device secret, `last_used_at`, idle expiry), [`#web-app`](../../../../plan-marshall-documentation/doc/specification/cli-and-security/03-web-server.adoc#web-app) (localStorage device secret) |
| **Local Client Design** | [`doc/design/web-app/entry.adoc`](../../design/web-app/entry.adoc), [`doc/design/web-app/README.adoc`](../../design/web-app/README.adoc) |
| **Predecessor Plans** | [`implemented/web-session-pairing-and-dev-host-plan.md`](web-session-pairing-and-dev-host-plan.md), [`implemented/web-session-follow-up-fixes-plan.md`](web-session-follow-up-fixes-plan.md) |
| **Implementation Recipe** | [`doc/meta/implement_use-cases.adoc`](../../meta/implement_use-cases.adoc) |
| **Operational SOP** | [`doc/agy/runbook.md`](../runbook.md) |

### 1.1 Finding Analysis (finding -> root cause -> correction)

| # | Finding | Root cause in the landed code | Correction in this plan |
|---|---|---|---|
| **F1** | `http://mac-mini.local:5173/pair#code=…` is refused with `Blocked request. This host ("mac-mini.local") is not allowed.` | `vite.config.ts#machineHostNames` keeps `os.hostname()`/`PM_WEB_HOST` exactly as returned (`Mac-Mini.fritz.box`, `Mac-Mini`, `Mac-Mini.local`), and `allowedHostNames` keeps additions exactly as typed. Vite 8's `isHostAllowedInternal` compares the lower-case `Host` header by exact string equality. `start.sh` also prints the mixed-case `Mac-Mini.local`. The allowed list therefore lacks the lower-case `mac-mini.local` the browser sends. | Normalize every allowed name to lower case and strip a trailing dot; normalize `PM_WEB_ALLOWED_HOSTS` additions; print the resolved name lower-case from `connection-urls.sh`. Verify with `curl --resolve` and an explicit lower-case `Host` header. |
| **F2** | The session is lost after a build or checkout change; the app asks for a new pairing code. | `pm-mock-runtime/start.sh` stores the device secret under `$SCRIPT_DIR/target/machine/state/mock-web-device-secret`, or under `${TMPDIR}/pm-mock-<checkout-hash>` for a deep path. Both are checkout-local build/temp locations. `mvn clean` removes the first; a second worktree or a temp cleanup changes the second. The mock then accepts a different secret, the web app's 30-second connection heartbeat or the next authenticated call receives `401`, and `ApiClient` correctly clears the pairing and shows the revoked page. The single-use code is already consumed or past its 300-second lifetime, so a new one is required. | Store the dev device secret once per machine outside the checkout and outside `target`, e.g. `~/.plan-marshall-mcp/mock-web-device-secret` (mode `0600`), with a `PM_MOCK_DEVICE_SECRET_FILE` override. `start.sh` reads/creates it and passes it through the existing `--device-secret` option; the per-checkout base remains for scenario state. Deleting the global secret file is the explicit fresh-session action. |
| **F3** | No regression check proves the printed host is allowed or that the secret survives a rebuild/checkout change. | The existing tests cover the URL string matrix, not Vite's actual `Host` validation; the secret tests restart within one base, not across `target/machine` removal or a second checkout. | Add a curl-based host check using `curl --resolve` and a curl-based session check that pairs, records the secret, deletes `target/machine`, restarts the mock, and reuses the same secret. Record both outputs in the completion report. |

> [!NOTE]
> The pairing code remains single-use and short-lived. This plan does not make a consumed or expired code redeemable again; it prevents the **device secret** from changing underneath a still-open browser. A browser that genuinely lost its `localStorage` secret (different origin, storage cleared, or explicit revoke) still needs a fresh code.

---

## 2. Scope & Objectives

### Part A: Make the Printed Host Allowed (F1)

1. **Canonicalize the Vite allowed-host list**:
   - Add a `normalizeHost(value)` helper in `pm-web-app/vite.config.ts` that trims, lower-cases, and strips one trailing dot.
   - Use it for `PM_WEB_HOST`/`os.hostname()`, the short name, both `.local` forms, and every `PM_WEB_ALLOWED_HOSTS` addition.
   - Do not append `.local` to an IP literal; keep the existing IP allowance and only add name forms for DNS names.
2. **Print the same canonical name**:
   - In `pm-web-app/scripts/connection-urls.sh`, normalize the resolved host to lower case before building `Network`, `Network (IP)`, and `Paired` authorities. IPv6 bracketing stays.
   - Update `pm-web-app/scripts/connection-urls.test.sh` to assert `Mac-Mini.local` is printed as `mac-mini.local` and the existing loopback/name/IP matrix still holds.
3. **Keep the security posture**:
   - `allowedHosts` remains a finite list; no `true`, no wildcard, no foreign host. `PM_WEB_ALLOWED_HOSTS` still adds, never replaces, the machine's names.
4. **Curl proof**:
   - Start Vite with `PM_WEB_HOST=Mac-Mini.local`, bind the test server to `127.0.0.1` with `--host 127.0.0.1`, and assert `http://mac-mini.local:<port>/` and `/pair` return `200` via `curl --resolve`, while an unknown host returns `403`.

### Part B: Keep the Dev Device Secret Across Builds and Checkouts (F2)

1. **One machine-wide secret**:
   - Add `pm-mock-runtime/scripts/mock-device-secret.sh` with:
     - `mock_secret_file()`: `${PM_MOCK_DEVICE_SECRET_FILE:-$HOME/.plan-marshall-mcp/mock-web-device-secret}`.
     - `ensure_mock_device_secret()`: create the parent directory, generate `pmw_mock_secret_<random>` only when the file is absent or empty, write through a same-directory temporary file with mode `0600`, and atomically move it into place.
   - The helper never prints the secret value to a log or the terminal.
2. **Pass it to the mock**:
   - In `pm-mock-runtime/start.sh`, parse `-d`/`--device-secret`/`--device-secret=*` so an explicit operator secret still wins.
   - When no explicit secret is given, source the helper, call `ensure_mock_device_secret`, and add `--device-secret "$secret"` to `FINAL_ARGS`.
   - Keep the existing checkout-local `--base` (or explicit `--base`) for scenario state, socket, and runtime token; only the web device secret becomes machine-wide.
   - The mock already accepts `--device-secret` (`MockRuntimeCli`); no Java change is required.
3. **Document the lifecycle**:
   - `pm-mock-runtime/README.adoc`, the root `README.md`, and `pm-web-app/README.md` state that the dev secret lives at the global path, survives `mvn clean` and worktree switches, and is reset by deleting that file (not by deleting `target/machine`).
4. **Keep the session touched**:
   - The existing `+layout.svelte` 30-second authenticated `getConnectionStatus` heartbeat already updates the real runtime's `last_used_at`; add a Vitest case with fake timers that proves the heartbeat fires a second time after 30 seconds while paired.
   - If the test reveals the heartbeat is not active on a route, fix the effect there; do not change the specification's 90-day idle expiry.
5. **Curl proof**:
   - Pair once, capture the `device_secret`, verify `200` on `/api/v1/status`, stop the mock, delete `pm-mock-runtime/target/machine`, start the mock again, and verify the **same** secret still returns `200`.
   - Repeat from a second worktree when available; the global secret file must be the same. Verify a missing/wrong secret still returns `401`.

### Non-Goals (Explicitly Out of Scope)

- Changing the single-use pairing contract, pairing-code lifetime (`300` seconds), failed-redemption limit, or the real runtime's 90-day device idle expiry.
- Making a consumed or expired pairing code reusable.
- Changing `server.host` from network-bound to loopback, the `/api` or `/mock` proxies, the Vite port, or the TLS/LAN listener.
- Adding a new authentication scheme; the device secret remains the credential.
- Fixing unrelated web views or the E2E suite.

---

## 3. Concurrency & Isolation

- **Branch**: `fix/web-dev-host-and-session-persistence` off latest `main`.
- **Worktree**: `/Users/oliver/git/worktrees/pm-mcp-clients-web-dev-host-and-session-persistence`.
- **Parallelizable with**: nothing currently active; this touches the Vite config, the dev launch scripts, and one heartbeat test.

### Boundary Rules

- **Sole Owner of**:
  - `pm-web-app/vite.config.ts`
  - `pm-web-app/scripts/connection-urls.sh` and `connection-urls.test.sh`
  - `pm-web-app/start.sh` (only if the printed host path needs a call-site adjustment)
  - `pm-web-app/src/routes/layout.test.ts` (heartbeat case only)
  - `pm-mock-runtime/start.sh`
  - `pm-mock-runtime/scripts/mock-device-secret.sh` (new)
  - `pm-mock-runtime/scripts/mock-device-secret.test.sh` (new)
  - `pm-web-app/README.md`, `pm-mock-runtime/README.adoc`, root `README.md`
  - `doc/agy/web-dev-host-and-session-persistence-plan.md`, its completion report, and the `doc/agy` dashboard/index rows for this plan
- **No Touch**:
  - `pm-mock-runtime/src/main/java/**` (the CLI's `--device-secret` already exists)
  - `pm-api`, `pm-operator`, `pm-relay`, `pm-exec`
  - the pairing/session logic in `client.ts`, `runtime.ts`, `PairingView.svelte`, and the catalogues
  - `pm-web-app/vite.config.ts` proxy rules, host binding, and allowed-host security (except canonicalization)
  - `pm-web-e2e` and any unrelated scenario/start files

---

## 4. Workflow Checklist (conforming to `doc/agy/runbook.md`)

- [x] 1. Isolated worktree created at `/Users/oliver/git/worktrees/pm-mcp-clients-web-dev-host-and-session-persistence` on branch `fix/web-dev-host-and-session-persistence` off latest `main`; Step 1 committed and pushed.
- [x] 2. Domain skills & engineering personas loaded where applicable:
  - `plan-marshall-persona-implementer` & `pm-dev-frontend:javascript` for the Vite host canonicalization and the shell secret helper.
  - `plan-marshall-persona-module-tester` & `pm-dev-frontend:jest-testing` for the curl matrix, Bash test, and heartbeat test.
  - `plan-marshall-persona-security-expert` for the `0600` secret file, atomic publication, and no-secret-in-log rule.
  - `plan-marshall-persona-documenter` for the README and the global-secret lifecycle.
  - `plan-marshall-persona-code-reviewer` for review-bot triage.
- [x] 3. Incremental implementation with tests following the phased technical recipe.
- [x] 4. Final completion report written (`doc/agy/web-dev-host-and-session-persistence-plan-report.md`), initial plan moved to `doc/agy/implemented/web-dev-host-and-session-persistence-plan.md`, **all relative links rebased for the new location**, and the dashboard/implemented index updated. (Do NOT move the report to `verified/` — Step 8 is reserved for the orchestrator.)
- [x] 5. Pre-commit check passed (`./mvnw verify -Ppre-commit`; required because this plan changes TypeScript, shell scripts, and `package.json`/docs).
- [ ] 6. PR opened (NEVER use `skip-bot-review` on a worker PR), review timer scheduled, 100% of bot & Sonar findings resolved via `persona-code-reviewer`.
- [ ] 7. Enqueued in merge queue, landed on `main`, worktree and branch cleaned up.
- [ ] 8. Post-landing verification performed and verified report moved to `doc/agy/verified/web-dev-host-and-session-persistence-plan-report.md` by the orchestrator session.

> [!IMPORTANT]
> **Plan archival link rule**: this plan is written at `doc/agy/`; its links use `../meta`, `../design`, `../../../plan-marshall-documentation`, `runbook.md`, and `implemented/<name>.md`. When Step 4 moves it to `doc/agy/implemented/`, rebase every relative link (`../../meta`, `../../design`, `../../../../plan-marshall-documentation`, `../runbook.md`, sibling `<name>.md`) and re-run the internal-link check.

---

## 5. Cognitive Personas & Domain Skills Matrix

| Phase / Task | Primary Persona Skill | Supporting Domain Skills | Focus |
|---|---|---|---|
| **Phase 1: Reproduce** | `plan-marshall-persona-integration-tester` | `pm-dev-frontend:javascript` | Curl `Host` reproduction, secret-loss reproduction, evidence for the plan. |
| **Phase 2: Host allow-list** | `plan-marshall-persona-implementer` | `pm-dev-frontend:javascript`<br>`plan-marshall-persona-module-tester` | Lower-case canonicalization, printed host, curl matrix. |
| **Phase 3: Session secret** | `plan-marshall-persona-implementer` | `plan-marshall-persona-security-expert`<br>`plan-marshall-persona-module-tester` | Global `0600` secret, atomic create, `--device-secret`, restart/checkout proof. |
| **Phase 4: Verification** | `plan-marshall-persona-module-tester` | `pm-dev-frontend:jest-testing`<br>`plan-marshall-persona-integration-tester` | Bash tests, heartbeat test, curl proofs, full pre-commit. |
| **Phase 5: Report & archive** | `plan-marshall-persona-documenter` | `plan-marshall-persona-code-reviewer` | Report, link rebase, dashboard/index, review triage. |

---

## 6. Phased Technical Recipe

### Phase 1: Reproduce the Two Failures

- [x] **1.1 Reproduce the host rejection**:
  - In `pm-web-app`, start Vite with `PM_WEB_HOST=Mac-Mini.local npm run dev -- --host 127.0.0.1 --port 5199 --strictPort` (background; wait for the port).
  - Run:
    ```bash
    curl --resolve mac-mini.local:5199:127.0.0.1 -sS -o /dev/null -w '%{http_code}\n' \
      http://mac-mini.local:5199/
    ```
    Expect **403** before the fix. Record the exact body: it names `mac-mini.local` and `server.allowedHosts`.
  - Confirm the allow-list code keeps the mixed-case `Mac-Mini.local` (`vite.config.ts#machineHostNames`) and that Vite compares the lower-case `Host` header exactly (`isHostAllowedInternal`).
- [x] **1.2 Reproduce the session loss**:
  - Start the mock via `pm-mock-runtime/start.sh --scenario web-pairing --web-port 7420`.
  - Read the open code from `GET /mock/state`, redeem it with `POST /api/v1/pair`, and capture `device_secret`.
  - `curl -H "Authorization: Bearer $SECRET" -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:7420/api/v1/status` → expect **200**.
  - Stop the mock, `rm -rf pm-mock-runtime/target/machine`, start it again, and repeat the same curl → expect **401** before the fix. This is the reproducible session loss: the secret was base-local, not machine-local.
  - Record that the current base is `$SCRIPT_DIR/target/machine` or `${TMPDIR}/pm-mock-<checkout-hash>` and that `mvn clean` / a second checkout changes it.

### Phase 2: Host Allow-List (F1)

- [x] **2.1 Canonicalize names in Vite**:
  - In `vite.config.ts`, add:
    ```ts
    function normalizeHost(value: string): string {
      return value.trim().toLowerCase().replace(/\.$/, '');
    }
    ```
  - Build `machineHostNames()` from `normalizeHost(process.env.PM_WEB_HOST ?? os.hostname())`; for a DNS name include the normalized name, `${name}.local`, the short name, and `${short}.local`; do not synthesize `.local` forms for an IP literal.
  - Normalize `PM_WEB_ALLOWED_HOSTS` additions with the same helper and deduplicate the union.
  - Keep `allowedHosts` finite; do not set `allowedHosts: true`.
- [x] **2.2 Print the canonical name**:
  - In `connection-urls.sh`, normalize the resolved host to lower case before `url_host`; keep the existing loopback and IPv6 handling.
  - Update `connection-urls.test.sh`: add `Mac-Mini.local` → `mac-mini.local`; keep the IPv6, name+IP, no-code, and no-IPv4 cases.
  - Only touch `start.sh` if the call-site needs no change; the rendering helper owns the output.
- [x] **2.3 Add the curl host test**:
  - Add `pm-web-app/scripts/verify-dev-host.sh` (or record the exact commands in the completion report if a committed script would duplicate the Vite launch):
    ```bash
    PM_WEB_HOST=Mac-Mini.local npm run dev -- --host 127.0.0.1 --port 5199 --strictPort &
    # wait for 127.0.0.1:5199
    curl --resolve mac-mini.local:5199:127.0.0.1 -sS -o /dev/null -w '%{http_code}\n' http://mac-mini.local:5199/
    curl --resolve mac-mini.local:5199:127.0.0.1 -sS -o /dev/null -w '%{http_code}\n' http://mac-mini.local:5199/pair
    curl -H 'Host: mac-mini.local:5199' -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:5199/
    curl -H 'Host: not-this-machine.local:5199' -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:5199/
    ```
    Expect `200`, `200`, `200`, `403`. `--resolve` makes the test independent of mDNS; the Host header is the lower-case form the browser sends.

### Phase 3: Machine-Wide Dev Secret (F2)

- [x] **3.1 Add the secret helper**:
  - Create `pm-mock-runtime/scripts/mock-device-secret.sh` with:
    - `mock_secret_file`: `${PM_MOCK_DEVICE_SECRET_FILE:-$HOME/.plan-marshall-mcp/mock-web-device-secret}`.
    - `ensure_mock_device_secret`: ensure the parent directory; if the file is absent or empty, generate `pmw_mock_secret_<random>`; write a same-directory temp file with `umask 077`/mode `0600`; `mv` it into place; print only the secret value on stdout for the caller.
    - It must not write the secret to stderr, the runtime log, or the completion report.
  - Add `pm-mock-runtime/scripts/mock-device-secret.test.sh`:
    - A temporary `PM_MOCK_DEVICE_SECRET_FILE` is created once and reused with the same value.
    - The file is non-empty and `0600`.
    - An empty file is regenerated.
    - Two calls in the same state directory return the same secret.
- [x] **3.2 Wire `start.sh`**:
  - Add `HAS_DEVICE_SECRET=false` and parse `-d|--device-secret` and `--device-secret=*` into `PASSED_ARGS`; keep an explicit operator secret authoritative.
  - When no explicit secret is given, source `scripts/mock-device-secret.sh`, set `SECRET="$(ensure_mock_device_secret)"`, and append `--device-secret "$SECRET"` to `FINAL_ARGS`.
  - Keep the `--base` logic exactly as is; the base still carries the scenario state, runtime token, and socket. Do not put the global secret under `target/`.
- [x] **3.3 Document the reset**:
  - `pm-mock-runtime/README.adoc`, root `README.md`, and `pm-web-app/README.md`: the dev secret is at the global path, survives `mvn clean` and worktree switches, and is reset by deleting that file. Deleting `target/machine` resets scenario state but **not** the device secret.
- [x] **3.4 Keep the session heartbeat honest**:
  - Add a `layout.test.ts` case with fake timers: render paired, assert the first `getConnectionStatus` call, advance 30 seconds, assert a second call, and assert the offline state is cleared when a probe succeeds.
  - The existing 30-second heartbeat is the keepalive; do not change the 90-day real-runtime idle expiry. If the test shows the effect stops on a route, fix that route's dependency/cleanup path.

### Phase 4: Verification (Curl Is Mandatory)

- [x] **4.1 Host curl matrix** (exact commands above): `200` for the printed lower-case name and `/pair`, `200` for the explicit lower-case Host header, `403` for an unknown host. Record the four status codes in the report.
- [x] **4.2 Session curl proof**:
  - Start the mock with the new global secret; pair via `curl` as in Phase 1 and capture `SECRET`.
  - `curl -H "Authorization: Bearer $SECRET" .../api/v1/status` → `200`.
  - Stop; `rm -rf pm-mock-runtime/target/machine`; start again; same curl → **`200`** (the fix), with the global secret file unchanged.
  - Repeat the restart from a second worktree when available; the same secret must still return `200`.
  - `curl` with no secret and with a wrong secret → `401` (sanity).
- [x] **4.3 Unit/module gates**:
  - `bash pm-mock-runtime/scripts/mock-device-secret.test.sh`
  - `cd pm-web-app && npm run test:urls`
  - `cd pm-web-app && npm test`
  - `cd pm-web-app && npm run check && npm run lint`
  - Prettier check on every changed file (`npx prettier --check <files>`); record any pre-existing repo-wide drift without changing untouched files.
  - `./mvnw verify -Ppre-commit` with `BUILD SUCCESS` and a clean `git status`.
- [x] **4.4 Report and archive**:
  - Write `doc/agy/web-dev-host-and-session-persistence-plan-report.md` with the two curl matrices, the restart proof, the secret-file mode/path, the test counts, and the link-rebase result.
  - Move the plan to `doc/agy/implemented/web-dev-host-and-session-persistence-plan.md` and **rebase all relative links**; run the internal-link check.
  - Update `doc/agy/implemented/README.md` and the dashboard; mark Steps 1–7 as executed, leaving Step 8 for the orchestrator.

---

## 7. Acceptance Criteria

| Finding | Acceptance criterion | Evidence |
|---|---|---|
| **F1** | Vite returns `200` for `http://mac-mini.local:<port>/` and `/pair` when the request is sent with the lower-case name, using `curl --resolve` so DNS/mDNS is not a confounder. | Curl matrix status codes + `connection-urls.test.sh`. |
| **F1** | Vite still returns `403` for an unknown host, and the allowed list is finite; `PM_WEB_ALLOWED_HOSTS` additions are normalized and still additive. | Curl unknown-host case + config review. |
| **F2** | A paired `device_secret` still returns `200` after the mock is stopped, `pm-mock-runtime/target/machine` is deleted, and the mock is restarted; the global secret file is unchanged and `0600`. | Curl restart proof + `mock-device-secret.test.sh`. |
| **F2** | The same secret survives a start from a second worktree when one is available. | Curl second-worktree proof. |
| **F2** | The 30-second heartbeat runs while paired and refreshes the session; the 90-day idle-expiry specification is unchanged. | `layout.test.ts` fake-timer case. |
| **F3** | Missing/wrong secrets still return `401`; the single-use/expired code behavior is unchanged. | Curl negative cases + existing tests. |
| **F4** | `npm test`, `npm run check`, `npm run lint`, Prettier on the changed files, the Bash tests, and `./mvnw verify -Ppre-commit` pass. | Phase 4 output; completion report. |

---

## 8. Risk & Rollback Notes

- **Host canonicalization**: Vite compares exact strings. Lower-casing the **allowed list** and the **printed name** aligns with the browser's lower-case `Host`; it does not broaden the list to wildcards. IP literals remain allowed by Vite without a name entry.
- **Global dev secret**: This is a development credential, not a production one. It lives under the user's home with `0600`, is never logged, and is separate from the real runtime's `state/web-devices.json`. Deleting it is the explicit reset; deleting `target/machine` only resets scenario state.
- **Explicit operator secret**: `-d`/`--device-secret` continues to win over the global file; existing manual runs and tests are unaffected.
- **Concurrent starts**: The helper uses a same-directory temporary file and atomic move. Two simultaneous starts may generate two candidates; the last move wins and both processes read the file after the move. A start that sees an existing non-empty file reuses it.
- **Real runtime**: The 90-day idle expiry is a specification behavior; the existing authenticated heartbeat keeps `last_used_at` fresh. This plan does not change it. A genuine revocation or idle expiry still requires a new pairing.
- **Rollback**: Revert the single follow-up commit; the only persistent artifact is the global dev-secret file, which can be deleted independently. No scenario, API, or production data format changes.

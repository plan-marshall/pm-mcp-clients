# Completion Report: `web-dev-host-and-session-persistence`

The printed development host is now the canonical name Vite accepts, and the mock runtime's web
device secret is one machine-wide file that survives a rebuild and a worktree switch.

Companion plan: [`implemented/web-dev-host-and-session-persistence-plan.md`](implemented/web-dev-host-and-session-persistence-plan.md)
(input plan, archived after this report).

---

## 1. Executive Summary

Two live development-loop findings are closed:

| Finding | Root cause | Change |
|---|---|---|
| **F1** — `http://mac-mini.local:5173/…` was refused with `403 Blocked request. This host ("mac-mini.local") is not allowed.` | The allowed-host list held the mixed-case `Mac-Mini.local`, while Vite compares the browser's lower-case `Host` header by exact string equality. `start.sh` also printed the mixed-case name. | `vite.config.ts` canonicalizes every allowed name (trim, lower case, one trailing dot removed, no `.local` forms for IP literals); `connection-urls.sh` prints the same canonical form. |
| **F2** — the browser lost its session after `mvn clean`, a temp cleanup, or a second worktree, and required a new pairing code. | The mock's device secret lived under the checkout-local base (`pm-mock-runtime/target/machine`) or a checkout-derived temp path. | A new `pm-mock-runtime/scripts/mock-device-secret.sh` keeps one `0600` secret at `~/.plan-marshall-mcp/mock-web-device-secret` (`PM_MOCK_DEVICE_SECRET_FILE` overrides) and `start.sh` passes it with `--device-secret`; an explicit `-d`/`--device-secret` still wins. |

The single-use pairing contract, the 300-second code lifetime, the failed-redemption limit, the
real runtime's 90-day idle expiry, and the network binding are unchanged. No Java production code
was modified.

## 2. Delivered Artifacts

| File | Status | Purpose |
|---|---|---|
| `pm-web-app/vite.config.ts` | Modified | `normalizeHost`/`isIpLiteral`; canonical machine names and `PM_WEB_ALLOWED_HOSTS` additions; finite `allowedHosts` preserved. |
| `pm-web-app/scripts/connection-urls.sh` | Modified | Prints the lower-case host without a trailing dot, Bash 3.2-safe via `tr`; loopback and IPv6 handling unchanged. |
| `pm-web-app/scripts/connection-urls.test.sh` | Modified | Lower-case assertions plus a trailing-dot case; existing loopback/IPv6/name/IP/no-code/no-IPv4 cases retained. |
| `pm-web-app/scripts/verify-dev-host.sh` | New | `curl --resolve` host matrix against a loopback-bound Vite; `200` for the printed name and `/pair`, `403` for an unknown host. |
| `pm-web-app/package.json` | Modified | `test:dev-host` runs the matrix. |
| `pm-web-app/src/routes/+layout.svelte` | Modified | The heartbeat's effect depends on an `isInitialized && isPaired` derived value, so an unrelated store update no longer tears down and re-opens the event subscription. |
| `pm-web-app/src/routes/layout.test.ts` | Modified | Fake-interval case: first probe, a second probe after 30 s, and the stale offline banner cleared by the successful probe. |
| `pm-mock-runtime/scripts/mock-device-secret.sh` | New | `mock_secret_file`, `new_mock_device_secret`, `ensure_mock_device_secret`; exclusive `mkdir` lock, atomic same-directory publication, `0600`, value only on stdout. |
| `pm-mock-runtime/scripts/mock-device-secret.test.sh` | New | Nine cases: default path, override, creation, mode, silent stderr, reuse, empty-file regeneration, tightening an existing file, and two concurrent calls agreeing on one secret. |
| `pm-mock-runtime/start.sh` | Modified | Parses `-d`/`--device-secret`/`--device-secret=*`; supplies the machine-wide secret only when the operator did not; base comment corrected. |
| `README.md`, `pm-mock-runtime/README.adoc`, `pm-web-app/README.md` | Modified | Document the global secret path, the reset action, the canonical host, and the new check. |

No changes under `pm-mock-runtime/src/main/java/**`, `pm-api`, `pm-operator`, `pm-relay`,
`pm-exec`, `pm-web-e2e`, the pairing/session logic (`client.ts`, `runtime.ts`,
`PairingView.svelte`), or the catalogues.

## 3. Reproduction Before the Fix

### 3.1 Host rejection

Vite was started as `PM_WEB_HOST=Mac-Mini.local node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5199 --strictPort`. The four-request matrix returned:

```text
GET / via mac-mini.local          403
GET /pair via mac-mini.local      403
GET / with Host: mac-mini.local   403
GET / with an unknown Host        403
```

The `/` body was:

```text
Blocked request. This host ("mac-mini.local") is not allowed.
To allow this host, add "mac-mini.local" to `server.allowedHosts` in vite.config.js.
```

The allow-list held the mixed-case spelling; the browser sent the lower-case `Host`.

### 3.2 Session loss

`pm-mock-runtime/start.sh --scenario web-pairing --web-port 7420` printed a base under the temp
directory (`…/T/pm-mock-3159831222`) because the worktree path is too deep for a Unix socket.
After pairing with `KXQB-MRTN` the captured secret returned:

```text
GET /api/v1/status (before restart)   200
GET /api/v1/status (after deleting the base)   401
```

The secret had been persisted in the base, not per machine.

## 4. Test & Verification Results

### 4.1 Host curl matrix after the fix (`npm run test:dev-host`)

```text
Dev host check: configured 'Mac-Mini.local', printed and accepted as 'mac-mini.local'
  GET / by lower-case name                   200 (expected 200)
  GET /pair by lower-case name               200 (expected 200)
  GET / with the lower-case Host header      200 (expected 200)
  GET / with an unknown Host header          403 (expected 403)
verify-dev-host: all cases passed
```

The unknown host is still refused and the allowed list remains finite — no `true`, no wildcard,
and `PM_WEB_ALLOWED_HOSTS` still adds rather than replaces the machine's names. The script derives
its host from `reachable_host` — the same resolver `start.sh` prints — unless `PM_WEB_HOST`
overrides it, and every `curl` passes `--noproxy '*'` so a configured proxy cannot answer for the
loopback server.

### 4.2 Session restart proof

With the machine-wide secret in place:

| Step | Result |
|---|---|
| Pair, capture `device_secret`, compare with `~/.plan-marshall-mcp/mock-web-device-secret` | Identical (64 characters) |
| `GET /api/v1/status` with the captured secret | `200` |
| Stop the mock, delete `pm-mock-runtime/target/machine` **and** the printed temp `Base` | Both removed while the global file stayed `0600`, 64 bytes |
| Restart the mock, same `GET /api/v1/status` | **`200`** — the secret survived |
| Start the mock from a second checkout (a detached worktree of the same commit, port 7423, its own base `pm-mock-2782052548`) and reuse the same secret | **`200`** — the second base and checkout path do not change the credential |
| `GET /api/v1/status` with no secret | `401` |
| `GET /api/v1/status` with a wrong secret | `401` |

The secret file is `~/.plan-marshall-mcp/mock-web-device-secret`, mode `0600`, 64 bytes. Its value
never appears in the runtime log (`grep` count `0`) or in this report.

An explicit operator secret was verified separately: `start.sh --device-secret=…` returned `200`
for the explicit value and `401` for the global value, so the explicit option still wins.

### 4.3 Deterministic test gates

| Gate | Result |
|---|---|
| `bash pm-web-app/scripts/connection-urls.test.sh` | 11 cases passed |
| `bash pm-mock-runtime/scripts/mock-device-secret.test.sh` | 9 cases passed, including two concurrent calls that must agree on one secret |
| `cd pm-web-app && npm run test:urls` | Passed |
| `cd pm-web-app && npm run test:dev-host` | 4/4 status codes as expected |
| `cd pm-web-app && npm test` | 52 files, 454 tests passed |
| `cd pm-web-app && npm run check` | 0 errors, 0 warnings |
| `cd pm-web-app && npm run lint` | Passed |
| `npx vitest run src/routes/layout.test.ts` | 6 tests passed |
| Prettier | `vite.config.ts`, `src/routes/layout.test.ts`, `src/routes/+layout.svelte`, and `package.json` pass. `README.md` and `pm-web-app/README.md` are non-conforming at `HEAD` as well (verified with `git show HEAD:<path>`), so that pre-existing drift is recorded and not rewritten. Shell scripts are not supported by Prettier. |

The two READMEs were already non-conforming at `HEAD` (Prettier reports the `HEAD` copies as
well); that pre-existing drift was recorded and the untouched formatting was not rewritten.

### 4.4 Heartbeat

The new `layout.test.ts` case renders a paired layout, observes the first
`getConnectionStatus` probe, marks the store offline, advances 30 seconds, observes the second
probe, and asserts the stale offline flag is cleared by the successful request. Because the effect
now depends on `isInitialized && isPaired` rather than the whole store, unrelated store updates no
longer restart the interval and the event subscription.

## 5. Domain Invariants

| Invariant | Status |
|---|---|
| Vite `allowedHosts` finite, no wildcard/`true`, unknown host `403` | Holds |
| `PM_WEB_ALLOWED_HOSTS` additive and canonicalized | Holds |
| Bash 3.2 / `set -u`: no direct empty-array expansion, `tr` instead of `${var,,}` | Holds (`"${PASSED_ARGS[@]+…}"` retained) |
| Secret file owner-only (`0600`), atomic same-directory publication, never logged | Holds |
| Explicit `--device-secret` remains authoritative | Verified |
| Single-use code, 300-second lifetime, failed-redemption limit, 90-day idle expiry unchanged | Untouched |
| No Java production change; no pairing/session logic change | Holds |
| Responsive layout below 320 px | Untouched |

## 6. Build & Quality Evidence

```text
./mvnw verify -Ppre-commit
BUILD SUCCESS
Total time: 08:44 min
```

All eight reactor modules succeeded. OpenRewrite introduced no additional changes; `git status`
showed only the intended files after the gate. Javadoc warnings reported by the reactor are
pre-existing in `pm-operator` and unrelated to this change.

## 7. Plan Archival & Link Rebase

- The plan moved to `doc/agy/implemented/web-dev-host-and-session-persistence-plan.md`.
- Every relative link was rebased for the new depth: `../../meta`, `../../design`,
  `../../../../plan-marshall-documentation`, `../runbook.md`, `../verified/<name>.md`, and the
  sibling `web-session-*.md` plans.
- The internal-link check resolves every link inside this repository. The two cross-repository
  use-case anchors (`plan-marshall-documentation/doc/use-cases/02-entry.adoc`) are structurally
  rebased but unresolved in this checkout: the sibling documentation worktree at
  `/Users/oliver/git/worktrees/plan-marshall-documentation` is a stale detached checkout whose
  use-case file is still named `02-web-app.adoc`, while the main documentation checkout at
  `/Users/oliver/git/plan-marshall-documentation` carries the target `02-entry.adoc`. This is a
  pre-existing cross-repository checkout condition, not a link introduced here.
- `doc/agy/implemented/README.md` and `doc/agy/README.md` record the plan and this report.
- Step 8 (moving this report to `doc/agy/verified/`) remains with the orchestrator session.

# AGENTS.md

Guidance for AI coding agents working in this repository.

`CLAUDE.md` is the repository's agent guide: build commands, dependency rules, code standards, publishing, work
packages and git workflow. OpenCode V2 does not load it automatically — **read it before starting work** and follow
it. This file holds only what is specific to OpenCode and not covered there.

## Ports

Each plan uses the block its spec names in Standing Constraints:
- Mock web port: `20000 + 10 × NN`
- App port: `20000 + 10 × NN + 1`
where `NN` is the plan number (for example, PLAN-76 uses mock web port `20760` and app port `20761`). The rest of the block (`20000 + 10 × NN + 2` to `20000 + 10 × NN + 9`) is reserved.

Without a block the defaults apply: `7420` (mock web port) and `5173` (app port), which is the operator's own loop.

Environment variables:
- `PM_MOCK_WEB_PORT`: sets the mock runtime loopback web port.
- `PM_WEB_PORT`: sets the web app port.

An assigned port that is in use is a blocker, never a reason to take another port or kill foreign processes.

## Browser checks (Playwright MCP)

Playwright MCP is configured in `opencode.json`, pinned to a package version. `pm-web-app/README.md` is the source of
truth for the development setup; this section holds only what an automated browser check needs.

### Ports and startup

- App: `http://localhost:<app-port>` (default `5173`); mock runtime: `http://127.0.0.1:<mock-web-port>` (default `7420`). Vite proxies `/api` and `/mock` to the mock.
- Start the mock first, then the app; `pm-web-app/start.sh` does not start the mock:
  ```bash
  ./pm-mock-runtime/start.sh --scenario <scenario> --web-port <mock-web-port>   # stop with ./pm-mock-runtime/stop.sh
  PM_MOCK_WEB_PORT=<mock-web-port> ./pm-web-app/start.sh -p <app-port>          # stop with ./pm-web-app/stop.sh
  ```
  Use the mock's `start.sh`, not a bare `mvn exec:java`: the bare run keeps the base in a temporary directory and
  deletes it on shutdown, which takes the stored device secret with it, so a browser that paired once is refused
  with 401 on the next start.
- `start.sh` accepts `-p`/`--port` (and `-m`/`--mock-port`) and records the effective port in `pm-web-app/target/pm-web-app.port`. Read that
  file instead of assuming `5173` when a non-default port was used.
- `start.sh` prints `Local:`, `Network:` and `Paired:` coordinates, the last with a code that works right now.

### Pairing code

- A code is **single-use**. Take the current one from the `Paired:` line, or ask the mock:
  `POST http://127.0.0.1:<mock-web-port>/mock/pairing` returns the open code, or issues the next one when the previous was
  consumed. Re-running `start.sh` while a code is open prints the same code.
- Open `<app>/pair#code=<CODE>`. Do not rewrite a URL that still carries the code fragment; it is redeemed on load.
- `/mock` is restricted to clients on this machine. Never read or issue a code through the `Network:` URL.

### Persistence

- The browser keeps its pairing in `localStorage` under `pm_device_secret`.
- `pm-mock-runtime/start.sh` keeps the paired device secret in one machine-wide file at
  `~/.plan-marshall-mcp/mock-web-device-secret` (mode `0600`; `PM_MOCK_DEVICE_SECRET_FILE` overrides the path) and
  passes it as `--device-secret`. A pairing therefore survives `mvn clean`, a mock restart and a switch to another
  worktree.
- Deleting `pm-mock-runtime/target/machine` (or the printed `Base`) resets the scenario, **not** the device secret.
- Reset the pairing only by stopping the mock and deleting the secret file — then pair again with a fresh code.

### Rules for the MCP browser

- `opencode.json` passes `--isolated`, so the browser profile lives in memory and `localStorage` is empty on every
  launch: **pair again per session** with a fresh code, and never assume a previous session is still paired. This
  also keeps a stored secret from masking a broken pairing flow.
- Without `--isolated` the server uses a persistent profile under
  `~/Library/Caches/ms-playwright-mcp/mcp-{channel}-{workspace-hash}`. Use that only for a long manual session where
  pairing once is intended, never for a pairing-flow check; `--isolated` and `--user-data-dir` are mutually exclusive.
- `--output-dir .plan/temp/playwright-mcp` catches automatically named screenshots, snapshots and traces. Explicit
  `filename` arguments resolve against the workspace root instead, so omit the filename or pass a path under
  `.plan/temp/`. `.playwright-mcp/` is gitignored as a backstop when the flag is absent.

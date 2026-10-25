# AGENTS.md

Guidance for AI coding agents working in this repository.

`CLAUDE.md` is the repository's agent guide: build commands, dependency rules, code standards, publishing, work
packages and git workflow. OpenCode V2 does not load it automatically — **read it before starting work** and follow
it. This file holds only what is specific to OpenCode and not covered there.

## Browser checks (Playwright MCP)

Playwright MCP is configured in `opencode.json`, pinned to a package version. `pm-web-app/README.md` is the source of
truth for the development setup; this section holds only what an automated browser check needs.

### Ports and startup

- App: `http://localhost:5173`; mock runtime: `http://127.0.0.1:7420`. Vite proxies `/api` and `/mock` to the mock.
- Start the mock first, then the app; `start.sh` does not start the mock:
  ```bash
  ./mvnw -pl pm-mock-runtime exec:java -Dexec.args="--scenario <scenario> --web-port 7420"
  ./pm-web-app/start.sh          # stop with ./pm-web-app/stop.sh
  ```
- `start.sh` accepts `-p`/`--port` and records the effective port in `pm-web-app/target/pm-web-app.port`. Read that
  file instead of assuming `5173` when a non-default port was used.
- `start.sh` prints `Local:`, `Network:` and `Paired:` coordinates, the last with a code that works right now.

### Pairing code

- A code is **single-use**. Take the current one from the `Paired:` line, or ask the mock:
  `POST http://127.0.0.1:7420/mock/pairing` returns the open code, or issues the next one when the previous was
  consumed. Re-running `start.sh` while a code is open prints the same code.
- Open `<app>/pair#code=<CODE>`. Do not rewrite a URL that still carries the code fragment; it is redeemed on load.
- `/mock` is restricted to clients on this machine. Never read or issue a code through the `Network:` URL.

### Persistence

- The browser keeps its pairing in `localStorage` under `pm_device_secret`.
- The mock keeps the paired device secret in one file per machine at `~/.plan-marshall-mcp/mock-web-device-secret`
  (mode `0600`; `PM_MOCK_DEVICE_SECRET_FILE` overrides the path). A pairing therefore survives `mvn clean`, a mock
  restart and a switch to another worktree.
- Deleting `pm-mock-runtime/target/machine` (or the printed `Base`) resets the scenario, **not** the device secret.
- Reset the pairing only by stopping the mock and deleting the secret file — then pair again with a fresh code.

### Rules for the MCP browser

- By default the MCP server uses a temporary profile, so `localStorage` is empty on every launch: **pair again per
  session** using a fresh code. This is the right default; do not assume a previous session is still paired.
- Add `--user-data-dir .plan/temp/playwright-profile` to the `opencode.json` command only for a long manual session
  where pairing once is intended.
- Checks of the pairing flow itself must use a clean profile (`--isolated` or a fresh directory); a stored secret
  would mask a broken flow.
- Write screenshots, snapshots and traces under `.plan/temp/` (`--output-dir`), never the repository root.
  `.playwright-mcp/` is gitignored as a backstop.
